// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public class ItemType {
    public static final ItemType ALARM_CLOCK = registerBase("AlarmClock");
    public static final ItemType ALARM_CLOCK_CLOTHING = registerBase("AlarmClockClothing");
    public static final ItemType ANIMAL = registerBase("Animal");
    public static final ItemType CLOTHING = registerBase("Clothing");
    public static final ItemType CONTAINER = registerBase("Container");
    public static final ItemType DRAINABLE = registerBase("Drainable");
    public static final ItemType FOOD = registerBase("Food");
    public static final ItemType KEY = registerBase("Key");
    public static final ItemType KEY_RING = registerBase("KeyRing");
    public static final ItemType LITERATURE = registerBase("Literature");
    public static final ItemType MAP = registerBase("Map");
    public static final ItemType MOVEABLE = registerBase("Moveable");
    public static final ItemType NORMAL = registerBase("Normal");
    public static final ItemType RADIO = registerBase("Radio");
    public static final ItemType WEAPON = registerBase("Weapon");
    public static final ItemType WEAPON_PART = registerBase("WeaponPart");

    private ItemType() {
    }

    public static ItemType get(ResourceLocation id) {
        return Registries.ITEM_TYPE.get(id);
    }

    @Override
    public String toString() {
        return Registries.ITEM_TYPE.getLocation(this).toString();
    }

    public static ItemType register(String id) {
        return register(false, id);
    }

    private static ItemType registerBase(String id) {
        return register(true, id);
    }

    private static ItemType register(boolean allowDefaultNamespace, String id) {
        return Registries.ITEM_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new ItemType());
    }
}
