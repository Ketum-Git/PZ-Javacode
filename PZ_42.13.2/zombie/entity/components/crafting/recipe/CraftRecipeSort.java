// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.resources.Resource;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

@UsedFromLua
public class CraftRecipeSort {
    private static final Comparator<CraftRecipe> alphaNumComparator = new Comparator<CraftRecipe>() {
        public int compare(CraftRecipe o1, CraftRecipe o2) {
            return o1.getTranslationName().compareTo(o2.getTranslationName());
        }
    };

    public static List<CraftRecipe> alphaNumeric(List<CraftRecipe> listToSort) {
        listToSort.sort(alphaNumComparator);
        return listToSort;
    }

    public static List<CraftRecipe> validRecipes(List<CraftRecipe> listToSort, IsoGameCharacter character) {
        listToSort.sort(new CraftRecipeSort.ValidRecipeComparator(listToSort, character));
        return listToSort;
    }

    public static List<CraftRecipe> canPerformAndValidRecipes(
        List<CraftRecipe> listToSort,
        IsoGameCharacter character,
        ArrayList<Resource> sourceResources,
        ArrayList<InventoryItem> sourceItems,
        ArrayList<ItemContainer> containers
    ) {
        listToSort.sort(new CraftRecipeSort.ValidCanPerformRecipeComparator(listToSort, character, sourceResources, sourceItems, containers));
        return listToSort;
    }

    public static class ValidCanPerformRecipeComparator implements Comparator<CraftRecipe> {
        private final HashSet<CraftRecipe> isValidCache = new HashSet<>();
        private final HashSet<CraftRecipe> canPerformCache = new HashSet<>();

        public ValidCanPerformRecipeComparator(
            List<CraftRecipe> compareList,
            IsoGameCharacter character,
            ArrayList<Resource> sourceResources,
            ArrayList<InventoryItem> sourceItems,
            ArrayList<ItemContainer> containers
        ) {
            CraftRecipeData recipeData = new CraftRecipeData(CraftMode.Handcraft, true, true, false, true);
            recipeData.setCharacter(character);

            for (int i = 0; i < compareList.size(); i++) {
                CraftRecipe recipe = compareList.get(i);
                if (CraftRecipeManager.isValidRecipeForCharacter(recipe, character, null, null)) {
                    this.isValidCache.add(recipe);
                }

                recipeData.setRecipe(recipe);
                if (recipeData.canPerform(character, sourceResources, sourceItems, false, containers)) {
                    this.canPerformCache.add(recipe);
                }
            }
        }

        public int compare(CraftRecipe v1, CraftRecipe v2) {
            boolean isValidV1 = this.isValidCache.contains(v1);
            boolean isValidV2 = this.isValidCache.contains(v2);
            if (isValidV1 && !isValidV2) {
                return -1;
            } else if (!isValidV1 && isValidV2) {
                return 1;
            } else {
                boolean canPerformV1 = this.canPerformCache.contains(v1);
                boolean canPerformV2 = this.canPerformCache.contains(v2);
                if (canPerformV1 && !canPerformV2) {
                    return -1;
                } else {
                    return !canPerformV1 && canPerformV2 ? 1 : v1.getTranslationName().compareTo(v2.getTranslationName());
                }
            }
        }
    }

    public static class ValidRecipeComparator implements Comparator<CraftRecipe> {
        private final HashSet<CraftRecipe> isValidCache = new HashSet<>();

        public ValidRecipeComparator(List<CraftRecipe> compareList, IsoGameCharacter character) {
            for (int i = 0; i < compareList.size(); i++) {
                CraftRecipe recipe = compareList.get(i);
                if (CraftRecipeManager.isValidRecipeForCharacter(recipe, character, null, null)) {
                    this.isValidCache.add(recipe);
                }
            }
        }

        public int compare(CraftRecipe v1, CraftRecipe v2) {
            boolean isValidV1 = this.isValidCache.contains(v1);
            boolean isValidV2 = this.isValidCache.contains(v2);
            if (isValidV1 && !isValidV2) {
                return -1;
            } else {
                return !isValidV1 && isValidV2 ? 1 : v1.getTranslationName().compareTo(v2.getTranslationName());
            }
        }
    }
}
