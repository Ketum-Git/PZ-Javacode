// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import de.taimos.totp.TOTP;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import zombie.ZomboidFileSystem;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkUser;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.WordsFilter;
import zombie.core.secure.PZcrypt;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.util.PZSQLUtils;

public class ServerWorldDatabase {
    public static final int AUTH_TYPE_USERNAME_PASSWORD = 1;
    public static final int AUTH_TYPE_GOOGLE_AUTH = 2;
    public static final int AUTH_TYPE_TWO_FACTOR = 3;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static ServerWorldDatabase instance = new ServerWorldDatabase();
    public String commandLineAdminUsername = "admin";
    public String commandLineAdminPassword;
    public boolean doAdmin = true;
    static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
    Connection conn;
    private static final String nullChar = String.valueOf('\u0000');

    public void getWhitelistUsers(HashMap<String, NetworkUser> users) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT world, username, lastConnection, role, authType, steamid, displayName FROM whitelist");
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                IsoPlayer player = GameServer.getPlayerByUserNameForCommand(rs.getString("username"));
                Role role = Roles.getRoleById(rs.getInt("role"));
                if (role == null) {
                    DebugLog.Multiplayer
                        .error(
                            "ServerWorldDatabase.getWhitelistUsers player '" + rs.getString("username") + " have no role. (roleId=" + rs.getInt("role") + ")"
                        );
                    role = Roles.getDefaultForUser();
                }

