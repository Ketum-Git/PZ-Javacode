// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import astar.AStar;
import astar.ISearchNode;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.math.PZMath;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.pathfind.AdjustStartEndNodeData;
import zombie.pathfind.Chunk;
import zombie.pathfind.Connection;
import zombie.pathfind.Edge;
import zombie.pathfind.Node;
import zombie.pathfind.PMMover;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.VehicleRect;
import zombie.pathfind.VisibilityGraph;
import zombie.util.list.PZArrayUtil;

public class HLAStar extends AStar {
    static final int CPW = 8;
    public static int modificationCount;
    public static final PerformanceProfileProbe PerfFindPath = new PerformanceProfileProbe("HLAStar.findPath");
    public static final PerformanceProfileProbe PerfGetSuccessors = new PerformanceProfileProbe("HLAStar.getSuccessors");
    public static final PerformanceProfileProbe PerfGetSuccessors_OnSameChunk = new PerformanceProfileProbe("HLAStar.getSuccessors.OnSameChunk");
    public static final PerformanceProfileProbe PerfGetSuccessors_OnAdjacentChunks = new PerformanceProfileProbe("HLAStar.getSuccessors.OnAdjacentChunks");
    public static final PerformanceProfileProbe PerfGetSuccessors_VisibilityGraphs = new PerformanceProfileProbe("HLAStar.getSuccessors.VisibilityGraphs");
    public static final PerformanceProfileProbe PerfInitStairs = new PerformanceProfileProbe("HLAStar.initStairs");
    HLSearchNode initialNode;
    final HLGoalNode goalNode = new HLGoalNode();
    final HashMap<HLChunkRegion, HLSearchNode> nodeMapChunkRegion = new HashMap<>();
    final HashMap<HLLevelTransition, HLSearchNode> nodeMapLevelTransition = new HashMap<>();
    final HashMap<Node, HLSearchNode> nodeMapVisGraph = new HashMap<>();
    boolean goalInUnloadedArea;
    HLSearchNode unloadedSearchNode;
    final ArrayList<VisibilityGraph> visibilityGraphs = new ArrayList<>();
    final HLAStar.AdjustVisibilityGraphData adjustVisibilityGraphDataStart = new HLAStar.AdjustVisibilityGraphData();
    final HLAStar.AdjustVisibilityGraphData adjustVisibilityGraphDataGoal = new HLAStar.AdjustVisibilityGraphData();

    public void findPath(
        PMMover mover,
        float x1,
        float y1,
        int z1,
        float x2,
        float y2,
        int z2,
        ArrayList<HLLevelTransition> levelTransitionList,
        ArrayList<HLChunkLevel> chunkList,
        ArrayList<Boolean> bottomOfLevelTransition,
        boolean bRender
    ) {
        levelTransitionList.clear();
        chunkList.clear();
        if (mover != HLGlobals.mover) {
            HLGlobals.mover.set(mover);
        }

        this.releaseSearchNodes();

        try {
            this.adjustVisibilityGraphDataStart.adjusted = false;
            this.adjustVisibilityGraphDataStart.graph = null;
            this.adjustVisibilityGraphDataStart.vgNode = null;
            this.adjustVisibilityGraphDataGoal.adjusted = false;
            this.adjustVisibilityGraphDataGoal.graph = null;
            this.adjustVisibilityGraphDataGoal.vgNode = null;
            Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(x2), PZMath.fastfloor(y2));
            this.goalInUnloadedArea = chunk == null;
            this.unloadedSearchNode = null;
            HLSearchNode initialNode = this.initStartNode(x1, y1, z1);
            if (initialNode == null) {
                return;
            }

            HLSearchNode goalNode1 = this.initGoalNode(x2, y2, z2);
            if (goalNode1 == null) {
                return;
            }

            if (DebugOptions.instance.pathfindRenderChunkRegions.getValue() && this.getRegionAt(x1, y1, z1) != null) {
                this.getRegionAt(x1, y1, z1).renderDebug();
            }

            if (DebugOptions.instance.pathfindRenderChunkRegions.getValue() && this.getRegionAt(x2, y2, z2) != null) {
                this.getRegionAt(x2, y2, z2).renderDebug();
            }

            if (initialNode.chunkRegion == goalNode1.chunkRegion && initialNode.chunkRegion != null) {
                if (bRender) {
                    initialNode.chunkRegion.renderDebug();
                }

                this.addChunkLevelAndAdjacentToList(initialNode.chunkRegion.levelData, chunkList);
                return;
            }

            if (initialNode.levelTransition == null || initialNode.levelTransition != goalNode1.levelTransition) {
                this.goalNode.init(goalNode1);
                this.initialNode = initialNode;
                ArrayList<ISearchNode> path = this.shortestPath(initialNode, this.goalNode);
                if (path == null) {
                    return;
                }

                bottomOfLevelTransition.clear();

                for (int pi = 0; pi < path.size(); pi++) {
                    HLSearchNode searchNode = (HLSearchNode)path.get(pi);
                    if (searchNode.chunkRegion != null) {
                        if (bRender && DebugOptions.instance.polymapRenderClusters.getValue()) {
                            searchNode.chunkRegion.renderDebug();
                        }

                        this.addChunkLevelAndAdjacentToList(searchNode.chunkRegion.levelData, chunkList);
                    }

                    if (searchNode.levelTransition != null) {
                        if (bRender && DebugOptions.instance.polymapRenderClusters.getValue()) {
                            searchNode.levelTransition.renderDebug();
                        }

                        levelTransitionList.add(searchNode.levelTransition);
                        this.addLevelTransitionChunkLevelData(searchNode.levelTransition, chunkList);
                        boolean bPathToBottom = false;
                        if (pi == path.size() - 1) {
                            bPathToBottom = true;
                        } else {
                            HLSearchNode next = (HLSearchNode)path.get(pi + 1);
                            bPathToBottom = PZMath.fastfloor(next.getZ()) <= searchNode.levelTransition.getBottomFloorZ();
                        }

                        bottomOfLevelTransition.add(bPathToBottom);
                    }

                    if (searchNode.vgNode != null) {
                        if (bRender) {
                        }

                        chunk = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(searchNode.vgNode.x), PZMath.fastfloor(searchNode.vgNode.y));
                        if (chunk != null) {
                            HLChunkLevel levelData = chunk.getLevelData(searchNode.vgNode.z).getHighLevelData();
                            this.addChunkLevelAndAdjacentToList(levelData, chunkList);
                        }
                    }
                }

                if (bRender) {
                    for (int i = 1; i < path.size(); i++) {
                        HLSearchNode node1 = (HLSearchNode)path.get(i - 1);
                        HLSearchNode node2 = (HLSearchNode)path.get(i);
                        LineDrawer.addRect(node1.getX() - 0.05F, node1.getY() - 0.05F, node1.getZ() - 32.0F, 0.1F, 0.1F, 0.0F, 1.0F, 0.0F);
                        LineDrawer.addLine(
                            node1.getX(), node1.getY(), node1.getZ() - 32.0F, node2.getX(), node2.getY(), node2.getZ() - 32.0F, 0.0F, 1.0F, 0.0F, 1.0F
                        );
                    }
                }

                return;
            }

