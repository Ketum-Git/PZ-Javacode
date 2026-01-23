// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.logger;

import org.lwjglx.opengl.OpenGLException;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.ui.UITransition;

public final class ExceptionLogger {
    private static int exceptionCount;
    private static boolean ignore;
    private static final boolean bExceptionPopup = true;
    private static long popupFrameMS;
    private static final UITransition transition = new UITransition();
    private static boolean hide;

    public static synchronized void logException(Throwable ex) {
        logException(ex, null);
    }

    public static synchronized void logException(Throwable ex, String errorMessage) {
        logException(ex, errorMessage, DebugLog.General, LogSeverity.Error);
    }

    public static synchronized void logException(Throwable ex, String errorMessage, DebugLogStream out, LogSeverity severity) {
        if (ex instanceof OpenGLException glEx) {
            RenderThread.logGLException(glEx, false);
        }

        out.printException(ex, errorMessage, DebugLogStream.generateCallerPrefix(), severity);

        try {
            if (ignore) {
                return;
            }

            ignore = true;
            exceptionCount++;
            if (!GameServer.server) {
                showPopup();
                return;
            }
        } catch (Throwable var8) {
            out.printException(var8, "Exception thrown while trying to logException.", LogSeverity.Error);
            return;
        } finally {
            ignore = false;
        }
    }

    public static void showPopup() {
        float elapsed = popupFrameMS > 0L ? transition.getElapsed() : 0.0F;
        popupFrameMS = 3000L;
        transition.setIgnoreUpdateTime(true);
        transition.init(500.0F, false);
        transition.setElapsed(elapsed);
        hide = false;
    }

    public static void render() {
        if (!UIManager.useUiFbo || Core.getInstance().uiRenderThisFrame) {
            boolean force = false;
            if (popupFrameMS > 0L) {
                popupFrameMS = (long)(popupFrameMS - UIManager.getMillisSinceLastRender());
                transition.update();
                int fontHgt = TextManager.instance.getFontHeight(UIFont.DebugConsole);
                int width = 100;
                int height = fontHgt * 2 + 4;
                int x = Core.getInstance().getScreenWidth() - 100;
                int y = Core.getInstance().getScreenHeight() - (int)(height * transition.fraction());
                SpriteRenderer.instance.renderi(null, x, y, 100, height, 0.8F, 0.0F, 0.0F, 1.0F, null);
                SpriteRenderer.instance.renderi(null, x + 1, y + 1, 98, fontHgt - 1, 0.0F, 0.0F, 0.0F, 1.0F, null);
                TextManager.instance.DrawStringCentre(UIFont.DebugConsole, x + 50, y, "ERROR", 1.0, 0.0, 0.0, 1.0);
                TextManager.instance.DrawStringCentre(UIFont.DebugConsole, x + 50, y + fontHgt, Integer.toString(exceptionCount), 0.0, 0.0, 0.0, 1.0);
                if (popupFrameMS <= 0L && !hide) {
                    popupFrameMS = 500L;
                    transition.init(500.0F, true);
                    hide = true;
                }
            }
        }
    }
}
