// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.gameStates.IngameState;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.MapFiles;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.popman.ZombiePopulationManager;

public final class MapCollisionData {
    public static final MapCollisionData instance = new MapCollisionData();
    public static final byte BIT_SOLID = 1;
    public static final byte BIT_WALLN = 2;
    public static final byte BIT_WALLW = 4;
    public static final byte BIT_WATER = 8;
    public static final byte BIT_ROOM = 16;
    private static final int SQUARES_PER_CHUNK = 8;
    private static final int CHUNKS_PER_CELL = 32;
    private static final int SQUARES_PER_CELL = 256;
    private static final int[] curXY = new int[2];
    public final Object renderLock = new Object();
    private final Stack<MapCollisionData.PathTask> freePathTasks = new Stack<>();
    private final ConcurrentLinkedQueue<MapCollisionData.PathTask> pathTaskQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MapCollisionData.PathTask> pathResultQueue = new ConcurrentLinkedQueue<>();
    private final MapCollisionData.Sync sync = new MapCollisionData.Sync();
    private final byte[] squares = new byte[64];
    private static final int SQUARE_UPDATE_SIZE = 9;
    private final ByteBuffer squareUpdateBuffer = ByteBuffer.allocateDirect(1024);
    private boolean client;
    private boolean paused;
    private boolean noSave;
    private MapCollisionData.MCDThread thread;
    private long lastUpdate;

    private static native void n_init(int var0, int var1, int var2, int var3);

    private static native void n_chunkUpdateTask(int var0, int var1, byte[] var2);

    private static native void n_squareUpdateTask(int var0, ByteBuffer var1);

    private static native int n_pathTask(int var0, int var1, int var2, int var3, int[] var4);

    private static native boolean n_hasDataForThread();

    private static native boolean n_shouldWait();

    private static native void n_update();

    private static native void n_save();

    private static native void n_stop();

    private static native void n_setGameState(String var0, boolean var1);

    private static native void n_setGameState(String var0, double var1);

    private static native void n_setGameState(String var0, float var1);

    private static native void n_setGameState(String var0, int var1);

    private static native void n_setGameState(String var0, String var1);

    private static native void n_initMetaGrid(int var0, int var1, int var2, int var3);

    private static native void n_initMetaCell(int var0, int var1, String var2);

    private static native void n_initMetaChunk(int var0, int var1, int var2, int var3, int var4);

    private static void writeToStdErr(String message) {
        System.err.println(message);
    }

    public void init(IsoMetaGrid metaGrid) {
        this.client = GameClient.client;
        if (!this.client) {
            int minX = metaGrid.minNonProceduralX;
            int minY = metaGrid.minNonProceduralY;
            int width = metaGrid.maxNonProceduralX - minX + 1;
            int height = metaGrid.maxNonProceduralY - minY + 1;
            minX = metaGrid.minX;
            minY = metaGrid.minY;
            width = metaGrid.getWidth();
            height = metaGrid.getHeight();
            n_setGameState("Core.GameMode", Core.getInstance().getGameMode());
            n_setGameState("Core.GameSaveWorld", Core.gameSaveWorld);
            n_setGameState("Core.bLastStand", Core.lastStand);
            n_setGameState("Core.noSave", this.noSave = Core.getInstance().isNoSave());
            n_setGameState("GameWindow.CacheDir", ZomboidFileSystem.instance.getCacheDir());
            n_setGameState("GameWindow.GameModeCacheDir", ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator);
            n_setGameState("GameWindow.SaveDir", ZomboidFileSystem.instance.getSaveDir());
            n_setGameState("SandboxOptions.Distribution", SandboxOptions.instance.distribution.getValue());
            n_setGameState("SandboxOptions.Zombies", SandboxOptions.instance.zombies.getValue());
            n_setGameState("SavefileNaming.SUBDIR_CHUNKDATA", "chunkdata");
            n_setGameState("SavefileNaming.SUBDIR_ZPOP", "zpop");
            n_setGameState("World.ZombiesDisabled", IsoWorld.getZombiesDisabled());
            n_setGameState("PAUSED", this.paused = true);
            n_initMetaGrid(minX, minY, width, height);

            for (int cy = minY; cy < minY + height; cy++) {
                for (int cx = minX; cx < minX + width; cx++) {
                    IsoMetaCell metaCell = metaGrid.getCellData(cx, cy);

                    for (int i = 0; i < IsoLot.MapFiles.size(); i++) {
                        MapFiles mapFiles = IsoLot.MapFiles.get(i);
                        n_initMetaCell(cx, cy, mapFiles.infoFileNames.get("chunkdata_" + cx + "_" + cy + ".bin"));
                    }

                    if (metaCell != null) {
                        for (int wy = 0; wy < 32; wy++) {
                            for (int wx = 0; wx < 32; wx++) {
                                int intensity = LotHeader.getZombieIntensityForChunk(metaCell.info, wx, wy);
                                n_initMetaChunk(cx, cy, wx, wy, Math.max(intensity, 0));
                            }
                        }
                    }
                }
            }

            n_init(minX, minY, width, height);
        }
    }

