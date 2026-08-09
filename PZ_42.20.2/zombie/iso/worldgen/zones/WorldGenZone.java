// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.iso.zones.Zone;

public class WorldGenZone extends Zone {
    private boolean rocks = true;

    public WorldGenZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
        super(name, type, x, y, z, w, h);
        if (properties != null && properties.rawget("Rocks") instanceof Boolean b) {
            this.rocks = b;
        }
    }

    public boolean getRocks() {
        return this.rocks;
    }
}
