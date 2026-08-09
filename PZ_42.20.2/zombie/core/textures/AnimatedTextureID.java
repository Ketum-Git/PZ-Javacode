// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.util.ArrayList;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;

public class AnimatedTextureID extends Asset {
    public static final AssetType ASSET_TYPE = new AssetType("AnimatedTextureID");
    private int width;
    private int height;
    public final ArrayList<AnimatedTextureIDFrame> frames = new ArrayList<>();
    public AnimatedTextureID.AnimatedTextureIDAssetParams assetParams;

    protected AnimatedTextureID(AssetPath path, AssetManager manager, AnimatedTextureID.AnimatedTextureIDAssetParams params) {
        super(path, manager);
        this.assetParams = params;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public void setImageData(ImageData data) {
        this.width = data.getWidth();
        this.height = data.getHeight();

        for (int i = 0; i < data.frames.size(); i++) {
            ImageDataFrame frame = data.frames.get(i);
            ImageData imageData = new ImageData(frame);
            frame.data = null;
            TextureID textureID1 = new TextureID(imageData);
            AnimatedTextureIDFrame frame1 = new AnimatedTextureIDFrame();
            frame1.textureId = textureID1;
            frame1.apngFrame = frame.apngFrame;
            this.frames.add(frame1);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getFrameCount() {
        return this.frames.size();
    }

    public AnimatedTextureIDFrame getFrame(int index) {
        return index >= 0 && index < this.frames.size() ? this.frames.get(index) : null;
    }

    public boolean isDestroyed() {
        return false;
    }

    public void destroy() {
    }

    public static final class AnimatedTextureIDAssetParams extends AssetManager.AssetParams {
        int flags = 0;
    }
}
