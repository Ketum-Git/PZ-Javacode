// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Translator;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class ClimateForecaster {
    private static final int OffsetToday = 10;
    private ClimateValues climateValues;
    private final ClimateForecaster.DayForecast[] forecasts = new ClimateForecaster.DayForecast[40];
    private final ArrayList<ClimateForecaster.DayForecast> forecastList = new ArrayList<>(40);

    public ArrayList<ClimateForecaster.DayForecast> getForecasts() {
        return this.forecastList;
    }

    public ClimateForecaster.DayForecast getForecast() {
        return this.getForecast(0);
    }

    public ClimateForecaster.DayForecast getForecast(int offset) {
        int target = 10 + offset;
        return target >= 0 && target < this.forecasts.length ? this.forecasts[target] : null;
    }

    private void populateForecastList() {
        this.forecastList.clear();

        for (int i = 0; i < this.forecasts.length; i++) {
            this.forecastList.add(this.forecasts[i]);
        }
    }

    protected void init(ClimateManager climateManager) {
        this.climateValues = climateManager.getClimateValuesCopy();

        for (int i = 0; i < this.forecasts.length; i++) {
            int offset = i - 10;
            ClimateForecaster.DayForecast dayForecast = new ClimateForecaster.DayForecast();
            dayForecast.weatherPeriod = new WeatherPeriod(climateManager, climateManager.getThunderStorm());
            dayForecast.weatherPeriod.setDummy(true);
            dayForecast.indexOffset = offset;
            dayForecast.airFront = new ClimateManager.AirFront();
            this.sampleDay(climateManager, dayForecast, offset);
            this.forecasts[i] = dayForecast;
        }

        this.populateForecastList();
    }

    protected void updateDayChange(ClimateManager climateManager) {
        ClimateForecaster.DayForecast first = this.forecasts[0];

        for (int i = 0; i < this.forecasts.length; i++) {
            if (i > 0 && i < this.forecasts.length) {
                this.forecasts[i].indexOffset = i - 1 - 10;
                this.forecasts[i - 1] = this.forecasts[i];
            }
        }

        first.reset();
        this.sampleDay(climateManager, first, this.forecasts.length - 1 - 10);
        first.indexOffset = this.forecasts.length - 1 - 10;
        this.forecasts[this.forecasts.length - 1] = first;
        this.populateForecastList();
    }

    protected void sampleDay(ClimateManager climateManager, ClimateForecaster.DayForecast dayForecast, int dayOffset) {
        GameTime gt = GameTime.getInstance();
        int year = gt.getYear();
        int month = gt.getMonth();
        int day = gt.getDayPlusOne();
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 0, 0);
        calendar.add(5, dayOffset);
        boolean lastFrontWarm = true;
        ClimateForecaster.DayForecast overlapDay = this.getWeatherOverlap(dayOffset + 10, 0.0F);
        dayForecast.weatherOverlap = overlapDay;
        dayForecast.weatherPeriod.stopWeatherPeriod();
        dayForecast.name = Translator.getText("IGUI_Forecaster_Day") + ": " + calendar.get(1) + " - " + (calendar.get(2) + 1) + " - " + calendar.get(5);

        for (int i = 0; i < 24; i++) {
            if (i != 0) {
                calendar.add(11, 1);
            }

            this.climateValues.pollDate(calendar);
            if (i == 0) {
                lastFrontWarm = this.climateValues.getNoiseAirmass() >= 0.0F;
                dayForecast.airFrontString = lastFrontWarm ? "WARM" : "COLD";
                dayForecast.dawn = this.climateValues.getDawn();
                dayForecast.dusk = this.climateValues.getDusk();
                dayForecast.dayLightHours = dayForecast.dusk - dayForecast.dawn;
            }

            if (!dayForecast.weatherStarts
                && (lastFrontWarm && this.climateValues.getNoiseAirmass() < 0.0F || !lastFrontWarm && this.climateValues.getNoiseAirmass() >= 0.0F)) {
                int LAST_TYPE = this.climateValues.getNoiseAirmass() >= 0.0F ? -1 : 1;
                dayForecast.airFront.setFrontType(LAST_TYPE);
                climateManager.CalculateWeatherFrontStrength(calendar.get(1), calendar.get(2), calendar.get(5), dayForecast.airFront);
                dayForecast.airFront.setFrontWind(this.climateValues.getWindAngleDegrees());
                if (dayForecast.airFront.getStrength() >= 0.1F) {
                    ClimateForecaster.DayForecast overlap = this.getWeatherOverlap(dayOffset + 10, i);
                    float overlapStrength = overlap != null ? overlap.weatherPeriod.getTotalStrength() : -1.0F;
                    if (overlapStrength < 0.1F) {
                        dayForecast.weatherStarts = true;
                        dayForecast.weatherStartTime = i;
                        dayForecast.weatherPeriod
                            .init(dayForecast.airFront, this.climateValues.getCacheWorldAgeHours(), calendar.get(1), calendar.get(2), calendar.get(5));
                    }
                }

                if (!dayForecast.weatherStarts) {
                    lastFrontWarm = !lastFrontWarm;
                }
            }

            boolean isDayTime = i > this.climateValues.getDawn() && i <= this.climateValues.getDusk();
            float temperature = this.climateValues.getTemperature();
            float humidity = this.climateValues.getHumidity();
            float windDirection = this.climateValues.getWindAngleDegrees();
            float windPower = this.climateValues.getWindIntensity();
            float cloudiness = this.climateValues.getCloudIntensity();
            if (dayForecast.weatherStarts || dayForecast.weatherOverlap != null) {
                WeatherPeriod weatherPeriod = dayForecast.weatherStarts ? dayForecast.weatherPeriod : dayForecast.weatherOverlap.weatherPeriod;
                if (weatherPeriod != null) {
                    windDirection = weatherPeriod.getWindAngleDegrees();
                    WeatherPeriod.WeatherStage stage = weatherPeriod.getStageForWorldAge(this.climateValues.getCacheWorldAgeHours());
                    if (stage != null) {
                        if (!dayForecast.weatherStages.contains(stage.getStageID())) {
                            dayForecast.weatherStages.add(stage.getStageID());
                        }

                        switch (stage.getStageID()) {
                            case 1:
                                dayForecast.hasHeavyRain = true;
                            case 4:
                            case 5:
                            case 6:
                            default:
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * 0.25F;
                                cloudiness = 0.35F + 0.5F * weatherPeriod.getTotalStrength();
                                break;
                            case 2:
                                windPower = 0.5F * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5F + 0.5F * windPower;
                                dayForecast.hasHeavyRain = true;
                                break;
                            case 3:
                                windPower = 0.2F + 0.5F * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5F + 0.5F * windPower;
                                dayForecast.hasStorm = true;
                                break;
                            case 7:
                                dayForecast.chanceOnSnow = true;
                                windPower = 0.75F + 0.25F * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5F + 0.5F * windPower;
                                dayForecast.hasBlizzard = true;
                                break;
                            case 8:
                                windPower = 0.4F + 0.6F * weatherPeriod.getTotalStrength();
                                temperature -= WeatherPeriod.getMaxTemperatureInfluence() * windPower;
                                cloudiness = 0.5F + 0.5F * windPower;
                                dayForecast.hasTropicalStorm = true;
                        }
                    } else if (dayForecast.weatherOverlap != null && i < dayForecast.weatherEndTime) {
                        dayForecast.weatherEndTime = i;
                    }
                }

                if (temperature < 0.0F) {
                    dayForecast.chanceOnSnow = true;
                }
            }

            dayForecast.temperature.add(temperature, isDayTime);
            dayForecast.humidity.add(humidity, isDayTime);
            dayForecast.windDirection.add(windDirection, isDayTime);
            dayForecast.windPower.add(windPower, isDayTime);
            dayForecast.cloudiness.add(cloudiness, isDayTime);
        }

        dayForecast.temperature.calculate();
        dayForecast.humidity.calculate();
        dayForecast.windDirection.calculate();
        dayForecast.windPower.calculate();
        dayForecast.cloudiness.calculate();
        dayForecast.hasFog = this.climateValues.isDayDoFog();
        dayForecast.fogStrength = this.climateValues.getDayFogStrength();
        dayForecast.fogDuration = this.climateValues.getDayFogDuration();
    }

    private ClimateForecaster.DayForecast getWeatherOverlap(int index, float hour) {
        int start = Math.max(0, index - 10);
        if (start == index) {
            return null;
        } else {
            for (int i = start; i < index; i++) {
                if (this.forecasts[i].weatherStarts) {
                    float days = (float)this.forecasts[i].weatherPeriod.getDuration() / 24.0F;
                    float end = i + this.forecasts[i].weatherStartTime / 24.0F;
                    end += days;
                    float stamptoday = index + hour / 24.0F;
                    if (end > stamptoday) {
                        return this.forecasts[i];
                    }
                }
            }

            return null;
        }
    }

    public int getDaysTillFirstWeather() {
        int first = -1;

        for (int i = 10; i < this.forecasts.length - 1; i++) {
            if (this.forecasts[i].weatherStarts && first < 0) {
                first = i;
            }
        }

        return first;
    }

    @UsedFromLua
    public static class DayForecast {
        private int indexOffset;
        private String name = "Day x";
        private WeatherPeriod weatherPeriod;
        private final ClimateForecaster.ForecastValue temperature = new ClimateForecaster.ForecastValue();
        private final ClimateForecaster.ForecastValue humidity = new ClimateForecaster.ForecastValue();
        private final ClimateForecaster.ForecastValue windDirection = new ClimateForecaster.ForecastValue();
        private final ClimateForecaster.ForecastValue windPower = new ClimateForecaster.ForecastValue();
        private final ClimateForecaster.ForecastValue cloudiness = new ClimateForecaster.ForecastValue();
        private boolean weatherStarts;
        private float weatherStartTime;
        private float weatherEndTime = 24.0F;
        private boolean chanceOnSnow;
        private String airFrontString = "";
        private boolean hasFog;
        private float fogStrength;
        private float fogDuration;
        private ClimateManager.AirFront airFront;
        private ClimateForecaster.DayForecast weatherOverlap;
        private boolean hasHeavyRain;
        private boolean hasStorm;
        private boolean hasTropicalStorm;
        private boolean hasBlizzard;
        private float dawn;
        private float dusk;
        private float dayLightHours;
        private final ArrayList<Integer> weatherStages = new ArrayList<>();

        public int getIndexOffset() {
            return this.indexOffset;
        }

        public String getName() {
            return this.name;
        }

        public ClimateForecaster.ForecastValue getTemperature() {
            return this.temperature;
        }

        public ClimateForecaster.ForecastValue getHumidity() {
            return this.humidity;
        }

        public ClimateForecaster.ForecastValue getWindDirection() {
            return this.windDirection;
        }

        public ClimateForecaster.ForecastValue getWindPower() {
            return this.windPower;
        }

        public ClimateForecaster.ForecastValue getCloudiness() {
            return this.cloudiness;
        }

        public WeatherPeriod getWeatherPeriod() {
            return this.weatherPeriod;
        }

        public boolean isWeatherStarts() {
            return this.weatherStarts;
        }

        public float getWeatherStartTime() {
            return this.weatherStartTime;
        }

        public float getWeatherEndTime() {
            return this.weatherEndTime;
        }

        public boolean isChanceOnSnow() {
            return this.chanceOnSnow;
        }

        public String getAirFrontString() {
            return this.airFrontString;
        }

        public boolean isHasFog() {
            return this.hasFog;
        }

        public ClimateManager.AirFront getAirFront() {
            return this.airFront;
        }

        public ClimateForecaster.DayForecast getWeatherOverlap() {
            return this.weatherOverlap;
        }

        public String getMeanWindAngleString() {
            return ClimateManager.getWindAngleString(this.windDirection.getTotalMean());
        }

        public float getFogStrength() {
            return this.fogStrength;
        }

        public float getFogDuration() {
            return this.fogDuration;
        }

        public boolean isHasHeavyRain() {
            return this.hasHeavyRain;
        }

        public boolean isHasStorm() {
            return this.hasStorm;
        }

        public boolean isHasTropicalStorm() {
            return this.hasTropicalStorm;
        }

        public boolean isHasBlizzard() {
            return this.hasBlizzard;
        }

        public ArrayList<Integer> getWeatherStages() {
            return this.weatherStages;
        }

        public float getDawn() {
            return this.dawn;
        }

        public float getDusk() {
            return this.dusk;
        }

        public float getDayLightHours() {
            return this.dayLightHours;
        }

        private void reset() {
            this.weatherPeriod.stopWeatherPeriod();
            this.temperature.reset();
            this.humidity.reset();
            this.windDirection.reset();
            this.windPower.reset();
            this.cloudiness.reset();
            this.weatherStarts = false;
            this.weatherStartTime = 0.0F;
            this.weatherEndTime = 24.0F;
            this.chanceOnSnow = false;
            this.hasFog = false;
            this.fogStrength = 0.0F;
            this.fogDuration = 0.0F;
            this.weatherOverlap = null;
            this.hasHeavyRain = false;
            this.hasStorm = false;
            this.hasTropicalStorm = false;
            this.hasBlizzard = false;
            this.weatherStages.clear();
        }
    }

    @UsedFromLua
    public static class ForecastValue {
        private float dayMin;
        private float dayMax;
        private float dayMean;
        private int dayMeanTicks;
        private float nightMin;
        private float nightMax;
        private float nightMean;
        private int nightMeanTicks;
        private float totalMin;
        private float totalMax;
        private float totalMean;
        private int totalMeanTicks;

        public ForecastValue() {
            this.reset();
        }

        public float getDayMin() {
            return this.dayMin;
        }

        public float getDayMax() {
            return this.dayMax;
        }

        public float getDayMean() {
            return this.dayMean;
        }

        public float getNightMin() {
            return this.nightMin;
        }

        public float getNightMax() {
            return this.nightMax;
        }

        public float getNightMean() {
            return this.nightMean;
        }

        public float getTotalMin() {
            return this.totalMin;
        }

        public float getTotalMax() {
            return this.totalMax;
        }

        public float getTotalMean() {
            return this.totalMean;
        }

        protected void add(float val, boolean isDay) {
            if (isDay) {
                if (val < this.dayMin) {
                    this.dayMin = val;
                }

                if (val > this.dayMax) {
                    this.dayMax = val;
                }

                this.dayMean += val;
                this.dayMeanTicks++;
            } else {
                if (val < this.nightMin) {
                    this.nightMin = val;
                }

                if (val > this.nightMax) {
                    this.nightMax = val;
                }

                this.nightMean += val;
                this.nightMeanTicks++;
            }

            if (val < this.totalMin) {
                this.totalMin = val;
            }

            if (val > this.totalMax) {
                this.totalMax = val;
            }

            this.totalMean += val;
            this.totalMeanTicks++;
        }

        protected void calculate() {
            if (this.totalMeanTicks <= 0) {
                this.totalMean = 0.0F;
            } else {
                this.totalMean = this.totalMean / this.totalMeanTicks;
            }

            if (this.dayMeanTicks <= 0) {
                this.dayMin = this.totalMin;
                this.dayMax = this.totalMax;
                this.dayMean = this.totalMean;
            } else {
                this.dayMean = this.dayMean / this.dayMeanTicks;
            }

            if (this.nightMeanTicks <= 0) {
                this.nightMin = this.totalMin;
                this.nightMax = this.totalMax;
                this.nightMean = this.totalMean;
            } else {
                this.nightMean = this.nightMean / this.nightMeanTicks;
            }
        }

        protected void reset() {
            this.dayMin = 10000.0F;
            this.dayMax = -10000.0F;
            this.dayMean = 0.0F;
            this.dayMeanTicks = 0;
            this.nightMin = 10000.0F;
            this.nightMax = -10000.0F;
            this.nightMean = 0.0F;
            this.nightMeanTicks = 0;
            this.totalMin = 10000.0F;
            this.totalMax = -10000.0F;
            this.totalMean = 0.0F;
            this.totalMeanTicks = 0;
        }
    }
}
