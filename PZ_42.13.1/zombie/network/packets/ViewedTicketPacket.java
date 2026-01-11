// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.AnswerTickets, handlingType = 1)
public class ViewedTicketPacket implements INetworkPacket {
    @JSONField
    String author;
    @JSONField
    int ticketId;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.author);
        b.putInt(this.ticketId);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.author = GameWindow.ReadString(b);
        this.ticketId = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.viewedTicket(this.ticketId, true);
            GameServer.sendTickets(null, connection);
        } catch (SQLException var4) {
            var4.printStackTrace();
        }
    }

    @Override
    public void setData(Object... values) {
        this.author = (String)values[0];
        this.ticketId = (Integer)values[1];
    }
}
