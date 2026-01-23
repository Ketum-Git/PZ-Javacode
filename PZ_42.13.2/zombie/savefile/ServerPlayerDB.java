// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;

public final class ServerPlayerDB {
    private static ServerPlayerDB instance;
    private static boolean allow;
    public Connection conn;
    private ConcurrentLinkedQueue<ServerPlayerDB.NetworkCharacterData> charactersToSave;

    public static void setAllow(boolean en) {
        allow = en;
    }

    public static boolean isAllow() {
        return allow;
    }

    public static synchronized ServerPlayerDB getInstance() {
        if (instance == null && allow) {
            instance = new ServerPlayerDB();
        }

        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public ServerPlayerDB() {
        if (!Core.getInstance().isNoSave()) {
            this.create();
        }
    }

    public void close() {
        instance = null;
        allow = false;
    }

    private void create() {
        this.conn = PlayerDBHelper.create();
        this.charactersToSave = new ConcurrentLinkedQueue<>();
        DatabaseMetaData md = null;

        try {
            md = this.conn.getMetaData();
            Statement stat = this.conn.createStatement();
            ResultSet rs = md.getColumns(null, null, "networkPlayers", "steamid");
            if (!rs.next()) {
                stat.executeUpdate("ALTER TABLE 'networkPlayers' ADD 'steamid' STRING NULL");
            }

            rs.close();
            stat.close();
        } catch (SQLException var4) {
            var4.printStackTrace();
        }
    }

    public void process() {
        if (!this.charactersToSave.isEmpty()) {
            for (ServerPlayerDB.NetworkCharacterData characterData = this.charactersToSave.poll();
                characterData != null;
                characterData = this.charactersToSave.poll()
            ) {
                this.serverUpdateNetworkCharacterInt(characterData);
            }
        }
    }

    @Deprecated
    public void serverUpdateNetworkCharacter(ByteBuffer bb, UdpConnection connection) {
        this.charactersToSave.add(new ServerPlayerDB.NetworkCharacterData(bb, connection));
    }

    public void save() {
        for (UdpConnection connection : GameServer.udpEngine.connections) {
            for (IsoPlayer player : connection.players) {
                if (player != null) {
                    this.serverUpdateNetworkCharacter(player, player.getIndex(), connection);
                }
            }
        }

        while (!this.charactersToSave.isEmpty()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException var7) {
                throw new RuntimeException(var7);
            }
        }

        DebugLog.log("Saving players");
    }

    public void serverUpdateNetworkCharacter(IsoPlayer player, int playerIndex, UdpConnection connection) {
        this.charactersToSave.add(new ServerPlayerDB.NetworkCharacterData(player, playerIndex, connection));
    }

