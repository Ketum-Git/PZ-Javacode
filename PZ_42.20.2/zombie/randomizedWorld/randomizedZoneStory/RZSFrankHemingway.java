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
public class RZSFrankHemingway extends RandomizedZoneStoryBase {
    public RZSFrankHemingway() {
        this.name = "Frank Hemingway";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.FrankHemingway.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        this.addZombiesOnSquare(Rand.Next(15, 20), null, null, sq);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "FrankHemingway", 0, sq);
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie frank = zeds.get(0);
            frank.getHumanVisual().setSkinTextureIndex(1);
            SurvivorDesc desc = frank.getDescriptor();
            if (desc != null) {
                desc.setForename("Frank");
                desc.setSurname("Hemingway");
                frank.addRandomVisualDamages();
            }
        }
    }
}
