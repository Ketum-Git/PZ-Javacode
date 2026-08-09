// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugType;
import zombie.util.StringUtils;

@UsedFromLua
public class AnimalGene {
    public String name;
    public int id = Rand.Next(1000000);
    public AnimalAllele allele1;
    public AnimalAllele allele2;

    public AnimalGene() {
    }

    public AnimalGene(AnimalGene gene) {
        this.name = gene.name;
        this.id = gene.id;
        this.allele1 = new AnimalAllele(gene.allele1);
        this.allele2 = new AnimalAllele(gene.allele2);
    }

    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        output.putInt(this.id);
        GameWindow.WriteString(output, this.name);
        this.allele1.save(output, isDebugSave);
        this.allele2.save(output, isDebugSave);
    }

    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        this.id = input.getInt();
        this.name = GameWindow.ReadString(input);
        this.allele1 = new AnimalAllele();
        this.allele1.load(input, worldVersion, isDebugSave);
        this.allele2 = new AnimalAllele();
        this.allele2.load(input, worldVersion, isDebugSave);
    }

    public static void initGenome(IsoAnimal animal) {
        if (animal.adef.genes != null) {
            ArrayList<String> genes = animal.adef.genes;
            animal.fullGenome = new HashMap<>();
            HashMap<String, AnimalBreed.ForcedGenes> forcedGenes = animal.getBreed().forcedGenes;
            if (forcedGenes != null) {
                for (String key : forcedGenes.keySet()) {
                    AnimalGene geneSet = new AnimalGene();
                    String name = key.toLowerCase();
                    geneSet.name = name;
                    AnimalBreed.ForcedGenes value = forcedGenes.get(name);
                    AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(name);
                    if (def == null) {
                        DebugType.Animal.debugln(name + " wasn't found in AnimalGenomeDefinitions.lua");
                    } else {
                        for (int j = 0; j < 2; j++) {
                            AnimalAllele newGene = initAllele(name, true, Rand.Next(value.minValue, value.maxValue), def.forcedValues);
                            if (j == 0) {
                                geneSet.allele1 = newGene;
                            } else {
                                geneSet.allele2 = newGene;
                            }
                        }

                        geneSet.initUsedGene();
                        animal.fullGenome.put(name, geneSet);
                    }
                }
            }

            for (int i = 0; i < genes.size(); i++) {
                AnimalGene geneSet = new AnimalGene();
                String name = genes.get(i).toLowerCase();
                geneSet.name = name;
                if (!animal.fullGenome.containsKey(name)) {
                    AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(name);
                    if (def == null) {
                        DebugType.Animal.debugln(name + " wasn't found in AnimalGenomeDefinitions.lua");
                    } else {
                        for (int jx = 0; jx < 2; jx++) {
                            AnimalAllele allele = initAllele(name, false, Rand.Next(def.minValue, def.maxValue), def.forcedValues);
                            if (jx == 0) {
                                geneSet.allele1 = allele;
                            } else {
                                geneSet.allele2 = allele;
                            }
                        }

                        geneSet.initUsedGene();
                        animal.fullGenome.put(name, geneSet);
                    }
                }
            }

            for (String s : animal.fullGenome.keySet()) {
                AnimalGene gene = animal.fullGenome.get(s);
                AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
                AnimalAllele baseGene = gene.allele1;
                if (gene.allele2.used) {
                    baseGene = gene.allele2;
                }

                doRatio(def, animal.fullGenome, baseGene);
            }
        }
    }

    public void initUsedGene() {
        int nb;
        if (this.allele1.dominant) {
            if (this.allele2.dominant) {
                nb = Rand.Next(2);
            } else {
                nb = 0;
            }
        } else if (this.allele2.dominant) {
            if (this.allele1.dominant) {
                nb = Rand.Next(2);
            } else {
                nb = 1;
            }
        } else {
            nb = Rand.Next(2);
        }

        if (nb == 0) {
            this.allele1.used = true;
        } else {
            this.allele2.used = true;
        }
    }

    private static AnimalAllele initAllele(String name, boolean forcedDominant, float value, boolean forcedValues) {
        if (value < 0.0F) {
            value = 0.0F;
        }

        AnimalAllele newAllele = new AnimalAllele();
        if (forcedDominant) {
            newAllele.dominant = true;
        } else {
            if (!forcedValues && Rand.Next(100) < 15) {
                value = Rand.Next(0.0F, 1.0F);
            }

            float avgDist = Math.abs(value - 0.5F);
            int recChance = 30;
            if (avgDist > 0.45) {
                recChance = 90;
            } else if (avgDist > 0.4) {
                recChance = 80;
            } else if (avgDist > 0.3) {
                recChance = 75;
            } else if (avgDist > 0.2) {
                recChance = 60;
            } else if (avgDist > 0.15) {
                recChance = 50;
            } else if (avgDist > 0.1) {
                recChance = 40;
            }

            newAllele.dominant = Rand.Next(100) > recChance;
        }

        newAllele.name = name;
        newAllele.currentValue = value;
        doMutation(newAllele);
        return newAllele;
    }

    public static void doRatio(AnimalGenomeDefinitions def, HashMap<String, AnimalGene> fullGenome, AnimalAllele allele) {
        if (def.ratios != null) {
            for (String s : def.ratios.keySet()) {
                String name = s.toLowerCase();
                Float value = def.ratios.get(name);
                AnimalGene affectedGenes = fullGenome.get(name);
                if (affectedGenes == null) {
                    DebugType.Animal.debugln("RATIO CALC: " + name + " wasn't found in animal genome but define in animal's genes ratio");
                } else {
                    float newValue = value - allele.currentValue;
                    affectedGenes.allele1.trueRatioValue = Math.max(Rand.Next(0.01F, 0.05F), affectedGenes.allele1.currentValue + newValue);
                    affectedGenes.allele2.trueRatioValue = Math.max(Rand.Next(0.01F, 0.05F), affectedGenes.allele2.currentValue + newValue);
                }
            }
        }
    }

    public static HashMap<String, AnimalGene> initGenesFromParents(HashMap<String, AnimalGene> femaleGenome, HashMap<String, AnimalGene> maleGenome) {
        HashMap<String, AnimalGene> fullGenome = new HashMap<>();
        if (maleGenome == null || maleGenome.isEmpty()) {
            maleGenome = femaleGenome;
        }

        for (String name : femaleGenome.keySet()) {
            AnimalGene femaleGenes = femaleGenome.get(name);
            AnimalGene maleGenes = maleGenome.get(name);
            if (maleGenes == null) {
                maleGenes = femaleGenes;
            }

            if (femaleGenes == null) {
                femaleGenes = maleGenes;
            }

            AnimalAllele alleleFemale = Rand.NextBool(2) ? femaleGenes.allele1 : femaleGenes.allele2;
            AnimalAllele alleleMale = Rand.NextBool(2) ? maleGenes.allele1 : maleGenes.allele2;
            AnimalGene gene = new AnimalGene();
            gene.name = name;
            gene.allele1 = new AnimalAllele();
            gene.allele1.currentValue = alleleFemale.currentValue;
            gene.allele1.dominant = alleleFemale.dominant;
            gene.allele1.name = alleleFemale.name;
            gene.allele1.geneticDisorder = alleleFemale.geneticDisorder;
            gene.allele2 = new AnimalAllele();
            gene.allele2.currentValue = alleleMale.currentValue;
            gene.allele2.dominant = alleleMale.dominant;
            gene.allele2.name = alleleMale.name;
            gene.allele2.geneticDisorder = alleleMale.geneticDisorder;
            gene.initUsedGene();
            fullGenome.put(name, gene);
        }

        for (String name : fullGenome.keySet()) {
            AnimalGene gene = fullGenome.get(name);
            AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
            doMutation(gene.allele1);
            doMutation(gene.allele2);
        }

        for (String name : fullGenome.keySet()) {
            AnimalGene gene = fullGenome.get(name);
            AnimalGenomeDefinitions def = AnimalGenomeDefinitions.fullGenomeDef.get(gene.name);
            AnimalAllele baseGene = gene.allele1;
            if (gene.allele2.used) {
                baseGene = gene.allele2;
            }

            doRatio(def, fullGenome, baseGene);
        }

        return fullGenome;
    }

    public static void checkGeneticDisorder(IsoAnimal animal) {
        for (int i = 0; i < animal.getFullGenomeList().size(); i++) {
            AnimalGene gene = animal.getFullGenomeList().get(i);
            if (!StringUtils.isNullOrEmpty(gene.allele1.geneticDisorder)
                && gene.allele1.geneticDisorder.equals(gene.allele2.geneticDisorder)
                && !animal.geneticDisorder.contains(gene.allele1.geneticDisorder)) {
                animal.geneticDisorder.add(gene.allele1.geneticDisorder);
            }
        }
    }

    public static void doMutation(AnimalAllele allele) {
        if (Rand.Next(100) <= 10) {
            allele.currentValue = allele.currentValue + (Rand.NextBool(2) ? 0.05F : -0.05F);
        }

        if (Rand.Next(100) <= 5) {
            allele.currentValue += 0.2F;
        }

        if (Rand.Next(100) <= 5) {
            allele.dominant = !allele.dominant;
        }

        if (StringUtils.isNullOrEmpty(allele.geneticDisorder) && Rand.Next(100) <= 2) {
            allele.geneticDisorder = AnimalGenomeDefinitions.geneticDisorder.get(Rand.Next(0, AnimalGenomeDefinitions.geneticDisorder.size()));
        }

        if (Rand.Next(100) <= 2) {
            allele.geneticDisorder = null;
        }

        if (allele.currentValue <= 0.05F) {
            allele.currentValue = 0.05F;
        }
    }

    public String getName() {
        return this.name;
    }

    public AnimalAllele getAllele1() {
        return this.allele1;
    }

    public AnimalAllele getAllele2() {
        return this.allele2;
    }

    public AnimalAllele getUsedGene() {
        return this.allele1.used ? this.allele1 : this.allele2;
    }
}
