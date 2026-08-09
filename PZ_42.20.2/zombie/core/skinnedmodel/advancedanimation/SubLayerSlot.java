// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.Pool;
import zombie.util.PooledObject;

public class SubLayerSlot extends PooledObject {
    public boolean shouldBeActive;
    public int desiredLayer = -1;
    public AnimLayer animLayer;
    private static final Pool<SubLayerSlot> s_pool = new Pool<>(SubLayerSlot::new);
    private AnimState nextState;

    private SubLayerSlot() {
    }

    public static SubLayerSlot alloc(IAnimatable character, AdvancedAnimator parentAnimator) {
        SubLayerSlot newSlot = s_pool.alloc();
        newSlot.animLayer = AnimLayer.alloc(character, parentAnimator);
        return newSlot;
    }

    public void reset() {
        this.shouldBeActive = false;
        this.desiredLayer = -1;
        this.animLayer = Pool.tryRelease(this.animLayer);
    }

    @Override
    public void onReleased() {
        this.reset();
        super.onReleased();
    }

    public void update(float deltaT) {
        this.animLayer.Update(deltaT);
    }

    public void clearShouldBeActiveFlag() {
        this.shouldBeActive = false;
    }

    public void applyNextTransition() {
        if (this.animLayer != null) {
            if (!this.shouldBeActive) {
                this.animLayer.transitionTo(null);
                this.nextState = null;
            } else {
                if (this.nextState != null) {
                    this.animLayer.transitionTo(this.nextState);
                    this.nextState = null;
                }
            }
        }
    }

    public void setParentLayer(AnimLayer parentLayer) {
        this.animLayer.setParentLayer(parentLayer);
    }

    public void setParentSlot(SubLayerSlot parentSlot) {
        this.animLayer.setParentLayer(parentSlot != null ? parentSlot.animLayer : null);
    }

    public boolean hasRunningAnims() {
        return !this.animLayer.getLiveAnimNodes().isEmpty();
    }

    public void setNextTransitionTo(AnimState nextState) {
        this.nextState = nextState;
    }

    public boolean isNextOrCurrentState(AnimState state) {
        return this.nextState != null ? this.nextState == state : this.animLayer != null && this.animLayer.isCurrentState(state);
    }

    public static int compare(SubLayerSlot a, SubLayerSlot b) {
        if (a.desiredLayer != b.desiredLayer) {
            return Integer.compare(a.desiredLayer, b.desiredLayer);
        } else {
            return a.shouldBeActive != b.shouldBeActive
                ? Boolean.compare(a.shouldBeActive, b.shouldBeActive)
                : Integer.compare(a.animLayer.getDepth(), b.animLayer.getDepth());
        }
    }
}
