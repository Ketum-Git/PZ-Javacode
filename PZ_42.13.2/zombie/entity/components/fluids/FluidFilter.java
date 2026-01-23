// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidFilterScript;

@DebugClassFields
@UsedFromLua
public class FluidFilter {
    private FluidFilterScript filterScript;
    private final EnumSet<FluidCategory> categories = EnumSet.noneOf(FluidCategory.class);
    private final EnumSet<FluidType> fluidEnums = EnumSet.noneOf(FluidType.class);
    private final HashSet<String> fluidStrings = new HashSet<>();
    private FluidFilter.FilterType filterType = FluidFilter.FilterType.Whitelist;
    private boolean isSealed;
    private String cachedFilterDisplayName;
    private String cachedFilterTooltipText;

    public void setFilterScript(String filterScriptName) {
        if (filterScriptName == null) {
            this.filterScript = null;
        } else if (this.filterScript == null || !filterScriptName.equalsIgnoreCase(this.filterScript.getScriptObjectFullType())) {
            FluidFilterScript script = ScriptManager.instance.getFluidFilter(filterScriptName);
            if (script == null) {
                DebugLog.General.warn("FluidFilter '" + filterScriptName + "' not found.");
            }

            this.filterScript = script;
        }
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            return s + "{EMPTY_FILTER}";
        } else {
            s = s + "{";
            s = s + "<cats=" + this.categories + ">";
            s = s + "<fluids=" + this.fluidEnums + ">";
            s = s + "<strings=" + this.fluidStrings.size() + ">";
            return s + "}";
        }
    }

    public void seal() {
        this.isSealed = true;
    }

    public boolean isSealed() {
        return this.isSealed;
    }

    public FluidFilter copy() {
        FluidFilter copy = new FluidFilter();
        copy.filterType = this.filterType;
        copy.categories.addAll(this.categories);
        copy.fluidEnums.addAll(this.fluidEnums);
        copy.fluidStrings.addAll(this.fluidStrings);
        return copy;
    }

    public FluidFilter.FilterType getFilterType() {
        return this.filterType;
    }

    public FluidFilter setFilterType(FluidFilter.FilterType filterType) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting setFilterType on sealed filter.");
            return this;
        } else {
            this.filterType = filterType;
            return this;
        }
    }

    public FluidFilter add(FluidCategory category) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        } else {
            this.categories.add(category);
            return this;
        }
    }

    public FluidFilter remove(FluidCategory category) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        } else {
            this.categories.remove(category);
            return this;
        }
    }

    public boolean contains(FluidCategory category) {
        return this.categories.contains(category);
    }

    public FluidFilter add(FluidType fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        } else if (fluid == FluidType.Modded) {
            DebugLog.log("Cannot add enum FluidType.Modded to fluid filter, add the fluid string (getFluidTypeString) instead.");
            return this;
        } else {
            if (!this.fluidEnums.contains(fluid)) {
                this.fluidEnums.add(fluid);
                this.fluidStrings.add(fluid.toString());
            }

            return this;
        }
    }

    public FluidFilter add(Fluid fluid) {
        return fluid.getFluidType() != FluidType.Modded ? this.add(fluid.getFluidType()) : this.add(fluid.getFluidTypeString());
    }

    public FluidFilter add(String fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting add on sealed filter.");
            return this;
        } else {
            if (!this.fluidStrings.contains(fluid)) {
                if (Fluid.Get(fluid) != null) {
                    this.fluidStrings.add(fluid);
                } else {
                    DebugLog.log("FluidFilter.add -> fluid '" + fluid + "' is not a valid registered fluid.");
                }
            }

            return this;
        }
    }

    public FluidFilter remove(FluidType fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        } else {
            this.fluidEnums.remove(fluid);
            this.fluidStrings.remove(fluid.toString());
            return this;
        }
    }

    public FluidFilter remove(Fluid fluid) {
        return fluid.getFluidType() != FluidType.Modded ? this.remove(fluid.getFluidType()) : this.remove(fluid.getFluidTypeString());
    }

    public FluidFilter remove(String fluid) {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> attempting remove on sealed filter.");
            return this;
        } else {
            this.fluidStrings.remove(fluid);
            return this;
        }
    }

    public boolean contains(FluidType fluid) {
        return this.fluidEnums.contains(fluid);
    }

    public boolean contains(Fluid fluid) {
        return fluid.getFluidType() != FluidType.Modded ? this.contains(fluid.getFluidType()) : this.contains(fluid.getFluidTypeString());
    }

    public boolean contains(String fluid) {
        return this.fluidStrings.contains(fluid);
    }

    public boolean allows(FluidType fluidType) {
        Fluid fluid = Fluid.Get(fluidType);
        return this.allows(fluid);
    }

    public boolean allows(Fluid fluid) {
        if (fluid == null) {
            return false;
        } else {
            boolean typeMatch = this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty();
            boolean categoryMatch = this.categories.isEmpty();
            if (fluid.getFluidType() != FluidType.Modded && this.fluidEnums.contains(fluid.getFluidType())
                || fluid.getFluidType() == FluidType.Modded && this.fluidStrings.contains(fluid.getFluidTypeString())) {
                typeMatch = true;
            }

            for (FluidCategory category : this.categories) {
                if (fluid.isCategory(category)) {
                    categoryMatch = true;
                    break;
                }
            }

            boolean fluidMatch = typeMatch && categoryMatch;
            return this.filterType == FluidFilter.FilterType.Whitelist ? fluidMatch : !fluidMatch;
        }
    }

    public boolean allows(String fluidString) {
        Fluid fluid = Fluid.Get(fluidString);
        return this.allows(fluid);
    }

    public String getFilterDisplayName() {
        if (this.cachedFilterDisplayName != null) {
            return this.cachedFilterDisplayName;
        } else if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            this.cachedFilterDisplayName = Translator.getText("Fluid_Name_Any");
            return this.cachedFilterDisplayName;
        } else {
            ArrayList<String> fluidNames = new ArrayList<>();
            ArrayList<String> fluidCategories = new ArrayList<>();

            for (FluidType fluid : this.fluidEnums) {
                if (!fluidNames.contains(fluid.getDisplayName())) {
                    fluidNames.add(fluid.getDisplayName());
                }
            }

            for (String fluidx : this.fluidStrings) {
                String name = Fluid.Get(fluidx).getDisplayName();
                if (!fluidNames.contains(name)) {
                    fluidNames.add(name);
                }
            }

            for (FluidCategory category : this.categories) {
                fluidCategories.add(category.name());
            }

            this.cachedFilterDisplayName = "";
            if (!fluidNames.isEmpty()) {
                fluidNames.sort(String::compareTo);
                if (fluidNames.size() == 1) {
                    this.cachedFilterDisplayName = fluidNames.get(0);
                } else {
                    this.cachedFilterDisplayName = Translator.getText("Fluid_And_Others", fluidNames.get(0));
                }

                return this.cachedFilterDisplayName;
            } else {
                if (fluidCategories.isEmpty()) {
                    this.cachedFilterDisplayName = Translator.getText("Fluid_Name_Any");
                } else {
                    fluidCategories.sort(String::compareTo);
                    if (fluidCategories.size() == 1) {
                        this.cachedFilterDisplayName = Translator.getText("Fluid_Any", fluidCategories.get(0));
                    } else {
                        this.cachedFilterDisplayName = Translator.getText("Fluid_Any_Others", fluidCategories.get(0));
                    }
                }

                return this.cachedFilterDisplayName;
            }
        }
    }

    public String getFilterTooltipText() {
        if (this.cachedFilterTooltipText != null) {
            return this.cachedFilterTooltipText;
        } else if (this.categories.isEmpty() && this.fluidEnums.isEmpty() && this.fluidStrings.isEmpty()) {
            this.cachedFilterTooltipText = Translator.getText("Fluid_Name_Any");
            return this.cachedFilterTooltipText;
        } else {
            ArrayList<String> fluidNames = new ArrayList<>();
            ArrayList<String> fluidCategories = new ArrayList<>();

            for (FluidType fluid : this.fluidEnums) {
                String name = fluid.getDisplayName();
                if (fluidNames.stream().noneMatch(entry -> entry.equals(name))) {
                    fluidNames.add(fluid.getDisplayName());
                }
            }

            for (String fluidx : this.fluidStrings) {
                String name = Fluid.Get(fluidx).getDisplayName();
                if (fluidNames.stream().noneMatch(entry -> entry.equals(name))) {
                    fluidNames.add(name);
                }
            }

            for (FluidCategory category : this.categories) {
                fluidCategories.add(category.name());
            }

            this.cachedFilterTooltipText = "";
            if (!fluidNames.isEmpty()) {
                this.cachedFilterTooltipText = this.cachedFilterTooltipText + fluidNames.get(0);

                for (int i = 1; i < fluidNames.size(); i++) {
                    this.cachedFilterTooltipText = this.cachedFilterTooltipText + "\n" + fluidNames.get(i);
                }

                return this.cachedFilterTooltipText;
            } else {
                if (fluidCategories.isEmpty()) {
                    this.cachedFilterTooltipText = Translator.getText("Fluid_Name_Any");
                } else {
                    this.cachedFilterTooltipText = fluidCategories.get(0);

                    for (int i = 1; i < fluidCategories.size(); i++) {
                        this.cachedFilterTooltipText = this.cachedFilterTooltipText + "\n" + fluidCategories.get(i);
                    }
                }

                return this.cachedFilterTooltipText;
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.filterType == FluidFilter.FilterType.Whitelist ? 1 : 0));
        output.put((byte)this.fluidEnums.size());
        this.fluidEnums.forEach(e -> output.put(e.getId()));
        output.put((byte)this.fluidStrings.size());
        this.fluidStrings.forEach(s -> GameWindow.WriteString(output, s));
        output.put((byte)this.categories.size());
        this.categories.forEach(c -> output.put(c.getId()));
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        if (this.isSealed) {
            DebugLog.log("FluidFilter -> Warning loading on a sealed fluid filter!");
            if (Core.debug) {
                throw new RuntimeException("Loading on a sealed fluid filter!");
            }
        }

        this.fluidEnums.clear();
        this.fluidStrings.clear();
        this.categories.clear();
        this.filterType = input.get() == 1 ? FluidFilter.FilterType.Whitelist : FluidFilter.FilterType.Blacklist;
        int count = input.get();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                byte id = input.get();
                FluidType type = FluidType.FromId(id);
                if (type != null) {
                    this.fluidEnums.add(type);
                }
            }
        }

        int var7 = input.get();
        if (var7 > 0) {
            for (int ix = 0; ix < var7; ix++) {
                String type = GameWindow.ReadString(input);
                if (Fluid.Get(type) != null) {
                    this.fluidStrings.add(type);
                }
            }
        }

        var7 = input.get();
        if (var7 > 0) {
            for (int ixx = 0; ixx < var7; ixx++) {
                byte id = input.get();
                FluidCategory category = FluidCategory.FromId(id);
                if (category != null) {
                    this.categories.add(category);
                }
            }
        }
    }

    @UsedFromLua
    public static enum FilterType {
        Whitelist,
        Blacklist;
    }
}
