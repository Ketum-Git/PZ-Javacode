// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.AttackType;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.util.StringUtils;

public final class PlayerOnBedState extends State {
    private static final PlayerOnBedState _instance = new PlayerOnBedState();

    public static PlayerOnBedState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setIgnoreMovement(true);
        owner.setCollidable(false);
        owner.setOnBed(true);
        if (!(owner.getPrimaryHandItem() instanceof HandWeapon) && !(owner.getSecondaryHandItem() instanceof HandWeapon)) {
            owner.setHideWeaponModel(true);
        }

        if (owner.getStateMachine().getPrevious() == IdleState.instance()) {
            owner.clearVariable("forceGetUp");
            owner.clearVariable("OnBedAnim");
            owner.clearVariable("OnBedStarted");
        }

        IsoDirections dir = IsoDirections.fromAngle(owner.getAnimAngleRadians());
        switch (dir) {
            case N:
                owner.setY((int)owner.getY() + 0.3F);
                break;
            case S:
                owner.setY((int)owner.getY() + 0.7F);
                break;
            case W:
                owner.setX((int)owner.getX() + 0.3F);
                break;
            case E:
                owner.setX((int)owner.getX() + 0.7F);
        }

        owner.blockTurning = true;
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        }

        if (owner.getVariableBoolean("OnBedStarted")) {
        }

        player.setInitiateAttack(false);
        player.setAttackStarted(false);
        player.setAttackType(AttackType.NONE);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        if (StringUtils.isNullOrEmpty(owner.getVariableString("HitReaction"))) {
            owner.clearVariable("forceGetUp");
            owner.clearVariable("OnBedAnim");
            owner.clearVariable("OnBedStarted");
            owner.setIgnoreMovement(false);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("OnBedStarted")) {
            owner.setVariable("OnBedStarted", true);
            owner.setVariable("OnBedAnim", "Awake");
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }
}
