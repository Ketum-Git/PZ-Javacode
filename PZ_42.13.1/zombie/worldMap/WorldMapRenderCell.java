// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import zombie.popman.ObjectPool;

public final class WorldMapRenderCell {
    int cellX;
    int cellY;
    final ArrayList<WorldMapRenderLayer> renderLayers = new ArrayList<>();
    static ObjectPool<WorldMapRenderCell> pool = new ObjectPool<>(WorldMapRenderCell::new);

    static WorldMapRenderCell alloc() {
        return pool.alloc();
    }
}
