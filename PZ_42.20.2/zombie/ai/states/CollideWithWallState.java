// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterTripObstacleType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.util.Type;

public final class CollideWithWallState extends State {
    private static final CollideWithWallState INSTANCE = new CollideWithWallState();
    public static final State.Param<String> COLLIDE_TYPE = State.Param.ofString("collide_type", "wall");

    public static CollideWithWallState instance() {
        return INSTANCE;
    }

    private CollideWithWallState() {
        super(true, false, true, false);
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
            owner.getEmitter().setParameterValueByName(instance, "TripObstacleType", ParameterTripObstacleType.ObstacleType.COLLIDE_WITH_WALL.getValue());
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
        if (owner.isLocal()) {
            owner.set(COLLIDE_TYPE, owner.getCollideType());
        } else {
            owner.setCollideType(owner.get(COLLIDE_TYPE));
        }

        super.setParams(owner, stage);
    }
}
