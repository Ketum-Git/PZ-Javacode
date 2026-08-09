// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.input.JoypadAxis1d;
import zombie.input.JoypadAxis2d;
import zombie.input.JoypadButton;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum CharacterJoypadButtonBinding {
    Aim(JoypadAxis2d.RightStick, 0.1F),
    PrecisionAim(JoypadAxis1d.RightTrigger, -0.8F),
    Melee(JoypadAxis1d.LeftTrigger, -0.8F),
    Attack(JoypadAxis1d.RightTrigger, 0.93F),
    Run(JoypadAxis2d.LeftStick, 0.975F),
    Interact(JoypadButton.A),
    WalkTo,
    Crouch(JoypadButton.LeftStick),
    ReloadWeapon(JoypadButton.RightBump),
    RackFirearm(JoypadButton.LeftBump),
    Sprint(JoypadButton.RightStick),
    CancelAction(JoypadButton.B),
    ManualFloorAtk(JoypadButton.LeftBump),
    Inventory(JoypadButton.Y),
    Loot(JoypadButton.X),
    ClosePanel(JoypadButton.B),
    CycleLoot(JoypadButton.RightBump),
    CycleInventory(JoypadButton.LeftBump),
    CycleTabsLeft(JoypadButton.LeftBump),
    CycleTabsRight(JoypadButton.RightBump),
    TransferItem(JoypadButton.X),
    InteractOptions(JoypadButton.X),
    ClimbThrough(JoypadButton.B),
    SmashWindow(JoypadButton.B),
    Brakes(JoypadButton.B),
    CruiseControl(JoypadButton.X),
    ZoomIn,
    ZoomOut;

    private final CharacterJoypadButtonBinding.IsDownBinding defaultBinding;
    private CharacterJoypadButtonBinding.IsDownBinding binding;
    private static final CharacterJoypadButtonBinding[] values = values();

    private CharacterJoypadButtonBinding() {
        this.defaultBinding = null;
        this.setDefault();
    }

    private CharacterJoypadButtonBinding(final JoypadButton buttonBinding) {
        this.defaultBinding = new CharacterJoypadButtonBinding.JoypadButtonBinding(buttonBinding);
        this.setDefault();
    }

    private CharacterJoypadButtonBinding(final JoypadAxis1d axis1d, final float min, final float max) {
        this.defaultBinding = new CharacterJoypadButtonBinding.Axis1dMinMaxBinding(axis1d, min, max);
        this.setDefault();
    }

    private CharacterJoypadButtonBinding(final JoypadAxis1d axis1d, final float min) {
        this.defaultBinding = new CharacterJoypadButtonBinding.Axis1dMinMaxBinding(axis1d, min, Float.MAX_VALUE);
        this.setDefault();
    }

    private CharacterJoypadButtonBinding(final JoypadAxis2d axis2d, final float min, final float max) {
        this.defaultBinding = new CharacterJoypadButtonBinding.Axis2dMinMaxBinding(axis2d, min, max);
        this.setDefault();
    }

    private CharacterJoypadButtonBinding(final JoypadAxis2d axis2d, final float min) {
        this.defaultBinding = new CharacterJoypadButtonBinding.Axis2dMinMaxBinding(axis2d, min, Float.MAX_VALUE);
        this.setDefault();
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding[] allBindings() {
        return values;
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding findBinding(JoypadButton joypadButton) {
        return PZArrayUtil.find(allBindings(), joypadButton, (binding, button) -> binding.getJoypadButton() == button);
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding[] findBindings(JoypadButton joypadButton) {
        return PZArrayUtil.filtered(allBindings(), joypadButton, (binding, button) -> binding.getJoypadButton() == button);
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding[] findBindings(JoypadAxis1d joypadAxis) {
        return PZArrayUtil.filtered(allBindings(), joypadAxis, (binding, axis) -> binding.getJoypadAxis1d() == axis);
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding[] findBindings(JoypadAxis2d joypadAxis) {
        return PZArrayUtil.filtered(allBindings(), joypadAxis, (binding, axis) -> binding.getJoypadAxis2d() == axis);
    }

    @UsedFromLua
    public static CharacterJoypadButtonBinding fromString(String name) {
        return StringUtils.tryParseEnum(CharacterJoypadButtonBinding.class, name, null);
    }

    @UsedFromLua
    public String getNameTranslationKey() {
        return "UI_optionscreen_gamepad_CharacterJoypadButtonBinding_name_" + this.name();
    }

    @UsedFromLua
    public JoypadButton getJoypadButton() {
        return this.binding != null ? this.binding.getButton() : null;
    }

    @UsedFromLua
    public JoypadAxis1d getJoypadAxis1d() {
        return this.binding != null ? this.binding.getAxis1d() : null;
    }

    @UsedFromLua
    public JoypadAxis2d getJoypadAxis2d() {
        return this.binding != null ? this.binding.getAxis2d() : null;
    }

    public CharacterJoypadButtonBinding.IsDownBinding getBinding() {
        return this.binding;
    }

    @UsedFromLua
    public float getAxisMinThreshold() {
        return this.binding != null ? this.binding.getMin() : 0.0F;
    }

    @UsedFromLua
    public float getAxisMaxThreshold() {
        return this.binding != null ? this.binding.getMax() : 0.0F;
    }

    @UsedFromLua
    public boolean isAxisMaxThresholdInfinity() {
        return this.getAxisMaxThreshold() > 1.0F;
    }

    public boolean containsBinding(JoypadButton button) {
        return this.getJoypadButton() == button;
    }

    public boolean containsBinding(JoypadAxis1d axis1d) {
        return this.getJoypadAxis1d() == axis1d;
    }

    public boolean containsBinding(JoypadAxis2d axis2d) {
        return this.getJoypadAxis2d() == axis2d;
    }

    @UsedFromLua
    public void removeBinding(JoypadButton button) {
        if (this.containsBinding(button)) {
            this.binding = null;
        }
    }

    @UsedFromLua
    public void removeBinding(JoypadAxis1d axis1d) {
        if (this.containsBinding(axis1d)) {
            this.binding = null;
        }
    }

    @UsedFromLua
    public void removeBinding(JoypadAxis2d axis2d) {
        if (this.containsBinding(axis2d)) {
            this.binding = null;
        }
    }

    @UsedFromLua
    public void addBinding(JoypadButton button) {
        if (!this.containsBinding(button)) {
            this.setBinding(button);
        }
    }

    @UsedFromLua
    public void addBinding(JoypadAxis1d axis1d) {
        if (!this.containsBinding(axis1d)) {
            this.setBinding(axis1d, 0.5F);
        }
    }

    @UsedFromLua
    public void addBinding(JoypadAxis2d axis2d) {
        if (!this.containsBinding(axis2d)) {
            this.setBinding(axis2d, 0.5F);
        }
    }

    @UsedFromLua
    public void moveBindingFrom(CharacterJoypadButtonBinding fromBinding) {
        if (this != fromBinding) {
            this.binding = fromBinding.binding;
            fromBinding.binding = null;
        }
    }

    public void setBinding(JoypadButton newBinding) {
        this.binding = new CharacterJoypadButtonBinding.JoypadButtonBinding(newBinding);
    }

    public void setBinding(JoypadAxis1d axis1d, float min, float max) {
        this.binding = new CharacterJoypadButtonBinding.Axis1dMinMaxBinding(axis1d, min, max);
    }

    public void setBinding(JoypadAxis1d axis1d, float min) {
        this.binding = new CharacterJoypadButtonBinding.Axis1dMinMaxBinding(axis1d, min, Float.MAX_VALUE);
    }

    public void setBinding(JoypadAxis2d axis2d, float min, float max) {
        this.binding = new CharacterJoypadButtonBinding.Axis2dMinMaxBinding(axis2d, min, max);
    }

    public void setBinding(JoypadAxis2d axis2d, float min) {
        this.binding = new CharacterJoypadButtonBinding.Axis2dMinMaxBinding(axis2d, min, Float.MAX_VALUE);
    }

    public void setDefault() {
        this.binding = this.defaultBinding;
    }

    @UsedFromLua
    public static void setAllToDefault() {
        for (CharacterJoypadButtonBinding binding : allBindings()) {
            binding.setDefault();
        }
    }

    public boolean isDown(int joypadBind) {
        return this.binding != null && this.binding.isDown(joypadBind);
    }

    public static class Axis1dMinMaxBinding implements CharacterJoypadButtonBinding.IsDownBinding {
        private final JoypadAxis1d binding;
        private final float min;
        private final float max;

        public Axis1dMinMaxBinding(JoypadAxis1d binding, float min, float max) {
            this.binding = binding;
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean isDown(int joypadBind) {
            return this.binding != null && PZMath.isBetween(this.binding.getValue(joypadBind), this.min, this.max);
        }

        @Override
        public JoypadAxis1d getAxis1d() {
            return this.binding;
        }

        @Override
        public float getMin() {
            return this.min;
        }

        @Override
        public float getMax() {
            return this.max;
        }
    }

    public static class Axis2dMinMaxBinding implements CharacterJoypadButtonBinding.IsDownBinding {
        private final JoypadAxis2d binding;
        private final float min;
        private final float max;

        public Axis2dMinMaxBinding(JoypadAxis2d binding, float min, float max) {
            this.binding = binding;
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean isDown(int joypadBind) {
            return this.binding != null && PZMath.isBetween(this.binding.getLength(joypadBind), this.min, this.max);
        }

        @Override
        public JoypadAxis2d getAxis2d() {
            return this.binding;
        }

        @Override
        public float getMin() {
            return this.min;
        }

        @Override
        public float getMax() {
            return this.max;
        }
    }

    public interface IsDownBinding {
        boolean isDown(int var1);

        default JoypadButton getButton() {
            return null;
        }

        default JoypadAxis1d getAxis1d() {
            return null;
        }

        default JoypadAxis2d getAxis2d() {
            return null;
        }

        default float getMin() {
            return 0.0F;
        }

        default float getMax() {
            return 0.0F;
        }
    }

    public static class JoypadButtonBinding implements CharacterJoypadButtonBinding.IsDownBinding {
        private final JoypadButton binding;

        public JoypadButtonBinding(JoypadButton binding) {
            this.binding = binding;
        }

        @Override
        public boolean isDown(int joypadBind) {
            return this.binding != null && this.binding.isDown(joypadBind);
        }

        @Override
        public JoypadButton getButton() {
            return this.binding;
        }
    }
}
