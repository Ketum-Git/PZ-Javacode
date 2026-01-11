// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.util.ArrayDeque;

public final class ZomboidNetDataPool {
    public static ZomboidNetDataPool instance = new ZomboidNetDataPool();
    private final ArrayDeque<ZomboidNetData> pool = new ArrayDeque<>();

    public ZomboidNetData get() {
        synchronized (this.pool) {
            return this.pool.isEmpty() ? new ZomboidNetData() : this.pool.pop();
        }
    }

    public void discard(ZomboidNetData data) {
        data.reset();
        if (data.buffer.capacity() == 2048) {
            synchronized (this.pool) {
                this.pool.add(data);
            }
        }
    }

    public ZomboidNetData getLong(int len) {
        return new ZomboidNetData(len);
    }
}
