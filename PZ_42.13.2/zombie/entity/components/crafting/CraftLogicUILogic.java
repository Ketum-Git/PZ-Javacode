// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeListNodeCollection;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.InputItemNode;
import zombie.entity.components.crafting.recipe.InputItemNodeCollection;
import zombie.entity.components.crafting.recipe.ItemDataList;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceItem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

@UsedFromLua
public class CraftLogicUILogic {
    private final IsoPlayer player;
    private final GameEntity entity;
    private final CraftLogic component;
    private CraftRecipe selectedRecipe;
    private final CraftRecipeListNodeCollection filteredRecipeList = new CraftRecipeListNodeCollection();
    private final CraftLogicUILogic.RecipeComparator recipeComparator;
    private String filterString;
    InputItemNodeCollection inputItemNodeCollection = new InputItemNodeCollection(true, false);
    InputItemNodeCollection resourceItemNodeCollection = new InputItemNodeCollection(false, true);
    protected final HashMap<String, ArrayList<BaseCraftingLogic.CraftEventHandler>> events = new HashMap<>();
    private boolean showManualSelectInputs;
    private InputScript manualSelectInputScriptFilter;
    private KahluaTable manualSelectItemSlot;
    private int cachedPossibleCraftCount = -1;
    private final ArrayList<ItemContainer> containers = new ArrayList<>();
    protected final ArrayList<InventoryItem> allItems = new ArrayList<>();

    public CraftLogicUILogic(IsoPlayer player, GameEntity entity, CraftLogic component) {
        this.player = player;
        this.entity = entity;
        this.component = component;
        this.recipeComparator = new CraftLogicUILogic.RecipeComparator(player);
        this.inputItemNodeCollection.setCharacter(player);
        this.registerEvent("onRecipeChanged");
        this.registerEvent("onRebuildInputItemNodes");
        this.registerEvent("onInputsChanged");
        this.registerEvent("onUpdateRecipeList");
        this.registerEvent("onShowManualSelectChanged");
        this.registerEvent("onResourceSlotContentsChanged");
        this.filterRecipeList(this.filterString, null, true);
        this.selectedRecipe = component.getCurrentRecipe();
        if (this.selectedRecipe == null) {
            this.selectedRecipe = this.filteredRecipeList.getFirstRecipe();
            component.getCraftTestData().setRecipe(this.selectedRecipe);
        }

        this.inputItemNodeCollection.setRecipe(this.selectedRecipe);
        this.resourceItemNodeCollection.setRecipe(this.selectedRecipe);
        this.setSortModeInternal(this.getRecipeSortMode());
    }

    public CraftLogic getCraftLogic() {
        return this.component;
    }

    public GameEntity getEntity() {
        return this.entity;
    }

    public void setRecipe(CraftRecipe recipe) {
        if (recipe != this.selectedRecipe) {
            this.selectedRecipe = recipe;
            this.component.getCraftTestData().setRecipe(recipe);
            this.inputItemNodeCollection.setRecipe(recipe);
            this.resourceItemNodeCollection.setRecipe(recipe);
            this.triggerEvent("onRecipeChanged", recipe);
            this.triggerEvent("onRebuildInputItemNodes");
        }
    }

    public CraftRecipe getRecipe() {
        return this.selectedRecipe;
    }

    public CraftRecipeListNodeCollection getRecipeList() {
        return this.filteredRecipeList;
    }

    public boolean cachedCanStart(IsoPlayer player) {
        return this.component.canStart(player);
    }

    private void registerEvent(String eventName) {
        this.events.put(eventName, new ArrayList<>());
    }

    public void addEventListener(String event, Object function) {
        this.addEventListener(event, function, null);
    }

    public void addEventListener(String event, Object function, Object targetTable) {
        if (this.events.containsKey(event)) {
            this.events.get(event).add(new BaseCraftingLogic.CraftEventHandler(function, targetTable));
            if (Core.debug && this.events.get(event).size() > 10) {
                throw new RuntimeException("Sanity check, event '" + event + "' has >10 listeners");
            }
        } else {
            DebugLog.General.warn("Event '" + event + "' is unknown.");
        }
    }

