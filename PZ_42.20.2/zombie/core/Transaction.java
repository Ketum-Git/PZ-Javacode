// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.IDescriptor;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.CharacterTrait;
import zombie.util.StringUtils;

public abstract class Transaction implements IDescriptor {
    protected static byte lastId;
    @JSONField
    protected byte id;
    @JSONField
    protected Transaction.TransactionState state;
    @JSONField
    protected final List<Transaction.TransactionEntry> entries = new ArrayList<>();
    @JSONField
    protected final PlayerID playerId = new PlayerID();
    @JSONField
    protected String extra;
    @JSONField
    protected IsoDirections direction;
    protected float xoff;
    protected float yoff;
    protected float zoff;
    @JSONField
    protected long startTime;
    @JSONField
    protected long endTime;
    public IsoGridSquare square;

    public void set(
        IsoPlayer player,
        List<InventoryItem> items,
        ItemContainer source,
        ItemContainer destination,
        String extra,
        IsoDirections direction,
        float posX,
        float posY,
        float posZ
    ) {
        this.state = GameServer.server ? Transaction.TransactionState.Accept : Transaction.TransactionState.Request;
        this.playerId.set(player);
        this.entries.clear();

        for (InventoryItem item : items) {
            Transaction.TransactionEntry entry = new Transaction.TransactionEntry(TransactionManager.getItemId(item, source));
            if (source.getType().equals("object")) {
                entry.sourceId.set(source, source.getParent());
            } else if (source.getType().equals("floor")) {
                if (item.getWorldItem() != null) {
                    entry.sourceId.set(source, item.getWorldItem());
                } else {
                    entry.sourceId.set(source, item.deadBodyObject);
                }
            } else {
                entry.sourceId.set(source);
            }

            if (destination == null) {
                entry.destinationId.set(null);
            } else if (destination.getType().equals("object")) {
                if (source != null && destination.getParent() == source.getParent()) {
                    entry.destinationId.set(destination, destination.getParent());
                } else {
                    entry.destinationId.setObject(destination, destination.getParent(), destination.sourceGrid);
                }

                this.direction = direction;
                this.xoff = posX;
                this.yoff = posY;
                this.zoff = posZ;
            } else if (destination.getType().equals("floor")) {
                entry.destinationId.setFloor(destination, player.getCurrentSquare());
            } else {
                entry.destinationId.set(destination);
            }

            this.entries.add(entry);
        }

        this.extra = extra;
        this.startTime = GameTime.getServerTimeMills();
        this.endTime = GameServer.server ? this.startTime + (long)this.getDuration() : this.startTime;
        if (lastId == 0) {
            lastId++;
        }

        this.id = lastId++;
    }

    public void setTimeData() {
        this.startTime = GameTime.getServerTimeMills();
        this.endTime = this.startTime + (long)this.getDuration();
    }

    public boolean update() {
        for (Transaction.TransactionEntry entry : this.entries) {
            if (!this.updateItem(entry)) {
                return false;
            }
        }

        return true;
    }

