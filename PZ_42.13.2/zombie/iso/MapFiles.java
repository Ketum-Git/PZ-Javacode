// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.MapGroups;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.utils.BooleanGrid;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;

public final class MapFiles {
    public final String mapDirectoryName;
    public final String mapDirectoryZfsPath;
    public final String mapDirectoryAbsolutePath;
    public final int priority;
    public int minX = Integer.MAX_VALUE;
    public int minY = Integer.MAX_VALUE;
    public int maxX = Integer.MIN_VALUE;
    public int maxY = Integer.MIN_VALUE;
    public final HashMap<String, LotHeader> infoHeaders = new HashMap<>();
    public final ArrayList<String> infoHeaderNames = new ArrayList<>();
    public final HashMap<String, String> infoFileNames = new HashMap<>();
    public final HashMap<String, ChunkGenerationStatus> infoFileModded = new HashMap<>();
    public BooleanGrid bgHasCell;
    public int minCell300X;
    public int minCell300Y;
    public int maxCell300X;
    public int maxCell300Y;
    public BooleanGrid bgHasCell300;
    private static final ThreadLocal<TLongObjectHashMap<String>> LotHeaderFileNameCache = ThreadLocal.withInitial(TLongObjectHashMap::new);
    static final ArrayList<MapFiles> currentMapFiles = new ArrayList<>();
    static String currentMap;

    public MapFiles(String mapDirectoryName, String mapDirectoryZfsPath, String mapDirectoryAbsolutePath, int priority) {
        this.mapDirectoryName = mapDirectoryName;
        this.mapDirectoryZfsPath = mapDirectoryZfsPath;
        this.mapDirectoryAbsolutePath = mapDirectoryAbsolutePath;
        this.priority = priority;
    }

    public int getWidthInCells() {
        return this.maxX - this.minX + 1;
    }

    public int getHeightInCells() {
        return this.maxY - this.minY + 1;
    }

    private LotHeader createLotHeader(ChooseGameInfo.Map mapInfo, String fileName) {
        String[] split = fileName.split("_");
        split[1] = split[1].replace(".lotheader", "");
        int x = Integer.parseInt(split[0].trim());
        int y = Integer.parseInt(split[1].trim());
        this.minX = PZMath.min(this.minX, x);
        this.minY = PZMath.min(this.minY, y);
        this.maxX = PZMath.max(this.maxX, x);
        this.maxY = PZMath.max(this.maxY, y);
        LotHeader lotHeader = new LotHeader(x, y);
        lotHeader.fixed2x = mapInfo.isFixed2x();
        lotHeader.mapFiles = this;
        lotHeader.fileName = fileName;
        lotHeader.absoluteFilePath = this.mapDirectoryAbsolutePath + File.separator + fileName;
        return lotHeader;
    }

    public boolean load() {
        File fo = new File(this.mapDirectoryAbsolutePath);
        if (!fo.isDirectory()) {
            return false;
        } else {
            String absolutePath = this.mapDirectoryAbsolutePath;
            ChooseGameInfo.Map mapInfo = ChooseGameInfo.getMapDetails(this.mapDirectoryName);
            String[] internalNames = fo.list();

            for (int i = 0; i < internalNames.length; i++) {
                String fileName = internalNames[i];
                if (fileName.endsWith(".lotheader")) {
                    LotHeader info = this.createLotHeader(mapInfo, fileName);
                    this.infoFileNames.put(fileName, info.absoluteFilePath);
                    this.infoFileModded.put(fileName, ZomboidFileSystem.instance.isModded(this.mapDirectoryAbsolutePath));
                    this.infoHeaders.put(fileName, info);
                    this.infoHeaderNames.add(fileName);
                } else if (fileName.endsWith(".lotpack")) {
                    this.infoFileNames.put(fileName, absolutePath + File.separator + fileName);
                    this.infoFileModded.put(fileName, ZomboidFileSystem.instance.isModded(this.mapDirectoryAbsolutePath));
                } else if (fileName.startsWith("chunkdata_")) {
                    this.infoFileNames.put(fileName, absolutePath + File.separator + fileName);
                    this.infoFileModded.put(fileName, ZomboidFileSystem.instance.isModded(this.mapDirectoryAbsolutePath));
                }
            }

            return true;
        }
    }

