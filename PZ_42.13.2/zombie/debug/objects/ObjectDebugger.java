// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.objects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.entity.util.assoc.AssocArray;

public class ObjectDebugger {
    private static final ConcurrentLinkedDeque<ArrayList<String>> array_list_pool = new ConcurrentLinkedDeque<>();
    private static final String tab_str = "  ";
    private static final ThreadLocal<ObjectDebugger.Parser> localParser = new ThreadLocal<ObjectDebugger.Parser>() {
        protected ObjectDebugger.Parser initialValue() {
            return new ObjectDebugger.Parser();
        }
    };
    private static final ConcurrentLinkedDeque<ObjectDebugger.LogObject> pool_object = new ConcurrentLinkedDeque<>();
    private static final Comparator<ObjectDebugger.LogField> fieldComparator = (object1, object2) -> {
        if (object1.value instanceof ObjectDebugger.LogEntry && !(object2.value instanceof ObjectDebugger.LogEntry)) {
            return 1;
        } else if (!(object1.value instanceof ObjectDebugger.LogEntry) && object2.value instanceof ObjectDebugger.LogEntry) {
            return -1;
        } else if (object1.isFunction && !object2.isFunction) {
            return 1;
        } else {
            return !object1.isFunction && object2.isFunction ? -1 : object1.field.compareTo(object2.field);
        }
    };
    private static final ConcurrentLinkedDeque<ObjectDebugger.LogField> pool_field = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<ObjectDebugger.LogList> pool_list = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<ObjectDebugger.LogMap> pool_map = new ConcurrentLinkedDeque<>();

