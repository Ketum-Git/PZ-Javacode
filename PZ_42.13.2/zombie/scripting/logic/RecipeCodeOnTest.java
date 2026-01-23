// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoThumpable;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.util.list.PZArrayList;

@UsedFromLua
public class RecipeCodeOnTest extends RecipeCodeHelper {
    public static boolean cutFish(InventoryItem item, IsoGameCharacter character) {
        return item instanceof Food food ? food.getActualWeight() > 0.6F : true;
    }

    public static boolean breakGlass(InventoryItem item, IsoGameCharacter character) {
        return item.getFluidContainer() == null || item.getFluidContainer().isEmpty();
    }

    public static boolean hotFluidContainer(InventoryItem item, IsoGameCharacter character) {
        return !item.hasComponent(ComponentType.FluidContainer) || item.getItemHeat() > 1.6F;
    }

    public static boolean cutFillet(InventoryItem item, IsoGameCharacter character) {
        return item instanceof Food foodItem ? foodItem.getActualWeight() > 1.0F : true;
    }

    public static boolean purifyWater(InventoryItem item, IsoGameCharacter character) {
        return item.hasComponent(ComponentType.FluidContainer) ? item.getFluidContainer().contains(Fluid.TaintedWater) : true;
    }

    public static boolean canAddToPack(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof DrainableComboItem drainable) {
            return item.hasTag(ItemTag.PACKED) ? drainable.getCurrentUsesFloat() < 1.0F : true;
        } else {
            return true;
        }
    }

    public static boolean genericPacking(InventoryItem item, IsoGameCharacter character) {
        if (item instanceof Food food && food.isNormalAndFullFood()) {
            return false;
        } else if (item.getBloodLevel() > 0.0F) {
            return false;
        } else if (item instanceof Clothing clothing && clothing.getDirtyness() > 0.0F) {
            return false;
        } else if (item instanceof InventoryContainer cont && !cont.isEmpty()) {
            return false;
        } else if (item.getCondition() != item.getConditionMax()) {
            return false;
        } else {
            Item scriptItem = item.getScriptItem();
            return item.getColorRed() == scriptItem.getColorRed()
                && item.getColorGreen() == scriptItem.getColorGreen()
                && item.getColorBlue() == scriptItem.getColorBlue();
        }
    }

    public static boolean scratchTicket(InventoryItem item, IsoGameCharacter character) {
        return !((KahluaTableImpl)item.getModData()).rawgetBool("scratched");
    }

    public static boolean haveFilter(InventoryItem item, IsoGameCharacter character) {
        return ((Clothing)item).hasFilter();
    }

    public static boolean noFilter(InventoryItem item, IsoGameCharacter character) {
        return item instanceof Clothing clothing ? !clothing.hasFilter() : true;
    }

    public static boolean haveOxygenTank(InventoryItem item, IsoGameCharacter character) {
        return ((Clothing)item).hasTank();
    }

    public static boolean noOxygenTank(InventoryItem item, IsoGameCharacter character) {
        return item instanceof Clothing clothing ? !clothing.hasTank() : true;
    }

    public static boolean openFire(InventoryItem item, IsoGameCharacter character) {
        List<IsoGridSquare> squares = character.getCurrentSquare().getRadius(2);

        for (int i = 0; i < squares.size(); i++) {
            PZArrayList<IsoObject> objects = squares.get(i).getObjects();

            for (int i1 = 0; i1 < objects.size(); i1++) {
                IsoObject obj = objects.get(i1);
                if (!(obj instanceof IsoFireplace) && !(obj instanceof IsoBarbecue)) {
                    if (obj instanceof IsoThumpable && ((KahluaTableImpl)obj.getModData()).rawgetBool("isLit")) {
                        return true;
                    }
                } else if (obj.isLit()) {
                    return true;
                }
            }
        }

        return character.getSquare().hasAdjacentFireObject();
    }

    public static boolean copyKey(InventoryItem item, IsoGameCharacter character) {
        return !item.hasTag(ItemTag.BUILDING_KEY) || item.getKeyId() != -1;
    }
}
