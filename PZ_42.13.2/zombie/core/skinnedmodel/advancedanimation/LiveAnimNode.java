// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.Vector3;
import zombie.util.IPooledObject;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.util.list.PZEmptyIterable;

/**
 * The Live version of an AnimNode
 *  The AnimNode represents the data.
 *  The LiveAnimNode represents the playback of said data, its blend weights, timing, transitions etc.
 */
public class LiveAnimNode extends PooledObject implements IAnimListener {
    private AnimNode sourceNode;
    private AnimLayer animLayer;
    private boolean active;
    private boolean wasActive;
    boolean transitioningOut;
    private float weight;
    private float rawWeight;
    private boolean isNew;
    public boolean isBlendField;
    public AnimationTrack runningRagdollTrack;
    private final LiveAnimNode.TransitionIn transitionIn = new LiveAnimNode.TransitionIn();
    private final List<AnimationTrack> animationTracks = new ArrayList<>();
    final List<AnimationTrack> ragdollTracks = new ArrayList<>();
    float nodeAnimTime;
    float prevNodeAnimTime;
    private boolean blendingIn;
    private boolean blendingOut;
    private AnimTransition transitionOut;
    private boolean tweeningInGrapple;
    private boolean tweeningInGrappleFinished;
    private final Vector3f grappleTweenStartPos = new Vector3f();
    private final ArrayList<LiveAnimNode.WhileAliveFlag> whileAliveFlags = new ArrayList<>();
    private static final Pool<LiveAnimNode> s_pool = new Pool<>(LiveAnimNode::new);
    private String cachedRandomAnim = "";

    protected LiveAnimNode() {
    }

    public static LiveAnimNode alloc(AnimLayer animLayer, AnimNode sourceNode) {
        synchronized (s_pool) {
            LiveAnimNode newNode = s_pool.alloc();
            newNode.reset();
            newNode.sourceNode = sourceNode;
            newNode.animLayer = animLayer;
            return newNode;
        }
    }

    private void reset() {
        this.decrementWhileAliveFlags();
        this.sourceNode = null;
        this.animLayer = null;
        this.active = false;
        this.wasActive = false;
        this.transitioningOut = false;
        this.weight = 0.0F;
        this.rawWeight = 0.0F;
        this.isNew = true;
        this.isBlendField = false;
        this.transitionIn.reset();
        this.animationTracks.clear();
        this.ragdollTracks.clear();
        this.runningRagdollTrack = null;
        this.nodeAnimTime = 0.0F;
        this.prevNodeAnimTime = 0.0F;
        this.blendingIn = false;
        this.blendingOut = false;
        this.transitionOut = null;
        this.tweeningInGrapple = false;
        this.tweeningInGrappleFinished = false;
        Pool.tryRelease(this.whileAliveFlags);
        this.cachedRandomAnim = "";
    }

    @Override
    public void onReleased() {
        this.removeAllTracks();
        this.reset();
    }

    public String getName() {
        return this.sourceNode.name;
    }

    public boolean isBlendingIn() {
        return this.blendingIn;
    }

    public boolean isBlendingOut() {
        return this.blendingOut;
    }

    public boolean isTransitioningIn() {
        return this.transitionIn.active && this.transitionIn.track != null;
    }

    public void startTransitionIn(LiveAnimNode transitionFrom, AnimTransition transitionIn, AnimationTrack track) {
        this.startTransitionIn(transitionFrom.getSourceNode(), transitionIn, track);
    }

    public void startTransitionIn(AnimNode transitionFrom, AnimTransition transitionIn, AnimationTrack track) {
        if (this.transitionIn.track != null) {
            DebugLog.Animation
                .debugln(
                    "Removing existing TransitioningIn track: %s. Replaced by: %s", this.transitionIn.track.getName(), track != null ? track.getName() : "null"
                );
            this.stopTransitionIn();
        }

        this.transitionIn.active = track != null;
        this.transitionIn.transitionedFrom = transitionFrom.name;
        this.transitionIn.data = transitionIn;
        this.transitionIn.track = track;
        this.transitionIn.weight = 0.0F;
        this.transitionIn.rawWeight = 0.0F;
        this.transitionIn.blendingIn = true;
        this.transitionIn.blendingOut = false;
        this.transitionIn.time = 0.0F;
        if (this.transitionIn.track != null) {
            this.transitionIn.track.addListener(this);
        }

        this.setMainTracksPlaying(false);
    }

