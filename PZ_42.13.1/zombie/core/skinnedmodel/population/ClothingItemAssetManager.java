// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.asset.FileTask_ParseXML;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.util.list.PZArrayUtil;

public class ClothingItemAssetManager extends AssetManager {
    public static final ClothingItemAssetManager instance = new ClothingItemAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        FileSystem fs = asset.getAssetManager().getOwner().getFileSystem();
        FileTask fileTask = new FileTask_ParseXML(
            ClothingItemXML.class, asset.getPath().getPath(), result -> this.onFileTaskFinished((ClothingItem)asset, result), fs
        );
        AssetTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        assetTask.execute();
    }

    private void onFileTaskFinished(ClothingItem asset, Object result) {
        if (result instanceof ClothingItemXML xml) {
            asset.maleModel = this.fixPath(xml.maleModel);
            asset.femaleModel = this.fixPath(xml.femaleModel);
            asset.altMaleModel = this.fixPath(xml.altMaleModel);
            asset.altFemaleModel = this.fixPath(xml.altFemaleModel);
            asset.isStatic = xml.isStatic;
            PZArrayUtil.arrayCopy(asset.baseTextures, this.fixPaths(xml.baseTextures));
            asset.attachBone = xml.attachBone;
            PZArrayUtil.arrayCopy(asset.masks, xml.masks);
            asset.masksFolder = this.fixPath(xml.masksFolder);
            asset.underlayMasksFolder = this.fixPath(xml.underlayMasksFolder);
            PZArrayUtil.arrayCopy(asset.textureChoices, this.fixPaths(xml.textureChoices));
            asset.allowRandomHue = xml.allowRandomHue;
            asset.allowRandomTint = xml.allowRandomTint;
            asset.decalGroup = xml.decalGroup;
            asset.shader = xml.shader;
            asset.hatCategory = xml.hatCategory;
            PZArrayUtil.arrayCopy(asset.spawnWith, this.fixPaths(xml.spawnWith));
            this.onLoadingSucceeded(asset);
        } else {
            this.onLoadingFailed(asset);
        }
    }

    private String fixPath(String path) {
        return path == null ? null : path.replaceAll("\\\\", "/");
    }

    private ArrayList<String> fixPaths(ArrayList<String> paths) {
        if (paths == null) {
            return null;
        } else {
            for (int i = 0; i < paths.size(); i++) {
                paths.set(i, this.fixPath(paths.get(i)));
            }

            return paths;
        }
    }

    @Override
    public void onStateChanged(Asset.State old_state, Asset.State new_state, Asset asset) {
        super.onStateChanged(old_state, new_state, asset);
        if (new_state == Asset.State.READY) {
            OutfitManager.instance.onClothingItemStateChanged((ClothingItem)asset);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new ClothingItem(path, this);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
