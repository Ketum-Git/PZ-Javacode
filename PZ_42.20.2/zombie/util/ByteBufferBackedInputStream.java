// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ByteBufferBackedInputStream extends InputStream {
    final ByteBuffer buf;

    public ByteBufferBackedInputStream(ByteBuffer buf) {
        Objects.requireNonNull(buf);
        this.buf = buf;
    }

    @Override
    public int read() throws IOException {
        return !this.buf.hasRemaining() ? -1 : this.buf.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!this.buf.hasRemaining()) {
            return -1;
        } else {
            len = Math.min(len, this.buf.remaining());
            this.buf.get(bytes, off, len);
            return len;
        }
    }
}
