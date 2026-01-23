// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;

public final class ZomboidBitFlag {
    final EnumSet<IsoFlagType> isoFlagTypeSet = EnumSet.noneOf(IsoFlagType.class);

    public ZomboidBitFlag(int size) {
    }

    public ZomboidBitFlag(ZomboidBitFlag fl) {
        if (fl != null) {
            this.isoFlagTypeSet.addAll(fl.isoFlagTypeSet);
        }
    }

    public void set(int off, boolean b) {
        if (off < IsoFlagType.MAX.index()) {
            if (b) {
                this.isoFlagTypeSet.add(IsoFlagType.fromIndex(off));
            } else {
                this.isoFlagTypeSet.remove(IsoFlagType.fromIndex(off));
            }
        }
    }

    public void clear() {
        this.isoFlagTypeSet.clear();
    }

    public boolean isSet(int off) {
        return this.isoFlagTypeSet.contains(IsoFlagType.fromIndex(off));
    }

    public boolean isSet(IsoFlagType flag) {
        return this.isoFlagTypeSet.contains(flag);
    }

    public void set(IsoFlagType flag, boolean b) {
        if (b) {
            this.isoFlagTypeSet.add(flag);
        } else {
            this.isoFlagTypeSet.remove(flag);
        }
    }

    public boolean isSet(IsoObjectType flag) {
        return this.isSet(flag.index());
    }

    public void set(IsoObjectType flag, boolean b) {
        this.set(flag.index(), b);
    }

    public void Or(ZomboidBitFlag SpriteFlags) {
        this.isoFlagTypeSet.addAll(SpriteFlags.isoFlagTypeSet);
    }

    public void save(DataOutputStream output) throws IOException {
    }

    public void load(DataInputStream input) throws IOException {
    }

    public void getFromLong(long l) {
    }
}
