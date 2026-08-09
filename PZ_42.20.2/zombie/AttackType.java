// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

public enum AttackType {
    BITE("bite", AttackTypeModifier.Bite),
    CHARGE("charge"),
    DEFAULT("default"),
    GRAPPLE_GRAB("grapplegrab", AttackTypeModifier.GrappleGrab),
    MELEE_STAB("meleestab", AttackTypeModifier.MeleeStab),
    MELEE_STAB_TO_FLOOR("meleestabtofloor", AttackTypeModifier.MeleeStabToFloor),
    MELEE_SWING("meleeswing", AttackTypeModifier.MeleeSwing),
    MELEE_TO_FLOOR("meleetofloor", AttackTypeModifier.MeleeToFloor),
    MISS("miss"),
    NONE("", AttackTypeModifier.None),
    OVERHEAD("overhead"),
    SHOT("shot", AttackTypeModifier.Shot),
    SHOVE("shove", AttackTypeModifier.Shove),
    SPEAR_STAB("spearstab"),
    STOMP("stomp", AttackTypeModifier.Stomp),
    UPPERCUT("uppercut");

    private final String id;
    private final AttackTypeModifier attackTypeModifier;

    private AttackType(final String id) {
        this(id, AttackTypeModifier.None);
    }

    private AttackType(final String id, final AttackTypeModifier attackTypeModifier) {
        this.id = id;
        this.attackTypeModifier = attackTypeModifier;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public boolean hasModifier(AttackTypeModifier attackTypeModifier) {
        return this.attackTypeModifier.hasFlag(attackTypeModifier);
    }
}
