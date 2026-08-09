// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterRainIntensity extends FMODGlobalParameter {
    public ParameterRainIntensity() {
        super("RainIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        return ClimateManager.getInstance().getRainIntensity();
    }
}
