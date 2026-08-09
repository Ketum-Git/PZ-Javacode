// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

/**
 * Created by LEMMYMAIN on 23/02/2015.
 */
public interface IAnimListener {
    void onAnimStarted(AnimationTrack track);

    void onLoopedAnim(AnimationTrack track);

    void onNonLoopedAnimFadeOut(AnimationTrack track);

    void onNonLoopedAnimFinished(AnimationTrack track);

    void onNoAnimConditionsPass();

    void onTrackDestroyed(AnimationTrack track);
}
