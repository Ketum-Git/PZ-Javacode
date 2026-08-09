// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;

public final class ScreenFader {
    private ScreenFader.Stage stage = ScreenFader.Stage.StartFadeToBlack;
    private float alpha;
    private float targetAlpha = 1.0F;
    private long fadeStartMs;
    private final long fadeDurationMs = 350L;

    public void startFadeToBlack() {
        this.alpha = 0.0F;
        this.stage = ScreenFader.Stage.StartFadeToBlack;
    }

    public void startFadeFromBlack() {
        this.alpha = 1.0F;
        this.stage = ScreenFader.Stage.StartFadeFromBlack;
    }

    public void update() {
        switch (this.stage) {
            case StartFadeToBlack:
                this.targetAlpha = 1.0F;
                this.stage = ScreenFader.Stage.UpdateFadeToBlack;
                this.fadeStartMs = System.currentTimeMillis();
                break;
            case UpdateFadeToBlack:
                this.alpha = PZMath.clamp((float)(System.currentTimeMillis() - this.fadeStartMs) / 350.0F, 0.0F, 1.0F);
                if (this.alpha >= this.targetAlpha) {
                    this.stage = ScreenFader.Stage.Hold;
                }
            case Hold:
            default:
                break;
            case StartFadeFromBlack:
                this.targetAlpha = 0.0F;
                this.stage = ScreenFader.Stage.UpdateFadeFromBlack;
                this.fadeStartMs = System.currentTimeMillis();
                break;
            case UpdateFadeFromBlack:
                this.alpha = 1.0F - PZMath.clamp((float)(System.currentTimeMillis() - this.fadeStartMs) / 350.0F, 0.0F, 1.0F);
                if (this.alpha <= this.targetAlpha) {
                    this.stage = ScreenFader.Stage.Hold;
                }
        }
    }

    public void preRender() {
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        Core.getInstance().StartFrameUI();
        this.update();
    }

    public void postRender() {
        this.render();
        Core.getInstance().EndFrameUI();
    }

    public void render() {
        int screenW = Core.getInstance().getScreenWidth();
        int screenH = Core.getInstance().getScreenHeight();
        SpriteRenderer.instance.renderi(null, 0, 0, screenW, screenH, 0.0F, 0.0F, 0.0F, this.alpha, null);
    }

    public boolean isFading() {
        return this.stage != ScreenFader.Stage.Hold;
    }

    public float getAlpha() {
        return this.alpha;
    }

    private static enum Stage {
        StartFadeToBlack,
        UpdateFadeToBlack,
        Hold,
        StartFadeFromBlack,
        UpdateFadeFromBlack;
    }
}
