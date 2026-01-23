// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.VirtualZombieManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.VehiclePoly;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.VehicleScript;
import zombie.util.Type;

public final class SurroundVehicle {
    private static final ObjectPool<SurroundVehicle.Position> s_positionPool = new ObjectPool<>(SurroundVehicle.Position::new);
    private static final Vector3f s_tempVector3f = new Vector3f();
    private final BaseVehicle vehicle;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public float x3;
    public float y3;
    public float x4;
    public float y4;
    private float x1p;
    private float y1p;
    private float x2p;
    private float y2p;
    private float x3p;
    private float y3p;
    private float x4p;
    private float y4p;
    private boolean moved;
    private final ArrayList<SurroundVehicle.Position> positions = new ArrayList<>();
    private long updateMs;

    public SurroundVehicle(BaseVehicle vehicle) {
        Objects.requireNonNull(vehicle);
        this.vehicle = vehicle;
    }

    private void calcPositionsLocal() {
        s_positionPool.release(this.positions);
        this.positions.clear();
        VehicleScript script = this.vehicle.getScript();
        if (script != null) {
            Vector3f ext = script.getExtents();
            Vector3f com = script.getCenterOfMassOffset();
            float width = ext.x;
            float length = ext.z;
            float CAN_STAND_AT_FUDGE = 0.005F;
            float radius = 0.155F;
            float minX = com.x - width / 2.0F - 0.155F;
            float minY = com.z - length / 2.0F - 0.155F;
            float maxX = com.x + width / 2.0F + 0.155F;
            float maxY = com.z + length / 2.0F + 0.155F;
            this.addPositions(minX, com.z - length / 2.0F, minX, com.z + length / 2.0F, SurroundVehicle.PositionSide.Right);
            this.addPositions(maxX, com.z - length / 2.0F, maxX, com.z + length / 2.0F, SurroundVehicle.PositionSide.Left);
            this.addPositions(minX, minY, maxX, minY, SurroundVehicle.PositionSide.Rear);
            this.addPositions(minX, maxY, maxX, maxY, SurroundVehicle.PositionSide.Front);
        }
    }

    private void addPositions(float x1, float y1, float x2, float y2, SurroundVehicle.PositionSide side) {
        Vector3f passengerPos = this.vehicle.getPassengerLocalPos(0, s_tempVector3f);
        if (passengerPos != null) {
            float RADIUS = 0.3F;
            if (side != SurroundVehicle.PositionSide.Left && side != SurroundVehicle.PositionSide.Right) {
                float targetX = 0.0F;
                float targetY = y1;

                for (float x = 0.0F; x >= x1 + 0.3F; x -= 0.6F) {
                    this.addPosition(x, targetY, side);
                }

                for (float x = 0.6F; x < x2 - 0.3F; x += 0.6F) {
                    this.addPosition(x, targetY, side);
                }
            } else {
                float targetX = x1;
                float targetY = passengerPos.z;

                for (float y = targetY; y >= y1 + 0.3F; y -= 0.6F) {
                    this.addPosition(targetX, y, side);
                }

                for (float y = targetY + 0.6F; y < y2 - 0.3F; y += 0.6F) {
                    this.addPosition(targetX, y, side);
                }
            }
        }
    }

    private SurroundVehicle.Position addPosition(float localX, float localY, SurroundVehicle.PositionSide side) {
        SurroundVehicle.Position position = s_positionPool.alloc();
        position.posLocal.set(localX, localY);
        position.side = side;
        this.positions.add(position);
        return position;
    }

