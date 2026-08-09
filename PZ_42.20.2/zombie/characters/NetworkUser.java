// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerWorldDatabase;

@UsedFromLua
public class NetworkUser {
    public boolean inWhitelist;
    public String ipBanned;
    public String steamIdBanned;
    public String world;
    public String username;
    public String lastConnection;
    public Role role;
    public NetworkUser.AuthType authType;
    public String steamid;
    public String displayName;
    public boolean online;
    private UdpConnection.ConnectionType connectionType = UdpConnection.ConnectionType.Disconnected;
    public int warningPoints;
    public int suspicionPoints;
    public int kicks;
    public short ping;

    public NetworkUser() {
    }

    public NetworkUser(String world, String username, String lastConnection, Role role, int authType, String steamid, String displayName, boolean online) {
        this.inWhitelist = false;
        this.world = world;
        this.username = username;
        this.lastConnection = lastConnection;
        this.role = role;
        switch (authType) {
            case 1:
                this.authType = NetworkUser.AuthType.password;
                break;
            case 2:
                this.authType = NetworkUser.AuthType.google_auth;
                break;
            case 3:
                this.authType = NetworkUser.AuthType.two_factor;
        }

        this.steamid = steamid;
        this.displayName = displayName;
        this.online = online;
        this.warningPoints = 0;
        this.suspicionPoints = 0;
        this.kicks = 0;
        this.ipBanned = this.getFirstBannedIPForUser(username);
        this.steamIdBanned = this.isSteamIdBanned(steamid);
        this.ping = -1;
    }

    public String getFirstBannedIPForUser(String username) {
        return ServerWorldDatabase.instance.getFirstBannedIPForUser(username);
    }

    public String isSteamIdBanned(String steamId) {
        return ServerWorldDatabase.instance.isSteamIdBanned(steamId);
    }

    public String getSteamIdBanned() {
        return this.steamIdBanned;
    }

    public String getIpBanned() {
        return this.ipBanned;
    }

    public String getWorld() {
        return this.world;
    }

    public String getUsername() {
        return this.username;
    }

    public String getLastConnection() {
        return this.lastConnection;
    }

    public Role getRole() {
        return this.role;
    }

    public NetworkUser.AuthType getAuthType() {
        return this.authType;
    }

    public String getAuthTypeName() {
        return this.authType == null ? "-" : this.authType.name();
    }

    public String getSteamid() {
        return this.steamid;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isOnline() {
        return this.online;
    }

    public UdpConnection.ConnectionType getConnectionType() {
        return this.connectionType;
    }

    public void setConnectionType(UdpConnection.ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public boolean isConnectedDirectly() {
        return this.online && this.connectionType == UdpConnection.ConnectionType.UDPRakNet;
    }

    public void setWarningPoints(int warningPoints) {
        this.warningPoints = warningPoints;
    }

    public int getWarningPoints() {
        return this.warningPoints;
    }

    public void setSuspicionPoints(int suspicionPoints) {
        this.suspicionPoints = suspicionPoints;
    }

    public int getSuspicionPoints() {
        return this.suspicionPoints;
    }

    public void setKicks(int kicks) {
        this.kicks = kicks;
    }

    public int getKicks() {
        return this.kicks;
    }

    public void setInWhitelist(boolean inWhitelist) {
        this.inWhitelist = inWhitelist;
    }

    public boolean isInWhitelist() {
        return this.inWhitelist;
    }

    public void setPing(short ping) {
        this.ping = ping;
    }

    public short getPing() {
        return this.ping;
    }

    public void send(ByteBufferWriter output) {
        output.putUTF(this.world);
        output.putUTF(this.username);
        output.putUTF(this.lastConnection);
        output.putUTF(this.role.getName());
        output.putEnum(this.authType);
        output.putUTF(this.steamid);
        output.putUTF(this.displayName);
        output.putBoolean(this.online);
        output.putEnum(this.connectionType != null ? this.connectionType : UdpConnection.ConnectionType.Disconnected);
        output.putInt(this.warningPoints);
        output.putInt(this.suspicionPoints);
        output.putInt(this.kicks);
        output.putBoolean(this.inWhitelist);
        output.putUTF(this.ipBanned);
        output.putUTF(this.steamIdBanned);
        output.putShort(this.ping);
    }

    public void parse(ByteBufferReader input) {
        this.world = input.getUTF();
        this.username = input.getUTF();
        this.lastConnection = input.getUTF();
        String roleName = input.getUTF();
        this.role = Roles.getRole(roleName);
        this.authType = input.getEnum(NetworkUser.AuthType.class);
        this.steamid = input.getUTF();
        this.displayName = input.getUTF();
        this.online = input.getBoolean();
        this.connectionType = input.getEnum(UdpConnection.ConnectionType.class);
        this.warningPoints = input.getInt();
        this.suspicionPoints = input.getInt();
        this.kicks = input.getInt();
        this.inWhitelist = input.getBoolean();
        this.ipBanned = input.getUTF();
        this.steamIdBanned = input.getUTF();
        this.ping = input.getShort();
    }

    public static enum AuthType {
        password,
        google_auth,
        two_factor;
    }
}
