// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.flags;

public interface OrdinalShortFlag extends ShortFlag {
    @Override
    default short flag() {
        return (short)(1 << this.ordinal());
    }

    int ordinal();
}
