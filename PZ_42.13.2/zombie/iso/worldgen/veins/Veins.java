// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.veins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.iso.worldgen.WorldGenParams;

public class Veins {
    private final Map<String, List<OreVein>> cache = new HashMap<>();
    private final Map<String, OreVeinConfig> config;

    public Veins(Map<String, OreVeinConfig> config) {
        this.config = config;
    }

    public List<OreVein> get(int cellX, int cellY) {
        if (this.cache.containsKey(cellX + "_" + cellY)) {
            return this.cache.get(cellX + "_" + cellY);
        } else {
            Random rnd = WorldGenParams.INSTANCE.getRandom(cellX, cellY);
            List<OreVein> ret = new ArrayList<>();

            for (OreVeinConfig subConfig : this.config.values()) {
                if (!(rnd.nextFloat() > subConfig.getProbability())) {
                    ret.add(new OreVein(cellX, cellY, subConfig, rnd));
                }
            }

            this.cache.put(cellX + "_" + cellY, ret);
            if (!ret.isEmpty()) {
                DebugLog.log(ret.toString());
            }

            return ret;
        }
    }
}
