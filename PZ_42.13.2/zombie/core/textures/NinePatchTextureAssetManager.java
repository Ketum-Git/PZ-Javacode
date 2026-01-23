// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class NinePatchTextureAssetManager extends AssetManager {
    public static final NinePatchTextureAssetManager instance = new NinePatchTextureAssetManager();
    private final HashSet<String> watchedFiles = new HashSet<>();
    private final PredicatedFileWatcher watcher = new PredicatedFileWatcher(
        NinePatchTextureAssetManager::isWatched, NinePatchTextureAssetManager::watchedFileChanged
    );

    private NinePatchTextureAssetManager() {
        DebugFileWatcher.instance.add(this.watcher);
    }

    @Override
    protected void startLoading(Asset asset) {
        NinePatchTexture npt = (NinePatchTexture)asset;

        try (
            InputStream input = new FileInputStream(asset.getPath().getPath());
            BufferedInputStream bis = new BufferedInputStream(input);
        ) {
            ImageData imageData = new ImageData(bis, false);
            this.onFileTaskFinished(asset, imageData);
        } catch (Exception var11) {
            this.onFileTaskFinished(asset, null);
        }
    }

    @Override
    protected void unloadData(Asset asset) {
        NinePatchTexture npt = (NinePatchTexture)asset;
        npt.unloadData();
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new NinePatchTexture(path, this);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private void onFileTaskFinished(Asset asset, Object result) {
        NinePatchTexture npt = (NinePatchTexture)asset;
        if (result instanceof ImageData imageData) {
            npt.setImageData(imageData);
            imageData.dispose();
            this.onLoadingSucceeded(asset);
            this.addWatchedFile(asset.getPath().getPath());
        } else {
            this.onLoadingFailed(asset);
        }
    }

    private static boolean isWatched(String entryKey) {
        if (!StringUtils.endsWithIgnoreCase(entryKey, ".png")) {
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
            NinePatchTexture npt = (NinePatchTexture)asset;
            if (!npt.isEmpty() && fullPath.equalsIgnoreCase(npt.getPath().getPath())) {
                instance.reload(asset, null);
            }

            return true;
        });
    }

    public void addWatchedFile(String fileName) {
        this.watchedFiles.add(fileName);
    }
}
