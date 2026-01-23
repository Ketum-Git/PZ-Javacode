// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PolygonalMap2;
import zombie.util.Type;

@UsedFromLua
public final class WalkTowardState extends State {
    private static final WalkTowardState _instance = new WalkTowardState();
    private static final Integer PARAM_IGNORE_OFFSET = 0;
    private static final Integer PARAM_IGNORE_TIME = 1;
    private static final Integer PARAM_TICK_COUNT = 2;
    private final Vector2 temp = new Vector2();
    private final Vector3f worldPos = new Vector3f();

    public static WalkTowardState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (StateMachineParams.get(PARAM_IGNORE_OFFSET) == null) {
            StateMachineParams.put(PARAM_IGNORE_OFFSET, Boolean.FALSE);
            StateMachineParams.put(PARAM_IGNORE_TIME, 0L);
        }

        if (StateMachineParams.get(PARAM_IGNORE_OFFSET) == Boolean.TRUE && System.currentTimeMillis() - (Long)StateMachineParams.get(PARAM_IGNORE_TIME) > 3000L
            )
         {
            StateMachineParams.put(PARAM_IGNORE_OFFSET, Boolean.FALSE);
            StateMachineParams.put(PARAM_IGNORE_TIME, 0L);
        }

        StateMachineParams.put(PARAM_TICK_COUNT, 0L);
        if (((IsoZombie)owner).isUseless()) {
            owner.changeState(ZombieIdleState.instance());
        }

        owner.getPathFindBehavior2().walkingOnTheSpot.reset(owner.getX(), owner.getY());
        ((IsoZombie)owner).networkAi.extraUpdate();
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoZombie zomb = (IsoZombie)owner;
        if (!zomb.crawling) {
            owner.setOnFloor(false);
        }

        IsoGameCharacter targetChr = Type.tryCastTo(zomb.target, IsoGameCharacter.class);
        if (zomb.target != null) {
            if (zomb.isTargetLocationKnown()) {
                if (targetChr != null) {
                    zomb.getPathFindBehavior2().pathToCharacter(targetChr);
                    if (targetChr.getVehicle() != null && zomb.DistToSquared(zomb.target) < 16.0F) {
                        Vector3f v = targetChr.getVehicle().chooseBestAttackPosition(targetChr, zomb, this.worldPos);
                        if (v == null) {
                            zomb.setVariable("bMoving", false);
                            return;
                        }

                        if (Math.abs(owner.getX() - zomb.getPathFindBehavior2().getTargetX()) > 0.1F
                            || Math.abs(owner.getY() - zomb.getPathFindBehavior2().getTargetY()) > 0.1F) {
                            zomb.setVariable("bPathfind", true);
                            zomb.setVariable("bMoving", false);
                            return;
                        }
                    }
                }
            } else if (zomb.lastTargetSeenX != -1
                && !owner.getPathFindBehavior2().isTargetLocation(zomb.lastTargetSeenX + 0.5F, zomb.lastTargetSeenY + 0.5F, zomb.lastTargetSeenZ)) {
                owner.pathToLocation(zomb.lastTargetSeenX, zomb.lastTargetSeenY, zomb.lastTargetSeenZ);
            }
        }

        if (owner.getPathTargetX() == PZMath.fastfloor(owner.getX()) && owner.getPathTargetY() == PZMath.fastfloor(owner.getY())) {
            if (zomb.target == null) {
                zomb.setVariable("bPathfind", false);
                zomb.setVariable("bMoving", false);
                return;
            }

            if (PZMath.fastfloor(zomb.target.getZ()) != PZMath.fastfloor(owner.getZ())) {
                zomb.setVariable("bPathfind", true);
                zomb.setVariable("bMoving", false);
                return;
            }
        }

        boolean bCollidedWithVehicle = owner.isCollidedWithVehicle();
        if (targetChr != null && targetChr.getVehicle() != null && targetChr.getVehicle().isCharacterAdjacentTo(owner)) {
            bCollidedWithVehicle = false;
        }

        boolean bCollidedWithObject = owner.isCollidedThisFrame();
        if (bCollidedWithObject && StateMachineParams.get(PARAM_IGNORE_OFFSET) == Boolean.FALSE) {
            StateMachineParams.put(PARAM_IGNORE_OFFSET, Boolean.TRUE);
            StateMachineParams.put(PARAM_IGNORE_TIME, System.currentTimeMillis());
            float x = zomb.getPathFindBehavior2().getTargetX();
            float y = zomb.getPathFindBehavior2().getTargetY();
            float z = zomb.getZ();
            bCollidedWithObject = !this.isPathClear(owner, x, y, z);
        }

