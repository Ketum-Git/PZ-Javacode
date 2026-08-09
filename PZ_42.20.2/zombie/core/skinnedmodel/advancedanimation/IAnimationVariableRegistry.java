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
        String key,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(String key, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(String key, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet,
        AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String key, boolean defaultVal, AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        boolean defaultVal,
        AnimationVariableSlotCallbackBool.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackBool.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String key, String defaultVal, AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        String defaultVal,
        AnimationVariableSlotCallbackString.CallbackGetStrongTyped callbackGet,
        AnimationVariableSlotCallbackString.CallbackSetStrongTyped callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String key, float defaultVal, AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        float defaultVal,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatSupplier callbackGet,
        AnimationVariableSlotCallbackFloat.PrimitiveFloatConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default void setVariable(
        String key, int defaultVal, AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, descriptor);
    }

    default void setVariable(
        String key,
        int defaultVal,
        AnimationVariableSlotCallbackInt.PrimitiveIntSupplier callbackGet,
        AnimationVariableSlotCallbackInt.PrimitiveIntConsumer callbackSet,
        IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, defaultVal, callbackGet, callbackSet, descriptor);
    }

    default <EnumType extends Enum<EnumType>> void setVariable(
        String key, Class<EnumType> enumTypeClass, Supplier<EnumType> callbackGet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, enumTypeClass, callbackGet, descriptor);
    }

    default <EnumType extends Enum<EnumType>> void setVariable(
        String key, Class<EnumType> enumTypeClass, Supplier<EnumType> callbackGet, Consumer<EnumType> callbackSet, IAnimationVariableSlotDescriptor descriptor
    ) {
        this.getGameVariablesInternal().setVariable(key, enumTypeClass, callbackGet, callbackSet, descriptor);
    }
}
