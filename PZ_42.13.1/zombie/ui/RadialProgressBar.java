// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.Vector2;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class RadialProgressBar extends UIElement {
    private static final boolean DEBUG = false;
    Texture radialTexture;
    float deltaValue = 1.0F;
    private static final RadialProgressBar.RadSegment[] segments = new RadialProgressBar.RadSegment[8];
    private static final float TWO_PI = 6.283185F;
    private static final float PI_OVER_TWO = 1.570796F;

    public RadialProgressBar(KahluaTable table, Texture tex) {
        super(table);
        this.radialTexture = tex;
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void render() {
        if (this.enabled) {
            if (this.isVisible()) {
                if (this.parent == null || this.parent.maxDrawHeight == -1 || !(this.parent.maxDrawHeight <= this.y)) {
                    if (this.radialTexture != null) {
                        float dx = (float)(this.xScroll + this.getAbsoluteX() + this.radialTexture.offsetX);
                        float dy = (float)(this.yScroll + this.getAbsoluteY() + this.radialTexture.offsetY);
                        float o_uvx = this.radialTexture.xStart;
                        float o_uvy = this.radialTexture.yStart;
                        float o_uvw = this.radialTexture.xEnd - this.radialTexture.xStart;
                        float o_uvh = this.radialTexture.yEnd - this.radialTexture.yStart;
                        float centerX = dx + 0.5F * this.width;
                        float centerY = dy + 0.5F * this.height;
                        float percent = this.deltaValue;
                        float angle = percent * 6.283185F - 1.570796F;
                        Vector2 angleVector = new Vector2((float)Math.cos(angle), (float)Math.sin(angle));
                        float scalar;
                        float textureScalar;
                        if (Math.abs(this.width / 2.0F / angleVector.x) < Math.abs(this.height / 2.0F / angleVector.y)) {
                            scalar = Math.abs(this.width / 2.0F / angleVector.x);
                            textureScalar = Math.abs(0.5F / angleVector.x);
                        } else {
                            scalar = Math.abs(this.height / 2.0F / angleVector.y);
                            textureScalar = Math.abs(0.5F / angleVector.y);
                        }

                        float tx = centerX + angleVector.x * scalar;
                        float ty = centerY + angleVector.y * scalar;
                        float uvx = 0.5F + angleVector.x * textureScalar;
                        float uvy = 0.5F + angleVector.y * textureScalar;
                        int index = (int)(percent * 8.0F);
                        if (percent <= 0.0F) {
                            index = -1;
                        }

                        for (int i = 0; i < segments.length; i++) {
                            RadialProgressBar.RadSegment s = segments[i];
                            if (s != null && i <= index) {
                                if (i != index) {
                                    SpriteRenderer.instance
                                        .renderPoly(
                                            this.radialTexture,
                                            dx + s.vertex[0].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[0].y * this.radialTexture.getHeight(),
                                            dx + s.vertex[1].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[1].y * this.radialTexture.getHeight(),
                                            dx + s.vertex[2].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[2].y * this.radialTexture.getHeight(),
                                            dx + s.vertex[2].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[2].y * this.radialTexture.getHeight(),
                                            1.0F,
                                            1.0F,
                                            1.0F,
                                            1.0F,
                                            o_uvx + s.uv[0].x * o_uvw,
                                            o_uvy + s.uv[0].y * o_uvh,
                                            o_uvx + s.uv[1].x * o_uvw,
                                            o_uvy + s.uv[1].y * o_uvh,
                                            o_uvx + s.uv[2].x * o_uvw,
                                            o_uvy + s.uv[2].y * o_uvh,
                                            o_uvx + s.uv[2].x * o_uvw,
                                            o_uvy + s.uv[2].y * o_uvh
                                        );
                                } else {
                                    SpriteRenderer.instance
                                        .renderPoly(
                                            this.radialTexture,
                                            dx + s.vertex[0].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[0].y * this.radialTexture.getHeight(),
                                            tx,
                                            ty,
                                            dx + s.vertex[2].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[2].y * this.radialTexture.getHeight(),
                                            dx + s.vertex[2].x * this.radialTexture.getWidth(),
                                            dy + s.vertex[2].y * this.radialTexture.getHeight(),
                                            1.0F,
                                            1.0F,
                                            1.0F,
                                            1.0F,
                                            o_uvx + s.uv[0].x * o_uvw,
                                            o_uvy + s.uv[0].y * o_uvh,
                                            o_uvx + uvx * o_uvw,
                                            o_uvy + uvy * o_uvh,
                                            o_uvx + s.uv[2].x * o_uvw,
                                            o_uvy + s.uv[2].y * o_uvh,
                                            o_uvx + s.uv[2].x * o_uvw,
                                            o_uvy + s.uv[2].y * o_uvh
                                        );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void setValue(float delta) {
        this.deltaValue = PZMath.clamp(delta, 0.0F, 1.0F);
    }

    public float getValue() {
        return this.deltaValue;
    }

    public void setTexture(Texture texture) {
        this.radialTexture = texture;
    }

    public Texture getTexture() {
        return this.radialTexture;
    }

    private void printTexture(Texture t) {
        DebugLog.log("xStart = " + t.xStart);
        DebugLog.log("yStart = " + t.yStart);
        DebugLog.log("offX = " + t.offsetX);
        DebugLog.log("offY = " + t.offsetY);
        DebugLog.log("xEnd = " + t.xEnd);
        DebugLog.log("yEnd = " + t.yEnd);
        DebugLog.log("Width = " + t.getWidth());
        DebugLog.log("Height = " + t.getHeight());
        DebugLog.log("RealWidth = " + t.getRealWidth());
        DebugLog.log("RealHeight = " + t.getRealHeight());
        DebugLog.log("OrigWidth = " + t.getWidthOrig());
        DebugLog.log("OrigHeight = " + t.getHeightOrig());
    }

    static {
        segments[0] = new RadialProgressBar.RadSegment();
        segments[0].set(0.5F, 0.0F, 1.0F, 0.0F, 0.5F, 0.5F);
        segments[1] = new RadialProgressBar.RadSegment();
        segments[1].set(1.0F, 0.0F, 1.0F, 0.5F, 0.5F, 0.5F);
        segments[2] = new RadialProgressBar.RadSegment();
        segments[2].set(1.0F, 0.5F, 1.0F, 1.0F, 0.5F, 0.5F);
        segments[3] = new RadialProgressBar.RadSegment();
        segments[3].set(1.0F, 1.0F, 0.5F, 1.0F, 0.5F, 0.5F);
        segments[4] = new RadialProgressBar.RadSegment();
        segments[4].set(0.5F, 1.0F, 0.0F, 1.0F, 0.5F, 0.5F);
        segments[5] = new RadialProgressBar.RadSegment();
        segments[5].set(0.0F, 1.0F, 0.0F, 0.5F, 0.5F, 0.5F);
        segments[6] = new RadialProgressBar.RadSegment();
        segments[6].set(0.0F, 0.5F, 0.0F, 0.0F, 0.5F, 0.5F);
        segments[7] = new RadialProgressBar.RadSegment();
        segments[7].set(0.0F, 0.0F, 0.5F, 0.0F, 0.5F, 0.5F);
    }

    private static class RadSegment {
        Vector2[] vertex = new Vector2[3];
        Vector2[] uv = new Vector2[3];

        private RadialProgressBar.RadSegment set(int index, float vx, float vy, float uv1, float uv2) {
            this.vertex[index] = new Vector2(vx, vy);
            this.uv[index] = new Vector2(uv1, uv2);
            return this;
        }

        private void set(float x0, float y0, float x1, float y1, float x2, float y2) {
            this.vertex[0] = new Vector2(x0, y0);
            this.vertex[1] = new Vector2(x1, y1);
            this.vertex[2] = new Vector2(x2, y2);
            this.uv[0] = new Vector2(x0, y0);
            this.uv[1] = new Vector2(x1, y1);
            this.uv[2] = new Vector2(x2, y2);
        }
    }
}
