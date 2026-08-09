// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import se.krka.kahlua.vm.KahluaTable;
import zombie.DebugFileWatcher;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.PersistentOutfits;
import zombie.PredicatedFileWatcher;
import zombie.SandboxOptions;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ai.states.PathFindState;
import zombie.ai.states.WalkTowardState;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.packets.service.PopmanDebugCommandPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.list.PZArrayUtil;

public final class ZombiePopulationManager {
    public static final ZombiePopulationManager instance = new ZombiePopulationManager();
    protected static final int SQUARES_PER_CHUNK = 8;
    protected static final int CHUNKS_PER_CELL = 32;
    protected static final int SQUARES_PER_CELL = 256;
    protected static final byte OLD_ZOMBIE_CRAWLER_CAN_WALK = 1;
    protected static final byte OLD_ZOMBIE_FAKE_DEAD = 2;
    protected static final byte OLD_ZOMBIE_CRAWLER = 3;
    protected static final byte OLD_ZOMBIE_WALKER = 4;
    public static final int INVALID_PATH_XY = Integer.MIN_VALUE;
    protected int minX;
    protected int minY;
    protected int width;
    protected int height;
    protected boolean stopped;
    private final DebugCommands dbgCommands = new DebugCommands();
    public static boolean debugLoggingEnabled;
    public static final ReentrantLock saveLock = new ReentrantLock();
    private static final ConcurrentLinkedQueue<ZombiePopulationManager.PendingCellSave> pendingSaveCells = new ConcurrentLinkedQueue<>();
    private final LoadedAreas loadedAreas = new LoadedAreas(false);
    private final LoadedAreas loadedServerCells = new LoadedAreas(true);
    private final PlayerSpawns playerSpawns = new PlayerSpawns();
    private short[] realZombieCount;
    private short[] realZombieCount2;
    private long realZombieUpdateTime;
    private final ArrayList<IsoZombie> saveRealZombieHack = new ArrayList<>();
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
    private final ByteBuffer readByteBuffer = ByteBuffer.allocateDirect(1024);
    private final ByteBuffer writeByteBuffer = ByteBuffer.allocateDirect(1024);
    private final TIntHashSet newChunks = new TIntHashSet();
    private final Set<Integer> zedClearedChunks = new HashSet<>();
    private final ArrayList<ChooseGameInfo.SpawnOrigin> spawnOrigins = new ArrayList<>();
    private float zombiesMinPerChunk;
    private float zombiesMaxPerChunk = 255.0F;
    public float[] radarXy;
    public int radarCount;
    public boolean radarRenderFlag;
    public boolean radarRequestFlag;
    private final ArrayList<IsoDirections> sittingDirections = new ArrayList<>();

    ZombiePopulationManager() {
        this.newChunks.setAutoCompactionFactor(0.0F);
    }

    private static native void n_init(boolean var0, boolean var1, int var2, int var3, int var4, int var5);

    private static native void n_config(float var0, float var1, float var2, int var3, float var4, float var5, float var6, float var7, int var8);

    private static native void n_configFloat(String var0, float var1);

    private static native void n_configInt(String var0, int var1);

    private static native void n_setSpawnOrigins(int[] var0);

    private static native void n_setOutfitNames(String[] var0);

    private static native void n_updateMain(float var0, double var1);

    private static native boolean n_hasDataForThread();

    private static native boolean n_readyToPause();

    private static native void n_updateThread();

    private static native boolean n_shouldWait();

    private static native void n_beginSaveRealZombies(int var0);

    private static native void n_saveRealZombies(int var0, ByteBuffer var1);

    private static native void n_save();

    private static native void n_saveCell(int var0, int var1);

    private static native void n_stop();

    private static native void n_addZombie(float var0, float var1, float var2, byte var3, int var4, int var5, int var6, int var7);

    private static native void n_aggroTarget(int var0, int var1, int var2);

    private static native void n_loadChunk(int var0, int var1, boolean var2);

    private static native void n_loadedAreas(int var0, int[] var1, boolean var2);

    protected static native void n_realZombieCount(short var0, short[] var1);

    protected static native void n_spawnHorde(int var0, int var1, int var2, int var3, float var4, float var5, int var6);

    private static native void n_worldSound(int var0, int var1, int var2, int var3);

    private static native int n_getAddZombieCount();

    private static native int n_getAddZombieData(int var0, ByteBuffer var1);

    private static native boolean n_hasRadarData();

    private static native void n_requestRadarData();

