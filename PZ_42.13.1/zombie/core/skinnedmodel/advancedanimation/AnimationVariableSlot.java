// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public abstract class AnimationVariableSlot implements IAnimationVariableSlot {
    private final String key;
    private IAnimationVariableSlotDescriptor descriptor;

    protected AnimationVariableSlot(String key, IAnimationVariableSlotDescriptor descriptor) {
        this.key = key.toLowerCase().trim();
        this.descriptor = descriptor;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getDescription(IAnimationVariableSource owner) {
        return this.descriptor != null ? this.descriptor.getDescription(owner) : null;
    }
}
