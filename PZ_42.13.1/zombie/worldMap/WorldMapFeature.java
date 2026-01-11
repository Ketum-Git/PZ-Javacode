// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

public final class WorldMapFeature {
    public final WorldMapCell cell;
    public WorldMapGeometry geometry;
    public WorldMapProperties properties;

    public WorldMapFeature(WorldMapCell cell) {
        this.cell = cell;
    }

    public boolean hasLineString() {
        return this.geometry != null && this.geometry.type == WorldMapGeometry.Type.LineString;
    }

    public boolean hasPoint() {
        return this.geometry != null && this.geometry.type == WorldMapGeometry.Type.Point;
    }

    public boolean hasPolygon() {
        return this.geometry != null && this.geometry.type == WorldMapGeometry.Type.Polygon;
    }

    public boolean containsPoint(float x, float y) {
        return this.geometry != null && this.geometry.containsPoint(x, y);
    }

    public void clearTriangles() {
        if (this.geometry != null) {
            this.geometry.clearTriangles();
        }
    }

    public void dispose() {
        if (this.geometry != null) {
            this.geometry.dispose();
        }

        this.properties.clear();
    }
}
