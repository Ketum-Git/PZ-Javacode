// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.Optional;
import zombie.UsedFromLua;

@UsedFromLua
public class AmmoType {
    public static final AmmoType BULLETS_223 = registerBase("bullets_223", ItemKey.Normal.BULLETS_223);
    public static final AmmoType BULLETS_308 = registerBase("bullets_308", ItemKey.Normal.BULLETS_308);
    public static final AmmoType BULLETS_38 = registerBase("bullets_38", ItemKey.Normal.BULLETS_38);
    public static final AmmoType BULLETS_44 = registerBase("bullets_44", ItemKey.Normal.BULLETS_44);
    public static final AmmoType BULLETS_45 = registerBase("bullets_45", ItemKey.Normal.BULLETS_45);
    public static final AmmoType BULLETS_556 = registerBase("bullets_556", ItemKey.Normal.BULLETS_556);
    public static final AmmoType BULLETS_9MM = registerBase("bullets_9mm", ItemKey.Normal.BULLETS_9MM);
    public static final AmmoType CAP_GUN_CAP = registerBase("cap_gun_cap", ItemKey.Normal.CAP_GUN_CAP);
    public static final AmmoType SHOTGUN_SHELLS = registerBase("shotgun_shells", ItemKey.Normal.SHOTGUN_SHELLS);
    private final String itemKey;
    private final String translationName;

    private AmmoType(String itemKey, String translationName) {
        this.itemKey = itemKey;
        this.translationName = translationName;
    }

    public static AmmoType get(ResourceLocation id) {
        return Registries.AMMO_TYPE.get(id);
    }

    public static Optional<AmmoType> getByItemKey(String key) {
        for (AmmoType ammo : Registries.AMMO_TYPE.values()) {
            if (ammo.itemKey.equals(key)) {
                return Optional.of(ammo);
            }
        }

        return Optional.empty();
    }

    @Override
    public String toString() {
        return Registries.AMMO_TYPE.getLocation(this).toString();
    }

    public String getItemKey() {
        return this.itemKey;
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static AmmoType register(String id, String itemKey) {
        return register(false, id, new AmmoType(itemKey, id));
    }

    private static AmmoType registerBase(String id, ItemKey itemKey) {
        return register(true, id, new AmmoType(itemKey.toString(), id));
    }

    private static AmmoType register(boolean allowDefaultNamespace, String id, AmmoType t) {
        return Registries.AMMO_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }
}
