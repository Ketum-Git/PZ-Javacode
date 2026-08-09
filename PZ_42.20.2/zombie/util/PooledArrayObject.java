// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.function.Function;

public class PooledArrayObject<T> extends PooledObject {
    private T[] array;

    public T[] array() {
        return this.array;
    }

    public int length() {
        return this.array.length;
    }

    public T get(int idx) {
        return this.array[idx];
    }

    public void set(int idx, T val) {
        this.array[idx] = val;
    }

    protected void initCapacity(int count, Function<Integer, T[]> allocator) {
        if (this.array == null || this.array.length != count) {
            this.array = (T[])((Object[])allocator.apply(count));
        }
    }

    public boolean isEmpty() {
        return this.array == null || this.array.length == 0;
    }
}
