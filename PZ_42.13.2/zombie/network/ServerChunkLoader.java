// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.CRC32;
import zombie.GameTime;
import zombie.ZomboidFileSystem;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.WorldReuserThread;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;

public class ServerChunkLoader {
    private final long debugSlowMapLoadingDelay = 0L;
    private boolean mapLoading;
    private final ServerChunkLoader.LoaderThread threadLoad;
    private final ServerChunkLoader.SaveChunkThread threadSave;
    private final CRC32 crcSave = new CRC32();
    private final ServerChunkLoader.RecalcAllThread threadRecalc;

    public ServerChunkLoader() {
        this.threadLoad = new ServerChunkLoader.LoaderThread();
        this.threadLoad.setName("LoadChunk");
        this.threadLoad.setDaemon(true);
        this.threadLoad.start();
        this.threadRecalc = new ServerChunkLoader.RecalcAllThread();
        this.threadRecalc.setName("RecalcAll");
        this.threadRecalc.setDaemon(true);
        this.threadRecalc.setPriority(10);
        this.threadRecalc.start();
        this.threadSave = new ServerChunkLoader.SaveChunkThread();
        this.threadSave.setName("SaveChunk");
        this.threadSave.setDaemon(true);
        this.threadSave.start();
    }

    public void addJob(ServerMap.ServerCell cell) {
        this.mapLoading = DebugType.MapLoading.isEnabled();
        this.threadLoad.toThread.add(cell);
    }

    public void getLoaded(ArrayList<ServerMap.ServerCell> loaded) {
        this.threadLoad.fromThread.drainTo(loaded);
    }

