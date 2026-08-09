// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

public class Type {
    public static <R, I> R tryCastTo(I val, Class<R> clazz) {
        return clazz.isInstance(val) ? clazz.cast(val) : null;
    }

    public static boolean asBoolean(Object val) {
        return asBoolean(val, false);
    }

    public static boolean asBoolean(Object val, boolean defaultVal) {
        if (val == null) {
            return defaultVal;
        } else {
            return val instanceof Boolean valSt ? valSt : defaultVal;
        }
    }
}
