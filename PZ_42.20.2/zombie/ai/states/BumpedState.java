// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import zombie.AttackType;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.audio.parameters.ParameterTripObstacleType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.Type;

public final class BumpedState extends State {
    private static final BumpedState INSTANCE = new BumpedState();
    public static final State.Param<String> BUMP_TYPE = State.Param.ofString("bump_type", "");
    public static final State.Param<String> BUMP_FALL_TYPE = State.Param.ofString("bump_fall_type", "");
    public static final State.Param<Boolean> BUMP_FALL = State.Param.ofBool("bump_fall", false);
    public static final State.Param<IsoGameCharacter> CHARACTER_BUMP = State.Param.of("character_bump", IsoGameCharacter.class);
    public static final State.Param<String> CHARACTER_BUMP_TYPE = State.Param.ofString("character_bump_type", "");
    public static final State.Param<Boolean> CHARACTER_BUMP_BEHIND = State.Param.ofBool("character_bump_behind", false);

    public static BumpedState instance() {
        return INSTANCE;
    }

    private BumpedState() {
        super(true, true, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setBumpDone(false);
        owner.setVariable("BumpFallAnimFinished", false);
        owner.getAnimationPlayer().setTargetToAngle();
        owner.setForwardDirectionFromAnimAngle();
        this.setCharacterBlockMovement(owner, true);
        if (owner.getVariableBoolean("BumpFall")) {
            long instance = owner.getEmitter().playSoundImpl("TripOverObstacle", null);
            ParameterCharacterMovementSpeed parameter = ((IsoPlayer)owner).getParameterCharacterMovementSpeed();
            owner.getEmitter().setParameterValue(instance, parameter.getParameterDescription(), parameter.calculateCurrentValue());
            String tripStr = owner.getVariableString("TripObstacleType");
            if (tripStr == null) {
                tripStr = "zombie";
            }

            owner.clearVariable("TripObstacleType");
            byte var8 = -1;
            switch (tripStr.hashCode()) {
                case 3568542:
                    if (tripStr.equals("tree")) {
                        var8 = 0;
                    }
                default:
                    ParameterTripObstacleType.ObstacleType tripType = switch (var8) {
                        case 0 -> ParameterTripObstacleType.ObstacleType.TREE;
                        default -> ParameterTripObstacleType.ObstacleType.ZOMBIE;
                    };
                    owner.getEmitter().setParameterValueByName(instance, "TripObstacleType", tripType.getValue());
            }
        }

        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        boolean blockMovement = owner.isBumpFall() || owner.isBumpStaggered();
        this.setCharacterBlockMovement(owner, blockMovement);
    }

    private void setCharacterBlockMovement(IsoGameCharacter owner, boolean blockMovement) {
        if (owner instanceof IsoPlayer player) {
            player.setBlockMovement(blockMovement);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("BumpFallType");
        owner.clearVariable("BumpFallAnimFinished");
        owner.clearVariable("BumpAnimFinished");
        owner.clearVariable("PlayerVoiceSound");
        owner.setBumpType("");
        owner.setBumpedChr(null);
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (player != null) {
            player.setInitiateAttack(false);
            player.setAttackStarted(false);
            player.setAttackType(AttackType.NONE);
        }

        if (player != null && owner.isBumpFall()) {
            owner.fallenOnKnees();
        }

        owner.setOnFloor(false);
        owner.setBumpFall(false);
        this.setCharacterBlockMovement(owner, false);
        if (owner instanceof IsoZombie isoZombie && isoZombie.target != null) {
            owner.pathToLocation(
                PZMath.fastfloor(isoZombie.target.getX()), PZMath.fastfloor(isoZombie.target.getY()), PZMath.fastfloor(isoZombie.target.getZ())
            );
        }

        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
            owner.setOnFloor(owner.isFallOnFront());
        }

        if (event.eventName.equalsIgnoreCase("FallOnBack")) {
            owner.setOnFloor(Boolean.parseBoolean(event.parameterValue));
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
        IsoGameCharacter character = owner.getBumpedChr();
        if (owner.isLocal()) {
            if (State.Stage.Enter == stage) {
                owner.set(BUMP_TYPE, owner.getBumpType());
                owner.set(BUMP_FALL_TYPE, owner.getBumpFallType());
                owner.set(BUMP_FALL, owner.isBumpFall());
            }

            if (character != null) {
                owner.set(CHARACTER_BUMP, character);
                owner.set(CHARACTER_BUMP_TYPE, character.getBumpType());
                owner.set(CHARACTER_BUMP_BEHIND, character.isHitFromBehind());
            } else {
                owner.remove(CHARACTER_BUMP);
                owner.remove(CHARACTER_BUMP_TYPE);
                owner.remove(CHARACTER_BUMP_BEHIND);
            }
        } else if (State.Stage.Exit == stage) {
            owner.setBumpType("");
            owner.setBumpFallType(owner.get(BUMP_FALL_TYPE));
            owner.setBumpFall(owner.get(BUMP_FALL));
            owner.setBumpedChr(null);
            owner.postAnimationFinishing("bumped");
        } else {
            boolean wasBumped = owner.isBumped();
            owner.setBumpType(owner.get(BUMP_TYPE));
            owner.setBumpFallType(owner.get(BUMP_FALL_TYPE));
            owner.setBumpFall(owner.get(BUMP_FALL));
            owner.setBumpedChr(owner.get(CHARACTER_BUMP));
            if (wasBumped && !owner.isBumped()) {
                owner.postAnimationFinishing("bumped");
            }

            if (character != null) {
                wasBumped = character.isBumped();
                character.setBumpType(owner.get(CHARACTER_BUMP_TYPE));
                character.setHitFromBehind(owner.get(CHARACTER_BUMP_BEHIND));
                if (wasBumped && !character.isBumped()) {
                    character.postAnimationFinishing("bumped");
                }
            }
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (BUMP_FALL.fromDelegate(delegate)) {
            owner.fallenOnKnees();
        }
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.HALF;
    }
}
