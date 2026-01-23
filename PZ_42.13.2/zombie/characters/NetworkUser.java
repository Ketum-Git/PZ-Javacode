// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
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
    public int warningPoints;
    public int suspicionPoints;
    public int kicks;

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
    }

    public String getFirstBannedIPForUser(String _username) {
        return ServerWorldDatabase.instance.getFirstBannedIPForUser(_username);
    }

    public String isSteamIdBanned(String _steamid) {
        return ServerWorldDatabase.instance.isSteamIdBanned(_steamid);
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

    public void send(ByteBuffer output) {
        GameWindow.WriteStringUTF(output, this.world);
        GameWindow.WriteStringUTF(output, this.username);
        GameWindow.WriteStringUTF(output, this.lastConnection);
        GameWindow.WriteStringUTF(output, this.role.getName());
        output.put((byte)this.authType.ordinal());
        GameWindow.WriteStringUTF(output, this.steamid);
        GameWindow.WriteStringUTF(output, this.displayName);
        output.put((byte)(this.online ? 1 : 0));
        output.putInt(this.warningPoints);
        output.putInt(this.suspicionPoints);
        output.putInt(this.kicks);
        output.put((byte)(this.inWhitelist ? 1 : 0));
        GameWindow.WriteStringUTF(output, this.ipBanned);
        GameWindow.WriteStringUTF(output, this.steamIdBanned);
    }

    public void parse(ByteBuffer input) {
        this.world = GameWindow.ReadString(input);
        this.username = GameWindow.ReadString(input);
        this.lastConnection = GameWindow.ReadString(input);
        String roleName = GameWindow.ReadString(input);
        this.role = Roles.getRole(roleName);
        this.authType = NetworkUser.AuthType.values()[input.get()];
        this.steamid = GameWindow.ReadString(input);
        this.displayName = GameWindow.ReadString(input);
        this.online = input.get() > 0;
        this.warningPoints = input.getInt();
        this.suspicionPoints = input.getInt();
        this.kicks = input.getInt();
        this.inWhitelist = input.get() > 0;
        this.ipBanned = GameWindow.ReadString(input);
        this.steamIdBanned = GameWindow.ReadString(input);
    }

    public static enum AuthType {
        password,
        google_auth,
        two_factor;
    }
}
