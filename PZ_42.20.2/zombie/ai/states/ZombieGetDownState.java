// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class ZombieGetDownState extends State {
    private static final ZombieGetDownState INSTANCE = new ZombieGetDownState();
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Float> WAIT_TIME = State.Param.ofFloat("wait_time", 0.0F);
    public static final State.Param<Float> START_X = State.Param.ofFloat("start_x", Float.NEGATIVE_INFINITY);
    public static final State.Param<Float> START_Y = State.Param.ofFloat("start_y", Float.NEGATIVE_INFINITY);

    public static ZombieGetDownState instance() {
        return INSTANCE;
    }

    private ZombieGetDownState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.set(PREV_STATE, owner.getStateMachine().getPrevious());
        owner.set(START_X, owner.getX());
        owner.set(START_Y, owner.getY());
        owner.setStateEventDelayTimer(owner.get(WAIT_TIME));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.setStateEventDelayTimer(0.0F);
        zombie.allowRepathDelay = 0.0F;
        State prevState = owner.get(PREV_STATE);
        if (prevState == PathFindState.instance()) {
            if (owner.getPathFindBehavior2().getTargetChar() == null) {
                owner.setVariable("bPathfind", true);
                owner.setVariable("bMoving", false);
            } else if (zombie.isTargetLocationKnown()) {
                owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
            } else if (zombie.lastTargetSeenX != -1) {
                owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
            }
        } else if (prevState == WalkTowardState.instance()) {
            owner.setVariable("bPathFind", false);
            owner.setVariable("bMoving", true);
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (event.eventName.equalsIgnoreCase("StartCrawling") && !zombie.isCrawling()) {
            zombie.toggleCrawling();
        }
    }

    public boolean isNearStartXY(IsoGameCharacter owner) {
        float startX = owner.get(START_X);
        float startY = owner.get(START_Y);
        return startX != Float.NEGATIVE_INFINITY && startY != Float.NEGATIVE_INFINITY ? owner.DistToSquared(startX, startY) <= 0.25F : false;
    }

    public void setParams(IsoGameCharacter owner) {
        owner.set(WAIT_TIME, Rand.Next(60.0F, 150.0F));
    }
}