    private static native int n_getRadarZombieData(float[] var0);

    private static void noise(String s) {
        if (debugLoggingEnabled && (Core.debug || GameServer.server && GameServer.debug)) {
            DebugLog.log("ZPOP: " + s);
        }
    }

    public static void init() {
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.popman"))) {
            DebugLog.log("***** Loading debug version of PZPopMan");
            libSuffix = "d";
        }

        if (System.getProperty("os.name").contains("OS X")) {
            System.loadLibrary("PZPopMan");
        } else {
            System.loadLibrary("PZPopMan64" + libSuffix);
        }

        DebugFileWatcher.instance
            .add(new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_Zombie.xml"), ZombiePopulationManager::onTriggeredZombieFile));
    }

    public void requestSaveCell(int popmanCellX, int popmanCellY) {
        if (!GameClient.client) {
            saveLock.lock();

            try {
                List<ZombiePopulationManager.ZombieSaveData> snapshot = new ArrayList<>();

                for (IsoZombie z : IsoWorld.instance.currentCell.getZombieList()) {
                    if (!z.isReanimatedPlayer() && (!GameServer.server || !z.indoorZombie) && !z.isDead()) {
                        int zCellX = (int)Math.floor(z.getX() / 256.0F);
                        int zCellY = (int)Math.floor(z.getY() / 256.0F);
                        if (zCellX == popmanCellX && zCellY == popmanCellY) {
                            snapshot.add(new ZombiePopulationManager.ZombieSaveData(z));
                        }
                    }
                }

                pendingSaveCells.offer(new ZombiePopulationManager.PendingCellSave(popmanCellX, popmanCellY, snapshot));
            } finally {
                saveLock.unlock();
            }
        }
    }

    public void processPendingSaveCells() {
        if (!GameClient.client) {
            ZombiePopulationManager.PendingCellSave req;
            while ((req = pendingSaveCells.poll()) != null) {
                saveLock.lock();

                try {
                    this.writeCellSnapshot(req);
                } finally {
                    saveLock.unlock();
                }
            }
        }
    }

    private void writeCellSnapshot(ZombiePopulationManager.PendingCellSave req) {
        int total = req.aliveZombies.size();
        n_beginSaveRealZombies(total);
        int i = 0;

        while (i < total) {
            this.byteBuffer.clear();
            int count = 0;

            while (i < total) {
                int position = this.byteBuffer.position();
                ZombiePopulationManager.ZombieSaveData zd = req.aliveZombies.get(i++);
                this.byteBuffer.putFloat(zd.x);
                this.byteBuffer.putFloat(zd.y);
                this.byteBuffer.putFloat(zd.z);
                this.byteBuffer.put(zd.dir);
                this.byteBuffer.putInt(zd.descriptorID);
                this.byteBuffer.putInt(zd.state);
                count++;
                int numBytes = this.byteBuffer.position() - position;
                if (this.byteBuffer.position() + numBytes > this.byteBuffer.capacity()) {
                    break;
                }
            }

            n_saveRealZombies(count, this.byteBuffer);
        }

        n_saveCell(req.popmanCellX, req.popmanCellY);
    }

    private static void onTriggeredZombieFile(String xmlFile) {
        DebugType.General.println("ZombiePopulationManager.onTriggeredZombieFile(" + xmlFile + ">");

        ZombieTriggerXmlFile triggerXml;
        try {
            triggerXml = PZXmlUtil.parse(ZombieTriggerXmlFile.class, xmlFile);
        } catch (PZXmlParserException var3) {
            System.err.println("ZombiePopulationManager.onTriggeredZombieFile> Exception thrown. " + var3);
            DebugType.General.printException(var3, LogSeverity.Error);
            return;
        }

        if (triggerXml.spawnHorde > 0) {
            processTriggerSpawnHorde(triggerXml);
        }

        if (triggerXml.setDebugLoggingEnabled && debugLoggingEnabled != triggerXml.debugLoggingEnabled) {
            debugLoggingEnabled = triggerXml.debugLoggingEnabled;
            DebugType.General.println("  bDebugLoggingEnabled: " + debugLoggingEnabled);
        }
    }

    private static void processTriggerSpawnHorde(ZombieTriggerXmlFile triggerXml) {
        DebugType.General.println("  spawnHorde: " + triggerXml.spawnHorde);
        if (IsoPlayer.getInstance() != null) {
            IsoPlayer player = IsoPlayer.getInstance();
            instance.createHordeFromTo(
                PZMath.fastfloor(player.getX()),
                PZMath.fastfloor(player.getY()),
                PZMath.fastfloor(player.getX()),
                PZMath.fastfloor(player.getY()),
                triggerXml.spawnHorde
            );
        }
    }

