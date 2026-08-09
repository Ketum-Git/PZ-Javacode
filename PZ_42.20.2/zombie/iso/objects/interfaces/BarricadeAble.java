// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects.interfaces;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoBarricade;

@UsedFromLua
public interface BarricadeAble {
    boolean isBarricaded();

    boolean isBarricadeAllowed();

    IsoBarricade getBarricadeOnSameSquare();

    IsoBarricade getBarricadeOnOppositeSquare();

    IsoBarricade getBarricadeForCharacter(IsoGameCharacter chr);

    IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter chr);

    IsoGridSquare getSquare();

    IsoGridSquare getOppositeSquare();

    boolean getNorth();

    default IsoBarricade addBarricadesFromCraftRecipe(IsoGameCharacter chr, ArrayList<InventoryItem> items, CraftRecipeData craftRecipeData, boolean opposite) {
        if (items.isEmpty()) {
            String type = "Base.Plank";
            if (craftRecipeData.getRecipe().canUseItem("Base.SheetMetal")) {
                type = "Base.SheetMetal";
            } else if (craftRecipeData.getRecipe().canUseItem("Base.MetalBar")) {
                type = "Base.MetalBar";
            }

            InventoryItem item = InventoryItemFactory.CreateItem(type);
            items.add(item);
        }

        IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(this, opposite);
        if (barricade != null) {
            barricade.addFromCraftRecipe(chr, items);
        }

        return barricade;
    }
}
