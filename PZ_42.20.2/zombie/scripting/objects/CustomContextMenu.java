// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum CustomContextMenu {
    CHEW("Chew"),
    DRINK("Drink"),
    EAT("Eat"),
    SMOKE("Smoke"),
    SNIFF("Sniff"),
    TAKE("Take");

    private final String id;

    private CustomContextMenu(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
