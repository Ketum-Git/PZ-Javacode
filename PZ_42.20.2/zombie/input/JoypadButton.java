// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.UsedFromLua;

@UsedFromLua
public enum JoypadButton {
    A(JoypadManager::isAPressed),
    B(JoypadManager::isBPressed),
    X(JoypadManager::isXPressed),
    Y(JoypadManager::isYPressed),
    LeftStick(JoypadManager::isL3Pressed),
    RightStick(JoypadManager::isR3Pressed),
    LeftBump(JoypadManager::isLBPressed),
    RightBump(JoypadManager::isRBPressed),
    Back(JoypadManager::isBackPressed),
    Start(JoypadManager::isStartPressed),
    Guide(JoypadManager::isGuidePressed),
    DPadLeft(JoypadManager::isLeftPressed),
    DPadRight(JoypadManager::isRightPressed),
    DPadUp(JoypadManager::isUpPressed),
    DPadDown(JoypadManager::isDownPressed);

    private final IsButtonDownSupplier buttonDownSupplier;
    private static final JoypadButton[] values = values();

    private JoypadButton(final IsButtonDownSupplier buttonDownSupplier) {
        this.buttonDownSupplier = buttonDownSupplier;
    }

    @UsedFromLua
    public boolean isDown(int joypadBind) {
        return this.buttonDownSupplier.isDown(JoypadManager.instance, joypadBind);
    }

    @UsedFromLua
    public String getNameTranslationKey() {
        return "UI_optionscreen_gamepad_JoypadButton_name_" + this.name();
    }

    public static int getButtonCount() {
        return values.length;
    }

    @UsedFromLua
    public static boolean isButtonDown(int joypadBind, int buttonIdx) {
        return buttonIdx >= 0 && buttonIdx < getButtonCount() && fromIndex(buttonIdx).isDown(joypadBind);
    }

    @UsedFromLua
    public static JoypadButton[] getButtons() {
        return values;
    }

    public static JoypadButton fromIndex(int buttonIdx) {
        if (buttonIdx >= 0 && buttonIdx < getButtonCount()) {
            return values[buttonIdx];
        } else {
            throw new ArrayIndexOutOfBoundsException(String.format("Index out of range: %d. Expected 0 - %d", buttonIdx, getButtonCount()));
        }
    }
}
