// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.asset.FileTask_LoadImageData;
import zombie.core.opengl.RenderThread;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;

public final class AnimatedTextureIDAssetManager extends AssetManager {
    public static final AnimatedTextureIDAssetManager instance = new AnimatedTextureIDAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        FileSystem fs = this.getOwner().getFileSystem();
        FileTask fileTask = new FileTask_LoadImageData(asset.getPath().getPath(), fs, result -> this.onFileTaskFinished(asset, result));
        fileTask.setPriority(7);
        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    @Override
    protected void unloadData(Asset asset) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        if (!textureID.isDestroyed()) {
            RenderThread.invokeOnRenderContext(textureID::destroy);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new AnimatedTextureID(path, this, (AnimatedTextureID.AnimatedTextureIDAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private void onFileTaskFinished(Asset asset, Object result) {
        AnimatedTextureID textureID = (AnimatedTextureID)asset;
        if (result instanceof ImageData imageData) {
            textureID.setImageData(imageData);
            this.onLoadingSucceeded(asset);
        } else {
            this.onLoadingFailed(asset);
        }
    }
}
