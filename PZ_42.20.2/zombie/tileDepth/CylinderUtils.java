// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.util.Arrays;
import org.joml.Vector3f;
import zombie.vehicles.UI3DScene;

public final class CylinderUtils {
    public static boolean intersect(float radius, float height, UI3DScene.Ray rayIn, CylinderUtils.IntersectionRecord outRecord) {
        Vector3f center = new Vector3f(0.0F);
        Vector3f eminusc = rayIn.origin.sub(center, new Vector3f());
        double a = Math.pow(rayIn.direction.x, 2.0) + Math.pow(rayIn.direction.y, 2.0);
        double b = 2.0F * (rayIn.direction.x * eminusc.x + rayIn.direction.y * eminusc.y);
        double c = Math.pow(eminusc.x, 2.0) + Math.pow(eminusc.y, 2.0) - Math.pow(radius, 2.0);
        double discriminant = b * b - 4.0 * a * c;
        if (discriminant < 0.0) {
            return false;
        } else {
            double t1 = Math.min((-b + Math.sqrt(discriminant)) / (2.0 * a), (-b - Math.sqrt(discriminant)) / (2.0 * a));
            double t2 = (height / 2.0 - eminusc.z) / rayIn.direction.z;
            double t3 = (-height / 2.0 - eminusc.z) / rayIn.direction.z;
            double[] tarr = new double[]{t1, t2, t3};
            Arrays.sort(tarr);
            Double t = null;

            for (double x : tarr) {
                CylinderUtils.IntersectionRecord tmp = new CylinderUtils.IntersectionRecord();
                rayIn.origin.add(getScaledVector(rayIn.direction, (float)x), tmp.location);
                if (x == t1) {
                    if (Math.abs(tmp.location.z - center.z) < height / 2.0) {
                        outRecord.normal.set(tmp.location.x - center.x, tmp.location.y - center.y, 0.0F);
                        outRecord.normal.normalize();
                        t = x;
                        break;
                    }
                } else if (Math.pow(tmp.location.x - center.x, 2.0) + Math.pow(tmp.location.y - center.y, 2.0) - Math.pow(radius, 2.0) <= 0.0) {
                    if (x == t2) {
                        outRecord.normal.set(0.0F, 0.0F, 1.0F);
                    } else if (x == t3) {
                        outRecord.normal.set(0.0F, 0.0F, -1.0F);
                    }

                    t = x;
                    break;
                }
            }

            if (t == null) {
                return false;
            } else {
                rayIn.t = t.floatValue();
                outRecord.t = t;
                rayIn.origin.add(getScaledVector(rayIn.direction, t.floatValue()), outRecord.location);
                return true;
            }
        }
    }

    private static Vector3f getScaledVector(Vector3f v, float x) {
        return new Vector3f(v).mul(x);
    }

    public static final class IntersectionRecord {
        public final Vector3f location = new Vector3f();
        public final Vector3f normal = new Vector3f();
        double t;
    }
}
