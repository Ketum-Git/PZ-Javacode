// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.debug.objects.DebugClassFields;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
@UsedFromLua
public class SealedFluidProperties {
    public static final String Str_Fatigue = "Fatigue";
    public static final String Str_Hunger = "Hunger";
    public static final String Str_Stress = "Stress";
    public static final String Str_Thirst = "Thirst";
    public static final String Str_Unhappy = "Unhappy";
    public static final String Str_Calories = "Calories";
    public static final String Str_Carbohydrates = "Carbohydrates";
    public static final String Str_Lipids = "Lipids";
    public static final String Str_Proteins = "Proteins";
    public static final String Str_Alcohol = "Alcohol";
    public static final String Str_Flu = "FluReduction";
    public static final String Str_Pain = "PainReduction";
    public static final String Str_Endurance = "EnduranceChange";
    public static final String Str_FoodSickness = "FoodSicknessChange";
    private float fatigueChange;
    private float hungerChange;
    private float stressChange;
    private float thirstChange;
    private float unhappyChange;
    private float calories;
    private float carbohydrates;
    private float lipids;
    private float proteins;
    private float alcohol;
    private float poison;
    private float fluReduction;
    private float painReduction;
    private float enduranceChange;
    private int foodSicknessChange;

    public void save(ByteBuffer output) throws IOException {
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, output);
        if (this.fatigueChange > 0.0F) {
            header.addFlags(1);
            output.putFloat(this.fatigueChange);
        }

        if (this.hungerChange > 0.0F) {
            header.addFlags(2);
            output.putFloat(this.hungerChange);
        }

        if (this.stressChange > 0.0F) {
            header.addFlags(4);
            output.putFloat(this.stressChange);
        }

        if (this.thirstChange > 0.0F) {
            header.addFlags(8);
            output.putFloat(this.thirstChange);
        }

        if (this.unhappyChange > 0.0F) {
            header.addFlags(16);
            output.putFloat(this.unhappyChange);
        }

        if (this.calories > 0.0F) {
            header.addFlags(32);
            output.putFloat(this.calories);
        }

        if (this.carbohydrates > 0.0F) {
            header.addFlags(64);
            output.putFloat(this.carbohydrates);
        }

        if (this.lipids > 0.0F) {
            header.addFlags(128);
            output.putFloat(this.lipids);
        }

        if (this.proteins > 0.0F) {
            header.addFlags(256);
            output.putFloat(this.proteins);
        }

        if (this.alcohol > 0.0F) {
            header.addFlags(512);
            output.putFloat(this.alcohol);
        }

        if (this.fluReduction > 0.0F) {
            header.addFlags(1024);
            output.putFloat(this.fluReduction);
        }

        if (this.painReduction > 0.0F) {
            header.addFlags(2048);
            output.putFloat(this.painReduction);
        }

        if (this.enduranceChange > 0.0F) {
            header.addFlags(4096);
            output.putFloat(this.enduranceChange);
        }

        if (this.foodSicknessChange != 0) {
            header.addFlags(8192);
            output.putFloat(this.foodSicknessChange);
        }

        if (this.poison > 0.0F) {
            header.addFlags(16384);
            output.putFloat(this.poison);
        }

