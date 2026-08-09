// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.datas;

import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.IsoAnimal;

public class AnimalGrowStage {
    public int ageToGrow;
    public String nextStage;
    public String nextStageMale;
    public String stage;

    public int getAgeToGrow(IsoAnimal animal) {
        AnimalAllele allele = animal.getUsedGene("ageToGrow");
        float aValue = 1.0F;
        if (allele != null) {
            aValue = allele.currentValue;
        }

        int baseValue = this.ageToGrow;
        float modifier = 0.25F - aValue / 4.0F + 1.0F;
        return (int)(baseValue * modifier);
    }
}
