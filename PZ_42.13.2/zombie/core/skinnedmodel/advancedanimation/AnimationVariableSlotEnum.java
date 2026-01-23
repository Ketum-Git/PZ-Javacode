// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public class AnimationVariableSlotEnum<EnumType extends Enum<EnumType>> extends AbstractAnimationVariableSlotEnum<EnumType> {
    private EnumType value;

    public AnimationVariableSlotEnum(Class<EnumType> enumTypeClass, String key, EnumType in_defaultVal, IAnimationVariableSlotDescriptor descriptor) {
        super(enumTypeClass, key, in_defaultVal, descriptor);
        this.value = in_defaultVal;
    }

    @Override
    public EnumType getValue() {
        return this.value;
    }

    @Override
    public void setValue(EnumType in_newValue) {
        if (!this.isReadOnly()) {
            this.value = in_newValue;
        }
    }
}
