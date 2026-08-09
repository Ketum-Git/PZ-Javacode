// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.characters.IsoGameCharacter;
import zombie.core.Colors;
import zombie.core.math.PZMath;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.metagrid.WorldRegionToMetaGrid;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoRegionManager;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

/**
 * TurboTuTone.
 */
public final class DataRoot {
    private final Map<Integer, DataCell> cellMap = new HashMap<>();
    public final DataRoot.SelectInfo select = new DataRoot.SelectInfo(this);
    private final DataRoot.SelectInfo selectInternal = new DataRoot.SelectInfo(this);
    public final IsoRegionManager regionManager;
    private final ArrayList<IsoWorldRegion> dirtyIsoWorldRegions = new ArrayList<>();
    private final ArrayList<DataChunk> dirtyChunks = new ArrayList<>();
    private final ArrayList<IsoGameCharacter.Location> changedCells = new ArrayList<>();
    protected static int recalcs;
    protected static int floodFills;
    protected static int merges;
    private static final long[] t_start = new long[5];
    private static final long[] t_end = new long[5];
    private static final long[] t_time = new long[5];
    private static final WorldRegionToMetaGrid wrtmg = new WorldRegionToMetaGrid();

    public DataRoot() {
        this.regionManager = new IsoRegionManager(this);
    }

    public void getAllChunks(List<DataChunk> list) {
        for (Entry<Integer, DataCell> entry : this.cellMap.entrySet()) {
            entry.getValue().getAllChunks(list);
        }
    }

    private DataCell getCell(int cellID) {
        return this.cellMap.get(cellID);
    }

    private DataCell addCell(int cellID) {
        DataCell cell = new DataCell(this);
        this.cellMap.put(cellID, cell);
        return cell;
    }

    public DataChunk getDataChunk(int chunkx, int chunky) {
        int cellID = IsoRegions.hash(chunkx / 32, chunky / 32);
        DataCell dataCell = this.cellMap.get(cellID);
        if (dataCell != null) {
            int chunkID = IsoRegions.hash(chunkx, chunky);
            return dataCell.getChunk(chunkID);
        } else {
            return null;
        }
    }

    private void setDataChunk(DataChunk chunk) {
        int cellID = IsoRegions.hash(chunk.getCellX(), chunk.getCellY());
        DataCell dataCell = this.cellMap.get(cellID);
        if (dataCell == null) {
            dataCell = this.addCell(cellID);
        }

        dataCell.setChunk(chunk);
    }

    public IsoWorldRegion getIsoWorldRegion(int x, int y, int z) {
        this.selectInternal.reset(x, y, z, false);
        if (this.selectInternal.chunk != null) {
            IsoChunkRegion cr = this.selectInternal.chunk.getIsoChunkRegion(this.selectInternal.chunkSquareX, this.selectInternal.chunkSquareY, z);
            if (cr != null) {
                return cr.getIsoWorldRegion();
            }
        }

        return null;
    }

    public ArrayList<IsoWorldRegion> getIsoWorldRegionsInCell(int cellX, int cellY, ArrayList<IsoWorldRegion> worldRegions) {
        worldRegions.clear();
        DataCell dataCell = this.getCell(IsoRegions.hash(cellX, cellY));
        if (dataCell == null) {
            return worldRegions;
        } else {
            ArrayList<DataChunk> dataChunks = new ArrayList<>();
            dataCell.getAllChunks(dataChunks);

            for (DataChunk dataChunk : dataChunks) {
                for (int z = 0; z < 32; z++) {
                    for (IsoChunkRegion chunkRegion : dataChunk.getChunkRegions(z)) {
                        IsoWorldRegion worldRegion = chunkRegion.getIsoWorldRegion();
                        if (!worldRegions.contains(worldRegion)) {
                            worldRegions.add(worldRegion);
                        }
                    }
                }
            }

            return worldRegions;
        }
    }

    public byte getSquareFlags(int x, int y, int z) {
        this.selectInternal.reset(x, y, z, false);
        return this.selectInternal.square;
    }

    public IsoChunkRegion getIsoChunkRegion(int x, int y, int z) {
        this.selectInternal.reset(x, y, z, false);
        return this.selectInternal.chunk != null
            ? this.selectInternal.chunk.getIsoChunkRegion(this.selectInternal.chunkSquareX, this.selectInternal.chunkSquareY, z)
            : null;
    }

    public void resetAllData() {
        ArrayList<IsoWorldRegion> isoWorldRegions = new ArrayList<>();

        for (Entry<Integer, DataCell> entry : this.cellMap.entrySet()) {
            DataCell cell = entry.getValue();

            for (Entry<Integer, DataChunk> chunkEntry : cell.dataChunks.entrySet()) {
                DataChunk chunk = chunkEntry.getValue();

                for (int z = 0; z < 32; z++) {
                    for (IsoChunkRegion region : chunk.getChunkRegions(z)) {
                        if (region.getIsoWorldRegion() != null && !isoWorldRegions.contains(region.getIsoWorldRegion())) {
                            isoWorldRegions.add(region.getIsoWorldRegion());
                        }

                        region.setIsoWorldRegion(null);
                        this.regionManager.releaseIsoChunkRegion(region);
                    }
                }
            }

            cell.dataChunks.clear();
        }

        this.cellMap.clear();

        for (IsoWorldRegion m : isoWorldRegions) {
            this.regionManager.releaseIsoWorldRegion(m);
        }
    }

