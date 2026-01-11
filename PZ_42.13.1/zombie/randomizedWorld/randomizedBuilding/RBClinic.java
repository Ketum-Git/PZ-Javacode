// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

/**
 * Add some food on table
 */
@UsedFromLua
public final class RBClinic extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject obj = sq.getObjects().get(i);
                            if (Rand.NextBool(2)
                                && obj.getSurfaceOffsetNoTable() > 0.0F
                                && obj.getContainer() == null
                                && !sq.hasWater()
                                && !obj.hasFluid()
                                && obj.hasAdjacentCanStandSquare()) {
                                int nbrItem = Rand.Next(1, 3);

                                for (int j = 0; j < nbrItem; j++) {
                                    ItemSpawner.spawnItem(
                                        RBBasic.getMedicallutterItem(), sq, Rand.Next(0.4F, 0.6F), Rand.Next(0.4F, 0.6F), obj.getSurfaceOffsetNoTable() / 96.0F
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null
            && ("hospitalroom".equals(sq.getRoom().getName()) || "clinic".equals(sq.getRoom().getName()) || "medical".equals(sq.getRoom().getName()));
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("medical") != null || def.getRoom("clinic") != null || force;
    }

    public RBClinic() {
        this.name = "Clinic (Vet, Doctor..)";
        this.setAlwaysDo(true);
    }
}
