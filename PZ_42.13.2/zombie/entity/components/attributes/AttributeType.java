// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;
import zombie.util.StringUtils;

@UsedFromLua
public abstract class AttributeType {
    private static final int MAX_ID = 8128;
    private final short id;
    private final java.lang.String name;
    private final java.lang.String translateKey;
    private final java.lang.String tooltipOverride;
    private final Attribute.UI.Display optionDisplay;
    private final Attribute.UI.DisplayAsBar optionDisplayAsBar;
    private final boolean readOnly;

    protected AttributeType(short id, java.lang.String name, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
        this(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Default, tooltipOverride);
    }

    protected AttributeType(
        short id, java.lang.String name, boolean readOnly, Attribute.UI.Display display, Attribute.UI.DisplayAsBar asBar, java.lang.String tooltipOverride
    ) {
        if (id < 0 || id > 8128) {
            throw new RuntimeException("AttributeType Id may not exceed '8128' or be less than zero.");
        } else if (name == null) {
            throw new RuntimeException("AttributeType name cannot be null.");
        } else {
            if (StringUtils.containsWhitespace(name)) {
                DebugLog.General.error("Sanitizing AttributeType name '" + name + "', name may not contain whitespaces.");
                name = StringUtils.removeWhitespace(name);
            }

            this.id = id;
            this.name = name;
            this.optionDisplay = display;
            this.optionDisplayAsBar = asBar;
            this.tooltipOverride = tooltipOverride;
            this.translateKey = "Attribute_Type_" + this.name;
            this.readOnly = readOnly;
        }
    }

    public short id() {
        return this.id;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    @Override
    public java.lang.String toString() {
        return this.getName();
    }

    public java.lang.String getName() {
        return this.name;
    }

    public abstract AttributeValueType getValueType();

    public boolean isNumeric() {
        return AttributeValueType.IsNumeric(this.getValueType());
    }

    public boolean isDecimal() {
        return AttributeValueType.IsDecimal(this.getValueType());
    }

    public boolean isHiddenUI() {
        return this.optionDisplay == Attribute.UI.Display.Hidden;
    }

    protected Attribute.UI.DisplayAsBar getDisplayAsBar() {
        return this.optionDisplayAsBar;
    }

    public java.lang.String getTranslateKey() {
        return this.translateKey;
    }

    private java.lang.String getTranslatedName() {
        java.lang.String s = Translator.getAttributeTextOrNull(this.translateKey);
        return s != null ? s : this.getName();
    }

    public java.lang.String getNameUI() {
        if (this.tooltipOverride != null) {
            java.lang.String s = Translator.getAttributeTextOrNull(this.tooltipOverride);
            if (s != null) {
                return s;
            }
        }

        return this.getTranslatedName();
    }

    @UsedFromLua
    public static class Bool extends AttributeType {
        private final boolean initialValue;

        protected Bool(short id, java.lang.String name, boolean initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = initialValue;
        }

        protected Bool(short id, java.lang.String name, boolean initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = initialValue;
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Boolean;
        }

        public boolean getInitialValue() {
            return this.initialValue;
        }
    }

    @UsedFromLua
    public static class Byte extends AttributeType.Numeric<AttributeType.Byte, java.lang.Byte> {
        protected Byte(short id, java.lang.String name, byte initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Byte(
            short id,
            java.lang.String name,
            byte initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Byte;
        }

        public java.lang.Byte validate(java.lang.Byte value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = (byte)Math.min(value, this.getVars().max);
                value = (byte)Math.max(value, this.getVars().min);
            }

            return value;
        }

        public java.lang.Byte getMin() {
            return this.getVars() != null ? this.getVars().min : -128;
        }

        public java.lang.Byte getMax() {
            return this.getVars() != null ? this.getVars().max : 127;
        }

        protected boolean withinBounds(java.lang.Byte value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public static class Double extends AttributeType.Numeric<AttributeType.Double, java.lang.Double> {
        protected Double(short id, java.lang.String name, double initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Double(
            short id,
            java.lang.String name,
            double initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Double;
        }

        public java.lang.Double validate(java.lang.Double value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, this.getVars().max);
                value = Math.max(value, this.getVars().min);
            }

            return value;
        }

        public java.lang.Double getMin() {
            return this.getVars() != null ? this.getVars().min : java.lang.Double.MIN_VALUE;
        }

        public java.lang.Double getMax() {
            return this.getVars() != null ? this.getVars().max : java.lang.Double.MAX_VALUE;
        }

        protected boolean withinBounds(java.lang.Double value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public static class Enum<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
        private final Class<E> enumClass;
        private final E initialValue;

        protected Enum(short id, java.lang.String name, E initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = Objects.requireNonNull(initialValue);
            this.enumClass = this.initialValue.getDeclaringClass();
        }

        protected Enum(short id, java.lang.String name, E initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = Objects.requireNonNull(initialValue);
            this.enumClass = this.initialValue.getDeclaringClass();
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Enum;
        }

        public E getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            Class<E> clazz = this.initialValue.getDeclaringClass();

            for (E option : (java.lang.Enum[])clazz.getEnumConstants()) {
                if (option.getByteId() == id) {
                    return option;
                }
            }

            return null;
        }
    }

    @UsedFromLua
    public static class EnumSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
        private final Class<E> enumClass;
        private final java.util.EnumSet<E> initialValue;

        protected EnumSet(short id, java.lang.String name, Class<E> clazz) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
        }

        protected EnumSet(short id, java.lang.String name, Class<E> clazz, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = java.util.EnumSet.noneOf(this.enumClass);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.EnumSet;
        }

        public java.util.EnumSet<E> getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            for (E option : (java.lang.Enum[])this.enumClass.getEnumConstants()) {
                if (option.getByteId() == id) {
                    return option;
                }
            }

            return null;
        }

