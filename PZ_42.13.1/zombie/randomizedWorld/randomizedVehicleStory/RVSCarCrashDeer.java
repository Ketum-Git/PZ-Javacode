// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

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
public final class RVSCarCrashDeer extends RandomizedVehicleStoryBase {
    public RVSCarCrashDeer() {
        this.name = "Car Crash Deer";
        this.minZoneWidth = 5;
        this.minZoneHeight = 11;
        this.setChance(10);
        this.needsRuralVegetation = true;
        this.notTown = true;
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
                        String deer = "doe";
                        if (Rand.Next(2) == 0) {
                            deer = "buck";
                        }

                        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), (int)element.position.x, (int)element.position.y, 0, deer, "whitetailed");
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
