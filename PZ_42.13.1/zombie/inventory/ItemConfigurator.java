// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.HashMap;
import java.util.Map.Entry;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.scripting.ScriptManager;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;

public class ItemConfigurator {
    private static final String[] vehicle_containers = new String[]{
        "TruckBed", "TruckBedOpen", "SeatFrontLeft", "SeatFrontRight", "SeatMiddleLeft", "SeatMiddleRight", "SeatRearLeft", "SeatRearRight", "GloveBox"
    };
    private static final boolean verbose = false;
    private static final boolean verbose_tiles = false;
    private static int nextId;
    private static final HashMap<String, ItemConfigurator.IntegerStore> STRING_INTEGER_HASH_MAP = new HashMap<>();
    private static final HashMap<String, ItemConfigurator.IntegerStore> TILE_INTEGER_HASH_MAP = new HashMap<>();

    private static boolean registerString(String s) {
        if (s != null && !STRING_INTEGER_HASH_MAP.containsKey(s)) {
            STRING_INTEGER_HASH_MAP.put(s, new ItemConfigurator.IntegerStore(nextId++));
            return true;
        } else {
            return false;
        }
    }

    public static boolean registerZone(String s) {
        return registerString(s);
    }

    public static int GetIdForString(String s) {
        ItemConfigurator.IntegerStore store = STRING_INTEGER_HASH_MAP.get(s);
        return store != null ? store.get() : -1;
    }

    public static int GetIdForSprite(String s) {
        ItemConfigurator.IntegerStore store = TILE_INTEGER_HASH_MAP.get(s);
        return store != null ? store.get() : -1;
    }

    public static void Preprocess() {
        STRING_INTEGER_HASH_MAP.clear();
        TILE_INTEGER_HASH_MAP.clear();

        for (Zone zone : IsoWorld.instance.metaGrid.zones) {
            if (!StringUtils.isNullOrWhitespace(zone.name) && registerString(zone.name)) {
            }

            if (!StringUtils.isNullOrWhitespace(zone.type) && registerString(zone.type)) {
            }
        }

        for (BuildingDef buildingDef : IsoWorld.instance.metaGrid.buildings) {
            for (RoomDef roomDef : buildingDef.rooms) {
                if (registerString(roomDef.getName())) {
                }
            }
        }

        for (Item item : ScriptManager.instance.getAllItems()) {
            if (item.isItemType(ItemType.CONTAINER) && registerString(item.getName())) {
            }
        }

        for (String vehicle_container : vehicle_containers) {
            if (registerString(vehicle_container)) {
            }
        }

        for (VehicleScript vehicle : ScriptManager.instance.getAllVehicleScripts()) {
            if (vehicle.getName() != null && registerString(vehicle.getName())) {
            }
        }

        registerString("freezer");

        for (Entry<String, IsoSprite> entry : IsoSpriteManager.instance.namedMap.entrySet()) {
            if (entry.getValue().getProperties() != null
                && entry.getValue().getProperties().has(IsoFlagType.container)
                && registerString(entry.getValue().getProperties().get("container"))) {
            }
        }

        for (IsoSprite sprite : IsoSpriteManager.instance.intMap.valueCollection()) {
            if (sprite != null && sprite.getID() >= 0 && sprite.getName() != null) {
                TILE_INTEGER_HASH_MAP.put(sprite.getName(), new ItemConfigurator.IntegerStore(sprite.getID()));
            }
        }

        for (ItemConfig itemConfig : ScriptManager.instance.getAllItemConfigs()) {
            itemConfig.BuildBuckets();
        }
    }

    public static void ConfigureItem(InventoryItem item, ItemPickInfo pickInfo, boolean isJunk, float zombieDensity) {
        if (item != null && pickInfo != null && item.getScriptItem() != null && item.getScriptItem().getItemConfig() != null) {
            pickInfo.setJunk(isJunk);
            Item itemScript = item.getScriptItem();
            ItemConfig itemConfig = itemScript.getItemConfig();
            if (itemConfig != null) {
                try {
                    itemConfig.ConfigureEntitySpawned(item, pickInfo);
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }
        }
    }

    public static void ConfigureItemOnCreate(InventoryItem item) {
        if (item != null && item.getScriptItem() != null && item.getScriptItem().getItemConfig() != null) {
            Item itemScript = item.getScriptItem();
            ItemConfig itemConfig = itemScript.getItemConfig();
            if (itemConfig != null) {
                try {
                    itemConfig.ConfigureEntityOnCreate(item);
                } catch (Exception var4) {
                    var4.printStackTrace();
                }
            }
        }
    }

    public static class IntegerStore {
        private final int id;

        public IntegerStore(int id) {
            this.id = id;
        }

        public int get() {
            return this.id;
        }
    }
}
