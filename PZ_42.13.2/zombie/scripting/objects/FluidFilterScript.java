// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidFilter;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.world.StringDictionary;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
@UsedFromLua
public class FluidFilterScript extends BaseScriptObject {
    private final ArrayList<String> fluids = new ArrayList<>();
    private final ArrayList<String> categories = new ArrayList<>();
    private boolean isWhitelist = true;
    private FluidFilter filter;
    private String name;
    private final boolean anonymous;

    public FluidFilterScript() {
        super(ScriptType.FluidFilter);
        this.anonymous = false;
    }

    private FluidFilterScript(boolean anonymous) {
        super(ScriptType.FluidFilter);
        this.anonymous = anonymous;
    }

    public static FluidFilterScript GetAnonymous() {
        return new FluidFilterScript(true);
    }

    public static FluidFilterScript GetAnonymous(boolean isWhitelist) {
        FluidFilterScript ffs = new FluidFilterScript(true);
        ffs.isWhitelist = isWhitelist;
        return ffs;
    }

    public FluidFilterScript copy() {
        FluidFilterScript ffs = new FluidFilterScript(this.anonymous);
        ffs.fluids.addAll(this.fluids);
        ffs.categories.addAll(this.categories);
        ffs.isWhitelist = this.isWhitelist;
        ffs.name = this.name;
        return ffs;
    }

    public boolean isSingleFluid() {
        return this.fluids.size() == 1 && this.categories.isEmpty();
    }

    private void addFluid(String fluid) {
        if (!this.fluids.contains(fluid)) {
            this.fluids.add(fluid);
        }
    }

    private void addCategory(String category) {
        if (!this.categories.contains(category)) {
            this.categories.add(category);
        }
    }

    public FluidFilter getFilter() {
        return this.filter;
    }

    public FluidFilter createFilter() throws Exception {
        if (!Fluid.FluidsInitialized() && Core.debug) {
            throw new RuntimeException("Fluids not yet initialized.");
        } else {
            FluidFilter.FilterType filterType = this.isWhitelist ? FluidFilter.FilterType.Whitelist : FluidFilter.FilterType.Blacklist;
            this.filter = new FluidFilter();
            this.filter.setFilterType(filterType);

            for (String s : this.categories) {
                FluidCategory category = FluidCategory.valueOf(s);
                this.filter.add(category);
            }

            for (String s : this.fluids) {
                Fluid fluid = Fluid.Get(s);
                if (fluid == null) {
                    throw new Exception("Cannot add fluid '" + s + "' in filter script.");
                }

                this.filter.add(fluid);
            }

            return this.filter;
        }
    }

    @Override
    public void getVersion(IVersionHash hash) {
        if (this.name != null) {
            hash.add(this.name);
        }
    }

    @Override
    public void PreReload() {
        this.fluids.clear();
        this.categories.clear();
        this.filter = null;
        this.isWhitelist = true;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        if (loadMode == ScriptLoadMode.Init && !this.anonymous) {
            StringDictionary.Generic.register(this.getScriptObjectFullType());
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        this.createFilter();
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        if (this.anonymous) {
            throw new Exception("Cannot load, is anonymous");
        } else {
            ScriptParser.Block block = ScriptParser.parse(totalFile);
            block = block.children.get(0);
            this.name = name;
            this.LoadCommonBlock(block);
            this.readBlock(block);
        }
    }

    public void LoadAnonymousFromBlock(ScriptParser.Block block) throws Exception {
        if (!this.anonymous) {
            throw new Exception("Cannot load, is not anonymous");
        } else {
            this.name = "anonymous";
            this.readBlock(block);
        }
    }

    public void LoadAnonymousSingleFluid(String fluidName) throws Exception {
        if (!this.anonymous) {
            throw new Exception("Cannot load, is not anonymous");
        } else {
            this.name = "anonymous";
            this.fluids.add(fluidName);
        }
    }

    private void readBlock(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("fluid")) {
                    this.parseInputString(this.fluids, val);
                } else if (key.equalsIgnoreCase("category")) {
                    this.parseInputString(this.categories, val);
                } else if (key.equalsIgnoreCase("filterType")) {
                    this.isWhitelist = val.equalsIgnoreCase("whitelist");
                } else if (key.equalsIgnoreCase("whitelist")) {
                    this.isWhitelist = Boolean.parseBoolean(val);
                } else if (key.equalsIgnoreCase("blacklist")) {
                    this.isWhitelist = !Boolean.parseBoolean(val);
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("fluids".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, this.fluids);
            } else if ("categories".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, this.categories);
            }
        }
    }

    private void readFilterBlock(ScriptParser.Block block, ArrayList<String> list) {
        for (ScriptParser.Value value : block.values) {
            if (value.string != null && !value.string.trim().isEmpty()) {
                String s = value.string.trim();
                if (!s.contains("=")) {
                    this.parseInputString(list, s);
                }
            }
        }
    }

    private void parseInputString(ArrayList<String> list, String input) {
        String[] split = input.split("/");

        for (String s : split) {
            s = s.trim();
            if (!list.contains(s)) {
                list.add(s);
            }
        }
    }
}
