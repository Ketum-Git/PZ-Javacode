// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;

public class AnimNodeAssetManager extends AssetManager {
    public static final AnimNodeAssetManager instance = new AnimNodeAssetManager();

    @Override
    protected void startLoading(Asset asset) {
        AnimNodeAsset animNodeAsset = (AnimNodeAsset)asset;
        animNodeAsset.animNode = AnimNode.Parse(asset.getPath().getPath());
        if (animNodeAsset.animNode == null) {
            this.onLoadingFailed(asset);
        } else {
            this.onLoadingSucceeded(asset);
        }
    }

    @Override
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
        super.onStateChanged(oldState, newState, asset);
        if (newState == Asset.State.READY) {
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new AnimNodeAsset(path, this);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
