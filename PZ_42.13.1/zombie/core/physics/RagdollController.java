// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.RagdollBuilder;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.scripting.objects.RagdollBodyDynamics;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class RagdollController extends PooledObject {
    private static final float ActiveRagdollDistance = 1.0F;
    public static final float MovementThreshold = 0.01F;
    public static final float MovementThresholdTime = 1.5F;
    private static final float simulationTimeoutDecayFactor = 10.0F;
    public static float vehicleCollisionFriction = 0.4F;
    private static final float[] skeletonBuffer = new float[245];
    private static final float[] rigidBodyBuffer = new float[77];
    private static final float[] impulseBuffer = new float[6];
    private static final RagdollBodyDynamics vehicleRagdollBodyDynamics = new RagdollBodyDynamics();
    private static final float[] vehicleRagdollBodyDynamicsParams = new float[8];
    private boolean isInitialized;
    private boolean isUpright = true;
    private boolean isOnBack;
    private final Vector3 headPosition = new Vector3();
    private final Vector3 pelvisPosition = new Vector3();
    private final Vector3 previousHeadPosition = new Vector3();
    private final Vector3 previousPelvisPosition = new Vector3();
    private boolean addedToWorld;
    private final RagdollControllerDebugRenderer.DebugDrawSettings debugDrawSettings = new RagdollControllerDebugRenderer.DebugDrawSettings();
    private int simulationState = -1;
    private IsoGameCharacter gameCharacterObject;
    private boolean wasContactingVehicle;
    private final RagdollStateData ragdollStateData = new RagdollStateData();
    private Keyframe[] keyframesForBone;
    private final Vector3f ragdollWorldPosition = new Vector3f();
    private final Vector3f ragdollWorldPositionPzBullet = new Vector3f();
    private final Quaternion ragdollWorldRotationPzBullet = new Quaternion();
    private final Quaternion ragdollLocalRotation = new Quaternion();
    private final RagdollController.Reusables_Quaternions quaternions = new RagdollController.Reusables_Quaternions();
    private static int numberOfActiveSimulations;
    private static final Pool<RagdollController> ragdollControllerPool = new Pool<>(RagdollController::new);

    private RagdollController() {
    }

    public static RagdollController alloc() {
        return ragdollControllerPool.alloc();
    }

    public RagdollStateData getRagdollStateData() {
        return this.ragdollStateData;
    }

    public boolean isIsoPlayer() {
        return Type.tryCastTo(this.getGameCharacterObject(), IsoPlayer.class) != null;
    }

    public boolean isSimulationSleeping() {
        return this.simulationState == RagdollController.SimulationState.ISLAND_SLEEPING.ordinal();
    }

    public boolean isSimulationActive() {
        return this.ragdollStateData.isSimulating;
    }

    public IsoGameCharacter getGameCharacterObject() {
        return this.gameCharacterObject;
    }

    public void setGameCharacterObject(IsoGameCharacter gameCharacterObject) {
        this.gameCharacterObject = gameCharacterObject;
    }

    public int getID() {
        return this.getGameCharacterObject().getID();
    }

    public RagdollControllerDebugRenderer.DebugDrawSettings getDebugDrawSettings() {
        return this.debugDrawSettings;
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public boolean isUpright() {
        return this.isUpright;
    }

    public void setUpright(boolean upright) {
        this.isUpright = upright;
    }

    public boolean isOnBack() {
        return this.isOnBack;
    }

    public void setOnBack(boolean onBack) {
        this.isOnBack = onBack;
    }

    public Vector3 getHeadPosition(Vector3 out_headPosition) {
        out_headPosition.set(this.headPosition);
        return out_headPosition;
    }

    public void setHeadPosition(Vector3 headPosition) {
        this.headPosition.set(headPosition);
    }

    public Vector3 getPelvisPosition(Vector3 out_pelvisPosition) {
        out_pelvisPosition.set(this.pelvisPosition);
        return out_pelvisPosition;
    }

    public float getPelvisPositionX() {
        return this.pelvisPosition.x;
    }

    public float getPelvisPositionY() {
        return this.pelvisPosition.y;
    }

    public float getPelvisPositionZ() {
        return this.pelvisPosition.z;
    }

    public void setPelvisPosition(Vector3 pelvisPosition) {
        this.pelvisPosition.set(pelvisPosition);
    }

    private boolean initialize() {
        if (this.isInitialized()) {
            return true;
        } else {
            DebugType.Ragdoll.debugln("Initializing...");
            if (this.getAnimationPlayer() != null && this.getAnimationPlayer().isReady()) {
                this.ragdollStateData.reset();
                this.ragdollStateData.isSimulating = true;
                this.addToWorld();
                this.updateRagdollSkeleton();
                this.setActive(true);
                return true;
            } else {
                DebugType.Ragdoll.warn("AnimationPlayer is not ready. %s", this.getGameCharacterObject());
                return false;
            }
        }
    }

    private void reset() {
        this.removeFromWorld();
        this.ragdollStateData.reset();
        this.isUpright = true;
        this.isOnBack = false;
        this.simulationState = -1;
        this.gameCharacterObject.setRagdollFall(false);
        this.isInitialized = false;
        this.gameCharacterObject.setUsePhysicHitReaction(false);
    }

    public void reinitialize() {
        this.reset();
        this.isInitialized = this.initialize();
    }

    public static Vector3f pzSpaceToBulletSpace(Vector3f inout_result) {
        float x = inout_result.x;
        float y = inout_result.y;
        float z = inout_result.z;
        inout_result.x = x;
        inout_result.y = z * 2.44949F;
        inout_result.z = y;
        return inout_result;
    }

    public void setActive(boolean active) {
        this.updateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        Bullet.setRagdollActive(this.getID(), active);
    }

    public void addToWorld() {
        if (!this.addedToWorld) {
            numberOfActiveSimulations++;
            DebugType.Ragdoll.debugln("Adding to world: Character:%s ID: %d", this.getGameCharacterObject(), this.getID());
            this.calculateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
            Bullet.addRagdoll(this.getID(), this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
            this.addedToWorld = true;
        }
    }

    private void removeFromWorld() {
        if (this.addedToWorld) {
            numberOfActiveSimulations--;
            Bullet.removeRagdoll(this.getID());
            this.addedToWorld = false;
        }
    }

    public void updateRagdollSkeleton() {
        int id = this.getID();
        this.setRagdollLocalRotation();
        this.updateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        this.uploadAnimationBoneTransformsToRagdoll(id);
        this.uploadAnimationBonePreviousTransformsToRagdoll(id);
    }

    private void uploadAnimationBoneTransformsToRagdoll(int id) {
        this.getBoneTransformsFromAnimation(skeletonBuffer);
        Bullet.updateRagdollSkeletonTransforms(id, this.getNumberOfBones(), skeletonBuffer);
    }

    private void uploadAnimationBonePreviousTransformsToRagdoll(int id) {
        if (DebugOptions.instance.character.debug.ragdoll.enableInitialVelocities.getValue()) {
            AnimationPlayer animationPlayer = this.getAnimationPlayer();
            float deltaT = animationPlayer.getBoneTransformsTimeDelta();
            if (!(deltaT <= 0.0F)) {
                this.getBoneTransformVelocitiesFromAnimation(skeletonBuffer);
                Bullet.updateRagdollSkeletonPreviousTransforms(id, this.getNumberOfBones(), deltaT, skeletonBuffer);
            }
        }
    }

    public void update(float in_deltaT, Vector3f out_ragdollWorldPosition, Quaternion out_ragdollWorldRotation) {
        if (!this.isInitialized()) {
            this.isInitialized = this.initialize();
        } else {
            DebugType.Ragdoll.trace("Simulating Ragdoll for Character:%s ID: %d", this.getGameCharacterObject(), this.getID());
            this.simulateRagdoll(
                this.getID(), this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet, skeletonBuffer, rigidBodyBuffer
            );
            this.updateSimulationStateID();
            this.simulateHitReaction();
            if (Core.debug) {
                RagdollControllerDebugRenderer.updateDebug(this);
            }

            out_ragdollWorldPosition.set(this.ragdollWorldPosition);
            out_ragdollWorldRotation.set(this.ragdollWorldRotationPzBullet);
        }
    }

    public void postUpdate(float in_deltaT) {
        RagdollStateData ragdollStateData = this.getRagdollStateData();
        this.calculateSimulationData(ragdollStateData, in_deltaT);
        this.updateSimulationTimeout(ragdollStateData);
    }

    private void updateSimulationTimeout(RagdollStateData ragdollStateData) {
        boolean endSimulation = false;
        if (ragdollStateData.simulationTimeout > 0.0F) {
            ragdollStateData.simulationTimeout = ragdollStateData.simulationTimeout - GameTime.getInstance().getTimeDelta();
        }

        if (!ragdollStateData.isSimulationMovement && ragdollStateData.simulationTimeout <= 0.0F) {
            endSimulation = true;
        }

        if (this.isSimulationSleeping() && endSimulation) {
            ragdollStateData.isSimulating = false;
            this.setActive(false);
        }
    }

    private void simulateHitReaction() {
        BallisticsTarget ballisticsTarget = this.gameCharacterObject.getBallisticsTarget();
        if (ballisticsTarget != null && !ballisticsTarget.getCombatDamageDataProcessed()) {
            BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
            if (combatDamageData != null && combatDamageData.bodyPart != RagdollBodyPart.BODYPART_COUNT) {
                ballisticsTarget.setCombatDamageDataProcessed(true);
                boolean dismember = false;
                RagdollJoint dismemberJoint = RagdollJoint.JOINT_COUNT;
                DebugType.Ragdoll.debugln("RagdollState: HitReaction %s", this.gameCharacterObject.getHitReaction());
                String hitReaction = this.gameCharacterObject.getHitReaction();
                RagdollSettingsManager ragdollSettingsManager = RagdollSettingsManager.getInstance();
                if (ragdollSettingsManager.isForcedHitReaction()) {
                    hitReaction = ragdollSettingsManager.getForcedHitReactionLocationAsShotLocation();
                }

                RagdollBodyPart bodyPart = combatDamageData.bodyPart;
                this.gameCharacterObject.setHitReaction("");
                if (DebugOptions.instance.character.debug.ragdoll.physics.physicsHitReaction.getValue()) {
                    Vector3 direction = new Vector3();
                    Vector3 targetPosition = new Vector3();
                    Vector3 attackerPosition = new Vector3();
                    combatDamageData.target.getPosition(targetPosition);
                    combatDamageData.attacker.getPosition(attackerPosition);
                    ballisticsTarget.setCombatDamageDataProcessed(true);
                    float impulse;
                    float upwardImpulse;
                    if (combatDamageData.handWeapon.getAmmoType() != null) {
                        impulse = PhysicsHitReactionScript.getImpulse(bodyPart, combatDamageData.handWeapon.getAmmoType());
                        upwardImpulse = PhysicsHitReactionScript.getUpwardImpulse(bodyPart, combatDamageData.handWeapon.getAmmoType());
                    } else {
                        if (!combatDamageData.handWeapon.isExplosive()) {
                            return;
                        }

                        impulse = PhysicsHitReactionScript.getImpulse(bodyPart, combatDamageData.handWeapon.getPhysicsObject());
                        upwardImpulse = PhysicsHitReactionScript.getUpwardImpulse(bodyPart, combatDamageData.handWeapon.getPhysicsObject());
                        combatDamageData.handWeapon.getAttackTargetSquare(attackerPosition);
                    }

                    targetPosition.sub(attackerPosition, direction);
                    direction.normalize();
                    impulseBuffer[0] = direction.x * impulse;
                    impulseBuffer[1] = upwardImpulse;
                    impulseBuffer[2] = direction.y * impulse;
                    impulseBuffer[3] = 0.0F;
                    impulseBuffer[4] = 0.0F;
                    impulseBuffer[5] = 0.0F;
                    Bullet.applyImpulse(this.gameCharacterObject.getID(), bodyPart.ordinal(), impulseBuffer);
                    if (DebugOptions.instance.character.debug.ragdoll.physics.allowJointConstraintDetach.getValue()
                        && false & dismemberJoint != RagdollJoint.JOINT_COUNT) {
                        Bullet.detachConstraint(this.gameCharacterObject.getID(), dismemberJoint.ordinal());
                        this.gameCharacterObject.getAnimationPlayer().dismember(this.getJointAssociatedBone(dismemberJoint).ordinal());
                    }
                }
            }
        }
    }

    private void simulateHitReaction0() {
        if (this.gameCharacterObject.hasHitReaction()) {
            boolean dismember = false;
            RagdollJoint dismemberJoint = RagdollJoint.JOINT_COUNT;
            DebugType.Ragdoll.debugln("RagdollState: HitReaction %s", this.gameCharacterObject.getHitReaction());
            String hitReaction = this.gameCharacterObject.getHitReaction();
            RagdollSettingsManager ragdollSettingsManager = RagdollSettingsManager.getInstance();
            if (ragdollSettingsManager.isForcedHitReaction()) {
                hitReaction = ragdollSettingsManager.getForcedHitReactionLocationAsShotLocation();
            }

            RagdollBodyPart bodyPart = RagdollBodyPart.BODYPART_PELVIS;
            switch (hitReaction) {
                case "ShotBelly":
                case "ShotBellyStep":
                    bodyPart = RagdollBodyPart.BODYPART_PELVIS;
                    break;
                case "ShotChest":
                case "ShotChestR":
                case "ShotChestL":
                    bodyPart = RagdollBodyPart.BODYPART_SPINE;
                    break;
                case "ShotLegR":
                    bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
                    break;
                case "ShotLegL":
                    bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
                    break;
                case "ShotShoulderStepR":
                    bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
                    break;
                case "ShotShoulderStepL":
                    bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
                    break;
                case "ShotHeadFwd":
                case "ShotHeadBwd":
                case "ShotHeadFwd02":
                    bodyPart = RagdollBodyPart.BODYPART_HEAD;
                    break;
                default:
                    DebugType.Ragdoll.debugln("RagdollState: HitReaction %s CASE NOT DEFINED", this.gameCharacterObject.getHitReaction());
                    return;
            }

            this.gameCharacterObject.setHitReaction("");
            float impulse;
            float upImpulse;
            if (ragdollSettingsManager.isForcedHitReaction()) {
                RagdollSettingsManager.HitReactionSetting hitReactionSetting = ragdollSettingsManager.getHitReactionSetting(0);
                impulse = ragdollSettingsManager.getGlobalImpulseSetting();
                upImpulse = ragdollSettingsManager.getGlobalUpImpulseSetting();
                if (!hitReactionSetting.isEnableAdmin()) {
                    boolean enabled = ragdollSettingsManager.getEnabledSetting(bodyPart);
                    if (enabled) {
                        impulse = ragdollSettingsManager.getImpulseSetting(bodyPart);
                        upImpulse = ragdollSettingsManager.getUpImpulseSetting(bodyPart);
                    }
                }
            } else {
                impulse = ragdollSettingsManager.getSandboxHitReactionImpulseStrength();
                upImpulse = ragdollSettingsManager.getSandboxHitReactionUpImpulseStrength();
            }

            Vector3 direction = new Vector3();
            BallisticsTarget ballisticsTarget = this.gameCharacterObject.getBallisticsTarget();
            if (ballisticsTarget != null) {
                Vector3 targetPosition = new Vector3();
                Vector3 attackerPosition = new Vector3();
                BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
                combatDamageData.target.getPosition(targetPosition);
                combatDamageData.attacker.getPosition(attackerPosition);
                targetPosition.sub(attackerPosition, direction);
                direction.normalize();
            }

            impulseBuffer[0] = direction.x * impulse;
            impulseBuffer[1] = direction.z * upImpulse;
            impulseBuffer[2] = direction.y * impulse;
            impulseBuffer[3] = 0.0F;
            impulseBuffer[4] = 0.0F;
            impulseBuffer[5] = 0.0F;
            Bullet.applyImpulse(this.gameCharacterObject.getID(), bodyPart.ordinal(), impulseBuffer);
            if (false & dismemberJoint != RagdollJoint.JOINT_COUNT) {
                Bullet.detachConstraint(this.gameCharacterObject.getID(), dismemberJoint.ordinal());
                this.gameCharacterObject.getAnimationPlayer().dismember(this.getJointAssociatedBone(dismemberJoint).ordinal());
            }
        }
    }

    private SkeletonBone getJointAssociatedBone(RagdollJoint joint) {
        SkeletonBone bone = SkeletonBone.Dummy01;
        switch (joint) {
            case JOINT_PELVIS_SPINE:
                bone = SkeletonBone.Bip01_Pelvis;
                break;
            case JOINT_SPINE_HEAD:
                bone = SkeletonBone.Bip01_Neck;
                break;
            case JOINT_LEFT_HIP:
                bone = SkeletonBone.Bip01_L_Thigh;
                break;
            case JOINT_LEFT_KNEE:
                bone = SkeletonBone.Bip01_L_Calf;
                break;
            case JOINT_RIGHT_HIP:
                bone = SkeletonBone.Bip01_R_Thigh;
                break;
            case JOINT_RIGHT_KNEE:
                bone = SkeletonBone.Bip01_R_Calf;
                break;
            case JOINT_LEFT_SHOULDER:
                bone = SkeletonBone.Bip01_L_UpperArm;
                break;
            case JOINT_LEFT_ELBOW:
                bone = SkeletonBone.Bip01_L_Forearm;
                break;
            case JOINT_RIGHT_SHOULDER:
                bone = SkeletonBone.Bip01_R_UpperArm;
                break;
            case JOINT_RIGHT_ELBOW:
                bone = SkeletonBone.Bip01_R_Forearm;
        }

        return bone;
    }

    public void debugRender() {
        if (DebugOptions.instance.character.debug.ragdoll.render.pelvisLocation.getValue()) {
            RagdollControllerDebugRenderer.drawIsoDebug(
                this.getGameCharacterObject(), this.isOnBack, this.isUpright, this.pelvisPosition, this.ragdollStateData
            );
        }
    }

    public void simulateRagdoll(
        int id,
        Vector3f out_ragdollWorldPosition,
        Vector3f out_ragdollWorldPositionPZBullet,
        Quaternion out_ragdollWorldRotationPZBullet,
        float[] inout_skeletonBuffer,
        float[] inout_rigidBodyBuffer
    ) {
        this.updateRagdollWorldTransform(out_ragdollWorldPosition, out_ragdollWorldPositionPZBullet, out_ragdollWorldRotationPZBullet);
        this.setRagdollLocalRotation();
        int numberOfBones = Bullet.simulateRagdollWithRigidBodyOutput(id, inout_skeletonBuffer, inout_rigidBodyBuffer);
        this.setBoneTransformsToAnimation(inout_skeletonBuffer, numberOfBones);
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        AnimationPlayer animPlayer = gameCharacterObject.getAnimationPlayer();
        this.getRagdollStateData().simulationRenderedAngle = animPlayer.getRenderedAngle();
        this.getRagdollStateData().simulationCharacterForwardAngle = gameCharacterObject.getAnimAngleRadians();
    }

    private void setRagdollLocalRotation() {
        Quaternion ragdollLocalRotation = this.getRagdollLocalRotation(this.ragdollLocalRotation);
        Bullet.setRagdollLocalTransformRotation(this.getID(), ragdollLocalRotation.x, ragdollLocalRotation.y, ragdollLocalRotation.z, ragdollLocalRotation.w);
    }

    private void updateRagdollWorldTransform(
        Vector3f out_ragdollWorldPosition, Vector3f out_ragdollWorldPositionPZBullet, Quaternion out_ragdollWorldRotationPZBullet
    ) {
        int id = this.getID();
        this.calculateRagdollWorldTransform(out_ragdollWorldPosition, out_ragdollWorldPositionPZBullet, out_ragdollWorldRotationPZBullet);
        Bullet.updateRagdoll(id, out_ragdollWorldPositionPZBullet, out_ragdollWorldRotationPZBullet);
    }

    private void calculateRagdollWorldTransform(Vector3f out_position, Vector3f out_positionPZBullet, Quaternion out_ragdollWorldRotationPZBullet) {
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        gameCharacterObject.getPosition(out_position);
        out_positionPZBullet.set(out_position);
        pzSpaceToBulletSpace(out_positionPZBullet);
        float forward = gameCharacterObject.getAnimAngleRadians();
        this.calculateRagdollWorldRotation(forward, out_ragdollWorldRotationPZBullet);
    }

    private Quaternion calculateRagdollWorldRotation(float in_characterForwardAngle, Quaternion out_result) {
        Quaternion quatYaw = PZMath.setFromAxisAngle(0.0F, 1.0F, 0.0F, -in_characterForwardAngle, this.quaternions.yAxis);
        out_result.set(quatYaw);
        return out_result;
    }

    private Quaternion getRagdollLocalRotation(Quaternion out_result) {
        Quaternion quatXaxis = PZMath.setFromAxisAngle(1.0F, 0.0F, 0.0F, (float) Math.PI, this.quaternions.xAxis);
        Quaternion quatZaxis = PZMath.setFromAxisAngle(0.0F, 0.0F, 1.0F, (float) Math.PI, this.quaternions.zAxis);
        Quaternion.mul(quatZaxis, quatXaxis, out_result);
        Quaternion quatYaw = PZMath.setFromAxisAngle(0.0F, 0.0F, 1.0F, (float) (-Math.PI / 2), this.quaternions.yawAdjust);
        Quaternion.mul(out_result, quatYaw, out_result);
        return out_result;
    }

    public void updateSimulationStateID() {
        this.simulationState = Bullet.getRagdollSimulationState(this.getID());
    }

    private void setBoneTransformsToAnimation(float[] in_floats, int in_numberOfBones) {
        AnimationClip ragdollSimulationAnimationClip = this.getRagdollSimulationAnimationClip();
        if (ragdollSimulationAnimationClip == null) {
            DebugType.Ragdoll.warn("No Ragdoll Simulation AnimationClip found,");
        } else {
            ragdollSimulationAnimationClip.setRagdollSimulationActive(true);
            SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
            if (skeletonHierarchy == null) {
                DebugType.Ragdoll.warn("No Skeleton found,");
            } else {
                Vector3f pos = HelperFunctions.allocVector3f();
                Quaternion rot = HelperFunctions.allocQuaternion();
                Vector3f scale = HelperFunctions.allocVector3f(1.0F, 1.0F, 1.0F);
                SkeletonBone[] skeletonBones = SkeletonBone.all();
                int floatArrayIndex = 0;

                for (int i = 0; i < in_numberOfBones; i++) {
                    SkeletonBone skeletonBone = skeletonBones[i];
                    SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
                    pos.x = in_floats[floatArrayIndex++] / 1.5F;
                    pos.y = in_floats[floatArrayIndex++] / 1.5F;
                    pos.z = in_floats[floatArrayIndex++] / 1.5F;
                    float rx = in_floats[floatArrayIndex++];
                    float ry = in_floats[floatArrayIndex++];
                    float rz = in_floats[floatArrayIndex++];
                    float rw = in_floats[floatArrayIndex++];
                    rot.set(rx, ry, rz, rw);
                    if (bone != null) {
                        pos.x *= -1.0F;
                        pos.y *= -1.0F;
                        pos.z *= -1.0F;
                        int boneIndex = bone.index;
                        this.setBoneKeyframePRS(ragdollSimulationAnimationClip, boneIndex, pos, rot, scale);
                    }
                }

                HelperFunctions.releaseVector3f(pos);
                HelperFunctions.releaseQuaternion(rot);
                HelperFunctions.releaseVector3f(scale);
            }
        }
    }

    private void getBoneTransformsFromAnimation(float[] inout_floats) {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        Vector3f position = HelperFunctions.allocVector3f();
        Quaternion rotation = HelperFunctions.allocQuaternion();
        int floatArrayIndex = 0;

        for (SkeletonBone skeletonBone : SkeletonBone.all()) {
            SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
            if (bone != null) {
                int boneIndex = bone.index;
                AnimatorsBoneTransform boneTransform = animationPlayer.getBoneTransformAt(boneIndex);
                boneTransform.getPosition(position);
                boneTransform.getRotation(rotation);
            } else {
                position.set(0.0F, 0.0F, 0.0F);
                rotation.setIdentity();
            }

            inout_floats[floatArrayIndex++] = -position.x * 1.5F;
            inout_floats[floatArrayIndex++] = -position.y * 1.5F;
            inout_floats[floatArrayIndex++] = -position.z * 1.5F;
            inout_floats[floatArrayIndex++] = rotation.x;
            inout_floats[floatArrayIndex++] = rotation.y;
            inout_floats[floatArrayIndex++] = rotation.z;
            inout_floats[floatArrayIndex++] = rotation.w;
        }

        HelperFunctions.releaseVector3f(position);
        HelperFunctions.releaseQuaternion(rotation);
    }

    private void getBoneTransformVelocitiesFromAnimation(float[] inout_floats) {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        Vector3f previousPos = HelperFunctions.allocVector3f();
        Quaternion previousRot = HelperFunctions.allocQuaternion();
        Vector3f currentPos = HelperFunctions.allocVector3f();
        Quaternion currentRot = HelperFunctions.allocQuaternion();
        Vector3f velocityPos = HelperFunctions.allocVector3f();
        Quaternion velocityRot = HelperFunctions.allocQuaternion();
        BoneTransform previousTransform = BoneTransform.alloc();
        int floatArrayIndex = 0;

        for (SkeletonBone skeletonBone : SkeletonBone.all()) {
            SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
            if (bone != null) {
                int boneIndex = bone.index;
                AnimatorsBoneTransform boneTransform = animationPlayer.getBoneTransformAt(boneIndex);
                boneTransform.getPosition(currentPos);
                boneTransform.getRotation(currentRot);
                boneTransform.getPreviousTransform(previousTransform);
                previousTransform.getPosition(previousPos);
                previousTransform.getRotation(previousRot);
                float deltaT = boneTransform.getTimeDelta();
                velocityPos.x = (currentPos.x - previousPos.x) / deltaT;
                velocityPos.y = (currentPos.y - previousPos.y) / deltaT;
                velocityPos.z = (currentPos.z - previousPos.z) / deltaT;
                velocityRot.x = (currentRot.x - previousRot.x) / deltaT;
                velocityRot.y = (currentRot.y - previousRot.y) / deltaT;
                velocityRot.z = (currentRot.z - previousRot.z) / deltaT;
                velocityRot.w = (currentRot.w - previousRot.w) / deltaT;
            } else {
                velocityPos.set(0.0F, 0.0F, 0.0F);
                velocityRot.setIdentity();
            }

            inout_floats[floatArrayIndex++] = -velocityPos.x * 1.5F;
            inout_floats[floatArrayIndex++] = -velocityPos.y * 1.5F;
            inout_floats[floatArrayIndex++] = -velocityPos.z * 1.5F;
            inout_floats[floatArrayIndex++] = velocityRot.x;
            inout_floats[floatArrayIndex++] = velocityRot.y;
            inout_floats[floatArrayIndex++] = velocityRot.z;
            inout_floats[floatArrayIndex++] = velocityRot.w;
        }

        HelperFunctions.releaseVector3f(previousPos);
        HelperFunctions.releaseQuaternion(previousRot);
        HelperFunctions.releaseVector3f(currentPos);
        HelperFunctions.releaseQuaternion(currentRot);
        HelperFunctions.releaseVector3f(velocityPos);
        HelperFunctions.releaseQuaternion(velocityRot);
        previousTransform.release();
    }

    private Keyframe[] getKeyframesForBone(int boneIndex) {
        AnimationClip ragdollSimulationAnimationClip = this.getRagdollSimulationAnimationClip();
        if (ragdollSimulationAnimationClip == null) {
            DebugType.Ragdoll.warn("No Ragdoll Simulation AnimationClip found,");
            return null;
        } else {
            SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
            if (skeletonHierarchy == null) {
                DebugType.Ragdoll.warn("No Skeleton found,");
                return null;
            } else {
                return this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
            }
        }
    }

    private Keyframe[] getKeyframesForBone(AnimationClip ragdollSimulationAnimationClip, int boneIndex) {
        this.keyframesForBone = ragdollSimulationAnimationClip.getKeyframesForBone(boneIndex, this.keyframesForBone);

        assert this.keyframesForBone.length == 2;

        assert this.keyframesForBone[1].none == boneIndex;

        return this.keyframesForBone;
    }

    private Keyframe getKeyframeForBone(AnimationClip ragdollSimulationAnimationClip, int boneIndex) {
        Keyframe[] keyframesForBone = this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
        return keyframesForBone[1];
    }

    private void setBoneKeyframePRS(AnimationClip ragdollSimulationAnimationClip, int boneIndex, Vector3f pos, Quaternion rot, Vector3f scale) {
        Keyframe[] keyframesForBone = this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
        Keyframe prevKeyframe = keyframesForBone[0];
        Keyframe keyframe = keyframesForBone[1];

        assert prevKeyframe.none == boneIndex && keyframe.none == boneIndex;

        prevKeyframe.set(keyframe.position, keyframe.rotation, keyframe.scale);
        keyframe.set(pos, rot, scale);
    }

    private AnimationClip getRagdollSimulationAnimationClip() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        return animationPlayer == null ? null : animationPlayer.getRagdollSimulationAnimationClip();
    }

    private SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        return animationPlayer == null ? null : animationPlayer.getSkeletonBoneHierarchy();
    }

    @Override
    public void onReleased() {
        this.reset();
    }

    public int getNumberOfBones() {
        return SkeletonBone.count();
    }

    public AnimationPlayer getAnimationPlayer() {
        IsoGameCharacter character = this.getGameCharacterObject();
        return character == null ? null : character.getAnimationPlayer();
    }

    public boolean isSimulationDirectionCalculated() {
        return this.ragdollStateData.isCalculated;
    }

    public Vector2 getCalculatedSimulationDirection(Vector2 out_result) {
        out_result.set(this.ragdollStateData.simulationDirection);
        return out_result;
    }

    public float getCalculatedSimulationDirectionAngle() {
        return this.ragdollStateData.simulationDirection.getDirection();
    }

    public float getSimulationRenderedAngle() {
        return this.ragdollStateData.simulationRenderedAngle;
    }

    public float getSimulationCharacterForwardAngle() {
        return this.ragdollStateData.simulationCharacterForwardAngle;
    }

    private void calculateSimulationData(RagdollStateData inout_ragdollStateData, float in_deltaT) {
        this.previousHeadPosition.set(this.headPosition);
        this.previousPelvisPosition.set(this.pelvisPosition);
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        Model.BoneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.headBoneIndex, this.headPosition);
        Model.BoneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.pelvisBoneIndex, this.pelvisPosition);
        if (!inout_ragdollStateData.isCalculated) {
            this.previousHeadPosition.set(this.headPosition);
            this.previousPelvisPosition.set(this.pelvisPosition);
        }

        inout_ragdollStateData.isSimulationMovement = false;
        float headDistance = this.previousHeadPosition.distanceTo(this.headPosition);
        float pelvisDistance = this.previousPelvisPosition.distanceTo(this.pelvisPosition);
        if (inout_ragdollStateData.isContactingVehicle) {
            inout_ragdollStateData.simulationTimeout = 1.5F;
            inout_ragdollStateData.isContactingVehicle = false;
        } else if (!(headDistance > 0.01F) && !(pelvisDistance > 0.01F)) {
            if (this.isSimulationSleeping()) {
                inout_ragdollStateData.simulationTimeout -= in_deltaT * 10.0F;
            }
        } else {
            if (inout_ragdollStateData.simulationTimeout < 1.5F) {
                inout_ragdollStateData.simulationTimeout = 1.5F;
            }

            inout_ragdollStateData.isSimulationMovement = true;
        }

        Vector3 groundPosition = new Vector3();
        Model.BoneToWorldCoords(gameCharacterObject, 0, groundPosition);
        this.isUpright = this.headPosition.z > groundPosition.z + 0.6F - 0.2F;
        gameCharacterObject.setOnFloor(!this.isUpright);
        Model.BoneZDirectionToWorldCoords(gameCharacterObject, RagdollBuilder.instance.pelvisBoneIndex, inout_ragdollStateData.pelvisDirection, 0.5F);
        this.isOnBack = this.pelvisPosition.z < inout_ragdollStateData.pelvisDirection.z;
        if (this.isOnBack) {
            inout_ragdollStateData.simulationDirection.x = this.pelvisPosition.x - this.headPosition.x;
            inout_ragdollStateData.simulationDirection.y = this.pelvisPosition.y - this.headPosition.y;
        } else {
            inout_ragdollStateData.simulationDirection.x = this.headPosition.x - this.pelvisPosition.x;
            inout_ragdollStateData.simulationDirection.y = this.headPosition.y - this.pelvisPosition.y;
        }

        inout_ragdollStateData.isCalculated = true;
        inout_ragdollStateData.simulationDirection.normalize();
        gameCharacterObject.setFallOnFront(!this.isOnBack);
        gameCharacterObject.setRagdollFall(true);
    }

    public static int getNumberOfActiveSimulations() {
        return numberOfActiveSimulations;
    }

    public static boolean checkForActiveRagdoll(IsoGridSquare isoGridSquare) {
        for (IPooledObject iPooledObject : ragdollControllerPool.getPoolStacks().get().getInUse()) {
            if (iPooledObject instanceof RagdollController ragdollController) {
                float distance = isoGridSquare.DistToProper(ragdollController.getGameCharacterObject());
                if (distance < 1.0F) {
                    return true;
                }
            }
        }

        return false;
    }

    public void vehicleCollision(IsoZombie isoZombie, BaseVehicle collidedVehicle) {
        boolean isCurrentlyContacting = false;
        if (collidedVehicle != null) {
            this.ragdollStateData.lastCollidedVehicle = collidedVehicle;
        }

        if (this.ragdollStateData.lastCollidedVehicle != null) {
            this.ragdollStateData.isContactingVehicle = this.ragdollStateData.lastCollidedVehicle.isCollided(this.gameCharacterObject);
            isCurrentlyContacting = this.ragdollStateData.lastCollidedVehicle.testTouchingVehicle(isoZombie, this);
        }

        if (this.wasContactingVehicle && !isCurrentlyContacting) {
            DebugLog.Physics.println("ResetVehicleBodyDynamics");
            this.resetVehicleRagdollBodyDynamics();
        } else if (!this.wasContactingVehicle && isCurrentlyContacting) {
            Bullet.setRagdollBodyDynamics(this.getID(), vehicleRagdollBodyDynamicsParams);
        }

        this.wasContactingVehicle = isCurrentlyContacting;
    }

    public static void setVehicleRagdollBodyDynamics(RagdollBodyDynamics ragdollBodyDynamics) {
        vehicleRagdollBodyDynamics.linearDamping = ragdollBodyDynamics.defaultLinearDamping;
        vehicleRagdollBodyDynamics.angularDamping = ragdollBodyDynamics.defaultAngularDamping;
        vehicleRagdollBodyDynamics.deactivationTime = ragdollBodyDynamics.defaultDeactivationTime;
        vehicleRagdollBodyDynamics.linearSleepingThreshold = ragdollBodyDynamics.defaultLinearSleepingThreshold;
        vehicleRagdollBodyDynamics.angularSleepingThreshold = ragdollBodyDynamics.defaultAngularSleepingThreshold;
        vehicleRagdollBodyDynamics.friction = vehicleCollisionFriction;
        vehicleRagdollBodyDynamics.rollingFriction = ragdollBodyDynamics.defaultRollingFriction;
        int arrayIndex = 0;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollBodyPart.BODYPART_COUNT.ordinal();
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.linearDamping;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.angularDamping;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.deactivationTime;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.linearSleepingThreshold;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.angularSleepingThreshold;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.friction;
        vehicleRagdollBodyDynamicsParams[arrayIndex++] = vehicleRagdollBodyDynamics.rollingFriction;
    }

    private void resetVehicleRagdollBodyDynamics() {
        Bullet.resetRagdollBodyDynamics(this.getID());
    }

    private static class Reusables_Quaternions {
        private final Quaternion xAxis = new Quaternion();
        private final Quaternion yAxis = new Quaternion();
        private final Quaternion zAxis = new Quaternion();
        private final Quaternion yawAdjust = new Quaternion();
    }

    public static enum SimulationState {
        UNKNOWN,
        ACTIVE_TAG,
        ISLAND_SLEEPING,
        WANTS_DEACTIVATION,
        DISABLE_DEACTIVATION,
        DISABLE_SIMULATION;
    }
}
