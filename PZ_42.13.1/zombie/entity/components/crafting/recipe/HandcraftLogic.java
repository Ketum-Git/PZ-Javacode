// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.textures.Texture;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.resources.Resource;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;

@UsedFromLua
public class HandcraftLogic extends BaseCraftingLogic {
    private static final int workstationInteractDistance = 3;
    private boolean craftActionInProgress;
    private int numCraftActionInProgress;
    private KahluaTableImpl craftActionTable;
    private final ArrayList<HandcraftLogic.CachedRecipeInfo> cachedRecipeInfos = new ArrayList<>();
    private final HashMap<CraftRecipe, HandcraftLogic.CachedRecipeInfo> cachedRecipeInfoMap = new HashMap<>();

    public HandcraftLogic(IsoGameCharacter player, CraftBench craftBench, IsoObject isoObject) {
        super(player, craftBench);
        this.isoObject = isoObject;
        if (this.craftBench != null) {
            this.sourceResources.addAll(craftBench.getResources());
        }

        this.registerEvent("onRecipeChanged");
        this.registerEvent("onStartCraft");
        this.registerEvent("onStopCraft");
        this.setSortModeInternal(this.getRecipeSortMode());
        this.setManualSelectInputs(this.getLastManualInputMode());
    }

    public IsoGameCharacter getPlayer() {
        return this.player;
    }

    public CraftBench getCraftBench() {
        return this.craftBench;
    }

    public IsoObject getIsoObject() {
        return this.isoObject;
    }

    public CraftRecipeData getRecipeData() {
        return this.recipeData;
    }

    public ArrayList<Resource> getSourceResources() {
        return this.sourceResources;
    }

    public CraftRecipeListNodeCollection getRecipeList() {
        return this.filteredRecipeList;
    }

    public ArrayList<InventoryItem> getAllItems() {
        return this.allItems;
    }

    public void startCraftAction(KahluaTableImpl actionTable) {
        this.craftActionInProgress = true;
        this.craftActionTable = actionTable;
        this.numCraftActionInProgress++;
        if (this.getRecipe() == actionTable.rawget("craftRecipe")) {
            List<InventoryItem> items = (List<InventoryItem>)actionTable.rawget("items");
            if (items != null) {
                this.populateInputs(items, null, true);
            }
        }

        this.triggerEvent("onStartCraft", actionTable);
    }

    public void stopCraftAction() {
        if (GameClient.client) {
            if (this.numCraftActionInProgress > 0) {
                this.numCraftActionInProgress--;
            }

            if (this.numCraftActionInProgress == 0) {
                this.craftActionInProgress = false;
                this.craftActionTable = null;
            }
        } else {
            this.craftActionInProgress = false;
            this.craftActionTable = null;
        }

        this.triggerEvent("onStopCraft");
    }

    public float getResidualFluidFromInput(InputScript inputScript) {
        if (inputScript != null && this.recipeData != null) {
            ArrayList<InputScript> inputScripts = inputScript.getParentRecipe().getInputs();

            for (int i = 0; i < inputScripts.size(); i++) {
                if (inputScripts.get(i).getConsumeFromItemScript() == inputScript) {
                    CraftRecipeData.InputScriptData inputScriptData = this.recipeData.getDataForInputScript(inputScripts.get(i));
                    if (inputScriptData != null) {
                        return inputScriptData.getInputItemFluidUses() - inputScript.getAmount();
                    }
                }
            }

            return 0.0F;
        } else {
            return 0.0F;
        }
    }

    public boolean isCraftActionInProgress() {
        return this.craftActionInProgress;
    }

    public KahluaTableImpl getCraftActionTable() {
        return this.craftActionTable;
    }

    public boolean performCurrentRecipe() {
        if (!this.isContainersAccessible(this.containers)) {
            return false;
        } else {
            this.updateFloorContainer(this.containers);
            return this.recipeData.perform(this.player, this.sourceResources, this.isManualSelectInputs() ? null : this.allItems, this.containers);
        }
    }

    @Override
    public void setRecipe(CraftRecipe recipe) {
        if (this.recipeData.getRecipe() != recipe) {
            this.setLastSelectedRecipe(recipe);
            super.setRecipe(recipe);
        }
    }

    public void setRecipeFromContextClick(CraftRecipe recipe, InventoryItem inventoryItem) {
        this.setManualSelectInputs(true);
        this.recipeData.setRecipe(recipe);
        this.recipeData.offerAndReplaceInputItem(inventoryItem);
        this.inputItemNodeCollection.setRecipe(recipe);
        this.populateInputs(this.allItems, this.sourceResources, false);
        this.recipeData.canConsumeInputs(this.sourceResources);
    }

