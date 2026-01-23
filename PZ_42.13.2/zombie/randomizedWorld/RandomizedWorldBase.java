// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.debug.DebugLog;
import zombie.entity.GameEntityFactory;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;

@UsedFromLua
public class RandomizedWorldBase {
    private static final Vector2 s_tempVector2 = new Vector2();
    protected int minimumDays;
    protected int maximumDays;
    protected int minimumRooms;
    protected boolean unique;
    private boolean rvsVehicleKeyAddedToZombie;
    protected boolean isRat;
    protected String name;
    protected String debugLine = "";
    protected boolean reallyAlwaysForce;
    private static final ArrayList<String> barnClutter = new ArrayList<>();
    private static final ArrayList<String> bathroomSinkClutter = new ArrayList<>();
    private static final ArrayList<String> bedClutter = new ArrayList<>();
    private static final ArrayList<String> beachPartyClutter = new ArrayList<>();
    private static final ArrayList<String> bbqClutter = new ArrayList<>();
    private static final ArrayList<String> cafeClutter = new ArrayList<>();
    private static final ArrayList<String> carpentryToolClutter = new ArrayList<>();
    private static final ArrayList<String> deadEndClutter = new ArrayList<>();
    private static final ArrayList<String> dormClutter = new ArrayList<>();
    private static final ArrayList<String> farmStorageClutter = new ArrayList<>();
    private static final ArrayList<String> footballNightDrinks = new ArrayList<>();
    private static final ArrayList<String> footballNightSnacks = new ArrayList<>();
    private static final ArrayList<String> garageStorageClutter = new ArrayList<>();
    private static final ArrayList<String> gigamartClutter = new ArrayList<>();
    private static final ArrayList<String> groceryClutter = new ArrayList<>();
    private static final ArrayList<String> hairSalonClutter = new ArrayList<>();
    private static final ArrayList<String> hallClutter = new ArrayList<>();
    private static final ArrayList<String> henDoDrinks = new ArrayList<>();
    private static final ArrayList<String> henDoSnacks = new ArrayList<>();
    private static final ArrayList<String> hoedownClutter = new ArrayList<>();
    private static final ArrayList<String> housePartyClutter = new ArrayList<>();
    private static final ArrayList<String> judgeClutter = new ArrayList<>();
    private static final ArrayList<String> kidClutter = new ArrayList<>();
    private static final ArrayList<String> kitchenSinkClutter = new ArrayList<>();
    private static final ArrayList<String> kitchenCounterClutter = new ArrayList<>();
    private static final ArrayList<String> kitchenStoveClutter = new ArrayList<>();
    private static final ArrayList<String> laundryRoomClutter = new ArrayList<>();
    private static final ArrayList<String> livingRoomClutter = new ArrayList<>();
    private static final ArrayList<String> medicalClutter = new ArrayList<>();
    private static final ArrayList<String> murderSceneClutter = new ArrayList<>();
    private static final ArrayList<String> nastyMattressClutter = new ArrayList<>();
    private static final ArrayList<String> oldShelterClutter = new ArrayList<>();
    private static final ArrayList<String> officeCarDealerClutter = new ArrayList<>();
    private static final ArrayList<String> officePaperworkClutter = new ArrayList<>();
    private static final ArrayList<String> officePenClutter = new ArrayList<>();
    private static final ArrayList<String> officeOtherClutter = new ArrayList<>();
    private static final ArrayList<String> officeTreatClutter = new ArrayList<>();
    private static final ArrayList<String> ovenFoodClutter = new ArrayList<>();
    private static final ArrayList<String> pillowClutter = new ArrayList<>();
    private static final ArrayList<String> pokerNightClutter = new ArrayList<>();
    private static final ArrayList<String> richJerkClutter = new ArrayList<>();
    private static final ArrayList<String> sadCampsiteClutter = new ArrayList<>();
    private static final ArrayList<String> sidetableClutter = new ArrayList<>();
    private static final ArrayList<String> survivalistCampsiteClutter = new ArrayList<>();
    private static final ArrayList<String> twiggyClutter = new ArrayList<>();
    private static final ArrayList<String> utilityToolClutter = new ArrayList<>();
    private static final ArrayList<String> vanCampClutter = new ArrayList<>();
    private static final ArrayList<String> watchClutter = new ArrayList<>();
    private static final ArrayList<String> woodcraftClutter = new ArrayList<>();

    public BaseVehicle addVehicle(Zone zone, IsoGridSquare sq, IsoChunk chunk, String zoneName, String scriptName, IsoDirections dir) {
        return this.addVehicle(zone, sq, chunk, zoneName, scriptName, null, dir, null);
    }

    public BaseVehicle addVehicleFlipped(
        Zone zone, IsoGridSquare sq, IsoChunk chunk, String zoneName, String scriptName, Integer skinIndex, IsoDirections dir, String specificContainer
    ) {
        if (sq == null) {
            return null;
        } else {
            if (dir == null) {
                dir = IsoDirections.getRandom();
            }

            Vector2 angle = dir.ToVector();
            return this.addVehicleFlipped(zone, sq.x, sq.y, sq.z, angle.getDirection(), zoneName, scriptName, skinIndex, specificContainer);
        }
    }

