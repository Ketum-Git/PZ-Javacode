// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RemoveCorpseFromMapPacket implements INetworkPacket {
    protected final ObjectID objectId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    private IsoDeadBody deadBody;

    public void set(IsoDeadBody deadBody) {
        this.objectId.set(deadBody.getObjectID());
        this.deadBody = deadBody;
    }

    @Override
    public void setData(Object... values) {
        this.set((IsoDeadBody)values[0]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.objectId.load(b);
        this.deadBody = (IsoDeadBody)this.objectId.getObject();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.objectId.save(b.bb);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoDeadBody.removeDeadBody(this.objectId);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoDeadBody.removeDeadBody(this.objectId);
    }

    @Override
    public String getDescription() {
        return String.format(this.getClass().getSimpleName() + " id=%s", this.objectId);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.deadBody != null && this.deadBody.getSquare() != null;
    }

    public boolean isRelevant(UdpConnection connection) {
        return connection.RelevantTo(this.deadBody.getX(), this.deadBody.getY());
    }
}
