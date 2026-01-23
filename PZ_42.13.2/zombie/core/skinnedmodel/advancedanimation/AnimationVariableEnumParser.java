// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.Map;
import java.util.TreeMap;
import zombie.AttackType;
import zombie.util.StringUtils;

public class AnimationVariableEnumParser {
    private static final AnimationVariableEnumParser s_instance = new AnimationVariableEnumParser();
    private final Map<String, AnimationVariableEnumParser.Slot<? extends Enum<?>>> registeredEnumClasses = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private AnimationVariableEnumParser.Slot<? extends Enum<?>> findEnumClass(String in_className) {
        return StringUtils.isNullOrWhitespace(in_className) ? null : this.registeredEnumClasses.get(in_className.trim().toLowerCase());
    }

    private <E extends Enum<E>> void registerClassInternal(Class<E> in_class, E in_defaultValue) {
        String className = in_class.getSimpleName();
        String classNameKey = className.toLowerCase();
        this.registeredEnumClasses
            .put(classNameKey, (AnimationVariableEnumParser.Slot<? extends Enum<?>>)(new AnimationVariableEnumParser.Slot<>(in_class, in_defaultValue)));
    }

    public static <E extends Enum<E>> void registerEnumClass(Class<E> in_class, E in_defaultValue) {
        getInstance().registerClassInternal(in_class, in_defaultValue);
    }

    public static <E extends Enum<?>> E tryParse(String in_enumClassName, String in_enumStr) {
        AnimationVariableEnumParser.Slot<?> slot = getInstance().findEnumClass(in_enumClassName);
        return (E)(slot == null ? null : slot.tryParse(in_enumStr));
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

        public Slot(Class<EnumType> in_enumClass, EnumType in_defaultValue) {
            this.enumClass = in_enumClass;
            this.defaultValue = in_defaultValue;
        }

        EnumType tryParse(String in_enumStr) {
            return StringUtils.tryParseEnum(this.enumClass, in_enumStr, this.defaultValue);
        }
    }
}
