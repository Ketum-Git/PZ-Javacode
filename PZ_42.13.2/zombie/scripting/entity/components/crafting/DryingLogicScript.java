// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

public class DryingLogicScript extends ComponentScript {
    private String dryingRecipeTagQuery;
    private String fuelRecipeTagQuery;
    private StartMode startMode = StartMode.Manual;
    private String inputsGroupName;
    private String outputsGroupName;
    private String fuelInputsGroupName;
    private String fuelOutputsGroupName;

    private DryingLogicScript() {
        super(ComponentType.DryingLogic);
    }

    public String getDryingRecipeTagQuery() {
        return this.dryingRecipeTagQuery;
    }

    public String getFuelRecipeTagQuery() {
        return this.fuelRecipeTagQuery;
    }

    public StartMode getStartMode() {
        return this.startMode;
    }

    public String getInputsGroupName() {
        return this.inputsGroupName;
    }

    public String getOutputsGroupName() {
        return this.outputsGroupName;
    }

    public String getFuelInputsGroupName() {
        return this.fuelInputsGroupName;
    }

    public String getFuelOutputsGroupName() {
        return this.fuelOutputsGroupName;
    }

    public boolean isUsesFuel() {
        return this.fuelRecipeTagQuery != null;
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public boolean isoMasterOnly() {
        return true;
    }

    @Override
    public void PreReload() {
        this.dryingRecipeTagQuery = null;
        this.fuelRecipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
        this.fuelInputsGroupName = null;
        this.fuelOutputsGroupName = null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (!StringUtils.isNullOrWhitespace(this.dryingRecipeTagQuery)) {
            this.dryingRecipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.dryingRecipeTagQuery);
            if ((!Core.debug || !StringUtils.isNullOrWhitespace(this.inputsGroupName)) && StringUtils.isNullOrWhitespace(this.outputsGroupName)) {
            }

            if (!StringUtils.isNullOrWhitespace(this.fuelRecipeTagQuery)) {
                this.fuelRecipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.fuelRecipeTagQuery);
                if ((!Core.debug || !StringUtils.isNullOrWhitespace(this.fuelInputsGroupName)) && StringUtils.isNullOrWhitespace(this.fuelOutputsGroupName)) {
                }
            }
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("dryingRecipes")) {
                    this.dryingRecipeTagQuery = val;
                }

                if (key.equalsIgnoreCase("fuelRecipes")) {
                    this.fuelRecipeTagQuery = val;
                } else if (key.equalsIgnoreCase("startMode")) {
                    this.startMode = StartMode.valueOf(val);
                } else if (key.equalsIgnoreCase("inputGroup")) {
                    this.inputsGroupName = val;
                } else if (key.equalsIgnoreCase("outputGroup")) {
                    this.outputsGroupName = val;
                } else if (key.equalsIgnoreCase("fuelInputGroup")) {
                    this.fuelInputsGroupName = val;
                } else if (key.equalsIgnoreCase("fuelOutputGroup")) {
                    this.fuelOutputsGroupName = val;
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
        }
    }
}
