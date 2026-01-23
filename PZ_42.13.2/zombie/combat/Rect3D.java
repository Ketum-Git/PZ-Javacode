// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

import org.joml.Vector3f;
import zombie.iso.Vector3;

public class Rect3D {
    private final Vector3 min = new Vector3();
    private final Vector3 max = new Vector3();

    public void set(Vector3 min, Vector3 max) {
        this.min.x = min.x;
        this.min.y = min.y;
        this.min.z = min.z;
        this.max.x = max.x;
        this.max.y = max.y;
        this.max.z = max.z;
    }

    public void setMin(float x, float y, float z) {
        this.min.set(x, y, z);
    }

    public void setMax(float x, float y, float z) {
        this.max.set(x, y, z);
    }

    public Vector3f[] getCorners() {
        return new Vector3f[]{
            new Vector3f(this.min.x, this.min.y, this.min.z),
            new Vector3f(this.min.x, this.min.y, this.max.z),
            new Vector3f(this.min.x, this.max.y, this.min.z),
            new Vector3f(this.min.x, this.max.y, this.max.z),
            new Vector3f(this.max.x, this.min.y, this.min.z),
            new Vector3f(this.max.x, this.min.y, this.max.z),
            new Vector3f(this.max.x, this.max.y, this.min.z),
            new Vector3f(this.max.x, this.max.y, this.max.z)
        };
    }

    public Vector3f getCenter() {
        return new Vector3f((this.min.x + this.max.x) * 0.5F, (this.min.y + this.max.y) * 0.5F, (this.min.z + this.max.z) * 0.5F);
    }

    public float rayIntersection(Vector3f origin, Vector3f direction) {
        float tMin = (this.min.x - origin.x) / direction.x;
        float tMax = (this.max.x - origin.x) / direction.x;
        if (tMin > tMax) {
            float temp = tMin;
            tMin = tMax;
            tMax = temp;
        }

        float tyMin = (this.min.y - origin.y) / direction.y;
        float tyMax = (this.max.y - origin.y) / direction.y;
        if (tyMin > tyMax) {
            float temp = tyMin;
            tyMin = tyMax;
            tyMax = temp;
        }

        if (!(tMin > tyMax) && !(tyMin > tMax)) {
            tMin = Math.max(tMin, tyMin);
            tMax = Math.min(tMax, tyMax);
            float tzMin = (this.min.z - origin.z) / direction.z;
            float tzMax = (this.max.z - origin.z) / direction.z;
            if (tzMin > tzMax) {
                float temp = tzMin;
                tzMin = tzMax;
                tzMax = temp;
            }

            if (!(tMin > tzMax) && !(tzMin > tMax)) {
                tMin = Math.max(tMin, tzMin);
                tMax = Math.min(tMax, tzMax);
                return tMin > 0.0F ? tMin : Float.POSITIVE_INFINITY;
            } else {
                return Float.POSITIVE_INFINITY;
            }
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }
}
