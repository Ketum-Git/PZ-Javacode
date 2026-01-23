// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.profiling;

import zombie.GameProfiler;

public class PerformanceProfileFrameProbe extends PerformanceProfileProbe {
    public PerformanceProfileFrameProbe(String name) {
        super(name);
    }

    @Override
    public void start() {
        GameProfiler.getInstance().startFrame(this.name);
        super.start();
    }

    @Override
    public void end() {
        super.end();
        GameProfiler.getInstance().endFrame();
    }
}
