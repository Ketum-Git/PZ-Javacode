// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

/**
 * The base implementation of IPooledObject
 *   Extend from this class if you wish to take advantage of the Pool's functionality.
 * 
 *   If extending from this class is not possible, implement IPooledObject instead.
 */
public abstract class PooledObject implements IPooledObject {
    private boolean isFree = true;
    private Pool.PoolReference pool;

    @Override
    public final Pool.PoolReference getPoolReference() {
        return this.pool;
    }

    @Override
    public final synchronized void setPool(Pool.PoolReference pool) {
        this.pool = pool;
    }

    @Override
    public final synchronized void release() {
        if (this.pool != null) {
            synchronized (this.pool.pool) {
                this.pool.release(this);
            }
        } else {
            this.onReleased();
        }
    }

    @Override
    public final synchronized boolean isFree() {
        return this.isFree;
    }

    @Override
    public final synchronized void setFree(boolean isFree) {
        this.isFree = isFree;
    }
}
