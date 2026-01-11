// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableCallbackMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;

public class BaseGrappleable implements IGrappleable {
    private IsoGameCharacter character;
    private IsoDeadBody deadBody;
    private IsoMovingObject isoMovingObject;
    private IGrappleable parentGrappleable;
    private boolean doGrapple;
    private boolean doContinueGrapple;
    private boolean beingGrappled;
    private IGrappleable grappledBy;
    private boolean isGrappling;
    private IGrappleable grapplingTarget;
    private String sharedGrappleType = "";
    private String sharedGrappleAnimNode = "";
    private float sharedGrappleTime;
    private float sharedGrappleFraction;
    private String grappleResult = "";
    private float grappleOffsetForward;
    private float grappleOffsetYaw;
    private GrappleOffsetBehaviour grappleOffsetBehaviour = GrappleOffsetBehaviour.NONE;
    private boolean isPerformingGrappleGrabAnim;
    private Invokers.Params0.ICallback onGrappleBeginCallback;
    private Invokers.Params0.ICallback onGrappleEndCallback;

    public BaseGrappleable() {
    }

    public BaseGrappleable(IsoGameCharacter in_character) {
        this.character = in_character;
        this.isoMovingObject = this.character;
        this.parentGrappleable = this.character;
    }

    public BaseGrappleable(IsoDeadBody in_deadBody) {
        this.deadBody = in_deadBody;
        this.isoMovingObject = this.deadBody;
        this.parentGrappleable = this.deadBody;
    }

    @Override
    public IAnimatable getAnimatable() {
        return this.parentGrappleable.getAnimatable();
    }

    @Override
    public void Grappled(IGrappleable in_grappler, HandWeapon in_grapplersWeapon, float in_grappleEffectiveness, String in_grappleType) {
        if (in_grappler == null) {
            DebugLog.Grapple.warn("Grappler is null. Nothing to grapple us.");
        } else if (in_grappleEffectiveness < 0.5F) {
            DebugLog.Grapple.debugln("Effectiveness insufficient. %f. Rejecting grapple.", in_grappleEffectiveness);
            in_grappler.RejectGrapple(this.getParentGrappleable());
        } else if (!this.canBeGrappled()) {
            DebugLog.Grapple.debugln("No transition available to grappled state.");
            in_grappler.RejectGrapple(this.getParentGrappleable());
        } else {
            this.beingGrappled = true;
            this.grappledBy = in_grappler;
            this.sharedGrappleType = in_grappleType;
            this.sharedGrappleAnimNode = "";
            this.sharedGrappleTime = 0.0F;
            this.sharedGrappleFraction = 0.0F;
            DebugLog.Grapple.debugln("Accepting grapple by: %s", this.getGrappledByString(), this.getGrappledBy().getClass().getName());
            in_grappler.AcceptGrapple(this.getParentGrappleable(), in_grappleType);
            this.invokeOnGrappleBeginEvent();
        }
    }

    @Override
    public void RejectGrapple(IGrappleable in_target) {
        if (this.isGrappling() && !this.isGrapplingTarget(in_target)) {
            DebugLog.Grapple.warn("Target is not being grappled.");
        } else {
            DebugLog.Grapple.debugln("Grapple rejected.");
            this.resetGrappleStateToDefault("Rejected");
        }
    }

    @Override
    public void AcceptGrapple(IGrappleable in_target, String in_grappleType) {
        this.setGrapplingTarget(in_target, in_grappleType);
        DebugLog.Grapple.debugln("Grapple accepted. Grappled target: %s", this.getGrapplingTarget().getClass().getName());
        this.invokeOnGrappleBeginEvent();
    }

