// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

/**
 * Create a dead survivor with alcohol bottles around him
 */
@UsedFromLua
public final class RDSDeadDrunk extends RandomizedDeadSurvivorBase {
    final ArrayList<String> alcoholList = new ArrayList<>();

    public RDSDeadDrunk() {
        this.name = "Dead Drunk";
        this.setChance(10);
        this.alcoholList.add("Base.Whiskey");
        this.alcoholList.add("Base.WhiskeyEmpty");
        this.alcoholList.add("Base.Wine");
        this.alcoholList.add("Base.WineEmpty");
        this.alcoholList.add("Base.Wine2");
        this.alcoholList.add("Base.Wine2Empty");
        this.alcoholList.add("Base.WineBox");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoDeadBody body = createRandomDeadBody(room, 0);
        if (body != null) {
            int bleach = Rand.Next(2, 4);

            for (int b = 0; b < bleach; b++) {
                InventoryItem whiskey = InventoryItemFactory.CreateItem(this.alcoholList.get(Rand.Next(0, this.alcoholList.size())));
                ItemSpawner.spawnItem(whiskey, body.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
                def.alarmed = false;
            }

            body.setPrimaryHandItem(InventoryItemFactory.CreateItem("Base.WhiskeyEmpty"));
        }
    }
}
