// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.markers;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.util.Pool;
import zombie.worldMap.UIWorldMap;

@UsedFromLua
public final class WorldMapMarkers {
    private static final Pool<WorldMapGridSquareMarker> s_gridSquareMarkerPool = new Pool<>(WorldMapGridSquareMarker::new);
    private final ArrayList<WorldMapMarker> markers = new ArrayList<>();

    public WorldMapGridSquareMarker addGridSquareMarker(int worldX, int worldY, int radius, float r, float g, float b, float a) {
        WorldMapGridSquareMarker marker = s_gridSquareMarkerPool.alloc().init(worldX, worldY, radius, r, g, b, a);
        this.markers.add(marker);
        return marker;
    }

    public void removeMarker(WorldMapMarker marker) {
        if (this.markers.contains(marker)) {
            this.markers.remove(marker);
            marker.release();
        }
    }

    public void clear() {
        for (int i = 0; i < this.markers.size(); i++) {
            this.markers.get(i).release();
        }

        this.markers.clear();
    }

    public void render(UIWorldMap ui) {
        for (int i = 0; i < this.markers.size(); i++) {
            this.markers.get(i).render(ui);
        }
    }
}
