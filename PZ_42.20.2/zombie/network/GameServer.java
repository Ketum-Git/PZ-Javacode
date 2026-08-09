// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientSoundManager;
import zombie.AmbientStreamManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.HiddenFromLua;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.asset.AssetManagers;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.characters.Safety;
import zombie.characters.SafetySystemManager;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandBase;
import zombie.core.ActionManager;
import zombie.core.Core;
import zombie.core.ImportantAreaManager;
import zombie.core.Languages;
import zombie.core.PerformanceSettings;
import zombie.core.ProxyPrintStream;
import zombie.core.ThreadGroups;
import zombie.core.TradingManager;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.WordsFilter;
import zombie.core.backup.ZipBackup;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LimitSizeFileOutputStream;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.model.AiSceneAsset;
import zombie.core.skinnedmodel.model.AiSceneAssetManager;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.AnimationAssetManager;
import zombie.core.skinnedmodel.model.MeshAssetManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemAssetManager;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDAssetManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.NinePatchTextureAssetManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureAssetManager;
import zombie.core.textures.TextureID;
import zombie.core.textures.TextureIDAssetManager;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.GameServerDetails;
import zombie.core.znet.PortMapper;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.IngameState;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.BuildingDef;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.iso.worldgen.WorldGenUtils;
import zombie.iso.zones.Zone;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.chat.ChatServer;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.AddBrokenGlassPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.MetaGridPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.actions.HelicopterPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.hit.HitCharacter;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.WorldSoundPacket;
import zombie.network.server.EventManager;
import zombie.network.statistics.StatisticManager;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombieManager;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModRegistries;
import zombie.seating.SeatingManager;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileSeamManager;
import zombie.util.PZSQLUtils;
import zombie.util.PublicServerUtil;
import zombie.util.StringUtils;
import zombie.vehicleNetworkSound.server.Manager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;
import zombie.worldMap.WorldMapVisitedServer;
import zombie.worldMap.network.HiddenAuthors;
import zombie.worldMap.network.WorldMapServer;

public class GameServer {
    public static final int MAX_PLAYERS = 512;
    public static final int TimeLimitForProcessPackets = 70;
    public static final int PacketsUpdateRate = 200;
    public static final int FPS = 10;
    private static final HashMap<String, GameServer.CCFilter> ccFilters = new HashMap<>();
    public static int test = 432432;
    public static int defaultPort = 16261;
    public static int udpPort = 16262;
    public static String ipCommandline;
    public static int portCommandline = -1;
    public static int udpPortCommandline = -1;
    public static Boolean steamVacCommandline;
    public static boolean guiCommandline;
    public static boolean server;
    public static boolean coop;
    public static boolean debug;
    public static boolean closed;
    public static boolean softReset;
    public static String seed = "";
    public static UdpEngine udpEngine;
    public static final HashMap<Short, Long> IDToAddressMap = new HashMap<>();
    public static final HashMap<Short, IsoPlayer> IDToPlayerMap = new HashMap<>();
    public static final Map<String, Short> UserNameToPlayerMap = new HashMap<>();
    public static final ArrayList<IsoPlayer> Players = new ArrayList<>();
    public static float timeSinceKeepAlive;
    public static final HashSet<UdpConnection> DebugPlayer = new HashSet<>();
    public static int resetId;
    public static final ArrayList<String> ServerMods = new ArrayList<>();
    public static final ArrayList<Long> WorkshopItems = new ArrayList<>();
    public static String[] workshopInstallFolders;
    public static long[] workshopTimeStamps;
    public static String serverName = "servertest";
    public static final DiscordBot discordBot = new DiscordBot(
        serverName, (user, msg) -> ChatServer.getInstance().sendMessageFromDiscordToGeneralChat(user, msg)
    );
    public static String checksum = "";
    public static String gameMap = "Muldraugh, KY";
    public static boolean fastForward;
    public static String ip = "127.0.0.1";
    public static final UdpConnection[] SlotToConnection = new UdpConnection[512];
    public static final HashMap<IsoPlayer, Long> PlayerToAddressMap = new HashMap<>();
    private static boolean done;
    private static boolean launched;
    private static final ArrayList<String> consoleCommands = new ArrayList<>();
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopPlayerUpdateQ = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataHighPriorityQ = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataQ = new ConcurrentLinkedQueue<>();
    private static final ArrayList<IZomboidPacket> MainLoopNetData2 = new ArrayList<>();
    public static final HashMap<Short, Vector2> playerToCoordsMap = new HashMap<>();
    private String poisonousBerry;
    private String poisonousMushroom;
    private String difficulty = "Hardcore";
    private static int droppedPackets;
    private static int countOfDroppedPackets;
    public static int countOfDroppedConnections;
    public static UdpConnection removeZombiesConnection;
    public static UdpConnection removeAnimalsConnection;
    public static UdpConnection removeCorpsesConnection;
    public static UdpConnection removeVehiclesConnection;
    private static final UpdateLimit calcCountPlayersInRelevantPositionLimiter = new UpdateLimit(2000L);
    private static final UpdateLimit sendWorldMapPlayerPositionLimiter = new UpdateLimit(1000L);
    private static int mainCycleExceptionLogCount = 25;
    public static Thread mainThread;
    public static final ArrayList<IsoPlayer> tempPlayers = new ArrayList<>();
    private static final ConcurrentHashMap<String, GameServer.DelayedConnection> MainLoopDelayedDisconnectQ = new ConcurrentHashMap<>();
    private static final Thread shutdownHook = new Thread() {
        @Override
        public void run() {
            try {
                System.out.println("Shutdown handling started");
                CoopSlave.status("UI_ServerStatus_Terminated");
                DebugLog.log(DebugType.Network, "Server exited");
                if (GameServer.softReset) {
                    return;
                }

                GameServer.done = true;
                ServerMap.instance.QueuedQuit();
                Set<Thread> runningThreads = Thread.getAllStackTraces().keySet();

                for (Thread th : runningThreads) {
                    if (th != Thread.currentThread() && !th.isDaemon() && th.getClass().getName().startsWith("zombie")) {
                        System.out.println("Interrupting '" + th.getClass() + "' termination");
                        th.interrupt();
                    }
                }

                for (Thread thx : runningThreads) {
                    if (thx != Thread.currentThread() && !thx.isDaemon() && thx.isInterrupted()) {
                        System.out.println("Waiting '" + thx.getName() + "' termination");
                        thx.join();
                    }
                }
            } catch (InterruptedException var4) {
                System.out.println("Shutdown handling interrupted");
            }

            System.out.println("Shutdown handling finished");
        }
    };

    private static String parseIPFromCommandline(String[] args, int n, String option) {
        if (n == args.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"\" + option + \"\"");
            System.exit(0);
        } else {
            String[] ss = args[n + 1].trim().split("\\.");
            if (ss.length == 4) {
                for (int i = 0; i < 4; i++) {
                    try {
                        int octet = Integer.parseInt(ss[i]);
                        if (octet < 0 || octet > 255) {
                            DebugLog.log("expected IP address after \"" + option + "\", got \"" + args[n + 1] + "\"");
                            System.exit(0);
                        }
                    } catch (NumberFormatException var6) {
                        DebugLog.log("expected IP address after \"" + option + "\", got \"" + args[n + 1] + "\"");
                        System.exit(0);
                    }
                }
            } else {
                DebugLog.log("expected IP address after \"" + option + "\", got \"" + args[n + 1] + "\"");
                System.exit(0);
            }
        }

