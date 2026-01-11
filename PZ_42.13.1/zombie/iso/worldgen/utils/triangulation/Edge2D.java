// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils.triangulation;

import java.util.Objects;

public class Edge2D {
    public Vector2D a;
    public Vector2D b;

    public Edge2D(Vector2D a, Vector2D b) {
        boolean test = a.magSqrt() > b.magSqrt();
        this.a = test ? a : b;
        this.b = test ? b : a;
    }

    public double mag() {
        return Math.sqrt(this.magSqrt());
    }

    public double magSqrt() {
        return (this.a.x - this.b.x) * (this.a.x - this.b.x) + (this.a.y - this.b.y) * (this.a.y - this.b.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Edge2D edge2D = (Edge2D)o;
            return Objects.equals(this.a, edge2D.a) && Objects.equals(this.b, edge2D.b);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.a, this.b);
    }
}
