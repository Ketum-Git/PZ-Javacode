// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import zombie.iso.IsoDirections;
import zombie.popman.ObjectPool;

final class SlopedSurface {
    byte x;
    byte y;
    IsoDirections direction;
    float heightMin;
    float heightMax;
    static final ObjectPool<SlopedSurface> pool = new ObjectPool<>(SlopedSurface::new);

    static SlopedSurface alloc() {
        return pool.alloc();
    }

    void release() {
        pool.release(this);
    }
}
