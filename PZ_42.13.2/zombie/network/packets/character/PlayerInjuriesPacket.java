// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 4, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class PlayerInjuriesPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.getPlayer().getVariableFloat("IdleSpeed", 0.01F));
        b.putFloat(this.getPlayer().getVariableFloat("StrafeSpeed", 1.0F));
        b.putFloat(this.getPlayer().getVariableFloat("WalkSpeed", 1.0F));
        b.putFloat(this.getPlayer().getVariableFloat("WalkInjury", 0.0F));
        b.putFloat(this.getPlayer().getSneakLimpSpeedScale());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            this.getPlayer().setVariable("IdleSpeed", b.getFloat());
            this.getPlayer().setVariable("StrafeSpeed", b.getFloat());
            this.getPlayer().setVariable("WalkSpeed", b.getFloat());
            this.getPlayer().setVariable("WalkInjury", b.getFloat());
            this.getPlayer().setSneakLimpSpeedScale(b.getFloat());
        }
    }
}
