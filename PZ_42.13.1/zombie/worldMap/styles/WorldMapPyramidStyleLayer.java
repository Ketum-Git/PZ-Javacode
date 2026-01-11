// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.io.File;
import java.util.ArrayList;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapImages;

public class WorldMapPyramidStyleLayer extends WorldMapStyleLayer {
    public String fileName;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList<>();

    public WorldMapPyramidStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Pyramid";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args) {
    }

    @Override
    public void renderCell(WorldMapStyleLayer.RenderArgs args) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        if (fill.a < 0.01F) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        } else {
            args.drawer.drawImagePyramid(args.cellX, args.cellY, this.fileName, fill);
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        }
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        WorldMap worldMap = renderArgs.drawer.worldMap;

        for (int i = worldMap.getImagesCount() - 1; i >= 0; i--) {
            WorldMapImages images = worldMap.getImagesByIndex(i);
            if (images.getAbsolutePath().endsWith(File.separator + this.fileName)) {
                renderArgs.drawer.renderImagePyramid(images);
            }
        }
    }

    public boolean isVisible(WorldMapStyleLayer.RenderArgs args) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        boolean bVisible = fill.a > 0.0F;
        WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        return bVisible;
    }

    public WorldMapStyleLayer.RGBAf getFill(WorldMapStyleLayer.RenderArgs args) {
        return this.evalColor(args, this.fill);
    }
}
