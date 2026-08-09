// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public class WeaponCategory {
    public static final WeaponCategory AXE = registerBase("Axe");
    public static final WeaponCategory BLUNT = registerBase("Blunt");
    public static final WeaponCategory IMPROVISED = registerBase("Improvised");
    public static final WeaponCategory LONG_BLADE = registerBase("LongBlade");
    public static final WeaponCategory SMALL_BLADE = registerBase("SmallBlade");
    public static final WeaponCategory SMALL_BLUNT = registerBase("SmallBlunt");
    public static final WeaponCategory SPEAR = registerBase("Spear");
    public static final WeaponCategory UNARMED = registerBase("Unarmed");
    private final String translationName;

    private WeaponCategory(String translationName) {
        this.translationName = translationName;
    }

    public static WeaponCategory get(ResourceLocation id) {
        return Registries.WEAPON_CATEGORY.get(id);
    }

    @Override
    public String toString() {
        return Registries.WEAPON_CATEGORY.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static WeaponCategory register(String id) {
        return register(false, id);
    }

    private static WeaponCategory registerBase(String id) {
        return register(true, id);
    }

    private static WeaponCategory register(boolean allowDefaultNamespace, String id) {
        return Registries.WEAPON_CATEGORY.register(RegistryReset.createLocation(id, allowDefaultNamespace), new WeaponCategory(id));
    }
}
