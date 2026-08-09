// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;

public final class TextureAssetManager extends AssetManager {
    public static final TextureAssetManager instance = new TextureAssetManager();

    @Override
    protected void startLoading(Asset asset) {
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new Texture(path, this, (Texture.TextureAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }
}
