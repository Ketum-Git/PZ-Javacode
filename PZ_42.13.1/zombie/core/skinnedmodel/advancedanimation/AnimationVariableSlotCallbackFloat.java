// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;

public final class AnimationVariableSlotCallbackFloat extends AnimationVariableSlotCallback<Float> {
    private final AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGetPrimitive;
    private final AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSetPrimitive;
    private float defaultValue;

    public AnimationVariableSlotCallbackFloat(
        String key, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
    }

    public AnimationVariableSlotCallbackFloat(
        String key,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
    }

    public AnimationVariableSlotCallbackFloat(
        String key, float defaultVal, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackFloat(
        String key,
        float defaultVal,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
        this.defaultValue = defaultVal;
    }

    public Float getValue() {
        return this.callbackGetPrimitive == null ? (Float)super.getValue() : this.callbackGetPrimitive.get();
    }

    public Float getDefaultValue() {
        return this.defaultValue;
    }

    public boolean trySetValue(Float val) {
        if (this.callbackSetPrimitive == null) {
            return super.trySetValue(val);
        } else if (this.isReadOnly()) {
            DebugLog.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
            return false;
        } else {
            this.callbackSetPrimitive.accept(val);
            return true;
        }
    }

    @Override
    public boolean isReadOnly() {
        return this.callbackSetPrimitive == null && super.isReadOnly();
    }

    @Override
    public String getValueString() {
        return this.getValue().toString();
    }

    @Override
    public float getValueFloat() {
        return this.callbackGetPrimitive == null ? (Float)super.getValue() : this.callbackGetPrimitive.get();
    }

    @Override
    public boolean getValueBool() {
        return this.getValueFloat() != 0.0F;
    }

    @Override
    public void setValue(String val) {
        this.trySetValue(PZMath.tryParseFloat(val, 0.0F));
    }

    @Override
    public void setValue(float val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? 1.0F : 0.0F);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Float;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public interface CallbackGetStrongTyped extends Supplier<Float> {
    }

    public interface CallbackSetStrongTyped extends Consumer<Float> {
    }

    public interface PrimitiveFloatConsumer {
        void accept(float arg0);
    }

    public interface PrimitiveFloatSupplier {
        float get();
    }
}