    private boolean updateItem(Transaction.TransactionEntry entry) {
        entry.sourceId.findObject();
        entry.destinationId.findObject();
        if (entry.sourceId.containerType == ContainerID.ContainerType.IsoObject) {
            if (entry.destinationId.containerType == ContainerID.ContainerType.Undefined) {
                LuaEventManager.triggerEvent(
                    "OnProcessTransaction", "scrapMoveable", this.playerId.getPlayer(), null, entry.sourceId, entry.destinationId, null
                );
                return true;
            } else if (entry.sourceId.hashCode() != entry.destinationId.hashCode()) {
                LuaEventManager.triggerEvent(
                    "OnProcessTransaction", "pickUpMoveable", this.playerId.getPlayer(), null, entry.sourceId, entry.destinationId, null
                );
                return true;
            } else {
                KahluaTable tbl = LuaManager.platform.newTable();
                tbl.rawset("direction", this.direction.name());
                LuaEventManager.triggerEvent(
                    "OnProcessTransaction", "rotateMoveable", this.playerId.getPlayer(), null, entry.sourceId, entry.destinationId, tbl
                );
                return true;
            }
        } else {
            InventoryItem item;
            if (entry.itemId == -1 && entry.sourceId.containerType == ContainerID.ContainerType.DeadBody) {
                item = ((IsoDeadBody)entry.sourceId.getObject()).getItem();
            } else {
                ItemContainer player = entry.sourceId.getContainer();
                if (!(player instanceof ItemContainer)) {
                    return false;
                }

                item = player.getItemWithID(entry.itemId);
                player.setExplored(true);
                player.setHasBeenLooted(true);
            }

            if (item != null) {
                boolean isReplacedOnCooked = false;
                if (item instanceof Food food) {
                    food.setChef(this.playerId.getPlayer().getUsername());
                    isReplacedOnCooked = food.getReplaceOnCooked() != null;
                }

                if (item instanceof DrainableComboItem drainableComboItem) {
                    isReplacedOnCooked = drainableComboItem.getReplaceOnCooked() != null;
                }

                if (!isReplacedOnCooked) {
                    item.update();
                }

                if (item instanceof Clothing clothing) {
                    clothing.updateWetness();
                }
            }

            if (entry.destinationId.containerType == ContainerID.ContainerType.IsoObject) {
                KahluaTable tbl = LuaManager.platform.newTable();
                tbl.rawset("direction", this.direction.name());
                LuaEventManager.triggerEvent("OnProcessTransaction", "placeMoveable", this.playerId.getPlayer(), item, entry.sourceId, entry.destinationId, tbl);
                return true;
            } else if (ContainerID.ContainerType.Floor == entry.destinationId.containerType
                && ContainerID.ContainerType.PlayerInventory == entry.sourceId.containerType) {
                IsoPlayer player = this.playerId.getPlayer();
                player.removeAttachedItem(item);
                if (player.isEquipped(item)) {
                    player.removeFromHands(item);
                    player.removeWornItem(item, false);
                    LuaEventManager.triggerEvent("OnClothingUpdated", player);
                    player.updateHandEquips();
                }

                KahluaTable tbl = LuaManager.platform.newTable();
                tbl.rawset("square", this.square);
                LuaEventManager.triggerEvent("OnProcessTransaction", "dropOnFloor", this.playerId.getPlayer(), item, entry.sourceId, entry.destinationId, tbl);
                return true;
            } else if (!StringUtils.isNullOrEmpty(this.extra)) {
                int newItemId = entry.itemId;
                if (item != null
                    && entry.sourceId.containerType == ContainerID.ContainerType.PlayerInventory
                    && entry.sourceId.getContainer() == entry.destinationId.getContainer()) {
                    DebugType.Objects.noise("Extra option for \"%s\"", this.extra);
                    IsoPlayer player = this.playerId.getPlayer();
                    if (player != null) {
                        player.removeFromHands(item);
                        player.removeWornItem(item, false);
                        player.getInventory().Remove(item);
                        InventoryItem newItem = InventoryItemFactory.CreateItem(item, this.extra);
                        player.getInventory().AddItem(newItem);
                        if (newItem instanceof InventoryContainer && newItem.canBeEquipped() != null) {
                            player.setWornItem(newItem.canBeEquipped(), newItem);
                        } else if (newItem.IsClothing()) {
                            player.setWornItem(newItem.getBodyLocation(), newItem);
                        }

                        newItemId = newItem.id;
                        GameServer.sendRemoveItemFromContainer(entry.sourceId.getContainer(), item);
                        GameServer.sendAddItemToContainer(entry.destinationId.getContainer(), newItem);
                        player.updateHandEquips();
                        GameServer.sendSyncClothing(player, newItem.getBodyLocation(), newItem);
                    }
                }

                return entry.destinationId.getContainer().getItemWithID(newItemId) != null && entry.sourceId.getContainer().getItemWithID(entry.itemId) == null;
            } else {
                if (entry.sourceId.containerType == ContainerID.ContainerType.WorldObject && entry.itemId == -1) {
                    if (entry.sourceId.getObject() == null) {
                        return false;
                    }

                    item = ((IsoWorldInventoryObject)entry.sourceId.getObject()).getItem();
                }

                if (entry.destinationId.getContainer() == null) {
                    return false;
                } else if (entry.sourceId.containerType == ContainerID.ContainerType.Vehicle
                    && IsoUtils.DistanceTo(
                            this.playerId.getPlayer().getX(),
                            this.playerId.getPlayer().getY(),
                            entry.sourceId.getVehicle().getX(),
                            entry.sourceId.getVehicle().getY()
                        )
                        > 5.0) {
                    return false;
                } else if (entry.destinationId.containerType == ContainerID.ContainerType.Vehicle
                    && IsoUtils.DistanceTo(
                            this.playerId.getPlayer().getX(),
                            this.playerId.getPlayer().getY(),
                            entry.destinationId.getVehicle().getX(),
                            entry.destinationId.getVehicle().getY()
                        )
                        > 5.0) {
                    return false;
                } else if (item == null) {
                    return false;
                } else {
                    if (entry.sourceId.containerType == ContainerID.ContainerType.WorldObject && entry.itemId == -1) {
                        if (item instanceof Clothing clothing) {
                            clothing.flushWetness();
                        }

                        GameServer.RemoveItemFromMap(entry.sourceId.getObject());
                    } else if (entry.itemId == -1 && entry.sourceId.containerType == ContainerID.ContainerType.DeadBody) {
                        INetworkPacket.sendToAll(PacketTypes.PacketType.RemoveCorpseFromMap, entry.sourceId.getObject());
                        entry.sourceId.getObject().getSquare().removeCorpse((IsoDeadBody)entry.sourceId.getObject(), true);
                    } else {
                        IsoPlayer player = this.playerId.getPlayer();
                        player.removeAttachedItem(item);
                        if (player.isEquipped(item)) {
                            player.removeFromHands(item);
                            player.removeWornItem(item, false);
                            LuaEventManager.triggerEvent("OnClothingUpdated", player);
                            player.updateHandEquips();
                            GameServer.sendSyncClothing(player, null, item);
                        }

                        if (entry.sourceId.getContainer().getCharacter() instanceof IsoPlayer) {
                            INetworkPacket.send(
                                (IsoPlayer)entry.sourceId.getContainer().getCharacter(),
                                PacketTypes.PacketType.RemoveInventoryItemFromContainer,
                                entry.sourceId.getContainer(),
                                item
                            );
                        } else {
                            INetworkPacket.sendToRelative(
                                PacketTypes.PacketType.RemoveInventoryItemFromContainer,
                                entry.sourceId.x,
                                entry.sourceId.y,
                                entry.sourceId.getContainer(),
                                item
                            );
                        }
                    }

                    entry.sourceId.getContainer().Remove(item);
                    if (!TransactionManager.getLightweightData(this.playerId.getPlayer()).lastItemFullType.equals(item.getFullType())) {
                        TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsCount = 0;
                    }

                    TransactionManager.getLightweightData(this.playerId.getPlayer()).lastItemFullType = item.getFullType() == null ? "" : item.getFullType();
                    TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsLastTransactionTime = System.currentTimeMillis();
                    entry.destinationId.getContainer().addItem(item);
                    if (entry.destinationId.containerType == ContainerID.ContainerType.Floor) {
                        IsoWorldInventoryObject worldInvObj = (IsoWorldInventoryObject)entry.destinationId.getObject();
                        worldInvObj.square.AddWorldInventoryItem(item, worldInvObj.xoff, worldInvObj.yoff, worldInvObj.zoff, true);
                    } else {
                        if (entry.sourceId.containerType == ContainerID.ContainerType.WorldObject
                            || entry.sourceId.containerType == ContainerID.ContainerType.Floor) {
                            item.setWorldItem(null);
                            entry.sourceId.set(null);
                        }

                        if (entry.destinationId.getContainer().getCharacter() instanceof IsoPlayer) {
                            INetworkPacket.send(
                                (IsoPlayer)entry.destinationId.getContainer().getCharacter(),
                                PacketTypes.PacketType.AddInventoryItemToContainer,
                                entry.destinationId.getContainer(),
                                item
                            );
                        } else {
                            INetworkPacket.sendToRelative(
                                PacketTypes.PacketType.AddInventoryItemToContainer,
                                entry.destinationId.x,
                                entry.destinationId.y,
                                entry.destinationId.getContainer(),
                                item
                            );
                        }
                    }

                    if (entry.sourceId.containerType == ContainerID.ContainerType.DeadBody && entry.itemId == -1) {
                        if (entry.destinationId.containerType == ContainerID.ContainerType.PlayerInventory) {
                            IsoPlayer playerx = this.playerId.getPlayer();
                            playerx.setPrimaryHandItem(item);
                            playerx.setSecondaryHandItem(item);
                            playerx.updateHandEquips();
                        } else {
                            DebugType.Objects.error("A player " + this.playerId.getDescription() + " sent invalid transaction " + this.getDuration());
                        }
                    }

                    return entry.sourceId.containerType != ContainerID.ContainerType.Floor
                        || !(IsoUtils.DistanceTo(entry.sourceId.x, entry.sourceId.y, this.playerId.getPlayer().getX(), this.playerId.getPlayer().getY()) > 1.1);
                }
            }
        }
    }

