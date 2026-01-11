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
public final class RBPizzaWhirled extends RandomizedBuildingBase {
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
                            if (Rand.NextBool(2) && this.isTableFor3DItems(obj, sq)) {
                                this.addWorldItem("Pizza", sq, obj);
                                if (Rand.NextBool(3)) {
                                    this.addWorldItem("PlasticFork", sq, obj);
                                }

                                if (Rand.NextBool(3)) {
                                    this.addWorldItem("PlasticKnife", sq, obj);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && ("pizzawhirled".equals(sq.getRoom().getName()) || "italianrestaurant".equals(sq.getRoom().getName()));
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("pizzawhirled") != null || def.getRoom("italianrestaurant") != null || force;
    }

    public RBPizzaWhirled() {
        this.name = "Pizza Whirled Restaurant";
        this.setAlwaysDo(true);
    }
}
