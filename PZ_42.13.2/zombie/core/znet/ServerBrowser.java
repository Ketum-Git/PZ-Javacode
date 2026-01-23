// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.network.GameServer;
import zombie.network.Server;

public class ServerBrowser {
    private static boolean suppressLuaCallbacks;
    private static IServerBrowserCallback callbackInterface;

    public static boolean init() {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_Init();
        }

        return result;
    }

    public static void shutdown() {
        if (SteamUtils.isSteamModeEnabled()) {
            n_Shutdown();
        }
    }

    public static void RefreshInternetServers() {
        if (SteamUtils.isSteamModeEnabled()) {
            n_RefreshInternetServers();
        }
    }

    public static int GetServerCount() {
        int result = 0;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_GetServerCount();
        }

        return result;
    }

    public static GameServerDetails GetServerDetails(int serverIndex) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_GetServerDetails(serverIndex);
        }

        return result;
    }

    public static void Release() {
        if (SteamUtils.isSteamModeEnabled()) {
            n_Release();
        }
    }

    public static boolean IsRefreshing() {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_IsRefreshing();
        }

        return result;
    }

    public static boolean QueryServer(String host, int port) {
        boolean result = false;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_QueryServer(host, port);
        }

        return result;
    }

    public static GameServerDetails GetServerDetails(String host, int port) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled()) {
            result = n_GetServerDetails(host, port);
        }

        return result;
    }

    public static void ReleaseServerQuery(String host, int port) {
        if (SteamUtils.isSteamModeEnabled()) {
            n_ReleaseServerQuery(host, port);
        }
    }

    public static List<GameServerDetails> GetServerList() {
        List<GameServerDetails> result = new ArrayList<>();
        if (SteamUtils.isSteamModeEnabled()) {
            try {
                while (IsRefreshing()) {
                    Thread.sleep(100L);
                    SteamUtils.runLoop();
                }
            } catch (InterruptedException var3) {
                var3.printStackTrace();
            }

            for (int i = 0; i < GetServerCount(); i++) {
                GameServerDetails details = GetServerDetails(i);
                if (details.steamId != 0L) {
                    result.add(details);
                }
            }
        }

        return result;
    }

    public static GameServerDetails GetServerDetailsSync(String host, int port) {
        GameServerDetails result = null;
        if (SteamUtils.isSteamModeEnabled()) {
            result = GetServerDetails(host, port);
            if (result == null) {
                QueryServer(host, port);

                try {
                    while (result == null) {
                        Thread.sleep(100L);
                        SteamUtils.runLoop();
                        result = GetServerDetails(host, port);
                    }
                } catch (InterruptedException var4) {
                    var4.printStackTrace();
                }
            }
        }

        return result;
    }

    public static boolean RequestServerRules(String host, int port) {
        return n_RequestServerRules(host, port);
    }

    public static void setSuppressLuaCallbacks(boolean bSupress) {
        suppressLuaCallbacks = bSupress;
    }

    public static void setCallbackInterface(IServerBrowserCallback callbackInterface) {
        ServerBrowser.callbackInterface = callbackInterface;
    }

    private static native boolean n_Init();

    private static native void n_Shutdown();

    private static native void n_RefreshInternetServers();

    private static native int n_GetServerCount();

    private static native GameServerDetails n_GetServerDetails(int var0);

    private static native void n_Release();

    private static native boolean n_IsRefreshing();

    private static native boolean n_QueryServer(String var0, int var1);

    private static native GameServerDetails n_GetServerDetails(String var0, int var1);

    private static native void n_ReleaseServerQuery(String var0, int var1);

    private static native boolean n_RequestServerRules(String var0, int var1);

    private static void onServerRespondedCallback(int serverIndex) {
        if (callbackInterface != null) {
            callbackInterface.OnServerResponded(serverIndex);
        }

        if (!suppressLuaCallbacks) {
            LuaEventManager.triggerEvent("OnSteamServerResponded", serverIndex);
        }
    }

    private static void onServerFailedToRespondCallback(int serverIndex) {
        if (callbackInterface != null) {
            callbackInterface.OnServerFailedToRespond(serverIndex);
        }
    }

    private static void onRefreshCompleteCallback() {
        if (callbackInterface != null) {
            callbackInterface.OnRefreshComplete();
        }

        if (!suppressLuaCallbacks) {
            LuaEventManager.triggerEvent("OnSteamRefreshInternetServers");
        }
    }

    private static void onServerRespondedCallback(String host, int port) {
        if (callbackInterface != null) {
            callbackInterface.OnServerResponded(host, port);
        }

        GameServerDetails details = GetServerDetails(host, port);
        if (details != null) {
            Server newServer = GameServer.steamGetInternetServerDetails(details);
            ReleaseServerQuery(host, port);
            if (!suppressLuaCallbacks) {
                LuaEventManager.triggerEvent("OnSteamServerResponded2", host, (double)port, newServer);
            }
        }
    }

    private static void onServerFailedToRespondCallback(String host, int port) {
        if (callbackInterface != null) {
            callbackInterface.OnServerFailedToRespond(host, port);
        }

        if (!suppressLuaCallbacks) {
            LuaEventManager.triggerEvent("OnSteamServerFailedToRespond2", host, (double)port);
        }
    }

    private static void onRulesRefreshComplete(String host, int port, String[] rulesArray) {
        if (callbackInterface != null) {
            callbackInterface.OnSteamRulesRefreshComplete(host, port);
        }

        KahluaTable rulesTable = LuaManager.platform.newTable();

        for (int i = 0; i < rulesArray.length; i += 2) {
            rulesTable.rawset(rulesArray[i], rulesArray[i + 1]);
        }

        if (!suppressLuaCallbacks) {
            LuaEventManager.triggerEvent("OnSteamRulesRefreshComplete", host, (double)port, rulesTable);
        }
    }
}
