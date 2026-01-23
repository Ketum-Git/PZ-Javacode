// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.packets.ItemTransactionPacket;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public class TransactionManager {
    public static final byte success = 0;
    public static final byte reject = 1;
    public static final byte invalid = -1;
    public static final TransactionManager instance = new TransactionManager();
    private static final ConcurrentLinkedQueue<Transaction> transactions = new ConcurrentLinkedQueue<>();
    private static final HashMap<IsoPlayer, TransactionManager.LightweightData> lightweightData = new HashMap<>();

    public static void add(Transaction transaction) {
        transactions.add(transaction);
    }

    public static TransactionManager.LightweightData getLightweightData(IsoPlayer player) {
        if (!lightweightData.containsKey(player)) {
            lightweightData.put(player, new TransactionManager.LightweightData());
        }

        return lightweightData.get(player);
    }

    public static void update() {
        if (GameServer.server) {
            for (Transaction transaction : transactions) {
                if (transaction.state == Transaction.TransactionState.Accept && transaction.endTime <= GameTime.getServerTimeMills()) {
                    if (transaction.update()) {
                        transaction.setState(Transaction.TransactionState.Done);
                        if (transaction instanceof ItemTransactionPacket itemTransactionPacket) {
                            UdpConnection connection = GameServer.getConnectionFromPlayer(transaction.playerId.getPlayer());
                            if (connection != null && connection.isFullyConnected()) {
                                ByteBufferWriter bbw2 = connection.startPacket();
                                PacketTypes.PacketType.ItemTransaction.doPacket(bbw2);
                                itemTransactionPacket.write(bbw2);
                                PacketTypes.PacketType.ItemTransaction.send(connection);
                            }
                        }
                    } else {
                        transaction.setState(Transaction.TransactionState.Reject);
                    }
                }
            }

            transactions.removeIf(r -> r.state == Transaction.TransactionState.Done || r.state == Transaction.TransactionState.Reject);
        } else if (GameClient.client) {
            Stream<Transaction> transactionForDelete = transactions.stream().filter(t -> GameTime.getServerTimeMills() > t.startTime + 10000L);
            transactionForDelete.forEach(transactionx -> {
                transactions.remove(transactionx);
                DebugLog.Objects.noise("Timeout: %s", transactionx);
            });
        }
    }

    public static byte isConsistent(
        int itemId,
        InventoryItem worldItem,
        ItemContainer source,
        ItemContainer destination,
        String extra,
        ItemTransactionPacket dropTransaction,
        IsoPlayer player
    ) {
        String description = String.format(
            "item=%d source=%s destination=%s %s",
            itemId,
            source == null ? "null" : source,
            destination == null ? "null" : destination,
            dropTransaction == null ? "" : dropTransaction.getDescription()
        );
        if (destination != null
            || dropTransaction != null
                && dropTransaction.sourceId.containerType == ContainerID.ContainerType.IsoObject
                && dropTransaction.destinationId.containerType == ContainerID.ContainerType.IsoObject) {
            if (itemId != -1 && source != null) {
                if (!source.containsID(itemId)) {
                    DebugLog.Objects.noise("Inconsistent: source container is not contain the item (%s)", description);
                    return -1;
                }

                if (destination != null && !destination.isItemAllowed(source.getItemWithID(itemId))) {
                    DebugLog.Objects.noise("Inconsistent: destination container can't contain the item (%s)", description);
                    return 1;
                }

                if (source.getType().equals("floor")
                    && source.getItemWithID(itemId) != null
                    && source.getItemWithID(itemId).getWorldItem() == null
                    && source.getItemWithID(itemId).deadBodyObject == null) {
                    DebugLog.Objects.noise("Inconsistent: this item is not on the floor (%s)", description);
                    return -1;
                }

                if (destination != null
                    && destination.getCharacter() != null
                    && source.getParent() instanceof BaseVehicle
                    && IsoUtils.DistanceTo(
                            destination.getCharacter().getX(), destination.getCharacter().getY(), source.getParent().getX(), source.getParent().getY()
                        )
                        > 5.0) {
                    return 1;
                }

                if (source.getCharacter() != null
                    && destination.getParent() instanceof BaseVehicle
                    && IsoUtils.DistanceTo(
                            source.getCharacter().getX(), source.getCharacter().getY(), destination.getParent().getX(), destination.getParent().getY()
                        )
                        > 5.0) {
                    return 1;
                }
            }

            float destinationWeightInTransaction = 0.0F;

            for (Transaction transaction : transactions) {
                if (itemId != -1) {
                    if (destination != null && destination.getCharacter() != null) {
                        if (itemId == transaction.itemId && extra == null
                            || source != null && source.id == transaction.itemId
                            || transaction.sourceId.getContainer() == null
                            || itemId == transaction.sourceId.getContainer().id
                            || itemId == transaction.destinationId.getContainer().id) {
                            DebugLog.Objects.noise("Inconsistent: item to inventory (%s) t=(%s)", description, transaction);
                            return 1;
                        }
                    } else if (destination != null
                        && (
                            itemId == transaction.itemId
                                || destination.id == transaction.itemId && transaction.itemId != -1
                                || transaction.sourceId.isContainerTheSame(itemId, destination) && transaction.itemId == -1
                        )) {
                        DebugLog.Objects.noise("Inconsistent: item from inventory (%s) t=(%s)", description, transaction);
                        return 1;
                    }
                } else if (destination.getCharacter() != null
                    && source != null
                    && (transaction.destinationId.isContainerTheSame(itemId, source) || transaction.sourceId.isContainerTheSame(itemId, source))) {
                    DebugLog.Objects.noise("Inconsistent: object to inventory (%s) t=(%s)", description, transaction);
                    return 1;
                }

                if (transaction.itemId != -1
                    && transaction.sourceId.getContainer() != null
                    && transaction.destinationId.getContainer() != null
                    && transaction.destinationId.isContainerTheSame(itemId, destination)
                    && transaction.sourceId.getContainer().containsID(transaction.itemId)) {
                    InventoryItem item = transaction.sourceId.getContainer().getItemWithID(transaction.itemId);
                    if (item != null) {
                        destinationWeightInTransaction += item.getUnequippedWeight();
                    }
                }
            }

            if (itemId != -1 && source != null) {
                InventoryItem item = source.getItemWithID(itemId);
                if (destination != null && "floor".equals(destination.getType())) {
                    IsoGameCharacter chr = (IsoGameCharacter)(source.getCharacter() != null ? source.getCharacter() : player);
                    IsoGridSquare dropSquare = getNotFullFloorSquare(chr, item, destination);
                    if (dropSquare == null) {
                        DebugLog.Objects.warn("Inconsistent: destination square does not contain enough space for the item (%s)", description);
                        return 1;
                    }

                    if (dropTransaction != null) {
                        dropTransaction.square = dropSquare;
                    }

                    return 0;
                }

                if (source == destination
                    && source.getParent() instanceof IsoPlayer
                    && item != null
                    && item.getClothingItemExtraOption() != null
                    && !item.getClothingItemExtraOption().isEmpty()
                    && !StringUtils.isNullOrEmpty(extra)) {
                    return 0;
                }

                if (item == null) {
                    DebugLog.Multiplayer.error("Transaction failed: item is null");
                    return 1;
                }

                if (destination != null
                    && destination.getParent() instanceof BaseVehicle
                    && !destination.hasRoomFor(player, item.getUnequippedWeight() + destinationWeightInTransaction)) {
                    DebugLog.Objects.noise("Inconsistent: destination container is not contain enough space for the item (%s)", description);
                    return 1;
                }

                if (destination != null
                    && !(destination.getParent() instanceof IsoGameCharacter)
                    && !(destination.getParent() instanceof BaseVehicle)
                    && destination.getEffectiveCapacity(player) < destination.getCapacityWeight() + item.getUnequippedWeight() + destinationWeightInTransaction
                    )
                 {
                    DebugLog.Objects.noise("Inconsistent: destination container is not contain enough space for the item (%s)", description);
                    return 1;
                }
            }

            if (worldItem != null && destination != null && !destination.isItemAllowed(worldItem)) {
                DebugLog.Objects.noise("Inconsistent: destination container can't contain the item (%s)", description);
                return 1;
            } else if (worldItem != null
                && destination != null
                && destination.getParent() instanceof BaseVehicle
                && !destination.hasRoomFor(player, worldItem.getUnequippedWeight() + destinationWeightInTransaction)) {
                DebugLog.Objects.noise("Inconsistent: destination container is not contain enough space for the item (%s)", description);
                return 1;
            } else if (worldItem != null
                && destination != null
                && !(destination.getParent() instanceof IsoGameCharacter)
                && !(destination.getParent() instanceof BaseVehicle)
                && destination.getEffectiveCapacity(player)
                    < destination.getCapacityWeight() + worldItem.getUnequippedWeight() + destinationWeightInTransaction) {
                DebugLog.Objects.noise("Inconsistent: destination container is not contain enough space for the item (%s)", description);
                return 1;
            } else {
                return 0;
            }
        } else {
            DebugLog.Objects.noise("Inconsistent: destination container can't be found (%s)", description);
            return 1;
        }
    }

    private static boolean floorHasRoomFor(IsoGridSquare square, IsoGameCharacter character, InventoryItem item, ItemContainer destContainer) {
        float capacity = destContainer.getEffectiveCapacity(character);
        float totalWeight = square.getTotalWeightOfItemsOnFloor();
        if (totalWeight >= capacity) {
            return false;
        } else if (transactions.stream().anyMatch(t -> t.square == square)) {
            return false;
        } else {
            return ItemContainer.floatingPointCorrection(totalWeight) + item.getUnequippedWeight() <= capacity ? true : item.getUnequippedWeight() >= capacity;
        }
    }

    private static boolean canDropOnFloor(IsoGridSquare square, IsoGameCharacter character) {
        if (square == null) {
            return false;
        } else if (!square.TreatAsSolidFloor()) {
            return false;
        } else if (!square.isSolid() && !square.isSolidTrans()) {
            IsoGridSquare current = character.getCurrentSquare();
            if (current != null && square != current) {
                if (current.isBlockedTo(square) || current.isWindowTo(square)) {
                    return false;
                }

                if (current.HasStairs() != square.HasStairs()) {
                    return false;
                }

                if (current.HasStairs() && !current.isSameStaircase(square.getX(), square.getY(), square.getZ())) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static IsoGridSquare getNotFullFloorSquare(IsoGameCharacter character, InventoryItem item, ItemContainer destContainer) {
        IsoGridSquare square = character.getCurrentSquare();
        if (canDropOnFloor(square, character) && floorHasRoomFor(square, character, item, destContainer)) {
            return square;
        } else {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0) {
                        square = IsoWorld.instance
                            .getCell()
                            .getGridSquare((double)(character.getX() + dx), (double)(character.getY() + dy), (double)character.getZ());
                        if (canDropOnFloor(square, character) && floorHasRoomFor(square, character, item, destContainer)) {
                            return square;
                        }
                    }
                }
            }

            return null;
        }
    }

    public static boolean isRejected(byte id) {
        return transactions.stream().filter(r -> id == r.id).allMatch(r -> r.state == Transaction.TransactionState.Reject);
    }

    public static boolean isDone(byte id) {
        return transactions.stream().filter(r -> id == r.id).allMatch(r -> r.state == Transaction.TransactionState.Done);
    }

    public static void cancelAllRelevantToUser(IsoPlayer player) {
        for (Transaction transaction : transactions) {
            if (transaction.sourceId.containerType == ContainerID.ContainerType.PlayerInventory && transaction.sourceId.playerId.getID() == player.onlineId
                || transaction.destinationId.containerType == ContainerID.ContainerType.PlayerInventory
                    && transaction.destinationId.playerId.getID() == player.onlineId) {
                transaction.setState(Transaction.TransactionState.Reject);
            }
        }
    }

    public static int getDuration(byte id) {
        Optional<Transaction> res = transactions.stream().filter(r -> id == r.id).findFirst();
        if (res.isEmpty()) {
            return -1;
        } else {
            Transaction t = res.get();
            return (int)(t.endTime - t.startTime);
        }
    }

    public static byte createItemTransaction(IsoPlayer player, InventoryItem item, ItemContainer source, ItemContainer destination) {
        return createItemTransaction(player, item, source, destination, IsoDirections.N, 0.0F, 0.0F, 0.0F);
    }

    public static byte createItemTransaction(
        IsoPlayer player, InventoryItem item, ItemContainer source, ItemContainer destination, IsoDirections direction, float posX, float posY, float posZ
    ) {
        int itemId = -1;
        if (item != null) {
            itemId = item.id;
        }

        if (source.getType().equals("floor") && item.getWorldItem() != null) {
            itemId = -1;
        }

        if (isConsistent(itemId, item, source, destination, null, null, player) == 0) {
            ItemTransactionPacket transaction = new ItemTransactionPacket();

            try {
                transaction.set(player, item, source, destination, null, direction, posX, posY, posZ);
                ByteBufferWriter bbw2 = GameClient.connection.startPacket();
                PacketTypes.PacketType.ItemTransaction.doPacket(bbw2);
                transaction.write(bbw2);
                PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
                add(transaction);
            } catch (Exception var11) {
                DebugLog.General
                    .printException(
                        var11,
                        "createItemTransaction: error while creating package. player="
                            + player
                            + " item="
                            + item
                            + " source="
                            + source
                            + " destination="
                            + destination
                            + " direction="
                            + direction
                            + " posX="
                            + posX
                            + " posY="
                            + posY
                            + " posZ="
                            + posZ,
                        LogSeverity.Error
                    );
                return 0;
            }

            return transaction.id;
        } else {
            return 0;
        }
    }

    public static byte changeItemTypeTransaction(IsoPlayer player, InventoryItem item, ItemContainer source, ItemContainer destination, String extra) {
        if (isConsistent(item.id, item, source, destination, extra, null, player) == 0) {
            ItemTransactionPacket transaction = new ItemTransactionPacket();
            transaction.set(player, item, source, destination, extra, null, 0.0F, 0.0F, 0.0F);
            ByteBufferWriter bbw2 = GameClient.connection.startPacket();
            PacketTypes.PacketType.ItemTransaction.doPacket(bbw2);
            transaction.write(bbw2);
            PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
            add(transaction);
            return transaction.id;
        } else {
            return 0;
        }
    }

    public static void removeItemTransaction(byte id, boolean isCanceled) {
        if (GameClient.client) {
            if (id != 0) {
                if (isCanceled) {
                    ItemTransactionPacket transaction = new ItemTransactionPacket();
                    transaction.id = id;
                    transaction.setState(Transaction.TransactionState.Reject);
                    ByteBufferWriter bbw2 = GameClient.connection.startPacket();
                    PacketTypes.PacketType.ItemTransaction.doPacket(bbw2);
                    transaction.write(bbw2);
                    PacketTypes.PacketType.ItemTransaction.send(GameClient.connection);
                }

                transactions.removeIf(r -> r.id == id);
            }
        } else if (GameServer.server) {
            transactions.removeIf(r -> r.id == id);
        }
    }

    public void setStateFromPacket(ItemTransactionPacket packet) {
        for (Transaction transaction : transactions) {
            if (packet.id == transaction.id) {
                transaction.setState(packet.state);
                transaction.setDuration(packet.duration);
                break;
            }
        }
    }

    public static class LightweightData {
        public String lastItemFullType = "";
        public int itemsCount;
        public long itemsLastTransactionTime;

        public LightweightData() {
            this.lastItemFullType = "";
            this.itemsCount = 0;
            this.itemsLastTransactionTime = 0L;
        }
    }
}
