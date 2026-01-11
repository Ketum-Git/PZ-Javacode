// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.erosion.ErosionMain;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.SaveBufferMap;
import zombie.iso.SliceY;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;
import zombie.radio.ZomboidRadio;
import zombie.ui.SpeedControls;
import zombie.ui.UIManager;
import zombie.util.ByteBufferOutputStream;
import zombie.util.ByteBufferPooledObject;
import zombie.util.PZCalendar;

/**
 * Tracks both in-game time and real world time. This class is very old and so has a lot of random/deprecated functionality.
 */
@UsedFromLua
public final class GameTime {
    public static final float MinutesPerHour = 60.0F;
    public static final float SecondsPerHour = 3600.0F;
    public static final float SecondsPerMinute = 60.0F;
    public static final float MULTIPLIER = 0.8F;
    /**
     * Because of how Kahlua exposes static fields, when accessed from Lua, this will return a stale GameTime object that does not hold the correct game state. Lua mods should always use getGameTime() or GameTime.getInstance() instead of this field.
     */
    public static GameTime instance = new GameTime();
    private static long serverTimeShift;
    private static boolean serverTimeShiftIsSet;
    private static boolean isUTest;
    private final float minutesPerDayStart = 30.0F;
    private final boolean rainingToday = true;
    private final float[] gunFireTimes = new float[5];
    public float timeOfDay = 9.0F;
    public int nightsSurvived;
    public PZCalendar calender;
    public float fpsMultiplier = 1.0F;
    public float moon;
    public float serverTimeOfDay;
    public float serverLastTimeOfDay;
    public int serverNewDays;
    public float lightSourceUpdate;
    public float multiplierBias = 1.0F;
    public float lastLastTimeOfDay;
    public float perObjectMultiplier = 1.0F;
    private int helicopterTime1Start;
    private int helicopterTime1End;
    private int helicopterDay1;
    private float ambient = 0.9F;
    private float ambientMax = 1.0F;
    private float ambientMin = 0.24F;
    private int day = 22;
    private int startDay = 22;
    private float maxZombieCountStart = 750.0F;
    private float minZombieCountStart = 750.0F;
    private float maxZombieCount = 750.0F;
    private float minZombieCount = 750.0F;
    private int month = 7;
    private int startMonth = 7;
    private float startTimeOfDay = 9.0F;
    private float viewDistMax = 42.0F;
    private float viewDistMin = 19.0F;
    private int year = 2012;
    private int startYear = 2012;
    private double hoursSurvived;
    private float minutesPerDay = 30.0F;
    private float lastTimeOfDay;
    private int targetZombies = (int)this.minZombieCountStart;
    private boolean gunFireEventToday;
    private int numGunFireEvents = 1;
    private long lastClockSync;
    private KahluaTable table;
    private int minutesMod = -1;
    private boolean thunderDay = true;
    private boolean randomAmbientToday = true;
    private float multiplier = 1.0F;
    private int dusk = 3;
    private int dawn = 12;
    private float nightMin;
    private float nightMax = 1.0F;
    private long minutesStamp;
    private long previousMinuteStamp;
    int lastSkyLight = -100;

    public GameTime() {
        serverTimeShift = 0L;
        serverTimeShiftIsSet = false;
    }

    public static GameTime getInstance() {
        return instance;
    }

    public static void setInstance(GameTime aInstance) {
        instance = aInstance;
    }

    public static void syncServerTime(long timeClientSend, long timeServer, long timeClientReceive) {
        long localPing = timeClientReceive - timeClientSend;
        long localServerTimeShift = timeServer - timeClientReceive + localPing / 2L;
        long serverTimeShiftLast = serverTimeShift;
        if (!serverTimeShiftIsSet) {
            serverTimeShift = localServerTimeShift;
        } else {
            serverTimeShift = serverTimeShift + (localServerTimeShift - serverTimeShift) / 100L;
        }

        long serverTimeuQality = 10000000L;
        if (Math.abs(serverTimeShift - serverTimeShiftLast) > 10000000L) {
            INetworkPacket.send(PacketTypes.PacketType.TimeSync);
        } else {
            serverTimeShiftIsSet = true;
        }
    }

    public static long getServerTime() {
        if (isUTest) {
            return System.nanoTime() + serverTimeShift;
        } else if (GameServer.server) {
            return System.nanoTime();
        } else if (GameClient.client) {
            return !serverTimeShiftIsSet ? 0L : System.nanoTime() + serverTimeShift;
        } else {
            return 0L;
        }
    }

    public static long getServerTimeMills() {
        return TimeUnit.NANOSECONDS.toMillis(getServerTime());
    }

    public static boolean getServerTimeShiftIsSet() {
        return serverTimeShiftIsSet;
    }

    public static void setServerTimeShift(long tshift) {
        isUTest = true;
        serverTimeShift = tshift;
        serverTimeShiftIsSet = true;
    }

    public static boolean isGamePaused() {
        if (GameServer.server) {
            return GameServer.Players.isEmpty() && ServerOptions.instance.pauseEmpty.getValue();
        } else if (GameClient.client) {
            return GameClient.IsClientPaused();
        } else {
            SpeedControls speedControls = UIManager.getSpeedControls();
            return speedControls != null && speedControls.getCurrentGameSpeed() == 0;
        }
    }

    /**
     * Number of real seconds since the last tick.
     */
    public float getRealworldSecondsSinceLastUpdate() {
        return 0.016666668F * this.fpsMultiplier;
    }

    /**
     * Number of real world seconds since the last tick, multiplied by game speed.
     */
    public float getMultipliedSecondsSinceLastUpdate() {
        return 0.016666668F * this.getUnmoddedMultiplier();
    }

    /**
     * Number of in-game seconds passed since the last tick.
     */
    public float getGameWorldSecondsSinceLastUpdate() {
        float dif = 1440.0F / this.getMinutesPerDay();
        return this.getTimeDelta() * dif;
    }

    /**
     * Returns the number of days in a month.
     * 
     * @param year Year of the month. Required to account for leap years.
     * @param month 0 indexed month of the year.
     * @return Number of days in the month.
     */
    public int daysInMonth(int year, int month) {
        if (this.calender == null) {
            this.updateCalendar(this.getYear(), this.getMonth(), this.getDay(), (int)this.getTimeOfDay(), this.getMinutes());
        }

        int[] daysInMonths = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        daysInMonths[1] += this.getCalender().isLeapYear(year) ? 1 : 0;
        return daysInMonths[month];
    }

