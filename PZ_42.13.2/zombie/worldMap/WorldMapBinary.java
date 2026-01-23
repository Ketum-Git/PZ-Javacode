// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.util.SharedStrings;

public final class WorldMapBinary {
    private static final int VERSION1 = 1;
    private static final int VERSION2 = 2;
    private static final int VERSION_LATEST = 2;
    private final SharedStrings sharedStrings = new SharedStrings();
    private final TIntObjectHashMap<String> stringTable = new TIntObjectHashMap<>();
    private final WorldMapProperties properties = new WorldMapProperties();
    private final ArrayList<WorldMapProperties> sharedProperties = new ArrayList<>();

    public boolean read(String filePath, WorldMapData data) throws Exception {
        try (
            FileInputStream fis = new FileInputStream(filePath);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            int b1 = bis.read();
            int b2 = bis.read();
            int b3 = bis.read();
            int b4 = bis.read();
            if (b1 == 73 && b2 == 71 && b3 == 77 && b4 == 66) {
                int version = this.readInt(bis);
                if (version < 1 || version > 2) {
                    throw new IOException("unrecognized version " + version);
                } else if (version == 1) {
                    throw new IOException("version 1 (cell size 300) is not supported");
                } else {
                    int cellSize = this.readInt(bis);
                    int width = this.readInt(bis);
                    int height = this.readInt(bis);
                    if (cellSize != 256) {
                        throw new IOException("unsupported cell size %d".formatted(cellSize));
                    } else {
                        this.readStringTable(bis);

                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                WorldMapCell cell = this.parseCell(bis);
                                if (cell != null) {
                                    data.cells.add(cell);
                                }
                            }
                        }

                        return true;
                    }
                }
            } else {
                throw new IOException("invalid format (magic doesn't match)");
            }
        }
    }

    private int readByte(InputStream in) throws IOException {
        return in.read();
    }

    private int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        } else {
            return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
        }
    }

    private short readShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        } else {
            return (short)((ch1 << 0) + (ch2 << 8));
        }
    }

    private void readStringTable(InputStream in) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        byte[] bytes = new byte[1024];
        int numStrings = this.readInt(in);

        for (int i = 0; i < numStrings; i++) {
            bb.clear();
            int length = this.readShort(in);
            bb.putShort((short)length);
            in.read(bytes, 0, length);
            bb.put(bytes, 0, length);
            bb.flip();
            this.stringTable.put(i, GameWindow.ReadStringUTF(bb));
        }
    }

    private String readStringIndexed(InputStream in) throws IOException {
        int index = this.readShort(in);
        if (!this.stringTable.containsKey(index)) {
            throw new IOException("invalid string-table index " + index);
        } else {
            return this.stringTable.get(index);
        }
    }

    private WorldMapCell parseCell(InputStream in) throws IOException {
        int x = this.readInt(in);
        if (x == -1) {
            return null;
        } else {
            int y = this.readInt(in);
            WorldMapCell cell = new WorldMapCell();
            cell.x = x;
            cell.y = y;
            int numFeatures = this.readInt(in);
            cell.features.ensureCapacity(numFeatures);

            for (int i = 0; i < numFeatures; i++) {
                WorldMapFeature feature = this.parseFeature(cell, in);
                cell.features.add(feature);
            }

            cell.features.trimToSize();
            return cell;
        }
    }

    private WorldMapFeature parseFeature(WorldMapCell cell, InputStream in) throws IOException {
        WorldMapFeature feature = new WorldMapFeature(cell);
        WorldMapGeometry geometry = this.parseGeometry(cell, in);
        feature.geometry = geometry;
        this.parseFeatureProperties(in, feature);
        return feature;
    }

    private void parseFeatureProperties(InputStream in, WorldMapFeature feature) throws IOException {
        this.properties.clear();
        int numProperties = this.readByte(in);

        for (int i = 0; i < numProperties; i++) {
            String name = this.sharedStrings.get(this.readStringIndexed(in));
            String value = this.sharedStrings.get(this.readStringIndexed(in));
            this.properties.put(name, value);
        }

        feature.properties = this.getOrCreateProperties(this.properties);
    }

    private WorldMapProperties getOrCreateProperties(WorldMapProperties properties) {
        for (int i = 0; i < this.sharedProperties.size(); i++) {
            if (this.sharedProperties.get(i).equals(properties)) {
                return this.sharedProperties.get(i);
            }
        }

        WorldMapProperties result = new WorldMapProperties();
        result.putAll(properties);
        this.sharedProperties.add(result);
        return result;
    }

    private WorldMapGeometry parseGeometry(WorldMapCell cell, InputStream in) throws IOException {
        WorldMapGeometry geometry = new WorldMapGeometry(cell);
        geometry.type = WorldMapGeometry.Type.valueOf(this.readStringIndexed(in));
        int numCoordinates = this.readByte(in);
        geometry.points.ensureCapacity(numCoordinates);

        for (int i = 0; i < numCoordinates; i++) {
            WorldMapPoints points = new WorldMapPoints(geometry);
            this.parseGeometryCoordinates(cell, in, points);
            geometry.points.add(points);
        }

        geometry.points.trimToSize();
        geometry.calculateBounds();
        return geometry;
    }

    private void parseGeometryCoordinates(WorldMapCell cell, InputStream in, WorldMapPoints points) throws IOException {
        int numPoints = this.readShort(in);
        ShortBuffer pointBuffer = cell.getPointBuffer(numPoints);
        int firstPoint = pointBuffer.position();
        points.setPoints((short)firstPoint, (short)numPoints);

        for (int i = 0; i < numPoints; i++) {
            pointBuffer.put(this.readShort(in));
            pointBuffer.put(this.readShort(in));
        }
    }
}
