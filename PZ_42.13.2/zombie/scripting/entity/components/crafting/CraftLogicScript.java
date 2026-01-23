// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftLogicScript extends ComponentScript {
    private String recipeTagQuery;
    private StartMode startMode = StartMode.Manual;
    private String inputsGroupName;
    private String outputsGroupName;
    private String actionAnim;

    private CraftLogicScript() {
        super(ComponentType.CraftLogic);
    }

    protected CraftLogicScript(ComponentType type) {
        super(type);
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
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

    public String getActionAnim() {
        return this.actionAnim;
    }

    @Deprecated
    public ArrayList<Object> getCraftProcessorScripts() {
        return new ArrayList<>();
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
        this.recipeTagQuery = null;
        this.startMode = StartMode.Manual;
        this.inputsGroupName = null;
        this.outputsGroupName = null;
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
        if (!StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
            if ((!Core.debug || !StringUtils.isNullOrWhitespace(this.inputsGroupName)) && StringUtils.isNullOrWhitespace(this.outputsGroupName)) {
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
                if (key.equalsIgnoreCase("recipes")) {
                    this.recipeTagQuery = val;
                } else if (key.equalsIgnoreCase("startMode")) {
                    this.startMode = StartMode.valueOf(val);
                } else if (key.equalsIgnoreCase("inputGroup")) {
                    this.inputsGroupName = val;
                } else if (key.equalsIgnoreCase("outputGroup")) {
                    this.outputsGroupName = val;
                } else if (key.equalsIgnoreCase("actionAnim")) {
                    this.actionAnim = val;
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("craftProcessor")) {
                DebugLog.General.warn("Block craft processor is deprecated.");
            } else {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
            }
        }
    }
}
