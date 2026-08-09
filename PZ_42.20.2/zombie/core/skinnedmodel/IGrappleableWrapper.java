// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;

public interface IGrappleableWrapper extends IGrappleable {
    IGrappleable getWrappedGrappleable();

    @Override
    default boolean isDoGrapple() {
        return this.getWrappedGrappleable().isDoGrapple();
    }

    @Override
    default void setDoGrapple(boolean doGrapple) {
        this.getWrappedGrappleable().setDoGrapple(doGrapple);
    }

    @Override
    default boolean isDoContinueGrapple() {
        return this.getWrappedGrappleable().isDoContinueGrapple();
    }

    @Override
    default void setDoContinueGrapple(boolean doContinueGrapple) {
        this.getWrappedGrappleable().setDoContinueGrapple(doContinueGrapple);
    }

    @Override
    default void Grappled(IGrappleable grappler, HandWeapon weapon, float grappleEffectiveness, String grappleType) {
        this.getWrappedGrappleable().Grappled(grappler, weapon, grappleEffectiveness, grappleType);
    }

    @Override
    default void RejectGrapple(IGrappleable grappleRejector) {
        this.getWrappedGrappleable().RejectGrapple(grappleRejector);
    }

    @Override
    default void AcceptGrapple(IGrappleable grappleAcceptor, String grappleType) {
        this.getWrappedGrappleable().AcceptGrapple(grappleAcceptor, grappleType);
    }

    @Override
    default void LetGoOfGrappled(String grappleResult) {
        this.getWrappedGrappleable().LetGoOfGrappled(grappleResult);
    }

    @Override
    default void GrapplerLetGo(IGrappleable grappler, String grappleResult) {
        this.getWrappedGrappleable().GrapplerLetGo(grappler, grappleResult);
    }

