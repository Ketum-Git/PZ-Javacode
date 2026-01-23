// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

public final class ImageDataFrame {
    public ImageData owner;
    public int widthHw;
    public int heightHw;
    public MipMapLevel data;
    public final APNGFrame apngFrame = new APNGFrame();

    public ImageDataFrame set(ImageData owner1, APNGFrame apngFrame1) {
        this.owner = owner1;
        boolean bFullCompositedFrame = true;
        this.widthHw = owner1.getWidthHW();
        this.heightHw = owner1.getHeightHW();
        this.data = new MipMapLevel(this.widthHw, this.heightHw);
        this.apngFrame.set(apngFrame1);
        return this;
    }
}
