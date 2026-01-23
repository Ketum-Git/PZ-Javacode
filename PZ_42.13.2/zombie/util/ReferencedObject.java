// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReferencedObject {
    private final AtomicInteger referenceCount = new AtomicInteger(0);

    int getReferenceCount() {
        return this.referenceCount.get();
    }

    void retain() {
        this.referenceCount.incrementAndGet();
    }

    void release() {
        this.referenceCount.decrementAndGet();
    }
}
