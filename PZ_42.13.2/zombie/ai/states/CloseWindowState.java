// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class CloseWindowState extends State {
    private static final CloseWindowState _instance = new CloseWindowState();

    public static CloseWindowState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        IsoWindow window = (IsoWindow)StateMachineParams.get(0);
        if (Core.debug && DebugOptions.instance.cheat.window.unlock.getValue()) {
            window.setIsLocked(false);
            window.setPermaLocked(false);
        }

        if (window.isNorth()) {
            if (window.getSquare().getY() < owner.getY()) {
                owner.setDir(IsoDirections.N);
            } else {
                owner.setDir(IsoDirections.S);
            }
        } else if (window.getSquare().getX() < owner.getX()) {
            owner.setDir(IsoDirections.W);
        } else {
            owner.setDir(IsoDirections.E);
        }

        owner.setVariable("bCloseWindow", true);
        owner.clearVariable("BlockWindow");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.getVariableBoolean("bCloseWindow")) {
            IsoPlayer player = (IsoPlayer)owner;
            if (!player.pressedMovement(false) && !player.pressedCancelAction()) {
                if (StateMachineParams.get(0) instanceof IsoWindow window) {
                    if (window != null && window.getObjectIndex() != -1) {
                        player.setCollidable(true);
                        player.updateLOS();
                        if (window.isNorth()) {
                            if (window.getSquare().getY() < owner.getY()) {
                                owner.setDir(IsoDirections.N);
                            } else {
                                owner.setDir(IsoDirections.S);
                            }
                        } else if (window.getSquare().getX() < owner.getX()) {
                            owner.setDir(IsoDirections.W);
                        } else {
                            owner.setDir(IsoDirections.E);
                        }
                    } else {
                        owner.setVariable("bCloseWindow", false);
                    }
                } else {
                    owner.setVariable("bCloseWindow", false);
                }
            } else {
                owner.setVariable("bCloseWindow", false);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("BlockWindow");
        owner.clearVariable("bCloseWindow");
        owner.clearVariable("CloseWindowOutcome");
        owner.clearVariable("StopAfterAnimLooped");
        owner.setIgnoreMovement(false);
        owner.setHideWeaponModel(false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.getVariableBoolean("bCloseWindow")) {
            if (!(StateMachineParams.get(0) instanceof IsoWindow window)) {
                owner.setVariable("bCloseWindow", false);
            } else {
                if (event.eventName.equalsIgnoreCase("WindowAnimLooped")) {
                    if ("start".equalsIgnoreCase(event.parameterValue)) {
                        int randStruggle = Math.max(5 - owner.getMoodles().getMoodleLevel(MoodleType.PANIC), 1);
                        if (!window.isPermaLocked() && window.getFirstCharacterClimbingThrough() == null) {
                            owner.setVariable("CloseWindowOutcome", "success");
                        } else {
                            owner.setVariable("CloseWindowOutcome", "struggle");
                        }

                        return;
                    }

                    if (event.parameterValue.equalsIgnoreCase(owner.getVariableString("StopAfterAnimLooped"))) {
                        owner.setVariable("bCloseWindow", false);
                    }
                }

                if (event.eventName.equalsIgnoreCase("WindowCloseAttempt")) {
                    this.onAttemptFinished(owner, window);
                } else if (event.eventName.equalsIgnoreCase("WindowCloseSuccess")) {
                    this.onSuccess(owner, window);
                }
            }
        }
    }

    /**
     * @return TRUE if this state handles the "Cancel Action" key or the B controller button.
     */
    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return true;
    }

    private void onAttemptFinished(IsoGameCharacter owner, IsoWindow window) {
        this.exert(owner);
        if (window.isPermaLocked()) {
            owner.getEmitter().playSound("WindowIsLocked", window);
            owner.setVariable("CloseWindowOutcome", "fail");
            owner.setVariable("StopAfterAnimLooped", "fail");
        } else {
            int randStruggle = Math.max(5 - owner.getMoodles().getMoodleLevel(MoodleType.PANIC), 3);
            if (!window.isPermaLocked() && window.getFirstCharacterClimbingThrough() == null) {
                owner.setVariable("CloseWindowOutcome", "success");
            } else {
                owner.setVariable("CloseWindowOutcome", "struggle");
            }
        }
    }

    private void onSuccess(IsoGameCharacter owner, IsoWindow window) {
        owner.setVariable("StopAfterAnimLooped", "success");
        ((IsoPlayer)owner).contextPanic = 0.0F;
        if (window.getObjectIndex() != -1 && window.IsOpen() && ((IsoPlayer)owner).isLocalPlayer()) {
            window.ToggleWindow(owner);
        }
    }

    private void exert(IsoGameCharacter owner) {
        float delta = GameTime.getInstance().getThirtyFPSMultiplier();
        switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
            case 1:
                owner.exert(0.01F * delta);
                break;
            case 2:
                owner.exert(0.009F * delta);
                break;
            case 3:
                owner.exert(0.008F * delta);
                break;
            case 4:
                owner.exert(0.007F * delta);
                break;
            case 5:
                owner.exert(0.006F * delta);
                break;
            case 6:
                owner.exert(0.005F * delta);
                break;
            case 7:
                owner.exert(0.004F * delta);
                break;
            case 8:
                owner.exert(0.003F * delta);
                break;
            case 9:
                owner.exert(0.0025F * delta);
                break;
            case 10:
                owner.exert(0.002F * delta);
        }
    }

    public IsoWindow getWindow(IsoGameCharacter owner) {
        if (!owner.isCurrentState(this)) {
            return null;
        } else {
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            return (IsoWindow)StateMachineParams.get(0);
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return true;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }
}