    public BaseVehicle addVehicleFlipped(
        Zone zone,
        float vehicleX,
        float vehicleY,
        float vehicleZ,
        float direction,
        String zoneName,
        String scriptName,
        Integer skinIndex,
        String specificContainer
    ) {
        if (StringUtils.isNullOrEmpty(zoneName)) {
            zoneName = "junkyard";
        }

        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)vehicleX, (double)vehicleY, (double)vehicleZ);
        if (sq == null) {
            return null;
        } else {
            IsoChunk chunk = sq.getChunk();
            IsoDirections dir = IsoDirections.fromAngle(direction);
            BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
            v.specificDistributionId = specificContainer;
            VehicleType vehicleType = VehicleType.getRandomVehicleType(zoneName, false);
            if (!StringUtils.isNullOrEmpty(scriptName)) {
                v.setScriptName(scriptName);
                v.setScript();
                if (skinIndex != null) {
                    v.setSkinIndex(skinIndex);
                }
            } else {
                if (vehicleType == null) {
                    return null;
                }

                v.setVehicleType(vehicleType.name);
                if (!chunk.RandomizeModel(v, zone, zoneName, vehicleType)) {
                    return null;
                }
            }

            if (vehicleType.isSpecialCar) {
                v.setDoColor(false);
            }

            v.setDir(dir);
            float angle = direction - (float) (Math.PI / 2);

            while (angle > Math.PI * 2) {
                angle = (float)(angle - (Math.PI * 2));
            }

            v.savedRot.rotationXYZ(0.0F, -angle, (float) Math.PI);
            v.jniTransform.setRotation(v.savedRot);
            float physicsZ = PZMath.max(vehicleZ * 3.0F * 0.8164967F, PZMath.fastfloor(vehicleZ) + v.getScript().getExtents().y() + 0.1F);
            v.jniTransform.origin.y = physicsZ;
            vehicleZ = physicsZ / 2.44949F;
            v.setX(vehicleX);
            v.setY(vehicleY);
            v.setZ(vehicleZ);
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                v.setSquare(sq);
                sq.chunk.vehicles.add(v);
                v.chunk = sq.chunk;
                v.savedPhysicsZ = physicsZ;
                v.addToWorld(true);
                VehiclesDB2.instance.addVehicle(v);
            }

            v.savedPhysicsZ = physicsZ;
            v.setGeneralPartCondition(0.2F, 70.0F);
            v.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
            return v;
        }
    }

    public BaseVehicle addVehicle(
        Zone zone, IsoGridSquare sq, IsoChunk chunk, String zoneName, String scriptName, Integer skinIndex, IsoDirections dir, String specificContainer
    ) {
        if (sq == null) {
            return null;
        } else {
            if (dir == null) {
                dir = IsoDirections.getRandom();
            }

            Vector2 angle = dir.ToVector();
            angle.rotate(Rand.Next(-0.5F, 0.5F));
            return this.addVehicle(zone, sq.x, sq.y, sq.z, angle.getDirection(), zoneName, scriptName, skinIndex, specificContainer, false);
        }
    }

    public BaseVehicle addVehicle(
        Zone zone,
        IsoGridSquare sq,
        IsoChunk chunk,
        String zoneName,
        String scriptName,
        Integer skinIndex,
        IsoDirections dir,
        String specificContainer,
        boolean crashed
    ) {
        if (sq == null) {
            return null;
        } else {
            if (dir == null) {
                dir = IsoDirections.getRandom();
            }

            Vector2 angle = dir.ToVector();
            angle.rotate(Rand.Next(-0.5F, 0.5F));
            return this.addVehicle(zone, sq.x, sq.y, sq.z, angle.getDirection(), zoneName, scriptName, skinIndex, specificContainer, crashed);
        }
    }

    public BaseVehicle addVehicle(
        IsoGridSquare sq, IsoChunk chunk, String zoneName, String scriptName, Integer skinIndex, IsoDirections dir, String specificContainer
    ) {
        if (sq == null) {
            return null;
        } else {
            if (dir == null) {
                dir = IsoDirections.getRandom();
            }

            Vector2 angle = dir.ToVector();
            angle.rotate(Rand.Next(-0.5F, 0.5F));
            return this.addVehicle(sq.x, sq.y, sq.z, angle.getDirection(), zoneName, scriptName, skinIndex, specificContainer);
        }
    }

    public BaseVehicle addVehicle(
        Zone zone,
        float vehicleX,
        float vehicleY,
        float vehicleZ,
        float direction,
        String zoneName,
        String scriptName,
        Integer skinIndex,
        String specificContainer
    ) {
        return this.addVehicle(zone, vehicleX, vehicleY, vehicleZ, direction, zoneName, scriptName, skinIndex, specificContainer, false);
    }

    public BaseVehicle addVehicle(
        Zone zone,
        float vehicleX,
        float vehicleY,
        float vehicleZ,
        float direction,
        String zoneName,
        String scriptName,
        Integer skinIndex,
        String specificContainer,
        boolean crashed
    ) {
        if (StringUtils.isNullOrEmpty(zoneName)) {
            zoneName = "junkyard";
        }

        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)vehicleX, (double)vehicleY, (double)vehicleZ);
        if (sq == null) {
            return null;
        } else {
            IsoChunk chunk = sq.getChunk();
            IsoDirections dir = IsoDirections.fromAngle(direction);
            BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
            v.specificDistributionId = specificContainer;
            VehicleType vehicleType = VehicleType.getRandomVehicleType(zoneName, false);
            if (!StringUtils.isNullOrEmpty(scriptName)) {
                v.setScriptName(scriptName);
                v.setScript();
                if (skinIndex != null) {
                    v.setSkinIndex(skinIndex);
                }
            } else {
                if (vehicleType == null) {
                    return null;
                }

                v.setVehicleType(vehicleType.name);
                if (!chunk.RandomizeModel(v, zone, zoneName, vehicleType)) {
                    return null;
                }
            }

            if (vehicleType.isSpecialCar) {
                v.setDoColor(false);
            }

            v.setDir(dir);
            float angle = direction - (float) (Math.PI / 2);

            while (angle > Math.PI * 2) {
                angle = (float)(angle - (Math.PI * 2));
            }

            v.savedRot.setAngleAxis(-angle, 0.0F, 1.0F, 0.0F);
            v.jniTransform.setRotation(v.savedRot);
            v.setX(vehicleX);
            v.setY(vehicleY);
            v.setZ(vehicleZ);
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                v.setSquare(sq);
                sq.chunk.vehicles.add(v);
                v.chunk = sq.chunk;
                v.addToWorld(crashed);
                VehiclesDB2.instance.addVehicle(v);
            }

            v.setGeneralPartCondition(0.2F, 70.0F);
            v.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
            v.setPreviouslyMoved(true);
            v.setAlarmed(false);
            return v;
        }
    }

    public BaseVehicle addVehicle(
        float vehicleX, float vehicleY, float vehicleZ, float direction, String zoneName, String scriptName, Integer skinIndex, String specificContainer
    ) {
        return this.addVehicle(vehicleX, vehicleY, vehicleZ, direction, zoneName, scriptName, skinIndex, specificContainer, false);
    }

    public BaseVehicle addVehicle(
        float vehicleX,
        float vehicleY,
        float vehicleZ,
        float direction,
        String zoneName,
        String scriptName,
        Integer skinIndex,
        String specificContainer,
        boolean crashed
    ) {
        if (StringUtils.isNullOrEmpty(zoneName)) {
            zoneName = "junkyard";
        }

        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)vehicleX, (double)vehicleY, (double)vehicleZ);
        if (sq == null) {
            return null;
        } else {
            IsoChunk chunk = sq.getChunk();
            IsoDirections dir = IsoDirections.fromAngle(direction);
            BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
            v.specificDistributionId = specificContainer;
            VehicleType vehicleType = VehicleType.getRandomVehicleType(zoneName, false);
            if (!StringUtils.isNullOrEmpty(scriptName)) {
                v.setScriptName(scriptName);
                v.setScript();
                if (skinIndex != null) {
                    v.setSkinIndex(skinIndex);
                }
            } else {
                if (vehicleType == null) {
                    return null;
                }

                v.setVehicleType(vehicleType.name);
            }

            if (vehicleType.isSpecialCar) {
                v.setDoColor(false);
            }

            v.setDir(dir);
            float angle = direction - (float) (Math.PI / 2);

            while (angle > Math.PI * 2) {
                angle = (float)(angle - (Math.PI * 2));
            }

            v.savedRot.setAngleAxis(-angle, 0.0F, 1.0F, 0.0F);
            v.jniTransform.setRotation(v.savedRot);
            v.setX(vehicleX);
            v.setY(vehicleY);
            v.setZ(vehicleZ);
            if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                v.setSquare(sq);
                sq.chunk.vehicles.add(v);
                v.chunk = sq.chunk;
                v.addToWorld(crashed);
                VehiclesDB2.instance.addVehicle(v);
            }

            v.setGeneralPartCondition(0.2F, 70.0F);
            v.rust = Rand.Next(100) < 70 ? 1.0F : 0.0F;
            v.setPreviouslyMoved(true);
            v.setAlarmed(false);
            return v;
        }
    }

    public static void removeAllVehiclesOnZone(Zone zone) {
        for (int x = zone.x; x < zone.x + zone.w; x++) {
            for (int y = zone.y; y < zone.y + zone.h; y++) {
                IsoGridSquare square = IsoCell.getInstance().getGridSquare(x, y, 0);
                if (square != null) {
                    BaseVehicle vehicle = square.getVehicleContainer();
                    if (vehicle != null) {
                        vehicle.permanentlyRemove();
                    }
                }
            }
        }
    }

    /**
     * Add zombies near the vehicles, around a 4x4 square around it, avoiding being
     *  ON the vehicle & randomizing square for each zombies
     */
    public ArrayList<IsoZombie> addZombiesOnVehicle(int totalZombies, String outfit, Integer femaleChance, BaseVehicle vehicle) {
        ArrayList<IsoZombie> result = new ArrayList<>();
        if (vehicle == null) {
            return result;
        } else {
            int maxTry = 100;
            IsoGridSquare sq = vehicle.getSquare();
            if (sq != null && sq.getCell() != null) {
                for (; totalZombies > 0; maxTry = 100) {
                    while (maxTry > 0) {
                        IsoGridSquare sq2 = sq.getCell().getGridSquare(Rand.Next(sq.x - 4, sq.x + 4), Rand.Next(sq.y - 4, sq.y + 4), sq.z);
                        if (sq2 != null && sq2.getVehicleContainer() == null) {
                            totalZombies--;
                            result.addAll(this.addZombiesOnSquare(1, outfit, femaleChance, sq2));
                            break;
                        }

                        maxTry--;
                    }
                }

                if (!this.rvsVehicleKeyAddedToZombie && !result.isEmpty() && !vehicle.getScript().neverSpawnKey()) {
                    IsoZombie zed = result.get(Rand.Next(0, result.size()));
                    zed.addItemToSpawnAtDeath(vehicle.createVehicleKey());
                    this.rvsVehicleKeyAddedToZombie = true;
                }

                return result;
            } else {
                return result;
            }
        }
    }

    public static IsoDeadBody createRandomDeadBody(RoomDef room, int blood) {
        if (IsoWorld.getZombiesDisabled()) {
            return null;
        } else if (room == null) {
            return null;
        } else {
            IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
            return freeSQ == null ? null : createRandomDeadBody(freeSQ, null, blood, 0, null);
        }
    }

    public ArrayList<IsoZombie> addZombiesOnSquare(int totalZombies, String outfit, Integer femaleChance, IsoGridSquare square) {
        ArrayList<IsoZombie> result = new ArrayList<>();
        if (IsoWorld.getZombiesDisabled()) {
            return result;
        } else if (square == null) {
            return result;
        } else if (square.isWaterSquare()) {
            return result;
        } else {
            for (int j = 0; j < totalZombies; j++) {
                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(square);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom().index(), false);
                if (zombie != null) {
                    if (femaleChance != null) {
                        zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
                    }

                    if (outfit != null) {
                        zombie.dressInPersistentOutfit(outfit);
                        zombie.dressInRandomOutfit = false;
                    } else {
                        zombie.dressInRandomOutfit();
                        zombie.dressInRandomOutfit = false;
                    }

                    result.add(zombie);
                }
            }

            ZombieSpawnRecorder.instance.record(result, this.getClass().getSimpleName());
            return result;
        }
    }

    public static IsoDeadBody createRandomDeadBody(int x, int y, int z, IsoDirections dir, int blood) {
        return createRandomDeadBody(x, y, z, dir, blood, 0);
    }

    public static IsoDeadBody createRandomDeadBody(int x, int y, int z, IsoDirections dir, int blood, int crawlerChance) {
        IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
        return createRandomDeadBody(sq, dir, blood, crawlerChance, null);
    }

    public static IsoDeadBody createRandomDeadBody(IsoGridSquare sq, IsoDirections dir, int blood, int crawlerChance, String outfit) {
        if (sq == null) {
            return null;
        } else {
            boolean bRandomDirection = dir == null;
            if (bRandomDirection) {
                dir = IsoDirections.getRandom();
            }

            return createRandomDeadBody(
                sq.x + Rand.Next(0.05F, 0.95F),
                sq.y + Rand.Next(0.05F, 0.95F),
                sq.z,
                dir.ToVector().getDirection(),
                bRandomDirection,
                blood,
                crawlerChance,
                outfit
            );
        }
    }

    public static IsoDeadBody createRandomDeadBody(
        float x, float y, float z, float direction, boolean alignToSquare, int blood, int crawlerChance, String outfit
    ) {
        if (IsoWorld.getZombiesDisabled()) {
            return null;
        } else {
            IsoGridSquare sq = IsoCell.getInstance().getGridSquare((double)x, (double)y, (double)z);
            if (sq == null) {
                return null;
            } else {
                IsoDirections dir = IsoDirections.fromAngle(direction);
                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(sq);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(dir.index(), false);
                if (zombie == null) {
                    return null;
                } else {
                    if (outfit != null) {
                        zombie.dressInPersistentOutfit(outfit);
                        zombie.dressInRandomOutfit = false;
                    } else {
                        zombie.dressInRandomOutfit();
                    }

                    if (Rand.Next(100) < crawlerChance) {
                        zombie.setFakeDead(true);
                        zombie.setCrawler(true);
                        zombie.setCanWalk(false);
                        zombie.setCrawlerType(1);
                    } else {
                        zombie.setFakeDead(false);
                        zombie.setHealth(0.0F);
                    }

                    zombie.getHumanVisual().zombieRotStage = ((HumanVisual)zombie.getVisual()).pickRandomZombieRotStage();

                    for (int i = 0; i < blood; i++) {
                        zombie.addBlood(null, false, true, true);
                    }

                    zombie.DoCorpseInventory();
                    zombie.setX(x);
                    zombie.setY(y);
                    zombie.getForwardDirection().setLengthAndDirection(direction, 1.0F);
                    if (alignToSquare || zombie.isSkeleton()) {
                        alignCorpseToSquare(zombie, sq);
                    }

                    IsoDeadBody body = new IsoDeadBody(zombie, true);
                    if (!body.isFakeDead() && !body.isSkeleton() && Rand.Next(20) == 0) {
                        body.setFakeDead(true);
                        if (Rand.Next(5) == 0) {
                            body.setCrawling(true);
                        }
                    }

                    return body;
                }
            }
        }
    }

    public static IsoDeadBody createRandomDeadBody(
        IsoGridSquare sq, IsoDirections dir2, boolean alignToSquare, int blood, int crawlerChance, String outfit, Integer femaleChance
    ) {
        float x = sq.x + Rand.Next(0.05F, 0.95F);
        float y = sq.y + Rand.Next(0.05F, 0.95F);
        if (IsoWorld.getZombiesDisabled()) {
            return null;
        } else if (sq == null) {
            return null;
        } else {
            boolean bRandomDirection = dir2 == null;
            if (bRandomDirection) {
                dir2 = IsoDirections.getRandom();
            }

            float direction = dir2.ToVector().getDirection();
            IsoDirections dir = IsoDirections.fromAngle(direction);
            VirtualZombieManager.instance.choices.clear();
            VirtualZombieManager.instance.choices.add(sq);
            IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(dir.index(), false);
            if (zombie == null) {
                return null;
            } else {
                if (femaleChance != null) {
                    zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
                }

                if (outfit != null) {
                    zombie.dressInPersistentOutfit(outfit);
                    zombie.dressInRandomOutfit = false;
                } else {
                    zombie.dressInRandomOutfit();
                }

                if (Rand.Next(100) < crawlerChance) {
                    zombie.setFakeDead(true);
                    zombie.setCrawler(true);
                    zombie.setCanWalk(false);
                    zombie.setCrawlerType(1);
                } else {
                    zombie.setFakeDead(false);
                    zombie.setHealth(0.0F);
                }

                zombie.getHumanVisual().zombieRotStage = ((HumanVisual)zombie.getVisual()).pickRandomZombieRotStage();

                for (int i = 0; i < blood; i++) {
                    zombie.addBlood(null, false, true, true);
                }

                zombie.DoCorpseInventory();
                zombie.setX(x);
                zombie.setY(y);
                zombie.getForwardDirection().setLengthAndDirection(direction, 1.0F);
                if (alignToSquare || zombie.isSkeleton()) {
                    alignCorpseToSquare(zombie, sq);
                }

                IsoDeadBody body = new IsoDeadBody(zombie, true);
                if (!body.isFakeDead() && !body.isSkeleton() && Rand.Next(20) == 0) {
                    body.setFakeDead(true);
                    if (Rand.Next(5) == 0) {
                        body.setCrawling(true);
                    }
                }

                return body;
            }
        }
    }

    public void addTraitOfBlood(IsoDirections dir, int time, int x, int y, int z) {
        for (int i = 0; i < time; i++) {
            float velX = 0.0F;
            float velY = 0.0F;
            if (dir == IsoDirections.S) {
                velY = Rand.Next(-2.0F, 0.5F);
            }

            if (dir == IsoDirections.N) {
                velY = Rand.Next(-0.5F, 2.0F);
            }

            if (dir == IsoDirections.E) {
                velX = Rand.Next(-2.0F, 0.5F);
            }

            if (dir == IsoDirections.W) {
                velX = Rand.Next(-0.5F, 2.0F);
            }

            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, IsoCell.getInstance(), x, y, z + 0.2F, velX, velY);
        }
    }

    public void addTrailOfBlood(float x, float y, float z, float direction, int count) {
        Vector2 v = s_tempVector2;

        for (int i = 0; i < count; i++) {
            float velocity = Rand.Next(-0.5F, 2.0F);
            if (velocity < 0.0F) {
                v.setLengthAndDirection(direction + (float) Math.PI, -velocity);
            } else {
                v.setLengthAndDirection(direction, velocity);
            }

            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, IsoCell.getInstance(), x, y, z + 0.2F, v.x, v.y);
        }
    }

    public void addBloodSplat(IsoGridSquare sq, int nbr) {
        if (sq != null) {
            for (int i = 0; i < nbr; i++) {
                sq.getChunk().addBloodSplat(sq.x + Rand.Next(-0.5F, 0.5F), sq.y + Rand.Next(-0.5F, 0.5F), sq.z, Rand.Next(8));
            }
        }
    }

    public void setAttachedItem(IsoZombie zombie, String location, String item, String ensureItem) {
        InventoryItem weapon = InventoryItemFactory.CreateItem(item);
        if (weapon != null) {
            weapon.setCondition(Rand.Next(Math.max(2, weapon.getConditionMax() - 5), weapon.getConditionMax()), false);
            if (weapon instanceof HandWeapon handWeapon) {
                handWeapon.randomizeBullets();
            }

            zombie.setAttachedItem(location, weapon);
            if (!StringUtils.isNullOrEmpty(ensureItem)) {
                zombie.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem(ensureItem));
            }
        }
    }

    public static IsoGameCharacter createRandomZombie(RoomDef room) {
        IsoGridSquare freeSQ = getRandomSpawnSquare(room);
        return createRandomZombie(freeSQ.getX(), freeSQ.getY(), freeSQ.getZ());
    }

    public static IsoGameCharacter createRandomZombieForCorpse(RoomDef room) {
        IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
        if (freeSQ == null) {
            return null;
        } else {
            IsoGameCharacter zombie = createRandomZombie(freeSQ.getX(), freeSQ.getY(), freeSQ.getZ());
            if (zombie != null) {
                alignCorpseToSquare(zombie, freeSQ);
            }

            return zombie;
        }
    }

    public static IsoDeadBody createBodyFromZombie(IsoGameCharacter chr) {
        if (IsoWorld.getZombiesDisabled()) {
            return null;
        } else {
            for (int b = 0; b < 6; b++) {
                chr.splatBlood(Rand.Next(1, 4), 0.3F);
            }

            return new IsoDeadBody(chr, true);
        }
    }

    public static IsoGameCharacter createRandomZombie(int x, int y, int z) {
        IsoGameCharacter chr = new RandomizedBuildingBase.HumanCorpse(IsoWorld.instance.getCell(), x, y, z);
        chr.setDescriptor(SurvivorFactory.CreateSurvivor());
        chr.setFemale(chr.getDescriptor().isFemale());
        chr.setDir(IsoDirections.fromIndex(Rand.Next(8)));
        chr.initWornItems("Human");
        chr.initAttachedItems("Human");
        Outfit outfit = chr.getRandomDefaultOutfit();
        chr.dressInNamedOutfit(outfit.name);
        chr.initSpritePartsEmpty();
        chr.Dressup(chr.getDescriptor());
        return chr;
    }

    private static boolean isSquareClear(IsoGridSquare square) {
        return square != null
            && canSpawnAt(square)
            && !square.HasStairs()
            && !square.HasTree()
            && !square.getProperties().has(IsoFlagType.bed)
            && !square.getProperties().has(IsoFlagType.waterPiped);
    }

    private static boolean isSquareClear(IsoGridSquare square1, IsoDirections dir) {
        IsoGridSquare square2 = square1.getAdjacentSquare(dir);
        return isSquareClear(square2) && !square1.isSomethingTo(square2) && square1.getRoomID() == square2.getRoomID();
    }

    public static boolean is1x1AreaClear(IsoGridSquare square) {
        return isSquareClear(square);
    }

    public static boolean is1x2AreaClear(IsoGridSquare square) {
        return isSquareClear(square) && isSquareClear(square, IsoDirections.N);
    }

    public static boolean is2x1AreaClear(IsoGridSquare square) {
        return isSquareClear(square) && isSquareClear(square, IsoDirections.W);
    }

    public static boolean is2x1or1x2AreaClear(IsoGridSquare square) {
        return isSquareClear(square) && (isSquareClear(square, IsoDirections.W) || isSquareClear(square, IsoDirections.N));
    }

    public static boolean is2x2AreaClear(IsoGridSquare square) {
        return isSquareClear(square)
            && isSquareClear(square, IsoDirections.N)
            && isSquareClear(square, IsoDirections.W)
            && isSquareClear(square, IsoDirections.NW);
    }

    public static void alignCorpseToSquare(IsoGameCharacter chr, IsoGridSquare square) {
        int x = square.x;
        int y = square.y;
        IsoDirections dir = IsoDirections.fromIndex(Rand.Next(8));
        boolean b1x2Clear = is1x2AreaClear(square);
        boolean b2x1Clear = is2x1AreaClear(square);
        if (b1x2Clear && b2x1Clear) {
            b1x2Clear = Rand.Next(2) == 0;
            b2x1Clear = !b1x2Clear;
        }

        if (is2x2AreaClear(square)) {
            chr.setX(x);
            chr.setY(y);
        } else if (b1x2Clear) {
            chr.setX(x + 0.5F);
            chr.setY(y);
            dir = Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S;
        } else if (b2x1Clear) {
            chr.setX(x);
            chr.setY(y + 0.5F);
            dir = Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E;
        } else if (is1x2AreaClear(square.getAdjacentSquare(IsoDirections.S))) {
            chr.setX(x + 0.5F);
            chr.setY(y + 0.99F);
            dir = Rand.Next(2) == 0 ? IsoDirections.N : IsoDirections.S;
        } else if (is2x1AreaClear(square.getAdjacentSquare(IsoDirections.E))) {
            chr.setX(x + 0.99F);
            chr.setY(y + 0.5F);
            dir = Rand.Next(2) == 0 ? IsoDirections.W : IsoDirections.E;
        }

        chr.setDir(dir);
        chr.setLastX(chr.setNextX(chr.getX()));
        chr.setLastY(chr.setNextY(chr.getY()));
        chr.setScriptnx(chr.getX());
        chr.setScriptny(chr.getY());
    }

    /**
     * Get a random room in the building
     */
    public RoomDef getRandomRoom(BuildingDef bDef, int minArea) {
        return bDef.getRandomRoom(minArea, false);
    }

    public RoomDef getRandomRoomNoKids(BuildingDef bDef, int minArea) {
        return bDef.getRandomRoom(minArea, true);
    }

    /**
     * Return the wanted room
     */
    public RoomDef getRoom(BuildingDef bDef, String roomName) {
        return bDef.getRoom(roomName);
    }

    public RoomDef getRoomNoKids(BuildingDef bDef, String roomName) {
        return bDef.getRoom(roomName, true);
    }

    /**
     * Get either the living room or kitchen (in this order)
     */
    public RoomDef getLivingRoomOrKitchen(BuildingDef bDef) {
        RoomDef room = bDef.getRoom("livingroom");
        if (room == null) {
            room = bDef.getRoom("kitchen");
        }

        return room;
    }

    private static boolean canSpawnAt(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            return square.HasStairs() ? false : VirtualZombieManager.instance.canSpawnAt(square.x, square.y, square.z);
        }
    }

    public static IsoGridSquare getRandomSpawnSquare(RoomDef roomDef) {
        return roomDef == null ? null : roomDef.getRandomSquare(RandomizedWorldBase::canSpawnAt);
    }

    public static IsoGridSquare getRandomSquareForCorpse(RoomDef roomDef) {
        IsoGridSquare freeSQ = roomDef.getRandomSquare(RandomizedWorldBase::is2x2AreaClear);
        IsoGridSquare freeSQ2 = roomDef.getRandomSquare(RandomizedWorldBase::is2x1or1x2AreaClear);
        if (freeSQ == null || freeSQ2 != null && Rand.Next(4) == 0) {
            freeSQ = freeSQ2;
        }

        return freeSQ;
    }

    public BaseVehicle spawnCarOnNearestNav(String carName, BuildingDef def) {
        return this.spawnCarOnNearestNav(carName, def, null);
    }

    public BaseVehicle spawnCarOnNearestNav(String carName, BuildingDef def, String distribution) {
        IsoGridSquare square = null;
        int centerX = (def.x + def.x2) / 2;
        int centerY = (def.y + def.y2) / 2;

        for (int x = centerX; x < centerX + 20; x++) {
            IsoGridSquare testSquare = IsoCell.getInstance().getGridSquare(x, centerY, 0);
            if (testSquare != null && "Nav".equals(testSquare.getZoneType()) && !this.checkAreaForCarsSpawn(testSquare)) {
                square = testSquare;
                break;
            }
        }

        if (square != null) {
            return this.spawnCar(carName, square);
        } else {
            for (int xx = centerX; xx > centerX - 20; xx--) {
                IsoGridSquare testSquare = IsoCell.getInstance().getGridSquare(xx, centerY, 0);
                if (testSquare != null && "Nav".equals(testSquare.getZoneType()) && !this.checkAreaForCarsSpawn(testSquare)) {
                    square = testSquare;
                    break;
                }
            }

            if (square != null) {
                return this.spawnCar(carName, square);
            } else {
                for (int y = centerY; y < centerY + 20; y++) {
                    IsoGridSquare testSquare = IsoCell.getInstance().getGridSquare(centerX, y, 0);
                    if (testSquare != null && "Nav".equals(testSquare.getZoneType()) && !this.checkAreaForCarsSpawn(testSquare)) {
                        square = testSquare;
                        break;
                    }
                }

                if (square != null) {
                    return this.addVehicle(null, square, null, null, carName, null, null, distribution);
                } else {
                    for (int yx = centerY; yx > centerY - 20; yx--) {
                        IsoGridSquare testSquare = IsoCell.getInstance().getGridSquare(centerX, yx, 0);
                        if (testSquare != null && "Nav".equals(testSquare.getZoneType()) && !this.checkAreaForCarsSpawn(testSquare)) {
                            square = testSquare;
                            break;
                        }
                    }

                    if (square == null || this.checkAreaForCarsSpawn(square)) {
                        return null;
                    } else {
                        return distribution == null
                            ? this.spawnCar(carName, square, true)
                            : this.addVehicle(null, square, null, null, carName, null, null, distribution, true);
                    }
                }
            }
        }
    }

    public boolean checkAreaForCarsSpawn(IsoGridSquare square) {
        return this.checkRadiusForCarSpawn(square, 2);
    }

    public boolean checkRadiusForCarSpawn(IsoGridSquare square, int radius) {
        if (radius < 1) {
            return false;
        } else {
            for (int z = 0 - radius; z < radius; z++) {
                for (int y = 0 - radius; y < radius; y++) {
                    if (square != null && !square.isVehicleIntersecting() && !square.isSolid() && !square.isSolidTrans() && !square.HasTree()) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    private BaseVehicle spawnCar(String carName, IsoGridSquare square) {
        return this.spawnCar(carName, square, false);
    }

    private BaseVehicle spawnCar(String carName, IsoGridSquare square, boolean crashed) {
        BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
        v.setScriptName(carName);
        v.setX(square.x + 0.5F);
        v.setY(square.y + 0.5F);
        v.setZ(0.0F);
        v.savedRot.setAngleAxis(Rand.Next(0.0F, (float) (Math.PI * 2)), 0.0F, 1.0F, 0.0F);
        v.jniTransform.setRotation(v.savedRot);
        if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
            v.keySpawned = 1;
            v.setSquare(square);
            v.square.chunk.vehicles.add(v);
            v.chunk = v.square.chunk;
            v.addToWorld(crashed);
            VehiclesDB2.instance.addVehicle(v);
        }

        v.setGeneralPartCondition(0.3F, 70.0F);
        return v;
    }

    public InventoryItem addItemOnGround(IsoGridSquare square, String type) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(type) == 0.0F) {
            return null;
        } else {
            return square != null && !StringUtils.isNullOrWhitespace(type)
                ? ItemSpawner.spawnItem(type, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, true)
                : null;
        }
    }

    public InventoryItem addItemOnGroundNoLoot(IsoGridSquare square, String type) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(type) == 0.0F) {
            return null;
        } else {
            return square != null && !StringUtils.isNullOrWhitespace(type)
                ? ItemSpawner.spawnItem(type, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, false)
                : null;
        }
    }

    public static InventoryItem addItemOnGroundStatic(IsoGridSquare square, String type) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(type) == 0.0F) {
            return null;
        } else {
            return square != null && !StringUtils.isNullOrWhitespace(type)
                ? ItemSpawner.spawnItem(type, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F)
                : null;
        }
    }

    public InventoryItem addItemOnGround(IsoGridSquare square, InventoryItem item) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item.getFullType()) == 0.0F) {
            return null;
        } else {
            return square != null && item != null ? ItemSpawner.spawnItem(item, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F) : null;
        }
    }

    public InventoryItem addItemOnGround(IsoGridSquare square, InventoryItem item, boolean fill) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item.getFullType()) == 0.0F) {
            return null;
        } else {
            return square != null && item != null ? ItemSpawner.spawnItem(item, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, fill) : null;
        }
    }

    public InventoryItem addItemOnGroundNoLoot(IsoGridSquare square, InventoryItem item) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item.getFullType()) == 0.0F) {
            return null;
        } else {
            return square != null && item != null ? ItemSpawner.spawnItem(item, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F, false) : null;
        }
    }

    public static InventoryItem addItemOnGroundStatic(IsoGridSquare square, InventoryItem item) {
        if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item.getFullType()) == 0.0F) {
            return null;
        } else {
            return square != null && item != null ? ItemSpawner.spawnItem(item, square, Rand.Next(0.2F, 0.8F), Rand.Next(0.2F, 0.8F), 0.0F) : null;
        }
    }

    public void addRandomItemsOnGround(RoomDef room, String type, int count) {
        for (int i = 0; i < count; i++) {
            IsoGridSquare square = getRandomSpawnSquare(room);
            this.addItemOnGround(square, type);
        }
    }

    public void addRandomItemsOnGround(RoomDef room, ArrayList<String> types, int count) {
        for (int i = 0; i < count; i++) {
            IsoGridSquare square = getRandomSpawnSquare(room);
            this.addRandomItemOnGround(square, types);
        }
    }

    public InventoryItem addRandomItemOnGround(IsoGridSquare square, ArrayList<String> types) {
        if (square != null && !types.isEmpty()) {
            String type = PZArrayUtil.pickRandom(types);
            return this.addItemOnGround(square, type);
        } else {
            return null;
        }
    }

    /**
     * Create and return a weapon, if it's ranged you can ask for some bullets in it
     */
    public HandWeapon addWeapon(String type, boolean addRandomBullets) {
        HandWeapon weapon = InventoryItemFactory.CreateItem(type);
        if (weapon == null) {
            return null;
        } else {
            if (weapon.isRanged() && addRandomBullets) {
                if (!StringUtils.isNullOrWhitespace(weapon.getMagazineType())) {
                    weapon.setContainsClip(true);
                }

                weapon.setCurrentAmmoCount(Rand.Next(Math.max(weapon.getMaxAmmo() - 8, 0), weapon.getMaxAmmo() - 2));
            }

            return weapon;
        }
    }

    public IsoDeadBody createSkeletonCorpse(RoomDef room) {
        if (room == null) {
            return null;
        } else {
            IsoGridSquare freeSQ = room.getRandomSquare(RandomizedWorldBase::is2x1or1x2AreaClear);
            return freeSQ == null ? null : this.createCorpse(freeSQ, true);
        }
    }

    public IsoDeadBody createSkeletonCorpse(IsoGridSquare freeSQ) {
        return this.createCorpse(freeSQ, true);
    }

    public IsoDeadBody createCorpse(RoomDef room) {
        return this.createCorpse(room, false);
    }

    public IsoDeadBody createCorpse(RoomDef room, boolean skeleton) {
        if (room == null) {
            return null;
        } else {
            IsoGridSquare freeSQ = room.getRandomSquare(RandomizedWorldBase::is2x1or1x2AreaClear);
            return freeSQ == null ? null : this.createCorpse(freeSQ, skeleton);
        }
    }

    public IsoDeadBody createCorpse(IsoGridSquare freeSQ, boolean skeleton) {
        return freeSQ == null ? null : freeSQ.createCorpse(skeleton);
    }

    public IsoDeadBody createCorpse(IsoGridSquare freeSQ, IsoZombie zombie) {
        return freeSQ == null ? null : freeSQ.createCorpse(zombie);
    }

    /**
     * Check if the world age is correct for our definition
     */
    public boolean isTimeValid(boolean force) {
        if (this.minimumDays == 0 && this.maximumDays == 0) {
            return true;
        } else {
            float worldAgeDays = (float)GameTime.getInstance().getWorldAgeHours() / 24.0F;
            worldAgeDays += (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
            return this.minimumDays > 0 && worldAgeDays < this.minimumDays ? false : this.maximumDays <= 0 || !(worldAgeDays > this.maximumDays);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getDebugLine() {
        return this.debugLine;
    }

    public void setDebugLine(String debugLine) {
        this.debugLine = debugLine;
    }

    public int getMaximumDays() {
        return this.maximumDays;
    }

    public void setMaximumDays(int maximumDays) {
        this.maximumDays = maximumDays;
    }

    public boolean isUnique() {
        return this.unique;
    }

    public boolean isRat() {
        return this.isRat;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public static IsoGridSquare getSq(int x, int y, int z) {
        return IsoWorld.instance.getCell().getGridSquare(x, y, z);
    }

    public IsoObject addTileObject(int x, int y, int z, String spriteName) {
        return this.addTileObject(getSq(x, y, z), spriteName, false);
    }

    public IsoObject addTileObject(int x, int y, int z, String spriteName, boolean dirt) {
        return this.addTileObject(getSq(x, y, z), spriteName, dirt);
    }

    public IsoObject addTileObject(IsoGridSquare sq, String spriteName) {
        return this.addTileObject(sq, spriteName, false);
    }

    public IsoObject addTileObject(IsoGridSquare sq, String spriteName, boolean dirt) {
        if (sq == null) {
            return null;
        } else {
            RandomizedZoneStoryBase.cleanSquareForStory(getSq(sq.x, sq.y, sq.z));
            if (dirt) {
                sq.dirtStamp();
            }

            IsoObject obj = IsoObject.getNew(sq, spriteName, null, false);
            if (GameServer.server) {
                sq.transmitAddObjectToSquare(obj, -1);
            } else {
                sq.AddTileObject(obj);
            }

            MapObjects.newGridSquare(sq);
            MapObjects.loadGridSquare(sq);
            return obj;
        }
    }

    public IsoObject addTileObject(IsoGridSquare sq, IsoObject obj) {
        return this.addTileObject(sq, obj, false);
    }

    public IsoObject addTileObject(IsoGridSquare sq, IsoObject obj, boolean dirt) {
        if (sq == null) {
            return null;
        } else {
            RandomizedZoneStoryBase.cleanSquareForStory(getSq(sq.x, sq.y, sq.z));
            if (dirt) {
                sq.dirtStamp();
            }

            if (GameServer.server) {
                sq.transmitAddObjectToSquare(obj, -1);
            } else {
                sq.AddTileObject(obj);
            }

            MapObjects.newGridSquare(sq);
            MapObjects.loadGridSquare(sq);
            return obj;
        }
    }

    public void addSleepingBagOrTentNorthSouth(int x, int y, int z) {
        if (Rand.NextBool(2)) {
            this.addRandomTentNorthSouth(x, y, z);
        } else {
            this.addSleepingBagNorthSouth(x, y + 1, z);
        }
    }

    public void addSleepingBagOrTentWestEast(int x, int y, int z) {
        if (Rand.NextBool(2)) {
            this.addRandomTentWestEast(x, y, z);
        } else {
            this.addSleepingBagWestEast(x + 1, y, z);
        }
    }

    public void addRandomTentNorthSouth(int x, int y, int z) {
        if (Rand.NextBool(5)) {
            this.addTentNorthSouth(x, y - 1, z);
        } else {
            this.addTentNorthSouthNew(x, y, z);
        }
    }

    public void addRandomTentWestEast(int x, int y, int z) {
        if (Rand.NextBool(5)) {
            this.addTentWestEast(x - 1, y, z);
        } else {
            this.addTentWestEastNew(x, y, z);
        }
    }

    public void addRandomShelterNorthSouth(int x, int y, int z) {
        if (Rand.NextBool(2)) {
            this.addSleepingBagOrTentNorthSouth(x, y, z);
        } else {
            this.addShelterWestEast(x, y, z);
        }
    }

    public void addRandomShelterWestEast(int x, int y, int z) {
        if (Rand.NextBool(2)) {
            this.addSleepingBagOrTentWestEast(x, y, z);
        } else {
            this.addShelterNorthSouth(x, y, z);
        }
    }

    public void addTentNorthSouth(int x, int y, int z) {
        this.addTileObject(x, y - 1, z, "camping_01_1");
        this.addTileObject(x, y, z, "camping_01_0");
    }

    public void addTentWestEast(int x, int y, int z) {
        this.addTileObject(x - 1, y, z, "camping_01_2");
        this.addTileObject(x, y, z, "camping_01_3");
    }

    public void addMattressNorthSouth(int x, int y, int z) {
        this.addTileObject(x, y - 1, z, "carpentry_02_79", true);
        this.addTileObject(x, y, z, "carpentry_02_78", true);
    }

    public void addMattressWestEast(int x, int y, int z) {
        this.addTileObject(x - 1, y, z, "carpentry_02_76", true);
        this.addTileObject(x, y, z, "carpentry_02_77", true);
    }

    public void addSleepingBagNorthSouth(int x, int y, int z) {
        int i = Rand.Next(10) * 8 + 3;
        if (Rand.NextBool(2)) {
            i += 4;
        }

        this.addTileObject(x, y - 1, z, "camping_02_" + i);
        this.addTileObject(x, y, z, "camping_02_" + (i - 1));
    }

    public void addSleepingBagWestEast(int x, int y, int z) {
        int i = Rand.Next(10) * 8;
        if (Rand.NextBool(2)) {
            i += 4;
        }

        this.addTileObject(x - 1, y, z, "camping_02_" + i);
        this.addTileObject(x, y, z, "camping_02_" + (i + 1));
    }

    public void addShelterNorthSouth(int x, int y, int z) {
        int roll = Rand.Next(3);
        switch (roll) {
            case 0:
                this.addTileObject(x, y - 1, z, "camping_03_5", true);
                this.addTileObject(x, y, z, "camping_03_4", true);
                break;
            case 1:
                this.addTileObject(x, y - 1, z, "camping_03_13", true);
                this.addTileObject(x, y, z, "camping_03_12", true);
                break;
            case 2:
                this.addTileObject(x - 1, y, z, "camping_03_26", true);
                this.addTileObject(x, y, z, "camping_03_27", true);
        }
    }

    public void addShelterWestEast(int x, int y, int z) {
        int roll = Rand.Next(3);
        switch (roll) {
            case 0:
                this.addTileObject(x - 1, y, z, "camping_03_2", true);
                this.addTileObject(x, y, z, "camping_03_3", true);
                break;
            case 1:
                this.addTileObject(x - 1, y, z, "camping_03_14", true);
                this.addTileObject(x, y, z, "camping_03_15", true);
                break;
            case 2:
                this.addTileObject(x, y - 1, z, "camping_03_25", true);
                this.addTileObject(x, y, z, "camping_03_24", true);
        }
    }

    public void addTentNorthSouthNew(int x, int y, int z) {
        y -= 3;
        int tentType = Rand.Next(4) * 32;
        this.addTileObject(x - 1, y + 3, z, "camping_04_" + (0 + tentType));
        this.addTileObject(x - 1, y + 2, z, "camping_04_" + (1 + tentType));
        this.addTileObject(x - 1, y + 1, z, "camping_04_" + (2 + tentType));
        this.addTileObject(x - 1, y, z, "camping_04_" + (3 + tentType));
        this.addTileObject(x, y + 3, z, "camping_04_" + (4 + tentType));
        this.addTileObject(x, y + 2, z, "camping_04_" + (5 + tentType));
        this.addTileObject(x, y + 1, z, "camping_04_" + (6 + tentType));
        this.addTileObject(x, y, z, "camping_04_" + (7 + tentType));
    }

    public void addTentWestEastNew(int x, int y, int z) {
        int tentType = Rand.Next(4) * 32;
        this.addTileObject(x - 3, y - 1, z, "camping_04_" + (16 + tentType));
        this.addTileObject(x - 2, y - 1, z, "camping_04_" + (17 + tentType));
        this.addTileObject(x - 1, y - 1, z, "camping_04_" + (18 + tentType));
        this.addTileObject(x, y - 1, z, "camping_04_" + (19 + tentType));
        this.addTileObject(x - 3, y, z, "camping_04_" + (20 + tentType));
        this.addTileObject(x - 2, y, z, "camping_04_" + (21 + tentType));
        this.addTileObject(x - 1, y, z, "camping_04_" + (22 + tentType));
        this.addTileObject(x, y, z, "camping_04_" + (23 + tentType));
    }

    public BaseVehicle addTrailer(BaseVehicle v, Zone zone, IsoChunk chunk, String zoneName, String vehicleDistrib, String trailerName) {
        IsoGridSquare carSq = v.getSquare();
        IsoDirections carDir = v.getDir();
        int xoffset = 0;
        int yoffset = 0;
        if (carDir == IsoDirections.S) {
            yoffset = -3;
        }

        if (carDir == IsoDirections.N) {
            yoffset = 3;
        }

        if (carDir == IsoDirections.W) {
            xoffset = 3;
        }

        if (carDir == IsoDirections.E) {
            xoffset = -3;
        }

        BaseVehicle trailer = this.addVehicle(
            zone, getSq(carSq.x + xoffset, carSq.y + yoffset, carSq.z), chunk, zoneName, trailerName, null, carDir, vehicleDistrib
        );
        if (trailer != null) {
            v.positionTrailer(trailer);
        }

        return trailer;
    }

    public void addCampfire(IsoGridSquare sq) {
        sq.dirtStamp();
        this.addTileObject(sq, "camping_01_6", true);
    }

    public void addSimpleCookingPit(IsoGridSquare sq) {
        sq.dirtStamp();
        IsoObject object = new IsoFireplace(IsoWorld.instance.currentCell, sq, LuaManager.GlobalObject.getSprite("camping_03_16"));
        this.addTileObject(sq, object, true);
    }

    public void addCookingPit(IsoGridSquare sq) {
        sq.dirtStamp();
        IsoObject object = new IsoFireplace(IsoWorld.instance.currentCell, sq, LuaManager.GlobalObject.getSprite("camping_03_19"));
        this.addTileObject(sq, object, true);
    }

    public void addBrazier(IsoGridSquare sq) {
        sq.dirtStamp();
        IsoObject object = new IsoFireplace(IsoWorld.instance.currentCell, sq, LuaManager.GlobalObject.getSprite("crafted_02_42"));
        this.addTileObject(sq, object, true);
    }

    public void addSimpleFire(IsoGridSquare sq) {
        int roll = Rand.Next(2);
        IsoObject object = null;
        switch (roll) {
            case 0:
                this.addCampfire(sq);
                break;
            case 1:
                this.addSimpleCookingPit(sq);
        }
    }

    public void addRandomFirepit(IsoGridSquare sq) {
        int roll = Rand.Next(3);
        IsoObject object = null;
        switch (roll) {
            case 0:
                this.addCampfire(sq);
                break;
            case 1:
                this.addSimpleCookingPit(sq);
                break;
            case 2:
                this.addCookingPit(sq);
        }
    }

    public void addCampfireOrPit(IsoGridSquare sq) {
        int roll = Rand.Next(2);
        IsoObject object = null;
        switch (roll) {
            case 0:
                this.addCampfire(sq);
                break;
            case 1:
                this.addCookingPit(sq);
        }
    }

    public void dirtBomb(IsoGridSquare sq) {
        this.cleanSquareAndNeighbors(sq);
        sq.dirtStamp();
    }

    public void cleanSquareAndNeighbors(IsoGridSquare sq) {
        RandomizedZoneStoryBase.cleanSquareForStory(sq);
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.S));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.SE));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.E));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.NE));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.N));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.NW));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.W));
        RandomizedZoneStoryBase.cleanSquareForStory(sq.getAdjacentSquare(IsoDirections.SW));
    }

    public void addCharcoalBurner(IsoGridSquare sq) {
        this.dirtBomb(sq);
        GameEntityScript script = null;
        String sprite = null;
        int food = Rand.Next(4);
        switch (food) {
            case 0:
                script = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_DarkGreenBarrel");
                sprite = "crafted_02_50";
                break;
            case 1:
                script = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_LightGreenBarrel");
                sprite = "crafted_02_48";
                break;
            case 2:
                script = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_OrangeBarrel");
                sprite = "crafted_02_49";
                break;
            case 3:
                script = ScriptManager.instance.getGameEntityScript("Base.Charcoal_Burner_MetalDrum");
                sprite = "crafted_02_51";
        }

        if (script != null) {
            IsoBarbecue thumpable = new IsoBarbecue(IsoWorld.instance.getCell(), sq, IsoSpriteManager.instance.namedMap.get(sprite));
            GameEntityFactory.CreateIsoObjectEntity(thumpable, script, true);
            sq.AddSpecialObject(thumpable);
            thumpable.transmitCompleteItemToClients();
        }
    }

    public void addWorkstationEntity(IsoGridSquare sq, GameEntityScript script, String sprite) {
        IsoThumpable thumpable = new IsoThumpable(IsoWorld.instance.getCell(), sq, sprite, false, null);
        this.addWorkstationEntity(thumpable, sq, script, sprite);
    }

    public void addWorkstationEntity(IsoThumpable thumpable, IsoGridSquare sq, GameEntityScript script, String sprite) {
        if (script != null) {
            thumpable.setHealth(thumpable.getMaxHealth());
            thumpable.setBreakSound("BreakObject");
            GameEntityFactory.CreateIsoObjectEntity(thumpable, script, true);
            sq.AddSpecialObject(thumpable);
            thumpable.transmitCompleteItemToClients();
        }
    }

    public InventoryItem addItemToObjectSurface(String item, IsoObject object) {
        return object.spawnItemToObjectSurface(item, false);
    }

    public boolean isValidGraffSquare(IsoGridSquare sq, boolean north, boolean recursive) {
        if (sq == null || !sq.hasRoomDef()) {
            return false;
        } else if (sq.getRoom().getRoomDef().isKidsRoom()) {
            return false;
        } else if (north && !sq.getProperties().has(IsoFlagType.WallN)) {
            return false;
        } else if (!north && !sq.getProperties().has(IsoFlagType.WallW)) {
            return false;
        } else if (north && !sq.getProperties().has(IsoFlagType.WallNTrans)) {
            return false;
        } else if (!north && !sq.getProperties().has(IsoFlagType.WallWTrans)) {
            return false;
        } else if (north && sq.has(IsoFlagType.HoppableN)) {
            return false;
        } else if (!north && sq.has(IsoFlagType.HoppableW)) {
            return false;
        } else if (north && sq.getProperties().has(IsoFlagType.DoorWallN)) {
            return false;
        } else if (!north && sq.getProperties().has(IsoFlagType.DoorWallW)) {
            return false;
        } else if (north && sq.getProperties().has(IsoFlagType.WindowN)) {
            return false;
        } else if (!north && sq.getProperties().has(IsoFlagType.WindowW)) {
            return false;
        } else if (sq.getObjects().size() > 2 || sq.isOutside()) {
            return false;
        } else if (sq.getWall(north) == null || sq.getDoor(north) != null || sq.getWindow(north) != null) {
            return false;
        } else if (recursive && sq.getWall(!north) != null) {
            return false;
        } else if (north && recursive) {
            return this.isValidGraffSquare(sq.getAdjacentSquare(IsoDirections.W), true, false);
        } else {
            return recursive ? this.isValidGraffSquare(sq.getAdjacentSquare(IsoDirections.S), false, false) : true;
        }
    }

    public void graffSquare(IsoGridSquare sq, boolean north) {
        this.graffSquare(sq, null, north);
    }

    public void graffSquare(IsoGridSquare sq, String sprite, boolean north) {
        ArrayList<String> graffN = new ArrayList<>();
        graffN.add("overlay_graffiti_wall_01_103");
        ArrayList<String> graffW = new ArrayList<>();
        graffW.add("overlay_graffiti_wall_01_10");
        graffW.add("overlay_graffiti_wall_01_85");
        graffW.add("overlay_graffiti_wall_01_86");
        if (sprite == null) {
            if (north) {
                sprite = graffN.get(Rand.Next(graffN.size()));
            } else {
                sprite = graffW.get(Rand.Next(graffW.size()));
            }
        }

        if (sq != null) {
            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(sprite);
            if (Objects.equals(sprite, "overlay_graffiti_wall_01_40") || Objects.equals(sprite, "overlay_graffiti_wall_01_85")) {
                sq = sq.getAdjacentSquare(IsoDirections.S);
            }

            if (sq != null && spr.getProperties().has(IsoFlagType.WallOverlay)) {
                IsoObject wall = null;
                if (!north && spr.getProperties().has(IsoFlagType.attachedW)) {
                    wall = sq.getWall(false);
                } else if (north && spr.getProperties().has(IsoFlagType.attachedN)) {
                    wall = sq.getWall(true);
                }

                if (wall != null) {
                    if (wall.attachedAnimSprite == null) {
                        wall.attachedAnimSprite = new ArrayList<>(4);
                    }

                    wall.attachedAnimSprite.add(IsoSpriteInstance.get(spr));
                }
            }

            if (sq != null) {
                sq.RecalcAllWithNeighbours(true);
            }
        }
    }

    public void trashSquare(IsoGridSquare sq) {
        if (sq.getRoom() == null || sq.getRoom().getRoomDef() == null || !sq.getRoom().getRoomDef().isKidsRoom()) {
            int roll = Rand.Next(51);
            if (roll > 12) {
                roll += 12;
            }

            String sprite = "trash_01_" + roll;
            IsoObject trash = new IsoObject(IsoWorld.instance.currentCell, sq, sprite);
            sq.getObjects().add(trash);
            sq.RecalcProperties();
            if (GameServer.server) {
                trash.transmitCompleteItemToClients();
            }
        }
    }

    public String getBBQClutterItem() {
        if (bbqClutter != null && !bbqClutter.isEmpty()) {
            return bbqClutter.get(Rand.Next(bbqClutter.size()));
        } else {
            DebugLog.log("Error: clutter array bbqFloorClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getBBQClutter() {
        return bbqClutter;
    }

    public static String getBarnClutterItem() {
        if (barnClutter != null && !barnClutter.isEmpty()) {
            return barnClutter.get(Rand.Next(barnClutter.size()));
        } else {
            DebugLog.log("Error: clutter array barnClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getBarnClutter() {
        return barnClutter;
    }

    public String getBathroomSinkClutterItem() {
        if (bathroomSinkClutter != null && !bathroomSinkClutter.isEmpty()) {
            return bathroomSinkClutter.get(Rand.Next(bathroomSinkClutter.size()));
        } else {
            DebugLog.log("Error: clutter array bathroomSinkClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getBathroomSinkClutter() {
        return bathroomSinkClutter;
    }

    public String getBeachPartyClutterItem() {
        if (beachPartyClutter != null && !beachPartyClutter.isEmpty()) {
            return beachPartyClutter.get(Rand.Next(beachPartyClutter.size()));
        } else {
            DebugLog.log("Error: clutter array beachPartyClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getBeachPartyClutter() {
        return beachPartyClutter;
    }

    public String getBedClutterItem() {
        if (bedClutter != null && !bedClutter.isEmpty()) {
            return bedClutter.get(Rand.Next(bedClutter.size()));
        } else {
            DebugLog.log("Error: clutter array bedClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getBedClutter() {
        return bedClutter;
    }

    public String getCarpentryToolClutterItem() {
        if (carpentryToolClutter != null && !carpentryToolClutter.isEmpty()) {
            return carpentryToolClutter.get(Rand.Next(carpentryToolClutter.size()));
        } else {
            DebugLog.log("Error: clutter array carpentryToolClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getCarpentryToolClutter() {
        return carpentryToolClutter;
    }

    public static String getCafeClutterItem() {
        if (cafeClutter != null && !cafeClutter.isEmpty()) {
            return cafeClutter.get(Rand.Next(cafeClutter.size()));
        } else {
            DebugLog.log("Error: clutter array cafeClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getCafeClutter() {
        return cafeClutter;
    }

    public static String getDeadEndClutterItem() {
        if (deadEndClutter != null && !deadEndClutter.isEmpty()) {
            return deadEndClutter.get(Rand.Next(deadEndClutter.size()));
        } else {
            DebugLog.log("Error: clutter array deadEndClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getDeadEndClutter() {
        return deadEndClutter;
    }

    public static String getDormClutterItem() {
        if (dormClutter != null && !dormClutter.isEmpty()) {
            return dormClutter.get(Rand.Next(dormClutter.size()));
        } else {
            DebugLog.log("Error: clutter array dormClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getDormClutter() {
        return dormClutter;
    }

    public static String getFarmStorageClutterItem() {
        if (farmStorageClutter != null && !farmStorageClutter.isEmpty()) {
            return farmStorageClutter.get(Rand.Next(farmStorageClutter.size()));
        } else {
            DebugLog.log("Error: clutter array farmStorageClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getFarmStorageClutter() {
        return farmStorageClutter;
    }

    public static String getFootballNightDrinkItem() {
        if (footballNightDrinks != null && !footballNightDrinks.isEmpty()) {
            return footballNightDrinks.get(Rand.Next(footballNightDrinks.size()));
        } else {
            DebugLog.log("Error: clutter array footballNightDrinks doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getFootballNightDrinks() {
        return footballNightDrinks;
    }

    public static String getFootballNightSnackItem() {
        if (footballNightSnacks != null && !footballNightSnacks.isEmpty()) {
            return footballNightSnacks.get(Rand.Next(footballNightSnacks.size()));
        } else {
            DebugLog.log("Error: clutter array footballSnacks doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getFootballNightSnacks() {
        return footballNightSnacks;
    }

    public static String getGarageStorageClutterItem() {
        if (garageStorageClutter != null && !garageStorageClutter.isEmpty()) {
            return garageStorageClutter.get(Rand.Next(garageStorageClutter.size()));
        } else {
            DebugLog.log("Error: clutter array garageStorageClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getGarageStorageClutter() {
        return garageStorageClutter;
    }

    public static String getGigamartClutterItem() {
        if (gigamartClutter != null && !gigamartClutter.isEmpty()) {
            return gigamartClutter.get(Rand.Next(gigamartClutter.size()));
        } else {
            DebugLog.log("Error: clutter array gigamartClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getGigamartClutter() {
        return gigamartClutter;
    }

    public static String getGroceryClutterItem() {
        if (groceryClutter != null && !groceryClutter.isEmpty()) {
            return groceryClutter.get(Rand.Next(groceryClutter.size()));
        } else {
            DebugLog.log("Error: clutter array groceryClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getGroceryClutter() {
        return groceryClutter;
    }

    public static String getHairSalonClutterItem() {
        if (hairSalonClutter != null && !hairSalonClutter.isEmpty()) {
            return hairSalonClutter.get(Rand.Next(hairSalonClutter.size()));
        } else {
            DebugLog.log("Error: clutter array hairSalonClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHairSalonClutter() {
        return hairSalonClutter;
    }

    public static String getHallClutterItem() {
        if (hallClutter != null && !hallClutter.isEmpty()) {
            return hallClutter.get(Rand.Next(hallClutter.size()));
        } else {
            DebugLog.log("Error: clutter array hallClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHallClutter() {
        return hallClutter;
    }

    public static String getHenDoDrinkItem() {
        if (henDoDrinks != null && !henDoDrinks.isEmpty()) {
            return henDoDrinks.get(Rand.Next(henDoDrinks.size()));
        } else {
            DebugLog.log("Error: clutter array henDoDrinks doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHenDoDrinks() {
        return henDoDrinks;
    }

    public static String getHenDoSnackItem() {
        if (henDoSnacks != null && !henDoSnacks.isEmpty()) {
            return henDoSnacks.get(Rand.Next(henDoSnacks.size()));
        } else {
            DebugLog.log("Error: clutter array henDoSnacks doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHenDoSnacks() {
        return henDoSnacks;
    }

    public String getHoedownClutterItem() {
        if (hoedownClutter != null && !hoedownClutter.isEmpty()) {
            return hoedownClutter.get(Rand.Next(hoedownClutter.size()));
        } else {
            DebugLog.log("Error: clutter array hoedownClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHoedownClutter() {
        return hoedownClutter;
    }

    public String getHousePartyClutterItem() {
        if (housePartyClutter != null && !housePartyClutter.isEmpty()) {
            return housePartyClutter.get(Rand.Next(housePartyClutter.size()));
        } else {
            DebugLog.log("Error: clutter array housePartyClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getHousePartyClutter() {
        return housePartyClutter;
    }

    public static String getJudgeClutterItem() {
        if (judgeClutter != null && !judgeClutter.isEmpty()) {
            return judgeClutter.get(Rand.Next(judgeClutter.size()));
        } else {
            DebugLog.log("Error: clutter array judgeClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getJudgeClutter() {
        return judgeClutter;
    }

    public String getKidClutterItem() {
        if (kidClutter == null || kidClutter.isEmpty()) {
            DebugLog.log("Error: clutter array kidClutter doesn't exist");
            return null;
        } else {
            return Rand.NextBool(100) ? "Base.SpiffoBig" : kidClutter.get(Rand.Next(kidClutter.size()));
        }
    }

    public ArrayList<String> getKidClutter() {
        return kidClutter;
    }

    public String getKitchenCounterClutterItem() {
        if (kitchenCounterClutter != null && !kitchenCounterClutter.isEmpty()) {
            return kitchenCounterClutter.get(Rand.Next(kitchenCounterClutter.size()));
        } else {
            DebugLog.log("Error: clutter array kitchenCounterClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getKitchenCounterClutter() {
        return kitchenCounterClutter;
    }

    public String getKitchenSinkClutterItem() {
        if (kitchenSinkClutter != null && !kitchenSinkClutter.isEmpty()) {
            return kitchenSinkClutter.get(Rand.Next(kitchenSinkClutter.size()));
        } else {
            DebugLog.log("Error: clutter array kitchenSinkClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getKitchenSinkClutter() {
        return kitchenSinkClutter;
    }

    public String getKitchenStoveClutterItem() {
        if (kitchenStoveClutter != null && !kitchenStoveClutter.isEmpty()) {
            return kitchenStoveClutter.get(Rand.Next(kitchenStoveClutter.size()));
        } else {
            DebugLog.log("Error: clutter array kitchenStoveClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getKitchenStoveClutter() {
        return kitchenStoveClutter;
    }

    public String getLaundryRoomClutterItem() {
        if (laundryRoomClutter != null && !laundryRoomClutter.isEmpty()) {
            return laundryRoomClutter.get(Rand.Next(laundryRoomClutter.size()));
        } else {
            DebugLog.log("Error: clutter array laundryRoomClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getLaundryRoomClutter() {
        return laundryRoomClutter;
    }

    public String getLivingroomClutterItem() {
        if (livingRoomClutter != null && !livingRoomClutter.isEmpty()) {
            return livingRoomClutter.get(Rand.Next(livingRoomClutter.size()));
        } else {
            DebugLog.log("Error: clutter array livingRoomClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getLivingroomClutter() {
        return livingRoomClutter;
    }

    public static String getMedicallutterItem() {
        if (medicalClutter != null && !medicalClutter.isEmpty()) {
            return medicalClutter.get(Rand.Next(medicalClutter.size()));
        } else {
            DebugLog.log("Error: clutter array medicalClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getMedicalClutter() {
        return medicalClutter;
    }

    public static String getMurderSceneClutterItem() {
        if (murderSceneClutter != null && !murderSceneClutter.isEmpty()) {
            return murderSceneClutter.get(Rand.Next(murderSceneClutter.size()));
        } else {
            DebugLog.log("Error: clutter array murderSceneClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getMurderSceneClutter() {
        return murderSceneClutter;
    }

    public static String getNastyMattressClutterItem() {
        if (nastyMattressClutter != null && !nastyMattressClutter.isEmpty()) {
            return nastyMattressClutter.get(Rand.Next(nastyMattressClutter.size()));
        } else {
            DebugLog.log("Error: clutter array nastyMattressClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getNastyMattressClutter() {
        return nastyMattressClutter;
    }

    public static String getOldShelterClutterItem() {
        if (oldShelterClutter != null && !oldShelterClutter.isEmpty()) {
            return oldShelterClutter.get(Rand.Next(oldShelterClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officePaperworkClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOldShelterClutter() {
        return oldShelterClutter;
    }

    public static String getOfficeCarDealerClutterItem() {
        if (officeCarDealerClutter != null && !officeCarDealerClutter.isEmpty()) {
            return officeCarDealerClutter.get(Rand.Next(officeCarDealerClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officeCarDealerClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOfficeCarDealerClutter() {
        return officeCarDealerClutter;
    }

    public static String getOfficePaperworkClutterItem() {
        if (officePaperworkClutter != null && !officePaperworkClutter.isEmpty()) {
            return officePaperworkClutter.get(Rand.Next(officePaperworkClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officePaperworkClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOfficePaperworkClutter() {
        return officePaperworkClutter;
    }

    public static String getOfficePenClutterItem() {
        if (officePenClutter != null && !officePenClutter.isEmpty()) {
            return officePenClutter.get(Rand.Next(officePenClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officePenClutter. doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOfficePenClutter() {
        return officePenClutter;
    }

    public static String getOfficeOtherClutterItem() {
        if (officeOtherClutter != null && !officeOtherClutter.isEmpty()) {
            return officeOtherClutter.get(Rand.Next(officeOtherClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officeOtherClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOfficeOtherClutter() {
        return officeOtherClutter;
    }

    public static String getOfficeTreatClutterItem() {
        if (officeTreatClutter != null && !officeTreatClutter.isEmpty()) {
            return officeTreatClutter.get(Rand.Next(officeTreatClutter.size()));
        } else {
            DebugLog.log("Error: clutter array officeTreatClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getOfficeTreatClutter() {
        return officeTreatClutter;
    }

    public String getOvenFoodClutterItem() {
        if (ovenFoodClutter != null && !ovenFoodClutter.isEmpty()) {
            return ovenFoodClutter.get(Rand.Next(ovenFoodClutter.size()));
        } else {
            DebugLog.log("Error: clutter array ovenFood doesn't exist - will use jave-defined coldFood instead");
            return null;
        }
    }

    public ArrayList<String> getOvenFoodClutter() {
        return ovenFoodClutter;
    }

    public String getPillowClutterItem() {
        if (pillowClutter != null && !pillowClutter.isEmpty()) {
            return pillowClutter.get(Rand.Next(pillowClutter.size()));
        } else {
            DebugLog.log("Error: clutter array pillowClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getPillowClutter() {
        return pillowClutter;
    }

    public String getPokerNightClutterItem() {
        if (pokerNightClutter != null && !pokerNightClutter.isEmpty()) {
            return pokerNightClutter.get(Rand.Next(pokerNightClutter.size()));
        } else {
            DebugLog.log("Error: clutter array pokerNightClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getPokerNightClutter() {
        return pokerNightClutter;
    }

    public String getRichJerkClutterItem() {
        if (richJerkClutter != null && !richJerkClutter.isEmpty()) {
            return richJerkClutter.get(Rand.Next(richJerkClutter.size()));
        } else {
            DebugLog.log("Error: clutter array richJerkClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getRichJerkClutter() {
        return richJerkClutter;
    }

    public String getSadCampsiteClutterItem() {
        if (sadCampsiteClutter != null && !sadCampsiteClutter.isEmpty()) {
            return sadCampsiteClutter.get(Rand.Next(sadCampsiteClutter.size()));
        } else {
            DebugLog.log("Error: clutter array sadCampsiteClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getSadCampsiteClutter() {
        return sadCampsiteClutter;
    }

    public String getSidetableClutterItem() {
        if (sidetableClutter == null || sidetableClutter.isEmpty()) {
            DebugLog.log("Error: clutter array sidetableClutter doesn't exist");
            return null;
        } else {
            return Rand.NextBool(10) ? this.getWatchClutterItem() : sidetableClutter.get(Rand.Next(sidetableClutter.size()));
        }
    }

    public ArrayList<String> getSidetableClutter() {
        return sidetableClutter;
    }

    public String getSurvivalistCampsiteClutterItem() {
        if (survivalistCampsiteClutter != null && !survivalistCampsiteClutter.isEmpty()) {
            return survivalistCampsiteClutter.get(Rand.Next(survivalistCampsiteClutter.size()));
        } else {
            DebugLog.log("Error: clutter array survivalistCampsiteClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getSurvivalistCampsiteClutter() {
        return survivalistCampsiteClutter;
    }

    public static String getTwiggyClutterItem() {
        if (twiggyClutter != null && !twiggyClutter.isEmpty()) {
            return twiggyClutter.get(Rand.Next(twiggyClutter.size()));
        } else {
            DebugLog.log("Error: clutter array woodcraftClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getTwiggyClutter() {
        return twiggyClutter;
    }

    public String getUtilityToolClutterItem() {
        if (utilityToolClutter != null && !utilityToolClutter.isEmpty()) {
            return utilityToolClutter.get(Rand.Next(utilityToolClutter.size()));
        } else {
            DebugLog.log("Error: clutter array utilityToolClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getUtilityToolClutter() {
        return utilityToolClutter;
    }

    public String getVanCampClutterItem() {
        if (vanCampClutter != null && !vanCampClutter.isEmpty()) {
            return vanCampClutter.get(Rand.Next(vanCampClutter.size()));
        } else {
            DebugLog.log("Error: clutter array vanCampClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getVanCampClutter() {
        return vanCampClutter;
    }

    public String getWatchClutterItem() {
        if (watchClutter != null && !watchClutter.isEmpty()) {
            return watchClutter.get(Rand.Next(watchClutter.size()));
        } else {
            DebugLog.log("Error: clutter array watchClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getWatchClutter() {
        return watchClutter;
    }

    public static String getWoodcraftClutterItem() {
        if (woodcraftClutter != null && !woodcraftClutter.isEmpty()) {
            return woodcraftClutter.get(Rand.Next(woodcraftClutter.size()));
        } else {
            DebugLog.log("Error: clutter array woodcraftClutter doesn't exist");
            return null;
        }
    }

    public ArrayList<String> getWoodcraftClutter() {
        return woodcraftClutter;
    }

    public static String getClutterItem(ArrayList<String> clutterArray) {
        if (clutterArray != null && !clutterArray.isEmpty()) {
            return clutterArray.get(Rand.Next(clutterArray.size()));
        } else {
            DebugLog.log("Error: clutterArray doesn't exist");
            return null;
        }
    }

    public TIntObjectHashMap<String> getClutterCopy(ArrayList<String> clutter) {
        return this.getClutterCopy(clutter, new TIntObjectHashMap<>());
    }

    public TIntObjectHashMap<String> getClutterCopy(ArrayList<String> clutter, TIntObjectHashMap<String> copy) {
        copy.clear();

        for (int i = 0; i < clutter.size(); i++) {
            if (clutter.get(i) != null) {
                String entry = clutter.get(i);
                copy.put(i + 1, entry);
            }
        }

        return copy;
    }
}
