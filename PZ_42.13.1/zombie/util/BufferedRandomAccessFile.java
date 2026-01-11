// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public final class BufferedRandomAccessFile extends RandomAccessFile {
    private final byte[] buffer;
    private int bufEnd;
    private int bufPos;
    private long realPos;
    private final int bufSize;

    public BufferedRandomAccessFile(String filename, String mode, int bufsize) throws IOException {
        super(filename, mode);
        this.invalidate();
        this.bufSize = bufsize;
        this.buffer = new byte[this.bufSize];
    }

    public BufferedRandomAccessFile(File file, String mode, int bufsize) throws IOException {
        super(file, mode);
        this.invalidate();
        this.bufSize = bufsize;
        this.buffer = new byte[this.bufSize];
    }

    @Override
    public final int read() throws IOException {
        if (this.bufPos >= this.bufEnd && this.fillBuffer() < 0) {
            return -1;
        } else {
            return this.bufEnd == 0 ? -1 : this.buffer[this.bufPos++] & 0xFF;
        }
    }

    @Override
    public int read(byte[] bb) throws IOException {
        return this.read(bb, 0, bb.length);
    }

    private int fillBuffer() throws IOException {
        int n = super.read(this.buffer, 0, this.bufSize);
        if (n >= 0) {
            this.realPos += n;
            this.bufEnd = n;
            this.bufPos = 0;
        }

        return n;
    }

    private void invalidate() throws IOException {
        this.bufEnd = 0;
        this.bufPos = 0;
        this.realPos = super.getFilePointer();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int leftover = this.bufEnd - this.bufPos;
        if (len <= leftover) {
            System.arraycopy(this.buffer, this.bufPos, b, off, len);
            this.bufPos += len;
            return len;
        } else {
            for (int i = 0; i < len; i++) {
                int c = this.read();
                if (c == -1) {
                    return i;
                }

                b[off + i] = (byte)c;
            }

            return len;
        }
    }

    @Override
    public long getFilePointer() throws IOException {
        long l = this.realPos;
        return l - this.bufEnd + this.bufPos;
    }

    @Override
    public void seek(long pos) throws IOException {
        int n = (int)(this.realPos - pos);
        if (n >= 0 && n <= this.bufEnd) {
            this.bufPos = this.bufEnd - n;
        } else {
            super.seek(pos);
            this.invalidate();
        }
    }

    public final String getNextLine() throws IOException {
        String str = null;
        if (this.bufEnd - this.bufPos <= 0 && this.fillBuffer() < 0) {
            throw new IOException("error in filling buffer!");
        } else {
            int lineend = -1;

            for (int i = this.bufPos; i < this.bufEnd; i++) {
                if (this.buffer[i] == 10) {
                    lineend = i;
                    break;
                }
            }

            if (lineend < 0) {
                StringBuilder input = new StringBuilder(128);

                int c;
                while ((c = this.read()) != -1 && c != 10) {
                    input.append((char)c);
                }

                return c == -1 && input.isEmpty() ? null : input.toString();
            } else {
                if (lineend > 0 && this.buffer[lineend - 1] == 13) {
                    str = new String(this.buffer, this.bufPos, lineend - this.bufPos - 1, StandardCharsets.UTF_8);
                } else {
                    str = new String(this.buffer, this.bufPos, lineend - this.bufPos, StandardCharsets.UTF_8);
                }

                this.bufPos = lineend + 1;
                return str;
            }
        }
    }
}
