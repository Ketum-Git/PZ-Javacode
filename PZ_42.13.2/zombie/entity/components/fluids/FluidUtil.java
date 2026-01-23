// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public class FluidUtil {
    public static final float UNIT_L = 1.0F;
    public static final float UNIT_dL = 0.1F;
    public static final float UNIT_cL = 0.01F;
    public static final float UNIT_mL = 0.001F;
    public static final float UNIT_dmL = 1.0E-4F;
    public static final float UNIT_cmL = 1.0E-5F;
    public static final float UNIT_uL = 1.0E-6F;
    public static final float MIN_UNIT = 1.0E-4F;
    public static final float MIN_CONTAINER_CAPACITY = 0.05F;
    private static final DecimalFormat df_liter = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final DecimalFormat df_liter10 = new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final DecimalFormat df_liter1000 = new DecimalFormat("#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public static final float TRANSFER_ACTION_TIME_PER_LITER = 40.0F;
    public static final float MIN_TRANSFER_ACTION_TIME = 20.0F;

    public static float getUnitLiter() {
        return 1.0F;
    }

    public static float getUnitDeciLiter() {
        return 0.1F;
    }

    public static float getUnitCentiLiter() {
        return 0.01F;
    }

    public static float getUnitMilliLiter() {
        return 0.001F;
    }

    public static float getUnitDeciMilliLiter() {
        return 1.0E-4F;
    }

    public static float getUnitCentiMilliLiter() {
        return 1.0E-5F;
    }

    public static float getUnitMicroLiter() {
        return 1.0E-6F;
    }

    public static float getMinUnit() {
        return 1.0E-4F;
    }

    public static float getMinContainerCapacity() {
        return 0.05F;
    }

    public static String getAmountFormatted(float amount) {
        if (amount >= 1000.0F) {
            return getAmountLiter1000(amount);
        } else if (amount >= 10.0F) {
            return getAmountLiter10(amount);
        } else {
            return amount >= 1.0F ? getAmountLiter(amount) : getAmountMilli(amount);
        }
    }

    public static String getFractionFormatted(float numerator, float denominator) {
        float amount = PZMath.max(numerator, denominator);
        if (amount >= 1000.0F) {
            return String.format("%s / %s", df_liter1000.format(numerator), df_liter1000.format(denominator) + " L");
        } else if (amount >= 10.0F) {
            return String.format("%s / %s", df_liter10.format(numerator), df_liter10.format(denominator) + " L");
        } else {
            return amount >= 1.0F
                ? String.format("%s / %s", df_liter.format(numerator), df_liter.format(denominator) + " L")
                : String.format("%s / %s", Math.round(numerator * 1000.0F), Math.round(denominator * 1000.0F) + " mL");
        }
    }

    public static String getAmountLiter1000(float amount) {
        return df_liter1000.format(amount) + " L";
    }

    public static String getAmountLiter10(float amount) {
        return df_liter10.format(amount) + " L";
    }

    public static String getAmountLiter(float amount) {
        return df_liter.format(amount) + " L";
    }

    public static String getAmountMilli(float amount) {
        int ml = Math.round(amount * 1000.0F);
        return ml + " mL";
    }

    public static float roundTransfer(float amount) {
        return Math.round(amount * 100.0F) / 100.0F;
    }

    public static float getTransferActionTimePerLiter() {
        return 40.0F;
    }

    public static float getMinTransferActionTime() {
        return 20.0F;
    }
}
