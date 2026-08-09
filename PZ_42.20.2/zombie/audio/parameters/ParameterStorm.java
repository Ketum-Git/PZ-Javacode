// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.WeatherPeriod;

public final class ParameterStorm extends FMODGlobalParameter {
    public ParameterStorm() {
        super("Storm");
    }

    @Override
    public float calculateCurrentValue() {
        WeatherPeriod weatherPeriod = ClimateManager.getInstance().getWeatherPeriod();
        if (weatherPeriod.isRunning()) {
            if (weatherPeriod.isThunderStorm()) {
                return 1.0F;
            }

            if (weatherPeriod.isTropicalStorm()) {
                return 2.0F;
            }

            if (weatherPeriod.isBlizzard()) {
                return 3.0F;
            }
        }

        return 0.0F;
    }
}
