// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class RVSHerdOnRoad extends RandomizedVehicleStoryBase {
    public RVSHerdOnRoad() {
        this.name = "Herd On Road";
        this.minZoneWidth = 4;
        this.minZoneHeight = 4;
        this.setChance(10);
        this.setMinimumDays(30);
        this.needsFarmland = true;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<>();
        result.add("rhodeisland");
        result.add("leghorn");
        result.add("angus");
        result.add("simmental");
        result.add("holstein");
        result.add("landrace");
        result.add("largeblack");
        result.add("suffolk");
        result.add("rambouillet");
        result.add("friesian");
        return result;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        LinkedHashMap<String, String> breedFemale = new LinkedHashMap<>();
        LinkedHashMap<String, String> breedMale = new LinkedHashMap<>();
        breedFemale.put("rhodeisland", "hen");
        breedFemale.put("leghorn", "hen");
        breedFemale.put("angus", "cow");
        breedFemale.put("simmental", "cow");
        breedFemale.put("holstein", "cow");
        breedFemale.put("landrace", "sow");
        breedFemale.put("largeblack", "sow");
        breedFemale.put("suffolk", "ewe");
        breedFemale.put("rambouillet", "ewe");
        breedFemale.put("friesian", "ewe");
        breedMale.put("rhodeisland", "cockerel");
        breedMale.put("leghorn", "cockerel");
        breedMale.put("angus", "bull");
        breedMale.put("simmental", "bull");
        breedMale.put("holstein", "bull");
        breedMale.put("landrace", "boar");
        breedMale.put("largeblack", "boar");
        breedMale.put("suffolk", "ram");
        breedMale.put("rambouillet", "ram");
        breedMale.put("friesian", "ram");
        ArrayList<String> breeds = getBreeds();
        String breed = breeds.get(Rand.Next(breeds.size()));
        String type = breedMale.get(breed);
        IsoGridSquare sq = this.getCenterOfChunk(zone, chunk);
        IsoGridSquare square = getRandomFreeUnoccupiedSquare(this, zone, sq);
        IsoAnimal animal = null;
        if (square != null) {
            animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), type, breed);
            animal.addToWorld();
            animal.randomizeAge();
            if (Rand.NextBool(2)) {
                animal.setStateEventDelayTimer(0.0F);
            }
        }

        type = breedFemale.get(breed);
        int moreAnimals = Rand.Next(1, 6);

        for (int i = 0; i < moreAnimals; i++) {
            square = getRandomFreeUnoccupiedSquare(this, zone, sq);
            if (square != null) {
                animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), type, breed);
                animal.addToWorld();
                animal.randomizeAge();
                if (Rand.NextBool(2)) {
                    animal.setStateEventDelayTimer(0.0F);
                }
            }
        }
    }
}
