// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.sprite;

public enum RenderStateSlot {
    Populating(0),
    Ready(1),
    Rendering(2);

    private final int index;

    private RenderStateSlot(final int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }

    public int count() {
        return 3;
    }
}
