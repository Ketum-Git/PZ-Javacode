// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.StringUtils;

public final class AnimalIdleState extends State {
    private static final AnimalIdleState _instance = new AnimalIdleState();

    public static AnimalIdleState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.isLocalPlayer()) {
            animal.getBehavior().wanderIdle();
            if (animal.eatFromTrough != null) {
                animal.faceThisObject(animal.eatFromTrough);
            }

            if (animal.drinkFromTrough != null) {
                animal.faceThisObject(animal.drinkFromTrough);
            }

            if (animal.isAnimalEating()) {
                animal.getStateMachine().changeState(AnimalEatState.instance(), null);
            }

            if (animal.getVariableBoolean("bMoving")) {
                animal.getStateMachine().changeState(AnimalWalkState.instance(), null);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoAnimal animal = (IsoAnimal)owner;
        if (event.eventName.equalsIgnoreCase("idleActionEnd")) {
            if (StringUtils.isNullOrEmpty(owner.getVariableString("sittingAnim")) && !"sit".equals(owner.getVariableString("idleAction"))) {
                owner.clearVariable("idleAction");
            }

            owner.clearVariable("sittingAnim");
        }

        if ("PlayBreedSound".equalsIgnoreCase(event.eventName)) {
            if ("petting".equalsIgnoreCase(event.parameterValue)) {
                long petTime = StateMachineParams.getOrDefault("PetSoundTime", 0L) instanceof Long l ? l : 0L;
                if (System.currentTimeMillis() - petTime < 5000L) {
                    return;
                }

                StateMachineParams.put("PetSoundTime", System.currentTimeMillis());
            }

            animal.onPlayBreedSoundEvent(event.parameterValue);
            animal.clearVariable("PlayBreedSound");
        }
    }
}
