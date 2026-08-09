// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkPlayerAI;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
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
        IsoPlayer player = this.getPlayer();
        b.putFloat(player.getVariableFloat("IdleSpeed", 0.01F));
        b.putFloat(player.getVariableFloat("StrafeSpeed", 1.0F));
        b.putFloat(player.getVariableFloat("WalkInjury", 0.0F));
        NetworkPlayerAI networkAi = player.getNetworkCharacterAI();
        b.putFloat(networkAi.walkSpeed);
        b.putFloat(networkAi.runSpeed);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            IsoPlayer player = this.getPlayer();
            player.setVariable("IdleSpeed", b.getFloat());
            player.setVariable("StrafeSpeed", b.getFloat());
            player.setVariable("WalkInjury", b.getFloat());
            NetworkPlayerAI networkAi = player.getNetworkCharacterAI();
            networkAi.walkSpeed = b.getFloat();
            networkAi.runSpeed = b.getFloat();
        }
    }
}
