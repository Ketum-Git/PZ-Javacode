// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSRoadKill extends RandomizedVehicleStoryBase {
    public RVSRoadKill() {
        this.name = "Roadkill - Large Farm Animal Struck By Vehicle";
        this.minZoneWidth = 5;
        this.minZoneHeight = 11;
        this.setChance(10);
        this.setMinimumDays(30);
        this.needsFarmland = true;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<>();
        result.add("angus");
        result.add("simmental");
        result.add("holstein");
        result.add("landrace");
        result.add("largeblack");
        return result;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        float RAND_ANGLE = (float) (Math.PI / 6);
        this.callVehicleStorySpawner(zone, chunk, Rand.Next((float) (-Math.PI / 6), (float) (Math.PI / 6)));
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float vehicleY = 2.5F;
        spawner.addElement("vehicle1", 0.0F, 2.5F, v.getDirection(), 2.0F, 5.0F);
        spawner.addElement("corpse", 0.0F, 2.5F - (debug ? 7 : Rand.Next(4, 7)), v.getDirection() + (float) Math.PI, 1.0F, 2.0F);
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
            Zone zone = spawner.getParameter("zone", Zone.class);
            BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
            String var7 = element.id;
            switch (var7) {
                case "corpse":
                    if (vehicle1 != null) {
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
                        String breed = spawner.getParameterString("breed");
                        String type = breedFemale.get(breed);
                        if (Rand.Next(5) == 0) {
                            type = breedMale.get(breed);
                        }

                        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), (int)element.position.x, (int)element.position.y, 0, type, breed);
                        animal.randomizeAge();
                        animal.setHealth(0.0F);
                        this.addTrailOfBlood(element.position.x, element.position.y, element.z, element.direction, 15);
                    }
                    break;
                case "vehicle1":
                    vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1 = vehicle1.setSmashed("Front");
                        vehicle1.setBloodIntensity("Front", 1.0F);
                        spawner.setParameter("vehicle1", vehicle1);
                    }
            }
        }
    }
}
