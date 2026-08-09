// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.events.LocalAnimEvent;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.IAnimListener;
import zombie.core.skinnedmodel.animation.StartAnimTrackParameters;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;

public final class AnimLayer extends PooledObject implements IAnimListener {
    private AnimLayer parentLayer;
    private IAnimatable character;
    private AnimState state;
    private AdvancedAnimator parentAnimator;
    private LiveAnimNode currentSyncNode;
    private AnimationTrack currentSyncTrack;
    private final List<AnimNode> reusableAnimNodes = new ArrayList<>();
    private final List<LiveAnimNode> liveAnimNodes = new ArrayList<>();
    private boolean noAnimConditionsEventSent;
    private final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("AnimLayer.Update");
    private final AnimLayer.Reusables reusables = new AnimLayer.Reusables();
    private static final Pool<AnimLayer> s_pool = new Pool<>(AnimLayer::new);

    private AnimLayer() {
    }

    public static AnimLayer alloc(IAnimatable character, AdvancedAnimator parentAnimator) {
        return alloc(null, character, parentAnimator);
    }

    public static AnimLayer alloc(AnimLayer parentLayer, IAnimatable character, AdvancedAnimator parentAnimator) {
        AnimLayer newLayer = s_pool.alloc();
        newLayer.parentLayer = parentLayer;
        newLayer.character = character;
        newLayer.parentAnimator = parentAnimator;
        return newLayer;
    }

    @Override
    public void onReleased() {
        super.onReleased();
        IPooledObject.release(this.liveAnimNodes);
        this.parentLayer = null;
        this.character = null;
        this.state = null;
        this.parentAnimator = null;
        this.currentSyncNode = null;
        this.currentSyncTrack = null;
        this.noAnimConditionsEventSent = false;
    }

    public String getCurrentStateName() {
        return this.state == null ? null : this.state.name;
    }

    public static String getCurrentStateName(AnimLayer sender) {
        return sender != null ? sender.getCurrentStateName() : null;
    }

    public boolean hasState() {
        return this.state != null;
    }

    public boolean isStateless() {
        return this.state == null;
    }

    public boolean isSubLayer() {
        return this.parentLayer != null;
    }

    public boolean isCurrentState(String stateName) {
        return this.state != null && StringUtils.equals(this.state.name, stateName);
    }

    public boolean isCurrentState(AnimState state) {
        return this.state == state;
    }

    public void setParentLayer(AnimLayer parentLayer) {
        if (this == parentLayer) {
            throw new IllegalArgumentException("Attempting to set layer's parent to itself.");
        } else {
            if (this.parentLayer != parentLayer) {
                this.parentLayer = parentLayer;

                for (int inode = 0; inode < this.liveAnimNodes.size(); inode++) {
                    this.liveAnimNodes.get(inode).onTransferredToLayer(this);
                }
            }
        }
    }

    public AnimLayer getParentLayer() {
        return this.parentLayer;
    }

    public AnimationMultiTrack getAnimationTrack() {
        if (this.character == null) {
            return null;
        } else {
            AnimationPlayer animationPlayer = this.character.getAnimationPlayer();
            return animationPlayer == null ? null : animationPlayer.getMultiTrack();
        }
    }

    public IAnimationVariableSource getVariableSource() {
        return this.character;
    }

    public LiveAnimNode getCurrentSyncNode() {
        return this.currentSyncNode;
    }

    public AnimationTrack getCurrentSyncTrack() {
        return this.currentSyncTrack;
    }

    @Override
    public void onAnimStarted(AnimationTrack track) {
    }

    @Override
    public void onLoopedAnim(AnimationTrack track) {
        this.invokeAnimEvent(track, LocalAnimEvent.ActiveAnimLoopedEvent, false);
    }

    @Override
    public void onNonLoopedAnimFadeOut(AnimationTrack track) {
        this.invokeAnimEvent(track, LocalAnimEvent.ActiveAnimFinishingEvent, true);
        this.invokeAnimEvent(track, LocalAnimEvent.ActiveNonLoopedAnimFadeOutEvent, true);
    }

    @Override
    public void onNonLoopedAnimFinished(AnimationTrack track) {
        this.invokeAnimEvent(track, LocalAnimEvent.ActiveAnimFinishingEvent, false);
        this.invokeAnimEvent(track, LocalAnimEvent.ActiveNonLoopedAnimFinishedEvent, true);
    }

    @Override
    public void onTrackDestroyed(AnimationTrack track) {
    }

    @Override
    public void onNoAnimConditionsPass() {
        if (!this.noAnimConditionsEventSent) {
            this.invokeAnimEvent(null, null, LocalAnimEvent.NoAnimConditionsPass);
            this.noAnimConditionsEventSent = true;
        }
    }

    private void invokeAnimEvent(AnimationTrack track, LocalAnimEvent animEvent, boolean includeTransitioningOut) {
        if (this.parentAnimator != null) {
            int anIdx = 0;

            for (int activeNodeCount = this.liveAnimNodes.size(); anIdx < activeNodeCount; anIdx++) {
                LiveAnimNode node = this.liveAnimNodes.get(anIdx);
                if ((!node.transitioningOut || includeTransitioningOut)
                    && node.getSourceNode().parentState == this.state
                    && node.containsMainAnimationTrack(track)) {
                    this.invokeAnimEvent(node, track, animEvent);
                    break;
                }
            }
        }
    }

    private void invokeAnimEvent(LiveAnimNode node, AnimationTrack track, LocalAnimEvent animEvent) {
        this.invokeAnimEvent(node, track, animEvent.getAnimEvent());
    }

    protected void invokeAnimEvent(LiveAnimNode node, AnimationTrack track, AnimEvent animEvent) {
        if (this.parentAnimator == null) {
            DebugType.Animation.warn("invokeAnimEvent. No listener. %s", animEvent.toDetailsString());
        } else {
            if (this.isRecording()) {
                this.logAnimEvent(track, animEvent);
            }

            if (animEvent instanceof AnimEventFlagWhileAlive evtSetFlag && node.incrementWhileAliveFlagOnce(evtSetFlag.variableReference, evtSetFlag.flagValue)
                )
             {
                this.parentAnimator.incrementWhileAliveFlag(evtSetFlag.variableReference, evtSetFlag.flagValue);
            }

            this.parentAnimator.OnAnimEvent(this, track, animEvent);
        }
    }

