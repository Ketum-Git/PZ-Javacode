// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.SandboxOptions;
import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterWeatherEvent extends FMODGlobalParameter {
    private final ParameterWeatherEvent.Event event = ParameterWeatherEvent.Event.None;

    public ParameterWeatherEvent() {
        super("WeatherEvent");
    }

    @Override
    public float calculateCurrentValue() {
        float snowOnGroundZeroToOne = ClimateManager.getInstance().getSnowFracNow();
        if (!SandboxOptions.instance.enableSnowOnGround.getValue()) {
            snowOnGroundZeroToOne = 0.0F;
        }

        return this.event.value;
    }

    public static enum Event {
        None(0),
        FreshSnow(1);

        final int value;

        private Event(final int value) {
            this.value = value;
        }
    }
}
