// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

public class CutawayAttachedModifier implements Consumer<TextureDraw> {
    public static CutawayAttachedModifier instance = new CutawayAttachedModifier();
    private Texture depthTexture;
    private Texture cutawayTexture;
    private IsoSprite sprite;
    protected final int[] col = new int[4];
    protected int colTint;
    private int cutawayX;
    private int cutawayY;
    private int cutawayW;
    private int cutawayH;
    private SpriteRenderer.WallShaderTexRender wallShaderTexRender;

    public void accept(TextureDraw textureDraw) {
        Texture tex0 = textureDraw.tex;
        Texture tex1 = this.depthTexture;
        Texture tex2 = this.cutawayTexture;
        float left = textureDraw.x0 - tex0.getOffsetX();
        float top = textureDraw.y0 - tex0.getOffsetY();
        int cutawayNWHeight = 226;
        int TILE_WIDTH = 128;
        int TILE_HEIGHT = 226;
        int tex2_offsetX = this.cutawayX % 128;
        int tex2_offsetY = this.cutawayY % 226;
        int tex2_width = this.cutawayW;
        int tex2_height = this.cutawayH;
        float x3;
        float x0 = x3 = PZMath.max(tex0.offsetX, tex1.offsetX, (float)tex2_offsetX);
        float x2;
        float x1 = x2 = PZMath.min(tex0.offsetX + tex0.getWidth(), tex1.offsetX + tex1.getWidth(), (float)(tex2_offsetX + tex2_width));
        float y1;
        float y0 = y1 = PZMath.max(tex0.offsetY, tex1.offsetY, (float)tex2_offsetY);
        float y3;
        float y2 = y3 = PZMath.min(tex0.offsetY + tex0.getHeight(), tex1.offsetY + tex1.getHeight(), (float)(tex2_offsetY + tex2_height));
        if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.LeftOnly) {
            x1 = x2 = PZMath.min(x1, 63.0F);
        }

