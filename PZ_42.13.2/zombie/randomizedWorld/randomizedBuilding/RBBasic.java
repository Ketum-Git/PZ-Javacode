// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBandPractice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBanditRaid;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBathroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBedroomZed;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSBleach;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSCorpsePsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDeadDrunk;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSDevouredByRats;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSFootballNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGrouchos;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunmanInBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSGunslinger;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHenDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHockeyPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSHouseParty;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPokerNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPoliceAtHouse;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscape;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSPrisonEscapeWithPolice;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRPGNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatInfested;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSRatKing;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSResourceGarage;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSkeletonPsycho;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSpecificProfession;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStagDo;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSStudentNight;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSSuicidePact;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSTinFoilHat;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombieLockedBathroom;
import zombie.randomizedWorld.randomizedDeadSurvivor.RDSZombiesEating;
import zombie.randomizedWorld.randomizedDeadSurvivor.RandomizedDeadSurvivorBase;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * This is a basic randomized building, some inside door will be opened, can
 *  have profession specific loots and cold cooked food in stove Also this type
 *  of house can have speicfic dead survivor/zombies/story inside them
 */
@UsedFromLua
public final class RBBasic extends RandomizedBuildingBase {
    private final ArrayList<String> specificProfessionDistribution = new ArrayList<>();
    private final Map<String, String> specificProfessionRoomDistribution = new HashMap<>();
    private final Map<String, String> plankStash = new HashMap<>();
    private final ArrayList<RandomizedDeadSurvivorBase> deadSurvivorsStory = new ArrayList<>();
    private int totalChanceRds;
    private static final HashMap<RandomizedDeadSurvivorBase, Integer> rdsMap = new HashMap<>();
    private static final ArrayList<String> uniqueRDSSpawned = new ArrayList<>();
    private ArrayList<IsoObject> tablesDone = new ArrayList<>();
    private boolean doneTable;
    private static final ObjectPool<TIntObjectHashMap<String>> s_clutterCopyPool = new ObjectPool<>(TIntObjectHashMap::new);

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.tablesDone = new ArrayList<>();
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean doPlankStash = Rand.NextBool(100);
        if ((this.getRoom(def, "kitchen") == null || this.getRoom(def, "hall") == null) && Rand.NextBool(20)) {
            RoomDef roomDef = null;
            IsoGridSquare sq = null;
            if (Rand.NextBool(2)) {
                roomDef = this.getRoom(def, "hall");
            }

            if (roomDef == null) {
                roomDef = this.getRoom(def, "kitchen");
            }

            if (roomDef != null) {
                sq = roomDef.getExtraFreeSquare();
            }

            if (sq != null) {
                this.addItemOnGround(sq, "Base.WaterDish");
                if (Rand.NextBool(3)) {
                    sq = roomDef.getExtraFreeSquare();
                    if (sq != null) {
                        String item = "Base.CatToy";
                        int rand = Rand.Next(6);
                        switch (rand) {
                            case 0:
                                item = "Base.CatFoodBag";
                                break;
                            case 1:
                                item = "Base.CatTreats";
                                break;
                            case 2:
                                item = "Base.DogChew";
                                break;
                            case 3:
                                item = "Base.DogFoodBag";
                                break;
                            case 4:
                                item = "Base.Leash";
                                break;
                            case 5:
                                item = "Base.DogTag_Pet";
                        }

                        this.addItemOnGround(sq, item);
                    }
                }
            }
        }

