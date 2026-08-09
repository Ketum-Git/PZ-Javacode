// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import java.net.ConnectException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.secure.PZcrypt;
import zombie.core.znet.SteamUser;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.RequestDataManager;
import zombie.network.anticheats.AntiCheat;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombieManager;

public class UdpEngine {
    private final int maxConnections;
    private final Map<Long, UdpConnection> connectionMap = new HashMap<>();
    public final List<UdpConnection> connections = new ArrayList<>();
    protected final RakNetPeerInterface peer;
    final boolean server;
    Lock bufferLock = new ReentrantLock();
    private final ByteBuffer bb = ByteBuffer.allocate(500000);
    private final ByteBufferWriter bbw = new ByteBufferWriter(this.bb);
    public int port;
    private final Thread thread;
    private volatile boolean quit;
    UdpConnection[] connectionArray = new UdpConnection[256];
    ByteBufferReader buf = new ByteBufferReader(ByteBuffer.allocate(1000000));

    public UdpEngine(int port, int udpPort, int maxConnections, String serverPassword, boolean bListen) throws ConnectException {
        this.port = port;
        this.peer = new RakNetPeerInterface();
        DebugType.Network.println("Initialising RakNet...");
        this.peer.Init(SteamUtils.isSteamModeEnabled());
        this.peer.SetMaximumIncomingConnections(maxConnections);
        this.server = bListen;
        if (this.server) {
            if (GameServer.ipCommandline != null) {
                this.peer.SetServerIP(GameServer.ipCommandline);
            }

            this.peer.SetServerPort(port, udpPort);
            this.peer.SetIncomingPassword(this.hashServerPassword(serverPassword));
        } else {
            this.peer.SetClientPort(GameServer.defaultPort + Rand.Next(10000) + 1234);
        }

        this.peer.SetOccasionalPing(true);
        this.maxConnections = maxConnections;
        int startupResult = this.peer.Startup(maxConnections);
        DebugType.Network.debugln("RakNet.Startup() return code: %s (0 means success)", startupResult);
        if (startupResult != 0) {
            throw new ConnectException("Connection Startup Failed. Code: " + startupResult);
        } else {
            if (bListen) {
                VoiceManager.instance.InitVMServer();
            }

            this.thread = new Thread(ThreadGroups.Network, this::threadRun, "UdpEngine");
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    private void threadRun() {
        while (!this.quit) {
            ByteBufferReader buffer = this.Receive();
            if (!this.quit) {
                try {
                    this.decode(buffer);
                } catch (Exception var3) {
                    DebugType.Network.printException(var3, "Exception thrown during decode.", LogSeverity.Error);
                }
                continue;
            }
            break;
        }
    }

    public void Shutdown() {
        DebugLog.log("waiting for UdpEngine thread termination");
        this.quit = true;

        while (this.thread.isAlive()) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException var2) {
            }
        }

        this.peer.Shutdown();
    }

    public void SetServerPassword(String password) {
        if (this.peer != null) {
            this.peer.SetIncomingPassword(password);
        }
    }

    public String hashServerPassword(String password) {
        return PZcrypt.hash(password, true);
    }

    public String getServerIP() {
        return this.peer.GetServerIP();
    }

    public long getClientSteamID(long guid) {
        return this.peer.GetClientSteamID(guid);
    }

    public long getClientOwnerSteamID(long guid) {
        return this.peer.GetClientOwnerSteamID(guid);
    }

    public ByteBufferWriter startPacket() {
        this.bufferLock.lock();
        this.bb.clear();
        return this.bbw;
    }

    public void endPacketBroadcast(PacketTypes.PacketType packetType) {
        this.bb.flip();
        this.peer.Send(this.bb, packetType.packetPriority, packetType.packetPriority, (byte)0, -1L, true);
        this.bufferLock.unlock();
    }

    public void endPacketBroadcastExcept(int priority, int reliability, UdpConnection connection) {
        this.bb.flip();
        this.peer.Send(this.bb, priority, reliability, (byte)0, connection.getConnectedGUID(), true);
        this.bufferLock.unlock();
    }

