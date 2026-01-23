// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.dbg;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.SimplexNoise;
import zombie.iso.weather.WeatherPeriod;
import zombie.network.GameClient;

public class ClimMngrDebug extends ClimateManager {
    private GregorianCalendar calendar;
    private double worldAgeHours;
    private double worldAgeHoursStart;
    private double weatherPeriodTime;
    private double simplexOffsetA;
    private final ClimateManager.AirFront currentFront;
    private final WeatherPeriod weatherPeriod;
    private boolean tickIsDayChange;
    public ArrayList<ClimMngrDebug.RunInfo> runs = new ArrayList<>();
    private ClimMngrDebug.RunInfo currentRun;
    private ErosionSeason season;
    private final int totalDaysPeriodIndexMod = 5;
    private boolean doOverrideSandboxRainMod;
    private int sandboxRainModOverride = 3;
    private int durDays;
    private static final int WEATHER_NORMAL = 0;
    private static final int WEATHER_STORM = 1;
    private static final int WEATHER_TROPICAL = 2;
    private static final int WEATHER_BLIZZARD = 3;
    private FileWriter writer;

    public ClimMngrDebug() {
        this.currentFront = new ClimateManager.AirFront();
        this.weatherPeriod = new WeatherPeriod(this, null);
        this.weatherPeriod.setPrintStuff(false);
    }

    public void setRainModOverride(int rainmod) {
        this.doOverrideSandboxRainMod = true;
        this.sandboxRainModOverride = rainmod;
    }

    public void unsetRainModOverride() {
        this.doOverrideSandboxRainMod = false;
        this.sandboxRainModOverride = 3;
    }

    public void SimulateDays(int amountOfDays, int totalRuns) {
        this.durDays = amountOfDays;
        DebugLog.log("Starting " + totalRuns + " simulations of " + amountOfDays + " days per run...");
        int startMonth = 0;
        int startDay = 0;
        DebugLog.log("Year: " + GameTime.instance.getYear() + ", Month: 0, Day: 0");

        for (int k = 0; k < totalRuns; k++) {
            this.calendar = new GregorianCalendar(GameTime.instance.getYear(), 0, 0, 0, 0);
            this.season = ClimateManager.getInstance().getSeason().clone();
            this.season
                .init(
                    this.season.getLat(),
                    this.season.getTempMax(),
                    this.season.getTempMin(),
                    this.season.getTempDiff(),
                    this.season.getSeasonLag(),
                    this.season.getHighNoon(),
                    Rand.Next(0, 255),
                    Rand.Next(0, 255),
                    Rand.Next(0, 255)
                );
            this.simplexOffsetA = Rand.Next(0, 8000);
            this.worldAgeHours = 250.0;
            this.weatherPeriodTime = this.worldAgeHours;
            this.worldAgeHoursStart = this.worldAgeHours;
            double airMassNoiseFrequencyMod = this.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier());
            float airMass = (float)SimplexNoise.noise(this.simplexOffsetA, this.worldAgeHours / airMassNoiseFrequencyMod);
            int airType = airMass < 0.0F ? -1 : 1;
            this.currentFront.setFrontType(airType);
            this.weatherPeriod.stopWeatherPeriod();
            double nextDay = this.worldAgeHours + 24.0;
            int totalhours = amountOfDays * 24;
            this.currentRun = new ClimMngrDebug.RunInfo();
            this.currentRun.durationDays = amountOfDays;
            this.currentRun.durationHours = totalhours;
            this.currentRun.seedA = this.simplexOffsetA;
            this.runs.add(this.currentRun);

            for (int i = 0; i < totalhours; i++) {
                this.tickIsDayChange = false;
                this.worldAgeHours++;
                if (this.worldAgeHours >= nextDay) {
                    this.tickIsDayChange = true;
                    nextDay += 24.0;
                    this.calendar.add(5, 1);
                    int day = this.calendar.get(5);
                    int month = this.calendar.get(2);
                    int year = this.calendar.get(1);
                    this.season.setDay(day, month, year);
                }

                this.update_sim();
            }
        }

