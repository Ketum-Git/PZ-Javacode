// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spnetwork;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.network.packets.ObjectChangePacket;

public final class SinglePlayerClient {
    private static final ArrayList<ZomboidNetData> MainLoopNetData = new ArrayList<>();
    public static final UdpEngine udpEngine = new SinglePlayerClient.UdpEngineClient();
    public static final UdpConnection connection = new UdpConnection(udpEngine);

    public static void addIncoming(short id, ByteBuffer bb) {
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

    public static void update() throws Exception {
        if (!GameClient.client) {
            for (short i = 0; i < IsoPlayer.numPlayers; i++) {
                if (IsoPlayer.players[i] != null) {
                    IsoPlayer.players[i].setOnlineID(i);
                }
            }

            synchronized (MainLoopNetData) {
                for (int n = 0; n < MainLoopNetData.size(); n++) {
                    ZomboidNetData data = MainLoopNetData.get(n);

                    try {
                        mainLoopDealWithNetData(data);
                    } finally {
                        MainLoopNetData.remove(n--);
                    }
                }
            }
        }
    }

    private static void mainLoopDealWithNetData(ZomboidNetData d) throws Exception {
        ByteBufferReader bb = d.bufferReader;

        try {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(d.type);
            switch (packetType) {
                case ClientCommand:
                    receiveServerCommand(bb);
                    break;
                case GlobalObjects:
                    CGlobalObjectNetwork.receive(bb);
                    break;
                case ObjectChange:
                    receiveObjectChange(bb);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + packetType);
            }
        } finally {
            ZomboidNetDataPool.instance.discard(d);
        }
    }

    private static void delayPacket(int x, int y, int z) {
    }

    private static IsoPlayer getPlayerByID(int id) {
        return IsoPlayer.players[id];
    }

    private static void receiveObjectChange(ByteBufferReader bb) {
        try {
            ObjectChangePacket.parseObjectChange(null, bb, connection);
        } catch (ObjectChangePacket.IsoObjectChangeTargetNotFoundException var2) {
            DebugType.Network.error("SinglePlayerClient failed to parse ObjectChange. %s", var2.getMessage());
        }
    }

    public static void sendClientCommand(IsoPlayer player, String module, String command, KahluaTable args) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putByte(player != null ? player.playerIndex : -1);
        b.putUTF(module);
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
                DebugType.General.printException(var6, LogSeverity.Error);
            }
        }

        connection.endPacketImmediate();
    }

    private static void receiveServerCommand(ByteBufferReader bb) {
        String module = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var6) {
                DebugType.General.printException(var6, LogSeverity.Error);
                return;
            }
        }

        LuaEventManager.triggerEvent("OnServerCommand", module, command, tbl);
    }

    public static void Reset() {
        for (ZomboidNetData data : MainLoopNetData) {
            ZomboidNetDataPool.instance.discard(data);
        }

        MainLoopNetData.clear();
    }

    private static final class UdpEngineClient extends UdpEngine {
        @Override
        public void Send(ByteBuffer bb) {
            SinglePlayerServer.udpEngine.Receive(bb);
        }

        @Override
        public void Receive(ByteBuffer bb) {
            int id = bb.get() & 255;
            short pkt = bb.getShort();
            SinglePlayerClient.addIncoming(pkt, bb);
        }
    }
}
