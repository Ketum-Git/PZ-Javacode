// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSCarCrashCorpse extends RandomizedVehicleStoryBase {
    /**
     * Vehicle alone, corpse not so far from the car's front with blood trail
     */
    public RVSCarCrashCorpse() {
        this.name = "Basic Car Crash Corpse";
        this.minZoneWidth = 6;
        this.minZoneHeight = 11;
        this.setChance(100);
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
                        createRandomDeadBody(element.position.x, element.position.y, element.z, element.direction, false, 35, 30, null);
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
