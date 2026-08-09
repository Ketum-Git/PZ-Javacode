// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import zombie.UsedFromLua;
import zombie.util.StringUtils;

/**
 * Created by LEMMYPC on 31/12/13.
 */
@UsedFromLua
public enum DebugType {
    General(),
    Packet(),
    NetworkFileDebug(),
    Network(),
    ZNet(),
    DetailedInfo(),
    Lua(),
    LuaObject(),
    GameOption(),
    Mod(),
    Sound(),
    Zombie(),
    Combat(),
    Objects(),
    Fireplace(),
    Radio(),
    MapLoading(),
    Clothing(),
    Animation(),
    AnimationDetailed(),
    AnimationLayers(),
    Asset(),
    Script(),
    Shader(),
    Sprite(),
    Input(),
    Recipe(),
    ActionSystem(),
    ActionSystemEvents(),
    IsoRegion(),
    FileIO(),
    Multiplayer(),
    Damage(),
    Death(),
    Discord(),
    Statistic(),
    Vehicle(),
    VehicleHit(),
    Voice(),
    Checksum(),
    Animal(),
    ItemPicker(),
    CraftLogic(),
    Action(),
    Entity(),
    Lightning(),
    Grapple(),
    ExitDebug(),
    BodyDamage(),
    Xml(),
    Physics(),
    Ballistics(),
    Ragdoll(),
    PZBullet(),
    ModelManager(),
    LoadAnimation(),
    Zone(),
    WorldGen(),
    Foraging(),
    Saving(),
    Fluid(),
    Energy(),
    Translation(),
    Moveable(),
    Basement(),
    FallDamage(),
    ImGui(),
    CharacterTrait(),
    ISUI(),
    ISUIStackTrace(),
    FaceLocationFix(),
    Context(),
    AnimationRecorder(General, Animation, ActionSystem, ActionSystemEvents, Death, Vehicle, VehicleHit, Ragdoll, Context);

    public static final DebugType Default = General;
    private LogSeverity logSeverity = LogSeverity.Off;
    private DebugType orWith;
    private volatile DebugLogStream logStream;
    private final Object logStreamLock = new Object();
    private final IDebugLogFormatter formatter;

    private DebugType(final DebugType... alsoActivate) {
        this.logStream = null;
        this.formatter = this::formatLogStringForConsole;

        for (DebugType toBind : alsoActivate) {
            toBind.orWith = this;
        }
    }

    public boolean isName(String rhs) {
        return StringUtils.equalsIgnoreCase(this.name(), rhs);
    }

    public boolean isEnabled() {
        return this.logSeverity != LogSeverity.Off || this.orWith != null && this.orWith.isEnabled();
    }

    public boolean isEnabled(LogSeverity logSeverity) {
        return this.logSeverity.isLogEnabled(logSeverity) || this.orWith != null && this.orWith.isEnabled(logSeverity);
    }

    public DebugLogStream getLogStream() {
        if (this.logStream == null) {
            synchronized (this.logStreamLock) {
                this.logStream = DebugLog.getInstance().createLogStream(this);
            }
        }

        return this.logStream;
    }

    private String formatLogStringForConsole(DebugType debugType, LogSeverity logSeverity, String affix, Object outputString) {
        return DebugLog.getInstance().formatLogStringForConsole(debugType, logSeverity, affix, outputString);
    }

    public void setLogSeverity(LogSeverity newSeverity) {
        this.logSeverity = newSeverity;
    }

    public LogSeverity getLogSeverity() {
        return this.logSeverity;
    }

    public IDebugLogFormatter getFormatter() {
        return this.formatter;
    }

    public void print(boolean b) {
        this.getLogStream().print(b);
    }

    public void print(char c) {
        this.getLogStream().print(c);
    }

    public void print(int i) {
        this.getLogStream().print(i);
    }

    public void print(long l) {
        this.getLogStream().print(l);
    }

    public void print(float f) {
        this.getLogStream().print(f);
    }

    public void print(double d) {
        this.getLogStream().print(d);
    }

    public void print(String s) {
        this.getLogStream().print(s);
    }

    public void print(Object obj) {
        this.getLogStream().print(obj);
    }

    public PrintStream printf(String format, Object... args) {
        return this.getLogStream().printf(format, args);
    }

    public void println() {
        this.getLogStream().println();
    }

    public void println(boolean x) {
        this.getLogStream().println(x);
    }

