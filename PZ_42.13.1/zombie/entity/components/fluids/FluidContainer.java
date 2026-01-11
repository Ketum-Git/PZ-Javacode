// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Moveable;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
@UsedFromLua
public class FluidContainer extends Component {
    public static final int MAX_FLUIDS = 8;
    public static final String DEF_CONTAINER_NAME = "FluidContainer";
    private static final Color colorDef = Color.white;
    private float capacity = 1.0F;
    private FluidFilter whitelist;
    private FluidFilter blacklist;
    private float temperature = 22.0F;
    private final ArrayList<FluidInstance> fluids = new ArrayList<>(8);
    private final SealedFluidProperties propertiesCache = new SealedFluidProperties();
    private float amountCache;
    private final Color color = new Color().set(colorDef);
    private String containerName = "FluidContainer";
    private String translatedContainerName;
    private String nameCache;
    private String customDrinkSound;
    private boolean cacheInvalidated;
    private boolean inputLocked;
    private boolean canPlayerEmpty = true;
    private boolean hiddenAmount;
    private float rainCatcher;
    private boolean fillsWithCleanWater;
    private static final ArrayList<FluidInstance> tempFluidUI = new ArrayList<>();
    private static final DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    public static FluidContainer CreateContainer() {
        return (FluidContainer)ComponentType.FluidContainer.CreateComponent();
    }

    public static void DisposeContainer(FluidContainer container) {
        ComponentType.ReleaseComponent(container);
    }

    private FluidContainer() {
        super(ComponentType.FluidContainer);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        FluidContainerScript script = (FluidContainerScript)componentScript;
        this.whitelist = script.getWhitelistCopy();
        this.blacklist = script.getBlacklistCopy();
        this.containerName = script.getContainerName();
        this.customDrinkSound = script.getCustomDrinkSound();
        this.rainCatcher = script.getRainCatcher();
        this.fillsWithCleanWater = script.isFilledWithCleanWater();
        this.inputLocked = false;
        this.canPlayerEmpty = true;
        this.setCapacity(script.getCapacity());
        if (script.getInitialFluids() != null && !script.getInitialFluids().isEmpty() && script.getInitialAmount() > 0.0F) {
            float initialAmount = script.getInitialAmount();
            if (script.getInitialFluids().size() == 1) {
                this.addInitialFluid(initialAmount, script.getInitialFluids().get(0), script.getName());
            } else if (script.isInitialFluidsIsRandom()) {
                int rnd = Rand.Next(script.getInitialFluids().size());
                this.addInitialFluid(initialAmount, script.getInitialFluids().get(rnd), script.getName());
            } else {
                for (int i = 0; i < script.getInitialFluids().size(); i++) {
                    FluidContainerScript.FluidScript fs = script.getInitialFluids().get(i);
                    this.addInitialFluid(initialAmount, fs, script.getName());
                }
            }
        }

        this.inputLocked = script.getInputLocked();
        this.canPlayerEmpty = script.getCanEmpty();
        this.hiddenAmount = script.isHiddenAmount();
    }

