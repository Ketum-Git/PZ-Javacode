// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClass;
import zombie.debug.objects.DebugField;
import zombie.debug.objects.DebugMethod;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.FluidDefinitionScript;
import zombie.scripting.objects.Item;

@DebugClass
@UsedFromLua
public class Fluid {
    private static boolean hasInitialized;
    private static final HashMap<FluidType, Fluid> fluidEnumMap = new HashMap<>();
    private static final HashMap<String, Fluid> fluidStringMap = new HashMap<>();
    private static final HashMap<String, Fluid> cacheStringMap = new HashMap<>();
    private static final HashMap<FluidDefinitionScript, Fluid> scriptToFluidMap = new HashMap<>();
    private static final ArrayList<Fluid> allFluids = new ArrayList<>();
    public static final Fluid Water = addFluid(FluidType.Water);
    public static final Fluid TaintedWater = addFluid(FluidType.TaintedWater);
    public static final Fluid Petrol = addFluid(FluidType.Petrol);
    public static final Fluid Alcohol = addFluid(FluidType.Alcohol);
    public static final Fluid PoisonPotent = addFluid(FluidType.PoisonPotent);
    public static final Fluid Beer = addFluid(FluidType.Beer);
    public static final Fluid Whiskey = addFluid(FluidType.Whiskey);
    public static final Fluid SodaPop = addFluid(FluidType.SodaPop);
    public static final Fluid Coffee = addFluid(FluidType.Coffee);
    public static final Fluid Tea = addFluid(FluidType.Tea);
    public static final Fluid Wine = addFluid(FluidType.Wine);
    public static final Fluid Bleach = addFluid(FluidType.Bleach);
    public static final Fluid Blood = addFluid(FluidType.Blood);
    public static final Fluid Honey = addFluid(FluidType.Honey);
    public static final Fluid Mead = addFluid(FluidType.Mead);
    public static final Fluid Acid = addFluid(FluidType.Acid);
    public static final Fluid SpiffoJuice = addFluid(FluidType.SpiffoJuice);
    public static final Fluid SecretFlavoring = addFluid(FluidType.SecretFlavoring);
    public static final Fluid CarbonatedWater = addFluid(FluidType.CarbonatedWater);
    public static final Fluid CleaningLiquid = addFluid(FluidType.CleaningLiquid);
    public static final Fluid CowMilk = addFluid(FluidType.CowMilk);
    public static final Fluid SheepMilk = addFluid(FluidType.SheepMilk);
    public static final Fluid AnimalBlood = addFluid(FluidType.AnimalBlood);
    public static final Fluid AnimalGrease = addFluid(FluidType.AnimalGrease);
    public static final Fluid Dye = addFluid(FluidType.Dye);
    public static final Fluid HairDye = addFluid(FluidType.HairDye);
    public static final Fluid AnimalMilk = addFluid(FluidType.AnimalMilk);
    @DebugNonRecursive
    private FluidDefinitionScript script;
    @DebugField
    private final FluidType fluidType;
    @DebugField
    private final String fluidTypeStr;
    @DebugField
    private final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    @DebugField
    private ImmutableSet<FluidCategory> categories;
    private String categoriesCacheStr;
    @DebugField
    private FluidFilter blendWhitelist;
    @DebugField
    private FluidFilter blendBlacklist;
    @DebugField
    private PoisonInfo poisonInfo;
    @DebugField
    private SealedFluidProperties properties;

    private static Fluid addFluid(FluidType type) {
        if (fluidEnumMap.containsKey(type)) {
            throw new RuntimeException("Fluid defined twice: " + type);
        } else {
            Fluid fluid = new Fluid(type);
            fluidEnumMap.put(type, fluid);
            return fluid;
        }
    }

