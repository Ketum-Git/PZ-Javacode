// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import org.joml.Matrix4f;
import zombie.UsedFromLua;
import zombie.config.ConfigOption;
import zombie.input.Mouse;
import zombie.inventory.types.MapItem;
import zombie.worldMap.markers.WorldMapMarkers;
import zombie.worldMap.markers.WorldMapMarkersV1;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleV1;
import zombie.worldMap.symbols.WorldMapSymbolsAPI;
import zombie.worldMap.symbols.WorldMapSymbolsV1;

@UsedFromLua
public class UIWorldMapV1 {
    final UIWorldMap ui;
    protected final WorldMap worldMap;
    protected final WorldMapStyle style;
    protected final WorldMapRenderer renderer;
    protected final WorldMapMarkers markers;
    protected WorldMapMarkersV1 markersV1;
    protected WorldMapStyleV1 styleV1;
    protected WorldMapSymbolsV1 symbolsV1;

    public UIWorldMapV1(UIWorldMap ui) {
        this.ui = ui;
        this.worldMap = this.ui.worldMap;
        this.style = this.ui.style;
        this.renderer = this.ui.renderer;
        this.markers = this.ui.markers;
    }

    public void setMapItem(MapItem mapItem) {
        this.ui.setMapItem(mapItem);
    }

    public WorldMapRenderer getRenderer() {
        return this.renderer;
    }

    public WorldMapMarkers getMarkers() {
        return this.markers;
    }

    public WorldMapStyle getStyle() {
        return this.style;
    }

    public WorldMapMarkersV1 getMarkersAPI() {
        if (this.markersV1 == null) {
            this.markersV1 = new WorldMapMarkersV1(this.ui);
        }

        return this.markersV1;
    }

    public WorldMapStyleV1 getStyleAPI() {
        if (this.styleV1 == null) {
            this.styleV1 = new WorldMapStyleV1(this.ui);
        }

        return this.styleV1;
    }

    public WorldMapSymbolsAPI getSymbolsAPI() {
        if (this.symbolsV1 == null) {
            this.symbolsV1 = new WorldMapSymbolsV1(this.ui, this.ui.symbols);
        }

        return this.symbolsV1;
    }

    public void addData(String fileName) {
        boolean hasData = this.worldMap.hasData();
        this.worldMap.addData(fileName);
        if (!hasData && this.ui.getWidth() > 0.0 && this.ui.getHeight() > 0.0) {
            this.renderer
                .setMap(
                    this.worldMap,
                    this.ui.getAbsoluteX().intValue(),
                    this.ui.getAbsoluteY().intValue(),
                    this.ui.getWidth().intValue(),
                    this.ui.getHeight().intValue()
                );
            this.resetView();
        }
    }

    public int getDataCount() {
        return this.worldMap.getDataCount();
    }

    public String getDataFileByIndex(int index) {
        WorldMapData data = this.worldMap.getDataByIndex(index);
        return data.relativeFileName;
    }

    public void clearData() {
        this.worldMap.clearData();
    }

    public void endDirectoryData() {
        this.worldMap.endDirectoryData();
    }

    public void addImages(String directory) {
        boolean hasImages = this.worldMap.hasImages();
        this.worldMap.addImages(directory);
        if (!hasImages && this.ui.getWidth() > 0.0 && this.ui.getHeight() > 0.0) {
            this.renderer
                .setMap(
                    this.worldMap,
                    this.ui.getAbsoluteX().intValue(),
                    this.ui.getAbsoluteY().intValue(),
                    this.ui.getWidth().intValue(),
                    this.ui.getHeight().intValue()
                );
            this.resetView();
        }
    }

    public int getImagesCount() {
        return this.worldMap.getImagesCount();
    }

    public void setBoundsInCells(int minX, int minY, int maxX, int maxY) {
        int CELL_SIZE = 256;
        boolean changed = minX * 256 != this.worldMap.minX
            || minY * 256 != this.worldMap.minY
            || maxX * 256 + 256 - 1 != this.worldMap.maxX
            || maxY + 256 + 256 - 1 != this.worldMap.maxY;
        this.worldMap.setBoundsInCells(minX, minY, maxX, maxY);
        if (changed && this.worldMap.hasData()) {
            this.resetView();
        }
    }

