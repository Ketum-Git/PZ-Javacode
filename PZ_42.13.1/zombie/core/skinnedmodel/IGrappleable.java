// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;

public interface IGrappleable {
    void Grappled(IGrappleable arg0, HandWeapon arg1, float arg2, String arg3);

    void AcceptGrapple(IGrappleable arg0, String arg1);

    void RejectGrapple(IGrappleable arg0);

    void LetGoOfGrappled(String arg0);

    void GrapplerLetGo(IGrappleable arg0, String arg1);

    GrappleOffsetBehaviour getGrappleOffsetBehaviour();

    void setGrappleoffsetBehaviour(GrappleOffsetBehaviour arg0);

    boolean isDoGrapple();

    void setDoGrapple(boolean arg0);

    default void setDoGrappleLetGo() {
        this.setDoContinueGrapple(false);
    }

    IAnimatable getAnimatable();

    static IAnimatable getAnimatable(IGrappleable in_grappleable) {
        return in_grappleable != null ? in_grappleable.getAnimatable() : null;
    }

    boolean isDoContinueGrapple();

    void setDoContinueGrapple(boolean arg0);

    IGrappleable getGrappledBy();

    String getGrappledByString();

    String getGrappledByType();

    boolean isGrappling();

    boolean isBeingGrappled();

    boolean isBeingGrappledBy(IGrappleable arg0);

    Vector2 getAnimForwardDirection(Vector2 arg0);

    Vector3f getTargetGrapplePos(Vector3f arg0);

    zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 arg0);

    default void setTargetGrapplePos(Vector3f in_grapplePos) {
        this.setTargetGrapplePos(in_grapplePos.x, in_grapplePos.y, in_grapplePos.z);
    }

    default void setTargetGrapplePos(zombie.iso.Vector3 in_grapplePos) {
        this.setTargetGrapplePos(in_grapplePos.x, in_grapplePos.y, in_grapplePos.z);
    }

    void setTargetGrapplePos(float arg0, float arg1, float arg2);

    Vector2 getTargetGrappleRotation(Vector2 arg0);

    default void setTargetGrappleRotation(Vector2 in_forward) {
        this.setTargetGrappleRotation(in_forward.x, in_forward.y);
    }

    void setTargetGrappleRotation(float arg0, float arg1);

    default void setGrappleDeferredOffset(Vector3f in_grappleOffset) {
        this.setGrappleDeferredOffset(in_grappleOffset.x, in_grappleOffset.y, in_grappleOffset.z);
    }

    default void setGrappleDeferredOffset(zombie.iso.Vector3 in_grappleOffset) {
        this.setGrappleDeferredOffset(in_grappleOffset.x, in_grappleOffset.y, in_grappleOffset.z);
    }

    void setGrappleDeferredOffset(float arg0, float arg1, float arg2);

    Vector3f getGrappleOffset(Vector3f arg0);

    zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 arg0);

    void setForwardDirection(float arg0, float arg1);

    void setTargetAndCurrentDirection(float arg0, float arg1);

    zombie.iso.Vector3 getPosition(zombie.iso.Vector3 arg0);

    org.lwjgl.util.vector.Vector3f getPosition(org.lwjgl.util.vector.Vector3f arg0);

    default void setPosition(zombie.iso.Vector3 in_position) {
        this.setPosition(in_position.x, in_position.y, in_position.z);
    }

    void setPosition(float arg0, float arg1, float arg2);

    float getGrapplePosOffsetForward();

    void setGrapplePosOffsetForward(float arg0);

    float getGrappleRotOffsetYaw();

    void setGrappleRotOffsetYaw(float arg0);

    boolean isGrapplingTarget(IGrappleable arg0);

    IGrappleable getGrapplingTarget();

    float getBearingToGrappledTarget();

    float getBearingFromGrappledTarget();

    String getSharedGrappleType();

    void setSharedGrappleType(String arg0);

    String getSharedGrappleAnimNode();

    void setSharedGrappleAnimNode(String arg0);

    float getSharedGrappleAnimTime();

    float getSharedGrappleAnimFraction();

    void setSharedGrappleAnimTime(float arg0);

    void setSharedGrappleAnimFraction(float arg0);

    String getGrappleResult();

    void setGrappleResult(String arg0);

    default int getID() {
        return -1;
    }

    boolean canBeGrappled();

    boolean isPerformingAnyGrappleAnimation();

    boolean isPerformingGrappleGrabAnimation();

    void setPerformingGrappleGrabAnimation(boolean arg0);

    boolean isPerformingGrappleAnimation();

    boolean isOnFloor();

    void setOnFloor(boolean arg0);

    boolean isFallOnFront();

    void setFallOnFront(boolean arg0);

    boolean isKilledByFall();

    void setKilledByFall(boolean arg0);

    default boolean isMoving() {
        return false;
    }

    void resetGrappleStateToDefault(String arg0);
}
