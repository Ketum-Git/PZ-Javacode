// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.util.Type;

public final class CollideWithWallState extends State {
    private static final CollideWithWallState _instance = new CollideWithWallState();
    private static final Integer PARAM_COLLIDE_TYPE = 0;

    public static CollideWithWallState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        if (owner instanceof IsoPlayer) {
            owner.setIsAiming(false);
        }

        if (owner.isCollidedN()) {
            owner.setDir(IsoDirections.N);
        }

        if (owner.isCollidedS()) {
            owner.setDir(IsoDirections.S);
        }

        if (owner.isCollidedE()) {
            owner.setDir(IsoDirections.E);
        }

        if (owner.isCollidedW()) {
            owner.setDir(IsoDirections.W);
        }

        owner.setCollideType("wall");
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        owner.setLastCollideTime(70.0F);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("PlayerVoiceSound");
        owner.setCollideType(null);
        owner.setIgnoreMovement(false);
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if ("PlayCollideSound".equalsIgnoreCase(event.eventName)) {
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            ParameterCharacterMovementSpeed parameter = ((IsoPlayer)owner).getParameterCharacterMovementSpeed();
            owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), ParameterCharacterMovementSpeed.MovementType.Sprint.label);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), 7.0F);
        }

        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound")) {
            if (owner.getVariableBoolean("PlayerVoiceSound")) {
                return;
            }

            if (player == null) {
                return;
            }

            owner.setVariable("PlayerVoiceSound", true);
            player.playerVoiceSound(event.parameterValue);
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_COLLIDE_TYPE, owner.getCollideType());
        } else {
            owner.setCollideType((String)StateMachineParams.getOrDefault(PARAM_COLLIDE_TYPE, "wall"));
        }

        super.setParams(owner, stage);
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
        return false;
    }
}
