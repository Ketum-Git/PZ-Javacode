// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.build;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeListNodeCollection;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.WallCoveringConfigScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.WallCoveringType;

@UsedFromLua
public class BuildLogic extends BaseCraftingLogic {
    private CraftRecipe selectedRecipe;
    private final CraftRecipeData recipeDataInProgress;
    private boolean craftActionInProgress;
    private final Dictionary<CraftRecipe, CraftRecipeComponentScript> recipeComponentScriptLookup = new Hashtable<>();

    public BuildLogic(IsoGameCharacter player, CraftBench craftBench, IsoObject isoObject) {
        super(player, craftBench);
        this.selectedRecipe = null;
        this.isoObject = isoObject;
        this.recipeDataInProgress = new CraftRecipeData(CraftMode.Handcraft, craftBench != null, true, false, true);
        this.recipeDataInProgress.setCharacter(player);
        if (this.craftBench != null) {
            this.sourceResources.addAll(craftBench.getResources());
        }

        this.registerEvent("onRecipeChanged");
        this.registerEvent("onStartCraft");
        this.registerEvent("onStopCraft");
        this.setSortModeInternal(this.getRecipeSortMode());
        this.setManualSelectInputs(this.getLastManualInputMode());
    }

    @Override
    public List<Item> getSatisfiedInputItems(InputScript inputScript) {
        ArrayList<Item> output = new ArrayList<>();
        if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(inputScript)) {
            CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);

            for (int i = 0; i < data.getAppliedItemsCount(); i++) {
                output.add(data.getAppliedItem(i).getScriptItem());
            }

