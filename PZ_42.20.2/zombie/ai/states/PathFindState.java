// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
import zombie.iso.IsoChunk;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;

@UsedFromLua
public final class PathFindState extends State {
    private static final PathFindState INSTANCE = new PathFindState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);

    public static PathFindState instance() {
        return INSTANCE;
    }

    private PathFindState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVariable("bPathfind", true);
        owner.setVariable("bMoving", false);
        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.getNetworkCharacterAI().extraUpdate();
        }

        owner.set(TICK_COUNT, 0L);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
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
                long tickCount = owner.get(TICK_COUNT);
                if (tickCount == 2L) {
                    isoZombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
                }

                owner.set(TICK_COUNT, tickCount + 1L);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        if (owner instanceof IsoZombie isoZombie) {
            isoZombie.getNetworkCharacterAI().extraUpdate();
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
