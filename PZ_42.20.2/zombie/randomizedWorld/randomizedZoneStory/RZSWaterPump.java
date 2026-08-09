// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSWaterPump extends RandomizedZoneStoryBase {
    public RZSWaterPump() {
        this.name = "Water Pump";
        this.chance = 5;
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
            this.dirtBomb(midSq);
            this.addTileObject(midSq, "camping_01_6" + (4 + Rand.Next(4)));
            if (Rand.NextBool(2)) {
                this.addItemOnGround(midSq, "Base.MetalCup");
            } else if (Rand.NextBool(2)) {
                this.addItemOnGround(midSq, "Base.Bucket");
            } else if (Rand.NextBool(2)) {
                this.addItemOnGround(midSq, "Base.BucketWood");
            }
        }
    }
}
