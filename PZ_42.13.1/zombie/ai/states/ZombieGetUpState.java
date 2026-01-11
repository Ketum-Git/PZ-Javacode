// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.network.GameClient;
import zombie.util.StringUtils;

@UsedFromLua
public final class ZombieGetUpState extends State {
    private static final ZombieGetUpState _instance = new ZombieGetUpState();
    static final Integer PARAM_PREV_STATE = 2;

    public static ZombieGetUpState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter chrOwner) {
        IsoZombie owner = (IsoZombie)chrOwner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        State previousState = owner.getStateMachine().getPrevious();
        if (previousState == ZombieGetUpFromCrawlState.instance()) {
            previousState = (State)owner.getStateMachineParams(ZombieGetUpFromCrawlState.instance()).get(1);
        }

        StateMachineParams.put(PARAM_PREV_STATE, previousState);
        owner.parameterZombieState.setState(ParameterZombieState.State.GettingUp);
        owner.setOnFloor(true);
        if (GameClient.client) {
            owner.setKnockedDown(false);
        }
    }

    @Override
    public void exit(IsoGameCharacter chrOwner) {
        IsoZombie owner = (IsoZombie)chrOwner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setCollidable(true);
        owner.clearVariable("SprinterTripped");
        owner.clearVariable("ShouldStandUp");
        if (StringUtils.isNullOrEmpty(owner.getHitReaction())) {
            owner.setSitAgainstWall(false);
        }

        owner.setKnockedDown(false);
        owner.allowRepathDelay = 0.0F;
        if (StateMachineParams.get(PARAM_PREV_STATE) == PathFindState.instance()) {
            if (owner.getPathFindBehavior2().getTargetChar() == null) {
                owner.setVariable("bPathfind", true);
                owner.setVariable("bMoving", false);
            } else if (owner.isTargetLocationKnown()) {
                owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
            } else if (owner.lastTargetSeenX != -1) {
                owner.pathToLocation(owner.lastTargetSeenX, owner.lastTargetSeenY, owner.lastTargetSeenZ);
            }
        } else if (StateMachineParams.get(PARAM_PREV_STATE) == WalkTowardState.instance()) {
            owner.setVariable("bPathFind", false);
            owner.setVariable("bMoving", true);
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
