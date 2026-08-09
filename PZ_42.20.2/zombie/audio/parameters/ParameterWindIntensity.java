// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterWindIntensity extends FMODGlobalParameter {
    public ParameterWindIntensity() {
        super("WindIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        float value = ClimateManager.getInstance().getWindIntensity();
        return (int)(value * 1000.0F) / 1000.0F;
    }
}
