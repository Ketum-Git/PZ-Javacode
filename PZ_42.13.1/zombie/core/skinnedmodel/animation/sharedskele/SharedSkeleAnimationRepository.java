// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.sharedskele;

import java.util.HashMap;
import zombie.core.skinnedmodel.animation.AnimationClip;

public class SharedSkeleAnimationRepository {
    private final HashMap<AnimationClip, SharedSkeleAnimationTrack> tracksMap = new HashMap<>();

    public SharedSkeleAnimationTrack getTrack(AnimationClip clip) {
        return this.tracksMap.get(clip);
    }

    public void setTrack(AnimationClip clip, SharedSkeleAnimationTrack track) {
        this.tracksMap.put(clip, track);
    }
}
