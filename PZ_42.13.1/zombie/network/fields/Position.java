// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;

public class Position implements IPositional, INetworkPacketField {
    @JSONField
    protected float x;
    @JSONField
    protected float y;
    @JSONField
    protected float z;

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }

    @Override
    public float getZ() {
        return this.z;
    }

    public void copy(Position position) {
        this.set(position.x, position.y, position.z);
    }
}
