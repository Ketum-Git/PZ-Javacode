// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;

public final class HighLevelGoalNode implements IGoalNode {
    HighLevelSearchNode searchNode;

    HighLevelGoalNode init(HighLevelSearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}
