// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

public interface IDebugLogFormatter {
    String format(LogSeverity arg0, String arg1, boolean arg2, String arg3);

    String format(LogSeverity arg0, String arg1, boolean arg2, String arg3, Object... arg4);
}
