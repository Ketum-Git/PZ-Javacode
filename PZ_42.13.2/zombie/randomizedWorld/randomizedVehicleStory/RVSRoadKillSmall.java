// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import zombie.UsedFromLua;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class RVSRoadKillSmall extends RandomizedVehicleStoryBase {
    public RVSRoadKillSmall() {
        this.name = "Roadkill - Small Animal Run Over By Vehicle";
        this.minZoneWidth = 4;
        this.minZoneHeight = 11;
        this.setChance(10);
        this.needsRuralVegetation = true;
        this.notTown = true;
    }

    public static ArrayList<String> getBreeds() {
        ArrayList<String> result = new ArrayList<>();
        result.add("appalachian");
        result.add("cottontail");
        result.add("swamp");
        result.add("grey");
        result.add("meleagris");
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
            String var4 = element.id;
            byte var5 = -1;
            switch (var4.hashCode()) {
                case -1354663044:
                    if (var4.equals("corpse")) {
                        var5 = 0;
                    }
                default:
                    switch (var5) {
                        case 0:
                            LinkedHashMap<String, String> breedFemale = new LinkedHashMap<>();
                            LinkedHashMap<String, String> breedMale = new LinkedHashMap<>();
                            breedFemale.put("appalachian", "rabdoe");
                            breedFemale.put("cottontail", "rabdoe");
                            breedFemale.put("swamp", "rabdoe");
                            breedFemale.put("meleagris", "turkeyhen");
                            breedMale.put("appalachian", "rabbuck");
                            breedMale.put("cottontail", "rabbuck");
                            breedMale.put("swamp", "rabbuck");
                            breedMale.put("meleagris", "gobblers");
                            String breed = spawner.getParameterString("breed");
                            String type = breedFemale.get(breed);
                            if (Rand.NextBool(2)) {
                                type = breedMale.get(breed);
                            }

                            AnimalDefinitions adef = AnimalDefinitions.getDef(type);
                            if (adef == null) {
                                DebugLog.General.warn("can't spawn animal type \"", type, "\"");
                            } else {
                                AnimalBreed breed1 = adef.getBreedByName(breed);
                                if (breed1 == null) {
                                    DebugLog.General.warn("can't spawn animal type/breed \"", type, "\" / \"", breed, "\"");
                                } else {
                                    IsoAnimal animal = new IsoAnimal(
                                        IsoWorld.instance.getCell(), (int)element.position.x, (int)element.position.y, 0, type, breed
                                    );
                                    animal.randomizeAge();
                                    animal.setHealth(0.0F);
                                    this.addTrailOfBlood(element.position.x, element.position.y, element.z, element.direction, 1);
                                }
                            }
                    }
            }
        }
    }
}
