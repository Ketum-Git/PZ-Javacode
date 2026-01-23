// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum WeaponReloadType {
    BOLT_ACTION("boltaction"),
    BOLT_ACTION_NO_MAG("boltactionnomag"),
    DOUBLE_BARREL_SHOTGUN("doublebarrelshotgun"),
    DOUBLE_BARREL_SHOTGUN_SAWN("doublebarrelshotgunsawn"),
    HANDGUN("handgun"),
    REVOLVER("revolver"),
    SHOTGUN("shotgun");

    private final String id;

    private WeaponReloadType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
