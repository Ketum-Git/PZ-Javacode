// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.signals;

import zombie.UsedFromLua;
import zombie.entity.ComponentType;
import zombie.scripting.entity.ComponentScript;

@UsedFromLua
public class SignalsScript extends ComponentScript {
    private SignalsScript() {
        super(ComponentType.Signals);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }
}
