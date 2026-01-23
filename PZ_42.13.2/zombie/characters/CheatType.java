// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.HashMap;

public enum CheatType {
    GOD_MODE,
    INVISIBLE,
    UNLIMITED_ENDURANCE,
    UNLIMITED_AMMO,
    KNOW_ALL_RECIPES,
    UNLIMITED_CARRY,
    BUILD,
    FARMING,
    FISHING,
    HEALTH,
    MECHANICS,
    FAST_MOVE,
    MOVABLES,
    TIMED_ACTION_INSTANT,
    BRUSH_TOOL,
    NO_CLIP,
    CAN_SEE_EVERYONE,
    CAN_HEAR_EVERYONE,
    ZOMBIES_DONT_ATTACK,
    SHOW_MP_INFO,
    LOOT_TOOL,
    DEBUG_CONTEXT_MENU,
    ANIMAL;

    private static final HashMap<String, CheatType> BY_NAME = new HashMap<>();
    private static final CheatType[] values = values();

    public static CheatType fromId(byte id) {
        return values[id];
    }

    public static CheatType fromString(String str) {
        return BY_NAME.get(str);
    }

    static {
        for (CheatType e : values) {
            BY_NAME.put(e.name(), e);
        }
    }
}