    public static ArrayList<String> AllocList() {
        ArrayList<String> list = array_list_pool.poll();
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void ReleaseList(ArrayList<String> list) {
        list.clear();
        array_list_pool.offer(list);
    }

    public static void Log(Object o) {
        Log(o, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, boolean forceAccessFields) {
        Log(o, Integer.MAX_VALUE, forceAccessFields, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, boolean forceAccessFields, boolean useClassAnnotations) {
        Log(o, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth) {
        Log(o, inheritanceDepth, true, true, Integer.MAX_VALUE);
    }

    public static void Log(Object o, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        Log(DebugLog.General, o, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);
    }

    public static void Log(DebugLogStream logStream, Object o) {
        Log(logStream, o, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE);
    }

    public static void Log(DebugLogStream logStream, Object o, boolean forceAccessFields) {
        Log(logStream, o, Integer.MAX_VALUE, forceAccessFields, true, Integer.MAX_VALUE);
    }

    public static void Log(DebugLogStream logStream, Object o, boolean forceAccessFields, boolean useClassAnnotations) {
        Log(logStream, o, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void Log(DebugLogStream logStream, Object o, int inheritanceDepth) {
        Log(logStream, o, inheritanceDepth, true, true, Integer.MAX_VALUE);
    }

    public static void Log(DebugLogStream logStream, Object o, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        if (logStream == null) {
            logStream = DebugLog.General;
        }

        if (o == null) {
            logStream.println("[null]");
        } else if (!Core.debug) {
            logStream.println("ObjectDebugger can only run in debug mode.");
        } else {
            try {
                ObjectDebugger.Parser parser = localParser.get();
                parser.parse(o, parser.lines, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);

                for (int i = 0; i < parser.lines.size(); i++) {
                    logStream.println(parser.lines.get(i));
                }
            } catch (Exception var11) {
                var11.printStackTrace();
            } finally {
                localParser.get().reset();
            }
        }
    }

    public static void GetLines(Object o, ArrayList<String> list) {
        GetLines(o, list, Integer.MAX_VALUE, true, false, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, boolean forceAccessFields) {
        GetLines(o, list, Integer.MAX_VALUE, forceAccessFields, false, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, boolean forceAccessFields, boolean useClassAnnotations) {
        GetLines(o, list, Integer.MAX_VALUE, forceAccessFields, useClassAnnotations, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth) {
        GetLines(o, list, inheritanceDepth, true, false, Integer.MAX_VALUE);
    }

    public static void GetLines(Object o, ArrayList<String> list, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) {
        if (o == null) {
            list.add("[null]");
        } else if (!Core.debug) {
            list.add("ObjectDebugger can only run in debug mode.");
        } else {
            try {
                localParser.get().parse(o, list, inheritanceDepth, forceAccessFields, useClassAnnotations, memberDepth);
            } catch (Exception var10) {
                var10.printStackTrace();
            } finally {
                localParser.get().reset();
            }
        }
    }

    protected static ObjectDebugger.LogObject alloc_log_object(Object o) {
        ObjectDebugger.LogObject log = pool_object.poll();
        if (log == null) {
            log = new ObjectDebugger.LogObject();
        }

        log.object = o;
        return log;
    }

    protected static ObjectDebugger.LogField alloc_log_field(String field, Object value, boolean isFunction) {
        ObjectDebugger.LogField log = pool_field.poll();
        if (log == null) {
            log = new ObjectDebugger.LogField();
        }

        log.field = field;
        log.value = value;
        log.isFunction = isFunction;
        return log;
    }

    protected static ObjectDebugger.LogList alloc_log_list() {
        ObjectDebugger.LogList log = pool_list.poll();
        if (log == null) {
            log = new ObjectDebugger.LogList();
        }

        return log;
    }

    protected static ObjectDebugger.LogMap alloc_log_map() {
        ObjectDebugger.LogMap log = pool_map.poll();
        if (log == null) {
            log = new ObjectDebugger.LogMap();
        }

        return log;
    }

    private abstract static class LogCollection extends ObjectDebugger.LogEntry {
        protected abstract int size();
    }

    private abstract static class LogEntry {
        protected abstract void build(ArrayList<String> var1, int var2);

        protected abstract void reset();

        protected abstract void release();

        protected abstract void sort();

        protected String print(Object value) {
            if (value instanceof String) {
                return "\"" + value + "\"";
            } else if (value instanceof Byte) {
                return "(byte) " + value;
            } else if (value instanceof Short) {
                return "(short) " + value;
            } else if (value instanceof Integer) {
                return "(int) " + value;
            } else if (value instanceof Float) {
                return "(float) " + value;
            } else if (value instanceof Double) {
                return "(double) " + value;
            } else if (value instanceof Long) {
                return "(long) " + value;
            } else {
                return value instanceof Enum ? value.getClass().getSimpleName() + "." + value : Objects.toString(value);
            }
        }
    }

    private static class LogField extends ObjectDebugger.LogEntry {
        protected String field;
        protected Object value;
        protected boolean isFunction;

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            if (this.value instanceof ObjectDebugger.LogEntry logEntry) {
                if (this.value instanceof ObjectDebugger.LogCollection logCollection && logCollection.size() == 0) {
                    list.add("  ".repeat(tabs) + this.getPrintName() + " = <EMPTY> " + this.getValueType());
                } else {
                    list.add("  ".repeat(tabs) + this.getPrintName() + " = " + this.getValueType());
                    list.add("  ".repeat(tabs) + "{");
                    if (this.value instanceof ObjectDebugger.LogObject logObject) {
                        logObject.buildMembers(list, tabs + 1);
                    } else {
                        logEntry.build(list, tabs + 1);
                    }

                    list.add("  ".repeat(tabs) + "}");
                }
            } else {
                list.add("  ".repeat(tabs) + this.getPrintName() + " = " + this.print(this.value));
            }
        }

        private String getPrintName() {
            return this.isFunction ? "func:" + this.field + "()" : this.field;
        }

        private String getValueType() {
            if (this.value instanceof ObjectDebugger.LogObject logObject) {
                return logObject.getHeader();
            } else if (this.value instanceof ObjectDebugger.LogList) {
                return "(List<T>)";
            } else {
                return this.value instanceof ObjectDebugger.LogMap ? "(Map<K,V>)" : "";
            }
        }

        @Override
        protected void reset() {
            this.field = null;
            if (this.value instanceof ObjectDebugger.LogEntry logEntry) {
                logEntry.release();
            }

            this.value = null;
            this.isFunction = false;
        }

        @Override
        protected void release() {
            this.reset();
            ObjectDebugger.pool_field.offer(this);
        }

        @Override
        protected void sort() {
            if (this.value instanceof ObjectDebugger.LogEntry logEntry) {
                logEntry.sort();
            }
        }
    }

    private static class LogList extends ObjectDebugger.LogCollection {
        private final List<Object> elements = new ArrayList<>();

        protected void addElement(Object element) {
            this.elements.add(element);
        }

        @Override
        protected int size() {
            return this.elements.size();
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            for (int i = 0; i < this.elements.size(); i++) {
                Object o = this.elements.get(i);
                if (o instanceof ObjectDebugger.LogObject logObject) {
                    list.add("  ".repeat(tabs) + "[" + i + "] = " + logObject.getHeader());
                    list.add("  ".repeat(tabs) + "{");
                    logObject.buildMembers(list, tabs + 1);
                    list.add("  ".repeat(tabs) + "}");
                } else {
                    list.add("  ".repeat(tabs) + "[" + i + "] = " + this.print(o));
                }
            }
        }

        @Override
        protected void reset() {
            for (int i = 0; i < this.elements.size(); i++) {
                if (this.elements.get(i) instanceof ObjectDebugger.LogEntry logEntry) {
                    logEntry.release();
                }
            }

            this.elements.clear();
        }

        @Override
        protected void release() {
            this.reset();
            ObjectDebugger.pool_list.offer(this);
        }

        @Override
        protected void sort() {
            for (int i = 0; i < this.elements.size(); i++) {
                if (this.elements.get(i) instanceof ObjectDebugger.LogEntry logEntry) {
                    logEntry.sort();
                }
            }
        }
    }

    private static class LogMap extends ObjectDebugger.LogCollection {
        private final Map<Object, Object> elements = new HashMap<>();

        protected void putElement(Object key, Object value) {
            this.elements.put(key, value);
        }

        @Override
        protected int size() {
            return this.elements.size();
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            for (Entry<Object, Object> entry : this.elements.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof ObjectDebugger.LogObject logObject) {
                    list.add("  ".repeat(tabs) + "[" + key + "] = " + logObject.getHeader());
                    list.add("  ".repeat(tabs) + "{");
                    logObject.buildMembers(list, tabs + 1);
                    list.add("  ".repeat(tabs) + "}");
                } else {
                    list.add("  ".repeat(tabs) + "[" + key + "] = " + this.print(val));
                }
            }
        }

        @Override
        protected void reset() {
            for (Entry<Object, Object> entry : this.elements.entrySet()) {
                if (entry.getValue() instanceof ObjectDebugger.LogEntry logEntry) {
                    logEntry.release();
                }
            }

            this.elements.clear();
        }

        @Override
        protected void release() {
            this.reset();
            ObjectDebugger.pool_map.offer(this);
        }

        @Override
        protected void sort() {
            for (Entry<Object, Object> entry : this.elements.entrySet()) {
                if (entry.getValue() instanceof ObjectDebugger.LogEntry logEntry) {
                    logEntry.sort();
                }
            }
        }
    }

    private static class LogObject extends ObjectDebugger.LogEntry {
        private Object object;
        protected final List<ObjectDebugger.LogField> members = new ArrayList<>();

        protected boolean containsMember(String fieldName) {
            for (int i = 0; i < this.members.size(); i++) {
                if (this.members.get(i).field.equals(fieldName)) {
                    return true;
                }
            }

            return false;
        }

        protected void addMember(ObjectDebugger.LogField entry) {
            if (!this.members.contains(entry)) {
                this.members.add(entry);
            }
        }

        protected String getHeader() {
            return "[" + this.object.getClass().getCanonicalName() + "]";
        }

        @Override
        protected void build(ArrayList<String> list, int tabs) {
            list.add("  ".repeat(tabs) + this.getHeader());
            list.add("  ".repeat(tabs) + "{");
            this.buildMembers(list, tabs + 1);
            list.add("  ".repeat(tabs) + "}");
        }

        protected void buildMembers(ArrayList<String> list, int tabs) {
            for (int i = 0; i < this.members.size(); i++) {
                ObjectDebugger.LogField entry = this.members.get(i);
                entry.build(list, tabs);
            }
        }

        @Override
        protected void reset() {
            this.object = null;

            for (int i = 0; i < this.members.size(); i++) {
                ObjectDebugger.LogField entry = this.members.get(i);
                entry.release();
            }

            this.members.clear();
        }

        @Override
        protected void release() {
            this.reset();
            ObjectDebugger.pool_object.offer(this);
        }

        @Override
        protected void sort() {
            for (int i = 0; i < this.members.size(); i++) {
                ObjectDebugger.LogField entry = this.members.get(i);
                entry.sort();
            }

            this.members.sort(ObjectDebugger.fieldComparator);
        }
    }

    private static class Parser {
        private final ArrayList<String> lines = new ArrayList<>();
        private final Set<Object> parsedObjects = new HashSet<>();
        private int originalInheritanceDepth = Integer.MAX_VALUE;
        private boolean useClassAnnotations;
        private boolean forceAccessFields;
        private ObjectDebugger.LogObject root;

        private void reset() {
            this.lines.clear();
            this.parsedObjects.clear();
            this.originalInheritanceDepth = Integer.MAX_VALUE;
            this.useClassAnnotations = false;
            this.forceAccessFields = false;
            if (this.root != null) {
                this.root.release();
            }

            this.root = null;
        }

        private boolean inheritsAnnotations(Class<?> c, int inheritanceDepth) {
            return c.getAnnotation(DebugClass.class) == null && c.getAnnotation(DebugClassFields.class) == null
                ? c.getSuperclass() != null && inheritanceDepth >= 0 && this.inheritsAnnotations(c.getSuperclass(), inheritanceDepth - 1)
                : true;
        }

        private boolean validClass(Class<?> c, int inheritanceDept) {
            if (c == null) {
                return false;
            } else if (this.useClassAnnotations) {
                return this.inheritsAnnotations(c, inheritanceDept);
            } else if (String.class.isAssignableFrom(c)) {
                return false;
            } else if (Boolean.class.isAssignableFrom(c)) {
                return false;
            } else if (Byte.class.isAssignableFrom(c)) {
                return false;
            } else if (Short.class.isAssignableFrom(c)) {
                return false;
            } else if (Integer.class.isAssignableFrom(c)) {
                return false;
            } else if (Long.class.isAssignableFrom(c)) {
                return false;
            } else if (Float.class.isAssignableFrom(c)) {
                return false;
            } else if (Double.class.isAssignableFrom(c)) {
                return false;
            } else if (Enum.class.isAssignableFrom(c)) {
                return false;
            } else {
                return Collection.class.isAssignableFrom(c) ? false : !Iterable.class.isAssignableFrom(c);
            }
        }

        private void parse(Object o, ArrayList<String> list, int inheritanceDepth, boolean forceAccessFields, boolean useClassAnnotations, int memberDepth) throws Exception {
            this.originalInheritanceDepth = inheritanceDepth;
            this.useClassAnnotations = useClassAnnotations;
            this.forceAccessFields = forceAccessFields;
            this.root = ObjectDebugger.alloc_log_object(o);
            this.parseInternal(this.root, inheritanceDepth, memberDepth);
            this.root.sort();
            this.root.build(list, 0);
        }

        private void parseInternal(ObjectDebugger.LogObject log, int inheritanceDepth, int memberDepth) throws Exception {
            Class<?> c = log.object.getClass();
            if (this.validClass(c, inheritanceDepth)) {
                if (this.parsedObjects.contains(log.object)) {
                }

                this.parsedObjects.add(log.object);
                this.parseClass(log, c, inheritanceDepth, memberDepth);
            }
        }

        private void parseClass(ObjectDebugger.LogObject log, Class<?> c, int inheritanceDepth, int memberDepth) throws Exception {
            this.parseClassMembers(log, c, memberDepth);
            if (c.getSuperclass() != null && inheritanceDepth >= 0) {
                this.parseClass(log, c.getSuperclass(), inheritanceDepth - 1, memberDepth);
            }
        }

        private void parseClassMembers(ObjectDebugger.LogObject log, Class<?> c, int memberDepth) throws Exception {
            if (!this.useClassAnnotations || c.getAnnotation(DebugClass.class) != null || c.getAnnotation(DebugClassFields.class) != null) {
                boolean allFields = !this.useClassAnnotations || c.getAnnotation(DebugClassFields.class) != null;
                Field[] fields = c.getDeclaredFields();

                for (Field f : fields) {
                    if ((allFields || f.getAnnotation(DebugField.class) != null)
                        && !Modifier.isStatic(f.getModifiers())
                        && f.getAnnotation(DebugIgnoreField.class) == null) {
                        boolean access = f.canAccess(log.object);
                        if (!access) {
                            if (!this.forceAccessFields) {
                                continue;
                            }

                            if (!f.trySetAccessible()) {
                                DebugLog.log("Cannot debug field: failed accessibility. field = " + f.getName());
                                continue;
                            }
                        }

                        if (log == this.root) {
                            this.parsedObjects.clear();
                            this.parsedObjects.add(this.root.object);
                        }

                        this.parseMember(log, f.getName(), f.get(log.object), f.getAnnotation(DebugNonRecursive.class) != null ? 0 : memberDepth, false);
                        if (!access) {
                            f.setAccessible(access);
                        }
                    }
                }

                Method[] methods = c.getDeclaredMethods();

                for (Method m : methods) {
                    if (m.getAnnotation(DebugMethod.class) != null) {
                        if (Modifier.isStatic(m.getModifiers())) {
                            DebugLog.log("Cannot debug method: is static. method = " + m.getName());
                        } else if (!Modifier.isPublic(m.getModifiers())) {
                            DebugLog.log("Cannot debug method: not public. method = " + m.getName());
                        } else if (m.getParameterCount() > 0) {
                            DebugLog.log("Cannot debug method: has parameters. method = " + m.getName());
                        } else if (!log.containsMember(m.getName())) {
                            if (log == this.root) {
                                this.parsedObjects.clear();
                                this.parsedObjects.add(this.root.object);
                            }

                            this.parseMember(log, m.getName(), m.invoke(log.object), m.getAnnotation(DebugNonRecursive.class) != null ? 0 : memberDepth, true);
                        }
                    }
                }
            }
        }

        private void parseMember(ObjectDebugger.LogObject log, String memberName, Object value, int memberDepth, boolean isFunction) throws Exception {
            boolean handled = false;
            if (value != null) {
                Class<?> clazz = value.getClass();
                if (memberDepth > 0 && value instanceof List<?> listObj) {
                    ObjectDebugger.LogList logList = ObjectDebugger.alloc_log_list();
                    if (!listObj.isEmpty()) {
                        for (int i = 0; i < listObj.size(); i++) {
                            Object element = listObj.get(i);
                            Class<?> elementClazz = element.getClass();
                            if (this.validClass(elementClazz, this.originalInheritanceDepth)) {
                                ObjectDebugger.LogObject logObject = ObjectDebugger.alloc_log_object(element);
                                this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                                logList.addElement(logObject);
                            } else {
                                logList.addElement(element);
                            }
                        }
                    }

                    ObjectDebugger.LogField logField = ObjectDebugger.alloc_log_field(memberName, logList, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && value instanceof Map<?, ?> mapObj) {
                    ObjectDebugger.LogMap logMap = ObjectDebugger.alloc_log_map();

                    for (Entry<?, ?> entry : mapObj.entrySet()) {
                        Object key = entry.getKey();
                        Object val = entry.getValue();
                        Class<?> valClazz = val.getClass();
                        if (this.validClass(valClazz, this.originalInheritanceDepth)) {
                            ObjectDebugger.LogObject logObject = ObjectDebugger.alloc_log_object(val);
                            this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                            logMap.putElement(key, logObject);
                        } else {
                            logMap.putElement(key, val);
                        }
                    }

                    ObjectDebugger.LogField logField = ObjectDebugger.alloc_log_field(memberName, logMap, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && value instanceof AssocArray<?, ?> mapObj) {
                    ObjectDebugger.LogMap logMap = ObjectDebugger.alloc_log_map();

                    for (int ix = 0; ix < mapObj.size(); ix++) {
                        Object key = mapObj.getKey(ix);
                        Object val = mapObj.getValue(ix);
                        Class<?> valClazz = val.getClass();
                        if (this.validClass(valClazz, this.originalInheritanceDepth)) {
                            ObjectDebugger.LogObject logObject = ObjectDebugger.alloc_log_object(val);
                            this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                            logMap.putElement(key, logObject);
                        } else {
                            logMap.putElement(key, val);
                        }
                    }

                    ObjectDebugger.LogField logField = ObjectDebugger.alloc_log_field(memberName, logMap, isFunction);
                    log.addMember(logField);
                    handled = true;
                } else if (memberDepth > 0 && this.validClass(clazz, this.originalInheritanceDepth)) {
                    ObjectDebugger.LogObject logObject = ObjectDebugger.alloc_log_object(value);
                    this.parseInternal(logObject, this.originalInheritanceDepth, memberDepth - 1);
                    ObjectDebugger.LogField logField = ObjectDebugger.alloc_log_field(memberName, logObject, isFunction);
                    log.addMember(logField);
                    handled = true;
                }
            }

            if (!handled) {
                ObjectDebugger.LogField logField = ObjectDebugger.alloc_log_field(memberName, value, isFunction);
                log.addMember(logField);
            }
        }
    }
}
