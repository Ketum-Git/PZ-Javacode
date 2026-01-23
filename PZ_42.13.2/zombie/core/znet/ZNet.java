// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import zombie.SystemDisabler;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public class ZNet {
    private static final SimpleDateFormat s_logSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");

    public static native void init();

    private static native void setLogLevel(int var0);

    public static void SetLogLevel(int level) {
        DebugLog.enableLog(DebugType.Network, switch (level) {
            case 0 -> LogSeverity.Warning;
            case 1 -> LogSeverity.General;
            case 2 -> LogSeverity.Debug;
            default -> LogSeverity.Error;
        });
    }

    public static void SetLogLevel(LogSeverity severity) {
        setLogLevel(severity.ordinal());
    }

    private static void logPutsCallback(String s) {
        if (SystemDisabler.printDetailedInfo()) {
            String time = s_logSdf.format(Calendar.getInstance().getTime());
            DebugLog.Network.print("[" + time + "] > " + s);
            System.out.flush();
        }
    }
}