    public void start() {
        if (!this.client) {
            if (this.thread == null) {
                this.thread = new MapCollisionData.MCDThread();
                this.thread.setDaemon(true);
                this.thread.setName("MapCollisionDataJNI");
                if (GameServer.server) {
                    this.thread.start();
                }
            }
        }
    }

    public void startGame() {
        if (!GameClient.client) {
            this.updateMain();
            ZombiePopulationManager.instance.updateMain();
            n_update();
            ZombiePopulationManager.instance.updateThread();
            this.updateMain();
            ZombiePopulationManager.instance.updateMain();
            this.thread.start();
        }
    }

    public void updateMain() {
        if (!this.client) {
            for (MapCollisionData.PathTask task = this.pathResultQueue.poll(); task != null; task = this.pathResultQueue.poll()) {
                task.result.finished(task.status, task.curX, task.curY);
                task.release();
            }

            long ms = System.currentTimeMillis();
            if (ms - this.lastUpdate > 10000L) {
                this.lastUpdate = ms;
                this.notifyThread();
            }
        }
    }

    public boolean hasDataForThread() {
        if (this.squareUpdateBuffer.position() > 0) {
            try {
                n_squareUpdateTask(this.squareUpdateBuffer.position() / 9, this.squareUpdateBuffer);
            } finally {
                this.squareUpdateBuffer.clear();
            }
        }

        return n_hasDataForThread();
    }

    public void updateGameState() {
        boolean noSave = Core.getInstance().isNoSave();
        if (this.noSave != noSave) {
            this.noSave = noSave;
            n_setGameState("Core.noSave", this.noSave);
        }

        boolean paused = GameTime.isGamePaused();
        if (GameWindow.states.current != IngameState.instance) {
            paused = true;
        }

        if (GameServer.server) {
            paused = IngameState.instance.paused;
        }

        if (paused != this.paused) {
            this.paused = paused;
            n_setGameState("PAUSED", this.paused);
        }
    }

    public void notifyThread() {
        synchronized (this.thread.notifier) {
            this.thread.notifier.notify();
        }
    }

