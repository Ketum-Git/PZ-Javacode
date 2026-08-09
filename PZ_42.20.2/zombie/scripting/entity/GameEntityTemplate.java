// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity;

import zombie.UsedFromLua;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

@DebugClassFields
@UsedFromLua
public class GameEntityTemplate extends BaseScriptObject {
    public String name;
    public String body;
    public GameEntityScript script;

    public GameEntityTemplate(ScriptModule module, String name, String body) {
        super(ScriptType.EntityTemplate);
        this.setModule(module);
        this.name = name;
        this.body = body;
    }

    @Override
    public void Load(String name, String body) {
    }

    public GameEntityScript getScript() throws Exception {
        if (this.script == null) {
            this.script = new GameEntityScript();
            this.script.setModule(this.getModule());
            this.script.Load(this.name, this.body);
        }

        return this.script;
    }
}
