// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.utils.Bits;
import zombie.entity.components.attributes.AttributeContainer;
import zombie.entity.components.combat.Durability;
import zombie.entity.components.combat.DurabilityScript;
import zombie.entity.components.contextmenuconfig.ContextMenuConfig;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.components.crafting.CraftRecipeComponent;
import zombie.entity.components.crafting.DryingCraftLogic;
import zombie.entity.components.crafting.DryingLogic;
import zombie.entity.components.crafting.FurnaceLogic;
import zombie.entity.components.crafting.MashingLogic;
import zombie.entity.components.crafting.WallCoveringConfig;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.lua.LuaComponent;
import zombie.entity.components.parts.Parts;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.signals.Signals;
import zombie.entity.components.sounds.CraftBenchSounds;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.spriteconfig.SpriteOverlayConfig;
import zombie.entity.components.test.TestComponent;
import zombie.entity.components.ui.UiConfig;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.util.BitSet;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftLogicScript;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.DryingCraftLogicScript;
import zombie.scripting.entity.components.crafting.DryingLogicScript;
import zombie.scripting.entity.components.crafting.FurnaceLogicScript;
import zombie.scripting.entity.components.crafting.MashingLogicScript;
import zombie.scripting.entity.components.crafting.WallCoveringConfigScript;
import zombie.scripting.entity.components.fluids.FluidContainerScript;
import zombie.scripting.entity.components.lua.LuaComponentScript;
import zombie.scripting.entity.components.parts.PartsScript;
import zombie.scripting.entity.components.resources.ResourcesScript;
import zombie.scripting.entity.components.signals.SignalsScript;
import zombie.scripting.entity.components.sound.CraftBenchSoundsScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.spriteconfig.SpriteOverlayConfigScript;
import zombie.scripting.entity.components.test.TestComponentScript;
import zombie.scripting.entity.components.ui.UiConfigScript;

