// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.ScriptType;

@UsedFromLua
public final class VehicleTemplate extends BaseScriptObject {
    public String name;
    public String body;
    public VehicleScript script;

    public VehicleTemplate(ScriptModule module, String name, String body) {
        super(ScriptType.VehicleTemplate);
        this.setModule(module);
        this.name = name;
        this.body = body;
    }

    public VehicleScript getScript() {
        if (this.script == null) {
            this.script = new VehicleScript();
            this.script.setModule(this.getModule());

            try {
                this.script.Load(this.name, this.body);
            } catch (Exception var2) {
                var2.printStackTrace();
            }
        }

        return this.script;
    }
}
