// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.ArrayList;
import java.util.function.Supplier;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.util.lambda.Invokers;

public class AutoCloseablePool extends PooledObject implements AutoCloseable {
    private static final Pool<AutoCloseablePool> s_pool = new Pool<>(AutoCloseablePool::new);
    private final ArrayList<AutoCloseablePool.AutoCloseableEntry<?>> entries = new ArrayList<>();

    public static AutoCloseablePool alloc() {
        return s_pool.alloc();
    }

    private AutoCloseablePool() {
    }

    @Override
    public void onReleased() {
        this.releaseAll();
    }

    @Override
    public void close() {
        this.release();
    }

    private void releaseAll() {
        for (AutoCloseablePool.AutoCloseableEntry<?> entry : this.entries) {
            entry.release();
        }

        this.entries.clear();
    }

    public <T> T alloc(Supplier<T> in_alloc, Invokers.Params1.ICallback<T> in_release) {
        T in_newVal = in_alloc.get();
        AutoCloseablePool.AutoCloseableEntry<T> autoCloseable = AutoCloseablePool.AutoCloseableEntry.alloc(in_newVal, in_release);
        this.entries.add(autoCloseable);
        return in_newVal;
    }

    public Vector2 allocVector2() {
        return this.alloc(() -> Vector2ObjectPool.get().alloc(), val -> Vector2ObjectPool.get().release(val));
    }

    private static class AutoCloseableEntry<T> extends PooledObject {
        private T entry;
        private Invokers.Params1.ICallback<T> onRelease;
        private static final Pool<AutoCloseablePool.AutoCloseableEntry<?>> s_pool = new Pool<>(AutoCloseablePool.AutoCloseableEntry::new);

        @Override
        public void onReleased() {
            this.onRelease.accept(this.entry);
        }

        public static <T> AutoCloseablePool.AutoCloseableEntry<T> alloc(T val, Invokers.Params1.ICallback<T> onRelease) {
            AutoCloseablePool.AutoCloseableEntry<T> newInstance = (AutoCloseablePool.AutoCloseableEntry<T>)s_pool.alloc();
            newInstance.entry = val;
            newInstance.onRelease = onRelease;
            return newInstance;
        }
    }
}
