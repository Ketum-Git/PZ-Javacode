// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

@UsedFromLua
public final class RBReverend extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        RBBasic.doWoodcraftStuff(sq);
                    }
                }
            }
        }

        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Rev_Peter_Watts", 0, null);
        IsoZombie dean = zeds.get(0);
        dean.getHumanVisual().setSkinTextureIndex(1);
        SurvivorDesc desc = dean.getDescriptor();
        if (desc != null) {
            desc.setForename("Peter");
            desc.setSurname("Watts");
            InventoryItem bible = InventoryItemFactory.CreateItem("Base.Book_Bible");
            if (bible != null) {
                dean.addItemToSpawnAtDeath(bible);
                ItemSpawner.spawnItem("Goblet", dean.getSquare(), Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "church".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("church") != null;
    }

    public RBReverend() {
        this.name = "Reverend";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}
