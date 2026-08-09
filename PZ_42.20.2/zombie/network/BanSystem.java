// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkUser;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugType;

public class BanSystem {
    private static final boolean unbanAllUsersWithSteamID = false;

    public static String BanUser(String username, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
            return "You don't have capability to ban/unban users.";
        } else {
            Role role = ServerWorldDatabase.instance.getUserRoleNameByUsername(username);
            if (role != null && role.hasCapability(Capability.CantBeBannedByUser)) {
                return "This user can't be banned.";
            } else {
                String usernameAdmin;
                if (adminConnection != null) {
                    usernameAdmin = adminConnection.getUserName();
                } else {
                    usernameAdmin = "System";
                }

                ServerWorldDatabase.instance.banUser(username, ban);
                String message = usernameAdmin + (ban ? " banned" : " unbanned") + " user " + username;
                ServerWorldDatabase.instance.addUserlog(username, Userlog.UserlogType.Banned, argument, usernameAdmin, 1);
                if (ban) {
                    LoggerManager.getLogger("admin").write(usernameAdmin + " banned user " + username + (argument != null ? argument : ""), "IMPORTANT");
                    KickUser(username, "You were banned", "command-banid");
                } else {
                    if (SteamUtils.isSteamModeEnabled()) {
                        String steamID = "";
                        HashMap<String, NetworkUser> users = new HashMap<>();
                        ServerWorldDatabase.instance.getWhitelistUsers(users);
                        if (users.containsKey(username)) {
                            steamID = users.get(username).steamid;
                        }

                        ServerWorldDatabase.instance.banSteamID(steamID, argument, false);
                    } else {
                        String ip = ServerWorldDatabase.instance.getFirstBannedIPForUser(username);
                        ServerWorldDatabase.instance.banIp(ip, username, argument, false);
                    }

                    LoggerManager.getLogger("admin").write(message, "IMPORTANT");
                    DebugType.Multiplayer.println(message);
                }

                return message;
            }
        }
    }

    public static void KickUser(String username, String reason, String description) {
        IsoPlayer pl = GameServer.getPlayerByUserName(username);
        if (pl != null) {
            UdpConnection c = GameServer.getConnectionFromPlayer(pl);
            if (c != null) {
                GameServer.kick(c, reason, null);
                c.forceDisconnect(description);
            }
        }
    }

    public static String BanUserBySteamID(String steamID, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        String message = "";
        if (SteamUtils.isSteamModeEnabled()) {
            if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
                return "You don't have capability to ban/unban users.";
            }

            String usernameAdmin;
            if (adminConnection != null) {
                usernameAdmin = adminConnection.getUserName();
            } else {
                usernameAdmin = "System";
            }

            ServerWorldDatabase.instance.banSteamID(steamID, argument, ban);
            message = usernameAdmin + (ban ? " banned" : " unbanned") + " SteamID " + steamID + "(";
            HashMap<String, NetworkUser> users = new HashMap<>();
            ServerWorldDatabase.instance.getWhitelistUsers(users);

            for (Entry<String, NetworkUser> user : users.entrySet()) {
                String userSteamID = user.getValue().getSteamid();
                if (userSteamID != null && userSteamID.equals(steamID)) {
                    String username = user.getKey();
                    if (!username.equals(usernameAdmin) || usernameAdmin.equals("System")) {
                        ServerWorldDatabase.instance.banUser(username, ban);
                        message = message + username + ", ";
                        if (ban) {
                            KickUser(username, "You were banned", "command-banid");
                        }
                    }
                }
            }

            message = message + ")" + (argument != null ? argument : "");
            LoggerManager.getLogger("admin").write(message, "IMPORTANT");
            DebugType.Multiplayer.println(message);
        }

        return message;
    }

    public static String BanIP(String ip, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
            return "You don't have capability to ban/unban users.";
        } else {
            String usernameAdmin;
            if (adminConnection != null) {
                usernameAdmin = adminConnection.getUserName();
            } else {
                usernameAdmin = "System";
            }

            if (ban && isIpUsedBySteamRelayConnection(ip)) {
                return "Cannot ban IP " + ip + " (Steam Relay shared address). Use bansteamid or banuser instead.";
            } else {
                String connectionTypeNote = formatConnectionTypeNoteForIp(ip);
                String message = usernameAdmin + (ban ? " banned" : " unbanned") + " IP " + ip + connectionTypeNote + (argument != null ? argument : "");
                LoggerManager.getLogger("admin").write(message, "IMPORTANT");
                ServerWorldDatabase.instance.banIp(ip, "", argument, ban);
                return message;
            }
        }
    }

    private static boolean isIpUsedBySteamRelayConnection(String ip) {
        UdpConnection c = GameServer.getConnectionByIp(ip);
        return c != null && c.getConnectionType() == UdpConnection.ConnectionType.Steam;
    }

    private static String formatConnectionTypeNoteForIp(String ip) {
        UdpConnection c = GameServer.getConnectionByIp(ip);
        return c == null ? "" : " (connection-type=" + c.getConnectionType().name() + ")";
    }

    public static String BanUserByIP(String username, UdpConnection adminConnection, String argument, boolean ban) throws SQLException {
        if (adminConnection != null && !adminConnection.players[0].getRole().hasCapability(Capability.BanUnbanUser)) {
            return "You don't have capability to ban/unban users.";
        } else {
            String usernameAdmin;
            if (adminConnection != null) {
                usernameAdmin = adminConnection.getUserName();
            } else {
                usernameAdmin = "System";
            }

            String message = "";
            if (ban) {
                IsoPlayer pl = GameServer.getPlayerByUserName(username);
                if (pl != null) {
                    UdpConnection c = GameServer.getConnectionFromPlayer(pl);
                    if (c != null) {
                        if (c.getConnectionType() == UdpConnection.ConnectionType.Steam) {
                            return "Cannot ban IP for player '" + username + "' (Steam Relay, real IP unavailable). Use bansteamid or banuser without -ip.";
                        }

                        LoggerManager.getLogger("admin")
                            .write(
                                usernameAdmin
                                    + " banned IP "
                                    + c.getIP()
                                    + " ("
                                    + c.getUserName()
                                    + ", connection-type="
                                    + c.getConnectionType().name()
                                    + ")"
                                    + (argument != null ? argument : ""),
                                "IMPORTANT"
                            );
                        ServerWorldDatabase.instance.banIp(c.getIP(), username, argument, true);
                        if (pl.getRole() != Roles.getDefaultForBanned()) {
                            message = BanUser(username, adminConnection, argument, true);
                        }
                    } else {
                        DebugType.Multiplayer.println("Connection not found");
                    }
                } else {
                    DebugType.Multiplayer.println("Player not found");
                }
            } else {
                LoggerManager.getLogger("admin").write(usernameAdmin + " unbanned IP (" + username + ")" + (argument != null ? argument : ""), "IMPORTANT");
                String ip = ServerWorldDatabase.instance.getFirstBannedIPForUser(username);
                ServerWorldDatabase.instance.banIp(ip, username, argument, false);
                message = BanUser(username, adminConnection, argument, false);
            }

            return message;
        }
    }
}