        protected Class<E> getEnumClass() {
            return this.enumClass;
        }
    }

    @UsedFromLua
    public static class EnumStringSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeType {
        private final Class<E> enumClass;
        private final EnumStringObj<E> initialValue;

        protected EnumStringSet(short id, java.lang.String name, Class<E> clazz) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = new EnumStringObj<>();
            this.initialValue.initialize(this.enumClass);
        }

        protected EnumStringSet(
            short id, java.lang.String name, Class<E> clazz, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride
        ) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.enumClass = Objects.requireNonNull(clazz);
            this.initialValue = new EnumStringObj<>();
            this.initialValue.initialize(this.enumClass);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.EnumStringSet;
        }

        public EnumStringObj<E> getInitialValue() {
            return this.initialValue;
        }

        public E enumValueFromString(java.lang.String s) {
            return AttributeUtil.enumValueFromScriptString(this.enumClass, s);
        }

        public E enumValueFromByteID(byte id) {
            for (E option : (java.lang.Enum[])this.enumClass.getEnumConstants()) {
                if (option.getByteId() == id) {
                    return option;
                }
            }

            return null;
        }

        protected Class<E> getEnumClass() {
            return this.enumClass;
        }
    }

    @UsedFromLua
    public static class Float extends AttributeType.Numeric<AttributeType.Float, java.lang.Float> {
        protected Float(short id, java.lang.String name, float initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Float(
            short id,
            java.lang.String name,
            float initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Float;
        }

        public java.lang.Float validate(java.lang.Float value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, this.getVars().max);
                value = Math.max(value, this.getVars().min);
            }

            return value;
        }

        public java.lang.Float getMin() {
            return this.getVars() != null ? this.getVars().min : java.lang.Float.MIN_VALUE;
        }

        public java.lang.Float getMax() {
            return this.getVars() != null ? this.getVars().max : java.lang.Float.MAX_VALUE;
        }

        protected boolean withinBounds(java.lang.Float value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public static class Int extends AttributeType.Numeric<AttributeType.Int, Integer> {
        protected Int(short id, java.lang.String name, int initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Int(
            short id,
            java.lang.String name,
            int initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Int;
        }

        public Integer validate(Integer value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, this.getVars().max);
                value = Math.max(value, this.getVars().min);
            }

            return value;
        }

        public Integer getMin() {
            return this.getVars() != null ? this.getVars().min : Integer.MIN_VALUE;
        }

        public Integer getMax() {
            return this.getVars() != null ? this.getVars().max : Integer.MAX_VALUE;
        }

        protected boolean withinBounds(Integer value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public static class Long extends AttributeType.Numeric<AttributeType.Long, java.lang.Long> {
        protected Long(short id, java.lang.String name, long initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Long(
            short id,
            java.lang.String name,
            long initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Long;
        }

        public java.lang.Long validate(java.lang.Long value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = Math.min(value, this.getVars().max);
                value = Math.max(value, this.getVars().min);
            }

            return value;
        }

        public java.lang.Long getMin() {
            return this.getVars() != null ? this.getVars().min : java.lang.Long.MIN_VALUE;
        }

        public java.lang.Long getMax() {
            return this.getVars() != null ? this.getVars().max : java.lang.Long.MAX_VALUE;
        }

        protected boolean withinBounds(java.lang.Long value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public abstract static class Numeric<C extends AttributeType.Numeric<C, T>, T extends Number> extends AttributeType {
        private final T initialValue;
        private AttributeType.Numeric.NumericVars<T> vars;
        private boolean requiresValidation;

        protected Numeric(
            short id,
            java.lang.String name,
            T initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, readOnly, display, asBar, tooltipOverride);
            this.initialValue = Objects.requireNonNull(initialValue);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Float;
        }

        protected boolean isRequiresValidation() {
            return this.requiresValidation;
        }

        protected AttributeType.Numeric.NumericVars<T> getVars() {
            return this.vars;
        }

        public T getInitialValue() {
            return this.initialValue;
        }

        protected final AttributeType.Numeric<C, T> setBounds(T min, T max) {
            if (!(min.doubleValue() < 0.0) && !(min.doubleValue() >= max.doubleValue()) && !(max.doubleValue() <= 0.0)) {
                this.requiresValidation = true;
                if (this.vars == null) {
                    this.vars = new AttributeType.Numeric.NumericVars<>(min, max);
                }

                if (!this.withinBounds(this.initialValue)) {
                    throw new IllegalArgumentException("Initialvalue outside set bounds.");
                } else {
                    return this;
                }
            } else {
                throw new IllegalArgumentException("Illegal 'Bounds' on Attribute [" + this.toString() + "]");
            }
        }

        public boolean hasBounds() {
            return this.requiresValidation;
        }

        public abstract T validate(T var1);

        public abstract T getMin();

        public abstract T getMax();

        protected abstract boolean withinBounds(T var1);

        protected static class NumericVars<T> {
            protected final T min;
            protected final T max;

            protected NumericVars(T min, T max) {
                this.min = min;
                this.max = max;
            }
        }
    }

    @UsedFromLua
    public static class Short extends AttributeType.Numeric<AttributeType.Short, java.lang.Short> {
        protected Short(short id, java.lang.String name, short initialValue) {
            super(id, name, initialValue, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Default, null);
        }

        protected Short(
            short id,
            java.lang.String name,
            short initialValue,
            boolean readOnly,
            Attribute.UI.Display display,
            Attribute.UI.DisplayAsBar asBar,
            java.lang.String tooltipOverride
        ) {
            super(id, name, initialValue, readOnly, display, asBar, tooltipOverride);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.Short;
        }

        public java.lang.Short validate(java.lang.Short value) {
            if (this.isRequiresValidation() && this.getVars() != null) {
                value = (short)Math.min(value, this.getVars().max);
                value = (short)Math.max(value, this.getVars().min);
            }

            return value;
        }

        public java.lang.Short getMin() {
            return this.getVars() != null ? this.getVars().min : -32768;
        }

        public java.lang.Short getMax() {
            return this.getVars() != null ? this.getVars().max : 32767;
        }

        protected boolean withinBounds(java.lang.Short value) {
            return this.getVars() == null ? true : value >= this.getVars().min && value <= this.getVars().max;
        }
    }

    @UsedFromLua
    public static class String extends AttributeType {
        private final java.lang.String initialValue;

        protected String(short id, java.lang.String name, java.lang.String initialValue) {
            super(id, name, false, Attribute.UI.Display.Visible, Attribute.UI.DisplayAsBar.Never, null);
            this.initialValue = Objects.requireNonNull(initialValue);
        }

        protected String(
            short id, java.lang.String name, java.lang.String initialValue, boolean readOnly, Attribute.UI.Display display, java.lang.String tooltipOverride
        ) {
            super(id, name, readOnly, display, Attribute.UI.DisplayAsBar.Never, tooltipOverride);
            this.initialValue = Objects.requireNonNull(initialValue);
        }

        @Override
        public AttributeValueType getValueType() {
            return AttributeValueType.String;
        }

        public java.lang.String getInitialValue() {
            return this.initialValue;
        }
    }
}
