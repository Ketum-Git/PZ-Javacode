// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntries;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntry;

public final class AnimationTrackRecordingFrame extends GenericNameWeightRecordingFrame {
    public AnimationTrackRecordingFrame(String fileKey) {
        super(fileKey);
    }

    @Override
    public void reset() {
        super.reset();
    }

    public void logAnimWeights(LiveAnimationTrackEntries trackEntries) {
        for (int i = 0; i < trackEntries.count(); i++) {
            LiveAnimationTrackEntry trackEntry = trackEntries.get(i);
            float animWeight = trackEntry.getBlendWeight();
            AnimationTrack track = trackEntry.getTrack();
            String animName = track.getName();
            int layer = track.getLayerIdx();
            this.logWeight(animName, layer, animWeight);
        }
    }
}
