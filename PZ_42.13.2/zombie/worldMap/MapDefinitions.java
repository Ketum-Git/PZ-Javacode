// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.util.list.PZArrayUtil;

public final class MapDefinitions {
    private static MapDefinitions instance;
    private final ArrayList<String> definitions = new ArrayList<>();

    public static MapDefinitions getInstance() {
        if (instance == null) {
            instance = new MapDefinitions();
        }

        return instance;
    }

    public String pickRandom() {
        if (this.definitions.isEmpty()) {
            this.initDefinitionsFromLua();
        }

        return this.definitions.isEmpty() ? "Default" : PZArrayUtil.pickRandom(this.definitions);
    }

    private void initDefinitionsFromLua() {
        if (LuaManager.env.rawget("LootMaps") instanceof KahluaTable LootMaps) {
            if (LootMaps.rawget("Init") instanceof KahluaTable Init) {
                KahluaTableIterator var7 = Init.iterator();

                while (var7.advance()) {
                    if (var7.getKey() instanceof String mapID) {
                        this.definitions.add(mapID);
                    }
                }
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.definitions.clear();
            instance = null;
        }
    }
}
