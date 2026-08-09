// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import zombie.UsedFromLua;

@UsedFromLua
public final class MusicIntensityEvent {
    private final String id;
    private final float intensity;
    private final long durationMs;
    private long elapsedTimeMs;

    public MusicIntensityEvent(String label, float intensity, long durationMs) {
        this.id = label;
        this.intensity = intensity;
        this.durationMs = durationMs;
    }

    public String getId() {
        return this.id;
    }

    public float getIntensity() {
        return this.intensity;
    }

    public long getDuration() {
        return this.durationMs;
    }

    public long getElapsedTime() {
        return this.elapsedTimeMs;
    }

    public void setElapsedTime(long milliseconds) {
        this.elapsedTimeMs = milliseconds;
    }
}
