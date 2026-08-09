// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import zombie.GameWindow;
import zombie.characters.CharacterInputKeyBinding;
import zombie.characters.CharacterInputMode;
import zombie.characters.CharacterJoypadAxis2dBinding;
import zombie.characters.CharacterJoypadButtonBinding;
import zombie.characters.TimedInputHandler;
import zombie.characters.ecs.ECSComponent;
import zombie.characters.ecs.componentmods.ECSFrameStep;
import zombie.characters.ecs.componentmods.ECSGameLoadingStateEnter;
import zombie.characters.ecs.componentmods.ECSInGameStateEnter;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableLogger;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.input.GameKeyboard;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.ui.UIManager;

public class CharacterInputComponent extends ECSComponent implements ECSFrameStep, ECSInGameStateEnter, ECSGameLoadingStateEnter, IAnimationVariableLogger {
    public static final float GAMEPAD_MIN_VALUE_TRIGGER_AIMING = -0.8F;
    public static final float GAMEPAD_MIN_VALUE_TRIGGER_SHOOT = 0.93F;
    public static final float GAMEPAD_MIN_VALUE_TRIGGER_MELEE = -0.8F;
    public static final float GAMEPAD_AIM_VALUE_MIN = 0.1F;
    public static final float MOVEMENT_RATE_STOPPED = 0.0F;
    public static final float MOVEMENT_RATE_MAX_WALKING = 1.95F;
    public static final float MOVEMENT_RATE_MAX = 2.0F;
    public static final float GAMEPAD_MIN_VALUE_RUN = 0.975F;
    private static final Vector2 joypadMoveVector = new Vector2();
    private static final Vector2 joypadAimVector = new Vector2();
    private int joypadBind = -1;
    private boolean joypadButtonsActive = true;
    private boolean ignoreInputsForDirection;
    private boolean joypadIgnoreAim;
    private boolean joypadIgnoreAimUntilCentered;
    private boolean ignoreAimingInput;
    private boolean allowSprint = true;
    private boolean allowRun = true;
    private boolean forceAim;
    private boolean forceRun;
    private boolean forceSprint;
    private final TimedInputHandler melee = new TimedInputHandler(this::isMeleeButtonDownInternal, null, null);
    private final TimedInputHandler toggleAim = new TimedInputHandler(
        this::isToggleAimKeyDown, this::isToggleAimKeyMouse, () -> !CharacterInputComponent.Options.isToggleToAim()
    );
    private final TimedInputHandler toggleSprint = new TimedInputHandler(
        this::isToggleSprintButtonDown, null, () -> !CharacterInputComponent.Options.isToggleToSprint()
    );
    private final TimedInputHandler toggleRun = new TimedInputHandler(this::isToggleRunButtonDown, null, () -> !CharacterInputComponent.Options.isToggleToRun());
    private final TimedInputHandler toggleCrouch = new TimedInputHandler(this::isToggleCrouchButtonDown, null, null);
    private final TimedInputHandler interact = new TimedInputHandler(this::isInteractButtonDownInternal, null, () -> !this.isJoypadButtonsActive());
    private final TimedInputHandler reloadWeapon = new TimedInputHandler(this::isReloadWeaponButtonDownInternal, null, () -> !this.isJoypadButtonsActive());
    private final TimedInputHandler rackFirearm = new TimedInputHandler(this::isRackFirearmButtonDownInternal, null, () -> !this.isJoypadButtonsActive());
    private final TimedInputHandler zoomIn = new TimedInputHandler(this::isZoomInButtonDownInternal, null, () -> !this.isJoypadButtonsActive());
    private final TimedInputHandler zoomOut = new TimedInputHandler(this::isZoomOutButtonDownInternal, null, () -> !this.isJoypadButtonsActive());
    private final TimedInputHandler build = new TimedInputHandler(this::isBuildButtonDown, null, () -> !this.isJoypadButtonsActive());

    @Override
    public void frameStep() {
        this.checkJoypadIgnoreAimUntilCentered();
        this.updateToggleButtons();
    }

    public int getJoypadBind() {
        return this.joypadBind;
    }

    public void setJoypadBind(int joypadBind) {
        this.joypadBind = joypadBind;
    }

