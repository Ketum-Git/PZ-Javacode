// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RVSTrailerCrash extends RandomizedVehicleStoryBase {
    public RVSTrailerCrash() {
        this.name = "Trailer Crash";
        this.minZoneWidth = 5;
        this.minZoneHeight = 12;
        this.setChance(45);
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
        float vehicle1X = 0.0F;
        float vehicle1Y = -1.5F;
        spawner.addElement("vehicle1", 0.0F, -1.5F, v.getDirection(), 2.0F, 5.0F);
        int trailerLength = 4;
        spawner.addElement("trailer", 0.0F, 4.0F, v.getDirection(), 2.0F, 4.0F);
        boolean east = Rand.NextBool(2);
        v = east ? IsoDirections.E.ToVector() : IsoDirections.W.ToVector();
        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        float vehicle2X = 0.0F;
        float vehicle2Y = -5.0F;
        spawner.addElement("vehicle2", 0.0F, -5.0F, v.getDirection(), 2.0F, 5.0F);
        spawner.setParameter("zone", zone);
        spawner.setParameter("east", east);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            boolean east = spawner.getParameterBoolean("east");
            String var7 = element.id;
            switch (var7) {
                case "vehicle1":
                    BaseVehicle vehicle1 = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, "Base.PickUpVan", null, null, true
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1 = vehicle1.setSmashed("Front");
                        ArrayList<String> trailers = new ArrayList<>();
                        trailers.add("Base.Trailer");
                        trailers.add("Base.TrailerCover");
                        trailers.add("Base.Trailer_Livestock");
                        String trailerType = trailers.get(Rand.Next(trailers.size()));
                        if (Rand.NextBool(6)) {
                            trailerType = "Base.TrailerAdvert";
                        }

                        BaseVehicle trailer = this.addTrailer(vehicle1, zone, square.getChunk(), null, null, trailerType);
                        if (trailer != null && Rand.NextBool(3)) {
                            trailer.setAngles(trailer.getAngleX(), Rand.Next(90.0F, 110.0F), trailer.getAngleZ());
                        }

                        if (Rand.Next(10) < 4) {
                            this.addZombiesOnVehicle(Rand.Next(2, 5), null, null, vehicle1);
                        }

                        spawner.setParameter("vehicle1", vehicle1);
                    }
                    break;
                case "vehicle2":
                    BaseVehicle vehicle2 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "bad", null, null, null);
                    if (vehicle2 != null) {
                        vehicle2.setAlarmed(false);
                        String location = east ? "Right" : "Left";
                        vehicle2 = vehicle2.setSmashed(location);
                        vehicle2.setBloodIntensity(location, 1.0F);
                        spawner.setParameter("vehicle2", vehicle2);
                    }
            }
        }
    }
}
