// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BufferUtils {
    private static boolean trackDirectMemory;
    private static final ReferenceQueue<Buffer> removeCollected = new ReferenceQueue<>();
    private static final ConcurrentHashMap<BufferUtils.BufferInfo, BufferUtils.BufferInfo> trackedBuffers = new ConcurrentHashMap<>();
    static BufferUtils.ClearReferences cleanupThread;
    private static final AtomicBoolean loadedMethods = new AtomicBoolean(false);
    private static Method cleanerMethod;
    private static final Method cleanMethod = null;
    private static Method viewedBufferMethod;
    private static Method freeMethod;

    public static void setTrackDirectMemoryEnabled(boolean enabled) {
        trackDirectMemory = enabled;
    }

    private static void onBufferAllocated(Buffer buffer) {
        if (trackDirectMemory) {
            if (cleanupThread == null) {
                cleanupThread = new BufferUtils.ClearReferences();
                cleanupThread.start();
            }

            if (buffer instanceof ByteBuffer) {
                BufferUtils.BufferInfo info = new BufferUtils.BufferInfo(ByteBuffer.class, buffer.capacity(), buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof FloatBuffer) {
                BufferUtils.BufferInfo info = new BufferUtils.BufferInfo(FloatBuffer.class, buffer.capacity() * 4, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof IntBuffer) {
                BufferUtils.BufferInfo info = new BufferUtils.BufferInfo(IntBuffer.class, buffer.capacity() * 4, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof ShortBuffer) {
                BufferUtils.BufferInfo info = new BufferUtils.BufferInfo(ShortBuffer.class, buffer.capacity() * 2, buffer, removeCollected);
                trackedBuffers.put(info, info);
            } else if (buffer instanceof DoubleBuffer) {
                BufferUtils.BufferInfo info = new BufferUtils.BufferInfo(DoubleBuffer.class, buffer.capacity() * 8, buffer, removeCollected);
                trackedBuffers.put(info, info);
            }
        }
    }

    public static void printCurrentDirectMemory(StringBuilder store) {
        long totalHeld = 0L;
        long heapMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        boolean printStout = store == null;
        if (store == null) {
            store = new StringBuilder();
        }

        if (trackDirectMemory) {
            int fBufs = 0;
            int bBufs = 0;
            int iBufs = 0;
            int sBufs = 0;
            int dBufs = 0;
            int fBufsM = 0;
            int bBufsM = 0;
            int iBufsM = 0;
            int sBufsM = 0;
            int dBufsM = 0;

            for (BufferUtils.BufferInfo b : trackedBuffers.values()) {
                if (b.type == ByteBuffer.class) {
                    totalHeld += b.size;
                    bBufsM += b.size;
                    bBufs++;
                } else if (b.type == FloatBuffer.class) {
                    totalHeld += b.size;
                    fBufsM += b.size;
                    fBufs++;
                } else if (b.type == IntBuffer.class) {
                    totalHeld += b.size;
                    iBufsM += b.size;
                    iBufs++;
                } else if (b.type == ShortBuffer.class) {
                    totalHeld += b.size;
                    sBufsM += b.size;
                    sBufs++;
                } else if (b.type == DoubleBuffer.class) {
                    totalHeld += b.size;
                    dBufsM += b.size;
                    dBufs++;
                }
            }

            store.append("Existing buffers: ").append(trackedBuffers.size()).append("\n");
            store.append("(b: ")
                .append(bBufs)
                .append("  f: ")
                .append(fBufs)
                .append("  i: ")
                .append(iBufs)
                .append("  s: ")
                .append(sBufs)
                .append("  d: ")
                .append(dBufs)
                .append(")")
                .append("\n");
            store.append("Total   heap memory held: ").append(heapMem / 1024L).append("kb\n");
            store.append("Total direct memory held: ").append(totalHeld / 1024L).append("kb\n");
            store.append("(b: ")
                .append(bBufsM / 1024)
                .append("kb  f: ")
                .append(fBufsM / 1024)
                .append("kb  i: ")
                .append(iBufsM / 1024)
                .append("kb  s: ")
                .append(sBufsM / 1024)
                .append("kb  d: ")
                .append(dBufsM / 1024)
                .append("kb)")
                .append("\n");
        } else {
            store.append("Total   heap memory held: ").append(heapMem / 1024L).append("kb\n");
            store.append(
                    "Only heap memory available, if you want to monitor direct memory use BufferUtils.setTrackDirectMemoryEnabled(true) during initialization."
                )
                .append("\n");
        }

        if (printStout) {
            System.out.println(store.toString());
        }
    }

    private static Method loadMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (SecurityException | ClassNotFoundException | NoSuchMethodException var3) {
            return null;
        }
    }

    public static ByteBuffer createByteBuffer(int size) {
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        buf.clear();
        onBufferAllocated(buf);
        return buf;
    }

    private static void loadCleanerMethods() {
        if (!loadedMethods.getAndSet(true)) {
            synchronized (loadedMethods) {
                cleanerMethod = loadMethod("sun.nio.ch.DirectBuffer", "cleaner");
                viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "viewedBuffer");
                if (viewedBufferMethod == null) {
                    viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "attachment");
                }

                ByteBuffer bb = createByteBuffer(1);
                Class<?> clazz = bb.getClass();

                try {
                    freeMethod = clazz.getMethod("free");
                } catch (SecurityException | NoSuchMethodException var5) {
                }
            }
        }
    }

    public static void destroyDirectBuffer(Buffer toBeDestroyed) {
        if (toBeDestroyed.isDirect()) {
            loadCleanerMethods();

            try {
                if (freeMethod != null) {
                    freeMethod.invoke(toBeDestroyed);
                } else {
                    Object cleaner = cleanerMethod.invoke(toBeDestroyed);
                    if (cleaner == null) {
                        Object viewedBuffer = viewedBufferMethod.invoke(toBeDestroyed);
                        if (viewedBuffer != null) {
                            destroyDirectBuffer((Buffer)viewedBuffer);
                        } else {
                            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "Buffer cannot be destroyed: {0}", toBeDestroyed);
                        }
                    }
                }
            } catch (IllegalArgumentException | InvocationTargetException | SecurityException | IllegalAccessException var3) {
                Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", (Throwable)var3);
            }
        }
    }

    private static class BufferInfo extends PhantomReference<Buffer> {
        private final Class<?> type;
        private final int size;

        public BufferInfo(Class<?> type, int size, Buffer referent, ReferenceQueue<? super Buffer> q) {
            super(referent, q);
            this.type = type;
            this.size = size;
        }
    }

    private static class ClearReferences extends Thread {
        ClearReferences() {
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Reference<? extends Buffer> toClean = BufferUtils.removeCollected.remove();
                    BufferUtils.trackedBuffers.remove(toClean);
                }
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }
        }
    }
}
