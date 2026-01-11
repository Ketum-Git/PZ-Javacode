// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import zombie.util.list.PZArrayUtil;

/**
 * A thread-safe object pool. Useful for re-using memory without it falling into the garbage collector.
 * 
 *  Beware: Once an item has been allocated, it MUST be released at some point by calling its release() function.
 *          If not, the item's memory will never be recycled, and it will be considered a memory leak.
 */
public final class Pool<PO extends IPooledObject> {
    private final Supplier<PO> allocator;
    private final ThreadLocal<Pool.PoolStacks> stacks = ThreadLocal.withInitial(Pool.PoolStacks::new);

    public ThreadLocal<Pool.PoolStacks> getPoolStacks() {
        return this.stacks;
    }

    public Pool(Supplier<PO> allocator) {
        this.allocator = allocator;
    }

    public PO alloc() {
        Supplier<PO> allocator = this.allocator;
        Pool.PoolStacks poolStacks = this.stacks.get();
        synchronized (poolStacks.lock) {
            return this.allocInternal(poolStacks, allocator);
        }
    }

    public void release(IPooledObject item) {
        Pool.PoolReference itemPoolRef = item.getPoolReference();
        Pool<IPooledObject> itemPool = itemPoolRef.getPool();
        Pool.PoolStacks itemStacks = itemPoolRef.getPoolStacks();
        synchronized (itemStacks.lock) {
            this.releaseItemInternal(item, itemStacks, itemPool);
        }
    }

    private PO allocInternal(Pool.PoolStacks poolStacks, Supplier<PO> allocator) {
        THashSet<IPooledObject> usedStack = poolStacks.inUse;
        List<IPooledObject> releasedStack = poolStacks.released;
        IPooledObject newObj;
        if (!releasedStack.isEmpty()) {
            newObj = releasedStack.remove(releasedStack.size() - 1);
        } else {
            newObj = allocator.get();
            if (newObj == null) {
                throw new NullPointerException("Allocator returned a nullPtr. This is not allowed.");
            }

            newObj.setPool(new Pool.PoolReference(this, poolStacks));
        }

        newObj.setFree(false);
        usedStack.add(newObj);
        return (PO)newObj;
    }

    private void releaseItemInternal(IPooledObject item, Pool.PoolStacks poolStacks, Pool<IPooledObject> itemPool) {
        THashSet<IPooledObject> usedStack = poolStacks.inUse;
        List<IPooledObject> releasedStack = poolStacks.released;
        if (itemPool != this) {
            throw new UnsupportedOperationException("Cannot release item. Not owned by this pool.");
        } else if (item.isFree()) {
            throw new UnsupportedOperationException("Cannot release item. Already released.");
        } else if (!usedStack.remove(item)) {
            throw new UnsupportedOperationException(
                "Attempting to release PooledObject not in Pool, possibly releasing on different thread than alloc. " + item
            );
        } else {
            item.setFree(true);
            releasedStack.add(item);
            item.onReleased();
        }
    }

    public static <E> E tryRelease(E obj) {
        IPooledObject pooledObject = Type.tryCastTo(obj, IPooledObject.class);
        if (pooledObject != null && !pooledObject.isFree()) {
            pooledObject.release();
            return null;
        } else if (obj instanceof List<?> list) {
            PZArrayUtil.forEach(list, Pool::tryRelease);
            list.clear();
            return obj;
        } else if (obj instanceof Collection<?> collection) {
            PZArrayUtil.forEach((Iterable<E>)collection, Pool::tryRelease);
            collection.clear();
            return obj;
        } else if (obj instanceof Iterable<?> iterable) {
            PZArrayUtil.forEach(iterable, Pool::tryRelease);
            return obj;
        } else {
            return null;
        }
    }

    public static <E extends IPooledObject> E tryRelease(E pooledObject) {
        if (pooledObject != null && !pooledObject.isFree()) {
            pooledObject.release();
        }

        return null;
    }

    public static <E extends IPooledObject> E[] tryRelease(E[] objArray) {
        PZArrayUtil.forEach(objArray, Pool::tryRelease);
        return null;
    }

    public static final class PoolReference {
        final Pool<IPooledObject> pool;
        final Pool.PoolStacks poolStacks;

        private PoolReference(Pool<IPooledObject> in_pool, Pool.PoolStacks in_poolStacks) {
            this.pool = in_pool;
            this.poolStacks = in_poolStacks;
        }

        public Pool<IPooledObject> getPool() {
            return this.pool;
        }

        private Pool.PoolStacks getPoolStacks() {
            return this.poolStacks;
        }

        public void release(IPooledObject in_item) {
            this.pool.release(in_item);
        }
    }

    public static final class PoolStacks {
        final THashSet<IPooledObject> inUse = new THashSet<>();
        final List<IPooledObject> released = new ArrayList<>();
        final Object lock = new Object();

        PoolStacks() {
            this.inUse.setAutoCompactionFactor(0.0F);
        }

        public THashSet<IPooledObject> getInUse() {
            return this.inUse;
        }

        public List<IPooledObject> getReleased() {
            return this.released;
        }
    }
}