    public void stopTransitionIn() {
        this.removeTrack(this.transitionIn.track);
        this.transitionIn.reset();
    }

    private void removeTrack(AnimationTrack in_track) {
        AnimationMultiTrack rootTrack = this.animLayer.getAnimationTrack();
        if (rootTrack != null) {
            rootTrack.removeTrack(in_track);
        }
    }

    public void removeAllTracks() {
        AnimationMultiTrack multiTrack = this.animLayer.getAnimationTrack();
        if (multiTrack != null) {
            multiTrack.removeTracks(this.animationTracks);
            multiTrack.removeTracks(this.ragdollTracks);
            multiTrack.removeTrack(this.runningRagdollTrack);
            multiTrack.removeTrack(this.getTransitionInTrack());
        } else {
            IPooledObject.release(this.animationTracks);
            IPooledObject.release(this.ragdollTracks);
            Pool.tryRelease(this.runningRagdollTrack);
            Pool.tryRelease(this.getTransitionInTrack());
        }
    }

    public void setTransitionOut(AnimTransition transitionOut) {
        this.transitionOut = transitionOut;
    }

    public void update(float timeDelta) {
        this.isNew = false;
        if (this.active != this.wasActive) {
            this.blendingIn = this.active;
            this.blendingOut = !this.active;
            if (this.transitionIn.active) {
                this.transitionIn.blendingIn = this.active;
                this.transitionIn.blendingOut = !this.active;
            }

            this.wasActive = this.active;
        }

        boolean wasMainAnimActive = this.isMainAnimActive();
        if (this.isTransitioningIn()) {
            this.updateTransitioningIn(timeDelta);
        }

        boolean mainAnimActive = this.isMainAnimActive();
        if (mainAnimActive) {
            if (this.blendingOut && this.sourceNode.stopAnimOnExit) {
                this.setMainTracksPlaying(false);
            } else {
                this.setMainTracksPlaying(true);
            }
        } else {
            this.setMainTracksPlaying(false);
        }

        if (mainAnimActive) {
            boolean mainAnimStarted = !wasMainAnimActive;
            if (mainAnimStarted && this.isLooped()) {
                float rewindAmount = this.getMainInitialRewindTime();
                PZArrayUtil.forEach(this.animationTracks, Lambda.consumer(rewindAmount, AnimationTrack::scaledRewind));
            }

            if (this.blendingIn) {
                this.updateBlendingIn(timeDelta);
            } else if (this.blendingOut) {
                this.updateBlendingOut(timeDelta);
            }

            this.prevNodeAnimTime = this.nodeAnimTime;
            this.nodeAnimTime += timeDelta;
            if (!this.transitionIn.active && this.transitionIn.track != null && this.transitionIn.track.getBlendWeight() <= 0.0F) {
                this.stopTransitionIn();
            }

            this.updateRagdollTracks();
        }
    }

    private void updateRagdollTracks() {
        if (!this.ragdollTracks.isEmpty()) {
            if (this.runningRagdollTrack == null) {
                AnimationTrack foundRagdollTrack = null;

                for (int i = 0; i < this.ragdollTracks.size(); i++) {
                    AnimationTrack track = this.ragdollTracks.get(i);
                    if (!(track.ragdollStartTime > this.nodeAnimTime)) {
                        if (foundRagdollTrack == null) {
                            foundRagdollTrack = track;
                        } else if (track.ragdollStartTime < foundRagdollTrack.ragdollStartTime) {
                            foundRagdollTrack = track;
                        }
                    }
                }

                if (foundRagdollTrack != null) {
                    this.runningRagdollTrack = foundRagdollTrack;
                    this.runningRagdollTrack.setBlendFieldWeight(0.0F);
                }
            }

            if (this.runningRagdollTrack != null) {
                if (this.animationTracks.isEmpty()) {
                    this.runningRagdollTrack.setBlendFieldWeight(1.0F);
                } else if (this.runningRagdollTrack.getBlendFieldWeight() != 1.0F) {
                    AnimationTrack ragdollTrack = this.runningRagdollTrack;
                    float timeDiff = this.nodeAnimTime - ragdollTrack.ragdollStartTime;
                    float ragdollBlendInTime = this.sourceNode.blendTime;
                    float newRagdollTrackWeight;
                    if (timeDiff > 0.0F && timeDiff <= ragdollBlendInTime) {
                        newRagdollTrackWeight = timeDiff / ragdollBlendInTime;
                    } else if (timeDiff > ragdollBlendInTime) {
                        newRagdollTrackWeight = 1.0F;
                    } else {
                        newRagdollTrackWeight = 0.0F;
                    }

                    this.runningRagdollTrack.setBlendFieldWeight(PZMath.max(this.runningRagdollTrack.getBlendFieldWeight(), newRagdollTrackWeight));
                }
            }
        }
    }

