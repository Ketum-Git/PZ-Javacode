// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

class GenericDebugLogFormatter implements IDebugLogFormatter {
    private final DebugType debugType;

    public GenericDebugLogFormatter(DebugType debugType) {
        this.debugType = debugType;
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean in_allowRepeat, String formatNoParams) {
        return DebugLog.formatString(this.debugType, logSeverity, affix, in_allowRepeat, formatNoParams);
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean in_allowRepeat, String format, Object... params) {
        return DebugLog.formatString(this.debugType, logSeverity, affix, in_allowRepeat, format, params);
    }
}