    public void init(IsoMetaGrid metaGrid) {
        if (!GameClient.client) {
            this.minX = metaGrid.getMinX();
            this.minY = metaGrid.getMinY();
            this.width = metaGrid.getWidth();
            this.height = metaGrid.getHeight();
            this.stopped = false;
            n_init(GameClient.client, GameServer.server, this.minX, this.minY, this.width, this.height);
            this.onConfigReloaded();
            String[] outfitNames = PersistentOutfits.instance.getOutfitNames().toArray(new String[0]);

            for (int i = 0; i < outfitNames.length; i++) {
                outfitNames[i] = outfitNames[i].toLowerCase();
            }

            n_setOutfitNames(outfitNames);
            TIntArrayList origins = new TIntArrayList();

            for (ChooseGameInfo.SpawnOrigin spawnOrigin : this.spawnOrigins) {
                origins.add(spawnOrigin.x);
                origins.add(spawnOrigin.y);
                origins.add(spawnOrigin.w);
                origins.add(spawnOrigin.h);
            }

            n_setSpawnOrigins(origins.toArray());
        }
    }

    public void onConfigReloaded() {
        SandboxOptions.ZombieConfig cfg = SandboxOptions.instance.zombieConfig;
        n_configFloat("PopulationMultiplier", (float)cfg.populationMultiplier.getValue());
        n_configFloat("PopulationStartMultiplier", (float)cfg.populationStartMultiplier.getValue());
        n_configFloat("PopulationPeakMultiplier", (float)cfg.populationPeakMultiplier.getValue());
        n_configInt("PopulationPeakDay", cfg.populationPeakDay.getValue());
        n_configFloat("RespawnHours", (float)cfg.respawnHours.getValue());
        n_configFloat("RespawnUnseenHours", (float)cfg.respawnUnseenHours.getValue());
        n_configFloat("RespawnMultiplier", (float)cfg.respawnMultiplier.getValue());
        n_configFloat("RedistributeHours", (float)cfg.redistributeHours.getValue());
        n_configInt("FollowSoundDistance", cfg.followSoundDistance.getValue());
        n_configFloat("MinZombiesPerChunk", this.zombiesMinPerChunk);
        n_configFloat("MaxZombiesPerChunk", this.zombiesMaxPerChunk);
        n_configFloat("UniformZombiesPerChunk", 0.2F);
    }

    public void registerSpawnOrigin(int x, int y, int width, int height, KahluaTable properties) {
        if (x >= 0 && y >= 0 && width >= 0 && height >= 0) {
            this.spawnOrigins.add(new ChooseGameInfo.SpawnOrigin(x, y, width, height));
        }
    }

    public void playerSpawnedAt(int x, int y, int z) {
        this.playerSpawns.addSpawn(x, y, z);
    }

    public void setZombiesMinPerChunk(float f) {
        this.zombiesMinPerChunk = f;
    }

    public void setZombiesMaxPerChunk(float f) {
        this.zombiesMaxPerChunk = f;
    }

    public void addChunkToWorld(IsoChunk chunk) {
        if (!GameClient.client) {
            if (chunk.isNewChunk()) {
                int key = chunk.wy << 16 | chunk.wx;
                this.newChunks.add(key);
            }

            n_loadChunk(chunk.wx, chunk.wy, true);
        }
    }

