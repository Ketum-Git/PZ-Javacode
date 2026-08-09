// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.WornItems;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.ItemBodyLocation;

/**
 * ILuaGameCharacterClothing
 *   Provides the functions expected by LUA when dealing with objects of this type.
 */
public interface ILuaGameCharacterClothing {
    void dressInNamedOutfit(String outfitName);

    void dressInPersistentOutfit(String outfitName);

    void dressInPersistentOutfitID(int outfitID);

    String getOutfitName();

    WornItems getWornItems();

    void setWornItems(WornItems other);

    InventoryItem getWornItem(ItemBodyLocation var1);

    void setWornItem(ItemBodyLocation var1, InventoryItem var2);

    void removeWornItem(InventoryItem item);

    void removeWornItem(InventoryItem var1, boolean var2);

    void clearWornItems();

    BodyLocationGroup getBodyLocationGroup();

    void setClothingItem_Head(InventoryItem item);

    void setClothingItem_Torso(InventoryItem item);

    void setClothingItem_Back(InventoryItem item);

    void setClothingItem_Hands(InventoryItem item);

    void setClothingItem_Legs(InventoryItem item);

    void setClothingItem_Feet(InventoryItem item);

    void Dressup(SurvivorDesc desc);
}
