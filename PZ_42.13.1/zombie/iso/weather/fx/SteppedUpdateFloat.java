// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

/**
 * TurboTuTone.
 */
public class SteppedUpdateFloat {
    private float current;
    private final float step;
    private float target;
    private final float min;
    private final float max;

    public SteppedUpdateFloat(float current, float step, float min, float max) {
        this.current = current;
        this.step = step;
        this.target = current;
        this.min = min;
        this.max = max;
    }

    public float value() {
        return this.current;
    }

    public void setTarget(float target) {
        this.target = this.clamp(this.min, this.max, target);
    }

    public float getTarget() {
        return this.target;
    }

    public void overrideCurrentValue(float f) {
        this.current = f;
    }

    private float clamp(float min, float max, float val) {
        val = Math.min(max, val);
        return Math.max(min, val);
    }

    public void update(float delta) {
        if (this.current != this.target) {
            if (this.target > this.current) {
                this.current = this.current + this.step * delta;
                if (this.current > this.target) {
                    this.current = this.target;
                }
            } else if (this.target < this.current) {
                this.current = this.current - this.step * delta;
                if (this.current < this.target) {
                    this.current = this.target;
                }
            }
        }
    }
}
