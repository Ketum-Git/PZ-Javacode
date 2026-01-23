// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.zones;

import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.iso.IsoDirections;

@UsedFromLua
public final class VehicleZone extends Zone {
    public static final short VZF_FaceDirection = 1;
    public IsoDirections dir = IsoDirections.Max;
    public short flags;

    public VehicleZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
        super(name, type, x, y, z, w, h);
        if (properties != null) {
            if (properties.rawget("Direction") instanceof String s) {
                this.dir = IsoDirections.valueOf(s);
            }

            Object var11 = properties.rawget("FaceDirection");
            if (var11 == Boolean.TRUE) {
                this.flags = (short)(this.flags | 1);
            }
        }
    }

    public boolean isFaceDirection() {
        return (this.flags & 1) != 0;
    }
}
