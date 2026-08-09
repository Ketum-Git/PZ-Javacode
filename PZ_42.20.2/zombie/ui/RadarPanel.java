// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.network.GameClient;
import zombie.popman.ZombiePopulationManager;

@UsedFromLua
public final class RadarPanel extends UIElement {
    private final int playerIndex;
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    private final Texture mask;
    private final Texture border;
    private final ArrayList<RadarPanel.ZombiePos> zombiePos = new ArrayList<>();
    private final RadarPanel.ZombiePosPool zombiePosPool = new RadarPanel.ZombiePosPool();
    private int zombiePosFrameCount;
    private final boolean[] zombiePosOccupied = new boolean[360];

    public RadarPanel(int playerIndex) {
        this.setX(IsoCamera.getScreenLeft(playerIndex) + 20);
        this.setY(IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex) - 120 - 20);
        this.setWidth(120.0);
        this.setHeight(120.0);
        this.mask = Texture.getSharedTexture("media/ui/RadarMask.png");
        this.border = Texture.getSharedTexture("media/ui/RadarBorder.png");
        this.playerIndex = playerIndex;
    }

    @Override
    public void update() {
        int dy = 0;
        if (IsoPlayer.players[this.playerIndex] != null && IsoPlayer.players[this.playerIndex].getJoypadBind() != -1) {
            dy = -72;
        }

        this.setX(IsoCamera.getScreenLeft(this.playerIndex) + 20);
        this.setY(IsoCamera.getScreenTop(this.playerIndex) + IsoCamera.getScreenHeight(this.playerIndex) - this.getHeight() - 20.0 + dy);
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (IsoPlayer.players[this.playerIndex] != null) {
                if (!GameClient.client) {
                    this.draww = this.getWidth().intValue();
                    this.drawh = this.getHeight().intValue();
                    this.xPos = IsoPlayer.players[this.playerIndex].getX();
                    this.yPos = IsoPlayer.players[this.playerIndex].getY();
                    this.offx = this.getAbsoluteX().intValue();
                    this.offy = this.getAbsoluteY().intValue();
                    this.zoom = 3.0F;
                    this.stencilOn();
                    SpriteRenderer.instance.render(null, this.offx, this.offy, this.getWidth().intValue(), this.drawh, 0.0F, 0.2F, 0.0F, 0.66F, null);
                    this.renderBuildings();
                    this.renderRect(this.xPos - 0.5F, this.yPos - 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    this.stencilOff();
                    this.renderZombies();
                    SpriteRenderer.instance
                        .render(this.border, this.offx - 4.0F, this.offy - 4.0F, this.draww + 8.0F, this.drawh + 8.0F, 1.0F, 1.0F, 1.0F, 0.25F, null);
                }
            }
        }
    }

    private void stencilOn() {
        IndieGL.glStencilMask(255);
        IndieGL.glClear(1280);
        IndieGL.enableStencilTest();
        IndieGL.glStencilFunc(519, 128, 255);
        IndieGL.glStencilOp(7680, 7680, 7681);
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc(516, 0.1F);
        IndieGL.glColorMask(false, false, false, false);
        SpriteRenderer.instance.renderi(this.mask, (int)this.x, (int)this.y, (int)this.width, (int)this.height, 1.0F, 1.0F, 1.0F, 1.0F, null);
        IndieGL.glColorMask(true, true, true, true);
        IndieGL.glAlphaFunc(516, 0.0F);
        IndieGL.glStencilFunc(514, 128, 128);
        IndieGL.glStencilOp(7680, 7680, 7680);
    }

    private void stencilOff() {
        IndieGL.glAlphaFunc(519, 0.0F);
        IndieGL.disableStencilTest();
        IndieGL.disableAlphaTest();
        IndieGL.glStencilFunc(519, 255, 255);
        IndieGL.glStencilOp(7680, 7680, 7680);
        IndieGL.glClear(1280);
    }

    private void renderBuildings() {
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minX = (int)((this.xPos - 100.0F) / 256.0F) - metaGrid.minX;
        int minY = (int)((this.yPos - 100.0F) / 256.0F) - metaGrid.minY;
        int maxX = (int)((this.xPos + 100.0F) / 256.0F) - metaGrid.minX;
        int maxY = (int)((this.yPos + 100.0F) / 256.0F) - metaGrid.minY;
        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(maxX, metaGrid.gridX() - 1);
        maxY = Math.min(maxY, metaGrid.gridY() - 1);

        for (int xx = minX; xx <= maxX; xx++) {
            for (int y = minY; y <= maxY; y++) {
                if (metaGrid.hasCell(xx, y)) {
                    IsoMetaCell metaCell = metaGrid.getCell(xx, y);
                    if (metaCell != null) {
                        for (int n = 0; n < metaCell.buildings.size(); n++) {
                            BuildingDef def = metaCell.buildings.get(n);

                            for (int r = 0; r < def.rooms.size(); r++) {
                                if (def.rooms.get(r).level <= 0) {
                                    ArrayList<RoomDef.RoomRect> rects = def.rooms.get(r).getRects();

                                    for (int rr = 0; rr < rects.size(); rr++) {
                                        RoomDef.RoomRect rect = rects.get(rr);
                                        this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5F, 0.5F, 0.8F, 0.3F);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderZombies() {
        float x0 = this.offx + this.draww / 2.0F;
        float y0 = this.offy + this.drawh / 2.0F;
        float radius = this.draww / 2.0F;
        float screenWH = 0.5F * this.zoom;
        if (++this.zombiePosFrameCount >= PerformanceSettings.getLockFPS() / 5) {
            this.zombiePosFrameCount = 0;
            this.zombiePosPool.release(this.zombiePos);
            this.zombiePos.clear();
            Arrays.fill(this.zombiePosOccupied, false);
            ArrayList<IsoZombie> zombies = IsoWorld.instance.currentCell.getZombieList();

            for (int i = 0; i < zombies.size(); i++) {
                IsoZombie zombie = zombies.get(i);
                float x1 = this.worldToScreenX(zombie.getX());
                float y1 = this.worldToScreenY(zombie.getY());
                float dist = IsoUtils.DistanceToSquared(x0, y0, x1, y1);
                if (dist > radius * radius) {
                    double radians = Math.atan2(y1 - y0, x1 - x0) + Math.PI;
                    double degrees = (Math.toDegrees(radians) + 180.0) % 360.0;
                    this.zombiePosOccupied[(int)degrees] = true;
                } else {
                    this.zombiePos.add(this.zombiePosPool.alloc(zombie.getX(), zombie.getY()));
                }
            }

            if (Core.lastStand) {
                if (ZombiePopulationManager.instance.radarXy == null) {
                    ZombiePopulationManager.instance.radarXy = new float[2048];
                }

                float[] zpos = ZombiePopulationManager.instance.radarXy;
                synchronized (zpos) {
                    for (int ix = 0; ix < ZombiePopulationManager.instance.radarCount; ix++) {
                        float zx = zpos[ix * 2 + 0];
                        float zy = zpos[ix * 2 + 1];
                        float x1 = this.worldToScreenX(zx);
                        float y1 = this.worldToScreenY(zy);
                        float dist = IsoUtils.DistanceToSquared(x0, y0, x1, y1);
                        if (dist > radius * radius) {
                            double radians = Math.atan2(y1 - y0, x1 - x0) + Math.PI;
                            double degrees = (Math.toDegrees(radians) + 180.0) % 360.0;
                            this.zombiePosOccupied[(int)degrees] = true;
                        } else {
                            this.zombiePos.add(this.zombiePosPool.alloc(zx, zy));
                        }
                    }

                    ZombiePopulationManager.instance.radarRenderFlag = true;
                }
            }
        }

        int size = this.zombiePos.size();

        for (int ixx = 0; ixx < size; ixx++) {
            RadarPanel.ZombiePos zpos = this.zombiePos.get(ixx);
            this.renderRect(zpos.x - 0.5F, zpos.y - 0.5F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        }

        for (int ixx = 0; ixx < this.zombiePosOccupied.length; ixx++) {
            if (this.zombiePosOccupied[ixx]) {
                double radians = Math.toRadians((float)ixx / this.zombiePosOccupied.length * 360.0F);
                SpriteRenderer.instance
                    .render(
                        null,
                        x0 + (radius + 1.0F) * (float)Math.cos(radians) - screenWH,
                        y0 + (radius + 1.0F) * (float)Math.sin(radians) - screenWH,
                        1.0F * this.zoom,
                        1.0F * this.zoom,
                        1.0F,
                        1.0F,
                        0.0F,
                        1.0F,
                        null
                    );
            }
        }
    }

    private float worldToScreenX(float x) {
        x -= this.xPos;
        x *= this.zoom;
        x += this.offx;
        return x + this.draww / 2.0F;
    }

    private float worldToScreenY(float y) {
        y -= this.yPos;
        y *= this.zoom;
        y += this.offy;
        return y + this.drawh / 2.0F;
    }

    private void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float x1 = this.worldToScreenX(x);
        float y1 = this.worldToScreenY(y);
        float x2 = this.worldToScreenX(x + w);
        float y2 = this.worldToScreenY(y + h);
        w = x2 - x1;
        h = y2 - y1;
        if (!(x1 >= this.offx + this.draww) && !(x2 < this.offx) && !(y1 >= this.offy + this.drawh) && !(y2 < this.offy)) {
            SpriteRenderer.instance.render(null, x1, y1, w, h, r, g, b, a, null);
        }
    }

    private static final class ZombiePos {
        public float x;
        public float y;

        public ZombiePos(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public RadarPanel.ZombiePos set(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }
    }

    private static class ZombiePosPool {
        private final ArrayDeque<RadarPanel.ZombiePos> pool = new ArrayDeque<>();

        public RadarPanel.ZombiePos alloc(float x, float y) {
            return this.pool.isEmpty() ? new RadarPanel.ZombiePos(x, y) : this.pool.pop().set(x, y);
        }

        public void release(Collection<RadarPanel.ZombiePos> other) {
            this.pool.addAll(other);
        }
    }
}
