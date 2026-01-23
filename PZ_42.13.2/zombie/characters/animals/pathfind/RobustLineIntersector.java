// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class RobustLineIntersector {
    public static final int NO_INTERSECTION = 0;
    public static final int POINT_INTERSECTION = 1;
    public static final int COLLINEAR_INTERSECTION = 2;

    public static int computeIntersection(Vector2f p1, Vector2f p2, Vector2f q1, Vector2f q2, Vector2f intPt1, Vector2f intPt2) {
        if (!Envelope.intersects(p1, p2, q1, q2)) {
            return 0;
        } else {
            int Pq1 = Orientation.index(p1, p2, q1);
            int Pq2 = Orientation.index(p1, p2, q2);
            if ((Pq1 <= 0 || Pq2 <= 0) && (Pq1 >= 0 || Pq2 >= 0)) {
                int Qp1 = Orientation.index(q1, q2, p1);
                int Qp2 = Orientation.index(q1, q2, p2);
                if ((Qp1 <= 0 || Qp2 <= 0) && (Qp1 >= 0 || Qp2 >= 0)) {
                    boolean collinear = Pq1 == 0 && Pq2 == 0 && Qp1 == 0 && Qp2 == 0;
                    return collinear ? computeCollinearIntersection(p1, p2, q1, q2, intPt1, intPt2) : 0;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    static int computeCollinearIntersection(Vector2f p1, Vector2f p2, Vector2f q1, Vector2f q2, Vector2f intPt1, Vector2f intPt2) {
        boolean p1q1p2 = Envelope.intersects(p1, p2, q1);
        boolean p1q2p2 = Envelope.intersects(p1, p2, q2);
        boolean q1p1q2 = Envelope.intersects(q1, q2, p1);
        boolean q1p2q2 = Envelope.intersects(q1, q2, p2);
        if (p1q1p2 && p1q2p2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(q1);
                intPt2.set(q2);
            }

            return 2;
        } else if (q1p1q2 && q1p2q2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(p1);
                intPt2.set(p2);
            }

            return 2;
        } else if (p1q1p2 && q1p1q2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(q1);
                intPt2.set(p1);
            }

            return q1.equals(p1) && !p1q2p2 && !q1p2q2 ? 1 : 2;
        } else if (p1q1p2 && q1p2q2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(q1);
                intPt2.set(p2);
            }

            return q1.equals(p2) && !p1q2p2 && !q1p1q2 ? 1 : 2;
        } else if (p1q2p2 && q1p1q2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(q2);
                intPt2.set(p1);
            }

            return q2.equals(p1) && !p1q1p2 && !q1p2q2 ? 1 : 2;
        } else if (p1q2p2 && q1p2q2) {
            if (intPt1 != null && intPt2 != null) {
                intPt1.set(q2);
                intPt2.set(p2);
            }

            return q2.equals(p2) && !p1q1p2 && !q1p1q2 ? 1 : 2;
        } else {
            return 0;
        }
    }
}
