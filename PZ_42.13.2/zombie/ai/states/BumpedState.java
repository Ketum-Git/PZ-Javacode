// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.HashMap;
import zombie.AttackType;
import zombie.ai.State;
import zombie.audio.parameters.ParameterCharacterMovementSpeed;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.Type;

public final class BumpedState extends State {
    private static final BumpedState _instance = new BumpedState();
    private static final Integer PARAM_BUMP_TYPE = 0;
    private static final Integer PARAM_BUMP_FALL_TYPE = 1;
    private static final Integer PARAM_BUMP_FALL = 2;
    private static final Integer PARAM_CHARACTER_BUMP = 3;
    private static final Integer PARAM_CHARACTER_BUMP_TYPE = 4;
    private static final Integer PARAM_CHARACTER_BUMP_BEHIND = 5;

    public static BumpedState instance() {
        return _instance;
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
                    int tripType = switch (var8) {
                        case 0 -> 5;
                        default -> 6;
                    };
                    owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), tripType);
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_BUMP_TYPE, owner.getBumpType());
            StateMachineParams.put(PARAM_BUMP_FALL_TYPE, owner.getBumpFallType());
            StateMachineParams.put(PARAM_BUMP_FALL, owner.isBumpFall());
            IsoGameCharacter character = owner.getBumpedChr();
            if (character != null) {
                StateMachineParams.put(PARAM_CHARACTER_BUMP, character);
                StateMachineParams.put(PARAM_CHARACTER_BUMP_TYPE, character.getBumpType());
                StateMachineParams.put(PARAM_CHARACTER_BUMP_BEHIND, character.isHitFromBehind());
            } else {
                StateMachineParams.remove(PARAM_CHARACTER_BUMP);
                StateMachineParams.remove(PARAM_CHARACTER_BUMP_TYPE);
                StateMachineParams.remove(PARAM_CHARACTER_BUMP_BEHIND);
            }
        } else {
            owner.setBumpType((String)StateMachineParams.getOrDefault(PARAM_BUMP_TYPE, owner.getBumpType()));
            owner.setBumpFallType((String)StateMachineParams.getOrDefault(PARAM_BUMP_FALL_TYPE, owner.getBumpFallType()));
            owner.setBumpFall((Boolean)StateMachineParams.getOrDefault(PARAM_BUMP_FALL, owner.isBumpFall()));
            owner.setBumpedChr((IsoGameCharacter)StateMachineParams.getOrDefault(PARAM_CHARACTER_BUMP, null));
            if (owner.getBumpedChr() != null) {
                owner.getBumpedChr().setBumpType((String)StateMachineParams.getOrDefault(PARAM_CHARACTER_BUMP_TYPE, ""));
                owner.getBumpedChr().setHitFromBehind((Boolean)StateMachineParams.getOrDefault(PARAM_CHARACTER_BUMP_BEHIND, false));
            }

            if (State.Stage.Exit == stage) {
                owner.reportEvent("ActiveAnimFinishing");
            }
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return true;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