    public void addChunkToWorld(IsoChunk chunk) {
        if (!this.client) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    IsoGridSquare sq = chunk.getGridSquare(x, y, 0);
                    if (sq == null) {
                        this.squares[x + y * 8] = 1;
                    } else {
                        byte bits = 0;
                        if (this.isSolid(sq)) {
                            bits = (byte)(bits | 1);
                        }

                        if (this.isBlockedN(sq)) {
                            bits = (byte)(bits | 2);
                        }

                        if (this.isBlockedW(sq)) {
                            bits = (byte)(bits | 4);
                        }

                        if (this.isWater(sq)) {
                            bits = (byte)(bits | 8);
                        }

                        if (this.isRoom(sq)) {
                            bits = (byte)(bits | 16);
                        }

                        this.squares[x + y * 8] = bits;
                    }
                }
            }

            n_chunkUpdateTask(chunk.wx, chunk.wy, this.squares);
        }
    }

    public void removeChunkFromWorld(IsoChunk chunk) {
        if (!this.client) {
            ;
        }
    }

    public void squareChanged(IsoGridSquare sq) {
        if (!this.client) {
            try {
                byte bits = 0;
                if (this.isSolid(sq)) {
                    bits = (byte)(bits | 1);
                }

                if (this.isBlockedN(sq)) {
                    bits = (byte)(bits | 2);
                }

                if (this.isBlockedW(sq)) {
                    bits = (byte)(bits | 4);
                }

                if (this.isWater(sq)) {
                    bits = (byte)(bits | 8);
                }

                if (this.isRoom(sq)) {
                    bits = (byte)(bits | 16);
                }

                this.squareUpdateBuffer.putInt(sq.x);
                this.squareUpdateBuffer.putInt(sq.y);
                this.squareUpdateBuffer.put(bits);
                if (this.squareUpdateBuffer.remaining() < 9) {
                    n_squareUpdateTask(this.squareUpdateBuffer.position() / 9, this.squareUpdateBuffer);
                    this.squareUpdateBuffer.clear();
                }
            } catch (Exception var3) {
                ExceptionLogger.logException(var3);
            }
        }
    }

    public void save() {
        if (!this.client) {
            ZombiePopulationManager.instance.beginSaveRealZombies();
            if (!this.thread.isAlive()) {
                n_save();
                ZombiePopulationManager.instance.save();
            } else {
                this.thread.save = true;
                synchronized (this.thread.notifier) {
                    this.thread.notifier.notify();
                }

                while (this.thread.save) {
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException var3) {
                    }
                }

                ZombiePopulationManager.instance.endSaveRealZombies();
            }
        }
    }

    public void stop() {
        if (!this.client) {
            this.thread.stop = true;
            synchronized (this.thread.notifier) {
                this.thread.notifier.notify();
            }

            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(5L);
                } catch (InterruptedException var3) {
                }
            }

            n_stop();
            this.thread = null;
            this.pathTaskQueue.clear();
            this.pathResultQueue.clear();
            this.squareUpdateBuffer.clear();
        }
    }

    private boolean isSolid(IsoGridSquare sq) {
        boolean solid = sq.isSolid() || sq.isSolidTrans();
        if (sq.HasStairs()) {
            solid = true;
        }

        if (sq.has(IsoFlagType.water)) {
            solid = false;
        }

        if (sq.has(IsoObjectType.tree)) {
            solid = false;
        }

        return solid;
    }

    private boolean isBlockedN(IsoGridSquare sq) {
        if (sq.has(IsoFlagType.HoppableN)) {
            return false;
        } else {
            boolean blocked = sq.has(IsoFlagType.collideN);
            if (sq.has(IsoObjectType.doorFrN)) {
                blocked = true;
            }

            if (sq.getProperties().has(IsoFlagType.DoorWallN)) {
                blocked = true;
            }

            if (sq.has(IsoObjectType.windowFN)) {
                blocked = true;
            }

            if (sq.has(IsoFlagType.windowN)) {
                blocked = true;
            }

            if (sq.getProperties().has(IsoFlagType.WindowN)) {
                blocked = true;
            }

            return blocked;
        }
    }

    private boolean isBlockedW(IsoGridSquare sq) {
        if (sq.has(IsoFlagType.HoppableW)) {
            return false;
        } else {
            boolean blocked = sq.has(IsoFlagType.collideW);
            if (sq.has(IsoObjectType.doorFrW)) {
                blocked = true;
            }

            if (sq.getProperties().has(IsoFlagType.DoorWallW)) {
                blocked = true;
            }

            if (sq.has(IsoObjectType.windowFW)) {
                blocked = true;
            }

            if (sq.has(IsoFlagType.windowW)) {
                blocked = true;
            }

            if (sq.getProperties().has(IsoFlagType.WindowW)) {
                blocked = true;
            }

            return blocked;
        }
    }

    private boolean isWater(IsoGridSquare sq) {
        return sq.has(IsoFlagType.water);
    }

    private boolean isRoom(IsoGridSquare sq) {
        return sq.getRoom() != null;
    }

    public interface IPathResult {
        void finished(int var1, int var2, int var3);
    }

    private final class MCDThread extends Thread {
        public final Object notifier;
        public boolean stop;
        public volatile boolean save;
        public volatile boolean waiting;
        public final Queue<MapCollisionData.PathTask> pathTasks;

        private MCDThread() {
            Objects.requireNonNull(MapCollisionData.this);
            super();
            this.notifier = new Object();
            this.pathTasks = new ArrayDeque<>();
        }

        @Override
        public void run() {
            while (!this.stop) {
                try {
                    this.runInner();
                } catch (Exception var2) {
                    ExceptionLogger.logException(var2);
                }
            }
        }

        private void runInner() {
            MapCollisionData.this.sync.startFrame();
            synchronized (MapCollisionData.this.renderLock) {
                for (MapCollisionData.PathTask task = MapCollisionData.this.pathTaskQueue.poll();
                    task != null;
                    task = MapCollisionData.this.pathTaskQueue.poll()
                ) {
                    task.execute();
                    task.release();
                }

                if (this.save) {
                    MapCollisionData.n_save();
                    ZombiePopulationManager.instance.save();
                    this.save = false;
                }

                MapCollisionData.n_update();
                ZombiePopulationManager.instance.updateThread();
            }

            MapCollisionData.this.sync.endFrame();

            while (this.shouldWait()) {
                synchronized (this.notifier) {
                    this.waiting = true;

                    try {
                        this.notifier.wait();
                    } catch (InterruptedException var5) {
                    }
                }
            }

            this.waiting = false;
        }

        private boolean shouldWait() {
            if (this.stop || this.save) {
                return false;
            } else if (!MapCollisionData.n_shouldWait()) {
                return false;
            } else {
                return !ZombiePopulationManager.instance.shouldWait() ? false : MapCollisionData.this.pathTaskQueue.isEmpty() && this.pathTasks.isEmpty();
            }
        }
    }

    private final class PathTask {
        public int startX;
        public int startY;
        public int endX;
        public int endY;
        public int curX;
        public int curY;
        public int status;
        public MapCollisionData.IPathResult result;
        public boolean myThread;

        private PathTask() {
            Objects.requireNonNull(MapCollisionData.this);
            super();
        }

        public void init(int sx, int sy, int ex, int ey, MapCollisionData.IPathResult result) {
            this.startX = sx;
            this.startY = sy;
            this.endX = ex;
            this.endY = ey;
            this.status = 0;
            this.result = result;
        }

        public void execute() {
            this.status = MapCollisionData.n_pathTask(this.startX, this.startY, this.endX, this.endY, MapCollisionData.curXY);
            this.curX = MapCollisionData.curXY[0];
            this.curY = MapCollisionData.curXY[1];
            if (this.myThread) {
                this.result.finished(this.status, this.curX, this.curY);
            } else {
                MapCollisionData.this.pathResultQueue.add(this);
            }
        }

        public void release() {
            MapCollisionData.this.freePathTasks.push(this);
        }
    }

    static class Sync {
        private final int fps = 10;
        private final long period = 100000000L;
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
            long sleepTime = 100000000L - timeDiff - this.overSleepTime;
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
