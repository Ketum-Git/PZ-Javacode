// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSTrapperCamp extends RandomizedZoneStoryBase {
    public RZSTrapperCamp() {
        this.name = "Trappers Forest Camp";
        this.chance = 7;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    public static ArrayList<String> getTrapList() {
        ArrayList<String> result = new ArrayList<>();
        result.add("constructedobjects_01_3");
        result.add("constructedobjects_01_4");
        result.add("constructedobjects_01_7");
        result.add("constructedobjects_01_8");
        result.add("constructedobjects_01_11");
        result.add("constructedobjects_01_13");
        result.add("constructedobjects_01_16");
        return result;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> trapList = getTrapList();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addSimpleFire(midSq);
        int randX = Rand.Next(-1, 2);
        int randY = Rand.Next(-1, 2);
        this.addRandomTentWestEast(midX + randX - 2, midY + randY, zone.z);
        if (Rand.Next(100) < 70) {
            this.addRandomTentNorthSouth(midX + randX, midY + randY - 2, zone.z);
        }

        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
            this.addTileObject(sq, trapList.get(Rand.Next(trapList.size())));
        }

        this.addZombiesOnSquare(Rand.Next(2, 5), "Hunter", 0, this.getRandomExtraFreeSquare(this, zone));
    }
}
