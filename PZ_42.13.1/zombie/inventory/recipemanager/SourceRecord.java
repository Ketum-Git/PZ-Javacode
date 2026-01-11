// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.scripting.objects.Recipe;
import zombie.util.Type;

public class SourceRecord {
    private static final ArrayDeque<SourceRecord> pool = new ArrayDeque<>();
    private ItemRecipe itemRecipe;
    private int sourceIndex;
    private Recipe.Source recipeSource;
    private final ArrayList<SourceType> sourceTypes = new ArrayList<>();
    public final ArrayList<ItemRecord> itemRecords = new ArrayList<>();
    private final ArrayList<ItemRecord> collectedItemRecords = new ArrayList<>();
    private final ArrayList<InventoryItem> collectedItems = new ArrayList<>();

    protected static SourceRecord alloc(ItemRecipe itemRecipe, int sourceIndex, Recipe.Source recipeSource) {
        return pool.isEmpty() ? new SourceRecord().init(itemRecipe, sourceIndex, recipeSource) : pool.pop().init(itemRecipe, sourceIndex, recipeSource);
    }

    protected static void release(SourceRecord o) {
        assert !pool.contains(o);

        pool.push(o.reset());
    }

    private SourceRecord() {
    }

    private SourceRecord init(ItemRecipe itemRecipe, int sourceIndex, Recipe.Source recipeSource) {
        this.itemRecipe = itemRecipe;
        this.sourceIndex = sourceIndex;
        this.recipeSource = recipeSource;

        for (int i = 0; i < recipeSource.getItems().size(); i++) {
            String itemType = recipeSource.getItems().get(i);
            SourceType sourceType = SourceType.alloc(itemType);
            this.sourceTypes.add(sourceType);
        }

        return this;
    }

    private SourceRecord reset() {
        this.sourceIndex = -1;
        this.recipeSource = null;

        for (int i = 0; i < this.itemRecords.size(); i++) {
            ItemRecord.release(this.itemRecords.get(i));
        }

        this.itemRecords.clear();
        this.sourceTypes.clear();
        this.collectedItemRecords.clear();
        this.collectedItems.clear();
        return this;
    }

    protected void applyUses(UsedItemProperties usedItemProperties) {
        float usesRequired = this.getUsesRequired();
        RecipeMonitor.IncTab();
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[" + this.sourceIndex + "] SourceRecord uses required: " + usesRequired);
        }

        RecipeMonitor.IncTab();

