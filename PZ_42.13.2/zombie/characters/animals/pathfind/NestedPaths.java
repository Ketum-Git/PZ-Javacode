// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.iso.zones.Zone;
import zombie.vehicles.Clipper;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.UIWorldMapV1;
import zombie.worldMap.WorldMapRenderer;

public final class NestedPaths {
    private static Clipper clipper;
    private static ByteBuffer clipperBuffer;
    public final ArrayList<NestedPath> paths = new ArrayList<>();

    public void init(Zone zone) {
        int inset = 5;

        while (this.generatePolygon(zone.points, inset)) {
            inset += 5;
        }
    }

    boolean generatePolygon(TIntArrayList points, int inset) {
        if (clipper == null) {
            clipper = new Clipper();
        }

        clipper.clear();
        if (clipperBuffer == null || clipperBuffer.capacity() < points.size() * 8 * 4) {
            clipperBuffer = ByteBuffer.allocateDirect(points.size() * 8 * 4);
        }

        clipperBuffer.clear();
        if (this.isClockwise(points)) {
            for (int i = this.numPoints(points) - 1; i >= 0; i--) {
                clipperBuffer.putFloat(this.getX(points, i));
                clipperBuffer.putFloat(this.getY(points, i));
            }
        } else {
            for (int i = 0; i < this.numPoints(points); i++) {
                clipperBuffer.putFloat(this.getX(points, i));
                clipperBuffer.putFloat(this.getY(points, i));
            }
        }

        clipper.addPath(this.numPoints(points), clipperBuffer, false);
        int numPolys = clipper.generatePolygons(-inset);
        if (numPolys <= 0) {
            return false;
        } else {
            boolean added = false;

            for (int i = 0; i < numPolys; i++) {
                clipperBuffer.clear();
                clipper.getPolygon(i, clipperBuffer);
                int numPoints = clipperBuffer.getShort();
                if (numPoints >= 3) {
                    float[] newPoints = new float[numPoints * 2];
                    float minX = Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE;
                    float maxY = Float.MIN_VALUE;

                    for (int j = 0; j < numPoints; j++) {
                        newPoints[j * 2] = clipperBuffer.getFloat();
                        newPoints[j * 2 + 1] = clipperBuffer.getFloat();
                        minX = PZMath.min(minX, newPoints[j * 2]);
                        minY = PZMath.min(minY, newPoints[j * 2 + 1]);
                        maxX = PZMath.max(maxX, newPoints[j * 2]);
                        maxY = PZMath.max(maxY, newPoints[j * 2 + 1]);
                    }

                    if (!(maxX - minX < 5.0F) && !(maxY - minY < 5.0F)) {
                        NestedPath path = new NestedPath();
                        path.points = newPoints;
                        path.inset = inset;
                        path.minX = minX;
                        path.minY = minY;
                        path.maxX = maxX;
                        path.maxY = maxY;
                        path.length = this.getLength(newPoints);
                        this.paths.add(path);
                        added = true;
                    }
                }
            }

            return added;
        }
    }

    boolean isClockwise(TIntArrayList points) {
        float sum = 0.0F;

        for (int i = 0; i < this.numPoints(points); i++) {
            int p1x = this.getX(points, i);
            int p1y = this.getY(points, i);
            int p2x = this.getX(points, (i + 1) % this.numPoints(points));
            int p2y = this.getY(points, (i + 1) % this.numPoints(points));
            sum += (p2x - p1x) * (p2y + p1y);
        }

        return sum > 0.0;
    }

    int numPoints(TIntArrayList points) {
        return points.size() / 2;
    }

    int getX(TIntArrayList points, int index) {
        return points.get(index * 2);
    }

    int getY(TIntArrayList points, int index) {
        return points.get(index * 2 + 1);
    }

    float getLength(float[] points) {
        float length = 0.0F;

        for (int i = 0; i < points.length; i += 2) {
            float x1 = points[i];
            float y1 = points[i + 1];
            float x2 = points[(i + 2) % points.length];
            float y2 = points[(i + 3) % points.length];
            length += Vector2f.length(x2 - x1, y2 - y1);
        }

        return length;
    }

    public void render(UIWorldMap ui) {
        for (NestedPath path : this.paths) {
            float[] points = path.points;

            for (int i = 0; i < points.length; i += 2) {
                float x1 = points[i];
                float y1 = points[i + 1];
                float x2 = points[(i + 2) % points.length];
                float y2 = points[(i + 3) % points.length];
                this.drawLine(ui.getAPIv1(), x1, y1, x2, y2, 0.0F, 0.0F, 1.0F, 1.0F);
            }
        }
    }

    public void drawLine(UIWorldMapV1 api, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        WorldMapRenderer rr = api.getRenderer();
        float _x1 = rr.worldToUIX(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y1 = rr.worldToUIY(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _x2 = rr.worldToUIX(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y2 = rr.worldToUIY(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        SpriteRenderer.instance.renderline(null, (int)_x1, (int)_y1, (int)_x2, (int)_y2, r, g, b, a, 1.0F);
    }
}
