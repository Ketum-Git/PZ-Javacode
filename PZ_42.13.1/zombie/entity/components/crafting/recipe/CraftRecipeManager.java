// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.InventoryContainer;
import zombie.network.GameClient;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.util.TaggedObjectManager;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZUnmodifiableList;

@UsedFromLua
public class CraftRecipeManager {
    private static final float FLOAT_EPSILON = 1.0E-5F;
    private static boolean initialized;
    private static TaggedObjectManager<CraftRecipe> craftRecipeTagManager;
    private static List<CraftRecipe> unmodifiableAllRecipes;
    private static final Map<IsoPlayer, CraftRecipeData> playerCraftDataMap = new HashMap<>();
    private static final ArrayList<CraftRecipe> RecipeList = new ArrayList<>();

    public static void Reset() {
        playerCraftDataMap.clear();
        craftRecipeTagManager.clear();
        craftRecipeTagManager = null;
        unmodifiableAllRecipes = null;
        initialized = false;
    }

    public static void Init() {
        craftRecipeTagManager = new TaggedObjectManager<>(new CraftRecipeManager.CraftRecipeListProvider());
        craftRecipeTagManager.registerObjectsFromBackingList();
        initialized = true;
        LogAllRecipesToFile();
    }

    public static String FormatAndRegisterRecipeTagsQuery(String tagQueryString) throws Exception {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            return craftRecipeTagManager.formatAndRegisterQueryString(tagQueryString);
        }
    }

    public static String sanitizeTagQuery(String tagQueryString) {
        return craftRecipeTagManager.formatQueryString(tagQueryString);
    }

    public static List<CraftRecipe> getRecipesForTag(String category) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            return craftRecipeTagManager.getListForTag(category);
        }
    }

    public static List<String> getAllRecipeTags() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            return craftRecipeTagManager.getRegisteredTags();
        }
    }

    public static List<String> getTagGroups() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            ArrayList<String> list = new ArrayList<>();
            craftRecipeTagManager.getRegisteredTagGroups(list);
            return list;
        }
    }

    public static void debugPrintTagManager() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            craftRecipeTagManager.debugPrint();
        }
    }

    public static ArrayList<String> debugPrintTagManagerLines() {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            ArrayList<String> list = new ArrayList<>();
            craftRecipeTagManager.debugPrint(list);
            return list;
        }
    }

    public static void LogAllRecipesToFile() {
        List<CraftRecipe> recipes = queryRecipes("*");
        List<String> tags = getAllRecipeTags();

        try {
            String outputPath = ZomboidFileSystem.instance.getCacheDirSub("Crafting");
            ZomboidFileSystem.ensureFolderExists(outputPath);
            FileWriter writer = new FileWriter(outputPath + File.separator + "AllRecipes.txt");
            writer.write("Recipe and Tag reference\n\n");
            writer.write("Available Tags:\n");

            for (int i = 0; i < tags.size(); i++) {
                writer.write(i + ": \t" + tags.get(i) + "\n");
            }

            writer.write("\nAll Recipes:\n");

            for (int i = 0; i < recipes.size(); i++) {
                writer.write(i + ": \t" + recipes.get(i).getName() + "\n");
            }

            writer.flush();
            writer.close();
        } catch (Exception var5) {
        }
    }

    public static List<CraftRecipe> queryRecipes(String tagQueryString) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else if (!StringUtils.isNullOrWhitespace(tagQueryString) && tagQueryString.equals("*")) {
            if (unmodifiableAllRecipes == null) {
                unmodifiableAllRecipes = PZUnmodifiableList.wrap(ScriptManager.instance.getAllCraftRecipes());
            }

            return unmodifiableAllRecipes;
        } else {
            return craftRecipeTagManager.queryTaggedObjects(tagQueryString);
        }
    }

    public static List<CraftRecipe> populateRecipeList(String tagQueryString, List<CraftRecipe> listToPopulate, boolean clearList) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            return craftRecipeTagManager.populateList(tagQueryString, listToPopulate, null, clearList);
        }
    }

    public static List<CraftRecipe> populateRecipeList(String tagQueryString, List<CraftRecipe> listToPopulate, List<CraftRecipe> sourceList, boolean clearList) {
        if (!initialized && Core.debug) {
            throw new RuntimeException("Not initialized.");
        } else {
            return craftRecipeTagManager.populateList(tagQueryString, listToPopulate, sourceList, clearList);
        }
    }

    public static List<CraftRecipe> filterRecipeList(String filterString, List<CraftRecipe> listToPopulate) {
        return filterRecipeList(filterString, listToPopulate, ScriptManager.instance.getAllCraftRecipes());
    }

    public static List<CraftRecipe> filterRecipeList(String filterString, List<CraftRecipe> listToPopulate, List<CraftRecipe> sourceList) {
        if (listToPopulate != null) {
            listToPopulate.clear();
        }

        if (sourceList == null || listToPopulate == null) {
            DebugLog.General.error("one of list parameters is null.");
            return listToPopulate;
        } else if (StringUtils.isNullOrWhitespace(filterString)) {
            listToPopulate.addAll(sourceList);
            return listToPopulate;
        } else {
            CraftRecipeManager.FilterMode filterMode = CraftRecipeManager.FilterMode.Name;
            if (filterString.startsWith("@")) {
                filterMode = CraftRecipeManager.FilterMode.ModName;
                filterString = filterString.substring(1);
            } else if (filterString.startsWith("$")) {
                filterMode = CraftRecipeManager.FilterMode.Tags;
                filterString = filterString.substring(1);
            }

            filterString = filterString.toLowerCase();
            switch (filterMode) {
                case Name:
                    for (int ixx = 0; ixx < sourceList.size(); ixx++) {
                        CraftRecipe recipe = sourceList.get(ixx);
                        if (recipe.getTranslationName() != null && recipe.getTranslationName().toLowerCase().contains(filterString)) {
                            listToPopulate.add(recipe);
                        }
                    }
                    break;
                case ModName:
                    for (int ix = 0; ix < sourceList.size(); ix++) {
                        CraftRecipe recipe = sourceList.get(ix);
                        if (recipe.getModName() != null && recipe.getModName().toLowerCase().contains(filterString)) {
                            listToPopulate.add(recipe);
                        }
                    }
                    break;
                case Tags:
                    StringBuilder query = new StringBuilder();

                    for (int i = 0; i < craftRecipeTagManager.getRegisteredTags().size(); i++) {
                        String tag = craftRecipeTagManager.getRegisteredTags().get(i);
                        if (tag.contains(filterString)) {
                            if (!query.isEmpty()) {
                                query.append(";");
                            }

                            query.append(tag);
                        }
                    }

                    String queryString = query.toString();
                    craftRecipeTagManager.filterList(queryString, listToPopulate, sourceList, true);
            }

            return listToPopulate;
        }
    }

    public static CraftRecipeData getCraftDataForPlayer(IsoPlayer player) {
        CraftRecipeData data = playerCraftDataMap.get(player);
        if (data == null) {
            playerCraftDataMap.put(player, data);
        }

        throw new RuntimeException("not implemented");
    }

    public static ArrayList<InventoryItem> getAllItemsFromContainers(ArrayList<ItemContainer> containers, ArrayList<InventoryItem> items) {
        items.clear();

        for (int i = 0; i < containers.size(); i++) {
            PZArrayUtil.addAll(items, containers.get(i).getItems());
        }

        return items;
    }

    public static ArrayList<InventoryItem> getAllValidItemsForRecipe(
        CraftRecipe recipe, ArrayList<InventoryItem> sourceItems, ArrayList<InventoryItem> filteredItems
    ) {
        for (int i = 0; i < sourceItems.size(); i++) {
            InventoryItem inventoryItem = sourceItems.get(i);
            if (isItemValidForRecipe(recipe, inventoryItem)) {
                filteredItems.add(inventoryItem);
            }
        }

        return filteredItems;
    }

    public static InputScript getValidInputScriptForItem(CraftRecipe recipe, InventoryItem inventoryItem) {
        for (int i = 0; i < recipe.getInputs().size(); i++) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() == ResourceType.Item && isItemValidForInputScript(input, inventoryItem)) {
                return input;
            }
        }

        return null;
    }

    public static ArrayList<InputScript> getAllValidInputScriptsForItem(CraftRecipe recipe, InventoryItem inventoryItem) {
        ArrayList<InputScript> output = new ArrayList<>();

        for (int i = 0; i < recipe.getInputs().size(); i++) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() == ResourceType.Item && isItemValidForInputScript(input, inventoryItem)) {
                output.add(input);
            }
        }

        return output;
    }

    public static boolean isItemToolForRecipe(CraftRecipe recipe, InventoryItem inventoryItem) {
        InputScript input = getValidInputScriptForItem(recipe, inventoryItem);
        return input != null && (input.hasFlag(InputFlag.ToolLeft) || input.hasFlag(InputFlag.ToolRight));
    }

    public static boolean isItemValidForRecipe(CraftRecipe recipe, InventoryItem inventoryItem) {
        InputScript input = getValidInputScriptForItem(recipe, inventoryItem);
        return input != null;
    }

    public static boolean isItemValidForInputScript(InputScript input, InventoryItem inventoryItem) {
        return input.getResourceType() == ResourceType.Item ? consumeInputItem(input, inventoryItem, true, null, null) : false;
    }

    public static boolean isValidRecipeForCharacter(CraftRecipe recipe, IsoGameCharacter character, CraftRecipeMonitor _m, ArrayList<ItemContainer> containers) {
        if (recipe == null) {
            DebugLog.CraftLogic.debugln("Recipe is null");
            return false;
        } else if (character != null && !validateHasRequiredSkill(recipe, character, containers)) {
            if (_m != null) {
                _m.log("Player doesn't have required skill for " + recipe.getScriptObjectFullType());
            }

            DebugLog.CraftLogic.debugln("Player doesn't have required skill for " + recipe.getScriptObjectFullType());
            return false;
        } else if (character != null && recipe.needToBeLearn() && !character.isRecipeKnown(recipe, true)) {
            if (_m != null) {
                _m.log("Player doesn't know recipe " + recipe.getScriptObjectFullType());
            }

            DebugLog.CraftLogic.debugln("Player doesn't know recipe " + recipe.getScriptObjectFullType());
            return false;
        } else {
            return true;
        }
    }

    private static boolean validateHasRequiredSkill(CraftRecipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        if (recipe.getRequiredSkillCount() > 0) {
            for (int i = 0; i < recipe.getRequiredSkillCount(); i++) {
                CraftRecipe.RequiredSkill skill = recipe.getRequiredSkill(i);
                if (chr.getPerkLevel(skill.getPerk()) < skill.getLevel()) {
                    if (containers == null) {
                        return false;
                    }

                    if (!recipe.validateBenefitFromRecipeAtHand(chr, containers)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean hasPlayerLearnedRecipe(CraftRecipe recipe, IsoGameCharacter character) {
        return recipe.hasPlayerLearned(character);
    }

    public static boolean hasPlayerRequiredSkill(CraftRecipe.RequiredSkill requiredSkill, IsoGameCharacter character) {
        return requiredSkill != null && character != null ? character.getPerkLevel(requiredSkill.getPerk()) >= requiredSkill.getLevel() : false;
    }

    public static int getAutoCraftCountItems(CraftRecipe recipe, ArrayList<InventoryItem> allItems) {
        int currentInputCount = 0;
        int worstInputCount = 0;
        int fullConsumeCount = 0;

        for (int i = 0; i < recipe.getInputs().size(); i++) {
            InputScript input = recipe.getInputs().get(i);
            if (input.getResourceType() == ResourceType.Item) {
                currentInputCount = 0;
                fullConsumeCount = 0;

                for (int j = 0; j < allItems.size(); j++) {
                    InventoryItem inventoryItem = allItems.get(j);
                    if (consumeInputItem(input, inventoryItem, true, null, null)) {
                        if (!input.isKeep()) {
                            fullConsumeCount++;
                        } else if (input.hasConsumeFromItem()) {
                            switch (input.getResourceType()) {
                                case Item:
                                    currentInputCount += inventoryItem.getCurrentUses() / (int)input.getAmount();
                                    break;
                                case Fluid:
                                    currentInputCount += PZMath.fastfloor(inventoryItem.getFluidContainer().getAmount() / input.getAmount());
                                    break;
                                case Energy:
                                    currentInputCount += inventoryItem.getCurrentUses() / (int)input.getAmount();
                            }
                        } else {
                            currentInputCount++;
                        }
                    }
                }

                if (fullConsumeCount > 0) {
                    int consumes = PZMath.fastfloor((float)fullConsumeCount / input.getIntAmount());
                    worstInputCount = PZMath.min(worstInputCount, consumes);
                } else if (currentInputCount > 0) {
                    worstInputCount = PZMath.min(worstInputCount, currentInputCount);
                }

                if (worstInputCount == 0) {
                    break;
                }
            }
        }

        return PZMath.max(0, worstInputCount);
    }

    private static boolean validateInputScript(InputScript inputScript, ResourceType resourceType, boolean testOnly) {
        if (inputScript != null && inputScript.getResourceType() == resourceType) {
            if (Core.debug && !testOnly && GameClient.client) {
                throw new RuntimeException("Recipes can only be tested on client, input=" + inputScript);
            } else {
                return true;
            }
        } else if (Core.debug) {
            throw new RuntimeException("Wrong InputScript.ResourceType for call or null, input=" + inputScript);
        } else {
            return false;
        }
    }

    private static boolean validateOutputScript(OutputScript outputScript, ResourceType resourceType, boolean testOnly) {
        if (outputScript != null && outputScript.getResourceType() == resourceType) {
            if (Core.debug && !testOnly && GameClient.client) {
                throw new RuntimeException("Recipes can only be tested on client, output=" + outputScript);
            } else {
                return true;
            }
        } else if (Core.debug) {
            throw new RuntimeException("Wrong OutputScript.ResourceType for call or null, output=" + outputScript);
        } else {
            return false;
        }
    }

    protected static boolean consumeInputFromResource(
        InputScript input, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        if (input != null && resource != null && input.getResourceType() == resource.getType()) {
            switch (input.getResourceType()) {
                case Item:
                    return consumeInputItem(input, resource, testOnly, cacheData, character);
                case Fluid:
                    return consumeInputFluid(input, resource, testOnly, cacheData);
                case Energy:
                    return consumeInputEnergy(input, resource, testOnly, cacheData);
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    protected static boolean consumeInputItem(
        InputScript input, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        if (!validateInputScript(input, ResourceType.Item, testOnly)) {
            return false;
        } else if (resource != null && !resource.isEmpty() && resource.getType() == ResourceType.Item) {
            float targetRequiredAmount = input.getIntAmount();
            float maxAllowedAmount = input.getIntMaxAmount();
            if (input.hasConsumeFromItem()) {
                InputScript consumeScript = input.getConsumeFromItemScript();
                targetRequiredAmount = consumeScript.getAmount();
                maxAllowedAmount = consumeScript.getMaxAmount();
            }

            if (targetRequiredAmount <= 0.0F) {
                return false;
            } else {
                HashSet<InventoryItem> tempUsedItems = new HashSet<>();
                HashSet<InventoryItem> tempConsumedUsedItems = new HashSet<>();
                HashMap<Resource, ArrayList<InventoryItem>> tempVariableOverfillUsedItems = new HashMap<>();
                float requiredAmount = targetRequiredAmount;
                float allowedAmount = maxAllowedAmount;
                List<Item> inputItems = input.getPossibleInputItems();

                for (int j = 0; j < inputItems.size(); j++) {
                    for (int i = 0; i < resource.getItemAmount(); i++) {
                        InventoryItem inventoryItem = resource.peekItem(i);
                        if (inventoryItem.getScriptItem().getFullName().equals(inputItems.get(j).getFullName()) && !tempUsedItems.contains(inventoryItem)) {
                            float preUsedFluidAvail = 0.0F;
                            if (!testOnly && input.hasConsumeFromItem()) {
                                InputScript consumeScript = input.getConsumeFromItemScript();
                                switch (consumeScript.getResourceType()) {
                                    case Fluid:
                                        preUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                                }
                            }

                            if (consumeInputItem(input, inventoryItem, testOnly, cacheData, character)) {
                                tempUsedItems.add(inventoryItem);
                                if (!testOnly) {
                                    if (requiredAmount > 1.0E-5F) {
                                        tempConsumedUsedItems.add(inventoryItem);
                                    } else {
                                        ArrayList<InventoryItem> items = tempVariableOverfillUsedItems.get(resource);
                                        if (items == null) {
                                            items = new ArrayList<>();
                                            tempVariableOverfillUsedItems.put(resource, items);
                                        }

                                        items.add(inventoryItem);
                                    }
                                }

                                float uses = input.isItemCount() ? 1.0F : inventoryItem.getCurrentUses();
                                float scale = input.getRelativeScale(inventoryItem.getFullType());
                                float requses = uses / scale;
                                if (input.hasConsumeFromItem()) {
                                    InputScript script = input.getConsumeFromItemScript();
                                    switch (script.getResourceType()) {
                                        case Fluid:
                                            if (!testOnly) {
                                                float postUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                                                uses = preUsedFluidAvail - postUsedFluidAvail;
                                                requses = uses;
                                            } else {
                                                uses = inventoryItem.getFluidContainer().getAmount();
                                                requses = uses;
                                            }
                                    }
                                }

                                uses = Math.min(requses, uses);
                                requiredAmount -= uses;
                                allowedAmount -= uses;
                                if (allowedAmount <= 1.0E-5F) {
                                    break;
                                }
                            }
                        }
                    }

                    if (requiredAmount <= 1.0E-5F) {
                        if (cacheData != null && !resource.canMoveItemsToOutput()) {
                            cacheData.setMoveToOutputs(false);
                        }

                        if (!testOnly && (!input.isKeep() || resource.canMoveItemsToOutput())) {
                            for (InventoryItem item : tempConsumedUsedItems) {
                                InventoryItem removedItem = ((ResourceItem)resource).removeItem(item);
                                if ((removedItem == null || removedItem != item) && Core.debug) {
                                    throw new RuntimeException("Item didn't get removed.");
                                }
                            }

                            cacheData.getRecipeData().addOverfilledResource(input, tempVariableOverfillUsedItems);
                        }

                        if (input.isVariableAmount()) {
                            float filledAmount = Math.min(targetRequiredAmount - requiredAmount, maxAllowedAmount);
                            float filledRatio = filledAmount / targetRequiredAmount;
                            cacheData.getRecipeData()
                                .setCalculatedVariableInputRatio(Math.min(cacheData.getRecipeData().getCalculatedVariableInputRatio(), filledRatio));
                        }

                        return true;
                    }

                    if (input.isExclusive()) {
                        requiredAmount = targetRequiredAmount;
                        allowedAmount = maxAllowedAmount;
                        tempUsedItems.clear();
                        tempVariableOverfillUsedItems.clear();
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    protected static boolean consumeInputItem(
        InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        if (!validateInputScript(input, ResourceType.Item, testOnly)) {
            return false;
        } else {
            boolean success = consumeInputItemInternal(input, inventoryItem, testOnly, cacheData, character);
            boolean moveToOutput = input.isKeep();
            if (success && input.hasConsumeFromItem()) {
                InputScript script = input.getConsumeFromItemScript();
                switch (script.getResourceType()) {
                    case Item:
                        success = consumeInputItemUsesInternal(script, inventoryItem, testOnly, cacheData);
                        if (success && !input.isDestroy()) {
                            moveToOutput = inventoryItem.getCurrentUses() < input.getAmount();
                        }
                        break;
                    case Fluid:
                        success = consumeInputFluidInternal(script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                        if (success) {
                            moveToOutput = inventoryItem.getFluidContainer().getAmount() < input.getAmount();
                        }
                        break;
                    case Energy:
                        success = consumeInputEnergyFromItemInternal(script, inventoryItem, testOnly, cacheData);
                        if (success) {
                            moveToOutput = inventoryItem.getCurrentUses() < input.getAmount();
                        }
                }
            }

            if (success && input.hasCreateToItem()) {
                OutputScript script = input.getCreateToItemScript();
                switch (script.getResourceType()) {
                    case Item:
                        success = createOutputItemUsesInternal(script, inventoryItem, testOnly, cacheData);
                        break;
                    case Fluid:
                        success = createOutputFluidInternal(script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                        break;
                    case Energy:
                        success = createOutputEnergyToItemInternal(script, inventoryItem, testOnly, cacheData);
                }
            }

            if (success) {
                if (cacheData != null) {
                    cacheData.setMoveToOutputs(moveToOutput);
                }

                return true;
            } else {
                if (cacheData != null) {
                    cacheData.softReset();
                }

                return false;
            }
        }
    }

    private static boolean consumeInputItemInternal(
        InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        Item item = inventoryItem.getScriptItem();
        if (item == null) {
            return false;
        } else if (input.containsItem(item)) {
            String inputItemType = inventoryItem.getType();
            if (!input.getParentRecipe().OnTestItem(inventoryItem)) {
                return false;
            } else if (!input.doesItemPassRoutineStatusTests(inventoryItem, character)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails routine status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else if (!input.doesItemPassClothingTypeStatusTests(inventoryItem)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails clothing status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else if (!input.doesItemPassSharpnessStatusTests(inventoryItem)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails sharpness status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else if (!input.doesItemPassDamageStatusTests(inventoryItem)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails damage status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else if (!input.doesItemPassIsOrNotEmptyAndFullTests(inventoryItem)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails empty/full status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else if (!input.doesItemPassFoodAndCookingTests(inventoryItem)) {
                DebugLog.CraftLogic
                    .debugln(inputItemType + " fails food/cooking status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                return false;
            } else {
                if (input.hasFlag(InputFlag.IsEmptyContainer) && inventoryItem instanceof InventoryContainer bag && !bag.isEmpty()) {
                    DebugLog.CraftLogic
                        .debugln(inputItemType + " fails empty bag status test for " + input.getParentRecipe().getName() + " - " + input.getOriginalLine());
                }

                if (cacheData != null) {
                    cacheData.addAppliedItem(inventoryItem);
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private static boolean consumeInputItemUsesInternal(InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (inventoryItem instanceof DrainableComboItem comboItem) {
            Item item = inventoryItem.getScriptItem();
            if (item == null || !input.containsItem(item)) {
                return false;
            }

            if (input.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                return false;
            }

            int useAmount = 0;
            if (input.getAmount() > 0.0F) {
                useAmount = (int)input.getAmount();
            }

            if (comboItem.getCurrentUses() >= useAmount) {
                if (!testOnly && !input.isKeep()) {
                    comboItem.setCurrentUses(comboItem.getCurrentUses() - useAmount);
                }

                if (cacheData != null) {
                    cacheData.usesConsumed = useAmount;
                }

                return true;
            }
        }

        return false;
    }

    private static boolean consumeInputFluid(InputScript input, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!validateInputScript(input, ResourceType.Fluid, testOnly)) {
            return false;
        } else {
            return resource != null && resource.getType() == ResourceType.Fluid && !resource.isEmpty()
                ? consumeInputFluidInternal(input, ((ResourceFluid)resource).getFluidContainer(), testOnly, cacheData)
                : false;
        }
    }

    private static boolean consumeInputFluidInternal(InputScript input, FluidContainer fc, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (fc != null && !fc.isEmpty()) {
            if (input.hasFlag(InputFlag.IsFull) && !fc.isFull()) {
                return false;
            }

            float requiredFluid = input.getAmount();
            InputScript parentScript = input.hasParentScript() ? input.getParentScript() : input;
            if (parentScript.isItemCount() && fc.getAmount() < requiredFluid) {
                return false;
            }

            if (cacheData != null) {
                requiredFluid = input.getAmount() - cacheData.fluidConsumed;
            }

            if (input.isFluidMatch(fc)) {
                float fluidToTake = Math.min(requiredFluid, fc.getAmount());
                if (!testOnly) {
                    if (cacheData != null) {
                        FluidSample newSample = fc.createFluidSample(fluidToTake);
                        cacheData.fluidSample.combineWith(newSample);
                        newSample.release();
                    }

                    if (!input.isKeep()) {
                        if (cacheData != null) {
                            FluidConsume newConsume = fc.removeFluid(fluidToTake, true);
                            cacheData.fluidConsume.combineWith(newConsume);
                            newConsume.release();
                        } else {
                            fc.removeFluid(fluidToTake, false);
                        }
                    }
                }

                if (cacheData != null) {
                    cacheData.fluidConsumed += fluidToTake;
                }

                return true;
            }
        }

        return false;
    }

    private static boolean consumeInputEnergy(InputScript input, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!validateInputScript(input, ResourceType.Energy, testOnly)) {
            return false;
        } else if (resource != null && resource.getType() == ResourceType.Energy && !resource.isEmpty()) {
            ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
            if (input.hasFlag(InputFlag.IsFull) && !resource.isFull()) {
                return false;
            } else if (resourceEnergy.getEnergyAmount() >= input.getAmount() && input.isEnergyMatch(resourceEnergy.getEnergy())) {
                if (!input.isKeep() && !testOnly) {
                    resourceEnergy.setEnergyAmount(resource.getEnergyAmount() - input.getAmount());
                }

                if (cacheData != null) {
                    cacheData.energyConsumed = input.getAmount();
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean consumeInputEnergyFromItemInternal(
        InputScript input, InventoryItem inventoryItem, boolean testOnly, CraftRecipeData.CacheData cacheData
    ) {
        if (inventoryItem instanceof DrainableComboItem comboItem) {
            if (input.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                return false;
            }

            int useAmount = input.getAmount() > 0.0F ? (int)input.getAmount() : 1;
            if (comboItem.getCurrentUses() >= useAmount && input.isEnergyMatch(comboItem)) {
                if (!testOnly && !input.isKeep()) {
                    comboItem.setCurrentUses(comboItem.getCurrentUses() - useAmount);
                }

                if (cacheData != null) {
                    cacheData.energyConsumed = useAmount;
                }

                return true;
            }
        }

        return false;
    }

    protected static boolean createOutputToResource(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (output != null && resource != null && output.getResourceType() == resource.getType()) {
            switch (output.getResourceType()) {
                case Fluid:
                    return createOutputFluid(output, resource, testOnly, cacheData);
                case Energy:
                    return createOutputEnergy(output, resource, testOnly, cacheData);
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    protected static boolean createOutputItem(OutputScript output, Item item, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character) {
        if (!validateOutputScript(output, ResourceType.Item, testOnly)) {
            return false;
        } else {
            boolean success = createOutputItemInternal(output, item, testOnly, cacheData, character);
            if (success && !testOnly && output.hasCreateToItem() && cacheData.getMostRecentItem() != null) {
                OutputScript script = output.getCreateToItemScript();
                boolean b = false;
                InventoryItem inventoryItem = cacheData.getMostRecentItem();

                assert inventoryItem != null;

                switch (script.getResourceType()) {
                    case Item:
                        b = createOutputItemUsesInternal(script, inventoryItem, testOnly, cacheData);
                        break;
                    case Fluid:
                        b = createOutputFluidInternal(script, inventoryItem.getFluidContainer(), testOnly, cacheData);
                        break;
                    case Energy:
                        b = createOutputEnergyToItemInternal(script, inventoryItem, testOnly, cacheData);
                }

                if (!b) {
                    DebugLog.General.warn("unable to create uses/fluid/energy to item: " + (item != null ? item.getFullName() : "unknown"));
                }
            }

            if (success) {
                return true;
            } else {
                cacheData.softReset();
                return false;
            }
        }
    }

    private static boolean createOutputItemInternal(
        OutputScript output, Item item, boolean testOnly, CraftRecipeData.CacheData data, IsoGameCharacter character
    ) {
        if (!testOnly && output.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > output.getChance()) {
            return true;
        } else if (item != null && item.getFullName() != null) {
            if (!testOnly) {
                InventoryItem inventoryItem = InventoryItemFactory.CreateItem(item.getFullName());
                if (inventoryItem != null) {
                    if (data != null) {
                        data.addAppliedItem(inventoryItem);
                    }

                    if (output.hasFlag(OutputFlag.IsBlunt) && inventoryItem.hasSharpness()) {
                        inventoryItem.setSharpness(0.0F);
                    }

                    if (output.hasFlag(OutputFlag.HasOneUse)) {
                        inventoryItem.setCurrentUses(1);
                    }

                    if (output.hasFlag(OutputFlag.HasNoUses)) {
                        inventoryItem.setCurrentUses(0);
                    }

                    if (output.hasFlag(OutputFlag.SetActivated)) {
                        inventoryItem.setCurrentUses(0);
                    }

                    if (output.hasFlag(OutputFlag.EquipSecondary) && character != null) {
                        if (character.getPrimaryHandItem() == character.getSecondaryHandItem()) {
                            character.setPrimaryHandItem(null);
                        }

                        character.setSecondaryHandItem(inventoryItem);
                    }
                } else {
                    DebugLog.General.warn("Failed to create item: " + item.getFullName());
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static boolean createOutputItemUsesInternal(OutputScript output, InventoryItem targetItem, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (targetItem instanceof DrainableComboItem comboItem) {
            int stored = targetItem.getCurrentUses();
            if (stored > 0 && output.hasFlag(OutputFlag.ForceEmpty)) {
                stored = 0;
                if (!testOnly) {
                    comboItem.setCurrentUses(0);
                }
            }

            if (stored > 0 && output.hasFlag(OutputFlag.IsEmpty)) {
                return false;
            } else {
                int createAmount = output.getAmount() > 0.0F ? (int)output.getAmount() : 1;
                int amount = PZMath.min(createAmount, comboItem.getMaxUses() - stored);
                if (output.hasFlag(OutputFlag.AlwaysFill)) {
                    amount = comboItem.getMaxUses() - stored;
                }

                if (amount <= 0) {
                    return false;
                } else if (amount < createAmount && output.hasFlag(OutputFlag.RespectCapacity)) {
                    return false;
                } else if (!testOnly && output.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > output.getChance()) {
                    return true;
                } else {
                    if (!testOnly) {
                        comboItem.setCurrentUses(PZMath.min(comboItem.getCurrentUses() + amount, comboItem.getMaxUses()));
                    }

                    if (cacheData != null) {
                        cacheData.usesCreated = amount;
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static boolean createOutputFluid(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!validateOutputScript(output, ResourceType.Fluid, testOnly)) {
            return false;
        } else {
            return resource != null && resource.getType() == ResourceType.Fluid && !resource.isFull()
                ? createOutputFluidInternal(output, ((ResourceFluid)resource).getFluidContainer(), testOnly, cacheData)
                : false;
        }
    }

    private static boolean createOutputFluidInternal(OutputScript output, FluidContainer fc, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (output.getFluid() == null) {
            return false;
        } else if (fc != null && !fc.isFull()) {
            float stored = fc.getAmount();
            if (stored > 0.0F && output.hasFlag(OutputFlag.ForceEmpty)) {
                stored = 0.0F;
                if (!testOnly) {
                    fc.Empty();
                }
            }

            if (stored > 0.0F && output.hasFlag(OutputFlag.IsEmpty)) {
                return false;
            } else {
                float amount = PZMath.min(output.getAmount(), fc.getCapacity() - stored);
                if (output.hasFlag(OutputFlag.AlwaysFill)) {
                    amount = fc.getCapacity() - stored;
                }

                if (amount <= 0.0F) {
                    return false;
                } else if (amount < output.getAmount() && output.hasFlag(OutputFlag.RespectCapacity)) {
                    return false;
                } else if (!fc.canAddFluid(output.getFluid())) {
                    return false;
                } else if (!testOnly && output.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > output.getChance()) {
                    return true;
                } else {
                    if (!testOnly) {
                        fc.addFluid(output.getFluid(), amount);
                    }

                    if (cacheData != null) {
                        cacheData.fluidCreated = amount;
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private static boolean createOutputEnergy(OutputScript output, Resource resource, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (!validateOutputScript(output, ResourceType.Energy, testOnly)) {
            return false;
        } else if (resource != null && !resource.isFull()) {
            ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
            if (resourceEnergy.getEnergy() != null && resourceEnergy.getEnergy().equals(output.getEnergy())) {
                float stored = resource.getEnergyAmount();
                if (stored > 0.0F && output.hasFlag(OutputFlag.ForceEmpty)) {
                    stored = 0.0F;
                    if (!testOnly) {
                        resourceEnergy.setEnergyAmount(0.0F);
                    }
                }

                if (stored > 0.0F && output.hasFlag(OutputFlag.IsEmpty)) {
                    return false;
                } else {
                    float amount = PZMath.min(output.getAmount(), resourceEnergy.getEnergyCapacity() - stored);
                    if (output.hasFlag(OutputFlag.AlwaysFill)) {
                        amount = resourceEnergy.getEnergyCapacity() - stored;
                    }

                    if (amount < 0.0F) {
                        return false;
                    } else if (amount < output.getAmount() && output.hasFlag(OutputFlag.RespectCapacity)) {
                        return false;
                    } else if (!testOnly && output.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > output.getChance()) {
                        return true;
                    } else {
                        if (!testOnly) {
                            resourceEnergy.setEnergyAmount(resource.getEnergyAmount() + amount);
                        }

                        if (cacheData != null) {
                            cacheData.energyCreated = amount;
                        }

                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean createOutputEnergyToItemInternal(
        OutputScript output, InventoryItem targetItem, boolean testOnly, CraftRecipeData.CacheData cacheData
    ) {
        if (targetItem instanceof DrainableComboItem comboItem) {
            if (comboItem.isEnergy() && comboItem.getEnergy() != null && comboItem.getEnergy().equals(output.getEnergy())) {
                int stored = comboItem.getCurrentUses();
                if (stored > 0 && output.hasFlag(OutputFlag.ForceEmpty)) {
                    stored = 0;
                    if (!testOnly) {
                        comboItem.setCurrentUses(0);
                    }
                }

                if (stored > 0 && output.hasFlag(OutputFlag.IsEmpty)) {
                    return false;
                } else {
                    int createAmount = output.getAmount() > 0.0F ? (int)output.getAmount() : 1;
                    int amount = PZMath.min(createAmount, comboItem.getMaxUses() - stored);
                    if (output.hasFlag(OutputFlag.AlwaysFill)) {
                        amount = comboItem.getMaxUses() - stored;
                    }

                    if (amount <= 0) {
                        return false;
                    } else if (amount < createAmount && output.hasFlag(OutputFlag.RespectCapacity)) {
                        return false;
                    } else if (!testOnly && output.getChance() < 1.0F && Rand.Next(0.0F, 1.0F) > output.getChance()) {
                        return true;
                    } else {
                        if (!testOnly) {
                            comboItem.setCurrentUses(PZMath.min(comboItem.getCurrentUses() + amount, comboItem.getMaxUses()));
                        }

                        if (cacheData != null) {
                            cacheData.energyCreated = amount;
                        }

                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static ArrayList<CraftRecipe> getUniqueRecipeItems(InventoryItem item, IsoGameCharacter chr, ArrayList<ItemContainer> containers) {
        RecipeList.clear();
        List<CraftRecipe> allRecipes = queryRecipes("InHandCraft");
        HandcraftLogic logic = new HandcraftLogic(chr, null, null);
        logic.setContainers(containers);

        for (int i = 0; i < allRecipes.size(); i++) {
            CraftRecipe recipe = allRecipes.get(i);
            if (isValidRecipeForCharacter(recipe, chr, null, containers) && getValidInputScriptForItem(recipe, item) != null && recipe.OnTestItem(item)) {
                logic.setRecipeFromContextClick(recipe, item);
                if (logic.canPerformCurrentRecipe()) {
                    RecipeList.add(recipe);
                }
            }
        }

        return RecipeList;
    }

    private static class CraftRecipeListProvider implements TaggedObjectManager.BackingListProvider<CraftRecipe> {
        public ArrayList<CraftRecipe> getTaggedObjectList() {
            return ScriptManager.instance.getAllCraftRecipes();
        }
    }

    public static enum FilterMode {
        Name,
        ModName,
        Tags,
        InputName,
        OutputName;
    }
}
