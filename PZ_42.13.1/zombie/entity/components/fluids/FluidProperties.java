// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import zombie.UsedFromLua;
import zombie.debug.objects.DebugClassFields;

@DebugClassFields
@UsedFromLua
public class FluidProperties extends SealedFluidProperties {
    public SealedFluidProperties getSealedFluidProperties() {
        SealedFluidProperties sealed = new SealedFluidProperties();
        sealed.setEffects(
            this.getFatigueChange(),
            this.getHungerChange(),
            this.getStressChange(),
            this.getThirstChange(),
            this.getUnhappyChange(),
            this.getAlcohol(),
            this.getPoison()
        );
        sealed.setNutrients(this.getCalories(), this.getCarbohydrates(), this.getLipids(), this.getProteins());
        sealed.setReductions(this.getFluReduction(), this.getPainReduction(), this.getEnduranceChange(), this.getFoodSicknessChange());
        sealed.setAlcohol(this.getAlcohol());
        sealed.setPoison(this.getPoison());
        return sealed;
    }

    @Override
    public void setEffects(
        float fatigueChange, float hungerChange, float stressChange, float thirstChange, float unhappyChange, float alcoholChange, float poisonChange
    ) {
        super.setEffects(fatigueChange, hungerChange, stressChange, thirstChange, unhappyChange, alcoholChange, poisonChange);
    }

    @Override
    public void setNutrients(float calories, float carbohydrates, float lipids, float proteins) {
        super.setNutrients(calories, carbohydrates, lipids, proteins);
    }

    @Override
    public void setReductions(float fluReduction, float painReduction, float enduranceChange, int foodSicknessChange) {
        super.setReductions(fluReduction, painReduction, enduranceChange, foodSicknessChange);
    }

    @Override
    public void setFatigueChange(float fatigueChange) {
        super.setFatigueChange(fatigueChange);
    }

    @Override
    public void setHungerChange(float hungerChange) {
        super.setHungerChange(hungerChange);
    }

    @Override
    public void setStressChange(float stressChange) {
        super.setStressChange(stressChange);
    }

    @Override
    public void setThirstChange(float thirstChange) {
        super.setThirstChange(thirstChange);
    }

    @Override
    public void setUnhappyChange(float unhappyChange) {
        super.setUnhappyChange(unhappyChange);
    }

    @Override
    public void setCalories(float calories) {
        super.setCalories(calories);
    }

    @Override
    public void setCarbohydrates(float carbohydrates) {
        super.setCarbohydrates(carbohydrates);
    }

    @Override
    public void setLipids(float lipids) {
        super.setLipids(lipids);
    }

    @Override
    public void setProteins(float proteins) {
        super.setProteins(proteins);
    }

    @Override
    public void setAlcohol(float alcohol) {
        super.setAlcohol(alcohol);
    }

    @Override
    public void setFluReduction(float fluReduction) {
        super.setFluReduction(fluReduction);
    }

    @Override
    public void setPainReduction(float painReduction) {
        super.setPainReduction(painReduction);
    }

    @Override
    public void setEnduranceChange(float enduranceChange) {
        super.setEnduranceChange(enduranceChange);
    }

    @Override
    public void setFoodSicknessChange(int foodSicknessChange) {
        super.setFoodSicknessChange(foodSicknessChange);
    }
}
