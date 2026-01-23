// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.math.PZMath;

public final class AnimationVariableSlotFloat extends AnimationVariableSlot {
    private float value;
    private boolean isReadOnly;

    public AnimationVariableSlotFloat(String key, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
    }

    @Override
    public String getValueString() {
        return String.valueOf(this.value);
    }

    @Override
    public float getValueFloat() {
        return this.value;
    }

    @Override
    public boolean getValueBool() {
        return this.value != 0.0F;
    }

    @Override
    public void setValue(String val) {
        if (!this.isReadOnly()) {
            this.value = PZMath.tryParseFloat(val, 0.0F);
        }
    }

    @Override
    public void setValue(float val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public void setValue(boolean val) {
        if (!this.isReadOnly()) {
            this.value = val ? 1.0F : 0.0F;
        }
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Float;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return PZMath.canParseFloat(val);
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.value = 0.0F;
        }
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public boolean setReadOnly(boolean in_set) {
        if (in_set != this.isReadOnly) {
            this.isReadOnly = in_set;
            return true;
        } else {
            return false;
        }
    }
}
