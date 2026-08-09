// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.Item;

@UsedFromLua
public final class ComboItem extends InventoryItem {
    public ComboItem(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
    }

    public ComboItem(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
    }
}
