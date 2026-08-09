// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.UsedFromLua;

@UsedFromLua
public enum JoypadAxis1d {
    LeftTrigger(JoypadManager::getLTValue, JoypadManager::getLeftTriggerDeadZone, JoypadManager::setLeftTriggerDeadZone),
    RightTrigger(JoypadManager::getRTValue, JoypadManager::getRightTriggerDeadZone, JoypadManager::setRightTriggerDeadZone),
    LeftStickX(JoypadManager::getMovementAxisX, JoypadManager::getMovementAxisDeadZoneX, JoypadManager::setMovementAxisDeadZoneX),
    LeftStickY(JoypadManager::getMovementAxisY, JoypadManager::getMovementAxisDeadZoneY, JoypadManager::setMovementAxisDeadZoneY),
    RightStickX(JoypadManager::getAimingAxisX, JoypadManager::getAimingAxisDeadZoneX, JoypadManager::setAimingAxisDeadZoneX),
    RightStickY(JoypadManager::getAimingAxisY, JoypadManager::getAimingAxisDeadZoneY, JoypadManager::setAimingAxisDeadZoneY);

    private final AxisValueSupplier axisSupplier;
    private final AxisValueSupplier deadZoneSupplier;
    private final AxisValueAcceptor deadZoneAcceptor;
    private static final JoypadAxis1d[] values = values();

    private JoypadAxis1d(final AxisValueSupplier axisSupplier, final AxisValueSupplier deadZoneSupplier, final AxisValueAcceptor deadZoneAcceptor) {
        this.axisSupplier = axisSupplier;
        this.deadZoneSupplier = deadZoneSupplier;
        this.deadZoneAcceptor = deadZoneAcceptor;
    }

    @UsedFromLua
    public String getNameTranslationKey() {
        return "UI_optionscreen_gamepad_JoypadAxis1d_name_" + this.name();
    }

    @UsedFromLua
    public float getValue(int joypadBind) {
        return this.axisSupplier.getAxisValue(JoypadManager.instance, joypadBind);
    }

    @UsedFromLua
    public static JoypadAxis1d[] getAxes() {
        return values;
    }

    @UsedFromLua
    public static int getAxisCount() {
        return values.length;
    }

    @UsedFromLua
    public float getDeadZone(int joypadBind) {
        return this.deadZoneSupplier.getAxisValue(JoypadManager.instance, joypadBind);
    }

    @UsedFromLua
    public void setDeadZone(int joypadBind, float newValue) {
        this.deadZoneAcceptor.setAxisValue(JoypadManager.instance, joypadBind, newValue);
    }

    @UsedFromLua
    public static JoypadAxis1d fromIndex(int axisIdx) {
        if (axisIdx >= 0 && axisIdx < getAxisCount()) {
            return values[axisIdx];
        } else {
            throw new ArrayIndexOutOfBoundsException(String.format("Index out of range: %d. Expected 0 - %d", axisIdx, getAxisCount()));
        }
    }
}
