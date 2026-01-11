// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterTemperature extends FMODGlobalParameter {
    public ParameterTemperature() {
        super("Temperature");
    }

    @Override
    public float calculateCurrentValue() {
        return (int)(ClimateManager.getInstance().getTemperature() * 100.0F) / 100.0F;
    }
}