    public void println(char x) {
        this.getLogStream().println(x);
    }

    public void println(int x) {
        this.getLogStream().println(x);
    }

    public void println(long x) {
        this.getLogStream().println(x);
    }

    public void println(float x) {
        this.getLogStream().println(x);
    }

    public void println(double x) {
        this.getLogStream().println(x);
    }

    public void println(char[] x) {
        this.getLogStream().println(x);
    }

    public void println(String x) {
        this.getLogStream().println(x);
    }

    public void println(Object x) {
        this.getLogStream().println(x);
    }

    public void println(String format, Object... params) {
        this.getLogStream().println(format, params);
    }

    public void trace(Object formatNoParams) {
        this.getLogStream().traceWithTraceOffset(1, formatNoParams);
    }

    public void trace(String format, Object... params) {
        this.getLogStream().traceWithTraceOffset(1, format, params);
    }

    public void debugln(Object formatNoParams) {
        this.getLogStream().debuglnWithTraceOffset(1, formatNoParams);
    }

    public void debugln(String format, Object... params) {
        this.getLogStream().debuglnWithTraceOffset(1, format, params);
    }

    public void debugOnceln(Object formatNoParams) {
        this.getLogStream().debugOncelnWithTraceOffset(1, formatNoParams);
    }

    public void debugOnceln(String format, Object... params) {
        this.getLogStream().debugOncelnWithTraceOffset(1, format, params);
    }

    public void noise(Object formatNoParams) {
        this.getLogStream().noiseWithTraceOffset(1, formatNoParams);
    }

    public void noise(String format, Object... params) {
        this.getLogStream().noiseWithTraceOffset(1, format, params);
    }

    public void warn(Object formatNoParams) {
        this.getLogStream().warnWithTraceOffset(1, formatNoParams);
    }

    public void warn(String format, Object... params) {
        this.getLogStream().warnWithTraceOffset(1, format, params);
    }

    public void warnOnce(Object formatNoParams) {
        this.getLogStream().warnOnceWithTraceOffset(1, formatNoParams);
    }

    public void warnOnce(String format, Object... params) {
        this.getLogStream().warnOnceWithTraceOffset(1, format, params);
    }

    public void error(Object formatNoParams) {
        this.getLogStream().errorWithTraceOffset(1, formatNoParams);
    }

    public void error(String format, Object... params) {
        this.getLogStream().errorWithTraceOffset(1, format, params);
    }

    public void write(LogSeverity logSeverity, String logText) {
        this.routedWrite(1, logSeverity, logText);
    }

    public void routedWrite(int backTraceOffset, LogSeverity logSeverity, String logText) {
        switch (logSeverity) {
            case Trace:
                this.getLogStream().traceWithTraceOffset(backTraceOffset + 1, logText);
                break;
            case Noise:
                this.getLogStream().noiseWithTraceOffset(backTraceOffset + 1, logText);
                break;
            case Debug:
                this.getLogStream().debuglnWithTraceOffset(backTraceOffset + 1, logText);
                break;
            case General:
                this.getLogStream().println(logText);
                break;
            case Warning:
                this.getLogStream().warnWithTraceOffset(backTraceOffset + 1, logText);
                break;
            case Error:
                this.getLogStream().errorWithTraceOffset(backTraceOffset + 1, logText);
            case Off:
        }
    }

    public void printException(Throwable ex, LogSeverity logSeverity) {
        this.getLogStream().printException(ex, null, DebugLogStream.generateCallerPrefix(), logSeverity);
    }

    public void printException(Throwable ex, String message, LogSeverity logSeverity) {
        this.getLogStream().printException(ex, message, DebugLogStream.generateCallerPrefix(), logSeverity);
    }

    public void printException(Throwable ex, LogSeverity logSeverity, String messageFormat, Object... params) {
        this.getLogStream().printException(ex, logSeverity, DebugLogStream.generateCallerPrefix(), messageFormat, logSeverity);
    }

    public void printStackTrace() {
        this.getLogStream().printStackTrace(LogSeverity.Error, 1, -1, null);
    }

    public void printStackTrace(String message) {
        this.getLogStream().printStackTrace(LogSeverity.Error, 1, -1, message);
    }

    public void printStackTrace(LogSeverity severity, int depth, String messageFormat, Object... params) {
        this.getLogStream().printStackTrace(severity, 1, depth, messageFormat, params);
    }
}
