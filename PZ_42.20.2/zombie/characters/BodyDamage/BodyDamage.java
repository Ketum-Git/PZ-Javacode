// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.BodyDamage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.FliesSound;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.audio.MusicIntensityConfig;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.ClothingWetness;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.Stats;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.inventory.types.WeaponType;
import zombie.iso.CorpseCount;
import zombie.iso.IsoGridSquare;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.WeaponCategory;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public final class BodyDamage {
    private static final String behindStr = "BEHIND";
    private static final String leftStr = "LEFT";
    private static final String rightStr = "RIGHT";
    private final ArrayList<BodyPart> bodyParts = new ArrayList<>(18);
    private final ArrayList<BodyPartLast> bodyPartsLastState = new ArrayList<>(18);
    private int damageModCount = 60;
    private float infectionGrowthRate = 0.001F;
    private boolean isInfected;
    private float infectionTime = -1.0F;
    private float infectionMortalityDuration = -1.0F;
    public boolean isFakeInfected;
    private float overallBodyHealth = 100.0F;
    private float standardHealthAddition = 0.002F;
    private float reducedHealthAddition = 0.0013F;
    private float severlyReducedHealthAddition = 8.0E-4F;
    private float sleepingHealthAddition = 0.02F;
    private float healthFromFood = 0.015F;
    private float healthReductionFromSevereBadMoodles = 0.0165F;
    private int standardHealthFromFoodTime = 1600;
    private float healthFromFoodTimer;
    private float boredomDecreaseFromReading = 0.5F;
    private float initialThumpPain = 14.0F;
    private float initialScratchPain = 18.0F;
    private float initialBitePain = 25.0F;
    private float initialWoundPain = 80.0F;
    private float continualPainIncrease = 0.001F;
    private float painReductionFromMeds = 30.0F;
    private float standardPainReductionWhenWell = 0.01F;
    private int oldNumZombiesVisible;
    private int currentNumZombiesVisible;
    private float panicIncreaseValue = 7.0F;
    private final float panicIncreaseValueFrame = 0.035F;
    private float panicReductionValue = 0.06F;
    private float drunkIncreaseValue = 400.0F;
    private float drunkReductionValue = 0.0042F;
    private boolean isOnFire;
    private boolean burntToDeath;
    private float catchACold;
    private boolean hasACold;
    private float coldStrength;
    private float coldProgressionRate = 0.0112F;
    private float timeToSneezeOrCough = -1.0F;
    private final int smokerSneezeTimerMin = 43200;
    private final int smokerSneezeTimerMax = 129600;
    private int mildColdSneezeTimerMin = 600;
    private int mildColdSneezeTimerMax = 800;
    private int coldSneezeTimerMin = 300;
    private int coldSneezeTimerMax = 600;
    private int nastyColdSneezeTimerMin = 200;
    private int nastyColdSneezeTimerMax = 300;
    private int sneezeCoughActive;
    private int sneezeCoughTime;
    private int sneezeCoughDelay = 25;
    private float coldDamageStage;
    private final IsoGameCharacter parentChar;
    private final Stats stats;
    private int remotePainLevel;
    private boolean reduceFakeInfection;
    private float painReduction;
    private float coldReduction;
    private Thermoregulator thermoregulator;
    public static final float InfectionLevelToZombify = 0.001F;
    private boolean wasDraggingCorpse;
    private boolean startedDraggingCorpse;

    public BodyDamage(IsoGameCharacter parentCharacter) {
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Upper, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Lower, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Head, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Neck, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Groin, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_R, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_L, parentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_R, parentCharacter));

        for (BodyPart part : this.bodyParts) {
            this.bodyPartsLastState.add(new BodyPartLast());
        }

        this.RestoreToFullHealth();
        this.parentChar = parentCharacter;
        this.stats = this.parentChar.getStats();
        if (this.parentChar instanceof IsoPlayer) {
            this.thermoregulator = new Thermoregulator(this);
        }

        this.setBodyPartsLastState();
    }

    public BodyPart getBodyPart(BodyPartType type) {
        return this.bodyParts.get(BodyPartType.ToIndex(type));
    }

    public BodyPartLast getBodyPartsLastState(BodyPartType type) {
        return this.bodyPartsLastState.get(BodyPartType.ToIndex(type));
    }

    public void setBodyPartsLastState() {
        for (int n = 0; n < this.getBodyParts().size(); n++) {
            BodyPart p = this.getBodyParts().get(n);
            BodyPartLast pls = this.bodyPartsLastState.get(n);
            pls.copy(p);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        for (int n = 0; n < this.getBodyParts().size(); n++) {
            BodyPart p = this.getBodyParts().get(n);
            p.setCut(input.get() != 0, false);
            p.SetBitten(input.get() != 0);
            p.setScratched(input.get() != 0, false);
            p.setBandaged(input.get() != 0, 0.0F);
            p.setBleeding(input.get() != 0);
            p.setDeepWounded(input.get() != 0);
            p.SetFakeInfected(input.get() != 0);
            p.SetInfected(input.get() != 0);
            p.SetHealth(input.getFloat());
            if (p.bandaged()) {
                p.setBandageLife(input.getFloat());
            }

            p.setInfectedWound(input.get() != 0);
            if (p.isInfectedWound()) {
                p.setWoundInfectionLevel(input.getFloat());
            }

            p.setCutTime(input.getFloat());
            p.setBiteTime(input.getFloat());
            p.setScratchTime(input.getFloat());
            p.setBleedingTime(input.getFloat());
            p.setAlcoholLevel(input.getFloat());
            p.setAdditionalPain(input.getFloat());
            p.setDeepWoundTime(input.getFloat());
            p.setHaveGlass(input.get() != 0);
            p.setGetBandageXp(input.get() != 0);
            p.setStitched(input.get() != 0);
            p.setStitchTime(input.getFloat());
            p.setGetStitchXp(input.get() != 0);
            p.setGetSplintXp(input.get() != 0);
            p.setFractureTime(input.getFloat());
            p.setSplint(input.get() != 0, 0.0F);
            if (p.isSplint()) {
                p.setSplintFactor(input.getFloat());
            }

            p.setHaveBullet(input.get() != 0, 0);
            p.setBurnTime(input.getFloat());
            p.setNeedBurnWash(input.get() != 0);
            p.setLastTimeBurnWash(input.getFloat());
            p.setSplintItem(GameWindow.ReadString(input));
            p.setBandageType(GameWindow.ReadString(input));
            p.setCutTime(input.getFloat());
            p.setWetness(input.getFloat());
            p.setStiffness(input.getFloat());
            if (worldVersion >= 227) {
                p.setComfreyFactor(input.getFloat());
                p.setGarlicFactor(input.getFloat());
                p.setPlantainFactor(input.getFloat());
            }
        }

        this.setBodyPartsLastState();
        this.loadMainFields(input, worldVersion);
        if (input.get() != 0) {
            if (this.thermoregulator != null) {
                this.thermoregulator.load(input, worldVersion);
            } else {
                Thermoregulator thermos = new Thermoregulator(this);
                thermos.load(input, worldVersion);
                DebugLog.log("Couldnt load Thermoregulator, == null");
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        for (int n = 0; n < this.getBodyParts().size(); n++) {
            BodyPart p = this.getBodyParts().get(n);
            output.put((byte)(p.isCut() ? 1 : 0));
            output.put((byte)(p.bitten() ? 1 : 0));
            output.put((byte)(p.scratched() ? 1 : 0));
            output.put((byte)(p.bandaged() ? 1 : 0));
            output.put((byte)(p.bleeding() ? 1 : 0));
            output.put((byte)(p.deepWounded() ? 1 : 0));
            output.put((byte)(p.IsFakeInfected() ? 1 : 0));
            output.put((byte)(p.IsInfected() ? 1 : 0));
            output.putFloat(p.getHealth());
            if (p.bandaged()) {
                output.putFloat(p.getBandageLife());
            }

            output.put((byte)(p.isInfectedWound() ? 1 : 0));
            if (p.isInfectedWound()) {
                output.putFloat(p.getWoundInfectionLevel());
            }

            output.putFloat(p.getCutTime());
            output.putFloat(p.getBiteTime());
            output.putFloat(p.getScratchTime());
            output.putFloat(p.getBleedingTime());
            output.putFloat(p.getAlcoholLevel());
            output.putFloat(p.getAdditionalPain());
            output.putFloat(p.getDeepWoundTime());
            output.put((byte)(p.haveGlass() ? 1 : 0));
            output.put((byte)(p.isGetBandageXp() ? 1 : 0));
            output.put((byte)(p.stitched() ? 1 : 0));
            output.putFloat(p.getStitchTime());
            output.put((byte)(p.isGetStitchXp() ? 1 : 0));
            output.put((byte)(p.isGetSplintXp() ? 1 : 0));
            output.putFloat(p.getFractureTime());
            output.put((byte)(p.isSplint() ? 1 : 0));
            if (p.isSplint()) {
                output.putFloat(p.getSplintFactor());
            }

            output.put((byte)(p.haveBullet() ? 1 : 0));
            output.putFloat(p.getBurnTime());
            output.put((byte)(p.isNeedBurnWash() ? 1 : 0));
            output.putFloat(p.getLastTimeBurnWash());
            GameWindow.WriteString(output, p.getSplintItem());
            GameWindow.WriteString(output, p.getBandageType());
            output.putFloat(p.getCutTime());
            output.putFloat(p.getWetness());
            output.putFloat(p.getStiffness());
            output.putFloat(p.getComfreyFactor());
            output.putFloat(p.getGarlicFactor());
            output.putFloat(p.getPlantainFactor());
        }

        this.saveMainFields(output);
        output.put((byte)(this.thermoregulator != null ? 1 : 0));
        if (this.thermoregulator != null) {
            this.thermoregulator.save(output);
        }
    }

    public void saveMainFields(ByteBuffer output) {
        output.putFloat(this.getCatchACold());
        output.put((byte)(this.isHasACold() ? 1 : 0));
        output.putFloat(this.getColdStrength());
        output.putInt((int)this.getTimeToSneezeOrCough());
        output.put((byte)(this.isReduceFakeInfection() ? 1 : 0));
        output.putFloat(this.healthFromFoodTimer);
        output.putFloat(this.painReduction);
        output.putFloat(this.coldReduction);
        output.putFloat(this.infectionTime);
        output.putFloat(this.infectionMortalityDuration);
        output.putFloat(this.coldDamageStage);
    }

    public void loadMainFields(ByteBuffer input, int worldVersion) {
        this.setCatchACold(input.getFloat());
        this.setHasACold(input.get() != 0);
        this.setColdStrength(input.getFloat());
        if (worldVersion >= 222) {
            this.setTimeToSneezeOrCough(input.getInt());
        }

        this.setReduceFakeInfection(input.get() != 0);
        this.setHealthFromFoodTimer(input.getFloat());
        this.painReduction = input.getFloat();
        this.coldReduction = input.getFloat();
        this.infectionTime = input.getFloat();
        this.infectionMortalityDuration = input.getFloat();
        this.coldDamageStage = input.getFloat();
        this.calculateOverallHealth();
    }

    public boolean IsFakeInfected() {
        return this.isIsFakeInfected();
    }

    public void OnFire(boolean onFire) {
        this.setIsOnFire(onFire);
    }

    public boolean IsOnFire() {
        return this.isIsOnFire();
    }

    public boolean WasBurntToDeath() {
        return this.isBurntToDeath();
    }

    public void IncreasePanicFloat(float delta) {
        float del = 1.0F;
        if (this.parentChar.getBetaEffect() > 0.0F) {
            del -= this.parentChar.getBetaDelta();
            if (del > 1.0F) {
                del = 1.0F;
            }

            if (del < 0.0F) {
                del = 0.0F;
            }
        }

        if (this.parentChar.hasTrait(CharacterTrait.COWARDLY)) {
            del *= 2.0F;
        }

        if (this.parentChar.hasTrait(CharacterTrait.BRAVE)) {
            del *= 0.3F;
        }

        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            del *= 0.15F;
        }

        this.stats.add(CharacterStat.PANIC, this.getPanicIncreaseValueFrame() * delta * del);
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
        }
    }

    public void IncreasePanic(int numNewZombiesSeen) {
        if (this.parentChar.getVehicle() != null) {
            numNewZombiesSeen /= 2;
        }

        float del = 1.0F;
        if (this.parentChar.getBetaEffect() > 0.0F) {
            del -= this.parentChar.getBetaDelta();
            if (del > 1.0F) {
                del = 1.0F;
            }

            if (del < 0.0F) {
                del = 0.0F;
            }
        }

        if (this.parentChar.hasTrait(CharacterTrait.COWARDLY)) {
            del *= 2.0F;
        }

        if (this.parentChar.hasTrait(CharacterTrait.BRAVE)) {
            del *= 0.3F;
        }

        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            del *= 0.15F;
        }

        this.stats.add(CharacterStat.PANIC, this.getPanicIncreaseValue() * numNewZombiesSeen * del);
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
        }
    }

    public void ReducePanic() {
        if (!this.stats.isAtMinimum(CharacterStat.PANIC)) {
            float delta = this.getPanicReductionValue() * GameTime.getInstance().getThirtyFPSMultiplier();
            int monthSurvived = PZMath.fastfloor((float)((int)this.parentChar.getHoursSurvived() / 24 / 30));
            if (monthSurvived > 5) {
                monthSurvived = 5;
            }

            delta += this.getPanicReductionValue() * monthSurvived;
            if (this.parentChar.isAsleep()) {
                delta *= 2.0F;
            }

            this.stats.remove(CharacterStat.PANIC, delta);
        }
    }

    public void UpdateDraggingCorpse() {
        boolean isDraggingCorpse = this.parentChar.isDraggingCorpse();
        if (isDraggingCorpse != this.getWasDraggingCorpse()) {
            this.startedDraggingCorpse = isDraggingCorpse;
            this.setWasDraggingCorpse(isDraggingCorpse);
        } else {
            this.startedDraggingCorpse = false;
        }
    }

    public void UpdatePanicState() {
        int numVisibleZombies = this.stats.numVisibleZombies;
        int oldNumZombiesVisible = this.getOldNumZombiesVisible();
        this.setOldNumZombiesVisible(numVisibleZombies);
        int inNumNewZombies = numVisibleZombies - oldNumZombiesVisible;
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.stats.reset(CharacterStat.PANIC);
        } else {
            int increasePanicCount = 0;
            if (inNumNewZombies > 0) {
                increasePanicCount += inNumNewZombies;
            }

            if (increasePanicCount > 0) {
                this.IncreasePanic(increasePanicCount);
            } else {
                this.ReducePanic();
            }
        }
    }

    public void JustDrankBooze(Food food, float percentage) {
        float del = 1.0F;
        if (food.getBaseHunger() != 0.0F) {
            percentage = food.getHungChange() * percentage / food.getBaseHunger() * 2.0F;
        }

        del *= percentage;
        if (food.getName().toLowerCase().contains("beer") || food.hasTag(ItemTag.LOW_ALCOHOL)) {
            del *= 0.25F;
        }

        if (this.stats.get(CharacterStat.HUNGER) > 0.8F) {
            del *= 1.25F;
        } else if (this.stats.get(CharacterStat.HUNGER) > 0.6F) {
            del *= 1.1F;
        }

        this.stats.add(CharacterStat.INTOXICATION, this.getDrunkIncreaseValue() * del);
        this.parentChar.SleepingTablet(0.02F * percentage);
        this.parentChar.BetaAntiDepress(0.4F * percentage);
        this.parentChar.BetaBlockers(0.2F * percentage);
        this.parentChar.PainMeds(0.2F * percentage);
    }

    public void JustDrankBoozeFluid(float alcohol) {
        float del = 1.0F;
        del *= alcohol;
        if (this.stats.get(CharacterStat.HUNGER) > 0.8F) {
            del *= 1.1F;
        } else if (this.stats.get(CharacterStat.HUNGER) > 0.6F) {
            del *= 1.25F;
        }

        this.stats.add(CharacterStat.INTOXICATION, this.getDrunkIncreaseValue() * del);
        this.parentChar.SleepingTablet(0.02F * alcohol);
        this.parentChar.BetaAntiDepress(0.4F * alcohol);
        this.parentChar.BetaBlockers(0.2F * alcohol);
        this.parentChar.PainMeds(0.2F * alcohol);
    }

    public void JustTookPill(InventoryItem pill) {
        if ("PillsBeta".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.BetaBlockers(0.15F);
            } else {
                this.parentChar.BetaBlockers(0.3F);
            }
        } else if ("PillsAntiDep".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.BetaAntiDepress(0.15F);
            } else {
                this.parentChar.BetaAntiDepress(0.3F);
            }
        } else if ("PillsSleepingTablets".equals(pill.getType())) {
            this.parentChar.SleepingTablet(0.1F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                isoPlayer.setSleepingPillsTaken(isoPlayer.getSleepingPillsTaken() + 1);
            }
        } else if ("Pills".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.PainMeds(0.15F);
            } else {
                this.parentChar.PainMeds(0.45F);
            }
        } else if ("PillsVitamins".equals(pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.stats.add(CharacterStat.FATIGUE, pill.getFatigueChange() / 2.0F);
            } else {
                this.stats.add(CharacterStat.FATIGUE, pill.getFatigueChange());
            }
        }

        this.stats.add(CharacterStat.STRESS, pill.getStressChange());
        DrainableComboItem pill2 = (DrainableComboItem)pill;
        Object functionObj = LuaManager.getFunctionObject(pill2.getOnEat());
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, pill, this.parentChar);
        }

        pill.UseAndSync();
    }

    public void JustAteFood(Food newFood, float percentage) {
        this.JustAteFood(newFood, percentage, false);
    }

    public void JustAteFood(Food newFood, float percentage, boolean useUtensil) {
        if (newFood.getPoisonPower() > 0) {
            float poisonPower = newFood.getPoisonPower() * percentage;
            if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT) && !Objects.equals(newFood.getType(), "Bleach")) {
                poisonPower /= 2.0F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                poisonPower *= 2.0F;
            }

            this.stats.add(CharacterStat.POISON, poisonPower);
            this.stats.add(CharacterStat.PAIN, newFood.getPoisonPower() * percentage / 6.0F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                String debugStr = String.format(
                    "Player %s just ate poisoned food %s with poison power %f", isoPlayer.getDisplayName(), newFood.getDisplayName(), poisonPower
                );
                DebugType.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write(debugStr);
            }
        }

        if (newFood.isTainted()) {
            float poisonPowerx = 20.0F * percentage;
            this.stats.add(CharacterStat.POISON, poisonPowerx);
            this.stats.add(CharacterStat.PAIN, 10.0F * percentage / 6.0F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                String debugStr = String.format(
                    "Player %s just ate tainted food %s with poison power %f", isoPlayer.getDisplayName(), newFood.getDisplayName(), poisonPowerx
                );
                DebugType.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write(debugStr);
            }
        }

        if (newFood.getReduceInfectionPower() > 0.0F) {
            this.parentChar.setReduceInfectionPower(newFood.getReduceInfectionPower());
        }

        float modifier = 1.0F;
        if (useUtensil) {
            if (newFood.getBoredomChange() * percentage < 0.0F) {
                modifier = 1.25F;
            } else {
                modifier = 0.75F;
            }

            DebugLog.log("boredomChange %modifier from using an eating utensil: " + modifier);
        }

        this.stats.add(CharacterStat.BOREDOM, newFood.getBoredomChange() * percentage * modifier);
        modifier = 1.0F;
        if (useUtensil) {
            if (newFood.getUnhappyChange() * percentage < 0.0F) {
                modifier = 1.25F;
            } else {
                modifier = 0.75F;
            }

            DebugLog.log("unhappyChange %modifier from using an eating utensil: " + modifier);
        }

        this.stats.add(CharacterStat.UNHAPPINESS, newFood.getUnhappyChange() * percentage * modifier);
        if (newFood.isAlcoholic()) {
            this.JustDrankBooze(newFood, percentage);
        }

        if (this.stats.isAtMinimum(CharacterStat.HUNGER)) {
            float hungerChange = Math.abs(newFood.getHungerChange()) * percentage;
            this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            if (newFood.isCooked()) {
                this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            }

            if (this.getHealthFromFoodTimer() > 11000.0F) {
                this.setHealthFromFoodTimer(11000.0F);
            }
        }

        if (!"Tutorial".equals(Core.getInstance().getGameMode())) {
            if (!newFood.isCooked() && newFood.isbDangerousUncooked()) {
                this.setHealthFromFoodTimer(0.0F);
                int illnessChance = 75;
                if (newFood.hasTag(ItemTag.EGG)) {
                    illnessChance = 5;
                }

                if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                    illnessChance /= 2;
                    if (newFood.hasTag(ItemTag.EGG)) {
                        illnessChance = 0;
                    }
                }

                if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                    illnessChance *= 2;
                }

                if (illnessChance > 0 && !this.isInfected() && !newFood.isBurnt()) {
                    this.stats.add(CharacterStat.POISON, 15.0F * percentage);
                }
            }

            if (newFood.getAge() >= newFood.getOffAgeMax()) {
                float offness = newFood.getAge() - newFood.getOffAgeMax();
                if (offness == 0.0F) {
                    offness = 1.0F;
                }

                if (offness > 5.0F) {
                    offness = 5.0F;
                }

                int illnessChancex;
                if (newFood.getOffAgeMax() > newFood.getOffAge()) {
                    illnessChancex = (int)(offness / (newFood.getOffAgeMax() - newFood.getOffAge()) * 100.0F);
                } else {
                    illnessChancex = 100;
                }

                if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                    illnessChancex /= 2;
                }

                if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                    illnessChancex *= 2;
                }

                if (!this.isInfected()) {
                    if (Rand.Next(100) < illnessChancex) {
                        float poisonPowerx = 5.0F * Math.abs(newFood.getHungChange() * 10.0F) * percentage;
                        this.stats.add(CharacterStat.POISON, poisonPowerx);
                        if (this.parentChar instanceof IsoPlayer isoPlayer) {
                            String debugStr = String.format(
                                "Player %s just ate spoiled food %s with poison power %f", isoPlayer.getDisplayName(), newFood.getDisplayName(), poisonPowerx
                            );
                            DebugType.Objects.debugln(debugStr);
                            LoggerManager.getLogger("user").write(debugStr);
                        }
                    } else {
                        this.stats.add(CharacterStat.POISON, 2.0F * Math.abs(newFood.getHungChange() * 10.0F) * percentage);
                    }
                }
            }
        }
    }

    public void JustAteFood(Food newFood) {
        this.JustAteFood(newFood, 100.0F);
    }

    private float getHealthFromFoodTimeByHunger() {
        return 13000.0F;
    }

    public void JustReadSomething(Literature literature) {
        this.stats.add(CharacterStat.BOREDOM, literature.getBoredomChange());
        this.stats.add(CharacterStat.UNHAPPINESS, literature.getUnhappyChange());
    }

    public void JustTookPainMeds() {
        this.stats.remove(CharacterStat.PAIN, this.getPainReductionFromMeds());
    }

    public void UpdateWetness() {
        IsoGridSquare square = this.parentChar.getCurrentSquare();
        BaseVehicle vehicle = this.parentChar.getVehicle();
        boolean isOutside = square == null || !square.isInARoom() && !square.haveRoof;
        if (vehicle != null && vehicle.hasRoof(vehicle.getSeat(this.parentChar))) {
            isOutside = false;
        }

        ClothingWetness clothingWetness = this.parentChar.getClothingWetness();
        float wetnessIncrease = 0.0F;
        float wetnessDecrease = 0.0F;
        float windshieldMod = 0.0F;
        if (vehicle != null && ClimateManager.getInstance().isRaining()) {
            VehiclePart windshield = vehicle.getPartById("Windshield");
            if (windshield != null) {
                VehicleWindow window = windshield.getWindow();
                if (window != null && window.isDestroyed()) {
                    float val = ClimateManager.getInstance().getRainIntensity();
                    val *= val;
                    val *= vehicle.getCurrentSpeedKmHour() / 50.0F;
                    if (val < 0.1F) {
                        val = 0.0F;
                    }

                    if (val > 1.0F) {
                        val = 1.0F;
                    }

                    windshieldMod = val * 3.0F;
                    wetnessIncrease = val;
                }
            }
        }

        if (isOutside
            && (this.parentChar.isAsleep() || this.parentChar.isSitOnGround() || this.parentChar.isSittingOnFurniture() || this.parentChar.isResting())
            && this.parentChar.getBed() != null
            && this.parentChar.getBed().getSprite() != null
            && this.parentChar.getBed().isTent()) {
            isOutside = false;
        }

        if (isOutside && ClimateManager.getInstance().isRaining()) {
            float valx = ClimateManager.getInstance().getRainIntensity();
            if (valx < 0.1F) {
                valx = 0.0F;
            }

            wetnessIncrease = valx;
        } else if (!isOutside || !ClimateManager.getInstance().isRaining()) {
            float temperature = ClimateManager.getInstance().getAirTemperatureForCharacter(this.parentChar);
            float valx = 0.1F;
            if (temperature > 5.0F) {
                valx += (temperature - 5.0F) / 10.0F;
            }

            valx -= windshieldMod;
            if (valx < 0.0F) {
                valx = 0.0F;
            }

            wetnessDecrease = valx;
        }

        if (clothingWetness != null) {
            clothingWetness.updateWetness(wetnessIncrease, wetnessDecrease);
            if (GameServer.server) {
                this.parentChar.getClothingWetnessSync().update();
            }
        }

        float currentWetness = this.stats.get(CharacterStat.WETNESS);
        float averageWetness = 0.0F;
        if (!this.bodyParts.isEmpty()) {
            for (BodyPart bodyPart : this.bodyParts) {
                averageWetness += bodyPart.getWetness();
            }

            averageWetness /= this.bodyParts.size();
        }

        float mergeFactor = 0.1F;
        float targetWetness = averageWetness + (currentWetness - averageWetness) * 0.1F;
        if (!this.bodyParts.isEmpty()) {
            for (BodyPart bodyPart : this.bodyParts) {
                bodyPart.setWetness(targetWetness);
            }
        }

        this.stats.set(CharacterStat.WETNESS, targetWetness);
        float delta = 0.0F;
        if (this.thermoregulator != null) {
            delta = this.thermoregulator.getCatchAColdDelta();
        }

        if (!this.isHasACold() && delta > 0.1F) {
            if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                delta *= 1.7F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                delta *= 0.45F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.OUTDOORSMAN)) {
                delta *= 0.25F;
            }

            this.setCatchACold(this.getCatchACold() + (float)ZomboidGlobals.catchAColdIncreaseRate * delta * GameTime.instance.getMultiplier());
            if (this.getCatchACold() >= 100.0F) {
                this.setCatchACold(0.0F);
                this.setHasACold(true);
                this.setColdStrength(20.0F);
                this.setTimeToSneezeOrCough(0.0F);
            }
        }

        if (delta <= 0.1F) {
            this.setCatchACold(this.getCatchACold() - (float)ZomboidGlobals.catchAColdDecreaseRate);
            if (this.getCatchACold() <= 0.0F) {
                this.setCatchACold(0.0F);
            }
        }
    }

    public void TriggerSneezeCough() {
        if (this.getSneezeCoughActive() <= 0) {
            boolean smoker = this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) < 1 && this.parentChar.hasTrait(CharacterTrait.SMOKER);
            if (Rand.Next(100) > 50 && !smoker) {
                this.setSneezeCoughActive(1);
            } else {
                this.setSneezeCoughActive(2);
            }

            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 2) {
                this.setSneezeCoughActive(1);
            }

            this.setSneezeCoughTime(this.getSneezeCoughDelay());
            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 4) {
                this.setTimeToSneezeOrCough(
                    this.getNastyColdSneezeTimerMin() + Rand.Next(this.getNastyColdSneezeTimerMax() - this.getNastyColdSneezeTimerMin())
                );
            } else if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 3) {
                this.setTimeToSneezeOrCough(this.getColdSneezeTimerMin() + Rand.Next(this.getColdSneezeTimerMax() - this.getColdSneezeTimerMin()));
            } else if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) == 2) {
                this.setTimeToSneezeOrCough(this.getMildColdSneezeTimerMin() + Rand.Next(this.getMildColdSneezeTimerMax() - this.getMildColdSneezeTimerMin()));
            } else if (smoker) {
                this.setTimeToSneezeOrCough(this.getSmokerSneezeTimerMin() + Rand.Next(this.getSmokerSneezeTimerMax() - this.getSmokerSneezeTimerMin()));
            }

            boolean tissueConsumed = false;
            if (this.parentChar.getPrimaryHandItem() == null
                || !this.parentChar.getPrimaryHandItem().getType().equals("Tissue")
                    && !this.parentChar.getPrimaryHandItem().getType().equals("ToiletPaper")
                    && !this.parentChar.getPrimaryHandItem().hasTag(ItemTag.MUFFLE_SNEEZE)) {
                if (this.parentChar.getSecondaryHandItem() != null
                    && (
                        this.parentChar.getSecondaryHandItem().getType().equals("Tissue")
                            || this.parentChar.getSecondaryHandItem().getType().equals("ToiletPaper")
                            || this.parentChar.getSecondaryHandItem().hasTag(ItemTag.MUFFLE_SNEEZE)
                    )
                    && this.parentChar.getSecondaryHandItem().getCurrentUses() > 0) {
                    this.parentChar.getSecondaryHandItem().setCurrentUses(this.parentChar.getSecondaryHandItem().getCurrentUses() - 1);
                    if (this.parentChar.getSecondaryHandItem().getCurrentUses() <= 0) {
                        this.parentChar.getSecondaryHandItem().Use();
                    }

                    tissueConsumed = true;
                }
            } else if (this.parentChar.getPrimaryHandItem().getCurrentUses() > 0) {
                this.parentChar.getPrimaryHandItem().setCurrentUses(this.parentChar.getPrimaryHandItem().getCurrentUses() - 1);
                if (this.parentChar.getPrimaryHandItem().getCurrentUses() <= 0) {
                    this.parentChar.getPrimaryHandItem().Use();
                }

                tissueConsumed = true;
            }

            if (tissueConsumed) {
                this.setSneezeCoughActive(this.getSneezeCoughActive() + 2);
            } else {
                int dist = 20;
                int vol = 20;
                if (this.getSneezeCoughActive() == 1) {
                    dist = 20;
                    vol = 25;
                }

                if (this.getSneezeCoughActive() == 2) {
                    dist = 35;
                    vol = 40;
                }

                WorldSoundManager.WorldSound sneeze = WorldSoundManager.instance
                    .addSound(
                        this.parentChar,
                        PZMath.fastfloor(this.parentChar.getX()),
                        PZMath.fastfloor(this.parentChar.getY()),
                        PZMath.fastfloor(this.parentChar.getZ()),
                        dist,
                        vol,
                        false
                    );
                sneeze.stressAnimals = false;
            }

            if (GameServer.server && this.parentChar instanceof IsoPlayer player) {
                int sneezeVar = 0;
                if (this.IsSneezingCoughing() == 1 || this.IsSneezingCoughing() == 3) {
                    sneezeVar = Rand.Next(2) + 1;
                }

                GameServer.sendSneezingCoughing(player, this.IsSneezingCoughing(), (byte)sneezeVar);
            }
        }
    }

    public int IsSneezingCoughing() {
        return this.getSneezeCoughActive();
    }

    public void UpdateCold() {
        if (this.isHasACold()) {
            boolean recovering = true;
            IsoGridSquare sq = this.parentChar.getCurrentSquare();
            if (sq == null
                || !sq.isInARoom()
                || this.parentChar.getMoodles().getMoodleLevel(MoodleType.WET) > 0
                || this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) >= 1
                || this.stats.get(CharacterStat.FATIGUE) > 0.5F
                || this.stats.get(CharacterStat.HUNGER) > 0.25F
                || this.stats.get(CharacterStat.THIRST) > 0.25F) {
                recovering = false;
            }

            if (this.getColdReduction() > 0.0F) {
                recovering = true;
                this.setColdReduction(this.getColdReduction() - 0.005F * GameTime.instance.getMultiplier());
                if (this.getColdReduction() < 0.0F) {
                    this.setColdReduction(0.0F);
                }
            }

            if (recovering) {
                float delta = 1.0F;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    delta = 0.5F;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    delta = 1.5F;
                }

                this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * delta * GameTime.instance.getMultiplier());
                if (this.getColdReduction() > 0.0F) {
                    this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * delta * GameTime.instance.getMultiplier());
                }

                if (this.getColdStrength() < 0.0F) {
                    this.setColdStrength(0.0F);
                    this.setHasACold(false);
                    this.setCatchACold(0.0F);
                }
            } else {
                float deltax = 1.0F;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    deltax = 1.2F;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    deltax = 0.8F;
                }

                this.setColdStrength(this.getColdStrength() + this.getColdProgressionRate() * deltax * GameTime.instance.getMultiplier());
                if (this.getColdStrength() > 100.0F) {
                    this.setColdStrength(100.0F);
                }
            }

            if (this.getSneezeCoughTime() > 0) {
                this.setSneezeCoughTime(this.getSneezeCoughTime() - 1);
                if (this.getSneezeCoughTime() == 0) {
                    this.setSneezeCoughActive(0);
                }
            }

            if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HAS_A_COLD) > 1
                && this.getTimeToSneezeOrCough() >= 0.0F
                && !this.parentChar.IsSpeaking()) {
                this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - 1.0F);
                if (this.getTimeToSneezeOrCough() <= 0.0F) {
                    this.TriggerSneezeCough();
                }
            }
        } else if (this.parentChar.hasTrait(CharacterTrait.SMOKER)) {
            if (this.getSneezeCoughTime() > 0) {
                this.setSneezeCoughTime(this.getSneezeCoughTime() - 1);
                if (this.getSneezeCoughTime() == 0) {
                    this.setSneezeCoughActive(0);
                }
            }

            if (this.getTimeToSneezeOrCough() >= 0.0F) {
                if (!this.parentChar.IsSpeaking()) {
                    this.setTimeToSneezeOrCough(this.getTimeToSneezeOrCough() - GameTime.instance.getGameWorldSecondsSinceLastUpdate());
                    if (this.getTimeToSneezeOrCough() <= 0.0F) {
                        this.TriggerSneezeCough();
                    }
                }
            } else {
                this.setTimeToSneezeOrCough(this.getSmokerSneezeTimerMin() + Rand.Next(this.getSmokerSneezeTimerMax() - this.getSmokerSneezeTimerMin()));
            }
        }
    }

    public float getColdStrength() {
        return this.isHasACold() ? this.coldStrength : 0.0F;
    }

    public void AddDamage(BodyPartType bodyPart, float val) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).AddDamage(val);
    }

    public void AddGeneralHealth(float val) {
        int numDamagedParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).getHealth() < 100.0F) {
                numDamagedParts++;
            }
        }

        if (numDamagedParts > 0) {
            float healthPerPart = val / numDamagedParts;

            for (int ix = 0; ix < BodyPartType.ToIndex(BodyPartType.MAX); ix++) {
                if (this.getBodyParts().get(ix).getHealth() < 100.0F) {
                    this.getBodyParts().get(ix).AddHealth(healthPerPart);
                }
            }
        }
    }

    public void ReduceGeneralHealth(float val) {
        if (this.getOverallBodyHealth() <= 10.0F) {
            this.parentChar.forceAwake();
        }

        if (!(val <= 0.0F)) {
            float healthPerPart = val / BodyPartType.ToIndex(BodyPartType.MAX);

            for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
                this.getBodyParts().get(i).ReduceHealth(healthPerPart / BodyPartType.getDamageModifyer(i));
            }
        }
    }

    public void AddDamage(int bodyPartIndex, float val) {
        this.getBodyParts().get(bodyPartIndex).AddDamage(val);
    }

    public void splatBloodFloorBig() {
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
        this.parentChar.splatBloodFloorBig();
    }

    private static boolean isSpikedPart(IsoGameCharacter owner, IsoGameCharacter target, int partIndex) {
        boolean behind;
        if (!owner.isAimAtFloor()) {
            behind = owner.isBehind(target);
        } else {
            behind = target.isFallOnFront();
        }

        boolean spikedPart;
        if (behind) {
            spikedPart = target.bodyPartIsSpikedBehind(partIndex);
        } else {
            spikedPart = target.bodyPartIsSpiked(partIndex);
        }

        return spikedPart;
    }

    public static void damageFromSpikedArmor(IsoGameCharacter owner, IsoGameCharacter target, int partIndex, HandWeapon weapon) {
        boolean shove = owner instanceof IsoLivingCharacter isoLivingCharacter && isoLivingCharacter.isDoShove();
        if (owner != null && (shove || WeaponType.getWeaponType(weapon) == WeaponType.KNIFE)) {
            boolean spikedPart = isSpikedPart(owner, target, partIndex);
            boolean spikedFoot = spikedPart && owner.isAimAtFloor() && shove;
            boolean spikedPrimary = spikedPart && !spikedFoot && (owner.getPrimaryHandItem() == null || owner.getPrimaryHandItem() instanceof HandWeapon);
            boolean spikedSecondary = spikedPart
                && !spikedFoot
                && (owner.getSecondaryHandItem() == null || owner.getSecondaryHandItem() instanceof HandWeapon)
                && shove;
            if (spikedFoot) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Foot_R);
            }

            if (spikedPrimary) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Hand_R);
            }

            if (spikedSecondary) {
                target.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                owner.spikePart(BodyPartType.Hand_L);
            }
        }
    }

    public void applyDamageFromWeapon(int partIndex, float damage, int damageType, float pain) {
        BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
        switch (damageType) {
            case 1:
                part.generateDeepWound();
                break;
            case 2:
            case 4:
                part.setCut(true);
                break;
            case 3:
            case 5:
                part.setScratched(true, true);
                break;
            case 6:
                part.setHaveBullet(true, 0);
        }

        this.AddDamage(partIndex, damage);
        this.stats.add(CharacterStat.PAIN, pain);
        if (GameServer.server) {
            this.parentChar.getNetworkCharacterAI().syncDamage();
        }
    }

    public void DamageFromWeapon(HandWeapon weapon, int partIndex) {
        IsoPlayer player = Type.tryCastTo(this.parentChar, IsoPlayer.class);
        if (!GameClient.client || player == null || player.isLocalPlayer()) {
            int damageType = 0;
            boolean blunt = false;
            boolean blade = false;
            boolean bullet = false;
            if (weapon.isOfWeaponCategory(WeaponCategory.BLUNT) || weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                blunt = true;
            } else if (!weapon.isAimedFirearm()) {
                blade = true;
            } else {
                bullet = true;
            }

            if (partIndex == -1) {
                partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            }

            BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
            float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), blade, bullet);
            if (Rand.Next(100) < def) {
                IsoPlayer owner = weapon.getUsingPlayer();
                if (owner != null && WeaponType.getWeaponType(weapon) == WeaponType.KNIFE && !weapon.hasTag(ItemTag.HANDGUARD)) {
                    boolean spikedPart = isSpikedPart(owner, this.parentChar, partIndex);
                    if (spikedPart) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        owner.spikePart(BodyPartType.Hand_R);
                    }
                }

                this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), false);
                this.parentChar.playWeaponHitArmourSound(partIndex, bullet);
                if (player != null) {
                    player.syncVisuals();
                }
            } else {
                this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex));
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                float pain = 0.0F;
                if (blade) {
                    if (Rand.NextBool(6)) {
                        damageType = 1;
                        part.generateDeepWound();
                    } else if (Rand.NextBool(3)) {
                        damageType = 2;
                        part.setCut(true);
                    } else {
                        damageType = 3;
                        part.setScratched(true, true);
                    }

                    pain = this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex);
                } else if (blunt) {
                    if (Rand.NextBool(4)) {
                        damageType = 4;
                        part.setCut(true);
                    } else {
                        damageType = 5;
                        part.setScratched(true, true);
                    }

                    pain = this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex);
                } else if (bullet) {
                    damageType = 6;
                    part.setHaveBullet(true, 0);
                    pain = this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex);
                }

                float damage = Rand.Next(weapon.getMinDamage(), weapon.getMaxDamage()) * 15.0F;
                if (partIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
                    damage *= 4.0F;
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    damage *= 4.0F;
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
                    damage *= 2.0F;
                }

                if (GameServer.server) {
                    if (weapon.isRanged()) {
                        damage = (float)(damage * ServerOptions.getInstance().pvpFirearmDamageModifier.getValue());
                    } else {
                        damage = (float)(damage * ServerOptions.getInstance().pvpMeleeDamageModifier.getValue());
                    }
                }

                damageFromSpikedArmor(weapon.getUsingPlayer(), this.parentChar, partIndex, weapon);
                this.applyDamageFromWeapon(partIndex, damage, damageType, pain);
                this.parentChar.playWeaponHitArmourSound(partIndex, bullet);
                if (player != null) {
                    player.syncVisuals();
                }
            }
        }
    }

    public boolean AddRandomDamageFromZombie(IsoZombie zombie, String hitReaction, int partIndex) {
        if (StringUtils.isNullOrEmpty(hitReaction)) {
            hitReaction = "Bite";
        }

        this.parentChar.setVariable("hitpvp", false);
        int painType = 0;
        int baseChance = 15 + this.parentChar.getMeleeCombatMod();
        int baseBiteChance = 85;
        int baseLacerationChance = 65;
        String dotSide = this.parentChar.testDotSide(zombie);
        boolean isBehind = dotSide.equals("BEHIND");
        boolean isLeftOrRight = dotSide.equals("LEFT") || dotSide.equals("RIGHT");
        int zombiesAttacking = this.parentChar.getSurroundingAttackingZombies();
        zombiesAttacking = Math.max(zombiesAttacking, 1);
        baseChance -= (zombiesAttacking - 1) * 10;
        baseBiteChance -= (zombiesAttacking - 1) * 30;
        baseLacerationChance -= (zombiesAttacking - 1) * 15;
        int neededZedToDragDown = 3;
        if (SandboxOptions.instance.lore.strength.getValue() == 1) {
            neededZedToDragDown = 2;
        }

        if (SandboxOptions.instance.lore.strength.getValue() == 3) {
            neededZedToDragDown = 6;
        }

        if (this.parentChar.hasTrait(CharacterTrait.THICK_SKINNED)) {
            baseChance = (int)(baseChance * 1.3);
        }

        if (this.parentChar.hasTrait(CharacterTrait.THIN_SKINNED)) {
            baseChance = (int)(baseChance / 1.3);
        }

        int dragDownZeds = this.parentChar.getSurroundingAttackingZombies(SandboxOptions.instance.lore.zombiesCrawlersDragDown.getValue());
        if (!"EndDeath".equals(this.parentChar.getHitReaction())) {
            if (!this.parentChar.isGodMod()
                && dragDownZeds >= neededZedToDragDown
                && SandboxOptions.instance.lore.zombiesDragDown.getValue()
                && !this.parentChar.isSitOnGround()) {
                baseBiteChance = 0;
                baseLacerationChance = 0;
                baseChance = 0;
                this.parentChar.setHitReaction("EndDeath");
                this.parentChar.setDeathDragDown(true);
            } else {
                this.parentChar.setHitReaction(hitReaction);
            }
        }

        if (isBehind) {
            baseChance -= 15;
            baseBiteChance -= 25;
            baseLacerationChance -= 35;
            if (SandboxOptions.instance.rearVulnerability.getValue() == 1) {
                baseChance += 15;
                baseBiteChance += 25;
                baseLacerationChance += 35;
            }

            if (SandboxOptions.instance.rearVulnerability.getValue() == 2) {
                baseChance += 7;
                baseBiteChance += 17;
                baseLacerationChance += 23;
            }

            if (zombiesAttacking > 2) {
                baseBiteChance -= 15;
                baseLacerationChance -= 15;
            }
        }

        if (isLeftOrRight) {
            baseChance -= 30;
            baseBiteChance -= 7;
            baseLacerationChance -= 27;
            if (SandboxOptions.instance.rearVulnerability.getValue() == 1) {
                baseChance += 30;
                baseBiteChance += 7;
                baseLacerationChance += 27;
            }

            if (SandboxOptions.instance.rearVulnerability.getValue() == 2) {
                baseChance += 15;
                baseBiteChance += 4;
                baseLacerationChance += 15;
            }
        }

        if (partIndex < 0) {
            if (!zombie.crawling) {
                if (Rand.Next(10) == 0) {
                    partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Groin) + 1);
                } else {
                    partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
                }

                float chanceToGetNeck = 10.0F * zombiesAttacking;
                if (isBehind) {
                    chanceToGetNeck += 5.0F;
                }

                if (isLeftOrRight) {
                    chanceToGetNeck += 2.0F;
                }

                if (isBehind && Rand.Next(100) < chanceToGetNeck) {
                    partIndex = BodyPartType.ToIndex(BodyPartType.Neck);
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Head) || partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    int percent = 70;
                    if (isBehind) {
                        percent = 90;
                    }

                    if (isLeftOrRight) {
                        percent = 80;
                    }

                    if (Rand.Next(100) > percent) {
                        boolean done = false;

                        while (!done) {
                            done = true;
                            partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1);
                            if (partIndex == BodyPartType.ToIndex(BodyPartType.Head)
                                || partIndex == BodyPartType.ToIndex(BodyPartType.Neck)
                                || partIndex == BodyPartType.ToIndex(BodyPartType.Groin)) {
                                done = false;
                            }
                        }
                    }
                }
            } else {
                if (Rand.Next(2) != 0) {
                    return false;
                }

                if (Rand.Next(10) == 0) {
                    partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.MAX));
                } else {
                    partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.MAX));
                }
            }
        }

        if (zombie.inactive) {
            baseChance += 20;
            baseBiteChance += 20;
            baseLacerationChance += 20;
        }

        float damage = Rand.Next(1000) / 1000.0F;
        damage *= Rand.Next(10) + 10;
        if (GameServer.server && this.parentChar instanceof IsoPlayer || Core.debug && this.parentChar instanceof IsoPlayer) {
            DebugType.DetailedInfo
                .trace(
                    "zombie did "
                        + damage
                        + " dmg to "
                        + ((IsoPlayer)this.parentChar).getDisplayName()
                        + " on body part "
                        + BodyPartType.getDisplayName(BodyPartType.FromIndex(partIndex))
                );
        }

        boolean holeDone = false;
        boolean scratchOrBite = true;
        boolean behind = isBehind || this.parentChar.isFallOnFront();
        IsoPlayer isoPlayer = Type.tryCastTo(this.parentChar, IsoPlayer.class);
        if (Rand.Next(100) > baseChance) {
            boolean spikedPart;
            if (behind) {
                spikedPart = this.parentChar.bodyPartIsSpikedBehind(partIndex);
            } else {
                spikedPart = this.parentChar.bodyPartIsSpiked(partIndex);
            }

            zombie.scratch = true;
            this.parentChar.helmetFall(partIndex == BodyPartType.ToIndex(BodyPartType.Neck) || partIndex == BodyPartType.ToIndex(BodyPartType.Head));
            if (Rand.Next(100) > baseLacerationChance) {
                zombie.scratch = false;
                zombie.laceration = true;
            }

            if (Rand.Next(100) > baseBiteChance && !zombie.cantBite()) {
                zombie.scratch = false;
                zombie.laceration = false;
                scratchOrBite = false;
            }

            if (zombie.scratch) {
                float defense = this.parentChar.getBodyPartClothingDefense(partIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackScratch);
                if (this.getHealth() > 0.0F) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }

                if (this.getHealth() > 0.0F && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }

                if (Rand.Next(100) < defense) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    return false;
                }

                boolean addedHole = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHole) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                painType = 1;
                if (isoPlayer != null) {
                    DebugType.DetailedInfo.trace("zombie scratched %s in body location %s", isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex));
                    isoPlayer.playerVoiceSound("PainFromScratch");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction, partIndex);
                        return true;
                    }
                }

                this.AddDamage(partIndex, damage);
                this.SetScratched(partIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, true);
            } else if (zombie.laceration) {
                float defensex = this.parentChar.getBodyPartClothingDefense(partIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackLacerate);
                if (this.getHealth() > 0.0F) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }

                if (this.getHealth() > 0.0F && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }

                if (Rand.Next(100) < defensex) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    return false;
                }

                boolean addedHolex = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHolex) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                painType = 1;
                if (isoPlayer != null) {
                    DebugType.DetailedInfo.trace("zombie laceration %s in body location %s", isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex));
                    isoPlayer.playerVoiceSound("PainFromLacerate");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction, partIndex);
                        return true;
                    }
                }

                this.AddDamage(partIndex, damage);
                this.SetCut(partIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, true);
            } else {
                float defensexx = this.parentChar.getBodyPartClothingDefense(partIndex, true, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackBite);
                if (this.getHealth() > 0.0F) {
                    String soundName = zombie.getBiteSoundName();
                    if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                        soundName = "NeckBite";
                    }

                    this.parentChar.getEmitter().playSoundImpl(soundName, null);
                }

                if (Rand.Next(100) < defensexx) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
                    if (spikedPart) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, false);
                        zombie.spikePart(BodyPartType.Head);
                    }

                    return false;
                }

                boolean addedHolexx = this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex), true);
                if (addedHolexx) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                painType = 2;
                if (isoPlayer != null) {
                    DebugType.DetailedInfo.trace("zombie bite %s in body location %s", isoPlayer.getUsername(), BloodBodyPartType.FromIndex(partIndex));
                    isoPlayer.playerVoiceSound("PainFromBite");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer, true, hitReaction, partIndex);
                        return true;
                    }
                }

                this.AddDamage(partIndex, damage);
                this.SetBitten(partIndex, true);
                if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.Torso_Upper, false, true, false);
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                }

                this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, true);
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), false, true, false);
                    zombie.spikePart(BodyPartType.Head);
                    zombie.Kill(null);
                }
            }
        }

        if (!holeDone) {
            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), scratchOrBite);
        }

        switch (painType) {
            case 0:
                this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            case 1:
                this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex));
                break;
            case 2:
                this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex));
        }

        if (GameServer.server && isoPlayer != null) {
            isoPlayer.getNetworkCharacterAI().syncDamage();
            isoPlayer.syncVisuals();
        }

        return true;
    }

    public boolean doesBodyPartHaveInjury(BodyPartType part) {
        return this.getBodyParts().get(BodyPartType.ToIndex(part)).HasInjury();
    }

    /**
     * Returns TRUE if either body part is injured. ie. A OR B
     */
    public boolean doBodyPartsHaveInjuries(BodyPartType partA, BodyPartType partB) {
        return this.doesBodyPartHaveInjury(partA) || this.doesBodyPartHaveInjury(partB);
    }

    /**
     * Returns TRUE if the specified body part's bleeding time is greater than 0.
     */
    public boolean isBodyPartBleeding(BodyPartType part) {
        return this.getBodyPart(part).getBleedingTime() > 0.0F;
    }

    /**
     * Returns TRUE if either body part is bleeding. ie. A OR B
     */
    public boolean areBodyPartsBleeding(BodyPartType partA, BodyPartType partB) {
        return this.isBodyPartBleeding(partA) || this.isBodyPartBleeding(partB);
    }

    public void DrawUntexturedQuad(int x, int y, int width, int height, float r, float g, float b, float a) {
        SpriteRenderer.instance.renderi(null, x, y, width, height, r, g, b, a, null);
    }

    public float getBodyPartHealth(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).getHealth();
    }

    public float getBodyPartHealth(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).getHealth();
    }

    public String getBodyPartName(BodyPartType bodyPart) {
        return BodyPartType.ToString(bodyPart);
    }

    public String getBodyPartName(int bodyPartIndex) {
        return BodyPartType.ToString(BodyPartType.FromIndex(bodyPartIndex));
    }

    public float getHealth() {
        return this.getOverallBodyHealth();
    }

    public float getApparentInfectionLevel() {
        float infectionLevel = Math.max(this.stats.get(CharacterStat.ZOMBIE_FEVER), this.stats.get(CharacterStat.ZOMBIE_INFECTION));
        return Math.max(this.stats.get(CharacterStat.FOOD_SICKNESS), infectionLevel);
    }

    public int getNumPartsBleeding() {
        int bleedingParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).bleeding()) {
                bleedingParts++;
            }
        }

        return bleedingParts;
    }

    public boolean isNeckBleeding() {
        return this.getBodyPart(BodyPartType.Neck).bleeding();
    }

    public int getNumPartsScratched() {
        int scratchedParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).scratched()) {
                scratchedParts++;
            }
        }

        return scratchedParts;
    }

    public int getNumPartsBitten() {
        int bittenParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).bitten()) {
                bittenParts++;
            }
        }

        return bittenParts;
    }

    public boolean HasInjury() {
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).HasInjury()) {
                return true;
            }
        }

        return false;
    }

    public boolean IsBandaged(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bandaged();
    }

    public boolean IsDeepWounded(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).deepWounded();
    }

    public boolean IsBandaged(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bandaged();
    }

    public boolean IsBitten(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bitten();
    }

    public boolean IsBitten(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bitten();
    }

    public boolean IsBleeding(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).bleeding();
    }

    public boolean IsBleeding(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).bleeding();
    }

    public boolean IsBleedingStemmed(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsBleedingStemmed();
    }

    public boolean IsBleedingStemmed(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsBleedingStemmed();
    }

    public boolean IsCauterized(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsCauterized();
    }

    public boolean IsCauterized(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsCauterized();
    }

    public boolean IsInfected() {
        return this.isInfected;
    }

    public boolean IsInfected(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).IsInfected();
    }

    public boolean IsInfected(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsInfected();
    }

    public boolean IsFakeInfected(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).IsFakeInfected();
    }

    public void DisableFakeInfection(int bodyPartIndex) {
        this.getBodyParts().get(bodyPartIndex).DisableFakeInfection();
    }

    public boolean IsScratched(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).scratched();
    }

    public boolean IsCut(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).getCutTime() > 0.0F;
    }

    public boolean IsScratched(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).scratched();
    }

    public boolean IsStitched(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).stitched();
    }

    public boolean IsStitched(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).stitched();
    }

    public boolean IsWounded(BodyPartType bodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).deepWounded();
    }

    public boolean IsWounded(int bodyPartIndex) {
        return this.getBodyParts().get(bodyPartIndex).deepWounded();
    }

    public void RestoreToFullHealth() {
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            this.getBodyParts().get(i).RestoreToFullHealth();
        }

        if (this.parentChar != null && this.parentChar.getStats() != null) {
            this.stats.resetStats();
        }

        if (this.parentChar != null) {
            this.parentChar.setCorpseSicknessRate(0.0F);
        }

        this.setInfected(false);
        this.setIsFakeInfected(false);
        this.setOverallBodyHealth(100.0F);
        this.setCatchACold(0.0F);
        this.setHasACold(false);
        this.setColdStrength(0.0F);
        this.setSneezeCoughActive(0);
        this.setSneezeCoughTime(0);
        this.setInfectionTime(-1.0F);
        this.setInfectionMortalityDuration(-1.0F);
        if (this.thermoregulator != null) {
            this.thermoregulator.reset();
        }

        MusicIntensityConfig.getInstance().restoreToFullHealth(this.parentChar);
    }

    public void SetBandaged(int bodyPartIndex, boolean bandaged, float bandageLife, boolean isAlcoholic, String bandageType) {
        this.getBodyParts().get(bodyPartIndex).setBandaged(bandaged, bandageLife, isAlcoholic, bandageType);
    }

    public void SetBitten(BodyPartType bodyPart, boolean bitten) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetBitten(bitten);
    }

    public void SetBitten(int bodyPartIndex, boolean bitten) {
        this.getBodyParts().get(bodyPartIndex).SetBitten(bitten);
    }

    public void SetBitten(int bodyPartIndex, boolean bitten, boolean infected) {
        this.getBodyParts().get(bodyPartIndex).SetBitten(bitten, infected);
    }

    public void SetBleeding(BodyPartType bodyPart, boolean bleeding) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setBleeding(bleeding);
    }

    public void SetBleeding(int bodyPartIndex, boolean bleeding) {
        this.getBodyParts().get(bodyPartIndex).setBleeding(bleeding);
    }

    public void SetBleedingStemmed(BodyPartType bodyPart, boolean bleedingStemmed) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetBleedingStemmed(bleedingStemmed);
    }

    public void SetBleedingStemmed(int bodyPartIndex, boolean bleedingStemmed) {
        this.getBodyParts().get(bodyPartIndex).SetBleedingStemmed(bleedingStemmed);
    }

    public void SetCauterized(BodyPartType bodyPart, boolean cauterized) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).SetCauterized(cauterized);
    }

    public void SetCauterized(int bodyPartIndex, boolean cauterized) {
        this.getBodyParts().get(bodyPartIndex).SetCauterized(cauterized);
    }

    public BodyPart setScratchedWindow() {
        if (GameClient.client) {
            return null;
        } else {
            int bodyPart = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.ForeArm_R) + 1);
            this.getBodyPart(BodyPartType.FromIndex(bodyPart)).AddDamage(10.0F);
            this.getBodyPart(BodyPartType.FromIndex(bodyPart)).SetScratchedWindow(true);
            return this.getBodyPart(BodyPartType.FromIndex(bodyPart));
        }
    }

    public void SetScratched(BodyPartType bodyPart, boolean scratched) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setScratched(scratched, false);
    }

    public void SetScratched(int bodyPartIndex, boolean scratched) {
        this.getBodyParts().get(bodyPartIndex).setScratched(scratched, false);
    }

    public void SetScratchedFromWeapon(int bodyPartIndex, boolean scratched) {
        this.getBodyParts().get(bodyPartIndex).SetScratchedWeapon(scratched);
    }

    public void SetCut(int bodyPartIndex, boolean cut) {
        this.getBodyParts().get(bodyPartIndex).setCut(cut, false);
    }

    public void SetWounded(BodyPartType bodyPart, boolean wounded) {
        this.getBodyParts().get(BodyPartType.ToIndex(bodyPart)).setDeepWounded(wounded);
    }

    public void SetWounded(int bodyPartIndex, boolean wounded) {
        this.getBodyParts().get(bodyPartIndex).setDeepWounded(wounded);
    }

    public void ShowDebugInfo() {
        if (this.getDamageModCount() > 0) {
            this.setDamageModCount(this.getDamageModCount() - 1);
        }
    }

    public void UpdateBoredom() {
        if (!(this.parentChar instanceof IsoSurvivor)) {
            if (!(this.parentChar instanceof IsoPlayer) || !this.parentChar.asleep) {
                if (!this.parentChar.getCurrentSquare().isInARoom() && !(this.parentChar.getIdleSquareTime() >= 1800.0F)) {
                    if (this.parentChar.getVehicle() != null) {
                        float speed = this.parentChar.getVehicle().getCurrentSpeedKmHour();
                        if (Math.abs(speed) <= 0.1F) {
                            if (this.parentChar.isReading()) {
                                this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate / 5.0 * GameTime.instance.getMultiplier()));
                            } else {
                                this.stats.add(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomIncreaseRate * GameTime.instance.getMultiplier()));
                            }
                        } else {
                            this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.5 * GameTime.instance.getMultiplier()));
                        }
                    } else {
                        this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.1F * GameTime.instance.getMultiplier()));
                    }
                } else {
                    if (this.parentChar.isCurrentlyIdle()) {
                        this.stats
                            .add(
                                CharacterStat.BOREDOM,
                                (float)(ZomboidGlobals.boredomIncreaseRate * this.stats.get(CharacterStat.IDLENESS) * GameTime.instance.getMultiplier())
                            );
                    } else {
                        this.stats
                            .add(
                                CharacterStat.BOREDOM,
                                (float)(ZomboidGlobals.boredomIncreaseRate / 10.0 * this.stats.get(CharacterStat.IDLENESS) * GameTime.instance.getMultiplier())
                            );
                    }

                    if (this.parentChar.IsSpeaking() && !this.parentChar.callOut) {
                        this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * GameTime.instance.getMultiplier()));
                    }

                    if (this.parentChar.getNumSurvivorsInVicinity() > 0) {
                        this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.1F * GameTime.instance.getMultiplier()));
                    }

                    if (this.parentChar.isCurrentlyBusy() && this.stats.get(CharacterStat.IDLENESS) < 0.1F) {
                        this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 0.5 * GameTime.instance.getMultiplier()));
                    }
                }

                if (this.stats.get(CharacterStat.INTOXICATION) > 20.0F) {
                    this.stats.remove(CharacterStat.BOREDOM, (float)(ZomboidGlobals.boredomDecreaseRate * 2.0 * GameTime.instance.getMultiplier()));
                }

                if (this.stats.get(CharacterStat.PANIC) > 5.0F) {
                    this.stats.reset(CharacterStat.BOREDOM);
                }

                if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BORED) > 1 && !this.parentChar.isReading()) {
                    this.stats
                        .add(
                            CharacterStat.UNHAPPINESS,
                            (float)(
                                ZomboidGlobals.unhappinessIncrease
                                    * this.parentChar.getMoodles().getMoodleLevel(MoodleType.BORED)
                                    * GameTime.instance.getMultiplier()
                            )
                        );
                }

                if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.STRESS) > 1 && !this.parentChar.isReading()) {
                    this.stats
                        .add(
                            CharacterStat.UNHAPPINESS,
                            (float)(
                                ZomboidGlobals.unhappinessIncrease
                                    / 2.0
                                    * this.parentChar.getMoodles().getMoodleLevel(MoodleType.STRESS)
                                    * GameTime.instance.getMultiplier()
                            )
                        );
                }

                if (this.parentChar.hasTrait(CharacterTrait.SMOKER)) {
                    this.parentChar.setTimeSinceLastSmoke(this.parentChar.getTimeSinceLastSmoke() + 1.0E-4F * GameTime.instance.getMultiplier());
                    if (this.parentChar.getTimeSinceLastSmoke() > 1.0F) {
                        double lastTimeSmoke = PZMath.fastfloor(this.parentChar.getTimeSinceLastSmoke() / 10.0F) + 1.0F;
                        if (lastTimeSmoke > 10.0) {
                            lastTimeSmoke = 10.0;
                        }

                        this.stats
                            .add(
                                CharacterStat.NICOTINE_WITHDRAWAL,
                                (float)(ZomboidGlobals.stressFromBiteOrScratch / 8.0 * lastTimeSmoke * GameTime.instance.getMultiplier())
                            );
                    }
                }
            }
        }
    }

    public void UpdateStrength() {
        int numStrengthReducers = 0;
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
            numStrengthReducers += 3;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 2) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 3) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 2) {
            numStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 3) {
            numStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 4) {
            numStrengthReducers += 3;
        }

        this.parentChar.setMaxWeight((int)(this.parentChar.getMaxWeightBase() * this.parentChar.getWeightMod()) - numStrengthReducers);
        if (this.parentChar.getMaxWeight() < 0) {
            this.parentChar.setMaxWeight(0);
        }

        if (this.parentChar instanceof IsoPlayer isoPlayer) {
            this.parentChar.setMaxWeight((int)(this.parentChar.getMaxWeight() * isoPlayer.getMaxWeightDelta()));
        }
    }

    public float pickMortalityDuration() {
        float del = 1.0F;
        if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
            del = 1.25F;
        }

        if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
            del = 0.75F;
        }
        return switch (SandboxOptions.instance.lore.mortality.getValue()) {
            case 1 -> 0.0F;
            case 2 -> Rand.Next(0.0F, 30.0F) / 3600.0F * del;
            case 3 -> Rand.Next(0.5F, 1.0F) / 60.0F * del;
            case 4 -> Rand.Next(3.0F, 12.0F) * del;
            case 5 -> Rand.Next(2.0F, 3.0F) * 24.0F * del;
            case 6 -> Rand.Next(1.0F, 2.0F) * 7.0F * 24.0F * del;
            case 7 -> -1.0F;
            default -> -1.0F;
        };
    }

    public void Update() {
        if (!(this.parentChar instanceof IsoZombie) && !this.parentChar.isAnimal()) {
            if (GameClient.client) {
                IsoPlayer player = Type.tryCastTo(this.parentChar, IsoPlayer.class);
                if (player != null && player.isAlive()) {
                    if (!player.isLocalPlayer()) {
                        this.RestoreToFullHealth();
                    }

                    return;
                }
            }

            if (this.parentChar.isGodMod()) {
                this.RestoreToFullHealth();
                ((IsoPlayer)this.parentChar).bleedingLevel = 0;
            } else {
                float lastPain = this.stats.get(CharacterStat.PAIN);
                int n = this.getNumPartsBleeding() * 2;
                n += this.getNumPartsScratched();
                n += this.getNumPartsBitten() * 6;
                if (this.getHealth() >= 60.0F && n <= 3) {
                    n = 0;
                }

                ((IsoPlayer)this.parentChar).bleedingLevel = (byte)n;
                if (n > 0) {
                    float bleedChance = 1.0F / n * 200.0F * GameTime.instance.getInvMultiplier();
                    if (Rand.Next((int)bleedChance) < bleedChance * 0.3F) {
                        this.parentChar.splatBloodFloor();
                    }

                    if (Rand.Next((int)bleedChance) == 0) {
                        this.parentChar.splatBloodFloor();
                    }
                }

                if (this.thermoregulator != null) {
                    this.thermoregulator.update();
                }

                this.UpdateDraggingCorpse();
                this.UpdateWetness();
                this.UpdateCold();
                this.UpdateBoredom();
                this.UpdateStrength();
                this.UpdatePanicState();
                this.UpdateTemperatureState();
                this.UpdateDiscomfort();
                this.UpdateIllness();
                if (this.getOverallBodyHealth() != 0.0F) {
                    if (!this.isInfected()) {
                        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
                            if (this.IsInfected(i)) {
                                this.setInfected(true);
                                if (this.IsFakeInfected(i)) {
                                    this.DisableFakeInfection(i);
                                    this.stats.set(CharacterStat.ZOMBIE_INFECTION, this.stats.get(CharacterStat.ZOMBIE_FEVER));
                                    this.stats.reset(CharacterStat.ZOMBIE_FEVER);
                                    this.setIsFakeInfected(false);
                                    this.setReduceFakeInfection(false);
                                }
                            }
                        }

                        if (this.isInfected() && this.getInfectionTime() < 0.0F && SandboxOptions.instance.lore.mortality.getValue() != 7) {
                            this.setInfectionTime(this.getCurrentTimeForInfection());
                            this.setInfectionMortalityDuration(this.pickMortalityDuration());
                        }
                    }

                    if (!this.isInfected() && !this.isIsFakeInfected()) {
                        for (int ix = 0; ix < BodyPartType.ToIndex(BodyPartType.MAX); ix++) {
                            if (this.IsFakeInfected(ix)) {
                                this.setIsFakeInfected(true);
                                break;
                            }
                        }
                    }

                    if (this.isIsFakeInfected() && !this.isReduceFakeInfection() && this.parentChar.getReduceInfectionPower() == 0.0F) {
                        this.stats.add(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
                        if (this.stats.isAtMaximum(CharacterStat.ZOMBIE_FEVER)) {
                            this.setReduceFakeInfection(true);
                        }
                    }

                    this.stats.remove(CharacterStat.INTOXICATION, this.getDrunkReductionValue() * GameTime.instance.getMultiplier());
                    float healthToAdd = 0.0F;
                    if (this.getHealthFromFoodTimer() > 0.0F) {
                        healthToAdd += this.getHealthFromFood() * GameTime.instance.getMultiplier();
                        this.setHealthFromFoodTimer(this.getHealthFromFoodTimer() - 1.0F * GameTime.instance.getMultiplier());
                    }

                    int reduced = 0;
                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
                        reduced = 1;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
                        reduced = 2;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                        reduced = 3;
                    }

                    if (this.parentChar.isAsleep()) {
                        reduced = -1;
                    }

                    switch (reduced) {
                        case 0:
                            healthToAdd += this.getStandardHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 1:
                            healthToAdd += this.getReducedHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 2:
                            healthToAdd += this.getSeverlyReducedHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 3:
                            healthToAdd += 0.0F;
                    }

                    if (this.parentChar.isAsleep()) {
                        if (GameClient.client) {
                            healthToAdd += 15.0F * GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 3600.0F;
                        } else {
                            healthToAdd += this.getSleepingHealthAddition() * GameTime.instance.getMultiplier();
                        }

                        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4
                            || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                            healthToAdd = 0.0F;
                        }
                    }

                    this.AddGeneralHealth(healthToAdd);
                    healthToAdd = 0.0F;
                    float poisonDamage = 0.0F;
                    float hungryDamage = 0.0F;
                    float sickDamage = 0.0F;
                    float bleedingDamage = 0.0F;
                    float thirstDamage = 0.0F;
                    float heavyLoadDamage = 0.0F;
                    float poison = this.stats.get(CharacterStat.POISON);
                    if (poison > 0.0F) {
                        if (poison > 10.0F && this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) >= 1) {
                            poisonDamage = 0.0035F * Math.min(poison / 10.0F, 3.0F) * GameTime.instance.getMultiplier();
                            healthToAdd += poisonDamage;
                        }

                        float decreaseWithWellFed = 0.0F;
                        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN) > 0) {
                            decreaseWithWellFed = 1.5E-4F * this.parentChar.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN);
                        }

                        this.stats
                            .remove(CharacterStat.POISON, (float)(decreaseWithWellFed + ZomboidGlobals.poisonLevelDecrease * GameTime.instance.getMultiplier()));
                        this.stats
                            .add(
                                CharacterStat.FOOD_SICKNESS,
                                this.getInfectionGrowthRate()
                                    * (2.0F + Math.round(this.stats.get(CharacterStat.POISON) / 10.0F))
                                    * GameTime.instance.getMultiplier()
                            );
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4) {
                        hungryDamage = this.getHealthReductionFromSevereBadMoodles() / 50.0F * GameTime.instance.getMultiplier();
                        healthToAdd += hungryDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
                        if (this.stats.get(CharacterStat.FOOD_SICKNESS) > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                            sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                            healthToAdd += sickDamage;
                        } else if (SandboxOptions.instance.woundInfectionFactor.getValue() > 0.0
                            && this.getGeneralWoundInfectionLevel() > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                            sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                            healthToAdd += sickDamage;
                        }
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
                        bleedingDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                        healthToAdd += bleedingDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                        thirstDamage = this.getHealthReductionFromSevereBadMoodles() / 10.0F * GameTime.instance.getMultiplier();
                        healthToAdd += thirstDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) > 2
                        && this.parentChar.getVehicle() == null
                        && !this.parentChar.isAsleep()
                        && !this.parentChar.isSitOnGround()
                        && !this.parentChar.isSittingOnFurniture()
                        && this.getThermoregulator().getMetabolicTarget() != Metabolics.SeatedResting.getMet()
                        && (!GameServer.server || !GameServer.fastForward)
                        && this.getHealth() > 75.0F
                        && Rand.PerThirtiethOfASecond(10)) {
                        heavyLoadDamage = this.getHealthReductionFromSevereBadMoodles()
                            / ((5 - this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD)) / 10.0F)
                            * GameTime.instance.getMultiplier();
                        healthToAdd += heavyLoadDamage;
                        this.parentChar.addBackMuscleStrain(heavyLoadDamage / 2.0F);
                    }

                    this.ReduceGeneralHealth(healthToAdd);
                    if (poisonDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "POISON", poisonDamage);
                    }

                    if (hungryDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "HUNGRY", hungryDamage);
                    }

                    if (sickDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "SICK", sickDamage);
                    }

                    if (bleedingDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "BLEEDING", bleedingDamage);
                    }

                    if (thirstDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "THIRST", thirstDamage);
                    }

                    if (heavyLoadDamage > 0.0F) {
                        LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "HEAVYLOAD", heavyLoadDamage);
                    }

                    if (this.parentChar.getPainEffect() > 0.0F) {
                        this.stats.remove(CharacterStat.PAIN, 0.023333333F * GameTime.getInstance().getThirtyFPSMultiplier());
                        this.parentChar.setPainEffect(this.parentChar.getPainEffect() - GameTime.getInstance().getThirtyFPSMultiplier());
                    } else {
                        this.parentChar.setPainDelta(0.0F);
                        healthToAdd = 0.0F;

                        for (int ixx = 0; ixx < BodyPartType.ToIndex(BodyPartType.MAX); ixx++) {
                            healthToAdd += this.getBodyParts().get(ixx).getPain() * BodyPartType.getPainModifyer(ixx);
                        }

                        healthToAdd -= this.getPainReduction();
                        if (healthToAdd > this.stats.get(CharacterStat.PAIN)) {
                            this.stats.add(CharacterStat.PAIN, (healthToAdd - this.stats.get(CharacterStat.PAIN)) / 500.0F);
                        } else {
                            this.stats.set(CharacterStat.PAIN, healthToAdd);
                        }
                    }

                    this.setPainReduction(this.getPainReduction() - 0.005F * GameTime.getInstance().getMultiplier());
                    if (this.getPainReduction() < 0.0F) {
                        this.setPainReduction(0.0F);
                    }

                    if (this.isInfected()) {
                        int mortality = SandboxOptions.instance.lore.mortality.getValue();
                        if (mortality == 1) {
                            this.ReduceGeneralHealth(110.0F);
                            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", 110);
                            this.stats.set(CharacterStat.ZOMBIE_INFECTION, CharacterStat.ZOMBIE_INFECTION.getMaximumValue());
                        } else if (mortality != 7) {
                            float worldAgeHours = this.getCurrentTimeForInfection();
                            if (this.infectionMortalityDuration < 0.0F) {
                                this.infectionMortalityDuration = this.pickMortalityDuration();
                            }

                            this.infectionTime = GameTime.checkHours(this.infectionTime, worldAgeHours);
                            hungryDamage = Math.min((worldAgeHours - this.infectionTime) / this.infectionMortalityDuration, 1.0F);
                            this.stats.set(CharacterStat.ZOMBIE_INFECTION, hungryDamage * 100.0F);
                            if (hungryDamage == 1.0F) {
                                this.ReduceGeneralHealth(110.0F);
                                LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", 110);
                            } else {
                                hungryDamage *= hungryDamage;
                                hungryDamage *= hungryDamage;
                                sickDamage = (1.0F - hungryDamage) * 100.0F;
                                bleedingDamage = this.getOverallBodyHealth() - sickDamage;
                                if (bleedingDamage > 0.0F && sickDamage <= 99.0F) {
                                    this.ReduceGeneralHealth(bleedingDamage);
                                    LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", bleedingDamage);
                                }
                            }
                        }
                    }

                    for (int ixx = 0; ixx < BodyPartType.ToIndex(BodyPartType.MAX); ixx++) {
                        this.getBodyParts().get(ixx).DamageUpdate();
                    }

                    this.calculateOverallHealth();
                    if (this.getOverallBodyHealth() <= 0.0F) {
                        if (this.isIsOnFire()) {
                            this.setBurntToDeath(true);

                            for (int ixx = 0; ixx < BodyPartType.ToIndex(BodyPartType.MAX); ixx++) {
                                this.getBodyParts().get(ixx).SetHealth(Rand.Next(90));
                            }
                        } else {
                            this.setBurntToDeath(false);
                        }
                    }

                    if (this.isReduceFakeInfection() && this.getOverallBodyHealth() > 0.0F) {
                        this.stats.remove(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier() * 2.0F);
                    }

                    if (this.parentChar.getReduceInfectionPower() > 0.0F && this.getOverallBodyHealth() > 0.0F) {
                        this.stats.remove(CharacterStat.ZOMBIE_FEVER, this.getInfectionGrowthRate() * GameTime.instance.getMultiplier());
                        this.parentChar
                            .setReduceInfectionPower(
                                this.parentChar.getReduceInfectionPower() - this.getInfectionGrowthRate() * GameTime.instance.getMultiplier()
                            );
                        if (this.parentChar.getReduceInfectionPower() < 0.0F) {
                            this.parentChar.setReduceInfectionPower(0.0F);
                        }
                    }

                    if (this.stats.get(CharacterStat.ZOMBIE_FEVER) <= 0.0F) {
                        for (int ixx = 0; ixx < BodyPartType.ToIndex(BodyPartType.MAX); ixx++) {
                            this.getBodyParts().get(ixx).SetFakeInfected(false);
                        }

                        this.setIsFakeInfected(false);
                        this.stats.reset(CharacterStat.ZOMBIE_FEVER);
                        this.setReduceFakeInfection(false);
                    }

                    if (lastPain == this.stats.get(CharacterStat.PAIN)) {
                        this.stats.remove(CharacterStat.PAIN, 0.25F * GameTime.getInstance().getThirtyFPSMultiplier());
                    }
                }
            }
        }
    }

    public void calculateOverallHealth() {
        float totalDamage = 0.0F;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            BodyPart bodyPart = this.getBodyParts().get(i);
            totalDamage += (100.0F - bodyPart.getHealth()) * BodyPartType.getDamageModifyer(i);
        }

        totalDamage += this.getDamageFromPills();
        if (totalDamage > 100.0F) {
            totalDamage = 100.0F;
        }

        this.setOverallBodyHealth(100.0F - totalDamage);
    }

    public static float getSicknessFromCorpsesRate(int corpseCount) {
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() == 1) {
            return 0.0F;
        } else if (corpseCount > 5) {
            float inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.07F;
            switch (SandboxOptions.instance.decayingCorpseHealthImpact.getValue()) {
                case 2:
                    inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.01F;
                case 3:
                default:
                    break;
                case 4:
                    inc = (float)ZomboidGlobals.foodSicknessDecrease * 0.11F;
                    break;
                case 5:
                    inc = (float)ZomboidGlobals.foodSicknessDecrease;
            }

            int cap = Math.min(corpseCount - 5, FliesSound.maxCorpseCount - 5);
            return inc * cap;
        } else {
            return 0.0F;
        }
    }

    private void UpdateIllness() {
        if (SandboxOptions.instance.decayingCorpseHealthImpact.getValue() != 1) {
            float rate = this.GetBaseCorpseSickness();
            if (rate > 0.0F) {
                float defense = this.parentChar.getCorpseSicknessDefense(rate, true);
                if (defense > 0.0F) {
                    float multiplier = Math.max(0.0F, 1.0F - defense / 100.0F);
                    rate *= multiplier;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    rate *= 0.75F;
                } else if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    rate *= 1.25F;
                }

                if (rate > 0.0F) {
                    this.stats.add(CharacterStat.FOOD_SICKNESS, rate * GameTime.getInstance().getMultiplier());
                    this.parentChar.setCorpseSicknessRate(rate);
                    return;
                }
            }
        }

        this.parentChar.setCorpseSicknessRate(0.0F);
        if (this.stats.isAtMinimum(CharacterStat.POISON) && this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS)) {
            this.stats.remove(CharacterStat.FOOD_SICKNESS, (float)ZomboidGlobals.foodSicknessDecrease * GameTime.getInstance().getMultiplier());
        }
    }

    public float GetBaseCorpseSickness() {
        return getSicknessFromCorpsesRate(CorpseCount.instance.getCorpseCount(this.parentChar));
    }

    private void UpdateTemperatureState() {
        float delta = 0.06F;
        if (this.parentChar instanceof IsoPlayer isoPlayer) {
            if (this.coldDamageStage > 0.0F) {
                float maxHealth = 100.0F - this.coldDamageStage * 100.0F;
                if (maxHealth <= 0.0F) {
                    this.parentChar.setHealth(0.0F);
                    return;
                }

                if (this.overallBodyHealth > maxHealth) {
                    this.ReduceGeneralHealth(this.overallBodyHealth - maxHealth);
                }
            }

            isoPlayer.setMoveSpeed(0.06F);
        }
    }

    private float getDamageFromPills() {
        if (this.parentChar instanceof IsoPlayer player) {
            if (player.getSleepingPillsTaken() == 10) {
                return 40.0F;
            }

            if (player.getSleepingPillsTaken() == 11) {
                return 80.0F;
            }

            if (player.getSleepingPillsTaken() >= 12) {
                return 100.0F;
            }
        }

        return 0.0F;
    }

    public boolean UseBandageOnMostNeededPart() {
        int highestScore = 0;
        BodyPart part = null;

        for (int n = 0; n < this.getBodyParts().size(); n++) {
            int score = 0;
            if (!this.getBodyParts().get(n).bandaged()) {
                if (this.getBodyParts().get(n).bleeding()) {
                    score += 100;
                }

                if (this.getBodyParts().get(n).scratched()) {
                    score += 50;
                }

                if (this.getBodyParts().get(n).bitten()) {
                    score += 50;
                }

                if (score > highestScore) {
                    highestScore = score;
                    part = this.getBodyParts().get(n);
                }
            }
        }

        if (highestScore > 0 && part != null) {
            part.setBandaged(true, 10.0F);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the BodyParts
     */
    public ArrayList<BodyPart> getBodyParts() {
        return this.bodyParts;
    }

    /**
     * @return the DamageModCount
     */
    public int getDamageModCount() {
        return this.damageModCount;
    }

    /**
     * 
     * @param damageModCount the DamageModCount to set
     */
    public void setDamageModCount(int damageModCount) {
        this.damageModCount = damageModCount;
    }

    /**
     * @return the InfectionGrowthRate
     */
    public float getInfectionGrowthRate() {
        return this.infectionGrowthRate;
    }

    /**
     * 
     * @param infectionGrowthRate the InfectionGrowthRate to set
     */
    public void setInfectionGrowthRate(float infectionGrowthRate) {
        this.infectionGrowthRate = infectionGrowthRate;
    }

    public boolean isInfected() {
        return this.isInfected;
    }

    public void setInfected(boolean infected) {
        this.isInfected = infected;
    }

    public float getInfectionTime() {
        return this.infectionTime;
    }

    public void setInfectionTime(float worldHours) {
        this.infectionTime = worldHours;
    }

    public float getInfectionMortalityDuration() {
        return this.infectionMortalityDuration;
    }

    public void setInfectionMortalityDuration(float worldHours) {
        this.infectionMortalityDuration = worldHours;
    }

    private float getCurrentTimeForInfection() {
        return this.parentChar instanceof IsoPlayer ? (float)this.parentChar.getHoursSurvived() : (float)GameTime.getInstance().getWorldAgeHours();
    }

    /**
     * @return the inf
     */
    @Deprecated
    public boolean isInf() {
        return this.isInfected;
    }

    /**
     * 
     * @param inf the inf to set
     */
    @Deprecated
    public void setInf(boolean inf) {
        this.isInfected = inf;
    }

    /**
     * @return the IsFakeInfected
     */
    public boolean isIsFakeInfected() {
        return this.isFakeInfected;
    }

    /**
     * 
     * @param isFakeInfected the IsFakeInfected to set
     */
    public void setIsFakeInfected(boolean isFakeInfected) {
        this.isFakeInfected = isFakeInfected;
        this.getBodyParts().get(0).SetFakeInfected(isFakeInfected);
    }

    /**
     * @return the OverallBodyHealth
     */
    public float getOverallBodyHealth() {
        return this.overallBodyHealth;
    }

    /**
     * 
     * @param overallBodyHealth the OverallBodyHealth to set
     */
    public void setOverallBodyHealth(float overallBodyHealth) {
        this.overallBodyHealth = overallBodyHealth;
    }

    /**
     * @return the StandardHealthAddition
     */
    public float getStandardHealthAddition() {
        return this.standardHealthAddition;
    }

    /**
     * 
     * @param standardHealthAddition the StandardHealthAddition to set
     */
    public void setStandardHealthAddition(float standardHealthAddition) {
        this.standardHealthAddition = standardHealthAddition;
    }

    /**
     * @return the ReducedHealthAddition
     */
    public float getReducedHealthAddition() {
        return this.reducedHealthAddition;
    }

    /**
     * 
     * @param reducedHealthAddition the ReducedHealthAddition to set
     */
    public void setReducedHealthAddition(float reducedHealthAddition) {
        this.reducedHealthAddition = reducedHealthAddition;
    }

    /**
     * @return the SeverlyReducedHealthAddition
     */
    public float getSeverlyReducedHealthAddition() {
        return this.severlyReducedHealthAddition;
    }

    /**
     * 
     * @param severlyReducedHealthAddition the SeverlyReducedHealthAddition to set
     */
    public void setSeverlyReducedHealthAddition(float severlyReducedHealthAddition) {
        this.severlyReducedHealthAddition = severlyReducedHealthAddition;
    }

    /**
     * @return the SleepingHealthAddition
     */
    public float getSleepingHealthAddition() {
        return this.sleepingHealthAddition;
    }

    /**
     * 
     * @param sleepingHealthAddition the SleepingHealthAddition to set
     */
    public void setSleepingHealthAddition(float sleepingHealthAddition) {
        this.sleepingHealthAddition = sleepingHealthAddition;
    }

    /**
     * @return the HealthFromFood
     */
    public float getHealthFromFood() {
        return this.healthFromFood;
    }

    /**
     * 
     * @param healthFromFood the HealthFromFood to set
     */
    public void setHealthFromFood(float healthFromFood) {
        this.healthFromFood = healthFromFood;
    }

    /**
     * @return the HealthReductionFromSevereBadMoodles
     */
    public float getHealthReductionFromSevereBadMoodles() {
        return this.healthReductionFromSevereBadMoodles;
    }

    /**
     * 
     * @param healthReductionFromSevereBadMoodles the HealthReductionFromSevereBadMoodles to set
     */
    public void setHealthReductionFromSevereBadMoodles(float healthReductionFromSevereBadMoodles) {
        this.healthReductionFromSevereBadMoodles = healthReductionFromSevereBadMoodles;
    }

    /**
     * @return the StandardHealthFromFoodTime
     */
    public int getStandardHealthFromFoodTime() {
        return this.standardHealthFromFoodTime;
    }

    /**
     * 
     * @param standardHealthFromFoodTime the StandardHealthFromFoodTime to set
     */
    public void setStandardHealthFromFoodTime(int standardHealthFromFoodTime) {
        this.standardHealthFromFoodTime = standardHealthFromFoodTime;
    }

    /**
     * @return the HealthFromFoodTimer
     */
    public float getHealthFromFoodTimer() {
        return this.healthFromFoodTimer;
    }

    /**
     * 
     * @param healthFromFoodTimer the HealthFromFoodTimer to set
     */
    public void setHealthFromFoodTimer(float healthFromFoodTimer) {
        this.healthFromFoodTimer = healthFromFoodTimer;
    }

    /**
     * @return the BoredomDecreaseFromReading
     */
    public float getBoredomDecreaseFromReading() {
        return this.boredomDecreaseFromReading;
    }

    /**
     * 
     * @param boredomDecreaseFromReading the BoredomDecreaseFromReading to set
     */
    public void setBoredomDecreaseFromReading(float boredomDecreaseFromReading) {
        this.boredomDecreaseFromReading = boredomDecreaseFromReading;
    }

    /**
     * @return the InitialThumpPain
     */
    public float getInitialThumpPain() {
        return this.initialThumpPain;
    }

    /**
     * 
     * @param initialThumpPain the InitialThumpPain to set
     */
    public void setInitialThumpPain(float initialThumpPain) {
        this.initialThumpPain = initialThumpPain;
    }

    /**
     * @return the InitialScratchPain
     */
    public float getInitialScratchPain() {
        return this.initialScratchPain;
    }

    /**
     * 
     * @param initialScratchPain the InitialScratchPain to set
     */
    public void setInitialScratchPain(float initialScratchPain) {
        this.initialScratchPain = initialScratchPain;
    }

    /**
     * @return the InitialBitePain
     */
    public float getInitialBitePain() {
        return this.initialBitePain;
    }

    /**
     * 
     * @param initialBitePain the InitialBitePain to set
     */
    public void setInitialBitePain(float initialBitePain) {
        this.initialBitePain = initialBitePain;
    }

    /**
     * @return the InitialWoundPain
     */
    public float getInitialWoundPain() {
        return this.initialWoundPain;
    }

    /**
     * 
     * @param initialWoundPain the InitialWoundPain to set
     */
    public void setInitialWoundPain(float initialWoundPain) {
        this.initialWoundPain = initialWoundPain;
    }

    /**
     * @return the ContinualPainIncrease
     */
    public float getContinualPainIncrease() {
        return this.continualPainIncrease;
    }

    /**
     * 
     * @param continualPainIncrease the ContinualPainIncrease to set
     */
    public void setContinualPainIncrease(float continualPainIncrease) {
        this.continualPainIncrease = continualPainIncrease;
    }

    /**
     * @return the PainReductionFromMeds
     */
    public float getPainReductionFromMeds() {
        return this.painReductionFromMeds;
    }

    /**
     * 
     * @param painReductionFromMeds the PainReductionFromMeds to set
     */
    public void setPainReductionFromMeds(float painReductionFromMeds) {
        this.painReductionFromMeds = painReductionFromMeds;
    }

    /**
     * @return the StandardPainReductionWhenWell
     */
    public float getStandardPainReductionWhenWell() {
        return this.standardPainReductionWhenWell;
    }

    /**
     * 
     * @param standardPainReductionWhenWell the StandardPainReductionWhenWell to set
     */
    public void setStandardPainReductionWhenWell(float standardPainReductionWhenWell) {
        this.standardPainReductionWhenWell = standardPainReductionWhenWell;
    }

    /**
     * @return the OldNumZombiesVisible
     */
    public int getOldNumZombiesVisible() {
        return this.oldNumZombiesVisible;
    }

    /**
     * 
     * @param oldNumZombiesVisible the OldNumZombiesVisible to set
     */
    public void setOldNumZombiesVisible(int oldNumZombiesVisible) {
        this.oldNumZombiesVisible = oldNumZombiesVisible;
    }

    public boolean getWasDraggingCorpse() {
        return this.wasDraggingCorpse;
    }

    public void setWasDraggingCorpse(boolean wasDraggingCorpse) {
        this.wasDraggingCorpse = wasDraggingCorpse;
    }

    /**
     * @return the CurrentNumZombiesVisible
     */
    public int getCurrentNumZombiesVisible() {
        return this.currentNumZombiesVisible;
    }

    /**
     * 
     * @param currentNumZombiesVisible the CurrentNumZombiesVisible to set
     */
    public void setCurrentNumZombiesVisible(int currentNumZombiesVisible) {
        this.currentNumZombiesVisible = currentNumZombiesVisible;
    }

    /**
     * @return the PanicIncreaseValue
     */
    public float getPanicIncreaseValue() {
        return this.panicIncreaseValue;
    }

    public float getPanicIncreaseValueFrame() {
        return 0.035F;
    }

    /**
     * 
     * @param panicIncreaseValue the PanicIncreaseValue to set
     */
    public void setPanicIncreaseValue(float panicIncreaseValue) {
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.panicIncreaseValue = 0.0F;
        } else {
            this.panicIncreaseValue = panicIncreaseValue;
        }
    }

    /**
     * @return the PanicReductionValue
     */
    public float getPanicReductionValue() {
        return this.panicReductionValue;
    }

    /**
     * 
     * @param panicReductionValue the PanicReductionValue to set
     */
    public void setPanicReductionValue(float panicReductionValue) {
        this.panicReductionValue = panicReductionValue;
    }

    /**
     * @return the DrunkIncreaseValue
     */
    public float getDrunkIncreaseValue() {
        return this.drunkIncreaseValue;
    }

    /**
     * 
     * @param drunkIncreaseValue the DrunkIncreaseValue to set
     */
    public void setDrunkIncreaseValue(float drunkIncreaseValue) {
        this.drunkIncreaseValue = drunkIncreaseValue;
    }

    /**
     * @return the DrunkReductionValue
     */
    public float getDrunkReductionValue() {
        return this.drunkReductionValue;
    }

    /**
     * 
     * @param drunkReductionValue the DrunkReductionValue to set
     */
    public void setDrunkReductionValue(float drunkReductionValue) {
        this.drunkReductionValue = drunkReductionValue;
    }

    /**
     * @return the IsOnFire
     */
    public boolean isIsOnFire() {
        return this.isOnFire;
    }

    /**
     * 
     * @param isOnFire the IsOnFire to set
     */
    public void setIsOnFire(boolean isOnFire) {
        this.isOnFire = isOnFire;
    }

    /**
     * @return the BurntToDeath
     */
    public boolean isBurntToDeath() {
        return this.burntToDeath;
    }

    /**
     * 
     * @param burntToDeath the BurntToDeath to set
     */
    public void setBurntToDeath(boolean burntToDeath) {
        this.burntToDeath = burntToDeath;
    }

    /**
     * @return the CatchACold
     */
    public float getCatchACold() {
        return this.catchACold;
    }

    /**
     * 
     * @param catchACold the CatchACold to set
     */
    public void setCatchACold(float catchACold) {
        this.catchACold = catchACold;
    }

    /**
     * @return the HasACold
     */
    public boolean isHasACold() {
        return this.hasACold;
    }

    /**
     * 
     * @param hasACold the HasACold to set
     */
    public void setHasACold(boolean hasACold) {
        this.hasACold = hasACold;
    }

    /**
     * 
     * @param coldStrength the ColdStrength to set
     */
    public void setColdStrength(float coldStrength) {
        this.coldStrength = coldStrength;
    }

    /**
     * @return the ColdProgressionRate
     */
    public float getColdProgressionRate() {
        return this.coldProgressionRate;
    }

    /**
     * 
     * @param coldProgressionRate the ColdProgressionRate to set
     */
    public void setColdProgressionRate(float coldProgressionRate) {
        this.coldProgressionRate = coldProgressionRate;
    }

    public float getTimeToSneezeOrCough() {
        return this.timeToSneezeOrCough;
    }

    public void setTimeToSneezeOrCough(float timeToSneezeOrCough) {
        this.timeToSneezeOrCough = timeToSneezeOrCough;
    }

    public int getSmokerSneezeTimerMin() {
        return 43200;
    }

    public int getSmokerSneezeTimerMax() {
        return 129600;
    }

    /**
     * @return the MildColdSneezeTimerMin
     */
    public int getMildColdSneezeTimerMin() {
        return this.mildColdSneezeTimerMin;
    }

    /**
     * 
     * @param mildColdSneezeTimerMin the MildColdSneezeTimerMin to set
     */
    public void setMildColdSneezeTimerMin(int mildColdSneezeTimerMin) {
        this.mildColdSneezeTimerMin = mildColdSneezeTimerMin;
    }

    /**
     * @return the MildColdSneezeTimerMax
     */
    public int getMildColdSneezeTimerMax() {
        return this.mildColdSneezeTimerMax;
    }

    /**
     * 
     * @param mildColdSneezeTimerMax the MildColdSneezeTimerMax to set
     */
    public void setMildColdSneezeTimerMax(int mildColdSneezeTimerMax) {
        this.mildColdSneezeTimerMax = mildColdSneezeTimerMax;
    }

    /**
     * @return the ColdSneezeTimerMin
     */
    public int getColdSneezeTimerMin() {
        return this.coldSneezeTimerMin;
    }

    /**
     * 
     * @param coldSneezeTimerMin the ColdSneezeTimerMin to set
     */
    public void setColdSneezeTimerMin(int coldSneezeTimerMin) {
        this.coldSneezeTimerMin = coldSneezeTimerMin;
    }

    /**
     * @return the ColdSneezeTimerMax
     */
    public int getColdSneezeTimerMax() {
        return this.coldSneezeTimerMax;
    }

    /**
     * 
     * @param coldSneezeTimerMax the ColdSneezeTimerMax to set
     */
    public void setColdSneezeTimerMax(int coldSneezeTimerMax) {
        this.coldSneezeTimerMax = coldSneezeTimerMax;
    }

    /**
     * @return the NastyColdSneezeTimerMin
     */
    public int getNastyColdSneezeTimerMin() {
        return this.nastyColdSneezeTimerMin;
    }

    /**
     * 
     * @param nastyColdSneezeTimerMin the NastyColdSneezeTimerMin to set
     */
    public void setNastyColdSneezeTimerMin(int nastyColdSneezeTimerMin) {
        this.nastyColdSneezeTimerMin = nastyColdSneezeTimerMin;
    }

    /**
     * @return the NastyColdSneezeTimerMax
     */
    public int getNastyColdSneezeTimerMax() {
        return this.nastyColdSneezeTimerMax;
    }

    /**
     * 
     * @param nastyColdSneezeTimerMax the NastyColdSneezeTimerMax to set
     */
    public void setNastyColdSneezeTimerMax(int nastyColdSneezeTimerMax) {
        this.nastyColdSneezeTimerMax = nastyColdSneezeTimerMax;
    }

    /**
     * @return the SneezeCoughActive
     */
    public int getSneezeCoughActive() {
        return this.sneezeCoughActive;
    }

    /**
     * 
     * @param sneezeCoughActive the SneezeCoughActive to set
     */
    public void setSneezeCoughActive(int sneezeCoughActive) {
        this.sneezeCoughActive = sneezeCoughActive;
    }

    /**
     * @return the SneezeCoughTime
     */
    public int getSneezeCoughTime() {
        return this.sneezeCoughTime;
    }

    /**
     * 
     * @param sneezeCoughTime the SneezeCoughTime to set
     */
    public void setSneezeCoughTime(int sneezeCoughTime) {
        this.sneezeCoughTime = sneezeCoughTime;
    }

    /**
     * @return the SneezeCoughDelay
     */
    public int getSneezeCoughDelay() {
        return this.sneezeCoughDelay;
    }

    /**
     * 
     * @param sneezeCoughDelay the SneezeCoughDelay to set
     */
    public void setSneezeCoughDelay(int sneezeCoughDelay) {
        this.sneezeCoughDelay = sneezeCoughDelay;
    }

    /**
     * @return the ParentChar
     */
    public IsoGameCharacter getParentChar() {
        return this.parentChar;
    }

    public boolean isReduceFakeInfection() {
        return this.reduceFakeInfection;
    }

    public void setReduceFakeInfection(boolean reduceFakeInfection) {
        this.reduceFakeInfection = reduceFakeInfection;
    }

    public void AddRandomDamage() {
        BodyPart bodyPart = this.getBodyParts().get(Rand.Next(this.getBodyParts().size()));
        switch (Rand.Next(4)) {
            case 0:
                bodyPart.generateDeepWound();
                if (Rand.Next(4) == 0) {
                    bodyPart.setInfectedWound(true);
                }
                break;
            case 1:
                bodyPart.generateDeepShardWound();
                if (Rand.Next(4) == 0) {
                    bodyPart.setInfectedWound(true);
                }
                break;
            case 2:
                bodyPart.setFractureTime(Rand.Next(30, 50));
                break;
            case 3:
                bodyPart.setBurnTime(Rand.Next(30, 50));
        }
    }

    public float getPainReduction() {
        return this.painReduction;
    }

    public void setPainReduction(float painReduction) {
        this.painReduction = painReduction;
    }

    public float getColdReduction() {
        return this.coldReduction;
    }

    public void setColdReduction(float coldReduction) {
        this.coldReduction = coldReduction;
    }

    public int getRemotePainLevel() {
        return this.remotePainLevel;
    }

    public void setRemotePainLevel(int painLevel) {
        this.remotePainLevel = painLevel;
    }

    public float getColdDamageStage() {
        return this.coldDamageStage;
    }

    public void setColdDamageStage(float coldDamageStage) {
        this.coldDamageStage = coldDamageStage;
    }

    public Thermoregulator getThermoregulator() {
        return this.thermoregulator;
    }

    public void decreaseBodyWetness(float amount) {
        if (!this.bodyParts.isEmpty()) {
            for (int i = 0; i < this.bodyParts.size(); i++) {
                BodyPart bp = this.bodyParts.get(i);
                bp.setWetness(bp.getWetness() - amount);
            }
        }

        this.stats.remove(CharacterStat.WETNESS, amount);
    }

    public void increaseBodyWetness(float amount) {
        if (!this.bodyParts.isEmpty()) {
            for (int i = 0; i < this.bodyParts.size(); i++) {
                BodyPart bp = this.bodyParts.get(i);
                bp.setWetness(bp.getWetness() + amount);
            }
        }

        this.stats.add(CharacterStat.WETNESS, amount);
    }

    public void DamageFromAnimal(IsoAnimal wielder) {
        float damage = wielder.calcDamage();
        String dotSide = this.parentChar.testDotSide(wielder);
        boolean isBehind = dotSide.equals("BEHIND");
        this.parentChar.setHitFromBehind(isBehind);
        if (!GameClient.client) {
            int painType = 1;
            boolean doDamage = true;
            int partIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            boolean blade = true;
            boolean bullet = false;
            BodyPart part = this.getBodyPart(BodyPartType.FromIndex(partIndex));
            float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), true, false);
            if (Rand.Next(100) < def) {
                doDamage = false;
                this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(partIndex), false);
            }

            if (doDamage) {
                this.parentChar.addHole(BloodBodyPartType.FromIndex(partIndex));
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                if (wielder.adef.canDoLaceration && Rand.NextBool(6)) {
                    part.generateDeepWound();
                } else if (wielder.adef.canDoLaceration && Rand.NextBool(3)) {
                    part.setCut(true);
                } else if (Rand.NextBool(2)) {
                    part.setScratched(true, true);
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
                    damage *= 4.0F;
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    damage *= 4.0F;
                }

                if (partIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
                    damage *= 2.0F;
                }

                this.AddDamage(partIndex, damage);
                switch (1) {
                    case 0:
                        this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(partIndex));
                        break;
                    case 1:
                        this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(partIndex));
                        break;
                    case 2:
                        this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(partIndex));
                }

                if (GameServer.server) {
                    this.parentChar.getNetworkCharacterAI().syncDamage();
                }

                boolean behind;
                if (!wielder.isAimAtFloor()) {
                    behind = wielder.isBehind(this.parentChar);
                } else {
                    behind = this.parentChar.isFallOnFront();
                }

                boolean spikedPart;
                if (behind) {
                    spikedPart = this.parentChar.bodyPartIsSpikedBehind(partIndex);
                } else {
                    spikedPart = this.parentChar.bodyPartIsSpiked(partIndex);
                }

                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(partIndex), true, false, false);
                    wielder.spikePart(BodyPartType.Head);
                }

                if (GameServer.server && this.parentChar instanceof IsoPlayer player) {
                    player.syncVisuals();
                }
            }
        }
    }

    public float getGeneralWoundInfectionLevel() {
        if (SandboxOptions.instance.woundInfectionFactor.getValue() <= 0.0) {
            return 0.0F;
        } else {
            float woundInfectionLevel = 0.0F;
            if (!this.bodyParts.isEmpty()) {
                for (int i = 0; i < this.bodyParts.size(); i++) {
                    BodyPart bp = this.bodyParts.get(i);
                    if (bp.isInfectedWound()) {
                        woundInfectionLevel += bp.getWoundInfectionLevel();
                    }
                }
            }

            woundInfectionLevel *= 10.0F;
            woundInfectionLevel *= (float)SandboxOptions.instance.woundInfectionFactor.getValue();
            return Math.min(woundInfectionLevel, 100.0F);
        }
    }

    public void UpdateDiscomfort() {
        float draggingCorpseMod = this.parentChar.isDraggingCorpse() ? 0.3F : 0.0F;
        float clothingMod = this.parentChar.getClothingDiscomfortModifier();
        float bedMod = 0.0F;
        if (this.parentChar.isAsleep()) {
            String drunkMod = this.parentChar.getBedType();
            switch (drunkMod) {
                case "badBed":
                    bedMod = 0.3F;
                    break;
                case "badBedPillow":
                    bedMod = 0.2F;
                    break;
                case "floor":
                    bedMod = 0.5F;
                    break;
                case "floorPillow":
                    bedMod = 0.4F;
            }
        }

        float drunkMod = 1.0F - 0.5F * (this.stats.get(CharacterStat.INTOXICATION) / 100.0F);
        float hypoMod = 0.1F * this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
        float hyperMod = 0.1F * this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
        float wetMod = 0.1F * this.parentChar.getMoodles().getMoodleLevel(MoodleType.WET);
        float vehicleMod = this.parentChar.getVehicleDiscomfortModifier();
        float discomfortMod = 0.0F;
        discomfortMod += bedMod;
        discomfortMod += clothingMod;
        discomfortMod += draggingCorpseMod;
        discomfortMod += hypoMod;
        discomfortMod += hyperMod;
        discomfortMod += wetMod;
        discomfortMod += vehicleMod;
        discomfortMod *= drunkMod;
        float discomfortTarget = PZMath.clamp(discomfortMod, 0.0F, 1.0F) * 100.0F;
        float discomfortStepRate = 0.005F * GameTime.instance.getMultiplier();
        if (discomfortTarget > this.stats.get(CharacterStat.DISCOMFORT)) {
            discomfortStepRate *= 0.025F;
        }

        if (this.parentChar.isAsleep()) {
            this.stats.set(CharacterStat.DISCOMFORT, discomfortTarget);
        } else {
            float discomfort = this.stats.get(CharacterStat.DISCOMFORT);
            if (!PZMath.equal(discomfort, discomfortTarget, discomfortStepRate)) {
                this.stats.set(CharacterStat.DISCOMFORT, PZMath.lerp(discomfort, discomfortTarget, discomfortStepRate));
            } else if (discomfort != discomfortTarget) {
                this.stats.set(CharacterStat.DISCOMFORT, discomfortTarget);
            }
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.UNCOMFORTABLE) >= 1
            && this.stats.get(CharacterStat.STRESS) < CharacterStat.STRESS.getMaximumValue()) {
            float discomfortMalus = discomfortMod > 1.0F ? 3.0F + (discomfortMod - 1.0F) * 9.0F : 3.0F;
            this.stats
                .add(
                    CharacterStat.STRESS,
                    (float)(
                        ZomboidGlobals.stressFromDiscomfort
                            * (this.stats.get(CharacterStat.DISCOMFORT) * discomfortMalus)
                            * GameTime.instance.getMultiplier()
                            * GameTime.instance.getDeltaMinutesPerDay()
                    )
                );
        }
    }

    public void addStiffness(BodyPart part, float stiffness) {
        part.addStiffness(stiffness);
    }

    public void addStiffness(BodyPartType partType, float stiffness) {
        BodyPart part = this.getBodyPart(partType);
        part.addStiffness(stiffness);
    }
}
