// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoObject;
import zombie.iso.SpriteModel;
import zombie.iso.objects.IsoDoor;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class IsoObjectAnimations {
    private static IsoObjectAnimations instance;
    final ObjectPool<IsoObjectAnimations.AnimatedObject> animatedObjectPool = new ObjectPool<>(IsoObjectAnimations.AnimatedObject::new);
    final ArrayList<IsoObjectAnimations.AnimatedObject> animatedObjects = new ArrayList<>();
    final ArrayList<IsoObjectAnimations.MatrixPaletteForFrame> matrixPalettes = new ArrayList<>();
    final ArrayList<IsoObject> dancingDoors = new ArrayList<>();
    float dancingDoorsTimer;
    boolean dancingDoorsOpen;

    public static IsoObjectAnimations getInstance() {
        if (instance == null) {
            instance = new IsoObjectAnimations();
        }

        return instance;
    }

    public void addObject(IsoObject object, SpriteModel spriteModel, String animationName) {
        if (!GameServer.server) {
            IsoObjectAnimations.AnimatedObject animatedObject = this.getAnimatedObject(object);
            if (animatedObject == null) {
                animatedObject = this.animatedObjectPool.alloc();
                this.animatedObjects.add(animatedObject);
            }

            animatedObject.object = object;
            animatedObject.spriteModel = spriteModel;
            animatedObject.animationName = animationName;
            animatedObject.modelScript = ScriptManager.instance.getModelScript(spriteModel.getModelScriptName());
            animatedObject.initModel();
            animatedObject.initAnimationPlayer();
        }
    }

    public void update() {
        if (!GameServer.server) {
            this.updateDancingDoors();

            for (int i = 0; i < this.animatedObjects.size(); i++) {
                IsoObjectAnimations.AnimatedObject animatedObject = this.animatedObjects.get(i);
                if (animatedObject.object.getObjectIndex() == -1) {
                    this.animatedObjects.remove(i--);
                    this.animatedObjectPool.release(animatedObject);
                } else {
                    boolean bRemove = animatedObject.update();
                    if (bRemove) {
                        animatedObject.object.onAnimationFinished();
                        animatedObject.object.invalidateRenderChunkLevel(256L);
                        this.animatedObjects.remove(i--);
                        this.animatedObjectPool.release(animatedObject);
                    }
                }
            }
        }
    }

    private IsoObjectAnimations.AnimatedObject getAnimatedObject(IsoObject object) {
        for (int i = 0; i < this.animatedObjects.size(); i++) {
            IsoObjectAnimations.AnimatedObject animatedObject = this.animatedObjects.get(i);
            if (animatedObject.object == object) {
                return animatedObject;
            }
        }

        return null;
    }

    public AnimationPlayer getAnimationPlayer(IsoObject object) {
        IsoObjectAnimations.AnimatedObject animatedObject = this.getAnimatedObject(object);
        return animatedObject == null ? null : animatedObject.animationPlayer;
    }

    public Matrix4f getAttachmentTransform(IsoObject object, String attachmentName, Matrix4f xfrm) {
        xfrm.identity();
        IsoObjectAnimations.AnimatedObject animatedObject = this.getAnimatedObject(object);
        if (animatedObject == null) {
            SpriteModel spriteModel = object.getSpriteModel();
            ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.getModelScriptName());
            if (modelScript == null) {
                return xfrm;
            } else {
                ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
                if (attachment == null) {
                    return xfrm;
                } else {
                    ModelInstanceRenderData.makeAttachmentTransform(attachment, xfrm);
                    Model model = ModelManager.instance.getLoadedModel(modelScript.getMeshName());
                    if (model != null && model.isReady()) {
                        SkinningData skinningData = (SkinningData)model.tag;
                        int parentBone = skinningData.boneIndices.get(attachment.getBone());
                        if (parentBone == -1) {
                            return xfrm;
                        } else {
                            IsoObjectAnimations.MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(
                                model, spriteModel.animationName, spriteModel.animationTime
                            );
                            int position = mpff.matrixPalette.position();
                            mpff.matrixPalette.position(parentBone * 4 * 4);
                            Matrix4f boneXfrm = BaseVehicle.allocMatrix4f().set(mpff.matrixPalette);
                            mpff.matrixPalette.position(position);
                            boneXfrm.transpose();
                            boneXfrm.mul(xfrm, xfrm);
                            BaseVehicle.releaseMatrix4f(boneXfrm);
                            return xfrm;
                        }
                    } else {
                        return xfrm;
                    }
                }
            }
        } else {
            ModelAttachment attachment = animatedObject.modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return xfrm;
            } else {
                ModelInstanceRenderData.makeAttachmentTransform(attachment, xfrm);
                Matrix4f boneXfrm = BaseVehicle.allocMatrix4f();
                ModelInstanceRenderData.makeBoneTransform2(animatedObject.animationPlayer, attachment.getBone(), boneXfrm);
                boneXfrm.mul(xfrm, xfrm);
                BaseVehicle.releaseMatrix4f(boneXfrm);
                return xfrm;
            }
        }
    }

    private IsoObjectAnimations.MatrixPaletteForFrame getOrCreateMatrixPaletteForFrame(Model model, String animation, float time) {
        IsoObjectAnimations.MatrixPaletteForFrame mpff = null;

        for (int i = 0; i < this.matrixPalettes.size(); i++) {
            IsoObjectAnimations.MatrixPaletteForFrame mpff2 = this.matrixPalettes.get(i);
            if (mpff2.model == model && mpff2.animation.equalsIgnoreCase(animation) && mpff2.time == time) {
                mpff = mpff2;
                break;
            }
        }

        if (mpff == null) {
            mpff = new IsoObjectAnimations.MatrixPaletteForFrame();
            mpff.model = model;
            mpff.animation = animation;
            mpff.time = time;
            mpff.init();
            this.matrixPalettes.add(mpff);
        }

        if (mpff.matrixPalette == null || mpff.modificationCount != model.mesh.modificationCount) {
            mpff.init();
        }

        return mpff;
    }

    public FloatBuffer getMatrixPaletteForFrame(Model model, String animation, float time) {
        IsoObjectAnimations.MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(model, animation, time);
        return mpff.matrixPalette;
    }

    public TFloatArrayList getBonesForFrame(Model model, String animation, float time) {
        IsoObjectAnimations.MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(model, animation, time);
        return mpff.boneCoords;
    }

    public void addDancingDoor(IsoObject object) {
        if (!GameServer.server) {
            if (DebugOptions.instance.animation.dancingDoors.getValue()) {
                this.dancingDoors.add(object);
            }
        }
    }

    public void removeDancingDoor(IsoObject object) {
        if (!GameServer.server) {
            if (DebugOptions.instance.animation.dancingDoors.getValue()) {
                this.dancingDoors.remove(object);
            }
        }
    }

    private void updateDancingDoors() {
        if (!GameServer.server) {
            if (DebugOptions.instance.animation.dancingDoors.getValue()) {
                this.dancingDoorsTimer = this.dancingDoorsTimer + GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
                if (!(this.dancingDoorsTimer < 2.0F)) {
                    while (this.dancingDoorsTimer >= 2.0F) {
                        this.dancingDoorsTimer -= 2.0F;
                    }

                    this.dancingDoorsOpen = !this.dancingDoorsOpen;

                    for (int i = 0; i < this.dancingDoors.size(); i++) {
                        IsoObject object = this.dancingDoors.get(i);
                        if (object.getObjectIndex() == -1) {
                            this.dancingDoors.remove(i--);
                        } else {
                            IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
                            if (door != null && door.IsOpen() != this.dancingDoorsOpen) {
                                door.ToggleDoorActual(IsoPlayer.getInstance());
                            }
                        }
                    }
                }
            }
        }
    }

    static final class AnimatedObject {
        IsoObject object;
        SpriteModel spriteModel;
        String animationName;
        ModelScript modelScript;
        Model model;
        AnimationPlayer animationPlayer;
        AnimationTrack track;

        void initModel() {
            String meshName = this.modelScript.getMeshName();
            String texName = this.modelScript.getTextureName();
            String shaderName = this.modelScript.getShaderName();
            boolean bStatic = this.modelScript.isStatic;
            this.model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
            if (this.model == null && !bStatic && this.modelScript.animationsMesh != null) {
                AnimationsMesh animationsMesh = ScriptManager.instance.getAnimationsMesh(this.modelScript.animationsMesh);
                if (animationsMesh != null && animationsMesh.modelMesh != null) {
                    this.model = ModelManager.instance.loadModel(meshName, texName, animationsMesh.modelMesh, shaderName);
                }
            }

            if (this.model == null) {
                ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                this.model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
            }
        }

        void initAnimationPlayer() {
            if (this.track != null) {
                this.animationPlayer.getMultiTrack().removeTrack(this.track);
                this.track = null;
            }

            if (this.animationPlayer != null && this.animationPlayer.getModel() != this.model) {
                this.animationPlayer.release();
                this.animationPlayer = null;
            }

            if (this.animationPlayer == null) {
                this.animationPlayer = AnimationPlayer.alloc(this.model);
            }

            this.track = this.animationPlayer.play(this.animationName, false);
            if (this.track != null) {
                this.track.setBlendWeight(1.0F);
                this.track.setSpeedDelta(1.5F);
                this.track.isPlaying = true;
                this.track.reverse = false;
            }
        }

        boolean update() {
            if (this.animationPlayer == null) {
                return true;
            } else {
                this.animationPlayer.Update(!GameTime.isGamePaused() && GameWindow.isIngameState() ? GameTime.instance.getTimeDelta() : 0.0F);
                return this.track == null ? true : this.track.isFinished();
            }
        }
    }

    static final class MatrixPaletteForFrame {
        static AnimationPlayer animationPlayer;
        Model model;
        int modificationCount;
        String animation;
        float time;
        FloatBuffer matrixPalette;
        final TFloatArrayList boneCoords = new TFloatArrayList();

        void init() {
            if (animationPlayer != null) {
                while (animationPlayer.getMultiTrack().getTrackCount() > 0) {
                    animationPlayer.getMultiTrack().removeTrackAt(0);
                }
            }

            if (animationPlayer != null && animationPlayer.getModel() != this.model) {
                animationPlayer.release();
                animationPlayer = null;
            }

            if (animationPlayer == null) {
                animationPlayer = AnimationPlayer.alloc(this.model);
            }

            if (animationPlayer.isReady()) {
                this.modificationCount = this.model.mesh.modificationCount;
                AnimationTrack track = animationPlayer.play(this.animation, false);
                if (track != null) {
                    float duration = track.getDuration();
                    track.setCurrentTimeValue(this.time * duration);
                    track.setBlendWeight(1.0F);
                    track.setSpeedDelta(1.0F);
                    track.isPlaying = false;
                    track.reverse = false;
                    animationPlayer.Update(100.0F);
                    this.initMatrixPalette();
                    this.initSkeleton(animationPlayer);
                }
            }
        }

        private void initMatrixPalette() {
            SkinningData skinningData = (SkinningData)this.model.tag;
            if (skinningData == null) {
                DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
            } else {
                org.lwjgl.util.vector.Matrix4f[] skinTransforms = animationPlayer.getSkinTransforms(skinningData);
                int matrixFloats = 16;
                if (this.matrixPalette == null || this.matrixPalette.capacity() < skinTransforms.length * 16) {
                    this.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
                }

                this.matrixPalette.clear();

                for (int i = 0; i < skinTransforms.length; i++) {
                    skinTransforms[i].store(this.matrixPalette);
                }

                this.matrixPalette.flip();
            }
        }

        void initSkeleton(AnimationPlayer animPlayer) {
            this.boneCoords.resetQuick();
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

        void initSkeleton(AnimationPlayer animPlayer, int boneIndex) {
            org.lwjgl.util.vector.Matrix4f boneTransform = animPlayer.getModelTransformAt(boneIndex);
            float x = boneTransform.m03;
            float y = boneTransform.m13;
            float z = boneTransform.m23;
            this.boneCoords.add(x);
            this.boneCoords.add(y);
            this.boneCoords.add(z);
        }
    }
}