    @Override
    default GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
        return this.getWrappedGrappleable().getGrappleOffsetBehaviour();
    }

    @Override
    default void setGrappleoffsetBehaviour(GrappleOffsetBehaviour newBehaviour) {
        this.getWrappedGrappleable().setGrappleoffsetBehaviour(newBehaviour);
    }

    @Override
    default boolean isBeingGrappled() {
        return this.getWrappedGrappleable().isBeingGrappled();
    }

    @Override
    default boolean isBeingGrappledBy(IGrappleable grappledBy) {
        return this.getWrappedGrappleable().isBeingGrappledBy(grappledBy);
    }

    @Override
    default IGrappleable getGrappledBy() {
        return this.getWrappedGrappleable().getGrappledBy();
    }

    @Override
    default String getGrappledByString() {
        return this.getWrappedGrappleable().getGrappledByString();
    }

    @Override
    default String getGrappledByType() {
        return this.getWrappedGrappleable().getGrappledByType();
    }

    @Override
    default boolean isGrappling() {
        return this.getWrappedGrappleable().isGrappling();
    }

    @Override
    default boolean isGrapplingTarget(IGrappleable grapplingTarget) {
        return this.getWrappedGrappleable().isGrapplingTarget(grapplingTarget);
    }

    @Override
    default IGrappleable getGrapplingTarget() {
        return this.getWrappedGrappleable().getGrapplingTarget();
    }

    @Override
    default float getBearingToGrappledTarget() {
        return this.getWrappedGrappleable().getBearingToGrappledTarget();
    }

    @Override
    default float getBearingFromGrappledTarget() {
        return this.getWrappedGrappleable().getBearingFromGrappledTarget();
    }

    @Override
    default String getSharedGrappleType() {
        return this.getWrappedGrappleable().getSharedGrappleType();
    }

    @Override
    default void setSharedGrappleType(String sharedGrappleType) {
        this.getWrappedGrappleable().setSharedGrappleType(sharedGrappleType);
    }

    @Override
    default String getSharedGrappleAnimNode() {
        return this.getWrappedGrappleable().getSharedGrappleAnimNode();
    }

    @Override
    default void setSharedGrappleAnimNode(String sharedGrappleAnimNode) {
        this.getWrappedGrappleable().setSharedGrappleAnimNode(sharedGrappleAnimNode);
    }

    @Override
    default float getSharedGrappleAnimTime() {
        return this.getWrappedGrappleable().getSharedGrappleAnimTime();
    }

    @Override
    default float getSharedGrappleAnimFraction() {
        return this.getWrappedGrappleable().getSharedGrappleAnimFraction();
    }

    @Override
    default void setSharedGrappleAnimTime(float grappleAnimTime) {
        this.getWrappedGrappleable().setSharedGrappleAnimTime(grappleAnimTime);
    }

    @Override
    default void setSharedGrappleAnimFraction(float grappleAnimFraction) {
        this.getWrappedGrappleable().setSharedGrappleAnimFraction(grappleAnimFraction);
    }

    @Override
    default String getGrappleResult() {
        return this.getWrappedGrappleable().getGrappleResult();
    }

    @Override
    default void setGrappleResult(String grappleResult) {
        this.getWrappedGrappleable().setGrappleResult(grappleResult);
    }

    @Override
    default void setGrapplePosOffsetForward(float grappleOffsetForward) {
        this.getWrappedGrappleable().setGrapplePosOffsetForward(grappleOffsetForward);
    }

    @Override
    default float getGrappleRotOffsetYaw() {
        return this.getWrappedGrappleable().getGrappleRotOffsetYaw();
    }

    @Override
    default void setGrappleRotOffsetYaw(float grappleOffsetYaw) {
        this.getWrappedGrappleable().setGrappleRotOffsetYaw(grappleOffsetYaw);
    }

    @Override
    default float getGrapplePosOffsetForward() {
        return this.getWrappedGrappleable().getGrapplePosOffsetForward();
    }

    @Override
    default void setTargetAndCurrentDirection(float directionX, float directionY) {
        this.setForwardDirection(directionX, directionY);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetAndCurrentDirection(directionX, directionY);
        }
    }

    @Override
    default Vector3f getTargetGrapplePos(Vector3f result) {
        result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(result);
        }

        return result;
    }

    @Override
    default zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 result) {
        result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(result);
        }

        return result;
    }

    @Override
    default void setTargetGrapplePos(float x, float y, float z) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetGrapplePos(x, y, z);
        }
    }

    @Override
    default void setTargetGrappleRotation(float x, float y) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetGrappleRotation(x, y);
        }
    }

    @Override
    default Vector2 getTargetGrappleRotation(Vector2 result) {
        result.set(1.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrappleRotation(result);
        }

        return result;
    }

    @Override
    default Vector3f getGrappleOffset(Vector3f result) {
        result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(result);
        }

        return result;
    }

    @Override
    default zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 result) {
        result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(result);
        }

        return result;
    }

    @Override
    default void setGrappleDeferredOffset(float x, float y, float z) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setGrappleOffset(x, y, z);
        }
    }

    @Override
    default boolean canBeGrappled() {
        return !this.isBeingGrappled();
    }

    @Override
    default boolean isPerformingAnyGrappleAnimation() {
        return this.getWrappedGrappleable().isPerformingAnyGrappleAnimation();
    }

    @Override
    default boolean isPerformingGrappleGrabAnimation() {
        return this.getWrappedGrappleable().isPerformingGrappleGrabAnimation();
    }

    @Override
    default void setPerformingGrappleGrabAnimation(boolean grappleGrabAnim) {
        this.getWrappedGrappleable().setPerformingGrappleGrabAnimation(grappleGrabAnim);
    }

    @Override
    default boolean isOnFloor() {
        return this.getWrappedGrappleable().isOnFloor();
    }

    @Override
    default void setOnFloor(boolean onFloor) {
        this.getWrappedGrappleable().setOnFloor(onFloor);
    }

    @Override
    default void resetGrappleStateToDefault(String grappleResult) {
        this.getWrappedGrappleable().resetGrappleStateToDefault(grappleResult);
    }
}
