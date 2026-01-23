// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import org.joml.Vector2d;

public final class MapProjection {
    public static final double EARTH_RADIUS_METERS = 6378137.0;
    public static final double EARTH_HALF_CIRCUMFERENCE_METERS = 2.0037508342789244E7;
    public static final double EARTH_CIRCUMFERENCE_METERS = 4.007501668557849E7;
    public static final double MAX_LATITUDE_DEGREES = 85.05112878;
    private static final double LOG_2 = Math.log(2.0);

    static MapProjection.ProjectedMeters lngLatToProjectedMeters(MapProjection.LngLat lngLat) {
        MapProjection.ProjectedMeters meters = new MapProjection.ProjectedMeters();
        meters.x = lngLat.longitude * 2.0037508342789244E7 / 180.0;
        meters.y = Math.log(Math.tan((Math.PI / 4) + lngLat.latitude * Math.PI / 360.0)) * 6378137.0;
        return meters;
    }

    static double metersPerTileAtZoom(int zoom) {
        return 4.007501668557849E7 / (1 << zoom);
    }

    static double metersPerPixelAtZoom(double zoom, double tileSize) {
        return 4.007501668557849E7 / (exp2(zoom) * tileSize);
    }

    static double zoomAtMetersPerPixel(double metersPerPixel, double tileSize) {
        return log2(4.007501668557849E7 / (metersPerPixel * tileSize));
    }

    static MapProjection.BoundingBox mapLngLatBounds() {
        return new MapProjection.BoundingBox(new Vector2d(-180.0, -85.05112878), new Vector2d(180.0, 85.05112878));
    }

    static MapProjection.BoundingBox mapProjectedMetersBounds() {
        MapProjection.BoundingBox bound = mapLngLatBounds();
        return new MapProjection.BoundingBox(
            lngLatToProjectedMeters(new MapProjection.LngLat(bound.min.x, bound.min.y)),
            lngLatToProjectedMeters(new MapProjection.LngLat(bound.max.x, bound.max.y))
        );
    }

    public static double exp2(double N) {
        return Math.pow(2.0, N);
    }

    public static double log2(double N) {
        return Math.log(N) / LOG_2;
    }

    public static final class BoundingBox {
        Vector2d min;
        Vector2d max;

        public BoundingBox(Vector2d min, Vector2d max) {
            this.min = min;
            this.max = max;
        }
    }

    public static final class LngLat {
        double longitude;
        double latitude;

        public LngLat(double lng, double lat) {
            this.longitude = lng;
            this.latitude = lat;
        }
    }

    public static final class ProjectedMeters extends Vector2d {
    }
}
