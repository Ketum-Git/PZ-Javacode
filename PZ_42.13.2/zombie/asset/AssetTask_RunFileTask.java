// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;

public final class AssetTask_RunFileTask extends AssetTask {
    protected final FileTask fileTask;
    int asyncOp = -1;

    public AssetTask_RunFileTask(FileTask fileTask, Asset asset) {
        super(asset);
        this.fileTask = fileTask;
    }

    @Override
    public void execute() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        this.asyncOp = fs.runAsync(this.fileTask);
    }

    @Override
    public void cancel() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        fs.cancelAsync(this.asyncOp);
        this.asyncOp = -1;
    }
}