    public void quit() {
        this.threadLoad.quit();

        while (this.threadLoad.isAlive()) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var3) {
            }
        }

        this.threadSave.quit();

        while (this.threadSave.isAlive()) {
            try {
                Thread.sleep(500L);
            } catch (InterruptedException var2) {
            }
        }
    }

    public void addSaveUnloadedJob(IsoChunk chunk) {
        this.threadSave.addUnloadedJob(chunk);
    }

    public void addSaveLoadedJob(IsoChunk chunk) {
        this.threadSave.addLoadedJob(chunk);
    }

    public void saveLater(GameTime gameTime) {
        this.threadSave.saveLater(gameTime);
    }

    public void updateSaved() {
        this.threadSave.update();
    }

    public void addRecalcJob(ServerMap.ServerCell cell) {
        this.threadRecalc.toThread.add(cell);
    }

    public void getRecalc(ArrayList<ServerMap.ServerCell> loaded) {
        this.threadRecalc.fromThread.drainTo(loaded);
    }

    private class GetSquare implements IsoGridSquare.GetSquare {
        ServerMap.ServerCell cell;

        private GetSquare() {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
        }

        @Override
        public IsoGridSquare getGridSquare(int x, int y, int z) {
            x -= this.cell.wx * 64;
            y -= this.cell.wy * 64;
            if (x < 0 || x >= 64) {
                return null;
            } else if (y >= 0 && y < 64) {
                IsoChunk chunk = this.cell.chunks[x / 8][y / 8];
                return chunk == null ? null : chunk.getGridSquare(x % 8, y % 8, z);
            } else {
                return null;
            }
        }

        public boolean contains(int x, int y, int z) {
            return x < 0 || x >= 64 ? false : y >= 0 && y < 64;
        }

        public IsoChunk getChunkForSquare(int x, int y) {
            x -= this.cell.wx * 64;
            y -= this.cell.wy * 64;
            if (x < 0 || x >= 64) {
                return null;
            } else {
                return y >= 0 && y < 64 ? this.cell.chunks[x / 8][y / 8] : null;
            }
        }

        public void EnsureSurroundNotNull(int x, int y, int z) {
            int minX = this.cell.wx * 64;
            int minY = this.cell.wy * 64;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx != 0 || dy != 0) && this.contains(x + dx, y + dy, z)) {
                        IsoGridSquare sq2 = this.getGridSquare(minX + x + dx, minY + y + dy, z);
                        if (sq2 == null) {
                            sq2 = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, minX + x + dx, minY + y + dy, z);
                            int chx = (x + dx) / 8;
                            int chy = (y + dy) / 8;
                            int sqx = (x + dx) % 8;
                            int sqy = (y + dy) % 8;
                            if (this.cell.chunks[chx][chy] != null) {
                                this.cell.chunks[chx][chy].setSquare(sqx, sqy, z, sq2);
                            }
                        }
                    }
                }
            }
        }
    }

    private class LoaderThread extends Thread {
        private final LinkedBlockingQueue<ServerMap.ServerCell> toThread;
        private final LinkedBlockingQueue<ServerMap.ServerCell> fromThread;
        ArrayDeque<IsoGridSquare> isoGridSquareCache;

        private LoaderThread() {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
            this.toThread = new LinkedBlockingQueue<>();
            this.fromThread = new LinkedBlockingQueue<>();
            this.isoGridSquareCache = new ArrayDeque<>();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ServerMap.ServerCell cell = this.toThread.take();
                    if (this.isoGridSquareCache.size() < 10000) {
                        IsoGridSquare.getSquaresForThread(this.isoGridSquareCache, 10000);
                        IsoGridSquare.loadGridSquareCache = this.isoGridSquareCache;
                    }

                    if (cell.wx == -1 && cell.wy == -1) {
                        return;
                    }

                    if (cell.cancelLoading) {
                        if (ServerChunkLoader.this.mapLoading) {
                            DebugLog.MapLoading.debugln("LoaderThread: cancelled " + cell.wx + "," + cell.wy);
                        }

                        cell.loadingWasCancelled = true;
                    } else {
                        long start = System.nanoTime();

                        for (int x = 0; x < 8; x++) {
                            for (int y = 0; y < 8; y++) {
                                int wx = cell.wx * 8 + x;
                                int wy = cell.wy * 8 + y;
                                if (IsoWorld.instance.metaGrid.isValidChunk(wx, wy)) {
                                    IsoChunk chunk = IsoChunkMap.chunkStore.poll();
                                    if (chunk == null) {
                                        chunk = new IsoChunk((IsoCell)null);
                                    }

                                    chunk.assignLoadID();
                                    ServerChunkLoader.this.threadSave.saveNow(wx, wy);

                                    try {
                                        if (chunk.LoadOrCreate(wx, wy, null)) {
                                            chunk.loaded = true;
                                        } else {
                                            ChunkChecksum.setChecksum(wx, wy, 0L);
                                            chunk.Blam(wx, wy);
                                            if (chunk.LoadBrandNew(wx, wy)) {
                                                chunk.loaded = true;
                                            }
                                        }
                                    } catch (Exception var10) {
                                        var10.printStackTrace();
                                        LoggerManager.getLogger("map").write(var10);
                                    }

                                    if (chunk.loaded) {
                                        cell.chunks[x][y] = chunk;
                                    }
                                }
                            }
                        }

                        if (GameServer.debug) {
                        }

                        float time = (float)(System.nanoTime() - start) / 1000000.0F;
                        this.fromThread.add(cell);
                    }
                } catch (Exception var11) {
                    var11.printStackTrace();
                    LoggerManager.getLogger("map").write(var11);
                }
            }
        }

        public void quit() {
            ServerMap.ServerCell quitCell = new ServerMap.ServerCell();
            quitCell.wx = -1;
            quitCell.wy = -1;
            this.toThread.add(quitCell);
        }
    }

    private class QuitThreadTask implements ServerChunkLoader.SaveTask {
        private QuitThreadTask() {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
        }

        @Override
        public void save() throws Exception {
            ServerChunkLoader.this.threadSave.quit = true;
        }

        @Override
        public void release() {
        }

        @Override
        public int wx() {
            return 0;
        }

        @Override
        public int wy() {
            return 0;
        }
    }

    private class RecalcAllThread extends Thread {
        private final LinkedBlockingQueue<ServerMap.ServerCell> toThread;
        private final LinkedBlockingQueue<ServerMap.ServerCell> fromThread;
        private final ServerChunkLoader.GetSquare serverCellGetSquare;

        private RecalcAllThread() {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
            this.toThread = new LinkedBlockingQueue<>();
            this.fromThread = new LinkedBlockingQueue<>();
            this.serverCellGetSquare = ServerChunkLoader.this.new GetSquare();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    this.runInner();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }
        }

        private void runInner() throws InterruptedException {
            ServerMap.ServerCell cell = this.toThread.take();
            if (cell.cancelLoading && !this.hasAnyBrandNewChunks(cell)) {
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        IsoChunk chunk = cell.chunks[x][y];
                        if (chunk != null) {
                            cell.chunks[x][y] = null;
                            WorldReuserThread.instance.addReuseChunk(chunk);
                        }
                    }
                }

                if (ServerChunkLoader.this.mapLoading) {
                    DebugLog.MapLoading.debugln("RecalcAllThread: cancelled " + cell.wx + "," + cell.wy);
                }

                cell.loadingWasCancelled = true;
            } else {
                long start = System.nanoTime();
                this.serverCellGetSquare.cell = cell;
                int sx = cell.wx * 64;
                int sy = cell.wy * 64;
                int ex = sx + 64;
                int ey = sy + 64;
                int maxZ = 0;
                int nSquares = 64;

                for (int cx = 0; cx < 8; cx++) {
                    for (int cy = 0; cy < 8; cy++) {
                        IsoChunk chunk = cell.chunks[cx][cy];
                        if (chunk != null) {
                            chunk.loaded = false;

                            for (int i = 0; i < 64; i++) {
                                for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                                    int zz = chunk.squaresIndexOfLevel(z);
                                    IsoGridSquare g = chunk.squares[zz][i];
                                    if (z == 0 && g == null) {
                                        int xx = chunk.wx * 8 + i % 8;
                                        int y = chunk.wy * 8 + i / 8;
                                        g = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, xx, y, z);
                                        chunk.setSquare(xx % 8, y % 8, z, g);
                                    }

                                    if (g != null) {
                                        g.RecalcProperties();
                                        IsoRoom room = g.getRoom();
                                        if (room != null) {
                                            room.addSquare(g);
                                        }
                                    }
                                }
                            }

                            if (chunk.maxLevel > maxZ) {
                                maxZ = chunk.maxLevel;
                            }
                        }
                    }
                }

                for (int cx = 0; cx < 8; cx++) {
                    for (int cyx = 0; cyx < 8; cyx++) {
                        IsoChunk chunk = cell.chunks[cx][cyx];
                        if (chunk != null) {
                            for (int i = 0; i < 64; i++) {
                                for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                                    int squaresIndexOfLevel = chunk.squaresIndexOfLevel(z);
                                    IsoGridSquare gx = chunk.squares[squaresIndexOfLevel][i];
                                    if (gx != null) {
                                        if (z != 0 && !gx.getObjects().isEmpty()) {
                                            this.serverCellGetSquare.EnsureSurroundNotNull(gx.x - sx, gx.y - sy, z);
                                        }

                                        gx.RecalcAllWithNeighbours(false, this.serverCellGetSquare);
                                    }
                                }
                            }
                        }
                    }
                }

                for (int cx = 0; cx < 8; cx++) {
                    for (int cyxx = 0; cyxx < 8; cyxx++) {
                        IsoChunk chunk = cell.chunks[cx][cyxx];
                        if (chunk != null) {
                            for (int i = 0; i < 64; i++) {
                                for (int zx = chunk.maxLevel; zx > chunk.minLevel; zx--) {
                                    int squaresIndexOfLevel = chunk.squaresIndexOfLevel(zx);
                                    IsoGridSquare sq = chunk.squares[squaresIndexOfLevel][i];
                                    if (sq != null && sq.hasRainBlockingTile()) {
                                        zx--;

                                        for (; zx >= chunk.minLevel; zx--) {
                                            squaresIndexOfLevel = chunk.squaresIndexOfLevel(zx);
                                            sq = chunk.squares[squaresIndexOfLevel][i];
                                            if (sq != null) {
                                                sq.haveRoof = true;
                                                sq.getProperties().unset(IsoFlagType.exterior);
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (GameServer.debug) {
                }

                float time = (float)(System.nanoTime() - start) / 1000000.0F;
                if (ServerChunkLoader.this.mapLoading) {
                    DebugLog.MapLoading.debugln("RecalcAll for cell " + cell.wx + "," + cell.wy + " ms=" + time);
                }

                this.fromThread.add(cell);
            }
        }

        private boolean hasAnyBrandNewChunks(ServerMap.ServerCell cell) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    IsoChunk chunk = cell.chunks[x][y];
                    if (chunk != null && !chunk.getErosionData().init) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private class SaveChunkThread extends Thread {
        private final LinkedBlockingQueue<ServerChunkLoader.SaveTask> toThread;
        private final LinkedBlockingQueue<ServerChunkLoader.SaveTask> fromThread;
        private boolean quit;
        private final CRC32 crc32;
        private final ClientChunkRequest ccr;
        private final ArrayList<ServerChunkLoader.SaveTask> toSaveChunk;
        private final ArrayList<ServerChunkLoader.SaveTask> savedChunks;

        private SaveChunkThread() {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
            this.toThread = new LinkedBlockingQueue<>();
            this.fromThread = new LinkedBlockingQueue<>();
            this.crc32 = new CRC32();
            this.ccr = new ClientChunkRequest();
            this.toSaveChunk = new ArrayList<>();
            this.savedChunks = new ArrayList<>();
        }

        @Override
        public void run() {
            do {
                ServerChunkLoader.SaveTask task = null;

                try {
                    task = this.toThread.take();
                    task.save();
                    this.fromThread.add(task);
                } catch (InterruptedException var3) {
                } catch (Exception var4) {
                    var4.printStackTrace();
                    if (task != null) {
                        LoggerManager.getLogger("map").write("Error saving chunk " + task.wx() + "," + task.wy());
                    }

                    LoggerManager.getLogger("map").write(var4);
                }
            } while (!this.quit || !this.toThread.isEmpty());
        }

        public void addUnloadedJob(IsoChunk chunk) {
            this.toThread.add(ServerChunkLoader.this.new SaveUnloadedTask(chunk));
        }

        public void addLoadedJob(IsoChunk chunk) {
            ClientChunkRequest.Chunk reqChunk = this.ccr.getChunk();
            reqChunk.wx = chunk.wx;
            reqChunk.wy = chunk.wy;
            this.ccr.getByteBuffer(reqChunk);

            try {
                chunk.SaveLoadedChunk(reqChunk, this.crc32);
            } catch (Exception var4) {
                var4.printStackTrace();
                LoggerManager.getLogger("map").write(var4);
                this.ccr.releaseChunk(reqChunk);
                return;
            }

            this.toThread.add(ServerChunkLoader.this.new SaveLoadedTask(this.ccr, reqChunk));
        }

        public void saveLater(GameTime gameTime) {
            this.toThread.add(ServerChunkLoader.this.new SaveGameTimeTask(gameTime));
        }

        public void saveNow(int chunkX, int chunkY) {
            this.toSaveChunk.clear();
            this.toThread.drainTo(this.toSaveChunk);

            for (int i = 0; i < this.toSaveChunk.size(); i++) {
                ServerChunkLoader.SaveTask task = this.toSaveChunk.get(i);
                if (task.wx() == chunkX && task.wy() == chunkY) {
                    try {
                        this.toSaveChunk.remove(i--);
                        task.save();
                    } catch (Exception var6) {
                        var6.printStackTrace();
                        LoggerManager.getLogger("map").write("Error saving chunk " + chunkX + "," + chunkY);
                        LoggerManager.getLogger("map").write(var6);
                    }

                    this.fromThread.add(task);
                }
            }

            this.toThread.addAll(this.toSaveChunk);
        }

        public void quit() {
            this.toThread.add(ServerChunkLoader.this.new QuitThreadTask());
        }

        public void update() {
            this.savedChunks.clear();
            this.fromThread.drainTo(this.savedChunks);

            for (int i = 0; i < this.savedChunks.size(); i++) {
                this.savedChunks.get(i).release();
            }

            this.savedChunks.clear();
        }
    }

    private class SaveGameTimeTask implements ServerChunkLoader.SaveTask {
        private byte[] bytes;

        public SaveGameTimeTask(final GameTime gameTime) {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();

            try {
                try (
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
                    DataOutputStream dos = new DataOutputStream(baos);
                ) {
                    gameTime.save(dos);
                    dos.close();
                    this.bytes = baos.toByteArray();
                }
            } catch (Exception var11) {
                var11.printStackTrace();
            }
        }

        @Override
        public void save() throws Exception {
            if (this.bytes != null) {
                File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(this.bytes);
                } catch (Exception var7) {
                    var7.printStackTrace();
                    return;
                }
            }
        }

        @Override
        public void release() {
        }

        @Override
        public int wx() {
            return 0;
        }

        @Override
        public int wy() {
            return 0;
        }
    }

    private class SaveLoadedTask implements ServerChunkLoader.SaveTask {
        private final ClientChunkRequest ccr;
        private final ClientChunkRequest.Chunk chunk;

        public SaveLoadedTask(final ClientChunkRequest ccr, final ClientChunkRequest.Chunk chunk) {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
            this.ccr = ccr;
            this.chunk = chunk;
        }

        @Override
        public void save() throws Exception {
            long crc = ChunkChecksum.getChecksumIfExists(this.chunk.wx, this.chunk.wy);
            ServerChunkLoader.this.crcSave.reset();
            ServerChunkLoader.this.crcSave.update(this.chunk.bb.array(), 0, this.chunk.bb.position());
            if (crc != ServerChunkLoader.this.crcSave.getValue()) {
                ChunkChecksum.setChecksum(this.chunk.wx, this.chunk.wy, ServerChunkLoader.this.crcSave.getValue());
                IsoChunk.SafeWrite(this.chunk.wx, this.chunk.wy, this.chunk.bb);
            }
        }

        @Override
        public void release() {
            this.ccr.releaseChunk(this.chunk);
        }

        @Override
        public int wx() {
            return this.chunk.wx;
        }

        @Override
        public int wy() {
            return this.chunk.wy;
        }
    }

    private interface SaveTask {
        void save() throws Exception;

        void release();

        int wx();

        int wy();
    }

    private class SaveUnloadedTask implements ServerChunkLoader.SaveTask {
        private final IsoChunk chunk;

        public SaveUnloadedTask(final IsoChunk chunk) {
            Objects.requireNonNull(ServerChunkLoader.this);
            super();
            this.chunk = chunk;
        }

        @Override
        public void save() throws Exception {
            this.chunk.Save(false);
        }

        @Override
        public void release() {
            WorldReuserThread.instance.addReuseChunk(this.chunk);
        }

        @Override
        public int wx() {
            return this.chunk.wx;
        }

        @Override
        public int wy() {
            return this.chunk.wy;
        }
    }
}
