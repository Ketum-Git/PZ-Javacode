// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ZomboidNetDataPool {
    public static final ZomboidNetDataPool instance = new ZomboidNetDataPool();
    final ConcurrentLinkedQueue<ZomboidNetData> pool = new ConcurrentLinkedQueue<>();

    public ZomboidNetData get() {
        ZomboidNetData data = this.pool.poll();
        return data == null ? new ZomboidNetData() : data;
    }

    public void discard(ZomboidNetData data) {
        data.reset();
        if (data.buffer.capacity() == 2048) {
            this.pool.add(data);
        }
    }

    public ZomboidNetData getLong(int len) {
        return new ZomboidNetData(len);
    }
}