        return args[n + 1];
    }

    private static int parsePortFromCommandline(String[] args, int n, String option) {
        if (n == args.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"" + option + "\"");
            System.exit(0);
        } else {
            try {
                return Integer.parseInt(args[n + 1].trim());
            } catch (NumberFormatException var4) {
                DebugLog.log("expected an integer after \"" + option + "\"");
                System.exit(0);
            }
        }

        return -1;
    }

    private static boolean parseBooleanFromCommandline(String[] args, int n, String option) {
        if (n == args.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"" + option + "\"");
            System.exit(0);
        } else {
            String arg = args[n + 1].trim();
            if ("true".equalsIgnoreCase(arg)) {
                return true;
            }

            if ("false".equalsIgnoreCase(arg)) {
                return false;
            }

            DebugLog.log("expected true or false after \"" + option + "\"");
            System.exit(0);
        }

        return false;
    }

    public static void setupCoop() throws FileNotFoundException {
        CoopSlave.init();
    }

    @HiddenFromLua
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        mainThread = Thread.currentThread();
        server = true;
        softReset = System.getProperty("softreset") != null;
        Core.getInstance().setConsoleDotTxtSizeKB(20480);
        String consoleDotTxtSizeString = System.getProperty("zomboid.ConsoleDotTxtSizeKB");
        Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);

        for (int n = 0; n < args.length; n++) {
            if (args[n] != null) {
                if (args[n].startsWith("-cachedir=")) {
                    ZomboidFileSystem.instance.setCacheDir(args[n].replace("-cachedir=", "").trim());
                } else if (args[n].startsWith("-console_dot_txt_size_kb=")) {
                    consoleDotTxtSizeString = args[n].replace("-console_dot_txt_size_kb=", "").trim();
                    Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);
                } else if (args[n].equals("-coop")) {
                    coop = true;
                } else if (args[n].equals("-anti-cheats")) {
                    Core.antiCheats = true;
                }
            }
        }

        if (coop) {
            try {
                CoopSlave.initStreams();
            } catch (FileNotFoundException var52) {
                DebugType.General.printException(var52, "", LogSeverity.Error);
            }
        } else {
            try {
                String logFileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "server-console.txt";
                int consoleDotTxtSizeKB = Core.getInstance().getConsoleDotTxtSizeKB();
                LimitSizeFileOutputStream fout = new LimitSizeFileOutputStream(new File(logFileName), consoleDotTxtSizeKB);
                PrintStream fileStream = new PrintStream(fout, true);
                System.setOut(new ProxyPrintStream(System.out, fileStream));
                System.setErr(new ProxyPrintStream(System.err, fileStream));
            } catch (FileNotFoundException var51) {
                DebugType.General.printException(var51, "", LogSeverity.Error);
            }
        }

        DebugLog.getInstance().init();
        LoggerManager.init();
        DebugType.DetailedInfo.trace("cachedir set to \"" + ZomboidFileSystem.instance.getCacheDir() + "\"");
        if (coop) {
            try {
                setupCoop();
                CoopSlave.status("UI_ServerStatus_Initialising");
            } catch (FileNotFoundException var50) {
                DebugType.General.printException(var50, "", LogSeverity.Error);
                SteamUtils.shutdown();
                System.exit(37);
                return;
            }
        }

        PZSQLUtils.init();
        Clipper.init();
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        if (System.getProperty("debug") != null) {
            debug = true;
            Core.debug = true;
        }

        DebugLog.setDefaultLogSeverity();
        if (Core.debug) {
            DebugLog.getInstance().loadDebugConfig(null);
        }

        DebugLog.printLogLevels();
        DebugType.General.println("version=%s demo=%s", Core.getInstance().getVersion(), false);
        if (!"ffe7a8a4b1".isEmpty()) {
            DebugType.General.println("revision=%s", "ffe7a8a4b1");
        }

        seed = WorldGenUtils.INSTANCE.generateSeed();

        for (int nx = 0; nx < args.length; nx++) {
            if (args[nx] != null) {
                if (!args[nx].startsWith("-disablelog=")) {
                    if (args[nx].startsWith("-debuglog=")) {
                        for (String t : args[nx].replace("-debuglog=", "").split(",")) {
                            try {
                                DebugLog.setLogEnabled(DebugType.valueOf(t), true);
                            } catch (IllegalArgumentException var48) {
                            }
                        }
                    } else if (args[nx].equals("-adminusername")) {
                        if (nx == args.length - 1) {
                            DebugLog.log("expected argument after \"-adminusername\"");
                            System.exit(0);
                        } else if (!ServerWorldDatabase.isValidUserName(args[nx + 1].trim())) {
                            DebugLog.log("invalid username given to \"-adminusername\"");
                            System.exit(0);
                        } else {
                            ServerWorldDatabase.instance.commandLineAdminUsername = args[nx + 1].trim();
                            nx++;
                        }
                    } else if (args[nx].equals("-adminpassword")) {
                        if (nx == args.length - 1) {
                            DebugLog.log("expected argument after \"-adminpassword\"");
                            System.exit(0);
                        } else if (args[nx + 1].trim().isEmpty()) {
                            DebugLog.log("empty argument given to \"-adminpassword\"");
                            System.exit(0);
                        } else {
                            ServerWorldDatabase.instance.commandLineAdminPassword = args[nx + 1].trim();
                            nx++;
                        }
                    } else if (!args[nx].startsWith("-cachedir=")) {
                        if (args[nx].equals("-ip")) {
                            ipCommandline = parseIPFromCommandline(args, nx, "-ip");
                            nx++;
                        } else if (args[nx].equals("-gui")) {
                            guiCommandline = true;
                        } else if (args[nx].equals("-nosteam")) {
                            System.setProperty("zomboid.steam", "0");
                        } else if (args[nx].equals("-port")) {
                            portCommandline = parsePortFromCommandline(args, nx, "-port");
                            nx++;
                        } else if (args[nx].equals("-udpport")) {
                            udpPortCommandline = parsePortFromCommandline(args, nx, "-udpport");
                            nx++;
                        } else if (args[nx].equals("-steamvac")) {
                            steamVacCommandline = parseBooleanFromCommandline(args, nx, "-steamvac");
                            nx++;
                        } else if (args[nx].equals("-servername")) {
                            if (nx == args.length - 1) {
                                DebugLog.log("expected argument after \"-servername\"");
                                System.exit(0);
                            } else if (args[nx + 1].trim().isEmpty()) {
                                DebugLog.log("empty argument given to \"-servername\"");
                                System.exit(0);
                            } else {
                                serverName = args[nx + 1].trim();
                                nx++;
                            }
                        } else if (args[nx].equals("-coop")) {
                            ServerWorldDatabase.instance.doAdmin = false;
                        } else if (args[nx].startsWith("-seed=")) {
                            try {
                                seed = args[nx].replace("-seed=", "");
                                if (!seed.isEmpty()) {
                                    ServerOptions.instance.seed.setValue(seed);
                                }
                            } catch (IllegalArgumentException var47) {
                            }
                        } else if (args[nx].equals("-no-worldgen")) {
                            IsoChunk.doWorldgen = false;
                        } else if (args[nx].equals("-no-foraging")) {
                            IsoChunk.doForaging = false;
                        } else if (args[nx].equals("-no-attachment")) {
                            IsoChunk.doAttachments = false;
                        } else {
                            DebugLog.log("unknown option \"" + args[nx] + "\"");
                        }
                    }
                } else {
                    for (String t : args[nx].replace("-disablelog=", "").split(",")) {
                        if ("All".equals(t)) {
                            for (DebugType dt : DebugType.values()) {
                                DebugLog.setLogEnabled(dt, false);
                            }
                        } else {
                            try {
                                DebugLog.setLogEnabled(DebugType.valueOf(t), false);
                            } catch (IllegalArgumentException var49) {
                            }
                        }
                    }
                }
            }
        }

        DebugType.DetailedInfo.trace("server name is \"" + serverName + "\"");
        String versionUnsupportedString = isWorldVersionUnsupported();
        if (versionUnsupportedString != null) {
            DebugLog.log(versionUnsupportedString);
            CoopSlave.status(versionUnsupportedString);
        } else {
            SteamUtils.init();
            RakNetPeerInterface.init();
            ZombiePopulationManager.init();
            PathfindNative.init();

            try {
                ZomboidFileSystem.instance.init();
                Languages.instance.init();
                Translator.loadFiles();
            } catch (Exception var46) {
                DebugType.General.printException(var46, "Exception Thrown", LogSeverity.Error);
                DebugType.General.println("Server Terminated.");
            }

            SeatingManager.getInstance().init();
            ServerOptions.instance.init();
            initClientCommandFilter();
            if (portCommandline != -1) {
                ServerOptions.instance.defaultPort.setValue(portCommandline);
            }

            if (udpPortCommandline != -1) {
                ServerOptions.instance.udpPort.setValue(udpPortCommandline);
            }

            if (steamVacCommandline != null) {
                ServerOptions.instance.steamVac.setValue(steamVacCommandline);
            }

            defaultPort = ServerOptions.instance.defaultPort.getValue();
            GameServer.udpPort = ServerOptions.instance.udpPort.getValue();
            if (CoopSlave.instance != null) {
                ServerOptions.instance.serverPlayerId.setValue("");
            }

            if (SteamUtils.isSteamModeEnabled()) {
                String s = ServerOptions.instance.publicName.getValue();
                if (s == null || s.isEmpty()) {
                    ServerOptions.instance.publicName.setValue("My PZ Server");
                }
            }

            String map = ServerOptions.instance.map.getValue();
            if (map != null && !map.trim().isEmpty()) {
                gameMap = map.trim();
                if (gameMap.contains(";")) {
                    String[] ss = gameMap.split(";");
                    map = ss[0];
                }

                Core.gameMap = map.trim();
            }

            String mods = ServerOptions.instance.mods.getValue();
            if (mods != null) {
                String[] ss = mods.replace("\\", "").split(";");

                for (String modId : ss) {
                    if (!modId.trim().isEmpty()) {
                        ServerMods.add(modId.trim());
                    }
                }
            }

            if (SteamUtils.isSteamModeEnabled()) {
                int serverMode = ServerOptions.instance.steamVac.getValue() ? 3 : 2;
                if (!SteamGameServer.Init(ipCommandline, defaultPort, GameServer.udpPort, serverMode, Core.getInstance().getSteamServerVersion())) {
                    SteamUtils.shutdown();
                    return;
                }

                SteamGameServer.SetProduct("zomboid");
                SteamGameServer.SetGameDescription("Project Zomboid");
                SteamGameServer.SetModDir("zomboid");
                SteamGameServer.SetDedicatedServer(true);
                SteamGameServer.SetMaxPlayerCount(ServerOptions.getInstance().getMaxPlayers());
                SteamGameServer.SetServerName(ServerOptions.instance.publicName.getValue());
                SteamGameServer.SetMapName(ServerOptions.instance.map.getValue());
                setupSteamGameServer();
                String workshopItems = ServerOptions.instance.workshopItems.getValue();
                if (workshopItems != null) {
                    String[] ss = workshopItems.split(";");

                    for (String itemID : ss) {
                        itemID = itemID.trim();
                        if (!itemID.isEmpty() && SteamUtils.isValidSteamID(itemID)) {
                            WorkshopItems.add(SteamUtils.convertStringToSteamID(itemID));
                        }
                    }
                }

                if (coop) {
                    CoopSlave.instance.sendMessage("status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Count", WorkshopItems.size()));
                }

                SteamWorkshop.init();
                ZomboidFileSystem.instance.resetModFolders();
                SteamGameServer.LogOnAnonymous();
                SteamGameServer.EnableHeartBeats(true);
                DebugLog.log("Waiting for response from Steam servers");

                while (true) {
                    SteamUtils.runLoop();
                    int state = SteamGameServer.GetSteamServersConnectState();
                    if (state == 1) {
                        if (coop) {
                            CoopSlave.status("UI_ServerStatus_Downloading_Workshop_Items");
                        }

                        if (!GameServerWorkshopItems.Install(WorkshopItems)) {
                            return;
                        }
                        break;
                    }

                    if (state == 2) {
                        DebugLog.log("Failed to connect to Steam servers");
                        SteamUtils.shutdown();
                        return;
                    }

                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException var45) {
                    }
                }
            }

            ZipBackup.onStartup();
            ZipBackup.onVersion();
            int updateDBCount = 0;

            try {
                ServerWorldDatabase.instance.create();
            } catch (ClassNotFoundException | SQLException var44) {
                DebugType.General.printException(var44, "", LogSeverity.Error);
            }

            Roles.init();
            if (ServerOptions.instance.uPnp.getValue()) {
                DebugLog.log("Router detection/configuration starting.");
                DebugLog.log("If the server hangs here, set UPnP=false.");
                PortMapper.startup();
                if (PortMapper.discover()) {
                    DebugType.DetailedInfo.trace("UPnP-enabled internet gateway found: " + PortMapper.getGatewayInfo());
                    String extAddr = PortMapper.getExternalAddress();
                    DebugType.DetailedInfo.trace("External IP address: " + extAddr);
                    DebugLog.log("trying to setup port forwarding rules...");
                    int leaseTime = 86400;
                    boolean force = true;
                    if (PortMapper.addMapping(defaultPort, defaultPort, "PZ Server default port", "UDP", 86400, true)) {
                        DebugLog.log(DebugType.Network, "Default port has been mapped successfully");
                    } else {
                        DebugLog.log(DebugType.Network, "Failed to map default port");
                    }

                    if (SteamUtils.isSteamModeEnabled()) {
                        int udpPort = ServerOptions.instance.udpPort.getValue();
                        if (PortMapper.addMapping(udpPort, udpPort, "PZ Server UDPPort", "UDP", 86400, true)) {
                            DebugLog.log(DebugType.Network, "AdditionUDPPort has been mapped successfully");
                        } else {
                            DebugLog.log(DebugType.Network, "Failed to map AdditionUDPPort");
                        }
                    }
                } else {
                    DebugLog.log(
                        DebugType.Network,
                        "No UPnP-enabled Internet gateway found, you must configure port forwarding on your gateway manually in order to make your server accessible from the Internet."
                    );
                }
            }

            Core.getInstance().setGameMode("Multiplayer");
            done = false;
            DebugLog.log(DebugType.Network, "Initialising Server Systems...");
            CoopSlave.status("UI_ServerStatus_Initialising");

            try {
                doMinimumInit();
            } catch (Exception var43) {
                DebugType.General.printException(var43, "Exception Thrown", LogSeverity.Error);
                DebugType.General.println("Server Terminated.");
            }

            LosUtil.init(100, 100);
            ChatServer.getInstance().init();
            DebugLog.log(DebugType.Network, "Loading world...");
            CoopSlave.status("UI_ServerStatus_LoadingWorld");

            try {
                ClimateManager.setInstance(new ClimateManager());
                RagdollSettingsManager.setInstance(new RagdollSettingsManager());
                IsoWorld.instance.init();
            } catch (Exception var42) {
                DebugType.General.printException(var42, "Exception Thrown", LogSeverity.Error);
                DebugType.General.println("Server Terminated.");
                CoopSlave.status("UI_ServerStatus_Terminated");
                return;
            }

            File testFile = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
            if (!testFile.exists()) {
                ServerOptions.instance.changeOption("ResetID", Integer.toString(Rand.Next(100000000)));
            }

            try {
                SpawnPoints.instance.initServer2(IsoWorld.instance.metaGrid);
            } catch (Exception var41) {
                DebugType.General.printException(var41, "", LogSeverity.Error);
            }

            LuaEventManager.triggerEvent("OnGameTimeLoaded");
            SGlobalObjects.initSystems();
            SoundManager.instance = new SoundManager();
            AmbientStreamManager.instance = new AmbientSoundManager();
            AmbientStreamManager.instance.init();
            ServerMap.instance.lastSaved = System.currentTimeMillis();
            VehicleManager.instance = new VehicleManager();
            ServerPlayersVehicles.instance.init();
            DebugOptions.instance.init();
            GameProfiler.init();
            WorldMapServer.instance.readSavefile();

            try {
                startServer();
            } catch (ConnectException var40) {
                DebugType.General.printException(var40, "", LogSeverity.Error);
                SteamUtils.shutdown();
                return;
            }

            if (SteamUtils.isSteamModeEnabled()) {
                DebugType.DetailedInfo.trace("##########\nServer Steam ID " + SteamGameServer.GetSteamID() + "\n##########");
            }

            UpdateLimit serverUpdateLimiter = new UpdateLimit(100L);
            PerformanceSettings.setLockFPS(10);
            IngameState statex = new IngameState();
            float averageFPS = PerformanceSettings.getLockFPS();
            long serverCycle = System.currentTimeMillis();
            if (!SteamUtils.isSteamModeEnabled()) {
                PublicServerUtil.init();
                PublicServerUtil.insertOrUpdate();
            }

            ServerLOS.init();
            NetworkAIParams.Init();
            int rconPort = ServerOptions.instance.rconPort.getValue();
            String rconPwd = ServerOptions.instance.rconPassword.getValue();
            if (rconPort != 0 && rconPwd != null && !rconPwd.isEmpty()) {
                String isLocal = System.getProperty("rconlo");
                RCONServer.init(rconPort, rconPwd, isLocal != null);
            }

            LuaManager.GlobalObject.refreshAnimSets(true);

            while (!done) {
                try {
                    long startServerCycle = System.nanoTime();
                    MainLoopNetData2.clear();

                    for (IZomboidPacket data = MainLoopNetDataHighPriorityQ.poll(); data != null; data = MainLoopNetDataHighPriorityQ.poll()) {
                        MainLoopNetData2.add(data);
                    }

                    Iterator<Entry<String, GameServer.DelayedConnection>> iterator = MainLoopDelayedDisconnectQ.entrySet().iterator();

                    while (iterator.hasNext()) {
                        GameServer.DelayedConnection packet = iterator.next().getValue();
                        if (packet.isCooldown()) {
                            packet.disconnect();
                            iterator.remove();
                        }
                    }

                    NetworkStatistic.getInstance().packets.increase(MainLoopNetData2.size());

                    for (int nxx = 0; nxx < MainLoopNetData2.size(); nxx++) {
                        IZomboidPacket data = MainLoopNetData2.get(nxx);
                        if (data.isConnect()) {
                            if (!closed) {
                                ((GameServer.DelayedConnection)data).connect();
                            } else {
                                ((GameServer.DelayedConnection)data).connection.forceDisconnect("server-closed");
                            }
                        } else if (data.isDisconnect()) {
                            ((GameServer.DelayedConnection)data).disconnect();
                        } else {
                            mainLoopDealWithNetData((ZomboidNetData)data);
                        }
                    }

                    MainLoopNetData2.clear();

                    for (IZomboidPacket data = MainLoopPlayerUpdateQ.poll(); data != null; data = MainLoopPlayerUpdateQ.poll()) {
                        MainLoopNetData2.add(data);
                    }

                    NetworkStatistic.getInstance().packets.increase(MainLoopNetData2.size());

                    for (int nxxx = 0; nxxx < MainLoopNetData2.size(); nxxx++) {
                        IZomboidPacket data = MainLoopNetData2.get(nxxx);

                        try (AbstractPerformanceProfileProbe playerCount = GameServer.s_performance.mainLoopDealWithNetData.profile()) {
                            mainLoopDealWithNetData((ZomboidNetData)data);
                        }
                    }

                    MainLoopNetData2.clear();

                    for (IZomboidPacket data = MainLoopNetDataQ.poll(); data != null; data = MainLoopNetDataQ.poll()) {
                        MainLoopNetData2.add(data);
                    }

                    for (int nxxx = 0; nxxx < MainLoopNetData2.size(); nxxx++) {
                        if (nxxx % 10 == 0 && (System.nanoTime() - startServerCycle) / 1000000L > 70L) {
                            if (droppedPackets == 0) {
                                String message = "Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.";
                                DebugLog.log("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                                ChatServer.getInstance()
                                    .sendMessageToAdminChat(
                                        "Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections."
                                    );
                                EventManager.instance()
                                    .report("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                            }

                            droppedPackets += 2;
                            countOfDroppedPackets = countOfDroppedPackets + (MainLoopNetData2.size() - nxxx);
                            break;
                        }

                        IZomboidPacket data = MainLoopNetData2.get(nxxx);

                        try (AbstractPerformanceProfileProbe var117 = GameServer.s_performance.mainLoopDealWithNetData.profile()) {
                            mainLoopDealWithNetData((ZomboidNetData)data);
                        }
                    }

                    MainLoopNetData2.clear();
                    if (droppedPackets == 1) {
                        DebugLog.log(
                            "Server is working normal. Server will not drop updates of vehicle's physics. The server is open for new connections. Server dropped "
                                + countOfDroppedPackets
                                + " packets and "
                                + countOfDroppedConnections
                                + " connections."
                        );
                        countOfDroppedPackets = 0;
                        countOfDroppedConnections = 0;
                    }

                    droppedPackets = Math.max(0, Math.min(1000, droppedPackets - 1));
                    if (!serverUpdateLimiter.Check()) {
                        long delay = PZMath.clamp((5000000L - System.nanoTime() + startServerCycle) / 1000000L, 0L, 100L);
                        if (delay > 0L) {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException var39) {
                                DebugType.General.printException(var39, "", LogSeverity.Error);
                            }
                        }
                    } else {
                        IsoCamera.frameState.frameCount++;
                        IsoCamera.frameState.updateUnPausedAccumulator();

                        try (AbstractPerformanceProfileProbe var107 = GameServer.s_performance.frameStep.profile()) {
                            timeSinceKeepAlive = timeSinceKeepAlive + GameTime.getInstance().getMultiplier();
                            ServerMap.instance.preupdate();
                            synchronized (consoleCommands) {
                                for (int i = 0; i < consoleCommands.size(); i++) {
                                    String command = consoleCommands.get(i);

                                    try {
                                        if (CoopSlave.instance == null || !CoopSlave.instance.handleCommand(command)) {
                                            System.out.println(handleServerCommand(command, null));
                                        }
                                    } catch (Exception var56) {
                                        DebugType.General.printException(var56, "", LogSeverity.Error);
                                    }
                                }

                                consoleCommands.clear();
                            }

                            if (removeZombiesConnection != null) {
                                NetworkZombieManager.removeZombies(removeZombiesConnection);
                                removeZombiesConnection = null;
                            }

                            if (removeAnimalsConnection != null) {
                                AnimalInstanceManager.removeAnimals(removeAnimalsConnection);
                                removeAnimalsConnection = null;
                            }

                            if (removeCorpsesConnection != null) {
                                IsoDeadBody.removeDeadBodies(removeCorpsesConnection);
                                removeCorpsesConnection = null;
                            }

                            if (removeVehiclesConnection != null) {
                                for (IsoPlayer player : removeVehiclesConnection.players) {
                                    if (player != null) {
                                        VehicleManager.instance.removeVehicles(player);
                                    }
                                }

                                removeVehiclesConnection = null;
                            }

                            try (AbstractPerformanceProfileProbe var115 = GameServer.s_performance.RCONServerUpdate.profile()) {
                                RCONServer.update();
                            }

                            try {
                                MapCollisionData.instance.updateGameState();
                                statex.update();
                                VehicleManager.instance.serverUpdate();
                                ObjectIDManager.getInstance().checkForSaveDataFile(false);
                            } catch (Exception var38) {
                                DebugType.General.printException(var38, "", LogSeverity.Error);
                            }

                            int asleepCount = 0;
                            int playerCount = 0;

                            for (int nxxx = 0; nxxx < Players.size(); nxxx++) {
                                IsoPlayer p = Players.get(nxxx);
                                if (p.isAlive()) {
                                    if (!IsoWorld.instance.currentCell.getObjectList().contains(p)) {
                                        IsoWorld.instance.currentCell.getObjectList().add(p);
                                    }

                                    playerCount++;
                                    if (p.isAsleep()) {
                                        asleepCount++;
                                    }
                                }

                                ServerMap.instance.characterIn(p);
                            }

                            ImportantAreaManager.getInstance().process(statex.paused);
                            setFastForward(ServerOptions.instance.sleepAllowed.getValue() && playerCount > 0 && asleepCount == playerCount);
                            boolean needCalcCountPlayersInRelevantPosition = calcCountPlayersInRelevantPositionLimiter.Check();

                            for (int nxxx = 0; nxxx < udpEngine.connections.size(); nxxx++) {
                                UdpConnection c = udpEngine.connections.get(nxxx);
                                if (needCalcCountPlayersInRelevantPosition) {
                                    c.calcCountPlayersInRelevantPosition();
                                }

                                for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                                    Vector3 area = c.connectArea[playerIndex];
                                    if (area != null) {
                                        ServerMap.instance.characterIn(PZMath.fastfloor(area.x), PZMath.fastfloor(area.y), PZMath.fastfloor(area.z));
                                    }

                                    ClientServerMap.characterIn(c, playerIndex);
                                }

                                if (c.getPlayerDownloadServer() != null) {
                                    c.getPlayerDownloadServer().update();
                                }
                            }

                            Set<IsoMovingObject> toRemove = new HashSet<>();

                            for (IsoMovingObject o : IsoWorld.instance.currentCell.getObjectList()) {
                                if (!(o instanceof IsoAnimal) && o instanceof IsoPlayer && !Players.contains(o)) {
                                    DebugLog.log("Disconnected player in CurrentCell.getObjectList() removed");
                                    toRemove.add(o);
                                }
                            }

                            IsoWorld.instance.currentCell.getObjectList().removeAll(toRemove);
                            toRemove.clear();
                            if (++updateDBCount > 150) {
                                for (int nxxx = 0; nxxx < udpEngine.connections.size(); nxxx++) {
                                    UdpConnection connection = udpEngine.connections.get(nxxx);

                                    try {
                                        if (connection.getUserName() == null
                                            && !connection.awaitingCoopApprove
                                            && !LoginQueue.isInTheQueue(connection)
                                            && connection.isConnectionAttemptTimeout()
                                            && (!connection.googleAuth || connection.isGoogleAuthTimeout())) {
                                            disconnect(connection, "connection-attempt-timeout");
                                            udpEngine.forceDisconnect(connection.getConnectedGUID(), "connection-attempt-timeout");
                                        }
                                    } catch (Exception var58) {
                                        DebugType.General.printException(var58, "", LogSeverity.Error);
                                    }
                                }

                                updateDBCount = 0;
                            }

                            ServerMap.instance.postupdate();

                            try {
                                ServerGUI.update();
                            } catch (Exception var37) {
                                DebugType.General.printException(var37, "", LogSeverity.Error);
                            }

                            long serverCycleLast = serverCycle;
                            serverCycle = System.currentTimeMillis();
                            long dif = serverCycle - serverCycleLast;
                            float frames = 1000.0F / (float)dif;
                            if (!Float.isNaN(frames)) {
                                averageFPS = (float)(averageFPS + Math.min((frames - averageFPS) * 0.05, 1.0));
                            }

                            GameTime.instance.fpsMultiplier = 60.0F / averageFPS;
                            launchCommandHandler();
                            StatisticManager.getInstance().update(dif);
                            if (!SteamUtils.isSteamModeEnabled()) {
                                PublicServerUtil.update();
                                PublicServerUtil.updatePlayerCountIfChanged();
                            }

                            for (int i = 0; i < udpEngine.connections.size(); i++) {
                                UdpConnection connection = udpEngine.connections.get(i);
                                connection.getValidator().update();
                                if (!connection.chunkObjectStateRequests.isEmpty()) {
                                    int chunksPerWidth = 8;

                                    for (int j = 0; j < connection.chunkObjectStateRequests.size(); j += 2) {
                                        short wx = connection.chunkObjectStateRequests.get(j);
                                        short wy = connection.chunkObjectStateRequests.get(j + 1);
                                        if (!connection.RelevantTo(wx * 8 + 4, wy * 8 + 4, connection.getChunkGridWidth() * 4 * 8)) {
                                            connection.chunkObjectStateRequests.remove(j, 2);
                                            j -= 2;
                                        }
                                    }
                                }
                            }

                            if (sendWorldMapPlayerPositionLimiter.Check()) {
                                try {
                                    sendWorldMapPlayerPosition();
                                } catch (Exception var36) {
                                    boolean var133 = true;
                                }
                            }

                            if (CoopSlave.instance != null) {
                                CoopSlave.instance.update();
                                if (CoopSlave.instance.masterLost()) {
                                    DebugLog.log("Coop master is not responding, terminating");
                                    ServerMap.instance.QueueQuit();
                                }
                            }

                            LoginQueue.update();
                            ZipBackup.onPeriod();
                            SteamUtils.runLoop();
                            TradingManager.getInstance().update();
                            WarManager.update();
                            SafeHouse.update();
                            NetworkPlayerManager.getInstance().update();
                            GameWindow.fileSystem.updateAsyncTransactions();
                            WorldMapVisitedServer.getInstance().update();
                        } catch (Exception var60) {
                            if (mainCycleExceptionLogCount-- > 0) {
                                DebugType.Multiplayer.printException(var60, "Server processing error", LogSeverity.Error);
                            }
                        }
                    }
                } catch (Exception var61) {
                    if (mainCycleExceptionLogCount-- > 0) {
                        DebugType.Multiplayer.printException(var61, "Server error", LogSeverity.Error);
                    }
                }
            }

            System.exit(0);
        }
    }

    public static void setupSteamGameServer() {
        SteamGameServer.SetServerName(ServerOptions.instance.publicName.getValue());
        SteamGameServer.SetKeyValue("description", ServerOptions.instance.publicDescription.getValue());
        SteamGameServer.SetKeyValue("version", Core.getInstance().getVersionNumber());
        SteamGameServer.SetKeyValue("open", ServerOptions.instance.open.getValue() ? "1" : "0");
        SteamGameServer.SetKeyValue("public", ServerOptions.instance.isPublic.getValue() ? "1" : "0");
        SteamGameServer.SetKeyValue("pvp", ServerOptions.instance.pvp.getValue() ? "1" : "0");
        String tags = ServerOptions.instance.isPublic.getValue() ? "" : "hidden";
        tags = tags + (CoopSlave.instance != null ? ";hosted" : "");
        tags = tags + (ServerOptions.instance.mods.getValue().isEmpty() ? ";vanilla" : ";modded");
        tags = tags + (!ServerOptions.instance.open.getValue() ? ";closed" : "");
        tags = tags + (ServerOptions.instance.pvp.getValue() ? ";pvp" : "");
        tags = tags + ";VERSION:" + Core.getInstance().getVersionNumber();
        SteamGameServer.SetGameTags(tags);
        String modsString = ServerOptions.instance.mods.getValue();
        int totalMods = 0;
        String[] modIDs = modsString.split(";");

        for (String modID : modIDs) {
            if (!StringUtils.isNullOrWhitespace(modID)) {
                totalMods++;
            }
        }

        if (modsString.length() > 128) {
            StringBuilder sb = new StringBuilder();
            String[] ss = modsString.split(";");

            for (String modIDx : ss) {
                if (sb.length() + 1 + modIDx.length() > 128) {
                    break;
                }

                if (!sb.isEmpty()) {
                    sb.append(';');
                }

                sb.append(modIDx);
            }

            modsString = sb.toString();
        }

        SteamGameServer.SetKeyValue("mods", modsString);
        SteamGameServer.SetKeyValue("modCount", String.valueOf(totalMods));
    }

    public static Server steamGetInternetServerDetails(GameServerDetails steamServer) {
        if (steamServer == null) {
            return null;
        } else {
            Server newServer = new Server();
            newServer.setName(steamServer.name);
            newServer.setDescription("");
            newServer.setSteamId(Long.toString(steamServer.steamId));
            newServer.setPing(Integer.toString(steamServer.ping));
            newServer.setPlayers(Integer.toString(steamServer.numPlayers));
            newServer.setMaxPlayers(Integer.toString(steamServer.maxPlayers));
            newServer.setOpen(!steamServer.tags.contains("closed"));
            newServer.setPublic(!steamServer.tags.contains("hidden"));
            newServer.setIp(steamServer.address);
            newServer.setPort(steamServer.port);
            newServer.setMods(steamServer.tags.contains("modded") ? "+" : "");
            if (!steamServer.tags.contains("VERSION:") && !steamServer.tags.replace("hidden", "").replace("hosted", "").replace(";", "").isEmpty()) {
                newServer.setMods(steamServer.tags.replace(";hosted", "").replace("hidden", ""));
            }

            newServer.setHosted(steamServer.tags.contains("hosted"));
            newServer.setVersion("");
            int versionIndex = steamServer.tags.indexOf("VERSION:");
            if (versionIndex != -1) {
                newServer.setVersion(steamServer.tags.substring(versionIndex + "VERSION:".length()));
            }

            newServer.setLastUpdate(1);
            newServer.setPasswordProtected(steamServer.passwordProtected);
            newServer.setMapName(steamServer.map);
            return newServer;
        }
    }

    private static void launchCommandHandler() {
        if (!launched) {
            launched = true;
            new Thread(ThreadGroups.Workers, () -> {
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

                    while (true) {
                        while (!input.ready()) {
                            Thread.sleep(100L);
                        }

                        String command = input.readLine();
                        if (command == null) {
                            consoleCommands.add("process-status@eof");
                            break;
                        }

                        if (!command.isEmpty()) {
                            System.out.println("command entered via server console (System.in): \"" + command + "\"");
                            synchronized (consoleCommands) {
                                consoleCommands.add(command);
                            }
                        }
                    }
                } catch (Exception var5) {
                    DebugType.General.printException(var5, "", LogSeverity.Error);
                }
            }, "command handler").start();
        }
    }

    public static String rcon(String command) {
        try {
            return handleServerCommand(command, null);
        } catch (Throwable var2) {
            var2.printStackTrace();
            return null;
        }
    }

    private static String handleServerCommand(String input, UdpConnection connection) {
        if (input == null) {
            return null;
        } else {
            String adminUsername = "admin";
            Role accessLevel = Roles.getDefaultForAdmin();
            if (connection != null) {
                adminUsername = connection.getUserName();
                if (!connection.isCoopHost) {
                    accessLevel = connection.getRole();
                }
            }

            Class<?> cls = CommandBase.findCommandCls(input);
            if (cls != null) {
                Constructor<?> constructor = cls.getConstructors()[0];

                try {
                    CommandBase com = (CommandBase)constructor.newInstance(adminUsername, accessLevel, input, connection);
                    return com.Execute();
                } catch (InvocationTargetException var7) {
                    DebugType.General.printException(var7, "", LogSeverity.Error);
                    return "A InvocationTargetException error occured";
                } catch (IllegalAccessException var8) {
                    DebugType.General.printException(var8, "", LogSeverity.Error);
                    return "A IllegalAccessException error occured";
                } catch (InstantiationException var9) {
                    DebugType.General.printException(var9, "", LogSeverity.Error);
                    return "A InstantiationException error occured";
                } catch (SQLException var10) {
                    DebugType.General.printException(var10, "", LogSeverity.Error);
                    return "A SQL error occured";
                }
            } else {
                return "Unknown command " + input;
            }
        }
    }

    public static void sendTeleport(IsoPlayer player, float x, float y, float z) {
        if (player != null) {
            UdpConnection playerConnection = getConnectionFromPlayer(player);
            INetworkPacket.send(playerConnection, PacketTypes.PacketType.Teleport, player, x, y, z);
            if (player.getNetworkCharacterAI() != null) {
                player.getNetworkCharacterAI().resetSpeedLimiter();
            }

            AntiCheatNoClip.teleport(player);
            player.getNetworkCharacterAI().resetState();

            for (IsoPlayer other : IDToPlayerMap.values()) {
                if (other.getOnlineID() != player.getOnlineID() && other.isAlive()) {
                    UdpConnection otherConnection = getConnectionFromPlayer(other);
                    if (otherConnection != null
                        && playerConnection != null
                        && otherConnection.isRelevantTo(x, y)
                        && !otherConnection.isRelevantTo(player.getX(), player.getY())) {
                        other.getNetworkCharacterAI().getState().sync(playerConnection);
                        INetworkPacket.send(otherConnection, PacketTypes.PacketType.PlayerInjuries, player);
                    }
                }
            }

            INetworkPacket.send(playerConnection, PacketTypes.PacketType.PlayerInjuries, player);
        }
    }

    public static void sendPlayerExtraInfo(IsoPlayer p, UdpConnection connection) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.ExtraInfo, p, false);
    }

    public static void sendPlayerExtraInfo(IsoPlayer p, UdpConnection connection, boolean isForced) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.ExtraInfo, p, isForced);
    }

    public static boolean canModifyPlayerStats(UdpConnection c, IsoPlayer player) {
        return c.getRole().hasCapability(Capability.CanModifyPlayerStatsInThePlayerStatsUI) || c.havePlayer(player);
    }

    static void receiveChangePlayerStats(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short id = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(id);
        if (player != null) {
            String adminUserName = bb.getUTF();
            player.setPlayerStats(bb, adminUserName);

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    if (c.getConnectedGUID() == PlayerToAddressMap.get(player)) {
                        c.setAllChatMuted(player.isAllChatMuted());
                        c.setRole(player.role);
                    }

                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.ChangePlayerStats.doPacket(b);
                    player.createPlayerStats(b, adminUserName);
                    PacketTypes.PacketType.ChangePlayerStats.send(c);
                }
            }
        }
    }

    public static void doMinimumInit() throws IOException {
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        DebugFileWatcher.instance.init();
        ArrayList<String> mods = new ArrayList<>(ServerMods);
        ZomboidFileSystem.instance.loadMods(mods);
        LuaManager.init();
        PerkFactory.init();
        CustomPerks.instance.init();
        CustomPerks.instance.initLua();
        if (guiCommandline && !softReset) {
            ServerGUI.init();
        }

        AssetManagers assetManagers = GameWindow.assetManagers;
        AiSceneAssetManager.instance.create(AiSceneAsset.ASSET_TYPE, assetManagers);
        AnimatedTextureIDAssetManager.instance.create(AnimatedTextureID.ASSET_TYPE, assetManagers);
        AnimationAssetManager.instance.create(AnimationAsset.ASSET_TYPE, assetManagers);
        AnimNodeAssetManager.instance.create(AnimationAsset.ASSET_TYPE, assetManagers);
        ClothingItemAssetManager.instance.create(ClothingItem.ASSET_TYPE, assetManagers);
        MeshAssetManager.instance.create(ModelMesh.ASSET_TYPE, assetManagers);
        ModelAssetManager.instance.create(Model.ASSET_TYPE, assetManagers);
        NinePatchTextureAssetManager.instance.create(NinePatchTexture.ASSET_TYPE, assetManagers);
        PhysicsShapeAssetManager.instance.create(PhysicsShape.ASSET_TYPE, assetManagers);
        TextureIDAssetManager.instance.create(TextureID.ASSET_TYPE, assetManagers);
        TextureAssetManager.instance.create(Texture.ASSET_TYPE, assetManagers);
        if (guiCommandline && !softReset) {
            TileDepthTextureManager.getInstance().init();
            TileDepthMapManager.instance.init();
            TileSeamManager.instance.init();
            GameWindow.initFonts();
        }

        CustomSandboxOptions.instance.init();
        CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
        ModRegistries.init();
        ScriptManager.instance.Load();
        CustomizationManager.getInstance().load();
        ClothingDecals.init();
        BeardStyles.init();
        HairStyles.init();
        OutfitManager.init();
        VoiceStyles.init();
        JAssImpImporter.Init();
        ModelManager.noOpenGL = !ServerGUI.isCreated();
        ModelManager.instance.create();
        System.out.println("LOADING ASSETS: START");
        CoopSlave.status("UI_ServerStatus_Loading_Assets");

        while (GameWindow.fileSystem.hasWork()) {
            GameWindow.fileSystem.updateAsyncTransactions();
        }

        System.out.println("LOADING ASSETS: FINISH");
        CoopSlave.status("UI_ServerStatus_Initing_Checksum");

        try {
            AdvancedAnimator.load();
            LuaManager.initChecksum();
            LuaManager.LoadDirBase("shared");
            LuaManager.LoadDirBase("client", true);
            LuaManager.LoadDirBase("server");
            LuaManager.finishChecksum();
        } catch (Exception var3) {
            DebugType.General.printException(var3, "", LogSeverity.Error);
        }

        ScriptManager.instance.LoadedAfterLua();
        CoopSlave.status("UI_ServerStatus_Loading_Sandbox_Vars");
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + "_SandboxVars.lua");
        if (file.exists()) {
            if (!SandboxOptions.instance.loadServerLuaFile(serverName)) {
                System.out.println("Exiting due to errors loading " + file.getCanonicalPath());
                System.exit(1);
            }

            SandboxOptions.instance.handleOldServerZombiesFile();
            SandboxOptions.instance.saveServerLuaFile(serverName);
            SandboxOptions.instance.toLua();
        } else {
            SandboxOptions.instance.handleOldServerZombiesFile();
            SandboxOptions.instance.saveServerLuaFile(serverName);
            SandboxOptions.instance.toLua();
        }

        WordsFilter.getInstance().loadWords(ServerOptions.instance.badWordListFile.getValue(), ServerOptions.instance.goodWordListFile.getValue());
        LuaEventManager.triggerEvent("OnGameBoot");
        ZomboidGlobals.Load();
        SpawnPoints.instance.initServer1();
        ServerGUI.init2();
        StatisticManager.getInstance().init();
    }

    public static void startServer() throws ConnectException {
        String serverPassword = ServerOptions.instance.password.getValue();
        if (CoopSlave.instance != null && SteamUtils.isSteamModeEnabled()) {
            serverPassword = "";
        }

        udpEngine = new UdpEngine(defaultPort, udpPort, 101, serverPassword, true);
        DebugLog.log(DebugType.Network, "*** SERVER STARTED ****");
        DebugLog.log(DebugType.Network, "*** Steam is " + (SteamUtils.isSteamModeEnabled() ? "enabled" : "not enabled"));
        if (SteamUtils.isSteamModeEnabled()) {
            DebugType.DetailedInfo
                .trace("Server is listening on port " + defaultPort + " (for Steam connection) and port " + udpPort + " (for UDPRakNet connection)");
            DebugType.DetailedInfo.trace("Clients should use " + defaultPort + " port for connections");
        } else {
            DebugType.DetailedInfo.trace("server is listening on port " + defaultPort);
        }

        resetId = ServerOptions.instance.resetId.getValue();
        if (CoopSlave.instance != null) {
            if (SteamUtils.isSteamModeEnabled()) {
                RakNetPeerInterface peer = udpEngine.getPeer();
                CoopSlave.instance.sendMessage("server-address", null, peer.GetServerIP() + ":" + defaultPort);
                long serverSteamID = SteamGameServer.GetSteamID();
                CoopSlave.instance.sendMessage("steam-id", null, SteamUtils.convertSteamIDToString(serverSteamID));
            } else {
                String serverAddress = "127.0.0.1";
                CoopSlave.instance.sendMessage("server-address", null, "127.0.0.1:" + defaultPort);
            }
        }

        LuaEventManager.triggerEvent("OnServerStarted");
        if (SteamUtils.isSteamModeEnabled()) {
            CoopSlave.status("UI_ServerStatus_Started");
        } else {
            CoopSlave.status("UI_ServerStatus_Started");
        }

        boolean discordEnable = ServerOptions.instance.discordEnable.getValue();
        String discordToken = ServerOptions.instance.discordToken.getValue();
        String discordChatChannel = ServerOptions.instance.discordChatChannel.getValue();
        String discordLogChannel = ServerOptions.instance.discordLogChannel.getValue();
        String discordCommandChannel = ServerOptions.instance.discordCommandChannel.getValue();
        discordBot.connect(discordEnable, discordToken, discordChatChannel, discordLogChannel, discordCommandChannel);
        EventManager.instance().registerCallback(discordBot);
        EventManager.instance().report("Server connected");
        String webhookAddress = ServerOptions.instance.webhookAddress.getValue();
        if (!webhookAddress.isEmpty()) {
            StackBot stackBot = new StackBot(webhookAddress);
            EventManager.instance().registerCallback(stackBot);
        }
    }

    private static void mainLoopDealWithNetData(ZomboidNetData d) {
        if (SystemDisabler.getDoMainLoopDealWithNetData()) {
            UdpConnection connection = udpEngine.getActiveConnection(d.connection);
            if (d.type == null) {
                ZomboidNetDataPool.instance.discard(d);
            } else {
                try {
                    if (connection == null) {
                        DebugLog.log(DebugType.Network, "Received packet type=" + d.type.name() + " connection is null.");
                        return;
                    }

                    if (connection.getUserName() == null) {
                        switch (d.type) {
                            case Login:
                            case Ping:
                            case ScoreboardUpdate:
                            case GoogleAuth:
                            case GoogleAuthKey:
                                break;
                            default:
                                DebugLog.log(
                                    "Received packet type="
                                        + d.type.name()
                                        + " before Login, disconnecting "
                                        + connection.getInetSocketAddress().getHostString()
                                );
                                connection.forceDisconnect("unacceptable-packet");
                                ZomboidNetDataPool.instance.discard(d);
                                return;
                        }
                    }

                    d.type.onServerPacket(d.buffer, connection);
                } catch (Exception var3) {
                    if (connection == null) {
                        DebugLog.log(DebugType.Network, "Error with packet of type: " + d.type + " connection is null.");
                    } else {
                        DebugType.General.error("Error with packet of type: " + d.type + " for " + connection.getConnectedGUID());
                        AntiCheat.PacketException.act(connection, d.type.name() + ": exception occurred");
                    }

                    DebugType.General.printException(var3, "", LogSeverity.Error);
                }

                ZomboidNetDataPool.instance.discard(d);
            }
        }
    }

    static void receiveInvMngRemoveItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int itemId = bb.getInt();
        short requested = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(requested);
        if (player != null) {
            InventoryItem item = player.getInventory().getItemWithID(itemId);
            if (item != null) {
                player.getInventory().Remove(item);
                sendRemoveItemFromContainer(player.getInventory(), item);
            }
        }
    }

    static void receiveInvMngGetItem(ByteBufferReader bb, UdpConnection connection, short packetType) throws IOException {
        short caller = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(caller);
        if (player != null) {
            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID() && c.getConnectedGUID() == PlayerToAddressMap.get(player)) {
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.InvMngGetItem.doPacket(b);
                    bb.rewind();
                    b.put(bb);
                    PacketTypes.PacketType.InvMngGetItem.send(c);
                    break;
                }
            }
        }
    }

    static void receiveInvMngReqItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int itemId = 0;
        String type = null;
        if (bb.getBoolean()) {
            type = bb.getUTF();
        } else {
            itemId = bb.getInt();
        }

        short caller = bb.getShort();
        short requested = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(requested);
        if (player != null) {
            IsoPlayer callerPlayer = IDToPlayerMap.get(caller);
            if (callerPlayer != null) {
                InventoryItem item;
                if (type == null) {
                    item = player.getInventory().getItemWithIDRecursiv(itemId);
                    if (item == null) {
                        return;
                    }
                } else {
                    item = InventoryItemFactory.CreateItem(type);
                }

                if (item != null) {
                    callerPlayer.getInventory().addItem(item);
                    INetworkPacket.send(callerPlayer, PacketTypes.PacketType.AddInventoryItemToContainer, callerPlayer.getInventory(), item);
                    if (item.getCategory().equals("Clothing")) {
                        player.removeWornItem(item);
                    }

                    if (item == player.getPrimaryHandItem()) {
                        player.setPrimaryHandItem(null);
                    } else if (item == player.getSecondaryHandItem()) {
                        player.setSecondaryHandItem(null);
                    }

                    if (type == null) {
                        player.getInventory().removeItemWithID(itemId);
                        INetworkPacket.send(player, PacketTypes.PacketType.RemoveInventoryItemFromContainer, player.getInventory(), item);
                    } else {
                        item = player.getInventory().getItemFromType(type.split("\\.")[1]);
                        player.getInventory().Remove(item);
                        INetworkPacket.sendToAll(PacketTypes.PacketType.SyncItemDelete, player.getInventory(), item);
                    }
                }
            }
        }
    }

    static void receiveInvMngUpdateItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short playerID = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(playerID);
        if (player != null) {
            InventoryItem itemNew;
            try {
                itemNew = InventoryItem.loadItem(bb.bb, 249);
            } catch (IOException var8) {
                return;
            }

            InventoryItem itemOld = player.getInventory().getItemWithIDRecursiv(itemNew.getID());
            if (itemOld != null) {
                ItemContainer container = itemOld.getContainer();
                container.Remove(itemOld);
                container.AddItem(itemNew);
                INetworkPacket.sendToAll(PacketTypes.PacketType.ReplaceInventoryItemInContainer, container, itemOld, itemNew);
            }
        }
    }

    static void receivePlayerStartPMChat(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ChatServer.getInstance().processPlayerStartWhisperChatPacket(bb);
    }

    static void receiveSandboxOptions(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            SandboxOptions.instance.load(bb.bb);
            SandboxOptions.instance.applySettings();
            SandboxOptions.instance.toLua();
            SandboxOptions.instance.saveServerLuaFile(serverName);

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SandboxOptions.doPacket(b);
                bb.rewind();
                b.put(bb);
                PacketTypes.PacketType.SandboxOptions.send(c);
            }
        } catch (Exception var6) {
            DebugType.General.printException(var6, "", LogSeverity.Error);
        }
    }

    static void receiveChangeTextColor(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short playerIndex = bb.getShort();
        IsoPlayer player = getPlayerFromConnection(connection, playerIndex);
        if (player != null) {
            float r = bb.getFloat();
            float g = bb.getFloat();
            float b = bb.getFloat();
            player.setSpeakColourInfo(new ColorInfo(r, g, b, 1.0F));

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.ChangeTextColor.doPacket(b2);
                    b2.putShort(player.getOnlineID());
                    b2.putFloat(r);
                    b2.putFloat(g);
                    b2.putFloat(b);
                    PacketTypes.PacketType.ChangeTextColor.send(c);
                }
            }
        }
    }

    static void receiveSyncCompost(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        String spriteName = bb.getUTF();
        if (sq != null) {
            IsoCompost compost = sq.getCompost();
            if (compost == null) {
                assert compost != null;

                compost = new IsoCompost(sq.getCell(), sq, spriteName);
                sq.AddSpecialObject(compost);
            }

            float compostValue = bb.getFloat();
            compost.setCompost(compostValue);
            sendCompost(compost, connection);
        }
    }

    public static void sendCompost(IsoCompost compost, UdpConnection connection) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isRelevantTo(compost.square.x, compost.square.y)
                && (connection != null && c.getConnectedGUID() != connection.getConnectedGUID() || connection == null)) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SyncCompost.doPacket(b);
                b.putInt(compost.square.x);
                b.putInt(compost.square.y);
                b.putInt(compost.square.z);
                b.putUTF(compost.getSpriteName());
                b.putFloat(compost.getCompost());
                PacketTypes.PacketType.SyncCompost.send(c);
            }
        }
    }

    public static void sendHelicopter(float x, float y, boolean active) {
        HelicopterPacket packet = new HelicopterPacket();
        packet.set(x, y, active);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            ByteBufferWriter bb = c.startPacket();
            PacketTypes.PacketType.Helicopter.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.Helicopter.send(c);
        }
    }

    public static void open() {
        closed = false;
        String message = "Server was opened for all";
        DebugType.General.println("Server was opened for all");
        ChatServer.getInstance().sendMessageToAdminChat("Server was opened for all");
        EventManager.instance().report("[SERVER] Server was opened for all");
    }

    public static void close() {
        closed = true;

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            c.forceDisconnect("server-closed");
        }

        String message = "Server was closed for all";
        DebugType.General.println("Server was closed for all");
        ChatServer.getInstance().sendMessageToAdminChat("Server was closed for all");
        EventManager.instance().report("[SERVER] Server was closed for all");
    }

    public static void sendZone(Zone zone) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.RegisterZone.doPacket(b);
            b.putUTF(zone.name);
            b.putUTF(zone.type);
            b.putInt(zone.x);
            b.putInt(zone.y);
            b.putInt(zone.z);
            b.putInt(zone.w);
            b.putInt(zone.h);
            b.putInt(zone.lastActionTimestamp);
            PacketTypes.PacketType.RegisterZone.send(c);
        }
    }

    static void receiveConstructedZone(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        Zone zone = IsoWorld.instance.metaGrid.getZoneAt(x, y, z);
        if (zone != null) {
            zone.setHaveConstruction(true);
        }
    }

    public static void addXp(IsoPlayer p, PerkFactory.Perk perk, float xp) {
        addXp(p, perk, xp, false, false);
    }

    public static void addXp(IsoPlayer p, PerkFactory.Perk perk, float xp, boolean noMultiplier) {
        addXp(p, perk, xp, noMultiplier, false);
    }

    public static void addXp(IsoPlayer player, PerkFactory.Perk perk, float xp, boolean noMultiplier, boolean showXp) {
        UdpConnection connection = getConnectionFromPlayer(player);
        if (connection != null) {
            if (canModifyPlayerStats(connection, player)) {
                if (player != null && !player.isDead()) {
                    player.getXp().AddXP(perk, xp, false, !noMultiplier, true, showXp);
                    player.getNetworkCharacterAI().updateXpChecker();
                }
            }
        }
    }

    public static void addXpMultiplier(IsoPlayer p, PerkFactory.Perk perk, float multiplier, int minLevel, int maxLevel) {
        p.getXp().addXpMultiplier(perk, multiplier, minLevel, maxLevel);
        INetworkPacket.send(p, PacketTypes.PacketType.AddXPMultiplier, p, perk, multiplier, minLevel, maxLevel);
    }

    private static void answerPing(ByteBufferReader bb, UdpConnection connection) {
        String ip = bb.getUTF();

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.Ping.doPacket(b);
                b.putUTF(ip);
                b.putInt(udpEngine.connections.size());
                b.putInt(512);
                PacketTypes.PacketType.Ping.send(c);
            }
        }
    }

    static void receiveUpdateItemSprite(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int bbbb = bb.getInt();
        String spriteName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        int index = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq != null && index < sq.getObjects().size()) {
            try {
                IsoObject o = sq.getObjects().get(index);
                if (o != null) {
                    o.sprite = IsoSpriteManager.instance.getSprite(bbbb);
                    if (o.sprite == null && !spriteName.isEmpty()) {
                        o.setSprite(spriteName);
                    }

                    o.RemoveAttachedAnims();
                    int count = bb.getByte() & 255;

                    for (int i = 0; i < count; i++) {
                        int id = bb.getInt();
                        IsoSprite spr = IsoSpriteManager.instance.getSprite(id);
                        if (spr != null) {
                            o.AttachExistingAnim(spr, 0, 0, false, 0, false, 0.0F);
                        }
                    }

                    o.transmitUpdatedSpriteToClients(connection);
                }
            } catch (Exception var15) {
            }
        }
    }

    public static void sendOptionsToClients() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            c.getValidator().resetCounters();
            c.getValidator().playerUpdateTimeoutReset();
            INetworkPacket.send(c, PacketTypes.PacketType.ReloadOptions);
        }
    }

    public static void sendCorpse(IsoDeadBody body) {
        IsoGridSquare sq = body.getSquare();
        if (sq != null) {
            AddCorpseToMapPacket packet = new AddCorpseToMapPacket();
            packet.set(sq, body);

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.isRelevantTo(sq.x, sq.y)) {
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.AddCorpseToMap.doPacket(b);
                    packet.write(b);
                    PacketTypes.PacketType.AddCorpseToMap.send(c);
                }
            }
        }
    }

    static void receiveChatMessageFromPlayer(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ChatServer.getInstance().processMessageFromPlayerPacket(bb, connection);
    }

    public static void loadModData(IsoGridSquare sq) {
        if (sq.getModData().rawget("id") != null
            && sq.getModData().rawget("id") != null
            && (sq.getModData().rawget("remove") == null || sq.getModData().rawget("remove").equals("false"))) {
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":x", (double)sq.getX());
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":y", (double)sq.getY());
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":z", (double)sq.getZ());
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":typeOfSeed", sq.getModData().rawget("typeOfSeed"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":nbOfGrow", sq.getModData().rawget("nbOfGrow"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":id", sq.getModData().rawget("id"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterLvl", sq.getModData().rawget("waterLvl"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":lastWaterHour", sq.getModData().rawget("lastWaterHour"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterNeeded", sq.getModData().rawget("waterNeeded"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterNeededMax", sq.getModData().rawget("waterNeededMax"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":mildewLvl", sq.getModData().rawget("mildewLvl"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":aphidLvl", sq.getModData().rawget("aphidLvl"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":fliesLvl", sq.getModData().rawget("fliesLvl"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":fertilizer", sq.getModData().rawget("fertilizer"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":nextGrowing", sq.getModData().rawget("nextGrowing"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":hasVegetable", sq.getModData().rawget("hasVegetable"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":hasSeed", sq.getModData().rawget("hasSeed"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":health", sq.getModData().rawget("health"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":badCare", sq.getModData().rawget("badCare"));
            GameTime.getInstance()
                .getModData()
                .rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":state", sq.getModData().rawget("state"));
            if (sq.getModData().rawget("hoursElapsed") != null) {
                GameTime.getInstance().getModData().rawset("hoursElapsed", sq.getModData().rawget("hoursElapsed"));
            }
        }

        ReceiveModDataPacket packet = new ReceiveModDataPacket();
        packet.set(sq);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isRelevantTo(sq.getX(), sq.getY())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.ReceiveModData.doPacket(b2);
                packet.write(b2);
                PacketTypes.PacketType.ReceiveModData.send(c);
            }
        }
    }

    static void receiveDrink(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int playerIndex = bb.getByte();
        float am = bb.getFloat();
        IsoPlayer pl = getPlayerFromConnection(connection, playerIndex);
        if (pl != null) {
            pl.getStats().remove(CharacterStat.THIRST, am);
        }
    }

    static void receiveReceiveCommand(ByteBufferReader bb, UdpConnection connection, short packetType) {
        String chat = bb.getUTF();
        String message = handleClientCommand(chat.substring(1), connection);
        if (message == null) {
            message = handleServerCommand(chat.substring(1), connection);
        }

        if (message == null) {
            message = "Unknown command " + chat;
        }

        if (!chat.substring(1).startsWith("roll") && !chat.substring(1).startsWith("card")) {
            ChatServer.getInstance().sendMessageToServerChat(connection, message);
        } else {
            ChatServer.getInstance().sendMessageToServerChat(message);
        }
    }

    private static String handleClientCommand(String input, UdpConnection connection) {
        if (input == null) {
            return null;
        } else {
            ArrayList<String> args1 = new ArrayList<>();
            Matcher m1 = Pattern.compile("([^\"]\\S*|\".*?\")\\s*").matcher(input);

            while (m1.find()) {
                args1.add(m1.group(1).replace("\"", ""));
            }

            int argc = args1.size();
            String[] argv = args1.toArray(new String[argc]);
            String command = argc > 0 ? argv[0].toLowerCase() : "";
            if (command.equals("card")) {
                PlayWorldSoundServer("ChatDrawCard", false, getAnyPlayerFromConnection(connection).getCurrentSquare(), 0.0F, 3.0F, 1.0F, false);
                return connection.getUserName() + " drew " + ServerOptions.getRandomCard();
            } else if (command.equals("roll")) {
                if (argc != 2) {
                    return ServerOptions.clientOptionsList.get("roll");
                } else {
                    try {
                        int number = Integer.parseInt(argv[1]);
                        PlayWorldSoundServer("ChatRollDice", false, getAnyPlayerFromConnection(connection).getCurrentSquare(), 0.0F, 3.0F, 1.0F, false);
                        return connection.getUserName() + " rolls a " + number + "-sided dice and obtains " + Rand.Next(number);
                    } catch (Exception var10) {
                        return ServerOptions.clientOptionsList.get("roll");
                    }
                }
            } else if (command.equals("changepwd")) {
                if (argc == 3) {
                    String previousPass = argv[1];
                    String newPass = argv[2];

                    try {
                        return ServerWorldDatabase.instance.changePwd(connection.getUserName(), previousPass.trim(), newPass.trim());
                    } catch (SQLException var11) {
                        DebugType.General.printException(var11, "A SQL error occured", LogSeverity.Error);
                        return "A SQL error occured";
                    }
                } else {
                    return ServerOptions.clientOptionsList.get("changepwd");
                }
            } else if (command.equals("dragons")) {
                return "Sorry, you don't have the required materials.";
            } else if (command.equals("dance")) {
                return "Stop kidding me...";
            } else if (command.equals("safehouse")) {
                if (argc != 2 || connection == null) {
                    return ServerOptions.clientOptionsList.get("safehouse");
                } else if (!ServerOptions.instance.playerSafehouse.getValue() && !ServerOptions.instance.adminSafehouse.getValue()) {
                    return "Safehouses are disabled on this server.";
                } else if ("release".equals(argv[1])) {
                    SafeHouse safeHouse = SafeHouse.hasSafehouse(connection.getUserName());
                    if (safeHouse == null) {
                        return "You don't have a safehouse.";
                    } else if (!safeHouse.isOwner(connection.getUserName())) {
                        return "Only owner can release safehouse";
                    } else if (!ServerOptions.instance.playerSafehouse.getValue() && !connection.getRole().hasCapability(Capability.CanSetupSafehouses)) {
                        return "Only admin or moderator may release safehouses";
                    } else {
                        SafeHouse.removeSafeHouse(safeHouse);
                        return "Safehouse released";
                    }
                } else {
                    return ServerOptions.clientOptionsList.get("safehouse");
                }
            } else {
                return null;
            }
        }
    }

    private static void PlayWorldSound(String name, IsoGridSquare source, float radius, int index) {
        if (server && source != null) {
            int x = source.getX();
            int y = source.getY();
            int z = source.getZ();
            PlayWorldSoundPacket packet = new PlayWorldSoundPacket();
            packet.set(name, x, y, (byte)z, index);
            DebugLog.log(DebugType.Sound, "sending " + packet.getDescription() + " radius=" + radius);

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                IsoPlayer p = getAnyPlayerFromConnection(c);
                if (p != null && c.RelevantTo(x, y, radius * 2.0F)) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.PlayWorldSound.doPacket(b2);
                    packet.write(b2);
                    PacketTypes.PacketType.PlayWorldSound.send(c);
                }
            }
        }
    }

    public static void PlayWorldSoundServer(String name, IsoGridSquare source, float radius, int index) {
        PlayWorldSound(name, source, radius, index);
    }

    public static void PlayWorldSoundServer(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        PlayWorldSound(name, source, radius, -1);
    }

    public static void PlayWorldSoundServer(
        IsoGameCharacter character, String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside
    ) {
        if (character == null || !character.isInvisible() || DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            PlayWorldSound(name, source, radius, -1);
        }
    }

    public static void PlayWorldSoundWavServer(
        String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside
    ) {
        PlayWorldSound(name, source, radius, -1);
    }

    public static void PlaySoundAtEveryPlayer(String name, int x, int y, int z) {
        PlaySoundAtEveryPlayer(name, x, y, z, false);
    }

    public static void PlaySoundAtEveryPlayer(String name) {
        PlaySoundAtEveryPlayer(name, -1, -1, -1, true);
    }

    public static void PlaySoundAtEveryPlayer(String name, int x, int y, int z, boolean usePlrCoords) {
        if (server) {
            if (usePlrCoords) {
                DebugLog.log(DebugType.Sound, "sound: sending " + name + " at every player (using player location)");
            } else {
                DebugLog.log(DebugType.Sound, "sound: sending " + name + " at every player location x=" + x + " y=" + y);
            }

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                IsoPlayer p = getAnyPlayerFromConnection(c);
                if (p != null && !p.hasTrait(CharacterTrait.DEAF)) {
                    if (usePlrCoords) {
                        x = PZMath.fastfloor(p.getX());
                        y = PZMath.fastfloor(p.getY());
                        z = PZMath.fastfloor(p.getZ());
                    }

                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.PlaySoundEveryPlayer.doPacket(b2);
                    b2.putUTF(name);
                    b2.putInt(x);
                    b2.putInt(y);
                    b2.putInt(z);
                    PacketTypes.PacketType.PlaySoundEveryPlayer.send(c);
                }
            }
        }
    }

    public static void sendCharacterSound(IsoGameCharacter chr, String soundName, byte flags) {
        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, chr.getX(), chr.getY(), soundName, flags, chr);
    }

    public static void sendCharacterSound(IsoGameCharacter chr, String soundName, byte flags, ParameterMeleeHitSurface.Material material) {
        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlaySound, chr.getX(), chr.getY(), soundName, flags, chr, material);
    }

    public static void sendZombieSound(IsoZombie.ZombieSound sound, IsoZombie zombie) {
        float radius = sound.radius();
        DebugLog.log(DebugType.Sound, "sound: sending zombie sound " + sound);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isFullyConnected() && c.RelevantTo(zombie.getX(), zombie.getY(), radius)) {
                ByteBufferWriter bb = c.startPacket();
                PacketTypes.PacketType.ZombieSound.doPacket(bb);
                bb.putShort(zombie.onlineId);
                bb.putEnum(sound);
                PacketTypes.PacketType.ZombieSound.send(c);
            }
        }
    }

    public static void initClientCommandFilter() {
        String filter = ServerOptions.getInstance().clientCommandFilter.getValue();
        ccFilters.clear();
        String[] ss = filter.split(";");

        for (String s : ss) {
            if (!s.isEmpty() && s.contains(".") && (s.startsWith("+") || s.startsWith("-"))) {
                String[] ss1 = s.split("\\.");
                if (ss1.length == 2) {
                    String module = ss1[0].substring(1);
                    String command = ss1[1];
                    GameServer.CCFilter ccf = new GameServer.CCFilter();
                    ccf.command = command;
                    ccf.allow = ss1[0].startsWith("+");
                    ccf.next = ccFilters.get(module);
                    ccFilters.put(module, ccf);
                }
            }
        }
    }

    static void receiveClientCommand(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int playerIndex = bb.getByte();
        String module = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();

            try {
                TableNetworkUtils.load(tbl, bb);
            } catch (Exception var10) {
                DebugType.General.printException(var10, "", LogSeverity.Error);
                return;
            }
        }

        IsoPlayer player = getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = getAnyPlayerFromConnection(connection);
        }

        if (player == null) {
            DebugLog.log("receiveClientCommand: player is null");
        } else {
            GameServer.CCFilter ccf = ccFilters.get(module);
            if (ccf == null || ccf.passes(command)) {
                LoggerManager.getLogger("cmd")
                    .write(
                        connection.getIDStr()
                            + " \""
                            + player.username
                            + "\" "
                            + module
                            + "."
                            + command
                            + " @ "
                            + player.getXi()
                            + ","
                            + player.getYi()
                            + ","
                            + player.getZi()
                    );
            }

            if (!"vehicle".equals(module)
                || !"remove".equals(command)
                || Core.debug
                || connection.getRole().hasCapability(Capability.GeneralCheats)
                || player.getNetworkCharacterAI().isDismantleAllowed()) {
                LuaEventManager.triggerEvent("OnClientCommand", module, command, player, tbl);
            }
        }
    }

    static void receiveWorldMap(ByteBufferReader bb, UdpConnection connection, short packetType) throws IOException {
        WorldMapServer.instance.receive(bb, connection);
    }

    public static IsoPlayer getAnyPlayerFromConnection(IConnection connection) {
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            if (connection.getPlayerAt(playerIndex) != null) {
                return connection.getPlayerAt(playerIndex);
            }
        }

        return null;
    }

    public static IsoPlayer getPlayerFromConnection(IConnection connection, int playerIndex) {
        return playerIndex >= 0 && playerIndex < 4 ? connection.getPlayerAt(playerIndex) : null;
    }

    public static IsoPlayer getPlayerByRealUserName(String username) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                IsoPlayer player = c.players[playerIndex];
                if (player != null && player.username.equals(username)) {
                    return player;
                }
            }
        }

        return null;
    }

    public static IsoPlayer getPlayerByUserName(String username) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                IsoPlayer player = c.players[playerIndex];
                if (player != null && (player.getDisplayName().equals(username) || player.getUsername().equals(username))) {
                    return player;
                }
            }
        }

        return null;
    }

    public static IsoPlayer getPlayerByUserNameForCommand(String username) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                IsoPlayer player = c.players[playerIndex];
                if (player != null && player.getUsername().equalsIgnoreCase(username)) {
                    return player;
                }
            }
        }

        return null;
    }

    public static UdpConnection getConnectionByPlayerOnlineID(short onlineID) {
        return udpEngine.getActiveConnection(IDToAddressMap.get(onlineID));
    }

    public static UdpConnection getConnectionFromPlayer(IsoPlayer player) {
        Long guid = PlayerToAddressMap.get(player);
        return guid == null ? null : udpEngine.getActiveConnection(guid);
    }

    public static UdpConnection getConnectionByIp(String ip) {
        if (ip != null && udpEngine != null) {
            for (int i = 0; i < udpEngine.connections.size(); i++) {
                UdpConnection c = udpEngine.connections.get(i);
                if (c != null && ip.equals(c.getIP())) {
                    return c;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static void sendAddItemToContainer(ItemContainer container, InventoryItem item) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, container, item);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.getParent().getX(), (int)container.getParent().getY(), container, item
            );
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.AddInventoryItemToContainer,
                (int)container.inventoryContainer.getWorldItem().getX(),
                (int)container.inventoryContainer.getWorldItem().getY(),
                container,
                item
            );
        }
    }

    public static void sendAddItemsToContainer(ItemContainer container, ArrayList<InventoryItem> items) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, container, items);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.getParent().getX(), (int)container.getParent().getY(), container, items
            );
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.AddInventoryItemToContainer,
                (int)container.inventoryContainer.getWorldItem().getX(),
                (int)container.inventoryContainer.getWorldItem().getY(),
                container,
                items
            );
        }
    }

    public static void sendReplaceItemInContainer(ItemContainer container, InventoryItem oldItem, InventoryItem newItem) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.ReplaceInventoryItemInContainer, container, oldItem, newItem);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.ReplaceInventoryItemInContainer,
                (int)container.getParent().getX(),
                (int)container.getParent().getY(),
                container,
                oldItem,
                newItem
            );
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.ReplaceInventoryItemInContainer,
                (int)container.inventoryContainer.getWorldItem().getX(),
                (int)container.inventoryContainer.getWorldItem().getY(),
                container,
                oldItem,
                newItem
            );
        }
    }

    public static void sendRemoveItemFromContainer(ItemContainer container, InventoryItem item) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, item);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.getParent().getX(), (int)container.getParent().getY(), container, item
            );
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.RemoveInventoryItemFromContainer,
                (int)container.inventoryContainer.getWorldItem().getX(),
                (int)container.inventoryContainer.getWorldItem().getY(),
                container,
                item
            );
        }
    }

    public static void sendRemoveItemsFromContainer(ItemContainer container, ArrayList<InventoryItem> items) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, items);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.getParent().getX(), (int)container.getParent().getY(), container, items
            );
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.RemoveInventoryItemFromContainer,
                (int)container.inventoryContainer.getWorldItem().getX(),
                (int)container.inventoryContainer.getWorldItem().getY(),
                container,
                items
            );
        }
    }

    public static void sendSyncPlayerFields(IsoPlayer player, byte syncParams) {
        if (player != null && player.onlineId != -1) {
            INetworkPacket.send(player, PacketTypes.PacketType.SyncPlayerFields, player, syncParams);
        }
    }

    public static void sendSyncClothing(IsoPlayer player, ItemBodyLocation location, InventoryItem item) {
        if (player != null && player.onlineId != -1) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, player.getX(), player.getY(), player);
        }
    }

    public static void syncVisuals(IsoPlayer player) {
        if (player != null && player.onlineId != -1) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, player.getX(), player.getY(), player);
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncVisuals, player.getX(), player.getY(), player);
        }
    }

    public static void syncHumanVisual(IsoPlayer player) {
        if (player != null && player.onlineId != -1) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.HumanVisual, player.getX(), player.getY(), player);
        }
    }

    public static void syncClothingFields(IsoPlayer player) {
        player.getWornItems().forEach(item -> {
            if (item != null && item.getItem() != null) {
                INetworkPacket.send(player, PacketTypes.PacketType.SyncItemFields, player, item.getItem());
            }
        });
    }

    public static void sendItemsInContainer(IsoObject o, ItemContainer container) {
        if (udpEngine != null) {
            if (container == null) {
                DebugLog.log("sendItemsInContainer: container is null");
            } else {
                if (o instanceof IsoWorldInventoryObject worldInvObj) {
                    if (!(worldInvObj.getItem() instanceof InventoryContainer invContainer)) {
                        DebugLog.log("sendItemsInContainer: IsoWorldInventoryObject item isn't a container");
                        return;
                    }

                    if (invContainer.getInventory() != container) {
                        DebugLog.log("sendItemsInContainer: wrong container for IsoWorldInventoryObject");
                        return;
                    }
                } else if (o instanceof BaseVehicle) {
                    if (container.vehiclePart == null || container.vehiclePart.getItemContainer() != container || container.vehiclePart.getVehicle() != o) {
                        DebugLog.log("sendItemsInContainer: wrong container for BaseVehicle");
                        return;
                    }
                } else if (o instanceof IsoDeadBody) {
                    if (container != o.getContainer()) {
                        DebugLog.log("sendItemsInContainer: wrong container for IsoDeadBody");
                        return;
                    }
                } else if (o.getContainerIndex(container) == -1) {
                    DebugLog.log("sendItemsInContainer: wrong container for IsoObject");
                    return;
                }

                if (o != null && !container.getItems().isEmpty()) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, o.square.x, o.square.y, container, container.getItems());
                }
            }
        }
    }

    public static void addConnection(UdpConnection con) {
        synchronized (MainLoopNetDataHighPriorityQ) {
            MainLoopNetDataHighPriorityQ.add(new GameServer.DelayedConnection(con, true));
        }
    }

    public static void addDisconnect(UdpConnection con) {
        synchronized (MainLoopNetDataHighPriorityQ) {
            MainLoopNetDataHighPriorityQ.add(new GameServer.DelayedConnection(con, false));
        }
    }

    public static void addDelayedDisconnect(UdpConnection con) {
        synchronized (MainLoopDelayedDisconnectQ) {
            MainLoopDelayedDisconnectQ.put(con.getUserName(), new GameServer.DelayedConnection(con, false));
        }
    }

    public static void doDelayedDisconnect(IsoPlayer player) {
        synchronized (MainLoopDelayedDisconnectQ) {
            GameServer.DelayedConnection data = MainLoopDelayedDisconnectQ.remove(player.username);
            if (data != null) {
                data.disconnect();
            }
        }
    }

    public static boolean isDelayedDisconnect(UdpConnection con) {
        return con != null && con.getUserName() != null ? MainLoopDelayedDisconnectQ.containsKey(con.getUserName()) : false;
    }

    public static boolean isDelayedDisconnect(IsoPlayer player) {
        return player != null && player.username != null ? MainLoopDelayedDisconnectQ.containsKey(player.username) : false;
    }

    public static void disconnectPlayer(IsoPlayer player, IConnection connection) {
        if (player != null) {
            SafetySystemManager.storeSafety(player);
            ChatServer.getInstance().disconnectPlayer(player.getOnlineID());
            if (player.getVehicle() != null) {
                VehiclesDB2.instance.updateVehicleAndTrailer(player.getVehicle());
                if (player.getVehicle().isDriver(player) && player.getVehicle().isNetPlayerId(player.getOnlineID())) {
                    player.getVehicle().setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
                    if (player.getVehicle().getController() != null) {
                        player.getVehicle().getController().clientForce = 0.0F;
                    }

                    player.getVehicle().jniLinearVelocity.set(0.0F, 0.0F, 0.0F);
                }

                int seat = player.getVehicle().getSeat(player);
                if (seat != -1) {
                    player.getVehicle().clearPassenger(seat);
                }
            }

            NetworkZombieManager.getInstance().clearTargetAuth(connection, player);
            player.removeFromWorld();
            player.removeFromSquare();
            PlayerToAddressMap.remove(player);
            IDToAddressMap.remove(player.onlineId);
            IDToPlayerMap.remove(player.onlineId);
            UserNameToPlayerMap.remove(player.username);
            Players.remove(player);
            SafeHouse.updateSafehousePlayersConnected();
            SafeHouse safeHouse = SafeHouse.hasSafehouse(player);
            if (safeHouse != null && safeHouse.isOwner(player)) {
                for (IsoPlayer member : IDToPlayerMap.values()) {
                    safeHouse.checkTrespass(member);
                }
            }

            connection.setUserName(player.playerIndex, null);
            connection.setPlayerAt(player.playerIndex, null);
            connection.setPlayerId(player.playerIndex, (short)-1);
            connection.setRelevantPos(player.playerIndex, null);
            connection.setConnectArea(player.playerIndex, null);
            INetworkPacket.sendToAll(PacketTypes.PacketType.PlayerTimeout, player);
            ServerLOS.instance.removePlayer(player);
            ZombiePopulationManager.instance.updateLoadedAreas();
            DebugType.DetailedInfo.trace("Disconnected player \"" + player.getDisplayName() + "\" " + connection.getConnectedGUID());
            LoggerManager.getLogger("user")
                .write(connection.getIDStr() + " \"" + player.getUsername() + "\" disconnected player " + LoggerManager.getPlayerCoords(player));
            SteamGameServer.RemovePlayer(player);
        }
    }

    public static short getFreeSlot() {
        for (short n = 0; n < udpEngine.getMaxConnections(); n++) {
            if (SlotToConnection[n] == null) {
                return n;
            }
        }

        return -1;
    }

    public static void receiveClientConnect(UdpConnection connection, ServerWorldDatabase.LogonResult r) {
        ConnectionManager.log("receive-packet", "client-connect", connection);
        int slot = getFreeSlot();
        short playerID = (short)(slot * 4);
        if (connection.getPlayerDownloadServer() != null) {
            try {
                IDToAddressMap.put(playerID, connection.getConnectedGUID());
                connection.getPlayerDownloadServer().destroy();
            } catch (Exception var9) {
                DebugType.General.printException(var9, "", LogSeverity.Error);
            }
        }

        playerToCoordsMap.put(playerID, new Vector2());
        SlotToConnection[slot] = connection;
        connection.playerIds[0] = playerID;
        IDToAddressMap.put(playerID, connection.getConnectedGUID());
        connection.setPlayerDownloadServer(new PlayerDownloadServer(connection));
        DebugLog.log(DebugType.Network, "Connected new client " + connection.getConnectedGUID() + " ID # " + playerID);
        KahluaTable spawnRegions = SpawnPoints.instance.getSpawnRegions();

        for (int i = 1; i < spawnRegions.size() + 1; i++) {
            ByteBufferWriter b2 = connection.startPacket();
            PacketTypes.PacketType.SpawnRegion.doPacket(b2);
            b2.putInt(i);

            try {
                ((KahluaTable)spawnRegions.rawget(i)).save(b2.bb);
                PacketTypes.PacketType.SpawnRegion.send(connection);
            } catch (IOException var8) {
                DebugType.General.printException(var8, "", LogSeverity.Error);
            }
        }

        RequestDataPacket packet = new RequestDataPacket();
        packet.sendConnectingDetails(connection, r);
    }

    public static void sendMetaGrid(int cellX, int cellY, int roomID, UdpConnection connection) {
        MetaGridPacket packet = new MetaGridPacket();
        if (packet.set(cellX, cellY, roomID)) {
            ByteBufferWriter bb = connection.startPacket();
            PacketTypes.PacketType.MetaGrid.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.MetaGrid.send(connection);
        }
    }

    public static void sendMetaGrid(int cellX, int cellY, int roomID) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendMetaGrid(cellX, cellY, roomID, c);
        }
    }

    private static void preventIndoorZombies(int x, int y, int z) {
        RoomDef room = IsoWorld.instance.metaGrid.getRoomAt(x, y, z);
        if (room != null) {
            boolean killThemAll = isSpawnBuilding(room.getBuilding());
            room.getBuilding().setAllExplored(true);
            room.getBuilding().setAlarmed(false);
            ArrayList<IsoZombie> zombies = IsoWorld.instance.currentCell.getZombieList();

            for (int i = 0; i < zombies.size(); i++) {
                IsoZombie zombie = zombies.get(i);
                if ((killThemAll || zombie.indoorZombie)
                    && zombie.getSquare() != null
                    && zombie.getSquare().getRoom() != null
                    && zombie.getSquare().getRoom().def.building == room.getBuilding()) {
                    VirtualZombieManager.instance.removeZombieFromWorld(zombie);
                    if (i >= zombies.size() || zombies.get(i) != zombie) {
                        i--;
                    }
                }
            }
        }
    }

    public static void setCustomVariables(IsoPlayer p, IConnection c) {
        for (String key : VariableSyncPacket.syncedVariables) {
            if (p.getVariable(key) != null) {
                INetworkPacket.send(c, PacketTypes.PacketType.VariableSync, p, key, p.getVariableString(key));
            }
        }
    }

    public static void sendPlayerConnected(IsoPlayer p, IConnection c) {
        if (p != null) {
            boolean reply = PlayerToAddressMap.get(p) != null && c.getConnectedGUID() == PlayerToAddressMap.get(p) && !isDelayedDisconnect(p);
            INetworkPacket.send(c, PacketTypes.PacketType.ConnectedPlayer, p, reply);
            setCustomVariables(p, c);
            if (!reply) {
                INetworkPacket.send(c, PacketTypes.PacketType.Equip, p);
                syncActivatedItems(p, c);
            }
        }
    }

    private static void syncActivatedItems(IsoPlayer p, IConnection c) {
        syncActivatedItem(p, p.getPrimaryHandItem(), c);
        syncActivatedItem(p, p.getSecondaryHandItem(), c);
        p.getAttachedItems().forEach(item -> syncActivatedItem(p, item.getItem(), c));
    }

    private static void syncActivatedItem(IsoPlayer p, InventoryItem item, IConnection c) {
        if (item != null && item.isActivated()) {
            INetworkPacket.send(c, PacketTypes.PacketType.SyncItemActivated, p, item);
        }
    }

    public static void receivePlayerConnect(ByteBufferReader bb, IConnection connection, String username) {
        ConnectionManager.log("receive-packet", "player-connect", connection);
        int playerIndex = bb.getByte();
        DebugType.DetailedInfo.trace("User: \"%s\" index=%d ip=%s is trying to connect", username, playerIndex, connection.getIP());
        if (playerIndex >= 0 && playerIndex < 4 && connection.getPlayerAt(playerIndex) == null) {
            byte range = (byte)Math.max(12, Math.min(20, bb.getByte()));
            connection.setRelevantRange((byte)(range / 2 + 2));
            IsoPlayer player;
            if (coop && SteamUtils.isSteamModeEnabled()) {
                player = ServerPlayerDB.getInstance().serverLoadNetworkCharacter(playerIndex, connection.getIDStr());
            } else {
                player = ServerPlayerDB.getInstance().serverLoadNetworkCharacter(playerIndex, connection.getUserName());
            }

            if (player == null) {
                kick(connection, "UI_LoadPlayerProfileError", null);
                connection.forceDisconnect("UI_LoadPlayerProfileError");
            } else {
                connection.getRelevantPos(playerIndex).x = player.getX();
                connection.getRelevantPos(playerIndex).y = player.getY();
                connection.getRelevantPos(playerIndex).z = player.getZ();
                connection.setConnectArea(playerIndex, null);
                connection.setChunkGridWidth(range);
                connection.setLoadedCells(
                    playerIndex, new ClientServerMap(playerIndex, PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), range)
                );
                player.realx = player.getX();
                player.realy = player.getY();
                player.realz = (byte)player.getZi();
                player.playerIndex = playerIndex;
                player.onlineChunkGridWidth = range;
                Players.add(player);
                player.remote = true;
                connection.setPlayerAt(playerIndex, player);
                short o = connection.getPlayerId(playerIndex);
                IDToPlayerMap.put(o, player);
                PlayerToAddressMap.put(player, connection.getConnectedGUID());
                UserNameToPlayerMap.put(username, o);
                player.setOnlineID(o);
                byte extraInfoFlags = bb.getByte();
                player.setRole(connection.getRole());
                player.setExtraInfoFlags(extraInfoFlags, true);
                if (SteamUtils.isSteamModeEnabled()) {
                    player.setSteamID(connection.getSteamId());
                    SteamGameServer.BUpdateUserData(connection.getSteamId(), connection.getUserName(), 0);
                }

                player.username = username;
                ChatServer.getInstance().initPlayer(player.onlineId);
                connection.setFullyConnected();
                sendWeather(connection);
                SafetySystemManager.restoreSafety(player);
                if (!connection.getRole().hasCapability(Capability.HideFromSteamUserList)) {
                    SteamGameServer.AddPlayer(player);
                }

                for (int n = 0; n < udpEngine.connections.size(); n++) {
                    UdpConnection c = udpEngine.connections.get(n);
                    sendPlayerConnected(player, c);
                    sendPlayerExtraInfo(player, c, true);
                }

                for (IsoPlayer isoPlayer : IDToPlayerMap.values()) {
                    if (isoPlayer.getOnlineID() != player.getOnlineID() && isoPlayer.isAlive()) {
                        sendPlayerConnected(isoPlayer, connection);
                        setCustomVariables(isoPlayer, connection);
                        isoPlayer.getNetworkCharacterAI().getState().sync(connection);
                        INetworkPacket.send(connection, PacketTypes.PacketType.PlayerInjuries, isoPlayer);
                    }
                }

                connection.getLoadedCell(playerIndex).setLoaded();
                connection.getLoadedCell(playerIndex).sendPacket(connection);
                preventIndoorZombies(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), PZMath.fastfloor(player.getZ()));
                ServerLOS.instance.addPlayer(player);
                WarManager.sendWarToPlayer(player);
                Set<String> hiddenAuthorsSet = HiddenAuthors.getSetForUser(username);
                if (hiddenAuthorsSet != null) {
                    if (hiddenAuthorsSet.size() <= 127) {
                        INetworkPacket.send(connection, PacketTypes.PacketType.HiddenAuthors, true, hiddenAuthorsSet);
                    } else {
                        Set<String> chunk = new HashSet<>();

                        for (String s : hiddenAuthorsSet) {
                            chunk.add(s);
                            if (chunk.size() == 127) {
                                INetworkPacket.send(connection, PacketTypes.PacketType.HiddenAuthors, true, chunk);
                                chunk.clear();
                            }
                        }

                        if (!chunk.isEmpty()) {
                            INetworkPacket.send(connection, PacketTypes.PacketType.HiddenAuthors, true, chunk);
                        }
                    }
                }

                LoggerManager.getLogger("user")
                    .write(connection.getIDStr() + " \"" + player.username + "\" fully connected " + LoggerManager.getPlayerCoords(player));
            }
        }
    }

    public static void sendInitialWorldState(IConnection c) {
        if (RainManager.isRaining()) {
            sendStartRain(c);
        }

        INetworkPacket.send(c, PacketTypes.PacketType.VehicleTowingState, VehicleManager.instance.towedVehicleMap);

        try {
            if (!ClimateManager.getInstance().isUpdated()) {
                ClimateManager.getInstance().update();
            }

            ClimateManager.getInstance().sendInitialState(c);
            INetworkPacket.send(c, PacketTypes.PacketType.SyncPuddles, IsoPuddles.getInstance());
        } catch (Exception var2) {
            DebugType.General.printException(var2, "", LogSeverity.Error);
        }
    }

    public static void sendObjectModData(IsoObject o) {
        if (!softReset && !fastForward) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectModData, o.getX(), o.getY(), o);
        }
    }

    public static void sendSlowFactor(IsoGameCharacter chr) {
        if (chr instanceof IsoPlayer isoPlayer) {
            INetworkPacket.send(isoPlayer, PacketTypes.PacketType.SlowFactor, chr);
        }
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, KahluaTable tbl) {
        if (!softReset) {
            if (o != null && o.getSquare() != null) {
                INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectChange, (int)o.getX(), (int)o.getY(), o, change, tbl);
            }
        }
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, Object... objects) {
        if (!softReset) {
            if (objects.length == 0) {
                sendObjectChange(o, change, (KahluaTable)null);
            } else if (objects.length % 2 == 0) {
                KahluaTable t = LuaManager.platform.newTable();

                for (int i = 0; i < objects.length; i += 2) {
                    Object v = objects[i + 1];
                    if (v instanceof Float f) {
                        t.rawset(objects[i], f.doubleValue());
                    } else if (v instanceof Integer integer) {
                        t.rawset(objects[i], integer.doubleValue());
                    } else if (v instanceof Short s) {
                        t.rawset(objects[i], s.doubleValue());
                    } else {
                        t.rawset(objects[i], v);
                    }
                }

                sendObjectChange(o, change, t);
            }
        }
    }

    static void receiveSyncIsoObject(ByteBufferReader bb, UdpConnection connection, short packetType) {
        if (DebugOptions.instance.network.server.syncIsoObject.getValue()) {
            int x = bb.getInt();
            int y = bb.getInt();
            int z = bb.getInt();
            byte index = bb.getByte();
            byte exist = bb.getByte();
            byte state = bb.getByte();
            if (exist == 1) {
                IsoGridSquare sq = ServerMap.instance.getGridSquare(x, y, z);
                if (sq != null && index >= 0 && index < sq.getObjects().size()) {
                    sq.getObjects().get(index).syncIsoObject(true, state, connection, bb);
                } else if (sq != null) {
                    DebugLog.log("SyncIsoObject: index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
                } else {
                    DebugLog.log("SyncIsoObject: sq is null x,y,z=" + x + "," + y + "," + z);
                }
            }
        }
    }

    public static int RemoveItemFromMap(IsoObject obj) {
        int x = obj.getSquare().getX();
        int y = obj.getSquare().getY();
        int z = obj.getSquare().getZ();
        int index = obj.getObjectIndex();
        INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveItemFromSquare, null, x, y, obj);
        RemoveItemFromSquarePacket.removeItemFromMap(null, x, y, z, index);
        return index;
    }

    public static void sendBloodSplatter(HandWeapon weapon, float x, float y, float z, Vector2 hitDir, boolean closeKilled, boolean radial) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.BloodSplatter.doPacket(b);
            b.putUTF(weapon != null ? weapon.getType() : "");
            b.putFloat(x);
            b.putFloat(y);
            b.putFloat(z);
            b.putFloat(hitDir.getX());
            b.putFloat(hitDir.getY());
            b.putBoolean(closeKilled);
            b.putBoolean(radial);
            b.putByte(weapon == null ? 0 : Math.max(weapon.getSplatNumber(), 1));
            PacketTypes.PacketType.BloodSplatter.send(c);
        }
    }

    public static void connect(UdpConnection connection) {
        Manager.getInstance().connect(connection);
    }

    public static void disconnect(UdpConnection connection, String description) {
        if (connection.getPlayerDownloadServer() != null) {
            try {
                connection.getPlayerDownloadServer().destroy();
            } catch (Exception var4) {
                DebugType.General.printException(var4, "", LogSeverity.Error);
            }

            connection.setPlayerDownloadServer(null);
        }

        RequestDataManager.getInstance().disconnect(connection);
        Manager.getInstance().disconnect(connection);

        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            IsoPlayer player = connection.players[playerIndex];
            if (player != null) {
                TransactionManager.cancelAllRelevantToUser(player);
                ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(player, playerIndex, connection);
                ChatServer.getInstance().disconnectPlayer(connection.playerIds[playerIndex]);
                disconnectPlayer(player, connection);
            }

            connection.usernames[playerIndex] = null;
            connection.players[playerIndex] = null;
            connection.playerIds[playerIndex] = -1;
            connection.releventPos[playerIndex] = null;
            connection.connectArea[playerIndex] = null;
        }

        for (int i = 0; i < udpEngine.getMaxConnections(); i++) {
            if (SlotToConnection[i] == connection) {
                SlotToConnection[i] = null;
            }
        }

        Iterator<Entry<Short, Long>> iter = IDToAddressMap.entrySet().iterator();

        while (iter.hasNext()) {
            Entry<Short, Long> entry = iter.next();
            if (entry.getValue() == connection.getConnectedGUID()) {
                iter.remove();
            }
        }

        if (!SteamUtils.isSteamModeEnabled()) {
            PublicServerUtil.updatePlayers();
        }

        if (CoopSlave.instance != null && connection.isCoopHost) {
            DebugLog.log("Host user disconnected, stopping the server");
            ServerMap.instance.QueueQuit();
        }

        if (server) {
            ConnectionManager.log("disconnect", description, connection);
            EventManager.instance().report("[" + connection.getUserName() + "] disconnected from server");
            WorldMapVisitedServer.getInstance().unloadUser(connection.getUserName());
        }
    }

    public static void addIncoming(short id, ByteBufferReader bb, UdpConnection connection) {
        ZomboidNetData d;
        if (bb.limit() > 2048) {
            d = ZomboidNetDataPool.instance.getLong(bb.limit());
        } else {
            d = ZomboidNetDataPool.instance.get();
        }

        d.read(id, bb, connection);
        if (d.type == null) {
            try {
                AntiCheat.PacketType.act(connection, id + ": unknown Zed packet");
            } catch (Exception var5) {
                DebugType.General.printException(var5, "", LogSeverity.Error);
            }
        } else {
            d.time = System.currentTimeMillis();
            if (d.type == PacketTypes.PacketType.PlayerUpdateUnreliable || d.type == PacketTypes.PacketType.PlayerUpdateReliable) {
                MainLoopPlayerUpdateQ.add(d);
            } else if (d.type != PacketTypes.PacketType.VehiclePhysicsReliable && d.type != PacketTypes.PacketType.VehiclePhysicsUnreliable) {
                MainLoopNetDataHighPriorityQ.add(d);
            } else {
                MainLoopNetDataQ.add(d);
            }
        }
    }

    public static void smashWindow(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setSmashWindow(isoWindow);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isRelevantTo(isoWindow.getX(), isoWindow.getY())) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SmashWindow.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.SmashWindow.send(c);
            }
        }
    }

    public static void removeBrokenGlass(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setRemoveBrokenGlass(isoWindow);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isRelevantTo(isoWindow.getX(), isoWindow.getY())) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SmashWindow.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.SmashWindow.send(c);
            }
        }
    }

    public static void sendHitCharacter(HitCharacter packet, PacketTypes.PacketType packetType, UdpConnection connection) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && packet.isRelevant(c)) {
                ByteBufferWriter bbw = c.startPacket();
                packetType.doPacket(bbw);
                packet.write(bbw);
                packetType.send(c);
            }
        }
    }

    public static void sendCharacterDeath(IsoDeadBody body) {
        if (body.isZombie()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ZombieDeath, body.getX(), body.getY(), body);
        } else if (body.isAnimal()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalDeath, body.getX(), body.getY(), body);
        } else if (body.isPlayer()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.PlayerDeath, body.getX(), body.getY(), body);
        }
    }

    public static void sendItemStats(InventoryItem item) {
        if (item.getContainer() != null && item.getContainer().getParent() instanceof IsoPlayer) {
            INetworkPacket.send(
                getConnectionFromPlayer((IsoPlayer)item.getContainer().getParent()), PacketTypes.PacketType.ItemStats, item.getContainer(), item
            );
        } else if (item.getWorldItem() != null) {
            ItemContainer container = new ItemContainer("floor", item.getWorldItem().square, null);
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.ItemStats, item.getWorldItem().getSquare().x, item.getWorldItem().getSquare().y, container, item
            );
        } else {
            if (item.getOutermostContainer() != null) {
                if (item.getOutermostContainer().getSourceGrid() != null) {
                    INetworkPacket.sendToRelative(
                        PacketTypes.PacketType.ItemStats,
                        item.getOutermostContainer().getSourceGrid().x,
                        item.getOutermostContainer().getSourceGrid().y,
                        item.getContainer(),
                        item
                    );
                } else if (item.getOutermostContainer().getParent() != null) {
                    INetworkPacket.sendToRelative(
                        PacketTypes.PacketType.ItemStats,
                        item.getOutermostContainer().getParent().getX(),
                        item.getOutermostContainer().getParent().getY(),
                        item.getContainer(),
                        item
                    );
                } else {
                    INetworkPacket.sendToAll(PacketTypes.PacketType.ItemStats, item.getOutermostContainer(), item);
                }
            }
        }
    }

    public static void sendSyncItemFields(InventoryItem item) {
        if (item != null) {
            ItemContainer outermost = item.getOutermostContainer();
            if (outermost != null) {
                int x;
                int y;
                if (outermost.getSourceGrid() != null) {
                    x = outermost.getSourceGrid().x;
                    y = outermost.getSourceGrid().y;
                } else {
                    if (outermost.getParent() == null) {
                        return;
                    }

                    x = (int)outermost.getParent().getX();
                    y = (int)outermost.getParent().getY();
                }

                INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncItemFields, x, y, null, item);
            }
        }
    }

    public static void receiveEatBody(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, "ReceiveEatBody");
            }

            short zombieID = bb.getShort();
            IsoZombie zombie = ServerMap.instance.zombieMap.get(zombieID);
            if (zombie == null) {
                DebugType.Multiplayer.error("ReceiveEatBody: zombie " + zombieID + " not found");
                return;
            }

            for (UdpConnection c : udpEngine.connections) {
                if (c.isRelevantTo(zombie.getX(), zombie.getY())) {
                    if (Core.debug) {
                        DebugLog.log(DebugType.Multiplayer, "SendEatBody");
                    }

                    ByteBufferWriter bbw = c.startPacket();
                    PacketTypes.PacketType.EatBody.doPacket(bbw);
                    bb.position(0);
                    bbw.put(bb);
                    PacketTypes.PacketType.EatBody.send(c);
                }
            }
        } catch (Exception var8) {
            DebugType.Multiplayer.printException(var8, "ReceiveEatBody: failed", LogSeverity.Error);
        }
    }

    public static void receiveSyncRadioData(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            boolean isCanHearAll = bb.getBoolean();
            int radioDataSize = bb.getInt();
            int[] radioData = new int[radioDataSize];

            for (int i = 0; i < radioDataSize; i++) {
                radioData[i] = bb.getInt();
            }

            RakVoice.SetChannelsRouting(connection.getConnectedGUID(), isCanHearAll, radioData, (short)radioDataSize);

            for (UdpConnection c : udpEngine.connections) {
                if (c != connection && connection.players[0] != null) {
                    ByteBufferWriter bbw = c.startPacket();
                    PacketTypes.PacketType.SyncRadioData.doPacket(bbw);
                    bbw.putShort(connection.players[0].onlineId);
                    bb.position(0);
                    bbw.put(bb);
                    PacketTypes.PacketType.SyncRadioData.send(c);
                }
            }
        } catch (Exception var9) {
            DebugType.Multiplayer.printException(var9, "SyncRadioData: failed", LogSeverity.Error);
        }
    }

    public static void sendWorldSound(WorldSoundManager.WorldSound sound, UdpConnection connection) {
        WorldSoundPacket packet = new WorldSoundPacket();
        packet.setData(sound);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isFullyConnected() && c.RelevantTo(sound.x, sound.y, sound.radius)) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.WorldSoundPacket.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.WorldSoundPacket.send(c);
            }
        }
    }

    public static void kick(IConnection connection, String description, String reason) {
        ConnectionManager.log("kick", reason, connection);
        INetworkPacket.send(connection, PacketTypes.PacketType.Kicked, description, reason);
    }

    private static void sendStartRain(IConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.StartRain.doPacket(b);
        b.putInt(RainManager.randRainMin);
        b.putInt(RainManager.randRainMax);
        b.putFloat(RainManager.rainDesiredIntensity);
        PacketTypes.PacketType.StartRain.send(c);
    }

    public static void startRain() {
        if (udpEngine != null) {
            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                sendStartRain(c);
            }
        }
    }

    private static void sendStopRain(UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.StopRain.doPacket(b);
        PacketTypes.PacketType.StopRain.send(c);
    }

    public static void stopRain() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendStopRain(c);
        }
    }

    private static void sendWeather(IConnection c) {
        WeatherPacket packet = new WeatherPacket();
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.Weather.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.Weather.send(c);
    }

    public static void sendWeather() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendWeather(c);
        }
    }

    private static boolean isInSameFaction(IsoPlayer player1, IsoPlayer player2) {
        Faction factionLocal = Faction.getPlayerFaction(player1);
        Faction factionRemote = Faction.getPlayerFaction(player2);
        return factionLocal != null && factionLocal == factionRemote;
    }

    private static boolean isAnyPlayerInSameFaction(UdpConnection c1, IsoPlayer player2) {
        for (int i = 0; i < 4; i++) {
            IsoPlayer player1 = c1.players[i];
            if (player1 != null && isInSameFaction(player1, player2)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAnyPlayerInSameSafehouse(UdpConnection c1, IsoPlayer player2) {
        for (int i = 0; i < 4; i++) {
            IsoPlayer player1 = c1.players[i];
            if (player1 != null && SafeHouse.isInSameSafehouse(player1.getUsername(), player2.getUsername())) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldSendWorldMapPlayerPosition(UdpConnection c, IsoPlayer player) {
        if (player != null && !player.isDead()) {
            UdpConnection c2 = getConnectionFromPlayer(player);
            if (c2 != null && c2 != c && c2.isFullyConnected()) {
                if (c.getRole().hasCapability(Capability.SeeWorldMap)) {
                    return true;
                } else {
                    GameServer.MapRemotePlayerVisibility mapRemotePlayerVisibility = GameServer.MapRemotePlayerVisibility.values()[ServerOptions.getInstance()
                            .mapRemotePlayerVisibility
                            .getValue()
                        - 1];
                    if (mapRemotePlayerVisibility == GameServer.MapRemotePlayerVisibility.ALL) {
                        return true;
                    } else {
                        if (mapRemotePlayerVisibility == GameServer.MapRemotePlayerVisibility.FACTION_AND_VISIBLE_ONLY) {
                            for (IsoPlayer connectedPlayer : c.players) {
                                if (connectedPlayer.checkCanSeeClient(player)) {
                                    return true;
                                }
                            }
                        }

                        return (
                                mapRemotePlayerVisibility == GameServer.MapRemotePlayerVisibility.FACTION_AND_VISIBLE_ONLY
                                    || mapRemotePlayerVisibility == GameServer.MapRemotePlayerVisibility.FACTION_ONLY
                            )
                            && (isAnyPlayerInSameFaction(c, player) || isAnyPlayerInSameSafehouse(c, player));
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void sendWorldMapPlayerPosition(UdpConnection c) {
        tempPlayers.clear();

        for (int i = 0; i < Players.size(); i++) {
            IsoPlayer player = Players.get(i);
            if (shouldSendWorldMapPlayerPosition(c, player)) {
                tempPlayers.add(player);
            }
        }

        if (!tempPlayers.isEmpty()) {
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(b);
            b.putBoolean(false);
            b.putShort(tempPlayers.size());

            for (int ix = 0; ix < tempPlayers.size(); ix++) {
                IsoPlayer player = tempPlayers.get(ix);
                WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getOrCreatePlayer(player);
                remotePlayer.setPlayer(player);
                b.putShort(remotePlayer.getOnlineID());
                b.putShort(remotePlayer.getChangeCount());
                b.putFloat(remotePlayer.getX());
                b.putFloat(remotePlayer.getY());
            }

            PacketTypes.PacketType.WorldMapPlayerPosition.send(c);
        }
    }

    public static void sendWorldMapPlayerPosition() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendWorldMapPlayerPosition(c);
        }
    }

    public static void receiveWorldMapPlayerPosition(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short count = bb.getShort();
        tempPlayers.clear();

        for (int i = 0; i < count; i++) {
            short playerID = bb.getShort();
            IsoPlayer player = IDToPlayerMap.get(playerID);
            if (player != null && shouldSendWorldMapPlayerPosition(connection, player)) {
                tempPlayers.add(player);
            }
        }

        if (!tempPlayers.isEmpty()) {
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(b);
            b.putBoolean(true);
            b.putShort(tempPlayers.size());

            for (int ix = 0; ix < tempPlayers.size(); ix++) {
                IsoPlayer player = tempPlayers.get(ix);
                WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getOrCreatePlayer(player);
                remotePlayer.setPlayer(player);
                b.putShort(remotePlayer.getOnlineID());
                b.putShort(remotePlayer.getChangeCount());
                b.putUTF(remotePlayer.getUsername());
                b.putUTF(remotePlayer.getForename());
                b.putUTF(remotePlayer.getSurname());
                b.putUTF(remotePlayer.getAccessLevel());
                b.putInt(remotePlayer.getRolePower());
                b.putFloat(remotePlayer.getX());
                b.putFloat(remotePlayer.getY());
                b.putBoolean(remotePlayer.isInvisible());
                b.putBoolean(remotePlayer.isDisguised());
                b.putBoolean(remotePlayer.canSeeInvisiblePlayer());
            }

            PacketTypes.PacketType.WorldMapPlayerPosition.send(connection);
        }
    }

    private static void syncClock(UdpConnection c) {
        INetworkPacket.send(c, PacketTypes.PacketType.SyncClock);
    }

    public static void syncClock() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            syncClock(c);
        }
    }

    public static void sendServerCommand(String module, String command, KahluaTable args, UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putUTF(module);
        b.putUTF(command);
        if (b.putBoolean(args != null && !args.isEmpty())) {
            try {
                KahluaTableIterator it = args.iterator();

                while (it.advance()) {
                    if (!TableNetworkUtils.canSave(it.getKey(), it.getValue())) {
                        DebugLog.log("ERROR: sendServerCommand: can't save key,value=" + it.getKey() + "," + it.getValue());
                    }
                }

                TableNetworkUtils.save(args, b);
            } catch (IOException var6) {
                DebugType.General.printException(var6, "", LogSeverity.Error);
            }
        }

        PacketTypes.PacketType.ClientCommand.send(c);
    }

    public static void sendServerCommand(String module, String command, KahluaTable args) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendServerCommand(module, command, args, c);
        }
    }

    public static void sendServerCommandToRelevant(float x, float y, String module, String command, KahluaTable args) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.isFullyConnected() && c.isRelevantTo(x, y)) {
                sendServerCommand(module, command, args, c);
            }
        }
    }

    public static void sendServerCommandV(String module, String command, Object... objects) {
        if (objects.length == 0) {
            sendServerCommand(module, command, null);
        } else if (objects.length % 2 != 0) {
            DebugLog.log("ERROR: sendServerCommand called with invalid number of arguments (" + module + " " + command + ")");
        } else {
            KahluaTable t = LuaManager.platform.newTable();

            for (int i = 0; i < objects.length; i += 2) {
                Object v = objects[i + 1];
                if (v instanceof Float f) {
                    t.rawset(objects[i], f.doubleValue());
                } else if (v instanceof Integer integer) {
                    t.rawset(objects[i], integer.doubleValue());
                } else if (v instanceof Short s) {
                    t.rawset(objects[i], s.doubleValue());
                } else {
                    t.rawset(objects[i], v);
                }
            }

            sendServerCommand(module, command, t);
        }
    }

    public static void sendServerCommand(IsoPlayer player, String module, String command, KahluaTable args) {
        if (PlayerToAddressMap.containsKey(player)) {
            long id = PlayerToAddressMap.get(player);
            UdpConnection con = udpEngine.getActiveConnection(id);
            if (con != null) {
                sendServerCommand(module, command, args, con);
            }
        }
    }

    public static ArrayList<IsoPlayer> getPlayers(ArrayList<IsoPlayer> players) {
        players.clear();

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                IsoPlayer player = c.players[playerIndex];
                if (player != null && player.onlineId != -1) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    public static ArrayList<IsoPlayer> getPlayers() {
        ArrayList<IsoPlayer> players = new ArrayList<>();
        return getPlayers(players);
    }

    public static int getPlayerCount() {
        int count = 0;

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getRole() != null && !c.getRole().hasCapability(Capability.HideFromSteamUserList)) {
                for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                    if (c.playerIds[playerIndex] != -1) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static String addUser(String newUsername, String newUserPassword) {
        if (!ServerWorldDatabase.isValidUserName(newUsername)) {
            return "Invalid username \"" + newUsername + "\"";
        } else {
            try {
                return ServerWorldDatabase.instance.addUser(newUsername.trim(), newUserPassword.trim());
            } catch (SQLException var3) {
                var3.printStackTrace();
                return "exception occurs";
            }
        }
    }

    public static String changeRole(String adminName, UdpConnection adminConnection, String user, String newAccessLevelName) throws SQLException {
        IsoPlayer pl = getPlayerByUserName(user);
        if (!ServerWorldDatabase.instance.containsUser(user) && pl != null) {
            UdpConnection c = getConnectionFromPlayer(pl);
            if (c != null) {
                String addUserResult = addUser(user, c.password);
                if (!ServerWorldDatabase.instance.containsUser(user)) {
                    return addUserResult;
                }
            }
        }

        if ((adminConnection == null || !adminConnection.isCoopHost) && !ServerWorldDatabase.instance.containsUser(user) && adminConnection != null) {
            return "User \"" + user + "\" is not in the whitelist nor the server, use /adduser first";
        } else {
            Role newRole = Roles.getRole(newAccessLevelName.trim());
            if (adminConnection == null
                || !adminConnection.getRole().hasCapability(Capability.ChangeAccessLevel)
                || adminConnection.getRole().getPosition() >= ServerWorldDatabase.instance.getUserRoleNameByUsername(user).getPosition()
                    && adminConnection.getRole().getPosition() >= newRole.getPosition()) {
                if (newRole == null) {
                    String accessLevels = "";

                    for (Role r : Roles.getRoles()) {
                        if (!accessLevels.isEmpty()) {
                            accessLevels = accessLevels + ", ";
                        }

                        accessLevels = accessLevels + r.getName();
                    }

                    return "Access Level '" + newAccessLevelName.trim() + "' unknown, list of access level: " + accessLevels;
                } else {
                    if (pl != null) {
                        if (pl.getNetworkCharacterAI() != null) {
                            pl.getNetworkCharacterAI().setCheckAccessLevelDelay(2000L);
                        }

                        UdpConnection connection1 = getConnectionFromPlayer(pl);
                        Role oldRole = null;
                        if (connection1 != null) {
                            oldRole = connection1.getRole();
                        }

                        if (oldRole != newRole) {
                            if (newRole.hasCapability(Capability.AdminChat) && !oldRole.hasCapability(Capability.AdminChat)) {
                                ChatServer.getInstance().joinAdminChat(pl.onlineId);
                            } else if (!newRole.hasCapability(Capability.AdminChat) && oldRole.hasCapability(Capability.AdminChat)) {
                                ChatServer.getInstance().leaveAdminChat(pl.onlineId);
                            }
                        }

                        if (!newRole.hasCapability(Capability.ToggleInvisibleHimself) && oldRole.hasCapability(Capability.ToggleInvisibleHimself)) {
                            pl.setGhostMode(false);
                        }

                        if (!newRole.hasCapability(Capability.ToggleNoclipHimself) && oldRole.hasCapability(Capability.ToggleNoclipHimself)) {
                            pl.setNoClip(false);
                        }

                        if (!newRole.hasCapability(Capability.ToggleGodModHimself) && oldRole.hasCapability(Capability.ToggleGodModHimself)) {
                            pl.setGodMod(false);
                        }

                        pl.setRole(newRole);
                        if (connection1 != null) {
                            connection1.setRole(newRole);
                        }

                        if (!newRole.hasCapability(Capability.HideFromSteamUserList) && oldRole.hasCapability(Capability.HideFromSteamUserList)) {
                            SteamGameServer.AddPlayer(pl);
                        }

                        if (newRole.hasCapability(Capability.HideFromSteamUserList) && !oldRole.hasCapability(Capability.HideFromSteamUserList)) {
                            SteamGameServer.RemovePlayer(pl);
                        }

                        if (newRole.hasCapability(Capability.ToggleInvisibleHimself) && !oldRole.hasCapability(Capability.ToggleInvisibleHimself)) {
                            pl.setGhostMode(true);
                        }

                        if (newRole.hasCapability(Capability.ToggleNoclipHimself) && !oldRole.hasCapability(Capability.ToggleNoclipHimself)) {
                            pl.setNoClip(true);
                        }

                        if (newRole.hasCapability(Capability.ToggleGodModHimself) && !oldRole.hasCapability(Capability.ToggleGodModHimself)) {
                            pl.setGodMod(true);
                        }

                        sendPlayerExtraInfo(pl, null);
                    }

                    LoggerManager.getLogger("admin").write(adminName + " granted " + newRole.getName() + " access level on " + user);
                    return ServerWorldDatabase.instance.setRole(user, newRole);
                }
            } else {
                return "You do not have sufficient rights to set this access level.";
            }
        }
    }

    public static void sendAmbient(String name, int x, int y, int radius, float volume) {
        DebugLog.log(DebugType.Sound, "ambient: sending " + name + " at " + x + "," + y + " radius=" + radius);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            IsoPlayer p = getAnyPlayerFromConnection(c);
            if (p != null) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.AddAmbient.doPacket(b2);
                b2.putUTF(name);
                b2.putInt(x);
                b2.putInt(y);
                b2.putInt(radius);
                b2.putFloat(volume);
                PacketTypes.PacketType.AddAmbient.send(c);
            }
        }
    }

    public static void sendChangeSafety(Safety safety) {
        try {
            SafetyPacket packet = new SafetyPacket(safety);

            for (UdpConnection c : udpEngine.connections) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.ChangeSafety.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ChangeSafety.send(c);
            }
        } catch (Exception var5) {
            DebugType.Multiplayer.printException(var5, "SendChangeSafety: failed", LogSeverity.Error);
        }
    }

    static void receivePing(ByteBufferReader bb, UdpConnection connection, short packetType) {
        connection.setPinged(true);
        answerPing(bb, connection);
    }

    public static void updateOverlayForClients(IsoObject object, String spriteName, float r, float g, float b, float a, UdpConnection playerConnection) {
        if (udpEngine != null) {
            INetworkPacket.sendToRelative(
                PacketTypes.PacketType.UpdateOverlaySprite, playerConnection, object.square.x, object.square.y, object, spriteName, r, g, b, a
            );
        }
    }

    public static void sendReanimatedZombieID(IsoPlayer player, IsoZombie zombie) {
        if (PlayerToAddressMap.containsKey(player)) {
            sendObjectChange(player, IsoObjectChange.REANIMATED_ID, "ID", (double)zombie.onlineId);
        }
    }

    public static void receiveRadioServerData(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ByteBufferWriter bb2 = connection.startPacket();
        PacketTypes.PacketType.RadioServerData.doPacket(bb2);
        ZomboidRadio.getInstance().WriteRadioServerDataPacket(bb2);
        PacketTypes.PacketType.RadioServerData.send(connection);
    }

    public static void receiveRadioDeviceDataState(ByteBufferReader bb, UdpConnection connection, short packetType) {
        byte deviceType = bb.getByte();
        if (deviceType == 1) {
            int x = bb.getInt();
            int y = bb.getInt();
            int z = bb.getInt();
            int index = bb.getInt();
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (sq != null && index >= 0 && index < sq.getObjects().size()) {
                IsoObject obj = sq.getObjects().get(index);
                if (obj instanceof IsoWaveSignal isoWaveSignal) {
                    DeviceData deviceData = isoWaveSignal.getDeviceData();
                    if (deviceData != null) {
                        try {
                            deviceData.receiveDeviceDataStatePacket(bb, null);
                        } catch (Exception var15) {
                            System.out.print(var15.getMessage());
                        }
                    }
                }
            }
        } else if (deviceType == 0) {
            byte playerIndex = bb.getByte();
            IsoPlayer player = getPlayerFromConnection(connection, playerIndex);
            int hand = bb.getByte();
            if (player != null) {
                Radio radio = null;
                if (hand == 1 && player.getPrimaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getPrimaryHandItem();
                }

                if (hand == 2 && player.getSecondaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getSecondaryHandItem();
                }

                if (radio != null && radio.getDeviceData() != null) {
                    try {
                        radio.getDeviceData().receiveDeviceDataStatePacket(bb, connection);
                    } catch (Exception var14) {
                        System.out.print(var14.getMessage());
                    }
                }
            }
        } else if (deviceType == 2) {
            short vehicleID = bb.getShort();
            short partIndex = bb.getShort();
            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(vehicleID);
            if (vehicle != null) {
                VehiclePart part = vehicle.getPartByIndex(partIndex);
                if (part != null) {
                    DeviceData deviceData = part.getDeviceData();
                    if (deviceData != null) {
                        try {
                            deviceData.receiveDeviceDataStatePacket(bb, null);
                        } catch (Exception var13) {
                            System.out.print(var13.getMessage());
                        }
                    }
                }
            }
        }
    }

    public static void sendIsoWaveSignal(
        long source, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV
    ) {
        WaveSignalPacket packet = new WaveSignalPacket();
        packet.set(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (source != c.getConnectedGUID()) {
                ByteBufferWriter bb = c.startPacket();
                PacketTypes.PacketType.WaveSignal.doPacket(bb);
                packet.write(bb);
                PacketTypes.PacketType.WaveSignal.send(c);
            }
        }
    }

    public static void receivePlayerListensChannel(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int channel = bb.getInt();
        boolean listenmode = bb.getBoolean();
        boolean isTV = bb.getBoolean();
        ZomboidRadio.getInstance().PlayerListensChannel(channel, listenmode, isTV);
    }

    public static void sendAlarm(int x, int y) {
        DebugLog.log(DebugType.Multiplayer, "SendAlarm at [ " + x + " , " + y + " ]");

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            IsoPlayer p = getAnyPlayerFromConnection(c);
            if (p != null) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.AddAlarm.doPacket(b2);
                b2.putInt(x);
                b2.putInt(y);
                PacketTypes.PacketType.AddAlarm.send(c);
            }
        }
    }

    public static void sendToxicBuilding(int x, int y, boolean toxic) {
        DebugLog.log(DebugType.Multiplayer, "Send Toxic Building at [ " + x + " , " + y + " Toxic: " + toxic + " ]");

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            IsoPlayer p = getAnyPlayerFromConnection(c);
            if (p != null) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.ToxicBuilding.doPacket(b2);
                b2.putInt(x);
                b2.putInt(y);
                b2.putBoolean(toxic);
                PacketTypes.PacketType.ToxicBuilding.send(c);
            }
        }
    }

    public static boolean isSpawnBuilding(BuildingDef def) {
        return SpawnPoints.instance.isSpawnBuilding(def);
    }

    private static void setFastForward(boolean fastForward) {
        if (fastForward != GameServer.fastForward) {
            GameServer.fastForward = fastForward;
            syncClock();
        }
    }

    static void receiveViewBannedIPs(ByteBufferReader bb, UdpConnection connection, short packetType) throws SQLException {
        sendBannedIPs(connection);
    }

    private static void sendBannedIPs(UdpConnection connection) throws SQLException {
        ArrayList<DBBannedIP> result = ServerWorldDatabase.instance.getBannedIPs();

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.ViewBannedIPs.doPacket(b);
                b.putInt(result.size());

                for (int i = 0; i < result.size(); i++) {
                    DBBannedIP bannedIP = result.get(i);
                    b.putUTF(bannedIP.getUsername());
                    b.putUTF(bannedIP.getIp());
                    b.putUTF(bannedIP.getReason());
                }

                PacketTypes.PacketType.ViewBannedIPs.send(c);
                break;
            }
        }
    }

    static void receiveViewBannedSteamIDs(ByteBufferReader bb, UdpConnection connection, short packetType) throws SQLException {
        sendBannedSteamIDs(connection);
    }

    private static void sendBannedSteamIDs(UdpConnection connection) throws SQLException {
        ArrayList<DBBannedSteamID> result = ServerWorldDatabase.instance.getBannedSteamIDs();

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.ViewBannedSteamIDs.doPacket(b);
                b.putInt(result.size());

                for (int i = 0; i < result.size(); i++) {
                    DBBannedSteamID bannedSteamID = result.get(i);
                    b.putUTF(bannedSteamID.getSteamID());
                    b.putUTF(bannedSteamID.getReason());
                }

                PacketTypes.PacketType.ViewBannedSteamIDs.send(c);
                break;
            }
        }
    }

    public static void sendTickets(String author, UdpConnection connection) throws SQLException {
        ArrayList<DBTicket> result = ServerWorldDatabase.instance.getTickets(author);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.ViewTickets.doPacket(b);
                b.putInt(result.size());

                for (int i = 0; i < result.size(); i++) {
                    DBTicket ticket = result.get(i);
                    b.putUTF(ticket.getAuthor());
                    b.putUTF(ticket.getMessage());
                    b.putInt(ticket.getTicketID());
                    b.putBoolean(ticket.isViewed());
                    if (b.putBoolean(ticket.getAnswer() != null)) {
                        b.putUTF(ticket.getAnswer().getAuthor());
                        b.putUTF(ticket.getAnswer().getMessage());
                        b.putInt(ticket.getAnswer().getTicketID());
                        b.putBoolean(ticket.getAnswer().isViewed());
                    }
                }

                PacketTypes.PacketType.ViewTickets.send(c);
                break;
            }
        }
    }

    public static boolean sendItemListNet(
        UdpConnection ignore, IsoPlayer sender, ArrayList<InventoryItem> items, IsoPlayer receiver, String sessionID, String custom
    ) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (ignore == null || c != ignore) {
                if (receiver != null) {
                    boolean hasReceiver = false;

                    for (int i = 0; i < c.players.length; i++) {
                        IsoPlayer p = c.players[i];
                        if (p != null && p == receiver) {
                            hasReceiver = true;
                            break;
                        }
                    }

                    if (!hasReceiver) {
                        continue;
                    }
                }

                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SendItemListNet.doPacket(b);
                if (b.putBoolean(receiver != null)) {
                    b.putShort(receiver.getOnlineID());
                }

                if (b.putBoolean(sender != null)) {
                    b.putShort(sender.getOnlineID());
                }

                b.putUTF(sessionID);
                if (b.putBoolean(custom != null)) {
                    b.putUTF(custom);
                }

                try {
                    CompressIdenticalItems.save(b.bb, items, null);
                } catch (Exception var11) {
                    DebugType.General.printException(var11, "", LogSeverity.Error);
                    c.cancelPacket();
                    return false;
                }

                PacketTypes.PacketType.SendItemListNet.send(c);
            }
        }

        return true;
    }

    static void receiveSendItemListNet(ByteBufferReader bb, UdpConnection connection, short packetType) {
        IsoPlayer receiver = null;
        if (bb.getBoolean()) {
            receiver = IDToPlayerMap.get(bb.getShort());
        }

        IsoPlayer sender = null;
        if (bb.getBoolean()) {
            sender = IDToPlayerMap.get(bb.getShort());
        }

        String sessionID = bb.getUTF();
        String custom = null;
        if (bb.getBoolean()) {
            custom = bb.getUTF();
        }

        ArrayList<InventoryItem> items = new ArrayList<>();

        try {
            CompressIdenticalItems.load(bb.bb, 249, items, null);
        } catch (Exception var9) {
            DebugType.General.printException(var9, "", LogSeverity.Error);
        }

        if (receiver == null) {
            LuaEventManager.triggerEvent("OnReceiveItemListNet", sender, items, receiver, sessionID, custom);
        } else {
            sendItemListNet(connection, sender, items, receiver, sessionID, custom);
        }
    }

    static void receiveClimateManagerPacket(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ClimateManager cm = ClimateManager.getInstance();
        if (cm != null) {
            try {
                cm.receiveClimatePacket(bb, connection);
            } catch (Exception var5) {
                DebugType.General.printException(var5, "", LogSeverity.Error);
            }
        }
    }

    static void receiveIsoRegionClientRequestFullUpdate(ByteBufferReader bb, UdpConnection connection, short packetType) {
        IsoRegions.receiveClientRequestFullDataChunks(bb, connection);
    }

    private static String isWorldVersionUnsupported() {
        File inFile = new File(
            ZomboidFileSystem.instance.getSaveDir() + File.separator + "Multiplayer" + File.separator + serverName + File.separator + "map_t.bin"
        );
        if (inFile.exists()) {
            DebugLog.log("checking server WorldVersion in map_t.bin");

            try {
                String savedWorldVersion;
                try (
                    FileInputStream inStream = new FileInputStream(inFile);
                    DataInputStream input = new DataInputStream(inStream);
                ) {
                    byte b1 = input.readByte();
                    byte b2 = input.readByte();
                    byte b3 = input.readByte();
                    byte b4 = input.readByte();
                    if (b1 == 71 && b2 == 77 && b3 == 84 && b4 == 77) {
                        int savedWorldVersionx = input.readInt();
                        if (savedWorldVersionx > 249) {
                            return "The server savefile appears to be from a newer version of the game and cannot be loaded.";
                        }

                        if (savedWorldVersionx > 143) {
                            return null;
                        }

                        return "The server savefile appears to be from a pre-animations version of the game and cannot be loaded.\nDue to the extent of changes required to implement animations, saves from earlier versions are not compatible.";
                    }

                    savedWorldVersion = "The server savefile appears to be from an old version of the game and cannot be loaded.";
                }

                return savedWorldVersion;
            } catch (Exception var13) {
                DebugType.General.printException(var13, "", LogSeverity.Error);
            }
        } else {
            DebugLog.log("map_t.bin does not exist, cannot determine the server's WorldVersion.  This is ok the first time a server is started.");
        }

        return null;
    }

    public String getPoisonousBerry() {
        return this.poisonousBerry;
    }

    public void setPoisonousBerry(String poisonousBerry) {
        this.poisonousBerry = poisonousBerry;
    }

    public String getPoisonousMushroom() {
        return this.poisonousMushroom;
    }

    public void setPoisonousMushroom(String poisonousMushroom) {
        this.poisonousMushroom = poisonousMushroom;
    }

    public String getDifficulty() {
        return this.difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public static void transmitBrokenGlass(IsoGridSquare sq) {
        AddBrokenGlassPacket packet = new AddBrokenGlassPacket();
        packet.set(sq);

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            try {
                if (c.isRelevantTo(sq.getX(), sq.getY())) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.AddBrokenGlass.doPacket(b2);
                    packet.write(b2);
                    PacketTypes.PacketType.AddBrokenGlass.send(c);
                }
            } catch (Throwable var5) {
                c.cancelPacket();
                ExceptionLogger.logException(var5);
            }
        }
    }

    public static void transmitBigWaterSplash(int x, int y, float dx, float dy) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            try {
                if (c.isRelevantTo(x, y)) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.StartFishSplash.doPacket(b2);
                    b2.putInt(x);
                    b2.putInt(y);
                    b2.putFloat(dx);
                    b2.putFloat(dy);
                    PacketTypes.PacketType.StartFishSplash.send(c);
                }
            } catch (Throwable var7) {
                c.cancelPacket();
                ExceptionLogger.logException(var7);
            }
        }
    }

    public static void receiveBigWaterSplash(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        float dx = bb.getFloat();
        float dy = bb.getFloat();

        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID() && c.isRelevantTo(x, y)) {
                try {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.StartFishSplash.doPacket(b2);
                    b2.putInt(x);
                    b2.putInt(y);
                    b2.putFloat(dx);
                    b2.putFloat(dy);
                    PacketTypes.PacketType.StartFishSplash.send(c);
                } catch (Throwable var10) {
                    c.cancelPacket();
                    ExceptionLogger.logException(var10);
                }
            }
        }
    }

    public static void transmitFishingData(
        int seed, int trashSeed, TLongIntHashMap noiseFishPointDisabler, TLongObjectHashMap<FishSchoolManager.ChumData> chumPoints
    ) {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);

            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.FishingData.doPacket(b2);
                b2.putInt(seed);
                b2.putInt(trashSeed);
                b2.putInt(noiseFishPointDisabler.size());
                noiseFishPointDisabler.forEachKey(l -> {
                    b2.putLong(l);
                    return true;
                });
                b2.putInt(chumPoints.size());
                chumPoints.forEachEntry((key, chumData) -> {
                    b2.putLong(key);
                    b2.putInt(chumData.maxForceTime);
                    return true;
                });
                PacketTypes.PacketType.FishingData.send(c);
            } catch (Throwable var7) {
                c.cancelPacket();
                ExceptionLogger.logException(var7);
            }
        }
    }

    static void receiveFishingDataRequest(ByteBufferReader bb, UdpConnection c, short packetType) {
        try {
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.FishingData.doPacket(b2);
            FishSchoolManager.getInstance().setFishingData(b2);
            PacketTypes.PacketType.FishingData.send(c);
        } catch (Throwable var4) {
            c.cancelPacket();
            ExceptionLogger.logException(var4);
        }
    }

    public static boolean isServerDropPackets() {
        return droppedPackets > 0;
    }

    static void receiveSyncPerks(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int playerIndex = bb.getByte();
        int sneakLvl = bb.getInt();
        int strLvl = bb.getInt();
        int fitLvl = bb.getInt();
        IsoPlayer p = getPlayerFromConnection(connection, playerIndex);
        if (p != null) {
            p.remoteSneakLvl = sneakLvl;
            p.remoteStrLvl = strLvl;
            p.remoteFitLvl = fitLvl;

            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    IsoPlayer p2 = getAnyPlayerFromConnection(connection);
                    if (p2 != null) {
                        try {
                            ByteBufferWriter b2 = c.startPacket();
                            PacketTypes.PacketType.SyncPerks.doPacket(b2);
                            b2.putShort(p.onlineId);
                            b2.putInt(sneakLvl);
                            b2.putInt(strLvl);
                            b2.putInt(fitLvl);
                            PacketTypes.PacketType.SyncPerks.send(c);
                        } catch (Throwable var12) {
                            connection.cancelPacket();
                            ExceptionLogger.logException(var12);
                        }
                    }
                }
            }
        }
    }

    static void receiveSyncEquippedRadioFreq(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int playerIndex = bb.getByte();
        int size = bb.getInt();
        ArrayList<Integer> invRadioFreq = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            invRadioFreq.add(bb.getInt());
        }

        IsoPlayer p = getPlayerFromConnection(connection, playerIndex);
        if (p != null) {
            for (int n = 0; n < udpEngine.connections.size(); n++) {
                UdpConnection c = udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    IsoPlayer p2 = getAnyPlayerFromConnection(connection);
                    if (p2 != null) {
                        try {
                            ByteBufferWriter b2 = c.startPacket();
                            PacketTypes.PacketType.SyncEquippedRadioFreq.doPacket(b2);
                            b2.putShort(p.onlineId);
                            b2.putInt(size);

                            for (int i = 0; i < invRadioFreq.size(); i++) {
                                b2.putInt(invRadioFreq.get(i));
                            }

                            PacketTypes.PacketType.SyncEquippedRadioFreq.send(c);
                        } catch (Throwable var12) {
                            connection.cancelPacket();
                            ExceptionLogger.logException(var12);
                        }
                    }
                }
            }
        }
    }

    public static void sendRadioPostSilence() {
        for (int n = 0; n < udpEngine.connections.size(); n++) {
            UdpConnection c = udpEngine.connections.get(n);
            sendRadioPostSilence(c);
        }
    }

    public static void sendRadioPostSilence(UdpConnection c) {
        try {
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.RadioPostSilenceEvent.doPacket(b);
            b.putBoolean(ZomboidRadio.postRadioSilence);
            PacketTypes.PacketType.RadioPostSilenceEvent.send(c);
        } catch (Exception var2) {
            DebugType.General.printException(var2, "", LogSeverity.Error);
            c.cancelPacket();
        }
    }

    public static void sendSneezingCoughing(IsoPlayer player, int sneezingCoughing, byte sneezeVar) {
        INetworkPacket.sendToRelative(PacketTypes.PacketType.SneezeCough, player.getX(), player.getY(), player, sneezingCoughing, sneezeVar);
    }

    public static boolean isPlayerConnected(IsoPlayer player) {
        return player == null ? false : getConnectionFromPlayer(player) != null;
    }

    private static final class CCFilter {
        String command;
        boolean allow;
        GameServer.CCFilter next;

        boolean matches(String command) {
            return this.command.equals(command) || "*".equals(this.command);
        }

        boolean passes(String command) {
            if (this.matches(command)) {
                return this.allow;
            } else {
                return this.next == null ? true : this.next.passes(command);
            }
        }
    }

    private static class DelayedConnection implements IZomboidPacket {
        public UdpConnection connection;
        public boolean connect;
        public String hostString;
        public long timestamp;

        public DelayedConnection(UdpConnection connection, boolean connect) {
            this.connection = connection;
            this.connect = connect;
            if (connect) {
                try {
                    this.hostString = connection.getInetSocketAddress().getHostString();
                } catch (Exception var4) {
                    DebugType.General.printException(var4, "", LogSeverity.Error);
                }
            }

            this.timestamp = System.currentTimeMillis() + ServerOptions.getInstance().safetyDisconnectDelay.getValue() * 2000L;
        }

        @Override
        public boolean isConnect() {
            return this.connect;
        }

        @Override
        public boolean isDisconnect() {
            return !this.connect;
        }

        public boolean isCooldown() {
            return System.currentTimeMillis() > this.timestamp;
        }

        public void connect() {
            LoggerManager.getLogger("user")
                .write(
                    String.format(
                        "Connection add index=%d guid=%d id=%s", this.connection.getIndex(), this.connection.getConnectedGUID(), this.connection.getIDStr()
                    )
                );
            GameServer.udpEngine.connections.add(this.connection);
            GameServer.connect(this.connection);
        }

        public void disconnect() {
            LoginQueue.disconnect(this.connection);
            ActionManager.getInstance().disconnectPlayer(this.connection);
            LoggerManager.getLogger("user")
                .write(
                    String.format(
                        "Connection remove index=%d guid=%d id=%s", this.connection.getIndex(), this.connection.getConnectedGUID(), this.connection.getIDStr()
                    )
                );
            GameServer.udpEngine.connections.remove(this.connection);
            GameServer.disconnect(this.connection, "receive-disconnect");
        }
    }

    public static enum MapRemotePlayerVisibility {
        NONE(1),
        FACTION_ONLY(2),
        FACTION_AND_VISIBLE_ONLY(3),
        ALL(4);

        public final int value;

        private MapRemotePlayerVisibility(final int value) {
            this.value = value;
        }
    }

    private static class s_performance {
        static final PerformanceProfileFrameProbe frameStep = new PerformanceProfileFrameProbe("GameServer.frameStep");
        static final PerformanceProfileProbe mainLoopDealWithNetData = new PerformanceProfileProbe("GameServer.mainLoopDealWithNetData");
        static final PerformanceProfileProbe RCONServerUpdate = new PerformanceProfileProbe("RCONServer.update");
    }
}
