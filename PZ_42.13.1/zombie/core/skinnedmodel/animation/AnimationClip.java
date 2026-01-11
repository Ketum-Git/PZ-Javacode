// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import zombie.util.list.PZArrayUtil;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class AnimationClip {
    public final String name;
    public final boolean isRagdoll;
    public final boolean keepLastFrame;
    private final float duration;
    private final AnimationClip.KeyframeByBoneIndexElement[] keyFramesByBoneIndex;
    private final List<Keyframe> rootMotionKeyframes = new ArrayList<>();
    private final Keyframe[] keyframeArray;
    private boolean isRagdollSimulationActive;

    public AnimationClip(float duration, List<Keyframe> keyframes, String name, boolean bKeepLastFrame) {
        this(duration, keyframes, name, bKeepLastFrame, false);
    }

    public AnimationClip(float duration, List<Keyframe> keyframes, String name, boolean bKeepLastFrame, boolean isRagdoll) {
        this.name = name;
        this.isRagdoll = isRagdoll;
        this.duration = duration;
        this.keepLastFrame = bKeepLastFrame;
        this.keyframeArray = keyframes.toArray(new Keyframe[0]);
        this.keyFramesByBoneIndex = new AnimationClip.KeyframeByBoneIndexElement[60];
        this.recalculateKeyframesByBoneIndex();
    }

    public Keyframe getKeyframe(int keyframeIndex) {
        return this.keyframeArray[keyframeIndex];
    }

    public Keyframe[] getBoneFramesAt(int idx) {
        return this.keyFramesByBoneIndex[idx].keyframes;
    }

    public int getRootMotionFrameCount() {
        return this.rootMotionKeyframes.size();
    }

    public Keyframe getRootMotionFrameAt(int idx) {
        return this.rootMotionKeyframes.get(idx);
    }

    public Keyframe[] getKeyframes() {
        return this.keyframeArray;
    }

    public float getDuration() {
        return this.duration;
    }

    private AnimationClip.KeyframeByBoneIndexElement getKeyframesForBone(int in_boneIdx) {
        return this.keyFramesByBoneIndex[in_boneIdx];
    }

    public Keyframe[] getKeyframesForBone(int in_boneIdx, Keyframe[] inout_keyframesForBone) {
        AnimationClip.KeyframeByBoneIndexElement allFrames = this.getKeyframesForBone(in_boneIdx);
        int numKeyframes = allFrames.keyframes.length;
        if (PZArrayUtil.lengthOf(inout_keyframesForBone) < numKeyframes) {
            inout_keyframesForBone = PZArrayUtil.newInstance(Keyframe.class, inout_keyframesForBone, numKeyframes, false, Keyframe::new);
        }

        PZArrayUtil.arrayCopy(inout_keyframesForBone, allFrames.keyframes);
        return inout_keyframesForBone;
    }

    public boolean isRagdollSimulationActive() {
        return this.isRagdollSimulationActive;
    }

    public void setRagdollSimulationActive(boolean in_val) {
        this.isRagdollSimulationActive = in_val;
    }

    public float getTranslationLength(BoneAxis deferredBoneAxis) {
        float x = this.keyframeArray[this.keyframeArray.length - 1].position.x - this.keyframeArray[0].position.x;
        float y;
        if (deferredBoneAxis == BoneAxis.Y) {
            y = -this.keyframeArray[this.keyframeArray.length - 1].position.z + this.keyframeArray[0].position.z;
        } else {
            y = this.keyframeArray[this.keyframeArray.length - 1].position.y - this.keyframeArray[0].position.y;
        }

        return (float)Math.sqrt(x * x + y * y);
    }

    public void recalculateKeyframesByBoneIndex() {
        ArrayList<Keyframe> bkf = new ArrayList<>();
        int frameCount = this.keyframeArray.length > 1 ? this.keyframeArray.length - (this.keepLastFrame ? 0 : 1) : 1;

        for (int boneIdx = 0; boneIdx < 60; boneIdx++) {
            bkf.clear();

            for (int k = 0; k < frameCount; k++) {
                Keyframe keyframe = this.keyframeArray[k];
                if (keyframe.none == boneIdx) {
                    bkf.add(keyframe);
                }
            }

            this.keyFramesByBoneIndex[boneIdx] = new AnimationClip.KeyframeByBoneIndexElement(bkf);
        }
    }

    private static class KeyframeByBoneIndexElement {
        final Keyframe[] keyframes;

        KeyframeByBoneIndexElement(List<Keyframe> keyframes) {
            this.keyframes = keyframes.toArray(new Keyframe[0]);
        }
    }
}
