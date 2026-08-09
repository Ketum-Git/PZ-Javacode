// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public final class POTChunkData {
    static final int FILE_VERSION = 1;
    static final int BIT_SOLID = 1;
    static final int BIT_WALLN = 2;
    static final int BIT_WALLW = 4;
    static final int BIT_WATER = 8;
    static final int BIT_ROOM = 16;
    static final int EMPTY_CHUNK = 0;
    static final int SOLID_CHUNK = 1;
    static final int REGULAR_CHUNK = 2;
    static final int WATER_CHUNK = 3;
    static final int ROOM_CHUNK = 4;
    static final int NUM_CHUNK_TYPES = 5;
    public final boolean pot;
    public final int chunkDim;
    public final int chunksPerCell;
    public final int cellDim;
    public final int x;
    public final int y;
    public final POTChunkData.Chunk[] chunks;

    POTChunkData(int x, int y, boolean pot) {
        this.chunkDim = pot ? 8 : 10;
        this.chunksPerCell = pot ? 32 : 30;
        this.cellDim = pot ? 256 : 300;
        this.pot = pot;
        this.x = x;
        this.y = y;
        this.chunks = new POTChunkData.Chunk[this.chunksPerCell * this.chunksPerCell];

        for (int i = 0; i < this.chunks.length; i++) {
            this.chunks[i] = new POTChunkData.Chunk();
        }
    }

    void load(File file) throws IOException {
        try (
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
        ) {
            int version = dis.readShort();

            assert version == 1;

            for (int y = 0; y < this.chunksPerCell; y++) {
                for (int x = 0; x < this.chunksPerCell; x++) {
                    this.chunks[x + y * this.chunksPerCell].load(dis);
                }
            }
        }
    }

    void save(String fileName) throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(fileName);
            DataOutputStream dos = new DataOutputStream(fos);
        ) {
            dos.writeShort(1);

            for (int y = 0; y < this.chunksPerCell; y++) {
                for (int x = 0; x < this.chunksPerCell; x++) {
                    this.chunks[x + y * this.chunksPerCell].save(dos);
                }
            }
        }
    }

    int getMinSquareX() {
        return this.x * this.cellDim;
    }

    int getMinSquareY() {
        return this.y * this.cellDim;
    }

    int getMaxSquareX() {
        return (this.x + 1) * this.cellDim - 1;
    }

    int getMaxSquareY() {
        return (this.y + 1) * this.cellDim - 1;
    }

    boolean containsSquare(int squareX, int squareY) {
        return squareX >= this.getMinSquareX() && squareX <= this.getMaxSquareX() && squareY >= this.getMinSquareY() && squareY <= this.getMaxSquareY();
    }

    byte getSquareBits(int squareX, int squareY) {
        if (!this.containsSquare(squareX, squareY)) {
            return 0;
        } else {
            int chunkX = (squareX - this.getMinSquareX()) / this.chunkDim;
            int chunkY = (squareY - this.getMinSquareY()) / this.chunkDim;
            POTChunkData.Chunk chunk = this.chunks[chunkX + chunkY * this.chunksPerCell];
            return chunk.getBits((squareX - this.getMinSquareX()) % this.chunkDim, (squareY - this.getMinSquareY()) % this.chunkDim);
        }
    }

    void setSquareBits(int squareX, int squareY, byte bits) {
        int chunkX = (squareX - this.getMinSquareX()) / this.chunkDim;
        int chunkY = (squareY - this.getMinSquareY()) / this.chunkDim;
        POTChunkData.Chunk chunk = this.chunks[chunkX + chunkY * this.chunksPerCell];
        chunk.setBits((squareX - this.getMinSquareX()) % this.chunkDim, (squareY - this.getMinSquareY()) % this.chunkDim, bits);
    }

    final class Chunk {
        public final int[] counts;
        public byte[] bits;
        final int nSqrs;

        Chunk() {
            Objects.requireNonNull(POTChunkData.this);
            super();
            this.nSqrs = POTChunkData.this.chunkDim * POTChunkData.this.chunkDim;
            this.counts = new int[5];
            this.counts[0] = this.nSqrs;
        }

        void load(DataInputStream dis) throws IOException {
            Arrays.fill(this.counts, 0);
            int type = dis.readByte();
            if (type != 0 && type != 1 && type != 3 && type != 4) {
                assert type == 2;

                this.bits = new byte[this.nSqrs];

                for (int i = 0; i < this.nSqrs; i++) {
                    this.bits[i] = dis.readByte();
                    this.counts[this.getTypeOf(this.bits[i])]++;
                }
            } else {
                this.counts[type] = this.nSqrs;
            }
        }

        void save(DataOutputStream dos) throws IOException {
            int type = this.getType();
            dos.writeByte(type);
            if (type == 2) {
                dos.write(this.bits);
            }
        }

        byte getBits(int x, int y) {
            if (this.counts[0] == this.nSqrs) {
                return 0;
            } else if (this.counts[1] == this.nSqrs) {
                return 1;
            } else if (this.counts[3] == this.nSqrs) {
                return 8;
            } else {
                return this.counts[4] == this.nSqrs ? 16 : this.bits[x + y * POTChunkData.this.chunkDim];
            }
        }

        byte setBits(int x, int y, byte bits) {
            byte bitsOld = this.getBits(x, y);
            int typeOld = this.getTypeOf(bitsOld);
            int typeNew = this.getTypeOf(bits);
            if (typeOld == typeNew && typeOld != 2) {
                return bits;
            } else {
                assert this.counts[typeOld] > 0;

                assert this.counts[typeNew] < this.nSqrs;

                this.counts[typeOld]--;
                this.counts[typeNew]++;
                if (this.getType() == 2) {
                    if (this.bits == null) {
                        this.bits = new byte[this.nSqrs];
                        Arrays.fill(this.bits, bitsOld);
                    }

                    this.bits[x + y * POTChunkData.this.chunkDim] = bits;
                } else {
                    this.bits = null;
                }

                return bitsOld;
            }
        }

        int getType() {
            if (this.counts[0] == this.nSqrs) {
                return 0;
            } else if (this.counts[1] == this.nSqrs) {
                return 1;
            } else if (this.counts[3] == this.nSqrs) {
                return 3;
            } else {
                return this.counts[4] == this.nSqrs ? 4 : 2;
            }
        }

        int getTypeOf(byte bits) {
            if (bits == 0) {
                return 0;
            } else if (bits == 1) {
                return 1;
            } else if (bits == 8) {
                return 3;
            } else {
                return bits == 16 ? 4 : 2;
            }
        }
    }
}
