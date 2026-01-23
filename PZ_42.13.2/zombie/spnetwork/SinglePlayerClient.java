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
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.network.fields.Square;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

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
        ByteBuffer bb = d.buffer;

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

    private static void receiveObjectChange(ByteBuffer bb) {
        byte type = bb.get();
        if (type == 1) {
            short id = bb.getShort();
            String change = GameWindow.ReadString(bb);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + change);
            }

            IsoPlayer p = getPlayerByID(id);
            if (p != null) {
                p.loadChange(change, bb);
            }
        } else if (type == 2) {
            short idx = bb.getShort();
            String changex = GameWindow.ReadString(bb);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changex);
            }

            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(idx);
            if (vehicle != null) {
                vehicle.loadChange(changex, bb);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: unknown vehicle id=" + idx);
            }
        } else if (type == 3) {
            Square square = new Square();
            square.parse(bb, null);
            int itemID = bb.getInt();
            String changexx = GameWindow.ReadString(bb);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changexx);
            }

            IsoGridSquare sq = square.getSquare();
            if (sq == null) {
                delayPacket(PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()));
                return;
            }

            for (int i = 0; i < sq.getWorldObjects().size(); i++) {
                IsoWorldInventoryObject worldItem = sq.getWorldObjects().get(i);
                if (worldItem.getItem() != null && worldItem.getItem().getID() == itemID) {
                    worldItem.loadChange(changexx, bb);
                    return;
                }
            }

            if (Core.debug) {
                DebugLog.log("receiveObjectChange: itemID=" + itemID + " is invalid x,y,z=" + square.getX() + "," + square.getY() + "," + square.getZ());
            }
        } else {
            Square squarex = new Square();
            squarex.parse(bb, null);
            int index = bb.getInt();
            String changexxx = GameWindow.ReadString(bb);
            if (Core.debug) {
                DebugLog.log("receiveObjectChange " + changexxx);
            }

            IsoGridSquare sqx = squarex.getSquare();
            if (sqx == null) {
                delayPacket(PZMath.fastfloor(squarex.getX()), PZMath.fastfloor(squarex.getY()), PZMath.fastfloor(squarex.getZ()));
                return;
            }

            if (index >= 0 && index < sqx.getObjects().size()) {
                IsoObject o = sqx.getObjects().get(index);
                o.loadChange(changexxx, bb);
            } else if (Core.debug) {
                DebugLog.log("receiveObjectChange: index=" + index + " is invalid x,y,z=" + squarex.getX() + "," + squarex.getY() + "," + squarex.getZ());
            }
        }
    }

    public static void sendClientCommand(IsoPlayer player, String module, String command, KahluaTable args) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putByte((byte)(player != null ? player.playerIndex : -1));
        b.putUTF(module);
        b.putUTF(command);
        if (args != null && !args.isEmpty()) {
            b.putByte((byte)1);

            try {
                KahluaTableIterator it = args.iterator();

                while (it.advance()) {
                    if (!TableNetworkUtils.canSave(it.getKey(), it.getValue())) {
                        DebugLog.log("ERROR: sendClientCommand: can't save key,value=" + it.getKey() + "," + it.getValue());
                    }
                }

                TableNetworkUtils.save(args, b.bb);
            } catch (IOException var6) {
                var6.printStackTrace();
            }
        } else {
            b.putByte((byte)0);
        }

        connection.endPacketImmediate();
    }

    private static void receiveServerCommand(ByteBuffer bb) {
        String module = GameWindow.ReadString(bb);
        String command = GameWindow.ReadString(bb);
        boolean hasArgs = bb.get() == 1;
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var6) {
                var6.printStackTrace();
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
