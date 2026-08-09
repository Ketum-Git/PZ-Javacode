// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;
import zombie.util.PZXmlUtil;

public final class FileTask_ParseXML extends FileTask {
    Class<? extends Object> clazz;
    String filename;

    public FileTask_ParseXML(Class<? extends Object> klass, String filename, IFileTaskCallback cb, FileSystem fileSystem) {
        super(fileSystem, cb);
        this.clazz = klass;
        this.filename = filename;
    }

    @Override
    public String getErrorMessage() {
        return this.filename;
    }

    @Override
    public void done() {
        this.clazz = null;
        this.filename = null;
    }

    @Override
    public Object call() throws Exception {
        return PZXmlUtil.parse(this.clazz, this.filename);
    }
}
