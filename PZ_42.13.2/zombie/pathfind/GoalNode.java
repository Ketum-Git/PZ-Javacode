// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import astar.IGoalNode;
import astar.ISearchNode;

final class GoalNode implements IGoalNode {
    SearchNode searchNode;

    GoalNode init(SearchNode node) {
        this.searchNode = node;
        return this;
    }

    @Override
    public boolean inGoal(ISearchNode other) {
        return other == this.searchNode;
    }
}
