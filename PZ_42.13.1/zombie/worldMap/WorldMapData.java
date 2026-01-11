// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.math.PZMath;

public final class WorldMapData extends Asset {
    public static final HashMap<String, WorldMapData> s_fileNameToData = new HashMap<>();
    public String relativeFileName;
    public final ArrayList<WorldMapCell> cells = new ArrayList<>();
    public final TLongObjectHashMap<WorldMapCell> cellLookup = new TLongObjectHashMap<>();
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;
    public static final AssetType ASSET_TYPE = new AssetType("WorldMapData");

    public static WorldMapData getOrCreateData(String fileName) {
        WorldMapData data = s_fileNameToData.get(fileName);
        if (data == null && Files.exists(Paths.get(fileName))) {
            data = (WorldMapData)WorldMapDataAssetManager.instance.load(new AssetPath(fileName));
            s_fileNameToData.put(fileName, data);
        }

        return data;
    }

    public WorldMapData(AssetPath path, AssetManager manager) {
        super(path, manager);
    }

    public WorldMapData(AssetPath path, AssetManager manager, AssetManager.AssetParams params) {
        super(path, manager);
    }

    public void clearTriangles() {
        for (WorldMapCell cell : this.cells) {
            cell.clearTriangles();
        }
    }

    public void clear() {
        for (WorldMapCell cell : this.cells) {
            cell.dispose();
        }

        this.cells.clear();
        this.cellLookup.clear();
        this.minX = 0;
        this.minY = 0;
        this.maxX = 0;
        this.maxY = 0;
    }

    public int getWidthInCells() {
        return this.maxX - this.minX + 1;
    }

    public int getHeightInCells() {
        return this.maxY - this.minY + 1;
    }

    public int getWidthInSquares() {
        return this.getWidthInCells() * 256;
    }

    public int getHeightInSquares() {
        return this.getHeightInCells() * 256;
    }

    public void onLoaded() {
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.cellLookup.clear();

        for (WorldMapCell cell : this.cells) {
            long index = this.getCellKey(cell.x, cell.y);
            this.cellLookup.put(index, cell);
            this.minX = Math.min(this.minX, cell.x);
            this.minY = Math.min(this.minY, cell.y);
            this.maxX = Math.max(this.maxX, cell.x);
            this.maxY = Math.max(this.maxY, cell.y);
        }
    }

    public WorldMapCell getCell(int x, int y) {
        long index = this.getCellKey(x, y);
        return this.cellLookup.get(index);
    }

    private long getCellKey(int x, int y) {
        return x + ((long)y << 32);
    }

    public void hitTest(float x, float y, ArrayList<WorldMapFeature> features) {
        int cellX = (int)PZMath.floor(x / 256.0F);
        int cellY = (int)PZMath.floor(y / 256.0F);
        if (cellX >= this.minX && cellX <= this.maxX && cellY >= this.minY && cellY <= this.maxY) {
            WorldMapCell cell = this.getCell(cellX, cellY);
            if (cell != null) {
                cell.hitTest(x, y, features);
            }
        }
    }

    public static void Reset() {
        for (WorldMapData data : s_fileNameToData.values()) {
            data.clearTriangles();
        }
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    @Override
    protected void onBeforeEmpty() {
        super.onBeforeEmpty();
        this.clear();
    }
}
