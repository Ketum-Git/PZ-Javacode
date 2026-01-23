// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import java.util.ArrayList;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.GameProfiler;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;

public final class Event {
    public static final int ADD = 0;
    public static final int NUM_FUNCTIONS = 1;
    private final Event.Add add;
    private final Event.Remove remove;
    public final ArrayList<LuaClosure> callbacks = new ArrayList<>();
    public String name;
    private int index;

    public boolean trigger(KahluaTable env, LuaCaller caller, Object[] params) {
        if (this.callbacks.isEmpty()) {
            return false;
        } else {
            GameProfiler profiler = GameProfiler.getInstance();
            if (DebugOptions.instance.checks.slowLuaEvents.getValue()) {
                for (int n = 0; n < this.callbacks.size(); n++) {
                    LuaClosure closure = this.callbacks.get(n);

                    try (GameProfiler.ProfileArea ignored = profiler.profile("Lua - " + this.name)) {
                        long start = System.nanoTime();
                        caller.protectedCallVoid(LuaManager.thread, closure, params);
                        double delayMS = (System.nanoTime() - start) / 1000000.0;
                        if (delayMS > 250.0) {
                            DebugLog.Lua.warn("SLOW Lua event callback %s %s %dms", closure.prototype.file, closure, (int)delayMS);
                        }
                    } catch (Exception var15) {
                        ExceptionLogger.logException(var15);
                    }

                    if (!this.callbacks.contains(closure)) {
                        n--;
                    }
                }

                return true;
            } else {
                for (int n = 0; n < this.callbacks.size(); n++) {
                    LuaClosure closure = this.callbacks.get(n);

                    try (GameProfiler.ProfileArea ignoredx = profiler.profile("Lua - " + this.name)) {
                        caller.protectedCallVoid(LuaManager.thread, closure, params);
                    } catch (Exception var17) {
                        ExceptionLogger.logException(var17);
                    }

                    if (!this.callbacks.contains(closure)) {
                        n--;
                    }
                }

                return true;
            }
        }
    }

    public Event(String name, int index) {
        this.index = index;
        this.name = name;
        this.add = new Event.Add(this);
        this.remove = new Event.Remove(this);
    }

    public void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        table.rawset("Add", this.add);
        table.rawset("Remove", this.remove);
        environment.rawset(this.name, table);
    }

    public static final class Add implements JavaFunction {
        Event e;

        public Add(Event e) {
            this.e = e;
        }

        /**
         * Description copied from interface: se.krka.kahlua.vm.JavaFunction
         * @return N, number of return values. The top N objects on the stack are considered the return values.
         */
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (LuaCompiler.rewriteEvents) {
                return 0;
            } else {
                Object param = callFrame.get(0);
                if (this.e.name.contains("CreateUI")) {
                    boolean tab = false;
                }

                if (param instanceof LuaClosure tab) {
                    this.e.callbacks.add(tab);
                }

                return 0;
            }
        }
    }

    public static final class Remove implements JavaFunction {
        Event e;

        public Remove(Event e) {
            this.e = e;
        }

        /**
         * Description copied from interface: se.krka.kahlua.vm.JavaFunction
         * @return N, number of return values. The top N objects on the stack are considered the return values.
         */
        @Override
        public int call(LuaCallFrame callFrame, int nArguments) {
            if (LuaCompiler.rewriteEvents) {
                return 0;
            } else {
                if (callFrame.get(0) instanceof LuaClosure tab) {
                    this.e.callbacks.remove(tab);
                }

                return 0;
            }
        }
    }
}
