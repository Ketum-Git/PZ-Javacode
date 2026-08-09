// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.characters.CharacterInputMode;
import zombie.characters.IsoPlayer;
import zombie.characters.component.CharacterInputComponent;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;
import zombie.util.lambda.PZOptional;
import zombie.util.list.PZArrayUtil;

public class AimingReticle {
    public static final float AIMING_AXIS_MIN = 0.2F;
    public static final float AIMING_AXIS_MAX = 0.99F;
    public static final float AIMING_VALUE_MIN = 0.01F;
    public static final float AIMING_VALUE_MAX = 0.98F;
    private static final Vector2[] aimingPositions = PZArrayUtil.newInstance(Vector2.class, 4, Vector2::new);
    private static final Vector2 axisVec = new Vector2();

    public static int getX(int playerIndex) {
        return (int)(getXA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getY(int playerIndex) {
        return (int)(getYA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getXA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        return player != null && player.getInputMode() == CharacterInputMode.GAMEPAD ? (int)aimingPositions[playerIndex].x : Mouse.getXA();
    }

    public static int getYA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        return player != null && player.getInputMode() == CharacterInputMode.GAMEPAD ? (int)aimingPositions[playerIndex].y : Mouse.getYA();
    }

    private static float axisToScreenX(IsoPlayer player, float aimingX) {
        float screenHalfWidth = getAimingCenterX(player);
        return screenHalfWidth * (1.0F + aimingX);
    }

    private static float axisToScreenY(IsoPlayer player, float aimingY) {
        float screenHalfHeight = getAimingCenterY(player);
        return screenHalfHeight * (1.0F + aimingY);
    }

    private static float renormalizeAimingAxisValue(IsoPlayer player, float aimingAxisVal) {
        float zoom = Core.getInstance().getZoom(player.getIndex());
        float aimingAxisSign = PZMath.sign(aimingAxisVal);
        float renormalizedAxis = PZMath.clamp((aimingAxisVal * aimingAxisSign - 0.2F) / 0.79F, 0.0F, 1.0F) * aimingAxisSign;
        return PZMath.lerp(0.01F / zoom, 0.98F, renormalizedAxis * aimingAxisSign, LerpType.EaseOutQuad) * aimingAxisSign;
    }

    private static float getAimingScreenX(IsoPlayer player) {
        float aimingAxisRaw = PZOptional.ifPresent(
                player.tryGetECSComponent(CharacterInputComponent.class), axisVec.set(0.0F, 0.0F), CharacterInputComponent::getJoypadAimVector, axisVec
            )
            .x;
        float aimingAxis = renormalizeAimingAxisValue(player, aimingAxisRaw);
        return axisToScreenX(player, aimingAxis);
    }

    private static float getAimingScreenY(IsoPlayer player) {
        float aimingAxisRaw = PZOptional.ifPresent(
                player.tryGetECSComponent(CharacterInputComponent.class), axisVec.set(0.0F, 0.0F), CharacterInputComponent::getJoypadAimVector, axisVec
            )
            .y;
        float aimingAxis = renormalizeAimingAxisValue(player, aimingAxisRaw);
        return axisToScreenY(player, aimingAxis);
    }

    private static float getAimingCenterX(IsoPlayer player) {
        int playerIndex = player.getIndex();
        int screenWidth = IsoCamera.getScreenWidth(playerIndex);
        return screenWidth / 2.0F;
    }

    private static float getAimingCenterY(IsoPlayer player) {
        int playerIndex = player.getIndex();
        int screenHeight = IsoCamera.getScreenHeight(playerIndex);
        return screenHeight / 2.0F - player.getScreenChestHeight();
    }

    public static void update() {
        for (int i = 0; i < aimingPositions.length; i++) {
            IsoPlayer player = IsoPlayer.getPlayer(i);
            if (player != null && player.getInputMode() == CharacterInputMode.GAMEPAD) {
                AimingMode aimingMode = player.getAimingMode();
                aimingMode.lerpAiming(
                    player,
                    aimingPositions[i],
                    getAimingScreenX(player),
                    getAimingScreenY(player),
                    getAimingCenterX(player),
                    getAimingCenterY(player),
                    Core.getInstance().getZoom(i),
                    aimingPositions[i]
                );
            }
        }
    }
}
