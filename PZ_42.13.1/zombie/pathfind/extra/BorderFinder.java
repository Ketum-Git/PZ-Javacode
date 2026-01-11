// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.extra;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.worldgen.utils.SquareCoord;

public class BorderFinder {
    private final IsoWorld world;
    private final Position player;
    private final int maxRange;
    private final List<SquareCoord> found = new ArrayList<>();
    private final List<Queue<Position>> parsed = new ArrayList<>();
    private final Map<SquareCoord, Position> path = new HashMap<>();
    private SquareCoord coordinates;
    private int maxDistance;
    private boolean running = true;
    private int tick;

    public BorderFinder(int x, int y, int z, int maxRange, IsoWorld world) {
        this.coordinates = new SquareCoord(x, y, z);
        this.world = world;
        this.player = new Position(this.coordinates, Direction.NORTH, 0, 0, new EnumMap<>(Direction.class));
        this.maxRange = maxRange;
    }

    public void run() {
        this.found.clear();
        this.path.clear();
        this.parsed.clear();
        this.parsed.add(new PriorityQueue<>((p1, p2) -> this.player.manhattan(p2.coords()) - this.player.manhattan(p1.coords())));

        for (int i = 1; i < 9; i++) {
            this.parsed.add(new LinkedList<>());
        }

        while (this.running) {
            this.oneStep();
            this.tick++;
        }

        if (DebugOptions.instance.pathfindBorderFinder.getValue()) {
            BorderFinderRenderer.instance.addAllPath(this.path.values());
        }
    }

    private BorderStatus hasWall(IsoGridSquare square, Direction direction) {
        if (direction != Direction.NORTH || !square.getProperties().has(IsoFlagType.collideN) && !square.getProperties().has(IsoFlagType.doorN)) {
            if (direction != Direction.WEST || !square.getProperties().has(IsoFlagType.collideW) && !square.getProperties().has(IsoFlagType.doorW)) {
                SquareCoord c = Direction.move(square.getCoords(), direction);
                if (direction == Direction.SOUTH) {
                    IsoGridSquare s = this.world.currentCell.getGridSquare(c.x(), c.y(), c.z());
                    if (s == null || s.getProperties().has(IsoFlagType.collideN) || s.getProperties().has(IsoFlagType.doorN)) {
                        return BorderStatus.WALL;
                    }
                }

                if (direction == Direction.EAST) {
                    IsoGridSquare s = this.world.currentCell.getGridSquare(c.x(), c.y(), c.z());
                    if (s == null || s.getProperties().has(IsoFlagType.collideW) || s.getProperties().has(IsoFlagType.doorW)) {
                        return BorderStatus.WALL;
                    }
                }

                if (c.x() - this.player.coords().x() <= this.maxRange
                    && c.x() - this.player.coords().x() >= -this.maxRange
                    && c.y() - this.player.coords().y() <= this.maxRange
                    && c.y() - this.player.coords().y() >= -this.maxRange) {
                    return square.getCollideMatrix(direction.x(), direction.y(), direction.z()) ? BorderStatus.WALL : BorderStatus.OPEN;
                } else {
                    return BorderStatus.OUT_OF_RANGE;
                }
            } else {
                return BorderStatus.WALL;
            }
        } else {
            return BorderStatus.WALL;
        }
    }

    private void oneStep() {
        IsoGridSquare square = this.world.currentCell.getGridSquare(this.coordinates.x(), this.coordinates.y(), this.coordinates.z());
        if (square != null) {
            for (Direction dir : Direction.values()) {
                SquareCoord coords = Direction.move(this.coordinates, dir);
                IsoGridSquare innerSquare = this.world.currentCell.getGridSquare(coords.x(), coords.y(), coords.z());
                if (!this.found.contains(coords) && this.hasWall(square, dir) == BorderStatus.OPEN && innerSquare != null) {
                    EnumMap<Direction, BorderStatus> walls = new EnumMap<>(Direction.class);
                    int nwalls = 0;

                    for (Direction innerDir : Direction.values()) {
                        BorderStatus status = this.hasWall(innerSquare, innerDir);
                        walls.put(innerDir, status);
                        nwalls += status != BorderStatus.OPEN ? 1 : 0;
                    }

                    this.parsed.get(nwalls).offer(new Position(coords, dir, this.player.manhattan(coords), this.tick, walls));
                    this.found.add(coords);
                }
            }

            for (int walls = 8; walls >= 0; walls--) {
                Queue<Position> queue = this.parsed.get(walls);
                if (!queue.isEmpty()) {
                    Position position = queue.poll();
                    if (walls == 0 && position.distance() < this.maxDistance) {
                        this.running = false;
                    }

                    this.maxDistance = PZMath.max(this.maxDistance, position.distance());
                    this.coordinates = position.coords();
                    this.path.put(position.coords(), position);
                    break;
                }
            }
        }
    }

    public Map<SquareCoord, Position> getPath() {
        return this.path;
    }
}
