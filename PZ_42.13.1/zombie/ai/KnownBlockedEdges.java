// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayList;
import zombie.GameWindow;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;

public final class KnownBlockedEdges {
    public int x;
    public int y;
    public int z;
    public boolean w;
    public boolean n;
    static final ObjectPool<KnownBlockedEdges> pool = new ObjectPool<>(KnownBlockedEdges::new);

    public KnownBlockedEdges init(KnownBlockedEdges other) {
        return this.init(other.x, other.y, other.z, other.w, other.n);
    }

    public KnownBlockedEdges init(int x, int y, int z) {
        return this.init(x, y, z, false, false);
    }

    public KnownBlockedEdges init(int x, int y, int z, boolean w, boolean n) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        this.n = n;
        return this;
    }

    public boolean isBlocked(int otherX, int otherY) {
        return this.x > otherX && this.w ? true : this.y > otherY && this.n;
    }

    public static KnownBlockedEdges alloc() {
        assert GameServer.server || Thread.currentThread() == GameWindow.gameThread;

        return pool.alloc();
    }

    public static void releaseAll(ArrayList<KnownBlockedEdges> objs) {
        assert GameServer.server || Thread.currentThread() == GameWindow.gameThread;

        pool.release(objs);
    }

    public void release() {
        assert GameServer.server || Thread.currentThread() == GameWindow.gameThread;

        pool.release(this);
    }
}
