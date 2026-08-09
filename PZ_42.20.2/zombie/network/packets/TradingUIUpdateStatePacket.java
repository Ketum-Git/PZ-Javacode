// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.TradingManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class TradingUIUpdateStatePacket implements INetworkPacket {
    PlayerID playerA = new PlayerID();
    PlayerID playerB = new PlayerID();
    protected TradingUIUpdateStatePacket.State state;

    public void set(IsoPlayer you, IsoPlayer other, byte state) {
        this.playerA.set(you);
        this.playerB.set(other);
        this.state = TradingUIUpdateStatePacket.State.values()[state];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerA.write(b);
        this.playerB.write(b);
        b.putEnum(this.state);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerA.parse(b, connection);
        this.playerB.parse(b, connection);
        this.state = b.getEnum(TradingUIUpdateStatePacket.State.class);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.playerA.getPlayer() != null) {
            LuaEventManager.triggerEvent("TradingUIUpdateState", this.playerA.getPlayer(), this.playerB.getPlayer(), this.state.ordinal());
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == TradingUIUpdateStatePacket.State.WindowsWasClosed) {
            TradingManager.getInstance().cancelTrading(this.playerA.getPlayer());
        } else if (this.state == TradingUIUpdateStatePacket.State.DealWasSealed) {
            TradingManager.getInstance().dealSealStatusChanged(this.playerA.getPlayer(), true);
        } else if (this.state == TradingUIUpdateStatePacket.State.DealWasUnsealed) {
            TradingManager.getInstance().dealSealStatusChanged(this.playerA.getPlayer(), false);
        } else if (this.state == TradingUIUpdateStatePacket.State.DealWasFinalized) {
            TradingManager.getInstance().finishTrading(this.playerA.getPlayer());
        }

        connection = GameServer.getConnectionFromPlayer(this.playerB.getPlayer());
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.TradingUIUpdateState.doPacket(b);
        this.write(b);
        PacketTypes.PacketType.TradingUIUpdateState.send(connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerA.getPlayer() != null && this.playerB.getPlayer() != null;
    }

    static enum State {
        WindowsWasClosed,
        DealWasSealed,
        DealWasUnsealed,
        DealWasFinalized;
    }
}
