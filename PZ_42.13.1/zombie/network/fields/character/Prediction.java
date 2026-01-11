// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.Vector3;
import zombie.network.JSONField;
import zombie.network.packets.INetworkPacket;

public class Prediction implements INetworkPacket {
    @JSONField
    public byte type = 0;
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public byte z;
    @JSONField
    public float direction;
    @JSONField
    public byte distance;
    public final Vector3 position = new Vector3();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.type = b.get();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.get();
        this.direction = b.getFloat();
        this.distance = b.get();
        this.position.set(this.distance * (float)Math.cos(this.direction) + this.x, this.distance * (float)Math.sin(this.direction) + this.y, this.z);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.type);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.put(this.z);
        b.putFloat(this.direction);
        b.put(this.distance);
    }

    public void copy(Prediction other) {
        this.type = other.type;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.direction = other.direction;
        this.distance = other.distance;
    }
}
