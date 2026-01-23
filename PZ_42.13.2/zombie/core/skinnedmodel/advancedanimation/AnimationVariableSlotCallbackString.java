// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

public final class AnimationVariableSlotCallbackString extends AnimationVariableSlotCallback<String> {
    private String defaultValue = "";

    public AnimationVariableSlotCallbackString(
        String key, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
    }

    public AnimationVariableSlotCallbackString(
        String key,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
    }

    public AnimationVariableSlotCallbackString(
        String key, String defaultVal, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackString(
        String key,
        String defaultVal,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
        this.defaultValue = defaultVal;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return this.getValue();
    }

    @Override
    public float getValueFloat() {
        return PZMath.tryParseFloat(this.getValue(), 0.0F);
    }

    @Override
    public boolean getValueBool() {
        return StringUtils.tryParseBoolean(this.getValue());
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(float val) {
        this.trySetValue(String.valueOf(val));
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? "true" : "false");
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.String;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public interface CallbackGetStrongTyped extends Supplier<String> {
    }

    public interface CallbackSetStrongTyped extends Consumer<String> {
    }
}