    public void removeChunkFromWorld(IsoChunk chunk) {
        if (!GameClient.client) {
            if (!this.stopped) {
                n_loadChunk(chunk.wx, chunk.wy, false);

                for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            IsoGridSquare sq = chunk.getGridSquare(x, y, z);
                            if (sq != null && !sq.getMovingObjects().isEmpty()) {
                                for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                                    IsoMovingObject mo = sq.getMovingObjects().get(i);
                                    if (mo instanceof IsoZombie realZombie
                                        && (!GameServer.server || !realZombie.indoorZombie)
                                        && !realZombie.isReanimatedPlayer()) {
                                        int state = ZombieStateFlags.intFromZombie(realZombie);
                                        saveLock.lock();

                                        try {
                                            if (z != 0
                                                || sq.getRoom() != null
                                                || realZombie.getCurrentState() != WalkTowardState.instance()
                                                    && realZombie.getCurrentState() != PathFindState.instance()) {
                                                DebugType.Zombie.debugln("Virtualizing stationary Zombie: %s", realZombie);
                                                n_addZombie(
                                                    realZombie.getX(),
                                                    realZombie.getY(),
                                                    realZombie.getZ(),
                                                    (byte)realZombie.getForwardIsoDirection().ordinal(),
                                                    realZombie.getPersistentOutfitID(),
                                                    state,
                                                    Integer.MIN_VALUE,
                                                    Integer.MIN_VALUE
                                                );
                                                realZombie.removeFromWorld();
                                                realZombie.removeFromSquare();
                                                i--;
                                            } else {
                                                DebugType.Zombie.debugln("Virtualizing moving Zombie: %s", realZombie);
                                                n_addZombie(
                                                    realZombie.getX(),
                                                    realZombie.getY(),
                                                    realZombie.getZ(),
                                                    (byte)realZombie.getForwardIsoDirection().ordinal(),
                                                    realZombie.getPersistentOutfitID(),
                                                    state,
                                                    realZombie.getPathTargetX(),
                                                    realZombie.getPathTargetY()
                                                );
                                                realZombie.removeFromWorld();
                                                realZombie.removeFromSquare();
                                                i--;
                                            }
                                        } finally {
                                            saveLock.unlock();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                int key = chunk.wy << 16 | chunk.wx;
                this.newChunks.remove(key);
                if (GameServer.server) {
                    MapCollisionData.instance.notifyThread();
                }
            }
        }
    }

    public void virtualizeZombie(IsoZombie realZombie) {
        DebugType.Zombie.debugln("Virtualizing Zombie: %s", realZombie);
        int state = ZombieStateFlags.intFromZombie(realZombie);
        saveLock.lock();

        try {
            n_addZombie(
                realZombie.getX(),
                realZombie.getY(),
                realZombie.getZ(),
                (byte)realZombie.getForwardIsoDirection().ordinal(),
                realZombie.getPersistentOutfitID(),
                state,
                realZombie.getPathTargetX(),
                realZombie.getPathTargetY()
            );
            realZombie.removeFromWorld();
            realZombie.removeFromSquare();
        } finally {
            saveLock.unlock();
        }
    }

    public void setAggroTarget(int id, int x, int y) {
        n_aggroTarget(id, x, y);
    }

    public void createHordeFromTo(int spawnX, int spawnY, int targetX, int targetY, int count) {
        n_spawnHorde(spawnX, spawnY, 0, 0, targetX, targetY, count);
    }

    public void createHordeInAreaTo(int spawnX, int spawnY, int spawnW, int spawnH, int targetX, int targetY, int count) {
        n_spawnHorde(spawnX, spawnY, spawnW, spawnH, targetX, targetY, count);
    }

    public boolean readyToPause() {
        return n_readyToPause();
    }

    public void addWorldSound(WorldSoundManager.WorldSound sound, boolean doSend) {
        if (!GameClient.client) {
            if (sound.radius >= 50) {
                if (!sound.sourceIsZombie) {
                    if (sound.stressZombies) {
                        int hearing = SandboxOptions.instance.lore.hearing.getValue();
                        if (hearing == 4 || hearing == 5) {
                            hearing = 2;
                        }

                        float radiusMultiplier = WorldSoundManager.instance.getHearingMultiplier(hearing);
                        n_worldSound(sound.x, sound.y, (int)PZMath.ceil(sound.radius * radiusMultiplier), sound.volume);
                    }
                }
            }
        }
    }

    private void updateRealZombieCount() {
        if (this.realZombieCount == null || this.realZombieCount.length != this.width * this.height) {
            this.realZombieCount = new short[this.width * this.height];
            this.realZombieCount2 = new short[this.width * this.height * 3];
        }

        Arrays.fill(this.realZombieCount, (short)0);
        ArrayList<IsoZombie> zombies = IsoWorld.instance.currentCell.getZombieList();

        for (int i = 0; i < zombies.size(); i++) {
            IsoZombie z = zombies.get(i);
            int x = PZMath.fastfloor(z.getX() / 256.0F) - this.minX;
            int y = PZMath.fastfloor(z.getY() / 256.0F) - this.minY;
            int countIdx = x + y * this.width;
            if (countIdx >= 0 && countIdx < this.realZombieCount.length) {
                this.realZombieCount[countIdx]++;
            }
        }

        short nonZero = 0;

        for (int ix = 0; ix < this.width * this.height; ix++) {
            if (this.realZombieCount[ix] > 0) {
                this.realZombieCount2[nonZero * 3 + 0] = (short)(ix % this.width);
                this.realZombieCount2[nonZero * 3 + 1] = (short)(ix / this.width);
                this.realZombieCount2[nonZero * 3 + 2] = this.realZombieCount[ix];
                nonZero++;
            }
        }

        n_realZombieCount(nonZero, this.realZombieCount2);
    }

    public void updateMain() {
        if (!GameClient.client) {
            long currentTimeMs = System.currentTimeMillis();
            n_updateMain(GameTime.getInstance().getMultiplier(), GameTime.getInstance().getWorldAgeHours());
            this.zedClearedChunks.clear();
            int numStanding = 0;
            int numMoving = 0;
            int total = n_getAddZombieCount();
            int offset = 0;

            while (offset < total) {
                this.readByteBuffer.clear();
                int count = n_getAddZombieData(offset, this.readByteBuffer);
                offset += count;

                for (int i = 0; i < count; i++) {
                    float x = this.readByteBuffer.getFloat();
                    float y = this.readByteBuffer.getFloat();
                    float z = this.readByteBuffer.getFloat();
                    IsoDirections dir = IsoDirections.fromIndex(this.readByteBuffer.get());
                    int descriptorID = this.readByteBuffer.getInt();
                    ZombieStateFlags state = ZombieStateFlags.fromInt(this.readByteBuffer.getInt());
                    int pathTargetX = this.readByteBuffer.getInt();
                    int pathTargetY = this.readByteBuffer.getInt();
                    int wx = PZMath.fastfloor(x) / 8;
                    int wy = PZMath.fastfloor(y) / 8;
                    int key = wy << 16 | wx;
                    if (this.newChunks.contains(key)) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
                        if (sq != null && sq.roomId != -1L) {
                            continue;
                        }
                    }

                    if (pathTargetX != Integer.MIN_VALUE && this.loadedAreas.isOnEdge(PZMath.fastfloor(x), PZMath.fastfloor(y))) {
                        pathTargetX = Integer.MIN_VALUE;
                        pathTargetY = Integer.MIN_VALUE;
                    }

                    if (pathTargetX == Integer.MIN_VALUE) {
                        this.addZombieStanding(x, y, z, dir, descriptorID, state);
                        numStanding++;
                    } else {
                        this.addZombieMoving(x, y, z, dir, descriptorID, state, pathTargetX, pathTargetY);
                        numMoving++;
                    }
                }
            }

            if (numStanding > 0) {
                noise("unloaded -> real " + total);
            }

            if (numMoving > 0) {
                noise("virtual -> real " + total);
            }

            if (this.radarRenderFlag && this.radarXy != null) {
                if (this.radarRequestFlag) {
                    if (n_hasRadarData()) {
                        this.radarCount = n_getRadarZombieData(this.radarXy);
                        this.radarRenderFlag = false;
                        this.radarRequestFlag = false;
                    }
                } else {
                    n_requestRadarData();
                    this.radarRequestFlag = true;
                }
            }

            this.updateLoadedAreas();
            if (this.realZombieUpdateTime + 5000L < currentTimeMs) {
                this.realZombieUpdateTime = currentTimeMs;
                this.updateRealZombieCount();
            }

            if (GameServer.server) {
                MPDebugInfo.instance.serverUpdate();
            }

            boolean hasData1 = n_hasDataForThread();
            boolean hasData2 = MapCollisionData.instance.hasDataForThread();
            if (hasData1 || hasData2) {
                MapCollisionData.instance.notifyThread();
            }

            this.playerSpawns.update();
        }
    }

    private void clearChunkForReplace(IsoChunk chunk) {
        if (chunk != null) {
            int key = chunk.wy << 16 | chunk.wx;
            if (this.zedClearedChunks.add(key)) {
                for (int z = chunk.minLevel; z <= chunk.maxLevel; z++) {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            this.clearSquare(chunk.getGridSquare(x, y, z));
                        }
                    }
                }
            }
        }
    }