    public CharacterInputMode getInputMode() {
        return this.isJoypadControllerActive() ? CharacterInputMode.GAMEPAD : CharacterInputMode.KEYBOARD;
    }

    public boolean isForceAim() {
        return this.forceAim;
    }

    public void setForceAim(boolean forceAim) {
        this.forceAim = forceAim;
    }

    public final boolean toggleForceAim() {
        this.setForceAim(!this.isForceAim());
        return this.isForceAim();
    }

    public boolean isForceSprint() {
        return this.forceSprint;
    }

    public void setForceSprint(boolean forceSprint) {
        this.forceSprint = forceSprint;
    }

    public final boolean toggleForceSprint() {
        this.setForceSprint(!this.isForceSprint());
        return this.isForceSprint();
    }

    public boolean isForceRun() {
        return this.forceRun;
    }

    public void setForceRun(boolean forceRun) {
        this.forceRun = forceRun;
    }

    public final boolean toggleForceRun() {
        this.setForceRun(!this.isForceRun());
        return this.isForceRun();
    }

    public void updateToggleButtons() {
        this.melee.updateState();
        this.updateToggleToAim();
        this.updateToggleToRun();
        this.updateToggleToSprint();
        this.toggleCrouch.updateState();
        this.interact.updateState();
        this.reloadWeapon.updateState();
        this.rackFirearm.updateState();
        this.zoomIn.updateState();
        this.zoomOut.updateState();
        this.build.updateState();
    }

    public void updateToggleToAim() {
        this.toggleAim.updateState();
        if (this.toggleAim.isMuted()) {
            this.setForceAim(false);
        } else {
            if (this.toggleAim.isKeyReleased() && this.toggleAim.wasMouseKey() != this.toggleAim.isKeyClicked()) {
                this.toggleForceAim();
            }
        }
    }

    public void updateToggleToSprint() {
        this.toggleSprint.updateState();
        if (this.toggleSprint.isMuted()) {
            this.setForceSprint(false);
        } else if (!this.shouldAllowForceRunOrSprint()) {
            this.setForceSprint(false);
        } else {
            if (this.toggleSprint.isKeyClicked()) {
                this.toggleForceSprint();
            }
        }
    }

    public void updateToggleToRun() {
        this.toggleRun.updateState();
        if (this.toggleRun.isMuted()) {
            this.setForceRun(false);
        } else if (!this.shouldAllowForceRunOrSprint()) {
            this.setForceRun(false);
        } else {
            if (this.toggleRun.isKeyClicked()) {
                this.toggleForceRun();
            }
        }
    }

    private boolean shouldAllowForceRunOrSprint() {
        IAnimationVariableSource parentVariables = this.tryGetECSOwnerEntityAs(IAnimationVariableSource.class);
        return parentVariables == null || !parentVariables.getVariableBoolean("ClimbWindowStarted") && !parentVariables.getVariableBoolean("ClimbFenceStarted");
    }

    public boolean isJoypadControllerActive() {
        return GameWindow.activatedJoyPad != null && this.getJoypadBind() != -1;
    }

    public Vector2 getInputMoveVector(Vector2 out) {
        out.set(0.0F, 0.0F);
        boolean isRunButtonDown = this.isAllowRun() && this.isRunButtonDown();
        float maxMovementRate = isRunButtonDown ? 2.0F : 1.95F;
        switch (this.getInputMode()) {
            case KEYBOARD:
                if (this.isForwardKeyDown()) {
                    IsoDirections.N.addToVector(out, out);
                } else if (this.isBackwardKeyDown()) {
                    IsoDirections.S.addToVector(out, out);
                }

                if (this.isLeftKeyDown()) {
                    IsoDirections.W.addToVector(out, out);
                } else if (this.isRightKeyDown()) {
                    IsoDirections.E.addToVector(out, out);
                }

                out.setLength(maxMovementRate);
                return out;
            case GAMEPAD:
                if (!this.isJoypadMovementAxisApplied()) {
                    return out;
                }

                CharacterJoypadAxis2dBinding.Movement.getValue(this.getJoypadBind(), out);
                out.scale(maxMovementRate);
                out.setMaxLength(maxMovementRate);
                return out;
            default:
                return out;
        }
    }

    public float getInputMovementRate() {
        return this.getInputMoveVector(joypadMoveVector).getLength();
    }

