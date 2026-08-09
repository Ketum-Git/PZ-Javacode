// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public class FallDamage {
    private float impactIsoSpeed;
    private boolean isDamagingFall;
    private FallSeverity impactFallSeverity = FallSeverity.None;

    public void registerVariableCallbacks(IsoGameCharacter owner) {
        owner.setVariable("bLandLight", this::isDamagingFall, arg0 -> "Character has landed lightly.");
        owner.setVariable(
            "bLandLightMask", () -> this.isLightFall() && this.isDamagingFall(), arg0 -> "Character has landed lightly, with some damage incurred."
        );
        owner.setVariable("bHardFall", this::isHardFall, arg0 -> "Character has landed hard, with damage incurred.");
        owner.setVariable(
            "bHardFall2", this::isMoreThanHardFall, arg0 -> "Character has had a severe or fatal fall, with severe or fatal damage likely incurred."
        );
        owner.setVariable(
            "fallImpactSeverity",
            FallSeverity.class,
            this::getFallImpactSeverity,
            arg0 -> "Character has impacted the ground, with the specified FallSeverity."
        );
    }

    public void registerDebugGameVariables(IsoGameCharacter owner) {
        owner.setVariable("dbg.fallImpactIsoSpeed", this::getImpactIsoSpeed, var0 -> "Character has impacted the ground at this IsoSpeed.");
    }

    public void reset() {
        this.impactIsoSpeed = 0.0F;
        this.isDamagingFall = false;
        this.impactFallSeverity = FallSeverity.None;
    }

    public void setLandingImpact(float impactIsoSpeed) {
        this.impactIsoSpeed = impactIsoSpeed;
        this.isDamagingFall = FallingConstants.isDamagingFall(impactIsoSpeed);
        this.impactFallSeverity = FallingConstants.getFallSeverity(impactIsoSpeed);
    }

    public boolean isFall() {
        return this.impactFallSeverity != FallSeverity.None;
    }

    public boolean isDamagingFall() {
        return this.isDamagingFall;
    }

    public boolean isLightFall() {
        return this.impactFallSeverity == FallSeverity.Light;
    }

    public boolean isMoreThanLightFall() {
        return this.impactFallSeverity.ordinal() > FallSeverity.Light.ordinal();
    }

    public boolean isHardFall() {
        return this.impactFallSeverity == FallSeverity.Hard;
    }

    public boolean isMoreThanHardFall() {
        return this.impactFallSeverity.ordinal() > FallSeverity.Hard.ordinal();
    }

    public boolean isSevereFall() {
        return this.impactFallSeverity == FallSeverity.Severe;
    }

    public boolean isLethalFall() {
        return this.impactFallSeverity == FallSeverity.Lethal;
    }

    public float getImpactIsoSpeed() {
        return this.impactIsoSpeed;
    }

    public FallSeverity getFallImpactSeverity() {
        return this.impactFallSeverity;
    }
}
