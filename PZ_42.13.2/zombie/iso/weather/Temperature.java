// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class Temperature {
    public static final boolean DO_DEFAULT_BASE = false;
    public static final boolean DO_DAYLEN_MOD = true;
    public static final String CELSIUS_POSTFIX = "\u00b0C";
    public static final String FAHRENHEIT_POSTFIX = "\u00b0F";
    public static final float skinCelciusMin = 20.0F;
    public static final float skinCelciusFavorable = 33.0F;
    public static final float skinCelciusMax = 42.0F;
    public static final float homeostasisDefault = 37.0F;
    public static final float FavorableNakedTemp = 27.0F;
    public static final float FavorableRoomTemp = 22.0F;
    public static final float coreCelciusMin = 20.0F;
    public static final float coreCelciusMax = 42.0F;
    public static final float neutralZone = 27.0F;
    public static final float Hypothermia_1 = 36.5F;
    public static final float Hypothermia_2 = 35.0F;
    public static final float Hypothermia_3 = 30.0F;
    public static final float Hypothermia_4 = 25.0F;
    public static final float Hyperthermia_1 = 37.5F;
    public static final float Hyperthermia_2 = 39.0F;
    public static final float Hyperthermia_3 = 40.0F;
    public static final float Hyperthermia_4 = 41.0F;
    public static final float TrueInsulationMultiplier = 2.0F;
    public static final float TrueWindresistMultiplier = 1.0F;
    public static final float BodyMinTemp = 20.0F;
    public static final float BodyMaxTemp = 42.0F;
    private static String cacheTempString = "";
    private static float cacheTemp = -9000.0F;
    private static final Color tempColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Color col_0 = new Color(29, 34, 237);
    private static final Color col_25 = new Color(0, 255, 234);
    private static final Color col_50 = new Color(84, 255, 55);
    private static final Color col_75 = new Color(255, 246, 0);
    private static final Color col_100 = new Color(255, 0, 0);

    public static String getCelsiusPostfix() {
        return "\u00b0C";
    }

    public static String getFahrenheitPostfix() {
        return "\u00b0F";
    }

    public static String getTemperaturePostfix() {
        return Core.getInstance().getOptionTemperatureDisplayCelsius() ? "\u00b0C" : "\u00b0F";
    }

    public static String getTemperatureString(float celsius) {
        float v = Core.getInstance().getOptionTemperatureDisplayCelsius() ? celsius : CelsiusToFahrenheit(celsius);
        v = Math.round(v * 10.0F) / 10.0F;
        if (cacheTemp != v) {
            cacheTemp = v;
            cacheTempString = v + " " + getTemperaturePostfix();
        }

        return cacheTempString;
    }

    public static int getRoundedDisplayTemperature(float celsius) {
        return Core.getInstance().getOptionTemperatureDisplayCelsius() ? PZMath.roundToInt(celsius) : PZMath.roundToInt(CelsiusToFahrenheit(celsius));
    }

    public static float CelsiusToFahrenheit(float celsius) {
        return celsius * 1.8F + 32.0F;
    }

    public static float FahrenheitToCelsius(float fahrenheit) {
        return (fahrenheit - 32.0F) / 1.8F;
    }

    public static float WindchillCelsiusKph(float t, float v) {
        float w = 13.12F + 0.6215F * t - 11.37F * (float)Math.pow(v, 0.16F) + 0.3965F * t * (float)Math.pow(v, 0.16F);
        return w < t ? w : t;
    }

    public static float getTrueInsulationValue(float insulation) {
        return insulation * 2.0F + 0.5F * insulation * insulation * insulation;
    }

    public static float getTrueWindresistanceValue(float windresist) {
        return windresist * 1.0F + 0.5F * windresist * windresist;
    }

    public static void reset() {
    }

    public static float getFractionForRealTimeRatePerMin(float rate) {
        float mod = (float)SandboxOptions.instance.getDayLengthMinutes() / SandboxOptions.instance.getDayLengthMinutesDefault();
        if (mod < 1.0F) {
            mod = 0.5F + 0.5F * mod;
        } else if (mod > 1.0F) {
            mod = 1.0F + mod / 16.0F;
        }

        return rate / (1440.0F / SandboxOptions.instance.getDayLengthMinutes()) * mod;
    }

    public static Color getValueColor(float val) {
        val = ClimateManager.clamp(0.0F, 1.0F, val);
        tempColor.set(0.0F, 0.0F, 0.0F, 1.0F);
        float t = 0.0F;
        if (val < 0.25F) {
            t = val / 0.25F;
            col_0.interp(col_25, t, tempColor);
        } else if (val < 0.5F) {
            t = (val - 0.25F) / 0.25F;
            col_25.interp(col_50, t, tempColor);
        } else if (val < 0.75F) {
            t = (val - 0.5F) / 0.25F;
            col_50.interp(col_75, t, tempColor);
        } else {
            t = (val - 0.75F) / 0.25F;
            col_75.interp(col_100, t, tempColor);
        }

        return tempColor;
    }

    public static float getWindChillAmountForPlayer(IsoPlayer player) {
        if (player.getVehicle() == null && (player.getSquare() == null || !player.getSquare().isInARoom())) {
            ClimateManager clim = ClimateManager.getInstance();
            float airTemperature = clim.getAirTemperatureForCharacter(player, true);
            float windChillAmount = 0.0F;
            if (airTemperature < clim.getTemperature()) {
                windChillAmount = clim.getTemperature() - airTemperature;
            }

            return windChillAmount;
        } else {
            return 0.0F;
        }
    }
}
