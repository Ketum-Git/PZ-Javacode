// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class AnimationVariableSource implements IAnimationVariableMap, IAnimationVariableCallbackMap {
    private boolean isEmpty = true;
    private IAnimationVariableSlot[] cachedGameVariableSlots = new IAnimationVariableSlot[0];

    /**
     * Returns the specified variable slot. Or NULL if not found.
     */
    @Override
    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        if (handle == null) {
            return null;
        } else {
            int handleIdx = handle.getVariableIndex();
            if (handleIdx < 0) {
                return null;
            } else {
                return this.cachedGameVariableSlots != null && handleIdx < this.cachedGameVariableSlots.length ? this.cachedGameVariableSlots[handleIdx] : null;
            }
        }
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    private IAnimationVariableSlot getOrCreateVariable(AnimationVariableHandle handle, AnimationVariableSource.AnimationVariableSlotGenerator creator) {
        IAnimationVariableSlot slot = this.getVariable(handle);
        if (slot == null) {
            slot = creator.Create(handle.getVariableName(), null);
            this.setVariable(slot);
        }

        return slot;
    }

    private IAnimationVariableSlot getOrCreateVariable(String key, AnimationVariableSource.AnimationVariableSlotGenerator creator) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        if (handle == null) {
            return null;
        } else {
            IAnimationVariableSlot slot = this.getVariable(handle);
            if (slot == null) {
                slot = creator.Create(handle.getVariableName(), null);
                this.setVariable(slot);
            }

            return slot;
        }
    }

    private <EnumType extends Enum<EnumType>> IAnimationVariableSlot getOrCreateVariable_Enum(String key, EnumType in_initialVal) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        if (handle == null) {
            return null;
        } else {
            IAnimationVariableSlot slot = this.getVariable(handle);
            if (slot == null) {
                Class<EnumType> enumClass = (Class<EnumType>)in_initialVal.getClass();
                slot = new AnimationVariableSlotEnum<>(enumClass, key, in_initialVal, null);
                this.setVariable(slot);
            }

            return slot;
        }
    }

    private IAnimationVariableSlot getOrCreateVariable_Bool(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotBool::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_String(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotString::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_Float(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotFloat::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_Bool(AnimationVariableHandle handle) {
        return this.getOrCreateVariable(handle, AnimationVariableSlotBool::new);
    }

    /**
     * Description copied from interface: IAnimationVariableMap
     */
    @Override
    public void setVariable(IAnimationVariableSlot var) {
        AnimationVariableHandle handle = var.getHandle();
        int handleIdx = handle.getVariableIndex();
        if (handleIdx >= this.cachedGameVariableSlots.length) {
            IAnimationVariableSlot[] newArray = new IAnimationVariableSlot[handleIdx + 1];
            IAnimationVariableSlot[] oldArray = this.cachedGameVariableSlots;
            if (oldArray != null) {
                PZArrayUtil.arrayCopy(newArray, oldArray, 0, oldArray.length);
            }

            this.cachedGameVariableSlots = newArray;
        }

        this.cachedGameVariableSlots[handleIdx] = var;
        if (var != null) {
            this.isEmpty = false;
        } else if (this.isEmpty) {
            this.isEmpty = !this.getGameVariables().iterator().hasNext();
        }
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, String value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_String(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, boolean value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Bool(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, float value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Float(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(AnimationVariableHandle handle, boolean value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Bool(handle);
        slot.setValue(value);
        return slot;
    }

    @Override
    public <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String in_key, EnumType in_val) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Enum(in_key, in_val);
        slot.setEnumValue(in_val);
        return slot;
    }

    @Override
    public void clearVariable(String key) {
        IAnimationVariableSlot var = this.getVariable(key);
        if (var != null) {
            var.clear();
        }
    }

    @Override
    public void clearVariables() {
        for (IAnimationVariableSlot var : this.getGameVariables()) {
            var.clear();
        }
    }

    public void removeAllVariables() {
        this.cachedGameVariableSlots = new IAnimationVariableSlot[0];
        this.isEmpty = true;
    }

    /**
     * Returns all Game variables.
     */
    @Override
    public Iterable<IAnimationVariableSlot> getGameVariables() {
        return () -> new Iterator<IAnimationVariableSlot>() {
            private int nextSlotIndex;
            private IAnimationVariableSlot nextSlot;

            {
                Objects.requireNonNull(AnimationVariableSource.this);
                this.nextSlotIndex = -1;
                this.nextSlot = this.findNextSlot();
            }

            @Override
            public boolean hasNext() {
                return this.nextSlot != null;
            }

            public IAnimationVariableSlot next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    IAnimationVariableSlot currentSlot = this.nextSlot;
                    this.nextSlot = this.findNextSlot();
                    return currentSlot;
                }
            }

            private IAnimationVariableSlot findNextSlot() {
                IAnimationVariableSlot nextSlot = null;

                while (this.nextSlotIndex + 1 < AnimationVariableSource.this.cachedGameVariableSlots.length) {
                    IAnimationVariableSlot slot = AnimationVariableSource.this.cachedGameVariableSlots[++this.nextSlotIndex];
                    if (slot != null) {
                        nextSlot = slot;
                        break;
                    }
                }

                return nextSlot;
            }
        };
    }

    /**
     * Compares (ignoring case) the value of the specified variable.
     *  Returns TRUE if they match.
     */
    @Override
    public boolean isVariable(String name, String val) {
        return StringUtils.equalsIgnoreCase(this.getVariableString(name), val);
    }

    @Override
    public boolean containsVariable(String key) {
        return this.getVariable(key) != null;
    }

    public interface AnimationVariableSlotGenerator {
        IAnimationVariableSlot Create(String arg0, IAnimationVariableSlotDescriptor arg1);
    }
}
