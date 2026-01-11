// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public interface IAnimationVariableSource {
    /**
     * Returns the specified variable slot. Or NULL if not found.
     */
    IAnimationVariableSlot getVariable(AnimationVariableHandle handle);

    /**
     * Returns the specified variable slot. Or NULL if not found.
     */
    default IAnimationVariableSlot getVariable(String key) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        return this.getVariable(handle);
    }

    /**
     * Returns the specified variable. Or an empty string "" if not found.
     */
    default String getVariableString(String key) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueString() : "";
    }

    /**
     * Returns the specified variable, as a float.
     *   Attempts to convert the string variable to a float.
     *   If that fails, or if variable not found, returns the defaultValue
     */
    default float getVariableFloat(String key, float defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueFloat() : defaultVal;
    }

    /**
     * Returns the specified variable, as a boolean.
     *   Attempts to convert the string variable to a boolean.
     *   If that fails, or if variable not found, returns FALSE
     */
    default boolean getVariableBoolean(String key) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null && slot.getValueBool();
    }

    /**
     * Returns the specified variable, as a boolean.
     *  Attempts to convert the string variable to a boolean.
     *  If that fails, or if variable not found, returns defaultVal
     */
    default boolean getVariableBoolean(String key, boolean defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(key);
        return slot != null ? slot.getValueBool() : defaultVal;
    }

    default boolean getVariableBoolean(AnimationVariableHandle handle) {
        IAnimationVariableSlot slot = this.getVariable(handle);
        return slot != null && slot.getValueBool();
    }

    default <EnumType extends Enum<EnumType>> EnumType getVariableEnum(String in_key, EnumType in_defaultVal) {
        IAnimationVariableSlot slot = this.getVariable(in_key);
        return slot != null ? slot.getEnumValue(in_defaultVal) : in_defaultVal;
    }

    /**
     * Returns all Game variables.
     */
    Iterable<IAnimationVariableSlot> getGameVariables();

    /**
     * Compares (ignoring case) the value of the specified variable.
     *  Returns TRUE if they match.
     */
    boolean isVariable(String name, String val);

    boolean containsVariable(String name);

    default IAnimationVariableSource getSubVariableSource(String in_subVariableSourceName) {
        return null;
    }
}
