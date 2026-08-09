// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSWasteDump extends RandomizedZoneStoryBase {
    public RZSWasteDump() {
        this.name = "Waste Dump";
        this.chance = 1;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int nbOfItem = Rand.Next(6, 14);

        for (int i = 0; i < nbOfItem; i++) {
            IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
            if (sq != null) {
                String type = switch (Rand.Next(8)) {
                    case 1 -> "industry_01_22";
                    case 2 -> "industry_01_23";
                    case 3 -> "location_military_generic_01_6";
                    case 4 -> "location_military_generic_01_7";
                    case 5 -> "location_military_generic_01_14";
                    case 6 -> "location_military_generic_01_15";
                    case 7 -> "construction_01_5";
                    default -> "crafted_01_32";
                };
                this.addTileObject(sq, type, true);
            }
        }
    }
}
