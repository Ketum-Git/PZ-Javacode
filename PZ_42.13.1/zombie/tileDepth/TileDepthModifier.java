// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.util.function.Consumer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;

public final class TileDepthModifier implements Consumer<TextureDraw> {
    public static final TileDepthModifier instance = new TileDepthModifier();
    private Texture depthTexture;
    private IsoSprite sprite;
    private float scaleX = 1.0F;
    private float scaleY = 1.0F;

    public void accept(TextureDraw textureDraw) {
        Texture tex0 = textureDraw.tex;
        Texture tex1 = this.depthTexture;
        if (tex0 != null && tex1 != null) {
            float x3;
            float x0 = x3 = PZMath.max(this.sprite.soffX + tex0.offsetX * this.scaleX, tex1.offsetX);
            float x2;
            float x1 = x2 = PZMath.min(this.sprite.soffX + (tex0.offsetX + tex0.getWidth()) * this.scaleX, tex1.offsetX + tex1.getWidth());
            float y1;
            float y0 = y1 = PZMath.max(this.sprite.soffY + tex0.offsetY * this.scaleY, tex1.offsetY);
            float y3;
            float y2 = y3 = PZMath.min(this.sprite.soffY + (tex0.offsetY + tex0.getHeight()) * this.scaleY, tex1.offsetY + tex1.getHeight());
            float xStart = tex1.getXStart();
            float yStart = tex1.getYStart();
            float widthHW = tex1.getWidthHW();
            float heightHW = tex1.getHeightHW();
            textureDraw.tex1 = this.depthTexture;
            textureDraw.tex1U0 = xStart + (x0 - tex1.offsetX) / widthHW;
            textureDraw.tex1U1 = xStart + (x1 - tex1.offsetX) / widthHW;
            textureDraw.tex1U2 = xStart + (x2 - tex1.offsetX) / widthHW;
            textureDraw.tex1U3 = xStart + (x3 - tex1.offsetX) / widthHW;
            textureDraw.tex1V0 = yStart + (y0 - tex1.offsetY) / heightHW;
            textureDraw.tex1V1 = yStart + (y1 - tex1.offsetY) / heightHW;
            textureDraw.tex1V2 = yStart + (y2 - tex1.offsetY) / heightHW;
            textureDraw.tex1V3 = yStart + (y3 - tex1.offsetY) / heightHW;
        }
    }

    public void setupFloorDepth(IsoSprite sprite) {
        this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.Floor);
        this.sprite = sprite;
        this.scaleX = 1.0F;
        this.scaleY = 1.0F;
    }

    public void setupWallDepth(IsoSprite sprite, IsoDirections dir) {
        if (sprite.depthTexture != null) {
            this.depthTexture = sprite.depthTexture.getTexture();
            this.sprite = sprite;
            this.scaleX = 1.0F;
            this.scaleY = 1.0F;
        } else {
            TileDepthMapManager.TileDepthPreset presetID = TileDepthMapManager.TileDepthPreset.Floor;
            switch (dir) {
                case N:
                    presetID = TileDepthMapManager.TileDepthPreset.NWall;
                    if (sprite.getProperties().has(IsoFlagType.DoorWallN) && !sprite.getProperties().has(IsoFlagType.doorN)) {
                        presetID = TileDepthMapManager.TileDepthPreset.NDoorFrame;
                    }
                    break;
                case NW:
                    presetID = TileDepthMapManager.TileDepthPreset.NWWall;
                    break;
                case W:
                    presetID = TileDepthMapManager.TileDepthPreset.WWall;
                    if (sprite.getProperties().has(IsoFlagType.DoorWallW) && !sprite.getProperties().has(IsoFlagType.doorW)) {
                        presetID = TileDepthMapManager.TileDepthPreset.WDoorFrame;
                    }

                    if (sprite.isWallSE()) {
                        presetID = TileDepthMapManager.TileDepthPreset.SEWall;
                    }
                    break;
                case SE:
                    presetID = TileDepthMapManager.TileDepthPreset.SEWall;
            }

            this.depthTexture = TileDepthMapManager.instance.getTextureForPreset(presetID);
            this.sprite = sprite;
            this.scaleX = 1.0F;
            this.scaleY = 1.0F;
        }
    }

    public void setupTileDepthTexture(IsoSprite sprite, TileDepthTexture depthTexture) {
        this.depthTexture = depthTexture.getTexture();
        this.sprite = sprite;
        this.scaleX = 1.0F;
        this.scaleY = 1.0F;
    }

    public void setSpriteScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
}
