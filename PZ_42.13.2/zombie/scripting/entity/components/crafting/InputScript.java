// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.FluidMatchMode;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.ItemApplyMode;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.OutputMapper;
import zombie.entity.components.crafting.recipe.OverlayMapper;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemTags;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponPart;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.StringUtils;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
@UsedFromLua
public class InputScript extends CraftRecipe.IOScript {
    private final ArrayList<String> loadedItems = new ArrayList<>();
    private final ArrayList<String> loadedFluids = new ArrayList<>();
    private final ArrayList<String> loadedEnergies = new ArrayList<>();
    private final ArrayList<String> items = new ArrayList<>();
    private final Set<ItemTag> itemTags = new HashSet<>();
    private final ArrayList<String> categories = new ArrayList<>();
    private final FluidFilter fluidFilter = new FluidFilter();
    private final ArrayList<Fluid> filteredFluidCache = new ArrayList<>();
    private final ArrayList<Energy> energies = new ArrayList<>();
    private boolean acceptsAnyItem;
    private boolean acceptsAnyFluid;
    private boolean acceptsAnyEnergy;
    private final ResourceType type;
    private List<Item> itemScriptCache = new ArrayList<>();
    private ItemApplyMode itemApplyMode = ItemApplyMode.Normal;
    private FluidMatchMode fluidMatchMode = FluidMatchMode.Exact;
    private float amount;
    private float maxamount;
    private final ArrayList<Float> amounts = new ArrayList<>();
    private final ArrayList<Float> maxamounts = new ArrayList<>();
    private boolean applyOnTick;
    private int shapedIndex = -1;
    private String originalLine = "";
    protected OutputScript createToItemScript;
    protected InputScript consumeFromItemScript;
    protected InputScript parentScript;
    private final EnumSet<InputFlag> flags = EnumSet.noneOf(InputFlag.class);

    private InputScript(CraftRecipe parentRecipe, ResourceType type) {
        super(parentRecipe);
        this.type = type;
    }

    private boolean typeCheck(ResourceType type) {
        return this.type == type;
    }

    protected boolean isValid() {
        if (this.type == ResourceType.Item) {
            return this.acceptsAnyItem || !this.items.isEmpty();
        } else if (this.type == ResourceType.Fluid) {
            return this.acceptsAnyFluid || this.fluidFilter.isSealed();
        } else {
            return this.type != ResourceType.Energy ? false : this.acceptsAnyEnergy || !this.energies.isEmpty();
        }
    }

    public List<Item> getPossibleInputItems() {
        return this.itemScriptCache;
    }

