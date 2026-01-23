// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;

public final class AnimationVariableSlotCallbackInt extends AnimationVariableSlotCallback<Integer> {
    private int defaultValue;

    public AnimationVariableSlotCallbackInt(
        String key, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
    }

    public AnimationVariableSlotCallbackInt(
        String key,
        AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackInt.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
    }

    public AnimationVariableSlotCallbackInt(
        String key, int defaultVal, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, descriptor);
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackInt(
        String key,
        int defaultVal,
        AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackInt.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, callbackGet, callbackSet, descriptor);
        this.defaultValue = defaultVal;
    }

    public Integer getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return this.getValue().toString();
    }

    @Override
    public float getValueFloat() {
        return this.getValue().intValue();
    }

    @Override
    public boolean getValueBool() {
        return this.getValueFloat() != 0.0F;
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(PZMath.tryParseInt(val, 0));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue((int)val);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? 1 : 0);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Float;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public interface CallbackGetStrongTyped extends Supplier<Integer> {
    }

    public interface CallbackSetStrongTyped extends Consumer<Integer> {
    }
}