    public void setBoundsInSquares(int minX, int minY, int maxX, int maxY) {
        boolean changed = minX != this.worldMap.minX || minY != this.worldMap.minY || maxX != this.worldMap.maxX || maxY != this.worldMap.maxY;
        this.worldMap.setBoundsInSquares(minX, minY, maxX, maxY);
        if (this.renderer.getWorldMap() == null && this.ui.getWidth() > 0.0) {
            this.renderer
                .setMap(
                    this.worldMap,
                    this.ui.getAbsoluteX().intValue(),
                    this.ui.getAbsoluteY().intValue(),
                    this.ui.getWidth().intValue(),
                    this.ui.getHeight().intValue()
                );
        }

        if (changed && (this.worldMap.hasData() || this.worldMap.hasImages())) {
            this.resetView();
        }
    }

    public void setBoundsFromWorld() {
        this.worldMap.setBoundsFromWorld();
    }

    public void setBoundsFromData() {
        this.worldMap.setBoundsFromData();
    }

    public int getMinXInCells() {
        return this.worldMap.getMinXInCells();
    }

    public int getMinYInCells() {
        return this.worldMap.getMinYInCells();
    }

    public int getMaxXInCells() {
        return this.worldMap.getMaxXInCells();
    }

    public int getMaxYInCells() {
        return this.worldMap.getMaxYInCells();
    }

    public int getWidthInCells() {
        return this.worldMap.getWidthInCells();
    }

    public int getHeightInCells() {
        return this.worldMap.getHeightInCells();
    }

    public int getMinXInSquares() {
        return this.worldMap.getMinXInSquares();
    }

    public int getMinYInSquares() {
        return this.worldMap.getMinYInSquares();
    }

    public int getMaxXInSquares() {
        return this.worldMap.getMaxXInSquares();
    }

    public int getMaxYInSquares() {
        return this.worldMap.getMaxYInSquares();
    }

    public int getWidthInSquares() {
        return this.worldMap.getWidthInSquares();
    }

    public int getHeightInSquares() {
        return this.worldMap.getHeightInSquares();
    }

    public float uiToWorldX(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        return this.renderer.uiToWorldX(uiX, uiY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
    }

    public float uiToWorldY(float uiX, float uiY, float zoomF, float centerWorldX, float centerWorldY) {
        return this.renderer.uiToWorldY(uiX, uiY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
    }

    protected float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        return this.renderer.worldToUIX(worldX, worldY, zoomF, centerWorldX, centerWorldY, projection, modelView);
    }

    protected float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f projection, Matrix4f modelView) {
        return this.renderer.worldToUIY(worldX, worldY, zoomF, centerWorldX, centerWorldY, projection, modelView);
    }

    protected float worldToUIX(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        return this.renderer.worldToUIX(worldX, worldY, zoomF, centerWorldX, centerWorldY, modelViewProjection);
    }

    protected float worldToUIY(float worldX, float worldY, float zoomF, float centerWorldX, float centerWorldY, Matrix4f modelViewProjection) {
        return this.renderer.worldToUIY(worldX, worldY, zoomF, centerWorldX, centerWorldY, modelViewProjection);
    }

    protected float worldOriginUIX(float zoomF, float centerWorldX) {
        return this.renderer.worldOriginUIX(zoomF, centerWorldX);
    }

    protected float worldOriginUIY(float zoomF, float centerWorldY) {
        return this.renderer.worldOriginUIY(zoomF, centerWorldY);
    }

    protected float zoomMult() {
        return this.renderer.zoomMult();
    }

    protected float getWorldScale(float zoomF) {
        return this.renderer.getWorldScale(zoomF);
    }

    public float worldOriginX() {
        return this.renderer.worldOriginUIX(this.renderer.getDisplayZoomF(), this.renderer.getCenterWorldX());
    }

    public float worldOriginY() {
        return this.renderer.worldOriginUIY(this.renderer.getDisplayZoomF(), this.renderer.getCenterWorldY());
    }

    public float getBaseZoom() {
        return this.renderer.getBaseZoom();
    }

    public float getZoomF() {
        return this.renderer.getDisplayZoomF();
    }

    public float getWorldScale() {
        return this.renderer.getWorldScale(this.renderer.getDisplayZoomF());
    }

