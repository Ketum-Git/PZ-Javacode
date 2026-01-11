// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import java.nio.ByteBuffer;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.network.BodyDamageSync;
import zombie.network.GameClient;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public final class BodyPart {
    public BodyPartType type;
    private final float biteDamage = 2.1875F;
    private final float bleedDamage = 0.2857143F;
    private float damageScaler = 0.0057142857F;
    private float health;
    private boolean bandaged;
    private boolean bitten;
    private boolean bleeding;
    private boolean isBleedingStemmed;
    private boolean isCauterized;
    private boolean scratched;
    private boolean stitched;
    private boolean deepWounded;
    private boolean isInfected;
    private boolean isFakeInfected;
    private final IsoGameCharacter parentChar;
    private float bandageLife;
    private float scratchTime;
    private float biteTime;
    private boolean alcoholicBandage;
    private float stiffness;
    private float woundInfectionLevel;
    private boolean infectedWound;
    private final float scratchDamage = 0.9375F;
    private final float cutDamage = 1.875F;
    private final float woundDamage = 3.125F;
    private final float burnDamage = 3.75F;
    private final float bulletDamage = 3.125F;
    private final float fractureDamage = 3.125F;
    private float bleedingTime;
    private float deepWoundTime;
    private boolean haveGlass;
    private float stitchTime;
    private float alcoholLevel;
    private float additionalPain;
    private String bandageType;
    private boolean getBandageXp = true;
    private boolean getStitchXp = true;
    private boolean getSplintXp = true;
    private float fractureTime;
    private boolean splint;
    private float splintFactor;
    private boolean haveBullet;
    private float burnTime;
    private boolean needBurnWash;
    private float lastTimeBurnWash;
    private String splintItem;
    private float plantainFactor;
    private float comfreyFactor;
    private float garlicFactor;
    private float cutTime;
    private boolean cut;
    private float scratchSpeedModifier;
    private float cutSpeedModifier;
    private float burnSpeedModifier;
    private float deepWoundSpeedModifier;
    private float wetness;
    protected Thermoregulator.ThermalNode thermalNode;

    public BodyPart(BodyPartType partType, IsoGameCharacter parent) {
        this.type = partType;
        this.parentChar = parent;
        if (partType == BodyPartType.Neck) {
            this.damageScaler *= 5.0F;
        }

        if (partType == BodyPartType.Hand_L || partType == BodyPartType.Hand_R || partType == BodyPartType.ForeArm_L || partType == BodyPartType.ForeArm_R) {
            this.scratchSpeedModifier = 85.0F;
            this.cutSpeedModifier = 95.0F;
            this.burnSpeedModifier = 45.0F;
            this.deepWoundSpeedModifier = 60.0F;
        }

        if (partType == BodyPartType.UpperArm_L || partType == BodyPartType.UpperArm_R) {
            this.scratchSpeedModifier = 65.0F;
            this.cutSpeedModifier = 75.0F;
            this.burnSpeedModifier = 35.0F;
            this.deepWoundSpeedModifier = 40.0F;
        }

        if (partType == BodyPartType.UpperLeg_L
            || partType == BodyPartType.UpperLeg_R
            || partType == BodyPartType.LowerLeg_L
            || partType == BodyPartType.LowerLeg_R) {
            this.scratchSpeedModifier = 45.0F;
            this.cutSpeedModifier = 55.0F;
            this.burnSpeedModifier = 15.0F;
            this.deepWoundSpeedModifier = 20.0F;
        }

        if (partType == BodyPartType.Foot_L || partType == BodyPartType.Foot_R) {
            this.scratchSpeedModifier = 35.0F;
            this.cutSpeedModifier = 45.0F;
            this.burnSpeedModifier = 10.0F;
            this.deepWoundSpeedModifier = 15.0F;
        }

        if (partType == BodyPartType.Groin) {
            this.scratchSpeedModifier = 45.0F;
            this.cutSpeedModifier = 55.0F;
            this.burnSpeedModifier = 15.0F;
            this.deepWoundSpeedModifier = 20.0F;
        }

        this.RestoreToFullHealth();
    }

    public IsoGameCharacter getParentChar() {
        return this.parentChar;
    }

    public void AddDamage(float Val) {
        this.ReduceHealth(Val);
    }

    public boolean isBandageDirty() {
        return this.getBandageLife() <= 0.0F;
    }

    public void DamageUpdate() {
        if (!GameClient.client || !(this.parentChar instanceof IsoPlayer player && player.isLocalPlayer())) {
            if (this.getDeepWoundTime() > 0.0F && !this.stitched()) {
                float damage = 3.125F * this.damageScaler * GameTime.getInstance().getMultiplier();
                if (this.bandaged()) {
                    damage = 1.5625F * this.damageScaler * GameTime.getInstance().getMultiplier();
                }

                CombatManager.getInstance().applyDamage(this, damage);
            }

            if (this.getScratchTime() > 0.0F && !this.bandaged()) {
                CombatManager.getInstance().applyDamage(this, 0.9375F * this.damageScaler * GameTime.getInstance().getMultiplier());
            }

            if (this.getCutTime() > 0.0F && !this.bandaged()) {
                CombatManager.getInstance().applyDamage(this, 1.875F * this.damageScaler * GameTime.getInstance().getMultiplier());
            }

            if (this.getBiteTime() > 0.0F && !this.bandaged()) {
                CombatManager.getInstance().applyDamage(this, 2.1875F * this.damageScaler * GameTime.getInstance().getMultiplier());
            }

            if (this.getBleedingTime() > 0.0F && !this.bandaged()) {
                float finalBleedDamage = 0.2857143F * this.damageScaler * GameTime.getInstance().getMultiplier() * (this.getBleedingTime() / 10.0F);
                this.parentChar.getBodyDamage().ReduceGeneralHealth(finalBleedDamage);
                LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "BLEEDING", finalBleedDamage);
                if (Rand.NextBool(Rand.AdjustForFramerate(1000))) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(BodyPartType.ToIndex(this.getType())), false, false, true);
                }
            }

            if (this.haveBullet()) {
                float damage = 3.125F * this.damageScaler * GameTime.getInstance().getMultiplier();
                if (this.bandaged()) {
                    damage = 1.5625F * this.damageScaler * GameTime.getInstance().getMultiplier();
                }

                CombatManager.getInstance().applyDamage(this, damage);
            }

            if (this.getBurnTime() > 0.0F && !this.bandaged()) {
                CombatManager.getInstance().applyDamage(this, 3.75F * this.damageScaler * GameTime.getInstance().getMultiplier());
            }

            if (this.getFractureTime() > 0.0F && !this.isSplint()) {
                CombatManager.getInstance().applyDamage(this, 3.125F * this.damageScaler * GameTime.getInstance().getMultiplier());
            }

            if (this.getBiteTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setBiteTime(this.getBiteTime() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                } else {
                    this.setBiteTime(this.getBiteTime() - (float)(5.0E-6 * GameTime.getInstance().getMultiplier()));
                }
            }

            if (this.getBurnTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setBurnTime(this.getBurnTime() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                } else {
                    this.setBurnTime(this.getBurnTime() - (float)(5.0E-6 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getLastTimeBurnWash() - this.getBurnTime() >= 20.0F) {
                    this.setLastTimeBurnWash(0.0F);
                    this.setNeedBurnWash(true);
                }
            }

            if (this.getBleedingTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setBleedingTime(this.getBleedingTime() - (float)(2.0E-4 * GameTime.getInstance().getMultiplier()));
                    if (this.getDeepWoundTime() > 0.0F) {
                        this.setBandageLife(this.getBandageLife() - (float)(0.005 * GameTime.getInstance().getMultiplier()));
                    } else {
                        this.setBandageLife(this.getBandageLife() - (float)(3.0E-4 * GameTime.getInstance().getMultiplier()));
                    }
                } else {
                    this.setBleedingTime(this.getBleedingTime() - (float)(2.0E-5 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getBleedingTime() < 3.0F && this.haveGlass()) {
                    this.setBleedingTime(3.0F);
                }

                if (this.getBleedingTime() < 0.0F) {
                    this.setBleedingTime(0.0F);
                    this.setBleeding(false);
                }
            }

            if (!this.isInfectedWound()
                && !this.isInfected
                && (!this.alcoholicBandage || !(this.getBandageLife() > 0.0F))
                && (this.getDeepWoundTime() > 0.0F || this.getScratchTime() > 0.0F || this.getCutTime() > 0.0F || this.getStitchTime() > 0.0F)) {
                int baseChance = 40000;
                if (!this.bandaged()) {
                    baseChance -= 10000;
                } else if (this.getBandageLife() == 0.0F) {
                    baseChance -= 35000;
                }

                if (this.getScratchTime() > 0.0F) {
                    baseChance -= 20000;
                }

                if (this.getCutTime() > 0.0F) {
                    baseChance -= 25000;
                }

                if (this.getDeepWoundTime() > 0.0F) {
                    baseChance -= 30000;
                }

                if (this.haveGlass()) {
                    baseChance -= 24000;
                }

                if (this.getBurnTime() > 0.0F) {
                    baseChance -= 23000;
                    if (this.isNeedBurnWash()) {
                        baseChance -= 7000;
                    }
                }

                if (this.hasDirtyClothing()) {
                    baseChance -= 20000;
                }

                if (this.hasBloodyClothing()) {
                    baseChance -= 24000;
                }

                if (baseChance <= 5000) {
                    baseChance = 5000;
                }

                if (Rand.Next(Rand.AdjustForFramerate(baseChance)) == 0) {
                    this.setInfectedWound(true);
                }
            } else if (this.isInfectedWound()) {
                boolean reduce = false;
                if (this.getAlcoholLevel() > 0.0F) {
                    this.setAlcoholLevel(this.getAlcoholLevel() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                    this.setWoundInfectionLevel(this.getWoundInfectionLevel() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                    if (this.getAlcoholLevel() < 0.0F) {
                        this.setAlcoholLevel(0.0F);
                    }

                    reduce = true;
                }

                if (this.parentChar.getReduceInfectionPower() > 0.0F) {
                    this.setWoundInfectionLevel(this.getWoundInfectionLevel() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                    this.parentChar.setReduceInfectionPower(this.parentChar.getReduceInfectionPower() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                    if (this.parentChar.getReduceInfectionPower() < 0.0F) {
                        this.parentChar.setReduceInfectionPower(0.0F);
                    }

                    reduce = true;
                }

                if (this.getGarlicFactor() > 0.0F) {
                    this.setWoundInfectionLevel(this.getWoundInfectionLevel() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                    this.setGarlicFactor(this.getGarlicFactor() - 8.0E-4F * GameTime.getInstance().getMultiplier());
                    reduce = true;
                }

                if (!reduce) {
                    if (this.isInfected) {
                        this.setWoundInfectionLevel(this.getWoundInfectionLevel() + 2.0E-4F * GameTime.getInstance().getMultiplier());
                    } else if (this.haveGlass()) {
                        this.setWoundInfectionLevel(this.getWoundInfectionLevel() + 1.0E-4F * GameTime.getInstance().getMultiplier());
                    } else {
                        this.setWoundInfectionLevel(this.getWoundInfectionLevel() + 1.0E-5F * GameTime.getInstance().getMultiplier());
                    }
                }
            }

            if (this.getWoundInfectionLevel() > 10.0F) {
                this.setWoundInfectionLevel(10.0F);
            }

            if (!this.isInfectedWound() && this.getAlcoholLevel() > 0.0F) {
                this.setAlcoholLevel(this.getAlcoholLevel() - 2.0E-4F * GameTime.getInstance().getMultiplier());
                if (this.getAlcoholLevel() < 0.0F) {
                    this.setAlcoholLevel(0.0F);
                }
            }

            if (this.isInfectedWound() && this.getBandageLife() > 0.0F) {
                if (this.alcoholicBandage) {
                    this.setWoundInfectionLevel(this.getWoundInfectionLevel() - 6.0E-4F * GameTime.getInstance().getMultiplier());
                }

                this.setBandageLife(this.getBandageLife() - (float)(2.0E-4 * GameTime.getInstance().getMultiplier()));
            }

            if (this.getScratchTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setScratchTime(this.getScratchTime() - (float)(1.5E-4 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(8.0E-5 * GameTime.getInstance().getMultiplier()));
                    if (this.getPlantainFactor() > 0.0F) {
                        this.setScratchTime(this.getScratchTime() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                        this.setPlantainFactor(this.getPlantainFactor() - (float)(8.0E-4 * GameTime.getInstance().getMultiplier()));
                    }
                } else {
                    this.setScratchTime(this.getScratchTime() - (float)(1.0E-5 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getScratchTime() < 0.0F) {
                    this.setScratchTime(0.0F);
                    this.setGetBandageXp(true);
                    this.setGetStitchXp(true);
                    this.setScratched(false, false);
                    this.setBleeding(false);
                    this.setBleedingTime(0.0F);
                    this.resetPoulticeFactors();
                }
            }

            if (this.getCutTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setCutTime(this.getCutTime() - (float)(5.0E-5 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(1.0E-5 * GameTime.getInstance().getMultiplier()));
                    if (this.getPlantainFactor() > 0.0F) {
                        this.setCutTime(this.getCutTime() - (float)(5.0E-5 * GameTime.getInstance().getMultiplier()));
                        this.setPlantainFactor(this.getPlantainFactor() - (float)(8.0E-4 * GameTime.getInstance().getMultiplier()));
                    }
                } else {
                    this.setCutTime(this.getCutTime() - (float)(1.0E-6 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getCutTime() < 0.0F) {
                    this.setCutTime(0.0F);
                    this.setGetBandageXp(true);
                    this.setGetStitchXp(true);
                    this.setBleeding(false);
                    this.setBleedingTime(0.0F);
                    this.resetPoulticeFactors();
                }
            }

            if (this.getDeepWoundTime() > 0.0F) {
                if (this.bandaged()) {
                    this.setDeepWoundTime(this.getDeepWoundTime() - (float)(2.0E-5 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                    if (this.getPlantainFactor() > 0.0F) {
                        this.setDeepWoundTime(this.getDeepWoundTime() - (float)(7.0E-6 * GameTime.getInstance().getMultiplier()));
                        this.setPlantainFactor(this.getPlantainFactor() - (float)(8.0E-4 * GameTime.getInstance().getMultiplier()));
                        if (this.getPlantainFactor() < 0.0F) {
                            this.setPlantainFactor(0.0F);
                        }
                    }
                } else {
                    this.setDeepWoundTime(this.getDeepWoundTime() - (float)(2.0E-6 * GameTime.getInstance().getMultiplier()));
                }

                if ((this.haveGlass() || !this.bandaged()) && this.getDeepWoundTime() < 3.0F) {
                    this.setDeepWoundTime(3.0F);
                }

                if (this.getDeepWoundTime() < 0.0F) {
                    this.setGetBandageXp(true);
                    this.setGetStitchXp(true);
                    this.setDeepWoundTime(0.0F);
                    this.setDeepWounded(false);
                }
            }

            if (this.getStitchTime() > 0.0F && this.getStitchTime() < 50.0F) {
                if (this.bandaged()) {
                    this.setStitchTime(this.getStitchTime() + (float)(4.0E-4 * GameTime.getInstance().getMultiplier()));
                    this.setBandageLife(this.getBandageLife() - (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                    if (!this.alcoholicBandage && Rand.Next(Rand.AdjustForFramerate(80000)) == 0) {
                        this.setInfectedWound(true);
                    }

                    this.setStitchTime(this.getStitchTime() + (float)(1.0E-4 * GameTime.getInstance().getMultiplier()));
                } else {
                    this.setStitchTime(this.getStitchTime() + (float)(2.0E-4 * GameTime.getInstance().getMultiplier()));
                    if (Rand.Next(Rand.AdjustForFramerate(20000)) == 0) {
                        this.setInfectedWound(true);
                    }
                }

                if (this.getStitchTime() > 30.0F) {
                    this.setGetStitchXp(true);
                }

                if (this.getStitchTime() > 50.0F) {
                    this.setStitchTime(50.0F);
                }
            }

            if (this.getFractureTime() > 0.0F) {
                if (this.getSplintFactor() > 0.0F) {
                    this.setFractureTime(this.getFractureTime() - (float)(5.0E-5 * GameTime.getInstance().getMultiplier() * this.getSplintFactor()));
                } else {
                    this.setFractureTime(this.getFractureTime() - (float)(5.0E-6 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getComfreyFactor() > 0.0F) {
                    this.setFractureTime(this.getFractureTime() - (float)(5.0E-6 * GameTime.getInstance().getMultiplier()));
                    this.setComfreyFactor(this.getComfreyFactor() - (float)(5.0E-4 * GameTime.getInstance().getMultiplier()));
                }

                if (this.getFractureTime() < 0.0F) {
                    this.setFractureTime(0.0F);
                    this.setGetSplintXp(true);
                    this.resetPoulticeFactors();
                }
            }

            if (this.getAdditionalPain() > 0.0F) {
                this.setAdditionalPain(this.getAdditionalPain() - (float)(0.005 * GameTime.getInstance().getMultiplier()));
                if (this.getAdditionalPain() < 0.0F) {
                    this.setAdditionalPain(0.0F);
                }
            }

            if (this.getStiffness() > 0.0F
                && this.parentChar instanceof IsoPlayer isoPlayer
                && isoPlayer.getFitness() != null
                && !isoPlayer.getFitness().onGoingStiffness()) {
                this.setStiffness(this.getStiffness() - (float)(0.002 * GameTime.getInstance().getMultiplier()));
                if (this.getStiffness() < 0.0F) {
                    this.setStiffness(0.0F);
                }
            }

            if (this.getBandageLife() < 0.0F) {
                this.setBandageLife(0.0F);
                this.setGetBandageXp(true);
            }

            if ((this.getWoundInfectionLevel() > 0.0F || this.isInfectedWound())
                && this.getBurnTime() <= 0.0F
                && this.getFractureTime() <= 0.0F
                && this.getDeepWoundTime() <= 0.0F
                && this.getScratchTime() <= 0.0F
                && this.getBiteTime() <= 0.0F
                && this.getCutTime() <= 0.0F
                && this.getStitchTime() <= 0.0F) {
                this.setWoundInfectionLevel(0.0F);
            }

            if (this.health < 0.0F) {
                this.health = 0.0F;
            }
        }
    }

    private void resetPoulticeFactors() {
        this.setComfreyFactor(0.0F);
        this.setGarlicFactor(0.0F);
        this.setPlantainFactor(0.0F);
    }

    public float getHealth() {
        return this.health;
    }

    public void SetHealth(float NewHealth) {
        this.health = NewHealth;
    }

    public void AddHealth(float Val) {
        this.health = PZMath.clamp(this.health + Val, 0.0F, 100.0F);
    }

    public void ReduceHealth(float Val) {
        this.health = PZMath.clamp(this.health - Val, 0.0F, 100.0F);
    }

    public boolean HasInjury() {
        return this.bitten
            | this.scratched
            | this.deepWounded
            | this.bleeding
            | this.getBiteTime() > 0.0F
            | this.getScratchTime() > 0.0F
            | this.getCutTime() > 0.0F
            | this.getFractureTime() > 0.0F
            | this.haveBullet()
            | this.getBurnTime() > 0.0F;
    }

    public boolean bandaged() {
        return this.bandaged;
    }

    public boolean bitten() {
        return this.bitten;
    }

    public boolean bleeding() {
        return this.bleeding;
    }

    public boolean IsBleedingStemmed() {
        return this.isBleedingStemmed;
    }

    public boolean IsCauterized() {
        return this.isCauterized;
    }

    public boolean IsInfected() {
        return this.isInfected;
    }

    public void SetInfected(boolean inf) {
        this.isInfected = inf;
    }

    public void SetFakeInfected(boolean inf) {
        this.isFakeInfected = inf;
    }

    public boolean IsFakeInfected() {
        return this.isFakeInfected;
    }

    public void DisableFakeInfection() {
        this.isFakeInfected = false;
    }

    public boolean scratched() {
        return this.scratched;
    }

    public boolean stitched() {
        return this.stitched;
    }

    public boolean deepWounded() {
        return this.deepWounded;
    }

    public void RestoreToFullHealth() {
        this.health = 100.0F;
        this.additionalPain = 0.0F;
        this.alcoholicBandage = false;
        this.alcoholLevel = 0.0F;
        this.bleeding = false;
        this.bandaged = false;
        this.bandageLife = 0.0F;
        this.biteTime = 0.0F;
        this.bitten = false;
        this.bleedingTime = 0.0F;
        this.burnTime = 0.0F;
        this.comfreyFactor = 0.0F;
        this.deepWounded = false;
        this.deepWoundTime = 0.0F;
        this.fractureTime = 0.0F;
        this.garlicFactor = 0.0F;
        this.haveBullet = false;
        this.haveGlass = false;
        this.infectedWound = false;
        this.isBleedingStemmed = false;
        this.isCauterized = false;
        this.isFakeInfected = false;
        this.isInfected = false;
        this.lastTimeBurnWash = 0.0F;
        this.needBurnWash = false;
        this.plantainFactor = 0.0F;
        this.scratched = false;
        this.scratchTime = 0.0F;
        this.splint = false;
        this.splintFactor = 0.0F;
        this.splintItem = null;
        this.stitched = false;
        this.stitchTime = 0.0F;
        this.woundInfectionLevel = 0.0F;
        this.cutTime = 0.0F;
        this.cut = false;
        this.stiffness = 0.0F;
    }

    public void setBandaged(boolean Bandaged, float bandageLife) {
        this.setBandaged(Bandaged, bandageLife, false, null);
    }

    public void setBandaged(boolean Bandaged, float bandageLife, boolean isAlcoholic, String bandageType) {
        if (Bandaged) {
            if (this.bleeding) {
                this.bleeding = false;
            }

            this.bitten = false;
            this.scratched = false;
            this.cut = false;
            this.alcoholicBandage = isAlcoholic;
            this.stitched = false;
            this.deepWounded = false;
            this.setBandageType(bandageType);
            this.setGetBandageXp(false);
        } else {
            if (this.getScratchTime() > 0.0F) {
                this.scratched = true;
            }

            if (this.getCutTime() > 0.0F) {
                this.cut = true;
            }

            if (this.getBleedingTime() > 0.0F) {
                this.bleeding = true;
            }

            if (this.getBiteTime() > 0.0F) {
                this.bitten = true;
            }

            if (this.getStitchTime() > 0.0F) {
                this.stitched = true;
            }

            if (this.getDeepWoundTime() > 0.0F) {
                this.deepWounded = true;
            }

            this.resetPoulticeFactors();
        }

        this.setBandageLife(bandageLife);
        this.bandaged = Bandaged;
    }

    public void SetBitten(boolean Bitten) {
        this.bitten = Bitten;
        if (Bitten) {
            this.bleeding = true;
            this.isBleedingStemmed = false;
            this.isCauterized = false;
            this.bandaged = false;
            this.setInfectedWound(true);
            this.setBiteTime(Rand.Next(50.0F, 80.0F));
            if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                this.setBiteTime(Rand.Next(30.0F, 50.0F));
            } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                this.setBiteTime(Rand.Next(80.0F, 150.0F));
            }
        }

        if (SandboxOptions.instance.lore.transmission.getValue() != 4) {
            this.isInfected = true;
            this.isFakeInfected = false;
        }

        if (this.isInfected && SandboxOptions.instance.lore.mortality.getValue() == 7) {
            this.isInfected = false;
            this.isFakeInfected = true;
        }

        this.generateBleeding();
    }

    public void SetBitten(boolean Bitten, boolean Infected) {
        this.bitten = Bitten;
        if (SandboxOptions.instance.lore.transmission.getValue() == 4) {
            this.isInfected = false;
            this.isFakeInfected = false;
            Infected = false;
        }

        if (Bitten) {
            this.bleeding = true;
            this.isBleedingStemmed = false;
            this.isCauterized = false;
            this.bandaged = false;
            if (Infected) {
                this.isInfected = true;
            }

            this.isFakeInfected = false;
            if (this.isInfected && SandboxOptions.instance.lore.mortality.getValue() == 7) {
                this.isInfected = false;
                this.isFakeInfected = true;
            }
        }
    }

    public void setBleeding(boolean Bleeding) {
        this.bleeding = Bleeding;
    }

    public void SetBleedingStemmed(boolean BleedingStemmed) {
        if (this.bleeding) {
            this.bleeding = false;
            this.isBleedingStemmed = true;
        }
    }

    public void SetCauterized(boolean Cauterized) {
        this.isCauterized = Cauterized;
        if (Cauterized) {
            this.bleeding = false;
            this.isBleedingStemmed = false;
            this.deepWounded = false;
            this.bandaged = false;
        }
    }

    public void setCut(boolean cut) {
        this.setCut(cut, true);
    }

    public void setCut(boolean cut, boolean forceNoInfection) {
        this.cut = cut;
        if (!cut) {
            this.setBleeding(false);
        } else {
            this.setStitched(false);
            this.setBandaged(false, 0.0F);
            float cutTime = Rand.Next(10.0F, 20.0F);
            if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                cutTime = Rand.Next(5.0F, 10.0F);
            } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                cutTime = Rand.Next(20.0F, 30.0F);
            }

            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1:
                    cutTime *= 0.5F;
                    break;
                case 3:
                    cutTime *= 1.5F;
            }

            this.setCutTime(cutTime);
            this.generateBleeding();
            if (!forceNoInfection) {
                this.generateZombieInfection(25);
            }
        }
    }

    public void generateZombieInfection(int baseChance) {
        if (Rand.Next(100) < baseChance) {
            this.isInfected = true;
        }

        if (SandboxOptions.instance.lore.transmission.getValue() == 2 || SandboxOptions.instance.lore.transmission.getValue() == 4) {
            this.isInfected = false;
            this.isFakeInfected = false;
        }

        if (this.isInfected && SandboxOptions.instance.lore.mortality.getValue() == 7) {
            this.isInfected = false;
            this.isFakeInfected = true;
        }
    }

    public void setScratched(boolean Scratched, boolean forceNoInfection) {
        this.scratched = Scratched;
        if (!Scratched) {
            this.setBleeding(false);
        } else {
            this.setStitched(false);
            this.setBandaged(false, 0.0F);
            float newScratchTime = Rand.Next(7.0F, 15.0F);
            if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                newScratchTime = Rand.Next(4.0F, 10.0F);
            } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                newScratchTime = Rand.Next(15.0F, 25.0F);
            }

            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1:
                    newScratchTime *= 0.5F;
                    break;
                case 3:
                    newScratchTime *= 1.5F;
            }

            this.setScratchTime(newScratchTime);
            if (!forceNoInfection) {
                this.generateZombieInfection(7);
            }
        }
    }

    public void SetScratchedWeapon(boolean Scratched) {
        this.scratched = Scratched;
        if (!Scratched) {
            this.setBleeding(false);
        } else {
            this.setStitched(false);
            this.setBandaged(false, 0.0F);
            float newScratchTime = Rand.Next(5.0F, 10.0F);
            if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                newScratchTime = Rand.Next(1.0F, 5.0F);
            } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                newScratchTime = Rand.Next(10.0F, 20.0F);
            }

            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1:
                    newScratchTime *= 0.5F;
                    break;
                case 3:
                    newScratchTime *= 1.5F;
            }

            this.setScratchTime(newScratchTime);
            this.generateBleeding();
        }
    }

    public void generateDeepWound() {
        float deepWoundTime = Rand.Next(15.0F, 20.0F);
        if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
            deepWoundTime = Rand.Next(11.0F, 15.0F);
        } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
            deepWoundTime = Rand.Next(20.0F, 32.0F);
        }

        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                deepWoundTime *= 0.5F;
                break;
            case 3:
                deepWoundTime *= 1.5F;
        }

        this.setDeepWoundTime(deepWoundTime);
        this.setDeepWounded(true);
        this.generateBleeding();
    }

    public void generateDeepShardWound() {
        float deepWoundTime = Rand.Next(15.0F, 20.0F);
        if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
            deepWoundTime = Rand.Next(11.0F, 15.0F);
        } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
            deepWoundTime = Rand.Next(20.0F, 32.0F);
        }

        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                deepWoundTime *= 0.5F;
                break;
            case 3:
                deepWoundTime *= 1.5F;
        }

        this.setDeepWoundTime(deepWoundTime);
        this.setHaveGlass(true);
        this.setDeepWounded(true);
        this.generateBleeding();
    }

    public void generateFracture(float fractureTime) {
        if (SandboxOptions.instance.boneFracture.getValue()) {
            if (this.getFractureTime() <= 0.0F && fractureTime > 0.0F) {
                this.parentChar.playSound("FirstAidFracture");
            }

            this.setFractureTime(fractureTime);
        }
    }

    public void generateFractureNew(float fractureTime) {
        if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
            fractureTime *= 0.6F;
        } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
            fractureTime *= 1.8F;
        }

        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                fractureTime *= 0.5F;
                break;
            case 3:
                fractureTime *= 1.5F;
        }

        this.generateFracture(fractureTime);
    }

    public void SetScratchedWindow(boolean Scratched) {
        if (Scratched) {
            this.setBandaged(false, 0.0F);
            this.setStitched(false);
            if (Rand.Next(7) == 0) {
                this.generateDeepShardWound();
            } else {
                this.scratched = Scratched;
                float newScratchTime = Rand.Next(12.0F, 20.0F);
                if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                    newScratchTime = Rand.Next(5.0F, 10.0F);
                } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                    newScratchTime = Rand.Next(20.0F, 30.0F);
                }

                switch (SandboxOptions.instance.injurySeverity.getValue()) {
                    case 1:
                        newScratchTime *= 0.5F;
                        break;
                    case 3:
                        newScratchTime *= 1.5F;
                }

                this.setScratchTime(newScratchTime);
            }

            this.generateBleeding();
        }
    }

    public void setStitched(boolean Stitched) {
        if (Stitched) {
            this.setBleedingTime(0.0F);
            this.setBleeding(false);
            this.setDeepWoundTime(0.0F);
            this.setDeepWounded(false);
            this.setGetStitchXp(false);
        } else if (this.stitched) {
            this.stitched = false;
            if (this.getStitchTime() < 40.0F) {
                this.setDeepWoundTime(Rand.Next(10.0F, this.getStitchTime()));
                this.setBleedingTime(Rand.Next(10.0F, this.getStitchTime()));
                this.setStitchTime(0.0F);
                this.setDeepWounded(true);
            } else {
                this.setScratchTime(Rand.Next(2.0F, this.getStitchTime() - 40.0F));
                this.scratched = true;
                this.setStitchTime(0.0F);
            }
        }

        this.stitched = Stitched;
    }

    public void damageFromFirearm(float damage) {
        this.setHaveBullet(true, 0);
    }

    public float getPain() {
        float result = 0.0F;
        if (this.getScratchTime() > 0.0F) {
            result += this.getScratchTime() * 1.7F;
        }

        if (this.getCutTime() > 0.0F) {
            result += this.getCutTime() * 2.5F;
        }

        if (this.getBiteTime() > 0.0F) {
            if (this.bandaged()) {
                result += 30.0F;
            } else {
                result += 50.0F;
            }
        }

        if (this.getDeepWoundTime() > 0.0F) {
            result += this.getDeepWoundTime() * 3.7F;
        }

        if (this.getStitchTime() > 0.0F && this.getStitchTime() < 35.0F) {
            if (this.bandaged()) {
                result += (35.0F - this.getStitchTime()) / 2.0F;
            } else {
                result += 35.0F - this.getStitchTime();
            }
        }

        if (this.getFractureTime() > 0.0F) {
            if (this.getSplintFactor() > 0.0F) {
                result += this.getFractureTime() / 2.0F;
            } else {
                result += this.getFractureTime();
            }
        }

        if (this.haveBullet()) {
            result += 50.0F;
        }

        if (this.haveGlass()) {
            result += 10.0F;
        }

        if (this.getBurnTime() > 0.0F) {
            result += this.getBurnTime();
        }

        if (this.bandaged()) {
            result /= 1.5F;
        }

        if (this.getWoundInfectionLevel() > 0.0F) {
            result += this.getWoundInfectionLevel();
        }

        result += this.getAdditionalPain(true);
        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                result *= 0.7F;
                break;
            case 3:
                result *= 1.3F;
        }

        return result;
    }

    public float getBiteTime() {
        return this.biteTime;
    }

    public void setBiteTime(float biteTime) {
        this.biteTime = biteTime;
    }

    public float getDeepWoundTime() {
        return this.deepWoundTime;
    }

    public void setDeepWoundTime(float deepWoundTime) {
        this.deepWoundTime = deepWoundTime;
    }

    public boolean haveGlass() {
        return this.haveGlass;
    }

    public void setHaveGlass(boolean haveGlass) {
        this.haveGlass = haveGlass;
    }

    public float getStitchTime() {
        return this.stitchTime;
    }

    public void setStitchTime(float stitchTime) {
        this.stitchTime = stitchTime;
    }

    public int getIndex() {
        return BodyPartType.ToIndex(this.type);
    }

    public float getAlcoholLevel() {
        return this.alcoholLevel;
    }

    public void setAlcoholLevel(float alcoholLevel) {
        this.alcoholLevel = alcoholLevel;
    }

    public float getAdditionalPain(boolean includeStiffness) {
        return includeStiffness ? this.additionalPain + this.stiffness / 3.5F : this.additionalPain;
    }

    public float getAdditionalPain() {
        return this.additionalPain;
    }

    public void setAdditionalPain(float additionalPain) {
        this.additionalPain = additionalPain;
    }

    public String getBandageType() {
        return this.bandageType;
    }

    public void setBandageType(String bandageType) {
        this.bandageType = bandageType;
    }

    public boolean isGetBandageXp() {
        return this.getBandageXp;
    }

    public void setGetBandageXp(boolean getBandageXp) {
        this.getBandageXp = getBandageXp;
    }

    public boolean isGetStitchXp() {
        return this.getStitchXp;
    }

    public void setGetStitchXp(boolean getStitchXp) {
        this.getStitchXp = getStitchXp;
    }

    public float getSplintFactor() {
        return this.splintFactor;
    }

    public void setSplintFactor(float splintFactor) {
        this.splintFactor = splintFactor;
    }

    public float getFractureTime() {
        return this.fractureTime;
    }

    public void setFractureTime(float fractureTime) {
        this.fractureTime = fractureTime;
    }

    public boolean isGetSplintXp() {
        return this.getSplintXp;
    }

    public void setGetSplintXp(boolean getSplintXp) {
        this.getSplintXp = getSplintXp;
    }

    public boolean isSplint() {
        return this.splint;
    }

    public void setSplint(boolean splint, float splintFactor) {
        this.splint = splint;
        this.setSplintFactor(splintFactor);
        if (splint) {
            this.setGetSplintXp(false);
        }
    }

    public boolean haveBullet() {
        return this.haveBullet;
    }

    public void setHaveBullet(boolean haveBullet, int doctorLevel) {
        if (this.haveBullet && !haveBullet) {
            float deepWoundTime = Rand.Next(17.0F, 23.0F) - doctorLevel / 2;
            if (this.parentChar != null) {
                if (this.parentChar.hasTrait(CharacterTrait.FAST_HEALER)) {
                    deepWoundTime = Rand.Next(12.0F, 18.0F) - doctorLevel / 2;
                } else if (this.parentChar.hasTrait(CharacterTrait.SLOW_HEALER)) {
                    deepWoundTime = Rand.Next(22.0F, 28.0F) - doctorLevel / 2;
                }
            }

            switch (SandboxOptions.instance.injurySeverity.getValue()) {
                case 1:
                    deepWoundTime *= 0.5F;
                    break;
                case 3:
                    deepWoundTime *= 1.5F;
            }

            this.setDeepWoundTime(deepWoundTime);
            this.setDeepWounded(true);
            this.haveBullet = false;
            this.generateBleeding();
        } else if (haveBullet) {
            this.haveBullet = true;
            this.generateBleeding();
        }

        this.haveBullet = haveBullet;
    }

    public float getBurnTime() {
        return this.burnTime;
    }

    public void setBurnTime(float burnTime) {
        this.burnTime = burnTime;
    }

    public boolean isNeedBurnWash() {
        return this.needBurnWash;
    }

    public void setNeedBurnWash(boolean needBurnWash) {
        if (this.needBurnWash && !needBurnWash) {
            this.setLastTimeBurnWash(this.getBurnTime());
        }

        this.needBurnWash = needBurnWash;
    }

    public float getLastTimeBurnWash() {
        return this.lastTimeBurnWash;
    }

    public void setLastTimeBurnWash(float lastTimeBurnWash) {
        this.lastTimeBurnWash = lastTimeBurnWash;
    }

    public boolean isInfectedWound() {
        return this.infectedWound;
    }

    public void setInfectedWound(boolean infectedWound) {
        this.infectedWound = infectedWound;
    }

    public BodyPartType getType() {
        return this.type;
    }

    public float getBleedingTime() {
        return this.bleedingTime;
    }

    public void setBleedingTime(float bleedingTime) {
        this.bleedingTime = bleedingTime;
        if (!this.bandaged()) {
            this.setBleeding(bleedingTime > 0.0F);
        }
    }

    public boolean isDeepWounded() {
        return this.deepWounded;
    }

    public void setDeepWounded(boolean Wounded) {
        this.deepWounded = Wounded;
        if (Wounded) {
            this.bleeding = true;
            this.isBleedingStemmed = false;
            this.isCauterized = false;
            this.bandaged = false;
            this.stitched = false;
        }
    }

    public float getBandageLife() {
        return this.bandageLife;
    }

    public void setBandageLife(float bandageLife) {
        this.bandageLife = bandageLife;
        if (this.bandageLife <= 0.0F) {
            this.alcoholicBandage = false;
        }
    }

    public float getScratchTime() {
        return this.scratchTime;
    }

    public void setScratchTime(float scratchTime) {
        scratchTime = Math.min(100.0F, scratchTime);
        this.scratchTime = scratchTime;
    }

    public float getWoundInfectionLevel() {
        return this.woundInfectionLevel;
    }

    public void setWoundInfectionLevel(float infectedWound) {
        this.woundInfectionLevel = infectedWound;
        if (this.woundInfectionLevel <= 0.0F) {
            this.setInfectedWound(false);
            if (this.woundInfectionLevel < -2.0F) {
                this.woundInfectionLevel = -2.0F;
            }
        } else {
            this.setInfectedWound(true);
        }
    }

    public void setBurned() {
        float burnTime = Rand.Next(50.0F, 100.0F);
        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                burnTime *= 0.5F;
                break;
            case 3:
                burnTime *= 1.5F;
        }

        this.setBurnTime(burnTime);
        this.setNeedBurnWash(true);
        this.setLastTimeBurnWash(0.0F);
    }

    public String getSplintItem() {
        return this.splintItem;
    }

    public void setSplintItem(String splintItem) {
        this.splintItem = splintItem;
    }

    public float getPlantainFactor() {
        return this.plantainFactor;
    }

    public void setPlantainFactor(float plantainFactor) {
        this.plantainFactor = PZMath.clamp(plantainFactor, 0.0F, 100.0F);
    }

    public float getGarlicFactor() {
        return this.garlicFactor;
    }

    public void setGarlicFactor(float garlicFactor) {
        this.garlicFactor = PZMath.clamp(garlicFactor, 0.0F, 100.0F);
    }

    public float getComfreyFactor() {
        return this.comfreyFactor;
    }

    public void setComfreyFactor(float comfreyFactor) {
        this.comfreyFactor = PZMath.clamp(comfreyFactor, 0.0F, 100.0F);
    }

    public void sync(BodyPart other, BodyDamageSync.Updater updater) {
        if (updater.updateField((byte)1, this.health, other.health)) {
            other.health = this.health;
        }

        if (this.bandaged != other.bandaged) {
            updater.updateField((byte)2, this.bandaged);
            other.bandaged = this.bandaged;
        }

        if (this.bitten != other.bitten) {
            updater.updateField((byte)3, this.bitten);
            other.bitten = this.bitten;
        }

        if (this.bleeding != other.bleeding) {
            updater.updateField((byte)4, this.bleeding);
            other.bleeding = this.bleeding;
        }

        if (this.isBleedingStemmed != other.isBleedingStemmed) {
            updater.updateField((byte)5, this.isBleedingStemmed);
            other.isBleedingStemmed = this.isBleedingStemmed;
        }

        if (this.scratched != other.scratched) {
            updater.updateField((byte)7, this.scratched);
            other.scratched = this.scratched;
        }

        if (this.cut != other.cut) {
            updater.updateField((byte)39, this.cut);
            other.cut = this.cut;
        }

        if (this.stitched != other.stitched) {
            updater.updateField((byte)8, this.stitched);
            other.stitched = this.stitched;
        }

        if (this.deepWounded != other.deepWounded) {
            updater.updateField((byte)9, this.deepWounded);
            other.deepWounded = this.deepWounded;
        }

        if (this.isInfected != other.isInfected) {
            updater.updateField((byte)10, this.isInfected);
            other.isInfected = this.isInfected;
        }

        if (this.isFakeInfected != other.isFakeInfected) {
            updater.updateField((byte)11, this.isFakeInfected);
            other.isFakeInfected = this.isFakeInfected;
        }

        if (updater.updateField((byte)12, this.bandageLife, other.bandageLife)) {
            other.bandageLife = this.bandageLife;
        }

        if (updater.updateField((byte)13, this.scratchTime, other.scratchTime)) {
            other.scratchTime = this.scratchTime;
        }

        if (updater.updateField((byte)14, this.biteTime, other.biteTime)) {
            other.biteTime = this.biteTime;
        }

        if (this.alcoholicBandage != other.alcoholicBandage) {
            updater.updateField((byte)15, this.alcoholicBandage);
            other.alcoholicBandage = this.alcoholicBandage;
        }

        if (updater.updateField((byte)16, this.woundInfectionLevel, other.woundInfectionLevel)) {
            other.woundInfectionLevel = this.woundInfectionLevel;
        }

        if (updater.updateField((byte)41, this.stiffness, other.stiffness)) {
            other.stiffness = this.stiffness;
        }

        if (this.infectedWound != other.infectedWound) {
            updater.updateField((byte)17, this.infectedWound);
            other.infectedWound = this.infectedWound;
        }

        if (updater.updateField((byte)18, this.bleedingTime, other.bleedingTime)) {
            other.bleedingTime = this.bleedingTime;
        }

        if (updater.updateField((byte)19, this.deepWoundTime, other.deepWoundTime)) {
            other.deepWoundTime = this.deepWoundTime;
        }

        if (updater.updateField((byte)40, this.cutTime, other.cutTime)) {
            other.cutTime = this.cutTime;
        }

        if (this.haveGlass != other.haveGlass) {
            updater.updateField((byte)20, this.haveGlass);
            other.haveGlass = this.haveGlass;
        }

        if (updater.updateField((byte)21, this.stitchTime, other.stitchTime)) {
            other.stitchTime = this.stitchTime;
        }

        if (updater.updateField((byte)22, this.alcoholLevel, other.alcoholLevel)) {
            other.alcoholLevel = this.alcoholLevel;
        }

        if (updater.updateField((byte)23, this.additionalPain, other.additionalPain)) {
            other.additionalPain = this.additionalPain;
        }

        if (this.bandageType != other.bandageType) {
            updater.updateField((byte)24, this.bandageType);
            other.bandageType = this.bandageType;
        }

        if (this.getBandageXp != other.getBandageXp) {
            updater.updateField((byte)25, this.getBandageXp);
            other.getBandageXp = this.getBandageXp;
        }

        if (this.getStitchXp != other.getStitchXp) {
            updater.updateField((byte)26, this.getStitchXp);
            other.getStitchXp = this.getStitchXp;
        }

        if (this.getSplintXp != other.getSplintXp) {
            updater.updateField((byte)27, this.getSplintXp);
            other.getSplintXp = this.getSplintXp;
        }

        if (updater.updateField((byte)28, this.fractureTime, other.fractureTime)) {
            other.fractureTime = this.fractureTime;
        }

        if (this.splint != other.splint) {
            updater.updateField((byte)29, this.splint);
            other.splint = this.splint;
        }

        if (updater.updateField((byte)30, this.splintFactor, other.splintFactor)) {
            other.splintFactor = this.splintFactor;
        }

        if (this.haveBullet != other.haveBullet) {
            updater.updateField((byte)31, this.haveBullet);
            other.haveBullet = this.haveBullet;
        }

        if (updater.updateField((byte)32, this.burnTime, other.burnTime)) {
            other.burnTime = this.burnTime;
        }

        if (this.needBurnWash != other.needBurnWash) {
            updater.updateField((byte)33, this.needBurnWash);
            other.needBurnWash = this.needBurnWash;
        }

        if (updater.updateField((byte)34, this.lastTimeBurnWash, other.lastTimeBurnWash)) {
            other.lastTimeBurnWash = this.lastTimeBurnWash;
        }

        if (this.splintItem != other.splintItem) {
            updater.updateField((byte)35, this.splintItem);
            other.splintItem = this.splintItem;
        }

        if (updater.updateField((byte)36, this.plantainFactor, other.plantainFactor)) {
            other.plantainFactor = this.plantainFactor;
        }

        if (updater.updateField((byte)37, this.comfreyFactor, other.comfreyFactor)) {
            other.comfreyFactor = this.comfreyFactor;
        }

        if (updater.updateField((byte)38, this.garlicFactor, other.garlicFactor)) {
            other.garlicFactor = this.garlicFactor;
        }
    }

    public void sync(ByteBuffer bb, byte id) {
        switch (id) {
            case 1:
                this.health = bb.getFloat();
                break;
            case 2:
                this.bandaged = bb.get() == 1;
                break;
            case 3:
                this.bitten = bb.get() == 1;
                break;
            case 4:
                this.bleeding = bb.get() == 1;
                break;
            case 5:
                this.isBleedingStemmed = bb.get() == 1;
                break;
            case 6:
                this.isCauterized = bb.get() == 1;
                break;
            case 7:
                this.scratched = bb.get() == 1;
                break;
            case 8:
                this.stitched = bb.get() == 1;
                break;
            case 9:
                this.deepWounded = bb.get() == 1;
                break;
            case 10:
                this.isInfected = bb.get() == 1;
                break;
            case 11:
                this.isFakeInfected = bb.get() == 1;
                break;
            case 12:
                this.bandageLife = bb.getFloat();
                break;
            case 13:
                this.scratchTime = bb.getFloat();
                break;
            case 14:
                this.biteTime = bb.getFloat();
                break;
            case 15:
                this.alcoholicBandage = bb.get() == 1;
                break;
            case 16:
                this.woundInfectionLevel = bb.getFloat();
                break;
            case 17:
                this.infectedWound = bb.get() == 1;
                break;
            case 18:
                this.bleedingTime = bb.getFloat();
                break;
            case 19:
                this.deepWoundTime = bb.getFloat();
                break;
            case 20:
                this.haveGlass = bb.get() == 1;
                break;
            case 21:
                this.stitchTime = bb.getFloat();
                break;
            case 22:
                this.alcoholLevel = bb.getFloat();
                break;
            case 23:
                this.additionalPain = bb.getFloat();
                break;
            case 24:
                this.bandageType = GameWindow.ReadStringUTF(bb);
                break;
            case 25:
                this.getBandageXp = bb.get() == 1;
                break;
            case 26:
                this.getStitchXp = bb.get() == 1;
                break;
            case 27:
                this.getSplintXp = bb.get() == 1;
                break;
            case 28:
                this.fractureTime = bb.getFloat();
                break;
            case 29:
                this.splint = bb.get() == 1;
                break;
            case 30:
                this.splintFactor = bb.getFloat();
                break;
            case 31:
                this.haveBullet = bb.get() == 1;
                break;
            case 32:
                this.burnTime = bb.getFloat();
                break;
            case 33:
                this.needBurnWash = bb.get() == 1;
                break;
            case 34:
                this.lastTimeBurnWash = bb.getFloat();
                break;
            case 35:
                this.splintItem = GameWindow.ReadStringUTF(bb);
                break;
            case 36:
                this.plantainFactor = bb.getFloat();
                break;
            case 37:
                this.comfreyFactor = bb.getFloat();
                break;
            case 38:
                this.garlicFactor = bb.getFloat();
                break;
            case 39:
                this.cut = bb.get() == 1;
                break;
            case 40:
                this.cutTime = bb.getFloat();
                break;
            case 41:
                this.stiffness = bb.getFloat();
        }
    }

    public void syncWrite(ByteBufferWriter bb, int id) {
        switch (id) {
            case 1:
                bb.putFloat(this.health);
                break;
            case 2:
                bb.putByte((byte)(this.bandaged ? 1 : 0));
                break;
            case 3:
                bb.putByte((byte)(this.bitten ? 1 : 0));
                break;
            case 4:
                bb.putByte((byte)(this.bleeding ? 1 : 0));
                break;
            case 5:
                bb.putByte((byte)(this.isBleedingStemmed ? 1 : 0));
                break;
            case 6:
                bb.putByte((byte)(this.isCauterized ? 1 : 0));
                break;
            case 7:
                bb.putByte((byte)(this.scratched ? 1 : 0));
                break;
            case 8:
                bb.putByte((byte)(this.stitched ? 1 : 0));
                break;
            case 9:
                bb.putByte((byte)(this.deepWounded ? 1 : 0));
                break;
            case 10:
                bb.putByte((byte)(this.isInfected ? 1 : 0));
                break;
            case 11:
                bb.putByte((byte)(this.isFakeInfected ? 1 : 0));
                break;
            case 12:
                bb.putFloat(this.bandageLife);
                break;
            case 13:
                bb.putFloat(this.scratchTime);
                break;
            case 14:
                bb.putFloat(this.biteTime);
                break;
            case 15:
                bb.putByte((byte)(this.alcoholicBandage ? 1 : 0));
                break;
            case 16:
                bb.putFloat(this.woundInfectionLevel);
                break;
            case 17:
                bb.putByte((byte)(this.infectedWound ? 1 : 0));
                break;
            case 18:
                bb.putFloat(this.bleedingTime);
                break;
            case 19:
                bb.putFloat(this.deepWoundTime);
                break;
            case 20:
                bb.putByte((byte)(this.haveGlass ? 1 : 0));
                break;
            case 21:
                bb.putFloat(this.stitchTime);
                break;
            case 22:
                bb.putFloat(this.alcoholLevel);
                break;
            case 23:
                bb.putFloat(this.additionalPain);
                break;
            case 24:
                bb.putUTF(this.bandageType);
                break;
            case 25:
                bb.putByte((byte)(this.getBandageXp ? 1 : 0));
                break;
            case 26:
                bb.putByte((byte)(this.getStitchXp ? 1 : 0));
                break;
            case 27:
                bb.putByte((byte)(this.getSplintXp ? 1 : 0));
                break;
            case 28:
                bb.putFloat(this.fractureTime);
                break;
            case 29:
                bb.putByte((byte)(this.splint ? 1 : 0));
                break;
            case 30:
                bb.putFloat(this.splintFactor);
                break;
            case 31:
                bb.putByte((byte)(this.haveBullet ? 1 : 0));
                break;
            case 32:
                bb.putFloat(this.burnTime);
                break;
            case 33:
                bb.putByte((byte)(this.needBurnWash ? 1 : 0));
                break;
            case 34:
                bb.putFloat(this.lastTimeBurnWash);
                break;
            case 35:
                bb.putUTF(this.splintItem);
                break;
            case 36:
                bb.putFloat(this.plantainFactor);
                break;
            case 37:
                bb.putFloat(this.comfreyFactor);
                break;
            case 38:
                bb.putFloat(this.garlicFactor);
                break;
            case 39:
                bb.putByte((byte)(this.cut ? 1 : 0));
                break;
            case 40:
                bb.putFloat(this.cutTime);
                break;
            case 41:
                bb.putFloat(this.stiffness);
        }
    }

    public float getCutTime() {
        return this.cutTime;
    }

    public void setCutTime(float cutTime) {
        cutTime = Math.min(100.0F, cutTime);
        this.cutTime = cutTime;
    }

    public boolean isCut() {
        return this.cut;
    }

    public float getScratchSpeedModifier() {
        return this.scratchSpeedModifier;
    }

    public void setScratchSpeedModifier(float scratchSpeedModifier) {
        this.scratchSpeedModifier = scratchSpeedModifier;
    }

    public float getCutSpeedModifier() {
        return this.cutSpeedModifier;
    }

    public void setCutSpeedModifier(float cutSpeedModifier) {
        this.cutSpeedModifier = cutSpeedModifier;
    }

    public float getBurnSpeedModifier() {
        return this.burnSpeedModifier;
    }

    public void setBurnSpeedModifier(float burnSpeedModifier) {
        this.burnSpeedModifier = burnSpeedModifier;
    }

    public float getDeepWoundSpeedModifier() {
        return this.deepWoundSpeedModifier;
    }

    public void setDeepWoundSpeedModifier(float deepWoundSpeedModifier) {
        this.deepWoundSpeedModifier = deepWoundSpeedModifier;
    }

    public boolean isBurnt() {
        return this.getBurnTime() > 0.0F;
    }

    /**
     * Generate an amount of bleeding time
     *  will depend on injuries type and body part type.
     *  Use this instead of setBleedingTime() so all is automated.
     */
    public void generateBleeding() {
        float bleedingTime = 0.0F;
        if (this.scratched()) {
            bleedingTime = Rand.Next(this.getScratchTime() * 0.3F, this.getScratchTime() * 0.6F);
        }

        if (this.isCut()) {
            bleedingTime += Rand.Next(this.getCutTime() * 0.7F, this.getCutTime() * 1.0F);
        }

        if (this.isBurnt()) {
            bleedingTime += Rand.Next(this.getBurnTime() * 0.3F, this.getBurnTime() * 0.6F);
        }

        if (this.isDeepWounded()) {
            bleedingTime += Rand.Next(this.getDeepWoundTime() * 0.7F, this.getDeepWoundTime());
        }

        if (this.haveGlass()) {
            bleedingTime += Rand.Next(5.0F, 10.0F);
        }

        if (this.haveBullet()) {
            bleedingTime += Rand.Next(5.0F, 10.0F);
        }

        if (this.bitten()) {
            bleedingTime += Rand.Next(7.5F, 15.0F);
        }

        switch (SandboxOptions.instance.injurySeverity.getValue()) {
            case 1:
                bleedingTime *= 0.5F;
                break;
            case 3:
                bleedingTime *= 1.5F;
        }

        bleedingTime *= BodyPartType.getBleedingTimeModifyer(BodyPartType.ToIndex(this.getType()));
        this.setBleedingTime(bleedingTime);
    }

    public float getInnerTemperature() {
        return this.thermalNode != null ? this.thermalNode.getCelcius() : 0.0F;
    }

    public float getSkinTemperature() {
        return this.thermalNode != null ? this.thermalNode.getSkinCelcius() : 0.0F;
    }

    public float getDistToCore() {
        return this.thermalNode != null ? this.thermalNode.getDistToCore() : BodyPartType.GetDistToCore(this.type);
    }

    public float getSkinSurface() {
        return this.thermalNode != null ? this.thermalNode.getSkinSurface() : BodyPartType.GetSkinSurface(this.type);
    }

    public Thermoregulator.ThermalNode getThermalNode() {
        return this.thermalNode;
    }

    public float getWetness() {
        return this.wetness;
    }

    public void setWetness(float wetness) {
        this.wetness = PZMath.clamp(wetness, 0.0F, 100.0F);
    }

    public float getStiffness() {
        return this.stiffness;
    }

    public void setStiffness(float stiffness) {
        this.stiffness = PZMath.clamp(stiffness, 0.0F, 100.0F);
    }

    public boolean hasDirtyClothing() {
        return this.parentChar.hasDirtyClothing(BodyPartType.ToIndex(this.getType()));
    }

    public boolean hasBloodyClothing() {
        return this.parentChar.hasBloodyClothing(BodyPartType.ToIndex(this.getType()));
    }

    public void addStiffness(float stiffness) {
        this.stiffness = PZMath.clamp(this.getStiffness() + stiffness, 0.0F, 100.0F);
    }

    public float getDamageScaler() {
        return this.damageScaler;
    }

    public float getBandageNeededDamageLevel() {
        if (this.bandaged()) {
            return 0.0F;
        } else {
            float bleedDamage = this.getBleedingTime() > 0.0F ? 0.2857143F * (this.getBleedingTime() / 10.0F) : 0.0F;
            float biteDamage = this.bitten() ? 2.1875F : 0.0F;
            float burnDamage = this.isBurnt() ? 3.75F : 0.0F;
            float cutDamage = this.isCut() ? 1.875F : 0.0F;
            float deepWoundDamage = this.getDeepWoundTime() > 0.0F & !this.stitched() ? 3.125F : 0.0F;
            float scratchDamage = this.scratched() ? 0.9375F : 0.0F;
            float damage = Math.max(Math.max(Math.max(Math.max(biteDamage, burnDamage), cutDamage), deepWoundDamage), scratchDamage) + bleedDamage;
            return damage * this.damageScaler;
        }
    }
}
