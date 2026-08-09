// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.input.JoypadAxis2d;
import zombie.iso.Vector2;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum CharacterJoypadAxis2dBinding {
    Movement(JoypadAxis2d.LeftStick),
    Aiming(JoypadAxis2d.RightStick);

    private final JoypadAxis2d defaultBinding;
    private JoypadAxis2d binding;
    private static final CharacterJoypadAxis2dBinding[] values = values();

    private CharacterJoypadAxis2dBinding(final JoypadAxis2d axisBinding) {
        this.defaultBinding = axisBinding;
        this.setDefault();
    }

    @UsedFromLua
    public String getNameTranslationKey() {
        return "UI_optionscreen_gamepad_CharacterJoypadAxis2dBinding_name_" + this.name();
    }

    @UsedFromLua
    public static CharacterJoypadAxis2dBinding[] allBindings() {
        return values;
    }

    @UsedFromLua
    public static CharacterJoypadAxis2dBinding fromString(String name) {
        return StringUtils.tryParseEnum(CharacterJoypadAxis2dBinding.class, name, null);
    }

    @UsedFromLua
    public JoypadAxis2d getJoypadAxis() {
        return this.binding;
    }

    public JoypadAxis2d getBinding() {
        return this.binding;
    }

    public void setBinding(JoypadAxis2d newBinding) {
        this.binding = newBinding;
    }

    @UsedFromLua
    public void removeBinding(JoypadAxis2d binding) {
        if (this.binding == binding) {
            this.binding = null;
        }
    }

    @UsedFromLua
    public void moveBindingFrom(CharacterJoypadAxis2dBinding fromBinding) {
        if (this != fromBinding) {
            this.binding = fromBinding.binding;
            fromBinding.binding = null;
        }
    }

    public void setDefault() {
        this.binding = this.defaultBinding;
    }

    @UsedFromLua
    public static void setAllToDefault() {
        for (CharacterJoypadAxis2dBinding binding : allBindings()) {
            binding.setDefault();
        }
    }

    public float getLength(int joypadBind) {
        return PZMath.getLength(this.getValueX(joypadBind), this.getValueY(joypadBind));
    }

    public Vector2 getValue(int joypadBind, Vector2 out) {
        return out.set(this.getValueX(joypadBind), this.getValueY(joypadBind));
    }

    public float getValueX(int joypadBind) {
        return this.binding != null ? this.binding.getValueX(joypadBind) : 0.0F;
    }

    public float getValueY(int joypadBind) {
        return this.binding != null ? this.binding.getValueY(joypadBind) : 0.0F;
    }

    public boolean isApplied(int joypadBind) {
        return this.binding != null && this.binding.isApplied(joypadBind);
    }

    @UsedFromLua
    public static CharacterJoypadAxis2dBinding[] findBindings(JoypadAxis2d joypadAxis) {
        return PZArrayUtil.filtered(allBindings(), joypadAxis, (binding, axis) -> binding.getJoypadAxis() == axis);
    }
}
