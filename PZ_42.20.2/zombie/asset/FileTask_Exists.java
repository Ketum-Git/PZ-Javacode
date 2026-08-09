// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import java.io.File;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_Exists extends FileTask {
    String fileName;

    public FileTask_Exists(String fileName, IFileTaskCallback cb, FileSystem fileSystem) {
        super(fileSystem, cb);
        this.fileName = fileName;
    }

    @Override
    public void done() {
    }

    @Override
    public Object call() throws Exception {
        return new File(this.fileName).exists();
    }
}
