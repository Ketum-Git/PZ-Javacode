// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.buildingRooms;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
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
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;

public final class BuildingRoomsDrawer extends TextureDraw.GenericDrawer {
    private static final int FLOATS_PER_RECT = 7;
    TFloatArrayList floats = new TFloatArrayList();
    int currentLevel;
    int highlightRectForDeletion = -1;

    void set(ArrayList<BREBuilding> buildings, BRERoom room, int level, int highlightRectForDeletion) {
        this.highlightRectForDeletion = -1;
        this.floats.resetQuick();
        BREBuilding selectedBuilding = room == null ? null : room.building;
        this.currentLevel = level;

        for (BREBuilding building : buildings) {
            for (BRERoom room1 : building.rooms) {
                if (room1.getLevel() == level) {
                    for (int rectIndex = 0; rectIndex < room1.roomDef.rects.size(); rectIndex++) {
                        RoomDef.RoomRect rect = room1.getRectangle(rectIndex);
                        if (room1 == room && highlightRectForDeletion == rectIndex) {
                            this.highlightRectForDeletion = this.floats.size() / 7;
                        }

                        this.floats.add(rect.x);
                        this.floats.add(rect.y);
                        this.floats.add(rect.w);
                        this.floats.add(rect.h);
                        this.floats.add(room1.getLevel());
                        this.floats.add(room1 == room ? 1.0F : 0.0F);
                        this.floats.add(building == selectedBuilding ? 1.0F : 0.0F);
                    }
                }
            }
        }
    }

    @Override
    public void render() {
        this.setMatrices();
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
        GL11.glEnable(2929);
        GL11.glDepthFunc(515);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(770, 771);
        VBORenderer vbor = VBORenderer.getInstance();
        float f = 0.05F;

        for (int i = 0; i < this.floats.size(); i += 7) {
            float x = this.floats.get(i);
            float y = this.floats.get(i + 1);
            float w = this.floats.get(i + 2);
            float h = this.floats.get(i + 3);
            int level = Math.round(this.floats.get(i + 4));
            boolean bSelected = Math.round(this.floats.get(i + 5)) != 0;
            boolean bSameBuilding = Math.round(this.floats.get(i + 6)) != 0;
            float r = 0.0F;
            float g = 1.0F;
            float b = 0.0F;
            float a = 0.2F;
            if (bSelected) {
                if (i == this.highlightRectForDeletion * 7) {
                    r = 1.0F;
                    g = 0.0F;
                    b = 0.0F;
                }
            } else {
                r = 0.0F;
                g = 0.0F;
                b = 1.0F;
            }

            if (!bSameBuilding) {
                b = 0.75F;
                g = 0.75F;
                r = 0.75F;
            }

            this.renderRect(playerX, playerY, playerZ, cx, cy, x + 0.05F, y + 0.05F, w - 0.1F, h - 0.1F, level, r, g, b, 0.2F);
        }

        for (int i = 0; i < this.floats.size(); i += 7) {
            float xx = this.floats.get(i);
            float yx = this.floats.get(i + 1);
            float wx = this.floats.get(i + 2);
            float hx = this.floats.get(i + 3);
            int levelx = Math.round(this.floats.get(i + 4));
            boolean bSelectedx = Math.round(this.floats.get(i + 5)) != 0;
            boolean bSameBuildingx = Math.round(this.floats.get(i + 6)) != 0;
            float rx = 0.0F;
            float gx = 1.0F;
            float bx = 0.0F;
            float ax = 1.0F;
            if (bSelectedx) {
                if (i == this.highlightRectForDeletion * 7) {
                    rx = 1.0F;
                    gx = 0.0F;
                    bx = 0.0F;
                }
            } else {
                rx = 0.0F;
                gx = 0.0F;
                bx = 1.0F;
            }

            if (!bSameBuildingx) {
                bx = 0.75F;
                gx = 0.75F;
                rx = 0.75F;
            }

            this.renderRectOutline(playerX, playerY, playerZ, cx, cy, xx + 0.05F, yx + 0.05F, wx - 0.1F, hx - 0.1F, levelx, rx, gx, bx, 1.0F);
        }

        vbor.flush();
        Core.getInstance().projectionMatrixStack.pop();
        GLStateRenderThread.restore();
    }

