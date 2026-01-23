// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.savefile.SavefileThumbnail;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class FBORenderObjectOutline {
    private static FBORenderObjectOutline instance;
    private boolean rendering;
    private final HashSet<IsoObject> objectSet = new HashSet<>();
    private final ArrayList<IsoObject> objectList = new ArrayList<>();
    private TextureFBO fbo;
    private Texture fboTexture;
    private final ObjectPool<FBORenderObjectOutline.Drawer> drawerPool = new ObjectPool<>(FBORenderObjectOutline.Drawer::new);
    private final ObjectPool<FBORenderObjectOutline.Drawer2> drawer2Pool = new ObjectPool<>(FBORenderObjectOutline.Drawer2::new);
    private final FBORenderObjectOutline.PerPlayerData[] perPlayerData = new FBORenderObjectOutline.PerPlayerData[4];

    public static FBORenderObjectOutline getInstance() {
        if (instance == null) {
            instance = new FBORenderObjectOutline();
        }

        return instance;
    }

    private FBORenderObjectOutline() {
        for (int i = 0; i < 4; i++) {
            this.perPlayerData[i] = new FBORenderObjectOutline.PerPlayerData();
        }
    }

    public boolean isRendering() {
        return this.rendering;
    }

    public void registerObject(IsoObject object) {
        if (!(object instanceof IsoGameCharacter)) {
            if (!this.objectSet.contains(object)) {
                this.objectSet.add(object);
            }
        }
    }

    public void unregisterObject(IsoObject object) {
        boolean bRemoved = this.objectSet.remove(object);
    }

    public void render(int playerIndex) {
        if (!SavefileThumbnail.isCreatingThumbnail()) {
            this.objectList.clear();
            if (!this.objectSet.isEmpty()) {
                this.objectList.addAll(this.objectSet);
            }

            this.rendering = true;
            this.render(playerIndex, true);
            this.render(playerIndex, false);
            this.rendering = false;
        }
    }

    private void render(int playerIndex, boolean bBehindPlayer) {
        for (int i = 0; i < this.objectList.size(); i++) {
            IsoObject object = this.objectList.get(i);
            if (object.getObjectIndex() == -1 && object.getStaticMovingObjectIndex() == -1) {
                this.objectSet.remove(object);
            } else if (!object.isOutlineHighlight()) {
                this.objectSet.remove(object);
            } else if (object.isOutlineHighlight(playerIndex) && this.isBehindPlayer(object) == bBehindPlayer) {
                this.renderObject(playerIndex, object);
            }
        }
    }

    private boolean isBehindPlayer(IsoObject object) {
        float objectX = object.getSquare().getX() + 0.5F;
        float objectY = object.getSquare().getY() + 0.5F;
        if (object instanceof IsoWorldInventoryObject worldInventoryObject) {
            objectX = worldInventoryObject.getWorldPosX();
            objectY = worldInventoryObject.getWorldPosY();
        }

        return objectX + objectY < IsoCamera.frameState.camCharacterX + IsoCamera.frameState.camCharacterY;
    }

    private void renderObject(int playerIndex, IsoObject object) {
        if (object.getSpriteModel() == null) {
            if (object.getSprite() != null) {
                if (object instanceof IsoDeadBody corpse) {
                    if (corpse.getAtlasTexture() == null || corpse.getAtlasTexture().getTexture() == null) {
                        return;
                    }
                } else if (object instanceof IsoMannequin mannequin) {
                    if (mannequin.getAtlasTexture() == null || mannequin.getAtlasTexture().getTexture() == null) {
                        return;
                    }
                } else {
                    Texture texture = object.getSprite().getTextureForCurrentFrame(object.getDir());
                    if (texture == null && !this.hasWorldItemAtlasTexture(object)) {
                        return;
                    }
                }

                ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                if (renderInfo.layer != ObjectRenderLayer.None && !(renderInfo.targetAlpha <= 0.0F)) {
                    FBORenderObjectOutline.Drawer2 drawer = this.drawer2Pool.alloc();
                    drawer.init(object);
                    SpriteRenderer.instance.drawGeneric(drawer);
                }
            }
        }
    }

    private boolean hasWorldItemAtlasTexture(IsoObject object) {
        IsoWorldInventoryObject worldInventoryObject = Type.tryCastTo(object, IsoWorldInventoryObject.class);
        if (worldInventoryObject != null && worldInventoryObject.getItem() != null) {
            boolean bUseAtlas = Core.getInstance().isOption3DGroundItem() && DebugOptions.instance.worldItemAtlas.enable.getValue();
            if (!bUseAtlas) {
                return false;
            } else {
                InventoryItem item = worldInventoryObject.getItem();
                int playerIndex = IsoCamera.frameState.playerIndex;
                float zoom = Core.getInstance().getZoom(playerIndex);
                boolean bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75F;
                WorldItemAtlas.ItemTexture atlasTexture = item.atlasTexture;
                if (atlasTexture != null && !atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
                    atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
                }

                return atlasTexture != null && atlasTexture.isStillValid(item, bMaxZoomIsOne);
            }
        } else {
            return false;
        }
    }

    public void setDuringUIRenderTime(int playerIndex, IsoObject object, long time) {
        FBORenderObjectOutline.PerPlayerData ppd = this.perPlayerData[playerIndex];
        if (time == 0L) {
            ppd.duringUiRenderTime.remove(object);
        } else {
            ppd.duringUiRenderTime.put(object, time);
        }
    }

    public long getDuringUIRenderTime(int playerIndex, IsoObject object) {
        FBORenderObjectOutline.PerPlayerData ppd = this.perPlayerData[playerIndex];
        return ppd.duringUiRenderTime.get(object);
    }

    public void setDuringUIUpdateTime(int playerIndex, IsoObject object, long time) {
        FBORenderObjectOutline.PerPlayerData ppd = this.perPlayerData[playerIndex];
        if (time == 0L) {
            ppd.duringUiUpdateTime.remove(object);
        } else {
            ppd.duringUiUpdateTime.put(object, time);
        }
    }

    public long getDuringUIUpdateTime(int playerIndex, IsoObject object) {
        FBORenderObjectOutline.PerPlayerData ppd = this.perPlayerData[playerIndex];
        return ppd.duringUiUpdateTime.get(object);
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        final ArrayList<Texture> textures = new ArrayList<>();
        float offsetY;

        FBORenderObjectOutline.Drawer init(IsoObject object) {
            this.textures.clear();
            this.offsetY = 0.0F;
            this.initTexture(object, object.getSprite());
            this.initTexture(object, object.getOverlaySprite());
            if (object.getAttachedAnimSprite() != null && !object.hasAnimatedAttachments()) {
                for (int i = 0; i < object.getAttachedAnimSprite().size(); i++) {
                    this.initTexture(object, object.getAttachedAnimSprite().get(i).getParentSprite());
                }
            }

            return this;
        }

        void initTexture(IsoObject object, IsoSprite sprite) {
            if (sprite != null && sprite.getTextureForCurrentFrame(object.getDir()) != null) {
                Texture texture = sprite.getTextureForCurrentFrame(object.getDir());
                this.textures.add(texture);
            }
        }

        @Override
        public void render() {
            if (FBORenderObjectOutline.instance.fbo == null) {
                FBORenderObjectOutline.instance.fboTexture = new Texture(64 * Core.tileScale, 128 * Core.tileScale, 16);
                FBORenderObjectOutline.instance.fbo = new TextureFBO(FBORenderObjectOutline.instance.fboTexture);
            }

            TextureFBO fbo = FBORenderObjectOutline.instance.fbo;
            GL11.glPushAttrib(2048);
            GL11.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
            Matrix4f PROJECTION = BaseVehicle.allocMatrix4f();
            PROJECTION.setOrtho2D(0.0F, fbo.getWidth(), 0.0F, fbo.getHeight());
            Matrix4f MODELVIEW = BaseVehicle.allocMatrix4f();
            MODELVIEW.identity();
            PZGLUtil.pushAndLoadMatrix(5889, PROJECTION);
            PZGLUtil.pushAndLoadMatrix(5888, MODELVIEW);
            BaseVehicle.releaseMatrix4f(PROJECTION);
            BaseVehicle.releaseMatrix4f(MODELVIEW);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(3089);
            fbo.startDrawing(true, true);
            VBORenderer vbor = VBORenderer.getInstance();

            for (int i = 0; i < this.textures.size(); i++) {
                Texture texture = this.textures.get(i);
                float x1 = texture.getOffsetX();
                float y1 = texture.getOffsetY() + this.offsetY;
                vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
                vbor.setMode(7);
                vbor.setTextureID(texture.getTextureId());
                vbor.addQuad(
                    x1,
                    y1,
                    texture.getXStart(),
                    texture.getYStart(),
                    x1 + texture.getWidth(),
                    y1 + texture.getHeight(),
                    texture.getXEnd(),
                    texture.getYEnd(),
                    0.0F,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
                vbor.endRun();
            }

            vbor.flush();
            fbo.endDrawing();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            GL11.glPopAttrib();
            GLStateRenderThread.restore();
            GL11.glEnable(3089);
        }

        @Override
        public void postRender() {
            this.textures.clear();
            FBORenderObjectOutline.instance.drawerPool.release(this);
        }
    }

    private static final class Drawer2 extends TextureDraw.GenericDrawer {
        final ArrayList<Texture> textures = new ArrayList<>();
        final ColorInfo outlineColor = new ColorInfo();
        boolean outlineBehindPlayer = true;
        float renderX;
        float renderY;
        float renderW;
        float renderH;
        static Shader outlineShader;

        FBORenderObjectOutline.Drawer2 init(IsoObject object) {
            this.textures.clear();
            int playerIndex = IsoCamera.frameState.playerIndex;
            this.outlineColor.setABGR(object.getOutlineHighlightCol(playerIndex));
            if (object.isOutlineHlBlink(playerIndex)) {
                this.outlineColor.a = this.outlineColor.a * Core.blinkAlpha;
            }

            this.outlineBehindPlayer = FBORenderObjectOutline.getInstance().isBehindPlayer(object);
            float jx = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareX;
            float jy = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareY;
            float sx = IsoUtils.XToScreen(object.getX() + jx, object.getY() + jy, object.getZ(), 0);
            float sy = IsoUtils.YToScreen(object.getX() + jx, object.getY() + jy, object.getZ(), 0);
            sx -= object.offsetX;
            sy -= object.offsetY + object.getRenderYOffset() * Core.tileScale;
            if (IsoSprite.globalOffsetX == -1.0F) {
                IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
            }

            sx += IsoSprite.globalOffsetX;
            sy += IsoSprite.globalOffsetY;
            this.renderX = sx;
            this.renderY = sy;
            this.renderW = this.renderH = -1.0F;
            if (object instanceof IsoDeadBody corpse) {
                DeadBodyAtlas.BodyTexture atlasTexture = corpse.getAtlasTexture();
                this.renderX = atlasTexture.getRenderX(sx);
                this.renderY = atlasTexture.getRenderY(sy);
                this.renderW = atlasTexture.getRenderWidth();
                this.renderH = atlasTexture.getRenderHeight();
                this.textures.add(atlasTexture.getTexture());
                return this;
            } else if (object instanceof IsoMannequin mannequin) {
                float surface = 0.0F;
                IsoObject[] objects = object.getSquare().getObjects().getElements();

                for (int i = 0; i < object.getSquare().getObjects().size(); i++) {
                    IsoObject object1 = objects[i];
                    if (object1 == object) {
                        break;
                    }

                    if (object1.isTableSurface()) {
                        surface += object1.getSurfaceOffset() * Core.tileScale;
                    }
                }

                if (object.getRenderYOffset() + surface > 0.0F) {
                    sy -= 2.0F;
                }

                DeadBodyAtlas.BodyTexture atlasTexture = mannequin.getAtlasTexture();
                this.renderX = atlasTexture.getRenderX(sx);
                this.renderY = atlasTexture.getRenderY(sy);
                this.renderW = atlasTexture.getRenderWidth();
                this.renderH = atlasTexture.getRenderHeight();
                this.textures.add(atlasTexture.getTexture());
                return this;
            } else if (this.initWorldInventoryObject(object)) {
                return this;
            } else {
                this.initTexture(object, object.getSprite());
                if (object.isOutlineHlAttached(playerIndex) && (object.hasOverlaySprite() || object.hasAttachedAnimSprites())) {
                    this.initTexture(object, object.getOverlaySprite());
                    if (object.hasAttachedAnimSprites() && !object.hasAnimatedAttachments()) {
                        for (int i = 0; i < object.getAttachedAnimSprite().size(); i++) {
                            this.initTexture(object, object.getAttachedAnimSprite().get(i).getParentSprite());
                        }
                    }
                }

                return this;
            }
        }

        boolean initWorldInventoryObject(IsoObject object) {
            if (!(object instanceof IsoWorldInventoryObject worldInventoryObject)) {
                return false;
            } else {
                int playerIndex = IsoCamera.frameState.playerIndex;
                float jx = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareX;
                float jy = IsoCamera.cameras[playerIndex].fixJigglyModelsSquareY;
                float sx = IsoUtils.XToScreen(
                    object.getX() + worldInventoryObject.xoff + jx,
                    object.getY() + worldInventoryObject.yoff + jy,
                    object.getZ() + worldInventoryObject.zoff,
                    0
                );
                float sy = IsoUtils.YToScreen(
                    object.getX() + worldInventoryObject.xoff + jx,
                    object.getY() + worldInventoryObject.yoff + jy,
                    object.getZ() + worldInventoryObject.zoff,
                    0
                );
                sx += IsoSprite.globalOffsetX;
                sy += IsoSprite.globalOffsetY;
                boolean bUseAtlas = Core.getInstance().isOption3DGroundItem() && DebugOptions.instance.worldItemAtlas.enable.getValue();
                if (bUseAtlas) {
                    InventoryItem item = worldInventoryObject.getItem();
                    float zoom = Core.getInstance().getZoom(playerIndex);
                    boolean bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75F;
                    WorldItemAtlas.ItemTexture atlasTexture = item.atlasTexture;
                    if (atlasTexture != null && !atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
                        atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
                    }

                    if (atlasTexture != null && atlasTexture.isStillValid(item, bMaxZoomIsOne) && atlasTexture.getTexture() != null) {
                        this.renderX = atlasTexture.getRenderX(sx);
                        this.renderY = atlasTexture.getRenderY(sy);
                        this.renderW = atlasTexture.getRenderWidth();
                        this.renderH = atlasTexture.getRenderHeight();
                        Texture texture = atlasTexture.getTexture();
                        this.textures.add(texture);
                        return true;
                    }
                }

                if (object.sprite != null && object.sprite.getTextureForCurrentFrame(object.getDir()) != null) {
                    Texture tex = object.sprite.getTextureForCurrentFrame(object.getDir());
                    float dx = tex.getWidthOrig() * object.sprite.def.getScaleX() / 2.0F;
                    float dy = tex.getHeightOrig() * object.sprite.def.getScaleY() * 3.0F / 4.0F;
                    this.renderX = sx - dx;
                    this.renderY = sy - dy;
                }

                return false;
            }
        }

        void initTexture(IsoObject object, IsoSprite sprite) {
            if (sprite != null && sprite.getTextureForCurrentFrame(object.getDir()) != null) {
                Texture texture = sprite.getTextureForCurrentFrame(object.getDir());
                this.textures.add(texture);
            }
        }

        @Override
        public void render() {
            boolean clear = ModelOutlines.instance.beginRenderOutline(this.outlineColor, this.outlineBehindPlayer, false);
            GL11.glDepthMask(true);
            ModelOutlines.instance.fboA.startDrawing(clear, true);
            if (outlineShader == null) {
                outlineShader = new Shader("vboRenderer_SpriteOutline", true, false);
            }

            VBORenderer vbor = VBORenderer.getInstance();

            for (int i = 0; i < this.textures.size(); i++) {
                Texture texture = this.textures.get(i);
                float x1 = this.renderX + texture.getOffsetX();
                float y1 = this.renderY + texture.getOffsetY();
                vbor.startRun(VBORenderer.getInstance().formatPositionColorUv);
                vbor.setMode(7);
                vbor.setTextureID(texture.getTextureId());
                vbor.setShaderProgram(outlineShader.getShaderProgram());
                float texW = texture.getWidth();
                float texH = texture.getHeight();
                if (this.renderW > 0.0F) {
                    texW = this.renderW;
                    texH = this.renderH;
                }

                vbor.addQuad(
                    x1, y1, texture.getXStart(), texture.getYStart(), x1 + texW, y1 + texH, texture.getXEnd(), texture.getYEnd(), 0.0F, 1.0F, 1.0F, 1.0F, 1.0F
                );
                vbor.endRun();
            }

            vbor.flush();
            ModelOutlines.instance.fboA.endDrawing();
        }

        @Override
        public void postRender() {
            this.textures.clear();
            FBORenderObjectOutline.instance.drawer2Pool.release(this);
        }
    }

    private static final class PerPlayerData {
        final TObjectLongHashMap<IsoObject> duringUiRenderTime = new TObjectLongHashMap<>();
        final TObjectLongHashMap<IsoObject> duringUiUpdateTime = new TObjectLongHashMap<>();

        PerPlayerData() {
            this.duringUiRenderTime.setAutoCompactionFactor(0.0F);
            this.duringUiUpdateTime.setAutoCompactionFactor(0.0F);
        }
    }
}
