// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.weather.ClimateManager;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleLight;
import zombie.vehicles.VehiclePart;

public final class LightingJNI {
    public static final int ROOM_SPAWN_DIST = 50;
    public static boolean init;
    public static final int[][] ForcedVis = new int[][]{
        {-1, 0, -1, -1, 0, -1, 1, -1, 1, 0, -2, -2, -1, -2, 0, -2, 1, -2, 2, -2},
        {-1, 1, -1, 0, -1, -1, 0, -1, 1, -1, -2, 0, -2, -1, -2, -2, -1, -2, 0, -2},
        {0, 1, -1, 1, -1, 0, -1, -1, 0, -1, -2, 2, -2, 1, -2, 0, -2, -1, -2, -2},
        {1, 1, 0, 1, -1, 1, -1, 0, -1, -1, 0, 2, -1, 2, -2, 2, -2, 1, -2, 0},
        {1, 0, 1, 1, 0, 1, -1, 1, -1, 0, 2, 2, 1, 2, 0, 2, -1, 2, -2, 2},
        {-1, 1, 0, 1, 1, 1, 1, 0, 1, -1, 2, 0, 2, 1, 2, 2, 1, 2, 0, 2},
        {0, 1, 1, 1, 1, 0, 1, -1, 0, -1, 2, -2, 2, -1, 2, 0, 2, 1, 2, 2},
        {-1, -1, 0, -1, 1, -1, 1, 0, 1, 1, 0, -2, 1, -2, 2, -2, 2, -1, 2, 0}
    };
    private static final ArrayList<IsoGameCharacter.TorchInfo> torches = new ArrayList<>();
    private static final ArrayList<IsoGameCharacter.TorchInfo> activeTorches = new ArrayList<>();
    private static final ArrayList<IsoLightSource> JNILights = new ArrayList<>();
    private static final int[] updateCounter = new int[4];
    private static final int[] buildingsChangedCounter = new int[4];
    private static boolean wasElecShut;
    private static boolean wasNight;
    private static final Vector2 tempVector2 = new Vector2();
    private static final int MAX_PLAYERS = 256;
    private static final int MAX_LIGHTS_PER_PLAYER = 4;
    private static final int MAX_LIGHTS_PER_VEHICLE = 10;
    private static final ArrayList<InventoryItem> tempItems = new ArrayList<>();
    private static final ArrayList<LightingJNI.VisibleRoom>[] visibleRooms = new ArrayList[4];
    private static long[] visibleRoomIDs = new long[32];
    private static CompletableFuture<Void> checkLightsFuture;
    static float visionConeLerp;
    static float lumaInvertedLerp;
    static final ColorInfo lightTransmissionW;
    static final ColorInfo lightTransmissionN;
    static final ColorInfo lightTransmissionE;
    static final ColorInfo lightTransmissionS;

    public static void doInvalidateGlobalLights(int playerIndex) {
        Core.dirtyGlobalLightsCount++;
    }

