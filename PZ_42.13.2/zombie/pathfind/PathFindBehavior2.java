// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import java.util.Objects;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.ai.WalkingOnTheSpot;
import zombie.ai.astar.AStarPathFinder;
import zombie.ai.astar.Mover;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.CollideWithWallState;
import zombie.ai.states.LungeNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieGetDownState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.VehicleScript;
import zombie.seating.SeatingManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class PathFindBehavior2 implements IPathfinder {
    private static final Vector2 tempVector2 = new Vector2();
    private static final Vector2f tempVector2f = new Vector2f();
    private static final Vector2 tempVector2_2 = new Vector2();
    private static final Vector3f tempVector3f_1 = new Vector3f();
    private static final PathFindBehavior2.PointOnPath pointOnPath = new PathFindBehavior2.PointOnPath();
    public boolean pathNextIsSet;
    public float pathNextX;
    public float pathNextY;
    public ArrayList<IPathfinder> listeners = new ArrayList<>();
    public PathFindBehavior2.NPCData npcData = new PathFindBehavior2.NPCData();
    private final IsoGameCharacter chr;
    private float startX;
    private float startY;
    private float startZ;
    private float targetX;
    private float targetY;
    private float targetZ;
    private final TFloatArrayList targetXyz = new TFloatArrayList();
    private final Path path = new Path();
    private int pathIndex;
    private boolean isCancel = true;
    private boolean startedMoving;
    public boolean stopping;
    private boolean turningToObstacle;
    public final WalkingOnTheSpot walkingOnTheSpot = new WalkingOnTheSpot();
    private final ArrayList<PathFindBehavior2.DebugPt> actualPos = new ArrayList<>();
    private static final ObjectPool<PathFindBehavior2.DebugPt> actualPool = new ObjectPool<>(PathFindBehavior2.DebugPt::new);
    private PathFindBehavior2.Goal goal = PathFindBehavior2.Goal.None;
    private IsoGameCharacter goalCharacter;
    private IsoObject goalSitOnFurnitureObject;
    private boolean goalSitOnFurnitureAnySpriteGridObject;
    private BaseVehicle goalVehicle;
    private String goalVehicleArea;
    private int goalVehicleSeat;

    public PathFindBehavior2(IsoGameCharacter chr) {
        this.chr = chr;
    }

    public boolean isGoalNone() {
        return this.goal == PathFindBehavior2.Goal.None;
    }

    public boolean isGoalCharacter() {
        return this.goal == PathFindBehavior2.Goal.Character;
    }

    public boolean isGoalLocation() {
        return this.goal == PathFindBehavior2.Goal.Location;
    }

    public boolean isGoalSound() {
        return this.goal == PathFindBehavior2.Goal.Sound;
    }

    public boolean isGoalSitOnFurniture() {
        return this.goal == PathFindBehavior2.Goal.SitOnFurniture;
    }

    public IsoObject getGoalSitOnFurnitureObject() {
        return this.goalSitOnFurnitureObject;
    }

    public boolean isGoalVehicleAdjacent() {
        return this.goal == PathFindBehavior2.Goal.VehicleAdjacent;
    }

    public boolean isGoalVehicleArea() {
        return this.goal == PathFindBehavior2.Goal.VehicleArea;
    }

    public boolean isGoalVehicleSeat() {
        return this.goal == PathFindBehavior2.Goal.VehicleSeat;
    }

    public void reset() {
        this.startX = this.chr.getX();
        this.startY = this.chr.getY();
        this.startZ = this.chr.getZ();
        this.targetX = this.startX;
        this.targetY = this.startY;
        this.targetZ = this.startZ;
        this.targetXyz.resetQuick();
        this.pathIndex = 0;
        this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
        this.walkingOnTheSpot.reset(this.startX, this.startY);
    }

    public void pathToCharacter(IsoGameCharacter target) {
        this.isCancel = false;
        this.startedMoving = false;
        this.goal = PathFindBehavior2.Goal.Character;
        this.goalCharacter = target;
        if (target.getVehicle() != null) {
            Vector3f v = target.getVehicle().chooseBestAttackPosition(target, this.chr, tempVector3f_1);
            if (v != null) {
                this.setData(v.x, v.y, PZMath.fastfloor(target.getVehicle().getZ()));
                return;
            }

            this.setData(target.getVehicle().getX(), target.getVehicle().getY(), PZMath.fastfloor(target.getVehicle().getZ()));
            if (this.chr.DistToSquared(target.getVehicle()) < 100.0F) {
                if (this.chr instanceof IsoZombie zombie) {
                    zombie.allowRepathDelay = 100.0F;
                }

                this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
            }
        }

        if (target.isSittingOnFurniture()) {
            IsoDirections dir = target.getSitOnFurnitureDirection();
            int x = PZMath.fastfloor(target.getX());
            int y = PZMath.fastfloor(target.getY());
            int z = PZMath.fastfloor(target.getZ());
            float RADIUS = 0.3F;
            switch (dir) {
                case N:
                    this.setData(x + 0.5F, y - 0.3F, z);
                    break;
                case S:
                    this.setData(x + 0.5F, y + 1 + 0.3F, z);
                    break;
                case W:
                    this.setData(x - 0.3F, y + 0.5F, z);
                    break;
                case E:
                    this.setData(x + 1 + 0.3F, y + 0.5F, z);
                    break;
                default:
                    DebugLog.General.warn("unhandled sitting direction");
                    this.setData(target.getX(), target.getY(), target.getZ());
            }
        } else {
            this.setData(target.getX(), target.getY(), target.getZ());
        }
    }

    public void pathToLocation(int x, int y, int z) {
        this.isCancel = false;
        this.startedMoving = false;
        this.goal = PathFindBehavior2.Goal.Location;
        this.setData(x + 0.5F, y + 0.5F, z);
    }

    public void pathToLocationF(float x, float y, float z) {
        this.isCancel = false;
        this.startedMoving = false;
        this.goal = PathFindBehavior2.Goal.Location;
        this.setData(x, y, z);
    }

    public void pathToSound(int x, int y, int z) {
        this.isCancel = false;
        this.startedMoving = false;
        this.goal = PathFindBehavior2.Goal.Sound;
        this.setData(x + 0.5F, y + 0.5F, z);
    }

    public void pathToNearest(TFloatArrayList locations) {
        if (locations != null && !locations.isEmpty()) {
            if (locations.size() % 3 != 0) {
                throw new IllegalArgumentException("locations should be multiples of x,y,z");
            } else {
                this.isCancel = false;
                this.startedMoving = false;
                this.goal = PathFindBehavior2.Goal.Location;
                this.setData(locations.get(0), locations.get(1), locations.get(2));

                for (int i = 3; i < locations.size(); i += 3) {
                    this.targetXyz.add(locations.get(i));
                    this.targetXyz.add(locations.get(i + 1));
                    this.targetXyz.add(locations.get(i + 2));
                }
            }
        } else {
            throw new IllegalArgumentException("locations is null or empty");
        }
    }

    public void pathToNearestTable(KahluaTable locationsTable) {
        if (locationsTable != null && !locationsTable.isEmpty()) {
            if (locationsTable.len() % 3 != 0) {
                throw new IllegalArgumentException("locations table should be multiples of x,y,z");
            } else {
                TFloatArrayList locations = new TFloatArrayList(locationsTable.size());
                int i = 1;

                for (int len = locationsTable.len(); i <= len; i += 3) {
                    Double d1 = Type.tryCastTo(locationsTable.rawget(i), Double.class);
                    Double d2 = Type.tryCastTo(locationsTable.rawget(i + 1), Double.class);
                    Double d3 = Type.tryCastTo(locationsTable.rawget(i + 2), Double.class);
                    if (d1 == null || d2 == null || d3 == null) {
                        throw new IllegalArgumentException("locations table should be multiples of x,y,z");
                    }

                    locations.add(d1.floatValue());
                    locations.add(d2.floatValue());
                    locations.add(d3.floatValue());
                }

                this.pathToNearest(locations);
            }
        } else {
            throw new IllegalArgumentException("locations table is null or empty");
        }
    }

    public void pathToSitOnFurniture(IsoObject furniture, boolean bAnySpriteGridObject) {
        TFloatArrayList locations = new TFloatArrayList(12);
        ArrayList<IsoObject> objects = new ArrayList<>();
        if (bAnySpriteGridObject) {
            furniture.getSpriteGridObjectsExcludingSelf(objects);
        }

        objects.add(furniture);

        for (int i = 0; i < objects.size(); i++) {
            this.pathToSitOnFurnitureNoSpriteGrid(objects.get(i), locations);
        }

        if (locations.isEmpty()) {
            this.isCancel = false;
            this.startedMoving = false;
            this.goal = PathFindBehavior2.Goal.SitOnFurniture;
            this.goalSitOnFurnitureObject = furniture;
            this.goalSitOnFurnitureAnySpriteGridObject = bAnySpriteGridObject;
            this.setData(this.chr.getX(), this.chr.getY(), this.chr.getZ());
            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
        } else {
            this.isCancel = false;
            this.startedMoving = false;
            this.goal = PathFindBehavior2.Goal.SitOnFurniture;
            this.goalSitOnFurnitureObject = furniture;
            this.goalSitOnFurnitureAnySpriteGridObject = bAnySpriteGridObject;
            this.setData(locations.get(0), locations.get(1), locations.get(2));

            for (int i = 3; i < locations.size(); i += 3) {
                this.targetXyz.add(locations.get(i));
                this.targetXyz.add(locations.get(i + 1));
                this.targetXyz.add(locations.get(i + 2));
            }
        }
    }

    private void pathToSitOnFurnitureNoSpriteGrid(IsoObject furniture, TFloatArrayList locations) {
        Vector3f worldPos = new Vector3f();
        float RADIUS = 0.3F;
        String[] directions = new String[]{"N", "S", "W", "E"};
        String[] sides = new String[]{"Front", "Left", "Right"};

        for (String direction : directions) {
            for (String side : sides) {
                boolean bValid = SeatingManager.getInstance()
                    .getAdjacentPosition(this.chr, furniture, direction, side, "sitonfurniture", "SitOnFurniture" + side, worldPos);
                if (bValid) {
                    IsoGridSquare square = furniture.getSquare();
                    if ((square.isSolid() || square.isSolidTrans()) && this.isPointInSquare(worldPos.x, worldPos.y, square.getX(), square.getY())) {
                        float ox = worldPos.x;
                        float oy = worldPos.y;
                        if (direction == directions[0]) {
                            if (side == sides[0]) {
                                worldPos.y = square.getY() - 0.3F;
                            } else if (side == sides[1]) {
                                worldPos.x = square.getX() - 0.3F;
                            } else if (side == sides[2]) {
                                worldPos.x = square.getX() + 1 + 0.3F;
                            }
                        } else if (direction == directions[1]) {
                            if (side == sides[0]) {
                                worldPos.y = square.getY() + 1 + 0.3F;
                            } else if (side == sides[1]) {
                                worldPos.x = square.getX() + 1 + 0.3F;
                            } else if (side == sides[2]) {
                                worldPos.x = square.getX() - 0.3F;
                            }
                        } else if (direction == directions[2]) {
                            if (side == sides[0]) {
                                worldPos.x = square.getX() - 0.3F;
                            } else if (side == sides[1]) {
                                worldPos.y = square.getY() + 1 + 0.3F;
                            } else if (side == sides[2]) {
                                worldPos.y = square.getY() - 0.3F;
                            }
                        } else if (direction == directions[3]) {
                            if (side == sides[0]) {
                                worldPos.x = square.getX() + 1 + 0.3F;
                            } else if (side == sides[1]) {
                                worldPos.y = square.getY() - 0.3F;
                            } else if (side == sides[2]) {
                                worldPos.y = square.getY() + 1 + 0.3F;
                            }
                        }

                        LosUtil.TestResults testResults = LosUtil.lineClear(
                            IsoWorld.instance.currentCell,
                            PZMath.fastfloor(worldPos.x),
                            PZMath.fastfloor(worldPos.y),
                            PZMath.fastfloor(worldPos.z),
                            PZMath.fastfloor(ox),
                            PZMath.fastfloor(oy),
                            PZMath.fastfloor(worldPos.z),
                            false
                        );
                        if (testResults == LosUtil.TestResults.Blocked
                            || testResults == LosUtil.TestResults.ClearThroughClosedDoor
                            || testResults == LosUtil.TestResults.ClearThroughWindow) {
                            int dbg = 1;
                            continue;
                        }
                    } else if (!this.isPointInSquare(worldPos.x, worldPos.y, square.getX(), square.getY())) {
                        LosUtil.TestResults testResults = LosUtil.lineClear(
                            IsoWorld.instance.currentCell,
                            PZMath.fastfloor(worldPos.x),
                            PZMath.fastfloor(worldPos.y),
                            PZMath.fastfloor(worldPos.z),
                            square.getX(),
                            square.getY(),
                            square.getZ(),
                            false
                        );
                        if (testResults == LosUtil.TestResults.Blocked
                            || testResults == LosUtil.TestResults.ClearThroughClosedDoor
                            || testResults == LosUtil.TestResults.ClearThroughWindow) {
                            int dbg = 1;
                            continue;
                        }
                    }

                    if (!square.isSolid() && !square.isSolidTrans() && !this.chr.canStandAt(worldPos.x, worldPos.y, worldPos.z)) {
                        int dbg = 1;
                    } else {
                        locations.add(worldPos.x);
                        locations.add(worldPos.y);
                        locations.add(worldPos.z);
                    }
                }
            }
        }
    }

    private boolean isPointInSquare(float x, float y, int squareX, int squareY) {
        return x >= squareX && x < squareX + 1.0F && y >= squareY && y < squareY + 1;
    }

    private void fixSitOnFurniturePath(float targetX, float targetY) {
        if (this.goalSitOnFurnitureObject != null && this.goalSitOnFurnitureObject.getObjectIndex() != -1) {
            Vector3f closest = new Vector3f();
            float closestDistSq = Float.MAX_VALUE;
            ArrayList<IsoObject> objects = new ArrayList<>();
            if (this.goalSitOnFurnitureAnySpriteGridObject) {
                this.goalSitOnFurnitureObject.getSpriteGridObjectsExcludingSelf(objects);
            }

            objects.add(this.goalSitOnFurnitureObject);
            IsoObject closestObject = this.goalSitOnFurnitureObject;

            for (int i = 0; i < objects.size(); i++) {
                IsoObject object = objects.get(i);
                String[] directions = new String[]{"N", "S", "W", "E"};
                String[] sides = new String[]{"Front", "Left", "Right"};
                Vector3f worldPos = new Vector3f();

                for (String direction : directions) {
                    for (String side : sides) {
                        boolean bValid = SeatingManager.getInstance()
                            .getAdjacentPosition(this.chr, object, direction, side, "sitonfurniture", "SitOnFurniture" + side, worldPos);
                        if (bValid) {
                            float distSq = IsoUtils.DistanceToSquared(targetX, targetY, worldPos.x, worldPos.y);
                            if (distSq < closestDistSq) {
                                closest.set(worldPos);
                                closestDistSq = distSq;
                                closestObject = object;
                            }
                        }
                    }
                }
            }

            if (!(closestDistSq > 1.0F)) {
                this.goalSitOnFurnitureObject = closestObject;
                if (IsoUtils.DistanceToSquared(closest.x, closest.y, targetX, targetY) > 0.0025000002F) {
                    this.path.addNode(closest.x, closest.y, closest.z);
                    this.targetX = closest.x;
                    this.targetY = closest.y;
                }
            }
        }
    }

    public boolean shouldIgnoreCollisionWithSquare(IsoGridSquare square) {
        return this.goal == PathFindBehavior2.Goal.SitOnFurniture
            && this.goalSitOnFurnitureObject != null
            && this.goalSitOnFurnitureObject.getSquare() == square;
    }

    public void pathToVehicleAdjacent(BaseVehicle vehicle) {
        this.isCancel = false;
        this.startedMoving = false;
        this.goal = PathFindBehavior2.Goal.VehicleAdjacent;
        this.goalVehicle = vehicle;
        VehicleScript script = vehicle.getScript();
        Vector3f ext = script.getExtents();
        Vector3f com = script.getCenterOfMassOffset();
        float width = ext.x;
        float length = ext.z;
        float radius = 0.3F;
        float minX = com.x - width / 2.0F - 0.3F;
        float minY = com.z - length / 2.0F - 0.3F;
        float maxX = com.x + width / 2.0F + 0.3F;
        float maxY = com.z + length / 2.0F + 0.3F;
        TFloatArrayList locations = new TFloatArrayList();
        Vector3f v = vehicle.getWorldPos(minX, com.y, com.z, tempVector3f_1);
        if (PolygonalMap2.instance.canStandAt(v.x, v.y, PZMath.fastfloor(this.targetZ), vehicle, false, true)) {
            locations.add(v.x);
            locations.add(v.y);
            locations.add(this.targetZ);
        }

        v = vehicle.getWorldPos(maxX, com.y, com.z, tempVector3f_1);
        if (PolygonalMap2.instance.canStandAt(v.x, v.y, PZMath.fastfloor(this.targetZ), vehicle, false, true)) {
            locations.add(v.x);
            locations.add(v.y);
            locations.add(this.targetZ);
        }

        v = vehicle.getWorldPos(com.x, com.y, minY, tempVector3f_1);
        if (PolygonalMap2.instance.canStandAt(v.x, v.y, PZMath.fastfloor(this.targetZ), vehicle, false, true)) {
            locations.add(v.x);
            locations.add(v.y);
            locations.add(this.targetZ);
        }

        v = vehicle.getWorldPos(com.x, com.y, maxY, tempVector3f_1);
        if (PolygonalMap2.instance.canStandAt(v.x, v.y, PZMath.fastfloor(this.targetZ), vehicle, false, true)) {
            locations.add(v.x);
            locations.add(v.y);
            locations.add(this.targetZ);
        }

        this.setData(locations.get(0), locations.get(1), locations.get(2));

        for (int i = 3; i < locations.size(); i += 3) {
            this.targetXyz.add(locations.get(i));
            this.targetXyz.add(locations.get(i + 1));
            this.targetXyz.add(locations.get(i + 2));
        }
    }

    public void pathToVehicleArea(BaseVehicle vehicle, String areaId) {
        Vector2 areaCenter = vehicle.getAreaCenter(areaId);
        if (areaCenter == null) {
            this.targetX = this.chr.getX();
            this.targetY = this.chr.getY();
            this.targetZ = this.chr.getZ();
            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
        } else {
            this.isCancel = false;
            this.startedMoving = false;
            this.goal = PathFindBehavior2.Goal.VehicleArea;
            this.goalVehicle = vehicle;
            this.goalVehicleArea = areaId;
            this.setData(areaCenter.getX(), areaCenter.getY(), PZMath.fastfloor(vehicle.getZ()));
            if (this.chr instanceof IsoPlayer
                && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ)
                && !PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), this.targetX, this.targetY, PZMath.fastfloor(this.targetZ), null)
                )
             {
                this.path.clear();
                this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                this.path.addNode(this.targetX, this.targetY, this.targetZ);
                this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
            }
        }
    }

    public void pathToVehicleSeat(BaseVehicle vehicle, int seat) {
        VehicleScript.Position posn = vehicle.getPassengerPosition(seat, "outside2");
        if (posn != null) {
            Vector3f worldPos = BaseVehicle.TL_vector3f_pool.get().alloc();
            if (posn.area == null) {
                vehicle.getPassengerPositionWorldPos(posn, worldPos);
            } else {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                VehicleScript.Area area = vehicle.getScript().getAreaById(posn.area);
                Vector2 areaPos = vehicle.areaPositionWorld4PlayerInteract(area, vector2);
                worldPos.x = areaPos.x;
                worldPos.y = areaPos.y;
                worldPos.z = 0.0F;
                Vector2ObjectPool.get().release(vector2);
            }

            worldPos.sub(this.chr.getX(), this.chr.getY(), this.chr.getZ());
            if (worldPos.length() < 2.0F) {
                vehicle.getPassengerPositionWorldPos(posn, worldPos);
                this.setData(worldPos.x(), worldPos.y(), PZMath.fastfloor(worldPos.z()));
                if (this.chr instanceof IsoPlayer && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ)) {
                    BaseVehicle.TL_vector3f_pool.get().release(worldPos);
                    this.path.clear();
                    this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                    this.path.addNode(this.targetX, this.targetY, this.targetZ);
                    this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
                    return;
                }
            }

            BaseVehicle.TL_vector3f_pool.get().release(worldPos);
        }

        posn = vehicle.getPassengerPosition(seat, "outside");
        if (posn == null) {
            VehiclePart door = vehicle.getPassengerDoor(seat);
            if (door == null) {
                this.targetX = this.chr.getX();
                this.targetY = this.chr.getY();
                this.targetZ = this.chr.getZ();
                this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
            } else {
                this.pathToVehicleArea(vehicle, door.getArea());
            }
        } else {
            this.isCancel = false;
            this.startedMoving = false;
            this.goal = PathFindBehavior2.Goal.VehicleSeat;
            this.goalVehicle = vehicle;
            Vector3f worldPosx = BaseVehicle.TL_vector3f_pool.get().alloc();
            if (posn.area == null) {
                vehicle.getPassengerPositionWorldPos(posn, worldPosx);
            } else {
                Vector2 vector2 = Vector2ObjectPool.get().alloc();
                VehicleScript.Area area = vehicle.getScript().getAreaById(posn.area);
                Vector2 areaPos = vehicle.areaPositionWorld4PlayerInteract(area, vector2);
                worldPosx.x = areaPos.x;
                worldPosx.y = areaPos.y;
                worldPosx.z = PZMath.fastfloor(vehicle.jniTransform.origin.y / 2.44949F);
                Vector2ObjectPool.get().release(vector2);
            }

            this.setData(worldPosx.x(), worldPosx.y(), PZMath.fastfloor(worldPosx.z()));
            BaseVehicle.TL_vector3f_pool.get().release(worldPosx);
            if (this.chr instanceof IsoPlayer
                && PZMath.fastfloor(this.chr.getZ()) == PZMath.fastfloor(this.targetZ)
                && !PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), this.targetX, this.targetY, PZMath.fastfloor(this.targetZ), null)
                )
             {
                this.path.clear();
                this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                this.path.addNode(this.targetX, this.targetY, this.targetZ);
                this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
            }
        }
    }

    public void cancel() {
        this.isCancel = true;
    }

    public boolean getIsCancelled() {
        return this.isCancel;
    }

    public void setData(float targetX, float targetY, float targetZ) {
        this.startX = this.chr.getX();
        this.startY = this.chr.getY();
        this.startZ = this.chr.getZ();
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetXyz.resetQuick();
        this.pathIndex = 0;
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.cancelRequest(this.chr);
        } else {
            PolygonalMap2.instance.cancelRequest(this.chr);
        }

        this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notrunning;
        this.stopping = false;
        actualPool.release(this.actualPos);
        this.actualPos.clear();
    }

    public float getTargetX() {
        return this.targetX;
    }

    public float getTargetY() {
        return this.targetY;
    }

    public float getTargetZ() {
        return this.targetZ;
    }

    public float getPathLength() {
        if (this.path != null && !this.path.nodes.isEmpty()) {
            if (this.pathIndex + 1 >= this.path.nodes.size()) {
                return (float)Math.sqrt(
                    (this.chr.getX() - this.targetX) * (this.chr.getX() - this.targetX) + (this.chr.getY() - this.targetY) * (this.chr.getY() - this.targetY)
                );
            } else {
                float length = (float)Math.sqrt(
                    (this.chr.getX() - this.path.nodes.get(this.pathIndex + 1).x) * (this.chr.getX() - this.path.nodes.get(this.pathIndex + 1).x)
                        + (this.chr.getY() - this.path.nodes.get(this.pathIndex + 1).y) * (this.chr.getY() - this.path.nodes.get(this.pathIndex + 1).y)
                );

                for (int i = this.pathIndex + 2; i < this.path.nodes.size(); i++) {
                    length += (float)Math.sqrt(
                        (this.path.nodes.get(i - 1).x - this.path.nodes.get(i).x) * (this.path.nodes.get(i - 1).x - this.path.nodes.get(i).x)
                            + (this.path.nodes.get(i - 1).y - this.path.nodes.get(i).y) * (this.path.nodes.get(i - 1).y - this.path.nodes.get(i).y)
                    );
                }

                return length;
            }
        } else {
            return (float)Math.sqrt(
                (this.chr.getX() - this.targetX) * (this.chr.getX() - this.targetX) + (this.chr.getY() - this.targetY) * (this.chr.getY() - this.targetY)
            );
        }
    }

    public IsoGameCharacter getTargetChar() {
        return this.goal == PathFindBehavior2.Goal.Character ? this.goalCharacter : null;
    }

    public boolean isTargetLocation(float x, float y, float z) {
        return this.goal == PathFindBehavior2.Goal.Location && x == this.targetX && y == this.targetY && PZMath.fastfloor(z) == PZMath.fastfloor(this.targetZ);
    }

    public PathFindBehavior2.BehaviorResult update() {
        if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.notrunning) {
            if (PathfindNative.useNativeCode) {
                zombie.pathfind.nativeCode.PathFindRequest request = PathfindNative.instance
                    .addRequest(this, this.chr, this.startX, this.startY, this.startZ, this.targetX, this.targetY, this.targetZ);
                request.targetXyz.resetQuick();
                request.targetXyz.addAll(this.targetXyz);
            } else {
                PathFindRequest request = PolygonalMap2.instance
                    .addRequest(this, this.chr, this.startX, this.startY, this.startZ, this.targetX, this.targetY, this.targetZ);
                request.targetXyz.resetQuick();
                request.targetXyz.addAll(this.targetXyz);
            }

            this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.notyetfound;
            this.walkingOnTheSpot.reset(this.chr.getX(), this.chr.getY());
            this.updateWhileRunningPathfind();
            return PathFindBehavior2.BehaviorResult.Working;
        } else if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.notyetfound) {
            this.updateWhileRunningPathfind();
            return PathFindBehavior2.BehaviorResult.Working;
        } else if (this.chr.getFinder().progress == AStarPathFinder.PathFindProgress.failed) {
            return PathFindBehavior2.BehaviorResult.Failed;
        } else {
            State state = this.chr.getCurrentState();
            if (Core.debug && DebugOptions.instance.pathfindRenderPath.getValue() && this.chr instanceof IsoPlayer && !this.chr.isAnimal()) {
                while (this.actualPos.size() > 100) {
                    actualPool.release(this.actualPos.remove(0));
                }

                this.actualPos
                    .add(
                        actualPool.alloc()
                            .init(
                                this.chr.getX(),
                                this.chr.getY(),
                                this.chr.getZ(),
                                state == ClimbOverFenceState.instance() || state == ClimbThroughWindowState.instance()
                            )
                    );
            }

            if (state != ClimbOverFenceState.instance() && state != ClimbThroughWindowState.instance()) {
                if (this.chr.getVehicle() != null) {
                    return PathFindBehavior2.BehaviorResult.Failed;
                } else if (this.walkingOnTheSpot.check(this.chr)) {
                    return PathFindBehavior2.BehaviorResult.Failed;
                } else {
                    this.chr.setMoving(true);
                    this.chr.setPath2(this.path);
                    IsoZombie zombie = Type.tryCastTo(this.chr, IsoZombie.class);
                    if (this.goal == PathFindBehavior2.Goal.Character
                        && zombie != null
                        && this.goalCharacter != null
                        && this.goalCharacter.getVehicle() != null
                        && this.chr.DistToSquared(this.targetX, this.targetY) < 16.0F) {
                        Vector3f v = this.goalCharacter.getVehicle().chooseBestAttackPosition(this.goalCharacter, this.chr, tempVector3f_1);
                        if (v == null) {
                            return PathFindBehavior2.BehaviorResult.Failed;
                        }

                        if (Math.abs(v.x - this.targetX) > 0.1F || Math.abs(v.y - this.targetY) > 0.1F) {
                            if (Math.abs(this.goalCharacter.getVehicle().getCurrentSpeedKmHour()) > 0.8F) {
                                if (!PolygonalMap2.instance
                                    .lineClearCollide(this.chr.getX(), this.chr.getY(), v.x, v.y, PZMath.fastfloor(this.targetZ), this.goalCharacter)) {
                                    this.path.clear();
                                    this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                                    this.path.addNode(v.x, v.y, v.z);
                                } else if (IsoUtils.DistanceToSquared(v.x, v.y, this.targetX, this.targetY)
                                    > IsoUtils.DistanceToSquared(this.chr.getX(), this.chr.getY(), v.x, v.y)) {
                                    return PathFindBehavior2.BehaviorResult.Working;
                                }
                            } else if (zombie.allowRepathDelay <= 0.0F) {
                                zombie.allowRepathDelay = 6.25F;
                                if (PolygonalMap2.instance.lineClearCollide(this.chr.getX(), this.chr.getY(), v.x, v.y, PZMath.fastfloor(this.targetZ), null)) {
                                    this.setData(v.x, v.y, this.targetZ);
                                    return PathFindBehavior2.BehaviorResult.Working;
                                }

                                this.path.clear();
                                this.path.addNode(this.chr.getX(), this.chr.getY(), this.chr.getZ());
                                this.path.addNode(v.x, v.y, v.z);
                            }
                        }
                    }

                    closestPointOnPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), this.chr, this.path, pointOnPath);
                    this.pathIndex = pointOnPath.pathIndex;
                    if (this.pathIndex == this.path.nodes.size() - 2) {
                        PathNode node = this.path.nodes.get(this.path.nodes.size() - 1);
                        float distToEnd = IsoUtils.DistanceTo(this.chr.getX(), this.chr.getY(), node.x, node.y);
                        if (distToEnd <= 0.05F) {
                            this.chr.getDeferredMovement(tempVector2);
                            float lengthTest = 0.0F;
                            if (this.chr instanceof IsoAnimal isoAnimal) {
                                lengthTest = isoAnimal.adef.animalSize;
                            }

                            if (!(tempVector2.getLength() > lengthTest)) {
                                this.pathNextIsSet = false;
                                return PathFindBehavior2.BehaviorResult.Succeeded;
                            }

                            if (zombie != null || this.chr instanceof IsoPlayer) {
                                this.chr.setMoving(false);
                            }

                            tempVector2_2.set(node.x - this.chr.getX(), node.y - this.chr.getY());
                            tempVector2_2.setLength(PZMath.min(distToEnd, 0.005F * GameTime.getInstance().getMultiplier()));
                            this.chr.MoveUnmodded(tempVector2_2);
                            this.stopping = true;
                            return PathFindBehavior2.BehaviorResult.Working;
                        }

                        this.stopping = false;
                    } else if (this.pathIndex < this.path.nodes.size() - 2 && pointOnPath.dist > 0.999F) {
                        this.pathIndex++;
                    }

                    PathNode v1 = this.path.nodes.get(this.pathIndex);
                    PathNode v2 = this.path.nodes.get(this.pathIndex + 1);
                    this.pathNextX = v2.x;
                    this.pathNextY = v2.y;
                    this.pathNextIsSet = true;
                    Vector2 dir = tempVector2.set(this.pathNextX - this.chr.getX(), this.pathNextY - this.chr.getY());
                    dir.normalize();
                    this.chr.getDeferredMovement(tempVector2_2);
                    if (!GameServer.server && !this.chr.isAnimationUpdatingThisFrame()) {
                        tempVector2_2.set(0.0F, 0.0F);
                    }

                    float speed = tempVector2_2.getLength();
                    if (zombie != null) {
                        zombie.running = false;
                        if (SandboxOptions.instance.lore.speed.getValue() == 1) {
                            zombie.running = true;
                        }
                    }

                    float mult = 1.0F;
                    float dist = speed * 1.0F;
                    float distTo = IsoUtils.DistanceTo(this.pathNextX, this.pathNextY, this.chr.getX(), this.chr.getY());
                    if (dist >= distTo) {
                        speed *= distTo / dist;
                        this.pathIndex++;
                    }

                    if (zombie != null) {
                        this.checkCrawlingTransition(v1, v2, distTo);
                    }

                    if (zombie == null && distTo >= 0.5F) {
                        if (this.checkDoorHoppableWindow(
                            this.chr.getX() + dir.x * Math.max(0.5F, speed), this.chr.getY() + dir.y * Math.max(0.5F, speed), this.chr.getZ()
                        )) {
                            return PathFindBehavior2.BehaviorResult.Failed;
                        }

                        if (state != this.chr.getCurrentState()) {
                            return PathFindBehavior2.BehaviorResult.Working;
                        }
                    }

                    if (speed <= 0.0F) {
                        this.walkingOnTheSpot.reset(this.chr.getX(), this.chr.getY());
                        return PathFindBehavior2.BehaviorResult.Working;
                    } else {
                        if (this.shouldBeMoving()) {
                            tempVector2_2.set(dir);
                            tempVector2_2.setLength(speed);
                            this.chr.MoveUnmodded(tempVector2_2);
                            this.startedMoving = true;
                        }

                        if (this.isStrafing()) {
                            if ((
                                    this.goal == PathFindBehavior2.Goal.VehicleAdjacent
                                        || this.goal == PathFindBehavior2.Goal.VehicleArea
                                        || this.goal == PathFindBehavior2.Goal.VehicleSeat
                                )
                                && this.goalVehicle != null) {
                                this.chr.faceThisObject(this.goalVehicle);
                            }
                        } else if (!this.chr.isAiming()) {
                            if (this.isTurningToObstacle() && this.chr.shouldBeTurning()) {
                                boolean var21 = true;
                            } else if (this.chr.isAnimatingBackwards()) {
                                tempVector2.set(this.chr.getX() - this.pathNextX, this.chr.getY() - this.pathNextY);
                                if (tempVector2.getLengthSquared() > 0.0F) {
                                    this.chr.DirectionFromVector(tempVector2);
                                    tempVector2.normalize();
                                    this.chr.setForwardDirection(tempVector2.x, tempVector2.y);
                                    AnimationPlayer animationPlayer = this.chr.getAnimationPlayer();
                                    if (animationPlayer != null && animationPlayer.isReady()) {
                                        animationPlayer.updateForwardDirection(this.chr);
                                    }
                                }
                            } else {
                                this.chr.faceLocationF(this.pathNextX, this.pathNextY);
                            }
                        }

                        return PathFindBehavior2.BehaviorResult.Working;
                    }
                }
            } else {
                if (GameClient.client && this.chr instanceof IsoPlayer isoPlayer && !isoPlayer.isLocalPlayer()) {
                    this.chr.getDeferredMovement(tempVector2_2);
                    this.chr.MoveUnmodded(tempVector2_2);
                }

                return PathFindBehavior2.BehaviorResult.Working;
            }
        }
    }

    private void updateWhileRunningPathfind() {
        if (this.pathNextIsSet) {
            this.moveToPoint(this.pathNextX, this.pathNextY, 1.0F);
        }
    }

    public void moveToPoint(float x, float y, float speedMul) {
        if (!(this.chr instanceof IsoPlayer) || this.chr.getCurrentState() != CollideWithWallState.instance()) {
            IsoZombie zombie = Type.tryCastTo(this.chr, IsoZombie.class);
            Vector2 dir = tempVector2.set(x - this.chr.getX(), y - this.chr.getY());
            if (PZMath.fastfloor(x) != PZMath.fastfloor(this.chr.getX())
                || PZMath.fastfloor(y) != PZMath.fastfloor(this.chr.getY())
                || !(dir.getLength() <= 0.1F)) {
                dir.normalize();
                this.chr.getDeferredMovement(tempVector2_2);
                float speed = tempVector2_2.getLength();
                speed *= speedMul;
                boolean isRemoteZombieWithTarget = false;
                if (zombie != null) {
                    zombie.running = SandboxOptions.instance.lore.speed.getValue() == 1;
                    isRemoteZombieWithTarget = GameClient.client
                        && zombie.isRemoteZombie()
                        && zombie.getTarget() != null
                        && zombie.isCurrentState(LungeNetworkState.instance());
                }

                if (!(speed <= 0.0F)) {
                    tempVector2_2.set(dir);
                    tempVector2_2.setLength(speed);
                    this.chr.MoveUnmodded(tempVector2_2);
                    if (!isRemoteZombieWithTarget) {
                        this.chr.faceLocation(x - 0.5F, y - 0.5F);
                        this.chr.setForwardDirection(x - this.chr.getX(), y - this.chr.getY());
                        this.chr.getForwardDirection().normalize();
                    }
                }
            }
        }
    }

    public void moveToDir(IsoMovingObject target, float speedMul) {
        Vector2 dir = tempVector2.set(target.getX() - this.chr.getX(), target.getY() - this.chr.getY());
        if (!(dir.getLength() <= 0.1F)) {
            dir.normalize();
            this.chr.getDeferredMovement(tempVector2_2);
            float speed = tempVector2_2.getLength();
            speed *= speedMul;
            if (this.chr instanceof IsoZombie isoZombie) {
                isoZombie.running = false;
                if (SandboxOptions.instance.lore.speed.getValue() == 1) {
                    isoZombie.running = true;
                }
            }

            if (!(speed <= 0.0F)) {
                tempVector2_2.set(dir);
                tempVector2_2.setLength(speed);
                this.chr.MoveUnmodded(tempVector2_2);
                this.chr.faceLocation(target.getX() - 0.5F, target.getY() - 0.5F);
                this.chr.setForwardDirection(target.getX() - this.chr.getX(), target.getY() - this.chr.getY());
                this.chr.getForwardDirection().normalize();
            }
        }
    }

    private boolean checkDoorHoppableWindow(float nx, float ny, float z) {
        this.turningToObstacle = false;
        IsoGridSquare current = this.chr.getCurrentSquare();
        if (current == null) {
            return false;
        } else {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)nx, (double)ny, (double)z);
            if (square != null && square != current) {
                int dx = square.x - current.x;
                int dy = square.y - current.y;
                if (dx != 0 && dy != 0) {
                    return false;
                } else {
                    IsoObject object = this.chr.getCurrentSquare().getDoorTo(square);
                    if (object instanceof IsoDoor door) {
                        if (!door.open) {
                            if (this.chr instanceof IsoPlayer player && !player.isAnimal() && player.timeSinceCloseDoor < 50.0F) {
                                this.chr.setCollidable(false);
                            } else {
                                if (!door.couldBeOpen(this.chr)) {
                                    door.ToggleDoor(this.chr);
                                    return true;
                                }

                                this.chr.setCollidable(true);
                                door.ToggleDoor(this.chr);
                                if (!door.open) {
                                    return true;
                                }
                            }
                        }
                    } else if (object instanceof IsoThumpable doorx && doorx.isDoor() && !doorx.open) {
                        if (this.chr instanceof IsoPlayer player && !player.isAnimal() && player.timeSinceCloseDoor < 50.0F) {
                            this.chr.setCollidable(false);
                        } else {
                            if (!doorx.couldBeOpen(this.chr)) {
                                doorx.ToggleDoor(this.chr);
                                return true;
                            }

                            this.chr.setCollidable(true);
                            doorx.ToggleDoor(this.chr);
                            if (!doorx.open) {
                                return true;
                            }
                        }
                    }

                    IsoWindow window = current.getWindowTo(square);
                    if (window != null) {
                        if (window.canClimbThrough(this.chr) && (!window.isSmashed() || window.isGlassRemoved())) {
                            if (this.chr.isAiming()) {
                                return false;
                            } else {
                                this.chr.faceThisObject(window);
                                if (this.chr.shouldBeTurning()) {
                                    this.turningToObstacle = true;
                                    return false;
                                } else {
                                    this.chr.climbThroughWindow(window);
                                    return false;
                                }
                            }
                        } else {
                            return true;
                        }
                    } else {
                        IsoThumpable windowThumpable = current.getWindowThumpableTo(square);
                        if (windowThumpable == null) {
                            IsoWindowFrame windowFrame = current.getWindowFrameTo(square);
                            if (windowFrame != null) {
                                this.chr.climbThroughWindowFrame(windowFrame);
                                return false;
                            } else {
                                IsoDirections climbDir = IsoDirections.Max;
                                if (dx > 0 && square.has(IsoFlagType.HoppableW)) {
                                    climbDir = IsoDirections.E;
                                } else if (dx < 0 && current.has(IsoFlagType.HoppableW)) {
                                    climbDir = IsoDirections.W;
                                } else if (dy < 0 && current.has(IsoFlagType.HoppableN)) {
                                    climbDir = IsoDirections.N;
                                } else if (dy > 0 && square.has(IsoFlagType.HoppableN)) {
                                    climbDir = IsoDirections.S;
                                }

                                if (climbDir != IsoDirections.Max) {
                                    if (this.chr.isAiming()) {
                                        return false;
                                    }

                                    this.chr.faceDirection(climbDir);
                                    if (this.chr.shouldBeTurning()) {
                                        this.turningToObstacle = true;
                                        return false;
                                    }

                                    this.chr.climbOverFence(climbDir);
                                }

                                climbDir = IsoDirections.Max;
                                if (dx <= 0 || !square.has(IsoFlagType.TallHoppableW) && !square.has(IsoFlagType.WallW) && !square.has(IsoFlagType.WallWTrans)) {
                                    if (dx >= 0
                                        || !current.has(IsoFlagType.TallHoppableW) && !current.has(IsoFlagType.WallW) && !current.has(IsoFlagType.WallWTrans)) {
                                        if (dy >= 0
                                            || !current.has(IsoFlagType.TallHoppableN)
                                                && !current.has(IsoFlagType.WallN)
                                                && !current.has(IsoFlagType.WallNTrans)) {
                                            if (dy > 0
                                                && (
                                                    square.has(IsoFlagType.TallHoppableN)
                                                        || square.has(IsoFlagType.WallN)
                                                        || square.has(IsoFlagType.WallNTrans)
                                                )) {
                                                climbDir = IsoDirections.S;
                                            }
                                        } else {
                                            climbDir = IsoDirections.N;
                                        }
                                    } else {
                                        climbDir = IsoDirections.W;
                                    }
                                } else {
                                    climbDir = IsoDirections.E;
                                }

                                if (climbDir != IsoDirections.Max && this.chr instanceof IsoPlayer player) {
                                    player.climbOverWall(climbDir);
                                    return false;
                                } else {
                                    return false;
                                }
                            }
                        } else if (windowThumpable.isBarricaded()) {
                            return true;
                        } else if (this.chr.isAiming()) {
                            return false;
                        } else {
                            this.chr.faceThisObject(windowThumpable);
                            if (this.chr.shouldBeTurning()) {
                                this.turningToObstacle = true;
                                return false;
                            } else {
                                this.chr.climbThroughWindow(windowThumpable);
                                return false;
                            }
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    private void checkCrawlingTransition(PathNode v1, PathNode v2, float distTo) {
        IsoZombie zombie = (IsoZombie)this.chr;
        if (this.pathIndex < this.path.nodes.size() - 2) {
            v1 = this.path.nodes.get(this.pathIndex);
            v2 = this.path.nodes.get(this.pathIndex + 1);
            distTo = IsoUtils.DistanceTo(v2.x, v2.y, this.chr.getX(), this.chr.getY());
        }

        if (zombie.isCrawling()) {
            if (!zombie.isCanWalk()) {
                return;
            }

            if (zombie.isBeingSteppedOn()) {
            }

            if (zombie.getStateMachine().getPrevious() == ZombieGetDownState.instance() && ZombieGetDownState.instance().isNearStartXY(zombie)) {
                return;
            }

            this.advanceAlongPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), 0.5F, pointOnPath);
            if (!PolygonalMap2.instance.canStandAt(pointOnPath.x, pointOnPath.y, PZMath.fastfloor(zombie.getZ()), null, false, true)) {
                return;
            }

            if (!v2.hasFlag(1) && PolygonalMap2.instance.canStandAt(zombie.getX(), zombie.getY(), PZMath.fastfloor(zombie.getZ()), null, false, true)) {
                zombie.setVariable("ShouldStandUp", true);
            }
        } else {
            if (v1.hasFlag(1) && v2.hasFlag(1)) {
                zombie.setVariable("ShouldBeCrawling", true);
                ZombieGetDownState.instance().setParams(this.chr);
                return;
            }

            if (distTo < 0.4F && !v1.hasFlag(1) && v2.hasFlag(1)) {
                zombie.setVariable("ShouldBeCrawling", true);
                ZombieGetDownState.instance().setParams(this.chr);
            }
        }
    }

    public boolean shouldGetUpFromCrawl() {
        return this.chr.getVariableBoolean("ShouldStandUp");
    }

    public boolean shouldBeMoving() {
        return this.stopping ? false : !this.allowTurnAnimation() || !this.chr.shouldBeTurning();
    }

    public boolean hasStartedMoving() {
        return this.startedMoving;
    }

    public boolean allowTurnAnimation() {
        return !this.hasStartedMoving() || this.isTurningToObstacle();
    }

    public boolean isTurningToObstacle() {
        return this.turningToObstacle;
    }

    public boolean isStrafing() {
        if (this.chr.isZombie()) {
            return false;
        } else {
            return this.stopping
                ? false
                : this.path.nodes.size() == 2
                    && IsoUtils.DistanceToSquared(this.startX, this.startY, this.startZ * 3.0F, this.targetX, this.targetY, this.targetZ * 3.0F) < 0.25F;
        }
    }

    public static void closestPointOnPath(float x3, float y3, float z, IsoMovingObject mover, Path path, PathFindBehavior2.PointOnPath pop) {
        IsoCell cell = IsoWorld.instance.currentCell;
        pop.pathIndex = 0;
        float closestDist = Float.MAX_VALUE;

        for (int i = 0; i < path.nodes.size() - 1; i++) {
            PathNode node1 = path.nodes.get(i);
            PathNode node2 = path.nodes.get(i + 1);
            if (PZMath.fastfloor(node1.z) == PZMath.fastfloor(z) || PZMath.fastfloor(node2.z) == PZMath.fastfloor(z)) {
                float x1 = node1.x;
                float y1 = node1.y;
                float x2 = node2.x;
                float y2 = node2.y;
                double u = ((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
                double xu = x1 + u * (x2 - x1);
                double yu = y1 + u * (y2 - y1);
                if (u <= 0.0) {
                    xu = x1;
                    yu = y1;
                    u = 0.0;
                } else if (u >= 1.0) {
                    xu = x2;
                    yu = y2;
                    u = 1.0;
                }

                int dx = PZMath.fastfloor(xu) - PZMath.fastfloor(x3);
                int dy = PZMath.fastfloor(yu) - PZMath.fastfloor(y3);
                if ((dx != 0 || dy != 0) && Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                    IsoGridSquare square1 = cell.getGridSquare(PZMath.fastfloor(x3), PZMath.fastfloor(y3), PZMath.fastfloor(z));
                    IsoGridSquare square2 = cell.getGridSquare(PZMath.fastfloor(xu), PZMath.fastfloor(yu), PZMath.fastfloor(z));
                    if (mover instanceof IsoZombie isoZombie) {
                        boolean Ghost = isoZombie.ghost;
                        isoZombie.ghost = true;

                        try {
                            if (square1 != null && square2 != null && square1.testCollideAdjacent(mover, dx, dy, 0)) {
                                continue;
                            }
                        } finally {
                            isoZombie.ghost = Ghost;
                        }
                    } else if (square1 != null && square2 != null && square1.testCollideAdjacent(mover, dx, dy, 0)) {
                        continue;
                    }
                }

                float closestZ = z;
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                    IsoGridSquare square1 = cell.getGridSquare(PZMath.fastfloor(node1.x), PZMath.fastfloor(node1.y), PZMath.fastfloor(node1.z));
                    IsoGridSquare square2 = cell.getGridSquare(PZMath.fastfloor(node2.x), PZMath.fastfloor(node2.y), PZMath.fastfloor(node2.z));
                    float z1 = square1 == null ? node1.z : PolygonalMap2.instance.getApparentZ(square1);
                    float z2 = square2 == null ? node2.z : PolygonalMap2.instance.getApparentZ(square2);
                    closestZ = z1 + (z2 - z1) * (float)u;
                }

                float dist = IsoUtils.DistanceToSquared(x3, y3, z, (float)xu, (float)yu, closestZ);
                if (dist < closestDist) {
                    closestDist = dist;
                    pop.pathIndex = i;
                    pop.dist = u == 1.0 ? 1.0F : (float)u;
                    pop.x = (float)xu;
                    pop.y = (float)yu;
                }
            }
        }
    }

    void advanceAlongPath(float x, float y, float z, float dist, PathFindBehavior2.PointOnPath pop) {
        closestPointOnPath(x, y, z, this.chr, this.path, pop);

        for (int i = pop.pathIndex; i < this.path.nodes.size() - 1; i++) {
            PathNode node1 = this.path.nodes.get(i);
            PathNode node2 = this.path.nodes.get(i + 1);
            double dist2 = IsoUtils.DistanceTo2D(x, y, node2.x, node2.y);
            if (!(dist > dist2)) {
                pop.pathIndex = i;
                pop.dist = pop.dist + dist / IsoUtils.DistanceTo2D(node1.x, node1.y, node2.x, node2.y);
                pop.x = node1.x + pop.dist * (node2.x - node1.x);
                pop.y = node1.y + pop.dist * (node2.y - node1.y);
                return;
            }

            x = node2.x;
            y = node2.y;
            dist = (float)(dist - dist2);
            pop.dist = 0.0F;
        }

        pop.pathIndex = this.path.nodes.size() - 1;
        pop.dist = 1.0F;
        pop.x = this.path.nodes.get(pop.pathIndex).x;
        pop.y = this.path.nodes.get(pop.pathIndex).y;
    }

    public void render() {
        if (this.chr.getCurrentState() == WalkTowardState.instance()) {
            WalkTowardState.instance().calculateTargetLocation((IsoZombie)this.chr, tempVector2);
            tempVector2.x = tempVector2.x - this.chr.getX();
            tempVector2.y = tempVector2.y - this.chr.getY();
            tempVector2.setLength(Math.min(100.0F, tempVector2.getLength()));
            LineDrawer.addLine(
                this.chr.getX(),
                this.chr.getY(),
                this.chr.getZ(),
                this.chr.getX() + tempVector2.x,
                this.chr.getY() + tempVector2.y,
                this.targetZ,
                1.0F,
                1.0F,
                1.0F,
                null,
                true
            );
        } else if (this.chr.getPath2() != null) {
            for (int i = 0; i < this.path.nodes.size() - 1; i++) {
                PathNode v1 = this.path.nodes.get(i);
                PathNode v2 = this.path.nodes.get(i + 1);
                float r = 1.0F;
                float g = 1.0F;
                if (PZMath.fastfloor(v1.z) != PZMath.fastfloor(v2.z)) {
                    g = 0.0F;
                }

                LineDrawer.addLine(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z, 1.0F, g, 0.0F, null, true);
            }

            for (int i = 0; i < this.path.nodes.size(); i++) {
                PathNode v1 = this.path.nodes.get(i);
                float r = 1.0F;
                float g = 1.0F;
                float b = 0.0F;
                if (i == 0) {
                    r = 0.0F;
                    b = 1.0F;
                }

                LineDrawer.addLine(v1.x - 0.05F, v1.y - 0.05F, v1.z, v1.x + 0.05F, v1.y + 0.05F, v1.z, r, 1.0F, b, null, false);
            }

            closestPointOnPath(this.chr.getX(), this.chr.getY(), this.chr.getZ(), this.chr, this.path, pointOnPath);
            LineDrawer.addLine(
                pointOnPath.x - 0.05F,
                pointOnPath.y - 0.05F,
                this.chr.getZ(),
                pointOnPath.x + 0.05F,
                pointOnPath.y + 0.05F,
                this.chr.getZ(),
                0.0F,
                1.0F,
                0.0F,
                null,
                false
            );

            for (int i = 0; i < this.actualPos.size() - 1; i++) {
                PathFindBehavior2.DebugPt v0 = this.actualPos.get(i);
                PathFindBehavior2.DebugPt v1 = this.actualPos.get(i + 1);
                LineDrawer.addLine(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, 1.0F, 1.0F, 1.0F, null, true);
                LineDrawer.addLine(v0.x - 0.05F, v0.y - 0.05F, v0.z, v0.x + 0.05F, v0.y + 0.05F, v0.z, 1.0F, v0.climbing ? 1.0F : 0.0F, 0.0F, null, false);
            }
        }
    }

    @Override
    public void Succeeded(Path path, Mover mover) {
        this.path.copyFrom(path);
        if (!this.isCancel) {
            this.chr.setPath2(this.path);
        }

        if (!path.isEmpty()) {
            PathNode node = path.nodes.get(path.nodes.size() - 1);
            this.targetX = node.x;
            this.targetY = node.y;
            this.targetZ = node.z;
            if (this.isGoalSitOnFurniture()) {
                this.fixSitOnFurniturePath(this.targetX, this.targetY);
            }
        }

        this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.found;
    }

    @Override
    public void Failed(Mover mover) {
        this.chr.getFinder().progress = AStarPathFinder.PathFindProgress.failed;
    }

    public boolean isMovingUsingPathFind() {
        return !this.stopping && !this.isGoalNone() && !this.isCancel;
    }

    @UsedFromLua
    public static enum BehaviorResult {
        Working,
        Failed,
        Succeeded;
    }

    private static final class DebugPt {
        float x;
        float y;
        float z;
        boolean climbing;

        PathFindBehavior2.DebugPt init(float x, float y, float z, boolean climbing) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.climbing = climbing;
            return this;
        }
    }

    public static enum Goal {
        None,
        Character,
        Location,
        Sound,
        VehicleAdjacent,
        VehicleArea,
        VehicleSeat,
        SitOnFurniture;

        public static PathFindBehavior2.Goal fromByte(Byte goal) {
            return goal >= 0 && goal <= values().length ? values()[goal] : None;
        }
    }

    public class NPCData {
        public boolean doDirectMovement;
        public int maxSteps;
        public int nextTileX;
        public int nextTileY;
        public int nextTileZ;

        public NPCData() {
            Objects.requireNonNull(PathFindBehavior2.this);
            super();
        }
    }

    public static final class PointOnPath {
        int pathIndex;
        float dist;
        float x;
        float y;
    }
}
