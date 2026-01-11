// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;

public final class RBTSFoodPreparation extends RBTableStoryBase {
    public RBTSFoodPreparation() {
        this.chance = 8;
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.addWorldItem("Base.BakingTray", this.table1.getSquare(), 0.695F, 0.648F, this.table1.getSurfaceOffsetNoTable() / 96.0F, 1);
        String food = "Base.Chicken";
        int rand = Rand.Next(0, 4);
        switch (rand) {
            case 0:
                food = "Base.Steak";
                break;
            case 1:
                food = "Base.MuttonChop";
                break;
            case 2:
                food = "Base.Smallbirdmeat";
        }

        this.addWorldItem(food, this.table1.getSquare(), 0.531F, 0.625F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
        this.addWorldItem(food, this.table1.getSquare(), 0.836F, 0.627F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
        this.addWorldItem(Rand.NextBool(2) ? "Base.Pepper" : "Base.Salt", this.table1.getSquare(), 0.492F, 0.94F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
        this.addWorldItem("Base.KitchenKnife", this.table1.getSquare(), 0.492F, 0.29F, this.table1.getSurfaceOffsetNoTable() / 96.0F, 1);
        food = "Base.Tomato";
        rand = Rand.Next(0, 4);
        switch (rand) {
            case 0:
                food = "Base.BellPepper";
                break;
            case 1:
                food = "Base.Broccoli";
                break;
            case 2:
                food = "Base.Carrots";
        }

        this.addWorldItem(food, this.table1.getSquare(), 0.77F, 0.97F, this.table1.getSurfaceOffsetNoTable() / 96.0F, 70);
    }
}
