// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public class FallDamage {
    private float impactIsoSpeed;
    private boolean isDamagingFall;
    private FallSeverity impactFallSeverity = FallSeverity.None;

    public void registerVariableCallbacks(IsoGameCharacter in_owner) {
        in_owner.setVariable("bLandLight", this::isDamagingFall, owner -> "Character has landed lightly.");
        in_owner.setVariable(
            "bLandLightMask", () -> this.isLightFall() && this.isDamagingFall(), owner -> "Character has landed lightly, with some damage incurred."
        );
        in_owner.setVariable("bHardFall", this::isHardFall, owner -> "Character has landed hard, with damage incurred.");
        in_owner.setVariable(
            "bHardFall2", this::isMoreThanHardFall, owner -> "Character has had a severe or fatal fall, with severe or fatal damage likely incurred."
        );
        in_owner.setVariable(
            "fallImpactSeverity",
            FallSeverity.class,
            this::getFallImpactSeverity,
            owner -> "Character has impacted the ground, with the specified FallSeverity."
        );
    }

    public void registerDebugGameVariables(IsoGameCharacter in_owner) {
        in_owner.setVariable("dbg.fallImpactIsoSpeed", this::getImpactIsoSpeed, owner -> "Character has impacted the ground at this IsoSpeed.");
    }

    public void reset() {
        this.impactIsoSpeed = 0.0F;
        this.isDamagingFall = false;
        this.impactFallSeverity = FallSeverity.None;
    }

    public void setLandingImpact(float in_impactIsoSpeed) {
        this.impactIsoSpeed = in_impactIsoSpeed;
        this.isDamagingFall = FallingConstants.isDamagingFall(in_impactIsoSpeed);
        this.impactFallSeverity = FallingConstants.getFallSeverity(in_impactIsoSpeed);
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
