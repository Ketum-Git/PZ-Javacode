// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.parts;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class PartsScript extends ComponentScript {
    private PartsScript() {
        super(ComponentType.Parts);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }
}
