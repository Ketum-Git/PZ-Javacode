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
 * Add pen, pencils, books... on school desk
 */
@UsedFromLua
public final class RBSchool extends RandomizedBuildingBase {
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
                            if (Rand.NextBool(3) && this.isTableFor3DItems(obj, sq) && obj.hasAdjacentCanStandSquare()) {
                                int penType = Rand.Next(0, 8);
                                switch (penType) {
                                    case 0:
                                        trySpawnStoryItem("Pen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 1:
                                        trySpawnStoryItem("Pencil", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 2:
                                        trySpawnStoryItem("Crayons", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 3:
                                        trySpawnStoryItem("RedPen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 4:
                                        trySpawnStoryItem("BluePen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 5:
                                        trySpawnStoryItem("Eraser", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 6:
                                        trySpawnStoryItem(
                                            "CorrectionFluid", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                }

                                int bookType = Rand.Next(0, 6);
                                switch (bookType) {
                                    case 0:
                                        trySpawnStoryItem("DoodleKids", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 1:
                                        trySpawnStoryItem(
                                            "Book_SchoolTextbook", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                        break;
                                    case 2:
                                        trySpawnStoryItem("Notebook", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                        break;
                                    case 3:
                                        trySpawnStoryItem(
                                            "SheetPaper2", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                }
                            }
                        }

                        if (sq.getRoom() != null && "classroom".equals(sq.getRoom().getName()) && sq.hasAdjacentCanStandSquare()) {
                            if (Rand.NextBool(50)) {
                                int bookType = Rand.Next(0, 10);
                                switch (bookType) {
                                    case 0:
                                        trySpawnStoryItem("DoodleKids", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 1:
                                        trySpawnStoryItem("Book_SchoolTextbook", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 2:
                                        trySpawnStoryItem("Notebook", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 3:
                                        trySpawnStoryItem("SheetPaper2", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 4:
                                        trySpawnStoryItem("Pen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 5:
                                        trySpawnStoryItem("Pencil", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 6:
                                        trySpawnStoryItem("Crayons", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 7:
                                        trySpawnStoryItem("RedPen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 8:
                                        trySpawnStoryItem("BluePen", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                        break;
                                    case 9:
                                        trySpawnStoryItem("Eraser", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                                }
                            }

                            if (Rand.NextBool(120)) {
                                trySpawnStoryItem("Bag_Schoolbag_Kids", sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), 0.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "classroom".equals(sq.getRoom().getName());
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("classroom") != null || force;
    }

    public RBSchool() {
        this.name = "School";
        this.setAlwaysDo(true);
    }
}
