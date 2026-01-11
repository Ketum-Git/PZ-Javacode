// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.WorldStreamer;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.popman.ZombiePopulationRenderer;
import zombie.util.ByteBufferBackedInputStream;
import zombie.util.ByteBufferOutputStream;
import zombie.util.PZSQLUtils;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.Type;

public final class VehiclesDB2 {
    public static final int INVALID_ID = -1;
    private static final int MIN_ID = 1;
    public static final VehiclesDB2 instance = new VehiclesDB2();
    private static final ThreadLocal<ByteBuffer> TL_SliceBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(32768));
    private static final ThreadLocal<byte[]> TL_Bytes = ThreadLocal.withInitial(() -> new byte[1024]);
    private final VehiclesDB2.MainThread main = new VehiclesDB2.MainThread();
    private final VehiclesDB2.WorldStreamerThread worldStreamer = new VehiclesDB2.WorldStreamerThread();

    public void init() {
        this.worldStreamer.store.init(this.main.usedIds, this.main.seenChunks);
    }

    public void Reset() {
        assert WorldStreamer.instance.worldStreamer == null;

        this.updateWorldStreamer();

        for (VehiclesDB2.QueueItem item = this.main.queue.poll(); item != null; item = this.main.queue.poll()) {
            item.release();
        }

        this.main.Reset();
        this.worldStreamer.Reset();
    }

    public void updateMain() throws IOException {
        this.main.update();
    }

    public void updateWorldStreamer() {
        this.worldStreamer.update();
    }

    public void setForceSave() {
        this.main.forceSave = true;
    }

    public void renderDebug(ZombiePopulationRenderer renderer) {
    }

    public void setChunkSeen(int wx, int wy) {
        this.main.setChunkSeen(wx, wy);
    }

    public boolean isChunkSeen(int wx, int wy) {
        return this.main.isChunkSeen(wx, wy);
    }

    public void setVehicleLoaded(BaseVehicle vehicle) {
        this.main.setVehicleLoaded(vehicle);
    }

    public void setVehicleUnloaded(BaseVehicle vehicle) {
        this.main.setVehicleUnloaded(vehicle);
    }

    public boolean isVehicleLoaded(BaseVehicle vehicle) {
        return this.main.loadedIds.contains(vehicle.sqlId);
    }

    public void loadChunkMain(IsoChunk chunk) {
        this.main.loadChunk(chunk);
    }

    public void loadChunk(IsoChunk chunk) throws IOException {
        this.worldStreamer.loadChunk(chunk);
    }

    public void unloadChunk(IsoChunk chunk) {
        if (Thread.currentThread() != WorldStreamer.instance.worldStreamer) {
            boolean var2 = true;
        }

        this.worldStreamer.unloadChunk(chunk);
    }

    public void addVehicle(BaseVehicle vehicle) {
        try {
            this.main.addVehicle(vehicle);
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }
    }

    public void removeVehicle(BaseVehicle vehicle) {
        this.main.removeVehicle(vehicle);
    }

    public void updateVehicle(BaseVehicle vehicle) {
        try {
            this.main.updateVehicle(vehicle);
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }
    }

    public void updateVehicleAndTrailer(BaseVehicle vehicle) {
        if (vehicle != null) {
            this.updateVehicle(vehicle);
            BaseVehicle trailer = vehicle.getVehicleTowing();
            if (trailer != null) {
                this.updateVehicle(trailer);
            }
        }
    }

    public void importPlayersFromOldDB(VehiclesDB2.IImportPlayerFromOldDB consumer) {
        VehiclesDB2.SQLStore sqlStore = Type.tryCastTo(this.worldStreamer.store, VehiclesDB2.SQLStore.class);
        if (sqlStore != null && sqlStore.conn != null) {
            try {
                label100: {
                    DatabaseMetaData metaData = sqlStore.conn.getMetaData();

                    try (ResultSet rs = metaData.getTables(null, null, "localPlayers", null)) {
                        if (rs.next()) {
                            break label100;
                        }
                    }

                    return;
                }
            } catch (Exception var22) {
                ExceptionLogger.logException(var22);
                return;
            }

            String sql = "SELECT id, name, wx, wy, x, y, z, worldversion, data, isDead FROM localPlayers";

            try (PreparedStatement pstmt = sqlStore.conn.prepareStatement("SELECT id, name, wx, wy, x, y, z, worldversion, data, isDead FROM localPlayers")) {
                ResultSet rsx = pstmt.executeQuery();

                while (rsx.next()) {
                    int sqlId = rsx.getInt(1);
                    String name = rsx.getString(2);
                    int wx = rsx.getInt(3);
                    int wy = rsx.getInt(4);
                    float x = rsx.getFloat(5);
                    float y = rsx.getFloat(6);
                    float z = rsx.getFloat(7);
                    int worldVersion = rsx.getInt(8);
                    byte[] buf = rsx.getBytes(9);
                    boolean isDead = rsx.getBoolean(10);
                    consumer.accept(sqlId, name, wx, wy, x, y, z, worldVersion, buf, isDead);
                }
            } catch (Exception var20) {
                ExceptionLogger.logException(var20);
            }

            try {
                Statement stmt = sqlStore.conn.createStatement();
                stmt.executeUpdate("DROP TABLE localPlayers");
                stmt.executeUpdate("DROP TABLE networkPlayers");
                sqlStore.conn.commit();
            } catch (Exception var17) {
                ExceptionLogger.logException(var17);
            }
        }
    }

    public interface IImportPlayerFromOldDB {
        void accept(int var1, String var2, int var3, int var4, float var5, float var6, float var7, int var8, byte[] var9, boolean var10);
    }

    private abstract static class IVehicleStore {
        abstract void init(TIntHashSet var1, TIntHashSet var2);

        abstract void Reset();

        abstract void loadChunk(IsoChunk var1, VehiclesDB2.ThrowingBiConsumer<IsoChunk, VehiclesDB2.VehicleBuffer, IOException> var2) throws IOException;

        abstract void loadChunk(int var1, int var2, VehiclesDB2.ThrowingConsumer<VehiclesDB2.VehicleBuffer, IOException> var3) throws IOException;

        abstract void updateVehicle(VehiclesDB2.VehicleBuffer var1);

        abstract void removeVehicle(int var1);
    }

    private static final class MainThread {
        final TIntHashSet seenChunks = new TIntHashSet();
        final TIntHashSet usedIds = new TIntHashSet();
        final TIntHashSet loadedIds = new TIntHashSet();
        boolean forceSave;
        final ConcurrentLinkedQueue<VehiclesDB2.QueueItem> queue = new ConcurrentLinkedQueue<>();

        MainThread() {
            this.seenChunks.setAutoCompactionFactor(0.0F);
            this.usedIds.setAutoCompactionFactor(0.0F);
            this.loadedIds.setAutoCompactionFactor(0.0F);
        }

        void Reset() {
            this.seenChunks.clear();
            this.usedIds.clear();
            this.loadedIds.clear();

            assert this.queue.isEmpty();

            this.queue.clear();
            this.forceSave = false;
        }

        void update() throws IOException {
            if (!GameClient.client && !GameServer.server && this.forceSave) {
                this.forceSave = false;

                for (int i = 0; i < 4; i++) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player != null && player.getVehicle() != null && player.getVehicle().isEngineRunning()) {
                        this.updateVehicle(player.getVehicle());
                        BaseVehicle trailer = player.getVehicle().getVehicleTowing();
                        if (trailer != null) {
                            this.updateVehicle(trailer);
                        }
                    }
                }
            }

            for (VehiclesDB2.QueueItem item = this.queue.poll(); item != null; item = this.queue.poll()) {
                try {
                    item.processMain();
                } finally {
                    item.release();
                }
            }
        }

        void setChunkSeen(int wx, int wy) {
            int key = wy << 16 | wx;
            this.seenChunks.add(key);
        }

        boolean isChunkSeen(int wx, int wy) {
            int key = wy << 16 | wx;
            return this.seenChunks.contains(key);
        }

        int allocateID() {
            synchronized (this.usedIds) {
                for (int i = 1; i < Integer.MAX_VALUE; i++) {
                    if (!this.usedIds.contains(i)) {
                        this.usedIds.add(i);
                        return i;
                    }
                }
            }

            throw new RuntimeException("ran out of unused vehicle ids");
        }

        void setVehicleLoaded(BaseVehicle vehicle) {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }

            assert !this.loadedIds.contains(vehicle.sqlId);

            this.loadedIds.add(vehicle.sqlId);
        }

        void setVehicleUnloaded(BaseVehicle vehicle) {
            if (vehicle.sqlId != -1) {
                this.loadedIds.remove(vehicle.sqlId);
            }
        }

        void addVehicle(BaseVehicle vehicle) throws IOException {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }

            VehiclesDB2.QueueAddVehicle item = VehiclesDB2.QueueAddVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void removeVehicle(BaseVehicle vehicle) {
            VehiclesDB2.QueueRemoveVehicle item = VehiclesDB2.QueueRemoveVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void updateVehicle(BaseVehicle vehicle) throws IOException {
            if (vehicle.sqlId == -1) {
                vehicle.sqlId = this.allocateID();
            }

            VehiclesDB2.QueueUpdateVehicle item = VehiclesDB2.QueueUpdateVehicle.s_pool.alloc();
            item.init(vehicle);
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }

        void loadChunk(IsoChunk chunk) {
            VehiclesDB2.QueueLoadChunk item = VehiclesDB2.QueueLoadChunk.s_pool.alloc();
            item.init(chunk.wx, chunk.wy);
            chunk.loadVehiclesObject = item;
            VehiclesDB2.instance.worldStreamer.queue.add(item);
        }
    }

    private static class MemoryStore extends VehiclesDB2.IVehicleStore {
        final TIntObjectHashMap<VehiclesDB2.VehicleBuffer> idToVehicle = new TIntObjectHashMap<>();
        final TIntObjectHashMap<ArrayList<VehiclesDB2.VehicleBuffer>> chunkToVehicles = new TIntObjectHashMap<>();

        @Override
        void init(TIntHashSet usedIDs, TIntHashSet seenChunks) {
            usedIDs.clear();
            seenChunks.clear();
        }

        @Override
        void Reset() {
            this.idToVehicle.clear();
            this.chunkToVehicles.clear();
        }

        @Override
        void loadChunk(IsoChunk chunk, VehiclesDB2.ThrowingBiConsumer<IsoChunk, VehiclesDB2.VehicleBuffer, IOException> consumer) throws IOException {
            int key = chunk.wy << 16 | chunk.wx;
            ArrayList<VehiclesDB2.VehicleBuffer> vehicles = this.chunkToVehicles.get(key);
            if (vehicles != null) {
                for (int i = 0; i < vehicles.size(); i++) {
                    VehiclesDB2.VehicleBuffer vehicleBuffer = vehicles.get(i);
                    vehicleBuffer.bb.rewind();
                    boolean serialize = vehicleBuffer.bb.get() == 1;
                    int hashCode = vehicleBuffer.bb.getInt();
                    consumer.accept(chunk, vehicleBuffer);
                }
            }
        }

        @Override
        void loadChunk(int wx, int wy, VehiclesDB2.ThrowingConsumer<VehiclesDB2.VehicleBuffer, IOException> consumer) throws IOException {
            int key = wy << 16 | wx;
            ArrayList<VehiclesDB2.VehicleBuffer> vehicles = this.chunkToVehicles.get(key);
            if (vehicles != null) {
                for (int i = 0; i < vehicles.size(); i++) {
                    VehiclesDB2.VehicleBuffer vehicleBuffer = vehicles.get(i);
                    vehicleBuffer.bb.rewind();
                    boolean serialize = vehicleBuffer.bb.get() == 1;
                    int hashCode = vehicleBuffer.bb.getInt();
                    consumer.accept(vehicleBuffer);
                }
            }
        }

        @Override
        void updateVehicle(VehiclesDB2.VehicleBuffer vehicle) {
            assert vehicle.id >= 1;

            synchronized (VehiclesDB2.instance.main.usedIds) {
                assert VehiclesDB2.instance.main.usedIds.contains(vehicle.id);
            }

            vehicle.bb.rewind();
            VehiclesDB2.VehicleBuffer vehicle2 = this.idToVehicle.get(vehicle.id);
            if (vehicle2 == null) {
                vehicle2 = new VehiclesDB2.VehicleBuffer();
                vehicle2.id = vehicle.id;
                this.idToVehicle.put(vehicle.id, vehicle2);
            } else {
                int key = vehicle2.wy << 16 | vehicle2.wx;
                this.chunkToVehicles.get(key).remove(vehicle2);
            }

            vehicle2.wx = vehicle.wx;
            vehicle2.wy = vehicle.wy;
            vehicle2.x = vehicle.x;
            vehicle2.y = vehicle.y;
            vehicle2.worldVersion = vehicle.worldVersion;
            vehicle2.setBytes(vehicle.bb);
            int key = vehicle2.wy << 16 | vehicle2.wx;
            if (this.chunkToVehicles.get(key) == null) {
                this.chunkToVehicles.put(key, new ArrayList<>());
            }

            this.chunkToVehicles.get(key).add(vehicle2);
        }

        @Override
        void removeVehicle(int id) {
            VehiclesDB2.VehicleBuffer vehicle = this.idToVehicle.remove(id);
            if (vehicle != null) {
                int key = vehicle.wy << 16 | vehicle.wx;
                this.chunkToVehicles.get(key).remove(vehicle);
            }
        }
    }

    private static final class QueueAddVehicle extends VehiclesDB2.QueueItem {
        static final Pool<VehiclesDB2.QueueAddVehicle> s_pool = new Pool<>(VehiclesDB2.QueueAddVehicle::new);
        final VehiclesDB2.VehicleBuffer vehicleBuffer = new VehiclesDB2.VehicleBuffer();

        void init(BaseVehicle vehicle) throws IOException {
            this.vehicleBuffer.set(vehicle);
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.updateVehicle(this.vehicleBuffer);
        }
    }

    private abstract static class QueueItem extends PooledObject {
        abstract void processMain();

        abstract void processWorldStreamer();
    }

    private static class QueueLoadChunk extends VehiclesDB2.QueueItem {
        static final Pool<VehiclesDB2.QueueLoadChunk> s_pool = new Pool<>(VehiclesDB2.QueueLoadChunk::new);
        int wx;
        int wy;
        final ArrayList<BaseVehicle> vehicles = new ArrayList<>();
        IsoGridSquare dummySquare;

        void init(int wx, int wy) {
            this.wx = wx;
            this.wy = wy;
            this.vehicles.clear();
            if (this.dummySquare == null) {
                this.dummySquare = IsoGridSquare.getNew(IsoWorld.instance.currentCell, null, 0, 0, 0);
            }
        }

        @Override
        void processMain() {
            IsoChunk chunk = ServerMap.instance.getChunk(this.wx, this.wy);
            if (chunk == null) {
                this.vehicles.clear();
            } else if (chunk.loadVehiclesObject != this) {
                this.vehicles.clear();
            } else {
                chunk.loadVehiclesObject = null;
                int CPW = 8;

                for (int i = 0; i < this.vehicles.size(); i++) {
                    BaseVehicle vehicle = this.vehicles.get(i);
                    IsoGridSquare sq = chunk.getGridSquare(PZMath.fastfloor(vehicle.getX() - chunk.wx * 8), PZMath.fastfloor(vehicle.getY() - chunk.wy * 8), 0);
                    vehicle.setSquare(sq);
                    vehicle.setCurrent(sq);
                    vehicle.chunk = chunk;
                    if (chunk.jobType == IsoChunk.JobType.SoftReset) {
                        vehicle.softReset();
                    }

                    if (!vehicle.addedToWorld && VehiclesDB2.instance.isVehicleLoaded(vehicle)) {
                        vehicle.removeFromSquare();
                        this.vehicles.remove(i);
                        i--;
                    } else {
                        chunk.vehicles.add(vehicle);
                        if (!vehicle.addedToWorld) {
                            vehicle.addToWorld();
                        }
                    }
                }

                this.vehicles.clear();
            }
        }

        @Override
        void processWorldStreamer() {
            try {
                VehiclesDB2.instance.worldStreamer.store.loadChunk(this.wx, this.wy, this::vehicleLoaded);
            } catch (Exception var2) {
                ExceptionLogger.logException(var2);
            }
        }

        void vehicleLoaded(VehiclesDB2.VehicleBuffer vehicleBuffer) throws IOException {
            assert vehicleBuffer.id >= 1;

            int CPW = 8;
            int squareX = PZMath.fastfloor(vehicleBuffer.x - this.wx * 8);
            int squareY = PZMath.fastfloor(vehicleBuffer.y - this.wy * 8);
            this.dummySquare.x = squareX;
            this.dummySquare.y = squareY;
            IsoGridSquare sq = this.dummySquare;
            BaseVehicle vehicle = new BaseVehicle(IsoWorld.instance.currentCell);
            vehicle.setSquare(sq);
            vehicle.setCurrent(sq);

            try {
                vehicle.load(vehicleBuffer.bb, vehicleBuffer.worldVersion);
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
                DebugLog.General.error("vehicle %d is being deleted because an error occurred loading it", vehicleBuffer.id);
                VehiclesDB2.instance.worldStreamer.store.removeVehicle(vehicleBuffer.id);
                return;
            }

            vehicle.sqlId = vehicleBuffer.id;
            this.vehicles.add(vehicle);
        }
    }

    private static class QueueRemoveVehicle extends VehiclesDB2.QueueItem {
        static final Pool<VehiclesDB2.QueueRemoveVehicle> s_pool = new Pool<>(VehiclesDB2.QueueRemoveVehicle::new);
        int id;

        void init(BaseVehicle vehicle) {
            this.id = vehicle.sqlId;
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.removeVehicle(this.id);
        }
    }

    private static final class QueueUpdateVehicle extends VehiclesDB2.QueueItem {
        static final Pool<VehiclesDB2.QueueUpdateVehicle> s_pool = new Pool<>(VehiclesDB2.QueueUpdateVehicle::new);
        final VehiclesDB2.VehicleBuffer vehicleBuffer = new VehiclesDB2.VehicleBuffer();

        void init(BaseVehicle vehicle) throws IOException {
            this.vehicleBuffer.set(vehicle);
        }

        @Override
        void processMain() {
        }

        @Override
        void processWorldStreamer() {
            VehiclesDB2.instance.worldStreamer.store.updateVehicle(this.vehicleBuffer);
        }
    }

    private static final class SQLStore extends VehiclesDB2.IVehicleStore {
        Connection conn;
        final VehiclesDB2.VehicleBuffer vehicleBuffer = new VehiclesDB2.VehicleBuffer();

        @Override
        void init(TIntHashSet usedIDs, TIntHashSet seenChunks) {
            usedIDs.clear();
            seenChunks.clear();
            if (!Core.getInstance().isNoSave()) {
                this.create();

                try {
                    this.initUsedIDs(usedIDs, seenChunks);
                } catch (SQLException var4) {
                    ExceptionLogger.logException(var4);
                }
            }
        }

        @Override
        void Reset() {
            if (this.conn != null) {
                try {
                    this.conn.close();
                } catch (SQLException var2) {
                    ExceptionLogger.logException(var2);
                }

                this.conn = null;
            }
        }

        @Override
        void loadChunk(IsoChunk chunk, VehiclesDB2.ThrowingBiConsumer<IsoChunk, VehiclesDB2.VehicleBuffer, IOException> consumer) throws IOException {
            if (this.conn != null && chunk != null) {
                String sql = "SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?")) {
                    pstmt.setInt(1, chunk.wx);
                    pstmt.setInt(2, chunk.wy);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        this.vehicleBuffer.id = rs.getInt(1);
                        this.vehicleBuffer.wx = chunk.wx;
                        this.vehicleBuffer.wy = chunk.wy;
                        this.vehicleBuffer.x = rs.getFloat(2);
                        this.vehicleBuffer.y = rs.getFloat(3);
                        InputStream input = rs.getBinaryStream(4);
                        this.vehicleBuffer.setBytes(input);
                        this.vehicleBuffer.worldVersion = rs.getInt(5);
                        boolean serialise = this.vehicleBuffer.bb.get() != 0;
                        byte classID = this.vehicleBuffer.bb.get();
                        if (classID == IsoObject.getFactoryVehicle().getClassID() && serialise) {
                            consumer.accept(chunk, this.vehicleBuffer);
                        }
                    }
                } catch (Exception var11) {
                    ExceptionLogger.logException(var11);
                }
            }
        }

        @Override
        void loadChunk(int wx, int wy, VehiclesDB2.ThrowingConsumer<VehiclesDB2.VehicleBuffer, IOException> consumer) throws IOException {
            if (this.conn != null) {
                String sql = "SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT id, x, y, data, worldversion FROM vehicles WHERE wx=? AND wy=?")) {
                    pstmt.setInt(1, wx);
                    pstmt.setInt(2, wy);
                    ResultSet rs = pstmt.executeQuery();

                    while (rs.next()) {
                        this.vehicleBuffer.id = rs.getInt(1);
                        this.vehicleBuffer.wx = wx;
                        this.vehicleBuffer.wy = wy;
                        this.vehicleBuffer.x = rs.getFloat(2);
                        this.vehicleBuffer.y = rs.getFloat(3);
                        InputStream input = rs.getBinaryStream(4);
                        this.vehicleBuffer.setBytes(input);
                        this.vehicleBuffer.worldVersion = rs.getInt(5);
                        boolean serialise = this.vehicleBuffer.bb.get() != 0;
                        byte classID = this.vehicleBuffer.bb.get();
                        if (classID == IsoObject.getFactoryVehicle().getClassID() && serialise) {
                            consumer.accept(this.vehicleBuffer);
                        }
                    }
                } catch (Exception var12) {
                    ExceptionLogger.logException(var12);
                }
            }
        }

        @Override
        void updateVehicle(VehiclesDB2.VehicleBuffer vehicleBuffer) {
            if (this.conn != null) {
                assert vehicleBuffer.id >= 1;

                synchronized (VehiclesDB2.instance.main.usedIds) {
                    assert VehiclesDB2.instance.main.usedIds.contains(vehicleBuffer.id);
                }

                try {
                    if (this.isInDB(vehicleBuffer.id)) {
                        this.updateDB(vehicleBuffer);
                    } else {
                        this.addToDB(vehicleBuffer);
                    }
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                    this.rollback();
                }
            }
        }

        boolean isInDB(int id) throws SQLException {
            String sql = "SELECT 1 FROM vehicles WHERE id=?";

            boolean var5;
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT 1 FROM vehicles WHERE id=?")) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                var5 = rs.next();
            }

            return var5;
        }

        void addToDB(VehiclesDB2.VehicleBuffer vehicleBuffer) throws SQLException {
            String sql = "INSERT INTO vehicles(wx,wy,x,y,worldversion,data,id) VALUES(?,?,?,?,?,?,?)";

            try {
                try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO vehicles(wx,wy,x,y,worldversion,data,id) VALUES(?,?,?,?,?,?,?)")) {
                    pstmt.setInt(1, vehicleBuffer.wx);
                    pstmt.setInt(2, vehicleBuffer.wy);
                    pstmt.setFloat(3, vehicleBuffer.x);
                    pstmt.setFloat(4, vehicleBuffer.y);
                    pstmt.setInt(5, vehicleBuffer.worldVersion);
                    ByteBuffer bb = vehicleBuffer.bb;
                    bb.rewind();
                    pstmt.setBinaryStream(6, new ByteBufferBackedInputStream(bb), bb.remaining());
                    pstmt.setInt(7, vehicleBuffer.id);
                    int rowAffected = pstmt.executeUpdate();
                    this.conn.commit();
                }
            } catch (Exception var8) {
                this.rollback();
                throw var8;
            }
        }

        void updateDB(VehiclesDB2.VehicleBuffer vehicleBuffer) throws SQLException {
            String sql_update = "UPDATE vehicles SET wx = ?, wy = ?, x = ?, y = ?, worldversion = ?, data = ? WHERE id=?";

            try {
                try (PreparedStatement pstmt = this.conn
                        .prepareStatement("UPDATE vehicles SET wx = ?, wy = ?, x = ?, y = ?, worldversion = ?, data = ? WHERE id=?")) {
                    pstmt.setInt(1, vehicleBuffer.wx);
                    pstmt.setInt(2, vehicleBuffer.wy);
                    pstmt.setFloat(3, vehicleBuffer.x);
                    pstmt.setFloat(4, vehicleBuffer.y);
                    pstmt.setInt(5, vehicleBuffer.worldVersion);
                    ByteBuffer bb = vehicleBuffer.bb;
                    bb.rewind();
                    pstmt.setBinaryStream(6, new ByteBufferBackedInputStream(bb), bb.remaining());
                    pstmt.setInt(7, vehicleBuffer.id);
                    int rowAffected = pstmt.executeUpdate();
                    this.conn.commit();
                }
            } catch (Exception var8) {
                this.rollback();
                throw var8;
            }
        }

        @Override
        void removeVehicle(int sqlID) {
            if (this.conn != null && sqlID >= 1) {
                String sql = "DELETE FROM vehicles WHERE id=?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM vehicles WHERE id=?")) {
                    pstmt.setInt(1, sqlID);
                    int rowAffected = pstmt.executeUpdate();
                    this.conn.commit();
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                    this.rollback();
                }
            }
        }

        void create() {
            String filename = ZomboidFileSystem.instance.getCurrentSaveDir();
            File dbDir = new File(filename);
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            File dbFile = new File(filename + File.separator + "vehicles.db");
            dbFile.setReadable(true, false);
            dbFile.setExecutable(true, false);
            dbFile.setWritable(true, false);
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                    Statement stat = this.conn.createStatement();
                    stat.executeUpdate(
                        "CREATE TABLE vehicles (id   INTEGER PRIMARY KEY NOT NULL,wx    INTEGER,wy    INTEGER,x    FLOAT,y    FLOAT,worldversion    INTEGER,data BLOB);"
                    );
                    stat.executeUpdate("CREATE INDEX ivwx ON vehicles (wx);");
                    stat.executeUpdate("CREATE INDEX ivwy ON vehicles (wy);");
                    stat.close();
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                    DebugLog.log("failed to create vehicles database");
                    System.exit(1);
                }
            }

            if (this.conn == null) {
                try {
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                } catch (Exception var7) {
                    DebugLog.log("failed to create vehicles database");
                    ExceptionLogger.logException(var7);
                    System.exit(1);
                }
            }

            try {
                Statement stat = this.conn.createStatement();
                stat.executeQuery("PRAGMA JOURNAL_MODE=TRUNCATE;");
                stat.close();
            } catch (Exception var6) {
                ExceptionLogger.logException(var6);
                System.exit(1);
            }

            try {
                this.conn.setAutoCommit(false);
            } catch (SQLException var5) {
                ExceptionLogger.logException(var5);
            }
        }

        private String searchPathForSqliteLib(String library) {
            for (String path : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
                File file = new File(path, library);
                if (file.exists()) {
                    return path;
                }
            }

            return "";
        }

        void initUsedIDs(TIntHashSet usedIDs, TIntHashSet seenChunks) throws SQLException {
            String sql = "SELECT wx,wy,id FROM vehicles";

            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT wx,wy,id FROM vehicles")) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    int wx = rs.getInt(1);
                    int wy = rs.getInt(2);
                    seenChunks.add(wy << 16 | wx);
                    usedIDs.add(rs.getInt(3));
                }
            }
        }

        private void rollback() {
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException var2) {
                    ExceptionLogger.logException(var2);
                }
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T1, T2, E extends Exception> {
        void accept(T1 var1, T2 var2) throws E;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T1, E extends Exception> {
        void accept(T1 var1) throws E;
    }

    private static final class VehicleBuffer {
        int id = -1;
        int wx;
        int wy;
        float x;
        float y;
        int worldVersion;
        ByteBuffer bb = ByteBuffer.allocate(32768);

        void set(BaseVehicle vehicle) throws IOException {
            assert vehicle.sqlId >= 1;

            synchronized (VehiclesDB2.instance.main.usedIds) {
                assert VehiclesDB2.instance.main.usedIds.contains(vehicle.sqlId);
            }

            this.id = vehicle.sqlId;
            this.wx = vehicle.chunk.wx;
            this.wy = vehicle.chunk.wy;
            this.x = vehicle.getX();
            this.y = vehicle.getY();
            this.worldVersion = IsoWorld.getWorldVersion();
            ByteBuffer bb = VehiclesDB2.TL_SliceBuffer.get();
            bb.clear();

            while (true) {
                try {
                    vehicle.save(bb);
                    break;
                } catch (BufferOverflowException var4) {
                    if (bb.capacity() >= 2097152) {
                        DebugLog.General.error("the vehicle %d cannot be saved", vehicle.sqlId);
                        throw var4;
                    }

                    bb = ByteBuffer.allocate(bb.capacity() + 32768);
                    VehiclesDB2.TL_SliceBuffer.set(bb);
                }
            }

            bb.flip();
            this.setBytes(bb);
        }

        void setBytes(ByteBuffer bytes) {
            bytes.rewind();
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            byte[] temp = VehiclesDB2.TL_Bytes.get();
            int total = bytes.limit();

            while (total > 0) {
                int count = Math.min(temp.length, total);
                bytes.get(temp, 0, count);
                bbos.write(temp, 0, count);
                total -= count;
            }

            bbos.flip();
            this.bb = bbos.getWrappedBuffer();
        }

        void setBytes(byte[] bytes) {
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            bbos.write(bytes);
            bbos.flip();
            this.bb = bbos.getWrappedBuffer();
        }

        void setBytes(InputStream input) throws IOException {
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.bb, true);
            bbos.clear();
            byte[] temp = VehiclesDB2.TL_Bytes.get();

            while (true) {
                int count = input.read(temp);
                if (count < 1) {
                    bbos.flip();
                    this.bb = bbos.getWrappedBuffer();
                    return;
                }

                bbos.write(temp, 0, count);
            }
        }
    }

    private static final class WorldStreamerThread {
        final VehiclesDB2.IVehicleStore store = new VehiclesDB2.SQLStore();
        final ConcurrentLinkedQueue<VehiclesDB2.QueueItem> queue = new ConcurrentLinkedQueue<>();
        final VehiclesDB2.VehicleBuffer vehicleBuffer = new VehiclesDB2.VehicleBuffer();

        void Reset() {
            this.store.Reset();

            assert this.queue.isEmpty();

            this.queue.clear();
        }

        void update() {
            for (VehiclesDB2.QueueItem item = this.queue.poll(); item != null; item = this.queue.poll()) {
                try {
                    item.processWorldStreamer();
                } finally {
                    VehiclesDB2.instance.main.queue.add(item);
                }
            }
        }

        void loadChunk(IsoChunk chunk) throws IOException {
            this.store.loadChunk(chunk, this::vehicleLoaded);
        }

        void vehicleLoaded(IsoChunk chunk, VehiclesDB2.VehicleBuffer vehicleBuffer) throws IOException {
            assert vehicleBuffer.id >= 1;

            int CPW = 8;
            IsoGridSquare sq = chunk.getGridSquare((int)(vehicleBuffer.x - chunk.wx * 8), (int)(vehicleBuffer.y - chunk.wy * 8), 0);
            BaseVehicle vehicle = new BaseVehicle(IsoWorld.instance.currentCell);
            vehicle.setSquare(sq);
            vehicle.setCurrent(sq);

            try {
                vehicle.load(vehicleBuffer.bb, vehicleBuffer.worldVersion);
            } catch (Exception var7) {
                ExceptionLogger.logException(var7);
                DebugLog.General.error("vehicle %d is being deleted because an error occurred loading it", vehicleBuffer.id);
                this.store.removeVehicle(vehicleBuffer.id);
                return;
            }

            vehicle.sqlId = vehicleBuffer.id;
            vehicle.chunk = chunk;
            if (chunk.jobType == IsoChunk.JobType.SoftReset) {
                vehicle.softReset();
            }

            chunk.vehicles.add(vehicle);
        }

        void unloadChunk(IsoChunk chunk) {
            for (int i = 0; i < chunk.vehicles.size(); i++) {
                try {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    this.vehicleBuffer.set(vehicle);
                    this.store.updateVehicle(this.vehicleBuffer);
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                }
            }
        }
    }
}
