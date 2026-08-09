// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.collision;

import java.util.ArrayList;
import zombie.iso.Vector2;

public final class Polygon {
    public ArrayList<Vector2> points = new ArrayList<>(4);
    public ArrayList<Vector2> edges = new ArrayList<>(4);
    float x;
    float y;
    float x2;
    float y2;
    Vector2[] vecs = new Vector2[4];
    Vector2[] eds = new Vector2[4];
    static Vector2 temp = new Vector2();

    public void Set(float x, float y, float x2, float y2) {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
        this.points.clear();
        if (this.vecs[0] == null) {
            for (int n = 0; n < 4; n++) {
                this.vecs[n] = new Vector2();
                this.eds[n] = new Vector2();
            }
        }

        this.vecs[0].x = x;
        this.vecs[0].y = y;
        this.vecs[1].x = x2;
        this.vecs[1].y = y;
        this.vecs[2].x = x2;
        this.vecs[2].y = y2;
        this.vecs[3].x = x;
        this.vecs[3].y = y2;
        this.points.add(this.vecs[0]);
        this.points.add(this.vecs[1]);
        this.points.add(this.vecs[2]);
        this.points.add(this.vecs[3]);
        this.BuildEdges();
    }

    public Vector2 Center() {
        temp.x = this.x + (this.x2 - this.x) / 2.0F;
        temp.y = this.y + (this.y2 - this.y) / 2.0F;
        return temp;
    }

    public void BuildEdges() {
        this.edges.clear();

        for (int i = 0; i < this.points.size(); i++) {
            Vector2 p1 = this.points.get(i);
            Vector2 p2;
            if (i + 1 >= this.points.size()) {
                p2 = this.points.get(0);
            } else {
                p2 = this.points.get(i + 1);
            }

            this.eds[i].x = p2.x - p1.x;
            this.eds[i].y = p2.y - p1.y;
            this.edges.add(this.eds[i]);
        }
    }

    public void Offset(float x, float y) {
        for (int i = 0; i < this.points.size(); i++) {
            Vector2 p = this.points.get(i);
            p.x += x;
            p.y += y;
        }
    }

    public void Offset(Vector2 v) {
        this.Offset(v.x, v.y);
    }
}