    public static Fluid Get(FluidType type) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        } else {
            return fluidEnumMap.get(type);
        }
    }

    public static Fluid Get(String name) {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        } else {
            return fluidStringMap.get(name);
        }
    }

    public static ArrayList<Fluid> getAllFluids() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        } else {
            return allFluids;
        }
    }

    public static ArrayList<Item> getAllFluidItemsDebug() {
        if (Core.debug && !hasInitialized) {
            throw new RuntimeException("Fluids have not yet been initialized!");
        } else {
            ArrayList<Item> items = ScriptManager.instance.getAllItems();
            ArrayList<Item> fluidItems = new ArrayList<>();

            for (Item item : items) {
                if (item.containsComponent(ComponentType.FluidContainer)) {
                    fluidItems.add(item);
                }
            }

            return fluidItems;
        }
    }

    public static boolean FluidsInitialized() {
        return hasInitialized;
    }

    public static void Init(ScriptLoadMode loadMode) throws Exception {
        DebugLog.Fluid.println("*************************************");
        DebugLog.Fluid.println("* Fluid: initialize Fluids.         *");
        DebugLog.Fluid.println("*************************************");
        ArrayList<FluidDefinitionScript> scripts = ScriptManager.instance.getAllFluidDefinitionScripts();
        cacheStringMap.clear();
        scriptToFluidMap.clear();
        allFluids.clear();
        if (loadMode == ScriptLoadMode.Reload) {
            for (Entry<String, Fluid> entry : fluidStringMap.entrySet()) {
                cacheStringMap.put(entry.getKey(), entry.getValue());
            }

            fluidStringMap.clear();
        }

        for (FluidDefinitionScript script : scripts) {
            if (script.getFluidType() == FluidType.Modded) {
                DebugLog.Fluid.println(script.getModID() + " = " + script.getFluidTypeString());
                Fluid fluid = cacheStringMap.get(script.getFluidTypeString());
                if (fluid == null) {
                    fluid = new Fluid(script.getFluidTypeString());
                }

                fluid.setScript(script);
                fluidStringMap.put(script.getFluidTypeString(), fluid);
                scriptToFluidMap.put(script, fluid);
                allFluids.add(fluid);
            } else {
                DebugLog.Fluid.println(script.getModID() + " = " + script.getFluidType());
                Fluid fluid = fluidEnumMap.get(script.getFluidType());
                if (fluid == null) {
                    if (Core.debug) {
                        throw new Exception("Fluid not found: " + script.getFluidType());
                    }
                } else {
                    fluid.setScript(script);
                    scriptToFluidMap.put(script, fluid);
                    allFluids.add(fluid);
                }
            }
        }

        for (Entry<FluidType, Fluid> entry : fluidEnumMap.entrySet()) {
            if (Core.debug && entry.getValue().script == null) {
                throw new Exception("Fluid has no script set: " + entry.getKey());
            }

            fluidStringMap.put(entry.getKey().toString(), entry.getValue());
        }

        cacheStringMap.clear();
        hasInitialized = true;

        for (FluidDefinitionScript scriptx : scripts) {
            Fluid fluid = scriptToFluidMap.get(scriptx);
            if (fluid != null) {
                if (scriptx.getBlendWhitelist() != null) {
                    fluid.blendWhitelist = scriptx.getBlendWhitelist().createFilter();
                    fluid.blendWhitelist.seal();
                    DebugLog.Fluid.debugln("[Created fluid blend whitelist: " + fluid.getFluidTypeString() + "]");
                    DebugLog.Fluid.debugln(fluid.blendWhitelist);
                }

                if (scriptx.getBlendBlackList() != null) {
                    fluid.blendBlacklist = scriptx.getBlendBlackList().createFilter();
                    fluid.blendBlacklist.seal();
                    DebugLog.Fluid.debugln("[Created fluid blend blacklist: " + fluid.getFluidTypeString() + "]");
                    DebugLog.Fluid.debugln(fluid.blendBlacklist);
                }
            }
        }

        DebugLog.Fluid.println("*************************************");
    }

    public static void PreReloadScripts() {
        hasInitialized = false;
    }

    public static void Reset() {
        fluidStringMap.clear();
        scriptToFluidMap.clear();
        hasInitialized = false;
    }

    public static void saveFluid(Fluid fluid, ByteBuffer output) {
        output.put((byte)(fluid != null ? 1 : 0));
        if (fluid != null) {
            if (fluid.fluidType == FluidType.Modded) {
                output.put((byte)1);
                GameWindow.WriteString(output, fluid.fluidTypeStr);
            } else {
                output.put((byte)0);
                output.put(fluid.fluidType.getId());
            }
        }
    }

    public static Fluid loadFluid(ByteBuffer input, int WorldVersion) {
        if (input.get() == 0) {
            return null;
        } else {
            Fluid fluid;
            if (input.get() == 1) {
                String fluidTypeString = GameWindow.ReadString(input);
                fluid = Get(fluidTypeString);
            } else {
                FluidType fluidType = FluidType.FromId(input.get());
                fluid = Get(fluidType);
            }

            return fluid;
        }
    }

    private Fluid(FluidType fluidType) {
        this.fluidType = Objects.requireNonNull(fluidType);
        this.fluidTypeStr = fluidType.toString();
    }

    private Fluid(String fluidTypeStr) {
        this.fluidType = FluidType.Modded;
        this.fluidTypeStr = Objects.requireNonNull(fluidTypeStr);
    }

    private void setScript(FluidDefinitionScript script) {
        this.script = Objects.requireNonNull(script);
        this.color.set(script.getColor());
        this.categories = Sets.immutableEnumSet(script.getCategories());
        this.categoriesCacheStr = this.categories.toString();
        if (script.hasPropertiesSet()) {
            FluidProperties props = new FluidProperties();
            props.setEffects(
                script.getFatigueChange(),
                script.getHungerChange(),
                script.getStressChange(),
                script.getThirstChange(),
                script.getUnhappyChange(),
                script.getAlcohol(),
                script.getPoisonMaxEffect().getPlayerEffect()
            );
            props.setNutrients(script.getCalories(), script.getCarbohydrates(), script.getLipids(), script.getProteins());
            props.setAlcohol(script.getAlcohol());
            props.setReductions(script.getFluReduction(), script.getPainReduction(), script.getEnduranceChange(), script.getFoodSicknessChange());
            this.properties = props.getSealedFluidProperties();
        } else {
            this.properties = null;
        }

        if (script.getPoisonMaxEffect() != PoisonEffect.None) {
            this.poisonInfo = new PoisonInfo(this, script.getPoisonMinAmount(), script.getPoisonDiluteRatio(), script.getPoisonMaxEffect());
        } else {
            this.poisonInfo = null;
        }
    }

    @DebugMethod
    public boolean isVanilla() {
        return this.script != null && this.script.isVanilla();
    }

    @Override
    public String toString() {
        return this.fluidTypeStr;
    }

    public FluidInstance getInstance() {
        return FluidInstance.Alloc(this);
    }

    public FluidType getFluidType() {
        return this.fluidType;
    }

    public String getFluidTypeString() {
        return this.fluidTypeStr;
    }

    public Color getColor() {
        return this.color;
    }

    public ImmutableSet<FluidCategory> getCategories() {
        return this.categories;
    }

    public boolean isCategory(FluidCategory category) {
        return this.categories.contains(category);
    }

    public String getDisplayName() {
        return this.script != null ? this.script.getDisplayName() : "<unknown_fluid>";
    }

    public String getTranslatedName() {
        return this.getDisplayName();
    }

    public String getTranslatedNameLower() {
        return this.getDisplayName().toLowerCase();
    }

    public boolean canBlendWith(Fluid fluid) {
        return fluid == this
            ? true
            : (this.blendWhitelist == null || this.blendWhitelist.allows(fluid)) && (this.blendBlacklist == null || this.blendBlacklist.allows(fluid));
    }

    public SealedFluidProperties getProperties() {
        return this.properties;
    }

    public PoisonInfo getPoisonInfo() {
        return this.poisonInfo;
    }

    public boolean isPoisonous() {
        return this.poisonInfo != null;
    }

    public FluidDefinitionScript getScript() {
        return this.script;
    }
}
