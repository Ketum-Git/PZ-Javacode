// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import generation.ItemGenerationConstants;
import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.MapItem;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.ContainerOverlays;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.radio.media.RecordedMedia;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.logic.RecipeCodeHelper;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.Newspaper;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class ItemPickerJava {
    private static IsoPlayer player;
    private static float otherLootModifier;
    private static float foodLootModifier;
    private static float cannedFoodLootModifier;
    private static float weaponLootModifier;
    private static float rangedWeaponLootModifier;
    private static float ammoLootModifier;
    private static float literatureLootModifier;
    private static float survivalGearsLootModifier;
    private static float medicalLootModifier;
    private static float bagLootModifier;
    private static float mechanicsLootModifier;
    private static float clothingLootModifier;
    private static float containerLootModifier;
    private static float keyLootModifier;
    private static float keyLootModifierD100;
    private static float mediaLootModifier;
    private static float mementoLootModifier;
    private static float cookwareLootModifier;
    private static float materialLootModifier;
    private static float farmingLootModifier;
    private static float toolLootModifier;
    private static final String OtherLootType = "Other";
    private static final String FoodLootType = "Food";
    private static final String CannedFoodLootType = "CannedFood";
    private static final String WeaponLootType = "Weapon";
    private static final String RangedWeaponLootType = "RangedWeapon";
    private static final String AmmoLootType = "Ammo";
    private static final String LiteratureLootType = "Literature";
    private static final String SurvivalGearsLootType = "SurvivalGears";
    private static final String MedicalLootType = "Medical";
    private static final String MechanicsLootType = "Mechanics";
    private static final String ClothingLootType = "Clothing";
    private static final String ContainerLootType = "Container";
    private static final String KeyLootType = "Key";
    private static final String MediaLootType = "Media";
    private static final String MementoLootType = "Memento";
    private static final String CookwareLootType = "Cookware";
    private static final String MaterialLootType = "Material";
    private static final String FarmingLootType = "Farming";
    private static final String ToolLootType = "Tool";
    private static final String GeneratorLootType = "Generator";
    public static float zombieDensityCap = 8.0F;
    public static final ArrayList<String> NoContainerFillRooms = new ArrayList<>();
    public static final ArrayList<ItemPickerJava.ItemPickerUpgradeWeapons> WeaponUpgrades = new ArrayList<>();
    public static final HashMap<String, ItemPickerJava.ItemPickerUpgradeWeapons> WeaponUpgradeMap = new HashMap<>();
    public static final THashMap<String, ItemPickerJava.ItemPickerRoom> rooms = new THashMap<>();
    public static final THashMap<String, ItemPickerJava.ItemPickerContainer> containers = new THashMap<>();
    public static final THashMap<String, ItemPickerJava.ItemPickerContainer> ProceduralDistributions = new THashMap<>();
    public static final THashMap<String, ItemPickerJava.VehicleDistribution> VehicleDistributions = new THashMap<>();
    private static final ArrayList<String> addedInvalidAlready = new ArrayList<>();

    public static THashMap<String, ItemPickerJava.ItemPickerContainer> getItemPickerContainers() {
        return containers;
    }

    public static void Parse() {
        rooms.clear();
        ItemPickerJava.NoContainerFillRooms.clear();
        WeaponUpgradeMap.clear();
        ItemPickerJava.WeaponUpgrades.clear();
        containers.clear();
        addedInvalidAlready.clear();
        KahluaTableImpl NoContainerFillRooms = (KahluaTableImpl)LuaManager.env.rawget("NoContainerFillRooms");

        for (Entry<Object, Object> objectObjectEntry : NoContainerFillRooms.delegate.entrySet()) {
            String room = objectObjectEntry.getKey().toString();
            ItemPickerJava.NoContainerFillRooms.add(room);
        }

        KahluaTableImpl WeaponUpgrades = (KahluaTableImpl)LuaManager.env.rawget("WeaponUpgrades");

        for (Entry<Object, Object> objectObjectEntry : WeaponUpgrades.delegate.entrySet()) {
            String type = objectObjectEntry.getKey().toString();
            ItemPickerJava.ItemPickerUpgradeWeapons w = new ItemPickerJava.ItemPickerUpgradeWeapons();
            w.name = type;
            ItemPickerJava.WeaponUpgrades.add(w);
            WeaponUpgradeMap.put(type, w);
            KahluaTableImpl upgrades = (KahluaTableImpl)objectObjectEntry.getValue();

            for (Entry<Object, Object> up : upgrades.delegate.entrySet()) {
                String upType = up.getValue().toString();
                w.upgrades.add(upType);
            }
        }

        ParseSuburbsDistributions();
        ParseVehicleDistributions();
        ParseProceduralDistributions();
    }

    private static void ParseSuburbsDistributions() {
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.env.rawget("SuburbsDistributions");

        for (Entry<Object, Object> objectObjectEntry : table.delegate.entrySet()) {
            String key = objectObjectEntry.getKey().toString();
            KahluaTableImpl containers = (KahluaTableImpl)objectObjectEntry.getValue();
            if (containers.delegate.containsKey("rolls")) {
                ItemPickerJava.ItemPickerContainer c = ExtractContainersFromLua(containers);
                ItemPickerJava.containers.put(key, c);
            } else {
                ItemPickerJava.ItemPickerRoom room = new ItemPickerJava.ItemPickerRoom();
                rooms.put(key, room);

                for (Entry<Object, Object> containerSet : containers.delegate.entrySet()) {
                    String containerName = containerSet.getKey().toString();
                    if (containerSet.getValue() instanceof Double) {
                        room.fillRand = ((Double)containerSet.getValue()).intValue();
                    } else if ("isShop".equals(containerName)) {
                        room.isShop = (Boolean)containerSet.getValue();
                    } else if ("professionChance".equals(containerName)) {
                        room.professionChance = ((Double)containerSet.getValue()).intValue();
                    } else if ("outfit".equals(containerName)) {
                        room.outfit = (String)containerSet.getValue();
                    } else if ("outfitFemale".equals(containerName)) {
                        room.outfitFemale = (String)containerSet.getValue();
                    } else if ("outfitMale".equals(containerName)) {
                        room.outfitMale = (String)containerSet.getValue();
                    } else if ("outfitChance".equals(containerName)) {
                        room.outfitChance = (String)containerSet.getValue();
                    } else if ("vehicle".equals(containerName)) {
                        room.vehicle = (String)containerSet.getValue();
                    } else if ("vehicles".equals(containerName)) {
                        room.vehicles = Arrays.asList(containerName.split(";"));
                    } else if ("vehicleChance".equals(containerName)) {
                        room.vehicleChance = (String)containerSet.getValue();
                    } else if ("vehicleDistribution".equals(containerName)) {
                        room.vehicleDistribution = (String)containerSet.getValue();
                    } else if ("vehicleSkin".equals(containerName)) {
                        room.vehicleSkin = (Integer)containerSet.getValue();
                    } else if ("femaleChance".equals(containerName)) {
                        room.femaleChance = (String)containerSet.getValue();
                    } else if ("roomTypes".equals(containerName)) {
                        room.roomTypes = (String)containerSet.getValue();
                    } else if ("zoneRequires".equals(containerName)) {
                        room.zoneRequires = (String)containerSet.getValue();
                    } else if ("zoneDisallows".equals(containerName)) {
                        room.zoneDisallows = (String)containerSet.getValue();
                    } else if ("containerChance".equals(containerName)) {
                        room.containerChance = (String)containerSet.getValue();
                    } else if ("femaleOdds".equals(containerName)) {
                        room.femaleOdds = (String)containerSet.getValue();
                    } else if ("bagType".equals(containerName)) {
                        room.bagType = (String)containerSet.getValue();
                    } else if ("bagTable".equals(containerName)) {
                        room.bagTable = (String)containerSet.getValue();
                    } else {
                        KahluaTableImpl con = null;

                        try {
                            con = (KahluaTableImpl)containerSet.getValue();
                        } catch (Exception var11) {
                            var11.printStackTrace();
                        }

                        if (con.delegate.containsKey("procedural")
                            || !containerName.isEmpty() && con.delegate.containsKey("rolls") && con.delegate.containsKey("items")) {
                            ItemPickerJava.ItemPickerContainer c = ExtractContainersFromLua(con);
                            room.containers.put(containerName, c);
                        } else {
                            DebugLog.ItemPicker.error("ERROR: SuburbsDistributions[\"" + key + "\"] is broken");
                        }
                    }
                }

                room.compact();
            }
        }
    }

    private static void ParseVehicleDistributions() {
        VehicleDistributions.clear();
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.env.rawget("VehicleDistributions");
        if (table != null && table.rawget(1) instanceof KahluaTableImpl) {
            table = (KahluaTableImpl)table.rawget(1);

            for (Entry<Object, Object> entry : table.delegate.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof KahluaTableImpl tableEntry) {
                    ItemPickerJava.VehicleDistribution vehicleDistribution = new ItemPickerJava.VehicleDistribution();
                    if (tableEntry.rawget("Normal") instanceof KahluaTableImpl normal) {
                        ItemPickerJava.ItemPickerRoom room = new ItemPickerJava.ItemPickerRoom();

                        for (Entry<Object, Object> containerSet : normal.delegate.entrySet()) {
                            String containerName = containerSet.getKey().toString();
                            if (!containerName.equals("specificId")) {
                                room.containers.put(containerName, ExtractContainersFromLua((KahluaTableImpl)containerSet.getValue()));
                            }
                        }

                        vehicleDistribution.normal = room;
                    }

                    if (tableEntry.rawget("Specific") instanceof KahluaTableImpl specific) {
                        for (int i = 1; i <= specific.len(); i++) {
                            KahluaTableImpl table3 = (KahluaTableImpl)specific.rawget(i);
                            ItemPickerJava.ItemPickerRoom room = new ItemPickerJava.ItemPickerRoom();

                            for (Entry<Object, Object> containerSetx : table3.delegate.entrySet()) {
                                String containerName = containerSetx.getKey().toString();
                                if (containerName.equals("specificId")) {
                                    room.specificId = (String)containerSetx.getValue();
                                } else {
                                    room.containers.put(containerName, ExtractContainersFromLua((KahluaTableImpl)containerSetx.getValue()));
                                }
                            }

                            vehicleDistribution.specific.add(room);
                        }
                    }

                    vehicleDistribution.compact();
                    if (vehicleDistribution.normal != null) {
                        VehicleDistributions.put((String)entry.getKey(), vehicleDistribution);
                    }
                }
            }
        }
    }

    private static void ParseProceduralDistributions() {
        ProceduralDistributions.clear();
        if (LuaManager.env.rawget("ProceduralDistributions") instanceof KahluaTableImpl t1) {
            if (t1.rawget("list") instanceof KahluaTableImpl t2) {
                for (Entry<Object, Object> entry : t2.delegate.entrySet()) {
                    String name = entry.getKey().toString();
                    KahluaTableImpl t3 = (KahluaTableImpl)entry.getValue();
                    ItemPickerJava.ItemPickerContainer ipc = ExtractContainersFromLua(t3);
                    ProceduralDistributions.put(name, ipc);
                }
            }
        }
    }

    private static ItemPickerJava.ItemPickerContainer ExtractContainersFromLua(KahluaTableImpl con) {
        ItemPickerJava.ItemPickerContainer c = new ItemPickerJava.ItemPickerContainer();
        if (con.delegate.containsKey("isShop")) {
            c.isShop = con.rawgetBool("isShop");
        }

        if (con.delegate.containsKey("procedural")) {
            c.procedural = con.rawgetBool("procedural");
            c.proceduralItems = ExtractProcList(con);
            return c;
        } else {
            if (con.delegate.containsKey("noAutoAge")) {
                c.noAutoAge = con.rawgetBool("noAutoAge");
            }

            if (con.delegate.containsKey("fillRand")) {
                c.fillRand = con.rawgetInt("fillRand");
            }

            if (con.delegate.containsKey("maxMap")) {
                c.maxMap = con.rawgetInt("maxMap");
            }

            if (con.delegate.containsKey("stashChance")) {
                c.stashChance = con.rawgetInt("stashChance");
            }

            if (con.delegate.containsKey("dontSpawnAmmo")) {
                c.dontSpawnAmmo = con.rawgetBool("dontSpawnAmmo");
            }

            if (con.delegate.containsKey("ignoreZombieDensity")) {
                c.ignoreZombieDensity = con.rawgetBool("ignoreZombieDensity");
            }

            if (con.delegate.containsKey("cookFood")) {
                c.cookFood = con.rawgetBool("cookFood");
            }

            if (con.delegate.containsKey("canBurn")) {
                c.canBurn = con.rawgetBool("canBurn");
            }

            if (con.delegate.containsKey("isTrash")) {
                c.isTrash = con.rawgetBool("isTrash");
            }

            if (con.delegate.containsKey("isWorn")) {
                c.isWorn = con.rawgetBool("isWorn");
            }

            if (con.delegate.containsKey("isRotten")) {
                c.isRotten = con.rawgetBool("isRotten");
            }

            if (con.delegate.containsKey("onlyOne")) {
                c.onlyOne = con.rawgetBool("onlyOne");
            }

            double rolls = (Double)con.delegate.get("rolls");
            if (con.delegate.containsKey("junk")) {
                c.junk = ExtractContainersFromLua((KahluaTableImpl)con.rawget("junk"));
            }

            if (con.delegate.containsKey("bags")) {
                c.bags = ExtractContainersFromLua((KahluaTableImpl)con.rawget("bags"));
            }

            if (con.delegate.containsKey("defaultInventoryLoot")) {
                c.defaultInventoryLoot = con.rawgetBool("defaultInventoryLoot");
            }

            c.rolls = (int)rolls;
            KahluaTableImpl itemListT = (KahluaTableImpl)con.delegate.get("items");
            ArrayList<ItemPickerJava.ItemPickerItem> itemList = new ArrayList<>();
            int len = itemListT.len();

            for (int i = 0; i < len; i += 2) {
                String name = Type.tryCastTo(itemListT.delegate.get(KahluaUtil.toDouble((long)(i + 1))), String.class);
                Double chance = Type.tryCastTo(itemListT.delegate.get(KahluaUtil.toDouble((long)(i + 2))), Double.class);
                if (name != null && chance != null) {
                    Item scriptItem = ScriptManager.instance.FindItem(name);
                    boolean itemExists = scriptItem != null || InventoryItemFactory.getItem(name, true) != null;
                    if (!itemExists || scriptItem != null && scriptItem.obsolete) {
                        if (Core.debug && !addedInvalidAlready.contains(name)) {
                            addedInvalidAlready.add(name);
                            DebugLog.ItemPicker.println("ignoring invalid ItemPicker item type \"%s\", obsolete = \"%s\"", name, scriptItem != null);
                        }
                    } else {
                        if (scriptItem != null) {
                            scriptItem.setCanSpawnAsLoot(true);
                        }

                        ItemPickerJava.ItemPickerItem ipi = new ItemPickerJava.ItemPickerItem();
                        ipi.itemName = name;
                        ipi.chance = chance.floatValue();
                        itemList.add(ipi);
                    }
                }
            }

            c.items = itemList.toArray(c.items);
            return c;
        }
    }

    private static ArrayList<ItemPickerJava.ProceduralItem> ExtractProcList(KahluaTableImpl table) {
        ArrayList<ItemPickerJava.ProceduralItem> result = new ArrayList<>();
        KahluaTableImpl procList = (KahluaTableImpl)table.rawget("procList");
        KahluaTableIterator it = procList.iterator();

        while (it.advance()) {
            KahluaTableImpl entry = (KahluaTableImpl)it.getValue();
            ItemPickerJava.ProceduralItem procItem = new ItemPickerJava.ProceduralItem();
            procItem.name = entry.rawgetStr("name");
            procItem.min = entry.rawgetInt("min");
            procItem.max = entry.rawgetInt("max");
            procItem.weightChance = entry.rawgetInt("weightChance");
            String forceForItems = entry.rawgetStr("forceForItems");
            String forceForZones = entry.rawgetStr("forceForZones");
            String forceForTiles = entry.rawgetStr("forceForTiles");
            String forceForRooms = entry.rawgetStr("forceForRooms");
            if (!StringUtils.isNullOrWhitespace(forceForItems)) {
                procItem.forceForItems = Arrays.asList(forceForItems.split(";"));
            }

            if (!StringUtils.isNullOrWhitespace(forceForZones)) {
                procItem.forceForZones = Arrays.asList(forceForZones.split(";"));
            }

            if (!StringUtils.isNullOrWhitespace(forceForTiles)) {
                procItem.forceForTiles = Arrays.asList(forceForTiles.split(";"));
            }

            if (!StringUtils.isNullOrWhitespace(forceForRooms)) {
                procItem.forceForRooms = Arrays.asList(forceForRooms.split(";"));
            }

            result.add(procItem);
        }

        return result;
    }

    public static void InitSandboxLootSettings() {
        otherLootModifier = (float)SandboxOptions.getInstance().otherLootNew.getValue();
        foodLootModifier = (float)SandboxOptions.getInstance().foodLootNew.getValue();
        weaponLootModifier = (float)SandboxOptions.getInstance().weaponLootNew.getValue();
        rangedWeaponLootModifier = (float)SandboxOptions.getInstance().rangedWeaponLootNew.getValue();
        ammoLootModifier = (float)SandboxOptions.getInstance().ammoLootNew.getValue();
        cannedFoodLootModifier = (float)SandboxOptions.getInstance().cannedFoodLootNew.getValue();
        literatureLootModifier = (float)SandboxOptions.getInstance().literatureLootNew.getValue();
        survivalGearsLootModifier = (float)SandboxOptions.getInstance().survivalGearsLootNew.getValue();
        medicalLootModifier = (float)SandboxOptions.getInstance().medicalLootNew.getValue();
        mechanicsLootModifier = (float)SandboxOptions.getInstance().mechanicsLootNew.getValue();
        clothingLootModifier = (float)SandboxOptions.getInstance().clothingLootNew.getValue();
        containerLootModifier = (float)SandboxOptions.getInstance().containerLootNew.getValue();
        keyLootModifier = (float)SandboxOptions.getInstance().keyLootNew.getValue();
        keyLootModifierD100 = (float)SandboxOptions.getInstance().keyLootNew.getValue();
        mediaLootModifier = (float)SandboxOptions.getInstance().mediaLootNew.getValue();
        mementoLootModifier = (float)SandboxOptions.getInstance().mementoLootNew.getValue();
        cookwareLootModifier = (float)SandboxOptions.getInstance().cookwareLootNew.getValue();
        materialLootModifier = (float)SandboxOptions.getInstance().materialLootNew.getValue();
        farmingLootModifier = (float)SandboxOptions.getInstance().farmingLootNew.getValue();
        toolLootModifier = (float)SandboxOptions.getInstance().toolLootNew.getValue();
    }

    private static float doSandboxSettings(int value) {
        switch (value) {
            case 1:
                return 0.0F;
            case 2:
                return (float)SandboxOptions.instance.insaneLootFactor.getValue();
            case 3:
                return (float)SandboxOptions.instance.extremeLootFactor.getValue();
            case 4:
                return (float)SandboxOptions.instance.rareLootFactor.getValue();
            case 5:
                return (float)SandboxOptions.instance.normalLootFactor.getValue();
            case 6:
                return (float)SandboxOptions.instance.commonLootFactor.getValue();
            case 7:
                return (float)SandboxOptions.instance.abundantLootFactor.getValue();
            default:
                return 0.6F;
        }
    }

    public static void fillContainer(ItemContainer container, IsoPlayer player) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.FillContainer);
        fillContainerInternal(pickInfo, container, player);
    }

    private static void fillContainerInternal(ItemPickInfo pickInfo, ItemContainer container, IsoPlayer player) {
        if (!GameClient.client && !"Tutorial".equals(Core.gameMode)) {
            if (container != null) {
                IsoGridSquare sq = container.getSourceGrid();
                IsoRoom room = null;
                if (sq != null) {
                    room = sq.getRoom();
                    if (!container.getType().equals("inventorymale") && !container.getType().equals("inventoryfemale")) {
                        ItemPickerJava.ItemPickerRoom roomDist = null;
                        if (rooms.containsKey("all")) {
                            roomDist = rooms.get("all");
                        }

                        if (room != null && rooms.containsKey(room.getName())) {
                            String roomName = room.getName();
                            ItemPickerJava.ItemPickerRoom roomDist2 = rooms.get(roomName);
                            ItemPickerJava.ItemPickerContainer containerDist = null;
                            if (roomDist2.containers.containsKey(container.getType())) {
                                containerDist = roomDist2.containers.get(container.getType());
                            }

                            if (containerDist == null && roomDist2.containers.containsKey("other")) {
                                containerDist = roomDist2.containers.get("other");
                            }

                            if (containerDist == null && roomDist2.containers.containsKey("all")) {
                                containerDist = roomDist2.containers.get("all");
                                roomName = "all";
                            }

                            if (containerDist == null) {
                                fillContainerTypeInternal(pickInfo, roomDist, container, roomName, player);
                                LuaEventManager.triggerEvent("OnFillContainer", roomName, container.getType(), container);
                            } else {
                                if (rooms.containsKey(room.getName())) {
                                    roomDist = rooms.get(room.getName());
                                }

                                if (roomDist != null) {
                                    fillContainerTypeInternal(pickInfo, roomDist, container, room.getName(), player);
                                    LuaEventManager.triggerEvent("OnFillContainer", room.getName(), container.getType(), container);
                                }
                            }
                        } else {
                            String roomNamex = null;
                            if (room != null) {
                                roomNamex = room.getName();
                            } else {
                                roomNamex = "all";
                            }

                            fillContainerTypeInternal(pickInfo, roomDist, container, roomNamex, player);
                            LuaEventManager.triggerEvent("OnFillContainer", roomNamex, container.getType(), container);
                        }
                    } else if (container.getParent() == null
                        || !(container.getParent() instanceof IsoDeadBody)
                        || !((IsoDeadBody)container.getParent()).isSkeleton()) {
                        String containerType = container.getType();
                        if (container.getParent() != null && container.getParent() instanceof IsoDeadBody) {
                            containerType = ((IsoDeadBody)container.getParent()).getOutfitName();
                        }

                        ItemPickerJava.ItemPickerContainer containerDistx = rooms.get("all").containers.get("Outfit_" + containerType);

                        for (int i = 0; i < container.getItems().size(); i++) {
                            InventoryItem item = container.getItems().get(i);
                            if (item instanceof InventoryContainer inventoryContainer) {
                                ItemPickerJava.ItemPickerContainer itemPickerContainer = containers.get(item.getType());
                                if (containerDistx != null && containerDistx.bags != null && !item.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                                    rollContainerItemInternal(pickInfo, inventoryContainer, null, containerDistx.bags);
                                    LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", item.getType(), containerDistx.bags);
                                } else if (itemPickerContainer != null && Rand.Next(itemPickerContainer.fillRand) == 0) {
                                    rollContainerItemInternal(pickInfo, inventoryContainer, null, containers.get(item.getType()));
                                    LuaEventManager.triggerEvent("OnFillContainer", "Zombie Bag", item.getType(), inventoryContainer.getItemContainer());
                                }
                            }
                        }

                        boolean defaultInventoryLoot = true;
                        if (containerDistx != null) {
                            defaultInventoryLoot = containerDistx.defaultInventoryLoot;
                        }

                        if (containerDistx != null) {
                            rollItemInternal(pickInfo, containerDistx, container, true, player, null);
                        }

                        if (defaultInventoryLoot) {
                            containerDistx = rooms.get("all").containers.get(container.getType());
                            rollItemInternal(pickInfo, containerDistx, container, true, player, null);
                        }

                        InstanceTracker.inc("Container Rolls", "Zombie/" + containerType);
                        LuaEventManager.triggerEvent("OnFillContainer", "Zombie", containerType, container);
                    }
                }
            }
        }
    }

    public static void fillContainerType(ItemPickerJava.ItemPickerRoom roomDist, ItemContainer container, String roomName, IsoGameCharacter character) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.FillContainerType);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }

        fillContainerTypeInternal(pickInfo, roomDist, container, roomName, character);
    }

    private static void fillContainerTypeInternal(
        ItemPickInfo pickInfo, ItemPickerJava.ItemPickerRoom roomDist, ItemContainer container, String roomName, IsoGameCharacter character
    ) {
        boolean doItemContainer = true;
        if (NoContainerFillRooms.contains(roomName)) {
            doItemContainer = false;
        }

        ItemPickerJava.ItemPickerContainer containerDist = null;
        if (roomDist == null) {
            containerDist = roomDist.containers.get("all");
            rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        } else if (roomDist.containers.containsKey("all")) {
            containerDist = roomDist.containers.get("all");
            rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        }

        InstanceTracker.inc("Container Rolls", (StringUtils.isNullOrEmpty(roomName) ? "unknown" : roomName) + "/" + container.getType());
        containerDist = roomDist.containers.get(container.getType());
        if (containerDist == null) {
            containerDist = roomDist.containers.get("other");
        }

        if (containerDist != null) {
            rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
        }
    }

    public static InventoryItem tryAddItemToContainer(ItemContainer container, String itemType, ItemPickerJava.ItemPickerContainer containerDist) {
        Item scriptItem = ScriptManager.instance.FindItem(itemType);
        if (scriptItem == null) {
            return null;
        } else if (scriptItem.obsolete) {
            return null;
        } else {
            float totalWeight = scriptItem.getActualWeight() * scriptItem.getCount();
            if (!container.hasRoomFor(null, totalWeight)) {
                return null;
            } else {
                boolean corpse = container.getContainingItem() instanceof InventoryContainer
                    && container.getContainingItem().getContainer() != null
                    && container.getContainingItem().getContainer().getParent() != null
                    && container.getContainingItem().getContainer().getParent() instanceof IsoDeadBody;
                if (!corpse && container.getContainingItem() instanceof InventoryContainer) {
                    ItemContainer contain = container.getContainingItem().getContainer();
                    if (contain != null && !contain.hasRoomFor(null, totalWeight)) {
                        return null;
                    }
                }

                return ItemSpawner.spawnItem(itemType, container);
            }
        }
    }

    private static void rollProceduralItem(
        ArrayList<ItemPickerJava.ProceduralItem> proceduralItems,
        ItemContainer container,
        float zombieDensity,
        IsoGameCharacter character,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.RollProceduralItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }

        rollProceduralItemInternal(pickInfo, proceduralItems, container, zombieDensity, character, roomDist);
    }

    private static void rollProceduralItemInternal(
        ItemPickInfo itemPickInfo,
        ArrayList<ItemPickerJava.ProceduralItem> proceduralItems,
        ItemContainer container,
        float zombieDensity,
        IsoGameCharacter character,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        if (container.getSourceGrid() != null) {
            boolean room = container.getSourceGrid().getRoom() != null;
            HashMap<String, Integer> alreadySpawnedStuff = null;
            if (room) {
                alreadySpawnedStuff = container.getSourceGrid().getRoom().getRoomDef().getProceduralSpawnedContainer();
            }

            HashMap<String, Integer> forcedToSpawn = new HashMap<>();
            HashMap<String, Integer> normalSpawn = new HashMap<>();

            for (int ip = 0; ip < proceduralItems.size(); ip++) {
                ItemPickerJava.ProceduralItem proceduralItem = proceduralItems.get(ip);
                String name = proceduralItem.name;
                int min = proceduralItem.min;
                int max = proceduralItem.max;
                int weightChance = proceduralItem.weightChance;
                List<String> forceForItems = proceduralItem.forceForItems;
                List<String> forceForZones = proceduralItem.forceForZones;
                List<String> forceForTiles = proceduralItem.forceForTiles;
                List<String> forceForRooms = proceduralItem.forceForRooms;
                if (alreadySpawnedStuff != null && alreadySpawnedStuff.get(name) == null) {
                    alreadySpawnedStuff.put(name, 0);
                }

                if (forceForItems != null
                    && room
                    && container.getSourceGrid() != null
                    && container.getSourceGrid().getRoom() != null
                    && container.getSourceGrid().getRoom().getBuilding() != null
                    && container.getSourceGrid().getRoom().getBuilding().getRoomsNumber() <= RandomizedBuildingBase.maximumRoomCount) {
                    for (int x = container.getSourceGrid().getRoom().getRoomDef().x; x < container.getSourceGrid().getRoom().getRoomDef().x2; x++) {
                        for (int y = container.getSourceGrid().getRoom().getRoomDef().y; y < container.getSourceGrid().getRoom().getRoomDef().y2; y++) {
                            IsoGridSquare sq = container.getSourceGrid().getCell().getGridSquare(x, y, container.getSourceGrid().z);
                            if (sq != null) {
                                for (int i = 0; i < sq.getObjects().size(); i++) {
                                    IsoObject obj = sq.getObjects().get(i);
                                    if (forceForItems.contains(obj.getSprite().name)) {
                                        forcedToSpawn.clear();
                                        forcedToSpawn.put(name, -1);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (forceForTiles == null) {
                    if (forceForRooms != null && room) {
                        IsoGridSquare sq = container.getSourceGrid();
                        if (sq != null) {
                            for (int ix = 0; ix < forceForRooms.size(); ix++) {
                                if (sq.getBuilding().getRandomRoom(forceForRooms.get(ix)) != null) {
                                    forcedToSpawn.clear();
                                    forcedToSpawn.put(name, -1);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    IsoGridSquare sq = container.getSourceGrid();
                    if (sq != null) {
                        for (int ixx = 0; ixx < sq.getObjects().size(); ixx++) {
                            IsoObject obj = sq.getObjects().get(ixx);
                            if (obj.getSprite() != null && forceForTiles.contains(obj.getSprite().getName())) {
                                forcedToSpawn.clear();
                                forcedToSpawn.put(name, -1);
                                break;
                            }
                        }
                    } else if (forceForZones != null) {
                        ArrayList<Zone> metazones = IsoWorld.instance.metaGrid.getZonesAt(container.getSourceGrid().x, container.getSourceGrid().y, 0);

                        for (int ixxx = 0; ixxx < metazones.size(); ixxx++) {
                            if ((alreadySpawnedStuff == null || alreadySpawnedStuff.get(name) < max)
                                && (forceForZones.contains(metazones.get(ixxx).type) || forceForZones.contains(metazones.get(ixxx).name))) {
                                forcedToSpawn.clear();
                                forcedToSpawn.put(name, -1);
                                break;
                            }
                        }
                    }
                }

                if (forceForItems == null && forceForZones == null && forceForTiles == null && forceForRooms == null) {
                    if (room && min == 1 && alreadySpawnedStuff.get(name) == 0) {
                        forcedToSpawn.put(name, weightChance);
                    } else if (!room || alreadySpawnedStuff.get(name) < max) {
                        normalSpawn.put(name, weightChance);
                    }
                }
            }

            String containerNameToSpawn = null;
            if (!forcedToSpawn.isEmpty()) {
                containerNameToSpawn = getDistribInHashMap(forcedToSpawn);
            } else if (!normalSpawn.isEmpty()) {
                containerNameToSpawn = getDistribInHashMap(normalSpawn);
            }

            if (containerNameToSpawn != null) {
                ItemPickerJava.ItemPickerContainer containerDistToSpawn = ProceduralDistributions.get(containerNameToSpawn);
                if (containerDistToSpawn != null) {
                    if (containerDistToSpawn.junk != null) {
                        doRollItemInternal(itemPickInfo, containerDistToSpawn.junk, container, zombieDensity, character, true, roomDist, true);
                    }

                    doRollItemInternal(itemPickInfo, containerDistToSpawn, container, zombieDensity, character, true, roomDist);
                    if (alreadySpawnedStuff != null) {
                        alreadySpawnedStuff.put(containerNameToSpawn, alreadySpawnedStuff.get(containerNameToSpawn) + 1);
                    }
                }
            }
        }
    }

    private static String getDistribInHashMap(HashMap<String, Integer> map) {
        int totalWeight = 0;
        int currentChance = 0;

        for (String name : map.keySet()) {
            totalWeight += map.get(name);
        }

        if (totalWeight == -1) {
            int rand = Rand.Next(map.size());
            Iterator<String> var8 = map.keySet().iterator();

            for (int index = 0; var8.hasNext(); index++) {
                if (index == rand) {
                    return (String)var8.next();
                }
            }
        }

        int rand = Rand.Next(totalWeight);

        for (String name : map.keySet()) {
            int chance = map.get(name);
            currentChance += chance;
            if (currentChance >= rand) {
                return name;
            }
        }

        return null;
    }

    public static void rollItem(
        ItemPickerJava.ItemPickerContainer containerDist,
        ItemContainer container,
        boolean doItemContainer,
        IsoGameCharacter character,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.RollItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }

        rollItemInternal(pickInfo, containerDist, container, doItemContainer, character, roomDist);
    }

    private static void rollItemInternal(
        ItemPickInfo itemPickInfo,
        ItemPickerJava.ItemPickerContainer containerDist,
        ItemContainer container,
        boolean doItemContainer,
        IsoGameCharacter character,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        if (!GameClient.client && !GameServer.server) {
            player = IsoPlayer.getInstance();
        }

        if (containerDist != null && container != null) {
            float zombieDensity = getZombieDensityFactor(containerDist, container);
            if (containerDist.procedural) {
                rollProceduralItemInternal(itemPickInfo, containerDist.proceduralItems, container, zombieDensity, character, roomDist);
            } else {
                if (containerDist.junk != null) {
                    doRollItemInternal(itemPickInfo, containerDist.junk, container, zombieDensity, character, doItemContainer, roomDist, true);
                }

                doRollItemInternal(itemPickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist);
            }
        }
    }

    public static void doRollItem(
        ItemPickerJava.ItemPickerContainer containerDist,
        ItemContainer container,
        float zombieDensity,
        IsoGameCharacter character,
        boolean doItemContainer,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(container, ItemPickInfo.Caller.DoRollItem);
        if (pickInfo != null) {
            pickInfo.updateRoomDist(roomDist);
        }

        doRollItemInternal(pickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist);
    }

    private static void doRollItemInternal(
        ItemPickInfo pickInfo,
        ItemPickerJava.ItemPickerContainer containerDist,
        ItemContainer container,
        float zombieDensity,
        IsoGameCharacter character,
        boolean doItemContainer,
        ItemPickerJava.ItemPickerRoom roomDist
    ) {
        doRollItemInternal(pickInfo, containerDist, container, zombieDensity, character, doItemContainer, roomDist, false);
    }

    private static void doRollItemInternal(
        ItemPickInfo pickInfo,
        ItemPickerJava.ItemPickerContainer containerDist,
        ItemContainer container,
        float zombieDensity,
        IsoGameCharacter character,
        boolean doItemContainer,
        ItemPickerJava.ItemPickerRoom roomDist,
        boolean isJunk
    ) {
        IsoObject parent = null;
        if (container.getParent() != null) {
            parent = container.getParent();
        }

        String itemName = "";
        boolean dirtyClothes = false;
        boolean wetClothes = false;
        boolean isEmptyFluidContainer = false;
        if ((parent instanceof IsoClothingDryer || Objects.equals(container.getType(), "clothingdryer")) && Rand.NextBool(5)) {
            wetClothes = true;
        }

        if (parent instanceof IsoClothingWasher || Objects.equals(container.getType(), "clothingwasher")) {
            if (Rand.NextBool(2)) {
                wetClothes = true;
            } else {
                dirtyClothes = true;
            }
        }

        boolean isTrash = containerDist.isTrash;
        boolean isShop = false;
        if (!container.isCorpse() && !isTrash) {
            isShop = containerDist.isShop || container.isShop();
            if (!isShop && roomDist != null) {
                isShop = roomDist.isShop;
            }

            if (!isShop && container.getSquare() != null) {
                isShop = container.getSquare().isShop();
            }

            if (!isShop && container.getSquare() != null && container.getSquare().getRoom() != null) {
                isShop = container.getSquare().getRoom().isShop();
            }
        }

        boolean laundry = parent instanceof IsoClothingDryer
            || Objects.equals(container.getType(), "clothingdryer")
            || parent instanceof IsoClothingWasher
            || Objects.equals(container.getType(), "clothingwasher");
        int rolls = (int)(containerDist.rolls * SandboxOptions.instance.rollsMultiplier.getValue());
        rolls = Math.max(rolls, 1);

        for (int m = 0; m < rolls; m++) {
            ItemPickerJava.ItemPickerItem[] items = containerDist.items;

            for (int i = 0; i < items.length; i++) {
                ItemPickerJava.ItemPickerItem item = items[i];
                itemName = item.itemName;
                Item scriptItem = ScriptManager.instance.FindItem(itemName);
                if (scriptItem == null && itemName.endsWith("Empty")) {
                    itemName = itemName.substring(0, itemName.length() - 5);
                    scriptItem = ScriptManager.instance.FindItem(itemName, true);
                    if (scriptItem != null) {
                        if (!scriptItem.containsComponent(ComponentType.FluidContainer)) {
                            scriptItem = null;
                        } else {
                            isEmptyFluidContainer = true;
                        }
                    }
                }

                if (Rand.Next(10000) <= getActualSpawnChance(item, character, container, zombieDensity, isJunk)) {
                    InventoryItem spawnItem = tryAddItemToContainer(container, itemName, containerDist);
                    if (spawnItem == null) {
                        return;
                    }

                    float zombieDensity2 = getAdjustedZombieDensity(zombieDensity, scriptItem, isJunk);
                    ItemConfigurator.ConfigureItem(spawnItem, pickInfo, isJunk, zombieDensity2);
                    if (!isShop) {
                        checkStashItem(spawnItem, containerDist);
                    }

                    if (container.getType().equals("freezer") && spawnItem instanceof Food food && food.isFreezing()) {
                        food.freeze();
                    }

                    if (spawnItem instanceof Key key
                        && spawnItem.hasTag(ItemTag.BUILDING_KEY)
                        && !container.getType().equals("inventoryfemale")
                        && !container.getType().equals("inventorymale")) {
                        key.takeKeyId();
                        if (container.getSourceGrid() != null
                            && container.getSourceGrid().getBuilding() != null
                            && container.getSourceGrid().getBuilding().getDef() != null) {
                            BuildingDef def = container.getSourceGrid().getBuilding().getDef();
                            int keys = def.getKeySpawned();
                            int maxKeys = container.getSourceGrid().getBuilding().getRoomsNumber() / 10 + 1;
                            if (maxKeys < 2) {
                                maxKeys = 2;
                            }

                            if (keys <= maxKeys && container.getCountTagRecurse(ItemTag.BUILDING_KEY) <= 1) {
                                def.setKeySpawned(keys + 1);
                                ItemPickerJava.KeyNamer.nameKey(spawnItem, container.getSourceGrid());
                            } else {
                                container.Remove(spawnItem);
                            }
                        } else {
                            container.Remove(spawnItem);
                        }
                    } else if (spawnItem instanceof Key
                        && spawnItem.hasTag(ItemTag.BUILDING_KEY)
                        && (container.getType().equals("inventoryfemale") || container.getType().equals("inventorymale"))) {
                        container.Remove(spawnItem);
                    }

                    if (spawnItem instanceof Key && (spawnItem.getFullType().equals("Base.CarKey") || spawnItem.hasTag(ItemTag.CAR_KEY))) {
                        addVehicleKeyAsLoot(spawnItem, container);
                    }

                    String mediaCat = spawnItem.getScriptItem().getRecordedMediaCat();
                    if (mediaCat != null) {
                        RecordedMedia recordedMedia = ZomboidRadio.getInstance().getRecordedMedia();
                        MediaData mediaData = recordedMedia.getRandomFromCategory(mediaCat);
                        if (mediaData == null) {
                            container.Remove(spawnItem);
                            if ("Home-VHS".equalsIgnoreCase(mediaCat)) {
                                mediaData = recordedMedia.getRandomFromCategory("Retail-VHS");
                                if (mediaData == null) {
                                    return;
                                }

                                spawnItem = ItemSpawner.spawnItem("Base.VHS_Retail", container);
                                if (spawnItem == null) {
                                    return;
                                }

                                spawnItem.setRecordedMediaData(mediaData);
                            }

                            return;
                        }

                        spawnItem.setRecordedMediaData(mediaData);
                    }

                    if (!containerDist.noAutoAge) {
                        spawnItem.setAutoAge();
                    }

                    if (!isTrash && WeaponUpgradeMap.containsKey(spawnItem.getType())) {
                        DoWeaponUpgrade(spawnItem);
                    }

                    if (spawnItem instanceof DrainableComboItem comboItem && spawnItem.hasTag(ItemTag.LESS_FULL) && !isShop && !isTrash && Rand.Next(100) < 80) {
                        comboItem.randomizeUses();
                    } else if (spawnItem instanceof DrainableComboItem drainableComboItem && !isShop && !isTrash && Rand.Next(100) < 40) {
                        drainableComboItem.randomizeUses();
                    }

                    if (isEmptyFluidContainer && spawnItem.hasComponent(ComponentType.FluidContainer)) {
                        spawnItem.getFluidContainer().Empty();
                    }

                    if (!isShop
                        && !isTrash
                        && (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon || spawnItem.hasSharpness())
                        && Rand.Next(100) < 40) {
                        spawnItem.randomizeGeneralCondition();
                    }

                    if (!isTrash && spawnItem instanceof HandWeapon weapon && !containerDist.dontSpawnAmmo && Rand.Next(100) < 90) {
                        weapon.randomizeFirearmAsLoot();
                    }

                    if (!isShop && spawnItem instanceof InventoryContainer inventoryContainer) {
                        if (containerDist.bags != null && !spawnItem.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                            rollContainerItemInternal(pickInfo, inventoryContainer, character, containerDist.bags);
                            LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), containerDist.bags);
                        } else if (containers.containsKey(spawnItem.getType())) {
                            ItemPickerJava.ItemPickerContainer itemPickerContainer = containers.get(spawnItem.getType());
                            if (doItemContainer && Rand.Next(itemPickerContainer.fillRand) == 0) {
                                rollContainerItemInternal(pickInfo, inventoryContainer, character, containers.get(spawnItem.getType()));
                                if (containers.get(spawnItem.getType()).junk != null) {
                                    rollContainerItemInternal(pickInfo, inventoryContainer, character, containers.get(spawnItem.getType()).junk, true);
                                }

                                if (spawnItem.hasTag(ItemTag.NEVER_EMPTY) && inventoryContainer.getItemContainer().isEmpty()) {
                                    container.Remove(spawnItem);
                                } else {
                                    LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), inventoryContainer.getItemContainer());
                                }
                            }
                        }
                    }

                    if (spawnItem instanceof Food food
                        && spawnItem.isCookable()
                        && (containerDist.cookFood || containerDist.canBurn)
                        && food.getReplaceOnCooked() == null) {
                        if (containerDist.canBurn && food.getMinutesToBurn() > 0.0F && Rand.Next(100) < 25) {
                            food.setBurnt(true);
                        } else {
                            food.setCooked(true);
                            food.setAutoAge();
                        }
                    }

                    if (containerDist.isTrash) {
                        trashItem(spawnItem);
                    }

                    if (containerDist.isWorn) {
                        wearDownItem(spawnItem);
                    }

                    if (containerDist.isRotten) {
                        rotItem(spawnItem);
                    }

                    if (spawnItem.hasTag(ItemTag.REGIONAL)) {
                        IsoGridSquare sq = container.getSquare();
                        if (sq != null) {
                            onCreateRegion(spawnItem, getSquareRegion(sq));
                        }
                    }

                    if (spawnItem instanceof Clothing clothing && (dirtyClothes || wetClothes)) {
                        if (dirtyClothes) {
                            clothing.randomizeCondition(0, 75, 1, 0);
                        }

                        if (wetClothes) {
                            clothing.randomizeCondition(100, 0, 0, 0);
                        }
                    }

                    if (!StringUtils.isNullOrEmpty(spawnItem.getScriptItem().getSpawnWith()) && !isTrash && !containerDist.isWorn) {
                        InventoryItem spawnWithItem = ItemSpawner.spawnItem(spawnItem.getScriptItem().getSpawnWith(), container);
                        if (spawnWithItem != null) {
                            spawnWithItem.copyClothing(spawnItem);
                        }
                    }

                    if (parent != null
                        && spawnItem.hasTag(ItemTag.APPLY_OWNER_NAME)
                        && parent instanceof IsoDeadBody deadBody
                        && deadBody.getDescriptor() != null) {
                        spawnItem.nameAfterDescriptor(deadBody.getDescriptor());
                    } else if (parent != null
                        && spawnItem.hasTag(ItemTag.MONOGRAM_OWNER_NAME)
                        && parent instanceof IsoDeadBody isoDeadBody
                        && isoDeadBody.getDescriptor() != null) {
                        spawnItem.monogramAfterDescriptor(isoDeadBody.getDescriptor());
                    }

                    if (spawnItem.hasTag(ItemTag.SPAWN_FULL_UNLESS_LAUNDRY) && !laundry) {
                        spawnItem.setCurrentUses(spawnItem.getMaxUses());
                    }

                    if (!isTrash && !container.isCorpse()) {
                        itemSpawnSanityCheck(spawnItem, container);
                    } else {
                        itemSpawnSanityCheck(spawnItem);
                    }

                    if (containerDist.onlyOne) {
                        return;
                    }
                }
            }
        }
    }

    private static void checkStashItem(InventoryItem spawnItem, ItemPickerJava.ItemPickerContainer containerDist) {
        if (containerDist.stashChance > 0 && spawnItem instanceof MapItem mapItem && !StringUtils.isNullOrEmpty(mapItem.getMapID())) {
            spawnItem.setStashChance(containerDist.stashChance);
        }

        StashSystem.checkStashItem(spawnItem);
    }

    public static void rollContainerItem(InventoryContainer bag, IsoGameCharacter character, ItemPickerJava.ItemPickerContainer containerDist) {
        ItemPickInfo pickInfo = ItemPickInfo.GetPickInfo(bag.getItemContainer(), ItemPickInfo.Caller.RollContainerItem);
        rollContainerItemInternal(pickInfo, bag, character, containerDist);
        if (containerDist.junk != null) {
            rollContainerItemInternal(pickInfo, bag, character, containerDist.junk, true);
        }
    }

    private static void rollContainerItemInternal(
        ItemPickInfo itemPickInfo, InventoryContainer bag, IsoGameCharacter character, ItemPickerJava.ItemPickerContainer containerDist
    ) {
        rollContainerItemInternal(itemPickInfo, bag, character, containerDist, false);
    }

    private static void rollContainerItemInternal(
        ItemPickInfo itemPickInfo, InventoryContainer bag, IsoGameCharacter character, ItemPickerJava.ItemPickerContainer containerDist, boolean isJunk
    ) {
        if (containerDist != null) {
            IsoObject parent = null;
            if (bag.getOutermostContainer() != null && bag.getOutermostContainer().getParent() != null) {
                parent = bag.getOutermostContainer().getParent();
            }

            ItemContainer container = bag.getInventory();
            float zombieDensity = getZombieDensityFactor(containerDist, container);
            String itemName = "";
            int rolls = (int)(containerDist.rolls * SandboxOptions.instance.rollsMultiplier.getValue());
            rolls = Math.max(rolls, 1);

            for (int m = 0; m < rolls; m++) {
                ItemPickerJava.ItemPickerItem[] items = containerDist.items;

                for (int i = 0; i < items.length; i++) {
                    ItemPickerJava.ItemPickerItem item = items[i];
                    itemName = item.itemName;
                    Item scriptItem = ScriptManager.instance.FindItem(itemName);
                    boolean isEmptyFluidContainer = false;
                    if (scriptItem == null && itemName.endsWith("Empty")) {
                        itemName = itemName.substring(0, itemName.length() - 5);
                        scriptItem = ScriptManager.instance.FindItem(itemName, true);
                        if (scriptItem != null) {
                            if (!scriptItem.containsComponent(ComponentType.FluidContainer)) {
                                continue;
                            }

                            isEmptyFluidContainer = true;
                        }
                    }

                    if (Rand.Next(10000) <= getActualSpawnChance(item, character, container, zombieDensity, isJunk)) {
                        InventoryItem spawnItem = tryAddItemToContainer(container, itemName, containerDist);
                        if (spawnItem == null) {
                            return;
                        }

                        ItemConfigurator.ConfigureItem(spawnItem, itemPickInfo, false, 0.0F);
                        MapItem mapItem = Type.tryCastTo(spawnItem, MapItem.class);
                        if (mapItem != null && !StringUtils.isNullOrEmpty(mapItem.getMapID()) && containerDist.maxMap > 0) {
                            int totalMap = 0;

                            for (int j = 0; j < container.getItems().size(); j++) {
                                MapItem invMap = Type.tryCastTo(container.getItems().get(j), MapItem.class);
                                if (invMap != null && !StringUtils.isNullOrEmpty(invMap.getMapID())) {
                                    totalMap++;
                                }
                            }

                            if (totalMap > containerDist.maxMap) {
                                container.Remove(spawnItem);
                            }
                        }

                        checkStashItem(spawnItem, containerDist);
                        if (!containerDist.isTrash && WeaponUpgradeMap.containsKey(spawnItem.getType())) {
                            DoWeaponUpgrade(spawnItem);
                        }

                        if (!containerDist.isTrash && spawnItem instanceof HandWeapon weapon && !containerDist.dontSpawnAmmo && Rand.Next(100) < 90) {
                            weapon.randomizeFirearmAsLoot();
                        }

                        if (!containerDist.isTrash && (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon || spawnItem.hasSharpness())
                            )
                         {
                            spawnItem.randomizeGeneralCondition();
                        }

                        if (container.getType().equals("freezer") && spawnItem instanceof Food food && food.isFreezing()) {
                            food.freeze();
                        }

                        if (spawnItem instanceof DrainableComboItem comboItem && spawnItem.hasTag(ItemTag.LESS_FULL) && Rand.Next(100) < 80) {
                            comboItem.randomizeUses();
                        } else if (spawnItem instanceof DrainableComboItem drainableComboItem && Rand.Next(100) < 40) {
                            drainableComboItem.randomizeUses();
                        }

                        if (isEmptyFluidContainer && spawnItem.hasComponent(ComponentType.FluidContainer)) {
                            spawnItem.getFluidContainer().Empty();
                        }

                        ItemContainer outside = bag.getOutermostContainer();
                        if (spawnItem instanceof Key key && spawnItem.hasTag(ItemTag.BUILDING_KEY) && outside != null && outside.getType() != null) {
                            key.takeKeyId();
                            BuildingDef def = null;
                            if (outside.getSquare() != null && outside.getSquare().getBuilding() != null && outside.getSquare().getBuilding().getDef() != null) {
                                def = outside.getSquare().getBuilding().getDef();
                            }

                            if (def != null) {
                                int keys = def.getKeySpawned();
                                int maxKeys = outside.getSquare().getBuilding().getRoomsNumber() / 5 + 1;
                                if (maxKeys < 2) {
                                    maxKeys = 2;
                                }

                                if (keys <= maxKeys && outside.getCountTagRecurse(ItemTag.BUILDING_KEY) <= 1) {
                                    def.setKeySpawned(keys + 1);
                                    ItemPickerJava.KeyNamer.nameKey(spawnItem, outside.getSquare());
                                } else {
                                    container.Remove(spawnItem);
                                }
                            } else {
                                container.Remove(spawnItem);
                            }
                        }

                        String mediaCat = spawnItem.getScriptItem().getRecordedMediaCat();
                        if (mediaCat != null) {
                            RecordedMedia recordedMedia = ZomboidRadio.getInstance().getRecordedMedia();
                            MediaData mediaData = recordedMedia.getRandomFromCategory(mediaCat);
                            if (mediaData == null) {
                                container.Remove(spawnItem);
                                if ("Home-VHS".equalsIgnoreCase(mediaCat)) {
                                    mediaData = recordedMedia.getRandomFromCategory("Retail-VHS");
                                    if (mediaData == null) {
                                        return;
                                    }

                                    spawnItem = ItemSpawner.spawnItem("Base.VHS_Retail", container);
                                    if (spawnItem == null) {
                                        return;
                                    }

                                    spawnItem.setRecordedMediaData(mediaData);
                                }

                                return;
                            }

                            spawnItem.setRecordedMediaData(mediaData);
                        }

                        if (spawnItem instanceof InventoryContainer inventoryContainer) {
                            if (containerDist.bags != null && !spawnItem.hasTag(ItemTag.BAGS_FILL_EXCEPTION)) {
                                rollContainerItemInternal(itemPickInfo, inventoryContainer, character, containerDist.bags);
                                LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), containerDist.bags);
                            } else if (containers.containsKey(spawnItem.getType())) {
                                ItemPickerJava.ItemPickerContainer itemPickerContainer = containers.get(spawnItem.getType());
                                rollContainerItemInternal(itemPickInfo, inventoryContainer, character, containers.get(spawnItem.getType()));
                                if (containers.get(spawnItem.getType()).junk != null) {
                                    rollContainerItemInternal(itemPickInfo, inventoryContainer, character, containers.get(spawnItem.getType()).junk, true);
                                }

                                if (spawnItem.hasTag(ItemTag.NEVER_EMPTY) && inventoryContainer.getItemContainer().isEmpty()) {
                                    container.Remove(spawnItem);
                                } else {
                                    LuaEventManager.triggerEvent("OnFillContainer", "Container", spawnItem.getType(), inventoryContainer.getItemContainer());
                                }
                            }
                        }

                        if (spawnItem instanceof Key && (spawnItem.getFullType().equals("Base.CarKey") || spawnItem.hasTag(ItemTag.CAR_KEY))) {
                            addVehicleKeyAsLoot(spawnItem, container);
                        }

                        if (!container.getType().equals("freezer")) {
                            spawnItem.setAutoAge();
                        }

                        if (containerDist.isTrash) {
                            trashItem(spawnItem);
                        }

                        if (containerDist.isWorn) {
                            wearDownItem(spawnItem);
                        }

                        if (spawnItem.hasTag(ItemTag.REGIONAL) && outside != null && outside.getSquare() != null) {
                            onCreateRegion(spawnItem, getSquareRegion(outside.getSquare()));
                        }

                        if (!StringUtils.isNullOrEmpty(spawnItem.getScriptItem().getSpawnWith()) && !containerDist.isWorn && !containerDist.isTrash) {
                            InventoryItem spawnWithItem = ItemSpawner.spawnItem(spawnItem.getScriptItem().getSpawnWith(), container);
                            if (spawnWithItem != null) {
                                spawnWithItem.copyClothing(spawnItem);
                            }
                        }

                        if (parent != null
                            && spawnItem.hasTag(ItemTag.APPLY_OWNER_NAME)
                            && parent instanceof IsoDeadBody deadBody
                            && deadBody.getDescriptor() != null) {
                            spawnItem.nameAfterDescriptor(deadBody.getDescriptor());
                        } else if (parent != null
                            && spawnItem.hasTag(ItemTag.MONOGRAM_OWNER_NAME)
                            && parent instanceof IsoDeadBody isoDeadBody
                            && isoDeadBody.getDescriptor() != null) {
                            spawnItem.monogramAfterDescriptor(isoDeadBody.getDescriptor());
                        }

                        if (spawnItem.hasTag(ItemTag.SPAWN_FULL_UNLESS_LAUNDRY)) {
                            spawnItem.setCurrentUses(spawnItem.getMaxUses());
                        }

                        itemSpawnSanityCheck(spawnItem);
                        if (containerDist.onlyOne) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void DoWeaponUpgrade(InventoryItem item) {
        ItemPickerJava.ItemPickerUpgradeWeapons itemPickerUpgradeWeapons = WeaponUpgradeMap.get(item.getType());
        if (itemPickerUpgradeWeapons != null) {
            if (!itemPickerUpgradeWeapons.upgrades.isEmpty()) {
                int randUpgrade = Rand.Next(itemPickerUpgradeWeapons.upgrades.size());

                for (int x = 0; x < randUpgrade; x++) {
                    String s = PZArrayUtil.pickRandom(itemPickerUpgradeWeapons.upgrades);
                    InventoryItem part = InventoryItemFactory.CreateItem(s);
                    if (part != null) {
                        ((HandWeapon)item).attachWeaponPart((WeaponPart)part);
                    }
                }
            }
        }
    }

    public static float getLootModifier(String itemname) {
        Item item = ScriptManager.instance.FindItem(itemname);
        if (item == null) {
            return 0.6F;
        } else if (!SandboxOptions.instance.lootItemRemovalListContains(itemname)
            && !SandboxOptions.instance.lootItemRemovalListContains(ScriptManager.getItemName(itemname))) {
            String lootType = getLootType(item);
            return getLootModifierFromType(lootType);
        } else {
            return 0.0F;
        }
    }

    public static float getLootModifierFromType(String lootType) {
        float lootModifier = otherLootModifier;
        if (Objects.equals(lootType, "Generator")) {
            return doSandboxSettings(SandboxOptions.instance.generatorSpawning.getValue());
        } else if (Objects.equals(lootType, "Memento")) {
            return mementoLootModifier;
        } else if (Objects.equals(lootType, "Medical")) {
            return medicalLootModifier;
        } else if (Objects.equals(lootType, "Mechanics")) {
            return mechanicsLootModifier;
        } else if (Objects.equals(lootType, "Material")) {
            return materialLootModifier;
        } else if (Objects.equals(lootType, "Farming")) {
            return farmingLootModifier;
        } else if (Objects.equals(lootType, "Tool")) {
            return toolLootModifier;
        } else if (Objects.equals(lootType, "Cookware")) {
            return cookwareLootModifier;
        } else if (Objects.equals(lootType, "SurvivalGears")) {
            return survivalGearsLootModifier;
        } else if (Objects.equals(lootType, "CannedFood")) {
            return cannedFoodLootModifier;
        } else if (Objects.equals(lootType, "Food")) {
            return foodLootModifier;
        } else if (Objects.equals(lootType, "Ammo")) {
            return ammoLootModifier;
        } else if (Objects.equals(lootType, "Weapon")) {
            return weaponLootModifier;
        } else if (Objects.equals(lootType, "RangedWeapon")) {
            return rangedWeaponLootModifier;
        } else if (Objects.equals(lootType, "Key")) {
            return keyLootModifier;
        } else if (Objects.equals(lootType, "Container")) {
            return containerLootModifier;
        } else if (Objects.equals(lootType, "Literature")) {
            return literatureLootModifier;
        } else if (Objects.equals(lootType, "Clothing")) {
            return clothingLootModifier;
        } else {
            return Objects.equals(lootType, "Media") ? mediaLootModifier : lootModifier;
        }
    }

    public static String getLootType(Item item) {
        if (Objects.equals(item.getName(), "Generator") || Objects.equals(item.getFullName(), "Base.Generator") || item.hasTag(ItemTag.GENERATOR)) {
            return "Generator";
        } else if (item.isMementoLoot()) {
            return "Memento";
        } else if (item.isMedicalLoot()) {
            return "Medical";
        } else if (item.isMechanicsLoot()) {
            return "Mechanics";
        } else if (item.isMaterialLoot()) {
            return "Material";
        } else if (item.isFarmingLoot()) {
            return "Farming";
        } else if (item.isToolLoot()) {
            return "Tool";
        } else if (item.isCookwareLoot()) {
            return "Cookware";
        } else if (item.isSurvivalGearLoot()) {
            return "SurvivalGears";
        } else if (!item.isItemType(ItemType.FOOD) && !"Food".equals(item.getDisplayCategory())) {
            if (!"Ammo".equals(item.getDisplayCategory())
                && !item.hasTag(ItemTag.AMMO_CASE)
                && (!item.isItemType(ItemType.NORMAL) || item.getAmmoType() == null)) {
                if (item.isItemType(ItemType.WEAPON) && !item.isRanged()) {
                    return "Weapon";
                } else if (item.isItemType(ItemType.WEAPON_PART) || item.isItemType(ItemType.WEAPON) && item.isRanged() || item.hasTag(ItemTag.FIREARM_LOOT)) {
                    return "RangedWeapon";
                } else if (item.isItemType(ItemType.KEY) || item.isItemType(ItemType.KEY_RING) || item.hasTag(ItemTag.KEY_RING)) {
                    return "Key";
                } else if (item.capacity > 0 || item.isItemType(ItemType.CONTAINER) || "Bag".equals(item.getDisplayCategory())) {
                    return "Container";
                } else if (item.isItemType(ItemType.LITERATURE)) {
                    return "Literature";
                } else if (item.isItemType(ItemType.CLOTHING)) {
                    return "Clothing";
                } else {
                    return item.getRecordedMediaCat() != null ? "Media" : "Other";
                }
            } else {
                return "Ammo";
            }
        } else {
            return !item.cannedFood && item.getDaysFresh() != 1000000000 && item.isItemType(ItemType.FOOD) ? "Food" : "CannedFood";
        }
    }

    public static void updateOverlaySprite(IsoObject obj) {
        ContainerOverlays.instance.updateContainerOverlaySprite(obj);
    }

    public static void doOverlaySprite(IsoGridSquare sq) {
        if (!GameClient.client) {
            if (sq != null && sq.getRoom() != null && !sq.isOverlayDone()) {
                PZArrayList<IsoObject> objects = sq.getObjects();

                for (int i = 0; i < objects.size(); i++) {
                    IsoObject obj = objects.get(i);
                    if (obj != null && obj.getContainer() != null && !obj.getContainer().isExplored()) {
                        fillContainer(obj.getContainer(), IsoPlayer.getInstance());
                        obj.getContainer().setExplored(true);
                        if (GameServer.server) {
                            LuaManager.GlobalObject.sendItemsInContainer(obj, obj.getContainer());
                        }
                    }

                    updateOverlaySprite(obj);
                }

                sq.setOverlayDone(true);
            }
        }
    }

    public static ItemPickerJava.ItemPickerContainer getItemContainer(String room, String container, String proceduralName, boolean junk) {
        ItemPickerJava.ItemPickerRoom iproom = rooms.get(room);
        if (iproom == null) {
            return null;
        } else {
            ItemPickerJava.ItemPickerContainer ipcont = iproom.containers.get(container);
            if (ipcont != null && ipcont.procedural) {
                ArrayList<ItemPickerJava.ProceduralItem> procList = ipcont.proceduralItems;

                for (int i = 0; i < procList.size(); i++) {
                    ItemPickerJava.ProceduralItem proceduralItem = procList.get(i);
                    if (proceduralName.equals(proceduralItem.name)) {
                        ItemPickerJava.ItemPickerContainer containerDistToSpawn = ProceduralDistributions.get(proceduralName);
                        if (containerDistToSpawn.junk != null && junk) {
                            return containerDistToSpawn.junk;
                        }

                        if (!junk) {
                            return containerDistToSpawn;
                        }
                    }
                }
            }

            return junk && ipcont != null ? ipcont.junk : ipcont;
        }
    }

    public static void keyNamerBuilding(InventoryItem item, IsoGridSquare square) {
        ItemPickerJava.KeyNamer.nameKey(item, square);
    }

    public static void trashItem(InventoryItem spawnItem) {
        if (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon handWeapon && handWeapon.getPhysicsObject() == null) {
            if (Rand.Next(100) < 95) {
                spawnItem.setConditionNoSound(1);
            } else {
                spawnItem.setConditionNoSound(1);
            }
        }

        if (spawnItem.hasHeadCondition()) {
            if (Rand.NextBool(2)) {
                spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
            } else {
                spawnItem.setHeadCondition(1);
            }
        }

        if (spawnItem.hasSharpness()) {
            if (Rand.NextBool(2)) {
                spawnItem.setSharpness(Rand.Next(0.0F, spawnItem.getMaxSharpness()));
            } else {
                spawnItem.setSharpness(0.0F);
            }
        }

        if (spawnItem instanceof DrainableComboItem drainableComboItem) {
            if (Rand.Next(100) < 90) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }

        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food food && (!scriptItem.cannedFood || !scriptItem.cantEat)) {
            boolean burnt = false;
            boolean gross = false;
            if (!food.hasTag(ItemTag.VERMIN) && food.isCookable() && !scriptItem.cannedFood && food.getReplaceOnCooked() == null && Rand.Next(100) < 75) {
                if (Rand.Next(100) < 50) {
                    food.setCooked(true);
                } else {
                    burnt = true;
                    food.setBurnt(true);
                }
            }

            if (!food.isRotten() && food.getOffAgeMax() < 1000000000 && Rand.Next(100) < 95) {
                food.setRotten(true);
                food.setAge(food.getOffAgeMax());
                gross = true;
            } else if (food.isFresh() && food.getOffAge() < 1000000000 && Rand.Next(100) < 95) {
                food.setAge(food.getOffAge());
                gross = true;
            }

            if (gross && Rand.Next(2) == 0) {
                gross = false;
            }

            if (food.isbDangerousUncooked() && !food.isCooked()) {
                gross = true;
            }

            if (food.hasTag(ItemTag.VERMIN)) {
                gross = true;
            }

            double baseHunger = food.getBaseHunger() * 100.0F * -1.0F + 0.1;
            double hungerChange = food.getHungerChange() * 100.0F * -1.0F + 0.1;
            if (hungerChange < baseHunger) {
                baseHunger = hungerChange;
            }

            if (!burnt && !gross && Rand.Next(100) != 0) {
                if (baseHunger >= 4.0) {
                    int roll = Rand.Next(8);
                    if (roll == 0) {
                        food.multiplyFoodValues(0.75F);
                    } else if (roll <= 2) {
                        food.multiplyFoodValues(0.5F);
                    } else {
                        food.multiplyFoodValues(0.25F);
                    }
                } else if (baseHunger >= 2.0) {
                    food.multiplyFoodValues(0.5F);
                }
            }
        }

        if (spawnItem instanceof Clothing clothing) {
            clothing.randomizeCondition(25, 95, 10, 75);
        }
    }

    public static void trashItemLooted(InventoryItem spawnItem) {
        if ((spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon handWeapon && handWeapon.getPhysicsObject() == null)
            && Rand.Next(100) < 50) {
            spawnItem.setCondition(1, false);
        }

        if (spawnItem.hasHeadCondition()) {
            spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
        }

        if (spawnItem.hasSharpness()) {
            spawnItem.setSharpness(Rand.Next(0.0F, spawnItem.getMaxSharpness()));
        }

        if (spawnItem instanceof DrainableComboItem drainableComboItem) {
            if (Rand.Next(100) < 75) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }

        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food food && (!scriptItem.cannedFood || !scriptItem.cantEat)) {
            boolean gross = food.isbDangerousUncooked() && !food.isCooked();
            if (food.hasTag(ItemTag.VERMIN)) {
                gross = true;
            }

            double baseHunger = food.getBaseHunger() * 100.0F * -1.0F + 0.1;
            double hungerChange = food.getHungerChange() * 100.0F * -1.0F + 0.1;
            if (hungerChange < baseHunger) {
                baseHunger = hungerChange;
            }

            if (!gross && Rand.Next(100) < 75) {
                if (baseHunger >= 4.0) {
                    int roll = Rand.Next(8);
                    if (roll == 0) {
                        food.multiplyFoodValues(0.75F);
                    } else if (roll <= 2) {
                        food.multiplyFoodValues(0.5F);
                    } else {
                        food.multiplyFoodValues(0.25F);
                    }
                } else if (baseHunger >= 2.0) {
                    food.multiplyFoodValues(0.5F);
                }
            }
        }

        if (spawnItem instanceof Clothing clothing) {
            clothing.randomizeCondition(10, 50, 10, 50);
        }
    }

    public static void trashItemRats(InventoryItem spawnItem) {
        if ((spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon handWeapon && handWeapon.getPhysicsObject() == null)
            && Rand.Next(100) < 75) {
            wearDownItem(spawnItem);
        }

        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food food && (!scriptItem.cannedFood || !scriptItem.cantEat)) {
            double baseHunger = food.getBaseHunger() * 100.0F * -1.0F + 0.1;
            double hungerChange = food.getHungerChange() * 100.0F * -1.0F + 0.1;
            if (hungerChange < baseHunger) {
                baseHunger = hungerChange;
            }

            if (baseHunger >= 4.0) {
                int roll = Rand.Next(8);
                if (roll == 0) {
                    food.multiplyFoodValues(0.75F);
                } else if (roll <= 2) {
                    food.multiplyFoodValues(0.5F);
                } else {
                    food.multiplyFoodValues(0.25F);
                }
            } else if (baseHunger >= 2.0) {
                food.multiplyFoodValues(0.5F);
            }
        }

        if (spawnItem instanceof Clothing clothing) {
            clothing.randomizeCondition(25, 95, 10, 95);
        }
    }

    public static void wearDownItem(InventoryItem spawnItem) {
        if (spawnItem.hasTag(ItemTag.SHOW_CONDITION) || spawnItem instanceof HandWeapon handWeapon && handWeapon.getPhysicsObject() == null) {
            if (Rand.Next(100) < 25) {
                spawnItem.setCondition(1, false);
            } else {
                int roll1 = Rand.Next(spawnItem.getConditionMax());
                int roll2 = Rand.Next(spawnItem.getConditionMax());
                int roll3 = Rand.Next(spawnItem.getConditionMax());
                if (roll2 < roll1) {
                    roll1 = roll2;
                }

                if (roll3 < roll1) {
                    roll1 = roll3;
                }

                spawnItem.setCondition(roll1 + 1, false);
            }
        }

        if (spawnItem.hasHeadCondition()) {
            spawnItem.setHeadCondition(Rand.Next(1, spawnItem.getHeadConditionMax()));
        }

        if (spawnItem.hasSharpness()) {
            spawnItem.setSharpness(Rand.Next(0.0F, spawnItem.getMaxSharpness()));
        }

        if (spawnItem instanceof DrainableComboItem drainableComboItem) {
            int maxUses = spawnItem.getMaxUses();
            maxUses--;
            if (Rand.Next(100) < 75) {
                spawnItem.setCurrentUses(1);
            } else {
                drainableComboItem.randomizeUses();
            }
        }

        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food food && (!scriptItem.cannedFood || !scriptItem.cantEat)) {
            boolean burnt = false;
            boolean gross = false;
            if (!food.hasTag(ItemTag.VERMIN) && food.isCookable() && food.getReplaceOnCooked() == null && Rand.Next(100) < 50) {
                if (Rand.Next(100) < 50) {
                    food.setCooked(true);
                } else {
                    burnt = true;
                    food.setBurnt(true);
                }
            }

            if (!food.isRotten() && food.getOffAgeMax() < 1000000000 && Rand.Next(100) < 75) {
                food.setRotten(true);
                food.setAge(food.getOffAgeMax());
                gross = true;
            } else if (food.isFresh() && food.getOffAge() < 1000000000 && Rand.Next(100) < 75) {
                food.setAge(food.getOffAge());
                gross = true;
            }

            if (gross && Rand.Next(2) == 0) {
                gross = false;
            }

            if (food.isbDangerousUncooked() && !food.isCooked()) {
                gross = true;
            }

            if (food.hasTag(ItemTag.VERMIN)) {
                gross = true;
            }

            double baseHunger = food.getBaseHunger() * 100.0F * -1.0F + 0.1;
            double hungerChange = food.getHungerChange() * 100.0F * -1.0F + 0.1;
            if (hungerChange < baseHunger) {
                baseHunger = hungerChange;
            }

            if (!burnt && !gross && Rand.Next(100) < 75) {
                if (baseHunger >= 4.0) {
                    int roll = Rand.Next(8);
                    if (roll <= 2) {
                        food.multiplyFoodValues(0.75F);
                    } else if (roll <= 4) {
                        food.multiplyFoodValues(0.5F);
                    } else {
                        food.multiplyFoodValues(0.25F);
                    }
                } else if (baseHunger >= 2.0) {
                    food.multiplyFoodValues(0.5F);
                }
            }
        }

        if (spawnItem instanceof Clothing clothing) {
            clothing.randomizeCondition(0, 25, 1, 25);
        }
    }

    public static void rotItem(InventoryItem spawnItem) {
        Item scriptItem = spawnItem.getScriptItem();
        if (spawnItem instanceof Food food && (!scriptItem.cannedFood || !scriptItem.cantEat) && food.getOffAgeMax() < 1000000000) {
            if (food.isRotten()) {
                return;
            }

            if (Rand.Next(100) < 75) {
                food.setRotten(true);
                food.setAge(food.getOffAgeMax());
            } else if (food.isFresh() && Rand.Next(100) < 95) {
                food.setAge(food.getOffAge());
            }
        }

        if (spawnItem instanceof Clothing clothing) {
            clothing.randomizeCondition(0, 75, 1, 25);
        }
    }

    public static void spawnLootCarKey(InventoryItem spawnItem, ItemContainer container) {
        spawnLootCarKey(spawnItem, container, container);
    }

    public static void spawnLootCarKey(InventoryItem spawnItem, ItemContainer container, ItemContainer outtermost) {
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        if (vehicles.isEmpty()) {
            container.Remove(spawnItem);
        } else {
            BaseVehicle vehicle = vehicles.get(Rand.Next(vehicles.size()));
            if (vehicle != null && !vehicle.isPreviouslyMoved() && isGoodKey(vehicle.getScriptName())) {
                Key key = (Key)spawnItem;
                key.setKeyId(vehicle.getKeyId());
                vehicle.setPreviouslyMoved(true);
                vehicle.keySpawned = 1;
                BaseVehicle.keyNamerVehicle(key, vehicle);
                Color newC = Color.HSBtoRGB(vehicle.colorHue, vehicle.colorSaturation * 0.5F, vehicle.colorValue);
                key.setColor(newC);
                key.setCustomColor(true);
                if (Rand.Next(100) < 1.0F * keyLootModifierD100
                    && outtermost.getSourceGrid() != null
                    && outtermost.getSourceGrid().getBuilding() != null
                    && outtermost.getSourceGrid().getBuilding().getDef() != null) {
                    vehicle.addBuildingKeyToGloveBox(outtermost.getSourceGrid());
                }
            } else {
                container.Remove(spawnItem);
            }
        }
    }

    public static boolean isGoodKey(String vehicleType) {
        return !vehicleType.contains("Burnt")
            && !vehicleType.contains("Smashed")
            && !vehicleType.equals("TrailerAdvert")
            && !vehicleType.equals("TrailerCover")
            && !vehicleType.equals("Trailer");
    }

    public static boolean addVehicleKeyAsLoot(InventoryItem spawnItem, ItemContainer container) {
        if (container.getCountTagRecurse(ItemTag.CAR_KEY) < 2) {
            spawnLootCarKey(spawnItem, container);
            return true;
        } else {
            container.Remove(spawnItem);
            return false;
        }
    }

    public static boolean containerHasZone(ItemContainer container, String zone) {
        return squareHasZone(container.getSourceGrid(), zone);
    }

    public static boolean squareHasZone(IsoGridSquare square, String zone) {
        ArrayList<Zone> metazones = IsoWorld.instance.metaGrid.getZonesAt(square.x, square.y, 0);

        for (int i = 0; i < metazones.size(); i++) {
            if (Objects.equals(metazones.get(i).name, zone) || Objects.equals(metazones.get(i).type, zone)) {
                return true;
            }
        }

        return false;
    }

    public static String getContainerZombiesType(ItemContainer container) {
        if (container != null && container.getSourceGrid() != null) {
            String stringThing = getSquareZombiesType(container.getSourceGrid());
            return ItemPickerJava.KeyNamer.badZones.contains(stringThing) ? null : stringThing;
        } else {
            return null;
        }
    }

    public static String getSquareZombiesType(IsoGridSquare square) {
        return square.getSquareZombiesType();
    }

    public static String getSquareBuildingName(IsoGridSquare square) {
        ArrayList<Zone> zones = LuaManager.GlobalObject.getZones(square.x, square.y, 0);

        for (int i = 0; i < zones.size(); i++) {
            if (Objects.equals(zones.get(i).type, "BuildingName") && !ItemPickerJava.KeyNamer.badZones.contains(zones.get(i).name)) {
                return zones.get(i).name;
            }
        }

        return null;
    }

    public static String getSquareRegion(IsoGridSquare square) {
        return square.getSquareRegion();
    }

    public static float getBaseChance(ItemPickerJava.ItemPickerItem item, IsoGameCharacter character, boolean isJunk) {
        String itemName = item.itemName;
        Item scriptItem = ScriptManager.instance.FindItem(itemName);
        return item.chance * getBaseChanceMultiplier(character, isJunk, scriptItem);
    }

    public static float getBaseChanceMultiplier(IsoGameCharacter character, boolean isJunk, Item scriptItem) {
        float multiplier = 1.0F;
        if (isJunk) {
            multiplier *= 1.4F;
        }

        if (scriptItem != null
            && scriptItem.hasTag(ItemTag.MORE_WHEN_NO_ZOMBIES)
            && (SandboxOptions.instance.zombies.getValue() == 6 || SandboxOptions.instance.zombieConfig.populationMultiplier.getValue() == 0.0)) {
            multiplier *= 2.0F;
        }

        return multiplier;
    }

    public static float getLootModifier(String itemName, boolean isJunk) {
        float lootModifier = getLootModifier(itemName);
        if (isJunk && lootModifier > 0.0F) {
            lootModifier = 1.0F;
        }

        return lootModifier;
    }

    public static float getAdjustedZombieDensity(float zombieDensity, Item scriptItem, boolean isJunk) {
        return !isJunk && (scriptItem == null || !scriptItem.ignoreZombieDensity()) ? zombieDensity : 0.0F;
    }

    public static float getActualSpawnChance(
        ItemPickerJava.ItemPickerItem item, IsoGameCharacter character, ItemContainer container, float zombieDensity, boolean isJunk
    ) {
        String itemName = item.itemName;
        Item scriptItem = ScriptManager.instance.FindItem(itemName);
        float lootModifier = getLootModifier(itemName, isJunk);
        float baseChance = getBaseChance(item, character, isJunk);
        zombieDensity = getAdjustedZombieDensity(zombieDensity, scriptItem, isJunk);
        float overTimeModifier;
        if (container.getSourceGrid() != null) {
            overTimeModifier = SandboxOptions.instance.getCurrentLootMultiplier(container.getSourceGrid());
        } else {
            overTimeModifier = SandboxOptions.instance.getCurrentLootMultiplier();
        }

        return (baseChance * 100.0F * lootModifier + zombieDensity) * overTimeModifier;
    }

    public static float getZombieDensityFactor(ItemPickerJava.ItemPickerContainer containerDist, ItemContainer container) {
        float zombieDensity = 0.0F;
        if (!containerDist.ignoreZombieDensity && IsoWorld.instance != null && SandboxOptions.instance.zombiePopLootEffect.getValue() != 0) {
            IsoMetaChunk chunk = null;
            if (player != null) {
                chunk = IsoWorld.instance.getMetaChunk(PZMath.fastfloor(player.getX() / 8.0F), PZMath.fastfloor(player.getY() / 8.0F));
            } else if (container.getSourceGrid() != null) {
                chunk = IsoWorld.instance
                    .getMetaChunk(PZMath.fastfloor(container.getSourceGrid().getX() / 8.0F), PZMath.fastfloor(container.getSourceGrid().getY() / 8.0F));
            }

            if (chunk != null) {
                zombieDensity = chunk.getLootZombieIntensity();
            }

            zombieDensity = Math.min(zombieDensity, zombieDensityCap);
            return zombieDensity * SandboxOptions.instance.zombiePopLootEffect.getValue();
        } else {
            return zombieDensity;
        }
    }

    public static void itemSpawnSanityCheck(InventoryItem spawnItem) {
        itemSpawnSanityCheck(spawnItem, null);
    }

    public static void itemSpawnSanityCheck(InventoryItem spawnItem, ItemContainer container) {
        if (container != null
            && container.isShop()
            && spawnItem instanceof InventoryContainer inventoryContainer
            && !spawnItem.hasTag(ItemTag.ALWAYS_HAS_STUFF)) {
            inventoryContainer.getItemContainer().getItems().clear();
        }

        if (spawnItem instanceof InventoryContainer inventoryContainer) {
            inventoryContainer.getItemContainer().setExplored(true);
        }

        if (spawnItem instanceof Food && spawnItem.hasTag(ItemTag.SPAWN_COOKED)) {
            spawnItem.setCooked(true);
            if (!StringUtils.isNullOrEmpty(((Food)spawnItem).getOnCooked())) {
                Object functionObj = LuaManager.getFunctionObject(((Food)spawnItem).getOnCooked());
                if (functionObj != null) {
                    LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, spawnItem);
                }
            }
        }

        spawnItem.unsealIfNotFull();
        if (spawnItem instanceof DrainableComboItem && !spawnItem.isKeepOnDeplete() && spawnItem.getCurrentUses() <= 0) {
            spawnItem.setCurrentUses(1);
        }
    }

    public static String getLootDebugString(IsoObject object) {
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue()
            && object != null
            && object.getContainer() != null
            && object.getContainer().getType() != null
            && object.getSquare() != null
            && object.getSquare().getRoom() != null
            && object.getSquare().getRoom().getRoomDef() != null) {
            String roomdef = object.getSquare().getRoom().getRoomDef().getName();
            if (!hasDistributionForRoom(roomdef)) {
                return "No loot distro for " + roomdef;
            } else {
                String containerType = object.getContainer().getType();
                if (!hasDistributionForContainerInRoom(containerType, roomdef)) {
                    ItemPickerJava.ItemPickerRoom roomDist = rooms.get(roomdef);
                    if (roomDist.containers.get("other") != null) {
                        return "'other' container distro used for " + containerType + " in " + roomdef;
                    } else if (roomDist.containers.containsKey("all")) {
                        return "'all' container distro used for " + containerType + " in " + roomdef;
                    } else {
                        roomDist = rooms.get("all");
                        return roomDist.containers.containsKey(containerType)
                            ? "'all' roomdef distro used for " + containerType + " in " + roomdef
                            : "No loot distro for " + containerType + " in " + roomdef;
                    }
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static boolean hasDistributionForRoom(String roomdef) {
        return roomdef != null && rooms.containsKey(roomdef);
    }

    public static boolean hasDistributionForContainerInRoom(String containerType, String roomdef) {
        if (roomdef == null || containerType == null) {
            return false;
        } else if (!rooms.containsKey(roomdef)) {
            return false;
        } else {
            ItemPickerJava.ItemPickerRoom roomDist2 = rooms.get(roomdef);
            return roomDist2.containers.containsKey(containerType);
        }
    }

    public static void onCreateRegion(InventoryItem item, String region) {
        List<Newspaper> regional_papers = ItemGenerationConstants.REGIONAL_PAPERS.get(region);
        if (regional_papers == null) {
            regional_papers = ItemGenerationConstants.REGIONAL_PAPERS.get("General");
        }

        RecipeCodeHelper.nameNewspaper(item, Rand.Next(regional_papers));
    }

    public static final class ItemPickerContainer {
        public ItemPickerJava.ItemPickerItem[] items = new ItemPickerJava.ItemPickerItem[0];
        public float rolls;
        public boolean noAutoAge;
        public boolean isShop;
        public int fillRand;
        public int maxMap;
        public int stashChance;
        public ItemPickerJava.ItemPickerContainer junk;
        public ItemPickerJava.ItemPickerContainer bags;
        public boolean procedural;
        public boolean dontSpawnAmmo;
        public boolean ignoreZombieDensity;
        public boolean cookFood;
        public boolean canBurn;
        public boolean isTrash;
        public boolean isWorn;
        public boolean isRotten;
        public boolean onlyOne;
        public boolean defaultInventoryLoot = true;
        public ArrayList<ItemPickerJava.ProceduralItem> proceduralItems;

        void compact() {
            if (this.proceduralItems != null) {
                this.proceduralItems.trimToSize();
            }

            if (this.junk != null) {
                this.junk.compact();
            }

            if (this.bags != null) {
                this.bags.compact();
            }
        }
    }

    public static final class ItemPickerItem {
        public String itemName;
        public float chance;
    }

    public static final class ItemPickerRoom {
        public THashMap<String, ItemPickerJava.ItemPickerContainer> containers = new THashMap<>();
        public int fillRand;
        public boolean isShop;
        public String specificId;
        public int professionChance;
        public String outfit;
        public String outfitFemale;
        public String outfitMale;
        public String outfitChance;
        public String vehicle;
        public List<String> vehicles;
        public String vehicleChance;
        public String vehicleDistribution;
        public Integer vehicleSkin;
        public String femaleChance;
        public String roomTypes;
        public String zoneRequires;
        public String zoneDisallows;
        public String containerChance;
        public String femaleOdds;
        public String bagType;
        public String bagTable;
        public int professionChanceInt;

        void compact() {
            for (ItemPickerJava.ItemPickerContainer container : this.containers.values()) {
                container.compact();
            }

            this.containers.trimToSize();
        }
    }

    public static final class ItemPickerUpgradeWeapons {
        public String name;
        public ArrayList<String> upgrades = new ArrayList<>();
    }

    @UsedFromLua
    public static final class KeyNamer {
        @UsedFromLua
        public static ArrayList<String> badZones = new ArrayList<>();
        @UsedFromLua
        public static ArrayList<String> bigBuildingRooms = new ArrayList<>();
        @UsedFromLua
        public static ArrayList<String> restaurantSubstrings = new ArrayList<>();
        @UsedFromLua
        public static ArrayList<String> restaurants = new ArrayList<>();
        @UsedFromLua
        public static ArrayList<String> roomSubstrings = new ArrayList<>();
        @UsedFromLua
        public static ArrayList<String> rooms = new ArrayList<>();

        public static void clear() {
            badZones.clear();
            bigBuildingRooms.clear();
            restaurantSubstrings.clear();
            restaurants.clear();
            roomSubstrings.clear();
            rooms.clear();
        }

        public static void nameKey(InventoryItem item, IsoGridSquare square) {
            if (item != null && square != null) {
                item.setOrigin(square);
            }

            String keyName = getName(square);
            if (keyName != null) {
                keyName = keyName + "Key";
                if (Translator.getTextOrNull("IGUI_" + keyName) != null) {
                    keyName = Translator.getText("IGUI_" + keyName);
                    item.setName(Translator.getText(item.getDisplayName()) + " - " + keyName);
                }
            }
        }

        public static String getName(IsoGridSquare square) {
            if (square != null && square.getBuilding() != null) {
                IsoBuilding building = square.getBuilding();
                String zone = ItemPickerJava.getSquareZombiesType(square);
                if (badZones.contains(zone)) {
                    zone = null;
                }

                String keyName = null;
                if (ItemPickerJava.getSquareBuildingName(square) != null) {
                    return ItemPickerJava.getSquareBuildingName(square);
                } else if (building.containsRoom("bedroom") && building.containsRoom("livingroom") && building.containsRoom("kitchen")) {
                    return "Residential";
                } else {
                    if (zone != null) {
                        switch (zone) {
                            case "Prison":
                                keyName = "Prison";
                                break;
                            case "Police":
                                keyName = "Police";
                                break;
                            case "Army":
                                keyName = "Army";
                        }
                    }

                    if (badZones.contains(keyName)) {
                        keyName = null;
                    }

                    if (keyName != null) {
                        return keyName;
                    } else {
                        for (String buildingName : bigBuildingRooms) {
                            if (buildingName.equals("storageunit") && building.containsRoom("bedroom")) {
                                break;
                            }

                            if (building.containsRoom(buildingName)) {
                                return buildingName;
                            }
                        }

                        if (ItemPickerJava.getSquareZombiesType(square) != null && !badZones.contains(ItemPickerJava.getSquareZombiesType(square))) {
                            String testName = ItemPickerJava.getSquareZombiesType(square);
                            if (testName != null) {
                                String keyNameTest = testName + "Key";
                                if (Translator.getTextOrNull("IGUI_" + keyNameTest) != null) {
                                    return testName;
                                }
                            }
                        }

                        if (square.getRoom() != null && square.getRoom().getRoomDef() != null) {
                            String roomDef = square.getRoom().getRoomDef().getName();
                            if (rooms.contains(roomDef)) {
                                return roomDef;
                            }

                            for (String name : roomSubstrings) {
                                if (roomDef.contains(name)) {
                                    return name;
                                }
                            }

                            if (restaurants.contains(roomDef)) {
                                return roomDef;
                            }

                            for (String namex : restaurantSubstrings) {
                                if (roomDef.contains(namex)) {
                                    return namex;
                                }
                            }
                        }

                        if (ItemPickerJava.squareHasZone(square, "TrailerPark") && building.containsRoom("bedroom")) {
                            return "TrailerPark";
                        } else if (ItemPickerJava.squareHasZone(square, "Ranch") && building.containsRoom("bedroom")) {
                            return "Ranch";
                        } else if (ItemPickerJava.squareHasZone(square, "Forest")) {
                            return "Forest";
                        } else {
                            return ItemPickerJava.squareHasZone(square, "DeepForest") ? "DeepForest" : null;
                        }
                    }
                }
            } else {
                return null;
            }
        }
    }

    public static final class ProceduralItem {
        public String name;
        public int min;
        public int max;
        public List<String> forceForItems;
        public List<String> forceForZones;
        public List<String> forceForTiles;
        public List<String> forceForRooms;
        public int weightChance;
    }

    public static final class VehicleDistribution {
        public ItemPickerJava.ItemPickerRoom normal;
        public final ArrayList<ItemPickerJava.ItemPickerRoom> specific = new ArrayList<>();

        void compact() {
            if (this.normal != null) {
                this.normal.compact();
            }

            for (int i = 0; i < this.specific.size(); i++) {
                ItemPickerJava.ItemPickerRoom room = this.specific.get(i);
                room.compact();
            }

            this.specific.trimToSize();
        }
    }
}
