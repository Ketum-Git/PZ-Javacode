// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.UsedFromLua;

@UsedFromLua
public final class UITransition {
    private float duration = 100.0F;
    private float elapsed;
    private float frac;
    private boolean fadeOut;
    private boolean ignoreUpdateTime;
    private long updateTimeMs;
    private static long currentTimeMS;
    private static long elapsedTimeMS;

    public static void UpdateAll() {
        long current = System.currentTimeMillis();
        elapsedTimeMS = current - currentTimeMS;
        currentTimeMS = current;
    }

    public void init(float duration, boolean fadeOut) {
        this.duration = Math.max(duration, 1.0F);
        if (this.frac >= 1.0F) {
            this.elapsed = 0.0F;
        } else if (this.fadeOut != fadeOut) {
            this.elapsed = (1.0F - this.frac) * this.duration;
        } else {
            this.elapsed = this.frac * this.duration;
        }

        this.fadeOut = fadeOut;
    }

    public void update() {
        if (!this.ignoreUpdateTime && this.updateTimeMs != 0L) {
            long msDuration = (long)this.duration;
            if (this.updateTimeMs + msDuration < currentTimeMS) {
                this.elapsed = this.duration;
            }
        }

        this.updateTimeMs = currentTimeMS;
        this.frac = this.elapsed / this.duration;
        this.elapsed = Math.min(this.elapsed + (float)elapsedTimeMS, this.duration);
    }

    public float fraction() {
        return this.fadeOut ? 1.0F - this.frac : this.frac;
    }

    public void setFadeIn(boolean fadeIn) {
        if (fadeIn) {
            if (this.fadeOut) {
                this.init(100.0F, false);
            }
        } else if (!this.fadeOut) {
            this.init(200.0F, true);
        }
    }

    public void reset() {
        this.elapsed = 0.0F;
    }

    public void setIgnoreUpdateTime(boolean ignore) {
        this.ignoreUpdateTime = ignore;
    }

    public float getElapsed() {
        return this.elapsed;
    }

    public void setElapsed(float elapsed) {
        this.elapsed = elapsed;
    }
}
