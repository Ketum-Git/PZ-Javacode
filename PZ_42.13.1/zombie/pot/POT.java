// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import zombie.asset.AssetPath;
import zombie.core.math.PZMath;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.iso.BuildingDef;
import zombie.iso.NewMapBinaryFile;
import zombie.iso.SliceY;
import zombie.worldMap.WorldMapBinary;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapDataAssetManager;
import zombie.worldMap.WorldMapFeature;

public final class POT {
    String mapDirectoryIn;
    String mapDirectoryOut;
    int minX;
    int minY;
    int maxX;
    int maxY;
    final TIntObjectHashMap<File> lotHeaderFiles = new TIntObjectHashMap<>();
    final TIntObjectHashMap<File> lotPackFiles = new TIntObjectHashMap<>();
    final TIntObjectHashMap<File> chunkDataFiles = new TIntObjectHashMap<>();
    final byte[] zombieDensityPerSquare = new byte[65536];
    final TIntObjectHashMap<POTLotHeader> newLotHeader = new TIntObjectHashMap<>();
    final TIntObjectHashMap<POTLotHeader> oldLotHeader = new TIntObjectHashMap<>();
    final TIntObjectHashMap<POTLotPack> oldLotPack = new TIntObjectHashMap<>();
    final TIntObjectHashMap<POTChunkData> oldChunkData = new TIntObjectHashMap<>();
    final TIntArrayList onlyTheseCells = new TIntArrayList();
    public static final int CHUNK_DIM_OLD = 10;
    public static final int CHUNK_PER_CELL_OLD = 30;
    public static final int CELL_DIM_OLD = 300;
    public static final int CHUNK_DIM_NEW = 8;
    public static final int CHUNK_PER_CELL_NEW = 32;
    public static final int CELL_DIM_NEW = 256;
    static final int LEVELS = 64;

    public void convertMapDirectory(String mapDirectoryIn, String mapDirectoryOut) throws Exception {
        Files.createDirectories(Paths.get(mapDirectoryOut));
        this.mapDirectoryIn = mapDirectoryIn;
        this.mapDirectoryOut = mapDirectoryOut;
        this.readFileNames();
        this.convertLotHeaders();
        this.convertLotPack();
        this.convertChunkData();
        if (this.onlyTheseCells.isEmpty()) {
            this.convertObjectsLua();
            this.convertSpawnPointsLua();
            this.convertWorldMapBIN("worldmap.xml.bin");
            this.convertWorldMapBIN("worldmap-forest.xml.bin");
            this.convertWorldMapXML();
        }
    }

    boolean shouldIgnoreCell(int oldCellX, int oldCellY) {
        if (this.onlyTheseCells.isEmpty()) {
            return false;
        } else {
            for (int i = 0; i < this.onlyTheseCells.size(); i += 2) {
                int oldCellX1 = this.onlyTheseCells.get(i);
                int oldCellY1 = this.onlyTheseCells.get(i + 1);
                if (oldCellX >= oldCellX1 - 1 && oldCellX <= oldCellX1 + 1 && oldCellY >= oldCellY1 - 1 && oldCellY <= oldCellY1 + 1) {
                    return false;
                }
            }

            return true;
        }
    }

