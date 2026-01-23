// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.network;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public final class ByteBufferReader {
    public ByteBuffer bb;

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

        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException var4) {
            throw new RuntimeException("Bad encoding!");
        }
    }
}