    public boolean hasPossibleFrozenFoodInputItems() {
        for (int i = 0; i < this.itemScriptCache.size(); i++) {
            Item item = this.itemScriptCache.get(i);
            if (item.isItemType(ItemType.FOOD) && !item.isCantBeFrozen()) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Fluid> getPossibleInputFluids() {
        return this.acceptsAnyFluid ? Fluid.getAllFluids() : this.filteredFluidCache;
    }

    public String getInputFluidFilterDisplayName() {
        return this.acceptsAnyFluid ? Translator.getText("Fluid_Name_Any") : this.fluidFilter.getFilterDisplayName();
    }

    public String getInputFluidFilterTooltip() {
        return this.acceptsAnyFluid ? Translator.getText("Fluid_Name_Any") : this.fluidFilter.getFilterTooltipText();
    }

    public ArrayList<Energy> getPossibleInputEnergies() {
        return this.acceptsAnyEnergy ? Energy.getAllEnergies() : this.energies;
    }

    public boolean hasCreateToItem() {
        return this.createToItemScript != null;
    }

    public OutputScript getCreateToItemScript() {
        return this.createToItemScript;
    }

    public boolean hasConsumeFromItem() {
        return this.consumeFromItemScript != null;
    }

    public InputScript getConsumeFromItemScript() {
        return this.consumeFromItemScript;
    }

    public boolean hasParentScript() {
        return this.parentScript != null;
    }

    public InputScript getParentScript() {
        return this.parentScript;
    }

    public boolean hasFlag(InputFlag flag) {
        return this.flags.contains(flag);
    }

    public String getOriginalLine() {
        return this.originalLine;
    }

    public ResourceType getResourceType() {
        return this.type;
    }

    public boolean isUsesPartialItem(Item item) {
        if (this.isItemCount() || this.isDestroy() || this.isKeep()) {
            return false;
        } else if (item != null && item.isItemType(ItemType.DRAINABLE)) {
            return item.getUseDelta() < 1.0F;
        } else {
            return item != null && item.isItemType(ItemType.FOOD) ? Math.abs(item.getHungerChange()) > 1.0F : false;
        }
    }

    public boolean isExclusive() {
        return this.hasFlag(InputFlag.IsExclusive);
    }

    public boolean isItemCount() {
        return this.hasFlag(InputFlag.ItemCount);
    }

    public boolean isDestroy() {
        return this.itemApplyMode == ItemApplyMode.Destroy;
    }

    public boolean isKeep() {
        return this.itemApplyMode == ItemApplyMode.Keep;
    }

    public boolean isTool() {
        return this.hasFlag(InputFlag.ToolRight) || this.hasFlag(InputFlag.ToolLeft);
    }

    public boolean isToolLeft() {
        return this.hasFlag(InputFlag.ToolLeft);
    }

    public boolean isToolRight() {
        return this.hasFlag(InputFlag.ToolRight);
    }

    public boolean isWorn() {
        return this.hasFlag(InputFlag.IsWorn);
    }

    public boolean isNotWorn() {
        return this.hasFlag(InputFlag.IsNotWorn);
    }

    public boolean isFull() {
        return this.hasFlag(InputFlag.IsFull);
    }

    public boolean isEmpty() {
        return this.hasFlag(InputFlag.IsEmpty);
    }

    public boolean notFull() {
        return this.hasFlag(InputFlag.NotFull);
    }

    public boolean notEmpty() {
        return this.hasFlag(InputFlag.NotEmpty);
    }

    public boolean isDamaged() {
        return this.hasFlag(InputFlag.IsDamaged);
    }

    public boolean isUndamaged() {
        return this.hasFlag(InputFlag.IsUndamaged);
    }

    public boolean allowFrozenItem() {
        return this.hasFlag(InputFlag.AllowFrozenItem);
    }

    public boolean dontAllowFrozenItem() {
        return this.getResourceType() == ResourceType.Item && this.hasPossibleFrozenFoodInputItems() ? !this.hasFlag(InputFlag.AllowFrozenItem) : false;
    }

    public boolean allowRottenItem() {
        return this.hasFlag(InputFlag.AllowRottenItem);
    }

    public boolean allowDestroyedItem() {
        return this.hasFlag(InputFlag.AllowDestroyedItem);
    }

    public boolean isEmptyContainer() {
        return this.hasFlag(InputFlag.IsEmptyContainer);
    }

    public boolean isWholeFoodItem() {
        return this.hasFlag(InputFlag.IsWholeFoodItem);
    }

    public boolean isUncookedFoodItem() {
        return this.hasFlag(InputFlag.IsUncookedFoodItem);
    }

    public boolean isCookedFoodItem() {
        return this.hasFlag(InputFlag.IsCookedFoodItem);
    }

    public boolean isHeadPart() {
        return this.hasFlag(InputFlag.IsHeadPart);
    }

    public boolean isSharpenable() {
        return this.hasFlag(InputFlag.IsSharpenable);
    }

    public boolean dontPutBack() {
        return this.hasFlag(InputFlag.DontPutBack);
    }

    public boolean inheritColor() {
        return this.hasFlag(InputFlag.InheritColor);
    }

    public boolean inheritCondition() {
        return this.hasFlag(InputFlag.InheritCondition);
    }

    public boolean inheritHeadCondition() {
        return this.hasFlag(InputFlag.InheritHeadCondition);
    }

    public boolean inheritSharpness() {
        return this.hasFlag(InputFlag.InheritSharpness);
    }

    public boolean inheritUses() {
        return this.hasFlag(InputFlag.InheritUses);
    }

    public boolean isNotDull() {
        return this.hasFlag(InputFlag.IsNotDull);
    }

    public boolean mayDegrade() {
        return this.hasFlag(InputFlag.MayDegrade);
    }

    public boolean mayDegradeLight() {
        return this.hasFlag(InputFlag.MayDegradeLight);
    }

    public boolean mayDegradeVeryLight() {
        return this.hasFlag(InputFlag.MayDegradeVeryLight);
    }

    public boolean mayDegradeHeavy() {
        return this.hasFlag(InputFlag.MayDegradeHeavy);
    }

    public boolean sharpnessCheck() {
        return this.hasFlag(InputFlag.SharpnessCheck);
    }

    @Deprecated
    public int getShapedIndex() {
        return this.shapedIndex;
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
        return this.fluidMatchMode == FluidMatchMode.Primary;
    }

    public boolean isFluidMixture() {
        return this.fluidMatchMode == FluidMatchMode.Mixture;
    }

    public boolean isFluidAnything() {
        return this.fluidMatchMode == FluidMatchMode.Anything;
    }

    public boolean isVariableAmount() {
        return this.amount != this.maxamount;
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

    public int getIntAmount(int idx) {
        return (int)this.getAmount(idx);
    }

    public float getAmount(int idx) {
        return idx < this.amounts.size() ? this.amounts.get(idx) : this.amount;
    }

    public int getIntMaxAmount(int idx) {
        return (int)this.getMaxAmount(idx);
    }

    public float getMaxAmount(int idx) {
        return idx < this.maxamounts.size() ? this.maxamounts.get(idx) : this.maxamount;
    }

    public int getIntAmount(String item) {
        return (int)this.getAmount(item);
    }

    public float getAmount(String item) {
        if (!item.contains(".")) {
            item = "Base." + item;
        }

        for (int i = 0; i < this.items.size(); i++) {
            String it = this.items.get(i);
            if (!it.contains(".")) {
                it = "Base." + it;
            }

            if (item.equalsIgnoreCase(it)) {
                return this.getAmount(i);
            }
        }

        return 1.0F;
    }

    public int getIntMaxAmount(String item) {
        return (int)this.getMaxAmount(item);
    }

    public float getMaxAmount(String item) {
        if (!item.contains(".")) {
            item = "Base." + item;
        }

        for (int i = 0; i < this.items.size(); i++) {
            String it = this.items.get(i);
            if (!it.contains(".")) {
                it = "Base." + it;
            }

            if (item.equalsIgnoreCase(it)) {
                return this.getMaxAmount(i);
            }
        }

        return 1.0F;
    }

    public float getRelativeScale(String item) {
        float amt = this.getAmount(item);
        return this.amount != 0.0F && amt != 0.0F ? amt / this.amount : 1.0F;
    }

    public boolean isProp1() {
        return this.flags.contains(InputFlag.Prop1);
    }

    public boolean isProp2() {
        return this.flags.contains(InputFlag.Prop2);
    }

    public boolean isApplyOnTick() {
        return this.applyOnTick;
    }

    public boolean isAcceptsAnyItem() {
        return this.acceptsAnyItem;
    }

    public boolean isAcceptsAnyFluid() {
        return this.acceptsAnyFluid;
    }

    public boolean isAcceptsAnyEnergy() {
        return this.acceptsAnyEnergy;
    }

    public boolean isHandcraftOnly() {
        return this.flags.contains(InputFlag.HandcraftOnly);
    }

    public boolean isAutomationOnly() {
        return this.flags.contains(InputFlag.AutomationOnly);
    }

    @Deprecated
    public boolean isReplace() {
        return false;
    }

    @Deprecated
    public OutputScript getReplaceOutputScript() {
        return null;
    }

    public boolean containsItem(Item item) {
        return item != null && this.itemScriptCache.contains(item);
    }

    public boolean containsFluid(Fluid fluid) {
        return fluid != null && (this.acceptsAnyFluid || this.fluidFilter.allows(fluid));
    }

    public boolean containsEnergy(Energy energy) {
        return energy != null && (this.acceptsAnyEnergy || this.energies.contains(energy));
    }

    public boolean isFluidMatch(FluidContainer container) {
        if (this.type != ResourceType.Fluid || container == null) {
            return false;
        } else if (container.isEmpty()) {
            return false;
        } else {
            boolean fluidMatch;
            if (this.isFluidExact()) {
                fluidMatch = !container.isMixture() && this.containsFluid(container.getPrimaryFluid());
            } else if (this.isFluidMixture()) {
                fluidMatch = true;
                FluidSample sample = container.createFluidSample();

                for (int i = 0; i < sample.size(); i++) {
                    if (!this.containsFluid(sample.getFluid(i))) {
                        fluidMatch = false;
                        break;
                    }
                }

                sample.release();
            } else if (this.isFluidPrimary()) {
                fluidMatch = this.containsFluid(container.getPrimaryFluid());
            } else {
                fluidMatch = true;
            }

            return fluidMatch;
        }
    }

    public boolean isEnergyMatch(DrainableComboItem item) {
        return item != null && item.isEnergy() && this.isEnergyMatch(item.getEnergy());
    }

    public boolean isEnergyMatch(Energy energy) {
        return this.type != ResourceType.Energy ? false : this.containsEnergy(energy);
    }

    protected static InputScript LoadBlock(CraftRecipe parentRecipe, ScriptParser.Block block) throws Exception {
        InputScript input = null;

        for (ScriptParser.Value value : block.values) {
            if (!StringUtils.isNullOrWhitespace(value.string) && !value.string.contains("=") && StringUtils.containsWhitespace(value.string)) {
                DebugLog.General.warn("Cannot load: " + value.string + ", recipe:" + parentRecipe.getScriptObjectFullType());
                String s = value.string.trim();
                input = Load(parentRecipe, s);
            }
        }

        if (input == null) {
            DebugLog.General.warn("Cannot load input block. " + parentRecipe.getScriptObjectFullType());
        }

        for (ScriptParser.Value valuex : block.values) {
            String key = valuex.getKey().trim();
            String val = valuex.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.equalsIgnoreCase("something")) {
            }
        }

        return input;
    }

    protected static InputScript Load(CraftRecipe parentRecipe, String line) throws Exception {
        return Load(parentRecipe, line, false);
    }

    protected static InputScript Load(CraftRecipe parentRecipe, String line, boolean isInternal) throws Exception {
        if (StringUtils.isNullOrWhitespace(line)) {
            return null;
        } else {
            String[] elems = line.trim().split("\\s+");
            StringUtils.trimArray(elems);
            String recipeName = parentRecipe.getScriptObjectFullType();
            ResourceType type = ResourceType.Any;
            if (elems[0].equalsIgnoreCase("item")) {
                type = ResourceType.Item;
            } else if (isInternal && elems[0].equalsIgnoreCase("uses")) {
                type = ResourceType.Item;
            } else if (elems[0].equalsIgnoreCase("fluid")) {
                type = ResourceType.Fluid;
            } else {
                if (!elems[0].equalsIgnoreCase("energy")) {
                    throw new Exception("unknown type in craftrecipe: " + elems[0]);
                }

                type = ResourceType.Energy;
            }

            InputScript input = new InputScript(parentRecipe, type);
            input.originalLine = line.trim();
            String inputAmount = elems[1];
            float baseamount = 1.0F;
            float upperamount = 1.0F;
            if (inputAmount.contains("variable")) {
                String valueString = inputAmount.substring(inputAmount.indexOf("[") + 1, inputAmount.indexOf("]"));
                String[] values = valueString.split(":");
                baseamount = PZMath.max(0.0F, Float.parseFloat(values[0]));
                upperamount = PZMath.max(0.0F, Float.parseFloat(values[1]));
            } else {
                baseamount = PZMath.max(0.0F, Float.parseFloat(inputAmount));
                upperamount = baseamount;
            }

            input.amount = baseamount;
            input.maxamount = upperamount;
            if (isInternal) {
                if (type == ResourceType.Item) {
                    input.acceptsAnyItem = true;
                }

                if (type == ResourceType.Fluid) {
                    input.acceptsAnyItem = true;
                }

                if (type == ResourceType.Energy) {
                    input.acceptsAnyItem = true;
                }
            }

            for (int i = 2; i < elems.length; i++) {
                String s = elems[i];
                if (s.startsWith("[")) {
                    s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                    String[] split = s.split(";");

                    for (int z = 0; z < split.length; z++) {
                        String entry = split[z];
                        if (type == ResourceType.Item) {
                            if ("*".equals(entry)) {
                                input.acceptsAnyItem = true;
                            } else if (entry.contains(":")) {
                                String[] split2 = entry.split(":");
                                input.loadedItems.add(split2[1]);
                                float itembaseamount = PZMath.max(0.0F, Float.parseFloat(split2[0]));
                                input.amounts.add(itembaseamount);
                                input.maxamounts.add(itembaseamount * (upperamount / baseamount));
                            } else {
                                input.loadedItems.add(entry);
                                input.amounts.add(baseamount);
                                input.maxamounts.add(upperamount);
                            }
                        } else if (type == ResourceType.Fluid) {
                            if ("*".equals(entry)) {
                                input.acceptsAnyFluid = true;
                            } else {
                                input.loadedFluids.add(entry);
                                input.amounts.add(baseamount);
                                input.maxamounts.add(upperamount);
                            }
                        } else if (type == ResourceType.Energy) {
                            if ("*".equals(entry)) {
                                input.acceptsAnyEnergy = true;
                            } else {
                                input.loadedEnergies.add(entry);
                                input.amounts.add(baseamount);
                                input.maxamounts.add(upperamount);
                            }
                        }
                    }
                } else if (s.startsWith("shapedIndex:")) {
                    s = s.substring(s.indexOf(":") + 1);
                    input.shapedIndex = Integer.parseInt(s);
                } else {
                    if (s.startsWith("apply:")) {
                        s = s.substring(s.indexOf(":") + 1);
                        throw new Exception("OnTick currently disabled for inputs.");
                    }

                    if (s.startsWith("mode:")) {
                        s = s.substring(s.indexOf(":") + 1);
                        switch (type) {
                            case Item:
                                if (!s.equalsIgnoreCase("use")) {
                                    if (s.equalsIgnoreCase("keep")) {
                                        input.itemApplyMode = ItemApplyMode.Keep;
                                    } else if (s.equalsIgnoreCase("destroy")) {
                                        input.itemApplyMode = ItemApplyMode.Destroy;
                                    } else if (s.equalsIgnoreCase("useprop1")) {
                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    } else if (s.equalsIgnoreCase("useprop2")) {
                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    } else if (s.equalsIgnoreCase("keepprop1")) {
                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    } else if (s.equalsIgnoreCase("keepprop2")) {
                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    } else if (s.equalsIgnoreCase("prop1")) {
                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    } else {
                                        if (!s.equalsIgnoreCase("prop2")) {
                                            throw new Exception("Invalid item mode Error - " + s);
                                        }

                                        DebugLog.General.error("Deprecated parameter in inputscript! for recipe " + recipeName);
                                    }
                                }
                                break;
                            case Fluid:
                                if (s.equalsIgnoreCase("exact")) {
                                    input.fluidMatchMode = FluidMatchMode.Exact;
                                } else if (s.equalsIgnoreCase("primary")) {
                                    input.fluidMatchMode = FluidMatchMode.Primary;
                                } else if (s.equalsIgnoreCase("mixture")) {
                                    input.fluidMatchMode = FluidMatchMode.Mixture;
                                } else {
                                    if (!s.equalsIgnoreCase("anything")) {
                                        throw new Exception("Invalid fluid mode Error");
                                    }

                                    input.fluidMatchMode = FluidMatchMode.Anything;
                                }
                                break;
                            case Energy:
                                if (!s.equalsIgnoreCase("keep")) {
                                    throw new Exception("Invalid energy mode Error");
                                }

                                input.itemApplyMode = ItemApplyMode.Keep;
                                break;
                            default:
                                DebugLog.General.warn("Cannot set mode for type = " + type + " for recipe " + recipeName);
                                if (Core.debug) {
                                    throw new Exception("Mode Error");
                                }
                        }
                    } else if (s.startsWith("tags")) {
                        if (type != ResourceType.Item) {
                            throw new Exception("cannot set tags on non-item: " + s);
                        }

                        s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                        String[] split = s.split(";");

                        for (int zx = 0; zx < split.length; zx++) {
                            String entry = split[zx];
                            input.itemTags.add(ItemTag.get(ResourceLocation.of(entry)));
                        }
                    } else if (s.startsWith("categories")) {
                        if (type != ResourceType.Fluid) {
                            throw new Exception("cannot set category on non-fluid: " + s);
                        }

                        s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                        String[] split = s.split(";");
                        input.categories.addAll(Arrays.asList(split));
                    } else if (s.startsWith("flags")) {
                        s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                        String[] split = s.split(";");

                        for (int zx = 0; zx < split.length; zx++) {
                            String entry = split[zx];
                            InputFlag flag = InputFlag.valueOf(entry);
                            input.flags.add(flag);
                        }
                    } else if (s.startsWith("mappers")) {
                        s = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
                        String[] split = s.split(";");

                        for (int zx = 0; zx < split.length; zx++) {
                            String entry = split[zx];
                            OutputMapper outputMapper = parentRecipe.getOrCreateOutputMapper(entry);
                            outputMapper.registerInputScript(input);
                        }
                    } else {
                        if (!s.startsWith("overlayMapper")) {
                            throw new Exception("unknown recipe param: " + s);
                        }

                        OverlayMapper overlayMapper = parentRecipe.getOverlayMapper();
                        overlayMapper.registerInputScript(input);
                    }
                }
            }

            if (input.amounts.size() > 1 && !input.isVariableAmount() && input.loadedFluids.isEmpty()) {
                input.amount = 1.0F;
                input.maxamount = 1.0F;
            }

            if (input.acceptsAnyItem) {
                input.loadedItems.clear();
                input.itemTags.clear();
            }

            if (input.acceptsAnyFluid) {
                input.loadedFluids.clear();
                input.categories.clear();
            }

            if (input.acceptsAnyEnergy) {
                input.loadedEnergies.clear();
            }

            return input;
        }
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    protected void OnPostWorldDictionaryInit() throws Exception {
        if (this.createToItemScript != null
            && this.consumeFromItemScript != null
            && this.createToItemScript.getResourceType() == this.consumeFromItemScript.getResourceType()) {
            throw new Exception("Input line cannot have a '-' and '+' line of the same ResourceType. line: " + this.originalLine);
        } else {
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

                this.itemApplyMode = ItemApplyMode.Keep;
            }

            if (this.consumeFromItemScript != null) {
                this.consumeFromItemScript.OnPostWorldDictionaryInit();
                if (this.getIntAmount() != 1 || this.isVariableAmount()) {
                    throw new Exception("Lines prior to a '-' line should have 1 item amount. line: " + this.originalLine);
                }

                if (this.type != ResourceType.Item) {
                    throw new Exception("Lines prior to a '-' line should be of resource type Item. line: " + this.originalLine);
                }

                if (this.applyOnTick) {
                    throw new Exception("Lines prior to a '-' line should not be apply on tick. line: " + this.originalLine);
                }

                if (this.itemApplyMode != ItemApplyMode.Destroy) {
                    this.itemApplyMode = ItemApplyMode.Keep;
                }
            }

            if (this.acceptsAnyItem) {
                this.itemScriptCache.clear();
                if (this.flags.contains(InputFlag.ItemIsFluid)
                    || this.consumeFromItemScript != null && this.consumeFromItemScript.getResourceType() == ResourceType.Fluid
                    || this.createToItemScript != null && this.createToItemScript.getResourceType() == ResourceType.Fluid) {
                    ArrayList<Item> allItems = ScriptManager.instance.getAllItems();

                    for (int i = 0; i < allItems.size(); i++) {
                        Item item = allItems.get(i);
                        if (item.containsComponent(ComponentType.FluidContainer)) {
                            this.itemScriptCache.add(item);
                        }
                    }
                } else if (this.flags.contains(InputFlag.ItemIsEnergy)
                    || this.consumeFromItemScript != null && this.consumeFromItemScript.getResourceType() == ResourceType.Energy
                    || this.createToItemScript != null && this.createToItemScript.getResourceType() == ResourceType.Energy) {
                    ArrayList<Item> allItems = ScriptManager.instance.getAllItems();

                    for (int ix = 0; ix < allItems.size(); ix++) {
                        Item item = allItems.get(ix);
                        if (item.isItemType(ItemType.DRAINABLE)) {
                            this.itemScriptCache.add(item);
                        }
                    }
                } else {
                    this.itemScriptCache = PZUnmodifiableList.wrap(ScriptManager.instance.getAllItems());
                }
            } else {
                for (String s : this.loadedItems) {
                    Item item = ScriptManager.instance.getItem(s);
                    if (item == null) {
                        throw new Exception(this.getParentRecipe().getName() + " item not found: " + s + ". line: " + this.originalLine);
                    }

                    if (!this.items.contains(s)) {
                        this.items.add(s);
                        this.itemScriptCache.add(item);
                    }

                    item.getUsedInRecipes().add(this.getParentRecipe().getName());
                    if (this.getParentRecipe().isBuildableRecipe()) {
                        item.isUsedInBuildRecipes = true;
                    }
                }

                for (ItemTag itemTag : this.itemTags) {
                    ArrayList<Item> tagItems = ItemTags.getItemsForTag(itemTag);
                    if (tagItems == null || tagItems.isEmpty()) {
                        throw new Exception("Tag has no items: " + itemTag.toString() + ". line: " + this.originalLine);
                    }

                    for (Item itemx : tagItems) {
                        if (!this.items.contains(itemx.getFullName())) {
                            this.items.add(itemx.getFullName());
                            this.itemScriptCache.add(itemx);
                        }

                        itemx.getUsedInRecipes().add(this.getParentRecipe().getName());
                        if (this.getParentRecipe().isBuildableRecipe()) {
                            itemx.isUsedInBuildRecipes = true;
                        }
                    }
                }
            }

            this.fluidFilter.setFilterType(FluidFilter.FilterType.Whitelist);

            for (String s : this.loadedFluids) {
                Fluid fluid = Fluid.Get(s);
                if (fluid == null) {
                    throw new Exception("Fluid not found: " + s + ". line: " + this.originalLine);
                }

                this.fluidFilter.add(fluid);
            }

            for (String s : this.categories) {
                FluidCategory fc = FluidCategory.valueOf(s);
                if (fc == null) {
                    throw new Exception("FluidCategory not found: " + s + ". line: " + this.originalLine);
                }

                this.fluidFilter.add(fc);
            }

            this.fluidFilter.seal();

            for (Fluid fluid : Fluid.getAllFluids()) {
                if (this.fluidFilter.allows(fluid)) {
                    this.filteredFluidCache.add(fluid);
                }
            }

            for (String s : this.loadedEnergies) {
                Energy energy = Energy.Get(s);
                if (energy == null) {
                    throw new Exception("Energy not found: " + s + ". line: " + this.originalLine);
                }

                if (!this.energies.contains(energy)) {
                    this.energies.add(energy);
                }
            }

            if (!this.isValid()) {
                throw new Exception("Invalid input. line: " + this.originalLine);
            }
        }
    }

    public boolean canUseItem(InventoryItem item, IsoGameCharacter character) {
        return CraftRecipeManager.isItemValidForInputScript(this, item, character);
    }

    public boolean canUseItem(String item) {
        if (this.getResourceType() != ResourceType.Item) {
            return false;
        } else {
            List<Item> inputItems = this.getPossibleInputItems();

            for (int i = 0; i < inputItems.size(); i++) {
                Item inputItem = this.getPossibleInputItems().get(i);
                if (Objects.equals(inputItem.getName(), item) || Objects.equals(inputItem.getFullName(), item)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean allowFavorites() {
        return this.isKeep() || this.hasFlag(InputFlag.AllowFavorite);
    }

    public boolean passesFavoriteTest(InventoryItem item) {
        return !item.isFavorite() ? true : this.allowFavorites();
    }

    public boolean passesRottenTest(InventoryItem item) {
        if (!item.isFood()) {
            return true;
        } else {
            return !((Food)item).isRotten() ? true : this.hasFlag(InputFlag.AllowRottenItem);
        }
    }

    public boolean passesFrozenTest(InventoryItem item) {
        if (!item.isFood()) {
            return true;
        } else {
            return !((Food)item).isFrozen() ? true : this.hasFlag(InputFlag.AllowFrozenItem);
        }
    }

    public boolean passesBrokenTest(InventoryItem item) {
        return !item.isBroken() ? true : this.hasFlag(InputFlag.AllowDestroyedItem);
    }

    public boolean passesSealedTest(InventoryItem item) {
        if (this.hasFlag(InputFlag.IsSealed)) {
            return item.getName() != null && item.getName().contains(Translator.getFluidText("Fluid_Sealed"));
        } else {
            return !this.hasFlag(InputFlag.IsNotSealed) ? true : item.getName() != null && !item.getName().contains(Translator.getFluidText("Fluid_Sealed"));
        }
    }

    public boolean doesItemPassRoutineStatusTests(InventoryItem item, IsoGameCharacter character) {
        return character != null && character instanceof IsoPlayer isoPlayer && item.isNoRecipes(isoPlayer)
            ? false
            : this.passesFavoriteTest(item)
                && this.passesRottenTest(item)
                && this.passesFrozenTest(item)
                && this.passesBrokenTest(item)
                && this.passesSealedTest(item);
    }

    public boolean doesItemPassClothingTypeStatusTests(InventoryItem item) {
        if (item.getContainer() != null && item.getContainer().getParent() instanceof IsoDeadBody) {
            return true;
        } else {
            return this.hasFlag(InputFlag.IsWorn) && !item.isWorn() ? false : !this.hasFlag(InputFlag.IsNotWorn) || !item.isWorn() && !item.isEquipped();
        }
    }

    public boolean doesItemPassSharpnessStatusTests(InventoryItem item) {
        if (this.hasFlag(InputFlag.IsNotDull) && item.hasSharpness() && item.isDull()) {
            return false;
        } else if (this.hasFlag(InputFlag.IsSharpenable) && item.hasSharpness() && !(item.getSharpness() < item.getMaxSharpness())) {
            return false;
        } else if (!this.hasFlag(InputFlag.IsSharpenable) || item.hasSharpness()) {
            return true;
        } else {
            return item.hasHeadCondition() ? item.getHeadCondition() > 0 : item.getCondition() > 0;
        }
    }

    public boolean doesItemPassDamageStatusTests(InventoryItem item) {
        return this.hasFlag(InputFlag.IsDamaged) && !item.isDamaged() ? false : !this.hasFlag(InputFlag.IsUndamaged) || !item.isDamaged();
    }

    public boolean doesItemPassIsOrNotEmptyAndFullTests(InventoryItem item) {
        if (item instanceof InventoryContainer containerItem && this.hasFlag(InputFlag.IsEmpty) && !containerItem.isEmpty()) {
            return false;
        } else {
            if (item instanceof DrainableComboItem comboItem) {
                if (this.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsEmpty) && !comboItem.isEmptyUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotFull) && comboItem.isFullUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotEmpty) && comboItem.isEmptyUses()) {
                    return false;
                }
            }

            if (item instanceof Radio radio && radio.getDeviceData().getIsBatteryPowered()) {
                if (this.hasFlag(InputFlag.IsFull) && radio.getDeviceData().getPower() != 1.0F) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsEmpty) && radio.getDeviceData().getHasBattery()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotFull) && radio.getDeviceData().getPower() == 1.0F) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotEmpty) && !radio.getDeviceData().getHasBattery()) {
                    return false;
                }
            }

