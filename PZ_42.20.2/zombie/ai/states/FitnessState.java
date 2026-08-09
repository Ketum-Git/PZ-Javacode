// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
    private static final FitnessState INSTANCE = new FitnessState();
    private static final int switchTime = 4;
    public static final State.Param<Boolean> FITNESS_FINISHED = State.Param.ofBool("fitness_finished", false);
    public static final State.Param<Boolean> EXERCISE_ENDED = State.Param.ofBool("exercise_ended", false);
    public static final State.Param<String> EXERCISE_TYPE = State.Param.ofString("exercise_type", "");
    public static final State.Param<String> EXERCISE_HAND = State.Param.ofString("exercise_hand", "");
    public static final State.Param<Float> FITNESS_SPEED = State.Param.ofFloat("fitness_speed", 1.0F);
    public static final State.Param<Boolean> FITNESS_STRUGGLE = State.Param.ofBool("fitness_struggle", false);

    public static FitnessState instance() {
        return INSTANCE;
    }

    private FitnessState() {
        super(true, true, true, true);
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
        if (owner.isLocal()) {
            owner.set(FITNESS_FINISHED, owner.getVariableBoolean("FitnessFinished"));
            owner.set(EXERCISE_ENDED, owner.getVariableBoolean("ExerciseEnded"));
            owner.set(EXERCISE_TYPE, owner.getVariableString("ExerciseType"));
            owner.set(EXERCISE_HAND, owner.getVariableString("ExerciseHand"));
            owner.set(FITNESS_SPEED, owner.getVariableFloat("FitnessSpeed", 1.0F));
            owner.set(FITNESS_STRUGGLE, owner.getVariableBoolean("FitnessStruggle"));
        } else {
            owner.setVariable("FitnessFinished", owner.get(FITNESS_FINISHED));
            owner.setVariable("ExerciseEnded", owner.get(EXERCISE_ENDED));
            owner.setVariable("ExerciseType", owner.get(EXERCISE_TYPE));
            owner.setVariable("ExerciseHand", owner.get(EXERCISE_HAND));
            owner.setVariable("FitnessSpeed", owner.get(FITNESS_SPEED));
            owner.setVariable("FitnessStruggle", owner.get(FITNESS_STRUGGLE));
            owner.getNetworkCharacterAI().switchTime = 4;
        }

        super.setParams(owner, stage);
    }
}
