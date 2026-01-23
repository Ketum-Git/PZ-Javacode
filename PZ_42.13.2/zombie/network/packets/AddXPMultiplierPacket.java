// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.Perk;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AddXPMultiplierPacket implements INetworkPacket {
    @JSONField
    public final PlayerID target = new PlayerID();
    @JSONField
    protected Perk perk = new Perk();
    @JSONField
    protected float multiplier;
    @JSONField
    protected int minLevel;
    @JSONField
    protected int maxLevel;

    @Override
    public void setData(Object... values) {
        this.target.set((IsoPlayer)values[0]);
        this.perk.set((PerkFactory.Perk)values[1]);
        this.multiplier = (Float)values[2];
        this.minLevel = (Integer)values[3];
        this.maxLevel = (Integer)values[4];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.target.parse(b, connection);
        this.perk.parse(b, connection);
        this.multiplier = b.getFloat();
        this.minLevel = b.getInt();
        this.maxLevel = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.target.write(b);
        this.perk.write(b);
        b.putFloat(this.multiplier);
        b.putInt(this.minLevel);
        b.putInt(this.maxLevel);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.target.getPlayer() != null && !this.target.getPlayer().isDead()) {
            if (this.target.getPlayer() != null && !this.target.getPlayer().isDead()) {
                this.target.getPlayer().getXp().addXpMultiplier(this.perk.getPerk(), this.multiplier, this.minLevel, this.maxLevel);
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.target.isConsistent(connection) && this.perk.isConsistent(connection);
    }
}