            if (bRender) {
                initialNode.levelTransition.renderDebug();
            }

            levelTransitionList.add(initialNode.levelTransition);
            this.addLevelTransitionChunkLevelData(initialNode.levelTransition, chunkList);
        } finally {
            this.cleanUpVisibilityGraphNode(this.adjustVisibilityGraphDataStart);
            this.cleanUpVisibilityGraphNode(this.adjustVisibilityGraphDataGoal);
            this.releaseSearchNodes();
        }
    }

    void releaseSearchNodes() {
        this.initialNode = null;
        if (this.unloadedSearchNode != null) {
            HLGlobals.searchNodePool.release(this.unloadedSearchNode);
            this.unloadedSearchNode = null;
        }

        HLGlobals.searchNodePool.releaseAll(new ArrayList<>(this.nodeMapChunkRegion.values()));
        this.nodeMapChunkRegion.clear();
        HLGlobals.searchNodePool.releaseAll(new ArrayList<>(this.nodeMapLevelTransition.values()));
        this.nodeMapLevelTransition.clear();
        HLGlobals.searchNodePool.releaseAll(new ArrayList<>(this.nodeMapVisGraph.values()));
        this.nodeMapVisGraph.clear();
    }

    HLSearchNode initStartNode(float x1, float y1, int z1) {
        Node vgNode = this.getSquarePolygonalMapNode(PZMath.fastfloor(x1), PZMath.fastfloor(y1), z1);
        if (vgNode != null && !this.shouldIgnoreNode(vgNode)) {
            return this.getSearchNode(vgNode);
        } else {
            HLChunkRegion chunkRegion1 = this.getRegionAt(x1, y1, z1);
            if (chunkRegion1 != null) {
                return this.getSearchNode(chunkRegion1);
            } else {
                vgNode = null;
                VisibilityGraph graph = PolygonalMap2.instance.getVisGraphAt(x1, y1, z1, 0);
                if (graph == null) {
                    graph = PolygonalMap2.instance.getVisGraphAt(x1, y1, z1, 1);
                    if (graph != null) {
                        if (!graph.isCreated()) {
                            graph.create();
                        }

                        Square square = PolygonalMap2.instance.getSquare(PZMath.fastfloor(x1), PZMath.fastfloor(y1), z1);
                        vgNode = PolygonalMap2.instance.getPointOutsideObjects(square, x1, y1);
                        graph.addNode(vgNode);
                        if (vgNode.x != x1 || vgNode.y != y1) {
                            this.adjustVisibilityGraphDataStart.adjusted = true;
                            this.adjustVisibilityGraphDataStart.adjustStartEndNodeData.isNodeNew = false;
                        }

                        this.adjustVisibilityGraphDataStart.vgNode = vgNode;
                        this.adjustVisibilityGraphDataStart.graph = graph;
                    }
                } else {
                    vgNode = this.initVisibilityGraphNode(graph, x1, y1, z1, this.adjustVisibilityGraphDataStart);
                }

                if (vgNode != null) {
                    return this.getSearchNode(vgNode);
                } else {
                    HLLevelTransition levelTransition = this.getLevelTransitionAt(PZMath.fastfloor(x1), PZMath.fastfloor(y1), z1);
                    return levelTransition != null ? this.getSearchNode(levelTransition, true) : null;
                }
            }
        }
    }

    HLSearchNode initGoalNode(float x2, float y2, int z2) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(PZMath.fastfloor(x2), PZMath.fastfloor(y2));
        if (chunk == null) {
            return this.getSearchNode(PZMath.fastfloor(x2), PZMath.fastfloor(y2));
        } else {
            Node vgNode = this.getSquarePolygonalMapNode(PZMath.fastfloor(x2), PZMath.fastfloor(y2), z2);
            if (vgNode != null && !this.shouldIgnoreNode(vgNode)) {
                return this.getSearchNode(vgNode);
            } else {
                HLChunkRegion chunkRegion2 = this.getRegionAt(x2, y2, z2);
                if (chunkRegion2 != null) {
                    return this.getSearchNode(chunkRegion2);
                } else {
                    vgNode = null;
                    VisibilityGraph graph = PolygonalMap2.instance.getVisGraphAt(x2, y2, z2, 0);
                    if (graph == null) {
                        graph = PolygonalMap2.instance.getVisGraphAt(x2, y2, z2, 1);
                        if (graph != null) {
                            if (!graph.isCreated()) {
                                graph.create();
                            }

                            Square square = PolygonalMap2.instance.getSquare(PZMath.fastfloor(x2), PZMath.fastfloor(y2), z2);
                            vgNode = PolygonalMap2.instance.getPointOutsideObjects(square, x2, y2);
                            graph.addNode(vgNode);
                            if (vgNode.x != x2 || vgNode.y != y2) {
                                this.adjustVisibilityGraphDataGoal.adjusted = true;
                                this.adjustVisibilityGraphDataGoal.adjustStartEndNodeData.isNodeNew = false;
                            }

                            this.adjustVisibilityGraphDataGoal.vgNode = vgNode;
                            this.adjustVisibilityGraphDataGoal.graph = graph;
                        }
                    } else {
                        vgNode = this.initVisibilityGraphNode(graph, x2, y2, z2, this.adjustVisibilityGraphDataGoal);
                    }

                    if (vgNode != null) {
                        return this.getSearchNode(vgNode);
                    } else {
                        HLLevelTransition levelTransition = this.getLevelTransitionAt(PZMath.fastfloor(x2), PZMath.fastfloor(y2), z2);
                        return levelTransition != null ? this.getSearchNode(levelTransition, true) : null;
                    }
                }
            }
        }
    }

    public HLLevelTransition getLevelTransitionAt(int x, int y, int z) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(x, y);
        if (chunk == null) {
            return null;
        } else if (!chunk.isValidLevel(z)) {
            return null;
        } else {
            HLChunkLevel levelData = chunk.getLevelData(z).getHighLevelData();
            levelData.initStairsIfNeeded();
            return levelData.getLevelTransitionAt(x, y);
        }
    }

    Node getSquarePolygonalMapNode(Square square) {
        if (square == null) {
            return null;
        } else {
            Node vgNode = PolygonalMap2.instance.getNodeForSquare(square);
            return vgNode.visible.isEmpty() ? null : vgNode;
        }
    }

    Node getSquarePolygonalMapNode(int x, int y, int z) {
        Square square = PolygonalMap2.instance.getSquare(x, y, z);
        return this.getSquarePolygonalMapNode(square);
    }

    Node getStairSquarePolygonalMapNode(Square square) {
        if (square == null) {
            return null;
        } else if (!square.has(504)) {
            return null;
        } else {
            Node vgNode = PolygonalMap2.instance.getNodeForSquare(square);
            return vgNode.visible.isEmpty() ? null : vgNode;
        }
    }

    Node getStairSquarePolygonalMapNode(int x, int y, int z) {
        Square square = PolygonalMap2.instance.getSquare(x, y, z);
        return this.getStairSquarePolygonalMapNode(square);
    }

    boolean isStairSquareWithPolygonalMapNode(Square square) {
        return this.getStairSquarePolygonalMapNode(square) != null;
    }

    boolean isStairSquareWithPolygonalMapNode(int x, int y, int z) {
        return this.getStairSquarePolygonalMapNode(x, y, z) != null;
    }

    Node initVisibilityGraphNode(VisibilityGraph graph, float x, float y, float z, HLAStar.AdjustVisibilityGraphData data) {
        data.adjusted = false;
        data.graph = null;
        data.vgNode = null;
        if (!graph.isCreated()) {
            graph.create();
        }

        int adjusted = graph.getPointOutsideObstacles(x, y, z, data.adjustStartEndNodeData);
        if (adjusted == -1) {
            return null;
        } else {
            if (adjusted == 1) {
                data.adjusted = true;
                data.vgNode = data.adjustStartEndNodeData.node;
                if (data.adjustStartEndNodeData.isNodeNew) {
                    data.graph = graph;
                }
            }

            if (data.vgNode == null) {
                data.vgNode = Node.alloc().init(x, y, PZMath.fastfloor(z));
                graph.addNode(data.vgNode);
                data.graph = graph;
            }

            return data.vgNode;
        }
    }

    void cleanUpVisibilityGraphNode(HLAStar.AdjustVisibilityGraphData data) {
        if (data.graph != null) {
            data.graph.removeNode(data.vgNode);
            data.vgNode.release();
        }

        if (data.adjusted && data.adjustStartEndNodeData.isNodeNew) {
            for (int i = 0; i < data.vgNode.edges.size(); i++) {
                Edge edge = data.vgNode.edges.get(i);
                edge.obstacle.unsplit(data.vgNode, edge.edgeRing);
            }

            data.graph.edges.remove(data.adjustStartEndNodeData.newEdge);
        }
    }

    HLChunkRegion getRegionAt(float x, float y, int z) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromChunkPos(PZMath.fastfloor(x / 8.0F), PZMath.fastfloor(y / 8.0F));
        if (chunk == null) {
            return null;
        } else if (!chunk.isValidLevel(z)) {
            return null;
        } else {
            HLChunkLevel levelData = chunk.getLevelData(z).getHighLevelData();
            return levelData.findRegionContainingSquare(PZMath.fastfloor(x), PZMath.fastfloor(y));
        }
    }

    void getSuccessors(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        try (AbstractPerformanceProfileProbe ignored = PerfGetSuccessors.profile()) {
            this.getSuccessorsInternal(searchNode, successors);
        }
    }

    void getSuccessorsInternal(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLGlobals.successorPool.releaseAll(searchNode.successors);
        searchNode.successors.clear();
        if (!searchNode.inUnloadedArea) {
            if (searchNode.onEdgeOfLoadedArea) {
                successors.add(this.unloadedSearchNode);
            }

            if (searchNode.levelTransition != null) {
                this.getSuccessorChunkRegionsFromLevelTransition(searchNode, successors);
                this.getSuccessorVisibilityGraphsFromLevelTransition(searchNode, successors);
            } else if (searchNode.vgNode != null) {
                this.getSuccessorsFromVisibilityGraph(searchNode, successors);
            } else {
                try (AbstractPerformanceProfileProbe ignored = PerfGetSuccessors_OnSameChunk.profile()) {
                    this.getSuccessorsOnSameChunk(searchNode, successors);
                }

                try (AbstractPerformanceProfileProbe ignored = PerfGetSuccessors_OnAdjacentChunks.profile()) {
                    this.getSuccessorsOnAdjacentChunks(searchNode, successors);
                }

                this.getSuccessorsGoingUp(searchNode, successors);
                this.getSuccessorsGoingDown(searchNode, successors);

                try (AbstractPerformanceProfileProbe ignored = PerfGetSuccessors_VisibilityGraphs.profile()) {
                    this.getSuccessorVisibilityGraphs(searchNode, successors);
                }
            }
        }
    }

    void getSuccessorChunkRegionsFromLevelTransition(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLLevelTransition levelTransition = searchNode.levelTransition;
        Square square1 = levelTransition.getTopFloorSquare();
        Square square2 = levelTransition.getBottomFloorSquare();
        if (square1 != null && square2 != null) {
            Chunk chunk2 = levelTransition.getBottomFloorChunk();
            if (chunk2 != null) {
                HLChunkLevel levelData2 = chunk2.getLevelData(levelTransition.getBottomFloorZ()).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(levelTransition.getBottomFloorX(), levelTransition.getBottomFloorY());
                if (chunkRegion2 != null) {
                    double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }

            chunk2 = levelTransition.getTopFloorChunk();
            if (chunk2 != null) {
                HLChunkLevel levelData2 = chunk2.getLevelData(levelTransition.getTopFloorZ()).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(levelTransition.getTopFloorX(), levelTransition.getTopFloorY());
                if (chunkRegion2 != null) {
                    double cost = this.calculateCost(searchNode, square2, chunkRegion2, square1);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }
        }
    }

    void getSuccessorVisibilityGraphsFromLevelTransition(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLLevelTransition levelTransition = searchNode.levelTransition;
        Square square1 = levelTransition.getTopFloorSquare();
        Square square2 = levelTransition.getBottomFloorSquare();
        if (square1 != null) {
            VisibilityGraph graph = PolygonalMap2.instance.getVisGraphAt(square1.getX() + 0.5F, square1.getY() + 0.5F, square1.getZ(), 1);
            if (graph != null) {
                if (!graph.isCreated()) {
                    graph.create();
                }

                Node vgNode = PolygonalMap2.instance.getNodeForSquare(square1);
                double cost = 1.0;
                this.addSuccessor(1.0, searchNode, vgNode, successors);
            }
        }

        if (square2 != null) {
            VisibilityGraph graph = PolygonalMap2.instance.getVisGraphAt(square2.getX() + 0.5F, square2.getY() + 0.5F, square2.getZ(), 1);
            if (graph != null) {
                if (!graph.isCreated()) {
                    graph.create();
                }

                Node vgNode = PolygonalMap2.instance.getNodeForSquare(square2);
                double cost = 1.0;
                this.addSuccessor(1.0, searchNode, vgNode, successors);
            }
        }
    }

    void getSuccessorsOnSameChunk(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLChunkRegion chunkRegion1 = searchNode.chunkRegion;
        HLChunkLevel levelData1 = chunkRegion1.levelData;
        Chunk chunk1 = chunkRegion1.getChunk();
        int level = levelData1.getLevel();

        for (int y = 0; y < 8; y++) {
            if (y > 0 && chunkRegion1.edgeN[y] != 0) {
                for (int i = 0; i < levelData1.regionList.size(); i++) {
                    HLChunkRegion chunkRegion2 = levelData1.regionList.get(i);
                    if (chunkRegion1 != chunkRegion2 && (chunkRegion2.edgeS[y - 1] & chunkRegion1.edgeN[y]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, y, chunkRegion2.edgeS[y - 1] & chunkRegion1.edgeN[y], chunk1, IsoDirections.N, level, successors
                        );
                    }
                }
            }

            if (y < 7 && chunkRegion1.edgeS[y] != 0) {
                for (int ix = 0; ix < levelData1.regionList.size(); ix++) {
                    HLChunkRegion chunkRegion2 = levelData1.regionList.get(ix);
                    if (chunkRegion1 != chunkRegion2 && (chunkRegion2.edgeN[y + 1] & chunkRegion1.edgeS[y]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, y, chunkRegion2.edgeN[y + 1] & chunkRegion1.edgeS[y], chunk1, IsoDirections.S, level, successors
                        );
                    }
                }
            }
        }

        for (int x = 0; x < 8; x++) {
            if (x > 0 && chunkRegion1.edgeW[x] != 0) {
                for (int ixx = 0; ixx < levelData1.regionList.size(); ixx++) {
                    HLChunkRegion chunkRegion2 = levelData1.regionList.get(ixx);
                    if (chunkRegion1 != chunkRegion2 && (chunkRegion2.edgeE[x - 1] & chunkRegion1.edgeW[x]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, x, chunkRegion2.edgeE[x - 1] & chunkRegion1.edgeW[x], chunk1, IsoDirections.W, level, successors
                        );
                    }
                }
            }

            if (x < 7 && chunkRegion1.edgeE[x] != 0) {
                for (int ixxx = 0; ixxx < levelData1.regionList.size(); ixxx++) {
                    HLChunkRegion chunkRegion2 = levelData1.regionList.get(ixxx);
                    if (chunkRegion1 != chunkRegion2 && (chunkRegion2.edgeW[x + 1] & chunkRegion1.edgeE[x]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, x, chunkRegion2.edgeW[x + 1] & chunkRegion1.edgeE[x], chunk1, IsoDirections.E, level, successors
                        );
                    }
                }
            }
        }
    }

    void getSuccessorsOnAdjacentChunks(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLChunkRegion chunkRegion1 = searchNode.chunkRegion;
        HLChunkLevel levelData1 = chunkRegion1.levelData;
        Chunk chunk1 = chunkRegion1.getChunk();
        int level = levelData1.getLevel();
        Square[][] squares1 = chunk1.getSquaresForLevel(level);
        if (chunkRegion1.edgeN[0] != 0) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx, chunk1.wy - 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();

                for (int i = 0; i < levelData2.regionList.size(); i++) {
                    HLChunkRegion chunkRegion2 = levelData2.regionList.get(i);
                    if ((chunkRegion2.edgeS[7] & chunkRegion1.edgeN[0]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, 0, chunkRegion2.edgeS[7] & chunkRegion1.edgeN[0], chunk1, IsoDirections.N, level, successors
                        );
                    }
                }
            }
        }

        if (chunkRegion1.edgeS[7] != 0) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx, chunk1.wy + 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();

                for (int ix = 0; ix < levelData2.regionList.size(); ix++) {
                    HLChunkRegion chunkRegion2 = levelData2.regionList.get(ix);
                    if ((chunkRegion2.edgeN[0] & chunkRegion1.edgeS[7]) != 0) {
                        this.addSuccessorsOnEdge(
                            searchNode, chunkRegion2, 7, chunkRegion2.edgeN[0] & chunkRegion1.edgeS[7], chunk1, IsoDirections.S, level, successors
                        );
                    }
                }
            }
        }

        if (chunkRegion1.edgeW[0] != 0) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx - 1, chunk1.wy);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();

                for (int ixx = 0; ixx < levelData2.regionList.size(); ixx++) {
                    HLChunkRegion chunkRegion2 = levelData2.regionList.get(ixx);
                    if ((chunkRegion2.edgeE[7] & chunkRegion1.edgeW[0]) != 0) {
                        for (int y = 0; y < 8; y++) {
                            this.addSuccessorsOnEdge(
                                searchNode, chunkRegion2, 0, chunkRegion2.edgeE[7] & chunkRegion1.edgeW[0], chunk1, IsoDirections.W, level, successors
                            );
                        }
                    }
                }
            }
        }

        if (chunkRegion1.edgeE[7] != 0) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx + 1, chunk1.wy);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();

                for (int ixxx = 0; ixxx < levelData2.regionList.size(); ixxx++) {
                    HLChunkRegion chunkRegion2 = levelData2.regionList.get(ixxx);
                    if ((chunkRegion2.edgeW[0] & chunkRegion1.edgeE[7]) != 0) {
                        for (int y = 0; y < 8; y++) {
                            this.addSuccessorsOnEdge(
                                searchNode, chunkRegion2, 7, chunkRegion2.edgeW[0] & chunkRegion1.edgeE[7], chunk1, IsoDirections.E, level, successors
                            );
                        }
                    }
                }
            }
        }

        int x1;
        int y1;
        if (chunkRegion1.containsSquare(x1 = chunk1.getMinX(), y1 = chunk1.getMinY())) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx - 1, chunk1.wy - 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(x1 - 1, y1 - 1);
                if (chunkRegion2 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, x1, y1, level, x1 - 1, y1 - 1, level)) {
                    Square square1 = squares1[0][0];
                    Square square2 = chunk2.getSquaresForLevel(level)[7][7];
                    double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }
        }

        if (chunkRegion1.containsSquare(x1 = chunk1.getMaxX(), y1 = chunk1.getMinY())) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx + 1, chunk1.wy - 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(x1 + 1, y1 - 1);
                if (chunkRegion2 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, x1, y1, level, x1 + 1, y1 - 1, level)) {
                    Square square1 = squares1[7][0];
                    Square square2 = chunk2.getSquaresForLevel(level)[0][7];
                    double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }
        }

        if (chunkRegion1.containsSquare(x1 = chunk1.getMaxX(), y1 = chunk1.getMaxY())) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx + 1, chunk1.wy + 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(x1 + 1, y1 + 1);
                if (chunkRegion2 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, x1, y1, level, x1 + 1, y1 + 1, level)) {
                    Square square1 = squares1[7][7];
                    Square square2 = chunk2.getSquaresForLevel(level)[0][0];
                    double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }
        }

        if (chunkRegion1.containsSquare(x1 = chunk1.getMinX(), y1 = chunk1.getMaxY())) {
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(chunk1.wx - 1, chunk1.wy + 1);
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();
                HLChunkRegion chunkRegion2 = levelData2.findRegionContainingSquare(x1 - 1, y1 + 1);
                if (chunkRegion2 != null && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, x1, y1, level, x1 - 1, y1 + 1, level)) {
                    Square square1 = squares1[0][7];
                    Square square2 = chunk2.getSquaresForLevel(level)[7][0];
                    double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                    this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                }
            }
        }
    }

    void addSuccessorsOnEdge(
        HLSearchNode node, HLChunkRegion chunkRegion2, int xy, int bits, Chunk chunk, IsoDirections dir, int level, ArrayList<ISearchNode> successors
    ) {
        Square[][] squares = chunk.getSquaresForLevel(level);

        for (int n = 0; n < 8; n++) {
            if ((bits & 1 << n) != 0) {
                Square square1 = dir.dx() == 0 ? squares[n][xy] : squares[xy][n];
                if (PolygonalMap2.instance
                    .canMoveBetween(
                        HLGlobals.mover, square1.getX(), square1.getY(), square1.getZ(), square1.getX() + dir.dx(), square1.getY() + dir.dy(), square1.getZ()
                    )) {
                    double cost = this.calculateCost(node, square1, chunkRegion2, square1.getAdjacentSquare(dir));
                    this.addSuccessor(cost, node, chunkRegion2, successors);
                }
            }
        }
    }

    void addSuccessorChunkRegionsAdjacentToSquare(HLSearchNode searchNode, Square square1, ArrayList<ISearchNode> successors) {
        for (int d = 0; d < 8; d++) {
            IsoDirections dir = IsoDirections.fromIndex(d);
            int x2 = square1.getX() + dir.dx();
            int y2 = square1.getY() + dir.dy();
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromSquarePos(x2, y2);
            if (chunk2 != null) {
                Square[][] squares2 = chunk2.getSquaresForLevel(square1.getZ());
                int x2m = PZMath.coordmodulo(x2, 8);
                int y2m = PZMath.coordmodulo(y2, 8);
                Square square2 = squares2[x2m][y2m];
                if (square2 != null
                    && !PolygonalMap2.instance
                        .canNotMoveBetween(HLGlobals.mover, square1.getX(), square1.getY(), square1.getZ(), square2.getX(), square2.getY(), square2.getZ())) {
                    HLChunkLevel levelData2 = chunk2.getLevelData(square1.getZ()).getHighLevelData();

                    for (int i = 0; i < levelData2.regionList.size(); i++) {
                        HLChunkRegion chunkRegion2 = levelData2.regionList.get(i);
                        if (chunkRegion2.containsSquare(x2, y2)) {
                            double cost = this.calculateCost(searchNode, square1, chunkRegion2, square2);
                            this.addSuccessor(cost, searchNode, chunkRegion2, successors);
                        }
                    }
                }
            }
        }
    }

    void getSuccessorsGoingUp(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLChunkRegion chunkRegion1 = searchNode.chunkRegion;
        HLChunkLevel levelData1 = chunkRegion1.levelData;
        Chunk chunk1 = chunkRegion1.getChunk();
        int level = levelData1.getLevel();
        levelData1.initStairsIfNeeded();
        Square[][] squares = chunk1.getSquaresForLevel(level);

        for (int i = 0; i < levelData1.stairs.size(); i++) {
            HLStaircase stair = levelData1.stairs.get(i);
            if (chunkRegion1.containsSquare(stair.getBottomFloorX(), stair.getBottomFloorY())) {
                Square square2 = squares[PZMath.coordmodulo(stair.getBottomFloorX(), 8)][PZMath.coordmodulo(stair.getBottomFloorY(), 8)];
                double cost = this.calculateCost(searchNode, square2);
                Node vgNode = this.getSquarePolygonalMapNode(square2);
                if (vgNode != null) {
                    this.addSuccessor(cost, searchNode, vgNode, successors);
                } else {
                    this.addSuccessor(cost, searchNode, stair, true, successors);
                }
            }
        }

        for (int ix = 0; ix < levelData1.slopedSurfaces.size(); ix++) {
            HLSlopedSurface slopedSurface = levelData1.slopedSurfaces.get(ix);
            if (chunkRegion1.containsSquare(slopedSurface.getBottomFloorX(), slopedSurface.getBottomFloorY())) {
                Square square2 = squares[PZMath.coordmodulo(slopedSurface.getBottomFloorX(), 8)][PZMath.coordmodulo(slopedSurface.getBottomFloorY(), 8)];
                double cost = this.calculateCost(searchNode, square2);
                Node vgNode = this.getSquarePolygonalMapNode(square2);
                if (vgNode != null) {
                    this.addSuccessor(cost, searchNode, vgNode, successors);
                } else {
                    this.addSuccessor(cost, searchNode, slopedSurface, true, successors);
                }
            }
        }
    }

    void getSuccessorsGoingDown(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLChunkRegion chunkRegion1 = searchNode.chunkRegion;
        HLChunkLevel levelData1 = chunkRegion1.levelData;
        Chunk chunk1 = chunkRegion1.getChunk();
        int level = levelData1.getLevel();
        this.getSuccessorsGoingDown_Staircases(searchNode, chunkRegion1, chunk1.wx, chunk1.wy, level, successors);
        this.getSuccessorsGoingDown_Staircases(searchNode, chunkRegion1, chunk1.wx, chunk1.wy + 1, level, successors);
        this.getSuccessorsGoingDown_Staircases(searchNode, chunkRegion1, chunk1.wx + 1, chunk1.wy, level, successors);
        this.getSuccessorsGoingDown_SlopedSurfaces(searchNode, chunkRegion1, chunk1.wx, chunk1.wy, level, successors);
        this.getSuccessorsGoingDown_SlopedSurfaces(searchNode, chunkRegion1, chunk1.wx, chunk1.wy + 1, level, successors);
        this.getSuccessorsGoingDown_SlopedSurfaces(searchNode, chunkRegion1, chunk1.wx, chunk1.wy - 1, level, successors);
        this.getSuccessorsGoingDown_SlopedSurfaces(searchNode, chunkRegion1, chunk1.wx + 1, chunk1.wy, level, successors);
        this.getSuccessorsGoingDown_SlopedSurfaces(searchNode, chunkRegion1, chunk1.wx - 1, chunk1.wy, level, successors);
    }

    void getSuccessorsGoingDown_Staircases(HLSearchNode searchNode, HLChunkRegion chunkRegion1, int wx, int wy, int level, ArrayList<ISearchNode> successors) {
        Chunk chunk1 = chunkRegion1.getChunk();
        Square[][] squares = chunk1.getSquaresForLevel(level);
        Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(wx, wy);
        if (chunk2 != null && chunk2.isValidLevel(level - 1)) {
            HLChunkLevel levelDataBelow = chunk2.getLevelData(level - 1).getHighLevelData();
            levelDataBelow.initStairsIfNeeded();

            for (int i = 0; i < levelDataBelow.stairs.size(); i++) {
                HLStaircase stairs = levelDataBelow.stairs.get(i);
                if (chunkRegion1.containsSquare(stairs.getTopFloorX(), stairs.getTopFloorY())) {
                    Square square2 = squares[PZMath.coordmodulo(stairs.getTopFloorX(), 8)][PZMath.coordmodulo(stairs.getTopFloorY(), 8)];
                    double cost = this.calculateCost(searchNode, square2);
                    Node vgNode = this.getSquarePolygonalMapNode(square2);
                    if (vgNode != null) {
                        this.addSuccessor(cost, searchNode, vgNode, successors);
                    } else {
                        this.addSuccessor(cost, searchNode, stairs, false, successors);
                    }
                }
            }
        }
    }

    void getSuccessorsGoingDown_SlopedSurfaces(
        HLSearchNode searchNode, HLChunkRegion chunkRegion1, int wx, int wy, int level, ArrayList<ISearchNode> successors
    ) {
        Chunk chunk1 = chunkRegion1.getChunk();
        Square[][] squares = chunk1.getSquaresForLevel(level);
        Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(wx, wy);
        if (chunk2 != null && chunk2.isValidLevel(level - 1)) {
            HLChunkLevel levelDataBelow = chunk2.getLevelData(level - 1).getHighLevelData();
            levelDataBelow.initStairsIfNeeded();

            for (int i = 0; i < levelDataBelow.slopedSurfaces.size(); i++) {
                HLSlopedSurface slopedSurface = levelDataBelow.slopedSurfaces.get(i);
                if (chunkRegion1.containsSquare(slopedSurface.getTopFloorX(), slopedSurface.getTopFloorY())) {
                    Square square2 = squares[PZMath.coordmodulo(slopedSurface.getTopFloorX(), 8)][PZMath.coordmodulo(slopedSurface.getTopFloorY(), 8)];
                    double cost = this.calculateCost(searchNode, square2);
                    Node vgNode = this.getSquarePolygonalMapNode(square2);
                    if (vgNode != null) {
                        this.addSuccessor(cost, searchNode, vgNode, successors);
                    } else {
                        this.addSuccessor(cost, searchNode, slopedSurface, false, successors);
                    }
                }
            }
        }
    }

    void getSuccessorVisibilityGraphs(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        HLChunkRegion chunkRegion1 = searchNode.chunkRegion;
        HLChunkLevel levelData1 = chunkRegion1.levelData;
        Chunk chunk1 = chunkRegion1.getChunk();
        int level = levelData1.getLevel();
        Square[][] squares1 = chunk1.getSquaresForLevel(level);
        int minX = chunk1.getMinX();
        int minY = chunk1.getMinY();
        int maxX = chunk1.getMaxX() + 1;
        int maxY = chunk1.getMaxY() + 1;
        this.visibilityGraphs.clear();
        PolygonalMap2.instance.getVisibilityGraphsOverlappingChunk(chunk1, level, this.visibilityGraphs);
        PolygonalMap2.instance.getVisibilityGraphsAdjacentToChunk(chunk1, level, this.visibilityGraphs);
        if (!this.visibilityGraphs.isEmpty()) {
            for (int i = 0; i < this.visibilityGraphs.size(); i++) {
                VisibilityGraph graph = this.visibilityGraphs.get(i);

                for (int j = 0; j < graph.perimeterNodes.size(); j++) {
                    Node vgNode = graph.perimeterNodes.get(j);
                    Square square2 = vgNode.square;
                    if (square2.isInside(minX - 1, minY - 1, maxX + 1, maxY + 1)) {
                        for (int d = 0; d < 8; d++) {
                            IsoDirections dir = IsoDirections.fromIndex(d);
                            int x2 = square2.getX() + dir.dx();
                            int y2 = square2.getY() + dir.dy();
                            if (chunkRegion1.containsSquare(x2, y2)
                                && PolygonalMap2.instance.canMoveBetween(HLGlobals.mover, x2, y2, level, square2.getX(), square2.getY(), level)) {
                                Square square1 = squares1[x2 - minX][y2 - minY];
                                double cost = this.calculateCost(searchNode, square1, square2.getX() + 0.5F, square2.getY() + 0.5F, square2);
                                this.addSuccessor(cost, searchNode, vgNode, successors);
                            }
                        }
                    }
                }
            }

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    Square square = squares1[x][y];
                    if (chunkRegion1.containsSquareLocal(x, y) && square != null) {
                        boolean bStairBN = square.has(256);
                        boolean bStairBW = square.has(32);
                        if (bStairBN || bStairBW) {
                            IsoDirections dir = bStairBN ? IsoDirections.N : IsoDirections.W;
                            Square square2 = square.getAdjacentSquare(dir.Rot180());
                            if (square2 != null) {
                                Node vgNode = PolygonalMap2.instance.getExistingNodeForSquare(square2);
                                if (vgNode != null
                                    && vgNode.hasFlag(16)
                                    && PolygonalMap2.instance
                                        .canMoveBetween(HLGlobals.mover, square.getX(), square.getY(), level, square2.getX(), square2.getY(), level)) {
                                    double cost = this.calculateCost(searchNode, square, square2.getX() + 0.5F, square2.getY() + 0.5F, square2);
                                    this.addSuccessor(cost, searchNode, vgNode, successors);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void getSuccessorsFromVisibilityGraph(HLSearchNode searchNode, ArrayList<ISearchNode> successors) {
        Node vgNode = searchNode.vgNode;
        if (vgNode.graphs != null) {
            for (int i = 0; i < vgNode.graphs.size(); i++) {
                VisibilityGraph graph = vgNode.graphs.get(i);
                if (!graph.isCreated()) {
                    graph.create();
                }
            }
        }

        for (int ix = 0; ix < vgNode.visible.size(); ix++) {
            Connection cxn = vgNode.visible.get(ix);
            Node visible = cxn.otherNode(vgNode);
            if (!this.shouldIgnoreNode(visible) && (HLGlobals.mover.canCrawl || !visible.hasFlag(2)) && (HLGlobals.mover.canThump || !cxn.has(2))) {
                double cost = IsoUtils.DistanceTo(vgNode.x, vgNode.y, visible.x, visible.y);
                this.addSuccessor(cost, searchNode, visible, successors);
            }
        }

        Square square1 = vgNode.square;
        if (square1 != null) {
            if (vgNode.hasFlag(8) || vgNode.hasFlag(16)) {
                this.addSuccessorChunkRegionsAdjacentToSquare(searchNode, square1, successors);
            }
        }
    }

    boolean shouldIgnoreNode(Node node) {
        if (node.hasFlag(16)) {
            for (int i = 0; i < node.visible.size(); i++) {
                Connection cxn = node.visible.get(i);
                if (!cxn.node1.hasFlag(16)) {
                    return false;
                }

                if (!cxn.node2.hasFlag(16)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    void getStaircasesInVisibilityGraph(VisibilityGraph graph, ArrayList<HLStaircase> staircases) {
        int level = graph.cluster.z;
        VehicleRect bounds = graph.cluster.bounds();
        int STAIR_SIZE = 4;
        int chunkMinX = PZMath.fastfloor((bounds.left() - 4) / 8.0F);
        int chunkMinY = PZMath.fastfloor((bounds.top() - 4) / 8.0F);
        int chunkMaxX = (int)PZMath.ceil((bounds.right() + 4) / 8.0F);
        int chunkMaxY = (int)PZMath.ceil((bounds.bottom() + 4) / 8.0F);

        for (int y = chunkMinY; y < chunkMaxY; y++) {
            for (int x = chunkMinX; x < chunkMaxX; x++) {
                Chunk chunk = PolygonalMap2.instance.getChunkFromChunkPos(x, y);
                if (chunk != null && chunk.isValidLevel(level)) {
                    HLChunkLevel levelData = chunk.getLevelData(level).getHighLevelData();

                    for (int i = 0; i < levelData.stairs.size(); i++) {
                        HLStaircase staircase = levelData.stairs.get(i);
                        if (staircase.getBottomFloorZ() == level) {
                            Square square = staircase.getBottomFloorSquare();
                            if (square != null && graph.contains(square)) {
                                staircases.add(staircase);
                            }
                        }
                    }

                    if (!chunk.isValidLevel(level - 1)) {
                        levelData = chunk.getLevelData(level - 1).getHighLevelData();

                        for (int ix = 0; ix < levelData.stairs.size(); ix++) {
                            HLStaircase staircase = levelData.stairs.get(ix);
                            if (staircase.getTopFloorZ() == level) {
                                Square square = staircase.getTopFloorSquare();
                                if (square != null && graph.contains(square)) {
                                    staircases.add(staircase);
                                }
                            }
                        }
                    }
                }
            }
        }

        bounds.release();
    }

    void addSuccessor(double cost, HLSearchNode node, HLChunkRegion chunkRegion, ArrayList<ISearchNode> successors) {
        if (chunkRegion != null) {
            HLSearchNode successorNode = this.getSearchNode(chunkRegion);
            if (!successors.contains(successorNode)) {
                successors.add(successorNode);
            }

            this.setLowestCostSuccessor(node, successorNode, cost);
        }
    }

    void addSuccessor(double cost, HLSearchNode node, HLLevelTransition levelTransition, boolean bBottomOfStaircase, ArrayList<ISearchNode> successors) {
        if (levelTransition != null) {
            HLSearchNode successorNode = this.getSearchNode(levelTransition, bBottomOfStaircase);
            if (!successors.contains(successorNode)) {
                successors.add(successorNode);
            }

            this.setLowestCostSuccessor(node, successorNode, cost);
        }
    }

    void addSuccessor(double cost, HLSearchNode node, Node vgNode, ArrayList<ISearchNode> successors) {
        HLSearchNode successorNode = this.getSearchNode(vgNode);
        if (!successors.contains(successorNode)) {
            successors.add(successorNode);
        }

        if (node.vgNode == null) {
            this.setLowestCostSuccessor(node, successorNode, cost);
        }
    }

    void setLowestCostSuccessor(HLSearchNode node, HLSearchNode successorNode, double cost) {
        HLSuccessor successor = PZArrayUtil.find(node.successors, hlSuccessor -> hlSuccessor.searchNode == successorNode);
        if (successor == null || !(successor.cost <= cost)) {
            if (successor == null) {
                successor = HLGlobals.successorPool.alloc();
                node.successors.add(successor);
            }

            successor.searchNode = successorNode;
            successor.cost = cost;
        }
    }

    double calculateCost(HLSearchNode node, Square square1, HLChunkRegion region2, Square square2) {
        return this.calculateCost(node, square1, (region2.minX + region2.maxX + 1) / 2.0F, (region2.minY + region2.maxY + 1) / 2.0F, square2);
    }

    double calculateCost(HLSearchNode node, Square square1, float node2X, float node2Y, Square square2) {
        float x1 = square1.getX() + 0.5F;
        float y1 = square1.getY() + 0.5F;
        float z1 = square1.getZ();
        float x2 = square1.getX() + 0.5F;
        float y2 = square1.getY() + 0.5F;
        float z2 = square1.getZ();
        double cost = IsoUtils.DistanceTo(node.getX(), node.getY(), x1, y1);
        cost += Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0) + Math.pow((z1 - z2) * 2.5F, 2.0));
        cost += IsoUtils.DistanceTo(x2, y2, node2X, node2Y);
        boolean bCrawlingZombie = HLGlobals.mover.isZombie() && HLGlobals.mover.crawling;
        boolean avoidWindows = !HLGlobals.mover.isZombie() || HLGlobals.mover.crawling;
        if (avoidWindows) {
            if (this.isAdjacentSquare(square1, square2, IsoDirections.N)) {
                if (!bCrawlingZombie && square1.isUnblockedWindowN()) {
                    cost += 20.0;
                } else if (square1.has(4096)) {
                    cost += 200.0;
                }
            } else if (this.isAdjacentSquare(square1, square2, IsoDirections.S)) {
                if (!bCrawlingZombie && square2.isUnblockedWindowN()) {
                    cost += 20.0;
                } else if (square2.has(4096)) {
                    cost += 200.0;
                }
            } else if (this.isAdjacentSquare(square1, square2, IsoDirections.W)) {
                if (!bCrawlingZombie && square1.isUnblockedWindowW()) {
                    cost += 20.0;
                } else if (square1.has(2048)) {
                    cost += 200.0;
                }
            } else if (this.isAdjacentSquare(square1, square2, IsoDirections.E)) {
                if (!bCrawlingZombie && square2.isUnblockedWindowW()) {
                    cost += 20.0;
                } else if (square2.has(2048)) {
                    cost += 200.0;
                }
            }
        }

        return cost;
    }

    double calculateCost(HLSearchNode node, Square square2) {
        return IsoUtils.DistanceTo(node.getX(), node.getY(), square2.getX() + 0.5F, square2.getY() + 0.5F);
    }

    boolean isAdjacentSquare(Square square1, Square square2, IsoDirections dir) {
        return square1.getX() + dir.dx() == square2.getX() && square1.getY() + dir.dy() == square2.getY() && square1.getZ() == square2.getZ();
    }

    HLSearchNode getSearchNode(HLChunkRegion chunkRegion) {
        HLSearchNode searchNode = this.nodeMapChunkRegion.get(chunkRegion);
        if (searchNode == null) {
            searchNode = HLGlobals.searchNodePool.alloc();
            searchNode.setG(0.0);
            searchNode.astar = this;
            searchNode.chunkRegion = chunkRegion;
            searchNode.levelTransition = null;
            searchNode.bottomOfStaircase = false;
            searchNode.vgNode = null;
            searchNode.unloadedX = searchNode.unloadedY = -1;
            searchNode.inUnloadedArea = false;
            searchNode.onEdgeOfLoadedArea = this.goalInUnloadedArea && searchNode.calculateOnEdgeOfLoadedArea();
            searchNode.parent = null;
            this.nodeMapChunkRegion.put(chunkRegion, searchNode);
        }

        return searchNode;
    }

    HLSearchNode getSearchNode(HLLevelTransition levelTransition, boolean bBottom) {
        HLSearchNode searchNode = this.nodeMapLevelTransition.get(levelTransition);
        if (searchNode == null) {
            searchNode = HLGlobals.searchNodePool.alloc();
            searchNode.setG(0.0);
            searchNode.astar = this;
            searchNode.chunkRegion = null;
            searchNode.levelTransition = levelTransition;
            searchNode.bottomOfStaircase = bBottom;
            searchNode.vgNode = null;
            searchNode.unloadedX = searchNode.unloadedY = -1;
            searchNode.inUnloadedArea = false;
            searchNode.onEdgeOfLoadedArea = this.goalInUnloadedArea && searchNode.calculateOnEdgeOfLoadedArea();
            searchNode.parent = null;
            this.nodeMapLevelTransition.put(levelTransition, searchNode);
        }

        return searchNode;
    }

    HLSearchNode getSearchNode(Node vgNode) {
        HLSearchNode searchNode = this.nodeMapVisGraph.get(vgNode);
        if (searchNode == null) {
            searchNode = HLGlobals.searchNodePool.alloc();
            searchNode.setG(0.0);
            searchNode.astar = this;
            searchNode.chunkRegion = null;
            searchNode.levelTransition = null;
            searchNode.bottomOfStaircase = false;
            searchNode.vgNode = vgNode;
            searchNode.unloadedX = searchNode.unloadedY = -1;
            searchNode.inUnloadedArea = false;
            searchNode.onEdgeOfLoadedArea = this.goalInUnloadedArea && searchNode.calculateOnEdgeOfLoadedArea();
            searchNode.parent = null;
            this.nodeMapVisGraph.put(vgNode, searchNode);
        }

        return searchNode;
    }

    HLSearchNode getSearchNode(int unloadedX, int unloadedY) {
        if (this.unloadedSearchNode == null) {
            HLSearchNode searchNode = HLGlobals.searchNodePool.alloc();
            searchNode.setG(0.0);
            searchNode.astar = this;
            searchNode.chunkRegion = null;
            searchNode.levelTransition = null;
            searchNode.bottomOfStaircase = false;
            searchNode.vgNode = null;
            searchNode.unloadedX = unloadedX;
            searchNode.unloadedY = unloadedY;
            searchNode.inUnloadedArea = true;
            searchNode.onEdgeOfLoadedArea = false;
            searchNode.parent = null;
            this.unloadedSearchNode = searchNode;
        }

        return this.unloadedSearchNode;
    }

    void addChunkLevelAndAdjacentToList(HLChunkLevel levelData, ArrayList<HLChunkLevel> chunkList) {
        if (!chunkList.contains(levelData)) {
            chunkList.add(levelData);
        }

        int wx = levelData.getChunk().wx;
        int wy = levelData.getChunk().wy;
        int level = levelData.getLevel();

        for (int i = 0; i < 8; i++) {
            IsoDirections dir = IsoDirections.fromIndex(i);
            Chunk chunk2 = PolygonalMap2.instance.getChunkFromChunkPos(wx + dir.dx(), wy + dir.dy());
            if (chunk2 != null && chunk2.isValidLevel(level)) {
                HLChunkLevel levelData2 = chunk2.getLevelData(level).getHighLevelData();
                if (!chunkList.contains(levelData2)) {
                    chunkList.add(levelData2);
                }
            }
        }
    }

    void addLevelTransitionChunkLevelData(HLLevelTransition levelTransition, ArrayList<HLChunkLevel> chunkList) {
        this.addLevelTransitionChunkLevelData(
            levelTransition, chunkList, levelTransition.getBottomFloorX(), levelTransition.getBottomFloorY(), levelTransition.getBottomFloorZ()
        );
        this.addLevelTransitionChunkLevelData(
            levelTransition, chunkList, levelTransition.getTopFloorX(), levelTransition.getTopFloorY(), levelTransition.getTopFloorZ()
        );
    }

    void addLevelTransitionChunkLevelData(HLLevelTransition levelTransition, ArrayList<HLChunkLevel> chunkList, int x, int y, int z) {
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(x, y);
        if (chunk != null) {
            HLChunkLevel levelData = chunk.getLevelData(z).getHighLevelData();
            this.addChunkLevelAndAdjacentToList(levelData, chunkList);
        }
    }

    static final class AdjustVisibilityGraphData {
        AdjustStartEndNodeData adjustStartEndNodeData = new AdjustStartEndNodeData();
        boolean adjusted;
        VisibilityGraph graph;
        Node vgNode;
    }
}
