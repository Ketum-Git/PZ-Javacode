// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.network.GameClient;
import zombie.util.StringUtils;

@UsedFromLua
public final class ZombieGetUpState extends State {
    private static final ZombieGetUpState INSTANCE = new ZombieGetUpState();
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);

    public static ZombieGetUpState instance() {
        return INSTANCE;
    }

    private ZombieGetUpState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter chrOwner) {
        IsoZombie owner = (IsoZombie)chrOwner;
        State previousState = owner.getStateMachine().getPrevious();
        if (previousState == ZombieGetUpFromCrawlState.instance()) {
            previousState = owner.get(ZombieGetUpFromCrawlState.STATE);
        }

        owner.set(PREV_STATE, previousState);
        owner.parameterZombieState.setState(ParameterZombieState.State.GettingUp);
        owner.setOnFloor(true);
        if (GameClient.client) {
            owner.setKnockedDown(false);
        }
    }

    @Override
    public void exit(IsoGameCharacter chrOwner) {
        IsoZombie owner = (IsoZombie)chrOwner;
        owner.setCollidable(true);
        owner.clearVariable("SprinterTripped");
        owner.clearVariable("ShouldStandUp");
        if (StringUtils.isNullOrEmpty(owner.getHitReaction())) {
            owner.setSitAgainstWall(false);
        }

        owner.setKnockedDown(false);
        owner.allowRepathDelay = 0.0F;
        State prevState = owner.get(PREV_STATE);
        if (prevState == PathFindState.instance()) {
            if (owner.getPathFindBehavior2().getTargetChar() == null) {
                owner.setVariable("bPathfind", true);
                owner.setVariable("bMoving", false);
            } else if (owner.isTargetLocationKnown()) {
                owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
            } else if (owner.lastTargetSeenX != -1) {
                owner.pathToLocation(owner.lastTargetSeenX, owner.lastTargetSeenY, owner.lastTargetSeenZ);
            }
        } else if (prevState == WalkTowardState.instance()) {
            owner.setVariable("bPathFind", false);
            owner.setVariable("bMoving", true);
        }
    }
}
