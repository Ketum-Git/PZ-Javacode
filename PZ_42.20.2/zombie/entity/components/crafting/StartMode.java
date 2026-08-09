// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum StartMode implements IOEnum {
    Manual((byte)1, 1),
    Automatic((byte)2, 2),
    Passive((byte)3, 4);

    private static final HashMap<Byte, StartMode> cache = new HashMap<>();
    final byte id;
    final int bits;

    private StartMode(final byte id, final int bits) {
        this.id = id;
        this.bits = bits;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    public static StartMode fromByteId(byte id) {
        return cache.get(id);
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        for (StartMode value : values()) {
            cache.put(value.id, value);
        }
    }
}
