// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.sql.SQLException;
import java.util.ArrayList;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.DBTicket;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ViewTicketsPacket implements INetworkPacket {
    @JSONField
    String author;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.author);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (GameServer.server) {
            this.author = b.getUTF();
            if ("".equals(this.author)) {
                this.author = null;
            }
        }

        if (GameClient.client) {
            ArrayList<DBTicket> result = new ArrayList<>();
            int size = b.getInt();

            for (int i = 0; i < size; i++) {
                DBTicket newTicket = new DBTicket(b.getUTF(), b.getUTF(), b.getInt(), b.getBoolean());
                result.add(newTicket);
                if (b.getBoolean()) {
                    DBTicket answer = new DBTicket(b.getUTF(), b.getUTF(), b.getInt(), b.getBoolean());
                    answer.setIsAnswer(true);
                    newTicket.setAnswer(answer);
                }
            }

            LuaEventManager.triggerEvent("ViewTickets", result);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            GameServer.sendTickets(this.author, connection);
        } catch (SQLException var4) {
            DebugType.General.printException(var4, LogSeverity.Error);
        }
    }

    @Override
    public void setData(Object... values) {
        this.author = (String)values[0];
    }
}
