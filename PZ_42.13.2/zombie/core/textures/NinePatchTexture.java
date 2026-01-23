// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.utils.DirectBufferAllocator;
import zombie.core.utils.ImageUtils;
import zombie.core.utils.WrappedBuffer;

@UsedFromLua
public class NinePatchTexture extends Asset {
    public static final AssetType ASSET_TYPE = new AssetType("NineGridTexture");
    public static final int TOP_LEFT = 0;
    public static final int TOP_MIDDLE = 1;
    public static final int TOP_RIGHT = 2;
    public static final int MIDDLE_LEFT = 3;
    public static final int MIDDLE_CENTER = 4;
    public static final int MIDDLE_RIGHT = 5;
    public static final int BOTTOM_LEFT = 6;
    public static final int BOTTOM_MIDDLE = 7;
    public static final int BOTTOM_RIGHT = 8;
    private static final HashMap<String, NinePatchTexture> s_sharedTextures = new HashMap<>();
    private static final HashSet<String> s_nullTextures = new HashSet<>();
    private static float red;
    private static float green;
    private static float blue;
    private static float alpha;
    private final int[] widths = new int[9];
    private final int[] heights = new int[9];
    private TextureID textureId;
    private Texture texture;

    public static NinePatchTexture getSharedTexture(String path) {
        if (s_nullTextures.contains(path)) {
            return null;
        } else {
            NinePatchTexture npt = s_sharedTextures.get(path);
            if (npt != null) {
                return npt;
            } else {
                String fullPath = ZomboidFileSystem.instance.getString(path);
                boolean bExists = fullPath != path;
                if (!bExists && !new File(path).exists()) {
                    s_nullTextures.add(path);
                    return null;
                } else {
                    npt = (NinePatchTexture)NinePatchTextureAssetManager.instance.load(new AssetPath(fullPath));
                    if (npt == null) {
                        s_nullTextures.add(path);
                    } else {
                        s_sharedTextures.put(path, npt);
                    }

                    return null;
                }
            }
        }
    }

    public static void onTexturePacksChanged() {
        s_nullTextures.clear();
        s_sharedTextures.clear();
    }

    public static void Reset() {
        s_nullTextures.clear();
    }

