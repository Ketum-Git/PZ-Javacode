// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.PerformanceSettings;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.PooledAnimBoneWeightArray;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.PooledFloatArrayObject;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.lambda.Consumers;
import zombie.util.list.IntMap;
import zombie.util.list.PZArrayUtil;

/**
 * Created by LEMMYPC on 07/01/14.
 */
public final class AnimationTrack extends PooledObject {
    public boolean isPlaying;
    public boolean isPrimary;
    public AnimationClip currentClip;
    public int priority;
    private boolean isRagdollFirstFrame;
    public float ragdollStartTime;
    public float ragdollMaxTime = 5.0F;
    private float currentTimeValue;
    private float previousTimeValue;
    public boolean syncTrackingEnabled;
    public boolean reverse;
    public boolean looping;
    private final AnimationTrack.KeyframeSpan[] pose = new AnimationTrack.KeyframeSpan[60];
    private final AnimationTrack.KeyframeSpan deferredPoseSpan = new AnimationTrack.KeyframeSpan();
    private IntMap<BoneTransform> poseAdjustments;
    private float speedDelta;
    private float blendWeight;
    private float blendFieldWeight;
    public IInterpolator blendCurve;
    private String name;
    private String matchingGrappledAnimNode;
    private boolean isGrappler;
    public float earlyBlendOutTime;
    public boolean triggerOnNonLoopedAnimFadeOutEvent;
    private int layerIdx;
    public AnimLayer animLayer;
    private PooledArrayObject<AnimBoneWeight> boneWeightBindings;
    private PooledFloatArrayObject boneWeights;
    private final ArrayList<IAnimListener> listeners = new ArrayList<>();
    private final ArrayList<IAnimListener> listenersInvoking = new ArrayList<>();
    private SkinningBone deferredBone;
    private BoneAxis deferredBoneAxis;
    private boolean useDeferredMovement = true;
    private boolean useDeferredRotation;
    private float deferredRotationScale = 1.0F;
    private final AnimationTrack.DeferredMotionData deferredMotion = new AnimationTrack.DeferredMotionData();
    public boolean isInitialAdjustmentCalculated;
    public final Vector3f initialAdjustment = new Vector3f();
    private static final Pool<AnimationTrack> s_pool = new Pool<>(AnimationTrack::new);

    public static AnimationTrack alloc() {
        return s_pool.alloc();
    }

    protected AnimationTrack() {
        PZArrayUtil.arrayPopulate(this.pose, AnimationTrack.KeyframeSpan::new);
        this.resetInternal();
    }

    private AnimationTrack resetInternal() {
        this.isPlaying = false;
        this.isPrimary = false;
        this.currentClip = null;
        this.priority = 0;
        this.isRagdollFirstFrame = false;
        this.ragdollStartTime = 0.0F;
        this.ragdollMaxTime = 5.0F;
        this.currentTimeValue = 0.0F;
        this.previousTimeValue = 0.0F;
        this.syncTrackingEnabled = true;
        this.reverse = false;
        this.looping = false;
        PZArrayUtil.forEach(this.pose, AnimationTrack.KeyframeSpan::clear);
        this.deferredPoseSpan.clear();
        this.poseAdjustments = Pool.tryRelease(this.poseAdjustments);
        this.speedDelta = 1.0F;
        this.blendWeight = 0.0F;
        this.blendFieldWeight = 1.0F;
        this.blendCurve = null;
        this.name = "!Empty!";
        this.earlyBlendOutTime = 0.0F;
        this.triggerOnNonLoopedAnimFadeOutEvent = false;
        this.animLayer = null;
        this.layerIdx = -1;
        this.boneWeightBindings = Pool.tryRelease(this.boneWeightBindings);
        this.boneWeights = Pool.tryRelease(this.boneWeights);
        this.listeners.clear();
        this.listenersInvoking.clear();
        this.deferredBone = null;
        this.deferredBoneAxis = BoneAxis.Y;
        this.useDeferredMovement = true;
        this.useDeferredRotation = false;
        this.deferredRotationScale = 1.0F;
        this.deferredMotion.reset();
        this.isInitialAdjustmentCalculated = false;
        return this;
    }

