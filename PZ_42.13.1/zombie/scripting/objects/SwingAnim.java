// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum SwingAnim {
    BAT("Bat"),
    HANDGUN("Handgun"),
    HEAVY("Heavy"),
    RIFLE("Rifle"),
    SHOVE("Shove"),
    SPEAR("Spear"),
    STAB("Stab"),
    STONE("Stone"),
    THROW("Throw");

    private final String id;

    private SwingAnim(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
