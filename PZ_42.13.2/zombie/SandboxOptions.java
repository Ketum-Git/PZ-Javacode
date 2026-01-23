// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.EnumConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerSettingsManager;
import zombie.sandbox.CustomBooleanSandboxOption;
import zombie.sandbox.CustomDoubleSandboxOption;
import zombie.sandbox.CustomEnumSandboxOption;
import zombie.sandbox.CustomIntegerSandboxOption;
import zombie.sandbox.CustomSandboxOption;
import zombie.sandbox.CustomSandboxOptions;
import zombie.sandbox.CustomStringSandboxOption;

@UsedFromLua
public final class SandboxOptions {
    public static final SandboxOptions instance = new SandboxOptions();
    public static final int FIRST_YEAR = 1993;
    public int speed = 3;
    private final ArrayList<SandboxOptions.SandboxOption> options = new ArrayList<>();
    private final HashMap<String, SandboxOptions.SandboxOption> optionByName = new HashMap<>();
    public final SandboxOptions.EnumSandboxOption zombies = this.newEnumOption("Zombies", 6, 4).setTranslation("ZombieCount");
    public final SandboxOptions.EnumSandboxOption distribution = this.newEnumOption("Distribution", 2, 1).setTranslation("ZombieDistribution");
    public final SandboxOptions.BooleanSandboxOption zombieVoronoiNoise = this.newBooleanOption("ZombieVoronoiNoise", true);
    public final SandboxOptions.EnumSandboxOption zombieRespawn = this.newEnumOption("ZombieRespawn", 4, 2).setTranslation("ZombieRespawn");
    public final SandboxOptions.BooleanSandboxOption zombieMigrate = this.newBooleanOption("ZombieMigrate", true).setTranslation("ZombieMigrate");
    public final SandboxOptions.EnumSandboxOption dayLength = this.newEnumOption("DayLength", 27, 4);
    public final SandboxOptions.EnumSandboxOption startYear = this.newEnumOption("StartYear", 100, 1);
    public final SandboxOptions.EnumSandboxOption startMonth = this.newEnumOption("StartMonth", 12, 7);
    public final SandboxOptions.EnumSandboxOption startDay = this.newEnumOption("StartDay", 31, 23);
    public final SandboxOptions.EnumSandboxOption startTime = this.newEnumOption("StartTime", 9, 2);
    public final SandboxOptions.EnumSandboxOption dayNightCycle = this.newEnumOption("DayNightCycle", 3, 1).setValueTranslation("DayNightCycle");
    public final SandboxOptions.EnumSandboxOption climateCycle = this.newEnumOption("ClimateCycle", 6, 1).setValueTranslation("ClimateCycle");
    public final SandboxOptions.EnumSandboxOption fogCycle = this.newEnumOption("FogCycle", 3, 1).setValueTranslation("FogCycle");
    public final SandboxOptions.EnumSandboxOption waterShut = this.newEnumOption("WaterShut", 9, 2).setValueTranslation("Shutoff");
    public final SandboxOptions.EnumSandboxOption elecShut = this.newEnumOption("ElecShut", 9, 2).setValueTranslation("Shutoff");
    public final SandboxOptions.EnumSandboxOption alarmDecay = this.newEnumOption("AlarmDecay", 6, 2).setValueTranslation("Shutoff");
    public final SandboxOptions.IntegerSandboxOption waterShutModifier = this.newIntegerOption("WaterShutModifier", -1, Integer.MAX_VALUE, 14)
        .setTranslation("WaterShut");
    public final SandboxOptions.IntegerSandboxOption elecShutModifier = this.newIntegerOption("ElecShutModifier", -1, Integer.MAX_VALUE, 14)
        .setTranslation("ElecShut");
    public final SandboxOptions.IntegerSandboxOption alarmDecayModifier = this.newIntegerOption("AlarmDecayModifier", -1, Integer.MAX_VALUE, 14)
        .setTranslation("AlarmDecay");
    public final SandboxOptions.DoubleSandboxOption foodLootNew = this.newDoubleOption("FoodLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption literatureLootNew = this.newDoubleOption("LiteratureLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption medicalLootNew = this.newDoubleOption("MedicalLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption survivalGearsLootNew = this.newDoubleOption("SurvivalGearsLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption cannedFoodLootNew = this.newDoubleOption("CannedFoodLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption weaponLootNew = this.newDoubleOption("WeaponLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption rangedWeaponLootNew = this.newDoubleOption("RangedWeaponLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption ammoLootNew = this.newDoubleOption("AmmoLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption mechanicsLootNew = this.newDoubleOption("MechanicsLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption otherLootNew = this.newDoubleOption("OtherLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption clothingLootNew = this.newDoubleOption("ClothingLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption containerLootNew = this.newDoubleOption("ContainerLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption keyLootNew = this.newDoubleOption("KeyLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption mediaLootNew = this.newDoubleOption("MediaLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption mementoLootNew = this.newDoubleOption("MementoLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption cookwareLootNew = this.newDoubleOption("CookwareLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption materialLootNew = this.newDoubleOption("MaterialLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption farmingLootNew = this.newDoubleOption("FarmingLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption toolLootNew = this.newDoubleOption("ToolLootNew", 0.0, 4.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption rollsMultiplier = this.newDoubleOption("RollsMultiplier", 0.1, 100.0, 1.0);
    public final SandboxOptions.StringSandboxOption lootItemRemovalList = this.newStringOption("LootItemRemovalList", "", -1);
    public final SandboxOptions.BooleanSandboxOption removeStoryLoot = this.newBooleanOption("RemoveStoryLoot", false);
    public final SandboxOptions.BooleanSandboxOption removeZombieLoot = this.newBooleanOption("RemoveZombieLoot", false);
    public final SandboxOptions.IntegerSandboxOption zombiePopLootEffect = this.newIntegerOption("ZombiePopLootEffect", 0, 20, 10)
        .setTranslation("ZombiePopLootEffect");
    public final SandboxOptions.DoubleSandboxOption insaneLootFactor = this.newDoubleOption("InsaneLootFactor", 0.0, 0.2, 0.05);
    public final SandboxOptions.DoubleSandboxOption extremeLootFactor = this.newDoubleOption("ExtremeLootFactor", 0.05, 0.6, 0.2);
    public final SandboxOptions.DoubleSandboxOption rareLootFactor = this.newDoubleOption("RareLootFactor", 0.2, 1.0, 0.6);
    public final SandboxOptions.DoubleSandboxOption normalLootFactor = this.newDoubleOption("NormalLootFactor", 0.6, 2.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption commonLootFactor = this.newDoubleOption("CommonLootFactor", 1.0, 3.0, 2.0);
    public final SandboxOptions.DoubleSandboxOption abundantLootFactor = this.newDoubleOption("AbundantLootFactor", 2.0, 4.0, 3.0);
    public final SandboxOptions.EnumSandboxOption temperature = this.newEnumOption("Temperature", 5, 3).setTranslation("WorldTemperature");
    public final SandboxOptions.EnumSandboxOption rain = this.newEnumOption("Rain", 5, 3).setTranslation("RainAmount");
    public final SandboxOptions.EnumSandboxOption erosionSpeed = this.newEnumOption("ErosionSpeed", 5, 3);
    public final SandboxOptions.IntegerSandboxOption erosionDays = this.newIntegerOption("ErosionDays", -1, 36500, 0);
    public final SandboxOptions.EnumSandboxOption farming = this.newEnumOption("Farming", 5, 3).setTranslation("FarmingSpeed");
    public final SandboxOptions.EnumSandboxOption compostTime = this.newEnumOption("CompostTime", 8, 2);
    public final SandboxOptions.EnumSandboxOption statsDecrease = this.newEnumOption("StatsDecrease", 5, 3).setTranslation("StatDecrease");
    public final SandboxOptions.EnumSandboxOption natureAbundance = this.newEnumOption("NatureAbundance", 5, 3).setTranslation("NatureAmount");
    public final SandboxOptions.EnumSandboxOption alarm = this.newEnumOption("Alarm", 6, 4).setTranslation("HouseAlarmFrequency");
    public final SandboxOptions.EnumSandboxOption lockedHouses = this.newEnumOption("LockedHouses", 6, 4).setTranslation("LockedHouseFrequency");
    public final SandboxOptions.BooleanSandboxOption starterKit = this.newBooleanOption("StarterKit", false);
    public final SandboxOptions.BooleanSandboxOption nutrition = this.newBooleanOption("Nutrition", false);
    public final SandboxOptions.EnumSandboxOption foodRotSpeed = this.newEnumOption("FoodRotSpeed", 5, 3).setTranslation("FoodSpoil");
    public final SandboxOptions.EnumSandboxOption fridgeFactor = this.newEnumOption("FridgeFactor", 6, 3).setTranslation("FridgeEffect");
    public final SandboxOptions.IntegerSandboxOption seenHoursPreventLootRespawn = this.newIntegerOption("SeenHoursPreventLootRespawn", 0, Integer.MAX_VALUE, 0);
    public final SandboxOptions.IntegerSandboxOption hoursForLootRespawn = this.newIntegerOption("HoursForLootRespawn", 0, Integer.MAX_VALUE, 0);
    public final SandboxOptions.IntegerSandboxOption maxItemsForLootRespawn = this.newIntegerOption("MaxItemsForLootRespawn", 0, Integer.MAX_VALUE, 5);
    public final SandboxOptions.BooleanSandboxOption constructionPreventsLootRespawn = this.newBooleanOption("ConstructionPreventsLootRespawn", true);
    public final SandboxOptions.StringSandboxOption worldItemRemovalList = this.newStringOption(
        "WorldItemRemovalList",
        "Base.Hat,Base.Glasses,Base.Dung_Turkey,Base.Dung_Chicken,Base.Dung_Cow,Base.Dung_Deer,Base.Dung_Mouse,Base.Dung_Pig,Base.Dung_Rabbit,Base.Dung_Rat,Base.Dung_Sheep",
        -1
    );
    public final SandboxOptions.DoubleSandboxOption hoursForWorldItemRemoval = this.newDoubleOption("HoursForWorldItemRemoval", 0.0, 2.147483647E9, 24.0);
    public final SandboxOptions.BooleanSandboxOption itemRemovalListBlacklistToggle = this.newBooleanOption("ItemRemovalListBlacklistToggle", false);
    public final SandboxOptions.EnumSandboxOption timeSinceApo = this.newEnumOption("TimeSinceApo", 13, 1);
    public final SandboxOptions.EnumSandboxOption plantResilience = this.newEnumOption("PlantResilience", 5, 3);
    public final SandboxOptions.EnumSandboxOption plantAbundance = this.newEnumOption("PlantAbundance", 5, 3).setValueTranslation("FarmingAmount");
    public final SandboxOptions.EnumSandboxOption endRegen = this.newEnumOption("EndRegen", 5, 3).setTranslation("EnduranceRegen");
    public final SandboxOptions.EnumSandboxOption helicopter = this.newEnumOption("Helicopter", 4, 2).setValueTranslation("HelicopterFreq");
    public final SandboxOptions.EnumSandboxOption metaEvent = this.newEnumOption("MetaEvent", 3, 2).setValueTranslation("MetaEventFreq");
    public final SandboxOptions.EnumSandboxOption sleepingEvent = this.newEnumOption("SleepingEvent", 3, 1).setValueTranslation("MetaEventFreq");
    public final SandboxOptions.DoubleSandboxOption generatorFuelConsumption = this.newDoubleOption("GeneratorFuelConsumption", 0.0, 100.0, 0.1);
    public final SandboxOptions.EnumSandboxOption generatorSpawning = this.newEnumOption("GeneratorSpawning", 7, 5);
    public final SandboxOptions.EnumSandboxOption annotatedMapChance = this.newEnumOption("AnnotatedMapChance", 6, 4);
    public final SandboxOptions.IntegerSandboxOption characterFreePoints = this.newIntegerOption("CharacterFreePoints", -100, 100, 0);
    public final SandboxOptions.EnumSandboxOption constructionBonusPoints = this.newEnumOption("ConstructionBonusPoints", 5, 3);
    public final SandboxOptions.EnumSandboxOption nightDarkness = this.newEnumOption("NightDarkness", 4, 3);
    public final SandboxOptions.EnumSandboxOption nightLength = this.newEnumOption("NightLength", 5, 3);
    public final SandboxOptions.BooleanSandboxOption boneFracture = this.newBooleanOption("BoneFracture", true);
    public final SandboxOptions.EnumSandboxOption injurySeverity = this.newEnumOption("InjurySeverity", 3, 2);
    public final SandboxOptions.DoubleSandboxOption hoursForCorpseRemoval = this.newDoubleOption("HoursForCorpseRemoval", -1.0, 2.147483647E9, -1.0);
    public final SandboxOptions.EnumSandboxOption decayingCorpseHealthImpact = this.newEnumOption("DecayingCorpseHealthImpact", 5, 3);
    public final SandboxOptions.BooleanSandboxOption zombieHealthImpact = this.newBooleanOption("ZombieHealthImpact", false);
    public final SandboxOptions.EnumSandboxOption bloodLevel = this.newEnumOption("BloodLevel", 5, 3);
    public final SandboxOptions.EnumSandboxOption clothingDegradation = this.newEnumOption("ClothingDegradation", 4, 3);
    public final SandboxOptions.BooleanSandboxOption fireSpread = this.newBooleanOption("FireSpread", true);
    public final SandboxOptions.IntegerSandboxOption daysForRottenFoodRemoval = this.newIntegerOption("DaysForRottenFoodRemoval", -1, Integer.MAX_VALUE, -1);
    public final SandboxOptions.BooleanSandboxOption allowExteriorGenerator = this.newBooleanOption("AllowExteriorGenerator", true);
    public final SandboxOptions.EnumSandboxOption maxFogIntensity = this.newEnumOption("MaxFogIntensity", 4, 1);
    public final SandboxOptions.EnumSandboxOption maxRainFxIntensity = this.newEnumOption("MaxRainFxIntensity", 3, 1);
    public final SandboxOptions.BooleanSandboxOption enableSnowOnGround = this.newBooleanOption("EnableSnowOnGround", true);
    public final SandboxOptions.BooleanSandboxOption attackBlockMovements = this.newBooleanOption("AttackBlockMovements", true);
    public final SandboxOptions.EnumSandboxOption survivorHouseChance = this.newEnumOption("SurvivorHouseChance", 7, 3);
    public final SandboxOptions.EnumSandboxOption vehicleStoryChance = this.newEnumOption("VehicleStoryChance", 7, 3)
        .setValueTranslation("SurvivorHouseChance");
    public final SandboxOptions.EnumSandboxOption zoneStoryChance = this.newEnumOption("ZoneStoryChance", 7, 3).setValueTranslation("SurvivorHouseChance");
    public final SandboxOptions.BooleanSandboxOption allClothesUnlocked = this.newBooleanOption("AllClothesUnlocked", false);
    public final SandboxOptions.BooleanSandboxOption enableTaintedWaterText = this.newBooleanOption("EnableTaintedWaterText", true);
    public final SandboxOptions.BooleanSandboxOption enableVehicles = this.newBooleanOption("EnableVehicles", true);
    public final SandboxOptions.EnumSandboxOption carSpawnRate = this.newEnumOption("CarSpawnRate", 5, 4);
    public final SandboxOptions.DoubleSandboxOption zombieAttractionMultiplier = this.newDoubleOption("ZombieAttractionMultiplier", 0.0, 100.0, 1.0);
    public final SandboxOptions.BooleanSandboxOption vehicleEasyUse = this.newBooleanOption("VehicleEasyUse", false);
    public final SandboxOptions.EnumSandboxOption initialGas = this.newEnumOption("InitialGas", 6, 3);
    public final SandboxOptions.BooleanSandboxOption fuelStationGasInfinite = this.newBooleanOption("FuelStationGasInfinite", false);
    public final SandboxOptions.DoubleSandboxOption fuelStationGasMin = this.newDoubleOption("FuelStationGasMin", 0.0, 1.0, 0.0);
    public final SandboxOptions.DoubleSandboxOption fuelStationGasMax = this.newDoubleOption("FuelStationGasMax", 0.0, 1.0, 0.7);
    public final SandboxOptions.IntegerSandboxOption fuelStationGasEmptyChance = this.newIntegerOption("FuelStationGasEmptyChance", 0, 100, 20);
    public final SandboxOptions.EnumSandboxOption lockedCar = this.newEnumOption("LockedCar", 6, 4);
    public final SandboxOptions.DoubleSandboxOption carGasConsumption = this.newDoubleOption("CarGasConsumption", 0.0, 100.0, 1.0);
    public final SandboxOptions.EnumSandboxOption carGeneralCondition = this.newEnumOption("CarGeneralCondition", 5, 3);
    public final SandboxOptions.EnumSandboxOption carDamageOnImpact = this.newEnumOption("CarDamageOnImpact", 5, 3);
    public final SandboxOptions.EnumSandboxOption damageToPlayerFromHitByACar = this.newEnumOption("DamageToPlayerFromHitByACar", 5, 1);
    public final SandboxOptions.BooleanSandboxOption trafficJam = this.newBooleanOption("TrafficJam", true);
    public final SandboxOptions.EnumSandboxOption carAlarm = this.newEnumOption("CarAlarm", 6, 4).setTranslation("CarAlarmFrequency");
    public final SandboxOptions.BooleanSandboxOption playerDamageFromCrash = this.newBooleanOption("PlayerDamageFromCrash", true);
    public final SandboxOptions.DoubleSandboxOption sirenShutoffHours = this.newDoubleOption("SirenShutoffHours", 0.0, 168.0, 0.0);
    public final SandboxOptions.EnumSandboxOption chanceHasGas = this.newEnumOption("ChanceHasGas", 3, 2);
    public final SandboxOptions.EnumSandboxOption recentlySurvivorVehicles = this.newEnumOption("RecentlySurvivorVehicles", 4, 3);
    public final SandboxOptions.BooleanSandboxOption multiHitZombies = this.newBooleanOption("MultiHitZombies", false);
    public final SandboxOptions.EnumSandboxOption rearVulnerability = this.newEnumOption("RearVulnerability", 3, 3);
    public final SandboxOptions.BooleanSandboxOption sirenEffectsZombies = this.newBooleanOption("SirenEffectsZombies", true);
    public final SandboxOptions.EnumSandboxOption animalStatsModifier = this.newEnumOption("AnimalStatsModifier", 6, 4).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalMetaStatsModifier = this.newEnumOption("AnimalMetaStatsModifier", 6, 4)
        .setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalPregnancyTime = this.newEnumOption("AnimalPregnancyTime", 6, 2).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalAgeModifier = this.newEnumOption("AnimalAgeModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalMilkIncModifier = this.newEnumOption("AnimalMilkIncModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalWoolIncModifier = this.newEnumOption("AnimalWoolIncModifier", 6, 3).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.EnumSandboxOption animalRanchChance = this.newEnumOption("AnimalRanchChance", 7, 7).setValueTranslation("AnimalRanchChance");
    public final SandboxOptions.IntegerSandboxOption animalGrassRegrowTime = this.newIntegerOption("AnimalGrassRegrowTime", 1, 9999, 240);
    public final SandboxOptions.BooleanSandboxOption animalMetaPredator = this.newBooleanOption("AnimalMetaPredator", false);
    public final SandboxOptions.BooleanSandboxOption animalMatingSeason = this.newBooleanOption("AnimalMatingSeason", true);
    public final SandboxOptions.EnumSandboxOption animalEggHatch = this.newEnumOption("AnimalEggHatch", 6, 3).setValueTranslation("AnimalSpeed");
    public final SandboxOptions.BooleanSandboxOption animalSoundAttractZombies = this.newBooleanOption("AnimalSoundAttractZombies", false);
    public final SandboxOptions.EnumSandboxOption animalTrackChance = this.newEnumOption("AnimalTrackChance", 6, 4).setValueTranslation("HouseAlarmFrequency");
    public final SandboxOptions.EnumSandboxOption animalPathChance = this.newEnumOption("AnimalPathChance", 6, 4).setValueTranslation("HouseAlarmFrequency");
    public final SandboxOptions.IntegerSandboxOption maximumRatIndex = this.newIntegerOption("MaximumRatIndex", 0, 50, 25);
    public final SandboxOptions.IntegerSandboxOption daysUntilMaximumRatIndex = this.newIntegerOption("DaysUntilMaximumRatIndex", 0, 365, 90);
    public final SandboxOptions.EnumSandboxOption metaKnowledge = this.newEnumOption("MetaKnowledge", 3, 3);
    public final SandboxOptions.BooleanSandboxOption seeNotLearntRecipe = this.newBooleanOption("SeeNotLearntRecipe", true);
    public final SandboxOptions.IntegerSandboxOption maximumLootedBuildingRooms = this.newIntegerOption("MaximumLootedBuildingRooms", 0, 200, 50);
    public final SandboxOptions.EnumSandboxOption enablePoisoning = this.newEnumOption("EnablePoisoning", 3, 1);
    public final SandboxOptions.EnumSandboxOption maggotSpawn = this.newEnumOption("MaggotSpawn", 3, 1);
    public final SandboxOptions.DoubleSandboxOption lightBulbLifespan = this.newDoubleOption("LightBulbLifespan", 0.0, 1000.0, 1.0);
    public final SandboxOptions.EnumSandboxOption fishAbundance = this.newEnumOption("FishAbundance", 5, 3).setTranslation("FishAmount");
    public final SandboxOptions.IntegerSandboxOption levelForMediaXpCutoff = this.newIntegerOption("LevelForMediaXPCutoff", 0, 10, 3);
    public final SandboxOptions.IntegerSandboxOption levelForDismantleXpCutoff = this.newIntegerOption("LevelForDismantleXPCutoff", 0, 10, 0);
    public final SandboxOptions.IntegerSandboxOption bloodSplatLifespanDays = this.newIntegerOption("BloodSplatLifespanDays", 0, 365, 0);
    public final SandboxOptions.IntegerSandboxOption literatureCooldown = this.newIntegerOption("LiteratureCooldown", 1, 365, 90);
    public final SandboxOptions.EnumSandboxOption negativeTraitsPenalty = this.newEnumOption("NegativeTraitsPenalty", 4, 1);
    public final SandboxOptions.DoubleSandboxOption minutesPerPage = this.newDoubleOption("MinutesPerPage", 0.0, 60.0, 2.0);
    public final SandboxOptions.BooleanSandboxOption killInsideCrops = this.newBooleanOption("KillInsideCrops", true);
    public final SandboxOptions.BooleanSandboxOption plantGrowingSeasons = this.newBooleanOption("PlantGrowingSeasons", true);
    public final SandboxOptions.BooleanSandboxOption placeDirtAboveground = this.newBooleanOption("PlaceDirtAboveground", false);
    public final SandboxOptions.DoubleSandboxOption farmingSpeedNew = this.newDoubleOption("FarmingSpeedNew", 0.1, 100.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption farmingAmountNew = this.newDoubleOption("FarmingAmountNew", 0.1, 10.0, 1.0);
    public final SandboxOptions.IntegerSandboxOption maximumLooted = this.newIntegerOption("MaximumLooted", 0, 200, 50);
    public final SandboxOptions.IntegerSandboxOption daysUntilMaximumLooted = this.newIntegerOption("DaysUntilMaximumLooted", 0, 3650, 90);
    public final SandboxOptions.DoubleSandboxOption ruralLooted = this.newDoubleOption("RuralLooted", 0.0, 2.0, 0.5);
    public final SandboxOptions.IntegerSandboxOption maximumDiminishedLoot = this.newIntegerOption("MaximumDiminishedLoot", 0, 100, 0);
    public final SandboxOptions.IntegerSandboxOption daysUntilMaximumDiminishedLoot = this.newIntegerOption("DaysUntilMaximumDiminishedLoot", 0, 3650, 3650);
    public final SandboxOptions.DoubleSandboxOption muscleStrainFactor = this.newDoubleOption("MuscleStrainFactor", 0.0, 10.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption discomfortFactor = this.newDoubleOption("DiscomfortFactor", 0.0, 10.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption woundInfectionFactor = this.newDoubleOption("WoundInfectionFactor", 0.0, 10.0, 0.0);
    public final SandboxOptions.BooleanSandboxOption noBlackClothes = this.newBooleanOption("NoBlackClothes", true);
    public final SandboxOptions.BooleanSandboxOption easyClimbing = this.newBooleanOption("EasyClimbing", false);
    public final SandboxOptions.IntegerSandboxOption maximumFireFuelHours = this.newIntegerOption("MaximumFireFuelHours", 1, 168, 8);
    public final SandboxOptions.BooleanSandboxOption firearmUseDamageChance = this.newBooleanOption("FirearmUseDamageChance", true);
    public final SandboxOptions.DoubleSandboxOption firearmNoiseMultiplier = this.newDoubleOption("FirearmNoiseMultiplier", 0.2, 2.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption firearmJamMultiplier = this.newDoubleOption("FirearmJamMultiplier", 0.0, 10.0, 0.0);
    public final SandboxOptions.DoubleSandboxOption firearmMoodleMultiplier = this.newDoubleOption("FirearmMoodleMultiplier", 0.0, 10.0, 1.0);
    public final SandboxOptions.DoubleSandboxOption firearmWeatherMultiplier = this.newDoubleOption("FirearmWeatherMultiplier", 0.0, 10.0, 1.0);
    public final SandboxOptions.BooleanSandboxOption firearmHeadGearEffect = this.newBooleanOption("FirearmHeadGearEffect", true);
    public final SandboxOptions.DoubleSandboxOption clayLakeChance = this.newDoubleOption("ClayLakeChance", 0.0, 1.0, 0.05);
    public final SandboxOptions.DoubleSandboxOption clayRiverChance = this.newDoubleOption("ClayRiverChance", 0.0, 1.0, 0.05);
    public final SandboxOptions.IntegerSandboxOption generatorTileRange = this.newIntegerOption("GeneratorTileRange", 1, 100, 20);
    public final SandboxOptions.IntegerSandboxOption generatorVerticalPowerRange = this.newIntegerOption("GeneratorVerticalPowerRange", 1, 15, 3);
    private final ArrayList<SandboxOptions.SandboxOption> customOptions = new ArrayList<>();
    public final SandboxOptions.Basement basement = new SandboxOptions.Basement();
    public final SandboxOptions.Map map = new SandboxOptions.Map();
    public final SandboxOptions.ZombieLore lore = new SandboxOptions.ZombieLore();
    public final SandboxOptions.ZombieConfig zombieConfig = new SandboxOptions.ZombieConfig();
    public final SandboxOptions.MultiplierConfig multipliersConfig = new SandboxOptions.MultiplierConfig();
    private static final int SANDBOX_VERSION = 6;
    private final HashSet<String> lootItemRemovalSet = new HashSet<>();
    private String lootItemRemovalString;
    private final HashSet<String> worldItemRemovalSet = new HashSet<>();
    private String worldItemRemovalString;

    public SandboxOptions() {
        CustomSandboxOptions.instance.initInstance(this);
        File defines = ZomboidFileSystem.instance.getMediaFile("lua/shared/defines.lua");
        LuaManager.RunLua(defines.getAbsolutePath());
        this.loadGameFile("Apocalypse");
        this.setDefaultsToCurrentValues();
    }

    public static SandboxOptions getInstance() {
        return instance;
    }

    public void toLua() {
        KahluaTable vars = (KahluaTable)LuaManager.env.rawget("SandboxVars");

        for (int i = 0; i < this.options.size(); i++) {
            this.options.get(i).toTable(vars);
        }
    }

    public void updateFromLua() {
        if (Core.gameMode.equals("LastStand")) {
            GameTime.instance.multiplierBias = 1.2F;
        }

        KahluaTable tab = (KahluaTable)LuaManager.env.rawget("SandboxVars");

        for (int i = 0; i < this.options.size(); i++) {
            this.options.get(i).fromTable(tab);
        }
        GameTime.instance.multiplierBias = switch (this.speed) {
            case 1 -> 0.8F;
            case 2 -> 0.9F;
            default -> 1.0F;
            case 4 -> 1.1F;
            case 5 -> 1.2F;
        };

        VirtualZombieManager.instance.maxRealZombies = switch (this.zombies.getValue()) {
            case 1 -> 400;
            case 2 -> 350;
            case 3 -> 300;
            default -> 200;
            case 5 -> 100;
            case 6 -> 0;
        };
        VirtualZombieManager.instance.maxRealZombies = 1;
        this.applySettings();
    }

    public void initSandboxVars() {
        KahluaTable vars = (KahluaTable)LuaManager.env.rawget("SandboxVars");

        for (int i = 0; i < this.options.size(); i++) {
            SandboxOptions.SandboxOption option = this.options.get(i);
            option.fromTable(vars);
            option.toTable(vars);
        }
    }

    /**
     * Random the number of day for the water shut off
     */
    public int randomWaterShut(int waterShutoffModifier) {
        return switch (waterShutoffModifier) {
            case 2 -> Rand.Next(0, 30);
            case 3 -> Rand.Next(0, 60);
            case 4 -> Rand.Next(0, 180);
            case 5 -> Rand.Next(0, 360);
            case 6 -> Rand.Next(0, 1800);
            case 7 -> Rand.Next(60, 180);
            case 8 -> Rand.Next(180, 360);
            case 9 -> Integer.MAX_VALUE;
            default -> -1;
        };
    }

    /**
     * Random the number of day for the selectricity shut off
     */
    public int randomElectricityShut(int electricityShutoffModifier) {
        return switch (electricityShutoffModifier) {
            case 2 -> Rand.Next(14, 30);
            case 3 -> Rand.Next(14, 60);
            case 4 -> Rand.Next(14, 180);
            case 5 -> Rand.Next(14, 360);
            case 6 -> Rand.Next(14, 1800);
            case 7 -> Rand.Next(60, 180);
            case 8 -> Rand.Next(180, 360);
            case 9 -> Integer.MAX_VALUE;
            default -> -1;
        };
    }

    public int randomAlarmDecay(int alarmDecayModifier) {
        return switch (alarmDecayModifier) {
            case 2 -> Rand.Next(0, 30);
            case 3 -> Rand.Next(0, 60);
            case 4 -> Rand.Next(0, 180);
            case 5 -> Rand.Next(0, 360);
            case 6 -> Rand.Next(0, 1800);
            default -> 0;
        };
    }

    public int getTemperatureModifier() {
        return this.temperature.getValue();
    }

    public int getRainModifier() {
        return this.rain.getValue();
    }

    public int getErosionSpeed() {
        return this.erosionSpeed.getValue();
    }

    public int getWaterShutModifier() {
        return this.waterShutModifier.getValue();
    }

    public int getElecShutModifier() {
        return this.elecShutModifier.getValue();
    }

    public int getTimeSinceApo() {
        return this.timeSinceApo.getValue();
    }

    public double getEnduranceRegenMultiplier() {
        return switch (this.endRegen.getValue()) {
            case 1 -> 1.8;
            case 2 -> 1.3;
            default -> 1.0;
            case 4 -> 0.7;
            case 5 -> 0.4;
        };
    }

    public double getStatsDecreaseMultiplier() {
        return switch (this.statsDecrease.getValue()) {
            case 1 -> 2.0;
            case 2 -> 1.6;
            default -> 1.0;
            case 4 -> 0.8;
            case 5 -> 0.65;
        };
    }

    public int getDayLengthMinutes() {
        int value = this.dayLength.getValue();

        return switch (value) {
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 90;
            default -> (value - 3) * 60;
        };
    }

    public int getDayLengthMinutesDefault() {
        int defaultValue = this.dayLength.getDefaultValue();

        return switch (defaultValue) {
            case 1 -> 15;
            case 2 -> 30;
            case 3 -> 60;
            case 4 -> 90;
            default -> (defaultValue - 3) * 60;
        };
    }

    public int getCompostHours() {
        return switch (this.compostTime.getValue()) {
            case 1 -> 168;
            case 2 -> 336;
            case 3 -> 504;
            case 4 -> 672;
            case 5 -> 1008;
            case 6 -> 1344;
            case 7 -> 1680;
            case 8 -> 2016;
            default -> 336;
        };
    }

    public void applySettings() {
        GameTime.instance.setStartYear(this.getFirstYear() + this.startYear.getValue() - 1);
        GameTime.instance.setStartMonth(this.startMonth.getValue() - 1);
        GameTime.instance.setStartDay(this.startDay.getValue() - 1);
        GameTime.instance.setMinutesPerDay(this.getDayLengthMinutes());

        GameTime.instance.setStartTimeOfDay(switch (this.startTime.getValue()) {
            case 1 -> 7.0F;
            default -> 9.0F;
            case 3 -> 12.0F;
            case 4 -> 14.0F;
            case 5 -> 17.0F;
            case 6 -> 21.0F;
            case 7 -> 0.0F;
            case 8 -> 2.0F;
            case 9 -> 5.0F;
        });
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)83);
        output.put((byte)65);
        output.put((byte)78);
        output.put((byte)68);
        output.putInt(241);
        output.putInt(6);
        output.putInt(this.options.size());

        for (int i = 0; i < this.options.size(); i++) {
            SandboxOptions.SandboxOption option = this.options.get(i);
            GameWindow.WriteStringUTF(output, option.asConfigOption().getName());
            GameWindow.WriteStringUTF(output, option.asConfigOption().getValueAsString());
        }

        GameWindow.WriteStringUTF(output, LuaManager.GlobalObject.getWorld().getPreset());
    }

    public void load(ByteBuffer input) throws IOException {
        input.mark();
        byte b1 = input.get();
        byte b2 = input.get();
        byte b3 = input.get();
        byte b4 = input.get();
        int WorldVersion = input.getInt();
        int VERSION = input.getInt();
        int count = input.getInt();

        for (int i = 0; i < count; i++) {
            String name = GameWindow.ReadStringUTF(input);
            String value = GameWindow.ReadStringUTF(input);
            name = this.upgradeOptionName(name, VERSION);
            value = this.upgradeOptionValue(name, value, VERSION);
            SandboxOptions.SandboxOption option = this.optionByName.get(name);
            if (option == null) {
                DebugLog.log("ERROR unknown SandboxOption \"" + name + "\"");
            } else {
                option.asConfigOption().parse(value);
            }
        }

        LuaManager.GlobalObject.getWorld().setPreset(GameWindow.ReadStringUTF(input));
    }

    public int getFirstYear() {
        return 1993;
    }

    private static String[] parseName(String name) {
        String[] ret = new String[]{null, name};
        if (name.contains(".")) {
            String[] ss = name.split("\\.");
            if (ss.length == 2) {
                ret[0] = ss[0];
                ret[1] = ss[1];
            }
        }

        return ret;
    }

    private SandboxOptions.BooleanSandboxOption newBooleanOption(String name, boolean defaultValue) {
        return new SandboxOptions.BooleanSandboxOption(this, name, defaultValue);
    }

    private SandboxOptions.DoubleSandboxOption newDoubleOption(String name, double min, double max, double defaultValue) {
        return new SandboxOptions.DoubleSandboxOption(this, name, min, max, defaultValue);
    }

    private SandboxOptions.EnumSandboxOption newEnumOption(String name, int numValues, int defaultValue) {
        return new SandboxOptions.EnumSandboxOption(this, name, numValues, defaultValue);
    }

    private SandboxOptions.IntegerSandboxOption newIntegerOption(String name, int min, int max, int defaultValue) {
        return new SandboxOptions.IntegerSandboxOption(this, name, min, max, defaultValue);
    }

    private SandboxOptions.StringSandboxOption newStringOption(String name, String defaultValue, int maxLength) {
        return new SandboxOptions.StringSandboxOption(this, name, defaultValue, maxLength);
    }

    protected SandboxOptions addOption(SandboxOptions.SandboxOption option) {
        this.options.add(option);
        this.optionByName.put(option.asConfigOption().getName(), option);
        return this;
    }

    public int getNumOptions() {
        return this.options.size();
    }

    public SandboxOptions.SandboxOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public SandboxOptions.SandboxOption getOptionByName(String name) {
        return this.optionByName.get(name);
    }

    public void set(String name, Object o) {
        if (name != null && o != null) {
            SandboxOptions.SandboxOption option = this.optionByName.get(name);
            if (option == null) {
                throw new IllegalArgumentException("unknown SandboxOption \"" + name + "\"");
            } else {
                option.asConfigOption().setValueFromObject(o);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void copyValuesFrom(SandboxOptions other) {
        if (other == null) {
            throw new NullPointerException();
        } else {
            for (int i = 0; i < this.options.size(); i++) {
                this.options.get(i).asConfigOption().setValueFromObject(other.options.get(i).asConfigOption().getValueAsObject());
            }
        }
    }

    public void resetToDefault() {
        for (int i = 0; i < this.options.size(); i++) {
            this.options.get(i).asConfigOption().resetToDefault();
        }
    }

    public void setDefaultsToCurrentValues() {
        for (int i = 0; i < this.options.size(); i++) {
            this.options.get(i).asConfigOption().setDefaultToCurrentValue();
        }
    }

    public SandboxOptions newCopy() {
        SandboxOptions copy = new SandboxOptions();
        copy.copyValuesFrom(this);
        return copy;
    }

    public static boolean isValidPresetName(String name) {
        return name == null || name.isEmpty()
            ? false
            : !name.contains("/") && !name.contains("\\") && !name.contains(":") && !name.contains(";") && !name.contains("\"") && !name.contains(".");
    }

    private boolean readTextFile(String fileName, boolean isPreset) {
        ConfigFile configFile = new ConfigFile();
        if (!configFile.read(fileName)) {
            return false;
        } else {
            int VERSION = configFile.getVersion();
            HashSet<String> fixZombieLore = null;
            if (isPreset && VERSION == 1) {
                fixZombieLore = new HashSet<>();

                for (int i = 0; i < this.options.size(); i++) {
                    if ("ZombieLore".equals(this.options.get(i).getTableName())) {
                        fixZombieLore.add(this.options.get(i).getShortName());
                    }
                }
            }

            for (int ix = 0; ix < configFile.getOptions().size(); ix++) {
                ConfigOption configOption = configFile.getOptions().get(ix);
                String optionName = configOption.getName();
                String optionValue = configOption.getValueAsString();
                if (fixZombieLore != null && fixZombieLore.contains(optionName)) {
                    optionName = "ZombieLore." + optionName;
                }

                if (isPreset && VERSION == 1) {
                    if ("WaterShutModifier".equals(optionName)) {
                        optionName = "WaterShut";
                    } else if ("ElecShutModifier".equals(optionName)) {
                        optionName = "ElecShut";
                    }
                }

                optionName = this.upgradeOptionName(optionName, VERSION);
                optionValue = this.upgradeOptionValue(optionName, optionValue, VERSION);
                SandboxOptions.SandboxOption option = this.optionByName.get(optionName);
                if (option != null) {
                    option.asConfigOption().parse(optionValue);
                }
            }

            return true;
        }
    }

    private boolean writeTextFile(String fileName, int version) {
        ConfigFile configFile = new ConfigFile();
        ArrayList<ConfigOption> configOptions = new ArrayList<>();

        for (SandboxOptions.SandboxOption option : this.options) {
            configOptions.add(option.asConfigOption());
        }

        return configFile.write(fileName, version, configOptions);
    }

    public boolean loadServerTextFile(String serverName) {
        return this.readTextFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_sandbox.ini"), false);
    }

    public boolean loadServerLuaFile(String serverName) {
        boolean read = this.readLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_SandboxVars.lua"));
        if (this.lore.speed.getValue() == 1 || this.lore.speed.getValue() > 3) {
            this.lore.speed.setValue(2);
        }

        return read;
    }

    public boolean saveServerLuaFile(String serverName) {
        return this.writeLuaFile(ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_SandboxVars.lua"), false);
    }

    public boolean loadPresetFile(String presetName) {
        return this.readTextFile(LuaManager.getSandboxCacheDir() + File.separator + presetName + ".cfg", true);
    }

    public boolean savePresetFile(String presetName) {
        return !isValidPresetName(presetName) ? false : this.writeTextFile(LuaManager.getSandboxCacheDir() + File.separator + presetName + ".cfg", 6);
    }

    public boolean loadGameFile(String presetName) {
        File file = ZomboidFileSystem.instance.getMediaFile("lua/shared/Sandbox/" + presetName + ".lua");
        if (!file.exists()) {
            throw new RuntimeException("media/lua/shared/Sandbox/" + presetName + ".lua not found");
        } else {
            try {
                LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
                if (!(LuaManager.RunLua(file.getAbsolutePath()) instanceof KahluaTable kahluaTable)) {
                    throw new RuntimeException(file.getName() + " must return a SandboxVars table");
                } else {
                    for (int i = 0; i < this.options.size(); i++) {
                        this.options.get(i).fromTable(kahluaTable);
                    }

                    return true;
                }
            } catch (Exception var6) {
                ExceptionLogger.logException(var6);
                return false;
            }
        }
    }

    public boolean saveGameFile(String presetName) {
        return !Core.debug ? false : this.writeLuaFile("media/lua/shared/Sandbox/" + presetName + ".lua", true);
    }

    private void saveCurrentGameBinFile() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

        try (
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
        ) {
            synchronized (SliceY.SliceBufferLock) {
                SliceY.SliceBuffer.clear();
                this.save(SliceY.SliceBuffer);
                bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
            }
        } catch (Exception var11) {
            ExceptionLogger.logException(var11);
        }
    }

    public void handleOldZombiesFile1() {
        if (!GameServer.server) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
            ConfigFile configFile = new ConfigFile();
            if (configFile.read(fileName)) {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);
                    SandboxOptions.SandboxOption option = this.optionByName.get("ZombieConfig." + configOption.getName());
                    if (option != null) {
                        option.asConfigOption().parse(configOption.getValueAsString());
                    }
                }
            }
        }
    }

    public void handleOldZombiesFile2() {
        if (!GameServer.server) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("zombies.ini");
            File file = new File(fileName);
            if (file.exists()) {
                try {
                    DebugLog.DetailedInfo.trace("deleting " + file.getAbsolutePath());
                    file.delete();
                    this.saveCurrentGameBinFile();
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                }
            }
        }
    }

    public void handleOldServerZombiesFile() {
        if (GameServer.server) {
            if (this.loadServerZombiesFile(GameServer.serverName)) {
                String fileName = ServerSettingsManager.instance.getNameInSettingsFolder(GameServer.serverName + "_zombies.ini");

                try {
                    File file = new File(fileName);
                    DebugLog.DetailedInfo.trace("deleting " + file.getAbsolutePath());
                    file.delete();
                    this.saveServerLuaFile(GameServer.serverName);
                } catch (Exception var3) {
                    ExceptionLogger.logException(var3);
                }
            }
        }
    }

    public boolean loadServerZombiesFile(String serverName) {
        String fileName = ServerSettingsManager.instance.getNameInSettingsFolder(serverName + "_zombies.ini");
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); i++) {
                ConfigOption configOption = configFile.getOptions().get(i);
                SandboxOptions.SandboxOption option = this.optionByName.get("ZombieConfig." + configOption.getName());
                if (option != null) {
                    option.asConfigOption().parse(configOption.getValueAsString());
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean readLuaFile(String fileName) {
        File file = new File(fileName).getAbsoluteFile();
        if (!file.exists()) {
            return false;
        } else {
            Object oldObj = LuaManager.env.rawget("SandboxVars");
            KahluaTable oldTable = null;
            if (oldObj instanceof KahluaTable kahluaTable) {
                oldTable = kahluaTable;
            }

            LuaManager.env.rawset("SandboxVars", null);

            boolean newTable;
            try {
                LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
                Object result = LuaManager.RunLua(file.getAbsolutePath());
                Object newObj = LuaManager.env.rawget("SandboxVars");
                if (newObj != null) {
                    if (newObj instanceof KahluaTable newTablex) {
                        int VERSION = 0;
                        Object versionObj = newTablex.rawget("VERSION");
                        if (versionObj != null) {
                            if (versionObj instanceof Double d) {
                                VERSION = d.intValue();
                            } else {
                                DebugLog.log("ERROR: VERSION=\"" + versionObj + "\" in " + fileName);
                            }

                            newTablex.rawset("VERSION", null);
                        }

                        KahluaTable var19 = this.upgradeLuaTable("", newTablex, VERSION);

                        for (int i = 0; i < this.options.size(); i++) {
                            this.options.get(i).fromTable(var19);
                        }
                    }

                    return true;
                }

                newTable = false;
            } catch (Exception var14) {
                ExceptionLogger.logException(var14);
                return false;
            } finally {
                if (oldTable != null) {
                    LuaManager.env.rawset("SandboxVars", oldTable);
                }
            }

            return newTable;
        }
    }

    private boolean writeLuaFile(String fileName, boolean isDeveloperFile) {
        File file = new File(fileName).getAbsoluteFile();
        DebugLog.log("writing " + fileName);

        try {
            try (FileWriter fw = new FileWriter(file)) {
                HashMap<String, ArrayList<SandboxOptions.SandboxOption>> tables = new HashMap<>();
                ArrayList<String> tableNames = new ArrayList<>();
                tables.put("", new ArrayList<>());

                for (SandboxOptions.SandboxOption option : this.options) {
                    if (option.getTableName() == null) {
                        tables.get("").add(option);
                    } else {
                        if (tables.get(option.getTableName()) == null) {
                            tables.put(option.getTableName(), new ArrayList<>());
                            tableNames.add(option.getTableName());
                        }

                        tables.get(option.getTableName()).add(option);
                    }
                }

                String lineSep = System.lineSeparator();
                if (isDeveloperFile) {
                    fw.write("return {" + lineSep);
                } else {
                    fw.write("SandboxVars = {" + lineSep);
                }

                fw.write("    VERSION = 6," + lineSep);

                for (SandboxOptions.SandboxOption optionx : tables.get("")) {
                    if (!isDeveloperFile) {
                        String tooltip = optionx.asConfigOption().getTooltip();
                        if (tooltip != null) {
                            tooltip = tooltip.replace("\\n", " ").replace("\\\"", "\"");
                            tooltip = tooltip.replaceAll("\n", lineSep + "    -- ");
                            fw.write("    -- " + tooltip + lineSep);
                        }

                        if (optionx instanceof SandboxOptions.EnumSandboxOption enumOption) {
                            for (int i = 1; i <= enumOption.getNumValues(); i++) {
                                try {
                                    String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(i);
                                    if (subOptionTranslated != null) {
                                        fw.write("    -- " + i + " = " + subOptionTranslated.replace("\\\"", "\"") + lineSep);
                                    }
                                } catch (Exception var18) {
                                    ExceptionLogger.logException(var18);
                                }
                            }
                        }
                    }

                    fw.write("    " + optionx.asConfigOption().getName() + " = " + optionx.asConfigOption().getValueAsLuaString() + "," + lineSep);
                }

                for (String tableName : tableNames) {
                    fw.write("    " + tableName + " = {" + lineSep);

                    for (SandboxOptions.SandboxOption optionx : tables.get(tableName)) {
                        if (!isDeveloperFile) {
                            String tooltipx = optionx.asConfigOption().getTooltip();
                            if (tooltipx != null) {
                                tooltipx = tooltipx.replace("\\n", " ").replace("\\\"", "\"");
                                tooltipx = tooltipx.replaceAll("\n", lineSep + "        -- ");
                                fw.write("        -- " + tooltipx + lineSep);
                            }

                            if (optionx instanceof SandboxOptions.EnumSandboxOption enumOption) {
                                for (int i = 1; i <= enumOption.getNumValues(); i++) {
                                    try {
                                        String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(i);
                                        if (subOptionTranslated != null) {
                                            fw.write("        -- " + i + " = " + subOptionTranslated + lineSep);
                                        }
                                    } catch (Exception var17) {
                                        ExceptionLogger.logException(var17);
                                    }
                                }
                            }
                        }

                        fw.write("        " + optionx.getShortName() + " = " + optionx.asConfigOption().getValueAsLuaString() + "," + lineSep);
                    }

                    fw.write("    }," + lineSep);
                }

                fw.write("}" + lineSep);
            }

            return true;
        } catch (Exception var20) {
            ExceptionLogger.logException(var20);
            return false;
        }
    }

    public void load() {
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

        try {
            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
                synchronized (SliceY.SliceBufferLock) {
                    SliceY.SliceBuffer.clear();
                    int numBytes = bis.read(SliceY.SliceBuffer.array());
                    SliceY.SliceBuffer.limit(numBytes);
                    this.load(SliceY.SliceBuffer);
                    this.handleOldZombiesFile1();
                    this.applySettings();
                    this.toLua();
                }
            }

            return;
        } catch (FileNotFoundException var12) {
        } catch (Exception var13) {
            ExceptionLogger.logException(var13);
        }

        this.resetToDefault();
        this.updateFromLua();
    }

    public void loadCurrentGameBinFile() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_sand.bin");

        try (
            FileInputStream inStream = new FileInputStream(inFile);
            BufferedInputStream input = new BufferedInputStream(inStream);
        ) {
            synchronized (SliceY.SliceBufferLock) {
                SliceY.SliceBuffer.clear();
                int numBytes = input.read(SliceY.SliceBuffer.array());
                SliceY.SliceBuffer.limit(numBytes);
                this.load(SliceY.SliceBuffer);
            }

            this.toLua();
        } catch (Exception var12) {
            ExceptionLogger.logException(var12);
        }
    }

    private String upgradeOptionName(String optionName, int version) {
        return optionName;
    }

    private String upgradeOptionValue(String optionName, String optionValue, int version) {
        if (version < 3 && "DayLength".equals(optionName)) {
            this.dayLength.parse(optionValue);
            if (this.dayLength.getValue() == 8) {
                this.dayLength.setValue(14);
            } else if (this.dayLength.getValue() == 9) {
                this.dayLength.setValue(26);
            }

            optionValue = this.dayLength.getValueAsString();
        }

        if (version < 4 && "CarSpawnRate".equals(optionName)) {
            try {
                int value = (int)Double.parseDouble(optionValue);
                if (value > 1) {
                    optionValue = Integer.toString(value + 1);
                }
            } catch (NumberFormatException var7) {
                var7.printStackTrace();
            }
        }

        if (version < 5) {
            if ("FoodLoot".equals(optionName)
                || "CannedFoodLoot".equals(optionName)
                || "LiteratureLoot".equals(optionName)
                || "SurvivalGearsLoot".equals(optionName)
                || "MedicalLoot".equals(optionName)
                || "WeaponLoot".equals(optionName)
                || "RangedWeaponLoot".equals(optionName)
                || "AmmoLoot".equals(optionName)
                || "MechanicsLoot".equals(optionName)
                || "OtherLoot".equals(optionName)) {
                try {
                    int value = (int)Double.parseDouble(optionValue);
                    if (value > 0) {
                        optionValue = Integer.toString(value + 2);
                    }
                } catch (NumberFormatException var6) {
                    var6.printStackTrace();
                }
            }

            if ("RecentlySurvivorVehicles".equals(optionName)) {
                try {
                    int value = (int)Double.parseDouble(optionValue);
                    if (value > 0) {
                        optionValue = Integer.toString(value + 1);
                    }
                } catch (NumberFormatException var5) {
                    var5.printStackTrace();
                }
            }
        }

        if (version < 6 && "DayLength".equals(optionName)) {
            this.dayLength.parse(optionValue);
            if (this.dayLength.getValue() > 3) {
                this.dayLength.setValue(this.dayLength.getValue() + 1);
            }
        }

        return optionValue;
    }

    private KahluaTable upgradeLuaTable(String prefix, KahluaTable table, int version) {
        KahluaTable newTable = LuaManager.platform.newTable();
        KahluaTableIterator it = table.iterator();

        while (it.advance()) {
            if (!(it.getKey() instanceof String)) {
                throw new IllegalStateException("expected a String key");
            }

            if (it.getValue() instanceof KahluaTable) {
                KahluaTable newTable1 = this.upgradeLuaTable(prefix + it.getKey() + ".", (KahluaTable)it.getValue(), version);
                newTable.rawset(it.getKey(), newTable1);
            } else {
                String optionName = this.upgradeOptionName(prefix + it.getKey(), version);
                String optionValue = this.upgradeOptionValue(optionName, it.getValue().toString(), version);
                newTable.rawset(optionName.replace(prefix, ""), optionValue);
            }
        }

        return newTable;
    }

    public void sendToServer() {
        if (GameClient.client) {
            GameClient.instance.sendSandboxOptionsToServer(this);
        }
    }

    public void newCustomOption(CustomSandboxOption customSandboxOption) {
        if (customSandboxOption instanceof CustomBooleanSandboxOption booleanOption) {
            this.addCustomOption(new SandboxOptions.BooleanSandboxOption(this, booleanOption.id, booleanOption.defaultValue), customSandboxOption);
        } else if (customSandboxOption instanceof CustomDoubleSandboxOption doubleOption) {
            this.addCustomOption(
                new SandboxOptions.DoubleSandboxOption(this, doubleOption.id, doubleOption.min, doubleOption.max, doubleOption.defaultValue),
                customSandboxOption
            );
        } else if (customSandboxOption instanceof CustomEnumSandboxOption enumOption) {
            SandboxOptions.EnumSandboxOption sandboxOption = new SandboxOptions.EnumSandboxOption(
                this, enumOption.id, enumOption.numValues, enumOption.defaultValue
            );
            if (enumOption.valueTranslation != null) {
                sandboxOption.setValueTranslation(enumOption.valueTranslation);
            }

            this.addCustomOption(sandboxOption, customSandboxOption);
        } else if (customSandboxOption instanceof CustomIntegerSandboxOption integerOption) {
            this.addCustomOption(
                new SandboxOptions.IntegerSandboxOption(this, integerOption.id, integerOption.min, integerOption.max, integerOption.defaultValue),
                customSandboxOption
            );
        } else if (customSandboxOption instanceof CustomStringSandboxOption stringOption) {
            this.addCustomOption(new SandboxOptions.StringSandboxOption(this, stringOption.id, stringOption.defaultValue, -1), customSandboxOption);
        } else {
            throw new IllegalArgumentException("unhandled CustomSandboxOption " + customSandboxOption);
        }
    }

    private void addCustomOption(SandboxOptions.SandboxOption option, CustomSandboxOption custom) {
        option.setCustom();
        if (custom.page != null) {
            option.setPageName(custom.page);
        }

        if (custom.translation != null) {
            option.setTranslation(custom.translation);
        }

        this.customOptions.add(option);
    }

    private void removeCustomOptions() {
        this.options.removeAll(this.customOptions);

        for (SandboxOptions.SandboxOption option : this.customOptions) {
            this.optionByName.remove(option.asConfigOption().getName());
        }

        this.customOptions.clear();
    }

    public static void Reset() {
        instance.removeCustomOptions();
    }

    public boolean getAllClothesUnlocked() {
        return this.allClothesUnlocked.getValue();
    }

    public int getCurrentRatIndex() {
        int maxRat = instance.maximumRatIndex.getValue();
        int daysUntilMax = instance.daysUntilMaximumRatIndex.getValue();
        if (maxRat <= 0) {
            return 0;
        } else if (daysUntilMax <= 0) {
            return maxRat;
        } else {
            int days = (int)((float)GameTime.getInstance().getWorldAgeHours() / 24.0F) + (instance.timeSinceApo.getValue() - 1) * 30;
            if (days <= 0) {
                days = 1;
            }

            if (days > daysUntilMax) {
                days = daysUntilMax;
            }

            int currentRatFactor = maxRat * days / daysUntilMax;
            if (currentRatFactor <= 0) {
                currentRatFactor = 1;
            }

            return currentRatFactor;
        }
    }

    public int getCurrentLootedChance() {
        return this.getCurrentLootedChance(null);
    }

    public int getCurrentLootedChance(IsoGridSquare square) {
        int maxLooted = instance.maximumLooted.getValue();
        int daysUntilMax = instance.daysUntilMaximumLooted.getValue();
        if (maxLooted <= 0) {
            return 0;
        } else if (daysUntilMax <= 0) {
            return maxLooted;
        } else {
            int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (instance.timeSinceApo.getValue() - 1) * 30;
            if (days <= 0) {
                days = 1;
            }

            if (days > daysUntilMax) {
                days = daysUntilMax;
            }

            int currentLootedChance = maxLooted * days / daysUntilMax;
            if (square != null && ItemPickerJava.getSquareRegion(square) == null) {
                currentLootedChance *= (int)instance.ruralLooted.getValue();
            }

            if (square != null && Objects.equals(square.getSquareZombiesType(), "Rich")) {
                currentLootedChance = (int)(currentLootedChance * 1.5);
            }

            if (currentLootedChance <= 0) {
                currentLootedChance = 1;
            }

            return currentLootedChance;
        }
    }

    public int getCurrentDiminishedLootPercentage() {
        return this.getCurrentDiminishedLootPercentage(null);
    }

    public int getCurrentDiminishedLootPercentage(IsoGridSquare square) {
        int maxLooted = instance.maximumDiminishedLoot.getValue();
        int daysUntilMax = instance.daysUntilMaximumDiminishedLoot.getValue();
        if (maxLooted <= 0) {
            return 0;
        } else if (daysUntilMax <= 0) {
            return maxLooted;
        } else {
            int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (instance.timeSinceApo.getValue() - 1) * 30;
            if (days <= 0) {
                days = 1;
            }

            if (days > daysUntilMax) {
                days = daysUntilMax;
            }

            int currentLooted = maxLooted * days / daysUntilMax;
            if (square != null && ItemPickerJava.getSquareRegion(square) == null) {
                currentLooted *= (int)instance.ruralLooted.getValue();
            }

            if (currentLooted < 0) {
                currentLooted = 0;
            }

            if (currentLooted > 100) {
                currentLooted = 100;
            }

            return currentLooted;
        }
    }

    public float getCurrentLootMultiplier() {
        return this.getCurrentLootMultiplier(null);
    }

    public float getCurrentLootMultiplier(IsoGridSquare square) {
        return 1.0F - this.getCurrentDiminishedLootPercentage(square) / 100.0F;
    }

    public boolean isUnstableScriptNameSpam() {
        return true;
    }

    public boolean doesPowerGridExist() {
        return this.doesPowerGridExist(0);
    }

    public boolean doesPowerGridExist(int offset) {
        return IsoWorld.instance.getWorldAgeDays() <= getInstance().getElecShutModifier() + offset;
    }

    public boolean lootItemRemovalListContains(String itemType) {
        if (this.lootItemRemovalString != this.lootItemRemovalList.getValue()) {
            this.lootItemRemovalString = this.lootItemRemovalList.getValue();
            Set<String> listOfStrings = this.lootItemRemovalList.getSplitCSVList();
            this.lootItemRemovalSet.clear();
            this.lootItemRemovalSet.addAll(listOfStrings);
        }

        return this.lootItemRemovalSet.contains(itemType);
    }

    public boolean worldItemRemovalListContains(String itemType) {
        if (this.worldItemRemovalString != this.worldItemRemovalList.getValue()) {
            this.worldItemRemovalString = this.worldItemRemovalList.getValue();
            Set<String> listOfStrings = this.worldItemRemovalList.getSplitCSVList();
            this.worldItemRemovalSet.clear();
            this.worldItemRemovalSet.addAll(listOfStrings);
        }

        return this.worldItemRemovalSet.contains(itemType);
    }

    public final class Basement {
        public final SandboxOptions.EnumSandboxOption spawnFrequency;

        public Basement() {
            Objects.requireNonNull(SandboxOptions.this);
            super();
            this.spawnFrequency = SandboxOptions.this.newEnumOption("Basement.SpawnFrequency", 7, 4).setTranslation("BasementSpawnFrequency");
        }
    }

    @UsedFromLua
    public static class BooleanSandboxOption extends BooleanConfigOption implements SandboxOptions.SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public BooleanSandboxOption(SandboxOptions owner, String name, boolean defaultValue) {
            super(name, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        public BooleanConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        public SandboxOptions.BooleanSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
        }

        @Override
        public void fromTable(KahluaTable table) {
            if (this.tableName != null) {
                if (!(table.rawget(this.tableName) instanceof KahluaTable kahluaTable)) {
                    return;
                }

                table = kahluaTable;
            }

            Object o = table.rawget(this.getShortName());
            if (o != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                if (table.rawget(this.tableName) instanceof KahluaTable kahluaTable) {
                    table = kahluaTable;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, table2);
                    table = table2;
                }
            }

            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        public SandboxOptions.BooleanSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    @UsedFromLua
    public static class DoubleSandboxOption extends DoubleConfigOption implements SandboxOptions.SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public DoubleSandboxOption(SandboxOptions owner, String name, double min, double max, double defaultValue) {
            super(name, min, max, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        public DoubleConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        public SandboxOptions.DoubleSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s1;
            if ("ZombieConfig".equals(this.tableName)) {
                s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_help");
            } else {
                s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            }

            String s2 = Translator.getText(
                "Sandbox_MinMaxDefault", String.format("%.02f", this.min), String.format("%.02f", this.max), String.format("%.02f", this.defaultValue)
            );
            if (s1 == null) {
                return s2;
            } else {
                return s2 == null ? s1 : s1 + "\\n" + s2;
            }
        }

        @Override
        public void fromTable(KahluaTable table) {
            if (this.tableName != null) {
                if (!(table.rawget(this.tableName) instanceof KahluaTable kahluaTable)) {
                    return;
                }

                table = kahluaTable;
            }

            Object o = table.rawget(this.getShortName());
            if (o != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                if (table.rawget(this.tableName) instanceof KahluaTable kahluaTable) {
                    table = kahluaTable;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, table2);
                    table = table2;
                }
            }

            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        public SandboxOptions.DoubleSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    @UsedFromLua
    public static class EnumSandboxOption extends EnumConfigOption implements SandboxOptions.SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;
        protected String valueTranslation;

        public EnumSandboxOption(SandboxOptions owner, String name, int numValues, int defaultValue) {
            super(name, numValues, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        public EnumConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        public SandboxOptions.EnumSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            String value = this.getValueTranslationByIndexOrNull(this.defaultValue);
            String s2 = value == null ? null : Translator.getText("Sandbox_Default", value);
            if (s1 == null) {
                return s2;
            } else {
                return s2 == null ? s1 : s1 + "\\n" + s2;
            }
        }

        @Override
        public void fromTable(KahluaTable table) {
            if (this.tableName != null) {
                if (!(table.rawget(this.tableName) instanceof KahluaTable kahluaTable)) {
                    return;
                }

                table = kahluaTable;
            }

            Object o = table.rawget(this.getShortName());
            if (o != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                if (table.rawget(this.tableName) instanceof KahluaTable kahluaTable) {
                    table = kahluaTable;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, table2);
                    table = table2;
                }
            }

            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        public SandboxOptions.EnumSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }

        public SandboxOptions.EnumSandboxOption setValueTranslation(String translation) {
            this.valueTranslation = translation;
            return this;
        }

        public String getValueTranslation() {
            return this.valueTranslation != null ? this.valueTranslation : (this.translation == null ? this.getShortName() : this.translation);
        }

        public String getValueTranslationByIndex(int index) {
            if (index >= 1 && index <= this.getNumValues()) {
                return Translator.getText("Sandbox_" + this.getValueTranslation() + "_option" + index);
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }

        public String getValueTranslationByIndexOrNull(int index) {
            if (index >= 1 && index <= this.getNumValues()) {
                return Translator.getTextOrNull("Sandbox_" + this.getValueTranslation() + "_option" + index);
            } else {
                throw new ArrayIndexOutOfBoundsException();
            }
        }
    }

    @UsedFromLua
    public static class IntegerSandboxOption extends IntegerConfigOption implements SandboxOptions.SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public IntegerSandboxOption(SandboxOptions owner, String name, int min, int max, int defaultValue) {
            super(name, min, max, defaultValue);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        public IntegerConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        public SandboxOptions.IntegerSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            String s1;
            if ("ZombieConfig".equals(this.tableName)) {
                s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_help");
            } else {
                s1 = Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
            }

            String s2 = Translator.getText("Sandbox_MinMaxDefault", this.min, this.max, this.defaultValue);
            if (s1 == null) {
                return s2;
            } else {
                return s2 == null ? s1 : s1 + "\\n" + s2;
            }
        }

        @Override
        public void fromTable(KahluaTable table) {
            if (this.tableName != null) {
                if (!(table.rawget(this.tableName) instanceof KahluaTable kahluaTable)) {
                    return;
                }

                table = kahluaTable;
            }

            Object o = table.rawget(this.getShortName());
            if (o != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                if (table.rawget(this.tableName) instanceof KahluaTable kahluaTable) {
                    table = kahluaTable;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, table2);
                    table = table2;
                }
            }

            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        public SandboxOptions.IntegerSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    public final class Map {
        public final SandboxOptions.BooleanSandboxOption allowMiniMap;
        public final SandboxOptions.BooleanSandboxOption allowWorldMap;
        public final SandboxOptions.BooleanSandboxOption mapAllKnown;
        public final SandboxOptions.BooleanSandboxOption mapNeedsLight;

        public Map() {
            Objects.requireNonNull(SandboxOptions.this);
            super();
            this.allowMiniMap = SandboxOptions.this.newBooleanOption("Map.AllowMiniMap", false);
            this.allowWorldMap = SandboxOptions.this.newBooleanOption("Map.AllowWorldMap", true);
            this.mapAllKnown = SandboxOptions.this.newBooleanOption("Map.MapAllKnown", false);
            this.mapNeedsLight = SandboxOptions.this.newBooleanOption("Map.MapNeedsLight", true);
        }
    }

    public final class MultiplierConfig {
        public final SandboxOptions.DoubleSandboxOption xpMultiplierGlobal;
        public final SandboxOptions.BooleanSandboxOption xpMultiplierGlobalToggle;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierFitness;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierStrength;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierSprinting;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierLightfoot;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierNimble;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierSneak;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierAxe;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierBlunt;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierSmallBlunt;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierLongBlade;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierSmallBlade;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierSpear;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierMaintenance;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierWoodwork;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierCooking;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierFarming;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierDoctor;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierElectricity;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierMetalWelding;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierMechanics;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierTailoring;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierAiming;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierReloading;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierFishing;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierTrapping;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierPlantScavenging;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierFlintKnapping;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierMasonry;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierPottery;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierCarving;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierHusbandry;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierTracking;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierBlacksmith;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierButchering;
        public final SandboxOptions.DoubleSandboxOption xpMultiplierGlassmaking;

        public MultiplierConfig() {
            Objects.requireNonNull(SandboxOptions.this);
            super();
            this.xpMultiplierGlobal = SandboxOptions.this.newDoubleOption("MultiplierConfig.Global", 0.0, 1000.0, 1.0);
            this.xpMultiplierGlobalToggle = SandboxOptions.this.newBooleanOption("MultiplierConfig.GlobalToggle", true);
            this.xpMultiplierFitness = SandboxOptions.this.newDoubleOption("MultiplierConfig.Fitness", 0.0, 1000.0, 1.0);
            this.xpMultiplierStrength = SandboxOptions.this.newDoubleOption("MultiplierConfig.Strength", 0.0, 1000.0, 1.0);
            this.xpMultiplierSprinting = SandboxOptions.this.newDoubleOption("MultiplierConfig.Sprinting", 0.0, 1000.0, 1.0);
            this.xpMultiplierLightfoot = SandboxOptions.this.newDoubleOption("MultiplierConfig.Lightfoot", 0.0, 1000.0, 1.0);
            this.xpMultiplierNimble = SandboxOptions.this.newDoubleOption("MultiplierConfig.Nimble", 0.0, 1000.0, 1.0);
            this.xpMultiplierSneak = SandboxOptions.this.newDoubleOption("MultiplierConfig.Sneak", 0.0, 1000.0, 1.0);
            this.xpMultiplierAxe = SandboxOptions.this.newDoubleOption("MultiplierConfig.Axe", 0.0, 1000.0, 1.0);
            this.xpMultiplierBlunt = SandboxOptions.this.newDoubleOption("MultiplierConfig.Blunt", 0.0, 1000.0, 1.0);
            this.xpMultiplierSmallBlunt = SandboxOptions.this.newDoubleOption("MultiplierConfig.SmallBlunt", 0.0, 1000.0, 1.0);
            this.xpMultiplierLongBlade = SandboxOptions.this.newDoubleOption("MultiplierConfig.LongBlade", 0.0, 1000.0, 1.0);
            this.xpMultiplierSmallBlade = SandboxOptions.this.newDoubleOption("MultiplierConfig.SmallBlade", 0.0, 1000.0, 1.0);
            this.xpMultiplierSpear = SandboxOptions.this.newDoubleOption("MultiplierConfig.Spear", 0.0, 1000.0, 1.0);
            this.xpMultiplierMaintenance = SandboxOptions.this.newDoubleOption("MultiplierConfig.Maintenance", 0.0, 1000.0, 1.0);
            this.xpMultiplierWoodwork = SandboxOptions.this.newDoubleOption("MultiplierConfig.Woodwork", 0.0, 1000.0, 1.0);
            this.xpMultiplierCooking = SandboxOptions.this.newDoubleOption("MultiplierConfig.Cooking", 0.0, 1000.0, 1.0);
            this.xpMultiplierFarming = SandboxOptions.this.newDoubleOption("MultiplierConfig.Farming", 0.0, 1000.0, 1.0);
            this.xpMultiplierDoctor = SandboxOptions.this.newDoubleOption("MultiplierConfig.Doctor", 0.0, 1000.0, 1.0);
            this.xpMultiplierElectricity = SandboxOptions.this.newDoubleOption("MultiplierConfig.Electricity", 0.0, 1000.0, 1.0);
            this.xpMultiplierMetalWelding = SandboxOptions.this.newDoubleOption("MultiplierConfig.MetalWelding", 0.0, 1000.0, 1.0);
            this.xpMultiplierMechanics = SandboxOptions.this.newDoubleOption("MultiplierConfig.Mechanics", 0.0, 1000.0, 1.0);
            this.xpMultiplierTailoring = SandboxOptions.this.newDoubleOption("MultiplierConfig.Tailoring", 0.0, 1000.0, 1.0);
            this.xpMultiplierAiming = SandboxOptions.this.newDoubleOption("MultiplierConfig.Aiming", 0.0, 1000.0, 1.0);
            this.xpMultiplierReloading = SandboxOptions.this.newDoubleOption("MultiplierConfig.Reloading", 0.0, 1000.0, 1.0);
            this.xpMultiplierFishing = SandboxOptions.this.newDoubleOption("MultiplierConfig.Fishing", 0.0, 1000.0, 1.0);
            this.xpMultiplierTrapping = SandboxOptions.this.newDoubleOption("MultiplierConfig.Trapping", 0.0, 1000.0, 1.0);
            this.xpMultiplierPlantScavenging = SandboxOptions.this.newDoubleOption("MultiplierConfig.PlantScavenging", 0.0, 1000.0, 1.0);
            this.xpMultiplierFlintKnapping = SandboxOptions.this.newDoubleOption("MultiplierConfig.FlintKnapping", 0.0, 1000.0, 1.0);
            this.xpMultiplierMasonry = SandboxOptions.this.newDoubleOption("MultiplierConfig.Masonry", 0.0, 1000.0, 1.0);
            this.xpMultiplierPottery = SandboxOptions.this.newDoubleOption("MultiplierConfig.Pottery", 0.0, 1000.0, 1.0);
            this.xpMultiplierCarving = SandboxOptions.this.newDoubleOption("MultiplierConfig.Carving", 0.0, 1000.0, 1.0);
            this.xpMultiplierHusbandry = SandboxOptions.this.newDoubleOption("MultiplierConfig.Husbandry", 0.0, 1000.0, 1.0);
            this.xpMultiplierTracking = SandboxOptions.this.newDoubleOption("MultiplierConfig.Tracking", 0.0, 1000.0, 1.0);
            this.xpMultiplierBlacksmith = SandboxOptions.this.newDoubleOption("MultiplierConfig.Blacksmith", 0.0, 1000.0, 1.0);
            this.xpMultiplierButchering = SandboxOptions.this.newDoubleOption("MultiplierConfig.Butchering", 0.0, 1000.0, 1.0);
            this.xpMultiplierGlassmaking = SandboxOptions.this.newDoubleOption("MultiplierConfig.Glassmaking", 0.0, 1000.0, 1.0);
        }
    }

    public interface SandboxOption {
        ConfigOption asConfigOption();

        String getShortName();

        String getTableName();

        SandboxOptions.SandboxOption setTranslation(String translation);

        String getTranslatedName();

        String getTooltip();

        void fromTable(KahluaTable table);

        void toTable(KahluaTable table);

        void setCustom();

        boolean isCustom();

        SandboxOptions.SandboxOption setPageName(String pageName);

        String getPageName();
    }

    @UsedFromLua
    public static class StringSandboxOption extends StringConfigOption implements SandboxOptions.SandboxOption {
        protected String translation;
        protected String tableName;
        protected String shortName;
        protected boolean custom;
        protected String pageName;

        public StringSandboxOption(SandboxOptions owner, String name, String defaultValue, int maxLength) {
            super(name, defaultValue, maxLength);
            String[] ss = SandboxOptions.parseName(name);
            this.tableName = ss[0];
            this.shortName = ss[1];
            owner.addOption(this);
        }

        public StringConfigOption asConfigOption() {
            return this;
        }

        @Override
        public String getShortName() {
            return this.shortName;
        }

        @Override
        public String getTableName() {
            return this.tableName;
        }

        public SandboxOptions.StringSandboxOption setTranslation(String translation) {
            this.translation = translation;
            return this;
        }

        @Override
        public String getTranslatedName() {
            return Translator.getText("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation));
        }

        @Override
        public String getTooltip() {
            return Translator.getTextOrNull("Sandbox_" + (this.translation == null ? this.getShortName() : this.translation) + "_tooltip");
        }

        @Override
        public void fromTable(KahluaTable table) {
            if (this.tableName != null) {
                if (!(table.rawget(this.tableName) instanceof KahluaTable kahluaTable)) {
                    return;
                }

                table = kahluaTable;
            }

            Object o = table.rawget(this.getShortName());
            if (o != null) {
                this.setValueFromObject(o);
            }
        }

        @Override
        public void toTable(KahluaTable table) {
            if (this.tableName != null) {
                if (table.rawget(this.tableName) instanceof KahluaTable kahluaTable) {
                    table = kahluaTable;
                } else {
                    KahluaTable table2 = LuaManager.platform.newTable();
                    table.rawset(this.tableName, table2);
                    table = table2;
                }
            }

            table.rawset(this.getShortName(), this.getValueAsObject());
        }

        @Override
        public void setCustom() {
            this.custom = true;
        }

        @Override
        public boolean isCustom() {
            return this.custom;
        }

        public SandboxOptions.StringSandboxOption setPageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        @Override
        public String getPageName() {
            return this.pageName;
        }
    }

    public final class ZombieConfig {
        public final SandboxOptions.DoubleSandboxOption populationMultiplier;
        public final SandboxOptions.DoubleSandboxOption populationStartMultiplier;
        public final SandboxOptions.DoubleSandboxOption populationPeakMultiplier;
        public final SandboxOptions.IntegerSandboxOption populationPeakDay;
        public final SandboxOptions.DoubleSandboxOption respawnHours;
        public final SandboxOptions.DoubleSandboxOption respawnUnseenHours;
        public final SandboxOptions.DoubleSandboxOption respawnMultiplier;
        public final SandboxOptions.DoubleSandboxOption redistributeHours;
        public final SandboxOptions.IntegerSandboxOption followSoundDistance;
        public final SandboxOptions.IntegerSandboxOption rallyGroupSize;
        public final SandboxOptions.IntegerSandboxOption rallyGroupSizeVariance;
        public final SandboxOptions.IntegerSandboxOption rallyTravelDistance;
        public final SandboxOptions.IntegerSandboxOption rallyGroupSeparation;
        public final SandboxOptions.IntegerSandboxOption rallyGroupRadius;
        public final SandboxOptions.IntegerSandboxOption zombiesCountBeforeDeletion;

        public ZombieConfig() {
            Objects.requireNonNull(SandboxOptions.this);
            super();
            this.populationMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationMultiplier", 0.0, 4.0, 0.65F);
            this.populationStartMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationStartMultiplier", 0.0, 4.0, 1.0);
            this.populationPeakMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.PopulationPeakMultiplier", 0.0, 4.0, 1.5);
            this.populationPeakDay = SandboxOptions.this.newIntegerOption("ZombieConfig.PopulationPeakDay", 1, 365, 28);
            this.respawnHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnHours", 0.0, 8760.0, 72.0);
            this.respawnUnseenHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnUnseenHours", 0.0, 8760.0, 16.0);
            this.respawnMultiplier = SandboxOptions.this.newDoubleOption("ZombieConfig.RespawnMultiplier", 0.0, 1.0, 0.1);
            this.redistributeHours = SandboxOptions.this.newDoubleOption("ZombieConfig.RedistributeHours", 0.0, 8760.0, 12.0);
            this.followSoundDistance = SandboxOptions.this.newIntegerOption("ZombieConfig.FollowSoundDistance", 10, 1000, 100);
            this.rallyGroupSize = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupSize", 0, 1000, 20);
            this.rallyGroupSizeVariance = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupSizeVariance", 0, 100, 50);
            this.rallyTravelDistance = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyTravelDistance", 5, 50, 20);
            this.rallyGroupSeparation = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupSeparation", 5, 25, 15);
            this.rallyGroupRadius = SandboxOptions.this.newIntegerOption("ZombieConfig.RallyGroupRadius", 1, 10, 3);
            this.zombiesCountBeforeDeletion = SandboxOptions.this.newIntegerOption("ZombieConfig.ZombiesCountBeforeDelete", 10, 500, 300);
        }
    }

    public final class ZombieLore {
        public final SandboxOptions.EnumSandboxOption speed;
        public final SandboxOptions.IntegerSandboxOption sprinterPercentage;
        public final SandboxOptions.EnumSandboxOption strength;
        public final SandboxOptions.EnumSandboxOption toughness;
        public final SandboxOptions.EnumSandboxOption transmission;
        public final SandboxOptions.EnumSandboxOption mortality;
        public final SandboxOptions.EnumSandboxOption reanimate;
        public final SandboxOptions.EnumSandboxOption cognition;
        public final SandboxOptions.EnumSandboxOption crawlUnderVehicle;
        public final SandboxOptions.EnumSandboxOption memory;
        public final SandboxOptions.EnumSandboxOption sight;
        public final SandboxOptions.EnumSandboxOption hearing;
        public final SandboxOptions.BooleanSandboxOption spottedLogic;
        public final SandboxOptions.BooleanSandboxOption thumpNoChasing;
        public final SandboxOptions.BooleanSandboxOption thumpOnConstruction;
        public final SandboxOptions.EnumSandboxOption activeOnly;
        public final SandboxOptions.BooleanSandboxOption triggerHouseAlarm;
        public final SandboxOptions.BooleanSandboxOption zombiesDragDown;
        public final SandboxOptions.BooleanSandboxOption zombiesCrawlersDragDown;
        public final SandboxOptions.BooleanSandboxOption zombiesFenceLunge;
        public final SandboxOptions.DoubleSandboxOption zombiesArmorFactor;
        public final SandboxOptions.IntegerSandboxOption zombiesMaxDefense;
        public final SandboxOptions.IntegerSandboxOption chanceOfAttachedWeapon;
        public final SandboxOptions.DoubleSandboxOption zombiesFallDamage;
        public final SandboxOptions.EnumSandboxOption disableFakeDead;
        public final SandboxOptions.EnumSandboxOption playerSpawnZombieRemoval;
        public final SandboxOptions.IntegerSandboxOption fenceThumpersRequired;
        public final SandboxOptions.DoubleSandboxOption fenceDamageMultiplier;

        public ZombieLore() {
            Objects.requireNonNull(SandboxOptions.this);
            super();
            this.speed = SandboxOptions.this.newEnumOption("ZombieLore.Speed", 4, 2).setTranslation("ZSpeed");
            this.sprinterPercentage = SandboxOptions.this.newIntegerOption("ZombieLore.SprinterPercentage", 0, 100, 33).setTranslation("ZSprinterPercentage");
            this.strength = SandboxOptions.this.newEnumOption("ZombieLore.Strength", 4, 2).setTranslation("ZStrength");
            this.toughness = SandboxOptions.this.newEnumOption("ZombieLore.Toughness", 4, 2).setTranslation("ZToughness");
            this.transmission = SandboxOptions.this.newEnumOption("ZombieLore.Transmission", 4, 1).setTranslation("ZTransmission");
            this.mortality = SandboxOptions.this.newEnumOption("ZombieLore.Mortality", 7, 5).setTranslation("ZInfectionMortality");
            this.reanimate = SandboxOptions.this.newEnumOption("ZombieLore.Reanimate", 6, 3).setTranslation("ZReanimateTime");
            this.cognition = SandboxOptions.this.newEnumOption("ZombieLore.Cognition", 4, 3).setTranslation("ZCognition");
            this.crawlUnderVehicle = SandboxOptions.this.newEnumOption("ZombieLore.CrawlUnderVehicle", 7, 5).setTranslation("ZCrawlUnderVehicle");
            this.memory = SandboxOptions.this.newEnumOption("ZombieLore.Memory", 6, 2).setTranslation("ZMemory");
            this.sight = SandboxOptions.this.newEnumOption("ZombieLore.Sight", 5, 2).setTranslation("ZSight");
            this.hearing = SandboxOptions.this.newEnumOption("ZombieLore.Hearing", 5, 2).setTranslation("ZHearing");
            this.spottedLogic = SandboxOptions.this.newBooleanOption("ZombieLore.SpottedLogic", true);
            this.thumpNoChasing = SandboxOptions.this.newBooleanOption("ZombieLore.ThumpNoChasing", false);
            this.thumpOnConstruction = SandboxOptions.this.newBooleanOption("ZombieLore.ThumpOnConstruction", true);
            this.activeOnly = SandboxOptions.this.newEnumOption("ZombieLore.ActiveOnly", 3, 1).setTranslation("ActiveOnly");
            this.triggerHouseAlarm = SandboxOptions.this.newBooleanOption("ZombieLore.TriggerHouseAlarm", false);
            this.zombiesDragDown = SandboxOptions.this.newBooleanOption("ZombieLore.ZombiesDragDown", true);
            this.zombiesCrawlersDragDown = SandboxOptions.this.newBooleanOption("ZombieLore.ZombiesCrawlersDragDown", false);
            this.zombiesFenceLunge = SandboxOptions.this.newBooleanOption("ZombieLore.ZombiesFenceLunge", true);
            this.zombiesArmorFactor = SandboxOptions.this.newDoubleOption("ZombieLore.ZombiesArmorFactor", 0.0, 100.0, 2.0);
            this.zombiesMaxDefense = SandboxOptions.this.newIntegerOption("ZombieLore.ZombiesMaxDefense", 0, 100, 85);
            this.chanceOfAttachedWeapon = SandboxOptions.this.newIntegerOption("ZombieLore.ChanceOfAttachedWeapon", 0, 100, 6);
            this.zombiesFallDamage = SandboxOptions.this.newDoubleOption("ZombieLore.ZombiesFallDamage", 0.0, 100.0, 1.0);
            this.disableFakeDead = SandboxOptions.this.newEnumOption("ZombieLore.DisableFakeDead", 3, 1);
            this.playerSpawnZombieRemoval = SandboxOptions.this.newEnumOption("ZombieLore.PlayerSpawnZombieRemoval", 4, 1).setTranslation("ZSpawnRemoval");
            this.fenceThumpersRequired = SandboxOptions.this.newIntegerOption("ZombieLore.FenceThumpersRequired", -1, 100, 50);
            this.fenceDamageMultiplier = SandboxOptions.this.newDoubleOption("ZombieLore.FenceDamageMultiplier", 0.01F, 100.0, 1.0);
        }
    }
}
