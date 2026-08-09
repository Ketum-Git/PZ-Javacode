// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSSurvivalistCamp extends RandomizedZoneStoryBase {
    public RZSSurvivalistCamp() {
        this.name = "Survivalist Campsite";
        this.chance = 2;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addSimpleFire(midSq);
        int randX = Rand.Next(0, 1);
        int randY = Rand.Next(0, 1);
        if (Rand.NextBool(2)) {
            this.addRandomShelterWestEast(midX + randX - 2, midY + randY, zone.z);
        } else {
            this.addRandomShelterNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getSurvivalistCampsiteClutterItem());
        }
        String outfitName = switch (Rand.Next(4)) {
            case 1 -> "Survivalist02";
            case 2 -> "Survivalist03";
            case 3 -> "Survivalist04";
            case 4 -> "Survivalist05";
            default -> "Survivalist";
        };
        this.addZombiesOnSquare(1, outfitName, null, this.getRandomExtraFreeSquare(this, zone));
        ArrayList<String> bags = new ArrayList<>();
        bags.add("Base.Bag_FoodCanned");
        bags.add("Base.Bag_SurvivorBag");
        bags.add("Base.Bag_WeaponBag");
        bags.add("Base.Bag_MedicalBag");
        bags.add("Base.Bag_Sheriff");
        bags.add("Base.Bag_ProtectiveCaseBulky_Survivalist");
        bags.add("Base.Bag_ProtectiveCaseBulkyAmmo");

        for (int i = 0; i < bags.size(); i++) {
            if (Rand.NextBool(4)) {
                InventoryContainer bag = InventoryItemFactory.CreateItem(bags.get(i));
                IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
                if (sq != null && bag != null) {
                    this.addItemOnGround(sq, bag);
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                }
            }
        }

        if (Rand.NextBool(2)) {
            int x = zone.x;
            int y = zone.y;
            if (Rand.Next(2) == 0) {
                x += zone.getWidth();
            }

            if (Rand.Next(2) == 0) {
                y += zone.getHeight();
            }
            String vehicleType = switch (Rand.Next(6)) {
                case 1 -> "Base.SUV";
                case 2 -> "Base.PickUpVan";
                case 3 -> "Base.PickUpTruck";
                case 4 -> "Base.PickUpVan_Camo";
                case 5 -> "Base.PickUpTruck_Camo";
                default -> "Base.OffRoad";
            };
            BaseVehicle vehicle = this.addVehicle(zone, getSq(x, y, zone.z), null, null, vehicleType, null, null, "Survivalist");
            if (vehicle != null) {
                if (vehicle.getPassengerDoor(0) != null && Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor() != null) {
                    Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor().setLocked(false);
                }

                int key = Rand.Next(3);
                if (key == 0) {
                    vehicle.setHotwired(true);
                } else if (key == 1) {
                    vehicle.addKeyToGloveBox();
                } else if (key == 2) {
                    vehicle.addKeyToWorld();
                }

                vehicle.setAlarmed(false);
            }
        }
    }
}
