// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import zombie.UsedFromLua;

@UsedFromLua
public enum ResourceIO {
    Input((byte)1),
    Output((byte)2),
    Any((byte)3);

    private final byte id;

    private ResourceIO(final byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static ResourceIO fromId(byte id) {
        for (ResourceIO val : values()) {
            if (val.id == id) {
                return val;
            }
        }

        return null;
    }
}
