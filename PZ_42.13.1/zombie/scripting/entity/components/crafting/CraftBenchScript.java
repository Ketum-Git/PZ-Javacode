// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import java.util.List;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

@UsedFromLua
public class CraftBenchScript extends ComponentScript {
    private final EnumBitStore<ResourceChannel> fluidInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private final EnumBitStore<ResourceChannel> energyInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private String recipeTagQuery;

    private CraftBenchScript() {
        super(ComponentType.CraftBench);
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public List<CraftRecipe> getRecipes() {
        return CraftRecipeManager.queryRecipes(this.recipeTagQuery);
    }

    public EnumBitStore<ResourceChannel> getFluidInputChannels() {
        return this.fluidInputChannels;
    }

    public EnumBitStore<ResourceChannel> getEnergyInputChannels() {
        return this.energyInputChannels;
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
        if (StringUtils.isNullOrWhitespace(this.recipeTagQuery)) {
            throw new Exception("Recipe tag query null or whitespace.");
        } else {
            this.recipeTagQuery = CraftRecipeManager.FormatAndRegisterRecipeTagsQuery(this.recipeTagQuery);
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
                } else if (key.equalsIgnoreCase("fluidInputChannels")) {
                    String[] split = val.split(";");

                    for (int i = 0; i < split.length; i++) {
                        ResourceChannel channel = ResourceChannel.valueOf(split[i]);
                        this.fluidInputChannels.add(channel);
                    }
                } else if (key.equalsIgnoreCase("energyInputChannels")) {
                    String[] split = val.split(";");

                    for (int i = 0; i < split.length; i++) {
                        ResourceChannel channel = ResourceChannel.valueOf(split[i]);
                        this.energyInputChannels.add(channel);
                    }
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("craftProcessor")) {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
            }
        }
    }
}
