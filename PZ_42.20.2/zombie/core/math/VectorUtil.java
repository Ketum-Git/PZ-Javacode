// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math;

import org.lwjgl.util.vector.Vector3f;

public class VectorUtil {
    public static Vector3f addScaled(Vector3f a, Vector3f b, float scale, Vector3f result) {
        result.set(a.x + b.x * scale, a.y + b.y * scale, a.z + b.z * scale);
        return result;
    }

    public static Vector3f setScaled(Vector3f a, float scale, Vector3f result) {
        result.set(a.x * scale, a.y * scale, a.z * scale);
        return result;
    }
}