    public void get(int bone, Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
        this.pose[bone].lerp(this.getCurrentAnimationTime(), out_pos, out_rot, out_scale);
        if (this.poseAdjustments != null && !this.poseAdjustments.isEmpty()) {
            BoneTransform poseAdjustment = this.poseAdjustments.get(bone);
            if (poseAdjustment != null) {
                BoneTransform l_boneTransform = BoneTransform.alloc();
                l_boneTransform.set(out_pos, out_rot, out_scale);
                BoneTransform.mul(poseAdjustment, l_boneTransform, l_boneTransform);
                l_boneTransform.getPRS(out_pos, out_rot, out_scale);
                Pool.tryRelease(l_boneTransform);
            }
        }
    }

    public void setBonePoseAdjustment(int bone, Vector3f in_pos, Quaternion in_rot, Vector3f in_scale) {
        BoneTransform poseAdjustment = BoneTransform.alloc();
        poseAdjustment.set(in_pos, in_rot, in_scale);
        if (this.poseAdjustments == null) {
            this.poseAdjustments = IntMap.alloc();
        }

        this.poseAdjustments.set(bone, poseAdjustment);
    }

    private Keyframe getDeferredMovementFrameAt(int boneIdx, float time, Keyframe out_result) {
        AnimationTrack.KeyframeSpan span = this.getKeyframeSpan(boneIdx, time, this.deferredPoseSpan);
        return span.lerp(time, out_result);
    }

    private AnimationTrack.KeyframeSpan getKeyframeSpan(int boneIdx, float time, AnimationTrack.KeyframeSpan in_out_result) {
        if (!in_out_result.isBone(boneIdx)) {
            in_out_result.clear();
        }

        Keyframe[] boneFrames = this.currentClip.getBoneFramesAt(boneIdx);
        int numFrames = boneFrames.length;
        if (numFrames == 0) {
            in_out_result.clear();
            return in_out_result;
        } else if (in_out_result.containsTime(time)) {
            return in_out_result;
        } else {
            Keyframe lastFrame = boneFrames[numFrames - 1];
            if (time >= lastFrame.time) {
                in_out_result.fromIdx = numFrames > 1 ? numFrames - 2 : 0;
                in_out_result.toIdx = numFrames - 1;
                in_out_result.from = boneFrames[in_out_result.fromIdx];
                in_out_result.to = boneFrames[in_out_result.toIdx];
                return in_out_result;
            } else {
                Keyframe firstFrame = boneFrames[0];
                if (time <= firstFrame.time) {
                    in_out_result.clear();
                    in_out_result.toIdx = 0;
                    in_out_result.to = firstFrame;
                    return in_out_result;
                } else {
                    int startIdx = 0;
                    if (in_out_result.isSpan() && in_out_result.to.time <= time) {
                        startIdx = in_out_result.toIdx;
                    }

                    in_out_result.clear();

                    for (int idx = startIdx; idx < numFrames - 1; idx++) {
                        Keyframe kcurr = boneFrames[idx];
                        Keyframe knext = boneFrames[idx + 1];
                        if (kcurr.time <= time && time <= knext.time) {
                            in_out_result.fromIdx = idx;
                            in_out_result.toIdx = idx + 1;
                            in_out_result.from = kcurr;
                            in_out_result.to = knext;
                            break;
                        }
                    }

                    return in_out_result;
                }
            }
        }
    }

    public void removeListener(IAnimListener listener) {
        this.listeners.remove(listener);
    }

    public void Update(float time) {
        try {
            this.UpdateKeyframes(time);
        } catch (Exception var3) {
            var3.printStackTrace();
        }
    }

    public void UpdateKeyframes(float dt) {
        try (AbstractPerformanceProfileProbe ignored = AnimationTrack.s_performance.updateKeyframes.profile()) {
            this.updateKeyframesInternal(dt);
        }
    }

    private void updateKeyframesInternal(float dt) {
        if (this.currentClip == null) {
            throw new RuntimeException("AnimationPlayer.Update was called before startClip");
        } else {
            if (dt > 0.0F) {
                this.TickCurrentTime(dt);
            }

            if (!GameServer.server || ServerGUI.isCreated()) {
                this.updatePose();
            }

            this.updateDeferredValues();
        }
    }

    private void updatePose() {
        try (AbstractPerformanceProfileProbe ignored = AnimationTrack.s_performance.updatePose.profile()) {
            this.updatePoseInternal();
        }
    }

    private void updatePoseInternal() {
        float currentTime = this.getCurrentAnimationTime();

        for (int n = 0; n < 60; n++) {
            this.getKeyframeSpan(n, currentTime, this.pose[n]);
        }
    }

