// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class WakeUpPlayerPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getPlayer().setAsleep(false);
        this.getPlayer().setAsleepTime(0.0F);
        this.sendToClients(PacketTypes.PacketType.WakeUpPlayer, connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        SleepingEvent.instance.wakeUp(this.getPlayer(), true);
    }
}
