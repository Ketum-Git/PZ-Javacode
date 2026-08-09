// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.debug.DebugType;

public class AnimationVariableSlotCallbackEnum<EnumType extends Enum<EnumType>> extends AbstractAnimationVariableSlotEnum<EnumType> {
    private final Supplier<EnumType> callbackGet;
    private final Consumer<EnumType> callbackSet;

    protected AnimationVariableSlotCallbackEnum(
        Class<EnumType> enumTypeClass, String key, EnumType defaultVal, Supplier<EnumType> callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this(enumTypeClass, key, defaultVal, callbackGet, null, descriptor);
    }

    protected AnimationVariableSlotCallbackEnum(
        Class<EnumType> enumTypeClass,
        String key,
        EnumType defaultVal,
        Supplier<EnumType> callbackGet,
        Consumer<EnumType> callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(enumTypeClass, key, defaultVal, descriptor);
        this.callbackGet = callbackGet;
        this.callbackSet = callbackSet;
    }

    @Override
    public EnumType getValue() {
        return this.callbackGet.get();
    }

    @Override
    public void setValue(EnumType newValue) {
        this.trySetValue(newValue);
    }

    public boolean trySetValue(EnumType val) {
        if (this.isReadOnly()) {
            DebugType.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
            return false;
        } else {
            this.callbackSet.accept(val);
            return true;
        }
    }

    @Override
    public boolean isReadOnly() {
        return this.callbackSet == null;
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.trySetValue(this.getDefaultValue());
        }
    }
}
