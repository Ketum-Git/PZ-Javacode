// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSBBQParty extends RandomizedZoneStoryBase {
    public RZSBBQParty() {
        this.name = "BBQ Party";
        this.chance = 10;
        this.minZoneHeight = 12;
        this.minZoneWidth = 12;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> coolerClutter = RZSForestCamp.getCoolerClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        String sprite = "appliances_cooking_01_35";
        if (Rand.NextBool(2)) {
            IsoBarbecue bbq = new IsoBarbecue(IsoWorld.instance.getCell(), midSq, IsoSpriteManager.instance.namedMap.get("appliances_cooking_01_35"));
            midSq.getObjects().add(bbq);
        } else {
            this.cleanSquareAndNeighbors(midSq);
            this.addCookingPit(midSq);
        }

        int chairNbr = Rand.Next(1, 4);

        for (int i = 0; i < chairNbr; i++) {
            this.addTileObject(this.getRandomExtraFreeSquare(this, zone), "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
        }

        InventoryContainer cooler = InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(4, 8);

        for (int i = 0; i < nbOfItem; i++) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        nbOfItem = Rand.Next(3, 7);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getBBQClutterItem());
        }

        int randZed = Rand.Next(2, 7);

        for (int i = 0; i < randZed; i++) {
            this.addZombiesOnSquare(1, "Tourist", null, this.getRandomExtraFreeSquare(this, zone));
        }

        this.addZombiesOnSquare(1, "Meat_Master", null, this.getRandomExtraFreeSquare(this, zone));
    }
}
