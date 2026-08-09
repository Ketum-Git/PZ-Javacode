// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.flags;

public final class ShortFlags {
    private short flags;

    private ShortFlags(short flags) {
        this.flags = flags;
    }

    public static ShortFlags alloc() {
        return new ShortFlags((short)0);
    }

    public static ShortFlags toFlags(short flags) {
        return new ShortFlags(flags);
    }

    public void set(ShortFlag flag, boolean set) {
        if (set) {
            this.set(flag);
        } else {
            this.clear(flag);
        }
    }

    public void set(ShortFlag flag) {
        this.flags = (short)(this.flags | flag.flag());
    }

    public void clear(ShortFlag flag) {
        this.flags = (short)(this.flags & (short)(~flag.flag()));
    }

    public boolean has(ShortFlag flag) {
        return (this.flags & flag.flag()) != 0;
    }

    public short asShort() {
        return this.flags;
    }
}
