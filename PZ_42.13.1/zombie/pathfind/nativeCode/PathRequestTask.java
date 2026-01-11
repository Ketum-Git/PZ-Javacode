// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.popman.ObjectPool;

class PathRequestTask implements IPathfindTask {
    PathFindRequest request;
    int queueNumber;
    static final ObjectPool<PathRequestTask> pool = new ObjectPool<>(PathRequestTask::new);

    PathRequestTask init(PathFindRequest request) {
        this.request = request;
        if (request.mover instanceof IsoPlayer && !(request.mover instanceof IsoAnimal)) {
            this.queueNumber = 1;
        } else if (request.mover instanceof IsoZombie isoZombie && isoZombie.target != null) {
            this.queueNumber = 2;
        } else {
            this.queueNumber = 3;
        }

        return this;
    }

    @Override
    public void execute() {
        PathfindNativeThread.instance.addRequest(this.request, this.queueNumber);
    }

    static PathRequestTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}
