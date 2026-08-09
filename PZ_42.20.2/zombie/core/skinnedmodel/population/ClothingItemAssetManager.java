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

    private void onFileTaskFinished(ClothingItem clothingItem, Object result) {
        if (result instanceof ClothingItemXML xml) {
            clothingItem.maleModel = this.fixPath(xml.maleModel);
            clothingItem.femaleModel = this.fixPath(xml.femaleModel);
            clothingItem.altMaleModel = this.fixPath(xml.altMaleModel);
            clothingItem.altFemaleModel = this.fixPath(xml.altFemaleModel);
            clothingItem.isStatic = xml.isStatic;
            PZArrayUtil.arrayCopy(clothingItem.baseTextures, this.fixPaths(xml.baseTextures));
            clothingItem.attachBone = xml.attachBone;
            PZArrayUtil.arrayCopy(clothingItem.masks, xml.masks);
            clothingItem.masksFolder = this.fixPath(xml.masksFolder);
            clothingItem.underlayMasksFolder = this.fixPath(xml.underlayMasksFolder);
            PZArrayUtil.arrayCopy(clothingItem.textureChoices, this.fixPaths(xml.textureChoices));
            clothingItem.allowRandomHue = xml.allowRandomHue;
            clothingItem.allowRandomTint = xml.allowRandomTint;
            clothingItem.decalGroup = xml.decalGroup;
            clothingItem.shader = xml.shader;
            clothingItem.hatCategory = xml.hatCategory;
            PZArrayUtil.arrayCopy(clothingItem.spawnWith, this.fixPaths(xml.spawnWith));
            this.onLoadingSucceeded(clothingItem);
        } else {
            this.onLoadingFailed(clothingItem);
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
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
        super.onStateChanged(oldState, newState, asset);
        if (newState == Asset.State.READY) {
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
