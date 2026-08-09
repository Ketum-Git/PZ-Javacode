// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import se.krka.kahlua.converter.JavaToLuaConverter;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.converter.LuaToJavaConverter;
import zombie.core.BoxedStaticValues;

public final class KahluaNumberConverter {
    private KahluaNumberConverter() {
    }

    public static void install(KahluaConverterManager manager) {
        manager.addLuaConverter(new LuaToJavaConverter<Double, Long>() {
            public Long fromLuaToJava(Double luaObject, Class<Long> javaClass) {
                return luaObject.longValue();
            }

            @Override
            public Class<Long> getJavaType() {
                return Long.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Integer>() {
            public Integer fromLuaToJava(Double luaObject, Class<Integer> javaClass) {
                return luaObject.intValue();
            }

            @Override
            public Class<Integer> getJavaType() {
                return Integer.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Float>() {
            public Float fromLuaToJava(Double luaObject, Class<Float> javaClass) {
                return luaObject.floatValue();
            }

            @Override
            public Class<Float> getJavaType() {
                return Float.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Byte>() {
            public Byte fromLuaToJava(Double luaObject, Class<Byte> javaClass) {
                return luaObject.byteValue();
            }

            @Override
            public Class<Byte> getJavaType() {
                return Byte.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Character>() {
            public Character fromLuaToJava(Double luaObject, Class<Character> javaClass) {
                return (char)luaObject.intValue();
            }

            @Override
            public Class<Character> getJavaType() {
                return Character.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addLuaConverter(new LuaToJavaConverter<Double, Short>() {
            public Short fromLuaToJava(Double luaObject, Class<Short> javaClass) {
                return luaObject.shortValue();
            }

            @Override
            public Class<Short> getJavaType() {
                return Short.class;
            }

            @Override
            public Class<Double> getLuaType() {
                return Double.class;
            }
        });
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Double.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Float.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Integer.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Long.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Short.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(Byte.class));
        manager.addJavaConverter(new KahluaNumberConverter.CharacterToLuaConverter(Character.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(double.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(float.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(int.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(long.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(short.class));
        manager.addJavaConverter(new KahluaNumberConverter.NumberToLuaConverter<>(byte.class));
        manager.addJavaConverter(new KahluaNumberConverter.CharacterToLuaConverter(char.class));
        manager.addJavaConverter(new JavaToLuaConverter<Boolean>() {
            public Object fromJavaToLua(Boolean javaObject) {
                return javaObject;
            }

            @Override
            public Class<Boolean> getJavaType() {
                return Boolean.class;
            }
        });
    }

    private record CharacterToLuaConverter(Class<Character> clazz) implements JavaToLuaConverter<Character> {
        public Object fromJavaToLua(Character javaObject) {
            return BoxedStaticValues.toDouble(javaObject.charValue());
        }

        @Override
        public Class<Character> getJavaType() {
            return this.clazz;
        }
    }

    private record NumberToLuaConverter<T extends Number>(Class<T> clazz) implements JavaToLuaConverter<T> {
        public Object fromJavaToLua(T javaObject) {
            return javaObject instanceof Double ? javaObject : BoxedStaticValues.toDouble(javaObject.doubleValue());
        }

        @Override
        public Class<T> getJavaType() {
            return this.clazz;
        }
    }
}
