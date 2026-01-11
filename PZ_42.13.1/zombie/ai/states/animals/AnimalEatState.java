// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class AnimalEatState extends State {
    private static final AnimalEatState _instance = new AnimalEatState();

    public static AnimalEatState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.getData().eatingGrass) {
            owner.getEmitter().playSound("AnimalFoleyEatGrass");
        } else if (animal.eatFromGround != null) {
            owner.getEmitter().playSound("AnimalFoleyEatGrass");
        } else if (animal.eatFromTrough != null) {
            owner.getEmitter().playSound("AnimalFoleyEatGrass");
        } else if (animal.drinkFromTrough == null && animal.isVariable("eatingAnim", "feed")) {
            owner.getEmitter().playSound("AnimalFoleyFeedFromMother");
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        animal.setStateEventDelayTimer(300.0F);
        if (animal.eatFromTrough != null) {
            animal.faceThisObject(animal.eatFromTrough);
        }

        if (animal.eatFromGround != null) {
            animal.faceThisObject(animal.eatFromGround);
        }

        if (animal.drinkFromTrough != null) {
            animal.faceThisObject(animal.drinkFromTrough);
        }

        if (animal.drinkFromRiver != null) {
            animal.faceThisObject(animal.drinkFromRiver.getFloor());
        }

        if (animal.drinkFromPuddle != null) {
            animal.faceThisObject(animal.drinkFromPuddle.getFloor());
        }

        animal.setVariable("bMoving", false);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.getEmitter().stopOrTriggerSoundByName("AnimalFoleyEatGrass");
        owner.getEmitter().stopOrTriggerSoundByName("AnimalFoleyFeedFromMother");
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("idleActionEnd")) {
            IsoAnimal animal = (IsoAnimal)owner;
            owner.clearVariable("eatingAnim");
            owner.clearVariable("idleAction");
            if (animal.drinkFromTrough != null || animal.drinkFromRiver != null || animal.drinkFromPuddle != null) {
                animal.getData().drink();
                return;
            }

            if (animal.eatFromGround != null && animal.eatFromGround.isPureWater(true)) {
                animal.getData().drinkFromGround();
                return;
            }

            animal.getData().eat();
        }
    }
}
