// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSAmbulanceCrash extends RandomizedVehicleStoryBase {
    public RVSAmbulanceCrash() {
        this.name = "Ambulance Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 7;
        this.setChance(50);
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
                case "vehicle1":
                    BaseVehicle vehicle1 = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, "Base.VanAmbulance", null, null, true
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        this.addZombiesOnVehicle(Rand.Next(1, 3), "AmbulanceDriver", null, vehicle1);
                        ArrayList<IsoZombie> zeds = this.addZombiesOnVehicle(Rand.Next(1, 3), "HospitalPatient", null, vehicle1);

                        for (int i = 0; i < zeds.size(); i++) {
                            for (int j = 0; j < 7; j++) {
                                if (Rand.NextBool(2)) {
                                    zeds.get(i).addVisualBandage(BodyPartType.getRandom(), true);
                                }
                            }
                        }
                    }
                    break;
                case "vehicle2":
                    BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null);
                    if (vehicle2 != null) {
                        vehicle2.setAlarmed(false);
                    }
            }
        }
    }
}
