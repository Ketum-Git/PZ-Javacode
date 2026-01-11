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
public final class RVSCarCrash extends RandomizedVehicleStoryBase {
    public RVSCarCrash() {
        this.name = "Basic Car Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 7;
        this.setChance(250);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        Vector2 v = IsoDirections.N.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", 0.0F, 1.0F, v.getDirection(), 2.0F, 5.0F);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle2", 0.0F, -2.5F, v.getDirection(), 2.0F, 5.0F);
        spawner.setParameter("zone", zone);
        spawner.setParameter("smashed", Rand.NextBool(3));
        spawner.setParameter("east", east);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            boolean smashed = spawner.getParameterBoolean("smashed");
            boolean vehicle2East = spawner.getParameterBoolean("east");
            String var8 = element.id;
            switch (var8) {
                case "vehicle1":
                case "vehicle2":
                    BaseVehicle vehicle = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                    if (vehicle != null) {
                        vehicle.setAlarmed(false);
                        if (smashed) {
                            String location = "Front";
                            if ("vehicle2".equals(element.id)) {
                                location = vehicle2East ? "Right" : "Left";
                            }

                            vehicle = vehicle.setSmashed(location);
                            vehicle.setBloodIntensity(location, 1.0F);
                        }

                        if ("vehicle1".equals(element.id) && Rand.Next(10) < 4) {
                            String victimOutfit = null;
                            if (vehicle.getZombieType() != null) {
                                victimOutfit = vehicle.getRandomZombieType();
                            }

                            this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, null, vehicle);
                        }
                    }
            }
        }
    }
}
