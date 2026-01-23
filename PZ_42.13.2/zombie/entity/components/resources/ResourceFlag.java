// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum ResourceFlag implements IOEnum {
    AutoDecay((byte)3, 4);

    private static final HashMap<Byte, ResourceFlag> cache = new HashMap<>();
    final byte id;
    final int bits;

    private ResourceFlag(final byte id, final int bits) {
        this.id = id;
        this.bits = bits;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    public static ResourceFlag fromByteId(byte id) {
        return cache.get(id);
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    static {
        for (ResourceFlag value : values()) {
            cache.put(value.id, value);
        }
    }
}
