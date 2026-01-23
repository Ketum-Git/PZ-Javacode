// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.physics.Bullet;
import zombie.core.physics.RagdollController;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.scripting.objects.RagdollScript;

public final class RagdollBuilder {
    public static final RagdollBuilder instance = new RagdollBuilder();
    private static float[] boneTransformData = new float[245];
    private boolean initialized;
    private AnimationAsset tPoseAnimationAsset;
    public AnimationClip tPoseAnimationClip;
    public int pelvisBoneIndex;
    public int headBoneIndex;
    private float mass = 70.0F;
    private static int[] boneHierarchy = new int[245];
    private final Vector3f position = new Vector3f();
    private final Quaternion rotation = new Quaternion();

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean Initialize() {
        DebugType.Ragdoll.debugOnceln("RagdollController::Initialize [Start]");
        if (this.tPoseAnimationAsset == null) {
            this.tPoseAnimationAsset = ModelManager.instance.getAnimationAsset("bob/bob_tpose");
            if (this.tPoseAnimationAsset == null) {
                return false;
            }
        }

        if (this.tPoseAnimationClip == null) {
            this.tPoseAnimationClip = ModelManager.instance.getAnimationClip("Bob_TPose");
            if (this.tPoseAnimationClip == null) {
                return false;
            }
        }

        this.initialized = true;
        int numberOfBones = this.getNumBones();
        if (boneHierarchy.length > numberOfBones * 7) {
            boneHierarchy = new int[numberOfBones * 7];
        }

        if (boneTransformData.length > numberOfBones * 7) {
            boneTransformData = new float[numberOfBones * 7];
        }

        this.getSkinningDataSkeletonHierarchy();
        if (!GameServer.server) {
            Bullet.initializeRagdollSkeleton(numberOfBones, boneHierarchy);
            Bullet.setBallisticsTargetAdjustingShapeScale(0.35F, 0.4F, 0.35F);
        }

        this.initializeRagdollPose();
        this.pelvisBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_Pelvis").index;
        this.headBoneIndex = this.tPoseAnimationAsset.skinningData.getBone("Bip01_Head").index;
        RagdollController.setVehicleRagdollBodyDynamics(RagdollScript.getRagdollBodyDynamicsList().get(0));
        DebugType.Ragdoll.debugOnceln("RagdollController::Initialize [Success]");
        return this.initialized;
    }

    private void getSkinningDataSkeletonHierarchy() {
        SkeletonBone[] skeletonBones = SkeletonBone.all();
        SkinningBoneHierarchy boneHierarchy1 = this.getSkeletonBoneHierarchy();

        for (SkeletonBone skeletonBone : skeletonBones) {
            SkinningBone boneAt = boneHierarchy1.getBone(skeletonBone);
            if (boneAt == null) {
                DebugType.Ragdoll.debugln("Bone not found: %s", skeletonBone.toString());
                boneHierarchy[skeletonBone.index()] = -1;
            } else {
                SkeletonBone parentBone = boneAt.getParentSkeletonBone();
                boneHierarchy[skeletonBone.index()] = parentBone.index();
                DebugType.Ragdoll.debugln("parent: %s, descendingBone: %s", boneAt.parent, boneAt);
            }
        }
    }

    public void initializeRagdollPose() {
        this.tPoseAnimationClip = ModelManager.instance.getAnimationClip("Bob_TPose");
        if (!GameServer.server) {
            this.getBoneTransforms();
            Bullet.initializeRagdollPose(this.getNumBones(), boneTransformData, (float) (-Math.PI / 2), (float) Math.PI, (float) Math.PI);
        }
    }

    private void getBoneTransforms() {
        int floatArrayIndex = 0;
        SkeletonBone[] skeletonBones = SkeletonBone.all();
        SkinningBoneHierarchy boneHierarchy1 = this.getSkeletonBoneHierarchy();

        for (SkeletonBone skeletonBone : skeletonBones) {
            SkinningBone bone = boneHierarchy1.getBone(skeletonBone);
            if (bone == null) {
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 0.0F;
                boneTransformData[floatArrayIndex++] = 1.0F;
            } else {
                int boneIdx = bone.index;
                Keyframe[] keyframes = this.tPoseAnimationClip.getBoneFramesAt(boneIdx);
                Keyframe keyframe = keyframes[1];
                keyframe.get(this.position, this.rotation, null);
                boneTransformData[floatArrayIndex++] = -this.position.x * 1.5F;
                boneTransformData[floatArrayIndex++] = -this.position.y * 1.5F;
                boneTransformData[floatArrayIndex++] = -this.position.z * 1.5F;
                boneTransformData[floatArrayIndex++] = this.rotation.x;
                boneTransformData[floatArrayIndex++] = this.rotation.y;
                boneTransformData[floatArrayIndex++] = this.rotation.z;
                boneTransformData[floatArrayIndex++] = this.rotation.w;
            }
        }
    }

    public boolean isSkeletonBoneHierarchyInitialized() {
        return this.tPoseAnimationAsset != null;
    }

    public SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        return this.tPoseAnimationAsset.skinningData.getSkeletonBoneHierarchy();
    }

    private int getNumBones() {
        return SkeletonBone.count();
    }

    public float getMass() {
        return this.mass;
    }

    public void setMass(float mass) {
        if (this.mass != mass) {
            Bullet.setRagdollMass(mass);
        }

        this.mass = mass;
    }
}
