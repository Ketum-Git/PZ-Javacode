// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.network;

import java.nio.ByteBuffer;
import zombie.GameWindow;

public final class ByteBufferWriter {
    public final ByteBuffer bb;

    public ByteBufferWriter(ByteBuffer bb) {
        this.bb = bb;
    }

    public boolean putBoolean(boolean v) {
        this.bb.put((byte)(v ? 1 : 0));
        return v;
    }

    public void putByte(byte v) {
        this.bb.put(v);
    }

    public void putByte(int v) {
        this.putByte((byte)v);
    }

    public void putByte(long v) {
        this.putByte((byte)v);
    }

    public void putChar(char v) {
        this.bb.putChar(v);
    }

    public void putDouble(double v) {
        this.bb.putDouble(v);
    }

    public void putFloat(float v) {
        this.bb.putFloat(v);
    }

    public void putInt(int v) {
        this.bb.putInt(v);
    }

    public void putLong(long v) {
        this.bb.putLong(v);
    }

    public void putShort(int v) {
        this.putShort((short)v);
    }

    public void putShort(short v) {
        this.bb.putShort(v);
    }

    public void putUTF(String string) {
        GameWindow.WriteString(this.bb, string);
    }

    public <E extends Enum<E>> void putEnum(E v) {
        this.putByte(v.ordinal() & 0xFF);
    }

    public void clear() {
        this.bb.clear();
    }

    public void flip() {
        this.bb.flip();
    }

    public void put(ByteBuffer src) {
        this.bb.put(src);
    }

    public void put(ByteBufferReader src) {
        this.put(src.bb);
    }

    public void put(byte[] src, int offset, int length) {
        this.bb.put(src, offset, length);
    }

    public int position() {
        return this.bb.position();
    }

    public void position(int newPosition) {
        this.bb.position(newPosition);
    }

    public void put(byte[] src) {
        this.bb.put(src);
    }

    @Override
    public int hashCode() {
        this.bb.flip();
        return this.bb.hashCode();
    }
}
