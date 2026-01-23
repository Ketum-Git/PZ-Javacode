// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.stash;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.Lua.LuaManager;
import zombie.characters.IsoZombie;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.MapItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;
import zombie.worldMap.symbols.WorldMapBaseSymbol;

@UsedFromLua
public final class StashSystem {
    public static ArrayList<Stash> allStashes;
    public static ArrayList<StashBuilding> possibleStashes;
    public static ArrayList<StashBuilding> buildingsToDo;
    private static final ArrayList<String> possibleTrap = new ArrayList<>();
    private static ArrayList<String> alreadyReadMap = new ArrayList<>();

    public static void init() {
        if (possibleStashes == null) {
            initAllStashes();
            buildingsToDo = new ArrayList<>();
            possibleTrap.add("Base.FlameTrapSensorV1");
            possibleTrap.add("Base.SmokeBombSensorV1");
            possibleTrap.add("Base.NoiseTrapSensorV1");
            possibleTrap.add("Base.NoiseTrapSensorV2");
            possibleTrap.add("Base.AerosolbombSensorV1");
        }
    }

    /**
     * Load our different stashes description from lua files in "media/lua/shared/StashDescriptions"
     */
    public static void initAllStashes() {
        allStashes = new ArrayList<>();
        possibleStashes = new ArrayList<>();
        KahluaTable stashTable = (KahluaTable)LuaManager.env.rawget("StashDescriptions");
        KahluaTableIterator it = stashTable.iterator();

        while (it.advance()) {
            KahluaTableImpl stashDesc = (KahluaTableImpl)it.getValue();
            Stash stashObj = new Stash(stashDesc.rawgetStr("name"));
            stashObj.load(stashDesc);
            allStashes.add(stashObj);
        }
    }

    public static ArrayList<Stash> getAllStashes() {
        return allStashes;
    }

    public static ArrayList<String> getAlreadyReadMap() {
        return alreadyReadMap;
    }

    /**
     * check if the spawned item could be a stash item (map or note...)
     */
    public static void checkStashItem(InventoryItem item) {
        if (!GameClient.client && !possibleStashes.isEmpty()) {
            int chance = 60;
            if (item.getStashChance() > 0) {
                chance = item.getStashChance();
            }

            switch (SandboxOptions.instance.annotatedMapChance.getValue()) {
                case 1:
                    return;
                case 2:
                    chance += 15;
                    break;
                case 3:
                    chance += 10;
                case 4:
                default:
                    break;
                case 5:
                    chance -= 10;
                    break;
                case 6:
                    chance -= 20;
            }

            if (Rand.Next(100) <= 100 - chance) {
                ArrayList<Stash> correctStashes = new ArrayList<>();

                for (int i = 0; i < allStashes.size(); i++) {
                    Stash stash = allStashes.get(i);
                    if (stash.item.equals(item.getFullType()) && checkSpecificSpawnProperties(stash, item)) {
                        boolean isPossible = false;

                        for (int j = 0; j < possibleStashes.size(); j++) {
                            StashBuilding stashBuilding = possibleStashes.get(j);
                            if (stashBuilding != null
                                && IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0) != null
                                && Objects.requireNonNull(IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0)).getBuilding() != null
                                )
                             {
                                BuildingDef def = Objects.requireNonNull(IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0))
                                    .getBuilding();
                                if (def != null) {
                                    boolean explored = def.isHasBeenVisited();
                                    if (stashBuilding.stashName.equals(stash.name) && !explored) {
                                        isPossible = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (isPossible) {
                            correctStashes.add(stash);
                        }
                    }
                }

                if (!correctStashes.isEmpty()) {
                    Stash stash = correctStashes.get(Rand.Next(0, correctStashes.size()));
                    doStashItem(stash, item);
                }
            }
        }
    }

    /**
     * Public for lua debug stash map
     */
    public static void doStashItem(Stash stash, InventoryItem item) {
        if (stash.customName != null) {
            item.setName(stash.customName);
        }

        if ("Map".equals(stash.type)) {
            if (!(item instanceof MapItem mapItem)) {
                throw new IllegalArgumentException(item + " is not a MapItem");
            }

            if (stash.annotations != null) {
                for (int i = 0; i < stash.annotations.size(); i++) {
                    StashAnnotation annotation = stash.annotations.get(i);
                    WorldMapBaseSymbol symbol;
                    if (annotation.symbol != null) {
                        symbol = mapItem.getSymbols()
                            .addTexture(annotation.symbol, annotation.x, annotation.y, 0.5F, 0.5F, 0.666F, annotation.r, annotation.g, annotation.b, 1.0F);
                    } else if (annotation.text != null && Translator.getTextOrNull(annotation.text) != null) {
                        symbol = mapItem.getSymbols()
                            .addUntranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0F);
                    } else {
                        if (annotation.text == null) {
                            continue;
                        }

                        symbol = mapItem.getSymbols()
                            .addTranslatedText(annotation.text, "note", annotation.x, annotation.y, annotation.r, annotation.g, annotation.b, 1.0F);
                    }

                    if (!Float.isNaN(annotation.anchorX) && !Float.isNaN(annotation.anchorY)) {
                        symbol.setAnchor(annotation.anchorX, annotation.anchorY);
                    }

                    if (!Float.isNaN(annotation.rotation)) {
                        symbol.setRotation(annotation.rotation);
                    }
                }
            }

            removeFromPossibleStash(stash);
            item.setStashMap(stash.name);
        }
    }

