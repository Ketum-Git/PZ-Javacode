// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSFishingTrip extends RandomizedZoneStoryBase {
    public RZSFishingTrip() {
        this.name = "Fishing Trip";
        this.chance = 10;
        this.minZoneHeight = 8;
        this.minZoneWidth = 8;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
    }

    public static ArrayList<String> getFishes() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.BlueCatfish");
        result.add("Base.ChannelCatfish");
        result.add("Base.FlatheadCatfish");
        result.add("Base.LargemouthBass");
        result.add("Base.SmallmouthBass");
        result.add("Base.WhiteBass");
        result.add("Base.StripedBass");
        result.add("Base.SpottedBass");
        result.add("Base.YellowPerch");
        result.add("Base.WhiteCrappie");
        result.add("Base.BlackCrappie");
        result.add("Base.BaitFish");
        return result;
    }

    public static ArrayList<String> getFishingTools() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.Bobber");
        result.add("Base.Bobber");
        result.add("Base.JigLure");
        result.add("Base.JigLure");
        result.add("Base.MinnowLure");
        result.add("Base.MinnowLure");
        result.add("Base.FishingHook");
        result.add("Base.FishingHook");
        result.add("Base.FishingHookBox");
        result.add("Base.FishingLine");
        result.add("Base.FishingLine");
        result.add("Base.PremiumFishingLine");
        result.add("Base.FishingNet");
        result.add("Base.FishingNet");
        result.add("Base.Worm");
        result.add("Base.Worm");
        result.add("Base.Worm");
        result.add("Base.Worm");
        result.add("Base.Worm");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        ArrayList<String> fishes = getFishes();
        ArrayList<String> fishingtools = getFishingTools();
        this.cleanAreaForStory(this, zone);
        this.addVehicle(zone, getSq(zone.x, zone.y, zone.z), null, null, "Base.PickUpTruck", null, null, "Fisherman");
        int chairNbr = Rand.Next(1, 3);

        for (int i = 0; i < chairNbr; i++) {
            this.addTileObject(this.getRandomExtraFreeSquare(this, zone), "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
        }

        InventoryContainer cooler = InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(4, 10);

        for (int i = 0; i < nbOfItem; i++) {
            cooler.getItemContainer().AddItem(fishes.get(Rand.Next(fishes.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        InventoryContainer toolbox = InventoryItemFactory.CreateItem("Base.Tacklebox");
        nbOfItem = Rand.Next(3, 8);

        for (int i = 0; i < nbOfItem; i++) {
            toolbox.getItemContainer().AddItem(fishingtools.get(Rand.Next(fishingtools.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), toolbox);
        nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), "FishingRod");
        }

        this.addZombiesOnSquare(Rand.Next(2, 5), "Fisherman", 0, this.getRandomExtraFreeSquare(this, zone));
    }
}
