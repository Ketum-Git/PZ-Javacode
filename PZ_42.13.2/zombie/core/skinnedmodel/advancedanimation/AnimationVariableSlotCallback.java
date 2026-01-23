// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.debug.DebugLog;

public abstract class AnimationVariableSlotCallback<VariableType> extends AnimationVariableSlot {
    private final Supplier<VariableType> callbackGet;
    private final Consumer<VariableType> callbackSet;

    protected AnimationVariableSlotCallback(String key, Supplier<VariableType> callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this(key, callbackGet, null, descriptor);
    }

    protected AnimationVariableSlotCallback(
        String key, Supplier<VariableType> callbackGet, Consumer<VariableType> callbackSet, IAnimationVariableSlotDescriptor descriptor
    ) {
        super(key, descriptor);
        this.callbackGet = callbackGet;
        this.callbackSet = callbackSet;
    }

    public VariableType getValue() {
        return this.callbackGet.get();
    }

    public abstract VariableType getDefaultValue();

    public boolean trySetValue(VariableType val) {
        if (this.isReadOnly()) {
            DebugLog.General.warn("Trying to set read-only variable \"%s\"", this.getKey());
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
