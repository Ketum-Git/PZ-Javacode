// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public final class SmashWindowState extends State {
    private static final SmashWindowState _instance = new SmashWindowState();
    private static final Integer PARAM_SCRATCHED = 0;

    public static SmashWindowState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setVariable("bSmashWindow", true);
        HandWeapon weapon = Type.tryCastTo(owner.getPrimaryHandItem(), HandWeapon.class);
        if (weapon != null && weapon.isRanged()) {
            owner.playSound("AttackShove");
        } else if (weapon != null && !StringUtils.isNullOrWhitespace(weapon.getSwingSound())) {
            owner.playSound(weapon.getSwingSound());
        }

        if (GameClient.client) {
            boolean scratch = !(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon);
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            StateMachineParams.put(PARAM_SCRATCHED, scratch ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (!(StateMachineParams.get(0) instanceof IsoWindow) && !(StateMachineParams.get(0) instanceof VehicleWindow)) {
            owner.setVariable("bSmashWindow", false);
        } else {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (player.pressedMovement(false) || player.pressedCancelAction()) {
                owner.setVariable("bSmashWindow", false);
            } else if (owner.getVariableBoolean("bSmashWindow")) {
                if (StateMachineParams.get(0) instanceof IsoWindow window) {
                    if (window.getObjectIndex() == -1 || window.isDestroyed() && !owner.getVariableBoolean("OwnerSmashedIt")) {
                        owner.setVariable("bSmashWindow", false);
                        return;
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
                } else if (StateMachineParams.get(0) instanceof VehicleWindow window) {
                    BaseVehicle vehicle = (BaseVehicle)StateMachineParams.get(1);
                    owner.faceThisObject(vehicle);
                    if (window.isDestroyed() && !owner.getVariableBoolean("OwnerSmashedIt")) {
                        owner.setVariable("bSmashWindow", false);
                    }
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.clearVariable("bSmashWindow");
        owner.clearVariable("OwnerSmashedIt");
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (StateMachineParams.get(0) instanceof IsoWindow window) {
            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
                owner.setVariable("OwnerSmashedIt", true);
                IsoPlayer.getInstance().contextPanic = 0.0F;
                if (!GameClient.client) {
                    window.WeaponHit(owner, null);
                    if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
                        owner.getBodyDamage().setScratchedWindow();
                        IsoPlayer player = (IsoPlayer)owner;
                        player.playerVoiceSound("PainFromGlassCut");
                    }
                }
            } else if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
                owner.setVariable("bSmashWindow", false);
                if (Boolean.TRUE == StateMachineParams.get(3)) {
                    owner.climbThroughWindow(window);
                }
            }
        } else if (StateMachineParams.get(0) instanceof VehicleWindow windowx) {
            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
                owner.setVariable("OwnerSmashedIt", true);
                IsoPlayer.getInstance().contextPanic = 0.0F;
                if (((IsoPlayer)owner).isLocalPlayer() && !GameClient.client) {
                    windowx.hit(owner);
                    if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
                        owner.getBodyDamage().setScratchedWindow();
                        IsoPlayer player = (IsoPlayer)owner;
                        player.playerVoiceSound("PainFromGlassCut");
                    }
                }
            } else if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
                owner.setVariable("bSmashWindow", false);
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
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        boolean scratched = (Boolean)delegate.getOrDefault(PARAM_SCRATCHED, false);
        if (scratched) {
            owner.getBodyDamage().setScratchedWindow();
        }
    }
}
