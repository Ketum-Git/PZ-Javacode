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
public class RZSHillbillyHoedown extends RandomizedZoneStoryBase {
    public RZSHillbillyHoedown() {
        this.name = "Hillbilly Hoedown";
        this.chance = 10;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getBagClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.Whiskey");
        result.add("Base.BeerCan");
        result.add("Base.BeerBottle");
        result.add("Base.CigaretteSingle");
        result.add("Base.Matches");
        result.add("Base.ChickenFried");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> bagClutter = getBagClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addSimpleFire(midSq);
        int chairNbr = Rand.Next(1, 3);

        for (int i = 0; i < chairNbr; i++) {
            this.addTileObject(this.getRandomExtraFreeSquare(this, zone), "location_restaurant_bar_01_26");
        }

        int nbOfItem = Rand.Next(3, 7);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getHoedownClutterItem());
        }

        this.addZombiesOnSquare(Rand.Next(3, 5), "Farmer", null, this.getRandomExtraFreeSquare(this, zone));
        if (Rand.Next(2) == 0) {
            InventoryContainer bag = InventoryItemFactory.CreateItem("Base.EmptySandbag");
            nbOfItem = Rand.Next(2, 5);

            for (int i = 0; i < nbOfItem; i++) {
                bag.getItemContainer().AddItem(bagClutter.get(Rand.Next(bagClutter.size())));
            }

            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), bag);
        }

        if (Rand.Next(2) == 0) {
            int x = zone.x;
            int y = zone.y;
            if (Rand.Next(2) == 0) {
                x += zone.getWidth();
            }

            if (Rand.Next(2) == 0) {
                y += zone.getHeight();
            }

            BaseVehicle vehicle = this.addVehicle(zone, getSq(x, y, zone.z), null, null, "Base.PickUpTruck", null, null, null);
            if (vehicle != null) {
                vehicle.addKeyToGloveBox();
                vehicle.addKeyToWorld();
                vehicle.setAlarmed(false);
            }
        }
    }
}
