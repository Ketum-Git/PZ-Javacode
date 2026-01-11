// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.debug.DebugLog;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class SubLayerSlot extends PooledObject {
    public boolean shouldBeActive;
    public AnimLayer animLayer;
    private static final Pool<SubLayerSlot> s_pool = new Pool<>(SubLayerSlot::new);

    private SubLayerSlot() {
    }

    public static SubLayerSlot alloc(AnimLayer in_parentLayer, IAnimatable character, AdvancedAnimator in_parentAnimator) {
        SubLayerSlot newSlot = s_pool.alloc();
        newSlot.animLayer = AnimLayer.alloc(in_parentLayer, character, in_parentAnimator);
        return newSlot;
    }

    public void reset() {
        this.shouldBeActive = false;
        this.animLayer = Pool.tryRelease(this.animLayer);
    }

    @Override
    public void onReleased() {
        this.reset();
        super.onReleased();
    }

    public void update(float in_deltaT) {
        this.animLayer.Update(in_deltaT);
    }

    public void transitionTo(AnimState in_newState, AnimLayer in_sourceLayer) {
        DebugLog.AnimationDetailed
            .debugln(
                "SubLayerSlot: TransitionTo: from Anim <%s> to State <%s>",
                this.animLayer.getLiveAnimNodes().isEmpty() ? "NoAnim" : this.animLayer.getLiveAnimNodes().get(0).getName(),
                in_newState != null ? in_newState.name : "NoState"
            );
        this.animLayer.transitionTo(in_newState, in_sourceLayer);
        this.shouldBeActive = in_newState != null;
    }

    public void applyTransition() {
        if (!this.shouldBeActive) {
            this.transitionTo(null, null);
        }
    }

    public void setParentLayer(AnimLayer in_parentLayer) {
        this.animLayer.setParentLayer(in_parentLayer);
    }

    public boolean isStateless() {
        return this.animLayer.isStateless();
    }

    public boolean hasRunningAnims() {
        return !this.animLayer.getLiveAnimNodes().isEmpty();
    }
}