    private void updateTransitioningIn(float timeDelta) {
        float speedDelta = this.transitionIn.track.getSpeedDelta();
        float transitionDuration = this.transitionIn.track.getDuration();
        this.transitionIn.time = this.transitionIn.track.getCurrentTimeValue();
        if (!(this.transitionIn.time >= transitionDuration) && !DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            if (!this.transitionIn.blendingOut) {
                boolean conditionsPass = AnimCondition.pass(this.animLayer.getVariableSource(), this.transitionIn.data.conditions);
                if (!conditionsPass) {
                    this.transitionIn.blendingIn = false;
                    this.transitionIn.blendingOut = true;
                }
            }

            float blendOutTime = this.getTransitionInBlendOutTime() * speedDelta;
            if (this.transitionIn.time >= transitionDuration - blendOutTime) {
                this.transitionIn.blendingIn = false;
                this.transitionIn.blendingOut = true;
            }

            if (this.transitionIn.blendingIn) {
                float blendTimeMax = this.getTransitionInBlendInTime() * speedDelta;
                float nextTime = this.incrementBlendTime(this.transitionIn.rawWeight, blendTimeMax, timeDelta * speedDelta);
                float rawWeight = PZMath.clamp(nextTime / blendTimeMax, 0.0F, 1.0F);
                this.transitionIn.rawWeight = rawWeight;
                this.transitionIn.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
                this.transitionIn.blendingIn = nextTime < blendTimeMax;
                this.transitionIn.active = nextTime < transitionDuration;
            }

            if (this.transitionIn.blendingOut) {
                float blendTimeMax = this.getTransitionInBlendOutTime() * speedDelta;
                float nextTime = this.incrementBlendTime(1.0F - this.transitionIn.rawWeight, blendTimeMax, timeDelta * speedDelta);
                float rawWeight = PZMath.clamp(1.0F - nextTime / blendTimeMax, 0.0F, 1.0F);
                this.transitionIn.rawWeight = rawWeight;
                this.transitionIn.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
                this.transitionIn.blendingOut = nextTime < blendTimeMax;
                this.transitionIn.active = this.transitionIn.blendingOut;
            }
        } else {
            this.stopTransitionIn();
        }
    }

    public void addMainTrack(AnimationTrack track) {
        if (!this.isLooped() && !this.sourceNode.stopAnimOnExit && this.sourceNode.earlyTransitionOut) {
            float blendOutTime = this.getBlendOutTime();
            if (blendOutTime > 0.0F && Float.isFinite(blendOutTime)) {
                track.earlyBlendOutTime = blendOutTime;
                track.triggerOnNonLoopedAnimFadeOutEvent = true;
            }
        }

        if (track.isRagdoll()) {
            this.ragdollTracks.add(track);
        } else {
            this.animationTracks.add(track);
        }
    }

    private void setMainTracksPlaying(boolean arePlaying) {
        for (int i = 0; i < this.animationTracks.size(); i++) {
            AnimationTrack track = this.animationTracks.get(i);
            track.isPlaying = arePlaying;
        }

        if (this.runningRagdollTrack != null) {
            this.runningRagdollTrack.isPlaying = arePlaying;
        }
    }

    private void updateBlendingIn(float timeDelta) {
        float blendTimeMax = this.getBlendInTime();
        if (!(blendTimeMax <= 0.0F) && !DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            float nextTime = this.incrementBlendTime(this.rawWeight, blendTimeMax, timeDelta);
            float rawWeight = PZMath.clamp(nextTime / blendTimeMax, 0.0F, 1.0F);
            this.rawWeight = rawWeight;
            this.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
            this.blendingIn = nextTime < blendTimeMax;
        } else {
            this.stopBlendingIn();
        }
    }

