// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import zombie.popman.ObjectPool;
import zombie.worldMap.styles.WorldMapStyleLayer;

public final class WorldMapRenderLayer {
    WorldMapRenderCell renderCell;
    WorldMapStyleLayer styleLayer;
    final ArrayList<WorldMapFeature> features = new ArrayList<>();
    static ObjectPool<WorldMapRenderLayer> pool = new ObjectPool<>(WorldMapRenderLayer::new);
}
