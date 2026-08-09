// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.core.textures.Texture;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class SteamFriends {
    public static final int k_EPersonaStateOffline = 0;
    public static final int k_EPersonaStateOnline = 1;
    public static final int k_EPersonaStateBusy = 2;
    public static final int k_EPersonaStateAway = 3;
    public static final int k_EPersonaStateSnooze = 4;
    public static final int k_EPersonaStateLookingToTrade = 5;
    public static final int k_EPersonaStateLookingToPlay = 6;

    public static void init() {
        if (SteamUtils.isSteamModeEnabled()) {
            n_Init();
        }
    }

    public static void shutdown() {
        if (SteamUtils.isSteamModeEnabled()) {
            n_Shutdown();
        }
    }

    public static native void n_Init();

    public static native void n_Shutdown();

    public static native String GetPersonaName();

    public static native int GetFriendCount();

    public static native long GetFriendByIndex(int var0);

    public static native String GetFriendPersonaName(long var0);

    public static native int GetFriendPersonaState(long var0);

    public static native boolean InviteUserToGame(long var0, String var2);

    public static native void ActivateGameOverlay(String var0);

    public static native void ActivateGameOverlayToUser(String var0, long var1);

    public static native void ActivateGameOverlayToWebPage(String var0);

    public static native void SetPlayedWith(long var0);

    public static native void UpdateRichPresenceConnectionInfo(String var0, String var1);

    public static List<SteamFriend> GetFriendList() {
        List<SteamFriend> result = new ArrayList<>();
        int friendCount = GetFriendCount();

        for (int i = 0; i < friendCount; i++) {
            long steamID = GetFriendByIndex(i);
            String name = GetFriendPersonaName(steamID);
            result.add(new SteamFriend(name, steamID));
        }

        return result;
    }

    public static native int CreateSteamAvatar(long var0, ByteBuffer var2);

    private static void onStatusChangedCallback(long steamID) {
        if (GameClient.client || GameServer.server) {
            LuaEventManager.triggerEvent("OnSteamFriendStatusChanged", Long.toString(steamID));
        }
    }

    private static void onAvatarChangedCallback(long steamID) {
        Texture.steamAvatarChanged(steamID);
    }

    private static void onProfileNameChanged(long steamID) {
    }
}
