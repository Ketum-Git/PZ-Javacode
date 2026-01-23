// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.VirtualZombieManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Roles;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.ImportantAreaManager;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.stash.StashSystem;
import zombie.core.utils.OnceEvery;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.entity.GameEntityManager;
import zombie.globalObjects.SGlobalObjects;
import zombie.iso.InstanceTracker;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.MetaTracker;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.WorldGenerate;
import zombie.iso.worldgen.WorldGenParams;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.ZombiePopulationManager;
import zombie.radio.ZomboidRadio;
import zombie.savefile.ServerPlayerDB;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;
import zombie.world.moddata.GlobalModData;
import zombie.worldMap.network.WorldMapServer;

public class ServerMap {
    public boolean updateLosThisFrame;
    public static final OnceEvery LOS_TICK = new OnceEvery(1.0F);
    public static final OnceEvery TIME_TICK = new OnceEvery(600.0F);
    public static final int CellSize = 64;
    public static final int ChunksPerCellWidth = 8;
    public long lastSaved;
    private static boolean mapLoading;
    public final IsoObjectID<IsoZombie> zombieMap = new IsoObjectID<>(IsoZombie.class);
    public boolean queuedSaveAll;
    public boolean queuedQuit;
    public static ServerMap instance = new ServerMap();
    public ServerMap.ServerCell[] cellMap;
    public ArrayList<ServerMap.ServerCell> loadedCells = new ArrayList<>();
    public ArrayList<ServerMap.ServerCell> releventNow = new ArrayList<>();
    int width;
    int height;
    IsoMetaGrid grid;
    ArrayList<ServerMap.ServerCell> toLoad = new ArrayList<>();
    static final ServerMap.DistToCellComparator distToCellComparator = new ServerMap.DistToCellComparator();
    private final ArrayList<ServerMap.ServerCell> tempCells = new ArrayList<>();
    long lastTick;

    public short getUniqueZombieId() {
        return this.zombieMap.allocateID();
    }

    public void SaveAll() {
        long start = System.nanoTime();

        for (int n = 0; n < this.loadedCells.size(); n++) {
            this.loadedCells.get(n).Save();
        }

        this.grid.save();
        DebugLog.log("SaveAll took " + (System.nanoTime() - start) / 1000000.0 + " ms");
    }

    public void QueueSaveAll() {
        this.queuedSaveAll = true;
    }

    public void QueueQuit() {
        this.queuedQuit = true;
    }

    public int toServerCellX(int x) {
        return PZMath.coorddivision(x * 256, 64);
    }

    public int toServerCellY(int y) {
        return PZMath.coorddivision(y * 256, 64);
    }

    public int toWorldCellX(int x) {
        return PZMath.coorddivision(x * 64, 256);
    }

    public int toWorldCellY(int y) {
        return PZMath.coorddivision(y * 64, 256);
    }

    public int getMaxX() {
        int x = this.toServerCellX(this.grid.maxX + 1);
        if ((this.grid.maxX + 1) * 256 % 64 == 0) {
            x--;
        }

        return x;
    }

    public int getMaxY() {
        int y = this.toServerCellY(this.grid.maxY + 1);
        if ((this.grid.maxY + 1) * 256 % 64 == 0) {
            y--;
        }

        return y;
    }

    public int getMinX() {
        return this.toServerCellX(this.grid.minX);
    }

    public int getMinY() {
        return this.toServerCellY(this.grid.minY);
    }

    public void init(IsoMetaGrid metaGrid) {
        this.grid = metaGrid;
        this.width = this.getMaxX() - this.getMinX() + 1;
        this.height = this.getMaxY() - this.getMinY() + 1;

        assert this.width * 64 >= metaGrid.getWidth() * 256;

        assert this.height * 64 >= metaGrid.getHeight() * 256;

        assert this.getMaxX() * 64 < (metaGrid.getMaxX() + 1) * 256;

        assert this.getMaxY() * 64 < (metaGrid.getMaxY() + 1) * 256;

        int tot = this.width * this.height;
        this.cellMap = new ServerMap.ServerCell[tot];
        StashSystem.init();
    }

