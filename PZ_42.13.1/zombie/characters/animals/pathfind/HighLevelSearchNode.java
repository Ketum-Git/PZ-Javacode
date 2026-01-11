// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import zombie.popman.ObjectPool;

public final class HighLevelSearchNode extends ASearchNode {
    static int nextID = 1;
    Integer id = nextID++;
    HighLevelSearchNode parent;
    HighLevelAStar astar;
    Mesh mesh;
    static final ObjectPool<HighLevelSearchNode> pool = new ObjectPool<>(HighLevelSearchNode::new);

    HighLevelSearchNode() {
    }

    @Override
    public double h() {
        float x1 = this.getX();
        float y1 = this.getY();
        float x2 = this.astar.goalNode.searchNode.getX();
        float y2 = this.astar.goalNode.searchNode.getY();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
    }

    @Override
    public double c(ISearchNode successor) {
        HighLevelSearchNode other = (HighLevelSearchNode)successor;
        float x1 = this.getX();
        float y1 = this.getY();
        float x2 = other.getX();
        float y2 = other.getY();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0));
    }

    @Override
    public void getSuccessors(ArrayList<ISearchNode> successors) {
        this.astar.getSuccessors(this, successors);
    }

    @Override
    public ISearchNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(ISearchNode parent) {
        this.parent = (HighLevelSearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    float getX() {
        return this.mesh.centroidX;
    }

    float getY() {
        return this.mesh.centroidY;
    }

    float getZ() {
        return this.mesh.meshList.z;
    }
}
