// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

public class SteamUser {
    public static long GetSteamID() {
        return SteamUtils.isSteamModeEnabled() ? n_GetSteamID() : 0L;
    }

    public static String GetSteamIDString() {
        if (SteamUtils.isSteamModeEnabled()) {
            long id = n_GetSteamID();
            return SteamUtils.convertSteamIDToString(id);
        } else {
            return null;
        }
    }

    private static native long n_GetSteamID();
}
