// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IAnimationVariableRegistry extends IAnimationVariableSourceContainer {
    AnimationVariableSource getGameVariablesInternal();

    default void setVariable(String key, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(String key, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(String in_key, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(in_key, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(String in_key, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(in_key, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackInt.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String in_key, boolean defaultVal, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        boolean defaultVal,
        AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String in_key, String defaultVal, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        String defaultVal,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String in_key, float defaultVal, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        float defaultVal,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String in_key, int defaultVal, AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String in_key,
        int defaultVal,
        AnimationVariableSlotCallbackInt.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackInt.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default <EnumType extends Enum<EnumType>> void setVariable(
        String in_key, Class<EnumType> in_enumTypeClass, Supplier<EnumType> callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, in_enumTypeClass, callbackGet, descriptor);
    }

    default <EnumType extends Enum<EnumType>> void setVariable(
        String in_key,
        Class<EnumType> in_enumTypeClass,
        Supplier<EnumType> callbackGet,
        Consumer<EnumType> callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(in_key, in_enumTypeClass, callbackGet, callbackSet, descriptor);
    }
}
