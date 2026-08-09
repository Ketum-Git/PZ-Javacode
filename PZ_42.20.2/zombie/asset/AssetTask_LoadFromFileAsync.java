// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import zombie.fileSystem.FileSystem;
import zombie.fileSystem.IFile;
import zombie.fileSystem.IFileTask2Callback;

final class AssetTask_LoadFromFileAsync extends AssetTask implements IFileTask2Callback {
    int asyncOp = -1;
    boolean stream;

    AssetTask_LoadFromFileAsync(Asset asset, boolean stream) {
        super(asset);
        this.stream = stream;
    }

    @Override
    public void execute() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        int mode = 4 | (this.stream ? 16 : 1);
        this.asyncOp = fs.openAsync(fs.getDefaultDevice(), this.asset.getPath().path, mode, this);
    }

    @Override
    public void cancel() {
        FileSystem fs = this.asset.getAssetManager().getOwner().getFileSystem();
        fs.cancelAsync(this.asyncOp);
        this.asyncOp = -1;
    }

    @Override
    public void onFileTaskFinished(IFile file, Object result) {
        this.asyncOp = -1;
        if (this.asset.priv.desiredState == Asset.State.READY) {
            if (result != Boolean.TRUE) {
                this.asset.priv.onLoadingFailed();
            } else if (!this.asset.getAssetManager().loadDataFromFile(this.asset, file)) {
                this.asset.priv.onLoadingFailed();
            } else {
                this.asset.priv.onLoadingSucceeded();
            }
        }
    }
}
