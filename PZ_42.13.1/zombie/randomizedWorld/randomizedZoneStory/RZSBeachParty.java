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
public class RZSBeachParty extends RandomizedZoneStoryBase {
    public RZSBeachParty() {
        this.name = "Beach Party";
        this.chance = 10;
        this.minZoneHeight = 13;
        this.minZoneWidth = 13;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> coolerClutter = RZSForestCamp.getCoolerClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        if (Rand.NextBool(2)) {
            this.cleanSquareAndNeighbors(midSq);
            int roll = Rand.Next(2);
            switch (roll) {
                case 0:
                    this.addTileObject(midSq, "camping_01_6");
                    break;
                case 1:
                    this.addCookingPit(midSq);
            }
        }

        int chairNbr = Rand.Next(1, 4);

        for (int i = 0; i < chairNbr; i++) {
            int chairType = Rand.Next(4) + 1;
            switch (chairType) {
                case 1:
                    chairType = 25;
                    break;
                case 2:
                    chairType = 26;
                    break;
                case 3:
                    chairType = 28;
                    break;
                case 4:
                    chairType = 31;
            }

            IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                this.addTileObject(sq, "furniture_seating_outdoor_01_" + chairType);
                if (chairType == 25) {
                    sq = getSq(sq.x, sq.y + 1, sq.z);
                    this.addTileObject(sq, "furniture_seating_outdoor_01_24");
                } else if (chairType == 26) {
                    sq = getSq(sq.x + 1, sq.y, sq.z);
                    this.addTileObject(sq, "furniture_seating_outdoor_01_27");
                } else if (chairType == 28) {
                    sq = getSq(sq.x, sq.y - 1, sq.z);
                    this.addTileObject(sq, "furniture_seating_outdoor_01_29");
                } else {
                    sq = getSq(sq.x - 1, sq.y, sq.z);
                    this.addTileObject(sq, "furniture_seating_outdoor_01_30");
                }
            }
        }

        chairNbr = Rand.Next(1, 3);

        for (int i = 0; i < chairNbr; i++) {
            IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                this.addTileObject(sq, "furniture_seating_outdoor_01_" + Rand.Next(16, 20));
            }
        }

        InventoryContainer cooler = InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(4, 8);

        for (int ix = 0; ix < nbOfItem; ix++) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }

        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        if (sq != null) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        }

        nbOfItem = Rand.Next(3, 7);

        for (int ix = 0; ix < nbOfItem; ix++) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), this.getBeachPartyClutterItem());
            }
        }

        int randZed = Rand.Next(3, 8);

        for (int ixx = 0; ixx < randZed; ixx++) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                this.addZombiesOnSquare(1, "Swimmer", null, this.getRandomExtraFreeSquare(this, zone));
            }
        }

        randZed = Rand.Next(1, 3);

        for (int ixxx = 0; ixxx < randZed; ixxx++) {
            sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                this.addZombiesOnSquare(1, "Tourist", null, this.getRandomExtraFreeSquare(this, zone));
            }
        }
    }
}
