// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.ServerMap;

@UsedFromLua
public final class MapObjects {
    private static final HashMap<String, MapObjects.Callback> onNew = new HashMap<>();
    private static final HashMap<String, MapObjects.Callback> onLoad = new HashMap<>();
    private static final ArrayList<IsoObject> tempObjects = new ArrayList<>();
    private static final Object[] params = new Object[1];

    private static MapObjects.Callback getOnNew(String spriteName) {
        MapObjects.Callback callback = onNew.get(spriteName);
        if (callback == null) {
            callback = new MapObjects.Callback(spriteName);
            onNew.put(spriteName, callback);
        }

        return callback;
    }

    public static void OnNewWithSprite(String spriteName, LuaClosure function, int priority) {
        if (spriteName != null && !spriteName.isEmpty()) {
            if (function == null) {
                throw new NullPointerException("function is null");
            } else {
                MapObjects.Callback callback = getOnNew(spriteName);

                for (int i = 0; i < callback.functions.size(); i++) {
                    if (callback.priority.get(i) < priority) {
                        callback.functions.add(i, function);
                        callback.priority.insert(i, (short)priority);
                        return;
                    }

                    if (callback.priority.get(i) == priority) {
                        callback.functions.set(i, function);
                        callback.priority.set(i, (short)priority);
                        return;
                    }
                }

                callback.functions.add(function);
                callback.priority.add((short)priority);
            }
        } else {
            throw new IllegalArgumentException("invalid sprite name");
        }
    }

    public static void OnNewWithSprite(KahluaTable spriteNames, LuaClosure function, int priority) {
        if (spriteNames != null && !spriteNames.isEmpty()) {
            if (function == null) {
                throw new NullPointerException("function is null");
            } else {
                KahluaTableIterator it = spriteNames.iterator();

                while (it.advance()) {
                    Object value = it.getValue();
                    if (!(value instanceof String s)) {
                        throw new IllegalArgumentException("expected string but got \"" + value + "\"");
                    }

                    OnNewWithSprite(s, function, priority);
                }
            }
        } else {
            throw new IllegalArgumentException("invalid sprite-name table");
        }
    }

