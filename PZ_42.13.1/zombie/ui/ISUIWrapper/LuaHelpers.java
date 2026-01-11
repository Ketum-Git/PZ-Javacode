// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui.ISUIWrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;

public class LuaHelpers {
    public static Object callLuaClass(String type, String function, KahluaTable context, Object... args) {
        Object[] result = callLuaClassReturnMultiple(type, function, context, args);
        return result == null ? null : result[0];
    }

    public static Object[] callLuaClassReturnMultiple(String type, String function, KahluaTable context, Object... args) {
        Object classObject = LuaManager.get(type);
        Object functionObject = LuaManager.getFunctionObject(type + "." + function);
        if (function.equals("new") && context == null) {
            context = (KahluaTable)classObject;
        }

        Object[] arguments = null;
        if (context == null) {
            arguments = args;
        } else {
            arguments = new Object[args.length + 1];
            arguments[0] = context;

            for (int i = 0; i < args.length; i++) {
                arguments[i + 1] = args[i];
            }
        }

        LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
        if (result.isSuccess() && !result.isEmpty()) {
            Object[] output = new Object[result.size()];

            for (int i = 0; i < result.size(); i++) {
                output[i] = result.get(i);
            }

            return output;
        } else {
            return null;
        }
    }

    public static KahluaTable getJoypadState(double playerNum) {
        KahluaTable JoypadState = (KahluaTable)LuaManager.env.rawget("JoypadState");
        KahluaTable JoypadState_players = (KahluaTable)JoypadState.rawget("players");
        return (KahluaTable)JoypadState_players.rawget(playerNum + 1.0);
    }

    public static boolean castBoolean(Object luaObject) {
        Boolean result = (Boolean)luaObject;
        return result == null ? false : result;
    }

    public static Double castDouble(Object luaObject) {
        Double result = (Double)luaObject;
        return result == null ? 0.0 : result;
    }

    public static String castString(Object luaObject) {
        String result = (String)luaObject;
        return result == null ? "" : result;
    }

    public static boolean tableContainsKey(KahluaTable table, Object value) {
        if (table == null) {
            return false;
        } else {
            KahluaTableIterator iterator = table.iterator();

            while (iterator.advance()) {
                if (iterator.getKey() == value) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean tableContainsValue(KahluaTable table, Object value) {
        if (table == null) {
            return false;
        } else {
            KahluaTableIterator iterator = table.iterator();

            while (iterator.advance()) {
                if (iterator.getValue() == value) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void tableSort(KahluaTable table, Comparator<Entry<Object, Object>> comparator) {
        ArrayList<Entry<Object, Object>> tempList = new ArrayList<>();
        KahluaTableIterator iterator = table.iterator();

        while (iterator.advance()) {
            if (iterator.getKey() != null) {
                tempList.add(Map.entry(iterator.getKey(), iterator.getValue()));
            }
        }

        tempList.sort(comparator);
        table.wipe();

        for (Entry<Object, Object> entry : tempList) {
            table.rawset(entry.getKey(), entry.getValue());
        }
    }

    public static KahluaTable getPlayerContextMenu(double id) {
        KahluaTable data = getPlayerData(id);
        return data != null ? (KahluaTable)data.rawget("contextMenu") : null;
    }

    public static KahluaTable getPlayerData(double id) {
        KahluaTable ISPlayerData = (KahluaTable)LuaManager.env.rawget("ISPlayerData");
        return (KahluaTable)ISPlayerData.rawget(id + 1.0);
    }
}
