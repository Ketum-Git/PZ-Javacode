// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.data;

import java.util.ArrayDeque;
import zombie.iso.areas.isoregion.IsoRegions;

/**
 * TurboTuTone.
 */
public final class DataSquarePos {
    public static boolean debugPool = true;
    private static final ArrayDeque<DataSquarePos> pool = new ArrayDeque<>();
    public int x;
    public int y;
    public int z;

    static DataSquarePos alloc(int x, int y, int z) {
        DataSquarePos ds = !pool.isEmpty() ? pool.pop() : new DataSquarePos();
        ds.set(x, y, z);
        return ds;
    }

    static void release(DataSquarePos o) {
        assert !pool.contains(o);

        if (debugPool && pool.contains(o)) {
            IsoRegions.warn("DataSquarePos.release Trying to release a DataSquarePos twice.");
        } else {
            pool.push(o.reset());
        }
    }

    private DataSquarePos() {
    }

    private DataSquarePos reset() {
        return this;
    }

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