    @Override
    public void LetGoOfGrappled(String in_grappleResult) {
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grappling.");
        } else {
            IGrappleable grappledCharacterToLetGo = this.getGrapplingTarget();
            this.resetGrappleStateToDefault(in_grappleResult);
            if (grappledCharacterToLetGo == null) {
                DebugLog.Grapple.warn("Nothing is being grappled. Nothing to let go of.");
            } else {
                DebugLog.Grapple.debugln("Letting go of grappled. Result: %s", in_grappleResult);
                grappledCharacterToLetGo.GrapplerLetGo(this.getParentGrappleable(), in_grappleResult);
                this.invokeOnGrappleEndEvent();
            }
        }
    }

    @Override
    public void GrapplerLetGo(IGrappleable in_grappler, String in_grappleResult) {
        if (!this.isBeingGrappled()) {
            DebugLog.Grapple.warn("GrapplerLetGo> Not currently being grappled,.");
        } else if (!this.isBeingGrappledBy(in_grappler)) {
            DebugLog.Grapple.warn("GrapplerLetGo> Not being grappled by this character.");
        } else {
            DebugLog.Grapple.debugln("Grappler has let us go. Result: %s.", in_grappleResult);
            this.resetGrappleStateToDefault(in_grappleResult);
            this.invokeOnGrappleEndEvent();
        }
    }

    private void resetGrappleStateToDefault() {
        this.resetGrappleStateToDefault("");
    }

    @Override
    public void resetGrappleStateToDefault(String in_grappleResult) {
        this.doGrapple = false;
        this.doContinueGrapple = false;
        this.isGrappling = false;
        this.beingGrappled = false;
        this.grapplingTarget = null;
        this.grappleResult = in_grappleResult;
        this.sharedGrappleType = "";
        this.sharedGrappleAnimNode = "";
        this.sharedGrappleTime = 0.0F;
        this.sharedGrappleFraction = 0.0F;
        this.grappleOffsetForward = 0.0F;
        this.grappleOffsetBehaviour = GrappleOffsetBehaviour.NONE;
        this.setGrappleDeferredOffset(0.0F, 0.0F, 0.0F);
    }

    @Override
    public boolean isBeingGrappled() {
        return this.beingGrappled;
    }

    @Override
    public boolean isBeingGrappledBy(IGrappleable in_grappledBy) {
        return this.isBeingGrappled() && this.getGrappledBy() == in_grappledBy;
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 out_forwardDirection) {
        out_forwardDirection.set(1.0F, 0.0F);
        return out_forwardDirection;
    }

    @Override
    public Vector3f getTargetGrapplePos(Vector3f out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        return out_result;
    }

    @Override
    public zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 out_result) {
        out_result.set(0.0F, 0.0F, 0.0F);
        return out_result;
    }

    @Override
    public void setTargetGrapplePos(Vector3f in_grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(in_grapplePos);
    }

    @Override
    public void setTargetGrapplePos(zombie.iso.Vector3 in_grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(in_grapplePos);
    }

    @Override
    public Vector2 getTargetGrappleRotation(Vector2 out_forward) {
        return this.getParentGrappleable().getTargetGrappleRotation(out_forward);
    }

    @Override
    public void setTargetGrappleRotation(float x, float y) {
        this.getParentGrappleable().setTargetGrappleRotation(x, y);
    }

    @Override
    public void setTargetGrapplePos(float x, float y, float z) {
        this.getParentGrappleable().setTargetGrapplePos(x, y, z);
    }

    @Override
    public void setGrappleDeferredOffset(float x, float y, float z) {
        this.getParentGrappleable().setGrappleDeferredOffset(x, y, z);
    }

    @Override
    public Vector3f getGrappleOffset(Vector3f out_result) {
        return this.getParentGrappleable().getGrappleOffset(out_result);
    }

    @Override
    public zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 out_result) {
        return this.getParentGrappleable().getGrappleOffset(out_result);
    }

    @Override
    public void setForwardDirection(float in_directionX, float in_directionY) {
        this.getParentGrappleable().setForwardDirection(in_directionX, in_directionY);
    }

    @Override
    public void setTargetAndCurrentDirection(float in_directionX, float in_directionY) {
        this.getParentGrappleable().setTargetAndCurrentDirection(in_directionX, in_directionY);
    }

    @Override
    public zombie.iso.Vector3 getPosition(zombie.iso.Vector3 out_position) {
        return this.getParentGrappleable().getPosition(out_position);
    }

    @Override
    public org.lwjgl.util.vector.Vector3f getPosition(org.lwjgl.util.vector.Vector3f out_position) {
        return this.getParentGrappleable().getPosition(out_position);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        this.getParentGrappleable().setPosition(x, y, z);
    }

    @Override
    public IGrappleable getGrappledBy() {
        return this.isBeingGrappled() ? this.grappledBy : null;
    }

    @Override
    public String getGrappledByString() {
        if (this.isBeingGrappled()) {
            return this.grappledBy != null ? this.grappledBy.getClass().getName() + "_" + this.grappledBy.getID() : "null";
        } else {
            return "";
        }
    }

    @Override
    public String getGrappledByType() {
        if (this.isBeingGrappled()) {
            return this.grappledBy != null ? this.grappledBy.getClass().getName() : "null";
        } else {
            return "None";
        }
    }

    @Override
    public boolean isGrappling() {
        return this.isGrappling;
    }

    @Override
    public boolean isGrapplingTarget(IGrappleable in_grapplingTarget) {
        return this.getGrapplingTarget() == in_grapplingTarget;
    }

    @Override
    public IGrappleable getGrapplingTarget() {
        return !this.isGrappling() ? null : this.grapplingTarget;
    }

    private void setGrapplingTarget(IGrappleable in_grapplingTarget, String in_grappleType) {
        this.resetGrappleStateToDefault();
        this.isGrappling = true;
        this.doContinueGrapple = true;
        this.grapplingTarget = in_grapplingTarget;
        this.sharedGrappleType = in_grappleType;
    }

    @Override
    public float getBearingToGrappledTarget() {
        IGrappleable grappledTarget = this.getGrapplingTarget();
        return grappledTarget == null
            ? 0.0F
            : PZMath.calculateBearing(
                this.getPosition(new zombie.iso.Vector3()), this.getAnimForwardDirection(new Vector2()), grappledTarget.getPosition(new zombie.iso.Vector3())
            );
    }

    @Override
    public float getBearingFromGrappledTarget() {
        IGrappleable grappledTarget = this.getGrapplingTarget();
        return grappledTarget == null
            ? 0.0F
            : PZMath.calculateBearing(
                grappledTarget.getPosition(new zombie.iso.Vector3()),
                grappledTarget.getAnimForwardDirection(new Vector2()),
                this.getPosition(new zombie.iso.Vector3())
            );
    }

    @Override
    public String getSharedGrappleType() {
        return this.sharedGrappleType;
    }

    @Override
    public void setSharedGrappleType(String in_sharedGrappleType) {
        if (!StringUtils.equals(this.sharedGrappleType, in_sharedGrappleType)) {
            this.sharedGrappleType = in_sharedGrappleType;
            IGrappleable grapplingTarget = this.getGrapplingTarget();
            if (grapplingTarget != null) {
                grapplingTarget.setSharedGrappleType(this.sharedGrappleType);
            }

            IGrappleable grappledBy = this.getGrappledBy();
            if (grappledBy != null) {
                grappledBy.setSharedGrappleType(this.sharedGrappleType);
            }
        }
    }

    @Override
    public String getSharedGrappleAnimNode() {
        return this.sharedGrappleAnimNode;
    }

    @Override
    public void setSharedGrappleAnimNode(String in_grappleAnim) {
        this.sharedGrappleAnimNode = in_grappleAnim;
    }

    @Override
    public float getSharedGrappleAnimTime() {
        return this.sharedGrappleTime;
    }

    @Override
    public float getSharedGrappleAnimFraction() {
        return this.sharedGrappleFraction;
    }

    @Override
    public void setSharedGrappleAnimTime(float in_grappleAnimTime) {
        this.sharedGrappleTime = in_grappleAnimTime;
    }

    @Override
    public void setSharedGrappleAnimFraction(float in_grappleAnimFraction) {
        this.sharedGrappleFraction = in_grappleAnimFraction;
    }

    @Override
    public String getGrappleResult() {
        return this.grappleResult;
    }

    @Override
    public void setGrappleResult(String in_grappleResult) {
        this.grappleResult = in_grappleResult;
    }

    public IGrappleable getParentGrappleable() {
        return this.parentGrappleable;
    }

    @Override
    public boolean canBeGrappled() {
        IGrappleable parentGrappleable = this.getParentGrappleable();
        return parentGrappleable != null && parentGrappleable.canBeGrappled();
    }

    @Override
    public void setGrapplePosOffsetForward(float in_grappleOffsetForward) {
        this.grappleOffsetForward = in_grappleOffsetForward;
    }

    @Override
    public float getGrapplePosOffsetForward() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrapplePosOffsetForward();
        } else {
            return this.isGrappling() ? this.grappleOffsetForward : 0.0F;
        }
    }

    @Override
    public void setGrappleRotOffsetYaw(float in_grappleOffsetYaw) {
        this.grappleOffsetYaw = in_grappleOffsetYaw;
    }

    @Override
    public float getGrappleRotOffsetYaw() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrappleRotOffsetYaw();
        } else {
            return this.isGrappling() ? this.grappleOffsetYaw : 0.0F;
        }
    }

    @Override
    public GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrappleOffsetBehaviour();
        } else {
            return this.isGrappling() ? this.grappleOffsetBehaviour : GrappleOffsetBehaviour.NONE;
        }
    }

    @Override
    public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour in_newBehaviour) {
        this.grappleOffsetBehaviour = in_newBehaviour;
    }

    @Override
    public boolean isDoGrapple() {
        return this.doGrapple || this.isPerformingGrappleGrabAnimation();
    }

    @Override
    public void setDoGrapple(boolean in_bDoGrapple) {
        this.doGrapple = in_bDoGrapple;
    }

    @Override
    public boolean isDoContinueGrapple() {
        return this.doContinueGrapple;
    }

    @Override
    public void setDoContinueGrapple(boolean in_continueGrapple) {
        this.doContinueGrapple = in_continueGrapple;
    }

    @Override
    public boolean isPerformingAnyGrappleAnimation() {
        return this.isPerformingGrappleGrabAnimation() || this.isPerformingGrappleAnimation();
    }

    @Override
    public boolean isPerformingGrappleGrabAnimation() {
        return this.isPerformingGrappleGrabAnim;
    }

    @Override
    public void setPerformingGrappleGrabAnimation(boolean in_grappleGrabAnim) {
        this.isPerformingGrappleGrabAnim = in_grappleGrabAnim;
    }

    @Override
    public boolean isPerformingGrappleAnimation() {
        return this.getParentGrappleable().isPerformingGrappleAnimation();
    }

    @Override
    public boolean isOnFloor() {
        return this.isoMovingObject != null && this.isoMovingObject.isOnFloor();
    }

    @Override
    public void setOnFloor(boolean in_bOnFloor) {
        if (this.isoMovingObject != null) {
            this.isoMovingObject.setOnFloor(in_bOnFloor);
        }
    }

    @Override
    public boolean isFallOnFront() {
        return this.character != null && this.character.isFallOnFront() || this.deadBody != null && this.deadBody.isFallOnFront();
    }

    @Override
    public void setFallOnFront(boolean in_bFallOnFront) {
        if (this.character != null) {
            this.character.setFallOnFront(in_bFallOnFront);
        }

        if (this.deadBody != null) {
            this.deadBody.setFallOnFront(in_bFallOnFront);
        }
    }

    @Override
    public boolean isKilledByFall() {
        return this.character != null && this.character.isKilledByFall() || this.deadBody != null && this.deadBody.isKilledByFall();
    }

    @Override
    public void setKilledByFall(boolean in_bKilledByFall) {
        if (this.character != null) {
            this.character.setKilledByFall(in_bKilledByFall);
        }

        if (this.deadBody != null) {
            this.deadBody.setKilledByFall(in_bKilledByFall);
        }
    }

    public void setOnGrappledBeginCallback(Invokers.Params0.ICallback in_onGrappleBegin) {
        this.onGrappleBeginCallback = in_onGrappleBegin;
    }

    private void invokeOnGrappleBeginEvent() {
        if (this.onGrappleBeginCallback != null) {
            this.onGrappleBeginCallback.accept();
        }
    }

    public void setOnGrappledEndCallback(Invokers.Params0.ICallback in_onGrappleBegin) {
        this.onGrappleEndCallback = in_onGrappleBegin;
    }

    private void invokeOnGrappleEndEvent() {
        if (this.onGrappleEndCallback != null) {
            this.onGrappleEndCallback.accept();
        }
    }

    public static void RegisterGrappleVariables(IAnimationVariableCallbackMap in_variableMap, IGrappleable in_grappleable) {
        in_variableMap.setVariable("bDoGrapple", in_grappleable::isDoGrapple, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("bDoContinueGrapple", in_grappleable::isDoContinueGrapple, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("bIsGrappling", in_grappleable::isGrappling, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("grappleResult", in_grappleable::getGrappleResult, in_grappleable::setGrappleResult, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("sharedGrappleType", in_grappleable::getSharedGrappleType, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable(
            "sharedGrappleAnimNode", in_grappleable::getSharedGrappleAnimNode, in_grappleable::setSharedGrappleAnimNode, IAnimationVariableSlotDescriptor.Null
        );
        in_variableMap.setVariable("sharedGrappleTime", in_grappleable::getSharedGrappleAnimTime, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("sharedGrappleFraction", in_grappleable::getSharedGrappleAnimFraction, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable(
            "grappleOffsetForward",
            in_grappleable::getGrapplePosOffsetForward,
            in_grappleable::setGrapplePosOffsetForward,
            IAnimationVariableSlotDescriptor.Null
        );
        in_variableMap.setVariable(
            "grappleOffsetBehaviour",
            GrappleOffsetBehaviour.class,
            in_grappleable::getGrappleOffsetBehaviour,
            in_grappleable::setGrappleoffsetBehaviour,
            IAnimationVariableSlotDescriptor.Null
        );
        in_variableMap.setVariable("bearingToGrappledTarget", in_grappleable::getBearingToGrappledTarget, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("bearingFromGrappledTarget", in_grappleable::getBearingFromGrappledTarget, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("bBeingGrappled", in_grappleable::isBeingGrappled, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("grappledBy", in_grappleable::getGrappledByString, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("grappledByType", in_grappleable::getGrappledByType, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable(
            "GrappleGrabAnim",
            in_grappleable::isPerformingGrappleGrabAnimation,
            in_grappleable::setPerformingGrappleGrabAnimation,
            IAnimationVariableSlotDescriptor.Null
        );
        in_variableMap.setVariable("GrappleAnim", in_grappleable::isPerformingGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
        in_variableMap.setVariable("AnyGrappleAnim", in_grappleable::isPerformingAnyGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
    }
}
