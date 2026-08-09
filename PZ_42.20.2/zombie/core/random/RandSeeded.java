// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import java.util.Random;
import zombie.iso.worldgen.WorldGenParams;

public class RandSeeded extends RandAbstract {
    public RandSeeded(long seed) {
        this.rand = new Random(seed);
    }

    @Override
    public void init() {
        this.rand = new Random(WorldGenParams.INSTANCE.getSeed());
    }
}
