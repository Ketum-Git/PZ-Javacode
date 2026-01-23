// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.reflect;

import java.lang.reflect.Array;

public final class ArrayReflection {
    public static Object newInstance(Class<?> c, int size) {
        return Array.newInstance(c, size);
    }

    public static int getLength(Object array) {
        return Array.getLength(array);
    }

    public static Object get(Object array, int index) {
        return Array.get(array, index);
    }

    public static void set(Object array, int index, Object value) {
        Array.set(array, index, value);
    }
}
