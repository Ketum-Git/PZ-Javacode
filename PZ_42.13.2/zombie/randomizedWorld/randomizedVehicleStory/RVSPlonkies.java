// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class RVSPlonkies extends RandomizedVehicleStoryBase {
    public RVSPlonkies() {
        this.name = "Plonkies";
        this.minZoneWidth = 5;
        this.minZoneHeight = 5;
        this.setChance(1);
        this.setUnique(true);
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
        boolean damageRightWheel = Rand.NextBool(2);
        Vector2 v = IsoDirections.N.ToVector();
        float RAND_ANGLE = (float) (Math.PI / 6);
        if (debug) {
            RAND_ANGLE = 0.0F;
        }

        v.rotate(Rand.Next(-RAND_ANGLE, RAND_ANGLE));
        spawner.addElement("vehicle1", -r * 2.0F, 0.0F, v.getDirection(), 2.0F, 5.0F);
        float direction = 0.0F;
        int nbrOfBags = Rand.Next(2, 5);

        for (int i = 0; i < nbrOfBags; i++) {
            spawner.addElement("bag", r * Rand.Next(0.0F, 3.0F), -Rand.Next(0.7F, 2.3F), 0.0F, 1.0F, 1.0F);
        }

        spawner.setParameter("damageRightWheel", damageRightWheel);
        spawner.setParameter("zone", zone);
        return true;
    }

    @Override
    public void spawnElement(VehicleStorySpawner spawner, VehicleStorySpawner.Element element) {
        IsoGridSquare square = element.square;
        if (square != null) {
            float z = element.z;
            Zone zone = spawner.getParameter("zone", Zone.class);
            boolean damageRightWheel = spawner.getParameterBoolean("damageRightWheel");
            BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
            String var8 = element.id;
            switch (var8) {
                case "bag":
                    if (vehicle1 != null) {
                        String itemType = "Base.Plonkies";
                        InventoryItem bag = InventoryItemFactory.CreateItem("Base.Plonkies");
                        this.addItemOnGround(square, bag);
                    }
                    break;
                case "vehicle1":
                    vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, null, "StepVan_Plonkies", null, null, true);
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1.setGeneralPartCondition(0.7F, 40.0F);
                        vehicle1.setRust(0.0F);
                        VehiclePart part = vehicle1.getPartById(damageRightWheel ? "TireRearRight" : "TireRearLeft");
                        part.setCondition(0);
                        VehiclePart part2 = vehicle1.getPartById("GasTank");
                        part2.setContainerContentAmount(0.0F);
                        String outfit = "PlonkiesGuy";
                        this.addZombiesOnVehicle(1, "PlonkiesGuy", 0, vehicle1);
                        spawner.setParameter("vehicle1", vehicle1);
                        vehicle1.addKeyToWorld();
                    }
            }
        }
    }
}