    /**
     * Used when you read an annoted map
     */
    public static void prepareBuildingStash(String stashName) {
        if (stashName != null) {
            Stash stash = getStash(stashName);
            if (stash != null && !alreadyReadMap.contains(stashName)) {
                alreadyReadMap.add(stashName);
                buildingsToDo.add(new StashBuilding(stash.name, stash.buildingX, stash.buildingY));
                RoomDef roomDef = IsoWorld.instance.getMetaGrid().getRoomAt(stash.buildingX, stash.buildingY, 0);
                if (roomDef != null && roomDef.getBuilding() != null && roomDef.getBuilding().isFullyStreamedIn()) {
                    doBuildingStash(roomDef.getBuilding());
                }
            }
        }
    }

    private static boolean checkSpecificSpawnProperties(Stash stash, InventoryItem item) {
        return !stash.spawnOnlyOnZed || item.getContainer() != null && item.getContainer().getParent() instanceof IsoDeadBody
            ? (stash.minDayToSpawn <= -1 || GameTime.instance.getDaysSurvived() >= stash.minDayToSpawn)
                && (stash.maxDayToSpawn <= -1 || GameTime.instance.getDaysSurvived() <= stash.maxDayToSpawn)
            : false;
    }

    private static void removeFromPossibleStash(Stash stash) {
        for (int i = 0; i < possibleStashes.size(); i++) {
            StashBuilding possibleStash = possibleStashes.get(i);
            if (possibleStash.buildingX == stash.buildingX && possibleStash.buildingY == stash.buildingY) {
                possibleStashes.remove(i);
                i--;
            }
        }
    }

