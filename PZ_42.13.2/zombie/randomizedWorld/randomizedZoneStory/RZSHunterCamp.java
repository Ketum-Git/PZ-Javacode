// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSHunterCamp extends RandomizedZoneStoryBase {
    public RZSHunterCamp() {
        this.name = "Hunter Forest Camp";
        this.chance = 5;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getForestClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.VarmintRifle");
        result.add("Base.223Box");
        result.add("Base.HuntingRifle");
        result.add("Base.308Box");
        result.add("Base.Shotgun");
        result.add("Base.ShotgunShellsBox");
        result.add("Base.DoubleBarrelShotgun");
        result.add("Base.AssaultRifle");
        result.add("Base.556Box");
        result.add("Base.Lantern_Propane");
        result.add("Base.Bag_RifleCaseCloth");
        result.add("Base.Bag_RifleCaseCloth2");
        result.add("Base.Bag_ShotgunCaseCloth");
        result.add("Base.RifleCase4");
        result.add("Base.Bag_AmmoBox_Hunting");
        result.add("Base.HandAxe");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> clutter = getForestClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addSimpleFire(midSq);

        String vehicleType = switch (Rand.Next(5)) {
            case 1 -> "Base.PickUpVan";
            case 2 -> "Base.PickUpTruck";
            case 3 -> "Base.PickUpVan_Camo";
            case 4 -> "Base.PickUpTruck_Camo";
            default -> "Base.OffRoad";
        };
        int x2 = zone.x;
        int y2 = zone.y;
        if (Rand.Next(2) == 0) {
            x2 += zone.getWidth();
        }

        if (Rand.Next(2) == 0) {
            y2 += zone.getHeight();
        }

        BaseVehicle vehicle = this.addVehicle(zone, getSq(x2, y2, 0), null, null, vehicleType, null, null, "Hunter");
        if (vehicle != null) {
            if (vehicle.getPassengerDoor(0) != null && Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor() != null) {
                Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor().setLocked(false);
            }

            vehicle.addKeyToWorld();
            vehicle.setAlarmed(false);
        }

        int randX = Rand.Next(-1, 2);
        int randY = Rand.Next(-1, 2);
        this.addRandomTentWestEast(midX + randX - 2, midY + randY, zone.z);
        if (Rand.Next(100) < 70) {
            this.addRandomTentNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        if (Rand.Next(100) < 30) {
            this.addRandomTentNorthSouth(midX + randX + 3, midY + randY - 2, zone.z);
        }

        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }

        this.addZombiesOnSquare(Rand.Next(2, 5), "Hunter", 0, this.getRandomExtraFreeSquare(this, zone));
        if (Rand.Next(2) == 0) {
            IsoGridSquare square = this.getRandomExtraFreeSquare(this, zone);
            if (square != null) {
                String breed;
                String type;
                if (Rand.Next(2) == 0) {
                    breed = "whitetailed";
                    type = "doe";
                    if (Rand.Next(2) == 0) {
                        type = "buck";
                    }
                } else {
                    breed = "meleagris";
                    type = "turkeyhen";
                    if (Rand.Next(2) == 0) {
                        type = "gobblers";
                    }
                }

                IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), zone.z, type, breed);
                animal.randomizeAge();
                animal.setHealth(0.0F);
            }
        }
    }
}
