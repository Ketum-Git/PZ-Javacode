// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.inventory.InventoryItem;

public interface ILuaGameCharacterAttachedItems {
    AttachedItems getAttachedItems();

    void setAttachedItems(AttachedItems other);

    InventoryItem getAttachedItem(String location);

    void setAttachedItem(String location, InventoryItem item);

    void removeAttachedItem(InventoryItem item);

    void clearAttachedItems();

    AttachedLocationGroup getAttachedLocationGroup();
}
