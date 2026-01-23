// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.worldMap.streets.WorldMapStreetsV1;
import zombie.worldMap.styles.WorldMapStyleV1;
import zombie.worldMap.styles.WorldMapStyleV2;

@UsedFromLua
public class UIWorldMapV3 extends UIWorldMapV2 {
    protected WorldMapStreetsV1 streetsV1;
    private WorldMapStyleV2 styleV2;

    public UIWorldMapV3(UIWorldMap ui) {
        super(ui);
    }

    public boolean isDataLoaded() {
        return this.worldMap.isDataLoaded();
    }

    public int getDataWidthInCells() {
        return this.worldMap.getDataWidthInCells();
    }

    public int getDataHeightInCells() {
        return this.worldMap.getDataHeightInCells();
    }

    public void addImagePyramid(String fileName) {
        if (fileName.startsWith("media/")) {
            fileName = ZomboidFileSystem.instance.getString(fileName);
        }

        boolean hasImages = this.worldMap.hasImages();
        this.worldMap.addImagePyramid(fileName);
        if (!hasImages && this.worldMap.getWidthInSquares() > 1 && this.ui.getWidth() > 0.0) {
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

    public void clearImages() {
        this.worldMap.clearImages();
    }

    public int getImagePyramidMinX(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getMinX();
    }

    public int getImagePyramidMinY(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getMinY();
    }

    public int getImagePyramidMaxX(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getMaxX();
    }

    public int getImagePyramidMaxY(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getMaxY();
    }

    public int getImagePyramidWidthInSquares(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getWidthInSquares();
    }

    public int getImagePyramidHeightInSquares(String fileName) {
        WorldMapImages worldMapImages = this.worldMap.getWorldMapImagesByFileName(fileName);
        return worldMapImages == null ? -1 : worldMapImages.getHeightInSquares();
    }

    public void setMaxZoom(float maxZoom) {
        this.ui.renderer.setMaxZoom(maxZoom);
    }

    public float getMaxZoom() {
        return this.ui.renderer.getMaxZoom();
    }

    public void transitionTo(float worldX, float worldY, float zoomF) {
        if (this.worldMap.hasData() && this.worldMap.isDataLoaded() || this.worldMap.hasImages()) {
            if (this.renderer.getWorldMap() != null) {
                this.renderer.transitionTo(worldX, worldY, zoomF);
            }
        }
    }

    public void setDisplayedArea(float worldX1, float worldY1, float worldX2, float worldY2) {
        this.renderer.centerOn(worldX1 + (worldX2 - worldX1) / 2.0F, worldY1 + (worldY2 - worldY1) / 2.0F);
        if (this.renderer.getHeight() <= this.renderer.getWidth()) {
            double mpp = (worldY2 - worldY1) / this.renderer.getHeight();
            this.setZoom((float)MapProjection.zoomAtMetersPerPixel(mpp, this.renderer.getHeight()));
        } else {
            double mpp = this.renderer.getWidth() / (worldX2 - worldX1);
            this.setZoom((float)MapProjection.zoomAtMetersPerPixel(mpp, this.renderer.getWidth()));
        }
    }

    public WorldMapStreetsV1 getStreetsAPI() {
        if (this.streetsV1 == null) {
            this.streetsV1 = new WorldMapStreetsV1(this.ui);
        }

        return this.streetsV1;
    }

    @Override
    public WorldMapStyleV1 getStyleAPI() {
        if (this.styleV2 == null) {
            this.styleV2 = new WorldMapStyleV2(this.ui);
        }

        return this.styleV2;
    }
}