    public void decrementWhileAliveFlags(LiveAnimNode animNode) {
        ArrayList<LiveAnimNode.WhileAliveFlag> whileAliveFlags = animNode.getWhileAliveFlags();

        for (int i = 0; i < whileAliveFlags.size(); i++) {
            LiveAnimNode.WhileAliveFlag whileAliveFlag = whileAliveFlags.get(i);
            this.parentAnimator.decrementWhileAliveFlag(whileAliveFlag.variableReference, whileAliveFlag.whileAliveValue);
        }

        whileAliveFlags.clear();
    }

    public String GetDebugString() {
        String nodeName = this.character.getAdvancedAnimator().animSet.name;
        if (this.state != null) {
            nodeName = nodeName + "/" + this.state.name;
        }

        StringBuilder debug = new StringBuilder("State: " + nodeName);

        for (LiveAnimNode an : this.liveAnimNodes) {
            debug.append("\n  Node: ").append(an.getSourceNode().name);
        }

        AnimationMultiTrack multiTrack = this.getAnimationTrack();
        if (multiTrack != null) {
            debug.append("\n  AnimTrack:");

            for (AnimationTrack anmt : multiTrack.getTracks()) {
                if (anmt.animLayer == this) {
                    debug.append("\n    Anim: ").append(anmt.getName()).append(" Weight: ").append(anmt.getBlendWeight());
                }
            }
        }

        return debug.toString();
    }

    public void reset() {
        IPooledObject.release(this.liveAnimNodes);
        this.noAnimConditionsEventSent = false;
        this.state = null;
    }

    public boolean transitionTo(AnimState newState) {
        return this.transitionTo(newState, false);
    }

    public boolean transitionTo(AnimState newState, boolean force) {
        DebugType.AnimationDetailed
            .debugln(
                "TransitionTo: from Anim <%s> to State <%s>",
                this.getLiveAnimNodes().isEmpty() ? "NoAnim" : this.getLiveAnimNodes().getFirst().getName(),
                newState != null ? newState.name : "NoState"
            );
        if (!force && newState == this.state) {
            return true;
        } else {
            if (DebugOptions.instance.animation.animLayer.logStateChanges.getValue()) {
                String parentStr = this.parentLayer == null ? "" : AnimState.getStateName(this.parentLayer.state) + " | ";
                String stateChangeStr = String.format("State: %s%s => %s", parentStr, AnimState.getStateName(this.state), AnimState.getStateName(newState));
                DebugType.General.debugln(stateChangeStr);
                if (this.character instanceof IsoGameCharacter isoGameCharacter) {
                    isoGameCharacter.setSayLine(stateChangeStr);
                }
            }

            this.state = newState;
            this.noAnimConditionsEventSent = false;

            for (int i = 0; i < this.liveAnimNodes.size(); i++) {
                LiveAnimNode an = this.liveAnimNodes.get(i);
                an.transitioningOut = true;
            }

            return true;
        }
    }

    public void updateLiveAnimNodes() {
        this.removeFadedOutNodes();
        this.updateNodeActiveFlags();
    }

    public void Update(float deltaT) {
        try (AbstractPerformanceProfileProbe var2 = this.updateInternal.profile()) {
            this.updateInternal(deltaT);
        }
    }

    public SkinningData getSkinningData() {
        if (this.character == null) {
            return null;
        } else {
            AnimationPlayer animationPlayer = this.character.getAnimationPlayer();
            return animationPlayer == null ? null : animationPlayer.getSkinningData();
        }
    }

