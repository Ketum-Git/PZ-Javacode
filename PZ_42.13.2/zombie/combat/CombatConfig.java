// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;

@UsedFromLua
public class CombatConfig {
    private final EnumMap<CombatConfigKey, Float> values = new EnumMap<>(CombatConfigKey.class);

    public CombatConfig() {
        for (CombatConfigKey combatConfigKey : CombatConfigKey.values()) {
            this.values.put(combatConfigKey, combatConfigKey.getDefaultValue());
        }
    }

    public float get(CombatConfigKey combatConfigKey) {
        return this.values.get(combatConfigKey);
    }

    public void set(CombatConfigKey combatConfigKey, float value) {
        value = Math.max(combatConfigKey.getMinimum(), Math.min(combatConfigKey.getMaximum(), value));
        this.values.put(combatConfigKey, value);
    }

    public Map<CombatConfigKey, Float> getByCategory(CombatConfigCategory combatConfigCategory) {
        EnumMap<CombatConfigKey, Float> map = new EnumMap<>(CombatConfigKey.class);

        for (Entry<CombatConfigKey, Float> entry : this.values.entrySet()) {
            if (entry.getKey().getCategory().equals(combatConfigCategory)) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }
}
