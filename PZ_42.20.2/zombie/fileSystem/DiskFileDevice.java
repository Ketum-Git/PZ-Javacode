// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import zombie.core.logger.ExceptionLogger;

public final class DiskFileDevice implements IFileDevice {
    private final String name;

    public DiskFileDevice(String name) {
        this.name = name;
    }

    @Override
    public IFile createFile(IFile child) {
        return new DiskFileDevice.DiskFile(child, this);
    }

    @Override
    public void destroyFile(IFile file) {
    }

    @Override
    public InputStream createStream(String path, InputStream child) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public void destroyStream(InputStream stream) {
    }

    @Override
    public String name() {
        return this.name;
    }

    private static final class DiskFile implements IFile {
        final DiskFileDevice device;
        RandomAccessFile file;
        InputStream inputStream;
        final IFile fallthrough;
        boolean useFallthrough;

        DiskFile(IFile fallthrough, DiskFileDevice device) {
            this.device = device;
            this.fallthrough = fallthrough;
            this.useFallthrough = false;
        }

        @Override
        public boolean open(String path, int mode) {
            File file = new File(path);
            boolean wantRead = (mode & 1) != 0;
            if (wantRead && !file.exists() && this.fallthrough != null) {
                this.useFallthrough = true;
                return this.fallthrough.open(path, mode);
            } else {
                try {
                    if ((mode & 16) == 0) {
                        this.file = new RandomAccessFile(path, FileOpenMode.toStringMode(mode));
                    } else {
                        this.inputStream = new FileInputStream(path);
                    }

                    return true;
                } catch (IOException var6) {
                    ExceptionLogger.logException(var6);
                    return false;
                }
            }
        }

        @Override
        public void close() {
            if (this.fallthrough != null) {
                this.fallthrough.close();
            }

            if (this.file != null || this.inputStream != null) {
                try {
                    if (this.file != null) {
                        this.file.close();
                    }

                    if (this.inputStream != null) {
                        this.inputStream.close();
                    }
                } catch (IOException var2) {
                    ExceptionLogger.logException(var2);
                }

                this.file = null;
                this.inputStream = null;
                this.useFallthrough = false;
            }
        }

        @Override
        public boolean read(byte[] buffer, long size) {
            if (this.useFallthrough) {
                return this.fallthrough.read(buffer, size);
            } else if (this.file == null) {
                return false;
            } else {
                try {
                    return this.file.read(buffer, 0, (int)size) == size;
                } catch (IOException var5) {
                    ExceptionLogger.logException(var5);
                    return false;
                }
            }
        }

        @Override
        public boolean write(byte[] buffer, long size) {
            if (this.useFallthrough) {
                return this.fallthrough.write(buffer, size);
            } else if (this.file == null) {
                return false;
            } else {
                try {
                    this.file.write(buffer, 0, (int)size);
                    return true;
                } catch (IOException var5) {
                    ExceptionLogger.logException(var5);
                    return false;
                }
            }
        }

        @Override
        public byte[] getBuffer() {
            return this.useFallthrough ? this.fallthrough.getBuffer() : null;
        }

        @Override
        public long size() {
            if (this.useFallthrough) {
                return this.fallthrough.size();
            } else if (this.file == null) {
                return 0L;
            } else {
                try {
                    return this.file.length();
                } catch (IOException var2) {
                    ExceptionLogger.logException(var2);
                    return 0L;
                }
            }
        }

        @Override
        public boolean seek(FileSeekMode mode, long pos) {
            if (this.useFallthrough) {
                return this.fallthrough.seek(mode, pos);
            } else if (this.file == null) {
                return false;
            } else {
                try {
                    this.file.seek(switch (mode) {
                        case CURRENT -> this.file.getFilePointer();
                        case END -> this.file.length();
                    });
                    return true;
                } catch (IOException var5) {
                    ExceptionLogger.logException(var5);
                    return false;
                }
            }
        }

        @Override
        public long pos() {
            if (this.useFallthrough) {
                return this.fallthrough.pos();
            } else if (this.file == null) {
                return 0L;
            } else {
                try {
                    return this.file.getFilePointer();
                } catch (IOException var2) {
                    ExceptionLogger.logException(var2);
                    return 0L;
                }
            }
        }

        @Override
        public InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public IFileDevice getDevice() {
            return this.device;
        }

        @Override
        public void release() {
            this.getDevice().destroyFile(this);
        }
    }
}
