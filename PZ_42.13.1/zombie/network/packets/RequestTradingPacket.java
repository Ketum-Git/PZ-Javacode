// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.TradingManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RequestTradingPacket implements INetworkPacket {
    RequestTradingPacket.RequestType type;
    PlayerID playerA = new PlayerID();
    PlayerID playerB = new PlayerID();

    public void ask(IsoPlayer player, IsoPlayer other) {
        this.type = RequestTradingPacket.RequestType.Ask;
        this.playerA.set(player);
        this.playerB.set(other);
    }

    public void accept(IsoPlayer player, IsoPlayer other) {
        this.type = RequestTradingPacket.RequestType.Accept;
        this.playerA.set(player);
        this.playerB.set(other);
    }

    public void reject(IsoPlayer player, IsoPlayer other) {
        this.type = RequestTradingPacket.RequestType.Reject;
        this.playerA.set(player);
        this.playerB.set(other);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.type = RequestTradingPacket.RequestType.values()[b.get()];
        this.playerA.parse(b, connection);
        this.playerB.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.type.ordinal());
        this.playerA.write(b);
        this.playerB.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.type == RequestTradingPacket.RequestType.Ask) {
            LuaEventManager.triggerEvent("RequestTrade", this.playerA.getPlayer());
        } else {
            LuaEventManager.triggerEvent("AcceptedTrade", this.type == RequestTradingPacket.RequestType.Accept);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.type == RequestTradingPacket.RequestType.Accept) {
            TradingManager.getInstance().addNewTrading(this.playerA.getPlayer(), this.playerB.getPlayer());
        }

        connection = GameServer.getConnectionFromPlayer(this.playerB.getPlayer());
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.RequestTrading.doPacket(b);
        this.write(b);
        PacketTypes.PacketType.RequestTrading.send(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerA.getPlayer() != null && this.playerB.getPlayer() != null;
    }

    static enum RequestType {
        Ask,
        Accept,
        Reject;
    }
}
