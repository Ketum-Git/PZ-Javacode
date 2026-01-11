// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.lang.reflect.Field;

public class FieldWalker {
    public static void walkFields(Object obj, String parentName, FieldWalker.TriConsumer<String, Object, Field> consumer) {
        walkFields(obj, parentName, consumer, true);
    }

    public static void walkFields(Object obj, String parentName, FieldWalker.TriConsumer<String, Object, Field> consumer, boolean recursive) {
        if (obj != null) {
            Field[] fields = obj.getClass().getFields();

            for (Field field : fields) {
                String fullName = parentName.isEmpty() ? field.getName() : parentName + "." + field.getName();

                try {
                    Class<?> type = field.getType();
                    if (type == float.class || type == double.class || type == int.class) {
                        consumer.accept(fullName, obj, field);
                    } else if (recursive) {
                        Object child = field.get(obj);
                        walkFields(child, fullName, consumer, true);
                    }
                } catch (IllegalAccessException var12) {
                    var12.printStackTrace();
                }
            }
        }
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T var1, U var2, V var3);
    }
}
