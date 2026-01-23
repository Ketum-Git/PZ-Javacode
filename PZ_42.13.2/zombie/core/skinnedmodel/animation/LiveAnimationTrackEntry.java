// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class LiveAnimationTrackEntry extends PooledObject {
    private float animBlendWeight = -1.0F;
    private int animBlendLayer = -1;
    private int animBlendPriority = -1;
    private AnimationTrack animTrack;
    private static final Pool<LiveAnimationTrackEntry> s_pool = new Pool<>(LiveAnimationTrackEntry::new);

    @Override
    public void onReleased() {
        this.reset();
    }

    public void reset() {
        this.animBlendWeight = -1.0F;
        this.animBlendLayer = -1;
        this.animBlendPriority = -1;
        this.animTrack = null;
    }

    public static LiveAnimationTrackEntry alloc() {
        return s_pool.alloc();
    }

    public static LiveAnimationTrackEntry alloc(AnimationTrack in_track) {
        LiveAnimationTrackEntry newEntry = alloc();
        newEntry.animBlendWeight = in_track.getBlendWeight();
        newEntry.animBlendLayer = in_track.getLayerIdx();
        newEntry.animBlendPriority = in_track.getPriority();
        newEntry.animTrack = in_track;
        return newEntry;
    }

    public float getBlendWeight() {
        return this.animBlendWeight;
    }

    public void setBlendWeight(float in_blendWeight) {
        this.animBlendWeight = in_blendWeight;
    }

    public int getLayer() {
        return this.animBlendLayer;
    }

    public int getPriority() {
        return this.animBlendPriority;
    }

    public AnimationTrack getTrack() {
        return this.animTrack;
    }
}
