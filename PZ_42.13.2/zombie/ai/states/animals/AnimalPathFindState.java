// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import java.util.HashMap;
import zombie.ai.State;
import zombie.ai.astar.AStarPathFinder;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;

public final class AnimalPathFindState extends State {
    private static final Integer PARAM_TICK_COUNT = 0;
    private static final AnimalPathFindState _instance = new AnimalPathFindState();

    public static AnimalPathFindState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setVariable("bPathfind", true);
        owner.setVariable("bMoving", false);
        StateMachineParams.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoAnimal animal = (IsoAnimal)owner;
        PathFindBehavior2.BehaviorResult result = owner.getPathFindBehavior2().update();
        if (result == PathFindBehavior2.BehaviorResult.Failed) {
            int pathX = owner.getPathTargetX();
            int pathY = owner.getPathTargetY();
            int pathZ = owner.getPathTargetZ();
            ((IsoAnimal)owner).pathFailed();
            animal.setShouldFollowWall(true);
            animal.getPathFindBehavior2().pathToLocation(pathX, pathY, pathZ);
            owner.setVariable("bMoving", true);
        } else if (result == PathFindBehavior2.BehaviorResult.Succeeded) {
            int tx = (int)owner.getPathFindBehavior2().getTargetX();
            int ty = (int)owner.getPathFindBehavior2().getTargetY();
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(tx / 8, ty / 8) : IsoWorld.instance.currentCell.getChunkForGridSquare(tx, ty, 0);
            if (chunk == null) {
                owner.setVariable("bPathfind", false);
                owner.setVariable("bMoving", true);
            } else {
                owner.setVariable("bPathfind", false);
                owner.setVariable("bMoving", false);
                owner.setPath2(null);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bPathfind", false);
        owner.setVariable("bMoving", false);
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.cancelRequest(owner);
        } else {
            PolygonalMap2.instance.cancelRequest(owner);
        }

        owner.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
        owner.setPath2(null);
        ((IsoAnimal)owner).getBehavior().doBehaviorAction();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoAnimal animal = (IsoAnimal)owner;
        if ("PlayBreedSound".equalsIgnoreCase(event.eventName)) {
            animal.onPlayBreedSoundEvent(event.parameterValue);
        }
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return owner.isMoving();
    }
}
