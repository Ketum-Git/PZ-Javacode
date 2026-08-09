// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.characters.component.CharacterInputComponent;
import zombie.characters.ecs.ECSEntity;
import zombie.iso.Vector2;
import zombie.util.lambda.PZOptional;

public interface CharacterInputComponentEntity extends ECSEntity {
    default CharacterInputComponent getCharacterInputComponent() {
        return this.tryGetECSComponent(CharacterInputComponent.class);
    }

    default int getJoypadBind() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), -1, CharacterInputComponent::getJoypadBind);
    }

    default void setJoypadBind(int joypadBind) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setJoypadBind, joypadBind);
    }

    default CharacterInputMode getInputMode() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputMode.NONE, CharacterInputComponent::getInputMode);
    }

    default boolean isForceAim() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isForceAim);
    }

    default void setForceAim(boolean forceAim) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setForceAim, forceAim);
    }

    default boolean toggleForceAim() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::toggleForceAim);
    }

    default boolean isForceSprint() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isForceSprint);
    }

    default void setForceSprint(boolean forceSprint) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setForceSprint, forceSprint);
    }

    default boolean isForceRun() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isForceRun);
    }

    default void setForceRun(boolean forceRun) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setForceRun, forceRun);
    }

    default Vector2 getInputMoveVector(Vector2 out) {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), out.set(0.0F, 0.0F), CharacterInputComponent::getInputMoveVector, out);
    }

    default float getInputMovementRate() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), 0.0F, CharacterInputComponent::getInputMovementRate);
    }

    default boolean isInputMoveAxisApplied() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), false, CharacterInputComponent::isInputMoveAxisApplied);
    }

    default boolean isAimKeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isAimKeyDown);
    }

    default boolean isPrecisionAimKeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isPrecisionAimKeyDown);
    }

    default boolean isAnyAimKeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isAnyAimKeyDown);
    }

    default boolean isMeleeButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isMeleeButtonDown);
    }

    default boolean isAttackButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isAttackButtonDown);
    }

    default boolean isBuildButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isBuildButtonDown);
    }

    default boolean isBuildButtonReleased() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isBuildButtonReleased);
    }

    default boolean isRunButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isRunButtonDown);
    }

    default boolean wasRunButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::wasRunButtonDown);
    }

    default boolean isInteractButtonPressed() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isInteractButtonPressed);
    }

    default boolean isInteractButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isInteractButtonDown);
    }

    default boolean isInteractButtonClicked() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isInteractButtonClicked);
    }

    default boolean isWalkToButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isWalkToButtonDown);
    }

    default boolean isCrouchButtonPressed() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isCrouchButtonPressed);
    }

    default boolean isSprintButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isSprintButtonDown);
    }

    default boolean isManualFloorAtkButtonDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isManualFloorAtkButtonDown);
    }

    default boolean isShiftKeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isShiftKeyDown);
    }

    default boolean isF12KeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isF12KeyDown);
    }

    default boolean isChangeCharacterKeyDown() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isChangeCharacterKeyDown);
    }

    default void setJoypadButtonsActive(boolean joypadMovementActive) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setJoypadButtonsActive, joypadMovementActive);
    }

    default boolean isJoypadButtonsActive() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isJoypadButtonsActive);
    }

    @UsedFromLua
    default void setIgnoreInputsForDirection(boolean ignoreInputsForDirection) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setIgnoreInputsForDirection, ignoreInputsForDirection);
    }

    @UsedFromLua
    default boolean isIgnoreInputsForDirection() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isIgnoreInputsForDirection);
    }

    @UsedFromLua
    default void setJoypadIgnoreAim(boolean ignore) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setJoypadIgnoreAim, ignore);
    }

    @UsedFromLua
    default void setJoypadIgnoreAimUntilCentered(boolean ignore) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setJoypadIgnoreAimUntilCentered, ignore);
    }

    @UsedFromLua
    default boolean isJoypadIgnoreAimUntilCentered() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isJoypadIgnoreAimUntilCentered);
    }

    default void setIgnoreAimingInput(boolean b) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setIgnoreAimingInput, b);
    }

    default boolean isIgnoringAimingInput() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::isIgnoringAimingInput);
    }

    default boolean isAllowSprint() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), false, CharacterInputComponent::isAllowSprint);
    }

    default void setAllowSprint(boolean allowSprint) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setAllowSprint, allowSprint);
    }

    default boolean isAllowRun() {
        return PZOptional.ifPresent(this.getCharacterInputComponent(), false, CharacterInputComponent::isAllowRun);
    }

    default void setAllowRun(boolean allowRun) {
        PZOptional.ifPresent(this.getCharacterInputComponent(), CharacterInputComponent::setAllowRun, allowRun);
    }
}
