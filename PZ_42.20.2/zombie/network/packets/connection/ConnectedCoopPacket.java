// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ConnectedCoopPacket implements INetworkPacket {
    @JSONField
    protected byte playerIndex;
    @JSONField
    protected boolean accessGranted;
    @JSONField
    protected String reason;

    public void setAccessGranted(byte playerIndex) {
        this.accessGranted = true;
        this.playerIndex = playerIndex;
    }

    public void setAccessDenied(String reason, byte playerIndex) {
        this.accessGranted = false;
        this.playerIndex = playerIndex;
        this.reason = reason;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.accessGranted = b.getBoolean();
        this.playerIndex = b.getByte();
        if (!this.accessGranted) {
            this.reason = b.getUTF();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.accessGranted);
        b.putByte(this.playerIndex);
        if (!this.accessGranted) {
            b.putUTF(this.reason);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.accessGranted) {
            for (int i = 0; i < IsoWorld.instance.addCoopPlayers.size(); i++) {
                IsoWorld.instance.addCoopPlayers.get(i).accessGranted(this.playerIndex);
            }
        } else {
            for (int i = 0; i < IsoWorld.instance.addCoopPlayers.size(); i++) {
                IsoWorld.instance.addCoopPlayers.get(i).accessDenied(this.playerIndex, this.reason);
            }
        }
    }
}
