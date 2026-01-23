// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.energy;

import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public enum EnergyType {
    None((byte)-1),
    Electric((byte)1),
    Mechanical((byte)2),
    Thermal((byte)3),
    Steam((byte)4),
    VoidEnergy((byte)126),
    Modded((byte)127);

    private static final HashSet<String> energyNames = new HashSet<>();
    private static final HashMap<Byte, EnergyType> energyIdMap = new HashMap<>();
    private static final HashMap<String, EnergyType> energyNameMap = new HashMap<>();
    private final byte id;
    private String lowerCache;

    private EnergyType(final byte typeID) {
        this.id = typeID;
    }

    public byte getId() {
        return this.id;
    }

    public String toStringLower() {
        if (this.lowerCache != null) {
            return this.lowerCache;
        } else {
            this.lowerCache = this.toString().toLowerCase();
            return this.lowerCache;
        }
    }

    public static boolean containsNameLowercase(String name) {
        return energyNames.contains(name.toLowerCase());
    }

    public static EnergyType FromId(byte id) {
        return energyIdMap.get(id);
    }

    public static EnergyType FromNameLower(String name) {
        return energyNameMap.get(name.toLowerCase());
    }

    static {
        for (EnergyType type : values()) {
            if (Core.debug && energyIdMap.containsKey(type.id)) {
                throw new IllegalStateException("ID duplicate in EnergyType");
            }

            energyNames.add(type.toString().toLowerCase());
            energyNameMap.put(type.toString().toLowerCase(), type);
            energyIdMap.put(type.id, type);
        }
    }
}
