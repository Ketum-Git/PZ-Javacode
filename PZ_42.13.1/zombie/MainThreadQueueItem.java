// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.function.BooleanSupplier;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;

public final class MainThreadQueueItem {
    private Runnable runnable;
    private boolean isFinished;
    private boolean isWaiting;
    private Throwable runnableThrown;
    private final Object waitLock = "MainThreadQueueItem Wait Lock";

    private MainThreadQueueItem() {
    }

    public static MainThreadQueueItem alloc(Runnable runnable) {
        MainThreadQueueItem newItem = new MainThreadQueueItem();
        newItem.resetInternal();
        newItem.runnable = runnable;
        return newItem;
    }

    private void resetInternal() {
        this.runnable = null;
        this.isFinished = false;
        this.runnableThrown = null;
    }

    public void waitUntilFinished(BooleanSupplier waitCallback) throws InterruptedException {
        while (!this.isFinished()) {
            if (!waitCallback.getAsBoolean()) {
                return;
            }

            synchronized (this.waitLock) {
                if (!this.isFinished()) {
                    this.waitLock.wait();
                }
            }
        }
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public void setWaiting() {
        this.isWaiting = true;
    }

    public boolean isWaiting() {
        return this.isWaiting;
    }

    public void invoke() {
        try {
            this.runnableThrown = null;
            this.runnable.run();
        } catch (Throwable var13) {
            this.runnableThrown = var13;
            DebugLog.General.error("%s thrown during invoke().", var13.toString());
            ExceptionLogger.logException(var13);
        } finally {
            synchronized (this.waitLock) {
                this.isFinished = true;
                this.waitLock.notifyAll();
            }
        }
    }

    public Throwable getThrown() {
        return this.runnableThrown;
    }

    public void notifyWaitingListeners() {
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }
}
