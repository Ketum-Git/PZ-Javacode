// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoWindow;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.MovingObject;
import zombie.network.fields.NetObject;
import zombie.vehicles.BaseVehicle;

public class HitInfo implements INetworkPacketField {
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public float z;
    @JSONField
    public float dot;
    @JSONField
    public float distSq;
    @JSONField
    public int chance;
    @JSONField
    public MovingObject object = new MovingObject();
    @JSONField
    public NetObject window = new NetObject();

    public HitInfo init(IsoMovingObject obj, float dot, float distSq, float x, float y, float z) {
        this.object = new MovingObject();
        this.window = new NetObject();
        this.object.set(obj);
        this.window.setObject(null);
        this.x = x;
        this.y = y;
        this.z = z;
        this.dot = dot;
        this.distSq = distSq;
        return this;
    }

    public HitInfo init(IsoWindow obj, float dot, float distSq) {
        this.object = new MovingObject();
        this.window = new NetObject();
        this.object.set(null);
        this.window.setObject(obj);
        Vector2 pos = obj.getFacingPosition(BaseVehicle.allocVector2());
        this.x = pos.getX();
        this.y = pos.getY();
        BaseVehicle.releaseVector2(pos);
        this.z = obj.getZ();
        this.dot = dot;
        this.distSq = distSq;
        return this;
    }

    public HitInfo init(HitInfo other) {
        this.object = new MovingObject();
        this.window = new NetObject();
        this.object.set(other.object.getObject());
        this.window.setObject(other.window.getObject());
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.dot = other.dot;
        this.distSq = other.distSq;
        return this;
    }

    public IsoMovingObject getObject() {
        return (IsoMovingObject)this.object.getObject();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.object.parse(b, connection);
        this.window.parse(b, connection);
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
        this.dot = b.getFloat();
        this.distSq = b.getFloat();
        this.chance = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.object.write(b);
        this.window.write(b);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
        b.putFloat(this.dot);
        b.putFloat(this.distSq);
        b.putInt(this.chance);
    }

    @Override
    public int getPacketSizeBytes() {
        return 24 + this.object.getPacketSizeBytes() + this.window.getPacketSizeBytes();
    }
}