    private void updateDeferredValues() {
        try (AbstractPerformanceProfileProbe ignored = AnimationTrack.s_performance.updateDeferredValues.profile()) {
            this.updateDeferredValuesInternal();
        }
    }

    private void updateDeferredValuesInternal() {
        if (this.deferredBone != null) {
            AnimationTrack.DeferredMotionData dm = this.deferredMotion;
            dm.deferredRotationDiff = 0.0F;
            dm.deferredMovementDiff.set(0.0F, 0.0F);
            dm.counterRotatedMovementDiff.set(0.0F, 0.0F);
            float prevTime = this.getPreviousAnimationTime();
            float currentTime = this.getCurrentAnimationTime();
            if (this.isLooping() && prevTime > currentTime) {
                float endTime = this.getDuration();
                this.appendDeferredValues(dm, prevTime, endTime);
                prevTime = 0.0F;
            }

            this.appendDeferredValues(dm, prevTime, currentTime);
        }
    }

    private void appendDeferredValues(AnimationTrack.DeferredMotionData dm, float prevTime, float currentTime) {
        int deferredBoneIdx = this.getDeferredMovementBoneIdx();
        Keyframe prevKeyFrame = this.getDeferredMovementFrameAt(deferredBoneIdx, prevTime, AnimationTrack.L_updateDeferredValues.prevKeyFrame);
        Keyframe keyFrame = this.getDeferredMovementFrameAt(deferredBoneIdx, currentTime, AnimationTrack.L_updateDeferredValues.keyFrame);
        if (!GameServer.server) {
            dm.prevDeferredRotation = this.getDeferredTwistRotation(prevKeyFrame.rotation);
            dm.targetDeferredRotationQ.set(keyFrame.rotation);
            dm.targetDeferredRotation = this.getDeferredTwistRotation(keyFrame.rotation);
            float angleDiff = PZMath.getClosestAngle(dm.prevDeferredRotation, dm.targetDeferredRotation);
            dm.deferredRotationDiff = dm.deferredRotationDiff + angleDiff * this.getDeferredRotationScale();
        }

        this.getDeferredMovement(prevKeyFrame.position, dm.prevDeferredMovement);
        dm.targetDeferredPosition.set(keyFrame.position);
        this.getDeferredMovement(keyFrame.position, dm.targetDeferredMovement);
        Vector2 diff = AnimationTrack.L_updateDeferredValues.diff
            .set(dm.targetDeferredMovement.x - dm.prevDeferredMovement.x, dm.targetDeferredMovement.y - dm.prevDeferredMovement.y);
        Vector2 crDiff = AnimationTrack.L_updateDeferredValues.crDiff.set(diff);
        if (this.getUseDeferredRotation() && !this.isRagdoll()) {
            float len = crDiff.normalize();
            crDiff.rotate(-(dm.targetDeferredRotation + (float) (Math.PI / 2)));
            crDiff.scale(-len);
        }

        dm.deferredMovementDiff.x = dm.deferredMovementDiff.x + diff.x;
        dm.deferredMovementDiff.y = dm.deferredMovementDiff.y + diff.y;
        dm.counterRotatedMovementDiff.x = dm.counterRotatedMovementDiff.x + crDiff.x;
        dm.counterRotatedMovementDiff.y = dm.counterRotatedMovementDiff.y + crDiff.y;
    }

    private float getDeferredTwistRotation(Quaternion boneRotation) {
        if (this.deferredBoneAxis == BoneAxis.Z) {
            return HelperFunctions.getRotationZ(boneRotation);
        } else if (this.deferredBoneAxis == BoneAxis.Y) {
            return HelperFunctions.getRotationY(boneRotation);
        } else {
            DebugLog.Animation.error("BoneAxis unhandled: %s", String.valueOf(this.deferredBoneAxis));
            return 0.0F;
        }
    }

    private Vector2 getDeferredMovement(Vector3f bonePos, Vector2 out_deferredPos) {
        if (this.deferredBoneAxis == BoneAxis.Y) {
            out_deferredPos.set(bonePos.x, -bonePos.z);
        } else {
            out_deferredPos.set(bonePos.x, bonePos.y);
        }

        return out_deferredPos;
    }

    public Vector3f getCurrentDeferredCounterPosition(Vector3f out_result) {
        this.getCurrentDeferredPosition(out_result);
        if (this.deferredBoneAxis == BoneAxis.Y) {
            out_result.set(-out_result.x, 0.0F, out_result.z);
        } else {
            out_result.set(-out_result.x, -out_result.y, 0.0F);
        }

        return out_result;
    }

