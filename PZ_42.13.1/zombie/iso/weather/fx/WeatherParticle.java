// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

import org.lwjgl.util.Rectangle;
import zombie.core.Color;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;

/**
 * TurboTuTone.
 */
public abstract class WeatherParticle {
    protected ParticleRectangle parent;
    protected Rectangle bounds;
    protected Texture texture;
    protected Color color = Color.white;
    protected Vector2 position = new Vector2(0.0F, 0.0F);
    protected Vector2 velocity = new Vector2(0.0F, 0.0F);
    protected float alpha = 1.0F;
    protected float speed;
    protected SteppedUpdateFloat alphaFadeMod = new SteppedUpdateFloat(0.0F, 0.1F, 0.0F, 1.0F);
    protected float renderAlpha;
    protected float oWidth;
    protected float oHeight;
    protected float zoomMultiW;
    protected float zoomMultiH;
    protected boolean recalcSizeOnZoom;
    protected float lastZoomMod = -1.0F;

    public WeatherParticle(Texture texture) {
        this.texture = texture;
        this.bounds = new Rectangle(0, 0, texture.getWidth(), texture.getHeight());
        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
    }

    public WeatherParticle(Texture texture, int w, int h) {
        this.texture = texture;
        this.bounds = new Rectangle(0, 0, w, h);
        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
    }

    protected void setParent(ParticleRectangle parent) {
        this.parent = parent;
    }

    public void update(float delta) {
        this.update(delta, true);
    }

    public void update(float delta, boolean doBounds) {
        this.alphaFadeMod.update(delta);
        if (this.position.x > this.parent.getWidth()) {
            this.position.x = this.position.x - (int)(this.position.x / this.parent.getWidth()) * this.parent.getWidth();
        } else if (this.position.x < 0.0F) {
            this.position.x = this.position.x - (int)((this.position.x - this.parent.getWidth()) / this.parent.getWidth()) * this.parent.getWidth();
        }

        if (this.position.y > this.parent.getHeight()) {
            this.position.y = this.position.y - (int)(this.position.y / this.parent.getHeight()) * this.parent.getHeight();
        } else if (this.position.y < 0.0F) {
            this.position.y = this.position.y - (int)((this.position.y - this.parent.getHeight()) / this.parent.getHeight()) * this.parent.getHeight();
        }

        if (doBounds) {
            this.bounds.setLocation((int)this.position.x - this.bounds.getWidth() / 2, (int)this.position.y - this.bounds.getHeight() / 2);
        }
    }

    protected boolean updateZoomSize() {
        if (this.recalcSizeOnZoom && this.lastZoomMod != IsoWeatherFX.zoomMod) {
            this.lastZoomMod = IsoWeatherFX.zoomMod;
            this.oWidth = this.bounds.getWidth();
            this.oHeight = this.bounds.getHeight();
            if (this.lastZoomMod > 0.0F) {
                this.oWidth = this.oWidth * (1.0F + IsoWeatherFX.zoomMod * this.zoomMultiW);
                this.oHeight = this.oHeight * (1.0F + IsoWeatherFX.zoomMod * this.zoomMultiH);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean isOnScreen(float offsetx, float offsety) {
        int screenW = IsoCamera.frameState.offscreenWidth;
        int screenH = IsoCamera.frameState.offscreenHeight;
        float x1 = offsetx + this.bounds.getX();
        float y1 = offsety + this.bounds.getY();
        float x2 = x1 + this.oWidth;
        float y2 = y1 + this.oHeight;
        return x1 >= screenW || x2 <= 0.0F ? false : !(y1 >= screenH) && !(y2 <= 0.0F);
    }

    public void render(float offsetx, float offsety) {
        if (PerformanceSettings.fboRenderChunk) {
            IsoWeatherFX.instance
                .getDrawer(this.parent.id)
                .addParticle(
                    this.texture,
                    offsetx + this.bounds.getX(),
                    offsety + this.bounds.getY(),
                    this.oWidth,
                    this.oHeight,
                    this.color.r,
                    this.color.g,
                    this.color.b,
                    this.renderAlpha
                );
        } else {
            SpriteRenderer.instance
                .render(
                    this.texture,
                    offsetx + this.bounds.getX(),
                    offsety + this.bounds.getY(),
                    this.oWidth,
                    this.oHeight,
                    this.color.r,
                    this.color.g,
                    this.color.b,
                    this.renderAlpha,
                    null
                );
        }
    }
}
