// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 4, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class PlayerStatsPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);

        try {
            this.getPlayer().getStats().save(b.bb);
            this.getPlayer().getNutrition().save(b.bb);
            b.putFloat(this.getPlayer().getTimeSinceLastSmoke());
            this.getPlayer().getBodyDamage().saveMainFields(b.bb);
        } catch (IOException var3) {
            DebugLog.Multiplayer.printException(var3, "PlayerDamagePacket: failed", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            try {
                this.getPlayer().getStats().load(b, IsoWorld.getWorldVersion());
                this.getPlayer().getNutrition().load(b);
                this.getPlayer().setTimeSinceLastSmoke(b.getFloat());
                this.getPlayer().getBodyDamage().loadMainFields(b, IsoWorld.getWorldVersion());
            } catch (IOException var4) {
                DebugLog.Multiplayer.printException(var4, "PlayerDamagePacket: failed", LogSeverity.Error);
            }
        }
    }
}