    private void updateInternal(float deltaT) {
        LiveAnimNode highestWeightedNode = this.getHighestLiveNode();
        this.currentSyncNode = highestWeightedNode;
        this.currentSyncTrack = null;
        if (highestWeightedNode != null) {
            int anIdx = 0;

            for (int liveNodeCount = this.liveAnimNodes.size(); anIdx < liveNodeCount; anIdx++) {
                LiveAnimNode an = this.liveAnimNodes.get(anIdx);
                an.update(deltaT);
            }

            IAnimationVariableSource varSource = this.character;
            this.updateMaximumTwist(varSource);
            boolean dbgForceScalars = DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue()
                && varSource.getVariableBoolean("dbgForceAnim")
                && varSource.getVariableBoolean("dbgForceAnimScalars");
            String dbgForceScalarNodeName = dbgForceScalars ? varSource.getVariableString("dbgForceAnimNodeName") : null;
            AnimationTrack syncTrack = this.findSyncTrack(highestWeightedNode);
            this.currentSyncTrack = syncTrack;
            float syncValue = syncTrack != null ? syncTrack.getCurrentTimeFraction() : -1.0F;
            IGrappleable thisGrappleable = this.character.getGrappleable();
            int anIdxx = 0;

            for (int liveNodeCount = this.liveAnimNodes.size(); anIdxx < liveNodeCount; anIdxx++) {
                LiveAnimNode liveAnimNode = this.liveAnimNodes.get(anIdxx);
                float duration = 1.0F;
                int trackIdx = 0;

                for (int liveNodeTracksCount = liveAnimNode.getPlayingTrackCount(); trackIdx < liveNodeTracksCount; trackIdx++) {
                    AnimationTrack track = liveAnimNode.getPlayingTrackAt(trackIdx);
                    if (track.isPlaying) {
                        if (syncTrack != null && track.syncTrackingEnabled && track.isLooping() && track != syncTrack) {
                            track.moveCurrentTimeValueToFraction(syncValue);
                        }

                        if (track.isPrimary) {
                            duration = track.getDuration();
                            liveAnimNode.nodeAnimTime = track.getCurrentTimeValue();
                        }
                    }
                }

                this.updateInternalGrapple(thisGrappleable, liveAnimNode);
                if (this.parentAnimator != null && !liveAnimNode.getSourceNode().events.isEmpty()) {
                    float animPercent = liveAnimNode.nodeAnimTime / duration;
                    float animPrevPercent = liveAnimNode.prevNodeAnimTime / duration;
                    List<AnimEvent> events = liveAnimNode.getSourceNode().events;
                    int i = 0;

                    for (int eventCount = events.size(); i < eventCount; i++) {
                        AnimEvent event = events.get(i);
                        if (event.time == AnimEvent.AnimEventTime.PERCENTAGE) {
                            float eventTimePc = event.timePc;
                            if (animPrevPercent < eventTimePc && eventTimePc <= animPercent) {
                                this.invokeAnimEvent(liveAnimNode, null, event);
                            } else {
                                if (!liveAnimNode.isLooped() && animPercent < eventTimePc) {
                                    break;
                                }

                                if (liveAnimNode.isLooped() && animPrevPercent > animPercent) {
                                    if (animPrevPercent < eventTimePc && eventTimePc <= animPercent + 1.0F) {
                                        this.invokeAnimEvent(liveAnimNode, null, event);
                                    } else if (animPrevPercent > eventTimePc && eventTimePc <= animPercent) {
                                        this.invokeAnimEvent(liveAnimNode, null, event);
                                    }
                                }
                            }
                        }
                    }
                }

                if (liveAnimNode.getPlayingTrackCount() != 0) {
                    boolean dbgForceScalarsOnNode = dbgForceScalars && StringUtils.equalsIgnoreCase(liveAnimNode.getSourceNode().name, dbgForceScalarNodeName);
                    String scalarName = dbgForceScalarsOnNode ? "dbgForceScalar" : liveAnimNode.getSourceNode().scalar;
                    String scalar2Name = dbgForceScalarsOnNode ? "dbgForceScalar2" : liveAnimNode.getSourceNode().scalar2;
                    float transitionInWeight = liveAnimNode.getTransitionInWeight();
                    liveAnimNode.setTransitionInBlendDelta(transitionInWeight);
                    float nodeWeight = liveAnimNode.getWeight();
                    float remainingNodeWeight = nodeWeight;
                    AnimationTrack ragdollTrack = liveAnimNode.runningRagdollTrack;
                    if (ragdollTrack != null) {
                        float weightScalar = PZMath.clamp(varSource.getVariableFloat(scalarName, 1.0F), 0.0F, 1.0F);
                        ragdollTrack.setBlendWeight(nodeWeight * weightScalar);
                        remainingNodeWeight = nodeWeight - ragdollTrack.getBlendFieldWeight() * nodeWeight;
                    }

                    if (liveAnimNode.hasMainAnimationTracks()) {
                        if (liveAnimNode.isBlendField) {
                            float x = varSource.getVariableFloat(scalarName, 0.0F);
                            float y = varSource.getVariableFloat(scalar2Name, 0.0F);
                            this.applyBlendField(liveAnimNode, remainingNodeWeight, x, y);
                        } else {
                            float weightScalar = PZMath.clamp(varSource.getVariableFloat(scalarName, 1.0F), 0.0F, 1.0F);
                            float mainTrackWeight = remainingNodeWeight * weightScalar;
                            int numMainTracks = liveAnimNode.getMainAnimationTracksCount();
                            float mainTrackPortionWeight = mainTrackWeight / numMainTracks;

                            for (int i = 0; i < numMainTracks; i++) {
                                AnimationTrack mainTrack = liveAnimNode.getMainAnimationTrackAt(i);
                                mainTrack.setBlendWeight(mainTrackPortionWeight);
                            }
                        }
                    }
                }
            }

            if (this.isRecording()) {
                this.logBlendWeights();
                this.logCurrentState();
            }
        }
    }

    private void updateInternalGrapple(IGrappleable thisGrappleable, LiveAnimNode liveAnimNode) {
        if (thisGrappleable != null) {
            if (thisGrappleable.isGrappling() && liveAnimNode.isGrappler()) {
                this.updateInternalWhileGrappling(thisGrappleable, liveAnimNode);
            }

            if (thisGrappleable.isBeingGrappled()) {
                this.updateInternalWhileGrappled(thisGrappleable, liveAnimNode);
            }
        }
    }

