// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.util.StringUtils;

public final class AnimationVariableSlotCallbackBool extends AnimationVariableSlotCallback<Boolean> {
    private boolean defaultValue;

    public AnimationVariableSlotCallbackBool(
        String key, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
    }

    public AnimationVariableSlotCallbackBool(
        String key,
        AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
    }

    public AnimationVariableSlotCallbackBool(
        String key, boolean defaultVal, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackBool(
        String key,
        boolean defaultVal,
        AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
        this.defaultValue = defaultVal;
    }

    public Boolean getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return this.getValue() ? "true" : "false";
    }

    @Override
    public float getValueFloat() {
        return this.getValue() ? 1.0F : 0.0F;
    }

    @Override
    public boolean getValueBool() {
        return this.getValue();
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(StringUtils.tryParseBoolean(val));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue(val != 0.0);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Boolean;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return StringUtils.tryParseBoolean(val);
    }

    /**
     * Strong-typed utility type. Useful for auto-typed function overrides, such as AnimationVariableSource setVariable
     */
    public interface CallbackGetStrongTyped extends Supplier<Boolean> {
    }

    public interface CallbackSetStrongTyped extends Consumer<Boolean> {
    }
}
