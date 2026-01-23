// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

public class RBTableStoryBase extends RandomizedBuildingBase {
    public static ArrayList<RBTableStoryBase> allStories = new ArrayList<>();
    public static int totalChance;
    protected int chance;
    protected ArrayList<String> rooms = new ArrayList<>();
    protected boolean need2Tables;
    protected boolean ignoreAgainstWall;
    protected IsoObject table2;
    protected IsoObject table1;
    protected boolean westTable;
    private static final HashMap<RBTableStoryBase, Integer> rbtsmap = new HashMap<>();
    private static final ArrayList<IsoObject> tableObjects = new ArrayList<>();
    public ArrayList<HashMap<String, Integer>> fullTableMap = new ArrayList<>();

    public static void initStories(IsoGridSquare sq, IsoObject table) {
        if (allStories.isEmpty()) {
            allStories.add(new RBTSBreakfast());
            allStories.add(new RBTSDinner());
            allStories.add(new RBTSSoup());
            allStories.add(new RBTSSewing());
            allStories.add(new RBTSElectronics());
            allStories.add(new RBTSFoodPreparation());
            allStories.add(new RBTSButcher());
            allStories.add(new RBTSSandwich());
            allStories.add(new RBTSDrink());
        }

        totalChance = 0;
        rbtsmap.clear();

        for (int i = 0; i < allStories.size(); i++) {
            RBTableStoryBase rbts = allStories.get(i);
            if (rbts.isValid(sq, table, false) && rbts.isTimeValid(false)) {
                totalChance = totalChance + rbts.chance;
                rbtsmap.put(rbts, rbts.chance);
            }
        }
    }

    public static ArrayList<RBTableStoryBase> getAllTableStories() {
        return allStories;
    }

    public static RBTableStoryBase getRandomStory(IsoGridSquare sq, IsoObject table) {
        initStories(sq, table);
        int choice = Rand.Next(totalChance);
        Iterator<RBTableStoryBase> it = rbtsmap.keySet().iterator();
        int subTotal = 0;

        while (it.hasNext()) {
            RBTableStoryBase testTable = it.next();
            subTotal += rbtsmap.get(testTable);
            if (choice < subTotal) {
                testTable.table1 = table;
                return testTable;
            }
        }

        return null;
    }

    public boolean isValid(IsoGridSquare sq, IsoObject table, boolean force) {
        if (force) {
            return true;
        } else if (this.rooms != null && sq.getRoom() != null && !this.rooms.contains(sq.getRoom().getName())) {
            return false;
        } else {
            if (this.need2Tables) {
                this.table2 = this.getSecondTable(table);
                if (this.table2 == null) {
                    return false;
                }
            }

            return !this.ignoreAgainstWall || !sq.getWallFull();
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
