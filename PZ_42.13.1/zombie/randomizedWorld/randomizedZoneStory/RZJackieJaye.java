// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZJackieJaye extends RandomizedZoneStoryBase {
    public RZJackieJaye() {
        this.name = "JackieJaye";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.JackieJaye.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Jackie_Jaye", 100, sq);
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie jackie = zeds.get(0);
            jackie.getHumanVisual().setSkinTextureIndex(1);
            SurvivorDesc desc = jackie.getDescriptor();
            if (desc != null) {
                desc.setForename("Jackie");
                desc.setSurname("Jaye");
                InventoryItem pressID = InventoryItemFactory.CreateItem("Base.PressID");
                if (pressID != null) {
                    pressID.nameAfterDescriptor(desc);
                    jackie.addItemToSpawnAtDeath(pressID);
                }
            }
        }
    }
}
