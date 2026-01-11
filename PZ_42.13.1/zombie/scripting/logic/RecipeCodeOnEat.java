// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import zombie.UsedFromLua;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Stats;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public class RecipeCodeOnEat extends RecipeCodeHelper {
    private static void consumeNicotineLogic(InventoryItem item, IsoGameCharacter character, float percent) {
        int foodSickness = item.getFoodSicknessChange();
        int invChanceSmokerCough = item.getInverseCoughProbabilitySmoker();
        int invProbabilityCoughNoSmoker = item.getInverseCoughProbability();
        float stressChange = item.getStressChange();
        Stats stats = character.getStats();
        int invProbabilityCough = invChanceSmokerCough;
        if (character.hasTrait(CharacterTrait.SMOKER)) {
            stats.add(CharacterStat.UNHAPPINESS, stressChange * percent);
            stats.add(CharacterStat.STRESS, stressChange * percent);
            float reduceNicotineWithdrawal = CharacterStat.NICOTINE_WITHDRAWAL.getMaximumValue();
            stats.remove(CharacterStat.NICOTINE_WITHDRAWAL, reduceNicotineWithdrawal * percent);
            character.setTimeSinceLastSmoke(stats.get(CharacterStat.NICOTINE_WITHDRAWAL) / reduceNicotineWithdrawal);
        } else {
            invProbabilityCough = invProbabilityCoughNoSmoker;
            stats.add(CharacterStat.FOOD_SICKNESS, foodSickness * percent);
        }

        if (invProbabilityCough > 0 && Rand.NextBool(invProbabilityCough)) {
            character.triggerCough();
        }
    }

    public static void consumeNicotine(DrainableComboItem item, IsoGameCharacter character) {
        consumeNicotineLogic(item, character, 1.0F);
    }

    public static void consumeNicotine(Food item, IsoGameCharacter character, float percent) {
        consumeNicotineLogic(item, character, percent);
    }

    public static void consumeCorrectionFluid(DrainableComboItem item, IsoGameCharacter character) {
        character.getStats().remove(CharacterStat.UNHAPPINESS, 5.0F);
        character.getStats().remove(CharacterStat.BOREDOM, 5.0F);
        character.getStats().add(CharacterStat.FOOD_SICKNESS, item.getFoodSicknessChange());
    }

    public static void consumeRatPoison(DrainableComboItem item, IsoGameCharacter character) {
        character.getStats().set(CharacterStat.POISON, CharacterStat.POISON.getMaximumValue());
        character.getStats().set(CharacterStat.THIRST, CharacterStat.THIRST.getMaximumValue());
        character.getStats().add(CharacterStat.FOOD_SICKNESS, item.getFoodSicknessChange());
    }

    public static void consumeWildFoodGeneric(Food item, IsoGameCharacter character, float percent) {
        if (item.getPoisonPower() > 0) {
            character.getStats().add(CharacterStat.FOOD_SICKNESS, 50.0F * percent);
        }
    }
}
