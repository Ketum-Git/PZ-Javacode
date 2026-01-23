// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.network.PacketsCache;

public final class UdpConnection extends PacketsCache {
    final UdpEngine engine;
    private final Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    public final IsoPlayer[] players;

    public UdpConnection(UdpEngine engine) {
        this.players = IsoPlayer.players;
        this.engine = engine;
    }

    public boolean ReleventTo(float x, float y) {
        return true;
    }

    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bb.clear();
        return this.bbw;
    }

    public void endPacketImmediate() {
        this.bb.flip();
        this.engine.Send(this.bb);
        this.bufferLock.unlock();
    }

    public void cancelPacket() {
        this.bufferLock.unlock();
    }
}
