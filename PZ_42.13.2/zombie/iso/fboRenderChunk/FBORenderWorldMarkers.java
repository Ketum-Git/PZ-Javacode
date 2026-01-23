// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.WorldMarkers;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public class FBORenderWorldMarkers {
    private static FBORenderWorldMarkers instance;
    private final ArrayList<FBORenderWorldMarkers.Marker> highlights = new ArrayList<>();
    private final ObjectPool<FBORenderWorldMarkers.Marker> highlightPool = new ObjectPool<>(FBORenderWorldMarkers.Marker::new);
    private final ObjectPool<FBORenderWorldMarkers.Drawer> drawerPool = new ObjectPool<>(FBORenderWorldMarkers.Drawer::new);
    private final boolean outline = false;
    private final float outlineR = 1.0F;
    private final float outlineG = 1.0F;
    private final float outlineB = 1.0F;
    private final boolean useGroundDepth = true;

    public static FBORenderWorldMarkers getInstance() {
        if (instance == null) {
            instance = new FBORenderWorldMarkers();
        }

        return instance;
    }

    public void render(int z, List<WorldMarkers.GridSquareMarker> markerList) {
        for (int i = 0; i < this.highlights.size(); i++) {
            FBORenderWorldMarkers.Marker ah = this.highlights.get(i);
            this.highlights.remove(i--);
            this.highlightPool.release(ah);
        }

        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderWorldMarkers.Drawer drawer = this.drawerPool.alloc();
        drawer.playerIndex = playerIndex;
        this.highlightPool.releaseAll(drawer.highlights);
        drawer.highlights.clear();

        for (int i = 0; i < markerList.size(); i++) {
            WorldMarkers.GridSquareMarker gsm = markerList.get(i);
            if (gsm.isActive() && gsm.getZ() == z) {
                float centerX = gsm.getOriginalX() + 0.5F;
                float centerY = gsm.getOriginalY() + 0.5F;
                float radius = gsm.getSize() * 0.69F;
                FBORenderWorldMarkers.Marker mkr = this.highlightPool
                    .alloc()
                    .set(
                        centerX - radius,
                        centerY - radius,
                        centerX + radius,
                        centerY + radius,
                        gsm.getOriginalZ(),
                        gsm.getR(),
                        gsm.getG(),
                        gsm.getB(),
                        gsm.getAlpha()
                    );
                String textureName = gsm.getTextureName();
                String overlayTextureName = gsm.getOverlayTextureName();
                if (StringUtils.equals(textureName, "circle_center") || StringUtils.equals(textureName, "circle_highlight_2")) {
                    mkr.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
                } else if (StringUtils.equals(textureName, "circle_highlight")) {
                    mkr.texture1 = Texture.getSharedTexture("media/textures/worldMap/circle_center.png");
                    mkr.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
                }

                if (StringUtils.equals(overlayTextureName, "circle_only_highlight") || StringUtils.equals(overlayTextureName, "circle_only_highlight_2")) {
                    mkr.texture2 = Texture.getSharedTexture("media/textures/worldMap/circle_only_highlight.png");
                }

                this.highlights.add(mkr);
            }
        }

        for (int ix = 0; ix < this.highlights.size(); ix++) {
            FBORenderWorldMarkers.Marker ah1 = this.highlights.get(ix);
            if (ah1.isOnScreen(playerIndex)) {
                FBORenderWorldMarkers.Marker ah2 = this.highlightPool.alloc().set(ah1);
                this.renderOutline(ah2);
                drawer.highlights.add(ah2);
            }
        }

        if (drawer.highlights.isEmpty()) {
            this.drawerPool.release(drawer);
        } else {
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void renderOutline(FBORenderWorldMarkers.Marker ah) {
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        final ArrayList<FBORenderWorldMarkers.Marker> highlights = new ArrayList<>();
        int playerIndex;

        @Override
        public void render() {
            SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
            PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
            float rcx = camera.rightClickX;
            float rcy = camera.rightClickY;
            float tox = camera.getTOffX();
            float toy = camera.getTOffY();
            float defx = camera.deferedX;
            float defy = camera.deferedY;
            float playerX = Core.getInstance().floatParamMap.get(0);
            float playerY = Core.getInstance().floatParamMap.get(1);
            float playerZ = Core.getInstance().floatParamMap.get(2);
            float cx = playerX - camera.XToIso(-tox - rcx, -toy - rcy, 0.0F);
            float cy = playerY - camera.YToIso(-tox - rcx, -toy - rcy, 0.0F);
            cx += defx;
            cy += defy;
            double screenWidth = camera.offscreenWidth / 1920.0F;
            double screenHeight = camera.offscreenHeight / 1920.0F;
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho(
                -((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F
            );
            Core.getInstance().projectionMatrixStack.push(PROJECTION);
            GL11.glEnable(2929);
            GL11.glDepthFunc(515);
            GL11.glDepthMask(false);
            GL11.glBlendFunc(770, 771);

            for (int i = 0; i < this.highlights.size(); i++) {
                FBORenderWorldMarkers.Marker ah = this.highlights.get(i);
                ah.render(playerX, playerY, playerZ, cx, cy);
            }

            VBORenderer vbor = VBORenderer.getInstance();
            vbor.flush();
            Core.getInstance().projectionMatrixStack.pop();
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderWorldMarkers.getInstance().drawerPool.release(this);
        }
    }

    private static final class Marker {
        float x1;
        float y1;
        float x2;
        float y2;
        float z;
        float r;
        float g;
        float b;
        float a;
        long renderTimeMs;
        Texture texture1;
        Texture texture2;

        FBORenderWorldMarkers.Marker set(float x1, float y1, float x2, float y2, float z, float r, float g, float b, float a) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.renderTimeMs = UIManager.uiRenderTimeMS;
            this.texture1 = null;
            this.texture2 = null;
            return this;
        }

        FBORenderWorldMarkers.Marker set(FBORenderWorldMarkers.Marker other) {
            this.set(other.x1, other.y1, other.x2, other.y2, other.z, other.r, other.g, other.b, other.a);
            this.texture1 = other.texture1;
            this.texture2 = other.texture2;
            return this;
        }

        boolean isOnScreen(int playerIndex) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (chunkMap.ignore) {
                return false;
            } else {
                int xMin = chunkMap.getWorldXMinTiles();
                int yMin = chunkMap.getWorldYMinTiles();
                int xMax = chunkMap.getWorldXMaxTiles();
                int yMax = chunkMap.getWorldYMaxTiles();
                return this.x1 < xMax && this.x2 > xMin && this.y1 < yMax && this.y2 > yMin;
            }
        }

        void render(float playerX, float playerY, float playerZ, float cx, float cy) {
            VBORenderer vbor = VBORenderer.getInstance();
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = 1.0F;
            float v1 = 0.0F;
            float u2 = 1.0F;
            float v2 = 1.0F;
            float u3 = 0.0F;
            float v3 = 1.0F;
            float ox = (this.x1 + this.x2) / 2.0F;
            float oy = (this.y1 + this.y2) / 2.0F;
            float DEPTH_ADJUST = -1.0E-4F;
            DEPTH_ADJUST -= 5.0E-5F;
            float depth0 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.x1, this.y1, this.z).depthStart
                + DEPTH_ADJUST;
            float depth1 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.x2, this.y1, this.z).depthStart
                + DEPTH_ADJUST;
            float depth2 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.x2, this.y2, this.z).depthStart
                + DEPTH_ADJUST;
            float depth3 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.x1, this.y2, this.z).depthStart
                + DEPTH_ADJUST;
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.scaling(Core.scale);
            MODELVIEW.scale(Core.tileScale / 2.0F);
            MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
            MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
            double difX = ox - cx;
            double difY = oy - cy;
            double difZ = (this.z - playerZ) * 2.44949F;
            MODELVIEW.translate(-((float)difX), (float)difZ, -((float)difY));
            MODELVIEW.scale(-1.0F, 1.0F, -1.0F);
            MODELVIEW.translate(0.0F, -0.71999997F, 0.0F);
            vbor.cmdPushAndLoadMatrix(5888, MODELVIEW);
            vbor.startRun(vbor.formatPositionColorUvDepth);
            vbor.setMode(7);
            vbor.setDepthTest(true);
            if (this.texture1 != null) {
                vbor.setTextureID(this.texture1.getTextureId());
            }

            float z = 0.0F;
            vbor.addQuadDepth(
                this.x1 - ox,
                0.0F,
                this.y1 - oy,
                0.0F,
                0.0F,
                depth0,
                this.x2 - ox,
                0.0F,
                this.y1 - oy,
                1.0F,
                0.0F,
                depth1,
                this.x2 - ox,
                0.0F,
                this.y2 - oy,
                1.0F,
                1.0F,
                depth2,
                this.x1 - ox,
                0.0F,
                this.y2 - oy,
                0.0F,
                1.0F,
                depth3,
                this.r,
                this.g,
                this.b,
                this.a
            );
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColorUvDepth);
            vbor.setMode(7);
            vbor.setDepthTest(true);
            if (this.texture2 != null) {
                vbor.setTextureID(this.texture2.getTextureId());
            }

            vbor.addQuadDepth(
                this.x1 - ox,
                0.0F,
                this.y1 - oy,
                0.0F,
                0.0F,
                depth0,
                this.x2 - ox,
                0.0F,
                this.y1 - oy,
                1.0F,
                0.0F,
                depth1,
                this.x2 - ox,
                0.0F,
                this.y2 - oy,
                1.0F,
                1.0F,
                depth2,
                this.x1 - ox,
                0.0F,
                this.y2 - oy,
                0.0F,
                1.0F,
                depth3,
                this.r,
                this.g,
                this.b,
                this.a
            );
            vbor.endRun();
            vbor.cmdPopMatrix(5888);
        }
    }
}
