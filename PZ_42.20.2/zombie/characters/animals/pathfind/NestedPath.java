// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;

public final class NestedPath {
    float[] points;
    int inset;
    float minX;
    float minY;
    float maxX;
    float maxY;
    float length;

    public int getNumPoints() {
        return this.points.length / 2;
    }

    public float getX(int index) {
        return this.points[index * 2];
    }

    public float getY(int index) {
        return this.points[index * 2 + 1];
    }

    public float getLength() {
        return this.length;
    }

    public boolean getPointOn(float t, Vector2f out) {
        t = PZMath.clampFloat(t, 0.0F, 1.0F);
        out.set(0.0F);
        float length = this.getLength();
        if (length <= 0.0F) {
            return false;
        } else {
            float distanceFromStart = length * t;
            float segmentStart = 0.0F;

            for (int i = 0; i < this.getNumPoints(); i++) {
                float x1 = this.getX(i);
                float y1 = this.getY(i);
                float x2 = this.getX((i + 1) % this.getNumPoints());
                float y2 = this.getY((i + 1) % this.getNumPoints());
                float segmentLength = Vector2f.length(x2 - x1, y2 - y1);
                if (segmentStart + segmentLength >= distanceFromStart) {
                    float f = (distanceFromStart - segmentStart) / segmentLength;
                    out.set(x1 + (x2 - x1) * f, y1 + (y2 - y1) * f);
                    return true;
                }

                segmentStart += segmentLength;
            }

            return false;
        }
    }

    boolean pickRandomPointOn(Vector2f out) {
        return this.getPointOn(Rand.Next(0.0F, 1.0F), out);
    }

    public float getClosestPointOn(float px, float py, Vector2f out) {
        float closestDist = Float.MAX_VALUE;
        float distanceFromStart = 0.0F;
        float length = 0.0F;

        for (int i = 0; i < this.getNumPoints(); i++) {
            float x1 = this.getX(i);
            float y1 = this.getY(i);
            float x2 = this.getX((i + 1) % this.getNumPoints());
            float y2 = this.getY((i + 1) % this.getNumPoints());
            float segmentLength = Vector2f.distance(x1, y1, x2, y2);
            double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = x1 + u * (x2 - x1);
            double yu = y1 + u * (y2 - y1);
            if (u <= 0.0) {
                xu = x1;
                yu = y1;
                u = 0.0;
            } else if (u >= 1.0) {
                xu = x2;
                yu = y2;
                u = 1.0;
            }

            float dist = IsoUtils.DistanceToSquared(px, py, (float)xu, (float)yu);
            if (dist < closestDist) {
                closestDist = dist;
                out.set(xu, yu);
                distanceFromStart = length + (float)(u * segmentLength);
            }

            length += segmentLength;
        }

        return distanceFromStart / length;
    }

    public float getDistanceOfPointFromStart(int pointIndex) {
        float length = 0.0F;

        for (int i = 0; i < pointIndex; i++) {
            float x1 = this.getX(i);
            float y1 = this.getY(i);
            float x2 = this.getX((i + 1) % this.getNumPoints());
            float y2 = this.getY((i + 1) % this.getNumPoints());
            length += Vector2f.length(x2 - x1, y2 - y1);
        }

        return length;
    }
}