                NetworkUser user = new NetworkUser(
                    rs.getString("world"),
                    rs.getString("username"),
                    rs.getString("lastConnection"),
                    role,
                    rs.getInt("authType"),
                    rs.getString("steamid"),
                    rs.getString("displayName"),
                    player != null
                );
                user.setInWhitelist(true);
                users.put(user.getUsername(), user);
            }

            stat.close();
        } catch (SQLException var7) {
            var7.printStackTrace();
        }
    }

    public void getUserlogUsers(HashMap<String, NetworkUser> users) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM userlog");
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                if (!users.containsKey(username)) {
                    NetworkUser user = new NetworkUser(GameServer.serverName, username, "", Roles.getDefaultForUser(), 1, "", "", false);
                    users.put(username, user);
                }
            }

            stat.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }
    }

    private int getUserCounter(String username, String type) {
        int counter = 0;

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT SUM(amount) FROM userlog WHERE username = ? AND type = ?");
            stat.setString(1, username);
            stat.setString(2, type);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                counter = rs.getInt(1);
            }

            stat.close();
        } catch (SQLException var6) {
            DebugLog.General.printException(var6, "DataBase counter " + type + " read failed", LogSeverity.Error);
        }

        return counter;
    }

    public void updateUserCounters(Collection<NetworkUser> users) {
        for (NetworkUser user : users) {
            int warningPoints = this.getUserCounter(user.getUsername(), Userlog.UserlogType.WarningPoint.name());
            user.setWarningPoints(warningPoints);
            int suspicionPoints = this.getUserCounter(user.getUsername(), Userlog.UserlogType.SuspiciousActivity.name());
            user.setSuspicionPoints(suspicionPoints);
            int kicks = this.getUserCounter(user.getUsername(), Userlog.UserlogType.Kicked.name());
            user.setKicks(kicks);
        }
    }

    public boolean containsUser(String user, String world) {
        try {
            if (this.conn == null) {
                return false;
            }

            PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
            stat.setString(1, user);
            stat.setString(2, world);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                stat.close();
                return true;
            }

            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return false;
    }

    public boolean containsUser(String user) {
        return this.containsUser(user, Core.gameSaveWorld);
    }

    public boolean containsCaseinsensitiveUser(String user) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
            stat.setString(1, user);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                stat.close();
                return true;
            }

            stat.close();
        } catch (SQLException var4) {
            var4.printStackTrace();
        }

        return false;
    }

    public String changeUsername(String user, String newUsername) throws SQLException {
        PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
        stat.setString(1, user);
        stat.setString(2, Core.gameSaveWorld);
        ResultSet rs = stat.executeQuery();
        if (rs.next()) {
            String id = rs.getString("id");
            stat.close();
            stat = this.conn.prepareStatement("UPDATE whitelist SET username = ? WHERE id = ?");
            stat.setString(1, newUsername);
            stat.setString(2, id);
            stat.executeUpdate();
            stat.close();
            return "Changed " + user + " user's name into " + newUsername;
        } else {
            return !ServerOptions.instance.getBoolean("Open")
                ? "User \"" + user + "\" is not in the whitelist, use /adduser first"
                : "Changed's name " + user + " into " + newUsername;
        }
    }

    public String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }

    public String addUser(String user, String pass) throws SQLException {
        return this.addUser(user, pass, 1);
    }

    public String addUser(String user, String pass, int authType) throws SQLException {
        if (this.containsCaseinsensitiveUser(user)) {
            return "A user with this name already exists";
        } else {
            try {
                PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
                stat.setString(1, user);
                stat.setString(2, Core.gameSaveWorld);
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    stat.close();
                    return "User " + user + " already exist.";
                }

                stat.close();
                stat = this.conn.prepareStatement("INSERT INTO whitelist (world, username, password, authType, role) VALUES (?, ?, ?, ?, ?)");
                stat.setString(1, Core.gameSaveWorld);
                stat.setString(2, user);
                stat.setString(3, pass);
                stat.setInt(4, authType);
                stat.setInt(5, Roles.getDefaultForNewUser().getId());
                stat.executeUpdate();
                stat.close();
            } catch (SQLException var6) {
                var6.printStackTrace();
            }

            return "User " + user + " created with the password " + pass;
        }
    }

    public void updateDisplayName(String user, String displayName) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
            stat.setString(1, user);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                stat.close();
                stat = this.conn.prepareStatement("UPDATE whitelist SET displayName = ? WHERE id = ?");
                stat.setString(1, displayName);
                stat.setInt(2, id);
                stat.executeUpdate();
                stat.close();
            }

            stat.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }
    }

    public String getDisplayName(String username) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
            stat.setString(1, username);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                String displayName = rs.getString("displayName");
                stat.close();
                return displayName;
            }

            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return null;
    }

    public String removeUser(String username, String world) throws SQLException {
        try {
            PreparedStatement stat = this.conn.prepareStatement("DELETE FROM whitelist WHERE world = ? and username = ?");
            stat.setString(1, world);
            stat.setString(2, username);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var4) {
            var4.printStackTrace();
        }

        return "User " + username + " removed from white list";
    }

    public String removeUser(String username) throws SQLException {
        return this.removeUser(username, Core.gameSaveWorld);
    }

    public void removeUserLog(String username, String type, String text) throws SQLException {
        try {
            PreparedStatement stat = this.conn.prepareStatement("DELETE FROM userlog WHERE username = ? AND type = ? AND text = ?");
            stat.setString(1, username);
            stat.setString(2, type);
            stat.setString(3, text);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }
    }

    public void connect() {
        File dbDir = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        File dbFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db" + File.separator + GameServer.serverName + ".db");
        dbFile.setReadable(true, false);
        dbFile.setExecutable(true, false);
        dbFile.setWritable(true, false);
        DebugLog.DetailedInfo.trace("user database \"" + dbFile.getPath() + "\"");
        if (!dbFile.exists()) {
            DebugLog.log("user database doesn't exist");
        } else {
            if (this.conn == null) {
                try {
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                } catch (Exception var5) {
                    var5.printStackTrace();
                    DebugLog.log("failed to open user database");
                }
            } else {
                try {
                    if (this.conn.isClosed()) {
                        this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                    }
                } catch (Exception var4) {
                    var4.printStackTrace();
                    DebugLog.log("failed to open user database");
                }
            }
        }
    }

    public void create() throws SQLException, ClassNotFoundException {
        File dbDir = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        File dbFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "db" + File.separator + GameServer.serverName + ".db");
        dbFile.setReadable(true, false);
        dbFile.setExecutable(true, false);
        dbFile.setWritable(true, false);
        DebugLog.DetailedInfo.trace("user database \"" + dbFile.getPath() + "\"");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
                this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                Statement stat = this.conn.createStatement();
                stat.executeUpdate(
                    "CREATE TABLE [whitelist] ([id] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,[world] TEXT DEFAULT '' NULL,[username] TEXT  NULL, [password] TEXT  NULL, [lastConnection] TEXT NULL, [role] INTEGER NOT NULL, [authType] INTEGER NULL DEFAULT 1, [googleKey] TEXT NULL, [steamid] TEXT NULL, [ownerid] TEXT NULL, [displayName] TEXT NULL)"
                );
                stat.executeUpdate("CREATE UNIQUE INDEX [id] ON [whitelist]([id]  ASC)");
                stat.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
                stat.executeUpdate("CREATE TABLE [bannedip] ([ip] TEXT NOT NULL,[username] TEXT NULL, [reason] TEXT NULL)");
                stat.executeUpdate("CREATE INDEX idx_bannedip_ip ON bannedip (ip);");
                stat.executeUpdate("CREATE INDEX idx_bannedip_username ON bannedip (username);");
                stat.executeUpdate(
                    "CREATE TABLE [role] ([id] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, [name] TEXT NOT NULL,[description] TEXT NULL, [colorR] REAL NOT NULL, [colorG] REAL NOT NULL, [colorB] REAL NOT NULL, [readonly] BOOLEAN NULL DEFAULT false, [position] INTEGER NOT NULL DEFAULT -1 )"
                );
                stat.executeUpdate("CREATE INDEX idx_role_id ON role (id)");
                stat.executeUpdate("CREATE TABLE [capabilities] ([name] TEXT NOT NULL,[role] INTEGER)");
                stat.executeUpdate("CREATE INDEX idx_capabilities_role ON capabilities (role);");
                stat.executeUpdate("CREATE TABLE [defaultRoles] ([name] TEXT NOT NULL,[role] INTEGER)");
                stat.executeUpdate("CREATE INDEX idx_defaultRoles_name ON defaultRoles (name);");
                stat.close();
            } catch (Exception var19) {
                var19.printStackTrace();
                DebugLog.log("failed to create user database, server shut down");
                System.exit(1);
            }
        }

        if (this.conn == null) {
            try {
                this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
            } catch (Exception var18) {
                var18.printStackTrace();
                DebugLog.log("failed to open user database, server shut down");
                System.exit(1);
            }
        } else {
            try {
                if (this.conn.isClosed()) {
                    this.conn = PZSQLUtils.getConnection(dbFile.getAbsolutePath());
                }
            } catch (Exception var17) {
                var17.printStackTrace();
                DebugLog.log("failed to open user database, server shut down");
            }
        }

        DatabaseMetaData md = this.conn.getMetaData();
        Statement stat = this.conn.createStatement();
        ResultSet rs = md.getColumns(null, null, "whitelist", "lastConnection");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'lastConnection' TEXT NULL");
        }

        rs.close();
        rs = md.getColumns(null, null, "whitelist", "authType");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'authType' INTEGER NULL DEFAULT 1");
        }

        rs.close();
        rs = md.getColumns(null, null, "whitelist", "googleKey");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'googleKey' TEXT NULL");
        }

        rs.close();
        if (SteamUtils.isSteamModeEnabled()) {
            rs = md.getColumns(null, null, "whitelist", "steamid");
            if (!rs.next()) {
                stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'steamid' TEXT NULL");
            }

            rs.close();
            rs = md.getColumns(null, null, "whitelist", "ownerid");
            if (!rs.next()) {
                stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'ownerid' TEXT NULL");
            }

            rs.close();
        }

        rs = md.getColumns(null, null, "whitelist", "role");
        if (!rs.next()) {
            stat.executeUpdate(
                "CREATE TABLE [whitelist_new] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,[world] TEXT DEFAULT '"
                    + GameServer.serverName
                    + "' NULL,[username] TEXT  NULL, [password] TEXT  NULL, [lastConnection] TEXT NULL, [role] TEXT 'user', [authType] INTEGER NULL DEFAULT 1, [googleKey] TEXT NULL, [steamid] TEXT NULL, [ownerid] TEXT NULL)"
            );
            PreparedStatement pstat1 = this.conn.prepareStatement("SELECT * FROM whitelist");
            ResultSet rs2 = pstat1.executeQuery();

            while (rs2.next()) {
                PreparedStatement pstat2 = this.conn
                    .prepareStatement(
                        "INSERT INTO whitelist_new (world, username, password, lastConnection, role, authType, googleKey, steamid, ownerid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    );
                int roleId = Roles.getDefaultForUser().getId();
                if ("true".equals(rs2.getString("banned"))) {
                    roleId = Roles.getDefaultForBanned().getId();
                }

                if ("true".equals(rs2.getString("priority"))) {
                    roleId = Roles.getDefaultForPriorityUser().getId();
                }

                if ("true".equals(rs2.getString("moderator"))) {
                    roleId = Roles.getDefaultForModerator().getId();
                }

                if ("true".equals(rs2.getString("admin"))) {
                    roleId = Roles.getDefaultForAdmin().getId();
                }

                if ("admin".equals(rs2.getString("accesslevel"))) {
                    roleId = Roles.getDefaultForAdmin().getId();
                }

                pstat2.setString(1, rs2.getString("world"));
                pstat2.setString(2, rs2.getString("username"));
                pstat2.setString(3, rs2.getString("password"));
                pstat2.setString(4, rs2.getString("lastConnection"));
                pstat2.setInt(5, roleId);
                pstat2.setString(6, rs2.getString("authType"));
                pstat2.setString(7, rs2.getString("googleKey"));

                try {
                    pstat2.setString(8, rs2.getString("steamid"));
                    pstat2.setString(9, rs2.getString("ownerid"));
                } catch (Exception var16) {
                    pstat2.setString(8, null);
                    pstat2.setString(9, null);
                }

                pstat2.executeUpdate();
            }

            rs2.close();
            stat.executeUpdate("DROP TABLE 'whitelist'");
            stat.executeUpdate("ALTER TABLE 'whitelist_new' RENAME TO 'whitelist'");
            stat.executeUpdate("CREATE UNIQUE INDEX [id] ON [whitelist]([id]  ASC)");
            stat.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
        }

        rs.close();
        rs = md.getColumns(null, null, "whitelist", "displayName");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'displayName' TEXT NULL");
        }

        rs.close();
        rs = stat.executeQuery("SELECT * FROM sqlite_master WHERE type = 'index' AND sql LIKE '%UNIQUE%' and name = 'username'");
        if (!rs.next()) {
            try {
                stat.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
            } catch (Exception var15) {
                System.out
                    .println("Can't create the username index because some of the username in the database are in double, will drop the double username.");
                stat.executeUpdate(
                    "DELETE FROM whitelist WHERE whitelist.rowid > (SELECT rowid FROM whitelist dbl WHERE whitelist.rowid <> dbl.rowid AND  whitelist.username = dbl.username);"
                );
                stat.executeUpdate("CREATE UNIQUE INDEX [username] ON [whitelist]([username]  ASC)");
            }
        }

        rs = md.getTables(null, null, "bannedip", null);
        if (!rs.next()) {
            stat.executeUpdate("CREATE TABLE [bannedip] ([ip] TEXT NOT NULL,[username] TEXT NULL, [reason] TEXT NULL)");
        }

        rs.close();
        rs = md.getTables(null, null, "bannedid", null);
        if (!rs.next()) {
            stat.executeUpdate("CREATE TABLE [bannedid] ([steamid] TEXT NOT NULL, [reason] TEXT NULL)");
        }

        rs.close();
        rs = md.getTables(null, null, "userlog", null);
        if (!rs.next()) {
            stat.executeUpdate(
                "CREATE TABLE [userlog] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL,[username] TEXT  NULL,[type] TEXT  NULL, [text] TEXT  NULL, [issuedBy] TEXT  NULL, [amount] INTEGER NULL, [lastUpdate] TEXT NULL)"
            );
        }

        rs.close();
        rs = md.getColumns(null, null, "userlog", "lastUpdate");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'userlog' ADD 'lastUpdate' TEXT NULL");
        }

        rs.close();
        rs = md.getTables(null, null, "tickets", null);
        if (!rs.next()) {
            stat.executeUpdate(
                "CREATE TABLE [tickets] ([id] INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL, [message] TEXT NOT NULL, [author] TEXT NOT NULL,[answeredID] INTEGER,[viewed] BOOLEAN NULL DEFAULT false)"
            );
        }

        rs.close();
        rs = md.getTables(null, null, "role", null);
        if (!rs.next()) {
            stat.executeUpdate(
                "CREATE TABLE [role] ([id] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, [name] TEXT NOT NULL,[description] TEXT NULL, [colorR] REAL NOT NULL, [colorG] REAL NOT NULL, [colorB] REAL NOT NULL, [readonly] BOOLEAN NULL DEFAULT false, [position] INTEGER NOT NULL DEFAULT -1 )"
            );
            stat.executeUpdate("CREATE INDEX idx_role_id ON role (id)");
            stat.executeUpdate("CREATE TABLE [capabilities] ([name] TEXT NOT NULL,[role] INTEGER)");
            stat.executeUpdate("CREATE INDEX idx_capabilities_role ON capabilities (role);");
            stat.executeUpdate("CREATE TABLE [defaultRoles] ([name] TEXT NOT NULL,[role] INTEGER)");
            stat.executeUpdate("CREATE INDEX idx_defaultRoles_name ON defaultRoles (name);");
            stat.executeUpdate("ALTER TABLE 'whitelist' RENAME 'role' TO 'rolename'");
            stat.executeUpdate("ALTER TABLE 'whitelist' ADD 'role' INTEGER NOT NULL DEFAULT -1");
            Roles.init();

            try (PreparedStatement stat2 = this.conn.prepareStatement("SELECT id, rolename, username FROM whitelist")) {
                ResultSet rs2 = stat2.executeQuery();

                while (rs2.next()) {
                    int userId = rs2.getInt("id");
                    Role role = Roles.getRole(rs2.getString("rolename"));
                    if (role == null) {
                        role = Roles.getDefaultForUser();
                        DebugLog.Multiplayer
                            .warn("The server can't find role for " + rs2.getString("username") + ". The '" + role.getName() + "' role is set.");
                    }

                    try (PreparedStatement pstat3 = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE id = ?")) {
                        pstat3.setInt(1, role.getId());
                        pstat3.setInt(2, userId);
                        pstat3.executeUpdate();
                    }
                }
            } catch (Exception var22) {
                DebugLog.Multiplayer.printException(var22, "Query execution failed", LogSeverity.Error);
            }

            stat.executeUpdate("ALTER TABLE 'whitelist' DROP rolename");
        }

        rs.close();
        rs = md.getColumns(null, null, "role", "position");
        if (!rs.next()) {
            stat.executeUpdate("ALTER TABLE 'role' ADD 'position' INTEGER NOT NULL DEFAULT -1");
        }

        rs.close();
        PreparedStatement pstat1 = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ?");
        pstat1.setString(1, this.commandLineAdminUsername);
        rs = pstat1.executeQuery();
        if (!rs.next()) {
            pstat1.close();
            String pwd = this.commandLineAdminPassword;
            if (pwd == null || pwd.isEmpty()) {
                Scanner scanner = new Scanner(new InputStreamReader(System.in));
                System.out.println("User 'admin' not found, creating it ");
                System.out.println("Command line admin password: " + this.commandLineAdminPassword);
                System.out.println("Enter new administrator password: ");

                for (pwd = scanner.nextLine(); pwd == null || "".equals(pwd); pwd = scanner.nextLine()) {
                    System.out.println("Enter new administrator password: ");
                }

                System.out.println("Confirm the password: ");

                for (String confirmPwd = scanner.nextLine();
                    confirmPwd == null || "".equals(confirmPwd) || !pwd.equals(confirmPwd);
                    confirmPwd = scanner.nextLine()
                ) {
                    System.out.println("Wrong password, confirm the password: ");
                }
            }

            Roles.init();
            pstat1 = this.conn.prepareStatement("INSERT INTO whitelist (world, username, password, role) VALUES (?, ?, ?, ?)");
            pstat1.setString(1, GameServer.serverName);
            pstat1.setString(2, this.commandLineAdminUsername);
            pstat1.setString(3, PZcrypt.hash(encrypt(pwd)));
            pstat1.setInt(4, this.doAdmin ? Roles.getDefaultForAdmin().getId() : Roles.getDefaultForNewUser().getId());
            pstat1.executeUpdate();
            pstat1.close();
            System.out.println("Administrator account '" + this.commandLineAdminUsername + "' created.");
        } else {
            pstat1.close();
        }

        stat.close();
        if (this.commandLineAdminPassword != null && !this.commandLineAdminPassword.isEmpty() && !GameServer.softReset) {
            String encryptedPwd = PZcrypt.hash(encrypt(this.commandLineAdminPassword));
            PreparedStatement pstat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ?");
            pstat.setString(1, this.commandLineAdminUsername);
            rs = pstat.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                pstat.close();
                pstat = this.conn.prepareStatement("UPDATE whitelist SET password = ? WHERE id = ?");
                pstat.setString(1, encryptedPwd);
                pstat.setInt(2, id);
                pstat.executeUpdate();
                System.out.println("admin password changed via -adminpassword option");
            } else {
                System.out.println("ERROR: -adminpassword ignored, no '" + this.commandLineAdminUsername + "' account in db");
            }

            pstat.close();
        }
    }

    public void close() {
        try {
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (SQLException var2) {
            var2.printStackTrace();
        }
    }

    public static boolean isValidUserName(String user) {
        if (user == null
            || user.trim().isEmpty()
            || user.contains(";")
            || user.contains("@")
            || user.contains("$")
            || user.contains(",")
            || user.contains("\\")
            || user.contains("/")
            || user.contains(".")
            || user.contains("'")
            || user.contains("?")
            || user.contains("\"")
            || user.trim().length() < 2
            || user.length() > 20) {
            return false;
        } else if (user.contains(nullChar)) {
            return false;
        } else if (user.trim().equals("admin")) {
            return true;
        } else {
            return user.trim().toLowerCase().startsWith("admin") ? false : !WordsFilter.getInstance().detectBadWords(user);
        }
    }

    public void saveRole(Role role) {
        boolean isNewRole = role.getId() == -1;
        if (isNewRole) {
            try {
                PreparedStatement stat = this.conn
                    .prepareStatement("INSERT INTO role (name, description, colorR, colorG, colorB, readonly, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                stat.setString(1, role.getName());
                stat.setString(2, role.getDescription());
                stat.setFloat(3, role.getColor().r);
                stat.setFloat(4, role.getColor().g);
                stat.setFloat(5, role.getColor().b);
                stat.setBoolean(6, role.isReadOnly());
                stat.setInt(7, role.getPosition());
                stat.executeUpdate();
                stat.close();
                stat = this.conn.prepareStatement("SELECT id FROM role WHERE name = ?");
                stat.setString(1, role.getName());
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    role.setId(rs.getInt("id"));
                }

                rs.close();
                stat.close();
            } catch (SQLException var6) {
                throw new RuntimeException(var6);
            }
        } else {
            try {
                PreparedStatement stat = this.conn
                    .prepareStatement(
                        "UPDATE role SET name = ?, description = ?, colorR = ?, colorG = ?, colorB = ?, position = ? WHERE id = ? AND readonly = false"
                    );
                stat.setString(1, role.getName());
                stat.setString(2, role.getDescription());
                stat.setFloat(3, role.getColor().r);
                stat.setFloat(4, role.getColor().g);
                stat.setFloat(5, role.getColor().b);
                stat.setInt(6, role.getPosition());
                stat.setInt(7, role.getId());
                stat.executeUpdate();
                stat.close();
            } catch (SQLException var5) {
                throw new RuntimeException(var5);
            }
        }

        this.saveRoleCapabilities(role);
    }

    public void removeRole(Role role, Role newRoleInsteadExist) {
        if (!role.isReadOnly()) {
            try {
                PreparedStatement stat = this.conn.prepareStatement("SELECT readonly FROM role WHERE id = ?");
                stat.setInt(1, role.getId());
                ResultSet rs = stat.executeQuery();
                if (rs.next() && rs.getBoolean("readonly")) {
                    rs.close();
                    stat.close();
                } else {
                    rs.close();
                    stat.close();
                    stat = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE role = ?");
                    stat.setInt(1, newRoleInsteadExist.getId());
                    stat.setInt(2, role.getId());
                    stat.executeUpdate();
                    stat.close();
                    stat = this.conn.prepareStatement("DELETE FROM role WHERE id = ?");
                    stat.setInt(1, role.getId());
                    stat.executeUpdate();
                    stat.close();
                    stat = this.conn.prepareStatement("DELETE FROM capabilities WHERE role = ?");
                    stat.setInt(1, role.getId());
                    stat.executeUpdate();
                    stat.close();
                }
            } catch (SQLException var5) {
                throw new RuntimeException(var5);
            }
        }
    }

    public void saveDefaultRole(Role role, String name) {
        try {
            int newRole = -1;
            PreparedStatement stat = this.conn.prepareStatement("SELECT role FROM defaultRoles WHERE LOWER(name) = LOWER(?)");
            stat.setString(1, name);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                newRole = rs.getInt("role");
            }

            rs.close();
            stat.close();
            if (newRole == -1) {
                stat = this.conn.prepareStatement("INSERT INTO defaultRoles (name, role) VALUES (?, ?)");
                stat.setString(1, name);
                stat.setInt(2, role.getId());
                stat.executeUpdate();
                stat.close();
            }

            if (newRole != role.getId()) {
                stat = this.conn.prepareStatement("UPDATE defaultRoles SET role = ? WHERE name = ?");
                stat.setInt(1, role.getId());
                stat.setString(2, name);
                stat.executeUpdate();
                stat.close();
            }
        } catch (SQLException var6) {
            throw new RuntimeException(var6);
        }
    }

    public int getDefaultRoleId(String name) {
        try {
            int roleId = -1;
            PreparedStatement stat = this.conn.prepareStatement("SELECT role FROM defaultRoles WHERE LOWER(name) = LOWER(?)");
            stat.setString(1, name);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                roleId = rs.getInt("role");
            }

            rs.close();
            stat.close();
            return roleId;
        } catch (SQLException var5) {
            throw new RuntimeException(var5);
        }
    }

    public void saveRoleCapabilities(Role role) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("DELETE FROM capabilities WHERE role = ?");
            stat.setInt(1, role.getId());
            stat.executeUpdate();
            stat.close();

            for (Capability c : role.getCapabilities()) {
                stat = this.conn.prepareStatement("INSERT INTO capabilities (name, role) VALUES (?, ?)");
                stat.setString(1, c.name());
                stat.setInt(2, role.getId());
                stat.executeUpdate();
                stat.close();
            }
        } catch (SQLException var5) {
            throw new RuntimeException(var5);
        }
    }

    public void loadRoles(ArrayList<Role> roles) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM role");
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                Role r = new Role("");
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                boolean isReadOnly = rs.getBoolean("readonly");
                if (isReadOnly) {
                    for (Role r2 : roles) {
                        if (r2.getName().equals(r.getName())) {
                            r2.setId(r.getId());
                            break;
                        }
                    }
                } else {
                    r.setDescription(rs.getString("description"));
                    Color cl = new Color();
                    cl.r = rs.getFloat("colorR");
                    cl.g = rs.getFloat("colorG");
                    cl.b = rs.getFloat("colorB");
                    cl.a = 1.0F;
                    r.setColor(cl);
                    r.setPosition(rs.getInt("position"));
                    r.cleanCapability();
                    PreparedStatement stat2 = this.conn.prepareStatement("SELECT name FROM capabilities WHERE role = ?");
                    stat2.setInt(1, r.getId());
                    ResultSet rs2 = stat2.executeQuery();

                    while (rs2.next()) {
                        try {
                            r.addCapability(Capability.valueOf(rs2.getString("name")));
                        } catch (Exception var10) {
                            DebugLog.General.warn("Capability " + rs2.getString("name") + " not found");
                        }
                    }

                    rs2.close();
                    stat2.close();
                    roles.add(r);
                }
            }

            rs.close();
            stat.close();
        } catch (SQLException var11) {
            throw new RuntimeException(var11);
        }
    }

    public ServerWorldDatabase.LogonResult googleAuthClient(String user, String code) {
        ServerWorldDatabase.LogonResult result = new ServerWorldDatabase.LogonResult();

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT googleKey FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
            stat.setString(1, user);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                if (isNullOrEmpty(rs.getString("googleKey"))) {
                    result.authorized = false;
                    stat.close();
                    result.dcReason = "GoogleSecretKeyIsAbsent";
                    return result;
                }

                if (!code.equals(this.getTOTPCode(rs.getString("googleKey")))) {
                    result.authorized = false;
                    stat.close();
                    result.dcReason = "InvalidGoogleAuthCode";
                    return result;
                }

                result.authorized = true;
            }
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return result;
    }

    public ServerWorldDatabase.LogonResult authClient(String user, String pass, String ip, long steamID, int authType) {
        DebugLog.DetailedInfo.trace("User " + user + " is trying to connect.");
        ServerWorldDatabase.LogonResult result = new ServerWorldDatabase.LogonResult();
        if (!ServerOptions.instance.allowNonAsciiUsername.getValue() && !asciiEncoder.canEncode(user)) {
            result.authorized = false;
            result.dcReason = "NonAsciiCharacters";
            return result;
        } else if (!isValidUserName(user)) {
            result.authorized = false;
            result.dcReason = "InvalidUsername";
            return result;
        } else {
            try {
                if (!SteamUtils.isSteamModeEnabled() && !ip.equals("127.0.0.1") && !ip.equals("localhost")) {
                    PreparedStatement stat = this.conn.prepareStatement("SELECT reason FROM bannedip WHERE ip = ?");
                    stat.setString(1, ip);
                    ResultSet rs = stat.executeQuery();
                    if (rs.next()) {
                        result.authorized = false;
                        result.bannedReason = rs.getString("reason");
                        result.role = Roles.getDefaultForBanned();
                        stat.close();
                        return result;
                    }

                    stat.close();
                }

                if (isNullOrEmpty(pass)
                    && ServerOptions.instance.open.getValue()
                    && ServerOptions.instance.autoCreateUserInWhiteList.getValue()
                    && authType != 2) {
                    result.dcReason = "UserPasswordRequired";
                    result.authorized = false;
                    return result;
                }

                PreparedStatement stat = this.conn
                    .prepareStatement("SELECT role, password, authType, googleKey FROM whitelist WHERE LOWER(username) = LOWER(?) AND world = ?");
                stat.setString(1, user);
                stat.setString(2, Core.gameSaveWorld);
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    Role roleInternal = Roles.getRoleById(rs.getInt("role"));
                    result.role = roleInternal != null ? roleInternal : Roles.getDefaultForBanned();
                    if (!result.role.hasCapability(Capability.LoginOnServer)) {
                        result.authorized = false;
                        result.bannedReason = "";
                        stat.close();
                        return result;
                    }

                    if (rs.getInt("authType") != authType) {
                        result.authorized = false;
                        stat.close();
                        result.dcReason = "WrongAuthenticationMethod";
                    }

                    if ((rs.getInt("authType") == 1 || rs.getInt("authType") == 3)
                        && !isNullOrEmpty(rs.getString("password"))
                        && !rs.getString("password").equals(pass)) {
                        result.authorized = false;
                        stat.close();
                        if (isNullOrEmpty(pass)) {
                            result.dcReason = "DuplicateAccount";
                        } else {
                            result.dcReason = "InvalidUsernamePassword";
                        }

                        return result;
                    }

                    if (rs.getInt("authType") == 2 || rs.getInt("authType") == 3) {
                        if (!isNullOrEmpty(rs.getString("googleKey"))) {
                            result.needSecondFactor = true;
                        } else {
                            result.needSecondFactor = false;
                        }
                    }

                    result.authorized = true;
                    stat.close();
                    return result;
                }

                if (ServerOptions.instance.open.getValue()) {
                    if (!this.isNewAccountAllowed(ip, steamID)) {
                        stat.close();
                        result.authorized = false;
                        result.dcReason = "MaxAccountsReached";
                        return result;
                    }

                    result.authorized = true;
                    stat.close();
                    return result;
                }

                result.authorized = false;
                result.dcReason = "UnknownUsername";
                stat.close();
            } catch (Exception var11) {
                var11.printStackTrace();
            }

            return result;
        }
    }

    public ServerWorldDatabase.LogonResult authClient(long steamID) {
        String steamIDstr = SteamUtils.convertSteamIDToString(steamID);
        System.out.println("Steam client " + steamIDstr + " is initiating a connection.");
        ServerWorldDatabase.LogonResult result = new ServerWorldDatabase.LogonResult();

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM bannedid WHERE steamid = ?");
            stat.setString(1, steamIDstr);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                result.authorized = false;
                result.bannedReason = rs.getString("reason");
                result.role = Roles.getDefaultForBanned();
                stat.close();
                return result;
            }

            stat.close();
            result.authorized = true;
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return result;
    }

    public ServerWorldDatabase.LogonResult authOwner(long steamID, long ownerID) {
        String steamIDstr = SteamUtils.convertSteamIDToString(steamID);
        String ownerIDstr = SteamUtils.convertSteamIDToString(ownerID);
        System.out.println("Steam client " + steamIDstr + " borrowed the game from " + ownerIDstr);
        ServerWorldDatabase.LogonResult result = new ServerWorldDatabase.LogonResult();

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM bannedid WHERE steamid = ?");
            stat.setString(1, ownerIDstr);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                result.authorized = false;
                result.bannedReason = rs.getString("reason");
                result.role = Roles.getDefaultForBanned();
                stat.close();
                return result;
            }

            stat.close();
            result.authorized = true;
            stat = this.conn.prepareStatement("UPDATE whitelist SET ownerid = ? where steamid = ?");
            stat.setString(1, ownerIDstr);
            stat.setString(2, steamIDstr);
            stat.executeUpdate();
            stat.close();
        } catch (Exception var10) {
            var10.printStackTrace();
        }

        return result;
    }

    private boolean isNewAccountAllowed(String ip, long steamID) {
        int max = ServerOptions.instance.maxAccountsPerUser.getValue();
        if (max <= 0) {
            return true;
        } else if (!SteamUtils.isSteamModeEnabled()) {
            return true;
        } else {
            String steamIDStr = SteamUtils.convertSteamIDToString(steamID);
            int count = 0;

            try (PreparedStatement stat = this.conn.prepareStatement("SELECT COUNT(id) FROM whitelist WHERE steamid = ?")) {
                stat.setString(1, steamIDStr);
                ResultSet rs = stat.executeQuery();
                count = rs.getInt(1);
            } catch (Exception var16) {
                DebugLog.Multiplayer.printException(var16, "Query execution failed", LogSeverity.Error);
                return true;
            }

            try {
                label97: {
                    boolean var10;
                    try (PreparedStatement stat = this.conn.prepareStatement("SELECT role FROM whitelist WHERE steamid = ?")) {
                        stat.setString(1, steamIDStr);
                        ResultSet rs = stat.executeQuery();

                        Role role;
                        do {
                            if (!rs.next()) {
                                break label97;
                            }

                            role = Roles.getRoleById(rs.getInt(1));
                        } while (!role.hasCapability(Capability.PriorityLogin));

                        var10 = true;
                    }

                    return var10;
                }
            } catch (Exception var15) {
                DebugLog.Multiplayer.printException(var15, "Query execution failed", LogSeverity.Error);
                return true;
            }

            DebugLog.Multiplayer.debugln("IsNewAccountAllowed: steam-id=%d count=%d/%d", steamID, count, max);
            return count < max;
        }
    }

    public static String encrypt(String previousPwd) {
        if (isNullOrEmpty(previousPwd)) {
            return "";
        } else {
            byte[] crypted = null;

            try {
                crypted = MessageDigest.getInstance("MD5").digest(previousPwd.getBytes());
            } catch (NoSuchAlgorithmException var5) {
                System.out.println("Can't encrypt password");
                var5.printStackTrace();
            }

            StringBuilder hashString = new StringBuilder();

            for (int i = 0; i < crypted.length; i++) {
                String hex = Integer.toHexString(crypted[i]);
                if (hex.length() == 1) {
                    hashString.append('0');
                    hashString.append(hex.charAt(hex.length() - 1));
                } else {
                    hashString.append(hex.substring(hex.length() - 2));
                }
            }

            return hashString.toString();
        }
    }

    public String changePassword(String username, String newPwd) throws SQLException {
        PreparedStatement stat = this.conn.prepareStatement("UPDATE whitelist SET password = ?, authType = ? WHERE username = ? and world = ?");
        stat.setString(1, newPwd);
        stat.setInt(2, 1);
        stat.setString(3, username);
        stat.setString(4, Core.gameSaveWorld);
        stat.executeUpdate();
        stat.close();
        return "Your new password is " + newPwd;
    }

    public String changePwd(String username, String previousPwd, String newPwd) throws SQLException {
        PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND password = ? AND world = ?");
        stat.setString(1, username);
        stat.setString(2, previousPwd);
        stat.setString(3, Core.gameSaveWorld);
        ResultSet rs = stat.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("id");
            stat.close();
            stat = this.conn.prepareStatement("UPDATE whitelist SET password = ? WHERE id = ?");
            stat.setString(1, newPwd);
            stat.setInt(2, id);
            stat.executeUpdate();
            stat.close();
            return "Your new password is " + newPwd;
        } else {
            stat.close();
            return "Wrong password for user " + username;
        }
    }

    public String setRole(String username, Role role) throws SQLException {
        if (!this.containsUser(username)) {
            this.addUser(username, "");
        }

        PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
        stat.setString(1, username);
        stat.setString(2, Core.gameSaveWorld);
        ResultSet rs = stat.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("id");
            stat.close();
            stat = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE id = ?");
            stat.setInt(1, role.getId());
            stat.setInt(2, id);
            stat.executeUpdate();
            stat.close();
            return "User " + username + " is now " + role.getName();
        } else {
            stat.close();
            return "User \"" + username + "\" is not in the whitelist, use /adduser first";
        }
    }

    public ArrayList<Userlog> getUserlog(String username) {
        ArrayList<Userlog> result = new ArrayList<>();

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT type, text, issuedBy, amount, lastUpdate  FROM userlog WHERE username = ?");
            stat.setString(1, username);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                result.add(
                    new Userlog(username, rs.getString("type"), rs.getString("text"), rs.getString("issuedBy"), rs.getInt("amount"), rs.getString("lastUpdate"))
                );
            }

            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return result;
    }

    public void addUserlog(String username, Userlog.UserlogType type, String text, String issuedBy, int amount) {
        try {
            boolean add = true;
            String lastUpdate = dateFormat.format(Calendar.getInstance().getTime());
            if (type != Userlog.UserlogType.LuaChecksum && type != Userlog.UserlogType.DupeItem) {
                if (type == Userlog.UserlogType.Kicked
                    || type == Userlog.UserlogType.Banned
                    || type == Userlog.UserlogType.SuspiciousActivity
                    || type == Userlog.UserlogType.UnauthorizedPacket) {
                    PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM userlog WHERE username = ? AND type = ? AND text = ? AND issuedBy = ?");
                    stat.setString(1, username);
                    stat.setString(2, type.toString());
                    stat.setString(3, text);
                    stat.setString(4, issuedBy);
                    ResultSet rs = stat.executeQuery();
                    if (rs.next()) {
                        add = false;
                        amount = Integer.parseInt(rs.getString("amount")) + 1;
                        stat.close();
                        PreparedStatement stat2 = this.conn
                            .prepareStatement("UPDATE userlog set amount = ?, lastUpdate = ? WHERE username = ? AND type = ? AND text = ? AND issuedBy = ?");
                        stat2.setString(1, String.valueOf(amount));
                        stat2.setString(2, lastUpdate);
                        stat2.setString(3, username);
                        stat2.setString(4, type.toString());
                        stat2.setString(5, text);
                        stat2.setString(6, issuedBy);
                        stat2.executeUpdate();
                        stat2.close();
                    }
                }
            } else {
                PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM userlog WHERE username = ? AND type = ?");
                stat.setString(1, username);
                stat.setString(2, type.toString());
                ResultSet rs = stat.executeQuery();
                if (rs.next()) {
                    add = false;
                    amount = Integer.parseInt(rs.getString("amount")) + 1;
                    stat.close();
                    PreparedStatement stat2 = this.conn
                        .prepareStatement("UPDATE userlog set amount = ?, lastUpdate = ?, text = ? WHERE username = ? AND type = ?");
                    stat2.setString(1, String.valueOf(amount));
                    stat2.setString(2, lastUpdate);
                    stat2.setString(3, text);
                    stat2.setString(4, username);
                    stat2.setString(5, type.toString());
                    stat2.executeUpdate();
                    stat2.close();
                }
            }

            if (add) {
                PreparedStatement stat2 = this.conn
                    .prepareStatement("INSERT INTO userlog (username, type, text, issuedBy, amount, lastUpdate) VALUES (?, ?, ?, ?, ?, ?)");
                stat2.setString(1, username);
                stat2.setString(2, type.toString());
                stat2.setString(3, text);
                stat2.setString(4, issuedBy);
                stat2.setString(5, String.valueOf(amount));
                stat2.setString(6, lastUpdate);
                stat2.executeUpdate();
                stat2.close();
            }
        } catch (Exception var11) {
            var11.printStackTrace();
        }
    }

    public Role getUserRoleNameByUsername(String username) {
        Role role = Roles.getDefaultForUser();

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT role FROM whitelist WHERE username = ? AND world = ?");
            stat.setString(1, username);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            boolean exists = rs.next();
            if (exists) {
                role = Roles.getRoleById(rs.getInt("role"));
            }

            stat.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }

        return role;
    }

    public String banUser(String username, boolean ban) throws SQLException {
        PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
        stat.setString(1, username);
        stat.setString(2, Core.gameSaveWorld);
        ResultSet rs = stat.executeQuery();
        boolean exists = rs.next();
        if (ban && !exists) {
            PreparedStatement stat2 = this.conn.prepareStatement("INSERT INTO whitelist (world, username, role, password) VALUES (?, ?, ?, ?)");
            stat2.setString(1, Core.gameSaveWorld);
            stat2.setString(2, username);
            stat2.setInt(3, Roles.getDefaultForBanned().getId());
            stat2.setString(4, PZcrypt.hash(encrypt("BANNEDPASS" + username)));
            stat2.executeUpdate();
            stat2.close();
            rs = stat.executeQuery();
            exists = true;
        }

        stat.close();
        if (exists) {
            Role role = ban ? Roles.getDefaultForBanned() : Roles.getDefaultForUser();
            stat = this.conn.prepareStatement("UPDATE whitelist SET role = ? WHERE username = ?");
            stat.setInt(1, role.getId());
            stat.setString(2, username);
            stat.executeUpdate();
            stat.close();
            return "User \"" + username + "\" is now " + (ban ? "banned" : "un-banned");
        } else {
            return "User \"" + username + "\" is not in the whitelist, use /adduser first";
        }
    }

    public String getFirstBannedIPForUser(String username) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT ip FROM bannedip WHERE username = ?");
            stat.setString(1, username);
            ResultSet rs = stat.executeQuery();
            boolean exists = rs.next();
            String res = null;
            if (exists) {
                res = rs.getString(1);
            }

            stat.close();
            return res;
        } catch (SQLException var6) {
            var6.printStackTrace();
            return null;
        }
    }

    public String banIp(String ip, String username, String reason, boolean ban) throws SQLException {
        if (ban) {
            PreparedStatement stat = this.conn.prepareStatement("INSERT INTO bannedip (ip, username, reason) VALUES (?, ?, ?)");
            stat.setString(1, ip);
            stat.setString(2, username);
            stat.setString(3, reason);
            stat.executeUpdate();
            stat.close();
        } else {
            if (ip != null) {
                PreparedStatement stat = this.conn.prepareStatement("DELETE FROM bannedip WHERE ip = ?");
                stat.setString(1, ip);
                stat.executeUpdate();
                stat.close();
            }

            if (username != "") {
                PreparedStatement stat = this.conn.prepareStatement("DELETE FROM bannedip WHERE username = ?");
                stat.setString(1, username);
                stat.executeUpdate();
                stat.close();
            }
        }

        return "IP " + ip + "(" + username + ")  is now " + (ban ? "banned" : "un-banned");
    }

    public String isSteamIdBanned(String _steamID) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT steamid FROM bannedid WHERE steamid = ?");
            stat.setString(1, _steamID);
            ResultSet rs = stat.executeQuery();
            boolean exists = rs.next();
            String res = null;
            if (exists) {
                res = _steamID;
            }

            stat.close();
            return res;
        } catch (SQLException var6) {
            var6.printStackTrace();
            return null;
        }
    }

    public String banSteamID(String steamID, String reason, boolean ban) throws SQLException {
        if (ban) {
            PreparedStatement stat = this.conn.prepareStatement("INSERT INTO bannedid (steamid, reason) VALUES (?, ?)");
            stat.setString(1, steamID);
            stat.setString(2, reason);
            stat.executeUpdate();
            stat.close();
        } else {
            PreparedStatement stat = this.conn.prepareStatement("DELETE FROM bannedid WHERE steamid = ?");
            stat.setString(1, steamID);
            stat.executeUpdate();
            stat.close();
        }

        return "SteamID " + steamID + " is now " + (ban ? "banned" : "un-banned");
    }

    public String setUserSteamID(String user, String steamID) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT steamid FROM whitelist WHERE username = ?");
            stat.setString(1, user);
            ResultSet rs = stat.executeQuery();
            if (!rs.next()) {
                stat.close();
                return "User " + user + " not found";
            }

            stat.close();
            stat = this.conn.prepareStatement("UPDATE whitelist SET steamid = ? WHERE username = ?");
            stat.setString(1, steamID);
            stat.setString(2, user);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return "User " + user + " SteamID set to " + steamID;
    }

    public void setPassword(String username, String encryptedPwd) throws SQLException {
        try {
            PreparedStatement stat = this.conn.prepareStatement("UPDATE whitelist SET password = ? WHERE username = ? and world = ?");
            stat.setString(1, encryptedPwd);
            stat.setString(2, username);
            stat.setString(3, Core.gameSaveWorld);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var4) {
            var4.printStackTrace();
        }
    }

    public String getUserGoogleKey(String username) throws SQLException {
        String key = "";

        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT googleKey FROM whitelist WHERE username = ? and world = ?");
            stat.setString(1, username);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                key = rs.getString("googleKey");
            }

            stat.close();
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

        return key;
    }

    public boolean setUserGoogleKey(String username, String key) throws SQLException {
        try {
            PreparedStatement stat = this.conn.prepareStatement("SELECT id FROM whitelist WHERE username = ? AND world = ?");
            stat.setString(1, username);
            stat.setString(2, Core.gameSaveWorld);
            ResultSet rs = stat.executeQuery();
            boolean exists = rs.next();
            if (!exists) {
                stat.close();
                return false;
            }

            stat = this.conn.prepareStatement("UPDATE whitelist SET googleKey = ? WHERE username = ? and world = ?");
            stat.setString(1, key);
            stat.setString(2, username);
            stat.setString(3, Core.gameSaveWorld);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var6) {
            var6.printStackTrace();
        }

        return true;
    }

    public void resetUserGoogleKey(String username) throws SQLException {
        try {
            PreparedStatement stat = this.conn.prepareStatement("UPDATE whitelist SET googleKey = ?, authType = ? WHERE username = ? and world = ?");
            stat.setString(1, "");
            stat.setInt(2, 1);
            stat.setString(3, username);
            stat.setString(4, Core.gameSaveWorld);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var3) {
            var3.printStackTrace();
        }
    }

    public void updateLastConnectionDate(String u, String p) {
        try {
            PreparedStatement stat = this.conn.prepareStatement("UPDATE whitelist SET lastConnection = ? WHERE username = ? AND password = ?");
            stat.setString(1, dateFormat.format(Calendar.getInstance().getTime()));
            stat.setString(2, u);
            stat.setString(3, p);
            stat.executeUpdate();
            stat.close();
        } catch (SQLException var4) {
            var4.printStackTrace();
        }
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public String addWarningPoint(String username, String reason, int amount, String issuedBy) throws SQLException {
        PreparedStatement stat = this.conn.prepareStatement("SELECT * FROM whitelist WHERE username = ? AND world = ?");
        stat.setString(1, username);
        stat.setString(2, Core.gameSaveWorld);
        ResultSet rs = stat.executeQuery();
        if (rs.next()) {
            this.addUserlog(username, Userlog.UserlogType.WarningPoint, reason, issuedBy, amount);
            return "Added a warning point on " + username + " reason: " + reason;
        } else {
            return "User " + username + " doesn't exist.";
        }
    }

    public void addTicket(String author, String message, int ticketID) throws SQLException {
        if (ticketID > -1) {
            PreparedStatement stat = this.conn.prepareStatement("INSERT INTO tickets (author, message, answeredID) VALUES (?, ?, ?)");
            stat.setString(1, author);
            stat.setString(2, message);
            stat.setInt(3, ticketID);
            stat.executeUpdate();
            stat.close();
        } else {
            PreparedStatement stat = this.conn.prepareStatement("INSERT INTO tickets (author, message) VALUES (?, ?)");
            stat.setString(1, author);
            stat.setString(2, message);
            stat.executeUpdate();
            stat.close();
        }
    }

    public void viewedTicket(int ticketID, boolean viewed) throws SQLException {
        if (ticketID > -1) {
            PreparedStatement stat = this.conn.prepareStatement("UPDATE tickets SET viewed = ? WHERE id = ?");
            stat.setBoolean(1, viewed);
            stat.setInt(2, ticketID);
            stat.executeUpdate();
            stat.close();
        }
    }

    public ArrayList<DBBannedIP> getBannedIPs() throws SQLException {
        ArrayList<DBBannedIP> result = new ArrayList<>();
        PreparedStatement stat = null;
        stat = this.conn.prepareStatement("SELECT * FROM bannedip");
        ResultSet rs = stat.executeQuery();

        while (rs.next()) {
            DBBannedIP bannedIP = new DBBannedIP(rs.getString("username"), rs.getString("ip"), rs.getString("reason"));
            result.add(bannedIP);
        }

        return result;
    }

    public ArrayList<DBBannedSteamID> getBannedSteamIDs() throws SQLException {
        ArrayList<DBBannedSteamID> result = new ArrayList<>();
        PreparedStatement stat = null;
        stat = this.conn.prepareStatement("SELECT * FROM bannedid");
        ResultSet rs = stat.executeQuery();

        while (rs.next()) {
            DBBannedSteamID bannedSteamID = new DBBannedSteamID(rs.getString("steamid"), rs.getString("reason"));
            result.add(bannedSteamID);
        }

        return result;
    }

    public ArrayList<DBTicket> getTickets(String playerName) throws SQLException {
        ArrayList<DBTicket> result = new ArrayList<>();
        PreparedStatement stat = null;
        if (playerName != null) {
            stat = this.conn.prepareStatement("SELECT * FROM tickets WHERE author = ? and answeredID is null");
            stat.setString(1, playerName);
        } else {
            stat = this.conn.prepareStatement("SELECT * FROM tickets where answeredID is null");
        }

        ResultSet rs = stat.executeQuery();

        while (rs.next()) {
            DBTicket ticket = new DBTicket(rs.getString("author"), rs.getString("message"), rs.getInt("id"), rs.getBoolean("viewed"));
            result.add(ticket);
            DBTicket answer = this.getAnswer(ticket.getTicketID());
            if (answer != null) {
                ticket.setAnswer(answer);
            }
        }

        return result;
    }

    private DBTicket getAnswer(int ticketID) throws SQLException {
        PreparedStatement stat = null;
        stat = this.conn.prepareStatement("SELECT * FROM tickets WHERE answeredID = ?");
        stat.setInt(1, ticketID);
        ResultSet rs = stat.executeQuery();
        return rs.next() ? new DBTicket(rs.getString("author"), rs.getString("message"), rs.getInt("id")) : null;
    }

    public void removeTicket(int ticketID) throws SQLException {
        DBTicket answer = this.getAnswer(ticketID);
        if (answer != null) {
            PreparedStatement stat = this.conn.prepareStatement("DELETE FROM tickets WHERE id = ?");
            stat.setInt(1, answer.getTicketID());
            stat.executeUpdate();
            stat.close();
        }

        PreparedStatement stat = this.conn.prepareStatement("DELETE FROM tickets WHERE id = ?");
        stat.setInt(1, ticketID);
        stat.executeUpdate();
        stat.close();
    }

    public class LogonResult {
        public boolean authorized;
        public boolean needSecondFactor;
        public int x;
        public int y;
        public int z;
        public String bannedReason;
        public String dcReason;
        public Role role;

        public LogonResult() {
            Objects.requireNonNull(ServerWorldDatabase.this);
            super();
            this.role = Roles.getDefaultForNewUser();
        }
    }
}
