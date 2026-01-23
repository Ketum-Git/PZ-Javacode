// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.styles;

import java.util.ArrayList;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapRenderer;

public final class WorldMapTextStyleLayer extends WorldMapStyleLayer {
    public UIFont font = UIFont.Handwritten;
    public int lineHeight = 40;
    public final ArrayList<WorldMapStyleLayer.ColorStop> fill = new ArrayList<>();

    public WorldMapTextStyleLayer(String id) {
        super(id);
    }

    @Override
    public String getTypeString() {
        return "Text";
    }

    @Override
    public boolean ignoreFeatures() {
        return true;
    }

    @Override
    public void render(WorldMapFeature feature, WorldMapStyleLayer.RenderArgs args) {
    }

    @Override
    public void renderVisibleCells(WorldMapStyleLayer.RenderArgs renderArgs) {
    }

    public UIFont getFont() {
        return this.font;
    }

    public float calculateScale(UIWorldMap ui) {
        return (float)this.lineHeight / TextManager.instance.getFontHeight(this.getFont());
    }

    public float calculateScale(WorldMapRenderer.Drawer drawer) {
        return (float)this.lineHeight / TextManager.instance.getFontHeight(this.getFont());
    }
}
