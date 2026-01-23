// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSDean extends RandomizedZoneStoryBase {
    public RZSDean() {
        this.name = "Dean";
        this.chance = 1;
        this.minZoneHeight = 10;
        this.minZoneWidth = 10;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        if (midSq != null) {
            this.cleanSquareAndNeighbors(midSq);
            this.addSimpleCookingPit(midSq);
            this.addItemOnGround(midSq, "Base.TinCanEmpty");
            int roll = Rand.Next(3);
            switch (roll) {
                case 0:
                    this.addItemOnGround(midSq, "Base.PanForged");
                    break;
                case 1:
                    this.addItemOnGround(midSq, "Base.PotForged");
                    break;
                case 2:
                    this.addItemOnGround(midSq, "Base.MetalCup");
            }

            int randX = Rand.Next(0, 1);
            int randY = Rand.Next(0, 1);
            if (Rand.NextBool(2)) {
                this.addShelterWestEast(midX + randX - 2, midY + randY, zone.z);
            } else {
                this.addShelterNorthSouth(midX + randX, midY + randY - 2, zone.z);
            }

            this.addItemOnGround(this.getRandomFreeSquare(this, zone), getOldShelterClutterItem());
            this.addItemOnGround(this.getRandomFreeSquare(this, zone), getOldShelterClutterItem());
            IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
            ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Dean", 0, this.getRandomExtraFreeSquare(this, zone));
            if (zeds != null && !zeds.isEmpty()) {
                IsoZombie dean = zeds.get(0);
                dean.getHumanVisual().setSkinTextureIndex(1);
                SurvivorDesc desc = dean.getDescriptor();
                if (desc != null) {
                    desc.setForename("Dean");
                    desc.setSurname("Porch");
                }
            }
        }
    }
}
