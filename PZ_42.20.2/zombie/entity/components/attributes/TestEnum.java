// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import zombie.entity.util.enums.IOEnum;

public enum TestEnum implements IOEnum {
    TestValueA((byte)1),
    TestValueB((byte)2),
    TestValueC((byte)3),
    TestValueD((byte)4),
    TestValueE((byte)5),
    TestValueF((byte)6);

    private final byte id;

    private TestEnum(final byte id) {
        this.id = id;
    }

    @Override
    public byte getByteId() {
        return this.id;
    }

    @Override
    public int getBits() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