    private void clearSquare(IsoGridSquare sq) {
        if (sq != null) {
            List<IsoMovingObject> mov = sq.getMovingObjects();
            if (mov != null && !mov.isEmpty()) {
                List<IsoZombie> toRemove = new ArrayList<>();

                for (IsoMovingObject obj : mov) {
                    if (obj instanceof IsoZombie zb && !zb.isReanimatedPlayer() && (!GameServer.server || !zb.indoorZombie)) {
                        toRemove.add(zb);
                    }
                }

                if (!toRemove.isEmpty()) {
                    for (IsoZombie zb : toRemove) {
                        if (GameServer.server) {
                            NetworkZombiePacker.getInstance().deleteZombie(zb);
                        }

                        zb.removeFromWorld();
                        zb.removeFromSquare();
                    }
                }
            }
        }
    }

    private void addZombieStanding(float x, float y, float z, IsoDirections dir, int descriptorID, ZombieStateFlags state) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        if (sq != null && (sq.solidFloorCached ? sq.solidFloor : sq.TreatAsSolidFloor())) {
            if (!Core.lastStand && !this.playerSpawns.allowZombie(sq)) {
                noise("removed zombie near player spawn " + PZMath.fastfloor(x) + "," + PZMath.fastfloor(y) + "," + PZMath.fastfloor(z));
            } else {
                VirtualZombieManager.instance.choices.clear();
                IsoGridSquare sqWall = null;
                if (!state.isCrawling() && !state.isFakeDead() && Rand.Next(3) == 0) {
                    sqWall = this.getSquareForSittingZombie(x, y, PZMath.fastfloor(z));
                }

                if (sqWall != null) {
                    VirtualZombieManager.instance.choices.add(sqWall);
                } else {
                    VirtualZombieManager.instance.choices.add(sq);
                }

                this.clearChunkForReplace(sq.getChunk());
                IsoZombie realZombie = VirtualZombieManager.instance.createRealZombieAlways(descriptorID, dir, false);
                if (realZombie == null) {
                    DebugType.Zombie.debugln("Failed to create standing Zombie.");
                } else {
                    if (sqWall != null) {
                        this.sitAgainstWall(realZombie, sqWall);
                    } else {
                        realZombie.setX(x);
                        realZombie.setY(y);
                    }

                    if (state.isFakeDead()) {
                        realZombie.setHealth(0.5F + Rand.Next(0.0F, 0.3F));
                        realZombie.sprite = realZombie.legsSprite;
                        realZombie.setFakeDead(true);
                    } else if (state.isCrawling()) {
                        realZombie.setCrawler(true);
                        realZombie.setCanWalk(state.isCanWalk());
                        realZombie.setOnFloor(true);
                        realZombie.setFallOnFront(true);
                        realZombie.walkVariant = "ZombieWalk";
                        realZombie.DoZombieStats();
                    }

                    if (state.isInitialized()) {
                        realZombie.setCanCrawlUnderVehicle(state.isCanCrawlUnderVehicle());
                    } else {
                        this.firstTimeLoaded(realZombie, state);
                    }

                    realZombie.setReanimatedForGrappleOnly(state.isReanimatedForGrappleOnly());
                    DebugType.Zombie.debugln("Created standing Zombie: %s", realZombie);
                }
            }
        } else {
            noise("real -> unloaded");
            n_addZombie(x, y, z, (byte)dir.ordinal(), descriptorID, state.asInt(), Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }

    private IsoGridSquare getSquareForSittingZombie(float x, float y, int z) {
        int checkRange = 3;

        for (int dx = -3; dx < 3; dx++) {
            for (int dy = -3; dy < 3; dy++) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(x) + dx, PZMath.fastfloor(y) + dy, z);
                if (square != null && square.isFree(true) && square.getBuilding() == null) {
                    int wallType = square.getWallType();
                    if (wallType != 0 && !PolygonalMap2.instance.lineClearCollide(x, y, square.x + 0.5F, square.y + 0.5F, square.z, null, false, true)) {
                        return square;
                    }
                }
            }
        }

