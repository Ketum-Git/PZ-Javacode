// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.map.hash.TShortShortHashMap;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.utils.UpdateLimit;
import zombie.core.utils.UpdateTimer;
import zombie.core.znet.ZNetStatistics;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector3;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketsCache;
import zombie.network.PlayerDownloadServer;
import zombie.network.anticheats.PacketValidator;
import zombie.network.server.EventManager;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public class UdpConnection extends PacketsCache implements IConnection {
    private static final long PLAYER_SAVE_INTERVAL = 180000L;
    private static final long ZOMBIE_LIST_REFRESH_INTERVAL = 3000L;
    final Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(1000000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    final Lock bufferLockPing = new ReentrantLock();
    private final ByteBuffer bbPing = ByteBuffer.allocate(50);
    private final ByteBufferWriter bbwPing = new ByteBufferWriter(this.bbPing);
    private final long connectedGuid;
    UdpEngine engine;
    private int index;
    private boolean allChatMuted;
    private String userName;
    public String[] usernames = new String[4];
    private byte relevantRange;
    private Role role;
    public static HashMap<String, Long> lastConnections = new HashMap<>();
    private long lastConnection;
    public long lastUnauthorizedPacket;
    private String ip;
    private boolean wasInLoadingQueue;
    public String password;
    private boolean ping;
    public Vector3[] releventPos = new Vector3[4];
    public short[] playerIds = new short[4];
    public IsoPlayer[] players = new IsoPlayer[4];
    public Vector3[] connectArea = new Vector3[4];
    private int chunkGridWidth;
    private final ClientServerMap[] loadedCells = new ClientServerMap[4];
    private PlayerDownloadServer playerDownloadServer;
    private int zombieListHash;
    private boolean isNeighborPlayer;
    public UdpConnection.ChecksumState checksumState = UdpConnection.ChecksumState.Init;
    public long checksumTime;
    public boolean awaitingCoopApprove;
    private long steamId;
    private long ownerId;
    private String idStr;
    public boolean isCoopHost;
    private int maxPlayers;
    public final TShortArrayList chunkObjectStateRequests = new TShortArrayList();
    public final TShortShortHashMap vehicleRequests = new TShortShortHashMap();
    public final TShortObjectHashMap<BaseVehicle.ServerVehicleState> vehicleStates = new TShortObjectHashMap<>();
    public final TShortObjectHashMap<IsoObject> thumpHits = new TShortObjectHashMap<>();
    public ZNetStatistics netStatistics;
    public final Deque<Integer> pingHistory = new ArrayDeque<>();
    private int averagePing;
    private int lowestPing;
    private int lastPing;
    private final PacketValidator validator = new PacketValidator(this);
    public boolean statisticTransmissionEnabled;
    public final UpdateLimit playerSave = new UpdateLimit(180000L);
    private static final long CONNECTION_ATTEMPT_TIMEOUT = 5000L;
    private static final long CONNECTION_GOOGLE_AUTH_TIMEOUT = 60000L;
    public static final long CONNECTION_GRACE_INTERVAL = 60000L;
    public static final long CONNECTION_READY_INTERVAL = 15000L;
    public long connectionTimestamp;
    public boolean googleAuth;
    private boolean ready;
    public UpdateTimer timerSendZombie = new UpdateTimer();
    public final UpdateLimit zombieListRefresh = new UpdateLimit(3000L);
    public HashMap<Short, UpdateLimit> timerUpdateAnimal = new HashMap<>();
    private boolean fullyConnected;

    public UdpConnection(UdpEngine engine, long connectedGuid, int index) {
        this.engine = engine;
        this.connectedGuid = connectedGuid;
        this.index = index;
        this.releventPos[0] = new Vector3();

        for (int i = 0; i < 4; i++) {
            this.playerIds[i] = -1;
        }

        this.setConnectionTimestamp(15000L);
        this.wasInLoadingQueue = false;
    }

    public int getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public RakNetPeerInterface getPeer() {
        return this.engine.peer;
    }

    @Override
    public long getConnectedGUID() {
        return this.connectedGuid;
    }

    @Override
    public String getIDStr() {
        return this.idStr;
    }

    public void setIDStr(String idStr) {
        this.idStr = idStr;
    }

    public long getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public void setPinged(boolean ping) {
        this.ping = ping;
    }

    @Override
    public boolean wasInLoadingQueue() {
        return this.wasInLoadingQueue;
    }

    public void setWasInLoadingQueue(boolean wasInLoadingQueue) {
        this.wasInLoadingQueue = wasInLoadingQueue;
    }

    @Override
    public PlayerDownloadServer getPlayerDownloadServer() {
        return this.playerDownloadServer;
    }

    public void setPlayerDownloadServer(PlayerDownloadServer playerDownloadServer) {
        this.playerDownloadServer = playerDownloadServer;
    }

    public int getZombieListHash() {
        return this.zombieListHash;
    }

    public void setZombieListHash(int zombieListHash) {
        this.zombieListHash = zombieListHash;
    }

    public boolean isNeighborPlayer() {
        return this.isNeighborPlayer;
    }

    public String getServerIP() {
        return this.engine.getServerIP();
    }

    @Override
    public IsoPlayer getPlayerAt(int playerIndex) {
        return this.players[playerIndex];
    }

    @Override
    public void setPlayerAt(int playerIndex, IsoPlayer player) {
        this.players[playerIndex] = player;
    }

    @Override
    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bbw.clear();
        return this.bbw;
    }

    public ByteBufferWriter startPingPacket() {
        this.bufferLockPing.lock();
        this.bbwPing.clear();
        return this.bbwPing;
    }

    @Override
    public boolean isRelevantTo(float x, float y) {
        for (int n = 0; n < 4; n++) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= minX && x < maxX && y >= minY && y < maxY) {
                    return true;
                }
            }

            if (this.releventPos[n] != null
                && Math.abs(this.releventPos[n].x - x) <= this.relevantRange * 8
                && Math.abs(this.releventPos[n].y - y) <= this.relevantRange * 8) {
                return true;
            }
        }

        return false;
    }

    public float getRelevantAndDistance(float x, float y, float z) {
        for (int n = 0; n < 4; n++) {
            if (this.releventPos[n] != null
                && Math.abs(this.releventPos[n].x - x) <= this.relevantRange * 8
                && Math.abs(this.releventPos[n].y - y) <= this.relevantRange * 8) {
                return IsoUtils.DistanceTo(this.releventPos[n].x, this.releventPos[n].y, x, y);
            }
        }

        return Float.POSITIVE_INFINITY;
    }

    public boolean RelevantToPlayerIndex(int n, float x, float y) {
        if (this.connectArea[n] != null) {
            int chunkMapWidth = (int)this.connectArea[n].z;
            int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
            int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
            int maxX = minX + chunkMapWidth * 8;
            int maxY = minY + chunkMapWidth * 8;
            if (x >= minX && x < maxX && y >= minY && y < maxY) {
                return true;
            }
        }

        return this.releventPos[n] != null
            && Math.abs(this.releventPos[n].x - x) <= this.relevantRange * 8
            && Math.abs(this.releventPos[n].y - y) <= this.relevantRange * 8;
    }

    public boolean RelevantTo(float x, float y, float radius) {
        for (int n = 0; n < 4; n++) {
            if (this.connectArea[n] != null) {
                int chunkMapWidth = (int)this.connectArea[n].z;
                int minX = PZMath.fastfloor(this.connectArea[n].x - chunkMapWidth / 2) * 8;
                int minY = PZMath.fastfloor(this.connectArea[n].y - chunkMapWidth / 2) * 8;
                int maxX = minX + chunkMapWidth * 8;
                int maxY = minY + chunkMapWidth * 8;
                if (x >= minX && x < maxX && y >= minY && y < maxY) {
                    return true;
                }
            }

            if (this.releventPos[n] != null && Math.abs(this.releventPos[n].x - x) <= radius && Math.abs(this.releventPos[n].y - y) <= radius) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void cancelPacket() {
        this.bufferLock.unlock();
    }

    @Override
    public long getLastConnection() {
        return this.lastConnection;
    }

    public void setLastConnection(long lastConnection) {
        this.lastConnection = lastConnection;
    }

    public int getBufferPosition() {
        return this.bb.position();
    }

    @Override
    public void endPacket(int priority, int reliability, byte ordering) {
        this.endPacket(this.bb, priority, reliability, ordering, this.bufferLock);
    }

    private void endPacket(ByteBuffer bb, int priority, int reliability, byte ordering, Lock lock) {
        int currentPosition = bb.position();
        bb.position(1);
        NetworkStatistic.getInstance().addOutcomePacket(bb.getShort(), currentPosition, this);
        bb.position(currentPosition);
        this.flipSendUnlock(bb, priority, reliability, ordering, lock);
    }

    private void flipSendUnlock(ByteBuffer bb, int priority, int reliability, byte ordering, Lock lock) {
        bb.flip();
        this.engine.peer.Send(bb, priority, reliability, ordering, this.connectedGuid, false);
        lock.unlock();
    }

    @Override
    public String getUserName() {
        return this.userName;
    }

    @Override
    public String getUserName(int playerIndex) {
        return this.usernames[playerIndex];
    }

    @Override
    public void setRelevantRange(byte b) {
        this.relevantRange = b;
    }

    public byte getRelevantRange() {
        return this.relevantRange;
    }

    public void endPacket() {
        this.endPacket(1, 3, (byte)0);
    }

    public void endPacketImmediate() {
        this.endPacket(0, 3, (byte)0);
    }

    public void endPacketUnordered() {
        this.endPacket(2, 2, (byte)0);
    }

    public void endPacketUnreliable() {
        this.flipSendUnlock(this.bb, 2, 1, (byte)0, this.bufferLock);
    }

    public void endPacketSuperHighUnreliable() {
        this.endPacket(0, 1, (byte)0);
    }

    public void endPingPacket() {
        this.endPacket(this.bbPing, 0, 1, (byte)0, this.bufferLockPing);
    }

    public InetSocketAddress getInetSocketAddress() {
        String ip = this.engine.peer.getIPFromGUID(this.connectedGuid);
        if ("UNASSIGNED_SYSTEM_ADDRESS".equals(ip)) {
            return null;
        } else {
            ip = ip.replace("|", "\u00a3");
            String[] spl = ip.split("\u00a3");
            return new InetSocketAddress(spl[0], Integer.parseInt(spl[1]));
        }
    }

    @Override
    public void forceDisconnect(String description) {
        if (!GameServer.server) {
            GameClient.instance.disconnect(!"receive-CustomizationData".equals(description) && !"lua-connect".equals(description));
        }

        this.engine.forceDisconnect(this.getConnectedGUID(), description);
        ConnectionManager.log("force-disconnect", description, this);
    }

    @Override
    public void setFullyConnected() {
        this.validator.playerUpdateTimeoutReset();
        this.fullyConnected = true;
        this.playerSave.Reset();
        this.setConnectionTimestamp(15000L);
        ConnectionManager.log("fully-connected", "", this);
        EventManager.instance().report("[" + this.userName + "] connected to server");
    }

    @Override
    public long getSteamId() {
        return this.steamId;
    }

    public void setSteamId(long steamId) {
        this.steamId = steamId;
    }

    @Override
    public String getIP() {
        return this.ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    @Override
    public short getPlayerId(int playerIndex) {
        return this.playerIds[playerIndex];
    }

    @Override
    public void setChunkGridWidth(int range) {
        this.chunkGridWidth = range;
    }

    @Override
    public void setLoadedCells(int playerIndex, ClientServerMap clientServerMap) {
        this.loadedCells[playerIndex] = clientServerMap;
    }

    @Override
    public ClientServerMap getLoadedCell(int playerIndex) {
        return this.loadedCells[playerIndex];
    }

    @Override
    public void addChunkObjectState(short w) {
        this.chunkObjectStateRequests.add(w);
    }

    @Override
    public void setConnectArea(int playerIndex, Vector3 connectArea) {
        this.connectArea[playerIndex] = connectArea;
    }

    @Override
    public Vector3 getRelevantPos(int playerIndex) {
        return this.releventPos[playerIndex];
    }

    @Override
    public void setConnectionTimestamp(long interval) {
        this.connectionTimestamp = System.currentTimeMillis() + interval;
        this.setReady(false);
    }

    @Override
    public PacketValidator getValidator() {
        return this.validator;
    }

    @Override
    public int getChunkGridWidth() {
        return this.chunkGridWidth;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isAllChatMuted() {
        return this.allChatMuted;
    }

    public void setAllChatMuted(boolean allChatMuted) {
        this.allChatMuted = allChatMuted;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void setUserName(int playerIndex, String userName) {
        this.usernames[playerIndex] = userName;
    }

    @Override
    public void setRelevantPos(int playerIndex, Vector3 vector3) {
        this.releventPos[playerIndex] = vector3;
    }

    @Override
    public void setPlayerId(int playerIndex, short playerID) {
        this.playerIds[playerIndex] = playerID;
    }

    public void checkReady() {
        if (!this.ready) {
            boolean ready = System.currentTimeMillis() > this.connectionTimestamp;
            if (this.ready != ready) {
                this.setReady(ready);
            }

            if (!ready) {
                IsoPlayer.getInstance().setAlpha(0, 0.6F);
            }
        }
    }

    public boolean isReady() {
        return this.ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isGoogleAuthTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isConnectionAttemptTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 5000L;
    }

    public boolean isConnectionGraceIntervalTimeout() {
        return System.currentTimeMillis() > this.connectionTimestamp + 60000L;
    }

    public boolean isFullyConnected() {
        return this.fullyConnected;
    }

    public void calcCountPlayersInRelevantPosition() {
        if (this.isFullyConnected()) {
            boolean isNeighborPlayer = false;

            for (int j = 0; j < GameServer.udpEngine.connections.size(); j++) {
                UdpConnection c2 = GameServer.udpEngine.connections.get(j);
                if (c2.isFullyConnected() && c2 != this) {
                    for (int k = 0; k < c2.players.length; k++) {
                        IsoPlayer p = c2.players[k];
                        if (p != null && this.RelevantTo(p.getX(), p.getY(), 120.0F)) {
                            isNeighborPlayer = true;
                        }
                    }

                    if (isNeighborPlayer) {
                        break;
                    }
                }
            }

            this.isNeighborPlayer = isNeighborPlayer;
        }
    }

    public ZNetStatistics getStatistics() {
        try {
            this.netStatistics = this.engine.peer.GetNetStatistics(this.connectedGuid);
        } catch (Exception var2) {
            return null;
        }

        return this.netStatistics;
    }

    public void updatePing() {
        this.averagePing = this.engine.peer.GetAveragePing(this.connectedGuid);
        this.lowestPing = this.engine.peer.GetLowestPing(this.connectedGuid);
        this.lastPing = this.engine.peer.GetLastPing(this.connectedGuid);
    }

    public int getAveragePing() {
        return this.averagePing;
    }

    public int getLastPing() {
        return this.lastPing;
    }

    public int getLowestPing() {
        return this.lowestPing;
    }

    @Override
    public int getMTUSize() {
        return this.engine.peer.GetMTUSize(this.connectedGuid);
    }

    public UdpConnection.ConnectionType getConnectionType() {
        return UdpConnection.ConnectionType.values()[this.engine.peer.GetConnectionType(this.connectedGuid)];
    }

    @Override
    public String getDescription() {
        return GameServer.server
            ? String.format(
                "guid=\"%s\" ip=\"%s\" steam-id=\"%s\" role=\"%s\" username=\"%s\" connection-type=\"%s\"",
                this.getConnectedGUID(),
                this.getIP(),
                this.getSteamId(),
                this.getRoleName(),
                this.getUserName(),
                this.getConnectionType()
            )
            : String.format("guid=%s", this.getConnectedGUID());
    }

    @Override
    public boolean hasPlayer(String userName) {
        if (StringUtils.isNullOrEmpty(userName)) {
            return false;
        } else {
            for (int i = 0; i < this.players.length; i++) {
                if (this.players[i] != null && userName.equals(this.players[i].getUsername())) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean hasPlayer(short playerId) {
        for (int i = 0; i < this.players.length; i++) {
            if (this.players[i] != null && this.players[i].getOnlineID() == playerId) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Role getRole() {
        return this.role;
    }

    public String getRoleName() {
        return this.role == null ? "" : this.role.getName();
    }

    @Override
    public void setRole(Role role) {
        this.role = role;
    }

    public boolean havePlayer(IsoPlayer p) {
        if (p == null) {
            return false;
        } else {
            for (int i = 0; i < this.players.length; i++) {
                if (this.players[i] == p) {
                    return true;
                }
            }

            return false;
        }
    }

    public byte getPlayerIndex(IsoPlayer p) {
        for (byte i = 0; i < this.playerIds.length; i++) {
            if (this.playerIds[i] == p.onlineId) {
                return i;
            }
        }

        return -1;
    }

    public static enum ChecksumState {
        Init,
        Different,
        Done;
    }

    public static enum ConnectionType {
        Disconnected,
        UDPRakNet,
        Steam;
    }
}
