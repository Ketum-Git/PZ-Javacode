// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import gnu.trove.set.hash.TIntHashSet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.iso.WorldStreamer;
import zombie.util.ByteBufferBackedInputStream;
import zombie.util.ByteBufferOutputStream;
import zombie.vehicles.VehiclesDB2;

public final class PlayerDB {
    public static final int INVALID_ID = -1;
    private static final int MIN_ID = 1;
    private static PlayerDB instance;
    private static final ThreadLocal<ByteBuffer> TL_SliceBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(32768));
    private static final ThreadLocal<byte[]> TL_Bytes = ThreadLocal.withInitial(() -> new byte[1024]);
    private static boolean allow;
    private final PlayerDB.IPlayerStore store = new PlayerDB.SQLPlayerStore();
    private final TIntHashSet usedIds = new TIntHashSet();
    private final ConcurrentLinkedQueue<PlayerDB.PlayerData> toThread = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PlayerDB.PlayerData> fromThread = new ConcurrentLinkedQueue<>();
    private boolean forceSavePlayers;
    public boolean canSavePlayers;
    private final UpdateLimit saveToDbPeriod = new UpdateLimit(10000L);

    public static synchronized PlayerDB getInstance() {
        if (instance == null && allow) {
            instance = new PlayerDB();
        }

        return instance;
    }

    public static void setAllow(boolean en) {
        allow = en;
    }

    public static boolean isAllow() {
        return allow;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public PlayerDB() {
        if (!Core.getInstance().isNoSave()) {
            this.create();
        }
    }

    private void create() {
        try {
            this.store.init(this.usedIds);
            this.usedIds.add(1);
        } catch (Exception var2) {
            ExceptionLogger.logException(var2);
        }
    }

    public void close() {
        assert WorldStreamer.instance.worldStreamer == null;

        this.updateWorldStreamer();

        assert this.toThread.isEmpty();

        try {
            this.store.Reset();
        } catch (Exception var2) {
            ExceptionLogger.logException(var2);
        }

        this.fromThread.clear();
        instance = null;
        allow = false;
    }

    private int allocateID() {
        synchronized (this.usedIds) {
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                if (!this.usedIds.contains(i)) {
                    this.usedIds.add(i);
                    return i;
                }
            }
        }

        throw new RuntimeException("ran out of unused players.db ids");
    }

    private PlayerDB.PlayerData allocPlayerData() {
        PlayerDB.PlayerData playerData = this.fromThread.poll();
        if (playerData == null) {
            playerData = new PlayerDB.PlayerData();
        }

        assert playerData.sqlId == -1;

        return playerData;
    }

    private void releasePlayerData(PlayerDB.PlayerData playerData) {
        playerData.sqlId = -1;
        this.fromThread.add(playerData);
    }

    public void updateMain() {
        if (this.canSavePlayers && (this.forceSavePlayers || this.saveToDbPeriod.Check())) {
            this.forceSavePlayers = false;
            this.savePlayersAsync();
            VehiclesDB2.instance.setForceSave();
        }
    }

    public void updateWorldStreamer() {
        for (PlayerDB.PlayerData playerData = this.toThread.poll(); playerData != null; playerData = this.toThread.poll()) {
            try {
                this.store.save(playerData);
            } catch (Exception var6) {
                ExceptionLogger.logException(var6);
            } finally {
                this.releasePlayerData(playerData);
            }
        }
    }

    private void savePlayerAsync(IsoPlayer player) throws Exception {
        if (player != null) {
            if (player.sqlId == -1) {
                player.sqlId = this.allocateID();
            }

            PlayerDB.PlayerData playerData = this.allocPlayerData();

            try {
                playerData.set(player);
                this.toThread.add(playerData);
            } catch (Exception var4) {
                this.releasePlayerData(playerData);
                throw var4;
            }
        }
    }

    private void savePlayersAsync() {
        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player != null) {
                try {
                    this.savePlayerAsync(player);
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                }
            }
        }
    }

    public void savePlayers() {
        if (this.canSavePlayers) {
            this.forceSavePlayers = true;
        }
    }

    public void saveLocalPlayersForce() {
        this.savePlayersAsync();
        if (WorldStreamer.instance.worldStreamer == null) {
            this.updateWorldStreamer();
        }
    }

    public void importPlayersFromVehiclesDB() {
        VehiclesDB2.instance.importPlayersFromOldDB((sqlId, name, wx, wy, x, y, z, WorldVersion, bytes, bDead) -> {
            PlayerDB.PlayerData playerData = this.allocPlayerData();
            playerData.sqlId = this.allocateID();
            playerData.x = x;
            playerData.y = y;
            playerData.z = z;
            playerData.isDead = bDead;
            playerData.name = name;
            playerData.worldVersion = WorldVersion;
            playerData.setBytes(bytes);

            try {
                this.store.save(playerData);
            } catch (Exception var13) {
                ExceptionLogger.logException(var13);
            }

            this.releasePlayerData(playerData);
        });
    }

    public void uploadLocalPlayers2DB() {
        this.savePlayersAsync();
        String saveDir = ZomboidFileSystem.instance.getCurrentSaveDir();

        for (int n = 1; n < 100; n++) {
            File file = new File(saveDir + File.separator + "map_p" + n + ".bin");
            if (file.exists()) {
                try {
                    IsoPlayer player = new IsoPlayer(IsoWorld.instance.currentCell);
                    player.load(file.getAbsolutePath());
                    this.savePlayerAsync(player);
                    file.delete();
                } catch (Exception var5) {
                    ExceptionLogger.logException(var5);
                }
            }
        }

        if (WorldStreamer.instance.worldStreamer == null) {
            this.updateWorldStreamer();
        }
    }

    private boolean loadPlayer(int sqlID, IsoPlayer player) {
        PlayerDB.PlayerData playerData = this.allocPlayerData();

        boolean ex;
        try {
            playerData.sqlId = sqlID;
            if (this.store.load(playerData)) {
                player.load(playerData.byteBuffer, playerData.worldVersion);
                if (playerData.isDead) {
                    player.getBodyDamage().setOverallBodyHealth(0.0F);
                    player.setHealth(0.0F);
                }

                player.sqlId = sqlID;
                return true;
            }

            ex = false;
        } catch (Exception var8) {
            ExceptionLogger.logException(var8);
            return false;
        } finally {
            this.releasePlayerData(playerData);
        }

        return ex;
    }

    public boolean loadLocalPlayer(int sqlId) {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                player = new IsoPlayer(IsoCell.getInstance());
                IsoPlayer.setInstance(player);
                IsoPlayer.players[0] = player;
            }

            if (this.loadPlayer(sqlId, player)) {
                int wx = PZMath.fastfloor(player.getX() / 8.0F);
                int wy = PZMath.fastfloor(player.getY() / 8.0F);
                IsoChunkMap chunkMap = IsoCell.getInstance().chunkMap[IsoPlayer.getPlayerIndex()];
                if ($assertionsDisabled || wx + IsoWorld.saveoffsetx * 32 == chunkMap.worldX && wy + IsoWorld.saveoffsety * 32 == chunkMap.worldY) {
                    chunkMap.worldX = wx + IsoWorld.saveoffsetx * 32;
                    chunkMap.worldY = wy + IsoWorld.saveoffsety * 32;
                    return true;
                }

                throw new AssertionError();
            }
        } catch (Exception var6) {
            ExceptionLogger.logException(var6);
        }

        return false;
    }

    public ArrayList<IsoPlayer> getAllLocalPlayers() {
        ArrayList<IsoPlayer> players = new ArrayList<>();
        this.usedIds.forEach(sqlID -> {
            if (sqlID <= 1) {
                return true;
            } else {
                IsoPlayer player = new IsoPlayer(IsoWorld.instance.currentCell);
                if (this.loadPlayer(sqlID, player)) {
                    players.add(player);
                }

                return true;
            }
        });
        return players;
    }

    public boolean loadLocalPlayerInfo(int sqlId) {
        PlayerDB.PlayerData playerData = this.allocPlayerData();

        boolean ex;
        try {
            playerData.sqlId = sqlId;
            if (!this.store.loadEverythingExceptBytes(playerData)) {
                return false;
            }

            IsoChunkMap.worldXa = PZMath.fastfloor(playerData.x);
            IsoChunkMap.worldYa = PZMath.fastfloor(playerData.y);
            IsoChunkMap.worldZa = PZMath.fastfloor(playerData.z);
            IsoChunkMap.worldXa = IsoChunkMap.worldXa + 256 * IsoWorld.saveoffsetx;
            IsoChunkMap.worldYa = IsoChunkMap.worldYa + 256 * IsoWorld.saveoffsety;
            IsoChunkMap.SWorldX[0] = PZMath.fastfloor(playerData.x / 8.0F);
            IsoChunkMap.SWorldY[0] = PZMath.fastfloor(playerData.y / 8.0F);
            IsoChunkMap.SWorldX[0] = IsoChunkMap.SWorldX[0] + 32 * IsoWorld.saveoffsetx;
            IsoChunkMap.SWorldY[0] = IsoChunkMap.SWorldY[0] + 32 * IsoWorld.saveoffsety;
            ex = true;
        } catch (Exception var7) {
            ExceptionLogger.logException(var7);
            return false;
        } finally {
            this.releasePlayerData(playerData);
        }

        return ex;
    }

    private interface IPlayerStore {
        void init(TIntHashSet var1) throws Exception;

        void Reset() throws Exception;

        void save(PlayerDB.PlayerData var1) throws Exception;

        boolean load(PlayerDB.PlayerData var1) throws Exception;

        boolean loadEverythingExceptBytes(PlayerDB.PlayerData var1) throws Exception;
    }

    private static final class PlayerData {
        int sqlId = -1;
        float x;
        float y;
        float z;
        boolean isDead;
        String name;
        int worldVersion;
        ByteBuffer byteBuffer = ByteBuffer.allocate(32768);

        PlayerDB.PlayerData set(IsoPlayer player) throws IOException {
            assert player.sqlId >= 1;

            this.sqlId = player.sqlId;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.isDead = player.isDead();
            this.name = player.getDescriptor().getForename() + " " + player.getDescriptor().getSurname();
            this.worldVersion = IsoWorld.getWorldVersion();
            ByteBuffer bb = PlayerDB.TL_SliceBuffer.get();
            bb.clear();

            while (true) {
                try {
                    player.save(bb);
                    break;
                } catch (BufferOverflowException var4) {
                    if (bb.capacity() >= 2097152) {
                        DebugLog.DetailedInfo.error("the player %s cannot be saved", player.getUsername());
                        throw var4;
                    }

                    bb = ByteBuffer.allocate(bb.capacity() + 32768);
                    PlayerDB.TL_SliceBuffer.set(bb);
                }
            }

            bb.flip();
            this.setBytes(bb);
            return this;
        }

        void setBytes(ByteBuffer bytes) {
            bytes.rewind();
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.byteBuffer, true);
            bbos.clear();
            byte[] temp = PlayerDB.TL_Bytes.get();
            int total = bytes.limit();

            while (total > 0) {
                int count = Math.min(temp.length, total);
                bytes.get(temp, 0, count);
                bbos.write(temp, 0, count);
                total -= count;
            }

            bbos.flip();
            this.byteBuffer = bbos.getWrappedBuffer();
        }

        void setBytes(byte[] bytes) {
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.byteBuffer, true);
            bbos.clear();
            bbos.write(bytes);
            bbos.flip();
            this.byteBuffer = bbos.getWrappedBuffer();
        }

        void setBytes(InputStream input) throws IOException {
            ByteBufferOutputStream bbos = new ByteBufferOutputStream(this.byteBuffer, true);
            bbos.clear();
            byte[] temp = PlayerDB.TL_Bytes.get();

            while (true) {
                int count = input.read(temp);
                if (count < 1) {
                    bbos.flip();
                    this.byteBuffer = bbos.getWrappedBuffer();
                    return;
                }

                bbos.write(temp, 0, count);
            }
        }
    }

    private static final class SQLPlayerStore implements PlayerDB.IPlayerStore {
        Connection conn;

        @Override
        public void init(TIntHashSet usedIDs) throws Exception {
            usedIDs.clear();
            if (!Core.getInstance().isNoSave()) {
                this.conn = PlayerDBHelper.create();
                this.initUsedIDs(usedIDs);
            }
        }

        @Override
        public void Reset() {
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
        public void save(PlayerDB.PlayerData playerData) throws Exception {
            assert playerData.sqlId >= 1;

            if (this.conn != null) {
                if (this.isInDB(playerData.sqlId)) {
                    this.update(playerData);
                } else {
                    this.add(playerData);
                }
            }
        }

        @Override
        public boolean load(PlayerDB.PlayerData playerData) throws Exception {
            assert playerData.sqlId >= 1;

            if (this.conn == null) {
                return false;
            } else {
                String sql = "SELECT data,worldversion,x,y,z,isDead,name FROM localPlayers WHERE id=?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT data,worldversion,x,y,z,isDead,name FROM localPlayers WHERE id=?")) {
                    pstmt.setInt(1, playerData.sqlId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        InputStream is = rs.getBinaryStream(1);
                        playerData.setBytes(is);
                        playerData.worldVersion = rs.getInt(2);
                        playerData.x = rs.getFloat(3);
                        playerData.y = rs.getFloat(4);
                        playerData.z = rs.getFloat(5);
                        playerData.isDead = rs.getBoolean(6);
                        playerData.name = rs.getString(7);
                        return true;
                    }
                }

                return false;
            }
        }

        @Override
        public boolean loadEverythingExceptBytes(PlayerDB.PlayerData playerData) throws Exception {
            if (this.conn == null) {
                return false;
            } else {
                String sql = "SELECT worldversion,x,y,z,isDead,name FROM localPlayers WHERE id=?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT worldversion,x,y,z,isDead,name FROM localPlayers WHERE id=?")) {
                    pstmt.setInt(1, playerData.sqlId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        playerData.worldVersion = rs.getInt(1);
                        playerData.x = rs.getFloat(2);
                        playerData.y = rs.getFloat(3);
                        playerData.z = rs.getFloat(4);
                        playerData.isDead = rs.getBoolean(5);
                        playerData.name = rs.getString(6);
                        return true;
                    }
                }

                return false;
            }
        }

        void initUsedIDs(TIntHashSet usedIDs) throws SQLException {
            String sql = "SELECT id FROM localPlayers";

            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT id FROM localPlayers")) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    usedIDs.add(rs.getInt(1));
                }
            }
        }

        boolean isInDB(int id) throws SQLException {
            String sql = "SELECT 1 FROM localPlayers WHERE id=?";

            boolean var5;
            try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT 1 FROM localPlayers WHERE id=?")) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                var5 = rs.next();
            }

            return var5;
        }

        void add(PlayerDB.PlayerData player) throws Exception {
            if (this.conn != null && player.sqlId >= 1) {
                String sql = "INSERT INTO localPlayers(wx,wy,x,y,z,worldversion,data,isDead,name,id) VALUES(?,?,?,?,?,?,?,?,?,?)";

                try {
                    try (PreparedStatement pstmt = this.conn
                            .prepareStatement("INSERT INTO localPlayers(wx,wy,x,y,z,worldversion,data,isDead,name,id) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                        pstmt.setInt(1, PZMath.fastfloor(player.x) / 8);
                        pstmt.setInt(2, PZMath.fastfloor(player.y) / 8);
                        pstmt.setFloat(3, player.x);
                        pstmt.setFloat(4, player.y);
                        pstmt.setFloat(5, player.z);
                        pstmt.setInt(6, player.worldVersion);
                        ByteBuffer bb = player.byteBuffer;
                        bb.rewind();
                        pstmt.setBinaryStream(7, new ByteBufferBackedInputStream(bb), bb.remaining());
                        pstmt.setBoolean(8, player.isDead);
                        pstmt.setString(9, player.name);
                        pstmt.setInt(10, player.sqlId);
                        int rowAffected = pstmt.executeUpdate();
                        this.conn.commit();
                    }
                } catch (Exception var8) {
                    PlayerDBHelper.rollback(this.conn);
                    throw var8;
                }
            }
        }

        public void update(PlayerDB.PlayerData player) throws Exception {
            if (this.conn != null && player.sqlId >= 1) {
                String sql_update = "UPDATE localPlayers SET wx = ?, wy = ?, x = ?, y = ?, z = ?, worldversion = ?, data = ?, isDead = ?, name = ? WHERE id=?";

                try {
                    try (PreparedStatement pstmt = this.conn
                            .prepareStatement(
                                "UPDATE localPlayers SET wx = ?, wy = ?, x = ?, y = ?, z = ?, worldversion = ?, data = ?, isDead = ?, name = ? WHERE id=?"
                            )) {
                        pstmt.setInt(1, PZMath.fastfloor(player.x / 8.0F));
                        pstmt.setInt(2, PZMath.fastfloor(player.y / 8.0F));
                        pstmt.setFloat(3, player.x);
                        pstmt.setFloat(4, player.y);
                        pstmt.setFloat(5, player.z);
                        pstmt.setInt(6, player.worldVersion);
                        ByteBuffer bb = player.byteBuffer;
                        bb.rewind();
                        pstmt.setBinaryStream(7, new ByteBufferBackedInputStream(bb), bb.remaining());
                        pstmt.setBoolean(8, player.isDead);
                        pstmt.setString(9, player.name);
                        pstmt.setInt(10, player.sqlId);
                        int rowAffected = pstmt.executeUpdate();
                        this.conn.commit();
                    }
                } catch (Exception var8) {
                    PlayerDBHelper.rollback(this.conn);
                    throw var8;
                }
            }
        }
    }
}
