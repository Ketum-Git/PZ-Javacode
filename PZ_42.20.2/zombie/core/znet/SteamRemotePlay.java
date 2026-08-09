// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

public class SteamRemotePlay {
    private static native int n_GetSessionCount();

    public static int GetSessionCount() {
        return SteamUtils.isSteamModeEnabled() ? n_GetSessionCount() : 0;
    }
}
