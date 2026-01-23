// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionMain;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.ConnectToServerState;
import zombie.gameStates.MainScreenState;
import zombie.globalObjects.SGlobalObjects;
import zombie.world.WorldDictionary;

public class ConnectionDetails {
    public static void write(UdpConnection connection, ServerWorldDatabase.LogonResult logonResult, ByteBuffer bb) {
        try {
            writeServerDetails(bb, connection, logonResult);
            writeGameMap(bb);
            if (SteamUtils.isSteamModeEnabled()) {
                writeWorkshopItems(bb);
            }

            writeMods(bb);
            writeStartLocation(bb);
            writeServerOptions(bb);
            writeSandboxOptions(bb);
            writeGameTime(bb);
            writeErosionMain(bb);
            writeGlobalObjects(bb);
            writeResetID(bb);
            writeBerries(bb);
            writeWorldDictionary(bb);
        } catch (Throwable var4) {
            throw new RuntimeException(var4);
        }
    }

    public static void parse(ByteBuffer b) {
        ConnectionManager.log("receive-packet", "connection-details", GameClient.connection);
        Calendar endAuth = Calendar.getInstance();
        ConnectToServerState ctss = new ConnectToServerState(b);
        ctss.enter();
        MainScreenState.getInstance().setConnectToServerState(ctss);
        DebugLog.General.println("LOGGED INTO : %d millisecond", endAuth.getTimeInMillis() - GameClient.startAuth.getTimeInMillis());
    }

    private static void writeServerDetails(ByteBuffer b, UdpConnection connection, ServerWorldDatabase.LogonResult logonResult) {
        b.put((byte)(connection.isCoopHost ? 1 : 0));
        b.putInt(ServerOptions.getInstance().getMaxPlayers());
        if (SteamUtils.isSteamModeEnabled() && CoopSlave.instance != null && !connection.isCoopHost) {
            b.put((byte)1);
            b.putLong(CoopSlave.instance.hostSteamId);
            GameWindow.WriteString(b, GameServer.serverName);
        } else {
            b.put((byte)0);
        }

        int slot = connection.playerIds[0] / 4;
        b.put((byte)slot);
        logonResult.role.send(b);
    }

    private static void writeGameMap(ByteBuffer b) {
        GameWindow.WriteString(b, GameServer.gameMap);
    }

    private static void writeWorkshopItems(ByteBuffer b) {
        b.putShort((short)GameServer.WorkshopItems.size());

        for (int i = 0; i < GameServer.WorkshopItems.size(); i++) {
            b.putLong(GameServer.WorkshopItems.get(i));
            b.putLong(GameServer.workshopTimeStamps[i]);
        }
    }

    private static void writeMods(ByteBuffer b) {
        ArrayList<ChooseGameInfo.Mod> mods = new ArrayList<>();
        ArrayList<String> missing_mods = new ArrayList<>();

        for (String modId : GameServer.ServerMods) {
            String modDir = ZomboidFileSystem.instance.getModDir(modId);
            ChooseGameInfo.Mod mod;
            if (modDir != null) {
                try {
                    mod = ChooseGameInfo.readModInfo(modDir);
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                    missing_mods.add(modId);
                    mod = null;
                }
            } else {
                missing_mods.add(modId);
                mod = null;
            }

            if (mod != null) {
                mods.add(mod);
            }
        }

        b.putInt(mods.size() + missing_mods.size());

        for (ChooseGameInfo.Mod modF : mods) {
            GameWindow.WriteString(b, modF.getId());
            GameWindow.WriteString(b, modF.getUrl());
            GameWindow.WriteString(b, modF.getName());
        }

        for (String modF : missing_mods) {
            GameWindow.WriteString(b, modF);
            GameWindow.WriteString(b, "");
            GameWindow.WriteString(b, modF);
        }
    }

    private static void writeStartLocation(ByteBuffer b) {
        ServerWorldDatabase.LogonResult r = null;
        b.putInt(10745);
        b.putInt(9412);
        b.putInt(0);
    }

    private static void writeServerOptions(ByteBuffer b) {
        b.putInt(ServerOptions.instance.getPublicOptions().size());

        for (String key : ServerOptions.instance.getPublicOptions()) {
            GameWindow.WriteString(b, key);
            GameWindow.WriteString(b, ServerOptions.instance.getOption(key));
        }
    }

    private static void writeSandboxOptions(ByteBuffer b) throws IOException {
        SandboxOptions.instance.save(b);
    }

    private static void writeGameTime(ByteBuffer b) throws IOException {
        GameTime.getInstance().saveToPacket(b);
    }

    private static void writeErosionMain(ByteBuffer b) {
        ErosionMain.getInstance().getConfig().save(b);
    }

    private static void writeGlobalObjects(ByteBuffer b) throws IOException {
        SGlobalObjects.saveInitialStateForClient(b);
    }

    private static void writeResetID(ByteBuffer b) {
        b.putInt(GameServer.resetId);
    }

    private static void writeBerries(ByteBuffer b) {
        GameWindow.WriteString(b, Core.getInstance().getPoisonousBerry());
        GameWindow.WriteString(b, Core.getInstance().getPoisonousMushroom());
    }

    private static void writeWorldDictionary(ByteBuffer b) throws IOException {
        WorldDictionary.saveDataForClient(b);
    }
}
