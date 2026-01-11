// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.PrintStream;
import zombie.UsedFromLua;

/**
 * Created by LEMMYPC on 31/12/13.
 */
@UsedFromLua
public enum DebugType {
    Packet,
    NetworkFileDebug,
    Network,
    General,
    DetailedInfo,
    Lua,
    Mod,
    Sound,
    Zombie,
    Combat,
    Objects,
    Fireplace,
    Radio,
    MapLoading,
    Clothing,
    Animation,
    AnimationDetailed,
    Asset,
    Script,
    Shader,
    Sprite,
    Input,
    Recipe,
    ActionSystem,
    ActionSystemEvents,
    IsoRegion,
    FileIO,
    Multiplayer,
    Damage,
    Death,
    Discord,
    Statistic,
    Vehicle,
    Voice,
    Checksum,
    Animal,
    ItemPicker,
    CraftLogic,
    Action,
    Entity,
    Lightning,
    Grapple,
    ExitDebug,
    BodyDamage,
    Xml,
    Physics,
    Ballistics,
    Ragdoll,
    PZBullet,
    ModelManager,
    LoadAnimation,
    Zone,
    WorldGen,
    Foraging,
    Saving,
    Fluid,
    Energy,
    Translation,
    Moveable,
    Basement,
    FallDamage,
    ImGui,
    CharacterTrait;

    public static final DebugType Default = General;

    public boolean isEnabled() {
        return DebugLog.isEnabled(this);
    }

    public DebugLogStream getLogStream() {
        return DebugLog.getOrCreateDebugLogStream(this);
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

    public void trace(Object in_formatNoParams) {
        this.getLogStream().trace(1, in_formatNoParams);
    }

    public void trace(String in_format, Object... in_params) {
        this.getLogStream().trace(1, in_format, in_params);
    }

    public void debugln(Object in_formatNoParams) {
        this.getLogStream().debugln(1, in_formatNoParams);
    }

    public void debugln(String in_format, Object... in_params) {
        this.getLogStream().debugln(1, in_format, in_params);
    }

    public void debugOnceln(Object in_formatNoParams) {
        this.getLogStream().debugOnceln(1, in_formatNoParams);
    }

    public void debugOnceln(String in_format, Object... in_params) {
        this.getLogStream().debugOnceln(1, in_format, in_params);
    }

    public void noise(Object in_formatNoParams) {
        this.getLogStream().noise(1, in_formatNoParams);
    }

    public void noise(String in_format, Object... in_params) {
        this.getLogStream().noise(1, in_format, in_params);
    }

    public void warn(Object in_formatNoParams) {
        this.getLogStream().warn(1, in_formatNoParams);
    }

    public void warn(String in_format, Object... in_params) {
        this.getLogStream().warn(1, in_format, in_params);
    }

    public void error(Object in_formatNoParams) {
        this.getLogStream().error(1, in_formatNoParams);
    }

    public void error(String in_format, Object... in_params) {
        this.getLogStream().error(1, in_format, in_params);
    }

    public void write(LogSeverity in_logSeverity, String in_logText) {
        this.routedWrite(1, in_logSeverity, in_logText);
    }

    public void routedWrite(int in_backTraceOffset, LogSeverity in_logSeverity, String in_logText) {
        switch (in_logSeverity) {
            case Trace:
                this.getLogStream().trace(in_backTraceOffset + 1, in_logText);
                break;
            case Noise:
                this.getLogStream().noise(in_backTraceOffset + 1, in_logText);
                break;
            case Debug:
                this.getLogStream().debugln(in_backTraceOffset + 1, in_logText);
                break;
            case General:
                this.getLogStream().println(in_logText);
                break;
            case Warning:
                this.getLogStream().warn(in_backTraceOffset + 1, in_logText);
                break;
            case Error:
                this.getLogStream().error(in_backTraceOffset + 1, in_logText);
            case Off:
        }
    }

    public StackTraceContainer getStackTrace(LogSeverity in_logSeverity, String in_indent, int in_depth) {
        return this.getStackTrace(in_logSeverity, in_indent, 3, in_depth);
    }

    public StackTraceContainer getStackTrace(LogSeverity in_logSeverity, String in_indent, int in_depthStart, int in_depthCount) {
        if (!this.isEnabled()) {
            return null;
        } else if (!this.getLogStream().isLogEnabled(in_logSeverity)) {
            return null;
        } else {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            return new StackTraceContainer(stackTraceElements, in_indent, in_depthStart, in_depthCount);
        }
    }

    public void printException(Exception in_ex, String in_message, LogSeverity in_logSeverity) {
        this.getLogStream().printException(in_ex, in_message, DebugLogStream.generateCallerPrefix(), in_logSeverity);
    }
}
