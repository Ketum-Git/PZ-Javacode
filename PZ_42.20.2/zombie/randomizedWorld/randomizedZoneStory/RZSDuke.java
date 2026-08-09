// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSDuke extends RandomizedZoneStoryBase {
    public RZSDuke() {
        this.name = "Duke";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Duke.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Duke", 0, sq);
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie duke = zeds.get(0);
            duke.getHumanVisual().setSkinTextureIndex(4);
            SurvivorDesc desc = duke.getDescriptor();
            if (desc != null) {
                desc.setForename("Duke");
                desc.setSurname("Redding");
            }
        }
    }
}
