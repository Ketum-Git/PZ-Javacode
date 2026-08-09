// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.lua;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class LuaComponentScript extends ComponentScript {
    private LuaComponentScript() {
        super(ComponentType.Lua);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }
}
