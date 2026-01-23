// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.network.Account;
import zombie.network.Server;
import zombie.util.PZSQLUtils;

public class AccountDBHelper {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AccountDBHelper instance;
    Connection conn;
    private static LocalDateTime lastSave = LocalDateTime.now();

    public static synchronized AccountDBHelper getInstance() {
        if (instance == null) {
            instance = new AccountDBHelper();
        }

        return instance;
    }

    public Connection create() {
        if (this.conn != null) {
            return this.conn;
        } else {
            File dbDir = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }

            String filename = ZomboidFileSystem.instance.getCacheDir()
                + File.separator
                + "db"
                + File.separator
                + (SteamUtils.isSteamModeEnabled() ? "ServerListSteam.db" : "ServerList.db");
            File dbFile = new File(filename);
            dbFile.setReadable(true, false);
            dbFile.setExecutable(true, false);
            dbFile.setWritable(true, false);
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                    Statement stat = this.conn.createStatement();
                    stat.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS server (\n id INTEGER PRIMARY KEY AUTOINCREMENT,\n name TEXT NOT NULL,\n ip TEXT NOT NULL,\n port INTEGER NOT NULL,\n serverPassword TEXT,\n description TEXT,\n mods TEXT,\n icon BLOB,\n banner BLOB,\n panelBackground BLOB,\n screenBackground BLOB,\n lastOnline TEXT,\n lastDataUpdate TEXT\n);"
                    );
                    stat.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS account (\n id INTEGER PRIMARY KEY AUTOINCREMENT,\n serverId INTEGER NOT NULL,\n playerFirstAndLastName  TEXT,\n username TEXT NOT NULL,\n password TEXT,\n isSavePassword INTEGER DEFAULT 0,\n isUseSteamRelay INTEGER DEFAULT 0,\n authType INTEGER DEFAULT 1,\n icon BLOB,\n timePlayed INTEGER DEFAULT 0,\n lastLogon TEXT,\n FOREIGN KEY (serverId) REFERENCES server (id)\n);"
                    );
                    stat.close();
                } catch (Exception var9) {
                    ExceptionLogger.logException(var9);
                    DebugLog.log("failed to create accounts database");
                    System.exit(1);
                }
            }

            if (this.conn == null) {
                try {
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                    DebugLog.log("failed to create ServerList database");
                    System.exit(1);
                }
            }

            try {
                Statement stat = this.conn.createStatement();
                stat.executeQuery("PRAGMA JOURNAL_MODE=TRUNCATE;");
                stat.close();
            } catch (Exception var7) {
                ExceptionLogger.logException(var7);
                DebugLog.log("failed to config ServerList.db");
                System.exit(1);
            }

            try {
                this.conn.setAutoCommit(false);
            } catch (SQLException var6) {
                DebugLog.log("failed to setAutoCommit for ServerList.db");
            }

            String oldServerListFilename = LuaManager.getLuaCacheDir()
                + File.separator
                + (SteamUtils.isSteamModeEnabled() ? "ServerListSteam.txt" : "ServerList.txt");
            File oldDBFile = new File(oldServerListFilename);
            if (oldDBFile.exists()) {
                this.parseInputFile(oldDBFile.getAbsolutePath());
                oldDBFile.delete();
            }

            return this.conn;
        }
    }

    private void parseInputFile(String filename) {
        HashMap<String, String> data = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    if (parts[0].trim().equals("name") && !data.isEmpty()) {
                        int serverId = this.insertServer(data);
                        if (serverId > 0) {
                            this.insertAccount(data, serverId);
                            this.conn.commit();
                            System.out.println("Data loaded successfully!");
                        }

                        data.clear();
                    }

                    data.put(parts[0].trim(), parts[1].trim());
                }
            }

            if (!data.isEmpty()) {
                int serverId = this.insertServer(data);
                if (serverId > 0) {
                    this.insertAccount(data, serverId);
                    this.conn.commit();
                    System.out.println("Data loaded successfully!");
                }

                data.clear();
            }
        } catch (IOException var9) {
            System.err.println("Error reading input file: " + var9.getMessage());
        } catch (SQLException var10) {
            System.err.println("Database error: " + var10.getMessage());
        }
    }

    private int insertServer(HashMap<String, String> data) throws SQLException {
        String sql = "INSERT INTO server (name, ip, port, serverPassword, description) VALUES (?, ?, ?, ?, ?)";

        byte var12;
        try (PreparedStatement pstmt = this.conn.prepareStatement("INSERT INTO server (name, ip, port, serverPassword, description) VALUES (?, ?, ?, ?, ?)", 1)) {
            pstmt.setString(1, data.getOrDefault("name", ""));
            pstmt.setString(2, data.getOrDefault("ip", ""));
            pstmt.setInt(3, Integer.parseInt(data.getOrDefault("port", "0")));
            pstmt.setString(4, data.getOrDefault("serverpassword", ""));
            pstmt.setString(5, data.getOrDefault("description", ""));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

            var12 = -1;
        }

        return var12;
    }

    private void insertAccount(HashMap<String, String> data, int serverId) throws SQLException {
        String sql = "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String accountStr = data.getOrDefault("account", "");
        if (!accountStr.isEmpty()) {
            String[] accountData = accountStr.split(";;");

            try (PreparedStatement pstmt = this.conn
                    .prepareStatement(
                        "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    )) {
                pstmt.setInt(1, serverId);
                pstmt.setString(2, accountData[0]);
                pstmt.setString(3, accountData[1]);
                pstmt.setBoolean(4, Boolean.parseBoolean(accountData[2]));
                pstmt.setBoolean(5, Boolean.parseBoolean(accountData[3]));
                pstmt.setInt(6, Integer.parseInt(accountData[4]));
                pstmt.setInt(7, 0);
                pstmt.executeUpdate();
            }
        } else {
            try (PreparedStatement pstmt = this.conn
                    .prepareStatement(
                        "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    )) {
                pstmt.setInt(1, serverId);
                pstmt.setString(2, data.getOrDefault("user", ""));
                pstmt.setString(3, data.getOrDefault("password", ""));
                pstmt.setBoolean(4, Boolean.parseBoolean(data.getOrDefault("remember", "false")));
                pstmt.setBoolean(5, Boolean.parseBoolean(data.getOrDefault("usesteamrelay", "false")));
                pstmt.setString(6, data.getOrDefault("authType", ""));
                pstmt.setInt(7, 0);
                pstmt.executeUpdate();
            }
        }
    }

    public ArrayList<Server> getServerList() {
        this.create();
        ArrayList<Server> servers = new ArrayList<>();

        try {
            String serverSql = "SELECT s.* FROM server s LEFT JOIN account a ON s.id = a.serverId GROUP BY s.id ORDER BY MAX(a.lastLogon) DESC NULLS LAST;";

            try (
                Statement stmt = this.conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT s.* FROM server s LEFT JOIN account a ON s.id = a.serverId GROUP BY s.id ORDER BY MAX(a.lastLogon) DESC NULLS LAST;"
                );
            ) {
                while (rs.next()) {
                    Server server = new Server();
                    server.setID(rs.getInt("id"));
                    server.setName(rs.getString("name"));
                    server.setIp(rs.getString("ip"));
                    server.setPort(rs.getInt("port"));
                    server.setServerPassword(rs.getString("serverPassword"));
                    server.setDescription(rs.getString("description"));
                    server.setLastOnline(this.parseDateTime(rs.getString("lastOnline")));
                    server.setLastDataUpdate(this.parseDateTime(rs.getString("lastDataUpdate")));
                    this.loadAccountsForServer(server);
                    servers.add(server);
                }
            }

            this.conn.close();
            this.conn = null;
        } catch (SQLException var11) {
            System.err.println("Error retrieving server list: " + var11.getMessage());
        }

        return servers;
    }

    private void loadAccountsForServer(Server server) throws SQLException {
        String accountSql = "SELECT * FROM account WHERE serverId = ?";

        try (PreparedStatement pstmt = this.conn.prepareStatement("SELECT * FROM account WHERE serverId = ?")) {
            pstmt.setInt(1, server.getID());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account();
                    account.setID(rs.getInt("id"));
                    account.setUserName(rs.getString("username"));
                    account.setPwd(rs.getString("password"));
                    account.setSavePwd(rs.getBoolean("isSavePassword"));
                    account.setUseSteamRelay(rs.getBoolean("isUseSteamRelay"));
                    account.setAuthType(rs.getInt("authType"));
                    account.setPlayerFirstAndLastName(rs.getString("playerFirstAndLastName"));
                    InputStream iconData = rs.getBinaryStream("icon");
                    if (iconData != null) {
                        account.setIcon(new Texture("accountIcon" + account.getID(), new BufferedInputStream(iconData), false));
                    }

                    account.setTimePlayed(rs.getInt("timePlayed"));
                    account.setLastLogon(this.parseDateTime(rs.getString("lastLogon")));
                    server.addAccount(account);
                }
            } catch (Exception var10) {
                throw new RuntimeException(var10);
            }
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr != null && !dateTimeStr.isEmpty()) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (Exception var3) {
                System.err.println("Error parsing datetime: " + dateTimeStr);
                return null;
            }
        } else {
            return null;
        }
    }

    public int saveNewServer(Server server) {
        this.create();
        String sql = "INSERT INTO server (name, ip, port, serverPassword, description, lastOnline, lastDataUpdate) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = this.conn
                .prepareStatement(
                    "INSERT INTO server (name, ip, port, serverPassword, description, lastOnline, lastDataUpdate) VALUES (?, ?, ?, ?, ?, ?, ?)", 1
                )) {
            pstmt.setString(1, server.getName());
            pstmt.setString(2, server.getIp());
            pstmt.setInt(3, server.getPort());
            pstmt.setString(4, server.getServerPassword());
            pstmt.setString(5, server.getDescription());
            pstmt.setString(6, server.getLastOnline() == null ? null : server.getLastOnline().format(formatter));
            pstmt.setString(7, server.getLastDataUpdate() == null ? null : server.getLastDataUpdate().format(formatter));
            int affectedRows = pstmt.executeUpdate();
            this.conn.commit();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int result = rs.getInt(1);
                        this.conn.close();
                        this.conn = null;
                        return result;
                    }
                }
            }

            this.conn.close();
            this.conn = null;
            return -1;
        } catch (SQLException var12) {
            System.err.println("Error saving new server: " + var12.getMessage());
            return -1;
        }
    }

    public boolean updateServer(Server server) {
        if (server != null && server.getID() > 0) {
            this.create();
            String sql = "UPDATE server SET name = ?, ip = ?, port = ?, serverPassword = ?, description = ?, lastOnline = ?, lastDataUpdate = ? WHERE id = ?";

            try {
                boolean var5;
                try (PreparedStatement pstmt = this.conn
                        .prepareStatement(
                            "UPDATE server SET name = ?, ip = ?, port = ?, serverPassword = ?, description = ?, lastOnline = ?, lastDataUpdate = ? WHERE id = ?"
                        )) {
                    pstmt.setString(1, server.getName());
                    pstmt.setString(2, server.getIp());
                    pstmt.setInt(3, server.getPort());
                    pstmt.setString(4, server.getServerPassword());
                    pstmt.setString(5, server.getDescription());
                    pstmt.setString(6, server.getLastOnline() == null ? null : server.getLastOnline().format(formatter));
                    pstmt.setString(7, server.getLastDataUpdate() == null ? null : server.getLastDataUpdate().format(formatter));
                    pstmt.setInt(8, server.getID());
                    int affectedRows = pstmt.executeUpdate();
                    this.conn.commit();
                    this.conn.close();
                    this.conn = null;
                    var5 = affectedRows > 0;
                }

                return var5;
            } catch (SQLException var8) {
                System.err.println("Error updating server: " + var8.getMessage());
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteServer(Server server) {
        if (server.getID() <= 0) {
            return false;
        } else {
            this.create();

            try {
                String deleteAccountsSql = "DELETE FROM account WHERE serverId = ?";

                try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM account WHERE serverId = ?")) {
                    pstmt.setInt(1, server.getID());
                    pstmt.executeUpdate();
                }

                String deleteServerSql = "DELETE FROM server WHERE id = ?";

                boolean var6;
                try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM server WHERE id = ?")) {
                    pstmt.setInt(1, server.getID());
                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows > 0) {
                        this.conn.commit();
                        this.conn.close();
                        this.conn = null;
                        return true;
                    }

                    this.conn.rollback();
                    this.conn.close();
                    this.conn = null;
                    var6 = false;
                }

                return var6;
            } catch (SQLException var12) {
                try {
                    if (this.conn != null) {
                        this.conn.rollback();
                    }

                    this.conn.close();
                    this.conn = null;
                } catch (SQLException var7) {
                    System.err.println("Error during rollback: " + var7.getMessage());
                }

                System.err.println("Error deleting server and accounts: " + var12.getMessage());
                return false;
            }
        }
    }

    public int saveNewAccount(Server server, Account account) {
        this.create();
        String sql = "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed, lastLogon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = this.conn
                .prepareStatement(
                    "INSERT INTO account (serverId, username, password, isSavePassword, isUseSteamRelay, authType, timePlayed, lastLogon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    1
                )) {
            String currentTime = LocalDateTime.now().format(formatter);
            pstmt.setInt(1, server.getID());
            pstmt.setString(2, account.getUserName());
            pstmt.setString(3, account.getPwd());
            pstmt.setBoolean(4, account.isSavePwd());
            pstmt.setBoolean(5, account.getUseSteamRelay());
            pstmt.setInt(6, account.getAuthType());
            pstmt.setInt(7, account.getTimePlayed());
            pstmt.setString(8, account.getLastLogon());
            int affectedRows = pstmt.executeUpdate();
            this.conn.commit();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int result = rs.getInt(1);
                        this.conn.close();
                        this.conn = null;
                        return result;
                    }
                }
            }

            this.conn.close();
            this.conn = null;
            return -1;
        } catch (SQLException var14) {
            System.err.println("Error saving new account: " + var14.getMessage());
            return -1;
        }
    }

    public boolean updateAccount(Account account) {
        if (account.getID() <= 0) {
            return false;
        } else {
            this.create();
            String sql = "UPDATE account SET username = ?, password = ?, isSavePassword = ?, isUseSteamRelay = ?, authType = ?, timePlayed = ?, lastLogon = ?, playerFirstAndLastName = ? WHERE id = ?";

            try {
                boolean var5;
                try (PreparedStatement pstmt = this.conn
                        .prepareStatement(
                            "UPDATE account SET username = ?, password = ?, isSavePassword = ?, isUseSteamRelay = ?, authType = ?, timePlayed = ?, lastLogon = ?, playerFirstAndLastName = ? WHERE id = ?"
                        )) {
                    pstmt.setString(1, account.getUserName());
                    pstmt.setString(2, account.getPwd());
                    pstmt.setBoolean(3, account.isSavePwd());
                    pstmt.setBoolean(4, account.getUseSteamRelay());
                    pstmt.setInt(5, account.getAuthType());
                    pstmt.setInt(6, account.getTimePlayed());
                    pstmt.setString(7, account.getLastLogon());
                    pstmt.setString(8, account.getPlayerFirstAndLastName());
                    pstmt.setInt(9, account.getID());
                    int affectedRows = pstmt.executeUpdate();
                    this.conn.commit();
                    this.conn.close();
                    this.conn = null;
                    var5 = affectedRows > 0;
                }

                return var5;
            } catch (SQLException var8) {
                System.err.println("Error updating account: " + var8.getMessage());
                return false;
            }
        }
    }

    public boolean deleteAccount(Account account) {
        if (account.getID() <= 0) {
            return false;
        } else {
            this.create();
            String sql = "DELETE FROM account WHERE id = ?";

            try {
                boolean var5;
                try (PreparedStatement pstmt = this.conn.prepareStatement("DELETE FROM account WHERE id = ?")) {
                    pstmt.setInt(1, account.getID());
                    int affectedRows = pstmt.executeUpdate();
                    this.conn.commit();
                    this.conn.close();
                    this.conn = null;
                    var5 = affectedRows > 0;
                }

                return var5;
            } catch (SQLException var8) {
                System.err.println("Error deleting account: " + var8.getMessage());
                return false;
            }
        }
    }

    public void setupLastSave() {
        lastSave = LocalDateTime.now();
    }

    public boolean updateAccountIconAndData(String serverAddress, int port, String username, ByteBuffer icon) {
        this.create();
        LocalDateTime currentTime = LocalDateTime.now();
        Duration duration = Duration.between(lastSave, currentTime);
        String sql = "UPDATE account\nSET \n    icon = ?,\n    timePlayed = ?,\n    lastLogon = ?\nWHERE \n    username = ? \n    AND serverId IN (\n        SELECT id \n        FROM server \n        WHERE ip = ? AND port = ?\n    );";

        try {
            boolean var10;
            try (PreparedStatement pstmt = this.conn
                    .prepareStatement(
                        "UPDATE account\nSET \n    icon = ?,\n    timePlayed = ?,\n    lastLogon = ?\nWHERE \n    username = ? \n    AND serverId IN (\n        SELECT id \n        FROM server \n        WHERE ip = ? AND port = ?\n    );"
                    )) {
                pstmt.setBytes(1, icon.array());
                pstmt.setInt(2, (int)duration.toMinutes());
                pstmt.setString(3, currentTime.format(formatter));
                pstmt.setString(4, username);
                pstmt.setString(5, serverAddress);
                pstmt.setInt(6, port);
                int affectedRows = pstmt.executeUpdate();
                this.conn.commit();
                this.conn.close();
                this.conn = null;
                var10 = affectedRows > 0;
            }

            return var10;
        } catch (SQLException var13) {
            System.err.println("Error updating account: " + var13.getMessage());
            return false;
        }
    }
}
