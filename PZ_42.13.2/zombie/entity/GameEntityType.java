// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum GameEntityType implements IOEnum {
    IsoObject((byte)1, 1),
    InventoryItem((byte)2, 2),
    VehiclePart((byte)3, 4),
    IsoMovingObject((byte)4, 8),
    Template((byte)5, 16),
    MetaEntity((byte)5, 32);

    private static final HashMap<Byte, GameEntityType> map = new HashMap<>();
    private final byte id;
    private final int bits;

    private GameEntityType(final byte id, final int bits) {
        this.id = id;
        this.bits = bits;
    }

    public byte getId() {
        return this.id;
    }

    public static GameEntityType FromID(byte id) {
        return map.get(id);
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        for (GameEntityType type : values()) {
            map.put(type.id, type);
        }
    }
}
