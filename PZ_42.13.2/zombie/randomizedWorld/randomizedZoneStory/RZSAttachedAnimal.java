// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSAttachedAnimal extends RandomizedZoneStoryBase {
    public RZSAttachedAnimal() {
        this.name = "Attached Animal";
        this.chance = 5;
        this.minZoneHeight = 8;
        this.minZoneWidth = 8;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.minimumDays = 30;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<>();
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
    public void randomizeZoneStory(Zone zone) {
        LinkedHashMap<String, String> breedFemale = new LinkedHashMap<>();
        LinkedHashMap<String, String> breedMale = new LinkedHashMap<>();
        breedFemale.put("angus", "cow");
        breedFemale.put("simmental", "cow");
        breedFemale.put("holstein", "cow");
        breedFemale.put("landrace", "sow");
        breedFemale.put("largeblack", "sow");
        breedFemale.put("suffolk", "ewe");
        breedFemale.put("rambouillet", "ewe");
        breedFemale.put("friesian", "ewe");
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
        String type = breedFemale.get(breed);
        if (Rand.Next(5) == 0) {
            type = breedMale.get(breed);
        }

        IsoGridSquare square = getRandomExtraFreeUnoccupiedSquare(this, zone);
        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), type, breed);
        animal.addToWorld();
        animal.randomizeAge();
        IsoGridSquare square2 = getRandomExtraFreeUnoccupiedSquare(this, zone);
        IsoObject stake = this.addTileObject(square2.getX(), square2.getY(), square2.getZ(), "location_community_cemetary_01_31");
        if (stake != null) {
            animal.getData().setAttachedTree(stake);
        }
    }
}
