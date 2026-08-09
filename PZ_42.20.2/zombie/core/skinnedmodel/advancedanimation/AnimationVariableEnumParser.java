// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.Map;
import java.util.TreeMap;
import zombie.AttackType;
import zombie.util.StringUtils;

public class AnimationVariableEnumParser {
    private static final AnimationVariableEnumParser s_instance = new AnimationVariableEnumParser();
    private final Map<String, AnimationVariableEnumParser.Slot<? extends Enum<?>>> registeredEnumClasses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private AnimationVariableEnumParser.Slot<? extends Enum<?>> findEnumClass(String className) {
        return StringUtils.isNullOrWhitespace(className) ? null : this.registeredEnumClasses.get(className.trim().toLowerCase());
    }

    private <E extends Enum<E>> void registerClassInternal(Class<E> clazz, E defaultValue) {
        String className = clazz.getSimpleName();
        String classNameKey = className.toLowerCase();
        this.registeredEnumClasses
            .put(classNameKey, (AnimationVariableEnumParser.Slot<? extends Enum<?>>)(new AnimationVariableEnumParser.Slot<>(clazz, defaultValue)));
    }

    public static <E extends Enum<E>> void registerEnumClass(Class<E> clazz, E defaultValue) {
        getInstance().registerClassInternal(clazz, defaultValue);
    }

    public static <E extends Enum<?>> E tryParse(String enumClassName, String enumStr) {
        AnimationVariableEnumParser.Slot<?> slot = getInstance().findEnumClass(enumClassName);
        return (E)(slot == null ? null : slot.tryParse(enumStr));
    }

    public static AnimationVariableEnumParser getInstance() {
        return s_instance;
    }

    static {
        registerEnumClass(AttackType.class, AttackType.NONE);
    }

    private static class Slot<EnumType extends Enum<EnumType>> {
        public final Class<EnumType> enumClass;
        public final EnumType defaultValue;

        public Slot(Class<EnumType> enumClass, EnumType defaultValue) {
            this.enumClass = enumClass;
            this.defaultValue = defaultValue;
        }

        EnumType tryParse(String enumStr) {
            return StringUtils.tryParseEnum(this.enumClass, enumStr, this.defaultValue);
        }
    }
}
