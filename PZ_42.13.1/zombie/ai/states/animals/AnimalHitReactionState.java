// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class AnimalHitReactionState extends State {
    private static final AnimalHitReactionState _instance = new AnimalHitReactionState();

    public static AnimalHitReactionState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHitReaction(null);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
        }

        if ("PlayBreedSound".equalsIgnoreCase(event.eventName)) {
            animal.onPlayBreedSoundEvent(event.parameterValue);
        }
    }
}
