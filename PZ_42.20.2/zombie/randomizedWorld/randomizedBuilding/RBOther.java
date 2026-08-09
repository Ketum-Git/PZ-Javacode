// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;

/**
 * Building full of toilet paper
 */
@UsedFromLua
public final class RBOther extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        def.setAllExplored(true);
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject obj = sq.getObjects().get(i);
                            if (obj.getContainer() != null) {
                                obj.getContainer().emptyIt();
                                obj.getContainer().AddItems("Base.ToiletPaper", Rand.Next(10, 30));
                                ItemPickerJava.updateOverlaySprite(obj);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!super.isValid(def, force)) {
            return false;
        } else if (def.getRooms().size() > 10) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else {
            return true;
        }
    }

    public RBOther() {
        this.name = "Other";
        this.setChance(1);
        this.setUnique(true);
    }
}
