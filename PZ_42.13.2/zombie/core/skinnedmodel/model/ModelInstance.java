// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.IsoRoom;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.ReferencedObject;
import zombie.util.StringUtils;

/**
 * Created by LEMMYPC on 05/01/14.
 */
public class ModelInstance extends ReferencedObject {
    public static final float MODEL_LIGHT_MULT_OUTSIDE = 1.7F;
    public static final float MODEL_LIGHT_MULT_ROOM = 1.7F;
    public Model model;
    public AnimationPlayer animPlayer;
    public SkinningData data;
    public Texture tex;
    public ModelInstanceTextureInitializer textureInitializer;
    public IsoGameCharacter character;
    public IsoMovingObject object;
    public float tintR = 1.0F;
    public float tintG = 1.0F;
    public float tintB = 1.0F;
    public ModelInstance parent;
    public int parentBone;
    public String parentBoneName;
    public float hue;
    public float depthBias;
    public ModelInstance matrixModel;
    public SoftwareModelMeshInstance softwareMesh;
    public final ArrayList<ModelInstance> sub = new ArrayList<>();
    public float targetDepth;
    private int instanceSkip;
    private ItemVisual itemVisual;
    public boolean resetAfterRender;
    private Object owner;
    public int renderRefCount;
    private static final int INITIAL_SKIP_VALUE = Integer.MAX_VALUE;
    private int skipped = Integer.MAX_VALUE;
    public final Object lock = "ModelInstance Thread Lock";
    public ModelScript modelScript;
    public String attachmentNameSelf;
    public String attachmentNameParent;
    public float scale = 1.0F;
    public String maskVariableValue;
    private static final Matrix4f gcAttachmentMatrix = new Matrix4f();
    private static final Matrix4f gcTransposedAttachmentMatrix = new Matrix4f();
    private static final org.lwjgl.util.vector.Matrix4f gcBoneModelTransform = new org.lwjgl.util.vector.Matrix4f();
    private static final Vector3f gcVector3f = new Vector3f();
    private static final AxisAngle4f gcAxisAngle4f = new AxisAngle4f();
    private static final Vector3 modelSpaceForward = new Vector3(0.0F, 0.0F, 1.0F);
    private static final Vector3 gcRotatedForward = new Vector3();
    public ModelInstance.PlayerData[] playerData;
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final ColorInfo tempColorInfo2 = new ColorInfo();

    public ModelInstance init(Model model, IsoGameCharacter character, AnimationPlayer player) {
        this.data = (SkinningData)model.tag;
        this.model = model;
        this.tex = model.tex;
        if (!model.isStatic && player == null) {
            player = AnimationPlayer.alloc(model);
        }

        this.animPlayer = player;
        this.character = character;
        this.object = character;
        return this;
    }

    public boolean isRendering() {
        return this.renderRefCount > 0;
    }

    public void reset() {
        if (this.tex instanceof SmartTexture) {
            Texture tex = this.tex;
            RenderThread.queueInvokeOnRenderContext(tex::destroy);
        }

        this.animPlayer = null;
        this.character = null;
        this.data = null;
        this.hue = 0.0F;
        this.itemVisual = null;
        this.matrixModel = null;
        this.model = null;
        this.object = null;
        this.parent = null;
        this.parentBone = 0;
        this.parentBoneName = null;
        this.skipped = Integer.MAX_VALUE;
        this.sub.clear();
        this.softwareMesh = null;
        this.tex = null;
        if (this.textureInitializer != null) {
            this.textureInitializer.release();
            this.textureInitializer = null;
        }

        this.tintR = 1.0F;
        this.tintG = 1.0F;
        this.tintB = 1.0F;
        this.resetAfterRender = false;
        this.renderRefCount = 0;
        this.scale = 1.0F;
        this.owner = null;
        this.modelScript = null;
        this.attachmentNameSelf = null;
        this.attachmentNameParent = null;
        this.maskVariableValue = null;
        if (this.playerData != null) {
            ModelInstance.PlayerData.pool.release(this.playerData);
            Arrays.fill(this.playerData, null);
        }
    }

