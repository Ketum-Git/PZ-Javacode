// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.network.JSONField;
import zombie.network.NetworkVariables;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.MovingObject;
import zombie.network.fields.character.ZombieID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ThumpPacket implements INetworkPacket {
    @JSONField
    ZombieID zombie = new ZombieID();
    @JSONField
    String type;
    @JSONField
    MovingObject object = new MovingObject();

    @Override
    public void setData(Object... values) {
        this.zombie.set((IsoZombie)values[0]);
        this.type = this.zombie.getZombie().getVariableString("ThumpType");
        this.object.set((IsoObject)this.zombie.getZombie().getThumpTarget());
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.zombie.write(b);
        b.putByte((byte)NetworkVariables.ThumpType.fromString(this.type).ordinal());
        this.object.write(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.zombie.parse(b, connection);
        this.type = NetworkVariables.ThumpType.fromByte(b.get()).toString();
        this.object.parse(b, connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToRelativeClients(PacketTypes.PacketType.Thump, null, this.zombie.getX(), this.zombie.getY());
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.zombie.getZombie() != null) {
            this.zombie.getZombie().setVariable("ThumpType", this.type);
            this.zombie.getZombie().setThumpTarget(this.object.getObject());
        } else {
            DebugLog.log("ThumpPacket processClient zombie.getZombie()==null");
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.zombie.isConsistent(connection);
    }
}
