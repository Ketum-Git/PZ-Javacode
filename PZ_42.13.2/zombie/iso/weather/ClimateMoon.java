// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import zombie.UsedFromLua;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class ClimateMoon {
    private static final int[] day_year = new int[]{-1, -1, 30, 58, 89, 119, 150, 180, 211, 241, 272, 303, 333};
    private static final String[] moon_phase_name = new String[]{
        "New", "Waxing crescent", "First quarter", "Waxing gibbous", "Full", "Waning gibbous", "Third quarter", "Waning crescent"
    };
    private static final float[] units = new float[]{0.0F, 0.25F, 0.5F, 0.75F, 1.0F, 0.75F, 0.5F, 0.25F};
    private int lastYear;
    private int lastMonth;
    private int lastDay;
    private int currentPhase;
    private float currentFloat;
    private static final ClimateMoon instance = new ClimateMoon();

    public static ClimateMoon getInstance() {
        return instance;
    }

    public void updatePhase(int year, int month, int day) {
        if (year != this.lastYear || month != this.lastMonth || day != this.lastDay) {
            this.lastYear = year;
            this.lastMonth = month;
            this.lastDay = day;
            this.currentPhase = this.getMoonPhase(year, month, day);
            if (this.currentPhase > 7) {
                this.currentPhase = 7;
            }

            if (this.currentPhase < 0) {
                this.currentPhase = 0;
            }

            this.currentFloat = units[this.currentPhase];
        }
    }

    public String getPhaseName() {
        return moon_phase_name[this.currentPhase];
    }

    public float getMoonFloat() {
        return this.currentFloat;
    }

    public int getCurrentMoonPhase() {
        return this.currentPhase;
    }

    private int getMoonPhase(int year, int month, int day) {
        if (month < 0 || month > 12) {
            month = 0;
        }

        int diy = day + day_year[month];
        if (month > 2 && this.isLeapYearP(year)) {
            diy++;
        }

        int cent = year / 100 + 1;
        int golden = year % 19 + 1;
        int epact = (11 * golden + 20 + (8 * cent + 5) / 25 - 5 - (3 * cent / 4 - 12)) % 30;
        if (epact <= 0) {
            epact += 30;
        }

        if (epact == 25 && golden > 11 || epact == 24) {
            epact++;
        }

        return ((diy + epact) * 6 + 11) % 177 / 22 & 7;
    }

    private int daysInMonth(int month, int year) {
        int result = 31;
        switch (month) {
            case 2:
                result = this.isLeapYearP(year) ? 29 : 28;
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            default:
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                result = 30;
        }

        return result;
    }

    private boolean isLeapYearP(int year) {
        return year % 4 == 0 && (year % 400 == 0 || year % 100 != 0);
    }

    public void Reset() {
        this.currentFloat = 0.0F;
        this.currentPhase = 0;
        this.lastYear = this.lastMonth = this.lastDay = 0;
    }
}