            return output;
        } else {
            return output;
        }
    }

    public CraftRecipeListNodeCollection getRecipeList() {
        return this.filteredRecipeList;
    }

    @Override
    public CraftRecipe getRecipe() {
        return this.selectedRecipe;
    }

    public CraftRecipeData getRecipeData() {
        return this.recipeData;
    }

    public CraftRecipeData getRecipeDataInProgress() {
        return this.recipeDataInProgress;
    }

    public SpriteConfigManager.ObjectInfo getSelectedBuildObject() {
        if (this.selectedRecipe != null) {
            CraftRecipeComponentScript craftRecipeComponentScript = this.recipeComponentScriptLookup.get(this.selectedRecipe);
            GameEntityScript gameEntityScript = (GameEntityScript)craftRecipeComponentScript.getParent();
            if (gameEntityScript != null) {
                SpriteConfigScript configScript = gameEntityScript.getComponentScriptFor(ComponentType.SpriteConfig);
                if (configScript != null) {
                    return SpriteConfigManager.GetObjectInfo(configScript.getName());
                }
            }
        }

        return null;
    }

    public KahluaTable getWallCoveringParams() {
        WallCoveringConfigScript wallCoveringConfigScript = null;
        if (this.selectedRecipe != null) {
            CraftRecipeComponentScript craftRecipeComponentScript = this.recipeComponentScriptLookup.get(this.selectedRecipe);
            GameEntityScript gameEntityScript = (GameEntityScript)craftRecipeComponentScript.getParent();
            if (gameEntityScript != null) {
                wallCoveringConfigScript = gameEntityScript.getComponentScriptFor(ComponentType.WallCoveringConfig);
            }
        }

        if (wallCoveringConfigScript != null) {
            KahluaTable table = LuaManager.platform.newTable();
            table.rawset("actionType", wallCoveringConfigScript.getType());
            if (wallCoveringConfigScript.getType() == WallCoveringType.PAINT_SIGN) {
                table.rawset("sign", wallCoveringConfigScript.getSignIndex().doubleValue());
            }

            if (wallCoveringConfigScript.getType() == WallCoveringType.PAINT_SIGN || wallCoveringConfigScript.getType() == WallCoveringType.PAINT_THUMP) {
                InventoryItem paint = this.recipeData.getFirstInputItemWithTag(ItemTag.PAINT);
                if (paint != null) {
                    table.rawset("paintType", paint.getType());
                    Color paintColor = this.getPaintColor(paint.getType());
                    if (paintColor != null) {
                        table.rawset("r", paintColor.getR());
                        table.rawset("g", paintColor.getG());
                        table.rawset("b", paintColor.getB());
                    }
                }
            }

            if (wallCoveringConfigScript.getType() == WallCoveringType.WALLPAPER) {
                InventoryItem wallpaper = this.recipeData.getFirstInputItemWithTag(ItemTag.WALLPAPER);
                if (wallpaper != null) {
                    table.rawset("wallpaperType", wallpaper.getType());
                }
            }

            return table;
        } else {
            return null;
        }
    }

    private Color getPaintColor(String paintType) {
        if (LuaManager.env.rawget("ISPaintMenu") instanceof KahluaTable paintTable && paintTable.rawget("PaintMenuItems") instanceof KahluaTable paintItems) {
            KahluaTableIterator iterator = paintItems.iterator();

            while (iterator.advance()) {
                if (iterator.getValue() instanceof KahluaTable value
                    && value.rawget("paint") instanceof String type
                    && type.equalsIgnoreCase(paintType)
                    && value.rawget("color") instanceof KahluaTable color
                    && color.rawget(1) instanceof Double r
                    && color.rawget(2) instanceof Double g
                    && color.rawget(2) instanceof Double b) {
                    return new Color(r.floatValue(), g.floatValue(), b.floatValue());
                }
            }
        }

        return null;
    }

    public ArrayList<CraftRecipe> getAllBuildableRecipes() {
        ArrayList<CraftRecipe> allBuildableRecipes = new ArrayList<>();
        ArrayList<GameEntityScript> entityScripts = ScriptManager.instance.getAllGameEntities();

        for (int i = 0; i < entityScripts.size(); i++) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();

            for (int j = 0; j < allComponents.size(); j++) {
                if (allComponents.get(j).type == ComponentType.CraftRecipe) {
                    CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                    CraftRecipe craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                    if (craftRecipe != null && craftRecipe.hasTag(CraftRecipeTag.ENTITY_RECIPE)) {
                        allBuildableRecipes.add(craftRecipe);
                        this.recipeComponentScriptLookup.put(craftRecipe, componentScript);
                        break;
                    }
                }
            }
        }

        return allBuildableRecipes;
    }

    @Override
    public void setRecipe(CraftRecipe recipe) {
        if (this.selectedRecipe != recipe) {
            this.selectedRecipe = recipe;
            this.setLastSelectedRecipe(recipe);
            super.setRecipe(recipe);
        }
    }

    public boolean isCraftActionInProgress() {
        return this.craftActionInProgress;
    }

    @Override
    public boolean areAllInputItemsSatisfied() {
        if (this.selectedRecipe == null) {
            return false;
        } else {
            for (InputScript inputScript : this.selectedRecipe.getInputs()) {
                if (!this.isInputSatisfied(inputScript)) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    public boolean isInputSatisfied(InputScript inputScript) {
        if (this.recipeData.getRecipe() != null && this.recipeData.getRecipe().containsIO(inputScript)) {
            CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
            return data.isCachedCanConsume();
        } else {
            return false;
        }
    }

    public void startCraftAction(KahluaTableImpl actionTable) {
        this.craftActionInProgress = true;
        this.recipeDataInProgress.setRecipe(this.recipeData.getRecipe());
        this.updateFloorContainer();
        if (this.isManualSelectInputs()) {
            ArrayList<InventoryItem> inputList = new ArrayList<>();

            for (InputScript input : this.recipeData.getRecipe().getInputs()) {
                this.recipeDataInProgress.setManualInputsFor(input, this.getMulticraftConsumedItemsFor(input, inputList));
                inputList.clear();
            }
        }

        this.triggerEvent("onStartCraft", actionTable);
    }

    public void updateFloorContainer() {
        if (this.updateFloorContainer(this.containers)) {
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
            this.cachedCanPerform = this.canPerformCurrentRecipe();
        }
    }

    public boolean performCurrentRecipe() {
        if (!this.isContainersAccessible(this.containers)) {
            return false;
        } else {
            this.updateFloorContainer();
            return this.recipeDataInProgress.perform(this.player, this.sourceResources, this.isManualSelectInputs() ? null : this.allItems, this.containers);
        }
    }

    public void stopCraftAction() {
        this.craftActionInProgress = false;
        this.recipeDataInProgress.setRecipe(null);
        this.triggerEvent("onStopCraft");
    }

    public ArrayList<InventoryItem> getAllConsumedItems() {
        return this.recipeData != null ? this.recipeData.getAllConsumedItems() : null;
    }

    public void setSelectedRecipeStyle(String style) {
        this.setSelectedRecipeStyle("build", style);
    }

    public String getSelectedRecipeStyle() {
        return this.getSelectedRecipeStyle("build");
    }

    public void setRecipeSortMode(String sortMode) {
        this.setRecipeSortMode("build", sortMode);
    }

    public String getRecipeSortMode() {
        return this.getRecipeSortMode("build");
    }

    public void setLastSelectedRecipe(CraftRecipe recipe) {
        this.setLastSelectedRecipe("build", recipe);
    }

    public CraftRecipe getLastSelectedRecipe() {
        return this.getLastSelectedRecipe("build");
    }

    public void setLastManualInputMode(boolean b) {
        this.setLastManualInputMode("build", b);
    }

    protected boolean getLastManualInputMode() {
        return this.getLastManualInputMode("build");
    }
}
