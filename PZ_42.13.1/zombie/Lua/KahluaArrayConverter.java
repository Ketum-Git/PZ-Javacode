// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import java.lang.reflect.Array;
import java.util.Objects;
import se.krka.kahlua.converter.JavaToLuaConverter;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.converter.LuaToJavaConverter;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;

public class KahluaArrayConverter {
    private final J2SEPlatform platform;
    private final KahluaConverterManager manager;

    public KahluaArrayConverter(J2SEPlatform in_platform, KahluaConverterManager in_manager) {
        this.platform = in_platform;
        this.manager = in_manager;
    }

    public void install() {
        this.manager.addJavaConverter(new JavaToLuaConverter<Object>() {
            {
                Objects.requireNonNull(KahluaArrayConverter.this);
            }

            @Override
            public Object fromJavaToLua(Object javaObject) {
                if (!javaObject.getClass().isArray()) {
                    return null;
                } else {
                    KahluaTable t = KahluaArrayConverter.this.platform.newTable();
                    int n = Array.getLength(javaObject);

                    for (int i = 0; i < n; i++) {
                        Object value = Array.get(javaObject, i);
                        t.rawset(i + 1, KahluaArrayConverter.this.manager.fromJavaToLua(value));
                    }

                    return t;
                }
            }

            @Override
            public Class<Object> getJavaType() {
                return Object.class;
            }
        });
        this.manager.addLuaConverter(new LuaToJavaConverter<KahluaTable, Object>() {
            {
                Objects.requireNonNull(KahluaArrayConverter.this);
            }

            public Object fromLuaToJava(KahluaTable luaObject, Class<Object> javaClass) throws IllegalArgumentException {
                if (!javaClass.isArray()) {
                    return null;
                } else {
                    Class<?> arrayElementType = javaClass.getComponentType();
                    int numElements = luaObject.len();
                    boolean canCast = true;

                    for (int i = 0; i < numElements; i++) {
                        Object element = luaObject.rawget(i + 1);
                        if (element != null) {
                            Class<?> elementType = element.getClass();
                            if (!arrayElementType.isAssignableFrom(elementType)) {
                                canCast = false;
                                break;
                            }
                        }
                    }

                    if (!canCast) {
                        return null;
                    } else {
                        Object elementArray = Array.newInstance(arrayElementType, numElements);

                        for (int ix = 0; ix < numElements; ix++) {
                            Object element = luaObject.rawget(ix + 1);
                            Array.set(elementArray, ix, element);
                        }

                        return elementArray;
                    }
                }
            }

            @Override
            public Class<Object> getJavaType() {
                return Object.class;
            }

            @Override
            public Class<KahluaTable> getLuaType() {
                return KahluaTable.class;
            }
        });
    }
}
