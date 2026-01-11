// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.components.resources.Resource;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;

@UsedFromLua
public class CraftRecipeMonitor {
    private final ArrayList<String> lines = new ArrayList<>();
    private final ArrayList<String> tempStrings = new ArrayList<>();
    private int openedBlocks;
    private boolean sealed;
    private boolean printToConsole;
    private CraftRecipe recipe;

    public static CraftRecipeMonitor Create() {
        CraftRecipeMonitor m = new CraftRecipeMonitor();
        m.log("[root]");
        m.open();
        return m;
    }

    private CraftRecipeMonitor() {
    }

    public void setPrintToConsole(boolean b) {
        this.printToConsole = b;
    }

    public void reset() {
        this.lines.clear();
        this.log("[root]");
        this.open();
    }

    public void setRecipe(CraftRecipe recipe) {
        this.recipe = recipe;
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public ArrayList<String> GetLines() {
        if (!this.sealed) {
            this.seal();
        }

        return this.lines;
    }

    public CraftRecipeMonitor seal() {
        if (!this.sealed) {
            this.close();
            if (this.openedBlocks > 0) {
                DebugLog.General.warn("seal called but '" + this.openedBlocks + "' open blocks remain, auto resolving...");

                while (this.openedBlocks > 0) {
                    this.log("}");
                    this.openedBlocks--;
                }
            }

            this.sealed = true;
        }

        return this;
    }

    public void open() {
        if (this.canLog()) {
            this.openedBlocks++;
            this.log("{");
        }
    }

    public void close() {
        if (this.canLog()) {
            if (this.openedBlocks > 0) {
                this.openedBlocks--;
                this.log("}");
            } else {
                DebugLog.General.warn("close called but no more opened blocks");
            }
        }
    }

    public boolean canLog() {
        return Core.debug && !this.sealed;
    }

    public void warn(String s) {
        if (this.canLog()) {
            this.lines.add("<WARNING> " + s);
            if (this.printToConsole) {
                DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
            }
        }
    }

    public void success(String s) {
        if (this.canLog()) {
            this.lines.add("<SUCCESS> " + s);
            if (this.printToConsole) {
                DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
            }
        }
    }

    public void log(String s) {
        if (this.canLog()) {
            this.lines.add(s);
            if (this.printToConsole) {
                DebugLog.General.debugln(this.lines.get(this.lines.size() - 1));
            }
        }
    }

    public <T> void logList(String tag, ArrayList<T> list) {
        if (this.canLog()) {
            this.log(tag);
            this.open();
            if (list != null && !list.isEmpty()) {
                for (T element : list) {
                    this.log(element.toString());
                }
            }

            this.close();
        }
    }

    public void logCraftLogic(CraftLogic logic) {
        if (this.canLog()) {
            this.log("[CraftLogic]");
            this.open();
            this.log("isValid = " + logic.isValid());
            this.log("StartMode = " + logic.getStartMode());
            this.log("Query = " + logic.getRecipeTagQuery());
            this.log("InputGroup = " + logic.getInputsGroupName());
            this.log("OutputGroup = " + logic.getOutputsGroupName());
            this.log("[Recipes]");
            this.open();

            for (int i = 0; i < logic.getRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.close();
        }
    }

    public void logFurnaceLogic(FurnaceLogic logic) {
        if (this.canLog()) {
            this.log("[FurnaceLogic]");
            this.open();
            this.log("isValid = " + logic.isValid());
            this.log("StartMode = " + logic.getStartMode());
            this.log("FuelQuery = " + logic.getFuelRecipeTagQuery());
            this.log("FurnaceQuery = " + logic.getFurnaceRecipeTagQuery());
            this.log("FuelInputGroup = " + logic.getFuelInputsGroupName());
            this.log("FuelOutputGroup = " + logic.getFuelOutputsGroupName());
            this.log("FurnaceInputGroup = " + logic.getFurnaceInputsGroupName());
            this.log("FurnaceOutputGroup = " + logic.getFurnaceOutputsGroupName());
            this.log("[FuelRecipes]");
            this.open();

            for (int i = 0; i < logic.getFuelRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getFuelRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.log("[FurnaceRecipes]");
            this.open();

            for (int i = 0; i < logic.getFurnaceRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getFurnaceRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.close();
        }
    }

    public void logDryingLogic(DryingLogic logic) {
        if (this.canLog()) {
            this.log("[CraftLogic]");
            this.open();
            this.log("isValid = " + logic.isValid());
            this.log("StartMode = " + logic.getStartMode());
            this.log("UsesFuel = " + logic.isUsesFuel());
            this.log("FuelQuery = " + logic.getFuelRecipeTagQuery());
            this.log("FurnaceQuery = " + logic.getDryingRecipeTagQuery());
            this.log("FuelInputGroup = " + logic.getFuelInputsGroupName());
            this.log("FuelOutputGroup = " + logic.getFuelOutputsGroupName());
            this.log("DryingInputGroup = " + logic.getDryingInputsGroupName());
            this.log("DryingOutputGroup = " + logic.getDryingOutputsGroupName());
            this.log("[FuelRecipes]");
            this.open();

            for (int i = 0; i < logic.getFuelRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getFuelRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.log("[DryingRecipes]");
            this.open();

            for (int i = 0; i < logic.getDryingRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getDryingRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.close();
        }
    }

    public void logMashingLogic(MashingLogic logic) {
        if (this.canLog()) {
            this.log("[MashingLogic]");
            this.open();
            this.log("isValid = " + logic.isValid());
            this.log("Query = " + logic.getRecipeTagQuery());
            this.log("InputGroup = " + logic.getInputsGroupName());
            this.log("[Recipes]");
            this.open();

            for (int i = 0; i < logic.getRecipes().size(); i++) {
                this.log("[" + i + "] " + logic.getRecipes().get(i).getScriptObjectFullType());
            }

            this.close();
            this.close();
        }
    }

    public void logResources(List<Resource> inputs, List<Resource> outputs) {
        if (this.canLog()) {
            this.log("[resources]");
            this.open();
            if (inputs != null && !inputs.isEmpty()) {
                this.logResourcesList("[Inputs]", inputs);
            }

            if (outputs != null && !outputs.isEmpty()) {
                this.logResourcesList("[Outputs]", outputs);
            }

            this.close();
        }
    }

    public void logResourcesList(String tag, List<Resource> resources) {
        if (this.canLog()) {
            this.log(tag);
            this.open();

            for (int i = 0; i < resources.size(); i++) {
                Resource resource = resources.get(i);
                this.log("[" + i + "]");
                this.open();
                if (resource.getId() != null) {
                    this.log("Id = \"" + resource.getId() + "\"");
                } else {
                    this.log("Id = " + resource.getId());
                }

                this.log("Type = " + resource.getType());
                this.log("Io = " + resource.getIO());
                this.log("Channel = " + resource.getChannel());
                this.log("Flags = " + resource.getDebugFlagsString());
                this.close();
            }

            this.close();
        }
    }

    public void logRecipe(CraftRecipe recipe, boolean doInputsOutputs) {
        if (this.canLog()) {
            this.log("[Recipe]");
            this.open();
            this.log("CraftRecipe = " + recipe.getScriptObjectFullType());
            this.log("Enabled = " + recipe.isEnabled());
            this.log("DebugOnly = " + recipe.isDebugOnly());
            this.log("Time = (int) " + recipe.getTime());

            for (CraftRecipe.LuaCall luaCall : CraftRecipe.LuaCall.values()) {
                this.log(luaCall.toString() + " = " + recipe.getLuaCallString(luaCall));
            }

            if (doInputsOutputs) {
                this.log("[Inputs]");
                this.open();

                for (int i = 0; i < recipe.getInputs().size(); i++) {
                    this.log("[" + i + "]");
                    this.open();
                    this.logInputScript(recipe.getInputs().get(i));
                    this.close();
                }

                this.close();
                this.log("[Outputs]");
                this.open();

                for (int i = 0; i < recipe.getOutputs().size(); i++) {
                    this.log("[" + i + "]");
                    this.open();
                    this.logOutputScript(recipe.getOutputs().get(i));
                    this.close();
                }

                this.close();
            }

            this.close();
        }
    }

    public void logInputScript(InputScript input) {
        if (this.canLog()) {
            this.log("Line = \"" + input.getOriginalLine() + "\"");
            this.log("Type = " + input.getResourceType());
            this.log("Amount = (float)" + input.getAmount());
            this.log("AmountInt = (int) " + input.getIntAmount());
            this.log("MaxAmountInt = (int) " + input.getIntMaxAmount());
            this.log("ShapedIndex = (int) " + input.getShapedIndex());
            this.log("isKeep = " + input.isKeep());
            this.log("isDestroy = " + input.isDestroy());
            this.log("ItemApplyMode = " + input.getItemApplyMode());
            this.log("FluidMatchMode = " + input.getFluidMatchMode());
            if (input.isReplace()) {
                this.open();
                this.log("[Replace->Output]");
                this.logOutputScript(input.getReplaceOutputScript());
                this.close();
            }
        }
    }

    public void logOutputScript(OutputScript output) {
        if (this.canLog()) {
            this.log("Line = \"" + output.getOriginalLine() + "\"");
            this.log("Type = " + output.getResourceType());
            this.log("Amount = (float) " + output.getAmount());
            this.log("AmountInt = (int) " + output.getIntAmount());
            this.log("MaxAmountInt = (int) " + output.getIntMaxAmount());
            this.log("Chance = (float) " + output.getChance());
            switch (output.getResourceType()) {
                case Item:
                    if (!output.getPossibleResultItems().isEmpty()) {
                        if (output.getPossibleResultItems().size() > 1) {
                            this.log("Item = " + output.getPossibleResultItems().size() + " possibilities.");

                            for (int i = 0; i < output.getPossibleResultItems().size(); i++) {
                                this.log("   - " + output.getPossibleResultItems().get(i).getScriptObjectFullType());
                            }
                        } else {
                            this.log("Item = " + output.getPossibleResultItems().get(0).getScriptObjectFullType());
                        }
                    } else {
                        this.warn("Item = null");
                    }
                    break;
                case Fluid:
                    if (output.getFluid() != null) {
                        this.log("Fluid = " + output.getFluid().getTranslatedName());
                    } else {
                        this.warn("Fluid = null");
                    }
                    break;
                case Energy:
                    if (output.getEnergy() != null) {
                        this.log("Energy = " + output.getEnergy().getDisplayName());
                    } else {
                        this.warn("Energy = null");
                    }
            }
        }
    }
}
