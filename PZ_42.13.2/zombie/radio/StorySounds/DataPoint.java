// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import zombie.UsedFromLua;

/**
 * Turbo
 */
@UsedFromLua
public final class DataPoint {
    protected float time;
    protected float intensity;

    public DataPoint(float time, float intensity) {
        this.setTime(time);
        this.setIntensity(intensity);
    }

    public float getTime() {
        return this.time;
    }

    public void setTime(float time) {
        if (time < 0.0F) {
            time = 0.0F;
        }

        if (time > 1.0F) {
            time = 1.0F;
        }

        this.time = time;
    }

    public float getIntensity() {
        return this.intensity;
    }

    public void setIntensity(float intensity) {
        if (intensity < 0.0F) {
            intensity = 0.0F;
        }

        if (intensity > 1.0F) {
            intensity = 1.0F;
        }

        this.intensity = intensity;
    }
}
