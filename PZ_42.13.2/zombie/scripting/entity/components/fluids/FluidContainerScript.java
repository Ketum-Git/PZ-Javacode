// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.fluids;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidUtil;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.objects.FluidFilterScript;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class FluidContainerScript extends ComponentScript {
    private static final String TAG_OVERRIDE = "override";
    private FluidFilterScript whitelist;
    private FluidFilterScript blacklist;
    private String containerName = "FluidContainer";
    private float capacity = 1.0F;
    private boolean initialAmountSet;
    private float initialAmountMin;
    private float initialAmountMax = 1.0F;
    private ArrayList<FluidContainerScript.FluidScript> initialFluids;
    private boolean initialFluidsIsRandom;
    private boolean inputLocked;
    private boolean canEmpty = true;
    private boolean hiddenAmount;
    private float rainCatcher;
    private boolean fillsWithCleanWater;
    private String customDrinkSound = "DrinkingFromGeneric";

    private FluidContainerScript() {
        super(ComponentType.FluidContainer);
    }

    @Override
    public void PreReload() {
        this.whitelist = null;
        this.blacklist = null;
        this.containerName = "FluidContainer";
        this.capacity = 1.0F;
        this.initialAmountSet = false;
        this.initialAmountMin = 0.0F;
        this.initialAmountMax = 1.0F;
        this.initialFluids = null;
        this.initialFluidsIsRandom = false;
        this.inputLocked = false;
        this.canEmpty = true;
        this.hiddenAmount = false;
        this.rainCatcher = 0.0F;
        this.fillsWithCleanWater = false;
        this.customDrinkSound = "DrinkingFromGeneric";
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        this.capacity = PZMath.max(this.capacity, FluidUtil.getMinContainerCapacity());
        if (this.initialAmountMin > this.initialAmountMax) {
            float max = this.initialAmountMax;
            this.initialAmountMax = this.initialAmountMin;
            this.initialAmountMin = max;
        }

        this.initialAmountMin = PZMath.clamp(this.initialAmountMin, 0.0F, 1.0F);
        this.initialAmountMax = PZMath.clamp(this.initialAmountMax, 0.0F, 1.0F);
        this.initialAmountMin = this.initialAmountMin * this.capacity;
        this.initialAmountMax = this.initialAmountMax * this.capacity;
        if (!this.initialFluidsIsRandom && this.initialFluids != null && !this.initialFluids.isEmpty()) {
            PZMath.normalize(this.initialFluids, FluidContainerScript.FluidScript::getPercentage, FluidContainerScript.FluidScript::setPercentage);
        }

        if (this.whitelist != null) {
            this.whitelist.createFilter().seal();
        }

        if (this.blacklist != null) {
            this.blacklist.createFilter().seal();
        }
    }

    @Override
    protected void copyFrom(ComponentScript componentScript) {
        FluidContainerScript other = (FluidContainerScript)componentScript;
        this.containerName = other.containerName;
        this.capacity = other.capacity;
        this.initialFluidsIsRandom = other.initialFluidsIsRandom;
        this.initialAmountSet = other.initialAmountSet;
        this.initialAmountMin = other.initialAmountMin;
        this.initialAmountMax = other.initialAmountMax;
        this.inputLocked = other.inputLocked;
        this.canEmpty = other.canEmpty;
        this.rainCatcher = other.rainCatcher;
        this.fillsWithCleanWater = other.fillsWithCleanWater;
        this.hiddenAmount = other.hiddenAmount;
        this.customDrinkSound = other.customDrinkSound;
        if (other.initialFluids != null) {
            this.initialFluids = new ArrayList<>();

            for (FluidContainerScript.FluidScript fsOther : other.initialFluids) {
                FluidContainerScript.FluidScript fs = this.getOrCreateFluidScript(fsOther.fluidType);
                fs.percentage = fsOther.percentage;
                if (fsOther.customColor != null) {
                    fs.customColor = new Color();
                    fs.customColor.set(fsOther.customColor);
                }
            }
        }

        if (other.whitelist != null) {
            this.whitelist = other.whitelist.copy();
        }

        if (other.blacklist != null) {
            this.blacklist = other.blacklist.copy();
        }
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String s = element.asValue().string;
                if (!s.trim().isEmpty() && s.contains("=")) {
                    String[] split = s.split("=");
                    String k = split[0].trim();
                    String v = split[1].trim();
                    if (k.equalsIgnoreCase("Capacity")) {
                        this.capacity = Float.parseFloat(v);
                    } else if (k.equalsIgnoreCase("ContainerName")) {
                        if (StringUtils.containsWhitespace(v)) {
                            DebugLog.General.error("Sanitizing container name '" + v + "', name may not contain whitespaces.");
                            v = StringUtils.removeWhitespace(v);
                        }

                        this.containerName = v;
                    } else if (k.equalsIgnoreCase("InitialPercent")) {
                        this.initialAmountMin = Float.parseFloat(v);
                        this.initialAmountMax = this.initialAmountMin;
                        this.initialAmountSet = true;
                    } else if (k.equalsIgnoreCase("InitialPercentMin")) {
                        this.initialAmountMin = Float.parseFloat(v);
                        this.initialAmountSet = true;
                    } else if (k.equalsIgnoreCase("InitialPercentMax")) {
                        this.initialAmountMax = Float.parseFloat(v);
                        this.initialAmountSet = true;
                    } else if (k.equalsIgnoreCase("PickRandomFluid")) {
                        this.initialFluidsIsRandom = v.equalsIgnoreCase("true");
                    } else if (k.equalsIgnoreCase("InputLocked")) {
                        this.inputLocked = v.equalsIgnoreCase("true");
                    } else if (k.equalsIgnoreCase("Opened")) {
                        this.canEmpty = v.equalsIgnoreCase("true");
                    } else if (k.equalsIgnoreCase("HiddenAmount")) {
                        this.hiddenAmount = v.equalsIgnoreCase("true");
                    } else if (k.equalsIgnoreCase("RainFactor")) {
                        this.rainCatcher = Float.parseFloat(v);
                    } else if (k.equalsIgnoreCase("FillsWithCleanWater")) {
                        this.fillsWithCleanWater = v.equalsIgnoreCase("true");
                    } else if (k.equalsIgnoreCase("CustomDrinkSound")) {
                        this.customDrinkSound = v;
                    }
                }
            } else {
                ScriptParser.Block child = element.asBlock();
                boolean isOverride = block.id != null && block.id.equalsIgnoreCase("override");
                if (child.type.equalsIgnoreCase("fluids")) {
                    this.loadBlockFluids(child, isOverride);
                } else if (child.type.equalsIgnoreCase("whitelist") || child.type.equalsIgnoreCase("blacklist")) {
                    boolean isWhiteList = child.type.equalsIgnoreCase("whitelist");
                    FluidFilterScript filter;
                    if (!isOverride && isWhiteList && this.whitelist != null) {
                        filter = this.whitelist;
                    } else if (!isOverride && !isWhiteList && this.blacklist != null) {
                        filter = this.blacklist;
                    } else {
                        filter = FluidFilterScript.GetAnonymous(isWhiteList);
                    }

                    filter.LoadAnonymousFromBlock(child);
                    if (isWhiteList) {
                        this.whitelist = filter;
                    } else {
                        this.blacklist = filter;
                    }
                }
            }
        }
    }

    private void loadBlockFluids(ScriptParser.Block block, boolean isOverride) {
        if (isOverride && this.initialFluids != null) {
            this.initialFluids.clear();
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if (k.equalsIgnoreCase("Fluid")) {
                FluidContainerScript.FluidScript fs = this.readFluid(v);
                if (this.initialFluids == null) {
                    this.initialFluids = new ArrayList<>();
                }

                this.initialFluids.add(fs);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("Fluid")) {
                FluidContainerScript.FluidScript fs = this.readFluidAsBlock(child);
                if (fs != null) {
                    if (this.initialFluids == null) {
                        this.initialFluids = new ArrayList<>();
                    }

                    this.initialFluids.add(fs);
                } else {
                    DebugLog.General.warn("Unable to read fluid block.");
                }
            }
        }
    }

    private FluidContainerScript.FluidScript getOrCreateFluidScript(String fluidType) {
        return new FluidContainerScript.FluidScript(fluidType);
    }

    private FluidContainerScript.FluidScript readFluidAsBlock(ScriptParser.Block block) {
        FluidContainerScript.FluidScript fs = null;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("type".equalsIgnoreCase(k)) {
                fs = this.getOrCreateFluidScript(v);
            }
        }

        if (fs == null) {
            return null;
        } else {
            for (ScriptParser.Value valuex : block.values) {
                String k = valuex.getKey().trim();
                String v = valuex.getValue().trim();
                if ("percentage".equalsIgnoreCase(k)) {
                    fs.percentage = Float.parseFloat(v);
                } else if ("r".equalsIgnoreCase(k)) {
                    if (fs.customColor == null) {
                        fs.customColor = new Color(1.0F, 1.0F, 1.0F);
                    }

                    fs.customColor.r = Float.parseFloat(v);
                } else if ("g".equalsIgnoreCase(k)) {
                    if (fs.customColor == null) {
                        fs.customColor = new Color(1.0F, 1.0F, 1.0F);
                    }

                    fs.customColor.g = Float.parseFloat(v);
                } else if ("b".equalsIgnoreCase(k)) {
                    if (fs.customColor == null) {
                        fs.customColor = new Color(1.0F, 1.0F, 1.0F);
                    }

                    fs.customColor.b = Float.parseFloat(v);
                }
            }

            return fs;
        }
    }

    private FluidContainerScript.FluidScript readFluid(String v) {
        String[] opt = v.split(":");
        FluidContainerScript.FluidScript fs = this.getOrCreateFluidScript(opt[0]);
        if (opt.length > 1) {
            fs.percentage = Float.parseFloat(opt[1]);
        }

        if (opt.length == 5) {
            float r = Float.parseFloat(opt[2]);
            float g = Float.parseFloat(opt[3]);
            float b = Float.parseFloat(opt[4]);
            Color c = new Color(r, g, b);
            fs.customColor = c;
        }

        return fs;
    }

    public FluidFilter getWhitelistCopy() {
        return this.whitelist != null ? this.whitelist.getFilter().copy() : null;
    }

    public FluidFilter getBlacklistCopy() {
        return this.blacklist != null ? this.blacklist.getFilter().copy() : null;
    }

    public String getContainerName() {
        return this.containerName;
    }

    public String getCustomDrinkSound() {
        return this.customDrinkSound;
    }

    public float getCapacity() {
        return this.capacity;
    }

    public float getInitialAmount() {
        if (!this.initialAmountSet) {
            return this.capacity;
        } else {
            return this.initialAmountMin == this.initialAmountMax ? this.initialAmountMin : Rand.Next(this.initialAmountMin, this.initialAmountMax);
        }
    }

    public ArrayList<FluidContainerScript.FluidScript> getInitialFluids() {
        return this.initialFluids;
    }

    public boolean isInitialFluidsIsRandom() {
        return this.initialFluidsIsRandom;
    }

    public boolean getInputLocked() {
        return this.inputLocked;
    }

    public boolean getCanEmpty() {
        return this.canEmpty;
    }

    public boolean isHiddenAmount() {
        return this.hiddenAmount;
    }

    public float getRainCatcher() {
        return this.rainCatcher;
    }

    public boolean isFilledWithCleanWater() {
        return this.fillsWithCleanWater;
    }

    @DebugClassFields
    @UsedFromLua
    public static class FluidScript {
        private final String fluidType;
        private float percentage = 1.0F;
        private Color customColor;
        private Fluid fluid;

        private FluidScript(String fluidType) {
            this.fluidType = fluidType;
        }

        public String getFluidType() {
            return this.fluidType;
        }

        public Fluid getFluid() {
            if (this.fluid == null) {
                this.fluid = Fluid.Get(this.fluidType);
                if (this.fluid == null) {
                    DebugLog.General.warn("Cannot find fluid '" + this.fluidType + "' in fluid script.");
                }
            }

            return this.fluid;
        }

        protected void setPercentage(float f) {
            this.percentage = f;
        }

        public float getPercentage() {
            return this.percentage;
        }

        public Color getCustomColor() {
            return this.customColor;
        }
    }
}
