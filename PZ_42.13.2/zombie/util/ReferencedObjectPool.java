// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import zombie.core.Core;
import zombie.debug.DebugLog;

public class ReferencedObjectPool<T extends ReferencedObject> {
    private final ConcurrentLinkedQueue<T> released = new ConcurrentLinkedQueue<>();
    private final Supplier<T> allocator;

    public ReferencedObjectPool(Supplier<T> allocator) {
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
                DebugLog.General.printStackTrace("Object is referenced " + obj.getReferenceCount() + " times");
            }

            return this.create();
        }
    }

    public void release(T obj) {
        if (obj.getReferenceCount() == 1) {
            obj.release();
            this.released.offer(obj);
        } else if (Core.debug) {
            DebugLog.General.printStackTrace("Object is referenced " + obj.getReferenceCount() + " times");
        }
    }

    int size() {
        return this.released.size();
    }

    private T create() {
        T obj = this.allocator.get();
        obj.retain();
        return obj;
    }
}
