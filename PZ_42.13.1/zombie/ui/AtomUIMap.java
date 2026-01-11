// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.UIWorldMapV2;
import zombie.worldMap.WorldMapVisited;

@UsedFromLua
public class AtomUIMap extends AtomUI {
    UIWorldMap map;

    public AtomUIMap(KahluaTable table) {
        super(table);
    }

    @Override
    public void render() {
        if (this.visible) {
            super.render();
            double[] absPos = this.getAbsolutePosition(this.x, this.y);
            double[] absPos1 = this.getAbsolutePosition(this.x + this.width, this.y + this.height);
            this.map.setX(absPos[0]);
            this.map.setY(absPos[1]);
            this.map.setWidth(absPos1[0] - absPos[0]);
            this.map.setHeight(absPos1[1] - absPos[1]);
            this.map.render();
        }
    }

    @Override
    public void init() {
        this.map = new UIWorldMap(null);
        this.map.setDoStencil(false);
        UIWorldMapV2 api = this.map.getAPIv2();
        api.setBoolean("ClampBaseZoomToPoint5", false);
        api.setBoolean("Isometric", false);
        api.setBoolean("WorldBounds", false);
        api.setBoolean("Features", false);
        api.setBoolean("ImagePyramid", true);
        super.init();
        this.updateInternalValues();
    }

    @Override
    void updateInternalValues() {
        super.updateInternalValues();
    }

    public UIWorldMap getMapUI() {
        return this.map;
    }

    public void revealOnMap() {
        UIWorldMapV1 api = this.map.getAPIv1();
        int x1 = api.getMinXInSquares();
        int y1 = api.getMinYInSquares();
        int x2 = api.getMaxXInSquares();
        int y2 = api.getMaxYInSquares();
        WorldMapVisited.getInstance().setKnownInSquares(x1, y1, x2, y2);
    }
}
