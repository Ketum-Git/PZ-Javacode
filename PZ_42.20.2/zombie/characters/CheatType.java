// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import zombie.UsedFromLua;

@UsedFromLua
public enum CheatType {
    GOD_MODE("GodMod"),
    INVISIBLE("Invisible"),
    UNLIMITED_ENDURANCE("UnlimitedEndurance"),
    UNLIMITED_AMMO("UnlimitedAmmo"),
    KNOW_ALL_RECIPES("KnowAllRecipes"),
    UNLIMITED_CARRY("UnlimitedCarry"),
    BUILD("BuildCheat"),
    FARMING("FarmingCheat"),
    FISHING("FishingCheat"),
    HEALTH("HealthCheat"),
    MECHANICS("MechanicsCheat"),
    FAST_MOVE("FastMove"),
    MOVABLES("MoveableCheat"),
    TIMED_ACTION_INSTANT("TimedActionInstant"),
    BRUSH_TOOL("BrushTool"),
    NO_CLIP("NoClip"),
    CAN_SEE_EVERYONE("CanSeeAll"),
    CAN_HEAR_EVERYONE("CanHearAll"),
    ZOMBIES_DONT_ATTACK("ZombiesDontAttack"),
    LOOT_ZED("LootZed"),
    LOOT_LOG("LootLog"),
    DEBUG_CONTEXT_MENU("DebugContextMenu"),
    ANIMAL("AnimalCheat"),
    ANIMAL_EXTRA_VALUES("AnimalExtraValues"),
    ALWAYS_DAY("AlwaysDay");

    private static final HashMap<String, CheatType> BY_NAME = new HashMap<>();
    private static final CheatType[] values = values();
    private static final List<CheatType> LIST = List.copyOf(Arrays.asList(values()));
    private final String tooltip;

    public static CheatType fromId(byte id) {
        return values[id];
    }

    public static CheatType fromString(String str) {
        return BY_NAME.get(str);
    }

    public static List<CheatType> getList() {
        return LIST;
    }

    private CheatType(final String tooltip) {
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    static {
        for (CheatType e : values) {
            BY_NAME.put(e.name(), e);
        }
    }
}