    private void updateBlendingOut(float timeDelta) {
        float blendTimeMax = this.getBlendOutTime();
        if (!(blendTimeMax <= 0.0F) && !DebugOptions.instance.animation.disableAnimationBlends.getValue()) {
            float nextTime = this.incrementBlendTime(1.0F - this.rawWeight, blendTimeMax, timeDelta);
            float rawWeight = PZMath.clamp(1.0F - nextTime / blendTimeMax, 0.0F, 1.0F);
            this.rawWeight = rawWeight;
            this.weight = PZMath.lerpFunc_EaseOutInQuad(rawWeight);
            this.blendingOut = nextTime < blendTimeMax;
        } else {
            this.stopBlendingOut();
        }
    }

    private void stopBlendingOut() {
        this.setWeightsToZero();
        this.blendingOut = false;
    }

    private void stopBlendingIn() {
        this.setWeightsToFull();
        this.blendingIn = false;
    }

    public void setWeightsToZero() {
        this.weight = 0.0F;
        this.rawWeight = 0.0F;
    }

    public void setWeightsToFull() {
        this.weight = 1.0F;
        this.rawWeight = 1.0F;
    }

    private float incrementBlendTime(float initialWeight, float blendTimeMax, float timeDelta) {
        float prevTime = initialWeight * blendTimeMax;
        return prevTime + timeDelta;
    }

    public float getTransitionInBlendInTime() {
        return this.transitionIn.data != null && this.transitionIn.data.blendInTime != Float.POSITIVE_INFINITY ? this.transitionIn.data.blendInTime : 0.0F;
    }

    public float getMainInitialRewindTime() {
        float randomAdvanceTime = 0.0F;
        if (this.sourceNode.randomAdvanceFraction > 0.0F) {
            float advanceFrac = Rand.Next(0.0F, this.sourceNode.randomAdvanceFraction);
            randomAdvanceTime = advanceFrac * this.getMaxDuration();
        }

        if (this.transitionIn.data == null) {
            return 0.0F - randomAdvanceTime;
        } else {
            float blendInTime = this.getTransitionInBlendOutTime();
            float syncAdjustTime = this.transitionIn.data.syncAdjustTime;
            return this.transitionIn.track != null ? blendInTime - syncAdjustTime : blendInTime - syncAdjustTime - randomAdvanceTime;
        }
    }

    private float getMaxDuration() {
        float maxDuration = 0.0F;
        int i = 0;

        for (int count = this.animationTracks.size(); i < count; i++) {
            AnimationTrack track = this.animationTracks.get(i);
            float duration = track.getDuration();
            maxDuration = PZMath.max(duration, maxDuration);
        }

        i = 0;

        for (int count = this.ragdollTracks.size(); i < count; i++) {
            AnimationTrack track = this.ragdollTracks.get(i);
            float duration = track.getDuration();
            maxDuration = PZMath.max(duration, maxDuration);
        }

        return maxDuration;
    }

    public float getTransitionInBlendOutTime() {
        return this.getBlendInTime();
    }

    public float getBlendInTime() {
        if (this.transitionIn.data == null) {
            return this.sourceNode.blendTime;
        } else if (this.transitionIn.track != null && this.transitionIn.data.blendOutTime != Float.POSITIVE_INFINITY) {
            return this.transitionIn.data.blendOutTime;
        } else {
            if (this.transitionIn.track == null) {
                if (this.transitionIn.data.blendInTime != Float.POSITIVE_INFINITY) {
                    return this.transitionIn.data.blendInTime;
                }

                if (this.transitionIn.data.blendOutTime != Float.POSITIVE_INFINITY) {
                    return this.transitionIn.data.blendOutTime;
                }
            }

            return this.sourceNode.blendTime;
        }
    }

