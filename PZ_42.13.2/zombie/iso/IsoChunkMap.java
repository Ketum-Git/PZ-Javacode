// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleManager;

@UsedFromLua
public final class IsoChunkMap {
    public static final int LEVELS = 64;
    public static final int GROUND_LEVEL = 32;
    public static final int TOP_LEVEL = 31;
    public static final int BOTTOM_LEVEL = -32;
    public static final int OLD_CHUNKS_PER_WIDTH = 10;
    public static final int CHUNKS_PER_WIDTH = 8;
    public static final int CHUNK_SIZE_IN_SQUARES = 8;
    public static final HashMap<Integer, IsoChunk> SharedChunks = new HashMap<>();
    public static int mpWorldXa;
    public static int mpWorldYa;
    public static int mpWorldZa;
    public static int worldXa = 11702;
    public static int worldYa = 6896;
    public static int worldZa;
    public static final int[] SWorldX = new int[4];
    public static final int[] SWorldY = new int[4];
    public static final ConcurrentLinkedQueue<IsoChunk> chunkStore = new ConcurrentLinkedQueue<>();
    public static final ReentrantLock bSettingChunk = new ReentrantLock(true);
    private static final int START_CHUNK_GRID_WIDTH = 13;
    public static int chunkGridWidth = 13;
    public static int chunkWidthInTiles = 8 * chunkGridWidth;
    private static final ColorInfo inf = new ColorInfo();
    private static final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType = new ArrayList<>();
    public int playerId;
    public boolean ignore;
    public int worldX = chunkMapSquareToChunkMapChunkXY(worldXa);
    public int worldY = chunkMapSquareToChunkMapChunkXY(worldYa);
    public final ArrayList<String> filenameServerRequests = new ArrayList<>();
    protected IsoChunk[] chunksSwapB;
    protected IsoChunk[] chunksSwapA;
    boolean readBufferA = true;
    int xMinTiles = -1;
    int yMinTiles = -1;
    int xMaxTiles = -1;
    int yMaxTiles = -1;
    private final IsoCell cell;
    private final UpdateLimit checkVehiclesFrequency = new UpdateLimit(3000L);
    private final UpdateLimit hotSaveFrequency = new UpdateLimit(1000L);
    public int maxHeight;
    public int minHeight;
    public static final PerformanceProfileProbe ppp_update;

    public IsoChunkMap(IsoCell cell) {
        this.cell = cell;
        WorldReuserThread.instance.finished = false;
        this.chunksSwapB = new IsoChunk[chunkGridWidth * chunkGridWidth];
        this.chunksSwapA = new IsoChunk[chunkGridWidth * chunkGridWidth];
    }

    public static void CalcChunkWidth() {
        if (DebugOptions.instance.worldChunkMap13x13.getValue()) {
            chunkGridWidth = 13;
            chunkWidthInTiles = chunkGridWidth * 8;
        } else if (DebugOptions.instance.worldChunkMap11x11.getValue()) {
            chunkGridWidth = 11;
            chunkWidthInTiles = chunkGridWidth * 8;
        } else if (DebugOptions.instance.worldChunkMap9x9.getValue()) {
            chunkGridWidth = 9;
            chunkWidthInTiles = chunkGridWidth * 8;
        } else if (DebugOptions.instance.worldChunkMap7x7.getValue()) {
            chunkGridWidth = 7;
            chunkWidthInTiles = chunkGridWidth * 8;
        } else if (DebugOptions.instance.worldChunkMap5x5.getValue()) {
            chunkGridWidth = 5;
            chunkWidthInTiles = chunkGridWidth * 8;
        } else {
            float delx = Core.getInstance().getScreenWidth() / 1920.0F;
            float dely = Core.getInstance().getScreenHeight() / 1080.0F;
            float del = Math.max(delx, dely);
            if (del > 1.0F) {
                del = 1.0F;
            }

            chunkGridWidth = (int)(13.0F * del * 1.5);
            if (chunkGridWidth / 2 * 2 == chunkGridWidth) {
                chunkGridWidth++;
            }

            chunkGridWidth = PZMath.min(chunkGridWidth, 19);
            chunkWidthInTiles = chunkGridWidth * 8;
        }
    }

    public static void setWorldStartPos(int x, int y) {
        SWorldX[IsoPlayer.getPlayerIndex()] = chunkMapSquareToChunkMapChunkXY(x);
        SWorldY[IsoPlayer.getPlayerIndex()] = chunkMapSquareToChunkMapChunkXY(y);
    }

    public void Dispose() {
        IsoChunk.loadGridSquare.clear();
        this.chunksSwapA = null;
        this.chunksSwapB = null;
    }

    public void setInitialPos(int wx, int wy) {
        this.worldX = wx;
        this.worldY = wy;
        this.xMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMinTiles = -1;
        this.yMaxTiles = -1;
    }

