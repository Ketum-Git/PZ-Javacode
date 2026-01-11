// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.file.Files;
import java.nio.file.Paths;
import zombie.DebugFileWatcher;
import zombie.GameWindow;
import zombie.PredicatedFileWatcher;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.gameStates.IngameState;

public final class WorldMapDataAssetManager extends AssetManager {
    public static final WorldMapDataAssetManager instance = new WorldMapDataAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        WorldMapData worldMapData = (WorldMapData)asset;
        FileSystem fileSystem = this.getOwner().getFileSystem();
        String filePath = asset.getPath().getPath();
        FileTask fileTask;
        if (Files.exists(Paths.get(filePath + ".bin"))) {
            fileTask = new FileTask_LoadWorldMapBinary(worldMapData, filePath + ".bin", fileSystem, result -> this.loadCallback(worldMapData, result));
        } else {
            fileTask = new FileTask_LoadWorldMapXML(worldMapData, filePath, fileSystem, result -> this.loadCallback(worldMapData, result));
        }

        if (GameWindow.states.current == IngameState.instance) {
            fileTask.setPriority(4);
        } else {
            fileTask.setPriority(6);
        }

        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    private void loadCallback(WorldMapData worldMapData, Object result) {
        if (result == Boolean.TRUE) {
            worldMapData.onLoaded();
            this.onLoadingSucceeded(worldMapData);
        } else {
            DebugLog.General.warn("Failed to load asset: " + worldMapData.getPath());
            this.onLoadingFailed(worldMapData);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        WorldMapData worldMapData = new WorldMapData(path, this, params);
        DebugFileWatcher.instance.add(new PredicatedFileWatcher(path.getPath(), entryKey -> this.reload(worldMapData, params)));
        return worldMapData;
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