    private void updateInternalWhileGrappling(IGrappleable thisGrappleable, LiveAnimNode liveAnimNode) {
        if (!thisGrappleable.isGrappling()) {
            DebugType.Grapple.warn("This Grappleable is not currently grappling: %s", thisGrappleable);
        } else if (!liveAnimNode.isGrappler()) {
            DebugType.Grapple.warn("This Grappleable's sourceNode is not a grappler: %s, sourceNode: %s", thisGrappleable, liveAnimNode.getSourceNode());
        } else {
            String matchingGrappledAnimNodeName = liveAnimNode.getMatchingGrappledAnimNode();
            int numGrapplingNodes = 0;
            int trackIdx = 0;

            for (int liveNodeTracksCount = liveAnimNode.getPlayingTrackCount(); trackIdx < liveNodeTracksCount; trackIdx++) {
                AnimationTrack track = liveAnimNode.getPlayingTrackAt(trackIdx);
                if (track.isPlaying && track.isGrappler()) {
                    if (++numGrapplingNodes > 1) {
                        DebugType.Grapple
                            .warn(
                                "More than one AnimNode is grappling. The node '%s' is being overwritten by node '%s'.",
                                thisGrappleable.getSharedGrappleAnimNode(),
                                liveAnimNode.getName()
                            );
                    }

                    float sharedAnimTime = track.getCurrentTrackTime();
                    float inGrappleAnimFraction = sharedAnimTime / track.getDuration();
                    thisGrappleable.setSharedGrappleAnimNode(liveAnimNode.getName());
                    thisGrappleable.setSharedGrappleAnimTime(sharedAnimTime);
                    thisGrappleable.setSharedGrappleAnimFraction(inGrappleAnimFraction);
                    IGrappleable grappledTarget = thisGrappleable.getGrapplingTarget();
                    grappledTarget.setSharedGrappleAnimNode(matchingGrappledAnimNodeName);
                    grappledTarget.setSharedGrappleAnimTime(sharedAnimTime);
                    grappledTarget.setSharedGrappleAnimFraction(inGrappleAnimFraction);
                    thisGrappleable.setGrappleoffsetBehaviour(liveAnimNode.getGrapplerOffsetBehaviour());
                    thisGrappleable.setGrapplePosOffsetForward(liveAnimNode.getGrappleOffsetForward());
                    thisGrappleable.setGrappleRotOffsetYaw(liveAnimNode.getGrappledOffsetYaw());
                }
            }

            switch (thisGrappleable.getGrappleOffsetBehaviour()) {
                case GRAPPLER:
                    IGrappleable grappledTarget = thisGrappleable.getGrapplingTarget();
                    float offsetForwardDistance = thisGrappleable.getGrapplePosOffsetForward();
                    float offsetRotYaw = thisGrappleable.getGrappleRotOffsetYaw();
                    this.performOffsetOfGrappleable(thisGrappleable, grappledTarget, offsetForwardDistance, offsetRotYaw, true);
                    break;
                case NONE_TWEEN_IN_GRAPPLER:
                    if (liveAnimNode.isTweeningInGrappleFinished()) {
                        Vector3 targetPos = thisGrappleable.getTargetGrapplePos(this.reusables.grappledPos);
                        this.setGrappleablePosition(thisGrappleable, targetPos, true);
                    } else {
                        if (!liveAnimNode.isTweeningInGrapple()) {
                            Vector3 currentPos = this.reusables.currentPos;
                            thisGrappleable.getPosition(currentPos);
                            liveAnimNode.setGrappleTweenStartPos(currentPos);
                            liveAnimNode.setTweeningInGrapple(true);
                            liveAnimNode.setTweeningInGrappleFinished(false);
                            Vector3 offsetPos = this.reusables.grappledPos;
                            Vector2 relativeToForward = this.reusables.animForwardDirection;
                            IGrappleable relativeTo = thisGrappleable.getGrapplingTarget();
                            float offsetForwardDistancex = thisGrappleable.getGrapplePosOffsetForward();
                            float offsetRotYawx = thisGrappleable.getGrappleRotOffsetYaw();
                            this.calculateOffsetsForGrappleable(relativeTo, offsetForwardDistancex, offsetRotYawx, relativeToForward, offsetPos);
                            thisGrappleable.setTargetGrapplePos(offsetPos);
                            thisGrappleable.setTargetGrappleRotation(relativeToForward);
                        }

                        float currentTime = thisGrappleable.getSharedGrappleAnimTime();
                        float maxTime = liveAnimNode.getGrappleTweenInTime();
                        float t = PZMath.min(currentTime, maxTime);
                        float tweenAlpha = t / maxTime;
                        float lerpAlpha = PZMath.lerpFunc_EaseOutInQuad(tweenAlpha);
                        Vector3 tweenedPos = this.reusables.tweenedPos;
                        Vector3 startingPos = liveAnimNode.getGrappleTweenStartPos(this.reusables.grappleTweenStartPos);
                        Vector3 targetPos = thisGrappleable.getTargetGrapplePos(this.reusables.grappledPos);
                        PZMath.lerp(tweenedPos, startingPos, targetPos, lerpAlpha);
                        Vector2 tweenedRot = this.reusables.tweenedRot;
                        Vector2 startingFwd = thisGrappleable.getAnimForwardDirection(this.reusables.animForwardDirection);
                        Vector2 targetRot = thisGrappleable.getTargetGrappleRotation(this.reusables.animTargetForwardDirection);
                        PZMath.lerp(tweenedRot, startingFwd, targetRot, lerpAlpha);
                        tweenedRot.normalize();
                        this.setGrappleablePosAndRotation(thisGrappleable, tweenedPos, tweenedRot, true);
                        if (currentTime >= maxTime) {
                            liveAnimNode.setTweeningInGrappleFinished(true);
                        }
                    }
                    break;
                default:
                    thisGrappleable.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
            }
        }
    }

    private void updateInternalWhileGrappled(IGrappleable thisGrappleable, LiveAnimNode liveAnimNode) {
        if (!thisGrappleable.isBeingGrappled()) {
            DebugType.Grapple.warn("This Grappleable is not being grappled: %s", thisGrappleable);
        } else {
            String grappledAnimNode = thisGrappleable.getSharedGrappleAnimNode();
            float grappledAnimFraction = thisGrappleable.getSharedGrappleAnimFraction();
            if (liveAnimNode.getName().equalsIgnoreCase(grappledAnimNode)) {
                int trackIdx = 0;

                for (int liveNodeTracksCount = liveAnimNode.getPlayingTrackCount(); trackIdx < liveNodeTracksCount; trackIdx++) {
                    AnimationTrack track = liveAnimNode.getPlayingTrackAt(trackIdx);
                    if (track.isPlaying && track.isPrimary) {
                        float grappledAnimTime = grappledAnimFraction * track.getDuration();
                        track.moveCurrentTimeValueTo(grappledAnimTime);
                        liveAnimNode.nodeAnimTime = track.getCurrentTimeValue();
                    }
                }

                switch (thisGrappleable.getGrappleOffsetBehaviour()) {
                    case GRAPPLED_TWEEN_OUT_TO_NONE:
                        float currentTime = thisGrappleable.getSharedGrappleAnimTime();
                        float maxTime = liveAnimNode.getGrappleTweenInTime();
                        if (currentTime <= maxTime) {
                            float offsetForwardDistancex = thisGrappleable.getGrapplePosOffsetForward();
                            float offsetRotYawx = thisGrappleable.getGrappleRotOffsetYaw();
                            IGrappleable grappledByx = thisGrappleable.getGrappledBy();
                            this.performOffsetOfGrappleable(thisGrappleable, grappledByx, offsetForwardDistancex, offsetRotYawx, true);
                        } else {
                            thisGrappleable.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
                        }
                        break;
                    case GRAPPLED:
                        float offsetForwardDistance = thisGrappleable.getGrapplePosOffsetForward();
                        float offsetRotYaw = thisGrappleable.getGrappleRotOffsetYaw();
                        IGrappleable grappledBy = thisGrappleable.getGrappledBy();
                        this.performOffsetOfGrappleable(thisGrappleable, grappledBy, offsetForwardDistance, offsetRotYaw, false);
                        break;
                    default:
                        thisGrappleable.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
                }
            }
        }
    }

