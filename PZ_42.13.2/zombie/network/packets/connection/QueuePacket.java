// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.nio.ByteBuffer;
import java.util.HashMap;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.LoadingQueueState;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.LoginQueue;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.ConnectionQueueStatistic;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 5)
public class QueuePacket implements INetworkPacket {
    @JSONField
    private QueuePacket.MessageType type = QueuePacket.MessageType.None;
    @JSONField
    private byte placeInQueue;
    @JSONField
    private short maxPlayers;
    @JSONField
    private short countPlayers;
    @JSONField
    private int countZombies;
    @JSONField
    private int zombiesKilledByFireToday;
    @JSONField
    private int zombiesKilledToday;
    @JSONField
    private int zombifiedPlayersToday;
    @JSONField
    private int playersKilledByFireToday;
    @JSONField
    private int playersKilledByZombieToday;
    @JSONField
    private int playersKilledByPlayerToday;
    @JSONField
    private int burnedCorpsesToday;
    @JSONField
    private byte serverTimeMinutes;
    @JSONField
    private byte serverTimeHour;
    @JSONField
    private byte serverTimeDay;
    @JSONField
    private byte serverTimeMonth;
    @JSONField
    private short serverTimeYear;
    @JSONField
    private float serverClimateBaseTemperature;
    @JSONField
    private float serverClimateHumidity;
    @JSONField
    private float serverClimateRainIntensity;
    @JSONField
    private float serverClimateWindspeedKph;
    @JSONField
    private float serverClimateWindAngleDegrees;
    @JSONField
    private float serverClimateFogIntensity;
    @JSONField
    private byte serverClimateSeasonId;
    @JSONField
    private byte serverClimateMoon;

    public void setInformationFields() {
        if (!ClimateManager.getInstance().isUpdated()) {
            ClimateManager.getInstance().update();
        }

        this.maxPlayers = (short)ServerOptions.getInstance().getMaxPlayers();
        this.countPlayers = (short)LoginQueue.getCountPlayers();
        this.countZombies = ServerMap.instance.zombieMap.size();
        this.zombiesKilledByFireToday = (int)ConnectionQueueStatistic.getInstance().zombiesKilledByFireToday.get();
        this.zombiesKilledToday = (int)ConnectionQueueStatistic.getInstance().zombiesKilledToday.get();
        this.zombifiedPlayersToday = (int)ConnectionQueueStatistic.getInstance().zombifiedPlayersToday.get();
        this.playersKilledByFireToday = (int)ConnectionQueueStatistic.getInstance().playersKilledByFireToday.get();
        this.playersKilledByZombieToday = (int)ConnectionQueueStatistic.getInstance().playersKilledByZombieToday.get();
        this.playersKilledByPlayerToday = (int)ConnectionQueueStatistic.getInstance().playersKilledByPlayerToday.get();
        this.burnedCorpsesToday = (int)ConnectionQueueStatistic.getInstance().burnedCorpsesToday.get();
        this.serverTimeMinutes = (byte)GameTime.getInstance().getMinutes();
        this.serverTimeHour = (byte)GameTime.getInstance().getHour();
        this.serverTimeDay = (byte)GameTime.getInstance().getDay();
        this.serverTimeMonth = (byte)GameTime.getInstance().getMonth();
        this.serverTimeYear = (short)GameTime.getInstance().getYear();
        this.serverClimateBaseTemperature = ClimateManager.getInstance().getBaseTemperature();
        this.serverClimateHumidity = ClimateManager.getInstance().getHumidity();
        this.serverClimateRainIntensity = ClimateManager.getInstance().getRainIntensity();
        this.serverClimateWindspeedKph = ClimateManager.getInstance().getWindspeedKph();
        this.serverClimateWindAngleDegrees = ClimateManager.getInstance().getWindAngleDegrees();
        this.serverClimateFogIntensity = ClimateManager.getInstance().getFogIntensity();
        this.serverClimateSeasonId = ClimateManager.getInstance().getSeasonId();
        this.serverClimateMoon = (byte)ClimateMoon.getInstance().getCurrentMoonPhase();
    }

    public void setPlaceInQueue(byte placeInQueue) {
        this.type = QueuePacket.MessageType.PlaceInQueue;
        this.placeInQueue = placeInQueue;
    }

