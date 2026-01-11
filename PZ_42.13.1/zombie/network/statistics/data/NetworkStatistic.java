// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.ZNetStatistics;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.statistics.StatisticManager;
import zombie.network.statistics.counters.Counter;

public class NetworkStatistic extends Statistic implements IStatistic {
    private static final NetworkStatistic instance = new NetworkStatistic("network");
    public final Counter packets = new Counter(this, "packets", 0.0, null, "", "packets");
    public final Counter receivedPackets = new Counter(this, "received-packets", 0.0, null, "", "packets");
    public final Counter receivedBytes = new Counter(this, "received-bytes", 0.0, null, "", "bytes");
    public final Counter receivedBps = new Counter(this, "received-bps", 0.0, null, "", "bytes/second");
    public final Counter maxReceivedBps = new Counter(this, "max-received-bps", 0.0, null, "", "bytes/second");
    public final Counter sentPackets = new Counter(this, "sent-packets", 0.0, null, "", "packets");
    public final Counter sentBytes = new Counter(this, "sent-bytes", 0.0, null, "", "bytes");
    public final Counter sentBps = new Counter(this, "sent-bps", 0.0, null, "", "bytes/second");
    public final Counter maxSentBps = new Counter(this, "max-sent-bps", 0.0, null, "", "bytes/second");
    public final Counter lastUserMessageBytesPushed = new Counter(this, "last-user-message-bytes-pushed", 0.0, null, "", "bytes");
    public final Counter lastUserMessageBytesSent = new Counter(this, "last-user-message-bytes-sent", 0.0, null, "", "bytes");
    public final Counter lastUserMessageBytesResent = new Counter(this, "last-user-message-bytes-resent", 0.0, null, "", "bytes");
    public final Counter lastUserMessageBytesReceivedProcessed = new Counter(this, "last-user-message-bytes-received-processed", 0.0, null, "", "bytes");
    public final Counter lastUserMessageBytesReceivedIgnored = new Counter(this, "last-user-message-bytes-received-ignored", 0.0, null, "", "bytes");
    public final Counter lastActualBytesSent = new Counter(this, "last-actual-bytes-sent", 0.0, null, "", "bytes");
    public final Counter lastActualBytesReceived = new Counter(this, "last-actual-bytes-received", 0.0, null, "", "bytes");
    public final Counter totalUserMessageBytesPushed = new Counter(this, "total-user-message-bytes-pushed", 0.0, null, "", "bytes");
    public final Counter totalUserMessageBytesSent = new Counter(this, "total-user-message-bytes-sent", 0.0, null, "", "bytes");
    public final Counter totalUserMessageBytesResent = new Counter(this, "total-user-message-bytes-resent", 0.0, null, "", "bytes");
    public final Counter totalUserMessageBytesReceivedProcessed = new Counter(this, "total-user-message-bytes-received-processed", 0.0, null, "", "bytes");
    public final Counter totalUserMessageBytesReceivedIgnored = new Counter(this, "total-user-message-bytes-received-ignored", 0.0, null, "", "bytes");
    public final Counter totalActualBytesSent = new Counter(this, "total-actual-bytes-sent", 0.0, null, "", "bytes");
    public final Counter totalActualBytesReceived = new Counter(this, "total-actual-bytes-received", 0.0, null, "", "bytes");
    public final Counter connectionStartTime = new Counter(this, "connection-start-time", 0.0, null, "", "timestamp");
    public final Counter bpsLimitByCongestionControl = new Counter(this, "bps-limit-by-congestion-control", 0.0, null, "", "bytes/second");
    public final Counter bpsLimitByOutgoingBandwidthLimit = new Counter(this, "bps-limit-by-outgoing-bandwidth-limit", 0.0, null, "", "bytes/second");
    public final Counter messageInSendBufferImmediate = new Counter(this, "messages-in-send-buffer-immediate", 0.0, null, "", "packets");
    public final Counter messageInSendBufferHigh = new Counter(this, "messages-in-send-buffer-high", 0.0, null, "", "packets");
    public final Counter messageInSendBufferMedium = new Counter(this, "messages-in-send-buffer-medium", 0.0, null, "", "packets");
    public final Counter messageInSendBufferLow = new Counter(this, "messages-in-send-buffer-low", 0.0, null, "", "packets");
    public final Counter bytesInSendBufferImmediate = new Counter(this, "bytes-in-send-buffer-immediate", 0.0, null, "", "bytes");
    public final Counter bytesInSendBufferHigh = new Counter(this, "bytes-in-send-buffer-high", 0.0, null, "", "bytes");
    public final Counter bytesInSendBufferMedium = new Counter(this, "bytes-in-send-buffer-medium", 0.0, null, "", "bytes");
    public final Counter bytesInSendBufferLow = new Counter(this, "bytes-in-send-buffer-low", 0.0, null, "", "bytes");
    public final Counter messagesInResendBuffer = new Counter(this, "messages-in-resend-buffer", 0.0, null, "", "packets");
    public final Counter bytesInResendBuffer = new Counter(this, "bytes-in-resend-buffer", 0.0, null, "", "bytes");
    public final Counter packetLossLastSecond = new Counter(this, "packet-loss-last-second", 0.0, null, "", "packets");
    public final Counter packetLossTotal = new Counter(this, "packet-loss-total", 0.0, null, "", "packets");
    public final Counter voipReceived = new Counter(this, "voip-received", 0.0, null, "", "bytes");
    public final Counter voipSent = new Counter(this, "voip-sent", 0.0, null, "", "bytes");