    /**
     * Returns the time survived string to show on death for a player.
     * 
     * @param playerObj Player to get the string for.
     * @return Time survived string.
     */
    public String getDeathString(IsoPlayer playerObj) {
        return Translator.getText("IGUI_Gametime_SurvivedFor", this.getTimeSurvived(playerObj));
    }

    /**
     * The number of full days survived by the current local player who has survived the longest modulo 30.
     * @return Highest number of days survived by a current local player modulo 30.
     */
    public int getDaysSurvived() {
        float hours = 0.0F;

        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null) {
                hours = Math.max(hours, (float)player.getHoursSurvived());
            }
        }

        int days = (int)hours / 24;
        return days % 30;
    }

    /**
     * Gets a string that describes how long a player has survived for.
     * 
     * @param playerObj Player to get the string for.
     * @return String describing how long the player has survived.
     */
    public String getTimeSurvived(IsoPlayer playerObj) {
        String total = "";
        float hours = (float)playerObj.getHoursSurvived();
        Integer hoursLeft = (int)hours % 24;
        Integer days = (int)hours / 24;
        Integer months = days / 30;
        days = days % 30;
        Integer years = months / 12;
        months = months % 12;
        String dayString = Translator.getText("IGUI_Gametime_day");
        String yearString = Translator.getText("IGUI_Gametime_year");
        String hourString = Translator.getText("IGUI_Gametime_hour");
        String monthString = Translator.getText("IGUI_Gametime_month");
        if (years != 0) {
            if (years > 1) {
                yearString = Translator.getText("IGUI_Gametime_years");
            }

            total = total + years + " " + yearString;
        }

        if (months != 0) {
            if (months > 1) {
                monthString = Translator.getText("IGUI_Gametime_months");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + months + " " + monthString;
        }

        if (days != 0) {
            if (days > 1) {
                dayString = Translator.getText("IGUI_Gametime_days");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + days + " " + dayString;
        }

        if (hoursLeft != 0) {
            if (hoursLeft > 1) {
                hourString = Translator.getText("IGUI_Gametime_hours");
            }

            if (!total.isEmpty()) {
                total = total + ", ";
            }

            total = total + hoursLeft + " " + hourString;
        }

        if (total.trim().isEmpty()) {
            int minutes = (int)(hours * 60.0F);
            int seconds = (int)(hours * 60.0F * 60.0F) - minutes * 60;
            total = minutes + " " + Translator.getText("IGUI_Gametime_minutes") + ", " + seconds + " " + Translator.getText("IGUI_Gametime_secondes");
        }

        return total;
    }

    /**
     * Returns a string describing how many zombies a player has killed.
     * 
     * @param playerObj Player to get the string for.
     * @return String describing how many zombies the player has killed.
     */
    public String getZombieKilledText(IsoPlayer playerObj) {
        int kills = playerObj.getZombieKills();
        if (kills == 0 || kills > 1) {
            return Translator.getText("IGUI_Gametime_zombiesCount", kills);
        } else {
            return kills == 1 ? Translator.getText("IGUI_Gametime_zombieCount", kills) : null;
        }
    }

    /**
     * String describing the current game mode.
     */
    public String getGameModeText() {
        String mode = Translator.getTextOrNull("IGUI_Gametime_" + Core.gameMode);
        if (mode == null) {
            mode = Core.gameMode;
        }

        String s = Translator.getTextOrNull("IGUI_Gametime_GameMode", mode);
        if (s == null) {
            s = "Game mode: " + mode;
        }

        if (Core.debug) {
            s = s + " (DEBUG)";
        }

        return s;
    }

    public void init() {
        this.setDay(this.getStartDay());
        this.setTimeOfDay(this.getStartTimeOfDay());
        this.setMonth(this.getStartMonth());
        this.setYear(this.getStartYear());
        if (SandboxOptions.instance.helicopter.getValue() != 1) {
            this.helicopterDay1 = Rand.Next(6, 10);
            this.helicopterTime1Start = Rand.Next(9, 19);
            this.helicopterTime1End = this.helicopterTime1Start + Rand.Next(4) + 1;
        }

        this.setMinutesStamp();
    }

    /**
     * Interpolates between two values by a given amount.
     * 
     * @param start Value to interpolation from.
     * @param end Value to interpolate to.
     * @param delta 0-1 amount to interpolate between the two values.
     * @return Interpolated value.
     */
    public float Lerp(float start, float end, float delta) {
        if (delta < 0.0F) {
            delta = 0.0F;
        }

        if (delta >= 1.0F) {
            delta = 1.0F;
        }

        float amount = end - start;
        float result = amount * delta;
        return start + result;
    }

    /**
     * Removes a specific number of zombies from the world.
     * 
     * @param i Number of zombies to remove.
     */
    public void RemoveZombiesIndiscriminate(int i) {
        if (i != 0) {
            for (int n = 0; n < IsoWorld.instance.currentCell.getZombieList().size(); n++) {
                IsoZombie zombie = IsoWorld.instance.currentCell.getZombieList().get(0);
                IsoWorld.instance.currentCell.getZombieList().remove(n);
                IsoWorld.instance.currentCell.getRemoveList().add(zombie);
                zombie.getCurrentSquare().getMovingObjects().remove(zombie);
                n--;
                if (--i == 0 || IsoWorld.instance.currentCell.getZombieList().isEmpty()) {
                    return;
                }
            }
        }
    }

    /**
     * Interpolates between two values based on the current time of day.
     * 
     * @param startVal Value to interpolate from.
     * @param endVal Value to interpoalte to.
     * @param startTime Time of day in hours to start interpolation. If the current time is before this, startVal is returned.
     * @param endTime Time of day in hours to end interpolation. If the current time is after this, endVal is returned. If this is less than startTime, it is considered a time in the next day.
     * @return Interpolated value based on the current time.
     */
    public float TimeLerp(float startVal, float endVal, float startTime, float endTime) {
        float TimeOfDay = getInstance().getTimeOfDay();
        if (endTime < startTime) {
            endTime += 24.0F;
        }

        boolean bReverse = false;
        if (TimeOfDay > endTime && TimeOfDay > startTime || TimeOfDay < endTime && TimeOfDay < startTime) {
            startTime += 24.0F;
            bReverse = true;
            startTime = endTime;
            endTime = startTime;
            if (TimeOfDay < startTime) {
                TimeOfDay += 24.0F;
            }
        }

        float dist = endTime - startTime;
        float current = TimeOfDay - startTime;
        float delta = 0.0F;
        if (current > dist) {
            delta = 1.0F;
        }

        if (current < dist && current > 0.0F) {
            delta = current / dist;
        }

        if (bReverse) {
            delta = 1.0F - delta;
        }

        float signval = 0.0F;
        delta = (delta - 0.5F) * 2.0F;
        if (delta < 0.0) {
            signval = -1.0F;
        } else {
            signval = 1.0F;
        }

        delta = Math.abs(delta);
        delta = 1.0F - delta;
        delta = (float)Math.pow(delta, 8.0);
        delta = 1.0F - delta;
        delta *= signval;
        delta = delta * 0.5F + 0.5F;
        return this.Lerp(startVal, endVal, delta);
    }

    /**
     * Delta between the default and current day length (as configured in the sandbox options). When using a time delta, multiply by this as well to make the value increase at a fixed game-time rate rather than real time.
     * @return The default day length is considered by this method to be 30 minutes, so a 0.33 delta is expected on default settings, not 1.
     */
    public float getDeltaMinutesPerDay() {
        return 30.0F / this.minutesPerDay;
    }

    /**
     * @deprecated
     */
    public float getNightMin() {
        return 1.0F - this.nightMin;
    }

    /**
     * @deprecated
     */
    public void setNightMin(float min) {
        this.nightMin = 1.0F - min;
    }

    /**
     * @deprecated
     */
    public float getNightMax() {
        return 1.0F - this.nightMax;
    }

    /**
     * @deprecated
     */
    public void setNightMax(float max) {
        this.nightMax = 1.0F - max;
    }

    public int getMinutes() {
        return (int)((this.getTimeOfDay() - (int)this.getTimeOfDay()) * 60.0F);
    }

    /**
     * @deprecated
     */
    public void setMoon(float moon) {
        this.moon = moon;
    }

    public void update(boolean bSleeping) {
        long ms = System.currentTimeMillis();
        int metaSandbox = 9000;
        if (SandboxOptions.instance.metaEvent.getValue() == 1) {
            metaSandbox = -1;
        }

        if (SandboxOptions.instance.metaEvent.getValue() == 3) {
            metaSandbox = 6000;
        }

        if (!GameClient.client && this.randomAmbientToday && metaSandbox != -1 && Rand.Next(Rand.AdjustForFramerate(metaSandbox)) == 0 && !isGamePaused()) {
            AmbientStreamManager.instance.addRandomAmbient();
            this.randomAmbientToday = SandboxOptions.instance.metaEvent.getValue() == 3 && Rand.Next(3) == 0;
        }

        if (GameServer.server && UIManager.getSpeedControls() != null) {
            UIManager.getSpeedControls().SetCurrentGameSpeed(1);
        }

        if (GameServer.server || !GameClient.client) {
            if (this.gunFireEventToday) {
                for (int n = 0; n < this.numGunFireEvents; n++) {
                    if (this.timeOfDay > this.gunFireTimes[n] && this.lastLastTimeOfDay < this.gunFireTimes[n]) {
                        AmbientStreamManager.instance.doGunEvent();
                    }
                }
            }

            if (this.nightsSurvived == this.helicopterDay1
                && this.timeOfDay > this.helicopterTime1Start
                && this.timeOfDay < this.helicopterTime1End
                && !IsoWorld.instance.helicopter.isActive()
                && Rand.Next((int)(800.0F * this.getInvMultiplier())) == 0) {
                this.helicopterTime1Start = (int)(this.helicopterTime1Start + 0.5F);
                IsoWorld.instance.helicopter.pickRandomTarget();
            }

            if (this.nightsSurvived > this.helicopterDay1
                && (SandboxOptions.instance.helicopter.getValue() == 3 || SandboxOptions.instance.helicopter.getValue() == 4)) {
                if (SandboxOptions.instance.helicopter.getValue() == 3) {
                    this.helicopterDay1 = this.nightsSurvived + Rand.Next(10, 16);
                }

                if (SandboxOptions.instance.helicopter.getValue() == 4) {
                    this.helicopterDay1 = this.nightsSurvived + Rand.Next(6, 10);
                }

                this.helicopterTime1Start = Rand.Next(9, 19);
                this.helicopterTime1End = this.helicopterTime1Start + Rand.Next(4) + 1;
            }
        }

        int previousHour = this.getHour();
        this.updateCalendar(
            this.getYear(), this.getMonth(), this.getDay(), (int)this.getTimeOfDay(), (int)((this.getTimeOfDay() - (int)this.getTimeOfDay()) * 60.0F)
        );
        float lastTimeOfDay = this.getTimeOfDay();
        if (!isGamePaused()) {
            float time = 1.0F / this.getMinutesPerDay() / 60.0F * this.getMultiplier() / 2.0F;
            if (Core.lastStand) {
                time = 1.0F / this.getMinutesPerDay() / 60.0F * this.getUnmoddedMultiplier() / 2.0F;
            }

            if (DebugOptions.instance.freezeTimeOfDay.getValue()) {
                time = 0.0F;
            }

            this.setTimeOfDay(this.getTimeOfDay() + time);
            if (!GameServer.server) {
                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null && player.isAlive()) {
                        player.setHoursSurvived(player.getHoursSurvived() + time);
                    }
                }
            }

            if (GameServer.server) {
                ArrayList<IsoPlayer> players = GameServer.getPlayers();

                for (int i1 = 0; i1 < players.size(); i1++) {
                    IsoPlayer player = players.get(i1);
                    player.setHoursSurvived(player.getHoursSurvived() + time);
                }
            }

            if (GameClient.client) {
                ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();

                for (int i1 = 0; i1 < players.size(); i1++) {
                    IsoPlayer player = players.get(i1);
                    if (player != null && !player.isDead() && !player.isLocalPlayer()) {
                        player.setHoursSurvived(player.getHoursSurvived() + time);
                    }
                }
            }

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                IsoPlayer player = IsoPlayer.players[pn];
                if (player != null) {
                    if (player.isAsleep()) {
                        player.setAsleepTime(player.getAsleepTime() + time);
                        SleepingEvent.instance.update(player);
                    } else {
                        player.setAsleepTime(0.0F);
                    }
                }
            }
        }

        if (!GameClient.client && lastTimeOfDay <= 7.0F && this.getTimeOfDay() > 7.0F) {
            this.setNightsSurvived(this.getNightsSurvived() + 1);
            this.doMetaEvents();
        }

        if (GameClient.client) {
            if (this.getTimeOfDay() >= 24.0F) {
                this.setTimeOfDay(this.getTimeOfDay() - 24.0F);
            }

            while (this.serverNewDays > 0) {
                this.serverNewDays--;
                this.setDay(this.getDay() + 1);
                if (this.getDay() >= this.daysInMonth(this.getYear(), this.getMonth())) {
                    this.setDay(0);
                    this.setMonth(this.getMonth() + 1);
                    if (this.getMonth() >= 12) {
                        this.setMonth(0);
                        this.setYear(this.getYear() + 1);
                    }
                }

                this.updateCalendar(this.getYear(), this.getMonth(), this.getDay(), (int)this.getTimeOfDay(), this.getMinutes());
                LuaEventManager.triggerEvent("EveryDays");
            }
        } else if (this.getTimeOfDay() >= 24.0F) {
            this.setTimeOfDay(this.getTimeOfDay() - 24.0F);
            this.setDay(this.getDay() + 1);
            if (this.getDay() >= this.daysInMonth(this.getYear(), this.getMonth())) {
                this.setDay(0);
                this.setMonth(this.getMonth() + 1);
                if (this.getMonth() >= 12) {
                    this.setMonth(0);
                    this.setYear(this.getYear() + 1);
                }
            }

            this.updateCalendar(this.getYear(), this.getMonth(), this.getDay(), (int)this.getTimeOfDay(), this.getMinutes());
            LuaEventManager.triggerEvent("EveryDays");
            if (GameServer.server) {
                GameServer.syncClock();
                this.lastClockSync = ms;
            }
        }

        float moonMod = this.moon * 20.0F;
        if (!ClimateManager.getInstance().getThunderStorm().isModifyingNight()) {
            this.setAmbient(this.TimeLerp(this.getAmbientMin(), this.getAmbientMax(), this.getDusk(), this.getDawn()));
        }

        if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
            this.setNightTint(0.0F);
        }

        this.setMinutesStamp();
        if (this.getHour() != previousHour) {
            LuaEventManager.triggerEvent("EveryHours");
        }

        if (GameServer.server && this.getHour() < previousHour) {
            ConnectionQueueStatistic.getInstance().zombiesKilledByFireToday.clear();
            ConnectionQueueStatistic.getInstance().zombiesKilledToday.clear();
            ConnectionQueueStatistic.getInstance().zombifiedPlayersToday.clear();
            ConnectionQueueStatistic.getInstance().playersKilledByFireToday.clear();
            ConnectionQueueStatistic.getInstance().playersKilledByZombieToday.clear();
            ConnectionQueueStatistic.getInstance().playersKilledByPlayerToday.clear();
            ConnectionQueueStatistic.getInstance().burnedCorpsesToday.clear();
        }

        int now = (int)((this.getTimeOfDay() - (int)this.getTimeOfDay()) * 60.0F);
        if (now / 10 != this.minutesMod) {
            IsoPlayer[] players = IsoPlayer.players;

            for (int i = 0; i < players.length; i++) {
                IsoPlayer player = players[i];
                if (player != null) {
                    player.dirtyRecalcGridStackTime = 1.0F;
                }
            }

            ErosionMain.EveryTenMinutes();
            ClimateManager.getInstance().updateEveryTenMins();
            getInstance().updateRoomLight();
            LuaEventManager.triggerEvent("EveryTenMinutes");
            this.minutesMod = now / 10;
            ZomboidRadio.getInstance().UpdateScripts(this.getHour(), now);
        }

        if (this.previousMinuteStamp != this.minutesStamp) {
            LuaEventManager.triggerEvent("EveryOneMinute");
            this.previousMinuteStamp = this.minutesStamp;
        }

        if (GameServer.server && (ms - this.lastClockSync > 10000L || GameServer.fastForward)) {
            GameServer.syncClock();
            this.lastClockSync = ms;
        }
    }

    private void updateRoomLight() {
    }

    private void setMinutesStamp() {
        this.minutesStamp = (long)this.getWorldAgeHours() * 60L + this.getMinutes();
    }

    /**
     * Number of minutes since the world was created. Has the same inaccuracy as getWorldAgeHours().
     * @return Number of minutes since the world was created.
     */
    public long getMinutesStamp() {
        return this.minutesStamp;
    }

    /**
     * @deprecated
     */
    public boolean getThunderStorm() {
        return ClimateManager.getInstance().getIsThunderStorming();
    }

    private void doMetaEvents() {
        int metaSandbox = 3;
        if (SandboxOptions.instance.metaEvent.getValue() == 1) {
            metaSandbox = -1;
        }

        if (SandboxOptions.instance.metaEvent.getValue() == 3) {
            metaSandbox = 2;
        }

        this.gunFireEventToday = metaSandbox != -1 && Rand.Next(metaSandbox) == 0;
        if (this.gunFireEventToday) {
            this.numGunFireEvents = 1;

            for (int n = 0; n < this.numGunFireEvents; n++) {
                this.gunFireTimes[n] = Rand.Next(18000) / 1000.0F + 7.0F;
            }
        }

        this.randomAmbientToday = true;
    }

    /**
     * @return the Ambient
     */
    @Deprecated
    public float getAmbient() {
        return ClimateManager.getInstance().getAmbient();
    }

    public int getSkyLightLevel() {
        RenderSettings.PlayerRenderSettings aa = RenderSettings.getInstance().getPlayerSettings(IsoPlayer.getPlayerIndex());
        Color e = ClimateManager.getInstance().getGlobalLight().getExterior();
        float b = aa.getBmod();
        float g = aa.getGmod();
        float r = aa.getRmod();
        b *= 2.0F;
        g *= 2.0F;
        r *= 2.0F;
        r = PZMath.clamp(r * aa.getAmbient(), 0.0F, 1.0F);
        g = PZMath.clamp(g * aa.getAmbient(), 0.0F, 1.0F);
        b = PZMath.clamp(b * aa.getAmbient(), 0.0F, 1.0F);
        int a = (int)(Math.min(1.0F, b) * 255.0F) | (int)(Math.min(1.0F, g) * 255.0F) << 8 | (int)(Math.min(1.0F, r) * 255.0F) << 16;
        if (DebugOptions.instance.fboRenderChunk.forceSkyLightLevel.getValue()) {
            a = 15000000;
        }

        if (a != this.lastSkyLight) {
            LightingJNI.doInvalidateGlobalLights(IsoPlayer.getPlayerIndex());
            this.lastSkyLight = a;
        }

        return a;
    }

    /**
     * 
     * @param Ambient the Ambient to set
     * @deprecated
     */
    public void setAmbient(float Ambient) {
        this.ambient = Ambient;
    }

    /**
     * @return the AmbientMax
     * @deprecated
     */
    public float getAmbientMax() {
        return this.ambientMax;
    }

    /**
     * 
     * @param AmbientMax the AmbientMax to set
     * @deprecated
     */
    public void setAmbientMax(float AmbientMax) {
        AmbientMax = Math.min(1.0F, AmbientMax);
        AmbientMax = Math.max(0.0F, AmbientMax);
        this.ambientMax = AmbientMax;
    }

    /**
     * @return the AmbientMin
     * @deprecated
     */
    public float getAmbientMin() {
        return this.ambientMin;
    }

    /**
     * 
     * @param AmbientMin the AmbientMin to set
     * @deprecated
     */
    public void setAmbientMin(float AmbientMin) {
        AmbientMin = Math.min(1.0F, AmbientMin);
        AmbientMin = Math.max(0.0F, AmbientMin);
        this.ambientMin = AmbientMin;
    }

    /**
     * Current day of the month in the game world.
     * @return 0 indexed day of the month.
     */
    public int getDay() {
        return this.day;
    }

    /**
     * Current day of the month in the game world.
     * 
     * @param Day 0 indexed day of the month.
     */
    public void setDay(int Day) {
        this.day = Day;
    }

    /**
     * Current day of the month in the game world, plus 1.
     * @return 1 indexed day of the month.
     */
    public int getDayPlusOne() {
        return this.day + 1;
    }

    /**
     * Day of the month the game started on as defined by sandbox options. The value will change if sandbox options are changed, so getNightsSurvived() or getWorldAgeHours() should be used instead to determine the age of the world.
     * @return 0 indexed day of the month the game started on.
     */
    public int getStartDay() {
        return this.startDay;
    }

    /**
     * Day of the month the game started on as defined by sandbox options. Changing this does not affect the age of the world.
     * 
     * @param StartDay 0 indexed day of the month the game started on.
     */
    public void setStartDay(int StartDay) {
        this.startDay = StartDay;
    }

    /**
     * @return the MaxZombieCountStart
     * @deprecated
     */
    public float getMaxZombieCountStart() {
        return 0.0F;
    }

    /**
     * 
     * @param MaxZombieCountStart the MaxZombieCountStart to set
     * @deprecated
     */
    public void setMaxZombieCountStart(float MaxZombieCountStart) {
        this.maxZombieCountStart = MaxZombieCountStart;
    }

    /**
     * @return the MinZombieCountStart
     * @deprecated
     */
    public float getMinZombieCountStart() {
        return 0.0F;
    }

    /**
     * 
     * @param MinZombieCountStart the MinZombieCountStart to set
     * @deprecated
     */
    public void setMinZombieCountStart(float MinZombieCountStart) {
        this.minZombieCountStart = MinZombieCountStart;
    }

    /**
     * @return the MaxZombieCount
     * @deprecated
     */
    public float getMaxZombieCount() {
        return this.maxZombieCount;
    }

    /**
     * 
     * @param MaxZombieCount the MaxZombieCount to set
     * @deprecated
     */
    public void setMaxZombieCount(float MaxZombieCount) {
        this.maxZombieCount = MaxZombieCount;
    }

    /**
     * @return the MinZombieCount
     * @deprecated
     */
    public float getMinZombieCount() {
        return this.minZombieCount;
    }

    /**
     * 
     * @param MinZombieCount the MinZombieCount to set
     * @deprecated
     */
    public void setMinZombieCount(float MinZombieCount) {
        this.minZombieCount = MinZombieCount;
    }

    /**
     * Current month of the year in the game world.
     * @return 0 indexed month of the year.
     */
    public int getMonth() {
        return this.month;
    }

    /**
     * Current month of the year in the game world.
     * 
     * @param Month 0 indexed month of the year.
     */
    public void setMonth(int Month) {
        this.month = Month;
    }

    /**
     * Month of the year the game started on as defined by sandbox options. The value will change if sandbox options are changed, so getNightsSurvived() or getWorldAgeHours() should be used instead to determine the age of the world.
     * @return 0 indexed month of the year the game started on.
     */
    public int getStartMonth() {
        return this.startMonth;
    }

    /**
     * Month of the year the game started on as defined by sandbox options. Changing this does not affect the age of the world.
     * 
     * @param StartMonth 0 indexed month of the year the game started on.
     */
    public void setStartMonth(int StartMonth) {
        this.startMonth = StartMonth;
    }

    /**
     * @return the NightTint
     * @deprecated
     */
    public float getNightTint() {
        return PerformanceSettings.fboRenderChunk ? 0.0F : ClimateManager.getInstance().getNightStrength();
    }

    /**
     * 
     * @param NightTint the NightTint to set
     * @deprecated
     */
    private void setNightTint(float NightTint) {
    }

    /**
     * @return the NightTint
     * @deprecated
     */
    public float getNight() {
        return ClimateManager.getInstance().getNightStrength();
    }

    /**
     * 
     * @param NightTint the NightTint to set
     * @deprecated
     */
    private void setNight(float NightTint) {
    }

    /**
     * @return the TimeOfDay
     */
    public float getTimeOfDay() {
        return this.timeOfDay;
    }

    /**
     * 
     * @param TimeOfDay the TimeOfDay to set
     */
    public void setTimeOfDay(float TimeOfDay) {
        this.timeOfDay = TimeOfDay;
    }

    /**
     * Time of day the game started on as defined by sandbox options. The value will change if sandbox options are changed, so getNightsSurvived() or getWorldAgeHours() should be used instead to determine the age of the world.
     * @return The time of day in hours the game started at.
     */
    public float getStartTimeOfDay() {
        return this.startTimeOfDay;
    }

    /**
     * Time of day the game started on as defined by sandbox options. The value will change if sandbox options are changed, so getNightsSurvived() or getWorldAgeHours() should be used instead to determine the age of the world. Changing this does not affect the age of the world.
     * 
     * @param StartTimeOfDay The time of day in hours the game started at.
     */
    public void setStartTimeOfDay(float StartTimeOfDay) {
        this.startTimeOfDay = StartTimeOfDay;
    }

    /**
     * @return the ViewDist
     */
    public float getViewDist() {
        return ClimateManager.getInstance().getViewDistance();
    }

    /**
     * @return the ViewDistMax
     */
    public float getViewDistMax() {
        return this.viewDistMax;
    }

    /**
     * 
     * @param ViewDistMax the ViewDistMax to set
     */
    public void setViewDistMax(float ViewDistMax) {
        this.viewDistMax = ViewDistMax;
    }

    /**
     * @return the ViewDistMin
     * @deprecated
     */
    public float getViewDistMin() {
        return this.viewDistMin;
    }

    /**
     * 
     * @param ViewDistMin the ViewDistMin to set
     * @deprecated
     */
    public void setViewDistMin(float ViewDistMin) {
        this.viewDistMin = ViewDistMin;
    }

    /**
     * Current year in the game world.
     * @return Current year in the game world.
     */
    public int getYear() {
        return this.year;
    }

    /**
     * Current year in the game world.
     */
    public void setYear(int Year) {
        this.year = Year;
    }

    /**
     * Year the game started on.
     * @return Year the game started on.
     */
    public int getStartYear() {
        return this.startYear;
    }

    /**
     * Year the game started on. Changing this does not affect the age of the world.
     * 
     * @param StartYear Year the game started on.
     */
    public void setStartYear(int StartYear) {
        this.startYear = StartYear;
    }

    /**
     * Gets the number of nights that have passed since the save was created. 7am is considered the end of a night.
     * @return Number of nights since game start.
     */
    public int getNightsSurvived() {
        return this.nightsSurvived;
    }

    /**
     * Number of nights since the game began. A night is survived when the time passes 7am.
     * 
     * @param NightsSurvived the NightsSurvived to set
     */
    public void setNightsSurvived(int NightsSurvived) {
        this.nightsSurvived = NightsSurvived;
    }

    public double getWorldAgeDaysSinceBegin() {
        return (float)(this.getWorldAgeHours() / 24.0 + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30);
    }

    /**
     * Gets the age of the world from the start of the game in hours. The value can be slightly off from the true value depending on game settings, as it considers every 7am passing to be a 24 hour period, however the game does not by default start at 7am. The true number of hours can be calculated by subtracting (getStartTimeOfDay() - 7). However, the uncorrected value is still suitable as a timestamp, as the offset is consistent.
     * @return Age of the world in hours.
     */
    public double getWorldAgeHours() {
        float elapsedHours = this.getNightsSurvived() * 24.0F;
        if (this.getTimeOfDay() >= 7.0F) {
            elapsedHours += this.getTimeOfDay() - 7.0F;
        } else {
            elapsedHours += this.getTimeOfDay() + 17.0F;
        }

        return elapsedHours;
    }

    /**
     * @return the HoursSurvived
     * @deprecated
     */
    public double getHoursSurvived() {
        DebugLog.log("GameTime.getHoursSurvived() has no meaning, use IsoPlayer.getHourSurvived() instead");
        return this.hoursSurvived;
    }

    /**
     * 
     * @param HoursSurvived the HoursSurvived to set
     * @deprecated
     */
    public void setHoursSurvived(double HoursSurvived) {
        DebugLog.log("GameTime.getHoursSurvived() has no meaning, use IsoPlayer.getHourSurvived() instead");
        this.hoursSurvived = HoursSurvived;
    }

    public int getHour() {
        double sec = Math.floor(this.getTimeOfDay() * 3600.0F);
        return (int)Math.floor(sec / 3600.0);
    }

    /**
     * @return the Calender
     */
    public PZCalendar getCalender() {
        this.updateCalendar(
            this.getYear(), this.getMonth(), this.getDay(), (int)this.getTimeOfDay(), (int)((this.getTimeOfDay() - (int)this.getTimeOfDay()) * 60.0F)
        );
        return this.calender;
    }

    /**
     * 
     * @param Calender the Calender to set
     */
    public void setCalender(PZCalendar Calender) {
        this.calender = Calender;
    }

    public void updateCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        if (this.calender == null) {
            this.calender = new PZCalendar(new GregorianCalendar());
        }

        this.calender.set(year, month, dayOfMonth, hourOfDay, minute);
    }

    /**
     * @return the MinutesPerDay
     */
    public float getMinutesPerDay() {
        return this.minutesPerDay;
    }

    /**
     * 
     * @param MinutesPerDay the MinutesPerDay to set
     */
    public void setMinutesPerDay(float MinutesPerDay) {
        this.minutesPerDay = MinutesPerDay;
    }

    /**
     * @return the LastTimeOfDay
     */
    public float getLastTimeOfDay() {
        return this.lastTimeOfDay;
    }

    /**
     * 
     * @param LastTimeOfDay the LastTimeOfDay to set
     */
    public void setLastTimeOfDay(float LastTimeOfDay) {
        this.lastTimeOfDay = LastTimeOfDay;
    }

    /**
     * 
     * @param TargetZombies the TargetZombies to set
     * @deprecated
     */
    public void setTargetZombies(int TargetZombies) {
        this.targetZombies = TargetZombies;
    }

    /**
     * @return the RainingToday
     */
    public boolean isRainingToday() {
        return true;
    }

    /**
     * Number of real world seconds since the last tick, multiplied by game speed. Also multiplied by 48 for some reason.
     * @return the Multiplier
     */
    public float getMultiplier() {
        if (!GameServer.server && !GameClient.client && IsoPlayer.getInstance() != null && IsoPlayer.allPlayersAsleep()) {
            return 200.0F * (30.0F / PerformanceSettings.getLockFPS());
        } else {
            float multiplier = 1.0F;
            if (GameServer.server && GameServer.fastForward) {
                multiplier = (float)ServerOptions.instance.fastForwardMultiplier.getValue() / this.getDeltaMinutesPerDay();
            } else if (GameClient.client && GameClient.fastForward && GameWindow.isIngameState()) {
                multiplier = (float)ServerOptions.instance.fastForwardMultiplier.getValue() / this.getDeltaMinutesPerDay();
            }

            multiplier *= this.multiplier;
            multiplier *= this.fpsMultiplier;
            multiplier *= this.multiplierBias;
            multiplier *= this.perObjectMultiplier;
            if (DebugOptions.instance.gameTimeSpeedEighth.getValue()) {
                multiplier /= 8.0F;
            }

            if (DebugOptions.instance.gameTimeSpeedQuarter.getValue()) {
                multiplier /= 4.0F;
            }

            if (DebugOptions.instance.gameTimeSpeedHalf.getValue()) {
                multiplier /= 2.0F;
            }

            return multiplier * 0.8F;
        }
    }

    /**
     * The multiplier scales the game simulation speed. getTrueMultiplier() can be used to retrieve this value. getMultiplier() does not return this value.
     * 
     * @param in_multiplier the Multiplier to set
     */
    public void setMultiplier(float in_multiplier) {
        this.multiplier = in_multiplier;
    }

    /**
     * Number of real world seconds since the last tick, multiplied by game speed.
     */
    public float getTimeDelta() {
        return this.getTimeDeltaFromMultiplier(this.getMultiplier());
    }

    public float getTimeDeltaFromMultiplier(float in_multiplier) {
        return in_multiplier / 0.8F / this.multiplierBias / 60.0F;
    }

    public float getMultiplierFromTimeDelta(float in_timeDelta) {
        return in_timeDelta * 0.8F * this.multiplierBias * 60.0F;
    }

    /**
     * Delta based on the target framerate rather than the actual framerate. Unclear purpose. Probably shouldn't be used.
     */
    public float getServerMultiplier() {
        float FPSMultiplier = 10.0F / GameWindow.averageFPS / (PerformanceSettings.manualFrameSkips + 1);
        float multiplier = this.multiplier * FPSMultiplier;
        multiplier *= 0.5F;
        if (!GameServer.server && !GameClient.client && IsoPlayer.getInstance() != null && IsoPlayer.allPlayersAsleep()) {
            return 200.0F * (30.0F / PerformanceSettings.getLockFPS());
        } else {
            multiplier *= 1.6F;
            return multiplier * this.multiplierBias;
        }
    }

    /**
     * Number of real world seconds since the last tick, multiplied by game speed.
     */
    public float getUnmoddedMultiplier() {
        return !GameServer.server && !GameClient.client && IsoPlayer.getInstance() != null && IsoPlayer.allPlayersAsleep()
            ? 200.0F * (30.0F / PerformanceSettings.getLockFPS())
            : this.multiplier * this.fpsMultiplier * this.perObjectMultiplier;
    }

    /**
     * Return the inverse of getMultiplier() (1 / getMultiplier()). Per-tick RNG functions can multiply by this value to keep chances relatively stable across different framerates.
     */
    public float getInvMultiplier() {
        return 1.0F / this.getMultiplier();
    }

    /**
     * Returns the current game speed multiplier (from the singleplayer speed up UI or while all players are sleeping).
     */
    public float getTrueMultiplier() {
        return this.multiplier * this.perObjectMultiplier;
    }

    public float getThirtyFPSMultiplier() {
        return this.getMultiplier() / 1.6F;
    }

    public void saveToBufferMap(SaveBufferMap bufferMap) {
        synchronized (SliceY.SliceBufferLock) {
            SliceY.SliceBuffer.clear();
            DataOutputStream output = new DataOutputStream(new ByteBufferOutputStream(SliceY.SliceBuffer, false));

            try {
                instance.save(output);
                ByteBufferPooledObject buffer = bufferMap.allocate(SliceY.SliceBuffer.position());
                buffer.put(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                output.close();
                String outFile = ZomboidFileSystem.instance.getFileNameInCurrentSave("map_t.bin");
                bufferMap.put(outFile, buffer);
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        }
    }

    public void save() {
        File outFile = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_t.bin"));
        FileOutputStream outStream = null;

        try {
            outStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException var7) {
            var7.printStackTrace();
            return;
        }

        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(outStream));

        try {
            instance.save(output);
        } catch (IOException var6) {
            var6.printStackTrace();
        }

        try {
            output.flush();
            output.close();
        } catch (IOException var5) {
            var5.printStackTrace();
        }
    }

    public void save(DataOutputStream output) throws IOException {
        output.writeByte(71);
        output.writeByte(77);
        output.writeByte(84);
        output.writeByte(77);
        output.writeInt(240);
        output.writeFloat(this.multiplier);
        output.writeInt(this.nightsSurvived);
        output.writeInt(this.targetZombies);
        output.writeFloat(this.lastTimeOfDay);
        output.writeFloat(this.timeOfDay);
        output.writeInt(this.day);
        output.writeInt(this.month);
        output.writeInt(this.year);
        output.writeFloat(0.0F);
        output.writeFloat(0.0F);
        output.writeInt(0);
        if (this.table != null) {
            output.writeByte(1);
            this.table.save(output);
        } else {
            output.writeByte(0);
        }

        GameWindow.WriteString(output, Core.getInstance().getPoisonousBerry());
        GameWindow.WriteString(output, Core.getInstance().getPoisonousMushroom());
        output.writeInt(this.helicopterDay1);
        output.writeInt(this.helicopterTime1Start);
        output.writeInt(this.helicopterTime1End);
        ClimateManager.getInstance().save(output);
    }

    public void save(ByteBuffer output) throws IOException {
        output.putFloat(this.multiplier);
        output.putInt(this.nightsSurvived);
        output.putInt(this.targetZombies);
        output.putFloat(this.lastTimeOfDay);
        output.putFloat(this.timeOfDay);
        output.putInt(this.day);
        output.putInt(this.month);
        output.putInt(this.year);
        output.putFloat(0.0F);
        output.putFloat(0.0F);
        output.putInt(0);
        if (this.table != null) {
            output.put((byte)1);
            this.table.save(output);
        } else {
            output.put((byte)0);
        }
    }

    public void load(DataInputStream input) throws IOException {
        int WorldVersion = IsoWorld.savedWorldVersion;
        if (WorldVersion == -1) {
            WorldVersion = 240;
        }

        input.mark(0);
        byte b1 = input.readByte();
        byte b2 = input.readByte();
        byte b3 = input.readByte();
        byte b4 = input.readByte();
        if (b1 == 71 && b2 == 77 && b3 == 84 && b4 == 77) {
            WorldVersion = input.readInt();
        } else {
            input.reset();
        }

        this.multiplier = input.readFloat();
        this.nightsSurvived = input.readInt();
        this.targetZombies = input.readInt();
        this.lastTimeOfDay = input.readFloat();
        this.timeOfDay = input.readFloat();
        this.day = input.readInt();
        this.month = input.readInt();
        this.year = input.readInt();
        input.readFloat();
        input.readFloat();
        int nGroups = input.readInt();
        if (input.readByte() == 1) {
            if (this.table == null) {
                this.table = LuaManager.platform.newTable();
            }

            this.table.load(input, WorldVersion);
        }

        Core.getInstance().setPoisonousBerry(GameWindow.ReadString(input));
        Core.getInstance().setPoisonousMushroom(GameWindow.ReadString(input));
        this.helicopterDay1 = input.readInt();
        this.helicopterTime1Start = input.readInt();
        this.helicopterTime1End = input.readInt();
        ClimateManager.getInstance().load(input, WorldVersion);
        this.setMinutesStamp();
    }

    public void load(ByteBuffer input) throws IOException {
        int WorldVersion = 240;
        this.multiplier = input.getFloat();
        this.nightsSurvived = input.getInt();
        this.targetZombies = input.getInt();
        this.lastTimeOfDay = input.getFloat();
        this.timeOfDay = input.getFloat();
        this.day = input.getInt();
        this.month = input.getInt();
        this.year = input.getInt();
        input.getFloat();
        input.getFloat();
        int nGroups = input.getInt();
        if (input.get() == 1) {
            if (this.table == null) {
                this.table = LuaManager.platform.newTable();
            }

            this.table.load(input, 240);
        }

        this.setMinutesStamp();
    }

    public void load() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");

        try (
            FileInputStream fis = new FileInputStream(inFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            synchronized (SliceY.SliceBufferLock) {
                SliceY.SliceBuffer.clear();
                int numBytes = bis.read(SliceY.SliceBuffer.array());
                SliceY.SliceBuffer.limit(numBytes);
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(SliceY.SliceBuffer.array(), 0, numBytes));
                this.load(input);
            }
        } catch (FileNotFoundException var13) {
        } catch (Exception var14) {
            ExceptionLogger.logException(var14);
        }
    }

    /**
     * @deprecated
     */
    public int getDawn() {
        return this.dawn;
    }

    /**
     * @deprecated
     */
    public void setDawn(int dawn) {
        this.dawn = dawn;
    }

    /**
     * @deprecated
     */
    public int getDusk() {
        return this.dusk;
    }

    /**
     * @deprecated
     */
    public void setDusk(int dusk) {
        this.dusk = dusk;
    }

    /**
     * This was used to store non-object-specific mod data in the save file before global mod data was added. It is generally better to use the global mod data API provided by ModData.
     */
    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }

        return this.table;
    }

    /**
     * @deprecated
     */
    public boolean isThunderDay() {
        return this.thunderDay;
    }

    /**
     * @deprecated
     */
    public void setThunderDay(boolean thunderDay) {
        this.thunderDay = thunderDay;
    }

    public void saveToPacket(ByteBuffer bb) throws IOException {
        KahluaTable modData = getInstance().getModData();
        Object camping = modData.rawget("camping");
        Object farming = modData.rawget("farming");
        Object trapping = modData.rawget("trapping");
        modData.rawset("camping", null);
        modData.rawset("farming", null);
        modData.rawset("trapping", null);
        this.save(bb);
        modData.rawset("camping", camping);
        modData.rawset("farming", farming);
        modData.rawset("trapping", trapping);
    }

    public int getHelicopterDay1() {
        return this.helicopterDay1;
    }

    public int getHelicopterDay() {
        return this.helicopterDay1;
    }

    public void setHelicopterDay(int day) {
        this.helicopterDay1 = PZMath.max(day, 0);
    }

    public int getHelicopterStartHour() {
        return this.helicopterTime1Start;
    }

    public void setHelicopterStartHour(int hour) {
        this.helicopterTime1Start = PZMath.clamp(hour, 0, 24);
    }

    public int getHelicopterEndHour() {
        return this.helicopterTime1End;
    }

    public void setHelicopterEndHour(int hour) {
        this.helicopterTime1End = PZMath.clamp(hour, 0, 24);
    }

    public boolean isEndlessDay() {
        return SandboxOptions.getInstance().dayNightCycle.getValue() == 2;
    }

    public boolean isEndlessNight() {
        return SandboxOptions.getInstance().dayNightCycle.getValue() == 3;
    }

    public boolean isDay() {
        return this.isEndlessDay()
            || this.timeOfDay >= ClimateManager.getInstance().getSeason().getDawn() && this.timeOfDay <= ClimateManager.getInstance().getSeason().getDusk();
    }

    public boolean isNight() {
        return this.isEndlessNight() || !this.isDay();
    }

    public boolean isZombieActivityPhase() {
        return SandboxOptions.instance.lore.activeOnly.getValue() == 1
            || SandboxOptions.instance.lore.activeOnly.getValue() == 2 && this.isNight()
            || SandboxOptions.instance.lore.activeOnly.getValue() == 3 && this.isDay();
    }

    public boolean isZombieInactivityPhase() {
        return !this.isZombieActivityPhase();
    }

    public static class AnimTimer {
        public float elapsed;
        public float duration;
        public boolean finished = true;
        public int ticks;

        public void init(int ticks) {
            this.ticks = ticks;
            this.elapsed = 0.0F;
            this.duration = ticks / 30.0F;
            this.finished = false;
        }

        public void update() {
            this.elapsed = this.elapsed + GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F / 30.0F;
            if (this.elapsed >= this.duration) {
                this.elapsed = this.duration;
                this.finished = true;
            }
        }

        public float ratio() {
            return this.elapsed / this.duration;
        }

        public boolean finished() {
            return this.finished;
        }
    }
}