    public void checkValidRecipeSelected() {
        if (!this.filteredRecipeList.contains(this.recipeData.getRecipe())) {
            if (!this.filteredRecipeList.isEmpty()) {
                this.setRecipe(this.filteredRecipeList.getFirstRecipe());
            } else {
                this.setRecipe(null);
            }
        }
    }

    @Override
    public void setRecipes(List<CraftRecipe> recipes) {
        super.setRecipes(recipes);
        if (!this.filteredRecipeList.isEmpty() && (this.getRecipe() == null || !this.filteredRecipeList.contains(this.getRecipe()))) {
            CraftRecipe bestRecipe = this.getLastSelectedRecipe();
            if (!this.filteredRecipeList.contains(bestRecipe)) {
                bestRecipe = this.filteredRecipeList.getFirstRecipe();
            }

            this.setRecipe(bestRecipe);
        }
    }

    @Override
    public void filterRecipeList(String filter, String categoryFilter, boolean force, IsoPlayer player) {
        super.filterRecipeList(filter, categoryFilter, force, player);
        this.filteredRecipeList.removeIf(craftRecipe -> craftRecipe.hasTag(CraftRecipeTag.RIGHT_CLICK_ONLY));
    }

    public void getCreatedOutputItems(ArrayList<InventoryItem> list) {
        if (this.recipeData != null && this.recipeData.isAllowOutputItems()) {
            ItemDataList dataList = this.recipeData.getToOutputItems();
            if (dataList.hasUnprocessed()) {
                dataList.getUnprocessed(list);
            }
        }
    }

    @Override
    protected void rebuildCachedRecipeInfo() {
        if (this.cachedRecipeInfosDirty) {
            for (int i = 0; i < this.cachedRecipeInfos.size(); i++) {
                HandcraftLogic.CachedRecipeInfo info = this.cachedRecipeInfos.get(i);
                HandcraftLogic.CachedRecipeInfo.Release(info);
            }

            this.cachedRecipeInfos.clear();
            this.cachedRecipeInfoMap.clear();

            for (int i = 0; i < this.completeRecipeList.size(); i++) {
                CraftRecipe recipe = this.completeRecipeList.get(i);
                this.createCachedRecipeInfo(recipe, this.containers);
            }

            this.cachedRecipeInfosDirty = false;
            this.cachedCanPerformDirty = true;
        }
    }

    @Override
    protected BaseCraftingLogic.CachedRecipeInfo createCachedRecipeInfo(CraftRecipe recipe, ArrayList<ItemContainer> containers) {
        BaseCraftingLogic.CachedRecipeInfo info = super.createCachedRecipeInfo(recipe, containers);
        boolean workstationValid = true;
        if (recipe.isAnySurfaceCraft()) {
            workstationValid = this.isCharacterInRangeOfWorkbench();
        }

        info.overrideCanPerform(info.isCanPerform() && workstationValid);
        return info;
    }

    @Override
    public boolean isCharacterInRangeOfWorkbench() {
        return this.isoObject != null && this.isoObject.getSquare().DistToProper(this.player) < 3.0F;
    }

    public boolean isValidRecipeForCharacter(CraftRecipe recipe) {
        if (this.cachedRecipeInfosDirty) {
            this.rebuildCachedRecipeInfo();
        }

        return this.getCachedRecipeInfo(recipe).isValid();
    }

    public boolean canCharacterPerformRecipe(CraftRecipe recipe) {
        if (this.cachedRecipeInfosDirty) {
            this.rebuildCachedRecipeInfo();
        }

        return this.getCachedRecipeInfo(recipe).isCanPerform();
    }

    public boolean isRecipeAvailableForCharacter(CraftRecipe recipe) {
        if (this.cachedRecipeInfosDirty) {
            this.rebuildCachedRecipeInfo();
        }

        return this.getCachedRecipeInfo(recipe).isAvailable();
    }

