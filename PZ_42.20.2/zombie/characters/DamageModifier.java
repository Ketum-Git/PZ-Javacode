// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public enum DamageModifier {
    NONE(0.0F),
    LOW(0.5F),
    STANDARD(1.0F),
    HIGH(2.0F),
    EXTREME(5.0F);

    public final float multiplier;

    private DamageModifier(final float multiplier) {
        this.multiplier = multiplier;
    }
}
