// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.IdentityHashMap;
import zombie.core.Core;

public final class ByteBufferReader {
    private static final IdentityHashMap<Class<?>, Object[]> VALUES_BY_TYPE = new IdentityHashMap<>();
    public final ByteBuffer bb;

    public ByteBufferReader(ByteBuffer bb) {
        this.bb = bb;
    }

    public boolean getBoolean() {
        return this.bb.get() != 0;
    }

    public byte getByte() {
        return this.bb.get();
    }

    public char getChar() {
        return this.bb.getChar();
    }

    public double getDouble() {
        return this.bb.getDouble();
    }

    public float getFloat() {
        return this.bb.getFloat();
    }

    public int getInt() {
        return this.bb.getInt();
    }

    public long getLong() {
        return this.bb.getLong();
    }

    public short getShort() {
        return this.bb.getShort();
    }

    public String getUTF() {
        int length = this.bb.getShort();
        byte[] bytes = new byte[length];
        this.bb.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public <E extends Enum<E>> E getEnum(Class<E> clazz) {
        return (E)VALUES_BY_TYPE.computeIfAbsent(clazz, k -> {
            E[] values = clazz.getEnumConstants();
            if (Core.IS_DEV && values.length > 255) {
                throw new IllegalArgumentException("Enum class " + clazz.getName() + " is too large");
            } else {
                return values;
            }
        })[this.getByte() & 255];
    }

    public void position(int newPosition) {
        this.bb.position(newPosition);
    }

    public int position() {
        return this.bb.position();
    }

    public void flip() {
        this.bb.flip();
    }

    public void clear() {
        this.bb.clear();
    }

    public void put(byte[] src, int offset, int length) {
        this.bb.put(src, offset, length);
    }

    public int limit() {
        return this.bb.limit();
    }

    public void put(ByteBuffer src) {
        this.bb.put(src);
    }

    public void rewind() {
        this.bb.rewind();
    }

    public int capacity() {
        return this.bb.capacity();
    }

    public byte[] array() {
        return this.bb.array();
    }

    public int remaining() {
        return this.bb.remaining();
    }

    public void get(byte[] dst) {
        this.bb.get(dst);
    }

    public void get(byte[] dst, int offet, int length) {
        this.bb.get(dst, offet, length);
    }

    public void mark() {
        this.bb.mark();
    }

    public void reset() {
        this.bb.reset();
    }
}
