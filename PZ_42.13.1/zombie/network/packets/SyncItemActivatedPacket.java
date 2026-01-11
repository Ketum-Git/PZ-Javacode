// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncItemActivatedPacket implements INetworkPacket {
    protected final PlayerID playerId = new PlayerID();
    int itemId = -1;
    boolean activated;

    @Override
    public void setData(Object... values) {
        this.playerId.set((IsoPlayer)values[0]);
        this.itemId = (Integer)values[1];
        this.activated = (Boolean)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.itemId);
        b.putByte((byte)(this.activated ? 1 : 0));
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.itemId = b.getInt();
        this.activated = b.get() == 1;
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.playerId.getPlayer() == null) {
            DebugLog.Multiplayer.warn("[SyncItemActivated] not relevant null isoPlayer");
            return false;
        } else if (!connection.RelevantTo(this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y)) {
            DebugLog.Multiplayer
                .noise("[SyncItemActivated] not relevant client isoPlayer[x,y]=" + this.playerId.getPlayer().getX() + "," + this.playerId.getPlayer().getY());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
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
                DebugLog.Multiplayer.warn("[SyncItemActivated] unable to process on server null isoPlayer");
            } else {
                InventoryItem inventoryItem = player.getInventory().getItemWithID(this.itemId);
                if (inventoryItem != null && inventoryItem.canBeActivated()) {
                    inventoryItem.setActivated(this.activated);
                    this.sendToRelativeClients(PacketTypes.PacketType.SyncItemActivated, null, player.getX(), player.getY());
                } else {
                    DebugLog.Multiplayer
                        .noise(
                            "[SyncItemActivated] unable to find item for getPlayerNum()="
                                + player.getPlayerNum()
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
        if (inventoryItem != null && inventoryItem.isActivated() != this.activated) {
            inventoryItem.setActivated(this.activated);
            inventoryItem.playActivateDeactivateSound();
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
