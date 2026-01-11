// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.ItemSpawner;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

/**
 * Van with a sewer hole & road cones around it, some construction worker and a foreman + some tools in ground
 */
@UsedFromLua
public final class RVSConstructionSite extends RandomizedVehicleStoryBase {
    private ArrayList<String> tools;

    public RVSConstructionSite() {
        this.name = "Construction Site";
        this.minZoneWidth = 6;
        this.minZoneHeight = 6;
        this.setChance(30);
        this.tools = new ArrayList<>();
        this.tools.add("Base.PickAxe");
        this.tools.add("Base.Shovel");
        this.tools.add("Base.Shovel2");
        this.tools.add("Base.Hammer");
        this.tools.add("Base.LeadPipe");
        this.tools.add("Base.PipeWrench");
        this.tools.add("Base.Sledgehammer");
        this.tools.add("Base.Sledgehammer2");
        this.needsPavement = true;
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        boolean manholeOnRight = Rand.NextBool(2);
        if (debug) {
            manholeOnRight = true;
        }

        int r = manholeOnRight ? 1 : -1;
        Vector2 v = IsoDirections.N.ToVector();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", -r * 2.0F, 0.0F, v.getDirection(), 2.0F, 5.0F);
        float direction = 0.0F;
        spawner.addElement("manhole", r * 1.5F, 1.5F, direction, 3.0F, 3.0F);
        int nbrOfTools = Rand.Next(0, 3);

        for (int i = 0; i < nbrOfTools; i++) {
            direction = 0.0F;
            spawner.addElement("tool", r * Rand.Next(0.0F, 3.0F), -Rand.Next(0.7F, 2.3F), direction, 1.0F, 1.0F);
        }

        spawner.setParameter("zone", zone);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float xoff = PZMath.max(element.position.x - square.x, 0.001F);
            float yoff = PZMath.max(element.position.y - square.y, 0.001F);
            float zoff = 0.0F;
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
            String var10 = element.id;
            switch (var10) {
                case "manhole":
                    square.AddTileObject(IsoObject.getNew(square, "street_decoration_01_15", null, false));
                    IsoGridSquare sq = square.getAdjacentSquare(IsoDirections.E);
                    if (sq != null) {
                        sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                    }

                    sq = square.getAdjacentSquare(IsoDirections.W);
                    if (sq != null) {
                        sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                    }

                    sq = square.getAdjacentSquare(IsoDirections.S);
                    if (sq != null) {
                        sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                    }

                    sq = square.getAdjacentSquare(IsoDirections.N);
                    if (sq != null) {
                        sq.AddTileObject(IsoObject.getNew(sq, "street_decoration_01_26", null, false));
                    }
                    break;
                case "tool":
                    String itemType = this.tools.get(Rand.Next(this.tools.size()));
                    ItemSpawner.spawnItem(itemType, square, xoff, yoff, 0.0F);
                    break;
                case "vehicle1":
                    ArrayList<String> vehicles = new ArrayList<>();
                    vehicles.add("Base.PickUpTruck");
                    vehicles.add("Base.VanUtility");
                    String vehicleType = vehicles.get(Rand.Next(vehicles.size()));
                    vehicle1 = this.addVehicle(
                        zone, element.position.x, element.position.y, z, element.direction, null, vehicleType, null, "ConstructionWorker", true
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        String victimOutfit = "ConstructionWorker";
                        if (vehicle1.getZombieType() != null) {
                            victimOutfit = vehicle1.getRandomZombieType();
                        }

                        this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, 0, vehicle1);
                        this.addZombiesOnVehicle(1, "Foreman", 0, vehicle1);
                        spawner.setParameter("vehicle1", vehicle1);
                    }
            }
        }
    }
}
