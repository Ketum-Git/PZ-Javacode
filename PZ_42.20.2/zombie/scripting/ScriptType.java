// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import zombie.UsedFromLua;

@UsedFromLua
public enum ScriptType {
    EntityComponent("entityComponent"),
    VehicleTemplate(true, "vehicle"),
    EntityTemplate(true, "entity"),
    Item("item"),
    Recipe("recipe"),
    UniqueRecipe("uniquerecipe"),
    EvolvedRecipe("evolvedrecipe"),
    Fixing("fixing"),
    AnimationMesh("animationsMesh"),
    Mannequin("mannequin"),
    Model("model"),
    SpriteModel("spriteModel"),
    Sound("sound"),
    SoundTimeline("soundTimeline"),
    Vehicle("vehicle"),
    RuntimeAnimation("animation"),
    VehicleEngineRPM("vehicleEngineRPM"),
    ItemConfig("itemConfig"),
    Entity("entity"),
    XuiLayout("xuiLayout"),
    XuiStyle("xuiStyle"),
    XuiDefaultStyle("xuiDefaultStyle"),
    XuiColor("xuiGlobalColors"),
    XuiSkin("xuiSkin"),
    XuiConfig("xuiConfig"),
    ItemFilter("itemFilter"),
    CraftRecipe("craftRecipe"),
    FluidFilter("fluidFilter"),
    StringList("stringList"),
    EnergyDefinition("energy"),
    FluidDefinition("fluid"),
    PhysicsShape("physicsShape"),
    TimedAction("timedAction"),
    Ragdoll("ragdoll"),
    PhysicsHitReaction("physicsHitReaction"),
    Clock("clock"),
    CharacterTraitDefinition("character_trait_definition"),
    CharacterProfessionDefinition("character_profession_definition");

    private static final ArrayList<ScriptType> sortedList;
    private static final Comparator<ScriptType> typeComparator = (object1, object2) -> {
        if (object1.isTemplate && !object2.isTemplate) {
            return 1;
        } else {
            return !object1.isTemplate && object2.isTemplate ? -1 : object1.toString().compareTo(object2.toString());
        }
    };
    private final boolean isTemplate;
    private final String scriptTag;
    private final boolean isCritical = false;
    private Set<ScriptType.Flags> flags;
    private boolean verbose;

    private ScriptType(final String scriptTag) {
        this(false, scriptTag);
    }

    private ScriptType(final boolean isTemplate, final String scriptTag) {
        this.isTemplate = isTemplate;
        this.scriptTag = scriptTag;
    }

    public boolean isTemplate() {
        return this.isTemplate;
    }

    public boolean isCritical() {
        return false;
    }

    public String getScriptTag() {
        return this.scriptTag;
    }

    public boolean hasFlag(ScriptType.Flags flag) {
        return this.flags.contains(flag);
    }

    public boolean hasFlags(EnumSet<ScriptType.Flags> flags) {
        return this.flags.containsAll(flags);
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public static ArrayList<ScriptType> GetEnumListLua() {
        return sortedList;
    }

    static {
        EnumSet<ScriptType.Flags> defaultFlags = EnumSet.of(
            ScriptType.Flags.Clear,
            ScriptType.Flags.CacheFullType,
            ScriptType.Flags.ResetExisting,
            ScriptType.Flags.RemoveLoadError,
            ScriptType.Flags.SeekImports,
            ScriptType.Flags.AllowNewScriptDiscoveryOnReload
        );
        EnumSet<ScriptType.Flags> dictionaryFlags = EnumSet.copyOf(defaultFlags);
        dictionaryFlags.remove(ScriptType.Flags.AllowNewScriptDiscoveryOnReload);
        Item.flags = Collections.unmodifiableSet(dictionaryFlags);
        Entity.flags = Collections.unmodifiableSet(dictionaryFlags);
        EnumSet<ScriptType.Flags> vehicleFlags = EnumSet.copyOf(defaultFlags);
        vehicleFlags.remove(ScriptType.Flags.ResetExisting);
        vehicleFlags.add(ScriptType.Flags.NewInstanceOnReload);
        Vehicle.flags = Collections.unmodifiableSet(vehicleFlags);
        XuiSkin.flags = Collections.unmodifiableSet(
            EnumSet.of(
                ScriptType.Flags.Clear,
                ScriptType.Flags.CacheFullType,
                ScriptType.Flags.RemoveLoadError,
                ScriptType.Flags.SeekImports,
                ScriptType.Flags.ResetOnceOnReload
            )
        );
        XuiSkin.setVerbose(true);
        ArrayList<ScriptType> list = new ArrayList<>();

        for (ScriptType type : values()) {
            if (type.flags == null) {
                type.flags = Collections.unmodifiableSet(defaultFlags);
            }

            if (type != EntityComponent) {
                list.add(type);
            }
        }

        list.sort(typeComparator);
        sortedList = list;
    }

    public static enum Flags {
        Clear,
        FromList,
        CacheFullType,
        ResetExisting,
        RemoveLoadError,
        SeekImports,
        ResetOnceOnReload,
        AllowNewScriptDiscoveryOnReload,
        NewInstanceOnReload;
    }
}
