// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import org.lwjglx.opengl.Display;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.network.GameServer;
import zombie.ui.FPSGraph;

public final class LightingThread {
    public static final LightingThread instance = new LightingThread();
    public Thread lightingThread;
    public boolean finished;
    public volatile boolean interrupted;
    public static boolean debugLockTime;

    public void stop() {
        if (!PerformanceSettings.lightingThread) {
            LightingJNI.stop();
        } else {
            this.finished = true;

            while (this.lightingThread.isAlive()) {
            }

            LightingJNI.stop();
            this.lightingThread = null;
        }
    }

    public void create() {
        if (!GameServer.server) {
            if (PerformanceSettings.lightingThread) {
                this.finished = false;
                this.lightingThread = new Thread(ThreadGroups.Workers, this::threadLoop);
                this.lightingThread.setPriority(5);
                this.lightingThread.setDaemon(true);
                this.lightingThread.setName("Lighting Thread");
                this.lightingThread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
                this.lightingThread.start();
            }
        }
    }

    private void threadLoop() {
        while (!this.finished) {
            if (IsoWorld.instance.currentCell == null) {
                return;
            }

            try {
                this.runInner();
            } catch (Exception var2) {
                ExceptionLogger.logException(var2);
            }
        }
    }

    private void runInner() throws Exception {
        Display.sync(PerformanceSettings.lightingFps);
        LightingJNI.DoLightingUpdateNew(System.nanoTime(), Core.dirtyGlobalLightsCount > 0);
        if (Core.dirtyGlobalLightsCount > 3) {
            Core.dirtyGlobalLightsCount = 2;
        }

        if (Core.dirtyGlobalLightsCount > 0) {
            Core.dirtyGlobalLightsCount--;
        }

        while (LightingJNI.WaitingForMain() && !this.finished) {
            Thread.sleep(13L);
        }

        if (Core.debug && FPSGraph.instance != null) {
            FPSGraph.instance.addLighting(System.currentTimeMillis());
        }
    }

    public void GameLoadingUpdate() {
    }

    public void update() {
        if (IsoWorld.instance != null && IsoWorld.instance.currentCell != null) {
            if (LightingJNI.init) {
                LightingJNI.update();
            }
        }
    }

    public void scrollLeft(int playerIndex) {
        if (LightingJNI.init) {
            LightingJNI.scrollLeft(playerIndex);
        }
    }

    public void scrollRight(int playerIndex) {
        if (LightingJNI.init) {
            LightingJNI.scrollRight(playerIndex);
        }
    }

    public void scrollUp(int playerIndex) {
        if (LightingJNI.init) {
            LightingJNI.scrollUp(playerIndex);
        }
    }

    public void scrollDown(int playerIndex) {
        if (LightingJNI.init) {
            LightingJNI.scrollDown(playerIndex);
        }
    }
}
