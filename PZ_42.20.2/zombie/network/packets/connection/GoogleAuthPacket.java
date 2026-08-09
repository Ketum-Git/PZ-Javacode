// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.None, handlingType = 1)
public class GoogleAuthPacket implements INetworkPacket {
    @JSONField
    protected String username;
    @JSONField
    protected String code;

    @Override
    public void setData(Object... values) {
        this.set((String)values[0], (String)values[1]);
    }

    private void set(String username, String code) {
        this.username = username;
        this.code = code;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ServerWorldDatabase.LogonResult r = ServerWorldDatabase.instance.googleAuthClient(this.username, this.code);
        if (!r.authorized) {
            LoggerManager.getLogger("user").write("access denied: user \"" + this.username + "\" reason \"" + r.dcReason + "\"");
            INetworkPacket.send(connection, PacketTypes.PacketType.AccessDenied, r.dcReason != null ? r.dcReason : "AccessDenied");
            connection.forceDisconnect("access-denied-unauthorized");
        } else {
            connection.setUserName(this.username);
            connection.usernames[0] = this.username;
            connection.googleAuth = false;
            GameServer.receiveClientConnect(connection, r);
        }
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        this.username = bb.getUTF();
        this.code = bb.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.username);
        b.putUTF(this.code);
    }
}
