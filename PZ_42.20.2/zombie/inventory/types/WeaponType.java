// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import zombie.AttackType;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.util.list.WeightedList;

@UsedFromLua
public enum WeaponType {
    UNARMED("", list().add(AttackType.NONE, 10), true, false),
    TWO_HANDED("2handed", list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    ONE_HANDED("1handed", list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    HEAVY("heavy", list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10), true, false),
    KNIFE("knife", list().add(AttackType.DEFAULT, 20).add(AttackType.OVERHEAD, 10).add(AttackType.UPPERCUT, 10), true, false),
    SPEAR("spear", list().add(AttackType.DEFAULT, 10), true, false),
    HANDGUN("handgun", list().add(AttackType.NONE, 10), false, true),
    FIREARM("firearm", list().add(AttackType.NONE, 10), false, true),
    THROWING("throwing", list().add(AttackType.NONE, 10), false, true),
    CHAINSAW("chainsaw", list().add(AttackType.DEFAULT, 10), true, false);

    private final String type;
    private final WeightedList<AttackType> possibleAttack;
    private final boolean canMiss;
    private final boolean isRanged;

    private static WeightedList<AttackType> list() {
        return new WeightedList<>();
    }

    private WeaponType(final String type, final WeightedList<AttackType> possibleAttack, final boolean canMiss, final boolean isRanged) {
        this.type = type;
        this.possibleAttack = possibleAttack;
        this.canMiss = canMiss;
        this.isRanged = isRanged;
    }

    public static WeaponType getWeaponType(HandWeapon weapon) {
        String swing = weapon.getSwingAnim();
        if (swing.equalsIgnoreCase("Stab")) {
            return KNIFE;
        } else if (swing.equalsIgnoreCase("Heavy")) {
            return HEAVY;
        } else if (swing.equalsIgnoreCase("Throw")) {
            return THROWING;
        } else if (weapon.isRanged()) {
            return weapon.isTwoHandWeapon() ? FIREARM : HANDGUN;
        } else if (weapon.isTwoHandWeapon()) {
            if (swing.equalsIgnoreCase("Spear")) {
                return SPEAR;
            } else {
                return "Chainsaw".equals(weapon.getType()) ? CHAINSAW : TWO_HANDED;
            }
        } else {
            return ONE_HANDED;
        }
    }

    public static WeaponType getWeaponType(IsoGameCharacter chr) {
        return getWeaponType(chr, chr.getPrimaryHandItem(), chr.getSecondaryHandItem());
    }

    public static WeaponType getWeaponType(IsoGameCharacter chr, InventoryItem inv1, InventoryItem inv2) {
        if (chr == null) {
            return null;
        } else {
            WeaponType result = null;
            chr.setVariable("rangedWeapon", false);
            if (inv1 != null && inv1 instanceof HandWeapon handWeapon) {
                if (inv1.getSwingAnim().equalsIgnoreCase("Stab")) {
                    return KNIFE;
                }

                if (inv1.getSwingAnim().equalsIgnoreCase("Heavy")) {
                    return HEAVY;
                }

                if (inv1.getSwingAnim().equalsIgnoreCase("Throw")) {
                    chr.setVariable("rangedWeapon", true);
                    return THROWING;
                }

                if (!handWeapon.isRanged()) {
                    result = ONE_HANDED;
                    if (inv1 == inv2 && inv1.isTwoHandWeapon()) {
                        result = TWO_HANDED;
                        if (inv1.getSwingAnim().equalsIgnoreCase("Spear")) {
                            return SPEAR;
                        }

                        if ("Chainsaw".equals(inv1.getType())) {
                            return CHAINSAW;
                        }
                    }
                } else {
                    result = HANDGUN;
                    if (inv1 == inv2 && inv1.isTwoHandWeapon()) {
                        result = FIREARM;
                    }
                }
            }

            if (result == null) {
                result = UNARMED;
            }

            chr.setVariable("rangedWeapon", result == HANDGUN || result == FIREARM);
            return result;
        }
    }

    public String getType() {
        return this.type;
    }

    public WeightedList<AttackType> getPossibleAttack() {
        return this.possibleAttack;
    }

    public boolean isCanMiss() {
        return this.canMiss;
    }

    public boolean isRanged() {
        return this.isRanged;
    }
}
