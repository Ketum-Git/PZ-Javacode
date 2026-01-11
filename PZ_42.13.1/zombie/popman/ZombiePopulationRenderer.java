// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.MapCollisionData;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.ai.states.WalkTowardState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.opengl.VBORenderer;
import zombie.core.stash.StashSystem;
import zombie.core.textures.TextureDraw;
import zombie.input.Mouse;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclesDB2;

@UsedFromLua
public final class ZombiePopulationRenderer {
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    static final byte RENDER_RECT_FILLED = 0;
    static final byte RENDER_RECT_OUTLINE = 1;
    static final byte RENDER_LINE = 2;
    static final byte RENDER_CIRCLE = 3;
    static final byte RENDER_TEXT = 4;
    private final ZombiePopulationRenderer.Drawer[] drawers = new ZombiePopulationRenderer.Drawer[3];
    private ZombiePopulationRenderer.DrawerImpl currentDrawer;
    private final ZombiePopulationRenderer.DrawerImpl textDrawer = new ZombiePopulationRenderer.DrawerImpl();
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final ZombiePopulationRenderer.BooleanDebugOption cellGrid = new ZombiePopulationRenderer.BooleanDebugOption("CellGrid.256x256", true);
    private final ZombiePopulationRenderer.BooleanDebugOption cellGrid300 = new ZombiePopulationRenderer.BooleanDebugOption("CellGrid.300x300", true);
    private final ZombiePopulationRenderer.BooleanDebugOption cellInfo = new ZombiePopulationRenderer.BooleanDebugOption("CellInfo", true);
    private final ZombiePopulationRenderer.BooleanDebugOption metaGridBuildings = new ZombiePopulationRenderer.BooleanDebugOption("MetaGrid.Buildings", true);
    private final ZombiePopulationRenderer.BooleanDebugOption zombiesReal = new ZombiePopulationRenderer.BooleanDebugOption("Zombies.Real", true);
    private final ZombiePopulationRenderer.BooleanDebugOption zombiesStanding = new ZombiePopulationRenderer.BooleanDebugOption("Zombies.Standing", true);
    private final ZombiePopulationRenderer.BooleanDebugOption zombiesMoving = new ZombiePopulationRenderer.BooleanDebugOption("Zombies.Moving", true);
    private final ZombiePopulationRenderer.BooleanDebugOption mcdObstacles = new ZombiePopulationRenderer.BooleanDebugOption("MapCollisionData.Obstacles", true);
    private final ZombiePopulationRenderer.BooleanDebugOption mcdRegularChunkOutlines = new ZombiePopulationRenderer.BooleanDebugOption(
        "MapCollisionData.RegularChunkOutlines", true
    );
    private final ZombiePopulationRenderer.BooleanDebugOption mcdRooms = new ZombiePopulationRenderer.BooleanDebugOption("MapCollisionData.Rooms", true);
    private final ZombiePopulationRenderer.BooleanDebugOption vehicles = new ZombiePopulationRenderer.BooleanDebugOption("Vehicles", true);
    private final ZombiePopulationRenderer.BooleanDebugOption zombieIntensity = new ZombiePopulationRenderer.BooleanDebugOption("ZombieIntensity", false);

    private native void n_render(float arg0, int arg1, int arg2, float arg3, float arg4, int arg5, int arg6);

    private native void n_setWallFollowerStart(int arg0, int arg1);

    private native void n_setWallFollowerEnd(int arg0, int arg1);

    private native void n_wallFollowerMouseMove(int arg0, int arg1);

    private native void n_setDebugOption(String arg0, String arg1);

    public ZombiePopulationRenderer() {
        for (int i = 0; i < this.drawers.length; i++) {
            this.drawers[i] = new ZombiePopulationRenderer.Drawer();
        }
    }

    public float worldToScreenX(float x) {
        x -= this.xPos;
        x *= this.zoom;
        x += this.offx;
        return x + this.draww / 2.0F;
    }

    public float worldToScreenY(float y) {
        y -= this.yPos;
        y *= this.zoom;
        y += this.offy;
        return y + this.drawh / 2.0F;
    }

