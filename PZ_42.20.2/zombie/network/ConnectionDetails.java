// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugType;
import zombie.erosion.ErosionMain;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.ConnectToServerState;
import zombie.gameStates.MainScreenState;
import zombie.globalObjects.SGlobalObjects;
import zombie.world.WorldDictionary;

public class ConnectionDetails {
    public static void write(UdpConnection connection, ServerWorldDatabase.LogonResult logonResult, ByteBufferWriter bb) {
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

    public static void parse(ByteBufferReader b) {
        ConnectionManager.log("receive-packet", "connection-details", GameClient.connection);
        Calendar endAuth = Calendar.getInstance();
        ConnectToServerState ctss = new ConnectToServerState(b);
        ctss.enter();
        MainScreenState.getInstance().setConnectToServerState(ctss);
        DebugType.General.println("LOGGED INTO : %d millisecond", endAuth.getTimeInMillis() - GameClient.startAuth.getTimeInMillis());
    }

    private static void writeServerDetails(ByteBufferWriter b, UdpConnection connection, ServerWorldDatabase.LogonResult logonResult) {
        b.putBoolean(connection.isCoopHost);
        b.putInt(ServerOptions.getInstance().getMaxPlayers());
        if (b.putBoolean(SteamUtils.isSteamModeEnabled() && CoopSlave.instance != null && !connection.isCoopHost)) {
            b.putLong(CoopSlave.instance.hostSteamId);
            b.putUTF(GameServer.serverName);
        }

        b.putByte(connection.playerIds[0] / 4);
        logonResult.role.send(b);
    }

    private static void writeGameMap(ByteBufferWriter b) {
        b.putUTF(GameServer.gameMap);
    }

    private static void writeWorkshopItems(ByteBufferWriter b) {
        b.putShort(GameServer.WorkshopItems.size());

        for (int i = 0; i < GameServer.WorkshopItems.size(); i++) {
            b.putLong(GameServer.WorkshopItems.get(i));
            b.putLong(GameServer.workshopTimeStamps[i]);
        }
    }

    private static void writeMods(ByteBufferWriter b) {
        ArrayList<ChooseGameInfo.Mod> mods = new ArrayList<>();
        ArrayList<String> missingMods = new ArrayList<>();

        for (String modId : GameServer.ServerMods) {
            String modDir = ZomboidFileSystem.instance.getModDir(modId);
            ChooseGameInfo.Mod mod;
            if (modDir != null) {
                try {
                    mod = ChooseGameInfo.readModInfo(modDir);
                } catch (Exception var8) {
                    ExceptionLogger.logException(var8);
                    missingMods.add(modId);
                    mod = null;
                }
            } else {
                missingMods.add(modId);
                mod = null;
            }

            if (mod != null) {
                mods.add(mod);
            }
        }

        b.putInt(mods.size() + missingMods.size());

        for (ChooseGameInfo.Mod modF : mods) {
            b.putUTF(modF.getId());
            b.putUTF(modF.getWorkshopID());
            b.putUTF(modF.getName());
        }

        for (String modF : missingMods) {
            b.putUTF(modF);
            b.putUTF("");
            b.putUTF(modF);
        }
    }

    private static void writeStartLocation(ByteBufferWriter b) {
        ServerWorldDatabase.LogonResult r = null;
        b.putInt(10745);
        b.putInt(9412);
        b.putInt(0);
    }

    private static void writeServerOptions(ByteBufferWriter b) {
        b.putInt(ServerOptions.instance.getPublicOptions().size());

        for (String key : ServerOptions.instance.getPublicOptions()) {
            b.putUTF(key);
            b.putUTF(ServerOptions.instance.getOption(key));
        }
    }

    private static void writeSandboxOptions(ByteBufferWriter b) throws IOException {
        SandboxOptions.instance.save(b.bb);
    }

    private static void writeGameTime(ByteBufferWriter b) throws IOException {
        GameTime.getInstance().saveToPacket(b);
    }

    private static void writeErosionMain(ByteBufferWriter b) {
        ErosionMain.getInstance().getConfig().save(b);
    }

    private static void writeGlobalObjects(ByteBufferWriter b) throws IOException {
        SGlobalObjects.saveInitialStateForClient(b);
    }

    private static void writeResetID(ByteBufferWriter b) {
        b.putInt(GameServer.resetId);
    }

    private static void writeBerries(ByteBufferWriter b) {
        b.putUTF(Core.getInstance().getPoisonousBerry());
        b.putUTF(Core.getInstance().getPoisonousMushroom());
    }

    private static void writeWorldDictionary(ByteBufferWriter b) throws IOException {
        WorldDictionary.saveDataForClient(b);
    }
}
