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
public class RZSHermitCamp extends RandomizedZoneStoryBase {
    public RZSHermitCamp() {
        this.name = "Hermit Campsite";
        this.chance = 2;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    public static ArrayList<String> getForestClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.TinCanEmpty");
        result.add("Base.TreeBranch2");
        result.add("Base.Log");
        result.add("Base.Twigs");
        result.add("Base.DoubleBarrelShotgun");
        result.add("Base.WhiskeyEmpty");
        result.add("Base.Hat_Raccoon");
        result.add("Base.DeadRabbit");
        result.add("Base.DeadSquirrel");
        result.add("Base.KnifePocket");
        result.add("Base.BucketWaterFull");
        result.add("Base.CraftedFishingRodTwineLine");
        result.add("Base.Lantern_Hurricane");
        result.add("Base.MetalCup");
        result.add("Base.Broom_Twig");
        if (Rand.Next(2) == 0) {
            result.add("Base.Violin");
        } else {
            result.add("Base.Banjo");
        }

        result.add("Base.FirewoodBundle");
        return result;
    }

    public static ArrayList<String> getBagClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.Matches");
        result.add("Base.Matchbox");
        result.add("Base.TinnedBeans");
        result.add("Base.Dogfood");
        result.add("Base.Acorn");
        result.add("Base.Dandelions");
        result.add("Base.Nettles");
        result.add("Base.Thistle");
        result.add("Base.Rosehips");
        result.add("Base.TinOpener");
        result.add("Base.Twigs");
        result.add("Base.Whiskey");
        result.add("Base.DeadRabbit");
        result.add("Base.DeadSquirrel");
        result.add("Base.KnifePocket");
        result.add("Base.CigaretteRolled");
        result.add("Base.CigaretteRollingPapers");
        result.add("Base.TobaccoLoose");
        result.add("Base.HerbalistMag");
        result.add("Base.Twine");
        return result;
    }

    public static ArrayList<String> getFireClutter() {
        ArrayList<String> result = new ArrayList<>();
        result.add("Base.WaterPot");
        result.add("Base.PotForged");
        result.add("Base.PotOfStew");
        result.add("Base.OpenBeans");
        result.add("Base.DogfoodOpen");
        result.add("Base.PanForged");
        result.add("Base.PotForged");
        result.add("Base.PanForged");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        ArrayList<String> clutter = getForestClutter();
        ArrayList<String> bagClutter = getBagClutter();
        ArrayList<String> fireClutter = getFireClutter();
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addSimpleFire(midSq);
        this.addItemOnGround(midSq, fireClutter.get(Rand.Next(fireClutter.size())));
        if (Rand.Next(10) == 0) {
            this.addItemOnGround(midSq, "Base.FireplacePoker");
        }

        int randX = Rand.Next(0, 1);
        int randY = Rand.Next(0, 1);
        if (Rand.NextBool(2)) {
            this.addRandomShelterWestEast(midX + randX - 2, midY + randY, zone.z);
        } else {
            this.addRandomShelterNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }

        this.addZombiesOnSquare(1, "Hobbo", null, this.getRandomFreeSquare(this, zone));
        InventoryContainer bag = InventoryItemFactory.CreateItem("Base.EmptySandbag");
        nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            bag.getItemContainer().AddItem(bagClutter.get(Rand.Next(bagClutter.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), bag);
    }
}
