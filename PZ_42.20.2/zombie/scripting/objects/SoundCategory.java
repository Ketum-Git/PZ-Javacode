// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum SoundCategory {
    ANIMALS("Animals"),
    AUTO("AUTO"),
    GENERAL("General"),
    ITEM("Item"),
    META("Meta"),
    MUSIC("Music"),
    OBJECT("Object"),
    PLAYER("Player"),
    UI("UI"),
    VEHICLE("Vehicle"),
    WORLD("World"),
    ZOMBIE("Zombie");

    private final String id;

    private SoundCategory(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
