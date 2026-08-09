// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSOldShelter extends RandomizedZoneStoryBase {
    public RZSOldShelter() {
        this.name = "OldShelter";
        this.chance = 5;
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
        boolean fire = Rand.NextBool(2);
        if (fire) {
            this.cleanSquareAndNeighbors(midSq);
            this.addSimpleFire(midSq);
        }

        int randX = Rand.Next(0, 1);
        int randY = Rand.Next(0, 1);
        if (Rand.NextBool(2)) {
            this.addRandomShelterWestEast(midX + randX - 2, midY + randY, zone.z);
        } else {
            this.addRandomShelterNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        if (!Rand.NextBool(4)) {
            if (Rand.NextBool(2)) {
                this.addItemOnGround(midSq, "Base.TinCanEmpty");
            }

            if (fire && Rand.NextBool(2)) {
                int roll = Rand.Next(2);
                switch (roll) {
                    case 0:
                        this.addItemOnGround(midSq, "Base.PanForged");
                        break;
                    case 1:
                        this.addItemOnGround(midSq, "Base.PotForged");
                }
            }

            if (!Rand.NextBool(3)) {
                this.addItemOnGround(this.getRandomFreeSquare(this, zone), getOldShelterClutterItem());
            }
        }
    }
}
