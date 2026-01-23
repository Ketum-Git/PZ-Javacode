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
    default void setDoGrapple(boolean bDoGrapple) {
        this.getWrappedGrappleable().setDoGrapple(bDoGrapple);
    }

    @Override
    default boolean isDoContinueGrapple() {
        return this.getWrappedGrappleable().isDoContinueGrapple();
    }

    @Override
    default void setDoContinueGrapple(boolean in_bDoContinueGrapple) {
        this.getWrappedGrappleable().setDoContinueGrapple(in_bDoContinueGrapple);
    }

    @Override
    default void Grappled(IGrappleable in_grappler, HandWeapon in_grapplersWeapon, float in_grappleEffectiveness, String in_grappleType) {
        this.getWrappedGrappleable().Grappled(in_grappler, in_grapplersWeapon, in_grappleEffectiveness, in_grappleType);
    }

    @Override
    default void RejectGrapple(IGrappleable in_target) {
        this.getWrappedGrappleable().RejectGrapple(in_target);
    }

    @Override
    default void AcceptGrapple(IGrappleable in_target, String in_grappleType) {
        this.getWrappedGrappleable().AcceptGrapple(in_target, in_grappleType);
    }

    @Override
    default void LetGoOfGrappled(String in_grappleResult) {
        this.getWrappedGrappleable().LetGoOfGrappled(in_grappleResult);
    }

    @Override
    default void GrapplerLetGo(IGrappleable in_grappler, String in_grappleResult) {
        this.getWrappedGrappleable().GrapplerLetGo(in_grappler, in_grappleResult);
    }

    @Override
    default GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
        return this.getWrappedGrappleable().getGrappleOffsetBehaviour();
    }

    @Override
    default void setGrappleoffsetBehaviour(GrappleOffsetBehaviour in_newBehaviour) {
        this.getWrappedGrappleable().setGrappleoffsetBehaviour(in_newBehaviour);
    }

    @Override
    default boolean isBeingGrappled() {
        return this.getWrappedGrappleable().isBeingGrappled();
    }

    @Override
    default boolean isBeingGrappledBy(IGrappleable in_grappledBy) {
        return this.getWrappedGrappleable().isBeingGrappledBy(in_grappledBy);
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
    default boolean isGrapplingTarget(IGrappleable in_grapplingTarget) {
        return this.getWrappedGrappleable().isGrapplingTarget(in_grapplingTarget);
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
    default void setSharedGrappleType(String in_sharedGrappleType) {
        this.getWrappedGrappleable().setSharedGrappleType(in_sharedGrappleType);
    }

    @Override
    default String getSharedGrappleAnimNode() {
        return this.getWrappedGrappleable().getSharedGrappleAnimNode();
    }

    @Override
    default void setSharedGrappleAnimNode(String in_grappleAnim) {
        this.getWrappedGrappleable().setSharedGrappleAnimNode(in_grappleAnim);
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
    default void setSharedGrappleAnimTime(float in_grappleAnimTime) {
        this.getWrappedGrappleable().setSharedGrappleAnimTime(in_grappleAnimTime);
    }

    @Override
    default void setSharedGrappleAnimFraction(float in_grappleAnimFraction) {
        this.getWrappedGrappleable().setSharedGrappleAnimFraction(in_grappleAnimFraction);
    }

    @Override
    default String getGrappleResult() {
        return this.getWrappedGrappleable().getGrappleResult();
    }

    @Override
    default void setGrappleResult(String in_grappleResult) {
        this.getWrappedGrappleable().setGrappleResult(in_grappleResult);
    }

    @Override
    default void setGrapplePosOffsetForward(float in_grappleOffsetForward) {
        this.getWrappedGrappleable().setGrapplePosOffsetForward(in_grappleOffsetForward);
    }

    @Override
    default float getGrappleRotOffsetYaw() {
        return this.getWrappedGrappleable().getGrappleRotOffsetYaw();
    }

    @Override
    default void setGrappleRotOffsetYaw(float in_grappleOffsetYaw) {
        this.getWrappedGrappleable().setGrappleRotOffsetYaw(in_grappleOffsetYaw);
    }

    @Override
    default float getGrapplePosOffsetForward() {
        return this.getWrappedGrappleable().getGrapplePosOffsetForward();
    }

    @Override
    default void setTargetAndCurrentDirection(float in_directionX, float in_directionY) {
        this.setForwardDirection(in_directionX, in_directionY);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetAndCurrentDirection(in_directionX, in_directionY);
        }
    }

    @Override
    default Vector3f getTargetGrapplePos(Vector3f out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(out_result);
        }

        return out_result;
    }

    @Override
    default zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(out_result);
        }

        return out_result;
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
    default Vector2 getTargetGrappleRotation(Vector2 out_forward) {
        out_forward.set(1.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrappleRotation(out_forward);
        }

        return out_forward;
    }

    @Override
    default Vector3f getGrappleOffset(Vector3f out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(out_result);
        }

        return out_result;
    }

    @Override
    default zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(out_result);
        }

        return out_result;
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
    default void setPerformingGrappleGrabAnimation(boolean in_grappleGrabAnim) {
        this.getWrappedGrappleable().setPerformingGrappleGrabAnimation(in_grappleGrabAnim);
    }

    @Override
    default boolean isOnFloor() {
        return this.getWrappedGrappleable().isOnFloor();
    }

    @Override
    default void setOnFloor(boolean in_bOnFloor) {
        this.getWrappedGrappleable().setOnFloor(in_bOnFloor);
    }

    @Override
    default void resetGrappleStateToDefault(String in_grappleResult) {
        this.getWrappedGrappleable().resetGrappleStateToDefault(in_grappleResult);
    }
}
