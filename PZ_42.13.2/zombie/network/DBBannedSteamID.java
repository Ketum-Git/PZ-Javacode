// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.UsedFromLua;

@UsedFromLua
public class DBBannedSteamID {
    private String steamid = "";
    private String reason = "";

    public DBBannedSteamID(String steamid, String reason) {
        this.steamid = steamid;
        this.reason = reason;
    }

    public String getSteamID() {
        return this.steamid;
    }

    public void setSteamID(String steamid) {
        this.steamid = steamid;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
