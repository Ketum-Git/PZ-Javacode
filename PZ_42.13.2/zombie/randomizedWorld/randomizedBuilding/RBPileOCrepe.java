// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

/**
 * Add some food on table
 */
@UsedFromLua
public final class RBPileOCrepe extends RandomizedBuildingBase {
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
                            if (Rand.NextBool(3) && this.isTableFor3DItems(obj, sq)) {
                                if (Rand.Next(0.0F, 100.0F) <= 58.54F) {
                                    this.addWorldItem("PancakesRecipe", sq, obj);
                                } else {
                                    this.addWorldItem("WafflesRecipe", sq, obj);
                                }

                                if (Rand.NextBool(3)) {
                                    this.addWorldItem("Fork", sq, obj);
                                }

                                if (Rand.NextBool(3)) {
                                    this.addWorldItem("ButterKnife", sq, obj);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && ("pileocrepe".equals(sq.getRoom().getName()) || "kitchen_crepe".equals(sq.getRoom().getName()));
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("pileocrepe") != null || def.getRoom("kitchen_crepe") != null || force;
    }

    public RBPileOCrepe() {
        this.name = "PileOCrepe Restaurant";
        this.setAlwaysDo(true);
    }
}
