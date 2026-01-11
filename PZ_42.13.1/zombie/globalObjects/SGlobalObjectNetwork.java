// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.spnetwork.SinglePlayerServer;

public final class SGlobalObjectNetwork {
    public static final byte PACKET_ServerCommand = 1;
    public static final byte PACKET_ClientCommand = 2;
    public static final byte PACKET_NewLuaObjectAt = 3;
    public static final byte PACKET_RemoveLuaObjectAt = 4;
    public static final byte PACKET_UpdateLuaObjectAt = 5;
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(1048576);
    private static final ByteBufferWriter BYTE_BUFFER_WRITER = new ByteBufferWriter(BYTE_BUFFER);

    public static void receive(ByteBuffer bb, IsoPlayer player) {
        int packet = bb.get();
        switch (packet) {
            case 2:
                receiveClientCommand(bb, player);
        }
    }

    private static void sendPacket(ByteBuffer bb) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.bb.put(bb);
                c.endPacketImmediate();
            }
        } else {
            if (GameClient.client) {
                throw new IllegalStateException("can't call this method on the client");
            }

            for (int n = 0; n < SinglePlayerServer.udpEngine.connections.size(); n++) {
                zombie.spnetwork.UdpConnection c = SinglePlayerServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.bb.put(bb);
                c.endPacketImmediate();
            }
        }
    }

    private static void writeServerCommand(String systemName, String command, KahluaTable args, ByteBufferWriter b) {
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)1);
        b.putUTF(systemName);
        b.putUTF(command);
        if (args != null && !args.isEmpty()) {
            b.putByte((byte)1);

            try {
                KahluaTableIterator it = args.iterator();

                while (it.advance()) {
                    if (!TableNetworkUtils.canSave(it.getKey(), it.getValue())) {
                        DebugLog.log("ERROR: sendServerCommand: can't save key,value=" + it.getKey() + "," + it.getValue());
                    }
                }

                TableNetworkUtils.save(args, b.bb);
            } catch (IOException var5) {
                ExceptionLogger.logException(var5);
            }
        } else {
            b.putByte((byte)0);
        }
    }

    public static void sendServerCommand(String systemName, String command, KahluaTable args) {
        BYTE_BUFFER.clear();
        writeServerCommand(systemName, command, args, BYTE_BUFFER_WRITER);
        sendPacket(BYTE_BUFFER);
    }

    public static void addGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)3);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte((byte)globalObject.getZ());
        SGlobalObjectSystem system = (SGlobalObjectSystem)globalObject.system;
        TableNetworkUtils.saveSome(globalObject.getModData(), b.bb, system.objectSyncKeys);
        sendPacket(BYTE_BUFFER);
    }

    public static void removeGlobalObjectOnClient(GlobalObject globalObject) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)4);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte((byte)globalObject.getZ());
        sendPacket(BYTE_BUFFER);
    }

    public static void updateGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)5);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte((byte)globalObject.getZ());
        SGlobalObjectSystem system = (SGlobalObjectSystem)globalObject.system;
        TableNetworkUtils.saveSome(globalObject.getModData(), b.bb, system.objectSyncKeys);
        sendPacket(BYTE_BUFFER);
    }

    private static void receiveClientCommand(ByteBuffer bb, IsoPlayer player) {
        String systemName = GameWindow.ReadString(bb);
        String command = GameWindow.ReadString(bb);
        boolean hasArgs = bb.get() == 1;
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var7) {
                var7.printStackTrace();
                return;
            }
        }

        SGlobalObjects.receiveClientCommand(systemName, command, player, tbl);
    }
}
