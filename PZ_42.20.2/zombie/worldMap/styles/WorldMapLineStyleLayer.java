// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.worldMap.WorldMapFeature;

public class WorldMapLineStyleLayer extends WorldMapStyleLayer {
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList<>();
    public final ArrayList<WorldMapStyleLayer.FloatStop> lineWidth = new ArrayList<>();

    public WorldMapLineStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Line";
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args) {
        WorldMapStyleLayer.RGBAf fill = this.evalColor(args, this.fill);
        if (!(fill.a < 0.01F)) {
            float lineWidth;
            if (feature.properties.containsKey("width")) {
                lineWidth = PZMath.tryParseFloat(feature.properties.get("width"), 1.0F) * args.drawer.getWorldScale();
            } else {
                lineWidth = this.evalFloat(args, this.lineWidth);
            }

            args.drawer.drawLineString(args, feature, fill, lineWidth);
            WorldMapStyleLayer.RGBAf.s_pool.release(fill);
        }
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
        renderArgs.drawer.renderVisibleCells(this);
    }
}
