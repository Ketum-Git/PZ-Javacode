// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;
import zombie.HiddenFromLua;
import zombie.core.logger.ExceptionLogger;

@HiddenFromLua
public final class DesktopBrowser {
    private static final String[] browsers = new String[]{
        "google-chrome", "firefox", "mozilla", "epiphany", "konqueror", "netscape", "opera", "links", "lynx", "chromium", "brave-browser"
    };

    public static boolean openURL(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return true;
            }

            if (System.getProperty("os.name").contains("OS X")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
                return true;
            }

            if (System.getProperty("os.name").startsWith("Win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
                return true;
            }

            for (String b : browsers) {
                Process process = Runtime.getRuntime().exec(new String[]{"which", b});
                if (process.getInputStream().read() != -1) {
                    Runtime.getRuntime().exec(new String[]{b, url});
                    return true;
                }
            }
        } catch (IOException var6) {
            ExceptionLogger.logException(var6);
        }

        return false;
    }
}
