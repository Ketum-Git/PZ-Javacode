// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.astar;

public interface IPathfinder {
    void Failed(Mover mover);

    void Succeeded(Path path, Mover mover);

    String getName();
}
