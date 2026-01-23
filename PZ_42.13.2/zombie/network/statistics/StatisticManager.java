// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics;

import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.datapoints.GaugeDataPoint;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;
import zombie.network.statistics.data.GameStatistic;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.network.statistics.data.PerformanceStatistic;
import zombie.network.statistics.data.Statistic;

public class StatisticManager implements Iterable<Statistic> {
    private static final StatisticManager instance = new StatisticManager();
    static final HashMap<String, Statistic> statistics = new HashMap<>();
    private final UpdateLimit updateLimit = new UpdateLimit(1000L);
    public boolean prometheusEnabled;
    private Histogram serverPacketReceive;
    private Histogram serverPacketSend;

    public static StatisticManager getInstance() {
        return instance;
    }

    @Override
    public Iterator<Statistic> iterator() {
        return statistics.values().iterator();
    }

    public void init() {
        statistics.put(PerformanceStatistic.getInstance().getName(), PerformanceStatistic.getInstance());
        statistics.put(NetworkStatistic.getInstance().getName(), NetworkStatistic.getInstance());
        statistics.put(GameStatistic.getInstance().getName(), GameStatistic.getInstance());
        if (GameServer.server) {
            statistics.put(ConnectionQueueStatistic.getInstance().getName(), ConnectionQueueStatistic.getInstance());
        }

        String prometheusPort = System.getProperty("prometheusPort");
        if (prometheusPort != null) {
            JvmMetrics.builder().register();

            for (String statisticName : statistics.keySet()) {
                Statistic statistic = statistics.get(statisticName);
                statistic.prometheus = (Gauge)Gauge.builder().name(statisticName.replace("-", "_")).labelNames("parameter").register();
            }

            Info pzVersionInfo = (Info)Info.builder()
                .name("pz_info")
                .help("PZ server info")
                .labelNames("version", "ip", "ServerName", "checksum", "gamePort", "UDPPort")
                .register();
            pzVersionInfo.setLabelValues(
                Core.getInstance().getVersion(),
                GameServer.ip,
                GameServer.serverName,
                GameServer.checksum,
                String.valueOf(GameServer.defaultPort),
                String.valueOf(GameServer.udpPort)
            );
            this.serverPacketReceive = (Histogram)Histogram.builder().name("packet_receive").unit(Unit.BYTES).labelNames("packetType", "client").register();
            this.serverPacketSend = (Histogram)Histogram.builder().name("packet_send").unit(Unit.BYTES).labelNames("packetType", "client").register();
            HTTPServer server = null;

            try {
                server = HTTPServer.builder().port(Integer.parseInt(prometheusPort)).buildAndStart();
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }

            System.out.println("Prometheus HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");
            this.prometheusEnabled = true;
        }
    }

    public static String getInstanceName() {
        String prometheusPort = System.getProperty("prometheusPort");
        String prometheusHost = System.getProperty("prometheusHost");
        if (prometheusPort == null) {
            return prometheusHost == null ? GameServer.ip : prometheusHost;
        } else {
            return (prometheusHost == null ? GameServer.ip : prometheusHost) + ":" + prometheusPort;
        }
    }

    public void observeServerPacketProcessDuration(String packetType, String client, int size) {
        if (this.prometheusEnabled) {
            ((DistributionDataPoint)this.serverPacketReceive.labelValues(new String[]{packetType, client})).observe(size);
        }
    }

    public void observeServerPacketSendDuration(String packetType, String client, int size) {
        if (this.prometheusEnabled) {
            ((DistributionDataPoint)this.serverPacketSend.labelValues(new String[]{packetType, client})).observe(size);
        }
    }

    public Statistic get(String name) {
        return statistics.get(name);
    }

    public void update(long time) {
        PerformanceStatistic.getInstance().addUpdate(time);
        int period = ServerOptions.getInstance().multiplayerStatisticsPeriod.getValue() * 1000;
        if (period != 0 && this.updateLimit.Check()) {
            this.updateLimit.Reset(period);

            for (Statistic statistic : statistics.values()) {
                statistic.update();
                if (this.prometheusEnabled) {
                    for (String name : statistic.statistics.keySet()) {
                        Double val = statistic.statistics.get(name);
                        ((GaugeDataPoint)statistic.prometheus.labelValues(new String[]{name})).set(val);
                    }
                }
            }

            if (GameServer.server) {
                INetworkPacket.sendByCapability(PacketTypes.PacketType.Statistics, Capability.GetStatistic);
            }
        }
    }
}
