// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import astar.ASearchNode;
import astar.ISearchNode;
import java.util.ArrayList;
import zombie.iso.IsoUtils;
import zombie.pathfind.Node;
import zombie.util.list.PZArrayUtil;

public class HLSearchNode extends ASearchNode {
    static final int CPW = 8;
    static int nextID = 1;
    Integer id;
    HLSearchNode parent;
    HLAStar astar;
    HLChunkRegion chunkRegion;
    HLLevelTransition levelTransition;
    boolean bottomOfStaircase;
    Node vgNode;
    int unloadedX = -1;
    int unloadedY = -1;
    boolean inUnloadedArea;
    boolean onEdgeOfLoadedArea;
    final ArrayList<HLSuccessor> successors = new ArrayList<>();

    HLSearchNode() {
        this.id = nextID++;
    }

    @Override
    public double h() {
        float x1 = this.getX();
        float y1 = this.getY();
        float z1 = this.getZ();
        float x2 = this.astar.goalNode.searchNode.getX();
        float y2 = this.astar.goalNode.searchNode.getY();
        float z2 = this.astar.goalNode.searchNode.getZ();
        return Math.sqrt(Math.pow(x1 - x2, 2.0) + Math.pow(y1 - y2, 2.0) + Math.pow((z1 - z2) * 2.5F, 2.0));
    }

    @Override
    public double c(ISearchNode successor) {
        HLSearchNode other = (HLSearchNode)successor;
        if (other.inUnloadedArea) {
            return IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), other.getX(), other.getY(), other.getZ());
        } else if (this.vgNode != null && other.vgNode != null) {
            return IsoUtils.DistanceTo(this.getX(), this.getY(), this.getZ(), other.getX(), other.getY(), other.getZ());
        } else {
            HLSuccessor successor1 = PZArrayUtil.find(this.successors, hlSuccessor -> hlSuccessor.searchNode == other);
            return successor1.cost;
        }
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
        this.parent = (HLSearchNode)parent;
    }

    @Override
    public Integer keyCode() {
        return this.id;
    }

    float getX() {
        if (this.chunkRegion != null) {
            return (this.chunkRegion.minX + this.chunkRegion.maxX + 1) / 2.0F;
        } else if (this.levelTransition != null) {
            return this.levelTransition.getSearchNodeX(this.bottomOfStaircase);
        } else {
            return this.vgNode != null ? this.vgNode.x : this.unloadedX;
        }
    }

    float getY() {
        if (this.chunkRegion != null) {
            return (this.chunkRegion.minY + this.chunkRegion.maxY + 1) / 2.0F;
        } else if (this.levelTransition != null) {
            return this.levelTransition.getSearchNodeY(this.bottomOfStaircase);
        } else {
            return this.vgNode != null ? this.vgNode.y : this.unloadedY;
        }
    }

    float getZ() {
        if (this.chunkRegion != null) {
            return this.chunkRegion.getLevel();
        } else if (this.levelTransition != null) {
            return this.bottomOfStaircase ? this.levelTransition.getBottomFloorZ() : this.levelTransition.getTopFloorZ();
        } else {
            return this.vgNode != null ? this.vgNode.z : 32.0F;
        }
    }

    boolean calculateOnEdgeOfLoadedArea() {
        if (this.chunkRegion != null) {
            return this.chunkRegion.isOnEdgeOfLoadedArea();
        } else if (this.levelTransition != null) {
            return this.levelTransition.isOnEdgeOfLoadedArea();
        } else {
            return this.vgNode != null ? this.vgNode.isOnEdgeOfLoadedArea() : false;
        }
    }
}
