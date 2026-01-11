// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ai.astar.Mover;
import zombie.core.logger.ExceptionLogger;
import zombie.pathfind.Path;
import zombie.pathfind.PathNode;

public class PathfindNativeThread extends Thread {
    public static PathfindNativeThread instance;
    public boolean stop;
    public final Object notifier = new Object();
    public final Object renderLock = new Object();
    protected final ConcurrentLinkedQueue<IPathfindTask> chunkTaskQueue = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<SquareUpdateTask> squareTaskQueue = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<IPathfindTask> vehicleTaskQueue = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<PathRequestTask> requestTaskQueue = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<IPathfindTask> taskReturnQueue = new ConcurrentLinkedQueue<>();
    private final RequestQueue requests = new RequestQueue();
    protected final HashMap<Mover, PathFindRequest> requestMap = new HashMap<>();
    protected final ConcurrentLinkedQueue<PathFindRequest> requestToMain = new ConcurrentLinkedQueue<>();
    protected final Path shortestPath = new Path();
    protected final ByteBuffer pathBb = ByteBuffer.allocateDirect(3072);
    private final PathfindNativeThread.Sync sync = new PathfindNativeThread.Sync();

    PathfindNativeThread() {
        this.pathBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void run() {
        while (!this.stop) {
            try {
                this.runInner();
            } catch (Throwable var2) {
                ExceptionLogger.logException(var2);
            }
        }
    }

    private void runInner() {
        this.sync.startFrame();
        synchronized (this.renderLock) {
            this.updateThread();
        }

        this.sync.endFrame();

        while (this.shouldWait()) {
            synchronized (this.notifier) {
                try {
                    this.notifier.wait();
                } catch (InterruptedException var4) {
                }
            }
        }
    }

    private void updateThread() {
        int update = 10;

        for (IPathfindTask task = this.chunkTaskQueue.poll(); task != null; task = this.chunkTaskQueue.poll()) {
            task.execute();
            this.taskReturnQueue.add(task);
            if (task instanceof ChunkUpdateTask) {
                if (--update <= 0) {
                    break;
                }
            }
        }

        for (SquareUpdateTask taskx = this.squareTaskQueue.poll(); taskx != null; taskx = this.squareTaskQueue.poll()) {
            taskx.execute();
            this.taskReturnQueue.add(taskx);
        }

        for (IPathfindTask taskx = this.vehicleTaskQueue.poll(); taskx != null; taskx = this.vehicleTaskQueue.poll()) {
            taskx.execute();
            this.taskReturnQueue.add(taskx);
        }

        for (PathRequestTask taskx = this.requestTaskQueue.poll(); taskx != null; taskx = this.requestTaskQueue.poll()) {
            taskx.execute();
            this.taskReturnQueue.add(taskx);
        }

        PathfindNative.update();
        int requestsPerUpdate = 2;

        while (!this.requests.isEmpty()) {
            PathFindRequest request = this.requests.removeFirst();
            if (request.cancel) {
                this.requestToMain.add(request);
            } else {
                try {
                    this.findPath(request);
                    if (!request.targetXyz.isEmpty()) {
                        this.findShortestPathOfMultiple(request);
                    }
                } catch (Throwable var5) {
                    ExceptionLogger.logException(var5);
                    request.path.clear();
                }

                this.requestToMain.add(request);
                if (--requestsPerUpdate == 0) {
                    break;
                }
            }
        }
    }

    private boolean shouldWait() {
        if (this.stop) {
            return false;
        } else if (!this.chunkTaskQueue.isEmpty()) {
            return false;
        } else if (!this.squareTaskQueue.isEmpty()) {
            return false;
        } else if (!this.vehicleTaskQueue.isEmpty()) {
            return false;
        } else {
            return !this.requestTaskQueue.isEmpty() ? false : this.requests.isEmpty();
        }
    }

    void wake() {
        synchronized (this.notifier) {
            this.notifier.notify();
        }
    }

    public void addRequest(PathFindRequest request, int queueNumber) {
        if (queueNumber == 1) {
            this.requests.playerQ.add(request);
        } else if (queueNumber == 2) {
            this.requests.aggroZombieQ.add(request);
        } else {
            this.requests.otherQ.add(request);
        }
    }

    private void findPath(PathFindRequest request) {
        this.pathBb.clear();
        int result = PathfindNative.instance.findPath(request, this.pathBb, request.doNotRelease);
        int count = 0;
        if (result == 1) {
            count = this.pathBb.getShort();
            this.pathBb.limit(2 + count * 3 * 4);
        }

        request.path.clear();

        for (int i = 0; i < count; i++) {
            float x = this.pathBb.getFloat();
            float y = this.pathBb.getFloat();
            float z = this.pathBb.getFloat() - 32.0F;
            request.path.addNode(x, y, z);
        }

        if (count == 1) {
            PathNode node = request.path.getNode(0);
            request.path.addNode(node.x, node.y, node.z);
        }

        if (request.path.size() < 2) {
            request.path.clear();
        }
    }

    private void findShortestPathOfMultiple(PathFindRequest request) {
        this.shortestPath.copyFrom(request.path);
        float targetX = request.targetX;
        float targetY = request.targetY;
        float targetZ = request.targetZ;
        float minLength = this.shortestPath.isEmpty() ? Float.MAX_VALUE : this.shortestPath.length();

        for (int i = 0; i < request.targetXyz.size(); i += 3) {
            request.targetX = request.targetXyz.get(i);
            request.targetY = request.targetXyz.get(i + 1);
            request.targetZ = request.targetXyz.get(i + 2);
            request.path.clear();
            this.findPath(request);
            if (!request.path.isEmpty()) {
                float length = request.path.length();
                if (length < minLength) {
                    minLength = length;
                    this.shortestPath.copyFrom(request.path);
                    targetX = request.targetX;
                    targetY = request.targetY;
                    targetZ = request.targetZ;
                }
            }
        }

        request.path.copyFrom(this.shortestPath);
        request.targetX = targetX;
        request.targetY = targetY;
        request.targetZ = targetZ;
    }

    public void stopThread() {
        this.stop = true;
        this.wake();

        while (this.isAlive()) {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException var2) {
            }
        }
    }

