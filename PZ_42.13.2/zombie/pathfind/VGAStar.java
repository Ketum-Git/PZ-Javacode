// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import astar.AStar;
import astar.ISearchNode;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ai.KnownBlockedEdges;
import zombie.iso.IsoDirections;

final class VGAStar extends AStar {
    ArrayList<VisibilityGraph> graphs;
    final ArrayList<SearchNode> searchNodes = new ArrayList<>();
    final TIntObjectHashMap<SearchNode> nodeMap = new TIntObjectHashMap<>();
    final GoalNode goalNode = new GoalNode();
    final TIntObjectHashMap<SearchNode> squareToNode = new TIntObjectHashMap<>();
    PMMover mover = new PMMover();
    final TIntObjectHashMap<KnownBlockedEdges> knownBlockedEdges = new TIntObjectHashMap<>();
    final VGAStar.InitProc initProc = new VGAStar.InitProc();

    VGAStar init(ArrayList<VisibilityGraph> graphs, TIntObjectHashMap<Node> s2n) {
        this.setMaxSteps(5000);
        this.graphs = graphs;
        this.searchNodes.clear();
        this.nodeMap.clear();
        this.squareToNode.clear();
        s2n.forEachEntry(this.initProc);
        return this;
    }

    VisibilityGraph getVisGraphForSquare(Square square) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(square.x, square.y);
        if (chunk == null) {
            return null;
        } else {
            for (int i = 0; i < chunk.visibilityGraphs.size(); i++) {
                VisibilityGraph graph = chunk.visibilityGraphs.get(i);
                if (graph.contains(square)) {
                    return graph;
                }
            }

            return null;
        }
    }

    boolean isSquareInCluster(Square square) {
        return this.getVisGraphForSquare(square) != null;
    }

    SearchNode getSearchNode(Node vgNode) {
        if (vgNode.square != null) {
            return this.getSearchNode(vgNode.square);
        } else {
            SearchNode searchNode = this.nodeMap.get(vgNode.id);
            if (searchNode == null) {
                searchNode = SearchNode.alloc().init(this, vgNode);
                this.searchNodes.add(searchNode);
                this.nodeMap.put(vgNode.id, searchNode);
            }

            return searchNode;
        }
    }

    SearchNode getSearchNode(Square square) {
        SearchNode searchNode = this.squareToNode.get(square.id);
        if (searchNode == null) {
            searchNode = SearchNode.alloc().init(this, square);
            this.searchNodes.add(searchNode);
            this.squareToNode.put(square.id, searchNode);
        }

        return searchNode;
    }

    SearchNode getSearchNode(int x, int y) {
        SearchNode searchNode = SearchNode.alloc().init(this, x, y);
        this.searchNodes.add(searchNode);
        return searchNode;
    }

    ArrayList<ISearchNode> shortestPath(PathFindRequest request, SearchNode startNode, SearchNode goalNode) {
        this.mover.set(request);
        this.goalNode.init(goalNode);
        return this.shortestPath(startNode, this.goalNode);
    }

    boolean canMoveBetween(Square square1, Square square2, boolean isBetween) {
        return !this.canNotMoveBetween(square1, square2, isBetween);
    }

    boolean canNotMoveBetween(Square square1, Square square2, boolean isBetween) {
        assert Math.abs(square1.x - square2.x) <= 1;

        assert Math.abs(square1.y - square2.y) <= 1;

        assert square1.z == square2.z;

        assert square1 != square2;

        boolean testW = square2.x < square1.x;
        boolean testE = square2.x > square1.x;
        boolean testN = square2.y < square1.y;
        boolean testS = square2.y > square1.y;
        if (!square2.isNonThumpableSolid() && (this.mover.canThump || !square2.isReallySolid())) {
            if (square2.y < square1.y && square1.has(64)) {
                return true;
            } else if (square2.x < square1.x && square1.has(8)) {
                return true;
            } else if (square2.y > square1.y && square2.x == square1.x && square2.has(64)) {
                return true;
            } else if (square2.x > square1.x && square2.y == square1.y && square2.has(8)) {
                return true;
            } else if (square2.x != square1.x && square2.has(448)) {
                return true;
            } else if (square2.y != square1.y && square2.has(56)) {
                return true;
            } else if (square2.x != square1.x && square1.has(448)) {
                return true;
            } else if (square2.y != square1.y && square1.has(56)) {
                return true;
            } else {
                if (square2.z == square1.z) {
                    if (square2.x == square1.x
                        && square2.y == square1.y - 1
                        && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.N) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.S))) {
                        return true;
                    }

                    if (square2.x == square1.x
                        && square2.y == square1.y + 1
                        && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.S) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.N))) {
                        return true;
                    }

                    if (square2.x == square1.x - 1
                        && square2.y == square1.y
                        && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.W) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.E))) {
                        return true;
                    }

                    if (square2.x == square1.x + 1
                        && square2.y == square1.y
                        && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.E) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.W))) {
                        return true;
                    }
                }

                if (!square2.has(512) && !square2.has(504)) {
                    return true;
                } else if (this.isKnownBlocked(square1, square2)) {
                    return true;
                } else {
                    if (this.mover.isAnimal()) {
                        boolean bOpenDoorN = testN && square1.isUnblockedDoorN();
                        boolean bOpenDoorW = testW && square1.isUnblockedDoorW();
                        boolean colN = testN && (square1.isCollideN() || square1.isThumpN()) && !bOpenDoorN;
                        boolean colW = testW && (square1.isCollideW() || square1.isThumpW()) && !bOpenDoorW;
                        if (testN && square1.isCanPathN() && (square1.x != square2.x || isBetween)) {
                            return true;
                        }

                        if (testW && square1.isCanPathW() && (square1.y != square2.y || isBetween)) {
                            return true;
                        }

                        if ((colN || colW) && !this.canAnimalBreakObstacle(square1, square2, colW, colN, false, false)) {
                            return true;
                        }

                        boolean bOpenDoorS = testS && square2.isUnblockedDoorN();
                        boolean bOpenDoorE = testE && square2.isUnblockedDoorW();
                        boolean colS = testS && (square2.isCollideN() || square2.isThumpN()) && !bOpenDoorS;
                        boolean colE = testE && (square2.isCollideW() || square2.isThumpW()) && !bOpenDoorE;
                        if (testS && square2.isCanPathN() && (square1.x != square2.x || isBetween)) {
                            return true;
                        }

                        if (testE && square2.isCanPathW() && (square1.y != square2.y || isBetween)) {
                            return true;
                        }

                        if ((colS || colE) && !this.canAnimalBreakObstacle(square1, square2, false, false, colE, colS)) {
                            return true;
                        }
                    } else {
                        boolean canPathN = square1.isCanPathN() && (this.mover.canThump || !square1.isThumpN());
                        boolean canPathW = square1.isCanPathW() && (this.mover.canThump || !square1.isThumpW());
                        boolean colNx = testN && square1.isCollideN() && (square1.x != square2.x || isBetween || !canPathN);
                        boolean colWx = testW && square1.isCollideW() && (square1.y != square2.y || isBetween || !canPathW);
                        canPathN = square2.isCanPathN() && (this.mover.canThump || !square2.isThumpN());
                        canPathW = square2.isCanPathW() && (this.mover.canThump || !square2.isThumpW());
                        boolean colSx = testS && square2.has(131076) && (square1.x != square2.x || isBetween || !canPathN);
                        boolean colEx = testE && square2.has(131074) && (square1.y != square2.y || isBetween || !canPathW);
                        if (colNx || colWx || colSx || colEx) {
                            return true;
                        }
                    }

                    boolean diag = square2.x != square1.x && square2.y != square1.y;
                    if (diag) {
                        Square betweenA = PolygonalMap2.instance.getSquareRawZ(square1.x, square2.y, square1.z);
                        Square betweenB = PolygonalMap2.instance.getSquareRawZ(square2.x, square1.y, square1.z);

                        assert betweenA != square1 && betweenA != square2;

                        assert betweenB != square1 && betweenB != square2;

                        if (square2.x == square1.x + 1 && square2.y == square1.y + 1 && betweenA != null && betweenB != null) {
                            if (betweenA.has(4096) && betweenB.has(2048)) {
                                return true;
                            }

                            if (betweenA.isThumpN() && betweenB.isThumpW()) {
                                return true;
                            }
                        }

                        if (square2.x == square1.x - 1 && square2.y == square1.y - 1 && betweenA != null && betweenB != null) {
                            if (betweenA.has(2048) && betweenB.has(4096)) {
                                return true;
                            }

                            if (betweenA.isThumpW() && betweenB.isThumpN()) {
                                return true;
                            }
                        }

                        if (betweenA == null || this.canNotMoveBetween(square1, betweenA, true)) {
                            return true;
                        }

                        if (betweenB == null || this.canNotMoveBetween(square1, betweenB, true)) {
                            return true;
                        }

                        if (betweenA == null || this.canNotMoveBetween(square2, betweenA, true)) {
                            return true;
                        }

                        if (betweenB == null || this.canNotMoveBetween(square2, betweenB, true)) {
                            return true;
                        }
                    }

                    return false;
                }
            }
        } else {
            return true;
        }
    }

    boolean isKnownBlocked(Square square1, Square square2) {
        if (square1.z != square2.z) {
            return false;
        } else {
            KnownBlockedEdges kbe1 = this.knownBlockedEdges.get(square1.id);
            KnownBlockedEdges kbe2 = this.knownBlockedEdges.get(square2.id);
            return kbe1 != null && kbe1.isBlocked(square2.x, square2.y) ? true : kbe2 != null && kbe2.isBlocked(square1.x, square1.y);
        }
    }

    boolean canAnimalBreakObstacle(Square square1, Square square2, boolean colW, boolean colN, boolean colE, boolean colS) {
        if (!this.mover.canThump) {
            return false;
        } else if (colW) {
            return square1.has(2) && square1.has(8192);
        } else if (colN) {
            return square1.has(4) && square1.has(16384);
        } else if (colE) {
            return square2.has(2) && square2.has(8192);
        } else {
            return !colS ? false : square2.has(4) && square2.has(16384);
        }
    }

    final class InitProc implements TIntObjectProcedure<Node> {
        InitProc() {
            Objects.requireNonNull(VGAStar.this);
            super();
        }

        public boolean execute(int id, Node vgNode) {
            SearchNode searchNode = SearchNode.alloc().init(VGAStar.this, vgNode);
            searchNode.square = vgNode.square;
            VGAStar.this.squareToNode.put(id, searchNode);
            VGAStar.this.nodeMap.put(vgNode.id, searchNode);
            VGAStar.this.searchNodes.add(searchNode);
            return true;
        }
    }
}
