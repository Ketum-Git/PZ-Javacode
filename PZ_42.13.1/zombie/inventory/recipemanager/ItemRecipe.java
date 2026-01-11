// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.core.Colors;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.RecipeManager;
import zombie.scripting.objects.Recipe;

public class ItemRecipe {
    public static final String FLUID_PREFIX = "Fluid.";
    private static final ArrayDeque<ItemRecipe> pool = new ArrayDeque<>();
    private Recipe recipe;
    private IsoGameCharacter character;
    private InventoryItem selectedItem;
    private boolean allItems;
    private boolean valid;
    private boolean hasCollectedSources;
    private final UsedItemProperties usedItemProperties = new UsedItemProperties();
    private final ArrayList<SourceRecord> sourceRecords = new ArrayList<>();
    private final ArrayList<InventoryItem> allSourceItems = new ArrayList<>();
    private final ArrayList<InventoryItem> allResultItems = new ArrayList<>();
    private ArrayList<InventoryItem>[] resultsPerType;

    public static int getNumberOfTimesRecipeCanBeDone(Recipe recipe, IsoGameCharacter chr, ArrayList<ItemContainer> containers, InventoryItem selectedItem) {
        RecipeMonitor.suspend();
        ItemRecipe itemRecipe = Alloc(recipe, chr, containers, selectedItem, null, true);
        int times = itemRecipe.getNumberOfTimesRecipeCanBeDone();
        Release(itemRecipe);
        RecipeMonitor.resume();
        return times;
    }

    public static ItemRecipe Alloc(
        Recipe recipe,
        IsoGameCharacter character,
        ArrayList<ItemContainer> containers,
        InventoryItem selectedItem,
        ArrayList<InventoryItem> ignoreItems,
        boolean allItems
    ) {
        if (selectedItem != null && (selectedItem.getContainer() == null || !selectedItem.getContainer().contains(selectedItem))) {
            DebugLog.Recipe.warn("recipe: item appears to have been used already, ignoring " + selectedItem.getFullType());
            selectedItem = null;
        }

        if (containers == null) {
            containers = new ArrayList<>();
            containers.add(character.getInventory());
        }

        if (selectedItem != null && !RecipeManager.validateRecipeContainsSourceItem(recipe, selectedItem)) {
            throw new RuntimeException("item " + selectedItem.getFullType() + " isn't used in recipe " + recipe.getOriginalname());
        } else {
            RecipeMonitor.LogInit(recipe, character, containers, selectedItem, ignoreItems, allItems);
            ItemRecipe itemRecipe = pool.isEmpty() ? new ItemRecipe() : pool.pop();
            itemRecipe.init(recipe, character, containers, selectedItem, ignoreItems, allItems);
            return itemRecipe;
        }
    }

    public static void Release(ItemRecipe o) {
        assert !pool.contains(o);

        pool.push(o.reset());
    }

    private ItemRecipe() {
        this.ensureResultsPerType(20);
    }

    protected Recipe getRecipe() {
        return this.recipe;
    }

    protected IsoGameCharacter getCharacter() {
        return this.character;
    }

    protected InventoryItem getSelectedItem() {
        return this.selectedItem;
    }

    protected boolean isValid() {
        return this.valid;
    }

    private void ensureResultsPerType(int amount) {
        if (this.resultsPerType == null || this.resultsPerType.length < amount) {
            this.resultsPerType = new ArrayList[amount];

            for (int i = 0; i < this.resultsPerType.length; i++) {
                this.resultsPerType[i] = new ArrayList<>();
            }
        }
    }

    protected String getRecipeName() {
        return this.recipe != null ? this.recipe.getName() : "Recipe.Null";
    }

