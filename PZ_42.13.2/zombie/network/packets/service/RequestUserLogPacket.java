// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map.Entry;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.PacketValidator;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.ReadUserLog, handlingType = 3)
public class RequestUserLogPacket implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    ArrayList<Userlog> userLog = new ArrayList<>();
    @JSONField
    EnumMap<AntiCheat, Integer> suspiciousActivity = new EnumMap<>(AntiCheat.class);

    @Override
    public void setData(Object... values) {
        this.username = (String)values[0];
        if (GameServer.server) {
            this.userLog = ServerWorldDatabase.instance.getUserlog(this.username);
            PacketValidator validator = (PacketValidator)values[1];
            if (validator != null) {
                this.suspiciousActivity = validator.getCounters();
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteStringUTF(b.bb, this.username);
        if (GameServer.server) {
            b.putInt(this.userLog.size());

            for (Userlog r : this.userLog) {
                r.write(b.bb);
            }

            b.putInt(this.suspiciousActivity.size());

            for (Entry<AntiCheat, Integer> point : this.suspiciousActivity.entrySet()) {
                GameWindow.WriteStringUTF(b.bb, point.getKey().name());
                b.putInt(point.getValue());
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.username = GameWindow.ReadString(b);
    }

    @Override
    public void parseClient(ByteBuffer b, UdpConnection connection) {
        this.parse(b, connection);
        this.userLog.clear();
        int userLogSize = b.getInt();

        for (int i = 0; i < userLogSize; i++) {
            this.userLog.add(new Userlog(b));
        }

        this.suspiciousActivity.clear();
        int suspiciousActivitySize = b.getInt();

        for (int i = 0; i < suspiciousActivitySize; i++) {
            this.suspiciousActivity.put(AntiCheat.valueOf(GameWindow.ReadString(b)), b.getInt());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (connection.role.hasCapability(Capability.ReadUserLog)) {
            if (this.suspiciousActivity.isEmpty()) {
                LuaEventManager.triggerEvent("OnReceiveUserlog", this.username, this.userLog, null);
            } else {
                KahluaTable tbl = LuaManager.platform.newTable();

                for (Entry<AntiCheat, Integer> point : this.suspiciousActivity.entrySet()) {
                    tbl.rawset(point.getKey().name(), point.getValue().toString());
                }

                LuaEventManager.triggerEvent("OnReceiveUserlog", this.username, this.userLog, tbl);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (connection.role.hasCapability(Capability.ReadUserLog)) {
            PacketValidator validator = null;
            IsoPlayer player = GameServer.getPlayerByUserName(this.username);
            if (player != null) {
                UdpConnection c = GameServer.getConnectionFromPlayer(player);
                if (c != null) {
                    validator = c.validator;
                }
            }

            INetworkPacket.send(connection, PacketTypes.PacketType.RequestUserLog, this.username, validator);
        }
    }
}