    public Texture getResultTexture() {
        if (this.recipeData != null && this.recipeData.getFirstCreatedItem() != null && this.recipeData.getFirstCreatedItem().getIcon() != null) {
            return this.recipeData.getFirstCreatedItem().getIcon();
        } else {
            if (this.recipeData != null && this.recipeData.getRecipe() != null && this.recipeData.getRecipe().getOutputs() != null) {
                for (int i = 0; i < this.recipeData.getRecipe().getOutputs().size(); i++) {
                    OutputScript output = this.recipeData.getRecipe().getOutputs().get(i);
                    if (output.getOutputMapper() != null && output.getOutputMapper().getEntrees() != null) {
                        for (int j = 0; j < output.getOutputMapper().getEntrees().size(); j++) {
                            OutputMapper.OutputEntree entree = output.getOutputMapper().getEntrees().get(j);
                            if (!entree.pattern.isEmpty()) {
                                for (int k = 0; k < entree.pattern.size(); k++) {
                                    Item testItem = entree.pattern.get(k);

                                    for (int l = 0; l < this.recipeData.inputs.size(); l++) {
                                        InventoryItem consumedItem = this.recipeData.inputs.get(l).getFirstInputItem();
                                        if (consumedItem.getFullType().equals(testItem.moduleDotType)) {
                                            return Texture.trygetTexture("Item_" + entree.result.getIcon());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return this.recipeData != null && this.recipeData.getRecipe() != null && this.recipeData.getRecipe().getIconTexture() != null
                ? this.recipeData.getRecipe().getIconTexture()
                : null;
        }
    }

    public void setIsoObject(IsoObject isoObj) {
        this.isoObject = isoObj;
    }

    public void setSelectedRecipeStyle(String style) {
        this.setSelectedRecipeStyle("handcraft", style);
    }

    public String getSelectedRecipeStyle() {
        return this.getSelectedRecipeStyle("handcraft");
    }

    public void setRecipeSortMode(String sortMode) {
        this.setRecipeSortMode("handcraft", sortMode);
    }

    public String getRecipeSortMode() {
        return this.getRecipeSortMode("handcraft");
    }

    public boolean isUsingRecipeAtHandBenefit() {
        if (this.getPlayer() != null && this.getPlayer() instanceof IsoPlayer && !((IsoPlayer)this.getPlayer()).tooDarkToRead()) {
            return !this.getRecipe().canBenefitFromRecipeAtHand(this.getPlayer()) ? false : this.isRecipeAtHand();
        } else {
            return false;
        }
    }

    public InventoryItem getUsingRecipeAtHandItem() {
        if (this.getPlayer() != null && this.getPlayer() instanceof IsoPlayer && !((IsoPlayer)this.getPlayer()).tooDarkToRead()) {
            for (int i = 0; i < this.containers.size(); i++) {
                ItemContainer cont = this.containers.get(i);
                if (cont.hasRecipe(this.getRecipe().getName(), this.getPlayer(), false)) {
                    return cont.getRecipeItem(this.getRecipe().getName(), this.getPlayer(), false);
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public boolean isRecipeAtHand() {
        if (this.getPlayer() != null && this.getPlayer() instanceof IsoPlayer && !((IsoPlayer)this.getPlayer()).tooDarkToRead()) {
            for (int i = 0; i < this.containers.size(); i++) {
                ItemContainer cont = this.containers.get(i);
                if (cont.hasRecipe(this.getRecipe().getName(), this.getPlayer(), false)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public void setLastSelectedRecipe(CraftRecipe recipe) {
        this.setLastSelectedRecipe("handcraft", recipe);
    }

    public CraftRecipe getLastSelectedRecipe() {
        return this.getLastSelectedRecipe("handcraft");
    }

    public void setLastManualInputMode(boolean b) {
        this.setLastManualInputMode("handcraft", b);
    }

    protected boolean getLastManualInputMode() {
        return this.getLastManualInputMode("handcraft");
    }

    @UsedFromLua
    public static class CachedRecipeInfo {
        private static final ArrayDeque<HandcraftLogic.CachedRecipeInfo> pool = new ArrayDeque<>();
        private CraftRecipe recipe;
        private boolean isValid;
        private boolean canPerform;
        private boolean available;

        private static HandcraftLogic.CachedRecipeInfo Alloc(CraftRecipe recipe) {
            HandcraftLogic.CachedRecipeInfo info = pool.poll();
            if (info == null) {
                info = new HandcraftLogic.CachedRecipeInfo();
            }

            info.recipe = recipe;
            return info;
        }

        private static void Release(HandcraftLogic.CachedRecipeInfo info) {
            info.reset();

            assert !pool.contains(info);

            pool.offer(info);
        }

        public CraftRecipe getRecipe() {
            return this.recipe;
        }

        public boolean isValid() {
            return this.isValid;
        }

        public boolean isCanPerform() {
            return this.canPerform;
        }

        public boolean isAvailable() {
            return this.available;
        }

        private void reset() {
            this.recipe = null;
            this.isValid = false;
            this.canPerform = false;
            this.available = false;
        }
    }
}
