// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;

final class SearchNode extends ASearchNode {
    VGAStar astar;
    Node vgNode;
    Square square;
    int unloadedX;
    int unloadedY;
    boolean inUnloadedArea;
    SearchNode parent;
    static int nextID = 1;
    Integer id = nextID++;
    private static final double SQRT2 = Math.sqrt(2.0);
    static final ArrayDeque<SearchNode> pool = new ArrayDeque<>();

    SearchNode init(VGAStar astar, Node node) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = node;
        this.square = null;
        this.unloadedX = this.unloadedY = -1;
        this.inUnloadedArea = false;
        this.parent = null;
        return this;
    }

    SearchNode init(VGAStar astar, Square square) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = null;
        this.square = square;
        this.unloadedX = this.unloadedY = -1;
        this.inUnloadedArea = false;
        this.parent = null;
        return this;
    }

    SearchNode init(VGAStar astar, int x, int y) {
        this.setG(0.0);
        this.astar = astar;
        this.vgNode = null;
        this.square = null;
        this.unloadedX = x;
        this.unloadedY = y;
        this.inUnloadedArea = true;
        this.parent = null;
        return this;
    }

    @Override
    public double h() {
        return this.dist(this.astar.goalNode.searchNode);
    }

    @Override
    public double c(ISearchNode successor) {
        SearchNode other = (SearchNode)successor;
        if (other.inUnloadedArea) {
            return this.dist(other);
        } else {
            double add = 0.0;
            boolean bCrawlingZombie = this.astar.mover.isZombie() && this.astar.mover.crawling;
            boolean avoidWindows = !this.astar.mover.isZombie() || this.astar.mover.crawling;
            boolean animalCantClimb = this.astar.mover.isAnimal() && !this.astar.mover.canClimbFences;
            if (animalCantClimb) {
                avoidWindows = true;
            }

            if (avoidWindows && this.square != null && other.square != null) {
                if (this.square.x == other.square.x - 1 && this.square.y == other.square.y) {
                    if (other.square.has(2048)) {
                        add = !bCrawlingZombie && other.square.has(1048576) ? 20.0 : 200.0;
                    }
                } else if (this.square.x == other.square.x + 1 && this.square.y == other.square.y) {
                    if (this.square.has(2048)) {
                        add = !bCrawlingZombie && this.square.has(1048576) ? 20.0 : 200.0;
                    }
                } else if (this.square.y == other.square.y - 1 && this.square.x == other.square.x) {
                    if (other.square.has(4096)) {
                        add = !bCrawlingZombie && other.square.has(2097152) ? 20.0 : 200.0;
                    }
                } else if (this.square.y == other.square.y + 1 && this.square.x == other.square.x && this.square.has(4096)) {
                    add = !bCrawlingZombie && this.square.has(2097152) ? 20.0 : 200.0;
                }
            }

            if (other.square != null && other.square.has(131072)) {
                add = Math.max(add, 20.0);
            }

            if (this.vgNode != null && other.vgNode != null) {
                for (int i = 0; i < this.vgNode.visible.size(); i++) {
                    Connection cxn = this.vgNode.visible.get(i);
                    if (cxn.otherNode(this.vgNode) == other.vgNode) {
                        if ((this.vgNode.square == null || !this.vgNode.square.has(131072)) && cxn.has(2)) {
                            add = Math.max(add, 20.0);
                        }
                        break;
                    }
                }
            }

            Square square1 = this.square == null
                ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(this.vgNode.x), PZMath.fastfloor(this.vgNode.y), this.vgNode.z)
                : this.square;
            Square square2 = other.square == null
                ? PolygonalMap2.instance.getSquare(PZMath.fastfloor(other.vgNode.x), PZMath.fastfloor(other.vgNode.y), other.vgNode.z)
                : other.square;
            if (square1 != null && square2 != null) {
                if (square1.x == square2.x - 1 && square1.y == square2.y) {
                    if (square2.has(32768)) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.x == square2.x + 1 && square1.y == square2.y) {
                    if (square1.has(32768)) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.y == square2.y - 1 && square1.x == square2.x) {
                    if (square2.has(65536)) {
                        add = Math.max(add, 20.0);
                    }
                } else if (square1.y == square2.y + 1 && square1.x == square2.x && square1.has(65536)) {
                    add = Math.max(add, 20.0);
                }

                if (bCrawlingZombie || animalCantClimb) {
                    if (square1.x == square2.x - 1 && square1.y == square2.y) {
                        if (square2.has(2) && square2.has(8192) && (!this.astar.mover.isAnimal() || !square2.isUnblockedDoorW())) {
                            add = Math.max(add, 20.0);
                        }
                    } else if (square1.x == square2.x + 1 && square1.y == square2.y) {
                        if (square1.has(2) && square1.has(8192) && (!this.astar.mover.isAnimal() || !square1.isUnblockedDoorW())) {
                            add = Math.max(add, 20.0);
                        }
                    } else if (square1.y == square2.y - 1 && square1.x == square2.x) {
                        if (square2.has(4) && square2.has(16384) && (!this.astar.mover.isAnimal() || !square2.isUnblockedDoorN())) {
                            add = Math.max(add, 20.0);
                        }
                    } else if (square1.y == square2.y + 1
                        && square1.x == square2.x
                        && square1.has(4)
                        && square1.has(16384)
                        && (!this.astar.mover.isAnimal() || !square1.isUnblockedDoorN())) {
                        add = Math.max(add, 20.0);
                    }
                }
            }

            boolean bSelfUnderVehicle = this.vgNode != null && this.vgNode.hasFlag(2);
            boolean bOtherUnderVehicle = other.vgNode != null && other.vgNode.hasFlag(2);
            if (!bSelfUnderVehicle && bOtherUnderVehicle && !this.astar.mover.ignoreCrawlCost) {
                add += 10.0;
            }

            if (other.square != null) {
                add += other.square.cost;
            }

            return this.dist(other) + add;
        }
    }

    @Override
    public void getSuccessors(ArrayList<ISearchNode> successors) {
        if (this.astar.goalNode.searchNode.inUnloadedArea && this.isOnEdgeOfLoadedArea()) {
            successors.add(this.astar.goalNode.searchNode);
        }

        ArrayList<ISearchNode> ret = successors;
        if (this.vgNode != null) {
            this.vgNode.createGraphsIfNeeded();

            for (int i = 0; i < this.vgNode.visible.size(); i++) {
                Connection cxn = this.vgNode.visible.get(i);
                Node visible = cxn.otherNode(this.vgNode);
                SearchNode searchNode = this.astar.getSearchNode(visible);
                if ((this.vgNode.square == null || searchNode.square == null || !this.astar.isKnownBlocked(this.vgNode.square, searchNode.square))
                    && (this.astar.mover.canCrawl || !visible.hasFlag(2))
                    && (this.astar.mover.canThump || !cxn.has(2))) {
                    ret.add(searchNode);
                }
            }

            if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && this.vgNode.hasFlag(16)) {
                Square square1 = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z);
                if (square1 != null && square1.has(32)) {
                    if (!this.astar.mover.isAllowedChunkLevel(square1)) {
                        return;
                    }

                    if (this.astar.canMoveBetween(this.square, square1, false)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1);
                        if (ret.contains(searchNode)) {
                            boolean var42 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }

                square1 = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z);
                if (square1 != null && square1.has(256)) {
                    if (!this.astar.mover.isAllowedChunkLevel(square1)) {
                        return;
                    }

                    if (this.astar.canMoveBetween(this.square, square1, false)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1);
                        if (ret.contains(searchNode)) {
                            boolean var43 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }

                return;
            }

            if (this.vgNode.graphs != null && !this.vgNode.graphs.isEmpty() && !this.vgNode.hasFlag(8)) {
                return;
            }
        }

        if (this.square != null) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0) {
                        Square square1x = PolygonalMap2.instance.getSquareRawZ(this.square.x + dx, this.square.y + dy, this.square.z);
                        if (square1x != null
                            && this.astar.mover.isAllowedChunkLevel(square1x)
                            && (!this.astar.isSquareInCluster(square1x) || square1x.has(504))
                            && this.astar.canMoveBetween(this.square, square1x, false)) {
                            SearchNode searchNode = this.astar.getSearchNode(square1x);
                            if (ret.contains(searchNode)) {
                                boolean var7 = false;
                            } else {
                                ret.add(searchNode);
                            }
                        }
                    }
                }
            }

            if (this.square.has(288)) {
                IsoDirections dir = this.square.has(256) ? IsoDirections.N : IsoDirections.W;
                Square square1x = this.square.getAdjacentSquare(dir.Rot180());
                if (square1x != null
                    && PolygonalMap2.instance.getExistingNodeForSquare(square1x) != null
                    && PolygonalMap2.instance.getExistingNodeForSquare(square1x).hasFlag(16)
                    && this.astar.mover.isAllowedChunkLevel(square1x)
                    && this.astar.canMoveBetween(this.square, square1x, false)) {
                    SearchNode searchNode = this.astar.getSearchNode(square1x);
                    if (ret.contains(searchNode)) {
                        boolean var45 = false;
                    } else {
                        ret.add(searchNode);
                    }
                }
            }

            if (this.square.z > this.astar.mover.minLevel) {
                Square square1x = PolygonalMap2.instance.getSquare(this.square.x, this.square.y + 1, this.square.z - 1);
                if (square1x != null
                    && square1x.hasTransitionToLevelAbove(IsoDirections.N)
                    && !this.astar.isSquareInCluster(square1x)
                    && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, this.square, true)) {
                    SearchNode searchNode = this.astar.getSearchNode(square1x);
                    if (ret.contains(searchNode)) {
                        boolean var34 = false;
                    } else {
                        ret.add(searchNode);
                    }
                }

                square1x = PolygonalMap2.instance.getSquare(this.square.x, this.square.y - 1, this.square.z - 1);
                if (square1x != null
                    && square1x.hasTransitionToLevelAbove(IsoDirections.S)
                    && !this.astar.isSquareInCluster(square1x)
                    && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, this.square, true)) {
                    SearchNode searchNode = this.astar.getSearchNode(square1x);
                    if (ret.contains(searchNode)) {
                        boolean var35 = false;
                    } else {
                        ret.add(searchNode);
                    }
                }

                square1x = PolygonalMap2.instance.getSquare(this.square.x + 1, this.square.y, this.square.z - 1);
                if (square1x != null
                    && square1x.hasTransitionToLevelAbove(IsoDirections.W)
                    && !this.astar.isSquareInCluster(square1x)
                    && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, this.square, true)) {
                    SearchNode searchNode = this.astar.getSearchNode(square1x);
                    if (ret.contains(searchNode)) {
                        boolean var36 = false;
                    } else {
                        ret.add(searchNode);
                    }
                }

                square1x = PolygonalMap2.instance.getSquare(this.square.x - 1, this.square.y, this.square.z - 1);
                if (square1x != null
                    && square1x.hasTransitionToLevelAbove(IsoDirections.E)
                    && !this.astar.isSquareInCluster(square1x)
                    && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, this.square, true)) {
                    SearchNode searchNode = this.astar.getSearchNode(square1x);
                    if (ret.contains(searchNode)) {
                        boolean var37 = false;
                    } else {
                        ret.add(searchNode);
                    }
                }
            }

            if (this.square.z < this.astar.mover.maxLevel) {
                if (this.square.hasTransitionToLevelAbove(IsoDirections.N)) {
                    Square square1xx = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y - 1, this.square.z + 1);
                    if (square1xx != null
                        && !this.astar.isSquareInCluster(square1xx)
                        && this.astar.mover.isAllowedLevelTransition(IsoDirections.N, square1xx, true)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1xx);
                        if (ret.contains(searchNode)) {
                            boolean var38 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }

                if (this.square.hasTransitionToLevelAbove(IsoDirections.S)) {
                    Square square1xx = PolygonalMap2.instance.getSquareRawZ(this.square.x, this.square.y + 1, this.square.z + 1);
                    if (square1xx != null
                        && !this.astar.isSquareInCluster(square1xx)
                        && this.astar.mover.isAllowedLevelTransition(IsoDirections.S, square1xx, true)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1xx);
                        if (ret.contains(searchNode)) {
                            boolean var39 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }

                if (this.square.hasTransitionToLevelAbove(IsoDirections.W)) {
                    Square square1xx = PolygonalMap2.instance.getSquareRawZ(this.square.x - 1, this.square.y, this.square.z + 1);
                    if (square1xx != null
                        && !this.astar.isSquareInCluster(square1xx)
                        && this.astar.mover.isAllowedLevelTransition(IsoDirections.W, square1xx, true)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1xx);
                        if (ret.contains(searchNode)) {
                            boolean var40 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }

                if (this.square.hasTransitionToLevelAbove(IsoDirections.E)) {
                    Square square1xx = PolygonalMap2.instance.getSquareRawZ(this.square.x + 1, this.square.y, this.square.z + 1);
                    if (square1xx != null
                        && !this.astar.isSquareInCluster(square1xx)
                        && this.astar.mover.isAllowedLevelTransition(IsoDirections.E, square1xx, true)) {
                        SearchNode searchNode = this.astar.getSearchNode(square1xx);
                        if (ret.contains(searchNode)) {
                            boolean var41 = false;
                        } else {
                            ret.add(searchNode);
                        }
                    }
                }
            }
        }
    }

    @Override
    public ISearchNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ISearchNode parent) {
        this.parent = (SearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    public float getX() {
        if (this.square != null) {
            return this.square.x + 0.5F;
        } else {
            return this.vgNode != null ? this.vgNode.x : this.unloadedX;
        }
    }

    public float getY() {
        if (this.square != null) {
            return this.square.y + 0.5F;
        } else {
            return this.vgNode != null ? this.vgNode.y : this.unloadedY;
        }
    }

    public float getZ() {
        if (this.square != null) {
            return this.square.z;
        } else {
            return this.vgNode != null ? this.vgNode.z : 32.0F;
        }
    }

    boolean isOnEdgeOfLoadedArea() {
        int x = PZMath.fastfloor(this.getX());
        int y = PZMath.fastfloor(this.getY());
        boolean bOnEdgeOfLoadedArea = false;
        if (PZMath.coordmodulo(x, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x - 1, y) == null) {
            bOnEdgeOfLoadedArea = true;
        }

        if (PZMath.coordmodulo(x, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x + 1, y) == null) {
            bOnEdgeOfLoadedArea = true;
        }

        if (PZMath.coordmodulo(y, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x, y - 1) == null) {
            bOnEdgeOfLoadedArea = true;
        }

        if (PZMath.coordmodulo(y, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x, y + 1) == null) {
            bOnEdgeOfLoadedArea = true;
        }

        return bOnEdgeOfLoadedArea;
    }

    public double dist(SearchNode other) {
        if (this.square == null || other.square == null || Math.abs(this.square.x - other.square.x) > 1 || Math.abs(this.square.y - other.square.y) > 1) {
            float x1 = this.getX();
            float y1 = this.getY();
            float z1 = this.getZ();
            float x2 = other.getX();
            float y2 = other.getY();
            float z2 = other.getZ();
            return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0) + Math.pow((z1 - z2) * 2.5F, 2.0));
        } else {
            return this.square.x != other.square.x && this.square.y != other.square.y ? SQRT2 : 1.0;
        }
    }

    float getApparentZ() {
        if (this.square == null) {
            return this.vgNode.z;
        } else if (this.square.has(8) || this.square.has(64)) {
            return this.square.z + 0.75F;
        } else if (this.square.has(16) || this.square.has(128)) {
            return this.square.z + 0.5F;
        } else {
            return !this.square.has(32) && !this.square.has(256) ? this.square.z : this.square.z + 0.25F;
        }
    }

    static SearchNode alloc() {
        return pool.isEmpty() ? new SearchNode() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
