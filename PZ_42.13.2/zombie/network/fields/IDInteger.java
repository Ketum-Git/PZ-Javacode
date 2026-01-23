// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public abstract class IDInteger implements INetworkPacketField {
    protected int id;

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.id);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.id != -1;
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return this.id;
    }
}
