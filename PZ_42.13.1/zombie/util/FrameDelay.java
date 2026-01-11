// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import zombie.GameTime;

public class FrameDelay {
    public int delay = 1;
    private int count;
    private float delta;
    private float multiplier;

    public FrameDelay() {
    }

    public FrameDelay(int _delay) {
        this.delay = _delay;
    }

    public boolean update() {
        if (this.count == 0) {
            this.delta = 0.0F;
            this.multiplier = 0.0F;
        }

        this.delta = this.delta + GameTime.instance.getTimeDelta();
        this.multiplier = this.multiplier + GameTime.instance.getMultiplier();
        this.count = this.count + Math.round(GameTime.instance.perObjectMultiplier);
        if (this.count > this.delay) {
            this.count = 0;
            return true;
        } else {
            return false;
        }
    }

    public float getDelta() {
        return this.delta;
    }

    public float getMultiplier() {
        return this.multiplier;
    }

    public void reset() {
        this.count = 0;
        this.delta = 0.0F;
        this.multiplier = 0.0F;
    }
}