        RoomDef roomDefx = null;
        boolean kidsRoom = false;
        boolean didFireplaceStuff = false;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sqx = cell.getGridSquare(x, y, z);
                    if (sqx != null) {
                        if (doPlankStash && sqx.getFloor() != null && this.plankStash.containsKey(sqx.getFloor().getSprite().getName())) {
                            IsoThumpable cont = new IsoThumpable(sqx.getCell(), sqx, this.plankStash.get(sqx.getFloor().getSprite().getName()), false, null);
                            cont.setIsThumpable(false);
                            cont.container = new ItemContainer("plankstash", sqx, cont);
                            sqx.AddSpecialObject(cont);
                            sqx.RecalcAllWithNeighbours(true);
                            doPlankStash = false;
                        }

                        if (!didFireplaceStuff && sqx.hasFireplace()) {
                            if (Rand.NextBool(4)) {
                                this.addItemOnGround(sqx, "Base.Tongs");
                                didFireplaceStuff = true;
                            }

                            if (Rand.NextBool(4)) {
                                this.addItemOnGround(sqx, "Base.Bellows");
                                didFireplaceStuff = true;
                            }

                            if (Rand.NextBool(4)) {
                                this.addItemOnGround(sqx, "Base.FireplacePoker");
                                didFireplaceStuff = true;
                            }
                        }

                        for (int o = 0; o < sqx.getObjects().size(); o++) {
                            IsoObject obj = sqx.getObjects().get(o);
                            if (Rand.Next(100) <= 65
                                && obj instanceof IsoDoor door
                                && !door.getProperties().has("DoubleDoor")
                                && !door.getProperties().has("GarageDoor")
                                && !door.isExterior()) {
                                door.ToggleDoorSilent();
                                door.syncIsoObject(false, (byte)1, null, null);
                            }

                            if (obj instanceof IsoWindow window) {
                                if (Rand.NextBool(80)) {
                                    def.alarmed = false;
                                    window.ToggleWindow(null);
                                }

                                IsoCurtain curtain = window.HasCurtains();
                                if (curtain != null && Rand.NextBool(15)) {
                                    curtain.ToggleDoorSilent();
                                }
                            }

                            if (SandboxOptions.instance.survivorHouseChance.getValue() != 1) {
                                if (Rand.Next(100) < 15 && obj.getContainer() != null && obj.getContainer().getType().equals("stove")) {
                                    String foodString = this.getOvenFoodClutterItem();
                                    if (foodString != null) {
                                        InventoryItem food = obj.getContainer().AddItem(foodString);
                                        food.setCooked(true);
                                        food.setAutoAge();
                                    }
                                }

                                if (!this.tablesDone.contains(obj)
                                    && obj.getProperties().isTable()
                                    && obj.getContainer() == null
                                    && !this.doneTable
                                    && obj.hasAdjacentCanStandSquare()) {
                                    this.checkForTableSpawn(def, obj);
                                }
                            }
                        }

                        if (SandboxOptions.instance.survivorHouseChance.getValue() != 1 && sqx.getRoom() != null) {
                            if (roomDefx == null && sqx.getRoom().getRoomDef() != null) {
                                roomDefx = sqx.getRoom().getRoomDef();
                            } else if (sqx.getRoom().getRoomDef() != null && roomDefx != sqx.getRoom().getRoomDef()) {
                                roomDefx = sqx.getRoom().getRoomDef();
                                if ("kidsbedroom".equals(sqx.getRoom().getName())) {
                                    kidsRoom = true;
                                } else if ("bedroom".equals(sqx.getRoom().getName())) {
                                    kidsRoom = roomDefx.isKidsRoom();
                                } else {
                                    kidsRoom = false;
                                }
                            }

                            if (kidsRoom) {
                                this.doKidsBedroomStuff(sqx);
                            } else if ("kitchen".equals(sqx.getRoom().getName())) {
                                this.doKitchenStuff(sqx);
                            } else if ("barcountertwiggy".equals(sqx.getRoom().getName())) {
                                doTwiggyStuff(sqx);
                            } else if ("bathroom".equals(sqx.getRoom().getName())) {
                                this.doBathroomStuff(sqx);
                            } else if ("bedroom".equals(sqx.getRoom().getName())) {
                                this.doBedroomStuff(sqx);
                            } else if ("cafe".equals(sqx.getRoom().getName())) {
                                doCafeStuff(sqx);
                            } else if ("gigamart".equals(sqx.getRoom().getName())) {
                                doGroceryStuff(sqx);
                            } else if ("grocery".equals(sqx.getRoom().getName())) {
                                doGroceryStuff(sqx);
                            } else if ("livingroom".equals(sqx.getRoom().getName())) {
                                this.doLivingRoomStuff(sqx);
                            } else if ("office".equals(sqx.getRoom().getName())) {
                                doOfficeStuff(sqx);
                            } else if ("jackiejayestudio".equals(sqx.getRoom().getName())) {
                                doOfficeStuff(sqx);
                            } else if ("judgematthassset".equals(sqx.getRoom().getName())) {
                                doJudgeStuff(sqx);
                            } else if ("mayorwestpointoffice".equals(sqx.getRoom().getName())) {
                                doOfficeStuff(sqx);
                            } else if ("nolansoffice".equals(sqx.getRoom().getName())) {
                                doNolansOfficeStuff(sqx);
                            } else if ("woodcraftset".equals(sqx.getRoom().getName())) {
                                doWoodcraftStuff(sqx);
                            } else if ("laundry".equals(sqx.getRoom().getName())) {
                                this.doLaundryStuff(sqx);
                            } else if ("hall".equals(sqx.getRoom().getName())) {
                                doGeneralRoom(sqx, this.getHallClutter());
                            } else if ("garageStorage".equals(sqx.getRoom().getName()) || "garage".equals(sqx.getRoom().getName())) {
                                doGeneralRoom(sqx, this.getGarageStorageClutter());
                            }
                        }
                    }
                }
            }
        }

        if (Rand.Next(100) < 25) {
            this.addRandomDeadSurvivorStory(def);
            def.setAllExplored(true);
            def.alarmed = false;
        }

        this.doneTable = false;
    }

    public void forceVehicleDistribution(BaseVehicle vehicle, String distribution) {
        ItemPickerJava.VehicleDistribution distro = ItemPickerJava.VehicleDistributions.get(distribution);
        ItemPickerJava.ItemPickerRoom distro2 = distro.normal;

        for (int i = 0; i < vehicle.getPartCount(); i++) {
            VehiclePart part = vehicle.getPartByIndex(i);
            if (part.getItemContainer() != null) {
                if (GameServer.server && GameServer.softReset) {
                    part.getItemContainer().setExplored(false);
                }

                if (!part.getItemContainer().explored) {
                    part.getItemContainer().clear();
                    this.randomizeContainer(part, distro2);
                    part.getItemContainer().setExplored(true);
                }
            }
        }
    }

    private void randomizeContainer(VehiclePart part, ItemPickerJava.ItemPickerRoom distro2) {
        if (!GameClient.client) {
            if (distro2 != null) {
                ItemPickerJava.fillContainerType(distro2, part.getItemContainer(), "", null);
            }
        }
    }

    private void doLivingRoomStuff(IsoGridSquare sq) {
        this.doLivingRoomStuff(sq, this.getLivingroomClutter());
    }

    private void doLivingRoomStuff(IsoGridSquare sq, ArrayList<String> clutterArray) {
        if (sq.hasAdjacentCanStandSquare()) {
            if (clutterArray == null) {
                clutterArray = this.getLivingroomClutter();
            }

            IsoObject objToAdd = null;
            boolean TV = false;

            for (int o = 0; o < sq.getObjects().size(); o++) {
                IsoObject obj = sq.getObjects().get(o);
                if (obj instanceof IsoRadio || obj instanceof IsoTelevision) {
                    TV = true;
                    break;
                }

                boolean table = obj.getProperties().get("BedType") == null && obj.getSurfaceOffsetNoTable() > 0.0F && obj.getSurfaceOffsetNoTable() < 30.0F;
                if (table && Rand.NextBool(5)) {
                    objToAdd = obj;
                }
            }

            if (!TV && objToAdd != null) {
                String item = getClutterItem(clutterArray);
                if (item != null) {
                    objToAdd.spawnItemToObjectSurface(item, true);
                }
            }
        }
    }

    private void doBedroomStuff(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                boolean bed = obj.getSprite().getName().contains("bedding") && obj.getProperties().get("BedType") != null;
                boolean sidetable = obj.getContainer() != null && "sidetable".equals(obj.getContainer().getType());
                boolean pillow = false;
                IsoDirections facing = this.getFacing(obj.getSprite());
                IsoSpriteGrid grid = obj.getSprite().getSpriteGrid();
                if (bed && facing != null && grid != null) {
                    int gridPosX = grid.getSpriteGridPosX(obj.getSprite());
                    int gridPosY = grid.getSpriteGridPosY(obj.getSprite());
                    if (facing == IsoDirections.E && gridPosX == 0 && (gridPosY == 0 || gridPosY == 1)) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.W && gridPosX == 1 && (gridPosY == 0 || gridPosY == 1)) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.N && (gridPosX == 0 || gridPosX == 1) && gridPosY == 1) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.S && (gridPosX == 0 || gridPosX == 1) && gridPosY == 0) {
                        pillow = true;
                    }
                }

                int roll = 7;
                if (pillow) {
                    roll = 3;
                }

                if (bed && Rand.NextBool(roll)) {
                    if (!pillow) {
                        String item = this.getBedClutterItem();
                        obj.spawnItemToObjectSurface(item, true);
                        return;
                    }

                    String item = "Base.Pillow";
                    if (Rand.NextBool(100)) {
                        item = this.getPillowClutterItem();
                    }

                    if (facing == IsoDirections.E) {
                        this.addWorldItem(item, sq, 0.42F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(85, 95));
                        return;
                    }

                    if (facing == IsoDirections.W) {
                        this.addWorldItem(item, sq, 0.64F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(265, 275));
                        return;
                    }

                    if (facing == IsoDirections.N) {
                        this.addWorldItem(item, sq, Rand.Next(0.44F, 0.64F), 0.67F, obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(355, 365));
                        return;
                    }

                    if (facing == IsoDirections.S) {
                        this.addWorldItem(item, sq, Rand.Next(0.44F, 0.64F), 0.42F, obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(175, 185));
                        return;
                    }
                } else if (sidetable && Rand.NextBool(7)) {
                    String itemx = this.getSidetableClutterItem();
                    if (itemx != null && facing != null) {
                        obj.spawnItemToObjectSurface(itemx, true);
                        return;
                    }
                }
            }
        }
    }

    private void doKidsBedroomStuff(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                boolean bed = obj.getSprite().getName().contains("bedding") && obj.getProperties().get("BedType") != null;
                boolean sidetable = obj.getContainer() != null && "sidetable".equals(obj.getContainer().getType());
                boolean pillow = false;
                IsoDirections facing = this.getFacing(obj.getSprite());
                IsoSpriteGrid grid = obj.getSprite().getSpriteGrid();
                if (bed && facing != null && grid != null) {
                    int gridPosX = grid.getSpriteGridPosX(obj.getSprite());
                    int gridPosY = grid.getSpriteGridPosY(obj.getSprite());
                    if (facing == IsoDirections.E && gridPosX == 0 && (gridPosY == 0 || gridPosY == 1)) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.W && gridPosX == 1 && (gridPosY == 0 || gridPosY == 1)) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.N && (gridPosX == 0 || gridPosX == 1) && gridPosY == 1) {
                        pillow = true;
                    }

                    if (facing == IsoDirections.S && (gridPosX == 0 || gridPosX == 1) && gridPosY == 0) {
                        pillow = true;
                    }
                }

                int roll = 7;
                if (pillow) {
                    roll = 3;
                }

                if (bed && Rand.NextBool(roll)) {
                    if (!pillow) {
                        String item = this.getBedClutterItem();
                        if (Rand.NextBool(3)) {
                            item = this.getKidClutterItem();
                        }

                        obj.spawnItemToObjectSurface(item, true);
                        return;
                    }

                    String item = "Base.Pillow";
                    if (Rand.NextBool(20)) {
                        item = this.getKidClutterItem();
                    }

                    if (facing == IsoDirections.E) {
                        this.addWorldItem(item, sq, 0.42F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(85, 95));
                        return;
                    }

                    if (facing == IsoDirections.W) {
                        this.addWorldItem(item, sq, 0.64F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(265, 275));
                        return;
                    }

                    if (facing == IsoDirections.N) {
                        this.addWorldItem(item, sq, Rand.Next(0.44F, 0.64F), 0.67F, obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(355, 365));
                        return;
                    }

                    if (facing == IsoDirections.S) {
                        this.addWorldItem(item, sq, Rand.Next(0.44F, 0.64F), 0.42F, obj.getSurfaceOffsetNoTable() / 96.0F, Rand.Next(175, 185));
                        return;
                    }
                } else if (sidetable && Rand.NextBool(7)) {
                    String itemx = this.getKidClutterItem();
                    if (itemx != null && facing != null) {
                        obj.spawnItemToObjectSurface(itemx, true);
                        return;
                    }
                }
            }
        }
    }

    private void doKitchenStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> counterClutter = this.getClutterCopy(this.getKitchenCounterClutter(), s_clutterCopyPool.alloc());
        TIntObjectHashMap<String> sinkClutter = this.getClutterCopy(this.getKitchenSinkClutter(), s_clutterCopyPool.alloc());
        TIntObjectHashMap<String> stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        if (Rand.NextBool(100)) {
            sinkClutter.put(sinkClutter.size() + 1, "Base.PotScrubberFrog");
        }

        this.doKitchenStuff(sq, counterClutter, sinkClutter, stoveClutter);
        s_clutterCopyPool.release(counterClutter);
        s_clutterCopyPool.release(sinkClutter);
        s_clutterCopyPool.release(stoveClutter);
    }

    private void doKitchenStuff(
        IsoGridSquare sq, TIntObjectHashMap<String> counterClutter, TIntObjectHashMap<String> sinkClutter, TIntObjectHashMap<String> stoveClutter
    ) {
        boolean bReleaseCounter = counterClutter == null;
        boolean bReleaseSink = sinkClutter == null;
        boolean bReleaseStove = stoveClutter == null;
        if (counterClutter == null) {
            counterClutter = this.getClutterCopy(this.getKitchenCounterClutter(), s_clutterCopyPool.alloc());
        }

        if (sinkClutter == null) {
            sinkClutter = this.getClutterCopy(this.getKitchenSinkClutter(), s_clutterCopyPool.alloc());
        }

        if (stoveClutter == null) {
            stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        }

        boolean kitchenSink = false;
        boolean counter = false;

        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                if (!kitchenSink && obj.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
                    IsoDirections facing = this.getFacing(obj.getSprite());
                    if (facing != null) {
                        if (Rand.NextBool(100)) {
                            this.generateSinkClutter(facing, obj, sq, sinkClutter);
                        }

                        kitchenSink = true;
                    }
                } else if (!counter && obj.getContainer() != null && "counter".equals(obj.getContainer().getType()) && Rand.NextBool(6)) {
                    boolean doIt = true;

                    for (int o2 = 0; o2 < sq.getObjects().size(); o2++) {
                        IsoObject obj2 = sq.getObjects().get(o2);
                        if (obj2.getSprite() != null && obj2.getSprite().getName() != null && obj2.getSprite().getName().contains("sink")
                            || obj2 instanceof IsoStove
                            || obj2 instanceof IsoRadio) {
                            doIt = false;
                            break;
                        }
                    }

                    if (doIt) {
                        IsoDirections facing = this.getFacing(obj.getSprite());
                        if (facing != null) {
                            this.generateCounterClutter(facing, obj, sq, counterClutter);
                            counter = true;
                        }
                    }
                } else if (obj instanceof IsoStove && obj.getContainer() != null && "stove".equals(obj.getContainer().getType()) && Rand.NextBool(4)) {
                    IsoDirections facing = this.getFacing(obj.getSprite());
                    if (facing != null) {
                        this.generateKitchenStoveClutter(facing, obj, sq, stoveClutter);
                    }
                }
            }
        }

        if (bReleaseCounter) {
            s_clutterCopyPool.release(counterClutter);
        }

        if (bReleaseSink) {
            s_clutterCopyPool.release(sinkClutter);
        }

        if (bReleaseStove) {
            s_clutterCopyPool.release(stoveClutter);
        }
    }

    private void doBathroomStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> clutterCopy = this.getClutterCopy(this.getBathroomSinkClutter(), s_clutterCopyPool.alloc());
        this.doBathroomStuff(sq, clutterCopy);
        s_clutterCopyPool.release(clutterCopy);
    }

    private void doBathroomStuff(IsoGridSquare sq, TIntObjectHashMap<String> sinkClutter) {
        boolean bReleaseClutter = sinkClutter == null;
        if (sinkClutter == null) {
            sinkClutter = this.getClutterCopy(this.getBathroomSinkClutter(), s_clutterCopyPool.alloc());
        }

        boolean sink = false;
        boolean counter = false;
        boolean toilet = false;

        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                if (!toilet
                    && obj.getSprite().getProperties().has("CustomName")
                    && obj.getSprite().getProperties().get("CustomName").contains("Toilet")
                    && Rand.NextBool(5)) {
                    IsoDirections facing = this.getFacing(obj.getSprite());
                    String itemType = "Base.Plunger";
                    if (Rand.NextBool(2)) {
                        itemType = "Base.ToiletBrush";
                    }

                    if (Rand.NextBool(10)) {
                        itemType = "Base.ToiletPaper";
                    }

                    if (facing != null) {
                        toilet = true;
                        if (facing == IsoDirections.E) {
                            ItemSpawner.spawnItem(itemType, sq, 0.16F, 0.84F, 0.0F);
                        }

                        if (facing == IsoDirections.W) {
                            float position2 = 0.16F;
                            if (Rand.NextBool(2)) {
                                position2 = 0.84F;
                            }

                            ItemSpawner.spawnItem(itemType, sq, 0.84F, position2, 0.0F);
                        }

                        if (facing == IsoDirections.N) {
                            float position2 = 0.16F;
                            if (Rand.NextBool(2)) {
                                position2 = 0.84F;
                            }

                            ItemSpawner.spawnItem(itemType, sq, position2, 0.84F, 0.0F);
                        }

                        if (facing == IsoDirections.S) {
                            ItemSpawner.spawnItem(itemType, sq, 0.84F, 0.16F, 0.0F);
                        }
                        break;
                    }
                }

                if (!sink && !counter && obj.getSprite().getName().contains("sink") && Rand.NextBool(5) && obj.getSurfaceOffsetNoTable() > 0.0F) {
                    IsoDirections facingx = this.getFacing(obj.getSprite());
                    if (facingx != null) {
                        this.generateSinkClutter(facingx, obj, sq, sinkClutter);
                        sink = true;
                    }
                } else if (!sink && !counter && obj.getContainer() != null && "counter".equals(obj.getContainer().getType()) && Rand.NextBool(5)) {
                    boolean doIt = true;

                    for (int o2 = 0; o2 < sq.getObjects().size(); o2++) {
                        IsoObject obj2 = sq.getObjects().get(o2);
                        if (obj2.getSprite() != null && obj2.getSprite().getName() != null && obj2.getSprite().getName().contains("sink")
                            || obj2 instanceof IsoStove
                            || obj2 instanceof IsoRadio) {
                            doIt = false;
                            break;
                        }
                    }

                    if (doIt) {
                        IsoDirections facingx = this.getFacing(obj.getSprite());
                        if (facingx != null) {
                            this.generateCounterClutter(facingx, obj, sq, sinkClutter);
                            counter = true;
                        }
                    }
                }
            }
        }

        if (bReleaseClutter) {
            s_clutterCopyPool.release(sinkClutter);
        }
    }

    private void generateKitchenStoveClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq) {
        TIntObjectHashMap<String> clutterCopy = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        this.generateKitchenStoveClutter(facing, obj, sq, clutterCopy);
        s_clutterCopyPool.release(clutterCopy);
    }

    private void generateKitchenStoveClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> stoveClutter) {
        boolean bReleaseClutter = stoveClutter == null;
        if (stoveClutter == null) {
            stoveClutter = this.getClutterCopy(this.getKitchenStoveClutter(), s_clutterCopyPool.alloc());
        }

        int pos = Rand.Next(1, 3);
        String item = stoveClutter.get(Rand.Next(1, stoveClutter.size()));
        if (bReleaseClutter) {
            s_clutterCopyPool.release(stoveClutter);
        }

        if (facing == IsoDirections.W) {
            switch (pos) {
                case 1:
                    this.addWorldItem(item, sq, 0.5703125F, 0.8046875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    break;
                case 2:
                    this.addWorldItem(item, sq, 0.5703125F, 0.2578125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
            }
        }

        if (facing == IsoDirections.E) {
            switch (pos) {
                case 1:
                    this.addWorldItem(item, sq, 0.5F, 0.7890625F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    break;
                case 2:
                    this.addWorldItem(item, sq, 0.5F, 0.1875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
            }
        }

        if (facing == IsoDirections.S) {
            switch (pos) {
                case 1:
                    this.addWorldItem(item, sq, 0.3125F, 0.53125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    break;
                case 2:
                    this.addWorldItem(item, sq, 0.875F, 0.53125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
            }
        }

        if (facing == IsoDirections.N) {
            switch (pos) {
                case 1:
                    this.addWorldItem(item, sq, 0.3203F, 0.523475F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    break;
                case 2:
                    this.addWorldItem(item, sq, 0.8907F, 0.523475F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
            }
        }
    }

    private void generateCounterClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> itemMap) {
        int max = Math.min(5, itemMap.size() + 1);
        int nbrItem = Rand.Next(1, max);
        ArrayList<Integer> positions = new ArrayList<>();

        for (int i = 0; i < nbrItem; i++) {
            int rand = Rand.Next(1, 5);
            boolean added = false;

            while (!added) {
                if (!positions.contains(rand)) {
                    positions.add(rand);
                    added = true;
                } else {
                    rand = Rand.Next(1, 5);
                }
            }

            if (positions.size() == 4) {
            }
        }

        ArrayList<String> alreadyAdded = new ArrayList<>();

        for (int i = 0; i < positions.size(); i++) {
            int pos = positions.get(i);
            int randItem = Rand.Next(1, itemMap.size() + 1);
            String item = null;

            while (item == null) {
                item = itemMap.get(randItem);
                if (alreadyAdded.contains(item)) {
                    item = null;
                    randItem = Rand.Next(1, itemMap.size() + 1);
                }
            }

            if (item != null) {
                alreadyAdded.add(item);
                if (facing == IsoDirections.S) {
                    switch (pos) {
                        case 1:
                            this.addWorldItem(item, sq, 0.138F, Rand.Next(0.2F, 0.523F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            this.addWorldItem(item, sq, 0.383F, Rand.Next(0.2F, 0.523F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            this.addWorldItem(item, sq, 0.633F, Rand.Next(0.2F, 0.523F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            this.addWorldItem(item, sq, 0.78F, Rand.Next(0.2F, 0.523F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.N) {
                    switch (pos) {
                        case 1:
                            ItemSpawner.spawnItem(item, sq, 0.133F, Rand.Next(0.53125F, 0.9375F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            ItemSpawner.spawnItem(item, sq, 0.38F, Rand.Next(0.53125F, 0.9375F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            ItemSpawner.spawnItem(item, sq, 0.625F, Rand.Next(0.53125F, 0.9375F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            ItemSpawner.spawnItem(item, sq, 0.92F, Rand.Next(0.53125F, 0.9375F), obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.E) {
                    switch (pos) {
                        case 1:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.226F, 0.593F), 0.14F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.226F, 0.593F), 0.33F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.226F, 0.593F), 0.64F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.226F, 0.593F), 0.92F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.W) {
                    switch (pos) {
                        case 1:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.5859375F, 0.9F), 0.21875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.5859375F, 0.9F), 0.421875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.5859375F, 0.9F), 0.71F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            ItemSpawner.spawnItem(item, sq, Rand.Next(0.5859375F, 0.9F), 0.9175F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }
            }
        }
    }

    private void generateSinkClutter(IsoDirections facing, IsoObject obj, IsoGridSquare sq, TIntObjectHashMap<String> itemMap) {
        int max = Math.min(4, itemMap.size() + 1);
        int nbrItem = Rand.Next(1, max);
        ArrayList<Integer> positions = new ArrayList<>();

        for (int i = 0; i < nbrItem; i++) {
            int rand = Rand.Next(1, 5);
            boolean added = false;

            while (!added) {
                if (!positions.contains(rand)) {
                    positions.add(rand);
                    added = true;
                } else {
                    rand = Rand.Next(1, 5);
                }
            }

            if (positions.size() == 4) {
            }
        }

        ArrayList<String> alreadyAdded = new ArrayList<>();

        for (int i = 0; i < positions.size(); i++) {
            int pos = positions.get(i);
            int randItem = Rand.Next(1, itemMap.size() + 1);
            String item = null;

            while (item == null) {
                item = itemMap.get(randItem);
                if (alreadyAdded.contains(item)) {
                    item = null;
                    randItem = Rand.Next(1, itemMap.size() + 1);
                }
            }

            if (item != null) {
                alreadyAdded.add(item);
                if (facing == IsoDirections.S) {
                    switch (pos) {
                        case 1:
                            this.addWorldItem(item, sq, 0.71875F, 0.125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            this.addWorldItem(item, sq, 0.0935F, 0.21875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            this.addWorldItem(item, sq, 0.1328125F, 0.589375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            this.addWorldItem(item, sq, 0.7890625F, 0.589375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.N) {
                    switch (pos) {
                        case 1:
                            this.addWorldItem(item, sq, 0.921875F, 0.921875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            this.addWorldItem(item, sq, 0.1640625F, 0.8984375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            this.addWorldItem(item, sq, 0.021875F, 0.5F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            this.addWorldItem(item, sq, 0.8671875F, 0.5F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.E) {
                    switch (pos) {
                        case 1:
                            this.addWorldItem(item, sq, 0.234375F, 0.859375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            this.addWorldItem(item, sq, 0.59375F, 0.875F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            this.addWorldItem(item, sq, 0.53125F, 0.125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            this.addWorldItem(item, sq, 0.210937F, 0.1328125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }

                if (facing == IsoDirections.W) {
                    switch (pos) {
                        case 1:
                            this.addWorldItem(item, sq, 0.515625F, 0.109375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 2:
                            this.addWorldItem(item, sq, 0.578125F, 0.890625F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 3:
                            this.addWorldItem(item, sq, 0.8828125F, 0.8984375F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                            break;
                        case 4:
                            this.addWorldItem(item, sq, 0.8671875F, 0.1653125F, obj.getSurfaceOffsetNoTable() / 96.0F, true);
                    }
                }
            }
        }
    }

    private IsoDirections getFacing(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has("Facing")) {
            String Facing = sprite.getProperties().get("Facing");
            switch (Facing) {
                case "N":
                    return IsoDirections.N;
                case "S":
                    return IsoDirections.S;
                case "W":
                    return IsoDirections.W;
                case "E":
                    return IsoDirections.E;
            }
        }

        return null;
    }

    private void checkForTableSpawn(BuildingDef def, IsoObject table1) {
        if (table1.getSquare().getRoom() != null && Rand.NextBool(10)) {
            RBTableStoryBase tableStory = RBTableStoryBase.getRandomStory(table1.getSquare(), table1);
            if (tableStory != null) {
                tableStory.randomizeBuilding(def);
                this.doneTable = true;
            }
        }
    }

    private IsoObject checkForTable(IsoGridSquare sq, IsoObject table1) {
        if (!this.tablesDone.contains(table1) && sq != null) {
            for (int o = 0; o < sq.getObjects().size(); o++) {
                IsoObject obj = sq.getObjects().get(o);
                if (!this.tablesDone.contains(obj)
                    && obj.getProperties().isTable()
                    && obj.getProperties().getSurface() == 34
                    && obj.getContainer() == null
                    && obj != table1) {
                    return obj;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public void doProfessionStory(BuildingDef def, String professionChoosed) {
        this.spawnItemsInContainers(def, professionChoosed, 70);
    }

    private void addRandomDeadSurvivorStory(BuildingDef def) {
        this.initRDSMap(def);
        int choice = Rand.Next(this.totalChanceRds);
        Iterator<RandomizedDeadSurvivorBase> it = rdsMap.keySet().iterator();
        int subTotal = 0;

        while (it.hasNext()) {
            RandomizedDeadSurvivorBase testTable = it.next();
            subTotal += rdsMap.get(testTable);
            if (choice < subTotal) {
                testTable.randomizeDeadSurvivor(def);
                if (testTable.isUnique()) {
                    getUniqueRDSSpawned().add(testTable.getName());
                }
                break;
            }
        }
    }

    private void initRDSMap(BuildingDef def) {
        this.totalChanceRds = 0;
        rdsMap.clear();

        for (int i = 0; i < this.deadSurvivorsStory.size(); i++) {
            RandomizedDeadSurvivorBase story = this.deadSurvivorsStory.get(i);
            boolean noRats = SandboxOptions.instance.maximumRatIndex.getValue() <= 0;
            if (story.isValid(def, false)
                && story.isTimeValid(false)
                && (!story.isUnique() || !getUniqueRDSSpawned().contains(story.getName()))
                && (!noRats || !story.isRat())) {
                this.totalChanceRds = this.totalChanceRds + this.deadSurvivorsStory.get(i).getChance();
                rdsMap.put(this.deadSurvivorsStory.get(i), this.deadSurvivorsStory.get(i).getChance());
            }
        }
    }

    public void doRandomDeadSurvivorStory(BuildingDef buildingDef, RandomizedDeadSurvivorBase DSDef) {
        DSDef.randomizeDeadSurvivor(buildingDef);
    }

    public RBBasic() {
        this.name = "RBBasic";
        this.deadSurvivorsStory.add(new RDSBleach());
        this.deadSurvivorsStory.add(new RDSGunslinger());
        this.deadSurvivorsStory.add(new RDSGunmanInBathroom());
        this.deadSurvivorsStory.add(new RDSZombieLockedBathroom());
        this.deadSurvivorsStory.add(new RDSDeadDrunk());
        this.deadSurvivorsStory.add(new RDSSpecificProfession());
        this.deadSurvivorsStory.add(new RDSZombiesEating());
        this.deadSurvivorsStory.add(new RDSBanditRaid());
        this.deadSurvivorsStory.add(new RDSBandPractice());
        this.deadSurvivorsStory.add(new RDSBathroomZed());
        this.deadSurvivorsStory.add(new RDSBedroomZed());
        this.deadSurvivorsStory.add(new RDSFootballNight());
        this.deadSurvivorsStory.add(new RDSHenDo());
        this.deadSurvivorsStory.add(new RDSStagDo());
        this.deadSurvivorsStory.add(new RDSStudentNight());
        this.deadSurvivorsStory.add(new RDSPokerNight());
        this.deadSurvivorsStory.add(new RDSSuicidePact());
        this.deadSurvivorsStory.add(new RDSPrisonEscape());
        this.deadSurvivorsStory.add(new RDSPrisonEscapeWithPolice());
        this.deadSurvivorsStory.add(new RDSSkeletonPsycho());
        this.deadSurvivorsStory.add(new RDSCorpsePsycho());
        this.deadSurvivorsStory.add(new RDSPoliceAtHouse());
        this.deadSurvivorsStory.add(new RDSHouseParty());
        this.deadSurvivorsStory.add(new RDSTinFoilHat());
        this.deadSurvivorsStory.add(new RDSHockeyPsycho());
        this.deadSurvivorsStory.add(new RDSDevouredByRats());
        this.deadSurvivorsStory.add(new RDSRPGNight());
        this.deadSurvivorsStory.add(new RDSRatKing());
        this.deadSurvivorsStory.add(new RDSRatInfested());
        this.deadSurvivorsStory.add(new RDSResourceGarage());
        this.deadSurvivorsStory.add(new RDSGrouchos());
        this.specificProfessionDistribution.add("Carpenter");
        this.specificProfessionDistribution.add("Electrician");
        this.specificProfessionDistribution.add("Farmer");
        this.specificProfessionDistribution.add("Nurse");
        this.specificProfessionDistribution.add("Chef");
        this.specificProfessionRoomDistribution.put("Carpenter", "kitchen");
        this.specificProfessionRoomDistribution.put("Electrician", "kitchen");
        this.specificProfessionRoomDistribution.put("Farmer", "kitchen");
        this.specificProfessionRoomDistribution.put("Nurse", "kitchen;bathroom");
        this.specificProfessionRoomDistribution.put("Chef", "kitchen");
        this.plankStash.put("floors_interior_tilesandwood_01_40", "floors_interior_tilesandwood_01_56");
        this.plankStash.put("floors_interior_tilesandwood_01_41", "floors_interior_tilesandwood_01_57");
        this.plankStash.put("floors_interior_tilesandwood_01_42", "floors_interior_tilesandwood_01_58");
        this.plankStash.put("floors_interior_tilesandwood_01_43", "floors_interior_tilesandwood_01_59");
        this.plankStash.put("floors_interior_tilesandwood_01_44", "floors_interior_tilesandwood_01_60");
        this.plankStash.put("floors_interior_tilesandwood_01_45", "floors_interior_tilesandwood_01_61");
        this.plankStash.put("floors_interior_tilesandwood_01_46", "floors_interior_tilesandwood_01_62");
        this.plankStash.put("floors_interior_tilesandwood_01_47", "floors_interior_tilesandwood_01_63");
        this.plankStash.put("floors_interior_tilesandwood_01_52", "floors_interior_tilesandwood_01_68");
    }

    public ArrayList<RandomizedDeadSurvivorBase> getSurvivorStories() {
        return this.deadSurvivorsStory;
    }

    public ArrayList<String> getSurvivorProfession() {
        return this.specificProfessionDistribution;
    }

    public static ArrayList<String> getUniqueRDSSpawned() {
        return uniqueRDSSpawned;
    }

    public void doProfessionBuilding(BuildingDef def, String professionChoosed, ItemPickerJava.ItemPickerRoom prof) {
        Integer vehicleChance = Integer.valueOf(prof.vehicleChance);
        Integer femaleChance = Integer.valueOf(prof.femaleOdds);
        if (Core.debug) {
            DebugLog.log("Profession Female Chance: " + femaleChance);
        }

        if (prof.vehicleChance == null) {
            vehicleChance = 50;
        }

        if (prof.vehicles == null) {
            vehicleChance = null;
        }

        String vehicleDistribution = null;
        if (prof.vehicleDistribution != null) {
            vehicleDistribution = prof.vehicleDistribution;
        }

        if (Core.debug) {
            DebugLog.log("Profession House Initialized for " + professionChoosed + " at X: " + (def.x + def.x2) / 2 + ", Y: " + (def.y + def.y2) / 2);
        }

        String outfit = prof.outfit;
        IsoDeadBody body = null;
        ArrayList<IsoZombie> zombie = null;
        IsoZombie zomb = null;
        BaseVehicle profVehicle = null;
        IsoGridSquare sq = def.getFreeSquareInRoom();
        boolean doBag = Rand.Next(2) == 0;
        if (prof.bagType == null) {
            doBag = false;
        }

        boolean didBag = false;
        InventoryContainer bag = null;
        InventoryItem key2 = null;
        if (doBag) {
            DebugLog.log("Trying to spawn Profession Bag: " + prof.bagType);
            bag = InventoryItemFactory.CreateItem(prof.bagType);
            if (bag != null) {
                DebugLog.log("Profession Bag Spawned: " + prof.bagType);
                if (prof.bagTable != null) {
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(prof.bagTable));
                } else {
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                }

                String keyType = "Base.Key1";
                key2 = InventoryItemFactory.CreateItem("Base.Key1");
                if (key2 != null) {
                    key2.setKeyId(def.getKeyId());
                    ItemPickerJava.KeyNamer.nameKey(key2, sq);
                }

                if (bag.getItemContainer() != null) {
                    bag.getItemContainer().addItem(key2);
                }
            }
        }

        String vehicleType = null;
        if (prof.vehicle != null) {
            vehicleType = prof.vehicle;
        }

        if (vehicleType != null && vehicleChance != null && Rand.Next(100) < vehicleChance) {
            if (Core.debug) {
                DebugLog.log("Trying to spawn Profession vehicle: " + vehicleType);
            }

            profVehicle = this.spawnCarOnNearestNav(vehicleType, def, vehicleDistribution);
            if (profVehicle != null && Core.debug) {
                DebugLog.log(
                    "Profession Vehicle "
                        + profVehicle.getScriptName()
                        + " for "
                        + professionChoosed
                        + " at X: "
                        + profVehicle.getX()
                        + ", Y: "
                        + profVehicle.getY()
                );
            }
        }

        boolean corpseOrZombie = profVehicle == null;
        if (Rand.Next(2) == 0) {
            corpseOrZombie = true;
        }

        if (corpseOrZombie && sq != null) {
            boolean isFemale = Rand.Next(100) < femaleChance;
            if (isFemale) {
                femaleChance = 100;
            } else {
                femaleChance = 0;
            }

            if (isFemale && prof.outfitFemale != null) {
                outfit = prof.outfitFemale;
            }

            if (!isFemale && prof.outfitMale != null) {
                outfit = prof.outfitMale;
            }

            if (outfit != null) {
                body = createRandomDeadBody(sq, null, false, 0, 0, outfit, femaleChance);
            } else {
                body = createRandomDeadBody(sq, null, false, 0, 0, null, femaleChance);
            }

            if (body != null) {
                if (Core.debug) {
                    DebugLog.log(
                        "Profession Corpse for "
                            + professionChoosed
                            + " at X: "
                            + body.getX()
                            + ", Y: "
                            + body.getY()
                            + ", Z: "
                            + body.getZ()
                            + " in Outfit "
                            + outfit
                    );
                }

                String keyTypex = "Base.Key1";
                InventoryItem key = body.getItemContainer().AddItem("Base.Key1");
                if (key != null) {
                    key.setKeyId(def.getKeyId());
                    ItemPickerJava.KeyNamer.nameKey(key, sq);
                }

                ItemPickerJava.rollItem(prof.containers.get("body"), body.getContainer(), true, null, null);
            }

            if (body != null && doBag && !didBag && bag != null) {
                if (sq.getRoom() != null && Objects.requireNonNull(body.getSquare().getRoom()).getRandomFreeSquare() != null) {
                    this.addItemOnGround(sq.getRoom().getRandomFreeSquare(), bag);
                } else {
                    this.addItemOnGround(body.getSquare(), bag);
                }

                didBag = true;
                if (Core.debug) {
                    DebugLog.log("Profession Bag Spawned With Corpse");
                }
            }
        }

        if (profVehicle != null) {
            InventoryItem key = profVehicle.createVehicleKey();
            if (zomb != null) {
                zomb.addItemToSpawnAtDeath(key);
            }

            if (body != null) {
                body.getContainer().AddItem(key);
            }

            if (zomb == null && body == null && sq != null) {
                ItemContainer cont = Objects.requireNonNull(sq.getBuilding()).getRandomContainerSingle("sidetable");
                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("dresser");
                }

                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("counter");
                }

                if (cont == null) {
                    cont = sq.getBuilding().getRandomContainerSingle("wardrobe");
                }

                if (cont != null) {
                    if (doBag && !didBag && bag != null) {
                        cont.addItem(bag);
                        if (bag.getItemContainer() != null) {
                            bag.getItemContainer().addItem(key);
                        } else {
                            profVehicle.putKeyToContainer(cont, cont.getParent().getSquare(), cont.getParent());
                        }

                        didBag = true;
                        if (Core.debug) {
                            DebugLog.log(
                                "Profession Bag and Profession Vehicle Key Spawned in "
                                    + cont.getType()
                                    + " at X: "
                                    + cont.getParent().getX()
                                    + ", Y: "
                                    + cont.getParent().getY()
                                    + ", Z: "
                                    + cont.getParent().getZ()
                            );
                        }
                    } else {
                        profVehicle.putKeyToContainer(cont, cont.getParent().getSquare(), cont.getParent());
                        if (Core.debug) {
                            DebugLog.log(
                                "Profession Vehicle Key Spawned in "
                                    + cont.getType()
                                    + " at X: "
                                    + cont.getParent().getX()
                                    + ", Y: "
                                    + cont.getParent().getY()
                                    + ", Z: "
                                    + cont.getParent().getZ()
                            );
                        }
                    }
                } else if (doBag && !didBag && bag != null) {
                    this.addItemOnGround(sq, bag);
                    if (bag.getItemContainer() != null) {
                        bag.getItemContainer().addItem(key);
                    } else {
                        profVehicle.putKeyToWorld(sq);
                    }

                    didBag = true;
                    if (Core.debug) {
                        DebugLog.log("Profession Bag and Profession Vehicle Key Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                    }
                } else {
                    profVehicle.putKeyToWorld(sq);
                    if (Core.debug) {
                        DebugLog.log("Profession Vehicle Key Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                    }
                }
            }
        }

        if (body != null && Rand.Next(2) == 0) {
            body.reanimateNow();
            if (Core.debug) {
                DebugLog.log("Profession Corpse promoted to Zombie at X: " + body.getX() + ", Y: " + body.getY() + ", Z: " + body.getZ());
            }
        }

        if (zomb != null && Rand.Next(2) == 0) {
            zomb.Kill(null, true);
            if (Core.debug) {
                DebugLog.log("Profession Zombie promoted to Corpse at X: " + zomb.getX() + ", Y: " + zomb.getY() + ", Z: " + zomb.getZ());
            }
        }

        if (doBag && !didBag && bag != null && sq != null) {
            ItemContainer contx = Objects.requireNonNull(sq.getBuilding()).getRandomContainer("sidetable");
            if (contx == null) {
                contx = sq.getBuilding().getRandomContainerSingle("dresser");
            }

            if (contx == null) {
                contx = sq.getBuilding().getRandomContainerSingle("wardrobe");
            }

            if (contx == null) {
                contx = sq.getBuilding().getRandomContainerSingle("counter");
            }

            if (contx != null) {
                contx.addItem(bag);
                if (Core.debug) {
                    DebugLog.log(
                        "Profession Bag Spawned in "
                            + contx.getType()
                            + " at X: "
                            + contx.getParent().getX()
                            + ", Y: "
                            + contx.getParent().getY()
                            + ", Z: "
                            + contx.getParent().getZ()
                    );
                }
            } else {
                this.addItemOnGround(sq, bag);
                if (Core.debug) {
                    DebugLog.log("Profession Bag Spawned at X: " + sq.getX() + ", Y: " + sq.getY() + ", Z: " + sq.getZ());
                }
            }
        }
    }

    public static void doOfficeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()
                && Rand.NextBool(2)
                && sq.getObjects().size() == 2
                && obj.getProperties().get("BedType") == null
                && obj.isTableSurface()
                && (obj.getContainer() == null || "desk".equals(obj.getContainer().getType()))) {
                if (Rand.Next(100) < 66) {
                    ItemSpawner.spawnItem(getOfficePenClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }

                if (Rand.Next(100) < 66) {
                    ItemSpawner.spawnItem(
                        getOfficePaperworkClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                    );
                }

                if (Rand.Next(100) < 66) {
                    ItemSpawner.spawnItem(getOfficeOtherClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }

                if (Rand.Next(100) < 20) {
                    ItemSpawner.spawnItem(getOfficeTreatClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }

    public static void doNolansOfficeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()
                && Rand.Next(100) < 80
                && obj.getProperties().get("BedType") == null
                && (obj.getContainer() == null || "desk".equals(obj.getContainer().getType()))) {
                ItemSpawner.spawnItem(getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 50) {
                    ItemSpawner.spawnItem(
                        getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                    );
                    ItemSpawner.spawnItem(
                        getOfficeCarDealerClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                    );
                }
            }
        }
    }

    public static void doCafeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()) {
                if (Rand.NextBool(3)) {
                    ItemSpawner.spawnItem("MugWhite", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }

                if (Rand.Next(100) < 40
                    && sq.getObjects().size() == 2
                    && obj.getProperties().get("BedType") == null
                    && (obj.getContainer() == null || "desk".equals(obj.getContainer().getType()))) {
                    ItemSpawner.spawnItem(getCafeClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                    if (Rand.Next(100) < 40) {
                        ItemSpawner.spawnItem(getCafeClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                    }
                }
            }
        }
    }

    public static void doGigamartStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface() && Rand.Next(100) < 30 && sq.getObjects().size() == 2 && obj.getContainer() != null) {
                ItemSpawner.spawnItem(getGigamartClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 40) {
                    ItemSpawner.spawnItem(getGigamartClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }

    public static void doGroceryStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface() && Rand.Next(100) < 20 && sq.getObjects().size() == 2 && obj.getContainer() != null) {
                ItemSpawner.spawnItem(getGroceryClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 40) {
                    ItemSpawner.spawnItem(getGroceryClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }

    public static void doGeneralRoom(IsoGridSquare sq, ArrayList<String> clutter) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface() && sq.getObjects().size() <= 3 && Rand.NextBool(3)) {
                String item = getClutterItem(clutter);
                obj.spawnItemToObjectSurface(item, true);
            }
        }
    }

    private void doLaundryStuff(IsoGridSquare sq) {
        TIntObjectHashMap<String> clutter = this.getClutterCopy(this.getLaundryRoomClutter(), s_clutterCopyPool.alloc());
        boolean kitchenSink = false;
        boolean counter = false;

        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            IsoDirections facing = this.getFacing(obj.getSprite());
            boolean isCounter = obj.getContainer() != null && "counter".equals(obj.getContainer().getType());
            if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                if (!kitchenSink && obj.getSprite().getName().contains("sink") && Rand.NextBool(4)) {
                    if (facing != null) {
                        this.generateSinkClutter(facing, obj, sq, clutter);
                        kitchenSink = true;
                    }
                } else if (!counter && isCounter && Rand.NextBool(6)) {
                    boolean doIt = true;

                    for (int o2 = 0; o2 < sq.getObjects().size(); o2++) {
                        IsoObject obj2 = sq.getObjects().get(o2);
                        if (obj2.getSprite() != null && obj2.getSprite().getName() != null && obj2.getSprite().getName().contains("sink")
                            || obj2 instanceof IsoStove
                            || obj2 instanceof IsoRadio) {
                            doIt = false;
                            break;
                        }
                    }

                    if (doIt && facing != null) {
                        this.generateCounterClutter(facing, obj, sq, clutter);
                        counter = true;
                    }
                } else if (!isCounter && obj.isTableSurface() && sq.getObjects().size() <= 3 && Rand.NextBool(3)) {
                    String item = this.getLaundryRoomClutterItem();
                    obj.spawnItemToObjectSurface(item, true);
                }
            }
        }

        s_clutterCopyPool.release(clutter);
    }

    public static void doJudgeStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()
                && Rand.Next(100) < 80
                && obj.getProperties().get("BedType") == null
                && (obj.getContainer() == null || "desk".equals(obj.getContainer().getType()))) {
                ItemSpawner.spawnItem(getJudgeClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 50) {
                    ItemSpawner.spawnItem(getJudgeClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                    ItemSpawner.spawnItem(getJudgeClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }

    public static void doTwiggyStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()
                && Rand.Next(100) < 50
                && obj.getProperties().get("BedType") == null
                && (obj.getContainer() == null || "counter".equals(obj.getContainer().getType()))) {
                ItemSpawner.spawnItem(getTwiggyClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 30) {
                    ItemSpawner.spawnItem(getTwiggyClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }

    public static void doWoodcraftStuff(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject obj = sq.getObjects().get(i);
            if (obj.isTableSurface()
                && Rand.Next(100) < 80
                && obj.getProperties().get("BedType") == null
                && (obj.getContainer() == null || "desk".equals(obj.getContainer().getType()))) {
                ItemSpawner.spawnItem(getWoodcraftClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                if (Rand.Next(100) < 50) {
                    ItemSpawner.spawnItem(getWoodcraftClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                    ItemSpawner.spawnItem(getWoodcraftClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                }
            }
        }
    }
}