        if (!bCollidedWithObject && !bCollidedWithVehicle) {
            float targetX = zomb.getPathFindBehavior2().getTargetX();
            float targetY = zomb.getPathFindBehavior2().getTargetY();
            this.temp.x = targetX;
            this.temp.y = targetY;
            this.temp.x = this.temp.x - zomb.getX();
            this.temp.y = this.temp.y - zomb.getY();
            float dist = this.temp.getLength();
            if (dist < 0.25F) {
                owner.setX(targetX);
                owner.setY(targetY);
                owner.setNextX(owner.getX());
                owner.setNextY(owner.getY());
                dist = 0.0F;
            }

            if (dist < 0.025F) {
                zomb.setVariable("bPathfind", false);
                zomb.setVariable("bMoving", false);
            } else {
                if (!GameServer.server && !zomb.crawling && StateMachineParams.get(PARAM_IGNORE_OFFSET) == Boolean.FALSE) {
                    float distScale = Math.min(dist / 2.0F, 4.0F);
                    float x = (zomb.getID() + zomb.zombieId) % 20 / 10.0F - 1.0F;
                    float y = (zomb.getID() + zomb.zombieId) % 20 / 10.0F - 1.0F;
                    if (IsoUtils.DistanceTo(owner.getX(), owner.getY(), targetX + x * distScale, targetY + y * distScale) < dist) {
                        this.temp.x = targetX + x * distScale - zomb.getX();
                        this.temp.y = targetY + y * distScale - zomb.getY();
                    }
                }

                zomb.running = false;
                this.temp.normalize();
                if (zomb.crawling) {
                    if (zomb.getVariableString("TurnDirection").isEmpty()) {
                        zomb.setForwardDirection(this.temp);
                    }
                } else {
                    zomb.setDir(IsoDirections.fromAngle(this.temp));
                    zomb.setForwardDirection(this.temp);
                }

                if (!owner.isTurning() && owner.getPathFindBehavior2().walkingOnTheSpot.check(owner.getX(), owner.getY())) {
                    owner.setVariable("bMoving", false);
                }

                long tickCount = (Long)StateMachineParams.get(PARAM_TICK_COUNT);
                if (tickCount == 2L) {
                    zomb.parameterZombieState.setState(ParameterZombieState.State.Idle);
                }

                StateMachineParams.put(PARAM_TICK_COUNT, tickCount + 1L);
            }
        } else {
            zomb.allowRepathDelay = 0.0F;
            zomb.pathToLocation(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
            if (!zomb.getVariableBoolean("bPathfind")) {
                zomb.setVariable("bPathfind", true);
                zomb.setVariable("bMoving", false);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bMoving", false);
        ((IsoZombie)owner).networkAi.extraUpdate();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    /**
     * Return TRUE if the owner is currently moving.
     *  Defaults to FALSE
     */
    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return true;
    }

    private boolean isPathClear(IsoGameCharacter owner, float x, float y, float z) {
        int chunkX = PZMath.fastfloor(x) / 8;
        int chunkY = PZMath.fastfloor(y) / 8;
        IsoChunk chunk = GameServer.server
            ? ServerMap.instance.getChunk(chunkX, chunkY)
            : IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        if (chunk != null) {
            int flags = 1;
            flags |= 2;
            return !PolygonalMap2.instance
                .lineClearCollide(owner.getX(), owner.getY(), x, y, PZMath.fastfloor(z), owner.getPathFindBehavior2().getTargetChar(), flags);
        } else {
            return false;
        }
    }

    public boolean calculateTargetLocation(IsoZombie zomb, Vector2 location) {
        assert zomb.isCurrentState(this);

        HashMap<Object, Object> StateMachineParams = zomb.getStateMachineParams(this);
        float targetX = zomb.getPathFindBehavior2().getTargetX();
        float targetY = zomb.getPathFindBehavior2().getTargetY();
        location.x = targetX;
        location.y = targetY;
        this.temp.set(location);
        this.temp.x = this.temp.x - zomb.getX();
        this.temp.y = this.temp.y - zomb.getY();
        float dist = this.temp.getLength();
        if (dist < 0.025F) {
            return false;
        } else {
            if (!GameServer.server && !zomb.crawling && StateMachineParams.get(PARAM_IGNORE_OFFSET) == Boolean.FALSE) {
                float distScale = Math.min(dist / 2.0F, 4.0F);
                float x = (zomb.getID() + zomb.zombieId) % 20 / 10.0F - 1.0F;
                float y = (zomb.getID() + zomb.zombieId) % 20 / 10.0F - 1.0F;
                if (IsoUtils.DistanceTo(zomb.getX(), zomb.getY(), targetX + x * distScale, targetY + y * distScale) < dist) {
                    location.x = targetX + x * distScale;
                    location.y = targetY + y * distScale;
                    return true;
                }
            }

            return false;
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
