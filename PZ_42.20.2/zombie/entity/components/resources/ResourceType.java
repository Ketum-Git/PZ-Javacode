// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import zombie.UsedFromLua;

@UsedFromLua
public enum ResourceType {
    Item((byte)1),
    Fluid((byte)2),
    Energy((byte)3),
    Any((byte)0);

    private final byte id;

    private ResourceType(final byte id) {
        this.id = id;
    }

    public byte getId() {
        return this.id;
    }

    public static ResourceType fromId(byte id) {
        for (ResourceType val : values()) {
            if (val.id == id) {
                return val;
            }
        }

        return null;
    }
}
