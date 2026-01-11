// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;

final class PathRequestTask {
    PolygonalMap2 map;
    PathFindRequest request;
    static final ArrayDeque<PathRequestTask> pool = new ArrayDeque<>();

    PathRequestTask init(PolygonalMap2 map, PathFindRequest request) {
        this.map = map;
        this.request = request;
        return this;
    }

    void execute() {
        if (this.request.mover instanceof IsoPlayer) {
            this.map.requests.playerQ.add(this.request);
        } else if (this.request.mover instanceof IsoZombie isoZombie && isoZombie.target != null) {
            this.map.requests.aggroZombieQ.add(this.request);
        } else {
            this.map.requests.otherQ.add(this.request);
        }
    }

    static PathRequestTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new PathRequestTask() : pool.pop();
        }
    }

    public void release() {
        synchronized (pool) {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
