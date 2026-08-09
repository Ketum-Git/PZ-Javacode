// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import zombie.core.Core;
import zombie.debug.DebugType;
import zombie.network.statistics.counters.ObjectPoolCounter;

public class ReferencedObjectPool<T extends ReferencedObject> extends ObjectPoolCounter {
    private final ConcurrentLinkedQueue<T> released = new ConcurrentLinkedQueue<>();
    private final Supplier<T> allocator;

    public ReferencedObjectPool(Supplier<T> allocator, String name) {
        super(name);
        this.allocator = allocator;
    }

    public T alloc() {
        T obj = this.released.poll();
        if (obj == null) {
            return this.create();
        } else if (obj.getReferenceCount() == 0) {
            obj.retain();
            return obj;
        } else {
            if (Core.debug) {
                DebugType.General.printStackTrace("Object is referenced " + obj.getReferenceCount() + " times");
            }

            return this.create();
        }
    }

    public void release(T obj) {
        if (obj.getReferenceCount() == 1) {
            obj.release();
            this.released.offer(obj);
        } else if (Core.debug) {
            DebugType.General.printStackTrace("Object is referenced " + obj.getReferenceCount() + " times");
        }
    }

    @Override
    public int size() {
        return this.released.size();
    }

    private T create() {
        T obj = this.allocator.get();
        obj.retain();
        return obj;
    }
}
