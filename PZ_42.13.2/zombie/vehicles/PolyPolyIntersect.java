// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import org.joml.Vector2f;
import zombie.pathfind.VehiclePoly;

public final class PolyPolyIntersect {
    private static final Vector2f tempVector2f_1 = new Vector2f();
    private static final Vector2f tempVector2f_2 = new Vector2f();
    private static final Vector2f tempVector2f_3 = new Vector2f();

    public static boolean intersects(VehiclePoly a, VehiclePoly b) {
        for (int pn = 0; pn < 2; pn++) {
            VehiclePoly polygon = pn == 0 ? a : b;

            for (int i1 = 0; i1 < 4; i1++) {
                int i2 = (i1 + 1) % 4;
                Vector2f p1 = getPoint(polygon, i1, tempVector2f_1);
                Vector2f p2 = getPoint(polygon, i2, tempVector2f_2);
                Vector2f normal = tempVector2f_3.set(p2.y - p1.y, p1.x - p2.x);
                double minA = Double.MAX_VALUE;
                double maxA = Double.NEGATIVE_INFINITY;

                for (int i = 0; i < 4; i++) {
                    Vector2f p = getPoint(a, i, tempVector2f_1);
                    double projected = normal.x * p.x + normal.y * p.y;
                    if (projected < minA) {
                        minA = projected;
                    }

                    if (projected > maxA) {
                        maxA = projected;
                    }
                }

                double minB = Double.MAX_VALUE;
                double maxB = Double.NEGATIVE_INFINITY;

                for (int i = 0; i < 4; i++) {
                    Vector2f px = getPoint(b, i, tempVector2f_1);
                    double projectedx = normal.x * px.x + normal.y * px.y;
                    if (projectedx < minB) {
                        minB = projectedx;
                    }

                    if (projectedx > maxB) {
                        maxB = projectedx;
                    }
                }

                if (maxA < minB || maxB < minA) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Vector2f getPoint(VehiclePoly p, int i, Vector2f v) {
        if (i == 0) {
            return v.set(p.x1, p.y1);
        } else if (i == 1) {
            return v.set(p.x2, p.y2);
        } else if (i == 2) {
            return v.set(p.x3, p.y3);
        } else {
            return i == 3 ? v.set(p.x4, p.y4) : null;
        }
    }
}
