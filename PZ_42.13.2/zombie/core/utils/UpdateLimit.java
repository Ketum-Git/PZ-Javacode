// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

public final class UpdateLimit {
    private long delay;
    private long last;
    private long lastPeriod;

    public UpdateLimit(long ms) {
        this.delay = ms;
        this.last = System.currentTimeMillis();
        this.lastPeriod = this.last;
    }

    public UpdateLimit(long ms, long shift) {
        this.delay = ms;
        this.last = System.currentTimeMillis() - shift;
        this.lastPeriod = this.last;
    }

    public void BlockCheck() {
        this.last = System.currentTimeMillis() + this.delay;
    }

    public void Reset(long ms) {
        this.delay = ms;
        this.Reset();
    }

    public void Reset() {
        this.last = System.currentTimeMillis();
        this.lastPeriod = System.currentTimeMillis();
    }

    public void setUpdatePeriod(long ms) {
        this.delay = ms;
    }

    public void setSmoothUpdatePeriod(long ms) {
        this.delay = (long)((float)this.delay + 0.1F * (float)(ms - this.delay));
    }

    public boolean Check() {
        long ms = System.currentTimeMillis();
        if (ms - this.last > this.delay) {
            if (ms - this.last > 3L * this.delay) {
                this.last = ms;
            } else {
                this.last = this.last + this.delay;
            }

            return true;
        } else {
            return false;
        }
    }

    public long getLast() {
        return this.last;
    }

    public void updateTimePeriod() {
        long ms = System.currentTimeMillis();
        if (ms - this.last > this.delay) {
            if (ms - this.last > 3L * this.delay) {
                this.last = ms;
            } else {
                this.last = this.last + this.delay;
            }
        }

        this.lastPeriod = ms;
    }

    public double getTimePeriod() {
        return Math.min(((double)System.currentTimeMillis() - this.lastPeriod) / this.delay, 1.0);
    }

    public long getDelay() {
        return this.delay;
    }
}
