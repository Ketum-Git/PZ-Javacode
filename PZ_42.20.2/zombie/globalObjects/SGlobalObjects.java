// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.IOException;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.TableNetworkUtils;

@UsedFromLua
public final class SGlobalObjects {
    protected static final ArrayList<SGlobalObjectSystem> systems = new ArrayList<>();

    public static void noise(String message) {
        if (Core.debug) {
            DebugLog.log("SGlobalObjects: " + message);
        }
    }

    public static SGlobalObjectSystem registerSystem(String name) {
        SGlobalObjectSystem system = getSystemByName(name);
        if (system == null) {
            system = newSystem(name);
            system.load();
        }

        return system;
    }

    public static SGlobalObjectSystem newSystem(String name) throws IllegalStateException {
        if (getSystemByName(name) != null) {
            throw new IllegalStateException("system with that name already exists");
        } else {
            noise("newSystem " + name);
            SGlobalObjectSystem system = new SGlobalObjectSystem(name);
            systems.add(system);
            return system;
        }
    }

    public static int getSystemCount() {
        return systems.size();
    }

    public static SGlobalObjectSystem getSystemByIndex(int index) {
        return index >= 0 && index < systems.size() ? systems.get(index) : null;
    }

    public static SGlobalObjectSystem getSystemByName(String name) {
        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            if (system.name.equals(name)) {
                return system;
            }
        }

        return null;
    }

    public static void update() {
        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            system.update();
        }
    }

    public static void chunkLoaded(int wx, int wy) {
        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            system.chunkLoaded(wx, wy);
        }
    }

    public static void initSystems() {
        if (!GameClient.client) {
            LuaEventManager.triggerEvent("OnSGlobalObjectSystemInit");
            if (!GameServer.server) {
                try {
                    synchronized (SliceY.SliceBufferLock) {
                        SliceY.SliceBuffer.clear();
                        saveInitialStateForClient(SliceY.sliceBufferWriter);
                        SliceY.SliceBuffer.flip();
                        CGlobalObjects.loadInitialState(SliceY.sliceBufferReader);
                    }
                } catch (Throwable var3) {
                    ExceptionLogger.logException(var3);
                }
            }
        }
    }

    public static void saveInitialStateForClient(ByteBufferWriter bb) throws IOException {
        bb.putByte(systems.size());

        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            bb.putUTF(system.name);
            KahluaTable tbl = system.getInitialStateForClient();
            if (tbl == null) {
                tbl = LuaManager.platform.newTable();
            }

            KahluaTable objectsTable = LuaManager.platform.newTable();
            tbl.rawset("_objects", objectsTable);

            for (int j = 0; j < system.getObjectCount(); j++) {
                GlobalObject globalObject = system.getObjectByIndex(j);
                KahluaTable objTable = LuaManager.platform.newTable();
                objTable.rawset("x", BoxedStaticValues.toDouble(globalObject.getX()));
                objTable.rawset("y", BoxedStaticValues.toDouble(globalObject.getY()));
                objTable.rawset("z", BoxedStaticValues.toDouble(globalObject.getZ()));

                for (String key : system.objectSyncKeys) {
                    objTable.rawset(key, globalObject.getModData().rawget(key));
                }

                objectsTable.rawset(j + 1, objTable);
            }

            if (bb.putBoolean(!tbl.isEmpty())) {
                TableNetworkUtils.save(tbl, bb);
            }
        }
    }

    public static boolean receiveClientCommand(String systemName, String command, IsoPlayer playerObj, KahluaTable args) {
        noise("receiveClientCommand " + systemName + " " + command + " OnlineID=" + playerObj.getOnlineID());
        SGlobalObjectSystem system = getSystemByName(systemName);
        if (system == null) {
            throw new IllegalStateException("system '" + systemName + "' not found");
        } else {
            system.receiveClientCommand(command, playerObj, args);
            return true;
        }
    }

    public static void load() {
    }

    public static void save() {
        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            system.save();
        }
    }

    public static void OnIsoObjectChangedItself(String systemName, IsoObject isoObject) {
        if (!GameClient.client) {
            SGlobalObjectSystem system = getSystemByName(systemName);
            if (system != null) {
                system.OnIsoObjectChangedItself(isoObject);
            }
        }
    }

    public static void OnModDataChangeItself(String systemName, IsoObject isoObject) {
        if (!GameClient.client && isoObject != null) {
            SGlobalObjectSystem system = getSystemByName(systemName);
            if (system != null) {
                system.OnModDataChangeItself(isoObject);
            }
        }
    }

    public static void Reset() {
        for (int i = 0; i < systems.size(); i++) {
            SGlobalObjectSystem system = systems.get(i);
            system.Reset();
        }

        systems.clear();
        GlobalObjectLookup.Reset();
    }
}
