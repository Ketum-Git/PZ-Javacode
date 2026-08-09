// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.profiling;

import zombie.GameProfiler;

public interface IPerformanceProbe {
    default boolean isProbeEnabled() {
        return this.isEnabled() && GameProfiler.isRunning();
    }

    boolean isEnabled();

    void setEnabled(boolean arg0);
}