    public void setConnectionImmediate() {
        this.type = QueuePacket.MessageType.ConnectionImmediate;
        this.placeInQueue = 0;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LoginQueue.receiveServerLoginQueueRequest(connection);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        switch (this.type) {
            case ConnectionImmediate:
                LoadingQueueState.onConnectionImmediate();
                break;
            case PlaceInQueue:
                HashMap<String, Object> data = new HashMap<>();
                data.put("maxPlayers", this.maxPlayers);
                data.put("countPlayers", this.countPlayers);
                data.put("countZombies", this.countZombies);
                data.put("ZombiesKilledByFireToday", this.zombiesKilledByFireToday);
                data.put("ZombiesKilledToday", this.zombiesKilledToday);
                data.put("ZombifiedPlayersToday", this.zombifiedPlayersToday);
                data.put("PlayersKilledByFireToday", this.playersKilledByFireToday);
                data.put("PlayersKilledByZombieToday", this.playersKilledByZombieToday);
                data.put("PlayersKilledByPlayerToday", this.playersKilledByPlayerToday);
                data.put("BurnedCorpsesToday", this.burnedCorpsesToday);
                data.put("Minutes", this.serverTimeMinutes);
                data.put("Hour", this.serverTimeHour);
                data.put("Day", this.serverTimeDay + 1);
                data.put("Month", this.serverTimeMonth + 1);
                data.put("Year", this.serverTimeYear);
                data.put("Temperature", this.serverClimateBaseTemperature);
                data.put("Humidity", this.serverClimateHumidity);
                data.put("Rain", this.serverClimateRainIntensity);
                data.put("WindspeedKph", this.serverClimateWindspeedKph);
                data.put("WindAngleDegrees", this.serverClimateWindAngleDegrees);
                data.put("Fog", this.serverClimateFogIntensity);
                data.put("SeasonId", this.serverClimateSeasonId);
                data.put("Moon", this.serverClimateMoon);
                LuaEventManager.triggerEvent("OnConnectionStateChanged", "FormatMessage", "PlaceInQueue", this.placeInQueue);
                LoadingQueueState.onPlaceInQueue(this.placeInQueue, data);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (!GameServer.server) {
            this.type = QueuePacket.MessageType.values()[b.get()];
            if (this.type == QueuePacket.MessageType.PlaceInQueue) {
                this.placeInQueue = b.get();
                this.maxPlayers = b.getShort();
                this.countPlayers = b.getShort();
                this.countZombies = b.getInt();
                this.zombiesKilledByFireToday = b.getInt();
                this.zombiesKilledToday = b.getInt();
                this.zombifiedPlayersToday = b.getInt();
                this.playersKilledByFireToday = b.getInt();
                this.playersKilledByZombieToday = b.getInt();
                this.playersKilledByPlayerToday = b.getInt();
                this.burnedCorpsesToday = b.getInt();
                this.serverTimeMinutes = b.get();
                this.serverTimeHour = b.get();
                this.serverTimeDay = b.get();
                this.serverTimeMonth = b.get();
                this.serverTimeYear = b.getShort();
                this.serverClimateBaseTemperature = b.getFloat();
                this.serverClimateHumidity = b.getFloat();
                this.serverClimateRainIntensity = b.getFloat();
                this.serverClimateWindspeedKph = b.getFloat();
                this.serverClimateWindAngleDegrees = b.getFloat();
                this.serverClimateFogIntensity = b.getFloat();
                this.serverClimateSeasonId = b.get();
                this.serverClimateMoon = b.get();
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.type.ordinal());
        if (this.type == QueuePacket.MessageType.PlaceInQueue) {
            b.putByte(this.placeInQueue);
            b.putShort(this.maxPlayers);
            b.putShort(this.countPlayers);
            b.putInt(this.countZombies);
            b.putInt(this.zombiesKilledByFireToday);
            b.putInt(this.zombiesKilledToday);
            b.putInt(this.zombifiedPlayersToday);
            b.putInt(this.playersKilledByFireToday);
            b.putInt(this.playersKilledByZombieToday);
            b.putInt(this.playersKilledByPlayerToday);
            b.putInt(this.burnedCorpsesToday);
            b.putByte(this.serverTimeMinutes);
            b.putByte(this.serverTimeHour);
            b.putByte(this.serverTimeDay);
            b.putByte(this.serverTimeMonth);
            b.putShort(this.serverTimeYear);
            b.putFloat(this.serverClimateBaseTemperature);
            b.putFloat(this.serverClimateHumidity);
            b.putFloat(this.serverClimateRainIntensity);
            b.putFloat(this.serverClimateWindspeedKph);
            b.putFloat(this.serverClimateWindAngleDegrees);
            b.putFloat(this.serverClimateFogIntensity);
            b.putByte(this.serverClimateSeasonId);
            b.putByte(this.serverClimateMoon);
        }
    }

    public static enum MessageType {
        None,
        ConnectionImmediate,
        PlaceInQueue;
    }
}
