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
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

/**
 * An utility vehicle (mccoys, fire dept, police, ranger, postal..) with corresponding outfit zeds and sometimes tools
 */
@UsedFromLua
public final class RVSUtilityVehicle extends RandomizedVehicleStoryBase {
    private final RVSUtilityVehicle.Params params = new RVSUtilityVehicle.Params();

    public RVSUtilityVehicle() {
        this.name = "Utility Vehicle";
        this.minZoneWidth = 8;
        this.minZoneHeight = 9;
        this.setChance(70);
    }

    @Override
    public void randomizeVehicleStory(Zone zone, IsoChunk chunk) {
        this.callVehicleStorySpawner(zone, chunk, 0.0F);
    }

    public void doUtilityVehicle(
        Zone zone,
        IsoChunk chunk,
        String zoneName,
        String scriptName,
        String outfits,
        Integer femaleChance,
        String vehicleDistrib,
        ArrayList<String> items,
        int nbrOfItem,
        boolean addTrailer
    ) {
        this.params.zoneName = zoneName;
        this.params.scriptName = scriptName;
        this.params.outfits = outfits;
        this.params.femaleChance = femaleChance;
        this.params.vehicleDistrib = vehicleDistrib;
        this.params.items = items;
        this.params.nbrOfItem = nbrOfItem;
        this.params.addTrailer = addTrailer;
    }

    @Override
    public boolean initVehicleStorySpawner(Zone zone, IsoChunk chunk, boolean debug) {
        int utilityType = Rand.Next(0, 7);
        switch (utilityType) {
            case 0:
                this.doUtilityVehicle(
                    zone, chunk, null, "Base.VanUtility", "ConstructionWorker", 0, "ConstructionWorker", this.getUtilityToolClutter(), Rand.Next(0, 3), true
                );
                break;
            case 1:
                this.doUtilityVehicle(zone, chunk, "police", null, "Police", null, null, null, 0, false);
                break;
            case 2:
                this.doUtilityVehicle(zone, chunk, "fire", null, "Fireman", null, null, null, 0, false);
                break;
            case 3:
                this.doUtilityVehicle(zone, chunk, "ranger", null, "Ranger", null, null, null, 0, true);
                break;
            case 4:
                this.doUtilityVehicle(
                    zone, chunk, "carpenter", null, "ConstructionWorker", 0, "Carpenter", this.getCarpentryToolClutter(), Rand.Next(2, 6), true
                );
                break;
            case 5:
                this.doUtilityVehicle(zone, chunk, "postal", null, "Postal", null, null, null, 0, false);
                break;
            case 6:
                this.doUtilityVehicle(zone, chunk, "fossoil", null, "Fossoil", null, null, null, 0, false);
        }

        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
        spawner.clear();
        Vector2 v = IsoDirections.N.ToVector();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        float vehicleY = -2.0F;
        int vehicleLength = 5;
        spawner.addElement("vehicle1", 0.0F, -2.0F, v.getDirection(), 2.0F, 5.0F);
        if (this.params.addTrailer && Rand.NextBool(7)) {
            int trailerLength = 3;
            spawner.addElement("trailer", 0.0F, 3.0F, v.getDirection(), 2.0F, 3.0F);
        }

        if (this.params.items != null) {
            for (int i = 0; i < this.params.nbrOfItem; i++) {
                spawner.addElement("tool", Rand.Next(-3.5F, 3.5F), Rand.Next(-3.5F, 3.5F), 0.0F, 1.0F, 1.0F);
            }
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
            BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
            String var7 = element.id;
            switch (var7) {
                case "tool":
                    if (vehicle1 != null) {
                        float xoff = PZMath.max(element.position.x - square.x, 0.001F);
                        float yoff = PZMath.max(element.position.y - square.y, 0.001F);
                        float zoff = 0.0F;
                        ItemSpawner.spawnItem(PZArrayUtil.pickRandom(this.params.items), square, xoff, yoff, 0.0F);
                    }
                    break;
                case "trailer":
                    if (vehicle1 != null) {
                        this.addTrailer(
                            vehicle1,
                            zone,
                            square.getChunk(),
                            this.params.zoneName,
                            this.params.vehicleDistrib,
                            Rand.NextBool(1) ? "Base.Trailer" : "Base.TrailerCover"
                        );
                    }
                    break;
                case "vehicle1":
                    vehicle1 = this.addVehicle(
                        zone,
                        element.position.x,
                        element.position.y,
                        z,
                        element.direction,
                        this.params.zoneName,
                        this.params.scriptName,
                        null,
                        this.params.vehicleDistrib,
                        true
                    );
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        String victimOutfit = this.params.outfits;
                        if (vehicle1.getZombieType() != null) {
                            victimOutfit = vehicle1.getRandomZombieType();
                        }

                        this.addZombiesOnVehicle(Rand.Next(2, 5), victimOutfit, this.params.femaleChance, vehicle1);
                    }
            }
        }
    }

    private static final class Params {
        String zoneName;
        String scriptName;
        String outfits;
        Integer femaleChance;
        String vehicleDistrib;
        ArrayList<String> items;
        int nbrOfItem;
        boolean addTrailer;
    }
}
