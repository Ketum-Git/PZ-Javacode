// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.season;

import java.util.GregorianCalendar;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.erosion.utils.Noise2D;

@UsedFromLua
public final class ErosionSeason {
    public static final int SEASON_DEFAULT = 0;
    public static final int SEASON_SPRING = 1;
    public static final int SEASON_SUMMER = 2;
    public static final int SEASON_SUMMER2 = 3;
    public static final int SEASON_AUTUMN = 4;
    public static final int SEASON_WINTER = 5;
    public static final int NUM_SEASONS = 6;
    private int lat = 38;
    private int tempMax = 25;
    private int tempMin;
    private int tempDiff = 7;
    private float highNoon = 12.5F;
    private float highNoonCurrent = 12.5F;
    private int seasonLag = 31;
    private final float[] rain = new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};
    private double suSol;
    private double wiSol;
    private final GregorianCalendar zeroDay = new GregorianCalendar(1970, 0, 1, 0, 0);
    private int day;
    private int month;
    private int year;
    private boolean isH1;
    private final ErosionSeason.YearData[] yearData = new ErosionSeason.YearData[3];
    private int curSeason;
    private float curSeasonDay;
    private float curSeasonDays;
    private float curSeasonStrength;
    private float curSeasonProgression;
    private float dayMeanTemperature;
    private float dayTemperature;
    private float dayNoiseVal;
    private boolean isRainDay;
    private float rainYearAverage;
    private float rainDayStrength;
    private boolean isThunderDay;
    private boolean isSunnyDay;
    private float dayDusk;
    private float dayDawn;
    private float dayDaylight;
    private float winterMod;
    private float summerMod;
    private float summerTilt;
    private float curDayPercent;
    private final Noise2D per = new Noise2D();
    private int seedA = 64;
    private int seedB = 128;
    private int seedC = 255;
    String[] names = new String[]{"Default", "Spring", "Early Summer", "Late Summer", "Autumn", "Winter"};
    String[] namesTranslated = new String[]{
        Translator.getText("IGUI_Season_Default"),
        Translator.getText("IGUI_Season_Spring"),
        Translator.getText("IGUI_Season_SummerE"),
        Translator.getText("IGUI_Season_SummerL"),
        Translator.getText("IGUI_Season_Autumn"),
        Translator.getText("IGUI_Season_Winter")
    };

    public void init(int _lat, int _tempMax, int _tempMin, int _tempDiff, int _seasonLag, float _noon, int _seedA, int _seedB, int _seedC) {
        this.lat = _lat;
        this.tempMax = _tempMax;
        this.tempMin = _tempMin;
        this.tempDiff = _tempDiff;
        this.seasonLag = _seasonLag;
        this.highNoon = _noon;
        this.highNoonCurrent = _noon;
        this.seedA = _seedA;
        this.seedB = _seedB;
        this.seedC = _seedC;
        this.summerTilt = 2.0F;
        this.winterMod = this.tempMin < 0 ? 0.05F * -this.tempMin : 0.02F * -this.tempMin;
        this.summerMod = this.tempMax < 0 ? 0.05F * this.tempMax : 0.02F * this.tempMax;
        this.suSol = 2.0 * this.degree(Math.acos(-Math.tan(this.radian(this.lat)) * Math.tan(this.radian(23.44)))) / 15.0;
        this.wiSol = 2.0 * this.degree(Math.acos(Math.tan(this.radian(this.lat)) * Math.tan(this.radian(23.44)))) / 15.0;
        this.per.reset();
        this.per.addLayer(_seedA, 8.0F, 2.0F);
        this.per.addLayer(_seedB, 6.0F, 4.0F);
        this.per.addLayer(_seedC, 4.0F, 6.0F);
        this.yearData[0] = new ErosionSeason.YearData();
        this.yearData[1] = new ErosionSeason.YearData();
        this.yearData[2] = new ErosionSeason.YearData();
    }

    public int getLat() {
        return this.lat;
    }

    public int getTempMax() {
        return this.tempMax;
    }

    public int getTempMin() {
        return this.tempMin;
    }

    public int getTempDiff() {
        return this.tempDiff;
    }

    public int getSeasonLag() {
        return this.seasonLag;
    }

    public float getHighNoon() {
        return this.highNoon;
    }

    public int getSeedA() {
        return this.seedA;
    }

    public int getSeedB() {
        return this.seedB;
    }

    public int getSeedC() {
        return this.seedC;
    }

    public void setRain(
        float _jan, float _feb, float _mar, float _apr, float _may, float _jun, float _jul, float _aug, float _sep, float _oct, float _nov, float _dec
    ) {
        this.rain[0] = _jan;
        this.rain[1] = _feb;
        this.rain[2] = _mar;
        this.rain[3] = _apr;
        this.rain[4] = _may;
        this.rain[5] = _jun;
        this.rain[6] = _jul;
        this.rain[7] = _aug;
        this.rain[8] = _sep;
        this.rain[9] = _oct;
        this.rain[10] = _nov;
        this.rain[11] = _dec;
        float total = 0.0F;

        for (float v : this.rain) {
            total += v;
        }

        this.rainYearAverage = (int)Math.floor(365.0F * (total / this.rain.length));
    }

    public ErosionSeason clone() {
        ErosionSeason clone = new ErosionSeason();
        clone.init(this.lat, this.tempMax, this.tempMin, this.tempDiff, this.seasonLag, this.highNoon, this.seedA, this.seedB, this.seedC);
        clone.setRain(
            this.rain[0],
            this.rain[1],
            this.rain[2],
            this.rain[3],
            this.rain[4],
            this.rain[5],
            this.rain[6],
            this.rain[7],
            this.rain[8],
            this.rain[9],
            this.rain[10],
            this.rain[11]
        );
        return clone;
    }

    public float getCurDayPercent() {
        return this.curDayPercent;
    }

    public double getMaxDaylightWinter() {
        return this.wiSol;
    }

    public double getMaxDaylightSummer() {
        return this.suSol;
    }

    public float getDusk() {
        return this.dayDusk;
    }

    public float getDawn() {
        return this.dayDawn;
    }

    public float getDaylight() {
        return this.dayDaylight;
    }

    public float getDayTemperature() {
        return this.dayTemperature;
    }

    public float getDayMeanTemperature() {
        return this.dayMeanTemperature;
    }

    public int getSeason() {
        return this.curSeason;
    }

    public float getDayHighNoon() {
        return this.highNoonCurrent;
    }

    public String getSeasonName() {
        return this.names[this.curSeason];
    }

    public String getSeasonNameTranslated() {
        return this.namesTranslated[this.curSeason];
    }

    public boolean isSeason(int _season) {
        return _season == this.curSeason;
    }

    public GregorianCalendar getWinterStartDay(int day, int month, int year) {
        GregorianCalendar dayDate = new GregorianCalendar(year, month, day);
        long dayValue = dayDate.getTime().getTime();
        return dayValue < this.yearData[0].winterEndDayUnx ? this.yearData[0].winterStartDay : this.yearData[1].winterStartDay;
    }

    public float getSeasonDay() {
        return this.curSeasonDay;
    }

    public float getSeasonDays() {
        return this.curSeasonDays;
    }

    public float getSeasonStrength() {
        return this.curSeasonStrength;
    }

    public float getSeasonProgression() {
        return this.curSeasonProgression;
    }

    public float getDayNoiseVal() {
        return this.dayNoiseVal;
    }

    public boolean isRainDay() {
        return this.isRainDay;
    }

    public float getRainDayStrength() {
        return this.rainDayStrength;
    }

    public float getRainYearAverage() {
        return this.rainYearAverage;
    }

    public boolean isThunderDay() {
        return this.isThunderDay;
    }

    public boolean isSunnyDay() {
        return this.isSunnyDay;
    }

    public void setDay(int _day, int _month, int _year) {
        if (_year == 0) {
            DebugLog.log("NOTICE: year value is 0?");
        }

        GregorianCalendar dayDate = new GregorianCalendar(_year, _month, _day, 0, 0);
        long dayValue = dayDate.getTime().getTime();
        this.setYearData(_year);
        this.setSeasonData((float)dayValue, dayDate, _year, _month);
        this.setDaylightData(dayValue, dayDate);
    }

    private void setYearData(int _year) {
        if (this.yearData[1].year != _year) {
            for (int i = 0; i < 3; i++) {
                int ii = i - 1;
                int curYear = _year + ii;
                this.yearData[i].year = curYear;
                this.yearData[i].winSols = new GregorianCalendar(curYear, 11, 22);
                this.yearData[i].sumSols = new GregorianCalendar(curYear, 5, 22);
                this.yearData[i].winSolsUnx = this.yearData[i].winSols.getTime().getTime();
                this.yearData[i].sumSolsUnx = this.yearData[i].sumSols.getTime().getTime();
                this.yearData[i].hottestDay = new GregorianCalendar(curYear, 5, 22);
                this.yearData[i].coldestDay = new GregorianCalendar(curYear, 11, 22);
                this.yearData[i].hottestDay.add(5, this.seasonLag);
                this.yearData[i].coldestDay.add(5, this.seasonLag);
                this.yearData[i].hottestDayUnx = this.yearData[i].hottestDay.getTime().getTime();
                this.yearData[i].coldestDayUnx = this.yearData[i].coldestDay.getTime().getTime();
                this.yearData[i].winterS = this.per.layeredNoise(64 + curYear, 64.0F);
                this.yearData[i].winterE = this.per.layeredNoise(64.0F, 64 + curYear);
                this.yearData[i].winterStartDay = new GregorianCalendar(curYear, 11, 22);
                this.yearData[i].winterEndDay = new GregorianCalendar(curYear, 11, 22);
                this.yearData[i].winterStartDay.add(5, (int)(-Math.floor(40.0F + 40.0F * this.winterMod + 20.0F * this.yearData[i].winterS)));
                this.yearData[i].winterEndDay.add(5, (int)Math.floor(40.0F + 40.0F * this.winterMod + 20.0F * this.yearData[i].winterE));
                this.yearData[i].winterStartDayUnx = this.yearData[i].winterStartDay.getTime().getTime();
                this.yearData[i].winterEndDayUnx = this.yearData[i].winterEndDay.getTime().getTime();
                this.yearData[i].summerS = this.per.layeredNoise(128 + curYear, 128.0F);
                this.yearData[i].summerE = this.per.layeredNoise(128.0F, 128 + curYear);
                this.yearData[i].summerStartDay = new GregorianCalendar(curYear, 5, 22);
                this.yearData[i].summerEndDay = new GregorianCalendar(curYear, 5, 22);
                this.yearData[i].summerStartDay.add(5, (int)(-Math.floor(40.0F + 40.0F * this.summerMod + 20.0F * this.yearData[i].summerS)));
                this.yearData[i].summerEndDay.add(5, (int)Math.floor(40.0F + 40.0F * this.summerMod + 20.0F * this.yearData[i].summerE));
                this.yearData[i].summerStartDayUnx = this.yearData[i].summerStartDay.getTime().getTime();
                this.yearData[i].summerEndDayUnx = this.yearData[i].summerEndDay.getTime().getTime();
            }

            this.yearData[1].lastSummerStr = this.yearData[0].summerS + this.yearData[0].summerE - 1.0F;
            this.yearData[1].lastWinterStr = this.yearData[0].winterS + this.yearData[0].winterE - 1.0F;
            this.yearData[1].summerStr = this.yearData[1].summerS + this.yearData[1].summerE - 1.0F;
            this.yearData[1].winterStr = this.yearData[1].winterS + this.yearData[1].winterE - 1.0F;
            this.yearData[1].nextSummerStr = this.yearData[2].summerS + this.yearData[2].summerE - 1.0F;
            this.yearData[1].nextWinterStr = this.yearData[2].winterS + this.yearData[2].winterE - 1.0F;
        }
    }

    private void setSeasonData(float _dayValue, GregorianCalendar _dayDate, int _year, int _month) {
        GregorianCalendar sd;
        GregorianCalendar ed;
        if (_dayValue < (float)this.yearData[0].winterEndDayUnx) {
            this.curSeason = 5;
            sd = this.yearData[0].winterStartDay;
            ed = this.yearData[0].winterEndDay;
        } else if (_dayValue < (float)this.yearData[1].summerStartDayUnx) {
            this.curSeason = 1;
            sd = this.yearData[0].winterEndDay;
            ed = this.yearData[1].summerStartDay;
        } else if (_dayValue < (float)this.yearData[1].summerEndDayUnx) {
            this.curSeason = 2;
            sd = this.yearData[1].summerStartDay;
            ed = this.yearData[1].summerEndDay;
        } else if (_dayValue < (float)this.yearData[1].winterStartDayUnx) {
            this.curSeason = 4;
            sd = this.yearData[1].summerEndDay;
            ed = this.yearData[1].winterStartDay;
        } else {
            this.curSeason = 5;
            sd = this.yearData[1].winterStartDay;
            ed = this.yearData[1].winterEndDay;
        }

        this.curSeasonDay = this.dayDiff(_dayDate, sd);
        this.curSeasonDays = this.dayDiff(sd, ed);
        this.curSeasonStrength = this.curSeasonDays / 90.0F - 1.0F;
        this.curSeasonProgression = this.curSeasonDay / this.curSeasonDays;
        float tempA;
        float tempB;
        float dayPerc;
        if (_dayValue < (float)this.yearData[0].coldestDayUnx && _dayValue >= (float)this.yearData[0].hottestDayUnx) {
            tempA = this.tempMax + this.tempDiff / 2 * this.yearData[1].lastSummerStr;
            tempB = this.tempMin + this.tempDiff / 2 * this.yearData[1].lastWinterStr;
            dayPerc = this.dayDiff(_dayDate, this.yearData[0].hottestDay) / this.dayDiff(this.yearData[0].hottestDay, this.yearData[0].coldestDay);
        } else if (_dayValue < (float)this.yearData[1].hottestDayUnx && _dayValue >= (float)this.yearData[0].coldestDayUnx) {
            tempA = this.tempMin + this.tempDiff / 2 * this.yearData[1].lastWinterStr;
            tempB = this.tempMax + this.tempDiff / 2 * this.yearData[1].summerStr;
            dayPerc = this.dayDiff(_dayDate, this.yearData[0].coldestDay) / this.dayDiff(this.yearData[1].hottestDay, this.yearData[0].coldestDay);
        } else if (_dayValue < (float)this.yearData[1].coldestDayUnx && _dayValue >= (float)this.yearData[1].hottestDayUnx) {
            tempA = this.tempMax + this.tempDiff / 2 * this.yearData[1].summerStr;
            tempB = this.tempMin + this.tempDiff / 2 * this.yearData[1].winterStr;
            dayPerc = this.dayDiff(_dayDate, this.yearData[1].hottestDay) / this.dayDiff(this.yearData[1].hottestDay, this.yearData[1].coldestDay);
        } else {
            tempA = this.tempMin + this.tempDiff / 2 * this.yearData[1].winterStr;
            tempB = this.tempMax + this.tempDiff / 2 * this.yearData[1].nextSummerStr;
            dayPerc = this.dayDiff(_dayDate, this.yearData[1].coldestDay) / this.dayDiff(this.yearData[1].coldestDay, this.yearData[2].hottestDay);
        }

        float dayAverageTemp = (float)this.clerp(dayPerc, tempA, tempB);
        float superOffset = this.dayDiff(this.zeroDay, _dayDate) / 20.0F;
        this.dayNoiseVal = this.per.layeredNoise(superOffset, 0.0F);
        float modifier = this.dayNoiseVal * 2.0F - 1.0F;
        this.dayTemperature = dayAverageTemp + this.tempDiff * modifier;
        this.dayMeanTemperature = dayAverageTemp;
        this.isThunderDay = false;
        this.isRainDay = false;
        this.isSunnyDay = false;
        float rainMod = 0.1F + this.rain[_month] <= 1.0F ? 0.1F + this.rain[_month] : 1.0F;
        if (rainMod > 0.0F && this.dayNoiseVal < rainMod) {
            this.isRainDay = true;
            this.rainDayStrength = 1.0F - this.dayNoiseVal / rainMod;
            float tmp = this.per.layeredNoise(0.0F, superOffset);
            if (tmp > 0.6) {
                this.isThunderDay = true;
            }
        }

        if (this.dayNoiseVal > 0.6) {
            this.isSunnyDay = true;
        }
    }

    private void setDaylightData(long _dayValue, GregorianCalendar _dayDate) {
        GregorianCalendar cycleStart;
        GregorianCalendar cycleEnd;
        if (_dayValue < this.yearData[1].winSolsUnx && _dayValue >= this.yearData[1].sumSolsUnx) {
            this.isH1 = false;
            cycleStart = this.yearData[1].sumSols;
            cycleEnd = this.yearData[1].winSols;
        } else {
            this.isH1 = true;
            if (_dayValue >= this.yearData[1].winSolsUnx) {
                cycleStart = this.yearData[1].winSols;
                cycleEnd = this.yearData[2].sumSols;
            } else {
                cycleStart = this.yearData[0].winSols;
                cycleEnd = this.yearData[1].sumSols;
            }
        }

        float hCyclePerc = this.dayDiff(_dayDate, cycleStart) / this.dayDiff(cycleStart, cycleEnd);
        float perc2 = hCyclePerc;
        if (this.isH1) {
            this.dayDaylight = (float)this.clerp(hCyclePerc, this.wiSol, this.suSol);
        } else {
            this.dayDaylight = (float)this.clerp(hCyclePerc, this.suSol, this.wiSol);
            perc2 = 1.0F - hCyclePerc;
        }

        this.curDayPercent = perc2;
        if (this.isEndlessNight()) {
            this.highNoonCurrent = 0.0F;
            this.dayDawn = 0.0F;
            this.dayDusk = 0.0F;
            this.dayDaylight = 0.0F;
        } else if (this.isEndlessDay()) {
            this.dayDawn = 0.0F;
            this.dayDusk = 0.0F;
            this.dayDaylight = 24.0F;
        } else {
            this.highNoonCurrent = this.highNoon + this.summerTilt * perc2;
            this.dayDawn = this.highNoonCurrent - this.dayDaylight / 2.0F;
            this.dayDusk = this.highNoonCurrent + this.dayDaylight / 2.0F;
        }
    }

    private float dayDiff(GregorianCalendar _date1, GregorianCalendar _date2) {
        long diff = _date1.getTime().getTime() - _date2.getTime().getTime();
        return (float)Math.abs(diff / 86400000L);
    }

    private double clerp(double _t, double _a, double _b) {
        double t2 = (1.0 - Math.cos(_t * Math.PI)) / 2.0;
        return _a * (1.0 - t2) + _b * t2;
    }

    private double lerp(double _t, double _a, double _b) {
        return _a + _t * (_b - _a);
    }

    private double radian(double _a) {
        return _a * (Math.PI / 180.0);
    }

    private double degree(double _a) {
        return _a * (180.0 / Math.PI);
    }

    public static void Reset() {
    }

    public void setCurSeason(int season) {
        this.curSeason = season;
    }

    public boolean isEndlessDay() {
        return SandboxOptions.getInstance().dayNightCycle.getValue() == 2;
    }

    public boolean isEndlessNight() {
        return SandboxOptions.getInstance().dayNightCycle.getValue() == 3;
    }

    private static class YearData {
        public int year = Integer.MIN_VALUE;
        public GregorianCalendar winSols;
        public GregorianCalendar sumSols;
        public long winSolsUnx;
        public long sumSolsUnx;
        public GregorianCalendar hottestDay;
        public GregorianCalendar coldestDay;
        public long hottestDayUnx;
        public long coldestDayUnx;
        public float winterS;
        public float winterE;
        public GregorianCalendar winterStartDay;
        public GregorianCalendar winterEndDay;
        public long winterStartDayUnx;
        public long winterEndDayUnx;
        public float summerS;
        public float summerE;
        public GregorianCalendar summerStartDay;
        public GregorianCalendar summerEndDay;
        public long summerStartDayUnx;
        public long summerEndDayUnx;
        public float lastSummerStr;
        public float lastWinterStr;
        public float summerStr;
        public float winterStr;
        public float nextSummerStr;
        public float nextWinterStr;
    }
}