        for (int i = 0; i < this.collectedItemRecords.size(); i++) {
            RecipeMonitor.IncTab();
            usesRequired = this.collectedItemRecords.get(i).applyUses(usesRequired, usedItemProperties);
            RecipeMonitor.DecTab();
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("uses remaining: " + usesRequired + ", applied: " + this.collectedItemRecords.get(i));
            }
        }

        RecipeMonitor.DecTab();
        if (usesRequired > 0.0F) {
            DebugLog.General.error("Uses required is '" + usesRequired + "', should be zero. Recipe = " + this.itemRecipe.getRecipeName());
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("Uses required is '" + usesRequired + "', should be zero. Recipe = " + this.itemRecipe.getRecipeName(), RecipeMonitor.colNeg);
            }
        }

        RecipeMonitor.DecTab();
    }

    protected ArrayList<InventoryItem> getCollectedItems() {
        return this.collectedItems;
    }

    protected boolean isDestroy() {
        return this.recipeSource.isDestroy();
    }

    protected boolean isKeep() {
        return this.recipeSource.isKeep();
    }

    protected boolean isUseIsItemCount() {
        return this.recipeSource.use <= 0.0F;
    }

    protected int getNumberOfTimesSourceCanBeDone() {
        float usesRequired = this.getUsesRequired();
        if (usesRequired <= 0.0F) {
            DebugLog.General.error("Uses required is zero?");
            return 0;
        } else {
            float uses = this.getUsesTotalSourceItems();
            return (int)(uses / usesRequired);
        }
    }

    protected float getUsesTotalSourceItems() {
        float uses = 0.0F;

        for (int i = 0; i < this.itemRecords.size(); i++) {
            uses += this.itemRecords.get(i).getUses();
        }

        return uses;
    }

    public float getUsesRequired() {
        float usesNeeded = this.recipeSource.getCount();
        if (this.recipeSource.use > 0.0F) {
            usesNeeded = this.recipeSource.getUse();
        }

        return usesNeeded;
    }

    protected boolean isValid() {
        return this.getUsesTotalSourceItems() >= this.getUsesRequired();
    }

    protected void clearCollectedItems() {
        this.collectedItemRecords.clear();
        this.collectedItems.clear();
    }

    protected void collectItems() {
        this.itemRecords.sort(Comparator.comparing(ItemRecord::getPriority));
        float usesRequired = this.getUsesRequired();
        RecipeMonitor.IncTab();
        if (RecipeMonitor.canLog()) {
            RecipeMonitor.Log("[" + this.sourceIndex + "] SourceRecord uses required: " + usesRequired);
        }

        RecipeMonitor.IncTab();
        ItemRecord data = null;

        for (int i = 0; i < this.itemRecords.size(); i++) {
            data = this.itemRecords.get(i);
            this.collectedItemRecords.add(data);
            this.collectedItems.add(data.getItem());
            usesRequired -= data.getUses();
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("uses remaining: " + usesRequired + ", added: " + data);
            }

            if (usesRequired <= 0.0F) {
                break;
            }
        }

        if (usesRequired > 0.0F) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("SourceRecord not satisfied, uses remaining: " + usesRequired, RecipeMonitor.colNeg);
            }

            this.collectedItemRecords.clear();
            this.collectedItems.clear();
        }

        RecipeMonitor.DecTab();
        RecipeMonitor.DecTab();
    }

    protected boolean assignItemRecord(ItemRecord data) {
        for (int i = 0; i < this.sourceTypes.size(); i++) {
            if (this.isValidSourceItem(this.sourceTypes.get(i), data)) {
                data.setSource(this, this.sourceTypes.get(i));
                this.itemRecords.add(data);
                return true;
            }
        }

        return false;
    }

    private boolean isValidSourceItem(SourceType sourceType, ItemRecord data) {
        InventoryItem item = data.getItem();
        boolean isDrainable = item instanceof DrainableComboItem;
        Food food = Type.tryCastTo(item, Food.class);
        if (sourceType.isUsesFluid()) {
            if (item.getFluidContainer() != null
                && sourceType.getSourceFluid() != null
                && !item.getFluidContainer().isEmpty()
                && item.getFluidContainer().isPerceivedFluidToPlayer(sourceType.getSourceFluid(), this.itemRecipe.getCharacter())) {
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("valid: fluid container, " + sourceType.getSourceFluid(), RecipeMonitor.colPos);
                }

                return true;
            } else {
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("failed: invalid or no fluid container, " + sourceType.getSourceFluid() + ", " + sourceType, RecipeMonitor.colNeg);
                }

                return false;
            }
        } else if ("Water".equals(sourceType.getItemType()) && item instanceof DrainableComboItem && item.isWaterSource()) {
            return true;
        } else if (!sourceType.getItemType().equals(item.getFullType())) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("failed: not equals [" + sourceType.getItemType() + "], " + sourceType, RecipeMonitor.colNeg);
            }

            return false;
        } else if (!this.itemRecipe.testItem(item)) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("failed: testItem(), " + sourceType, RecipeMonitor.colNeg);
            }

            return false;
        } else if (this.itemRecipe.getRecipe().getHeat() > 0.0F
            && isDrainable
            && item.isCookable()
            && item.getInvHeat() + 1.0F < this.itemRecipe.getRecipe().getHeat()) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("failed: getHeat > 0, " + sourceType, RecipeMonitor.colNeg);
            }

            return false;
        } else if (this.itemRecipe.getRecipe().getHeat() < 0.0F
            && isDrainable
            && item.isCookable()
            && item.getInvHeat() > this.itemRecipe.getRecipe().getHeat()) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("failed: getHeat < 0, " + sourceType, RecipeMonitor.colNeg);
            }

            return false;
        } else if ("Clothing".equals(item.getCategory()) && item.isFavorite()) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("failed: is favorite clothing, " + sourceType, RecipeMonitor.colNeg);
            }

            return false;
        } else if (this.isDestroy()) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("valid: is destroy", RecipeMonitor.colPos);
            }

            return true;
        } else if (isDrainable) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("valid: isDrainable", RecipeMonitor.colPos);
            }

            return true;
        } else if (this.recipeSource.use > 0.0F) {
            if (food != null) {
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("valid: use > 0 [food]", RecipeMonitor.colPos);
                }

                return true;
            } else {
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("invalid, " + sourceType, RecipeMonitor.colNeg);
                }

                return false;
            }
        } else {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("valid: generic", RecipeMonitor.colPos);
            }

            return true;
        }
    }
}
