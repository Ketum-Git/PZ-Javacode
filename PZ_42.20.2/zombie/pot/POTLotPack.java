// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pot;

import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.SliceY;
import zombie.util.BufferedRandomAccessFile;

public final class POTLotPack {
    static File lastFile;
    static RandomAccessFile in;
    public final POTLotHeader lotHeader;
    public final boolean pot;
    public final int chunkDim;
    public final int chunksPerCell;
    public final int cellDim;
    public final int x;
    public final int y;
    int version;
    final boolean[] loadedChunks;
    final int[] offsetInData;
    final TIntArrayList data = new TIntArrayList();

    POTLotPack(POTLotHeader lotHeader) {
        this.lotHeader = lotHeader;
        this.pot = lotHeader.pot;
        this.x = lotHeader.x;
        this.y = lotHeader.y;
        this.chunkDim = this.pot ? 8 : 10;
        this.chunksPerCell = this.pot ? 32 : 30;
        this.cellDim = this.pot ? 256 : 300;
        this.loadedChunks = new boolean[this.chunksPerCell * this.chunksPerCell];
        this.offsetInData = new int[this.cellDim * this.cellDim * (lotHeader.maxLevel - lotHeader.minLevel + 1)];
        Arrays.fill(this.offsetInData, -1);
    }

    void clear() {
        this.data.clear();
    }

    void load(File file) throws IOException {
        if (in == null || lastFile != file) {
            if (in != null) {
                in.close();
            }

            System.out.println(file.getPath());
            in = new BufferedRandomAccessFile(file, "r", 4096);
            lastFile = file;
        }

        in.seek(0L);
        byte[] magic = new byte[4];
        in.read(magic, 0, 4);
        boolean bHasMagic = Arrays.equals(magic, LotHeader.LOTPACK_MAGIC);
        if (bHasMagic) {
            this.version = IsoLot.readInt(in);
            if (this.version < 0 || this.version > 1) {
                throw new IOException("Unsupported version " + this.version);
            }
        } else {
            in.seek(0L);
            this.version = 0;
        }

        for (int chunkX = 0; chunkX < this.chunksPerCell; chunkX++) {
            for (int chunkY = 0; chunkY < this.chunksPerCell; chunkY++) {
                this.loadChunk(this.x * this.chunksPerCell + chunkX, this.y * this.chunksPerCell + chunkY);
            }
        }
    }

