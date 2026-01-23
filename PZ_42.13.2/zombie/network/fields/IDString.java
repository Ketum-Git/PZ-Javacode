// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;

public class IDString implements INetworkPacketField {
    @JSONField
    protected String id;

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.id != null;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.id);
    }

    public void write(ByteBuffer b) {
        GameWindow.WriteString(b, this.id);
    }

    public void set(String ID) {
        this.id = ID;
    }

    public String get() {
        return this.id;
    }
}
