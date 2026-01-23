// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import zombie.Lua.LuaEventManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.CoopSlave;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;

public class SteamUtils {
    private static boolean steamEnabled;
    private static boolean netEnabled;
    private static boolean floatingGamepadTextInputVisible;
    private static final BigInteger TWO_64 = BigInteger.ONE.shiftLeft(64);
    private static final BigInteger MAX_ULONG = new BigInteger("FFFFFFFFFFFFFFFF", 16);
    private static List<IJoinRequestCallback> joinRequestCallbacks;
    public static final int k_EGamepadTextInputModeNormal = 0;
    public static final int k_EGamepadTextInputModePassword = 1;
    public static final int k_EGamepadTextInputLineModeSingleLine = 0;
    public static final int k_EGamepadTextInputLineModeMultipleLines = 1;
    public static final int k_EFloatingGamepadTextInputModeSingleLine = 0;
    public static final int k_EFloatingGamepadTextInputModeMultipleLines = 1;
    public static final int k_EFloatingGamepadTextInputModeEmail = 2;
    public static final int k_EFloatingGamepadTextInputModeNumeric = 3;

    private static void loadLibrary(String name) {
        DebugLog.log("Loading " + name + "...");
        System.loadLibrary(name);
    }

    public static void init() {
        steamEnabled = System.getProperty("zomboid.steam") != null && System.getProperty("zomboid.steam").equals("1");
        DebugLog.log("Loading networking libraries...");
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.znet"))) {
            DebugLog.log("***** Loading debug versions of libraries");
            libSuffix = "d";
        }

        try {
            if (System.getProperty("os.name").contains("OS X")) {
                if (steamEnabled) {
                    loadLibrary("steam_api");
                    loadLibrary("RakNet");
                    loadLibrary("ZNetJNI");
                } else {
                    loadLibrary("RakNet");
                    loadLibrary("ZNetNoSteam");
                }
            } else if (System.getProperty("os.name").startsWith("Win")) {
                if (steamEnabled) {
                    loadLibrary("steam_api64");
                    loadLibrary("RakNet64" + libSuffix);
                    loadLibrary("ZNetJNI64" + libSuffix);
                } else {
                    loadLibrary("RakNet64" + libSuffix);
                    loadLibrary("ZNetNoSteam64" + libSuffix);
                }
            } else if (steamEnabled) {
                loadLibrary("steam_api");
                loadLibrary("RakNet64");
                loadLibrary("ZNetJNI64");
            } else {
                loadLibrary("RakNet64");
                loadLibrary("ZNetNoSteam64");
            }

            netEnabled = true;
        } catch (UnsatisfiedLinkError var6) {
            steamEnabled = false;
            netEnabled = false;
            ExceptionLogger.logException(var6);
            if (System.getProperty("os.name").startsWith("Win")) {
                DebugLog.log("One of the game's DLLs could not be loaded.");
                DebugLog.log("  Your system may be missing a DLL needed by the game's DLL.");
                DebugLog.log("  You may need to install the Microsoft Visual C++ Redistributable 2013.");
                File file = new File("../_CommonRedist/vcredist/");
                if (file.exists()) {
                    DebugLog.DetailedInfo.trace("  This file is provided in " + file.getAbsolutePath());
                }
            }
        }

        String logLevelStr = System.getProperty("zomboid.znetlog");
        if (netEnabled && logLevelStr != null) {
            try {
                int logLevel = Integer.parseInt(logLevelStr);
                ZNet.SetLogLevel(logLevel);
            } catch (NumberFormatException var5) {
                ExceptionLogger.logException(var5);
            }
        }

        if (!netEnabled) {
            DebugLog.log("Failed to load networking libraries");
        } else {
            ZNet.init();
            ZNet.SetLogLevel(DebugLog.getLogLevel(DebugType.Network));
            synchronized (RenderThread.m_contextLock) {
                if (!steamEnabled) {
                    DebugLog.log("SteamUtils started without Steam");
                } else if (n_Init(GameServer.server)) {
                    DebugLog.log("SteamUtils initialised successfully");
                } else {
                    DebugLog.log("Could not initialise SteamUtils");
                    steamEnabled = false;
                }
            }
        }

