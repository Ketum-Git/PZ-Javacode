// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;

public final class RBTSButcher extends RBTableStoryBase {
    public RBTSButcher() {
        this.chance = 3;
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        String animal = "Base.DeadRabbit";
        String food = "Base.Rabbitmeat";
        int rand = Rand.Next(4);
        switch (rand) {
            case 0:
                animal = "Base.DeadBird";
                food = "Base.Smallbirdmeat";
                break;
            case 1:
                animal = "Base.DeadSquirrel";
                food = "Base.Smallanimalmeat";
                break;
            case 2:
                animal = "Base.BaitFish";
                food = "Base.FishFillet";
        }

        this.addWorldItem(animal, this.table1.getSquare(), 0.453F, 0.64F, this.table1.getSurfaceOffsetNoTable() / 96.0F, 1);
        this.addWorldItem(food, this.table1.getSquare(), 0.835F, 0.851F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
        this.addWorldItem("Base.KitchenKnife", this.table1.getSquare(), 0.742F, 0.445F, this.table1.getSurfaceOffsetNoTable() / 96.0F);
    }
}
