// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 6)
public class SyncClockPacket implements INetworkPacket {
    @JSONField
    boolean fastForward;
    @JSONField
    float timeOfDay;
    @JSONField
    int nightsSurvived;

    @Override
    public void setData(Object... values) {
        GameTime gt = GameTime.getInstance();
        this.fastForward = GameServer.fastForward;
        this.timeOfDay = gt.getTimeOfDay();
        this.nightsSurvived = gt.getNightsSurvived();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.fastForward);
        b.putFloat(this.timeOfDay);
        b.putInt(this.nightsSurvived);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.fastForward = b.get() == 1;
        this.timeOfDay = b.getFloat();
        this.nightsSurvived = b.getInt();
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameTime gt = GameTime.getInstance();
        GameClient.fastForward = this.fastForward;
        float delta = gt.getTimeOfDay() - gt.getLastTimeOfDay();
        gt.setTimeOfDay(this.timeOfDay);
        gt.setLastTimeOfDay(this.timeOfDay - delta);
        if (gt.getLastTimeOfDay() < 0.0F) {
            gt.setLastTimeOfDay(this.timeOfDay - delta + 24.0F);
        }

        gt.serverLastTimeOfDay = gt.serverTimeOfDay;
        gt.serverTimeOfDay = this.timeOfDay;
        gt.setNightsSurvived(this.nightsSurvived);
        if (gt.serverLastTimeOfDay > gt.serverTimeOfDay) {
            gt.serverNewDays++;
        }
    }
}