    public float getBlendOutTime() {
        if (this.transitionOut == null) {
            return this.sourceNode.getBlendOutTime();
        } else if (!StringUtils.isNullOrWhitespace(this.transitionOut.animName) && this.transitionOut.blendInTime != Float.POSITIVE_INFINITY) {
            return this.transitionOut.blendInTime;
        } else {
            if (StringUtils.isNullOrWhitespace(this.transitionOut.animName)) {
                if (this.transitionOut.blendOutTime != Float.POSITIVE_INFINITY) {
                    return this.transitionOut.blendOutTime;
                }

                if (this.transitionOut.blendInTime != Float.POSITIVE_INFINITY) {
                    return this.transitionOut.blendInTime;
                }
            }

            return this.sourceNode.getBlendOutTime();
        }
    }

    @Override
    public void onAnimStarted(AnimationTrack track) {
        this.invokeAnimStartTimeEvent(track);
    }

    @Override
    public void onLoopedAnim(AnimationTrack track) {
        if (!this.transitioningOut) {
            this.invokeAnimEndTimeEvent(track);
        }
    }

    @Override
    public void onNonLoopedAnimFadeOut(AnimationTrack track) {
        if (DebugOptions.instance.animation.allowEarlyTransitionOut.getValue()) {
            this.invokeAnimEndTimeEvent(track);
            this.transitioningOut = true;
            this.decrementWhileAliveFlags();
        }
    }

    @Override
    public void onNonLoopedAnimFinished(AnimationTrack track) {
        if (!this.transitioningOut) {
            this.invokeAnimEndTimeEvent(track);
            this.decrementWhileAliveFlags();
        }
    }

    @Override
    public void onTrackDestroyed(AnimationTrack track) {
        this.animationTracks.remove(track);
        this.ragdollTracks.remove(track);
        if (this.runningRagdollTrack == track) {
            this.runningRagdollTrack = null;
        }

        if (this.transitionIn.track == track) {
            this.transitionIn.track = null;
            this.transitionIn.active = false;
            this.transitionIn.weight = 0.0F;
            this.setMainTracksPlaying(true);
        }
    }

    @Override
    public void onNoAnimConditionsPass() {
    }

    private void invokeAnimStartTimeEvent(AnimationTrack track) {
        this.invokeAnimTimeEvent(track, AnimEvent.AnimEventTime.START);
    }

    private void invokeAnimEndTimeEvent(AnimationTrack track) {
        this.invokeAnimTimeEvent(track, AnimEvent.AnimEventTime.END);
    }

    private void invokeAnimTimeEvent(AnimationTrack track, AnimEvent.AnimEventTime eventTime) {
        if (this.sourceNode != null) {
            List<AnimEvent> events = this.getSourceNode().events;
            int i = 0;

            for (int eventCount = events.size(); i < eventCount; i++) {
                AnimEvent event = events.get(i);
                if (event.time == eventTime) {
                    this.animLayer.invokeAnimEvent(this, track, event);
                }
            }
        }
    }

    public AnimNode getSourceNode() {
        return this.sourceNode;
    }

