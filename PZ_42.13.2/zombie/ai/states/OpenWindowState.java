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
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWindow;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public final class OpenWindowState extends State {
    private static final OpenWindowState _instance = new OpenWindowState();
    private static final Integer PARAM_WINDOW = 1;

    public static OpenWindowState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoWindow window = (IsoWindow)StateMachineParams.get(PARAM_WINDOW);
        if (Core.debug
            && DebugOptions.instance.cheat.window.unlock.getValue()
            && window.getSprite() != null
            && !window.getSprite().getProperties().has("WindowLocked")) {
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

        owner.setVariable("bOpenWindow", true);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.getVariableBoolean("bOpenWindow")) {
            IsoPlayer player = (IsoPlayer)owner;
            if (!player.pressedMovement(false) && !player.pressedCancelAction()) {
                IsoWindow window = (IsoWindow)StateMachineParams.get(PARAM_WINDOW);
                if (window == null || window.getObjectIndex() == -1) {
                    owner.setVariable("bOpenWindow", false);
                } else if (player.contextPanic > 5.0F) {
                    player.contextPanic = 0.0F;
                    owner.setVariable("bOpenWindow", false);
                    owner.smashWindow(window);
                    owner.getStateMachineParams(SmashWindowState.instance()).put(3, Boolean.TRUE);
                } else {
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

                    if (Core.tutorial) {
                        if (owner.getX() != window.getX() + 0.5F && window.isNorth()) {
                            this.slideX(owner, window.getX() + 0.5F);
                        }

                        if (owner.getY() != window.getY() + 0.5F && !window.isNorth()) {
                            this.slideY(owner, window.getY() + 0.5F);
                        }
                    }
                }
            } else {
                owner.setVariable("bOpenWindow", false);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.clearVariable("bOpenWindow");
        owner.clearVariable("OpenWindowOutcome");
        owner.clearVariable("StopAfterAnimLooped");
        owner.setHideWeaponModel(false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.getVariableBoolean("bOpenWindow")) {
            IsoWindow window = (IsoWindow)StateMachineParams.get(PARAM_WINDOW);
            if (window == null) {
                owner.setVariable("bOpenWindow", false);
            } else {
                if (event.eventName.equalsIgnoreCase("WindowAnimLooped")) {
                    if ("start".equalsIgnoreCase(event.parameterValue)) {
                        if (window.isPermaLocked() || window.isLocked() && owner.getCurrentSquare().has(IsoFlagType.exterior)) {
                            owner.setVariable("OpenWindowOutcome", "struggle");
                        } else {
                            owner.setVariable("OpenWindowOutcome", "success");
                        }

                        return;
                    }

                    if (event.parameterValue.equalsIgnoreCase(owner.getVariableString("StopAfterAnimLooped"))) {
                        owner.setVariable("bOpenWindow", false);
                    }
                }

                if (event.eventName.equalsIgnoreCase("WindowOpenAttempt")) {
                    this.onAttemptFinished(owner, window);
                } else if (event.eventName.equalsIgnoreCase("WindowOpenSuccess")) {
                    this.onSuccess(owner, window);
                } else if (event.eventName.equalsIgnoreCase("WindowStruggleSound") && "struggle".equals(owner.getVariableString("OpenWindowOutcome"))) {
                    owner.playSound("WindowIsLocked");
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        this.exert(owner);
        if (window.isPermaLocked()) {
            if (!owner.getEmitter().isPlaying("WindowIsLocked")) {
            }

            owner.setVariable("OpenWindowOutcome", "fail");
            owner.setVariable("StopAfterAnimLooped", "fail");
        } else {
            int basePermaLockChance = 10;
            if (owner.hasTrait(CharacterTrait.BURGLAR)) {
                basePermaLockChance = 5;
            }

            if (window.isLocked() && owner.getCurrentSquare().has(IsoFlagType.exterior)) {
                if (Rand.Next(100) < basePermaLockChance) {
                    owner.getEmitter().playSound("BreakLockOnWindow", window);
                    window.setPermaLocked(true);
                    window.syncIsoObject(false, (byte)0, null, null);
                    StateMachineParams.put(PARAM_WINDOW, null);
                    owner.setVariable("OpenWindowOutcome", "fail");
                    owner.setVariable("StopAfterAnimLooped", "fail");
                    return;
                }

                boolean bSuccess = false;
                if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 7 && Rand.Next(100) < 20) {
                    bSuccess = true;
                } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 5 && Rand.Next(100) < 10) {
                    bSuccess = true;
                } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 3 && Rand.Next(100) < 6) {
                    bSuccess = true;
                } else if (owner.getPerkLevel(PerkFactory.Perks.Strength) > 1 && Rand.Next(100) < 4) {
                    bSuccess = true;
                } else if (Rand.Next(100) <= 1) {
                    bSuccess = true;
                }

                if (bSuccess) {
                    owner.setVariable("OpenWindowOutcome", "success");
                }
            } else {
                owner.setVariable("OpenWindowOutcome", "success");
            }
        }
    }

    private void onSuccess(IsoGameCharacter owner, IsoWindow window) {
        owner.setVariable("StopAfterAnimLooped", "success");
        ((IsoPlayer)owner).contextPanic = 0.0F;
        if (window.getObjectIndex() != -1 && !window.IsOpen() && ((IsoPlayer)owner).isLocalPlayer()) {
            window.ToggleWindow(owner);
        }
    }

    private void exert(IsoGameCharacter owner) {
        switch (owner.getPerkLevel(PerkFactory.Perks.Fitness)) {
            case 0:
                owner.exert(0.011F);
                break;
            case 1:
                owner.exert(0.01F);
                break;
            case 2:
                owner.exert(0.009F);
                break;
            case 3:
                owner.exert(0.008F);
                break;
            case 4:
                owner.exert(0.007F);
                break;
            case 5:
                owner.exert(0.006F);
                break;
            case 6:
                owner.exert(0.005F);
                break;
            case 7:
                owner.exert(0.004F);
                break;
            case 8:
                owner.exert(0.003F);
                break;
            case 9:
                owner.exert(0.0025F);
                break;
            case 10:
                owner.exert(0.002F);
                break;
            default:
                owner.exert(0.012F);
        }
    }

    private void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    private void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    public void setParams(IsoGameCharacter owner, IsoWindow window) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.clear();
        StateMachineParams.put(PARAM_WINDOW, window);
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
