// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import zombie.ai.astar.Mover;

public final class TestRequest implements IPathfinder {
    public final Path path = new Path();
    public boolean done;

    @Override
    public void Succeeded(Path path, Mover mover) {
        this.path.copyFrom(path);
        this.done = true;
    }

    @Override
    public void Failed(Mover mover) {
        this.path.clear();
        this.done = true;
    }
}