    /**
     * Returns TRUE if this Live node is currently Active, and if the source AnimNode is an Idle animation.
     */
    public boolean isIdleAnimActive() {
        return this.active && this.sourceNode.isIdleAnim();
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
        }
    }

    public boolean isLooped() {
        return this.sourceNode.isLooped;
    }

    public float getWeight() {
        return this.weight;
    }

    public float getTransitionInWeight() {
        return this.transitionIn.weight;
    }

    public boolean wasActivated() {
        return this.active != this.wasActive && this.active;
    }

    public boolean wasDeactivated() {
        return this.active != this.wasActive && this.wasActive;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public int getPlayingTrackCount() {
        int playingTrackCount = 0;
        if (this.isMainAnimActive()) {
            playingTrackCount += this.animationTracks.size();
        }

        if (this.runningRagdollTrack != null) {
            playingTrackCount++;
        }

        if (this.isTransitioningIn()) {
            playingTrackCount++;
        }

        return playingTrackCount;
    }

    public AnimationTrack getPlayingTrackAt(int trackIdx) {
        if (trackIdx < 0) {
            throw new IndexOutOfBoundsException("TrackIdx is negative. Out of bounds: " + trackIdx);
        } else {
            int playingTrackCount = 0;
            if (this.isMainAnimActive()) {
                playingTrackCount += this.animationTracks.size();
                if (trackIdx < playingTrackCount) {
                    return this.animationTracks.get(trackIdx);
                }
            }

            if (this.runningRagdollTrack != null) {
                if (trackIdx < ++playingTrackCount) {
                    return this.runningRagdollTrack;
                }
            }

            if (this.isTransitioningIn()) {
                if (trackIdx < ++playingTrackCount) {
                    return this.transitionIn.track;
                }
            }

            throw new IndexOutOfBoundsException("TrackIdx out of bounds 0 - " + playingTrackCount);
        }
    }

    public boolean isMainAnimActive() {
        return !this.isTransitioningIn() || this.transitionIn.blendingOut;
    }

    public String getTransitionFrom() {
        return this.transitionIn.transitionedFrom;
    }

    public void setTransitionInBlendDelta(float blendDelta) {
        if (this.transitionIn.track != null) {
            this.transitionIn.track.setBlendWeight(blendDelta);
        }
    }

    public AnimationTrack getTransitionInTrack() {
        return this.transitionIn.track;
    }

    public int getTransitionLayerIdx() {
        return this.transitionIn.track != null ? this.transitionIn.track.getLayerIdx() : -1;
    }

    public int getLayerIdx() {
        return this.animLayer.getDepth();
    }

    public int getPriority() {
        return this.sourceNode.getPriority();
    }

    public String getDeferredBoneName() {
        return this.sourceNode.getDeferredBoneName();
    }

    public BoneAxis getDeferredBoneAxis() {
        return this.sourceNode.getDeferredBoneAxis();
    }

    public List<AnimBoneWeight> getSubStateBoneWeights() {
        return this.sourceNode.subStateBoneWeights;
    }

    public AnimTransition findTransitionTo(IAnimationVariableSource in_varSource, AnimNode in_toNode) {
        return this.sourceNode.findTransitionTo(in_varSource, in_toNode);
    }

    public float getSpeedScale(IAnimationVariableSource varSource) {
        return this.sourceNode.getSpeedScale(varSource);
    }

    public boolean isGrappler() {
        return this.sourceNode.isGrappler();
    }

    public String getMatchingGrappledAnimNode() {
        return this.sourceNode.getMatchingGrappledAnimNode();
    }

    public GrappleOffsetBehaviour getGrapplerOffsetBehaviour() {
        return this.sourceNode.grapplerOffsetBehaviour;
    }

    public float getGrappleOffsetForward() {
        return this.sourceNode.grappleOffsetForward;
    }

    public float getGrappledOffsetYaw() {
        return this.sourceNode.grappleOffsetYaw;
    }

    public String getAnimName() {
        return !StringUtils.isNullOrWhitespace(this.cachedRandomAnim) ? this.cachedRandomAnim : this.sourceNode.animName;
    }

    public void selectRandomAnim() {
        this.cachedRandomAnim = this.sourceNode.getRandomAnim();
    }

    public boolean isTweeningInGrapple() {
        return this.tweeningInGrapple;
    }

    public void setTweeningInGrapple(boolean in_tweeningInGrapple) {
        this.tweeningInGrapple = in_tweeningInGrapple;
    }

    public boolean isTweeningInGrappleFinished() {
        return this.tweeningInGrappleFinished;
    }

    public void setTweeningInGrappleFinished(boolean in_tweeningInGrappleFinished) {
        this.tweeningInGrappleFinished = in_tweeningInGrappleFinished;
    }

    public Vector3f getGrappleTweenStartPos(Vector3f out_result) {
        out_result.set(this.grappleTweenStartPos);
        return out_result;
    }

    public void setGrappleTweenStartPos(Vector3f in_pos) {
        this.grappleTweenStartPos.set(in_pos);
    }

    public Vector3 getGrappleTweenStartPos(Vector3 out_result) {
        out_result.set(this.grappleTweenStartPos.x, this.grappleTweenStartPos.y, this.grappleTweenStartPos.z);
        return out_result;
    }

    public void setGrappleTweenStartPos(Vector3 in_pos) {
        this.grappleTweenStartPos.set(in_pos.x, in_pos.y, in_pos.z);
    }

    public float getGrappleTweenInTime() {
        return this.sourceNode.grappleTweenInTime;
    }

    public Iterable<AnimationTrack> getMainAnimationTracks() {
        return (Iterable<AnimationTrack>)(this.isMainAnimActive() ? this.animationTracks : PZEmptyIterable.getInstance());
    }

    public int getMainAnimationTracksCount() {
        return this.animationTracks.size();
    }

    public AnimationTrack getMainAnimationTrackAt(int idx) {
        return this.animationTracks.get(idx);
    }

    public boolean containsMainAnimationTrack(AnimationTrack track) {
        return this.animationTracks.contains(track);
    }

    public boolean hasMainAnimationTracks() {
        return !this.animationTracks.isEmpty();
    }

    public boolean incrementWhileAliveFlagOnce(AnimationVariableReference in_variableReference, boolean in_whileAliveFlagValue) {
        boolean alreadyExists = PZArrayUtil.contains(
            this.whileAliveFlags, in_variableReference, (variableReference, entry) -> entry.variableReference == variableReference
        );
        return alreadyExists ? false : this.whileAliveFlags.add(LiveAnimNode.WhileAliveFlag.alloc(in_variableReference, in_whileAliveFlagValue));
    }

    public ArrayList<LiveAnimNode.WhileAliveFlag> getWhileAliveFlags() {
        return this.whileAliveFlags;
    }

    private void decrementWhileAliveFlags() {
        if (this.animLayer != null) {
            this.animLayer.decrementWhileAliveFlags(this);
        }
    }

    public boolean getUseDeferredRotation() {
        return this.sourceNode.useDeferedRotation;
    }

    public boolean getUseDeferredMovement() {
        return this.sourceNode.useDeferredMovement;
    }

    public float getDeferredRotationScale() {
        return this.sourceNode.deferredRotationScale;
    }

    public void onTransferredToLayer(AnimLayer in_newParentLayer) {
        this.animLayer = in_newParentLayer;
        boolean useBoneMasks = this.animLayer.isSubLayer();

        for (int i = 0; i < this.animationTracks.size(); i++) {
            AnimationTrack track = this.animationTracks.get(i);
            track.setAnimLayer(this.animLayer);
            this.initTrackBoneWeights(track, useBoneMasks);
        }

        for (int i = 0; i < this.ragdollTracks.size(); i++) {
            AnimationTrack track = this.ragdollTracks.get(i);
            track.setAnimLayer(this.animLayer);
            this.initTrackBoneWeights(track, useBoneMasks);
        }
    }

    private void initTrackBoneWeights(AnimationTrack track, boolean useBoneMasks) {
        AnimNode node = this.getSourceNode();
        if (node != null) {
            SkinningData skinningData = this.getSkinningData();
            if (skinningData != null) {
                if (useBoneMasks) {
                    track.setBoneWeights(node.subStateBoneWeights);
                    track.initBoneWeights(skinningData);
                } else {
                    track.setBoneWeights(null);
                }
            }
        }
    }

    private SkinningData getSkinningData() {
        return this.animLayer == null ? null : this.animLayer.getSkinningData();
    }

    private static class TransitionIn {
        private float time;
        private String transitionedFrom;
        private boolean active;
        private AnimationTrack track;
        private AnimTransition data;
        private float weight;
        private float rawWeight;
        private boolean blendingIn;
        private boolean blendingOut;

        private void reset() {
            this.time = 0.0F;
            this.transitionedFrom = null;
            this.active = false;
            this.track = null;
            this.data = null;
            this.weight = 0.0F;
            this.rawWeight = 0.0F;
            this.blendingIn = false;
            this.blendingOut = false;
        }
    }

    public static class WhileAliveFlag extends PooledObject {
        public AnimationVariableReference variableReference;
        public boolean whileAliveValue;
        private static final Pool<LiveAnimNode.WhileAliveFlag> s_pool = new Pool<>(LiveAnimNode.WhileAliveFlag::new);

        private WhileAliveFlag() {
        }

        @Override
        public void onReleased() {
            this.variableReference = null;
            this.whileAliveValue = false;
        }

        public static LiveAnimNode.WhileAliveFlag alloc(AnimationVariableReference in_variableReference, boolean in_whileAliveFlagValue) {
            LiveAnimNode.WhileAliveFlag newInstance = s_pool.alloc();
            newInstance.variableReference = in_variableReference;
            newInstance.whileAliveValue = in_whileAliveFlagValue;
            return newInstance;
        }
    }
}
