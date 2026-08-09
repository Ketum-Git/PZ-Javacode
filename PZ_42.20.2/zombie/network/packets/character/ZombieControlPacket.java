// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.character.ZombieID;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieControlPacket implements INetworkPacket {
    @JSONField
    private final ZombieID zombieId = new ZombieID();
    @JSONField
    private final PlayerID playerId = new PlayerID();

    @Override
    public void write(ByteBufferWriter b) {
        this.zombieId.write(b);
        this.playerId.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.zombieId.parse(b, connection);
        this.playerId.parse(b, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer target = Type.tryCastTo(this.zombieId.getZombie().getTarget(), IsoPlayer.class);
        if (target == null || target.getOnlineID() != this.playerId.getPlayer().getOnlineID()) {
            this.zombieId.getZombie().setTargetSeenTime(0.0F);
            this.zombieId.getZombie().setTarget(this.playerId.getPlayer());
        }
    }

    @Override
    public void setData(Object... values) {
        this.zombieId.set((IsoZombie)values[0]);
        this.playerId.set((IsoPlayer)values[1]);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.zombieId.isConsistent(connection) && this.playerId.isConsistent(connection);
    }
}
