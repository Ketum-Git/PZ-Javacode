// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import zombie.UsedFromLua;
import zombie.inventory.types.Food;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemKey;

@UsedFromLua
public class RecipeCodeOnCooked extends RecipeCodeHelper {
    public static void cannedFood(Food food) {
        float aged = food.getAge() / food.getOffAgeMax();
        food.setOffAgeMax(1560);
        food.setOffAge(730);
        food.setAge(food.getOffAgeMax() * aged);
    }

    public static void nameCakePrep(Food cake) {
        cake.setCustomName(true);
        cake.setName(ScriptManager.instance.getItem(ItemKey.Food.CAKE_RAW.toString()).getDisplayName());
    }
}
