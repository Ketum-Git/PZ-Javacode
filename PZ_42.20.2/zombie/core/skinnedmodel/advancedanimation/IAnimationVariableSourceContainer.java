// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public interface IAnimationVariableSourceContainer extends IAnimationVariableSource {
    IAnimationVariableSource getGameVariablesInternal();

    @Override
    default IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        return this.getGameVariablesInternal().getVariable(handle);
    }

    @Override
    default IAnimationVariableSlot getVariable(String key) {
        return this.getGameVariablesInternal().getVariable(key);
    }

    @Override
    default String getVariableString(String name) {
        return this.getGameVariablesInternal().getVariableString(name);
    }

    @Override
    default float getVariableFloat(String name, float defaultVal) {
        return this.getGameVariablesInternal().getVariableFloat(name, defaultVal);
    }

    @Override
    default boolean getVariableBoolean(String name) {
        return this.getGameVariablesInternal().getVariableBoolean(name);
    }

    @Override
    default boolean getVariableBoolean(String key, boolean defaultVal) {
        return this.getGameVariablesInternal().getVariableBoolean(key, defaultVal);
    }

    @Override
    default Iterable<IAnimationVariableSlot> getGameVariables() {
        return this.getGameVariablesInternal().getGameVariables();
    }

    @Override
    default boolean isVariable(String name, String val) {
        return this.getGameVariablesInternal().isVariable(name, val);
    }

    @Override
    default boolean containsVariable(String name) {
        return this.getGameVariablesInternal().containsVariable(name);
    }
}
