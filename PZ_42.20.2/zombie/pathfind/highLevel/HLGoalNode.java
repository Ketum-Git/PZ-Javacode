// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import astar.IGoalNode;
import astar.ISearchNode;

public class HLGoalNode implements IGoalNode {
    HLSearchNode searchNode;

    HLGoalNode init(HLSearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}