    private void performOffsetOfGrappleable(
        IGrappleable toOffset, IGrappleable relativeTo, float offsetForwardDistance, float offsetRotYaw, boolean useDeferredOffset
    ) {
        Vector3 offsetPos = this.reusables.grappledPos;
        Vector2 relativeToForward = this.reusables.animForwardDirection;
        this.calculateOffsetsForGrappleable(relativeTo, offsetForwardDistance, offsetRotYaw, relativeToForward, offsetPos);
        this.setGrappleablePosAndRotation(toOffset, offsetPos, relativeToForward, useDeferredOffset);
    }

    private void setGrappleablePosAndRotation(IGrappleable grappleable, Vector3 position, Vector2 rotationForward, boolean useDeferredOffset) {
        grappleable.setTargetAndCurrentDirection(rotationForward.x, rotationForward.y);
        this.setGrappleablePosition(grappleable, position, useDeferredOffset);
    }

    private void setGrappleablePosition(IGrappleable grappleable, Vector3 position, boolean useDeferredOffset) {
        if (useDeferredOffset) {
            Vector3 currentPos = this.reusables.currentPos;
            grappleable.getPosition(currentPos);
            float deferredOffsetX = position.x - currentPos.x;
            float deferredOffsetY = position.y - currentPos.y;
            float deferredOffsetZ = position.z - currentPos.z;
            grappleable.setGrappleDeferredOffset(deferredOffsetX, deferredOffsetY, deferredOffsetZ);
        } else {
            grappleable.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
            grappleable.setPosition(position.x, position.y, position.z);
        }
    }

    private void calculateOffsetsForGrappleable(
        IGrappleable relativeTo, float offsetForwardDistance, float offsetRotYaw, Vector2 relativeToForward, Vector3 offsetPos
    ) {
        Vector3 relativeToPos = this.reusables.grappledByPos;
        relativeTo.getAnimForwardDirection(relativeToForward);
        relativeToForward.rotate((float) (Math.PI / 180.0) * offsetRotYaw);
        relativeTo.getPosition(relativeToPos);
        offsetPos.x = relativeToPos.x + relativeToForward.x * offsetForwardDistance;
        offsetPos.y = relativeToPos.y + relativeToForward.y * offsetForwardDistance;
        offsetPos.z = relativeToPos.z;
    }

    private void updateMaximumTwist(IAnimationVariableSource varSource) {
        IAnimationVariableSlot maxTwistVar = varSource.getVariable(AnimationVariableHandles.maxTwist);
        if (maxTwistVar != null) {
            float maximumTwist = this.getMaximumTwist(maxTwistVar);
            maxTwistVar.setValue(maximumTwist);
        }
    }

    public float getMaximumTwist(IAnimationVariableSlot maxTwistVar) {
        float charPrevTwist = maxTwistVar.getValueFloat();
        float maximumPossibleTwist = this.parentLayer != null ? this.parentLayer.getMaximumTwist(maxTwistVar) : 70.0F;
        float charTwist = 0.0F;
        float remainingWeight = 1.0F;

        for (int anIdx = this.liveAnimNodes.size() - 1; anIdx >= 0; anIdx--) {
            LiveAnimNode liveNode = this.liveAnimNodes.get(anIdx);
            float nodeWeight = liveNode.getWeight();
            if (remainingWeight <= 0.0F) {
                break;
            }

            float clampedWeight = PZMath.clamp(nodeWeight, 0.0F, remainingWeight);
            remainingWeight -= clampedWeight;
            float nodeMaxTwist = PZMath.clamp(liveNode.getSourceNode().maxTorsoTwist, 0.0F, maximumPossibleTwist);
            charTwist += nodeMaxTwist * clampedWeight;
        }

        if (remainingWeight > 0.0F) {
            charTwist += charPrevTwist * remainingWeight;
        }

        return charTwist;
    }

    public void updateNodeActiveFlags() {
        for (int anIdx = 0; anIdx < this.liveAnimNodes.size(); anIdx++) {
            LiveAnimNode an = this.liveAnimNodes.get(anIdx);
            an.setActive(false);
        }

        AnimState animState = this.state;
        if (animState != null) {
            IAnimationVariableSource varSource = this.character;
            if (!varSource.getVariableBoolean(AnimationVariableHandles.AnimLocked)) {
                List<AnimNode> nodes = animState.getAnimNodes(varSource, this.reusableAnimNodes);
                if (nodes.isEmpty()) {
                    this.onNoAnimConditionsPass();
                } else {
                    this.noAnimConditionsEventSent = false;
                }

                int nodeIdx = 0;

                for (int nodeCount = nodes.size(); nodeIdx < nodeCount; nodeIdx++) {
                    AnimNode node = nodes.get(nodeIdx);
                    this.getOrCreateLiveNode(node);
                }
            }
        }
    }

