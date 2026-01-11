// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import com.google.common.collect.Sets;
import fmod.fmod.FMODManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.zip.CRC32;
import org.json.JSONArray;
import org.json.JSONObject;
import zombie.characters.NetworkCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.secure.PZcrypt;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.packets.character.PlayerInjuriesPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.PathFindBehavior2;

public class FakeClientManager {
    private static final int SERVER_PORT = 16261;
    private static final int CLIENT_PORT = 17500;
    private static final String CLIENT_ADDRESS = "0.0.0.0";
    private static final String versionNumber = Core.getInstance().getVersionNumber();
    private static final DateFormat logDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final ThreadLocal<FakeClientManager.StringUTF> stringUTF = ThreadLocal.withInitial(FakeClientManager.StringUTF::new);
    private static int logLevel;
    private static final long startTime = System.currentTimeMillis();
    private static final HashSet<FakeClientManager.Player> players = new HashSet<>();

    public static String ReadStringUTF(ByteBuffer input) {
        return stringUTF.get().load(input);
    }

    public static void WriteStringUTF(ByteBuffer output, String str) {
        stringUTF.get().save(output, str);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }
    }

    private static HashMap<Integer, FakeClientManager.Movement> load(String filename) {
        HashMap<Integer, FakeClientManager.Movement> movements = new HashMap<>();

        try {
            Path path = Paths.get(filename);
            String jsonString = new String(Files.readAllBytes(path));
            JSONObject jsonObject = new JSONObject(jsonString);
            FakeClientManager.Movement.version = jsonObject.getString("version");
            JSONObject jsonConfig = jsonObject.getJSONObject("config");
            JSONObject jsonClient = jsonConfig.getJSONObject("client");
            JSONObject jsonConnection = jsonClient.getJSONObject("connection");
            if (jsonConnection.has("serverHost")) {
                FakeClientManager.Client.connectionServerHost = jsonConnection.getString("serverHost");
            }

            FakeClientManager.Client.connectionInterval = jsonConnection.getLong("interval");
            FakeClientManager.Client.connectionTimeout = jsonConnection.getLong("timeout");
            FakeClientManager.Client.connectionDelay = jsonConnection.getLong("delay");
            JSONObject jsonStatistics = jsonClient.getJSONObject("statistics");
            FakeClientManager.Client.statisticsPeriod = jsonStatistics.getInt("period");
            FakeClientManager.Client.statisticsClientID = Math.max(jsonStatistics.getInt("id"), -1);
            if (jsonClient.has("checksum")) {
                JSONObject jsonChecksum = jsonClient.getJSONObject("checksum");
                FakeClientManager.Client.luaChecksum = jsonChecksum.getString("lua");
                FakeClientManager.Client.scriptChecksum = jsonChecksum.getString("script");
            }

            if (jsonConfig.has("zombies")) {
                jsonConnection = jsonConfig.getJSONObject("zombies");
                FakeClientManager.ZombieSimulator.Behaviour behaviour = FakeClientManager.ZombieSimulator.Behaviour.Normal;
                if (jsonConnection.has("behaviour")) {
                    behaviour = FakeClientManager.ZombieSimulator.Behaviour.valueOf(jsonConnection.getString("behaviour"));
                }

                FakeClientManager.ZombieSimulator.behaviour = behaviour;
                if (jsonConnection.has("maxZombiesPerUpdate")) {
                    FakeClientManager.ZombieSimulator.maxZombiesPerUpdate = jsonConnection.getInt("maxZombiesPerUpdate");
                }

                if (jsonConnection.has("deleteZombieDistance")) {
                    int val = jsonConnection.getInt("deleteZombieDistance");
                    FakeClientManager.ZombieSimulator.deleteZombieDistanceSquared = val * val;
                }

                if (jsonConnection.has("forgotZombieDistance")) {
                    int val = jsonConnection.getInt("forgotZombieDistance");
                    FakeClientManager.ZombieSimulator.forgotZombieDistanceSquared = val * val;
                }

                if (jsonConnection.has("canSeeZombieDistance")) {
                    int val = jsonConnection.getInt("canSeeZombieDistance");
                    FakeClientManager.ZombieSimulator.canSeeZombieDistanceSquared = val * val;
                }

                if (jsonConnection.has("seeZombieDistance")) {
                    int val = jsonConnection.getInt("seeZombieDistance");
                    FakeClientManager.ZombieSimulator.seeZombieDistanceSquared = val * val;
                }

                if (jsonConnection.has("canChangeTarget")) {
                    FakeClientManager.ZombieSimulator.canChangeTarget = jsonConnection.getBoolean("canChangeTarget");
                }
            }

            jsonConnection = jsonConfig.getJSONObject("player");
            FakeClientManager.Player.fps = jsonConnection.getInt("fps");
            FakeClientManager.Player.predictInterval = jsonConnection.getInt("predict");
            if (jsonConnection.has("damage")) {
                FakeClientManager.Player.damage = (float)jsonConnection.getDouble("damage");
            }

            if (jsonConnection.has("voip")) {
                FakeClientManager.Player.isVOIPEnabled = jsonConnection.getBoolean("voip");
            }

            jsonStatistics = jsonConfig.getJSONObject("movement");
            FakeClientManager.Movement.defaultRadius = jsonStatistics.getInt("radius");
            JSONObject jsonMotion = jsonStatistics.getJSONObject("motion");
            FakeClientManager.Movement.aimSpeed = jsonMotion.getInt("aim");
            FakeClientManager.Movement.sneakSpeed = jsonMotion.getInt("sneak");
            FakeClientManager.Movement.sneakRunSpeed = jsonMotion.getInt("sneakrun");
            FakeClientManager.Movement.walkSpeed = jsonMotion.getInt("walk");
            FakeClientManager.Movement.runSpeed = jsonMotion.getInt("run");
            FakeClientManager.Movement.sprintSpeed = jsonMotion.getInt("sprint");
            JSONObject jsonPedestrianSpeed = jsonMotion.getJSONObject("pedestrian");
            FakeClientManager.Movement.pedestrianSpeedMin = jsonPedestrianSpeed.getInt("min");
            FakeClientManager.Movement.pedestrianSpeedMax = jsonPedestrianSpeed.getInt("max");
            JSONObject jsonVehicleSpeed = jsonMotion.getJSONObject("vehicle");
            FakeClientManager.Movement.vehicleSpeedMin = jsonVehicleSpeed.getInt("min");
            FakeClientManager.Movement.vehicleSpeedMax = jsonVehicleSpeed.getInt("max");
            JSONArray jsonMovements = jsonObject.getJSONArray("movements");

            for (int i = 0; i < jsonMovements.length(); i++) {
                jsonStatistics = jsonMovements.getJSONObject(i);
                int ID = jsonStatistics.getInt("id");
                String description = null;
                if (jsonStatistics.has("description")) {
                    description = jsonStatistics.getString("description");
                }

                int spawnX = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                int spawnY = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                if (jsonStatistics.has("spawn")) {
                    JSONObject jsonSpawn = jsonStatistics.getJSONObject("spawn");
                    spawnX = jsonSpawn.getInt("x");
                    spawnY = jsonSpawn.getInt("y");
                }

                FakeClientManager.Movement.Motion motion = Math.random() > 0.8F
                    ? FakeClientManager.Movement.Motion.Vehicle
                    : FakeClientManager.Movement.Motion.Pedestrian;
                if (jsonStatistics.has("motion")) {
                    motion = FakeClientManager.Movement.Motion.valueOf(jsonStatistics.getString("motion"));
                }

                int speed = 0;
                if (jsonStatistics.has("speed")) {
                    speed = jsonStatistics.getInt("speed");
                } else {
                    switch (motion) {
                        case Aim:
                            speed = FakeClientManager.Movement.aimSpeed;
                            break;
                        case Sneak:
                            speed = FakeClientManager.Movement.sneakSpeed;
                            break;
                        case Walk:
                            speed = FakeClientManager.Movement.walkSpeed;
                            break;
                        case SneakRun:
                            speed = FakeClientManager.Movement.sneakRunSpeed;
                            break;
                        case Run:
                            speed = FakeClientManager.Movement.runSpeed;
                            break;
                        case Sprint:
                            speed = FakeClientManager.Movement.sprintSpeed;
                            break;
                        case Pedestrian:
                            speed = (int)Math.round(
                                Math.random() * (FakeClientManager.Movement.pedestrianSpeedMax - FakeClientManager.Movement.pedestrianSpeedMin)
                                    + FakeClientManager.Movement.pedestrianSpeedMin
                            );
                            break;
                        case Vehicle:
                            speed = (int)Math.round(
                                Math.random() * (FakeClientManager.Movement.vehicleSpeedMax - FakeClientManager.Movement.vehicleSpeedMin)
                                    + FakeClientManager.Movement.vehicleSpeedMin
                            );
                    }
                }

                FakeClientManager.Movement.Type type = FakeClientManager.Movement.Type.Line;
                if (jsonStatistics.has("type")) {
                    type = FakeClientManager.Movement.Type.valueOf(jsonStatistics.getString("type"));
                }

                int radius = FakeClientManager.Movement.defaultRadius;
                if (jsonStatistics.has("radius")) {
                    radius = jsonStatistics.getInt("radius");
                }

                IsoDirections direction = IsoDirections.fromIndex((int)Math.round(Math.random() * 7.0));
                if (jsonStatistics.has("direction")) {
                    direction = IsoDirections.valueOf(jsonStatistics.getString("direction"));
                }

                boolean ghost = false;
                if (jsonStatistics.has("ghost")) {
                    ghost = jsonStatistics.getBoolean("ghost");
                }

                long connectDelay = ID * FakeClientManager.Client.connectionInterval;
                if (jsonStatistics.has("connect")) {
                    connectDelay = jsonStatistics.getLong("connect");
                }

                long disconnectDelay = 0L;
                if (jsonStatistics.has("disconnect")) {
                    disconnectDelay = jsonStatistics.getLong("disconnect");
                }

                long reconnectDelay = 0L;
                if (jsonStatistics.has("reconnect")) {
                    reconnectDelay = jsonStatistics.getLong("reconnect");
                }

                long teleportDelay = 0L;
                if (jsonStatistics.has("teleport")) {
                    teleportDelay = jsonStatistics.getLong("teleport");
                }

                int destinationX = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                int destinationY = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                if (jsonStatistics.has("destination")) {
                    JSONObject jsonSpawn = jsonStatistics.getJSONObject("destination");
                    destinationX = jsonSpawn.getInt("x");
                    destinationY = jsonSpawn.getInt("y");
                }

                FakeClientManager.HordeCreator hordeCreator = null;
                if (jsonStatistics.has("createHorde")) {
                    JSONObject jsonCreateHorde = jsonStatistics.getJSONObject("createHorde");
                    int hordeCount = jsonCreateHorde.getInt("count");
                    int hordeRadius = jsonCreateHorde.getInt("radius");
                    long hordeInterval = jsonCreateHorde.getLong("interval");
                    if (hordeInterval != 0L) {
                        hordeCreator = new FakeClientManager.HordeCreator(hordeRadius, hordeCount, hordeInterval);
                    }
                }

                FakeClientManager.SoundMaker soundMaker = null;
                if (jsonStatistics.has("makeSound")) {
                    JSONObject jsonMakeSound = jsonStatistics.getJSONObject("makeSound");
                    int makeSoundInterval = jsonMakeSound.getInt("interval");
                    int makeSoundRadius = jsonMakeSound.getInt("radius");
                    String makeSoundMessage = jsonMakeSound.getString("message");
                    if (makeSoundInterval != 0) {
                        soundMaker = new FakeClientManager.SoundMaker(makeSoundInterval, makeSoundRadius, makeSoundMessage);
                    }
                }

                FakeClientManager.Movement movement = new FakeClientManager.Movement(
                    ID,
                    description,
                    spawnX,
                    spawnY,
                    motion,
                    speed,
                    type,
                    radius,
                    destinationX,
                    destinationY,
                    direction,
                    ghost,
                    connectDelay,
                    disconnectDelay,
                    reconnectDelay,
                    teleportDelay,
                    hordeCreator,
                    soundMaker
                );
                if (movements.containsKey(ID)) {
                    error(ID, String.format("Client %d already exists", movement.id));
                } else {
                    movements.put(ID, movement);
                }
            }

            return movements;
        } catch (Exception var38) {
            error(-1, "Scenarios file load failed");
            var38.printStackTrace();
            return movements;
        } finally {
            ;
        }
    }

    private static void error(int ID, String message) {
        System.out.print(String.format("%5s : %s , [%2d] > %s\n", "ERROR", logDateFormat.format(Calendar.getInstance().getTime()), ID, message));
    }

    private static void info(int ID, String message) {
        if (logLevel >= 0) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "INFO", logDateFormat.format(Calendar.getInstance().getTime()), ID, message));
        }
    }

    private static void log(int ID, String message) {
        if (logLevel >= 1) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "LOG", logDateFormat.format(Calendar.getInstance().getTime()), ID, message));
        }
    }

    private static void trace(int ID, String message) {
        if (logLevel >= 2) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "TRACE", logDateFormat.format(Calendar.getInstance().getTime()), ID, message));
        }
    }

    public static boolean isVOIPEnabled() {
        return FakeClientManager.Player.isVOIPEnabled && getOnlineID() != -1L && getConnectedGUID() != -1L;
    }

    public static long getConnectedGUID() {
        return players.isEmpty() ? -1L : players.iterator().next().client.connectionGuid;
    }

    public static long getOnlineID() {
        return players.isEmpty() ? -1L : players.iterator().next().onlineId;
    }

    public static void main(String[] args) {
        String filename = null;
        int clientID = -1;

        for (int n = 0; n < args.length; n++) {
            if (args[n].startsWith("-scenarios=")) {
                filename = args[n].replace("-scenarios=", "").trim();
            } else if (args[n].startsWith("-id=")) {
                clientID = Integer.parseInt(args[n].replace("-id=", "").trim());
            }
        }

        if (filename == null || filename.isBlank()) {
            error(-1, "Invalid scenarios file name");
            System.exit(0);
        }

        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        System.loadLibrary("RakNet64");
        System.loadLibrary("ZNetNoSteam64");

        try {
            String networkLogLevel = System.getProperty("zomboid.znetlog");
            if (networkLogLevel != null) {
                logLevel = Integer.parseInt(networkLogLevel);
                ZNet.init();
                ZNet.SetLogLevel(logLevel);
            }
        } catch (NumberFormatException var9) {
            error(-1, "Invalid log arguments");
        }

        DebugLog.setLogEnabled(DebugType.General, false);
        HashMap<Integer, FakeClientManager.Movement> movements = load(filename);
        if (FakeClientManager.Player.isVOIPEnabled) {
            FMODManager.instance.init();
            VoiceManager.instance.InitVMClient();
            VoiceManager.instance.setMode(1);
        }

        FakeClientManager.Network network;
        int port;
        if (clientID != -1) {
            port = 17500 + clientID;
            network = new FakeClientManager.Network(movements.size(), port);
        } else {
            port = 17500;
            network = new FakeClientManager.Network(movements.size(), port);
        }

        if (network.isStarted()) {
            int connectionIndex = 0;
            if (clientID != -1) {
                FakeClientManager.Movement movement = movements.get(clientID);
                if (movement != null) {
                    players.add(new FakeClientManager.Player(movement, network, connectionIndex, port));
                } else {
                    error(clientID, "Client movement not found");
                }
            } else {
                for (FakeClientManager.Movement movement : movements.values()) {
                    players.add(new FakeClientManager.Player(movement, network, connectionIndex++, port));
                }
            }

            while (!players.isEmpty()) {
                sleep(1000L);
            }
        }
    }

    private static class Client {
        private static String connectionServerHost = "127.0.0.1";
        private static long connectionInterval = 1500L;
        private static long connectionTimeout = 10000L;
        private static long connectionDelay = 15000L;
        private static int statisticsClientID = -1;
        private static int statisticsPeriod = 1;
        private static long serverTimeShift;
        private static boolean serverTimeShiftIsSet;
        private final HashMap<Integer, FakeClientManager.Client.Request> requests = new HashMap<>();
        private final FakeClientManager.Player player;
        private final FakeClientManager.Network network;
        private final int connectionIndex;
        private final int port;
        private long connectionGuid = -1L;
        private int requestId;
        private long stateTime;
        private FakeClientManager.Client.State state;
        private String host;
        public static String luaChecksum = "";
        public static String scriptChecksum = "";

        private Client(FakeClientManager.Player _player, FakeClientManager.Network network, int connectionIndex, int port) {
            this.connectionIndex = connectionIndex;
            this.network = network;
            this.player = _player;
            this.port = port;

            try {
                this.host = InetAddress.getByName(connectionServerHost).getHostAddress();
                this.state = FakeClientManager.Client.State.CONNECT;
                Thread thread = new Thread(ThreadGroups.Workers, this::updateThread, this.player.username);
                thread.setDaemon(true);
                thread.start();
            } catch (UnknownHostException var6) {
                this.state = FakeClientManager.Client.State.QUIT;
                var6.printStackTrace();
            }
        }

        private void updateThread() {
            FakeClientManager.info(
                this.player.movement.id,
                String.format(
                    "Start client (%d) %s:%d => %s:%d / \"%s\"", this.connectionIndex, "0.0.0.0", this.port, this.host, 16261, this.player.movement.description
                )
            );
            FakeClientManager.sleep(this.player.movement.connectDelay);
            switch (this.player.movement.type) {
                case Line:
                    this.player.lineMovement();
                    break;
                case Circle:
                    this.player.circleMovement();
                    break;
                case AIAttackZombies:
                    this.player.aiAttackZombiesMovement();
                    break;
                case AIRunAwayFromZombies:
                    this.player.aiRunAwayFromZombiesMovement();
                    break;
                case AIRunToAnotherPlayers:
                    this.player.aiRunToAnotherPlayersMovement();
                    break;
                case AINormal:
                    this.player.aiNormalMovement();
            }

            while (this.state != FakeClientManager.Client.State.QUIT) {
                this.update();
                FakeClientManager.sleep(1L);
            }

            FakeClientManager.info(
                this.player.movement.id,
                String.format(
                    "Stop client (%d) %s:%d => %s:%d / \"%s\"", this.connectionIndex, "0.0.0.0", this.port, this.host, 16261, this.player.movement.description
                )
            );
        }

        private void updateTime() {
            this.stateTime = System.currentTimeMillis();
        }

        private long getServerTime() {
            return serverTimeShiftIsSet ? System.nanoTime() + serverTimeShift : 0L;
        }

        private boolean checkConnectionTimeout() {
            return System.currentTimeMillis() - this.stateTime > connectionTimeout;
        }

        private boolean checkConnectionDelay() {
            return System.currentTimeMillis() - this.stateTime > connectionDelay;
        }

        private void changeState(FakeClientManager.Client.State newState) {
            this.updateTime();
            FakeClientManager.log(this.player.movement.id, String.format("%s >> %s", this.state, newState));
            if (FakeClientManager.Client.State.RUN == newState) {
                this.player.movement.connect(this.player.onlineId);
                if (this.player.teleportLimiter == null) {
                    this.player.teleportLimiter = new UpdateLimit(this.player.movement.teleportDelay);
                }

                if (this.player.movement.id == statisticsClientID) {
                    this.sendTimeSync();
                    this.sendInjuries();
                }
            } else if (FakeClientManager.Client.State.DISCONNECT == newState && FakeClientManager.Client.State.DISCONNECT != this.state) {
                this.player.movement.disconnect(this.player.onlineId);
            }

            this.state = newState;
        }

        private void update() {
            switch (this.state) {
                case CONNECT:
                    this.player.movement.timestamp = System.currentTimeMillis();
                    this.network.connect(this.player.movement.id, this.host);
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case LOGIN:
                    this.sendPlayerLogin();
                    this.sendPlayerLoginRequest();
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case CHECKSUM:
                    this.sendChecksum();
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case PLAYER_CONNECT:
                    this.sendPlayerConnect();
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case PLAYER_EXTRA_INFO:
                    this.sendPlayerExtraInfo(this.player.movement.ghost, this.player.movement.hordeCreator != null || FakeClientManager.Player.isVOIPEnabled);
                    this.sendEquip();
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case LOAD:
                    this.requestId = 0;
                    this.requests.clear();
                    this.requestFullUpdate();
                    this.requestLargeAreaZip();
                    this.changeState(FakeClientManager.Client.State.WAIT);
                    break;
                case RUN:
                    if (this.player.movement.doDisconnect() && this.player.movement.checkDisconnect()) {
                        this.changeState(FakeClientManager.Client.State.DISCONNECT);
                    } else {
                        this.player.run();
                    }
                    break;
                case WAIT:
                    if (this.checkConnectionTimeout()) {
                        this.changeState(FakeClientManager.Client.State.DISCONNECT);
                    }
                    break;
                case DISCONNECT:
                    if (this.network.isConnected()) {
                        this.player.movement.timestamp = System.currentTimeMillis();
                        this.network.disconnect(this.connectionGuid, this.player.movement.id, this.host);
                    }

                    if (this.player.movement.doReconnect() && this.player.movement.checkReconnect()
                        || !this.player.movement.doReconnect() && this.checkConnectionDelay()) {
                        this.changeState(FakeClientManager.Client.State.CONNECT);
                    }
                case QUIT:
            }
        }

        private void receive(short type, ByteBuffer bb) {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(type);
            FakeClientManager.Network.logUserPacket(this.player.movement.id, type);
            switch (packetType) {
                case PlayerConnect:
                    if (this.receivePlayerConnect(bb)) {
                        if (luaChecksum.isEmpty()) {
                            this.changeState(FakeClientManager.Client.State.PLAYER_EXTRA_INFO);
                        } else {
                            this.changeState(FakeClientManager.Client.State.CHECKSUM);
                        }
                    }
                    break;
                case LoginQueueRequest:
                    if (this.receiveLoginQueueRequest(bb)) {
                        this.sendLoginQueueDone();
                        this.changeState(FakeClientManager.Client.State.PLAYER_CONNECT);
                    }
                    break;
                case ExtraInfo:
                    if (this.receivePlayerExtraInfo(bb)) {
                        this.changeState(FakeClientManager.Client.State.RUN);
                    }
                    break;
                case SentChunk:
                    if (this.state == FakeClientManager.Client.State.WAIT && this.receiveChunkPart(bb)) {
                        this.updateTime();
                        if (this.allChunkPartsReceived()) {
                            this.changeState(FakeClientManager.Client.State.PLAYER_CONNECT);
                        }
                    }
                    break;
                case NotRequiredInZip:
                    if (this.state == FakeClientManager.Client.State.WAIT && this.receiveNotRequired(bb)) {
                        this.updateTime();
                        if (this.allChunkPartsReceived()) {
                            this.changeState(FakeClientManager.Client.State.PLAYER_CONNECT);
                        }
                    }
                case PlayerHitSquare:
                case PlayerHitVehicle:
                case PlayerHitZombie:
                case PlayerHitPlayer:
                case PlayerHitAnimal:
                case ZombieHitPlayer:
                case AnimalHitPlayer:
                case AnimalHitAnimal:
                case AnimalHitThumpable:
                case VehicleHitZombie:
                case VehicleHitPlayer:
                default:
                    break;
                case TimeSync:
                    this.receiveTimeSync(bb);
                    break;
                case SyncClock:
                    this.receiveSyncClock(bb);
                    break;
                case ZombieSynchronizationUnreliable:
                case ZombieSynchronizationReliable:
                    this.receiveZombieSimulation(bb);
                    break;
                case PlayerUpdateUnreliable:
                case PlayerUpdateReliable:
                    this.player.playerManager.parsePlayer(bb);
                    break;
                case PlayerTimeout:
                    this.player.playerManager.parsePlayerTimeout(bb);
                    break;
                case Kicked:
                    this.receiveKicked(bb);
                    break;
                case Checksum:
                    this.receiveChecksum(bb);
                    break;
                case Teleport:
                    this.receiveTeleport(bb);
            }

            bb.clear();
        }

        private void doPacket(short type, ByteBuffer bb) {
            bb.put((byte)-122);
            bb.putShort(type);
        }

        private void putUTF(ByteBuffer b, String str) {
            if (str == null) {
                b.putShort((short)0);
            } else {
                byte[] bytes = str.getBytes();
                b.putShort((short)bytes.length);
                b.put(bytes);
            }
        }

        private void putBoolean(ByteBuffer b, boolean v) {
            b.put((byte)(v ? 1 : 0));
        }

        private void sendPlayerLogin() {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.Login.getId(), bb);
            this.putUTF(bb, this.player.username);
            this.putUTF(bb, this.player.username);
            this.putUTF(bb, FakeClientManager.versionNumber);
            bb.putInt(1);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendPlayerLoginRequest() {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.LoginQueueRequest.getId(), bb);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendLoginQueueDone() {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.LoginQueueDone.getId(), bb);
            bb.putLong(100L);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendPlayerConnect() {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerConnect.getId(), bb);
            this.writePlayerConnectData(bb);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void writePlayerConnectData(ByteBuffer b) {
            b.put((byte)0);
            b.put((byte)13);
            b.putFloat(this.player.x);
            b.putFloat(this.player.y);
            b.putFloat(this.player.z);
            b.putInt(0);
            this.putUTF(b, this.player.username);
            this.putUTF(b, this.player.username);
            this.putUTF(b, this.player.isFemale == 0 ? "Kate" : "Male");
            b.putInt(this.player.isFemale);
            this.putUTF(b, "fireofficer");
            b.putInt(0);
            b.putInt(4);
            this.putUTF(b, "Sprinting");
            b.putInt(1);
            this.putUTF(b, "Fitness");
            b.putInt(6);
            this.putUTF(b, "Strength");
            b.putInt(6);
            this.putUTF(b, "Axe");
            b.putInt(1);
            b.put((byte)0);
            b.put((byte)0);
            b.put((byte)Math.round(Math.random() * 5.0));
            b.put((byte)0);
            b.put((byte)0);
            b.put((byte)0);
            b.put((byte)0);
            int items = this.player.clothes.size();
            b.put((byte)items);

            for (FakeClientManager.Player.Clothes clothes : this.player.clothes) {
                b.put(clothes.flags);
                this.putUTF(b, "Base." + clothes.name);
                this.putUTF(b, null);
                this.putUTF(b, clothes.name);
                b.put((byte)-1);
                b.put((byte)-1);
                b.put((byte)-1);
                b.put(clothes.text);
                b.putFloat(0.0F);
                b.put((byte)0);
                b.put((byte)0);
                b.put((byte)0);
                b.put((byte)0);
                b.put((byte)0);
                b.put((byte)0);
            }

            this.putUTF(b, "fake_str");
            b.putShort((short)0);
            b.putInt(2);
            this.putUTF(b, "Fit");
            this.putUTF(b, "Stout");
            b.putFloat(0.0F);
            b.putInt(0);
            b.putInt(0);
            b.putInt(4);
            this.putUTF(b, "Sprinting");
            b.putFloat(75.0F);
            this.putUTF(b, "Fitness");
            b.putFloat(67500.0F);
            this.putUTF(b, "Strength");
            b.putFloat(67500.0F);
            this.putUTF(b, "Axe");
            b.putFloat(75.0F);
            b.putInt(4);
            this.putUTF(b, "Sprinting");
            b.putInt(1);
            this.putUTF(b, "Fitness");
            b.putInt(6);
            this.putUTF(b, "Strength");
            b.putInt(6);
            this.putUTF(b, "Axe");
            b.putInt(1);
            b.putInt(0);
            this.putBoolean(b, true);
            this.putUTF(b, "fake");
            b.putFloat(this.player.tagColor.r);
            b.putFloat(this.player.tagColor.g);
            b.putFloat(this.player.tagColor.b);
            b.putInt(0);
            b.putDouble(0.0);
            b.putInt(0);
            this.putUTF(b, this.player.username);
            b.putFloat(this.player.speakColor.r);
            b.putFloat(this.player.speakColor.g);
            b.putFloat(this.player.speakColor.b);
            this.putBoolean(b, true);
            this.putBoolean(b, false);
            b.put((byte)0);
            b.put((byte)0);
            b.putInt(0);
            b.putInt(0);
        }

        @Deprecated
        private void sendPlayerExtraInfo(boolean ghost, boolean admin) {
        }

        private void sendSyncRadioData() {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.SyncRadioData.getId(), b);
            b.put((byte)(FakeClientManager.Player.isVOIPEnabled ? 1 : 0));
            b.putInt(4);
            b.putInt(0);
            b.putInt((int)RakVoice.GetMaxDistance());
            b.putInt((int)this.player.x);
            b.putInt((int)this.player.y);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendEquip() {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.Equip.getId(), bb);
            bb.put((byte)0);
            bb.put((byte)0);
            bb.put((byte)1);
            bb.putInt(16);
            bb.putShort((short)1202);
            bb.put((byte)1);
            bb.putInt(837602032);
            bb.put((byte)0);
            bb.putInt(0);
            bb.putInt(0);
            bb.put((byte)0);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendChatMessage(String message) {
            ByteBuffer bb = this.network.startPacket();
            bb.putShort(this.player.onlineId);
            bb.putInt(2);
            this.putUTF(bb, this.player.username);
            this.putUTF(bb, message);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private short getBooleanVariables() {
            short booleanVariables = 0;
            if (this.player.movement.speed > 0.0F) {
                switch (this.player.movement.motion) {
                    case Aim:
                        booleanVariables = (short)(booleanVariables | 64);
                        break;
                    case Sneak:
                        booleanVariables = (short)(booleanVariables | 1);
                    case Walk:
                    default:
                        break;
                    case SneakRun:
                        booleanVariables = (short)(booleanVariables | 17);
                        break;
                    case Run:
                        booleanVariables = (short)(booleanVariables | 16);
                        break;
                    case Sprint:
                        booleanVariables = (short)(booleanVariables | 32);
                }

                booleanVariables = (short)(booleanVariables | 17408);
            }

            return booleanVariables;
        }

        private void sendPlayer(NetworkCharacter.Transform transform, int t, Vector2 direction) {
            PlayerPacket packet = new PlayerPacket();
            packet.prediction.x = transform.position.x;
            packet.prediction.y = transform.position.y;
            packet.prediction.z = (byte)this.player.z;
            packet.prediction.direction = direction.getDirection();
            packet.prediction.type = 0;
            packet.booleanVariables = this.getBooleanVariables();
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerUpdateReliable.getId(), b);
            ByteBufferWriter bw = new ByteBufferWriter(b);
            packet.write(bw);
            this.network.endPacket(this.connectionGuid);
        }

        private boolean receivePlayerConnect(ByteBuffer bb) {
            short id = bb.getShort();
            if (id == -1) {
                int playerIndex = bb.get();
                id = bb.getShort();
                this.player.onlineId = id;
                return true;
            } else {
                return false;
            }
        }

        private boolean receiveLoginQueueRequest(ByteBuffer bb) {
            return bb.get() == 1;
        }

        private boolean receivePlayerExtraInfo(ByteBuffer bb) {
            short id = bb.getShort();
            return id == this.player.onlineId;
        }

        private boolean receiveChunkPart(ByteBuffer bb) {
            boolean result = false;
            int requestNumber = bb.getInt();
            int numChunks = bb.getInt();
            int chunkIndex = bb.getInt();
            int fileSize = bb.getInt();
            int offset = bb.getInt();
            int count = bb.getInt();
            if (this.requests.remove(requestNumber) != null) {
                result = true;
            }

            return result;
        }

        private boolean receiveNotRequired(ByteBuffer bb) {
            boolean result = false;
            int count = bb.getInt();

            for (int n = 0; n < count; n++) {
                int requestNumber = bb.getInt();
                boolean sameOnServer = bb.get() == 1;
                if (this.requests.remove(requestNumber) != null) {
                    result = true;
                }
            }

            return result;
        }

        private boolean allChunkPartsReceived() {
            return this.requests.isEmpty();
        }

        private void addChunkRequest(int wx, int wy, int x, int y) {
            FakeClientManager.Client.Request request = new FakeClientManager.Client.Request(wx, wy, this.requestId);
            this.requestId++;
            this.requests.put(request.id, request);
        }

        private void requestZipList() {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.RequestZipList.getId(), b);
            b.putInt(this.requests.size());

            for (FakeClientManager.Client.Request request : this.requests.values()) {
                b.putInt(request.id);
                b.putInt(request.wx);
                b.putInt(request.wy);
                b.putLong(request.crc);
            }

            this.network.endPacket(this.connectionGuid);
        }

        private void requestLargeAreaZip() {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.RequestLargeAreaZip.getId(), b);
            b.putInt(this.player.worldX);
            b.putInt(this.player.worldY);
            b.putInt(13);
            this.network.endPacketImmediate(this.connectionGuid);
            int minX = this.player.worldX - 6 + 2;
            int minY = this.player.worldY - 6 + 2;
            int maxX = this.player.worldX + 6 + 2;
            int maxY = this.player.worldY + 6 + 2;

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    FakeClientManager.Client.Request request = new FakeClientManager.Client.Request(x, y, this.requestId);
                    this.requestId++;
                    this.requests.put(request.id, request);
                }
            }

            this.requestZipList();
        }

        private void requestFullUpdate() {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.getId(), b);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void requestChunkObjectState() {
            for (FakeClientManager.Client.Request request : this.requests.values()) {
                ByteBuffer b = this.network.startPacket();
                this.doPacket(PacketTypes.PacketType.ChunkObjectState.getId(), b);
                b.putShort((short)request.wx);
                b.putShort((short)request.wy);
                this.network.endPacket(this.connectionGuid);
            }
        }

        private void requestChunks() {
            if (!this.requests.isEmpty()) {
                this.requestZipList();
                this.requestChunkObjectState();
                this.requests.clear();
            }
        }

        private void receiveStatistics(ByteBuffer bb) {
            long min = bb.getLong();
            long max = bb.getLong();
            long avg = bb.getLong();
            long fps = bb.getLong();
            long tps = bb.getLong();
            long con = bb.getLong();
            long c1 = bb.getLong();
            long c2 = bb.getLong();
            long c3 = bb.getLong();
            FakeClientManager.info(
                this.player.movement.id,
                String.format("ServerStats: con=[%2d] fps=[%2d] tps=[%2d] upt=[%4d-%4d/%4d], c1=[%d] c2=[%d] c3=[%d]", con, fps, tps, min, max, avg, c1, c2, c3)
            );
        }

        private void sendTimeSync() {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.TimeSync.getId(), b);
            long time_ns_client = System.nanoTime();
            b.putLong(time_ns_client);
            b.putLong(0L);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void receiveTimeSync(ByteBuffer bb) {
            long timeClientSend = bb.getLong();
            long timeServer = bb.getLong();
            long timeClientReceive = System.nanoTime();
            long localPing = timeClientReceive - timeClientSend;
            long localServerTimeShift = timeServer - timeClientReceive + localPing / 2L;
            long serverTimeShiftLast = serverTimeShift;
            if (!serverTimeShiftIsSet) {
                serverTimeShift = localServerTimeShift;
            } else {
                serverTimeShift = (long)((float)serverTimeShift + (float)(localServerTimeShift - serverTimeShift) * 0.05F);
            }

            long serverTimeuQality = 10000000L;
            if (Math.abs(serverTimeShift - serverTimeShiftLast) > 10000000L) {
                this.sendTimeSync();
            } else {
                serverTimeShiftIsSet = true;
            }
        }

        private void receiveSyncClock(ByteBuffer bb) {
            FakeClientManager.trace(this.player.movement.id, String.format("Player %3d sync clock", this.player.onlineId));
        }

        private void receiveKicked(ByteBuffer bb) {
            String chat = FakeClientManager.ReadStringUTF(bb);
            FakeClientManager.info(this.player.movement.id, String.format("Client kicked. Reason: %s", chat));
        }

        private void receiveChecksum(ByteBuffer bb) {
            FakeClientManager.trace(this.player.movement.id, String.format("Player %3d receive Checksum", this.player.onlineId));
            short checksum = bb.getShort();
            boolean isLuaOk = bb.get() == 1;
            boolean isScriptOk = bb.get() == 1;
            if (checksum != 1 || !isLuaOk || !isScriptOk) {
                FakeClientManager.info(this.player.movement.id, String.format("checksum lua: %b, script: %b", isLuaOk, isScriptOk));
            }

            this.changeState(FakeClientManager.Client.State.PLAYER_EXTRA_INFO);
        }

        private void receiveTeleport(ByteBuffer bb) {
            int playerIndex = bb.get();
            float x = bb.getFloat();
            float y = bb.getFloat();
            float z = bb.getFloat();
            FakeClientManager.info(this.player.movement.id, String.format("Player %3d teleport to (%d, %d)", this.player.onlineId, (int)x, (int)y));
            this.player.x = x;
            this.player.y = y;
        }

        private void receiveZombieSimulation(ByteBuffer bb) {
            this.player.simulator.clear();
            boolean isNeighborPlayer = bb.get() == 1;
            short numDeletingZombies = bb.getShort();

            for (short n = 0; n < numDeletingZombies; n++) {
                short zombieId = bb.getShort();
                FakeClientManager.Zombie zombie = this.player.simulator.zombies.get(Integer.valueOf(zombieId));
                this.player.simulator.zombies4Delete.add(zombie);
            }

            short numAuths = bb.getShort();

            for (short n = 0; n < numAuths; n++) {
                short zombieId = bb.getShort();
                this.player.simulator.add(zombieId);
            }

            this.player.simulator.receivePacket(bb);
            this.player.simulator.process();
        }

        private void sendInjuries() {
            PlayerInjuriesPacket packet = new PlayerInjuriesPacket();
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerInjuries.getId(), b);
            ByteBufferWriter bw = new ByteBufferWriter(b);
            packet.write(bw);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendChecksum() {
            if (!luaChecksum.isEmpty()) {
                FakeClientManager.trace(this.player.movement.id, String.format("Player %3d sendChecksum", this.player.onlineId));
                ByteBuffer b = this.network.startPacket();
                this.doPacket(PacketTypes.PacketType.Checksum.getId(), b);
                b.putShort((short)1);
                this.putUTF(b, luaChecksum);
                this.putUTF(b, scriptChecksum);
                this.network.endPacketImmediate(this.connectionGuid);
            }
        }

        public void sendCommand(String command) {
            ByteBuffer b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.ReceiveCommand.getId(), b);
            FakeClientManager.WriteStringUTF(b, command);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendEventPacket(short id, int x, int y, int z, byte eventID, String type1) {
        }

        private void sendWorldSound4Player(int x, int y, int z, int radius, int volume) {
            ByteBuffer bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.WorldSoundPacket.getId(), bb);
            bb.putInt(x);
            bb.putInt(y);
            bb.putInt(z);
            bb.putInt(radius);
            bb.putInt(volume);
            bb.put((byte)0);
            bb.putFloat(0.0F);
            bb.putFloat(1.0F);
            bb.put((byte)0);
            bb.put((byte)0);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private static final class Request {
            private final int id;
            private final int wx;
            private final int wy;
            private final long crc;

            private Request(int wx, int wy, int requestId) {
                this.id = requestId;
                this.wx = wx;
                this.wy = wy;
                CRC32 crc32 = new CRC32();
                crc32.reset();
                crc32.update(String.format("map_%d_%d.bin", wx, wy).getBytes());
                this.crc = crc32.getValue();
            }
        }

        private static enum State {
            CONNECT,
            LOGIN,
            CHECKSUM,
            PLAYER_CONNECT,
            PLAYER_EXTRA_INFO,
            LOAD,
            RUN,
            WAIT,
            DISCONNECT,
            QUIT;
        }
    }

    private static class HordeCreator {
        private final int radius;
        private final int count;
        private final long interval;
        private final UpdateLimit hordeCreatorLimiter;

        public HordeCreator(int radius, int count, long interval) {
            this.radius = radius;
            this.count = count;
            this.interval = interval;
            this.hordeCreatorLimiter = new UpdateLimit(interval);
        }

        public String getCommand(int x, int y, int z) {
            return String.format(
                "/createhorde2 -x %d -y %d -z %d -count %d -radius %d -crawler false -isFallOnFront false -isFakeDead false -knockedDown false -health 1 -outfit",
                x,
                y,
                z,
                this.count,
                this.radius
            );
        }
    }

    private static class Movement {
        static String version;
        static int defaultRadius = 150;
        static int aimSpeed = 4;
        static int sneakSpeed = 6;
        static int walkSpeed = 7;
        static int sneakRunSpeed = 10;
        static int runSpeed = 13;
        static int sprintSpeed = 19;
        static int pedestrianSpeedMin = 5;
        static int pedestrianSpeedMax = 20;
        static int vehicleSpeedMin = 40;
        static int vehicleSpeedMax = 80;
        static final float zombieLungeDistanceSquared = 100.0F;
        static final float zombieWalkSpeed = 3.0F;
        static final float zombieLungeSpeed = 6.0F;
        final int id;
        final String description;
        final Vector2 spawn;
        FakeClientManager.Movement.Motion motion;
        float speed;
        final FakeClientManager.Movement.Type type;
        final int radius;
        final IsoDirections direction;
        final Vector2 destination;
        final boolean ghost;
        final long connectDelay;
        final long disconnectDelay;
        final long reconnectDelay;
        final long teleportDelay;
        final FakeClientManager.HordeCreator hordeCreator;
        FakeClientManager.SoundMaker soundMaker;
        long timestamp;

        public Movement(
            int id,
            String description,
            int spawnX,
            int spawnY,
            FakeClientManager.Movement.Motion motion,
            int speed,
            FakeClientManager.Movement.Type type,
            int radius,
            int destinationX,
            int destinationY,
            IsoDirections direction,
            boolean ghost,
            long connectDelay,
            long disconnectDelay,
            long reconnectDelay,
            long teleportDelay,
            FakeClientManager.HordeCreator hordeCreator,
            FakeClientManager.SoundMaker soundMaker
        ) {
            this.id = id;
            this.description = description;
            this.spawn = new Vector2(spawnX, spawnY);
            this.motion = motion;
            this.speed = speed;
            this.type = type;
            this.radius = radius;
            this.direction = direction;
            this.destination = new Vector2(destinationX, destinationY);
            this.ghost = ghost;
            this.connectDelay = connectDelay;
            this.disconnectDelay = disconnectDelay;
            this.reconnectDelay = reconnectDelay;
            this.teleportDelay = teleportDelay;
            this.hordeCreator = hordeCreator;
            this.soundMaker = soundMaker;
        }

        public void connect(int playerId) {
            long currentTime = System.currentTimeMillis();
            if (this.disconnectDelay != 0L) {
                FakeClientManager.info(
                    this.id,
                    String.format(
                        "Player %3d connect in %.3fs, disconnect in %.3fs",
                        playerId,
                        (float)(currentTime - this.timestamp) / 1000.0F,
                        (float)this.disconnectDelay / 1000.0F
                    )
                );
            } else {
                FakeClientManager.info(this.id, String.format("Player %3d connect in %.3fs", playerId, (float)(currentTime - this.timestamp) / 1000.0F));
            }

            this.timestamp = currentTime;
        }

        public void disconnect(int playerId) {
            long currentTime = System.currentTimeMillis();
            if (this.reconnectDelay != 0L) {
                FakeClientManager.info(
                    this.id,
                    String.format(
                        "Player %3d disconnect in %.3fs, reconnect in %.3fs",
                        playerId,
                        (float)(currentTime - this.timestamp) / 1000.0F,
                        (float)this.reconnectDelay / 1000.0F
                    )
                );
            } else {
                FakeClientManager.info(this.id, String.format("Player %3d disconnect in %.3fs", playerId, (float)(currentTime - this.timestamp) / 1000.0F));
            }

            this.timestamp = currentTime;
        }

        public boolean doTeleport() {
            return this.teleportDelay != 0L;
        }

        public boolean doDisconnect() {
            return this.disconnectDelay != 0L;
        }

        public boolean checkDisconnect() {
            return System.currentTimeMillis() - this.timestamp > this.disconnectDelay;
        }

        public boolean doReconnect() {
            return this.reconnectDelay != 0L;
        }

        public boolean checkReconnect() {
            return System.currentTimeMillis() - this.timestamp > this.reconnectDelay;
        }

        private static enum Motion {
            Aim,
            Sneak,
            Walk,
            SneakRun,
            Run,
            Sprint,
            Pedestrian,
            Vehicle;
        }

        private static enum Type {
            Stay,
            Line,
            Circle,
            AIAttackZombies,
            AIRunAwayFromZombies,
            AIRunToAnotherPlayers,
            AINormal;
        }
    }

    private static class Network {
        private final HashMap<Integer, FakeClientManager.Client> createdClients = new HashMap<>();
        private final HashMap<Long, FakeClientManager.Client> connectedClients = new HashMap<>();
        private final ByteBuffer rb = ByteBuffer.allocate(1000000);
        private final ByteBuffer wb = ByteBuffer.allocate(1000000);
        private final RakNetPeerInterface peer;
        private final int started;
        private int connected = -1;
        private static final HashMap<Integer, String> systemPacketTypeNames = new HashMap<>();

        boolean isConnected() {
            return this.connected == 0;
        }

        boolean isStarted() {
            return this.started == 0;
        }

        private Network(int maxConnections, int port) {
            this.peer = new RakNetPeerInterface();
            this.peer.Init(false);
            this.peer.SetMaximumIncomingConnections(0);
            this.peer.SetClientPort(port);
            this.peer.SetOccasionalPing(true);
            this.started = this.peer.Startup(maxConnections);
            if (this.started == 0) {
                Thread thread = new Thread(ThreadGroups.Network, this::receiveThread, "PeerInterfaceReceive");
                thread.setDaemon(true);
                thread.start();
                FakeClientManager.log(-1, "Network start ok");
            } else {
                FakeClientManager.error(-1, String.format("Network start failed: %d", this.started));
            }
        }

        private void connect(int clientID, String address) {
            this.connected = this.peer.Connect(address, 16261, PZcrypt.hash("", true), false);
            if (this.connected == 0) {
                FakeClientManager.log(clientID, String.format("Client connected to %s:%d", address, 16261));
            } else {
                FakeClientManager.error(clientID, String.format("Client connection to %s:%d failed: %d", address, 16261, this.connected));
            }
        }

        private void disconnect(long connectedGUID, int clientID, String address) {
            if (connectedGUID != 0L) {
                this.peer.disconnect(connectedGUID, "");
                this.connected = -1;
            }

            if (this.connected == -1) {
                FakeClientManager.log(clientID, String.format("Client disconnected from %s:%d", address, 16261));
            } else {
                FakeClientManager.log(clientID, String.format("Client disconnection from %s:%d failed: %d", address, 16261, connectedGUID));
            }
        }

        private ByteBuffer startPacket() {
            this.wb.clear();
            return this.wb;
        }

        private void cancelPacket() {
            this.wb.clear();
        }

        private void endPacket(long connectionGUID) {
            this.wb.flip();
            this.peer.Send(this.wb, 1, 3, (byte)0, connectionGUID, false);
        }

        private void endPacketImmediate(long connectionGUID) {
            this.wb.flip();
            this.peer.Send(this.wb, 0, 3, (byte)0, connectionGUID, false);
        }

        private void endPacketSuperHighUnreliable(long connectionGUID) {
            this.wb.flip();
            this.peer.Send(this.wb, 0, 1, (byte)0, connectionGUID, false);
        }

        private void receiveThread() {
            while (true) {
                if (this.peer.Receive(this.rb)) {
                    this.decode(this.rb);
                } else {
                    FakeClientManager.sleep(1L);
                }
            }
        }

        private static void logUserPacket(int ID, short type) {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(type);
            String packet = packetType == null ? "unknown user packet" : packetType.name();
            FakeClientManager.trace(ID, String.format("## %s (%d)", packet, type));
        }

        private static void logSystemPacket(int ID, int type) {
            String packet = systemPacketTypeNames.getOrDefault(type, "unknown system packet");
            FakeClientManager.trace(ID, String.format("## %s (%d)", packet, type));
        }

        private void decode(ByteBuffer buf) {
            int type = buf.get() & 255;
            int connectionIndex = -1;
            long connectionGUID = -1L;
            logSystemPacket(connectionIndex, type);
            switch (type) {
                case 0:
                case 1:
                case 20:
                case 25:
                case 31:
                case 33:
                default:
                    break;
                case 16:
                    connectionIndex = buf.get() & 255;
                    connectionGUID = this.peer.getGuidOfPacket();
                    FakeClientManager.Client clientx = this.createdClients.get(connectionIndex);
                    if (clientx != null) {
                        clientx.connectionGuid = connectionGUID;
                        this.connectedClients.put(connectionGUID, clientx);
                        VoiceManager.instance.VoiceConnectReq(connectionGUID);
                        clientx.changeState(FakeClientManager.Client.State.LOGIN);
                    }

                    FakeClientManager.log(-1, String.format("Connected clients: %d (connection index %d)", this.connectedClients.size(), connectionIndex));
                    break;
                case 17:
                case 18:
                case 23:
                case 24:
                case 32:
                    FakeClientManager.error(-1, "Connection failed: " + type);
                    break;
                case 19:
                    connectionIndex = buf.get() & 255;
                case 44:
                case 45:
                    connectionGUID = this.peer.getGuidOfPacket();
                    break;
                case 21:
                    connectionIndex = buf.get() & 255;
                    connectionGUID = this.peer.getGuidOfPacket();
                    FakeClientManager.Client clientx = this.connectedClients.get(connectionGUID);
                    if (clientx != null) {
                        this.connectedClients.remove(connectionGUID);
                        clientx.changeState(FakeClientManager.Client.State.DISCONNECT);
                    }

                    FakeClientManager.log(-1, String.format("Connected clients: %d (connection index %d)", this.connectedClients.size(), connectionIndex));
                    break;
                case 22:
                    connectionIndex = buf.get() & 255;
                    FakeClientManager.Client clientx = this.createdClients.get(connectionIndex);
                    if (clientx != null) {
                        clientx.changeState(FakeClientManager.Client.State.DISCONNECT);
                    }
                    break;
                case 134:
                    int userPacketId = buf.getShort();
                    connectionGUID = this.peer.getGuidOfPacket();
                    FakeClientManager.Client client = this.connectedClients.get(connectionGUID);
                    if (client != null) {
                        client.receive((short)userPacketId, buf);
                        connectionIndex = client.connectionIndex;
                    }
            }
        }

        static {
            systemPacketTypeNames.put(22, "connection lost");
            systemPacketTypeNames.put(21, "disconnected");
            systemPacketTypeNames.put(23, "connection banned");
            systemPacketTypeNames.put(17, "connection failed");
            systemPacketTypeNames.put(20, "no free connections");
            systemPacketTypeNames.put(16, "connection accepted");
            systemPacketTypeNames.put(18, "already connected");
            systemPacketTypeNames.put(44, "voice request");
            systemPacketTypeNames.put(45, "voice reply");
            systemPacketTypeNames.put(25, "wrong protocol version");
            systemPacketTypeNames.put(0, "connected ping");
            systemPacketTypeNames.put(1, "unconnected ping");
            systemPacketTypeNames.put(33, "new remote connection");
            systemPacketTypeNames.put(31, "remote disconnection");
            systemPacketTypeNames.put(32, "remote connection lost");
            systemPacketTypeNames.put(24, "invalid password");
            systemPacketTypeNames.put(19, "new connection");
            systemPacketTypeNames.put(134, "user packet");
        }
    }

    private static class Player {
        private static final int cellSize = 50;
        private static final int spawnMinX = 3550;
        private static final int spawnMaxX = 14450;
        private static final int spawnMinY = 5050;
        private static final int spawnMaxY = 12950;
        private static final int ChunkGridWidth = 13;
        private static final int ChunksPerWidth = 10;
        private static int fps = 60;
        private static int predictInterval = 1000;
        private static float damage = 1.0F;
        private static boolean isVOIPEnabled;
        private final NetworkCharacter networkCharacter;
        private final UpdateLimit updateLimiter;
        private final UpdateLimit predictLimiter;
        private final UpdateLimit timeSyncLimiter;
        private final FakeClientManager.Client client;
        private final FakeClientManager.Movement movement;
        private final ArrayList<FakeClientManager.Player.Clothes> clothes;
        private final String username;
        private final int isFemale;
        private final Color tagColor;
        private final Color speakColor;
        private UpdateLimit teleportLimiter;
        private short onlineId;
        private float x;
        private float y;
        private final float z;
        private final Vector2 direction;
        private int worldX;
        private int worldY;
        private float angle;
        private final FakeClientManager.ZombieSimulator simulator;
        private final FakeClientManager.PlayerManager playerManager;
        private final boolean weaponIsBareHeads = false;
        private final int weaponId = 837602032;
        private final short registryId = 1202;
        static float distance;
        private int lastPlayerForHello = -1;

        private Player(FakeClientManager.Movement movement, FakeClientManager.Network network, int connectionIndex, int port) {
            this.username = String.format("Client%d", movement.id);
            this.tagColor = Colors.SkyBlue;
            this.speakColor = Colors.GetRandomColor();
            this.isFemale = (int)Math.round(Math.random());
            this.onlineId = -1;
            this.clothes = new ArrayList<>();
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)11, (byte)0, "Shirt_FormalWhite"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)13, (byte)3, "Tie_Full"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)11, (byte)0, "Socks_Ankle"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)13, (byte)0, "Trousers_Suit"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)13, (byte)0, "Suit_Jacket"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)11, (byte)0, "Shoes_Black"));
            this.clothes.add(new FakeClientManager.Player.Clothes((byte)11, (byte)0, "Glasses_Sun"));
            this.worldX = (int)this.x / 10;
            this.worldY = (int)this.y / 10;
            this.movement = movement;
            this.z = 0.0F;
            this.angle = 0.0F;
            this.x = movement.spawn.x;
            this.y = movement.spawn.y;
            this.direction = movement.direction.ToVector();
            this.networkCharacter = new NetworkCharacter();
            this.simulator = new FakeClientManager.ZombieSimulator(this);
            this.playerManager = new FakeClientManager.PlayerManager(this);
            this.client = new FakeClientManager.Client(this, network, connectionIndex, port);
            network.createdClients.put(connectionIndex, this.client);
            this.updateLimiter = new UpdateLimit(1000 / fps);
            this.predictLimiter = new UpdateLimit((long)(predictInterval * 0.6F));
            this.timeSyncLimiter = new UpdateLimit(10000L);
        }

        private float getDistance(float speed) {
            return speed / 3.6F / fps;
        }

        private void teleportMovement() {
            float nx = this.movement.destination.x;
            float ny = this.movement.destination.y;
            FakeClientManager.info(
                this.movement.id,
                String.format(
                    "Player %3d teleport (%9.3f,%9.3f) => (%9.3f,%9.3f) / %9.3f, next in %.3fs",
                    this.onlineId,
                    this.x,
                    this.y,
                    nx,
                    ny,
                    Math.sqrt(Math.pow(nx - this.x, 2.0) + Math.pow(ny - this.y, 2.0)),
                    (float)this.movement.teleportDelay / 1000.0F
                )
            );
            this.x = nx;
            this.y = ny;
            this.angle = 0.0F;
            this.teleportLimiter.Reset(this.movement.teleportDelay);
        }

        private void lineMovement() {
            distance = this.getDistance(this.movement.speed);
            this.direction.set(this.movement.destination.x - this.x, this.movement.destination.y - this.y);
            this.direction.normalize();
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            if (this.x < this.movement.destination.x && nx > this.movement.destination.x
                || this.x > this.movement.destination.x && nx < this.movement.destination.x
                || this.y < this.movement.destination.y && ny > this.movement.destination.y
                || this.y > this.movement.destination.y && ny < this.movement.destination.y) {
                nx = this.movement.destination.x;
                ny = this.movement.destination.y;
            }

            this.x = nx;
            this.y = ny;
        }

        private void circleMovement() {
            this.angle = (this.angle + (float)(2.0 * Math.asin(this.getDistance(this.movement.speed) / 2.0F / this.movement.radius))) % 360.0F;
            float nx = this.movement.spawn.x + (float)(this.movement.radius * Math.sin(this.angle));
            float ny = this.movement.spawn.y + (float)(this.movement.radius * Math.cos(this.angle));
            this.x = nx;
            this.y = ny;
        }

        private FakeClientManager.Zombie getNearestZombie() {
            FakeClientManager.Zombie targetZombie = null;
            float distanceSquared = Float.POSITIVE_INFINITY;

            for (FakeClientManager.Zombie z : this.simulator.zombies.values()) {
                float ds = IsoUtils.DistanceToSquared(this.x, this.y, z.x, z.y);
                if (ds < distanceSquared) {
                    targetZombie = z;
                    distanceSquared = ds;
                }
            }

            return targetZombie;
        }

        private FakeClientManager.Zombie getNearestZombie(FakeClientManager.PlayerManager.RemotePlayer targetPlayer) {
            FakeClientManager.Zombie targetZombie = null;
            float distanceSquared = Float.POSITIVE_INFINITY;

            for (FakeClientManager.Zombie z : this.simulator.zombies.values()) {
                float ds = IsoUtils.DistanceToSquared(targetPlayer.x, targetPlayer.y, z.x, z.y);
                if (ds < distanceSquared) {
                    targetZombie = z;
                    distanceSquared = ds;
                }
            }

            return targetZombie;
        }

        private FakeClientManager.PlayerManager.RemotePlayer getNearestPlayer() {
            FakeClientManager.PlayerManager.RemotePlayer targetPlayer = null;
            float distanceSquared = Float.POSITIVE_INFINITY;
            synchronized (this.playerManager.players) {
                for (FakeClientManager.PlayerManager.RemotePlayer p : this.playerManager.players.values()) {
                    float ds = IsoUtils.DistanceToSquared(this.x, this.y, p.x, p.y);
                    if (ds < distanceSquared) {
                        targetPlayer = p;
                        distanceSquared = ds;
                    }
                }

                return targetPlayer;
            }
        }

        private void aiAttackZombiesMovement() {
            FakeClientManager.Zombie targetZombie = this.getNearestZombie();
            float distance = this.getDistance(this.movement.speed);
            if (targetZombie != null) {
                this.direction.set(targetZombie.x - this.x, targetZombie.y - this.y);
                this.direction.normalize();
            }

            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            this.x = nx;
            this.y = ny;
        }

        private void aiRunAwayFromZombiesMovement() {
            FakeClientManager.Zombie targetZombie = this.getNearestZombie();
            float distance = this.getDistance(this.movement.speed);
            if (targetZombie != null) {
                this.direction.set(this.x - targetZombie.x, this.y - targetZombie.y);
                this.direction.normalize();
            }

            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            this.x = nx;
            this.y = ny;
        }

        private void aiRunToAnotherPlayersMovement() {
            FakeClientManager.PlayerManager.RemotePlayer targetPlayer = this.getNearestPlayer();
            float distance = this.getDistance(this.movement.speed);
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            if (targetPlayer != null) {
                this.direction.set(targetPlayer.x - this.x, targetPlayer.y - this.y);
                float length = this.direction.normalize();
                if (length > 2.0F) {
                    this.x = nx;
                    this.y = ny;
                } else if (this.lastPlayerForHello != targetPlayer.onlineId) {
                    this.lastPlayerForHello = targetPlayer.onlineId;
                }
            }
        }

        private void aiNormalMovement() {
            float distance = this.getDistance(this.movement.speed);
            FakeClientManager.PlayerManager.RemotePlayer targetPlayer = this.getNearestPlayer();
            if (targetPlayer == null) {
                this.aiRunAwayFromZombiesMovement();
            } else {
                float dsp = IsoUtils.DistanceToSquared(this.x, this.y, targetPlayer.x, targetPlayer.y);
                if (dsp > 36.0F) {
                    this.movement.speed = 13.0F;
                    this.movement.motion = FakeClientManager.Movement.Motion.Run;
                } else {
                    this.movement.speed = 4.0F;
                    this.movement.motion = FakeClientManager.Movement.Motion.Walk;
                }

                FakeClientManager.Zombie targetZombie = this.getNearestZombie();
                float dsz = Float.POSITIVE_INFINITY;
                if (targetZombie != null) {
                    dsz = IsoUtils.DistanceToSquared(this.x, this.y, targetZombie.x, targetZombie.y);
                }

                FakeClientManager.Zombie targetZombieForPlayer = this.getNearestZombie(targetPlayer);
                float dsz4player = Float.POSITIVE_INFINITY;
                if (targetZombieForPlayer != null) {
                    dsz4player = IsoUtils.DistanceToSquared(targetPlayer.x, targetPlayer.y, targetZombieForPlayer.x, targetZombieForPlayer.y);
                }

                if (dsz4player < 25.0F) {
                    targetZombie = targetZombieForPlayer;
                    dsz = dsz4player;
                }

                if (dsp > 25.0F || targetZombie == null) {
                    this.direction.set(targetPlayer.x - this.x, targetPlayer.y - this.y);
                    float length = this.direction.normalize();
                    if (length > 4.0F) {
                        float nx = this.x + distance * this.direction.x;
                        float ny = this.y + distance * this.direction.y;
                        this.x = nx;
                        this.y = ny;
                    } else if (this.lastPlayerForHello != targetPlayer.onlineId) {
                        this.lastPlayerForHello = targetPlayer.onlineId;
                    }
                } else if (dsz < 25.0F) {
                    this.direction.set(targetZombie.x - this.x, targetZombie.y - this.y);
                    this.direction.normalize();
                    this.x = this.x + distance * this.direction.x;
                    this.y = this.y + distance * this.direction.y;
                }
            }
        }

        private void checkRequestChunks() {
            int currentWorldX = (int)this.x / 10;
            int currentWorldY = (int)this.y / 10;
            if (Math.abs(currentWorldX - this.worldX) < 13 && Math.abs(currentWorldY - this.worldY) < 13) {
                if (currentWorldX != this.worldX) {
                    if (currentWorldX < this.worldX) {
                        for (int i = -6; i <= 6; i++) {
                            this.client.addChunkRequest(this.worldX - 6, this.worldY + i, 0, i + 6);
                        }
                    } else {
                        for (int i = -6; i <= 6; i++) {
                            this.client.addChunkRequest(this.worldX + 6, this.worldY + i, 12, i + 6);
                        }
                    }
                } else if (currentWorldY != this.worldY) {
                    if (currentWorldY < this.worldY) {
                        for (int i = -6; i <= 6; i++) {
                            this.client.addChunkRequest(this.worldX + i, this.worldY - 6, i + 6, 0);
                        }
                    } else {
                        for (int i = -6; i <= 6; i++) {
                            this.client.addChunkRequest(this.worldX + i, this.worldY + 6, i + 6, 12);
                        }
                    }
                }
            } else {
                int minwx = this.worldX - 6;
                int minwy = this.worldY - 6;
                int maxwx = this.worldX + 6;
                int maxwy = this.worldY + 6;

                for (int xx = minwx; xx <= maxwx; xx++) {
                    for (int yy = minwy; yy <= maxwy; yy++) {
                        this.client.addChunkRequest(xx, yy, xx - minwx, yy - minwy);
                    }
                }
            }

            this.client.requestChunks();
            this.worldX = currentWorldX;
            this.worldY = currentWorldY;
        }

        private void hit() {
            FakeClientManager.info(this.movement.id, String.format("Player %3d hit", this.onlineId));
        }

        private void run() {
            this.simulator.update();
            if (this.updateLimiter.Check()) {
                if (isVOIPEnabled) {
                    FMODManager.instance.tick();
                    VoiceManager.instance.update();
                }

                if (this.movement.doTeleport() && this.teleportLimiter.Check()) {
                    this.teleportMovement();
                }

                switch (this.movement.type) {
                    case Line:
                        this.lineMovement();
                        break;
                    case Circle:
                        this.circleMovement();
                        break;
                    case AIAttackZombies:
                        this.aiAttackZombiesMovement();
                        break;
                    case AIRunAwayFromZombies:
                        this.aiRunAwayFromZombiesMovement();
                        break;
                    case AIRunToAnotherPlayers:
                        this.aiRunToAnotherPlayersMovement();
                        break;
                    case AINormal:
                        this.aiNormalMovement();
                }

                this.checkRequestChunks();
                if (this.predictLimiter.Check()) {
                    int currentTime = (int)(this.client.getServerTime() / 1000000L);
                    this.networkCharacter.checkResetPlayer(currentTime);
                    NetworkCharacter.Transform transform = this.networkCharacter
                        .predict(predictInterval, currentTime, this.x, this.y, this.direction.x, this.direction.y);
                    this.client.sendPlayer(transform, currentTime, this.direction);
                }

                if (this.timeSyncLimiter.Check()) {
                    this.client.sendTimeSync();
                    this.client.sendSyncRadioData();
                }

                if (this.movement.hordeCreator != null && this.movement.hordeCreator.hordeCreatorLimiter.Check()) {
                    this.client
                        .sendCommand(this.movement.hordeCreator.getCommand(PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), PZMath.fastfloor(this.z)));
                }

                if (this.movement.soundMaker != null && this.movement.soundMaker.soundMakerLimiter.Check()) {
                    this.client
                        .sendWorldSound4Player(
                            PZMath.fastfloor(this.x),
                            PZMath.fastfloor(this.y),
                            PZMath.fastfloor(this.z),
                            this.movement.soundMaker.radius,
                            this.movement.soundMaker.radius
                        );
                    this.client.sendChatMessage(this.movement.soundMaker.message);
                    this.client.sendEventPacket(this.onlineId, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), PZMath.fastfloor(this.z), (byte)4, "shout");
                }
            }
        }

        private static class Clothes {
            private final byte flags;
            private final byte text;
            private final String name;

            Clothes(byte flags, byte text, String name) {
                this.flags = flags;
                this.text = text;
                this.name = name;
            }
        }
    }

    private static class PlayerManager {
        private FakeClientManager.Player player;
        private final PlayerPacket playerPacket = new PlayerPacket();
        public final HashMap<Integer, FakeClientManager.PlayerManager.RemotePlayer> players = new HashMap<>();

        public PlayerManager(FakeClientManager.Player _player) {
            this.player = _player;
        }

        private void parsePlayer(ByteBuffer b) {
            PlayerPacket packet = this.playerPacket;
            packet.parse(b, null);
            synchronized (this.players) {
                FakeClientManager.PlayerManager.RemotePlayer p = this.players.get(packet.id);
                if (p == null) {
                    FakeClientManager.trace(this.player.movement.id, String.format("New player %s", p.onlineId));
                }

                p.x = packet.prediction.position.x;
                p.y = packet.prediction.position.y;
                p.z = packet.prediction.position.z;
            }
        }

        private void parsePlayerTimeout(ByteBuffer b) {
            short OnlineID = b.getShort();
            synchronized (this.players) {
                this.players.remove(OnlineID);
            }

            FakeClientManager.trace(this.player.movement.id, String.format("Remove player %s", OnlineID));
        }

        private class RemotePlayer {
            public float x;
            public float y;
            public float z;
            public short onlineId;
            public PlayerPacket playerPacket;

            public RemotePlayer(final short _OnlineId) {
                Objects.requireNonNull(PlayerManager.this);
                super();
                this.playerPacket = new PlayerPacket();
                this.onlineId = _OnlineId;
            }
        }
    }

    private static class SoundMaker {
        private final int radius;
        private final int interval;
        private final String message;
        private final UpdateLimit soundMakerLimiter;

        public SoundMaker(int interval, int radius, String message) {
            this.radius = radius;
            this.message = message;
            this.interval = interval;
            this.soundMakerLimiter = new UpdateLimit(interval);
        }
    }

    private static class StringUTF {
        private char[] chars;
        private ByteBuffer byteBuffer;
        private CharBuffer charBuffer;
        private CharsetEncoder ce;
        private CharsetDecoder cd;

        private int encode(String str) {
            if (this.chars == null || this.chars.length < str.length()) {
                int capacity = (str.length() + 128 - 1) / 128 * 128;
                this.chars = new char[capacity];
                this.charBuffer = CharBuffer.wrap(this.chars);
            }

            str.getChars(0, str.length(), this.chars, 0);
            this.charBuffer.limit(str.length());
            this.charBuffer.position(0);
            if (this.ce == null) {
                this.ce = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            }

            this.ce.reset();
            int maxBytes = (int)((double)str.length() * this.ce.maxBytesPerChar());
            maxBytes = (maxBytes + 128 - 1) / 128 * 128;
            if (this.byteBuffer == null || this.byteBuffer.capacity() < maxBytes) {
                this.byteBuffer = ByteBuffer.allocate(maxBytes);
            }

            this.byteBuffer.clear();
            CoderResult result = this.ce.encode(this.charBuffer, this.byteBuffer, true);
            return this.byteBuffer.position();
        }

        private String decode(int numBytes) {
            if (this.cd == null) {
                this.cd = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
            }

            this.cd.reset();
            int maxChars = (int)((double)numBytes * this.cd.maxCharsPerByte());
            if (this.chars == null || this.chars.length < maxChars) {
                int capacity = (maxChars + 128 - 1) / 128 * 128;
                this.chars = new char[capacity];
                this.charBuffer = CharBuffer.wrap(this.chars);
            }

            this.charBuffer.clear();
            CoderResult result = this.cd.decode(this.byteBuffer, this.charBuffer, true);
            return new String(this.chars, 0, this.charBuffer.position());
        }

        void save(ByteBuffer out, String str) {
            if (str != null && !str.isEmpty()) {
                int numBytes = this.encode(str);
                out.putShort((short)numBytes);
                this.byteBuffer.flip();
                out.put(this.byteBuffer);
            } else {
                out.putShort((short)0);
            }
        }

        String load(ByteBuffer in) {
            int numBytes = in.getShort();
            if (numBytes <= 0) {
                return "";
            } else {
                int maxBytes = (numBytes + 128 - 1) / 128 * 128;
                if (this.byteBuffer == null || this.byteBuffer.capacity() < maxBytes) {
                    this.byteBuffer = ByteBuffer.allocate(maxBytes);
                }

                this.byteBuffer.clear();
                if (in.remaining() < numBytes) {
                    DebugLog.General
                        .error("GameWindow.StringUTF.load> numBytes:" + numBytes + " is higher than the remaining bytes in the buffer:" + in.remaining());
                }

                int limit = in.limit();
                in.limit(in.position() + numBytes);
                this.byteBuffer.put(in);
                in.limit(limit);
                this.byteBuffer.flip();
                return this.decode(numBytes);
            }
        }
    }

    private static class Zombie {
        public long lastUpdate;
        public float x;
        public float y;
        public float z;
        public short onlineId;
        public boolean localOwnership;
        public ZombiePacket zombiePacket;
        public IsoDirections dir = IsoDirections.N;
        public float health = 1.0F;
        public byte walkType = (byte)Rand.Next(NetworkVariables.WalkType.values().length);
        public float dropPositionX;
        public float dropPositionY;
        public boolean isMoving;

        public Zombie(short _OnlineId) {
            this.zombiePacket = new ZombiePacket();
            this.zombiePacket.id = _OnlineId;
            this.onlineId = _OnlineId;
            this.localOwnership = false;
        }
    }

    private static class ZombieSimulator {
        public static FakeClientManager.ZombieSimulator.Behaviour behaviour = FakeClientManager.ZombieSimulator.Behaviour.Stay;
        public static int deleteZombieDistanceSquared = 10000;
        public static int forgotZombieDistanceSquared = 225;
        public static int canSeeZombieDistanceSquared = 100;
        public static int seeZombieDistanceSquared = 25;
        private static boolean canChangeTarget = true;
        private static final int updatePeriod = 100;
        private static final int attackPeriod = 1000;
        public static int maxZombiesPerUpdate = 300;
        private final ByteBuffer bb = ByteBuffer.allocate(1000000);
        private final UpdateLimit updateLimiter = new UpdateLimit(100L);
        private final UpdateLimit attackLimiter = new UpdateLimit(1000L);
        private FakeClientManager.Player player;
        private final ZombiePacket zombiePacket = new ZombiePacket();
        private HashSet<Short> authoriseZombiesCurrent = new HashSet<>();
        private HashSet<Short> authoriseZombiesLast = new HashSet<>();
        private final ArrayList<Short> unknownZombies = new ArrayList<>();
        private final HashMap<Integer, FakeClientManager.Zombie> zombies = new HashMap<>();
        private final ArrayDeque<FakeClientManager.Zombie> zombies4Add = new ArrayDeque<>();
        private final ArrayDeque<FakeClientManager.Zombie> zombies4Delete = new ArrayDeque<>();
        private final HashSet<Short> authoriseZombies = new HashSet<>();
        private final ArrayDeque<FakeClientManager.Zombie> sendQueue = new ArrayDeque<>();
        private static final Vector2 tmpDir = new Vector2();

        public ZombieSimulator(FakeClientManager.Player _player) {
            this.player = _player;
        }

        public void becomeLocal(FakeClientManager.Zombie z) {
            z.localOwnership = true;
        }

        public void becomeRemote(FakeClientManager.Zombie z) {
            z.localOwnership = false;
        }

        public void clear() {
            HashSet<Short> temp = this.authoriseZombiesCurrent;
            this.authoriseZombiesCurrent = this.authoriseZombiesLast;
            this.authoriseZombiesLast = temp;
            this.authoriseZombiesLast.removeIf(zombieId -> this.zombies.get(Integer.valueOf(zombieId)) == null);
            this.authoriseZombiesCurrent.clear();
        }

        public void add(short onlineId) {
            this.authoriseZombiesCurrent.add(onlineId);
        }

        public void receivePacket(ByteBuffer b) {
            short num = b.getShort();

            for (short n = 0; n < num; n++) {
                this.parseZombie(b);
            }
        }

        private void parseZombie(ByteBuffer b) {
            ZombiePacket packet = this.zombiePacket;
            packet.parse(b, null);
            FakeClientManager.Zombie z = this.zombies.get(Integer.valueOf(packet.id));
            if (!this.authoriseZombies.contains(packet.id) || z == null) {
                if (z == null) {
                    z = new FakeClientManager.Zombie(packet.id);
                    this.zombies4Add.add(z);
                    FakeClientManager.trace(this.player.movement.id, String.format("New zombie %s", z.onlineId));
                }

                z.lastUpdate = System.currentTimeMillis();
                z.zombiePacket.copy(packet);
                z.x = packet.realX;
                z.y = packet.realY;
                z.z = packet.realZ;
            }
        }

        public void process() {
            for (Short zombieId : Sets.difference(this.authoriseZombiesCurrent, this.authoriseZombiesLast)) {
                FakeClientManager.Zombie z = this.zombies.get(Integer.valueOf(zombieId));
                if (z != null) {
                    this.becomeLocal(z);
                } else if (!this.unknownZombies.contains(zombieId)) {
                    this.unknownZombies.add(zombieId);
                }
            }

            for (Short zombieIdx : Sets.difference(this.authoriseZombiesLast, this.authoriseZombiesCurrent)) {
                FakeClientManager.Zombie z = this.zombies.get(Integer.valueOf(zombieIdx));
                if (z != null) {
                    this.becomeRemote(z);
                }
            }

            synchronized (this.authoriseZombies) {
                this.authoriseZombies.clear();
                this.authoriseZombies.addAll(this.authoriseZombiesCurrent);
            }
        }

        public void send() {
            if (!this.authoriseZombies.isEmpty() || !this.unknownZombies.isEmpty()) {
                if (this.sendQueue.isEmpty()) {
                    synchronized (this.authoriseZombies) {
                        for (Short zombieId : this.authoriseZombies) {
                            FakeClientManager.Zombie z = this.zombies.get(Integer.valueOf(zombieId));
                            if (z != null && z.onlineId != -1) {
                                this.sendQueue.add(z);
                            }
                        }
                    }
                }

                this.bb.clear();
                this.bb.putShort((short)0);
                int unknownZombiesCount = this.unknownZombies.size();
                this.bb.putShort((short)unknownZombiesCount);

                for (int k = 0; k < this.unknownZombies.size(); k++) {
                    if (this.unknownZombies.get(k) == null) {
                        return;
                    }

                    this.bb.putShort(this.unknownZombies.get(k));
                }

                this.unknownZombies.clear();
                int position = this.bb.position();
                this.bb.putShort((short)maxZombiesPerUpdate);
                int realCount = 0;

                while (!this.sendQueue.isEmpty()) {
                    FakeClientManager.Zombie z = this.sendQueue.poll();
                    if (z.onlineId != -1) {
                        z.zombiePacket.write(this.bb);
                        if (++realCount >= maxZombiesPerUpdate) {
                            break;
                        }
                    }
                }

                if (realCount < maxZombiesPerUpdate) {
                    int endposition = this.bb.position();
                    this.bb.position(position);
                    this.bb.putShort((short)realCount);
                    this.bb.position(endposition);
                }

                if (realCount > 0 || unknownZombiesCount > 0) {
                    ByteBuffer bb2 = this.player.client.network.startPacket();
                    this.player.client.doPacket(PacketTypes.PacketType.ZombieSimulationReliable.getId(), bb2);
                    bb2.put(this.bb.array(), 0, this.bb.position());
                    this.player.client.network.endPacketSuperHighUnreliable(this.player.client.connectionGuid);
                }
            }
        }

        private void simulate(Integer id, FakeClientManager.Zombie zombie) {
            float distanceSquared = IsoUtils.DistanceToSquared(this.player.x, this.player.y, zombie.x, zombie.y);
            if (!(distanceSquared > deleteZombieDistanceSquared) && (zombie.localOwnership || zombie.lastUpdate + 5000L >= System.currentTimeMillis())) {
                tmpDir.set(-zombie.x + this.player.x, -zombie.y + this.player.y);
                if (zombie.isMoving) {
                    float a = 0.2F;
                    zombie.x = PZMath.lerp(zombie.x, zombie.zombiePacket.x, 0.2F);
                    zombie.y = PZMath.lerp(zombie.y, zombie.zombiePacket.y, 0.2F);
                    zombie.z = 0.0F;
                    zombie.dir = IsoDirections.fromAngle(tmpDir);
                }

                if (canChangeTarget) {
                    synchronized (this.player.playerManager.players) {
                        for (FakeClientManager.PlayerManager.RemotePlayer remotePlayer : this.player.playerManager.players.values()) {
                            float remotePlayerDistanceSquared = IsoUtils.DistanceToSquared(remotePlayer.x, remotePlayer.y, zombie.x, zombie.y);
                            if (remotePlayerDistanceSquared < seeZombieDistanceSquared) {
                                zombie.zombiePacket.target = remotePlayer.onlineId;
                                break;
                            }
                        }
                    }
                } else {
                    zombie.zombiePacket.target = this.player.onlineId;
                }

                if (behaviour == FakeClientManager.ZombieSimulator.Behaviour.Stay) {
                    zombie.isMoving = false;
                } else if (behaviour == FakeClientManager.ZombieSimulator.Behaviour.Normal) {
                    if (distanceSquared > forgotZombieDistanceSquared) {
                        zombie.isMoving = false;
                    }

                    if (distanceSquared < canSeeZombieDistanceSquared && (Rand.Next(100) < 1 || zombie.dir == IsoDirections.fromAngle(tmpDir))) {
                        zombie.isMoving = true;
                    }

                    if (distanceSquared < seeZombieDistanceSquared) {
                        zombie.isMoving = true;
                    }
                } else {
                    zombie.isMoving = true;
                }

                float speed = 0.0F;
                if (zombie.isMoving) {
                    Vector2 chrDir = zombie.dir.ToVector();
                    speed = 3.0F;
                    if (distanceSquared < 100.0F) {
                        speed = 6.0F;
                    }

                    long dt = System.currentTimeMillis() - zombie.lastUpdate;
                    zombie.zombiePacket.x = zombie.x + chrDir.x * (float)dt * 0.001F * speed;
                    zombie.zombiePacket.y = zombie.y + chrDir.y * (float)dt * 0.001F * speed;
                    zombie.zombiePacket.z = (byte)zombie.z;
                    zombie.zombiePacket.predictionType = 1;
                } else {
                    zombie.zombiePacket.x = zombie.x;
                    zombie.zombiePacket.y = zombie.y;
                    zombie.zombiePacket.z = (byte)zombie.z;
                    zombie.zombiePacket.predictionType = 0;
                }

                zombie.zombiePacket.booleanVariables = 0;
                if (distanceSquared < 100.0F) {
                    zombie.zombiePacket.booleanVariables = (short)(zombie.zombiePacket.booleanVariables | 2);
                }

                zombie.zombiePacket.timeSinceSeenFlesh = (short)(zombie.isMoving ? 0 : 32767);
                zombie.zombiePacket.smParamTargetAngle = 0;
                zombie.zombiePacket.speedMod = 1000;
                zombie.zombiePacket.walkType = NetworkVariables.WalkType.values()[zombie.walkType];
                zombie.zombiePacket.realX = zombie.x;
                zombie.zombiePacket.realY = zombie.y;
                zombie.zombiePacket.realZ = (byte)zombie.z;
                zombie.zombiePacket.health = (byte)(zombie.health * 100.0F);
                zombie.zombiePacket.realState = NetworkVariables.ZombieState.fromString("fakezombie-" + behaviour.toString().toLowerCase());
                if (zombie.isMoving) {
                    zombie.zombiePacket.pfb.goal = PathFindBehavior2.Goal.Character;
                    zombie.zombiePacket.pfb.target.setID(this.player.onlineId);
                } else {
                    zombie.zombiePacket.pfb.goal = PathFindBehavior2.Goal.None;
                }

                if (distanceSquared < 2.0F && this.attackLimiter.Check()) {
                    zombie.health = zombie.health - FakeClientManager.Player.damage;
                    this.sendHitCharacter(zombie, FakeClientManager.Player.damage);
                    if (zombie.health <= 0.0F) {
                        this.player.client.sendChatMessage("DIE!!");
                        this.zombies4Delete.add(zombie);
                    }
                }

                zombie.lastUpdate = System.currentTimeMillis();
            } else {
                this.zombies4Delete.add(zombie);
            }
        }

        private void writeHitInfoToZombie(ByteBuffer bb, short zombie_id, float x, float y, float damage) {
            bb.put((byte)2);
            bb.putShort(zombie_id);
            bb.put((byte)0);
            bb.putFloat(x);
            bb.putFloat(y);
            bb.putFloat(0.0F);
            bb.putFloat(damage);
            bb.putFloat(1.0F);
            bb.putInt(100);
        }

        private void sendHitCharacter(FakeClientManager.Zombie zombie, float damage) {
            boolean isCritical = false;
            ByteBuffer bb = this.player.client.network.startPacket();
            this.player.client.doPacket(PacketTypes.PacketType.PlayerHitZombie.getId(), bb);
            bb.put((byte)3);
            bb.putShort(this.player.onlineId);
            bb.putShort((short)0);
            bb.putFloat(this.player.x);
            bb.putFloat(this.player.y);
            bb.putFloat(this.player.z);
            bb.putFloat(this.player.direction.x);
            bb.putFloat(this.player.direction.y);
            FakeClientManager.WriteStringUTF(bb, "");
            FakeClientManager.WriteStringUTF(bb, "");
            FakeClientManager.WriteStringUTF(bb, "");
            bb.putShort((short)(0 + 0));
            bb.putFloat(1.0F);
            bb.putFloat(1.0F);
            bb.putFloat(1.0F);
            FakeClientManager.WriteStringUTF(bb, "default");
            byte flags = 0;
            flags = (byte)(flags | 0);
            bb.put(flags);
            bb.put((byte)0);
            bb.putShort((short)0);
            bb.putFloat(1.0F);
            bb.putInt(0);
            byte count = 1;
            bb.put(count);

            for (int i = 0; i < count; i++) {
                this.writeHitInfoToZombie(bb, zombie.onlineId, zombie.x, zombie.y, damage);
            }

            count = 0;
            bb.put(count);
            count = 1;
            bb.put(count);

            for (int i = 0; i < count; i++) {
                this.writeHitInfoToZombie(bb, zombie.onlineId, zombie.x, zombie.y, damage);
            }

            Objects.requireNonNull(this.player);
            bb.put((byte)0);
            bb.putShort(zombie.onlineId);
            bb.putShort((short)(damage >= zombie.health ? 3 : 0));
            bb.putFloat(zombie.x);
            bb.putFloat(zombie.y);
            bb.putFloat(zombie.z);
            bb.putFloat(zombie.dir.ToVector().x);
            bb.putFloat(zombie.dir.ToVector().y);
            FakeClientManager.WriteStringUTF(bb, "");
            FakeClientManager.WriteStringUTF(bb, "");
            FakeClientManager.WriteStringUTF(bb, "");
            bb.putShort((short)0);
            FakeClientManager.WriteStringUTF(bb, "");
            FakeClientManager.WriteStringUTF(bb, "FRONT");
            bb.put((byte)0);
            bb.putFloat(damage);
            bb.putFloat(1.0F);
            bb.putFloat(this.player.direction.x);
            bb.putFloat(this.player.direction.y);
            bb.putFloat(1.0F);
            bb.put((byte)0);
            if (tmpDir.getLength() > 0.0F) {
                zombie.dropPositionX = zombie.x + tmpDir.x / tmpDir.getLength();
                zombie.dropPositionY = zombie.y + tmpDir.y / tmpDir.getLength();
            } else {
                zombie.dropPositionX = zombie.x;
                zombie.dropPositionY = zombie.y;
            }

            bb.putFloat(zombie.dropPositionX);
            bb.putFloat(zombie.dropPositionY);
            bb.put((byte)zombie.z);
            bb.putFloat(zombie.dir.toAngle());
            this.player.client.network.endPacketImmediate(this.player.client.connectionGuid);
        }

        private void sendSendDeadZombie(FakeClientManager.Zombie zombie) {
            ByteBuffer bb = this.player.client.network.startPacket();
            this.player.client.doPacket(PacketTypes.PacketType.ZombieDeath.getId(), bb);
            bb.putShort(zombie.onlineId);
            bb.putFloat(zombie.x);
            bb.putFloat(zombie.y);
            bb.putFloat(zombie.z);
            bb.putFloat(zombie.dir.toAngle());
            bb.put((byte)zombie.dir.index());
            bb.put((byte)0);
            bb.put((byte)0);
            bb.put((byte)0);
            this.player.client.network.endPacketImmediate(this.player.client.connectionGuid);
        }

        public void simulateAll() {
            while (!this.zombies4Add.isEmpty()) {
                FakeClientManager.Zombie z = this.zombies4Add.poll();
                this.zombies.put(Integer.valueOf(z.onlineId), z);
            }

            this.zombies.forEach(this::simulate);

            while (!this.zombies4Delete.isEmpty()) {
                FakeClientManager.Zombie z = this.zombies4Delete.poll();
                this.zombies.remove(Integer.valueOf(z.onlineId));
            }
        }

        public void update() {
            if (this.updateLimiter.Check()) {
                this.simulateAll();
                this.send();
            }
        }

        private static enum Behaviour {
            Stay,
            Normal,
            Attack;
        }
    }
}
