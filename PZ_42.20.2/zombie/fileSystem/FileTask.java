// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

import java.util.concurrent.Callable;

public abstract class FileTask implements Callable<Object> {
    protected final FileSystem fileSystem;
    protected final IFileTaskCallback cb;
    protected int priority = 5;

    public FileTask(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.cb = null;
    }

    public FileTask(FileSystem fileSystem, IFileTaskCallback cb) {
        this.fileSystem = fileSystem;
        this.cb = cb;
    }

    public void handleResult(Object result) {
        if (this.cb != null) {
            this.cb.onFileTaskFinished(result);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public abstract void done();

    public String getErrorMessage() {
        return null;
    }
}
