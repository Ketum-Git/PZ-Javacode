// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.network.packets.INetworkPacket;

public final class SinglePlayerServer {
    private static final ArrayList<ZomboidNetData> MainLoopNetData = new ArrayList<>();
    public static final SinglePlayerServer.UdpEngineServer udpEngine = new SinglePlayerServer.UdpEngineServer();

    public static void addIncoming(short id, ByteBuffer bb, UdpConnection connection) {
        ZomboidNetData d;
        if (bb.remaining() > 2048) {
            d = ZomboidNetDataPool.instance.getLong(bb.remaining());
        } else {
            d = ZomboidNetDataPool.instance.get();
        }

        d.read(id, bb, connection);
        synchronized (MainLoopNetData) {
            MainLoopNetData.add(d);
        }
    }

    private static void sendObjectChange(IsoObject o, String change, KahluaTable tbl, UdpConnection c) {
        if (o.getSquare() != null) {
            try {
                INetworkPacket packet = c.getPacket(PacketTypes.PacketType.ObjectChange);
                packet.setData(o, change, tbl);
                ByteBufferWriter bbw = c.startPacket();
                PacketTypes.PacketType.ObjectChange.doPacket(bbw);
                packet.write(bbw);
                c.endPacketImmediate();
            } catch (Exception var6) {
                c.cancelPacket();
                DebugLog.Multiplayer.printException(var6, "Packet ObjectChange sp send error", LogSeverity.Error);
            }
        }
    }

    public static void sendObjectChange(IsoObject o, String change, KahluaTable tbl) {
        if (o != null) {
            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.ReleventTo(o.getX(), o.getY())) {
                    sendObjectChange(o, change, tbl, c);
                }
            }
        }
    }

    public static void sendObjectChange(IsoObject o, String change, Object... objects) {
        if (objects.length == 0) {
            sendObjectChange(o, change, (KahluaTable)null);
        } else if (objects.length % 2 == 0) {
            KahluaTable t = LuaManager.platform.newTable();

            for (int i = 0; i < objects.length; i += 2) {
                Object v = objects[i + 1];
                if (v instanceof Float f) {
                    t.rawset(objects[i], f.doubleValue());
                } else if (v instanceof Integer integer) {
                    t.rawset(objects[i], integer.doubleValue());
                } else if (v instanceof Short s) {
                    t.rawset(objects[i], s.doubleValue());
                } else {
                    t.rawset(objects[i], v);
                }
            }

            sendObjectChange(o, change, t);
        }
    }

    public static void sendServerCommand(String module, String command, KahluaTable args, UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putUTF(module);
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
            } catch (IOException var6) {
                var6.printStackTrace();
            }
        } else {
            b.putByte((byte)0);
        }

        c.endPacketImmediate();
    }

    public static void sendServerCommand(String module, String command, KahluaTable args) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendServerCommand(module, command, args, c);
        }
    }

    public static void update() {
        if (!GameClient.client) {
            for (short i = 0; i < IsoPlayer.numPlayers; i++) {
                if (IsoPlayer.players[i] != null) {
                    IsoPlayer.players[i].setOnlineID(i);
                }
            }

            synchronized (MainLoopNetData) {
                for (int n = 0; n < MainLoopNetData.size(); n++) {
                    ZomboidNetData data = MainLoopNetData.get(n);
                    mainLoopDealWithNetData(data);
                    MainLoopNetData.remove(n--);
                }
            }
        }
    }

    private static void mainLoopDealWithNetData(ZomboidNetData d) {
        ByteBuffer bb = d.buffer;

        try {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(d.type);
            switch (packetType) {
                case ClientCommand:
                    receiveClientCommand(bb, d.connection);
                    break;
                case GlobalObjects:
                    receiveGlobalObjects(bb, d.connection);
            }
        } finally {
            ZomboidNetDataPool.instance.discard(d);
        }
    }

    private static IsoPlayer getAnyPlayerFromConnection(UdpConnection connection) {
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            if (connection.players[playerIndex] != null) {
                return connection.players[playerIndex];
            }
        }

        return null;
    }

    private static IsoPlayer getPlayerFromConnection(UdpConnection connection, int playerIndex) {
        return playerIndex >= 0 && playerIndex < 4 ? connection.players[playerIndex] : null;
    }

    private static void receiveClientCommand(ByteBuffer bb, UdpConnection connection) {
        int playerIndex = bb.get();
        String module = GameWindow.ReadString(bb);
        String command = GameWindow.ReadString(bb);
        boolean hasArgs = bb.get() == 1;
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var8) {
                var8.printStackTrace();
                return;
            }
        }

        IsoPlayer player = getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = getAnyPlayerFromConnection(connection);
        }

        if (player == null) {
            DebugLog.log("receiveClientCommand: player is null");
        } else {
            LuaEventManager.triggerEvent("OnClientCommand", module, command, player, tbl);
        }
    }

    private static void receiveGlobalObjects(ByteBuffer bb, UdpConnection connection) {
        int playerIndex = bb.get();
        IsoPlayer player = getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = getAnyPlayerFromConnection(connection);
        }

        if (player == null) {
            DebugLog.log("receiveGlobalObjects: player is null");
        } else {
            SGlobalObjectNetwork.receive(bb, player);
        }
    }

    public static void Reset() {
        for (ZomboidNetData data : MainLoopNetData) {
            ZomboidNetDataPool.instance.discard(data);
        }

        MainLoopNetData.clear();
    }

    public static final class UdpEngineServer extends UdpEngine {
        public final ArrayList<UdpConnection> connections = new ArrayList<>();

        UdpEngineServer() {
            this.connections.add(new UdpConnection(this));
        }

        @Override
        public void Send(ByteBuffer bb) {
            SinglePlayerClient.udpEngine.Receive(bb);
        }

        @Override
        public void Receive(ByteBuffer bb) {
            int id = bb.get() & 255;
            short pkt = bb.getShort();
            SinglePlayerServer.addIncoming(pkt, bb, SinglePlayerServer.udpEngine.connections.get(0));
        }
    }
}