        this.saveData();
    }

    private void update_sim() {
        double airMassNoiseFrequencyMod = this.getAirMassNoiseFrequencyMod(SandboxOptions.instance.getRainModifier());
        float airMass = (float)SimplexNoise.noise(this.simplexOffsetA, this.worldAgeHours / airMassNoiseFrequencyMod);
        int airType = airMass < 0.0F ? -1 : 1;
        if (this.currentFront.getType() != airType) {
            if (this.worldAgeHours > this.weatherPeriodTime) {
                this.weatherPeriod.initSimulationDebug(this.currentFront, this.worldAgeHours);
                this.recordAndCloseWeatherPeriod();
            }

            this.currentFront.setFrontType(airType);
        }

        if (!winterIsComing
            && !theDescendingFog
            && this.worldAgeHours >= this.worldAgeHoursStart + 72.0
            && this.worldAgeHours <= this.worldAgeHoursStart + 96.0
            && !this.weatherPeriod.isRunning()
            && this.worldAgeHours > this.weatherPeriodTime
            && Rand.Next(0, 1000) < 50) {
            this.triggerCustomWeatherStage(3, 10.0F);
        }

        if (this.tickIsDayChange) {
            double daymidpoint = Math.floor(this.worldAgeHours) + 12.0;
            float noiseAirmass = (float)SimplexNoise.noise(this.simplexOffsetA, daymidpoint / airMassNoiseFrequencyMod);
            airType = noiseAirmass < 0.0F ? -1 : 1;
            if (airType == this.currentFront.getType()) {
                this.currentFront.addDaySample(noiseAirmass);
            }
        }
    }

    private void recordAndCloseWeatherPeriod() {
        if (this.weatherPeriod.isRunning()) {
            if (this.worldAgeHours - this.weatherPeriodTime > 0.0) {
                this.currentRun.addRecord(this.worldAgeHours - this.weatherPeriodTime);
            }

            this.weatherPeriodTime = this.worldAgeHours + Math.ceil(this.weatherPeriod.getDuration());
            boolean storm = false;
            boolean tropical = false;
            boolean blizzard = false;

            for (WeatherPeriod.WeatherStage stage : this.weatherPeriod.getWeatherStages()) {
                if (stage.getStageID() == 3) {
                    storm = true;
                }

                if (stage.getStageID() == 8) {
                    tropical = true;
                }

                if (stage.getStageID() == 7) {
                    blizzard = true;
                }
            }

            this.currentRun
                .addRecord(
                    this.currentFront.getType(), this.weatherPeriod.getDuration(), this.weatherPeriod.getFrontCache().getStrength(), storm, tropical, blizzard
                );
        }

        this.weatherPeriod.stopWeatherPeriod();
    }

    @Override
    public boolean triggerCustomWeatherStage(int stage, float duration) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            ClimateManager.AirFront front = new ClimateManager.AirFront();
            front.setFrontType(1);
            front.setStrength(0.95F);
            this.weatherPeriod.initSimulationDebug(front, this.worldAgeHours, stage, duration);
            this.recordAndCloseWeatherPeriod();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected double getAirMassNoiseFrequencyMod(int sandboxRain) {
        return this.doOverrideSandboxRainMod ? super.getAirMassNoiseFrequencyMod(this.sandboxRainModOverride) : super.getAirMassNoiseFrequencyMod(sandboxRain);
    }

    @Override
    protected float getRainTimeMultiplierMod(int sandboxRain) {
        return this.doOverrideSandboxRainMod ? super.getRainTimeMultiplierMod(this.sandboxRainModOverride) : super.getRainTimeMultiplierMod(sandboxRain);
    }

    @Override
    public ErosionSeason getSeason() {
        return this.season;
    }

    @Override
    public float getDayMeanTemperature() {
        return this.season.getDayMeanTemperature();
    }

    @Override
    public void resetOverrides() {
    }

    private ClimMngrDebug.RunInfo calculateTotal() {
        ClimMngrDebug.RunInfo total = new ClimMngrDebug.RunInfo();
        total.totalDaysPeriod = new int[50];
        double totalDurPeriod = 0.0;
        double totalDurEmpty = 0.0;
        float totalStr = 0.0F;
        float totalWarmStr = 0.0F;
        float totalColdStr = 0.0F;

        for (ClimMngrDebug.RunInfo r : this.runs) {
            if (r.totalPeriodDuration < total.mostDryPeriod) {
                total.mostDryPeriod = r.totalPeriodDuration;
            }

            if (r.totalPeriodDuration > total.mostWetPeriod) {
                total.mostWetPeriod = r.totalPeriodDuration;
            }

            total.totalPeriodDuration = total.totalPeriodDuration + r.totalPeriodDuration;
            if (r.longestPeriod > total.longestPeriod) {
                total.longestPeriod = r.longestPeriod;
            }

            if (r.shortestPeriod < total.shortestPeriod) {
                total.shortestPeriod = r.shortestPeriod;
            }

            total.totalPeriods = total.totalPeriods + r.totalPeriods;
            total.averagePeriod = total.averagePeriod + r.averagePeriod;
            if (r.longestEmpty > total.longestEmpty) {
                total.longestEmpty = r.longestEmpty;
            }

            if (r.shortestEmpty < total.shortestEmpty) {
                total.shortestEmpty = r.shortestEmpty;
            }

            total.totalEmpty = total.totalEmpty + r.totalEmpty;
            total.averageEmpty = total.averageEmpty + r.averageEmpty;
            if (r.highestStrength > total.highestStrength) {
                total.highestStrength = r.highestStrength;
            }

            if (r.lowestStrength < total.lowestStrength) {
                total.lowestStrength = r.lowestStrength;
            }

            total.averageStrength = total.averageStrength + r.averageStrength;
            if (r.highestWarmStrength > total.highestWarmStrength) {
                total.highestWarmStrength = r.highestWarmStrength;
            }

            if (r.lowestWarmStrength < total.lowestWarmStrength) {
                total.lowestWarmStrength = r.lowestWarmStrength;
            }

            total.averageWarmStrength = total.averageWarmStrength + r.averageWarmStrength;
            if (r.highestColdStrength > total.highestColdStrength) {
                total.highestColdStrength = r.highestColdStrength;
            }

            if (r.lowestColdStrength < total.lowestColdStrength) {
                total.lowestColdStrength = r.lowestColdStrength;
            }

            total.averageColdStrength = total.averageColdStrength + r.averageColdStrength;
            total.countNormalWarm = total.countNormalWarm + r.countNormalWarm;
            total.countNormalCold = total.countNormalCold + r.countNormalCold;
            total.countStorm = total.countStorm + r.countStorm;
            total.countTropical = total.countTropical + r.countTropical;
            total.countBlizzard = total.countBlizzard + r.countBlizzard;

            for (int i = 0; i < r.dayCountPeriod.length; i++) {
                total.dayCountPeriod[i] = total.dayCountPeriod[i] + r.dayCountPeriod[i];
            }

            for (int i = 0; i < r.dayCountWarmPeriod.length; i++) {
                total.dayCountWarmPeriod[i] = total.dayCountWarmPeriod[i] + r.dayCountWarmPeriod[i];
            }

            for (int i = 0; i < r.dayCountColdPeriod.length; i++) {
                total.dayCountColdPeriod[i] = total.dayCountColdPeriod[i] + r.dayCountColdPeriod[i];
            }

            for (int i = 0; i < r.dayCountEmpty.length; i++) {
                total.dayCountEmpty[i] = total.dayCountEmpty[i] + r.dayCountEmpty[i];
            }

            for (int i = 0; i < r.exceedingPeriods.size(); i++) {
                total.exceedingPeriods.add(r.exceedingPeriods.get(i));
            }

            for (int i = 0; i < r.exceedingEmpties.size(); i++) {
                total.exceedingEmpties.add(r.exceedingEmpties.get(i));
            }

            int days = (int)(r.totalPeriodDuration / 120.0);
            if (days < total.totalDaysPeriod.length) {
                total.totalDaysPeriod[days]++;
            } else {
                DebugLog.log("Total days Period is longer than allowed array, days = " + days * 5);
            }
        }

        if (!this.runs.isEmpty()) {
            int div = this.runs.size();
            total.totalPeriodDuration /= div;
            total.averagePeriod /= div;
            total.averageEmpty /= div;
            total.averageStrength /= div;
            total.averageWarmStrength /= div;
            total.averageColdStrength /= div;
        }

        return total;
    }

    private void saveData() {
        if (this.runs.size() > 0) {
            try {
                for (ClimMngrDebug.RunInfo r : this.runs) {
                    r.calculate();
                }

                ClimMngrDebug.RunInfo total = this.calculateTotal();
                String SAVE_FILE = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                ZomboidFileSystem.instance.getFileInCurrentSave("climate").mkdirs();
                File path = ZomboidFileSystem.instance.getFileInCurrentSave("climate");
                if (path.exists() && path.isDirectory()) {
                    String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("climate", SAVE_FILE + ".txt");
                    DebugLog.log("Attempting to save test data to: " + fileName);
                    File f = new File(fileName);
                    DebugLog.log("Saving climate test data: " + fileName);

                    try (FileWriter w = new FileWriter(f, false)) {
                        this.writer = w;
                        int div = this.runs.size();
                        this.write("Simulation results." + System.lineSeparator());
                        this.write("Runs: " + this.runs.size() + ", days per cycle: " + this.durDays);
                        if (this.doOverrideSandboxRainMod) {
                            this.write("RainModifier used: " + this.sandboxRainModOverride);
                        } else {
                            this.write("RainModifier used: " + SandboxOptions.instance.getRainModifier());
                        }

                        this.write("");
                        this.write("-------------------------------------------------------------------");
                        this.write(" TOTALS OVERVIEW");
                        this.write("-------------------------------------------------------------------");
                        this.write("");
                        this.write("Total weather periods: " + total.totalPeriods + ", average per cycle: " + total.totalPeriods / div);
                        this.write("Longest weather: " + this.formatDuration(total.longestPeriod));
                        this.write("Shortest weather: " + this.formatDuration(total.shortestPeriod));
                        this.write("Average weather: " + this.formatDuration(total.averagePeriod));
                        this.write("");
                        this.write("Average total weather days per cycle: " + this.formatDuration(total.totalPeriodDuration));
                        this.write("");
                        this.write("Driest cycle total weather days: " + this.formatDuration(total.mostDryPeriod));
                        this.write("Wettest cycle total weather days: " + this.formatDuration(total.mostWetPeriod));
                        this.write("");
                        this.write("Total clear periods: " + total.totalEmpty + ", average per cycle: " + total.totalEmpty / div);
                        this.write("Longest clear: " + this.formatDuration(total.longestEmpty));
                        this.write("Shortest clear: " + this.formatDuration(total.shortestEmpty));
                        this.write("Average clear: " + this.formatDuration(total.averageEmpty));
                        this.write("");
                        this.write("Highest Front strength: " + total.highestStrength);
                        this.write("Lowest Front strength: " + total.lowestStrength);
                        this.write("Average Front strength: " + total.averageStrength);
                        this.write("");
                        this.write("Highest WarmFront strength: " + total.highestWarmStrength);
                        this.write("Lowest WarmFront strength: " + total.lowestWarmStrength);
                        this.write("Average WarmFront strength: " + total.averageWarmStrength);
                        this.write("");
                        this.write("Highest ColdFront strength: " + total.highestColdStrength);
                        this.write("Lowest ColdFront strength: " + total.lowestColdStrength);
                        this.write("Average ColdFront strength: " + total.averageColdStrength);
                        this.write("");
                        this.write("Weather period types:");
                        double divd = div;
                        this.write("Normal warm: " + total.countNormalWarm + ", average: " + this.round(total.countNormalWarm / divd));
                        this.write("Normal cold: " + total.countNormalCold + ", average: " + this.round(total.countNormalCold / divd));
                        this.write("Normal storm: " + total.countStorm + ", average: " + this.round((double)total.countStorm / div));
                        this.write("Normal tropical: " + total.countTropical + ", average: " + this.round(total.countTropical / divd));
                        this.write("Normal blizzard: " + total.countBlizzard + ", average: " + this.round(total.countBlizzard / divd));
                        this.write("");
                        this.write("Distribution duration in days (total periods)");
                        this.printCountTable(w, total.dayCountPeriod);
                        this.write("");
                        this.write("Distribution duration in days (WARM periods)");
                        this.printCountTable(w, total.dayCountWarmPeriod);
                        this.write("");
                        this.write("Distribution duration in days (COLD periods)");
                        this.printCountTable(w, total.dayCountColdPeriod);
                        this.write("");
                        this.write("Distribution duration in days (clear periods)");
                        this.printCountTable(w, total.dayCountEmpty);
                        this.write("");
                        this.write("Amount of weather periods exceeding threshold: " + total.exceedingPeriods.size());
                        if (!total.exceedingPeriods.isEmpty()) {
                            for (Integer integer : total.exceedingPeriods) {
                                this.writer.write(integer + " days, ");
                            }
                        }

                        this.write("");
                        this.write("");
                        this.write("Amount of clear periods exceeding threshold: " + total.exceedingEmpties.size());
                        if (!total.exceedingEmpties.isEmpty()) {
                            for (Integer integer : total.exceedingEmpties) {
                                this.writer.write(integer + " days, ");
                            }
                        }

                        this.write("");
                        this.write("");
                        this.write("Distribution duration total weather days:");
                        this.printCountTable(this.writer, total.totalDaysPeriod, 5);
                        this.writeDataExtremes();
                        this.writer = null;
                    } catch (Exception var20) {
                        var20.printStackTrace();
                    }

                    f = ZomboidFileSystem.instance.getFileInCurrentSave("climate", SAVE_FILE + "_DATA.txt");

                    try (FileWriter w = new FileWriter(f, false)) {
                        this.writer = w;
                        this.writeData();
                        this.writer = null;
                    } catch (Exception var17) {
                        var17.printStackTrace();
                    }

                    f = ZomboidFileSystem.instance.getFileInCurrentSave("climate", SAVE_FILE + "_PATTERNS.txt");

                    try (FileWriter w = new FileWriter(f, false)) {
                        this.writer = w;
                        this.writePatterns();
                        this.writer = null;
                    } catch (Exception var14) {
                        var14.printStackTrace();
                    }
                }
            } catch (Exception var21) {
                var21.printStackTrace();
            }
        }
    }

    private double round(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private void writeRunInfo(ClimMngrDebug.RunInfo r, int id) throws Exception {
        this.write("-------------------------------------------------------------------");
        this.write(" RUN NR: " + id);
        this.write("-------------------------------------------------------------------");
        this.write("");
        this.write("Total weather periods: " + r.totalPeriods);
        this.write("Longest weather: " + this.formatDuration(r.longestPeriod));
        this.write("Shortest weather: " + this.formatDuration(r.shortestPeriod));
        this.write("Average weather: " + this.formatDuration(r.averagePeriod));
        this.write("");
        this.write("Total weather days for cycle: " + this.formatDuration(r.totalPeriodDuration));
        this.write("");
        this.write("Total clear periods: " + r.totalEmpty);
        this.write("Longest clear: " + this.formatDuration(r.longestEmpty));
        this.write("Shortest clear: " + this.formatDuration(r.shortestEmpty));
        this.write("Average clear: " + this.formatDuration(r.averageEmpty));
        this.write("");
        this.write("Highest Front strength: " + r.highestStrength);
        this.write("Lowest Front strength: " + r.lowestStrength);
        this.write("Average Front strength: " + r.averageStrength);
        this.write("");
        this.write("Highest WarmFront strength: " + r.highestWarmStrength);
        this.write("Lowest WarmFront strength: " + r.lowestWarmStrength);
        this.write("Average WarmFront strength: " + r.averageWarmStrength);
        this.write("");
        this.write("Highest ColdFront strength: " + r.highestColdStrength);
        this.write("Lowest ColdFront strength: " + r.lowestColdStrength);
        this.write("Average ColdFront strength: " + r.averageColdStrength);
        this.write("");
        this.write("Weather period types:");
        this.write("Normal warm: " + r.countNormalWarm);
        this.write("Normal cold: " + r.countNormalCold);
        this.write("Normal storm: " + r.countStorm);
        this.write("Normal tropical: " + r.countTropical);
        this.write("Normal blizzard: " + r.countBlizzard);
        this.write("");
        this.write("Distribution duration in days (total periods)");
        this.printCountTable(this.writer, r.dayCountPeriod);
        this.write("");
        this.write("Distribution duration in days (WARM periods)");
        this.printCountTable(this.writer, r.dayCountWarmPeriod);
        this.write("");
        this.write("Distribution duration in days (COLD periods)");
        this.printCountTable(this.writer, r.dayCountColdPeriod);
        this.write("");
        this.write("Distribution duration in days (clear periods)");
        this.printCountTable(this.writer, r.dayCountEmpty);
        this.write("");
        this.write("Amount of weather periods exceeding threshold: " + r.exceedingPeriods.size());
        if (!r.exceedingPeriods.isEmpty()) {
            for (Integer integer : r.exceedingPeriods) {
                this.write(integer + " days.");
            }
        }

        this.write("");
        this.write("Amount of clear periods exceeding threshold: " + r.exceedingEmpties.size());
        if (!r.exceedingEmpties.isEmpty()) {
            for (Integer integer : r.exceedingEmpties) {
                this.write(integer + " days.");
            }
        }
    }

    private void write(String str) throws Exception {
        this.writer.write(str + System.lineSeparator());
    }

    private void writeDataExtremes() throws Exception {
        int id = 0;
        int dryId = -1;
        int wetId = -1;
        ClimMngrDebug.RunInfo mostDry = null;
        ClimMngrDebug.RunInfo mostWet = null;

        for (ClimMngrDebug.RunInfo r : this.runs) {
            id++;
            if (mostDry == null || r.totalPeriodDuration < mostDry.totalPeriodDuration) {
                mostDry = r;
                dryId = id;
            }

            if (mostWet == null || r.totalPeriodDuration > mostWet.totalPeriodDuration) {
                mostWet = r;
                wetId = id;
            }
        }

        this.write("");
        this.write("MOST DRY RUN:");
        if (mostDry != null) {
            this.writeRunInfo(mostDry, dryId);
        }

        this.write("");
        this.write("MOST WET RUN:");
        if (mostWet != null) {
            this.writeRunInfo(mostWet, wetId);
        }
    }

    private void writeData() throws Exception {
        int id = 0;

        for (ClimMngrDebug.RunInfo r : this.runs) {
            this.writeRunInfo(r, ++id);
        }
    }

    private void writePatterns() throws Exception {
        String clear = "-";
        String weather = "#";
        String storm = "S";
        String tropical = "T";
        String blizzard = "B";
        int c = 0;
        int id = 0;

        for (ClimMngrDebug.RunInfo r : this.runs) {
            id = 0;

            for (ClimMngrDebug.RecordInfo ri : r.records) {
                c = (int)Math.ceil(ri.durationHours / 24.0);
                String s;
                if (ri.isWeather && ri.weatherType == 1) {
                    s = new String(new char[c]).replace("\u0000", "S");
                } else if (ri.isWeather && ri.weatherType == 2) {
                    s = new String(new char[c]).replace("\u0000", "T");
                } else if (ri.isWeather && ri.weatherType == 3) {
                    s = new String(new char[c]).replace("\u0000", "B");
                } else if (id == 0 && !ri.isWeather && c >= 2) {
                    s = new String(new char[c - 1]).replace("\u0000", "-");
                } else {
                    s = new String(new char[c]).replace("\u0000", ri.isWeather ? "#" : "-");
                }

                this.writer.write(s);
                id++;
            }

            this.writer.write(System.lineSeparator());
        }
    }

    private void printCountTable(FileWriter w, int[] table) throws Exception {
        this.printCountTable(w, table, 1);
    }

    private void printCountTable(FileWriter w, int[] table, int indexMod) throws Exception {
        if (table != null && table.length > 0) {
            int t = 0;

            for (int i = 0; i < table.length; i++) {
                if (table[i] > t) {
                    t = table[i];
                }
            }

            this.write("    DAYS   COUNT GRAPH");
            float mod = 50.0F / t;
            if (t > 0) {
                for (int ix = 0; ix < table.length; ix++) {
                    String s = "";
                    s = s + String.format("%1$8s", ix * indexMod + "-" + (ix * indexMod + indexMod));
                    int v = table[ix];
                    s = s + String.format("%1$8s", v);
                    s = s + " ";
                    int cnt = (int)(v * mod);
                    if (cnt > 0) {
                        s = s + new String(new char[cnt]).replace("\u0000", "#");
                    } else if (v > 0) {
                        s = s + "*";
                    }

                    this.write(s);
                }
            }
        }
    }

    private String formatDuration(double duration) {
        int days = (int)(duration / 24.0);
        int hours = (int)(duration - days * 24);
        return days + " days, " + hours + " hours.";
    }

    private class RecordInfo {
        public boolean isWeather;
        public float strength;
        public int airType;
        public double durationHours;
        public int weatherType;

        private RecordInfo() {
            Objects.requireNonNull(ClimMngrDebug.this);
            super();
        }
    }

    private class RunInfo {
        public double seedA;
        public int durationDays;
        public double durationHours;
        public ArrayList<ClimMngrDebug.RecordInfo> records;
        public double totalPeriodDuration;
        public double longestPeriod;
        public double shortestPeriod;
        public int totalPeriods;
        public double averagePeriod;
        public double longestEmpty;
        public double shortestEmpty;
        public int totalEmpty;
        public double averageEmpty;
        public float highestStrength;
        public float lowestStrength;
        public float averageStrength;
        public float highestWarmStrength;
        public float lowestWarmStrength;
        public float averageWarmStrength;
        public float highestColdStrength;
        public float lowestColdStrength;
        public float averageColdStrength;
        public int countNormalWarm;
        public int countNormalCold;
        public int countStorm;
        public int countTropical;
        public int countBlizzard;
        public int[] dayCountPeriod;
        public int[] dayCountWarmPeriod;
        public int[] dayCountColdPeriod;
        public int[] dayCountEmpty;
        public ArrayList<Integer> exceedingPeriods;
        public ArrayList<Integer> exceedingEmpties;
        public double mostWetPeriod;
        public double mostDryPeriod;
        public int[] totalDaysPeriod;

        private RunInfo() {
            Objects.requireNonNull(ClimMngrDebug.this);
            super();
            this.records = new ArrayList<>();
            this.shortestPeriod = 9.99999999E8;
            this.shortestEmpty = 9.99999999E8;
            this.lowestStrength = 1.0F;
            this.lowestWarmStrength = 1.0F;
            this.lowestColdStrength = 1.0F;
            this.dayCountPeriod = new int[16];
            this.dayCountWarmPeriod = new int[16];
            this.dayCountColdPeriod = new int[16];
            this.dayCountEmpty = new int[75];
            this.exceedingPeriods = new ArrayList<>();
            this.exceedingEmpties = new ArrayList<>();
            this.mostDryPeriod = 9.99999999E8;
        }

        public ClimMngrDebug.RecordInfo addRecord(double duration) {
            ClimMngrDebug.RecordInfo record = ClimMngrDebug.this.new RecordInfo();
            record.durationHours = duration;
            record.isWeather = false;
            this.records.add(record);
            return record;
        }

        public ClimMngrDebug.RecordInfo addRecord(int airType, double duration, float strength, boolean storm, boolean tropical, boolean blizzard) {
            ClimMngrDebug.RecordInfo record = ClimMngrDebug.this.new RecordInfo();
            record.durationHours = duration;
            record.isWeather = true;
            record.airType = airType;
            record.strength = strength;
            record.weatherType = 0;
            if (storm) {
                record.weatherType = 1;
            } else if (tropical) {
                record.weatherType = 2;
            } else if (blizzard) {
                record.weatherType = 3;
            }

            this.records.add(record);
            return record;
        }

        public void calculate() {
            double totalDurPeriod = 0.0;
            double totalDurEmpty = 0.0;
            float totalStr = 0.0F;
            float totalWarmStr = 0.0F;
            float totalColdStr = 0.0F;
            int warmPeriods = 0;
            int coldPeriods = 0;

            for (ClimMngrDebug.RecordInfo n : this.records) {
                int days = (int)(n.durationHours / 24.0);
                if (n.isWeather) {
                    this.totalPeriodDuration = this.totalPeriodDuration + n.durationHours;
                    if (n.durationHours > this.longestPeriod) {
                        this.longestPeriod = n.durationHours;
                    }

                    if (n.durationHours < this.shortestPeriod) {
                        this.shortestPeriod = n.durationHours;
                    }

                    this.totalPeriods++;
                    totalDurPeriod += n.durationHours;
                    if (n.strength > this.highestStrength) {
                        this.highestStrength = n.strength;
                    }

                    if (n.strength < this.lowestStrength) {
                        this.lowestStrength = n.strength;
                    }

                    totalStr += n.strength;
                    if (n.airType == 1) {
                        warmPeriods++;
                        if (n.strength > this.highestWarmStrength) {
                            this.highestWarmStrength = n.strength;
                        }

                        if (n.strength < this.lowestWarmStrength) {
                            this.lowestWarmStrength = n.strength;
                        }

                        totalWarmStr += n.strength;
                        if (n.weatherType == 1) {
                            this.countStorm++;
                        } else if (n.weatherType == 2) {
                            this.countTropical++;
                        } else if (n.weatherType == 3) {
                            this.countBlizzard++;
                        } else {
                            this.countNormalWarm++;
                        }

                        if (days < this.dayCountWarmPeriod.length) {
                            this.dayCountWarmPeriod[days]++;
                        }
                    } else {
                        coldPeriods++;
                        if (n.strength > this.highestColdStrength) {
                            this.highestColdStrength = n.strength;
                        }

                        if (n.strength < this.lowestColdStrength) {
                            this.lowestColdStrength = n.strength;
                        }

                        totalColdStr += n.strength;
                        this.countNormalCold++;
                        if (days < this.dayCountColdPeriod.length) {
                            this.dayCountColdPeriod[days]++;
                        }
                    }

                    if (days < this.dayCountPeriod.length) {
                        this.dayCountPeriod[days]++;
                    } else {
                        DebugLog.log("Period is longer than allowed array, days = " + days);
                        this.exceedingPeriods.add(days);
                    }
                } else {
                    if (n.durationHours > this.longestEmpty) {
                        this.longestEmpty = n.durationHours;
                    }

                    if (n.durationHours < this.shortestEmpty) {
                        this.shortestEmpty = n.durationHours;
                    }

                    this.totalEmpty++;
                    totalDurEmpty += n.durationHours;
                    if (days < this.dayCountEmpty.length) {
                        this.dayCountEmpty[days]++;
                    } else {
                        DebugLog.log("No-Weather period is longer than allowed array, days = " + days);
                        this.exceedingEmpties.add(days);
                    }
                }
            }

            if (this.totalPeriods > 0) {
                this.averagePeriod = totalDurPeriod / this.totalPeriods;
                this.averageStrength = totalStr / this.totalPeriods;
                if (warmPeriods > 0) {
                    this.averageWarmStrength = totalWarmStr / warmPeriods;
                }

                if (coldPeriods > 0) {
                    this.averageColdStrength = totalColdStr / coldPeriods;
                }
            }

            if (this.totalEmpty > 0) {
                this.averageEmpty = totalDurEmpty / this.totalEmpty;
            }
        }
    }
}