    public void processAllLoadGridSquare() {
        for (IsoChunk chunk = IsoChunk.loadGridSquare.poll(); chunk != null; chunk = IsoChunk.loadGridSquare.poll()) {
            bSettingChunk.lock();

            try {
                boolean loaded = false;

                for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                    IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[n];
                    if (!cm.ignore && cm.setChunkDirect(chunk, false)) {
                        loaded = true;
                    }
                }

                if (!loaded) {
                    WorldReuserThread.instance.addReuseChunk(chunk);
                } else {
                    chunk.doLoadGridsquare();
                }
            } finally {
                bSettingChunk.unlock();
            }
        }
    }

    public void update() {
        try (AbstractPerformanceProfileProbe ignored = ppp_update.profile()) {
            this.updateInternal();
        }
    }

    private void updateInternal() {
        boolean bChanged = false;
        int count = IsoChunk.loadGridSquare.size();
        if (count != 0) {
            count = 1 + count * 3 / chunkGridWidth;
        }

        while (count > 0) {
            IsoChunk chunk = IsoChunk.loadGridSquare.poll();
            if (chunk != null) {
                boolean loaded = false;

                for (int n = 0; n < IsoPlayer.numPlayers; n++) {
                    IsoChunkMap cm = IsoWorld.instance.currentCell.chunkMap[n];
                    if (!cm.ignore && cm.setChunkDirect(chunk, false)) {
                        loaded = true;
                    }
                }

                if (!loaded) {
                    WorldReuserThread.instance.addReuseChunk(chunk);
                    count--;
                    continue;
                }

                chunk.loaded = true;
                bSettingChunk.lock();

                try {
                    try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("IsoChunk.doLoadGridsquare")) {
                        chunk.doLoadGridsquare();
                        bChanged = true;
                    }

                    if (GameClient.client) {
                        List<VehicleCache> vehicles = VehicleCache.vehicleGet(chunk.wx, chunk.wy);
                        if (vehicles != null) {
                            for (VehicleCache vehicle : vehicles) {
                                VehicleManager.instance.sendVehicleRequest(vehicle.id, (short)1);
                            }
                        }
                    }
                } finally {
                    bSettingChunk.unlock();
                }

                for (int var19 = 0; var19 < IsoPlayer.numPlayers; var19++) {
                    IsoPlayer player = IsoPlayer.players[var19];
                    if (player != null) {
                        player.dirtyRecalcGridStackTime = 20.0F;
                    }
                }
            }

            count--;
        }

        if (bChanged) {
            this.calculateZExtentsForChunkMap();
        }

        if (this.hotSaveFrequency.Check()) {
            for (int y = 0; y < chunkGridWidth; y++) {
                for (int x = 0; x < chunkGridWidth; x++) {
                    IsoChunk chunk = this.getChunk(x, y);
                    if (chunk != null) {
                        chunk.update();
                        if (!GameClient.client && !GameServer.server && chunk.requiresHotSave && ChunkSaveWorker.instance.toSaveQueue.size() < 10) {
                            ChunkSaveWorker.instance.AddHotSave(chunk);
                            chunk.requiresHotSave = false;
                        }
                    }
                }
            }
        }

        if (this.checkVehiclesFrequency.Check() && GameClient.client) {
            this.checkVehicles();
        }
    }

    private void checkVehicles() {
        for (int y = 0; y < chunkGridWidth; y++) {
            for (int x = 0; x < chunkGridWidth; x++) {
                IsoChunk chunk = this.getChunk(x, y);
                if (chunk != null && chunk.loaded) {
                    List<VehicleCache> vehicles = VehicleCache.vehicleGet(chunk.wx, chunk.wy);
                    if (vehicles != null && chunk.vehicles.size() != vehicles.size()) {
                        for (int i = 0; i < vehicles.size(); i++) {
                            short id = vehicles.get(i).id;
                            boolean hasID = false;

                            for (int k = 0; k < chunk.vehicles.size(); k++) {
                                if (chunk.vehicles.get(k).getId() == id) {
                                    hasID = true;
                                    break;
                                }
                            }

                            if (!hasID && VehicleManager.instance.getVehicleByID(id) == null) {
                                VehicleManager.instance.sendVehicleRequest(id, (short)1);
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkIntegrity() {
        IsoWorld.instance.currentCell.chunkMap[0].xMinTiles = -1;

        for (int x = IsoWorld.instance.currentCell.chunkMap[0].getWorldXMinTiles(); x < IsoWorld.instance.currentCell.chunkMap[0].getWorldXMaxTiles(); x++) {
            for (int y = IsoWorld.instance.currentCell.chunkMap[0].getWorldYMinTiles(); y < IsoWorld.instance.currentCell.chunkMap[0].getWorldYMaxTiles(); y++) {
                IsoGridSquare grid = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
                if (grid != null && (grid.getX() != x || grid.getY() != y)) {
                    int cx = x / 8;
                    int cy = y / 8;
                    cx -= IsoWorld.instance.currentCell.chunkMap[0].getWorldXMin();
                    cy -= IsoWorld.instance.currentCell.chunkMap[0].getWorldYMin();
                    IsoChunk ch = null;
                    ch = new IsoChunk(IsoWorld.instance.currentCell);
                    ch.refs.add(IsoWorld.instance.currentCell.chunkMap[0]);
                    WorldStreamer.instance.addJob(ch, x / 8, y / 8, false);

                    while (!ch.loaded) {
                        try {
                            Thread.sleep(13L);
                        } catch (InterruptedException var8) {
                            var8.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void checkIntegrityThread() {
        IsoWorld.instance.currentCell.chunkMap[0].xMinTiles = -1;

        for (int x = IsoWorld.instance.currentCell.chunkMap[0].getWorldXMinTiles(); x < IsoWorld.instance.currentCell.chunkMap[0].getWorldXMaxTiles(); x++) {
            for (int y = IsoWorld.instance.currentCell.chunkMap[0].getWorldYMinTiles(); y < IsoWorld.instance.currentCell.chunkMap[0].getWorldYMaxTiles(); y++) {
                IsoGridSquare grid = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
                if (grid != null && (grid.getX() != x || grid.getY() != y)) {
                    int cx = x / 8;
                    int cy = y / 8;
                    cx -= IsoWorld.instance.currentCell.chunkMap[0].getWorldXMin();
                    cy -= IsoWorld.instance.currentCell.chunkMap[0].getWorldYMin();
                    IsoChunk ch = new IsoChunk(IsoWorld.instance.currentCell);
                    ch.refs.add(IsoWorld.instance.currentCell.chunkMap[0]);
                    WorldStreamer.instance.addJobInstant(ch, x, y, x / 8, y / 8);
                }

                if (grid != null) {
                }
            }
        }
    }

    public void LoadChunk(int wx, int wy, int x, int y) {
        IsoChunk chunk = null;
        if (SharedChunks.containsKey((wx << 16) + wy)) {
            chunk = SharedChunks.get((wx << 16) + wy);
            chunk.setCache();
            this.setChunk(x, y, chunk);
            chunk.refs.add(this);
        } else {
            chunk = chunkStore.poll();
            if (chunk == null) {
                chunk = new IsoChunk(this.cell);
            }

            chunk.assignLoadID();
            SharedChunks.put((wx << 16) + wy, chunk);
            chunk.refs.add(this);
            WorldStreamer.instance.addJob(chunk, wx, wy, false);
        }
    }

    public IsoChunk LoadChunkForLater(int wx, int wy, int x, int y) {
        if (!IsoWorld.instance.getMetaGrid().isValidChunk(wx, wy)) {
            return null;
        } else {
            IsoChunk chunk;
            if (SharedChunks.containsKey((wx << 16) + wy)) {
                chunk = SharedChunks.get((wx << 16) + wy);
                if (!chunk.refs.contains(this)) {
                    chunk.refs.add(this);
                    chunk.checkLightingLater_OnePlayer_AllLevels(this.playerId);
                }

                if (!chunk.loaded) {
                    return chunk;
                }

                this.setChunk(x, y, chunk);
            } else {
                chunk = chunkStore.poll();
                if (chunk == null) {
                    chunk = new IsoChunk(this.cell);
                }

                chunk.assignLoadID();
                SharedChunks.put((wx << 16) + wy, chunk);
                chunk.refs.add(this);
                WorldStreamer.instance.addJob(chunk, wx, wy, true);
            }

            return chunk;
        }
    }

    public IsoChunk getChunkForGridSquare(int worldSquareX, int worldSquareY) {
        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        if (!this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX) && !this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY)) {
            int chunkMapChunkX = chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
            int chunkMapChunkY = chunkMapSquareToChunkMapChunkXY(chunkMapSquareY);
            return this.getChunk(chunkMapChunkX, chunkMapChunkY);
        } else {
            return null;
        }
    }

    public IsoChunk getChunkCurrent(int x, int y) {
        if (x < 0 || x >= chunkGridWidth || y < 0 || y >= chunkGridWidth) {
            return null;
        } else {
            return !this.readBufferA ? this.chunksSwapA[chunkGridWidth * y + x] : this.chunksSwapB[chunkGridWidth * y + x];
        }
    }

    public void setGridSquare(IsoGridSquare square, int worldSquareX, int worldSquareY, int worldSquareZ) {
        assert square == null || square.x == worldSquareX && square.y == worldSquareY && square.z == worldSquareZ;

        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        if (GameServer.server
            || !this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX)
                && !this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY)
                && !this.isWorldSquareOutOfRangeZ(worldSquareZ)) {
            IsoChunk c;
            if (GameServer.server) {
                int chunkMapChunkX = chunkMapSquareToChunkMapChunkXY(worldSquareX);
                int chunkMapChunkY = chunkMapSquareToChunkMapChunkXY(worldSquareY);
                c = ServerMap.instance.getChunk(chunkMapChunkX, chunkMapChunkY);
            } else {
                int chunkMapChunkX = chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
                int chunkMapChunkY = chunkMapSquareToChunkMapChunkXY(chunkMapSquareY);
                c = this.getChunk(chunkMapChunkX, chunkMapChunkY);
            }

            if (c != null) {
                c.setSquare(this.chunkMapSquareToChunkSquareXY(chunkMapSquareX), this.chunkMapSquareToChunkSquareXY(chunkMapSquareY), worldSquareZ, square);
            }
        }
    }

    public IsoGridSquare getGridSquare(int worldSquareX, int worldSquareY, int worldSquareZ) {
        int chunkMapSquareX = this.worldSquareToChunkMapSquareX(worldSquareX);
        int chunkMapSquareY = this.worldSquareToChunkMapSquareY(worldSquareY);
        return this.getGridSquareDirect(chunkMapSquareX, chunkMapSquareY, worldSquareZ);
    }

    public IsoGridSquare getGridSquareDirect(int chunkMapSquareX, int chunkMapSquareY, int worldSquareZ) {
        if (!this.isChunkMapSquareOutOfRangeXY(chunkMapSquareX)
            && !this.isChunkMapSquareOutOfRangeXY(chunkMapSquareY)
            && !this.isWorldSquareOutOfRangeZ(worldSquareZ)) {
            int chunkMapChunkX = chunkMapSquareToChunkMapChunkXY(chunkMapSquareX);
            int chunkMapChunkY = chunkMapSquareToChunkMapChunkXY(chunkMapSquareY);
            IsoChunk c = this.getChunk(chunkMapChunkX, chunkMapChunkY);
            if (c == null) {
                return null;
            } else if (!c.loaded) {
                return null;
            } else {
                int chunkSquareX = this.chunkMapSquareToChunkSquareXY(chunkMapSquareX);
                int chunkSquareY = this.chunkMapSquareToChunkSquareXY(chunkMapSquareY);
                return c.getGridSquare(chunkSquareX, chunkSquareY, worldSquareZ);
            }
        } else {
            return null;
        }
    }

    private int chunkMapSquareToChunkSquareXY(int chunkMapSquareXY) {
        return chunkMapSquareXY % 8;
    }

    private static int chunkMapSquareToChunkMapChunkXY(int chunkMapSquareXY) {
        return chunkMapSquareXY / 8;
    }

    private boolean isChunkMapSquareOutOfRangeXY(int chunkMapSquareXY) {
        return chunkMapSquareXY < 0 || chunkMapSquareXY >= this.getWidthInTiles();
    }

    private boolean isWorldSquareOutOfRangeZ(int tileZ) {
        return tileZ < -32 || tileZ > 31;
    }

    private int worldSquareToChunkMapSquareX(int worldSquareX) {
        return worldSquareX - (this.worldX - chunkGridWidth / 2) * 8;
    }

    private int worldSquareToChunkMapSquareY(int worldSquareY) {
        return worldSquareY - (this.worldY - chunkGridWidth / 2) * 8;
    }

    public IsoChunk getChunk(int chunkMapChunkX, int chunkMapChunkY) {
        if (chunkMapChunkX < 0 || chunkMapChunkX >= chunkGridWidth || chunkMapChunkY < 0 || chunkMapChunkY >= chunkGridWidth) {
            return null;
        } else {
            return this.readBufferA
                ? this.chunksSwapA[chunkGridWidth * chunkMapChunkY + chunkMapChunkX]
                : this.chunksSwapB[chunkGridWidth * chunkMapChunkY + chunkMapChunkX];
        }
    }

    public IsoChunk[] getChunks() {
        return this.readBufferA ? this.chunksSwapA : this.chunksSwapB;
    }

    private void setChunk(int x, int y, IsoChunk c) {
        if (!this.readBufferA) {
            this.chunksSwapA[chunkGridWidth * y + x] = c;
        } else {
            this.chunksSwapB[chunkGridWidth * y + x] = c;
        }
    }

    public boolean setChunkDirect(IsoChunk c, boolean bRequireLock) {
        long start = System.nanoTime();
        if (bRequireLock) {
            bSettingChunk.lock();
        }

        long start2 = System.nanoTime();
        int x = c.wx - this.worldX;
        int y = c.wy - this.worldY;
        x += chunkGridWidth / 2;
        y += chunkGridWidth / 2;
        if (c.jobType == IsoChunk.JobType.Convert) {
            x = 0;
            y = 0;
        }

        if (!c.refs.isEmpty() && x >= 0 && y >= 0 && x < chunkGridWidth && y < chunkGridWidth) {
            try {
                if (this.readBufferA) {
                    this.chunksSwapA[chunkGridWidth * y + x] = c;
                } else {
                    this.chunksSwapB[chunkGridWidth * y + x] = c;
                }

                c.loaded = true;
                if (c.jobType == IsoChunk.JobType.None) {
                    c.setCache();
                    c.updateBuildings();
                }

                double duration1 = (System.nanoTime() - start2) / 1000000.0;
                double duration2 = (System.nanoTime() - start) / 1000000.0;
                if (LightingThread.debugLockTime && duration2 > 10.0) {
                    DebugLog.log("setChunkDirect time " + duration1 + "/" + duration2 + " ms");
                }
            } finally {
                if (bRequireLock) {
                    bSettingChunk.unlock();
                }
            }

            return true;
        } else {
            if (c.refs.contains(this)) {
                c.refs.remove(this);
                if (c.refs.isEmpty()) {
                    SharedChunks.remove((c.wx << 16) + c.wy);
                }
            }

            if (bRequireLock) {
                bSettingChunk.unlock();
            }

            return false;
        }
    }

    public void drawDebugChunkMap() {
        int x = 64;
        int y = 0;

        for (int n = 0; n < chunkGridWidth; n++) {
            int var7 = 0;

            for (int m = 0; m < chunkGridWidth; m++) {
                var7 += 64;
                IsoChunk ch = this.getChunk(n, m);
                if (ch != null) {
                    IsoGridSquare gr = ch.getGridSquare(0, 0, 0);
                    if (gr == null) {
                        TextManager.instance.DrawString(x, var7, "wx:" + ch.wx + " wy:" + ch.wy);
                    }
                }
            }

            x += 128;
        }
    }

    private void LoadLeft() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Left();
        WorldSimulation.instance.scrollGroundLeft(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;

        for (int y = -(chunkGridWidth / 2); y <= chunkGridWidth / 2; y++) {
            this.LoadChunkForLater(this.worldX - chunkGridWidth / 2, this.worldY + y, 0, y + chunkGridWidth / 2);
        }

        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollLeft(this.playerId);
    }

    public void SwapChunkBuffers() {
        for (int n = 0; n < chunkGridWidth * chunkGridWidth; n++) {
            if (this.readBufferA) {
                this.chunksSwapA[n] = null;
            } else {
                this.chunksSwapB[n] = null;
            }
        }

        this.xMinTiles = this.xMaxTiles = -1;
        this.yMinTiles = this.yMaxTiles = -1;
        this.readBufferA = !this.readBufferA;
    }

    private void setChunk(int n, IsoChunk c) {
        if (!this.readBufferA) {
            this.chunksSwapA[n] = c;
        } else {
            this.chunksSwapB[n] = c;
        }
    }

    private IsoChunk getChunk(int n) {
        return this.readBufferA ? this.chunksSwapA[n] : this.chunksSwapB[n];
    }

    private void LoadRight() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Right();
        WorldSimulation.instance.scrollGroundRight(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;

        for (int y = -(chunkGridWidth / 2); y <= chunkGridWidth / 2; y++) {
            this.LoadChunkForLater(this.worldX + chunkGridWidth / 2, this.worldY + y, chunkGridWidth - 1, y + chunkGridWidth / 2);
        }

        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollRight(this.playerId);
    }

    private void LoadUp() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Up();
        WorldSimulation.instance.scrollGroundUp(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;

        for (int x = -(chunkGridWidth / 2); x <= chunkGridWidth / 2; x++) {
            this.LoadChunkForLater(this.worldX + x, this.worldY - chunkGridWidth / 2, x + chunkGridWidth / 2, 0);
        }

        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollUp(this.playerId);
    }

    private void LoadDown() {
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.Down();
        WorldSimulation.instance.scrollGroundDown(this.playerId);
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;

        for (int x = -(chunkGridWidth / 2); x <= chunkGridWidth / 2; x++) {
            this.LoadChunkForLater(this.worldX + x, this.worldY + chunkGridWidth / 2, x + chunkGridWidth / 2, chunkGridWidth - 1);
        }

        this.SwapChunkBuffers();
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;
        this.UpdateCellCache();
        LightingThread.instance.scrollDown(this.playerId);
    }

    private void UpdateCellCache() {
    }

    private void Up() {
        for (int x = 0; x < chunkGridWidth; x++) {
            for (int y = chunkGridWidth - 1; y > 0; y--) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && y == chunkGridWidth - 1) {
                    int wx = this.worldX - chunkGridWidth / 2 + x;
                    int wy = this.worldY - chunkGridWidth / 2 + y;
                    ch = SharedChunks.get((wx << 16) + wy);
                    if (ch != null) {
                        if (ch.refs.contains(this)) {
                            ch.refs.remove(this);
                            if (ch.refs.isEmpty()) {
                                SharedChunks.remove((ch.wx << 16) + ch.wy);
                            }
                        }

                        ch = null;
                    }
                }

                if (ch != null && y == chunkGridWidth - 1) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }

                this.setChunk(x, y, this.getChunk(x, y - 1));
            }

            this.setChunk(x, 0, null);
        }

        this.worldY--;
    }

    private void Down() {
        for (int x = 0; x < chunkGridWidth; x++) {
            for (int y = 0; y < chunkGridWidth - 1; y++) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && y == 0) {
                    int wx = this.worldX - chunkGridWidth / 2 + x;
                    int wy = this.worldY - chunkGridWidth / 2 + y;
                    ch = SharedChunks.get((wx << 16) + wy);
                    if (ch != null) {
                        if (ch.refs.contains(this)) {
                            ch.refs.remove(this);
                            if (ch.refs.isEmpty()) {
                                SharedChunks.remove((ch.wx << 16) + ch.wy);
                            }
                        }

                        ch = null;
                    }
                }

                if (ch != null && y == 0) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }

                this.setChunk(x, y, this.getChunk(x, y + 1));
            }

            this.setChunk(x, chunkGridWidth - 1, null);
        }

        this.worldY++;
    }

    private void Left() {
        for (int y = 0; y < chunkGridWidth; y++) {
            for (int x = chunkGridWidth - 1; x > 0; x--) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && x == chunkGridWidth - 1) {
                    int wx = this.worldX - chunkGridWidth / 2 + x;
                    int wy = this.worldY - chunkGridWidth / 2 + y;
                    ch = SharedChunks.get((wx << 16) + wy);
                    if (ch != null) {
                        if (ch.refs.contains(this)) {
                            ch.refs.remove(this);
                            if (ch.refs.isEmpty()) {
                                SharedChunks.remove((ch.wx << 16) + ch.wy);
                            }
                        }

                        ch = null;
                    }
                }

                if (ch != null && x == chunkGridWidth - 1) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }

                this.setChunk(x, y, this.getChunk(x - 1, y));
            }

            this.setChunk(0, y, null);
        }

        this.worldX--;
    }

    private void Right() {
        for (int y = 0; y < chunkGridWidth; y++) {
            for (int x = 0; x < chunkGridWidth - 1; x++) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch == null && x == 0) {
                    int wx = this.worldX - chunkGridWidth / 2 + x;
                    int wy = this.worldY - chunkGridWidth / 2 + y;
                    ch = SharedChunks.get((wx << 16) + wy);
                    if (ch != null) {
                        if (ch.refs.contains(this)) {
                            ch.refs.remove(this);
                            if (ch.refs.isEmpty()) {
                                SharedChunks.remove((ch.wx << 16) + ch.wy);
                            }
                        }

                        ch = null;
                    }
                }

                if (ch != null && x == 0) {
                    ch.refs.remove(this);
                    if (ch.refs.isEmpty()) {
                        SharedChunks.remove((ch.wx << 16) + ch.wy);
                        ch.removeFromWorld();
                        ChunkSaveWorker.instance.Add(ch);
                    }
                }

                this.setChunk(x, y, this.getChunk(x + 1, y));
            }

            this.setChunk(chunkGridWidth - 1, y, null);
        }

        this.worldX++;
    }

    public int getWorldXMin() {
        return this.worldX - chunkGridWidth / 2;
    }

    public int getWorldYMin() {
        return this.worldY - chunkGridWidth / 2;
    }

    public void ProcessChunkPos(IsoGameCharacter chr) {
        float x1 = chr.getX();
        float y1 = chr.getY();
        int z = PZMath.fastfloor(chr.getZ());
        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getVehicle() != null) {
            IsoPlayer p = IsoPlayer.getInstance();
            BaseVehicle v = p.getVehicle();
            float s = v.getCurrentSpeedKmHour() / 5.0F;
            if (!p.isDriving()) {
                s = Math.min(s * 2.0F, 20.0F);
            }

            x1 += Math.round(p.getForwardDirectionX() * s);
            y1 += Math.round(p.getForwardDirectionY() * s);
        }

        int x = PZMath.fastfloor(x1 / 8.0F);
        int y = PZMath.fastfloor(y1 / 8.0F);
        if (x != this.worldX || y != this.worldY) {
            long start = System.nanoTime();
            double duration1 = 0.0;
            bSettingChunk.lock();
            long start2 = System.nanoTime();
            boolean changed = false;

            try {
                if (Math.abs(x - this.worldX) < chunkGridWidth && Math.abs(y - this.worldY) < chunkGridWidth) {
                    if (x != this.worldX) {
                        if (x < this.worldX) {
                            this.LoadLeft();
                        } else {
                            this.LoadRight();
                        }

                        changed = true;
                    } else if (y != this.worldY) {
                        if (y < this.worldY) {
                            this.LoadUp();
                        } else {
                            this.LoadDown();
                        }

                        changed = true;
                    }
                } else {
                    if (LightingJNI.init) {
                        LightingJNI.teleport(this.playerId, x - chunkGridWidth / 2, y - chunkGridWidth / 2);
                    }

                    this.Unload();
                    IsoPlayer player = IsoPlayer.players[this.playerId];
                    player.removeFromSquare();
                    player.square = null;
                    this.worldX = x;
                    this.worldY = y;
                    if (!GameServer.server) {
                        WorldSimulation.instance.activateChunkMap(this.playerId);
                    }

                    int minwx = this.worldX - chunkGridWidth / 2;
                    int minwy = this.worldY - chunkGridWidth / 2;
                    int maxwx = this.worldX + chunkGridWidth / 2;
                    int maxwy = this.worldY + chunkGridWidth / 2;

                    for (int xx = minwx; xx <= maxwx; xx++) {
                        for (int yy = minwy; yy <= maxwy; yy++) {
                            this.LoadChunkForLater(xx, yy, xx - minwx, yy - minwy);
                        }
                    }

                    this.SwapChunkBuffers();
                    this.UpdateCellCache();
                    IsoCell cell = IsoWorld.instance.getCell();
                    if (!cell.getObjectList().contains(player) && !cell.getAddList().contains(player)) {
                        cell.getAddList().add(player);
                    }

                    changed = true;
                }
            } finally {
                bSettingChunk.unlock();
                if (changed) {
                    this.calculateZExtentsForChunkMap();
                }
            }

            duration1 = (System.nanoTime() - start2) / 1000000.0;
            double duration2 = (System.nanoTime() - start) / 1000000.0;
            if (LightingThread.debugLockTime && duration2 > 10.0) {
                DebugLog.log("ProcessChunkPos time " + duration1 + "/" + duration2 + " ms");
            }
        }
    }

    public void calculateZExtentsForChunkMap() {
        int max = 0;
        int min = 0;

        for (int xx = 0; xx < this.chunksSwapA.length; xx++) {
            for (int yy = 0; yy < this.chunksSwapA.length; yy++) {
                IsoChunk c = this.getChunk(xx, yy);
                if (c != null) {
                    max = Math.max(c.maxLevel, max);
                    min = Math.min(min, c.minLevel);
                }
            }
        }

        this.maxHeight = max;
        this.minHeight = min;
    }

    public IsoRoom getRoom(int iD) {
        return null;
    }

    public int getWidthInTiles() {
        return chunkWidthInTiles;
    }

    public int getWorldXMinTiles() {
        if (this.xMinTiles != -1) {
            return this.xMinTiles;
        } else {
            this.xMinTiles = this.getWorldXMin() * 8;
            return this.xMinTiles;
        }
    }

    public int getWorldYMinTiles() {
        if (this.yMinTiles != -1) {
            return this.yMinTiles;
        } else {
            this.yMinTiles = this.getWorldYMin() * 8;
            return this.yMinTiles;
        }
    }

    public int getWorldXMaxTiles() {
        if (this.xMaxTiles != -1) {
            return this.xMaxTiles;
        } else {
            this.xMaxTiles = this.getWorldXMin() * 8 + this.getWidthInTiles();
            return this.xMaxTiles;
        }
    }

    public int getWorldYMaxTiles() {
        if (this.yMaxTiles != -1) {
            return this.yMaxTiles;
        } else {
            this.yMaxTiles = this.getWorldYMin() * 8 + this.getWidthInTiles();
            return this.yMaxTiles;
        }
    }

    public void Save() {
        if (!GameServer.server) {
            for (int x = 0; x < chunkGridWidth; x++) {
                for (int y = 0; y < chunkGridWidth; y++) {
                    IsoChunk c = this.getChunk(x, y);
                    if (c != null) {
                        try {
                            c.Save(true);
                        } catch (IOException var5) {
                            ExceptionLogger.logException(var5);
                        }
                    }
                }
            }
        }
    }

    public void renderBloodForChunks(int zza) {
        if (DebugOptions.instance.terrain.renderTiles.bloodDecals.getValue()) {
            if (!(zza > IsoCamera.getCameraCharacterZ())) {
                int OptionBloodDecals = Core.getInstance().getOptionBloodDecals();
                if (OptionBloodDecals != 0) {
                    float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
                    int playerIndex = IsoCamera.frameState.playerIndex;

                    for (int n = 0; n < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; n++) {
                        splatByType.get(n).clear();
                    }

                    for (int x = 0; x < chunkGridWidth; x++) {
                        for (int y = 0; y < chunkGridWidth; y++) {
                            IsoChunk ch = this.getChunk(x, y);
                            if (ch != null) {
                                for (int n = 0; n < ch.floorBloodSplatsFade.size(); n++) {
                                    IsoFloorBloodSplat b = ch.floorBloodSplatsFade.get(n);
                                    if ((b.index < 1 || b.index > 10 || IsoChunk.renderByIndex[OptionBloodDecals - 1][b.index - 1] != 0)
                                        && PZMath.fastfloor(b.z) == zza
                                        && b.type >= 0
                                        && b.type < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) {
                                        b.chunk = ch;
                                        splatByType.get(b.type).add(b);
                                    }
                                }

                                if (!ch.floorBloodSplats.isEmpty()) {
                                    for (int nx = 0; nx < ch.floorBloodSplats.size(); nx++) {
                                        IsoFloorBloodSplat b = ch.floorBloodSplats.get(nx);
                                        if ((b.index < 1 || b.index > 10 || IsoChunk.renderByIndex[OptionBloodDecals - 1][b.index - 1] != 0)
                                            && PZMath.fastfloor(b.z) == zza
                                            && b.type >= 0
                                            && b.type < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) {
                                            b.chunk = ch;
                                            splatByType.get(b.type).add(b);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (int nxx = 0; nxx < splatByType.size(); nxx++) {
                        ArrayList<IsoFloorBloodSplat> splats = splatByType.get(nxx);
                        if (!splats.isEmpty()) {
                            String type = IsoFloorBloodSplat.FLOOR_BLOOD_TYPES[nxx];
                            IsoSprite use = null;
                            if (!IsoFloorBloodSplat.spriteMap.containsKey(type)) {
                                IsoSprite sp = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                                sp.LoadFramesPageSimple(type, type, type, type);
                                IsoFloorBloodSplat.spriteMap.put(type, sp);
                                use = sp;
                            } else {
                                use = IsoFloorBloodSplat.spriteMap.get(type);
                            }

                            for (int i = 0; i < splats.size(); i++) {
                                IsoFloorBloodSplat b = splats.get(i);
                                inf.r = 1.0F;
                                inf.g = 1.0F;
                                inf.b = 1.0F;
                                inf.a = 0.27F;
                                float aa = (b.x + b.y / b.x) * (b.type + 1);
                                float bb = aa * b.x / b.y * (b.type + 1) / (aa + b.y);
                                float cc = bb * aa * bb * b.x / (b.y + 2.0F);
                                aa *= 42367.543F;
                                bb *= 6367.123F;
                                cc *= 23367.133F;
                                aa %= 1000.0F;
                                bb %= 1000.0F;
                                cc %= 1000.0F;
                                aa /= 1000.0F;
                                bb /= 1000.0F;
                                cc /= 1000.0F;
                                if (aa > 0.25F) {
                                    aa = 0.25F;
                                }

                                inf.r -= aa * 2.0F;
                                inf.g -= aa * 2.0F;
                                inf.b -= aa * 2.0F;
                                inf.r += bb / 3.0F;
                                inf.g -= cc / 3.0F;
                                inf.b -= cc / 3.0F;
                                float deltaAge = worldAge - b.worldAge;
                                if (deltaAge >= 0.0F && deltaAge < 72.0F) {
                                    float f = 1.0F - deltaAge / 72.0F;
                                    inf.r *= 0.2F + f * 0.8F;
                                    inf.g *= 0.2F + f * 0.8F;
                                    inf.b *= 0.2F + f * 0.8F;
                                    inf.a *= 0.25F + f * 0.75F;
                                } else {
                                    inf.r *= 0.2F;
                                    inf.g *= 0.2F;
                                    inf.b *= 0.2F;
                                    inf.a *= 0.25F;
                                }

                                if (b.fade > 0) {
                                    inf.a = inf.a * (b.fade / (PerformanceSettings.getLockFPS() * 5.0F));
                                    if (--b.fade == 0) {
                                        b.chunk.floorBloodSplatsFade.remove(b);
                                    }
                                }

                                IsoGridSquare square = b.chunk.getGridSquare(PZMath.fastfloor(b.x), PZMath.fastfloor(b.y), PZMath.fastfloor(b.z));
                                if (square != null) {
                                    int L0 = square.getVertLight(0, playerIndex);
                                    int L1 = square.getVertLight(1, playerIndex);
                                    int L2 = square.getVertLight(2, playerIndex);
                                    int L3 = square.getVertLight(3, playerIndex);
                                    float r0 = Color.getRedChannelFromABGR(L0);
                                    float g0 = Color.getGreenChannelFromABGR(L0);
                                    float b0 = Color.getBlueChannelFromABGR(L0);
                                    float r1 = Color.getRedChannelFromABGR(L1);
                                    float g1 = Color.getGreenChannelFromABGR(L1);
                                    float b1 = Color.getBlueChannelFromABGR(L1);
                                    float r2 = Color.getRedChannelFromABGR(L2);
                                    float g2 = Color.getGreenChannelFromABGR(L2);
                                    float b2 = Color.getBlueChannelFromABGR(L2);
                                    float r3 = Color.getRedChannelFromABGR(L3);
                                    float g3 = Color.getGreenChannelFromABGR(L3);
                                    float b3 = Color.getBlueChannelFromABGR(L3);
                                    inf.r *= (r0 + r1 + r2 + r3) / 4.0F;
                                    inf.g *= (g0 + g1 + g2 + g3) / 4.0F;
                                    inf.b *= (b0 + b1 + b2 + b3) / 4.0F;
                                }

                                use.renderBloodSplat(b.chunk.wx * 8 + b.x, b.chunk.wy * 8 + b.y, b.z, inf);
                            }
                        }
                    }
                }
            }
        }
    }

    public void copy(IsoChunkMap from) {
        IsoChunkMap to = this;
        this.worldX = from.worldX;
        this.worldY = from.worldY;
        this.xMinTiles = -1;
        this.yMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMaxTiles = -1;

        for (int n = 0; n < chunkGridWidth * chunkGridWidth; n++) {
            to.readBufferA = from.readBufferA;
            if (to.readBufferA) {
                if (from.chunksSwapA[n] != null) {
                    from.chunksSwapA[n].refs.add(to);
                    to.chunksSwapA[n] = from.chunksSwapA[n];
                }
            } else if (from.chunksSwapB[n] != null) {
                from.chunksSwapB[n].refs.add(to);
                to.chunksSwapB[n] = from.chunksSwapB[n];
            }
        }
    }

    public void Unload() {
        for (int y = 0; y < chunkGridWidth; y++) {
            for (int x = 0; x < chunkGridWidth; x++) {
                IsoChunk ch = this.getChunk(x, y);
                if (ch != null) {
                    if (ch.refs.contains(this)) {
                        ch.refs.remove(this);
                        if (ch.refs.isEmpty()) {
                            SharedChunks.remove((ch.wx << 16) + ch.wy);
                            ch.removeFromWorld();
                            ChunkSaveWorker.instance.Add(ch);
                        }
                    }

                    this.chunksSwapA[y * chunkGridWidth + x] = null;
                    this.chunksSwapB[y * chunkGridWidth + x] = null;
                }
            }
        }

        WorldSimulation.instance.deactivateChunkMap(this.playerId);
        this.xMinTiles = -1;
        this.xMaxTiles = -1;
        this.yMinTiles = -1;
        this.yMaxTiles = -1;
        if (IsoWorld.instance != null && IsoWorld.instance.currentCell != null) {
            IsoWorld.instance.currentCell.clearCacheGridSquare(this.playerId);
        }
    }

    public static boolean isGridSquareOutOfRangeZ(int tileZ) {
        return tileZ < -32 || tileZ > 31;
    }

    static {
        for (int i = 0; i < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; i++) {
            splatByType.add(new ArrayList<>());
        }

        ppp_update = new PerformanceProfileProbe("IsoChunkMap.update");
    }
}