@UsedFromLua
public enum ComponentType {
    Attributes((short)1, AttributeContainer.class, AttributesScript.class, 0),
    FluidContainer((short)2, FluidContainer.class, FluidContainerScript.class, 2, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    SpriteConfig((short)3, SpriteConfig.class, SpriteConfigScript.class, 0, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    Lua((short)6, LuaComponent.class, LuaComponentScript.class, 0),
    Parts((short)7, Parts.class, PartsScript.class, 0),
    Signals((short)8, Signals.class, SignalsScript.class, 0),
    Script((short)9, EntityScriptInfo.class, null, 0),
    UiConfig((short)11, UiConfig.class, UiConfigScript.class, 0),
    CraftLogic((short)12, CraftLogic.class, CraftLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    FurnaceLogic((short)13, FurnaceLogic.class, FurnaceLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    TestComponent((short)14, TestComponent.class, TestComponentScript.class, 0),
    MashingLogic((short)15, MashingLogic.class, MashingLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    DryingLogic((short)16, DryingLogic.class, DryingLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    MetaTag((short)17, MetaTagComponent.class, null, 0, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    Resources((short)18, Resources.class, ResourcesScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    CraftBench((short)19, CraftBench.class, CraftBenchScript.class, 0),
    CraftRecipe((short)20, CraftRecipeComponent.class, CraftRecipeComponentScript.class, 0),
    Durability((short)21, Durability.class, DurabilityScript.class, 0),
    DryingCraftLogic((short)22, DryingCraftLogic.class, DryingCraftLogicScript.class, 3, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)),
    ContextMenuConfig((short)23, ContextMenuConfig.class, ContextMenuConfigScript.class, 0),
    SpriteOverlayConfig(
        (short)24, SpriteOverlayConfig.class, SpriteOverlayConfigScript.class, 0, EnumBitStore.of(GameEntityType.IsoObject, GameEntityType.MetaEntity)
    ),
    CraftBenchSounds((short)25, CraftBenchSounds.class, CraftBenchSoundsScript.class, 0),
    WallCoveringConfig((short)26, WallCoveringConfig.class, WallCoveringConfigScript.class, 0),
    Undefined((short)0, null, null, 0);

    private static final Map<Short, ComponentType> idMap = new HashMap<>();
    private static final Map<Class<?>, ComponentType> classMap = new HashMap<>();
    private static final ArrayList<ComponentType> list = new ArrayList<>();
    private static final ComponentType[] array;
    static final BitSet bitsAddToEngine = new BitSet();
    static final BitSet bitsRunInMeta = new BitSet();
    static final BitSet bitsRenderLast = new BitSet();
    private static final ComponentFactory componentFactory = new ComponentFactory();
    private static final ComponentScriptFactory scriptFactory = new ComponentScriptFactory();
    public static final int MAX_ID_INDEX;
    final short id;
    final int flags;
    private final Class<? extends Component> componentClass;
    private final Class<? extends ComponentScript> componentScriptClass;
    private final EnumBitStore<GameEntityType> validEntityTypes;

    private <T extends Component, E extends ComponentScript> ComponentType(
        final short i, final Class<T> componentClass, final Class<E> scriptClass, final int flags
    ) {
        this(i, componentClass, scriptClass, flags, EnumBitStore.allOf(GameEntityType.class));
    }

    private <T extends Component, E extends ComponentScript> ComponentType(
        final short i, final Class<T> componentClass, final Class<E> scriptClass, final int flags, final EnumBitStore<GameEntityType> validEntityTypes
    ) {
        this.id = i;
        this.flags = flags;
        this.componentClass = componentClass;
        this.componentScriptClass = scriptClass;
        this.validEntityTypes = validEntityTypes;
        if (this.id > 0 && componentClass == null) {
            throw new IllegalArgumentException("ComponentType must have class extending 'Component' defined.");
        }
    }

    public short GetID() {
        return this.id;
    }

    public boolean isAddToEngine() {
        return Bits.hasFlags(this.flags, 1);
    }

    public boolean isRunInMeta() {
        return Bits.hasFlags(this.flags, 2);
    }

    public boolean isRenderLast() {
        return Bits.hasFlags(this.flags, 4);
    }

    public boolean isValidGameEntityType(GameEntityType type) {
        return this.validEntityTypes.contains(type);
    }

    public Class<? extends Component> GetComponentClass() {
        return this.componentClass;
    }

    public Component CreateComponent() {
        return componentFactory.alloc(this.componentClass);
    }

    public Component CreateComponentFromScript(ComponentScript script) {
        Component component = this.CreateComponent();
        component.readFromScript(script);
        return component;
    }

    public ComponentScript CreateComponentScript() {
        ComponentScript script = scriptFactory.create(this.componentScriptClass);
        if (script == null) {
            throw new RuntimeException("Unable to create script for component (No script class defined?): " + this);
        } else {
            return script;
        }
    }

    public static void ReleaseComponent(Component component) {
        componentFactory.release(component);
    }

    public static ComponentType FromId(short id) {
        ComponentType type = array[id];
        return type != null ? type : Undefined;
    }

    public static ComponentType FromClass(Class<? extends Component> clazz) {
        ComponentType type = classMap.get(clazz);
        return type != null ? type : Undefined;
    }

    public static ArrayList<ComponentType> GetList() {
        return list;
    }

    public static BitSet getBitsFor(ComponentType... componentTypes) {
        BitSet bits = new BitSet();
        int typesLength = componentTypes.length;

        for (int i = 0; i < typesLength; i++) {
            bits.set(componentTypes[i].GetID());
        }

        return bits;
    }

    static {
        int max_index = 0;

        for (ComponentType type : values()) {
            if (idMap.containsKey(type.id)) {
                throw new RuntimeException("ComponentType id '" + type.id + "' already assigned.");
            }

            idMap.put(type.id, type);
            classMap.put(type.componentClass, type);
            if (type != Undefined) {
                list.add(type);
            }

            max_index = PZMath.max(max_index, type.GetID());
            if (type.isAddToEngine()) {
                bitsAddToEngine.set(type.GetID());
            }

            if (type.isRunInMeta()) {
                bitsRunInMeta.set(type.GetID());
            }

            if (type.isRenderLast()) {
                bitsRenderLast.set(type.GetID());
            }
        }

        MAX_ID_INDEX = max_index + 1;
        if (MAX_ID_INDEX > 100 && Core.debug) {
            throw new RuntimeException("Warning MAX_ID_INDEX is high, increase warning threshold if this should be ignored.");
        } else {
            array = new ComponentType[MAX_ID_INDEX];
            Arrays.fill(array, Undefined);

            for (ComponentType type : values()) {
                array[type.GetID()] = type;
            }
        }
    }
}
