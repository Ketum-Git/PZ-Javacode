// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.ai.astar.AStarPathFinder;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.nativeCode.PathfindNative;

@UsedFromLua
public final class PathFindState2 extends State {
    private static final Integer PARAM_TICK_COUNT = 0;

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setVariable("bPathfind", true);
        owner.setVariable("bMoving", false);
        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.networkAi.extraUpdate();
        }

        StateMachineParams.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        PathFindBehavior2.BehaviorResult result = owner.getPathFindBehavior2().update();
        if (result == PathFindBehavior2.BehaviorResult.Failed) {
            owner.setPathFindIndex(-1);
            owner.setVariable("bPathfind", false);
            owner.setVariable("bMoving", false);
        } else if (result == PathFindBehavior2.BehaviorResult.Succeeded) {
            int tx = PZMath.fastfloor(owner.getPathFindBehavior2().getTargetX());
            int ty = PZMath.fastfloor(owner.getPathFindBehavior2().getTargetY());
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(tx / 8, ty / 8) : IsoWorld.instance.currentCell.getChunkForGridSquare(tx, ty, 0);
            if (chunk == null) {
                owner.setVariable("bPathfind", false);
                owner.setVariable("bMoving", true);
            } else {
                owner.setVariable("bPathfind", false);
                owner.setVariable("bMoving", false);
                owner.setPath2(null);
            }
        } else {
            if (owner instanceof IsoZombie isoZombie) {
                long tickCount = (Long)StateMachineParams.get(PARAM_TICK_COUNT);
                if (IngameState.instance.numberTicks - tickCount == 2L) {
                    isoZombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.networkAi.extraUpdate();
            isoZombie.allowRepathDelay = 0.0F;
        }

        owner.setVariable("bPathfind", false);
        owner.setVariable("bMoving", false);
        owner.setVariable("ShouldBeCrawling", false);
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.cancelRequest(owner);
        } else {
            PolygonalMap2.instance.cancelRequest(owner);
        }

        owner.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
        owner.setPath2(null);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return owner.isMoving();
    }
}
