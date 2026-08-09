// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 4, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class PlayerDamagePacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);

        try {
            b.putInt(this.getPlayer().getMaxWeight());
            b.putFloat(this.getPlayer().getCorpseSicknessRate());
            this.getPlayer().getBodyDamage().save(b.bb);
        } catch (IOException var3) {
            DebugType.Multiplayer.printException(var3, "PlayerDamagePacket: failed", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            try {
                this.getPlayer().setMaxWeight(b.getInt());
                this.getPlayer().setCorpseSicknessRate(b.getFloat());
                this.getPlayer().getBodyDamage().load(b.bb, IsoWorld.getWorldVersion());
            } catch (IOException var4) {
                DebugType.Multiplayer.printException(var4, "PlayerDamagePacket: failed", LogSeverity.Error);
            }
        }
    }
}
