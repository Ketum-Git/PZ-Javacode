// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.jobs;

public class JobApplyChanges extends RegionJob {
    protected boolean saveToDisk;

    protected JobApplyChanges() {
        super(RegionJobType.ApplyChanges);
    }

    @Override
    protected void reset() {
        this.saveToDisk = false;
    }

    public void setSaveToDisk(boolean b) {
        this.saveToDisk = b;
    }

    public boolean isSaveToDisk() {
        return this.saveToDisk;
    }
}
