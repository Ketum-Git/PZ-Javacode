// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector3f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.CharacterModelCamera;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.properties.PropertyContainer;
import zombie.core.properties.RoofProperties;
import zombie.core.skinnedmodel.ModelCameraRenderData;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteModel;
import zombie.iso.Vector3;
import zombie.iso.WorldConverter;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoMolotovCocktail;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperSliceN;
import zombie.iso.sprite.shapers.WallShaperSliceW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthModifier;
import zombie.tileDepth.TileDepthShader;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.tileDepth.TileSeamShader;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleModelCamera;

@UsedFromLua
public final class IsoSprite {
    public static int maxCount;
    public static float alphaStep = 0.05F;
    public static float globalOffsetX = -1.0F;
    public static float globalOffsetY = -1.0F;
    private static final ColorInfo info = new ColorInfo();
    private static final HashMap<String, Object[]> AnimNameSet = new HashMap<>();
    public int firerequirement;
    public String burntTile;
    public boolean forceAmbient;
    public boolean solidfloor;
    public boolean canBeRemoved;
    public boolean attachedFloor;
    public boolean cutW;
    public boolean cutN;
    public boolean solid;
    public boolean solidTrans;
    public boolean invisible;
    public boolean alwaysDraw;
    public boolean forceRender;
    public boolean moveWithWind;
    public boolean isBush;
    public static final byte RL_DEFAULT = 0;
    public static final byte RL_FLOOR = 1;
    public byte renderLayer = 0;
    public int windType = 1;
    public Texture texture;
    public boolean animate = true;
    public IsoAnim currentAnim;
    public boolean deleteWhenFinished;
    public boolean loop = true;
    public short soffX;
    public short soffY;
    public final PropertyContainer properties = new PropertyContainer();
    public final ColorInfo tintMod = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    public HashMap<String, IsoAnim> animMap;
    public ArrayList<IsoAnim> animStack;
    public String name;
    public String tilesetName;
    public int tileSheetIndex;
    public static final int DEFAULT_SPRITE_ID = 20000000;
    public int id = 20000000;
    public IsoSpriteInstance def;
    public ModelManager.ModelSlot modelSlot;
    IsoSpriteManager parentManager;
    private IsoObjectType type = IsoObjectType.MAX;
    private String parentObjectName;
    private IsoSpriteGrid spriteGrid;
    public boolean treatAsWallOrder;
    public SpriteModel spriteModel;
    public TileDepthTexture depthTexture;
    public int depthFlags;
    private boolean hideForWaterRender;
    private Vector3f curtainOffset;
    public static final int SDF_USE_OBJECT_DEPTH_TEXTURE = 1;
    public static final int SDF_TRANSLUCENT = 2;
    public static final int SDF_OPAQUE_PIXELS_ONLY = 4;
    public static TileSeamManager.Tiles seamFix2;
    public static boolean seamEast = true;
    public static final boolean SEAM_SOUTH = true;
    private static final IsoSprite.AndThen AND_THEN = new IsoSprite.AndThen();
    private boolean initRoofProperties;
    private RoofProperties roofProperties;

    public void setHideForWaterRender() {
        this.hideForWaterRender = true;
    }

    public IsoSprite() {
        this.parentManager = IsoSpriteManager.instance;
        this.def = IsoSpriteInstance.get(this);
    }

    public IsoSprite(IsoSpriteManager manager) {
        this.parentManager = manager;
        this.def = IsoSpriteInstance.get(this);
    }

    public static IsoSprite CreateSprite(IsoSpriteManager manager) {
        return new IsoSprite(manager);
    }

    public static IsoSprite CreateSpriteUsingCache(String objectName, String animName, int numFrames) {
        IsoSprite sprite = CreateSprite(IsoSpriteManager.instance);
        return sprite.setFromCache(objectName, animName, numFrames);
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, int id) {
        if (WorldConverter.instance.tilesetConversions != null
            && !WorldConverter.instance.tilesetConversions.isEmpty()
            && WorldConverter.instance.tilesetConversions.containsKey(id)) {
            id = WorldConverter.instance.tilesetConversions.get(id);
        }

        return manager.intMap.containsKey(id) ? manager.intMap.get(id) : null;
    }

    public static void setSpriteID(IsoSpriteManager manager, int id, IsoSprite spr) {
        if (manager.intMap.containsKey(spr.id)) {
            manager.intMap.remove(spr.id);
            spr.id = id;
            manager.intMap.put(id, spr);
        }
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, IsoSprite spr, int offset) {
        if (spr.name.contains("_")) {
            String[] split = spr.name.split("_");
            int id = Integer.parseInt(split[split.length - 1].trim());
            id += offset;
            return manager.namedMap.get(spr.name.substring(0, spr.name.lastIndexOf("_")) + "_" + id);
        } else {
            return null;
        }
    }

    public static IsoSprite getSprite(IsoSpriteManager manager, String name, int offset) {
        IsoSprite spr = manager.namedMap.get(name);
        if (spr == null) {
            return null;
        } else if (offset == 0) {
            return spr;
        } else if (spr.tilesetName == null) {
            if (spr.name.contains("_")) {
                String start = spr.name.substring(0, spr.name.lastIndexOf(95));
                String end = spr.name.substring(spr.name.lastIndexOf(95) + 1);
                int id = Integer.parseInt(end.trim());
                id += offset;
                return manager.getSprite(start + "_" + id);
            } else {
                return null;
            }
        } else {
            return manager.getSprite(spr.tilesetName + "_" + (spr.tileSheetIndex + offset));
        }
    }

    public static void DisposeAll() {
        AnimNameSet.clear();
    }

    public static boolean HasCache(String string) {
        return AnimNameSet.containsKey(string);
    }

    public IsoSpriteInstance newInstance() {
        return IsoSpriteInstance.get(this);
    }

    /**
     * @return the Properties
     */
    public PropertyContainer getProperties() {
        return this.properties;
    }

    public String getParentObjectName() {
        return this.parentObjectName;
    }

    public void setParentObjectName(String val) {
        this.parentObjectName = val;
    }

    public void save(DataOutputStream output) throws IOException {
        GameWindow.WriteString(output, this.name);
    }

    public void load(DataInputStream input) throws IOException {
        this.name = GameWindow.ReadString(input);
        this.LoadFramesNoDirPageSimple(this.name);
    }

    public void Dispose() {
        this.disposeAnimation();
        this.texture = null;
    }

    private void allocateAnimationIfNeeded() {
        if (this.currentAnim == null) {
            this.currentAnim = new IsoAnim();
        }

        if (this.animMap == null) {
            this.animMap = new HashMap<>(2);
        }

        if (this.animStack == null) {
            this.animStack = new ArrayList<>(1);
        }
    }

    public void disposeAnimation() {
        if (this.animMap != null) {
            for (IsoAnim anim : this.animMap.values()) {
                anim.Dispose();
            }

            this.animMap = null;
        }

        if (this.animStack != null) {
            this.animStack.clear();
            this.animStack = null;
        }

        this.currentAnim = null;
    }

