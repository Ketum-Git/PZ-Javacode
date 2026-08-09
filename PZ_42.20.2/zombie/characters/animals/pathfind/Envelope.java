// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;

public final class Envelope {
    public static boolean intersects(Vector2f p1, Vector2f p2, Vector2f q) {
        return q.x >= (p1.x < p2.x ? p1.x : p2.x)
            && q.x <= (p1.x > p2.x ? p1.x : p2.x)
            && q.y >= (p1.y < p2.y ? p1.y : p2.y)
            && q.y <= (p1.y > p2.y ? p1.y : p2.y);
    }

    public static boolean intersects(Vector2f p1, Vector2f p2, Vector2f q1, Vector2f q2) {
        double minq = Math.min(q1.x, q2.x);
        double maxq = Math.max(q1.x, q2.x);
        double minp = Math.min(p1.x, p2.x);
        double maxp = Math.max(p1.x, p2.x);
        if (minp > maxq) {
            return false;
        } else if (maxp < minq) {
            return false;
        } else {
            minq = Math.min(q1.y, q2.y);
            maxq = Math.max(q1.y, q2.y);
            minp = Math.min(p1.y, p2.y);
            maxp = Math.max(p1.y, p2.y);
            return minp > maxq ? false : !(maxp < minq);
        }
    }
}
