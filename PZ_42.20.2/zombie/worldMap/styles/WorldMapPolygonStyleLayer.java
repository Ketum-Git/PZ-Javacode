// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.textures.Texture;
import zombie.worldMap.WorldMapFeature;

public class WorldMapPolygonStyleLayer extends WorldMapStyleLayer {
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList<>();
    public final ArrayList<WorldMapStyleLayer.TextureStop> texture = new ArrayList<>();
    public final ArrayList<WorldMapStyleLayer.FloatStop> scale = new ArrayList<>();

    public WorldMapPolygonStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Polygon";
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        if (fill.a < 0.01F) {
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        } else {
            float scale = this.evalFloat(args, this.scale);
            Texture texture = this.evalTexture(args, this.texture);
            WorldMapStyleLayer.TextureScaling scaling = this.evalTextureScaling(args, this.texture, WorldMapStyleLayer.TextureScaling.IsoGridSquare);
            if (texture != null && texture.isReady()) {
                args.drawer.fillPolygon(args, feature, fill, texture, scale, scaling);
            } else {
                args.drawer.fillPolygon(args, feature, fill);
            }

            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        }
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        renderArgs.drawer.renderVisibleCells(this);
    }
}
