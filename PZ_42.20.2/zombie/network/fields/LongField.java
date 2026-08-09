// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.packets.INetworkPacket;

public class LongField implements INetworkPacket {
    @JSONField
    private long value;

    @Override
    public void setData(Object... values) {
        this.value = (Long)values[0];
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.value = b.getLong();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putLong(this.value);
    }

    public long getValue() {
        return this.value;
    }
}
