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
public final class RVSBanditRoad extends RandomizedVehicleStoryBase {
    public RVSBanditRoad() {
        this.name = "Bandits on Road";
        this.minZoneWidth = 7;
        this.minZoneHeight = 9;
        this.setMinimumDays(30);
        this.setChance(30);
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
        spawner.addElement("vehicle1", 0.0F, 2.0F, v.getDirection(), 2.0F, 5.0F);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        float vehicle2X = 0.0F;
        float vehicle2Y = -1.5F;
        spawner.addElement("vehicle2", 0.0F, -1.5F, v.getDirection(), 2.0F, 5.0F);
        int numCorpses = Rand.Next(3, 6);

        for (int i = 0; i < numCorpses; i++) {
            float x = Rand.Next(-3.0F, 3.0F);
            float y = Rand.Next(-4.5F, 1.5F);
            spawner.addElement("corpse", x, y, Rand.Next(0.0F, (float) (Math.PI * 2)), 1.0F, 2.0F);
        }

        spawner.setParameter("zone", zone);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            String var6 = element.id;
            switch (var6) {
                case "corpse":
                    BaseVehicle vehicle1x = spawner.getParameter("vehicle1", BaseVehicle.class);
                    if (vehicle1x != null) {
                        createRandomDeadBody(element.position.x, element.position.y, element.z, element.direction, false, 6, 0, null);
                        this.addTrailOfBlood(
                            element.position.x,
                            element.position.y,
                            element.z,
                            Vector2.getDirection(element.position.x - vehicle1x.getX(), element.position.y - vehicle1x.getY()),
                            15
                        );
                    }
                    break;
                case "vehicle1":
                    BaseVehicle vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1 = vehicle1.setSmashed("Front");
                        this.addZombiesOnVehicle(Rand.Next(3, 6), "Bandit", null, vehicle1);
                        spawner.setParameter("vehicle1", vehicle1);
                    }
                    break;
                case "vehicle2":
                    BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null, true);
                    if (vehicle2 != null) {
                        vehicle2.setAlarmed(false);
                        this.addZombiesOnVehicle(Rand.Next(3, 5), null, null, vehicle2);
                        spawner.setParameter("vehicle2", vehicle2);
                    }
            }
        }
    }
}