        header.write();
        header.release();
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.clear();
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Integer, input);
        if (header.hasFlags(1)) {
            this.fatigueChange = input.getFloat();
        }

        if (header.hasFlags(2)) {
            this.hungerChange = input.getFloat();
        }

        if (header.hasFlags(4)) {
            this.stressChange = input.getFloat();
        }

        if (header.hasFlags(8)) {
            this.thirstChange = input.getFloat();
        }

        if (header.hasFlags(16)) {
            this.unhappyChange = input.getFloat();
        }

        if (header.hasFlags(32)) {
            this.calories = input.getFloat();
        }

        if (header.hasFlags(64)) {
            this.carbohydrates = input.getFloat();
        }

        if (header.hasFlags(128)) {
            this.lipids = input.getFloat();
        }

        if (header.hasFlags(256)) {
            this.proteins = input.getFloat();
        }

        if (header.hasFlags(512)) {
            this.alcohol = input.getFloat();
        }

        if (header.hasFlags(1024)) {
            this.fluReduction = input.getFloat();
        }

        if (header.hasFlags(2048)) {
            this.painReduction = input.getFloat();
        }

        if (header.hasFlags(4096)) {
            this.enduranceChange = input.getFloat();
        }

        if (header.hasFlags(8192)) {
            this.foodSicknessChange = input.getInt();
        }

        if (header.hasFlags(16384)) {
            this.poison = input.getFloat();
        }

        header.release();
    }

    public boolean hasProperties() {
        if (this.fatigueChange != 0.0F) {
            return true;
        } else if (this.hungerChange != 0.0F) {
            return true;
        } else if (this.stressChange != 0.0F) {
            return true;
        } else if (this.thirstChange != 0.0F) {
            return true;
        } else if (this.unhappyChange != 0.0F) {
            return true;
        } else if (this.calories != 0.0F) {
            return true;
        } else if (this.carbohydrates != 0.0F) {
            return true;
        } else if (this.lipids != 0.0F) {
            return true;
        } else if (this.proteins != 0.0F) {
            return true;
        } else if (this.alcohol != 0.0F) {
            return true;
        } else if (this.poison != 0.0F) {
            return true;
        } else if (this.fluReduction != 0.0F) {
            return true;
        } else if (this.painReduction != 0.0F) {
            return true;
        } else {
            return this.enduranceChange != 0.0F ? true : this.foodSicknessChange != 0;
        }
    }

    protected void clear() {
        this.fatigueChange = 0.0F;
        this.hungerChange = 0.0F;
        this.stressChange = 0.0F;
        this.thirstChange = 0.0F;
        this.unhappyChange = 0.0F;
        this.calories = 0.0F;
        this.carbohydrates = 0.0F;
        this.lipids = 0.0F;
        this.proteins = 0.0F;
        this.alcohol = 0.0F;
        this.poison = 0.0F;
        this.fluReduction = 0.0F;
        this.painReduction = 0.0F;
        this.enduranceChange = 0.0F;
        this.foodSicknessChange = 0;
    }

    protected void addFromMultiplied(SealedFluidProperties other, float multi) {
        this.fatigueChange = this.fatigueChange + other.fatigueChange * multi;
        this.hungerChange = this.hungerChange + other.hungerChange * multi;
        this.stressChange = this.stressChange + other.stressChange * multi;
        this.thirstChange = this.thirstChange + other.thirstChange * multi;
        this.unhappyChange = this.unhappyChange + other.unhappyChange * multi;
        this.calories = this.calories + other.calories * multi;
        this.carbohydrates = this.carbohydrates + other.carbohydrates * multi;
        this.lipids = this.lipids + other.lipids * multi;
        this.proteins = this.proteins + other.proteins * multi;
        this.alcohol = this.alcohol + other.alcohol * multi;
        this.poison = this.poison + other.poison * multi;
        this.fluReduction = this.fluReduction + other.fluReduction * multi;
        this.painReduction = this.painReduction + other.painReduction * multi;
        this.enduranceChange = this.enduranceChange + other.enduranceChange * multi;
    }

    protected void setEffects(
        float fatigueChange, float hungerChange, float stressChange, float thirstChange, float unhappyChange, float alcoholChange, float poisonChange
    ) {
        this.setFatigueChange(fatigueChange);
        this.setHungerChange(hungerChange);
        this.setStressChange(stressChange);
        this.setThirstChange(thirstChange);
        this.setUnhappyChange(unhappyChange);
        this.setAlcohol(alcoholChange);
        this.setPoison(poisonChange);
    }

    protected void setNutrients(float calories, float carbohydrates, float lipids, float proteins) {
        this.setCalories(calories);
        this.setCarbohydrates(carbohydrates);
        this.setLipids(lipids);
        this.setProteins(proteins);
    }

    protected void setReductions(float fluReduction, float painReduction, float enduranceChange, int foodSicknessChange) {
        this.setFluReduction(fluReduction);
        this.setPainReduction(painReduction);
        this.setEnduranceChange(enduranceChange);
        this.setFoodSicknessChange(foodSicknessChange);
    }

    public float getFatigueChange() {
        return this.fatigueChange;
    }

    protected void setFatigueChange(float fatigueChange) {
        this.fatigueChange = fatigueChange;
    }

    public float getHungerChange() {
        return this.hungerChange;
    }

    protected void setHungerChange(float hungerChange) {
        this.hungerChange = hungerChange;
    }

    public float getStressChange() {
        return this.stressChange;
    }

    protected void setStressChange(float stressChange) {
        this.stressChange = stressChange;
    }

    public float getThirstChange() {
        return this.thirstChange;
    }

    protected void setThirstChange(float thirstChange) {
        this.thirstChange = thirstChange;
    }

    public float getUnhappyChange() {
        return this.unhappyChange;
    }

    protected void setUnhappyChange(float unhappyChange) {
        this.unhappyChange = unhappyChange;
    }

    public float getCalories() {
        return this.calories;
    }

    protected void setCalories(float calories) {
        this.calories = calories;
    }

    public float getCarbohydrates() {
        return this.carbohydrates;
    }

    protected void setCarbohydrates(float carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public float getLipids() {
        return this.lipids;
    }

    protected void setLipids(float lipids) {
        this.lipids = lipids;
    }

    public float getProteins() {
        return this.proteins;
    }

    protected void setProteins(float proteins) {
        this.proteins = proteins;
    }

    public float getAlcohol() {
        return this.alcohol;
    }

    protected void setAlcohol(float alcohol) {
        this.alcohol = alcohol;
    }

    public float getPoison() {
        return this.poison;
    }

    protected void setPoison(float poison) {
        this.poison = poison;
    }

    public float getFluReduction() {
        return this.fluReduction;
    }

    protected void setFluReduction(float fluReduction) {
        this.fluReduction = fluReduction;
    }

    public float getPainReduction() {
        return this.painReduction;
    }

    protected void setPainReduction(float painReduction) {
        this.painReduction = painReduction;
    }

    public float getEnduranceChange() {
        return this.enduranceChange;
    }

    protected void setEnduranceChange(float enduranceChange) {
        this.enduranceChange = enduranceChange;
    }

    public int getFoodSicknessChange() {
        return this.foodSicknessChange;
    }

    protected void setFoodSicknessChange(int foodSicknessChange) {
        this.foodSicknessChange = foodSicknessChange;
    }
}
