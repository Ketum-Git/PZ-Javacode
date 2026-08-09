// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.Lua.LuaManager;
import zombie.entity.GameEntity;
import zombie.scripting.itemConfig.RandomGenerator;
import zombie.util.StringUtils;

public class GeneratorLuaFunc extends RandomGenerator<GeneratorLuaFunc> {
    private final String luaFunc;

    public GeneratorLuaFunc(String luaFunc, float chance) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (StringUtils.isNullOrWhitespace(luaFunc)) {
            throw new IllegalArgumentException("LuaFunc can not be null or empty.");
        } else {
            this.luaFunc = luaFunc;
            this.setChance(chance);
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        Object functionObject = LuaManager.getFunctionObject(this.luaFunc);
        if (functionObject != null) {
            LuaManager.caller.protectedCall(LuaManager.thread, functionObject, entity);
            return true;
        } else {
            return false;
        }
    }

    public GeneratorLuaFunc copy() {
        return new GeneratorLuaFunc(this.luaFunc, this.getChance());
    }
}
