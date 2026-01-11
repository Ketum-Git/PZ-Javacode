// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import zombie.MapCollisionData;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.utils.Bits;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.objects.IsoThumpable;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;

/**
 * TurboTuTone.
 *  Base functionality copied from ZombiePopulationRenderer
 */
@UsedFromLua
public class IsoRegionsRenderer {
    private final List<DataChunk> tempChunkList = new ArrayList<>();
    private final List<String> debugLines = new ArrayList<>();
    private float xPos;
    private float yPos;
    private float offx;
    private float offy;
    private float zoom;
    private float draww;
    private float drawh;
    private boolean hasSelected;
    private boolean validSelection;
    private int selectedX;
    private int selectedY;
    private int selectedZ;
    private final HashSet<Integer> drawnCells = new HashSet<>();
    private boolean editSquareInRange;
    private int editSquareX;
    private int editSquareY;
    private final ArrayList<ConfigOption> editOptions = new ArrayList<>();
    private boolean editingEnabled;
    private final IsoRegionsRenderer.BooleanDebugOption editWallN = new IsoRegionsRenderer.BooleanDebugOption(this.editOptions, "Edit.WallN", false);
    private final IsoRegionsRenderer.BooleanDebugOption editWallW = new IsoRegionsRenderer.BooleanDebugOption(this.editOptions, "Edit.WallW", false);
    private final IsoRegionsRenderer.BooleanDebugOption editDoorN = new IsoRegionsRenderer.BooleanDebugOption(this.editOptions, "Edit.DoorN", false);
    private final IsoRegionsRenderer.BooleanDebugOption editDoorW = new IsoRegionsRenderer.BooleanDebugOption(this.editOptions, "Edit.DoorW", false);
    private final IsoRegionsRenderer.BooleanDebugOption editFloor = new IsoRegionsRenderer.BooleanDebugOption(this.editOptions, "Edit.Floor", false);
    private final ArrayList<ConfigOption> zLevelOptions = new ArrayList<>();
    private final IsoRegionsRenderer.BooleanDebugOption zLevelPlayer = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.Player", true);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel0 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.0", false, 0);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel1 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.1", false, 1);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel2 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.2", false, 2);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel3 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.3", false, 3);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel4 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.4", false, 4);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel5 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.5", false, 5);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel6 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.6", false, 6);
    private final IsoRegionsRenderer.BooleanDebugOption zLevel7 = new IsoRegionsRenderer.BooleanDebugOption(this.zLevelOptions, "zLevel.7", false, 7);
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final IsoRegionsRenderer.BooleanDebugOption cellGrid = new IsoRegionsRenderer.BooleanDebugOption(this.options, "CellGrid", true);
    private final IsoRegionsRenderer.BooleanDebugOption metaGridBuildings = new IsoRegionsRenderer.BooleanDebugOption(this.options, "MetaGrid.Buildings", true);
    private final IsoRegionsRenderer.BooleanDebugOption isoRegionRender = new IsoRegionsRenderer.BooleanDebugOption(this.options, "IsoRegion.Render", true);
    private final IsoRegionsRenderer.BooleanDebugOption isoRegionRenderChunks = new IsoRegionsRenderer.BooleanDebugOption(
        this.options, "IsoRegion.RenderChunks", false
    );
    private final IsoRegionsRenderer.BooleanDebugOption isoRegionRenderChunksPlus = new IsoRegionsRenderer.BooleanDebugOption(
        this.options, "IsoRegion.RenderChunksPlus", false
    );

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

    public void renderStringUI(float x, float y, String str, Color c) {
        this.renderStringUI(x, y, str, c.r, c.g, c.b, c.a);
    }

    public void renderStringUI(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.offx + x;
        float ty = this.offy + y;
        SpriteRenderer.instance
            .render(
                null,
                tx - 2.0F,
                ty - 2.0F,
                TextManager.instance.MeasureStringX(UIFont.Small, str) + 4,
                TextManager.instance.font.getLineHeight() + 4,
                0.0F,
                0.0F,
                0.0F,
                0.75F,
                null
            );
        TextManager.instance.DrawString(tx, ty, str, r, g, b, a);
    }

    public void renderString(float x, float y, String str, double r, double g, double b, double a) {
        float tx = this.worldToScreenX(x);
        float ty = this.worldToScreenY(y);
        SpriteRenderer.instance
            .render(
                null,
                tx - 2.0F,
                ty - 2.0F,
                TextManager.instance.MeasureStringX(UIFont.Small, str) + 4,
                TextManager.instance.font.getLineHeight() + 4,
                0.0F,
                0.0F,
                0.0F,
                0.75F,
                null
            );
        TextManager.instance.DrawString(tx, ty, str, r, g, b, a);
    }

    public void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
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

    public void renderLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        float sx1 = this.worldToScreenX(x1);
        float sy1 = this.worldToScreenY(y1);
        float sx2 = this.worldToScreenX(x2);
        float sy2 = this.worldToScreenY(y2);
        if ((!(sx1 >= Core.getInstance().getScreenWidth()) || !(sx2 >= Core.getInstance().getScreenWidth()))
            && (!(sy1 >= Core.getInstance().getScreenHeight()) || !(sy2 >= Core.getInstance().getScreenHeight()))
            && (!(sx1 < 0.0F) || !(sx2 < 0.0F))
            && (!(sy1 < 0.0F) || !(sy2 < 0.0F))) {
            SpriteRenderer.instance.renderline(null, (int)sx1, (int)sy1, (int)sx2, (int)sy2, r, g, b, a);
        }
    }

    public void outlineRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        this.renderLine(x, y, x + w, y, r, g, b, a);
        this.renderLine(x + w, y, x + w, y + h, r, g, b, a);
        this.renderLine(x, y + h, x + w, y + h, r, g, b, a);
        this.renderLine(x, y, x, y + h, r, g, b, a);
    }

    public void renderCellInfo(int cellX, int cellY, int effectivePopulation, int targetPopulation, float lastRepopTime) {
        float tx = this.worldToScreenX(cellX * 256) + 4.0F;
        float ty = this.worldToScreenY(cellY * 256) + 4.0F;
        String str = effectivePopulation + " / " + targetPopulation;
        if (lastRepopTime > 0.0F) {
            str = str + String.format(" %.2f", lastRepopTime);
        }

        SpriteRenderer.instance
            .render(
                null,
                tx - 2.0F,
                ty - 2.0F,
                TextManager.instance.MeasureStringX(UIFont.Small, str) + 4,
                TextManager.instance.font.getLineHeight() + 4,
                0.0F,
                0.0F,
                0.0F,
                0.75F,
                null
            );
        TextManager.instance.DrawString(tx, ty, str, 1.0, 1.0, 1.0, 1.0);
    }

    public void renderZombie(float x, float y, float r, float g, float b) {
        float zombieSize = 1.0F / this.zoom + 0.5F;
        this.renderRect(x - zombieSize / 2.0F, y - zombieSize / 2.0F, zombieSize, zombieSize, r, g, b, 1.0F);
    }

    public void renderSquare(float x, float y, float r, float g, float b, float alpha) {
        float zombieSize = 1.0F;
        this.renderRect(x, y, 1.0F, 1.0F, r, g, b, alpha);
    }

    public void renderEntity(float size, float x, float y, float r, float g, float b, float a) {
        float zombieSize = size / this.zoom + 0.5F;
        this.renderRect(x - zombieSize / 2.0F, y - zombieSize / 2.0F, zombieSize, zombieSize, r, g, b, a);
    }

    public void render(UIElement ui, float zoom, float xPos, float yPos) {
        synchronized (MapCollisionData.instance.renderLock) {
            this._render(ui, zoom, xPos, yPos);
        }
    }

    private void debugLine(String str) {
        this.debugLines.add(str);
    }

    public void recalcSurroundings() {
        IsoRegions.forceRecalcSurroundingChunks();
    }

    public boolean hasChunkRegion(int x, int y) {
        int z = this.getZLevel();
        DataRoot root = IsoRegions.getDataRoot();
        return root.getIsoChunkRegion(x, y, z) != null;
    }

    public IsoChunkRegion getChunkRegion(int x, int y) {
        int z = this.getZLevel();
        DataRoot root = IsoRegions.getDataRoot();
        return root.getIsoChunkRegion(x, y, z);
    }

    public void setSelected(int x, int y) {
        this.setSelectedWorld((int)this.uiToWorldX(x), (int)this.uiToWorldY(y));
    }

    public void setSelectedWorld(int x, int y) {
        this.selectedZ = this.getZLevel();
        this.hasSelected = true;
        this.selectedX = x;
        this.selectedY = y;
    }

    public void unsetSelected() {
        this.hasSelected = false;
    }

    public boolean isHasSelected() {
        return this.hasSelected;
    }

    private void _render(UIElement ui, float zoom, float xPos, float yPos) {
        this.debugLines.clear();
        this.drawnCells.clear();
        this.draww = ui.getWidth().intValue();
        this.drawh = ui.getHeight().intValue();
        this.xPos = xPos;
        this.yPos = yPos;
        this.offx = ui.getAbsoluteX().intValue();
        this.offy = ui.getAbsoluteY().intValue();
        this.zoom = zoom;
        this.debugLine("Zoom: " + zoom);
        this.debugLine("zLevel: " + this.getZLevel());
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        int minCellX = (int)(this.uiToWorldX(0.0F) / 256.0F) - metaGrid.minX;
        int minCellY = (int)(this.uiToWorldY(0.0F) / 256.0F) - metaGrid.minY;
        int maxCellX = (int)(this.uiToWorldX(this.draww) / 256.0F) + 1 - metaGrid.minX;
        int maxCellY = (int)(this.uiToWorldY(this.drawh) / 256.0F) + 1 - metaGrid.minY;
        minCellX = PZMath.clamp(minCellX, 0, metaGrid.getWidth() - 1);
        minCellY = PZMath.clamp(minCellY, 0, metaGrid.getHeight() - 1);
        maxCellX = PZMath.clamp(maxCellX, 0, metaGrid.getWidth() - 1);
        maxCellY = PZMath.clamp(maxCellY, 0, metaGrid.getHeight() - 1);
        float cc = Math.max(1.0F - zoom / 2.0F, 0.1F);
        IsoChunkRegion selChunkRegion = null;
        IsoWorldRegion selWorldRegion = null;
        this.validSelection = false;
        if (this.isoRegionRender.getValue()) {
            IsoPlayer player = IsoPlayer.getInstance();
            DataRoot root = IsoRegions.getDataRoot();
            this.tempChunkList.clear();
            root.getAllChunks(this.tempChunkList);
            this.debugLine("DataChunks: " + this.tempChunkList.size());
            this.debugLine("IsoChunkRegions: " + root.regionManager.getChunkRegionCount());
            this.debugLine("IsoWorldRegions: " + root.regionManager.getWorldRegionCount());
            if (this.hasSelected) {
                selChunkRegion = root.getIsoChunkRegion(this.selectedX, this.selectedY, this.selectedZ);
                selWorldRegion = root.getIsoWorldRegion(this.selectedX, this.selectedY, this.selectedZ);
                if (selWorldRegion != null
                    && !selWorldRegion.isEnclosed()
                    && (!this.isoRegionRenderChunks.getValue() || !this.isoRegionRenderChunksPlus.getValue())) {
                    selWorldRegion = null;
                    selChunkRegion = null;
                }

                if (selChunkRegion != null) {
                    this.validSelection = true;
                }
            }

            for (int i = 0; i < this.tempChunkList.size(); i++) {
                DataChunk chunk = this.tempChunkList.get(i);
                int cWX = chunk.getChunkX() * 8;
                int cWY = chunk.getChunkY() * 8;
                if (zoom > 0.1F) {
                    float x1 = this.worldToScreenX(cWX);
                    float y1 = this.worldToScreenY(cWY);
                    float x2 = this.worldToScreenX(cWX + 8);
                    float y2 = this.worldToScreenY(cWY + 8);
                    if (!(x1 >= this.offx + this.draww) && !(x2 < this.offx) && !(y1 >= this.offy + this.drawh) && !(y2 < this.offy)) {
                        this.renderRect(cWX, cWY, 8.0F, 8.0F, 0.0F, cc, 0.0F, 1.0F);
                    }
                }
            }
        }

        if (this.metaGridBuildings.getValue()) {
            float alphaMod = PZMath.clamp(0.3F * (zoom / 5.0F), 0.15F, 0.3F);

            for (int xx = minCellX; xx < maxCellX; xx++) {
                for (int y = minCellY; y < maxCellY; y++) {
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
                                            if (def.alarmed) {
                                                this.renderRect(
                                                    rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.8F * alphaMod, 0.8F * alphaMod, 0.5F * alphaMod, 1.0F
                                                );
                                            } else {
                                                this.renderRect(
                                                    rect.getX(), rect.getY(), rect.getW(), rect.getH(), 0.5F * alphaMod, 0.5F * alphaMod, 0.8F * alphaMod, 1.0F
                                                );
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

        if (this.isoRegionRender.getValue()) {
            int zLevel = this.getZLevel();
            DataRoot root = IsoRegions.getDataRoot();
            this.tempChunkList.clear();
            root.getAllChunks(this.tempChunkList);
            float renderAlpha = 1.0F;

            for (int ix = 0; ix < this.tempChunkList.size(); ix++) {
                DataChunk chunk = this.tempChunkList.get(ix);
                int cWX = chunk.getChunkX() * 8;
                int cWY = chunk.getChunkY() * 8;
                if (zoom <= 0.1F) {
                    int cellx = cWX / 256;
                    int celly = cWY / 256;
                    int cellID = IsoRegions.hash(cellx, celly);
                    if (!this.drawnCells.contains(cellID)) {
                        this.drawnCells.add(cellID);
                        this.renderRect(cellx * 256, celly * 256, 256.0F, 256.0F, 0.0F, cc, 0.0F, 1.0F);
                    }
                } else if (!(zoom < 1.0F)) {
                    float x1 = this.worldToScreenX(cWX);
                    float y1 = this.worldToScreenY(cWY);
                    float x2 = this.worldToScreenX(cWX + 8);
                    float y2 = this.worldToScreenY(cWY + 8);
                    if (!(x1 >= this.offx + this.draww) && !(x2 < this.offx) && !(y1 >= this.offy + this.drawh) && !(y2 < this.offy)) {
                        for (int x = 0; x < 8; x++) {
                            for (int yx = 0; yx < 8; yx++) {
                                int minZ = zLevel > 0 ? zLevel - 1 : zLevel;

                                for (int z = minZ; z <= zLevel; z++) {
                                    float alphaMod = z < zLevel ? 0.25F : 1.0F;
                                    byte flags = chunk.getSquare(x, yx, z);
                                    if (flags >= 0) {
                                        IsoChunkRegion isoChunkRegion = chunk.getIsoChunkRegion(x, yx, z);
                                        if (isoChunkRegion != null) {
                                            if (zoom > 6.0F && this.isoRegionRenderChunks.getValue() && this.isoRegionRenderChunksPlus.getValue()) {
                                                Color col = isoChunkRegion.getColor();
                                                renderAlpha = 1.0F;
                                                if (selChunkRegion != null && isoChunkRegion != selChunkRegion) {
                                                    renderAlpha = 0.25F;
                                                }

                                                this.renderSquare(cWX + x, cWY + yx, col.r, col.g, col.b, renderAlpha * alphaMod);
                                            } else {
                                                IsoWorldRegion isoWorldRegion = isoChunkRegion.getIsoWorldRegion();
                                                if (isoWorldRegion != null && isoWorldRegion.isEnclosed()) {
                                                    renderAlpha = 1.0F;
                                                    Color col;
                                                    if (this.isoRegionRenderChunks.getValue()) {
                                                        col = isoChunkRegion.getColor();
                                                        if (selChunkRegion != null && isoChunkRegion != selChunkRegion) {
                                                            renderAlpha = 0.25F;
                                                        }
                                                    } else {
                                                        col = isoWorldRegion.getColor();
                                                        if (selWorldRegion != null && isoWorldRegion != selWorldRegion) {
                                                            renderAlpha = 0.25F;
                                                        }
                                                    }

                                                    this.renderSquare(cWX + x, cWY + yx, col.r, col.g, col.b, renderAlpha * alphaMod);
                                                }
                                            }
                                        }

                                        if (z > 0 && z == zLevel) {
                                            isoChunkRegion = chunk.getIsoChunkRegion(x, yx, z);
                                            IsoWorldRegion isoWorldRegion = isoChunkRegion != null ? isoChunkRegion.getIsoWorldRegion() : null;
                                            boolean test = isoChunkRegion == null || isoWorldRegion == null || !isoWorldRegion.isEnclosed();
                                            if (test && Bits.hasFlags(flags, 16)) {
                                                this.renderSquare(cWX + x, cWY + yx, 0.5F, 0.5F, 0.5F, 1.0F);
                                            }
                                        }

                                        if (Bits.hasFlags(flags, 1) || Bits.hasFlags(flags, 4)) {
                                            this.renderRect(cWX + x, cWY + yx, 1.0F, 0.1F, 1.0F, 1.0F, 1.0F, 1.0F * alphaMod);
                                        }

                                        if (Bits.hasFlags(flags, 2) || Bits.hasFlags(flags, 8)) {
                                            this.renderRect(cWX + x, cWY + yx, 0.1F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F * alphaMod);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (this.cellGrid.getValue()) {
            float lineAlphaMod = 1.0F;
            if (zoom < 0.1F) {
                lineAlphaMod = Math.max(zoom / 0.1F, 0.25F);
            }

            for (int yx = minCellY; yx <= maxCellY; yx++) {
                this.renderLine(
                    metaGrid.minX * 256,
                    (metaGrid.minY + yx) * 256,
                    (metaGrid.maxX + 1) * 256,
                    (metaGrid.minY + yx) * 256,
                    1.0F,
                    1.0F,
                    1.0F,
                    0.15F * lineAlphaMod
                );
                if (zoom > 1.0F) {
                    for (int ixx = 1; ixx < 32; ixx++) {
                        this.renderLine(
                            metaGrid.minX * 256,
                            (metaGrid.minY + yx) * 256 + ixx * 8,
                            (metaGrid.maxX + 1) * 256,
                            (metaGrid.minY + yx) * 256 + ixx * 8,
                            1.0F,
                            1.0F,
                            1.0F,
                            0.0325F
                        );
                    }
                } else if (zoom > 0.15F) {
                    this.renderLine(
                        metaGrid.minX * 256,
                        (metaGrid.minY + yx) * 256 + 100,
                        (metaGrid.maxX + 1) * 256,
                        (metaGrid.minY + yx) * 256 + 100,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.075F
                    );
                    this.renderLine(
                        metaGrid.minX * 256,
                        (metaGrid.minY + yx) * 256 + 200,
                        (metaGrid.maxX + 1) * 256,
                        (metaGrid.minY + yx) * 256 + 200,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.075F
                    );
                }
            }

            for (int x = minCellX; x <= maxCellX; x++) {
                this.renderLine(
                    (metaGrid.minX + x) * 256,
                    metaGrid.minY * 256,
                    (metaGrid.minX + x) * 256,
                    (metaGrid.maxY + 1) * 256,
                    1.0F,
                    1.0F,
                    1.0F,
                    0.15F * lineAlphaMod
                );
                if (zoom > 1.0F) {
                    for (int ixx = 1; ixx < 32; ixx++) {
                        this.renderLine(
                            (metaGrid.minX + x) * 256 + ixx * 8,
                            metaGrid.minY * 256,
                            (metaGrid.minX + x) * 256 + ixx * 8,
                            (metaGrid.maxY + 1) * 256,
                            1.0F,
                            1.0F,
                            1.0F,
                            0.0325F
                        );
                    }
                } else if (zoom > 0.15F) {
                    this.renderLine(
                        (metaGrid.minX + x) * 256 + 100,
                        metaGrid.minY * 256,
                        (metaGrid.minX + x) * 256 + 100,
                        (metaGrid.maxY + 1) * 256,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.075F
                    );
                    this.renderLine(
                        (metaGrid.minX + x) * 256 + 200,
                        metaGrid.minY * 256,
                        (metaGrid.minX + x) * 256 + 200,
                        (metaGrid.maxY + 1) * 256,
                        1.0F,
                        1.0F,
                        1.0F,
                        0.075F
                    );
                }
            }
        }

        for (int ixx = 0; ixx < IsoPlayer.numPlayers; ixx++) {
            IsoPlayer players = IsoPlayer.players[ixx];
            if (players != null) {
                this.renderZombie(players.getX(), players.getY(), 0.0F, 0.5F, 0.0F);
            }
        }

        if (this.isEditingEnabled()) {
            float rx = this.editSquareInRange ? 0.0F : 1.0F;
            float g = this.editSquareInRange ? 1.0F : 0.0F;
            if (this.editWallN.getValue() || this.editDoorN.getValue()) {
                this.renderRect(this.editSquareX, this.editSquareY, 1.0F, 0.25F, rx, g, 0.0F, 0.5F);
                this.renderRect(this.editSquareX, this.editSquareY, 1.0F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05F, 0.25F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY + 0.2F, 1.0F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX + 0.95F, this.editSquareY, 0.05F, 0.25F, rx, g, 0.0F, 1.0F);
            } else if (!this.editWallW.getValue() && !this.editDoorW.getValue()) {
                this.renderRect(this.editSquareX, this.editSquareY, 1.0F, 1.0F, rx, g, 0.0F, 0.5F);
                this.renderRect(this.editSquareX, this.editSquareY, 1.0F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05F, 1.0F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY + 0.95F, 1.0F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX + 0.95F, this.editSquareY, 0.05F, 1.0F, rx, g, 0.0F, 1.0F);
            } else {
                this.renderRect(this.editSquareX, this.editSquareY, 0.25F, 1.0F, rx, g, 0.0F, 0.5F);
                this.renderRect(this.editSquareX, this.editSquareY, 0.25F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY, 0.05F, 1.0F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX, this.editSquareY + 0.95F, 0.25F, 0.05F, rx, g, 0.0F, 1.0F);
                this.renderRect(this.editSquareX + 0.2F, this.editSquareY, 0.05F, 1.0F, rx, g, 0.0F, 1.0F);
            }
        }

        if (selChunkRegion != null) {
            this.debugLine("- ChunkRegion -");
            this.debugLine("ID: " + selChunkRegion.getID());
            this.debugLine("Squares: " + selChunkRegion.getSquareSize());
            this.debugLine("Roofs: " + selChunkRegion.getRoofCnt());
            this.debugLine("Neighbors: " + selChunkRegion.getNeighborCount());
            this.debugLine("ConnectedNeighbors: " + selChunkRegion.getConnectedNeighbors().size());
            this.debugLine("FullyEnclosed: " + selChunkRegion.getIsEnclosed());
        }

        if (selWorldRegion != null) {
            this.debugLine("- WorldRegion -");
            this.debugLine("ID: " + selWorldRegion.getID());
            this.debugLine("Squares: " + selWorldRegion.getSquareSize());
            this.debugLine("Roofs: " + selWorldRegion.getRoofCnt());
            this.debugLine("IsFullyRoofed: " + selWorldRegion.isFullyRoofed());
            this.debugLine("RoofPercentage: " + selWorldRegion.getRoofedPercentage());
            this.debugLine("IsEnclosed: " + selWorldRegion.isEnclosed());
            this.debugLine("Neighbors: " + selWorldRegion.getNeighbors().size());
            this.debugLine("ChunkRegionCount: " + selWorldRegion.size());
        }

        int yxx = 15;

        for (int ixxx = 0; ixxx < this.debugLines.size(); ixxx++) {
            this.renderStringUI(10.0F, yxx, this.debugLines.get(ixxx), Colors.CornFlowerBlue);
            yxx += TextManager.instance.getFontHeight(UIFont.Small);
        }
    }

    public void setEditSquareCoord(int x, int y) {
        this.editSquareX = x;
        this.editSquareY = y;
        this.editSquareInRange = false;
        if (this.editCoordInRange(x, y)) {
            this.editSquareInRange = true;
        }
    }

    private boolean editCoordInRange(int x, int y) {
        IsoGridSquare gs = IsoWorld.instance.getCell().getGridSquare(x, y, 0);
        return gs != null;
    }

    public void editSquare(int x, int y) {
        if (this.isEditingEnabled()) {
            int z = this.getZLevel();
            IsoGridSquare gs = IsoWorld.instance.getCell().getGridSquare(x, y, z);
            DataRoot root = IsoRegions.getDataRoot();
            byte flags = root.getSquareFlags(x, y, z);
            if (this.editCoordInRange(x, y)) {
                if (gs == null) {
                    gs = IsoWorld.instance.getCell().createNewGridSquare(x, y, z, true);
                    if (gs == null) {
                        return;
                    }
                }

                this.editSquareInRange = true;

                for (int i = 0; i < this.editOptions.size(); i++) {
                    IsoRegionsRenderer.BooleanDebugOption setting = (IsoRegionsRenderer.BooleanDebugOption)this.editOptions.get(i);
                    if (setting.getValue()) {
                        String var9 = setting.getName();
                        switch (var9) {
                            case "Edit.WallW":
                            case "Edit.WallN":
                                IsoThumpable thumpablex;
                                if (setting.getName().equals("Edit.WallN")) {
                                    if (flags > 0 && Bits.hasFlags(flags, 1)) {
                                        return;
                                    }

                                    thumpablex = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_25", true, null);
                                } else {
                                    if (flags > 0 && Bits.hasFlags(flags, 2)) {
                                        return;
                                    }

                                    thumpablex = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_24", true, null);
                                }

                                thumpablex.setMaxHealth(100);
                                thumpablex.setName("Wall Debug");
                                thumpablex.setBreakSound("BreakObject");
                                gs.AddSpecialObject(thumpablex);
                                gs.RecalcAllWithNeighbours(true);
                                thumpablex.transmitCompleteItemToServer();
                                if (gs.getZone() != null) {
                                    gs.getZone().setHaveConstruction(true);
                                }
                                break;
                            case "Edit.DoorW":
                            case "Edit.DoorN":
                                IsoThumpable thumpable;
                                if (setting.getName().equals("Edit.DoorN")) {
                                    if (flags > 0 && Bits.hasFlags(flags, 1)) {
                                        return;
                                    }

                                    thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_35", true, null);
                                } else {
                                    if (flags > 0 && Bits.hasFlags(flags, 2)) {
                                        return;
                                    }

                                    thumpable = new IsoThumpable(IsoWorld.instance.getCell(), gs, "walls_exterior_wooden_01_34", true, null);
                                }

                                thumpable.setMaxHealth(100);
                                thumpable.setName("Door Frame Debug");
                                thumpable.setBreakSound("BreakObject");
                                gs.AddSpecialObject(thumpable);
                                gs.RecalcAllWithNeighbours(true);
                                thumpable.transmitCompleteItemToServer();
                                if (gs.getZone() != null) {
                                    gs.getZone().setHaveConstruction(true);
                                }
                                break;
                            case "Edit.Floor":
                                if (flags > 0 && Bits.hasFlags(flags, 16)) {
                                    return;
                                }

                                if (z == 0) {
                                    return;
                                }

                                gs.addFloor("carpentry_02_56");
                                if (gs.getZone() != null) {
                                    gs.getZone().setHaveConstruction(true);
                                }
                        }
                    }
                }
            } else {
                this.editSquareInRange = false;
            }
        }
    }

    public boolean isEditingEnabled() {
        return this.editingEnabled;
    }

    public void editRotate() {
        if (this.editWallN.getValue()) {
            this.editWallN.setValue(false);
            this.editWallW.setValue(true);
        } else if (this.editWallW.getValue()) {
            this.editWallW.setValue(false);
            this.editWallN.setValue(true);
        }

        if (this.editDoorN.getValue()) {
            this.editDoorN.setValue(false);
            this.editDoorW.setValue(true);
        } else if (this.editDoorW.getValue()) {
            this.editDoorW.setValue(false);
            this.editDoorN.setValue(true);
        }
    }

    public ConfigOption getEditOptionByName(String name) {
        for (int i = 0; i < this.editOptions.size(); i++) {
            ConfigOption setting = this.editOptions.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getEditOptionCount() {
        return this.editOptions.size();
    }

    public ConfigOption getEditOptionByIndex(int index) {
        return this.editOptions.get(index);
    }

    public void setEditOption(int index, boolean b) {
        for (int i = 0; i < this.editOptions.size(); i++) {
            IsoRegionsRenderer.BooleanDebugOption setting = (IsoRegionsRenderer.BooleanDebugOption)this.editOptions.get(i);
            if (i != index) {
                setting.setValue(false);
            } else {
                setting.setValue(b);
                this.editingEnabled = b;
            }
        }
    }

    public int getZLevel() {
        if (this.zLevelPlayer.getValue()) {
            return PZMath.fastfloor(IsoPlayer.getInstance().getZ());
        } else {
            for (int i = 0; i < this.zLevelOptions.size(); i++) {
                IsoRegionsRenderer.BooleanDebugOption setting = (IsoRegionsRenderer.BooleanDebugOption)this.zLevelOptions.get(i);
                if (setting.getValue()) {
                    return setting.zLevel;
                }
            }

            return 0;
        }
    }

    public ConfigOption getZLevelOptionByName(String name) {
        for (int i = 0; i < this.zLevelOptions.size(); i++) {
            ConfigOption setting = this.zLevelOptions.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getZLevelOptionCount() {
        return this.zLevelOptions.size();
    }

    public ConfigOption getZLevelOptionByIndex(int index) {
        return this.zLevelOptions.get(index);
    }

    public void setZLevelOption(int index, boolean b) {
        for (int i = 0; i < this.zLevelOptions.size(); i++) {
            IsoRegionsRenderer.BooleanDebugOption setting = (IsoRegionsRenderer.BooleanDebugOption)this.zLevelOptions.get(i);
            if (i != index) {
                setting.setValue(false);
            } else {
                setting.setValue(b);
            }
        }

        if (!b) {
            this.zLevelPlayer.setValue(true);
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
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "isoregions-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "isoregions-options.ini";
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
    }

    @UsedFromLua
    public static class BooleanDebugOption extends BooleanConfigOption {
        private final int index;
        private int zLevel;

        public BooleanDebugOption(ArrayList<ConfigOption> optionList, String name, boolean defaultValue, int zLevel) {
            super(name, defaultValue);
            this.index = optionList.size();
            this.zLevel = zLevel;
            optionList.add(this);
        }

        public BooleanDebugOption(ArrayList<ConfigOption> optionList, String name, boolean defaultValue) {
            super(name, defaultValue);
            this.index = optionList.size();
            optionList.add(this);
        }

        public int getIndex() {
            return this.index;
        }
    }
}
