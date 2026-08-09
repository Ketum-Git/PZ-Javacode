// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import zombie.util.Pool;

public class DoublePointPool {
    private static final Pool<DoublePoint> s_pool = new Pool<>(DoublePoint::new);

    public static DoublePoint alloc() {
        return s_pool.alloc();
    }

    public static void release(DoublePoint obj) {
        s_pool.release(obj);
    }
}
