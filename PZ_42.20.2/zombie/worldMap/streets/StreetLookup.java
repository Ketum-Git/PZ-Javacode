// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.core.math.PZMath;
import zombie.pathfind.LiangBarsky;
import zombie.util.list.PZArrayUtil;

public final class StreetLookup {
    static final int SQUARES_PER_CHUNK = 20;
    static final int CHUNKS_PER_CELL = 10;
    static final int SQUARES_PER_CELL = 200;
    static final LiangBarsky LB = new LiangBarsky();
    final TLongObjectHashMap<StreetLookup.Cell> cellLookup = new TLongObjectHashMap<>();

    long getCellKey(int cellX, int cellY) {
        return (long)cellY << 32 | cellX;
    }

    StreetLookup.Cell getCell(int cellX, int cellY) {
        long cellKey = this.getCellKey(cellX, cellY);
        return this.cellLookup.get(cellKey);
    }

    StreetLookup.Cell createCell(int cellX, int cellY) {
        StreetLookup.Cell cell = new StreetLookup.Cell(cellX, cellY);
        long cellKey = this.getCellKey(cellX, cellY);
        this.cellLookup.put(cellKey, cell);
        return cell;
    }

    StreetLookup.Cell getOrCreateCell(int cellX, int cellY) {
        StreetLookup.Cell cell = this.getCell(cellX, cellY);
        if (cell == null) {
            cell = this.createCell(cellX, cellY);
        }

        return cell;
    }

