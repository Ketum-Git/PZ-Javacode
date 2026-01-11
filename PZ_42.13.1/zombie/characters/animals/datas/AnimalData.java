// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.datas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.behavior.BehaviorAction;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.Fluid;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.SpawnRegions;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.util.PZCalendar;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class AnimalData {
    public IsoAnimal parent;
    public IsoPlayer attachedPlayer;
    private IsoObject attachedTree;
    private int attachedTreeX = Integer.MAX_VALUE;
    private int attachedTreeY = Integer.MAX_VALUE;
    public AnimalBreed breed;
    public float milkQty;
    public float woolQty;
    public boolean canHaveMilk;
    public float weight = 720.0F;
    private float size = 1.0F;
    private float originalSize = 1.0F;
    private int age;
    private int currentStageNbr;
    public int lastHourCheck = -1;
    public AnimalGrowStage currentStage;
    public boolean pregnant;
    public int pregnantTime;
    private final IsoAnimal femaleToCheck = null;
    public ArrayList<IsoAnimal> animalToInseminate = new ArrayList<>();
    private final int timerInseminate = 0;
    public float maxMilkActual;
    public boolean goingToMomTest;
    public boolean goingToMom;
    public float goingToMomTimer;
    private final ArrayList<SpawnRegions.Point> linkedTrough = new ArrayList<>();
    public boolean eatingGrass;
    public int eggsToday;
    public long eggTime;
    public boolean fertilized;
    public int fertilizedTime;
    private final int hutchToEnterTimerFailsafe = 0;
    public HashMap<String, AnimalGene> maleGenome = new HashMap<>();
    private int hutchPosition = -1;
    private int preferredHutchPosition = -1;
    private final int eggLayTime = -1;
    private final int hutchPathTimer = -1;
    private int troughPathTimer = -1;
    public IsoFeedingTrough troughToCheck;
    private final boolean goingToInseminate = false;
    public long lastMilkTimer;
    public long lastPregnancyTime;
    public static final long ONE_WEEK_MILLISECONDS = 604800000L;
    public static final long ONE_DAY_MILLISECONDS = 86400000L;
    public static final long ONE_HOUR_MILLISECONDS = 3600000L;
    public static final int FEATHER_CHANCE_PER_HOUR = 1;
    private final long timeToLoseMilk = 604800000L;
    public int lastImpregnateTime;
    public int clutchSize;
    public boolean clutchSizeDone;
    public int enterHutchTimerAfterDestroy;

    public AnimalData(IsoAnimal parent, AnimalBreed breed) {
        this.parent = parent;
        if (breed == null && parent.adef != null) {
            breed = parent.adef.breeds.get(Rand.Next(0, parent.adef.breeds.size() + 1));
        }

        this.breed = breed;
        if (parent.adef != null && parent.adef.female) {
            parent.getDescriptor().setFemale(true);
        } else if (parent.adef != null && parent.adef.male) {
            parent.getDescriptor().setFemale(false);
        } else {
            parent.getDescriptor().setFemale(Rand.NextBool(2));
        }
    }

    public void checkStages() {
        ArrayList<AnimalGrowStage> list = this.getGrowStage();
        AnimalGrowStage stage = null;
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                AnimalGrowStage stageCheck = list.get(i);
                if (stageCheck.stage.equals(this.parent.getAnimalType()) && this.getDaysSurvived() >= stageCheck.getAgeToGrow(this.parent)) {
                    stage = stageCheck;
                    break;
                }
            }
        }

        if (stage != null && stage.nextStage != null) {
            this.grow(this.parent.getDescriptor().isFemale() ? stage.nextStage : stage.nextStageMale);
        }
    }

    public void update() {
        boolean hourGrow = false;
        if (GameTime.getInstance().getHour() != this.lastHourCheck) {
            this.lastHourCheck = GameTime.getInstance().getHour();
            this.parent.setHoursSurvived(this.parent.getHoursSurvived() + 1.0);
            hourGrow = true;
        }

        boolean growUp = false;
        if (this.getAge() < this.getDaysSurvived()) {
            float mod = this.getAgeGrowModifier();
            this.setAge(Float.valueOf(this.getDaysSurvived() + (mod - 1.0F)).intValue());
            this.parent.setHoursSurvived(this.getAge() * 24);
            growUp = true;
        }

        if (hourGrow) {
            this.hourGrow(false);
        }

        if (growUp) {
            this.growUp(false);
        }

        this.checkStages();
        this.checkPregnancy();
        if (this.eggsToday < this.parent.adef.eggsPerDay && this.eggTime == 0L) {
            this.eggTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 43200);
        }
    }

    public void callToTrough(IsoFeedingTrough trough) {
        if (this.parent.isExistInTheWorld() && trough != null && trough.getSquare() != null) {
            this.parent.stopAllMovementNow();
            this.parent.faceThisObject(trough);
            if (this.troughPathTimer <= -1) {
                this.troughPathTimer = Rand.Next(100, 400);
            }

            this.troughToCheck = trough;
        }
    }

    private void checkPregnancy() {
        if (this.pregnant && this.pregnantTime > 0) {
            if (this.parent.stressLevel > 80.0F && Rand.NextBool(50)) {
                DebugLog.DetailedInfo.trace("Animal " + this.parent.getFullName() + " lose baby due to stress");
                this.pregnant = false;
                this.pregnantTime = 0;
            }

            if (this.pregnantTime >= this.getPregnantPeriod()) {
                DebugLog.DetailedInfo.trace("Pregnancy done for " + this.parent.getFullName());
                this.pregnant = false;
                this.pregnantTime = 0;
                int babyNbr = Rand.Next(this.parent.adef.minBaby, this.parent.adef.maxBaby + 1);
                float healthDelta = this.parent.getHealth();
                if (healthDelta < 0.5) {
                    DebugLog.Animal.trace("Mother health was too low, reducing nb of babies");
                    babyNbr *= (int)healthDelta;
                }

                DebugLog.Animal.trace("Should pop " + babyNbr + " babies (" + this.parent.adef.minBaby + "-" + this.parent.adef.maxBaby + ")");

                for (int i = 0; i < babyNbr; i++) {
                    this.parent.addBaby();
                }
            }
        }
    }

    public float getAgeGrowModifier() {
        return switch (SandboxOptions.getInstance().animalAgeModifier.getValue()) {
            case 1 -> 90.0F;
            case 2 -> 10.0F;
            case 3 -> 5.0F;
            default -> 1.0F;
            case 5 -> 1.0F;
            case 6 -> 1.0F;
        };
    }

    public void growUp(boolean meta) {
        this.eggsToday = 0;
        if (this.pregnant) {
            this.pregnantTime++;
            if (this.parent.adef.udder) {
                this.setCanHaveMilk(true);
            }
        }

        float sizeGrow = (this.getMaxSize() - this.getMinSize()) / this.currentStage.getAgeToGrow(this.parent);
        float weightGrow = (this.getMaxWeight() - this.getMinWeight()) / this.currentStage.getAgeToGrow(this.parent);
        if (this.parent.smallEnclosure) {
            sizeGrow /= 8.0F;
            weightGrow /= 8.0F;
        }

        this.setSize(Math.min(this.getMaxSize(), this.getSize() + sizeGrow * this.parent.getHealth()));
        this.setWeight(Math.min(this.getMaxWeight(), this.getWeight() + weightGrow * this.parent.getHealth()));
        this.checkPoop(meta, false);
        this.animalToInseminate.clear();
    }

    public InventoryItem checkPoop(boolean meta, boolean bForce) {
        if (!bForce) {
            if (Rand.Next(100) > 60) {
                return null;
            }

            if (this.parent.isWild()) {
                return null;
            }

            if (this.parent.getDZone() == null) {
                return null;
            }

            if (StringUtils.isNullOrEmpty(this.parent.adef.dung) || this.parent.getHutch() != null) {
                return null;
            }

            if (Rand.Next(100) > this.parent.adef.dungChancePerDay) {
                return null;
            }
        }

        int totalNbOfDungs = 0;

        for (DesignationZoneAnimal zone : this.parent.getConnectedDZone()) {
            totalNbOfDungs += zone.getNbOfDung();
        }

        if (!bForce && totalNbOfDungs > this.parent.getDZone().getFullZoneSize() / 2) {
            return null;
        } else {
            float baseSize = 1.0F;
            if (this.parent.isBaby()) {
                baseSize = 0.3F;
            }

            baseSize *= this.getSize() / this.getMaxSize();
            InventoryItem dung = InventoryItemFactory.CreateItem(this.parent.adef.dung);
            dung.setWeight(dung.getWeight() * baseSize);
            dung.setActualWeight(dung.getWeight());
            dung.setCustomWeight(true);
            IsoGridSquare sq = null;
            if (meta) {
                for (int check = 1000; check > 0 && (sq == null || !sq.isFree(false)); check--) {
                    sq = this.parent.getRandomSquareInZone();
                }
            }

            if (sq == null && this.parent.getSquare() != null) {
                sq = this.parent.getSquare();
            }

            if (sq != null) {
                sq.AddWorldInventoryItem(dung, 0.0F, 0.0F, 0.0F, true);
            }

            return dung;
        }
    }

    public InventoryItem dropFeather(boolean meta) {
        if (this.parent.isWild()) {
            return null;
        } else if (this.parent.getDZone() == null) {
            return null;
        } else if (!StringUtils.isNullOrEmpty(this.parent.getBreed().featherItem) && this.parent.getHutch() == null) {
            int totalNbOfFeather = 0;

            for (DesignationZoneAnimal zone : this.parent.getConnectedDZone()) {
                totalNbOfFeather += zone.getNbOfFeather();
            }

            if (totalNbOfFeather > this.parent.getDZone().getFullZoneSize() / 2) {
                return null;
            } else if (Rand.Next(100) >= 1) {
                return null;
            } else {
                InventoryItem feather = InventoryItemFactory.CreateItem(this.parent.getBreed().featherItem);
                IsoGridSquare sq = null;
                if (meta) {
                    for (int check = 1000; check > 0 && (sq == null || !sq.isFree(false)); check--) {
                        sq = this.parent.getRandomSquareInZone();
                    }
                }

                if (sq == null && this.parent.getSquare() != null) {
                    sq = this.parent.getSquare();
                }

                if (sq != null) {
                    sq.AddWorldInventoryItem(feather, 0.0F, 0.0F, 0.0F, true);
                }

                return feather;
            }
        } else {
            return null;
        }
    }

    public void updateHungerAndThirst(boolean fromMeta) {
        if (!this.parent.isWild()) {
            float mod = 1.0F;
            if (this.parent.hutch != null || this.parent.isAnimalSitting()) {
                mod = 0.5F;
            }

            if (!this.parent.isInvincible()) {
                float hungerRed = this.getHungerReduction();
                float thirstRed = this.getThirstReduction();
                if (ServerOptions.getInstance().ultraSpeedDoesnotAffectToAnimals.getValue() && GameTime.getInstance().getMultiplier() > 40.0F) {
                    hungerRed /= GameTime.getInstance().getMultiplier() / 100.0F;
                    thirstRed /= GameTime.getInstance().getMultiplier() / 100.0F;
                }

                if (fromMeta) {
                    hungerRed *= this.getHungerReductionMetaMod();
                    thirstRed *= this.getHungerReductionMetaMod();
                }

                this.parent.getStats().add(CharacterStat.HUNGER, hungerRed * mod);
                this.parent.getStats().add(CharacterStat.THIRST, thirstRed * mod);
            }
        }
    }

    public boolean reduceHealthDueToMilk() {
        return !this.canHaveMilk ? false : this.getMilkQuantity() / this.getMaxMilk() > 1.15F;
    }

    public void updateHealth() {
        if (!this.parent.isWild()) {
            boolean increaseHealth = true;
            if (this.parent.hutch != null && this.parent.hutch.getHutchDirt() > 40.0F) {
                increaseHealth = false;
            }

            if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.8F || this.parent.getStats().get(CharacterStat.THIRST) > 0.8F) {
                increaseHealth = false;
                this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(3.0F));
            }

            if (this.parent.isGeriatric()) {
                increaseHealth = false;
                this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(30.0F));
            }

            if (!this.reduceHealthDueToMilk() && this.parent.getHealth() < 1.0F && increaseHealth) {
                this.parent.setHealth(Math.min(1.0F, this.parent.getHealth() + this.getHealthLoss(3.0F)));
            }
        }
    }

    public void hourGrow(boolean meta) {
        this.parent.ignoredTrough.clear();
        if (this.lastImpregnateTime > 0) {
            this.lastImpregnateTime--;
        }

        this.updateHungerAndThirst(meta);
        this.dropFeather(meta);
        this.updateHealth();
        if (meta) {
            this.eatAndDrinkAfterMeta();
        }

        this.updateMilk();
        if (this.getBreed().woolType != null && this.getMaxWool() > 0.0F) {
            this.setWoolQuantity(Math.min(this.getMaxWool(), this.woolQty + this.getWoolInc()));
        }

        this.findFemaleToInseminate(null);
        if (meta && this.animalToInseminate != null && !this.animalToInseminate.isEmpty()) {
            for (int i = 0; i < this.animalToInseminate.size(); i++) {
                this.animalToInseminate.get(i).fertilize(this.parent, false);
            }

            this.animalToInseminate.clear();
        }

        this.parent.smallEnclosure = false;
        if (this.parent.getDZone() != null && this.parent.getDZone().getFullZoneSize() < this.parent.adef.minEnclosureSize) {
            this.parent.smallEnclosure = true;
        }

        if (!meta) {
            this.checkEggs(GameTime.getInstance().getCalender(), false);
        }

        this.checkFertilizedTime();
        this.checkOld();
        this.updateWeight();
    }

    private void updateWeight() {
        if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.8) {
            this.setWeight(this.getWeight() - this.getWeight() / 800.0F);
        }
    }

    private void updateMilk() {
        if (this.parent.adef.udder) {
            if (this.canHaveMilk) {
                this.setMilkQuantity(this.milkQty + this.getMilkInc());
                if (!this.isPregnant() && GameTime.getInstance().getCalender().getTimeInMillis() - this.lastMilkTimer > 604800000L) {
                    this.canHaveMilk = false;
                }
            } else {
                this.setMilkQuantity(this.milkQty - this.getMilkInc() / 2.0F);
            }
        }

        if (this.reduceHealthDueToMilk()) {
            this.parent.setHealth(this.parent.getHealth() - this.getHealthLoss(10.0F) * (this.milkQty / this.getMaxMilk()));
        }
    }

    private void checkOld() {
        if (!this.parent.isBaby() && this.getMaxAgeGeriatric() > 0.0F && this.getGeriatricPercentage() >= 0.95F) {
            this.parent.setHealth(this.parent.getHealth() - 0.1F);
        }
    }

    public float getHealthLoss(Float divide) {
        if (divide == null) {
            divide = 1.0F;
        }

        AnimalAllele resAllele = this.parent.getUsedGene("resistance");
        float base = this.parent.adef.healthLossMultiplier;
        if (resAllele == null) {
            return base;
        } else {
            float alleleValue = 1.5F - resAllele.currentValue;
            float dbgDivide = 1.0F;
            if (ServerOptions.getInstance().ultraSpeedDoesnotAffectToAnimals.getValue() && GameTime.instance.getMultiplier() > 50.0F) {
                dbgDivide = Math.abs(1000.0F - GameTime.instance.getMultiplier()) / 1000.0F;
            }

            return base * alleleValue / divide * dbgDivide;
        }
    }

    public float getMaxMilk() {
        AnimalAllele milkAllele = this.parent.getUsedGene("maxMilk");
        return milkAllele == null ? this.parent.adef.maxMilk : this.parent.adef.maxMilk * milkAllele.currentValue;
    }

    public float getMaxMilkActual() {
        return this.maxMilkActual;
    }

    public void setMaxMilkActual(float maxMilkActual) {
        this.maxMilkActual = maxMilkActual;
    }

    public float getMaxWool() {
        if (this.parent.adef.maxWool <= 0.0F) {
            return 0.0F;
        } else {
            AnimalAllele allele = this.parent.getUsedGene("maxWool");
            return allele == null ? this.parent.adef.maxWool : this.parent.adef.maxWool * allele.currentValue;
        }
    }

    public float getMinMilk() {
        AnimalAllele milkAllele = this.parent.getUsedGene("maxMilk");
        return milkAllele == null ? this.parent.adef.minMilk : this.parent.adef.minMilk * milkAllele.currentValue;
    }

    public float getMilkInc() {
        AnimalAllele milkAllele = this.parent.getUsedGene("milkInc");
        float inc = this.maxMilkActual / 20.0F;
        if (milkAllele == null) {
            return inc;
        } else {
            inc *= milkAllele.currentValue;
            float stressMod = 1.0F;
            if (this.parent.stressLevel > 40.0F) {
                stressMod = 40.0F / this.parent.stressLevel;
            }

            float disorderMod = 1.0F;
            if (this.parent.geneticDisorder.contains("poormilk")) {
                disorderMod = 0.2F;
            }

            return inc * this.getMilkIncModifier() * stressMod * disorderMod;
        }
    }

    public float getWoolInc() {
        AnimalAllele woolAllele = this.parent.getUsedGene("woolInc");
        float inc = this.getMaxWool() / 2400.0F;
        if (woolAllele == null) {
            return inc;
        } else {
            inc *= woolAllele.currentValue;
            float stressMod = 1.0F;
            if (this.parent.stressLevel > 40.0F) {
                stressMod = 40.0F / this.parent.stressLevel;
            }

            float disorderMod = 1.0F;
            if (this.parent.geneticDisorder.contains("poorwool")) {
                disorderMod = 0.2F;
            }

            return inc * this.getWoolIncModifier() * stressMod * disorderMod;
        }
    }

    private int calcClutchSize() {
        float mod = 1.0F;
        AnimalAllele clutchAllele = this.parent.getUsedGene("eggClutch");
        if (clutchAllele != null) {
            mod = clutchAllele.currentValue;
        }

        int nbOfEggs = Rand.Next(Float.valueOf(this.parent.adef.minClutchSize * mod).intValue(), Float.valueOf(this.parent.adef.maxClutchSize * mod).intValue());
        return Float.valueOf(this.parent.adef.maxClutchSize * mod).intValue();
    }

    public void checkEggs(PZCalendar realCal, boolean meta) {
        if (!this.parent.isWild()) {
            if (!this.parent.isDead()) {
                if (this.eggsToday < this.parent.adef.eggsPerDay && this.eggTime == 0L && meta) {
                    this.eggTime = Long.valueOf(realCal.getTimeInMillis() / 1000L).intValue() - 10;
                }

                if (this.parent.adef.eggsPerDay > 0) {
                    if (this.haveLayingEggPeriod() && !this.isInLayingEggPeriod(realCal)) {
                        this.clutchSizeDone = false;
                        return;
                    }

                    if (this.haveLayingEggPeriod()
                        && !this.clutchSizeDone
                        && this.clutchSize == 0
                        && this.parent.adef.minClutchSize > 0
                        && this.isInLayingEggPeriod(realCal)
                        && SandboxOptions.getInstance().animalMatingSeason.getValue()) {
                        this.clutchSize = this.calcClutchSize();
                        this.clutchSizeDone = true;
                    }

                    if (this.haveLayingEggPeriod()
                        && this.clutchSize == 0
                        && this.parent.adef.minClutchSize > 0
                        && SandboxOptions.getInstance().animalMatingSeason.getValue()) {
                        return;
                    }

                    if (this.eggTime > 0L && realCal.getTimeInMillis() / 1000L > this.eggTime) {
                        if (this.parent.adef.hutches != null
                            && this.getRegionHutch() != null
                            && !this.getRegionHutch().isDoorClosed()
                            && this.parent.hutch == null
                            && this.getRegionHutch().haveRoomForNewEggs()) {
                            if (meta) {
                                if (!this.getRegionHutch().addMetaEgg(this.parent)) {
                                    this.parent.addEgg(true);
                                }

                                this.eggsToday++;
                                this.eggTime = 0L;
                            } else {
                                this.parent.getBehavior().callToHutch(null, true);
                            }
                        } else if (this.parent.addEgg(false)) {
                            this.eggsToday++;
                            this.eggTime = 0L;
                        }
                    }
                }
            }
        }
    }

    public void checkFertilizedTime() {
        if (this.fertilized) {
            this.fertilizedTime++;
        }

        if (this.fertilizedTime > this.parent.adef.fertilizedTimeMax) {
            this.fertilized = false;
            this.fertilizedTime = 0;
        }
    }

    private float getMilkIncModifier() {
        return switch (SandboxOptions.getInstance().animalMilkIncModifier.getValue()) {
            case 1 -> 30.0F;
            case 2 -> 5.0F;
            case 3 -> 2.5F;
            default -> 1.0F;
            case 5 -> 0.7F;
            case 6 -> 0.2F;
        };
    }

    private float getWoolIncModifier() {
        return switch (SandboxOptions.getInstance().animalWoolIncModifier.getValue()) {
            case 1 -> 30.0F;
            case 2 -> 5.0F;
            case 3 -> 2.5F;
            default -> 1.0F;
            case 5 -> 0.7F;
            case 6 -> 0.2F;
        };
    }

    public int getPregnantPeriod() {
        int result = this.parent.adef.pregnantPeriod;

        float modifier = switch (SandboxOptions.getInstance().animalPregnancyTime.getValue()) {
            case 1 -> 0.01F;
            case 2 -> 0.2F;
            case 3 -> 0.7F;
            default -> 1.0F;
            case 5 -> 2.0F;
            case 6 -> 3.0F;
        };
        result = Float.valueOf(result * modifier).intValue();
        if (result < 8) {
            result = 8;
        }

        return result;
    }

    private float getThirstReduction() {
        AnimalAllele allele = this.parent.getUsedGene("thirstResistance");
        float base = this.parent.adef.thirstMultiplier;
        if (allele == null) {
            return base;
        } else {
            float mod = 1.0F - allele.currentValue + 1.0F;
            if (this.parent.geneticDisorder.contains("highthirst")) {
                base *= 10.0F;
            }

            return base * (mod * this.getHungerReductionMod());
        }
    }

    private float getHungerReduction() {
        AnimalAllele allele = this.parent.getUsedGene("hungerResistance");
        float base = this.parent.adef.hungerMultiplier;
        if (allele == null) {
            return base;
        } else {
            float mod = 1.0F - allele.currentValue + 1.0F;
            if (this.parent.geneticDisorder.contains("gluttonous")) {
                base *= 10.0F;
            }

            return base * (mod * this.getHungerReductionMod());
        }
    }

    private float getHungerReductionMetaMod() {
        return switch (SandboxOptions.getInstance().animalMetaStatsModifier.getValue()) {
            case 1 -> 4.0F;
            case 2 -> 2.0F;
            case 3 -> 1.5F;
            default -> 1.0F;
            case 5 -> 0.7F;
            case 6 -> 0.2F;
        };
    }

    private float getHungerReductionMod() {
        return switch (SandboxOptions.getInstance().animalStatsModifier.getValue()) {
            case 1 -> 4.0F;
            case 2 -> 2.0F;
            case 3 -> 1.5F;
            default -> 1.0F;
            case 5 -> 0.7F;
            case 6 -> 0.2F;
        };
    }

    private void eatAndDrinkAfterMetaVehicle() {
        if (!this.parent.isBaby()) {
            while (this.parent.getStats().get(CharacterStat.HUNGER) >= 0.1F) {
                boolean found = false;
                if (this.parent.getVehicle() != null && this.eatFromVehicle()) {
                    found = true;
                }

                if (!found) {
                    return;
                }
            }
        }
    }

    private void eatAndDrinkAfterMeta() {
        if (this.parent.getVehicle() != null) {
            this.eatAndDrinkAfterMetaVehicle();
        } else {
            while (this.parent.getStats().get(CharacterStat.HUNGER) >= 0.1F) {
                if (this.parent.isBaby()
                    && this.parent.adef.eatFromMother
                    && this.parent.mother != null
                    && this.parent.mother.isExistInTheWorld()
                    && this.parent.mother.getData().getMilkQuantity() > 0.1) {
                    this.parent.getStats().remove(CharacterStat.HUNGER, 0.2F);
                    this.parent.mother.getData().setMilkQuantity(this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.1F, 0.3F));
                } else {
                    boolean found = false;

                    for (int k = 0; k < this.parent.getConnectedDZone().size(); k++) {
                        DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(k);
                        if (zone.troughs.isEmpty() && zone.foodOnGround.isEmpty()) {
                            break;
                        }

                        for (int i = 0; i < zone.foodOnGround.size(); i++) {
                            IsoWorldInventoryObject food = zone.foodOnGround.get(i);
                            if (this.parent.adef.eatTypeTrough != null) {
                                for (int j = 0; j < this.parent.adef.eatTypeTrough.size(); j++) {
                                    String type = this.parent.adef.eatTypeTrough.get(j);
                                    if (food.getItem() instanceof Food) {
                                        if (type.equals(((Food)food.getItem()).getFoodType()) || type.equals(food.getItem().getAnimalFeedType())) {
                                            this.parent.eatFromGround = food;
                                            break;
                                        }
                                    } else if (food.getItem() instanceof DrainableComboItem && type.equals(food.getItem().getAnimalFeedType())) {
                                        this.parent.eatFromGround = food;
                                        break;
                                    }
                                }

                                if (this.parent.eatFromGround != null) {
                                    this.eat();
                                    found = true;
                                }
                                break;
                            }
                        }

                        if (!zone.troughs.isEmpty()) {
                            for (int ix = 0; ix < zone.troughs.size(); ix++) {
                                IsoFeedingTrough trough = zone.troughs.get(ix);
                                if (this.canEatFromTrough(trough) != null) {
                                    this.parent.eatFromTrough = trough;
                                    this.eat();
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found) {
                        break;
                    }
                }
            }

            while (this.parent.getStats().get(CharacterStat.THIRST) >= 0.1F) {
                boolean found = false;
                int k = 0;

                while (true) {
                    if (k < this.parent.getConnectedDZone().size()) {
                        DesignationZoneAnimal zonex = this.parent.getConnectedDZone().get(k);
                        if (!zonex.nearWaterSquares.isEmpty()) {
                            this.parent.drinkFromRiver = zonex.nearWaterSquares.get(0);
                            this.drink();
                            found = true;
                        } else if (IsoPuddles.getInstance().getPuddlesSize() > 0.13F) {
                            this.parent.drinkFromPuddle = this.parent.getSquare();
                            this.drink();
                            found = true;
                        } else {
                            for (int ixx = 0; ixx < zonex.troughs.size(); ixx++) {
                                IsoFeedingTrough trough = zonex.troughs.get(ixx);
                                if (trough.getWater() > 0.0F) {
                                    this.parent.drinkFromTrough = trough;
                                    this.drink();
                                    found = true;
                                    break;
                                }
                            }

                            if (found) {
                                k++;
                                continue;
                            }
                        }
                    }

                    if (!found) {
                        return;
                    }
                    break;
                }
            }
        }
    }

    private boolean eatFromVehicle() {
        BaseVehicle vehicle = this.parent.getVehicle();
        VehiclePart foodCont = vehicle.getPartById("TrailerAnimalFood");
        InventoryItem edibleItem = null;
        if (foodCont != null && foodCont.getItemContainer() != null) {
            for (int i = 0; i < foodCont.getItemContainer().getItems().size(); i++) {
                InventoryItem item = foodCont.getItemContainer().getItems().get(i);
                if (this.parent.adef.eatTypeTrough != null) {
                    for (int k = 0; k < this.parent.adef.eatTypeTrough.size(); k++) {
                        String type = this.parent.adef.eatTypeTrough.get(k);
                        if (item instanceof Food food) {
                            if (type.equals(food.getFoodType()) || type.equals(item.getAnimalFeedType())) {
                                edibleItem = item;
                                break;
                            }
                        } else if (item instanceof DrainableComboItem && type.equals(item.getAnimalFeedType())) {
                            edibleItem = item;
                            break;
                        }
                    }
                }

                if (edibleItem != null) {
                    break;
                }
            }

            if (edibleItem != null) {
                this.eatItem(edibleItem, false);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Deprecated
    public ArrayList<IsoFeedingTrough> getRandomTroughList() {
        ArrayList<IsoFeedingTrough> result = new ArrayList<>();

        for (int i = 0; i < this.parent.getConnectedDZone().size(); i++) {
            DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(i);
            result.addAll(zone.troughs);
        }

        shuffleList(result);
        return result;
    }

    public static void shuffleList(ArrayList<IsoFeedingTrough> a) {
        int n = a.size();
        Random random = new Random();
        random.nextInt();

        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swap(a, i, change);
        }
    }

    private static void swap(List<IsoFeedingTrough> a, int i, int change) {
        IsoFeedingTrough helper = a.get(i);
        a.set(i, a.get(change));
        a.set(change, helper);
    }

    public void resetEatingCheck() {
        this.parent.drinkFromPuddle = null;
        this.parent.drinkFromRiver = null;
        this.parent.drinkFromTrough = null;
        this.parent.eatFromTrough = null;
        this.parent.eatFromGround = null;
        this.parent.movingToFood = null;
        this.eatingGrass = false;
        this.parent.clearVariable("idleAction");
    }

    public void eatFood(InventoryItem item) {
        if (this.parent.isExistInTheWorld() && item != null) {
            if (item instanceof Food food) {
                float hungReduce = this.parent.getStats().get(CharacterStat.HUNGER);
                float hungOnFood = food.getHungerChange();
                if (Math.abs(hungOnFood) <= hungReduce) {
                    this.parent.getStats().add(CharacterStat.HUNGER, food.getHungerChange());
                } else {
                    float hungLeft = Math.abs(hungOnFood) - hungReduce;
                    this.parent.getStats().reset(CharacterStat.HUNGER);
                    food.setHungChange(-hungLeft);
                }

                this.parent.eatFromGround = null;
            }
        }
    }

    private InventoryItem canEatFromTrough(IsoFeedingTrough trough) {
        if (this.parent.adef.eatTypeTrough != null && trough.getContainer() != null) {
            for (int i = 0; i < trough.getContainer().getItems().size(); i++) {
                InventoryItem item = trough.getContainer().getItems().get(i);
                if (!(item instanceof Food food && food.isRotten())) {
                    if (this.parent.adef.eatTypeTrough.contains("All")
                        || this.parent.adef.eatTypeTrough.contains(item.getFullType())
                        || this.parent.adef.eatTypeTrough.contains(item.getAnimalFeedType())) {
                        return item;
                    }

                    if (item instanceof Food foodx && this.parent.adef.eatTypeTrough.contains(foodx.getFoodType())) {
                        return item;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public void drinkFromGround() {
        this.parent.eatFromGround.useFluid(2.0F / this.parent.adef.thirstBoost);
        this.parent.getStats().remove(CharacterStat.THIRST, 0.2F * this.parent.adef.thirstBoost);
        if (!(this.parent.getStats().get(CharacterStat.THIRST) < 0.1F) && this.parent.eatFromGround.hasFluid()) {
            if (this.parent.getStats().get(CharacterStat.THIRST) > 0.1F) {
                this.parent.setVariable("idleAction", "eat");
                if (this.parent.adef.eatingTypeNbr > 0) {
                    this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                }
            }
        } else {
            this.resetEatingCheck();
            this.parent.setStateEventDelayTimer(0.0F);
        }
    }

    private void drinkFromRiver() {
        this.parent.getStats().remove(CharacterStat.THIRST, 0.2F * this.parent.adef.thirstBoost);
        if (this.parent.getStats().get(CharacterStat.THIRST) < 0.1F) {
            this.resetEatingCheck();
            this.parent.setStateEventDelayTimer(0.0F);
        } else {
            if (this.parent.getStats().get(CharacterStat.THIRST) > 0.1F) {
                this.parent.setVariable("idleAction", "eat");
                if (this.parent.adef.eatingTypeNbr > 0) {
                    this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                }
            }
        }
    }

    private void drinkFromPuddle() {
        this.parent.getStats().remove(CharacterStat.THIRST, 0.2F * this.parent.adef.thirstBoost);
        if (this.parent.getStats().get(CharacterStat.THIRST) < 0.1F) {
            this.resetEatingCheck();
            this.parent.setStateEventDelayTimer(0.0F);
        } else {
            if (this.parent.getStats().get(CharacterStat.THIRST) > 0.1F) {
                this.parent.setVariable("idleAction", "eat");
                if (this.parent.adef.eatingTypeNbr > 0) {
                    this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                }
            }
        }
    }

    public void drink() {
        if (this.parent.drinkFromRiver != null) {
            this.drinkFromRiver();
        } else if (this.parent.drinkFromPuddle != null) {
            this.drinkFromPuddle();
        } else if (this.parent.drinkFromTrough != null && !(this.parent.drinkFromTrough.getWater() <= 0.0F)) {
            this.parent.drinkFromTrough.removeWater(Rand.Next(0.4F, 0.6F) / this.parent.adef.thirstBoost);
            this.parent.getStats().remove(CharacterStat.THIRST, 0.2F * this.parent.adef.thirstBoost);
            SGlobalObjects.OnIsoObjectChangedItself("feedingTrough", this.parent.drinkFromTrough);
            if (!(this.parent.getStats().get(CharacterStat.THIRST) < 0.1F) && !(this.parent.drinkFromTrough.getWater() <= 0.0F)) {
                if (this.parent.getStats().get(CharacterStat.THIRST) > 0.1F) {
                    this.parent.setVariable("idleAction", "eat");
                    if (this.parent.adef.eatingTypeNbr > 0) {
                        this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                    }
                }
            } else {
                this.resetEatingCheck();
                this.parent.setStateEventDelayTimer(0.0F);
            }
        }
    }

    public void eatItem(InventoryItem item, boolean onground) {
        IsoFeedingTrough trough = null;
        if (item.getContainer() != null && item.getContainer().parent instanceof IsoFeedingTrough isoFeedingTrough) {
            trough = isoFeedingTrough;
        }

        if (item.getFluidContainer() != null
            && (item.getFluidContainer().isPureFluid(Fluid.Get(this.parent.getBreed().getMilkType())) || item.getFluidContainer().isPureFluid(Fluid.AnimalMilk))
            )
         {
            while (
                !item.getFluidContainer().isEmpty()
                    && (!(this.parent.getStats().get(CharacterStat.HUNGER) <= 0.0F) || !(this.parent.getStats().get(CharacterStat.THIRST) <= 0.0F))
            ) {
                this.parent.getStats().remove(CharacterStat.HUNGER, 0.2F);
                this.parent.getStats().remove(CharacterStat.THIRST, 0.2F);
                item.getFluidContainer().removeFluid(Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost));
            }
        }

        boolean isItemRemoved = false;
        if (item instanceof Food food) {
            float hungReduce = this.parent.getStats().get(CharacterStat.HUNGER);
            float hungOnFood = food.getHungerChange();
            if (Math.abs(hungOnFood * this.parent.adef.hungerBoost) <= hungReduce) {
                this.parent.getStats().set(CharacterStat.HUNGER, this.parent.getStats().get(CharacterStat.HUNGER) + hungOnFood * this.parent.adef.hungerBoost);
                this.parent.getStats().add(CharacterStat.THIRST, food.getThirstChange());
                if (onground) {
                    item.getWorldItem().getSquare().removeWorldObject(item.getWorldItem());

                    for (int j = 0; j < this.parent.getConnectedDZone().size(); j++) {
                        DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(j);
                        zone.foodOnGround.remove(item.getWorldItem());
                        if (IsoPlayer.getInstance() != null) {
                            IsoPlayer.getInstance().setInvPageDirty(true);
                        }
                    }
                } else if (item.getContainer() != null) {
                    if (item.getContainer().getParent() != null
                        && item.getContainer().getParent() instanceof IsoPlayer player
                        && player.getPrimaryHandItem() == item) {
                        player.setPrimaryHandItem(null);
                    }

                    if (item.getReplaceOnUse() != null) {
                        InventoryItem newItem = item.getContainer().AddItem(item.getReplaceOnUse());

                        assert newItem != null;

                        newItem.copyConditionStatesFrom(item);
                        if (GameServer.server) {
                            GameServer.sendAddItemToContainer(item.getContainer(), newItem);
                        }
                    }

                    if (GameServer.server) {
                        isItemRemoved = true;
                        GameServer.sendRemoveItemFromContainer(item.getContainer(), item);
                    }

                    item.getContainer().Remove(item);
                    if (item.getContainer() != null) {
                        item.getContainer().setDrawDirty(true);
                    }
                }
            } else {
                float hungLeft = Math.abs(hungOnFood * this.parent.adef.hungerBoost) - hungReduce;
                float thirstLeft = Math.abs(food.getThirstChange()) - this.parent.getStats().get(CharacterStat.THIRST) / this.parent.adef.thirstBoost;
                this.parent.getStats().reset(CharacterStat.HUNGER);
                this.parent.getStats().add(CharacterStat.THIRST, food.getThirstChange() * this.parent.adef.thirstBoost);
                food.setHungChange(-(hungLeft / this.parent.adef.hungerBoost));
                if (Math.abs(food.getThirstChange()) > 0.0F) {
                    if (thirstLeft > 0.0F) {
                        food.setThirstChange(-thirstLeft);
                    } else {
                        food.setThirstChange(0.0F);
                    }
                }

                if (food.getHungChange() > -0.01 && item.getContainer() != null) {
                    if (item.getContainer().getParent() != null
                        && item.getContainer().getParent() instanceof IsoPlayer player
                        && player.getPrimaryHandItem() == item) {
                        player.setPrimaryHandItem(null);
                    }

                    if (item.getReplaceOnUse() != null) {
                        InventoryItem newItemx = item.getContainer().AddItem(item.getReplaceOnUse());

                        assert newItemx != null;

                        newItemx.copyConditionStatesFrom(item);
                        if (GameServer.server) {
                            GameServer.sendAddItemToContainer(item.getContainer(), newItemx);
                        }
                    }

                    if (GameServer.server) {
                        isItemRemoved = true;
                        GameServer.sendRemoveItemFromContainer(item.getContainer(), item);
                    }

                    item.getContainer().Remove(item);
                    if (item.getContainer() != null) {
                        item.getContainer().setDrawDirty(true);
                    }
                }

                if (IsoPlayer.getInstance() != null) {
                    IsoPlayer.getInstance().setInvPageDirty(true);
                }
            }
        }

        if (item instanceof DrainableComboItem foodx) {
            float hungReduce = this.parent.getStats().get(CharacterStat.HUNGER);
            float hungOnFood = foodx.getCurrentUses() * 0.1F;
            if (Math.abs(hungOnFood) <= hungReduce) {
                this.parent.getStats().remove(CharacterStat.HUNGER, hungOnFood);
                int usingDelta = foodx.getCurrentUses();

                for (int i = 0; i < usingDelta; i++) {
                    foodx.Use();
                    this.parent.getStats().remove(CharacterStat.HUNGER, 0.1F);
                }

                if (onground) {
                    for (int jx = 0; jx < this.parent.getConnectedDZone().size(); jx++) {
                        DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(jx);
                        zone.foodOnGround.remove(item.getWorldItem());
                    }
                } else if (item.getContainer() != null) {
                    if (item.getContainer().getParent() != null
                        && item.getContainer().getParent() instanceof IsoPlayer player
                        && player.getPrimaryHandItem() == item) {
                        player.setPrimaryHandItem(null);
                    }

                    if (foodx.getReplaceOnDeplete() != null) {
                        InventoryItem newItemxx = item.getContainer().AddItem(foodx.getReplaceOnDeplete());

                        assert newItemxx != null;

                        newItemxx.copyConditionStatesFrom(item);
                        if (GameServer.server) {
                            GameServer.sendAddItemToContainer(item.getContainer(), newItemxx);
                        }
                    }

                    if (GameServer.server) {
                        isItemRemoved = true;
                        GameServer.sendRemoveItemFromContainer(item.getContainer(), item);
                    }

                    item.getContainer().Remove(item);
                }
            } else {
                float usingDelta = Math.round(hungReduce * 10.0F);

                for (int i = 0; i < usingDelta; i++) {
                    foodx.Use();
                }

                this.parent.getStats().reset(CharacterStat.HUNGER);
            }

            if (IsoPlayer.getInstance() != null) {
                IsoPlayer.getInstance().setInvPageDirty(true);
            }
        }

        if (trough != null) {
            trough.getContainer().setDrawDirty(true);
            trough.checkOverlayAfterAnimalEat();
        }

        if (!isItemRemoved && GameServer.server) {
            item.syncItemFields();
        }
    }

    public void eat() {
        if (this.parent.eatFromGround != null) {
            if (!this.parent.eatFromGround.isExistInTheWorld()) {
                this.parent.eatFromGround = null;
            } else {
                this.eatItem(this.parent.eatFromGround.getItem(), true);
                this.resetEatingCheck();
                if (this.parent.getStats().get(CharacterStat.HUNGER) >= this.parent.adef.thirstHungerTrigger) {
                    this.parent.getBehavior().setDoingBehavior(false);
                    this.parent.getBehavior().checkBehavior();
                } else {
                    this.parent.getBehavior().setDoingBehavior(false);
                    this.parent.getBehavior().resetBehaviorAction();
                    this.parent.setStateEventDelayTimer(0.0F);
                }
            }
        } else if (this.parent.eatFromTrough != null) {
            InventoryItem item = this.canEatFromTrough(this.parent.eatFromTrough);
            if (item != null) {
                this.eatItem(item, false);
                if (this.parent.getStats().get(CharacterStat.HUNGER) < 0.1F) {
                    this.resetEatingCheck();
                    this.parent.setStateEventDelayTimer(0.0F);
                } else {
                    if (this.parent.getStats().get(CharacterStat.HUNGER) > this.parent.adef.thirstHungerTrigger) {
                        this.parent.setVariable("idleAction", "eat");
                        if (this.parent.adef.eatingTypeNbr > 0) {
                            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                        }
                    }

                    this.parent.setVariable("idleAction", "eat");
                    if (this.parent.adef.eatingTypeNbr > 0) {
                        this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
                    }
                }
            } else {
                this.resetEatingCheck();
                this.parent.setStateEventDelayTimer(0.0F);
            }
        } else if (this.parent.mother != null
            && this.parent.mother.getCurrentSquare() != null
            && this.parent.getCurrentSquare().DistToProper(this.parent.mother.getCurrentSquare()) <= 2.0F
            && this.parent.mother.haveEnoughMilkToFeedFrom()) {
            this.parent.getStats().remove(CharacterStat.HUNGER, 0.2F);
            this.parent.getStats().remove(CharacterStat.THIRST, 0.2F);
            this.parent
                .mother
                .getData()
                .setMilkQuantity(
                    this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost)
                );
            if (!(this.parent.getStats().get(CharacterStat.HUNGER) < 0.1F)
                && !(this.parent.getStats().get(CharacterStat.THIRST) < 0.1F)
                && this.parent.mother.haveEnoughMilkToFeedFrom()) {
                if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.05 && this.parent.mother.haveEnoughMilkToFeedFrom()) {
                    this.parent.setVariable("idleAction", "eat");
                    this.parent.setVariable("eatingAnim", "feed");
                }
            } else {
                this.resetEatingCheck();
                this.parent.setStateEventDelayTimer(0.0F);
                this.parent.mother.getBehavior().blockMovement = false;
            }
        } else {
            if (this.eatingGrass) {
                this.parent.getStats().remove(CharacterStat.HUNGER, 0.15F);
                if (this.parent.getCurrentSquare() != null) {
                    if (GameClient.client && this.parent.isLocal()) {
                        INetworkPacket.send(PacketTypes.PacketType.AnimalEvent, this.parent, this.parent.getCurrentSquare());
                    } else {
                        this.parent.getCurrentSquare().removeGrass();
                    }
                }

                if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.0F) {
                    this.parent.getBehavior().setDoingBehavior(false);
                    this.parent.getBehavior().checkBehavior();
                }

                this.resetEatingCheck();
            }
        }
    }

    public boolean canBePregnant() {
        if (!this.isFemale()) {
            return false;
        } else if (this.getTimeBeforeNextPregnancy() > 0
            && this.lastPregnancyTime > 0L
            && GameTime.getInstance().getCalender().getTimeInMillis() - this.lastPregnancyTime < 86400000L * this.getTimeBeforeNextPregnancy()) {
            return false;
        } else if (this.parent.isGeriatric()) {
            return false;
        } else {
            return this.parent.isInMatingSeason()
                    && this.parent.adef.eggsPerDay == 0
                    && !StringUtils.isNullOrEmpty(this.parent.adef.babyType)
                    && this.parent.getCurrentSquare() != null
                    && this.getDaysSurvived() >= this.parent.getMinAgeForBaby()
                    && !this.isPregnant()
                ? true
                : this.parent.isInMatingSeason()
                    && this.parent.adef.eggsPerDay > 0
                    && this.fertilizedTime == 0
                    && !StringUtils.isNullOrEmpty(this.parent.adef.babyType)
                    && this.parent.getCurrentSquare() != null
                    && this.getDaysSurvived() >= this.parent.getMinAgeForBaby()
                    && !this.fertilized;
        }
    }

    public void tryInseminateInMeta(PZCalendar realCal) {
        if (!this.parent.isFemale()) {
            this.findFemaleToInseminate(realCal);
            if (!this.animalToInseminate.isEmpty()) {
                IsoAnimal female = this.animalToInseminate.get(Rand.Next(0, this.animalToInseminate.size()));
                if (female != null) {
                    female.fertilize(this.parent, false);
                }
            }
        }
    }

    public void findFemaleToInseminate(PZCalendar realCal) {
        if (realCal == null) {
            realCal = GameTime.instance.getCalender();
        }

        if (this.parent.getCurrentSquare() != null) {
            if (this.animalToInseminate.isEmpty()
                && !this.parent.isFemale()
                && !StringUtils.isNullOrEmpty(this.parent.adef.mate)
                && this.getDaysSurvived() >= this.parent.getMinAgeForBaby()
                && this.getLastImpregnatePeriod(realCal) == 0) {
                for (int j = 0; j < this.parent.getConnectedDZone().size(); j++) {
                    DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(j);
                    ArrayList<IsoAnimal> animals = zone.getAnimals();

                    for (int i = 0; i < animals.size(); i++) {
                        IsoAnimal animal = animals.get(i);
                        if (animal != null && animal != this.parent && this.parent.adef.mate.equals(animal.getAnimalType()) && animal.getData().canBePregnant()
                            )
                         {
                            this.animalToInseminate.add(animal);
                        }
                    }
                }
            }
        }
    }

    public void initSize() {
        this.setSize(this.getMinSize());
        float sizeGrow = (this.getMaxSize() - this.getMinSize()) / this.currentStage.getAgeToGrow(this.parent);
        this.setSize(Math.max(this.getMinSize(), Math.min(this.getMaxSize(), this.getSize() + sizeGrow * (this.getAge() - this.parent.adef.minAge))));
        this.setSize(0.1F);
    }

    public void initWeight() {
        this.setWeight(this.getMinWeight());
        float weightGrow = (this.getMaxWeight() - this.getMinWeight()) / this.currentStage.getAgeToGrow(this.parent);
        this.setWeight(Math.max(this.getMinWeight(), Math.min(this.getMaxWeight(), this.getWeight() + weightGrow * (this.getAge() - this.parent.adef.minAge))));
    }

    public void initStage() {
        if (this.currentStage == null) {
            ArrayList<AnimalGrowStage> list = this.getGrowStage();
            if (list != null && !list.isEmpty()) {
                for (int i = 0; i < list.size(); i++) {
                    AnimalGrowStage stageCheck = list.get(i);
                    if (stageCheck.stage.equals(this.parent.getAnimalType())) {
                        this.currentStage = stageCheck;
                        break;
                    }
                }
            }
        }
    }

    public void grow(String newtype) {
        if (this.parent.mother != null && this.parent.mother.getBabies() != null) {
            this.parent.mother.getBabies().remove(this.parent);
        }

        IsoAnimal newAnimal = new IsoAnimal(this.parent.getCell(), this.parent.getXi(), this.parent.getYi(), this.parent.getZi(), newtype, this.breed);
        newAnimal.getData().setAge(this.getAge());
        newAnimal.setHoursSurvived(this.getAge() * 24);
        newAnimal.getData().currentStageNbr = this.currentStageNbr + 1;
        newAnimal.getData().setAttachedPlayer(this.attachedPlayer);
        newAnimal.getData().setAttachedTree(this.attachedTree);
        newAnimal.getStats().set(CharacterStat.HUNGER, this.parent.getStats().get(CharacterStat.HUNGER));
        newAnimal.getStats().set(CharacterStat.THIRST, this.parent.getStats().get(CharacterStat.THIRST));
        newAnimal.playerAcceptanceList = this.parent.playerAcceptanceList;
        newAnimal.stressLevel = this.parent.stressLevel;
        newAnimal.fullGenome = this.parent.fullGenome;
        AnimalGene.checkGeneticDisorder(newAnimal);
        newAnimal.getData().initSize();
        newAnimal.setCustomName(this.parent.getCustomName());
        newAnimal.setFemale(this.parent.isFemale());
        newAnimal.setVehicle(this.parent.getVehicle());
        newAnimal.setAnimalID(this.parent.getAnimalID());
        if (this.parent.checkForChickenpocalypse()) {
            newAnimal.delete();
        } else {
            float sizeDelta = (this.size - this.getMinSize()) / (this.getMaxSize() - this.getMinSize());
            if (sizeDelta > 0.7) {
                sizeDelta -= 0.7F;
                newAnimal.getData().setSize(newAnimal.getData().getSize() * (1.0F + sizeDelta));
            }

            newAnimal.setIsInvincible(this.parent.isInvincible());
            if (this.parent.getVehicle() == null) {
                if (this.parent.getContainer() != null) {
                    ArrayList<InventoryItem> animals = this.parent.getContainer().getAllType("Animal");

                    for (int i = 0; i < animals.size(); i++) {
                        AnimalInventoryItem invAnimal = (AnimalInventoryItem)animals.get(i);
                        if (invAnimal.getAnimal() == this.parent) {
                            this.parent.getContainer().Remove(invAnimal);
                            this.parent.delete();
                            IsoCell.getInstance().addToProcessItemsRemove(invAnimal);
                            if (GameServer.server) {
                                GameServer.sendRemoveItemFromContainer(this.parent.getContainer(), invAnimal);
                            }
                            break;
                        }
                    }
                } else {
                    this.parent.delete();
                    newAnimal.addToWorld();
                    if (GameServer.server) {
                        AnimalInstanceManager.getInstance().add(newAnimal, newAnimal.getOnlineID());
                    }
                }
            } else {
                this.parent.getVehicle().replaceGrownAnimalInTrailer(this.parent, newAnimal);
                newAnimal.delete();
            }
        }
    }

    public int getDaysSurvived() {
        float hours = 0.0F;
        hours = Math.max(hours, (float)this.parent.getHoursSurvived());
        return (int)hours / 24;
    }

    public boolean canHaveBaby() {
        return this.isFemale() && this.parent.adef.babyType != null && this.getDaysSurvived() >= this.parent.getMinAgeForBaby();
    }

    public void init() {
        this.initStage();
        this.initSize();
        this.initWeight();
        this.lastHourCheck = GameTime.getInstance().getHour();
    }

    public void setAttachedPlayer(IsoPlayer chr) {
        if (chr != null && (Core.getInstance().animalCheat || chr.getInventory().getFirstType("Rope") != null)) {
            chr.setPrimaryHandItem(chr.getInventory().getFirstType("Rope"));
        }

        this.attachedPlayer = chr;
    }

    public IsoPlayer getAttachedPlayer() {
        return this.attachedPlayer;
    }

    public void setAttachedTree(IsoObject tree) {
        this.attachedTree = tree;
        if (tree != null && tree.getSquare() != null) {
            this.attachedTreeX = tree.getSquare().getX();
            this.attachedTreeY = tree.getSquare().getY();
        } else {
            this.attachedTreeX = this.attachedTreeY = Integer.MAX_VALUE;
        }
    }

    public IsoObject getAttachedTree() {
        return this.attachedTree;
    }

    public int getAttachedTreeX() {
        return this.attachedTreeX;
    }

    public int getAttachedTreeY() {
        return this.attachedTreeY;
    }

    public AnimalBreed getBreed() {
        return this.breed;
    }

    public void setBreed(AnimalBreed breed) {
        this.breed = breed;
    }

    public float getMilkQuantity() {
        return this.milkQty;
    }

    public void setMilkQuantity(float milkQty) {
        if (milkQty < this.milkQty) {
            if (this.maxMilkActual < this.getMaxMilk()) {
                this.maxMilkActual = this.maxMilkActual + Rand.Next(0.01F, 0.03F);
                this.maxMilkActual = Math.min(this.maxMilkActual, this.getMaxMilk());
            }

            if (this.canHaveMilk) {
                this.updateLastTimeMilked();
            }
        }

        this.milkQty = Math.min(Math.max(milkQty, 0.0F), this.maxMilkActual);
    }

    public void setSize(float size) {
        size = PZMath.clamp(size, this.getMinSize(), this.getMaxSize());
        this.size = size;
    }

    public void setSizeForced(float size) {
        this.originalSize = this.size;
        this.size = size;
    }

    public float getSize() {
        return this.size;
    }

    public float getOriginalSize() {
        return this.originalSize;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getAge() {
        return this.age;
    }

    public ArrayList<AnimalGrowStage> getGrowStage() {
        return this.parent.adef.stages;
    }

    public float getWeight() {
        return this.weight;
    }

    public boolean isFemale() {
        return this.parent.getDescriptor().isFemale();
    }

    public String getAgeString(IsoGameCharacter chr) {
        return "Adult";
    }

    public boolean canHaveMilk() {
        return this.canHaveMilk;
    }

    public void setCanHaveMilk(boolean canHaveMilk) {
        if (this.getGeriatricPercentage() > 0.8) {
            canHaveMilk = false;
        }

        if (this.parent.geneticDisorder.contains("nomilk")) {
            canHaveMilk = false;
        }

        if (!this.canHaveMilk && canHaveMilk) {
            this.updateLastTimeMilked();
        }

        if (canHaveMilk && this.maxMilkActual == 0.0F) {
            this.maxMilkActual = this.getMinMilk();
        }

        this.canHaveMilk = canHaveMilk;
    }

    public void setPregnant(boolean pregnant) {
        this.pregnant = pregnant;
    }

    public boolean isPregnant() {
        return this.pregnant;
    }

    public int getPregnancyTime() {
        return this.pregnantTime;
    }

    public void setPregnancyTime(int period) {
        this.pregnantTime = period;
    }

    public boolean isFertilized() {
        return this.fertilized;
    }

    public void setFertilized(boolean b) {
        this.fertilized = b;
    }

    public int getFertilizedTime() {
        return this.fertilizedTime;
    }

    public int setFertilizedTime(int period) {
        return this.fertilizedTime = period;
    }

    public float getWoolQuantity() {
        return this.woolQty;
    }

    public void setMaleGenome(HashMap<String, AnimalGene> maleGenome) {
        this.maleGenome = maleGenome;
    }

    public void setWoolQuantity(float woolQty, boolean force) {
        if (force || this.parent.getAge() >= 200) {
            if (!force && this.parent.geneticDisorder.contains("nowool")) {
                woolQty = 0.0F;
            }

            float lastQty = this.woolQty;
            boolean wasFleece = lastQty >= this.getMaxWool() / 2.0F;
            this.woolQty = Math.min(woolQty, this.getMaxWool());
            boolean fleece = this.woolQty >= this.getMaxWool() / 2.0F;
            if (wasFleece != fleece) {
                this.parent.clearVariable("idleAction");
                this.parent.getPathFindBehavior2().reset();
                this.parent.resetModel();
            }
        }
    }

    public void setWoolQuantity(float woolQty) {
        this.setWoolQuantity(woolQty, false);
    }

    public IsoHutch getRegionHutch() {
        for (int k = 0; k < this.parent.getConnectedDZone().size(); k++) {
            DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(k);
            if (!zone.getHutchsConnected().isEmpty()) {
                for (int i = 0; i < zone.getHutchsConnected().size(); i++) {
                    IsoHutch testHutch = zone.getHutchsConnected().get(i);

                    for (int j = 0; j < this.parent.adef.hutches.size(); j++) {
                        String testType = this.parent.adef.hutches.get(j);
                        if (testType.equals(testHutch.type)) {
                            return testHutch;
                        }
                    }
                }
            }
        }

        return null;
    }

    public float getGeriatricPercentage() {
        if (this.getAge() < this.getMaxAgeGeriatric() / 10.0F * 7.0F) {
            return 0.0F;
        } else {
            float diff = this.getAge() - this.getMaxAgeGeriatric() / 10.0F * 7.0F;
            float total = this.getMaxAgeGeriatric() - this.getMaxAgeGeriatric() / 10.0F * 7.0F;
            return diff > total ? 1.0F : diff / total;
        }
    }

    public float getMaxAgeGeriatric() {
        int base = this.parent.adef.maxAgeGeriatric;
        AnimalAllele allele = this.parent.getUsedGene("lifeExpectancy");
        float aValue = 1.0F;
        if (allele != null) {
            aValue = allele.currentValue;
        }

        if (this.parent.geneticDisorder.contains("poorlife")) {
            base /= 3;
        }

        if (this.parent.geneticDisorder.contains("dwarf")) {
            base /= 2;
        }

        float modifier = 0.25F - aValue / 4.0F + 1.0F;
        return (int)(base * modifier);
    }

    public float getMinSize() {
        float base = this.parent.adef.minSize;
        AnimalAllele allele = this.parent.getUsedGene("maxSize");
        if (allele == null) {
            return base;
        } else {
            if (this.parent.geneticDisorder.contains("dwarf")) {
                if (this.parent.isBaby()) {
                    base /= 1.5F;
                } else {
                    base /= 2.2F;
                }
            }

            return base * allele.currentValue;
        }
    }

    public float getMaxSize() {
        float base = this.parent.adef.maxSize;
        AnimalAllele allele = this.parent.getUsedGene("maxSize");
        if (allele == null) {
            return base;
        } else {
            if (this.parent.geneticDisorder.contains("dwarf")) {
                if (this.parent.isBaby()) {
                    base /= 1.5F;
                } else {
                    base /= 2.2F;
                }
            }

            return base * allele.currentValue;
        }
    }

    public float getMinWeight() {
        float base = this.parent.adef.minWeight;
        AnimalAllele allele = this.parent.getUsedGene("maxWeight");
        if (allele == null) {
            return base;
        } else {
            if (this.parent.geneticDisorder.contains("skinny")) {
                base /= 3.0F;
            }

            return base * allele.currentValue;
        }
    }

    public float getMaxWeight() {
        float base = this.parent.adef.maxWeight;
        AnimalAllele allele = this.parent.getUsedGene("maxWeight");
        if (allele == null) {
            return base;
        } else {
            if (this.parent.geneticDisorder.contains("skinny")) {
                base /= 3.0F;
            }

            return base * allele.currentValue;
        }
    }

    public void setWeight(float weight) {
        weight = Math.min(this.getMaxWeight(), Math.max(this.getMinWeight(), weight));
        this.weight = weight;
    }

    public int getHutchPosition() {
        return this.hutchPosition;
    }

    public void setHutchPosition(int hutchPosition) {
        this.hutchPosition = hutchPosition;
    }

    public int getPreferredHutchPosition() {
        return this.preferredHutchPosition;
    }

    public void setPreferredHutchPosition(int preferredHutchPosition) {
        this.preferredHutchPosition = preferredHutchPosition;
    }

    public int getTimeBeforeNextPregnancy() {
        int timeBeforeNextPregnancy = this.parent.adef.timeBeforeNextPregnancy;
        if (timeBeforeNextPregnancy > 0) {
            float modifier = switch (SandboxOptions.getInstance().animalPregnancyTime.getValue()) {
                case 1 -> 0.01F;
                case 2 -> 0.2F;
                case 3 -> 0.7F;
                default -> 1.0F;
                case 5 -> 2.0F;
                case 6 -> 3.0F;
            };
            timeBeforeNextPregnancy = (int)(timeBeforeNextPregnancy * modifier);
        }

        return timeBeforeNextPregnancy;
    }

    public String getLastPregnancyPeriod() {
        int timeBeforeNextPregnancy = this.getTimeBeforeNextPregnancy();
        if (timeBeforeNextPregnancy > 0 && this.lastPregnancyTime > 0L) {
            int days = Long.valueOf(
                    (this.lastPregnancyTime + timeBeforeNextPregnancy * 86400000L - GameTime.getInstance().getCalender().getTimeInMillis()) / 86400000L
                )
                .intValue();
            return days > 0 ? days + "" : null;
        } else {
            return null;
        }
    }

    public void updateLastPregnancyTime() {
        this.lastPregnancyTime = GameTime.getInstance().getCalender().getTimeInMillis();
    }

    public int getLastImpregnatePeriod(PZCalendar realCal) {
        if (realCal == null) {
            realCal = GameTime.getInstance().getCalender();
        }

        return this.parent.adef.minAgeForBaby != 0 && this.getDaysSurvived() >= this.parent.getMinAgeForBaby() ? this.lastImpregnateTime : -1;
    }

    public Float getLastTimeMilkedInHour() {
        return (float)(GameTime.getInstance().getCalender().getTimeInMillis() - this.lastMilkTimer) / 3600000.0F / 24.0F;
    }

    public void updateLastTimeMilked() {
        this.lastMilkTimer = GameTime.getInstance().getCalender().getTimeInMillis();
    }

    public String getDebugBehaviorString() {
        String result = this.parent.getFullName() + " \r\n \r\n";
        if (this.parent.isAnimalSitting()) {
            result = result + "Animal is sitting. \r\n";
        } else {
            result = result + "Next wander in " + (int)this.parent.getStateEventDelayTimer() + ". \r\n";
        }

        if (this.parent.getBehavior().blockMovement) {
            result = result + "Animal is currently blocked from moving. (" + this.parent.getBehavior().blockedFor + ") \r\n";
        }

        if (this.parent.getBehavior().isDoingBehavior) {
            result = result + "Failcheck Behavior: " + (this.parent.getBehavior().behaviorMaxTime - this.parent.getBehavior().behaviorFailsafe) + "\r\n";
            if (this.parent.getBehavior().behaviorObject != null) {
                if (this.parent.getBehavior().behaviorObject instanceof IsoFeedingTrough isoFeedingTrough) {
                    result = result
                        + "Animal current behavior: "
                        + this.parent.getBehavior().behaviorAction
                        + " at "
                        + isoFeedingTrough.getSquare().getX()
                        + ","
                        + isoFeedingTrough.getSquare().getY()
                        + "\r\n";
                } else if (this.parent.getBehavior().behaviorObject instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
                    result = result
                        + "Animal current behavior: "
                        + this.parent.getBehavior().behaviorAction
                        + " at "
                        + isoWorldInventoryObject.getSquare().getX()
                        + ","
                        + isoWorldInventoryObject.getSquare().getY()
                        + "\r\n";
                } else if (this.parent.getBehavior().behaviorObject instanceof IsoAnimal isoAnimal) {
                    result = result
                        + "Animal current behavior: "
                        + this.parent.getBehavior().behaviorAction
                        + " at "
                        + isoAnimal.getSquare().getX()
                        + ","
                        + isoAnimal.getSquare().getY()
                        + "\r\n";
                } else if (this.parent.getBehavior().behaviorObject instanceof IsoHutch isoHutch) {
                    if (this.parent.getBehavior().hutchPathTimer > -1) {
                        result = result
                            + "Animal is waiting to enter hutch in "
                            + this.parent.getBehavior().hutchPathTimer
                            + " at "
                            + (isoHutch.getSquare().getX() + isoHutch.getEnterSpotX())
                            + ","
                            + (isoHutch.getSquare().getY() + isoHutch.getEnterSpotY())
                            + "\r\n";
                    } else {
                        result = result
                            + "Animal is going to enter hutch at "
                            + (isoHutch.getSquare().getX() + isoHutch.getEnterSpotX())
                            + ","
                            + (isoHutch.getSquare().getY() + isoHutch.getEnterSpotY())
                            + "\r\n";
                    }
                }

                if (this.parent.getBehavior().behaviorObject instanceof IsoGridSquare sq) {
                    if (this.parent.getBehavior().behaviorAction == BehaviorAction.DRINKFROMPUDDLE) {
                        result = result + "Animal is going to drink from puddle at " + sq.x + ", " + sq.y + "\r\n";
                    }

                    if (this.parent.getBehavior().behaviorAction == BehaviorAction.DRINKFROMRIVER) {
                        result = result + "Animal is going to drink from river at " + sq.x + ", " + sq.y + "\r\n";
                    }

                    if (this.parent.getBehavior().behaviorAction == BehaviorAction.EATGRASS) {
                        result = result + "Animal is going to eat grass at " + sq.x + ", " + sq.y + "\r\n";
                    }
                }
            }
        } else {
            result = result + "Next behavior action check in: " + Math.round(this.parent.getBehavior().behaviorCheckTimer) + "\r\n";
        }

        if (this.parent.fightingOpponent != null) {
            result = result + "has a fighter opponent";
            if (this.parent.fightingOpponent instanceof IsoAnimal isoAnimal && isoAnimal.fightingOpponent == null) {
                result = result + " but the other don't know it!";
            }

            result = result + "\r\n";
        }

        if (this.parent.getStats().get(CharacterStat.THIRST) >= 0.9F || this.parent.getStats().get(CharacterStat.HUNGER) >= 0.9F) {
            if (this.parent.getThumpDelay() == 0.0F) {
                result = result + "Animal will try to destroy walls due to hunger/thirst. \r\n";
            } else {
                result = result + "Animal will try to destroy walls due to hunger/thirst in: " + Math.round(this.parent.getThumpDelay()) + " \r\n";
            }
        }

        if (this.goingToMom) {
            result = result + "Pathing to mom. (" + this.goingToMomTimer + ") \r\n";
            if (this.parent.isAnimalEating()) {
                result = result + "Feeding from mom. \r\n";
            }
        }

        if (this.femaleToCheck != null) {
            result = result + "Pathing to fertilize " + this.femaleToCheck.getFullName() + ". \r\n";
        }

        if (this.eatingGrass) {
            result = result + "Try to eat grass. \r\n";
            if (this.parent.isAnimalEating()) {
                result = result + "Eating grass on ground. \r\n";
            }
        }

        if (this.parent.eatFromTrough != null && this.parent.isAnimalEating()) {
            result = result + "Eating food in trough. \r\n";
        }

        if ((this.parent.eatFromGround != null || this.parent.movingToFood != null) && this.parent.isAnimalEating()) {
            result = result + "Eating food on ground. \r\n";
        }

        if (this.parent.drinkFromTrough != null && this.parent.isAnimalEating()) {
            result = result + "Drinking from trough. \r\n";
        }

        if (this.parent.alertedChr != null) {
            result = result + "is alerted (" + this.parent.getBehavior().lastAlerted + ") \r\n";
        }

        if (this.parent.getBehavior().attackAnimalTimer > 0.0F) {
            result = result + "delay before attacking again: " + Math.round(this.parent.getBehavior().attackAnimalTimer) + " \r\n";
        }

        return result + "Current State: " + this.parent.getCurrentState().toString() + " \r\n";
    }

    public boolean isInLayingEggPeriod(PZCalendar cal) {
        return !SandboxOptions.getInstance().animalMatingSeason.getValue()
            ? true
            : this.parent.adef.layEggPeriodStart > -1 && cal.get(2) + 1 == this.parent.adef.layEggPeriodStart;
    }

    public boolean haveLayingEggPeriod() {
        return !SandboxOptions.getInstance().animalMatingSeason.getValue() ? false : this.parent.adef.layEggPeriodStart > -1;
    }

    public int getClutchSize() {
        return this.clutchSize;
    }

    public String getInventoryIconTextureName() {
        if (this.parent.isBaby()) {
            return this.getBreed().invIconBaby;
        } else {
            return this.parent.isFemale() ? this.getBreed().invIconFemale : this.getBreed().invIconMale;
        }
    }
}
