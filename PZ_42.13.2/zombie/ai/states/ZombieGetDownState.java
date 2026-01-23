// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
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
    private static final ZombieGetDownState _instance = new ZombieGetDownState();
    static final Integer PARAM_PREV_STATE = 1;
    static final Integer PARAM_WAIT_TIME = 2;
    static final Integer PARAM_START_X = 3;
    static final Integer PARAM_START_Y = 4;

    public static ZombieGetDownState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_PREV_STATE, owner.getStateMachine().getPrevious());
        StateMachineParams.put(PARAM_START_X, owner.getX());
        StateMachineParams.put(PARAM_START_Y, owner.getY());
        owner.setStateEventDelayTimer((Float)StateMachineParams.get(PARAM_WAIT_TIME));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoZombie zombie = (IsoZombie)owner;
        zombie.setStateEventDelayTimer(0.0F);
        zombie.allowRepathDelay = 0.0F;
        if (StateMachineParams.get(PARAM_PREV_STATE) == PathFindState.instance()) {
            if (owner.getPathFindBehavior2().getTargetChar() == null) {
                owner.setVariable("bPathfind", true);
                owner.setVariable("bMoving", false);
            } else if (zombie.isTargetLocationKnown()) {
                owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
            } else if (zombie.lastTargetSeenX != -1) {
                owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
            }
        } else if (StateMachineParams.get(PARAM_PREV_STATE) == WalkTowardState.instance()) {
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        Float startX = (Float)StateMachineParams.get(PARAM_START_X);
        Float startY = (Float)StateMachineParams.get(PARAM_START_Y);
        return startX != null && startY != null ? owner.DistToSquared(startX, startY) <= 0.25F : false;
    }

    public void setParams(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_WAIT_TIME, Rand.Next(60.0F, 150.0F));
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
