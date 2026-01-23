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
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.inventory.types.WeaponType;
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

    public BodyDamage(IsoGameCharacter ParentCharacter) {
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Hand_R, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.ForeArm_R, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperArm_R, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Upper, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Torso_Lower, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Head, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Neck, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Groin, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.UpperLeg_R, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.LowerLeg_R, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_L, ParentCharacter));
        this.bodyParts.add(new BodyPart(BodyPartType.Foot_R, ParentCharacter));

        for (BodyPart part : this.bodyParts) {
            this.bodyPartsLastState.add(new BodyPartLast());
        }

        this.RestoreToFullHealth();
        this.parentChar = ParentCharacter;
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

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        for (int n = 0; n < this.getBodyParts().size(); n++) {
            BodyPart p = this.getBodyParts().get(n);
            p.setCut(input.get() == 1, false);
            p.SetBitten(input.get() == 1);
            p.setScratched(input.get() == 1, false);
            p.setBandaged(input.get() == 1, 0.0F);
            p.setBleeding(input.get() == 1);
            p.setDeepWounded(input.get() == 1);
            p.SetFakeInfected(input.get() == 1);
            p.SetInfected(input.get() == 1);
            p.SetHealth(input.getFloat());
            if (p.bandaged()) {
                p.setBandageLife(input.getFloat());
            }

            p.setInfectedWound(input.get() == 1);
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
            p.setHaveGlass(input.get() == 1);
            p.setGetBandageXp(input.get() == 1);
            p.setStitched(input.get() == 1);
            p.setStitchTime(input.getFloat());
            p.setGetStitchXp(input.get() == 1);
            p.setGetSplintXp(input.get() == 1);
            p.setFractureTime(input.getFloat());
            p.setSplint(input.get() == 1, 0.0F);
            if (p.isSplint()) {
                p.setSplintFactor(input.getFloat());
            }

            p.setHaveBullet(input.get() == 1, 0);
            p.setBurnTime(input.getFloat());
            p.setNeedBurnWash(input.get() == 1);
            p.setLastTimeBurnWash(input.getFloat());
            p.setSplintItem(GameWindow.ReadString(input));
            p.setBandageType(GameWindow.ReadString(input));
            p.setCutTime(input.getFloat());
            p.setWetness(input.getFloat());
            p.setStiffness(input.getFloat());
            if (WorldVersion >= 227) {
                p.setComfreyFactor(input.getFloat());
                p.setGarlicFactor(input.getFloat());
                p.setPlantainFactor(input.getFloat());
            }
        }

        this.setBodyPartsLastState();
        this.loadMainFields(input, WorldVersion);
        if (input.get() == 1) {
            if (this.thermoregulator != null) {
                this.thermoregulator.load(input, WorldVersion);
            } else {
                Thermoregulator thermos = new Thermoregulator(this);
                thermos.load(input, WorldVersion);
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

    public void loadMainFields(ByteBuffer input, int WorldVersion) {
        this.setCatchACold(input.getFloat());
        this.setHasACold(input.get() == 1);
        this.setColdStrength(input.getFloat());
        if (WorldVersion >= 222) {
            this.setTimeToSneezeOrCough(input.getInt());
        }

        this.setReduceFakeInfection(input.get() == 1);
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

    public void OnFire(boolean OnFire) {
        this.setIsOnFire(OnFire);
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

    public void IncreasePanic(int in_numNewZombiesSeen) {
        if (this.parentChar.getVehicle() != null) {
            in_numNewZombiesSeen /= 2;
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

        this.stats.add(CharacterStat.PANIC, this.getPanicIncreaseValue() * in_numNewZombiesSeen * del);
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

    public void JustTookPill(InventoryItem Pill) {
        if ("PillsBeta".equals(Pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.BetaBlockers(0.15F);
            } else {
                this.parentChar.BetaBlockers(0.3F);
            }
        } else if ("PillsAntiDep".equals(Pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.BetaAntiDepress(0.15F);
            } else {
                this.parentChar.BetaAntiDepress(0.3F);
            }
        } else if ("PillsSleepingTablets".equals(Pill.getType())) {
            this.parentChar.SleepingTablet(0.1F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                isoPlayer.setSleepingPillsTaken(isoPlayer.getSleepingPillsTaken() + 1);
            }
        } else if ("Pills".equals(Pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.parentChar.PainMeds(0.15F);
            } else {
                this.parentChar.PainMeds(0.45F);
            }
        } else if ("PillsVitamins".equals(Pill.getType())) {
            if (this.parentChar != null && this.stats.get(CharacterStat.INTOXICATION) > 10.0F) {
                this.stats.add(CharacterStat.FATIGUE, Pill.getFatigueChange() / 2.0F);
            } else {
                this.stats.set(CharacterStat.FATIGUE, Pill.getFatigueChange());
            }
        }

        this.stats.add(CharacterStat.STRESS, Pill.getStressChange());
        DrainableComboItem Pill2 = (DrainableComboItem)Pill;
        Object functionObj = LuaManager.getFunctionObject(Pill2.getOnEat());
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, Pill, this.parentChar);
        }

        Pill.UseAndSync();
    }

    public void JustAteFood(Food NewFood, float percentage) {
        this.JustAteFood(NewFood, percentage, false);
    }

    public void JustAteFood(Food NewFood, float percentage, boolean useUtensil) {
        if (NewFood.getPoisonPower() > 0) {
            float poisonPower = NewFood.getPoisonPower() * percentage;
            if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT) && !Objects.equals(NewFood.getType(), "Bleach")) {
                poisonPower /= 2.0F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                poisonPower *= 2.0F;
            }

            this.stats.add(CharacterStat.POISON, poisonPower);
            this.stats.add(CharacterStat.PAIN, NewFood.getPoisonPower() * percentage / 6.0F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                String debugStr = String.format(
                    "Player %s just ate poisoned food %s with poison power %f", isoPlayer.getDisplayName(), NewFood.getDisplayName(), poisonPower
                );
                DebugLog.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write(debugStr);
            }
        }

        if (NewFood.isTainted()) {
            float poisonPowerx = 20.0F * percentage;
            this.stats.add(CharacterStat.POISON, poisonPowerx);
            this.stats.add(CharacterStat.PAIN, 10.0F * percentage / 6.0F);
            if (this.parentChar instanceof IsoPlayer isoPlayer) {
                String debugStr = String.format(
                    "Player %s just ate tainted food %s with poison power %f", isoPlayer.getDisplayName(), NewFood.getDisplayName(), poisonPowerx
                );
                DebugLog.Objects.debugln(debugStr);
                LoggerManager.getLogger("user").write(debugStr);
            }
        }

        if (NewFood.getReduceInfectionPower() > 0.0F) {
            this.parentChar.setReduceInfectionPower(NewFood.getReduceInfectionPower());
        }

        float modifier = 1.0F;
        if (useUtensil) {
            if (NewFood.getBoredomChange() * percentage < 0.0F) {
                modifier = 1.25F;
            } else {
                modifier = 0.75F;
            }

            DebugLog.log("boredomChange %modifier from using an eating utensil: " + modifier);
        }

        this.stats.add(CharacterStat.BOREDOM, NewFood.getBoredomChange() * percentage * modifier);
        modifier = 1.0F;
        if (useUtensil) {
            if (NewFood.getUnhappyChange() * percentage < 0.0F) {
                modifier = 1.25F;
            } else {
                modifier = 0.75F;
            }

            DebugLog.log("unhappyChange %modifier from using an eating utensil: " + modifier);
        }

        this.stats.add(CharacterStat.UNHAPPINESS, NewFood.getUnhappyChange() * percentage * modifier);
        if (NewFood.isAlcoholic()) {
            this.JustDrankBooze(NewFood, percentage);
        }

        if (this.stats.isAtMinimum(CharacterStat.HUNGER)) {
            float hungerChange = Math.abs(NewFood.getHungerChange()) * percentage;
            this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            if (NewFood.isCooked()) {
                this.setHealthFromFoodTimer((int)(this.getHealthFromFoodTimer() + hungerChange * this.getHealthFromFoodTimeByHunger()));
            }

            if (this.getHealthFromFoodTimer() > 11000.0F) {
                this.setHealthFromFoodTimer(11000.0F);
            }
        }

        if (!"Tutorial".equals(Core.getInstance().getGameMode())) {
            if (!NewFood.isCooked() && NewFood.isbDangerousUncooked()) {
                this.setHealthFromFoodTimer(0.0F);
                int IllnessChance = 75;
                if (NewFood.hasTag(ItemTag.EGG)) {
                    IllnessChance = 5;
                }

                if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                    IllnessChance /= 2;
                    if (NewFood.hasTag(ItemTag.EGG)) {
                        IllnessChance = 0;
                    }
                }

                if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                    IllnessChance *= 2;
                }

                if (IllnessChance > 0 && !this.isInfected() && !NewFood.isBurnt()) {
                    this.stats.add(CharacterStat.POISON, 15.0F * percentage);
                }
            }

            if (NewFood.getAge() >= NewFood.getOffAgeMax()) {
                float Offness = NewFood.getAge() - NewFood.getOffAgeMax();
                if (Offness == 0.0F) {
                    Offness = 1.0F;
                }

                if (Offness > 5.0F) {
                    Offness = 5.0F;
                }

                int IllnessChancex;
                if (NewFood.getOffAgeMax() > NewFood.getOffAge()) {
                    IllnessChancex = (int)(Offness / (NewFood.getOffAgeMax() - NewFood.getOffAge()) * 100.0F);
                } else {
                    IllnessChancex = 100;
                }

                if (this.parentChar.hasTrait(CharacterTrait.IRON_GUT)) {
                    IllnessChancex /= 2;
                }

                if (this.parentChar.hasTrait(CharacterTrait.WEAK_STOMACH)) {
                    IllnessChancex *= 2;
                }

                if (!this.isInfected()) {
                    if (Rand.Next(100) < IllnessChancex) {
                        float poisonPowerx = 5.0F * Math.abs(NewFood.getHungChange() * 10.0F) * percentage;
                        this.stats.add(CharacterStat.POISON, poisonPowerx);
                        if (this.parentChar instanceof IsoPlayer isoPlayer) {
                            String debugStr = String.format(
                                "Player %s just ate spoiled food %s with poison power %f", isoPlayer.getDisplayName(), NewFood.getDisplayName(), poisonPowerx
                            );
                            DebugLog.Objects.debugln(debugStr);
                            LoggerManager.getLogger("user").write(debugStr);
                        }
                    } else {
                        this.stats.add(CharacterStat.POISON, 2.0F * Math.abs(NewFood.getHungChange() * 10.0F) * percentage);
                    }
                }
            }
        }
    }

    public void JustAteFood(Food NewFood) {
        this.JustAteFood(NewFood, 100.0F);
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
        float WetnessIncrease = 0.0F;
        float WetnessDecrease = 0.0F;
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
                    WetnessIncrease = val;
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

            WetnessIncrease = valx;
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

            WetnessDecrease = valx;
        }

        if (clothingWetness != null) {
            clothingWetness.updateWetness(WetnessIncrease, WetnessDecrease);
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
        float Delta = 0.0F;
        if (this.thermoregulator != null) {
            Delta = this.thermoregulator.getCatchAColdDelta();
        }

        if (!this.isHasACold() && Delta > 0.1F) {
            if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                Delta *= 1.7F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                Delta *= 0.45F;
            }

            if (this.parentChar.hasTrait(CharacterTrait.OUTDOORSMAN)) {
                Delta *= 0.25F;
            }

            this.setCatchACold(this.getCatchACold() + (float)ZomboidGlobals.catchAColdIncreaseRate * Delta * GameTime.instance.getMultiplier());
            if (this.getCatchACold() >= 100.0F) {
                this.setCatchACold(0.0F);
                this.setHasACold(true);
                this.setColdStrength(20.0F);
                this.setTimeToSneezeOrCough(0.0F);
            }
        }

        if (Delta <= 0.1F) {
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

            boolean TissueConsumed = false;
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

                    TissueConsumed = true;
                }
            } else if (this.parentChar.getPrimaryHandItem().getCurrentUses() > 0) {
                this.parentChar.getPrimaryHandItem().setCurrentUses(this.parentChar.getPrimaryHandItem().getCurrentUses() - 1);
                if (this.parentChar.getPrimaryHandItem().getCurrentUses() <= 0) {
                    this.parentChar.getPrimaryHandItem().Use();
                }

                TissueConsumed = true;
            }

            if (TissueConsumed) {
                this.setSneezeCoughActive(this.getSneezeCoughActive() + 2);
            } else {
                int Dist = 20;
                int Vol = 20;
                if (this.getSneezeCoughActive() == 1) {
                    Dist = 20;
                    Vol = 25;
                }

                if (this.getSneezeCoughActive() == 2) {
                    Dist = 35;
                    Vol = 40;
                }

                WorldSoundManager.WorldSound sneeze = WorldSoundManager.instance
                    .addSound(
                        this.parentChar,
                        PZMath.fastfloor(this.parentChar.getX()),
                        PZMath.fastfloor(this.parentChar.getY()),
                        PZMath.fastfloor(this.parentChar.getZ()),
                        Dist,
                        Vol,
                        false
                    );
                sneeze.stressAnimals = false;
            }
        }
    }

    public int IsSneezingCoughing() {
        return this.getSneezeCoughActive();
    }

    public void UpdateCold() {
        if (this.isHasACold()) {
            boolean Recovering = true;
            IsoGridSquare sq = this.parentChar.getCurrentSquare();
            if (sq == null
                || !sq.isInARoom()
                || this.parentChar.getMoodles().getMoodleLevel(MoodleType.WET) > 0
                || this.parentChar.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) >= 1
                || this.stats.get(CharacterStat.FATIGUE) > 0.5F
                || this.stats.get(CharacterStat.HUNGER) > 0.25F
                || this.stats.get(CharacterStat.THIRST) > 0.25F) {
                Recovering = false;
            }

            if (this.getColdReduction() > 0.0F) {
                Recovering = true;
                this.setColdReduction(this.getColdReduction() - 0.005F * GameTime.instance.getMultiplier());
                if (this.getColdReduction() < 0.0F) {
                    this.setColdReduction(0.0F);
                }
            }

            if (Recovering) {
                float Delta = 1.0F;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    Delta = 0.5F;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    Delta = 1.5F;
                }

                this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * Delta * GameTime.instance.getMultiplier());
                if (this.getColdReduction() > 0.0F) {
                    this.setColdStrength(this.getColdStrength() - this.getColdProgressionRate() * Delta * GameTime.instance.getMultiplier());
                }

                if (this.getColdStrength() < 0.0F) {
                    this.setColdStrength(0.0F);
                    this.setHasACold(false);
                    this.setCatchACold(0.0F);
                }
            } else {
                float Deltax = 1.0F;
                if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    Deltax = 1.2F;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    Deltax = 0.8F;
                }

                this.setColdStrength(this.getColdStrength() + this.getColdProgressionRate() * Deltax * GameTime.instance.getMultiplier());
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

    public void AddDamage(BodyPartType BodyPart, float Val) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).AddDamage(Val);
    }

    public void AddGeneralHealth(float Val) {
        int NumDamagedParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).getHealth() < 100.0F) {
                NumDamagedParts++;
            }
        }

        if (NumDamagedParts > 0) {
            float HealthPerPart = Val / NumDamagedParts;

            for (int ix = 0; ix < BodyPartType.ToIndex(BodyPartType.MAX); ix++) {
                if (this.getBodyParts().get(ix).getHealth() < 100.0F) {
                    this.getBodyParts().get(ix).AddHealth(HealthPerPart);
                }
            }
        }
    }

    public void ReduceGeneralHealth(float Val) {
        if (this.getOverallBodyHealth() <= 10.0F) {
            this.parentChar.forceAwake();
        }

        if (!(Val <= 0.0F)) {
            float HealthPerPart = Val / BodyPartType.ToIndex(BodyPartType.MAX);

            for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
                this.getBodyParts().get(i).ReduceHealth(HealthPerPart / BodyPartType.getDamageModifyer(i));
            }
        }
    }

    public void AddDamage(int BodyPartIndex, float val) {
        this.getBodyParts().get(BodyPartIndex).AddDamage(val);
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

    public void DamageFromWeapon(HandWeapon weapon, int PartIndex) {
        if (GameClient.client) {
            IsoPlayer player = Type.tryCastTo(this.parentChar, IsoPlayer.class);
            if (player != null && !player.isLocalPlayer()) {
                return;
            }
        }

        int DamageType = 0;
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

        if (PartIndex == -1) {
            PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
        }

        BodyPart part = this.getBodyPart(BodyPartType.FromIndex(PartIndex));
        float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), blade, bullet);
        if (Rand.Next(100) < def) {
            IsoPlayer owner = weapon.getUsingPlayer();
            if (owner != null && WeaponType.getWeaponType(weapon) == WeaponType.KNIFE && !weapon.hasTag(ItemTag.HANDGUARD)) {
                boolean spikedPart = isSpikedPart(owner, this.parentChar, PartIndex);
                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                    owner.spikePart(BodyPartType.Hand_R);
                }
            }

            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), false);
            this.parentChar.playWeaponHitArmourSound(PartIndex, bullet);
        } else {
            this.parentChar.addHole(BloodBodyPartType.FromIndex(PartIndex));
            this.parentChar.splatBloodFloorBig();
            this.parentChar.splatBloodFloorBig();
            this.parentChar.splatBloodFloorBig();
            float Pain = 0.0F;
            if (blade) {
                if (Rand.NextBool(6)) {
                    DamageType = 1;
                    part.generateDeepWound();
                } else if (Rand.NextBool(3)) {
                    DamageType = 2;
                    part.setCut(true);
                } else {
                    DamageType = 3;
                    part.setScratched(true, true);
                }

                Pain = this.getInitialScratchPain() * BodyPartType.getPainModifyer(PartIndex);
            } else if (blunt) {
                if (Rand.NextBool(4)) {
                    DamageType = 4;
                    part.setCut(true);
                } else {
                    DamageType = 5;
                    part.setScratched(true, true);
                }

                Pain = this.getInitialThumpPain() * BodyPartType.getPainModifyer(PartIndex);
            } else if (bullet) {
                DamageType = 6;
                part.setHaveBullet(true, 0);
                Pain = this.getInitialBitePain() * BodyPartType.getPainModifyer(PartIndex);
            }

            float Damage = Rand.Next(weapon.getMinDamage(), weapon.getMaxDamage()) * 15.0F;
            if (PartIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
                Damage *= 4.0F;
            }

            if (PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                Damage *= 4.0F;
            }

            if (PartIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
                Damage *= 2.0F;
            }

            if (GameServer.server) {
                if (weapon.isRanged()) {
                    Damage = (float)(Damage * ServerOptions.getInstance().pvpFirearmDamageModifier.getValue());
                } else {
                    Damage = (float)(Damage * ServerOptions.getInstance().pvpMeleeDamageModifier.getValue());
                }
            }

            damageFromSpikedArmor(weapon.getUsingPlayer(), this.parentChar, PartIndex, weapon);
            this.applyDamageFromWeapon(PartIndex, Damage, DamageType, Pain);
            this.parentChar.playWeaponHitArmourSound(PartIndex, bullet);
        }
    }

    /**
     * This gonna decide the strength of the damage you'll get.
     *  Getting surrounded can also trigger an instant death animation.
     */
    public boolean AddRandomDamageFromZombie(IsoZombie zombie, String hitReaction) {
        if (StringUtils.isNullOrEmpty(hitReaction)) {
            hitReaction = "Bite";
        }

        this.parentChar.setVariable("hitpvp", false);
        int PainType = 0;
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

        int PartIndex;
        if (!zombie.crawling) {
            if (Rand.Next(10) == 0) {
                PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Groin) + 1);
            } else {
                PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1);
            }

            float chanceToGetNeck = 10.0F * zombiesAttacking;
            if (isBehind) {
                chanceToGetNeck += 5.0F;
            }

            if (isLeftOrRight) {
                chanceToGetNeck += 2.0F;
            }

            if (isBehind && Rand.Next(100) < chanceToGetNeck) {
                PartIndex = BodyPartType.ToIndex(BodyPartType.Neck);
            }

            if (PartIndex == BodyPartType.ToIndex(BodyPartType.Head) || PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                int percent = 70;
                if (isBehind) {
                    percent = 90;
                }

                if (isLeftOrRight) {
                    percent = 80;
                }

                if (Rand.Next(100) > percent) {
                    boolean Done = false;

                    while (!Done) {
                        Done = true;
                        PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Torso_Lower) + 1);
                        if (PartIndex == BodyPartType.ToIndex(BodyPartType.Head)
                            || PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)
                            || PartIndex == BodyPartType.ToIndex(BodyPartType.Groin)) {
                            Done = false;
                        }
                    }
                }
            }
        } else {
            if (Rand.Next(2) != 0) {
                return false;
            }

            if (Rand.Next(10) == 0) {
                PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.MAX));
            } else {
                PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.UpperLeg_L), BodyPartType.ToIndex(BodyPartType.MAX));
            }
        }

        if (zombie.inactive) {
            baseChance += 20;
            baseBiteChance += 20;
            baseLacerationChance += 20;
        }

        float Damage = Rand.Next(1000) / 1000.0F;
        Damage *= Rand.Next(10) + 10;
        if (GameServer.server && this.parentChar instanceof IsoPlayer || Core.debug && this.parentChar instanceof IsoPlayer) {
            DebugLog.DetailedInfo
                .trace(
                    "zombie did "
                        + Damage
                        + " dmg to "
                        + ((IsoPlayer)this.parentChar).getDisplayName()
                        + " on body part "
                        + BodyPartType.getDisplayName(BodyPartType.FromIndex(PartIndex))
                );
        }

        boolean holeDone = false;
        boolean scratchOrBite = true;
        boolean behind = isBehind || this.parentChar.isFallOnFront();
        if (Rand.Next(100) > baseChance) {
            boolean spikedPart;
            if (behind) {
                spikedPart = this.parentChar.bodyPartIsSpikedBehind(PartIndex);
            } else {
                spikedPart = this.parentChar.bodyPartIsSpiked(PartIndex);
            }

            zombie.scratch = true;
            this.parentChar.helmetFall(PartIndex == BodyPartType.ToIndex(BodyPartType.Neck) || PartIndex == BodyPartType.ToIndex(BodyPartType.Head));
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
                float defense = this.parentChar.getBodyPartClothingDefense(PartIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackScratch);
                if (this.getHealth() > 0.0F) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }

                if (this.getHealth() > 0.0F && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }

                if (Rand.Next(100) < defense) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), scratchOrBite);
                    return false;
                }

                boolean addedHole = this.parentChar.addHole(BloodBodyPartType.FromIndex(PartIndex), true);
                if (addedHole) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                PainType = 1;
                if (this.parentChar instanceof IsoPlayer isoPlayer) {
                    DebugLog.DetailedInfo.trace("zombie scratched %s in body location %s", isoPlayer.getUsername(), BloodBodyPartType.FromIndex(PartIndex));
                    isoPlayer.playerVoiceSound("PainFromScratch");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayer);
                        return true;
                    }
                }

                this.AddDamage(PartIndex, Damage);
                this.SetScratched(PartIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, true);
            } else if (zombie.laceration) {
                float defensex = this.parentChar.getBodyPartClothingDefense(PartIndex, false, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackLacerate);
                if (this.getHealth() > 0.0F) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieScratch", null);
                }

                if (this.getHealth() > 0.0F && spikedPart) {
                    if (Rand.NextBool(2)) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_L);
                    } else {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                        zombie.spikePart(BodyPartType.Hand_R);
                    }
                }

                if (Rand.Next(100) < defensex) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), scratchOrBite);
                    return false;
                }

                boolean addedHolex = this.parentChar.addHole(BloodBodyPartType.FromIndex(PartIndex), true);
                if (addedHolex) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                PainType = 1;
                if (this.parentChar instanceof IsoPlayer isoPlayerx) {
                    DebugLog.DetailedInfo.trace("zombie laceration %s in body location %s", isoPlayerx.getUsername(), BloodBodyPartType.FromIndex(PartIndex));
                    isoPlayerx.playerVoiceSound("PainFromLacerate");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayerx);
                        return true;
                    }
                }

                this.AddDamage(PartIndex, Damage);
                this.SetCut(PartIndex, true);
                this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, true);
            } else {
                float defensexx = this.parentChar.getBodyPartClothingDefense(PartIndex, true, false);
                zombie.parameterZombieState.setState(ParameterZombieState.State.AttackBite);
                if (this.getHealth() > 0.0F) {
                    String soundName = zombie.getBiteSoundName();
                    if (PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                        soundName = "NeckBite";
                    }

                    this.parentChar.getEmitter().playSoundImpl(soundName, null);
                }

                if (Rand.Next(100) < defensexx) {
                    this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), scratchOrBite);
                    if (spikedPart) {
                        this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), false, true, false);
                        zombie.spikePart(BodyPartType.Head);
                    }

                    return false;
                }

                boolean addedHolexx = this.parentChar.addHole(BloodBodyPartType.FromIndex(PartIndex), true);
                if (addedHolexx) {
                    this.parentChar.getEmitter().playSoundImpl("ZombieRipClothing", null);
                }

                holeDone = true;
                PainType = 2;
                if (this.parentChar instanceof IsoPlayer isoPlayerxx) {
                    DebugLog.DetailedInfo.trace("zombie bite %s in body location %s", isoPlayerxx.getUsername(), BloodBodyPartType.FromIndex(PartIndex));
                    isoPlayerxx.playerVoiceSound("PainFromBite");
                    if (GameClient.client) {
                        GameClient.sendZombieHit(zombie, isoPlayerxx);
                        return true;
                    }
                }

                this.AddDamage(PartIndex, Damage);
                this.SetBitten(PartIndex, true);
                if (PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), false, true, true);
                    this.parentChar.addBlood(BloodBodyPartType.Torso_Upper, false, true, false);
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                    this.parentChar.splatBloodFloorBig();
                }

                this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), false, true, true);
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                this.parentChar.splatBloodFloorBig();
                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), false, true, false);
                    zombie.spikePart(BodyPartType.Head);
                    zombie.Kill(null);
                }
            }
        }

        if (!holeDone) {
            this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), scratchOrBite);
        }

        switch (PainType) {
            case 0:
                this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(PartIndex));
                break;
            case 1:
                this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(PartIndex));
                break;
            case 2:
                this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(PartIndex));
        }

        if (GameServer.server && this.parentChar instanceof IsoPlayer isoPlayerxxx) {
            isoPlayerxxx.getNetworkCharacterAI().syncDamage();
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

    public void DrawUntexturedQuad(int X, int Y, int Width, int Height, float r, float g, float b, float a) {
        SpriteRenderer.instance.renderi(null, X, Y, Width, Height, r, g, b, a, null);
    }

    public float getBodyPartHealth(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).getHealth();
    }

    public float getBodyPartHealth(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).getHealth();
    }

    public String getBodyPartName(BodyPartType BodyPart) {
        return BodyPartType.ToString(BodyPart);
    }

    public String getBodyPartName(int BodyPartIndex) {
        return BodyPartType.ToString(BodyPartType.FromIndex(BodyPartIndex));
    }

    public float getHealth() {
        return this.getOverallBodyHealth();
    }

    public float getApparentInfectionLevel() {
        float infectionLevel = Math.max(this.stats.get(CharacterStat.ZOMBIE_FEVER), this.stats.get(CharacterStat.ZOMBIE_INFECTION));
        return Math.max(this.stats.get(CharacterStat.FOOD_SICKNESS), infectionLevel);
    }

    public int getNumPartsBleeding() {
        int BleedingParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).bleeding()) {
                BleedingParts++;
            }
        }

        return BleedingParts;
    }

    public boolean isNeckBleeding() {
        return this.getBodyPart(BodyPartType.Neck).bleeding();
    }

    public int getNumPartsScratched() {
        int ScratchedParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).scratched()) {
                ScratchedParts++;
            }
        }

        return ScratchedParts;
    }

    public int getNumPartsBitten() {
        int BittenParts = 0;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).bitten()) {
                BittenParts++;
            }
        }

        return BittenParts;
    }

    public boolean HasInjury() {
        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            if (this.getBodyParts().get(i).HasInjury()) {
                return true;
            }
        }

        return false;
    }

    public boolean IsBandaged(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).bandaged();
    }

    public boolean IsDeepWounded(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).deepWounded();
    }

    public boolean IsBandaged(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).bandaged();
    }

    public boolean IsBitten(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).bitten();
    }

    public boolean IsBitten(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).bitten();
    }

    public boolean IsBleeding(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).bleeding();
    }

    public boolean IsBleeding(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).bleeding();
    }

    public boolean IsBleedingStemmed(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).IsBleedingStemmed();
    }

    public boolean IsBleedingStemmed(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).IsBleedingStemmed();
    }

    public boolean IsCauterized(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).IsCauterized();
    }

    public boolean IsCauterized(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).IsCauterized();
    }

    public boolean IsInfected() {
        return this.isInfected;
    }

    public boolean IsInfected(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).IsInfected();
    }

    public boolean IsInfected(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).IsInfected();
    }

    public boolean IsFakeInfected(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).IsFakeInfected();
    }

    public void DisableFakeInfection(int BodyPartIndex) {
        this.getBodyParts().get(BodyPartIndex).DisableFakeInfection();
    }

    public boolean IsScratched(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).scratched();
    }

    public boolean IsCut(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).getCutTime() > 0.0F;
    }

    public boolean IsScratched(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).scratched();
    }

    public boolean IsStitched(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).stitched();
    }

    public boolean IsStitched(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).stitched();
    }

    public boolean IsWounded(BodyPartType BodyPart) {
        return this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).deepWounded();
    }

    public boolean IsWounded(int BodyPartIndex) {
        return this.getBodyParts().get(BodyPartIndex).deepWounded();
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

    public void SetBandaged(int BodyPartIndex, boolean Bandaged, float bandageLife, boolean isAlcoholic, String bandageType) {
        this.getBodyParts().get(BodyPartIndex).setBandaged(Bandaged, bandageLife, isAlcoholic, bandageType);
    }

    public void SetBitten(BodyPartType BodyPart, boolean Bitten) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).SetBitten(Bitten);
    }

    public void SetBitten(int BodyPartIndex, boolean Bitten) {
        this.getBodyParts().get(BodyPartIndex).SetBitten(Bitten);
    }

    public void SetBitten(int BodyPartIndex, boolean Bitten, boolean Infected) {
        this.getBodyParts().get(BodyPartIndex).SetBitten(Bitten, Infected);
    }

    public void SetBleeding(BodyPartType BodyPart, boolean Bleeding) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).setBleeding(Bleeding);
    }

    public void SetBleeding(int BodyPartIndex, boolean Bleeding) {
        this.getBodyParts().get(BodyPartIndex).setBleeding(Bleeding);
    }

    public void SetBleedingStemmed(BodyPartType BodyPart, boolean BleedingStemmed) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).SetBleedingStemmed(BleedingStemmed);
    }

    public void SetBleedingStemmed(int BodyPartIndex, boolean BleedingStemmed) {
        this.getBodyParts().get(BodyPartIndex).SetBleedingStemmed(BleedingStemmed);
    }

    public void SetCauterized(BodyPartType BodyPart, boolean Cauterized) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).SetCauterized(Cauterized);
    }

    public void SetCauterized(int BodyPartIndex, boolean Cauterized) {
        this.getBodyParts().get(BodyPartIndex).SetCauterized(Cauterized);
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

    public void SetScratched(BodyPartType BodyPart, boolean Scratched) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).setScratched(Scratched, false);
    }

    public void SetScratched(int BodyPartIndex, boolean Scratched) {
        this.getBodyParts().get(BodyPartIndex).setScratched(Scratched, false);
    }

    public void SetScratchedFromWeapon(int BodyPartIndex, boolean Scratched) {
        this.getBodyParts().get(BodyPartIndex).SetScratchedWeapon(Scratched);
    }

    public void SetCut(int BodyPartIndex, boolean Cut) {
        this.getBodyParts().get(BodyPartIndex).setCut(Cut, false);
    }

    public void SetWounded(BodyPartType BodyPart, boolean Wounded) {
        this.getBodyParts().get(BodyPartType.ToIndex(BodyPart)).setDeepWounded(Wounded);
    }

    public void SetWounded(int BodyPartIndex, boolean Wounded) {
        this.getBodyParts().get(BodyPartIndex).setDeepWounded(Wounded);
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
        int NumStrengthReducers = 0;
        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
            NumStrengthReducers += 3;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 2) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 3) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 2) {
            NumStrengthReducers++;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 3) {
            NumStrengthReducers += 2;
        }

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.INJURED) == 4) {
            NumStrengthReducers += 3;
        }

        this.parentChar.setMaxWeight((int)(this.parentChar.getMaxWeightBase() * this.parentChar.getWeightMod()) - NumStrengthReducers);
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
                    float HealthToAdd = 0.0F;
                    if (this.getHealthFromFoodTimer() > 0.0F) {
                        HealthToAdd += this.getHealthFromFood() * GameTime.instance.getMultiplier();
                        this.setHealthFromFoodTimer(this.getHealthFromFoodTimer() - 1.0F * GameTime.instance.getMultiplier());
                    }

                    int Reduced = 0;
                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 2
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 2
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 2) {
                        Reduced = 1;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 3
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 3
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 3) {
                        Reduced = 2;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4
                        || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                        Reduced = 3;
                    }

                    if (this.parentChar.isAsleep()) {
                        Reduced = -1;
                    }

                    switch (Reduced) {
                        case 0:
                            HealthToAdd += this.getStandardHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 1:
                            HealthToAdd += this.getReducedHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 2:
                            HealthToAdd += this.getSeverlyReducedHealthAddition() * GameTime.instance.getMultiplier();
                            break;
                        case 3:
                            HealthToAdd += 0.0F;
                    }

                    if (this.parentChar.isAsleep()) {
                        if (GameClient.client) {
                            HealthToAdd += 15.0F * GameTime.instance.getGameWorldSecondsSinceLastUpdate() / 3600.0F;
                        } else {
                            HealthToAdd += this.getSleepingHealthAddition() * GameTime.instance.getMultiplier();
                        }

                        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HUNGRY) == 4
                            || this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                            HealthToAdd = 0.0F;
                        }
                    }

                    this.AddGeneralHealth(HealthToAdd);
                    HealthToAdd = 0.0F;
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
                            HealthToAdd += poisonDamage;
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
                        HealthToAdd += hungryDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.SICK) == 4) {
                        if (this.stats.get(CharacterStat.FOOD_SICKNESS) > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                            sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                            HealthToAdd += sickDamage;
                        } else if (SandboxOptions.instance.woundInfectionFactor.getValue() > 0.0
                            && this.getGeneralWoundInfectionLevel() > this.stats.get(CharacterStat.ZOMBIE_INFECTION)) {
                            sickDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                            HealthToAdd += sickDamage;
                        }
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.BLEEDING) == 4) {
                        bleedingDamage = this.getHealthReductionFromSevereBadMoodles() * GameTime.instance.getMultiplier();
                        HealthToAdd += bleedingDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.THIRST) == 4) {
                        thirstDamage = this.getHealthReductionFromSevereBadMoodles() / 10.0F * GameTime.instance.getMultiplier();
                        HealthToAdd += thirstDamage;
                    }

                    if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD) > 2
                        && this.parentChar.getVehicle() == null
                        && !this.parentChar.isAsleep()
                        && !this.parentChar.isSitOnGround()
                        && !this.parentChar.isSittingOnFurniture()
                        && this.getThermoregulator().getMetabolicTarget() != Metabolics.SeatedResting.getMet()
                        && this.getHealth() > 75.0F
                        && Rand.Next(Rand.AdjustForFramerate(10)) == 0) {
                        heavyLoadDamage = this.getHealthReductionFromSevereBadMoodles()
                            / ((5 - this.parentChar.getMoodles().getMoodleLevel(MoodleType.HEAVY_LOAD)) / 10.0F)
                            * GameTime.instance.getMultiplier();
                        HealthToAdd += heavyLoadDamage;
                        this.parentChar.addBackMuscleStrain(heavyLoadDamage / 2.0F);
                    }

                    this.ReduceGeneralHealth(HealthToAdd);
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
                        HealthToAdd = 0.0F;

                        for (int ixx = 0; ixx < BodyPartType.ToIndex(BodyPartType.MAX); ixx++) {
                            HealthToAdd += this.getBodyParts().get(ixx).getPain() * BodyPartType.getPainModifyer(ixx);
                        }

                        HealthToAdd -= this.getPainReduction();
                        if (HealthToAdd > this.stats.get(CharacterStat.PAIN)) {
                            this.stats.add(CharacterStat.PAIN, (HealthToAdd - this.stats.get(CharacterStat.PAIN)) / 500.0F);
                        } else {
                            this.stats.set(CharacterStat.PAIN, HealthToAdd);
                        }
                    }

                    this.setPainReduction(this.getPainReduction() - 0.005F * GameTime.getInstance().getMultiplier());
                    if (this.getPainReduction() < 0.0F) {
                        this.setPainReduction(0.0F);
                    }

                    if (this.isInfected()) {
                        int Mortality = SandboxOptions.instance.lore.mortality.getValue();
                        if (Mortality == 1) {
                            this.ReduceGeneralHealth(110.0F);
                            LuaEventManager.triggerEvent("OnPlayerGetDamage", this.parentChar, "INFECTION", 110);
                            this.stats.set(CharacterStat.ZOMBIE_INFECTION, CharacterStat.ZOMBIE_INFECTION.getMaximumValue());
                        } else if (Mortality != 7) {
                            float worldAgeHours = this.getCurrentTimeForInfection();
                            if (this.infectionMortalityDuration < 0.0F) {
                                this.infectionMortalityDuration = this.pickMortalityDuration();
                            }

                            if (this.infectionTime < 0.0F) {
                                this.infectionTime = worldAgeHours;
                            }

                            if (this.infectionTime > worldAgeHours) {
                                this.infectionTime = worldAgeHours;
                            }

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
        float TotalDamage = 0.0F;

        for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
            BodyPart bodyPart = this.getBodyParts().get(i);
            TotalDamage += (100.0F - bodyPart.getHealth()) * BodyPartType.getDamageModifyer(i);
        }

        TotalDamage += this.getDamageFromPills();
        if (TotalDamage > 100.0F) {
            TotalDamage = 100.0F;
        }

        this.setOverallBodyHealth(100.0F - TotalDamage);
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
                float defense = this.parentChar.getCorpseSicknessDefense(rate, false);
                if (defense > 0.0F) {
                    float multiplier = Math.max(0.0F, 1.0F - defense / 100.0F);
                    rate *= multiplier;
                }

                if (this.parentChar.hasTrait(CharacterTrait.RESILIENT)) {
                    rate *= 0.75F;
                } else if (this.parentChar.hasTrait(CharacterTrait.PRONE_TO_ILLNESS)) {
                    rate *= 1.25F;
                }

                this.stats.add(CharacterStat.FOOD_SICKNESS, rate * GameTime.getInstance().getMultiplier());
                this.parentChar.setCorpseSicknessRate(rate);
                return;
            }
        }

        this.parentChar.setCorpseSicknessRate(0.0F);
        if (this.stats.isAtMinimum(CharacterStat.POISON) && this.stats.isAboveMinimum(CharacterStat.FOOD_SICKNESS)) {
            this.stats.remove(CharacterStat.FOOD_SICKNESS, (float)ZomboidGlobals.foodSicknessDecrease * GameTime.getInstance().getMultiplier());
        }
    }

    public float GetBaseCorpseSickness() {
        return getSicknessFromCorpsesRate(FliesSound.instance.getCorpseCount(this.parentChar));
    }

    private void UpdateTemperatureState() {
        float Delta = 0.06F;
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
     * @param DamageModCount the DamageModCount to set
     */
    public void setDamageModCount(int DamageModCount) {
        this.damageModCount = DamageModCount;
    }

    /**
     * @return the InfectionGrowthRate
     */
    public float getInfectionGrowthRate() {
        return this.infectionGrowthRate;
    }

    /**
     * 
     * @param InfectionGrowthRate the InfectionGrowthRate to set
     */
    public void setInfectionGrowthRate(float InfectionGrowthRate) {
        this.infectionGrowthRate = InfectionGrowthRate;
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
     * @param IsFakeInfected the IsFakeInfected to set
     */
    public void setIsFakeInfected(boolean IsFakeInfected) {
        this.isFakeInfected = IsFakeInfected;
        this.getBodyParts().get(0).SetFakeInfected(IsFakeInfected);
    }

    /**
     * @return the OverallBodyHealth
     */
    public float getOverallBodyHealth() {
        return this.overallBodyHealth;
    }

    /**
     * 
     * @param OverallBodyHealth the OverallBodyHealth to set
     */
    public void setOverallBodyHealth(float OverallBodyHealth) {
        this.overallBodyHealth = OverallBodyHealth;
    }

    /**
     * @return the StandardHealthAddition
     */
    public float getStandardHealthAddition() {
        return this.standardHealthAddition;
    }

    /**
     * 
     * @param StandardHealthAddition the StandardHealthAddition to set
     */
    public void setStandardHealthAddition(float StandardHealthAddition) {
        this.standardHealthAddition = StandardHealthAddition;
    }

    /**
     * @return the ReducedHealthAddition
     */
    public float getReducedHealthAddition() {
        return this.reducedHealthAddition;
    }

    /**
     * 
     * @param ReducedHealthAddition the ReducedHealthAddition to set
     */
    public void setReducedHealthAddition(float ReducedHealthAddition) {
        this.reducedHealthAddition = ReducedHealthAddition;
    }

    /**
     * @return the SeverlyReducedHealthAddition
     */
    public float getSeverlyReducedHealthAddition() {
        return this.severlyReducedHealthAddition;
    }

    /**
     * 
     * @param SeverlyReducedHealthAddition the SeverlyReducedHealthAddition to set
     */
    public void setSeverlyReducedHealthAddition(float SeverlyReducedHealthAddition) {
        this.severlyReducedHealthAddition = SeverlyReducedHealthAddition;
    }

    /**
     * @return the SleepingHealthAddition
     */
    public float getSleepingHealthAddition() {
        return this.sleepingHealthAddition;
    }

    /**
     * 
     * @param SleepingHealthAddition the SleepingHealthAddition to set
     */
    public void setSleepingHealthAddition(float SleepingHealthAddition) {
        this.sleepingHealthAddition = SleepingHealthAddition;
    }

    /**
     * @return the HealthFromFood
     */
    public float getHealthFromFood() {
        return this.healthFromFood;
    }

    /**
     * 
     * @param HealthFromFood the HealthFromFood to set
     */
    public void setHealthFromFood(float HealthFromFood) {
        this.healthFromFood = HealthFromFood;
    }

    /**
     * @return the HealthReductionFromSevereBadMoodles
     */
    public float getHealthReductionFromSevereBadMoodles() {
        return this.healthReductionFromSevereBadMoodles;
    }

    /**
     * 
     * @param HealthReductionFromSevereBadMoodles the HealthReductionFromSevereBadMoodles to set
     */
    public void setHealthReductionFromSevereBadMoodles(float HealthReductionFromSevereBadMoodles) {
        this.healthReductionFromSevereBadMoodles = HealthReductionFromSevereBadMoodles;
    }

    /**
     * @return the StandardHealthFromFoodTime
     */
    public int getStandardHealthFromFoodTime() {
        return this.standardHealthFromFoodTime;
    }

    /**
     * 
     * @param StandardHealthFromFoodTime the StandardHealthFromFoodTime to set
     */
    public void setStandardHealthFromFoodTime(int StandardHealthFromFoodTime) {
        this.standardHealthFromFoodTime = StandardHealthFromFoodTime;
    }

    /**
     * @return the HealthFromFoodTimer
     */
    public float getHealthFromFoodTimer() {
        return this.healthFromFoodTimer;
    }

    /**
     * 
     * @param HealthFromFoodTimer the HealthFromFoodTimer to set
     */
    public void setHealthFromFoodTimer(float HealthFromFoodTimer) {
        this.healthFromFoodTimer = HealthFromFoodTimer;
    }

    /**
     * @return the BoredomDecreaseFromReading
     */
    public float getBoredomDecreaseFromReading() {
        return this.boredomDecreaseFromReading;
    }

    /**
     * 
     * @param BoredomDecreaseFromReading the BoredomDecreaseFromReading to set
     */
    public void setBoredomDecreaseFromReading(float BoredomDecreaseFromReading) {
        this.boredomDecreaseFromReading = BoredomDecreaseFromReading;
    }

    /**
     * @return the InitialThumpPain
     */
    public float getInitialThumpPain() {
        return this.initialThumpPain;
    }

    /**
     * 
     * @param InitialThumpPain the InitialThumpPain to set
     */
    public void setInitialThumpPain(float InitialThumpPain) {
        this.initialThumpPain = InitialThumpPain;
    }

    /**
     * @return the InitialScratchPain
     */
    public float getInitialScratchPain() {
        return this.initialScratchPain;
    }

    /**
     * 
     * @param InitialScratchPain the InitialScratchPain to set
     */
    public void setInitialScratchPain(float InitialScratchPain) {
        this.initialScratchPain = InitialScratchPain;
    }

    /**
     * @return the InitialBitePain
     */
    public float getInitialBitePain() {
        return this.initialBitePain;
    }

    /**
     * 
     * @param InitialBitePain the InitialBitePain to set
     */
    public void setInitialBitePain(float InitialBitePain) {
        this.initialBitePain = InitialBitePain;
    }

    /**
     * @return the InitialWoundPain
     */
    public float getInitialWoundPain() {
        return this.initialWoundPain;
    }

    /**
     * 
     * @param InitialWoundPain the InitialWoundPain to set
     */
    public void setInitialWoundPain(float InitialWoundPain) {
        this.initialWoundPain = InitialWoundPain;
    }

    /**
     * @return the ContinualPainIncrease
     */
    public float getContinualPainIncrease() {
        return this.continualPainIncrease;
    }

    /**
     * 
     * @param ContinualPainIncrease the ContinualPainIncrease to set
     */
    public void setContinualPainIncrease(float ContinualPainIncrease) {
        this.continualPainIncrease = ContinualPainIncrease;
    }

    /**
     * @return the PainReductionFromMeds
     */
    public float getPainReductionFromMeds() {
        return this.painReductionFromMeds;
    }

    /**
     * 
     * @param PainReductionFromMeds the PainReductionFromMeds to set
     */
    public void setPainReductionFromMeds(float PainReductionFromMeds) {
        this.painReductionFromMeds = PainReductionFromMeds;
    }

    /**
     * @return the StandardPainReductionWhenWell
     */
    public float getStandardPainReductionWhenWell() {
        return this.standardPainReductionWhenWell;
    }

    /**
     * 
     * @param StandardPainReductionWhenWell the StandardPainReductionWhenWell to set
     */
    public void setStandardPainReductionWhenWell(float StandardPainReductionWhenWell) {
        this.standardPainReductionWhenWell = StandardPainReductionWhenWell;
    }

    /**
     * @return the OldNumZombiesVisible
     */
    public int getOldNumZombiesVisible() {
        return this.oldNumZombiesVisible;
    }

    /**
     * 
     * @param OldNumZombiesVisible the OldNumZombiesVisible to set
     */
    public void setOldNumZombiesVisible(int OldNumZombiesVisible) {
        this.oldNumZombiesVisible = OldNumZombiesVisible;
    }

    public boolean getWasDraggingCorpse() {
        return this.wasDraggingCorpse;
    }

    public void setWasDraggingCorpse(boolean WasDraggingCorpse) {
        this.wasDraggingCorpse = WasDraggingCorpse;
    }

    /**
     * @return the CurrentNumZombiesVisible
     */
    public int getCurrentNumZombiesVisible() {
        return this.currentNumZombiesVisible;
    }

    /**
     * 
     * @param CurrentNumZombiesVisible the CurrentNumZombiesVisible to set
     */
    public void setCurrentNumZombiesVisible(int CurrentNumZombiesVisible) {
        this.currentNumZombiesVisible = CurrentNumZombiesVisible;
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
     * @param PanicIncreaseValue the PanicIncreaseValue to set
     */
    public void setPanicIncreaseValue(float PanicIncreaseValue) {
        if (this.parentChar.hasTrait(CharacterTrait.DESENSITIZED)) {
            this.panicIncreaseValue = 0.0F;
        } else {
            this.panicIncreaseValue = PanicIncreaseValue;
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
     * @param PanicReductionValue the PanicReductionValue to set
     */
    public void setPanicReductionValue(float PanicReductionValue) {
        this.panicReductionValue = PanicReductionValue;
    }

    /**
     * @return the DrunkIncreaseValue
     */
    public float getDrunkIncreaseValue() {
        return this.drunkIncreaseValue;
    }

    /**
     * 
     * @param DrunkIncreaseValue the DrunkIncreaseValue to set
     */
    public void setDrunkIncreaseValue(float DrunkIncreaseValue) {
        this.drunkIncreaseValue = DrunkIncreaseValue;
    }

    /**
     * @return the DrunkReductionValue
     */
    public float getDrunkReductionValue() {
        return this.drunkReductionValue;
    }

    /**
     * 
     * @param DrunkReductionValue the DrunkReductionValue to set
     */
    public void setDrunkReductionValue(float DrunkReductionValue) {
        this.drunkReductionValue = DrunkReductionValue;
    }

    /**
     * @return the IsOnFire
     */
    public boolean isIsOnFire() {
        return this.isOnFire;
    }

    /**
     * 
     * @param IsOnFire the IsOnFire to set
     */
    public void setIsOnFire(boolean IsOnFire) {
        this.isOnFire = IsOnFire;
    }

    /**
     * @return the BurntToDeath
     */
    public boolean isBurntToDeath() {
        return this.burntToDeath;
    }

    /**
     * 
     * @param BurntToDeath the BurntToDeath to set
     */
    public void setBurntToDeath(boolean BurntToDeath) {
        this.burntToDeath = BurntToDeath;
    }

    /**
     * @return the CatchACold
     */
    public float getCatchACold() {
        return this.catchACold;
    }

    /**
     * 
     * @param CatchACold the CatchACold to set
     */
    public void setCatchACold(float CatchACold) {
        this.catchACold = CatchACold;
    }

    /**
     * @return the HasACold
     */
    public boolean isHasACold() {
        return this.hasACold;
    }

    /**
     * 
     * @param HasACold the HasACold to set
     */
    public void setHasACold(boolean HasACold) {
        this.hasACold = HasACold;
    }

    /**
     * 
     * @param ColdStrength the ColdStrength to set
     */
    public void setColdStrength(float ColdStrength) {
        this.coldStrength = ColdStrength;
    }

    /**
     * @return the ColdProgressionRate
     */
    public float getColdProgressionRate() {
        return this.coldProgressionRate;
    }

    /**
     * 
     * @param ColdProgressionRate the ColdProgressionRate to set
     */
    public void setColdProgressionRate(float ColdProgressionRate) {
        this.coldProgressionRate = ColdProgressionRate;
    }

    public float getTimeToSneezeOrCough() {
        return this.timeToSneezeOrCough;
    }

    public void setTimeToSneezeOrCough(float TimeToSneezeOrCough) {
        this.timeToSneezeOrCough = TimeToSneezeOrCough;
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
     * @param MildColdSneezeTimerMin the MildColdSneezeTimerMin to set
     */
    public void setMildColdSneezeTimerMin(int MildColdSneezeTimerMin) {
        this.mildColdSneezeTimerMin = MildColdSneezeTimerMin;
    }

    /**
     * @return the MildColdSneezeTimerMax
     */
    public int getMildColdSneezeTimerMax() {
        return this.mildColdSneezeTimerMax;
    }

    /**
     * 
     * @param MildColdSneezeTimerMax the MildColdSneezeTimerMax to set
     */
    public void setMildColdSneezeTimerMax(int MildColdSneezeTimerMax) {
        this.mildColdSneezeTimerMax = MildColdSneezeTimerMax;
    }

    /**
     * @return the ColdSneezeTimerMin
     */
    public int getColdSneezeTimerMin() {
        return this.coldSneezeTimerMin;
    }

    /**
     * 
     * @param ColdSneezeTimerMin the ColdSneezeTimerMin to set
     */
    public void setColdSneezeTimerMin(int ColdSneezeTimerMin) {
        this.coldSneezeTimerMin = ColdSneezeTimerMin;
    }

    /**
     * @return the ColdSneezeTimerMax
     */
    public int getColdSneezeTimerMax() {
        return this.coldSneezeTimerMax;
    }

    /**
     * 
     * @param ColdSneezeTimerMax the ColdSneezeTimerMax to set
     */
    public void setColdSneezeTimerMax(int ColdSneezeTimerMax) {
        this.coldSneezeTimerMax = ColdSneezeTimerMax;
    }

    /**
     * @return the NastyColdSneezeTimerMin
     */
    public int getNastyColdSneezeTimerMin() {
        return this.nastyColdSneezeTimerMin;
    }

    /**
     * 
     * @param NastyColdSneezeTimerMin the NastyColdSneezeTimerMin to set
     */
    public void setNastyColdSneezeTimerMin(int NastyColdSneezeTimerMin) {
        this.nastyColdSneezeTimerMin = NastyColdSneezeTimerMin;
    }

    /**
     * @return the NastyColdSneezeTimerMax
     */
    public int getNastyColdSneezeTimerMax() {
        return this.nastyColdSneezeTimerMax;
    }

    /**
     * 
     * @param NastyColdSneezeTimerMax the NastyColdSneezeTimerMax to set
     */
    public void setNastyColdSneezeTimerMax(int NastyColdSneezeTimerMax) {
        this.nastyColdSneezeTimerMax = NastyColdSneezeTimerMax;
    }

    /**
     * @return the SneezeCoughActive
     */
    public int getSneezeCoughActive() {
        return this.sneezeCoughActive;
    }

    /**
     * 
     * @param SneezeCoughActive the SneezeCoughActive to set
     */
    public void setSneezeCoughActive(int SneezeCoughActive) {
        this.sneezeCoughActive = SneezeCoughActive;
    }

    /**
     * @return the SneezeCoughTime
     */
    public int getSneezeCoughTime() {
        return this.sneezeCoughTime;
    }

    /**
     * 
     * @param SneezeCoughTime the SneezeCoughTime to set
     */
    public void setSneezeCoughTime(int SneezeCoughTime) {
        this.sneezeCoughTime = SneezeCoughTime;
    }

    /**
     * @return the SneezeCoughDelay
     */
    public int getSneezeCoughDelay() {
        return this.sneezeCoughDelay;
    }

    /**
     * 
     * @param SneezeCoughDelay the SneezeCoughDelay to set
     */
    public void setSneezeCoughDelay(int SneezeCoughDelay) {
        this.sneezeCoughDelay = SneezeCoughDelay;
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
        float Damage = wielder.calcDamage();
        String dotSide = this.parentChar.testDotSide(wielder);
        boolean isBehind = dotSide.equals("BEHIND");
        this.parentChar.setHitFromBehind(isBehind);
        if (!GameClient.client) {
            int PainType = 1;
            boolean doDamage = true;
            int PartIndex = Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.MAX));
            boolean blade = true;
            boolean bullet = false;
            BodyPart part = this.getBodyPart(BodyPartType.FromIndex(PartIndex));
            float def = this.parentChar.getBodyPartClothingDefense(part.getIndex(), true, false);
            if (Rand.Next(100) < def) {
                doDamage = false;
                this.parentChar.addHoleFromZombieAttacks(BloodBodyPartType.FromIndex(PartIndex), false);
            }

            if (doDamage) {
                this.parentChar.addHole(BloodBodyPartType.FromIndex(PartIndex));
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

                if (PartIndex == BodyPartType.ToIndex(BodyPartType.Head)) {
                    Damage *= 4.0F;
                }

                if (PartIndex == BodyPartType.ToIndex(BodyPartType.Neck)) {
                    Damage *= 4.0F;
                }

                if (PartIndex == BodyPartType.ToIndex(BodyPartType.Torso_Upper)) {
                    Damage *= 2.0F;
                }

                this.AddDamage(PartIndex, Damage);
                switch (1) {
                    case 0:
                        this.stats.add(CharacterStat.PAIN, this.getInitialThumpPain() * BodyPartType.getPainModifyer(PartIndex));
                        break;
                    case 1:
                        this.stats.add(CharacterStat.PAIN, this.getInitialScratchPain() * BodyPartType.getPainModifyer(PartIndex));
                        break;
                    case 2:
                        this.stats.add(CharacterStat.PAIN, this.getInitialBitePain() * BodyPartType.getPainModifyer(PartIndex));
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
                    spikedPart = this.parentChar.bodyPartIsSpikedBehind(PartIndex);
                } else {
                    spikedPart = this.parentChar.bodyPartIsSpiked(PartIndex);
                }

                if (spikedPart) {
                    this.parentChar.addBlood(BloodBodyPartType.FromIndex(PartIndex), true, false, false);
                    wielder.spikePart(BodyPartType.Head);
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
        float discomfortMod = 0.0F;
        discomfortMod += bedMod;
        discomfortMod += clothingMod;
        discomfortMod += draggingCorpseMod;
        discomfortMod += hypoMod;
        discomfortMod += hyperMod;
        discomfortMod += wetMod;
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

        if (this.parentChar.getMoodles().getMoodleLevel(MoodleType.UNCOMFORTABLE) >= 1) {
            float discomfortMalus = discomfortMod > 1.0F ? 1.0F + discomfortMod % 1.0F * 3.0F : 1.0F;
            float discomfortx = this.stats.get(CharacterStat.DISCOMFORT);
            if (this.stats.get(CharacterStat.UNHAPPINESS) < 100.0F && discomfortx > 0.0F) {
                this.stats
                    .add(
                        CharacterStat.UNHAPPINESS,
                        (float)(ZomboidGlobals.unhappinessIncrease / 2.0 * (discomfortx * discomfortMalus / 100.0F) * GameTime.instance.getMultiplier())
                    );
            }
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