    void loadChunk(int chunkX, int chunkY) throws IOException {
        int skip = 0;
        int lwx = chunkX - this.x * this.chunksPerCell;
        int lwy = chunkY - this.y * this.chunksPerCell;
        int index = lwx * this.chunksPerCell + lwy;
        in.seek((this.version >= 1 ? 8 : 0) + 4 + index * 8L);
        int pos = IsoLot.readInt(in);
        in.seek(pos);
        int minZ = Math.max(this.lotHeader.minLevel, -32);
        int maxZ = Math.min(this.lotHeader.maxLevel, 31);
        if (this.version == 0) {
            maxZ--;
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = 0; x < this.chunkDim; x++) {
                for (int y = 0; y < this.chunkDim; y++) {
                    int squareXYZ = x + y * this.cellDim;
                    squareXYZ += lwx * this.chunkDim + lwy * this.chunkDim * this.cellDim + (z - this.lotHeader.minLevel) * this.cellDim * this.cellDim;
                    this.offsetInData[squareXYZ] = -1;
                    if (skip > 0) {
                        skip--;
                    } else {
                        int count = IsoLot.readInt(in);
                        if (count == -1) {
                            skip = IsoLot.readInt(in);
                            if (skip > 0) {
                                skip--;
                                continue;
                            }
                        }

                        if (count > 1) {
                            this.offsetInData[squareXYZ] = this.data.size();
                            this.data.add(count - 1);
                            int roomID = IsoLot.readInt(in);

                            for (int n = 1; n < count; n++) {
                                int d = IsoLot.readInt(in);
                                this.data.add(d);
                            }
                        }
                    }
                }
            }
        }
    }

    void save(String fileName) throws IOException {
        int numChunks = this.chunksPerCell * this.chunksPerCell;
        ByteBuffer bb = SliceY.SliceBuffer;
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.clear();
        bb.put(LotHeader.LOTPACK_MAGIC);
        bb.putInt(1);
        bb.putInt(this.chunkDim);
        int chunkTableStart = bb.position();
        bb.position(chunkTableStart + numChunks * 8);

        for (int chunkX = 0; chunkX < this.chunksPerCell; chunkX++) {
            for (int chunkY = 0; chunkY < this.chunksPerCell; chunkY++) {
                this.saveChunk(bb, chunkTableStart, chunkX, chunkY);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(bb.array(), 0, bb.position());
        }
    }

    void saveChunk(ByteBuffer bb, int chunkTableStart, int chunkX, int chunkY) {
        bb.putLong(chunkTableStart + (chunkX * this.chunksPerCell + chunkY) * 8, bb.position());
        int notdonecount = 0;

        for (int z = this.lotHeader.minLevelNotEmpty; z <= this.lotHeader.maxLevelNotEmpty; z++) {
            for (int x = 0; x < this.chunkDim; x++) {
                for (int y = 0; y < this.chunkDim; y++) {
                    int squareXYZ = x + y * this.cellDim + (z - this.lotHeader.minLevel) * this.cellDim * this.cellDim;
                    squareXYZ += chunkX * this.chunkDim + chunkY * this.chunkDim * this.cellDim;
                    int offset = this.offsetInData[squareXYZ];
                    if (offset == -1) {
                        notdonecount++;
                    } else {
                        if (notdonecount > 0) {
                            bb.putInt(-1);
                            bb.putInt(notdonecount);
                            notdonecount = 0;
                        }

                        int numTiles = this.data.getQuick(offset);
                        bb.putInt(numTiles + 1);
                        int roomID = -1;
                        bb.putInt(-1);

                        for (int i = 0; i < numTiles; i++) {
                            bb.putInt(this.data.getQuick(offset + 1 + i));
                        }
                    }
                }
            }
        }

        if (notdonecount > 0) {
            bb.putInt(-1);
            bb.putInt(notdonecount);
        }
    }

    String[] getSquareData(int squareX, int squareY, int z) {
        squareX -= this.lotHeader.getMinSquareX();
        squareY -= this.lotHeader.getMinSquareY();
        int squareXYZ = squareX + squareY * this.cellDim + (z - this.lotHeader.minLevel) * this.cellDim * this.cellDim;
        int offset = this.offsetInData[squareXYZ];
        if (offset == -1) {
            return null;
        } else {
            int count = this.data.getQuick(offset);
            String[] result = new String[count];

            for (int i = 0; i < count; i++) {
                result[i] = this.lotHeader.tilesUsed.get(this.data.getQuick(offset + 1 + i));
            }

            return result;
        }
    }

    void setSquareData(int squareX, int squareY, int z, String[] data) {
        if (z >= this.lotHeader.minLevel && z <= this.lotHeader.maxLevel) {
            squareX -= this.lotHeader.getMinSquareX();
            squareY -= this.lotHeader.getMinSquareY();
            int squareXYZ = squareX + squareY * this.cellDim + (z - this.lotHeader.minLevel) * this.cellDim * this.cellDim;
            if (data != null && data.length != 0) {
                this.offsetInData[squareXYZ] = this.data.size();
                this.data.add(data.length);

                for (String tileName : data) {
                    this.data.add(this.lotHeader.getTileIndex(tileName));
                }

                this.lotHeader.minLevelNotEmpty = Math.min(this.lotHeader.minLevelNotEmpty, z);
                this.lotHeader.maxLevelNotEmpty = Math.max(this.lotHeader.maxLevelNotEmpty, z);
            } else {
                this.offsetInData[squareXYZ] = -1;
            }
        }
    }
}
