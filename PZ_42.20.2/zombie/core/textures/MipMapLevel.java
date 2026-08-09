// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.nio.ByteBuffer;
import zombie.core.utils.DirectBufferAllocator;
import zombie.core.utils.WrappedBuffer;

public final class MipMapLevel {
    public final int width;
    public final int height;
    public final WrappedBuffer data;

    public MipMapLevel(int width, int height) {
        this.width = width;
        this.height = height;
        this.data = DirectBufferAllocator.allocate(width * height * 4);
    }

    public MipMapLevel(int width, int height, WrappedBuffer data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public void dispose() {
        if (this.data != null) {
            this.data.dispose();
        }
    }

    public boolean isDisposed() {
        return this.data != null && this.data.isDisposed();
    }

    public void rewind() {
        if (this.data != null) {
            this.data.getBuffer().rewind();
        }
    }

    public ByteBuffer getBuffer() {
        return this.data == null ? null : this.data.getBuffer();
    }

    public int getDataSize() {
        return this.width * this.height * 4;
    }
}
