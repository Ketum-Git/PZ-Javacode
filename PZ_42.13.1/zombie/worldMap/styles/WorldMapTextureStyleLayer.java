// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.textures.Texture;
import zombie.worldMap.WorldMapFeature;

public class WorldMapTextureStyleLayer extends WorldMapStyleLayer {
    public int worldX1;
    public int worldY1;
    public int worldX2;
    public int worldY2;
    public boolean useWorldBounds;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList<>();
    public final ArrayList<WorldMapStyleLayer.TextureStop> texture = new ArrayList<>();
    public boolean tile;

    public WorldMapTextureStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Texture";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public boolean filter(WorldMapFeature feature, WorldMapStyleLayer.FilterArgs args) {
        return false;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args) {
    }

    @Override
    public void renderCell(WorldMapStyleLayer.RenderArgs args) {
        if (this.useWorldBounds) {
            this.worldX1 = args.renderer.getWorldMap().getMinXInSquares();
            this.worldY1 = args.renderer.getWorldMap().getMinYInSquares();
            this.worldX2 = args.renderer.getWorldMap().getMaxXInSquares() + 1;
            this.worldY2 = args.renderer.getWorldMap().getMaxYInSquares() + 1;
        }

        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        if (fill.a < 0.01F) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        } else {
            Texture texture = this.evalTexture(args, this.texture);
            if (texture == null) {
                WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            } else {
                if (this.tile) {
                    args.drawer.drawTextureTiled(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2, args.cellX, args.cellY);
                } else {
                    args.drawer.drawTexture(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2, args.cellX, args.cellY);
                }

                WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            }
        }
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs args) {
        if (this.useWorldBounds) {
            this.worldX1 = args.renderer.getWorldMap().getMinXInSquares();
            this.worldY1 = args.renderer.getWorldMap().getMinYInSquares();
            this.worldX2 = args.renderer.getWorldMap().getMaxXInSquares() + 1;
            this.worldY2 = args.renderer.getWorldMap().getMaxYInSquares() + 1;
        }

        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        if (fill.a < 0.01F) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        } else {
            Texture texture = this.evalTexture(args, this.texture);
            if (texture == null) {
                WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            } else {
                if (this.tile) {
                    args.drawer.drawTextureTiled(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2);
                } else {
                    args.drawer.drawTexture(texture, fill, this.worldX1, this.worldY1, this.worldX2, this.worldY2);
                }

                WorldMapStyleLayer.RGBAf.s_pool.release(fill);
            }
        }
    }
}
