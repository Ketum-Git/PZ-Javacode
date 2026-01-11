// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.lua;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class LuaComponentScript extends ComponentScript {
    private LuaComponentScript() {
        super(ComponentType.Lua);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public boolean isoMasterOnly() {
        return true;
    }

    @Override
    public void PreReload() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
    }
}
