// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.vehicles.UI3DScene;

@UsedFromLua
public final class TileDepthTexture {
    private final TilesetDepthTexture tileset;
    private final int index;
    private final int width;
    private final int height;
    private float[] pixels;
    private final String name;
    private Texture texture;
    private boolean empty = true;
    private static final float[] s_clampedPixels = new float[8192];
    private static boolean clampedPixelsInit;
    static TileGeometryFile.Polygon floorPolygon;

    public TileDepthTexture(TilesetDepthTexture tileset, int tileIndex) {
        this.tileset = tileset;
        this.index = tileIndex;
        this.width = tileset.getTileWidth();
        this.height = tileset.getTileHeight();
        this.name = this.tileset.getName() + "_" + tileIndex;
    }

    public TilesetDepthTexture getTileset() {
        return this.tileset;
    }

    public int getIndex() {
        return this.index;
    }

    public int getColumn() {
        return this.getIndex() % this.tileset.getColumns();
    }

    public int getRow() {
        return this.getIndex() / this.tileset.getColumns();
    }

    public String getName() {
        return this.name;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public float[] getPixels() {
        return this.pixels;
    }

    public void setPixel(int x, int y, float pixel) {
        this.allocPixelsIfNeeded();
        this.pixels[this.index(x, y)] = pixel;
    }

    public float getPixel(int x, int y) {
        return this.pixels == null ? -1.0F : this.pixels[this.index(x, y)];
    }

    public void setMinPixel(int x, int y, float pixel) {
        int index1 = this.index(x, y);
        this.allocPixelsIfNeeded();
        this.pixels[index1] = PZMath.min(this.pixels[index1], pixel);
    }

    public void setPixels(int x, int y, int w, int h, float pixel) {
        if (w > 0 && h > 0) {
            int x2 = x + w - 1;
            int y2 = y + h - 1;
            int x1 = PZMath.clamp(x, 0, this.getWidth() - 1);
            int y1 = PZMath.clamp(y, 0, this.getHeight() - 1);
            x2 = PZMath.clamp(x2, 0, this.getWidth() - 1);
            y2 = PZMath.clamp(y2, 0, this.getHeight() - 1);

            for (int var11 = y1; var11 <= y2; var11++) {
                for (int var10 = x1; var10 <= x2; var10++) {
                    this.setPixel(var10, var11, pixel);
                }
            }
        }
    }

    public void replacePixels(int x, int y, int w, int h, float oldPixel, float newPixel) {
        if (w > 0 && h > 0) {
            int x2 = x + w - 1;
            int y2 = y + h - 1;
            int x1 = PZMath.clamp(x, 0, this.getWidth() - 1);
            int y1 = PZMath.clamp(y, 0, this.getHeight() - 1);
            x2 = PZMath.clamp(x2, 0, this.getWidth() - 1);
            y2 = PZMath.clamp(y2, 0, this.getHeight() - 1);

            for (int var12 = y1; var12 <= y2; var12++) {
                for (int var11 = x1; var11 <= x2; var11++) {
                    if (this.getPixel(var11, var12) == oldPixel) {
                        this.setPixel(var11, var12, newPixel);
                    }
                }
            }
        }
    }

    public int index(int x, int y) {
        return x + y * this.width;
    }

    void allocPixelsIfNeeded() {
        if (this.pixels == null) {
            this.pixels = new float[this.width * this.height];
            Arrays.fill(this.pixels, -1.0F);
        }
    }

    @Deprecated
    void load(float[] s_pixels, BufferedImage bufferedImage, int left, int top) {
        this.empty = true;

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                int argb = bufferedImage.getRGB(left + x, top + y);
                int a = argb >> 24 & 0xFF;
                int b = argb & 0xFF;
                s_pixels[x + y * this.width] = a == 0 ? -1.0F : b / 255.0F;
                if (this.empty && a != 0) {
                    this.empty = false;
                }
            }
        }

        if (this.empty) {
            this.pixels = null;
        } else {
            this.allocPixelsIfNeeded();
            System.arraycopy(s_pixels, 0, this.pixels, 0, this.pixels.length);
            if (TileDepthTextureManager.getInstance().isLoadingFinished()) {
                IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(this.getName());
                if (sprite != null) {
                    sprite.depthTexture = this;
                }
            }
        }

