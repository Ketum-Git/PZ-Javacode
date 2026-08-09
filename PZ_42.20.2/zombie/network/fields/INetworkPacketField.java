// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.packets.IDescriptor;

public interface INetworkPacketField extends IDescriptor {
    void parse(ByteBufferReader var1, IConnection var2);

    void write(ByteBufferWriter arg0);

    default int getPacketSizeBytes() {
        return 0;
    }

    default boolean isConsistent(IConnection connection) {
        return true;
    }
}
