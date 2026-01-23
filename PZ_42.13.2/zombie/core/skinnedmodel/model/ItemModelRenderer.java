// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import zombie.characterTextures.ItemSmartTexture;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.debug.DebugOptions;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public final class ItemModelRenderer {
    private static final ObjectPool<ItemModelRenderer.WeaponPartParams> s_weaponPartParamPool = new ObjectPool<>(ItemModelRenderer.WeaponPartParams::new);
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final Matrix4f s_attachmentXfrm = new Matrix4f();
    private static final ItemModelRenderer.WorldModelCamera worldModelCamera = new ItemModelRenderer.WorldModelCamera();
    static final Vector3f tempVector3f = new Vector3f(0.0F, 5.0F, -2.0F);
    private Model model;
    private ArrayList<ItemModelRenderer.WeaponPartParams> weaponParts;
    private float hue;
    private float tintR;
    private float tintG;
    private float tintB;
    public float x;
    public float y;
    public float z;
    public final Vector3f angle = new Vector3f();
    private final Matrix4f transform = new Matrix4f();
    private float ambientR;
    private float ambientG;
    private float ambientB;
    private float alpha = 1.0F;
    private float squareDepth;
    private boolean rendered;
    private int cullFace = -1;
    private float bloodLevel;
    private float fluidLevel;
    private String modelTextureName;
    private String fluidTextureMask;
    private String tintMask;
    private final ItemSmartTexture smartTexture = new ItemSmartTexture(null);
    private Color tint = Color.white;
    static Vector3f[] verts = new Vector3f[8];

    public static boolean itemHasModel(InventoryItem item) {
        if (item == null) {
            return false;
        } else if (!StringUtils.isNullOrEmpty(item.getWorldStaticItem())) {
            ModelScript modelScript = ScriptManager.instance.getModelScript(item.getWorldStaticItem());
            return modelScript != null;
        } else if (!(item instanceof Clothing clothing)) {
            if (item instanceof HandWeapon) {
                ModelScript modelScript = ScriptManager.instance.getModelScript(item.getStaticModel());
                return modelScript != null;
            } else {
                return false;
            }
        } else {
            ClothingItem clothingItem = item.getClothingItem();
            ItemVisual itemVisual = item.getVisual();
            if (clothingItem != null
                && itemVisual != null
                && "Bip01_Head".equalsIgnoreCase(clothingItem.attachBone)
                && (!clothing.isCosmetic() || item.isBodyLocation(ItemBodyLocation.EYES))) {
                boolean bFemale = false;
                String meshName = clothingItem.getModel(false);
                if (!StringUtils.isNullOrWhitespace(meshName)) {
                    return true;
                }
            }

            return false;
        }
    }

    public ItemModelRenderer.RenderStatus renderMain(
        InventoryItem item,
        IsoGridSquare square,
        IsoGridSquare renderSquare,
        float x,
        float y,
        float z,
        float flipAngle,
        float forcedRotation,
        boolean bRenderToChunkTexture
    ) {
        this.reset();
        if (!itemHasModel(item)) {
            return ItemModelRenderer.RenderStatus.NoModel;
        } else if (!StringUtils.isNullOrEmpty(item.getWorldStaticItem())) {
            ModelScript modelScript = ScriptManager.instance.getModelScript(item.getWorldStaticItem());
            if (modelScript == null) {
                return ItemModelRenderer.RenderStatus.NoModel;
            } else {
                String meshName = modelScript.getMeshName();
                String texName = modelScript.getTextureName();
                String shaderName = modelScript.getShaderName();
                ImmutableColor tint = new ImmutableColor(item.getColorRed(), item.getColorGreen(), item.getColorBlue(), 1.0F);
                float hue = 1.0F;
                FluidContainer fluidContainer = item.getFluidContainer();
                if (fluidContainer == null && item.getWorldItem() != null) {
                    fluidContainer = item.getWorldItem().getFluidContainer();
                }

                if (fluidContainer != null) {
                    this.fluidLevel = fluidContainer.getFilledRatio();
                    this.tint = fluidContainer.getColor();
                    ModelScript modelScriptFluid = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "_Fluid");
                    if (fluidContainer.getFilledRatio() > 0.5F && modelScriptFluid != null) {
                        texName = modelScriptFluid.getTextureName();
                        meshName = modelScriptFluid.getMeshName();
                        shaderName = modelScriptFluid.getShaderName();
                        modelScript = modelScriptFluid;
                    }
                }

                if (item instanceof Food food) {
                    if (item.isCooked()) {
                        ModelScript modelScriptCooked = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Cooked");
                        if (modelScriptCooked != null) {
                            texName = modelScriptCooked.getTextureName();
                            meshName = modelScriptCooked.getMeshName();
                            shaderName = modelScriptCooked.getShaderName();
                            modelScript = modelScriptCooked;
                        }
                    }

                    if (item.isBurnt()) {
                        ModelScript modelScriptBurnt = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Burnt");
                        if (modelScriptBurnt != null) {
                            texName = modelScriptBurnt.getTextureName();
                            meshName = modelScriptBurnt.getMeshName();
                            shaderName = modelScriptBurnt.getShaderName();
                            modelScript = modelScriptBurnt;
                        }
                    }

                    if (food.isRotten()) {
                        ModelScript modelScriptRotten = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Rotten");
                        if (modelScriptRotten != null) {
                            texName = modelScriptRotten.getTextureName();
                            meshName = modelScriptRotten.getMeshName();
                            shaderName = modelScriptRotten.getShaderName();
                            modelScript = modelScriptRotten;
                        } else {
                            tint = WorldItemModelDrawer.ROTTEN_FOOD_COLOR;
                        }
                    }
                }

                if (item instanceof Clothing || item.getClothingItem() != null) {
                    texName = modelScript.getTextureName(true);
                    ItemVisual itemVisual = item.getVisual();
                    ClothingItem clothingItem = item.getClothingItem();
                    tint = itemVisual.getTint(clothingItem);
                    if (texName == null) {
                        if (clothingItem.textureChoices.isEmpty()) {
                            texName = itemVisual.getBaseTexture(clothingItem);
                        } else {
                            texName = itemVisual.getTextureChoice(clothingItem);
                        }
                    }
                }

                boolean bStatic = modelScript.isStatic;
                this.modelTextureName = this.initTextureName(texName);
                Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
                if (model == null) {
                    ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                }

                model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
                if (model == null) {
                    return ItemModelRenderer.RenderStatus.NoModel;
                } else if (model.isFailure()) {
                    return ItemModelRenderer.RenderStatus.Failed;
                } else if (!model.isReady() || model.mesh == null || !model.mesh.isReady()) {
                    return ItemModelRenderer.RenderStatus.Loading;
                } else if (model.tex != null && !model.tex.isReady()) {
                    return ItemModelRenderer.RenderStatus.Loading;
                } else {
                    String tintMask = this.initTextureName(texName, "TINT");
                    Texture tintTexture = Texture.getSharedTexture(tintMask);
                    if (tintTexture != null) {
                        if (!tintTexture.isReady()) {
                            return ItemModelRenderer.RenderStatus.Loading;
                        }

                        this.tintMask = tintMask;
                    }

                    if (this.fluidLevel > 0.0F && model.tex != null) {
                        Texture texture = Texture.getSharedTexture("media/textures/FullAlpha.png");
                        if (texture != null && !texture.isReady()) {
                            return ItemModelRenderer.RenderStatus.Loading;
                        }

                        String textureMask = this.initTextureName(texName, "FLUIDTINT");
                        texture = Texture.getSharedTexture(textureMask);
                        if (texture != null) {
                            if (!texture.isReady()) {
                                return ItemModelRenderer.RenderStatus.Loading;
                            }

                            this.fluidTextureMask = textureMask;
                        }
                    }

                    this.init(item, square, renderSquare, x, y, z, model, modelScript, 1.0F, tint, flipAngle, false, bRenderToChunkTexture);
                    if (modelScript.scale != 1.0F) {
                        this.transform.scale(modelScript.scale);
                    }

                    if (item.worldScale != 1.0F) {
                        this.transform.scale(item.worldScale);
                    }

                    this.cullFace = modelScript.cullFace;
                    if (forcedRotation < 0.0F) {
                        this.angle.x = item.worldXRotation;
                        this.angle.y = item.worldZRotation;
                        this.angle.z = item.worldYRotation;
                    } else {
                        this.angle.x = 0.0F;
                        this.angle.y = forcedRotation;
                        this.angle.z = 0.0F;
                    }

                    if (Core.debug) {
                    }

                    return ItemModelRenderer.RenderStatus.Ready;
                }
            }
        } else if (!(item instanceof Clothing clothing)) {
            if (item instanceof HandWeapon handWeapon) {
                this.bloodLevel = item.getBloodLevel();
                if (this.bloodLevel > 0.0F) {
                    Texture texturex = Texture.getSharedTexture("media/textures/BloodTextures/BloodOverlayWeapon.png");
                    if (texturex != null && !texturex.isReady()) {
                        return ItemModelRenderer.RenderStatus.Loading;
                    }

                    texturex = Texture.getSharedTexture("media/textures/BloodTextures/BloodOverlayWeaponMask.png");
                    if (texturex != null && !texturex.isReady()) {
                        return ItemModelRenderer.RenderStatus.Loading;
                    }
                }

                ModelScript modelScript = ScriptManager.instance.getModelScript(item.getStaticModel());
                if (modelScript == null) {
                    return ItemModelRenderer.RenderStatus.NoModel;
                } else {
                    String meshNamex = modelScript.getMeshName();
                    String texNamex = modelScript.getTextureName();
                    String shaderNamex = modelScript.getShaderName();
                    boolean bStaticx = modelScript.isStatic;
                    this.modelTextureName = this.initTextureName(texNamex);
                    Model modelx = ModelManager.instance.tryGetLoadedModel(meshNamex, texNamex, bStaticx, shaderNamex, false);
                    if (modelx == null) {
                        ModelManager.instance.loadAdditionalModel(meshNamex, texNamex, bStaticx, shaderNamex);
                    }

                    modelx = ModelManager.instance.getLoadedModel(meshNamex, texNamex, bStaticx, shaderNamex);
                    if (modelx == null) {
                        return ItemModelRenderer.RenderStatus.NoModel;
                    } else if (modelx.isFailure()) {
                        return ItemModelRenderer.RenderStatus.Failed;
                    } else if (modelx.isReady() && modelx.mesh != null && modelx.mesh.isReady()) {
                        float huex = 1.0F;
                        ImmutableColor tintx = new ImmutableColor(item.getColorRed(), item.getColorGreen(), item.getColorBlue(), 1.0F);
                        this.init(item, square, renderSquare, x, y, z, modelx, modelScript, 1.0F, tintx, flipAngle, true, bRenderToChunkTexture);
                        if (modelScript.scale != 1.0F) {
                            this.transform.scale(modelScript.scale);
                        }

                        if (item.worldScale != 1.0F) {
                            this.transform.scale(item.worldScale);
                        }

                        this.angle.x = 0.0F;
                        this.cullFace = modelScript.cullFace;
                        if (forcedRotation < 0.0F) {
                            this.angle.x = item.worldXRotation;
                            this.angle.y = item.worldZRotation;
                            this.angle.z = item.worldYRotation;
                        } else {
                            this.angle.y = forcedRotation;
                        }

                        ItemModelRenderer.RenderStatus status = this.initWeaponParts(handWeapon, modelScript);
                        if (status != ItemModelRenderer.RenderStatus.Ready) {
                            this.reset();
                            return status;
                        } else {
                            return ItemModelRenderer.RenderStatus.Ready;
                        }
                    } else {
                        return ItemModelRenderer.RenderStatus.Loading;
                    }
                }
            } else {
                return ItemModelRenderer.RenderStatus.NoModel;
            }
        } else {
            ClothingItem clothingItem = item.getClothingItem();
            ItemVisual itemVisual = item.getVisual();
            if (clothingItem != null
                && itemVisual != null
                && "Bip01_Head".equalsIgnoreCase(clothingItem.attachBone)
                && (!clothing.isCosmetic() || item.isBodyLocation(ItemBodyLocation.EYES))) {
                boolean bFemale = false;
                String meshNamexx = clothingItem.getModel(false);
                if (StringUtils.isNullOrWhitespace(meshNamexx)) {
                    return ItemModelRenderer.RenderStatus.NoModel;
                } else {
                    String texNamexx = itemVisual.getTextureChoice(clothingItem);
                    boolean bStaticxx = clothingItem.isStatic;
                    String shaderNamexx = clothingItem.shader;
                    this.modelTextureName = this.initTextureName(texNamexx);
                    Model modelxx = ModelManager.instance.tryGetLoadedModel(meshNamexx, texNamexx, bStaticxx, shaderNamexx, false);
                    if (modelxx == null) {
                        ModelManager.instance.loadAdditionalModel(meshNamexx, texNamexx, bStaticxx, shaderNamexx);
                    }

                    modelxx = ModelManager.instance.getLoadedModel(meshNamexx, texNamexx, bStaticxx, shaderNamexx);
                    if (modelxx == null) {
                        return ItemModelRenderer.RenderStatus.NoModel;
                    } else if (modelxx.isFailure()) {
                        return ItemModelRenderer.RenderStatus.Failed;
                    } else if (modelxx.isReady() && modelxx.mesh != null && modelxx.mesh.isReady()) {
                        float huexx = itemVisual.getHue(clothingItem);
                        ImmutableColor tintxx = itemVisual.getTint(clothingItem);
                        this.init(item, square, renderSquare, x, y, z, modelxx, null, huexx, tintxx, flipAngle, false, bRenderToChunkTexture);
                        if (forcedRotation < 0.0F) {
                            this.angle.x = item.worldXRotation;
                            this.angle.y = item.worldZRotation;
                            this.angle.z = item.worldYRotation;
                        } else {
                            this.angle.x = flipAngle;
                            this.angle.y = forcedRotation;
                            this.angle.z = 0.0F;
                        }

                        if (Core.debug) {
                        }

                        return ItemModelRenderer.RenderStatus.Ready;
                    } else {
                        return ItemModelRenderer.RenderStatus.Loading;
                    }
                }
            } else {
                return ItemModelRenderer.RenderStatus.NoModel;
            }
        }
    }

    private String initTextureName(String textureName) {
        return textureName.contains("media/") ? textureName : "media/textures/" + textureName + ".png";
    }

    private String initTextureName(String textureName, String suffix) {
        if (textureName.endsWith(".png")) {
            textureName = textureName.substring(0, textureName.length() - 4);
        }

        return !textureName.contains("media/") && !textureName.contains("media\\")
            ? "media/textures/" + textureName + suffix + ".png"
            : textureName + suffix + ".png";
    }

    public boolean isRendered() {
        return this.rendered;
    }

    private void init(
        InventoryItem item,
        IsoGridSquare square,
        IsoGridSquare renderSquare,
        float worldX,
        float worldY,
        float worldZ,
        Model model,
        ModelScript modelScript,
        float hue,
        ImmutableColor tint,
        float flipAngle,
        boolean bWeaponFix,
        boolean bRenderToChunkTexture
    ) {
        this.model = model;
        if (this.weaponParts != null) {
            s_weaponPartParamPool.release(this.weaponParts);
            this.weaponParts.clear();
        }

        this.tintR = tint.r;
        this.tintG = tint.g;
        this.tintB = tint.b;
        this.hue = hue;
        if (item.getWorldItem() != null && item.getWorldItem().isHighlighted()) {
            ColorInfo highlightColor = item.getWorldItem().getHighlightColor();
            this.tintR = highlightColor.r;
            this.tintG = highlightColor.g;
            this.tintB = highlightColor.b;
        }

        this.x = worldX;
        this.y = worldY;
        this.z = worldZ;
        this.transform.rotationZ((90.0F + flipAngle) * (float) (Math.PI / 180.0));
        if (item instanceof Clothing) {
            float lower = -0.08F;
            float forward = 0.05F;
            this.transform.translate(-0.08F, 0.0F, 0.05F);
        }

        this.angle.x = 0.0F;
        this.angle.y = 525.0F;
        this.angle.z = 0.0F;
        this.transform.identity();
        this.angle.y = 0.0F;
        if (bWeaponFix) {
            this.transform.rotateXYZ(0.0F, (float) Math.PI, (float) (Math.PI / 2));
        }

        if (modelScript != null) {
            ModelAttachment attachment = modelScript.getAttachmentById("world");
            if (attachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(attachment, s_attachmentXfrm);
                s_attachmentXfrm.invert();
                this.transform.mul(s_attachmentXfrm);
            }
        } else {
            ClothingItem clothingItem = item.getClothingItem();
            ItemVisual itemVisual = item.getVisual();
            if (clothingItem != null
                && itemVisual != null
                && "Bip01_Head".equalsIgnoreCase(clothingItem.attachBone)
                && (!((Clothing)item).isCosmetic() || item.isBodyLocation(ItemBodyLocation.EYES))) {
                s_attachmentXfrm.translation(model.mesh.minXyz.x, 0.0F, 0.0F);
                s_attachmentXfrm.rotateXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
                s_attachmentXfrm.invert();
                this.transform.mul(s_attachmentXfrm);
            }
        }

        ModelInstanceRenderData.postMultiplyMeshTransform(this.transform, model.mesh);
        square.interpolateLight(tempColorInfo, this.x % 1.0F, this.y % 1.0F);
        if (GameServer.server && ServerGUI.isCreated()) {
            tempColorInfo.set(1.0F, 1.0F, 1.0F, 1.0F);
        }

        this.ambientR = tempColorInfo.r;
        this.ambientG = tempColorInfo.g;
        this.ambientB = tempColorInfo.b;
        this.alpha = IsoWorldInventoryObject.getSurfaceAlpha(square, worldZ - PZMath.fastfloor(worldZ)) * item.getWorldAlpha();
        IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
            PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), worldX, worldY, worldZ
        );
        if (bRenderToChunkTexture) {
            results.depthStart = results.depthStart
                - IsoDepthHelper.getChunkDepthData(
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                        PZMath.fastfloor(renderSquare.x / 8.0F),
                        PZMath.fastfloor(renderSquare.y / 8.0F),
                        renderSquare.z
                    )
                    .depthStart;
        }

        this.squareDepth = results.depthStart;
    }

    private ItemModelRenderer.RenderStatus initWeaponParts(HandWeapon weapon, ModelScript parentModelScript) {
        ArrayList<ModelWeaponPart> modelWeaponParts = weapon.getModelWeaponPart();
        if (modelWeaponParts == null) {
            return ItemModelRenderer.RenderStatus.Ready;
        } else {
            List<WeaponPart> parts = weapon.getAllWeaponParts();

            for (int i = 0; i < parts.size(); i++) {
                WeaponPart part = parts.get(i);

                for (int j = 0; j < modelWeaponParts.size(); j++) {
                    ModelWeaponPart mwp = modelWeaponParts.get(j);
                    if (part.getFullType().equals(mwp.partType)) {
                        ItemModelRenderer.RenderStatus status = this.initWeaponPart(mwp, parentModelScript);
                        if (status != ItemModelRenderer.RenderStatus.Ready) {
                            return status;
                        }
                        break;
                    }
                }
            }

            return ItemModelRenderer.RenderStatus.Ready;
        }
    }

    private ItemModelRenderer.RenderStatus initWeaponPart(ModelWeaponPart mwp, ModelScript parentModelScript) {
        String partStaticModel = StringUtils.discardNullOrWhitespace(mwp.modelName);
        if (partStaticModel == null) {
            return ItemModelRenderer.RenderStatus.NoModel;
        } else {
            ModelScript modelScript = ScriptManager.instance.getModelScript(partStaticModel);
            if (modelScript == null) {
                return ItemModelRenderer.RenderStatus.NoModel;
            } else {
                String meshName = modelScript.getMeshName();
                String texName = modelScript.getTextureName();
                String shaderName = modelScript.getShaderName();
                boolean bStatic = modelScript.isStatic;
                Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, false);
                if (model == null) {
                    ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                }

                model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
                if (model == null) {
                    return ItemModelRenderer.RenderStatus.NoModel;
                } else if (model.isFailure()) {
                    return ItemModelRenderer.RenderStatus.Failed;
                } else if (model.isReady() && model.mesh != null && model.mesh.isReady()) {
                    ItemModelRenderer.WeaponPartParams partParams = s_weaponPartParamPool.alloc();
                    partParams.model = model;
                    partParams.attachmentNameSelf = mwp.attachmentNameSelf;
                    partParams.attachmentNameParent = mwp.attachmentParent;
                    partParams.initTransform(parentModelScript, modelScript);
                    this.transform.mul(partParams.transform, partParams.transform);
                    if (this.weaponParts == null) {
                        this.weaponParts = new ArrayList<>();
                    }

                    this.weaponParts.add(partParams);
                    return ItemModelRenderer.RenderStatus.Ready;
                } else {
                    return ItemModelRenderer.RenderStatus.Loading;
                }
            }
        }
    }

    public void DoRenderToWorld(float x, float y, float z, Vector3f rotate) {
        worldModelCamera.x = x;
        worldModelCamera.y = y;
        worldModelCamera.z = z;
        worldModelCamera.angle.set(rotate);
        this.DoRender(worldModelCamera, false, false);
    }

    public void DoRender(IModelCamera camera, boolean bChunkTexture, boolean bHighRes) {
        GL11.glPushAttrib(1048575);
        GL11.glPushClientAttrib(-1);
        camera.Begin();
        GL14.glBlendFuncSeparate(1, 771, 773, 1);
        GL11.glDepthFunc(515);
        GL11.glDepthMask(true);
        GL11.glDepthRange(0.0, 1.0);
        GL11.glEnable(2929);
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        this.rendered = true;
        this.renderModel(this.model, this.transform, bChunkTexture, bHighRes, false);
        if (this.weaponParts != null) {
            for (int i = 0; i < this.weaponParts.size(); i++) {
                ItemModelRenderer.WeaponPartParams partParams = this.weaponParts.get(i);
                this.renderModel(partParams.model, partParams.transform, bChunkTexture, bHighRes, true);
            }
        }

        if (Core.debug && DebugOptions.instance.model.render.axis.getValue()) {
            Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
        }

        camera.End();
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
        Texture.lastTextureID = -1;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        SpriteRenderer.ringBuffer.restoreVbos = true;
        GLStateRenderThread.restore();
    }

    void renderModel(Model model, Matrix4f transform, boolean bChunkTexture, boolean bHighRes, boolean bWeaponPart) {
        if (!model.isStatic) {
            this.rendered = false;
        } else {
            if (model.effect == null) {
                model.CreateShader("basicEffect");
            }

            Shader Effect = model.effect;
            if (Effect == null || model.mesh == null || !model.mesh.isReady()) {
                this.rendered = false;
            } else if (Core.debug && DebugOptions.instance.model.render.wireframe.getValue()) {
                GL11.glPolygonMode(1032, 6913);
                GL11.glEnable(2848);
                GL11.glLineWidth(0.75F);
                Shader effect2 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", model.isStatic, false);
                if (effect2 != null) {
                    effect2.Start();
                    if (model.isStatic) {
                        effect2.setTransformMatrix(transform, false);
                    }

                    float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
                    float targetDepth = this.squareDepth - (depthBufferValue + 1.0F) / 2.0F + 0.5F;
                    if (!PerformanceSettings.fboRenderChunk) {
                        targetDepth = 0.5F;
                    }

                    effect2.setTargetDepth(targetDepth);
                    model.mesh.Draw(effect2);
                    effect2.End();
                }

                GL11.glPolygonMode(1032, 6914);
                GL11.glDisable(2848);
            } else {
                if (this.cullFace == -1) {
                    GL11.glDisable(2884);
                } else if (this.cullFace == 0) {
                    GL11.glDisable(2884);
                } else {
                    GL11.glEnable(2884);
                    GL11.glCullFace(this.cullFace);
                }

                boolean bUseSmartTexture = !bWeaponPart && this.checkSmartTexture(this.modelTextureName);
                Effect.Start();
                if (bUseSmartTexture) {
                    Effect.setTexture(this.smartTexture, "Texture", 0);
                } else if (model.tex != null) {
                    Effect.setTexture(model.tex, "Texture", 0);
                }

                Effect.setDepthBias(0.0F);
                float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
                float targetDepth = this.squareDepth - (depthBufferValue + 1.0F) / 2.0F + 0.5F;
                if (!PerformanceSettings.fboRenderChunk) {
                    targetDepth = 0.5F;
                }

                Effect.setTargetDepth(targetDepth);
                Effect.setAmbient(this.ambientR * 0.4F, this.ambientG * 0.4F, this.ambientB * 0.4F);
                Effect.setLightingAmount(1.0F);
                Effect.setHueShift(this.hue);
                Effect.setTint(1.0F, 1.0F, 1.0F);
                if (this.tintMask == null) {
                    Effect.setTint(this.tintR, this.tintG, this.tintB);
                }

                Effect.setAlpha(this.alpha);

                for (int i = 0; i < 5; i++) {
                    Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                }

                Vector3f pos = tempVector3f;
                pos.x = 0.0F;
                pos.y = 5.0F;
                pos.z = -2.0F;
                pos.rotateY((float)Math.toRadians(this.angle.y));
                targetDepth = 1.5F;
                Effect.setLight(
                    4,
                    pos.x,
                    pos.z,
                    pos.y,
                    this.ambientR / 4.0F * 1.5F,
                    this.ambientG / 4.0F * 1.5F,
                    this.ambientB / 4.0F * 1.5F,
                    5000.0F,
                    Float.NaN,
                    0.0F,
                    0.0F,
                    0.0F,
                    null
                );
                if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                    Effect.setAmbient(1.0F, 1.0F, 1.0F);
                }

                Effect.setTransformMatrix(transform, false);
                if (bChunkTexture && bHighRes) {
                    Effect.setHighResDepthMultiplier(0.5F);
                }

                model.mesh.Draw(Effect);
                if (bChunkTexture && bHighRes) {
                    Effect.setHighResDepthMultiplier(0.0F);
                }

                Effect.End();
                if (DebugOptions.instance.model.render.bounds.getValue()) {
                    VBORenderer vbor = VBORenderer.getInstance();
                    vbor.cmdPushAndMultMatrix(5888, transform);
                    vbor.addAABB(0.0F, 0.0F, 0.0F, model.mesh.minXyz, model.mesh.maxXyz, 1.0F, 1.0F, 1.0F);
                    vbor.flush();
                    vbor.setDepthTestForAllRuns(null);
                    vbor.cmdPopMatrix(5888);
                }

                if (bUseSmartTexture) {
                    if (this.smartTexture.result != null) {
                        TextureCombiner.instance.releaseTexture(this.smartTexture.result);
                        this.smartTexture.result = null;
                    }

                    this.smartTexture.clear();
                }
            }
        }
    }

    private boolean checkSmartTexture(String modelTextureName) {
        if (modelTextureName == null) {
            return false;
        } else {
            boolean bUseSmartTexture = false;
            if (this.bloodLevel > 0.0F) {
                bUseSmartTexture = true;
            }

            String textureMask = null;
            String tintTextureMask = null;
            if (this.fluidLevel > 0.0F && this.fluidTextureMask != null) {
                bUseSmartTexture = true;
                textureMask = this.fluidTextureMask;
            }

            if (this.tintMask != null) {
                bUseSmartTexture = true;
                tintTextureMask = this.tintMask;
            }

            if (bUseSmartTexture) {
                this.smartTexture.clear();
                this.smartTexture.add(modelTextureName);
                if (this.tintMask != null) {
                    ImmutableColor temp = new ImmutableColor(this.tintR, this.tintG, this.tintB, 1.0F);
                    this.smartTexture.setTintMask(tintTextureMask, "media/textures/FullAlpha.png", 300, temp.toMutableColor());
                }

                if (this.bloodLevel > 0.0F) {
                    this.smartTexture
                        .setBlood(
                            "media/textures/BloodTextures/BloodOverlayWeapon.png",
                            "media/textures/BloodTextures/BloodOverlayWeaponMask.png",
                            this.bloodLevel,
                            301
                        );
                }

                if (this.fluidLevel > 0.0F && textureMask != null) {
                    String maskPath = "media/textures/FullAlpha.png";
                    this.smartTexture.setFluid(textureMask, "media/textures/FullAlpha.png", this.fluidLevel, 302, this.tint);
                }

                this.smartTexture.calculate();
                GL11.glDepthFunc(515);
                GL11.glDepthMask(true);
                GL11.glDepthRange(0.0, 1.0);
                GL11.glEnable(2929);
            }

            return bUseSmartTexture;
        }
    }

    public float calculateMinModelZ() {
        if (this.model != null && this.model.mesh != null && this.model.mesh.isReady()) {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            Vector3f minXYZ = this.model.mesh.minXyz;
            Vector3f maxXYZ = this.model.mesh.maxXyz;
            verts[0].set(minXYZ.x, minXYZ.y, minXYZ.z);
            verts[1].set(maxXYZ.x, minXYZ.y, minXYZ.z);
            verts[2].set(maxXYZ.x, minXYZ.y, maxXYZ.z);
            verts[3].set(minXYZ.x, minXYZ.y, maxXYZ.z);
            verts[4].set(minXYZ.x, maxXYZ.y, minXYZ.z);
            verts[5].set(maxXYZ.x, maxXYZ.y, minXYZ.z);
            verts[6].set(maxXYZ.x, maxXYZ.y, maxXYZ.z);
            verts[7].set(minXYZ.x, maxXYZ.y, maxXYZ.z);
            Matrix4f xfrm = BaseVehicle.allocMatrix4f();
            xfrm.identity();
            xfrm.rotate(this.angle.x * (float) (Math.PI / 180.0), 1.0F, 0.0F, 0.0F);
            xfrm.rotate(this.angle.y * (float) (Math.PI / 180.0), 0.0F, 1.0F, 0.0F);
            xfrm.rotate(this.angle.z * (float) (Math.PI / 180.0), 0.0F, 0.0F, 1.0F);
            xfrm.mul(this.transform, xfrm);

            for (int i = 0; i < verts.length; i++) {
                Vector3f v = xfrm.transformPosition(verts[i]);
                minX = PZMath.min(minX, v.x);
                minY = PZMath.min(minY, v.y);
                minZ = PZMath.min(minZ, v.z);
                maxX = PZMath.max(maxX, v.x);
                maxY = PZMath.max(maxY, v.y);
                maxZ = PZMath.max(maxZ, v.z);
            }

            BaseVehicle.releaseMatrix4f(xfrm);
            return minY / 1.5F;
        } else {
            return 0.0F;
        }
    }

    public void reset() {
        this.rendered = false;
        this.bloodLevel = 0.0F;
        this.fluidLevel = 0.0F;
        this.modelTextureName = null;
        this.fluidTextureMask = null;
        this.tintMask = null;
        this.tint = Color.white;
        this.cullFace = -1;
        if (this.weaponParts != null) {
            s_weaponPartParamPool.release(this.weaponParts);
            this.weaponParts.clear();
        }
    }

    static {
        for (int i = 0; i < verts.length; i++) {
            verts[i] = new Vector3f();
        }
    }

    public static enum RenderStatus {
        NoModel,
        Failed,
        Loading,
        Ready;
    }

    private static final class WeaponPartParams {
        Model model;
        String attachmentNameSelf;
        String attachmentNameParent;
        final Matrix4f transform = new Matrix4f();

        void initTransform(ModelScript parentModelScript, ModelScript modelScript) {
            this.transform.identity();
            Matrix4f attachmentXfrm = ItemModelRenderer.s_attachmentXfrm;
            ModelAttachment parentAttachment = parentModelScript.getAttachmentById(this.attachmentNameParent);
            if (parentAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
                this.transform.mul(attachmentXfrm);
            }

            ModelAttachment selfAttachment = modelScript.getAttachmentById(this.attachmentNameSelf);
            if (selfAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }

                this.transform.mul(attachmentXfrm);
            }
        }
    }

    private static final class WorldModelCamera implements IModelCamera {
        float x;
        float y;
        float z;
        final Vector3f angle = new Vector3f();

        @Override
        public void Begin() {
            Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z, 0.0F, false);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.peek();
            MODELVIEW.rotate((float) -Math.PI, 0.0F, 1.0F, 0.0F);
            MODELVIEW.rotate(this.angle.x * (float) (Math.PI / 180.0), 1.0F, 0.0F, 0.0F);
            MODELVIEW.rotate(this.angle.y * (float) (Math.PI / 180.0), 0.0F, 1.0F, 0.0F);
            MODELVIEW.rotate(this.angle.z * (float) (Math.PI / 180.0), 0.0F, 0.0F, 1.0F);
            GL11.glDepthMask(true);
        }

        @Override
        public void End() {
            Core.getInstance().DoPopIsoStuff();
        }
    }
}
