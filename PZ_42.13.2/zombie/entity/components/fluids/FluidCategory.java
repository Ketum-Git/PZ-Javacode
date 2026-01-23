// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;

@UsedFromLua
public enum FluidCategory {
    Beverage((byte)1),
    Alcoholic((byte)2),
    Hazardous((byte)3),
    Medical((byte)4),
    Industrial((byte)5),
    Colors((byte)6),
    Dyes((byte)7),
    HairDyes((byte)8),
    Paint((byte)9),
    Fuel((byte)10),
    Poisons((byte)11),
    Water((byte)12);

    private static final HashMap<Byte, FluidCategory> idMap = new HashMap<>();
    private static final ArrayList<FluidCategory> list = new ArrayList<>();
    private final byte id;

    private FluidCategory(final byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static FluidCategory FromId(byte id) {
        return idMap.get(id);
    }

    public static ArrayList<FluidCategory> getList() {
        return list;
    }

    static {
        for (FluidCategory category : values()) {
            idMap.put(category.id, category);
            list.add(category);
        }
    }
}
