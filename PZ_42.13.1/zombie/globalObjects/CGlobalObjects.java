// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.network.TableNetworkUtils;
import zombie.util.Type;

@UsedFromLua
public final class CGlobalObjects {
    protected static final ArrayList<CGlobalObjectSystem> systems = new ArrayList<>();
    protected static final HashMap<String, KahluaTable> initialState = new HashMap<>();

    public static void noise(String message) {
        if (Core.debug) {
            DebugLog.log("CGlobalObjects: " + message);
        }
    }

    public static CGlobalObjectSystem registerSystem(String name) {
        CGlobalObjectSystem system = getSystemByName(name);
        if (system == null) {
            system = newSystem(name);
            KahluaTable tbl = initialState.get(name);
            if (tbl != null) {
                KahluaTableIterator iterator = tbl.iterator();

                while (iterator.advance()) {
                    Object key = iterator.getKey();
                    Object value = iterator.getValue();
                    if ("_objects".equals(key)) {
                        KahluaTable objectsTable = Type.tryCastTo(value, KahluaTable.class);
                        int i = 1;

                        for (int n = objectsTable.len(); i <= n; i++) {
                            KahluaTable objTable = Type.tryCastTo(objectsTable.rawget(i), KahluaTable.class);
                            int x = ((Double)objTable.rawget("x")).intValue();
                            int y = ((Double)objTable.rawget("y")).intValue();
                            int z = ((Double)objTable.rawget("z")).intValue();
                            objTable.rawset("x", null);
                            objTable.rawset("y", null);
                            objTable.rawset("z", null);
                            CGlobalObject object = Type.tryCastTo(system.newObject(x, y, z), CGlobalObject.class);
                            KahluaTableIterator it = objTable.iterator();

                            while (it.advance()) {
                                object.getModData().rawset(it.getKey(), it.getValue());
                            }
                        }

                        objectsTable.wipe();
                    } else {
                        system.modData.rawset(key, value);
                    }
                }
            }
        }

        return system;
    }

    public static CGlobalObjectSystem newSystem(String name) throws IllegalStateException {
        if (getSystemByName(name) != null) {
            throw new IllegalStateException("system with that name already exists");
        } else {
            noise("newSystem " + name);
            CGlobalObjectSystem system = new CGlobalObjectSystem(name);
            systems.add(system);
            return system;
        }
    }

    public static int getSystemCount() {
        return systems.size();
    }

    public static CGlobalObjectSystem getSystemByIndex(int index) {
        return index >= 0 && index < systems.size() ? systems.get(index) : null;
    }

    public static CGlobalObjectSystem getSystemByName(String name) {
        for (int i = 0; i < systems.size(); i++) {
            CGlobalObjectSystem system = systems.get(i);
            if (system.name.equals(name)) {
                return system;
            }
        }

        return null;
    }

    public static void initSystems() {
        LuaEventManager.triggerEvent("OnCGlobalObjectSystemInit");
    }

    public static void loadInitialState(ByteBuffer bb) throws IOException {
        int count = bb.get();

        for (int i = 0; i < count; i++) {
            String systemName = GameWindow.ReadStringUTF(bb);
            if (bb.get() != 0) {
                KahluaTable tbl = LuaManager.platform.newTable();
                initialState.put(systemName, tbl);
                TableNetworkUtils.load(tbl, bb);
            }
        }
    }

    public static boolean receiveServerCommand(String systemName, String command, KahluaTable args) {
        CGlobalObjectSystem system = getSystemByName(systemName);
        if (system == null) {
            throw new IllegalStateException("system '" + systemName + "' not found");
        } else {
            system.receiveServerCommand(command, args);
            return true;
        }
    }

    public static void Reset() {
        for (int i = 0; i < systems.size(); i++) {
            CGlobalObjectSystem system = systems.get(i);
            system.Reset();
        }

        systems.clear();
        initialState.clear();
        CGlobalObjectNetwork.Reset();
    }
}
