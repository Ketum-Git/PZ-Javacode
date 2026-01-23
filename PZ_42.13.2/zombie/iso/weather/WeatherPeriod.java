// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class WeatherPeriod {
    public static final int STAGE_START = 0;
    public static final int STAGE_SHOWERS = 1;
    public static final int STAGE_HEAVY_PRECIP = 2;
    public static final int STAGE_STORM = 3;
    public static final int STAGE_CLEARING = 4;
    public static final int STAGE_MODERATE = 5;
    public static final int STAGE_DRIZZLE = 6;
    public static final int STAGE_BLIZZARD = 7;
    public static final int STAGE_TROPICAL_STORM = 8;
    public static final int STAGE_INTERMEZZO = 9;
    public static final int STAGE_MODDED = 10;
    public static final int STAGE_KATEBOB_STORM = 11;
    public static final int STAGE_MAX = 12;
    public static final float FRONT_STRENGTH_THRESHOLD = 0.1F;
    private final ClimateManager climateManager;
    private final ClimateManager.AirFront frontCache = new ClimateManager.AirFront();
    private double startTime;
    private double duration;
    private double currentTime;
    private WeatherPeriod.WeatherStage currentStage;
    private final ArrayList<WeatherPeriod.WeatherStage> weatherStages = new ArrayList<>(20);
    private int weatherStageIndex;
    private final Stack<WeatherPeriod.WeatherStage> stagesPool = new Stack<>();
    private boolean isRunning;
    private float totalProgress;
    private float stageProgress;
    private float weatherNoise;
    private static final float maxTemperatureInfluence = 7.0F;
    private float temperatureInfluence;
    private float currentStrength;
    private float rainThreshold;
    private float windAngleDirMod = 1.0F;
    private boolean isThunderStorm;
    private boolean isTropicalStorm;
    private boolean isBlizzard;
    private float precipitationFinal;
    private final ThunderStorm thunderStorm;
    private ClimateColorInfo cloudColor = new ClimateColorInfo(0.4F, 0.2F, 0.2F, 0.4F);
    private final ClimateColorInfo cloudColorReddish = new ClimateColorInfo(0.66F, 0.12F, 0.12F, 0.4F);
    private final ClimateColorInfo cloudColorGreenish = new ClimateColorInfo(0.32F, 0.48F, 0.12F, 0.4F);
    private final ClimateColorInfo cloudColorBlueish = new ClimateColorInfo(0.16F, 0.48F, 0.48F, 0.4F);
    private final ClimateColorInfo cloudColorPurplish = new ClimateColorInfo(0.66F, 0.12F, 0.66F, 0.4F);
    private final ClimateColorInfo cloudColorTropical = new ClimateColorInfo(0.4F, 0.2F, 0.2F, 0.4F);
    private final ClimateColorInfo cloudColorBlizzard = new ClimateColorInfo(0.12F, 0.13F, 0.21F, 0.5F, 0.38F, 0.4F, 0.5F, 0.8F);
    private static boolean printStuff;
    private static float kateBobStormProgress = 0.45F;
    private int kateBobStormX = 2000;
    private int kateBobStormY = 2000;
    private final Random seededRandom;
    private final ClimateValues climateValues;
    private boolean isDummy;
    private boolean hasStartedInit;
    private static final HashMap<Integer, WeatherPeriod.StrLerpVal> cache = new HashMap<>();

    public WeatherPeriod(ClimateManager climmgr, ThunderStorm ts) {
        this.climateManager = climmgr;
        this.thunderStorm = ts;

        for (int i = 0; i < 30; i++) {
            this.stagesPool.push(new WeatherPeriod.WeatherStage());
        }

        printStuff = true;
        this.seededRandom = new Random(1984L);
        this.climateValues = climmgr.getClimateValuesCopy();
    }

    public void setDummy(boolean b) {
        this.isDummy = b;
    }

    public static float getMaxTemperatureInfluence() {
        return 7.0F;
    }

    public void setKateBobStormProgress(float progress) {
        kateBobStormProgress = PZMath.clamp_01(progress);
    }

    public void setKateBobStormCoords(int x, int y) {
        this.kateBobStormX = x;
        this.kateBobStormY = y;
    }

    public ClimateColorInfo getCloudColorReddish() {
        return this.cloudColorReddish;
    }

    public ClimateColorInfo getCloudColorGreenish() {
        return this.cloudColorGreenish;
    }

    public ClimateColorInfo getCloudColorBlueish() {
        return this.cloudColorBlueish;
    }

    public ClimateColorInfo getCloudColorPurplish() {
        return this.cloudColorPurplish;
    }

    public ClimateColorInfo getCloudColorTropical() {
        return this.cloudColorTropical;
    }

    public ClimateColorInfo getCloudColorBlizzard() {
        return this.cloudColorBlizzard;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public double getDuration() {
        return this.duration;
    }

    public ClimateManager.AirFront getFrontCache() {
        return this.frontCache;
    }

    public int getCurrentStageID() {
        return this.currentStage != null ? this.currentStage.stageId : -1;
    }

    public WeatherPeriod.WeatherStage getCurrentStage() {
        return this.currentStage;
    }

    public double getWeatherNoise() {
        return this.weatherNoise;
    }

    public float getCurrentStrength() {
        return this.currentStrength;
    }

    public float getRainThreshold() {
        return this.rainThreshold;
    }

    public boolean isThunderStorm() {
        return this.isThunderStorm;
    }

    public boolean isTropicalStorm() {
        return this.isTropicalStorm;
    }

    public boolean isBlizzard() {
        return this.isBlizzard;
    }

    public float getPrecipitationFinal() {
        return this.precipitationFinal;
    }

    public ClimateColorInfo getCloudColor() {
        return this.cloudColor;
    }

    public void setCloudColor(ClimateColorInfo cloudcol) {
        this.cloudColor = cloudcol;
    }

    public float getTotalProgress() {
        return this.totalProgress;
    }

    public float getStageProgress() {
        return this.stageProgress;
    }

    public boolean hasTropical() {
        for (int i = 0; i < this.weatherStages.size(); i++) {
            if (this.weatherStages.get(i).getStageID() == 8) {
                return true;
            }
        }

        return false;
    }

    public boolean hasStorm() {
        for (int i = 0; i < this.weatherStages.size(); i++) {
            if (this.weatherStages.get(i).getStageID() == 3) {
                return true;
            }
        }

        return false;
    }

    public boolean hasBlizzard() {
        for (int i = 0; i < this.weatherStages.size(); i++) {
            if (this.weatherStages.get(i).getStageID() == 7) {
                return true;
            }
        }

        return false;
    }

    public boolean hasHeavyRain() {
        for (int i = 0; i < this.weatherStages.size(); i++) {
            if (this.weatherStages.get(i).getStageID() == 2) {
                return true;
            }
        }

        return false;
    }

    public float getTotalStrength() {
        return this.frontCache.getStrength();
    }

    public WeatherPeriod.WeatherStage getStageForWorldAge(double worldAgeHours) {
        for (int i = 0; i < this.weatherStages.size(); i++) {
            if (worldAgeHours >= this.weatherStages.get(i).getStageStart() && worldAgeHours < this.weatherStages.get(i).getStageEnd()) {
                return this.weatherStages.get(i);
            }
        }

        return null;
    }

    public float getWindAngleDegrees() {
        return this.frontCache.getAngleDegrees();
    }

    public int getFrontType() {
        return this.frontCache.getType();
    }

    private void print(String str) {
        if (printStuff && !this.isDummy) {
            DebugLog.log(str);
        }
    }

    public void setPrintStuff(boolean b) {
        printStuff = b;
    }

    public boolean getPrintStuff() {
        return printStuff;
    }

    public void initSimulationDebug(ClimateManager.AirFront front, double hoursSinceStart) {
        GameTime gt = GameTime.getInstance();
        this.init(front, hoursSinceStart, gt.getYear(), gt.getMonth(), gt.getDayPlusOne(), -1, -1.0F);
    }

    public void initSimulationDebug(ClimateManager.AirFront front, double hoursSinceStart, int doThisStageOnly, float singleStageDuration) {
        GameTime gt = GameTime.getInstance();
        this.init(front, hoursSinceStart, gt.getYear(), gt.getMonth(), gt.getDayPlusOne(), doThisStageOnly, singleStageDuration);
    }

    protected void init(ClimateManager.AirFront front, double hoursSinceStart, int year, int month, int day) {
        this.init(front, hoursSinceStart, year, month, day, -1, -1.0F);
    }

    protected void init(ClimateManager.AirFront front, double hoursSinceStart, int year, int month, int day, int doThisStageOnly, float singleStageDuration) {
        this.climateValues.pollDate(year, month, day);
        this.reseed(year, month, day);
        this.hasStartedInit = false;
        if (this.startInit(front, hoursSinceStart)) {
            if (doThisStageOnly >= 0 && doThisStageOnly < 12) {
                this.createSingleStage(doThisStageOnly, singleStageDuration);
            } else {
                this.createWeatherPattern();
            }

            LuaEventManager.triggerEvent("OnWeatherPeriodStart", this);
            this.endInit();
        }
    }

    protected void reseed(int year, int month, int day) {
        int baseSeedA = (int)this.climateManager.getSimplexOffsetA();
        int baseSeedB = (int)this.climateManager.getSimplexOffsetB();
        long seed = (year - 1990) * 100000;
        seed += month * day * 1234;
        seed += (year - 1990) * month * 10000;
        seed += (baseSeedB - baseSeedA) * day;
        this.print("Reseeding weather period, new seed: " + seed);
        this.seededRandom.setSeed(seed);
    }

    private float RandNext(float min, float max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                min = max;
                max = max;
            }

            return min + this.seededRandom.nextFloat() * (max - min);
        }
    }

    private float RandNext(float bound) {
        return this.seededRandom.nextFloat() * bound;
    }

    private int RandNext(int min, int max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                min = max;
                max = max;
            }

            return min + this.seededRandom.nextInt(max - min);
        }
    }

    private int RandNext(int bound) {
        return this.seededRandom.nextInt(bound);
    }

    public boolean startCreateModdedPeriod(boolean warmFront, float strength, float angle) {
        double hoursSinceStart = GameTime.getInstance().getWorldAgeHours();
        ClimateManager.AirFront front = new ClimateManager.AirFront();
        float windAngle = ClimateManager.clamp(0.0F, 360.0F, angle);
        front.setFrontType(warmFront ? 1 : -1);
        front.setFrontWind(windAngle);
        front.setStrength(ClimateManager.clamp01(strength));
        GameTime gt = GameTime.getInstance();
        this.reseed(gt.getYear(), gt.getMonth(), gt.getDayPlusOne());
        this.hasStartedInit = false;
        if (!this.startInit(front, hoursSinceStart)) {
            return false;
        } else {
            this.print("WeatherPeriod: Creating MODDED weather pattern with strength = " + this.frontCache.getStrength());
            this.clearCurrentWeatherStages();
            return true;
        }
    }

    public boolean endCreateModdedPeriod() {
        if (!this.endInit()) {
            return false;
        } else {
            this.linkWeatherStages();
            this.duration = 0.0;

            for (int i = 0; i < this.weatherStages.size(); i++) {
                this.duration = this.duration + this.weatherStages.get(i).stageDuration;
            }

            this.print("WeatherPeriod: Duration = " + this.duration + ".");
            this.weatherStageIndex = 0;
            this.currentStage = this.weatherStages.get(this.weatherStageIndex).startStage(this.startTime);
            this.print("WeatherPeriod: PATTERN GENERATION FINISHED.");
            return true;
        }
    }

    private boolean startInit(ClimateManager.AirFront front, double hoursSinceStart) {
        if (!this.isRunning && !GameClient.client && !(front.getStrength() < 0.1F)) {
            this.startTime = hoursSinceStart;
            this.frontCache.copyFrom(front);
            if (this.frontCache.getAngleDegrees() >= 90.0F && this.frontCache.getAngleDegrees() < 270.0F) {
                this.windAngleDirMod = 1.0F;
            } else {
                this.windAngleDirMod = -1.0F;
            }

            this.hasStartedInit = true;
            return true;
        } else {
            return false;
        }
    }

    private boolean endInit() {
        if (this.hasStartedInit && !this.isRunning && !GameClient.client && !this.weatherStages.isEmpty()) {
            this.currentStrength = 0.0F;
            this.totalProgress = 0.0F;
            this.stageProgress = 0.0F;
            this.isRunning = true;
            if (GameServer.server && !this.isDummy) {
                this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)1, null);
            }

            this.hasStartedInit = false;
            return true;
        } else {
            this.hasStartedInit = false;
            return false;
        }
    }

    public void stopWeatherPeriod() {
        this.clearCurrentWeatherStages();
        this.currentStage = null;
        this.resetClimateManagerOverrides();
        this.isRunning = false;
        this.totalProgress = 0.0F;
        this.stageProgress = 0.0F;
        LuaEventManager.triggerEvent("OnWeatherPeriodStop", this);
    }

    public void writeNetWeatherData(ByteBuffer output) throws IOException {
        output.put((byte)(this.isRunning ? 1 : 0));
        if (this.isRunning) {
            output.put((byte)(this.isThunderStorm ? 1 : 0));
            output.put((byte)(this.isTropicalStorm ? 1 : 0));
            output.put((byte)(this.isBlizzard ? 1 : 0));
            output.putFloat(this.currentStrength);
            output.putDouble(this.duration);
            output.putFloat(this.totalProgress);
            output.putFloat(this.stageProgress);
        }
    }

    public void readNetWeatherData(ByteBuffer input) throws IOException {
        this.isRunning = input.get() == 1;
        if (this.isRunning) {
            this.isThunderStorm = input.get() == 1;
            this.isTropicalStorm = input.get() == 1;
            this.isBlizzard = input.get() == 1;
            this.currentStrength = input.getFloat();
            this.duration = input.getDouble();
            this.totalProgress = input.getFloat();
            this.stageProgress = input.getFloat();
        } else {
            this.isThunderStorm = false;
            this.isTropicalStorm = false;
            this.isBlizzard = false;
            this.currentStrength = 0.0F;
            this.duration = 0.0;
            this.totalProgress = 0.0F;
            this.stageProgress = 0.0F;
        }
    }

    public ArrayList<WeatherPeriod.WeatherStage> getWeatherStages() {
        return this.weatherStages;
    }

    private void linkWeatherStages() {
        WeatherPeriod.WeatherStage prev = null;
        WeatherPeriod.WeatherStage next = null;
        WeatherPeriod.WeatherStage curr = null;

        for (int i = 0; i < this.weatherStages.size(); i++) {
            curr = this.weatherStages.get(i);
            next = null;
            if (i + 1 < this.weatherStages.size()) {
                next = this.weatherStages.get(i + 1);
            }

            curr.previousStage = prev;
            curr.nextStage = next;
            curr.creationFinished = true;
            prev = curr;
        }
    }

    private void clearCurrentWeatherStages() {
        this.print("WeatherPeriod: Clearing existing stages...");

        for (WeatherPeriod.WeatherStage ws : this.weatherStages) {
            ws.reset();
            this.stagesPool.push(ws);
        }

        this.weatherStages.clear();
    }

    private void createSingleStage(int stage, float duration) {
        this.print("WeatherPeriod: Creating single stage weather pattern with strength = " + this.frontCache.getStrength());
        if (stage == 8) {
            this.cloudColor = this.cloudColorTropical;
        } else if (stage == 7) {
            this.cloudColor = this.cloudColorBlizzard;
        }

        this.clearCurrentWeatherStages();
        this.createAndAddStage(0, 1.0);
        this.createAndAddStage(stage, duration);
        this.createAndAddStage(4, 1.0);
        this.linkWeatherStages();
        this.duration = 0.0;

        for (int i = 0; i < this.weatherStages.size(); i++) {
            this.duration = this.duration + this.weatherStages.get(i).stageDuration;
        }

        this.print("WeatherPeriod: Duration = " + duration + ".");
        this.weatherStageIndex = 0;
        this.currentStage = this.weatherStages.get(this.weatherStageIndex).startStage(this.startTime);
        this.print("WeatherPeriod: PATTERN GENERATION FINISHED.");
    }

    private void createWeatherPattern() {
        this.print("WeatherPeriod: Creating weather pattern with strength = " + this.frontCache.getStrength());
        this.clearCurrentWeatherStages();
        ErosionSeason season = this.climateManager.getSeason();
        float meanTemperature = this.climateValues.getDayMeanTemperature();
        this.print("WeatherPeriod: Day mean temperature = " + meanTemperature + " C.");
        this.print("WeatherPeriod: season = " + season.getSeasonName());
        float stormChance = 0.0F;
        float tropicalChance = 0.0F;
        float blizzardChance = 0.0F;
        float rainTimeMultiplier = 1.0F;
        float rnd = this.RandNext(0.0F, 100.0F);
        int SEASON_ID = season.getSeason();
        boolean winterIsComing = IsoWorld.instance.getGameMode().equals("Winter is Coming");
        if (winterIsComing) {
            SEASON_ID = 5;
        }

        switch (SEASON_ID) {
            case 1:
                if (rnd < 75.0F) {
                    this.cloudColor = this.cloudColorGreenish;
                } else {
                    this.cloudColor = this.cloudColorBlueish;
                }

                stormChance = 75.0F;
                tropicalChance = 10.0F;
                blizzardChance = 0.0F;
                rainTimeMultiplier = 1.25F;
                break;
            case 2:
                if (rnd < 25.0F) {
                    this.cloudColor = this.cloudColorGreenish;
                } else {
                    this.cloudColor = this.cloudColorReddish;
                }

                stormChance = 60.0F;
                tropicalChance = 55.0F;
                blizzardChance = 0.0F;
                break;
            case 3:
                this.cloudColor = this.cloudColorReddish;
                stormChance = 75.0F;
                tropicalChance = 80.0F;
                blizzardChance = 0.0F;
                rainTimeMultiplier = 1.15F;
                break;
            case 4:
                if (rnd < 50.0F) {
                    this.cloudColor = this.cloudColorReddish;
                } else if (rnd < 75.0F) {
                    this.cloudColor = this.cloudColorPurplish;
                } else {
                    this.cloudColor = this.cloudColorBlueish;
                }

                stormChance = 100.0F;
                tropicalChance = 25.0F;
                blizzardChance = 0.0F;
                rainTimeMultiplier = 1.35F;
                break;
            case 5:
                if (rnd < 45.0F) {
                    this.cloudColor = this.cloudColorPurplish;
                } else {
                    this.cloudColor = this.cloudColorBlueish;
                }

                stormChance = 10.0F;
                tropicalChance = 0.0F;
                if (meanTemperature < 5.5F) {
                    blizzardChance = ClimateManager.clamp(0.0F, 85.0F, (5.5F - meanTemperature) * 3.0F);
                    blizzardChance += 25.0F;
                    if (meanTemperature < 2.5F) {
                        blizzardChance += 55.0F;
                    } else if (meanTemperature < 0.0F) {
                        blizzardChance += 75.0F;
                    }

                    if (blizzardChance > 95.0F) {
                        blizzardChance = 95.0F;
                    }
                } else {
                    blizzardChance = 0.0F;
                }

                if (winterIsComing) {
                    if (this.frontCache.getStrength() > 0.75F) {
                        blizzardChance = 100.0F;
                    } else {
                        blizzardChance = 75.0F;
                    }

                    if (this.frontCache.getStrength() > 0.5F) {
                        rainTimeMultiplier = 1.45F;
                    }
                }
        }

        rainTimeMultiplier *= this.climateManager.getRainTimeMultiplierMod(SandboxOptions.instance.getRainModifier());
        this.print(
            "WeatherPeriod: cloudColor r="
                + this.cloudColor.getExterior().r
                + ", g="
                + this.cloudColor.getExterior().g
                + ", b="
                + this.cloudColor.getExterior().b
        );
        this.print(
            "WeatherPeriod: chances, storm="
                + stormChance
                + ", tropical="
                + tropicalChance
                + ", blizzard="
                + blizzardChance
                + ". rainTimeMulti="
                + rainTimeMultiplier
        );
        ArrayList<WeatherPeriod.WeatherStage> stages = new ArrayList<>();
        WeatherPeriod.WeatherStage ws = null;
        if (this.frontCache.getType() == 1) {
            this.print("WeatherPeriod: Warm to cold front selected.");
            boolean isBlizzard = false;
            boolean isTropical = false;
            boolean isStorm = false;
            if (this.frontCache.getStrength() > 0.75F) {
                if (tropicalChance > 0.0F && this.RandNext(0.0F, 100.0F) < tropicalChance) {
                    this.print("WeatherPeriod: tropical storm triggered.");
                    isTropical = true;
                } else if (blizzardChance > 0.0F && this.RandNext(0.0F, 100.0F) < blizzardChance) {
                    this.print("WeatherPeriod: blizzard triggered.");
                    isBlizzard = true;
                }
            }

            if (!isBlizzard && !isTropical && this.frontCache.getStrength() > 0.5F && stormChance > 0.0F && this.RandNext(0.0F, 100.0F) < stormChance) {
                this.print("WeatherPeriod: storm triggered.");
                isStorm = true;
            }

            float maxDuration = this.RandNext(24.0F, 48.0F) * this.frontCache.getStrength();
            float dur = 0.0F;
            if (isTropical) {
                stages.add(this.createStage(8, 8.0F + this.RandNext(0.0F, 16.0F * this.frontCache.getStrength())));
                this.cloudColor = this.cloudColorTropical;
                if (this.RandNext(0.0F, 100.0F) < 60.0F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 5.0F + this.RandNext(0.0F, 5.0F * this.frontCache.getStrength())));
                }

                if (this.RandNext(0.0F, 100.0F) < 30.0F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 5.0F + this.RandNext(0.0F, 5.0F * this.frontCache.getStrength())));
                }
            } else if (isBlizzard) {
                stages.add(this.createStage(7, 24.0F + this.RandNext(0.0F, 24.0F * this.frontCache.getStrength())));
                this.cloudColor = this.cloudColorBlizzard;
            } else if (isStorm) {
                stages.add(this.createStage(3, 5.0F + this.RandNext(0.0F, 5.0F * this.frontCache.getStrength())));
                if (this.RandNext(0.0F, 100.0F) < 70.0F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 4.0F + this.RandNext(0.0F, 4.0F * this.frontCache.getStrength())));
                }

                if (this.RandNext(0.0F, 100.0F) < 50.0F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 4.0F + this.RandNext(0.0F, 4.0F * this.frontCache.getStrength())));
                }

                if (this.RandNext(0.0F, 100.0F) < 25.0F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 4.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength())));
                }

                if (this.RandNext(0.0F, 100.0F) < 12.5F * this.frontCache.getStrength()) {
                    stages.add(this.createStage(3, 4.0F + this.RandNext(0.0F, 2.0F * this.frontCache.getStrength())));
                }
            }

            for (int i = 0; i < stages.size(); i++) {
                dur = (float)(dur + stages.get(i).getStageDuration());
            }

            while (dur < maxDuration) {
                ws = switch (this.RandNext(0, 10)) {
                    case 0 -> this.createStage(5, 1.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength()));
                    case 1, 2, 3 -> this.createStage(1, 2.0F + this.RandNext(0.0F, 4.0F * this.frontCache.getStrength()));
                    default -> this.createStage(2, 2.0F + this.RandNext(0.0F, 4.0F * this.frontCache.getStrength()));
                };
                dur = (float)(dur + ws.getStageDuration());
                stages.add(ws);
            }
        } else {
            this.print("WeatherPeriod: Cold to warm front selected.");
            if (this.cloudColor == this.cloudColorReddish) {
                rnd = this.RandNext(0.0F, 100.0F);
                if (rnd < 50.0F) {
                    this.cloudColor = this.cloudColorBlueish;
                } else {
                    this.cloudColor = this.cloudColorPurplish;
                }
            }

            float maxDuration = this.RandNext(12.0F, 24.0F) * this.frontCache.getStrength();
            float dur = 0.0F;

            while (dur < maxDuration) {
                ws = switch (this.RandNext(0, 10)) {
                    case 0 -> this.createStage(1, 2.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength()));
                    case 1, 2, 3, 4 -> this.createStage(6, 2.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength()));
                    default -> this.createStage(5, 2.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength()));
                };
                dur = (float)(dur + ws.getStageDuration());
                stages.add(ws);
            }
        }

        Collections.shuffle(stages, this.seededRandom);
        float chance = this.RandNext(30.0F, 60.0F);
        this.weatherStages.add(this.createStage(0, 1.0F + this.RandNext(0.0F, 2.0F * this.frontCache.getStrength())));

        for (int i = 0; i < stages.size(); i++) {
            this.weatherStages.add(stages.get(i));
            if (i < stages.size() - 1 && this.RandNext(0.0F, 100.0F) < chance) {
                this.weatherStages.add(this.createStage(4, 1.0F + this.RandNext(0.0F, 2.0F * this.frontCache.getStrength())));
                this.weatherStages.add(this.createStage(9, 1.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength())));
                chance = this.RandNext(30.0F, 60.0F);
            }
        }

        if (this.weatherStages.get(this.weatherStages.size() - 1).getStageID() != 9) {
            this.weatherStages.add(this.createStage(4, 2.0F + this.RandNext(0.0F, 3.0F * this.frontCache.getStrength())));
        }

        for (int ix = 0; ix < this.weatherStages.size(); ix++) {
            this.weatherStages.get(ix).stageDuration *= rainTimeMultiplier;
        }

        this.linkWeatherStages();
        this.duration = 0.0;

        for (int ix = 0; ix < this.weatherStages.size(); ix++) {
            this.duration = this.duration + this.weatherStages.get(ix).stageDuration;
        }

        this.print("WeatherPeriod: Duration = " + this.duration + ".");
        double timeWorldAgeHours = this.startTime;

        for (int ix = 0; ix < this.weatherStages.size(); ix++) {
            timeWorldAgeHours = this.weatherStages.get(ix).setStageStart(timeWorldAgeHours);
        }

        this.weatherStageIndex = 0;
        this.currentStage = this.weatherStages.get(this.weatherStageIndex).startStage(this.startTime);
        this.print("WeatherPeriod: PATTERN GENERATION FINISHED.");
    }

    public WeatherPeriod.WeatherStage createAndAddModdedStage(String moddedID, double duration) {
        return this.createAndAddStage(10, duration, moddedID);
    }

    public WeatherPeriod.WeatherStage createAndAddStage(int typeid, double duration) {
        return this.createAndAddStage(typeid, duration, null);
    }

    private WeatherPeriod.WeatherStage createAndAddStage(int typeid, double duration, String moddedID) {
        if (!this.isRunning && this.hasStartedInit && (typeid != 10 || moddedID != null)) {
            WeatherPeriod.WeatherStage ws = this.createStage(typeid, duration, moddedID);
            this.weatherStages.add(ws);
            return ws;
        } else {
            return null;
        }
    }

    private WeatherPeriod.WeatherStage createStage(int typeID, double duration) {
        return this.createStage(typeID, duration, null);
    }

    private WeatherPeriod.WeatherStage createStage(int typeID, double duration, String moddedID) {
        WeatherPeriod.WeatherStage stage = null;
        if (!this.stagesPool.isEmpty()) {
            stage = this.stagesPool.pop();
        } else {
            stage = new WeatherPeriod.WeatherStage();
        }

        stage.stageId = typeID;
        stage.modId = moddedID;
        stage.setStageDuration(duration);
        switch (typeID) {
            case 0:
                this.print("WeatherPeriod: Adding stage 'START' with duration: " + duration + "%.");
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.NextTarget);
                break;
            case 1:
                this.print("WeatherPeriod: Adding stage 'SHOWERS' with duration: " + duration + "%.");
                stage.targetStrength = this.frontCache.getStrength() * 0.5F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.NextTarget);
                break;
            case 2:
                this.print("WeatherPeriod: Adding stage 'HEAVY_PRECIP' with duration: " + duration + "%.");
                stage.targetStrength = this.frontCache.getStrength();
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.Target);
                break;
            case 3:
            case 11:
                this.print("WeatherPeriod: Adding stage 'STORM' with duration: " + duration + "%.");
                if (typeID == 11) {
                    this.print("WeatherPeriod: this storm is a kate and bob storm...");
                }

                stage.targetStrength = this.frontCache.getStrength();
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.Target);
                if (this.RandNext(0, 100) < 33) {
                    stage.fogStrength = 0.1F + this.RandNext(0.0F, 0.4F);
                }
                break;
            case 4:
                this.print("WeatherPeriod: Adding stage 'CLEARING' with duration: " + duration + "%.");
                stage.targetStrength = this.frontCache.getStrength() * 0.25F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.None);
                break;
            case 5:
                this.print("WeatherPeriod: Adding stage 'MODERATE' with duration: " + duration + "%.");
                stage.targetStrength = this.frontCache.getStrength() * 0.5F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.NextTarget);
                break;
            case 6:
                this.print("WeatherPeriod: Adding stage 'DRIZZLE' with duration: " + duration + "%.");
                stage.targetStrength = this.frontCache.getStrength() * 0.25F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.NextTarget);
                break;
            case 7:
                this.print("WeatherPeriod: Adding stage 'BLIZZARD' with duration: " + duration + "%.");
                stage.targetStrength = 1.0F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.Target);
                stage.fogStrength = 0.55F + this.RandNext(0.0F, 0.2F);
                break;
            case 8:
                this.print("WeatherPeriod: Adding stage 'TROPICAL_STORM' with duration: " + duration + "%.");
                stage.targetStrength = 1.0F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.Target);
                stage.fogStrength = 0.6F + this.RandNext(0.0F, 0.4F);
                break;
            case 9:
                this.print("WeatherPeriod: Adding stage 'INTERMEZZO' with duration: " + duration + "%.");
                stage.targetStrength = 0.0F;
                stage.lerpEntryTo(WeatherPeriod.StrLerpVal.Target, WeatherPeriod.StrLerpVal.NextTarget);
                break;
            case 10:
                this.print("WeatherPeriod: Adding stage 'MODDED' with duration: " + duration + "%.");
                LuaEventManager.triggerEvent("OnInitModdedWeatherStage", this, stage, this.frontCache.getStrength());
                break;
            default:
                this.print("WeatherPeriod Warning: trying to _INIT_ state that is not recognized, state id=" + typeID);
        }

        return stage;
    }

    private void updateCurrentStage() {
        if (!this.isDummy) {
            this.isBlizzard = false;
            this.isThunderStorm = false;
            this.isTropicalStorm = false;
            switch (this.currentStage.stageId) {
                case 0:
                    this.rainThreshold = 0.35F - this.frontCache.getStrength() * 0.2F;
                    this.climateManager.fogIntensity.setOverride(0.0F, this.currentStage.linearT);
                    break;
                case 1: {
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                    float lerpmod = ClimateManager.clamp01(this.currentStage.parabolicT * 3.0F);
                    this.climateManager.windIntensity.setOverride(0.1F * this.weatherNoise, lerpmod);
                    this.climateManager.windAngleIntensity.setOverride(0.0F, lerpmod);
                    break;
                }
                case 2: {
                    float pushupx = this.frontCache.getStrength() * 0.5F;
                    if (this.currentStage.linearT < 0.1F) {
                        pushupx = ClimateManager.clerp(
                            (float)((this.currentTime - this.currentStage.stageStart) / (this.currentStage.stageDuration * 0.1)),
                            0.0F,
                            this.frontCache.getStrength() * 0.5F
                        );
                    } else if (this.currentStage.linearT > 0.9F) {
                        pushupx = ClimateManager.clerp(
                            1.0F - (float)((this.currentStage.stageEnd - this.currentTime) / (this.currentStage.stageDuration * 0.1)),
                            this.frontCache.getStrength() * 0.5F,
                            0.0F
                        );
                    }

                    this.weatherNoise = pushupx + this.weatherNoise * (1.0F - pushupx);
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                    float lerpmod = ClimateManager.clamp01(this.currentStage.parabolicT * 3.0F);
                    this.climateManager.windIntensity.setOverride(0.5F * this.weatherNoise, lerpmod);
                    this.climateManager.windAngleIntensity.setOverride(0.7F * this.weatherNoise * this.windAngleDirMod, lerpmod);
                    break;
                }
                case 4:
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F - this.currentStage.linearT);
                    break;
                case 5:
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                    break;
                case 6:
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                    break;
                case 7: {
                    this.isBlizzard = true;
                    float pushup = this.frontCache.getStrength() * 0.5F;
                    if (this.currentStage.linearT < 0.1F) {
                        pushup = ClimateManager.clerp(
                            (float)((this.currentTime - this.currentStage.stageStart) / (this.currentStage.stageDuration * 0.1)),
                            0.0F,
                            this.frontCache.getStrength() * 0.5F
                        );
                    } else if (this.currentStage.linearT > 0.9F) {
                        pushup = ClimateManager.clerp(
                            1.0F - (float)((this.currentStage.stageEnd - this.currentTime) / (this.currentStage.stageDuration * 0.1)),
                            this.frontCache.getStrength() * 0.5F,
                            0.0F
                        );
                    }

                    this.weatherNoise = pushup + this.weatherNoise * (1.0F - pushup);
                    float lerpmod = ClimateManager.clamp01(this.currentStage.parabolicT * 3.0F);
                    this.climateManager.windIntensity.setOverride(0.75F + 0.25F * this.weatherNoise, lerpmod);
                    this.climateManager.windAngleIntensity.setOverride(0.7F * this.weatherNoise * this.windAngleDirMod, lerpmod);
                    if (PerformanceSettings.fogQuality != 2) {
                        if (this.currentStage.fogStrength > 0.0F) {
                            this.climateManager.fogIntensity.setOverride(this.currentStage.fogStrength, lerpmod);
                        } else {
                            this.climateManager.fogIntensity.setOverride(1.0F, lerpmod);
                        }
                    }
                    break;
                }
                case 8:
                    this.isTropicalStorm = true;
                case 3:
                case 11: {
                    this.isThunderStorm = !this.isTropicalStorm;
                    if (!this.currentStage.hasStartedCloud) {
                        float angl = this.frontCache.getAngleDegrees();
                        float strength = this.frontCache.getStrength();
                        float radius = 8000.0F * strength;
                        float eventFreq = strength;
                        float thunderRatio = 0.6F * strength;
                        double duration = this.currentStage.stageDuration;
                        boolean targetRandomPlayer = strength > 0.7;
                        int clouds = Rand.Next(1, 3);
                        if (this.currentStage.stageId == 8) {
                            clouds = 1;
                            radius = 15000.0F;
                            thunderRatio = 0.8F;
                            targetRandomPlayer = true;
                            strength = 1.0F;
                        }

                        for (int i = 0; i < clouds; i++) {
                            ThunderStorm.ThunderCloud cloud = this.thunderStorm
                                .startThunderCloud(
                                    strength,
                                    angl,
                                    radius,
                                    eventFreq,
                                    thunderRatio,
                                    duration,
                                    targetRandomPlayer,
                                    this.currentStage.stageId == 11 ? kateBobStormProgress : 0.0F
                                );
                            if (this.currentStage.stageId == 11 && targetRandomPlayer && cloud != null) {
                                cloud.setCenter(this.kateBobStormX, this.kateBobStormY, angl);
                            }

                            targetRandomPlayer = false;
                        }

                        this.currentStage.hasStartedCloud = true;
                    }

                    float pushupx = this.frontCache.getStrength() * 0.5F;
                    if (this.currentStage.linearT < 0.1F) {
                        pushupx = ClimateManager.clerp(
                            (float)((this.currentTime - this.currentStage.stageStart) / (this.currentStage.stageDuration * 0.1)),
                            0.0F,
                            this.frontCache.getStrength() * 0.5F
                        );
                    } else if (this.currentStage.linearT > 0.9F) {
                        pushupx = ClimateManager.clerp(
                            1.0F - (float)((this.currentStage.stageEnd - this.currentTime) / (this.currentStage.stageDuration * 0.1)),
                            this.frontCache.getStrength() * 0.5F,
                            0.0F
                        );
                    }

                    this.weatherNoise = pushupx + this.weatherNoise * (1.0F - pushupx);
                    float lerpmod = ClimateManager.clamp01(this.currentStage.parabolicT * 3.0F);
                    if (this.currentStage.stageId == 8) {
                        this.climateManager.windIntensity.setOverride(0.4F + 0.6F * this.weatherNoise, lerpmod);
                    } else {
                        this.climateManager.windIntensity.setOverride(0.2F + 0.5F * this.weatherNoise, lerpmod);
                    }

                    this.climateManager.windAngleIntensity.setOverride(0.7F * this.weatherNoise * this.windAngleDirMod, lerpmod);
                    if (PerformanceSettings.fogQuality != 2) {
                        if (this.currentStage.fogStrength > 0.0F) {
                            this.climateManager.fogIntensity.setOverride(this.currentStage.fogStrength, lerpmod);
                            if (this.currentStage.stageId == 8) {
                                this.climateManager.colorNewFog.setOverride(this.climateManager.getFogTintTropical(), lerpmod);
                            } else {
                                this.climateManager.colorNewFog.setOverride(this.climateManager.getFogTintStorm(), lerpmod);
                            }
                        } else {
                            this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                        }
                    }
                    break;
                }
                case 9:
                    this.climateManager.fogIntensity.setOverride(0.0F, 1.0F);
                    break;
                case 10:
                    LuaEventManager.triggerEvent("OnUpdateModdedWeatherStage", this, this.currentStage, this.frontCache.getStrength());
                    break;
                default:
                    this.print("WeatherPeriod Warning: trying to _UPDATE_ state that is not recognized, state id=" + this.currentStage.stageId);
                    this.resetClimateManagerOverrides();
                    this.isRunning = false;
                    if (GameServer.server) {
                        this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)1, null);
                    }
            }
        }
    }

    public void update(double hoursSinceStart) {
        if (!GameClient.client && !this.isDummy) {
            if (this.isRunning && this.currentStage != null && this.weatherStageIndex >= 0 && !this.weatherStages.isEmpty()) {
                if (this.currentTime > this.currentStage.stageEnd) {
                    this.weatherStageIndex++;
                    LuaEventManager.triggerEvent("OnWeatherPeriodStage", this);
                    if (this.weatherStageIndex >= this.weatherStages.size()) {
                        this.isRunning = false;
                        this.currentStage = null;
                        this.resetClimateManagerOverrides();
                        if (GameServer.server) {
                            this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)1, null);
                        }

                        return;
                    }

                    if (this.currentStage != null) {
                        this.currentStage.exitStrength = this.currentStrength;
                    }

                    this.currentStage = this.weatherStages.get(this.weatherStageIndex);
                    this.currentStage.entryStrength = this.currentStrength;
                    this.currentStage.startStage(hoursSinceStart);
                }

                this.currentTime = hoursSinceStart;
                this.weatherNoise = 0.3F * this.frontCache.getStrength()
                    + (float)SimplexNoise.noise(hoursSinceStart, 24000.0) * (1.0F - 0.3F * this.frontCache.getStrength());
                this.weatherNoise = (this.weatherNoise + 1.0F) * 0.5F;
                this.currentStage.updateT(this.currentTime);
                this.stageProgress = this.currentStage.linearT;
                this.totalProgress = (float)(this.currentTime - this.weatherStages.get(0).stageStart) / (float)this.duration;
                this.totalProgress = ClimateManager.clamp01(this.totalProgress);
                this.currentStrength = this.currentStage.getStageCurrentStrength();
                this.updateCurrentStage();
                float temperatureMod = ClimateManager.clamp(-1.0F, 1.0F, this.currentStrength * 2.0F) * 7.0F;
                if (this.frontCache.getType() == 1) {
                    this.temperatureInfluence = this.climateManager.temperature.internalValue - temperatureMod;
                } else {
                    this.temperatureInfluence = this.climateManager.temperature.internalValue + temperatureMod;
                }

                if (this.isRunning) {
                    if (this.weatherNoise > this.rainThreshold) {
                        this.precipitationFinal = (this.weatherNoise - this.rainThreshold) / (1.0F - this.rainThreshold);
                        this.precipitationFinal = this.precipitationFinal * this.currentStrength;
                    } else {
                        this.precipitationFinal = 0.0F;
                    }

                    float currentDarkness = this.precipitationFinal;
                    float strNightFix = currentDarkness * (1.0F - this.climateManager.nightStrength.internalValue);
                    float valCloud = 0.5F;
                    valCloud += 0.5F * (1.0F - this.climateManager.nightStrength.internalValue);
                    valCloud = Math.max(valCloud, this.climateManager.cloudIntensity.internalValue);
                    float baseAmbMod = 0.55F;
                    if (PerformanceSettings.fogQuality != 2 && this.currentStage.stageId == 8) {
                        baseAmbMod += 0.35F * this.currentStage.parabolicT;
                    }

                    float ambience = 1.0F - baseAmbMod * currentDarkness;
                    ambience = Math.min(ambience, 1.0F - this.climateManager.nightStrength.internalValue);
                    if (PerformanceSettings.fogQuality != 2 && this.currentStage.stageId == 7) {
                        float cloudmod = 1.0F - 0.75F * this.currentStage.parabolicT;
                        valCloud *= cloudmod;
                    }

                    this.climateManager.cloudIntensity.setOverride(valCloud, this.currentStrength);
                    this.climateManager.precipitationIntensity.setOverride(this.precipitationFinal, 1.0F);
                    this.climateManager.globalLight.setOverride(this.cloudColor, strNightFix);
                    this.climateManager.globalLightIntensity.setOverride(0.4F, strNightFix);
                    this.climateManager.desaturation.setOverride(0.3F, this.currentStrength);
                    this.climateManager.temperature.setOverride(this.temperatureInfluence, this.currentStrength);
                    this.climateManager.ambient.setOverride(ambience, currentDarkness);
                    this.climateManager.dayLightStrength.setOverride(ambience, currentDarkness);
                    if ((!(this.climateManager.getTemperature() < 0.0F) || !this.climateManager.getSeason().isSeason(5)) && !ClimateManager.winterIsComing) {
                        this.climateManager.precipitationIsSnow.setEnableOverride(false);
                    } else {
                        this.climateManager.precipitationIsSnow.setOverride(true);
                    }
                }
            } else {
                if (this.isRunning) {
                    this.resetClimateManagerOverrides();
                    this.isRunning = false;
                    LuaEventManager.triggerEvent("OnWeatherPeriodComplete", this);
                    if (GameServer.server) {
                        this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)1, null);
                    }
                }
            }
        }
    }

    private void resetClimateManagerOverrides() {
        if (this.climateManager != null && !this.isDummy) {
            this.climateManager.resetOverrides();
        }
    }

    /**
     * IO
     */
    public void save(DataOutputStream output) throws IOException {
        if (GameClient.client && !GameServer.server) {
            output.writeByte(0);
        } else {
            output.writeByte(1);
            output.writeBoolean(this.isRunning);
            if (this.isRunning) {
                output.writeInt(this.weatherStageIndex);
                output.writeFloat(this.currentStrength);
                output.writeFloat(this.rainThreshold);
                output.writeBoolean(this.isThunderStorm);
                output.writeBoolean(this.isTropicalStorm);
                output.writeBoolean(this.isBlizzard);
                this.frontCache.save(output);
                output.writeInt(this.weatherStages.size());

                for (int i = 0; i < this.weatherStages.size(); i++) {
                    WeatherPeriod.WeatherStage ws = this.weatherStages.get(i);
                    output.writeInt(ws.stageId);
                    output.writeDouble(ws.stageDuration);
                    ws.save(output);
                }

                this.cloudColor.save(output);
            }
        }
    }

    public void load(DataInputStream input, int worldVersion) throws IOException {
        byte hasstuff = input.readByte();
        if (hasstuff == 1) {
            this.isRunning = input.readBoolean();
            if (this.isRunning) {
                this.weatherStageIndex = input.readInt();
                this.currentStrength = input.readFloat();
                this.rainThreshold = input.readFloat();
                this.isThunderStorm = input.readBoolean();
                this.isTropicalStorm = input.readBoolean();
                this.isBlizzard = input.readBoolean();
                this.frontCache.load(input);
                if (this.frontCache.getAngleDegrees() >= 90.0F && this.frontCache.getAngleDegrees() < 270.0F) {
                    this.windAngleDirMod = 1.0F;
                } else {
                    this.windAngleDirMod = -1.0F;
                }

                this.print("WeatherPeriod: Loading weather pattern with strength = " + this.frontCache.getStrength());
                this.clearCurrentWeatherStages();
                int stagesCount = input.readInt();

                for (int i = 0; i < stagesCount; i++) {
                    int id = input.readInt();
                    double duration = input.readDouble();
                    WeatherPeriod.WeatherStage ws = !this.stagesPool.isEmpty() ? this.stagesPool.pop() : new WeatherPeriod.WeatherStage();
                    ws.stageId = id;
                    ws.setStageDuration(duration);
                    ws.load(input, worldVersion);
                    this.weatherStages.add(ws);
                }

                this.cloudColor.load(input, worldVersion);
                this.linkWeatherStages();
                this.duration = 0.0;

                for (int i = 0; i < this.weatherStages.size(); i++) {
                    this.duration = this.duration + this.weatherStages.get(i).stageDuration;
                }

                if (this.weatherStageIndex >= 0 && this.weatherStageIndex < this.weatherStages.size()) {
                    this.currentStage = this.weatherStages.get(this.weatherStageIndex);
                    this.print("WeatherPeriod: Pattern loaded!");
                } else {
                    this.print("WeatherPeriod: Couldnt load stages correctly.");
                    this.isRunning = false;
                }
            }
        }
    }

    @UsedFromLua
    public static enum StrLerpVal {
        Entry(1),
        Target(2),
        NextTarget(3),
        None(0);

        private final int value;

        private StrLerpVal(final int value) {
            this.value = value;
            if (WeatherPeriod.cache.containsKey(value)) {
                DebugLog.log("StrLerpVal WARNING: trying to add id twice. id=" + value);
            }

            WeatherPeriod.cache.put(value, this);
        }

        public int getValue() {
            return this.value;
        }

        public static WeatherPeriod.StrLerpVal fromValue(int id) {
            if (WeatherPeriod.cache.containsKey(id)) {
                return WeatherPeriod.cache.get(id);
            } else {
                DebugLog.log("StrLerpVal, trying to get from invalid id: " + id);
                return None;
            }
        }
    }

    @UsedFromLua
    public static class WeatherStage {
        protected WeatherPeriod.WeatherStage previousStage;
        protected WeatherPeriod.WeatherStage nextStage;
        private double stageStart;
        private double stageEnd;
        private double stageDuration;
        protected int stageId;
        protected float entryStrength;
        protected float exitStrength;
        protected float targetStrength;
        protected WeatherPeriod.StrLerpVal lerpMidVal;
        protected WeatherPeriod.StrLerpVal lerpEndVal;
        protected boolean hasStartedCloud;
        protected float fogStrength;
        protected float linearT;
        protected float parabolicT;
        protected boolean isCycleFirstHalf = true;
        protected boolean creationFinished;
        protected String modId;
        private float m;
        private float e;

        public WeatherStage() {
        }

        public WeatherStage(int id) {
            this.stageId = id;
        }

        public void setStageID(int id) {
            this.stageId = id;
        }

        public double getStageStart() {
            return this.stageStart;
        }

        public double getStageEnd() {
            return this.stageEnd;
        }

        public double getStageDuration() {
            return this.stageDuration;
        }

        public int getStageID() {
            return this.stageId;
        }

        public String getModID() {
            return this.modId;
        }

        public float getLinearT() {
            return this.linearT;
        }

        public float getParabolicT() {
            return this.parabolicT;
        }

        public void setTargetStrength(float t) {
            this.targetStrength = t;
        }

        public boolean getHasStartedCloud() {
            return this.hasStartedCloud;
        }

        public void setHasStartedCloud(boolean b) {
            this.hasStartedCloud = true;
        }

        public void save(DataOutputStream output) throws IOException {
            output.writeDouble(this.stageStart);
            output.writeFloat(this.entryStrength);
            output.writeFloat(this.exitStrength);
            output.writeFloat(this.targetStrength);
            output.writeInt(this.lerpMidVal.getValue());
            output.writeInt(this.lerpEndVal.getValue());
            output.writeBoolean(this.hasStartedCloud);
            output.writeByte(this.modId != null ? 1 : 0);
            if (this.modId != null) {
                GameWindow.WriteString(output, this.modId);
            }

            output.writeFloat(this.fogStrength);
        }

        public void load(DataInputStream input, int worldVersion) throws IOException {
            this.stageStart = input.readDouble();
            this.stageEnd = this.stageStart + this.stageDuration;
            this.entryStrength = input.readFloat();
            this.exitStrength = input.readFloat();
            this.targetStrength = input.readFloat();
            this.lerpMidVal = WeatherPeriod.StrLerpVal.fromValue(input.readInt());
            this.lerpEndVal = WeatherPeriod.StrLerpVal.fromValue(input.readInt());
            this.hasStartedCloud = input.readBoolean();
            if (input.readByte() == 1) {
                this.modId = GameWindow.ReadString(input);
            }

            this.fogStrength = input.readFloat();
        }

        protected void reset() {
            this.previousStage = null;
            this.nextStage = null;
            this.isCycleFirstHalf = true;
            this.hasStartedCloud = false;
            this.lerpMidVal = WeatherPeriod.StrLerpVal.None;
            this.lerpEndVal = WeatherPeriod.StrLerpVal.None;
            this.entryStrength = 0.0F;
            this.exitStrength = 0.0F;
            this.modId = null;
            this.creationFinished = false;
            this.fogStrength = 0.0F;
        }

        protected WeatherPeriod.WeatherStage startStage(double exitTime) {
            this.stageStart = exitTime;
            this.stageEnd = exitTime + this.stageDuration;
            this.hasStartedCloud = false;
            return this;
        }

        protected double setStageStart(double worldAgeHours) {
            this.stageStart = worldAgeHours;
            this.stageEnd = worldAgeHours + this.stageDuration;
            return this.stageEnd;
        }

        protected WeatherPeriod.WeatherStage setStageDuration(double time) {
            this.stageDuration = time;
            if (this.stageDuration < 1.0) {
                this.stageDuration = 1.0;
            }

            return this;
        }

        protected WeatherPeriod.WeatherStage overrideStageDuration(double time) {
            this.stageDuration = time;
            return this;
        }

        public void lerpEntryTo(int mid, int end) {
            if (!this.creationFinished) {
                this.lerpEntryTo(WeatherPeriod.StrLerpVal.fromValue(mid), WeatherPeriod.StrLerpVal.fromValue(end));
            }
        }

        protected void lerpEntryTo(WeatherPeriod.StrLerpVal end) {
            this.lerpEntryTo(WeatherPeriod.StrLerpVal.None, end);
        }

        protected void lerpEntryTo(WeatherPeriod.StrLerpVal mid, WeatherPeriod.StrLerpVal end) {
            if (!this.creationFinished) {
                this.lerpMidVal = mid;
                this.lerpEndVal = end;
            }
        }

        public float getStageCurrentStrength() {
            this.m = this.getLerpValue(this.lerpMidVal);
            this.e = this.getLerpValue(this.lerpEndVal);
            if (this.lerpMidVal == WeatherPeriod.StrLerpVal.None) {
                return ClimateManager.clerp(this.linearT, this.entryStrength, this.e);
            } else {
                return this.isCycleFirstHalf
                    ? ClimateManager.clerp(this.parabolicT, this.entryStrength, this.m)
                    : ClimateManager.clerp(this.parabolicT, this.e, this.m);
            }
        }

        private float getLerpValue(WeatherPeriod.StrLerpVal lerpVal) {
            switch (lerpVal) {
                case Entry:
                    return this.entryStrength;
                case Target:
                    return this.targetStrength;
                case NextTarget:
                    return this.nextStage != null ? this.nextStage.targetStrength : 0.0F;
                case None:
                    return 0.0F;
                default:
                    return 0.0F;
            }
        }

        private WeatherPeriod.WeatherStage updateT(double hour) {
            this.linearT = this.getPeriodLerpT(hour);
            if (this.stageId == 11) {
                this.linearT = WeatherPeriod.kateBobStormProgress + (1.0F - WeatherPeriod.kateBobStormProgress) * this.linearT;
            }

            if (this.linearT < 0.5F) {
                this.parabolicT = this.linearT * 2.0F;
                this.isCycleFirstHalf = true;
            } else {
                this.parabolicT = 2.0F - this.linearT * 2.0F;
                this.isCycleFirstHalf = false;
            }

            return this;
        }

        private float getPeriodLerpT(double hour) {
            if (hour < this.stageStart) {
                return 0.0F;
            } else {
                return hour > this.stageEnd ? 1.0F : (float)((hour - this.stageStart) / this.stageDuration);
            }
        }
    }
}
