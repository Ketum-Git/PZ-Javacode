// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.debug.DebugOptions;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.vehicles.BaseVehicle;

public final class ModelSlotDebugRenderData extends PooledObject {
    private static final Pool<ModelSlotDebugRenderData> s_pool = new Pool<>(ModelSlotDebugRenderData::new);
    private ModelSlotRenderData slotData;
    private final TFloatArrayList boneCoords = new TFloatArrayList();
    private final ArrayList<Matrix4f> boneMatrices = new ArrayList<>();
    private final TFloatArrayList squareLights = new TFloatArrayList();
    private org.joml.Matrix4f weaponMatrix;
    private float weaponLength;

    public static ModelSlotDebugRenderData alloc() {
        return s_pool.alloc();
    }

    public void initModel(ModelSlotRenderData slotData) {
        for (int i = 0; i < slotData.modelData.size(); i++) {
            ModelInstanceRenderData instData = slotData.modelData.get(i);
            instData.debugRenderData = ModelInstanceDebugRenderData.alloc();
        }
    }

    public ModelSlotDebugRenderData init(ModelSlotRenderData slotData) {
        this.slotData = slotData;
        this.initBoneAxis();
        this.initSkeleton();
        this.initLights();
        this.initWeaponHitPoint();

        for (int i = 0; i < slotData.modelData.size(); i++) {
            ModelInstanceRenderData instData = slotData.modelData.get(i);
            instData.debugRenderData.init(slotData, instData);
        }

        return this;
    }

    private void initBoneAxis() {
        for (int i = 0; i < this.boneMatrices.size(); i++) {
            HelperFunctions.returnMatrix(this.boneMatrices.get(i));
        }

        this.boneMatrices.clear();
        if (this.slotData.animPlayer != null && this.slotData.animPlayer.hasSkinningData()) {
            if (DebugOptions.instance.character.debug.render.bip01.getValue()) {
                this.initBoneAxis("Bip01");
            }

            if (DebugOptions.instance.character.debug.render.primaryHandBone.getValue()) {
                this.initBoneAxis("Bip01_Prop1");
            }

            if (DebugOptions.instance.character.debug.render.secondaryHandBone.getValue()) {
                this.initBoneAxis("Bip01_Prop2");
            }

            if (DebugOptions.instance.character.debug.render.translationData.getValue()) {
                this.initBoneAxis("Translation_Data");
            }
        }
    }

    private void initBoneAxis(String boneName) {
        Integer boneIndex = this.slotData.animPlayer.getSkinningData().boneIndices.get(boneName);
        if (boneIndex != null) {
            Matrix4f boneMatrix = HelperFunctions.getMatrix();
            boneMatrix.load(this.slotData.animPlayer.getModelTransformAt(boneIndex));
            if (this.slotData.character instanceof IsoAnimal) {
                Vector3f v = HelperFunctions.allocVector3f();
                v.set(this.slotData.finalScale, this.slotData.finalScale, this.slotData.finalScale);
                boneMatrix.scale(v);
                HelperFunctions.releaseVector3f(v);
            }

            this.boneMatrices.add(boneMatrix);
        }
    }

    private void initSkeleton() {
        this.boneCoords.resetQuick();
        if (DebugOptions.instance.model.render.bones.getValue()) {
            this.initSkeleton(this.slotData.animPlayer);
            if (this.slotData.object instanceof BaseVehicle) {
                for (int i = 0; i < this.slotData.modelData.size(); i++) {
                    ModelInstanceRenderData instData = this.slotData.modelData.get(i);
                    if (instData.modelInstance instanceof VehicleSubModelInstance vsmi) {
                        this.initSkeleton(vsmi.animPlayer);
                    }
                }
            }
        }
    }

    private void initSkeleton(AnimationPlayer animPlayer) {
        if (animPlayer != null && animPlayer.hasSkinningData() && !animPlayer.isBoneTransformsNeedFirstFrame()) {
            Integer translationBoneIndex = animPlayer.getSkinningData().boneIndices.get("Translation_Data");

            for (int i = 0; i < animPlayer.getModelTransformsCount(); i++) {
                if (translationBoneIndex == null || i != translationBoneIndex) {
                    int parentIdx = animPlayer.getSkinningData().skeletonHierarchy.get(i);
                    if (parentIdx >= 0) {
                        this.initSkeleton(animPlayer, i);
                        this.initSkeleton(animPlayer, parentIdx);
                    }
                }
            }
        }
    }

