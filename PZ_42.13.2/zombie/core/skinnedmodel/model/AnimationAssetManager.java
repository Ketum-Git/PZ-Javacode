// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;

public final class AnimationAssetManager extends AssetManager {
    public static final AnimationAssetManager instance = new AnimationAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        AnimationAsset anim = (AnimationAsset)asset;
        FileSystem fileSystem = this.getOwner().getFileSystem();
        FileTask fileTask = new FileTask_LoadAnimation(anim, fileSystem, result -> this.loadCallback(anim, result));
        fileTask.setPriority(4);
        String fileName = asset.getPath().getPath().toLowerCase();
        if (fileName.endsWith("bob_idle") || fileName.endsWith("bob_walk") || fileName.endsWith("bob_run")) {
            fileTask.setPriority(6);
        }

        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    private void loadCallback(AnimationAsset anim, Object result) {
        if (result instanceof ProcessedAiScene processedAiScene) {
            anim.onLoadedX(processedAiScene);
            this.onLoadingSucceeded(anim);
            ModelManager.instance.animationAssetLoaded(anim);
        } else if (result instanceof ModelTxt modelTxt) {
            anim.onLoadedTxt(modelTxt);
            this.onLoadingSucceeded(anim);
            ModelManager.instance.animationAssetLoaded(anim);
        } else {
            DebugLog.General.warn("Failed to load asset: " + anim.getPath());
            this.onLoadingFailed(anim);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new AnimationAsset(path, this, (AnimationAsset.AnimationAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