    public float uiToWorldX(float x) {
        x -= this.draww / 2.0F;
        x /= this.zoom;
        return x + this.xPos;
    }

    public float uiToWorldY(float y) {
        y -= this.drawh / 2.0F;
        y /= this.zoom;
        return y + this.yPos;
    }

    public void renderString(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.worldToScreenX(x);
        float ty = this.worldToScreenY(y);
        this.currentDrawer
            .renderRectFilledUI(
                tx - 2.0F,
                ty - 2.0F,
                (float)(TextManager.instance.MeasureStringX(UIFont.Small, str) + 4),
                (float)(TextManager.instance.font.getLineHeight() + 4),
                0.0F,
                0.0F,
                0.0F,
                0.75F
            );
        this.textDrawer.renderStringUI(tx, ty, str, r, g, b, a);
    }

    public void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float x1 = this.worldToScreenX(x);
        float y1 = this.worldToScreenY(y);
        float x2 = this.worldToScreenX(x + w);
        float y2 = this.worldToScreenY(y + h);
        w = x2 - x1;
        h = y2 - y1;
        if (!(x1 >= this.offx + this.draww) && !(x2 < this.offx) && !(y1 >= this.offy + this.drawh) && !(y2 < this.offy)) {
            this.currentDrawer.renderRectFilledUI(x1, y1, w, h, r, g, b, a);
        }
    }

    public void renderLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        float sx1 = this.worldToScreenX(x1);
        float sy1 = this.worldToScreenY(y1);
        float sx2 = this.worldToScreenX(x2);
        float sy2 = this.worldToScreenY(y2);
        if ((!(sx1 >= Core.getInstance().getScreenWidth()) || !(sx2 >= Core.getInstance().getScreenWidth()))
            && (!(sy1 >= Core.getInstance().getScreenHeight()) || !(sy2 >= Core.getInstance().getScreenHeight()))
            && (!(sx1 < 0.0F) || !(sx2 < 0.0F))
            && (!(sy1 < 0.0F) || !(sy2 < 0.0F))) {
            this.currentDrawer.renderLineUI(sx1, sy1, sx2, sy2, r, g, b, a);
        }
    }

    public void renderCircle(float x, float y, float radius, float r, float g, float b, float a) {
        byte segments = 32;
        double lx = x + radius * Math.cos(Math.toRadians(0.0));
        double ly = y + radius * Math.sin(Math.toRadians(0.0));

        for (int i = 1; i <= 32; i++) {
            double cx = x + radius * Math.cos(Math.toRadians(i * 360.0F / 32.0F));
            double cy = y + radius * Math.sin(Math.toRadians(i * 360.0F / 32.0F));
            int sx1 = (int)this.worldToScreenX((float)lx);
            int sy1 = (int)this.worldToScreenY((float)ly);
            int sx2 = (int)this.worldToScreenX((float)cx);
            int sy2 = (int)this.worldToScreenY((float)cy);
            this.currentDrawer.renderLineUI((float)sx1, (float)sy1, (float)sx2, (float)sy2, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    public void renderZombie(float x, float y, float r, float g, float b) {
        if (!(this.zoom < 0.2F)) {
            float zombieSize = 1.0F / this.zoom + 0.5F;
            this.renderRect(x - zombieSize / 2.0F, y - zombieSize / 2.0F, zombieSize, zombieSize, r, g, b, 1.0F);
        }
    }

    public void renderVehicle(int sqlid, float x, float y, float r, float g, float b) {
        float zombieSize = 2.0F / this.zoom + 0.5F;
        this.renderRect(x - zombieSize / 2.0F, y - zombieSize / 2.0F, zombieSize, zombieSize, r, g, b, 1.0F);
        this.renderString(x, y, String.format("%d", sqlid), r, g, b, 1.0);
    }

    public void outlineRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        this.renderLine(x, y, x + w, y, r, g, b, a);
        this.renderLine(x + w, y, x + w, y + h, r, g, b, a);
        this.renderLine(x, y + h, x + w, y + h, r, g, b, a);
        this.renderLine(x, y, x, y + h, r, g, b, a);
    }

    public void renderCellInfo(int cellX, int cellY, int effectivePopulation, int targetPopulation, float lastRepopTime) {
        if (this.cellInfo.getValue()) {
            float tx = this.worldToScreenX(cellX * 256) + 4.0F;
            float ty = this.worldToScreenY(cellY * 256) + 4.0F;
            String newLine = System.getProperty("line.separator");
            String str = Translator.getText("IGUI_ZombiePopulation_PopulationEffective")
                + ": "
                + effectivePopulation
                + newLine
                + Translator.getText("IGUI_ZombiePopulation_PopulationTarget")
                + ": "
                + targetPopulation;
            if (lastRepopTime > 0.0F) {
                str = str + newLine + Translator.getText("IGUI_ZombiePopulation_LastRepopTime") + ": " + String.format(" %.2f", lastRepopTime);
            }

            this.currentDrawer
                .renderRectFilledUI(
                    tx - 2.0F,
                    ty - 2.0F,
                    (float)(TextManager.instance.MeasureStringX(UIFont.Small, str) + 4),
                    (float)(TextManager.instance.MeasureStringY(UIFont.Small, str) + 4),
                    0.0F,
                    0.0F,
                    0.0F,
                    0.75F
                );
            this.textDrawer.renderStringUI(tx, ty, str, 1.0, 1.0, 1.0, 1.0);
        }
    }

    public void render(UIElement ui, float zoom, float xPos, float yPos) {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        this.currentDrawer = this.drawers[stateIndex].impl;
        this.currentDrawer.renderBuffer.clear();
        this.textDrawer.renderBuffer.clear();
        synchronized (MapCollisionData.instance.renderLock) {
            this._render(ui, zoom, xPos, yPos);
            this.currentDrawer.renderBuffer.flip();
            SpriteRenderer.instance.drawGeneric(this.drawers[stateIndex]);
        }

        this.renderAllText(ui);
    }

    private void renderAllText(UIElement ui) {
        ByteBuffer bb = this.textDrawer.renderBuffer;
        if (bb.position() != 0) {
            bb.flip();

            while (bb.position() < bb.limit()) {
                byte e = bb.get();
                switch (e) {
                    case 0: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        float w = bb.getFloat();
                        float h = bb.getFloat();
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        x = (float)(x - ui.getAbsoluteX());
                        y = (float)(y - ui.getAbsoluteY());
                        ui.DrawTextureScaledColor(null, (double)x, (double)y, (double)w, (double)h, (double)r, (double)g, (double)b, (double)a);
                        break;
                    }
                    case 4: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        String str = GameWindow.ReadStringUTF(bb);
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        TextManager.instance.DrawString(x, y, str, r, g, b, a);
                    }
                }
            }
        }
    }

    private void _render(UIElement ui, float zoom, float xPos, float yPos) {
        this.draww = ui.getWidth().intValue();
        this.drawh = ui.getHeight().intValue();
        this.xPos = xPos;
        this.yPos = yPos;
        this.offx = ui.getAbsoluteX().intValue();
        this.offy = ui.getAbsoluteY().intValue();
        this.zoom = zoom;
        IsoCell isoCell = IsoWorld.instance.currentCell;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[0];
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minCellX = (int)(this.uiToWorldX(0.0F) / 256.0F);
        int minCellY = (int)(this.uiToWorldY(0.0F) / 256.0F);
        int maxCellX = (int)(this.uiToWorldX(this.draww) / 256.0F) + 1;
        int maxCellY = (int)(this.uiToWorldY(this.drawh) / 256.0F) + 1;
        minCellX = PZMath.clamp(minCellX, metaGrid.getMinX(), metaGrid.getMaxX());
        minCellY = PZMath.clamp(minCellY, metaGrid.getMinY(), metaGrid.getMaxY());
        maxCellX = PZMath.clamp(maxCellX, metaGrid.getMinX(), metaGrid.getMaxX());
        maxCellY = PZMath.clamp(maxCellY, metaGrid.getMinY(), metaGrid.getMaxY());
        if (this.metaGridBuildings.getValue()) {
            for (int xx = minCellX; xx <= maxCellX; xx++) {
                for (int y = minCellY; y <= maxCellY; y++) {
                    if (metaGrid.hasCell(xx - metaGrid.minX, y - metaGrid.minY)) {
                        IsoMetaCell metaCell = metaGrid.getCell(xx - metaGrid.minX, y - metaGrid.minY);
                        if (metaCell != null) {
                            for (int n = 0; n < metaCell.buildings.size(); n++) {
                                BuildingDef def = metaCell.buildings.get(n);
                                boolean stash = StashSystem.isStashBuilding(def);
                                boolean spawn = SpawnPoints.instance.isSpawnBuilding(def);

                                for (int r = 0; r < def.rooms.size(); r++) {
                                    if (def.rooms.get(r).level <= 0) {
                                        ArrayList<RoomDef.RoomRect> rects = def.rooms.get(r).getRects();

                                        for (int rr = 0; rr < rects.size(); rr++) {
                                            RoomDef.RoomRect rect = rects.get(rr);
                                            if (stash && spawn) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.5F, 0.8F, 0.9F);
                                            } else if (stash) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.5F, 0.5F, 0.6F);
                                            } else if (def.alarmed) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.8F, 0.5F, 0.3F);
                                            } else if (spawn) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5F, 0.8F, 0.5F, 0.6F);
                                            } else {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5F, 0.5F, 0.8F, 0.3F);
                                            }
                                        }
                                    }
                                }

                                for (int rx = 0; rx < def.getEmptyOutside().size(); rx++) {
                                    if (def.getEmptyOutside().get(rx).level <= 0) {
                                        ArrayList<RoomDef.RoomRect> rects = def.getEmptyOutside().get(rx).getRects();

                                        for (int rrx = 0; rrx < rects.size(); rrx++) {
                                            RoomDef.RoomRect rect = rects.get(rrx);
                                            if (stash && spawn) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.5F, 0.8F, 0.9F);
                                            } else if (stash) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.5F, 0.5F, 0.6F);
                                            } else if (def.alarmed) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F, 0.8F, 0.5F, 0.3F);
                                            } else if (spawn) {
                                                this.renderRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5F, 0.8F, 0.5F, 0.6F);
                                            } else {
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
        }

        if (Core.debug) {
        }

        if (this.cellGrid.getValue()) {
            float cellSizeInPixels = 256.0F * zoom;
            int dxy = 1;

            while (cellSizeInPixels * dxy < 10.0F) {
                dxy += 2;
            }

            minCellX -= PZMath.coordmodulo(minCellX, dxy);
            minCellY -= PZMath.coordmodulo(minCellY, dxy);
            minCellX = PZMath.max(minCellX, IsoWorld.instance.metaGrid.minX);
            minCellY = PZMath.max(minCellY, IsoWorld.instance.metaGrid.minY);

            for (int yx = minCellY; yx <= maxCellY; yx += dxy) {
                this.renderLine(minCellX * 256, yx * 256, (maxCellX + 1) * 256, yx * 256, 1.0F, 1.0F, 1.0F, 0.15F);
            }

            for (int x = minCellX; x <= maxCellX; x += dxy) {
                this.renderLine(x * 256, minCellY * 256, x * 256, (maxCellY + 1) * 256, 1.0F, 1.0F, 1.0F, 0.15F);
            }
        }

        if (this.cellGrid300.getValue()) {
            int minCellX2 = (int)(this.uiToWorldX(0.0F) / 300.0F);
            int minCellY2 = (int)(this.uiToWorldY(0.0F) / 300.0F);
            int maxCellX2 = (int)(this.uiToWorldX(this.draww) / 300.0F) + 1;
            int maxCellY2 = (int)(this.uiToWorldY(this.drawh) / 300.0F) + 1;
            float cellSizeInPixels = 300.0F * zoom;
            int dxy = 1;

            while (cellSizeInPixels * dxy < 10.0F) {
                dxy += 2;
            }

            minCellX2 -= PZMath.coordmodulo(minCellX2, dxy);
            minCellY2 -= PZMath.coordmodulo(minCellY2, dxy);
            int minCell300X = PZMath.fastfloor(IsoWorld.instance.metaGrid.minX * 256.0F / 300.0F);
            int minCell300Y = PZMath.fastfloor(IsoWorld.instance.metaGrid.minY * 256.0F / 300.0F);
            int maxCell300X = PZMath.fastfloor((IsoWorld.instance.metaGrid.maxX + 1) * 256.0F / 300.0F) + 1;
            int maxCell300Y = PZMath.fastfloor((IsoWorld.instance.metaGrid.maxY + 1) * 256.0F / 300.0F) + 1;
            minCellX2 = PZMath.max(minCellX2, minCell300X);
            minCellY2 = PZMath.max(minCellY2, minCell300Y);
            maxCellX2 = PZMath.min(maxCellX2, maxCell300X);
            maxCellY2 = PZMath.min(maxCellY2, maxCell300Y);

            for (int yx = minCellY2; yx <= maxCellY2; yx += dxy) {
                this.renderLine(minCellX2 * 300, yx * 300, maxCellX2 * 300, yx * 300, 0.0F, 1.0F, 1.0F, 0.25F);
            }

            for (int x = minCellX2; x <= maxCellX2; x += dxy) {
                this.renderLine(x * 300, minCellY2 * 300, x * 300, maxCellY2 * 300, 0.0F, 1.0F, 1.0F, 0.25F);
            }

            this.outlineRect(
                minCell300X * 300, minCell300Y * 300, (maxCell300X - minCell300X) * 300, (maxCell300Y - minCell300Y) * 300, 0.0F, 1.0F, 1.0F, 0.25F
            );
        }

        boolean bShowZombieIntensity = this.zombieIntensity.getValue() && zoom > 0.5F;
        if (bShowZombieIntensity) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                    if (metaGrid.hasCell(cellX - metaGrid.minX, cellY - metaGrid.minY)) {
                        IsoMetaCell metaCell = metaGrid.getCell(cellX - metaGrid.minX, cellY - metaGrid.minY);
                        if (metaCell != null && metaCell.info != null) {
                            for (int chunkY = 0; chunkY < 32; chunkY++) {
                                for (int chunkX = 0; chunkX < 32; chunkX++) {
                                    int intensity = LotHeader.getZombieIntensityForChunk(metaCell.info, chunkX, chunkY);
                                    if (intensity > 0) {
                                        float intensityF = intensity / 255.0F;
                                        intensityF = PZMath.min(1.0F, intensityF + 0.1F);
                                        float g = 0.0F;
                                        float b = 0.0F;
                                        float a = 0.9F;
                                        this.renderRect(cellX * 256 + chunkX * 8, cellY * 256 + chunkY * 8, 8.0F, 8.0F, intensityF, 0.0F, 0.0F, 0.9F);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            double mouseX = Mouse.getXA() - ui.getAbsoluteX();
            double mouseY = Mouse.getYA() - ui.getAbsoluteY();
            float worldX = this.uiToWorldX((float)mouseX);
            float worldY = this.uiToWorldY((float)mouseY);
            int cellXx = PZMath.fastfloor(worldX / 256.0F);
            int cellY = PZMath.fastfloor(worldY / 256.0F);
            IsoMetaCell metaCell = metaGrid.getCellData(cellXx, cellY);
            if (metaCell != null && metaCell.info != null) {
                int chunkXx = (int)(worldX - cellXx * 256) / 8;
                int chunkY = (int)(worldY - cellY * 256) / 8;
                this.outlineRect(cellXx * 256 + chunkXx * 8, cellY * 256 + chunkY * 8, 8.0F, 8.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                int intensity = LotHeader.getZombieIntensityForChunk(metaCell.info, chunkXx, chunkY);
                intensity = PZMath.max(intensity, 0);
                String intensityStr = String.format(Translator.getText("IGUI_ZombiePopulation_Intensity") + ": %d", intensity);
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), 0);
                if (chunk != null) {
                    int total = 0;

                    for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); z++) {
                        IsoGridSquare[] squares = chunk.getSquaresForLevel(z);

                        for (int i = 0; i < 64; i++) {
                            IsoGridSquare square = squares[i];
                            if (square != null) {
                                for (int j = 0; j < square.getMovingObjects().size(); j++) {
                                    IsoMovingObject mov = square.getMovingObjects().get(j);
                                    if (mov instanceof IsoZombie) {
                                        total++;
                                    }
                                }
                            }
                        }
                    }

                    if (total > 0) {
                        intensityStr = intensityStr + "\n" + String.format(Translator.getText("IGUI_ZombiePopulation_Zombies") + ": %d", total);
                    }
                }

                float textX = this.worldToScreenX(cellXx * 256 + chunkXx * 8);
                float textY = this.worldToScreenY(cellY * 256 + chunkY * 8 + 8) + 1.0F;
                this.textDrawer
                    .renderRectFilledUI(
                        textX - 4.0F,
                        textY,
                        (float)(TextManager.instance.MeasureStringX(UIFont.Small, intensityStr) + 8),
                        (float)TextManager.instance.MeasureStringY(UIFont.Small, intensityStr),
                        0.0F,
                        0.0F,
                        0.0F,
                        0.75F
                    );
                this.textDrawer.renderStringUI(textX, textY, intensityStr, 1.0, 1.0, 1.0, 1.0);
            }
        }

        if (this.zombiesReal.getValue()) {
            for (int ix = 0; ix < IsoWorld.instance.currentCell.getZombieList().size(); ix++) {
                IsoZombie z = IsoWorld.instance.currentCell.getZombieList().get(ix);
                float red = 1.0F;
                float green = 1.0F;
                float blue = 0.0F;
                if (z.isReanimatedPlayer()) {
                    red = 0.0F;
                }

                this.renderZombie(z.getX(), z.getY(), red, 1.0F, 0.0F);
                if (z.getCurrentState() == WalkTowardState.instance()) {
                    this.renderLine(z.getX(), z.getY(), z.getPathTargetX(), z.getPathTargetY(), 1.0F, 1.0F, 1.0F, 0.5F);
                }
            }
        }

        for (int ix = 0; ix < IsoPlayer.numPlayers; ix++) {
            IsoPlayer player = IsoPlayer.players[ix];
            if (player != null) {
                this.renderZombie(player.getX(), player.getY(), 0.0F, 0.5F, 0.0F);
            }
        }

        if (GameClient.client) {
            MPDebugInfo.instance.render(this, zoom);
        } else {
            if (this.vehicles.getValue()) {
                VehiclesDB2.instance.renderDebug(this);
            }

            this.n_render(zoom, (int)this.offx, (int)this.offy, xPos, yPos, (int)this.draww, (int)this.drawh);
        }
    }

    public void setWallFollowerStart(int x, int y) {
        if (!GameClient.client) {
            this.n_setWallFollowerStart(x, y);
        }
    }

    public void setWallFollowerEnd(int x, int y) {
        if (!GameClient.client) {
            this.n_setWallFollowerEnd(x, y);
        }
    }

    public void wallFollowerMouseMove(int x, int y) {
        if (!GameClient.client) {
            this.n_wallFollowerMouseMove(x, y);
        }
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        if (this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption) {
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        return this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption ? booleanConfigOption.getValue() : false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "popman-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);

        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption option = this.options.get(i);
            this.n_setDebugOption(option.getName(), option.getValueAsString());
        }
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "popman-options.ini";
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); i++) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption != null) {
                    myOption.parse(configOption.getValueAsString());
                }
            }
        }

        for (int ix = 0; ix < this.options.size(); ix++) {
            ConfigOption option = this.options.get(ix);
            this.n_setDebugOption(option.getName(), option.getValueAsString());
        }
    }

    @UsedFromLua
    public class BooleanDebugOption extends BooleanConfigOption {
        public BooleanDebugOption(final String name, final boolean defaultValue) {
            Objects.requireNonNull(ZombiePopulationRenderer.this);
            super(name, defaultValue);
            ZombiePopulationRenderer.this.options.add(this);
        }
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        final ZombiePopulationRenderer.DrawerImpl impl = new ZombiePopulationRenderer.DrawerImpl();

        @Override
        public void render() {
            VBORenderer vbor = VBORenderer.getInstance();
            ByteBuffer bb = this.impl.renderBuffer;
            float z = 0.0F;

            while (bb.position() < bb.limit()) {
                byte e = bb.get();
                switch (e) {
                    case 0: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        float w = bb.getFloat();
                        float h = bb.getFloat();
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        vbor.startRun(vbor.formatPositionColor);
                        vbor.setMode(7);
                        vbor.addQuad(x, y, x + w, y + h, 0.0F, r, g, b, a);
                        vbor.endRun();
                    }
                    case 1:
                    case 3:
                    default:
                        break;
                    case 2: {
                        float x1 = bb.getFloat();
                        float y1 = bb.getFloat();
                        float x2 = bb.getFloat();
                        float y2 = bb.getFloat();
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                        vbor.startRun(vbor.formatPositionColor);
                        vbor.setMode(1);
                        vbor.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, a);
                        vbor.endRun();
                        break;
                    }
                    case 4: {
                        float x = bb.getFloat();
                        float y = bb.getFloat();
                        String str = GameWindow.ReadStringUTF(bb);
                        float r = bb.getFloat();
                        float g = bb.getFloat();
                        float b = bb.getFloat();
                        float a = bb.getFloat();
                    }
                }
            }

            vbor.flush();
        }
    }

    private static class DrawerImpl {
        ByteBuffer renderBuffer = ByteBuffer.allocate(1024);

        private void reserve(int nBytes) {
            if (this.renderBuffer.position() + nBytes > this.renderBuffer.capacity()) {
                ByteBuffer bb = ByteBuffer.allocate(this.renderBuffer.capacity() * 2);
                this.renderBuffer.flip();
                bb.put(this.renderBuffer);
                this.renderBuffer = bb;
            }
        }

        private void renderBufferByte(byte v) {
            this.reserve(1);
            this.renderBuffer.put(v);
        }

        private void renderBufferFloat(double v) {
            this.reserve(4);
            this.renderBuffer.putFloat((float)v);
        }

        private void renderBufferFloat(float v) {
            this.reserve(4);
            this.renderBuffer.putFloat(v);
        }

        private void renderBufferInt(int v) {
            this.reserve(4);
            this.renderBuffer.putInt(v);
        }

        private void renderBufferString(String v) {
            ByteBuffer utf = GameWindow.getEncodedBytesUTF(v);
            this.reserve(2 + utf.position());
            this.renderBuffer.putShort((short)utf.position());
            utf.flip();
            this.renderBuffer.put(utf);
        }

        private void renderLineUI(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
            this.renderBufferByte((byte)2);
            this.renderBufferFloat(x1);
            this.renderBufferFloat(y1);
            this.renderBufferFloat(x2);
            this.renderBufferFloat(y2);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }

        private void renderRectFilledUI(float x, float y, float w, float h, float r, float g, float b, float a) {
            this.renderBufferByte((byte)0);
            this.renderBufferFloat(x);
            this.renderBufferFloat(y);
            this.renderBufferFloat(w);
            this.renderBufferFloat(h);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }

        private void renderStringUI(float x, float y, String str, double r, double g, double b, double a) {
            this.renderBufferByte((byte)4);
            this.renderBufferFloat(x);
            this.renderBufferFloat(y);
            this.renderBufferString(str);
            this.renderBufferFloat(r);
            this.renderBufferFloat(g);
            this.renderBufferFloat(b);
            this.renderBufferFloat(a);
        }
    }
}