    public void EnqueueDirtyDataChunk(DataChunk chunk) {
        if (!this.dirtyChunks.contains(chunk)) {
            this.dirtyChunks.add(chunk);
        }
    }

    public void EnqueueDirtyIsoWorldRegion(IsoWorldRegion mr) {
        if (!this.dirtyIsoWorldRegions.contains(mr)) {
            this.dirtyIsoWorldRegions.add(mr);
        }
    }

    public void DequeueDirtyIsoWorldRegion(IsoWorldRegion mr) {
        this.dirtyIsoWorldRegions.remove(mr);
    }

    public void updateExistingSquare(int x, int y, int z, byte flags) {
        this.select.reset(x, y, z, false);
        if (this.select.chunk != null) {
            byte bufferFlags = -1;
            if (this.select.square != -1) {
                bufferFlags = this.select.square;
            }

            if (flags == bufferFlags) {
                return;
            }

            this.select.chunk.setOrAddSquare(this.select.chunkSquareX, this.select.chunkSquareY, this.select.z, flags, true);
        } else {
            IsoRegions.warn("DataRoot.updateExistingSquare -> trying to change a square on a unknown chunk");
        }
    }

    public void processDirtyChunks() {
        HashSet<IsoWorldRegion> done = new HashSet<>();
        IsoGameCharacter.Location tempLocation = new IsoGameCharacter.Location();
        if (!this.dirtyChunks.isEmpty()) {
            long start = System.nanoTime();
            recalcs = 0;
            floodFills = 0;
            merges = 0;
            t_start[0] = System.nanoTime();

            for (int i = 0; i < this.dirtyChunks.size(); i++) {
                DataChunk chunk = this.dirtyChunks.get(i);
                chunk.clearBuildingDefs(this.changedCells);
                tempLocation.set(chunk.getCellX(), chunk.getCellY(), 0);
                if (!this.changedCells.contains(tempLocation)) {
                    IsoGameCharacter.Location location = new IsoGameCharacter.Location(chunk.getCellX(), chunk.getCellY(), 0);
                    this.changedCells.add(location);
                }
            }

            for (int ix = 0; ix < this.dirtyChunks.size(); ix++) {
                DataChunk chunk = this.dirtyChunks.get(ix);
                chunk.recalculate();
                recalcs++;
            }

            t_end[0] = System.nanoTime();
            t_start[1] = System.nanoTime();

            for (int ix = 0; ix < this.dirtyChunks.size(); ix++) {
                DataChunk chunk = this.dirtyChunks.get(ix);
                DataChunk n = this.getDataChunk(chunk.getChunkX(), chunk.getChunkY() - 1);
                DataChunk w = this.getDataChunk(chunk.getChunkX() - 1, chunk.getChunkY());
                DataChunk s = this.getDataChunk(chunk.getChunkX(), chunk.getChunkY() + 1);
                DataChunk e = this.getDataChunk(chunk.getChunkX() + 1, chunk.getChunkY());
                chunk.link(n, w, s, e);
            }

            t_end[1] = System.nanoTime();
            t_start[2] = System.nanoTime();

            for (int ix = 0; ix < this.dirtyChunks.size(); ix++) {
                DataChunk chunk = this.dirtyChunks.get(ix);
                chunk.interConnect();
            }

            t_end[2] = System.nanoTime();
            t_start[3] = System.nanoTime();

            for (int ix = 0; ix < this.dirtyChunks.size(); ix++) {
                DataChunk chunk = this.dirtyChunks.get(ix);
                chunk.recalcRoofs();
                chunk.unsetDirtyAll();
            }

            t_end[3] = System.nanoTime();
            t_start[4] = System.nanoTime();
            if (!this.dirtyIsoWorldRegions.isEmpty()) {
                for (int ix = 0; ix < this.dirtyIsoWorldRegions.size(); ix++) {
                    IsoWorldRegion worldRegion = this.dirtyIsoWorldRegions.get(ix);
                    worldRegion.unlinkNeighbors();
                }

                for (int ix = 0; ix < this.dirtyIsoWorldRegions.size(); ix++) {
                    IsoWorldRegion worldRegion = this.dirtyIsoWorldRegions.get(ix);
                    worldRegion.linkNeighbors();
                }

                for (int ix = 0; ix < this.dirtyIsoWorldRegions.size(); ix++) {
                    IsoWorldRegion worldRegion = this.dirtyIsoWorldRegions.get(ix);
                    if (!done.contains(worldRegion)) {
                        tempLocation.set(worldRegion.getCellX(), worldRegion.getCellY(), 0);
                        if (!this.changedCells.contains(tempLocation)) {
                            IsoGameCharacter.Location location = new IsoGameCharacter.Location(worldRegion.getCellX(), worldRegion.getCellY(), 0);
                            this.changedCells.add(location);
                        }

                        done.add(worldRegion);
                    }

                    for (int j = 0; j < worldRegion.getNeighbors().size(); j++) {
                        IsoWorldRegion worldRegion1 = worldRegion.getNeighbors().get(j);
                        if (!done.contains(worldRegion1)) {
                            tempLocation.set(worldRegion.getCellX(), worldRegion.getCellY(), 0);
                            if (!this.changedCells.contains(tempLocation)) {
                                IsoGameCharacter.Location location = new IsoGameCharacter.Location(worldRegion1.getCellX(), worldRegion1.getCellY(), 0);
                                this.changedCells.add(location);
                            }

                            done.add(worldRegion1);
                        }
                    }
                }

                this.dirtyIsoWorldRegions.clear();
            }

            t_end[4] = System.nanoTime();
            this.dirtyChunks.clear();
            long end = System.nanoTime();
            long nanoSeconds = end - start;
            if (IsoRegions.printD) {
                t_time[0] = t_end[0] - t_start[0];
                t_time[1] = t_end[1] - t_start[1];
                t_time[2] = t_end[2] - t_start[2];
                t_time[3] = t_end[3] - t_start[3];
                t_time[4] = t_end[4] - t_start[4];
                IsoRegions.log(
                    "--- IsoRegion update: "
                        + String.format("%.6f", nanoSeconds / 1000000.0)
                        + " ms, recalc: "
                        + String.format("%.6f", t_time[0] / 1000000.0)
                        + " ms, link: "
                        + String.format("%.6f", t_time[1] / 1000000.0)
                        + " ms, interconnect: "
                        + String.format("%.6f", t_time[2] / 1000000.0)
                        + " ms, roofs: "
                        + String.format("%.6f", t_time[3] / 1000000.0)
                        + " ms, worldRegion: "
                        + String.format("%.6f", t_time[4] / 1000000.0)
                        + " ms, recalcs = "
                        + recalcs
                        + ", merges = "
                        + merges
                        + ", floodfills = "
                        + floodFills,
                    Colors.CornFlowerBlue
                );
            }
        }
    }

