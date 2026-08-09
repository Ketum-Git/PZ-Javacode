// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainer<T> extends PooledObject {
    public T returnVal;
    private static final Pool<ReturnValueContainer<Object>> s_pool = new Pool<>(ReturnValueContainer::new);

    @Override
    public void onReleased() {
        this.returnVal = null;
    }

    public static <E> ReturnValueContainer<E> alloc() {
        return (ReturnValueContainer<E>)s_pool.alloc();
    }
}
