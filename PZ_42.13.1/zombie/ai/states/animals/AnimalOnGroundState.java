// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class AnimalOnGroundState extends State {
    private static final AnimalOnGroundState _instance = new AnimalOnGroundState();

    public static AnimalOnGroundState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setCollidable(false);
        if (owner.isDead()) {
            owner.die();
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isDead()) {
            owner.die();
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }
}
