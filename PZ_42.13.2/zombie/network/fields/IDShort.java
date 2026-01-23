// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;

public abstract class IDShort implements INetworkPacketField {
    @JSONField
    private short id;

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.getShort();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.id);
    }

    public void write(ByteBuffer bb) {
        bb.putShort(this.id);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.id != -1;
    }

    public void setID(short id) {
        this.id = id;
    }

    public short getID() {
        return this.id;
    }
}
