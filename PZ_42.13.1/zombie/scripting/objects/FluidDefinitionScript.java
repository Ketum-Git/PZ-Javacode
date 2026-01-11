// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugIgnoreField;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidType;
import zombie.entity.components.fluids.PoisonEffect;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@DebugClassFields
@UsedFromLua
public class FluidDefinitionScript extends BaseScriptObject {
    @DebugIgnoreField
    private final HashMap<String, FluidDefinitionScript.PropertyValue> propertiesMap = new HashMap<>();
    @DebugIgnoreField
    private final ArrayList<FluidDefinitionScript.PropertyValue> properties = new ArrayList<>();
    private boolean existsAsVanilla;
    private String modId;
    private FluidType fluidType = FluidType.None;
    private String fluidTypeString;
    private String colorReference;
    private final Color color = new Color(0.0F, 0.0F, 1.0F, 1.0F);
    private final EnumSet<FluidCategory> categories = EnumSet.noneOf(FluidCategory.class);
    private String displayName = "Fluid";
    private FluidFilterScript blendWhitelist;
    private FluidFilterScript blendBlacklist;
    private boolean hasPoison;
    private PoisonEffect poisonMaxEffect = PoisonEffect.None;
    private float poisonMinAmount = 1.0F;
    private float poisonDiluteRatio;
    private final FluidDefinitionScript.PropertyValue fatigueChange = this.addProperty("fatigueChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue hungerChange = this.addProperty("hungerChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue stressChange = this.addProperty("stressChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue thirstChange = this.addProperty("thirstChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue unhappyChange = this.addProperty("unhappyChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue calories = this.addProperty("calories", 0.0F);
    private final FluidDefinitionScript.PropertyValue carbohydrates = this.addProperty("carbohydrates", 0.0F);
    private final FluidDefinitionScript.PropertyValue lipids = this.addProperty("lipids", 0.0F);
    private final FluidDefinitionScript.PropertyValue proteins = this.addProperty("proteins", 0.0F);
    private final FluidDefinitionScript.PropertyValue alcohol = this.addProperty("alcohol", 0.0F);
    private final FluidDefinitionScript.PropertyValue fluReduction = this.addProperty("fluReduction", 0.0F);
    private final FluidDefinitionScript.PropertyValue painReduction = this.addProperty("painReduction", 0.0F);
    private final FluidDefinitionScript.PropertyValue enduranceChange = this.addProperty("enduranceChange", 0.0F);
    private final FluidDefinitionScript.PropertyValue foodSicknessChange = this.addProperty("foodSicknessChange", 0.0F);

    private FluidDefinitionScript.PropertyValue addProperty(String name, float defaultValue) {
        if (this.propertiesMap.containsKey(name)) {
            throw new RuntimeException("Name defined twice");
        } else {
            FluidDefinitionScript.PropertyValue prop = new FluidDefinitionScript.PropertyValue(name, defaultValue);
            this.propertiesMap.put(name, prop);
            this.properties.add(prop);
            return prop;
        }
    }

    protected FluidDefinitionScript() {
        super(ScriptType.FluidDefinition);
    }

    public boolean getExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public boolean isVanilla() {
        return this.modId != null && this.modId.equals("pz-vanilla");
    }

    public String getModID() {
        return this.modId;
    }

    public FluidType getFluidType() {
        return this.fluidType;
    }

    public String getFluidTypeString() {
        return this.fluidTypeString;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Color getColor() {
        if (this.colorReference != null) {
            Color c = Colors.GetColorByName(this.colorReference);
            if (c == null) {
                throw new RuntimeException("Cannot find color: " + this.colorReference);
            }

            this.color.set(c);
        }

        return this.color;
    }

    public EnumSet<FluidCategory> getCategories() {
        return this.categories;
    }

    public FluidFilterScript getBlendWhitelist() {
        return this.blendWhitelist;
    }

    public FluidFilterScript getBlendBlackList() {
        return this.blendBlacklist;
    }

    public PoisonEffect getPoisonMaxEffect() {
        return this.poisonMaxEffect;
    }

    public float getPoisonMinAmount() {
        return this.poisonMinAmount;
    }

    public float getPoisonDiluteRatio() {
        return this.poisonDiluteRatio;
    }

    public float getFatigueChange() {
        return this.fatigueChange.get() / 100.0F;
    }

    public float getHungerChange() {
        return this.hungerChange.get() / 100.0F;
    }

    public float getStressChange() {
        return this.stressChange.get() / 100.0F;
    }

    public float getThirstChange() {
        return this.thirstChange.get() / 100.0F;
    }

    public float getUnhappyChange() {
        return this.unhappyChange.get();
    }

    public float getCalories() {
        return this.calories.get();
    }

    public float getCarbohydrates() {
        return this.carbohydrates.get();
    }

    public float getLipids() {
        return this.lipids.get();
    }

    public float getProteins() {
        return this.proteins.get();
    }

    public float getAlcohol() {
        return this.alcohol.get();
    }

    public float getFluReduction() {
        return this.fluReduction.get();
    }

    public float getPainReduction() {
        return this.painReduction.get();
    }

    public float getEnduranceChange() {
        return this.enduranceChange.get();
    }

    public int getFoodSicknessChange() {
        return (int)this.foodSicknessChange.get();
    }

    public boolean hasPropertiesSet() {
        for (int i = 0; i < this.properties.size(); i++) {
            if (this.properties.get(i).isSet()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        this.modId = ScriptManager.getCurrentLoadFileMod();
        if (this.modId.equals("pz-vanilla")) {
            this.existsAsVanilla = true;
        }
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        if (FluidType.containsNameLowercase(name)) {
            this.fluidType = FluidType.FromNameLower(name);
        } else {
            this.fluidType = FluidType.Modded;
            this.fluidTypeString = name;
        }

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("displayName")) {
                    this.displayName = Translator.getFluidText(val);
                } else if (key.equalsIgnoreCase("colorReference")) {
                    this.colorReference = val;
                } else if ("r".equalsIgnoreCase(key)) {
                    this.color.r = Float.parseFloat(val);
                } else if ("g".equalsIgnoreCase(key)) {
                    this.color.g = Float.parseFloat(val);
                } else if ("b".equalsIgnoreCase(key)) {
                    this.color.b = Float.parseFloat(val);
                } else if (key.equalsIgnoreCase("color")) {
                    String[] opt = val.split(":");
                    if (opt.length == 3) {
                        this.color.r = Float.parseFloat(opt[0]);
                        this.color.g = Float.parseFloat(opt[1]);
                        this.color.b = Float.parseFloat(opt[2]);
                    }
                } else {
                    DebugLog.General.error("Unknown key '" + key + "' val(" + val + ") in fluid definition: " + this.getScriptObjectFullType());
                    if (Core.debug) {
                        throw new Exception("FluidDefinition error.");
                    }
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("blendWhiteList".equalsIgnoreCase(child.type)) {
                this.blendWhitelist = FluidFilterScript.GetAnonymous(true);
                this.blendWhitelist.LoadAnonymousFromBlock(child);
            } else if ("blendBlackList".equalsIgnoreCase(child.type)) {
                this.blendBlacklist = FluidFilterScript.GetAnonymous(false);
                this.blendBlacklist.LoadAnonymousFromBlock(child);
            } else if ("categories".equalsIgnoreCase(child.type)) {
                this.LoadCategories(child);
            } else if ("properties".equalsIgnoreCase(child.type)) {
                this.LoadProperties(child);
            } else if ("poison".equalsIgnoreCase(child.type)) {
                this.LoadPoison(child);
            } else {
                DebugLog.General.error("Unknown block '" + block.type + "' val(" + block.id + ") in fluid definition: " + this.getScriptObjectFullType());
                if (Core.debug) {
                    throw new Exception("FluidDefinition error.");
                }
            }
        }
    }

    private void LoadCategories(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            if (!StringUtils.isNullOrWhitespace(value.string)) {
                FluidCategory category = FluidCategory.valueOf(value.string.trim());
                this.categories.add(category);
            }
        }
    }

    private void LoadPoison(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                this.hasPoison = true;
                if ("maxEffect".equalsIgnoreCase(key)) {
                    this.poisonMaxEffect = PoisonEffect.valueOf(val);
                } else if ("minAmount".equalsIgnoreCase(key)) {
                    this.poisonMinAmount = Float.parseFloat(val);
                } else if ("diluteRatio".equalsIgnoreCase(key)) {
                    this.poisonDiluteRatio = Float.parseFloat(val);
                } else {
                    DebugLog.General.error("Unknown key '" + key + "' val(" + val + ") in fluid poison definition: " + this.getScriptObjectFullType());
                    if (Core.debug) {
                        throw new Exception("FluidDefinition error.");
                    }
                }
            }
        }
    }

    private void LoadProperties(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if ("fatigueChange".equalsIgnoreCase(key)) {
                    this.fatigueChange.set(Float.parseFloat(val));
                } else if ("hungerChange".equalsIgnoreCase(key)) {
                    this.hungerChange.set(Float.parseFloat(val));
                } else if ("stressChange".equalsIgnoreCase(key)) {
                    this.stressChange.set(Float.parseFloat(val));
                } else if ("thirstChange".equalsIgnoreCase(key)) {
                    this.thirstChange.set(Float.parseFloat(val));
                } else if ("unhappyChange".equalsIgnoreCase(key)) {
                    this.unhappyChange.set(Float.parseFloat(val));
                } else if ("calories".equalsIgnoreCase(key)) {
                    this.calories.set(Float.parseFloat(val));
                } else if ("carbohydrates".equalsIgnoreCase(key)) {
                    this.carbohydrates.set(Float.parseFloat(val));
                } else if ("lipids".equalsIgnoreCase(key)) {
                    this.lipids.set(Float.parseFloat(val));
                } else if ("proteins".equalsIgnoreCase(key)) {
                    this.proteins.set(Float.parseFloat(val));
                } else if ("alcohol".equalsIgnoreCase(key)) {
                    this.alcohol.set(Float.parseFloat(val));
                } else if ("fluReduction".equalsIgnoreCase(key)) {
                    this.fluReduction.set(Float.parseFloat(val));
                } else if ("painReduction".equalsIgnoreCase(key)) {
                    this.painReduction.set(Float.parseFloat(val));
                } else if ("enduranceChange".equalsIgnoreCase(key)) {
                    this.enduranceChange.set(Float.parseFloat(val));
                } else if ("foodSicknessChange".equalsIgnoreCase(key)) {
                    this.foodSicknessChange.set(Float.parseFloat(val));
                } else {
                    DebugLog.General.error("Unknown key '" + key + "' val(" + val + ") in fluid properties definition: " + this.getScriptObjectFullType());
                    if (Core.debug) {
                        throw new Exception("FluidDefinition error.");
                    }
                }
            }
        }
    }

