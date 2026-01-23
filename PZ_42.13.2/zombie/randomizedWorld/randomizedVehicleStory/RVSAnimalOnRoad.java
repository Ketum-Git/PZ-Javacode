// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class RVSAnimalOnRoad extends RandomizedVehicleStoryBase {
    public RVSAnimalOnRoad() {
        this.name = "Animal On Road";
        this.minZoneWidth = 2;
        this.minZoneHeight = 2;
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
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        spawner.addElement("animal", 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        spawner.setParameter("zone", zone);
        ArrayList<String> breeds = getBreeds();
        String breed = breeds.get(Rand.Next(breeds.size()));
        spawner.setParameter("breed", breed);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            String var5 = element.id;
            byte var6 = -1;
            switch (var5.hashCode()) {
                case -1413116420:
                    if (var5.equals("animal")) {
                        var6 = 0;
                    }
                default:
                    switch (var6) {
                        case 0:
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
                            if (square != null) {
                                String breed = spawner.getParameterString("breed");
                                String type = breedFemale.get(breed);
                                if (Rand.NextBool(5)) {
                                    type = breedMale.get(breed);
                                }

                                IsoAnimal animal = new IsoAnimal(
                                    IsoWorld.instance.getCell(),
                                    PZMath.fastfloor(element.position.x),
                                    PZMath.fastfloor(element.position.y),
                                    PZMath.fastfloor(z),
                                    type,
                                    breed
                                );
                                animal.addToWorld();
                                animal.randomizeAge();
                            }
                    }
            }
        }
    }
}
