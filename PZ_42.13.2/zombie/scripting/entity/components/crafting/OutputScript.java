// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import java.util.EnumSet;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class OutputScript extends CraftRecipe.IOScript {
    private static final ArrayList<Item> _emptyItems = new ArrayList<>();
    private final ResourceType type;
    private String loadedFluid;
    private String loadedEnergy;
    private Fluid fluid;
    private Energy energy;
    private float amount;
    private float maxamount;
    private float chance = 1.0F;
    private boolean applyOnTick;
    @Deprecated
    private int shapedIndex = -1;
    private ItemApplyMode itemApplyMode = ItemApplyMode.Normal;
    private FluidMatchMode fluidMatchMode = FluidMatchMode.Exact;
    private String originalLine = "";
    protected OutputScript createToItemScript;
    private final EnumSet<OutputFlag> flags = EnumSet.noneOf(OutputFlag.class);
    private OutputMapper outputMapper;
    private final ArrayList<Fluid> possibleFluids = new ArrayList<>();
    private final ArrayList<Energy> possiblyEnergies = new ArrayList<>();

    private OutputScript(CraftRecipe parentRecipe, ResourceType type) {
        super(parentRecipe);
        this.type = type;
    }

    private boolean typeCheck(ResourceType type) {
        return this.type == type;
    }

    protected boolean isValid() {
        if (this.type == ResourceType.Item) {
            return !this.outputMapper.getResultItems().isEmpty();
        } else if (this.type == ResourceType.Fluid) {
            return this.fluid != null;
        } else {
            return this.type == ResourceType.Energy ? this.energy != null : false;
        }
    }

    public boolean hasCreateToItem() {
        return this.createToItemScript != null;
    }

    public OutputScript getCreateToItemScript() {
        return this.createToItemScript;
    }

    public boolean hasFlag(OutputFlag flag) {
        return this.flags.contains(flag);
    }

    @Deprecated
    public boolean isReplaceInput() {
        return false;
    }

    public String getOriginalLine() {
        return this.originalLine;
    }

    public ResourceType getResourceType() {
        return this.type;
    }

    public float getChance() {
        return this.chance;
    }

    public int getIntAmount() {
        return (int)this.amount;
    }

    public float getAmount() {
        return this.amount;
    }

    public int getIntMaxAmount() {
        return (int)this.maxamount;
    }

    public float getMaxAmount() {
        return this.maxamount;
    }

    public boolean isVariableAmount() {
        return this.amount != this.maxamount;
    }

    @Deprecated
    public int getShapedIndex() {
        return this.shapedIndex;
    }

    public boolean isApplyOnTick() {
        return this.applyOnTick;
    }

    public boolean isHandcraftOnly() {
        return this.flags.contains(OutputFlag.HandcraftOnly);
    }

    public boolean isAutomationOnly() {
        return this.flags.contains(OutputFlag.AutomationOnly);
    }

    public ArrayList<Item> getPossibleResultItems() {
        if (this.outputMapper == null) {
            DebugLog.General.warn("This output does not have items! returning empty list.");
            return _emptyItems;
        } else {
            return this.outputMapper.getResultItems();
        }
    }

    public ArrayList<Fluid> getPossibleResultFluids() {
        if (this.type == ResourceType.Fluid && this.possibleFluids.isEmpty() && this.fluid != null) {
            this.possibleFluids.add(this.fluid);
        }

        return this.possibleFluids;
    }

    public ArrayList<Energy> getPossibleResultEnergies() {
        if (this.type == ResourceType.Energy && this.possiblyEnergies.isEmpty() && this.energy != null) {
            this.possiblyEnergies.add(this.energy);
        }

        return this.possiblyEnergies;
    }

    public OutputMapper getOutputMapper() {
        return this.outputMapper;
    }

    public Item getItem(CraftRecipeData recipeData) {
        if (this.outputMapper == null) {
            DebugLog.General.warn("This output does not have items! returning null.");
            return null;
        } else {
            return this.outputMapper.getOutputItem(recipeData);
        }
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public Energy getEnergy() {
        return this.energy;
    }

    public ItemApplyMode getItemApplyMode() {
        return this.itemApplyMode;
    }

    public FluidMatchMode getFluidMatchMode() {
        return this.fluidMatchMode;
    }

    public boolean isFluidExact() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Exact;
    }

    public boolean isFluidPrimary() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Primary;
    }

    public boolean isFluidAnything() {
        return this.typeCheck(ResourceType.Fluid) && this.fluidMatchMode == FluidMatchMode.Anything;
    }

    @Deprecated
    public boolean isCreateUses() {
        return this.typeCheck(ResourceType.Item);
    }

    public boolean containsItem(Item item) {
        return true;
    }

    public boolean containsFluid(Fluid fluid) {
        return this.typeCheck(ResourceType.Fluid) && this.fluid != null && this.fluid.equals(fluid);
    }

    public boolean containsEnergy(Energy energy) {
        return this.typeCheck(ResourceType.Energy) && this.energy != null && this.energy.equals(energy);
    }

    public boolean isFluidMatch(FluidContainer container) {
        if (!this.typeCheck(ResourceType.Fluid) || container == null) {
            return false;
        } else if (container.isEmpty()) {
            return true;
        } else {
            boolean fluidMatch;
            if (this.isFluidExact()) {
                fluidMatch = !container.isMixture() && this.containsFluid(container.getPrimaryFluid());
            } else if (this.isFluidPrimary()) {
                fluidMatch = this.containsFluid(container.getPrimaryFluid());
            } else {
                fluidMatch = true;
            }

            return fluidMatch;
        }
    }

    public boolean isEnergyMatch(DrainableComboItem item) {
        return this.typeCheck(ResourceType.Energy) && item != null && item.isEnergy() && this.isEnergyMatch(item.getEnergy());
    }

    public boolean isEnergyMatch(Energy energy) {
        return !this.typeCheck(ResourceType.Energy) ? false : this.containsEnergy(energy);
    }

    protected static OutputScript LoadBlock(CraftRecipe parentRecipe, ScriptParser.Block block) throws Exception {
        OutputScript output = null;

        for (ScriptParser.Value value : block.values) {
            if (!StringUtils.isNullOrWhitespace(value.string) && !value.string.contains("=") && StringUtils.containsWhitespace(value.string)) {
                DebugLog.General.warn("Cannot load: " + value.string + ", recipe:" + parentRecipe.getScriptObjectFullType());
                String s = value.string.trim();
                output = Load(parentRecipe, s);
            }
        }

        if (output == null) {
            DebugLog.General.warn("Cannot load output block. " + parentRecipe.getScriptObjectFullType());
        }

        for (ScriptParser.Value valuex : block.values) {
            String key = valuex.getKey().trim();
            String val = valuex.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.equalsIgnoreCase("something")) {
            }
        }

        return output;
    }

    protected static OutputScript Load(CraftRecipe parentRecipe, String line) throws Exception {
        return Load(parentRecipe, line, false);
    }

    protected static OutputScript Load(CraftRecipe parentRecipe, String line, boolean isInternal) throws Exception {
        if (StringUtils.isNullOrWhitespace(line)) {
            return null;
        } else {
            String[] elems = line.trim().split("\\s+");
            StringUtils.trimArray(elems);
            boolean isUses = false;
            ResourceType type = ResourceType.Any;
            if (elems[0].equalsIgnoreCase("item")) {
                type = ResourceType.Item;
            } else if (elems[0].equalsIgnoreCase("fluid")) {
                type = ResourceType.Fluid;
            } else {
                if (!elems[0].equalsIgnoreCase("energy")) {
                    throw new Exception("unknown type in craftrecipe: " + elems[0]);
                }

                type = ResourceType.Energy;
            }

            OutputScript output = new OutputScript(parentRecipe, type);
            output.originalLine = line.trim();
            String outputAmount = elems[1];
            float baseamount = 1.0F;
            float upperamount = 1.0F;
            if (outputAmount.contains("variable")) {
                String valueString = outputAmount.substring(outputAmount.indexOf("[") + 1, outputAmount.indexOf("]"));
                String[] values = valueString.split(":");
                baseamount = PZMath.max(0.0F, Float.parseFloat(values[0]));
                upperamount = PZMath.max(0.0F, Float.parseFloat(values[1]));
            } else {
                baseamount = PZMath.max(0.0F, Float.parseFloat(outputAmount));
                upperamount = baseamount;
            }

            output.amount = baseamount;
            output.maxamount = upperamount;
            String result = elems[2];
            if (type == ResourceType.Item) {
                if (!isInternal) {
                    if (result.startsWith("mapper:")) {
                        result = result.substring(result.indexOf(":") + 1);
                        output.outputMapper = parentRecipe.getOutputMapper(result);
                        if (output.outputMapper == null) {
                            throw new Exception("Could not find output mapper: " + result);
                        }
                    } else {
                        output.outputMapper = new OutputMapper(result);
                        output.outputMapper.setDefaultOutputEntree(result);
                    }
                } else {
                    if (!"Uses".equalsIgnoreCase(result) && Core.debug) {
                        throw new Exception("Parameter with index=2 should be 'Uses'.");
                    }

                    isUses = true;
                }
            } else if (type == ResourceType.Fluid) {
                output.loadedFluid = result;
            } else if (type == ResourceType.Energy) {
                output.loadedEnergy = result;
            }

            for (int i = 3; i < elems.length; i++) {
                String s = elems[i];
                if (s.startsWith("chance:")) {
                    s = s.substring(s.indexOf(":") + 1);
                    output.chance = PZMath.clamp(Float.parseFloat(s), 0.0F, 1.0F);
                } else if (s.startsWith("shapedIndex:")) {
                    s = s.substring(s.indexOf(":") + 1);
                    output.shapedIndex = Integer.parseInt(s);
                } else if (s.startsWith("apply:")) {
                    if (isInternal) {
                        throw new Exception("Cannot apply 'onTick' on 'itemCreate' ('+' lines).");
                    }

                    s = s.substring(s.indexOf(":") + 1);
                    if (!s.equalsIgnoreCase("onTick")) {
                        throw new Exception("Apply Error");
                    }

                    if (type == ResourceType.Item) {
                        throw new Exception("Cannot apply 'onTick' on item.");
                    }

                    if (type == ResourceType.Fluid) {
                        throw new Exception("Cannot apply 'onTick' on fluid.");
                    }

                    output.applyOnTick = true;
                } else if (s.startsWith("mode:")) {
                    s = s.substring(s.indexOf(":") + 1);
                    switch (type) {
                        case Item:
                            break;
                        case Fluid:
                            if (s.equalsIgnoreCase("exact")) {
                                output.fluidMatchMode = FluidMatchMode.Exact;
                            } else if (s.equalsIgnoreCase("primary")) {
                                output.fluidMatchMode = FluidMatchMode.Primary;
                            } else {
                                if (!s.equalsIgnoreCase("anything")) {
                                    throw new Exception("Invalid fluid mode Error");
                                }

                                output.fluidMatchMode = FluidMatchMode.Anything;
                            }
                            break;
                        default:
                            DebugLog.General.warn("Cannot set mode for type = " + type);
                            if (Core.debug) {
                                throw new Exception("Mode Error");
                            }
                    }
                } else {
                    if (!s.startsWith("flags")) {
                        throw new Exception("unknown recipe param: " + s);
                    }

                    s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                    String[] split = s.split(";");

                    for (int z = 0; z < split.length; z++) {
                        String entry = split[z];
                        OutputFlag flag = OutputFlag.valueOf(entry);
                        output.flags.add(flag);
                    }
                }
            }

            return output;
        }
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    protected void OnPostWorldDictionaryInit() throws Exception {
        if (this.createToItemScript != null) {
            this.createToItemScript.OnPostWorldDictionaryInit();
            if (this.getIntAmount() != 1 || this.isVariableAmount()) {
                throw new Exception("Lines prior to a '+' line should have 1 item amount. line: " + this.originalLine);
            }

            if (this.type != ResourceType.Item) {
                throw new Exception("Lines prior to a '+' line should be of resource type Item. line: " + this.originalLine);
            }

            if (this.applyOnTick) {
                throw new Exception("Lines prior to a '+' line should not be apply on tick. line: " + this.originalLine);
            }

            this.itemApplyMode = ItemApplyMode.Normal;
        }

        if (this.type == ResourceType.Item) {
            if (this.outputMapper == null) {
                throw new Exception("No outputMapper set. line: " + this.originalLine);
            }

            this.outputMapper.OnPostWorldDictionaryInit(this.getParentRecipe().getName());
        } else if (this.type == ResourceType.Fluid) {
            Fluid fluid = Fluid.Get(this.loadedFluid);
            if (fluid == null) {
                throw new Exception("Fluid not found: " + this.loadedFluid + ", line: " + this.originalLine);
            }

            this.fluid = fluid;
        } else if (this.type == ResourceType.Energy) {
            Energy energy = Energy.Get(this.loadedEnergy);
            if (energy == null) {
                throw new Exception("Energy not found: " + this.loadedEnergy + ", line: " + this.originalLine);
            }

            this.energy = energy;
        }

        if (!this.isValid()) {
            throw new Exception("Invalid output. line: " + this.originalLine);
        }
    }

    public boolean canOutputItem(InventoryItem item) {
        return item.getScriptItem() == null ? false : this.canOutputItem(item.getScriptItem());
    }

    public boolean canOutputItem(Item item) {
        return this.getPossibleResultItems().contains(item);
    }
}