    public void FindTransitioningLiveAnimNode(TransitionNodeProxy liveAnimNodeProxy, boolean isRootLayer) {
        for (int i = 0; i < this.liveAnimNodes.size(); i++) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(i);
            if (liveAnimNode.isNew() && liveAnimNode.wasActivated()) {
                boolean bAlreadyStarted = false;

                for (int j = 0; j < liveAnimNodeProxy.allNewNodes.size(); j++) {
                    TransitionNodeProxy.NodeLayerPair newNode = liveAnimNodeProxy.allNewNodes.get(j);
                    if (newNode.liveAnimNode.getSourceNode() == liveAnimNode.getSourceNode()) {
                        bAlreadyStarted = true;
                        break;
                    }
                }

                if (!bAlreadyStarted) {
                    DebugType.AnimationDetailed
                        .debugln("** NEW ** newNode: <%s>; Layer: <%s>", liveAnimNode.getName(), isRootLayer ? "RootLayer" : "NoneRootLayer");
                    liveAnimNodeProxy.allNewNodes.add(liveAnimNodeProxy.allocNodeLayerPair(liveAnimNode, this));
                } else {
                    DebugType.AnimationDetailed
                        .debugln("** SKIPPED ** newNode: <%s>; Layer: <%s>", liveAnimNode.getName(), isRootLayer ? "RootLayer" : "NoneRootLayer");
                }
            } else if (liveAnimNode.wasDeactivated() && isRootLayer || !isRootLayer && liveAnimNode.transitioningOut) {
                boolean bAlreadyStarted = false;

                for (int jx = 0; jx < liveAnimNodeProxy.allOutgoingNodes.size(); jx++) {
                    TransitionNodeProxy.NodeLayerPair newNode = liveAnimNodeProxy.allOutgoingNodes.get(jx);
                    if (newNode.liveAnimNode.getSourceNode() == liveAnimNode.getSourceNode()) {
                        bAlreadyStarted = true;
                        break;
                    }
                }

                if (!bAlreadyStarted) {
                    DebugType.AnimationDetailed
                        .debugln("** NEW ** oldNode: <%s>; Layer: <%s>", liveAnimNode.getName(), isRootLayer ? "RootLayer" : "NoneRootLayer");
                    liveAnimNodeProxy.allOutgoingNodes.add(liveAnimNodeProxy.allocNodeLayerPair(liveAnimNode, this));
                } else {
                    DebugType.AnimationDetailed
                        .debugln("** SKIPPED ** oldNode: <%s>; Layer: <%s>", liveAnimNode.getName(), isRootLayer ? "RootLayer" : "NoneRootLayer");
                }
            }
        }
    }

    public AnimationTrack startTransitionAnimation(TransitionNodeProxy.TransitionNodeProxyData transitionData) {
        if (StringUtils.isNullOrWhitespace(transitionData.transitionOut.animName)) {
            DebugType.Animation
                .debugln("  TransitionTo found: %s -> <no anim> -> %s", transitionData.oldAnimNode.getName(), transitionData.newAnimNode.getName());
            return null;
        } else {
            float speedScale = transitionData.transitionOut.speedScale;
            if (speedScale == Float.POSITIVE_INFINITY) {
                speedScale = transitionData.newAnimNode.getSpeedScale(this.character);
            }

            StartAnimTrackParameters params = StartAnimTrackParameters.alloc();
            params.animName = transitionData.transitionOut.animName;
            params.subLayerBoneWeights = transitionData.oldAnimNode.getSubStateBoneWeights();
            params.speedScale = speedScale;
            params.deferredBoneName = transitionData.getDeferredBoneName();
            params.deferredBoneAxis = transitionData.getDeferredBoneAxis();
            params.useDeferredRotation = transitionData.getUseDeferredRotation();
            params.useDeferredMovement = transitionData.getUseDeferredMovement();
            params.deferredRotationScale = transitionData.getDeferredRotationScale();
            params.priority = transitionData.oldAnimNode.getPriority();
            AnimationTrack track = this.startTrackGeneric(params);
            params.release();
            if (track == null) {
                DebugType.Animation
                    .debugln(
                        "  TransitionTo failed to play transition track: %s -> %s -> %s",
                        transitionData.oldAnimNode.getName(),
                        transitionData.transitionOut.animName,
                        transitionData.newAnimNode.getName()
                    );
                return null;
            } else {
                DebugType.Animation
                    .debugln(
                        "  TransitionTo found: %s -> %s -> %s",
                        transitionData.oldAnimNode.getName(),
                        transitionData.transitionOut.animName,
                        transitionData.newAnimNode.getName()
                    );
                return track;
            }
        }
    }

    public void removeFadedOutNodes() {
        for (int anIdx = this.liveAnimNodes.size() - 1; anIdx >= 0; anIdx--) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(anIdx);
            if (!liveAnimNode.isActive()
                && (!liveAnimNode.isTransitioningIn() || !(liveAnimNode.getTransitionInWeight() > 0.01F))
                && !(liveAnimNode.getWeight() > 0.01F)) {
                this.removeLiveNodeAt(anIdx);
            }
        }
    }

    public void render() {
        IAnimationVariableSource varSource = this.character;
        boolean dbgForceScalars = DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue()
            && varSource.getVariableBoolean("dbgForceAnim")
            && varSource.getVariableBoolean("dbgForceAnimScalars");
        String dbgForceScalarNodeName = dbgForceScalars ? varSource.getVariableString("dbgForceAnimNodeName") : null;
        int anIdx = 0;

        for (int liveNodeCount = this.liveAnimNodes.size(); anIdx < liveNodeCount; anIdx++) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(anIdx);
            if (liveAnimNode.getMainAnimationTracksCount() > 1) {
                boolean dbgForceScalarsOnNode = dbgForceScalars && StringUtils.equalsIgnoreCase(liveAnimNode.getSourceNode().name, dbgForceScalarNodeName);
                String scalarName = dbgForceScalarsOnNode ? "dbgForceScalar" : liveAnimNode.getSourceNode().scalar;
                String scalar2Name = dbgForceScalarsOnNode ? "dbgForceScalar2" : liveAnimNode.getSourceNode().scalar2;
                float x = varSource.getVariableFloat(scalarName, 0.0F);
                float y = varSource.getVariableFloat(scalar2Name, 0.0F);
                if (liveAnimNode.isActive()) {
                    liveAnimNode.getSourceNode().blend2dPicker.render(x, y);
                }
            }
        }
    }

    private void logBlendWeights() {
        AnimationPlayerRecorder recorder = this.character.getAnimationPlayer().getRecorder();
        int anIdx = 0;

        for (int liveNodeCount = this.liveAnimNodes.size(); anIdx < liveNodeCount; anIdx++) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(anIdx);
            recorder.logAnimNode(liveAnimNode);
        }
    }

    private void logCurrentState() {
        AnimationPlayerRecorder recorder = this.character.getAnimationPlayer().getRecorder();
        recorder.logAnimState(this, this.state);
    }

    private void logAnimEvent(AnimationTrack track, AnimEvent evt) {
        AnimationPlayerRecorder recorder = this.character.getAnimationPlayer().getRecorder();
        recorder.logAnimEvent(this.character, track, evt);
    }

    private void removeLiveNodeAt(int anIdx) {
        synchronized (this.liveAnimNodes) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(anIdx);
            DebugType.AnimationDetailed.debugln("RemoveLiveNode: %s", liveAnimNode.getName());
            this.liveAnimNodes.remove(anIdx);
            liveAnimNode.release();
        }
    }

    private void applyBlendField(LiveAnimNode an, float nodeWeight, float x, float y) {
        if (an.isActive()) {
            AnimNode sourceNode = an.getSourceNode();
            Anim2DBlendPicker blendPicker = sourceNode.blend2dPicker;
            Anim2DBlendPicker.PickResults pickResult = blendPicker.Pick(x, y, this.reusables.pickResults);
            Anim2DBlend aa = pickResult.node1;
            Anim2DBlend ab = pickResult.node2;
            Anim2DBlend ac = pickResult.node3;
            if (Float.isNaN(pickResult.scale1)) {
                pickResult.scale1 = 0.5F;
            }

            if (Float.isNaN(pickResult.scale2)) {
                pickResult.scale2 = 0.5F;
            }

            if (Float.isNaN(pickResult.scale3)) {
                pickResult.scale3 = 0.5F;
            }

            float wv1 = pickResult.scale1;
            float wv2 = pickResult.scale2;
            float wv3 = pickResult.scale3;

            for (int i = 0; i < an.getMainAnimationTracksCount(); i++) {
                Anim2DBlend blend = sourceNode.blends2d.get(i);
                AnimationTrack track = an.getMainAnimationTrackAt(i);
                float blendFieldWeight;
                if (blend == aa) {
                    blendFieldWeight = wv1;
                } else if (blend == ab) {
                    blendFieldWeight = wv2;
                } else if (blend == ac) {
                    blendFieldWeight = wv3;
                } else {
                    blendFieldWeight = 0.0F;
                }

                if (blendFieldWeight < 1.0E-4F) {
                    blendFieldWeight = 0.0F;
                }

                blendFieldWeight = PZMath.clamp(blendFieldWeight, 0.0F, 1.0F);
                track.setBlendFieldWeight(blendFieldWeight);
            }
        }

        for (int i = 0; i < an.getMainAnimationTracksCount(); i++) {
            AnimationTrack trackx = an.getMainAnimationTrackAt(i);
            trackx.setBlendWeight(nodeWeight);
        }
    }

    private void getOrCreateLiveNode(AnimNode node) {
        LiveAnimNode liveAnimNode = this.findLiveNode(node);
        if (liveAnimNode != null) {
            liveAnimNode.setActive(true);
        } else {
            liveAnimNode = LiveAnimNode.alloc(this, node);
            this.startLiveNodeTracks(liveAnimNode);
            liveAnimNode.setActive(true);
            this.liveAnimNodes.add(liveAnimNode);
        }
    }

    private LiveAnimNode findLiveNode(AnimNode node) {
        LiveAnimNode found = null;
        int anIdx = 0;

        for (int liveNodeCount = this.liveAnimNodes.size(); anIdx < liveNodeCount; anIdx++) {
            LiveAnimNode an = this.liveAnimNodes.get(anIdx);
            if (!an.transitioningOut) {
                if (an.getSourceNode() == node) {
                    found = an;
                    break;
                }

                if (an.getSourceNode().parentState == node.parentState && an.getSourceNode().name.equals(node.name)) {
                    found = an;
                    break;
                }
            }
        }

        return found;
    }

    private void startLiveNodeTracks(LiveAnimNode liveAnimNode) {
        AnimNode node = liveAnimNode.getSourceNode();
        float speedScaleReal = node.getSpeedScale(this.character);
        float randomAlpha = Rand.Next(0.0F, 1.0F);
        float speedScaleMultiplierRandomMin = node.speedScaleRandomMultiplierMin;
        float speedScaleMultiplierRandomMax = node.speedScaleRandomMultiplierMax;
        float speedScaleMultiplier = PZMath.lerp(speedScaleMultiplierRandomMin, speedScaleMultiplierRandomMax, randomAlpha);
        float speedScale = speedScaleReal * speedScaleMultiplier;
        boolean isRagdoll = node.isRagdoll();
        float ragdollStartTimeMin = PZMath.max(node.ragdollStartTimeMin, 0.0F);
        float ragdollStartTimeMax = PZMath.max(node.ragdollStartTimeMax, ragdollStartTimeMin);
        float ragdollStartTime = Rand.Next(ragdollStartTimeMin, ragdollStartTimeMax);
        if (DebugOptions.instance.character.debug.ragdoll.physics.physicsHitReaction.getValue()) {
            ragdollStartTime = 0.0F;
        }

        boolean startAsRagdoll = isRagdoll && PZMath.equal(ragdollStartTime, 0.0F, 0.01F);
        boolean isBlendField = !startAsRagdoll && !node.blends2d.isEmpty();
        liveAnimNode.isBlendField = isBlendField;
        if (isRagdoll) {
            this.startRagdollTrack(liveAnimNode, speedScale, ragdollStartTime);
        }

        if (!startAsRagdoll) {
            if (!isBlendField) {
                this.startPrimaryAnimationTrack(liveAnimNode, speedScale);
            } else {
                int blendIdx = 0;

                for (int blendCount = node.blends2d.size(); blendIdx < blendCount; blendIdx++) {
                    Anim2DBlend ab = node.blends2d.get(blendIdx);
                    String animName = ab.animName;
                    if (StringUtils.equalsIgnoreCase(animName, node.animName)) {
                        this.startPrimaryAnimationTrack(liveAnimNode, speedScale);
                    } else {
                        this.startBlendFieldTrack(liveAnimNode, animName, speedScale);
                    }
                }
            }
        }
    }

    private void startRagdollTrack(LiveAnimNode liveAnimNode, float speedScale, float ragdollStartTime) {
        this.startTrackGeneric(liveAnimNode, "Ragdoll_" + liveAnimNode.getName(), false, speedScale, true, ragdollStartTime);
    }

    private void startBlendFieldTrack(LiveAnimNode liveAnimNode, String animName, float speedScale) {
        this.startTrackGeneric(liveAnimNode, animName, false, speedScale, false, -1.0F);
    }

    private void startPrimaryAnimationTrack(LiveAnimNode liveAnimNode, float speedScale) {
        liveAnimNode.selectRandomAnim();
        String animName = liveAnimNode.getAnimName();
        this.startTrackGeneric(liveAnimNode, animName, true, speedScale, false, -1.0F);
    }

    private void startTrackGeneric(
        LiveAnimNode liveAnimNode, String animName, boolean isPrimaryTrack, float speedScale, boolean isRagdoll, float ragdollStartTime
    ) {
        AnimNode node = liveAnimNode.getSourceNode();
        StartAnimTrackParameters params = StartAnimTrackParameters.alloc();
        params.animName = animName;
        params.isPrimary = isPrimaryTrack;
        params.isRagdoll = isRagdoll;
        params.ragdollStartTime = ragdollStartTime;
        params.ragdollMaxTime = node.getRagdollMaxTime();
        params.subLayerBoneWeights = node.subStateBoneWeights;
        params.syncTrackingEnabled = node.syncTrackingEnabled;
        params.trackTimeToVariable = node.trackTimeToVariable;
        params.speedScale = speedScale;
        params.initialWeight = liveAnimNode.getWeight();
        params.isLooped = liveAnimNode.isLooped();
        params.isReversed = node.isAnimReverse;
        params.deferredBoneName = node.getDeferredBoneName();
        params.deferredBoneAxis = node.getDeferredBoneAxis();
        params.useDeferredMovement = node.useDeferredMovement;
        params.useDeferredRotation = node.useDeferedRotation;
        params.deferredRotationScale = node.deferredRotationScale;
        params.priority = node.getPriority();
        params.matchingGrappledAnimNode = node.getMatchingGrappledAnimNode();
        AnimationTrack track = this.startTrackGeneric(params);
        if (track != null) {
            track.blendCurve = node.getBlendCurve();
            track.addListener(liveAnimNode);
            liveAnimNode.addMainTrack(track);
        }

        params.release();
    }

    private AnimationTrack startTrackGeneric(StartAnimTrackParameters params) {
        AnimationPlayer animPlayer = this.character.getAnimationPlayer();
        return !animPlayer.isReady() ? null : animPlayer.play(params, this);
    }

    /**
     * The layer's depth, how many layer ancestors (parent, grandparent, great-grandparent, etc) does this layer have.
     */
    public int getDepth() {
        return this.parentLayer != null ? this.parentLayer.getDepth() + 1 : 0;
    }

    public static int getDepth(AnimLayer layer) {
        return layer != null ? layer.getDepth() : -1;
    }

    private LiveAnimNode getHighestLiveNode() {
        if (this.liveAnimNodes.isEmpty()) {
            return null;
        } else {
            LiveAnimNode highestWeightNode = this.liveAnimNodes.getFirst();

            for (int i = this.liveAnimNodes.size() - 1; i >= 0; i--) {
                LiveAnimNode an = this.liveAnimNodes.get(i);
                if (an.getWeight() > highestWeightNode.getWeight()) {
                    highestWeightNode = an;
                }
            }

            return highestWeightNode;
        }
    }

    private AnimationTrack findSyncTrack(LiveAnimNode node) {
        AnimationTrack syncTrack = null;
        if (this.parentLayer != null) {
            syncTrack = this.parentLayer.getCurrentSyncTrack();
            if (syncTrack != null) {
                return syncTrack;
            }
        }

        int trackIdx = 0;

        for (int trackCount = node.getPlayingTrackCount(); trackIdx < trackCount; trackIdx++) {
            AnimationTrack track = node.getPlayingTrackAt(trackIdx);
            if (track.syncTrackingEnabled && track.hasClip() && (syncTrack == null || track.getBlendWeight() > syncTrack.getBlendWeight())) {
                syncTrack = track;
            }
        }

        return syncTrack;
    }

    public String getDebugNodeName() {
        String nodename = this.character.getAdvancedAnimator().animSet.name;
        if (this.state != null) {
            nodename = nodename + "/" + this.state.name;
            if (!this.liveAnimNodes.isEmpty()) {
                for (int i = 0; i < this.liveAnimNodes.size(); i++) {
                    LiveAnimNode liveAnimNode = this.liveAnimNodes.get(i);
                    if (this.state.nodes.contains(liveAnimNode.getSourceNode())) {
                        nodename = nodename + "/" + liveAnimNode.getName();
                        break;
                    }
                }
            }
        }

        return nodename;
    }

    public List<LiveAnimNode> getLiveAnimNodes() {
        return this.liveAnimNodes;
    }

    public boolean isRecording() {
        return this.character.getAnimationPlayer().isRecording();
    }

    public boolean isBlendingIn() {
        for (int i = 0; i < this.liveAnimNodes.size(); i++) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(i);
            if (liveAnimNode.isBlendingIn() && liveAnimNode.getWeight() < 0.9F) {
                return true;
            }
        }

        return false;
    }

    public boolean isBlendingOut() {
        for (int i = 0; i < this.liveAnimNodes.size(); i++) {
            LiveAnimNode liveAnimNode = this.liveAnimNodes.get(i);
            if (liveAnimNode.isBlendingOut()) {
                return true;
            }
        }

        return false;
    }

    private static final class Reusables {
        public final Vector2 animForwardDirection = new Vector2();
        public final Vector2 animTargetForwardDirection = new Vector2();
        public final Vector2 tweenedRot = new Vector2();
        public final Vector3 grappledPos = new Vector3();
        public final Vector3 grappledByPos = new Vector3();
        public final Vector3 currentPos = new Vector3();
        public final Vector3 grappleTweenStartPos = new Vector3();
        public final Vector3 tweenedPos = new Vector3();
        public final Anim2DBlendPicker.PickResults pickResults = new Anim2DBlendPicker.PickResults();
    }
}
