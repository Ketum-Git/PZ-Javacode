// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import astar.AStar;
import astar.ISearchNode;
import java.util.ArrayList;
import java.util.HashMap;

public final class HighLevelAStar extends AStar {
    private final MeshList meshList;
    HighLevelSearchNode initialNode;
    HighLevelGoalNode goalNode;
    HashMap<Mesh, HighLevelSearchNode> nodeMap = new HashMap<>();

    public HighLevelAStar(MeshList meshList) {
        this.meshList = meshList;
    }

    void findPath(float x1, float y1, int z1, float x2, float y2, int z2) {
        Mesh mesh1 = this.meshList.getMeshAt(x1, y1, z1);
        Mesh mesh2 = this.meshList.getMeshAt(x2, y2, z2);
        if (mesh1 != null && mesh2 != null) {
            HighLevelSearchNode initialNode = HighLevelSearchNode.pool.alloc();
            initialNode.parent = null;
            initialNode.astar = this;
            initialNode.mesh = mesh1;
            HighLevelSearchNode goalNode1 = HighLevelSearchNode.pool.alloc();
            goalNode1.parent = null;
            goalNode1.astar = this;
            goalNode1.mesh = mesh2;
            HighLevelGoalNode goalNode = new HighLevelGoalNode();
            goalNode.init(goalNode1);
            this.initialNode = initialNode;
            this.goalNode = goalNode;
            HighLevelSearchNode.pool.releaseAll(new ArrayList<>(this.nodeMap.values()));
            this.nodeMap.clear();
            this.nodeMap.put(initialNode.mesh, initialNode);
            this.nodeMap.put(goalNode1.mesh, goalNode1);
            ArrayList<ISearchNode> path = this.shortestPath(initialNode, goalNode);
            if (path != null) {
                for (ISearchNode node : path) {
                    HighLevelSearchNode var15 = (HighLevelSearchNode)node;
                }
            }

            HighLevelSearchNode.pool.releaseAll(new ArrayList<>(this.nodeMap.values()));
            this.nodeMap.clear();
        }
    }

    void getSuccessors(HighLevelSearchNode searchNode, ArrayList<ISearchNode> successors) {
        AnimalPathfind.getInstance().cdAStar.initOffMeshConnections(searchNode.mesh);
        if (!searchNode.mesh.offMeshConnections.isEmpty()) {
            for (OffMeshConnection omc : searchNode.mesh.offMeshConnections) {
                this.addSuccessor(omc.meshTo, successors);
            }
        }
    }

    void addSuccessor(Mesh mesh, ArrayList<ISearchNode> successors) {
        HighLevelSearchNode node = this.getSearchNode(mesh);
        if (!successors.contains(node)) {
            successors.add(node);
        }
    }

    HighLevelSearchNode getSearchNode(Mesh mesh) {
        HighLevelSearchNode searchNode = this.nodeMap.get(mesh);
        if (searchNode == null) {
            searchNode = HighLevelSearchNode.pool.alloc();
            searchNode.astar = this;
            searchNode.mesh = mesh;
            this.nodeMap.put(mesh, searchNode);
        }

        return searchNode;
    }
}