    public float getCurrentDeferredRotation() {
        return this.deferredMotion.targetDeferredRotation;
    }

    public Vector3f getCurrentDeferredPosition(Vector3f out_result) {
        out_result.set(this.deferredMotion.targetDeferredPosition);
        return out_result;
    }

    public int getDeferredMovementBoneIdx() {
        return this.deferredBone != null ? this.deferredBone.index : -1;
    }

    public float getCurrentTrackTime() {
        return this.getReversibleTimeValue(this.currentTimeValue);
    }

    public float getPreviousTrackTime() {
        return this.getReversibleTimeValue(this.previousTimeValue);
    }

    public float getCurrentAnimationTime() {
        return this.isRagdoll() ? this.currentClip.getDuration() : this.getCurrentTrackTime();
    }

    public float getPreviousAnimationTime() {
        return this.isRagdoll() ? 0.0F : this.getPreviousTrackTime();
    }

    private float getReversibleTimeValue(float timeValue) {
        return this.reverse ? this.getDuration() - timeValue : timeValue;
    }

    protected void TickCurrentTime(float time) {
        try (AbstractPerformanceProfileProbe ignored = AnimationTrack.s_performance.tickCurrentTime.profile()) {
            this.tickCurrentTimeInternal(time);
        }
    }

    private void tickCurrentTimeInternal(float time) {
        time *= this.speedDelta;
        if (!this.isPlaying) {
            time = 0.0F;
        }

        float endDuration = this.getDuration();
        this.previousTimeValue = this.currentTimeValue;
        this.currentTimeValue += time;
        if (this.looping) {
            if (this.previousTimeValue == 0.0F && this.currentTimeValue > 0.0F) {
                this.invokeOnAnimStartedEvent();
            }

            if (this.currentTimeValue >= endDuration) {
                this.invokeOnLoopedAnimEvent();
                this.currentTimeValue %= endDuration;
                this.invokeOnAnimStartedEvent();
            }
        } else {
            if (this.currentTimeValue < 0.0F) {
                this.currentTimeValue = 0.0F;
            }

            if (this.previousTimeValue == 0.0F && this.currentTimeValue > 0.0F) {
                this.invokeOnAnimStartedEvent();
            }

            if (this.triggerOnNonLoopedAnimFadeOutEvent) {
                float earlyBlendOutAtTime = endDuration - this.earlyBlendOutTime;
                if (this.previousTimeValue < earlyBlendOutAtTime && earlyBlendOutAtTime <= this.currentTimeValue) {
                    this.invokeOnNonLoopedAnimFadeOutEvent();
                }
            }

            if (this.currentTimeValue > endDuration) {
                this.currentTimeValue = endDuration;
            }

            boolean isRagdoll = this.isRagdoll();
            if (!isRagdoll && this.previousTimeValue < endDuration && this.currentTimeValue >= endDuration) {
                if (this.looping) {
                    this.invokeOnLoopedAnimEvent();
                }

                this.invokeOnNonLoopedAnimFinishedEvent();
            }
        }
    }

    public float getDuration() {
        if (this.isRagdoll()) {
            return this.ragdollMaxTime;
        } else {
            return this.hasClip() ? this.currentClip.getDuration() : 0.0F;
        }
    }

    private void invokeListeners(Consumer<IAnimListener> invoker) {
        if (!this.listeners.isEmpty()) {
            this.listenersInvoking.clear();
            PZArrayUtil.addAll(this.listenersInvoking, this.listeners);

            for (int i = 0; i < this.listenersInvoking.size(); i++) {
                IAnimListener listener = this.listenersInvoking.get(i);
                invoker.accept(listener);
            }
        }
    }

    private <T1> void invokeListeners(T1 val1, Consumers.Params1.ICallback<IAnimListener, T1> invoker) {
        Lambda.capture(this, val1, invoker, (stack, l_this, l_val1, l_invoker) -> l_this.invokeListeners(stack.consumer(l_val1, l_invoker)));
    }

    protected void invokeOnAnimStartedEvent() {
        this.invokeListeners(this, IAnimListener::onAnimStarted);
    }

    protected void invokeOnLoopedAnimEvent() {
        this.invokeListeners(this, IAnimListener::onLoopedAnim);
    }

