// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.jobs;

/**
 * TurboTuTone.
 */
public abstract class RegionJob {
    private final RegionJobType type;

    protected RegionJob(RegionJobType type) {
        this.type = type;
    }

    protected void reset() {
    }

    public RegionJobType getJobType() {
        return this.type;
    }
}
