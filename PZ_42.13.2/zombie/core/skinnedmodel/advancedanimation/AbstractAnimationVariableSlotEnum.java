// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public abstract class AbstractAnimationVariableSlotEnum<EnumType extends Enum<EnumType>> extends AnimationVariableSlot {
    private final EnumType defaultValue;
    private final Class<EnumType> enumTypeClass;

    public AbstractAnimationVariableSlotEnum(Class<EnumType> enumTypeClass, String key, EnumType in_defaultVal, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
        this.enumTypeClass = enumTypeClass;
        this.defaultValue = in_defaultVal;
    }

    public Class<EnumType> getEnumTypeClass() {
        return this.enumTypeClass;
    }

    public abstract EnumType getValue();

    public abstract void setValue(EnumType var1);

    @Override
    public <E extends Enum<E>> E getEnumValue(E in_defaultVal) {
        Class<EnumType> enumClass = this.getEnumTypeClass();
        Class<E> incomingValType = (Class<E>)in_defaultVal.getClass();
        if (incomingValType != enumClass) {
            String strValue = this.getValueString();
            return StringUtils.tryParseEnum(incomingValType, strValue, in_defaultVal);
        } else {
            return (E)this.getValue();
        }
    }

    @Override
    public <E extends Enum<E>> void setEnumValue(E in_val) {
        Class<EnumType> enumClass = this.getEnumTypeClass();
        Class<E> incomingValType = (Class<E>)in_val.getClass();
        if (incomingValType != enumClass) {
            String strValue = in_val.toString();
            this.setValue(strValue);
        } else {
            this.setValue((EnumType)in_val);
        }
    }

    @Override
    public String getValueString() {
        EnumType valueEnum = this.getValue();
        return valueEnum != null ? valueEnum.name() : "";
    }

    @Override
    public float getValueFloat() {
        return PZMath.tryParseFloat(this.getValueString(), 0.0F);
    }

    @Override
    public boolean getValueBool() {
        return StringUtils.tryParseBoolean(this.getValueString());
    }

    public EnumType getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setValue(String val) {
        try {
            this.setEnumValue(Enum.valueOf(this.enumTypeClass, val));
        } catch (IllegalArgumentException var3) {
            this.setEnumValue(this.defaultValue);
        }
    }

    @Override
    public void setValue(float val) {
        this.setValue(String.valueOf(val));
    }

    @Override
    public void setValue(boolean val) {
        this.setValue(val ? "true" : "false");
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.String;
    }

    @Override
    public boolean canConvertFrom(String val) {
        try {
            EnumType parsedVal = Enum.valueOf(this.enumTypeClass, val);
            return parsedVal != null;
        } catch (IllegalArgumentException var3) {
            return false;
        }
    }

    @Override
    public void clear() {
        this.setEnumValue(this.defaultValue);
    }
}
