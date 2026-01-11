// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity;

import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;

@DebugClassFields
@UsedFromLua
public abstract class ComponentScript extends BaseScriptObject {
    public final ComponentType type;

    protected ComponentScript(ComponentType type) {
        super(ScriptType.EntityComponent);
        this.type = type;
        this.InitLoadPP(type.toString());
    }

    public boolean isoMasterOnly() {
        return true;
    }

    public String getName() {
        if (!Core.debug || this.getParent() != null && this.getParent().getScriptObjectName() != null) {
            return this.getParent() != null ? this.getParent().getScriptObjectName() : "UnknownScriptName";
        } else {
            throw new RuntimeException("Parent is null or parent name is null.");
        }
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    protected abstract <T extends ComponentScript> void copyFrom(T var1);

    protected void load(ScriptParser.Block block) throws Exception {
    }

    protected boolean parseKeyValue(String k, String v) {
        throw new RuntimeException("'parseKeyValue' not implemented for " + this.getClass());
    }
}
