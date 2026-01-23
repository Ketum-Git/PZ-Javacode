// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.utils.BooleanGrid;
import zombie.pathfind.Chunk;
import zombie.pathfind.MoverType;
import zombie.pathfind.PMMover;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.VehicleRect;
import zombie.pathfind.VisibilityGraph;

public final class FloodFill {
    static final int CPW = 8;
    static final PMMover mover = new PMMover();
    final ArrayDeque<Square> stack = new ArrayDeque<>();
    boolean thumpable;
    private final BooleanGrid visited = new BooleanGrid(8, 8);
    final ArrayList<Square> choices = new ArrayList<>(64);
    HLChunkRegion region;
    Square[][] squares;
    int minX;
    int minY;
    final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList<>();

    void calculate(HLChunkRegion region, Square[][] squares, Square square) {
        this.region = region;
        this.squares = squares;
        this.thumpable = square.has(131072);
        this.minX = this.region.getChunk().getMinX();
        this.minY = this.region.getChunk().getMinY();
        this.initVisibilityGraphs();
        this.push(square.getX(), square.getY());

        while ((square = this.pop()) != null) {
            int x = square.getX();
            int y1 = square.getY();

            while (this.shouldVisit(x, y1, x, y1 - 1)) {
                y1--;
            }

            boolean spanLeft = false;
            boolean spanRight = false;

            do {
                this.visited.setValue(this.gridX(x), this.gridY(y1), true);
                Square sq2 = this.squares[this.gridX(x)][this.gridY(y1)];
                if (sq2 != null) {
                    this.choices.add(sq2);
                }

                if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1)) {
                    this.push(x - 1, y1);
                    spanLeft = true;
                } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1)) {
                    spanLeft = false;
                } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1)) {
                    this.push(x - 1, y1);
                }

                if (!spanRight && this.shouldVisit(x, y1, x + 1, y1)) {
                    this.push(x + 1, y1);
                    spanRight = true;
                } else if (spanRight && !this.shouldVisit(x, y1, x + 1, y1)) {
                    spanRight = false;
                } else if (spanRight && !this.shouldVisit(x + 1, y1, x + 1, y1 - 1)) {
                    this.push(x + 1, y1);
                }

                y1++;
            } while (this.shouldVisit(x, y1 - 1, x, y1));
        }

        this.region.minX = Integer.MAX_VALUE;
        this.region.minY = Integer.MAX_VALUE;
        this.region.maxX = Integer.MIN_VALUE;
        this.region.maxY = Integer.MIN_VALUE;

        for (int i = 0; i < this.choices.size(); i++) {
            square = this.choices.get(i);
            this.region.squaresMask.setValue(this.gridX(square.getX()), this.gridY(square.getY()), true);
            this.region.minX = PZMath.min(this.region.minX, square.getX());
            this.region.minY = PZMath.min(this.region.minY, square.getY());
            this.region.maxX = PZMath.max(this.region.maxX, square.getX());
            this.region.maxY = PZMath.max(this.region.maxY, square.getY());
        }
    }

    boolean shouldVisit(int x1, int y1, int x2, int y2) {
        if (this.gridX(x2) < 8 && this.gridX(x2) >= 0) {
            if (this.gridY(y2) < 8 && this.gridY(y2) >= 0) {
                if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
                    return false;
                } else {
                    Square square2 = this.squares[this.gridX(x2)][this.gridY(y2)];
                    if (this.thumpable) {
                        if (square2 == null) {
                            return false;
                        }

                        if (!square2.has(131072)) {
                            return false;
                        }

                        if (!square2.TreatAsSolidFloor()) {
                            return false;
                        }
                    } else {
                        if (!this.region.levelData.canWalkOnSquare(square2)) {
                            return false;
                        }

                        Square square1 = this.squares[this.gridX(x1)][this.gridY(y1)];
                        if (this.region.levelData.isCanPathTransition(square1, square2)) {
                            return false;
                        }
                    }

                    mover.type = MoverType.Player;
                    mover.minLevel = this.region.getLevel();
                    mover.maxLevel = this.region.getLevel();
                    return PolygonalMap2.instance.canMoveBetween(mover, x1, y1, this.region.getLevel(), x2, y2, this.region.getLevel());
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    void push(int worldSquareX, int worldSquareY) {
        Square square = this.squares[this.gridX(worldSquareX)][this.gridY(worldSquareY)];
        this.stack.push(square);
    }

    Square pop() {
        return this.stack.isEmpty() ? null : this.stack.pop();
    }

    int gridX(int squareX) {
        return squareX - this.minX;
    }

    int gridY(int squareY) {
        return squareY - this.minY;
    }

    void initVisibilityGraphs() {
        Chunk chunk = this.region.getChunk();
        this.visibilityGraphs.clear();
        PolygonalMap2.instance.getVisibilityGraphsOverlappingChunk(chunk, this.region.getLevel(), this.visibilityGraphs);

        for (int i = 0; i < this.visibilityGraphs.size(); i++) {
            VisibilityGraph graph = this.visibilityGraphs.get(i);

            for (int j = 0; j < graph.cluster.rects.size(); j++) {
                VehicleRect rect = graph.cluster.rects.get(j);

                for (int y = rect.y - 1; y < rect.y + rect.h + 1; y++) {
                    for (int x = rect.x - 1; x < rect.x + rect.w + 1; x++) {
                        if (chunk.contains(x, y)) {
                            Square square = this.squares[this.gridX(x)][this.gridY(y)];
                            if (square == null || !square.has(504) && !square.hasSlopedSurface()) {
                                this.visited.setValue(x - this.minX, y - this.minY, true);
                            }
                        }
                    }
                }
            }
        }
    }

    void reset() {
        this.choices.clear();
        this.stack.clear();
        this.visited.clear();
    }
}
