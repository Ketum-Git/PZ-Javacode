// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;

public class IDString implements INetworkPacketField {
    @JSONField
    protected String id;

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.id != null;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.id = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.id);
    }

    public void write(ByteBuffer b) {
        GameWindow.WriteString(b, this.id);
    }

    public void set(String id) {
        this.id = id;
    }

    public String get() {
        return this.id;
    }
}
