// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

public abstract class SingleThreadPool<T> {
    public final int max;
    public int peak;
    private final Array<T> freeObjects;

    public SingleThreadPool() {
        this(16, Integer.MAX_VALUE);
    }

    public SingleThreadPool(int initialCapacity) {
        this(initialCapacity, Integer.MAX_VALUE);
    }

    public SingleThreadPool(int initialCapacity, int max) {
        this.freeObjects = new Array<>(false, initialCapacity);
        this.max = max;
    }

    protected abstract T newObject();

    public T obtain() {
        return this.freeObjects.size == 0 ? this.newObject() : this.freeObjects.pop();
    }

    public void free(T object) {
        if (object == null) {
            throw new IllegalArgumentException("object cannot be null.");
        } else {
            if (this.freeObjects.size < this.max) {
                this.freeObjects.add(object);
                this.peak = Math.max(this.peak, this.freeObjects.size);
                this.reset(object);
            } else {
                this.discard(object);
            }
        }
    }

    public void fill(int size) {
        for (int i = 0; i < size; i++) {
            if (this.freeObjects.size < this.max) {
                this.freeObjects.add(this.newObject());
            }
        }

        this.peak = Math.max(this.peak, this.freeObjects.size);
    }

    protected void reset(T object) {
        if (object instanceof SingleThreadPool.Poolable poolable) {
            poolable.reset();
        }
    }

    protected void discard(T object) {
        this.reset(object);
    }

    public void freeAll(Array<T> objects) {
        if (objects == null) {
            throw new IllegalArgumentException("objects cannot be null.");
        } else {
            Array<T> freeObjects = this.freeObjects;
            int max = this.max;
            int i = 0;

            for (int n = objects.size; i < n; i++) {
                T object = objects.get(i);
                if (object != null) {
                    if (freeObjects.size < max) {
                        freeObjects.add(object);
                        this.reset(object);
                    } else {
                        this.discard(object);
                    }
                }
            }

            this.peak = Math.max(this.peak, freeObjects.size);
        }
    }

    public void clear() {
        Array<T> freeObjects = this.freeObjects;
        int i = 0;

        for (int n = freeObjects.size; i < n; i++) {
            this.discard(freeObjects.get(i));
        }

        freeObjects.clear();
    }

    public int getFree() {
        return this.freeObjects.size;
    }

    public interface Poolable {
        void reset();
    }
}
