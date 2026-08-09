// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetPath;

public final class AnimatedTextures {
    private static final HashMap<String, AnimatedTexture> textures = new HashMap<>();

    public static AnimatedTexture getTexture(String relativePath) {
        String path = ZomboidFileSystem.instance.getString(relativePath);
        if (textures.containsKey(path)) {
            return textures.get(path);
        } else {
            AnimatedTextureID.AnimatedTextureIDAssetParams params = null;
            AnimatedTextureID asset = (AnimatedTextureID)AnimatedTextureIDAssetManager.instance.load(new AssetPath(path), params);
            AnimatedTexture animatedTexture = new AnimatedTexture(asset);
            textures.put(path, animatedTexture);
            return animatedTexture;
        }
    }
}