    public static void init() {
        if (!init) {
            String suffix = "";
            if ("1".equals(System.getProperty("zomboid.debuglibs.lighting"))) {
                DebugLog.log("***** Loading debug version of Lighting");
                suffix = "d";
            }

            try {
                if (System.getProperty("os.name").contains("OS X")) {
                    System.loadLibrary("Lighting");
                } else if (System.getProperty("os.name").startsWith("Win")) {
                    System.loadLibrary("Lighting64" + suffix);
                } else {
                    System.loadLibrary("Lighting64");
                }

                for (int pn = 0; pn < 4; pn++) {
                    updateCounter[pn] = -1;
                }

                configure(0.005F);
                init = true;
            } catch (UnsatisfiedLinkError var4) {
                var4.printStackTrace();

                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException var3) {
                }

                System.exit(1);
            }
        }
    }

    private static int getTorchIndexById(int id) {
        for (int i = 0; i < torches.size(); i++) {
            IsoGameCharacter.TorchInfo torchInfo = torches.get(i);
            if (torchInfo.id == id) {
                return i;
            }
        }

        return -1;
    }

    private static void checkTorch(IsoPlayer p, InventoryItem item, int id) {
        int torchIndex = getTorchIndexById(id);
        IsoGameCharacter.TorchInfo torchInfo;
        if (torchIndex == -1) {
            torchInfo = IsoGameCharacter.TorchInfo.alloc();
            torches.add(torchInfo);
        } else {
            torchInfo = torches.get(torchIndex);
        }

        torchInfo.set(p, item);
        if (torchInfo.id == 0) {
            torchInfo.id = id;
        }

        updateTorch(
            torchInfo.id,
            torchInfo.x,
            torchInfo.y,
            torchInfo.z + 32.0F,
            torchInfo.angleX,
            torchInfo.angleY,
            torchInfo.dist,
            torchInfo.strength,
            torchInfo.cone,
            torchInfo.dot,
            torchInfo.focusing
        );
        activeTorches.add(torchInfo);
    }

    private static int checkPlayerTorches(IsoPlayer player, int playerIndex) {
        ArrayList<InventoryItem> lightItems = tempItems;
        lightItems.clear();
        player.getActiveLightItems(lightItems);
        int numItems = Math.min(lightItems.size(), 4);

        for (int i = 0; i < numItems; i++) {
            checkTorch(player, lightItems.get(i), playerIndex * 4 + i + 1);
        }

        return numItems;
    }

    private static void clearPlayerTorches(int playerIndex, int numItems) {
        for (int i = numItems; i < 4; i++) {
            int id = playerIndex * 4 + i + 1;
            int torchIndex = getTorchIndexById(id);
            if (torchIndex != -1) {
                IsoGameCharacter.TorchInfo torchInfo = torches.get(torchIndex);
                removeTorch(torchInfo.id);
                torchInfo.id = 0;
                IsoGameCharacter.TorchInfo.release(torchInfo);
                torches.remove(torchIndex);
                break;
            }
        }
    }

    private static void checkTorch(VehiclePart part, int id) {
        VehicleLight light = part.getLight();
        if (light != null && light.getActive()) {
            IsoGameCharacter.TorchInfo torchInfo = null;

            for (int j = 0; j < torches.size(); j++) {
                torchInfo = torches.get(j);
                if (torchInfo.id == id) {
                    break;
                }

                torchInfo = null;
            }

            if (torchInfo == null) {
                torchInfo = IsoGameCharacter.TorchInfo.alloc();
                torches.add(torchInfo);
            }

            torchInfo.set(part);
            if (torchInfo.id == 0) {
                torchInfo.id = id;
            }

            updateTorch(
                torchInfo.id,
                torchInfo.x,
                torchInfo.y,
                torchInfo.z + 32.0F,
                torchInfo.angleX,
                torchInfo.angleY,
                torchInfo.dist,
                torchInfo.strength,
                torchInfo.cone,
                torchInfo.dot,
                torchInfo.focusing
            );
            activeTorches.add(torchInfo);
        } else {
            for (int j = 0; j < torches.size(); j++) {
                IsoGameCharacter.TorchInfo torchInfo = torches.get(j);
                if (torchInfo.id == id) {
                    removeTorch(torchInfo.id);
                    torchInfo.id = 0;
                    IsoGameCharacter.TorchInfo.release(torchInfo);
                    torches.remove(j--);
                }
            }
        }
    }

    private static void checkLights() {
        if (IsoWorld.instance.currentCell != null) {
            if (GameClient.client) {
                IsoGenerator.updateSurroundingNow();
            }

            boolean bHydroPower = IsoWorld.instance.isHydroPowerOn();
            Stack<IsoLightSource> lights = IsoWorld.instance.currentCell.getLamppostPositions();

            for (int i = 0; i < lights.size(); i++) {
                IsoLightSource lightSource = lights.get(i);
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(lightSource.x, lightSource.y, lightSource.z);
                if (chunk != null && lightSource.chunk != null && lightSource.chunk != chunk) {
                    lightSource.life = 0;
                }

                if (lightSource.life != 0 && lightSource.isInBounds()) {
                    if (lightSource.hydroPowered) {
                        if (lightSource.switches.isEmpty()) {
                            assert false;

                            boolean hasPower = bHydroPower;
                            if (!bHydroPower) {
                                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(lightSource.x, lightSource.y, lightSource.z);
                                hasPower = sq != null && sq.haveElectricity();
                            }

                            if (lightSource.active != hasPower) {
                                lightSource.active = hasPower;
                                GameTime.instance.lightSourceUpdate = 100.0F;
                            }
                        } else {
                            IsoLightSwitch lightSwitch = lightSource.switches.get(0);
                            boolean hasPowerx = lightSwitch.canSwitchLight();
                            if (lightSwitch.streetLight && (GameTime.getInstance().getNight() < 0.5F || !lightSwitch.hasGridPower())) {
                                hasPowerx = false;
                            }

                            if (lightSource.active && !hasPowerx) {
                                lightSource.active = false;
                                GameTime.instance.lightSourceUpdate = 100.0F;
                            } else if (!lightSource.active && hasPowerx && lightSwitch.isActivated()) {
                                lightSource.active = true;
                                GameTime.instance.lightSourceUpdate = 100.0F;
                            }
                        }
                    }

                    float LIGHT_MULT = 2.0F;
                    if (lightSource.id == 0) {
                        lightSource.id = IsoLightSource.nextId++;
                        if (lightSource.life != -1) {
                            addTempLight(
                                lightSource.id,
                                lightSource.x,
                                lightSource.y,
                                lightSource.z + 32,
                                lightSource.radius,
                                lightSource.r,
                                lightSource.g,
                                lightSource.b,
                                (int)(lightSource.life * PerformanceSettings.getLockFPS() / 30.0F)
                            );
                            lights.remove(i--);
                        } else {
                            lightSource.rJni = lightSource.r;
                            lightSource.gJni = lightSource.g;
                            lightSource.bJni = lightSource.b;
                            lightSource.activeJni = lightSource.active;
                            JNILights.add(lightSource);
                            addLight(
                                lightSource.id,
                                lightSource.x,
                                lightSource.y,
                                lightSource.z + 32,
                                PZMath.min(lightSource.radius, 20),
                                PZMath.clamp(lightSource.r * 2.0F, 0.0F, 1.0F),
                                PZMath.clamp(lightSource.g * 2.0F, 0.0F, 1.0F),
                                PZMath.clamp(lightSource.b * 2.0F, 0.0F, 1.0F),
                                lightSource.localToBuilding == null ? -1 : lightSource.localToBuilding.id,
                                lightSource.active
                            );
                        }
                    } else {
                        if (lightSource.r != lightSource.rJni || lightSource.g != lightSource.gJni || lightSource.b != lightSource.bJni) {
                            lightSource.rJni = lightSource.r;
                            lightSource.gJni = lightSource.g;
                            lightSource.bJni = lightSource.b;
                            setLightColor(
                                lightSource.id,
                                PZMath.clamp(lightSource.r * 2.0F, 0.0F, 1.0F),
                                PZMath.clamp(lightSource.g * 2.0F, 0.0F, 1.0F),
                                PZMath.clamp(lightSource.b * 2.0F, 0.0F, 1.0F)
                            );
                        }

                        if (lightSource.activeJni != lightSource.active) {
                            lightSource.activeJni = lightSource.active;
                            setLightActive(lightSource.id, lightSource.active);
                        }
                    }
                } else {
                    lights.remove(i);
                    if (lightSource.id != 0) {
                        int ID = lightSource.id;
                        lightSource.id = 0;
                        JNILights.remove(lightSource);
                        removeLight(ID);
                        GameTime.instance.lightSourceUpdate = 100.0F;
                    }

                    i--;
                }
            }

            for (int i = 0; i < JNILights.size(); i++) {
                IsoLightSource lightSourcex = JNILights.get(i);
                if (!lights.contains(lightSourcex)) {
                    int ID = lightSourcex.id;
                    lightSourcex.id = 0;
                    JNILights.remove(i--);
                    removeLight(ID);
                }
            }

            ArrayList<IsoRoomLight> roomLights = IsoWorld.instance.currentCell.roomLights;

            for (int ix = 0; ix < roomLights.size(); ix++) {
                IsoRoomLight roomLight = roomLights.get(ix);
                if (!roomLight.isInBounds()) {
                    roomLights.remove(ix--);
                    if (roomLight.id != 0) {
                        int ID = roomLight.id;
                        roomLight.id = 0;
                        removeRoomLight(ID);
                        GameTime.instance.lightSourceUpdate = 100.0F;
                    }
                } else {
                    roomLight.active = roomLight.room.def.lightsActive;
                    if (!bHydroPower) {
                        boolean switchHasPower = false;

                        for (int j = 0; !switchHasPower && j < roomLight.room.lightSwitches.size(); j++) {
                            IsoLightSwitch lightSwitchx = roomLight.room.lightSwitches.get(j);
                            if (lightSwitchx.square != null && lightSwitchx.square.haveElectricity()) {
                                switchHasPower = true;
                            }
                        }

                        if (!switchHasPower && roomLight.active) {
                            roomLight.active = false;
                            if (roomLight.activeJni) {
                                IsoGridSquare.recalcLightTime = -1.0F;
                                if (PerformanceSettings.fboRenderChunk) {
                                    Core.dirtyGlobalLightsCount++;
                                }

                                GameTime.instance.lightSourceUpdate = 100.0F;
                            }
                        } else if (switchHasPower && roomLight.active && !roomLight.activeJni) {
                            IsoGridSquare.recalcLightTime = -1.0F;
                            if (PerformanceSettings.fboRenderChunk) {
                                Core.dirtyGlobalLightsCount++;
                            }

                            GameTime.instance.lightSourceUpdate = 100.0F;
                        }
                    }

                    if (roomLight.id == 0) {
                        roomLight.id = 100000 + IsoRoomLight.nextId++;
                        addRoomLight(
                            roomLight.id,
                            roomLight.room.building.def.id,
                            roomLight.room.def.id,
                            roomLight.x,
                            roomLight.y,
                            roomLight.z + 32,
                            roomLight.width,
                            roomLight.height,
                            roomLight.active
                        );
                        roomLight.activeJni = roomLight.active;
                        GameTime.instance.lightSourceUpdate = 100.0F;
                    } else if (roomLight.activeJni != roomLight.active) {
                        setRoomLightActive(roomLight.id, roomLight.active);
                        roomLight.activeJni = roomLight.active;
                        GameTime.instance.lightSourceUpdate = 100.0F;
                    }
                }
            }

            activeTorches.clear();
            if (GameClient.client) {
                ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();

                for (int ixx = 0; ixx < players.size(); ixx++) {
                    IsoPlayer p = players.get(ixx);
                    checkPlayerTorches(p, p.onlineId + 1);
                }
            } else {
                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null && !player.isDead() && (player.getVehicle() == null || player.isAiming())) {
                        int numItems = checkPlayerTorches(player, playerIndex);
                        clearPlayerTorches(playerIndex, numItems);
                    } else {
                        clearPlayerTorches(playerIndex, 0);
                    }
                }
            }

            for (int ixx = 0; ixx < IsoWorld.instance.currentCell.getVehicles().size(); ixx++) {
                BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(ixx);
                if (vehicle.vehicleId != -1) {
                    for (int jx = 0; jx < vehicle.getLightCount(); jx++) {
                        VehiclePart part = vehicle.getLightByIndex(jx);
                        checkTorch(part, 1024 + vehicle.vehicleId * 10 + jx);
                    }
                }
            }

            for (int ixxx = 0; ixxx < torches.size(); ixxx++) {
                IsoGameCharacter.TorchInfo torchInfo = torches.get(ixxx);
                if (!activeTorches.contains(torchInfo)) {
                    removeTorch(torchInfo.id);
                    torchInfo.id = 0;
                    IsoGameCharacter.TorchInfo.release(torchInfo);
                    torches.remove(ixxx--);
                }
            }
        }
    }

    private static float coneToDegrees(float cone) {
        return 180.0F + cone * 180.0F;
    }

    private static float degreesToCone(float degrees) {
        return (degrees - 180.0F) / 180.0F;
    }

    public static float calculateVisionConeOld(IsoGameCharacter player) {
        float cone;
        if (player.getVehicle() == null) {
            cone = -0.2F;
            cone -= player.getStats().get(CharacterStat.FATIGUE) - 0.6F;
            if (cone > -0.2F) {
                cone = -0.2F;
            }

            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 0.2F;
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= player.getStats().get(CharacterStat.INTOXICATION) * 0.002F;
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 0.2F;
            }

            if (player.isInARoom()) {
                ColorInfo light = player.square.getLightInfo(IsoPlayer.getPlayerIndex());
                float luma = light.r * 0.299F + light.g * 0.587F + light.b * 0.114F;
                cone -= 0.7F * (1.0F - luma);
            } else {
                cone -= 0.7F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
            }

            if (cone < -0.9F) {
                cone = -0.9F;
            }

            if (player.hasTrait(CharacterTrait.EAGLE_EYED)) {
                cone += 0.2F * ClimateManager.getInstance().getDayLightStrength();
            }

            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 0.2F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
            }

            DebugLog.Lightning.debugln("cone 1 %f", cone);
            cone *= player.getWornItemsVisionModifier();
            DebugLog.Lightning.debugln("cone 2 %f", cone);
            if (cone > 0.0F) {
                cone = 0.0F;
            } else if (cone < -0.9F) {
                cone = -0.9F;
            }

            DebugLog.Lightning.debugln("cone 3 %f", cone);
        } else {
            cone = 0.8F - 3.0F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 0.2F * (1.0F - ClimateManager.getInstance().getDayLightStrength());
            }

            cone *= player.getWornItemsVisionModifier();
            if (cone > 1.0F) {
                cone = 1.0F;
            }

            if (player.getVehicle().getHeadlightsOn() && player.getVehicle().getHeadlightCanEmmitLight() && cone < -0.8F) {
                cone = -0.8F;
            } else if (cone < -0.95F) {
                cone = -0.95F;
            }
        }

        return cone;
    }

    public static float calculateVisionCone(IsoGameCharacter player) {
        float dayLightStrength = ClimateManager.getInstance().getDayLightStrength();
        float dayLightStrengthInverted = 1.0F - dayLightStrength;
        float cone;
        if (player.getVehicle() == null) {
            cone = 144.0F;
            cone -= 72.0F * player.getStats().get(CharacterStat.FATIGUE);
            if (cone > 144.0F) {
                cone = 144.0F;
            }

            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 36.0F;
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= 0.36F * player.getStats().get(CharacterStat.INTOXICATION);
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 36.0F;
            }

            float lumaInverted = getLumaInverted(player);
            if (!PZMath.equal(lumaInvertedLerp, lumaInverted, 0.1F)) {
                lumaInvertedLerp = PZMath.lerp(lumaInvertedLerp, lumaInverted, 0.1F);
            }

            if (player.isInARoom()) {
                cone -= 126.0F * lumaInvertedLerp;
            } else {
                cone -= 126.0F * PZMath.min(dayLightStrengthInverted, lumaInvertedLerp);
            }

            cone = PZMath.clamp(cone, 18.0F, 180.0F);
            if (player.hasTrait(CharacterTrait.EAGLE_EYED)) {
                cone += 36.0F * dayLightStrength;
            }

            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 36.0F * dayLightStrengthInverted;
            }
        } else {
            cone = 324.0F;
            cone -= 540.0F * dayLightStrengthInverted;
            if (player.getStats().isAtMaximum(CharacterStat.FATIGUE)) {
                cone -= 36.0F;
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.DRUNK) >= 2) {
                cone -= 0.36F * player.getStats().get(CharacterStat.INTOXICATION);
            }

            if (player.getMoodles().getMoodleLevel(MoodleType.PANIC) == 4) {
                cone -= 36.0F;
            }

            if (player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                cone += 36.0F * dayLightStrengthInverted;
            }

            if (player.getVehicle().getHeadlightsOn() && player.getVehicle().getHeadlightCanEmmitLight() && cone < 36.0F) {
                cone = 36.0F;
            }
        }

        cone *= player.getWornItemsVisionMultiplier();
        cone = PZMath.clamp(cone, 18.0F, 360.0F);
        if (!PZMath.equal(visionConeLerp, cone, 0.033F)) {
            visionConeLerp = PZMath.lerp(visionConeLerp, cone, 0.033F);
        }

        return degreesToCone(visionConeLerp);
    }

    private static float getLumaInverted(IsoGameCharacter player) {
        IsoGridSquare testSquare = player.getSquare() != null ? player.getSquare() : null;
        IsoGridSquare testSquare2 = testSquare != null && player.getDir() != null ? testSquare.getAdjacentSquare(player.getDir()) : null;
        ColorInfo light = testSquare != null ? testSquare.getLightInfo(IsoPlayer.getPlayerIndex()) : null;
        ColorInfo light2 = testSquare2 != null ? testSquare2.getLightInfo(IsoPlayer.getPlayerIndex()) : null;
        float lightValue = light != null ? light.r * 0.299F + light.g * 0.587F + light.b * 0.114F : 0.0F;
        float lightValue2 = light2 != null ? light2.r * 0.299F + light2.g * 0.587F + light2.b * 0.114F : 0.0F;
        return 1.0F - PZMath.max(lightValue, lightValue2);
    }

    public static float calculateRearZombieDistance(IsoGameCharacter player) {
        return player.getSeeNearbyCharacterDistance();
    }

    public static void updatePlayer(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null) {
            float tireddel = player.getStats().get(CharacterStat.FATIGUE) - 0.6F;
            if (tireddel < 0.0F) {
                tireddel = 0.0F;
            }

            tireddel *= 2.5F;
            float ndist = 2.0F;
            if (player.hasTrait(CharacterTrait.HARD_OF_HEARING)) {
                ndist--;
            }

            if (player.hasTrait(CharacterTrait.KEEN_HEARING)) {
                ndist += 3.0F;
            }

            ndist *= player.getWornItemsHearingMultiplier();
            float cone = calculateVisionCone(player);
            Vector2 lookVector = player.getLookVector(tempVector2);
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle != null
                && !player.isAiming()
                && !player.isLookingWhileInVehicle()
                && vehicle.isDriver(player)
                && vehicle.getCurrentSpeedKmHour() < -1.0F) {
                lookVector.rotate((float) Math.PI);
            }

            playerSet(
                player.getX(),
                player.getY(),
                player.getZ() + 32.0F,
                lookVector.x,
                lookVector.y,
                false,
                player.reanimatedCorpse != null,
                player.isGhostMode(),
                player.hasTrait(CharacterTrait.SHORT_SIGHTED),
                tireddel,
                ndist,
                cone
            );
        }
    }

    public static void updateChunk(int playerIndex, IsoChunk mchunk) {
        chunkBeginUpdate(mchunk.wx, mchunk.wy, mchunk.getMinLevel() + 32, mchunk.getMaxLevel() + 32);

        for (int z = mchunk.getMinLevel(); z <= mchunk.getMaxLevel(); z++) {
            IsoChunkLevel chunkLevel = mchunk.getLevelData(z);
            if (chunkLevel.lightCheck[playerIndex]) {
                chunkLevel.lightCheck[playerIndex] = false;
                chunkLevelBeginUpdate(z + 32);

                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        IsoGridSquare sq = chunkLevel.squares[x + y * 8];
                        if (sq != null) {
                            squareBeginUpdate(x, y, z + 32);
                            int visionMatrix = sq.visionMatrix;
                            if (sq.isSeen(playerIndex)) {
                                visionMatrix |= 1 << 27 + playerIndex;
                            }

                            boolean isOpenAir = sq.getOpenAir();
                            if (isOpenAir) {
                                sq.lightLevel = GameTime.getInstance().getSkyLightLevel();
                            }

                            boolean hasElevatedFloor = sq.has(IsoObjectType.stairsTN)
                                || sq.has(IsoObjectType.stairsMN)
                                || sq.has(IsoObjectType.stairsTW)
                                || sq.has(IsoObjectType.stairsMW);
                            int visionUnblocked = 0;

                            for (int i = 0; i < 8; i++) {
                                IsoDirections dir = IsoDirections.fromIndex(i);
                                if (sq.testVisionAdjacent(dir.dx(), dir.dy(), 0, true, false) != LosUtil.TestResults.Blocked) {
                                    visionUnblocked |= 1 << i;
                                }
                            }

                            squareSet(
                                visionUnblocked,
                                sq.testVisionAdjacent(0, 0, 1, true, false) != LosUtil.TestResults.Blocked,
                                sq.testVisionAdjacent(0, 0, -1, true, false) != LosUtil.TestResults.Blocked,
                                hasElevatedFloor,
                                visionMatrix,
                                sq.getRoom() == null ? -1L : sq.getBuilding().getDef().getID(),
                                sq.getRoom() == null ? -1L : sq.getRoomID(),
                                sq.lightLevel,
                                isOpenAir
                            );
                            float intensityW = Float.MAX_VALUE;
                            float intensityN = Float.MAX_VALUE;
                            float intensityE = Float.MAX_VALUE;
                            float intensityS = Float.MAX_VALUE;
                            float keepW = 0.0F;
                            float keepN = 0.0F;
                            float keepE = 0.0F;
                            float keepS = 0.0F;
                            ColorInfo ltW = lightTransmissionW.setRGB(0.0F);
                            ColorInfo ltN = lightTransmissionN.setRGB(0.0F);
                            ColorInfo ltE = lightTransmissionE.setRGB(0.0F);
                            ColorInfo ltS = lightTransmissionS.setRGB(0.0F);

                            for (int ix = 0; ix < sq.getSpecialObjects().size(); ix++) {
                                IsoObject object = sq.getSpecialObjects().get(ix);
                                if (object instanceof IsoCurtain curtain) {
                                    float CURTAIN_R = 0.0F;
                                    float CURTAIN_G = 0.0F;
                                    float CURTAIN_B = 0.0F;
                                    String LightFilterR = curtain.getProperties().get("LightFilterR");
                                    String LightFilterG = curtain.getProperties().get("LightFilterG");
                                    String LightFilterB = curtain.getProperties().get("LightFilterB");
                                    if (LightFilterR != null) {
                                        CURTAIN_R = PZMath.clamp(PZMath.tryParseInt(LightFilterR, 0), 0, 255) / 255.0F;
                                    }

                                    if (LightFilterG != null) {
                                        CURTAIN_G = PZMath.clamp(PZMath.tryParseInt(LightFilterG, 0), 0, 255) / 255.0F;
                                    }

                                    if (LightFilterB != null) {
                                        CURTAIN_B = PZMath.clamp(PZMath.tryParseInt(LightFilterB, 0), 0, 255) / 255.0F;
                                    }

                                    float CURTAIN_INTENSITY = 0.33F;
                                    String LightFilterIntensity = curtain.getProperties().get("LightFilterIntensity");
                                    if (LightFilterIntensity != null) {
                                        CURTAIN_INTENSITY = PZMath.max(PZMath.tryParseInt(LightFilterIntensity, 0), 0) / 100.0F;
                                    }

                                    String LightFilterMix = curtain.getProperties().get("LightFilterMix");
                                    if (LightFilterMix != null) {
                                        CURTAIN_INTENSITY = PZMath.max(PZMath.tryParseInt(LightFilterMix, 0), 0) / 100.0F;
                                    }

                                    int wnes = 0;
                                    if (curtain.getType() == IsoObjectType.curtainW) {
                                        wnes |= 4;
                                        if (!curtain.IsOpen()) {
                                            intensityW = PZMath.min(intensityW, CURTAIN_INTENSITY);
                                            ltW.setRGB(CURTAIN_R, CURTAIN_G, CURTAIN_B);
                                        }
                                    } else if (curtain.getType() == IsoObjectType.curtainN) {
                                        wnes |= 8;
                                        if (!curtain.IsOpen()) {
                                            intensityN = PZMath.min(intensityN, CURTAIN_INTENSITY);
                                            ltN.setRGB(CURTAIN_R, CURTAIN_G, CURTAIN_B);
                                        }
                                    } else if (curtain.getType() == IsoObjectType.curtainE) {
                                        wnes |= 16;
                                        if (!curtain.IsOpen()) {
                                            intensityE = PZMath.min(intensityE, CURTAIN_INTENSITY);
                                            ltE.setRGB(CURTAIN_R, CURTAIN_G, CURTAIN_B);
                                        }
                                    } else if (curtain.getType() == IsoObjectType.curtainS) {
                                        wnes |= 32;
                                        if (!curtain.IsOpen()) {
                                            intensityS = PZMath.min(intensityS, CURTAIN_INTENSITY);
                                            ltS.setRGB(CURTAIN_R, CURTAIN_G, CURTAIN_B);
                                        }
                                    }

                                    squareAddCurtain(wnes, curtain.open);
                                } else if (object instanceof IsoDoor door) {
                                    boolean trans = door.sprite != null && door.sprite.getProperties().has("doorTrans");
                                    if (door.open) {
                                        trans = true;
                                    } else {
                                        trans = trans && (door.HasCurtains() == null || door.isCurtainOpen());
                                    }

                                    IsoBarricade barricade1 = door.getBarricadeOnSameSquare();
                                    IsoBarricade barricade2 = door.getBarricadeOnOppositeSquare();
                                    if (barricade1 != null && barricade1.isBlockVision()) {
                                        trans = false;
                                    }

                                    if (barricade2 != null && barricade2.isBlockVision()) {
                                        trans = false;
                                    }

                                    if (door.IsOpen() && IsoDoor.getGarageDoorIndex(door) != -1) {
                                        trans = true;
                                    }

                                    squareAddDoor(door.north, door.open, trans);
                                    if (!door.open && door.HasCurtains() == null) {
                                        if (door.getNorth()) {
                                            intensityN = PZMath.min(intensityN, 0.15F);
                                        } else {
                                            intensityW = PZMath.min(intensityW, 0.15F);
                                        }
                                    }

                                    if (!door.open && door.HasCurtains() != null && !door.isCurtainOpen()) {
                                        float CURTAIN_Rx = 0.0F;
                                        float CURTAIN_Gx = 0.0F;
                                        float CURTAIN_Bx = 0.0F;
                                        float CURTAIN_INTENSITYx = 0.33F;
                                        if (door.getNorth()) {
                                            intensityN = PZMath.min(intensityN, 0.33F);
                                            ltN.setRGB(0.0F, 0.0F, 0.0F);
                                        } else {
                                            intensityW = PZMath.min(intensityW, 0.33F);
                                            ltW.setRGB(0.0F, 0.0F, 0.0F);
                                        }
                                    }
                                } else if (object instanceof IsoThumpable thump) {
                                    boolean doorTrans = thump.getSprite().getProperties().has("doorTrans");
                                    if (thump.isDoor() && thump.open) {
                                        doorTrans = true;
                                    }

                                    squareAddThumpable(thump.north, thump.open, thump.isDoor(), doorTrans);
                                    boolean opaque = false;
                                    IsoBarricade barricade1x = thump.getBarricadeOnSameSquare();
                                    IsoBarricade barricade2x = thump.getBarricadeOnOppositeSquare();
                                    if (barricade1x != null) {
                                        opaque |= barricade1x.isBlockVision();
                                        if (thump.getNorth()) {
                                            intensityN = PZMath.min(intensityN, barricade1x.getLightTransmission());
                                        } else {
                                            intensityW = PZMath.min(intensityW, barricade1x.getLightTransmission());
                                        }
                                    }

                                    if (barricade2x != null) {
                                        opaque |= barricade2x.isBlockVision();
                                        if (thump.getNorth()) {
                                            intensityS = PZMath.min(intensityS, barricade2x.getLightTransmission());
                                        } else {
                                            intensityE = PZMath.min(intensityE, barricade2x.getLightTransmission());
                                        }
                                    }

                                    squareAddWindow(thump.north, thump.open, opaque);
                                } else if (object instanceof IsoWindow window) {
                                    boolean opaquex = false;
                                    IsoBarricade barricade1xx = window.getBarricadeOnSameSquare();
                                    IsoBarricade barricade2xx = window.getBarricadeOnOppositeSquare();
                                    if (barricade1xx != null) {
                                        opaquex |= barricade1xx.isBlockVision();
                                        if (window.getNorth()) {
                                            intensityN = PZMath.min(intensityN, barricade1xx.getLightTransmission());
                                        } else {
                                            intensityW = PZMath.min(intensityW, barricade1xx.getLightTransmission());
                                        }
                                    }

                                    if (barricade2xx != null) {
                                        opaquex |= barricade2xx.isBlockVision();
                                        if (window.getNorth()) {
                                            intensityS = PZMath.min(intensityS, barricade2xx.getLightTransmission());
                                        } else {
                                            intensityE = PZMath.min(intensityE, barricade2xx.getLightTransmission());
                                        }
                                    }

                                    squareAddWindow(window.isNorth(), window.IsOpen(), opaquex);
                                }
                            }

                            if (intensityW == Float.MAX_VALUE) {
                                intensityW = 0.0F;
                            }

                            if (intensityN == Float.MAX_VALUE) {
                                intensityN = 0.0F;
                            }

                            if (intensityE == Float.MAX_VALUE) {
                                intensityE = 0.0F;
                            }

                            if (intensityS == Float.MAX_VALUE) {
                                intensityS = 0.0F;
                            }

                            squareSetLightTransmission(
                                ltW.r,
                                ltW.g,
                                ltW.b,
                                intensityW,
                                0.0F,
                                ltN.r,
                                ltN.g,
                                ltN.b,
                                intensityN,
                                0.0F,
                                ltE.r,
                                ltE.g,
                                ltE.b,
                                intensityE,
                                0.0F,
                                ltS.r,
                                ltS.g,
                                ltS.b,
                                intensityS,
                                0.0F
                            );
                            squareEndUpdate();
                        } else {
                            squareSetNull(x, y, z + 32);
                        }
                    }
                }

                chunkLevelEndUpdate();
            }
        }

        chunkEndUpdate();
    }

    public static void preUpdate() {
        if (DebugOptions.instance.threadLighting.getValue()) {
            checkLightsFuture = CompletableFuture.runAsync(LightingJNI::checkLights, PZForkJoinPool.commonPool());
        }
    }

    public static void update() {
        if (IsoWorld.instance != null && IsoWorld.instance.currentCell != null) {
            GameProfiler profiler = GameProfiler.getInstance();
            if (checkLightsFuture != null) {
                try (GameProfiler.ProfileArea ignored = profiler.profile("checkLights")) {
                    checkLightsFuture.join();
                }
            } else {
                try (GameProfiler.ProfileArea ignored = profiler.profile("checkLights")) {
                    checkLights();
                }
            }

            checkLightsFuture = null;
            GameTime gameTime = GameTime.getInstance();
            RenderSettings renderSettings = RenderSettings.getInstance();
            boolean bElecShut = IsoWorld.instance.isHydroPowerOn();
            boolean bNight = GameTime.getInstance().getNight() < 0.5F;
            if (bElecShut != wasElecShut || bNight != wasNight) {
                wasElecShut = bElecShut;
                wasNight = bNight;
                IsoGridSquare.recalcLightTime = -1.0F;
                if (PerformanceSettings.fboRenderChunk) {
                    Core.dirtyGlobalLightsCount++;
                }

                gameTime.lightSourceUpdate = 100.0F;
            }

            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[playerIndex];
                if (cm != null && !cm.ignore) {
                    RenderSettings.PlayerRenderSettings plrSettings = renderSettings.getPlayerSettings(playerIndex);
                    stateBeginUpdate(playerIndex, cm.getWorldXMin(), cm.getWorldYMin(), IsoChunkMap.chunkGridWidth, IsoChunkMap.chunkGridWidth);
                    updatePlayer(playerIndex);
                    stateEndFrame(
                        plrSettings.getRmod(),
                        plrSettings.getGmod(),
                        plrSettings.getBmod(),
                        plrSettings.getAmbient(),
                        plrSettings.getNight(),
                        plrSettings.getViewDistance(),
                        gameTime.getViewDistMax(),
                        LosUtil.cachecleared[playerIndex],
                        gameTime.lightSourceUpdate,
                        GameTime.getInstance().getSkyLightLevel()
                    );
                    if (LosUtil.cachecleared[playerIndex]) {
                        LosUtil.cachecleared[playerIndex] = false;
                        IsoWorld.instance.currentCell.invalidatePeekedRoom(playerIndex);
                    }

                    for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
                        for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                            IsoChunk mchunk = cm.getChunk(cx, cy);
                            if (mchunk != null && mchunk.loaded) {
                                if (mchunk.lightCheck[playerIndex]) {
                                    updateChunk(playerIndex, mchunk);
                                    mchunk.lightCheck[playerIndex] = false;
                                }

                                mchunk.lightingNeverDone[playerIndex] = !chunkLightingDone(mchunk.wx, mchunk.wy);
                            }
                        }
                    }

                    stateEndUpdate();
                    updateCounter[playerIndex] = stateUpdateCounter(playerIndex);
                    if (gameTime.lightSourceUpdate > 0.0F && IsoPlayer.players[playerIndex] != null) {
                        IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 20.0F;
                    }
                }
            }

            try (GameProfiler.ProfileArea ignored = profiler.profile("DeadBodyAtlas")) {
                DeadBodyAtlas.instance.lightingUpdate(updateCounter[0], gameTime.lightSourceUpdate > 0.0F);
            }

            gameTime.lightSourceUpdate = 0.0F;
            updateVisibleRooms();
            checkChangedBuildings();
        }
    }

    public static void getTorches(ArrayList<IsoGameCharacter.TorchInfo> out) {
        out.addAll(torches);
    }

    public static int getUpdateCounter(int playerIndex) {
        return updateCounter[playerIndex];
    }

    public static void stop() {
        torches.clear();
        JNILights.clear();
        destroy();

        for (int i = 0; i < updateCounter.length; i++) {
            updateCounter[i] = -1;
        }

        wasElecShut = false;
        wasNight = false;
        IsoLightSource.nextId = 1;
        IsoRoomLight.nextId = 1;
    }

    public static native void configure(float var0);

    public static native void scrollLeft(int var0);

    public static native void scrollRight(int var0);

    public static native void scrollUp(int var0);

    public static native void scrollDown(int var0);

    public static native void stateBeginUpdate(int var0, int var1, int var2, int var3, int var4);

    public static native void stateEndFrame(
        float var0, float var1, float var2, float var3, float var4, float var5, float var6, boolean var7, float var8, int var9
    );

    public static native void stateEndUpdate();

    public static native int stateUpdateCounter(int var0);

    public static native void teleport(int var0, int var1, int var2);

    public static native void DoLightingUpdateNew(long var0, boolean var2);

    public static native boolean WaitingForMain();

    public static native void playerSet(
        float var0,
        float var1,
        float var2,
        float var3,
        float var4,
        boolean var5,
        boolean var6,
        boolean var7,
        boolean var8,
        float var9,
        float var10,
        float var11
    );

    public static native boolean chunkLightingDone(int var0, int var1);

    public static native boolean getChunkDirty(int var0, int var1, int var2, int var3);

    public static native void chunkBeginUpdate(int var0, int var1, int var2, int var3);

    public static native void chunkEndUpdate();

    public static native void chunkLevelBeginUpdate(int var0);

    public static native void chunkLevelEndUpdate();

    public static native void squareSetNull(int var0, int var1, int var2);

    public static native void squareBeginUpdate(int var0, int var1, int var2);

    public static native void squareSet(int var0, boolean var1, boolean var2, boolean var3, int var4, long var5, long var7, int var9, boolean var10);

    public static native void squareSetLightTransmission(
        float var0,
        float var1,
        float var2,
        float var3,
        float var4,
        float var5,
        float var6,
        float var7,
        float var8,
        float var9,
        float var10,
        float var11,
        float var12,
        float var13,
        float var14,
        float var15,
        float var16,
        float var17,
        float var18,
        float var19
    );

    public static native void squareAddCurtain(int var0, boolean var1);

    public static native void squareAddDoor(boolean var0, boolean var1, boolean var2);

    public static native void squareAddThumpable(boolean var0, boolean var1, boolean var2, boolean var3);

    public static native void squareAddWindow(boolean var0, boolean var1, boolean var2);

    public static native void squareEndUpdate();

    public static native int getVertLight(int var0, int var1, int var2, int var3, int var4);

    public static native float getLightInfo(int var0, int var1, int var2, int var3, int var4);

    public static native float getDarkMulti(int var0, int var1, int var2, int var3);

    public static native float getTargetDarkMulti(int var0, int var1, int var2, int var3);

    public static native boolean getSeen(int var0, int var1, int var2, int var3);

    public static native boolean getCanSee(int var0, int var1, int var2, int var3);

    public static native boolean getCouldSee(int var0, int var1, int var2, int var3);

    public static native boolean getSquareLighting(int var0, int var1, int var2, int var3, int[] var4);

    public static native boolean getSquareDirty(int var0, int var1, int var2, int var3);

    public static native void addLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8, boolean var9);

    public static native void addTempLight(int var0, int var1, int var2, int var3, int var4, float var5, float var6, float var7, int var8);

    public static native void removeLight(int var0);

    public static native void setLightActive(int var0, boolean var1);

    public static native void setLightColor(int var0, float var1, float var2, float var3);

    public static native void addRoomLight(int var0, long var1, long var3, int var5, int var6, int var7, int var8, int var9, boolean var10);

    public static native void removeRoomLight(int var0);

    public static native void setRoomLightActive(int var0, boolean var1);

    public static native void updateTorch(
        int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8, float var9, int var10
    );

    public static native void removeTorch(int var0);

    public static native int getVisibleRoomCount(int var0);

    public static native int getVisibleRooms(int var0, long[] var1);

    public static native void destroy();

    private static void updateVisibleRooms() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            if (buildingsChangedCounter[playerIndex] == -1) {
                LightingJNI.VisibleRoom.releaseAll(visibleRooms[playerIndex]);
                visibleRooms[playerIndex].clear();
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
                if (chunkMap != null && !chunkMap.ignore) {
                    int roomCount = getVisibleRoomCount(playerIndex);
                    if (roomCount != 0) {
                        if (visibleRoomIDs.length < roomCount) {
                            visibleRoomIDs = new long[roomCount];
                        }

                        getVisibleRooms(playerIndex, visibleRoomIDs);

                        for (int i = 0; i < roomCount; i++) {
                            long roomID = visibleRoomIDs[i];
                            int cellX = RoomID.getCellX(roomID);
                            int cellY = RoomID.getCellY(roomID);
                            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
                            if (metaCell != null) {
                                RoomDef roomDef = metaCell.rooms.get(roomID);
                                if (roomDef != null) {
                                    LightingJNI.VisibleRoom visibleRoom = LightingJNI.VisibleRoom.alloc();
                                    visibleRoom.cellX = cellX;
                                    visibleRoom.cellY = cellY;
                                    visibleRoom.metaId = roomDef.metaId;
                                    visibleRooms[playerIndex].add(visibleRoom);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static ArrayList<LightingJNI.VisibleRoom> getVisibleRooms(int playerIndex) {
        return visibleRooms[playerIndex];
    }

    public static boolean isRoomVisible(int playerIndex, int cellX, int cellY, long metaID) {
        ArrayList<LightingJNI.VisibleRoom> rooms = visibleRooms[playerIndex];

        for (int i = 0; i < rooms.size(); i++) {
            LightingJNI.VisibleRoom visibleRoom = rooms.get(i);
            if (visibleRoom.equals(cellX, cellY, metaID)) {
                return true;
            }
        }

        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null && player.getCurrentRoomDef() != null) {
            RoomDef roomDef = player.getCurrentRoomDef();
            if (cellX == roomDef.getBuilding().getCellX() && cellY == roomDef.getBuilding().getCellY() && metaID == roomDef.metaId) {
                return true;
            }
        }

        return false;
    }

    public static void buildingsChanged() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            buildingsChangedCounter[playerIndex] = updateCounter[playerIndex] + 2;
        }

        GameTime.instance.lightSourceUpdate = 100.0F;
        Arrays.fill(LosUtil.cachecleared, true);
        Core.dirtyGlobalLightsCount++;
    }

    private static void checkChangedBuildings() {
        boolean bUpdated = false;

        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            if (buildingsChangedCounter[playerIndex] != -1 && buildingsChangedCounter[playerIndex] <= updateCounter[playerIndex]) {
                buildingsChangedCounter[playerIndex] = -1;
                IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
                if (chunkMap != null && !chunkMap.ignore) {
                    bUpdated = true;

                    for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
                        for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                            IsoChunk chunk = chunkMap.getChunk(cx, cy);
                            if (chunk != null) {
                                chunk.getRenderLevels(playerIndex).invalidateAll(18496L);
                                chunk.getCutawayData().invalidateAll();
                                chunk.checkLightingLater_OnePlayer_AllLevels(playerIndex);
                            }
                        }
                    }
                }
            }
        }

        if (bUpdated) {
            IsoGridOcclusionData.SquareChanged();
            FBORenderCutaways.getInstance().squareChanged(null);
        }
    }

    static {
        for (int i = 0; i < 4; i++) {
            visibleRooms[i] = new ArrayList<>();
        }

        lightTransmissionW = new ColorInfo();
        lightTransmissionN = new ColorInfo();
        lightTransmissionE = new ColorInfo();
        lightTransmissionS = new ColorInfo();
    }

    public static final class JNILighting implements IsoGridSquare.ILighting {
        private static final int RESULT_LIGHTS_PER_SQUARE = 6;
        private static final int[] lightInts = new int[49];
        private static final byte VIS_SEEN = 1;
        private static final byte VIS_CAN_SEE = 2;
        private static final byte VIS_COULD_SEE = 4;
        private final int playerIndex;
        private final IsoGridSquare square;
        private final ColorInfo lightInfo = new ColorInfo();
        private byte vis;
        private float cacheDarkMulti;
        private float cacheTargetDarkMulti;
        private final int[] cacheVertLight = new int[8];
        private int updateTick = -1;
        private int lightsCount;
        private IsoGridSquare.ResultLight[] lights;
        private int lightLevel;
        static int notDirty;
        static int dirty;

        public JNILighting(int playerIndex, IsoGridSquare square) {
            this.playerIndex = playerIndex;
            this.square = square;
            this.cacheDarkMulti = 0.0F;
            this.cacheTargetDarkMulti = 0.0F;

            for (int i = 0; i < 8; i++) {
                this.cacheVertLight[i] = -16777216;
            }
        }

        @Override
        public int lightverts(int i) {
            return this.cacheVertLight[i];
        }

        @Override
        public float lampostTotalR() {
            return 0.0F;
        }

        @Override
        public float lampostTotalG() {
            return 0.0F;
        }

        @Override
        public float lampostTotalB() {
            return 0.0F;
        }

        @Override
        public boolean bSeen() {
            this.update();
            return (this.vis & 1) != 0;
        }

        @Override
        public boolean bCanSee() {
            this.update();
            return (this.vis & 2) != 0;
        }

        @Override
        public boolean bCouldSee() {
            this.update();
            return (this.vis & 4) != 0;
        }

        @Override
        public float darkMulti() {
            return this.cacheDarkMulti;
        }

        @Override
        public float targetDarkMulti() {
            return this.cacheTargetDarkMulti;
        }

        @Override
        public ColorInfo lightInfo() {
            this.update();
            return this.lightInfo;
        }

        @Override
        public void lightverts(int i, int value) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalR(float r) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalG(float g) {
            throw new IllegalStateException();
        }

        @Override
        public void lampostTotalB(float b) {
            throw new IllegalStateException();
        }

        @Override
        public void bSeen(boolean seen) {
            if (seen) {
                this.vis = (byte)(this.vis | 1);
            } else {
                this.vis &= -2;
            }
        }

        @Override
        public void bCanSee(boolean canSee) {
            throw new IllegalStateException();
        }

        @Override
        public void bCouldSee(boolean couldSee) {
            throw new IllegalStateException();
        }

        @Override
        public void darkMulti(float f) {
            throw new IllegalStateException();
        }

        @Override
        public void targetDarkMulti(float f) {
            throw new IllegalStateException();
        }

        @Override
        public int resultLightCount() {
            return this.lightsCount;
        }

        @Override
        public IsoGridSquare.ResultLight getResultLight(int index) {
            return this.lights[index];
        }

        @Override
        public void reset() {
            this.updateTick = -1;
            Arrays.fill(this.cacheVertLight, -16777216);
            this.vis = 0;
            this.cacheDarkMulti = 0.0F;
            this.cacheTargetDarkMulti = 0.0F;
            this.lightLevel = 0;
            this.lightInfo.set(0.0F, 0.0F, 0.0F, 1.0F);
        }

        private void update() {
            if (this.playerIndex != -1 && PerformanceSettings.fboRenderChunk) {
                this.updateFBORenderChunk();
            } else if (this.playerIndex == -1 || LightingJNI.updateCounter[this.playerIndex] != -1) {
                if (this.playerIndex == -1
                    || this.updateTick != LightingJNI.updateCounter[this.playerIndex]
                        && LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32)
                        && LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
                    IsoPlayer player = null;
                    if (this.playerIndex != -1) {
                        player = IsoPlayer.players[this.playerIndex];
                    }

                    boolean wasSeen = (this.vis & 1) != 0;
                    int kk = 0;
                    this.vis = (byte)(lightInts[kk++] & 7);
                    this.lightInfo.r = (lightInts[kk] & 0xFF) / 255.0F;
                    this.lightInfo.g = (lightInts[kk] >> 8 & 0xFF) / 255.0F;
                    this.lightInfo.b = (lightInts[kk++] >> 16 & 0xFF) / 255.0F;
                    this.lightInfo.a = 1.0F;
                    this.cacheDarkMulti = lightInts[kk++] / 100000.0F;
                    this.cacheTargetDarkMulti = lightInts[kk++] / 100000.0F;
                    this.lightLevel = lightInts[kk++];
                    this.square.lightLevel = this.lightLevel;
                    float colorModUpper = 1.0F;
                    float colorModLower = 1.0F;
                    if (player != null) {
                        int dZ = this.square.z - PZMath.fastfloor(player.getZ());
                        if (dZ == -1) {
                            colorModUpper = 1.0F;
                            colorModLower = 0.85F;
                        } else if (dZ < -1) {
                            colorModUpper = 0.85F;
                            colorModLower = 0.85F;
                        }

                        if ((this.vis & 2) == 0 && (this.vis & 4) != 0) {
                            int px = PZMath.fastfloor(player.getX());
                            int py = PZMath.fastfloor(player.getY());
                            int dx = this.square.x - px;
                            int dy = this.square.y - py;
                            if (player.dir != IsoDirections.Max && Math.abs(dx) <= 2 && Math.abs(dy) <= 2) {
                                int[] fv = LightingJNI.ForcedVis[player.dir.index()];

                                for (int i = 0; i < fv.length; i += 2) {
                                    if (dx == fv[i] && dy == fv[i + 1]) {
                                        this.vis = (byte)(this.vis | 2);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    for (int ix = 0; ix < 4; ix++) {
                        int col = lightInts[kk++];
                        float r = (col & 0xFF) * colorModLower;
                        float g = ((col & 0xFF00) >> 8) * colorModLower;
                        float b = ((col & 0xFF0000) >> 16) * colorModLower;
                        this.cacheVertLight[ix] = (int)r << 0 | (int)g << 8 | (int)b << 16 | 0xFF000000;
                    }

                    for (int ix = 4; ix < 8; ix++) {
                        int col = lightInts[kk++];
                        float r = (col & 0xFF) * colorModUpper;
                        float g = ((col & 0xFF00) >> 8) * colorModUpper;
                        float b = ((col & 0xFF0000) >> 16) * colorModUpper;
                        this.cacheVertLight[ix] = (int)r << 0 | (int)g << 8 | (int)b << 16 | 0xFF000000;
                    }

                    this.lightsCount = lightInts[kk++];

                    for (int ix = 0; ix < this.lightsCount; ix++) {
                        if (this.lights == null) {
                            this.lights = new IsoGridSquare.ResultLight[6];
                        }

                        if (this.lights[ix] == null) {
                            this.lights[ix] = new IsoGridSquare.ResultLight();
                        }

                        this.lights[ix].id = lightInts[kk++];
                        this.lights[ix].x = lightInts[kk++];
                        this.lights[ix].y = lightInts[kk++];
                        this.lights[ix].z = lightInts[kk++] - 32;
                        this.lights[ix].radius = lightInts[kk++];
                        int rgb = lightInts[kk++];
                        this.lights[ix].r = (rgb & 0xFF) / 255.0F;
                        this.lights[ix].g = (rgb >> 8 & 0xFF) / 255.0F;
                        this.lights[ix].b = (rgb >> 16 & 0xFF) / 255.0F;
                        this.lights[ix].flags = rgb >> 24 & 0xFF;
                    }

                    if (this.playerIndex == -1) {
                        return;
                    }

                    this.updateTick = LightingJNI.updateCounter[this.playerIndex];
                    if ((this.vis & 1) != 0) {
                        if (wasSeen && this.square.getRoom() != null && this.square.getRoom().def != null && !this.square.getRoom().def.explored) {
                            boolean var27 = true;
                        }

                        this.square.checkRoomSeen(this.playerIndex);
                        if (!wasSeen) {
                            assert !GameServer.server;

                            if (!GameClient.client) {
                                Meta.instance.dealWithSquareSeen(this.square);
                            }
                        }
                    } else if (this.square.getRoom() != null
                        && this.square.getRoom().def != null
                        && !this.square.getRoom().def.explored
                        && IsoUtils.DistanceToSquared(player.getX(), player.getY(), this.square.x + 0.5F, this.square.y + 0.5F) < 3.0F) {
                        this.square.checkRoomSeen(this.playerIndex);
                    }
                }
            }
        }

        private void updateFBORenderChunk() {
            if (this.square.chunk != null) {
                if (LightingJNI.updateCounter[this.playerIndex] != -1) {
                    if (this.updateTick != LightingJNI.updateCounter[this.playerIndex]) {
                        if (!LightingJNI.getSquareDirty(this.playerIndex, this.square.x, this.square.y, this.square.z + 32)) {
                            notDirty++;
                        } else {
                            dirty++;
                            if (LightingJNI.getSquareLighting(this.playerIndex, this.square.x, this.square.y, this.square.z + 32, lightInts)) {
                                IsoPlayer player = IsoPlayer.players[this.playerIndex];
                                boolean wasCanSee = (this.vis & 2) != 0;
                                boolean wasCouldSee = (this.vis & 4) != 0;
                                boolean wasSeen = (this.vis & 1) != 0;
                                int kk = 0;
                                this.vis = (byte)(lightInts[kk++] & 7);
                                int wasLightInfoR = (int)(this.lightInfo.r * 255.0F);
                                int wasLightInfoG = (int)(this.lightInfo.g * 255.0F);
                                int wasLightInfoB = (int)(this.lightInfo.b * 255.0F);
                                int wasDarkMulti = (int)(this.cacheDarkMulti * 1000.0F);
                                int wasDarkMultiTarget = (int)(this.cacheTargetDarkMulti * 1000.0F);
                                int wasLightLevel = this.lightLevel;
                                this.lightInfo.r = (lightInts[kk] & 0xFF) / 255.0F;
                                this.lightInfo.g = (lightInts[kk] >> 8 & 0xFF) / 255.0F;
                                this.lightInfo.b = (lightInts[kk++] >> 16 & 0xFF) / 255.0F;
                                this.lightInfo.a = 1.0F;
                                this.cacheDarkMulti = lightInts[kk++] / 100000.0F;
                                this.cacheTargetDarkMulti = lightInts[kk++] / 100000.0F;
                                this.lightLevel = lightInts[kk++];
                                this.square.lightLevel = this.lightLevel;
                                if (player != null && (this.vis & 2) == 0 && (this.vis & 4) != 0) {
                                    int px = PZMath.fastfloor(player.getX());
                                    int py = PZMath.fastfloor(player.getY());
                                    int dx = this.square.x - px;
                                    int dy = this.square.y - py;
                                    if (player.dir != IsoDirections.Max && Math.abs(dx) <= 2 && Math.abs(dy) <= 2) {
                                        int[] fv = LightingJNI.ForcedVis[player.dir.index()];

                                        for (int i = 0; i < fv.length; i += 2) {
                                            if (dx == fv[i] && dy == fv[i + 1]) {
                                                this.vis = (byte)(this.vis | 2);
                                                break;
                                            }
                                        }
                                    }
                                }

                                int wasVertLight1 = this.cacheVertLight[0];
                                int wasVertLight2 = this.cacheVertLight[1];
                                int wasVertLight3 = this.cacheVertLight[2];
                                int wasVertLight4 = this.cacheVertLight[3];
                                int wasVertLight5 = this.cacheVertLight[4];
                                int wasVertLight6 = this.cacheVertLight[5];
                                int wasVertLight7 = this.cacheVertLight[6];
                                int wasVertLight8 = this.cacheVertLight[7];

                                for (int ix = 0; ix < 8; ix++) {
                                    this.cacheVertLight[ix] = lightInts[kk++];
                                }

                                int isLightInfoR = (int)(this.lightInfo.r * 255.0F);
                                int isLightInfoG = (int)(this.lightInfo.g * 255.0F);
                                int isLightInfoB = (int)(this.lightInfo.b * 255.0F);
                                int isDarkMulti = (int)(this.cacheDarkMulti * 1000.0F);
                                int isDarkMultiTarget = (int)(this.cacheTargetDarkMulti * 1000.0F);
                                int isLightLevel = this.lightLevel;
                                int isVertLight1 = this.cacheVertLight[0];
                                int isVertLight2 = this.cacheVertLight[1];
                                int isVertLight3 = this.cacheVertLight[2];
                                int isVertLight4 = this.cacheVertLight[3];
                                int isVertLight5 = this.cacheVertLight[4];
                                int isVertLight6 = this.cacheVertLight[5];
                                int isVertLight7 = this.cacheVertLight[6];
                                int isVertLight8 = this.cacheVertLight[7];
                                FBORenderLevels renderLevels = this.square.chunk.getRenderLevels(this.playerIndex);
                                if (isDarkMulti == wasDarkMulti
                                    && isDarkMultiTarget == wasDarkMultiTarget
                                    && isLightLevel == wasLightLevel
                                    && isLightInfoR == wasLightInfoR
                                    && isLightInfoG == wasLightInfoG
                                    && isLightInfoB == wasLightInfoB) {
                                    if ((
                                            isVertLight1 != wasVertLight1
                                                || isVertLight2 != wasVertLight2
                                                || isVertLight3 != wasVertLight3
                                                || isVertLight4 != wasVertLight4
                                                || isVertLight5 != wasVertLight5
                                                || isVertLight6 != wasVertLight6
                                                || isVertLight7 != wasVertLight7
                                                || isVertLight8 != wasVertLight8
                                        )
                                        && !DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                                        renderLevels.invalidateLevel(this.square.z, 32L);
                                    }
                                } else if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                                    renderLevels.invalidateLevel(this.square.z, 32L);
                                }

                                if (wasCouldSee != ((this.vis & 4) != 0)) {
                                    FBORenderCutaways.getInstance().squareChanged(this.square);
                                }

                                this.lightsCount = lightInts[kk++];

                                for (int ix = 0; ix < this.lightsCount; ix++) {
                                    if (this.lights == null) {
                                        this.lights = new IsoGridSquare.ResultLight[6];
                                    }

                                    if (this.lights[ix] == null) {
                                        this.lights[ix] = new IsoGridSquare.ResultLight();
                                    }

                                    this.lights[ix].id = lightInts[kk++];
                                    this.lights[ix].x = lightInts[kk++];
                                    this.lights[ix].y = lightInts[kk++];
                                    this.lights[ix].z = lightInts[kk++] - 32;
                                    this.lights[ix].radius = lightInts[kk++];
                                    int rgb = lightInts[kk++];
                                    this.lights[ix].r = (rgb & 0xFF) / 255.0F;
                                    this.lights[ix].g = (rgb >> 8 & 0xFF) / 255.0F;
                                    this.lights[ix].b = (rgb >> 16 & 0xFF) / 255.0F;
                                    this.lights[ix].flags = rgb >> 24 & 0xFF;
                                }

                                if (this.updateTick == -1 && renderLevels.isOnScreen(this.square.z)) {
                                    renderLevels.invalidateLevel(this.square.z, 32L);
                                }

                                this.updateTick = LightingJNI.updateCounter[this.playerIndex];
                                if ((this.vis & 1) != 0) {
                                    this.square.checkRoomSeen(this.playerIndex);
                                    if (!wasSeen) {
                                        assert !GameServer.server;

                                        if (!GameClient.client) {
                                            Meta.instance.dealWithSquareSeen(this.square);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static final class VisibleRoom {
        public int cellX;
        public int cellY;
        public long metaId;
        private static final ObjectPool<LightingJNI.VisibleRoom> pool = new ObjectPool<>(LightingJNI.VisibleRoom::new);

        LightingJNI.VisibleRoom set(int cellX, int cellY, long metaID) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.metaId = metaID;
            return this;
        }

        public LightingJNI.VisibleRoom set(LightingJNI.VisibleRoom other) {
            return this.set(other.cellX, other.cellY, other.metaId);
        }

        @Override
        public boolean equals(Object rhs) {
            return rhs instanceof LightingJNI.VisibleRoom other ? this.equals(other.cellX, other.cellY, other.metaId) : false;
        }

        boolean equals(int cellX, int cellY, long metaID) {
            return this.cellX == cellX && this.cellY == cellY && this.metaId == metaID;
        }

        public static LightingJNI.VisibleRoom alloc() {
            return pool.alloc();
        }

        public void release() {
            pool.release(this);
        }

        public static void releaseAll(List<LightingJNI.VisibleRoom> objs) {
            pool.releaseAll(objs);
        }
    }
}