    private void serverUpdateNetworkCharacterInt(ServerPlayerDB.NetworkCharacterData data) {
        if (data.playerIndex >= 0 && data.playerIndex < 4) {
            if (this.conn != null) {
                String sqlSelect;
                if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                    sqlSelect = "SELECT id FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
                } else {
                    sqlSelect = "SELECT id FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
                }

                String sqlInsert = "INSERT INTO networkPlayers(world,username,steamid, playerIndex,name,x,y,z,worldversion,isDead,data) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
                String sqlUpdate = "UPDATE networkPlayers SET x=?, y=?, z=?, worldversion = ?, isDead = ?, data = ?, name = ? WHERE id=?";

                try {
                    try (PreparedStatement pstmt = this.conn.prepareStatement(sqlSelect)) {
                        if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                            pstmt.setString(1, data.steamid);
                        } else {
                            pstmt.setString(1, data.username);
                        }

                        pstmt.setString(2, Core.gameSaveWorld);
                        pstmt.setInt(3, data.playerIndex);
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()) {
                            int sqlId = rs.getInt(1);

                            try (PreparedStatement pstmtUpdate = this.conn
                                    .prepareStatement("UPDATE networkPlayers SET x=?, y=?, z=?, worldversion = ?, isDead = ?, data = ?, name = ? WHERE id=?")) {
                                pstmtUpdate.setFloat(1, data.x);
                                pstmtUpdate.setFloat(2, data.y);
                                pstmtUpdate.setFloat(3, data.z);
                                pstmtUpdate.setInt(4, data.worldVersion);
                                pstmtUpdate.setBoolean(5, data.isDead);
                                pstmtUpdate.setBytes(6, data.buffer);
                                pstmtUpdate.setString(7, data.playerName);
                                pstmtUpdate.setInt(8, sqlId);
                                int rowAffected = pstmtUpdate.executeUpdate();
                                this.conn.commit();
                                return;
                            }
                        }
                    }

                    try (PreparedStatement pstmtInsert = this.conn
                            .prepareStatement(
                                "INSERT INTO networkPlayers(world,username,steamid, playerIndex,name,x,y,z,worldversion,isDead,data) VALUES(?,?,?,?,?,?,?,?,?,?,?)"
                            )) {
                        pstmtInsert.setString(1, Core.gameSaveWorld);
                        pstmtInsert.setString(2, data.username);
                        pstmtInsert.setString(3, data.steamid);
                        pstmtInsert.setInt(4, data.playerIndex);
                        pstmtInsert.setString(5, data.playerName);
                        pstmtInsert.setFloat(6, data.x);
                        pstmtInsert.setFloat(7, data.y);
                        pstmtInsert.setFloat(8, data.z);
                        pstmtInsert.setInt(9, data.worldVersion);
                        pstmtInsert.setBoolean(10, data.isDead);
                        pstmtInsert.setBytes(11, data.buffer);
                        int rowAffected = pstmtInsert.executeUpdate();
                        this.conn.commit();
                    }
                } catch (Exception var17) {
                    ExceptionLogger.logException(var17);
                    PlayerDBHelper.rollback(this.conn);
                }
            }
        }
    }

    public void serverConvertNetworkCharacter(String username, String steamIdStr) {
        try {
            String sqlUpdate = "UPDATE networkPlayers SET steamid=? WHERE username=? AND world=? AND (steamid is null or steamid = '')";

            try (PreparedStatement pstmt = this.conn
                    .prepareStatement("UPDATE networkPlayers SET steamid=? WHERE username=? AND world=? AND (steamid is null or steamid = '')")) {
                pstmt.setString(1, steamIdStr);
                pstmt.setString(2, username);
                pstmt.setString(3, Core.gameSaveWorld);
                int rowAffected = pstmt.executeUpdate();
                if (rowAffected > 0) {
                    DebugLog.DetailedInfo
                        .warn("serverConvertNetworkCharacter: The steamid was set for the '" + username + "' for " + rowAffected + " players. ");
                }

                this.conn.commit();
            }
        } catch (SQLException var9) {
            ExceptionLogger.logException(var9);
        }
    }

    public IsoPlayer serverLoadNetworkCharacter(int playerIndex, String idStr) {
        if (playerIndex < 0 || playerIndex >= 4) {
            return null;
        } else if (this.conn == null) {
            return null;
        } else {
            String sqlSelect;
            if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                sqlSelect = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
            } else {
                sqlSelect = "SELECT id, x, y, z, data, worldversion, isDead FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
            }

            try {
                Object sqlId;
                try (PreparedStatement pstmt = this.conn.prepareStatement(sqlSelect)) {
                    pstmt.setString(1, idStr);
                    pstmt.setString(2, Core.gameSaveWorld);
                    pstmt.setInt(3, playerIndex);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int sqlIdx = rs.getInt(1);
                        float x = rs.getFloat(2);
                        float y = rs.getFloat(3);
                        float z = rs.getFloat(4);
                        byte[] data = rs.getBytes(5);
                        int worldversion = rs.getInt(6);
                        boolean isDead = rs.getBoolean(7);

                        try {
                            ByteBuffer BufferForLoadPlayer = ByteBuffer.allocate(data.length);
                            BufferForLoadPlayer.rewind();
                            BufferForLoadPlayer.put(data);
                            BufferForLoadPlayer.rewind();
                            IsoPlayer player = new IsoPlayer(IsoWorld.instance.currentCell);
                            player.serverPlayerIndex = playerIndex;

                            try {
                                player.load(BufferForLoadPlayer, worldversion);
                            } catch (Exception var22) {
                                DebugLog.General.printException(var22, "The server cannot load player data.", LogSeverity.Error);
                                rs.close();
                                pstmt.close();
                                if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                                    sqlSelect = "DELETE FROM networkPlayers WHERE steamid=? AND world=? AND playerIndex=?";
                                } else {
                                    sqlSelect = "DELETE FROM networkPlayers WHERE username=? AND world=? AND playerIndex=?";
                                }

                                try (PreparedStatement pstmt2 = this.conn.prepareStatement(sqlSelect)) {
                                    pstmt2.setString(1, idStr);
                                    pstmt2.setString(2, Core.gameSaveWorld);
                                    pstmt2.setInt(3, playerIndex);
                                    pstmt2.executeUpdate();
                                    pstmt2.close();
                                }

                                return null;
                            }

                            if (isDead) {
                                player.getBodyDamage().setOverallBodyHealth(0.0F);
                                player.setHealth(0.0F);
                            }

                            player.remote = true;
                            return player;
                        } catch (Exception var23) {
                            ExceptionLogger.logException(var23);
                            return null;
                        }
                    }

                    sqlId = null;
                }

                return (IsoPlayer)sqlId;
            } catch (SQLException var25) {
                ExceptionLogger.logException(var25);
                return null;
            }
        }
    }

    private static final class NetworkCharacterData {
        byte[] buffer;
        String username;
        String steamid;
        int playerIndex;
        String playerName;
        float x;
        float y;
        float z;
        boolean isDead;
        int worldVersion;

        public NetworkCharacterData(IsoPlayer player, int playerIndex, UdpConnection connection) {
            this.playerIndex = playerIndex;
            this.playerName = player.getDescriptor().getForename() + " " + player.getDescriptor().getSurname();
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.isDead = player.isDead();
            this.worldVersion = IsoWorld.getWorldVersion();

            try {
                ByteBuffer SliceBuffer4NetworkPlayer = ByteBuffer.allocate(65536);
                SliceBuffer4NetworkPlayer.clear();
                player.save(SliceBuffer4NetworkPlayer);
                this.buffer = new byte[SliceBuffer4NetworkPlayer.position()];
                SliceBuffer4NetworkPlayer.rewind();
                SliceBuffer4NetworkPlayer.get(this.buffer);
            } catch (IOException var5) {
                var5.printStackTrace();
            }

            if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                this.steamid = connection.idStr;
            } else {
                this.steamid = "";
            }

            this.username = connection.username;
        }

        @Deprecated
        public NetworkCharacterData(ByteBuffer bb, UdpConnection connection) {
            this.playerIndex = bb.get();
            this.playerName = GameWindow.ReadStringUTF(bb);
            this.x = bb.getFloat();
            this.y = bb.getFloat();
            this.z = bb.getFloat();
            this.isDead = bb.get() == 1;
            this.worldVersion = bb.getInt();
            int size = bb.getInt();
            this.buffer = new byte[size];
            bb.get(this.buffer);
            if (GameServer.coop && SteamUtils.isSteamModeEnabled()) {
                this.steamid = connection.idStr;
            } else {
                this.steamid = "";
            }

            this.username = connection.username;
        }
    }
}
