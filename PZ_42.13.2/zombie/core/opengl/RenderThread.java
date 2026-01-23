// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Controllers;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.core.Clipboard;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.TextureID;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoPuddles;
import zombie.network.GameServer;
import zombie.ui.FPSGraph;
import zombie.util.Lambda;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public class RenderThread {
    public static Thread renderThread;
    private static Thread contextThread;
    private static boolean isDisplayCreated;
    private static int contextLockReentrantDepth;
    public static final Object m_contextLock = "RenderThread borrowContext Lock";
    private static final ArrayList<RenderContextQueueItem> invokeOnRenderQueue = new ArrayList<>();
    private static final ArrayList<RenderContextQueueItem> invokeOnRenderQueue_Invoking = new ArrayList<>();
    private static boolean isInitialized;
    private static final Object m_initLock = "RenderThread Initialization Lock";
    private static volatile boolean isCloseRequested;
    private static volatile int displayWidth;
    private static volatile int displayHeight;
    private static volatile boolean renderingEnabled = true;
    private static volatile boolean waitForRenderState;
    private static volatile boolean hasContext;
    private static boolean cursorVisible = true;
    private static long renderTime;
    private static long startWaitTime;
    private static long waitTime;

    public static void init() throws IOException, LWJGLException {
        synchronized (m_initLock) {
            if (!isInitialized) {
                renderThread = Thread.currentThread();
                displayWidth = Display.getWidth();
                displayHeight = Display.getHeight();
                isInitialized = true;
                if (!GameServer.server) {
                    GameWindow.InitDisplay();
                    Controllers.create();
                    Clipboard.initMainThread();
                }
            }
        }
    }

    public static void initServerGUI() {
        synchronized (m_initLock) {
            if (isInitialized) {
                return;
            }

            renderThread = new Thread(ThreadGroups.Main, RenderThread::renderLoop, "RenderThread Main Loop");
            renderThread.setName("Render Thread");
            renderThread.setUncaughtExceptionHandler(RenderThread::uncaughtException);
            displayWidth = Display.getWidth();
            displayHeight = Display.getHeight();
            isInitialized = true;
        }

        renderThread.start();
    }

    public static long getRenderTime() {
        return renderTime;
    }

    public static void renderLoop() {
        if (!isInitialized) {
            throw new IllegalStateException("RenderThread is not initialized.");
        } else {
            acquireContextReentrant();
            boolean isAlive = true;

            while (isAlive) {
                long startTime = System.nanoTime();
                if (startWaitTime == 0L) {
                    startWaitTime = startTime;
                }

                synchronized (m_contextLock) {
                    if (!hasContext) {
                        acquireContextReentrant();
                    }

                    displayWidth = Display.getWidth();
                    displayHeight = Display.getHeight();
                    if (renderingEnabled) {
                        try (AbstractPerformanceProfileProbe ignored = RenderThread.s_performance.renderStep.profile()) {
                            renderStep();
                        }
                    } else if (isDisplayCreated && hasContext) {
                        Display.processMessages();
                    }

                    flushInvokeQueue();
                    if (!renderingEnabled) {
                        isCloseRequested = false;
                    } else {
                        GameWindow.GameInput.poll();
                        Mouse.poll();
                        GameKeyboard.poll();
                        isCloseRequested = isCloseRequested || Display.isCloseRequested();
                    }

                    if (!GameServer.server) {
                        Clipboard.updateMainThread();
                    }

                    DebugOptions.testThreadCrash(0);
                    isAlive = !GameWindow.gameThreadExited;
                }

                renderTime = System.nanoTime() - startTime;
                Thread.yield();
            }

            releaseContextReentrant();
            synchronized (m_initLock) {
                renderThread = null;
                isInitialized = false;
            }

            shutdown();
            System.exit(0);
        }
    }

    private static void uncaughtException(Thread thread, Throwable e) {
        if (e instanceof ThreadDeath) {
            DebugLog.General.println("Render Thread exited: ", thread.getName());
        } else {
            try {
                GameWindow.uncaughtException(thread, e);
            } finally {
                Runnable forceClose = () -> {
                    long maxTimeMs = 120000L;
                    long timeMs = 0L;
                    long timeNow = System.currentTimeMillis();
                    long timePrev = timeNow;
                    if (!GameWindow.gameThreadExited) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var11) {
                        }

                        DebugLog.General.error("  Waiting for GameThread to exit...");

                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException var10) {
                        }

                        while (!GameWindow.gameThreadExited) {
                            Thread.yield();
                            timeNow = System.currentTimeMillis();
                            long timeDiff = timeNow - timePrev;
                            timeMs += timeDiff;
                            if (timeMs >= 120000L) {
                                DebugLog.General.error("  GameThread failed to exit within time limit.");
                                break;
                            }

                            timePrev = timeNow;
                        }
                    }

                    DebugLog.General.error("  Shutting down...");
                    System.exit(1);
                };
                Thread forceCloseThread = new Thread(forceClose, "ForceCloseThread");
                forceCloseThread.start();
                DebugLog.General.error("Shutting down sequence starts.");
                isCloseRequested = true;
                DebugLog.General.error("  Notifying render state queue...");
                notifyRenderStateQueue();
                DebugLog.General.error("  Notifying InvokeOnRenderQueue...");
                synchronized (invokeOnRenderQueue) {
                    invokeOnRenderQueue_Invoking.addAll(invokeOnRenderQueue);
                    invokeOnRenderQueue.clear();
                }

                PZArrayUtil.forEach(invokeOnRenderQueue_Invoking, RenderContextQueueItem::notifyWaitingListeners);
            }
        }
    }

    private static boolean renderStep() {
        boolean result = false;

        try {
            result = lockStepRenderStep();
        } catch (OpenGLException var2) {
            logGLException(var2);
        } catch (Exception var3) {
            DebugLog.General.error("Thrown an " + var3.getClass().getTypeName() + ": " + var3.getMessage());
            ExceptionLogger.logException(var3);
        }

        return result;
    }

    public static long getWaitTime() {
        return waitTime;
    }

    private static boolean lockStepRenderStep() {
        SpriteRenderState renderState = SpriteRenderer.instance.acquireStateForRendering(RenderThread::waitForRenderStateCallback);
        if (renderState != null) {
            waitTime = System.nanoTime() - startWaitTime;
            startWaitTime = 0L;
            cursorVisible = renderState.cursorVisible;

            try (AbstractPerformanceProfileProbe ignored = RenderThread.s_performance.spriteRendererPostRender.profile()) {
                SpriteRenderer.instance.postRender();
            }

            try (AbstractPerformanceProfileProbe ignored = RenderThread.s_performance.displayUpdate.profile()) {
                Display.update(true);
                checkControllers();
            }

            if (Core.debug && FPSGraph.instance != null) {
                FPSGraph.instance.addRender(System.currentTimeMillis());
            }

            return true;
        } else {
            notifyRenderStateQueue();
            if (!waitForRenderState || LuaManager.thread != null && LuaManager.thread.step) {
                try (AbstractPerformanceProfileProbe ignored = RenderThread.s_performance.displayUpdate.profile()) {
                    Display.processMessages();
                }
            }

            return true;
        }
    }

    private static void checkControllers() {
    }

    private static boolean waitForRenderStateCallback() {
        flushInvokeQueue();
        return shouldContinueWaiting();
    }

    private static boolean shouldContinueWaiting() {
        return !isCloseRequested && !GameWindow.gameThreadExited && (waitForRenderState || SpriteRenderer.instance.isWaitingForRenderState());
    }

    public static boolean isWaitForRenderState() {
        return waitForRenderState;
    }

    public static void setWaitForRenderState(boolean wait) {
        waitForRenderState = wait;
    }

    private static void flushInvokeQueue() {
        synchronized (invokeOnRenderQueue) {
            if (!invokeOnRenderQueue.isEmpty()) {
                PZArrayUtil.addAll(invokeOnRenderQueue_Invoking, invokeOnRenderQueue);
                invokeOnRenderQueue.clear();
            }
        }

        try {
            if (!invokeOnRenderQueue_Invoking.isEmpty()) {
                long start = System.nanoTime();

                while (!invokeOnRenderQueue_Invoking.isEmpty()) {
                    RenderContextQueueItem item = invokeOnRenderQueue_Invoking.remove(0);
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

                for (int i = invokeOnRenderQueue_Invoking.size() - 1; i >= 0; i--) {
                    RenderContextQueueItem itemx = invokeOnRenderQueue_Invoking.get(i);
                    if (itemx.isWaiting()) {
                        while (i >= 0) {
                            RenderContextQueueItem item1 = invokeOnRenderQueue_Invoking.remove(0);
                            item1.invoke();
                            i--;
                        }
                        break;
                    }
                }
            }

            if (TextureID.deleteTextureIDS.position() > 0) {
                TextureID.deleteTextureIDS.flip();
                GL11.glDeleteTextures(TextureID.deleteTextureIDS);
                TextureID.deleteTextureIDS.clear();
            }
        } catch (OpenGLException var8) {
            logGLException(var8);
        } catch (Exception var9) {
            DebugLog.General.error("Thrown an " + var9.getClass().getTypeName() + ": " + var9.getMessage());
            var9.printStackTrace();
        }
    }

    public static void logGLException(OpenGLException glEx) {
        logGLException(glEx, true);
    }

    public static void logGLException(OpenGLException glEx, boolean stackTrace) {
        DebugLog.General.error("OpenGLException thrown: " + glEx.getMessage());

        for (int extraErrorCode = GL11.glGetError(); extraErrorCode != 0; extraErrorCode = GL11.glGetError()) {
            String error_string = Util.translateGLErrorString(extraErrorCode);
            DebugLog.General.error("  Also detected error: " + error_string + " ( code:" + extraErrorCode + ")");
        }

        if (stackTrace) {
            DebugLog.General.error("Stack trace:");
            glEx.printStackTrace();
        }
    }

    public static void Ready() {
        SpriteRenderer.instance.pushFrameDown();
        if (!isInitialized) {
            invokeOnRenderContext(RenderThread::renderStep);
        }
    }

    private static void acquireContextReentrant() {
        synchronized (m_contextLock) {
            acquireContextReentrantInternal();
        }
    }

    private static void releaseContextReentrant() {
        synchronized (m_contextLock) {
            releaseContextReentrantInternal();
        }
    }

    private static void acquireContextReentrantInternal() {
        Thread currentThread = Thread.currentThread();
        if (contextThread != null && contextThread != currentThread) {
            throw new RuntimeException("Context thread mismatch: " + contextThread + ", " + currentThread);
        } else {
            contextLockReentrantDepth++;
            if (contextLockReentrantDepth <= 1) {
                contextThread = currentThread;
                isDisplayCreated = Display.isCreated();
                if (isDisplayCreated) {
                    try {
                        hasContext = true;
                        Display.makeCurrent();
                        Display.setVSyncEnabled(Core.getInstance().getOptionVSync());
                    } catch (LWJGLException var2) {
                        DebugLog.General.error("Exception thrown trying to gain GL context.");
                        var2.printStackTrace();
                    }
                }
            }
        }
    }

    private static void releaseContextReentrantInternal() {
        Thread currentThread = Thread.currentThread();
        if (contextThread != currentThread) {
            throw new RuntimeException("Context thread mismatch: " + contextThread + ", " + currentThread);
        } else if (contextLockReentrantDepth == 0) {
            throw new RuntimeException("Context thread release overflow: 0: " + contextThread + ", " + currentThread);
        } else {
            contextLockReentrantDepth--;
            if (contextLockReentrantDepth <= 0) {
                if (isDisplayCreated && hasContext) {
                    try {
                        hasContext = false;
                        Display.releaseContext();
                    } catch (LWJGLException var2) {
                        DebugLog.General.error("Exception thrown trying to release GL context.");
                        var2.printStackTrace();
                    }
                }

                contextThread = null;
            }
        }
    }

    public static void invokeOnRenderContext(Runnable toInvoke) throws RenderContextQueueException {
        RenderContextQueueItem queueItem = RenderContextQueueItem.alloc(toInvoke);
        queueItem.setWaiting();
        queueInvokeOnRenderContext(queueItem);

        try {
            queueItem.waitUntilFinished(() -> {
                notifyRenderStateQueue();
                return !isCloseRequested && !GameWindow.gameThreadExited;
            });
        } catch (InterruptedException var3) {
            DebugLog.General.error("Thread Interrupted while waiting for queued item to finish:" + queueItem);
            notifyRenderStateQueue();
        }

        Throwable t = queueItem.getThrown();
        if (t != null) {
            throw new RenderContextQueueException(t);
        }
    }

    public static boolean invokeQueryOnRenderContext(Invokers.Params0.Boolean.ICallback in_invoker) {
        if (contextThread == Thread.currentThread()) {
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
            invokeOnRenderContext(invoker);
            boolean result = invoker.getAsBoolean();
            invoker.release();
            return result;
        }
    }

    public static <T1> void invokeOnRenderContext(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        Lambda.capture(val1, invoker, (stack, l_val1, l_invoker) -> invokeOnRenderContext(stack.invoker(l_val1, l_invoker)));
    }

    public static <T1, T2> void invokeOnRenderContext(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        Lambda.capture(val1, val2, invoker, (stack, l_val1, l_val2, l_invoker) -> invokeOnRenderContext(stack.invoker(l_val1, l_val2, l_invoker)));
    }

    public static <T1, T2, T3> void invokeOnRenderContext(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        Lambda.capture(
            val1, val2, val3, invoker, (stack, l_val1, l_val2, l_val3, l_invoker) -> invokeOnRenderContext(stack.invoker(l_val1, l_val2, l_val3, l_invoker))
        );
    }

    public static <T1, T2, T3, T4> void invokeOnRenderContext(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        Lambda.capture(
            val1,
            val2,
            val3,
            val4,
            invoker,
            (stack, l_val1, l_val2, l_val3, l_val4, l_invoker) -> invokeOnRenderContext(stack.invoker(l_val1, l_val2, l_val3, l_val4, l_invoker))
        );
    }

    protected static void notifyRenderStateQueue() {
        if (SpriteRenderer.instance != null) {
            SpriteRenderer.instance.notifyRenderStateQueue();
        }
    }

    public static void queueInvokeOnRenderContext(Runnable runnable) {
        queueInvokeOnRenderContext(RenderContextQueueItem.alloc(runnable));
    }

    public static void queueInvokeOnRenderContext(RenderContextQueueItem queueItem) {
        if (!isInitialized) {
            synchronized (m_initLock) {
                if (!isInitialized) {
                    try {
                        acquireContextReentrant();
                        queueItem.invoke();
                    } finally {
                        releaseContextReentrant();
                    }

                    return;
                }
            }
        }

        if (contextThread == Thread.currentThread()) {
            queueItem.invoke();
        } else {
            synchronized (invokeOnRenderQueue) {
                invokeOnRenderQueue.add(queueItem);
            }
        }
    }

    public static void shutdown() {
        GameWindow.GameInput.quit();
        IsoPuddles.getInstance().freeHMTextureBuffer();
        if (isInitialized) {
            queueInvokeOnRenderContext(Display::destroy);
        } else {
            Display.destroy();
        }
    }

    public static boolean isCloseRequested() {
        if (isCloseRequested) {
            DebugType.ExitDebug.debugln("RenderThread.isCloseRequested 1");
            return isCloseRequested;
        } else {
            if (!isInitialized) {
                synchronized (m_initLock) {
                    if (!isInitialized) {
                        isCloseRequested = Display.isCloseRequested();
                        if (isCloseRequested) {
                            DebugType.ExitDebug.debugln("RenderThread.isCloseRequested 2");
                        }
                    }
                }
            }

            return isCloseRequested;
        }
    }

    public static int getDisplayWidth() {
        return !isInitialized ? Display.getWidth() : displayWidth;
    }

    public static int getDisplayHeight() {
        return !isInitialized ? Display.getHeight() : displayHeight;
    }

    public static boolean isRunning() {
        return isInitialized;
    }

    public static void startRendering() {
        renderingEnabled = true;
    }

    public static void onGameThreadExited() {
        DebugLog.General.println("GameThread exited.");
        if (renderThread != null) {
            renderThread.interrupt();
        }
    }

    public static boolean isCursorVisible() {
        return cursorVisible;
    }

    private static class s_performance {
        static final PerformanceProfileFrameProbe renderStep = new PerformanceProfileFrameProbe("RenderThread.renderStep");
        static final PerformanceProfileProbe displayUpdate = new PerformanceProfileProbe("Display.update(true)");
        static final PerformanceProfileProbe spriteRendererPostRender = new PerformanceProfileProbe("SpriteRenderer.postRender");
    }
}
