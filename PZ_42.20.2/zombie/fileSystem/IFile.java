// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.InputStream;

public interface IFile {
    boolean open(String path, int mode);

    void close();

    boolean read(byte[] buffer, long size);

    boolean write(byte[] buffer, long size);

    byte[] getBuffer();

    long size();

    boolean seek(FileSeekMode mode, long pos);

    long pos();

    InputStream getInputStream();

    IFileDevice getDevice();

    void release();
}
