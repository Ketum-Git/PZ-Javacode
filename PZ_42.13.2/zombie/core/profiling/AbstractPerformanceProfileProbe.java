// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.profiling;

import zombie.GameProfiler;

public abstract class AbstractPerformanceProfileProbe implements IPerformanceProbe, AutoCloseable {
    public final String name;
    private boolean isEnabled = true;
    private boolean isRunning;
    private boolean isProfilerRunning;

    protected AbstractPerformanceProfileProbe(String name) {
        this.name = name;
    }

    public AbstractPerformanceProfileProbe profile() {
        this.start();
        return this;
    }

    @Override
    public void close() {
        this.end();
    }

    protected abstract void onStart();

    protected abstract void onEnd();

    @Deprecated
    public void start() {
        if (GameProfiler.isValidThread()) {
            if (this.isRunning) {
                throw new RuntimeException("start() already called. " + this.getClass().getSimpleName() + " is Non-reentrant. Please call end() first.");
            } else {
                this.isProfilerRunning = this.isEnabled() && GameProfiler.isRunning();
                if (this.isProfilerRunning) {
                    this.isRunning = true;
                    this.onStart();
                }
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    @Override
    public void setEnabled(boolean val) {
        this.isEnabled = val;
    }

    @Deprecated
    public void end() {
        if (GameProfiler.isValidThread()) {
            if (this.isProfilerRunning) {
                if (!this.isRunning) {
                    throw new RuntimeException("end() called without calling start().");
                } else {
                    this.onEnd();
                    this.isRunning = false;
                }
            }
        }
    }
}
