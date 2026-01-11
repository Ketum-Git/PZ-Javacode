// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;

public class Variables implements INetworkPacketField {
    @JSONField
    protected final HashMap<String, String> variables = new HashMap<>();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        int size = b.getInt();

        for (int i = 0; i < size; i++) {
            String key = GameWindow.ReadString(b);
            String value = GameWindow.ReadString(b);
            this.variables.put(key, value);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        int size = this.variables.size();
        b.putInt(size);

        for (Entry<String, String> variable : this.variables.entrySet()) {
            GameWindow.WriteString(b, variable.getKey());
            GameWindow.WriteString(b, variable.getValue());
        }
    }

    public void clear() {
        this.variables.clear();
    }

    public HashMap<String, String> get() {
        return this.variables;
    }
}