    /**
     * Fetch our list of building in which we'll spawn stash, if this building correspond, we do the necessary stuff
     */
    public static void doBuildingStash(BuildingDef def) {
        if (buildingsToDo == null) {
            init();
        }

        for (int i = 0; i < buildingsToDo.size(); i++) {
            StashBuilding stashBuilding = buildingsToDo.get(i);
            if (stashBuilding.buildingX > def.x && stashBuilding.buildingX < def.x2 && stashBuilding.buildingY > def.y && stashBuilding.buildingY < def.y2) {
                if (def.hasBeenVisited) {
                    buildingsToDo.remove(i);
                    i--;
                } else {
                    Stash stash = getStash(stashBuilding.stashName);
                    if (stash != null) {
                        ItemPickerJava.ItemPickerRoom buildingLoot = ItemPickerJava.rooms.get(stash.spawnTable);
                        if (buildingLoot != null) {
                            def.setAllExplored(true);
                            doSpecificBuildingProperties(stash, def);

                            for (int x = def.x - 1; x < def.x2 + 1; x++) {
                                for (int y = def.y - 1; y < def.y2 + 1; y++) {
                                    for (int z = -32; z < 31; z++) {
                                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                                        if (sq != null) {
                                            for (int o = 0; o < sq.getObjects().size(); o++) {
                                                IsoObject obj = sq.getObjects().get(o);
                                                if (obj.getContainer() != null
                                                    && sq.getRoom() != null
                                                    && sq.getRoom().getBuilding().getDef() == def
                                                    && sq.getRoom().getName() != null
                                                    && buildingLoot.containers.containsKey(obj.getContainer().getType())) {
                                                    ItemPickerJava.ItemPickerRoom originalLoot = ItemPickerJava.rooms.get(sq.getRoom().getName());
                                                    boolean clearIt = false;
                                                    if (originalLoot == null || !originalLoot.containers.containsKey(obj.getContainer().getType())) {
                                                        obj.getContainer().clear();
                                                        clearIt = true;
                                                    }

                                                    ItemPickerJava.fillContainerType(buildingLoot, obj.getContainer(), "", null);
                                                    ItemPickerJava.updateOverlaySprite(obj);
                                                    if (clearIt) {
                                                        obj.getContainer().setExplored(true);
                                                    }
                                                }

                                                BarricadeAble barricadeAble = Type.tryCastTo(obj, BarricadeAble.class);
                                                if (stash.barricades > -1
                                                    && barricadeAble != null
                                                    && barricadeAble.isBarricadeAllowed()
                                                    && Rand.Next(100) < stash.barricades) {
                                                    if (obj instanceof IsoDoor isoDoor) {
                                                        isoDoor.addRandomBarricades();
                                                    } else if (obj instanceof IsoWindow isoWindow) {
                                                        isoWindow.addRandomBarricades();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            buildingsToDo.remove(i);
                            i--;
                        }
                    }
                }
            }
        }
    }

    private static void doSpecificBuildingProperties(Stash stash, BuildingDef def) {
        if (stash.containers != null) {
            ArrayList<RoomDef> possibleRooms = new ArrayList<>();

            for (int i = 0; i < stash.containers.size(); i++) {
                StashContainer stashCont = stash.containers.get(i);
                IsoGridSquare sq = null;
                if (!"all".equals(stashCont.room)) {
                    for (int j = 0; j < def.rooms.size(); j++) {
                        RoomDef room = def.rooms.get(j);
                        if (stashCont.room.equals(room.name)) {
                            possibleRooms.add(room);
                        }
                    }
                } else if (stashCont.contX > -1 && stashCont.contY > -1 && stashCont.contZ > -1) {
                    sq = IsoWorld.instance.getCell().getGridSquare(stashCont.contX, stashCont.contY, stashCont.contZ);
                } else {
                    sq = def.getFreeSquareInRoom();
                }

                if (!possibleRooms.isEmpty()) {
                    RoomDef room = possibleRooms.get(Rand.Next(0, possibleRooms.size()));
                    sq = room.getFreeSquare();
                }

                if (sq != null) {
                    if (stashCont.containerItem != null && !stashCont.containerItem.isEmpty()) {
                        ItemPickerJava.ItemPickerRoom spawnTable = ItemPickerJava.rooms.get(stash.spawnTable);
                        if (spawnTable == null) {
                            DebugLog.log("Container distribution " + stash.spawnTable + " not found");
                            return;
                        }

                        InventoryItem item = InventoryItemFactory.CreateItem(stashCont.containerItem);
                        if (item == null) {
                            DebugLog.General.error("Item " + stashCont.containerItem + " Doesn't exist.");
                            return;
                        }

                        ItemPickerJava.ItemPickerContainer containerDist = spawnTable.containers.get(item.getType());
                        if (containerDist == null) {
                            DebugLog.General.error("ContainerDist " + item.getType() + " Doesn't exist. (" + stash.spawnTable + ")");
                            return;
                        }

                        ItemPickerJava.rollContainerItem((InventoryContainer)item, null, containerDist);
                        ItemSpawner.spawnItem(item, sq, 0.0F, 0.0F, 0.0F);
                    } else {
                        IsoThumpable cont = new IsoThumpable(sq.getCell(), sq, stashCont.containerSprite, false, null);
                        cont.setIsThumpable(false);
                        cont.container = new ItemContainer(stashCont.containerType, sq, cont);
                        sq.AddSpecialObject(cont);
                        sq.RecalcAllWithNeighbours(true);
                    }
                } else {
                    DebugLog.log("No free room was found to spawn special container for stash " + stash.name);
                }
            }
        }

        if (stash.minTrapToSpawn > -1) {
            for (int i = stash.minTrapToSpawn; i < stash.maxTrapToSpawn; i++) {
                IsoGridSquare sqx = def.getFreeSquareInRoom();
                if (sqx != null) {
                    HandWeapon trap = InventoryItemFactory.CreateItem(possibleTrap.get(Rand.Next(0, possibleTrap.size())));
                    IsoTrap isotrap = new IsoTrap(trap, sqx.getCell(), sqx);
                    sqx.AddTileObject(isotrap);
                    if (GameServer.server) {
                        isotrap.transmitCompleteItemToClients();
                    }
                }
            }
        }

        if (stash.zombies > -1) {
            for (int ix = 0; ix < def.rooms.size(); ix++) {
                RoomDef room = def.rooms.get(ix);
                if (IsoWorld.getZombiesEnabled()) {
                    int min = 1;
                    int zedInRoom = 0;

                    for (int jx = 0; jx < room.area; jx++) {
                        if (Rand.Next(100) < stash.zombies) {
                            zedInRoom++;
                        }
                    }

                    if (SandboxOptions.instance.zombies.getValue() == 1) {
                        zedInRoom += 4;
                    } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                        zedInRoom += 3;
                    } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                        zedInRoom += 2;
                    } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                        zedInRoom -= 4;
                    }

                    if (zedInRoom > room.area / 2) {
                        zedInRoom = room.area / 2;
                    }

                    if (zedInRoom < 1) {
                        zedInRoom = 1;
                    }

                    ArrayList<IsoZombie> zombies = VirtualZombieManager.instance.addZombiesToMap(zedInRoom, room, false);
                    ZombieSpawnRecorder.instance.record(zombies, "StashSystem");
                }
            }
        }
    }

    public static Stash getStash(String stashName) {
        for (int i = 0; i < allStashes.size(); i++) {
            Stash stash = allStashes.get(i);
            if (stash.name.equals(stashName)) {
                return stash;
            }
        }

        return null;
    }

    /**
     * Check if the visited building is in one of our random stash, in that case we won't spawn any stash for this building
     */
    public static void visitedBuilding(BuildingDef def) {
        if (!GameClient.client) {
            for (int i = 0; i < possibleStashes.size(); i++) {
                StashBuilding stash = possibleStashes.get(i);
                if (stash.buildingX > def.x && stash.buildingX < def.x2 && stash.buildingY > def.y && stash.buildingY < def.y2) {
                    possibleStashes.remove(i);
                    i--;
                }
            }
        }
    }

    public static void load(ByteBuffer input, int WorldVersion) {
        init();
        alreadyReadMap = new ArrayList<>();
        possibleStashes = new ArrayList<>();
        buildingsToDo = new ArrayList<>();
        int nPossibleStash = input.getInt();

        for (int i = 0; i < nPossibleStash; i++) {
            possibleStashes.add(new StashBuilding(GameWindow.ReadString(input), input.getInt(), input.getInt()));
        }

        int nBuildingsToDo = input.getInt();

        for (int i = 0; i < nBuildingsToDo; i++) {
            buildingsToDo.add(new StashBuilding(GameWindow.ReadString(input), input.getInt(), input.getInt()));
        }

        int nAlreadyReadMap = input.getInt();

        for (int i = 0; i < nAlreadyReadMap; i++) {
            alreadyReadMap.add(GameWindow.ReadString(input));
        }
    }

    public static void save(ByteBuffer output) {
        if (allStashes != null) {
            output.putInt(possibleStashes.size());

            for (int i = 0; i < possibleStashes.size(); i++) {
                StashBuilding stashBuilding = possibleStashes.get(i);
                GameWindow.WriteString(output, stashBuilding.stashName);
                output.putInt(stashBuilding.buildingX);
                output.putInt(stashBuilding.buildingY);
            }

            output.putInt(buildingsToDo.size());

            for (int i = 0; i < buildingsToDo.size(); i++) {
                StashBuilding stashBuilding = buildingsToDo.get(i);
                GameWindow.WriteString(output, stashBuilding.stashName);
                output.putInt(stashBuilding.buildingX);
                output.putInt(stashBuilding.buildingY);
            }

            output.putInt(alreadyReadMap.size());

            for (int i = 0; i < alreadyReadMap.size(); i++) {
                GameWindow.WriteString(output, alreadyReadMap.get(i));
            }
        }
    }

    public static ArrayList<StashBuilding> getPossibleStashes() {
        return possibleStashes;
    }

    public static void reinit() {
        possibleStashes = null;
        alreadyReadMap = new ArrayList<>();
        init();
    }

    public static void Reset() {
        allStashes = null;
        possibleStashes = null;
        buildingsToDo = null;
        possibleTrap.clear();
        alreadyReadMap.clear();
    }

    public static boolean isStashBuilding(BuildingDef def) {
        if (possibleStashes != null) {
            for (int i = 0; i < possibleStashes.size(); i++) {
                StashBuilding stashBuilding = possibleStashes.get(i);
                if (stashBuilding.buildingX > def.x && stashBuilding.buildingX < def.x2 && stashBuilding.buildingY > def.y && stashBuilding.buildingY < def.y2) {
                    return true;
                }
            }
        }

        if (buildingsToDo != null) {
            for (int ix = 0; ix < buildingsToDo.size(); ix++) {
                StashBuilding stashBuilding = buildingsToDo.get(ix);
                if (stashBuilding.buildingX > def.x && stashBuilding.buildingX < def.x2 && stashBuilding.buildingY > def.y && stashBuilding.buildingY < def.y2) {
                    return true;
                }
            }
        }

        return false;
    }
}