        return null;
    }

    public void sitAgainstWall(IsoZombie zombie, IsoGridSquare square) {
        float zedX = square.x + 0.5F;
        float zedY = square.y + 0.5F;
        zombie.setX(zedX);
        zombie.setY(zedY);
        zombie.setSitAgainstWall(true);
        int wallType = square.getWallType();
        if (wallType != 0) {
            this.sittingDirections.clear();
            if ((wallType & 1) != 0 && (wallType & 4) != 0) {
                this.sittingDirections.add(IsoDirections.SE);
            }

            if ((wallType & 1) != 0 && (wallType & 8) != 0) {
                this.sittingDirections.add(IsoDirections.SW);
            }

            if ((wallType & 2) != 0 && (wallType & 4) != 0) {
                this.sittingDirections.add(IsoDirections.NE);
            }

            if ((wallType & 2) != 0 && (wallType & 8) != 0) {
                this.sittingDirections.add(IsoDirections.NW);
            }

            if ((wallType & 1) != 0) {
                this.sittingDirections.add(IsoDirections.S);
            }

            if ((wallType & 2) != 0) {
                this.sittingDirections.add(IsoDirections.N);
            }

            if ((wallType & 4) != 0) {
                this.sittingDirections.add(IsoDirections.E);
            }

            if ((wallType & 8) != 0) {
                this.sittingDirections.add(IsoDirections.W);
            }

            IsoDirections dir = PZArrayUtil.pickRandom(this.sittingDirections);
            if (GameClient.client) {
                int index = (square.x & 1) + (square.y & 1);
                dir = this.sittingDirections.get(index % this.sittingDirections.size());
            }

            zombie.setDir(dir);
            zombie.setForwardDirection(dir.ToVector());
            if (zombie.getAnimationPlayer() != null) {
                zombie.getAnimationPlayer().setTargetAndCurrentDirection(zombie.getForwardDirectionX(), zombie.getForwardDirectionY());
            }
        }
    }

    private void addZombieMoving(float x, float y, float z, IsoDirections dir, int descriptorID, ZombieStateFlags state, int pathTargetX, int pathTargetY) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        if (sq != null && (sq.solidFloorCached ? sq.solidFloor : sq.TreatAsSolidFloor())) {
            if (!Core.lastStand && !this.playerSpawns.allowZombie(sq)) {
                noise("removed zombie near player spawn " + PZMath.fastfloor(x) + "," + PZMath.fastfloor(y) + "," + PZMath.fastfloor(z));
            } else {
                VirtualZombieManager.instance.choices.clear();
                VirtualZombieManager.instance.choices.add(sq);
                this.clearChunkForReplace(sq.getChunk());
                IsoZombie realZombie = VirtualZombieManager.instance.createRealZombieAlways(descriptorID, dir, false);
                if (realZombie == null) {
                    DebugType.Zombie.debugln("Failed to create moving Zombie.");
                } else {
                    realZombie.setX(x);
                    realZombie.setY(y);
                    if (state.isCrawling()) {
                        realZombie.setCrawler(true);
                        realZombie.setCanWalk(state.isCanWalk());
                        realZombie.setOnFloor(true);
                        realZombie.setFallOnFront(true);
                        realZombie.walkVariant = "ZombieWalk";
                        realZombie.DoZombieStats();
                    }

                    if (state.isInitialized()) {
                        realZombie.setCanCrawlUnderVehicle(state.isCanCrawlUnderVehicle());
                    } else {
                        this.firstTimeLoaded(realZombie, state);
                    }

                    if (Math.abs(pathTargetX - x) > 1.0F || Math.abs(pathTargetY - y) > 1.0F) {
                        realZombie.allowRepathDelay = -1.0F;
                        realZombie.pathToLocation(pathTargetX, pathTargetY, 0);
                    }

                    realZombie.setReanimatedForGrappleOnly(state.isReanimatedForGrappleOnly());
                    DebugType.Zombie.debugln("Created moving Zombie: %s", realZombie);
                }
            }
        } else {
            noise("real -> virtual " + x + "," + y);
            n_addZombie(x, y, z, (byte)dir.ordinal(), descriptorID, state.asInt(), pathTargetX, pathTargetY);
        }
    }

    private void firstTimeLoaded(IsoZombie zombie, ZombieStateFlags state) {
    }

    public void updateThread() {
        n_updateThread();
    }

    public boolean shouldWait() {
        synchronized (MapCollisionData.instance.renderLock) {
            return n_shouldWait();
        }
    }

    public void updateLoadedAreas() {
        if (this.loadedAreas.set()) {
            n_loadedAreas(this.loadedAreas.count, this.loadedAreas.areas, false);
        }

        if (GameServer.server && this.loadedServerCells.set()) {
            n_loadedAreas(this.loadedServerCells.count, this.loadedServerCells.areas, true);
        }
    }

    public void dbgSpawnTimeToZero(int cellX, int cellY) {
        if (!GameClient.client) {
            DebugCommands.n_debugCommand(3, cellX, cellY);
        } else if (GameClient.connection.getRole().hasCapability(Capability.PopmanManage)) {
            PopmanDebugCommandPacket packet = new PopmanDebugCommandPacket();
            packet.setSpawnTimeToZero((short)cellX, (short)cellY);
            if (packet.isConsistent(GameClient.connection)) {
                packet.processClient(null);
            }
        }
    }

    public void dbgClearZombies(int cellX, int cellY) {
        if (!GameClient.client) {
            DebugCommands.n_debugCommand(4, cellX, cellY);
        } else if (GameClient.connection.getRole().hasCapability(Capability.PopmanManage)) {
            PopmanDebugCommandPacket packet = new PopmanDebugCommandPacket();
            packet.setClearZombies((short)cellX, (short)cellY);
            if (packet.isConsistent(GameClient.connection)) {
                packet.processClient(null);
            }
        }
    }

    public void dbgSpawnNow(int cellX, int cellY) {
        if (!GameClient.client) {
            DebugCommands.n_debugCommand(5, cellX, cellY);
        } else if (GameClient.connection.getRole().hasCapability(Capability.PopmanManage)) {
            PopmanDebugCommandPacket packet = new PopmanDebugCommandPacket();
            packet.setSpawnNow((short)cellX, (short)cellY);
            if (packet.isConsistent(GameClient.connection)) {
                packet.processClient(null);
            }
        }
    }

    public void beginSaveRealZombies() {
        if (GameClient.client) {
            DebugType.Zombie.debugln("Client doesn't save Zeds.");
        } else {
            saveLock.lock();

            try {
                this.saveRealZombieHack.clear();

                for (IsoZombie realZombie : IsoWorld.instance.currentCell.getZombieList()) {
                    if (!realZombie.isReanimatedPlayer() && (!GameServer.server || !realZombie.indoorZombie) && !realZombie.isDead()) {
                        this.saveRealZombieHack.add(realZombie);
                    }
                }

                int total = this.saveRealZombieHack.size();
                n_beginSaveRealZombies(total);
                int i = 0;

                while (i < total) {
                    this.writeByteBuffer.clear();
                    int count = 0;

                    while (true) {
                        if (i < total) {
                            int position = this.writeByteBuffer.position();
                            IsoZombie zombie = this.saveRealZombieHack.get(i++);
                            this.writeByteBuffer.putFloat(zombie.getX());
                            this.writeByteBuffer.putFloat(zombie.getY());
                            this.writeByteBuffer.putFloat(zombie.getZ());
                            this.writeByteBuffer.put((byte)zombie.getForwardIsoDirection().ordinal());
                            this.writeByteBuffer.putInt(zombie.getPersistentOutfitID());
                            int state = ZombieStateFlags.intFromZombie(zombie);
                            this.writeByteBuffer.putInt(state);
                            count++;
                            int numBytes = this.writeByteBuffer.position() - position;
                            if (this.writeByteBuffer.position() + numBytes <= this.writeByteBuffer.capacity()) {
                                continue;
                            }
                        }

                        n_saveRealZombies(count, this.writeByteBuffer);
                        break;
                    }
                }

                this.saveRealZombieHack.clear();
            } finally {
                saveLock.unlock();
            }
        }
    }

    public void endSaveRealZombies() {
        if (!GameClient.client) {
            ;
        }
    }

    public void save() {
        if (!GameClient.client) {
            n_save();
        }
    }

    public void stop() {
        if (!GameClient.client) {
            this.stopped = true;
            n_stop();
            this.loadedAreas.clear();
            this.newChunks.clear();
            this.spawnOrigins.clear();
            this.radarXy = null;
            this.radarCount = 0;
            this.radarRenderFlag = false;
            this.radarRequestFlag = false;
            this.zombiesMinPerChunk = 0.0F;
            this.zombiesMaxPerChunk = 255.0F;
        }
    }

    private static final class PendingCellSave {
        final int popmanCellX;
        final int popmanCellY;
        final List<ZombiePopulationManager.ZombieSaveData> aliveZombies;

        PendingCellSave(int x, int y, List<ZombiePopulationManager.ZombieSaveData> zombies) {
            this.popmanCellX = x;
            this.popmanCellY = y;
            this.aliveZombies = zombies;
        }
    }

    private static final class ZombieSaveData {
        final float x;
        final float y;
        final float z;
        final byte dir;
        final int descriptorID;
        final int state;

        ZombieSaveData(IsoZombie zombie) {
            this.x = zombie.getX();
            this.y = zombie.getY();
            this.z = zombie.getZ();
            this.dir = (byte)zombie.getForwardIsoDirection().ordinal();
            this.descriptorID = zombie.getPersistentOutfitID();
            this.state = ZombieStateFlags.intFromZombie(zombie);
        }
    }
}
