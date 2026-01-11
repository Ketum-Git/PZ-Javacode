// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;

public class WalkTowardNetworkState extends State {
    private static final WalkTowardNetworkState INSTANCE = new WalkTowardNetworkState();
    private static final Integer PARAM_TICK_COUNT = 2;

    public static WalkTowardNetworkState instance() {
        return INSTANCE;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_TICK_COUNT, IngameState.instance.numberTicks);
        owner.setVariable("bMoving", true);
        owner.setVariable("bPathfind", false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        PathFindBehavior2 pfb2 = zombie.getPathFindBehavior2();
        zombie.vectorToTarget.x = zombie.networkAi.targetX - zombie.getX();
        zombie.vectorToTarget.y = zombie.networkAi.targetY - zombie.getY();
        pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
        if (zombie.getZ() != zombie.networkAi.targetZ || zombie.networkAi.predictionType != 3 && zombie.networkAi.predictionType != 4) {
            if (zombie.getZ() == zombie.networkAi.targetZ
                && !PolygonalMap2.instance
                    .lineClearCollide(zombie.getX(), zombie.getY(), zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.networkAi.targetZ, null)) {
                if (zombie.networkAi.usePathFind) {
                    pfb2.reset();
                    zombie.setPath2(null);
                    zombie.networkAi.usePathFind = false;
                }

                pfb2.moveToPoint(zombie.networkAi.targetX, zombie.networkAi.targetY, 1.0F);
                zombie.setVariable(
                    "bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F
                );
            } else if (zombie.getZ() == zombie.networkAi.targetZ
                && !PolygonalMap2.instance.lineClearCollide(zombie.getX(), zombie.getY(), zombie.realx, zombie.realy, zombie.realz, null)) {
                if (zombie.networkAi.usePathFind) {
                    pfb2.reset();
                    zombie.setPath2(null);
                    zombie.networkAi.usePathFind = false;
                }

                pfb2.moveToPoint(zombie.realx, zombie.realy, 1.0F);
                zombie.setVariable(
                    "bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F
                );
            } else {
                if (!zombie.networkAi.usePathFind) {
                    pfb2.pathToLocationF(zombie.realx, zombie.realy, zombie.realz);
                    pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
                    zombie.networkAi.usePathFind = true;
                }

                PathFindBehavior2.BehaviorResult result = pfb2.update();
                if (result == PathFindBehavior2.BehaviorResult.Failed) {
                    zombie.setPathFindIndex(-1);
                    return;
                }

                if (result == PathFindBehavior2.BehaviorResult.Succeeded) {
                    int tx = PZMath.fastfloor(zombie.getPathFindBehavior2().getTargetX());
                    int ty = PZMath.fastfloor(zombie.getPathFindBehavior2().getTargetY());
                    IsoChunk chunk = GameServer.server
                        ? ServerMap.instance.getChunk(tx / 8, ty / 8)
                        : IsoWorld.instance.currentCell.getChunkForGridSquare(tx, ty, 0);
                    if (chunk == null) {
                        zombie.setVariable("bMoving", true);
                        return;
                    }

                    zombie.setPath2(null);
                    zombie.setVariable("bMoving", true);
                    return;
                }
            }
        } else {
            if (zombie.networkAi.usePathFind) {
                pfb2.reset();
                zombie.setPath2(null);
                zombie.networkAi.usePathFind = false;
            }

            pfb2.moveToPoint(zombie.networkAi.targetX, zombie.networkAi.targetY, 1.0F);
            zombie.setVariable(
                "bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F
            );
        }

        if (!((IsoZombie)owner).crawling) {
            owner.setOnFloor(false);
        }

        boolean bCollidedWithVehicle = owner.isCollidedWithVehicle();
        if (zombie.target instanceof IsoGameCharacter isoGameCharacter
            && isoGameCharacter.getVehicle() != null
            && isoGameCharacter.getVehicle().isCharacterAdjacentTo(owner)) {
            bCollidedWithVehicle = false;
        }

        if (owner.isCollidedThisFrame() || bCollidedWithVehicle) {
            zombie.allowRepathDelay = 0.0F;
            zombie.pathToLocation(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
            if (!"true".equals(zombie.getVariableString("bPathfind"))) {
                zombie.setVariable("bPathfind", true);
                zombie.setVariable("bMoving", false);
            }
        }

        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        long tickCount = (Long)StateMachineParams.get(PARAM_TICK_COUNT);
        if (IngameState.instance.numberTicks - tickCount == 2L) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bMoving", false);
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