    private void initSkeleton(AnimationPlayer animPlayer, int boneIndex) {
        Matrix4f boneTransform = animPlayer.getModelTransformAt(boneIndex);
        float x = boneTransform.m03;
        float y = boneTransform.m13;
        float z = boneTransform.m23;
        if (this.slotData.character instanceof IsoAnimal) {
            x *= this.slotData.finalScale;
            y *= this.slotData.finalScale;
            z *= this.slotData.finalScale;
        }

        this.boneCoords.add(x);
        this.boneCoords.add(y);
        this.boneCoords.add(z);
    }

    private void initLights() {
        this.squareLights.resetQuick();
        if (DebugOptions.instance.model.render.lights.getValue()) {
            if (this.slotData.character != null) {
                if (this.slotData.character.getCurrentSquare() != null) {
                    int playerIndex = IsoCamera.frameState.playerIndex;
                    IsoGridSquare.ILighting lighting = this.slotData.character.getCurrentSquare().lighting[playerIndex];

                    for (int i = 0; i < lighting.resultLightCount(); i++) {
                        IsoGridSquare.ResultLight light = lighting.getResultLight(i);
                        this.squareLights.add(light.x);
                        this.squareLights.add(light.y);
                        this.squareLights.add(light.z);
                    }
                }
            }
        }
    }

    private void initWeaponHitPoint() {
        if (this.weaponMatrix != null) {
            BaseVehicle.TL_matrix4f_pool.get().release(this.weaponMatrix);
            this.weaponMatrix = null;
        }

        if (DebugOptions.instance.model.render.weaponHitPoint.getValue()) {
            if (this.slotData.animPlayer != null && this.slotData.animPlayer.hasSkinningData()) {
                if (this.slotData.character != null) {
                    Integer boneIndex = this.slotData.animPlayer.getSkinningData().boneIndices.get("Bip01_Prop1");
                    if (boneIndex != null) {
                        if (this.slotData.character.getPrimaryHandItem() instanceof HandWeapon weapon) {
                            this.weaponLength = weapon.weaponLength;
                            Matrix4f var4 = this.slotData.animPlayer.getModelTransformAt(boneIndex);
                            this.weaponMatrix = BaseVehicle.TL_matrix4f_pool.get().alloc();
                            PZMath.convertMatrix(var4, this.weaponMatrix);
                            this.weaponMatrix.transpose();
                        }
                    }
                }
            }
        }
    }

    public void render() {
        DefaultShader.isActive = false;
        ShaderHelper.forgetCurrentlyBound();
        GL20.glUseProgram(0);
        this.renderBonesAxis();
        this.renderSkeleton();
        this.renderLights();
        this.renderWeaponHitPoint();
        ShaderHelper.glUseProgramObjectARB(0);
    }

    private void renderBonesAxis() {
        if (!this.boneMatrices.isEmpty()) {
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);

            for (int i = 0; i < this.boneMatrices.size(); i++) {
                Model.drawBoneMtx(this.boneMatrices.get(i));
            }

            vbor.endRun();
            vbor.flush();
        }
    }

    private void renderSkeleton() {
        if (!this.boneCoords.isEmpty()) {
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);
            vbor.setDepthTest(false);
            vbor.setLineWidth(1.0F);

            for (int i = 0; i < this.boneCoords.size(); i += 6) {
                Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                float x1 = this.boneCoords.get(i);
                float y1 = this.boneCoords.get(i + 1);
                float z1 = this.boneCoords.get(i + 2);
                float x2 = this.boneCoords.get(i + 3);
                float y2 = this.boneCoords.get(i + 4);
                float z2 = this.boneCoords.get(i + 5);
                vbor.addLine(x1, y1, z1, x2, y2, z2, c.r, c.g, c.b, 1.0F);
            }

            vbor.endRun();
            vbor.flush();
            GL11.glColor3f(1.0F, 1.0F, 1.0F);
            GL11.glEnable(2929);
        }
    }

    private void renderLights() {
        for (int i = 0; i < this.squareLights.size(); i += 3) {
            float x = this.squareLights.get(i);
            float y = this.squareLights.get(i + 1);
            float z = this.squareLights.get(i + 2);
            Model.debugDrawLightSource(x, y, z, this.slotData.x, this.slotData.y, this.slotData.z, -this.slotData.animPlayerAngle);
        }
    }

    private void renderWeaponHitPoint() {
        if (this.weaponMatrix != null) {
            PZGLUtil.pushAndMultMatrix(5888, this.weaponMatrix);
            Model.debugDrawAxis(0.0F, this.weaponLength, 0.0F, 0.05F, 1.0F);
            PZGLUtil.popMatrix(5888);
        }
    }
}
