// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.veins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.entity.util.LongMap;
import zombie.iso.worldgen.WorldGenParams;

public class Veins {
    private final LongMap<List<OreVein>> cache = new LongMap<>();
    private final Map<String, OreVeinConfig> config;

    public Veins(Map<String, OreVeinConfig> config) {
        this.config = config;
    }

    public List<OreVein> get(int cellX, int cellY) {
        long key = (long)cellX << 32 | cellY & 4294967295L;
        if (this.cache.containsKey(key)) {
            return this.cache.get(key);
        } else {
            Random rnd = WorldGenParams.INSTANCE.getRandom(cellX, cellY);
            List<OreVein> ret = new ArrayList<>();

            for (OreVeinConfig subConfig : this.config.values()) {
                if (!(rnd.nextFloat() > subConfig.getProbability())) {
                    ret.add(new OreVein(cellX, cellY, subConfig, rnd));
                }
            }

            this.cache.put(key, ret);
            if (!ret.isEmpty()) {
                DebugLog.log(ret.toString());
            }

            return ret;
        }
    }
}
