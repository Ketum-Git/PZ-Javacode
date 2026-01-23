// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.random.RandLocation;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.erosion.season.ErosionIceQueen;
import zombie.erosion.season.ErosionSeason;
import zombie.erosion.utils.Noise2D;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;

@UsedFromLua
public final class ErosionMain {
    private static ErosionMain instance;
    private ErosionConfig cfg;
    private boolean debug;
    private final IsoSpriteManager sprMngr;
    private ErosionIceQueen iceQueen;
    private boolean isSnow;
    private String gameSaveWorld;
    private String cfgPath;
    private IsoChunk chunk;
    private ErosionData.Chunk chunkModData;
    private Noise2D noiseMain;
    private Noise2D noiseMoisture;
    private Noise2D noiseMinerals;
    private Noise2D noiseKudzu;
    private ErosionWorld world;
    private ErosionSeason season;
    private int tickUnit = 144;
    private int ticks;
    private int eTicks;
    private int day;
    private int month;
    private int year;
    private int epoch;
    private static final int[][] soilTable = new int[][]{
        {1, 1, 1, 1, 1, 4, 4, 4, 4, 4},
        {1, 1, 1, 1, 2, 5, 4, 4, 4, 4},
        {1, 1, 1, 2, 2, 5, 5, 4, 4, 4},
        {1, 1, 2, 2, 3, 6, 5, 5, 4, 4},
        {1, 2, 2, 3, 3, 6, 6, 5, 5, 4},
        {7, 8, 8, 9, 9, 12, 12, 11, 11, 10},
        {7, 7, 8, 8, 9, 12, 11, 11, 10, 10},
        {7, 7, 7, 8, 8, 11, 11, 10, 10, 10},
        {7, 7, 7, 7, 8, 11, 10, 10, 10, 10},
        {7, 7, 7, 7, 7, 10, 10, 10, 10, 10}
    };
    private int snowFrac;
    private int snowFracYesterday;
    private int[] snowFracOnDay;

    public static ErosionMain getInstance() {
        return instance;
    }

    public ErosionMain(IsoSpriteManager _sprMngr, boolean _debug) {
        instance = this;
        this.sprMngr = _sprMngr;
        this.debug = _debug;
        this.start();
    }

    public ErosionConfig getConfig() {
        return this.cfg;
    }

    public ErosionSeason getSeasons() {
        return this.season;
    }

    public int getEtick() {
        return this.eTicks;
    }

    public IsoSpriteManager getSpriteManager() {
        return this.sprMngr;
    }

