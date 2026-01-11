// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;

public interface IFileDevice {
    IFile createFile(IFile child);

    void destroyFile(IFile file);

    InputStream createStream(String path, InputStream child) throws IOException;

    void destroyStream(InputStream stream);

    String name();
}