        joinRequestCallbacks = new ArrayList<>();
    }

    public static void shutdown() {
        if (steamEnabled) {
            n_Shutdown();
        }
    }

    public static void runLoop() {
        if (steamEnabled) {
            n_RunLoop();
        }
    }

    public static boolean isSteamModeEnabled() {
        return steamEnabled;
    }

    public static boolean isOverlayEnabled() {
        return steamEnabled && n_IsOverlayEnabled();
    }

    public static String convertSteamIDToString(long steamID) {
        BigInteger b = BigInteger.valueOf(steamID);
        if (b.signum() < 0) {
            b.add(TWO_64);
        }

        return b.toString();
    }

    public static boolean isValidSteamID(String s) {
        try {
            BigInteger b = new BigInteger(s);
            return b.signum() >= 0 && b.compareTo(MAX_ULONG) <= 0;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    public static long convertStringToSteamID(String s) {
        try {
            BigInteger b = new BigInteger(s);
            return b.signum() >= 0 && b.compareTo(MAX_ULONG) <= 0 ? b.longValue() : -1L;
        } catch (NumberFormatException var2) {
            return -1L;
        }
    }

    public static void addJoinRequestCallback(IJoinRequestCallback callback) {
        joinRequestCallbacks.add(callback);
    }

    public static void removeJoinRequestCallback(IJoinRequestCallback callback) {
        joinRequestCallbacks.remove(callback);
    }

    public static boolean isRunningOnSteamDeck() {
        return n_IsSteamRunningOnSteamDeck();
    }

    public static boolean showGamepadTextInput(boolean password, boolean multipleLines, String description, int maxChars, String existingText) {
        return n_ShowGamepadTextInput(password ? 1 : 0, multipleLines ? 1 : 0, description, maxChars, existingText);
    }

    public static boolean showFloatingGamepadTextInput(boolean multipleLines, int x, int y, int width, int height) {
        if (floatingGamepadTextInputVisible) {
            return true;
        } else {
            floatingGamepadTextInputVisible = n_ShowFloatingGamepadTextInput(multipleLines ? 1 : 0, x, y, width, height);
            return floatingGamepadTextInputVisible;
        }
    }

    public static boolean isFloatingGamepadTextInputVisible() {
        return floatingGamepadTextInputVisible;
    }

    private static native boolean n_Init(boolean var0);

    private static native void n_Shutdown();

    private static native void n_RunLoop();

    private static native boolean n_IsOverlayEnabled();

    private static native boolean n_IsSteamRunningOnSteamDeck();

    private static native boolean n_ShowGamepadTextInput(int var0, int var1, String var2, int var3, String var4);

    private static native boolean n_ShowFloatingGamepadTextInput(int var0, int var1, int var2, int var3, int var4);

    private static void joinRequestCallback(long friendSteamID, String connectionString) {
        DebugLog.log("Got Join Request");

        for (IJoinRequestCallback callback : joinRequestCallbacks) {
            callback.onJoinRequest(friendSteamID, connectionString);
        }

        if (connectionString.contains("+connect ")) {
            String connect = connectionString.substring(9);
            System.setProperty("args.server.connect", connect);
            LuaEventManager.triggerEvent("OnSteamGameJoin");
        }
    }

    private static int clientInitiateConnectionCallback(long steamID) {
        if (CoopSlave.instance == null) {
            ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.authClient(steamID);
            return r.authorized ? 0 : 1;
        } else {
            return !CoopSlave.instance.isHost(steamID) && !CoopSlave.instance.isInvited(steamID) ? 2 : 0;
        }
    }

    private static int validateOwnerCallback(long steamID, long ownerID) {
        if (CoopSlave.instance != null) {
            return 0;
        } else {
            ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.authOwner(steamID, ownerID);
            return r.authorized ? 0 : 1;
        }
    }

    private static void gamepadTextInputDismissedCallback(String text) {
        if (text == null) {
            DebugLog.log("null");
        } else {
            DebugLog.log(text);
        }
    }

    private static void floatingGamepadTextInputDismissedCallback() {
        floatingGamepadTextInputVisible = false;
    }
}
