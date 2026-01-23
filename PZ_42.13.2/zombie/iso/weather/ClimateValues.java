// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.GregorianCalendar;
import java.util.Random;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class ClimateValues {
    private double simplexOffsetA;
    private double simplexOffsetB;
    private double simplexOffsetC;
    private double simplexOffsetD;
    private final ClimateManager clim;
    private final GameTime gt;
    private float time;
    private float dawn;
    private float dusk;
    private float noon;
    private float dayMeanTemperature;
    private double airMassNoiseFrequencyMod;
    private float noiseAirmass;
    private float airMassTemperature;
    private float baseTemperature;
    private float dayLightLagged;
    private float nightLagged;
    private float temperature;
    private boolean temperatureIsSnow;
    private float humidity;
    private float windIntensity;
    private float windAngleIntensity;
    private float windAngleDegrees;
    private float nightStrength;
    private float dayLightStrength;
    private float ambient;
    private float desaturation;
    private float dayLightStrengthBase;
    private float lerpNight;
    private float cloudyT;
    private float cloudIntensity;
    private float airFrontAirmass;
    private boolean dayDoFog;
    private float dayFogStrength;
    private float dayFogDuration;
    private ClimateManager.DayInfo testCurrentDay;
    private ClimateManager.DayInfo testNextDay;
    private double cacheWorldAgeHours;
    private int cacheYear;
    private int cacheMonth;
    private int cacheDay;
    private final Random seededRandom;

    public ClimateValues(ClimateManager clim) {
        this.simplexOffsetA = clim.getSimplexOffsetA();
        this.simplexOffsetB = clim.getSimplexOffsetB();
        this.simplexOffsetC = clim.getSimplexOffsetC();
        this.simplexOffsetD = clim.getSimplexOffsetD();
        this.clim = clim;
        this.gt = GameTime.getInstance();
        this.seededRandom = new Random(1984L);
    }

    public ClimateValues getCopy() {
        ClimateValues copy = new ClimateValues(this.clim);
        this.CopyValues(copy);
        return copy;
    }

    public void CopyValues(ClimateValues copy) {
        if (copy != this) {
            copy.time = this.time;
            copy.dawn = this.dawn;
            copy.dusk = this.dusk;
            copy.noon = this.noon;
            copy.dayMeanTemperature = this.dayMeanTemperature;
            copy.airMassNoiseFrequencyMod = this.airMassNoiseFrequencyMod;
            copy.noiseAirmass = this.noiseAirmass;
            copy.airMassTemperature = this.airMassTemperature;
            copy.baseTemperature = this.baseTemperature;
            copy.dayLightLagged = this.dayLightLagged;
            copy.nightLagged = this.nightLagged;
            copy.temperature = this.temperature;
            copy.temperatureIsSnow = this.temperatureIsSnow;
            copy.humidity = this.humidity;
            copy.windIntensity = this.windIntensity;
            copy.windAngleIntensity = this.windAngleIntensity;
            copy.windAngleDegrees = this.windAngleDegrees;
            copy.nightStrength = this.nightStrength;
            copy.dayLightStrength = this.dayLightStrength;
            copy.ambient = this.ambient;
            copy.desaturation = this.desaturation;
            copy.dayLightStrengthBase = this.dayLightStrengthBase;
            copy.lerpNight = this.lerpNight;
            copy.cloudyT = this.cloudyT;
            copy.cloudIntensity = this.cloudIntensity;
            copy.airFrontAirmass = this.airFrontAirmass;
            copy.dayDoFog = this.dayDoFog;
            copy.dayFogStrength = this.dayFogStrength;
            copy.dayFogDuration = this.dayFogDuration;
            copy.cacheWorldAgeHours = this.cacheWorldAgeHours;
            copy.cacheYear = this.cacheYear;
            copy.cacheMonth = this.cacheMonth;
            copy.cacheDay = this.cacheDay;
        }
    }

    public void print() {
        DebugLog.log("--------------------------------------------------");
        DebugLog.log("Current time of day = " + this.gt.getTimeOfDay());
        DebugLog.log("Current Worldagehours = " + this.gt.getWorldAgeHours());
        DebugLog.log("--------------------------------------------------");
        if (this.testCurrentDay == null) {
            GregorianCalendar now = new GregorianCalendar(this.cacheYear, this.cacheMonth, this.cacheDay);
            DebugLog.log("Printing climate values for: " + new SimpleDateFormat("yyyy MM dd").format(now.getTime()));
        } else {
            DebugLog.log("Printing climate values for: " + new SimpleDateFormat("yyyy MM dd").format(this.testCurrentDay.calendar.getTime()));
        }

        DebugLog.log("--------------------------------------------------");
        DebugLog.log("Poll Worldagehours = " + this.cacheWorldAgeHours);
        DebugLog.log("Poll time = " + this.time);
        DebugLog.log("dawn = " + this.dawn);
        DebugLog.log("dusk = " + this.dusk);
        DebugLog.log("noon = " + this.noon);
        DebugLog.log("daymeantemperature = " + this.dayMeanTemperature);
        DebugLog.log("airMassNoiseFrequencyMod = " + this.airMassNoiseFrequencyMod);
        DebugLog.log("noiseAirmass = " + this.noiseAirmass);
        DebugLog.log("airMassTemperature = " + this.airMassTemperature);
        DebugLog.log("baseTemperature = " + this.baseTemperature);
        DebugLog.log("dayLightLagged = " + this.dayLightLagged);
        DebugLog.log("nightLagged = " + this.nightLagged);
        DebugLog.log("temperature = " + this.temperature);
        DebugLog.log("temperatureIsSnow = " + this.temperatureIsSnow);
        DebugLog.log("humidity = " + this.humidity);
        DebugLog.log("windIntensity = " + this.windIntensity);
        DebugLog.log("windAngleIntensity = " + this.windAngleIntensity);
        DebugLog.log("windAngleDegrees = " + this.windAngleDegrees);
        DebugLog.log("nightStrength = " + this.nightStrength);
        DebugLog.log("dayLightStrength = " + this.dayLightStrength);
        DebugLog.log("ambient = " + this.ambient);
        DebugLog.log("desaturation = " + this.desaturation);
        DebugLog.log("dayLightStrengthBase = " + this.dayLightStrengthBase);
        DebugLog.log("lerpNight = " + this.lerpNight);
        DebugLog.log("cloudyT = " + this.cloudyT);
        DebugLog.log("cloudIntensity = " + this.cloudIntensity);
        DebugLog.log("airFrontAirmass = " + this.airFrontAirmass);
    }

    public void pollDate(int year, int month, int dayOfMonth) {
        this.pollDate(year, month, dayOfMonth, 0, 0);
    }

    public void pollDate(int year, int month, int dayOfMonth, int hourOfDay) {
        this.pollDate(year, month, dayOfMonth, hourOfDay, 0);
    }

    public void pollDate(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this.pollDate(new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute));
    }

    public void pollDate(GregorianCalendar calendar) {
        if (this.testCurrentDay == null) {
            this.testCurrentDay = new ClimateManager.DayInfo();
        }

        if (this.testNextDay == null) {
            this.testNextDay = new ClimateManager.DayInfo();
        }

        double currentWorldAgeHours = this.gt.getWorldAgeHours();
        this.clim.setDayInfo(this.testCurrentDay, calendar.get(5), calendar.get(2), calendar.get(1), 0);
        this.clim.setDayInfo(this.testNextDay, calendar.get(5), calendar.get(2), calendar.get(1), 1);
        GregorianCalendar now = new GregorianCalendar(this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne(), this.gt.getHour(), this.gt.getMinutes());
        double difference = ChronoUnit.MINUTES.between(now.toInstant(), calendar.toInstant());
        difference /= 60.0;
        double pollWorldAgeHours = currentWorldAgeHours + difference;
        float pollTime = calendar.get(11) + calendar.get(12) / 60.0F;
        this.updateValues(pollWorldAgeHours, pollTime, this.testCurrentDay, this.testNextDay);
    }

    protected void updateValues(double worldAgeHours, float time, ClimateManager.DayInfo currentDay, ClimateManager.DayInfo nextDay) {
        if (currentDay.year != this.cacheYear || currentDay.month != this.cacheMonth || currentDay.day != this.cacheDay) {
            int baseSeedA = (int)this.clim.getSimplexOffsetC();
            int baseSeedB = (int)this.clim.getSimplexOffsetD();
            long seed = (currentDay.year - 1990) * 100000;
            seed += currentDay.month * currentDay.day * 1234;
            seed += (currentDay.year - 1990) * currentDay.month * 10000;
            seed += (baseSeedB - baseSeedA) * currentDay.day;
            this.seededRandom.setSeed(seed);
            this.dayFogStrength = 0.0F;
            this.dayDoFog = false;
            this.dayFogDuration = 0.0F;
            float r = this.seededRandom.nextInt(1000);
            this.dayDoFog = r < 200.0F;
            if (this.dayDoFog) {
                this.dayFogDuration = 4.0F;
                if (r < 25.0F) {
                    this.dayFogStrength = 1.0F;
                    this.dayFogDuration += 2.0F;
                } else {
                    this.dayFogStrength = this.seededRandom.nextFloat();
                }

                float meanTemp = currentDay.season.getDayMeanTemperature();
                float airmassSample = (float)SimplexNoise.noise(
                    this.simplexOffsetA, (worldAgeHours + 12.0 - 48.0) / this.clim.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier())
                );
                meanTemp += airmassSample * 8.0F;
                float randy = this.seededRandom.nextFloat();
                if (meanTemp < 0.0F) {
                    this.dayFogDuration = this.dayFogDuration + 5.0F * this.dayFogStrength;
                    this.dayFogDuration += 8.0F * randy;
                } else if (meanTemp < 10.0F) {
                    this.dayFogDuration = this.dayFogDuration + 2.5F * this.dayFogStrength;
                    this.dayFogDuration += 5.0F * randy;
                } else if (meanTemp < 20.0F) {
                    this.dayFogDuration = this.dayFogDuration + 1.5F * this.dayFogStrength;
                    this.dayFogDuration += 2.5F * randy;
                } else {
                    this.dayFogDuration = this.dayFogDuration + 1.0F * this.dayFogStrength;
                    this.dayFogDuration += 1.0F * randy;
                }

                if (this.dayFogDuration > 24.0F - currentDay.season.getDawn()) {
                    this.dayFogDuration = 24.0F - currentDay.season.getDawn() - 1.0F;
                }
            }
        }

        this.cacheWorldAgeHours = worldAgeHours;
        this.cacheYear = currentDay.year;
        this.cacheMonth = currentDay.month;
        this.cacheDay = currentDay.day;
        this.time = time;
        this.dawn = currentDay.season.getDawn();
        this.dusk = currentDay.season.getDusk();
        this.noon = currentDay.season.getDayHighNoon();
        this.dayMeanTemperature = currentDay.season.getDayMeanTemperature();
        float dayLerpVal = time / 24.0F;
        float dayPercTimeNow = ClimateManager.lerp(dayLerpVal, currentDay.season.getCurDayPercent(), nextDay.season.getCurDayPercent());
        this.airMassNoiseFrequencyMod = this.clim.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier());
        this.noiseAirmass = (float)SimplexNoise.noise(this.simplexOffsetA, worldAgeHours / this.airMassNoiseFrequencyMod);
        float noiseHumidity = (float)SimplexNoise.noise(this.simplexOffsetC, worldAgeHours / this.airMassNoiseFrequencyMod);
        this.airMassTemperature = (float)SimplexNoise.noise(this.simplexOffsetA, (worldAgeHours - 48.0) / this.airMassNoiseFrequencyMod);
        double daymidpoint = Math.floor(worldAgeHours) + 12.0;
        this.airFrontAirmass = (float)SimplexNoise.noise(this.simplexOffsetA, daymidpoint / this.airMassNoiseFrequencyMod);
        float curTemperature = ClimateManager.clerp(dayLerpVal, currentDay.season.getDayTemperature(), nextDay.season.getDayTemperature());
        float curMeanTemperature = ClimateManager.clerp(dayLerpVal, currentDay.season.getDayMeanTemperature(), nextDay.season.getDayMeanTemperature());
        boolean belowMean = curTemperature < curMeanTemperature;
        this.baseTemperature = curMeanTemperature + this.airMassTemperature * 8.0F;
        float lag = 4.0F;
        float ddusk = this.dusk + 4.0F;
        if (ddusk >= 24.0F) {
            ddusk -= 24.0F;
        }

        this.dayLightLagged = this.clim.getTimeLerpHours(time, this.dawn + 4.0F, ddusk, true);
        float tempMod = 5.0F * (1.0F - this.dayLightLagged);
        this.nightLagged = this.clim.getTimeLerpHours(time, ddusk, this.dawn + 4.0F, true);
        tempMod += 5.0F * this.nightLagged;
        this.temperature = this.baseTemperature + 1.0F - tempMod;
        if (!(this.temperature < 0.0F) && !ClimateManager.winterIsComing) {
            this.temperatureIsSnow = false;
        } else {
            this.temperatureIsSnow = true;
        }

        float curTempOffset = this.temperature;
        curTempOffset = (45.0F - curTempOffset) / 90.0F;
        curTempOffset = ClimateManager.clamp01(1.0F - curTempOffset);
        float noiseHum = (1.0F + noiseHumidity) * 0.5F;
        this.humidity = noiseHum * curTempOffset;
        float windMod = 1.0F - (this.airMassTemperature + 1.0F) * 0.5F;
        float windMod2 = 1.0F - dayPercTimeNow * 0.4F;
        float noiseWindBase = (float)SimplexNoise.noise(worldAgeHours / 40.0, this.simplexOffsetA);
        float windBase = (noiseWindBase + 1.0F) * 0.5F;
        windBase *= windMod * windMod2;
        windBase *= 0.65F;
        this.windIntensity = windBase;
        float noiseAngleDegrees = (float)SimplexNoise.noise(worldAgeHours / 80.0, this.simplexOffsetB);
        this.windAngleIntensity = noiseAngleDegrees;
        float noiseWindAngleBase = (float)SimplexNoise.noise(worldAgeHours / 40.0, this.simplexOffsetD);
        noiseWindAngleBase = (noiseWindAngleBase + 1.0F) * 0.5F;
        this.windAngleDegrees = 360.0F * noiseWindAngleBase;
        if (this.gt.isEndlessNight()) {
            this.dayLightStrength = 0.0F;
            this.nightStrength = 1.0F;
        } else {
            this.lerpNight = this.clim.getTimeLerpHours(time, this.dusk, this.dawn, true);
            this.lerpNight = ClimateManager.clamp(0.0F, 1.0F, this.lerpNight * 2.0F);
            this.nightStrength = this.lerpNight;
        }

        if (this.gt.isEndlessDay()) {
            this.dayLightStrength = 1.0F - 0.15F * dayPercTimeNow - 0.2F * this.windIntensity;
            this.nightStrength = 0.0F;
        } else {
            this.dayLightStrengthBase = 1.0F - this.nightStrength;
            float modd = 1.0F - 0.15F * dayPercTimeNow - 0.2F * this.windIntensity;
            this.dayLightStrengthBase *= modd;
            this.dayLightStrength = this.dayLightStrengthBase;
        }

        this.ambient = this.dayLightStrength;
        float desatCurrDay = (1.0F - currentDay.season.getCurDayPercent()) * 0.4F;
        float desatNextDay = (1.0F - nextDay.season.getCurDayPercent()) * 0.4F;
        this.desaturation = ClimateManager.lerp(dayLerpVal, desatCurrDay, desatNextDay);
        this.cloudyT = 1.0F - ClimateManager.clamp01((this.airMassTemperature + 0.8F) * 0.625F);
        this.cloudyT *= 0.8F;
        this.cloudyT = ClimateManager.clamp01(this.cloudyT + this.windIntensity);
        this.cloudIntensity = ClimateManager.clamp01(this.windIntensity * 2.0F);
        this.cloudIntensity = this.cloudIntensity - this.cloudIntensity * 0.5F * this.nightStrength;
    }

    public float getTime() {
        return this.time;
    }

    public float getDawn() {
        return this.dawn;
    }

    public float getDusk() {
        return this.dusk;
    }

    public float getNoon() {
        return this.noon;
    }

    public double getAirMassNoiseFrequencyMod() {
        return this.airMassNoiseFrequencyMod;
    }

    public float getNoiseAirmass() {
        return this.noiseAirmass;
    }

    public float getAirMassTemperature() {
        return this.airMassTemperature;
    }

    public float getBaseTemperature() {
        return this.baseTemperature;
    }

    public float getDayLightLagged() {
        return this.dayLightLagged;
    }

    public float getNightLagged() {
        return this.nightLagged;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public boolean isTemperatureIsSnow() {
        return this.temperatureIsSnow;
    }

    public float getHumidity() {
        return this.humidity;
    }

    public float getWindIntensity() {
        return this.windIntensity;
    }

    public float getWindAngleIntensity() {
        return this.windAngleIntensity;
    }

    public float getWindAngleDegrees() {
        return this.windAngleDegrees;
    }

    public float getNightStrength() {
        return this.nightStrength;
    }

    public float getDayLightStrength() {
        return this.dayLightStrength;
    }

    public float getAmbient() {
        return this.ambient;
    }

    public float getDesaturation() {
        return this.desaturation;
    }

    public float getDayLightStrengthBase() {
        return this.dayLightStrengthBase;
    }

    public float getLerpNight() {
        return this.lerpNight;
    }

    public float getCloudyT() {
        return this.cloudyT;
    }

    public float getCloudIntensity() {
        return this.cloudIntensity;
    }

    public float getAirFrontAirmass() {
        return this.airFrontAirmass;
    }

    public double getCacheWorldAgeHours() {
        return this.cacheWorldAgeHours;
    }

    public int getCacheYear() {
        return this.cacheYear;
    }

    public int getCacheMonth() {
        return this.cacheMonth;
    }

    public int getCacheDay() {
        return this.cacheDay;
    }

    public float getDayMeanTemperature() {
        return this.dayMeanTemperature;
    }

    public boolean isDayDoFog() {
        return this.dayDoFog;
    }

    public float getDayFogStrength() {
        return this.dayFogStrength;
    }

    public float getDayFogDuration() {
        return this.dayFogDuration;
    }
}