    private void calcPositionsWorld() {
        for (int i = 0; i < this.positions.size(); i++) {
            SurroundVehicle.Position position = this.positions.get(i);
            this.vehicle.getWorldPos(position.posLocal.x, 0.0F, position.posLocal.y, position.posWorld);
            switch (position.side) {
                case Front:
                case Rear:
                    this.vehicle.getWorldPos(position.posLocal.x, 0.0F, 0.0F, position.posAxis);
                    break;
                case Left:
                case Right:
                    this.vehicle.getWorldPos(0.0F, 0.0F, position.posLocal.y, position.posAxis);
            }
        }

        VehiclePoly poly = this.vehicle.getPoly();
        this.x1p = poly.x1;
        this.x2p = poly.x2;
        this.x3p = poly.x3;
        this.x4p = poly.x4;
        this.y1p = poly.y1;
        this.y2p = poly.y2;
        this.y3p = poly.y3;
        this.y4p = poly.y4;
    }

    private SurroundVehicle.Position getClosestPositionFor(IsoZombie zombie) {
        if (zombie != null && zombie.getTarget() != null) {
            float closestDist = Float.MAX_VALUE;
            SurroundVehicle.Position closestPosition = null;

            for (int i = 0; i < this.positions.size(); i++) {
                SurroundVehicle.Position position = this.positions.get(i);
                if (!position.blocked) {
                    float moverDist = IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), position.posWorld.x, position.posWorld.y);
                    if (position.isOccupied()) {
                        float occupierDist = IsoUtils.DistanceToSquared(
                            position.zombie.getX(), position.zombie.getY(), position.posWorld.x, position.posWorld.y
                        );
                        if (occupierDist < moverDist) {
                            continue;
                        }
                    }

                    float dist = IsoUtils.DistanceToSquared(zombie.getTarget().getX(), zombie.getTarget().getY(), position.posWorld.x, position.posWorld.y);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestPosition = position;
                    }
                }
            }

            return closestPosition;
        } else {
            return null;
        }
    }

    public Vector2f getPositionForZombie(IsoZombie zombie, Vector2f out) {
        if ((!zombie.isOnFloor() || zombie.isCanWalk()) && PZMath.fastfloor(zombie.getZ()) == PZMath.fastfloor(this.vehicle.getZ())) {
            float distToVehicle = IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), this.vehicle.getX(), this.vehicle.getY());
            if (distToVehicle > 100.0F) {
                return out.set(this.vehicle.getX(), this.vehicle.getY());
            } else {
                if (this.checkPosition()) {
                    this.moved = true;
                }

                for (int i = 0; i < this.positions.size(); i++) {
                    SurroundVehicle.Position position = this.positions.get(i);
                    if (position.blocked) {
                        position.zombie = null;
                    }

                    if (position.zombie == zombie) {
                        return out.set(position.posWorld.x, position.posWorld.y);
                    }
                }

                SurroundVehicle.Position positionx = this.getClosestPositionFor(zombie);
                if (positionx == null) {
                    return null;
                } else {
                    positionx.zombie = zombie;
                    positionx.targetX = zombie.getTarget().getX();
                    positionx.targetY = zombie.getTarget().getY();
                    return out.set(positionx.posWorld.x, positionx.posWorld.y);
                }
            }
        } else {
            return out.set(this.vehicle.getX(), this.vehicle.getY());
        }
    }

    private boolean checkPosition() {
        if (this.vehicle.getScript() == null) {
            return false;
        } else {
            if (this.positions.isEmpty()) {
                this.calcPositionsLocal();
                this.x1 = -1.0F;
            }

            VehiclePoly poly = this.vehicle.getPoly();
            if (this.x1 == poly.x1
                && this.x2 == poly.x2
                && this.x3 == poly.x3
                && this.x4 == poly.x4
                && this.y1 == poly.y1
                && this.y2 == poly.y2
                && this.y3 == poly.y3
                && this.y4 == poly.y4) {
                return false;
            } else {
                this.x1 = poly.x1;
                this.x2 = poly.x2;
                this.x3 = poly.x3;
                this.x4 = poly.x4;
                this.y1 = poly.y1;
                this.y2 = poly.y2;
                this.y3 = poly.y3;
                this.y4 = poly.y4;
                this.calcPositionsWorld();
                return true;
            }
        }
    }

    private boolean movedSincePositionsWereCalculated() {
        VehiclePoly poly = this.vehicle.getPoly();
        return this.x1p != poly.x1
            || this.x2p != poly.x2
            || this.x3p != poly.x3
            || this.x4p != poly.x4
            || this.y1p != poly.y1
            || this.y2p != poly.y2
            || this.y3p != poly.y3
            || this.y4p != poly.y4;
    }

    private boolean hasOccupiedPositions() {
        for (int i = 0; i < this.positions.size(); i++) {
            SurroundVehicle.Position position = this.positions.get(i);
            if (position.zombie != null) {
                return true;
            }
        }

        return false;
    }

    public void update() {
        if (this.hasOccupiedPositions() && this.checkPosition()) {
            this.moved = true;
        }

        long now = System.currentTimeMillis();
        if (now - this.updateMs >= 1000L) {
            this.updateMs = now;
            if (this.moved) {
                this.moved = false;

                for (int i = 0; i < this.positions.size(); i++) {
                    SurroundVehicle.Position position = this.positions.get(i);
                    position.zombie = null;
                }
            }

            boolean bMovedSincePositionWereCalculated = this.movedSincePositionsWereCalculated();

            for (int i = 0; i < this.positions.size(); i++) {
                SurroundVehicle.Position position = this.positions.get(i);
                if (!bMovedSincePositionWereCalculated) {
                    position.checkBlocked(this.vehicle);
                }

                if (position.zombie != null) {
                    float distToVehicle = IsoUtils.DistanceToSquared(position.zombie.getX(), position.zombie.getY(), this.vehicle.getX(), this.vehicle.getY());
                    if (distToVehicle > 100.0F) {
                        position.zombie = null;
                    } else {
                        IsoGameCharacter target = Type.tryCastTo(position.zombie.getTarget(), IsoGameCharacter.class);
                        if (!position.zombie.isDead()
                            && !VirtualZombieManager.instance.isReused(position.zombie)
                            && !position.zombie.isOnFloor()
                            && target != null
                            && this.vehicle.getSeat(target) != -1) {
                            if (IsoUtils.DistanceToSquared(position.targetX, position.targetY, target.getX(), target.getY()) > 0.1F) {
                                position.zombie = null;
                            }
                        } else {
                            position.zombie = null;
                        }
                    }
                }
            }
        }
    }

    public void render() {
        if (this.hasOccupiedPositions()) {
            for (int i = 0; i < this.positions.size(); i++) {
                SurroundVehicle.Position position = this.positions.get(i);
                Vector3f v = position.posWorld;
                float r = 1.0F;
                float g = 1.0F;
                float b = 1.0F;
                if (position.isOccupied()) {
                    b = 0.0F;
                    r = 0.0F;
                } else if (position.blocked) {
                    b = 0.0F;
                    g = 0.0F;
                }

                this.vehicle.getController().drawCircle(v.x, v.y, 0.3F, r, g, b, 1.0F);
            }
        }
    }

    public void reset() {
        s_positionPool.release(this.positions);
        this.positions.clear();
    }

    private static final class Position {
        final Vector2f posLocal = new Vector2f();
        final Vector3f posWorld = new Vector3f();
        final Vector3f posAxis = new Vector3f();
        SurroundVehicle.PositionSide side;
        IsoZombie zombie;
        float targetX;
        float targetY;
        boolean blocked;

        boolean isOccupied() {
            return this.zombie != null;
        }

        void checkBlocked(BaseVehicle vehicle) {
            this.blocked = PolygonalMap2.instance
                .lineClearCollide(this.posWorld.x, this.posWorld.y, this.posAxis.x, this.posAxis.y, PZMath.fastfloor(vehicle.getZ()), vehicle);
            if (!this.blocked) {
                this.blocked = !PolygonalMap2.instance.canStandAt(this.posWorld.x, this.posWorld.y, PZMath.fastfloor(vehicle.getZ()), vehicle, false, false);
            }
        }
    }

    private static enum PositionSide {
        Front,
        Rear,
        Left,
        Right;
    }
}
