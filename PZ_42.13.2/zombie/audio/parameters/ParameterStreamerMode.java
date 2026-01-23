// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterStreamerMode extends FMODGlobalParameter {
    public ParameterStreamerMode() {
        super("StreamerMode");
    }

    @Override
    public float calculateCurrentValue() {
        return Core.getInstance().getOptionStreamerMode() ? 1.0F : 0.0F;
    }
}
