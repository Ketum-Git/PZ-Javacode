// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public final class PNGSize {
    private static final byte[] SIGNATURE = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
    private static final int IHDR = 1229472850;
    public int width;
    public int height;
    private int bitdepth;
    private int colorType;
    private int bytesPerPixel;
    private InputStream input;
    private final CRC32 crc = new CRC32();
    private final byte[] buffer = new byte[4096];
    private int chunkLength;
    private int chunkType;
    private int chunkRemaining;

    public void readSize(String path) {
        try (
            FileInputStream fis = new FileInputStream(path);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            this.readSize(bis);
        } catch (Exception var10) {
            var10.printStackTrace();
        }
    }

    public void readSize(InputStream input) throws IOException {
        this.input = input;
        this.readFully(this.buffer, 0, SIGNATURE.length);
        if (!this.checkSignature(this.buffer)) {
            throw new IOException("Not a valid PNG file");
        } else {
            this.openChunk(1229472850);
            this.readIHDR();
            this.closeChunk();
        }
    }

    private void readIHDR() throws IOException {
        this.checkChunkLength(13);
        this.readChunk(this.buffer, 0, 13);
        this.width = this.readInt(this.buffer, 0);
        this.height = this.readInt(this.buffer, 4);
        this.bitdepth = this.buffer[8] & 255;
        this.colorType = this.buffer[9] & 255;
    }

    private void openChunk() throws IOException {
        this.readFully(this.buffer, 0, 8);
        this.chunkLength = this.readInt(this.buffer, 0);
        this.chunkType = this.readInt(this.buffer, 4);
        this.chunkRemaining = this.chunkLength;
        this.crc.reset();
        this.crc.update(this.buffer, 4, 4);
    }

    private void openChunk(int expected) throws IOException {
        this.openChunk();
        if (this.chunkType != expected) {
            throw new IOException("Expected chunk: " + Integer.toHexString(expected));
        }
    }

    private void closeChunk() throws IOException {
        if (this.chunkRemaining > 0) {
            this.skip(this.chunkRemaining + 4);
        } else {
            this.readFully(this.buffer, 0, 4);
            int expectedCrc = this.readInt(this.buffer, 0);
            int computedCrc = (int)this.crc.getValue();
            if (computedCrc != expectedCrc) {
                throw new IOException("Invalid CRC");
            }
        }

        this.chunkRemaining = 0;
        this.chunkLength = 0;
        this.chunkType = 0;
    }

    private void checkChunkLength(int expected) throws IOException {
        if (this.chunkLength != expected) {
            throw new IOException("Chunk has wrong size");
        }
    }

    private int readChunk(byte[] buffer, int offset, int length) throws IOException {
        if (length > this.chunkRemaining) {
            length = this.chunkRemaining;
        }

        this.readFully(buffer, offset, length);
        this.crc.update(buffer, offset, length);
        this.chunkRemaining -= length;
        return length;
    }

    private void readFully(byte[] buffer, int offset, int length) throws IOException {
        do {
            int read = this.input.read(buffer, offset, length);
            if (read < 0) {
                throw new EOFException();
            }

            offset += read;
            length -= read;
        } while (length > 0);
    }

    private int readInt(byte[] buffer, int offset) {
        return buffer[offset] << 24 | (buffer[offset + 1] & 0xFF) << 16 | (buffer[offset + 2] & 0xFF) << 8 | buffer[offset + 3] & 0xFF;
    }

    private void skip(long amount) throws IOException {
        while (amount > 0L) {
            long skipped = this.input.skip(amount);
            if (skipped < 0L) {
                throw new EOFException();
            }

            amount -= skipped;
        }
    }

    private boolean checkSignature(byte[] buffer) {
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (buffer[i] != SIGNATURE[i]) {
                return false;
            }
        }

        return true;
    }
}
