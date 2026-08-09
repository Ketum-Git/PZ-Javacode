// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.UsedFromLua;
import zombie.util.StringUtils;

@UsedFromLua
public enum LogSeverity {
    Trace("TRACE: "),
    Noise("NOISE: "),
    Debug("DEBUG: "),
    General("LOG  : "),
    Warning("WARN : "),
    Error("ERROR: "),
    Off("!OFF!");

    public static final LogSeverity All = Trace;
    public final String logPrefix;

    private LogSeverity(final String logPrefix) {
        this.logPrefix = logPrefix;
    }

    public boolean isLogEnabled(LogSeverity logSeverity) {
        return this != Off && logSeverity.ordinal() >= this.ordinal();
    }

    public static ArrayList<LogSeverity> getValueList() {
        return new ArrayList<>(Arrays.asList(values()));
    }

    public boolean isName(String str) {
        return StringUtils.equalsIgnoreCase(this.name(), str);
    }
}
