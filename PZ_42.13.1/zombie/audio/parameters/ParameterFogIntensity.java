// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterFogIntensity extends FMODGlobalParameter {
    public ParameterFogIntensity() {
        super("FogIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        return ClimateManager.getInstance().getFogIntensity();
    }
}
