// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public enum WeaponReloadType {
    NONE(""),
    BOLT_ACTION("boltaction"),
    BOLT_ACTION_NO_MAG("boltactionnomag"),
    DOUBLE_BARREL_SHOTGUN("doublebarrelshotgun"),
    DOUBLE_BARREL_SHOTGUN_SAWN("doublebarrelshotgunsawn"),
    HANDGUN("handgun"),
    LEVER_ACTION("leveraction"),
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

    public static WeaponReloadType fromValue(String value) {
        for (WeaponReloadType weaponReloadType : values()) {
            if (weaponReloadType.id.equals(value)) {
                return weaponReloadType;
            }
        }

        return NONE;
    }
}