    public void connected() {
        VoiceManager.instance.VoiceConnectReq(GameClient.connection.getConnectedGUID());
        if (GameClient.client) {
            if (!GameClient.askPing && !GameClient.sendQR && !GameClient.askCustomizationData) {
                GameClient.startAuth = Calendar.getInstance();
                INetworkPacket.send(PacketTypes.PacketType.Login);
                RequestDataManager.getInstance().clear();
                ConnectionManager.log("send-packet", "login", GameClient.connection);
            } else if (GameClient.askPing) {
                ByteBufferWriter bb = GameClient.connection.startPacket();
                PacketTypes.PacketType.Ping.doPacket(bb);
                bb.putUTF(GameClient.ip);
                PacketTypes.PacketType.Ping.send(GameClient.connection);
                RequestDataManager.getInstance().clear();
            } else if (GameClient.sendQR) {
                INetworkPacket.send(PacketTypes.PacketType.GoogleAuthKey, "");
            }
        }
    }

    private void decode(ByteBufferReader buf) {
        int packetIdentifier = buf.getByte() & 255;
        switch (packetIdentifier) {
            case 0:
            case 1:
                break;
            case 16: {
                int id = buf.getByte() & 255;
                long guid = this.peer.getGuidOfPacket();
                if (GameClient.client) {
                    GameClient.connection = this.addConnection(id, guid);
                    ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[id]);
                    if (!SteamUtils.isSteamModeEnabled()) {
                        this.connected();
                    } else {
                        GameClient.steamID = SteamUser.GetSteamID();
                    }
                } else {
                    ConnectionManager.log("RakNet", "connection-request-accepted", this.connectionArray[id]);
                }
                break;
            }
            case 17:
                ConnectionManager.log("RakNet", "connection-attempt-failed", 0L);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            case 18:
                ConnectionManager.log("RakNet", "already-connected", 0L);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            case 19: {
                int id = buf.getByte() & 255;
                long guid = this.peer.getGuidOfPacket();
                this.addConnection(id, guid);
                ConnectionManager.log("RakNet", "new-incoming-connection", this.connectionArray[id]);
                break;
            }
            case 20:
                ConnectionManager.log("RakNet", "no-free-incoming-connections", 0L);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            case 21: {
                int id = buf.getByte() & 255;
                long guid = this.peer.getGuidOfPacket();
                ConnectionManager.log("RakNet", "disconnection-notification", guid);
                this.removeConnection(id);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            }
            case 22: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "connection-lost", this.connectionArray[id]);
                this.removeConnection(id);
                break;
            }
            case 23: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "connection-banned", this.connectionArray[id]);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            }
            case 24: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "invalid-password", this.connectionArray[id]);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            }
            case 25:
                ConnectionManager.log("RakNet", "incompatible-protocol-version", 0L);
                String version = buf.getUTF();
                LuaEventManager.triggerEvent("OnConnectionStateChanged", "ClientVersionMismatch", version);
                break;
            case 31: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "remote-disconnection-notification", this.connectionArray[id]);
                break;
            }
            case 32: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "remote-connection-lost", this.connectionArray[id]);
                if (GameClient.client) {
                    GameClient.instance.addDisconnectPacket(packetIdentifier);
                }
                break;
            }
            case 33: {
                int id = buf.getByte() & 255;
                ConnectionManager.log("RakNet", "remote-new-incoming-connection", this.connectionArray[id]);
                break;
            }
            case 44: {
                long guid = this.peer.getGuidOfPacket();
                VoiceManager.instance.VoiceConnectAccept(guid);
                break;
            }
            case 45: {
                long guid = this.peer.getGuidOfPacket();
                VoiceManager.instance.VoiceOpenChannelReply(guid, buf);
                break;
            }
            case 46: {
                long guid = this.peer.getGuidOfPacket();
                UdpConnection connection = this.connectionMap.get(guid);
                DebugType.Voice.println("RakVoice channel is closed \"%s\" guid=%d", connection.getUserName(), connection.getConnectedGUID());
                break;
            }
            case 134:
                short userPacketId = buf.getShort();
                if (GameServer.server) {
                    long guidx = this.peer.getGuidOfPacket();
                    UdpConnection con = this.connectionMap.get(guidx);
                    if (con == null) {
                        DebugType.Network.warn("GOT PACKET FROM UNKNOWN CONNECTION guid=%d packetId=%d", guidx, userPacketId);
                        return;
                    }

                    GameServer.addIncoming(userPacketId, buf, con);
                } else {
                    GameClient.instance.addIncoming(userPacketId, buf);
                }
                break;
            default:
                DebugType.Network.warn("Received unknown packet: %s", packetIdentifier);
                if (GameServer.server) {
                    long guidx = this.peer.getGuidOfPacket();
                    UdpConnection connectionx = this.connectionMap.get(guidx);
                    AntiCheat.PacketRakNet.act(connectionx, packetIdentifier + ": unknown RakNet packet");
                }
        }
    }

    public void removeConnection(int index) {
        UdpConnection con = this.connectionArray[index];
        if (con != null) {
            this.connectionArray[index] = null;
            this.connectionMap.remove(con.getConnectedGUID());
            if (GameClient.client) {
                GameClient.instance.connectionLost();
            } else if (GameServer.server) {
                float cooldown = SafetySystemManager.getCooldown(con);
                if (cooldown > 0.0F) {
                    GameServer.addDelayedDisconnect(con);
                    LoggerManager.getLogger("user")
                        .write(String.format("Connection delayed disconnect index=%d guid=%d id=%s", con.getIndex(), con.getConnectedGUID(), con.getIDStr()));

                    for (IsoPlayer player : con.players) {
                        if (player != null) {
                            player.getNetworkCharacterAI().setDisconnected(true);
                            NetworkZombieManager.getInstance().clearTargetAuth(con, player);
                            INetworkPacket.sendToRelative(PacketTypes.PacketType.PlayerUpdateReliable, con, player.getX(), player.getY(), player);
                        }
                    }
                } else {
                    GameServer.addDisconnect(con);
                    LoggerManager.getLogger("user")
                        .write(String.format("Connection disconnect index=%d guid=%d id=%s", con.getIndex(), con.getConnectedGUID(), con.getIDStr()));
                }
            }
        }
    }

    private UdpConnection addConnection(int id, long guid) {
        UdpConnection connection = new UdpConnection(this, guid, id);
        this.connectionMap.put(guid, connection);
        this.connectionArray[id] = connection;
        if (GameServer.server) {
            GameServer.addConnection(connection);
        }

        return connection;
    }

    public ByteBufferReader Receive() {
        while (!this.quit && !this.peer.Receive(this.buf.bb)) {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException var2) {
                DebugType.General.printException(var2, LogSeverity.Error);
            }
        }

        return this.buf;
    }

    public UdpConnection getActiveConnection(long connection) {
        return this.connectionMap.get(connection);
    }

    public void Connect(String hostname, int port, String serverPassword, boolean useSteamRelay) {
        if (port == 0 && SteamUtils.isSteamModeEnabled()) {
            long steamID;
            try {
                steamID = SteamUtils.convertStringToSteamID(hostname);
            } catch (NumberFormatException var9) {
                DebugType.General.printException(var9, LogSeverity.Error, "Exception thrown trying to convert String to SteamID..");
                LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
                return;
            }

            this.peer.ConnectToSteamServer(steamID, this.hashServerPassword(serverPassword), useSteamRelay);
        } else {
            String ip;
            try {
                InetAddress address = InetAddress.getByName(hostname);
                ip = address.getHostAddress();
            } catch (Exception var8) {
                DebugType.Network.error(Translator.getText("UI_OnConnectFailed_UnknownHost"));
                LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_UnknownHost"));
                return;
            }

            this.peer.Connect(ip, port, this.hashServerPassword(serverPassword), useSteamRelay);
        }
    }

    public void forceDisconnect(long connectedGUID, String message) {
        this.peer.disconnect(connectedGUID, message);
        if (this.connectionMap.containsKey(connectedGUID)) {
            this.removeConnection(this.connectionMap.get(connectedGUID).getIndex());
        }
    }

    public RakNetPeerInterface getPeer() {
        return this.peer;
    }

    public int getMaxConnections() {
        return this.maxConnections;
    }
}
