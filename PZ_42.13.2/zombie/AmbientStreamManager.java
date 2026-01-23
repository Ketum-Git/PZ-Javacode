// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import fmod.javafmod;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK_TYPE;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import org.joml.Vector2f;
import zombie.Lua.LuaEventManager;
import zombie.audio.parameters.ParameterCameraZoom;
import zombie.audio.parameters.ParameterCharacterElevation;
import zombie.audio.parameters.ParameterClosestExteriorWallDistance;
import zombie.audio.parameters.ParameterClosestWallDistance;
import zombie.audio.parameters.ParameterFogIntensity;
import zombie.audio.parameters.ParameterHardOfHearing;
import zombie.audio.parameters.ParameterInside;
import zombie.audio.parameters.ParameterMoodlePanic;
import zombie.audio.parameters.ParameterPowerSupply;
import zombie.audio.parameters.ParameterRainIntensity;
import zombie.audio.parameters.ParameterRoomSize;
import zombie.audio.parameters.ParameterRoomType;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.audio.parameters.ParameterSeason;
import zombie.audio.parameters.ParameterSnowIntensity;
import zombie.audio.parameters.ParameterStorm;
import zombie.audio.parameters.ParameterStreamerMode;
import zombie.audio.parameters.ParameterTemperature;
import zombie.audio.parameters.ParameterTimeOfDay;
import zombie.audio.parameters.ParameterWaterSupply;
import zombie.audio.parameters.ParameterWeatherEvent;
import zombie.audio.parameters.ParameterWindIntensity;
import zombie.audio.parameters.ParameterZone;
import zombie.audio.parameters.ParameterZoneWaterSide;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.input.Mouse;
import zombie.iso.Alarm;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.objects.RainManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.scripting.objects.CharacterTrait;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class AmbientStreamManager extends BaseAmbientStreamManager {
    public static int oneInAmbienceChance = 2500;
    public static int maxAmbientCount = 20;
    public static float maxRange = 1000.0F;
    public final ArrayList<Alarm> alarmList = new ArrayList<>();
    public static BaseAmbientStreamManager instance;
    public final ArrayList<AmbientStreamManager.Ambient> ambient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.WorldSoundEmitter> worldEmitters = new ArrayList<>();
    public final ArrayDeque<AmbientStreamManager.WorldSoundEmitter> freeEmitters = new ArrayDeque<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> allAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> nightAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> dayAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> rainAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> indoorAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> outdoorAmbient = new ArrayList<>();
    public final ArrayList<AmbientStreamManager.AmbientLoop> windAmbient = new ArrayList<>();
    public boolean initialized;
    private FMODSoundEmitter electricityShutOffEmitter;
    private long electricityShutOffEvent;
    private int electricityShutOffState = -1;
    private final ParameterFogIntensity parameterFogIntensity = new ParameterFogIntensity();
    private final ParameterRainIntensity parameterRainIntensity = new ParameterRainIntensity();
    private final ParameterSeason parameterSeason = new ParameterSeason();
    private final ParameterSnowIntensity parameterSnowIntensity = new ParameterSnowIntensity();
    private final ParameterStorm parameterStorm = new ParameterStorm();
    private final ParameterTimeOfDay parameterTimeOfDay = new ParameterTimeOfDay();
    private final ParameterTemperature parameterTemperature = new ParameterTemperature();
    private final ParameterWeatherEvent parameterWeatherEvent = new ParameterWeatherEvent();
    private final ParameterWindIntensity parameterWindIntensity = new ParameterWindIntensity();
    private final ParameterStreamerMode parameterStreamerMode = new ParameterStreamerMode();
    private final ParameterZone parameterZoneDeepForest = new ParameterZone("ZoneDeepForest", "DeepForest");
    private final ParameterZone parameterZoneFarm = new ParameterZone("ZoneFarm", "Farm");
    private final ParameterZone parameterZoneForest = new ParameterZone("ZoneForest", "Forest");
    private final ParameterZone parameterZoneNav = new ParameterZone("ZoneNav", "Nav");
    private final ParameterZone parameterZoneTown = new ParameterZone("ZoneTown", "TownZone");
    private final ParameterZone parameterZoneTrailerPark = new ParameterZone("ZoneTrailerPark", "TrailerPark");
    private final ParameterZone parameterZoneVegetation = new ParameterZone("ZoneVegetation", "Vegitation");
    private final ParameterZoneWaterSide parameterZoneWaterSide = new ParameterZoneWaterSide();
    private final ParameterCameraZoom parameterCameraZoom = new ParameterCameraZoom();
    private final ParameterCharacterElevation parameterCharacterElevation = new ParameterCharacterElevation();
    private final ParameterClosestWallDistance parameterClosestWallDistance = new ParameterClosestWallDistance();
    private final ParameterClosestExteriorWallDistance parameterClosestExteriorWallDistance = new ParameterClosestExteriorWallDistance();
    private final ParameterHardOfHearing parameterHardOfHearing = new ParameterHardOfHearing();
    private final ParameterInside parameterInside = new ParameterInside();
    private final ParameterMoodlePanic parameterMoodlePanic = new ParameterMoodlePanic();
    private final ParameterPowerSupply parameterPowerSupply = new ParameterPowerSupply();
    private final ParameterRoomSize parameterRoomSize = new ParameterRoomSize();
    private final ParameterRoomType parameterRoomType = new ParameterRoomType();
    private final ParameterRoomTypeEx parameterRoomTypeEx = new ParameterRoomTypeEx();
    private final ParameterWaterSupply parameterWaterSupply = new ParameterWaterSupply();
    private final Vector2 tempo = new Vector2();
    private final FMOD_STUDIO_EVENT_CALLBACK electricityShutOffEventCallback = new FMOD_STUDIO_EVENT_CALLBACK() {
        {
            Objects.requireNonNull(AmbientStreamManager.this);
        }

        @Override
        public void timelineMarker(long eventInstance, String name, int position) {
            DebugLog.Sound.debugln("timelineMarker %s %d", name, position);
            if ("ElectricityOff".equals(name)) {
                IsoWorld.instance.setHydroPowerOn(false);
                AmbientStreamManager.this.checkHaveElectricity();
            }
        }
    };

    public static BaseAmbientStreamManager getInstance() {
        return instance;
    }

    @Override
    public void update() {
        if (this.initialized) {
            if (!GameTime.isGamePaused()) {
                IsoPlayer player = null;
                IsoPlayer firstDeafPlayer = null;

                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer player1 = IsoPlayer.players[i];
                    if (player1 != null) {
                        if (!player1.hasTrait(CharacterTrait.DEAF)) {
                            player = player1;
                            break;
                        }

                        if (firstDeafPlayer == null) {
                            firstDeafPlayer = player1;
                        }
                    }
                }

                if (player == null) {
                    player = firstDeafPlayer;
                }

                if (player != null) {
                    IsoGridSquare playerSq = player.getCurrentSquare();
                    if (playerSq != null) {
                        this.updatePowerSupply();
                        this.parameterFogIntensity.update();
                        this.parameterRainIntensity.update();
                        this.parameterSeason.update();
                        this.parameterSnowIntensity.update();
                        this.parameterStorm.update();
                        this.parameterStreamerMode.update();
                        this.parameterTemperature.update();
                        this.parameterTimeOfDay.update();
                        this.parameterWeatherEvent.update();
                        this.parameterWindIntensity.update();
                        this.parameterZoneDeepForest.update();
                        this.parameterZoneFarm.update();
                        this.parameterZoneForest.update();
                        this.parameterZoneNav.update();
                        this.parameterZoneVegetation.update();
                        this.parameterZoneTown.update();
                        this.parameterZoneTrailerPark.update();
                        this.parameterZoneWaterSide.update();
                        this.parameterCameraZoom.update();
                        this.parameterCharacterElevation.update();
                        this.parameterClosestWallDistance.update();
                        this.parameterClosestExteriorWallDistance.update();
                        this.parameterHardOfHearing.update();
                        this.parameterInside.update();
                        this.parameterMoodlePanic.update();
                        this.parameterPowerSupply.update();
                        this.parameterRoomSize.update();
                        this.parameterRoomTypeEx.update();
                        this.parameterWaterSupply.update();
                        float timeOfDay = GameTime.instance.getTimeOfDay();

                        for (int n = 0; n < this.worldEmitters.size(); n++) {
                            AmbientStreamManager.WorldSoundEmitter worldSoundEmitter = this.worldEmitters.get(n);
                            if (worldSoundEmitter.daytime != null) {
                                IsoGridSquare sq = IsoWorld.instance
                                    .currentCell
                                    .getGridSquare((double)worldSoundEmitter.x, (double)worldSoundEmitter.y, (double)worldSoundEmitter.z);
                                if (sq == null) {
                                    worldSoundEmitter.fmodEmitter.stopAll();
                                    SoundManager.instance.unregisterEmitter(worldSoundEmitter.fmodEmitter);
                                    this.worldEmitters.remove(worldSoundEmitter);
                                    this.freeEmitters.add(worldSoundEmitter);
                                    n--;
                                } else {
                                    if (timeOfDay > worldSoundEmitter.dawn && timeOfDay < worldSoundEmitter.dusk) {
                                        if (worldSoundEmitter.fmodEmitter.isEmpty()) {
                                            worldSoundEmitter.channel = worldSoundEmitter.fmodEmitter.playAmbientLoopedImpl(worldSoundEmitter.daytime);
                                        }
                                    } else if (!worldSoundEmitter.fmodEmitter.isEmpty()) {
                                        worldSoundEmitter.fmodEmitter.stopSound(worldSoundEmitter.channel);
                                        worldSoundEmitter.channel = 0L;
                                    }

                                    if (!worldSoundEmitter.fmodEmitter.isEmpty()
                                        && (IsoWorld.instance.emitterUpdate || worldSoundEmitter.fmodEmitter.hasSoundsToStart())) {
                                        worldSoundEmitter.fmodEmitter.tick();
                                    }
                                }
                            } else if (player != null && player.hasTrait(CharacterTrait.DEAF)) {
                                worldSoundEmitter.fmodEmitter.stopAll();
                                SoundManager.instance.unregisterEmitter(worldSoundEmitter.fmodEmitter);
                                this.worldEmitters.remove(worldSoundEmitter);
                                this.freeEmitters.add(worldSoundEmitter);
                                n--;
                            } else {
                                IsoGridSquare sq = IsoWorld.instance
                                    .currentCell
                                    .getGridSquare((double)worldSoundEmitter.x, (double)worldSoundEmitter.y, (double)worldSoundEmitter.z);
                                if (sq != null && !worldSoundEmitter.fmodEmitter.isEmpty()) {
                                    worldSoundEmitter.fmodEmitter.x = worldSoundEmitter.x;
                                    worldSoundEmitter.fmodEmitter.y = worldSoundEmitter.y;
                                    worldSoundEmitter.fmodEmitter.z = worldSoundEmitter.z;
                                    if (IsoWorld.instance.emitterUpdate || worldSoundEmitter.fmodEmitter.hasSoundsToStart()) {
                                        worldSoundEmitter.fmodEmitter.tick();
                                    }
                                } else {
                                    worldSoundEmitter.fmodEmitter.stopAll();
                                    SoundManager.instance.unregisterEmitter(worldSoundEmitter.fmodEmitter);
                                    this.worldEmitters.remove(worldSoundEmitter);
                                    this.freeEmitters.add(worldSoundEmitter);
                                    n--;
                                }
                            }
                        }

                        float night = GameTime.instance.getNight();
                        boolean bIndoors = playerSq.isInARoom();
                        boolean bRaining = RainManager.isRaining();

                        for (int nx = 0; nx < this.allAmbient.size(); nx++) {
                            this.allAmbient.get(nx).targVol = 1.0F;
                        }

                        for (int nx = 0; nx < this.nightAmbient.size(); nx++) {
                            this.nightAmbient.get(nx).targVol *= night;
                        }

                        for (int nx = 0; nx < this.dayAmbient.size(); nx++) {
                            this.dayAmbient.get(nx).targVol *= 1.0F - night;
                        }

                        for (int nx = 0; nx < this.indoorAmbient.size(); nx++) {
                            this.indoorAmbient.get(nx).targVol *= bIndoors ? 0.8F : 0.0F;
                        }

                        for (int nx = 0; nx < this.outdoorAmbient.size(); nx++) {
                            this.outdoorAmbient.get(nx).targVol *= bIndoors ? 0.15F : 0.8F;
                        }

                        for (int nx = 0; nx < this.rainAmbient.size(); nx++) {
                            this.rainAmbient.get(nx).targVol *= bRaining ? 1.0F : 0.0F;
                            if (this.rainAmbient.get(nx).channel != 0L) {
                                javafmod.FMOD_Studio_EventInstance_SetParameterByName(
                                    this.rainAmbient.get(nx).channel, "RainIntensity", ClimateManager.getInstance().getPrecipitationIntensity()
                                );
                            }
                        }

                        for (int nxx = 0; nxx < this.allAmbient.size(); nxx++) {
                            this.allAmbient.get(nxx).update();
                        }

                        for (int nxx = 0; nxx < this.alarmList.size(); nxx++) {
                            this.alarmList.get(nxx).update();
                            if (this.alarmList.get(nxx).finished) {
                                this.alarmList.remove(nxx);
                                nxx--;
                            }
                        }

                        this.doOneShotAmbients();
                    }
                }
            }
        }
    }

    @Override
    public void doOneShotAmbients() {
        for (int n = 0; n < this.ambient.size(); n++) {
            AmbientStreamManager.Ambient a = this.ambient.get(n);
            if (a.finished()) {
                DebugLog.log(DebugType.Sound, "ambient: removing ambient sound " + a.name);
                this.ambient.remove(n--);
            } else {
                a.update();
            }
        }
    }

    @Override
    public void addRandomAmbient() {
        this.addRandomAmbient(true);
    }

    public void addRandomAmbient(boolean force) {
        if (!Core.gameMode.equals("LastStand") && !Core.gameMode.equals("Tutorial")) {
            ArrayList<IsoPlayer> players = new ArrayList<>();

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && player.isAlive()) {
                    players.add(player);
                }
            }

            if (!players.isEmpty()) {
                IsoPlayer player = players.get(Rand.Next(players.size()));
                String sound = "";
                boolean beforeWildlifeInCities = player.getSquare() != null
                    && !player.getSquare().isRuralExtraFussy()
                    && GameTime.instance.getWorldAgeDaysSinceBegin() < 29.0;
                if (GameTime.instance.getHour() > 7 && GameTime.instance.getHour() < 21) {
                    switch (Rand.Next(3)) {
                        case 0:
                            if (force || Rand.Next(10) < 2) {
                                sound = "MetaDogBark";
                            }
                            break;
                        case 1:
                            if (force || Rand.Next(10) < 3) {
                                sound = "MetaScream";
                            }
                    }
                } else {
                    switch (Rand.Next(5)) {
                        case 0:
                            if (force || Rand.Next(10) < 2) {
                                sound = "MetaDogBark";
                            }
                            break;
                        case 1:
                            if (force || Rand.Next(13) < 3) {
                                sound = "MetaScream";
                            }
                            break;
                        case 2:
                            if (!beforeWildlifeInCities) {
                                sound = "MetaOwl";
                            }

                            if (beforeWildlifeInCities) {
                                DebugLog.Sound.debugln("not playing ambient wildlife sound because player is in a city and 4 weeks haven't passed");
                            }
                            break;
                        case 3:
                            if (!beforeWildlifeInCities) {
                                sound = "MetaWolfHowl";
                            }
                    }
                }

                if (!sound.isEmpty()) {
                    if (!beforeWildlifeInCities || !sound.toLowerCase().contains("owl") && !sound.toLowerCase().contains("wolf")) {
                        float x = player.getX();
                        float y = player.getY();
                        double radians = Rand.Next((float) -Math.PI, (float) Math.PI);
                        this.tempo.x = (float)Math.cos(radians);
                        this.tempo.y = (float)Math.sin(radians);
                        this.tempo.setLength(1000.0F);
                        x += this.tempo.x;
                        y += this.tempo.y;
                        if (!GameClient.client) {
                            DebugLog.Sound
                                .debugln("playing ambient: " + sound + " at dist: " + Math.abs(x - player.getX()) + "," + Math.abs(y - player.getY()));
                            AmbientStreamManager.Ambient a = new AmbientStreamManager.Ambient(sound, x, y, 50.0F, Rand.Next(0.2F, 0.5F));
                            this.ambient.add(a);
                        }
                    } else {
                        DebugLog.Sound.debugln("not playing ambient wildlife sound because player is in a city");
                    }
                }
            }
        }
    }

    @Override
    public void addBlend(String name, float vol, boolean bIndoors, boolean bRain, boolean bNight, boolean bDay) {
        AmbientStreamManager.AmbientLoop loop = new AmbientStreamManager.AmbientLoop(0.0F, name, vol);
        this.allAmbient.add(loop);
        if (bIndoors) {
            this.indoorAmbient.add(loop);
        } else {
            this.outdoorAmbient.add(loop);
        }

        if (bRain) {
            this.rainAmbient.add(loop);
        }

        if (bNight) {
            this.nightAmbient.add(loop);
        }

        if (bDay) {
            this.dayAmbient.add(loop);
        }
    }

    @Override
    public void init() {
        if (!this.initialized) {
            this.initialized = true;
        }
    }

    @Override
    public void doGunEvent() {
        ArrayList<IsoPlayer> players = new ArrayList<>();

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null && player.isAlive()) {
                players.add(player);
            }
        }

        if (!players.isEmpty()) {
            IsoPlayer player = players.get(Rand.Next(players.size()));
            String weaponFire = null;
            switch (Rand.Next(6)) {
                case 0:
                    weaponFire = "MetaAssaultRifle1";
                    break;
                case 1:
                    weaponFire = "MetaPistol1";
                    break;
                case 2:
                    weaponFire = "MetaShotgun1";
                    break;
                case 3:
                    weaponFire = "MetaPistol2";
                    break;
                case 4:
                    weaponFire = "MetaPistol3";
                    break;
                case 5:
                    weaponFire = "MetaShotgun1";
            }

            float x = player.getX();
            float y = player.getY();
            int worldSoundRadius = 600;
            double radians = Rand.Next((float) -Math.PI, (float) Math.PI);
            this.tempo.x = (float)Math.cos(radians);
            this.tempo.y = (float)Math.sin(radians);
            this.tempo.setLength(500.0F);
            x += this.tempo.x;
            y += this.tempo.y;
            WorldSoundManager.instance.addSound(null, PZMath.fastfloor(x), PZMath.fastfloor(y), 0, 600, 600);
            float gain = 1.0F;
            AmbientStreamManager.Ambient a = new AmbientStreamManager.Ambient(weaponFire, x, y, 700.0F, 1.0F);
            this.ambient.add(a);
        }
    }

    @Override
    public void doAlarm(RoomDef room) {
        if (room != null && room.building != null && room.building.alarmed) {
            DebugLog.Sound.debugln("Elec shutoff = " + SandboxOptions.getInstance().getElecShutModifier());
            DebugLog.Sound.debugln("alarm decay = " + room.building.alarmDecay);
            DebugLog.Sound
                .debugln(
                    "nights survived = "
                        + (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30)
                );
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(room.x, room.y, 0);
            if (sq != null && sq.hasGridPower(room.building.alarmDecay)) {
                this.alarmList.add(new Alarm(room.x + room.getW() / 2, room.y + room.getH() / 2));
            }

            room.building.alarmed = false;
            room.building.setAllExplored(true);
        }
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    @Override
    public void handleThunderEvent(int x, int y) {
        int dist = 9999999;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null && player.isAlive()) {
                int pdist = this.GetDistance((int)player.getX(), (int)player.getY(), x, y);
                if (pdist < dist) {
                    dist = pdist;
                }
            }
        }

        int worldSoundRadius = 5000;
        if (dist <= 5000) {
            WorldSoundManager.instance.addSound(null, PZMath.fastfloor((float)x), PZMath.fastfloor((float)y), 0, 5000, 5000);
            AmbientStreamManager.Ambient a = new AmbientStreamManager.Ambient("", x, y, 5100.0F, 1.0F);
            this.ambient.add(a);
        }
    }

    @Override
    public void stop() {
        for (AmbientStreamManager.AmbientLoop l : this.allAmbient) {
            l.stop();
        }

        this.allAmbient.clear();
        this.ambient.clear();
        this.dayAmbient.clear();
        this.indoorAmbient.clear();
        this.nightAmbient.clear();
        this.outdoorAmbient.clear();
        this.rainAmbient.clear();
        this.windAmbient.clear();
        this.alarmList.clear();
        if (this.electricityShutOffEmitter != null) {
            this.electricityShutOffEmitter.stopAll();
            this.electricityShutOffEvent = 0L;
        }

        this.electricityShutOffState = -1;
        this.initialized = false;
    }

    @Override
    public void addAmbient(String name, int x, int y, int radius, float volume) {
        if (GameClient.client) {
            AmbientStreamManager.Ambient a = new AmbientStreamManager.Ambient(name, x, y, radius, volume, true);
            this.ambient.add(a);
        }
    }

    @Override
    public void addAmbientEmitter(float x, float y, int z, String name) {
        AmbientStreamManager.WorldSoundEmitter e = this.freeEmitters.isEmpty() ? new AmbientStreamManager.WorldSoundEmitter() : this.freeEmitters.pop();
        e.x = x;
        e.y = y;
        e.z = z;
        e.daytime = null;
        if (e.fmodEmitter == null) {
            e.fmodEmitter = new FMODSoundEmitter();
        }

        e.fmodEmitter.x = x;
        e.fmodEmitter.y = y;
        e.fmodEmitter.z = z;
        e.channel = e.fmodEmitter.playAmbientLoopedImpl(name);
        e.fmodEmitter.randomStart();
        SoundManager.instance.registerEmitter(e.fmodEmitter);
        this.worldEmitters.add(e);
    }

    @Override
    public void addDaytimeAmbientEmitter(float x, float y, int z, String name) {
        AmbientStreamManager.WorldSoundEmitter e = this.freeEmitters.isEmpty() ? new AmbientStreamManager.WorldSoundEmitter() : this.freeEmitters.pop();
        e.x = x;
        e.y = y;
        e.z = z;
        if (e.fmodEmitter == null) {
            e.fmodEmitter = new FMODSoundEmitter();
        }

        e.fmodEmitter.x = x;
        e.fmodEmitter.y = y;
        e.fmodEmitter.z = z;
        e.daytime = name;
        e.dawn = Rand.Next(7.0F, 8.0F);
        e.dusk = Rand.Next(19.0F, 20.0F);
        SoundManager.instance.registerEmitter(e.fmodEmitter);
        this.worldEmitters.add(e);
    }

    private void updatePowerSupply() {
        boolean bPowerOn = SandboxOptions.getInstance().doesPowerGridExist();
        if (this.electricityShutOffState == -1) {
            IsoWorld.instance.setHydroPowerOn(bPowerOn);
        }

        if (this.electricityShutOffState == 0 && bPowerOn) {
            IsoWorld.instance.setHydroPowerOn(true);
            this.checkHaveElectricity();
        }

        if (this.electricityShutOffState == 1 && !bPowerOn) {
            if (this.electricityShutOffEmitter == null) {
                this.electricityShutOffEmitter = new FMODSoundEmitter();
            }

            if (!this.electricityShutOffEmitter.isPlaying(this.electricityShutOffEvent)) {
                Vector2f pos = new Vector2f();
                this.getListenerPos(pos);
                BuildingDef buildingDef = getNearestBuilding(pos.x, pos.y);
                if (buildingDef == null) {
                    this.electricityShutOffEmitter.setPos(-1000.0F, -1000.0F, 0.0F);
                } else {
                    Vector2f closestXY = BaseVehicle.allocVector2f();
                    buildingDef.getClosestPoint(pos.x, pos.y, closestXY);
                    this.electricityShutOffEmitter.setPos(closestXY.x, closestXY.y, 0.0F);
                    BaseVehicle.releaseVector2f(closestXY);
                }

                this.electricityShutOffEvent = this.electricityShutOffEmitter.playSound("WorldEventElectricityShutdown");
                if (this.electricityShutOffEvent != 0L) {
                    javafmod.FMOD_Studio_EventInstance_SetCallback(
                        this.electricityShutOffEvent,
                        this.electricityShutOffEventCallback,
                        FMOD_STUDIO_EVENT_CALLBACK_TYPE.FMOD_STUDIO_EVENT_CALLBACK_TIMELINE_MARKER.bit
                    );
                }
            }
        }

        this.electricityShutOffState = bPowerOn ? 1 : 0;
        if (this.electricityShutOffEmitter != null) {
            this.electricityShutOffEmitter.tick();
        }
    }

    @Override
    public void checkHaveElectricity() {
        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[i];
            if (!chunkMap.ignore) {
                for (int z = -32; z <= 31; z++) {
                    for (int y = chunkMap.getWorldYMinTiles(); y <= chunkMap.getWorldYMaxTiles(); y++) {
                        for (int x = chunkMap.getWorldXMinTiles(); x <= chunkMap.getWorldXMaxTiles(); x++) {
                            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                            if (square != null) {
                                for (int j = 0; j < square.getObjects().size(); j++) {
                                    IsoObject obj = square.getObjects().get(j);
                                    obj.checkHaveElectricity();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isParameterInsideTrue() {
        return this.parameterInside.getCurrentValue() > 0.0F;
    }

    public static BuildingDef getNearestBuilding(float px, float py) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int cellX = PZMath.fastfloor(px / 256.0F);
        int cellY = PZMath.fastfloor(py / 256.0F);
        BuildingDef closest = null;
        float closestDist = Float.MAX_VALUE;

        for (int cy = cellY - 1; cy <= cellY + 1; cy++) {
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                IsoMetaCell metaCell = metaGrid.getCellData(cx, cy);
                if (metaCell != null && metaCell.info != null) {
                    for (BuildingDef buildingDef : metaCell.buildings) {
                        float dx = Math.abs(px - buildingDef.x);
                        dx = Math.min(dx, Math.abs(px - buildingDef.x2));
                        float dy = Math.abs(py - buildingDef.y);
                        dy = Math.min(dy, Math.abs(py - buildingDef.y2));
                        float dist = org.joml.Math.sqrt(dx * dx + dy * dy);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = buildingDef;
                        }
                    }
                }
            }
        }

        return closest;
    }

    private void getListenerPos(Vector2f pos) {
        IsoGameCharacter character = null;
        pos.set(0.0F);

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null
                && (
                    character == null
                        || character.isDead() && player.isAlive()
                        || character.hasTrait(CharacterTrait.DEAF) && !player.hasTrait(CharacterTrait.DEAF)
                )) {
                character = player;
                pos.set(player.getX(), player.getY());
            }
        }
    }

    @Override
    public void save(ByteBuffer bb) {
        bb.putShort((short)this.alarmList.size());

        for (int i = 0; i < this.alarmList.size(); i++) {
            this.alarmList.get(i).save(bb);
        }
    }

    @Override
    public void load(ByteBuffer bb, int worldVersion) {
        int size = bb.getShort();

        for (int i = 0; i < size; i++) {
            Alarm a = new Alarm(0, 0);
            a.load(bb, worldVersion);
            this.alarmList.add(a);
        }
    }

    public static final class Ambient {
        public float x;
        public float y;
        public String name;
        float radius;
        float volume;
        int worldSoundRadius;
        int worldSoundVolume;
        public boolean trackMouse;
        final FMODSoundEmitter emitter = new FMODSoundEmitter();

        public Ambient(String name, float x, float y, float radius, float volume) {
            this(name, x, y, radius, volume, false);
        }

        public Ambient(String name, float x, float y, float radius, float volume, boolean remote) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.volume = volume;
            this.emitter.x = x;
            this.emitter.y = y;
            this.emitter.z = 0.0F;
            this.emitter.playAmbientSound(name);
            this.update();
            LuaEventManager.triggerEvent("OnAmbientSound", name, x, y);
        }

        public boolean finished() {
            return this.emitter.isEmpty();
        }

        public void update() {
            this.emitter.tick();
            if (this.trackMouse && IsoPlayer.getInstance() != null) {
                float x = Mouse.getXA();
                float y = Mouse.getYA();
                x -= IsoCamera.getScreenLeft(IsoPlayer.getPlayerIndex());
                y -= IsoCamera.getScreenTop(IsoPlayer.getPlayerIndex());
                x *= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
                y *= Core.getInstance().getZoom(IsoPlayer.getPlayerIndex());
                int z = IsoPlayer.getInstance().getZi();
                this.emitter.x = PZMath.fastfloor(IsoUtils.XToIso(x, y, z));
                this.emitter.y = PZMath.fastfloor(IsoUtils.YToIso(x, y, z));
            }

            if (!GameClient.client && this.worldSoundRadius > 0 && this.worldSoundVolume > 0) {
                WorldSoundManager.instance.addSound(null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, this.worldSoundRadius, this.worldSoundVolume);
            }
        }

        public void repeatWorldSounds(int radius, int volume) {
            this.worldSoundRadius = radius;
            this.worldSoundVolume = volume;
        }

        private IsoGameCharacter getClosestListener(float soundX, float soundY) {
            IsoGameCharacter CamCharacter = null;
            float minDist = Float.MAX_VALUE;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoGameCharacter chr = IsoPlayer.players[i];
                if (chr != null && chr.getCurrentSquare() != null) {
                    float px = chr.getX();
                    float py = chr.getY();
                    float distSq = IsoUtils.DistanceToSquared(px, py, soundX, soundY);
                    distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0F);
                    if (chr.hasTrait(CharacterTrait.DEAF)) {
                        distSq = Float.MAX_VALUE;
                    }

                    if (distSq < minDist) {
                        CamCharacter = chr;
                        minDist = distSq;
                    }
                }
            }

            return CamCharacter;
        }
    }

    public static final class AmbientLoop {
        public static float volChangeAmount = 0.01F;
        public float targVol;
        public float currVol;
        public String name;
        public float volumedelta = 1.0F;
        public long channel = -1L;
        public final FMODSoundEmitter emitter = new FMODSoundEmitter();

        public AmbientLoop(float startVol, String name, float volDel) {
            this.volumedelta = volDel;
            this.channel = this.emitter.playAmbientLoopedImpl(name);
            this.targVol = startVol;
            this.currVol = 0.0F;
            this.update();
        }

        public void update() {
            if (this.targVol > this.currVol) {
                this.currVol = this.currVol + volChangeAmount;
                if (this.currVol > this.targVol) {
                    this.currVol = this.targVol;
                }
            }

            if (this.targVol < this.currVol) {
                this.currVol = this.currVol - volChangeAmount;
                if (this.currVol < this.targVol) {
                    this.currVol = this.targVol;
                }
            }

            this.emitter.setVolumeAll(this.currVol * this.volumedelta);
            this.emitter.tick();
        }

        public void stop() {
            this.emitter.stopAll();
        }
    }

    public static final class WorldSoundEmitter {
        public FMODSoundEmitter fmodEmitter;
        public float x;
        public float y;
        public float z;
        public long channel = -1L;
        public String daytime;
        public float dawn;
        public float dusk;
    }
}
