// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class MemoryFileDevice implements IFileDevice {
    @Override
    public IFile createFile(IFile child) {
        return new MemoryFileDevice.MemoryFile(child, this);
    }

    @Override
    public void destroyFile(IFile file) {
    }

    @Override
    public InputStream createStream(String path, InputStream child) throws IOException {
        return null;
    }

    @Override
    public void destroyStream(InputStream stream) {
    }

    @Override
    public String name() {
        return "memory";
    }

    private static class MemoryFile implements IFile {
        final MemoryFileDevice device;
        byte[] buffer;
        long size;
        long pos;
        IFile file;
        boolean write;

        MemoryFile(IFile file, MemoryFileDevice device) {
            this.device = device;
            this.buffer = null;
            this.size = 0L;
            this.pos = 0L;
            this.file = file;
            this.write = false;
        }

        @Override
        public boolean open(String path, int mode) {
            assert this.buffer == null;

            this.write = (mode & 2) != 0;
            if (this.file != null) {
                if (this.file.open(path, mode)) {
                    if ((mode & 1) != 0) {
                        this.size = this.file.size();
                        this.buffer = new byte[(int)this.size];
                        this.file.read(this.buffer, this.size);
                        this.pos = 0L;
                    }

                    return true;
                }
            } else if ((mode & 2) != 0) {
                return true;
            }

            return false;
        }

        @Override
        public void close() {
            if (this.file != null) {
                if (this.write) {
                    this.file.seek(FileSeekMode.BEGIN, 0L);
                    this.file.write(this.buffer, this.size);
                }

                this.file.close();
            }

            this.buffer = null;
        }

        @Override
        public boolean read(byte[] buffer, long size) {
            long amount = this.pos + size < this.size ? size : this.size - this.pos;
            System.arraycopy(this.buffer, (int)this.pos, buffer, 0, (int)amount);
            this.pos += amount;
            return false;
        }

        @Override
        public boolean write(byte[] buffer, long size) {
            long pos = this.pos;
            long cap = this.buffer.length;
            long sz = this.size;
            if (pos + size > cap) {
                long new_cap = Math.max(cap * 2L, pos + size);
                this.buffer = Arrays.copyOf(this.buffer, (int)new_cap);
            }

            System.arraycopy(buffer, 0, this.buffer, (int)pos, (int)size);
            this.pos += size;
            this.size = pos + size > sz ? pos + size : sz;
            return true;
        }

        @Override
        public byte[] getBuffer() {
            return this.buffer;
        }

        @Override
        public long size() {
            return this.size;
        }

        @Override
        public boolean seek(FileSeekMode mode, long pos) {
            switch (mode) {
                case BEGIN:
                    assert pos <= this.size;

                    this.pos = pos;
                    break;
                case CURRENT:
                    assert 0L <= this.pos + pos && this.pos + pos <= this.size;

                    this.pos += pos;
                    break;
                case END:
                    assert pos <= this.size;

                    this.pos = this.size - pos;
            }

            boolean ret = this.pos <= this.size;
            this.pos = Math.min(this.pos, this.size);
            return ret;
        }

        @Override
        public long pos() {
            return this.pos;
        }

        @Override
        public InputStream getInputStream() {
            return this.file != null ? this.file.getInputStream() : null;
        }

        @Override
        public IFileDevice getDevice() {
            return this.device;
        }

        @Override
        public void release() {
            this.buffer = null;
        }
    }
}