    protected void invokeOnNonLoopedAnimFadeOutEvent() {
        this.invokeListeners(this, IAnimListener::onNonLoopedAnimFadeOut);
    }

    protected void invokeOnNonLoopedAnimFinishedEvent() {
        this.invokeListeners(this, IAnimListener::onNonLoopedAnimFinished);
    }

    /**
     * onDestroyed
     *  Called by AnimationPlayer's ObjectPool, when this track has been released.
     *  
     *  Resets all internals, ready for reuse.
     *  
     *  Notifies all listeners that this track is to be discarded.
     */
    @Override
    public void onReleased() {
        if (!this.listeners.isEmpty()) {
            this.listenersInvoking.clear();
            PZArrayUtil.addAll(this.listenersInvoking, this.listeners);

            for (int i = 0; i < this.listenersInvoking.size(); i++) {
                IAnimListener listener = this.listenersInvoking.get(i);
                listener.onTrackDestroyed(this);
            }

            this.listeners.clear();
            this.listenersInvoking.clear();
        }

        this.reset();
    }

    public Vector2 getDeferredMovementDiff(Vector2 out_result) {
        out_result.set(this.deferredMotion.counterRotatedMovementDiff);
        return out_result;
    }

    public float getDeferredRotationDiff() {
        return this.deferredMotion.deferredRotationDiff;
    }

    public void addListener(IAnimListener listener) {
        this.listeners.add(listener);
    }

    public void startClip(AnimationClip clip, boolean loop, float in_ragdollMaxTime) {
        if (clip == null) {
            throw new NullPointerException("Supplied clip is null.");
        } else {
            this.reset();
            this.isPlaying = true;
            this.looping = loop;
            this.currentClip = clip;
            this.isRagdollFirstFrame = this.isRagdoll();
            this.ragdollMaxTime = in_ragdollMaxTime;
        }
    }

    public AnimationTrack reset() {
        return this.resetInternal();
    }

    public void setBoneWeights(List<AnimBoneWeight> boneWeights) {
        this.boneWeightBindings = PooledAnimBoneWeightArray.toArray(boneWeights);
        this.boneWeights = null;
    }

    public void initBoneWeights(SkinningData skinningData) {
        if (!this.hasBoneMask()) {
            if (this.boneWeightBindings != null) {
                if (this.boneWeightBindings.isEmpty()) {
                    this.boneWeights = PooledFloatArrayObject.alloc(0);
                } else {
                    this.boneWeights = PooledFloatArrayObject.alloc(skinningData.numBones());
                    PZArrayUtil.arraySet(this.boneWeights.array(), 0.0F);

                    for (int i = 0; i < this.boneWeightBindings.length(); i++) {
                        AnimBoneWeight weightBinding = this.boneWeightBindings.get(i);
                        this.initWeightBinding(skinningData, weightBinding);
                    }
                }
            }
        }
    }

    protected void initWeightBinding(SkinningData skinningData, AnimBoneWeight weightBinding) {
        if (weightBinding != null && !StringUtils.isNullOrEmpty(weightBinding.boneName)) {
            String boneName = weightBinding.boneName;
            SkinningBone bone = skinningData.getBone(boneName);
            if (bone == null) {
                DebugLog.Animation.error("Bone not found: %s", boneName);
            } else {
                float boneWeight = weightBinding.weight;
                this.assignBoneWeight(boneWeight, bone.index);
                if (weightBinding.includeDescendants) {
                    Lambda.forEach(
                        bone::forEachDescendant,
                        this,
                        boneWeight,
                        (descendantBone, l_this, l_boneWeight) -> l_this.assignBoneWeight(l_boneWeight, descendantBone.index)
                    );
                }
            }
        }
    }

    private void assignBoneWeight(float weight, int boneIdx) {
        if (!this.hasBoneMask()) {
            throw new NullPointerException("Bone weights array not initialized.");
        } else {
            float existingWeight = this.boneWeights.get(boneIdx);
            this.boneWeights.set(boneIdx, Math.max(weight, existingWeight));
        }
    }

    public float getBoneWeight(int boneIdx) {
        if (!this.hasBoneMask()) {
            return 1.0F;
        } else {
            return DebugOptions.instance.character.debug.animate.noBoneMasks.getValue()
                ? 1.0F
                : PZArrayUtil.getOrDefault(this.boneWeights.array(), boneIdx, 0.0F);
        }
    }

