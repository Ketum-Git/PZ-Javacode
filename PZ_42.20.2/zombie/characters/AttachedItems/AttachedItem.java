// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;

@UsedFromLua
public final class AttachedItem {
    protected final String location;
    protected final InventoryItem item;

    public AttachedItem(String location, InventoryItem item) {
        if (location == null) {
            throw new NullPointerException("location is null");
        } else if (location.isEmpty()) {
            throw new IllegalArgumentException("location is empty");
        } else if (item == null) {
            throw new NullPointerException("item is null");
        } else {
            this.location = location;
            this.item = item;
        }
    }

    public String getLocation() {
        return this.location;
    }

    public InventoryItem getItem() {
        return this.item;
    }
}