    private void setMatrices() {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        double screenWidth = camera.offscreenWidth / 1920.0F;
        double screenHeight = camera.offscreenHeight / 1920.0F;
        Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
        PROJECTION.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F);
        Core.getInstance().projectionMatrixStack.push(PROJECTION);
    }

    void renderRect(
        float playerX, float playerY, float playerZ, float cx, float cy, float x1, float y1, float w1, float h1, int level, float r, float g, float b, float a
    ) {
        VBORenderer vbor = VBORenderer.getInstance();
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 0.0F;
        float u2 = 1.0F;
        float v2 = 1.0F;
        float u3 = 0.0F;
        float v3 = 1.0F;
        float x2 = x1 + w1;
        float y2 = y1 + h1;
        float ox = (x1 + x2) / 2.0F;
        float oy = (y1 + y2) / 2.0F;
        float DEPTH_ADJUST = ox + oy < playerX + playerY ? -1.4E-4F : -1.0E-4F;
        float depth0 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), x1, y1, level).depthStart + DEPTH_ADJUST;
        float depth1 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), x2, y1, level).depthStart + DEPTH_ADJUST;
        float depth2 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), x2, y2, level).depthStart + DEPTH_ADJUST;
        float depth3 = IsoDepthHelper.getSquareDepthData(PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), x1, y2, level).depthStart + DEPTH_ADJUST;
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.scaling(Core.scale);
        MODELVIEW.scale(Core.tileScale / 2.0F);
        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = (level - playerZ) * 2.44949F;
        MODELVIEW.translate(-((float)difX), (float)difZ, -((float)difY));
        MODELVIEW.scale(-1.0F, 1.0F, -1.0F);
        MODELVIEW.translate(0.0F, -0.71999997F, 0.0F);
        vbor.cmdPushAndLoadMatrix(5888, MODELVIEW);
        vbor.startRun(vbor.formatPositionColorUvDepth);
        vbor.setMode(7);
        vbor.setDepthTest(false);
        vbor.setTextureID(Texture.getWhite().getTextureId());
        float z = 0.0F;
        vbor.addQuadDepth(
            x1 - ox,
            0.0F,
            y1 - oy,
            0.0F,
            0.0F,
            depth0,
            x2 - ox,
            0.0F,
            y1 - oy,
            1.0F,
            0.0F,
            depth1,
            x2 - ox,
            0.0F,
            y2 - oy,
            1.0F,
            1.0F,
            depth2,
            x1 - ox,
            0.0F,
            y2 - oy,
            0.0F,
            1.0F,
            depth3,
            r,
            g,
            b,
            a
        );
        vbor.endRun();
        vbor.cmdPopMatrix(5888);
    }

    void renderRectOutline(
        float playerX, float playerY, float playerZ, float cx, float cy, float x1, float y1, float w1, float h1, int level, float r, float g, float b, float a
    ) {
        VBORenderer vbor = VBORenderer.getInstance();
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 0.0F;
        float u2 = 1.0F;
        float v2 = 1.0F;
        float u3 = 0.0F;
        float v3 = 1.0F;
        float x2 = x1 + w1;
        float y2 = y1 + h1;
        float ox = (x1 + x2) / 2.0F;
        float oy = (y1 + y2) / 2.0F;
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.scaling(Core.scale);
        MODELVIEW.scale(Core.tileScale / 2.0F);
        MODELVIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = (level - playerZ) * 2.44949F;
        MODELVIEW.translate(-((float)difX), (float)difZ, -((float)difY));
        MODELVIEW.scale(-1.0F, 1.0F, -1.0F);
        MODELVIEW.translate(0.0F, -0.71999997F, 0.0F);
        vbor.cmdPushAndLoadMatrix(5888, MODELVIEW);
        vbor.startRun(vbor.formatPositionColorUvDepth);
        vbor.setMode(1);
        vbor.setDepthTest(false);
        vbor.setTextureID(Texture.getWhite().getTextureId());
        float z = 0.0F;
        vbor.addQuad(
            x1 - ox,
            0.0F,
            y1 - oy,
            0.0F,
            0.0F,
            x2 - ox,
            0.0F,
            y1 - oy,
            1.0F,
            0.0F,
            x2 - ox,
            0.0F,
            y2 - oy,
            1.0F,
            1.0F,
            x1 - ox,
            0.0F,
            y2 - oy,
            0.0F,
            1.0F,
            r,
            g,
            b,
            a
        );
        vbor.endRun();
        vbor.cmdPopMatrix(5888);
    }

    @Override
    public void postRender() {
        BuildingRoomsEditor.getInstance().drawerPool.release(this);
    }
}
