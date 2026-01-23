// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 4, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class PlayerHealthPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);

        for (BodyPartType bodyPartType : BodyPartType.values()) {
            if (bodyPartType != BodyPartType.MAX) {
                b.putFloat(this.getPlayer().getBodyDamage().getBodyPart(bodyPartType).getHealth());
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            for (BodyPartType bodyPartType : BodyPartType.values()) {
                if (bodyPartType != BodyPartType.MAX) {
                    this.getPlayer().getBodyDamage().getBodyPart(bodyPartType).SetHealth(b.getFloat());
                }
            }

            this.getPlayer().getBodyDamage().calculateOverallHealth();
        }
    }
}
