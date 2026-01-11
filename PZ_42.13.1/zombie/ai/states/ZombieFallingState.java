// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class ZombieFallingState extends State {
    private static final ZombieFallingState _instance = new ZombieFallingState();

    public static ZombieFallingState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.clearVariable("bLandAnimFinished");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("bLandAnimFinished");
        owner.clearFallDamage();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
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
