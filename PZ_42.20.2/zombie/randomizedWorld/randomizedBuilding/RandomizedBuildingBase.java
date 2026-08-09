// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.Core;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.util.StringUtils;
import zombie.util.list.WeightedList;

@UsedFromLua
public class RandomizedBuildingBase extends RandomizedWorldBase {
    private int chance;
    private static int totalChance;
    private static final HashMap<RandomizedBuildingBase, Integer> rbMap = new HashMap<>();
    protected static final int KBBuildingX = 10744;
    protected static final int KBBuildingY = 9409;
    private boolean alwaysDo;
    public static int maximumRoomCount = 500;
    private static final HashMap<String, String> weaponsList = new HashMap<>();

    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
    }

    public void init() {
        if (weaponsList.isEmpty()) {
            weaponsList.put("Base.Shotgun", "Base.ShotgunShellsBox");
            weaponsList.put("Base.Pistol", "Base.Bullets9mmBox");
            weaponsList.put("Base.Pistol2", "Base.Bullets45Box");
            weaponsList.put("Base.Pistol3", "Base.Bullets44Box");
            weaponsList.put("Base.VarmintRifle", "Base.556Box");
            weaponsList.put("Base.HuntingRifle", "Base.308Box");
        }
    }

    public static void initAllRBMapChance() {
        for (int i = 0; i < IsoWorld.instance.getRandomizedBuildingList().size(); i++) {
            totalChance = totalChance + IsoWorld.instance.getRandomizedBuildingList().get(i).getChance();
            rbMap.put(IsoWorld.instance.getRandomizedBuildingList().get(i), IsoWorld.instance.getRandomizedBuildingList().get(i).getChance());
        }
    }

    /**
     * Don't do any building change in a player's building Also check if the
     *  building have a bathroom, a kitchen and a bedroom
     *  This is ignored for the alwaysDo building (so i can do stuff in spiffo, pizzawhirled, etc..)
     */
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else {
            if (!GameServer.server) {
                if (!force
                    && IsoPlayer.getInstance().getSquare() != null
                    && IsoPlayer.getInstance().getSquare().getBuilding() != null
                    && IsoPlayer.getInstance().getSquare().getBuilding().def == def) {
                    this.customizeStartingHouse(IsoPlayer.getInstance().getSquare().getBuilding().def);
                    return false;
                }
            } else if (!force) {
                for (int i = 0; i < GameServer.Players.size(); i++) {
                    IsoPlayer player = GameServer.Players.get(i);
                    if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                        return false;
                    }
                }
            }

            boolean bedroom = false;
            boolean kitchen = false;
            boolean bathroom = false;

            for (int ix = 0; ix < def.rooms.size(); ix++) {
                RoomDef room = def.rooms.get(ix);
                if ("bedroom".equals(room.name)) {
                    bedroom = true;
                }

                if ("kitchen".equals(room.name) || "livingroom".equals(room.name)) {
                    kitchen = true;
                }

                if ("bathroom".equals(room.name)) {
                    bathroom = true;
                }
            }

            if (!bedroom) {
                this.debugLine = this.debugLine + "no bedroom ";
            }

            if (!bathroom) {
                this.debugLine = this.debugLine + "no bathroom ";
            }

            if (!kitchen) {
                this.debugLine = this.debugLine + "no living room or kitchen ";
            }

            return bedroom && bathroom && kitchen;
        }
    }

    private void customizeStartingHouse(BuildingDef def) {
    }

    public int getMinimumDays() {
        return this.minimumDays;
    }

    public void setMinimumDays(int minimumDays) {
        this.minimumDays = minimumDays;
    }

    public int getMinimumRooms() {
        return this.minimumRooms;
    }

    public void setMinimumRooms(int minimumRooms) {
        this.minimumRooms = minimumRooms;
    }

    public static void ChunkLoaded(IsoBuilding building) {
        boolean debugSpam = false;
        if (!GameClient.client && building.def != null && !building.def.seen && building.def.isFullyStreamedIn()) {
            int roomCount = building.rooms.size();
            boolean tooManyRooms = roomCount > maximumRoomCount;
            if (GameServer.server && GameServer.Players.isEmpty()) {
                return;
            }

            for (int i = 0; i < roomCount; i++) {
                if (building.rooms.get(i).def.explored) {
                    return;
                }
            }

            building.def.seen = true;
            if (!building.def.isAnyChunkNewlyLoaded()) {
                return;
            }

            ArrayList<RandomizedBuildingBase> forcedStory = new ArrayList<>();
            if (!tooManyRooms) {
                for (int ix = 0; ix < IsoWorld.instance.getRandomizedBuildingList().size(); ix++) {
                    RandomizedBuildingBase testTable = IsoWorld.instance.getRandomizedBuildingList().get(ix);
                    if (testTable.reallyAlwaysForce && testTable.isValid(building.def, false)) {
                        testTable.randomizeBuilding(building.def);
                    } else if (testTable.isAlwaysDo() && testTable.isValid(building.def, false)) {
                        forcedStory.add(testTable);
                    }
                }
            }

            if (tooManyRooms) {
                DebugLog.log(
                    "Building is too large for a  Building Story with "
                        + roomCount
                        + " rooms  at "
                        + building.def.x
                        + ", "
                        + building.def.y
                        + " and is rejected."
                );
                return;
            }

            if (building.def.x == 10744 && building.def.y == 9409 && Rand.Next(100) < 31) {
                RandomizedBuildingBase rb = new RBKateAndBaldspot();
                rb.randomizeBuilding(building.def);
                return;
            }

            if (!forcedStory.isEmpty()) {
                for (int ixx = 0; ixx < forcedStory.size(); ixx++) {
                    RandomizedBuildingBase rb = forcedStory.get(ixx);
                    if (rb != null) {
                        rb.randomizeBuilding(building.def);
                    }
                }
            }

            if (SpawnPoints.instance.isSpawnBuilding(building.getDef())) {
                return;
            }

            RandomizedBuildingBase rb = IsoWorld.instance.getRBBasic();
            if ("Tutorial".equals(Core.gameMode)) {
                return;
            }

            try {
                int chance = 10;
                switch (SandboxOptions.instance.survivorHouseChance.getValue()) {
                    case 1:
                        return;
                    case 2:
                        chance -= 5;
                    case 3:
                    default:
                        break;
                    case 4:
                        chance += 5;
                        break;
                    case 5:
                        chance += 10;
                        break;
                    case 6:
                        chance += 20;
                }

                if (SandboxOptions.instance.survivorHouseChance.getValue() == 7 || Rand.Next(100) <= chance) {
                    if (totalChance == 0) {
                        initAllRBMapChance();
                    }

                    rb = getRandomStory();
                    if (rb == null) {
                        return;
                    }
                }

                if (rb.isValid(building.def, false) && rb.isTimeValid(false)) {
                    rb.randomizeBuilding(building.def);
                }
            } catch (Exception var7) {
                DebugType.General.printException(var7, LogSeverity.Error);
            }
        }
    }

    public int getChance() {
        return this.getChance(null);
    }

    public int getChance(IsoGridSquare sq) {
        if (Objects.equals(this.name, "Rat Infested House")) {
            int ratFactor = SandboxOptions.instance.getCurrentRatIndex() / 10;
            if (ratFactor < 0) {
                ratFactor = 1;
            }

            return ratFactor;
        } else {
            return Objects.equals(this.name, "Trashed Building") ? SandboxOptions.instance.getCurrentLootedChance(sq) : this.chance;
        }
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public boolean isAlwaysDo() {
        return this.alwaysDo;
    }

    public void setAlwaysDo(boolean alwaysDo) {
        this.alwaysDo = alwaysDo;
    }

    private static RandomizedBuildingBase getRandomStory() {
        int choice = Rand.Next(totalChance);
        Iterator<RandomizedBuildingBase> it = rbMap.keySet().iterator();
        int subTotal = 0;

        while (it.hasNext()) {
            RandomizedBuildingBase testTable = it.next();
            subTotal += rbMap.get(testTable);
            if (choice < subTotal) {
                return testTable;
            }
        }

        return null;
    }

    @Override
    public ArrayList<IsoZombie> addZombiesOnSquare(int totalZombies, String outfit, Integer femaleChance, IsoGridSquare square) {
        if (!IsoWorld.getZombiesDisabled() && !"Tutorial".equals(Core.gameMode)) {
            ArrayList<IsoZombie> result = new ArrayList<>();

            for (int j = 0; j < totalZombies; j++) {
                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(square);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
                if (zombie != null) {
                    if ("Kate".equals(outfit) || "Bob".equals(outfit) || "Raider".equals(outfit)) {
                        zombie.doDirtBloodEtc = false;
                    }

                    if (femaleChance != null) {
                        zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
                    }

                    if (outfit != null) {
                        zombie.dressInPersistentOutfit(outfit);
                        zombie.dressInRandomOutfit = false;
                    } else {
                        zombie.dressInRandomOutfit = true;
                    }

                    result.add(zombie);
                }
            }

            ZombieSpawnRecorder.instance.record(result, this.getClass().getSimpleName());
            return result;
        } else {
            return null;
        }
    }

    /**
     * If you specify a outfit, make sure it works for both gender! (or force
     *  femaleChance to 0 or 1 if it's gender-specific)
     * 
     * @param def buildingDef
     * @param totalZombies zombies to spawn (if 0 we gonna randomize it)
     * @param outfit force zombies spanwed in a specific outfit (not mandatory)
     * @param femaleChance force female zombies (if not set it'll be 50% chance, you can set
     *             it to 0 to exclude female from spawning, or 100 to force only
     *             female)
     * @param room force spawn zombies inside a certain room (not mandatory)
     */
    public ArrayList<IsoZombie> addZombies(BuildingDef def, int totalZombies, String outfit, Integer femaleChance, RoomDef room) {
        boolean randomizeRoom = room == null;
        ArrayList<IsoZombie> result = new ArrayList<>();
        if (!IsoWorld.getZombiesDisabled() && !"Tutorial".equals(Core.gameMode)) {
            if (room == null) {
                room = this.getRandomRoom(def, 6);
            }

            int min = 2;
            int max = room.area / 2;
            if (totalZombies == 0) {
                if (SandboxOptions.instance.zombies.getValue() == 1) {
                    max += 4;
                } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                    max += 3;
                } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                    max += 2;
                } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                    max -= 4;
                }

                if (max > 8) {
                    max = 8;
                }

                if (max < min) {
                    max = min + 1;
                }
            } else {
                min = totalZombies;
                max = totalZombies;
            }

            int rand = Rand.Next(min, max);

            for (int j = 0; j < rand; j++) {
                IsoGridSquare sq = getRandomSpawnSquare(room);
                if (sq == null) {
                    break;
                }

                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(sq);
                IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
                if (zombie != null) {
                    if (femaleChance != null) {
                        zombie.setFemaleEtc(Rand.Next(100) < femaleChance);
                    }

                    if (outfit != null) {
                        zombie.dressInPersistentOutfit(outfit);
                        zombie.dressInRandomOutfit = false;
                    } else {
                        zombie.dressInRandomOutfit = true;
                    }

                    result.add(zombie);
                    if (randomizeRoom) {
                        room = this.getRandomRoom(def, 6);
                    }
                }
            }

            ZombieSpawnRecorder.instance.record(result, this.getClass().getSimpleName());
            return result;
        } else {
            return result;
        }
    }

    public HandWeapon addRandomRangedWeapon(ItemContainer container, boolean addBulletsInGun, boolean addBoxInContainer, boolean attachPart) {
        if (weaponsList == null || weaponsList.isEmpty()) {
            this.init();
        }

        ArrayList<String> weapons = new ArrayList<>(weaponsList.keySet());
        String selectedWeapon = weapons.get(Rand.Next(0, weapons.size()));
        HandWeapon weapon = this.addWeapon(selectedWeapon, addBulletsInGun);
        if (weapon == null) {
            return null;
        } else {
            if (addBoxInContainer) {
                container.addItem(InventoryItemFactory.CreateItem(weaponsList.get(selectedWeapon)));
            }

            if (attachPart) {
                KahluaTable weaponDistrib = (KahluaTable)LuaManager.env.rawget("WeaponUpgrades");
                if (weaponDistrib == null) {
                    return null;
                }

                KahluaTable weaponUpgrade = (KahluaTable)weaponDistrib.rawget(weapon.getType());
                if (weaponUpgrade == null) {
                    return null;
                }

                int upgrades = Rand.Next(1, weaponUpgrade.len() + 1);

                for (int u = 1; u <= upgrades; u++) {
                    int r = Rand.Next(weaponUpgrade.len()) + 1;
                    WeaponPart part = InventoryItemFactory.CreateItem((String)weaponUpgrade.rawget(r));
                    if (part != null && !part.getScriptItem().obsolete) {
                        weapon.attachWeaponPart(part);
                    }
                }
            }

            return weapon;
        }
    }

    public void spawnItemsInContainers(BuildingDef def, String distribName, int chance) {
        ArrayList<ItemContainer> container = new ArrayList<>();
        ItemPickerJava.ItemPickerRoom contDistrib = ItemPickerJava.rooms.get(distribName);
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (Rand.Next(100) <= chance
                                && obj.getContainer() != null
                                && sq.getRoom() != null
                                && sq.getRoom().getName() != null
                                && contDistrib.containers.containsKey(obj.getContainer().getType())) {
                                obj.getContainer().clear();
                                container.add(obj.getContainer());
                                obj.getContainer().setExplored(true);
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < container.size(); i++) {
            ItemContainer cont = container.get(i);
            ItemPickerJava.fillContainerType(contDistrib, cont, "", null);
            ItemPickerJava.updateOverlaySprite(cont.getParent());
            if (GameServer.server) {
                GameServer.sendItemsInContainer(cont.getParent(), cont);
            }
        }
    }

    protected void removeAllZombies(BuildingDef def) {
        for (int x = def.x - 1; x < def.x + def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y + def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = getSq(x, y, z);
                    if (sq != null) {
                        for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                            sq.getMovingObjects().remove(i);
                            i--;
                        }
                    }
                }
            }
        }
    }

    public IsoWindow getWindow(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj instanceof IsoWindow isoWindow) {
                return isoWindow;
            }
        }

        return null;
    }

    public IsoDoor getDoor(IsoGridSquare sq) {
        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj instanceof IsoDoor isoDoor) {
                return isoDoor;
            }
        }

        return null;
    }

    public void addBarricade(IsoGridSquare sq, int numPlanks) {
        for (int o = 0; o < sq.getObjects().size(); o++) {
            IsoObject obj = sq.getObjects().get(o);
            if (obj instanceof IsoDoor isoDoor) {
                if (!isoDoor.isBarricadeAllowed()) {
                    continue;
                }

                IsoGridSquare outside = sq.getRoom() == null ? sq : isoDoor.getOppositeSquare();
                if (outside != null && outside.getRoom() == null) {
                    boolean addOpposite = outside != sq;
                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoDoor, addOpposite);
                    if (barricade != null) {
                        for (int b = 0; b < numPlanks; b++) {
                            barricade.addPlank(null, null);
                        }

                        if (GameServer.server) {
                            barricade.transmitCompleteItemToClients();
                        }
                    }
                }
            }

            if (obj instanceof IsoWindow isoWindow && isoWindow.isBarricadeAllowed()) {
                IsoGridSquare outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare();
                boolean addOpposite = outside != sq;
                IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoWindow, addOpposite);
                if (barricade != null) {
                    for (int b = 0; b < numPlanks; b++) {
                        barricade.addPlank(null, null);
                    }

                    if (GameServer.server) {
                        barricade.transmitCompleteItemToClients();
                    }
                }
            }
        }
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset) {
        return this.addWorldItem(item, sq, xoffset, yoffset, zoffset, 0);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset, boolean randomRotation) {
        return randomRotation
            ? this.addWorldItem(item, sq, xoffset, yoffset, zoffset, Rand.Next(360))
            : this.addWorldItem(item, sq, xoffset, yoffset, zoffset, 0);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, float xoffset, float yoffset, float zoffset, int worldZ) {
        if (item != null && sq != null) {
            if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item) == 0.0F) {
                return null;
            } else {
                InventoryItem invItem = InventoryItemFactory.CreateItem(item);
                if (invItem == null) {
                    return null;
                } else {
                    invItem.setAutoAge();
                    invItem.setWorldZRotation(worldZ);
                    if (invItem.hasTag(ItemTag.SPAWN_COOKED)) {
                        invItem.setCooked(true);
                        if (!StringUtils.isNullOrEmpty(((Food)invItem).getOnCooked())) {
                            Object functionObj = LuaManager.getFunctionObject(((Food)invItem).getOnCooked());
                            if (functionObj != null) {
                                LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, invItem);
                            }
                        }
                    }

                    if (invItem instanceof HandWeapon handWeapon && Rand.Next(100) < 90) {
                        handWeapon.randomizeFirearmAsLoot();
                    }

                    if (invItem.hasTag(ItemTag.SHOW_CONDITION) || invItem instanceof HandWeapon || invItem.hasSharpness()) {
                        invItem.randomizeGeneralCondition();
                    }

                    invItem.unsealIfNotFull();
                    if (invItem instanceof InventoryContainer inventoryContainer) {
                        inventoryContainer.getItemContainer().setExplored(true);
                    }

                    return ItemSpawner.spawnItem(invItem, sq, xoffset, yoffset, zoffset);
                }
            }
        } else {
            return null;
        }
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, IsoObject obj) {
        return this.addWorldItem(item, sq, obj, false);
    }

    public InventoryItem addWorldItem(String item, IsoGridSquare sq, IsoObject obj, boolean randomRotation) {
        if (item != null && sq != null && sq.hasAdjacentCanStandSquare()) {
            if (SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(item) == 0.0F) {
                return null;
            } else {
                float z = 0.0F;
                if (obj != null) {
                    z = obj.getSurfaceOffsetNoTable() / 96.0F;
                }

                InventoryItem invItem = InventoryItemFactory.CreateItem(item);
                if (invItem != null) {
                    invItem.setAutoAge();
                    if (randomRotation) {
                        invItem.randomizeWorldZRotation();
                    }

                    return ItemSpawner.spawnItem(invItem, sq, Rand.Next(0.3F, 0.9F), Rand.Next(0.3F, 0.9F), z);
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public boolean isTableFor3DItems(IsoObject obj, IsoGridSquare sq) {
        return sq.hasAdjacentCanStandSquare()
            && obj.getSurfaceOffsetNoTable() > 0.0F
            && obj.getContainer() == null
            && !sq.hasWater()
            && !obj.hasFluid()
            && obj.getProperties().get("BedType") == null;
    }

    public InventoryItem trySpawnStoryItem(String itemType, IsoGridSquare square, IsoObject obj) {
        return SandboxOptions.instance.removeStoryLoot.getValue() && ItemPickerJava.getLootModifier(itemType) == 0.0F
            ? null
            : this.addWorldItem("PlasticFork", square, obj);
    }

    public static ArrayList<IsoObject> getBuildingObjectsSimple(BuildingDef def) {
        List<IsoGridSquare> defSquares = def.getSquares();
        ArrayList<IsoObject> objList = new ArrayList<>();

        for (IsoGridSquare sq : defSquares) {
            for (int i = 0; i < sq.getObjects().size(); i++) {
                if (sq.getObjects().get(i) != null) {
                    objList.add(sq.getObjects().get(i));
                }
            }
        }

        return objList;
    }

    public static ArrayList<IsoObject> getBuildingObjects(BuildingDef def) {
        ArrayList<IsoObject> objList = new ArrayList<>();
        ArrayList<IsoGridSquare> buildingSquares = getBuildingSquares(def);

        for (int i = 0; i < buildingSquares.size(); i++) {
            IsoGridSquare sq = buildingSquares.get(i);

            for (int j = 0; j < sq.getObjects().size(); j++) {
                if (sq.getObjects().get(j) != null) {
                    objList.add(sq.getObjects().get(j));
                }
            }
        }

        return objList;
    }

    public static ArrayList<IsoGridSquare> getBuildingSquares(BuildingDef def) {
        ArrayList<RoomDef> rooms = def.getRooms();
        ArrayList<IsoGridSquare> buildingSquares = new ArrayList<>();

        for (int i = 0; i < rooms.size(); i++) {
            RoomDef room = rooms.get(i);
            ArrayList<RoomDef.RoomRect> rects = room.getRects();

            for (int j = 0; j < rects.size(); j++) {
                RoomDef.RoomRect rect = rects.get(j);
                ArrayList<IsoGridSquare> rectSquares = getRectSquares(rect, room);
                buildingSquares.addAll(rectSquares);
            }
        }

        return buildingSquares;
    }

    public static ArrayList<IsoGridSquare> getRectSquares(RoomDef.RoomRect rect, RoomDef room) {
        ArrayList<IsoGridSquare> squares = new ArrayList<>();

        for (int x = rect.getX(); x < rect.getX2(); x++) {
            for (int y = rect.getY(); y < rect.getY2(); y++) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, room.getZ());
                if (sq != null && sq.getRoom() != null && sq.getRoom().getName().contains("gunstore")) {
                    squares.add(sq);
                }
            }
        }

        return squares;
    }

    public static void setWorldRotation(InventoryItem item, float xRotation, float yRotation, float zRotation) {
        item.setWorldXRotation(xRotation);
        item.setWorldYRotation(yRotation);
        item.setWorldZRotation(zRotation);
        if (GameServer.server && item.getWorldItem() != null) {
            item.getWorldItem().transmitCompleteItemToClients();
        }
    }

    public static void addClip(HandWeapon gun) {
        if (gun.usesExternalMagazine() && !gun.isContainsClip()) {
            gun.setContainsClip(true);
        }
    }

    public static HandWeapon spawnPistol(ItemKey gunType) {
        HandWeapon gun = InventoryItemFactory.CreateItem(gunType);
        rollWeaponUpgrades(gun);
        addClip(gun);
        return gun;
    }

    public static HandWeapon spawnRifle(ItemKey gunType) {
        HandWeapon gun = InventoryItemFactory.CreateItem(gunType);
        rollWeaponUpgrades(gun);
        addClip(gun);
        return gun;
    }

    public static void doGunShelfRifles(boolean facingE, IsoGridSquare sq, WeightedList<ItemKey> rifleTypes, int spawnChance) {
        int slotNumber = 4;
        float xOffset = facingE ? 0.12F : 0.56F;
        float yOffset = facingE ? 0.56F : 0.12F;
        float zOffset = 0.48F;
        float xRotation = facingE ? 90.0F : 270.0F;
        float yRotation = facingE ? 90.0F : 0.0F;
        float zRotation = facingE ? 180.0F : 0.0F;

        for (int n = 0; n < 4; n++) {
            if (Rand.Next(100) < spawnChance) {
                HandWeapon gun = spawnRifle(rifleTypes.getRandom());
                sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset += 0.08F;
            }
        }
    }

    public static void doGunShelfHandguns(
        boolean facingE, IsoGridSquare sq, WeightedList<ItemKey> pistolTypes, WeightedList<ItemKey> rifleTypes, int spawnChancePistol, int spawnChanceRifle
    ) {
        int column1Slots = 4;
        float xOffset = facingE ? 0.12F : 0.2F;
        float yOffset = facingE ? 0.92F : 0.12F;
        float zOffset = 0.48F;
        float xRotation = facingE ? 90.0F : 270.0F;
        float yRotation = facingE ? 90.0F : 0.0F;
        float zRotation = facingE ? 180.0F : 0.0F;

        for (int n = 0; n < 4; n++) {
            if (Rand.Next(100) < spawnChancePistol) {
                HandWeapon gun = spawnPistol(pistolTypes.getRandom());
                sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset += 0.08F;
            }
        }

        int column2Slots = 4;
        float xOffset2 = facingE ? 0.12F : 0.46F;
        float yOffset2 = facingE ? 0.64F : 0.12F;
        float zOffset2 = 0.48F;

        for (int nx = 0; nx < 4; nx++) {
            if (Rand.Next(100) < spawnChancePistol) {
                HandWeapon gun = spawnPistol(pistolTypes.getRandom());
                sq.AddWorldInventoryItem(gun, xOffset2, yOffset2, zOffset2, false, true);
                setWorldRotation(gun, xRotation, yRotation, zRotation);
                zOffset2 += 0.08F;
            }
        }

        int rifleSlots = 2;
        float xOffset3 = facingE ? 0.12F : 0.7F;
        float yOffset3 = facingE ? 0.2F : 0.12F;
        float zOffset3 = 0.6F;
        float xRotation3 = 0.0F;
        float yRotation3 = 90.0F;
        float zRotation3 = facingE ? 180.0F : 270.0F;

        for (int nxx = 0; nxx < 2; nxx++) {
            if (Rand.Next(100) < spawnChanceRifle) {
                HandWeapon gun = spawnRifle(rifleTypes.getRandom());
                sq.AddWorldInventoryItem(gun, xOffset3, yOffset3, 0.6F, false, true);
                setWorldRotation(gun, 0.0F, 90.0F, zRotation3);
                yOffset3 += facingE ? 0.18F : 0.0F;
                xOffset3 += !facingE ? 0.18F : 0.0F;
            }
        }
    }

    public static void doAmmoCans(PropertyContainer props, boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq, WeightedList<ItemKey> ammoCans) {
        if (!props.has(IsoPropertyType.GROUP_NAME) || props.get(IsoPropertyType.GROUP_NAME).contains("Green")) {
            int ammoSlots = Rand.NextInclusive(1, 6);
            float xOffset = facingE ? 0.34F : (facingW ? 0.82F : 0.2F);
            float yOffset = facingE ? 0.2F : (facingN ? 0.82F : (facingW ? 0.2F : 0.34F));
            float zOffset = 0.52F;
            float xRotation = 0.0F;
            float yRotation = 0.0F;
            float zRotation = facingE ? 90.0F : (facingN ? 0.0F : (facingW ? 270.0F : 180.0F));

            for (int j = 0; j < ammoSlots; j++) {
                InventoryContainer ammoCan = InventoryItemFactory.CreateItem(ammoCans.getRandom());
                sq.AddWorldInventoryItem(ammoCan, xOffset, yOffset, 0.52F, false, true);
                ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
                setWorldRotation(ammoCan, 0.0F, 0.0F, zRotation);
                if (!facingE && !facingW) {
                    xOffset += 0.14F;
                } else {
                    yOffset += 0.14F;
                }
            }
        }
    }

    public static void doHandgunCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq, WeightedList<ItemKey> pistolTypes) {
        float xOffset = facingE ? 0.8F : (facingW ? 0.42F : (facingN ? 0.82F : 0.38F));
        float yOffset = facingE ? 0.8F : (facingW ? 0.4F : (facingN ? 0.44F : 0.8F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));
        HandWeapon gun = spawnPistol(pistolTypes.getRandom());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38F, false, true);
        setWorldRotation(gun, 0.0F, 0.0F, zRotation);
        if (!StringUtils.isNullOrEmpty(gun.getMagazineType())) {
            float xOffset2 = facingE ? 0.88F : (facingW ? 0.32F : 0.58F);
            float yOffset2 = facingE ? 0.62F : (facingW ? 0.58F : (facingN ? 0.34F : 0.84F));
            float zRotation2 = facingE ? 180.0F : (facingW ? 0.0F : (facingN ? 90.0F : 270.0F));
            int clipSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < clipSlots; j++) {
                InventoryItem clip = InventoryItemFactory.CreateItem(gun.getMagazineType());
                sq.AddWorldInventoryItem(clip, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(clip, 0.0F, 0.0F, zRotation2);
                yOffset2 -= facingE ? 0.06F : 0.0F;
                yOffset2 += facingW ? 0.06F : 0.0F;
                xOffset2 -= facingN ? 0.06F : 0.0F;
                xOffset2 += !facingE && !facingW && !facingN ? 0.06F : 0.0F;
            }
        }

        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float xOffset2 = facingE ? 0.76F : (facingW ? 0.48F : (facingN ? 0.32F : 0.84F));
            float yOffset2 = facingE ? 0.38F : (facingW ? 0.84F : (facingN ? 0.48F : 0.78F));
            float zRotation2 = facingE ? 270.0F : 0.0F;
            int boxSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < boxSlots; j++) {
                InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, zRotation2);
                xOffset2 += facingE ? 0.12F : 0.0F;
                xOffset2 -= facingW ? 0.12F : 0.0F;
                yOffset2 -= facingN ? 0.12F : 0.0F;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12F : 0.0F;
            }
        }
    }

    public static void doRifleCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq, WeightedList<ItemKey> rifleTypes) {
        float xOffset = facingE ? 0.82F : (facingW ? 0.42F : 0.6F);
        float yOffset = facingE ? 0.58F : (facingW ? 0.62F : (facingN ? 0.42F : 0.86F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float standardZRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));
        HandWeapon gun = spawnRifle(rifleTypes.getRandom());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38F, false, true);
        setWorldRotation(gun, 0.0F, 0.0F, standardZRotation);
        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float xOffset2 = facingE ? 0.66F : (facingW ? 0.58F : (facingN ? 0.88F : 0.32F));
            float yOffset2 = facingE ? 0.88F : (facingW ? 0.34F : (facingN ? 0.6F : 0.68F));
            int boxSlots = Rand.NextInclusive(1, 2);

            for (int j = 0; j < boxSlots; j++) {
                InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, standardZRotation);
                yOffset2 -= facingE ? 0.22F : 0.0F;
                yOffset2 += facingW ? 0.22F : 0.0F;
                xOffset2 -= facingN ? 0.22F : 0.0F;
                xOffset2 += !facingE && !facingW && !facingN ? 0.22F : 0.0F;
            }
        }
    }

    public static void doCounterAmmoDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq, WeightedList<ItemKey> ammoBoxes) {
        int columns = 3;
        float xOffset = facingE ? 0.2F : (facingW ? 0.94F : 0.24F);
        float yOffset = facingE ? 0.84F : (facingW ? 0.84F : (facingN ? 0.94F : 0.24F));
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingE ? 270.0F : (facingW ? 90.0F : (facingN ? 180.0F : 0.0F));

        for (int j = 0; j < 3; j++) {
            float xOffset2 = 0.0F;
            float yOffset2 = 0.0F;

            for (int k = 0; k < Rand.NextInclusive(1, 4); k++) {
                if (xOffset2 == 0.0F || yOffset2 == 0.0F) {
                    xOffset2 = xOffset;
                    yOffset2 = yOffset;
                }

                InventoryItem ammoBox = InventoryItemFactory.CreateItem(ammoBoxes.getRandom());
                sq.AddWorldInventoryItem(ammoBox, xOffset2, yOffset2, 0.38F, false, true);
                setWorldRotation(ammoBox, 0.0F, 0.0F, zRotation);
                xOffset2 += facingE ? 0.12F : 0.0F;
                xOffset2 -= facingW ? 0.12F : 0.0F;
                yOffset2 -= facingN ? 0.12F : 0.0F;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12F : 0.0F;
            }

            if (!facingE && !facingW) {
                xOffset += 0.28F;
            } else {
                yOffset -= 0.28F;
            }
        }
    }

    public static void doCornerAmmoCans(
        boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq, WeightedList<ItemKey> ammoCases, WeightedList<ItemKey> ammoCans
    ) {
        int caseSlots = 2;
        float xOffset = facingW ? 0.44F : (facingN ? 0.44F : 0.38F);
        float yOffset = facingE ? 0.82F : (facingN ? 0.82F : 0.34F);
        float zOffset = 0.38F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = 0.0F;

        for (int j = 0; j < 2; j++) {
            InventoryContainer ammoCase = InventoryItemFactory.CreateItem(ammoCases.getRandom());
            sq.AddWorldInventoryItem(ammoCase, xOffset, yOffset, 0.38F, false, true);
            ItemPickerJava.rollContainerItem(ammoCase, null, ItemPickerJava.getItemPickerContainers().get(ammoCase.getType()));
            setWorldRotation(ammoCase, 0.0F, 0.0F, 0.0F);
            xOffset += 0.38F;
        }

        int ammoSlots = 2;
        float xOffset2 = facingW ? 0.82F : (facingN ? 0.82F : 0.38F);
        float yOffset2 = facingE ? 0.52F : (facingN ? 0.52F : 0.64F);
        float zOffset2 = 0.38F;
        float xRotation2 = 0.0F;
        float yRotation2 = 0.0F;
        float zRotation2 = facingW ? 270.0F : (facingN ? 270.0F : 90.0F);

        for (int j = 0; j < 2; j++) {
            InventoryContainer ammoCan = InventoryItemFactory.CreateItem(ammoCans.getRandom());
            sq.AddWorldInventoryItem(ammoCan, xOffset2, yOffset2, 0.38F, false, true);
            ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
            setWorldRotation(ammoCan, 0.0F, 0.0F, zRotation2);
            if (!facingE && !facingN) {
                yOffset2 += 0.14F;
            } else {
                yOffset2 -= 0.14F;
            }
        }
    }

    public static void doBodyArmor(boolean facingE, IsoGridSquare sq, ItemKey vestType, int spawnChance) {
        int slotNumber = 4;
        int currentSlot = 1;
        float xOffset = facingE ? 0.12F : 0.28F;
        float yOffset = facingE ? 0.78F : 0.12F;
        float zOffset = 0.5F;
        float xRotation = 270.0F;
        float yRotation = facingE ? 270.0F : 0.0F;
        float zRotation = 0.0F;

        for (int j = 0; j < 4; j++) {
            InventoryItem vest = InventoryItemFactory.CreateItem(vestType);
            if (currentSlot == 1) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset, spawnChance);
                zOffset += 0.2F;
            } else if (currentSlot == 2) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset, spawnChance);
                zOffset -= 0.2F;
                if (facingE) {
                    yOffset -= 0.44F;
                } else {
                    xOffset += 0.44F;
                }
            } else if (currentSlot == 3) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset, spawnChance);
                zOffset += 0.2F;
            } else if (currentSlot == 4) {
                spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset, spawnChance);
            }

            if (vest.getWorldItem() != null) {
                setWorldRotation(vest, 270.0F, yRotation, 0.0F);
            }

            currentSlot++;
        }
    }

    public static void spawnBodyArmor(InventoryItem vest, IsoGridSquare sq, float xOffset, float yOffset, float zOffset, int spawnChance) {
        if (Rand.Next(100) < spawnChance) {
            sq.AddWorldInventoryItem(vest, xOffset, yOffset, zOffset, false, true);
        }
    }

    private static void rollWeaponUpgrades(HandWeapon gun) {
        if (gun.is(ItemKey.Weapon.VARMINT_RIFLE)) {
            WeaponPart scope = InventoryItemFactory.CreateItem(ItemKey.WeaponPart.X2_SCOPE);
            gun.attachWeaponPart(scope);
        }

        if (gun.is(ItemKey.Weapon.HUNTING_RIFLE) || gun.is(ItemKey.Weapon.MSR7T_RIFLE)) {
            ItemKey scopeType = ItemKey.WeaponPart.X4_SCOPE;
            if (Rand.Next(100) >= 60) {
                scopeType = ItemKey.WeaponPart.X8_SCOPE;
            }

            WeaponPart scope = InventoryItemFactory.CreateItem(scopeType);
            gun.attachWeaponPart(scope);
        }

        if (gun.is(ItemKey.Weapon.REVOLVER_LONG) && Rand.NextBool(4)) {
            gun.attachWeaponPart(InventoryItemFactory.CreateItem(ItemKey.WeaponPart.X2_SCOPE));
        }
    }

    public static final class HumanCorpse extends IsoGameCharacter implements IHumanVisual {
        final HumanVisual humanVisual = new HumanVisual(this);
        final ItemVisuals itemVisuals = new ItemVisuals();
        public boolean isSkeleton;

        public HumanCorpse(IsoCell cell, float x, float y, float z) {
            super(cell, x, y, z);
            cell.getObjectList().remove(this);
            cell.getAddList().remove(this);
        }

        @Override
        public void dressInNamedOutfit(String outfitName) {
            this.getHumanVisual().dressInNamedOutfit(outfitName, this.itemVisuals);
            this.getHumanVisual().synchWithOutfit(this.getHumanVisual().getOutfit());
        }

        @Override
        public HumanVisual getHumanVisual() {
            return this.humanVisual;
        }

        public HumanVisual getVisual() {
            return this.humanVisual;
        }

        @Override
        public void Dressup(SurvivorDesc desc) {
            this.wornItems.setFromItemVisuals(this.itemVisuals);
            this.wornItems.addItemsToItemContainer(this.inventory);
        }

        @Override
        public boolean isSkeleton() {
            return this.isSkeleton;
        }
    }
}