    public void mainTimer() {
        if (GameClient.client) {
            if (Core.debug) {
                this.cfg.writeFile(this.cfgPath);
            }
        } else {
            int ErosionDays = SandboxOptions.instance.erosionDays.getValue();
            if (this.debug) {
                this.eTicks++;
            } else if (ErosionDays < 0) {
                this.eTicks = 0;
            } else if (ErosionDays > 0) {
                this.ticks++;
                this.eTicks = (int)(this.ticks / 144.0F / ErosionDays * 100.0F);
            } else {
                this.ticks++;
                if (this.ticks >= this.tickUnit) {
                    this.ticks = 0;
                    this.eTicks++;
                }
            }

            if (this.eTicks < 0) {
                this.eTicks = Integer.MAX_VALUE;
            }

            GameTime gameTime = GameTime.getInstance();
            if (gameTime.getDay() != this.day || gameTime.getMonth() != this.month || gameTime.getYear() != this.year) {
                this.month = gameTime.getMonth();
                this.year = gameTime.getYear();
                this.day = gameTime.getDay();
                this.epoch++;
                this.season.setDay(this.day, this.month, this.year);
                this.snowCheck();
            }

            if (GameServer.server) {
                for (int i = 0; i < ServerMap.instance.loadedCells.size(); i++) {
                    ServerMap.ServerCell cell = ServerMap.instance.loadedCells.get(i);
                    if (cell.isLoaded) {
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                IsoChunk chunk = cell.chunks[x][y];
                                if (chunk != null) {
                                    ErosionData.Chunk chunkData = chunk.getErosionData();
                                    if (chunkData.eTickStamp != this.eTicks || chunkData.epoch != this.epoch) {
                                        for (int yy = 0; yy < 8; yy++) {
                                            for (int xx = 0; xx < 8; xx++) {
                                                IsoGridSquare sq = chunk.getGridSquare(xx, yy, 0);
                                                if (sq != null) {
                                                    this.loadGridsquare(sq);
                                                }
                                            }
                                        }

                                        chunkData.eTickStamp = this.eTicks;
                                        chunkData.epoch = this.epoch;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.cfg.time.ticks = this.ticks;
            this.cfg.time.eticks = this.eTicks;
            this.cfg.time.epoch = this.epoch;
            this.cfg.writeFile(this.cfgPath);
        }
    }

    public void snowCheck() {
    }

    public int getSnowFraction() {
        return this.snowFrac;
    }

    public int getSnowFractionYesterday() {
        return this.snowFracYesterday;
    }

    public boolean isSnow() {
        return this.isSnow;
    }

    public void sendState(ByteBuffer bb) {
        if (GameServer.server) {
            bb.putInt(this.eTicks);
            bb.putInt(this.ticks);
            bb.putInt(this.epoch);
            bb.put((byte)this.getSnowFraction());
            bb.put((byte)this.getSnowFractionYesterday());
            bb.putFloat(GameTime.getInstance().getTimeOfDay());
        }
    }

    public void receiveState(ByteBuffer bb) {
        if (GameClient.client) {
            int oldTicks = this.eTicks;
            int oldEpoch = this.epoch;
            this.eTicks = bb.getInt();
            this.ticks = bb.getInt();
            this.epoch = bb.getInt();
            this.cfg.time.ticks = this.ticks;
            this.cfg.time.eticks = this.eTicks;
            this.cfg.time.epoch = this.epoch;
            int snowFrac = bb.get();
            int snowFracYesterday = bb.get();
            float snowTimeOfDay = bb.getFloat();
            GameTime gameTime = GameTime.getInstance();
            if (gameTime.getDay() != this.day || gameTime.getMonth() != this.month || gameTime.getYear() != this.year) {
                this.month = gameTime.getMonth();
                this.year = gameTime.getYear();
                this.day = gameTime.getDay();
                this.season.setDay(this.day, this.month, this.year);
            }

            if (oldTicks != this.eTicks || oldEpoch != this.epoch) {
                this.updateMapNow();
            }
        }
    }

    private void loadGridsquare(IsoGridSquare _sq) {
        if (_sq != null && _sq.chunk != null && _sq.getZ() == 0) {
            this.getChunk(_sq);
            ErosionData.Square erosionModData = _sq.getErosionData();
            if (!erosionModData.init) {
                this.initGridSquare(_sq, erosionModData);
                this.world.validateSpawn(_sq, erosionModData, this.chunkModData);
            }

            if (erosionModData.doNothing) {
                return;
            }

            if (this.chunkModData.eTickStamp >= this.eTicks && this.chunkModData.epoch == this.epoch) {
                return;
            }

            this.world.update(_sq, erosionModData, this.chunkModData, this.eTicks);
        }
    }

    private void initGridSquare(IsoGridSquare _sq, ErosionData.Square _erosionModData) {
        int sqx = _sq.getX();
        int sqy = _sq.getY();
        float noise = this.noiseMain.layeredNoise(sqx / 10.0F, sqy / 10.0F);
        _erosionModData.noiseMainByte = Bits.packFloatUnitToByte(noise);
        _erosionModData.noiseMain = noise;
        _erosionModData.noiseMainInt = (int)Math.floor(_erosionModData.noiseMain * 100.0F);
        _erosionModData.noiseKudzu = this.noiseKudzu.layeredNoise(sqx / 10.0F, sqy / 10.0F);
        _erosionModData.soil = this.chunkModData.soil;
        _erosionModData.rand = new RandLocation(sqx, sqy);
        float magic = _erosionModData.rand(sqx, sqy, 100) / 100.0F;
        _erosionModData.magicNumByte = Bits.packFloatUnitToByte(magic);
        _erosionModData.magicNum = magic;
        _erosionModData.regions.clear();
        _erosionModData.init = true;
    }

    private void getChunk(IsoGridSquare _sq) {
        this.chunk = _sq.getChunk();
        this.chunkModData = this.chunk.getErosionData();
        if (!this.chunkModData.init) {
            this.initChunk(this.chunk, this.chunkModData);
        }
    }

    private void initChunk(IsoChunk _chunk, ErosionData.Chunk _chunkModData) {
        _chunkModData.set(_chunk);
        float nx = _chunkModData.x / 5.0F;
        float ny = _chunkModData.y / 5.0F;
        float moisture = this.noiseMoisture.layeredNoise(nx, ny);
        float minerals = this.noiseMinerals.layeredNoise(nx, ny);
        int moi = moisture < 1.0F ? (int)Math.floor(moisture * 10.0F) : 9;
        int min = minerals < 1.0F ? (int)Math.floor(minerals * 10.0F) : 9;
        _chunkModData.init = true;
        _chunkModData.eTickStamp = -1;
        _chunkModData.epoch = -1;
        _chunkModData.moisture = moisture;
        _chunkModData.minerals = minerals;
        _chunkModData.soil = soilTable[moi][min] - 1;
    }

    private boolean initConfig() {
        String cfgName = "erosion.ini";
        if (GameClient.client) {
            this.cfg = GameClient.instance.erosionConfig;

            assert this.cfg != null;

            GameClient.instance.erosionConfig = null;
            this.cfgPath = ZomboidFileSystem.instance.getFileNameInCurrentSave("erosion.ini");
            return true;
        } else {
            this.cfg = new ErosionConfig();
            this.cfgPath = ZomboidFileSystem.instance.getFileNameInCurrentSave("erosion.ini");
            File cfgFile = new File(this.cfgPath);
            if (cfgFile.exists()) {
                DebugLog.DetailedInfo.trace("erosion: reading " + cfgFile.getAbsolutePath());
                if (this.cfg.readFile(cfgFile.getAbsolutePath())) {
                    return true;
                }

                this.cfg = new ErosionConfig();
            }

            cfgFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "erosion.ini");
            if (!cfgFile.exists() && !Core.getInstance().isNoSave()) {
                File cfgFileSrc = ZomboidFileSystem.instance.getMediaFile("data" + File.separator + "erosion.ini");
                if (cfgFileSrc.exists()) {
                    try {
                        DebugLog.DetailedInfo.trace("erosion: copying " + cfgFileSrc.getAbsolutePath() + " to " + cfgFile.getAbsolutePath());
                        Files.copy(cfgFileSrc.toPath(), cfgFile.toPath());
                    } catch (Exception var7) {
                        var7.printStackTrace();
                    }
                }
            }

            if (cfgFile.exists()) {
                DebugLog.DetailedInfo.trace("erosion: reading " + cfgFile.getAbsolutePath());
                if (!this.cfg.readFile(cfgFile.getAbsolutePath())) {
                    this.cfg = new ErosionConfig();
                }
            }

            int ErosionSpeed = SandboxOptions.instance.getErosionSpeed();
            switch (ErosionSpeed) {
                case 1:
                    this.cfg.time.tickunit /= 5;
                    break;
                case 2:
                    this.cfg.time.tickunit /= 2;
                case 3:
                default:
                    break;
                case 4:
                    this.cfg.time.tickunit *= 2;
                    break;
                case 5:
                    this.cfg.time.tickunit *= 5;
            }

            float daysTill100Percent = this.cfg.time.tickunit * 100 / 144.0F;
            float daysSinceApo = (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
            this.cfg.time.eticks = (int)Math.floor(Math.min(1.0F, daysSinceApo / daysTill100Percent) * 100.0F);
            int ErosionDays = SandboxOptions.instance.erosionDays.getValue();
            if (ErosionDays > 0) {
                this.cfg.time.tickunit = 144;
                this.cfg.time.eticks = (int)Math.floor(Math.min(1.0F, daysSinceApo / ErosionDays) * 100.0F);
            }

            return true;
        }
    }

    public void start() {
        if (this.initConfig()) {
            this.gameSaveWorld = Core.gameSaveWorld;
            this.tickUnit = this.cfg.time.tickunit;
            this.ticks = this.cfg.time.ticks;
            this.eTicks = this.cfg.time.eticks;
            this.month = GameTime.getInstance().getMonth();
            this.year = GameTime.getInstance().getYear();
            this.day = GameTime.getInstance().getDay();
            this.debug = !GameServer.server && this.cfg.debug.enabled;
            this.cfg.consolePrint();
            this.noiseMain = new Noise2D();
            this.noiseMain.addLayer(this.cfg.seeds.seedMain0, 0.5F, 3.0F);
            this.noiseMain.addLayer(this.cfg.seeds.seedMain1, 2.0F, 5.0F);
            this.noiseMain.addLayer(this.cfg.seeds.seedMain2, 5.0F, 8.0F);
            this.noiseMoisture = new Noise2D();
            this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture0, 2.0F, 3.0F);
            this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture1, 1.6F, 5.0F);
            this.noiseMoisture.addLayer(this.cfg.seeds.seedMoisture2, 0.6F, 8.0F);
            this.noiseMinerals = new Noise2D();
            this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals0, 2.0F, 3.0F);
            this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals1, 1.6F, 5.0F);
            this.noiseMinerals.addLayer(this.cfg.seeds.seedMinerals2, 0.6F, 8.0F);
            this.noiseKudzu = new Noise2D();
            this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu0, 6.0F, 3.0F);
            this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu1, 3.0F, 5.0F);
            this.noiseKudzu.addLayer(this.cfg.seeds.seedKudzu2, 0.5F, 8.0F);
            this.season = new ErosionSeason();
            ErosionConfig.Season sc = this.cfg.season;
            int tempMin = sc.tempMin;
            int tempMax = sc.tempMax;
            if (SandboxOptions.instance.getTemperatureModifier() == 1) {
                tempMin -= 10;
                tempMax -= 10;
            } else if (SandboxOptions.instance.getTemperatureModifier() == 2) {
                tempMin -= 5;
                tempMax -= 5;
            } else if (SandboxOptions.instance.getTemperatureModifier() == 4) {
                tempMin = (int)(tempMin + 7.5);
                tempMax += 4;
            } else if (SandboxOptions.instance.getTemperatureModifier() == 5) {
                tempMin += 15;
                tempMax += 8;
            }

            this.season.init(sc.lat, tempMax, tempMin, sc.tempDiff, sc.seasonLag, sc.noon, sc.seedA, sc.seedB, sc.seedC);
            this.season.setRain(sc.jan, sc.feb, sc.mar, sc.apr, sc.may, sc.jun, sc.jul, sc.aug, sc.sep, sc.oct, sc.nov, sc.dec);
            this.season.setDay(this.day, this.month, this.year);
            LuaEventManager.triggerEvent("OnInitSeasons", this.season);
            this.iceQueen = new ErosionIceQueen(this.sprMngr);
            this.world = new ErosionWorld();
            if (this.world.init()) {
                this.snowCheck();
            }
        }
    }

    private void loadChunk(IsoChunk _chunk) {
        ErosionData.Chunk chunkModData = _chunk.getErosionData();
        if (!chunkModData.init) {
            this.initChunk(_chunk, chunkModData);
        }

        chunkModData.eTickStamp = this.eTicks;
        chunkModData.epoch = this.epoch;
    }

    public void DebugUpdateMapNow() {
        this.updateMapNow();
    }

    private void updateMapNow() {
        for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
            IsoChunkMap cm = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
            if (!cm.ignore) {
                IsoChunkMap.bSettingChunk.lock();

                try {
                    for (int y = 0; y < IsoChunkMap.chunkGridWidth; y++) {
                        for (int x = 0; x < IsoChunkMap.chunkGridWidth; x++) {
                            IsoChunk chunk = cm.getChunk(x, y);
                            if (chunk != null) {
                                ErosionData.Chunk chunkData = chunk.getErosionData();
                                if (chunkData.eTickStamp != this.eTicks || chunkData.epoch != this.epoch) {
                                    for (int yy = 0; yy < 8; yy++) {
                                        for (int xx = 0; xx < 8; xx++) {
                                            IsoGridSquare sq = chunk.getGridSquare(xx, yy, 0);
                                            if (sq != null) {
                                                this.loadGridsquare(sq);
                                            }
                                        }
                                    }

                                    chunkData.eTickStamp = this.eTicks;
                                    chunkData.epoch = this.epoch;
                                }
                            }
                        }
                    }
                } finally {
                    IsoChunkMap.bSettingChunk.unlock();
                }
            }
        }
    }

    public static void LoadGridsquare(IsoGridSquare _sq) {
        instance.loadGridsquare(_sq);
    }

    public static void ChunkLoaded(IsoChunk _chunk) {
        instance.loadChunk(_chunk);
    }

    public static void EveryTenMinutes() {
        instance.mainTimer();
    }

    public static void Reset() {
        instance = null;
    }
}
