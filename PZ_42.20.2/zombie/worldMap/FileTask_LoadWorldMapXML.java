// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

public final class FileTask_LoadWorldMapXML extends FileTask {
    WorldMapData worldMapData;
    String filename;

    public FileTask_LoadWorldMapXML(WorldMapData worldMapData, String filename, FileSystem fileSystem, IFileTaskCallback cb) {
        super(fileSystem, cb);
        this.worldMapData = worldMapData;
        this.filename = filename;
    }

    @Override
    public String getErrorMessage() {
        return this.filename;
    }

    @Override
    public void done() {
        this.worldMapData = null;
        this.filename = null;
    }

    @Override
    public Object call() throws Exception {
        WorldMapXML reader = new WorldMapXML();
        return reader.read(this.filename, this.worldMapData) ? Boolean.TRUE : Boolean.FALSE;
    }
}
