// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoThumpable;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncThumpablePacket implements INetworkPacket {
    @JSONField
    NetObject netObject = new NetObject();
    @JSONField
    int lockedByCode;
    @JSONField
    boolean lockedByPadlock;
    @JSONField
    int keyId;

    @Override
    public void setData(Object... values) {
        IsoThumpable obj = (IsoThumpable)values[0];
        this.netObject.setObject(obj);
        this.lockedByCode = obj.getLockedByCode();
        this.lockedByPadlock = obj.isLockedByPadlock();
        this.keyId = obj.getKeyId();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.netObject.write(b);
        b.putInt(this.lockedByCode);
        b.putByte((byte)(this.lockedByPadlock ? 1 : 0));
        b.putInt(this.keyId);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.netObject.parse(b, connection);
        this.lockedByCode = b.getInt();
        this.lockedByPadlock = b.get() != 0;
        this.keyId = b.getInt();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoThumpable obj = (IsoThumpable)this.netObject.getObject();
        obj.lockedByCode = this.lockedByCode;
        obj.lockedByPadlock = this.lockedByPadlock;
        obj.keyId = this.keyId;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoThumpable obj = (IsoThumpable)this.netObject.getObject();
        obj.lockedByCode = this.lockedByCode;
        obj.lockedByPadlock = this.lockedByPadlock;
        obj.keyId = this.keyId;
        this.sendToRelativeClients(PacketTypes.PacketType.SyncThumpable, connection, obj.square.getX(), obj.square.getY());
    }
}