    public static void newGridSquare(IsoGridSquare square) {
        if (square != null && !square.getObjects().isEmpty()) {
            tempObjects.clear();

            for (int i = 0; i < square.getObjects().size(); i++) {
                tempObjects.add(square.getObjects().get(i));
            }

            for (int i = 0; i < tempObjects.size(); i++) {
                IsoObject obj = tempObjects.get(i);
                if (square.getObjects().contains(obj) && !(obj instanceof IsoWorldInventoryObject) && obj != null && obj.sprite != null) {
                    String spriteName = obj.sprite.name == null ? obj.spriteName : obj.sprite.name;
                    if (spriteName != null && !spriteName.isEmpty()) {
                        MapObjects.Callback callback = onNew.get(spriteName);
                        if (callback != null) {
                            params[0] = obj;

                            for (int n = 0; n < callback.functions.size(); n++) {
                                try {
                                    LuaManager.caller.protectedCallVoid(LuaManager.thread, callback.functions.get(n), params);
                                } catch (Throwable var7) {
                                    ExceptionLogger.logException(var7);
                                }

                                spriteName = obj.sprite != null && obj.sprite.name != null ? obj.sprite.name : obj.spriteName;
                                if (!square.getObjects().contains(obj) || obj.sprite == null || !callback.spriteName.equals(spriteName)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static MapObjects.Callback getOnLoad(String spriteName) {
        MapObjects.Callback callback = onLoad.get(spriteName);
        if (callback == null) {
            callback = new MapObjects.Callback(spriteName);
            onLoad.put(spriteName, callback);
        }

        return callback;
    }

    public static void OnLoadWithSprite(String spriteName, LuaClosure function, int priority) {
        if (spriteName != null && !spriteName.isEmpty()) {
            if (function == null) {
                throw new NullPointerException("function is null");
            } else {
                MapObjects.Callback callback = getOnLoad(spriteName);

                for (int i = 0; i < callback.functions.size(); i++) {
                    if (callback.priority.get(i) < priority) {
                        callback.functions.add(i, function);
                        callback.priority.insert(i, (short)priority);
                        return;
                    }

                    if (callback.priority.get(i) == priority) {
                        callback.functions.set(i, function);
                        callback.priority.set(i, (short)priority);
                        return;
                    }
                }

                callback.functions.add(function);
                callback.priority.add((short)priority);
            }
        } else {
            throw new IllegalArgumentException("invalid sprite name");
        }
    }

    public static void OnLoadWithSprite(KahluaTable spriteNames, LuaClosure function, int priority) {
        if (spriteNames != null && !spriteNames.isEmpty()) {
            if (function == null) {
                throw new NullPointerException("function is null");
            } else {
                KahluaTableIterator it = spriteNames.iterator();

                while (it.advance()) {
                    Object value = it.getValue();
                    if (!(value instanceof String s)) {
                        throw new IllegalArgumentException("expected string but got \"" + value + "\"");
                    }

                    OnLoadWithSprite(s, function, priority);
                }
            }
        } else {
            throw new IllegalArgumentException("invalid sprite-name table");
        }
    }

    public static void loadGridSquare(IsoGridSquare square) {
        if (square != null && !square.getObjects().isEmpty()) {
            tempObjects.clear();

            for (int i = 0; i < square.getObjects().size(); i++) {
                tempObjects.add(square.getObjects().get(i));
            }

            for (int i = 0; i < tempObjects.size(); i++) {
                IsoObject obj = tempObjects.get(i);
                if (square.getObjects().contains(obj) && !(obj instanceof IsoWorldInventoryObject) && obj != null && obj.sprite != null) {
                    String spriteName = obj.sprite.name == null ? obj.spriteName : obj.sprite.name;
                    if (spriteName != null && !spriteName.isEmpty()) {
                        MapObjects.Callback callback = onLoad.get(spriteName);
                        if (callback != null) {
                            params[0] = obj;

                            for (int n = 0; n < callback.functions.size(); n++) {
                                try {
                                    LuaManager.caller.protectedCallVoid(LuaManager.thread, callback.functions.get(n), params);
                                } catch (Throwable var7) {
                                    ExceptionLogger.logException(var7);
                                }

                                spriteName = obj.sprite != null && obj.sprite.name != null ? obj.sprite.name : obj.spriteName;
                                if (!square.getObjects().contains(obj) || obj.sprite == null || !callback.spriteName.equals(spriteName)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void debugNewSquare(int x, int y, int z) {
        if (Core.debug) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (square != null) {
                newGridSquare(square);
            }
        }
    }

    public static void debugLoadSquare(int x, int y, int z) {
        if (Core.debug) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (square != null) {
                loadGridSquare(square);
            }
        }
    }

    public static void debugLoadChunk(int wx, int wy) {
        if (Core.debug) {
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
            if (chunk != null) {
                for (int z = 0; z <= chunk.maxLevel; z++) {
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            IsoGridSquare square = chunk.getGridSquare(x, y, z);
                            if (square != null && !square.getObjects().isEmpty()) {
                                loadGridSquare(square);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void reroute(Prototype prototype, LuaClosure luaClosure) {
        for (MapObjects.Callback callback : onNew.values()) {
            for (int m = 0; m < callback.functions.size(); m++) {
                LuaClosure c = callback.functions.get(m);
                if (c.prototype.filename.equals(prototype.filename) && c.prototype.name.equals(prototype.name)) {
                    callback.functions.set(m, luaClosure);
                }
            }
        }
    }

    public static void Reset() {
        onNew.clear();
        onLoad.clear();
    }

    private static final class Callback {
        final String spriteName;
        final ArrayList<LuaClosure> functions = new ArrayList<>();
        final TShortArrayList priority = new TShortArrayList();

        Callback(String spriteName) {
            this.spriteName = spriteName;
        }
    }
}