    boolean shouldConvertNewCell(int newCellX, int newCellY) {
        if (this.onlyTheseCells.isEmpty()) {
            return true;
        } else {
            int oldCellMinX = newCellX * 256 / 300;
            int oldCellMinY = newCellY * 256 / 300;
            int oldCellMaxX = ((newCellX + 1) * 256 - 1) / 300;
            int oldCellMaxY = ((newCellY + 1) * 256 - 1) / 300;

            for (int y = oldCellMinY; y <= oldCellMaxY; y++) {
                for (int x = oldCellMinX; x <= oldCellMaxX; x++) {
                    for (int i = 0; i < this.onlyTheseCells.size(); i += 2) {
                        int oldCellX1 = this.onlyTheseCells.get(i);
                        int oldCellY1 = this.onlyTheseCells.get(i + 1);
                        if (x == oldCellX1 && y == oldCellY1) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    void readFileNames() {
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
        File file = new File(this.mapDirectoryIn);
        File[] files = file.listFiles();

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            String suffix = fileName.substring(fileName.lastIndexOf(46));
            fileName = fileName.substring(0, fileName.lastIndexOf(46));
            if (".lotheader".equals(suffix)) {
                String[] split = fileName.split("_");
                int x = Integer.parseInt(split[0]);
                int y = Integer.parseInt(split[1]);
                if (!this.shouldIgnoreCell(x, y)) {
                    this.minX = PZMath.min(this.minX, x);
                    this.minY = PZMath.min(this.minY, y);
                    this.maxX = PZMath.max(this.maxX, x);
                    this.maxY = PZMath.max(this.maxY, y);
                    int index = x + y * 1000;
                    this.lotHeaderFiles.put(index, files[i]);
                }
            } else if (".lotpack".equals(suffix)) {
                String[] split = fileName.replace("world_", "").split("_");
                int x = Integer.parseInt(split[0]);
                int y = Integer.parseInt(split[1]);
                if (!this.shouldIgnoreCell(x, y)) {
                    int index = x + y * 1000;
                    this.lotPackFiles.put(index, files[i]);
                }
            } else if (fileName.startsWith("chunkdata_")) {
                String[] split = fileName.replace("chunkdata_", "").split("_");
                int x = Integer.parseInt(split[0]);
                int y = Integer.parseInt(split[1]);
                if (!this.shouldIgnoreCell(x, y)) {
                    int index = x + y * 1000;
                    this.chunkDataFiles.put(index, files[i]);
                }
            }
        }
    }

    void convertLotHeaders() {
        for (int y = this.minY * 300; y < (this.maxY + 1) * 300; y += 256) {
            for (int x = this.minX * 300; x <= (this.maxX + 1) * 300; x += 256) {
                int newCellX = x / 256;
                int newCellY = y / 256;
                if (this.shouldConvertNewCell(newCellX, newCellY)) {
                    this.convertLotHeader(newCellX, newCellY);
                }
            }
        }
    }

    void convertLotHeader(int newCellX, int newCellY) {
        POTLotHeader newLotHeader = new POTLotHeader(newCellX, newCellY, true);
        int oldCellMinX = newCellX * 256 / 300;
        int oldCellMinY = newCellY * 256 / 300;
        int oldCellMaxX = ((newCellX + 1) * 256 - 1) / 300;
        int oldCellMaxY = ((newCellY + 1) * 256 - 1) / 300;
        Arrays.fill(this.zombieDensityPerSquare, (byte)0);

        for (int oldCellY = oldCellMinY; oldCellY <= oldCellMaxY; oldCellY++) {
            for (int oldCellX = oldCellMinX; oldCellX <= oldCellMaxX; oldCellX++) {
                POTLotHeader oldLotHeader = this.getOldLotHeader(oldCellX, oldCellY);
                if (oldLotHeader != null) {
                    for (BuildingDef buildingDef : oldLotHeader.buildings) {
                        if (newLotHeader.containsSquare(buildingDef.x, buildingDef.y)) {
                            newLotHeader.addBuilding(buildingDef);
                        }
                    }

                    for (int y = 0; y < 256; y++) {
                        for (int x = 0; x < 256; x++) {
                            this.zombieDensityPerSquare[x + y * 256] = oldLotHeader.getZombieDensityForSquare(oldCellX * 300 + x, oldCellY * 300 + y);
                        }
                    }
                }
            }
        }

        newLotHeader.setZombieDensity(this.zombieDensityPerSquare);
        int index = newCellX + newCellY * 1000;
        this.newLotHeader.put(index, newLotHeader);
    }

    POTLotHeader getNewLotHeader(int newCellX, int newCellY) {
        int index = newCellX + newCellY * 1000;
        POTLotHeader lotHeader = this.newLotHeader.get(index);
        if (lotHeader == null) {
            lotHeader = new POTLotHeader(newCellX, newCellY, true);
            this.newLotHeader.put(index, lotHeader);
        }

        return lotHeader;
    }

    POTLotHeader getOldLotHeader(int oldCellX, int oldCellY) {
        int index = oldCellX + oldCellY * 1000;
        File file = this.lotHeaderFiles.get(index);
        if (file == null) {
            return null;
        } else {
            POTLotHeader lotHeader = this.oldLotHeader.get(index);
            if (lotHeader == null) {
                lotHeader = new POTLotHeader(oldCellX, oldCellY, false);
                lotHeader.load(file);
                this.oldLotHeader.put(index, lotHeader);
            }

            return lotHeader;
        }
    }

    POTLotPack getOldLotPack(POTLotHeader oldLotHeader) throws IOException {
        int index = oldLotHeader.x + oldLotHeader.y * 1000;
        POTLotPack lotPack = this.oldLotPack.get(index);
        if (lotPack == null) {
            lotPack = new POTLotPack(oldLotHeader);
            File file = this.lotPackFiles.get(index);
            lotPack.load(file);
            this.oldLotPack.put(index, lotPack);
        }

        return lotPack;
    }

    void convertLotPack() throws IOException {
        for (int y = this.minY * 300; y < (this.maxY + 1) * 300; y += 256) {
            for (int x = this.minX * 300; x < (this.maxX + 1) * 300; x += 256) {
                int newCellX = x / 256;
                int newCellY = y / 256;
                if (this.shouldConvertNewCell(newCellX, newCellY)) {
                    if (newCellY == 30) {
                        boolean oldCellX = true;
                    }

                    this.convertLotPack(newCellX, newCellY);
                    int oldCellX = x / 300 - 1;
                    int oldCellY = y / 300;

                    for (int y2 = this.minY; y2 <= this.maxY; y2++) {
                        for (int x2 = this.minX; x2 <= this.maxX && (x2 != oldCellX || y2 != oldCellY); x2++) {
                            POTLotPack lotPack = this.oldLotPack.remove(x2 + y2 * 1000);
                            if (lotPack != null) {
                                lotPack.clear();
                            }

                            POTLotHeader lotHeader = this.oldLotHeader.remove(x2 + y2 * 1000);
                            if (lotHeader != null) {
                                lotHeader.clear();
                            }
                        }
                    }
                }
            }
        }
    }

    void convertLotPack(int newCellX, int newCellY) throws IOException {
        POTLotHeader newLotHeader = this.getNewLotHeader(newCellX, newCellY);
        if (newLotHeader != null) {
            newLotHeader.minLevelNotEmpty = 1000;
            newLotHeader.maxLevelNotEmpty = -1000;
            POTLotPack newLotPack = new POTLotPack(newLotHeader);

            for (int z = -32; z <= 31; z++) {
                int y = newLotHeader.getMinSquareY();

                for (int y2 = newLotHeader.getMaxSquareY(); y <= y2; y++) {
                    int x = newLotHeader.getMinSquareX();

                    for (int x2 = newLotHeader.getMaxSquareX(); x <= x2; x++) {
                        newLotPack.setSquareData(x, y, z, this.getOldLotPackSquareData(x, y, z));
                    }
                }
            }

            newLotHeader.save(String.format("%s%s%d_%d.lotheader", this.mapDirectoryOut, File.separator, newLotHeader.x, newLotHeader.y));
            newLotPack.save(String.format("%s%sworld_%d_%d.lotpack", this.mapDirectoryOut, File.separator, newLotPack.x, newLotPack.y));
            this.newLotHeader.remove(newCellX + newCellY * 1000);
            newLotHeader.clear();
            newLotPack.clear();
        }
    }

    String[] getOldLotPackSquareData(int squareX, int squareY, int z) throws IOException {
        POTLotHeader oldLotHeader = this.getOldLotHeader(squareX / 300, squareY / 300);
        if (oldLotHeader == null) {
            return null;
        } else if (!oldLotHeader.containsSquare(squareX, squareY)) {
            return null;
        } else if (z >= oldLotHeader.minLevel && z <= oldLotHeader.maxLevel) {
            POTLotPack oldLotPack = this.getOldLotPack(oldLotHeader);
            return oldLotPack.getSquareData(squareX, squareY, z);
        } else {
            return null;
        }
    }

    void convertChunkData() throws IOException {
        for (int y = this.minY * 300; y < (this.maxY + 1) * 300; y += 256) {
            for (int x = this.minX * 300; x < (this.maxX + 1) * 300; x += 256) {
                int newCellX = x / 256;
                int newCellY = y / 256;
                if (this.shouldConvertNewCell(newCellX, newCellY)) {
                    this.convertChunkData(newCellX, newCellY);
                }
            }
        }
    }

    void convertChunkData(int newCellX, int newCellY) throws IOException {
        POTChunkData newChunkData = new POTChunkData(newCellX, newCellY, true);
        int y = newChunkData.getMinSquareY();

        for (int y2 = newChunkData.getMaxSquareY(); y <= y2; y++) {
            int x = newChunkData.getMinSquareX();

            for (int x2 = newChunkData.getMaxSquareX(); x <= x2; x++) {
                newChunkData.setSquareBits(x, y, this.getOldChunkDataBits(x, y));
            }
        }

        newChunkData.save(String.format("%s%schunkdata_%d_%d.bin", this.mapDirectoryOut, File.separator, newChunkData.x, newChunkData.y));
    }

    POTChunkData getOldChunkData(int oldCellX, int oldCellY) throws IOException {
        int index = oldCellX + oldCellY * 1000;
        File file = this.chunkDataFiles.get(index);
        if (file == null) {
            return null;
        } else {
            POTChunkData chunkData = this.oldChunkData.get(index);
            if (chunkData == null) {
                chunkData = new POTChunkData(oldCellX, oldCellY, false);
                chunkData.load(file);
                this.oldChunkData.put(index, chunkData);
            }

            return chunkData;
        }
    }

    byte getOldChunkDataBits(int squareX, int squareY) throws IOException {
        POTChunkData oldChunkData = this.getOldChunkData(squareX / 300, squareY / 300);
        if (oldChunkData == null) {
            return 0;
        } else {
            return !oldChunkData.containsSquare(squareX, squareY) ? 0 : oldChunkData.getSquareBits(squareX, squareY);
        }
    }

    void convertObjectsLua() {
    }

    void convertSpawnPointsLua() {
    }

    void convertWorldMapBIN(String fileName) throws Exception {
        String filePath = this.mapDirectoryIn + File.separator + fileName;
        File file = new File(filePath);
        if (file.exists()) {
            WorldMapBinary wmb = new WorldMapBinary();
            WorldMapData wmd = new WorldMapData(new AssetPath(filePath), WorldMapDataAssetManager.instance);
            wmb.read(this.mapDirectoryIn + File.separator + fileName, wmd);
            wmd.onLoaded();
            POTWorldMapData newData = new POTWorldMapData();

            for (int y = wmd.minY; y <= wmd.maxY; y++) {
                for (int x = wmd.minX; x <= wmd.maxX; x++) {
                    WorldMapCell wmc = wmd.getCell(x, y);
                    if (wmc != null) {
                        for (WorldMapFeature wmf : wmc.features) {
                            newData.addFeature(wmf);
                        }
                    }
                }
            }

            newData.minX = Integer.MAX_VALUE;
            newData.minY = Integer.MAX_VALUE;
            newData.maxX = Integer.MIN_VALUE;
            newData.maxY = Integer.MIN_VALUE;

            for (WorldMapCell cell : newData.cells) {
                newData.minX = Math.min(newData.minX, cell.x);
                newData.minY = Math.min(newData.minY, cell.y);
                newData.maxX = Math.max(newData.maxX, cell.x);
                newData.maxY = Math.max(newData.maxY, cell.y);
            }

            newData.saveBIN(this.mapDirectoryOut + File.separator + fileName, true);
        }
    }

    void convertWorldMapXML() {
    }

    void convertNewMapBinaryDirectory(String folderIn, String folderOut) throws IOException {
        File file = new File(folderIn);
        File[] files = file.listFiles();

        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getName();
            String suffix = fileName.substring(fileName.lastIndexOf(46));
            if (".pzby".equalsIgnoreCase(suffix)) {
                this.convertNewMapBinaryFile(files[i], new File(folderOut, files[i].getName()));
            }
        }
    }

    void convertNewMapBinaryFile(File in, File out) throws IOException {
        NewMapBinaryFile file = new NewMapBinaryFile(false);
        NewMapBinaryFile.Header headerOld = file.loadHeader(in.getAbsolutePath());

        for (int ly = 0; ly < headerOld.height; ly++) {
            for (int lx = 0; lx < headerOld.width; lx++) {
                NewMapBinaryFile.ChunkData var7 = file.loadChunk(headerOld, lx, ly);
            }
        }
    }

    public static void runOnStart() {
        new POT();

        try {
            RandStandard.INSTANCE.init();
            RandLua.INSTANCE.init();
        } catch (Exception var2) {
            var2.printStackTrace();
            SliceY.SliceBuffer.order(ByteOrder.BIG_ENDIAN);
        }

        System.exit(0);
    }
}
