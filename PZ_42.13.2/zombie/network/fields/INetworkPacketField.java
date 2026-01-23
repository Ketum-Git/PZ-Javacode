// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.IDescriptor;

public interface INetworkPacketField extends IDescriptor {
    void parse(ByteBuffer arg0, UdpConnection arg1);

    void write(ByteBufferWriter arg0);

    default int getPacketSizeBytes() {
        return 0;
    }

    default boolean isConsistent(UdpConnection connection) {
        return true;
    }
}
