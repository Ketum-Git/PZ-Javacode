// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import zombie.popman.ObjectPool;

public class IsoSuperLot {
    public static final ObjectPool<IsoSuperLot> pool = new ObjectPool<>(IsoSuperLot::new);
    public int levels = 8;
    private int wxOld;
    private int wyOld;
    IsoLot[][] lots = new IsoLot[2][2];
    ArrayList<String>[][][] squares = new ArrayList[20][20][8];

    public static synchronized void put(IsoSuperLot lot) {
        lot.squares = new ArrayList[20][20][8];
        pool.release(lot);
    }

    public static synchronized IsoSuperLot get(Integer cX, Integer cY, Integer wX, Integer wY, IsoChunk ch) {
        IsoSuperLot l = pool.alloc();
        l.load(cX, cY, wX, wY, ch);
        return l;
    }

    public ArrayList<String> getSquareFromNewLotSize(int x, int y, int z) {
        return this.getSquare(x - this.wxOld * 10, y - this.wyOld * 10, z);
    }

    private ArrayList<String> getSquare(int squareX, int squareY, int squareZ) {
        return this.squares[squareX][squareY][squareZ];
    }

    private void load(Integer cX, Integer cY, Integer wX, Integer wY, IsoChunk ch) {
        int squareX = wX * 8;
        int squareY = wY * 8;
        int oldMinWorldX = squareX / 10;
        int oldMinWorldY = squareY / 10;
        this.wxOld = oldMinWorldX;
        this.wyOld = oldMinWorldY;
        int oldMaxWorldX = (squareX + 8) / 10;
        int oldMaxWorldY = (squareY + 8) / 10;
        int difX = oldMaxWorldX - oldMinWorldX;
        int difY = oldMaxWorldY - oldMinWorldY;

        for (int x = 0; x <= difX; x++) {
            for (int y = 0; y <= difY; y++) {
                int oldCellX = (oldMinWorldX + x) / 30;
                int oldCellY = (oldMinWorldY + y) / 30;
                IsoLot lot = IsoLot.get(IsoLot.MapFiles.get(0), oldCellX, oldCellY, oldMinWorldX + x, oldMinWorldY + y, ch);
                this.PlaceLot(lot, x, y);
            }
        }
    }

    private void PlaceLot(IsoLot lot, int x, int y) {
        for (int xx = 0; xx < 10; xx++) {
            for (int yy = 0; yy < 10; yy++) {
                for (int zz = 0; zz < 8; zz++) {
                    int squareXYZ = xx + yy * 10 + zz * 10 * 10;
                    int offsetInData = lot.offsetInData[squareXYZ];
                    if (offsetInData != -1) {
                        int numInts = lot.data.getQuick(offsetInData);
                        if (numInts > 0) {
                            for (int n = 0; n < numInts; n++) {
                                String tile = lot.info.tilesUsed.get(lot.data.get(offsetInData + 1 + n));
                                if (this.squares[xx + x * 10][yy + y * 10][zz] == null) {
                                    this.squares[xx + x * 10][yy + y * 10][zz] = new ArrayList<>();
                                }

                                this.squares[xx + x * 10][yy + y * 10][zz].add(tile);
                            }
                        }
                    }
                }
            }
        }
    }
}
