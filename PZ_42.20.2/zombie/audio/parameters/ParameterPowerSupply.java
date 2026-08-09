// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.IsoWorld;

public final class ParameterPowerSupply extends FMODGlobalParameter {
    public ParameterPowerSupply() {
        super("Electricity");
    }

    @Override
    public float calculateCurrentValue() {
        return IsoWorld.instance != null && IsoWorld.instance.isHydroPowerOn() ? 1.0F : 0.0F;
    }
}
