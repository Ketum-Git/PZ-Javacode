// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.MovingObject;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class GameCharacterAttachedItemPacket implements INetworkPacket {
    @JSONField
    final MovingObject movingObject = new MovingObject();
    @JSONField
    protected String location = "";
    @JSONField
    protected ContainerID containerId = new ContainerID();
    @JSONField
    protected int itemId;
    @JSONField
    protected InventoryItem item;

    @Override
    public void setData(Object... values) {
        this.movingObject.set((IsoGameCharacter)values[0]);
        this.location = (String)values[1];
        this.item = (InventoryItem)values[2];
        if (this.item != null) {
            this.containerId.set(this.item.getContainer());
            this.itemId = this.item.getID();
        } else {
            this.containerId.set(null);
            this.itemId = -1;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.movingObject.write(b);
        b.putUTF(this.location);
        if (GameClient.client) {
            this.containerId.write(b);
            b.putInt(this.itemId);
        } else if (this.item == null) {
            b.putByte((byte)0);
        } else {
            b.putByte((byte)1);

            try {
                this.item.saveWithSize(b.bb, false);
            } catch (IOException var3) {
                DebugLog.Multiplayer.printException(var3, "itemWriteError", LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.movingObject.parse(b, connection);
        this.location = GameWindow.ReadString(b);
        if (GameServer.server) {
            this.containerId.parse(b, connection);
            this.itemId = b.getInt();
        } else {
            this.item = null;
            boolean hasItem = b.get() == 1;
            if (hasItem) {
                try {
                    this.item = InventoryItem.loadItem(b, 241);
                    IsoPlayer player = this.movingObject.getObject() instanceof IsoPlayer ? (IsoPlayer)this.movingObject.getObject() : null;
                    if (player != null && player.isLocalPlayer()) {
                        this.item = player.getInventory().getItemWithID(this.item.getID());
                    }
                } catch (Exception var5) {
                    DebugLog.General.printException(var5, "", LogSeverity.Error);
                }
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.movingObject.isConsistent(connection) && this.containerId.isConsistent(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoObject obj = this.movingObject.getObject();
        if (!(obj instanceof IsoPlayer isoPlayer && isoPlayer.isLocalPlayer())) {
            if (obj instanceof IsoGameCharacter isoGameCharacter) {
                isoGameCharacter.setAttachedItem(this.location, this.item);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.item = null;
        if (this.containerId.containerType != ContainerID.ContainerType.Undefined) {
            this.item = this.containerId.getContainer().getItemWithID(this.itemId);
            if (this.item == null) {
                DebugLog.Multiplayer.println("PlayerAttachedItemPacket.processServer item can't be found container:" + this.containerId.getDescription());
                return;
            }
        }

        if (this.movingObject.getObject() instanceof IsoGameCharacter isoGameCharacter) {
            isoGameCharacter.setAttachedItem(this.location, this.item);

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    IsoPlayer p2 = GameServer.getAnyPlayerFromConnection(connection);
                    if (p2 != null) {
                        ByteBufferWriter b2 = c.startPacket();
                        PacketTypes.PacketType.GameCharacterAttachedItem.doPacket(b2);
                        this.write(b2);
                        PacketTypes.PacketType.GameCharacterAttachedItem.send(c);
                    }
                }
            }
        }
    }
}
