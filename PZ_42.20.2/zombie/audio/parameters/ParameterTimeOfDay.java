// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.GameTime;
import zombie.audio.FMODGlobalParameter;
import zombie.iso.weather.ClimateManager;

public final class ParameterTimeOfDay extends FMODGlobalParameter {
    public ParameterTimeOfDay() {
        super("TimeOfDay");
    }

    @Override
    public float calculateCurrentValue() {
        ClimateManager.DayInfo currentDay = ClimateManager.getInstance().getCurrentDay();
        if (currentDay == null) {
            return 1.0F;
        } else {
            float dawn = currentDay.season.getDawn();
            float dusk = currentDay.season.getDusk();
            float noon = currentDay.season.getDayHighNoon();
            float timeOfDay = GameTime.instance.getTimeOfDay();
            if (timeOfDay >= dawn - 1.0F && timeOfDay < dawn + 1.0F) {
                return 0.0F;
            } else if (timeOfDay >= dawn + 1.0F && timeOfDay < dawn + 2.0F) {
                return 1.0F;
            } else if (timeOfDay >= dawn + 2.0F && timeOfDay < dusk - 2.0F) {
                return 2.0F;
            } else if (timeOfDay >= dusk - 2.0F && timeOfDay < dusk - 1.0F) {
                return 3.0F;
            } else {
                return timeOfDay >= dusk - 1.0F && timeOfDay < dusk + 1.0F ? 4.0F : 5.0F;
            }
        }
    }
}