    public ServerMap.ServerCell getCell(int x, int y) {
        return this.isInvalidCell(x, y) ? null : this.cellMap[y * this.width + x];
    }

    public boolean isInvalidCell(int x, int y) {
        return x < 0 || y < 0 || x >= this.width || y >= this.height;
    }

    public void loadOrKeepRelevent(int x, int y) {
        if (!this.isInvalidCell(x, y)) {
            ServerMap.ServerCell cell = this.getCell(x, y);
            if (cell == null) {
                cell = new ServerMap.ServerCell();
                cell.wx = x + this.getMinX();
                cell.wy = y + this.getMinY();
                if (cell.wx == -1 && cell.wy == -1) {
                    return;
                }

                if (mapLoading) {
                    DebugLog.MapLoading
                        .debugln("Loading cell: " + cell.wx + ", " + cell.wy + " (" + this.toWorldCellX(cell.wx) + ", " + this.toWorldCellY(cell.wy) + ")");
                }

                this.cellMap[y * this.width + x] = cell;
                this.toLoad.add(cell);
                this.loadedCells.add(cell);
                this.releventNow.add(cell);
            } else if (!this.releventNow.contains(cell)) {
                this.releventNow.add(cell);
            }
        }
    }

    public void characterIn(IsoPlayer p) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var9) {
                var9.printStackTrace();
            }
        }

        int dist = p.onlineChunkGridWidth / 2 * 8;
        int minX = PZMath.fastfloor((p.getX() - dist) / 64.0F) - this.getMinX();
        int maxX = PZMath.fastfloor((p.getX() + dist) / 64.0F) - this.getMinX();
        int minY = PZMath.fastfloor((p.getY() - dist) / 64.0F) - this.getMinY();
        int maxY = PZMath.fastfloor((p.getY() + dist) / 64.0F) - this.getMinY();

        for (int yy = minY; yy <= maxY; yy++) {
            for (int xx = minX; xx <= maxX; xx++) {
                this.loadOrKeepRelevent(xx, yy);
            }
        }
    }

    public void characterIn(int wx, int wy, int chunkGridWidth) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var17) {
                var17.printStackTrace();
            }
        }

        int x = wx * 8;
        int y = wy * 8;
        x = PZMath.coorddivision(x, 64);
        y = PZMath.coorddivision(y, 64);
        x -= this.getMinX();
        y -= this.getMinY();
        int cx = PZMath.fastfloor((float)x);
        int cy = PZMath.fastfloor((float)y);
        int lx = wx * 8 % 64;
        int ly = wy * 8 % 64;
        int dist = chunkGridWidth / 2 * 8;
        int minX = cx;
        int minY = cy;
        int maxX = cx;
        int maxY = cy;
        if (lx < dist) {
            minX = cx - 1;
        }

        if (lx > 64 - dist) {
            maxX = cx + 1;
        }

        if (ly < dist) {
            minY = cy - 1;
        }

        if (ly > 64 - dist) {
            maxY = cy + 1;
        }

        for (int yy = minY; yy <= maxY; yy++) {
            for (int xx = minX; xx <= maxX; xx++) {
                this.loadOrKeepRelevent(xx, yy);
            }
        }
    }

    public void importantAreaIn(int sx, int sy) {
        while (this.grid == null) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var5) {
                var5.printStackTrace();
            }
        }

        int x = PZMath.fastfloor((float)sx);
        int y = PZMath.fastfloor((float)sy);
        x = PZMath.coorddivision(x, 64);
        y = PZMath.coorddivision(y, 64);
        x -= this.getMinX();
        y -= this.getMinY();
        this.loadOrKeepRelevent(x, y);
    }

    public void QueuedQuit() {
        this.QueuedSaveAll();
        ByteBufferWriter b = GameServer.udpEngine.startPacket();
        PacketTypes.PacketType.ServerQuit.doPacket(b);
        GameServer.udpEngine.endPacketBroadcast(PacketTypes.PacketType.ServerQuit);
        WorldGenerate.instance.stop();

        try {
            Thread.sleep(5000L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        Roles.save();
        PathfindNative.instance.stop();
        PathfindNative.freeMemoryAtExit();
        MapCollisionData.instance.stop();
        AnimalPopulationManager.getInstance().stop();
        ZombiePopulationManager.instance.stop();
        RCONServer.shutdown();
        ServerMap.ServerCell.chunkLoader.quit();
        ServerWorldDatabase.instance.close();
        ServerPlayersVehicles.instance.stop();
        ServerPlayerDB.getInstance().close();
        ObjectIDManager.getInstance().checkForSaveDataFile(true);
        ImportantAreaManager.getInstance().saveDataFile();
        VehiclesDB2.instance.Reset();
        GameServer.udpEngine.Shutdown();
        ServerGUI.shutdown();
        SteamUtils.shutdown();
    }

    public void QueuedSaveAll() {
        long start = System.nanoTime();
        this.SaveAll();
        ServerPlayerDB.getInstance().save();
        ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
        ReanimatedPlayers.instance.saveReanimatedPlayers();
        AnimalPopulationManager.getInstance().save();
        MapCollisionData.instance.save();
        SGlobalObjects.save();
        WorldGenParams.INSTANCE.save();
        InstanceTracker.save();
        MetaTracker.save();

        try {
            ZomboidRadio.getInstance().Save();
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        try {
            GlobalModData.instance.save();
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        GameEntityManager.Save();
        WorldMapServer.instance.writeSavefile();
        INetworkPacket.sendToAll(PacketTypes.PacketType.StopPause, null);
        System.out.println("Saving finish");
        DebugLog.log("Saving took " + (System.nanoTime() - start) / 1000000.0 + " ms");
    }

    public void preupdate() {
        this.lastTick = System.nanoTime();
        mapLoading = DebugType.MapLoading.isEnabled();

        for (int i = 0; i < this.toLoad.size(); i++) {
            ServerMap.ServerCell cell = this.toLoad.get(i);
            if (cell.loadingWasCancelled) {
                if (mapLoading) {
                    DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
                }

                int cx = cell.wx - this.getMinX();
                int cy = cell.wy - this.getMinY();

                assert this.cellMap[cx + cy * this.width] == cell;

                this.cellMap[cx + cy * this.width] = null;
                this.loadedCells.remove(cell);
                this.releventNow.remove(cell);
                ServerMap.ServerCell.loaded2.remove(cell);
                this.toLoad.remove(i--);
            }
        }

        for (int ix = 0; ix < this.loadedCells.size(); ix++) {
            ServerMap.ServerCell cell = this.loadedCells.get(ix);
            if (cell.cancelLoading) {
                if (mapLoading) {
                    DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
                }

                int cx = cell.wx - this.getMinX();
                int cy = cell.wy - this.getMinY();

                assert this.cellMap[cx + cy * this.width] == cell;

                this.cellMap[cx + cy * this.width] = null;
                this.loadedCells.remove(ix--);
                this.releventNow.remove(cell);
                ServerMap.ServerCell.loaded2.remove(cell);
                this.toLoad.remove(cell);
            }
        }

        for (int ixx = 0; ixx < ServerMap.ServerCell.loaded2.size(); ixx++) {
            ServerMap.ServerCell cell = ServerMap.ServerCell.loaded2.get(ixx);
            if (cell.cancelLoading) {
                if (mapLoading) {
                    DebugLog.MapLoading.debugln("MainThread: forgetting cancelled " + cell.wx + "," + cell.wy);
                }

                int cx = cell.wx - this.getMinX();
                int cy = cell.wy - this.getMinY();

                assert this.cellMap[cx + cy * this.width] == cell;

                this.cellMap[cx + cy * this.width] = null;
                this.loadedCells.remove(cell);
                this.releventNow.remove(cell);
                ServerMap.ServerCell.loaded2.remove(cell);
                this.toLoad.remove(cell);
            }
        }

        if (!this.toLoad.isEmpty()) {
            this.tempCells.clear();

            for (int ixxx = 0; ixxx < this.toLoad.size(); ixxx++) {
                ServerMap.ServerCell cell = this.toLoad.get(ixxx);
                if (!cell.cancelLoading && !cell.startedLoading) {
                    this.tempCells.add(cell);
                }
            }

            if (!this.tempCells.isEmpty()) {
                distToCellComparator.init();
                this.tempCells.sort(distToCellComparator);

                for (int ixxxx = 0; ixxxx < this.tempCells.size(); ixxxx++) {
                    ServerMap.ServerCell cell = this.tempCells.get(ixxxx);
                    ServerMap.ServerCell.chunkLoader.addJob(cell);
                    cell.startedLoading = true;
                }
            }

            ServerMap.ServerCell.chunkLoader.getLoaded(ServerMap.ServerCell.loaded);

            for (int ixxxx = 0; ixxxx < ServerMap.ServerCell.loaded.size(); ixxxx++) {
                ServerMap.ServerCell cell = ServerMap.ServerCell.loaded.get(ixxxx);
                if (!cell.doingRecalc) {
                    ServerMap.ServerCell.chunkLoader.addRecalcJob(cell);
                    cell.doingRecalc = true;
                }
            }

            ServerMap.ServerCell.loaded.clear();
            ServerMap.ServerCell.chunkLoader.getRecalc(ServerMap.ServerCell.loaded2);
            if (!ServerMap.ServerCell.loaded2.isEmpty()) {
                try {
                    ServerLOS.instance.suspend();

                    for (int x = 0; x < ServerMap.ServerCell.loaded2.size(); x++) {
                        ServerMap.ServerCell cell = ServerMap.ServerCell.loaded2.get(x);
                        if (cell.Load2()) {
                            x--;
                            this.toLoad.remove(cell);
                        }
                    }
                } finally {
                    ServerLOS.instance.resume();
                }
            }
        }

        int SaveWorldEveryMinutes = ServerOptions.instance.saveWorldEveryMinutes.getValue();
        if (SaveWorldEveryMinutes > 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime > this.lastSaved + SaveWorldEveryMinutes * 60L * 1000L) {
                this.queuedSaveAll = true;
                this.lastSaved = currentTime;
            }
        }

        if (this.queuedSaveAll) {
            this.queuedSaveAll = false;
            this.QueuedSaveAll();
        }

        if (this.queuedQuit) {
            System.exit(0);
        }

        this.releventNow.clear();
        this.updateLosThisFrame = LOS_TICK.Check();
        if (TIME_TICK.Check()) {
            ServerMap.ServerCell.chunkLoader.saveLater(GameTime.instance);
        }
    }

    public void postupdate() {
        boolean pathfindPaused = false;

        try {
            for (int n = 0; n < this.loadedCells.size(); n++) {
                ServerMap.ServerCell cell = this.loadedCells.get(n);
                boolean shouldBeLoaded = this.releventNow.contains(cell) || !this.outsidePlayerInfluence(cell);
                if (!cell.isLoaded) {
                    if (!shouldBeLoaded && !cell.cancelLoading) {
                        if (mapLoading) {
                            DebugLog.log(
                                DebugType.MapLoading, "MainThread: cancelling " + cell.wx + "," + cell.wy + " cell.startedLoading=" + cell.startedLoading
                            );
                        }

                        if (!cell.startedLoading) {
                            cell.loadingWasCancelled = true;
                        }

                        cell.cancelLoading = true;
                    }
                } else if (!shouldBeLoaded) {
                    int x = cell.wx - this.getMinX();
                    int y = cell.wy - this.getMinY();
                    if (!pathfindPaused) {
                        ServerLOS.instance.suspend();
                        pathfindPaused = true;
                    }

                    this.cellMap[y * this.width + x].Unload();
                    this.cellMap[y * this.width + x] = null;
                    this.loadedCells.remove(cell);
                    n--;
                } else {
                    cell.update();
                }
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        } finally {
            if (pathfindPaused) {
                ServerLOS.instance.resume();
            }
        }

        NetworkZombiePacker.getInstance().postupdate();
        ServerMap.ServerCell.chunkLoader.updateSaved();
    }

    public void physicsCheck(int x, int y) {
        int cx = PZMath.coorddivision(x, 64) - this.getMinX();
        int cy = PZMath.coorddivision(y, 64) - this.getMinY();
        ServerMap.ServerCell cell = this.getCell(cx, cy);
        if (cell != null && cell.isLoaded) {
            cell.physicsCheck = true;
        }
    }

    private boolean outsidePlayerInfluence(ServerMap.ServerCell cell) {
        int x1 = cell.wx * 64;
        int y1 = cell.wy * 64;
        int x2 = (cell.wx + 1) * 64;
        int y2 = (cell.wy + 1) * 64;

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.RelevantTo(x1, y1)) {
                return false;
            }

            if (c.RelevantTo(x2, y1)) {
                return false;
            }

            if (c.RelevantTo(x2, y2)) {
                return false;
            }

            if (c.RelevantTo(x1, y2)) {
                return false;
            }
        }

        return true;
    }

    public int worldSquareToServerCellXY(int worldSquareXY) {
        return PZMath.coorddivision(worldSquareXY, 64);
    }

    public int worldChunkToServerCellXY(int worldChunkXY) {
        return PZMath.coorddivision(worldChunkXY, 8);
    }

    public static IsoGridSquare getGridSquare(Vector3 v) {
        return instance.getGridSquare(PZMath.fastfloor(v.x), PZMath.fastfloor(v.y), PZMath.fastfloor(v.z));
    }

    public IsoGridSquare getGridSquare(int x, int y, int z) {
        if (!IsoWorld.instance.isValidSquare(x, y, z)) {
            return null;
        } else {
            int cx = this.worldSquareToServerCellXY(x);
            int cy = this.worldSquareToServerCellXY(y);
            int chx = (x - cx * 64) / 8;
            int chy = (y - cy * 64) / 8;
            int sqx = (x - cx * 64) % 8;
            int sqy = (y - cy * 64) % 8;
            cx -= this.getMinX();
            cy -= this.getMinY();
            ServerMap.ServerCell cell = this.getCell(cx, cy);
            if (cell != null && cell.isLoaded) {
                IsoChunk c = cell.chunks[chx][chy];
                return c == null ? null : c.getGridSquare(sqx, sqy, z);
            } else {
                return null;
            }
        }
    }

    public void setGridSquare(int x, int y, int z, IsoGridSquare sq) {
        int cx = this.worldSquareToServerCellXY(x);
        int cy = this.worldSquareToServerCellXY(y);
        int chx = (x - cx * 64) / 8;
        int chy = (y - cy * 64) / 8;
        int sqx = (x - cx * 64) % 8;
        int sqy = (y - cy * 64) % 8;
        cx -= this.getMinX();
        cy -= this.getMinY();
        ServerMap.ServerCell cell = this.getCell(cx, cy);
        if (cell != null) {
            IsoChunk c = cell.chunks[chx][chy];
            if (c != null) {
                c.setSquare(sqx, sqy, z, sq);
            }
        }
    }

    public IsoChunk getChunk(int wx, int wy) {
        int cx = this.worldChunkToServerCellXY(wx);
        int cy = this.worldChunkToServerCellXY(wy);
        int chx = (wx - cx * 8) % 8;
        int chy = (wy - cy * 8) % 8;
        cx -= this.getMinX();
        cy -= this.getMinY();
        ServerMap.ServerCell cell = this.getCell(cx, cy);
        return cell != null && cell.isLoaded ? cell.chunks[chx][chy] : null;
    }

    public void setSoftResetChunk(IsoChunk chunk) {
        int cx = this.worldChunkToServerCellXY(chunk.wx) - this.getMinX();
        int cy = this.worldChunkToServerCellXY(chunk.wy) - this.getMinY();
        if (!this.isInvalidCell(cx, cy)) {
            ServerMap.ServerCell cell = this.getCell(cx, cy);
            if (cell == null) {
                cell = new ServerMap.ServerCell();
                cell.isLoaded = true;
                this.cellMap[cy * this.width + cx] = cell;
            }

            int chx = (chunk.wx - cx * 8) % 8;
            int chy = (chunk.wy - cy * 8) % 8;
            cell.chunks[chx][chy] = chunk;
        }
    }

    public void clearSoftResetChunk(IsoChunk chunk) {
        int cx = this.worldChunkToServerCellXY(chunk.wx) - this.getMinX();
        int cy = this.worldChunkToServerCellXY(chunk.wy) - this.getMinY();
        ServerMap.ServerCell cell = this.getCell(cx, cy);
        if (cell != null) {
            int chx = (chunk.wx - cx * 8) % 8;
            int chy = (chunk.wy - cy * 8) % 8;
            cell.chunks[chx][chy] = null;
        }
    }

    private static final class DistToCellComparator implements Comparator<ServerMap.ServerCell> {
        private final Vector2[] pos = new Vector2[1024];
        private int posCount;

        public DistToCellComparator() {
            for (int i = 0; i < this.pos.length; i++) {
                this.pos[i] = new Vector2();
            }
        }

        public void init() {
            this.posCount = 0;

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.isFullyConnected()) {
                    for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                        if (c.players[playerIndex] != null) {
                            this.pos[this.posCount].set(c.players[playerIndex].getX(), c.players[playerIndex].getY());
                            this.posCount++;
                        }
                    }
                }
            }
        }

        public int compare(ServerMap.ServerCell a, ServerMap.ServerCell b) {
            float aScore = Float.MAX_VALUE;
            float bScore = Float.MAX_VALUE;

            for (int i = 0; i < this.posCount; i++) {
                float x = this.pos[i].x;
                float y = this.pos[i].y;
                aScore = Math.min(aScore, this.distToCell(x, y, a));
                bScore = Math.min(bScore, this.distToCell(x, y, b));
            }

            return Float.compare(aScore, bScore);
        }

        private float distToCell(float x, float y, ServerMap.ServerCell cell) {
            int minX = cell.wx * 64;
            int minY = cell.wy * 64;
            int maxX = minX + 64;
            int maxY = minY + 64;
            float closestX = x;
            float closestY = y;
            if (x < minX) {
                closestX = minX;
            } else if (x > maxX) {
                closestX = maxX;
            }

            if (y < minY) {
                closestY = minY;
            } else if (y > maxY) {
                closestY = maxY;
            }

            return IsoUtils.DistanceToSquared(x, y, closestX, closestY);
        }
    }

    public static final class ServerCell {
        public int wx;
        public int wy;
        public boolean isLoaded;
        public boolean physicsCheck;
        public final IsoChunk[][] chunks = new IsoChunk[8][8];
        private final HashSet<RoomDef> unexploredRooms = new HashSet<>();
        private static final ServerChunkLoader chunkLoader = new ServerChunkLoader();
        private static final ArrayList<ServerMap.ServerCell> loaded = new ArrayList<>();
        private boolean startedLoading;
        public boolean cancelLoading;
        public boolean loadingWasCancelled;
        private static final ArrayList<ServerMap.ServerCell> loaded2 = new ArrayList<>();
        private boolean doingRecalc;
        private final UpdateLimit hotSaveFrequency = new UpdateLimit(1000L);

        public boolean Load2() {
            chunkLoader.getRecalc(loaded2);

            for (int i = 0; i < loaded2.size(); i++) {
                if (loaded2.get(i) == this) {
                    long start = System.nanoTime();
                    this.RecalcAll2();
                    loaded2.remove(i);
                    if (ServerMap.mapLoading) {
                        DebugLog.MapLoading.debugln("loaded2=" + loaded2);
                    }

                    float time = (float)(System.nanoTime() - start) / 1000000.0F;
                    if (ServerMap.mapLoading) {
                        DebugLog.MapLoading.debugln("finish loading cell " + this.wx + "," + this.wy + " ms=" + time);
                    }

                    this.loadVehicles();
                    return true;
                }
            }

            return false;
        }

        private void loadVehicles() {
            for (int cx = 0; cx < 8; cx++) {
                for (int cy = 0; cy < 8; cy++) {
                    IsoChunk chunk = this.chunks[cx][cy];
                    if (chunk != null && !chunk.isNewChunk()) {
                        VehiclesDB2.instance.loadChunkMain(chunk);
                    }
                }
            }
        }

        public void RecalcAll2() {
            int sx = this.wx * 8 * 8;
            int sy = this.wy * 8 * 8;
            int ex = sx + 64;
            int ey = sy + 64;

            for (RoomDef def : this.unexploredRooms) {
                def.indoorZombies--;
            }

            this.unexploredRooms.clear();
            this.isLoaded = true;
            int minLevel = Integer.MAX_VALUE;
            int maxLevel = Integer.MIN_VALUE;

            for (int chunkY = 0; chunkY < 8; chunkY++) {
                for (int chunkX = 0; chunkX < 8; chunkX++) {
                    IsoChunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk != null) {
                        minLevel = PZMath.min(minLevel, chunk.getMinLevel());
                        maxLevel = PZMath.max(maxLevel, chunk.getMaxLevel());
                    }
                }
            }

            for (int z = 1; z <= maxLevel; z++) {
                for (int x = -1; x < 65; x++) {
                    IsoGridSquare sq = ServerMap.instance.getGridSquare(sx + x, sy - 1, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                    } else if (x >= 0 && x < 64) {
                        sq = ServerMap.instance.getGridSquare(sx + x, sy, z);
                        if (sq != null && !sq.getObjects().isEmpty()) {
                            IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                        }
                    }

                    sq = ServerMap.instance.getGridSquare(sx + x, sy + 64, z);
                    if (sq != null && !sq.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                    } else if (x >= 0 && x < 64) {
                        ServerMap.instance.getGridSquare(sx + x, sy + 64 - 1, z);
                        if (sq != null && !sq.getObjects().isEmpty()) {
                            IsoWorld.instance.currentCell.EnsureSurroundNotNull(sq.x, sq.y, z);
                        }
                    }
                }

                for (int y = 0; y < 64; y++) {
                    IsoGridSquare sqx = ServerMap.instance.getGridSquare(sx - 1, sy + y, z);
                    if (sqx != null && !sqx.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sqx.x, sqx.y, z);
                    } else {
                        sqx = ServerMap.instance.getGridSquare(sx, sy + y, z);
                        if (sqx != null && !sqx.getObjects().isEmpty()) {
                            IsoWorld.instance.currentCell.EnsureSurroundNotNull(sqx.x, sqx.y, z);
                        }
                    }

                    sqx = ServerMap.instance.getGridSquare(sx + 64, sy + y, z);
                    if (sqx != null && !sqx.getObjects().isEmpty()) {
                        IsoWorld.instance.currentCell.EnsureSurroundNotNull(sqx.x, sqx.y, z);
                    } else {
                        sqx = ServerMap.instance.getGridSquare(sx + 64 - 1, sy + y, z);
                        if (sqx != null && !sqx.getObjects().isEmpty()) {
                            IsoWorld.instance.currentCell.EnsureSurroundNotNull(sqx.x, sqx.y, z);
                        }
                    }
                }
            }

            for (int z = minLevel; z <= maxLevel; z++) {
                for (int x = 0; x < 64; x++) {
                    IsoGridSquare sqxx = ServerMap.instance.getGridSquare(sx + x, sy, z);
                    if (sqxx != null) {
                        sqxx.RecalcAllWithNeighbours(true);
                    }

                    sqxx = ServerMap.instance.getGridSquare(sx + x, ey - 1, z);
                    if (sqxx != null) {
                        sqxx.RecalcAllWithNeighbours(true);
                    }
                }

                for (int y = 0; y < 64; y++) {
                    IsoGridSquare sqxxx = ServerMap.instance.getGridSquare(sx, sy + y, z);
                    if (sqxxx != null) {
                        sqxxx.RecalcAllWithNeighbours(true);
                    }

                    sqxxx = ServerMap.instance.getGridSquare(ex - 1, sy + y, z);
                    if (sqxxx != null) {
                        sqxxx.RecalcAllWithNeighbours(true);
                    }
                }
            }

            int nSquares = 64;

            for (int cx = 0; cx < 8; cx++) {
                for (int cy = 0; cy < 8; cy++) {
                    IsoChunk chunk = this.chunks[cx][cy];
                    if (chunk != null) {
                        chunk.loaded = true;

                        for (int i = 0; i < 64; i++) {
                            for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                                int squaresIndexOfLevel = chunk.squaresIndexOfLevel(z);
                                IsoGridSquare g = chunk.squares[squaresIndexOfLevel][i];
                                if (g != null) {
                                    if (g.getRoom() != null && !g.getRoom().def.explored) {
                                        this.unexploredRooms.add(g.getRoom().def);
                                    }

                                    g.propertiesDirty = true;
                                }
                            }
                        }
                    }
                }
            }

            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    if (this.chunks[x][y] != null) {
                        this.chunks[x][y].doLoadGridsquare();
                    }
                }
            }

            for (RoomDef def : this.unexploredRooms) {
                def.indoorZombies++;
                if (def.indoorZombies == 1) {
                    try {
                        VirtualZombieManager.instance.tryAddIndoorZombies(def, false);
                    } catch (Exception var15) {
                        var15.printStackTrace();
                    }
                }
            }

            this.isLoaded = true;
        }

        public void Unload() {
            if (this.isLoaded) {
                if (ServerMap.mapLoading) {
                    DebugLog.MapLoading
                        .debugln(
                            "Unloading cell: "
                                + this.wx
                                + ", "
                                + this.wy
                                + " ("
                                + ServerMap.instance.toWorldCellX(this.wx)
                                + ", "
                                + ServerMap.instance.toWorldCellY(this.wy)
                                + ")"
                        );
                }

                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        IsoChunk chunk = this.chunks[x][y];
                        if (chunk != null) {
                            chunk.removeFromWorld();
                            chunk.loadVehiclesObject = null;

                            for (int i = 0; i < chunk.vehicles.size(); i++) {
                                BaseVehicle vehicle = chunk.vehicles.get(i);
                                VehiclesDB2.instance.updateVehicle(vehicle);
                            }

                            chunkLoader.addSaveUnloadedJob(chunk);
                            this.chunks[x][y] = null;
                        }
                    }
                }

                for (RoomDef def : this.unexploredRooms) {
                    def.indoorZombies--;
                }
            }
        }

        public void Save() {
            if (this.isLoaded) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        IsoChunk chunk = this.chunks[x][y];
                        if (chunk != null) {
                            try {
                                chunkLoader.addSaveLoadedJob(chunk);

                                for (int i = 0; i < chunk.vehicles.size(); i++) {
                                    BaseVehicle vehicle = chunk.vehicles.get(i);
                                    VehiclesDB2.instance.updateVehicle(vehicle);
                                }
                            } catch (Exception var6) {
                                var6.printStackTrace();
                                LoggerManager.getLogger("map").write(var6);
                            }
                        }
                    }
                }

                chunkLoader.updateSaved();
            }
        }

        public void saveChunk(IsoChunk chunk) {
            if (this.isLoaded) {
                if (chunk != null) {
                    chunkLoader.addSaveLoadedJob(chunk);
                }
            }
        }

        public void update() {
            boolean shouldProcessHotSaves = !GameServer.server && this.hotSaveFrequency.Check();

            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    IsoChunk chunk = this.chunks[x][y];
                    if (chunk != null) {
                        chunk.update();
                        if (shouldProcessHotSaves && chunk.requiresHotSave) {
                            this.saveChunk(chunk);
                            chunk.requiresHotSave = false;
                        }
                    }
                }
            }

            this.physicsCheck = false;
        }

        public IsoChunk getChunk(int x, int y) {
            if (x >= 0 && x < 8 && y >= 0 && y < 8) {
                IsoChunk chunk = this.chunks[x][y];
                if (chunk != null) {
                    return chunk;
                }
            }

            return null;
        }

        public int getWX() {
            return this.wx;
        }

        public int getWY() {
            return this.wy;
        }
    }
}
