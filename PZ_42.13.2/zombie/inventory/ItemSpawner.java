// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.List;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoGridSquare;

@UsedFromLua
public abstract class ItemSpawner {
    private static void inc(InventoryItem item, int count) {
        if (item != null) {
            InstanceTracker.adj("Item Spawns", item.getFullType(), count);
        }
    }

    public static List<InventoryItem> spawnItems(InventoryItem item, int count, ItemContainer container) {
        List<InventoryItem> items = container.AddItems(item, count);
        inc(items.get(0), items.size());
        return items;
    }

    public static List<InventoryItem> spawnItems(String itemType, int count, ItemContainer container) {
        List<InventoryItem> items = container.AddItems(itemType, count);
        inc(items.get(0), items.size());
        return items;
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, float x, float y, float z, boolean fill) {
        if (item == null) {
            return null;
        } else {
            square.AddWorldInventoryItem(item, x, y, z);
            if (item.getWorldItem() != null) {
                item.getWorldItem().setIgnoreRemoveSandbox(true);
            }

            inc(item, 1);
            if (fill && item instanceof InventoryContainer inventoryContainer && ItemPickerJava.containers.containsKey(item.getType())) {
                ItemPickerJava.rollContainerItem(inventoryContainer, null, ItemPickerJava.getItemPickerContainers().get(item.getType()));
                LuaEventManager.triggerEvent("OnFillContainer", "Container", item.getType(), inventoryContainer.getItemContainer());
            }

            item.setAutoAge();
            return item;
        }
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, float x, float y, float z) {
        return spawnItem(item, square, x, y, z, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square) {
        return spawnItem(item, square, 0.0F, 0.0F, 0.0F, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, IsoGridSquare square, boolean fill) {
        return spawnItem(item, square, 0.0F, 0.0F, 0.0F, fill);
    }

    public static InventoryItem spawnItem(String itemType, IsoGridSquare square, float x, float y, float z, boolean fill) {
        return spawnItem(InventoryItemFactory.CreateItem(itemType), square, x, y, z, fill);
    }

    public static InventoryItem spawnItem(String itemType, IsoGridSquare square, float x, float y, float z) {
        return spawnItem(InventoryItemFactory.CreateItem(itemType), square, x, y, z, true);
    }

    public static InventoryItem spawnItem(InventoryItem item, ItemContainer container, boolean fill) {
        if (!container.isItemAllowed(item)) {
            return null;
        } else {
            container.AddItem(item);
            inc(item, 1);
            if (fill && item instanceof InventoryContainer inventoryContainer && ItemPickerJava.containers.containsKey(item.getType())) {
                ItemPickerJava.rollContainerItem(inventoryContainer, null, ItemPickerJava.getItemPickerContainers().get(item.getType()));
                LuaEventManager.triggerEvent("OnFillContainer", "Container", item.getType(), inventoryContainer.getItemContainer());
            }

            item.setAutoAge();
            item.setAutoAge();
            return item;
        }
    }

    public static InventoryItem spawnItem(InventoryItem item, ItemContainer container) {
        return spawnItem(item, container, true);
    }

    public static InventoryItem spawnItem(String itemType, ItemContainer container, boolean fill) {
        return spawnItem(InventoryItemFactory.CreateItem(itemType), container, fill);
    }

    public static InventoryItem spawnItem(String itemType, ItemContainer container) {
        return spawnItem(InventoryItemFactory.CreateItem(itemType), container, true);
    }
}
