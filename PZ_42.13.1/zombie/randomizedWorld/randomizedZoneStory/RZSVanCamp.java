// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSVanCamp extends RandomizedZoneStoryBase {
    public RZSVanCamp() {
        this.name = "Van Camp";
        this.chance = 2;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
        this.setUnique(true);
    }

    public static ArrayList<String> getBriefcaseClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.HottieZ");
        result.add("Base.Socks_Ankle_Black");
        result.add("Base.Boxers_White");
        result.add("Base.Whiskey");
        result.add("Base.BeerCan");
        result.add("Base.BeerBottle");
        result.add("Base.BeefJerky");
        result.add("Base.TVDinner");
        result.add("Base.BeefJerky");
        result.add("Base.Hotdog");
        result.add("Base.Burger");
        result.add("Base.BaloneySlice");
        result.add("Base.Mustard");
        result.add("Base.Coffee2");
        result.add("Base.Suit_Jacket");
        result.add("Base.Suit_JacketTINT");
        result.add("Base.Trousers_Suit");
        result.add("Base.Tie_Full");
        result.add("Base.ToiletPaper");
        result.add("Base.Shirt_FormalWhite_ShortSleeve");
        result.add("Base.Shirt_FormalWhite");
        result.add("Base.Paperwork");
        result.add("Base.Paperback_SelfHelp");
        result.add("Base.PokerChips");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> briefcaseClutter = getBriefcaseClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.addCampfire(midSq);
        this.addTileObject(midX, midY - 2, zone.z, "furniture_seating_indoor_01_60");
        this.addTileObject(midX - 1, midY + 2, zone.z, "carpentry_02_76");
        this.addTileObject(midX, midY + 2, zone.z, "carpentry_02_77");
        InventoryContainer briefcase = InventoryItemFactory.CreateItem("Base.Briefcase");
        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            briefcase.getItemContainer().AddItem(briefcaseClutter.get(Rand.Next(briefcaseClutter.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), briefcase);
        nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getVanCampClutterItem());
        }

        this.addZombiesOnSquare(1, "OfficeWorker", 0, this.getRandomExtraFreeSquare(this, zone));
        int x = zone.x;
        int y = zone.y;
        if (Rand.Next(2) == 0) {
            x += zone.getWidth();
        }

        if (Rand.Next(2) == 0) {
            y += zone.getHeight();
        }

        BaseVehicle vehicle = this.addVehicle(zone, getSq(x, y, zone.z), null, null, "Base.Van", null, null, null);
        if (vehicle != null) {
            if (vehicle.getPassengerDoor(0) != null && vehicle.getPassengerDoor(0).getDoor() != null) {
                vehicle.getPassengerDoor(0).getDoor().setLocked(false);
            }

            vehicle.addKeyToWorld();
            vehicle.setAlarmed(false);
        }
    }
}
