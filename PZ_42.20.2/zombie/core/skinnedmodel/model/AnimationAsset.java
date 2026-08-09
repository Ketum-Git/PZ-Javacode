// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.HashMap;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;

public final class AnimationAsset extends Asset {
    public HashMap<String, AnimationClip> animationClips;
    public AnimationAsset.AnimationAssetParams assetParams;
    public SkinningData skinningData;
    public String modelManagerKey;
    public ModelManager.ModAnimations modAnimations;
    public static final AssetType ASSET_TYPE = new AssetType("Animation");

    public AnimationAsset(AssetPath path, AssetManager manager, AnimationAsset.AnimationAssetParams params) {
        super(path, manager);
        this.assetParams = params;
    }

    protected void onLoadedX(ProcessedAiScene processedAiScene) {
        processedAiScene.applyToAnimation(this);
    }

    protected void onLoadedTxt(ModelTxt modelTxt) {
        ModelLoader.instance.applyToAnimation(modelTxt, this);
    }

    @Override
    public void onBeforeReady() {
        super.onBeforeReady();
        if (this.assetParams != null) {
            this.assetParams.animationsMesh = null;
            this.assetParams = null;
        }
    }

    @Override
    public void setAssetParams(AssetManager.AssetParams params) {
        this.assetParams = (AnimationAsset.AnimationAssetParams)params;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public static final class AnimationAssetParams extends AssetManager.AssetParams {
        public ModelMesh animationsMesh;
    }
}