    public boolean isInputMoveAxisApplied() {
        return !PZMath.equal(this.getInputMovementRate(), 0.0F);
    }

    public Vector2 getJoypadAimVector(Vector2 out) {
        if (!this.isJoypadAimingAxisApplied()) {
            return out.set(0.0F, 0.0F);
        } else {
            CharacterJoypadAxis2dBinding.Aiming.getValue(this.getJoypadBind(), out);
            return out;
        }
    }

    public boolean isForwardKeyDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> !this.isKeyboardSelectingAll() && CharacterInputKeyBinding.Forward.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadAxis2dBinding.Movement.getValueY(this.getJoypadBind()) > 0.0F;
            default -> false;
        };
    }

    public boolean isBackwardKeyDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> !this.isKeyboardSelectingAll() && CharacterInputKeyBinding.Backward.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadAxis2dBinding.Movement.getValueY(this.getJoypadBind()) < 0.0F;
            default -> false;
        };
    }

    public boolean isLeftKeyDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> !this.isKeyboardSelectingAll() && CharacterInputKeyBinding.Left.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadAxis2dBinding.Movement.getValueX(this.getJoypadBind()) < 0.0F;
            default -> false;
        };
    }

    public boolean isRightKeyDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> !this.isKeyboardSelectingAll() && CharacterInputKeyBinding.Right.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadAxis2dBinding.Movement.getValueX(this.getJoypadBind()) > 0.0F;
            default -> false;
        };
    }

    public boolean isKeyboardSelectingAll() {
        return Core.getInstance().isSelectingAll()
            && UIManager.isMouseOverInventory()
            && this.isCtrlKeyDown()
            && (
                CharacterInputKeyBinding.Left.isKeyDown(30)
                    || CharacterInputKeyBinding.Right.isKeyDown(30)
                    || CharacterInputKeyBinding.Forward.isKeyDown(30)
                    || CharacterInputKeyBinding.Backward.isKeyDown(30)
            );
    }

    public boolean isAimKeyDown() {
        return this.isForceAim() || this.isAimKeyDownInternal();
    }

    public boolean isPrecisionAimKeyDown() {
        return this.isForceAim() || this.isPrecisionAimKeyDownInternal();
    }

    public boolean isToggleAimKeyDown() {
        if (CharacterInputComponent.Options.isToggleToAim()) {
            switch (this.getInputMode()) {
                case KEYBOARD:
                    if (this.isAimKeyDownInternal()) {
                        return true;
                    }
                case GAMEPAD:
                case NONE:
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        return false;
    }

    public boolean isToggleAimKeyMouse() {
        if (CharacterInputComponent.Options.isToggleToAim()) {
            switch (this.getInputMode()) {
                case KEYBOARD:
                    if (CharacterInputKeyBinding.Aim.isMouseKey()) {
                        return true;
                    }
                case GAMEPAD:
                case NONE:
                    break;
                default:
                    throw new MatchException(null, null);
            }
        }

        return false;
    }

    public boolean isToggleSprintButtonDown() {
        return CharacterInputComponent.Options.isToggleToSprint() && this.isSprintButtonDown();
    }

    public boolean isToggleRunButtonDown() {
        return CharacterInputComponent.Options.isToggleToRun() && this.isRunButtonDown();
    }

    public boolean isToggleCrouchButtonDown() {
        return this.isCrouchButtonDown();
    }

    public boolean isAimKeyDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Aim.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadAimingAxisApplied() && CharacterJoypadButtonBinding.Aim.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isPrecisionAimKeyDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> this.isAimKeyDownInternal();
            case GAMEPAD -> !this.isJoypadIgnoreAim()
                && !this.isJoypadIgnoreAimUntilCentered()
                && CharacterJoypadButtonBinding.PrecisionAim.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isAnyAimKeyDown() {
        return this.isAimKeyDown() || this.isPrecisionAimKeyDown();
    }

    public boolean isMeleeButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> this.melee.isKeyDown();
            case GAMEPAD -> this.melee.isKeyDown() && !this.melee.isKeyPressed();
            default -> false;
        };
    }

    public boolean isMeleeButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Melee.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadButtonBinding.Melee.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isAttackButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Attack.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadButtonBinding.Attack.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isBuildButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Attack.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadButtonBinding.Interact.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isBuildButtonReleased() {
        return this.build.isKeyReleased();
    }

    public boolean isRunButtonDown() {
        if (this.isAllowRun()) {
            switch (this.getInputMode()) {
                case KEYBOARD:
                    if (CharacterInputKeyBinding.Run.isKeyDown()) {
                        return true;
                    }
                    break;
                case GAMEPAD:
                    if (this.isJoypadMovementAxisApplied() && CharacterJoypadButtonBinding.Run.isDown(this.getJoypadBind())) {
                        return true;
                    }
            }
        }

        return false;
    }

    public boolean wasRunButtonDown() {
        return CharacterInputKeyBinding.Run.wasKeyDown();
    }

    public boolean isInteractButtonPressed() {
        return this.interact.isKeyPressed();
    }

    public boolean isInteractButtonDown() {
        return this.interact.isKeyDown();
    }

    public boolean isInteractButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Interact.isKeyDownInWorld();
            case GAMEPAD -> CharacterJoypadButtonBinding.Interact.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isInteractButtonClicked() {
        return this.interact.isKeyClicked();
    }

    public boolean isWalkToButtonDown() {
        return CharacterInputKeyBinding.WalkTo.isKeyDownInWorld();
    }

    public boolean isCrouchButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.Crouch.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.Crouch.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isCrouchButtonPressed() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> this.toggleCrouch.isKeyPressed();
            case GAMEPAD -> this.isJoypadButtonsActive() && this.toggleCrouch.isKeyClicked();
            default -> false;
        };
    }

    public boolean isReloadWeaponButtonPressed() {
        return this.reloadWeapon.isKeyPressed();
    }

    public boolean isRackFirearmButtonPressed() {
        return this.rackFirearm.isKeyPressed();
    }

    private boolean isReloadWeaponButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.ReloadWeapon.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.ReloadWeapon.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    private boolean isRackFirearmButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.RackFirearm.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.RackFirearm.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isSprintButtonDown() {
        if (this.isAllowSprint()) {
            switch (this.getInputMode()) {
                case KEYBOARD:
                    if (CharacterInputKeyBinding.Sprint.isKeyDown()) {
                        return true;
                    }
                    break;
                case GAMEPAD:
                    if (this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.Sprint.isDown(this.getJoypadBind())) {
                        return true;
                    }
            }
        }

        return false;
    }

    public boolean wasSprintButtonDown() {
        return CharacterInputKeyBinding.Sprint.wasKeyDown();
    }

    public boolean isCancelActionButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.CancelAction.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.CancelAction.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isManualFloorAtkButtonDown() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.ManualFloorAtk.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.ManualFloorAtk.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    private boolean isZoomInButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.ZoomIn.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.ZoomIn.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    private boolean isZoomOutButtonDownInternal() {
        return switch (this.getInputMode()) {
            case KEYBOARD -> CharacterInputKeyBinding.ZoomOut.isKeyDownInWorld();
            case GAMEPAD -> this.isJoypadButtonsActive() && CharacterJoypadButtonBinding.ZoomOut.isDown(this.getJoypadBind());
            default -> false;
        };
    }

    public boolean isZoomInButtonPressed() {
        return this.zoomIn.isKeyPressed();
    }

    public boolean isZoomOutButtonPressed() {
        return this.zoomOut.isKeyPressed();
    }

    public boolean isLShiftKeyDown() {
        return GameKeyboard.isKeyDown(42);
    }

    public boolean isRShiftKeyDown() {
        return GameKeyboard.isKeyDown(54);
    }

    public boolean isShiftKeyDown() {
        return this.isLShiftKeyDown() || this.isRShiftKeyDown();
    }

    public boolean isLCtrlKeyDown() {
        return GameKeyboard.isKeyDown(29);
    }

    public boolean isRCtrlKeyDown() {
        return GameKeyboard.isKeyDown(157);
    }

    public boolean isCtrlKeyDown() {
        return this.isLCtrlKeyDown() || this.isRCtrlKeyDown();
    }

    public boolean isF12KeyDown() {
        return GameKeyboard.isKeyDown(88);
    }

    public boolean isChangeCharacterKeyDown() {
        return GameKeyboard.isKeyDown(22);
    }

    public void checkJoypadIgnoreAimUntilCentered() {
        if (!this.isJoypadIgnoreAim()) {
            this.setJoypadIgnoreAimUntilCentered(this.isJoypadIgnoreAimUntilCentered() && this.isJoypadAimingAxisAppliedInternal());
        }
    }

    public boolean isJoypadMovementAxisApplied() {
        return !this.isIgnoreInputsForDirection() && this.isJoypadMovementAxisAppliedInternal();
    }

    public boolean isJoypadAimingAxisApplied() {
        return !this.isJoypadIgnoreAim() && !this.isJoypadIgnoreAimUntilCentered() && this.isJoypadAimingAxisAppliedInternal();
    }

    private boolean isJoypadAimingAxisAppliedInternal() {
        return this.getInputMode() == CharacterInputMode.GAMEPAD && CharacterJoypadAxis2dBinding.Aiming.isApplied(this.getJoypadBind());
    }

    private boolean isJoypadMovementAxisAppliedInternal() {
        return this.getInputMode() == CharacterInputMode.GAMEPAD && CharacterJoypadAxis2dBinding.Movement.isApplied(this.getJoypadBind());
    }

    public boolean isJoypadButtonsActive() {
        return this.joypadButtonsActive;
    }

    public void setJoypadButtonsActive(boolean joypadMovementActive) {
        this.joypadButtonsActive = joypadMovementActive;
    }

    public boolean isIgnoreInputsForDirection() {
        return this.ignoreInputsForDirection;
    }

    public void setIgnoreInputsForDirection(boolean ignoreInputsForDirection) {
        this.ignoreInputsForDirection = ignoreInputsForDirection;
    }

    public void setJoypadIgnoreAim(boolean ignore) {
        this.joypadIgnoreAim = ignore;
    }

    public boolean isJoypadIgnoreAim() {
        return this.joypadIgnoreAim;
    }

    public void setJoypadIgnoreAimUntilCentered(boolean ignore) {
        this.joypadIgnoreAimUntilCentered = ignore;
    }

    public boolean isJoypadIgnoreAimUntilCentered() {
        return this.joypadIgnoreAimUntilCentered;
    }

    public void setIgnoreAimingInput(boolean b) {
        this.ignoreAimingInput = b;
    }

    public boolean isIgnoringAimingInput() {
        return this.ignoreAimingInput;
    }

    public boolean isAllowSprint() {
        return this.allowSprint;
    }

    public void setAllowSprint(boolean allowSprint) {
        this.allowSprint = allowSprint;
    }

    public boolean isAllowRun() {
        return this.allowRun;
    }

    public void setAllowRun(boolean allowRun) {
        this.allowRun = allowRun;
    }

    @Override
    public void onGameLoadingStateEnter() {
        this.setJoypadButtonsActive(false);
        this.setIgnoreAimingInput(true);
        this.setIgnoreInputsForDirection(true);
        this.melee.mute();
        this.toggleAim.mute();
        this.toggleSprint.mute();
        this.toggleRun.mute();
        this.toggleCrouch.mute();
        this.interact.mute();
        this.reloadWeapon.mute();
        this.rackFirearm.mute();
        this.zoomIn.mute();
        this.zoomOut.mute();
    }

    @Override
    public void onInGameStateEnter() {
        this.setJoypadButtonsActive(true);
        this.setIgnoreAimingInput(false);
        this.setIgnoreInputsForDirection(false);
    }

    @Override
    public void logVariablesToRecording(AnimationPlayerRecorder animationRecorder) {
        animationRecorder.logVariable("input.inputMode", this.getInputMode().toString());
        animationRecorder.logVariable("input.force.isForceAim", this.isForceAim());
        animationRecorder.logVariable("input.force.isForceSprint", this.isForceSprint());
        animationRecorder.logVariable("input.force.isForceRun", this.isForceRun());
        animationRecorder.logVariable("input.isJoypadControllerActive", this.isJoypadControllerActive());
        animationRecorder.logVariable("input.keys.isForwardKeyDown", this.isForwardKeyDown());
        animationRecorder.logVariable("input.keys.isBackwardKeyDown", this.isBackwardKeyDown());
        animationRecorder.logVariable("input.keys.isLeftKeyDown", this.isLeftKeyDown());
        animationRecorder.logVariable("input.keys.isRightKeyDown", this.isRightKeyDown());
        animationRecorder.logVariable("input.keys.isAimKeyDown", this.isAimKeyDown());
        animationRecorder.logVariable("input.keys.isPrecisionAimKeyDown", this.isPrecisionAimKeyDown());
        animationRecorder.logVariable("input.toggle.isToggleAimKeyDown", this.isToggleAimKeyDown());
        animationRecorder.logVariable("input.toggle.isToggleSprintButtonDown", this.isToggleSprintButtonDown());
        animationRecorder.logVariable("input.toggle.isToggleRunButtonDown", this.isToggleRunButtonDown());
        animationRecorder.logVariable("input.keys.isAnyAimKeyDown", this.isAnyAimKeyDown());
        animationRecorder.logVariable("input.button.isMeleeButtonDown", this.isMeleeButtonDown());
        animationRecorder.logVariable("input.button.isAttackButtonDown", this.isAttackButtonDown());
        animationRecorder.logVariable("input.button.isRunButtonDown", this.isRunButtonDown());
        animationRecorder.logVariable("input.button.wasRunButtonDown", this.wasRunButtonDown());
        animationRecorder.logVariable("input.button.isInteractButtonPressed", this.isInteractButtonPressed());
        animationRecorder.logVariable("input.button.isInteractButtonDown", this.isInteractButtonDown());
        animationRecorder.logVariable("input.button.isInteractButtonClicked", this.isInteractButtonClicked());
        animationRecorder.logVariable("input.button.isWalkToButtonDown", this.isWalkToButtonDown());
        animationRecorder.logVariable("input.button.isCrouchButtonDown", this.isCrouchButtonDown());
        animationRecorder.logVariable("input.button.isReloadWeaponButtonDown", this.reloadWeapon.isKeyDown());
        animationRecorder.logVariable("input.button.isRackFirearmButtonDown", this.rackFirearm.isKeyDown());
        animationRecorder.logVariable("input.button.isSprintButtonDown", this.isSprintButtonDown());
        animationRecorder.logVariable("input.button.wasSprintButtonDown", this.wasSprintButtonDown());
        animationRecorder.logVariable("input.button.isCancelActionButtonDown", this.isCancelActionButtonDown());
        animationRecorder.logVariable("input.button.isManualFloorAtkButtonDown", this.isManualFloorAtkButtonDown());
        animationRecorder.logVariable("input.keys.isLShiftKeyDown", this.isLShiftKeyDown());
        animationRecorder.logVariable("input.keys.isRShiftKeyDown", this.isRShiftKeyDown());
        animationRecorder.logVariable("input.keys.isShiftKeyDown", this.isShiftKeyDown());
        animationRecorder.logVariable("input.keys.isF12KeyDown", this.isF12KeyDown());
        animationRecorder.logVariable("input.keys.isChangeCharacterKeyDown", this.isChangeCharacterKeyDown());
        animationRecorder.logVariable("input.axis.isJoypadMovementAxisApplied", this.isJoypadMovementAxisApplied());
        animationRecorder.logVariable("input.axis.isJoypadAimingAxisApplied", this.isJoypadAimingAxisApplied());
        animationRecorder.logVariable("input.active.isJoypadButtonsActive", this.isJoypadButtonsActive());
        animationRecorder.logVariable("input.active.isIgnoreInputsForDirection", this.isIgnoreInputsForDirection());
        animationRecorder.logVariable("input.active.isJoypadIgnoreAim", this.isJoypadIgnoreAim());
        animationRecorder.logVariable("input.active.isJoypadIgnoreAimUntilCentered", this.isJoypadIgnoreAimUntilCentered());
        animationRecorder.logVariable("input.axis.joypadMoveVector", this.getInputMoveVector(joypadMoveVector));
        animationRecorder.logVariable("input.axis.joypadAimVector", this.getJoypadAimVector(joypadAimVector));
    }

    public static class Options {
        public static boolean isToggleToAim() {
            return Core.getInstance().isToggleToAim();
        }

        public static boolean isToggleToSprint() {
            return Core.getInstance().isToggleToSprint();
        }

        public static boolean isToggleToRun() {
            return Core.getInstance().isToggleToRun();
        }
    }
}
