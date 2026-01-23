// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSForestCamp extends RandomizedZoneStoryBase {
    public RZSForestCamp() {
        this.name = "Basic Forest Camp";
        this.chance = 10;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getForestClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.Crisps");
        result.add("Base.Crisps2");
        result.add("Base.Crisps3");
        result.add("Base.Crisps4");
        result.add("Base.Pop");
        result.add("Base.Pop2");
        result.add("Base.WaterBottle");
        result.add("Base.CannedSardines");
        result.add("Base.CannedChili");
        result.add("Base.CannedBolognese");
        result.add("Base.CannedCornedBeef");
        result.add("Base.TinnedSoup");
        result.add("Base.TinnedBeans");
        result.add("Base.TunaTin");
        result.add("Base.Whiskey");
        result.add("Base.BeerBottle");
        result.add("Base.BeerCan");
        result.add("Base.BeerCan");
        result.add("Base.Lantern_Propane");
        result.add("Base.Bag_PicnicBasket");
        result.add("Base.GuitarAcoustic");
        result.add("Base.HandAxe");
        result.add("Base.FirewoodBundle");
        return result;
    }

    public static ArrayList<String> getCoolerClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.Pop");
        result.add("Base.Pop2");
        result.add("Base.BeefJerky");
        result.add("Base.Ham");
        result.add("Base.WaterBottle");
        result.add("Base.BeerCan");
        result.add("Base.BeerCan");
        result.add("Base.BeerCan");
        result.add("Base.BeerCan");
        result.add("Base.Smore");
        result.add("Base.WineBox");
        result.add("Base.Marshmallows");
        result.add("Base.HotdogPack");
        result.add("Base.BunsHotdog");
        return result;
    }

    public static ArrayList<String> getFireClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.WaterPotRice");
        result.add("Base.Pot");
        result.add("Base.WaterSaucepanRice");
        result.add("Base.WaterSaucepanPasta");
        result.add("Base.PotOfStew");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> clutter = getForestClutter();
        ArrayList<String> coolerClutter = getCoolerClutter();
        ArrayList<String> fireClutter = getFireClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addCampfireOrPit(midSq);
        this.addItemOnGround(getSq(midX, midY, zone.z), fireClutter.get(Rand.Next(fireClutter.size())));
        int randX = Rand.Next(-1, 2);
        int randY = Rand.Next(-1, 2);
        this.addRandomTentWestEast(midX + randX - 2, midY + randY, zone.z);
        if (Rand.Next(100) < 70) {
            this.addRandomTentNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        if (Rand.Next(100) < 30) {
            this.addRandomTentNorthSouth(midX + randX + 3, midY + randY - 2, zone.z);
        }

        this.addTileObject(midX + 2, midY, zone.z, "furniture_seating_outdoor_01_19");
        InventoryContainer cooler = InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        nbOfItem = Rand.Next(3, 7);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }

        String outfit = "Camper";
        if (Rand.NextBool(2)) {
            outfit = "Backpacker";
        }

        this.addZombiesOnSquare(Rand.Next(1, 3), outfit, null, this.getRandomExtraFreeSquare(this, zone));
    }
}