    @Override
    public void PreReload() {
        this.existsAsVanilla = false;
        this.modId = null;
        this.fluidType = FluidType.None;
        this.fluidTypeString = null;
        this.colorReference = null;
        this.color.set(0.0F, 0.0F, 1.0F, 1.0F);
        this.categories.clear();
        this.displayName = "Fluid";
        this.blendWhitelist = null;
        this.blendBlacklist = null;
        this.hasPoison = false;
        this.poisonMaxEffect = PoisonEffect.None;
        this.poisonMinAmount = 1.0F;
        this.poisonDiluteRatio = 0.0F;
        this.fatigueChange.reset();
        this.hungerChange.reset();
        this.stressChange.reset();
        this.thirstChange.reset();
        this.unhappyChange.reset();
        this.calories.reset();
        this.carbohydrates.reset();
        this.lipids.reset();
        this.proteins.reset();
        this.alcohol.reset();
        this.fluReduction.reset();
        this.painReduction.reset();
        this.enduranceChange.reset();
        this.foodSicknessChange.reset();
    }

    @Override
    public void reset() {
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        if (this.fluidType == FluidType.None && this.fluidTypeString == null) {
            throw new Exception("No fluid type set.");
        } else if (this.hasPoison && this.poisonMaxEffect == PoisonEffect.None) {
            throw new Exception("Poison block defined but poison effect is 'None'");
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    private static class PropertyValue {
        private final String name;
        private final float defaultValue;
        private float value;

        public PropertyValue(String name, float defaultValue) {
            this.name = Objects.requireNonNull(name);
            this.defaultValue = defaultValue;
        }

        public boolean matchesKey(String name) {
            return name != null ? this.name.equalsIgnoreCase(name) : false;
        }

        public void set(float value) {
            this.value = value;
        }

        public float get() {
            return this.value;
        }

        public boolean isSet() {
            return this.value != this.defaultValue;
        }

        public void reset() {
            this.value = this.defaultValue;
        }

        @Override
        public String toString() {
            return String.valueOf(this.defaultValue);
        }
    }
}
