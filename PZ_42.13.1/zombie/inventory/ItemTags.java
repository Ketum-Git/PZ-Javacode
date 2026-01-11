// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.debug.DebugLog;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;

public class ItemTags {
    private static final ArrayList<Item> emptyList = new ArrayList<>();
    private static final Map<ItemTag, ArrayList<Item>> tagItemMap = new HashMap<>();

    public static void Init(ArrayList<Item> allItems) {
        tagItemMap.clear();

        for (Item item : allItems) {
            for (ItemTag itemTag : item.getTags()) {
                registerItemTag(itemTag, item);
            }
        }
    }

    private static void registerItemTag(ItemTag itemTag, Item item) {
        if (!tagItemMap.containsKey(itemTag)) {
            tagItemMap.put(itemTag, new ArrayList<>());
        }

        if (!tagItemMap.get(itemTag).contains(item)) {
            tagItemMap.get(itemTag).add(item);
        }
    }

    public static ArrayList<Item> getItemsForTag(ItemTag itemTag) {
        return tagItemMap.containsKey(itemTag) ? tagItemMap.get(itemTag) : emptyList;
    }

    private static void printDebug() {
        DebugLog.log("==== ITEM TAGS ====");

        for (Entry<ItemTag, ArrayList<Item>> entry : tagItemMap.entrySet()) {
            DebugLog.log("[tag: " + entry.getKey() + "]");

            for (Item item : entry.getValue()) {
                DebugLog.log("  - " + item.getFullName());
            }
        }

        DebugLog.log("===/ ITEM TAGS /===");
    }
}
