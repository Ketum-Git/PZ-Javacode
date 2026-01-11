// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.ChunkMapFilenames;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.popman.ObjectPool;
import zombie.util.BufferedRandomAccessFile;

@UsedFromLua
public class IsoLot {
    public static final HashMap<String, LotHeader> InfoHeaders = new HashMap<>();
    public static final ArrayList<String> InfoHeaderNames = new ArrayList<>();
    public static final HashMap<String, String> InfoFileNames = new HashMap<>();
    public static final HashMap<String, ChunkGenerationStatus> InfoFileModded = new HashMap<>();
    public static final ArrayList<MapFiles> MapFiles = new ArrayList<>();
    public static final ObjectPool<IsoLot> pool = new ObjectPool<>(IsoLot::new);
    public int maxLevel;
    public int minLevel;
    private String lastUsedPath = "";
    public int wx;
    public int wy;
    public final int[] offsetInData = new int[4096];
    public final TIntArrayList data = new TIntArrayList();
    private RandomAccessFile in;
    private int version;
    public LotHeader info;

    public static void Dispose() {
        for (MapFiles mapFiles : MapFiles) {
            mapFiles.Dispose();
        }

        MapFiles.clear();
        InfoHeaders.clear();
        InfoHeaderNames.clear();
        InfoFileNames.clear();
        InfoFileModded.clear();
        pool.forEach(lot -> {
            RandomAccessFile raf = lot.in;
            if (raf != null) {
                lot.in = null;

                try {
                    raf.close();
                } catch (IOException var3) {
                    ExceptionLogger.logException(var3);
                }
            }
        });
    }

    public static String readString(BufferedRandomAccessFile in) throws EOFException, IOException {
        return in.getNextLine();
    }

    public static int readInt(RandomAccessFile in) throws EOFException, IOException {
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

    public static int readShort(RandomAccessFile in) throws EOFException, IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        } else {
            return (ch1 << 0) + (ch2 << 8);
        }
    }

    public static synchronized void put(IsoLot lot) {
        lot.info = null;
        lot.data.resetQuick();
        pool.release(lot);
    }

    public static synchronized IsoLot get(MapFiles mapFiles, int cX, int cY, int wX, int wY, IsoChunk ch) {
        return get(mapFiles, cX, cY, wX, Integer.valueOf(wY), ch);
    }

    public static synchronized IsoLot get(MapFiles mapFiles, Integer cX, Integer cY, Integer wX, Integer wY, IsoChunk ch) {
        IsoLot l = pool.alloc();
        l.load(mapFiles, cX, cY, wX, wY, ch);
        return l;
    }

    public void loadNew(int cX, int cY, int wX, int wY, IsoChunk ch) {
    }

    public void load(MapFiles mapFiles, Integer cX, Integer cY, Integer wX, Integer wY, IsoChunk ch) {
        String filenameheader = ChunkMapFilenames.instance.getHeader(cX, cY);
        this.info = mapFiles.infoHeaders.get(filenameheader);
        this.wx = wX;
        this.wy = wY;
        if (ch != null && this.info == InfoHeaders.get(filenameheader)) {
            ch.lotheader = this.info;
        }

        try {
            filenameheader = "world_" + cX + "_" + cY + ".lotpack";
            File fo = new File(mapFiles.infoFileNames.get(filenameheader));
            if (this.in == null || !this.lastUsedPath.equals(fo.getAbsolutePath())) {
                if (this.in != null) {
                    this.in.close();
                }

                this.in = new BufferedRandomAccessFile(fo.getAbsolutePath(), "r", 4096);
                this.lastUsedPath = fo.getAbsolutePath();
                byte[] magic = new byte[4];
                this.in.read(magic, 0, 4);
                boolean bHasMagic = Arrays.equals(magic, LotHeader.LOTPACK_MAGIC);
                if (bHasMagic) {
                    this.version = readInt(this.in);
                    if (this.version < 0 || this.version > 1) {
                        throw new IOException("Unsupported version " + this.version);
                    }
                } else {
                    this.in.seek(0L);
                    this.version = 0;
                }
            }

            int skip = 0;
            int lwx = this.wx - cX * 32;
            int lwy = this.wy - cY * 32;
            int index = lwx * 32 + lwy;
            this.in.seek((this.version >= 1 ? 8 : 0) + 4 + index * 8L);
            int pos = readInt(this.in);
            this.in.seek(pos);
            this.data.resetQuick();
            int minZ = Math.max(this.info.minLevel, -32);
            int maxZ = Math.min(this.info.maxLevel, 31);
            this.minLevel = 0;
            int usedZ = 0;

            for (int z = minZ; z <= maxZ; z++) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        int squareXYZ = x + y * 8 + (z - this.info.minLevel) * 8 * 8;
                        this.offsetInData[squareXYZ] = -1;
                        if (skip > 0) {
                            skip--;
                        } else {
                            int count = readInt(this.in);
                            if (count == -1) {
                                skip = readInt(this.in);
                                if (skip > 0) {
                                    skip--;
                                    continue;
                                }
                            }

                            if (count > 1) {
                                this.offsetInData[squareXYZ] = this.data.size();
                                this.data.add(count - 1);
                                this.minLevel = Math.min(z, this.minLevel);
                                usedZ = Math.max(z, usedZ);
                                int room = readInt(this.in);

                                for (int n = 1; n < count; n++) {
                                    int d = readInt(this.in);
                                    this.data.add(d);
                                }
                            }
                        }
                    }
                }
            }

            this.maxLevel = usedZ + 1;
        } catch (Exception var25) {
            Arrays.fill(this.offsetInData, -1);
            this.data.resetQuick();
            ExceptionLogger.logException(var25);
        }
    }

    public static LotHeader getHeader(int cellX, int cellY) {
        String filenameheader = ChunkMapFilenames.instance.getHeader(cellX, cellY);
        return InfoHeaders.get(filenameheader);
    }
}
