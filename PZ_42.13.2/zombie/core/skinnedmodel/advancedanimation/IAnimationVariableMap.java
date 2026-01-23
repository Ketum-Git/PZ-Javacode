// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

public interface IAnimationVariableMap extends IAnimationVariableSource {
    /**
     * Set the specified animation variable slot. Overwriting an existing slot if necessary.
     */
    void setVariable(IAnimationVariableSlot slot);

    IAnimationVariableSlot setVariable(String arg0, String arg1);

    IAnimationVariableSlot setVariable(String arg0, boolean arg1);

    IAnimationVariableSlot setVariable(String arg0, float arg1);

    IAnimationVariableSlot setVariable(AnimationVariableHandle arg0, boolean arg1);

    <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String var1, EnumType var2);

    void clearVariable(String key);

    void clearVariables();
}
