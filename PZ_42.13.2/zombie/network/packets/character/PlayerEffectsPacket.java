// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 4, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class PlayerEffectsPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.getPlayer().getSleepingTabletEffect());
        b.putFloat(this.getPlayer().getSleepingTabletDelta());
        b.putInt(this.getPlayer().getSleepingPillsTaken());
        b.putFloat(this.getPlayer().getBetaEffect());
        b.putFloat(this.getPlayer().getBetaDelta());
        b.putFloat(this.getPlayer().getDepressEffect());
        b.putFloat(this.getPlayer().getDepressDelta());
        b.putFloat(this.getPlayer().getPainEffect());
        b.putFloat(this.getPlayer().getPainDelta());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection) && GameTime.instance.calender != null) {
            this.getPlayer().setSleepingTabletEffect(b.getFloat());
            this.getPlayer().setSleepingTabletDelta(b.getFloat());
            this.getPlayer().setSleepingPillsTaken(b.getInt());
            this.getPlayer().setBetaEffect(b.getFloat());
            this.getPlayer().setBetaDelta(b.getFloat());
            this.getPlayer().setDepressEffect(b.getFloat());
            this.getPlayer().setDepressDelta(b.getFloat());
            this.getPlayer().setPainEffect(b.getFloat());
            this.getPlayer().setPainDelta(b.getFloat());
        }
    }
}
