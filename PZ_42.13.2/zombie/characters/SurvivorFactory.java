// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;

@UsedFromLua
public final class SurvivorFactory {
    public static final ArrayList<String> FemaleForenames = new ArrayList<>();
    public static final ArrayList<String> MaleForenames = new ArrayList<>();
    public static final ArrayList<String> Surnames = new ArrayList<>();

    public static void Reset() {
        FemaleForenames.clear();
        MaleForenames.clear();
        Surnames.clear();
        SurvivorDesc.HairCommonColors.clear();
        SurvivorDesc.TrouserCommonColors.clear();
    }

    public static SurvivorDesc[] CreateFamily(int nCount) {
        SurvivorDesc[] survivors = new SurvivorDesc[nCount];

        for (int n = 0; n < nCount; n++) {
            survivors[n] = CreateSurvivor();
            if (n > 0) {
                survivors[n].setSurname(survivors[0].getSurname());
            }
        }

        return survivors;
    }

    public static SurvivorDesc CreateSurvivor() {
        switch (Rand.Next(3)) {
            case 0:
                return CreateSurvivor(SurvivorFactory.SurvivorType.Friendly);
            case 1:
                return CreateSurvivor(SurvivorFactory.SurvivorType.Neutral);
            case 2:
                return CreateSurvivor(SurvivorFactory.SurvivorType.Aggressive);
            default:
                return null;
        }
    }

    public static SurvivorDesc CreateSurvivor(SurvivorFactory.SurvivorType survivorType, boolean bFemale) {
        SurvivorDesc survivor = new SurvivorDesc();
        survivor.setType(survivorType);
        IsoGameCharacter.getSurvivorMap().put(survivor.getID(), survivor);
        survivor.setFemale(bFemale);
        randomName(survivor);
        if (survivor.isFemale()) {
            setTorso(survivor);
        } else {
            setTorso(survivor);
        }

        return survivor;
    }

    public static void setTorso(SurvivorDesc survivor) {
        if (survivor.isFemale()) {
            survivor.setTorso("Kate");
        } else {
            survivor.setTorso("Male");
        }
    }

    public static SurvivorDesc CreateSurvivor(SurvivorFactory.SurvivorType survivorType) {
        return CreateSurvivor(survivorType, Rand.Next(2) == 0);
    }

    public static SurvivorDesc[] CreateSurvivorGroup(int nCount) {
        SurvivorDesc[] survivors = new SurvivorDesc[nCount];

        for (int n = 0; n < nCount; n++) {
            survivors[n] = CreateSurvivor();
        }

        return survivors;
    }

    public static IsoSurvivor InstansiateInCell(SurvivorDesc desc, IsoCell cell, int x, int y, int z) {
        desc.setInstance(new IsoSurvivor(desc, cell, x, y, z));
        return (IsoSurvivor)desc.getInstance();
    }

    public static void randomName(SurvivorDesc desc) {
        if (desc.isFemale()) {
            desc.setForename(FemaleForenames.get(Rand.Next(FemaleForenames.size())));
        } else {
            desc.setForename(MaleForenames.get(Rand.Next(MaleForenames.size())));
        }

        desc.setSurname(Surnames.get(Rand.Next(Surnames.size())));
    }

    public static void addSurname(String surName) {
        Surnames.add(Translator.getText("SurvivorSurname_" + surName));
    }

    public static void addFemaleForename(String forename) {
        FemaleForenames.add(Translator.getText("SurvivorName_" + forename));
    }

    public static void addMaleForename(String forename) {
        MaleForenames.add(Translator.getText("SurvivorName_" + forename));
    }

    public static String getRandomSurname() {
        return Surnames.get(Rand.Next(Surnames.size()));
    }

    public static String getRandomForename(boolean bFemale) {
        return bFemale ? FemaleForenames.get(Rand.Next(FemaleForenames.size())) : MaleForenames.get(Rand.Next(MaleForenames.size()));
    }

    @UsedFromLua
    public static enum SurvivorType {
        Friendly,
        Neutral,
        Aggressive;
    }
}
