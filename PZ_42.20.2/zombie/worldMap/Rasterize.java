// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

public final class Rasterize {
    final Rasterize.Edge edge1 = new Rasterize.Edge();
    final Rasterize.Edge edge2 = new Rasterize.Edge();
    final Rasterize.Edge edge3 = new Rasterize.Edge();

    void scanLine(int x0, int x1, int y, Rasterize.ICallback s) {
        for (int x = x0; x < x1; x++) {
            s.accept(x, y);
        }
    }

    void scanSpan(Rasterize.Edge e0, Rasterize.Edge e1, int min, int max, Rasterize.ICallback s) {
        int y0 = (int)Math.max((double)min, Math.floor(e1.y0));
        int y1 = (int)Math.min((double)max, Math.ceil(e1.y1));
        if (e0.x0 == e1.x0 && e0.y0 == e1.y0) {
            if (e0.x0 + e1.dy / e0.dy * e0.dx < e1.x1) {
                Rasterize.Edge tmp = e0;
                e0 = e1;
                e1 = tmp;
            }
        } else if (e0.x1 - e1.dy / e0.dy * e0.dx < e1.x0) {
            Rasterize.Edge tmp = e0;
            e0 = e1;
            e1 = tmp;
        }

        double m0 = e0.dx / e0.dy;
        double m1 = e1.dx / e1.dy;
        double d0 = e0.dx > 0.0F ? 1.0 : 0.0;
        double d1 = e1.dx < 0.0F ? 1.0 : 0.0;

        for (int y = y0; y < y1; y++) {
            double x0 = m0 * Math.max(0.0, Math.min((double)e0.dy, y + d0 - e0.y0)) + e0.x0;
            double x1 = m1 * Math.max(0.0, Math.min((double)e1.dy, y + d1 - e1.y0)) + e1.x0;
            this.scanLine((int)Math.floor(x1), (int)Math.ceil(x0), y, s);
        }
    }

    public void scanTriangle(float xa, float ya, float xb, float yb, float xc, float yc, int min, int max, Rasterize.ICallback s) {
        Rasterize.Edge ab = this.edge1.init(xa, ya, xb, yb);
        Rasterize.Edge bc = this.edge2.init(xb, yb, xc, yc);
        Rasterize.Edge ca = this.edge3.init(xc, yc, xa, ya);
        if (ab.dy > ca.dy) {
            Rasterize.Edge tmp = ab;
            ab = ca;
            ca = tmp;
        }

        if (bc.dy > ca.dy) {
            Rasterize.Edge tmp = bc;
            bc = ca;
            ca = tmp;
        }

        if (ab.dy > 0.0F) {
            this.scanSpan(ca, ab, min, max, s);
        }

        if (bc.dy > 0.0F) {
            this.scanSpan(ca, bc, min, max, s);
        }
    }

    private static final class Edge {
        float x0;
        float y0;
        float x1;
        float y1;
        float dx;
        float dy;

        Rasterize.Edge init(float x0, float y0, float x1, float y1) {
            if (y0 > y1) {
                this.x0 = x1;
                this.y0 = y1;
                this.x1 = x0;
                this.y1 = y0;
            } else {
                this.x0 = x0;
                this.y0 = y0;
                this.x1 = x1;
                this.y1 = y1;
            }

            this.dx = this.x1 - this.x0;
            this.dy = this.y1 - this.y0;
            return this;
        }
    }

    public interface ICallback {
        void accept(int arg0, int arg1);
    }
}
