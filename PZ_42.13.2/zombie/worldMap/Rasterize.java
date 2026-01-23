// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

public final class Rasterize {
    final Rasterize.Edge edge1 = new Rasterize.Edge();
    final Rasterize.Edge edge2 = new Rasterize.Edge();
    final Rasterize.Edge edge3 = new Rasterize.Edge();

    void scanLine(int _x0, int _x1, int _y, Rasterize.ICallback _s) {
        for (int x = _x0; x < _x1; x++) {
            _s.accept(x, _y);
        }
    }

    void scanSpan(Rasterize.Edge _e0, Rasterize.Edge _e1, int _min, int _max, Rasterize.ICallback _s) {
        int y0 = (int)Math.max((double)_min, Math.floor(_e1.y0));
        int y1 = (int)Math.min((double)_max, Math.ceil(_e1.y1));
        if (_e0.x0 == _e1.x0 && _e0.y0 == _e1.y0) {
            if (_e0.x0 + _e1.dy / _e0.dy * _e0.dx < _e1.x1) {
                Rasterize.Edge tmp = _e0;
                _e0 = _e1;
                _e1 = tmp;
            }
        } else if (_e0.x1 - _e1.dy / _e0.dy * _e0.dx < _e1.x0) {
            Rasterize.Edge tmp = _e0;
            _e0 = _e1;
            _e1 = tmp;
        }

        double m0 = _e0.dx / _e0.dy;
        double m1 = _e1.dx / _e1.dy;
        double d0 = _e0.dx > 0.0F ? 1.0 : 0.0;
        double d1 = _e1.dx < 0.0F ? 1.0 : 0.0;

        for (int y = y0; y < y1; y++) {
            double x0 = m0 * Math.max(0.0, Math.min((double)_e0.dy, y + d0 - _e0.y0)) + _e0.x0;
            double x1 = m1 * Math.max(0.0, Math.min((double)_e1.dy, y + d1 - _e1.y0)) + _e1.x0;
            this.scanLine((int)Math.floor(x1), (int)Math.ceil(x0), y, _s);
        }
    }

    public void scanTriangle(float _xa, float _ya, float _xb, float _yb, float _xc, float _yc, int _min, int _max, Rasterize.ICallback _s) {
        Rasterize.Edge ab = this.edge1.init(_xa, _ya, _xb, _yb);
        Rasterize.Edge bc = this.edge2.init(_xb, _yb, _xc, _yc);
        Rasterize.Edge ca = this.edge3.init(_xc, _yc, _xa, _ya);
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
            this.scanSpan(ca, ab, _min, _max, _s);
        }

        if (bc.dy > 0.0F) {
            this.scanSpan(ca, bc, _min, _max, _s);
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