    protected void triggerEvent(String event, Object... args) {
        if (this.events.containsKey(event)) {
            ArrayList<BaseCraftingLogic.CraftEventHandler> handlers = this.events.get(event);

            for (int i = 0; i < handlers.size(); i++) {
                BaseCraftingLogic.CraftEventHandler handler = handlers.get(i);

                try {
                    if (handler.targetTable != null) {
                        Object[] params = new Object[args.length + 1];
                        System.arraycopy(args, 0, params, 1, args.length);
                        params[0] = handler.targetTable;
                        LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), handler.function, params);
                    } else {
                        LuaManager.caller.protectedCallVoid(UIManager.getDefaultThread(), handler.function, args);
                    }
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }
        } else {
            DebugLog.General.warn("Event '" + event + "' is unknown.");
        }
    }

    public Texture getEntityIcon() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript(this.entity.getEntityFullTypeDebug());
        CraftRecipeComponentScript recipeScript = script.getComponentScriptFor(ComponentType.CraftRecipe);
        return recipeScript.getIconTexture();
    }

    public void setSelectedRecipeStyle(String style) {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeView";
        this.player.getModData().rawset(modString, style);
    }

    public String getSelectedRecipeStyle() {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeView";
        return (String)this.player.getModData().rawget(modString);
    }

    public void setRecipeSortMode(String sortMode) {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeSort";
        this.player.getModData().rawset(modString, sortMode);
        this.setSortModeInternal(sortMode);
    }

    public String getRecipeSortMode() {
        String modString = this.entity.getEntityFullTypeDebug() + "RecipeSort";
        String sortMode = (String)this.player.getModData().rawget(modString);
        if (sortMode == null) {
            sortMode = "RecipeName";
        }

        return sortMode;
    }

    protected void setSortModeInternal(String sortMode) {
        switch (sortMode) {
            case "LastUsed":
                this.recipeComparator.compareMode = CraftLogicUILogic.RecipeComparator.CompareMode.LAST_USED;
                break;
            case "MostUsed":
                this.recipeComparator.compareMode = CraftLogicUILogic.RecipeComparator.CompareMode.MOST_USED;
                break;
            default:
                this.recipeComparator.compareMode = CraftLogicUILogic.RecipeComparator.CompareMode.NAME;
        }
    }

    public void filterRecipeList(String filter, String categoryFilter) {
        this.filterRecipeList(filter, categoryFilter, false);
    }

    public void filterRecipeList(String filter, String categoryFilter, boolean force) {
        this.filterRecipeList(filter, categoryFilter, force, LuaManager.GlobalObject.getSpecificPlayer(0));
    }

    public void filterRecipeList(String filter, String categoryFilter, boolean force, IsoPlayer player) {
        ArrayList<CraftRecipe> recipesKnown = new ArrayList<>();
        if (player == null) {
            player = LuaManager.GlobalObject.getSpecificPlayer(0);
        }

        if (player == null) {
            recipesKnown.addAll(this.component.getRecipes());
        } else {
            for (int i = 0; i < this.component.getRecipes().size(); i++) {
                CraftRecipe testRecipe = this.component.getRecipes().get(i);
                if (!testRecipe.needToBeLearn() || CraftRecipeManager.hasPlayerLearnedRecipe(testRecipe, player)) {
                    recipesKnown.add(testRecipe);
                }
            }
        }

        boolean filterChanged = this.filterString != null && !this.filterString.equals(filter) || filter != null && !filter.equals(this.filterString);
        if (filterChanged || force) {
            this.filterString = filter;
            BaseCraftingLogic.filterAndSortRecipeList(filter, null, this.filteredRecipeList, recipesKnown, player, this.recipeComparator);
            this.triggerEvent("onUpdateRecipeList", this.filteredRecipeList);
        }
    }

    public void sortRecipeList() {
        this.filterRecipeList(this.filterString, null, true);
    }

    public int getPossibleCraftCount(boolean forceRecache) {
        if (forceRecache || this.cachedPossibleCraftCount == -1) {
            CraftRecipeData recipeData = this.getCraftLogic().getCraftTestData();
            this.cachedPossibleCraftCount = recipeData.getPossibleCraftCount(
                this.getCraftLogic().getInputResources(), null, new ArrayList<>(), new ArrayList<>(), false
            );
            int freeOutputSlots = this.getCraftLogic().getFreeOutputSlotCount();
            this.cachedPossibleCraftCount = Math.min(this.cachedPossibleCraftCount, freeOutputSlots);
        }

        return this.cachedPossibleCraftCount;
    }

    public KahluaTable getItemsInProgress() {
        KahluaTable output = LuaManager.platform.newTable();
        if (this.component.isRunning()) {
            for (CraftRecipeData craftRecipeData : this.component.getAllInProgressCraftData()) {
                ArrayList<InventoryItem> inputItems = craftRecipeData.getAllInputItems();

                for (int i = 0; i < inputItems.size(); i++) {
                    InventoryItem item = inputItems.get(i);
                    if (item != null) {
                        KahluaTable subTable = (KahluaTable)output.rawget(craftRecipeData);
                        if (subTable == null) {
                            subTable = LuaManager.platform.newTable();
                            output.rawset(craftRecipeData, subTable);
                        }

                        int currentCount = subTable.rawget(item.getScriptItem()) != null ? (Integer)subTable.rawget(item.getScriptItem()) : 0;
                        subTable.rawset(item.getScriptItem(), currentCount + 1);
                    }
                }
            }
        }

        return output;
    }

    public ArrayList<Texture> getStatusIconsForItemInProgress(InventoryItem item, CraftRecipeData craftRecipeData) {
        return this.component.isRunning() ? this.component.getStatusIconsForInputItem(item, craftRecipeData) : null;
    }

    public KahluaTable getOutputItems() {
        KahluaTable output = LuaManager.platform.newTable();
        if (this.cachedCanStart(this.player)) {
            ItemDataList outputItems = this.component.getCraftTestData().getToOutputItems();

            for (int i = 0; i < outputItems.size(); i++) {
                Item item = outputItems.getItem(i);
                if (item != null) {
                    double count = output.rawget(item) != null ? (Double)output.rawget(item) : 0.0;
                    output.rawset(item, count + 1.0);
                }
            }
        }

        return output;
    }

    public boolean shouldShowManualSelectInputs() {
        return this.showManualSelectInputs;
    }

    public void setShowManualSelectInputs(boolean b) {
        if (this.showManualSelectInputs != b) {
            this.showManualSelectInputs = b;
            this.triggerEvent("onShowManualSelectChanged", this.shouldShowManualSelectInputs());
        }
    }

    public InputScript getManualSelectInputScriptFilter() {
        return this.manualSelectInputScriptFilter;
    }

    public KahluaTable getManualSelectItemSlot() {
        return this.manualSelectItemSlot;
    }

    public void setManualSelectInputScriptFilter(InputScript script, KahluaTable itemSlot) {
        this.manualSelectInputScriptFilter = script;
        this.manualSelectItemSlot = itemSlot;
        ArrayList<InventoryItem> storedItems = new ArrayList<>();
        if (itemSlot != null) {
            ResourceItem resource = (ResourceItem)itemSlot.rawget("resource");
            storedItems = resource.getStoredItems();
        }

        this.resourceItemNodeCollection.setItems(storedItems);
        this.triggerEvent("onRebuildInputItemNodes");
    }

    public CraftRecipeData getRecipeData() {
        return this.getCraftLogic().getCraftTestData();
    }

    public ArrayList<InputItemNode> getInputItemNodes() {
        return this.inputItemNodeCollection.getInputItemNodes();
    }

    public ArrayList<InputItemNode> getInputItemNodesForInput(InputScript input) {
        return this.inputItemNodeCollection.getInputItemNodesForInput(input);
    }

    public ArrayList<InputItemNode> getResourceItemNodes() {
        return this.resourceItemNodeCollection.getInputItemNodes();
    }

    public void onResourceSlotContentsChanged() {
        ArrayList<InventoryItem> storedItems = new ArrayList<>();
        if (this.manualSelectItemSlot != null) {
            ResourceItem resource = (ResourceItem)this.manualSelectItemSlot.rawget("resource");
            storedItems = resource.getStoredItems();
        }

        this.resourceItemNodeCollection.setItems(storedItems);
        this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
        this.triggerEvent("onResourceSlotContentsChanged");
    }

    public void setCraftQuantity(int quantity) {
    }

    public void setContainers(ArrayList<ItemContainer> containersToUse) {
        this.containers.clear();
        this.containers.addAll(containersToUse);
        this.allItems.clear();
        CraftRecipeManager.getAllItemsFromContainers(this.containers, this.allItems);
        this.inputItemNodeCollection.setItems(this.allItems);
        this.triggerEvent("onUpdateContainers");
        this.triggerEvent("onRebuildInputItemNodes");
    }

    public ArrayList<ItemContainer> getContainers() {
        return this.containers;
    }

    public void doProgressSlotTooltip(KahluaTable itemSlot, ObjectTooltip tooltipUI) {
        int offsetY = 0;
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        int y = tooltipUI.padTop + 0;
        String s = Translator.getText("EC_CraftLogic_Progress");
        tooltipUI.DrawText(font, s, tooltipUI.padLeft, y, 1.0, 1.0, 0.8F, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        y += lineSpacing + 5;
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        Resource resource = (Resource)itemSlot.rawget("resource");
        CraftRecipeData recipeData = (CraftRecipeData)itemSlot.rawget("craftRecipeData");
        this.getCraftLogic().doProgressTooltip(layout, resource, recipeData);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public void doPreviewSlotTooltip(KahluaTable itemSlot, ObjectTooltip tooltipUI) {
        int offsetY = 0;
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int lineSpacing = tooltipUI.getLineSpacing();
        Item scriptItem = (Item)itemSlot.rawget("storedScriptItem");
        int y = tooltipUI.padTop + 0;
        String s = scriptItem != null ? scriptItem.getDisplayName() : Translator.getText("EC_CraftLogicTooltip_NoOutput");
        tooltipUI.DrawText(font, s, tooltipUI.padLeft, y, 1.0, 1.0, 0.8F, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        y += lineSpacing;
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public boolean cachedCanPerformCurrentRecipe() {
        return this.cachedCanStart(this.player);
    }

    public boolean areAllInputItemsSatisfied() {
        return this.component.getCraftTestData().areAllInputItemsSatisfied();
    }

    public static class RecipeComparator implements Comparator<CraftRecipe> {
        private final IsoPlayer player;
        public CraftLogicUILogic.RecipeComparator.CompareMode compareMode = CraftLogicUILogic.RecipeComparator.CompareMode.NAME;

        public RecipeComparator(IsoPlayer player) {
            this.player = player;
        }

        public int compare(CraftRecipe v1, CraftRecipe v2) {
            int nameCompareResult = v1.getTranslationName().compareTo(v2.getTranslationName());
            switch (this.compareMode) {
                case LAST_USED:
                    double v1LastCraftTime = this.player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getLastCraftTime();
                    double v2LastCraftTime = this.player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getLastCraftTime();
                    int lastUsedCompareResult = Double.compare(v2LastCraftTime, v1LastCraftTime);
                    if (lastUsedCompareResult == 0) {
                        return nameCompareResult;
                    }

                    return lastUsedCompareResult;
                case MOST_USED:
                    int v1CraftCount = this.player.getPlayerCraftHistory().getCraftHistoryFor(v1.getName()).getCraftCount();
                    int v2CraftCount = this.player.getPlayerCraftHistory().getCraftHistoryFor(v2.getName()).getCraftCount();
                    int mostUsedCompareResult = Integer.compare(v2CraftCount, v1CraftCount);
                    if (mostUsedCompareResult == 0) {
                        return nameCompareResult;
                    }

                    return mostUsedCompareResult;
                default:
                    return v1.getTranslationName().compareTo(v2.getTranslationName());
            }
        }

        public static enum CompareMode {
            NAME,
            LAST_USED,
            MOST_USED;
        }
    }
}
