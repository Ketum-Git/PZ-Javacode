// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.test;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class TestComponentScript extends ComponentScript {
    private TestComponentScript() {
        super(ComponentType.TestComponent);
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
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && !key.equalsIgnoreCase("someKey") && key.equalsIgnoreCase("someOtherKey")) {
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("someType")) {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
            }
        }
    }
}