    public float getDeferredBoneWeight() {
        return this.deferredBone == null ? 0.0F : this.getBoneWeight(this.deferredBone.index);
    }

    public int getLayerIdx() {
        this.layerIdx = PZMath.max(this.layerIdx, this.animLayer != null ? this.animLayer.getDepth() : 0);
        return this.layerIdx;
    }

    public boolean hasBoneMask() {
        return this.boneWeights != null;
    }

    public boolean isLooping() {
        return this.looping;
    }

    public void setDeferredBone(SkinningBone bone, BoneAxis axis) {
        this.deferredBone = bone;
        this.deferredBoneAxis = axis;
    }

    public void setUseDeferredMovement(boolean val) {
        this.useDeferredMovement = val;
    }

    public boolean getUseDeferredMovement() {
        return this.useDeferredMovement;
    }

    public void setUseDeferredRotation(boolean val) {
        this.useDeferredRotation = val;
    }

    public boolean getUseDeferredRotation() {
        return this.useDeferredRotation;
    }

    public void setDeferredRotationScale(float deferredRotationScale) {
        this.deferredRotationScale = deferredRotationScale;
    }

    public float getDeferredRotationScale() {
        return this.deferredRotationScale;
    }

    public boolean isFinished() {
        return this.isRagdoll() ? !this.isRagdollSimulationActive() : !this.looping && this.getDuration() > 0.0F && this.currentTimeValue >= this.getDuration();
    }

    public float getCurrentTimeValue() {
        return this.currentTimeValue;
    }

    public void setCurrentTimeValue(float m_currentTimeValue) {
        this.currentTimeValue = m_currentTimeValue;
    }

    public float getPreviousTimeValue() {
        return this.previousTimeValue;
    }

    public void setPreviousTimeValue(float m_previousTimeValue) {
        this.previousTimeValue = m_previousTimeValue;
    }

    public void rewind(float rewindAmount) {
        this.advance(-rewindAmount);
    }

    public void scaledRewind(float rewindAmount) {
        this.scaledAdvance(-rewindAmount);
    }

    public void scaledAdvance(float advanceAmount) {
        this.advance(advanceAmount * this.speedDelta);
    }

    public void advance(float advanceAmount) {
        this.currentTimeValue = PZMath.wrap(this.currentTimeValue + advanceAmount, 0.0F, this.getDuration());
        this.previousTimeValue = PZMath.wrap(this.previousTimeValue + advanceAmount, 0.0F, this.getDuration());
    }

    public void advanceFraction(float advanceFraction) {
        this.advance(this.getDuration() * advanceFraction);
    }

    public void moveCurrentTimeValueTo(float target) {
        float diff = target - this.currentTimeValue;
        this.advance(diff);
    }

    public void moveCurrentTimeValueToFraction(float fraction) {
        float targetTime = this.getDuration() * fraction;
        this.moveCurrentTimeValueTo(targetTime);
    }

    public float getCurrentTimeFraction() {
        return this.hasClip() ? this.currentTimeValue / this.getDuration() : 0.0F;
    }

    public boolean hasClip() {
        return this.currentClip != null;
    }