    private float getDuration() {
        float totalMaxTime = 0.0F;

        for (Transaction.TransactionEntry entry : this.entries) {
            ItemContainer source = entry.sourceId.getContainer();
            ItemContainer destination = entry.destinationId.getContainer();
            IsoPlayer player = this.playerId.getPlayer();
            float maxTime;
            if (source != null && destination != null && player != null) {
                InventoryItem item = entry.sourceId.getContainer().getItemWithID(entry.itemId);
                if (!destination.getType().equals("TradeUI") && !source.getType().equals("TradeUI")) {
                    maxTime = 120.0F;
                    float destCapacityDelta = 1.0F;
                    if (source == player.getInventory()) {
                        if (destination.isInCharacterInventory(player)) {
                            destCapacityDelta = destination.getCapacityWeight() / destination.getMaxWeight();
                        } else {
                            maxTime = 50.0F;
                        }
                    } else if (!source.isInCharacterInventory(player) && destination.isInCharacterInventory(player)) {
                        maxTime = 50.0F;
                    }

                    if (destCapacityDelta < 0.4F) {
                        destCapacityDelta = 0.4F;
                    }

                    if (item != null) {
                        float w = item.getActualWeight();
                        if (w > 3.0F) {
                            w = 3.0F;
                        }

                        maxTime = maxTime * w * destCapacityDelta;
                    }

                    if (Core.getInstance().getGameMode().equals("LastStand")) {
                        maxTime *= 0.3F;
                    }

                    if (destination.getType().equals("floor")) {
                        if (source == player.getInventory()) {
                            maxTime *= 0.1F;
                        } else if (!source.isInCharacterInventory(player)) {
                            maxTime *= 0.2F;
                        }
                    }

                    if (player.hasTrait(CharacterTrait.DEXTROUS)) {
                        maxTime *= 0.5F;
                    }

                    if (player.hasTrait(CharacterTrait.ALL_THUMBS) || player.isWearingAwkwardGloves()) {
                        maxTime *= 2.0F;
                    }
                } else {
                    maxTime = 0.0F;
                }
            } else {
                maxTime = 50.0F;
            }

            InventoryItem item = null;
            if (entry.sourceId.containerType == ContainerID.ContainerType.WorldObject && entry.itemId == -1 && entry.sourceId.getObject() != null) {
                item = ((IsoWorldInventoryObject)entry.sourceId.getObject()).getItem();
            }

            if (item != null
                && item.getWeight() <= 0.1F
                && (
                    TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsLastTransactionTime == 0L
                        || System.currentTimeMillis() - TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsLastTransactionTime < 2000L
                )) {
                if (TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsCount < 19
                    && TransactionManager.getLightweightData(this.playerId.getPlayer()).lastItemFullType.equals(item.getFullType())) {
                    maxTime = 0.0F;
                    TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsCount++;
                } else {
                    TransactionManager.getLightweightData(this.playerId.getPlayer()).itemsCount = 0;
                    TransactionManager.getLightweightData(this.playerId.getPlayer()).lastItemFullType = "";
                }
            }

            if (entry.sourceId.getContainer() == entry.destinationId.getContainer()
                && ContainerID.ContainerType.PlayerInventory == entry.sourceId.containerType
                && entry.itemId != -1
                && !StringUtils.isNullOrEmpty(this.extra)) {
                maxTime = 0.0F;
            }

            totalMaxTime = Math.max(maxTime, totalMaxTime);
        }

        return totalMaxTime * 20.0F;
    }

    public void setState(Transaction.TransactionState state) {
        this.state = state;
        DebugType.Objects.noise(this);
    }

    public void setDuration(long duration) {
        this.endTime = this.startTime + duration;
    }

    @Override
    public String toString() {
        return this.getDescription();
    }

    protected static class TransactionEntry {
        public final Integer itemId;
        public final ContainerID sourceId = new ContainerID();
        public final ContainerID destinationId = new ContainerID();

        public TransactionEntry(Integer itemId) {
            this.itemId = itemId;
        }
    }

    public static enum TransactionState {
        Reject,
        Request,
        Accept,
        Done;
    }
}
