// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.nio.ByteBuffer;

public abstract class UdpEngine {
    public abstract void Send(ByteBuffer bb);

    public abstract void Receive(ByteBuffer bb);
}