    protected NinePatchTexture(AssetPath path, AssetManager manager) {
        super(path, manager);
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    protected void unloadData() {
        this.textureId.getAssetManager().unloadWithoutDeref(this.textureId);
    }

    public int getColumnWidth(int column) {
        return switch (column) {
            case 0 -> PZMath.max(this.widths[0], this.widths[3], this.widths[6]);
            case 1 -> PZMath.max(this.widths[1], this.widths[4], this.widths[7]);
            case 2 -> PZMath.max(this.widths[2], this.widths[5], this.widths[8]);
            default -> throw new IllegalArgumentException("invalid column, must be 0,1 or 2");
        };
    }

    public int getRowHeight(int row) {
        return switch (row) {
            case 0 -> PZMath.max(this.heights[0], this.heights[1], this.heights[2]);
            case 1 -> PZMath.max(this.heights[3], this.heights[4], this.heights[5]);
            case 2 -> PZMath.max(this.heights[6], this.heights[7], this.heights[8]);
            default -> throw new IllegalArgumentException("invalid row, must be 0,1 or 2");
        };
    }

    public int getMinWidth() {
        return this.getColumnWidth(0) + this.getColumnWidth(1) + this.getColumnWidth(2);
    }

    public int getMinHeight() {
        return this.getRowHeight(0) + this.getRowHeight(1) + this.getRowHeight(2);
    }

    public boolean hasTopRow() {
        return this.heights[0] > 0;
    }

    public boolean hasBottomRow() {
        return this.heights[6] > 0;
    }

    public boolean hasLeftColumn() {
        return this.widths[0] > 0;
    }

    public boolean hasRightColumn() {
        return this.widths[2] > 0;
    }

    public boolean is9x9() {
        return this.hasTopRow() && this.hasBottomRow() && this.hasLeftColumn() && this.hasRightColumn();
    }

    public boolean is3x1() {
        return !this.hasTopRow() && !this.hasBottomRow() && this.hasLeftColumn() && this.hasRightColumn();
    }

    public boolean is1x3() {
        return this.hasTopRow() && this.hasBottomRow() && !this.hasLeftColumn() && !this.hasRightColumn();
    }

    public void render(float x, float y, float width, float height) {
        this.render(x, y, width, height, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void render(float x, float y, float width, float height, float r, float g, float b, float a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
        this.renderPatch(0, 0, this.widths[0], this.heights[0], x, y, this.widths[0], this.heights[0]);
        this.renderPatch(
            this.widths[0], 0, this.widths[1], this.heights[1], x + this.widths[0], y, width - this.widths[0] - this.widths[2], this.heights[1], true, false
        );
        this.renderPatch(
            this.texture.getWidth() - this.widths[2], 0, this.widths[2], this.heights[2], x + width - this.widths[2], y, this.widths[2], this.heights[2]
        );
        this.renderPatch(
            0,
            this.heights[0],
            this.widths[3],
            this.heights[3],
            x,
            y + this.heights[0],
            this.widths[3],
            height - this.heights[0] - this.heights[6],
            false,
            true
        );
        this.renderPatch(
            this.widths[3],
            this.heights[1],
            this.widths[4],
            this.heights[4],
            x + this.widths[3],
            y + this.heights[1],
            width - this.widths[3] - this.widths[5],
            height - this.heights[1] - this.heights[7],
            true,
            true
        );
        this.renderPatch(
            this.texture.getWidth() - this.widths[5],
            this.heights[2],
            this.widths[5],
            this.heights[5],
            x + width - this.widths[5],
            y + this.heights[2],
            this.widths[5],
            height - this.heights[2] - this.heights[8],
            false,
            true
        );
        this.renderPatch(
            0, this.texture.getHeight() - this.heights[6], this.widths[6], this.heights[6], x, y + height - this.heights[6], this.widths[6], this.heights[6]
        );
        this.renderPatch(
            this.widths[6],
            this.texture.getHeight() - this.heights[6],
            this.widths[7],
            this.heights[7],
            x + this.widths[6],
            y + height - this.heights[7],
            width - this.widths[6] - this.widths[8],
            this.heights[7],
            true,
            false
        );
        this.renderPatch(
            this.texture.getWidth() - this.widths[8],
            this.texture.getHeight() - this.heights[8],
            this.widths[8],
            this.heights[8],
            x + width - this.widths[8],
            y + height - this.heights[8],
            this.widths[8],
            this.heights[8]
        );
    }

    private void renderPatch(int xSrc, int ySrc, int widthSrc, int heightSrc, float x, float y, float width, float height) {
        this.renderPatch(xSrc, ySrc, widthSrc, heightSrc, x, y, width, height, false, false);
    }

    private void renderPatch(
        int xSrc, int ySrc, int widthSrc, int heightSrc, float x, float y, float width, float height, boolean isStretchW, boolean isStretchH
    ) {
        if (!(width <= 0.0F) && !(height <= 0.0F)) {
            float u1 = (float)xSrc / this.texture.getWidthHW();
            float v1 = (float)ySrc / this.texture.getHeightHW();
            float u2 = (float)(xSrc + widthSrc) / this.texture.getWidthHW();
            float v2 = (float)ySrc / this.texture.getHeightHW();
            float u3 = (float)(xSrc + widthSrc) / this.texture.getWidthHW();
            float v3 = (float)(ySrc + heightSrc) / this.texture.getHeightHW();
            float u4 = (float)xSrc / this.texture.getWidthHW();
            float v4 = (float)(ySrc + heightSrc) / this.texture.getHeightHW();
            if (this.textureId.getMinFilter() == 9729 || this.textureId.getMagFilter() == 9729) {
                if (isStretchW) {
                    float halfTexelWidth = 0.5F / this.texture.getWidthHW();
                    u1 += halfTexelWidth;
                    u2 -= halfTexelWidth;
                    u3 -= halfTexelWidth;
                    u4 += halfTexelWidth;
                }

                if (isStretchH) {
                    float halfTexelHeight = 0.5F / this.texture.getWidthHW();
                    v1 += halfTexelHeight;
                    v2 += halfTexelHeight;
                    v3 -= halfTexelHeight;
                    v4 -= halfTexelHeight;
                }
            }

            SpriteRenderer.instance.render(this.texture, x, y, width, height, red, green, blue, alpha, u1, v1, u2, v2, u3, v3, u4, v4);
        }
    }

    public void setImageData(ImageData imageData) {
        MipMapLevel data = imageData.getData();
        ByteBuffer bb = data.getBuffer();
        int BPP = 4;
        int INSET = 1;
        int ALPHA_THRESHOLD = 128;
        int start = 0;
        int end = imageData.getWidth();

        for (int x = 0; x < imageData.getWidth(); x++) {
            int alpha = bb.get(x * 4 + 3) & 255;
            if (alpha < 128) {
                if (start != 0) {
                    end = x;
                    break;
                }
            } else if (start == 0) {
                start = x;
            }
        }

        start = PZMath.max(start - 1, 0);
        end = PZMath.max(end - 1, 0);
        this.widths[0] = this.widths[3] = this.widths[6] = start;
        this.widths[1] = this.widths[4] = this.widths[7] = end - start;
        this.widths[2] = this.widths[5] = this.widths[8] = imageData.getWidth() - 1 - end;
        start = 0;
        end = imageData.getHeight();

        for (int y = 0; y < imageData.getHeight(); y++) {
            int alpha = bb.get(3 + y * 4 * data.width) & 255;
            if (alpha < 128) {
                if (start != 0) {
                    end = y;
                    break;
                }
            } else if (start == 0) {
                start = y;
            }
        }

        start = PZMath.max(start - 1, 0);
        end = PZMath.max(end - 1, 0);
        this.heights[0] = this.heights[1] = this.heights[2] = start;
        this.heights[3] = this.heights[4] = this.heights[5] = end - start;
        this.heights[6] = this.heights[7] = this.heights[8] = imageData.getHeight() - 1 - end;
        int width = imageData.getWidth() - 1;
        int height = imageData.getHeight() - 1;
        int widthHW = ImageUtils.getNextPowerOfTwoHW(width);
        int heightHW = ImageUtils.getNextPowerOfTwoHW(height);
        WrappedBuffer wrappedBuffer = DirectBufferAllocator.allocate(widthHW * 4 * heightHW);
        ByteBuffer bbNew = wrappedBuffer.getBuffer();

        for (int yx = 1; yx < imageData.getHeight(); yx++) {
            for (int xx = 1; xx < imageData.getWidth(); xx++) {
                bbNew.putInt((xx - 1) * 4 + (yx - 1) * widthHW * 4, bb.getInt(xx * 4 + yx * data.width * 4));
            }
        }

        ImageData imageData1 = new ImageData(width, height, wrappedBuffer);
        if (this.textureId == null) {
            TextureID.TextureIDAssetParams tap = new TextureID.TextureIDAssetParams();
            tap.flags = 3;
            this.textureId = new TextureID(this.getPath(), TextureIDAssetManager.instance, tap);
            this.textureId.setImageData(imageData1);
            this.textureId.onCreated(Asset.State.READY);
            this.texture = new Texture(this.textureId, this.getPath().getPath());
            this.addDependency(this.texture);
        } else {
            this.textureId.setImageData(imageData1);
            this.textureId.getAssetManager().onDataReloaded(this.textureId);
        }
    }
}