    public void clientProcessBuildings() {
        wrtmg.clientProcessBuildings(this.changedCells);
        this.changedCells.clear();
    }

    public static final class SelectInfo {
        public int x;
        public int y;
        public int z;
        public int chunkSquareX;
        public int chunkSquareY;
        public int chunkx;
        public int chunky;
        public int cellx;
        public int celly;
        public int chunkId;
        public int cellId;
        public DataCell cell;
        public DataChunk chunk;
        public byte square;
        private final DataRoot root;

        private SelectInfo(DataRoot root) {
            this.root = root;
        }

        public void reset(int x, int y, int z, boolean createSquare) {
            this.reset(x, y, z, createSquare, createSquare);
        }

        public void reset(int x, int y, int z, boolean createChunk, boolean createSquare) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.chunkSquareX = PZMath.coordmodulo(x, 8);
            this.chunkSquareY = PZMath.coordmodulo(y, 8);
            this.chunkx = x / 8;
            this.chunky = y / 8;
            this.cellx = x / 256;
            this.celly = y / 256;
            this.chunkId = IsoRegions.hash(this.chunkx, this.chunky);
            this.cellId = IsoRegions.hash(this.cellx, this.celly);
            this.cell = null;
            this.chunk = null;
            this.square = -1;
            this.ensureSquare(createSquare);
            if (this.chunk == null && createChunk) {
                this.ensureChunk(createChunk);
            }
        }

        private void ensureCell(boolean create) {
            if (this.cell == null) {
                this.cell = this.root.getCell(this.cellId);
            }

            if (this.cell == null && create) {
                this.cell = this.root.addCell(this.cellId);
            }
        }

        private void ensureChunk(boolean create) {
            this.ensureCell(create);
            if (this.cell != null) {
                if (this.chunk == null) {
                    this.chunk = this.cell.getChunk(this.chunkId);
                }

                if (this.chunk == null && create) {
                    this.chunk = this.cell.addChunk(this.chunkx, this.chunky, this.chunkId);
                }
            }
        }

        private void ensureSquare(boolean create) {
            this.ensureCell(create);
            if (this.cell != null) {
                this.ensureChunk(create);
                if (this.chunk != null) {
                    if (this.square == -1) {
                        this.square = this.chunk.getSquare(this.chunkSquareX, this.chunkSquareY, this.z, true);
                    }

                    if (this.square == -1 && create) {
                        this.square = this.chunk.setOrAddSquare(this.chunkSquareX, this.chunkSquareY, this.z, (byte)0, true);
                    }
                }
            }
        }
    }
}
