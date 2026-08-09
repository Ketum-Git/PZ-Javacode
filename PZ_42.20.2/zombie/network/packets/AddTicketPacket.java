// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class AddTicketPacket implements INetworkPacket {
    @JSONField
    String author;
    @JSONField
    String message;
    @JSONField
    int ticketId;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.author);
        b.putUTF(this.message);
        b.putInt(this.ticketId);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.author = b.getUTF();
        this.message = b.getUTF();
        this.ticketId = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            if (this.ticketId == -1) {
                for (UdpConnection c : GameServer.udpEngine.connections) {
                    if (c.isFullyConnected()
                        && c.getRole() != null
                        && (c.getRole().hasCapability(Capability.CanSeeMessageForAdmin) || c.getRole().hasCapability(Capability.AnswerTickets))) {
                        INetworkPacket.send(
                            c, PacketTypes.PacketType.MessageForAdmin, "user " + this.author + " added a ticket <LINE> <LINE> " + this.message, -1, -1, -1
                        );
                    }
                }
            }

            ServerWorldDatabase.instance.addTicket(this.author, this.message, this.ticketId);
            if (this.ticketId == -1) {
                GameServer.sendTickets(this.author, connection);
            } else {
                GameServer.sendTickets(null, connection);
            }
        } catch (SQLException var5) {
            DebugType.General.printException(var5, LogSeverity.Error);
        }
    }

    @Override
    public void setData(Object... values) {
        this.author = (String)values[0];
        this.message = (String)values[1];
        this.ticketId = (Integer)values[2];
    }
}
