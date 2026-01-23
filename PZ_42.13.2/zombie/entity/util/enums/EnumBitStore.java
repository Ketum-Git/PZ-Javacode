// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.enums;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;
import zombie.core.utils.Bits;

public class EnumBitStore<E extends Enum<E> & IOEnum> {
    private static final String emptyToString = "[]";
    private int bits = 0;
    final transient Class<E> elementType;

    private EnumBitStore(Class<E> elementType) {
        this.elementType = elementType;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> noneOf(Class<E> elementType) {
        return new EnumBitStore<>(elementType);
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> allOf(Class<E> elementType) {
        EnumBitStore<E> result = noneOf(elementType);
        result.addAll();
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> copyOf(EnumBitStore<E> other) {
        EnumBitStore<E> result = noneOf(other.elementType);
        result.copyFrom(other);
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E e) {
        EnumBitStore<E> result = noneOf(e.getDeclaringClass());
        result.add(e);
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E e1, E e2) {
        EnumBitStore<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E e1, E e2, E e3) {
        EnumBitStore<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E e1, E e2, E e3, E e4) {
        EnumBitStore<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumBitStore<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    @SafeVarargs
    public static <E extends Enum<E> & IOEnum> EnumBitStore<E> of(E first, E... rest) {
        EnumBitStore<E> result = noneOf(first.getDeclaringClass());
        result.add(first);

        for (E e : rest) {
            result.add(e);
        }

        return result;
    }

    public void copyFrom(EnumBitStore<E> other) {
        this.bits = other.bits;
    }

    public void addAll(EnumBitStore<E> other) {
        this.bits = Bits.addFlags(this.bits, other.bits);
    }

    public void addAll() {
        for (E e : (Enum[])this.elementType.getEnumConstants()) {
            this.add(e);
        }
    }

    public void add(E e) {
        this.bits = Bits.addFlags(this.bits, e.getBits());
    }

    public void remove(E e) {
        this.bits = Bits.removeFlags(this.bits, e.getBits());
    }

    public boolean contains(E e) {
        return this.contains(e.getBits());
    }

    public boolean contains(int bits) {
        return Bits.hasFlags(this.bits, bits);
    }

    public int size() {
        return Integer.bitCount(this.bits);
    }

    public boolean isEmpty() {
        return this.bits == 0;
    }

    public void clear() {
        this.bits = 0;
    }

    public int getBits() {
        return this.bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putInt(this.bits);
    }

    public void load(ByteBuffer input) throws IOException {
        this.bits = input.getInt();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EnumBitStore<?> es) {
            return es.elementType == this.elementType ? es.bits == this.bits : false;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.size() <= 0) {
            return "[]";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            EnumBitStore<E>.EnumBitStoreIterator<E> iterator = new EnumBitStore.EnumBitStoreIterator<>();

            while (iterator.hasNext()) {
                sb.append(iterator.next().toString());
                if (iterator.returned < iterator.size) {
                    sb.append(",");
                }
            }

            sb.append("]");
            return sb.toString();
        }
    }

    public Iterator<E> iterator() {
        return new EnumBitStore.EnumBitStoreIterator<>();
    }

    private class EnumBitStoreIterator<E extends Enum<E> & IOEnum> implements Iterator<E> {
        int index;
        int returned;
        int size;

        EnumBitStoreIterator() {
            Objects.requireNonNull(EnumBitStore.this);
            super();
            this.index = 0;
            this.size = EnumBitStore.this.size();
        }

        @Override
        public boolean hasNext() {
            return this.returned < this.size;
        }

        public E next() {
            while (this.index < ((Enum[])EnumBitStore.this.elementType.getEnumConstants()).length) {
                E e = null;
                if (EnumBitStore.this.contains(EnumBitStore.this.elementType.getEnumConstants()[this.index])) {
                    e = EnumBitStore.this.elementType.getEnumConstants()[this.index];
                }

                this.index++;
                if (e != null) {
                    this.returned++;
                    return e;
                }
            }

            throw new IllegalStateException();
        }
    }
}
