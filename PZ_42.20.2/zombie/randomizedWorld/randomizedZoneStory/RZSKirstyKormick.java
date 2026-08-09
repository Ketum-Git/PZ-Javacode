// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSKirstyKormick extends RandomizedZoneStoryBase {
    public RZSKirstyKormick() {
        this.name = "Kirsty Cormick";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.KirstyKormick.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare sq = this.getRandomExtraFreeSquare(this, zone);
        this.addZombiesOnSquare(Rand.Next(15, 20), null, null, sq);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "KirstyKormick", 100, sq);
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie kirsty = zeds.get(0);
            kirsty.getHumanVisual().setSkinTextureIndex(4);
            SurvivorDesc desc = kirsty.getDescriptor();
            if (desc != null) {
                desc.setForename("Kirsty");
                desc.setSurname("Cormick");
                kirsty.addRandomVisualDamages();
                InventoryItem pressID = InventoryItemFactory.CreateItem("Base.PressID");
                if (pressID != null) {
                    pressID.nameAfterDescriptor(desc);
                    kirsty.addItemToSpawnAtDeath(pressID);
                }
            }
        }
    }
}
