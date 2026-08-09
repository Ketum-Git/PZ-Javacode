// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSNastyMattress extends RandomizedZoneStoryBase {
    public RZSNastyMattress() {
        this.name = "Nasty Mattress";
        this.chance = 3;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        this.cleanAreaForStory(this, zone);
        if (Rand.Next(2) == 0) {
            int randX = Rand.Next(-1, 2);
            int randY = Rand.Next(-1, 2);
            this.addMattressWestEast(midX + randX - 3, midY + randY, zone.z);
        } else {
            int randX = Rand.Next(-1, 2);
            int randY = Rand.Next(-1, 2);
            this.addMattressNorthSouth(midX + randX, midY + randY - 3, zone.z);
        }

        int nbOfItem = Rand.Next(3, 7);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), getNastyMattressClutterItem());
        }
    }
}
