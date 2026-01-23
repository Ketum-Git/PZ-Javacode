// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.UsedFromLua;

@UsedFromLua
public class DBBannedIP {
    private String username;
    private String ip = "";
    private String reason = "";

    public DBBannedIP(String username, String ip, String reason) {
        this.username = username;
        this.ip = ip;
        this.reason = reason;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
