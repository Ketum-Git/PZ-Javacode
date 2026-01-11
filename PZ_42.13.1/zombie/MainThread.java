// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import zombie.core.ThreadGroups;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.Lambda;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public class MainThread {
    public static Thread mainThread;
    private static Runnable mainThreadStart;
    private static Runnable mainThreadLoop;
    private static Runnable mainThreadExit;
    private static UncaughtExceptionHandler mainThreadExceptionHandler;
    public static final Object m_contextLock = "MainThread borrowContext Lock";
    private static final ArrayList<MainThreadQueueItem> invokeOnMainQueue = new ArrayList<>();
    private static final ArrayList<MainThreadQueueItem> invokeOnMainQueue_Invoking = new ArrayList<>();
    private static boolean isInitialized;
    private static final Object m_initLock = "MainThread Initialization Lock";
    private static volatile boolean isCloseRequested;

    public static Thread init(
        Runnable in_mainThreadStart, Runnable in_mainThreadLoop, Runnable in_mainThreadExit, UncaughtExceptionHandler in_uncaughtExceptionHandler
    ) {
        synchronized (m_initLock) {
            if (isInitialized) {
                return mainThread;
            }

            mainThreadStart = in_mainThreadStart;
            mainThreadLoop = in_mainThreadLoop;
            mainThreadExit = in_mainThreadExit;
            mainThreadExceptionHandler = in_uncaughtExceptionHandler;
            mainThread = new Thread(ThreadGroups.Main, MainThread::mainLoop, "MainThread");
            mainThread.setUncaughtExceptionHandler(MainThread::uncaughtException);
            isInitialized = true;
            mainThread.start();
        }

        return mainThread;
    }

    public static void mainLoop() {
        if (!isInitialized) {
            throw new IllegalStateException("MainThread is not initialized.");
        } else {
            mainThreadStart.run();

            while (!RenderThread.isCloseRequested() && !GameWindow.closeRequested) {
                synchronized (m_contextLock) {
                    flushInvokeQueue();
                    DebugOptions.testThreadCrash(0);
                    mainThreadLoop.run();
                }

                isCloseRequested = RenderThread.isCloseRequested() || GameWindow.closeRequested;
                Thread.yield();
            }

            synchronized (m_initLock) {
                mainThread = null;
                isInitialized = false;
            }

            mainThreadExit.run();
            isCloseRequested = true;
        }
    }

    private static void uncaughtException(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath) {
            DebugLog.General.println("Main Thread exited: ", thread.getName());
        } else {
            try {
                mainThreadExceptionHandler.uncaughtException(thread, e);
            } finally {
                DebugLog.General.error("  Notifying InvokeOnMainQueue...");
                synchronized (invokeOnMainQueue) {
                    invokeOnMainQueue_Invoking.addAll(invokeOnMainQueue);
                    invokeOnMainQueue.clear();
                }

                PZArrayUtil.forEach(invokeOnMainQueue_Invoking, MainThreadQueueItem::notifyWaitingListeners);
            }
        }
    }

    private static void flushInvokeQueue() {
        synchronized (invokeOnMainQueue) {
            if (!invokeOnMainQueue.isEmpty()) {
                invokeOnMainQueue_Invoking.addAll(invokeOnMainQueue);
                invokeOnMainQueue.clear();
            }
        }

        try {
            if (!invokeOnMainQueue_Invoking.isEmpty()) {
                long start = System.nanoTime();

                while (!invokeOnMainQueue_Invoking.isEmpty()) {
                    MainThreadQueueItem item = invokeOnMainQueue_Invoking.remove(0);
                    long startJob = System.nanoTime();
                    item.invoke();
                    long endJob = System.nanoTime();
                    if (endJob - startJob > 1.0E7) {
                        boolean var7 = true;
                    }

                    if (endJob - start > 1.0E7) {
                        break;
                    }
                }

                for (int i = invokeOnMainQueue_Invoking.size() - 1; i >= 0; i--) {
                    MainThreadQueueItem itemx = invokeOnMainQueue_Invoking.get(i);
                    if (itemx.isWaiting()) {
                        while (i >= 0) {
                            MainThreadQueueItem item1 = invokeOnMainQueue_Invoking.remove(0);
                            item1.invoke();
                            i--;
                        }
                        break;
                    }
                }
            }
        } catch (Exception var8) {
            DebugLog.General.error("Thrown an " + var8.getClass().getTypeName() + ": " + var8.getMessage());
            var8.printStackTrace();
        }
    }

    public static void invokeOnMainThread(Runnable toInvoke) throws MainThreadQueueException {
        MainThreadQueueItem queueItem = MainThreadQueueItem.alloc(toInvoke);
        queueItem.setWaiting();
        queueInvokeOnMainThread(queueItem);

        try {
            queueItem.waitUntilFinished(() -> !isCloseRequested && !GameWindow.gameThreadExited);
        } catch (InterruptedException var3) {
            DebugLog.General.error("Thread Interrupted while waiting for queued item to finish:" + queueItem);
        }

        Throwable t = queueItem.getThrown();
        if (t != null) {
            throw new MainThreadQueueException(t);
        }
    }

    public static boolean invokeQueryOnMainThread(Invokers.Params0.Boolean.ICallback in_invoker) {
        if (mainThread == Thread.currentThread()) {
            return in_invoker.accept();
        } else {
            if (!isInitialized) {
                for (int i = 0; i < 1048576 && !isInitialized && !Thread.interrupted(); i++) {
                    Thread.yield();
                }

                if (!isInitialized) {
                    return false;
                }
            }

            Invokers.Params0.Boolean.CallbackStackItem invoker = Lambda.invokerBoolean(in_invoker);
            invokeOnMainThread(invoker);
            boolean result = invoker.getAsBoolean();
            invoker.release();
            return result;
        }
    }

    public static <T1> void invokeOnMainThread(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        Lambda.capture(val1, invoker, (stack, l_val1, l_invoker) -> invokeOnMainThread(stack.invoker(l_val1, l_invoker)));
    }

    public static <T1, T2> void invokeOnMainThread(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        Lambda.capture(val1, val2, invoker, (stack, l_val1, l_val2, l_invoker) -> invokeOnMainThread(stack.invoker(l_val1, l_val2, l_invoker)));
    }

    public static <T1, T2, T3> void invokeOnMainThread(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        Lambda.capture(
            val1, val2, val3, invoker, (stack, l_val1, l_val2, l_val3, l_invoker) -> invokeOnMainThread(stack.invoker(l_val1, l_val2, l_val3, l_invoker))
        );
    }

    public static <T1, T2, T3, T4> void invokeOnMainThread(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        Lambda.capture(
            val1,
            val2,
            val3,
            val4,
            invoker,
            (stack, l_val1, l_val2, l_val3, l_val4, l_invoker) -> invokeOnMainThread(stack.invoker(l_val1, l_val2, l_val3, l_val4, l_invoker))
        );
    }

    public static void queueInvokeOnMainThread(Runnable runnable) {
        queueInvokeOnMainThread(MainThreadQueueItem.alloc(runnable));
    }

    public static void queueInvokeOnMainThread(MainThreadQueueItem queueItem) {
        if (!isInitialized) {
            synchronized (m_initLock) {
                if (!isInitialized) {
                    queueItem.invoke();
                    return;
                }
            }
        }

        if (mainThread == Thread.currentThread()) {
            queueItem.invoke();
        } else {
            synchronized (invokeOnMainQueue) {
                invokeOnMainQueue.add(queueItem);
            }
        }
    }

    public static void shutdown() {
    }

    public static boolean isRunning() {
        return isInitialized;
    }

    public static void busyWait() {
        if (Thread.currentThread() == GameWindow.gameThread) {
            flushInvokeQueue();
        }
    }
}
