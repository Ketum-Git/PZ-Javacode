// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public class AnimationVariableSlotEnum<EnumType extends Enum<EnumType>> extends AbstractAnimationVariableSlotEnum<EnumType> {
    private EnumType value;

    public AnimationVariableSlotEnum(Class<EnumType> enumTypeClass, String key, EnumType defaultVal, IAnimationVariableSlotDescriptor descriptor) {
        super(enumTypeClass, key, defaultVal, descriptor);
        this.value = defaultVal;
    }

    @Override
    public EnumType getValue() {
        return this.value;
    }

    @Override
    public void setValue(EnumType newValue) {
        if (!this.isReadOnly()) {
            this.value = newValue;
        }
    }
}
