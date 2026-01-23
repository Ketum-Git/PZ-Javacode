// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.util.ArrayList;

public final class DirectBufferAllocator {
    private static final Object LOCK = "DirectBufferAllocator.LOCK";
    private static final ArrayList<WrappedBuffer> ALL = new ArrayList<>();

    public static WrappedBuffer allocate(int size) {
        synchronized (LOCK) {
            destroyDisposed();
            WrappedBuffer wrapped = new WrappedBuffer(size);
            ALL.add(wrapped);
            return wrapped;
        }
    }

    public static void destroyDisposed() {
        synchronized (LOCK) {
            for (int i = ALL.size() - 1; i >= 0; i--) {
                WrappedBuffer wrapped = ALL.get(i);
                if (wrapped.isDisposed()) {
                    ALL.remove(i);
                }
            }
        }
    }

    public static long getBytesAllocated() {
        synchronized (LOCK) {
            destroyDisposed();
            long total = 0L;

            for (int i = 0; i < ALL.size(); i++) {
                WrappedBuffer wrappedBuffer = ALL.get(i);
                if (!wrappedBuffer.isDisposed()) {
                    total += wrappedBuffer.capacity();
                }
            }

            return total;
        }
    }
}