    public boolean isMaskClicked(IsoDirections dir, int x, int y) {
        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return false;
            } else {
                Mask mask = image.getMask();
                if (mask == null) {
                    return false;
                } else {
                    x = (int)(x - image.offsetX);
                    y = (int)(y - image.offsetY);
                    return mask.get(x, y);
                }
            }
        } catch (Exception var6) {
            ExceptionLogger.logException(var6);
            return true;
        }
    }

    public boolean isMaskClicked(IsoDirections dir, int x, int y, boolean flip) {
        this.initSpriteInstance();

        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return false;
            } else {
                Mask mask = image.getMask();
                if (mask == null) {
                    return false;
                } else {
                    if (flip) {
                        x = (int)(x - (image.getWidthOrig() - image.getWidth() - image.offsetX));
                        y = (int)(y - image.offsetY);
                        x = image.getWidth() - x;
                    } else {
                        x = (int)(x - image.offsetX);
                        y = (int)(y - image.offsetY);
                    }

                    return x >= 0 && y >= 0 && x <= image.getWidth() && y <= image.getHeight() ? mask.get(x, y) : false;
                }
            }
        } catch (Exception var7) {
            ExceptionLogger.logException(var7);
            return true;
        }
    }

    public float getMaskClickedY(IsoDirections dir, int x, int y, boolean flip) {
        try {
            Texture image = this.getTextureForCurrentFrame(dir);
            if (image == null) {
                return 10000.0F;
            } else {
                Mask mask = image.getMask();
                if (mask == null) {
                    return 10000.0F;
                } else {
                    if (flip) {
                        x = (int)(x - (image.getWidthOrig() - image.getWidth() - image.offsetX));
                        y = (int)(y - image.offsetY);
                        x = image.getWidth() - x;
                    } else {
                        x = (int)(x - image.offsetX);
                        y = (int)(y - image.offsetY);
                        x = image.getWidth() - x;
                    }

                    return y;
                }
            }
        } catch (Exception var7) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var7);
            return 10000.0F;
        }
    }

    public Texture LoadSingleTexture(String textureName) {
        this.disposeAnimation();
        this.texture = Texture.getSharedTexture(textureName);
        return this.texture;
    }

    public Texture LoadFrameExplicit(String ObjectName) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        return this.currentAnim.LoadFrameExplicit(ObjectName);
    }

    public void LoadFrames(String ObjectName, String AnimName, int nFrames) {
        if (this.animMap == null || !this.animMap.containsKey(AnimName)) {
            this.currentAnim = new IsoAnim();
            this.allocateAnimationIfNeeded();
            this.animMap.put(AnimName, this.currentAnim);
            this.currentAnim.id = this.animStack.size();
            this.animStack.add(this.currentAnim);
            this.currentAnim.LoadFrames(ObjectName, AnimName, nFrames);
        }
    }

    public void LoadFramesReverseAltName(String ObjectName, String AnimName, String AltName, int nFrames) {
        if (this.animMap == null || !this.animMap.containsKey(AltName)) {
            this.currentAnim = new IsoAnim();
            this.allocateAnimationIfNeeded();
            this.animMap.put(AltName, this.currentAnim);
            this.currentAnim.id = this.animStack.size();
            this.animStack.add(this.currentAnim);
            this.currentAnim.LoadFramesReverseAltName(ObjectName, AnimName, AltName, nFrames);
        }
    }

    public void LoadFramesNoDirPage(String ObjectName, String AnimName, int nFrames) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(AnimName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPage(ObjectName, AnimName, nFrames);
    }

    public void LoadFramesNoDirPageDirect(String ObjectName, String AnimName, int nFrames) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put(AnimName, this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPageDirect(ObjectName, AnimName, nFrames);
    }

    public void LoadFramesNoDirPageSimple(String ObjectName) {
        if (this.animMap != null && this.animMap.containsKey("default")) {
            IsoAnim anim = this.animMap.get("default");
            this.animStack.remove(anim);
            this.animMap.remove("default");
        }

        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesNoDirPage(ObjectName);
    }

    public void ReplaceCurrentAnimFrames(String ObjectName) {
        if (this.currentAnim == null) {
            this.texture = Texture.getSharedTexture(ObjectName);
        } else {
            this.currentAnim.frames.clear();
            this.currentAnim.LoadFramesNoDirPage(ObjectName);
        }
    }

    public void LoadFramesPageSimple(String NObjectName, String SObjectName, String EObjectName, String WObjectName) {
        this.currentAnim = new IsoAnim();
        this.allocateAnimationIfNeeded();
        this.animMap.put("default", this.currentAnim);
        this.currentAnim.id = this.animStack.size();
        this.animStack.add(this.currentAnim);
        this.currentAnim.LoadFramesPageSimple(NObjectName, SObjectName, EObjectName, WObjectName);
    }

    public void PlayAnim(IsoAnim anim) {
        if (this.currentAnim == null || this.currentAnim != anim) {
            this.currentAnim = anim;
        }
    }

    public void PlayAnim(String name) {
        if ((this.currentAnim == null || !this.currentAnim.name.equals(name)) && this.animMap != null && this.animMap.containsKey(name)) {
            this.currentAnim = this.animMap.get(name);
        }
    }

    public void PlayAnimUnlooped(String name) {
        if (this.animMap != null) {
            if (this.animMap.containsKey(name)) {
                if (this.currentAnim == null || !this.currentAnim.name.equals(name)) {
                    this.currentAnim = this.animMap.get(name);
                }

                this.currentAnim.looped = false;
            }
        }
    }

    public void ChangeTintMod(ColorInfo NewTintMod) {
        this.tintMod.r = NewTintMod.r;
        this.tintMod.g = NewTintMod.g;
        this.tintMod.b = NewTintMod.b;
        this.tintMod.a = NewTintMod.a;
    }

    public void RenderGhostTile(int x, int y, int z) {
        this.RenderGhostTileColor(x, y, z, 1.0F, 1.0F, 1.0F, 0.6F);
    }

    public void RenderGhostTileRed(int x, int y, int z) {
        this.RenderGhostTileColor(x, y, z, 0.65F, 0.2F, 0.2F, 0.6F);
    }

    public void RenderGhostTileColor(int x, int y, int z, float r, float g, float b, float a) {
        this.RenderGhostTileColor(x, y, z, 0.0F, 0.0F, r, g, b, a);
    }

    public void RenderGhostTileColor(int x, int y, int z, float offsetX, float offsetY, float r, float g, float b, float a) {
        if (this.spriteModel != null) {
            ColorInfo col = new ColorInfo(r, g, b, 1.0F);
            float offset = this.getProperties().isSurfaceOffset() ? this.getProperties().getSurface() : 0.0F;
            IsoObjectModelDrawer.RenderStatus renderStatus = IsoObjectModelDrawer.renderMain(
                this.spriteModel, x + 0.5F, y + 0.5F, z, col, offsetY / Core.tileScale + offset, null, null, false, false
            );
            if (renderStatus == IsoObjectModelDrawer.RenderStatus.Loading || renderStatus == IsoObjectModelDrawer.RenderStatus.Ready) {
                return;
            }
        }

        IsoSpriteInstance spriteInstance = IsoSpriteInstance.get(this);
        spriteInstance.tintr = r;
        spriteInstance.tintg = g;
        spriteInstance.tintb = b;
        spriteInstance.alpha = spriteInstance.targetAlpha = a;
        IsoGridSquare.getDefColorInfo().set(1.0F, 1.0F, 1.0F, 1.0F);
        int SCL = Core.tileScale;
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(true);
            IndieGL.StartShader(0);
        }

        IndieGL.glDefaultBlendFunc();
        Texture texture = this.getTextureForCurrentFrame(IsoDirections.N);
        if (texture != null && texture.getName() != null && texture.getName().contains("JUMBO")) {
            this.render(spriteInstance, null, x, y, z, IsoDirections.N, 96 * SCL + offsetX, 224 * SCL + offsetY, IsoGridSquare.getDefColorInfo(), true);
        } else {
            this.render(spriteInstance, null, x, y, z, IsoDirections.N, 32 * SCL + offsetX, 96 * SCL + offsetY, IsoGridSquare.getDefColorInfo(), true);
        }

        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().setRenderingGhostTile(false);
        }

        IsoSpriteInstance.add(spriteInstance);
    }

    public boolean hasActiveModel() {
        if (!ModelManager.instance.debugEnableModels) {
            return false;
        } else {
            return !ModelManager.instance.isCreated() ? false : this.modelSlot != null && this.modelSlot.active;
        }
    }

    public void renderVehicle(
        IsoSpriteInstance inst, IsoObject obj, float x, float y, float z, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep
    ) {
        if (inst != null) {
            if (this.hasActiveModel()) {
                SpriteRenderer.instance.drawGeneric(ModelCameraRenderData.s_pool.alloc().init(VehicleModelCamera.instance, this.modelSlot));
                SpriteRenderer.instance.drawModel(this.modelSlot);
                if (!BaseVehicle.renderToTexture) {
                    return;
                }
            }

            info.r = info2.r;
            info.g = info2.g;
            info.b = info2.b;
            info.a = info2.a;

            try {
                if (bDoRenderPrep) {
                    inst.renderprep(obj);
                }

                float sx = 0.0F;
                float sy = 0.0F;
                if (globalOffsetX == -1.0F) {
                    globalOffsetX = -IsoCamera.frameState.offX;
                    globalOffsetY = -IsoCamera.frameState.offY;
                }

                if (obj == null || obj.sx == 0.0F || obj instanceof IsoMovingObject) {
                    sx = IsoUtils.XToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                    sy = IsoUtils.YToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
                    sx -= offsetX;
                    sy -= offsetY;
                    if (obj != null) {
                        obj.sx = sx;
                        obj.sy = sy;
                    }
                }

                if (obj != null) {
                    sx = obj.sx + globalOffsetX;
                    sy = obj.sy + globalOffsetY;
                    sx += this.soffX;
                    sy += this.soffY;
                } else {
                    sx += globalOffsetX;
                    sy += globalOffsetY;
                    sx += this.soffX;
                    sy += this.soffY;
                }

                if (bDoRenderPrep) {
                    if (inst.tintr != 1.0F || inst.tintg != 1.0F || inst.tintb != 1.0F) {
                        info.r = info.r * inst.tintr;
                        info.g = info.g * inst.tintg;
                        info.b = info.b * inst.tintb;
                    }

                    info.a = inst.alpha;
                }

                if (!this.hasActiveModel() && (this.tintMod.r != 1.0F || this.tintMod.g != 1.0F || this.tintMod.b != 1.0F)) {
                    info.r = info.r * this.tintMod.r;
                    info.g = info.g * this.tintMod.g;
                    info.b = info.b * this.tintMod.b;
                }

                if (this.hasActiveModel()) {
                    float scaleX = inst.getScaleX() * Core.tileScale;
                    float scaleY = -inst.getScaleY() * Core.tileScale;
                    float resized = 0.666F;
                    scaleX /= 2.664F;
                    scaleY /= 2.664F;
                    int texW = ModelManager.instance.bitmap.getTexture().getWidth();
                    int texH = ModelManager.instance.bitmap.getTexture().getHeight();
                    sx -= texW * scaleX / 2.0F;
                    sy -= texH * scaleY / 2.0F;
                    float dy = ((BaseVehicle)obj).jniTransform.origin.y / 2.44949F;
                    sy += 96.0F * dy / scaleY / 0.666F;
                    sy += 27.84F / scaleY / 0.666F;
                    if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                        SpriteRenderer.instance
                            .render((Texture)ModelManager.instance.bitmap.getTexture(), sx, sy, texW * scaleX, texH * scaleY, 1.0F, 1.0F, 1.0F, info.a, null);
                    } else {
                        SpriteRenderer.instance
                            .render(
                                (Texture)ModelManager.instance.bitmap.getTexture(), sx, sy, texW * scaleX, texH * scaleY, info.r, info.g, info.b, info.a, null
                            );
                    }

                    if (Core.debug && DebugOptions.instance.model.render.bounds.getValue()) {
                        LineDrawer.drawRect(sx, sy, texW * scaleX, texH * scaleY, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                    }
                }

                info.r = 1.0F;
                info.g = 1.0F;
                info.b = 1.0F;
            } catch (Exception var18) {
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var18);
            }
        }
    }

    private IsoSpriteInstance getSpriteInstance() {
        this.initSpriteInstance();
        return this.def;
    }

    private void initSpriteInstance() {
        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this);
        }
    }

    public final void render(IsoObject obj, float x, float y, float z, IsoDirections dir, float offsetX, float offsetY, ColorInfo info2, boolean bDoRenderPrep) {
        this.render(obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, null);
    }

    public final void render(
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.render(this.getSpriteInstance(), obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
    }

    public final void renderDepth(
        IsoObject obj,
        IsoDirections isoDirections,
        boolean cutawayNW,
        boolean cutawayNE,
        boolean cutawaySW,
        int cutawaySEX,
        float x,
        float y,
        float z,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.renderDepth(
            this.getSpriteInstance(),
            obj,
            isoDirections,
            cutawayNW,
            cutawayNE,
            cutawaySW,
            cutawaySEX,
            x,
            y,
            z,
            offsetX,
            offsetY,
            info2,
            bDoRenderPrep,
            texdModifier
        );
    }

    public final void render(
        IsoSpriteInstance inst,
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep
    ) {
        this.render(inst, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, null);
    }

    public void renderWallSliceW(
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.render(
            this.getSpriteInstance(),
            obj,
            x,
            y,
            z,
            dir,
            offsetX + 32 * Core.tileScale,
            offsetY - 16 * Core.tileScale,
            info2,
            bDoRenderPrep,
            WallShaperSliceW.instance
        );
    }

    public void renderWallSliceN(
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.render(
            this.getSpriteInstance(),
            obj,
            x,
            y,
            z,
            dir,
            offsetX - 32 * Core.tileScale,
            offsetY - 16 * Core.tileScale,
            info2,
            bDoRenderPrep,
            WallShaperSliceN.instance
        );
    }

    public void render(
        IsoSpriteInstance inst,
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        if (this.hasActiveModel()) {
            this.renderActiveModel();
        } else {
            this.renderCurrentAnim(inst, obj, x, y, z, dir, offsetX, offsetY, info2, bDoRenderPrep, texdModifier);
        }
    }

    public void renderDepth(
        IsoSpriteInstance inst,
        IsoObject obj,
        IsoDirections isoDirections,
        boolean cutawayNW,
        boolean cutawayNE,
        boolean cutawaySW,
        int cutawaySEX,
        float x,
        float y,
        float z,
        float offsetX,
        float offsetY,
        ColorInfo info2,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        this.renderCurrentAnimDepth(
            inst, obj, isoDirections, cutawayNW, cutawayNE, cutawaySW, cutawaySEX, x, y, z, offsetX, offsetY, info2, bDoRenderPrep, texdModifier
        );
    }

    public void renderCurrentAnim(
        IsoSpriteInstance inst,
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo col,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        if (DebugOptions.instance.isoSprite.renderSprites.getValue()) {
            if (!this.hasNoTextures()) {
                float frame = this.getCurrentSpriteFrame(inst);
                info.set(col);
                if (!WeatherFxMask.isRenderingMask()
                    && !FBORenderObjectHighlight.getInstance().isRenderingGhostTile()
                    && !FBORenderObjectOutline.getInstance().isRendering()) {
                    if (PerformanceSettings.fboRenderChunk) {
                        this.renderCurrentAnim_FBORender(inst, obj, x, y, z, dir, offsetX, offsetY, col, bDoRenderPrep, texdModifier);
                        return;
                    }
                } else {
                    IndieGL.disableDepthTest();
                }

                Vector3 colorInfoBackup = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
                Vector3 spritePos = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
                this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, 0.0F, texdModifier);
                info.r = colorInfoBackup.x;
                info.g = colorInfoBackup.y;
                info.b = colorInfoBackup.z;
            }
        }
    }

    private void renderCurrentAnim_FBORender(
        IsoSpriteInstance inst,
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        ColorInfo col,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        float frame = this.getCurrentSpriteFrame(inst);
        Vector3 colorInfoBackup = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
        if (this.getProperties().has(IsoFlagType.unlit)) {
            info.r = 1.0F;
            info.g = 1.0F;
            info.b = 1.0F;
        }

        Vector3 spritePos = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
        this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
        if (DebugOptions.instance.fboRenderChunk.depthTestAll.getValue()) {
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
        } else {
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(519);
        }

        if (texdModifier == CutawayAttachedModifier.instance) {
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(519);
            CutawayAttachedModifier.instance.setSprite(this);
            this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, texdModifier);
        } else if (dir == IsoDirections.NW) {
            if (texdModifier instanceof WallShaper && this.setupTileDepthWall(obj, dir, x, y, z, true)) {
                Consumer<TextureDraw> mod0 = (Consumer<TextureDraw>)(seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance);
                Consumer<TextureDraw> mod = (Consumer<TextureDraw>)(seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0);
                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
            }
        } else if (texdModifier != WallShaperWhole.instance && texdModifier != WallShaperSliceW.instance && texdModifier != WallShaperSliceN.instance) {
            float z2 = z;
            if (obj != null && obj.getRenderYOffset() != 0.0F) {
                z2 = z + obj.getRenderYOffset() / 96.0F;
            }

            if (this.setupTileDepth(obj, x, y, z, z2, true)) {
                TileDepthModifier.instance.setSpriteScale(this.def.scaleX, this.def.scaleY);
                Consumer<TextureDraw> mod0 = (Consumer<TextureDraw>)(seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance);
                Consumer<TextureDraw> mod = (Consumer<TextureDraw>)(texdModifier != null && seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0);
                if (FBORenderCell.instance.renderTranslucentOnly) {
                    IndieGL.glDepthMask(false);
                    IndieGL.enableDepthTest();
                }

                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
            } else {
                if (!(obj instanceof IsoTree tree && tree.useTreeShader)) {
                    IndieGL.StartShader(0);
                }

                if (FBORenderCell.instance.renderTranslucentOnly) {
                    int CPW = 8;
                    IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                        PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                        PZMath.fastfloor(x / 8.0F),
                        PZMath.fastfloor(y / 8.0F),
                        PZMath.fastfloor(z)
                    );
                    TextureDraw.nextChunkDepth = result.depthStart;
                    if (this.soffY != 0 && !(obj instanceof IsoFire) && !(obj instanceof IsoGameCharacter)) {
                        TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth + this.soffY / 96.0F * 0.0028867084F;
                    }

                    TextureDraw.nextChunkDepth += 0.5F;
                    TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0F - 1.0F;
                    IndieGL.glDepthMask(false);
                    IndieGL.enableDepthTest();
                    if (obj instanceof IsoTree isoTree && isoTree.useTreeShader) {
                        IsoTree.TreeShader.instance.setDepth(TextureDraw.nextChunkDepth, spritePos.z * 2.0F - 1.0F);
                    }

                    if (obj instanceof IsoTree) {
                        IndieGL.glDepthMask(true);
                    }
                } else if (!FBORenderChunkManager.instance.isCaching()) {
                    int CPWx = 8;
                    spritePos.z = spritePos.z
                        + IsoDepthHelper.getChunkDepthData(
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                                PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                                PZMath.fastfloor(x / 8.0F),
                                PZMath.fastfloor(y / 8.0F),
                                PZMath.fastfloor(z)
                            )
                            .depthStart;
                }

                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z * 2.0F - 1.0F, texdModifier);
            }
        } else if (texdModifier == WallShaperWhole.instance || texdModifier == WallShaperSliceW.instance || texdModifier == WallShaperSliceN.instance) {
            if (dir != IsoDirections.Max) {
                Consumer<TextureDraw> mod0 = (Consumer<TextureDraw>)(seamFix2 == null ? TileDepthModifier.instance : TileSeamModifier.instance);
                if (texdModifier == WallShaperSliceW.instance || texdModifier == WallShaperSliceN.instance) {
                    int dox = 0;
                    int doy = 0;
                    int dx = texdModifier == WallShaperSliceN.instance ? 1 : 0;
                    int dy = texdModifier == WallShaperSliceW.instance ? 1 : 0;
                    if (this.setupTileDepthWall2(dir, obj.square.x + dox, obj.square.y + doy, x + dx, y + dy, z, true)) {
                        IsoSprite.AndThen mod = AND_THEN.set(mod0, texdModifier);
                        this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                    }
                } else if (this.getParentSpriteDepthTextureToUse(obj) != null) {
                    TileDepthModifier.instance.setupTileDepthTexture(this, this.getParentSpriteDepthTextureToUse(obj));
                    this.startTileDepthShader(obj, x, y, z, z, true);
                    Consumer<TextureDraw> mod = (Consumer<TextureDraw>)(seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0);
                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                } else if (this.setupTileDepthWall(obj, dir, x, y, z, true)) {
                    Consumer<TextureDraw> mod = (Consumer<TextureDraw>)(seamFix2 == null ? AND_THEN.set(mod0, texdModifier) : mod0);
                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, mod);
                }
            } else {
                this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, texdModifier);
            }
        }

        info.r = colorInfoBackup.x;
        info.g = colorInfoBackup.y;
        info.b = colorInfoBackup.z;
        if (obj != null && FBORenderChunkManager.instance.isCaching()) {
            obj.sx = 0.0F;
        }
    }

    public void renderCurrentAnimDepth(
        IsoSpriteInstance inst,
        IsoObject obj,
        IsoDirections dir,
        boolean cutawayNW,
        boolean cutawayNE,
        boolean cutawaySW,
        int cutawaySEX,
        float x,
        float y,
        float z,
        float offsetX,
        float offsetY,
        ColorInfo col,
        boolean bDoRenderPrep,
        Consumer<TextureDraw> texdModifier
    ) {
        if (DebugOptions.instance.isoSprite.renderSprites.getValue()) {
            Texture texture1 = this.getTextureForCurrentFrame(dir);
            if (texture1 != null) {
                float frame = this.getCurrentSpriteFrame(inst);
                info.set(col);
                if (this.getProperties().has(IsoFlagType.unlit)) {
                    info.r = 1.0F;
                    info.g = 1.0F;
                    info.b = 1.0F;
                }

                Vector3 colorInfoBackup = IsoSprite.l_renderCurrentAnim.colorInfoBackup.set(info.r, info.g, info.b);
                Vector3 spritePos = IsoSprite.l_renderCurrentAnim.spritePos.set(0.0F, 0.0F, 0.0F);
                this.prepareToRenderSprite(inst, obj, x, y, z, dir, offsetX, offsetY, bDoRenderPrep, (int)frame, spritePos);
                if (this.setupTileDepthWall(obj, dir, x, y, z, false)) {
                    this.performRenderFrame(inst, obj, dir, (int)frame, spritePos.x, spritePos.y, spritePos.z, TileDepthModifier.instance);
                }

                info.r = colorInfoBackup.x;
                info.g = colorInfoBackup.y;
                info.b = colorInfoBackup.z;
            }
        }
    }

    private boolean setupTileDepth(IsoObject obj, float x, float y, float z, float z2, boolean drawPixels) {
        if (obj instanceof IsoTree) {
            return false;
        } else if (obj instanceof IsoBarbecue && obj.sprite != null && this != obj.sprite) {
            return false;
        } else if (obj instanceof IsoFire && this != obj.sprite) {
            TileDepthTexture billboardDepthTexture = TileDepthTextureManager.getInstance().getBillboardDepthTexture();
            if (billboardDepthTexture != null && !billboardDepthTexture.isEmpty()) {
                TileDepthModifier.instance.setupTileDepthTexture(this, billboardDepthTexture);
                this.startTileDepthShader(obj, x - 0.5F, y - 0.5F, z, z2, drawPixels);
                return true;
            } else {
                return false;
            }
        } else if (obj instanceof IsoCarBatteryCharger) {
            return false;
        } else if (obj instanceof IsoMolotovCocktail) {
            return false;
        } else if (obj instanceof IsoTrap && this.texture != null && this.texture.getName() != null && this.texture.getName().startsWith("Item_")) {
            return false;
        } else if (obj instanceof IsoWorldInventoryObject) {
            return false;
        } else if (obj instanceof IsoZombieGiblets) {
            return false;
        } else if (obj instanceof IsoGameCharacter) {
            return false;
        } else if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            if (seamFix2 == null) {
                TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            } else if (seamFix2 == TileSeamManager.Tiles.FloorSouthOneThird
                || seamFix2 == TileSeamManager.Tiles.FloorEastOneThird
                || seamFix2 == TileSeamManager.Tiles.FloorSouthTwoThirds
                || seamFix2 == TileSeamManager.Tiles.FloorEastTwoThirds) {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2, this.depthTexture);
            } else if (seamFix2 == TileSeamManager.Tiles.FloorSouth || seamFix2 == TileSeamManager.Tiles.FloorEast) {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2, this.depthTexture);
            }

            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        } else if (!this.getProperties().has(IsoFlagType.solidfloor) && !this.getProperties().has(IsoFlagType.FloorOverlay) && this.renderLayer != 1) {
            TileDepthTexture objectDepthTexture = this.getParentSpriteDepthTextureToUse(obj);
            if (objectDepthTexture != null) {
                TileDepthModifier.instance.setupTileDepthTexture(this, objectDepthTexture);
                this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                return true;
            } else if (!this.getProperties().has(IsoFlagType.windowN)
                && (!(obj instanceof IsoWindow) || !obj.getSprite().getProperties().has(IsoFlagType.windowN))) {
                if (!this.getProperties().has(IsoFlagType.windowW)
                    && (!(obj instanceof IsoWindow) || !obj.getSprite().getProperties().has(IsoFlagType.windowW))) {
                    if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedW)) {
                        TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
                        this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                        return true;
                    } else if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedN)) {
                        TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
                        this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                        return true;
                    } else if (obj instanceof IsoFireplace fireplace
                        && fireplace.isFireSpriteUsingOurDepthTexture()
                        && obj.sprite != null
                        && this != obj.sprite
                        && obj.sprite.depthTexture != null) {
                        TileDepthModifier.instance.setupTileDepthTexture(this, obj.sprite.depthTexture);
                        this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                        return true;
                    } else {
                        TileDepthTexture defaultDepthTexture = TileDepthTextureManager.getInstance().getDefaultDepthTexture();
                        if (defaultDepthTexture != null && !defaultDepthTexture.isEmpty()) {
                            TileDepthModifier.instance.setupTileDepthTexture(this, defaultDepthTexture);
                            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    TileDepthModifier.instance.setupWallDepth(this, IsoDirections.W);
                    this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                    return true;
                }
            } else {
                TileDepthModifier.instance.setupWallDepth(this, IsoDirections.N);
                this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
                return true;
            }
        } else {
            if (seamFix2 == null) {
                TileDepthModifier.instance.setupFloorDepth(this);
            } else {
                TileSeamModifier.instance.setupFloorDepth(this, seamFix2);
            }

            this.startTileDepthShader(obj, x, y, z, z2, drawPixels);
            return true;
        }
    }

    private boolean setupTileDepthWall(IsoObject obj, IsoDirections isoDirections, float x, float y, float z, boolean drawPixels) {
        if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            this.startTileDepthShader(obj, x, y, z, z, drawPixels);
            if (seamFix2 != null) {
                TileSeamModifier.instance.setupWallDepth(this, isoDirections);
            }

            return true;
        } else {
            if (seamFix2 == null) {
                TileDepthTexture objectDepthTexture = this.getParentSpriteDepthTextureToUse(obj);
                if (objectDepthTexture != null) {
                    TileDepthModifier.instance.setupTileDepthTexture(this, objectDepthTexture);
                    this.startTileDepthShader(obj, x, y, z, z, drawPixels);
                    return true;
                }

                IsoSprite sprite = this;
                if (obj != null && obj.getSprite() != null && obj.getSprite() != this && (this.depthFlags & 1) != 0) {
                    sprite = obj.getSprite();
                }

                TileDepthModifier.instance.setupWallDepth(sprite, isoDirections);
            } else {
                TileSeamModifier.instance.setupWallDepth(this, isoDirections);
            }

            this.startTileDepthShader(obj, x, y, z, z, drawPixels);
            return true;
        }
    }

    private void startTileDepthShader(IsoObject obj, float x, float y, float z, float z2, boolean drawPixels) {
        if (FBORenderCell.instance.renderTranslucentOnly) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            x += camera.fixJigglyModelsSquareX;
            y += camera.fixJigglyModelsSquareY;
        }

        int CPW = 8;
        float farDepthZ = IsoDepthHelper.getSquareDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x, y, z
            )
            .depthStart;
        float zPlusOne = z + 1.0F;
        float frontDepthZ = IsoDepthHelper.getSquareDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x + 1.0F, y + 1.0F, zPlusOne
            )
            .depthStart;
        if (z != z2) {
            float STACKED_CRATE_DEPTH_FIX = z2 - z > 0.6F ? 0.994F : 1.0F;
            STACKED_CRATE_DEPTH_FIX = 1.0F;
            farDepthZ -= (z2 - z) * 0.0028867084F * STACKED_CRATE_DEPTH_FIX;
            frontDepthZ -= (z2 - z) * 0.0028867084F * STACKED_CRATE_DEPTH_FIX;
        }

        if (FBORenderCell.instance.renderTranslucentOnly) {
            IndieGL.glDepthMask(false);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
            if (FBORenderObjectHighlight.getInstance().isRendering()) {
                frontDepthZ -= 5.0E-5F;
                farDepthZ -= 5.0E-5F;
            } else if (FBORenderCell.instance.renderAnimatedAttachments
                && obj != null
                && obj.getOnOverlay() != null
                && this == obj.getOnOverlay().getParentSprite()) {
                frontDepthZ -= 5.0E-5F;
                farDepthZ -= 5.0E-5F;
            } else if (obj instanceof IsoFireplace && this != obj.getSprite()) {
                frontDepthZ -= 5.0E-5F;
                farDepthZ -= 5.0E-5F;
            }
        } else {
            int chunkX = PZMath.fastfloor(x / 8.0F);
            int chunkY = PZMath.fastfloor(y / 8.0F);
            int level = PZMath.fastfloor(z);
            if (obj != null && obj.renderSquareOverride != null) {
                chunkX = obj.renderSquareOverride.chunk.wx;
                chunkY = obj.renderSquareOverride.chunk.wy;
                level = obj.renderSquareOverride.z;
            } else if (obj != null && obj.renderSquareOverride2 != null) {
                chunkX = PZMath.fastfloor(obj.square.x / 8.0F);
                chunkY = PZMath.fastfloor(obj.square.y / 8.0F);
            }

            IsoDepthHelper.Results resultFar = IsoDepthHelper.getChunkDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F), PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F), chunkX, chunkY, level
            );
            float chunkDepth = resultFar.depthStart;
            if (obj != null && obj.renderDepthAdjust != 0.0F) {
                chunkDepth += obj.renderDepthAdjust;
            }

            farDepthZ -= chunkDepth;
            frontDepthZ -= chunkDepth;
        }

        if (seamFix2 != null) {
            TileSeamShader shader = SceneShaderStore.tileSeamShader;
            ShaderUniformSetter uniforms = ShaderUniformSetter.uniform1f(shader, "zDepth", frontDepthZ);
            uniforms.setNext(ShaderUniformSetter.uniform1i(shader, "drawPixels", drawPixels ? 1 : 0))
                .setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendZ", frontDepthZ))
                .setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendToZ", farDepthZ));
            IndieGL.StartShader(SceneShaderStore.tileSeamShader.getID(), uniforms);
        } else {
            TileDepthShader shader = SceneShaderStore.tileDepthShader;
            if ((this.depthFlags & 4) != 0) {
                shader = SceneShaderStore.opaqueDepthShader;
            }

            ShaderUniformSetter uniforms = ShaderUniformSetter.uniform1f(shader, "zDepth", frontDepthZ);
            uniforms.setNext(ShaderUniformSetter.uniform1i(shader, "drawPixels", drawPixels ? 1 : 0))
                .setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendZ", frontDepthZ))
                .setNext(ShaderUniformSetter.uniform1f(shader, "zDepthBlendToZ", farDepthZ));
            IndieGL.StartShader(shader.getID(), uniforms);
        }
    }

    private boolean setupTileDepthWall2(IsoDirections isoDirections, int objX, int objY, float x, float y, float z, boolean drawPixels) {
        if (this.depthTexture != null && !this.depthTexture.isEmpty()) {
            TileDepthModifier.instance.setupTileDepthTexture(this, this.depthTexture);
            this.startTileDepthShader2(objX, objY, x, y, z, z, drawPixels);
            return true;
        } else {
            TileDepthModifier.instance.setupWallDepth(this, isoDirections);
            this.startTileDepthShader2(objX, objY, x, y, z, z, drawPixels);
            return true;
        }
    }

    private void startTileDepthShader2(int objX, int objY, float x, float y, float z, float z2, boolean drawPixels) {
        int CPW = 8;
        float farDepthZ = IsoDepthHelper.getSquareDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x, y, z
            )
            .depthStart;
        float zPlusOne = z + 1.0F;
        float frontDepthZ = IsoDepthHelper.getSquareDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX), PZMath.fastfloor(IsoCamera.frameState.camCharacterY), x + 1.0F, y + 1.0F, zPlusOne
            )
            .depthStart;
        if (z != z2) {
            float STACKED_CRATE_DEPTH_FIX = z2 - z > 0.6F ? 0.994F : 1.0F;
            STACKED_CRATE_DEPTH_FIX = 1.0F;
            farDepthZ -= (z2 - z) * 0.0028867084F * STACKED_CRATE_DEPTH_FIX;
            frontDepthZ -= (z2 - z) * 0.0028867084F * STACKED_CRATE_DEPTH_FIX;
        }

        if (FBORenderCell.instance.renderTranslucentOnly) {
            IndieGL.glDepthMask(false);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc(515);
        } else {
            IsoDepthHelper.Results resultFar = IsoDepthHelper.getChunkDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                PZMath.fastfloor((float)(objX / 8)),
                PZMath.fastfloor((float)(objY / 8)),
                PZMath.fastfloor(z)
            );
            float chunkDepth = resultFar.depthStart;
            chunkDepth -= 1.0E-4F;
            farDepthZ -= chunkDepth;
            frontDepthZ -= chunkDepth;
        }

        IndieGL.StartShader(SceneShaderStore.tileDepthShader.getID());
        IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepth", frontDepthZ);
        IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "drawPixels", drawPixels ? 1 : 0);
        IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepthBlendZ", frontDepthZ);
        IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepthBlendToZ", farDepthZ);
    }

    public static void renderTextureWithDepth(Texture texture, float width, float height, float r, float g, float b, float a, float x, float y, float z) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        x += camera.fixJigglyModelsSquareX;
        y += camera.fixJigglyModelsSquareY;
        if (FBORenderCell.instance.renderTranslucentOnly) {
            int CPW = 8;
            IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                PZMath.fastfloor(x / 8.0F),
                PZMath.fastfloor(y / 8.0F),
                PZMath.fastfloor(z)
            );
            TextureDraw.nextChunkDepth = result.depthStart;
            TextureDraw.nextChunkDepth += 0.5F;
            TextureDraw.nextChunkDepth = TextureDraw.nextChunkDepth * 2.0F - 1.0F;
        }

        TextureDraw.nextZ = IsoDepthHelper.calculateDepth(x, y, z) * 2.0F - 1.0F;
        IndieGL.StartShader(0);
        float screenX = IsoUtils.XToScreen(x, y, z, 0) - camera.getOffX() - width / 2.0F;
        float screenY = IsoUtils.YToScreen(x, y, z, 0) - camera.getOffY() - height;
        SpriteRenderer.instance.render(texture, screenX, screenY, width, height, r, g, b, a, null);
        IndieGL.EndShader();
    }

    private TileDepthTexture getParentSpriteDepthTextureToUse(IsoObject obj) {
        if (obj == null) {
            return null;
        } else {
            IsoSprite sprite = obj.getSprite();
            if (this == sprite) {
                return null;
            } else {
                boolean bUseObjectDepthTexture = false;
                if ((this.depthFlags & 1) != 0) {
                    bUseObjectDepthTexture = true;
                } else if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedN)) {
                    bUseObjectDepthTexture = true;
                } else if (this.getProperties().has(IsoFlagType.WallOverlay) && this.getProperties().has(IsoFlagType.attachedW)) {
                    bUseObjectDepthTexture = true;
                }

                return bUseObjectDepthTexture && sprite != null && sprite.depthTexture != null && !sprite.depthTexture.isEmpty() ? sprite.depthTexture : null;
            }
        }
    }

    public boolean hasAnimation() {
        return this.currentAnim != null;
    }

    public int getFrameCount() {
        return this.currentAnim == null ? 1 : this.currentAnim.frames.size();
    }

    public boolean hasNoTextures() {
        return this.currentAnim == null ? this.texture == null : this.currentAnim.hasNoTextures();
    }

    private float getCurrentSpriteFrame(IsoSpriteInstance inst) {
        if (!this.hasAnimation()) {
            inst.frame = 0.0F;
            return 0.0F;
        } else {
            if (this.currentAnim.framesArray == null) {
                this.currentAnim.framesArray = this.currentAnim.frames.toArray(new IsoDirectionFrame[0]);
            }

            if (this.currentAnim.framesArray.length != this.currentAnim.frames.size()) {
                this.currentAnim.framesArray = this.currentAnim.frames.toArray(this.currentAnim.framesArray);
            }

            float frame;
            if (inst.frame >= this.currentAnim.frames.size()) {
                frame = this.currentAnim.framesArray.length - 1;
            } else if (inst.frame < 0.0F) {
                inst.frame = 0.0F;
                frame = 0.0F;
            } else {
                frame = inst.frame;
            }

            return frame;
        }
    }

    private void prepareToRenderSprite(
        IsoSpriteInstance inst,
        IsoObject obj,
        float x,
        float y,
        float z,
        IsoDirections dir,
        float offsetX,
        float offsetY,
        boolean bDoRenderPrep,
        int frame,
        Vector3 spritePos
    ) {
        float ox = x;
        float oy = y;
        if (bDoRenderPrep) {
            inst.renderprep(obj);
        }

        float sx = 0.0F;
        float sy = 0.0F;
        if (globalOffsetX == -1.0F) {
            globalOffsetX = -IsoCamera.frameState.offX;
            globalOffsetY = -IsoCamera.frameState.offY;
        }

        float goX = globalOffsetX;
        float goY = globalOffsetY;
        if (FBORenderChunkManager.instance.isCaching()) {
            goX = FBORenderChunkManager.instance.getXOffset();
            goY = FBORenderChunkManager.instance.getYOffset();
            x = PZMath.coordmodulof(x, 8);
            y = PZMath.coordmodulof(y, 8);
            if (obj != null && obj.getSquare() != obj.getRenderSquare()) {
                x = ox - obj.getRenderSquare().chunk.wx * 8;
                y = oy - obj.getRenderSquare().chunk.wy * 8;
            } else if (obj != null && obj.renderSquareOverride2 != null) {
                x = ox - obj.getSquare().chunk.wx * 8;
                y = oy - obj.getSquare().chunk.wy * 8;
            }

            if (obj != null) {
                obj.sx = 0.0F;
            }
        }

        if (obj == null || obj.sx == 0.0F || obj instanceof IsoMovingObject) {
            sx = IsoUtils.XToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
            sy = IsoUtils.YToScreen(x + inst.offX, y + inst.offY, z + inst.offZ, 0);
            sx -= offsetX;
            sy -= offsetY;
            if (obj != null) {
                obj.sx = sx;
                obj.sy = sy;
            }

            sx += goX;
            sy += goY;
            sx += this.soffX;
            sy += this.soffY;
        } else if (obj != null) {
            sx = obj.sx + goX;
            sy = obj.sy + goY;
            sx += this.soffX;
            sy += this.soffY;
        } else {
            sx += goX;
            sy += goY;
            sx += this.soffX;
            sy += this.soffY;
        }

        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            float zoom = IsoCamera.frameState.zoom;
            sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * zoom;
            sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * zoom;
        }

        if (obj instanceof IsoMovingObject) {
            Texture texture1 = this.getTextureForFrame(frame, dir);
            sx -= texture1.getWidthOrig() / 2.0F * inst.getScaleX();
            sy -= texture1.getHeightOrig() * inst.getScaleY();
        }

        if (bDoRenderPrep) {
            if (inst.tintr != 1.0F || inst.tintg != 1.0F || inst.tintb != 1.0F) {
                info.r = info.r * inst.tintr;
                info.g = info.g * inst.tintg;
                info.b = info.b * inst.tintb;
            }

            info.a = inst.alpha;
            if (inst.multiplyObjectAlpha && obj != null) {
                info.a = info.a * obj.getAlpha(IsoCamera.frameState.playerIndex);
            }
        }

        if (this.tintMod.r != 1.0F || this.tintMod.g != 1.0F || this.tintMod.b != 1.0F) {
            info.r = info.r * this.tintMod.r;
            info.g = info.g * this.tintMod.g;
            info.b = info.b * this.tintMod.b;
        }

        if (PerformanceSettings.fboRenderChunk && !WeatherFxMask.isRenderingMask() && !FBORenderObjectHighlight.getInstance().isRenderingGhostTile()) {
            if (obj != null && obj.square == IsoPlayer.getInstance().getCurrentSquare()) {
                boolean var33 = false;
            }

            float sz = calculateDepth(x, y, z);
            if (obj instanceof IsoTree) {
                sz = calculateDepth(x + 0.99F, y + 0.99F, z);
                if (!FBORenderCell.instance.renderTranslucentOnly && obj.getSquare() != obj.getRenderSquare()) {
                    int CPW = 8;
                    sz = IsoDepthHelper.getSquareDepthData(
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                            ox + 0.99F,
                            oy + 0.99F,
                            z
                        )
                        .depthStart;
                    sz -= IsoDepthHelper.getChunkDepthData(
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                            PZMath.fastfloor(obj.getRenderSquare().x / 8.0F),
                            PZMath.fastfloor(obj.getRenderSquare().y / 8.0F),
                            PZMath.fastfloor(z)
                        )
                        .depthStart;
                }
            }

            if (obj != null && obj.square != null && obj.square.getWater() != null && obj.square.getWater().isValid()) {
                sz = calculateDepth(x + 0.99F, y + 0.99F, z);
            }

            if (obj instanceof IsoCarBatteryCharger || obj instanceof IsoTrap || obj instanceof IsoWorldInventoryObject) {
                sz = calculateDepth(x + 0.25F, y + 0.25F, z);
            }

            spritePos.set(sx, sy, sz);
        } else {
            spritePos.set(sx, sy, 0.0F);
        }
    }

    public static float calculateDepth(float x, float y, float z) {
        return IsoDepthHelper.calculateDepth(x, y, z);
    }

    private void performRenderFrame(
        IsoSpriteInstance inst, IsoObject obj, IsoDirections dir, int frame, float tx, float ty, float tdepth, Consumer<TextureDraw> texdModifier
    ) {
        Texture tex = this.getTextureForFrame(frame, dir);
        if (tex != null) {
            if (Core.tileScale == 2 && tex.getWidthOrig() == 64 && tex.getHeightOrig() == 128) {
                inst.setScale(2.0F, 2.0F);
            }

            if (Core.tileScale == 2 && inst.scaleX == 2.0F && inst.scaleY == 2.0F && tex.getWidthOrig() == 128 && tex.getHeightOrig() == 256) {
                inst.setScale(1.0F, 1.0F);
            }

            if (!(inst.scaleX <= 0.0F) && !(inst.scaleY <= 0.0F)) {
                float width = tex.getWidth();
                float height = tex.getHeight();
                float scaleX = inst.scaleX;
                float scaleY = inst.scaleY;
                float tx1 = tx;
                float ty1 = ty;
                if (scaleX != 1.0F) {
                    tx += tex.getOffsetX() * (scaleX - 1.0F);
                    width *= scaleX;
                }

                if (scaleY != 1.0F) {
                    ty += tex.getOffsetY() * (scaleY - 1.0F);
                    height *= scaleY;
                }

                TextureDraw.nextZ = tdepth;
                if (!this.hideForWaterRender || !IsoWater.getInstance().getShaderEnable()) {
                    if (this.hasAnimation()) {
                        IsoDirectionFrame fr = this.getAnimFrame(frame);
                        if (obj != null && obj.getObjectRenderEffectsToApply() != null) {
                            fr.render(obj.getObjectRenderEffectsToApply(), tx, ty, width, height, dir, info, inst.flip, texdModifier);
                        } else {
                            fr.render(tx, ty, width, height, dir, info, inst.flip, texdModifier);
                        }
                    } else if (obj != null && obj.getObjectRenderEffectsToApply() != null) {
                        if (inst.flip) {
                            tex.flip = !tex.flip;
                        }

                        tex.render(obj.getObjectRenderEffectsToApply(), tx, ty, width, height, info.r, info.g, info.b, info.a, texdModifier);
                        tex.flip = false;
                    } else {
                        if (inst.flip) {
                            tex.flip = !tex.flip;
                        }

                        tex.render(tx, ty, width, height, info.r, info.g, info.b, info.a, texdModifier);
                        tex.flip = false;
                    }
                }

                if (DebugOptions.instance.isoSprite.movingObjectEdges.getValue() && obj instanceof IsoMovingObject) {
                    this.renderSpriteOutline(tx1, ty1, tex, scaleX, scaleY);
                }

                if (DebugOptions.instance.isoSprite.dropShadowEdges.getValue() && StringUtils.equals(tex.getName(), "dropshadow")) {
                    this.renderSpriteOutline(tx1, ty1, tex, scaleX, scaleY);
                }

                if (PerformanceSettings.fboRenderChunk) {
                    if (obj != null) {
                        if (!FBORenderCell.instance.renderAnimatedAttachments) {
                            if (!WeatherFxMask.isRenderingMask()) {
                                if (info.a != 0.0F) {
                                    int playerIndex = IsoCamera.frameState.playerIndex;
                                    ObjectRenderInfo renderInfo = obj.getRenderInfo(playerIndex);
                                    if (FBORenderObjectHighlight.getInstance().isRendering() || FBORenderObjectOutline.getInstance().isRendering()) {
                                        boolean var18 = true;
                                    } else if (FBORenderCell.instance.renderTranslucentOnly) {
                                        renderInfo.renderX = obj.sx - IsoCamera.frameState.offX;
                                        renderInfo.renderY = obj.sy - IsoCamera.frameState.offY;
                                    } else if (FBORenderChunkManager.instance.renderChunk != null && FBORenderChunkManager.instance.renderChunk.highRes) {
                                        renderInfo.renderX = obj.sx
                                            + FBORenderChunkManager.instance.getXOffset()
                                            - FBORenderChunkManager.instance.renderChunk.w / 4.0F;
                                        renderInfo.renderY = obj.sy + FBORenderChunkManager.instance.getYOffset();
                                    } else {
                                        renderInfo.renderX = obj.sx + FBORenderChunkManager.instance.getXOffset();
                                        renderInfo.renderY = obj.sy + FBORenderChunkManager.instance.getYOffset();
                                    }

                                    renderInfo.renderWidth = tex.getWidthOrig() * scaleX;
                                    renderInfo.renderHeight = tex.getHeightOrig() * scaleY;
                                    renderInfo.renderScaleX = scaleX;
                                    renderInfo.renderScaleY = scaleY;
                                    renderInfo.renderAlpha = info.a;
                                }
                            }
                        }
                    }
                } else {
                    if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0 && obj != null) {
                        boolean flip = dir == IsoDirections.W || dir == IsoDirections.SW || dir == IsoDirections.S;
                        if (inst.flip) {
                            flip = !flip;
                        }

                        tx = obj.sx + globalOffsetX;
                        ty = obj.sy + globalOffsetY;
                        if (obj instanceof IsoMovingObject) {
                            tx -= tex.getWidthOrig() / 2.0F * scaleX;
                            ty -= tex.getHeightOrig() * scaleY;
                        }

                        IsoObjectPicker.Instance
                            .Add(
                                (int)tx,
                                (int)ty,
                                (int)(tex.getWidthOrig() * scaleX),
                                (int)(tex.getHeightOrig() * scaleY),
                                obj.square,
                                obj,
                                flip,
                                scaleX,
                                scaleY
                            );
                    }
                }
            }
        }
    }

    private void renderSpriteOutline(float tx, float ty, Texture tex, float scaleX, float scaleY) {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glBlendFunc(770, 771);
            IndieGL.StartShader(0);
            IndieGL.disableDepthTest();
        }

        LineDrawer.drawRect(tx, ty, tex.getWidthOrig() * scaleX, tex.getHeightOrig() * scaleY, 1.0F, 1.0F, 1.0F, 1.0F, 1);
        LineDrawer.drawRect(
            tx + tex.getOffsetX() * scaleX, ty + tex.getOffsetY() * scaleY, tex.getWidth() * scaleX, tex.getHeight() * scaleY, 1.0F, 1.0F, 1.0F, 1.0F, 1
        );
    }

    public void renderActiveModel() {
        if (this.modelSlot.model.object != null && DebugOptions.instance.isoSprite.renderModels.getValue()) {
            try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("Render Active Model")) {
                if (!PerformanceSettings.fboRenderChunk || !FBORenderCell.instance.renderDebugChunkState) {
                    this.modelSlot.model.updateLights();
                }

                ModelCameraRenderData cameraRenderData = ModelCameraRenderData.s_pool.alloc();
                cameraRenderData.init(CharacterModelCamera.instance, this.modelSlot);
                SpriteRenderer.instance.drawGeneric(cameraRenderData);
                SpriteRenderer.instance.drawModel(this.modelSlot);
            }
        }
    }

    public void renderBloodSplat(float x, float y, float z, ColorInfo info2) {
        Texture texture = this.getTextureForCurrentFrame(IsoDirections.N);
        if (texture != null) {
            int offsetX = -8;
            int offsetY = -228;
            int var13 = 0;
            int var14 = 0;

            try {
                if (globalOffsetX == -1.0F) {
                    globalOffsetX = -IsoCamera.frameState.offX;
                    globalOffsetY = -IsoCamera.frameState.offY;
                }

                float goX = globalOffsetX;
                float goY = globalOffsetY;
                if (FBORenderChunkManager.instance.isCaching()) {
                    goX = FBORenderChunkManager.instance.getXOffset();
                    goY = FBORenderChunkManager.instance.getYOffset();
                    x -= FBORenderChunkManager.instance.renderChunk.chunk.wx * 8;
                    y -= FBORenderChunkManager.instance.renderChunk.chunk.wy * 8;
                }

                float sx;
                float sy;
                sx = IsoUtils.XToScreen(x, y, z, 0);
                sy = IsoUtils.YToScreen(x, y, z, 0);
                sx = (int)sx;
                sy = (int)sy;
                sx -= var13;
                sy -= var14;
                sx -= texture.getWidth() / 2.0F * Core.tileScale;
                sy -= texture.getHeight() / 2.0F * Core.tileScale;
                sx += goX;
                sy += goY;
                label39:
                if (!PerformanceSettings.fboRenderChunk) {
                    if (!(sx >= IsoCamera.frameState.offscreenWidth) && !(sx + 64.0F <= 0.0F)) {
                        if (!(sy >= IsoCamera.frameState.offscreenHeight) && !(sy + 64.0F <= 0.0F)) {
                            break label39;
                        }

                        return;
                    }

                    return;
                }

                info.r = info2.r;
                info.g = info2.g;
                info.b = info2.b;
                info.a = info2.a;
                SpriteRenderer.instance.StartShader(SceneShaderStore.defaultShaderId, IsoCamera.frameState.playerIndex);
                IndieGL.disableDepthTest();
                IndieGL.glBlendFuncSeparate(770, 771, 773, 1);
                texture.render(sx, sy, texture.getWidth(), texture.getHeight(), info.r, info.g, info.b, info.a, TileDepthModifier.instance);
            } catch (Exception var12) {
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var12);
            }
        }
    }

    public void renderObjectPicker(IsoSpriteInstance def, IsoObject obj, IsoDirections dir) {
        if (def != null) {
            if (IsoPlayer.getInstance() == IsoPlayer.players[0]) {
                Texture tex = this.getTextureForFrame((int)def.frame, dir);
                if (tex != null) {
                    float sx = obj.sx + globalOffsetX;
                    float sy = obj.sy + globalOffsetY;
                    if (obj instanceof IsoMovingObject) {
                        sx -= tex.getWidthOrig() / 2.0F * def.getScaleX();
                        sy -= tex.getHeightOrig() * def.getScaleY();
                    }

                    if (IsoObjectPicker.Instance.wasDirty && IsoCamera.frameState.playerIndex == 0) {
                        boolean flip = dir == IsoDirections.W || dir == IsoDirections.SW || dir == IsoDirections.S;
                        if (def.flip) {
                            flip = !flip;
                        }

                        IsoObjectPicker.Instance
                            .Add(
                                (int)sx,
                                (int)sy,
                                (int)(tex.getWidthOrig() * def.getScaleX()),
                                (int)(tex.getHeightOrig() * def.getScaleY()),
                                obj.square,
                                obj,
                                flip,
                                def.getScaleX(),
                                def.getScaleY()
                            );
                    }
                }
            }
        }
    }

    public IsoDirectionFrame getAnimFrame(int frame) {
        if (this.currentAnim != null && !this.currentAnim.frames.isEmpty()) {
            if (this.currentAnim.framesArray == null) {
                this.currentAnim.framesArray = this.currentAnim.frames.toArray(new IsoDirectionFrame[0]);
            }

            if (this.currentAnim.framesArray.length != this.currentAnim.frames.size()) {
                this.currentAnim.framesArray = this.currentAnim.frames.toArray(this.currentAnim.framesArray);
            }

            if (frame >= this.currentAnim.framesArray.length) {
                frame = this.currentAnim.framesArray.length - 1;
            }

            if (frame < 0) {
                frame = 0;
            }

            return this.currentAnim.framesArray[frame];
        } else {
            return null;
        }
    }

    public Texture getTextureForFrame(int frame, IsoDirections dir) {
        return this.currentAnim != null && !this.currentAnim.frames.isEmpty() ? this.getAnimFrame(frame).getTexture(dir) : this.texture;
    }

    public Texture getTextureForCurrentFrame(IsoDirections dir) {
        this.initSpriteInstance();
        return this.getTextureForFrame((int)this.def.frame, dir);
    }

    public void update() {
        this.update(this.def);
    }

    public void update(IsoSpriteInstance def) {
        if (def == null) {
            def = IsoSpriteInstance.get(this);
        }

        if (this.currentAnim == null) {
            def.frame = 0.0F;
        } else {
            if (this.animate && !def.finished) {
                float lastFrame = def.frame;
                if (!GameTime.isGamePaused()) {
                    def.frame = def.frame + def.animFrameIncrease * (GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F);
                }

                if ((int)def.frame >= this.currentAnim.frames.size() && this.loop && def.looped) {
                    def.frame = 0.0F;
                }

                if ((int)lastFrame != (int)def.frame) {
                    def.nextFrame = true;
                }

                if ((int)def.frame >= this.currentAnim.frames.size() && (!this.loop || !def.looped)) {
                    def.finished = true;
                    def.frame = this.currentAnim.finishUnloopedOnFrame;
                    if (this.deleteWhenFinished) {
                        this.Dispose();
                        this.animate = false;
                    }
                }
            }
        }
    }

    public void CacheAnims(String key) {
        this.name = key;
        Stack<String> animList = new Stack<>();

        for (int n = 0; n < this.animStack.size(); n++) {
            IsoAnim anim = this.animStack.get(n);
            String total = key + anim.name;
            animList.add(total);
            if (!IsoAnim.GlobalAnimMap.containsKey(total)) {
                IsoAnim.GlobalAnimMap.put(total, anim);
            }
        }

        AnimNameSet.put(key, animList.toArray());
    }

    public void LoadCache(String string) {
        this.allocateAnimationIfNeeded();
        Object[] arr = AnimNameSet.get(string);
        this.name = string;

        for (int n = 0; n < arr.length; n++) {
            String s = (String)arr[n];
            IsoAnim a = IsoAnim.GlobalAnimMap.get(s);
            this.animMap.put(a.name, a);
            this.animStack.add(a);
            this.currentAnim = a;
        }
    }

    public IsoSprite setFromCache(String objectName, String animName, int numFrames) {
        String cacheKey = objectName + animName;
        if (HasCache(cacheKey)) {
            this.LoadCache(cacheKey);
        } else {
            this.LoadFramesNoDirPage(objectName, animName, numFrames);
            this.CacheAnims(cacheKey);
        }

        return this;
    }

    public IsoObjectType getType() {
        return this.type;
    }

    public void setType(IsoObjectType ntype) {
        this.type = ntype;
    }

    public void AddProperties(IsoSprite sprite) {
        this.getProperties().AddProperties(sprite.getProperties());
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public ColorInfo getTintMod() {
        return this.tintMod;
    }

    public void setTintMod(ColorInfo info) {
        this.tintMod.set(info);
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    public IsoSpriteGrid getSpriteGrid() {
        return this.spriteGrid;
    }

    public void setSpriteGrid(IsoSpriteGrid sGrid) {
        this.spriteGrid = sGrid;
    }

    public boolean isMoveWithWind() {
        return this.moveWithWind;
    }

    public boolean is(IsoFlagType flag) {
        return this.getProperties().has(flag);
    }

    public boolean isWallSE() {
        return this.is(IsoFlagType.WallSE);
    }

    public int getSheetGridIdFromName() {
        return this.name != null ? getSheetGridIdFromName(this.name) : -1;
    }

    public static int getSheetGridIdFromName(String name) {
        if (name != null) {
            int index = name.lastIndexOf(95);
            if (index > 0 && index + 1 < name.length()) {
                return Integer.parseInt(name.substring(index + 1));
            }
        }

        return -1;
    }

    public IsoDirections getFacing() {
        if (this.getProperties().has("Facing")) {
            String Facing = this.getProperties().get("Facing");
            if (Facing != null) {
                switch (Facing) {
                    case "N":
                        return IsoDirections.N;
                    case "S":
                        return IsoDirections.S;
                    case "W":
                        return IsoDirections.W;
                    case "E":
                        return IsoDirections.E;
                }
            }
        }

        return null;
    }

    private void initRoofProperties() {
        if (!this.initRoofProperties) {
            this.initRoofProperties = true;
            this.roofProperties = RoofProperties.initSprite(this);
        }
    }

    public RoofProperties getRoofProperties() {
        this.initRoofProperties();
        return this.roofProperties;
    }

    public void clearCurtainOffset() {
        this.curtainOffset = null;
    }

    public void setCurtainOffset(float x, float y, float z) {
        if (this.curtainOffset == null) {
            this.curtainOffset = new Vector3f();
        }

        this.curtainOffset.set(x, y, z);
    }

    public Vector3f getCurtainOffset() {
        return this.curtainOffset;
    }

    public boolean shouldHaveCollision() {
        return this.getProperties() == null
            ? false
            : this.getProperties().has(IsoFlagType.solid)
                || this.getProperties().has(IsoFlagType.solidtrans)
                || this.getProperties().has(IsoFlagType.WallN)
                || this.getProperties().has(IsoFlagType.WallNW)
                || this.getProperties().has(IsoFlagType.WallW)
                || this.getProperties().has(IsoFlagType.collideN)
                || this.getProperties().has(IsoFlagType.collideW);
    }

    private static final class AndThen implements Consumer<TextureDraw> {
        private Consumer<? super TextureDraw> a;
        private Consumer<? super TextureDraw> b;

        private IsoSprite.AndThen set(Consumer<TextureDraw> a, Consumer<TextureDraw> b) {
            this.a = a;
            this.b = b;
            return this;
        }

        public void accept(TextureDraw o) {
            this.a.accept(o);
            this.b.accept(o);
        }
    }

    private static class l_renderCurrentAnim {
        static final Vector3 colorInfoBackup = new Vector3();
        static final Vector3 spritePos = new Vector3();
    }
}