    private void addInitialFluid(float initialAmount, FluidContainerScript.FluidScript fs, String scriptName) {
        Fluid fluid = fs.getFluid();
        if (fluid != null) {
            float amount = initialAmount * fs.getPercentage();
            FluidInstance fluidInstance = FluidInstance.Alloc(fluid);
            if (fs.getCustomColor() != null) {
                fluidInstance.setColor(fs.getCustomColor());
            }

            this.addFluid(fluidInstance, amount);
            FluidInstance.Release(fluidInstance);
        } else {
            DebugLog.log("FluidContainer -> initial Fluid '" + fs.getFluidType() + "' not found! [" + scriptName + "]");
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.Empty();
        this.capacity = 1.0F;
        this.whitelist = null;
        this.blacklist = null;
        this.temperature = 22.0F;
        this.propertiesCache.clear();
        this.amountCache = 0.0F;
        this.color.set(colorDef);
        this.containerName = "FluidContainer";
        this.translatedContainerName = null;
        this.nameCache = null;
        this.cacheInvalidated = false;
        this.inputLocked = false;
        this.canPlayerEmpty = true;
        this.hiddenAmount = false;
        this.rainCatcher = 0.0F;
        this.fillsWithCleanWater = false;
        this.customDrinkSound = null;
    }

    public FluidContainer copy() {
        FluidContainer copy = CreateContainer();
        copy.capacity = this.capacity;

        for (FluidInstance fi : this.fluids) {
            copy.addFluid(fi, fi.getAmount());
        }

        if (this.whitelist != null) {
            copy.whitelist = this.whitelist.copy();
        }

        if (this.blacklist != null) {
            copy.blacklist = this.blacklist.copy();
        }

        copy.containerName = this.containerName;
        copy.recalculateCaches(true);
        return copy;
    }

    public void copyFluidsFrom(FluidContainer other) {
        for (int i = this.fluids.size() - 1; i >= 0; i--) {
            FluidInstance fi = this.fluids.get(i);
            FluidInstance.Release(fi);
        }

        this.fluids.clear();
        this.recalculateCaches(true);

        for (int i = 0; i < other.fluids.size(); i++) {
            FluidInstance fi = other.fluids.get(i);
            this.addFluid(fi, fi.getAmount());
        }

        this.recalculateCaches(true);
    }

    public String getCustomDrinkSound() {
        return this.customDrinkSound;
    }

    public void setInputLocked(boolean b) {
        this.inputLocked = b;
    }

    public boolean isInputLocked() {
        return this.inputLocked;
    }

    public boolean canPlayerEmpty() {
        return this.canPlayerEmpty;
    }

    public void setCanPlayerEmpty(boolean b) {
        this.canPlayerEmpty = b;
    }

    public float getRainCatcher() {
        return this.getOwner() instanceof IsoWorldInventoryObject obj && obj.getItem() instanceof Moveable mov && mov.getSpriteGrid() != null
            ? 0.0F
            : this.rainCatcher;
    }

    public void setRainCatcher(float rainCatcher) {
        this.rainCatcher = rainCatcher;
    }

    public boolean isFilledWithCleanWater() {
        return this.fillsWithCleanWater;
    }

    public boolean isHiddenAmount() {
        return this.hiddenAmount;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            IsoGameCharacter player = tooltipUI.getCharacter();
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getFluidText("Fluid_Amount") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue(FluidUtil.getFractionFormatted(this.getAmount(), this.getCapacity()), 1.0F, 1.0F, 1.0F, 1.0F);
            if (!this.isEmpty()) {
                if (this.fluids.size() == 1) {
                    FluidInstance fi = this.fluids.get(0);
                    item = layout.addItem();
                    item.setLabel(fi.getFluid().getTranslatedName() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    float f = fi.getAmount() / this.getCapacity();
                    item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0F);
                } else if (this.fluids.size() == 2 && this.fluids.get(0).getTranslatedName().equals(this.fluids.get(1).getTranslatedName())) {
                    FluidInstance fi = this.fluids.get(0);
                    item = layout.addItem();
                    item.setLabel(fi.getFluid().getTranslatedName() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    float f = (fi.getAmount() + this.fluids.get(1).getAmount()) / this.getCapacity();
                    item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0F);
                } else {
                    item = layout.addItem();
                    item.setLabel(Translator.getFluidText("Fluid_Mixture") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    float f = this.getAmount() / this.getCapacity();
                    item.setProgress(f, this.getColor().r, this.getColor().g, this.getColor().b, 1.0F);
                    item = layout.addItem();
                    item.setLabel(Translator.getFluidText("Fluid_Fluids") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    tempFluidUI.clear();

                    for (int i = 0; i < this.fluids.size(); i++) {
                        tempFluidUI.add(this.fluids.get(i));
                    }

                    tempFluidUI.sort(Comparator.comparing(FluidInstance::getTranslatedName));

                    for (int i = 0; i < tempFluidUI.size(); i++) {
                        FluidInstance fi = tempFluidUI.get(i);
                        item = layout.addItem();
                        item.setLabel(fi.getFluid().getTranslatedName() + ":", 0.7F, 0.7F, 0.4F, 1.0F);
                        f = fi.getAmount() / this.getAmount();
                        item.setProgress(f, fi.getColor().r, fi.getColor().g, fi.getColor().b, 1.0F);
                    }
                }

                if (this.isPoisonous() && this.getPoisonEffect() != PoisonEffect.None) {
                    ColorInfo badColor = Core.getInstance().getBadHighlitedColor();
                    item = layout.addItem();
                    item.setLabel(Translator.getText("Tooltip_tainted"), badColor.getR(), badColor.getG(), badColor.getB(), 1.0F);
                }

                if (Core.debug) {
                    if (this.propertiesCache.hasProperties()) {
                        item = layout.addItem();
                        item.setLabel(Translator.getFluidText("Fluid_Properties") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                        float amountMilli = this.getAmount() * 1000.0F;
                        this.doToolTipProp(layout, "Thirst", this.propertiesCache.getThirstChange() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Hunger", this.propertiesCache.getHungerChange() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Stress", this.propertiesCache.getStressChange() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Unhappy", this.propertiesCache.getUnhappyChange() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Fatigue", this.propertiesCache.getFatigueChange() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Calories", this.propertiesCache.getCalories() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Carbohydrates", this.propertiesCache.getCarbohydrates() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Lipids", this.propertiesCache.getLipids() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Proteins", this.propertiesCache.getProteins() / 1000.0F * amountMilli);
                        this.doToolTipProp(layout, "Alcohol", this.propertiesCache.getAlcohol());
                    }

                    if (this.isPoisonous()) {
                        String poison = Translator.getFluidText("Fluid_Poison")
                            + " "
                            + Translator.getFluidText("Fluid_Effect")
                            + " "
                            + Translator.getFluidText("Fluid_Per");
                        poison = poison + " " + FluidUtil.getAmountFormatted(this.getAmount());
                        item = layout.addItem();
                        item.setLabel(poison + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                        PoisonEffect effect = this.getPoisonEffect();
                        item = layout.addItem();
                        item.setLabel(Translator.getFluidText("Fluid_Effect") + ":", 0.7F, 0.7F, 0.4F, 1.0F);
                        item.setValue(Translator.getFluidText("Fluid_Poison_" + effect), 0.85F, 0.85F, 0.85F, 1.0F);
                    }

                    item = layout.addItem();
                    item.setLabel("Categories:", 1.0F, 1.0F, 0.8F, 1.0F);
                    ArrayList<String> cats = new ArrayList<>();

                    for (int i = 0; i < this.fluids.size(); i++) {
                        FluidInstance fi = this.fluids.get(i);

                        for (FluidCategory cat : fi.getFluid().getCategories()) {
                            if (!cats.contains(cat.toString())) {
                                cats.add(cat.toString());
                            }
                        }
                    }

                    Collections.sort(cats);

                    for (String s : cats) {
                        item = layout.addItem();
                        item.setLabel(s, 0.7F, 0.7F, 0.4F, 1.0F);
                    }
                }
            }
        }
    }

    private void doToolTipProp(ObjectTooltip.Layout layout, String transKey, float value) {
        if (this.getAmount() != 0.0F) {
            value /= this.getAmount();
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getFluidText("Fluid_Prop_" + transKey) + ":", 0.7F, 0.7F, 0.4F, 1.0F);
            if (transKey.equals("Alcohol")) {
                value = (int)(value * 100.0F);
                item.setValue(df.format(value) + "%", 0.85F, 0.85F, 0.85F, 1.0F);
            } else {
                item.setValue(df.format(value), 0.85F, 0.85F, 0.85F, 1.0F);
            }
        }
    }

    public String getContainerName() {
        return this.containerName;
    }

    public void setContainerName(String name) {
        if (StringUtils.containsWhitespace(name)) {
            DebugLog.General.error("Sanitizing container name '" + name + "', name may not contain whitespaces.");
            name = StringUtils.removeWhitespace(name);
        }

        this.containerName = name;
    }

    public String getTranslatedContainerName() {
        if (this.translatedContainerName == null) {
            this.translatedContainerName = Translator.getFluidText("Fluid_Container_" + this.containerName);
        }

        return this.translatedContainerName;
    }

    public String getUiName() {
        if (this.cacheInvalidated || this.nameCache == null) {
            this.recalculateCaches();
            String containerName = this.getTranslatedContainerName();
            if (this.fluids.isEmpty()) {
                this.nameCache = Translator.getText("Fluid_HoldingNone", containerName);
                InventoryItem item = null;
                if (this.getOwner() instanceof InventoryItem) {
                    item = (InventoryItem)this.getOwner();
                } else if (this.getOwner() instanceof IsoWorldInventoryObject) {
                    item = ((IsoWorldInventoryObject)this.getOwner()).getItem();
                }

                if (item != null && item.hasTag(ItemTag.OMIT_EMPTY_FROM_NAME)) {
                    this.nameCache = containerName;
                }
            } else if (this.fluids.size() == 1) {
                this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, this.fluids.get(0).getFluid().getTranslatedName());
                if (this.getPrimaryFluid() == Fluid.TaintedWater && SandboxOptions.instance.enableTaintedWaterText.getValue()) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }

                if (!this.canPlayerEmpty) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Sealed");
                }
            } else if (this.fluids.size() == 2 && this.isPoisonous()) {
                this.nameCache = containerName + " " + Translator.getFluidText("Fluid_Of") + " ";
                FluidInstance a = this.fluids.get(0);
                FluidInstance b = this.fluids.get(1);
                if (!a.getFluid().isCategory(FluidCategory.Poisons)) {
                    this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, a.getFluid().getTranslatedName());
                } else {
                    this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, b.getFluid().getTranslatedName());
                }

                if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }
            } else if (this.fluids.size() == 2 && !this.isPoisonous()) {
                FluidInstance ax = this.fluids.get(0);
                FluidInstance bx = this.fluids.get(1);
                FluidInstance fluid = ax.getAmount() < bx.getAmount() ? ax : bx;
                this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, fluid.getFluid().getTranslatedName());
                if (!ax.getTranslatedName().equals(bx.getTranslatedName())) {
                    FluidInstance fluidB = ax.getAmount() < bx.getAmount() ? bx : ax;
                    this.nameCache = Translator.getText(
                        "Fluid_HoldingTwoTypes", containerName, fluid.getFluid().getTranslatedName(), fluidB.getFluid().getTranslatedName()
                    );
                }

                if (this.contains(Fluid.TaintedWater) && this.getPoisonEffect() != PoisonEffect.None) {
                    this.nameCache = this.nameCache + " " + Translator.getFluidText("Fluid_Tainted");
                }
            } else {
                boolean allBeverages = true;
                int alcoholics = 0;

                for (int i = 0; i < this.fluids.size(); i++) {
                    FluidInstance fi = this.fluids.get(i);
                    if (!fi.getFluid().isCategory(FluidCategory.Beverage)) {
                        allBeverages = fi.getFluid().isCategory(FluidCategory.Poisons);
                    }

                    if (fi.getFluid().isCategory(FluidCategory.Beverage) && fi.getFluid().isCategory(FluidCategory.Alcoholic)) {
                        alcoholics++;
                    }
                }

                if (allBeverages) {
                    if (alcoholics >= 2) {
                        this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Cocktail"));
                    } else {
                        this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Mixed_Beverages"));
                    }
                } else {
                    this.nameCache = Translator.getText("Fluid_HoldingOneType", containerName, Translator.getFluidText("Fluid_Mixed_Fluids"));
                }
            }
        }

        return this.nameCache;
    }

    public SealedFluidProperties getProperties() {
        this.recalculateCaches();
        return this.propertiesCache;
    }

    public boolean isEmpty() {
        return this.getAmount() == 0.0F;
    }

    public boolean isFull() {
        return PZMath.equal(this.getAmount(), this.capacity, 1.0E-4F);
    }

    public float getCapacity() {
        return this.capacity;
    }

    public float getFreeCapacity() {
        float free = this.capacity - this.getAmount();
        return PZMath.equal(free, 0.0F, 1.0E-4F) ? 0.0F : free;
    }

    public float getFilledRatio() {
        if (this.isFull()) {
            return 1.0F;
        } else if (this.isEmpty()) {
            return 0.0F;
        } else {
            return this.capacity != 0.0F ? this.getAmount() / this.capacity : 0.0F;
        }
    }

    protected void invalidateColor() {
        this.cacheInvalidated = true;
    }

    public Color getColor() {
        this.recalculateCaches();
        return this.color;
    }

    public float getAmount() {
        this.recalculateCaches();
        return this.amountCache;
    }

    private void recalculateCaches() {
        this.recalculateCaches(false, false);
    }

    private void recalculateCaches(boolean force) {
        this.recalculateCaches(force, false);
    }

    private void recalculateCaches(boolean force, boolean removeInvalid) {
        if (this.cacheInvalidated || force) {
            this.amountCache = 0.0F;
            float r = 0.0F;
            float g = 0.0F;
            float b = 0.0F;

            for (int i = this.fluids.size() - 1; i >= 0; i--) {
                FluidInstance fluid = this.fluids.get(i);
                if (!removeInvalid || !this.removeFluidInstanceIfEmpty(fluid)) {
                    this.amountCache = this.amountCache + fluid.getAmount();
                }
            }

            this.propertiesCache.clear();
            if (this.amountCache > 0.0F) {
                for (int ix = 0; ix < this.fluids.size(); ix++) {
                    FluidInstance fluid = this.fluids.get(ix);
                    fluid.setPercentage(fluid.getAmount() / this.amountCache);
                    r += fluid.getColor().r * fluid.getPercentage();
                    g += fluid.getColor().g * fluid.getPercentage();
                    b += fluid.getColor().b * fluid.getPercentage();
                    if (fluid.getFluid().getProperties() != null) {
                        float multi = this.amountCache * fluid.getPercentage();
                        this.propertiesCache.addFromMultiplied(fluid.getFluid().getProperties(), multi);
                    }
                }
            }

            if (this.amountCache > 0.0F) {
                this.color.set(r, g, b);
            } else {
                this.color.set(colorDef);
            }

            if (this.getCapacity() - this.amountCache < 0.001F) {
                this.amountCache = this.getCapacity();
            }

            this.nameCache = null;
            this.cacheInvalidated = false;
            if (this.owner != null && this.owner instanceof InventoryItem item && item.isInPlayerInventory()) {
                ItemContainer itemContainer = item.getContainer();
                if (itemContainer != null) {
                    itemContainer.setDrawDirty(true);
                    itemContainer.setDirty(true);
                }
            }

            if (this.getGameEntity() != null) {
                this.getGameEntity().onFluidContainerUpdate();
            }
        }
    }

    private float getPoisonAmount() {
        float poisonAmount = 0.0F;

        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluid = this.fluids.get(i);
            if (fluid.getFluid().isPoisonous()) {
                poisonAmount += fluid.getAmount();
            }
        }

        return poisonAmount;
    }

    public float getPoisonRatio() {
        float poisonAmount = this.getPoisonAmount();
        if (poisonAmount > 0.0F) {
            float amount = this.getAmount();
            return amount > 0.0F && amount >= poisonAmount ? poisonAmount / amount : 1.0F;
        } else {
            return 0.0F;
        }
    }

    public boolean isPoisonous() {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (this.fluids.get(i).getFluid().isPoisonous()) {
                return true;
            }
        }

        return false;
    }

    public PoisonEffect getPoisonEffect() {
        PoisonEffect effect = PoisonEffect.None;

        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluid = this.fluids.get(i);
            if (fluid.getFluid().getPoisonInfo() != null) {
                PoisonInfo poison = fluid.getFluid().getPoisonInfo();
                PoisonEffect poisonEffect = poison.getPoisonEffect(this.getAmount() * fluid.getPercentage(), fluid.getPercentage());
                if (poisonEffect.getLevel() > effect.getLevel()) {
                    effect = poisonEffect;
                }
            }
        }

        return effect;
    }

    public boolean isTainted() {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (this.fluids.get(i).getFluid().getFluidType() == FluidType.TaintedWater) {
                return true;
            }
        }

        return false;
    }

    public void setCapacity(float capacity) {
        float oldCapacity = this.capacity;
        this.capacity = PZMath.max(capacity, 0.05F);
        if (this.getAmount() > this.capacity) {
            for (int i = this.fluids.size() - 1; i >= 0; i--) {
                FluidInstance fluid = this.fluids.get(i);
                float ratio = fluid.getAmount() / oldCapacity;
                fluid.setAmount(this.capacity * ratio);
                this.removeFluidInstanceIfEmpty(fluid);
            }
        }

        this.cacheInvalidated = true;
    }

    public void adjustAmount(float newAmount) {
        if (!this.isEmpty()) {
            newAmount = PZMath.clamp(newAmount, 0.0F, this.getCapacity());
            float ratio = newAmount / this.getAmount();

            for (int i = this.fluids.size() - 1; i >= 0; i--) {
                FluidInstance fluid = this.fluids.get(i);
                float fluidAmount = fluid.getAmount() * ratio;
                fluid.setAmount(fluidAmount);
                this.removeFluidInstanceIfEmpty(fluid);
            }

            this.cacheInvalidated = true;
        }
    }

    public void adjustSpecificFluidAmount(Fluid fluid, float newAmount) {
        if (!this.isEmpty()) {
            newAmount = PZMath.clamp(newAmount, 0.0F, this.getCapacity());

            for (int i = this.fluids.size() - 1; i >= 0; i--) {
                FluidInstance tempFluid = this.fluids.get(i);
                if (tempFluid.getFluid() == fluid) {
                    tempFluid.setAmount(newAmount);
                    this.removeFluidInstanceIfEmpty(tempFluid);
                }
            }

            this.cacheInvalidated = true;
        }
    }

    public float getSpecificFluidAmount(Fluid fluid) {
        if (this.isEmpty()) {
            return 0.0F;
        } else {
            FluidInstance test = null;

            for (int i = 0; i < this.fluids.size(); i++) {
                if (this.fluids.get(i).getFluid() == fluid) {
                    test = this.fluids.get(i);
                }
            }

            return test == null ? 0.0F : test.getAmount();
        }
    }

    public FluidSample createFluidSample() {
        return this.createFluidSample(this.getAmount());
    }

    public FluidSample createFluidSample(float scaleAmount) {
        FluidSample sample = FluidSample.Alloc();

        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluidInstance = this.fluids.get(i);
            sample.addFluid(fluidInstance);
        }

        if (scaleAmount != this.getAmount()) {
            sample.scaleToAmount(scaleAmount);
        }

        return sample.seal();
    }

    public FluidSample createFluidSample(FluidSample sample, float scaleAmount) {
        sample.clear();

        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fluidInstance = this.fluids.get(i);
            sample.addFluid(fluidInstance);
        }

        if (scaleAmount != this.getAmount()) {
            sample.scaleToAmount(scaleAmount);
        }

        return sample.seal();
    }

    public boolean isPureFluid(Fluid fluid) {
        return this.fluids.size() == 1 && this.fluids.get(0).getFluid() == fluid;
    }

    public Fluid getPrimaryFluid() {
        if (this.isEmpty()) {
            return null;
        } else if (this.fluids.size() == 1) {
            return this.fluids.get(0).getFluid();
        } else {
            FluidInstance primary = null;

            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance test = this.fluids.get(i);
                if (primary == null || test.getAmount() > primary.getAmount()) {
                    primary = test;
                }
            }

            return primary.getFluid();
        }
    }

    public float getPrimaryFluidAmount() {
        if (this.isEmpty()) {
            return 0.0F;
        } else if (this.fluids.size() == 1) {
            return this.fluids.get(0).getAmount();
        } else {
            FluidInstance primary = null;

            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance test = this.fluids.get(i);
                if (primary == null || test.getAmount() > primary.getAmount()) {
                    primary = test;
                }
            }

            return primary.getAmount();
        }
    }

    public boolean isPerceivedFluidToPlayer(Fluid fluid, IsoGameCharacter character) {
        return !this.isEmpty();
    }

    public boolean isMixture() {
        return this.fluids.size() > 1;
    }

    public FluidFilter getWhitelist() {
        return this.whitelist;
    }

    public FluidFilter getBlacklist() {
        return this.blacklist;
    }

    public void Empty() {
        this.Empty(true);
    }

    public void Empty(boolean bRecalculate) {
        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance.Release(this.fluids.get(i));
        }

        this.fluids.clear();
        this.propertiesCache.clear();
        this.amountCache = 0.0F;
        this.color.set(colorDef);
        if (bRecalculate) {
            this.recalculateCaches(true);
        }
    }

    private FluidInstance getFluidInstance(Fluid fluid) {
        if (this.fluids.isEmpty()) {
            return null;
        } else {
            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance fluidInstance = this.fluids.get(i);
                if (fluidInstance.getFluid() == fluid) {
                    return fluidInstance;
                }
            }

            return null;
        }
    }

    public boolean canAddFluid(Fluid fluid) {
        if ((this.fluids.size() < 8 || this.contains(fluid)) && !this.inputLocked && this.canPlayerEmpty) {
            for (int i = 0; i < this.fluids.size(); i++) {
                FluidInstance fluidInstance = this.fluids.get(i);
                if (!fluidInstance.getFluid().canBlendWith(fluid) || !fluid.canBlendWith(fluidInstance.getFluid())) {
                    return false;
                }
            }

            return (this.whitelist == null || this.whitelist.allows(fluid)) && (this.blacklist == null || this.blacklist.allows(fluid));
        } else {
            return false;
        }
    }

    public void addFluid(String fluidType, float amount) {
        this.addFluid(Fluid.Get(fluidType), amount);
    }

    public void addFluid(FluidType fluidType, float amount) {
        this.addFluid(Fluid.Get(fluidType), amount);
    }

    public void addFluid(Fluid fluid, float amount) {
        if (fluid != null) {
            FluidInstance fluidInstance = fluid.getInstance();
            this.addFluid(fluidInstance, amount);
            FluidInstance.Release(fluidInstance);
        }
    }

    private void addFluid(FluidInstance addInstance, float add) {
        add = PZMath.max(0.0F, add);
        if (add > 0.0F && !this.isFull() && this.canAddFluid(addInstance.getFluid())) {
            float available = this.capacity - this.getAmount();
            add = PZMath.min(add, available);
            FluidInstance fluidInstance = this.getFluidInstance(addInstance.getFluid());
            if (fluidInstance == null) {
                fluidInstance = addInstance.getFluid().getInstance();
                fluidInstance.setParent(this);
                fluidInstance.setColor(addInstance.getColor());
                this.fluids.add(fluidInstance);
            } else if (fluidInstance.getFluid().isCategory(FluidCategory.Colors) && !addInstance.getColor().equals(fluidInstance.getColor())) {
                fluidInstance.mixColor(addInstance.getColor(), add);
            }

            fluidInstance.setAmount(fluidInstance.getAmount() + add);
            this.cacheInvalidated = true;
        }
    }

    public void removeFluid() {
        this.removeFluid(this.getAmount(), false);
    }

    public FluidConsume removeFluid(boolean createFluidConsume) {
        return this.removeFluid(this.getAmount(), createFluidConsume);
    }

    public void removeFluid(float remove) {
        this.removeFluid(remove, false);
    }

    public FluidConsume removeFluid(float remove, boolean createFluidConsume) {
        return this.removeFluid(remove, createFluidConsume, null);
    }

    public FluidConsume removeFluid(float remove, boolean createFluidConsume, FluidConsume fluidConsume) {
        FluidConsume consume = fluidConsume;
        if (fluidConsume != null) {
            fluidConsume.clear();
        } else if (createFluidConsume) {
            consume = FluidConsume.Alloc();
        }

        remove = PZMath.max(0.0F, remove);
        if (remove > 0.0F && !this.isEmpty()) {
            remove = PZMath.min(remove, this.getAmount());
            if (consume != null) {
                consume.setAmount(remove);
            }

            for (int i = this.fluids.size() - 1; i >= 0; i--) {
                FluidInstance fluid = this.fluids.get(i);
                float fluidRemoveAmount = remove * fluid.getPercentage();
                if (consume != null) {
                    if (fluid.getFluid().getPoisonInfo() != null) {
                        PoisonInfo poison = fluid.getFluid().getPoisonInfo();
                        PoisonEffect effect = poison.getPoisonEffect(fluidRemoveAmount, fluid.getPercentage());
                        consume.setPoisonEffect(effect);
                    }

                    if (fluid.getFluid().getProperties() != null) {
                        SealedFluidProperties props = fluid.getFluid().getProperties();
                        consume.addFromMultiplied(props, fluidRemoveAmount);
                    }
                }

                fluid.setAmount(fluid.getAmount() - fluidRemoveAmount);
                this.removeFluidInstanceIfEmpty(fluid);
            }

            this.cacheInvalidated = true;
        }

        return consume;
    }

    private boolean removeFluidInstanceIfEmpty(FluidInstance fluid) {
        if (PZMath.equal(fluid.getAmount(), 0.0F, 1.0E-4F)) {
            this.fluids.remove(fluid);
            FluidInstance.Release(fluid);
            return true;
        } else {
            return false;
        }
    }

    public boolean contains(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (this.fluids.get(i).getFluid() == fluid) {
                return true;
            }
        }

        return false;
    }

    public float getRatioForFluid(Fluid fluid) {
        for (int i = 0; i < this.fluids.size(); i++) {
            if (this.fluids.get(i).getFluid() == fluid) {
                return this.fluids.get(i).getPercentage();
            }
        }

        return 0.0F;
    }

    public boolean isCategory(FluidCategory category) {
        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fi = this.fluids.get(i);
            if (fi.getFluid().isCategory(category)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAllCategory(FluidCategory category) {
        for (int i = 0; i < this.fluids.size(); i++) {
            FluidInstance fi = this.fluids.get(i);
            if (!fi.getFluid().isCategory(category)) {
                return false;
            }
        }

        return true;
    }

    public void transferTo(FluidContainer other) {
        this.transferTo(other, this.getAmount());
    }

    public void transferTo(FluidContainer other, float amount) {
        Transfer(this, other, amount);
    }

    public void transferFrom(FluidContainer other) {
        this.transferFrom(other, other.getAmount());
    }

    public void transferFrom(FluidContainer other, float amount) {
        Transfer(other, this, amount);
    }

    public static String GetTransferReason(FluidContainer source, FluidContainer target) {
        return GetTransferReason(source, target, false);
    }

    public static String GetTransferReason(FluidContainer source, FluidContainer target, boolean testFirst) {
        if (testFirst && CanTransfer(source, target)) {
            return Translator.getFluidText("Fluid_Reason_Allowed");
        } else if (source == null) {
            return Translator.getFluidText("Fluid_Reason_Source_Null");
        } else if (target == null) {
            return Translator.getFluidText("Fluid_Reason_Target_Null");
        } else if (source == target) {
            return Translator.getFluidText("Fluid_Reason_Equal");
        } else if (source.isEmpty()) {
            return Translator.getFluidText("Fluid_Reason_Source_Empty");
        } else if (target.isFull()) {
            return Translator.getFluidText("Fluid_Reason_Target_Full");
        } else if (target.isInputLocked()) {
            return Translator.getFluidText("Fluid_Reason_Target_Locked");
        } else {
            if (target.whitelist != null || target.blacklist != null) {
                boolean allows = true;

                for (int i = 0; i < source.fluids.size(); i++) {
                    FluidInstance fluid = source.fluids.get(i);
                    if (target.whitelist != null && !target.whitelist.allows(fluid.getFluid())) {
                        allows = false;
                        break;
                    }

                    if (target.blacklist != null && !target.blacklist.allows(fluid.getFluid())) {
                        allows = false;
                        break;
                    }
                }

                if (!allows) {
                    return Translator.getFluidText("Fluid_Reason_Target_Filter");
                }
            }

            return Translator.getFluidText("Fluid_Reason_Mixing_Locked");
        }
    }

    public static boolean CanTransfer(FluidContainer source, FluidContainer target) {
        if (source == null || target == null) {
            return false;
        } else if (source == target) {
            return false;
        } else if (!source.isEmpty() && !target.isFull() && !target.inputLocked) {
            int equalFluids = 0;

            for (int i = 0; i < source.fluids.size(); i++) {
                FluidInstance fluid = source.fluids.get(i);
                if (!target.canAddFluid(fluid.getFluid())) {
                    return false;
                }

                if (target.contains(fluid.getFluid())) {
                    equalFluids++;
                }
            }

            int totalFluids = equalFluids + source.fluids.size() - equalFluids + target.fluids.size() - equalFluids;
            return totalFluids <= 8;
        } else {
            return false;
        }
    }

    public static void Transfer(FluidContainer source, FluidContainer target) {
        Transfer(source, target, source.getAmount());
    }

    public static void Transfer(FluidContainer source, FluidContainer target, float amount) {
        Transfer(source, target, amount, false);
    }

    public static void Transfer(FluidContainer source, FluidContainer target, float amount, boolean keepSource) {
        if (CanTransfer(source, target)) {
            float transfer = PZMath.min(amount, source.getAmount());
            float available = target.getCapacity() - target.getAmount();
            transfer = PZMath.min(transfer, available);

            for (int i = source.fluids.size() - 1; i >= 0; i--) {
                FluidInstance fluid = source.fluids.get(i);
                float fluidTransfer = transfer * fluid.getPercentage();
                target.addFluid(fluid, fluidTransfer);
                if (!keepSource) {
                    fluid.setAmount(fluid.getAmount() - fluidTransfer);
                    source.removeFluidInstanceIfEmpty(fluid);
                }
            }

            if (!keepSource) {
                source.recalculateCaches(true);
            }

            target.recalculateCaches(true);
        }
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        switch (type) {
            default:
                return false;
        }
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 240);
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
        if (this.capacity != 1.0F) {
            header.addFlags(1);
            output.putFloat(this.capacity);
        }

        if (!this.fluids.isEmpty()) {
            header.addFlags(2);
            if (this.fluids.size() == 1) {
                header.addFlags(4);
                FluidInstance.save(this.fluids.get(0), output);
            } else {
                output.put((byte)this.fluids.size());

                for (int i = 0; i < this.fluids.size(); i++) {
                    FluidInstance fi = this.fluids.get(i);
                    FluidInstance.save(fi, output);
                }
            }
        }

        if (this.whitelist != null) {
            header.addFlags(8);
            this.whitelist.save(output);
        }

        if (this.blacklist != null) {
            header.addFlags(16);
            this.blacklist.save(output);
        }

        if (this.inputLocked) {
            header.addFlags(32);
        }

        if (this.canPlayerEmpty) {
            header.addFlags(64);
        }

        if (!this.containerName.equals("FluidContainer")) {
            header.addFlags(128);
            GameWindow.WriteString(output, this.containerName);
        }

        if (this.hiddenAmount) {
            header.addFlags(256);
        }

        if (this.rainCatcher > 0.0F) {
            header.addFlags(512);
            output.putFloat(this.rainCatcher);
        }

        header.write();
        header.release();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.amountCache = 0.0F;
        this.capacity = 1.0F;
        this.whitelist = null;
        this.blacklist = null;
        this.inputLocked = false;
        this.canPlayerEmpty = false;
        this.hiddenAmount = false;
        this.Empty(false);
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
        if (header.hasFlags(1)) {
            this.capacity = input.getFloat();
        }

        if (header.hasFlags(2)) {
            if (header.hasFlags(4)) {
                FluidInstance fi = FluidInstance.load(input, WorldVersion);
                if (fi.getFluid() != null) {
                    fi.setParent(this);
                    this.fluids.add(fi);
                }
            } else {
                int count = input.get();

                for (int i = 0; i < count; i++) {
                    FluidInstance fi = FluidInstance.load(input, WorldVersion);
                    if (fi.getFluid() != null) {
                        fi.setParent(this);
                        this.fluids.add(fi);
                    }
                }
            }
        }

        if (header.hasFlags(8)) {
            this.whitelist = new FluidFilter();
            this.whitelist.load(input, WorldVersion);
        }

        if (header.hasFlags(16)) {
            this.blacklist = new FluidFilter();
            this.blacklist.load(input, WorldVersion);
        }

        this.inputLocked = header.hasFlags(32);
        this.canPlayerEmpty = header.hasFlags(64);
        if (header.hasFlags(128)) {
            this.containerName = GameWindow.ReadString(input);
        } else {
            this.containerName = "FluidContainer";
        }

        this.hiddenAmount = header.hasFlags(256);
        if (header.hasFlags(512)) {
            this.rainCatcher = input.getFloat();
        }

        header.release();
        this.recalculateCaches(true);
    }

    public void unsealIfNotFull() {
        if (this.getFreeCapacity() > 0.0F) {
            this.unseal();
        }
    }

    public void unseal() {
        InventoryItem item = null;
        if (this.getOwner() instanceof InventoryItem) {
            item = (InventoryItem)this.getOwner();
        } else if (this.getOwner() instanceof IsoWorldInventoryObject) {
            item = ((IsoWorldInventoryObject)this.getOwner()).getItem();
        }

        this.setCanPlayerEmpty(true);
        FluidContainer copy = this.copy();
        this.copyFluidsFrom(copy);

        assert item != null;

        item.sendSyncEntity(null);
        DisposeContainer(copy);
    }

    @Override
    public boolean isQualifiesForMetaStorage() {
        return this.getRainCatcher() > 0.0F;
    }

    public void setWhitelist(FluidFilter ff) {
        this.whitelist = ff;
    }

    public boolean isTaintedStatusKnown() {
        if (this.getPrimaryFluid() == Fluid.Bleach) {
            return true;
        } else {
            return !SandboxOptions.instance.enableTaintedWaterText.getValue()
                ? false
                : (this.contains(Fluid.TaintedWater) || this.contains(Fluid.Bleach)) && this.getPoisonEffect() != PoisonEffect.None;
        }
    }

    public void setNonSavedFieldsFromItemScript(InventoryItem item) {
        Item itemScript = item.getScriptItem();
        FluidContainerScript componentScript = itemScript.getComponentScriptFor(ComponentType.FluidContainer);
        if (componentScript != null) {
            this.customDrinkSound = componentScript.getCustomDrinkSound();
            this.fillsWithCleanWater = componentScript.isFilledWithCleanWater();
        }
    }
}
