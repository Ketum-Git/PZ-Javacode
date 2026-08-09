// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.debug.DebugType;

public final class ZomboidGlobals {
    public static double runningEnduranceReduce;
    public static double sprintingEnduranceReduce;
    public static double imobileEnduranceReduce;
    public static double sittingEnduranceMultiplier = 5.0;
    public static double thirstIncrease;
    public static double thirstSleepingIncrease;
    public static double thirstLevelToAutoDrink;
    public static double thirstLevelReductionOnAutoDrink;
    public static double hungerIncrease;
    public static double hungerIncreaseWhenWellFed;
    public static double hungerIncreaseWhileAsleep;
    public static double hungerIncreaseWhenExercise;
    public static double fatigueIncrease;
    public static double stressReduction;
    public static double boredomIncreaseRate;
    public static double boredomDecreaseRate;
    public static double unhappinessIncrease;
    public static double stressFromSoundsMultiplier;
    public static double stressFromBiteOrScratch;
    public static double stressFromHemophobic;
    public static double stressFromDiscomfort;
    public static double angerDecrease;
    public static double sleepFatigueReduction;
    public static double wetnessIncrease;
    public static double wetnessDecrease;
    public static double catchAColdIncreaseRate;
    public static double catchAColdDecreaseRate;
    public static double poisonLevelDecrease;
    public static double poisonHealthReduction;
    public static double foodSicknessDecrease;
    public static double idleIncreaseRate;
    public static double idleDecreaseRate;
    public static double cleanBloodBleachAmount;
    public static double refillBlowtorchPropaneAmount = 70.0;
    public static double cleanStainCleaningFluidAmount;
    public static double vehicleDiscomfortWhenOverEncumbered;
    public static double equippedOrWornEncumbranceMultiplier;
    public static double corpseSicknessFilterDrainMultiplier;
    public static float basicMaskDefense = 25.0F;
    public static float improvisedGasMaskDefense = 85.0F;
    public static float standardGasMaskDefense = 100.0F;

    public static void Load() {
        KahluaTable globals = (KahluaTable)LuaManager.env.rawget("ZomboidGlobals");
        sprintingEnduranceReduce = (Double)globals.rawget("SprintingEnduranceReduce");
        runningEnduranceReduce = (Double)globals.rawget("RunningEnduranceReduce");
        imobileEnduranceReduce = (Double)globals.rawget("ImobileEnduranceIncrease");
        thirstIncrease = (Double)globals.rawget("ThirstIncrease");
        thirstSleepingIncrease = (Double)globals.rawget("ThirstSleepingIncrease");
        thirstLevelToAutoDrink = (Double)globals.rawget("ThirstLevelToAutoDrink");
        thirstLevelReductionOnAutoDrink = (Double)globals.rawget("ThirstLevelReductionOnAutoDrink");
        hungerIncrease = (Double)globals.rawget("HungerIncrease");
        hungerIncreaseWhenWellFed = (Double)globals.rawget("HungerIncreaseWhenWellFed");
        hungerIncreaseWhileAsleep = (Double)globals.rawget("HungerIncreaseWhileAsleep");
        hungerIncreaseWhenExercise = (Double)globals.rawget("HungerIncreaseWhenExercise");
        fatigueIncrease = (Double)globals.rawget("FatigueIncrease");
        stressReduction = (Double)globals.rawget("StressDecrease");
        boredomIncreaseRate = (Double)globals.rawget("BoredomIncrease");
        boredomDecreaseRate = (Double)globals.rawget("BoredomDecrease");
        unhappinessIncrease = (Double)globals.rawget("UnhappinessIncrease");
        stressFromSoundsMultiplier = (Double)globals.rawget("StressFromSoundsMultiplier");
        stressFromBiteOrScratch = (Double)globals.rawget("StressFromBiteOrScratch");
        stressFromHemophobic = (Double)globals.rawget("StressFromHemophobic");
        stressFromDiscomfort = (Double)globals.rawget("StressFromDiscomfort");
        angerDecrease = (Double)globals.rawget("AngerDecrease");
        sleepFatigueReduction = (Double)globals.rawget("SleepFatigueReduction");
        wetnessIncrease = (Double)globals.rawget("WetnessIncrease");
        wetnessDecrease = (Double)globals.rawget("WetnessDecrease");
        catchAColdIncreaseRate = (Double)globals.rawget("CatchAColdIncreaseRate");
        catchAColdDecreaseRate = (Double)globals.rawget("CatchAColdDecreaseRate");
        poisonLevelDecrease = (Double)globals.rawget("PoisonLevelDecrease");
        poisonHealthReduction = (Double)globals.rawget("PoisonHealthReduction");
        foodSicknessDecrease = (Double)globals.rawget("FoodSicknessDecrease");
        idleIncreaseRate = (Double)globals.rawget("IdleIncrease");
        idleDecreaseRate = (Double)globals.rawget("IdleDecrease");
        cleanBloodBleachAmount = (Double)globals.rawget("CleanBloodBleachAmount");
        cleanStainCleaningFluidAmount = (Double)globals.rawget("CleanStainCleaningFluidAmount");
        vehicleDiscomfortWhenOverEncumbered = (Double)globals.rawget("VehicleDiscomfortWhenOverEncumbered");
        equippedOrWornEncumbranceMultiplier = (Double)globals.rawget("EquippedOrWornEncumbranceMultiplier");
        corpseSicknessFilterDrainMultiplier = (Double)globals.rawget("CorpseSicknessFilterDrainMultiplier");
    }

    public static void toLua() {
        KahluaTable zg = (KahluaTable)LuaManager.env.rawget("ZomboidGlobals");
        if (zg == null) {
            DebugType.Zombie.error("ERROR: ZomboidGlobals table undefined in Lua");
        }
    }
}
