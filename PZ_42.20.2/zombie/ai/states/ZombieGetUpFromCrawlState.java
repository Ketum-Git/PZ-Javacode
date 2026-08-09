// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class ZombieGetUpFromCrawlState extends State {
    private static final ZombieGetUpFromCrawlState INSTANCE = new ZombieGetUpFromCrawlState();
    public static final State.Param<State> STATE = State.Param.of("state", State.class);

    public static ZombieGetUpFromCrawlState instance() {
        return INSTANCE;
    }

    private ZombieGetUpFromCrawlState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.set(STATE, owner.getStateMachine().getPrevious());
        IsoZombie zombie = (IsoZombie)owner;
        if (zombie.isCrawling()) {
            zombie.toggleCrawling();
            zombie.setOnFloor(true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.allowRepathDelay = 0.0F;
        if (owner.get(STATE) == PathFindState.instance()) {
            if (owner.getPathFindBehavior2().getTargetChar() == null) {
                owner.setVariable("bPathfind", true);
                owner.setVariable("bMoving", false);
            } else if (zombie.isTargetLocationKnown()) {
                owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
            } else if (zombie.lastTargetSeenX != -1) {
                owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
            }
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }
}
