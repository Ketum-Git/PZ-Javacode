// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.network;

import java.nio.ByteBuffer;
import zombie.GameWindow;

public final class ByteBufferWriter {
    public ByteBuffer bb;

    public ByteBufferWriter(ByteBuffer bb) {
        this.bb = bb;
    }

    public void putBoolean(boolean v) {
        this.bb.put((byte)(v ? 1 : 0));
    }

    public void putByte(byte v) {
        this.bb.put(v);
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

    public void putShort(short v) {
        this.bb.putShort(v);
    }

    public void putUTF(String string) {
        GameWindow.WriteStringUTF(this.bb, string);
    }
}
