// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;

public class StrokeGeometry {
    static StrokeGeometry.Point firstPoint;
    static StrokeGeometry.Point lastPoint;
    static final double EPSILON = 1.0E-4;

    static StrokeGeometry.Point newPoint(double x, double y) {
        if (firstPoint == null) {
            return new StrokeGeometry.Point(x, y);
        } else {
            StrokeGeometry.Point p = firstPoint;
            firstPoint = firstPoint.next;
            if (lastPoint == p) {
                lastPoint = null;
            }

            p.next = null;
            return p.set(x, y);
        }
    }

    static void release(StrokeGeometry.Point p) {
        if (p.next == null && p != lastPoint) {
            p.next = firstPoint;
            firstPoint = p;
            if (lastPoint == null) {
                lastPoint = p;
            }
        }
    }

    static void release(ArrayList<StrokeGeometry.Point> points) {
        for (int i = 0; i < points.size(); i++) {
            release(points.get(i));
        }
    }

    static ArrayList<StrokeGeometry.Point> getStrokeGeometry(StrokeGeometry.Point[] points, StrokeGeometry.Attrs attrs) {
        if (points.length < 2) {
            return null;
        } else {
            String cap = attrs.cap;
            String join = attrs.join;
            float lineWidth = attrs.width / 2.0F;
            float miterLimit = attrs.miterLimit;
            ArrayList<StrokeGeometry.Point> vertices = new ArrayList<>();
            ArrayList<StrokeGeometry.Point> middlePoints = new ArrayList<>();
            boolean closed = false;
            if (points.length == 2) {
                join = "bevel";
                createTriangles(points[0], StrokeGeometry.Point.Middle(points[0], points[1]), points[1], vertices, lineWidth, join, miterLimit);
            } else {
                for (int i = 0; i < points.length - 1; i++) {
                    if (i == 0) {
                        middlePoints.add(points[0]);
                    } else if (i == points.length - 2) {
                        middlePoints.add(points[points.length - 1]);
                    } else {
                        middlePoints.add(StrokeGeometry.Point.Middle(points[i], points[i + 1]));
                    }
                }

                for (int ix = 1; ix < middlePoints.size(); ix++) {
                    createTriangles(middlePoints.get(ix - 1), points[ix], middlePoints.get(ix), vertices, lineWidth, join, miterLimit);
                }
            }

            if (cap.equals("round")) {
                StrokeGeometry.Point p00 = vertices.get(0);
                StrokeGeometry.Point p01 = vertices.get(1);
                StrokeGeometry.Point p02 = points[1];
                StrokeGeometry.Point p10 = vertices.get(vertices.size() - 1);
                StrokeGeometry.Point p11 = vertices.get(vertices.size() - 3);
                StrokeGeometry.Point p12 = points[points.length - 2];
                createRoundCap(points[0], p00, p01, p02, vertices);
                createRoundCap(points[points.length - 1], p10, p11, p12, vertices);
            } else if (cap.equals("square")) {
                StrokeGeometry.Point p00 = vertices.get(vertices.size() - 1);
                StrokeGeometry.Point p01 = vertices.get(vertices.size() - 3);
                createSquareCap(
                    vertices.get(0),
                    vertices.get(1),
                    StrokeGeometry.Point.Sub(points[0], points[1]).normalize().scalarMult(StrokeGeometry.Point.Sub(points[0], vertices.get(0)).length()),
                    vertices
                );
                createSquareCap(
                    p00,
                    p01,
                    StrokeGeometry.Point.Sub(points[points.length - 1], points[points.length - 2])
                        .normalize()
                        .scalarMult(StrokeGeometry.Point.Sub(p01, points[points.length - 1]).length()),
                    vertices
                );
            }

            return vertices;
        }
    }

    static void createSquareCap(StrokeGeometry.Point p0, StrokeGeometry.Point p1, StrokeGeometry.Point dir, ArrayList<StrokeGeometry.Point> verts) {
        verts.add(p0);
        verts.add(StrokeGeometry.Point.Add(p0, dir));
        verts.add(StrokeGeometry.Point.Add(p1, dir));
        verts.add(p1);
        verts.add(StrokeGeometry.Point.Add(p1, dir));
        verts.add(p0);
    }

