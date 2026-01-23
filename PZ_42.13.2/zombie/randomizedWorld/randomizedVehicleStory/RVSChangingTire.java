// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemSpawner;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * Good car with a couple changing its tire
 */
@UsedFromLua
public final class RVSChangingTire extends RandomizedVehicleStoryBase {
    public RVSChangingTire() {
        this.name = "Changing Tire";
        this.minZoneWidth = 5;
        this.minZoneHeight = 5;
        this.setChance(30);
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
        boolean removeRightWheel = Rand.NextBool(2);
        if (debug) {
            removeRightWheel = true;
        }

        int r = removeRightWheel ? 1 : -1;
        Vector2 v = IsoDirections.N.ToVector();
        spawner.addElement("vehicle1", r * -1.5F, 0.0F, v.getDirection(), 2.0F, 5.0F);
        spawner.addElement("tire1", r * 0.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        spawner.addElement("tool1", r * 0.8F, -0.2F, 0.0F, 1.0F, 1.0F);
        spawner.addElement("tool2", r * 1.2F, 0.2F, 0.0F, 1.0F, 1.0F);
        spawner.addElement("tire2", r * 2.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        spawner.setParameter("zone", zone);
        spawner.setParameter("removeRightWheel", removeRightWheel);
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
            boolean removeRightWheel = spawner.getParameterBoolean("removeRightWheel");
            BaseVehicle vehicle1 = spawner.getParameter("vehicle1", BaseVehicle.class);
            String var11 = element.id;
            switch (var11) {
                case "tire1":
                    if (vehicle1 != null) {
                        InventoryItem newTire = ItemSpawner.spawnItem("Base.ModernTire" + vehicle1.getScript().getMechanicType(), square, xoff, yoff, 0.0F);
                        if (newTire != null) {
                            newTire.setItemCapacity(newTire.getMaxCapacity());
                        }

                        this.addBloodSplat(square, Rand.Next(10, 20));
                    }
                    break;
                case "tire2":
                    if (vehicle1 != null) {
                        InventoryItem oldTire = ItemSpawner.spawnItem("Base.OldTire" + vehicle1.getScript().getMechanicType(), square, xoff, yoff, 0.0F);
                        if (oldTire != null) {
                            oldTire.setCondition(0, false);
                        }
                    }
                    break;
                case "tool1":
                    if (Rand.Next(2) == 0) {
                        ItemSpawner.spawnItem("Base.LugWrench", square, xoff, yoff, 0.0F);
                    } else {
                        ItemSpawner.spawnItem("Base.TireIron", square, xoff, yoff, 0.0F);
                    }
                    break;
                case "tool2":
                    ItemSpawner.spawnItem("Base.Jack", square, xoff, yoff, 0.0F);
                    break;
                case "vehicle1":
                    vehicle1 = this.addVehicle(zone, element.position.x, element.position.y, z, element.direction, "good", null, null, null, true);
                    if (vehicle1 != null) {
                        vehicle1.setAlarmed(false);
                        vehicle1.setGeneralPartCondition(0.7F, 40.0F);
                        vehicle1.setRust(0.0F);
                        VehiclePart part = vehicle1.getPartById(removeRightWheel ? "TireRearRight" : "TireRearLeft");
                        vehicle1.setTireRemoved(part.getWheelIndex(), true);
                        part.setModelVisible("InflatedTirePlusWheel", false);
                        part.setInventoryItem(null);
                        this.addZombiesOnVehicle(2, null, null, vehicle1);
                        spawner.setParameter("vehicle1", vehicle1);
                    }
            }
        }
    }
}
