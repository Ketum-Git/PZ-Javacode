// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapGeometry;
import zombie.worldMap.WorldMapPoints;
import zombie.worldMap.WorldMapProperties;

public final class POTWorldMapData {
    static final int VERSION1 = 1;
    static final int VERSION2 = 2;
    static final int VERSION_LATEST = 2;
    static final TObjectIntHashMap<String> m_stringTable = new TObjectIntHashMap<>();
    public final ArrayList<WorldMapCell> cells = new ArrayList<>();
    public final HashMap<Integer, WorldMapCell> cellLookup = new HashMap<>();
    public int minX;
    public int minY;
    public int maxX;
    public int maxY;

    public WorldMapCell getCell(int x, int y) {
        Integer index = this.getCellKey(x, y);
        return this.cellLookup.get(index);
    }

    private Integer getCellKey(int x, int y) {
        return x + y * 1000;
    }

    public void addFeature(WorldMapFeature oldFeature) {
        int minCellX = this.getMinSquareX(oldFeature) / 256;
        int minCellY = this.getMinSquareY(oldFeature) / 256;
        int maxCellX = this.getMaxSquareX(oldFeature) / 256;
        int maxCellY = this.getMaxSquareY(oldFeature) / 256;

        for (int y = minCellY; y <= maxCellY; y++) {
            for (int x = minCellX; x <= maxCellX; x++) {
                WorldMapCell newCell = this.getCell(x, y);
                if (newCell == null) {
                    newCell = new WorldMapCell();
                    newCell.x = x;
                    newCell.y = y;
                    this.cells.add(newCell);
                    this.cellLookup.put(this.getCellKey(x, y), newCell);
                }

                WorldMapFeature newFeature = new WorldMapFeature(newCell);
                this.convertFeature(newFeature, oldFeature);
                newCell.features.add(newFeature);
            }
        }
    }

    public void saveBIN(String fileName, boolean b256) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(31457280);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.clear();
        bb.put((byte)73);
        bb.put((byte)71);
        bb.put((byte)77);
        bb.put((byte)66);
        bb.putInt(2);
        bb.putInt(b256 ? 256 : 300);
        bb.putInt(this.getWidthInCells());
        bb.putInt(this.getHeightInCells());
        this.writeStringTable(bb);

        for (int y = this.minY; y <= this.maxY; y++) {
            for (int x = this.minX; x <= this.maxX; x++) {
                WorldMapCell cell = this.getCell(x, y);
                if (cell == null) {
                    bb.putInt(-1);
                } else {
                    this.writeCell(bb, cell);
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(bb.array(), 0, bb.position());
        }
    }

    void writeStringTable(ByteBuffer bb) {
        m_stringTable.clear();
        ArrayList<String> strings = new ArrayList<>();

        for (int y = this.minY; y <= this.maxY; y++) {
            for (int x = this.minX; x <= this.maxX; x++) {
                WorldMapCell cell = this.getCell(x, y);
                if (cell != null) {
                    for (WorldMapFeature feature : cell.features) {
                        this.addString(strings, feature.geometry.type.name());

                        for (Entry<String, String> e : feature.properties.entrySet()) {
                            this.addString(strings, e.getKey());
                            this.addString(strings, e.getValue());
                        }
                    }
                }
            }
        }

        bb.putInt(strings.size());

        for (String str : strings) {
            this.SaveString(bb, str);
        }
    }

    void addString(ArrayList<String> strings, String str) {
        if (!m_stringTable.containsKey(str)) {
            m_stringTable.put(str, strings.size());
            strings.add(str);
        }
    }

    void writeCell(ByteBuffer bb, WorldMapCell cell) {
        if (cell.features.isEmpty()) {
            bb.putInt(-1);
        } else {
            bb.putInt(cell.x);
            bb.putInt(cell.y);
            bb.putInt(cell.features.size());

            for (WorldMapFeature feature : cell.features) {
                this.writeFeature(bb, feature);
            }
        }
    }

    void writeFeature(ByteBuffer bb, WorldMapFeature feature) {
        WorldMapGeometry geometry = feature.geometry;
        this.SaveStringIndex(bb, geometry.type.name());
        bb.put((byte)geometry.points.size());

        for (WorldMapPoints points : geometry.points) {
            bb.putShort((short)points.numPoints());

            for (int i = 0; i < points.numPoints(); i++) {
                bb.putShort((short)points.getX(i));
                bb.putShort((short)points.getY(i));
            }
        }

        bb.put((byte)feature.properties.size());

        for (Entry<String, String> e : feature.properties.entrySet()) {
            this.SaveStringIndex(bb, e.getKey());
            this.SaveStringIndex(bb, e.getValue());
        }
    }

    void SaveString(ByteBuffer bb, String str) {
        GameWindow.WriteStringUTF(bb, str);
    }

    void SaveStringIndex(ByteBuffer bb, String str) {
        bb.putShort((short)m_stringTable.get(str));
    }

    int getMinSquareX(WorldMapFeature feature) {
        return feature.cell.x * 300 + feature.geometry.minX;
    }

    int getMinSquareY(WorldMapFeature feature) {
        return feature.cell.y * 300 + feature.geometry.minY;
    }

    int getMaxSquareX(WorldMapFeature feature) {
        return feature.cell.x * 300 + feature.geometry.maxX;
    }

    int getMaxSquareY(WorldMapFeature feature) {
        return feature.cell.y * 300 + feature.geometry.maxY;
    }

    public int getWidthInCells() {
        return this.maxX - this.minX + 1;
    }

    public int getHeightInCells() {
        return this.maxY - this.minY + 1;
    }

    void convertFeature(WorldMapFeature newFeature, WorldMapFeature oldFeature) {
        WorldMapGeometry oldGeometry = oldFeature.geometry;
        WorldMapGeometry newGeometry = new WorldMapGeometry(newFeature.cell);
        newGeometry.type = oldGeometry.type;

        for (WorldMapPoints oldPoints : oldGeometry.points) {
            int numPoints = oldPoints.numPoints();
            WorldMapPoints newPoints = new WorldMapPoints(newGeometry);
            ShortBuffer pointBuffer = newFeature.cell.getPointBuffer(numPoints);
            int firstPoint = pointBuffer.position();
            newPoints.setPoints((short)firstPoint, (short)oldPoints.numPoints());

            for (int i = 0; i < numPoints; i++) {
                int oldX = oldFeature.cell.x * 300 + oldPoints.getX(i);
                int oldY = oldFeature.cell.y * 300 + oldPoints.getY(i);
                int newX = oldX - newFeature.cell.x * 256;
                int newY = oldY - newFeature.cell.y * 256;
                pointBuffer.put((short)newX);
                pointBuffer.put((short)newY);
            }

            newGeometry.points.add(newPoints);
        }

        newGeometry.calculateBounds();
        newFeature.geometry = newGeometry;
        newFeature.properties = new WorldMapProperties();
        newFeature.properties.putAll(oldFeature.properties);
    }
}
