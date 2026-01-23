// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import java.util.HashSet;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.util.StringUtils;

public final class PhysicsShapeAssetManager extends AssetManager {
    public static final PhysicsShapeAssetManager instance = new PhysicsShapeAssetManager();
    private final HashSet<String> watchedFiles = new HashSet<>();
    private final PredicatedFileWatcher watcher = new PredicatedFileWatcher(PhysicsShapeAssetManager::isWatched, PhysicsShapeAssetManager::watchedFileChanged);

    private PhysicsShapeAssetManager() {
        DebugFileWatcher.instance.add(this.watcher);
    }

    @Override
    protected void startLoading(Asset asset) {
        PhysicsShape physicsShape = (PhysicsShape)asset;
        FileSystem fileSystem = this.getOwner().getFileSystem();
        FileTask fileTask = new FileTask_LoadPhysicsShape(physicsShape, fileSystem, result -> this.loadCallback(physicsShape, result));
        fileTask.setPriority(6);
        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    private void loadCallback(PhysicsShape physicsShape, Object result) {
        if (result instanceof ProcessedAiScene processedAiScene) {
            physicsShape.onLoadedX(processedAiScene);
            this.onLoadingSucceeded(physicsShape);
        } else {
            DebugLog.General.warn("Failed to load asset: " + physicsShape.getPath());
            this.onLoadingFailed(physicsShape);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new PhysicsShape(path, this, (PhysicsShape.PhysicsShapeAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private static boolean isWatched(String entryKey) {
        if (!StringUtils.endsWithIgnoreCase(entryKey, ".fbx")
            && !StringUtils.endsWithIgnoreCase(entryKey, ".glb")
            && !StringUtils.endsWithIgnoreCase(entryKey, ".x")) {
            return false;
        } else {
            String fullPath = ZomboidFileSystem.instance.getString(entryKey);
            return instance.watchedFiles.contains(fullPath);
        }
    }

    private static void watchedFileChanged(String entryKey) {
        DebugLog.Asset.printf("%s changed\n", entryKey);
        String fullPath = ZomboidFileSystem.instance.getString(entryKey);
        instance.getAssetTable().forEachValue(asset -> {
            PhysicsShape physicsShape = (PhysicsShape)asset;
            if (!physicsShape.isEmpty() && fullPath.equalsIgnoreCase(physicsShape.fullPath)) {
                PhysicsShape.PhysicsShapeAssetParams assetParams = new PhysicsShape.PhysicsShapeAssetParams();
                assetParams.postProcess = physicsShape.postProcess;
                assetParams.allMeshes = physicsShape.allMeshes;
                instance.reload(asset, assetParams);
            }

            return true;
        });
    }

    public void addWatchedFile(String fileName) {
        this.watchedFiles.add(fileName);
    }
}
