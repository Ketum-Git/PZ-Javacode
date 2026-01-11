// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import java.util.EnumSet;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;

@Deprecated
public final class AiSceneAsset extends Asset {
    AiScene scene;
    EnumSet<AiPostProcessSteps> postProcessStepSet;
    AiSceneAsset.AiSceneAssetParams assetParams;
    public static final AssetType ASSET_TYPE = new AssetType("AiScene");

    protected AiSceneAsset(AssetPath path, AssetManager manager, AiSceneAsset.AiSceneAssetParams params) {
        super(path, manager);
        this.assetParams = params;
        this.scene = null;
        this.postProcessStepSet = params.postProcessStepSet;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public static final class AiSceneAssetParams extends AssetManager.AssetParams {
        EnumSet<AiPostProcessSteps> postProcessStepSet;
    }
}
