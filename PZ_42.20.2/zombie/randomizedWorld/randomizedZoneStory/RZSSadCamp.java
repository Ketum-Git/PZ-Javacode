// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSSadCamp extends RandomizedZoneStoryBase {
    public RZSSadCamp() {
        this.name = "Sad Campsite";
        this.chance = 5;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getOutfits() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Evacuee");
        result.add("Retiree");
        result.add("Student");
        result.add("Generic01");
        result.add("Generic02");
        result.add("Generic03");
        result.add("Generic04");
        result.add("Generic05");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addCampfire(midSq);
        int randX = Rand.Next(-1, 2);
        int randY = Rand.Next(-1, 2);
        int tentChance = 60;
        int tentsSpawned = 0;
        if (Rand.Next(100) < 60) {
            this.addSleepingBagOrTentNorthSouth(midX + randX - 1, midY + randY - 3, zone.z);
            tentsSpawned++;
        }

        if (Rand.Next(100) < 60) {
            this.addSleepingBagOrTentNorthSouth(midX + randX + 2, midY + randY - 2, zone.z);
            tentsSpawned++;
        }

        if (Rand.Next(100) < 60) {
            this.addSleepingBagOrTentWestEast(midX + randX + 3, midY + randY + 3, zone.z);
            tentsSpawned++;
        }

        if (tentsSpawned < 1 || Rand.Next(100) < 60) {
            this.addSleepingBagOrTentWestEast(midX + randX - 3, midY + randY, zone.z);
            tentsSpawned++;
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
            String vehicleType = switch (Rand.Next(4)) {
                case 1 -> "Base.SUV";
                case 2 -> "Base.CarNormal";
                case 3 -> "Base.CarStationWagon2";
                default -> "Base.CarStationWagon";
            };
            BaseVehicle vehicle = this.addVehicle(zone, getSq(x, y, zone.z), null, "medium", vehicleType, null, null, "Evacuee");
            if (vehicle != null) {
                if (vehicle.getPassengerDoor(0) != null && Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor() != null) {
                    Objects.requireNonNull(vehicle.getPassengerDoor(0)).getDoor().setLocked(false);
                }

                int key = Rand.Next(3);
                if (Rand.NextBool(2)) {
                    vehicle.addKeyToGloveBox();
                } else {
                    vehicle.addKeyToWorld();
                }

                vehicle.setAlarmed(false);
                tentsSpawned++;
            }
        }

        int nbOfItem = Rand.Next(1, 5) + tentsSpawned;

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getSadCampsiteClutterItem());
        }

        int amount = Rand.Next(1 + tentsSpawned / 2, 2 + tentsSpawned);
        String outfitName = getOutfits().get(Rand.Next(getOutfits().size()));

        for (int i = 0; i < amount; i++) {
            this.addZombiesOnSquare(1, outfitName, null, this.getRandomExtraFreeSquare(this, zone));
        }
    }
}
