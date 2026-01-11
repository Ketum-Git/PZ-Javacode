// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import zombie.UsedFromLua;

/**
 * TurboTuTone.
 */
@UsedFromLua
public enum Metabolics {
    Sleeping(0.8F),
    SeatedResting(1.0F),
    StandingAtRest(1.1F),
    SedentaryActivity(1.2F),
    Default(1.5F),
    DrivingCar(1.4F),
    LightDomestic(1.6F),
    HeavyDomestic(2.0F),
    DefaultExercise(3.0F),
    UsingTools(2.5F),
    LightWork(3.2F),
    MediumWork(3.9F),
    DiggingSpade(5.5F),
    HeavyWork(6.0F),
    ForestryAxe(8.0F),
    Walking2kmh(1.9F),
    Walking5kmh(3.1F),
    Running10kmh(6.9F),
    Running15kmh(9.5F),
    JumpFence(4.0F),
    ClimbRope(8.0F),
    Fitness(6.0F),
    FitnessHeavy(9.0F),
    MAX(10.3F);

    private final float met;

    private Metabolics(final float met) {
        this.met = met;
    }

    public float getMet() {
        return this.met;
    }

    public float getWm2() {
        return MetToWm2(this.met);
    }

    public float getW() {
        return MetToW(this.met);
    }

    public float getBtuHr() {
        return MetToBtuHr(this.met);
    }

    public static float MetToWm2(float met) {
        return 58.0F * met;
    }

    public static float MetToW(float met) {
        return MetToWm2(met) * 1.8F;
    }

    public static float MetToBtuHr(float met) {
        return 356.0F * met;
    }
}
