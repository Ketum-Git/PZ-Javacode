// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import org.joml.Vector4f;
import zombie.iso.Vector2;

public final class QuadranglesIntersection {
    private static final float EPS = 0.001F;

    public static boolean IsQuadranglesAreIntersected(Vector2[] q1, Vector2[] q2) {
        if (q1 != null && q2 != null && q1.length == 4 && q2.length == 4) {
            if (lineIntersection(q1[0], q1[1], q2[0], q2[1])) {
                return true;
            } else if (lineIntersection(q1[0], q1[1], q2[1], q2[2])) {
                return true;
            } else if (lineIntersection(q1[0], q1[1], q2[2], q2[3])) {
                return true;
            } else if (lineIntersection(q1[0], q1[1], q2[3], q2[0])) {
                return true;
            } else if (lineIntersection(q1[1], q1[2], q2[0], q2[1])) {
                return true;
            } else if (lineIntersection(q1[1], q1[2], q2[1], q2[2])) {
                return true;
            } else if (lineIntersection(q1[1], q1[2], q2[2], q2[3])) {
                return true;
            } else if (lineIntersection(q1[1], q1[2], q2[3], q2[0])) {
                return true;
            } else if (lineIntersection(q1[2], q1[3], q2[0], q2[1])) {
                return true;
            } else if (lineIntersection(q1[2], q1[3], q2[1], q2[2])) {
                return true;
            } else if (lineIntersection(q1[2], q1[3], q2[2], q2[3])) {
                return true;
            } else if (lineIntersection(q1[2], q1[3], q2[3], q2[0])) {
                return true;
            } else if (lineIntersection(q1[3], q1[0], q2[0], q2[1])) {
                return true;
            } else if (lineIntersection(q1[3], q1[0], q2[1], q2[2])) {
                return true;
            } else if (lineIntersection(q1[3], q1[0], q2[2], q2[3])) {
                return true;
            } else if (lineIntersection(q1[3], q1[0], q2[3], q2[0])) {
                return true;
            } else {
                return IsPointInTriangle(q1[0], q2[0], q2[1], q2[2]) || IsPointInTriangle(q1[0], q2[0], q2[2], q2[3])
                    ? true
                    : IsPointInTriangle(q2[0], q1[0], q1[1], q1[2]) || IsPointInTriangle(q2[0], q1[0], q1[2], q1[3]);
            }
        } else {
            System.out.println("ERROR: IsQuadranglesAreIntersected");
            return false;
        }
    }

    public static boolean IsPointInTriangle(Vector2 p, Vector2[] q) {
        return IsPointInTriangle(p, q[0], q[1], q[2]) || IsPointInTriangle(p, q[0], q[2], q[3]);
    }

    public static float det(float a, float b, float c, float d) {
        return a * d - b * c;
    }

    private static boolean between(float a, float b, double c) {
        return Math.min(a, b) <= c + 0.001F && c <= Math.max(a, b) + 0.001F;
    }

    private static boolean intersect_1(float a1, float b1, float c1, float d1) {
        float a;
        float b;
        if (a1 > b1) {
            b = a1;
            a = b1;
        } else {
            a = a1;
            b = b1;
        }

        float c;
        float d;
        if (c1 > d1) {
            d = c1;
            c = d1;
        } else {
            c = c1;
            d = d1;
        }

        return Math.max(a, c) <= Math.min(b, d);
    }

    public static boolean lineIntersection(Vector2 start1, Vector2 end1, Vector2 start2, Vector2 end2) {
        float A1 = start1.y - end1.y;
        float B1 = end1.x - start1.x;
        float C1 = -A1 * start1.x - B1 * start1.y;
        float A2 = start2.y - end2.y;
        float B2 = end2.x - start2.x;
        float C2 = -A2 * start2.x - B2 * start2.y;
        float zn = det(A1, B1, A2, B2);
        if (zn != 0.0F) {
            double x = -det(C1, B1, C2, B2) * 1.0 / zn;
            double y = -det(A1, C1, A2, C2) * 1.0 / zn;
            return between(start1.x, end1.x, x) && between(start1.y, end1.y, y) && between(start2.x, end2.x, x) && between(start2.y, end2.y, y);
        } else {
            return det(A1, C1, A2, C2) == 0.0F
                && det(B1, C1, B2, C2) == 0.0F
                && intersect_1(start1.x, end1.x, start2.x, end2.x)
                && intersect_1(start1.y, end1.y, start2.y, end2.y);
        }
    }

    public static boolean IsQuadranglesAreTransposed2(Vector4f q1, Vector4f q2) {
        if (IsPointInQuadrilateral(new Vector2(q1.x, q1.y), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        } else if (IsPointInQuadrilateral(new Vector2(q1.z, q1.y), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        } else if (IsPointInQuadrilateral(new Vector2(q1.x, q1.w), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        } else if (IsPointInQuadrilateral(new Vector2(q1.z, q1.w), q2.x, q2.z, q2.y, q2.w)) {
            return true;
        } else if (IsPointInQuadrilateral(new Vector2(q2.x, q2.y), q1.x, q1.z, q1.y, q1.w)) {
            return true;
        } else if (IsPointInQuadrilateral(new Vector2(q2.z, q2.y), q1.x, q1.z, q1.y, q1.w)) {
            return true;
        } else {
            return IsPointInQuadrilateral(new Vector2(q2.x, q2.w), q1.x, q1.z, q1.y, q1.w)
                ? true
                : IsPointInQuadrilateral(new Vector2(q2.z, q2.w), q1.x, q1.z, q1.y, q1.w);
        }
    }

    private static boolean IsPointInQuadrilateral(Vector2 point, float x1, float x2, float y1, float y2) {
        return IsPointInTriangle(point, new Vector2(x1, y1), new Vector2(x1, y2), new Vector2(x2, y2))
            ? true
            : IsPointInTriangle(point, new Vector2(x2, y2), new Vector2(x2, y1), new Vector2(x1, y1));
    }

    private static boolean IsPointInTriangle(Vector2 point, Vector2 t1, Vector2 t2, Vector2 t3) {
        float d1 = (t1.x - point.x) * (t2.y - t1.y) - (t2.x - t1.x) * (t1.y - point.y);
        float d2 = (t2.x - point.x) * (t3.y - t2.y) - (t3.x - t2.x) * (t2.y - point.y);
        float d3 = (t3.x - point.x) * (t1.y - t3.y) - (t1.x - t3.x) * (t3.y - point.y);
        return d1 >= 0.0F && d2 >= 0.0F && d3 >= 0.0F || d1 <= 0.0F && d2 <= 0.0F && d3 <= 0.0F;
    }
}