            if (item instanceof WeaponPart weaponPart) {
                if (this.hasFlag(InputFlag.IsFull) && weaponPart.getCurrentUses() != weaponPart.getMaxUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsEmpty) && weaponPart.getCurrentUses() > 0) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotFull) && weaponPart.getCurrentUses() == weaponPart.getMaxUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.NotEmpty) && weaponPart.getCurrentUses() <= 0) {
                    return false;
                }
            }

            if (item.isFluidContainer()) {
                FluidContainer fc = item.getFluidContainer();
                if (this.hasFlag(InputFlag.IsFull) && !fc.isFull()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsEmpty) && !fc.isEmpty()) {
                    return false;
                }
            }

            if (this.hasFlag(InputFlag.ItemIsEnergy)) {
                if (!(item instanceof DrainableComboItem comboItem)) {
                    return false;
                }

                if (!comboItem.isEnergy()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsFull) && !comboItem.isFullUses()) {
                    return false;
                }

                if (this.hasFlag(InputFlag.IsEmpty) && !comboItem.isEmptyUses()) {
                    return false;
                }
            }

            return true;
        }
    }

    public boolean doesItemPassFoodAndCookingTests(InventoryItem item) {
        if (!(item instanceof Food)) {
            return true;
        } else if (this.hasFlag(InputFlag.IsWholeFoodItem) && item instanceof Food food && !food.isWholeFoodItem()) {
            return false;
        } else {
            return this.hasFlag(InputFlag.IsUncookedFoodItem) && item instanceof Food foodx && item.isCookable() && !foodx.isUncooked()
                ? false
                : !(this.hasFlag(InputFlag.IsCookedFoodItem) && item instanceof Food food) || !item.isCookable() || !food.isUncooked();
        }
    }

    public boolean isCanBeDoneFromFloor() {
        return this.hasFlag(InputFlag.CanBeDoneFromFloor) || this.getParentRecipe().isCanBeDoneFromFloor();
    }

    public boolean isRecordInput() {
        if (this.hasFlag(InputFlag.RecordInput)) {
            return true;
        } else if (this.isKeep()) {
            return false;
        } else {
            return this.hasFlag(InputFlag.DontRecordInput) ? false : this.getParentRecipe().hasTag(CraftRecipeTag.ENTITY_RECIPE);
        }
    }

    public Set<ItemTag> getItemTags() {
        return this.itemTags;
    }
}