    public void LoadTexture(String name) {
        if (name != null && !name.isEmpty()) {
            this.tex = Texture.getSharedTexture("media/textures/" + name + ".png");
            if (this.tex == null) {
                if (name.equals("Vest_White")) {
                    this.tex = Texture.getSharedTexture("media/textures/Shirt_White.png");
                } else if (name.contains("Hair")) {
                    this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
                } else if (name.contains("Beard")) {
                    this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
                } else {
                    DebugLog.log("ERROR: model texture \"" + name + "\" wasn't found");
                }
            }
        } else {
            this.tex = null;
        }
    }

    public void dismember(int bone) {
        this.animPlayer.dismember(bone);
    }

    public void UpdateDir() {
        if (this.animPlayer != null) {
            this.animPlayer.updateForwardDirection(this.character);
        }
    }

    public void Update(float in_deltaT) {
        if (this.character != null) {
            float distToPlayer = this.character.DistTo(IsoPlayer.getInstance());
            if (!this.character.amputations.isEmpty() && distToPlayer > 0.0F && this.animPlayer != null) {
                this.animPlayer.dismembered.clear();
                ArrayList<String> amputations = this.character.amputations;

                for (int i = 0; i < amputations.size(); i++) {
                    String amputation = amputations.get(i);
                    this.animPlayer.dismember(this.animPlayer.getSkinningData().boneIndices.get(amputation));
                }
            }
        }

        this.instanceSkip = 0;
        if (this.animPlayer != null) {
            if (this.matrixModel == null) {
                if (this.skipped >= this.instanceSkip) {
                    if (this.skipped == Integer.MAX_VALUE) {
                        this.skipped = 1;
                    }

                    float time = in_deltaT * this.skipped;
                    this.animPlayer.Update(time);
                } else {
                    this.animPlayer.DoAngles(in_deltaT);
                }

                this.animPlayer.parentPlayer = null;
            } else {
                this.animPlayer.parentPlayer = this.matrixModel.animPlayer;
            }
        }

        if (this.skipped >= this.instanceSkip) {
            this.skipped = 0;
        }

        this.skipped++;
    }

    public void SetForceDir(Vector2 dir) {
        if (this.animPlayer != null) {
            this.animPlayer.setTargetAndCurrentDirection(dir);
        }
    }

    public void setInstanceSkip(int c) {
        this.instanceSkip = c;

        for (int i = 0; i < this.sub.size(); i++) {
            ModelInstance modelInstance = this.sub.get(i);
            modelInstance.instanceSkip = c;
        }
    }

    public void destroySmartTextures() {
        if (this.tex instanceof SmartTexture) {
            this.tex.destroy();
            this.tex = null;
        }

        for (int i = 0; i < this.sub.size(); i++) {
            ModelInstance inst = this.sub.get(i);
            inst.destroySmartTextures();
        }
    }

