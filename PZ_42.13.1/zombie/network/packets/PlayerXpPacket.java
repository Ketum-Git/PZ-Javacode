// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.CanModifyPlayerStatsInThePlayerStatsUI, handlingType = 3)
public class PlayerXpPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);

        try {
            this.getPlayer().getXp().save(b.bb);
        } catch (IOException var3) {
            DebugLog.Multiplayer.printException(var3, "Player XP save error", LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection) && !this.getPlayer().isDead()) {
            try {
                this.getPlayer().getXp().load(b, 240);
            } catch (IOException var4) {
                DebugLog.Multiplayer.printException(var4, "Player XP load error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClients(packetType, connection);
    }
}
