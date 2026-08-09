// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;
import zombie.debug.DebugType;

public final class AnimationVariableSlotCallbackInt extends AnimationVariableSlotCallback<Integer> {
    private final AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGetPrimitive;
    private final AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSetPrimitive;
    private int defaultValue;

    public AnimationVariableSlotCallbackInt(
        String key, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
    }

    public AnimationVariableSlotCallbackInt(
        String key,
        AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet,
        AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
    }

    public AnimationVariableSlotCallbackInt(
        String key, int defaultVal, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = null;
        this.defaultValue = defaultVal;
    }

    public AnimationVariableSlotCallbackInt(
        String key,
        int defaultVal,
        AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet,
        AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, null, null, descriptor);
        this.callbackGetPrimitive = callbackGet;
        this.callbackSetPrimitive = callbackSet;
        this.defaultValue = defaultVal;
    }

    public Integer getValue() {
        return this.getValueInt();
    }

    public Integer getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public String getValueString() {
        return this.getValue().toString();
    }

    @Override
    public int getValueInt() {
        return this.callbackGetPrimitive == null ? (Integer)super.getValue() : this.callbackGetPrimitive.get();
    }

    public boolean trySetValue(Integer val) {
        return this.trySetValue(val.intValue());
    }

    public boolean trySetValue(int val) {
        if (this.callbackSetPrimitive == null) {
            return super.trySetValue(val);
        } else if (this.isReadOnly()) {
            DebugType.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
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
    public float getValueFloat() {
        return this.getValueInt();
    }

    @Override
    public boolean getValueBool() {
        return this.getValueInt() != 0;
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
    public void setValue(int val) {
        this.trySetValue(val);
    }

    @Override
    public void setValue(boolean val) {
        this.trySetValue(val ? 1 : 0);
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Int;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return true;
    }

    public interface PrimitiveIntConsumer {
        void accept(int var1);
    }

    public interface PrimitiveIntSupplier {
        int get();
    }
}
