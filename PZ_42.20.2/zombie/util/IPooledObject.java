// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.List;
import zombie.util.list.PZArrayUtil;

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

    static <E extends IPooledObject> E[] tryReleaseAndBlank(E[] list) {
        return (E[])(list != null ? releaseAndBlank(list) : null);
    }

    static <E extends IPooledObject> E[] releaseAndBlank(E[] list) {
        PZArrayUtil.forEach(list, Pool::tryRelease);
        return null;
    }

    static void release(List<? extends IPooledObject> list) {
        PZArrayUtil.forEach(list, Pool::tryRelease);
        list.clear();
    }
}