    public void cleanup() {
        for (IPathfindTask task = this.chunkTaskQueue.poll(); task != null; task = this.chunkTaskQueue.poll()) {
            task.release();
        }

        for (SquareUpdateTask task = this.squareTaskQueue.poll(); task != null; task = this.squareTaskQueue.poll()) {
            task.release();
        }

        for (IPathfindTask task = this.vehicleTaskQueue.poll(); task != null; task = this.vehicleTaskQueue.poll()) {
            task.release();
        }

        for (PathRequestTask task = this.requestTaskQueue.poll(); task != null; task = this.requestTaskQueue.poll()) {
            task.release();
        }

        for (IPathfindTask task = this.taskReturnQueue.poll(); task != null; task = this.taskReturnQueue.poll()) {
            task.release();
        }

        while (!this.requests.isEmpty()) {
            PathFindRequest request = this.requests.removeLast();
            if (!request.doNotRelease) {
                request.release();
            }
        }

        while (!this.requestToMain.isEmpty()) {
            PathFindRequest request = this.requestToMain.remove();
            if (!request.doNotRelease) {
                request.release();
            }
        }

        this.requestMap.clear();
    }

    private static class Sync {
        private final int fps = 20;
        private final long period = 50000000L;
        private long excess;
        private long beforeTime = System.nanoTime();
        private long overSleepTime;

        void begin() {
            this.beforeTime = System.nanoTime();
            this.overSleepTime = 0L;
        }

        void startFrame() {
            this.excess = 0L;
        }

        void endFrame() {
            long afterTime = System.nanoTime();
            long timeDiff = afterTime - this.beforeTime;
            long sleepTime = 50000000L - timeDiff - this.overSleepTime;
            if (sleepTime > 0L) {
                try {
                    Thread.sleep(sleepTime / 1000000L);
                } catch (InterruptedException var8) {
                }

                this.overSleepTime = System.nanoTime() - afterTime - sleepTime;
            } else {
                this.excess -= sleepTime;
                this.overSleepTime = 0L;
            }

            this.beforeTime = System.nanoTime();
        }
    }
}
