// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;
import zombie.ui.UIManager;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class ThunderStorm {
    public static int mapMinX = -3000;
    public static int mapMinY = -3000;
    public static int mapMaxX = 25000;
    public static int mapMaxY = 20000;
    private boolean hasActiveThunderClouds;
    private final float cloudMaxRadius = 20000.0F;
    private final ThunderStorm.ThunderEvent[] events = new ThunderStorm.ThunderEvent[30];
    private final ThunderStorm.ThunderCloud[] clouds = new ThunderStorm.ThunderCloud[3];
    private final ClimateManager climateManager;
    private ArrayList<ThunderStorm.ThunderCloud> cloudCache;
    private final boolean donoise = false;
    private int strikeRadius = 4000;
    private final ThunderStorm.PlayerLightningInfo[] lightningInfos = new ThunderStorm.PlayerLightningInfo[4];
    private final ThunderStorm.ThunderEvent networkThunderEvent = new ThunderStorm.ThunderEvent();
    private ThunderStorm.ThunderCloud dummyCloud;

    public ArrayList<ThunderStorm.ThunderCloud> getClouds() {
        if (this.cloudCache == null) {
            this.cloudCache = new ArrayList<>(this.clouds.length);

            for (int i = 0; i < this.clouds.length; i++) {
                this.cloudCache.add(this.clouds[i]);
            }
        }

        return this.cloudCache;
    }

    public ThunderStorm(ClimateManager climmgr) {
        this.climateManager = climmgr;

        for (int i = 0; i < this.events.length; i++) {
            this.events[i] = new ThunderStorm.ThunderEvent();
        }

        for (int i = 0; i < this.clouds.length; i++) {
            this.clouds[i] = new ThunderStorm.ThunderCloud();
        }

        for (int i = 0; i < 4; i++) {
            this.lightningInfos[i] = new ThunderStorm.PlayerLightningInfo();
        }
    }

    private ThunderStorm.ThunderEvent getFreeEvent() {
        for (int i = 0; i < this.events.length; i++) {
            if (!this.events[i].isRunning) {
                return this.events[i];
            }
        }

        return null;
    }

    private ThunderStorm.ThunderCloud getFreeCloud() {
        for (int i = 0; i < this.clouds.length; i++) {
            if (!this.clouds[i].isRunning) {
                return this.clouds[i];
            }
        }

        return null;
    }

    private ThunderStorm.ThunderCloud getCloud(int id) {
        return id >= 0 && id < this.clouds.length ? this.clouds[id] : null;
    }

    public boolean HasActiveThunderClouds() {
        return this.hasActiveThunderClouds;
    }

    public void noise(String s) {
    }

    public void stopAllClouds() {
        for (int i = 0; i < this.clouds.length; i++) {
            this.stopCloud(i);
        }
    }

    public void stopCloud(int id) {
        ThunderStorm.ThunderCloud thunderCloud = this.getCloud(id);
        if (thunderCloud != null) {
            thunderCloud.isRunning = false;
        }
    }

    private static float addToAngle(float angle, float addition) {
        angle += addition;
        if (angle > 360.0F) {
            angle -= 360.0F;
        } else if (angle < 0.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    public static int getMapDiagonal() {
        int width = mapMaxX - mapMinX;
        int height = mapMaxY - mapMinY;
        int diag = (int)Math.sqrt(Math.pow(width, 2.0) + Math.pow(height, 2.0));
        return diag / 2;
    }

    public void startThunderCloud(float str, float angle, float radius, float eventFreq, float thunderRatio, double duration, boolean targetRandomPlayer) {
        this.startThunderCloud(str, angle, radius, eventFreq, thunderRatio, duration, targetRandomPlayer);
    }

    public ThunderStorm.ThunderCloud startThunderCloud(
        float str, float angle, float radius, float eventFreq, float thunderRatio, double duration, boolean targetRandomPlayer, float percentageOffset
    ) {
        if (GameClient.client) {
            return null;
        } else {
            ThunderStorm.ThunderCloud thunderCloud = this.getFreeCloud();
            if (thunderCloud != null) {
                angle = addToAngle(angle, Rand.Next(-10.0F, 10.0F));
                thunderCloud.startTime = GameTime.instance.getWorldAgeHours();
                thunderCloud.endTime = thunderCloud.startTime + duration;
                thunderCloud.duration = duration;
                thunderCloud.strength = ClimateManager.clamp01(str);
                thunderCloud.angle = angle;
                thunderCloud.radius = radius;
                if (thunderCloud.radius > 20000.0F) {
                    thunderCloud.radius = 20000.0F;
                }

                thunderCloud.eventFrequency = eventFreq;
                thunderCloud.thunderRatio = ClimateManager.clamp01(thunderRatio);
                thunderCloud.percentageOffset = PZMath.clamp_01(percentageOffset);
                float angleOpposing = addToAngle(angle, 180.0F);
                int width = mapMaxX - mapMinX;
                int height = mapMaxY - mapMinY;
                int centerX = Rand.Next(mapMinX + width / 5, mapMaxX - width / 5);
                int centerY = Rand.Next(mapMinY + height / 5, mapMaxY - height / 5);
                if (targetRandomPlayer) {
                    if (!GameServer.server) {
                        IsoPlayer player = IsoPlayer.getInstance();
                        if (player != null) {
                            centerX = (int)player.getX();
                            centerY = (int)player.getY();
                        }
                    } else {
                        if (GameServer.Players.isEmpty()) {
                            DebugLog.log("Thundercloud couldnt target player...");
                            return null;
                        }

                        ArrayList<IsoPlayer> players = GameServer.getPlayers();

                        for (int i = players.size() - 1; i >= 0; i--) {
                            if (players.get(i).getCurrentSquare() == null) {
                                players.remove(i);
                            }
                        }

                        if (!players.isEmpty()) {
                            IsoPlayer randomPlayer = players.get(Rand.Next(players.size()));
                            centerX = randomPlayer.getCurrentSquare().getX();
                            centerY = randomPlayer.getCurrentSquare().getY();
                        }
                    }
                }

                thunderCloud.setCenter(centerX, centerY, angle);
                thunderCloud.isRunning = true;
                thunderCloud.suspendTimer.init(3);
                return thunderCloud;
            } else {
                return null;
            }
        }
    }

    public void update(double currentTime) {
        if (!GameClient.client || GameServer.server) {
            this.hasActiveThunderClouds = false;

            for (int i = 0; i < this.clouds.length; i++) {
                ThunderStorm.ThunderCloud cloud = this.clouds[i];
                if (cloud.isRunning) {
                    if (currentTime < cloud.endTime) {
                        float t = (float)((currentTime - cloud.startTime) / cloud.duration);
                        if (cloud.percentageOffset > 0.0F) {
                            t = cloud.percentageOffset + (1.0F - cloud.percentageOffset) * t;
                        }

                        cloud.currentX = (int)ClimateManager.lerp(t, cloud.startX, cloud.endX);
                        cloud.currentY = (int)ClimateManager.lerp(t, cloud.startY, cloud.endY);
                        cloud.suspendTimer.update();
                        this.hasActiveThunderClouds = true;
                        if (cloud.suspendTimer.finished()) {
                            float suspendNext = Rand.Next(3.5F - 3.0F * cloud.strength, 24.0F - 20.0F * cloud.strength);
                            cloud.suspendTimer.init((int)(suspendNext * 60.0F));
                            float r = Rand.Next(0.0F, 1.0F);
                            if (r < 0.6F) {
                                this.strikeRadius = (int)(cloud.radius / 2.0F) / 3;
                            } else if (r < 0.9F) {
                                this.strikeRadius = (int)(cloud.radius / 2.0F) / 4 * 3;
                            } else {
                                this.strikeRadius = (int)(cloud.radius / 2.0F);
                            }

                            if (Rand.Next(0.0F, 1.0F) < cloud.thunderRatio) {
                                this.noise("trigger thunder event");
                                this.triggerThunderEvent(
                                    Rand.Next(cloud.currentX - this.strikeRadius, cloud.currentX + this.strikeRadius),
                                    Rand.Next(cloud.currentY - this.strikeRadius, cloud.currentY + this.strikeRadius),
                                    true,
                                    !Core.getInstance().getOptionLightSensitivity(),
                                    Rand.Next(0.0F, 1.0F) > 0.4F
                                );
                            } else {
                                this.triggerThunderEvent(
                                    Rand.Next(cloud.currentX - this.strikeRadius, cloud.currentX + this.strikeRadius),
                                    Rand.Next(cloud.currentY - this.strikeRadius, cloud.currentY + this.strikeRadius),
                                    false,
                                    false,
                                    true
                                );
                                this.noise("trigger rumble event");
                            }
                        }
                    } else {
                        cloud.isRunning = false;
                    }
                }
            }
        }

        if (GameClient.client || !GameServer.server) {
            for (int ix = 0; ix < 4; ix++) {
                ThunderStorm.PlayerLightningInfo linfo = this.lightningInfos[ix];
                if (linfo.lightningState == ThunderStorm.LightningState.ApplyLightning) {
                    linfo.timer.update();
                    if (!linfo.timer.finished()) {
                        linfo.lightningMod = ClimateManager.clamp01(linfo.timer.ratio());
                        this.climateManager.dayLightStrength.finalValue = this.climateManager.dayLightStrength.finalValue
                            + (1.0F - this.climateManager.dayLightStrength.finalValue) * (1.0F - linfo.lightningMod);
                        IsoPlayer player = IsoPlayer.players[ix];
                        if (player != null) {
                            player.dirtyRecalcGridStackTime = 1.0F;
                        }
                    } else {
                        this.noise("apply lightning done.");
                        linfo.timer.init(2);
                        linfo.lightningStrength = 0.0F;
                        linfo.lightningState = ThunderStorm.LightningState.Idle;
                    }
                }
            }

            boolean bFastForward = SpeedControls.instance.getCurrentGameSpeed() > 1;
            boolean bStrike = false;
            boolean bRumble = false;

            for (int ixx = 0; ixx < this.events.length; ixx++) {
                ThunderStorm.ThunderEvent event = this.events[ixx];
                if (event.isRunning) {
                    event.soundDelay.update();
                    if (event.soundDelay.finished()) {
                        event.isRunning = false;
                        boolean playSound = true;
                        if (UIManager.getSpeedControls() != null && UIManager.getSpeedControls().getCurrentGameSpeed() > 1) {
                            playSound = false;
                        }

                        if (playSound && !Core.soundDisabled && FMODManager.instance.getNumListeners() > 0) {
                            if (event.doStrike && (!bFastForward || !bStrike)) {
                                this.noise("thunder sound");
                                GameSound gameSound = GameSounds.getSound("Thunder");
                                GameSoundClip clip = gameSound == null ? null : gameSound.getRandomClip();
                                if (clip != null && clip.eventDescription != null) {
                                    long thunderEvent = clip.eventDescription.address;
                                    long inst = javafmod.FMOD_Studio_System_CreateEventInstance(thunderEvent);
                                    javafmod.FMOD_Studio_EventInstance3D(inst, event.eventX, event.eventY, 100.0F);
                                    javafmod.FMOD_Studio_EventInstance_SetVolume(inst, clip.getEffectiveVolume());
                                    javafmod.FMOD_Studio_StartEvent(inst);
                                    javafmod.FMOD_Studio_ReleaseEventInstance(inst);
                                }
                            }

                            if (event.doRumble && (!bFastForward || !bRumble)) {
                                this.noise("rumble sound");
                                GameSound gameSound = GameSounds.getSound("RumbleThunder");
                                GameSoundClip clip = gameSound == null ? null : gameSound.getRandomClip();
                                if (clip != null && clip.eventDescription != null) {
                                    long rumbleEvent = clip.eventDescription.address;
                                    long inst = javafmod.FMOD_Studio_System_CreateEventInstance(rumbleEvent);
                                    javafmod.FMOD_Studio_EventInstance3D(inst, event.eventX, event.eventY, 200.0F);
                                    javafmod.FMOD_Studio_EventInstance_SetVolume(inst, clip.getEffectiveVolume());
                                    javafmod.FMOD_Studio_StartEvent(inst);
                                    javafmod.FMOD_Studio_ReleaseEventInstance(inst);
                                }
                            }
                        }
                    } else {
                        bStrike = bStrike || event.doStrike;
                        bRumble = bRumble || event.doRumble;
                    }
                }
            }
        }
    }

    public void applyLightningForPlayer(RenderSettings.PlayerRenderSettings renderSettings, int plrIndex, IsoPlayer player) {
        ThunderStorm.PlayerLightningInfo linfo = this.lightningInfos[plrIndex];
        if (linfo.lightningState == ThunderStorm.LightningState.ApplyLightning) {
            ClimateColorInfo gl = renderSettings.cmGlobalLight;
            linfo.lightningColor.getExterior().r = gl.getExterior().r + linfo.lightningStrength * (1.0F - gl.getExterior().r);
            linfo.lightningColor.getExterior().g = gl.getExterior().g + linfo.lightningStrength * (1.0F - gl.getExterior().g);
            linfo.lightningColor.getExterior().b = gl.getExterior().b + linfo.lightningStrength * (1.0F - gl.getExterior().b);
            linfo.lightningColor.getInterior().r = gl.getInterior().r + linfo.lightningStrength * (1.0F - gl.getInterior().r);
            linfo.lightningColor.getInterior().g = gl.getInterior().g + linfo.lightningStrength * (1.0F - gl.getInterior().g);
            linfo.lightningColor.getInterior().b = gl.getInterior().b + linfo.lightningStrength * (1.0F - gl.getInterior().b);
            linfo.lightningColor.interp(renderSettings.cmGlobalLight, linfo.lightningMod, linfo.outColor);
            renderSettings.cmGlobalLight.getExterior().r = linfo.outColor.getExterior().r;
            renderSettings.cmGlobalLight.getExterior().g = linfo.outColor.getExterior().g;
            renderSettings.cmGlobalLight.getExterior().b = linfo.outColor.getExterior().b;
            renderSettings.cmGlobalLight.getInterior().r = linfo.outColor.getInterior().r;
            renderSettings.cmGlobalLight.getInterior().g = linfo.outColor.getInterior().g;
            renderSettings.cmGlobalLight.getInterior().b = linfo.outColor.getInterior().b;
            renderSettings.cmAmbient = ClimateManager.lerp(linfo.lightningMod, 1.0F, renderSettings.cmAmbient);
            renderSettings.cmDayLightStrength = ClimateManager.lerp(linfo.lightningMod, 1.0F, renderSettings.cmDayLightStrength);
            renderSettings.cmDesaturation = ClimateManager.lerp(linfo.lightningMod, 0.0F, renderSettings.cmDesaturation);
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                renderSettings.cmGlobalLightIntensity = ClimateManager.lerp(linfo.lightningMod, 1.0F, renderSettings.cmGlobalLightIntensity);
            } else {
                renderSettings.cmGlobalLightIntensity = ClimateManager.lerp(linfo.lightningMod, 0.0F, renderSettings.cmGlobalLightIntensity);
            }
        }
    }

    public boolean isModifyingNight() {
        return false;
    }

    public void triggerThunderEvent(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        if (GameServer.server) {
            this.networkThunderEvent.eventX = x;
            this.networkThunderEvent.eventY = y;
            this.networkThunderEvent.doStrike = doStrike;
            this.networkThunderEvent.doLightning = doLightning;
            this.networkThunderEvent.doRumble = doRumble;
            this.climateManager.transmitClimatePacket(ClimateManager.ClimateNetAuth.ServerOnly, (byte)2, null);
        } else if (!GameClient.client) {
            this.enqueueThunderEvent(x, y, doStrike, doLightning, doRumble);
        }
    }

    public void writeNetThunderEvent(ByteBuffer output) throws IOException {
        output.putInt(this.networkThunderEvent.eventX);
        output.putInt(this.networkThunderEvent.eventY);
        output.put((byte)(this.networkThunderEvent.doStrike ? 1 : 0));
        output.put((byte)(this.networkThunderEvent.doLightning ? 1 : 0));
        output.put((byte)(this.networkThunderEvent.doRumble ? 1 : 0));
    }

    public void readNetThunderEvent(ByteBuffer input) throws IOException {
        int x = input.getInt();
        int y = input.getInt();
        boolean doStrike = input.get() == 1;
        boolean doLightning = input.get() == 1;
        boolean doRumble = input.get() == 1;
        this.enqueueThunderEvent(x, y, doStrike, doLightning, doRumble);
    }

    public void enqueueThunderEvent(int x, int y, boolean doStrike, boolean doLightning, boolean doRumble) {
        LuaEventManager.triggerEvent("OnThunderEvent", x, y, doStrike, doLightning, doRumble);
        if (doStrike || doRumble) {
            int dist = 9999999;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    int pdist = this.GetDistance((int)player.getX(), (int)player.getY(), x, y);
                    if (pdist < dist) {
                        dist = pdist;
                    }

                    if (doLightning) {
                        this.lightningInfos[i].distance = pdist;
                        this.lightningInfos[i].x = x;
                        this.lightningInfos[i].y = y;
                    }
                }
            }

            this.noise("dist to player = " + dist);
            if (dist < 10000) {
                ThunderStorm.ThunderEvent event = this.getFreeEvent();
                if (event != null) {
                    event.doRumble = doRumble;
                    event.doStrike = doStrike;
                    event.eventX = x;
                    event.eventY = y;
                    event.isRunning = true;
                    event.soundDelay.init((int)(dist / 300.0F * 60.0F));
                    if (doLightning) {
                        for (int ix = 0; ix < IsoPlayer.numPlayers; ix++) {
                            IsoPlayer player = IsoPlayer.players[ix];
                            if (player != null && this.lightningInfos[ix].distance < 7500.0F) {
                                float ls = 1.0F - this.lightningInfos[ix].distance / 7500.0F;
                                this.lightningInfos[ix].lightningState = ThunderStorm.LightningState.ApplyLightning;
                                if (ls > this.lightningInfos[ix].lightningStrength) {
                                    this.lightningInfos[ix].lightningStrength = ls;
                                    this.lightningInfos[ix].timer.init(20 + (int)(80.0F * this.lightningInfos[ix].lightningStrength));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    /**
     * IO
     */
    public void save(DataOutputStream output) throws IOException {
        if (GameClient.client && !GameServer.server) {
            output.writeByte(0);
        } else {
            output.writeByte(this.clouds.length);

            for (int i = 0; i < this.clouds.length; i++) {
                ThunderStorm.ThunderCloud cloud = this.clouds[i];
                output.writeBoolean(cloud.isRunning);
                if (cloud.isRunning) {
                    output.writeInt(cloud.startX);
                    output.writeInt(cloud.startY);
                    output.writeInt(cloud.endX);
                    output.writeInt(cloud.endY);
                    output.writeFloat(cloud.radius);
                    output.writeFloat(cloud.angle);
                    output.writeFloat(cloud.strength);
                    output.writeFloat(cloud.thunderRatio);
                    output.writeDouble(cloud.startTime);
                    output.writeDouble(cloud.endTime);
                    output.writeDouble(cloud.duration);
                    output.writeFloat(cloud.percentageOffset);
                }
            }
        }
    }

    public void load(DataInputStream input) throws IOException {
        int len = input.readByte();
        if (len != 0) {
            if (len > this.clouds.length && this.dummyCloud == null) {
                this.dummyCloud = new ThunderStorm.ThunderCloud();
            }

            for (int i = 0; i < len; i++) {
                boolean isrunnin = input.readBoolean();
                ThunderStorm.ThunderCloud cloud;
                if (i >= this.clouds.length) {
                    cloud = this.dummyCloud;
                } else {
                    cloud = this.clouds[i];
                }

                cloud.isRunning = isrunnin;
                if (isrunnin) {
                    cloud.startX = input.readInt();
                    cloud.startY = input.readInt();
                    cloud.endX = input.readInt();
                    cloud.endY = input.readInt();
                    cloud.radius = input.readFloat();
                    cloud.angle = input.readFloat();
                    cloud.strength = input.readFloat();
                    cloud.thunderRatio = input.readFloat();
                    cloud.startTime = input.readDouble();
                    cloud.endTime = input.readDouble();
                    cloud.duration = input.readDouble();
                    cloud.percentageOffset = input.readFloat();
                }
            }
        }
    }

    private static enum LightningState {
        Idle,
        ApplyLightning;
    }

    private class PlayerLightningInfo {
        public ThunderStorm.LightningState lightningState;
        public GameTime.AnimTimer timer;
        public float lightningStrength;
        public float lightningMod;
        public ClimateColorInfo lightningColor;
        public ClimateColorInfo outColor;
        public int x;
        public int y;
        public int distance;

        private PlayerLightningInfo() {
            Objects.requireNonNull(ThunderStorm.this);
            super();
            this.lightningState = ThunderStorm.LightningState.Idle;
            this.timer = new GameTime.AnimTimer();
            this.lightningStrength = 1.0F;
            this.lightningColor = new ClimateColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
            this.outColor = new ClimateColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @UsedFromLua
    public static class ThunderCloud {
        private int currentX;
        private int currentY;
        private int startX;
        private int startY;
        private int endX;
        private int endY;
        private double startTime;
        private double endTime;
        private double duration;
        private float strength;
        private float angle;
        private float radius;
        private float eventFrequency;
        private float thunderRatio;
        private float percentageOffset;
        private boolean isRunning;
        private final GameTime.AnimTimer suspendTimer = new GameTime.AnimTimer();

        public int getCurrentX() {
            return this.currentX;
        }

        public int getCurrentY() {
            return this.currentY;
        }

        public float getRadius() {
            return this.radius;
        }

        public boolean isRunning() {
            return this.isRunning;
        }

        public float getStrength() {
            return this.strength;
        }

        public double lifeTime() {
            return (this.startTime - this.endTime) / this.duration;
        }

        public void setCenter(int centerX, int centerY, float angle) {
            int diag = ThunderStorm.getMapDiagonal();
            float angleOpposing = ThunderStorm.addToAngle(angle, 180.0F);
            int randDist = diag + Rand.Next(1500, 7500);
            int sx = (int)(centerX + randDist * Math.cos(Math.toRadians(angleOpposing)));
            int sy = (int)(centerY + randDist * Math.sin(Math.toRadians(angleOpposing)));
            randDist = diag + Rand.Next(1500, 7500);
            int ex = (int)(centerX + randDist * Math.cos(Math.toRadians(angle)));
            int ey = (int)(centerY + randDist * Math.sin(Math.toRadians(angle)));
            this.startX = sx;
            this.startY = sy;
            this.endX = ex;
            this.endY = ey;
            this.currentX = sx;
            this.currentY = sy;
        }
    }

    private static class ThunderEvent {
        private int eventX;
        private int eventY;
        private boolean doLightning;
        private boolean doRumble;
        private boolean doStrike;
        private final GameTime.AnimTimer soundDelay = new GameTime.AnimTimer();
        private boolean isRunning;
    }
}
