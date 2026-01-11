// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import zombie.util.StringUtils;

public final class AnimationVariableSlotBool extends AnimationVariableSlot {
    private boolean value;
    private boolean isReadOnly;

    public AnimationVariableSlotBool(String key, IAnimationVariableSlotDescriptor descriptor) {
        super(key, descriptor);
    }

    @Override
    public String getValueString() {
        return this.value ? "true" : "false";
    }

    @Override
    public float getValueFloat() {
        return this.value ? 1.0F : 0.0F;
    }

    @Override
    public boolean getValueBool() {
        return this.value;
    }

    @Override
    public void setValue(String val) {
        if (!this.isReadOnly()) {
            this.value = StringUtils.tryParseBoolean(val);
        }
    }

    @Override
    public void setValue(float val) {
        if (!this.isReadOnly()) {
            this.value = val != 0.0;
        }
    }

    @Override
    public void setValue(boolean val) {
        if (!this.isReadOnly()) {
            this.value = val;
        }
    }

    @Override
    public AnimationVariableType getType() {
        return AnimationVariableType.Boolean;
    }

    @Override
    public boolean canConvertFrom(String val) {
        return StringUtils.isBoolean(val);
    }

    @Override
    public void clear() {
        if (!this.isReadOnly()) {
            this.value = false;
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
