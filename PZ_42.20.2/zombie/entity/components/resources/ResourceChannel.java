// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.entity.util.enums.EnumBitStore;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public enum ResourceChannel implements IOEnum {
    NO_CHANNEL((byte)0, 0, Colors.Black),
    Channel_Red((byte)1, 1, Colors.Crimson),
    Channel_Yellow((byte)2, 2, Colors.Gold),
    Channel_Blue((byte)3, 4, Colors.DodgerBlue),
    Channel_Orange((byte)4, 8, Colors.Orange),
    Channel_Green((byte)5, 16, Colors.LimeGreen),
    Channel_Purple((byte)6, 32, Colors.MediumSlateBlue),
    Channel_Cyan((byte)7, 64, Colors.Cyan),
    Channel_Magenta((byte)8, 128, Colors.Magenta);

    public static final EnumBitStore<ResourceChannel> BitStoreAll = EnumBitStore.allOf(ResourceChannel.class);
    private final byte id;
    private final int bits;
    private final Color color;

    private ResourceChannel(final byte id, final int bits, final Color color) {
        this.id = id;
        this.bits = bits;
        this.color = color;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        return this.bits;
    }

    public Color getColor() {
        return this.color;
    }

    public static ResourceChannel fromId(byte id) {
        for (ResourceChannel val : values()) {
            if (val.id == id) {
                return val;
            }
        }

        return null;
    }
}
