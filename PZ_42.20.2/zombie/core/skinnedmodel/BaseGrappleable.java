// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableCallbackMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugType;
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
    private boolean isPickingUpBody;
    private boolean isPuttingDownBody;
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

    public BaseGrappleable(IsoGameCharacter character) {
        this.character = character;
        this.isoMovingObject = this.character;
        this.parentGrappleable = this.character;
    }

    public BaseGrappleable(IsoDeadBody deadBody) {
        this.deadBody = deadBody;
        this.isoMovingObject = this.deadBody;
        this.parentGrappleable = this.deadBody;
    }

    @Override
    public IAnimatable getAnimatable() {
        return this.parentGrappleable.getAnimatable();
    }

    @Override
    public void Grappled(IGrappleable grappler, HandWeapon weapon, float grappleEffectiveness, String grappleType) {
        if (grappler == null) {
            DebugType.Grapple.warn("Grappler is null. Nothing to grapple us.");
        } else if (grappleEffectiveness < 0.5F) {
            DebugType.Grapple.debugln("Effectiveness insufficient. %f. Rejecting grapple.", grappleEffectiveness);
            grappler.RejectGrapple(this.getParentGrappleable());
        } else if (!this.canBeGrappled()) {
            DebugType.Grapple.debugln("No transition available to grappled state.");
            grappler.RejectGrapple(this.getParentGrappleable());
        } else {
            this.beingGrappled = true;
            this.grappledBy = grappler;
            this.sharedGrappleType = grappleType;
            this.sharedGrappleAnimNode = "";
            this.sharedGrappleTime = 0.0F;
            this.sharedGrappleFraction = 0.0F;
            DebugType.Grapple.debugln("Accepting grapple by: %s", this.getGrappledByString(), this.getGrappledBy().getClass().getName());
            grappler.AcceptGrapple(this.getParentGrappleable(), grappleType);
            this.invokeOnGrappleBeginEvent();
        }
    }

    @Override
    public void RejectGrapple(IGrappleable grappleRejector) {
        if (this.isGrappling() && !this.isGrapplingTarget(grappleRejector)) {
            DebugType.Grapple.warn("Target is not being grappled.");
        } else {
            DebugType.Grapple.debugln("Grapple rejected.");
            this.resetGrappleStateToDefault("Rejected");
        }
    }

    @Override
    public void AcceptGrapple(IGrappleable grappleAcceptor, String grappleType) {
        this.setGrapplingTarget(grappleAcceptor, grappleType);
        if (this.character.isLocal()) {
            this.character.setVariable("bearingFromGrappledTarget", this.getBearingFromGrappledTarget());
        }

        DebugType.Grapple.debugln("Grapple accepted. Grappled target: %s", this.getGrapplingTarget().getClass().getName());
        this.invokeOnGrappleBeginEvent();
    }

    @Override
    public void LetGoOfGrappled(String grappleResult) {
        if (!this.isGrappling()) {
            DebugType.Grapple.warn("Not currently grappling.");
        } else {
            IGrappleable grappledCharacterToLetGo = this.getGrapplingTarget();
            this.resetGrappleStateToDefault(grappleResult);
            if (grappledCharacterToLetGo == null) {
                DebugType.Grapple.warn("Nothing is being grappled. Nothing to let go of.");
            } else {
                DebugType.Grapple.debugln("Letting go of grappled. Result: %s", grappleResult);
                grappledCharacterToLetGo.GrapplerLetGo(this.getParentGrappleable(), grappleResult);
                this.invokeOnGrappleEndEvent();
            }
        }
    }

    @Override
    public void GrapplerLetGo(IGrappleable grappler, String grappleResult) {
        if (!this.isBeingGrappled()) {
            DebugType.Grapple.warn("GrapplerLetGo> Not currently being grappled,.");
        } else if (!this.isBeingGrappledBy(grappler)) {
            DebugType.Grapple.warn("GrapplerLetGo> Not being grappled by this character.");
        } else {
            DebugType.Grapple.debugln("Grappler has let us go. Result: %s.", grappleResult);
            this.resetGrappleStateToDefault(grappleResult);
            this.invokeOnGrappleEndEvent();
        }
    }

    private void resetGrappleStateToDefault() {
        this.resetGrappleStateToDefault("");
    }

    @Override
    public void resetGrappleStateToDefault(String grappleResult) {
        this.doGrapple = false;
        this.isPickingUpBody = false;
        this.isPuttingDownBody = false;
        this.doContinueGrapple = false;
        this.isGrappling = false;
        this.beingGrappled = false;
        this.grapplingTarget = null;
        this.grappleResult = grappleResult;
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
    public boolean isBeingGrappledBy(IGrappleable grappledBy) {
        return this.isBeingGrappled() && this.getGrappledBy() == grappledBy;
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 forwardDirection) {
        forwardDirection.set(1.0F, 0.0F);
        return forwardDirection;
    }

    @Override
    public Vector3f getTargetGrapplePos(Vector3f result) {
        result.set(0.0F, 0.0F, 0.0F);
        return result;
    }

    @Override
    public zombie.iso.Vector3 getTargetGrapplePos(zombie.iso.Vector3 result) {
        result.set(0.0F, 0.0F, 0.0F);
        return result;
    }

    @Override
    public void setTargetGrapplePos(Vector3f grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(grapplePos);
    }

    @Override
    public void setTargetGrapplePos(zombie.iso.Vector3 grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(grapplePos);
    }

    @Override
    public Vector2 getTargetGrappleRotation(Vector2 result) {
        return this.getParentGrappleable().getTargetGrappleRotation(result);
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
    public Vector3f getGrappleOffset(Vector3f result) {
        return this.getParentGrappleable().getGrappleOffset(result);
    }

    @Override
    public zombie.iso.Vector3 getGrappleOffset(zombie.iso.Vector3 result) {
        return this.getParentGrappleable().getGrappleOffset(result);
    }

    @Override
    public void setForwardDirection(float directionX, float directionY) {
        this.getParentGrappleable().setForwardDirection(directionX, directionY);
    }

    @Override
    public void setTargetAndCurrentDirection(float directionX, float directionY) {
        this.getParentGrappleable().setTargetAndCurrentDirection(directionX, directionY);
    }

    @Override
    public zombie.iso.Vector3 getPosition(zombie.iso.Vector3 position) {
        return this.getParentGrappleable().getPosition(position);
    }

    @Override
    public org.lwjgl.util.vector.Vector3f getPosition(org.lwjgl.util.vector.Vector3f position) {
        return this.getParentGrappleable().getPosition(position);
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
    public boolean isGrapplingTarget(IGrappleable grapplingTarget) {
        return this.getGrapplingTarget() == grapplingTarget;
    }

    @Override
    public IGrappleable getGrapplingTarget() {
        return !this.isGrappling() ? null : this.grapplingTarget;
    }

    private void setGrapplingTarget(IGrappleable grapplingTarget, String grappleType) {
        this.resetGrappleStateToDefault();
        this.isGrappling = true;
        this.doContinueGrapple = true;
        this.grapplingTarget = grapplingTarget;
        this.sharedGrappleType = grappleType;
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
    public void setSharedGrappleType(String sharedGrappleType) {
        if (!StringUtils.equals(this.sharedGrappleType, sharedGrappleType)) {
            this.sharedGrappleType = sharedGrappleType;
            IGrappleable grapplingTarget = this.getGrapplingTarget();
            if (grapplingTarget != null) {
                grapplingTarget.setSharedGrappleType(this.sharedGrappleType);
            }

            IGrappleable grappledBy = this.getGrappledBy();
            if (grappledBy != null) {
                grappledBy.setSharedGrappleType(this.sharedGrappleType);
            }

            this.isPickingUpBody = false;
        }
    }

    @Override
    public String getSharedGrappleAnimNode() {
        return this.sharedGrappleAnimNode;
    }

    @Override
    public void setSharedGrappleAnimNode(String sharedGrappleAnimNode) {
        this.sharedGrappleAnimNode = sharedGrappleAnimNode;
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
    public void setSharedGrappleAnimTime(float grappleAnimTime) {
        this.sharedGrappleTime = grappleAnimTime;
    }

    @Override
    public void setSharedGrappleAnimFraction(float grappleAnimFraction) {
        this.sharedGrappleFraction = grappleAnimFraction;
    }

    @Override
    public String getGrappleResult() {
        return this.grappleResult;
    }

    @Override
    public void setGrappleResult(String grappleResult) {
        this.grappleResult = grappleResult;
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
    public void setGrapplePosOffsetForward(float grappleOffsetForward) {
        this.grappleOffsetForward = grappleOffsetForward;
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
    public void setGrappleRotOffsetYaw(float grappleOffsetYaw) {
        this.grappleOffsetYaw = grappleOffsetYaw;
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
    public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour newBehaviour) {
        this.grappleOffsetBehaviour = newBehaviour;
    }

    @Override
    public boolean isDoGrapple() {
        return this.doGrapple || this.isPerformingGrappleGrabAnimation();
    }

    public boolean isPickingUpBody() {
        return this.isPickingUpBody;
    }

    public boolean isPuttingDownBody() {
        return this.isPuttingDownBody;
    }

    @Override
    public void setDoGrapple(boolean doGrapple) {
        this.doGrapple = doGrapple;
    }

    @Override
    public boolean isDoContinueGrapple() {
        return this.doContinueGrapple;
    }

    @Override
    public void setDoContinueGrapple(boolean doContinueGrapple) {
        boolean wasContinuingGrapple = this.doContinueGrapple;
        this.doContinueGrapple = doContinueGrapple;
        if (this.isGrappling && wasContinuingGrapple && !doContinueGrapple) {
            this.isPuttingDownBody = true;
        }
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
    public void setPerformingGrappleGrabAnimation(boolean grappleGrabAnim) {
        this.isPerformingGrappleGrabAnim = grappleGrabAnim;
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
    public void setOnFloor(boolean onFloor) {
        if (this.isoMovingObject != null) {
            this.isoMovingObject.setOnFloor(onFloor);
        }
    }

    @Override
    public boolean isFallOnFront() {
        return this.character != null && this.character.isFallOnFront() || this.deadBody != null && this.deadBody.isFallOnFront();
    }

    @Override
    public void setFallOnFront(boolean fallOnFront) {
        if (this.character != null) {
            this.character.setFallOnFront(fallOnFront);
        }

        if (this.deadBody != null) {
            this.deadBody.setFallOnFront(fallOnFront);
        }
    }

    @Override
    public boolean isKilledByFall() {
        return this.character != null && this.character.isKilledByFall() || this.deadBody != null && this.deadBody.isKilledByFall();
    }

    @Override
    public void setKilledByFall(boolean killedByFall) {
        if (this.character != null) {
            this.character.setKilledByFall(killedByFall);
        }

        if (this.deadBody != null) {
            this.deadBody.setKilledByFall(killedByFall);
        }
    }

    public void setOnGrappledBeginCallback(Invokers.Params0.ICallback onGrappleBegin) {
        this.onGrappleBeginCallback = onGrappleBegin;
    }

    private void invokeOnGrappleBeginEvent() {
        this.isPickingUpBody = true;
        if (this.onGrappleBeginCallback != null) {
            this.onGrappleBeginCallback.accept();
        }
    }

    public void setOnGrappledEndCallback(Invokers.Params0.ICallback onGrappleBegin) {
        this.onGrappleEndCallback = onGrappleBegin;
    }

    private void invokeOnGrappleEndEvent() {
        this.isPuttingDownBody = false;
        if (this.onGrappleEndCallback != null) {
            this.onGrappleEndCallback.accept();
        }
    }

    public static void RegisterGrappleVariables(IAnimationVariableCallbackMap variableMap, IGrappleable grappleable) {
        variableMap.setVariable("bDoGrapple", grappleable::isDoGrapple, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bDoContinueGrapple", grappleable::isDoContinueGrapple, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bIsGrappling", grappleable::isGrappling, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappleResult", grappleable::getGrappleResult, grappleable::setGrappleResult, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleType", grappleable::getSharedGrappleType, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable(
            "sharedGrappleAnimNode", grappleable::getSharedGrappleAnimNode, grappleable::setSharedGrappleAnimNode, IAnimationVariableSlotDescriptor.Null
        );
        variableMap.setVariable("sharedGrappleTime", grappleable::getSharedGrappleAnimTime, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleFraction", grappleable::getSharedGrappleAnimFraction, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable(
            "grappleOffsetForward", grappleable::getGrapplePosOffsetForward, grappleable::setGrapplePosOffsetForward, IAnimationVariableSlotDescriptor.Null
        );
        variableMap.setVariable(
            "grappleOffsetBehaviour",
            GrappleOffsetBehaviour.class,
            grappleable::getGrappleOffsetBehaviour,
            grappleable::setGrappleoffsetBehaviour,
            IAnimationVariableSlotDescriptor.Null
        );
        variableMap.setVariable("bearingToGrappledTarget", grappleable::getBearingToGrappledTarget, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bBeingGrappled", grappleable::isBeingGrappled, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappledBy", grappleable::getGrappledByString, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappledByType", grappleable::getGrappledByType, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable(
            "GrappleGrabAnim",
            grappleable::isPerformingGrappleGrabAnimation,
            grappleable::setPerformingGrappleGrabAnimation,
            IAnimationVariableSlotDescriptor.Null
        );
        variableMap.setVariable("GrappleAnim", grappleable::isPerformingGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("AnyGrappleAnim", grappleable::isPerformingAnyGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
    }
}