    private ItemRecipe reset() {
        this.recipe = null;
        this.character = null;
        this.selectedItem = null;
        this.allItems = false;
        this.valid = false;
        this.hasCollectedSources = false;
        this.usedItemProperties.reset();
        this.allSourceItems.clear();

        for (int i = 0; i < this.sourceRecords.size(); i++) {
            SourceRecord.release(this.sourceRecords.get(i));
        }

        this.sourceRecords.clear();
        this.allResultItems.clear();
        if (this.resultsPerType != null) {
            for (int i = 0; i < this.resultsPerType.length; i++) {
                this.resultsPerType[i].clear();
            }
        }

        return this;
    }

    private void init(
        Recipe recipe,
        IsoGameCharacter character,
        ArrayList<ItemContainer> containers,
        InventoryItem selectedItem,
        ArrayList<InventoryItem> ignoreItems,
        boolean allItems
    ) {
        this.recipe = recipe;
        this.character = character;
        this.selectedItem = selectedItem;
        this.allItems = allItems;

        for (int i = 0; i < recipe.getSource().size(); i++) {
            SourceRecord sourceRecord = SourceRecord.alloc(this, i, recipe.getSource().get(i));
            this.sourceRecords.add(sourceRecord);
        }

        RecipeMonitor.LogSources(recipe.getSource());
        if (!this.testItem(selectedItem)) {
            RecipeMonitor.Log("SelectedItem testItem() failed, aborting.", RecipeMonitor.colNeg);
        } else {
            RecipeMonitor.LogBlanc();
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("[ParseItems]", RecipeMonitor.colHeader);
            }

            RecipeMonitor.IncTab();

            for (int i = 0; i < containers.size(); i++) {
                ItemContainer container = containers.get(i);
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log(RecipeMonitor.getContainerString(container));
                }

                RecipeMonitor.IncTab();

                for (int j = 0; j < container.getItems().size(); j++) {
                    InventoryItem item = container.getItems().get(j);
                    if (RecipeMonitor.canLog()) {
                        RecipeMonitor.Log("Parsing Item = " + item, RecipeMonitor.colGray);
                    }

                    RecipeMonitor.IncTab();
                    if (selectedItem == null || selectedItem.getContainer() != null && selectedItem.getContainer().contains(selectedItem)) {
                        if (ignoreItems != null && ignoreItems.contains(item)) {
                            if (RecipeMonitor.canLog()) {
                                RecipeMonitor.Log(" -> skipping ignored item", RecipeMonitor.colNeg);
                            }
                        } else if ((selectedItem == null || selectedItem != item) && character.isEquippedClothing(item)) {
                            if (RecipeMonitor.canLog()) {
                                RecipeMonitor.Log(" -> skipping, equipped clothing", RecipeMonitor.colNeg);
                            }

                            RecipeMonitor.DecTab();
                        } else {
                            ItemRecord data = ItemRecord.alloc(this, item);
                            boolean assigned = false;

                            for (int k = 0; k < this.sourceRecords.size(); k++) {
                                if (this.sourceRecords.get(k).assignItemRecord(data)) {
                                    if (RecipeMonitor.canLog()) {
                                        RecipeMonitor.Log(" -> assigned to source [" + k + "] : " + data.getSourceType());
                                    }

                                    assigned = true;
                                    break;
                                }
                            }

                            if (!assigned) {
                                ItemRecord.release(data);
                            }

                            RecipeMonitor.DecTab();
                        }
                    } else if (RecipeMonitor.canLog()) {
                        RecipeMonitor.Log(" -> skipping, appears to have been used already", RecipeMonitor.colNeg);
                    }
                }

                RecipeMonitor.DecTab();
            }

            RecipeMonitor.DecTab();
            this.valid = true;

