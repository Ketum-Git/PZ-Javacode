// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.recipemanager.ItemRecipe;
import zombie.inventory.recipemanager.RecipeMonitor;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.Moveable;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.MovableRecipe;
import zombie.scripting.objects.Recipe;
import zombie.scripting.objects.ScriptModule;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeManager {
    private static final ArrayList<Recipe> RecipeList = new ArrayList<>();

    public static void ScriptsLoaded() {
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();
        Set<String> reported = new HashSet<>();

        for (Recipe recipe : recipes) {
            for (Recipe.Source source : recipe.getSource()) {
                for (int i = 0; i < source.getItems().size(); i++) {
                    String sourceType = source.getItems().get(i);
                    if (sourceType.startsWith("Fluid.")) {
                        source.getItems().set(i, sourceType);
                    } else if (!"Water".equals(sourceType) && !sourceType.contains(".") && !sourceType.startsWith("[")) {
                        Item scriptItem = resolveItemModuleDotType(recipe, sourceType, reported, "recipe source");
                        if (scriptItem == null) {
                            source.getItems().set(i, "???." + sourceType);
                        } else {
                            source.getItems().set(i, scriptItem.getFullName());
                        }
                    }
                }
            }

            if (!recipe.getResults().isEmpty()) {
                for (Recipe.Result result : recipe.getResults()) {
                    if (result.getModule() == null) {
                        Item scriptItem = resolveItemModuleDotType(recipe, result.getType(), reported, "recipe result");
                        if (scriptItem == null) {
                            result.module = "???";
                        } else {
                            result.module = scriptItem.getModule().getName();
                        }
                    }
                }
            }
        }
    }

    private static Item resolveItemModuleDotType(Recipe recipe, String sourceType, Set<String> reported, String errorMsg) {
        ScriptModule module = recipe.getModule();
        Item scriptItem = module.getItem(sourceType);
        if (scriptItem != null && !scriptItem.getObsolete()) {
            return scriptItem;
        } else {
            for (int i = 0; i < ScriptManager.instance.moduleList.size(); i++) {
                ScriptModule module1 = ScriptManager.instance.moduleList.get(i);
                scriptItem = module1.getItem(sourceType);
                if (scriptItem != null && !scriptItem.getObsolete()) {
                    String moduleName = recipe.getModule().getName();
                    if (!reported.contains(moduleName)) {
                        reported.add(moduleName);
                        DebugLog.Recipe.warn("WARNING: module \"%s\" may have forgot to import module Base", moduleName);
                    }

                    return scriptItem;
                }
            }

            DebugLog.Recipe.warn("ERROR: can't find %s \"%s\" in recipe \"%s\"", errorMsg, sourceType, recipe.getOriginalname());
            return null;
        }
    }

    public static void LoadedAfterLua() {
        ArrayList<Item> scriptItems = new ArrayList<>();
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();

        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            DebugLog.Recipe.debugln("Checking Recipe " + recipe.name);

            for (Recipe.Source source : recipe.getSource()) {
                ScriptManager.resolveGetItemTypes(source.getItems(), scriptItems);
            }
        }

        scriptItems.clear();
    }

    private static void testLuaFunction(Recipe recipe, String functionName, String varName) {
        if (!StringUtils.isNullOrWhitespace(functionName)) {
            Object functionObject = LuaManager.getFunctionObject(functionName);
            if (functionObject == null) {
                DebugLog.Recipe.error("no such function %s = \"%s\" in recipe \"%s\"", varName, functionName, recipe.name);
            }
        }
    }

    public static int getKnownRecipesNumber(IsoGameCharacter chr) {
        int result = 0;
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();

        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            if (chr.isRecipeKnown(recipe)) {
                result++;
            }
        }

        return result;
    }

    public static ArrayList<Recipe> getUniqueRecipeItems(InventoryItem item, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        RecipeList.clear();
        ArrayList<Recipe> allRecipes = ScriptManager.instance.getAllRecipes();

        for (int i = 0; i < allRecipes.size(); i++) {
            Recipe recipe = allRecipes.get(i);
            if (IsRecipeValid(recipe, chr, item, containers)) {
                RecipeList.add(recipe);
            }
        }

        if (item instanceof Moveable moveable && RecipeList.isEmpty() && moveable.getWorldSprite() != null) {
            if (item.type != null && item.type.equalsIgnoreCase(moveable.getWorldSprite())) {
                MovableRecipe recipe = new MovableRecipe();
                LuaEventManager.triggerEvent("OnDynamicMovableRecipe", moveable.getWorldSprite(), recipe, item, chr);
                if (recipe.isValid() && IsRecipeValid(recipe, chr, item, containers)) {
                    RecipeList.add(recipe);
                }
            } else {
                DebugLog.Recipe.warn("RecipeManager -> Cannot create recipe for this movable item: " + item.getFullType());
            }
        }

        return RecipeList;
    }

    public static boolean IsRecipeValid(Recipe recipe, IsoGameCharacter chr, InventoryItem item, ArrayList<ItemContainer> containers) {
        if (Core.debug) {
        }

        if (recipe.result == null) {
            return false;
        } else if (!chr.isRecipeKnown(recipe)) {
            return false;
        } else if (item != null && !validateRecipeContainsSourceItem(recipe, item)) {
            return false;
        } else if (!validateHasAllRequiredItems(recipe, chr, item, containers)) {
            return false;
        } else if (!validateHasRequiredSkill(recipe, chr)) {
            return false;
        } else if (!validateNearIsoObject(recipe, chr)) {
            return false;
        } else if (!validateHasHeat(recipe, item, containers, chr)) {
            return false;
        } else {
            for (Recipe.Source source : recipe.getSource()) {
                if (!source.keep) {
                    boolean exist = false;

                    for (String itemType : source.getItems()) {
                        if (containers == null) {
                            for (InventoryItem item1 : chr.getInventory().getItems()) {
                                if (item1.getFullType().equals(itemType) && validateCanPerform(recipe, chr, item1)) {
                                    exist = true;
                                } else if (itemType.startsWith("Fluid.") && validateCanPerform(recipe, chr, item1)) {
                                    String fluidType = itemType.substring(6);
                                    Fluid fluid = Fluid.Get(fluidType);
                                    if (item1.hasComponent(ComponentType.FluidContainer)
                                        && item1.getFluidContainer().contains(fluid)
                                        && item1.getFluidContainer().getAmount() >= source.use) {
                                        exist = true;
                                    }
                                }
                            }
                        } else {
                            for (ItemContainer container : containers) {
                                for (InventoryItem item1x : container.getItems()) {
                                    if (item1x.getFullType().equals(itemType) && validateCanPerform(recipe, chr, item1x)) {
                                        exist = true;
                                    } else if (itemType.startsWith("Fluid.") && validateCanPerform(recipe, chr, item1x)) {
                                        String fluidType = itemType.substring(6);
                                        Fluid fluid = Fluid.Get(fluidType);
                                        if (item1x.hasComponent(ComponentType.FluidContainer)
                                            && item1x.getFluidContainer().contains(fluid)
                                            && item1x.getFluidContainer().getAmount() >= source.use) {
                                            exist = true;
                                        }
                                    }
                                }

                                if (exist) {
                                }
                            }
                        }

                        if (exist) {
                        }
                    }

                    if (!exist) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    public static void printDebugRecipeValid(Recipe recipe, IsoGameCharacter chr, InventoryItem item, ArrayList<ItemContainer> containers) {
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.LogBlanc();
        }

        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[DebugTestRecipeValid]", RecipeMonitor.colHeader);
        }

        if (RecipeMonitor.canLog()) {
            RecipeMonitor.IncTab();
        }

        boolean valid = true;
        if (recipe.result == null) {
            String s = "invalid: recipe result is null.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!chr.isRecipeKnown(recipe)) {
            String s = "invalid: recipe not known.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (item != null && !validateRecipeContainsSourceItem(recipe, item)) {
            String s = "invalid: recipe does not contain source item.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!validateHasAllRequiredItems(recipe, chr, item, containers)) {
            String s = "invalid: recipe does not have all required items.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!validateHasRequiredSkill(recipe, chr)) {
            String s = "invalid: character does not have required skill.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!validateNearIsoObject(recipe, chr)) {
            String s = "invalid: recipe is not near required IsoObject.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!validateHasHeat(recipe, item, containers, chr)) {
            String s = "invalid: recipe heat validation failed.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        if (!validateCanPerform(recipe, chr, item)) {
            String s = "invalid: recipe can perform failed.";
            DebugLog.Recipe.warn(s);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log(s, RecipeMonitor.colNeg);
            }

            valid = false;
        }

        String s = "recipe overall valid: " + valid;
        DebugLog.Recipe.println(s);
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.DecTab();
        }

        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log(s, valid ? RecipeMonitor.colPos : RecipeMonitor.colNeg);
        }
    }

    private static boolean validateNearIsoObject(Recipe recipe, IsoGameCharacter chr) {
        return false;
    }

    private static boolean validateCanPerform(Recipe recipe, IsoGameCharacter chr, InventoryItem item) {
        return false;
    }

    private static boolean validateHasRequiredSkill(Recipe recipe, IsoGameCharacter chr) {
        if (recipe.getRequiredSkillCount() > 0) {
            for (int i = 0; i < recipe.getRequiredSkillCount(); i++) {
                Recipe.RequiredSkill skill = recipe.getRequiredSkill(i);
                if (chr.getPerkLevel(skill.getPerk()) < skill.getLevel()) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean validateRecipeContainsSourceItem(Recipe recipe, InventoryItem item) {
        for (int i = 0; i < recipe.source.size(); i++) {
            Recipe.Source source = recipe.getSource().get(i);

            for (int j = 0; j < source.getItems().size(); j++) {
                String sourceFullType = source.getItems().get(j);
                if (sourceFullType.startsWith("Fluid.") && item.hasComponent(ComponentType.FluidContainer)) {
                    return true;
                }

                if (sourceFullType.equals(item.getFullType())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean validateHasAllRequiredItems(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        ArrayList<InventoryItem> items = getAvailableItemsNeeded(recipe, chr, containers, selectedItem, null);
        return !items.isEmpty();
    }

    public static boolean validateHasHeat(Recipe recipe, InventoryItem item, ArrayList<ItemContainer> containers, IsoGameCharacter chr) {
        if (recipe.getHeat() == 0.0F) {
            return true;
        } else {
            InventoryItem drainableItem = null;

            for (InventoryItem i : getAvailableItemsNeeded(recipe, chr, containers, item, null)) {
                if (i instanceof DrainableComboItem) {
                    drainableItem = i;
                    break;
                }
            }

            if (drainableItem != null) {
                for (ItemContainer container : containers) {
                    for (InventoryItem ix : container.getItems()) {
                        if (ix.getName().equals(drainableItem.getName())) {
                            if (recipe.getHeat() < 0.0F) {
                                if (ix.getInvHeat() <= recipe.getHeat()) {
                                    return true;
                                }
                            } else if (recipe.getHeat() > 0.0F && ix.getInvHeat() + 1.0F >= recipe.getHeat()) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    public static ArrayList<InventoryItem> getAvailableItemsAll(
        Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems
    ) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, true);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getAvailableItemsNeeded(
        Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem, ArrayList<InventoryItem> ignoreItems
    ) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, false);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getSourceItemsAll(
        Recipe recipe,
        int sourceIndex,
        IsoGameCharacter chr,
        ArrayList<ItemContainer> containers,
        InventoryItem selectedItem,
        ArrayList<InventoryItem> ignoreItems
    ) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, true);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems(sourceIndex);
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static ArrayList<InventoryItem> getSourceItemsNeeded(
        Recipe recipe,
        int sourceIndex,
        IsoGameCharacter chr,
        ArrayList<ItemContainer> containers,
        InventoryItem selectedItem,
        ArrayList<InventoryItem> ignoreItems
    ) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, ignoreItems, false);
        ArrayList<InventoryItem> items = itemRecipe.getSourceItems(sourceIndex);
        ItemRecipe.Release(itemRecipe);
        RecipeMonitor.resume();
        return items;
    }

    public static int getNumberOfTimesRecipeCanBeDone(Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem) {
        return ItemRecipe.getNumberOfTimesRecipeCanBeDone(recipe, chr, containers, selectedItem);
    }

    public static ArrayList<InventoryItem> PerformMakeItem(Recipe recipe, InventoryItem selectedItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        RecipeMonitor.StartMonitor();
        RecipeMonitor.setRecipe(recipe);
        ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, null, false);
        ArrayList<InventoryItem> results = itemRecipe.perform();
        ItemRecipe.Release(itemRecipe);
        return results;
    }

    public static ArrayList<EvolvedRecipe> getAllEvolvedRecipes() {
        Stack<EvolvedRecipe> allRecipes = ScriptManager.instance.getAllEvolvedRecipes();
        ArrayList<EvolvedRecipe> result = new ArrayList<>();

        for (int i = 0; i < allRecipes.size(); i++) {
            result.add(allRecipes.get(i));
        }

        return result;
    }

    public static ArrayList<EvolvedRecipe> getEvolvedRecipe(
        InventoryItem baseItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers, boolean need1ingredient
    ) {
        ArrayList<EvolvedRecipe> result = new ArrayList<>();
        if (baseItem instanceof Food food && food.isRotten() && chr.getPerkLevel(PerkFactory.Perks.Cooking) < 7) {
            return result;
        } else {
            Stack<EvolvedRecipe> allRecipes = ScriptManager.instance.getAllEvolvedRecipes();

            for (int i = 0; i < allRecipes.size(); i++) {
                EvolvedRecipe recipe = allRecipes.get(i);
                if ((!baseItem.isCooked() || recipe.addIngredientIfCooked)
                    && (baseItem.getFullType().equals(recipe.baseItem) || baseItem.getFullType().equals(recipe.resultItem))
                    && (!baseItem.getType().equals("WaterPot") || !(baseItem.getCurrentUsesFloat() < 0.75))) {
                    if (need1ingredient) {
                        ArrayList<InventoryItem> items = recipe.getItemsCanBeUse(chr, baseItem, containers);
                        if (!items.isEmpty()) {
                            if (!(baseItem instanceof Food food && food.isFrozen())) {
                                result.add(recipe);
                            } else if (recipe.isAllowFrozenItem()) {
                                result.add(recipe);
                            }
                        }
                    } else {
                        result.add(recipe);
                    }
                }
            }

            return result;
        }
    }

    public static Recipe getDismantleRecipeFor(String item) {
        RecipeList.clear();
        ArrayList<Recipe> allRecipes = ScriptManager.instance.getAllRecipes();

        for (int i = 0; i < allRecipes.size(); i++) {
            Recipe recipe = allRecipes.get(i);
            ArrayList<Recipe.Source> sources = recipe.getSource();
            if (!sources.isEmpty()) {
                for (int k = 0; k < sources.size(); k++) {
                    Recipe.Source source = sources.get(k);

                    for (int m = 0; m < source.getItems().size(); m++) {
                        if (source.getItems().get(m).equalsIgnoreCase(item) && recipe.name.toLowerCase().startsWith("dismantle ")) {
                            return recipe;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static InventoryItem GetMovableRecipeTool(
        boolean isPrimary, Recipe recipe, InventoryItem selectedItem, IsoGameCharacter chr, ArrayList<ItemContainer> containers
    ) {
        if (recipe instanceof MovableRecipe movableRecipe) {
            Recipe.Source source = isPrimary ? movableRecipe.getPrimaryTools() : movableRecipe.getSecondaryTools();
            if (source != null && source.getItems() != null && !source.getItems().isEmpty()) {
                RecipeMonitor.suspend();
                ItemRecipe itemRecipe = ItemRecipe.Alloc(recipe, chr, containers, selectedItem, null, false);
                if (itemRecipe.getSourceItems() != null && !itemRecipe.getSourceItems().isEmpty()) {
                    ArrayList<InventoryItem> items = itemRecipe.getSourceItems();
                    ItemRecipe.Release(itemRecipe);
                    RecipeMonitor.resume();

                    for (int i = 0; i < items.size(); i++) {
                        InventoryItem item = items.get(i);

                        for (int j = 0; j < source.getItems().size(); j++) {
                            if (item.getFullType().equalsIgnoreCase(source.getItems().get(j))) {
                                return item;
                            }
                        }
                    }

                    return null;
                } else {
                    ItemRecipe.Release(itemRecipe);
                    RecipeMonitor.resume();
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean HasAllRequiredItems(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        return validateHasAllRequiredItems(recipe, chr, selectedItem, containers);
    }

    public static boolean isAllItemsUsableRotten(Recipe recipe, IsoGameCharacter chr, InventoryItem selectedItem, ArrayList<ItemContainer> containers) {
        if (chr.getPerkLevel(PerkFactory.Perks.Cooking) >= 7) {
            return true;
        } else {
            for (InventoryItem item : getAvailableItemsNeeded(recipe, chr, containers, selectedItem, null)) {
                if (item instanceof Food food && food.isRotten()) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean hasHeat(Recipe recipe, InventoryItem item, ArrayList<ItemContainer> containers, IsoGameCharacter chr) {
        return validateHasHeat(recipe, item, containers, chr);
    }

    private static void DebugPrintAllRecipes() {
    }

    @Deprecated
    public static boolean IsItemDestroyed(String itemToUse, Recipe recipe) {
        DebugLog.Recipe.error("Method is deprecated.");
        return false;
    }

    @Deprecated
    public static float UseAmount(String sourceFullType, Recipe recipe, IsoGameCharacter chr) {
        DebugLog.Recipe.error("Method is deprecated.");
        return 0.0F;
    }

    @Deprecated
    public static boolean DoesWipeUseDelta(String itemToUse, String itemToMake) {
        DebugLog.Recipe.error("Method is deprecated.");
        return true;
    }

    @Deprecated
    public static boolean DoesUseItemUp(String itemToUse, Recipe recipe) {
        DebugLog.Recipe.error("Method is deprecated.");
        return false;
    }
}
