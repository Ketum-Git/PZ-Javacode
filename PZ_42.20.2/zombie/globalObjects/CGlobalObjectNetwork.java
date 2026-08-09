// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.spnetwork.SinglePlayerClient;

public final class CGlobalObjectNetwork {
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(1048576);
    private static final ByteBufferWriter BYTE_BUFFER_WRITER = new ByteBufferWriter(BYTE_BUFFER);
    private static KahluaTable tempTable;

    public static void receive(ByteBufferReader bb) throws IOException {
        int packet = bb.getByte();
        switch (packet) {
            case 1:
                receiveServerCommand(bb);
            case 2:
            default:
                break;
            case 3:
                receiveNewLuaObjectAt(bb);
                break;
            case 4:
                receiveRemoveLuaObjectAt(bb);
                break;
            case 5:
                receiveUpdateLuaObjectAt(bb);
        }
    }

    private static void receiveServerCommand(ByteBufferReader bb) {
        String systemName = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var6) {
                ExceptionLogger.logException(var6);
                return;
            }
        }

        CGlobalObjects.receiveServerCommand(systemName, command, tbl);
    }

    private static void receiveNewLuaObjectAt(ByteBufferReader bb) throws IOException {
        String systemName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getByte();
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }

        TableNetworkUtils.load(tempTable, bb);
        CGlobalObjectSystem system = CGlobalObjects.getSystemByName(systemName);
        if (system != null) {
            system.receiveNewLuaObjectAt(x, y, z, tempTable);
        }
    }

    private static void receiveRemoveLuaObjectAt(ByteBufferReader bb) {
        String systemName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getByte();
        CGlobalObjectSystem system = CGlobalObjects.getSystemByName(systemName);
        if (system != null) {
            system.receiveRemoveLuaObjectAt(x, y, z);
        }
    }

    private static void receiveUpdateLuaObjectAt(ByteBufferReader bb) throws IOException {
        String systemName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getByte();
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }

        TableNetworkUtils.load(tempTable, bb);
        CGlobalObjectSystem system = CGlobalObjects.getSystemByName(systemName);
        if (system != null) {
            system.receiveUpdateLuaObjectAt(x, y, z, tempTable);
        }
    }

    private static void sendPacket(ByteBuffer bb) {
        if (GameServer.server) {
            throw new IllegalStateException("can't call this method on the server");
        } else {
            if (GameClient.client) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                bb.flip();
                b.put(bb);
                PacketTypes.PacketType.GlobalObjects.send(GameClient.connection);
            } else {
                ByteBufferWriter b = SinglePlayerClient.connection.startPacket();
                bb.flip();
                b.put(bb);
                SinglePlayerClient.connection.endPacketImmediate();
            }
        }
    }

    public static void sendClientCommand(IsoPlayer player, String systemName, String command, KahluaTable args) {
        BYTE_BUFFER.clear();
        writeClientCommand(player, systemName, command, args, BYTE_BUFFER_WRITER);
        sendPacket(BYTE_BUFFER);
    }

    private static void writeClientCommand(IsoPlayer player, String systemName, String command, KahluaTable args, ByteBufferWriter b) {
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte(player != null ? player.playerIndex : -1);
        b.putByte((byte)2);
        b.putUTF(systemName);
        b.putUTF(command);
        if (b.putBoolean(args != null && !args.isEmpty())) {
            try {
                KahluaTableIterator it = args.iterator();

                while (it.advance()) {
                    if (!TableNetworkUtils.canSave(it.getKey(), it.getValue())) {
                        DebugLog.log("ERROR: sendClientCommand: can't save key,value=" + it.getKey() + "," + it.getValue());
                    }
                }

                TableNetworkUtils.save(args, b);
            } catch (IOException var6) {
                ExceptionLogger.logException(var6);
            }
        }
    }

    public static void Reset() {
        if (tempTable != null) {
            tempTable.wipe();
            tempTable = null;
        }
    }
}
