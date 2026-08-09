// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import java.util.ArrayList;
import java.util.List;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.util.list.WeightedList;

public class RBTableStoryBase extends RandomizedBuildingBase {
    public static final WeightedList<RBTableStoryBase> ALL_STORIES = new WeightedList<>();
    protected List<String> rooms = new ArrayList<>();
    protected boolean need2Tables;
    protected boolean ignoreAgainstWall;
    protected IsoObject table2;
    protected IsoObject table1;
    protected boolean westTable;
    private static final ArrayList<IsoObject> tableObjects = new ArrayList<>();

    public static void initStories() {
        if (ALL_STORIES.isEmpty()) {
            ALL_STORIES.add(new RBTSBreakfast(), 10);
            ALL_STORIES.add(new RBTSDinner(), 10);
            ALL_STORIES.add(new RBTSSoup(), 10);
            ALL_STORIES.add(new RBTSSewing(), 5);
            ALL_STORIES.add(new RBTSElectronics(), 5);
            ALL_STORIES.add(new RBTSFoodPreparation(), 8);
            ALL_STORIES.add(new RBTSButcher(), 3);
            ALL_STORIES.add(new RBTSSandwich(), 10);
            ALL_STORIES.add(new RBTSDrink(), 7);
        }
    }

    public static RBTableStoryBase getRandomStory(IsoObject table) {
        if (GameServer.server) {
            initStories();
        }

        return ALL_STORIES.getRandom();
    }

    public void initTables(IsoObject table) {
        this.table1 = table;
        this.table2 = this.getSecondTable(table);
    }

    public boolean isValid(IsoGridSquare sq, boolean force) {
        if (force) {
            return true;
        } else if (this.rooms != null && sq.getRoom() != null && !this.rooms.contains(sq.getRoom().getName())) {
            return false;
        } else {
            return this.need2Tables && this.table2 == null ? false : !this.ignoreAgainstWall || !sq.getWallFull();
        }
    }

    public IsoObject getSecondTable(IsoObject table1) {
        this.westTable = true;
        IsoGridSquare sq = table1.getSquare();
        if (this.ignoreAgainstWall && sq.getWallFull()) {
            return null;
        } else {
            table1.getSpriteGridObjects(tableObjects);
            IsoGridSquare sq2 = sq.getAdjacentSquare(IsoDirections.W);
            IsoObject table2 = this.checkForTable(sq2, table1, tableObjects);
            if (table2 == null) {
                sq2 = sq.getAdjacentSquare(IsoDirections.E);
                table2 = this.checkForTable(sq2, table1, tableObjects);
            }

            if (table2 == null) {
                this.westTable = false;
            }

            if (table2 == null) {
                sq2 = sq.getAdjacentSquare(IsoDirections.N);
                table2 = this.checkForTable(sq2, table1, tableObjects);
            }

            if (table2 == null) {
                sq2 = sq.getAdjacentSquare(IsoDirections.S);
                table2 = this.checkForTable(sq2, table1, tableObjects);
            }

            return table2 != null && this.ignoreAgainstWall && sq2.getWallFull() ? null : table2;
        }
    }

    private IsoObject checkForTable(IsoGridSquare sq, IsoObject table1, ArrayList<IsoObject> tableObjects) {
        if (sq == null) {
            return null;
        } else if (sq.isSomethingTo(table1.getSquare())) {
            return null;
        } else {
            for (int o = 0; o < sq.getObjects().size(); o++) {
                IsoObject obj = sq.getObjects().get(o);
                if ((tableObjects.isEmpty() || tableObjects.contains(obj)) && obj.getProperties().isTable() && obj.getContainer() == null && obj != table1) {
                    return obj;
                }
            }

            return null;
        }
    }
}
