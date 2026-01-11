// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.List;

public class WorldGenTiming {
    private long startTime;
    private long endTime;
    private long duration;
    private long totalDuration;
    private long minTime = Long.MAX_VALUE;
    private long maxTime;
    private int times;
    private final boolean keep;
    private final int toIgnore;
    private int ignore;
    private final List<Long> kept = new ArrayList<>();

    public WorldGenTiming() {
        this(false, 0);
    }

    public WorldGenTiming(boolean keep, int toIgnore) {
        this.keep = keep;
        this.toIgnore = toIgnore;
    }

    public void reset() {
        this.startTime = 0L;
        this.endTime = 0L;
        this.duration = 0L;
        this.totalDuration = 0L;
        this.minTime = Long.MAX_VALUE;
        this.maxTime = 0L;
        this.times = 0;
        this.ignore = 0;
        this.kept.clear();
    }

    public void start() {
        this.startTime = System.nanoTime();
    }

    public void stop() {
        if (this.ignore < this.toIgnore) {
            this.ignore++;
        } else {
            this.times++;
            this.endTime = System.nanoTime();
            this.duration = this.endTime - this.startTime;
            this.totalDuration = this.totalDuration + this.duration;
            this.minTime = Math.min(this.minTime, this.duration);
            this.maxTime = Math.max(this.maxTime, this.duration);
            if (this.keep) {
                this.kept.add(this.duration);
            }
        }
    }

    public long duration() {
        return this.duration;
    }

    public long totalDuration() {
        return this.totalDuration;
    }

    public long meanDuration() {
        return this.times == 0 ? 0L : this.totalDuration / this.times;
    }

    public int times() {
        return this.times;
    }

    public long minTime() {
        return this.minTime;
    }

    public long maxTime() {
        return this.maxTime;
    }

    public List<Long> getKept() {
        return this.kept;
    }
}
