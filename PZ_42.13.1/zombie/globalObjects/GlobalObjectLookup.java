// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.debug.DebugLog;
import zombie.iso.IsoMetaGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class GlobalObjectLookup {
    private static final int SQUARES_PER_CHUNK = 8;
    private static final int SQUARES_PER_CELL = 256;
    private static final int CHUNKS_PER_CELL = 32;
    private static IsoMetaGrid metaGrid;
    private static final GlobalObjectLookup.Shared sharedServer = new GlobalObjectLookup.Shared();
    private static final GlobalObjectLookup.Shared sharedClient = new GlobalObjectLookup.Shared();
    private final GlobalObjectSystem system;
    private final GlobalObjectLookup.Shared shared;
    private final GlobalObjectLookup.Cell[] cells;

    public GlobalObjectLookup(GlobalObjectSystem system) {
        this.system = system;
        this.shared = system instanceof SGlobalObjectSystem ? sharedServer : sharedClient;
        this.cells = this.shared.cells;
    }

    private GlobalObjectLookup.Cell getCellAt(int squareX, int squareY, boolean create) {
        int x = squareX - metaGrid.minX * 256;
        int y = squareY - metaGrid.minY * 256;
        if (x >= 0 && y >= 0 && x < metaGrid.getWidth() * 256 && y < metaGrid.getHeight() * 256) {
            int cellX = x / 256;
            int cellY = y / 256;
            int index = cellX + cellY * metaGrid.getWidth();
            if (this.cells[index] == null && create) {
                this.cells[index] = new GlobalObjectLookup.Cell(metaGrid.minX + cellX, metaGrid.minY + cellY);
            }

            return this.cells[index];
        } else {
            DebugLog.log("ERROR: GlobalObjectLookup.getCellForObject object location invalid " + squareX + "," + squareY);
            return null;
        }
    }

    private GlobalObjectLookup.Cell getCellForObject(GlobalObject object, boolean create) {
        return this.getCellAt(object.x, object.y, create);
    }

    private GlobalObjectLookup.Chunk getChunkForChunkPos(int wx, int wy, boolean create) {
        GlobalObjectLookup.Cell cell = this.getCellAt(wx * 8, wy * 8, create);
        return cell == null ? null : cell.getChunkAt(wx * 8, wy * 8, create);
    }

    public void addObject(GlobalObject object) {
        GlobalObjectLookup.Cell cell = this.getCellForObject(object, true);
        if (cell == null) {
            DebugLog.log("ERROR: GlobalObjectLookup.addObject object location invalid " + object.x + "," + object.y);
        } else {
            cell.addObject(object);
        }
    }

    public void removeObject(GlobalObject object) {
        GlobalObjectLookup.Cell cell = this.getCellForObject(object, false);
        if (cell == null) {
            DebugLog.log("ERROR: GlobalObjectLookup.removeObject object location invalid " + object.x + "," + object.y);
        } else {
            cell.removeObject(object);
        }
    }

    public GlobalObject getObjectAt(int x, int y, int z) {
        GlobalObjectLookup.Cell cell = this.getCellAt(x, y, false);
        if (cell == null) {
            return null;
        } else {
            GlobalObjectLookup.Chunk chunk = cell.getChunkAt(x, y, false);
            if (chunk == null) {
                return null;
            } else {
                for (int i = 0; i < chunk.objects.size(); i++) {
                    GlobalObject object = chunk.objects.get(i);
                    if (object.system == this.system && object.x == x && object.y == y && object.z == z) {
                        return object;
                    }
                }

                return null;
            }
        }
    }

    public boolean hasObjectsInChunk(int wx, int wy) {
        GlobalObjectLookup.Chunk chunk = this.getChunkForChunkPos(wx, wy, false);
        if (chunk == null) {
            return false;
        } else {
            for (int i = 0; i < chunk.objects.size(); i++) {
                GlobalObject object = chunk.objects.get(i);
                if (object.system == this.system) {
                    return true;
                }
            }

            return false;
        }
    }

    public ArrayList<GlobalObject> getObjectsInChunk(int wx, int wy, ArrayList<GlobalObject> objects) {
        GlobalObjectLookup.Chunk chunk = this.getChunkForChunkPos(wx, wy, false);
        if (chunk == null) {
            return objects;
        } else {
            for (int i = 0; i < chunk.objects.size(); i++) {
                GlobalObject object = chunk.objects.get(i);
                if (object.system == this.system) {
                    objects.add(object);
                }
            }

            return objects;
        }
    }

    public ArrayList<GlobalObject> getObjectsAdjacentTo(int x, int y, int z, ArrayList<GlobalObject> objects) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GlobalObject object = this.getObjectAt(x + dx, y + dy, z);
                if (object != null && object.system == this.system) {
                    objects.add(object);
                }
            }
        }

        return objects;
    }

    public static void init(IsoMetaGrid metaGrid) {
        GlobalObjectLookup.metaGrid = metaGrid;
        if (GameServer.server) {
            sharedServer.init(metaGrid);
        } else if (GameClient.client) {
            sharedClient.init(metaGrid);
        } else {
            sharedServer.init(metaGrid);
            sharedClient.init(metaGrid);
        }
    }

    public static void Reset() {
        sharedServer.reset();
        sharedClient.reset();
    }

    private static final class Cell {
        final int cx;
        final int cy;
        final GlobalObjectLookup.Chunk[] chunks = new GlobalObjectLookup.Chunk[1024];

        Cell(int cx, int cy) {
            this.cx = cx;
            this.cy = cy;
        }

        GlobalObjectLookup.Chunk getChunkAt(int squareX, int squareY, boolean create) {
            int x = (squareX - this.cx * 256) / 8;
            int y = (squareY - this.cy * 256) / 8;
            int index = x + y * 32;
            if (this.chunks[index] == null && create) {
                this.chunks[index] = new GlobalObjectLookup.Chunk();
            }

            return this.chunks[index];
        }

        GlobalObjectLookup.Chunk getChunkForObject(GlobalObject object, boolean create) {
            return this.getChunkAt(object.x, object.y, create);
        }

        void addObject(GlobalObject object) {
            GlobalObjectLookup.Chunk chunk = this.getChunkForObject(object, true);
            if (chunk.objects.contains(object)) {
                throw new IllegalStateException("duplicate object");
            } else {
                chunk.objects.add(object);
            }
        }

        void removeObject(GlobalObject object) {
            GlobalObjectLookup.Chunk chunk = this.getChunkForObject(object, false);
            if (chunk != null && chunk.objects.contains(object)) {
                chunk.objects.remove(object);
            } else {
                throw new IllegalStateException("chunk doesn't contain object");
            }
        }

        void Reset() {
            for (int i = 0; i < this.chunks.length; i++) {
                GlobalObjectLookup.Chunk chunk = this.chunks[i];
                if (chunk != null) {
                    chunk.Reset();
                }
            }

            Arrays.fill(this.chunks, null);
        }
    }

    private static final class Chunk {
        final ArrayList<GlobalObject> objects = new ArrayList<>();

        void Reset() {
            this.objects.clear();
        }
    }

    private static final class Shared {
        GlobalObjectLookup.Cell[] cells;

        void init(IsoMetaGrid metaGrid) {
            this.cells = new GlobalObjectLookup.Cell[metaGrid.getWidth() * metaGrid.getHeight()];
        }

        void reset() {
            if (this.cells != null) {
                for (int i = 0; i < this.cells.length; i++) {
                    GlobalObjectLookup.Cell cell = this.cells[i];
                    if (cell != null) {
                        cell.Reset();
                    }
                }

                this.cells = null;
            }
        }
    }
}
