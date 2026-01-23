// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.scripting.objects.ItemTag;

@UsedFromLua
public class ItemCodeOnTest {
    public static boolean hasScrewdriver(IsoGameCharacter character, HandWeapon weapon, WeaponPart part) {
        if (character == null) {
            return true;
        } else {
            for (InventoryItem item : character.getInventory().getAllTag(ItemTag.SCREWDRIVER)) {
                if (item.isBroken()) {
                    return false;
                }
            }

            return true;
        }
    }
}
