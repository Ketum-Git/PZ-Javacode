// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelInstanceRenderData extends AnimatedModel.AnimatedModelInstanceRenderData {
    public static boolean invertAttachmentSelfTransform;
    private static final ObjectPool<ModelInstanceRenderData> pool = new ObjectPool<>(ModelInstanceRenderData::new);
    public float depthBias;
    public float hue;
    public float tintR;
    public float tintG;
    public float tintB;
    public int parentBone;
    public SoftwareModelMeshInstance softwareMesh;
    protected ModelInstanceDebugRenderData debugRenderData;

    public ModelInstanceRenderData init() {
        super.init();

        assert this.modelInstance.character == null || this.modelInstance.animPlayer != null;

        if (this.modelInstance.getTextureInitializer() != null) {
            this.modelInstance.getTextureInitializer().renderMain();
        }

        return this;
    }

    @Override
    public void initModel(ModelInstance modelInstance, AnimatedModel.AnimatedModelInstanceRenderData parent) {
        super.initModel(modelInstance, parent);
        this.model = modelInstance.model;
        this.tex = modelInstance.tex;
        this.depthBias = modelInstance.depthBias;
        this.hue = modelInstance.hue;
        this.parentBone = modelInstance.parentBone;
        this.softwareMesh = modelInstance.softwareMesh;
        modelInstance.renderRefCount++;
        VehicleSubModelInstance vehicleSubModelInstance = Type.tryCastTo(modelInstance, VehicleSubModelInstance.class);
        if (modelInstance instanceof VehicleModelInstance || vehicleSubModelInstance != null) {
            if (modelInstance instanceof VehicleModelInstance) {
                this.xfrm.set(((BaseVehicle)modelInstance.object).renderTransform);
            } else {
                this.xfrm.set(vehicleSubModelInstance.modelInfo.renderTransform);
            }

            postMultiplyMeshTransform(this.xfrm, modelInstance.model.mesh);
        }
    }

    @Override
    public void UpdateCharacter(Shader shader) {
        super.UpdateCharacter(shader);
        if (!PerformanceSettings.fboRenderChunk) {
            this.properties.SetFloat("targetDepth", 0.5F);
        } else if (this.modelInstance.parent != null) {
            this.properties.SetFloat("targetDepth", this.modelInstance.parent.targetDepth);
        }

        this.properties.SetFloat("DepthBias", this.depthBias / 50.0F);
        this.properties.SetFloat("HueShift", this.hue);
        this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
    }

    public void renderDebug() {
        if (this.debugRenderData != null) {
            this.debugRenderData.render();
        }
    }

    public void RenderCharacter(ModelSlotRenderData slotData) {
        this.tintR = this.modelInstance.tintR;
        this.tintG = this.modelInstance.tintG;
        this.tintB = this.modelInstance.tintB;
        this.tex = this.modelInstance.tex;
        if (this.tex != null || this.modelInstance.model.tex != null) {
            this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
            this.model.DrawChar(slotData, this);
        }
    }

    public void RenderVehicle(ModelSlotRenderData slotData) {
        this.tintR = this.modelInstance.tintR;
        this.tintG = this.modelInstance.tintG;
        this.tintB = this.modelInstance.tintB;
        this.tex = this.modelInstance.tex;
        if (this.tex != null || this.modelInstance.model.tex != null) {
            this.model.DrawVehicle(slotData, this);
        }
    }

    public static Matrix4f makeAttachmentTransform(ModelAttachment attachment, Matrix4f attachmentXfrm) {
        attachmentXfrm.translation(attachment.getOffset());
        Vector3f rotate = attachment.getRotate();
        attachmentXfrm.rotateXYZ(rotate.x * (float) (Math.PI / 180.0), rotate.y * (float) (Math.PI / 180.0), rotate.z * (float) (Math.PI / 180.0));
        attachmentXfrm.scale(attachment.getScale());
        return attachmentXfrm;
    }

    public static void applyBoneTransform(ModelInstance parentInstance, String boneName, Matrix4f transform) {
        if (parentInstance != null && parentInstance.animPlayer != null) {
            Matrix4f boneXfrm2 = BaseVehicle.TL_matrix4f_pool.get().alloc();
            makeBoneTransform(parentInstance.animPlayer, boneName, boneXfrm2);
            boneXfrm2.mul(transform, transform);
            BaseVehicle.TL_matrix4f_pool.get().release(boneXfrm2);
        }
    }

    public static void makeBoneTransform(AnimationPlayer animationPlayer, String boneName, Matrix4f transform) {
        transform.identity();
        if (animationPlayer != null) {
            if (!StringUtils.isNullOrWhitespace(boneName)) {
                int parentBone = animationPlayer.getSkinningBoneIndex(boneName, -1);
                if (parentBone != -1) {
                    org.lwjgl.util.vector.Matrix4f boneXfrm = animationPlayer.getModelTransformAt(parentBone);
                    PZMath.convertMatrix(boneXfrm, transform);
                    transform.transpose();
                }
            }
        }
    }

    public static void makeBoneTransform2(AnimationPlayer animationPlayer, String boneName, Matrix4f transform) {
        transform.identity();
        if (animationPlayer != null) {
            if (!StringUtils.isNullOrWhitespace(boneName)) {
                int parentBone = animationPlayer.getSkinningBoneIndex(boneName, -1);
                if (parentBone != -1) {
                    org.lwjgl.util.vector.Matrix4f[] boneXfrms = animationPlayer.getSkinTransforms(animationPlayer.getSkinningData());
                    org.lwjgl.util.vector.Matrix4f boneXfrm = boneXfrms[parentBone];
                    PZMath.convertMatrix(boneXfrm, transform);
                    transform.transpose();
                }
            }
        }
    }

    public static Matrix4f preMultiplyMeshTransform(Matrix4f transform, ModelMesh mesh) {
        if (mesh != null && mesh.isReady() && mesh.transform != null) {
            Matrix4f meshTransform = BaseVehicle.allocMatrix4f().set(mesh.transform);
            meshTransform.transpose();
            meshTransform.mul(transform, transform);
            BaseVehicle.releaseMatrix4f(meshTransform);
        }

        return transform;
    }

    public static Matrix4f postMultiplyMeshTransform(Matrix4f transform, ModelMesh mesh) {
        if (mesh != null && mesh.transform != null) {
            if (mesh.isReady()) {
                Matrix4f meshTransform = BaseVehicle.allocMatrix4f().set(mesh.transform);
                meshTransform.transpose();
                transform.mul(meshTransform);
                BaseVehicle.releaseMatrix4f(meshTransform);
            } else {
                transform.scale(0.0F);
            }
        }

        return transform;
    }

    private void testOnBackItem(ModelInstance modelInstance) {
        if (modelInstance.parent != null && modelInstance.parent.modelScript != null) {
            AnimationPlayer animPlayer = modelInstance.parent.animPlayer;
            ModelAttachment attachment = null;

            for (int i = 0; i < modelInstance.parent.modelScript.getAttachmentCount(); i++) {
                ModelAttachment attachment2 = modelInstance.parent.getAttachment(i);
                if (attachment2.getBone() != null && this.parentBone == animPlayer.getSkinningBoneIndex(attachment2.getBone(), 0)) {
                    attachment = attachment2;
                    break;
                }
            }

            if (attachment != null) {
                Matrix4f attachmentXfrm = BaseVehicle.TL_matrix4f_pool.get().alloc();
                makeAttachmentTransform(attachment, attachmentXfrm);
                this.xfrm.transpose();
                this.xfrm.mul(attachmentXfrm);
                this.xfrm.transpose();
                ModelAttachment attachment1 = modelInstance.getAttachmentById(attachment.getId());
                if (attachment1 != null) {
                    makeAttachmentTransform(attachment1, attachmentXfrm);
                    if (invertAttachmentSelfTransform) {
                        attachmentXfrm.invert();
                    }

                    this.xfrm.transpose();
                    this.xfrm.mul(attachmentXfrm);
                    this.xfrm.transpose();
                }

                BaseVehicle.TL_matrix4f_pool.get().release(attachmentXfrm);
            }
        }
    }

    public static ModelInstanceRenderData alloc() {
        return pool.alloc();
    }

    public static synchronized void release(ArrayList<ModelInstanceRenderData> objs) {
        for (int i = 0; i < objs.size(); i++) {
            ModelInstanceRenderData data = objs.get(i);
            release(data);
        }
    }

    public static synchronized boolean release(ModelInstanceRenderData data) {
        if (data.modelInstance.getTextureInitializer() != null) {
            data.modelInstance.getTextureInitializer().postRender();
        }

        boolean bInstanceReleased = ModelManager.instance.derefModelInstance(data.modelInstance);
        data.modelInstance = null;
        data.model = null;
        data.tex = null;
        data.softwareMesh = null;
        data.debugRenderData = Pool.tryRelease(data.debugRenderData);
        pool.release(data);
        return bInstanceReleased;
    }
}
