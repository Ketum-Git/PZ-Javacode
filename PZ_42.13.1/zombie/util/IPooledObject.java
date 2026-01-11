// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.List;

/**
 * The base interface of all pooled objects managed by zombie.util.Pool
 */
public interface IPooledObject {
    Pool.PoolReference getPoolReference();

    void setPool(Pool.PoolReference arg0);

    void release();

    boolean isFree();

    void setFree(boolean isFree);

    default void onReleased() {
    }

    static <E extends IPooledObject> E[] release(E[] list) {
        int i = 0;

        for (int count = list.length; i < count; i++) {
            Pool.tryRelease(list[i]);
        }

        return null;
    }

    static <E extends IPooledObject> E[] tryReleaseAndBlank(E[] list) {
        return (E[])(list != null ? releaseAndBlank(list) : null);
    }

    static <E extends IPooledObject> E[] releaseAndBlank(E[] list) {
        int i = 0;

        for (int count = list.length; i < count; i++) {
            list[i] = Pool.tryRelease(list[i]);
        }

        return null;
    }

    static void release(List<? extends IPooledObject> list) {
        int i = 0;

        for (int count = list.size(); i < count; i++) {
            Pool.tryRelease(list.get(i));
        }

        list.clear();
    }
}
