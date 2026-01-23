// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Platform;
import zombie.debug.DebugLog;

public final class LuaHookManager implements JavaFunction {
    public static final ArrayList<LuaClosure> OnTickCallbacks = new ArrayList<>();
    static Object[] a = new Object[1];
    static Object[] b = new Object[2];
    static Object[] c = new Object[3];
    static Object[] d = new Object[4];
    static Object[] f = new Object[5];
    static Object[] g = new Object[6];
    private static final ArrayList<Event> EventList = new ArrayList<>();
    private static final HashMap<String, Event> EventMap = new HashMap<>();

    public static boolean TriggerHook(String event) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            a[0] = null;
            return e.trigger(LuaManager.env, LuaManager.caller, a);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            a[0] = param1;
            return e.trigger(LuaManager.env, LuaManager.caller, a);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1, Object param2) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            b[0] = param1;
            b[1] = param2;
            return e.trigger(LuaManager.env, LuaManager.caller, b);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            c[0] = param1;
            c[1] = param2;
            c[2] = param3;
            return e.trigger(LuaManager.env, LuaManager.caller, c);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            d[0] = param1;
            d[1] = param2;
            d[2] = param3;
            d[3] = param4;
            return e.trigger(LuaManager.env, LuaManager.caller, d);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4, Object param5) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            f[0] = param1;
            f[1] = param2;
            f[2] = param3;
            f[3] = param4;
            f[4] = param5;
            return e.trigger(LuaManager.env, LuaManager.caller, f);
        } else {
            return false;
        }
    }

    public static boolean TriggerHook(String event, Object param1, Object param2, Object param3, Object param4, Object param5, Object param6) {
        if (EventMap.containsKey(event)) {
            Event e = EventMap.get(event);
            g[0] = param1;
            g[1] = param2;
            g[2] = param3;
            g[3] = param4;
            g[4] = param5;
            g[5] = param6;
            return e.trigger(LuaManager.env, LuaManager.caller, g);
        } else {
            return false;
        }
    }

    public static void AddEvent(String name) {
        if (!EventMap.containsKey(name)) {
            Event event = new Event(name, EventList.size());
            EventList.add(event);
            EventMap.put(name, event);
            if (LuaManager.env.rawget("Hook") instanceof KahluaTable table) {
                event.register(LuaManager.platform, table);
            } else {
                DebugLog.log("ERROR: 'Hook' table not found or not a table");
            }
        }
    }

    private static void AddEvents() {
        AddEvent("AutoDrink");
        AddEvent("UseItem");
        AddEvent("Attack");
        AddEvent("CalculateStats");
        AddEvent("ContextualAction");
        AddEvent("WeaponHitCharacter");
        AddEvent("WeaponSwing");
        AddEvent("WeaponSwingHitPoint");
    }

    public static void clear() {
        a[0] = null;
        b[0] = null;
        b[1] = null;
        c[0] = null;
        c[1] = null;
        c[2] = null;
        d[0] = null;
        d[1] = null;
        d[2] = null;
        d[3] = null;
        f[0] = null;
        f[1] = null;
        f[2] = null;
        f[3] = null;
        f[4] = null;
        g[0] = null;
        g[1] = null;
        g[2] = null;
        g[3] = null;
        g[4] = null;
        g[5] = null;
    }

    public static void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        environment.rawset("Hook", table);
        AddEvents();
    }

    public static void Reset() {
        for (Event e : EventList) {
            e.callbacks.clear();
        }

        EventList.clear();
        EventMap.clear();
    }

    @Override
    public int call(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }

    private int OnTick(LuaCallFrame callFrame, int nArguments) {
        return 0;
    }
}
