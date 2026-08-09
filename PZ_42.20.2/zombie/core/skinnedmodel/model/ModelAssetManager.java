// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;

public final class ModelAssetManager extends AssetManager {
    public static final ModelAssetManager instance = new ModelAssetManager();

    @Override
    protected void startLoading(Asset asset) {
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new Model(path, this, (Model.ModelAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