    public void updateLights() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.playerData == null) {
            this.playerData = new ModelInstance.PlayerData[4];
        }

        boolean bNeverDone = this.playerData[playerIndex] == null;
        if (this.playerData[playerIndex] == null) {
            this.playerData[playerIndex] = ModelInstance.PlayerData.pool.alloc();
        }

        this.playerData[playerIndex].updateLights(this.character, bNeverDone);
    }

    public ItemVisual getItemVisual() {
        return this.itemVisual;
    }

    public void setItemVisual(ItemVisual itemVisual) {
        this.itemVisual = itemVisual;
    }

    public void applyModelScriptScale(String modelName) {
        this.modelScript = ScriptManager.instance.getModelScript(modelName);
        if (this.modelScript != null) {
            this.scale = this.modelScript.scale;
        }
    }

    public ModelAttachment getAttachment(int index) {
        return this.modelScript == null ? null : this.modelScript.getAttachment(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            return null;
        } else {
            return this.modelScript == null ? null : this.modelScript.getAttachmentById(id);
        }
    }

    public Matrix4f getAttachmentMatrix(ModelAttachment attachment, Matrix4f out) {
        out.translation(attachment.getOffset());
        Vector3f rot = attachment.getRotate();
        out.rotateXYZ(rot.x * (float) (Math.PI / 180.0), rot.y * (float) (Math.PI / 180.0), rot.z * (float) (Math.PI / 180.0));
        return out;
    }

    public Matrix4f getAttachmentMatrix(int index, Matrix4f out) {
        ModelAttachment attachment = this.getAttachment(index);
        return attachment == null ? out.identity() : this.getAttachmentMatrix(attachment, out);
    }

    public Matrix4f getAttachmentMatrixById(String id, Matrix4f out) {
        ModelAttachment attachment = this.getAttachmentById(id);
        return attachment == null ? out.identity() : this.getAttachmentMatrix(attachment, out);
    }

    public void setOwner(Object owner) {
        Objects.requireNonNull(owner);

        assert this.owner == null;

        this.owner = owner;
    }

    public void clearOwner(Object expectedOwner) {
        Objects.requireNonNull(expectedOwner);

        assert this.owner == expectedOwner;

        this.owner = null;
    }

    public Object getOwner() {
        return this.owner;
    }

    public void setTextureInitializer(ModelInstanceTextureInitializer textureInitializer) {
        this.textureInitializer = textureInitializer;
    }

    public ModelInstanceTextureInitializer getTextureInitializer() {
        return this.textureInitializer;
    }

    public boolean hasTextureCreator() {
        return this.textureInitializer != null && this.textureInitializer.isDirty();
    }

    public void getAttachmentWorldPosition(ModelAttachment in_attachment, Vector3 out_worldPosition, Vector3 out_worldDirectionUnnormalized) {
        float angle = this.animPlayer.getRenderedAngle();
        this.getAttachmentWorldPosition(in_attachment, angle, out_worldPosition, out_worldDirectionUnnormalized);
    }

    public void getAttachmentWorldPosition(ModelAttachment in_attachment, float in_yawAngle, Vector3 out_worldPosition, Vector3 out_worldDirectionUnnormalized) {
        this.animPlayer.getBoneModelTransform(this.parentBone, gcBoneModelTransform);
        this.getAttachmentMatrix(in_attachment, gcAttachmentMatrix);
        PZMath.convertMatrix(gcBoneModelTransform, gcTransposedAttachmentMatrix);
        gcTransposedAttachmentMatrix.transpose();
        gcTransposedAttachmentMatrix.mul(gcAttachmentMatrix, gcAttachmentMatrix);
        gcAttachmentMatrix.getTranslation(gcVector3f);
        out_worldPosition.x = gcVector3f.x;
        out_worldPosition.y = gcVector3f.y;
        out_worldPosition.z = gcVector3f.z;
        Model.VectorToWorldCoords(this.character.getX(), this.character.getY(), this.character.getZ(), in_yawAngle, out_worldPosition);
        gcAttachmentMatrix.getRotation(gcAxisAngle4f);
        this.rotateVectorByAxisAngle();
        out_worldDirectionUnnormalized.x = gcRotatedForward.x;
        out_worldDirectionUnnormalized.y = gcRotatedForward.y;
        out_worldDirectionUnnormalized.z = gcRotatedForward.z;
        Model.VectorToWorldCoords(0.0F, 0.0F, 0.0F, in_yawAngle, out_worldDirectionUnnormalized);
    }

    private void rotateVectorByAxisAngle() {
        float ux = gcAxisAngle4f.x;
        float uy = gcAxisAngle4f.y;
        float uz = gcAxisAngle4f.z;
        float angle = gcAxisAngle4f.angle;
        float cosTheta = org.joml.Math.cos(angle);
        float sinTheta = org.joml.Math.sin(angle);
        float x = modelSpaceForward.x * (cosTheta + ux * ux * (1.0F - cosTheta))
            + modelSpaceForward.y * (ux * uy * (1.0F - cosTheta) - uz * sinTheta)
            + modelSpaceForward.z * (ux * uz * (1.0F - cosTheta) + uy * sinTheta);
        float y = modelSpaceForward.x * (uy * ux * (1.0F - cosTheta) + uz * sinTheta)
            + modelSpaceForward.y * (cosTheta + uy * uy * (1.0F - cosTheta))
            + modelSpaceForward.z * (uy * uz * (1.0F - cosTheta) - ux * sinTheta);
        float z = modelSpaceForward.x * (uz * ux * (1.0F - cosTheta) - uy * sinTheta)
            + modelSpaceForward.y * (uz * uy * (1.0F - cosTheta) + ux * sinTheta)
            + modelSpaceForward.z * (cosTheta + uz * uz * (1.0F - cosTheta));
        gcRotatedForward.set(x, y, z);
    }

    public static final class EffectLight {
        public float x;
        public float y;
        public float z;
        public float r;
        public float g;
        public float b;
        public int radius;

        public void set(float x, float y, float z, float r, float g, float b, int radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.radius = radius;
        }

        public void clear() {
            this.set(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0);
        }

        public void set(IsoLightSource ls) {
            this.x = ls.x + 0.5F;
            this.y = ls.y + 0.5F;
            this.z = ls.z;
            this.r = ls.r;
            this.g = ls.g;
            this.b = ls.b;
            this.radius = ls.radius;
        }
    }

    public static enum FrameLightBlendStatus {
        In,
        During,
        Out;
    }

    public static final class FrameLightInfo {
        public ModelInstance.FrameLightBlendStatus stage;
        public int id;
        public int x;
        public int y;
        public int z;
        public float distSq;
        public int radius;
        public float r;
        public float g;
        public float b;
        public int flags;
        public final org.lwjgl.util.vector.Vector3f currentColor = new org.lwjgl.util.vector.Vector3f();
        public final org.lwjgl.util.vector.Vector3f targetColor = new org.lwjgl.util.vector.Vector3f();
        public boolean active;
        public boolean foundThisFrame;
    }

    public static final class PlayerData {
        private ModelInstance.FrameLightInfo[] frameLights;
        private ArrayList<IsoGridSquare.ResultLight> chosenLights;
        private Vector3f targetAmbient;
        public Vector3f currentAmbient;
        public ModelInstance.EffectLight[] effectLightsMain;
        private static final ObjectPool<ModelInstance.PlayerData> pool = new ObjectPool<>(ModelInstance.PlayerData::new);

        private void registerFrameLight(IsoGridSquare.ResultLight light) {
            this.chosenLights.add(light);
        }

        private void initFrameLightsForFrame() {
            if (this.frameLights == null) {
                this.effectLightsMain = new ModelInstance.EffectLight[5];

                for (int i = 0; i < 5; i++) {
                    this.effectLightsMain[i] = new ModelInstance.EffectLight();
                }

                this.frameLights = new ModelInstance.FrameLightInfo[5];
                this.chosenLights = new ArrayList<>();
                this.targetAmbient = new Vector3f();
                this.currentAmbient = new Vector3f();
            }

            for (ModelInstance.EffectLight effectLight : this.effectLightsMain) {
                effectLight.radius = -1;
            }

            this.chosenLights.clear();
        }

        private void completeFrameLightsForFrame() {
            for (int x = 0; x < 5; x++) {
                if (this.frameLights[x] != null) {
                    this.frameLights[x].foundThisFrame = false;
                }
            }

            for (int i = 0; i < this.chosenLights.size(); i++) {
                IsoGridSquare.ResultLight chosenLight = this.chosenLights.get(i);
                boolean found = false;
                int foundIndex = 0;

                for (int xx = 0; xx < 5; xx++) {
                    if (this.frameLights[xx] != null
                        && this.frameLights[xx].active
                        && (
                            chosenLight.id != -1
                                ? chosenLight.id == this.frameLights[xx].id
                                : this.frameLights[xx].x == chosenLight.x && this.frameLights[xx].y == chosenLight.y && this.frameLights[xx].z == chosenLight.z
                        )) {
                        found = true;
                        foundIndex = xx;
                        break;
                    }
                }

                if (found) {
                    this.frameLights[foundIndex].foundThisFrame = true;
                    this.frameLights[foundIndex].x = chosenLight.x;
                    this.frameLights[foundIndex].y = chosenLight.y;
                    this.frameLights[foundIndex].z = chosenLight.z;
                    this.frameLights[foundIndex].flags = chosenLight.flags;
                    this.frameLights[foundIndex].radius = chosenLight.radius;
                    this.frameLights[foundIndex].targetColor.x = chosenLight.r;
                    this.frameLights[foundIndex].targetColor.y = chosenLight.g;
                    this.frameLights[foundIndex].targetColor.z = chosenLight.b;
                    this.frameLights[foundIndex].stage = ModelInstance.FrameLightBlendStatus.In;
                } else {
                    for (int xxx = 0; xxx < 5; xxx++) {
                        if (this.frameLights[xxx] == null || !this.frameLights[xxx].active) {
                            if (this.frameLights[xxx] == null) {
                                this.frameLights[xxx] = new ModelInstance.FrameLightInfo();
                            }

                            this.frameLights[xxx].x = chosenLight.x;
                            this.frameLights[xxx].y = chosenLight.y;
                            this.frameLights[xxx].z = chosenLight.z;
                            this.frameLights[xxx].r = chosenLight.r;
                            this.frameLights[xxx].g = chosenLight.g;
                            this.frameLights[xxx].b = chosenLight.b;
                            this.frameLights[xxx].flags = chosenLight.flags;
                            this.frameLights[xxx].radius = chosenLight.radius;
                            this.frameLights[xxx].id = chosenLight.id;
                            this.frameLights[xxx].currentColor.x = 0.0F;
                            this.frameLights[xxx].currentColor.y = 0.0F;
                            this.frameLights[xxx].currentColor.z = 0.0F;
                            this.frameLights[xxx].targetColor.x = chosenLight.r;
                            this.frameLights[xxx].targetColor.y = chosenLight.g;
                            this.frameLights[xxx].targetColor.z = chosenLight.b;
                            this.frameLights[xxx].stage = ModelInstance.FrameLightBlendStatus.In;
                            this.frameLights[xxx].active = true;
                            this.frameLights[xxx].foundThisFrame = true;
                            break;
                        }
                    }
                }
            }

            float multiplier = GameTime.getInstance().getMultiplier();

            for (int xxxx = 0; xxxx < 5; xxxx++) {
                ModelInstance.FrameLightInfo frameLight = this.frameLights[xxxx];
                if (frameLight != null && frameLight.active) {
                    if (!frameLight.foundThisFrame) {
                        frameLight.targetColor.x = 0.0F;
                        frameLight.targetColor.y = 0.0F;
                        frameLight.targetColor.z = 0.0F;
                        frameLight.stage = ModelInstance.FrameLightBlendStatus.Out;
                    }

                    frameLight.currentColor.x = this.step(
                        frameLight.currentColor.x,
                        frameLight.targetColor.x,
                        Math.signum(frameLight.targetColor.x - frameLight.currentColor.x) / (60.0F * multiplier)
                    );
                    frameLight.currentColor.y = this.step(
                        frameLight.currentColor.y,
                        frameLight.targetColor.y,
                        Math.signum(frameLight.targetColor.y - frameLight.currentColor.y) / (60.0F * multiplier)
                    );
                    frameLight.currentColor.z = this.step(
                        frameLight.currentColor.z,
                        frameLight.targetColor.z,
                        Math.signum(frameLight.targetColor.z - frameLight.currentColor.z) / (60.0F * multiplier)
                    );
                    if (frameLight.stage == ModelInstance.FrameLightBlendStatus.Out
                        && frameLight.currentColor.x < 0.01F
                        && frameLight.currentColor.y < 0.01F
                        && frameLight.currentColor.z < 0.01F) {
                        frameLight.active = false;
                    }
                }
            }
        }

        private void sortLights(IsoGameCharacter character) {
            for (int i = 0; i < this.frameLights.length; i++) {
                ModelInstance.FrameLightInfo fl = this.frameLights[i];
                if (fl != null) {
                    if (!fl.active) {
                        fl.distSq = Float.MAX_VALUE;
                    } else {
                        fl.distSq = IsoUtils.DistanceToSquared(character.getX(), character.getY(), character.getZ(), fl.x + 0.5F, fl.y + 0.5F, fl.z);
                    }
                }
            }

            Arrays.sort(this.frameLights, (o1, o2) -> {
                boolean null1 = o1 == null || o1.radius == -1 || !o1.active;
                boolean null2 = o2 == null || o2.radius == -1 || !o2.active;
                if (null1 && null2) {
                    return 0;
                } else if (null1) {
                    return 1;
                } else if (null2) {
                    return -1;
                } else if (o1.stage.ordinal() < o2.stage.ordinal()) {
                    return -1;
                } else {
                    return o1.stage.ordinal() > o2.stage.ordinal() ? 1 : (int)Math.signum(o1.distSq - o2.distSq);
                }
            });
        }

        private void updateLights(IsoGameCharacter character, boolean bNeverDone) {
            this.initFrameLightsForFrame();
            if (character != null) {
                if (character.getCurrentSquare() != null) {
                    IsoGridSquare.ILighting lighting = character.getCurrentSquare().lighting[IsoCamera.frameState.playerIndex];
                    int lightCount = org.joml.Math.min(lighting.resultLightCount(), 4);

                    for (int i = 0; i < lightCount; i++) {
                        IsoGridSquare.ResultLight light = lighting.getResultLight(i);
                        this.registerFrameLight(light);
                    }

                    if (bNeverDone) {
                        for (int i = 0; i < this.frameLights.length; i++) {
                            if (this.frameLights[i] != null) {
                                this.frameLights[i].active = false;
                            }
                        }
                    }

                    this.completeFrameLightsForFrame();
                    character.getCurrentSquare().interpolateLight(ModelInstance.tempColorInfo, character.getX() % 1.0F, character.getY() % 1.0F);
                    this.targetAmbient.x = ModelInstance.tempColorInfo.r;
                    this.targetAmbient.y = ModelInstance.tempColorInfo.g;
                    this.targetAmbient.z = ModelInstance.tempColorInfo.b;
                    if (character.getZ() - PZMath.fastfloor(character.getZ()) > 0.2F) {
                        IsoGridSquare above = IsoWorld.instance
                            .currentCell
                            .getGridSquare(PZMath.fastfloor(character.getX()), PZMath.fastfloor(character.getY()), PZMath.fastfloor(character.getZ()) + 1);
                        if (above != null) {
                            ColorInfo colorAbove = ModelInstance.tempColorInfo2;
                            above.lighting[IsoCamera.frameState.playerIndex].lightInfo();
                            above.interpolateLight(colorAbove, character.getX() % 1.0F, character.getY() % 1.0F);
                            ModelInstance.tempColorInfo
                                .interp(colorAbove, (character.getZ() - (PZMath.fastfloor(character.getZ()) + 0.2F)) / 0.8F, ModelInstance.tempColorInfo);
                            this.targetAmbient.set(ModelInstance.tempColorInfo.r, ModelInstance.tempColorInfo.g, ModelInstance.tempColorInfo.b);
                        }
                    }

                    float multiplier = GameTime.getInstance().getMultiplier();
                    this.currentAmbient.x = this.step(
                        this.currentAmbient.x, this.targetAmbient.x, (this.targetAmbient.x - this.currentAmbient.x) / (10.0F * multiplier)
                    );
                    this.currentAmbient.y = this.step(
                        this.currentAmbient.y, this.targetAmbient.y, (this.targetAmbient.y - this.currentAmbient.y) / (10.0F * multiplier)
                    );
                    this.currentAmbient.z = this.step(
                        this.currentAmbient.z, this.targetAmbient.z, (this.targetAmbient.z - this.currentAmbient.z) / (10.0F * multiplier)
                    );
                    if (bNeverDone) {
                        this.setCurrentToTarget();
                    }

                    this.sortLights(character);
                    float lightMul = 0.7F;

                    for (int ix = 0; ix < 5; ix++) {
                        ModelInstance.FrameLightInfo light = this.frameLights[ix];
                        if (light != null && light.active) {
                            ModelInstance.EffectLight effectLight = this.effectLightsMain[ix];
                            int light_flags = light.flags;
                            if ((light_flags & 1) != 0) {
                                IsoRoom room = character.getCurrentSquare().getRoom();
                                if (room == null || room.findRoomLightByID(light.id) == null) {
                                    light_flags &= -2;
                                }
                            }

                            if ((light_flags & 1) != 0) {
                                effectLight.set(
                                    character.getX(),
                                    character.getY(),
                                    PZMath.fastfloor(character.getZ()) + 1,
                                    light.currentColor.x * 0.7F,
                                    light.currentColor.y * 0.7F,
                                    light.currentColor.z * 0.7F,
                                    light.radius
                                );
                            } else if ((light.flags & 2) != 0) {
                                if (character instanceof IsoPlayer isoPlayer) {
                                    int MAX_LIGHTS_PER_PLAYER = 4;
                                    if (GameClient.client) {
                                        int var10000 = isoPlayer.onlineId + 1;
                                    } else {
                                        int var34 = isoPlayer.playerIndex + 1;
                                    }

                                    int playerIndex = isoPlayer.playerIndex;
                                    int minID = playerIndex * 4 + 1;
                                    int maxID = playerIndex * 4 + 4 - 1 + 1;
                                    if (light.id < minID || light.id > maxID) {
                                        effectLight.set(
                                            light.x, light.y, light.z, light.currentColor.x, light.currentColor.y, light.currentColor.z, light.radius
                                        );
                                    }
                                } else {
                                    effectLight.set(
                                        light.x, light.y, light.z, light.currentColor.x * 2.0F, light.currentColor.y, light.currentColor.z, light.radius
                                    );
                                }
                            } else {
                                effectLight.set(
                                    light.x + 0.5F,
                                    light.y + 0.5F,
                                    light.z + 0.5F,
                                    light.currentColor.x * 0.7F,
                                    light.currentColor.y * 0.7F,
                                    light.currentColor.z * 0.7F,
                                    light.radius
                                );
                            }
                        }
                    }

                    if (lightCount <= 3 && character instanceof IsoPlayer && character.getTorchStrength() > 0.0F) {
                        this.effectLightsMain[2]
                            .set(
                                character.getX() + character.getForwardDirectionX() * 0.5F,
                                character.getY() + character.getForwardDirectionY() * 0.5F,
                                character.getZ() + 0.25F,
                                1.0F,
                                1.0F,
                                1.0F,
                                2
                            );
                    }

                    float MODEL_LIGHT_FROM = 0.0F;
                    float MODEL_LIGHT_TO = 1.0F;
                    float ambr = this.lerp(0.0F, 1.0F, this.currentAmbient.x);
                    float ambg = this.lerp(0.0F, 1.0F, this.currentAmbient.y);
                    float ambb = this.lerp(0.0F, 1.0F, this.currentAmbient.z);
                    if (character.getCurrentSquare().isOutside()) {
                        ambr *= 1.7F;
                        ambg *= 1.7F;
                        ambb *= 1.7F;
                        this.effectLightsMain[3]
                            .set(character.getX() - 2.0F, character.getY() - 2.0F, character.getZ() + 1.0F, ambr / 4.0F, ambg / 4.0F, ambb / 4.0F, 5000);
                        this.effectLightsMain[4]
                            .set(character.getX() + 2.0F, character.getY() + 2.0F, character.getZ() + 1.0F, ambr / 4.0F, ambg / 4.0F, ambb / 4.0F, 5000);
                    } else if (character.getCurrentSquare().getRoom() != null) {
                        ambr *= 1.7F;
                        ambg *= 1.7F;
                        ambb *= 1.7F;
                        this.effectLightsMain[3]
                            .set(character.getX() - 2.0F, character.getY() - 2.0F, character.getZ() + 1.0F, ambr / 4.0F, ambg / 4.0F, ambb / 4.0F, 5000);
                        this.effectLightsMain[4]
                            .set(character.getX() + 2.0F, character.getY() + 2.0F, character.getZ() + 1.0F, ambr / 4.0F, ambg / 4.0F, ambb / 4.0F, 5000);
                    }
                }
            }
        }

        private float lerp(float f, float t, float d) {
            return f + (t - f) * d;
        }

        private void setCurrentToTarget() {
            for (int i = 0; i < this.frameLights.length; i++) {
                ModelInstance.FrameLightInfo frameLight = this.frameLights[i];
                if (frameLight != null) {
                    frameLight.currentColor.set(frameLight.targetColor);
                }
            }

            this.currentAmbient.set(this.targetAmbient);
        }

        private float step(float current, float target, float add) {
            if (current < target) {
                return ClimateManager.clamp(0.0F, target, current + add);
            } else {
                return current > target ? ClimateManager.clamp(target, 1.0F, current + add) : current;
            }
        }
    }
}
