// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.iso.Vector2;

@UsedFromLua
public enum JoypadAxis2d {
    LeftStick(JoypadManager::getMovementAxisX, JoypadManager::getMovementAxisY, JoypadManager::isMovementAxisBeingApplied),
    RightStick(JoypadManager::getAimingAxisX, JoypadManager::getAimingAxisY, JoypadManager::isAimingAxisBeingApplied);

    private final AxisValueSupplier axisSupplierX;
    private final AxisValueSupplier axisSupplierY;
    private final JoypadAxis2d.IsAxisAppliedSupplier isAppliedSupplier;
    private static final JoypadAxis2d[] values = values();

    private JoypadAxis2d(
        final AxisValueSupplier axisSupplierX, final AxisValueSupplier axisSupplierY, final JoypadAxis2d.IsAxisAppliedSupplier isAppliedSupplier
    ) {
        this.axisSupplierX = axisSupplierX;
        this.axisSupplierY = axisSupplierY;
        this.isAppliedSupplier = isAppliedSupplier;
    }

    @UsedFromLua
    public String getNameTranslationKey() {
        return "UI_optionscreen_gamepad_JoypadAxis2d_name_" + this.name();
    }

    public float getLength(int joypadBind) {
        return PZMath.getLength(this.getValueX(joypadBind), this.getValueY(joypadBind));
    }

    public Vector2 getValue(int joypadBind, Vector2 out) {
        return out.set(this.getValueX(joypadBind), this.getValueY(joypadBind));
    }

    public float getValueX(int joypadBind) {
        return this.axisSupplierX.getAxisValue(JoypadManager.instance, joypadBind);
    }

    public float getValueY(int joypadBind) {
        return this.axisSupplierY.getAxisValue(JoypadManager.instance, joypadBind);
    }

    public boolean isApplied(int joypadBind) {
        return this.isAppliedSupplier.isApplied(JoypadManager.instance, joypadBind);
    }

    @UsedFromLua
    public static JoypadAxis2d[] getAxes() {
        return values;
    }

    public interface IsAxisAppliedSupplier {
        boolean isApplied(JoypadManager var1, int var2);
    }
}
