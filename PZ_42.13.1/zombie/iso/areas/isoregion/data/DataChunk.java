// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.data;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class DataChunk {
    private final DataCell cell;
    private final int hashId;
    private final int chunkX;
    private final int chunkY;
    private int highestZ;
    private long lastUpdateStamp;
    private final boolean[] activeZLayers = new boolean[32];
    private final boolean[] dirtyZLayers = new boolean[32];
    private byte[] squareFlags;
    private byte[] regionIds;
    private final ArrayList<ArrayList<IsoChunkRegion>> chunkRegions = new ArrayList<>(32);
    private static byte selectedFlags;
    static final HashSet<IsoWorldRegion> tempWorldRegions = new HashSet<>();
    private static final ArrayDeque<DataSquarePos> tmpSquares = new ArrayDeque<>();
    private static final HashSet<Integer> tmpLinkedChunks = new HashSet<>();
    private static final boolean[] exploredPositions = new boolean[64];
    private static IsoChunkRegion lastCurRegion;
    private static IsoChunkRegion lastOtherRegionFullConnect;
    private static ArrayList<IsoChunkRegion> oldList = new ArrayList<>();
    private static final ArrayDeque<IsoChunkRegion> chunkQueue = new ArrayDeque<>();

    DataChunk(int chunkX, int chunkY, DataCell cell, int chunkID) {
        this.cell = cell;
        this.hashId = chunkID < 0 ? IsoRegions.hash(chunkX, chunkY) : chunkID;
        this.chunkX = chunkX;
        this.chunkY = chunkY;

        for (int i = 0; i < 32; i++) {
            this.chunkRegions.add(new ArrayList<>());
        }
    }

    int getHashId() {
        return this.hashId;
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public int getCellX() {
        return PZMath.fastfloor(this.getChunkX() / 32.0F);
    }

    public int getCellY() {
        return PZMath.fastfloor(this.getChunkY() / 32.0F);
    }

    ArrayList<IsoChunkRegion> getChunkRegions(int z) {
        return this.chunkRegions.get(z);
    }

    public long getLastUpdateStamp() {
        return this.lastUpdateStamp;
    }

    public void setLastUpdateStamp(long lastUpdateStamp) {
        this.lastUpdateStamp = lastUpdateStamp;
    }

    private boolean isDirty(int z) {
        return this.activeZLayers[z] ? this.dirtyZLayers[z] : false;
    }

    private void setDirty(int z) {
        if (this.activeZLayers[z]) {
            this.dirtyZLayers[z] = true;
            this.cell.dataRoot.EnqueueDirtyDataChunk(this);
        }
    }

    public void setDirtyAllActive() {
        boolean queued = false;

        for (int z = 0; z < 32; z++) {
            if (this.activeZLayers[z]) {
                this.dirtyZLayers[z] = true;
                if (!queued) {
                    this.cell.dataRoot.EnqueueDirtyDataChunk(this);
                    queued = true;
                }
            }
        }
    }

    void unsetDirtyAll() {
        Arrays.fill(this.dirtyZLayers, false);
    }

    private boolean validCoords(int x, int y, int z) {
        return x >= 0 && x < 8 && y >= 0 && y < 8 && z >= 0 && z < this.highestZ + 1;
    }

    private int getCoord1D(int x, int y, int z) {
        return z * 8 * 8 + y * 8 + x;
    }

    public byte getSquare(int x, int y, int z) {
        return this.getSquare(x, y, z, false);
    }

    public byte getSquare(int x, int y, int z, boolean ignoreCoordCheck) {
        if (this.squareFlags != null && (ignoreCoordCheck || this.validCoords(x, y, z))) {
            return this.activeZLayers[z] ? this.squareFlags[this.getCoord1D(x, y, z)] : -1;
        } else {
            return -1;
        }
    }

    private byte setOrAddSquare(int x, int y, int z, byte flags) {
        return this.setOrAddSquare(x, y, z, flags, false);
    }

    byte setOrAddSquare(int x, int y, int z, byte flags, boolean ignoreCoordCheck) {
        if (!ignoreCoordCheck && !this.validCoords(x, y, z)) {
            return -1;
        } else {
            this.ensureSquares(z);
            int id = this.getCoord1D(x, y, z);
            if (this.squareFlags[id] != flags) {
                this.setDirty(z);
            }

            this.squareFlags[id] = flags;
            return flags;
        }
    }

    private void ensureSquares(int zlayer) {
        if (zlayer >= 0 && zlayer < 32) {
            if (!this.activeZLayers[zlayer]) {
                this.ensureSquareArray(zlayer);
                this.activeZLayers[zlayer] = true;
                if (zlayer > this.highestZ) {
                    this.highestZ = zlayer;
                }

                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {
                        int index = this.getCoord1D(x, y, zlayer);
                        this.squareFlags[index] = (byte)(zlayer == 0 ? 16 : 0);
                    }
                }
            }
        }
    }

    private void ensureSquareArray(int zlayer) {
        int squareArraySize = (zlayer + 1) * 8 * 8;
        if (this.squareFlags == null || this.squareFlags.length < squareArraySize) {
            byte[] oldArray = this.squareFlags;
            byte[] oldRegions = this.regionIds;
            this.squareFlags = new byte[squareArraySize];
            this.regionIds = new byte[squareArraySize];
            if (oldArray != null) {
                for (int i = 0; i < oldArray.length; i++) {
                    this.squareFlags[i] = oldArray[i];
                    this.regionIds[i] = oldRegions[i];
                }
            }
        }
    }

    /**
     * SAVE/LOAD
     */
    public void save(ByteBuffer bb) {
        try {
            int startPos = bb.position();
            bb.putInt(0);
            bb.putInt(this.highestZ);
            int maxBytes = (this.highestZ + 1) * 8 * 8;
            bb.putInt(maxBytes);

            for (int i = 0; i < maxBytes; i++) {
                bb.put(this.squareFlags[i]);
            }

            int endPos = bb.position();
            bb.position(startPos);
            bb.putInt(endPos - startPos);
            bb.position(endPos);
        } catch (Exception var5) {
            DebugLog.log(var5.getMessage());
            var5.printStackTrace();
        }
    }

    public void load(ByteBuffer bb, int worldVersion, boolean readLength) {
        try {
            if (readLength) {
                bb.getInt();
            }

            this.highestZ = bb.getInt();

            for (int z = this.highestZ; z >= 0; z--) {
                this.ensureSquares(z);
            }

            int maxBytes = bb.getInt();

            for (int i = 0; i < maxBytes; i++) {
                this.squareFlags[i] = bb.get();
            }
        } catch (Exception var6) {
            DebugLog.log(var6.getMessage());
            var6.printStackTrace();
        }
    }

    public void setSelectedFlags(int x, int y, int z) {
        if (z >= 0 && z <= this.highestZ) {
            selectedFlags = this.squareFlags[this.getCoord1D(x, y, z)];
        } else {
            selectedFlags = -1;
        }
    }

    public boolean selectedHasFlags(byte flags) {
        return (selectedFlags & flags) == flags;
    }

    private boolean squareHasFlags(int x, int y, int z, byte flags) {
        return this.squareHasFlags(this.getCoord1D(x, y, z), flags);
    }

    private boolean squareHasFlags(int coord1D, byte flags) {
        int f = this.squareFlags[coord1D];
        return (f & flags) == flags;
    }

    public byte squareGetFlags(int x, int y, int z) {
        return this.squareGetFlags(this.getCoord1D(x, y, z));
    }

    private byte squareGetFlags(int coord1D) {
        return this.squareFlags[coord1D];
    }

    private void squareAddFlags(int x, int y, int z, byte flags) {
        this.squareAddFlags(this.getCoord1D(x, y, z), flags);
    }

    private void squareAddFlags(int coord1D, byte flags) {
        this.squareFlags[coord1D] = (byte)(this.squareFlags[coord1D] | flags);
    }

    private void squareRemoveFlags(int x, int y, int z, byte flags) {
        this.squareRemoveFlags(this.getCoord1D(x, y, z), flags);
    }

    private void squareRemoveFlags(int coord1D, byte flags) {
        this.squareFlags[coord1D] = (byte)(this.squareFlags[coord1D] ^ flags);
    }

    private boolean squareCanConnect(int x, int y, int z, byte dir) {
        return this.squareCanConnect(this.getCoord1D(x, y, z), z, dir);
    }

    private boolean squareCanConnect(int coord1D, int z, byte dir) {
        if (z >= 0 && z < this.highestZ + 1) {
            if (dir == 0) {
                return !this.squareHasFlags(coord1D, (byte)1);
            }

            if (dir == 1) {
                return !this.squareHasFlags(coord1D, (byte)2);
            }

            if (dir == 2) {
                return true;
            }

            if (dir == 3) {
                return true;
            }

            if (dir == 4) {
                return !this.squareHasFlags(coord1D, (byte)64);
            }

            if (dir == 5) {
                return !this.squareHasFlags(coord1D, (byte)16);
            }
        }

        return false;
    }

    public IsoChunkRegion getIsoChunkRegion(int x, int y, int z) {
        return this.getIsoChunkRegion(this.getCoord1D(x, y, z), z);
    }

    private IsoChunkRegion getIsoChunkRegion(int coord1D, int z) {
        if (z >= 0 && z < this.highestZ + 1) {
            byte id = this.regionIds[coord1D];
            if (id >= 0 && id < this.chunkRegions.get(z).size()) {
                return this.chunkRegions.get(z).get(id);
            }
        }

        return null;
    }

    public void setRegion(int x, int y, int z, byte regionIndex) {
        this.regionIds[this.getCoord1D(x, y, z)] = regionIndex;
    }

    void clearBuildingDefs(ArrayList<IsoGameCharacter.Location> changedCells) {
        tempWorldRegions.clear();

        for (int z = 0; z <= this.highestZ; z++) {
            if (this.dirtyZLayers[z] && this.activeZLayers[z]) {
                ArrayList<IsoChunkRegion> zRegions = this.chunkRegions.get(z);

                for (int i = zRegions.size() - 1; i >= 0; i--) {
                    IsoChunkRegion isoChunkRegion = zRegions.get(i);
                    IsoWorldRegion isoWorldRegion = isoChunkRegion.getIsoWorldRegion();
                    this.clearBuildingDefs(isoWorldRegion, changedCells, tempWorldRegions);
                }
            }
        }
    }

    void clearBuildingDefs(IsoWorldRegion isoWorldRegion, ArrayList<IsoGameCharacter.Location> changedCells, HashSet<IsoWorldRegion> done) {
        if (!done.contains(isoWorldRegion)) {
            done.add(isoWorldRegion);
            isoWorldRegion.clearBuildingDef(changedCells);

            for (IsoWorldRegion isoWorldRegion1 : isoWorldRegion.getNeighbors()) {
                this.clearBuildingDefs(isoWorldRegion1, changedCells, done);
            }
        }
    }

    void recalculate() {
        for (int z = 0; z <= this.highestZ; z++) {
            if (this.dirtyZLayers[z] && this.activeZLayers[z]) {
                this.recalculate(z);
            }
        }
    }

    private void recalculate(int z) {
        ArrayList<IsoChunkRegion> zRegions = this.chunkRegions.get(z);

        for (int i = zRegions.size() - 1; i >= 0; i--) {
            IsoChunkRegion isoChunkRegion = zRegions.get(i);
            IsoWorldRegion mr = isoChunkRegion.unlinkFromIsoWorldRegion();
            if (mr != null && mr.size() <= 0) {
                this.cell.dataRoot.regionManager.releaseIsoWorldRegion(mr);
            }

            this.cell.dataRoot.regionManager.releaseIsoChunkRegion(isoChunkRegion);
            zRegions.remove(i);
        }

        zRegions.clear();
        int zBlockSize = 64;
        Arrays.fill(this.regionIds, z * 64, z * 64 + 64, (byte)-1);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (this.regionIds[this.getCoord1D(x, y, z)] == -1) {
                    IsoChunkRegion var6 = this.floodFill(x, y, z);
                }
            }
        }
    }

    private IsoChunkRegion floodFill(int x, int y, int z) {
        IsoChunkRegion region = this.cell.dataRoot.regionManager.allocIsoChunkRegion(this, z);
        byte regionIndex = (byte)this.chunkRegions.get(z).size();
        this.chunkRegions.get(z).add(region);
        this.clearExploredPositions();
        tmpSquares.clear();
        tmpLinkedChunks.clear();
        tmpSquares.add(DataSquarePos.alloc(x, y, z));

        DataSquarePos dsPos;
        while ((dsPos = tmpSquares.poll()) != null) {
            int coord1D = this.getCoord1D(dsPos.x, dsPos.y, dsPos.z);
            this.setExploredPosition(coord1D, dsPos.z);
            if (this.regionIds[coord1D] == -1) {
                this.regionIds[coord1D] = regionIndex;
                region.addSquareCount();

                for (byte dir = 0; dir < 4; dir++) {
                    DataSquarePos neighbor = this.getNeighbor(dsPos, dir);
                    if (neighbor != null) {
                        int neighborCoord1D = this.getCoord1D(neighbor.x, neighbor.y, neighbor.z);
                        if (this.isExploredPosition(neighborCoord1D, neighbor.z)) {
                            DataSquarePos.release(neighbor);
                        } else {
                            if (this.squareCanConnect(coord1D, dsPos.z, dir)
                                && this.squareCanConnect(neighborCoord1D, neighbor.z, IsoRegions.GetOppositeDir(dir))) {
                                if (this.regionIds[neighborCoord1D] == -1) {
                                    tmpSquares.add(neighbor);
                                    this.setExploredPosition(neighborCoord1D, neighbor.z);
                                    continue;
                                }
                            } else {
                                IsoChunkRegion nc = this.getIsoChunkRegion(neighborCoord1D, neighbor.z);
                                if (nc != null && nc != region) {
                                    if (!tmpLinkedChunks.contains(nc.getID())) {
                                        region.addNeighbor(nc);
                                        nc.addNeighbor(region);
                                        tmpLinkedChunks.add(nc.getID());
                                    }

                                    this.setExploredPosition(neighborCoord1D, neighbor.z);
                                    DataSquarePos.release(neighbor);
                                    continue;
                                }
                            }

                            DataSquarePos.release(neighbor);
                        }
                    } else if (this.squareCanConnect(coord1D, dsPos.z, dir)) {
                        region.addChunkBorderSquaresCnt();
                    }
                }
            }
        }

        return region;
    }

    private boolean isExploredPosition(int coord1D, int z) {
        int index = coord1D - z * 8 * 8;
        return exploredPositions[index];
    }

    private void setExploredPosition(int coord1D, int z) {
        int index = coord1D - z * 8 * 8;
        exploredPositions[index] = true;
    }

    private void clearExploredPositions() {
        Arrays.fill(exploredPositions, false);
    }

    private DataSquarePos getNeighbor(DataSquarePos pos, byte dir) {
        int tmpx = pos.x;
        int tmpy = pos.y;
        if (dir == 1) {
            tmpx = pos.x - 1;
        } else if (dir == 3) {
            tmpx = pos.x + 1;
        }

        if (dir == 0) {
            tmpy = pos.y - 1;
        } else if (dir == 2) {
            tmpy = pos.y + 1;
        }

        return tmpx >= 0 && tmpx < 8 && tmpy >= 0 && tmpy < 8 ? DataSquarePos.alloc(tmpx, tmpy, pos.z) : null;
    }

    void link(DataChunk n, DataChunk w, DataChunk s, DataChunk e) {
        for (int z = 0; z <= this.highestZ; z++) {
            if (this.dirtyZLayers[z] && this.activeZLayers[z]) {
                this.linkRegionsOnSide(z, n, (byte)0);
                this.linkRegionsOnSide(z, w, (byte)1);
                this.linkRegionsOnSide(z, s, (byte)2);
                this.linkRegionsOnSide(z, e, (byte)3);
            }
        }
    }

    private void linkRegionsOnSide(int z, DataChunk opposite, byte dir) {
        int xStart;
        int xMax;
        int yStart;
        int yMax;
        if (dir != 0 && dir != 2) {
            xStart = dir == 1 ? 0 : 7;
            xMax = xStart + 1;
            yStart = 0;
            yMax = 8;
        } else {
            xStart = 0;
            xMax = 8;
            yStart = dir == 0 ? 0 : 7;
            yMax = yStart + 1;
        }

        if (opposite != null && opposite.isDirty(z)) {
            opposite.resetEnclosedSide(z, IsoRegions.GetOppositeDir(dir));
        }

        lastCurRegion = null;
        lastOtherRegionFullConnect = null;

        for (int y = yStart; y < yMax; y++) {
            for (int x = xStart; x < xMax; x++) {
                int x_other;
                int y_other;
                if (dir != 0 && dir != 2) {
                    x_other = dir == 1 ? 7 : 0;
                    y_other = y;
                } else {
                    x_other = x;
                    y_other = dir == 0 ? 7 : 0;
                }

                int coord1D = this.getCoord1D(x, y, z);
                int otherCoord1D = this.getCoord1D(x_other, y_other, z);
                IsoChunkRegion curRegion = this.getIsoChunkRegion(coord1D, z);
                IsoChunkRegion otherRegion = opposite != null ? opposite.getIsoChunkRegion(otherCoord1D, z) : null;
                if (curRegion == null) {
                    IsoRegions.warn("ds.getRegion()==null, shouldnt happen at this point.");
                } else {
                    if (lastCurRegion != null && lastCurRegion != curRegion) {
                        lastOtherRegionFullConnect = null;
                    }

                    if (lastCurRegion == null || lastCurRegion != curRegion || otherRegion == null || lastOtherRegionFullConnect != otherRegion) {
                        if (opposite != null && otherRegion != null) {
                            if (this.squareCanConnect(coord1D, z, dir) && opposite.squareCanConnect(otherCoord1D, z, IsoRegions.GetOppositeDir(dir))) {
                                curRegion.addConnectedNeighbor(otherRegion);
                                otherRegion.addConnectedNeighbor(curRegion);
                                curRegion.addNeighbor(otherRegion);
                                otherRegion.addNeighbor(curRegion);
                                if (!otherRegion.getIsEnclosed()) {
                                    otherRegion.setEnclosed(IsoRegions.GetOppositeDir(dir), true);
                                }

                                lastOtherRegionFullConnect = otherRegion;
                            } else {
                                curRegion.addNeighbor(otherRegion);
                                otherRegion.addNeighbor(curRegion);
                                if (!otherRegion.getIsEnclosed()) {
                                    otherRegion.setEnclosed(IsoRegions.GetOppositeDir(dir), true);
                                }

                                lastOtherRegionFullConnect = null;
                            }
                        } else if (this.squareCanConnect(coord1D, z, dir)) {
                            curRegion.setEnclosed(dir, false);
                        }

                        lastCurRegion = curRegion;
                    }
                }
            }
        }
    }

    private void resetEnclosedSide(int z, byte dir) {
        ArrayList<IsoChunkRegion> regions = this.chunkRegions.get(z);

        for (int i = 0; i < regions.size(); i++) {
            IsoChunkRegion r = regions.get(i);
            if (r.getzLayer() == z) {
                r.setEnclosed(dir, true);
            }
        }
    }

    void interConnect() {
        for (int z = 0; z <= this.highestZ; z++) {
            if (this.dirtyZLayers[z] && this.activeZLayers[z]) {
                ArrayList<IsoChunkRegion> regionList = this.chunkRegions.get(z);

                for (int i = 0; i < regionList.size(); i++) {
                    IsoChunkRegion region = regionList.get(i);
                    if (region.getzLayer() == z && region.getIsoWorldRegion() == null) {
                        if (region.getConnectedNeighbors().isEmpty()) {
                            IsoWorldRegion ms = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                            this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(ms);
                            ms.addIsoChunkRegion(region);
                        } else {
                            IsoChunkRegion neighbor = region.getConnectedNeighborWithLargestIsoWorldRegion();
                            if (neighbor == null) {
                                IsoWorldRegion ms = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                                this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(ms);
                                this.floodFillExpandWorldRegion(region, ms);
                                DataRoot.floodFills++;
                            } else {
                                IsoWorldRegion largestWorldRegion = neighbor.getIsoWorldRegion();
                                oldList.clear();
                                oldList = largestWorldRegion.swapIsoChunkRegions(oldList);

                                for (int k = 0; k < oldList.size(); k++) {
                                    IsoChunkRegion r = oldList.get(k);
                                    r.setIsoWorldRegion(null);
                                }

                                this.cell.dataRoot.regionManager.releaseIsoWorldRegion(largestWorldRegion);
                                IsoWorldRegion target = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                                this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(target);
                                this.floodFillExpandWorldRegion(region, target);

                                for (int k = 0; k < oldList.size(); k++) {
                                    IsoChunkRegion r = oldList.get(k);
                                    if (r.getIsoWorldRegion() == null) {
                                        IsoWorldRegion mr = this.cell.dataRoot.regionManager.allocIsoWorldRegion();
                                        this.cell.dataRoot.EnqueueDirtyIsoWorldRegion(mr);
                                        this.floodFillExpandWorldRegion(r, mr);
                                    }
                                }

                                DataRoot.floodFills++;
                            }
                        }
                    }
                }
            }
        }
    }

    private void floodFillExpandWorldRegion(IsoChunkRegion start, IsoWorldRegion worldRegion) {
        chunkQueue.add(start);

        IsoChunkRegion current;
        while ((current = chunkQueue.poll()) != null) {
            worldRegion.addIsoChunkRegion(current);
            if (!current.getConnectedNeighbors().isEmpty()) {
                for (int i = 0; i < current.getConnectedNeighbors().size(); i++) {
                    IsoChunkRegion neighbor = current.getConnectedNeighbors().get(i);
                    if (!chunkQueue.contains(neighbor)) {
                        if (neighbor.getIsoWorldRegion() == null) {
                            chunkQueue.add(neighbor);
                        } else if (neighbor.getIsoWorldRegion() != worldRegion) {
                            worldRegion.merge(neighbor.getIsoWorldRegion());
                        }
                    }
                }
            }
        }
    }

    void recalcRoofs() {
        if (this.highestZ >= 1) {
            for (int i = 0; i < this.chunkRegions.size(); i++) {
                for (int j = 0; j < this.chunkRegions.get(i).size(); j++) {
                    IsoChunkRegion c = this.chunkRegions.get(i).get(j);
                    c.resetRoofCnt();
                }
            }

            int z = this.highestZ;

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    byte flags = this.getSquare(x, y, z);
                    boolean hasroof = false;
                    if (flags > 0) {
                        hasroof = this.squareHasFlags(x, y, z, (byte)16);
                    }

                    if (z >= 1) {
                        for (int zz = z - 1; zz >= 0; zz--) {
                            flags = this.getSquare(x, y, zz);
                            IsoChunkRegion region = this.getIsoChunkRegion(x, y, zz);
                            IsoWorldRegion worldRegion = region == null ? null : region.getIsoWorldRegion();
                            boolean bEnclosedRegion = worldRegion != null && worldRegion.isEnclosed();
                            if (flags <= 0 && !bEnclosedRegion) {
                                hasroof = false;
                            } else {
                                hasroof = hasroof || this.squareHasFlags(x, y, zz, (byte)32);
                                if (hasroof) {
                                    if (region != null) {
                                        region.addRoof();
                                        if (region.getIsoWorldRegion() != null && !region.getIsoWorldRegion().isEnclosed()) {
                                            hasroof = false;
                                        }
                                    } else {
                                        hasroof = false;
                                    }
                                }

                                if (!hasroof) {
                                    hasroof = this.squareHasFlags(x, y, zz, (byte)16);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