    public NetworkStatistic(String application) {
        super(application);
    }

    public static NetworkStatistic getInstance() {
        return instance;
    }

    @Override
    public void update() {
        if (GameServer.server) {
            for (UdpConnection connection : GameServer.udpEngine.connections) {
                this.updateConnection(connection);
            }
        } else if (GameClient.client) {
            this.updateConnection(GameClient.connection);
        }

        super.update();
    }

    private void updateConnection(UdpConnection connection) {
        long period = ServerOptions.getInstance().multiplayerStatisticsPeriod.getValue() * 1000L;
        long[] statsVOIP = new long[]{-1L, -1L};
        ZNetStatistics stats = connection.getStatistics();
        if (stats != null) {
            this.lastUserMessageBytesPushed.increase(connection.netStatistics.lastUserMessageBytesPushed);
            this.lastUserMessageBytesSent.increase(connection.netStatistics.lastUserMessageBytesSent);
            this.lastUserMessageBytesResent.increase(connection.netStatistics.lastUserMessageBytesResent);
            this.lastUserMessageBytesReceivedProcessed.increase(connection.netStatistics.lastUserMessageBytesReceivedProcessed);
            this.lastUserMessageBytesReceivedIgnored.increase(connection.netStatistics.lastUserMessageBytesReceivedIgnored);
            this.lastActualBytesSent.increase(connection.netStatistics.lastActualBytesSent);
            this.lastActualBytesReceived.increase(connection.netStatistics.lastActualBytesReceived);
            this.totalUserMessageBytesPushed.increase(connection.netStatistics.totalUserMessageBytesPushed);
            this.totalUserMessageBytesSent.increase(connection.netStatistics.totalUserMessageBytesSent);
            this.totalUserMessageBytesResent.increase(connection.netStatistics.totalUserMessageBytesResent);
            this.totalUserMessageBytesReceivedProcessed.increase(connection.netStatistics.totalUserMessageBytesReceivedProcessed);
            this.totalUserMessageBytesReceivedIgnored.increase(connection.netStatistics.totalUserMessageBytesReceivedIgnored);
            this.totalActualBytesSent.increase(connection.netStatistics.totalActualBytesSent);
            this.totalActualBytesReceived.increase(connection.netStatistics.totalActualBytesReceived);
            this.connectionStartTime.increase(connection.netStatistics.connectionStartTime);
            this.bpsLimitByCongestionControl.increase(connection.netStatistics.bpsLimitByCongestionControl);
            this.bpsLimitByOutgoingBandwidthLimit.increase(connection.netStatistics.bpsLimitByOutgoingBandwidthLimit);
            this.messageInSendBufferImmediate.increase(connection.netStatistics.messageInSendBufferImmediate);
            this.messageInSendBufferHigh.increase(connection.netStatistics.messageInSendBufferHigh);
            this.messageInSendBufferMedium.increase(connection.netStatistics.messageInSendBufferMedium);
            this.messageInSendBufferLow.increase(connection.netStatistics.messageInSendBufferLow);
            this.bytesInSendBufferImmediate.increase(connection.netStatistics.bytesInSendBufferImmediate);
            this.bytesInSendBufferHigh.increase(connection.netStatistics.bytesInSendBufferHigh);
            this.bytesInSendBufferMedium.increase(connection.netStatistics.bytesInSendBufferMedium);
            this.bytesInSendBufferLow.increase(connection.netStatistics.bytesInSendBufferLow);
            this.messagesInResendBuffer.increase(connection.netStatistics.messagesInResendBuffer);
            this.bytesInResendBuffer.increase(connection.netStatistics.bytesInResendBuffer);
            this.packetLossLastSecond.increase(connection.netStatistics.packetlossLastSecond);
            this.packetLossTotal.increase(connection.netStatistics.packetlossTotal);
        }

        if (RakVoice.GetChannelStatistics(connection.getConnectedGUID(), statsVOIP)) {
            this.voipReceived.increase(statsVOIP[0]);
            this.voipSent.increase(statsVOIP[1]);
        }

        this.voipReceived.set(this.voipReceived.get() / period);
        this.voipSent.set(this.voipSent.get() / period);
    }

    public void addIncomePacket(short ID, int size, UdpConnection connection) {
        PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(ID);
        if (packetType != null) {
            StatisticManager.getInstance().observeServerPacketProcessDuration(packetType.name(), connection.username == null ? "" : connection.username, size);
            this.receivedPackets.increase();
            this.receivedBytes.increase(size);
            this.receivedBps.increase(size);
            if (this.receivedBps.get() > this.maxReceivedBps.get()) {
                this.maxReceivedBps.increase(this.receivedBps.get());
            }

            this.receivedBps.clear();
        }
    }

    public void addOutcomePacket(short ID, int size, UdpConnection connection) {
        PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(ID);
        if (packetType != null) {
            StatisticManager.getInstance().observeServerPacketSendDuration(packetType.name(), connection.username == null ? "" : connection.username, size);
            this.sentPackets.increase();
            this.sentBytes.increase(size);
            this.sentBps.increase(size);
            if (this.sentBps.get() > this.maxSentBps.get()) {
                this.maxSentBps.increase(this.sentBps.get());
            }

            this.sentBps.clear();
        }
    }
}
