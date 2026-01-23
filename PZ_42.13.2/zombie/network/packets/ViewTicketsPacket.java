// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.DBTicket;
import zombie.network.GameClient;
import zombie.network.GameServer;
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
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (GameServer.server) {
            this.author = GameWindow.ReadString(b);
            if ("".equals(this.author)) {
                this.author = null;
            }
        }

        if (GameClient.client) {
            ArrayList<DBTicket> result = new ArrayList<>();
            int size = b.getInt();

            for (int i = 0; i < size; i++) {
                DBTicket newTicket = new DBTicket(GameWindow.ReadString(b), GameWindow.ReadString(b), b.getInt(), b.get() == 1);
                result.add(newTicket);
                if (b.get() == 1) {
                    DBTicket answer = new DBTicket(GameWindow.ReadString(b), GameWindow.ReadString(b), b.getInt(), b.get() == 1);
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
            var4.printStackTrace();
        }
    }

    @Override
    public void setData(Object... values) {
        this.author = (String)values[0];
    }
}
