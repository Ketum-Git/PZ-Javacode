// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum WeaponSubCategory {
    FIREARM("Firearm"),
    SPEAR("Spear"),
    STAB("Stab"),
    SWINGING("Swinging");

    private final String id;

    private WeaponSubCategory(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
