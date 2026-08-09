// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.NetworkZombieAI;
import zombie.core.math.PZMath;
import zombie.iso.IsoChunk;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;

public class WalkTowardNetworkState extends State {
    private static final WalkTowardNetworkState INSTANCE = new WalkTowardNetworkState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);

    public static WalkTowardNetworkState instance() {
        return INSTANCE;
    }

    private WalkTowardNetworkState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.set(TICK_COUNT, 0L);
        owner.setVariable("bMoving", true);
        owner.setVariable("bPathfind", false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        PathFindBehavior2 pfb2 = zombie.getPathFindBehavior2();
        NetworkZombieAI networkAi = zombie.getNetworkCharacterAI();
        zombie.vectorToTarget.x = networkAi.targetX - zombie.getX();
        zombie.vectorToTarget.y = networkAi.targetY - zombie.getY();
        pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
        if (zombie.getZ() != networkAi.targetZ || networkAi.predictionType != 3 && networkAi.predictionType != 4) {
            if (zombie.getZ() == networkAi.targetZ
                && !PolygonalMap2.instance.lineClearCollide(zombie.getX(), zombie.getY(), networkAi.targetX, networkAi.targetY, networkAi.targetZ, null)) {
                if (networkAi.usePathFind) {
                    pfb2.reset();
                    zombie.setPath2(null);
                    networkAi.usePathFind = false;
                }

                pfb2.moveToPoint(networkAi.targetX, networkAi.targetY, 1.0F);
                zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(networkAi.targetX, networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F);
            } else if (zombie.getZ() == networkAi.targetZ
                && !PolygonalMap2.instance.lineClearCollide(zombie.getX(), zombie.getY(), zombie.realx, zombie.realy, zombie.realz, null)) {
                if (networkAi.usePathFind) {
                    pfb2.reset();
                    zombie.setPath2(null);
                    networkAi.usePathFind = false;
                }

                pfb2.moveToPoint(zombie.realx, zombie.realy, 1.0F);
                zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(networkAi.targetX, networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F);
            } else {
                if (!networkAi.usePathFind) {
                    pfb2.pathToLocationF(zombie.realx, zombie.realy, zombie.realz);
                    pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
                    networkAi.usePathFind = true;
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
            if (networkAi.usePathFind) {
                pfb2.reset();
                zombie.setPath2(null);
                networkAi.usePathFind = false;
            }

            pfb2.moveToPoint(networkAi.targetX, networkAi.targetY, 1.0F);
            zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(networkAi.targetX, networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5F);
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

        long tickCount = owner.get(TICK_COUNT);
        if (tickCount == 2L) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }

        owner.set(TICK_COUNT, tickCount + 1L);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bMoving", false);
    }
}
