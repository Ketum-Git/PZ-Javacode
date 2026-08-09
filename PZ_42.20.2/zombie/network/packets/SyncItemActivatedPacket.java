// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncItemActivatedPacket implements INetworkPacket {
    protected final PlayerID playerId = new PlayerID();
    int itemId = -1;
    boolean activated;
    int uses;

    @Override
    public void setData(Object... values) {
        this.playerId.set((IsoPlayer)values[0]);
        if (values[1] instanceof InventoryItem item) {
            this.itemId = item.getID();
            this.activated = item.isActivated();
            this.uses = item.getCurrentUses();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.itemId);
        b.putBoolean(this.activated);
        b.putInt(this.uses);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.itemId = b.getInt();
        this.activated = b.getBoolean();
        this.uses = b.getInt();
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.playerId.getPlayer() == null) {
            DebugType.Multiplayer.warn("[SyncItemActivated] not relevant null isoPlayer");
            return false;
        } else if (!connection.isRelevantTo(this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y)) {
            DebugType.Multiplayer
                .noise("[SyncItemActivated] not relevant client isoPlayer[x,y]=" + this.playerId.getPlayer().getX() + "," + this.playerId.getPlayer().getY());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!this.playerId.isConsistent(connection)) {
            return false;
        } else {
            InventoryItem inventoryItem = this.getItem();
            return inventoryItem != null && inventoryItem.canBeActivated();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isRelevant(connection)) {
            IsoPlayer player = this.playerId.getPlayer();
            if (player == null) {
                DebugType.Multiplayer.warn("[SyncItemActivated] unable to process on server null isoPlayer");
            } else {
                InventoryItem inventoryItem = player.getInventory().getItemWithID(this.itemId);
                if (inventoryItem != null && inventoryItem.canBeActivated()) {
                    inventoryItem.setActivated(this.activated);
                    inventoryItem.setCurrentUses(this.uses);
                    this.sendToRelativeClients(PacketTypes.PacketType.SyncItemActivated, null, player.getX(), player.getY());
                } else {
                    DebugType.Multiplayer
                        .noise(
                            "[SyncItemActivated] unable to find item for getPlayerNum()="
                                + player.getIndex()
                                + " getOnlineID()="
                                + player.getOnlineID()
                                + " itemID:"
                                + this.itemId
                        );
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        InventoryItem inventoryItem = this.getItem();
        if (inventoryItem != null) {
            if (inventoryItem.isActivated() != this.activated) {
                inventoryItem.setActivated(this.activated);
                inventoryItem.playActivateDeactivateSound();
            }

            inventoryItem.setCurrentUses(this.uses);
        }
    }

    private InventoryItem getItem() {
        IsoPlayer player = this.playerId.getPlayer();
        if (!GameServer.server && !player.isLocalPlayer()) {
            if (player.getPrimaryHandItem() != null && player.getPrimaryHandItem().getID() == this.itemId) {
                return player.getPrimaryHandItem();
            } else if (player.getSecondaryHandItem() != null && player.getSecondaryHandItem().getID() == this.itemId) {
                return player.getSecondaryHandItem();
            } else {
                for (int i = 0; i < player.getAttachedItems().size(); i++) {
                    InventoryItem item = player.getAttachedItems().getItemByIndex(i);
                    if (item != null && item.getID() == this.itemId) {
                        return item;
                    }
                }

                return null;
            }
        } else {
            return player.getInventory().getItemWithIDRecursiv(this.itemId);
        }
    }
}
