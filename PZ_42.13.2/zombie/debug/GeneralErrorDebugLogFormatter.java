// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

class GeneralErrorDebugLogFormatter implements IDebugLogFormatter {
    @Override
    public String format(LogSeverity logSeverity, String affix, boolean in_allowRepeat, String formatNoParams) {
        return DebugLog.formatString(DebugType.General, logSeverity, affix, in_allowRepeat, formatNoParams);
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean in_allowRepeat, String format, Object... params) {
        return DebugLog.formatString(DebugType.General, logSeverity, affix, in_allowRepeat, format, params);
    }
}