    static void createRoundCap(
        StrokeGeometry.Point center,
        StrokeGeometry.Point _p0,
        StrokeGeometry.Point _p1,
        StrokeGeometry.Point nextPointInLine,
        ArrayList<StrokeGeometry.Point> verts
    ) {
        double radius = StrokeGeometry.Point.Sub(center, _p0).length();
        double angle0 = Math.atan2(_p1.y - center.y, _p1.x - center.x);
        double angle1 = Math.atan2(_p0.y - center.y, _p0.x - center.x);
        double orgAngle0 = angle0;
        if (angle1 > angle0) {
            if (angle1 - angle0 >= 3.141492653589793) {
                angle1 -= Math.PI * 2;
            }
        } else if (angle0 - angle1 >= 3.141492653589793) {
            angle0 -= Math.PI * 2;
        }

        double angleDiff = angle1 - angle0;
        if (Math.abs(angleDiff) >= 3.141492653589793 && Math.abs(angleDiff) <= 3.1416926535897933) {
            StrokeGeometry.Point r1 = StrokeGeometry.Point.Sub(center, nextPointInLine);
            if (r1.x == 0.0) {
                if (r1.y > 0.0) {
                    angleDiff = -angleDiff;
                }
            } else if (r1.x >= -1.0E-4) {
                angleDiff = -angleDiff;
            }
        }

        int nsegments = (int)(Math.abs(angleDiff * radius) / 7.0);
        nsegments++;
        double angleInc = angleDiff / nsegments;

        for (int i = 0; i < nsegments; i++) {
            verts.add(newPoint(center.x, center.y));
            verts.add(newPoint(center.x + radius * Math.cos(orgAngle0 + angleInc * i), center.y + radius * Math.sin(orgAngle0 + angleInc * i)));
            verts.add(newPoint(center.x + radius * Math.cos(orgAngle0 + angleInc * (1 + i)), center.y + radius * Math.sin(orgAngle0 + angleInc * (1 + i))));
        }
    }

    static double signedArea(StrokeGeometry.Point p0, StrokeGeometry.Point p1, StrokeGeometry.Point p2) {
        return (p1.x - p0.x) * (p2.y - p0.y) - (p2.x - p0.x) * (p1.y - p0.y);
    }

    static StrokeGeometry.Point lineIntersection(StrokeGeometry.Point p0, StrokeGeometry.Point p1, StrokeGeometry.Point p2, StrokeGeometry.Point p3) {
        double a0 = p1.y - p0.y;
        double b0 = p0.x - p1.x;
        double a1 = p3.y - p2.y;
        double b1 = p2.x - p3.x;
        double det = a0 * b1 - a1 * b0;
        if (det > -1.0E-4 && det < 1.0E-4) {
            return null;
        } else {
            double c0 = a0 * p0.x + b0 * p0.y;
            double c1 = a1 * p2.x + b1 * p2.y;
            double x = (b1 * c0 - b0 * c1) / det;
            double y = (a0 * c1 - a1 * c0) / det;
            return newPoint(x, y);
        }
    }