    public float getCenterWorldX() {
        return this.renderer.getCenterWorldX();
    }

    public float getCenterWorldY() {
        return this.renderer.getCenterWorldY();
    }

    public float uiToWorldX(float uiX, float uiY) {
        return !this.worldMap.hasData() && !this.worldMap.hasImages()
            ? 0.0F
            : this.uiToWorldX(uiX, uiY, this.renderer.getDisplayZoomF(), this.renderer.getCenterWorldX(), this.renderer.getCenterWorldY());
    }

    public float uiToWorldY(float uiX, float uiY) {
        return !this.worldMap.hasData() && !this.worldMap.hasImages()
            ? 0.0F
            : this.uiToWorldY(uiX, uiY, this.renderer.getDisplayZoomF(), this.renderer.getCenterWorldY(), this.renderer.getCenterWorldY());
    }

    public float worldToUIX(float worldX, float worldY) {
        return !this.worldMap.hasData() && !this.worldMap.hasImages()
            ? 0.0F
            : this.worldToUIX(
                worldX,
                worldY,
                this.renderer.getDisplayZoomF(),
                this.renderer.getCenterWorldX(),
                this.renderer.getCenterWorldY(),
                this.renderer.getModelViewProjectionMatrix()
            );
    }

    public float worldToUIY(float worldX, float worldY) {
        return !this.worldMap.hasData() && !this.worldMap.hasImages()
            ? 0.0F
            : this.worldToUIY(
                worldX,
                worldY,
                this.renderer.getDisplayZoomF(),
                this.renderer.getCenterWorldX(),
                this.renderer.getCenterWorldY(),
                this.renderer.getModelViewProjectionMatrix()
            );
    }

    public void centerOn(float worldX, float worldY) {
        if (this.worldMap.hasData() || this.worldMap.hasImages()) {
            this.renderer.centerOn(worldX, worldY);
        }
    }

    public void moveView(float dx, float dy) {
        if (this.worldMap.hasData() || this.worldMap.hasImages()) {
            this.renderer.moveView((int)dx, (int)dy);
        }
    }

    public void zoomAt(float uiX, float uiY, float delta) {
        if (this.worldMap.hasData() || this.worldMap.hasImages()) {
            this.renderer.zoomAt((int)uiX, (int)uiY, -((int)delta));
        }
    }

    public void setZoom(float zoom) {
        this.renderer.setZoom(zoom);
    }

    public void resetView() {
        if (this.worldMap.hasData() && this.worldMap.isDataLoaded() || this.worldMap.hasImages()) {
            if (this.renderer.getWorldMap() != null) {
                this.renderer.resetView();
            }
        }
    }

    public float mouseToWorldX() {
        float mouseX = Mouse.getXA() - this.ui.getAbsoluteX().intValue();
        float mouseY = Mouse.getYA() - this.ui.getAbsoluteY().intValue();
        return this.uiToWorldX(mouseX, mouseY);
    }

    public float mouseToWorldY() {
        float mouseX = Mouse.getXA() - this.ui.getAbsoluteX().intValue();
        float mouseY = Mouse.getYA() - this.ui.getAbsoluteY().intValue();
        return this.uiToWorldY(mouseX, mouseY);
    }

    public void setBackgroundRGBA(float r, float g, float b, float a) {
        this.ui.color.init(r, g, b, a);
    }

    public void setDropShadowWidth(int width) {
        this.ui.renderer.setDropShadowWidth(width);
    }

    public void setUnvisitedRGBA(float r, float g, float b, float a) {
        WorldMapVisited.getInstance().setUnvisitedRGBA(r, g, b, a);
    }

    public void setUnvisitedGridRGBA(float r, float g, float b, float a) {
        WorldMapVisited.getInstance().setUnvisitedGridRGBA(r, g, b, a);
    }

    public int getOptionCount() {
        return this.renderer.getOptionCount();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.renderer.getOptionByIndex(index);
    }

    public void setBoolean(String name, boolean value) {
        this.renderer.setBoolean(name, value);
    }

    public boolean getBoolean(String name) {
        return this.renderer.getBoolean(name);
    }

    public void setDouble(String name, double value) {
        this.renderer.setDouble(name, value);
    }

    public double getDouble(String name, double defaultValue) {
        return this.renderer.getDouble(name, defaultValue);
    }
}