            for (int i = 0; i < this.sourceRecords.size(); i++) {
                SourceRecord sourceRecord = this.sourceRecords.get(i);
                if (!sourceRecord.isValid()) {
                    this.valid = false;
                    break;
                }
            }

            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("Recipe valid = " + this.valid, this.valid ? RecipeMonitor.colPos : RecipeMonitor.colNeg);
            }

            if (RecipeMonitor.canLog()) {
                RecipeManager.printDebugRecipeValid(recipe, character, null, containers);
            }

            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("debug total times recipe can be done = " + this.getNumberOfTimesRecipeCanBeDone(), Colors.Magenta);
            }
        }
    }

    private void collectSourceItems() {
        if (!this.hasCollectedSources) {
            RecipeMonitor.LogBlanc();
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("[CollectingSourceItems]", RecipeMonitor.colHeader);
            }

            this.hasCollectedSources = true;
            if (this.allItems || this.isValid()) {
                boolean hasEmptySource = false;

                for (int i = 0; i < this.sourceRecords.size(); i++) {
                    SourceRecord sourceRecord = this.sourceRecords.get(i);
                    sourceRecord.collectItems();
                    if (sourceRecord.getCollectedItems().isEmpty()) {
                        hasEmptySource = true;
                    }
                }

                if (!this.allItems && hasEmptySource) {
                    if (RecipeMonitor.canLog()) {
                        RecipeMonitor.Log("allItems==true and not all sources satisfied, clearing", RecipeMonitor.colNeg);
                    }

                    for (int ix = 0; ix < this.sourceRecords.size(); ix++) {
                        SourceRecord sourceRecord = this.sourceRecords.get(ix);
                        sourceRecord.clearCollectedItems();
                    }
                }

                this.allSourceItems.clear();

                for (int ix = 0; ix < this.sourceRecords.size(); ix++) {
                    this.allSourceItems.addAll(this.sourceRecords.get(ix).getCollectedItems());
                }

                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("total items = " + this.allSourceItems.size());
                }
            }
        }
    }

    private int getNumberOfTimesRecipeCanBeDone() {
        if (!this.isValid()) {
            return 0;
        } else {
            int times = Integer.MAX_VALUE;

            for (int i = 0; i < this.sourceRecords.size(); i++) {
                int sourceTimes = this.sourceRecords.get(i).getNumberOfTimesSourceCanBeDone();
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("times source [" + i + "] can be done = " + sourceTimes, Colors.Magenta);
                }

                times = PZMath.min(times, sourceTimes);
                if (times <= 0) {
                    return 0;
                }
            }

            if (times == Integer.MAX_VALUE) {
                times = 0;
            }

            return times;
        }
    }

    public ArrayList<InventoryItem> perform() {
        this.collectSourceItems();
        RecipeMonitor.LogBlanc();
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[Perform]", RecipeMonitor.colHeader);
        }

        if (this.allSourceItems.isEmpty()) {
            throw new RuntimeException("collectSourceItems() didn't return the required number of items");
        } else if (this.allItems) {
            throw new RuntimeException("allItems = true, while attempting a recipe 'perform'");
        } else {
            this.character.removeFromHands(this.selectedItem);

            for (int i = 0; i < this.sourceRecords.size(); i++) {
                SourceRecord sourceRecord = this.sourceRecords.get(i);
                sourceRecord.applyUses(this.usedItemProperties);
            }

            return null;
        }
    }

    public ArrayList<InventoryItem> getSourceItems() {
        this.collectSourceItems();
        ArrayList<InventoryItem> items = new ArrayList<>();
        items.addAll(this.allSourceItems);
        return items;
    }

    public ArrayList<InventoryItem> getSourceItems(int sourceIndex) {
        this.collectSourceItems();
        if (sourceIndex >= 0 && sourceIndex < this.sourceRecords.size()) {
            ArrayList<InventoryItem> items = new ArrayList<>();
            items.addAll(this.sourceRecords.get(sourceIndex).getCollectedItems());
            return items;
        } else {
            DebugLog.General.error("Index '" + sourceIndex + "' is not valid for recipe '" + this.recipe.getName() + "'.");
            return null;
        }
    }

    protected boolean testItem(InventoryItem item) {
        return true;
    }
}