    public AnimationClip getClip() {
        return this.currentClip;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isGrappler() {
        return this.isGrappler;
    }

    public static AnimationTrack createClone(AnimationTrack source, Supplier<AnimationTrack> allocator) {
        AnimationTrack newTrack = allocator.get();
        newTrack.isPlaying = source.isPlaying;
        newTrack.currentClip = source.currentClip;
        newTrack.priority = source.priority;
        newTrack.isRagdollFirstFrame = source.isRagdollFirstFrame;
        newTrack.currentTimeValue = source.currentTimeValue;
        newTrack.previousTimeValue = source.previousTimeValue;
        newTrack.syncTrackingEnabled = source.syncTrackingEnabled;
        newTrack.reverse = source.reverse;
        newTrack.looping = source.looping;
        newTrack.speedDelta = source.speedDelta;
        newTrack.blendWeight = source.blendWeight;
        newTrack.blendFieldWeight = source.blendFieldWeight;
        newTrack.name = source.name;
        newTrack.earlyBlendOutTime = source.earlyBlendOutTime;
        newTrack.triggerOnNonLoopedAnimFadeOutEvent = source.triggerOnNonLoopedAnimFadeOutEvent;
        newTrack.setAnimLayer(source.animLayer);
        newTrack.boneWeightBindings = PooledAnimBoneWeightArray.toArray(source.boneWeightBindings);
        newTrack.boneWeights = PooledFloatArrayObject.toArray(source.boneWeights);
        newTrack.deferredBone = source.deferredBone;
        newTrack.deferredBoneAxis = source.deferredBoneAxis;
        newTrack.useDeferredMovement = source.useDeferredMovement;
        newTrack.useDeferredRotation = source.useDeferredRotation;
        newTrack.deferredRotationScale = source.deferredRotationScale;
        newTrack.matchingGrappledAnimNode = source.matchingGrappledAnimNode;
        newTrack.isGrappler = source.isGrappler();
        return newTrack;
    }

    public String getMatchingGrappledAnimNode() {
        return this.matchingGrappledAnimNode;
    }

    public void setMatchingGrappledAnimNode(String matchingGrappledAnimNode) {
        this.matchingGrappledAnimNode = matchingGrappledAnimNode;
        this.isGrappler = !StringUtils.isNullOrWhitespace(this.matchingGrappledAnimNode);
    }

    public void setAnimLayer(AnimLayer in_animLayer) {
        if (this.animLayer != in_animLayer) {
            if (this.animLayer != null) {
                this.removeListener(this.animLayer);
            }

            this.animLayer = in_animLayer;
            this.layerIdx = -1;
            if (this.animLayer != null) {
                this.addListener(in_animLayer);
            }
        }
    }

    public boolean isRagdollFirstFrame() {
        return this.isRagdollFirstFrame;
    }

    public void initRagdollTransform(int bone, Vector3f in_pos, Quaternion in_rot, Vector3f in_scale) {
        if (!this.isRagdoll()) {
            DebugLog.Animation.warn("This track is not a ragdoll track: %s", this.getName());
        } else {
            Keyframe[] keyframes = this.currentClip.getBoneFramesAt(bone);

            for (Keyframe key : keyframes) {
                key.set(in_pos, in_rot, in_scale);
            }
        }
    }

    public boolean isRagdoll() {
        return this.currentClip != null && this.currentClip.isRagdoll;
    }

    public boolean isRagdollSimulationActive() {
        return this.isRagdoll() && this.currentClip.isRagdollSimulationActive();
    }

    private void initRagdollTransform(int boneIdx, TwistableBoneTransform in_boneTransform) {
        Vector3f pos = new Vector3f();
        Quaternion rot = new Quaternion();
        Vector3f scale = new Vector3f();
        in_boneTransform.getPRS(pos, rot, scale);
        this.initRagdollTransform(boneIdx, pos, rot, scale);
    }

    private void initRagdollTransform(int boneIdx, Matrix4f in_boneMatrix) {
        Vector3f pos = new Vector3f();
        Quaternion rot = new Quaternion();
        Vector3f scale = new Vector3f();
        HelperFunctions.getPosition(in_boneMatrix, pos);
        HelperFunctions.getRotation(in_boneMatrix, rot);
        scale.set(1.0F, 1.0F, 1.0F);
        this.initRagdollTransform(boneIdx, pos, rot, scale);
    }

    public void initRagdollTransforms(List<Matrix4f> in_boneMatrices) {
        for (int boneIdx = 0; boneIdx < in_boneMatrices.size(); boneIdx++) {
            this.initRagdollTransform(boneIdx, in_boneMatrices.get(boneIdx));
        }

        this.isRagdollFirstFrame = false;
        this.updatePose();
    }

    public void initRagdollTransforms(TwistableBoneTransform[] in_boneTransforms) {
        for (int boneIdx = 0; boneIdx < in_boneTransforms.length; boneIdx++) {
            this.initRagdollTransform(boneIdx, in_boneTransforms[boneIdx]);
        }

        this.isRagdollFirstFrame = false;
        this.updatePose();
    }

    public String getName() {
        return this.currentClip != null ? this.currentClip.name : "!Empty!";
    }

    public float getSpeedDelta() {
        return this.speedDelta;
    }

    public void setSpeedDelta(float speedDelta) {
        this.speedDelta = speedDelta;
    }

    public float getBlendWeight() {
        float curveWeight;
        if (this.blendCurve != null) {
            float currentTime = this.getCurrentTrackTime();
            float trackDuration = this.getDuration();
            float timeAlpha = currentTime / trackDuration;
            curveWeight = this.blendCurve.lerp(timeAlpha);
        } else {
            curveWeight = 1.0F;
        }

        return this.blendWeight * curveWeight * this.blendFieldWeight;
    }

    public void setBlendWeight(float blendWeight) {
        this.blendWeight = blendWeight;
    }

    public float getBlendFieldWeight() {
        return this.blendFieldWeight;
    }

    public void setBlendFieldWeight(float blendFieldWeight) {
        this.blendFieldWeight = blendFieldWeight;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static class DeferredMotionData {
        float targetDeferredRotation;
        float prevDeferredRotation;
        final Quaternion targetDeferredRotationQ = new Quaternion();
        final Vector3f targetDeferredPosition = new Vector3f();
        final Vector2 prevDeferredMovement = new Vector2();
        final Vector2 targetDeferredMovement = new Vector2();
        float deferredRotationDiff;
        final Vector2 deferredMovementDiff = new Vector2();
        final Vector2 counterRotatedMovementDiff = new Vector2();

        public void reset() {
            this.deferredRotationDiff = 0.0F;
            this.targetDeferredRotation = 0.0F;
            this.prevDeferredRotation = 0.0F;
            this.targetDeferredRotationQ.setIdentity();
            this.targetDeferredMovement.set(0.0F, 0.0F);
            this.targetDeferredPosition.set(0.0F, 0.0F, 0.0F);
            this.prevDeferredMovement.set(0.0F, 0.0F);
            this.deferredMovementDiff.set(0.0F, 0.0F);
            this.counterRotatedMovementDiff.set(0.0F, 0.0F);
        }
    }

    private static class KeyframeSpan {
        Keyframe from;
        Keyframe to;
        int fromIdx = -1;
        int toIdx = -1;

        void clear() {
            this.from = null;
            this.to = null;
            this.fromIdx = -1;
            this.toIdx = -1;
        }

        Keyframe lerp(float time, Keyframe out_result) {
            out_result.setIdentity();
            if (this.from == null && this.to == null) {
                return out_result;
            } else if (this.to == null) {
                out_result.set(this.from);
                return out_result;
            } else if (this.from == null) {
                out_result.set(this.to);
                return out_result;
            } else if (this.from == this.to) {
                out_result.set(this.to);
                return out_result;
            } else {
                return Keyframe.lerp(this.from, this.to, time, out_result);
            }
        }

        void lerp(float time, Vector3f out_pos, Quaternion out_rot, Vector3f out_scale) {
            if (this.from == null && this.to == null) {
                Keyframe.setIdentity(out_pos, out_rot, out_scale);
            } else if (this.to == null) {
                this.from.get(out_pos, out_rot, out_scale);
            } else if (this.from == null) {
                this.to.get(out_pos, out_rot, out_scale);
            } else if (this.from == this.to) {
                this.to.get(out_pos, out_rot, out_scale);
            } else if (!PerformanceSettings.interpolateAnims) {
                this.to.get(out_pos, out_rot, out_scale);
            } else {
                Keyframe.lerp(this.from, this.to, time, out_pos, out_rot, out_scale);
            }
        }

        boolean isSpan() {
            return this.from != null && this.to != null;
        }

        boolean isPost() {
            return (this.from == null || this.to == null) && this.from != this.to;
        }

        boolean isEmpty() {
            return this.from == null && this.to == null;
        }

        boolean containsTime(float time) {
            return this.isSpan() && this.from.time <= time && time <= this.to.time;
        }

        public boolean isBone(int boneIdx) {
            return this.from != null && this.from.none == boneIdx || this.to != null && this.to.none == boneIdx;
        }
    }

    private static class L_updateDeferredValues {
        static final Keyframe keyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0F, 1.0F, 1.0F));
        static final Keyframe prevKeyFrame = new Keyframe(new Vector3f(), new Quaternion(), new Vector3f(1.0F, 1.0F, 1.0F));
        static final Vector2 crDiff = new Vector2();
        static final Vector2 diff = new Vector2();
    }

    private static class s_performance {
        static final PerformanceProfileProbe tickCurrentTime = new PerformanceProfileProbe("AnimationTrack.tickCurrentTime");
        static final PerformanceProfileProbe updateKeyframes = new PerformanceProfileProbe("AnimationTrack.updateKeyframes");
        static final PerformanceProfileProbe updateDeferredValues = new PerformanceProfileProbe("AnimationTrack.updateDeferredValues");
        static final PerformanceProfileProbe updatePose = new PerformanceProfileProbe("AnimationTrack.updatePose");
    }
}