    static void createTriangles(
        StrokeGeometry.Point p0,
        StrokeGeometry.Point p1,
        StrokeGeometry.Point p2,
        ArrayList<StrokeGeometry.Point> verts,
        float width,
        String join,
        float miterLimit
    ) {
        StrokeGeometry.Point t0 = StrokeGeometry.Point.Sub(p1, p0);
        StrokeGeometry.Point t2 = StrokeGeometry.Point.Sub(p2, p1);
        t0.perpendicular();
        t2.perpendicular();
        if (signedArea(p0, p1, p2) > 0.0) {
            t0.invert();
            t2.invert();
        }

        t0.normalize();
        t2.normalize();
        t0.scalarMult(width);
        t2.scalarMult(width);
        StrokeGeometry.Point pintersect = lineIntersection(
            StrokeGeometry.Point.Add(t0, p0), StrokeGeometry.Point.Add(t0, p1), StrokeGeometry.Point.Add(t2, p2), StrokeGeometry.Point.Add(t2, p1)
        );
        StrokeGeometry.Point anchor = null;
        double anchorLength = Double.MAX_VALUE;
        if (pintersect != null) {
            anchor = StrokeGeometry.Point.Sub(pintersect, p1);
            anchorLength = anchor.length();
        }

        double dd = (int)(anchorLength / width);
        StrokeGeometry.Point p0p1 = StrokeGeometry.Point.Sub(p0, p1);
        double p0p1Length = p0p1.length();
        StrokeGeometry.Point p1p2 = StrokeGeometry.Point.Sub(p1, p2);
        double p1p2Length = p1p2.length();
        if (!(anchorLength > p0p1Length) && !(anchorLength > p1p2Length)) {
            verts.add(StrokeGeometry.Point.Add(p0, t0));
            verts.add(StrokeGeometry.Point.Sub(p0, t0));
            verts.add(StrokeGeometry.Point.Sub(p1, anchor));
            verts.add(StrokeGeometry.Point.Add(p0, t0));
            verts.add(StrokeGeometry.Point.Sub(p1, anchor));
            verts.add(StrokeGeometry.Point.Add(p1, t0));
            if (join.equals("round")) {
                StrokeGeometry.Point _p0 = StrokeGeometry.Point.Add(p1, t0);
                StrokeGeometry.Point _p1 = StrokeGeometry.Point.Add(p1, t2);
                StrokeGeometry.Point _p2 = StrokeGeometry.Point.Sub(p1, anchor);
                verts.add(_p0);
                verts.add(p1);
                verts.add(_p2);
                createRoundCap(p1, _p0, _p1, _p2, verts);
                verts.add(p1);
                verts.add(_p1);
                verts.add(_p2);
            } else {
                if (join.equals("bevel") || join.equals("miter") && dd >= miterLimit) {
                    verts.add(StrokeGeometry.Point.Add(p1, t0));
                    verts.add(StrokeGeometry.Point.Add(p1, t2));
                    verts.add(StrokeGeometry.Point.Sub(p1, anchor));
                }

                if (join.equals("miter") && dd < miterLimit) {
                    verts.add(pintersect);
                    verts.add(StrokeGeometry.Point.Add(p1, t0));
                    verts.add(StrokeGeometry.Point.Add(p1, t2));
                }
            }

            verts.add(StrokeGeometry.Point.Add(p2, t2));
            verts.add(StrokeGeometry.Point.Sub(p1, anchor));
            verts.add(StrokeGeometry.Point.Add(p1, t2));
            verts.add(StrokeGeometry.Point.Add(p2, t2));
            verts.add(StrokeGeometry.Point.Sub(p1, anchor));
            verts.add(StrokeGeometry.Point.Sub(p2, t2));
        } else {
            verts.add(StrokeGeometry.Point.Add(p0, t0));
            verts.add(StrokeGeometry.Point.Sub(p0, t0));
            verts.add(StrokeGeometry.Point.Add(p1, t0));
            verts.add(StrokeGeometry.Point.Sub(p0, t0));
            verts.add(StrokeGeometry.Point.Add(p1, t0));
            verts.add(StrokeGeometry.Point.Sub(p1, t0));
            if (join.equals("round")) {
                createRoundCap(p1, StrokeGeometry.Point.Add(p1, t0), StrokeGeometry.Point.Add(p1, t2), p2, verts);
            } else if (!join.equals("bevel") && (!join.equals("miter") || !(dd >= miterLimit))) {
                if (join.equals("miter") && dd < miterLimit && pintersect != null) {
                    verts.add(StrokeGeometry.Point.Add(p1, t0));
                    verts.add(p1);
                    verts.add(pintersect);
                    verts.add(StrokeGeometry.Point.Add(p1, t2));
                    verts.add(p1);
                    verts.add(pintersect);
                }
            } else {
                verts.add(p1);
                verts.add(StrokeGeometry.Point.Add(p1, t0));
                verts.add(StrokeGeometry.Point.Add(p1, t2));
            }

            verts.add(StrokeGeometry.Point.Add(p2, t2));
            verts.add(StrokeGeometry.Point.Sub(p1, t2));
            verts.add(StrokeGeometry.Point.Add(p1, t2));
            verts.add(StrokeGeometry.Point.Add(p2, t2));
            verts.add(StrokeGeometry.Point.Sub(p1, t2));
            verts.add(StrokeGeometry.Point.Sub(p2, t2));
        }
    }

    static class Attrs {
        String cap = "butt";
        String join = "bevel";
        float width = 1.0F;
        float miterLimit = 10.0F;
    }

    public static final class Point {
        double x;
        double y;
        StrokeGeometry.Point next;

        Point() {
            this.x = 0.0;
            this.y = 0.0;
        }

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        StrokeGeometry.Point set(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        StrokeGeometry.Point scalarMult(double f) {
            this.x *= f;
            this.y *= f;
            return this;
        }

        StrokeGeometry.Point perpendicular() {
            double x = this.x;
            this.x = -this.y;
            this.y = x;
            return this;
        }

        StrokeGeometry.Point invert() {
            this.x = -this.x;
            this.y = -this.y;
            return this;
        }

        double length() {
            return Math.sqrt(this.x * this.x + this.y * this.y);
        }

        StrokeGeometry.Point normalize() {
            double mod = this.length();
            this.x /= mod;
            this.y /= mod;
            return this;
        }

        double angle() {
            return this.y / this.x;
        }

        static double Angle(StrokeGeometry.Point p0, StrokeGeometry.Point p1) {
            return Math.atan2(p1.x - p0.x, p1.y - p0.y);
        }

        static StrokeGeometry.Point Add(StrokeGeometry.Point p0, StrokeGeometry.Point p1) {
            return StrokeGeometry.newPoint(p0.x + p1.x, p0.y + p1.y);
        }

        static StrokeGeometry.Point Sub(StrokeGeometry.Point p1, StrokeGeometry.Point p0) {
            return StrokeGeometry.newPoint(p1.x - p0.x, p1.y - p0.y);
        }

        static StrokeGeometry.Point Middle(StrokeGeometry.Point p0, StrokeGeometry.Point p1) {
            return Add(p0, p1).scalarMult(0.5);
        }
    }
}