    void addStreet(WorldMapStreet street) {
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0F);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0F);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0F);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0F);

        for (int cellY = cellY1; cellY <= cellY2; cellY++) {
            for (int cellX = cellX1; cellX <= cellX2; cellX++) {
                if (streetIntersects(street, cellX * 200, cellY * 200, (cellX + 1) * 200, (cellY + 1) * 200)) {
                    StreetLookup.Cell cell = this.getOrCreateCell(cellX, cellY);
                    cell.addStreet(street);
                }
            }
        }
    }

    static boolean streetIntersects(WorldMapStreet street, int left, int top, int right, int bottom) {
        for (int i = 0; i < street.getNumPoints() - 1; i++) {
            float x1 = street.getPointX(i);
            float y1 = street.getPointY(i);
            float x2 = street.getPointX(i + 1);
            float y2 = street.getPointY(i + 1);
            boolean intersect = LB.lineRectIntersect(x1, y1, x2 - x1, y2 - y1, left, top, right, bottom);
            if (intersect) {
                return true;
            }
        }

        return false;
    }

    void addStreets(ArrayList<WorldMapStreet> streets) {
        for (WorldMapStreet street : streets) {
            this.addStreet(street);
        }
    }

    void removeStreet(WorldMapStreet street) {
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0F);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0F);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0F);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0F);

        for (int cellY = cellY1; cellY <= cellY2; cellY++) {
            for (int cellX = cellX1; cellX <= cellX2; cellX++) {
                StreetLookup.Cell cell = this.getCell(cellX, cellY);
                if (cell != null) {
                    cell.removeStreet(street);
                }
            }
        }
    }

    void onStreetChanged(WorldMapStreet street) {
    }

    void getStreetsOverlapping(int minX, int minY, int maxX, int maxY, THashSet<WorldMapStreet> result) {
        int cellX1 = minX / 200;
        int cellY1 = minY / 200;
        int cellX2 = maxX / 200;
        int cellY2 = maxY / 200;

        for (int cellY = cellY1; cellY <= cellY2; cellY++) {
            for (int cellX = cellX1; cellX <= cellX2; cellX++) {
                StreetLookup.Cell cell = this.getCell(cellX, cellY);
                if (cell != null) {
                    cell.getStreetsOverlapping(minX - cellX * 200, minY - cellY * 200, maxX - cellX * 200, maxY - cellY * 200, result);
                }
            }
        }
    }

    public void Dispose() {
        this.cellLookup.forEachValue(cell -> {
            cell.Dispose();
            return true;
        });
        this.cellLookup.clear();
    }

    static final class Cell {
        final int cellX;
        final int cellY;
        final StreetLookup.Chunk[] chunks = new StreetLookup.Chunk[100];

        Cell(int cellX, int cellY) {
            this.cellX = cellX;
            this.cellY = cellY;
        }

        StreetLookup.Chunk getChunk(int chunkX, int chunkY) {
            return this.chunks[chunkX + chunkY * 10];
        }

        StreetLookup.Chunk createChunk(int chunkX, int chunkY) {
            StreetLookup.Chunk chunk = new StreetLookup.Chunk();
            this.chunks[chunkX + chunkY * 10] = chunk;
            return chunk;
        }

        StreetLookup.Chunk getOrCreateChunk(int chunkX, int chunkY) {
            StreetLookup.Chunk chunk = this.getChunk(chunkX, chunkY);
            if (chunk == null) {
                chunk = this.createChunk(chunkX, chunkY);
            }

            return chunk;
        }

        void addStreet(WorldMapStreet street) {
            int minX = PZMath.fastfloor(street.getMinX() - this.cellX * 200);
            int minY = PZMath.fastfloor(street.getMinY() - this.cellY * 200);
            int maxX = PZMath.fastfloor(street.getMaxX() - this.cellX * 200);
            int maxY = PZMath.fastfloor(street.getMaxY() - this.cellY * 200);
            int chunkX1 = PZMath.max(minX / 20, 0);
            int chunkY1 = PZMath.max(minY / 20, 0);
            int chunkX2 = PZMath.min(maxX / 20, 9);
            int chunkY2 = PZMath.min(maxY / 20, 9);
            int left = this.cellX * 200;
            int top = this.cellY * 200;

            for (int chunkY = chunkY1; chunkY <= chunkY2; chunkY++) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
                    if (StreetLookup.streetIntersects(street, left + chunkX * 20, top + chunkY * 20, left + (chunkX + 1) * 20, top + (chunkY + 1) * 20)) {
                        StreetLookup.Chunk chunk = this.getOrCreateChunk(chunkX, chunkY);
                        chunk.addStreet(street);
                    }
                }
            }
        }

        void removeStreet(WorldMapStreet street) {
            for (int chunkY = 0; chunkY < 10; chunkY++) {
                for (int chunkX = 0; chunkX < 10; chunkX++) {
                    StreetLookup.Chunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk != null) {
                        chunk.removeStreet(street);
                    }
                }
            }
        }

        void getStreetsOverlapping(int minX, int minY, int maxX, int maxY, THashSet<WorldMapStreet> result) {
            int chunkX1 = PZMath.max(minX / 20, 0);
            int chunkY1 = PZMath.max(minY / 20, 0);
            int chunkX2 = PZMath.min(maxX / 20, 9);
            int chunkY2 = PZMath.min(maxY / 20, 9);

            for (int chunkY = chunkY1; chunkY <= chunkY2; chunkY++) {
                for (int chunkX = chunkX1; chunkX <= chunkX2; chunkX++) {
                    StreetLookup.Chunk chunk = this.getChunk(chunkX, chunkY);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.streetCount; i++) {
                            result.add(chunk.streets[i]);
                        }
                    }
                }
            }
        }

        void Dispose() {
            for (StreetLookup.Chunk chunk : this.chunks) {
                if (chunk != null) {
                    chunk.Dispose();
                }
            }

            Arrays.fill(this.chunks, null);
        }
    }

    static final class Chunk {
        WorldMapStreet[] streets;
        short streetCount;

        void addStreet(WorldMapStreet street) {
            if (!this.contains(street)) {
                this.streets = PZArrayUtil.newInstance(WorldMapStreet.class, this.streets, this.streetCount + 1, true);
                this.streets[this.streetCount++] = street;
            }
        }

        void removeStreet(WorldMapStreet street) {
            if (this.streets != null) {
                int index = this.indexOf(street);
                if (index != -1) {
                    System.arraycopy(this.streets, index + 1, this.streets, index, this.streetCount - index - 1);
                    this.streetCount--;
                }
            }
        }

        int indexOf(WorldMapStreet street) {
            return PZArrayUtil.indexOf(this.streets, this.streetCount, street);
        }

        boolean contains(WorldMapStreet street) {
            return PZArrayUtil.contains(this.streets, this.streetCount, street);
        }

        void Dispose() {
            if (this.streets != null) {
                Arrays.fill(this.streets, null);
                this.streets = null;
                this.streetCount = 0;
            }
        }
    }
}
