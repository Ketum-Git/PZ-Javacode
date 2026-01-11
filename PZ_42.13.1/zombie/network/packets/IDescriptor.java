// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public interface IDescriptor {
    default void getClassDescription(StringBuilder s, Class<?> cls, HashSet<Object> excludedObjects) {
        boolean isFirst = false;

        for (Field field : cls.getDeclaredFields()) {
            Annotation[] annotations = field.getAnnotationsByType(JSONField.class);
            if (annotations.length > 0) {
                String name = field.getName();

                try {
                    field.setAccessible(true);
                    Object obj = field.get(this);
                    if (isFirst) {
                        s.append(", ");
                    } else {
                        isFirst = true;
                    }

                    s.append("\"").append(name).append("\" : ").append(this.toJSON(obj, excludedObjects));
                } catch (IllegalAccessException var12) {
                    DebugLog.Multiplayer
                        .printException(var12, "INetworkPacketField.getDescription: can't get the value of the " + name + " field", LogSeverity.Error);
                }
            }
        }
    }

    default String getDescription(HashSet<Object> excludedObjects) {
        StringBuilder s = new StringBuilder("{ ");

        for (Class<?> cls = this.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            if (cls != this.getClass()) {
                s.append(" , ");
            }

            s.append("\"").append(cls.getSimpleName()).append("\": { ");
            this.getClassDescription(s, cls, excludedObjects);
            s.append(" } ");
        }

        s.append(" }");
        return s.toString();
    }

    default String getDescription() {
        return this.getDescription(new HashSet<>());
    }

    private String toJSON(Object obj, HashSet<Object> excludedObjects) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof INetworkPacketField iNetworkPacketField) {
            if (excludedObjects.contains(obj)) {
                return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
            } else {
                excludedObjects.add(obj);
                return iNetworkPacketField.getDescription(excludedObjects);
            }
        } else if (obj instanceof String) {
            return "\"" + obj + "\"";
        } else if (!(obj instanceof Boolean)
            && !(obj instanceof Byte)
            && !(obj instanceof Short)
            && !(obj instanceof Integer)
            && !(obj instanceof Long)
            && !(obj instanceof Float)
            && !(obj instanceof Double)) {
            if (obj instanceof Iterable<?> iterable) {
                if (excludedObjects.contains(obj)) {
                    return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
                } else {
                    excludedObjects.add(obj);
                    StringBuilder s = new StringBuilder("[");
                    boolean isFirst = true;

                    for (Object o : iterable) {
                        if (!isFirst) {
                            s.append(", ");
                        }

                        s.append(this.toJSON(o, excludedObjects));
                        isFirst = false;
                    }

                    return s + "]";
                }
            } else if (obj instanceof KahluaTable kahluaTable) {
                if (excludedObjects.contains(obj)) {
                    return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
                } else {
                    excludedObjects.add(obj);
                    KahluaTableIterator it = kahluaTable.iterator();
                    StringBuilder s = new StringBuilder("{");
                    boolean isFirst = true;

                    while (it.advance()) {
                        if (!isFirst) {
                            s.append(", ");
                        }

                        Object key = it.getKey();
                        if (!"netAction".equals(key)) {
                            s.append(this.toJSON(it.getKey(), excludedObjects));
                            s.append(": ");
                            s.append(this.toJSON(it.getValue(), excludedObjects));
                            isFirst = false;
                        }
                    }

                    s.append("}");
                    return s.toString();
                }
            } else if (excludedObjects.contains(obj)) {
                return "\"Previously described " + obj.getClass().getSimpleName() + "\"";
            } else {
                excludedObjects.add(obj);
                return "\"" + obj + "\"";
            }
        } else {
            return obj.toString();
        }
    }
}
