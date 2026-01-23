// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

public final class APNGFrame {
    public static final int APNG_DISPOSE_OP_NONE = 0;
    public static final int APNG_DISPOSE_OP_BACKGROUND = 1;
    public static final int APNG_DISPOSE_OP_PREVIOUS = 2;
    public static final int APNG_BLEND_OP_SOURCE = 0;
    public static final int APNG_BLEND_OP_OVER = 1;
    int sequenceNumber;
    int width;
    int height;
    int xOffset;
    int yOffset;
    short delayNum;
    short delayDen;
    byte disposeOp;
    byte blendOp;

    APNGFrame set(APNGFrame other) {
        this.sequenceNumber = other.sequenceNumber;
        this.width = other.width;
        this.height = other.height;
        this.xOffset = other.xOffset;
        this.yOffset = other.yOffset;
        this.delayNum = other.delayNum;
        this.delayDen = other.delayDen;
        this.disposeOp = other.disposeOp;
        this.blendOp = other.blendOp;
        return this;
    }
}
