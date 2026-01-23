// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

@UsedFromLua
public final class FitnessState extends State {
    private static final FitnessState _instance = new FitnessState();
    private static final Integer PARAM_FITNESS_FINISHED = 0;
    private static final Integer PARAM_EXERCISE_ENDED = 1;
    private static final Integer PARAM_EXERCISE_TYPE = 2;
    private static final Integer PARAM_EXERCISE_HAND = 3;
    private static final Integer PARAM_FITNESS_SPEED = 4;
    private static final Integer PARAM_FITNESS_STRUGGLE = 5;
    private static final int switchTime = 4;

    public static FitnessState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setVariable("FitnessFinished", false);
        owner.clearVariable("ExerciseStarted");
        owner.clearVariable("ExerciseEnded");
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.clearVariable("FitnessFinished");
        owner.clearVariable("ExerciseStarted");
        owner.clearVariable("ExerciseHand");
        owner.clearVariable("FitnessStruggle");
        owner.setVariable("ExerciseEnded", true);
        owner.clearVariable("PlayerVoiceSound");
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound") && player != null) {
            if (player.getVariableBoolean("PlayerVoiceSound")) {
            }

            player.setVariable("PlayerVoiceSound", true);
            player.stopPlayerVoiceSound(event.parameterValue);
            player.playerVoiceSound(event.parameterValue);
        } else if (event.eventName.equalsIgnoreCase("ActiveAnimLooped")) {
            if (owner.isLocal()) {
                INetworkPacket.send(PacketTypes.PacketType.State, owner, this, State.Stage.Execute);
            } else {
                owner.getNetworkCharacterAI().switchTime--;
                if (owner.getNetworkCharacterAI().switchTime == 0) {
                    if ("left".equals(owner.getVariableString("ExerciseHand"))) {
                        owner.clearVariable("ExerciseHand");
                        owner.setPrimaryHandItem(owner.getSecondaryHandItem());
                        owner.setSecondaryHandItem(null);
                    } else {
                        owner.setVariable("ExerciseHand", "left");
                        owner.setSecondaryHandItem(owner.getPrimaryHandItem());
                        owner.setPrimaryHandItem(null);
                    }

                    owner.getNetworkCharacterAI().switchTime = 4;
                }
            }
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_FITNESS_FINISHED, owner.getVariableBoolean("FitnessFinished"));
            StateMachineParams.put(PARAM_EXERCISE_ENDED, owner.getVariableBoolean("ExerciseEnded"));
            StateMachineParams.put(PARAM_EXERCISE_TYPE, owner.getVariableString("ExerciseType"));
            StateMachineParams.put(PARAM_EXERCISE_HAND, owner.getVariableString("ExerciseHand"));
            StateMachineParams.put(PARAM_FITNESS_SPEED, owner.getVariableFloat("FitnessSpeed", 1.0F));
            StateMachineParams.put(PARAM_FITNESS_STRUGGLE, owner.getVariableBoolean("FitnessStruggle"));
        } else {
            owner.setVariable("FitnessFinished", (Boolean)StateMachineParams.getOrDefault(PARAM_FITNESS_FINISHED, false));
            owner.setVariable("ExerciseEnded", (Boolean)StateMachineParams.getOrDefault(PARAM_EXERCISE_ENDED, false));
            owner.setVariable("ExerciseType", (String)StateMachineParams.getOrDefault(PARAM_EXERCISE_TYPE, ""));
            owner.setVariable("ExerciseHand", (String)StateMachineParams.getOrDefault(PARAM_EXERCISE_HAND, ""));
            owner.setVariable("FitnessSpeed", (Float)StateMachineParams.getOrDefault(PARAM_FITNESS_SPEED, 1.0F));
            owner.setVariable("FitnessStruggle", (Boolean)StateMachineParams.getOrDefault(PARAM_FITNESS_STRUGGLE, false));
            owner.getNetworkCharacterAI().switchTime = 4;
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
        return true;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }
}
