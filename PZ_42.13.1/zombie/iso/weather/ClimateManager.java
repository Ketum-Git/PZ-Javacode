// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.GregorianCalendar;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionMain;
import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.dbg.ClimMngrDebug;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.SteppedUpdateFloat;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.vehicles.BaseVehicle;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class ClimateManager {
    private boolean disableSimulation;
    private boolean disableFxUpdate;
    private boolean disableWeatherGeneration;
    public static final int FRONT_COLD = -1;
    public static final int FRONT_STATIONARY = 0;
    public static final int FRONT_WARM = 1;
    public static final float MAX_WINDSPEED_KPH = 120.0F;
    public static final float MAX_WINDSPEED_MPH = 74.5645F;
    private ErosionSeason season;
    private long lastMinuteStamp = -1L;
    private KahluaTable modDataTable;
    private float airMass;
    private float airMassDaily;
    private float airMassTemperature;
    private float baseTemperature;
    private float snowFall;
    private float snowStrength;
    private float snowMeltStrength;
    private float snowFracNow;
    boolean canDoWinterSprites;
    boolean wasForceSnow;
    private float windPower;
    private final WeatherPeriod weatherPeriod;
    private final ThunderStorm thunderStorm;
    private double simplexOffsetA;
    private double simplexOffsetB;
    private double simplexOffsetC;
    private double simplexOffsetD;
    private boolean dayDoFog;
    private float dayFogStrength;
    private GameTime gt;
    private double worldAgeHours;
    private boolean tickIsClimateTick;
    private boolean tickIsDayChange;
    private int lastHourStamp = -1;
    private boolean tickIsHourChange;
    private boolean tickIsTenMins;
    private final ClimateManager.AirFront currentFront = new ClimateManager.AirFront();
    private ClimateColorInfo colDay;
    private ClimateColorInfo colDusk;
    private ClimateColorInfo colDawn;
    private ClimateColorInfo colNight;
    private final ClimateColorInfo colNightNoMoon;
    private ClimateColorInfo colNightMoon;
    private ClimateColorInfo colTemp;
    private ClimateColorInfo colFog;
    private final ClimateColorInfo colFogLegacy;
    private final ClimateColorInfo colFogNew;
    private final ClimateColorInfo fogTintStorm;
    private final ClimateColorInfo fogTintTropical;
    private static ClimateManager instance = new ClimateManager();
    public static boolean winterIsComing;
    public static boolean theDescendingFog;
    public static boolean aStormIsComing;
    private ClimateValues climateValues;
    private final ClimateForecaster climateForecaster;
    private final ClimateHistory climateHistory;
    float dayLightLagged;
    float nightLagged;
    protected ClimateManager.ClimateFloat desaturation;
    protected ClimateManager.ClimateFloat globalLightIntensity;
    protected ClimateManager.ClimateFloat nightStrength;
    protected ClimateManager.ClimateFloat precipitationIntensity;
    protected ClimateManager.ClimateFloat temperature;
    protected ClimateManager.ClimateFloat fogIntensity;
    protected ClimateManager.ClimateFloat windIntensity;
    protected ClimateManager.ClimateFloat windAngleIntensity;
    protected ClimateManager.ClimateFloat cloudIntensity;
    protected ClimateManager.ClimateFloat ambient;
    protected ClimateManager.ClimateFloat viewDistance;
    protected ClimateManager.ClimateFloat dayLightStrength;
    protected ClimateManager.ClimateFloat humidity;
    protected ClimateManager.ClimateColor globalLight;
    protected ClimateManager.ClimateColor colorNewFog;
    protected ClimateManager.ClimateBool precipitationIsSnow;
    public static final int FLOAT_DESATURATION = 0;
    public static final int FLOAT_GLOBAL_LIGHT_INTENSITY = 1;
    public static final int FLOAT_NIGHT_STRENGTH = 2;
    public static final int FLOAT_PRECIPITATION_INTENSITY = 3;
    public static final int FLOAT_TEMPERATURE = 4;
    public static final int FLOAT_FOG_INTENSITY = 5;
    public static final int FLOAT_WIND_INTENSITY = 6;
    public static final int FLOAT_WIND_ANGLE_INTENSITY = 7;
    public static final int FLOAT_CLOUD_INTENSITY = 8;
    public static final int FLOAT_AMBIENT = 9;
    public static final int FLOAT_VIEW_DISTANCE = 10;
    public static final int FLOAT_DAYLIGHT_STRENGTH = 11;
    public static final int FLOAT_HUMIDITY = 12;
    public static final int FLOAT_MAX = 13;
    private final ClimateManager.ClimateFloat[] climateFloats = new ClimateManager.ClimateFloat[13];
    public static final int COLOR_GLOBAL_LIGHT = 0;
    public static final int COLOR_NEW_FOG = 1;
    public static final int COLOR_MAX = 2;
    private final ClimateManager.ClimateColor[] climateColors = new ClimateManager.ClimateColor[2];
    public static final int BOOL_IS_SNOW = 0;
    public static final int BOOL_MAX = 1;
    private final ClimateManager.ClimateBool[] climateBooleans = new ClimateManager.ClimateBool[1];
    public static final float AVG_FAV_AIR_TEMPERATURE = 22.0F;
    private int weatherOverride;
    private int fogOverride;
    private static double windNoiseOffset;
    private static double windNoiseBase;
    private static double windNoiseFinal;
    private static double windTickFinal;
    private final ClimateColorInfo colFlare = new ClimateColorInfo(1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
    private boolean flareLaunched;
    private final SteppedUpdateFloat flareIntensity = new SteppedUpdateFloat(0.0F, 0.01F, 0.0F, 1.0F);
    private float flareIntens;
    private float flareMaxLifeTime;
    private float flareLifeTime;
    private int nextRandomTargetIntens = 10;
    float fogLerpValue;
    private ClimateManager.SeasonColor seasonColorDawn;
    private ClimateManager.SeasonColor seasonColorDay;
    private ClimateManager.SeasonColor seasonColorDusk;
    private ClimateManager.DayInfo previousDay;
    private ClimateManager.DayInfo currentDay;
    private ClimateManager.DayInfo nextDay;
    public static final byte PacketUpdateClimateVars = 0;
    public static final byte PacketWeatherUpdate = 1;
    public static final byte PacketThunderEvent = 2;
    public static final byte PacketFlare = 3;
    public static final byte PacketAdminVarsUpdate = 4;
    public static final byte PacketRequestAdminVars = 5;
    public static final byte PacketClientChangedAdminVars = 6;
    public static final byte PacketClientChangedWeather = 7;
    private float networkLerp;
    private long networkUpdateStamp;
    private float networkLerpTime = 5000.0F;
    private final float networkLerpTimeBase = 5000.0F;
    private float networkAdjustVal;
    private final boolean networkPrint = false;
    private final ClimateManager.ClimateNetInfo netInfo = new ClimateManager.ClimateNetInfo();
    private ClimateValues climateValuesFronts;
    private static final float[] windAngles = new float[]{22.5F, 67.5F, 112.5F, 157.5F, 202.5F, 247.5F, 292.5F, 337.5F, 382.5F};
    private static final String[] windAngleStr = new String[]{"SE", "S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public float getMaxWindspeedKph() {
        return 120.0F;
    }

    public float getMaxWindspeedMph() {
        return 74.5645F;
    }

    public static float ToKph(float val) {
        return val * 120.0F;
    }

    public static float ToMph(float val) {
        return val * 74.5645F;
    }

    public static ClimateManager getInstance() {
        return instance;
    }

    public static void setInstance(ClimateManager inst) {
        instance = inst;
    }

    public ClimateManager() {
        this.colDay = new ClimateColorInfo();
        this.colDawn = new ClimateColorInfo();
        this.colDusk = new ClimateColorInfo();
        this.colNight = new ClimateColorInfo();
        this.colNightMoon = new ClimateColorInfo();
        this.colFog = new ClimateColorInfo();
        this.colTemp = new ClimateColorInfo();
        this.colDay = new ClimateColorInfo();
        this.colDawn = new ClimateColorInfo();
        this.colDusk = new ClimateColorInfo();
        this.colNight = new ClimateColorInfo(0.33F, 0.33F, 0.33F, 0.4F, 0.33F, 0.33F, 0.33F, 0.4F);
        this.colNightNoMoon = new ClimateColorInfo(0.33F, 0.33F, 0.33F, 0.4F, 0.33F, 0.33F, 0.33F, 0.4F);
        this.colNightMoon = new ClimateColorInfo(0.33F, 0.33F, 0.33F, 0.4F, 0.33F, 0.33F, 0.33F, 0.4F);
        this.colFog = new ClimateColorInfo(0.4F, 0.4F, 0.4F, 0.8F, 0.4F, 0.4F, 0.4F, 0.8F);
        this.colFogLegacy = new ClimateColorInfo(0.3F, 0.3F, 0.3F, 0.8F, 0.3F, 0.3F, 0.3F, 0.8F);
        this.colFogNew = new ClimateColorInfo(0.5F, 0.5F, 0.55F, 0.4F, 0.5F, 0.5F, 0.55F, 0.8F);
        this.fogTintStorm = new ClimateColorInfo(0.5F, 0.45F, 0.4F, 1.0F, 0.5F, 0.45F, 0.4F, 1.0F);
        this.fogTintTropical = new ClimateColorInfo(0.8F, 0.75F, 0.55F, 1.0F, 0.8F, 0.75F, 0.55F, 1.0F);
        this.colTemp = new ClimateColorInfo();
        this.simplexOffsetA = Rand.Next(0, 8000);
        this.simplexOffsetB = Rand.Next(8000, 16000);
        this.simplexOffsetC = Rand.Next(0, -8000);
        this.simplexOffsetD = Rand.Next(-8000, -16000);
        this.initSeasonColors();
        this.setup();
        this.climateValues = new ClimateValues(this);
        this.thunderStorm = new ThunderStorm(this);
        this.weatherPeriod = new WeatherPeriod(this, this.thunderStorm);
        this.climateForecaster = new ClimateForecaster();
        this.climateHistory = new ClimateHistory();

        try {
            LuaEventManager.triggerEvent("OnClimateManagerInit", this);
        } catch (Exception var2) {
            System.out.print(var2.getMessage());
            System.out.print(var2.getStackTrace());
        }
    }

    public ClimateColorInfo getColNight() {
        return this.colNight;
    }

    public ClimateColorInfo getColNightNoMoon() {
        return this.colNightNoMoon;
    }

    public ClimateColorInfo getColNightMoon() {
        return this.colNightMoon;
    }

    public ClimateColorInfo getColFog() {
        return this.colFog;
    }

    public ClimateColorInfo getColFogLegacy() {
        return this.colFogLegacy;
    }

    public ClimateColorInfo getColFogNew() {
        return this.colFogNew;
    }

    public ClimateColorInfo getFogTintStorm() {
        return this.fogTintStorm;
    }

    public ClimateColorInfo getFogTintTropical() {
        return this.fogTintTropical;
    }

    private void setup() {
        for (int i = 0; i < this.climateFloats.length; i++) {
            this.climateFloats[i] = new ClimateManager.ClimateFloat();
        }

        for (int i = 0; i < this.climateColors.length; i++) {
            this.climateColors[i] = new ClimateManager.ClimateColor();
        }

        for (int i = 0; i < this.climateBooleans.length; i++) {
            this.climateBooleans[i] = new ClimateManager.ClimateBool();
        }

        this.desaturation = this.initClimateFloat(0, "DESATURATION");
        this.globalLightIntensity = this.initClimateFloat(1, "GLOBAL_LIGHT_INTENSITY");
        this.nightStrength = this.initClimateFloat(2, "NIGHT_STRENGTH");
        this.precipitationIntensity = this.initClimateFloat(3, "PRECIPITATION_INTENSITY");
        this.temperature = this.initClimateFloat(4, "TEMPERATURE");
        this.temperature.min = -80.0F;
        this.temperature.max = 80.0F;
        this.fogIntensity = this.initClimateFloat(5, "FOG_INTENSITY");
        this.windIntensity = this.initClimateFloat(6, "WIND_INTENSITY");
        this.windAngleIntensity = this.initClimateFloat(7, "WIND_ANGLE_INTENSITY");
        this.windAngleIntensity.min = -1.0F;
        this.cloudIntensity = this.initClimateFloat(8, "CLOUD_INTENSITY");
        this.ambient = this.initClimateFloat(9, "AMBIENT");
        this.viewDistance = this.initClimateFloat(10, "VIEW_DISTANCE");
        this.viewDistance.min = 0.0F;
        this.viewDistance.max = 100.0F;
        this.dayLightStrength = this.initClimateFloat(11, "DAYLIGHT_STRENGTH");
        this.humidity = this.initClimateFloat(12, "HUMIDITY");
        this.globalLight = this.initClimateColor(0, "GLOBAL_LIGHT");
        this.colorNewFog = this.initClimateColor(1, "COLOR_NEW_FOG");
        this.colorNewFog.internalValue.setExterior(0.9F, 0.9F, 0.95F, 1.0F);
        this.colorNewFog.internalValue.setInterior(0.9F, 0.9F, 0.95F, 1.0F);
        this.precipitationIsSnow = this.initClimateBool(0, "IS_SNOW");
    }

    public int getFloatMax() {
        return 13;
    }

    private ClimateManager.ClimateFloat initClimateFloat(int id, String name) {
        if (id >= 0 && id < 13) {
            return this.climateFloats[id].init(id, name);
        } else {
            DebugLog.log("Climate: cannot get float override id.");
            return null;
        }
    }

    public ClimateManager.ClimateFloat getClimateFloat(int id) {
        if (id >= 0 && id < 13) {
            return this.climateFloats[id];
        } else {
            DebugLog.log("Climate: cannot get float override id.");
            return null;
        }
    }

    public int getColorMax() {
        return 2;
    }

    private ClimateManager.ClimateColor initClimateColor(int id, String name) {
        if (id >= 0 && id < 2) {
            return this.climateColors[id].init(id, name);
        } else {
            DebugLog.log("Climate: cannot get float override id.");
            return null;
        }
    }

    public ClimateManager.ClimateColor getClimateColor(int id) {
        if (id >= 0 && id < 2) {
            return this.climateColors[id];
        } else {
            DebugLog.log("Climate: cannot get float override id.");
            return null;
        }
    }

    public int getBoolMax() {
        return 1;
    }

    private ClimateManager.ClimateBool initClimateBool(int id, String name) {
        if (id >= 0 && id < 1) {
            return this.climateBooleans[id].init(id, name);
        } else {
            DebugLog.log("Climate: cannot get boolean id.");
            return null;
        }
    }

    public ClimateManager.ClimateBool getClimateBool(int id) {
        if (id >= 0 && id < 1) {
            return this.climateBooleans[id];
        } else {
            DebugLog.log("Climate: cannot get boolean id.");
            return null;
        }
    }

    public void setEnabledSimulation(boolean b) {
        if (!GameClient.client && !GameServer.server) {
            this.disableSimulation = !b;
        } else {
            this.disableSimulation = false;
        }
    }

    public boolean getEnabledSimulation() {
        return !this.disableSimulation;
    }

    public boolean getEnabledFxUpdate() {
        return !this.disableFxUpdate;
    }

    public void setEnabledFxUpdate(boolean b) {
        if (!GameClient.client && !GameServer.server) {
            this.disableFxUpdate = !b;
        } else {
            this.disableFxUpdate = false;
        }
    }

    public boolean getEnabledWeatherGeneration() {
        return this.disableWeatherGeneration;
    }

    public void setEnabledWeatherGeneration(boolean b) {
        this.disableWeatherGeneration = !b;
    }

    public Color getGlobalLightInternal() {
        return this.globalLight.internalValue.getExterior();
    }

    public ClimateColorInfo getGlobalLight() {
        return this.globalLight.finalValue;
    }

    public float getGlobalLightIntensity() {
        return this.globalLightIntensity.finalValue;
    }

    public ClimateColorInfo getColorNewFog() {
        return this.colorNewFog.finalValue;
    }

    public void setNightStrength(float b) {
        this.nightStrength.finalValue = clamp(0.0F, 1.0F, b);
    }

    public float getDesaturation() {
        return this.desaturation.finalValue;
    }

    public void setDesaturation(float desaturation) {
        this.desaturation.finalValue = desaturation;
    }

    public float getAirMass() {
        return this.airMass;
    }

    public float getAirMassDaily() {
        return this.airMassDaily;
    }

    public float getAirMassTemperature() {
        return this.airMassTemperature;
    }

    public float getDayLightStrength() {
        return this.dayLightStrength.finalValue;
    }

    public float getNightStrength() {
        return this.nightStrength.finalValue;
    }

    public float getDayMeanTemperature() {
        return this.currentDay.season.getDayMeanTemperature();
    }

    public float getTemperature() {
        return this.temperature.finalValue;
    }

    public float getBaseTemperature() {
        return this.baseTemperature;
    }

    public float getSnowStrength() {
        return this.snowStrength;
    }

    public boolean getPrecipitationIsSnow() {
        return this.precipitationIsSnow.finalValue;
    }

    public float getPrecipitationIntensity() {
        return this.precipitationIntensity.finalValue;
    }

    public float getFogIntensity() {
        return this.fogIntensity.finalValue;
    }

    public float getWindIntensity() {
        return this.windIntensity.finalValue;
    }

    public float getWindAngleIntensity() {
        return this.windAngleIntensity.finalValue;
    }

    public float getCorrectedWindAngleIntensity() {
        return (this.windAngleIntensity.finalValue + 1.0F) * 0.5F;
    }

    public float getWindPower() {
        return this.windPower;
    }

    public float getWindspeedKph() {
        return this.windPower * 120.0F;
    }

    public float getCloudIntensity() {
        return this.cloudIntensity.finalValue;
    }

    public float getAmbient() {
        return this.ambient.finalValue;
    }

    public float getViewDistance() {
        return this.viewDistance.finalValue;
    }

    public float getHumidity() {
        return this.humidity.finalValue;
    }

    public float getWindAngleDegrees() {
        float windAngle;
        if (this.windAngleIntensity.finalValue > 0.0F) {
            windAngle = lerp(this.windAngleIntensity.finalValue, 45.0F, 225.0F);
        } else if (this.windAngleIntensity.finalValue > -0.25F) {
            windAngle = lerp(Math.abs(this.windAngleIntensity.finalValue), 45.0F, 0.0F);
        } else {
            windAngle = lerp(Math.abs(this.windAngleIntensity.finalValue) - 0.25F, 360.0F, 180.0F);
        }

        if (windAngle > 360.0F) {
            windAngle -= 360.0F;
        }

        if (windAngle < 0.0F) {
            windAngle += 360.0F;
        }

        return windAngle;
    }

    public float getWindAngleRadians() {
        return (float)Math.toRadians(this.getWindAngleDegrees());
    }

    public float getWindSpeedMovement() {
        float windspeed = this.getWindIntensity();
        if (windspeed < 0.15F) {
            windspeed = 0.0F;
        } else {
            windspeed = (windspeed - 0.15F) / 0.85F;
        }

        return windspeed;
    }

    public float getWindForceMovement(IsoGameCharacter character, float angle) {
        if (character.square != null && !character.square.isInARoom()) {
            float windforce = angle - this.getWindAngleRadians();
            if (windforce > Math.PI * 2) {
                windforce = (float)(windforce - (Math.PI * 2));
            }

            if (windforce < 0.0F) {
                windforce = (float)(windforce + (Math.PI * 2));
            }

            if (windforce > Math.PI) {
                windforce = (float)(Math.PI - (windforce - Math.PI));
            }

            return (float)(windforce / Math.PI);
        } else {
            return 0.0F;
        }
    }

    public boolean isRaining() {
        return this.getPrecipitationIntensity() > 0.0F && !this.getPrecipitationIsSnow();
    }

    public float getRainIntensity() {
        return this.isRaining() ? this.getPrecipitationIntensity() : 0.0F;
    }

    public boolean isSnowing() {
        return this.getPrecipitationIntensity() > 0.0F && this.getPrecipitationIsSnow();
    }

    public float getSnowIntensity() {
        return this.isSnowing() ? this.getPrecipitationIntensity() : 0.0F;
    }

    public void setAmbient(float f) {
        this.ambient.finalValue = f;
    }

    public void setViewDistance(float f) {
        this.viewDistance.finalValue = f;
    }

    public void setDayLightStrength(float f) {
        this.dayLightStrength.finalValue = f;
    }

    public void setPrecipitationIsSnow(boolean b) {
        this.precipitationIsSnow.finalValue = b;
    }

    public ClimateManager.DayInfo getCurrentDay() {
        return this.currentDay;
    }

    public ClimateManager.DayInfo getPreviousDay() {
        return this.previousDay;
    }

    public ClimateManager.DayInfo getNextDay() {
        return this.nextDay;
    }

    public ErosionSeason getSeason() {
        return this.currentDay != null && this.currentDay.getSeason() != null ? this.currentDay.getSeason() : this.season;
    }

    public float getFrontStrength() {
        if (this.currentFront == null) {
            return 0.0F;
        } else {
            if (Core.debug) {
                this.CalculateWeatherFrontStrength(this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne(), this.currentFront);
            }

            return this.currentFront.strength;
        }
    }

    public void stopWeatherAndThunder() {
        if (!GameClient.client) {
            this.weatherPeriod.stopWeatherPeriod();
            this.thunderStorm.stopAllClouds();
            if (GameServer.server) {
                this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)1, null);
            }
        }
    }

    public ThunderStorm getThunderStorm() {
        return this.thunderStorm;
    }

    public WeatherPeriod getWeatherPeriod() {
        return this.weatherPeriod;
    }

    public boolean getIsThunderStorming() {
        return this.weatherPeriod.isRunning() && (this.weatherPeriod.isThunderStorm() || this.weatherPeriod.isTropicalStorm());
    }

    public float getWeatherInterference() {
        if (this.weatherPeriod.isRunning()) {
            return !this.weatherPeriod.isThunderStorm() && !this.weatherPeriod.isTropicalStorm() && !this.weatherPeriod.isBlizzard()
                ? 0.35F * this.weatherPeriod.getCurrentStrength()
                : 0.7F * this.weatherPeriod.getCurrentStrength();
        } else {
            return 0.0F;
        }
    }

    public KahluaTable getModData() {
        if (this.modDataTable == null) {
            this.modDataTable = LuaManager.platform.newTable();
        }

        return this.modDataTable;
    }

    public float getAirTemperatureForCharacter(IsoGameCharacter plr) {
        return this.getAirTemperatureForCharacter(plr, false);
    }

    public float getAirTemperatureForCharacter(IsoGameCharacter plr, boolean doWindChill) {
        if (plr.square != null) {
            return plr.getVehicle() != null
                ? this.getAirTemperatureForSquare(plr.square, plr.getVehicle(), doWindChill)
                : this.getAirTemperatureForSquare(plr.square, null, doWindChill);
        } else {
            return this.getTemperature();
        }
    }

    public float getAirTemperatureForSquare(IsoGridSquare square) {
        return this.getAirTemperatureForSquare(square, null);
    }

    public float getAirTemperatureForSquare(IsoGridSquare square, BaseVehicle vehicle) {
        return this.getAirTemperatureForSquare(square, vehicle, false);
    }

    public float getAirTemperatureForSquare(IsoGridSquare square, BaseVehicle vehicle, boolean doWindChill) {
        float temp = this.getTemperature();
        if (square != null) {
            boolean isInside = square.isInARoom();
            if (isInside || vehicle != null) {
                boolean electricity = IsoWorld.instance.isHydroPowerOn();
                if (temp <= 22.0F) {
                    float mod = (22.0F - temp) / 8.0F;
                    if (isInside && electricity) {
                        temp = 22.0F;
                    }

                    mod = 22.0F - temp;
                    if (square.getZ() < 1) {
                        temp += mod * (0.4F + 0.2F * this.dayLightLagged);
                    } else {
                        mod = (float)(mod * 0.85);
                        temp += mod * (0.4F + 0.2F * this.dayLightLagged);
                    }
                } else {
                    float modx = (temp - 22.0F) / 3.5F;
                    if (isInside && electricity) {
                        temp = 22.0F;
                    }

                    modx = temp - 22.0F;
                    if (square.getZ() < 1) {
                        modx = (float)(modx * 0.85);
                        temp -= modx * (0.4F + 0.2F * this.dayLightLagged);
                    } else {
                        temp -= modx * (0.4F + 0.2F * this.dayLightLagged + 0.2F * this.nightLagged);
                    }

                    if (!isInside && vehicle != null) {
                        temp = temp + modx + modx * this.dayLightLagged;
                    }
                }
            } else if (doWindChill) {
                temp = Temperature.WindchillCelsiusKph(temp, this.getWindspeedKph());
            }

            float heatsourceTemp = IsoWorld.instance.getCell().getHeatSourceHighestTemperature(temp, square.getX(), square.getY(), square.getZ());
            if (heatsourceTemp > temp) {
                temp = heatsourceTemp;
            }

            if (vehicle != null) {
                if (!isInside) {
                    temp += vehicle.getInsideTemperature();
                } else {
                    temp += vehicle.getInsideTemperature() > 0.0F ? vehicle.getInsideTemperature() : 0.0F;
                }
            }
        }

        return temp;
    }

    public String getSeasonName() {
        return this.season != null && this.season.getSeasonName() != null ? this.season.getSeasonName() : null;
    }

    public String getSeasonNameTranslated() {
        return this.season != null && this.season.getSeasonNameTranslated() != null ? this.season.getSeasonNameTranslated() : null;
    }

    public byte getSeasonId() {
        return (byte)this.season.getSeason();
    }

    public float getSeasonProgression() {
        return this.season.getSeasonProgression();
    }

    public float getSeasonStrength() {
        return this.season.getSeasonStrength();
    }

    public void init(IsoMetaGrid metaGrid) {
        WorldFlares.Clear();
        this.season = ErosionMain.getInstance().getSeasons();
        ThunderStorm.mapMinX = metaGrid.minX * 256 - 4000;
        ThunderStorm.mapMaxX = metaGrid.maxX * 256 + 4000;
        ThunderStorm.mapMinY = metaGrid.minY * 256 - 4000;
        ThunderStorm.mapMaxY = metaGrid.maxY * 256 + 4000;
        windNoiseOffset = 0.0;
        winterIsComing = IsoWorld.instance.getGameMode().equals("Winter is Coming");
        theDescendingFog = IsoWorld.instance.getGameMode().equals("The Descending Fog");
        aStormIsComing = IsoWorld.instance.getGameMode().equals("A Storm is Coming");
        this.climateForecaster.init(this);
        this.climateHistory.init(this);
    }

    public void updateEveryTenMins() {
        this.tickIsTenMins = true;
    }

    public void update() {
        this.tickIsClimateTick = false;
        this.tickIsHourChange = false;
        this.tickIsDayChange = false;
        this.gt = GameTime.getInstance();
        this.worldAgeHours = this.gt.getWorldAgeHours();
        if (this.lastMinuteStamp != this.gt.getMinutesStamp()) {
            this.lastMinuteStamp = this.gt.getMinutesStamp();
            this.tickIsClimateTick = true;
            this.updateDayInfo(this.gt.getDayPlusOne(), this.gt.getMonth(), this.gt.getYear());
            this.currentDay.hour = this.gt.getHour();
            this.currentDay.minutes = this.gt.getMinutes();
            if (this.gt.getHour() != this.lastHourStamp) {
                this.tickIsHourChange = true;
                this.lastHourStamp = this.gt.getHour();
            }

            ClimateMoon.getInstance().updatePhase(this.currentDay.getYear(), this.currentDay.getMonth(), this.currentDay.getDay());
        }

        if (this.disableSimulation) {
            IsoPlayer[] players = IsoPlayer.players;

            for (int i = 0; i < players.length; i++) {
                IsoPlayer player = players[i];
                if (player != null) {
                    player.dirtyRecalcGridStackTime = 1.0F;
                }
            }
        } else {
            if (this.tickIsDayChange && !GameClient.client) {
                this.climateForecaster.updateDayChange(this);
                this.climateHistory.updateDayChange(this);
            }

            if (GameClient.client) {
                this.networkLerp = 1.0F;
                long curtime = System.currentTimeMillis();
                if ((float)curtime < (float)this.networkUpdateStamp + this.networkLerpTime) {
                    this.networkLerp = (float)(curtime - this.networkUpdateStamp) / this.networkLerpTime;
                    if (this.networkLerp < 0.0F) {
                        this.networkLerp = 0.0F;
                    }
                }

                for (int ix = 0; ix < this.climateFloats.length; ix++) {
                    this.climateFloats[ix].interpolate = this.networkLerp;
                }

                for (int ix = 0; ix < this.climateColors.length; ix++) {
                    this.climateColors[ix].interpolate = this.networkLerp;
                }
            }

            if (this.tickIsClimateTick && !GameClient.client) {
                this.updateSandboxOverrides();
                this.updateValues();
                this.weatherPeriod.update(this.worldAgeHours);
            }

            if (this.tickIsClimateTick) {
                LuaEventManager.triggerEvent("OnClimateTick", this);
            }

            for (int ix = 0; ix < this.climateColors.length; ix++) {
                this.climateColors[ix].calculate();
            }

            for (int ix = 0; ix < this.climateFloats.length; ix++) {
                this.climateFloats[ix].calculate();
            }

            for (int ix = 0; ix < this.climateBooleans.length; ix++) {
                this.climateBooleans[ix].calculate();
            }

            this.updateWindTick();
            this.windPower = this.windIntensity.finalValue;
            this.updateTestFlare();
            this.thunderStorm.update(this.worldAgeHours);
            if (GameClient.client) {
                this.updateSnow();
            } else if (this.tickIsClimateTick && !GameClient.client) {
                this.updateSnow();
            }

            if (!GameClient.client) {
                this.updateViewDistance();
            }

            if (this.tickIsClimateTick && Core.debug && !GameServer.server) {
                LuaEventManager.triggerEvent("OnClimateTickDebug", this);
            }

            if (this.tickIsClimateTick && GameServer.server && this.tickIsTenMins) {
                this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
                this.tickIsTenMins = false;
            }

            if (!this.disableFxUpdate) {
                this.updateFx();
            }
        }
    }

    private void updateSandboxOverrides() {
        boolean weatherSettingChanged = false;
        float minuteLerp = GameTime.getInstance().getTimeOfDay() % 1.0F;
        int climateCycle = SandboxOptions.getInstance().climateCycle.getValue();
        boolean isOverridingWeather = climateCycle > 1;
        boolean isEndlessWeather = climateCycle > 2;
        boolean isNoWeather = climateCycle == 2;
        boolean isEndlessRain = climateCycle == 3;
        boolean isEndlessSnow = climateCycle == 5;
        boolean isEndlessBlizzard = climateCycle == 6;
        boolean isFreezingTemp = isEndlessSnow || isEndlessBlizzard;
        if (this.weatherOverride != climateCycle) {
            this.weatherOverride = climateCycle;
            this.setEnabledWeatherGeneration(this.weatherOverride == 1);
            this.precipitationIntensity.setOverrideValue(isOverridingWeather);
            this.windIntensity.setOverrideValue(isOverridingWeather);
            this.temperature.setOverrideValue(isFreezingTemp);
            Core.getInstance().setForceSnow(isFreezingTemp);
            if (isNoWeather) {
                Core.getInstance().setForceSnow(false);
                this.precipitationIntensity.setOverride(0.0F, 1.0F);
                this.windIntensity.setOverride(0.0F, 1.0F);
                this.temperature.setOverrideValue(false);
            } else if (isEndlessWeather) {
                this.precipitationIntensity.overrideInternal = 0.5F;
                this.windIntensity.overrideInternal = 0.3F;
                if (isFreezingTemp) {
                    this.temperature.overrideInternal = Rand.Next(-20.0F, 0.0F);
                }
            }

            weatherSettingChanged = isOverridingWeather;
        }

        if (isEndlessWeather) {
            this.precipitationIntensity.interpolate = minuteLerp;
            this.windIntensity.interpolate = minuteLerp;
            this.temperature.interpolate = minuteLerp;
            if (this.tickIsHourChange || weatherSettingChanged) {
                Core.getInstance().setForceSnow(isFreezingTemp);
                this.precipitationIsSnow.setOverride(isFreezingTemp);
                boolean isCalmWeather = isEndlessRain || isEndlessSnow;
                this.precipitationIntensity.overrideInternal = this.precipitationIntensity.override;
                this.precipitationIntensity.setOverride(Rand.Next(isCalmWeather ? 0.1F : 0.5F, isCalmWeather ? 0.5F : 1.0F), minuteLerp);
                this.windIntensity.overrideInternal = this.windIntensity.override;
                this.windIntensity.setOverride(Rand.Next(isCalmWeather ? 0.0F : 0.3F, isCalmWeather ? 0.3F : 1.0F), minuteLerp);
                if (isEndlessSnow || isEndlessBlizzard) {
                    this.temperature.overrideInternal = this.temperature.override;
                    this.temperature.setOverride(Rand.Next(-20.0F, 0.0F), minuteLerp);
                }
            }
        }

        boolean blizzardFog = isEndlessBlizzard && SandboxOptions.getInstance().fogCycle.getValue() != 2;
        int fogCycle = blizzardFog ? 4 : SandboxOptions.getInstance().fogCycle.getValue();
        boolean isOverridingFog = fogCycle > 1;
        boolean isNoFog = fogCycle == 2;
        boolean isEndlessFog = fogCycle >= 3;
        if (this.fogOverride != fogCycle) {
            this.fogOverride = fogCycle;
            this.fogIntensity.setEnableOverride(isOverridingFog);
            this.fogIntensity.setOverrideValue(isOverridingFog);
            if (isNoFog) {
                this.fogIntensity.setOverride(0.0F, 1.0F);
            } else if (isEndlessFog) {
                this.fogIntensity.overrideInternal = 0.5F;
            }

            weatherSettingChanged = isOverridingFog;
        }

        if (isEndlessFog) {
            this.fogIntensity.interpolate = minuteLerp;
            if (this.tickIsHourChange || weatherSettingChanged) {
                this.fogIntensity.overrideInternal = this.fogIntensity.override;
                this.fogIntensity.setOverride(Rand.Next(0.1F, 1.0F), minuteLerp);
            }
        }
    }

    public static double getWindNoiseBase() {
        return windNoiseBase;
    }

    public static double getWindNoiseFinal() {
        return windNoiseFinal;
    }

    public static double getWindTickFinal() {
        return windTickFinal;
    }

    private void updateWindTick() {
        if (!GameServer.server) {
            float wind = this.windIntensity.finalValue;
            windNoiseOffset = windNoiseOffset + (4.0E-4 + 6.0E-4 * wind) * GameTime.getInstance().getMultiplier();
            windNoiseBase = SimplexNoise.noise(0.0, windNoiseOffset);
            windNoiseFinal = windNoiseBase;
            if (windNoiseFinal > 0.0) {
                windNoiseFinal *= 0.04 + 0.1 * wind;
            } else {
                windNoiseFinal *= 0.04 + 0.1 * wind + 0.05F * (wind * wind);
            }

            wind = clamp01(wind + (float)windNoiseFinal);
            windTickFinal = wind;
        }
    }

    public void updateOLD() {
        this.tickIsClimateTick = false;
        this.tickIsHourChange = false;
        this.tickIsDayChange = false;
        this.gt = GameTime.getInstance();
        this.worldAgeHours = this.gt.getWorldAgeHours();
        if (this.lastMinuteStamp != this.gt.getMinutesStamp()) {
            this.lastMinuteStamp = this.gt.getMinutesStamp();
            this.tickIsClimateTick = true;
            this.updateDayInfo(this.gt.getDay(), this.gt.getMonth(), this.gt.getYear());
            this.currentDay.hour = this.gt.getHour();
            this.currentDay.minutes = this.gt.getMinutes();
            if (this.gt.getHour() != this.lastHourStamp) {
                this.tickIsHourChange = true;
                this.lastHourStamp = this.gt.getHour();
            }
        }

        if (GameClient.client) {
            if (!this.disableSimulation) {
                this.networkLerp = 1.0F;
                long curtime = System.currentTimeMillis();
                if ((float)curtime < (float)this.networkUpdateStamp + this.networkLerpTime) {
                    this.networkLerp = (float)(curtime - this.networkUpdateStamp) / this.networkLerpTime;
                    if (this.networkLerp < 0.0F) {
                        this.networkLerp = 0.0F;
                    }
                }

                for (int i = 0; i < this.climateFloats.length; i++) {
                    this.climateFloats[i].interpolate = this.networkLerp;
                }

                for (int i = 0; i < this.climateColors.length; i++) {
                    this.climateColors[i].interpolate = this.networkLerp;
                }

                if (this.tickIsClimateTick) {
                    LuaEventManager.triggerEvent("OnClimateTick", this);
                }

                this.updateOnTick();
                this.updateTestFlare();
                this.thunderStorm.update(this.worldAgeHours);
                this.updateSnow();
                if (this.tickIsTenMins) {
                    this.tickIsTenMins = false;
                }
            }

            this.updateFx();
        } else {
            if (!this.disableSimulation) {
                if (this.tickIsClimateTick) {
                    this.updateValues();
                    this.weatherPeriod.update(this.gt.getWorldAgeHours());
                }

                this.updateOnTick();
                this.updateTestFlare();
                this.thunderStorm.update(this.worldAgeHours);
                if (this.tickIsClimateTick) {
                    this.updateSnow();
                    LuaEventManager.triggerEvent("OnClimateTick", this);
                }

                this.updateViewDistance();
                if (this.tickIsClimateTick && this.tickIsTenMins) {
                    if (GameServer.server) {
                        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
                    }

                    this.tickIsTenMins = false;
                }
            }

            if (!this.disableFxUpdate && this.tickIsClimateTick) {
                this.updateFx();
            }

            if (this.disableSimulation) {
                IsoPlayer[] players = IsoPlayer.players;

                for (int i = 0; i < players.length; i++) {
                    IsoPlayer player = players[i];
                    if (player != null) {
                        player.dirtyRecalcGridStackTime = 1.0F;
                    }
                }
            }
        }
    }

    private void updateFx() {
        IsoWeatherFX weatherFX = IsoWorld.instance.getCell().getWeatherFX();
        if (weatherFX != null) {
            weatherFX.setPrecipitationIntensity(this.precipitationIntensity.finalValue);
            weatherFX.setWindIntensity(this.windIntensity.finalValue);
            weatherFX.setWindPrecipIntensity((float)windTickFinal * (float)windTickFinal);
            weatherFX.setWindAngleIntensity(this.windAngleIntensity.finalValue);
            weatherFX.setFogIntensity(this.fogIntensity.finalValue);
            weatherFX.setCloudIntensity(this.cloudIntensity.finalValue);
            weatherFX.setPrecipitationIsSnow(this.precipitationIsSnow.finalValue);
            SkyBox.getInstance().update(this);
            IsoWater.getInstance().update(this);
            IsoPuddles.getInstance().update(this);
        }
    }

    private void updateSnow() {
        if (GameClient.client) {
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0F));
            ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2F);
        } else if (Core.getInstance().isForceSnow()) {
            this.snowFracNow = 0.7F;
            IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0F));
            ErosionIceQueen.instance.setSnow(this.snowFracNow > 0.2F);
            this.wasForceSnow = true;
        } else {
            if (this.wasForceSnow) {
                this.snowFracNow = this.snowStrength > 7.5F ? 1.0F : this.snowStrength / 7.5F;
                IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0F));
                ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2F);
                this.wasForceSnow = false;
            }

            if (!this.tickIsHourChange) {
                this.canDoWinterSprites = this.season.isSeason(5) || winterIsComing;
                if (this.precipitationIsSnow.finalValue && this.precipitationIntensity.finalValue > this.snowFall) {
                    this.snowFall = this.precipitationIntensity.finalValue;
                }

                if (this.temperature.finalValue > 0.0F) {
                    float melt = this.temperature.finalValue / 10.0F;
                    melt = melt * 0.2F + melt * 0.8F * this.dayLightStrength.finalValue;
                    if (melt > this.snowMeltStrength) {
                        this.snowMeltStrength = melt;
                    }
                }

                if (!this.precipitationIsSnow.finalValue && this.precipitationIntensity.finalValue > 0.0F) {
                    this.snowMeltStrength = this.snowMeltStrength + this.precipitationIntensity.finalValue;
                }
            } else {
                this.snowStrength = this.snowStrength + this.snowFall;
                this.snowStrength = this.snowStrength - this.snowMeltStrength;
                this.snowStrength = clamp(0.0F, 10.0F, this.snowStrength);
                this.snowFracNow = this.snowStrength > 7.5F ? 1.0F : this.snowStrength / 7.5F;
                IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0F));
                ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2F);
                this.snowFall = 0.0F;
                this.snowMeltStrength = 0.0F;
            }
        }
    }

    private void updateSnowOLD() {
    }

    public float getSnowFracNow() {
        return this.snowFracNow;
    }

    public void resetOverrides() {
        for (int i = 0; i < this.climateColors.length; i++) {
            this.climateColors[i].setEnableOverride(false);
        }

        for (int i = 0; i < this.climateFloats.length; i++) {
            this.climateFloats[i].setEnableOverride(false);
        }

        for (int i = 0; i < this.climateBooleans.length; i++) {
            this.climateBooleans[i].setEnableOverride(false);
        }
    }

    public void resetModded() {
        for (int i = 0; i < this.climateColors.length; i++) {
            this.climateColors[i].setEnableModded(false);
        }

        for (int i = 0; i < this.climateFloats.length; i++) {
            this.climateFloats[i].setEnableModded(false);
        }

        for (int i = 0; i < this.climateBooleans.length; i++) {
            this.climateBooleans[i].setEnableModded(false);
        }
    }

    public void resetAdmin() {
        for (int i = 0; i < this.climateColors.length; i++) {
            this.climateColors[i].setEnableAdmin(false);
        }

        for (int i = 0; i < this.climateFloats.length; i++) {
            this.climateFloats[i].setEnableAdmin(false);
        }

        for (int i = 0; i < this.climateBooleans.length; i++) {
            this.climateBooleans[i].setEnableAdmin(false);
        }
    }

    public void triggerWinterIsComingStorm() {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            ClimateManager.AirFront front = new ClimateManager.AirFront();
            front.copyFrom(this.currentFront);
            front.strength = 0.95F;
            front.type = 1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne());
        }
    }

    public boolean triggerCustomWeather(float strength, boolean warmFront) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            ClimateManager.AirFront front = new ClimateManager.AirFront();
            front.strength = strength;
            front.type = warmFront ? 1 : -1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne());
            return true;
        } else {
            return false;
        }
    }

    public boolean triggerCustomWeatherStage(int stage, float duration) {
        if (!GameClient.client && !this.weatherPeriod.isRunning()) {
            ClimateManager.AirFront front = new ClimateManager.AirFront();
            front.strength = 0.95F;
            front.type = 1;
            GameTime gt = GameTime.getInstance();
            this.weatherPeriod.init(front, this.worldAgeHours, gt.getYear(), gt.getMonth(), gt.getDayPlusOne(), stage, duration);
            return true;
        } else {
            return false;
        }
    }

    private void updateOnTick() {
        for (int i = 0; i < this.climateColors.length; i++) {
            this.climateColors[i].calculate();
        }

        for (int i = 0; i < this.climateFloats.length; i++) {
            this.climateFloats[i].calculate();
        }

        for (int i = 0; i < this.climateBooleans.length; i++) {
            this.climateBooleans[i].calculate();
        }
    }

    private void updateTestFlare() {
        WorldFlares.update();
    }

    public void launchFlare() {
        DebugLog.log("Launching improved flare.");
        IsoPlayer player = IsoPlayer.getInstance();
        float windspeed = 0.0F;
        WorldFlares.launchFlare(7200.0F, (int)player.getX(), (int)player.getY(), 50, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
        if (IsoPlayer.getInstance() != null && !this.flareLaunched) {
            this.flareLaunched = true;
            this.flareLifeTime = 0.0F;
            this.flareMaxLifeTime = 7200.0F;
            this.flareIntensity.overrideCurrentValue(1.0F);
            this.flareIntens = 1.0F;
            this.nextRandomTargetIntens = 10;
        }
    }

    protected double getAirMassNoiseFrequencyMod(int sandboxRain) {
        if (sandboxRain == 1) {
            return 300.0;
        } else if (sandboxRain == 2) {
            return 240.0;
        } else {
            if (sandboxRain != 3) {
                if (sandboxRain == 4) {
                    return 145.0;
                }

                if (sandboxRain == 5) {
                    return 120.0;
                }
            }

            return 166.0;
        }
    }

    protected float getRainTimeMultiplierMod(int sandboxRain) {
        if (sandboxRain == 1) {
            return 0.5F;
        } else if (sandboxRain == 2) {
            return 0.75F;
        } else if (sandboxRain == 4) {
            return 1.25F;
        } else {
            return sandboxRain == 5 ? 1.5F : 1.0F;
        }
    }

    private void updateValues() {
        if (this.tickIsDayChange && Core.debug && !GameClient.client && !GameServer.server) {
            ErosionMain.getInstance().DebugUpdateMapNow();
        }

        this.climateValues.updateValues(this.worldAgeHours, this.gt.getTimeOfDay(), this.currentDay, this.nextDay);
        this.airMass = this.climateValues.getNoiseAirmass();
        this.airMassTemperature = this.climateValues.getAirMassTemperature();
        if (this.tickIsHourChange) {
            int airType = this.airMass < 0.0F ? -1 : 1;
            if (this.currentFront.type != airType) {
                if (!this.disableWeatherGeneration && (!winterIsComing || winterIsComing && GameTime.instance.getWorldAgeHours() > 96.0)) {
                    if (theDescendingFog) {
                        this.currentFront.type = -1;
                        this.currentFront.strength = Rand.Next(0.2F, 0.45F);
                        this.weatherPeriod.init(this.currentFront, this.worldAgeHours, this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne());
                    } else {
                        this.CalculateWeatherFrontStrength(this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne(), this.currentFront);
                        this.weatherPeriod.init(this.currentFront, this.worldAgeHours, this.gt.getYear(), this.gt.getMonth(), this.gt.getDayPlusOne());
                    }
                }

                this.currentFront.setFrontType(airType);
            }

            if (!winterIsComing
                && !theDescendingFog
                && GameTime.instance.getWorldAgeHours() >= 72.0
                && GameTime.instance.getWorldAgeHours() <= 96.0
                && !this.disableWeatherGeneration
                && !this.weatherPeriod.isRunning()
                && Rand.Next(0, 1000) < 50) {
            }

            if (this.tickIsDayChange) {
            }
        }

        this.dayDoFog = this.climateValues.isDayDoFog();
        this.dayFogStrength = this.climateValues.getDayFogStrength();
        if (PerformanceSettings.fogQuality == 2) {
            this.dayFogStrength = 0.5F + 0.5F * this.dayFogStrength;
        } else {
            this.dayFogStrength = 0.2F + 0.8F * this.dayFogStrength;
        }

        this.baseTemperature = this.climateValues.getBaseTemperature();
        this.dayLightLagged = this.climateValues.getDayLightLagged();
        this.nightLagged = this.climateValues.getDayLightLagged();
        this.temperature.internalValue = this.climateValues.getTemperature();
        this.precipitationIsSnow.internalValue = this.climateValues.isTemperatureIsSnow();
        this.humidity.internalValue = this.climateValues.getHumidity();
        this.windIntensity.internalValue = this.climateValues.getWindIntensity();
        this.windAngleIntensity.internalValue = this.climateValues.getWindAngleIntensity();
        this.windPower = this.windIntensity.internalValue;
        this.currentFront.setFrontWind(this.climateValues.getWindAngleDegrees());
        this.cloudIntensity.internalValue = this.climateValues.getCloudIntensity();
        this.precipitationIntensity.internalValue = 0.0F;
        this.nightStrength.internalValue = this.climateValues.getNightStrength();
        this.dayLightStrength.internalValue = this.climateValues.getDayLightStrength();
        this.ambient.internalValue = this.climateValues.getAmbient();
        this.desaturation.internalValue = this.climateValues.getDesaturation();
        int curSeason = this.season.getSeason();
        float seasonProg = this.season.getSeasonProgression();
        float tval = 0.0F;
        int lerpFromSeason = 0;
        int lerpToSeason = 0;
        if (curSeason == 2) {
            lerpFromSeason = 3;
            lerpToSeason = 0;
            tval = 0.5F + seasonProg * 0.5F;
        } else if (curSeason == 3) {
            lerpFromSeason = 0;
            lerpToSeason = 1;
            tval = seasonProg * 0.5F;
        } else if (curSeason == 4) {
            if (seasonProg < 0.5F) {
                lerpFromSeason = 0;
                lerpToSeason = 1;
                tval = 0.5F + seasonProg;
            } else {
                lerpFromSeason = 1;
                lerpToSeason = 2;
                tval = seasonProg - 0.5F;
            }
        } else if (curSeason == 5) {
            if (seasonProg < 0.5F) {
                lerpFromSeason = 1;
                lerpToSeason = 2;
                tval = 0.5F + seasonProg;
            } else {
                lerpFromSeason = 2;
                lerpToSeason = 3;
                tval = seasonProg - 0.5F;
            }
        } else if (curSeason == 1) {
            if (seasonProg < 0.5F) {
                lerpFromSeason = 2;
                lerpToSeason = 3;
                tval = 0.5F + seasonProg;
            } else {
                lerpFromSeason = 3;
                lerpToSeason = 0;
                tval = seasonProg - 0.5F;
            }
        }

        float cloudyT = this.climateValues.getCloudyT();
        this.colDawn = this.seasonColorDawn.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        this.colDay = this.seasonColorDay.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        this.colDusk = this.seasonColorDusk.update(cloudyT, tval, lerpFromSeason, lerpToSeason);
        float time = this.climateValues.getTime();
        float dawn = this.climateValues.getDawn();
        float dusk = this.climateValues.getDusk();
        float noon = this.climateValues.getNoon();
        float fogDuration = this.climateValues.getDayFogDuration();
        if (!theDescendingFog) {
            if (this.dayDoFog && this.dayFogStrength > 0.0F && time > dawn - 2.0F && time < dawn + fogDuration) {
                float lerpfog = this.getTimeLerpHours(time, dawn - 2.0F, dawn + fogDuration, true);
                lerpfog = clamp(0.0F, 1.0F, lerpfog * (fogDuration / 3.0F));
                this.fogLerpValue = lerpfog;
                this.cloudIntensity.internalValue = lerp(lerpfog, this.cloudIntensity.internalValue, 0.0F);
                float fogVal = this.dayFogStrength;
                this.fogIntensity.internalValue = clerp(lerpfog, 0.0F, fogVal);
                if (SceneShaderStore.weatherShader == null || Core.getInstance().getOffscreenBuffer() == null) {
                    this.desaturation.internalValue = clerp(lerpfog, this.desaturation.internalValue, 0.8F * fogVal);
                } else if (PerformanceSettings.fogQuality == 2) {
                    this.desaturation.internalValue = clerp(lerpfog, this.desaturation.internalValue, 0.8F * fogVal);
                } else {
                    this.desaturation.internalValue = clerp(lerpfog, this.desaturation.internalValue, 0.65F * fogVal);
                }
            } else {
                this.fogIntensity.internalValue = 0.0F;
            }
        } else {
            if (this.gt.getWorldAgeHours() < 72.0) {
                this.fogIntensity.internalValue = (float)this.gt.getWorldAgeHours() / 72.0F;
            } else {
                this.fogIntensity.internalValue = 1.0F;
            }

            this.cloudIntensity.internalValue = Math.min(this.cloudIntensity.internalValue, 1.0F - this.fogIntensity.internalValue);
            if (this.weatherPeriod.isRunning()) {
                this.fogIntensity.internalValue = Math.min(this.fogIntensity.internalValue, 0.6F);
            }

            if (PerformanceSettings.fogQuality == 2) {
                this.fogIntensity.internalValue *= 0.93F;
                this.desaturation.internalValue = 0.8F * this.fogIntensity.internalValue;
            } else {
                this.desaturation.internalValue = 0.65F * this.fogIntensity.internalValue;
            }
        }

        this.humidity.internalValue = clamp01(this.humidity.internalValue + this.fogIntensity.internalValue * 0.6F);
        float dayMax = this.climateValues.getDayLightStrengthBase();
        float nightMax = 0.4F;
        float duskDawnMin = 0.25F * this.climateValues.getDayLightStrengthBase();
        if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
            duskDawnMin = 0.8F * this.climateValues.getDayLightStrengthBase();
        }

        if (time < dawn || time > dusk) {
            float total = 24.0F - dusk + dawn;
            if (time > dusk) {
                float t = (time - dusk) / total;
                this.colDusk.interp(this.colDawn, t, this.globalLight.internalValue);
            } else {
                float t = (24.0F - dusk + time) / total;
                this.colDusk.interp(this.colDawn, t, this.globalLight.internalValue);
            }

            this.globalLightIntensity.internalValue = lerp(this.climateValues.getLerpNight(), duskDawnMin, 0.4F);
        } else if (time < noon + 2.0F) {
            float t = (time - dawn) / (noon + 2.0F - dawn);
            this.colDawn.interp(this.colDay, t, this.globalLight.internalValue);
            this.globalLightIntensity.internalValue = lerp(t, duskDawnMin, dayMax);
        } else {
            float t = (time - (noon + 2.0F)) / (dusk - (noon + 2.0F));
            this.colDay.interp(this.colDusk, t, this.globalLight.internalValue);
            this.globalLightIntensity.internalValue = lerp(t, dayMax, duskDawnMin);
        }

        if (this.fogIntensity.internalValue > 0.0F) {
            if (SceneShaderStore.weatherShader == null || Core.getInstance().getOffscreenBuffer() == null) {
                this.globalLight.internalValue.interp(this.colFogLegacy, this.fogIntensity.internalValue, this.globalLight.internalValue);
            } else if (PerformanceSettings.fogQuality == 2) {
                this.globalLight.internalValue.interp(this.colFog, this.fogIntensity.internalValue, this.globalLight.internalValue);
            } else {
                this.globalLight.internalValue.interp(this.colFogNew, this.fogIntensity.internalValue, this.globalLight.internalValue);
            }

            this.globalLightIntensity.internalValue = clerp(this.fogLerpValue, this.globalLightIntensity.internalValue, 0.8F);
        }

        this.colNightNoMoon.interp(this.colNightMoon, ClimateMoon.getInstance().getMoonFloat(), this.colNight);
        this.globalLight.internalValue.interp(this.colNight, this.nightStrength.internalValue, this.globalLight.internalValue);
        IsoPlayer[] players = IsoPlayer.players;

        for (int i = 0; i < players.length; i++) {
            IsoPlayer player = players[i];
            if (player != null) {
                player.dirtyRecalcGridStackTime = 1.0F;
            }
        }
    }

    private void updateViewDistance() {
        float viewMod = this.dayLightStrength.finalValue;
        float fogMod = this.fogIntensity.finalValue;
        float min = 19.0F - fogMod * 8.0F;
        float max = min + 4.0F + 7.0F * viewMod * (1.0F - fogMod);
        min *= 3.0F;
        max *= 3.0F;
        this.gt.setViewDistMin(min);
        this.gt.setViewDistMax(max);
        this.viewDistance.internalValue = min + (max - min) * viewMod;
        this.viewDistance.finalValue = this.viewDistance.internalValue;
    }

    public void setSeasonColorDawn(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDawn.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDawn.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public void setSeasonColorDay(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDay.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDay.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public void setSeasonColorDusk(int temperature, int season, float r, float g, float b, float a, boolean exterior) {
        if (exterior) {
            this.seasonColorDusk.setColorExterior(temperature, season, r, g, b, a);
        } else {
            this.seasonColorDusk.setColorInterior(temperature, season, r, g, b, a);
        }
    }

    public ClimateColorInfo getSeasonColor(int segment, int temperature, int season) {
        ClimateManager.SeasonColor s = null;
        if (segment == 0) {
            s = this.seasonColorDawn;
        } else if (segment == 1) {
            s = this.seasonColorDay;
        } else if (segment == 2) {
            s = this.seasonColorDusk;
        }

        return s != null ? s.getColor(temperature, season) : null;
    }

    private void initSeasonColors() {
        ClimateManager.SeasonColor s = new ClimateManager.SeasonColor();
        s.setIgnoreNormal(true);
        this.seasonColorDawn = s;
        s = new ClimateManager.SeasonColor();
        s.setIgnoreNormal(true);
        this.seasonColorDay = s;
        s = new ClimateManager.SeasonColor();
        s.setIgnoreNormal(false);
        this.seasonColorDusk = s;
    }

    /**
     * IO
     */
    public void save(DataOutputStream output) throws IOException {
        if (GameClient.client && !GameServer.server) {
            output.writeByte(0);
        } else {
            output.writeByte(1);
            output.writeDouble(this.simplexOffsetA);
            output.writeDouble(this.simplexOffsetB);
            output.writeDouble(this.simplexOffsetC);
            output.writeDouble(this.simplexOffsetD);
            this.currentFront.save(output);
            output.writeFloat(this.snowFracNow);
            output.writeFloat(this.snowStrength);
            output.writeBoolean(this.canDoWinterSprites);
            output.writeBoolean(this.dayDoFog);
            output.writeFloat(this.dayFogStrength);
        }

        this.weatherPeriod.save(output);
        this.thunderStorm.save(output);
        if (GameServer.server) {
            this.desaturation.saveAdmin(output);
            this.globalLightIntensity.saveAdmin(output);
            this.nightStrength.saveAdmin(output);
            this.precipitationIntensity.saveAdmin(output);
            this.temperature.saveAdmin(output);
            this.fogIntensity.saveAdmin(output);
            this.windIntensity.saveAdmin(output);
            this.windAngleIntensity.saveAdmin(output);
            this.cloudIntensity.saveAdmin(output);
            this.ambient.saveAdmin(output);
            this.viewDistance.saveAdmin(output);
            this.dayLightStrength.saveAdmin(output);
            this.globalLight.saveAdmin(output);
            this.precipitationIsSnow.saveAdmin(output);
        }

        if (this.modDataTable != null) {
            output.writeByte(1);
            this.modDataTable.save(output);
        } else {
            output.writeByte(0);
        }

        if (GameServer.server) {
            this.humidity.saveAdmin(output);
        }
    }

    public void load(DataInputStream input, int worldVersion) throws IOException {
        boolean hasstuff = input.readByte() == 1;
        if (hasstuff) {
            this.simplexOffsetA = input.readDouble();
            this.simplexOffsetB = input.readDouble();
            this.simplexOffsetC = input.readDouble();
            this.simplexOffsetD = input.readDouble();
            this.currentFront.load(input);
            this.snowFracNow = input.readFloat();
            this.snowStrength = input.readFloat();
            this.canDoWinterSprites = input.readBoolean();
            this.dayDoFog = input.readBoolean();
            this.dayFogStrength = input.readFloat();
        }

        this.weatherPeriod.load(input, worldVersion);
        this.thunderStorm.load(input);
        if (GameServer.server) {
            this.desaturation.loadAdmin(input, worldVersion);
            this.globalLightIntensity.loadAdmin(input, worldVersion);
            this.nightStrength.loadAdmin(input, worldVersion);
            this.precipitationIntensity.loadAdmin(input, worldVersion);
            this.temperature.loadAdmin(input, worldVersion);
            this.fogIntensity.loadAdmin(input, worldVersion);
            this.windIntensity.loadAdmin(input, worldVersion);
            this.windAngleIntensity.loadAdmin(input, worldVersion);
            this.cloudIntensity.loadAdmin(input, worldVersion);
            this.ambient.loadAdmin(input, worldVersion);
            this.viewDistance.loadAdmin(input, worldVersion);
            this.dayLightStrength.loadAdmin(input, worldVersion);
            this.globalLight.loadAdmin(input, worldVersion);
            this.precipitationIsSnow.loadAdmin(input, worldVersion);
        }

        if (input.readByte() == 1) {
            if (this.modDataTable == null) {
                this.modDataTable = LuaManager.platform.newTable();
            }

            this.modDataTable.load(input, worldVersion);
        }

        if (GameServer.server) {
            this.humidity.loadAdmin(input, worldVersion);
        }

        this.climateValues = new ClimateValues(this);
    }

    public void postCellLoadSetSnow() {
        IsoWorld.instance.currentCell.setSnowTarget((int)(this.snowFracNow * 100.0F));
        ErosionIceQueen.instance.setSnow(this.canDoWinterSprites && this.snowFracNow > 0.2F);
    }

    public void forceDayInfoUpdate() {
        this.currentDay.day = -1;
        this.currentDay.month = -1;
        this.currentDay.year = -1;
        this.gt = GameTime.getInstance();
        this.updateDayInfo(this.gt.getDayPlusOne(), this.gt.getMonth(), this.gt.getYear());
        this.currentDay.hour = this.gt.getHour();
        this.currentDay.minutes = this.gt.getMinutes();
    }

    private void updateDayInfo(int day, int month, int year) {
        this.tickIsDayChange = false;
        if (this.currentDay == null || this.currentDay.day != day || this.currentDay.month != month || this.currentDay.year != year) {
            this.tickIsDayChange = this.currentDay != null;
            if (this.currentDay == null) {
                this.currentDay = new ClimateManager.DayInfo();
            }

            this.setDayInfo(this.currentDay, day, month, year, 0);
            if (this.previousDay == null) {
                this.previousDay = new ClimateManager.DayInfo();
                this.previousDay.season = this.season.clone();
            }

            this.setDayInfo(this.previousDay, day, month, year, -1);
            if (this.nextDay == null) {
                this.nextDay = new ClimateManager.DayInfo();
                this.nextDay.season = this.season.clone();
            }

            this.setDayInfo(this.nextDay, day, month, year, 1);
        }
    }

    protected void setDayInfo(ClimateManager.DayInfo dayInfo, int day, int month, int year, int dayOffset) {
        dayInfo.calendar = new GregorianCalendar(year, month, day, 0, 0);
        dayInfo.calendar.add(5, dayOffset);
        dayInfo.day = dayInfo.calendar.get(5);
        dayInfo.month = dayInfo.calendar.get(2);
        dayInfo.year = dayInfo.calendar.get(1);
        dayInfo.dateValue = dayInfo.calendar.getTime().getTime();
        if (dayInfo.season == null) {
            dayInfo.season = this.season.clone();
        }

        dayInfo.season.setDay(dayInfo.day, dayInfo.month, dayInfo.year);
    }

    protected final void transmitClimatePacket(ClimateManager.ClimateNetAuth auth, byte type, UdpConnection ignoreConnection) {
        if (GameClient.client || GameServer.server) {
            if (auth == ClimateManager.ClimateNetAuth.Denied) {
                DebugLog.log("Denied ClimatePacket, id = " + type + ", isClient = " + GameClient.client);
            } else {
                if (GameClient.client && (auth == ClimateManager.ClimateNetAuth.ClientOnly || auth == ClimateManager.ClimateNetAuth.ClientAndServer)) {
                    try {
                        if (this.writePacketContents(GameClient.connection, type)) {
                            PacketTypes.PacketType.ClimateManagerPacket.send(GameClient.connection);
                        } else {
                            GameClient.connection.cancelPacket();
                        }
                    } catch (Exception var6) {
                        DebugLog.log(var6.getMessage());
                    }
                }

                if (GameServer.server && (auth == ClimateManager.ClimateNetAuth.ServerOnly || auth == ClimateManager.ClimateNetAuth.ClientAndServer)) {
                    try {
                        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                            UdpConnection c = GameServer.udpEngine.connections.get(n);
                            if (ignoreConnection == null || ignoreConnection != c) {
                                if (this.writePacketContents(c, type)) {
                                    PacketTypes.PacketType.ClimateManagerPacket.send(c);
                                } else {
                                    c.cancelPacket();
                                }
                            }
                        }
                    } catch (Exception var7) {
                        DebugLog.log(var7.getMessage());
                    }
                }
            }
        }
    }

    private boolean writePacketContents(UdpConnection connection, byte type) throws IOException {
        if (!GameClient.client && !GameServer.server) {
            return false;
        } else {
            ByteBufferWriter bbw = connection.startPacket();
            PacketTypes.PacketType.ClimateManagerPacket.doPacket(bbw);
            ByteBuffer output = bbw.bb;
            output.put(type);
            switch (type) {
                case 0:
                    for (int i = 0; i < this.climateFloats.length; i++) {
                        output.putFloat(this.climateFloats[i].finalValue);
                    }

                    for (int i = 0; i < this.climateColors.length; i++) {
                        this.climateColors[i].finalValue.write(output);
                    }

                    for (int i = 0; i < this.climateBooleans.length; i++) {
                        output.put((byte)(this.climateBooleans[i].finalValue ? 1 : 0));
                    }

                    output.putFloat(this.airMass);
                    output.putFloat(this.airMassDaily);
                    output.putFloat(this.airMassTemperature);
                    output.putFloat(this.snowFracNow);
                    output.putFloat(this.snowStrength);
                    output.putFloat(this.windPower);
                    output.put((byte)(this.dayDoFog ? 1 : 0));
                    output.putFloat(this.dayFogStrength);
                    output.put((byte)(this.canDoWinterSprites ? 1 : 0));
                    this.weatherPeriod.writeNetWeatherData(output);
                    return true;
                case 1:
                    this.weatherPeriod.writeNetWeatherData(output);
                    return true;
                case 2:
                    this.thunderStorm.writeNetThunderEvent(output);
                    return true;
                case 3:
                    return true;
                case 4:
                    if (!GameServer.server) {
                        return false;
                    }

                    for (int i = 0; i < this.climateFloats.length; i++) {
                        this.climateFloats[i].writeAdmin(output);
                    }

                    for (int i = 0; i < this.climateColors.length; i++) {
                        this.climateColors[i].writeAdmin(output);
                    }

                    for (int i = 0; i < this.climateBooleans.length; i++) {
                        this.climateBooleans[i].writeAdmin(output);
                    }

                    return true;
                case 5:
                    if (!GameClient.client) {
                        return false;
                    }

                    output.put((byte)1);
                    return true;
                case 6:
                    if (!GameClient.client) {
                        return false;
                    }

                    for (int i = 0; i < this.climateFloats.length; i++) {
                        this.climateFloats[i].writeAdmin(output);
                    }

                    for (int i = 0; i < this.climateColors.length; i++) {
                        this.climateColors[i].writeAdmin(output);
                    }

                    for (int i = 0; i < this.climateBooleans.length; i++) {
                        this.climateBooleans[i].writeAdmin(output);
                    }

                    return true;
                case 7:
                    if (!GameClient.client) {
                        return false;
                    }

                    output.put((byte)(this.netInfo.isStopWeather ? 1 : 0));
                    output.put((byte)(this.netInfo.isTrigger ? 1 : 0));
                    output.put((byte)(this.netInfo.isGenerate ? 1 : 0));
                    output.putFloat(this.netInfo.triggerDuration);
                    output.put((byte)(this.netInfo.triggerStorm ? 1 : 0));
                    output.put((byte)(this.netInfo.triggerTropical ? 1 : 0));
                    output.put((byte)(this.netInfo.triggerBlizzard ? 1 : 0));
                    output.putFloat(this.netInfo.generateStrength);
                    output.putInt(this.netInfo.generateFront);
                    return true;
                default:
                    return false;
            }
        }
    }

    public final void receiveClimatePacket(ByteBuffer bb, UdpConnection ignoreConnection) throws IOException {
        if (GameClient.client || GameServer.server) {
            byte packetType = bb.get();
            this.readPacketContents(bb, packetType, ignoreConnection);
        }
    }

    private boolean readPacketContents(ByteBuffer input, byte type, UdpConnection ignoreConnection) throws IOException {
        switch (type) {
            case 0:
                if (!GameClient.client) {
                    return false;
                }

                for (int i = 0; i < this.climateFloats.length; i++) {
                    ClimateManager.ClimateFloat fo = this.climateFloats[i];
                    fo.internalValue = fo.finalValue;
                    fo.setOverride(input.getFloat(), 0.0F);
                }

                for (int i = 0; i < this.climateColors.length; i++) {
                    ClimateManager.ClimateColor co = this.climateColors[i];
                    co.internalValue.setTo(co.finalValue);
                    co.setOverride(input, 0.0F);
                }

                for (int i = 0; i < this.climateBooleans.length; i++) {
                    ClimateManager.ClimateBool bo = this.climateBooleans[i];
                    bo.setOverride(input.get() == 1);
                }

                this.airMass = input.getFloat();
                this.airMassDaily = input.getFloat();
                this.airMassTemperature = input.getFloat();
                this.snowFracNow = input.getFloat();
                this.snowStrength = input.getFloat();
                this.windPower = input.getFloat();
                this.dayDoFog = input.get() == 1;
                this.dayFogStrength = input.getFloat();
                this.canDoWinterSprites = input.get() == 1;
                long curtime = System.currentTimeMillis();
                if ((float)(curtime - this.networkUpdateStamp) < this.networkLerpTime) {
                    this.networkAdjustVal++;
                    if (this.networkAdjustVal > 10.0F) {
                        this.networkAdjustVal = 10.0F;
                    }
                } else {
                    this.networkAdjustVal--;
                    if (this.networkAdjustVal < 0.0F) {
                        this.networkAdjustVal = 0.0F;
                    }
                }

                if (this.networkAdjustVal > 0.0F) {
                    this.networkLerpTime = 5000.0F / this.networkAdjustVal;
                } else {
                    this.networkLerpTime = 5000.0F;
                }

                this.networkUpdateStamp = curtime;
                this.weatherPeriod.readNetWeatherData(input);
                return true;
            case 1:
                this.weatherPeriod.readNetWeatherData(input);
                return true;
            case 2:
                this.thunderStorm.readNetThunderEvent(input);
                return true;
            case 3:
                return true;
            case 4:
                if (!GameClient.client) {
                    return false;
                }

                for (int ix = 0; ix < this.climateFloats.length; ix++) {
                    this.climateFloats[ix].readAdmin(input);
                }

                for (int ix = 0; ix < this.climateColors.length; ix++) {
                    this.climateColors[ix].readAdmin(input);
                }

                for (int ix = 0; ix < this.climateBooleans.length; ix++) {
                    this.climateBooleans[ix].readAdmin(input);
                }

                return true;
            case 5:
                if (!GameServer.server) {
                    return false;
                }

                input.get();
                this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)4, null);
                return true;
            case 6:
                if (!GameServer.server) {
                    return false;
                }

                for (int i = 0; i < this.climateFloats.length; i++) {
                    this.climateFloats[i].readAdmin(input);
                }

                for (int i = 0; i < this.climateColors.length; i++) {
                    this.climateColors[i].readAdmin(input);
                }

                for (int i = 0; i < this.climateBooleans.length; i++) {
                    this.climateBooleans[i].readAdmin(input);
                    if (i == 0) {
                        DebugLog.log("Snow = " + this.climateBooleans[i].adminValue + ", enabled = " + this.climateBooleans[i].isAdminOverride);
                    }
                }

                this.serverReceiveClientChangeAdminVars();
                return true;
            case 7:
                if (!GameServer.server) {
                    return false;
                }

                this.netInfo.isStopWeather = input.get() == 1;
                this.netInfo.isTrigger = input.get() == 1;
                this.netInfo.isGenerate = input.get() == 1;
                this.netInfo.triggerDuration = input.getFloat();
                this.netInfo.triggerStorm = input.get() == 1;
                this.netInfo.triggerTropical = input.get() == 1;
                this.netInfo.triggerBlizzard = input.get() == 1;
                this.netInfo.generateStrength = input.getFloat();
                this.netInfo.generateFront = input.getInt();
                this.serverReceiveClientChangeWeather();
                return true;
            default:
                return false;
        }
    }

    private void serverReceiveClientChangeAdminVars() {
        if (GameServer.server) {
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)4, null);
            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    private void serverReceiveClientChangeWeather() {
        if (GameServer.server) {
            if (this.netInfo.isStopWeather) {
                this.stopWeatherAndThunder();
            } else if (this.netInfo.isTrigger) {
                this.stopWeatherAndThunder();
                if (this.netInfo.triggerStorm) {
                    this.triggerCustomWeatherStage(3, this.netInfo.triggerDuration);
                } else if (this.netInfo.triggerTropical) {
                    this.triggerCustomWeatherStage(8, this.netInfo.triggerDuration);
                } else if (this.netInfo.triggerBlizzard) {
                    this.triggerCustomWeatherStage(7, this.netInfo.triggerDuration);
                }
            } else if (this.netInfo.isGenerate) {
                this.stopWeatherAndThunder();
                this.triggerCustomWeather(this.netInfo.generateStrength, this.netInfo.generateFront == 0);
            }

            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    public void transmitServerStopWeather() {
        if (GameServer.server) {
            this.stopWeatherAndThunder();
            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    public void transmitServerTriggerStorm(float duration) {
        if (GameServer.server) {
            this.netInfo.triggerDuration = duration;
            this.triggerCustomWeatherStage(3, this.netInfo.triggerDuration);
            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    public void transmitServerTriggerLightning(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        if (GameServer.server) {
            this.thunderStorm.triggerThunderEvent(x, y, doStrike, doLightning, doRumble);
        }
    }

    public void transmitServerStartRain(float intensity) {
        if (GameServer.server) {
            this.precipitationIntensity.setAdminValue(clamp01(intensity));
            this.precipitationIntensity.setEnableAdmin(true);
            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    public void transmitServerStopRain() {
        if (GameServer.server) {
            this.precipitationIntensity.setEnableAdmin(false);
            this.updateOnTick();
            this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)0, null);
        }
    }

    public void transmitRequestAdminVars() {
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)5, null);
    }

    public void transmitClientChangeAdminVars() {
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)6, null);
    }

    public void transmitStopWeather() {
        this.netInfo.reset();
        this.netInfo.isStopWeather = true;
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerStorm(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerStorm = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerTropical(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerTropical = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitTriggerBlizzard(float duration) {
        this.netInfo.reset();
        this.netInfo.isTrigger = true;
        this.netInfo.triggerBlizzard = true;
        this.netInfo.triggerDuration = duration;
        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    public void transmitGenerateWeather(float strength, int front) {
        this.netInfo.reset();
        this.netInfo.isGenerate = true;
        this.netInfo.generateStrength = clamp01(strength);
        this.netInfo.generateFront = front;
        if (this.netInfo.generateFront < 0 || this.netInfo.generateFront > 1) {
            this.netInfo.generateFront = 0;
        }

        this.transmitClimatePacket(ClimateManager.ClimateNetAuth.ClientOnly, (byte)7, null);
    }

    protected float getTimeLerpHours(float cur, float min, float max) {
        return this.getTimeLerpHours(cur, min, max, false);
    }

    protected float getTimeLerpHours(float cur, float min, float max, boolean doClerp) {
        return this.getTimeLerp(clamp(0.0F, 1.0F, cur / 24.0F), clamp(0.0F, 1.0F, min / 24.0F), clamp(0.0F, 1.0F, max / 24.0F), doClerp);
    }

    protected float getTimeLerp(float cur, float min, float max) {
        return this.getTimeLerp(cur, min, max, false);
    }

    protected float getTimeLerp(float cur, float min, float max, boolean doClerp) {
        boolean adjust = min > max;
        if (!adjust) {
            if (!(cur < min) && !(cur > max)) {
                float c = cur - min;
                float len = max - min;
                float mid = len * 0.5F;
                if (c < mid) {
                    return doClerp ? clerp(c / mid, 0.0F, 1.0F) : lerp(c / mid, 0.0F, 1.0F);
                } else {
                    return doClerp ? clerp((c - mid) / mid, 1.0F, 0.0F) : lerp((c - mid) / mid, 1.0F, 0.0F);
                }
            } else {
                return 0.0F;
            }
        } else if (cur < min && cur > max) {
            return 0.0F;
        } else {
            float minoffset = 1.0F - min;
            float c = cur >= min ? cur - min : cur + minoffset;
            float len = max + minoffset;
            float mid = len * 0.5F;
            if (c < mid) {
                return doClerp ? clerp(c / mid, 0.0F, 1.0F) : lerp(c / mid, 0.0F, 1.0F);
            } else {
                return doClerp ? clerp((c - mid) / mid, 1.0F, 0.0F) : lerp((c - mid) / mid, 1.0F, 0.0F);
            }
        }
    }

    public static float clamp01(float val) {
        return clamp(0.0F, 1.0F, val);
    }

    public static float clamp(float min, float max, float val) {
        val = Math.min(max, val);
        return Math.max(min, val);
    }

    public static int clamp(int min, int max, int val) {
        val = Math.min(max, val);
        return Math.max(min, val);
    }

    public static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    public static float clerp(float t, float a, float b) {
        float t2 = (float)(1.0 - Math.cos(t * Math.PI)) / 2.0F;
        return a * (1.0F - t2) + b * t2;
    }

    public static float normalizeRange(float v, float n) {
        return clamp(0.0F, 1.0F, v / n);
    }

    public static float posToPosNegRange(float v) {
        if (v > 0.5F) {
            return (v - 0.5F) * 2.0F;
        } else {
            return v < 0.5F ? -((0.5F - v) * 2.0F) : 0.0F;
        }
    }

    public void execute_Simulation() {
        if (Core.debug) {
            ClimMngrDebug dbg = new ClimMngrDebug();
            int days = 365;
            int simulations = 5000;
            dbg.SimulateDays(365, 5000);
        }
    }

    public void execute_Simulation(int RainModOverride) {
        if (Core.debug) {
            ClimMngrDebug dbg = new ClimMngrDebug();
            dbg.setRainModOverride(RainModOverride);
            int days = 365;
            int simulations = 5000;
            dbg.SimulateDays(365, 5000);
        }
    }

    public void triggerKateBobIntroStorm(int centerX, int centerY, double duration, float strength, float initialProgress, float angle, float initialPuddles) {
        this.triggerKateBobIntroStorm(centerX, centerY, duration, strength, initialProgress, angle, initialPuddles, null);
    }

    public void triggerKateBobIntroStorm(
        int centerX, int centerY, double duration, float strength, float initialProgress, float angle, float initialPuddles, ClimateColorInfo cloudcolor
    ) {
        if (!GameClient.client) {
            this.stopWeatherAndThunder();
            if (this.weatherPeriod.startCreateModdedPeriod(true, strength, angle)) {
                this.weatherPeriod.setKateBobStormProgress(initialProgress);
                this.weatherPeriod.setKateBobStormCoords(centerX, centerY);
                this.weatherPeriod.createAndAddStage(11, duration);
                this.weatherPeriod.createAndAddStage(2, duration / 2.0);
                this.weatherPeriod.createAndAddStage(4, duration / 4.0);
                this.weatherPeriod.endCreateModdedPeriod();
                if (cloudcolor != null) {
                    this.weatherPeriod.setCloudColor(cloudcolor);
                } else {
                    this.weatherPeriod.setCloudColor(this.weatherPeriod.getCloudColorBlueish());
                }

                IsoPuddles.PuddlesFloat pfloat = IsoPuddles.getInstance().getPuddlesFloat(3);
                pfloat.setFinalValue(initialPuddles);
                pfloat = IsoPuddles.getInstance().getPuddlesFloat(1);
                pfloat.setFinalValue(PZMath.clamp_01(initialPuddles * 1.2F));
            }
        }
    }

    public double getSimplexOffsetA() {
        return this.simplexOffsetA;
    }

    public double getSimplexOffsetB() {
        return this.simplexOffsetB;
    }

    public double getSimplexOffsetC() {
        return this.simplexOffsetC;
    }

    public double getSimplexOffsetD() {
        return this.simplexOffsetD;
    }

    public double getWorldAgeHours() {
        return this.worldAgeHours;
    }

    public ClimateValues getClimateValuesCopy() {
        return this.climateValues.getCopy();
    }

    public void CopyClimateValues(ClimateValues copy) {
        this.climateValues.CopyValues(copy);
    }

    public ClimateForecaster getClimateForecaster() {
        return this.climateForecaster;
    }

    public ClimateHistory getClimateHistory() {
        return this.climateHistory;
    }

    public void CalculateWeatherFrontStrength(int year, int month, int day, ClimateManager.AirFront front) {
        GregorianCalendar calendar = new GregorianCalendar(year, month, day, 0, 0);
        calendar.add(5, -3);
        if (this.climateValuesFronts == null) {
            this.climateValuesFronts = this.climateValues.getCopy();
        }

        int TARGET_TYPE = front.type;

        for (int i = 0; i < 4; i++) {
            this.climateValuesFronts.pollDate(calendar);
            float airmass = this.climateValuesFronts.getAirFrontAirmass();
            int TYPE = airmass < 0.0F ? -1 : 1;
            if (TYPE == TARGET_TYPE) {
                front.addDaySample(airmass);
            }

            calendar.add(5, 1);
        }
    }

    public static String getWindAngleString(float angle) {
        for (int i = 0; i < windAngles.length; i++) {
            if (angle < windAngles[i]) {
                return windAngleStr[i];
            }
        }

        return windAngleStr[windAngleStr.length - 1];
    }

    public void sendInitialState(UdpConnection connection) throws IOException {
        if (GameServer.server) {
            if (this.writePacketContents(connection, (byte)0)) {
                PacketTypes.PacketType.ClimateManagerPacket.send(connection);
            } else {
                connection.cancelPacket();
            }
        }
    }

    public boolean isUpdated() {
        return this.lastMinuteStamp != -1L;
    }

    public void Reset() {
        this.lastHourStamp = -1;
        this.lastMinuteStamp = -1L;
        if (this.currentDay != null) {
            this.currentDay.day = this.currentDay.month = this.currentDay.year = -1;
        }
    }

    @UsedFromLua
    public static class AirFront {
        private float days;
        private float maxNoise;
        private float totalNoise;
        private int type = 0;
        private float strength;
        private float tmpNoiseAbs;
        private final float[] noiseCache = new float[2];
        private float noiseCacheValue;
        private float frontWindAngleDegrees;

        public float getDays() {
            return this.days;
        }

        public float getMaxNoise() {
            return this.maxNoise;
        }

        public float getTotalNoise() {
            return this.totalNoise;
        }

        public int getType() {
            return this.type;
        }

        public float getStrength() {
            return this.strength;
        }

        public float getAngleDegrees() {
            return this.frontWindAngleDegrees;
        }

        public AirFront() {
            this.reset();
        }

        public void setFrontType(int type) {
            this.reset();
            this.type = type;
        }

        protected void setFrontWind(float windangledegrees) {
            this.frontWindAngleDegrees = windangledegrees;
        }

        public void setStrength(float str) {
            this.strength = str;
        }

        protected void reset() {
            this.days = 0.0F;
            this.maxNoise = 0.0F;
            this.totalNoise = 0.0F;
            this.type = 0;
            this.strength = 0.0F;
            this.frontWindAngleDegrees = 0.0F;

            for (int i = 0; i < this.noiseCache.length; i++) {
                this.noiseCache[i] = -1.0F;
            }
        }

        public void save(DataOutputStream output) throws IOException {
            output.writeFloat(this.days);
            output.writeFloat(this.maxNoise);
            output.writeFloat(this.totalNoise);
            output.writeInt(this.type);
            output.writeFloat(this.strength);
            output.writeFloat(this.frontWindAngleDegrees);
            output.writeInt(this.noiseCache.length);

            for (int i = 0; i < this.noiseCache.length; i++) {
                output.writeFloat(this.noiseCache[i]);
            }
        }

        public void load(DataInputStream input) throws IOException {
            this.days = input.readFloat();
            this.maxNoise = input.readFloat();
            this.totalNoise = input.readFloat();
            this.type = input.readInt();
            this.strength = input.readFloat();
            this.frontWindAngleDegrees = input.readFloat();
            int len = input.readInt();
            int max = len > this.noiseCache.length ? len : this.noiseCache.length;

            for (int i = 0; i < max; i++) {
                if (i < len) {
                    float val = input.readFloat();
                    if (i < this.noiseCache.length) {
                        this.noiseCache[i] = val;
                    }
                } else if (i < this.noiseCache.length) {
                    this.noiseCache[i] = -1.0F;
                }
            }
        }

        public void addDaySample(float noiseval) {
            this.days++;
            if ((this.type != 1 || !(noiseval <= 0.0F)) && (this.type != -1 || !(noiseval >= 0.0F))) {
                this.tmpNoiseAbs = Math.abs(noiseval);
                if (this.tmpNoiseAbs > this.maxNoise) {
                    this.maxNoise = this.tmpNoiseAbs;
                }

                this.totalNoise = this.totalNoise + this.tmpNoiseAbs;
                this.noiseCacheValue = 0.0F;

                for (int i = this.noiseCache.length - 1; i >= 0; i--) {
                    if (this.noiseCache[i] > this.noiseCacheValue) {
                        this.noiseCacheValue = this.noiseCache[i];
                    }

                    if (i < this.noiseCache.length - 1) {
                        this.noiseCache[i + 1] = this.noiseCache[i];
                    }
                }

                this.noiseCache[0] = this.tmpNoiseAbs;
                if (this.tmpNoiseAbs > this.noiseCacheValue) {
                    this.noiseCacheValue = this.tmpNoiseAbs;
                }

                this.strength = this.noiseCacheValue * 0.75F + this.maxNoise * 0.25F;
            } else {
                this.strength = 0.0F;
            }
        }

        public void copyFrom(ClimateManager.AirFront other) {
            this.days = other.days;
            this.maxNoise = other.maxNoise;
            this.totalNoise = other.totalNoise;
            this.type = other.type;
            this.strength = other.strength;
            this.frontWindAngleDegrees = other.frontWindAngleDegrees;
        }
    }

    @UsedFromLua
    public static class ClimateBool {
        protected boolean internalValue;
        protected boolean finalValue;
        protected boolean isOverride;
        protected boolean override;
        private boolean isModded;
        private boolean moddedValue;
        private boolean isAdminOverride;
        private boolean adminValue;
        private int id;
        private String name;

        public ClimateManager.ClimateBool init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public boolean getInternalValue() {
            return this.internalValue;
        }

        public boolean getOverride() {
            return this.override;
        }

        public void setOverride(boolean b) {
            this.isOverride = true;
            this.override = b;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(boolean b) {
            this.adminValue = b;
        }

        public boolean getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(boolean b) {
            this.moddedValue = b;
        }

        public boolean getModdedValue() {
            return this.moddedValue;
        }

        public void setFinalValue(boolean b) {
            this.finalValue = b;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
            } else if (this.isModded) {
                this.finalValue = this.moddedValue;
            } else {
                this.finalValue = this.isOverride ? this.override : this.internalValue;
            }
        }

        private void writeAdmin(ByteBuffer output) {
            output.put((byte)(this.isAdminOverride ? 1 : 0));
            output.put((byte)(this.adminValue ? 1 : 0));
        }

        private void readAdmin(ByteBuffer input) {
            this.isAdminOverride = input.get() == 1;
            this.adminValue = input.get() == 1;
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            output.writeBoolean(this.adminValue);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue = input.readBoolean();
        }
    }

    @UsedFromLua
    public static class ClimateColor {
        protected ClimateColorInfo internalValue = new ClimateColorInfo();
        protected ClimateColorInfo finalValue = new ClimateColorInfo();
        protected boolean isOverride;
        protected ClimateColorInfo override = new ClimateColorInfo();
        protected float interpolate;
        private boolean isModded;
        private final ClimateColorInfo moddedValue = new ClimateColorInfo();
        private float modInterpolate;
        private boolean isAdminOverride;
        private final ClimateColorInfo adminValue = new ClimateColorInfo();
        private int id;
        private String name;

        public ClimateManager.ClimateColor init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public ClimateColorInfo getInternalValue() {
            return this.internalValue;
        }

        public ClimateColorInfo getOverride() {
            return this.override;
        }

        public float getOverrideInterpolate() {
            return this.interpolate;
        }

        public void setOverride(ClimateColorInfo targ, float inter) {
            this.override.setTo(targ);
            this.interpolate = inter;
            this.isOverride = true;
        }

        public void setOverride(ByteBuffer input, float interp) {
            this.override.read(input);
            this.interpolate = interp;
            this.isOverride = true;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(float r, float g, float b, float a, float r1, float g1, float b1, float a1) {
            this.adminValue.getExterior().r = r;
            this.adminValue.getExterior().g = g;
            this.adminValue.getExterior().b = b;
            this.adminValue.getExterior().a = a;
            this.adminValue.getInterior().r = r1;
            this.adminValue.getInterior().g = g1;
            this.adminValue.getInterior().b = b1;
            this.adminValue.getInterior().a = a1;
        }

        public void setAdminValueExterior(float r, float g, float b, float a) {
            this.adminValue.getExterior().r = r;
            this.adminValue.getExterior().g = g;
            this.adminValue.getExterior().b = b;
            this.adminValue.getExterior().a = a;
        }

        public void setAdminValueInterior(float r, float g, float b, float a) {
            this.adminValue.getInterior().r = r;
            this.adminValue.getInterior().g = g;
            this.adminValue.getInterior().b = b;
            this.adminValue.getInterior().a = a;
        }

        public void setAdminValue(ClimateColorInfo targ) {
            this.adminValue.setTo(targ);
        }

        public ClimateColorInfo getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(ClimateColorInfo targ) {
            this.moddedValue.setTo(targ);
        }

        public ClimateColorInfo getModdedValue() {
            return this.moddedValue;
        }

        public void setModdedInterpolate(float f) {
            this.modInterpolate = ClimateManager.clamp01(f);
        }

        public void setFinalValue(ClimateColorInfo targ) {
            this.finalValue.setTo(targ);
        }

        public ClimateColorInfo getFinalValue() {
            return this.finalValue;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue.setTo(this.adminValue);
            } else {
                if (this.isModded && this.modInterpolate > 0.0F) {
                    this.internalValue.interp(this.moddedValue, this.modInterpolate, this.internalValue);
                }

                if (this.isOverride && this.interpolate > 0.0F) {
                    this.internalValue.interp(this.override, this.interpolate, this.finalValue);
                } else {
                    this.finalValue.setTo(this.internalValue);
                }
            }
        }

        private void writeAdmin(ByteBuffer output) {
            output.put((byte)(this.isAdminOverride ? 1 : 0));
            this.adminValue.write(output);
        }

        private void readAdmin(ByteBuffer input) {
            this.isAdminOverride = input.get() == 1;
            this.adminValue.read(input);
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            this.adminValue.save(output);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue.load(input, worldVersion);
        }
    }

    @UsedFromLua
    public static class ClimateFloat {
        protected float internalValue;
        protected float finalValue;
        protected boolean isOverride;
        protected float override;
        protected boolean isOverrideValue;
        protected float overrideInternal;
        protected float interpolate;
        private boolean isModded;
        private float moddedValue;
        private float modInterpolate;
        private boolean isAdminOverride;
        private float adminValue;
        private float min;
        private float max = 1.0F;
        private int id;
        private String name;

        public ClimateManager.ClimateFloat init(int id, String name) {
            this.id = id;
            this.name = name;
            return this;
        }

        public int getID() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public float getMin() {
            return this.min;
        }

        public float getMax() {
            return this.max;
        }

        public float getInternalValue() {
            return this.internalValue;
        }

        public float getOverride() {
            return this.override;
        }

        public float getOverrideInterpolate() {
            return this.interpolate;
        }

        public void setOverride(float targ, float inter) {
            this.override = targ;
            this.interpolate = inter;
            this.isOverride = true;
        }

        public void setOverrideValue(boolean overrideValue) {
            this.isOverrideValue = overrideValue;
            this.isOverride = overrideValue;
        }

        public void setEnableOverride(boolean b) {
            this.isOverride = b;
        }

        public boolean isEnableOverride() {
            return this.isOverride;
        }

        public void setEnableAdmin(boolean b) {
            this.isAdminOverride = b;
        }

        public boolean isEnableAdmin() {
            return this.isAdminOverride;
        }

        public void setAdminValue(float f) {
            this.adminValue = ClimateManager.clamp(this.min, this.max, f);
        }

        public float getAdminValue() {
            return this.adminValue;
        }

        public void setEnableModded(boolean b) {
            this.isModded = b;
        }

        public void setModdedValue(float f) {
            this.moddedValue = ClimateManager.clamp(this.min, this.max, f);
        }

        public float getModdedValue() {
            return this.moddedValue;
        }

        public void setModdedInterpolate(float f) {
            this.modInterpolate = ClimateManager.clamp01(f);
        }

        public void setFinalValue(float f) {
            this.finalValue = f;
        }

        public float getFinalValue() {
            return this.finalValue;
        }

        private void calculate() {
            if (this.isAdminOverride && !GameClient.client) {
                this.finalValue = this.adminValue;
            } else {
                if (this.isModded && this.modInterpolate > 0.0F) {
                    this.internalValue = ClimateManager.lerp(this.modInterpolate, this.internalValue, this.moddedValue);
                }

                if (!this.isOverride || !(this.interpolate > 0.0F)) {
                    this.finalValue = this.internalValue;
                } else if (this.isOverrideValue) {
                    this.finalValue = ClimateManager.lerp(this.interpolate, this.overrideInternal, this.override);
                } else {
                    this.finalValue = ClimateManager.lerp(this.interpolate, this.internalValue, this.override);
                }
            }
        }

        private void writeAdmin(ByteBuffer output) {
            output.put((byte)(this.isAdminOverride ? 1 : 0));
            output.putFloat(this.adminValue);
        }

        private void readAdmin(ByteBuffer input) {
            this.isAdminOverride = input.get() == 1;
            this.adminValue = input.getFloat();
        }

        private void saveAdmin(DataOutputStream output) throws IOException {
            output.writeBoolean(this.isAdminOverride);
            output.writeFloat(this.adminValue);
        }

        private void loadAdmin(DataInputStream input, int worldVersion) throws IOException {
            this.isAdminOverride = input.readBoolean();
            this.adminValue = input.readFloat();
        }
    }

    /**
     * NETWORKING
     */
    public static enum ClimateNetAuth {
        Denied,
        ClientOnly,
        ServerOnly,
        ClientAndServer;
    }

    private static class ClimateNetInfo {
        public boolean isStopWeather;
        public boolean isTrigger;
        public boolean isGenerate;
        public float triggerDuration;
        public boolean triggerStorm;
        public boolean triggerTropical;
        public boolean triggerBlizzard;
        public float generateStrength;
        public int generateFront;

        private void reset() {
            this.isStopWeather = false;
            this.isTrigger = false;
            this.isGenerate = false;
            this.triggerDuration = 0.0F;
            this.triggerStorm = false;
            this.triggerTropical = false;
            this.triggerBlizzard = false;
            this.generateStrength = 0.0F;
            this.generateFront = 0;
        }
    }

    /**
     * DAY INFO
     */
    @UsedFromLua
    public static class DayInfo {
        public int day;
        public int month;
        public int year;
        public int hour;
        public int minutes;
        public long dateValue;
        public GregorianCalendar calendar;
        public ErosionSeason season;

        public void set(int day, int month, int year) {
            this.calendar = new GregorianCalendar(year, month, day, 0, 0);
            this.dateValue = this.calendar.getTime().getTime();
            this.day = day;
            this.month = month;
            this.year = year;
        }

        public int getDay() {
            return this.day;
        }

        public int getMonth() {
            return this.month;
        }

        public int getYear() {
            return this.year;
        }

        public int getHour() {
            return this.hour;
        }

        public int getMinutes() {
            return this.minutes;
        }

        public long getDateValue() {
            return this.dateValue;
        }

        public ErosionSeason getSeason() {
            return this.season;
        }
    }

    protected static class SeasonColor {
        public static final int WARM = 0;
        public static final int NORMAL = 1;
        public static final int CLOUDY = 2;
        public static final int SUMMER = 0;
        public static final int FALL = 1;
        public static final int WINTER = 2;
        public static final int SPRING = 3;
        private final ClimateColorInfo finalCol = new ClimateColorInfo();
        private final ClimateColorInfo[] tempCol = new ClimateColorInfo[3];
        private final ClimateColorInfo[][] colors = new ClimateColorInfo[3][4];
        private boolean ignoreNormal = true;

        public SeasonColor() {
            for (int j = 0; j < 3; j++) {
                for (int i = 0; i < 4; i++) {
                    this.colors[j][i] = new ClimateColorInfo();
                }

                this.tempCol[j] = new ClimateColorInfo();
            }
        }

        public void setIgnoreNormal(boolean b) {
            this.ignoreNormal = b;
        }

        public ClimateColorInfo getColor(int temperature, int season) {
            return this.colors[temperature][season];
        }

        public void setColorInterior(int temperature, int season, float r, float g, float b, float a) {
            this.colors[temperature][season].getInterior().r = r;
            this.colors[temperature][season].getInterior().g = g;
            this.colors[temperature][season].getInterior().b = b;
            this.colors[temperature][season].getInterior().a = a;
        }

        public void setColorExterior(int temperature, int season, float r, float g, float b, float a) {
            this.colors[temperature][season].getExterior().r = r;
            this.colors[temperature][season].getExterior().g = g;
            this.colors[temperature][season].getExterior().b = b;
            this.colors[temperature][season].getExterior().a = a;
        }

        public ClimateColorInfo update(float temperatureLerp, float seasonLerp, int seasonFrom, int seasonTo) {
            for (int i = 0; i < 3; i++) {
                if (!this.ignoreNormal || i != 1) {
                    this.colors[i][seasonFrom].interp(this.colors[i][seasonTo], seasonLerp, this.tempCol[i]);
                }
            }

            if (!this.ignoreNormal) {
                if (temperatureLerp < 0.5F) {
                    float cf = temperatureLerp * 2.0F;
                    this.tempCol[0].interp(this.tempCol[1], cf, this.finalCol);
                } else {
                    float cf = 1.0F - (temperatureLerp - 0.5F) * 2.0F;
                    this.tempCol[2].interp(this.tempCol[1], cf, this.finalCol);
                }
            } else {
                this.tempCol[0].interp(this.tempCol[2], temperatureLerp, this.finalCol);
            }

            return this.finalCol;
        }
    }
}