        this.updateGPUTexture();
    }

    void load(float[] s_pixels, ByteBuffer bb, int stride, int left, int top) {
        int bytesPerPixel = 4;
        this.empty = true;

        for (int y = 0; y < this.height; y++) {
            int bbRowStart = (top + y) * stride;

            for (int x = 0; x < this.width; x++) {
                int bbPixelStart = bbRowStart + (left + x) * 4;
                int a = bb.get(bbPixelStart + 3) & 255;
                int b = bb.get(bbPixelStart + 2) & 255;
                s_pixels[x + y * this.width] = a == 0 ? -1.0F : b / 255.0F;
                if (this.empty && a != 0) {
                    this.empty = false;
                }
            }
        }

        if (this.empty) {
            this.pixels = null;
        } else {
            this.allocPixelsIfNeeded();
            System.arraycopy(s_pixels, 0, this.pixels, 0, this.pixels.length);
            if (TileDepthTextureManager.getInstance().isLoadingFinished()) {
                IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(this.getName());
                if (sprite != null) {
                    sprite.depthTexture = this;
                }
            }

            this.updateGPUTexture();
        }
    }

    BufferedImage setBufferedImage(BufferedImage bufferedImage, int left, int top) {
        int[] rowARGB = new int[this.width];

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                float pixel = this.getPixel(x, y);
                if (pixel >= 0.0F) {
                    pixel = PZMath.min(pixel, 1.0F);
                    int rgb = (int)Math.floor(pixel * 255.0F) & 0xFF;
                    int a = 255;
                    rowARGB[x] = 0xFF000000 | rgb << 16 | rgb << 8 | rgb;
                } else {
                    rowARGB[x] = 0;
                }
            }

            bufferedImage.setRGB(left, top + y, this.width, 1, rowARGB, 0, this.width);
        }

        return bufferedImage;
    }

    private float clampPixelToUpperFloor(int x, int y, float pixel) {
        initClampedPixels();
        return PZMath.max(pixel, s_clampedPixels[x + y * 128]);
    }

    private static void initClampedPixels() {
        if (!clampedPixelsInit) {
            clampedPixelsInit = true;
            float TOP = 2.44949F;
            float PUSH_TOP_DOWN = 0.011764706F;
            Vector3f planePoint = new Vector3f(0.0F, 2.44949F, 0.0F);

            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 128; x++) {
                    float pixel = TileGeometryUtils.getNormalizedDepthOnPlaneAt(x + 0.5F, y + 0.5F, UI3DScene.GridPlane.XZ, planePoint);
                    if (pixel >= 0.0F) {
                        pixel += 0.011764706F;
                    }

                    s_clampedPixels[x + y * 128] = pixel;
                }
            }
        }
    }

    public void save() throws Exception {
        this.getTileset().save();
    }

    public boolean fileExists() {
        return this.getTileset().fileExists();
    }

    public Texture getTexture() {
        if (this.empty && this.texture == null) {
            return TileDepthTextureManager.getInstance().getEmptyDepthTexture(this.width, this.height);
        } else {
            if (this.texture == null) {
                this.texture = new Texture(this.width, this.height, "DEPTH_" + this.getName(), 0);
                this.updateGPUTexture();
            }

            return this.texture;
        }
    }

    public void updateGPUTexture() {
        if (this.texture == null) {
            this.texture = new Texture(this.width, this.height, "DEPTH_" + this.getName(), 0);
        }

        RenderThread.queueInvokeOnRenderContext(() -> {
            GL11.glBindTexture(3553, Texture.lastTextureID = this.texture.getID());
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
            int BPP = 1;
            ByteBuffer pixels = MemoryUtil.memAlloc(this.getWidth() * this.getHeight() * 1);
            pixels.position(this.getWidth() * this.getHeight() * 1);
            boolean bClamp = true;
            if (this.tileset.getName().startsWith("roofs_")) {
                bClamp = false;
            }

            this.empty = true;

            for (int y = 0; y < this.getHeight(); y++) {
                for (int x = 0; x < this.getWidth(); x++) {
                    float pixel = this.getPixel(x, y);
                    if (pixel >= 0.0F && y < 64 && bClamp) {
                        pixel = this.clampPixelToUpperFloor(x, y, pixel);
                    }

                    if (pixel < 0.0F) {
                        pixels.put(x * 1 + y * this.getWidth() * 1, (byte)0);
                    } else {
                        pixel = PZMath.min(pixel, 1.0F);
                        int rgb = (int)Math.floor(pixel * 255.0F) & 0xFF;
                        if (rgb == 0) {
                            rgb = 1;
                        }

                        byte b = (byte)rgb;
                        pixels.put(x * 1 + y * this.getWidth() * 1, b);
                    }

                    if (pixel >= 0.0F && this.empty) {
                        this.empty = false;
                    }
                }
            }

            if (this.empty) {
                boolean var9 = true;
            }

            pixels.flip();
            GL11.glTexImage2D(3553, 0, 6403, this.getWidth(), this.getHeight(), 0, 6403, 5121, pixels);
            MemoryUtil.memFree(pixels);
            if (this.tileset != null && !this.tileset.isKeepPixels()) {
                this.pixels = null;
            }
        });
    }

    void recalculateDepth() {
        ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance()
            .getGeometry("game", this.getTileset().getName(), this.getColumn(), this.getRow());
        if (geometries != null && !geometries.isEmpty()) {
            float[] pixels = new float[32768];
            Arrays.fill(pixels, 1000.0F);

            for (int i = 0; i < geometries.size(); i++) {
                TileGeometryFile.Geometry geometry = geometries.get(i);
                if (geometry.isPolygon()) {
                    ((TileGeometryFile.Polygon)geometry).rasterize((tileX, tileY) -> {
                        if (tileX >= 0 && tileX < 128 && tileY >= 0 && tileY < 256) {
                            float newPixelx = geometry.getNormalizedDepthAt(tileX, tileY);
                            if (newPixelx >= 0.0F) {
                                pixels[this.index(tileX, tileY)] = PZMath.min(pixels[this.index(tileX, tileY)], newPixelx);
                            }
                        }
                    });
                } else {
                    for (int y = 0; y < this.getHeight(); y++) {
                        for (int x = 0; x < this.getWidth(); x++) {
                            float pixel = this.getPixel(x, y);
                            if (!(pixel < 0.0F)) {
                                float newPixel = geometry.getNormalizedDepthAt(x + 0.5F, y + 0.5F);
                                if (newPixel >= 0.0F) {
                                    pixels[this.index(x, y)] = PZMath.min(pixels[this.index(x, y)], newPixel);
                                }
                            }
                        }
                    }
                }
            }

            for (int y = 0; y < this.getHeight(); y++) {
                for (int xx = 0; xx < this.getWidth(); xx++) {
                    float pixel = pixels[this.index(xx, y)];
                    if (pixel != 1000.0F) {
                        this.setPixel(xx, y, pixel);
                    }
                }
            }

            this.updateGPUTexture();
        }
    }

    TileGeometryFile.Geometry getOrCreateFloorPolygon() {
        if (floorPolygon == null) {
            floorPolygon = new TileGeometryFile.Polygon();
            floorPolygon.plane = TileGeometryFile.Plane.XZ;
            floorPolygon.rotate.set(270.0F, 0.0F, 0.0F);
            floorPolygon.points.add(-1.0F);
            floorPolygon.points.add(-1.0F);
            floorPolygon.points.add(1.0F);
            floorPolygon.points.add(-1.0F);
            floorPolygon.points.add(1.0F);
            floorPolygon.points.add(1.0F);
            floorPolygon.points.add(-1.0F);
            floorPolygon.points.add(1.0F);
            floorPolygon.translate.set(0.0F, 0.0125F, 0.0F);
            floorPolygon.triangulate2();
        }

        return floorPolygon;
    }

    void recalculateShadowDepth() {
        ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance()
            .getGeometry("game", this.getTileset().getName(), this.getColumn(), this.getRow());
        if (geometries != null && !geometries.isEmpty()) {
            float[] pixels = new float[32768];
            Arrays.fill(pixels, 1000.0F);

            for (int y = 0; y < this.getHeight(); y++) {
                for (int x = 0; x < this.getWidth(); x++) {
                    float pixel = this.getPixel(x, y);
                    if (pixel < 0.0F) {
                        pixels[this.index(x, y)] = 1111.0F;
                    }
                }
            }

            TileGeometryFile.Geometry floorPolygon = this.getOrCreateFloorPolygon();

            for (int i = 0; i < geometries.size(); i++) {
                TileGeometryFile.Geometry geometry = geometries.get(i);
                if (geometry.isPolygon()) {
                    ((TileGeometryFile.Polygon)geometry).rasterize((tileX, tileY) -> {
                        if (tileX >= 0 && tileX < 128 && tileY >= 0 && tileY < 256) {
                            if (pixels[this.index(tileX, tileY)] != 1111.0F) {
                                float newPixelx = geometry.getNormalizedDepthAt(tileX + 0.5F, tileY + 0.5F);
                                if (newPixelx >= 0.0F) {
                                    pixels[this.index(tileX, tileY)] = 1111.0F;
                                } else {
                                    pixels[this.index(tileX, tileY)] = 2222.0F;
                                }
                            }
                        }
                    });
                } else {
                    for (int y = 0; y < this.getHeight(); y++) {
                        for (int xx = 0; xx < this.getWidth(); xx++) {
                            if (pixels[this.index(xx, y)] != 1111.0F) {
                                float newPixel = geometry.getNormalizedDepthAt(xx + 0.5F, y + 0.5F);
                                if (newPixel >= 0.0F) {
                                    pixels[this.index(xx, y)] = 1111.0F;
                                } else {
                                    pixels[this.index(xx, y)] = 2222.0F;
                                }
                            }
                        }
                    }
                }
            }

            TileDepthTexture.floorPolygon.rasterize((tileX, tileY) -> {
                if (tileX >= 0 && tileX < 128 && tileY >= 0 && tileY < 256) {
                    float pixelx = pixels[this.index(tileX, tileY)];
                    if (pixelx == 2222.0F) {
                        float newPixelx = floorPolygon.getNormalizedDepthAt(tileX + 0.5F, tileY + 0.5F);
                        if (newPixelx >= 0.0F) {
                            pixels[this.index(tileX, tileY)] = newPixelx;
                        } else {
                            pixels[this.index(tileX, tileY)] = 1000.0F;
                        }
                    }
                }
            });

            for (int y = 0; y < this.getHeight(); y++) {
                for (int xxx = 0; xxx < this.getWidth(); xxx++) {
                    float pixel = pixels[this.index(xxx, y)];
                    if (pixel < 1000.0F) {
                        this.setPixel(xxx, y, pixel);
                    }
                }
            }

            this.updateGPUTexture();
        }
    }

    public void reload() throws Exception {
        this.getTileset().reload();
    }

    public void Reset() {
    }
}
