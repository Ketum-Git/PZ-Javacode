// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.ui.UIManager;

public class FBORenderAreaHighlights {
    private static FBORenderAreaHighlights instance;
    private final ArrayList<FBORenderAreaHighlights.AreaHighlight> highlights = new ArrayList<>();
    private final ObjectPool<FBORenderAreaHighlights.AreaHighlight> highlightPool = new ObjectPool<>(FBORenderAreaHighlights.AreaHighlight::new);
    private final ObjectPool<FBORenderAreaHighlights.Drawer> drawerPool = new ObjectPool<>(FBORenderAreaHighlights.Drawer::new);
    private final boolean outline = true;
    private final float outlineR = 1.0F;
    private final float outlineG = 1.0F;
    private final float outlineB = 1.0F;
    private final boolean useGroundDepth = true;

    public static FBORenderAreaHighlights getInstance() {
        if (instance == null) {
            instance = new FBORenderAreaHighlights();
        }

        return instance;
    }

    public void addHighlight(int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
        FBORenderAreaHighlights.AreaHighlight ah = this.highlightPool.alloc().set(-1, x1, y1, x2, y2, z, r, g, b, a);
        this.highlights.add(ah);
    }

    public void addHighlightForPlayer(int playerIndex, int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
        FBORenderAreaHighlights.AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, x1, y1, x2, y2, z, r, g, b, a);
        this.highlights.add(ah);
    }

    public void render() {
        for (int i = 0; i < this.highlights.size(); i++) {
            FBORenderAreaHighlights.AreaHighlight ah = this.highlights.get(i);
            if (UIManager.uiRenderTimeMS != ah.renderTimeMs) {
                this.highlights.remove(i--);
                this.highlightPool.release(ah);
            }
        }

        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderAreaHighlights.Drawer drawer = this.drawerPool.alloc();
        drawer.playerIndex = playerIndex;
        this.highlightPool.releaseAll(drawer.highlights);
        drawer.highlights.clear();
        this.renderUserDefinedAreas(drawer);
        this.renderNonPVPZones(drawer);
        this.renderSafehouses(drawer);
        this.renderAnimalDesigationZones(drawer);
        if (drawer.highlights.isEmpty()) {
            this.drawerPool.release(drawer);
        } else {
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void renderUserDefinedAreas(FBORenderAreaHighlights.Drawer drawer) {
        int playerIndex = drawer.playerIndex;

        for (int i = 0; i < this.highlights.size(); i++) {
            FBORenderAreaHighlights.AreaHighlight ah1 = this.highlights.get(i);
            if ((ah1.playerIndex == -1 || ah1.playerIndex == playerIndex) && ah1.isOnScreen(playerIndex)) {
                FBORenderAreaHighlights.AreaHighlight ah2 = this.highlightPool.alloc().set(ah1);
                this.renderOutline(ah2);
                ah2.clampToChunkMap(playerIndex);
                drawer.highlights.add(ah2);
            }
        }
    }

    private void renderNonPVPZones(FBORenderAreaHighlights.Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (GameClient.client && player != null && player.isSeeNonPvpZone()) {
            ArrayList<NonPvpZone> nonPvpZones = NonPvpZone.getAllZones();

            for (int i = 0; i < nonPvpZones.size(); i++) {
                NonPvpZone npz = nonPvpZones.get(i);
                float r = 0.0F;
                float g = 0.0F;
                float b = 1.0F;
                float a = 0.25F;
                FBORenderAreaHighlights.AreaHighlight ah = this.highlightPool
                    .alloc()
                    .set(playerIndex, npz.getX(), npz.getY(), npz.getX2(), npz.getY2(), 0, 0.0F, 0.0F, 1.0F, 0.25F);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void renderSafehouses(FBORenderAreaHighlights.Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        if (GameClient.client && Core.debug) {
            ArrayList<SafeHouse> safeHouses = SafeHouse.getSafehouseList();

            for (int i = 0; i < safeHouses.size(); i++) {
                SafeHouse sh = safeHouses.get(i);
                float r = 1.0F;
                float g = 0.0F;
                float b = 0.0F;
                float a = 0.25F;
                FBORenderAreaHighlights.AreaHighlight ah = this.highlightPool
                    .alloc()
                    .set(playerIndex, sh.getX(), sh.getY(), sh.getX2(), sh.getY2(), 0, 1.0F, 0.0F, 0.0F, 0.25F);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void renderAnimalDesigationZones(FBORenderAreaHighlights.Drawer drawer) {
        int playerIndex = drawer.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player != null && player.isSeeDesignationZone()) {
            ArrayList<Double> selectedMetaAnimalZones = player.getSelectedZonesForHighlight();
            ArrayList<DesignationZone> zones = DesignationZone.allZones;

            for (int i = 0; i < zones.size(); i++) {
                DesignationZone dz = zones.get(i);
                float r = 0.2F;
                float g = 0.2F;
                float b = 0.9F;
                float a = 0.4F;
                if (selectedMetaAnimalZones.contains(dz.getId())) {
                    r = 0.2F;
                    g = 0.8F;
                    b = 0.9F;
                    a = 0.4F;
                }

                FBORenderAreaHighlights.AreaHighlight ah = this.highlightPool.alloc().set(playerIndex, dz.x, dz.y, dz.x + dz.w, dz.y + dz.h, dz.z, r, g, b, a);
                this.tryRenderArea(drawer, ah);
            }
        }
    }

    private void tryRenderArea(FBORenderAreaHighlights.Drawer drawer, FBORenderAreaHighlights.AreaHighlight ah) {
        int playerIndex = drawer.playerIndex;
        if (ah.isOnScreen(playerIndex)) {
            this.renderOutline(ah);
            ah.clampToChunkMap(playerIndex);
            drawer.highlights.add(ah);
        } else {
            this.highlightPool.release(ah);
        }
    }

    private void renderOutline(FBORenderAreaHighlights.AreaHighlight ah) {
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        r = ah.r;
        g = ah.g;
        b = ah.b;
        LineDrawer.addRect(ah.x1, ah.y1, ah.z, ah.x2 - ah.x1, ah.y2 - ah.y1, r, g, b);
    }

    private static final class AreaHighlight {
        int playerIndex = -1;
        int x1;
        int y1;
        int x2;
        int y2;
        int z;
        float r;
        float g;
        float b;
        float a;
        long renderTimeMs;

        FBORenderAreaHighlights.AreaHighlight set(int playerIndex, int x1, int y1, int x2, int y2, int z, float r, float g, float b, float a) {
            this.playerIndex = playerIndex;
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
            return this;
        }

        FBORenderAreaHighlights.AreaHighlight set(FBORenderAreaHighlights.AreaHighlight other) {
            return this.set(other.playerIndex, other.x1, other.y1, other.x2, other.y2, other.z, other.r, other.g, other.b, other.a);
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

        void clampToChunkMap(int playerIndex) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (!chunkMap.ignore) {
                int xMin = chunkMap.getWorldXMinTiles();
                int yMin = chunkMap.getWorldYMinTiles();
                int xMax = chunkMap.getWorldXMaxTiles();
                int yMax = chunkMap.getWorldYMaxTiles();
                this.x1 = PZMath.max(this.x1, xMin);
                this.y1 = PZMath.max(this.y1, yMin);
                this.x2 = PZMath.min(this.x2, xMax);
                this.y2 = PZMath.min(this.y2, yMax);
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
            float DEPTH_ADJUST = ox + oy < playerX + playerY ? -1.4E-4F : -1.0E-4F;
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
            vbor.setTextureID(Texture.getWhite().getTextureId());
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
            vbor.cmdPopMatrix(5888);
        }
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        final ArrayList<FBORenderAreaHighlights.AreaHighlight> highlights = new ArrayList<>();
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
                FBORenderAreaHighlights.AreaHighlight ah = this.highlights.get(i);
                ah.render(playerX, playerY, playerZ, cx, cy);
            }

            VBORenderer vbor = VBORenderer.getInstance();
            vbor.flush();
            Core.getInstance().projectionMatrixStack.pop();
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderAreaHighlights.getInstance().drawerPool.release(this);
        }
    }
}
