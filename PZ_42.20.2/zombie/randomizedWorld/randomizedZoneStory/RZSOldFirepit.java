// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSOldFirepit extends RandomizedZoneStoryBase {
    public RZSOldFirepit() {
        this.name = "Old Firepit";
        this.chance = 10;
        this.minZoneHeight = 5;
        this.minZoneWidth = 5;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        if (midSq != null) {
            this.cleanSquareAndNeighbors(midSq);
            this.addRandomFirepit(midSq);
            IsoGridSquare sq = midSq.getRandomAdjacent();
            if (sq != null && Rand.NextBool(2)) {
                this.addItemOnGround(sq, "Base.TinCanEmpty");
            }

            if (!Rand.NextBool(2)) {
                sq = midSq.getRandomAdjacent();
                if (sq != null) {
                    int roll = Rand.Next(2);
                    switch (roll) {
                        case 0:
                            this.addItemOnGround(sq, "Base.PanForged");
                            break;
                        case 1:
                            this.addItemOnGround(sq, "Base.PotForged");
                    }
                }
            }
        }
    }
}
