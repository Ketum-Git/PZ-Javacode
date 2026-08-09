// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;

@UsedFromLua
public class IsoDirectionSet {
    public int set;

    public static IsoDirections rotate(IsoDirections dir, int amount) {
        return IsoDirections.fromIndex(dir.ordinal() + amount);
    }

    public IsoDirections getNext() {
        for (int i = 0; i < 8; i++) {
            int bit = 1 << i;
            if ((this.set & bit) != 0) {
                this.set ^= bit;
                return IsoDirections.fromIndex(i);
            }
        }

        return null;
    }
}
