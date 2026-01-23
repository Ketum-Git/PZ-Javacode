// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.objects.ItemKey;

/**
 * Create a dead survivor in the kitchen with empty bleach bottle around him
 */
@UsedFromLua
public final class RDSBleach extends RandomizedDeadSurvivorBase {
    public RDSBleach() {
        this.name = "Suicide by Bleach";
        this.setChance(10);
        this.setMinimumDays(60);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoDeadBody body = createRandomDeadBody(room, 0);
        if (body != null) {
            if (Rand.NextBool(2)) {
                int bleach = Rand.Next(1, 3);

                for (int b = 0; b < bleach; b++) {
                    InventoryItem emptyBleach = InventoryItemFactory.<InventoryItem>CreateItem(ItemKey.Normal.BLEACH).emptyLiquid();
                    ItemSpawner.spawnItem(emptyBleach, body.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
                }

                body.setPrimaryHandItem(InventoryItemFactory.<InventoryItem>CreateItem(ItemKey.Normal.BLEACH).emptyLiquid());
            } else {
                InventoryItem poison = InventoryItemFactory.CreateItem(ItemKey.Drainable.RAT_POISON);
                poison.setCurrentUses(poison.getMaxUses() / 2);
                ItemSpawner.spawnItem(poison, body.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
            }

            if (Rand.Next(2) == 0) {
                InventoryItem note = InventoryItemFactory.CreateItem(ItemKey.Literature.NOTE);
                if (Rand.Next(2) == 0) {
                    ItemSpawner.spawnItem(note, body.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
                } else {
                    body.getContainer().addItem(note);
                }
            }

            def.alarmed = false;
        }
    }
}
