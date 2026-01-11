// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.iso.BuildingDef;

@UsedFromLua
public final class Trigger {
    public BuildingDef def;
    public int triggerRange;
    public int zombieExclusionRange;
    public String type;
    public boolean triggered;
    public KahluaTable data;

    public Trigger(BuildingDef def, int triggerRange, int zombieExclusionRange, String type) {
        this.def = def;
        this.triggerRange = triggerRange;
        this.zombieExclusionRange = zombieExclusionRange;
        this.type = type;
        this.data = LuaManager.platform.newTable();
    }

    public KahluaTable getModData() {
        return this.data;
    }
}
