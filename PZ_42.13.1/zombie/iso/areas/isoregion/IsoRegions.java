// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.data.DataRoot;
import zombie.iso.areas.isoregion.data.DataSquarePos;
import zombie.iso.areas.isoregion.regions.IChunkRegion;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.network.GameClient;
import zombie.network.GameServer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class IsoRegions {
    public static final int SINGLE_CHUNK_PACKET_SIZE = 2076;
    public static final int CHUNKS_DATA_PACKET_SIZE = 65536;
    public static boolean printD;
    public static final int CELL_DIM = 256;
    public static final int CELL_CHUNK_DIM = 32;
    public static final int CHUNK_DIM = 8;
    public static final int CHUNK_MAX_Z = 32;
    public static final byte BIT_EMPTY = 0;
    public static final byte BIT_WALL_N = 1;
    public static final byte BIT_WALL_W = 2;
    public static final byte BIT_PATH_WALL_N = 4;
    public static final byte BIT_PATH_WALL_W = 8;
    public static final byte BIT_HAS_FLOOR = 16;
    public static final byte BIT_STAIRCASE = 32;
    public static final byte BIT_HAS_ROOF = 64;
    public static final byte DIR_NONE = -1;
    public static final byte DIR_N = 0;
    public static final byte DIR_W = 1;
    public static final byte DIR_2D_NW = 2;
    public static final byte DIR_S = 2;
    public static final byte DIR_E = 3;
    public static final byte DIR_2D_MAX = 4;
    public static final byte DIR_TOP = 4;
    public static final byte DIR_BOT = 5;
    public static final byte DIR_MAX = 6;
    protected static final int CHUNK_LOAD_DIMENSIONS = 7;
    protected static boolean debugLoadAllChunks;
    public static final String FILE_PRE = "datachunk_";
    public static final String FILE_SEP = "_";
    public static final String FILE_EXT = ".bin";
    public static final String FILE_DIR = "isoregiondata";
    private static final int SQUARE_CHANGE_WARN_THRESHOLD = 20;
    private static int squareChangePerTick;
    private static String cacheDir;
    private static File cacheDirFile;
    private static File headDataFile;
    private static final Map<Integer, File> chunkFileNames = new HashMap<>();
    private static IsoRegionWorker regionWorker;
    private static DataRoot dataRoot;
    private static IsoRegionsLogger logger;
    protected static int lastChunkX = -1;
    protected static int lastChunkY = -1;
    private static byte previousFlags = 0;

    public static File getHeaderFile() {
        return headDataFile;
    }

    public static File getDirectory() {
        return cacheDirFile;
    }

    public static File getChunkFile(int chunkX, int chunkY) {
        int hashID = hash(chunkX, chunkY);
        if (chunkFileNames.containsKey(hashID)) {
            File f = chunkFileNames.get(hashID);
            if (f != null) {
                return chunkFileNames.get(hashID);
            }
        }

        String filename = cacheDir + "datachunk_" + chunkX + "_" + chunkY + ".bin";
        File f = new File(filename);
        chunkFileNames.put(hashID, f);
        return f;
    }

    public static byte GetOppositeDir(byte dir) {
        if (dir == 0) {
            return 2;
        } else if (dir == 1) {
            return 3;
        } else if (dir == 2) {
            return 0;
        } else if (dir == 3) {
            return 1;
        } else if (dir == 4) {
            return 5;
        } else {
            return (byte)(dir == 5 ? 4 : -1);
        }
    }

    public static void setDebugLoadAllChunks(boolean b) {
        debugLoadAllChunks = b;
    }

    public static boolean isDebugLoadAllChunks() {
        return debugLoadAllChunks;
    }

    public static int hash(int x, int y) {
        return y << 16 ^ x;
    }

    protected static DataRoot getDataRoot() {
        return dataRoot;
    }

    public static void init() {
        if (!Core.debug) {
            printD = false;
            DataSquarePos.debugPool = false;
        }

        logger = new IsoRegionsLogger(printD);
        chunkFileNames.clear();
        cacheDir = ZomboidFileSystem.instance.getFileNameInCurrentSave("isoregiondata") + File.separator;
        cacheDirFile = new File(cacheDir);
        if (!cacheDirFile.exists()) {
            cacheDirFile.mkdir();
        }

        String filename = cacheDir + "RegionHeader.bin";
        headDataFile = new File(filename);
        previousFlags = 0;
        dataRoot = new DataRoot();
        regionWorker = new IsoRegionWorker();
        regionWorker.create();
        regionWorker.load();
    }

    public static IsoRegionsLogger getLogger() {
        return logger;
    }

    public static void log(String str) {
        logger.log(str);
    }

    public static void log(String str, Color col) {
        logger.log(str, col);
    }

    public static void warn(String str) {
        logger.warn(str);
    }

    public static void reset() {
        previousFlags = 0;
        regionWorker.stop();
        regionWorker = null;
        dataRoot = null;
        chunkFileNames.clear();
    }

    public static void receiveServerUpdatePacket(ByteBuffer input) {
        if (regionWorker == null) {
            logger.warn("IsoRegion cannot receive server packet, regionWorker == null.");
        } else {
            if (GameClient.client) {
                regionWorker.readServerUpdatePacket(input);
            }
        }
    }

    public static void receiveClientRequestFullDataChunks(ByteBuffer input, UdpConnection conn) {
        if (regionWorker == null) {
            logger.warn("IsoRegion cannot receive client packet, regionWorker == null.");
        } else {
            if (GameServer.server) {
                regionWorker.readClientRequestFullUpdatePacket(input, conn);
            }
        }
    }

    public static void update() {
        if (Core.debug && squareChangePerTick > 20) {
            logger.warn("IsoRegion Warning -> " + squareChangePerTick + " squares have been changed in one tick.");
        }

        squareChangePerTick = 0;
        if (IsoRegionWorker.isRequestingBufferSwap.get()) {
            logger.log("IsoRegion Swapping DataRoot");
            DataRoot root = dataRoot;
            dataRoot = regionWorker.getRootBuffer();
            regionWorker.setRootBuffer(root);
            IsoRegionWorker.isRequestingBufferSwap.set(false);
            if (!GameServer.server) {
                clientResetCachedRegionReferences();
                dataRoot.clientProcessBuildings();
            }
        }

        if (!GameClient.client && !GameServer.server && debugLoadAllChunks && Core.debug) {
            int cx = PZMath.fastfloor(IsoPlayer.getInstance().getX()) / 8;
            int cy = PZMath.fastfloor(IsoPlayer.getInstance().getY()) / 8;
            if (lastChunkX != cx || lastChunkY != cy) {
                lastChunkX = cx;
                lastChunkY = cy;
                regionWorker.readSurroundingChunks(cx, cy, IsoChunkMap.chunkGridWidth - 2, true);
            }
        }

        regionWorker.update();
        logger.update();
    }

    protected static void forceRecalcSurroundingChunks() {
        if (Core.debug && !GameClient.client) {
            logger.log("[DEBUG] Forcing a full load/recalculate of chunks surrounding player.", Colors.Gold);
            int cx = PZMath.fastfloor(IsoPlayer.getInstance().getX()) / 8;
            int cy = PZMath.fastfloor(IsoPlayer.getInstance().getY()) / 8;
            regionWorker.readSurroundingChunks(cx, cy, IsoChunkMap.chunkGridWidth - 2, true, true);
        }
    }

    public static byte getSquareFlags(int x, int y, int z) {
        return dataRoot.getSquareFlags(x, y, z);
    }

    /**
     * Returns a IWorldRegion for the square.
     *  Note: Returned objects from this function should not be retained as the DataRoot may get swapped.
     *  Note: The IWorldRegion does get cached in IsoGridSquare for optimizing purposes but this gets handled in 'clientResetCachedRegionReferences()'
     * @return can be null.
     */
    public static IWorldRegion getIsoWorldRegion(int x, int y, int z) {
        return dataRoot.getIsoWorldRegion(x, y, z);
    }

    public static List<IsoWorldRegion> getIsoWorldRegionsInCell(int cellX, int cellY, ArrayList<IsoWorldRegion> worldRegions) {
        return getDataRoot().getIsoWorldRegionsInCell(cellX, cellY, worldRegions);
    }

    /**
     * Returns a DataChunk for the square.
     *  Note: Returned objects from this function should not be retained as the DataRoot may get swapped.
     * @return can be null.
     */
    public static DataChunk getDataChunk(int chunkx, int chunky) {
        return dataRoot.getDataChunk(chunkx, chunky);
    }

    /**
     * Returns a IChunkRegion for the square.
     *  Note: Returned objects from this function should not be retained as the DataRoot may get swapped.
     * @return can be null.
     */
    public static IChunkRegion getChunkRegion(int x, int y, int z) {
        return dataRoot.getIsoChunkRegion(x, y, z);
    }

    public static void ResetAllDataDebug() {
        if (Core.debug) {
            if (!GameServer.server && !GameClient.client) {
                regionWorker.addDebugResetJob();
            }
        }
    }

    private static void clientResetCachedRegionReferences() {
        if (!GameServer.server) {
            int chunkMinX = 0;
            int chunkMinY = 0;
            int chunkMaxX = IsoChunkMap.chunkGridWidth;
            int chunkMaxY = IsoChunkMap.chunkGridWidth;

            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                IsoChunkMap cm = IsoWorld.instance.getCell().getChunkMap(playerIndex);
                if (cm == null || cm.ignore) {
                    return;
                }

                for (int xx = 0; xx < chunkMaxX; xx++) {
                    for (int yy = 0; yy < chunkMaxY; yy++) {
                        IsoChunk c = cm.getChunk(xx, yy);
                        if (c != null) {
                            for (int z = 0; z < c.squares.length; z++) {
                                for (int p = 0; p < c.squares[0].length; p++) {
                                    IsoGridSquare sq = c.squares[z][p];
                                    if (sq != null) {
                                        sq.setIsoWorldRegion(null);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Needs to be called before a player manipulates the grid.
     *  Records bitFlags for the state of the square that are compared to bitFlags for the state of the square after manipulation to detect relevant changes.
     */
    public static void setPreviousFlags(IsoGridSquare gs) {
        previousFlags = calculateSquareFlags(gs);
    }

    /**
     * Called after the grid has been manipulated by a player.
     *  NOTE: setPreviousFlags needs to be called prior to the grid being manipulated by a player.
     */
    public static void squareChanged(IsoGridSquare gs) {
        squareChanged(gs, false);
    }

    /**
     * Called after the grid has been manipulated by a player.
     *  NOTE: setPreviousFlags needs to be called prior to the grid being manipulated by a player.
     */
    public static void squareChanged(IsoGridSquare gs, boolean isRemoval) {
        if (!GameClient.client) {
            if (gs != null) {
                byte flags = calculateSquareFlags(gs);
                if (flags != previousFlags) {
                    regionWorker.addSquareChangedJob(gs.getX(), gs.getY(), gs.getZ(), isRemoval, flags);
                    squareChangePerTick++;
                    previousFlags = 0;
                }
            }
        }
    }

    protected static byte calculateSquareFlags(IsoGridSquare gs) {
        int flags = 0;
        if (gs != null) {
            if (gs.has(IsoFlagType.solidfloor)) {
                flags |= 16;
            }

            if (gs.has(IsoFlagType.cutN) || gs.has(IsoObjectType.doorFrN)) {
                flags |= 1;
                if (gs.has(IsoFlagType.WindowN) || gs.has(IsoFlagType.windowN) || gs.has(IsoFlagType.DoorWallN)) {
                    flags |= 4;
                }
            }

            if (!gs.has(IsoFlagType.WallSE) && (gs.has(IsoFlagType.cutW) || gs.has(IsoObjectType.doorFrW))) {
                flags |= 2;
                if (gs.has(IsoFlagType.WindowW) || gs.has(IsoFlagType.windowW) || gs.has(IsoFlagType.DoorWallW)) {
                    flags |= 8;
                }
            }

            if (gs.HasStairsNorth() || gs.HasStairsWest()) {
                flags |= 32;
            }
        }

        return (byte)flags;
    }

    protected static IsoRegionWorker getRegionWorker() {
        return regionWorker;
    }
}
