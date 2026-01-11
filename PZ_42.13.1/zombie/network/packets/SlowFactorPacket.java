// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SlowFactorPacket implements INetworkPacket {
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    float slowTimer;
    @JSONField
    float slowFactor;

    @Override
    public void setData(Object... values) {
        IsoPlayer chr = (IsoPlayer)values[0];
        UdpConnection c = GameServer.getConnectionFromPlayer(chr);
        if (c != null) {
            this.playerId.set(chr);
            this.slowTimer = chr.getSlowTimer();
            this.slowFactor = chr.getSlowFactor();
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer player = this.playerId.getPlayer();
        if (player != null && !player.isDead()) {
            player.setSlowTimer(this.slowTimer);
            player.setSlowFactor(this.slowFactor);
            DebugLog.log(DebugType.Combat, "slowTimer=" + this.slowTimer + " slowFactor=" + this.slowFactor);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        b.putFloat(this.slowTimer);
        b.putFloat(this.slowFactor);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putFloat(this.slowTimer);
        b.putFloat(this.slowFactor);
    }
}
