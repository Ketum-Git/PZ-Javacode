// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import zombie.UsedFromLua;

@UsedFromLua
public final class MusicThreatStatus {
    private final String id;
    private float intensity;

    public MusicThreatStatus(String label, float intensity) {
        this.id = label;
        this.intensity = intensity;
    }

    public String getId() {
        return this.id;
    }

    public float getIntensity() {
        return MusicThreatConfig.getInstance().isStatusIntensityOverridden(this.getId())
            ? MusicThreatConfig.getInstance().getStatusIntensityOverride(this.getId())
            : this.intensity;
    }

    public void setIntensity(float value) {
        this.intensity = value;
    }
}