    public void postLoad() {
        if (this.minX > this.maxX) {
            this.minX = this.maxX = 0;
            this.minY = this.maxY = 0;
        }

        this.bgHasCell = new BooleanGrid(this.getWidthInCells(), this.getHeightInCells());
        int y = 0;

        for (int height = this.getHeightInCells(); y < height; y++) {
            int x = 0;

            for (int width = this.getWidthInCells(); x < width; x++) {
                this.bgHasCell.setValue(x, y, this.infoHeaders.containsKey(String.format("%d_%d.lotheader", this.minX + x, this.minY + y)));
            }
        }

        this.minCell300X = (int)Math.floor(this.minX * 256.0F / 300.0F);
        this.minCell300Y = (int)Math.floor(this.minY * 256.0F / 300.0F);
        this.maxCell300X = (int)Math.floor((this.maxX + 1) * 256.0F / 300.0F);
        this.maxCell300Y = (int)Math.floor((this.maxY + 1) * 256.0F / 300.0F);
        this.bgHasCell300 = new BooleanGrid(this.maxCell300X - this.minCell300X + 1, this.maxCell300Y - this.minCell300Y + 1);

        for (int cell300Y = this.minCell300Y; cell300Y <= this.maxCell300Y; cell300Y++) {
            for (int cell300X = this.minCell300X; cell300X <= this.maxCell300X; cell300X++) {
                int cell256X = (int)Math.floor(cell300X * 300.0F / 256.0F);
                int cell256Y = (int)Math.floor(cell300Y * 300.0F / 256.0F);
                if (this.hasCell(cell256X, cell256Y) && this.hasCell(cell256X + 1, cell256Y + 1)) {
                    this.bgHasCell300.setValue(cell300X - this.minCell300X, cell300Y - this.minCell300Y, true);
                }
            }
        }
    }

    public boolean isValidCellPos(int cellX, int cellY) {
        return cellX >= this.minX && cellY >= this.minY && cellX <= this.maxX && cellY <= this.maxY;
    }

    public LotHeader getLotHeader(int cellX, int cellY) {
        if (this.hasCell(cellX, cellY)) {
            long key = (long)cellY << 32 | cellX;
            TLongObjectHashMap<String> nameMap = LotHeaderFileNameCache.get();
            String name = nameMap.get(key);
            if (name == null) {
                name = String.format("%d_%d.lotheader", cellX, cellY);
                nameMap.put(key, name);
            }

            return this.infoHeaders.get(name);
        } else {
            return null;
        }
    }

    public boolean hasCell(int cellX, int cellY) {
        return this.isValidCellPos(cellX, cellY) && this.bgHasCell.getValue(cellX - this.minX, cellY - this.minY);
    }

    public boolean hasCell300(int cell300X, int cell300Y) {
        return this.bgHasCell300.getValue(cell300X - this.minCell300X, cell300Y - this.minCell300Y);
    }

    public void Dispose() {
        for (LotHeader lotHeader : this.infoHeaders.values()) {
            lotHeader.Dispose();
        }

        this.infoHeaders.clear();
        this.infoHeaderNames.clear();
        this.infoFileNames.clear();
        this.infoFileModded.clear();
    }

    public static ArrayList<MapFiles> getCurrentMapFiles() {
        if (!IsoLot.MapFiles.isEmpty()) {
            return IsoLot.MapFiles;
        } else {
            if (currentMapFiles.isEmpty() || !StringUtils.equals(currentMap, Core.gameMap)) {
                Reset();
                ArrayList<String> lotDirs = getLotDirectories();
                currentMap = Core.gameMap;

                for (int i = 0; i < lotDirs.size(); i++) {
                    String lotDir = lotDirs.get(i);
                    String path = ZomboidFileSystem.instance.getDirectoryString("media/maps/" + lotDir + "/");
                    File file = new File(path);
                    if (file.isDirectory()) {
                        MapFiles mapFiles = new MapFiles(lotDir, path, file.getAbsolutePath(), i);
                        currentMapFiles.add(mapFiles);
                        if (mapFiles.load()) {
                            mapFiles.postLoad();
                        }
                    }
                }
            }

            return currentMapFiles;
        }
    }

    private static void getLotDirectories(String mapName, ArrayList<String> result) {
        if (!result.contains(mapName)) {
            ChooseGameInfo.Map mapInfo = ChooseGameInfo.getMapDetails(mapName);
            if (mapInfo != null) {
                result.add(mapName);

                for (String lotDir : mapInfo.getLotDirectories()) {
                    getLotDirectories(lotDir, result);
                }
            }
        }
    }

    private static ArrayList<String> getLotDirectories() {
        if (GameClient.client) {
            Core.gameMap = GameClient.gameMap;
        }

        if (GameServer.server) {
            Core.gameMap = GameServer.gameMap;
        }

        if (Core.gameMap.equals("DEFAULT")) {
            MapGroups mapGroups = new MapGroups();
            mapGroups.createGroups();
            if (mapGroups.getNumberOfGroups() != 1) {
                throw new RuntimeException("GameMap is DEFAULT but there are multiple worlds to choose from");
            }

            mapGroups.setWorld(0);
        }

        ArrayList<String> result = new ArrayList<>();
        if (Core.gameMap.contains(";")) {
            String[] ss = Core.gameMap.split(";");

            for (int i = 0; i < ss.length; i++) {
                String lotDir = ss[i].trim();
                if (!lotDir.isEmpty() && !result.contains(lotDir)) {
                    result.add(lotDir);
                }
            }
        } else {
            getLotDirectories(Core.gameMap, result);
        }

        return result;
    }

    public static void Reset() {
        for (MapFiles mapFiles : currentMapFiles) {
            mapFiles.Dispose();
        }

        currentMapFiles.clear();
        currentMap = null;
    }
}
