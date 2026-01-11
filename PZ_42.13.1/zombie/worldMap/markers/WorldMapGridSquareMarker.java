// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.markers;

import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.worldMap.UIWorldMap;

@UsedFromLua
public final class WorldMapGridSquareMarker extends WorldMapMarker {
    Texture texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
    Texture texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
    float r = 1.0F;
    float g = 1.0F;
    float b = 1.0F;
    float a = 1.0F;
    int worldX;
    int worldY;
    int radius = 10;
    int minScreenRadius = 64;
    boolean blink = true;

    WorldMapGridSquareMarker init(int worldX, int worldY, int radius, float r, float g, float b, float a) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.radius = radius;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public void setBlink(boolean blink) {
        this.blink = blink;
    }

    public void setMinScreenRadius(int pixels) {
        this.minScreenRadius = pixels;
    }

    @Override
    void render(UIWorldMap ui) {
        float radius = PZMath.max((float)this.radius, this.minScreenRadius / ui.getAPI().getWorldScale());
        float x1 = ui.getAPI().worldToUIX(this.worldX - radius, this.worldY - radius);
        float y1 = ui.getAPI().worldToUIY(this.worldX - radius, this.worldY - radius);
        float x2 = ui.getAPI().worldToUIX(this.worldX + radius, this.worldY - radius);
        float y2 = ui.getAPI().worldToUIY(this.worldX + radius, this.worldY - radius);
        float x3 = ui.getAPI().worldToUIX(this.worldX + radius, this.worldY + radius);
        float y3 = ui.getAPI().worldToUIY(this.worldX + radius, this.worldY + radius);
        float x4 = ui.getAPI().worldToUIX(this.worldX - radius, this.worldY + radius);
        float y4 = ui.getAPI().worldToUIY(this.worldX - radius, this.worldY + radius);
        x1 = (float)(x1 + ui.getAbsoluteX());
        y1 = (float)(y1 + ui.getAbsoluteY());
        x2 = (float)(x2 + ui.getAbsoluteX());
        y2 = (float)(y2 + ui.getAbsoluteY());
        x3 = (float)(x3 + ui.getAbsoluteX());
        y3 = (float)(y3 + ui.getAbsoluteY());
        x4 = (float)(x4 + ui.getAbsoluteX());
        y4 = (float)(y4 + ui.getAbsoluteY());
        float alpha = this.a * (this.blink ? Core.blinkAlpha : 1.0F);
        if (this.texture1 != null && this.texture1.isReady()) {
            SpriteRenderer.instance.render(this.texture1, x1, y1, x2, y2, x3, y3, x4, y4, this.r, this.g, this.b, alpha, null);
        }

        if (this.texture2 != null && this.texture2.isReady()) {
            SpriteRenderer.instance.render(this.texture2, x1, y1, x2, y2, x3, y3, x4, y4, this.r, this.g, this.b, alpha, null);
        }
    }
}
