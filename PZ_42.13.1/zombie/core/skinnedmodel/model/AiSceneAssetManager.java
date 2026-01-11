// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;
import java.util.EnumSet;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;

@Deprecated
public final class AiSceneAssetManager extends AssetManager {
    public static final AiSceneAssetManager instance = new AiSceneAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        FileSystem fs = asset.getAssetManager().getOwner().getFileSystem();
        FileTask fileTask = new AiSceneAssetManager.FileTask_LoadAiScene(
            asset.getPath().getPath(), ((AiSceneAsset)asset).postProcessStepSet, result -> this.onFileTaskFinished((AiSceneAsset)asset, result), fs
        );
        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    public void onFileTaskFinished(AiSceneAsset asset, Object result) {
        if (result instanceof AiScene aiScene) {
            asset.scene = aiScene;
            this.onLoadingSucceeded(asset);
        } else {
            this.onLoadingFailed(asset);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new AiSceneAsset(path, this, (AiSceneAsset.AiSceneAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    static class FileTask_LoadAiScene extends FileTask {
        String filename;
        EnumSet<AiPostProcessSteps> postProcessStepSet;

        public FileTask_LoadAiScene(String filename, EnumSet<AiPostProcessSteps> pps, IFileTaskCallback cb, FileSystem fileSystem) {
            super(fileSystem, cb);
            this.filename = filename;
            this.postProcessStepSet = pps;
        }

        @Override
        public String getErrorMessage() {
            return this.filename;
        }

        @Override
        public void done() {
            this.filename = null;
            this.postProcessStepSet = null;
        }

        @Override
        public Object call() throws Exception {
            return Jassimp.importFile(this.filename, this.postProcessStepSet);
        }
    }
}