        if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.RightOnly) {
            x0 = x3 = PZMath.max(x0, 63.0F);
        }

        textureDraw.x0 = left + x0;
        textureDraw.x1 = left + x1;
        textureDraw.x2 = left + x2;
        textureDraw.x3 = left + x3;
        textureDraw.y0 = top + y0;
        textureDraw.y1 = top + y1;
        textureDraw.y2 = top + y2;
        textureDraw.y3 = top + y3;
        textureDraw.u0 = (tex0.getXStart() * tex0.getWidthHW() + (x0 - tex0.offsetX)) / tex0.getWidthHW();
        textureDraw.u1 = (tex0.getXStart() * tex0.getWidthHW() + (x1 - tex0.offsetX)) / tex0.getWidthHW();
        textureDraw.u2 = (tex0.getXStart() * tex0.getWidthHW() + (x2 - tex0.offsetX)) / tex0.getWidthHW();
        textureDraw.u3 = (tex0.getXStart() * tex0.getWidthHW() + (x3 - tex0.offsetX)) / tex0.getWidthHW();
        textureDraw.v0 = (tex0.getYStart() * tex0.getHeightHW() + (y0 - tex0.offsetY)) / tex0.getHeightHW();
        textureDraw.v1 = (tex0.getYStart() * tex0.getHeightHW() + (y1 - tex0.offsetY)) / tex0.getHeightHW();
        textureDraw.v2 = (tex0.getYStart() * tex0.getHeightHW() + (y2 - tex0.offsetY)) / tex0.getHeightHW();
        textureDraw.v3 = (tex0.getYStart() * tex0.getHeightHW() + (y3 - tex0.offsetY)) / tex0.getHeightHW();
        textureDraw.tex1 = this.depthTexture;
        textureDraw.tex1U0 = (tex1.getXStart() * tex1.getWidthHW() + (x0 - tex1.offsetX)) / tex1.getWidthHW();
        textureDraw.tex1U1 = (tex1.getXStart() * tex1.getWidthHW() + (x1 - tex1.offsetX)) / tex1.getWidthHW();
        textureDraw.tex1U2 = (tex1.getXStart() * tex1.getWidthHW() + (x2 - tex1.offsetX)) / tex1.getWidthHW();
        textureDraw.tex1U3 = (tex1.getXStart() * tex1.getWidthHW() + (x3 - tex1.offsetX)) / tex1.getWidthHW();
        textureDraw.tex1V0 = (tex1.getYStart() * tex1.getHeightHW() + (y0 - tex1.offsetY)) / tex1.getHeightHW();
        textureDraw.tex1V1 = (tex1.getYStart() * tex1.getHeightHW() + (y1 - tex1.offsetY)) / tex1.getHeightHW();
        textureDraw.tex1V2 = (tex1.getYStart() * tex1.getHeightHW() + (y2 - tex1.offsetY)) / tex1.getHeightHW();
        textureDraw.tex1V3 = (tex1.getYStart() * tex1.getHeightHW() + (y3 - tex1.offsetY)) / tex1.getHeightHW();
        textureDraw.tex2 = this.cutawayTexture;
        textureDraw.tex2U0 = (this.cutawayX + (x0 - tex2_offsetX)) / tex2.getWidthHW();
        textureDraw.tex2U1 = (this.cutawayX + (x1 - tex2_offsetX)) / tex2.getWidthHW();
        textureDraw.tex2U2 = (this.cutawayX + (x2 - tex2_offsetX)) / tex2.getWidthHW();
        textureDraw.tex2U3 = (this.cutawayX + (x3 - tex2_offsetX)) / tex2.getWidthHW();
        textureDraw.tex2V0 = (this.cutawayY + (y0 - tex2_offsetY)) / tex2.getHeightHW();
        textureDraw.tex2V1 = (this.cutawayY + (y1 - tex2_offsetY)) / tex2.getHeightHW();
        textureDraw.tex2V2 = (this.cutawayY + (y2 - tex2_offsetY)) / tex2.getHeightHW();
        textureDraw.tex2V3 = (this.cutawayY + (y3 - tex2_offsetY)) / tex2.getHeightHW();
        if (this.sprite == null || !this.sprite.getProperties().has(IsoFlagType.NoWallLighting)) {
            this.applyShading_Wall(textureDraw);
        }
    }

    public void setSprite(IsoSprite sprite) {
        this.sprite = sprite;
    }

    public void setVertColors(int col0, int col1, int col2, int col3) {
        this.col[0] = col0;
        this.col[1] = col1;
        this.col[2] = col2;
        this.col[3] = col3;
    }

    public void setAlpha4(float alpha) {
        int byteA = (int)(alpha * 255.0F) & 0xFF;
        this.col[0] = this.col[0] & 16777215 | byteA << 24;
        this.col[1] = this.col[1] & 16777215 | byteA << 24;
        this.col[2] = this.col[2] & 16777215 | byteA << 24;
        this.col[3] = this.col[3] & 16777215 | byteA << 24;
    }

    public void setTintColor(int tintABGR) {
        this.colTint = tintABGR;
    }

    public void setupWallDepth(
        IsoSprite sprite,
        IsoDirections dir,
        Texture cutawayTex,
        int cutawayX,
        int cutawayY,
        int cutawayW,
        int cutawayH,
        SpriteRenderer.WallShaderTexRender wallShaderTexRender
    ) {
        this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(switch (dir) {
            case N -> {
                yield TileDepthMapManager.TileDepthPreset.NWall;
                if (sprite.getProperties().has(IsoFlagType.DoorWallN) && !sprite.getProperties().has(IsoFlagType.doorN)) {
                    yield TileDepthMapManager.TileDepthPreset.NDoorFrame;
                }
            }
            case NW -> TileDepthMapManager.TileDepthPreset.NWWall;
            case W -> {
                yield TileDepthMapManager.TileDepthPreset.WWall;
                if (sprite.getProperties().has(IsoFlagType.DoorWallW) && !sprite.getProperties().has(IsoFlagType.doorW)) {
                    yield TileDepthMapManager.TileDepthPreset.WDoorFrame;
                }
            }
            case SE -> TileDepthMapManager.TileDepthPreset.SEWall;
            default -> TileDepthMapManager.TileDepthPreset.Floor;
        });
        if (sprite.depthTexture != null) {
            this.depthTexture = sprite.depthTexture.getTexture();
        }

        this.cutawayTexture = cutawayTex;
        this.cutawayX = cutawayX;
        this.cutawayY = cutawayY;
        this.cutawayW = cutawayW;
        this.cutawayH = cutawayH;
        this.wallShaderTexRender = wallShaderTexRender;
    }

    private void applyShading_Wall(TextureDraw texd) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lighting.getValue()) {
            texd.col0 = Color.blendBGR(texd.col0, this.col[0]);
            texd.col1 = Color.blendBGR(texd.col1, this.col[1]);
            texd.col2 = Color.blendBGR(texd.col2, this.col[2]);
            texd.col3 = Color.blendBGR(texd.col3, this.col[3]);
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            texd.col0 = -1;
            texd.col1 = -1;
            texd.col2 = -1;
            texd.col3 = -1;
        }

        if (this.colTint != 0) {
            texd.col0 = Color.tintABGR(texd.col0, this.colTint);
            texd.col1 = Color.tintABGR(texd.col1, this.colTint);
            texd.col2 = Color.tintABGR(texd.col2, this.colTint);
            texd.col3 = Color.tintABGR(texd.col3, this.colTint);
        }
    }
}
