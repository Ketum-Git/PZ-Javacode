// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.GameStatistic;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.network.statistics.data.PerformanceStatistic;

@PacketSetting(ordering = 0, priority = 3, reliability = 1, requiredCapability = Capability.GetStatistic, handlingType = 3)
public class StatisticsPacket implements INetworkPacket {
    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (GameClient.client) {
            NetworkStatistic.getInstance().getRemoteTable().load(b.bb, IsoWorld.getWorldVersion());
            PerformanceStatistic.getInstance().getRemoteTable().load(b.bb, IsoWorld.getWorldVersion());
            GameStatistic.getInstance().getRemoteTable().load(b.bb, IsoWorld.getWorldVersion());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameServer.server) {
            NetworkStatistic.getInstance().getLocalTable().save(b.bb);
            PerformanceStatistic.getInstance().getLocalTable().save(b.bb);
            GameStatistic.getInstance().getLocalTable().save(b.bb);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        connection.statisticTransmissionEnabled = !connection.statisticTransmissionEnabled;
    }
}
