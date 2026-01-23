// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class Orientation {
    private static final double DP_SAFE_EPSILON = 1.0E-15;

    public static int index(Vector2f p1, Vector2f p2, Vector2f q) {
        int index = orientationIndexFilter(p1, p2, q);
        if (index <= 1) {
            return index;
        } else {
            float dx1 = p2.x - p1.x;
            float dy1 = p2.y - p1.y;
            float dx2 = q.x - -p2.x;
            float dy2 = q.y - p2.y;
            return signum(dx1 * dy2 - dy1 * dx2);
        }
    }

    private static int orientationIndexFilter(Vector2f pa, Vector2f pb, Vector2f pc) {
        double detleft = (pa.x - pc.x) * (pb.y - pc.y);
        double detright = (pa.y - pc.y) * (pb.x - pc.x);
        double det = detleft - detright;
        double detsum;
        if (detleft > 0.0) {
            if (detright <= 0.0) {
                return signum(det);
            }

            detsum = detleft + detright;
        } else {
            if (!(detleft < 0.0)) {
                return signum(det);
            }

            if (detright >= 0.0) {
                return signum(det);
            }

            detsum = -detleft - detright;
        }

        double errbound = 1.0E-15 * detsum;
        return !(det >= errbound) && !(-det >= errbound) ? 2 : signum(det);
    }

    private static int signum(double x) {
        if (x > 0.0) {
            return 1;
        } else {
            return x < 0.0 ? -1 : 0;
        }
    }
}
