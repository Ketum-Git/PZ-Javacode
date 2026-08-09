// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

public enum AttackTypeModifier {
    None(0),
    Melee(1),
    Projectile(2),
    Grapple(4),
    Unarmed(8),
    Teeth(16),
    Prone(256),
    Standing(512),
    Piercing(4096),
    Bite(Teeth, Standing),
    Shove(Unarmed, Standing),
    Stomp(Unarmed, Prone),
    GrappleGrab(Grapple, Standing),
    MeleeSwing(Melee, Standing),
    MeleeStab(Melee, Piercing, Standing),
    MeleeToFloor(Melee, Standing, Prone),
    MeleeStabToFloor(Melee, Piercing, Standing, Prone),
    Shot(Projectile, Prone, Standing);

    private final int flags;

    private AttackTypeModifier(final int flags) {
        this.flags = flags;
    }

    private AttackTypeModifier(final AttackTypeModifier... flags) {
        int result = 0;

        for (AttackTypeModifier modifier : flags) {
            result |= modifier.flags;
        }

        this.flags = result;
    }

    public boolean hasFlag(AttackTypeModifier modifier) {
        return (modifier.flags & this.flags) == modifier.flags;
    }
}
