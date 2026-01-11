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
import zombie.vehicles.VehiclePart;

/**
 * Police barricading a road, 2 police cars, some zombies police
 */
@UsedFromLua
public final class RVSPoliceBlockade extends RandomizedVehicleStoryBase {
    public RVSPoliceBlockade() {
        this.name = "Police Blockade";
        this.minZoneWidth = 8;
        this.minZoneHeight = 8;
        this.setChance(30);
        this.setMaximumDays(30);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        float RAND_ANGLE = (float) (Math.PI / 18);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        float xOffset = 1.5F;
        float yOffset = 1.0F;
        if (this.zoneWidth >= 10) {
            xOffset = 2.5F;
            yOffset = 0.0F;
        }

        IsoDirections firstCarDir = Rand.NextBool(2) ? IsoDirections.W : IsoDirections.E;
        Vector2 v = firstCarDir.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", -xOffset, yOffset, v.getDirection(), 2.0F, 5.0F);
        v = firstCarDir.Rot180().ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle2", xOffset, -yOffset, v.getDirection(), 2.0F, 5.0F);
        String scriptName = "Base.CarLightsPolice";
        if (Rand.NextBool(3)) {
            scriptName = "Base.PickUpVanLightsPolice";
        }

        spawner.setParameter("zone", zone);
        spawner.setParameter("script", scriptName);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            String scriptName = spawner.getParameterString("script");
            String var7 = element.id;
            switch (var7) {
                case "vehicle1":
                case "vehicle2":
                    BaseVehicle vehicle = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, scriptName, null, null, true
                    );
                    if (vehicle != null) {
                        vehicle.setAlarmed(false);
                        if (Rand.NextBool(3)) {
                            vehicle.setHeadlightsOn(true);
                            vehicle.setLightbarLightsMode(2);
                            VehiclePart battery = vehicle.getBattery();
                            if (battery != null) {
                                battery.setLastUpdated(0.0F);
                            }
                        }

                        String outfit = "Police";
                        if (vehicle.getZombieType() != null) {
                            outfit = vehicle.getRandomZombieType();
                        }

                        this.addZombiesOnVehicle(Rand.Next(2, 4), outfit, null, vehicle);
                    }
            }
        }
    }
}
