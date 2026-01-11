// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.util.EnumSet;
import zombie.UsedFromLua;

@UsedFromLua
public enum AttributeValueType {
    Boolean((byte)0),
    String((byte)1),
    Float((byte)2),
    Double((byte)3),
    Byte((byte)4),
    Short((byte)5),
    Int((byte)6),
    Long((byte)7),
    Enum((byte)8),
    EnumSet((byte)9),
    EnumStringSet((byte)10);

    private static final EnumSet<AttributeValueType> numerics = java.util.EnumSet.of(Float, Double, Byte, Short, Int, Long);
    private static final EnumSet<AttributeValueType> decimals = java.util.EnumSet.of(Float, Double);
    private final byte byteIndex;

    private AttributeValueType(final byte index) {
        this.byteIndex = index;
    }

    public int getByteIndex() {
        return this.byteIndex;
    }

    public static boolean IsNumeric(AttributeValueType valueType) {
        return numerics.contains(valueType);
    }

    public static boolean IsDecimal(AttributeValueType valueType) {
        return decimals.contains(valueType);
    }

    public static AttributeValueType fromByteIndex(int value) {
        return AttributeValueType.class.getEnumConstants()[value];
    }

    public static AttributeValueType valueOfIgnoreCase(String s) {
        if (s == null) {
            return null;
        } else {
            for (AttributeValueType type : values()) {
                if (type.name().equalsIgnoreCase(s)) {
                    return type;
                }
            }

            return null;
        }
    }
}
