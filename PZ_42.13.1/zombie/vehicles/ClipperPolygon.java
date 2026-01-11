// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import zombie.popman.ObjectPool;

public final class ClipperPolygon {
    public final TFloatArrayList outer = new TFloatArrayList();
    public final ArrayList<TFloatArrayList> holes = new ArrayList<>();

    public ClipperPolygon makeCopy(ObjectPool<ClipperPolygon> polygonPool, ObjectPool<TFloatArrayList> floatArrayListPool) {
        ClipperPolygon copy = polygonPool.alloc();
        copy.outer.clear();
        copy.outer.addAll(this.outer);
        copy.holes.clear();

        for (int i = 0; i < this.holes.size(); i++) {
            TFloatArrayList hole = this.holes.get(i);
            TFloatArrayList holeCopy = floatArrayListPool.alloc();
            holeCopy.clear();
            holeCopy.addAll(hole);
            copy.holes.add(holeCopy);
        }

        return copy;
    }
}
