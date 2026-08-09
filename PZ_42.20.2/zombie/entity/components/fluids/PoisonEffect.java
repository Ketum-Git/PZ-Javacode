// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;

@UsedFromLua
public enum PoisonEffect {
    None(0),
    Mild(1),
    Medium(2),
    Severe(3),
    Extreme(4),
    Deadly(100);

    private static final HashSet<String> names = new HashSet<>();
    private static final HashMap<Integer, PoisonEffect> levelMap = new HashMap<>();
    private static final HashMap<String, PoisonEffect> nameMap = new HashMap<>();
    private final int level;
    private String lowerCache;

    private PoisonEffect(final int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    public int getPlayerEffect() {
        return this.level * 7;
    }

    public static PoisonEffect FromLevel(int level) {
        PoisonEffect effect = levelMap.get(level);
        return effect != null ? effect : None;
    }

    public String toStringLower() {
        if (this.lowerCache != null) {
            return this.lowerCache;
        } else {
            this.lowerCache = this.toString().toLowerCase();
            return this.lowerCache;
        }
    }

    public static PoisonEffect FromNameLower(String name) {
        return nameMap.get(name.toLowerCase());
    }

    public static boolean containsNameLowercase(String name) {
        return names.contains(name.toLowerCase());
    }

    static {
        for (PoisonEffect poisonEffect : values()) {
            names.add(poisonEffect.toString().toLowerCase());
            levelMap.put(poisonEffect.level, poisonEffect);
            nameMap.put(poisonEffect.toString().toLowerCase(), poisonEffect);
        }
    }
}
