// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import zombie.iso.areas.IsoBuilding;

/**
 * Created by ChrisWood (Tanglewood Games Limited) on 09/10/2017.
 */
public class IsoGridOcclusionData {
    public static final int MAXBUILDINGOCCLUDERS = 3;
    private static final THashSet<IsoBuilding> _leftBuildings = new THashSet<>(3);
    private static final THashSet<IsoBuilding> _rightBuildings = new THashSet<>(3);
    private static final THashSet<IsoBuilding> _allBuildings = new THashSet<>(3);
    private static int objectEpochCount;
    private final ArrayList<IsoBuilding> leftBuildingsArray = new ArrayList<>(3);
    private final ArrayList<IsoBuilding> rightBuildingsArray = new ArrayList<>(3);
    private final ArrayList<IsoBuilding> allBuildingsArray = new ArrayList<>(3);
    private IsoGridSquare ownerSquare;
    private boolean softInitialized;
    private boolean leftOccludedByOrphanStructures;
    private boolean rightOccludedByOrphanStructures;
    private int objectEpoch = -1;

    public IsoGridOcclusionData(IsoGridSquare inOwnerSquare) {
        this.ownerSquare = inOwnerSquare;
    }

    public static void SquareChanged() {
        objectEpochCount++;
        if (objectEpochCount < 0) {
            objectEpochCount = 0;
        }
    }

    public void Reset() {
        this.softInitialized = false;
        this.leftOccludedByOrphanStructures = false;
        this.rightOccludedByOrphanStructures = false;
        this.allBuildingsArray.clear();
        this.leftBuildingsArray.clear();
        this.rightBuildingsArray.clear();
        this.objectEpoch = -1;
    }

    /**
     * Returns whether built structures with no building id (orphans) could occlude some of the square.
     *  Depending on the exact shape of the structures, the square might not be hidden at all.
     *  This is used to hide player-built structures that might block our view of something in a square (at ground
     *  level)
     */
    public boolean getCouldBeOccludedByOrphanStructures(IsoGridOcclusionData.OcclusionFilter filter) {
        if (this.objectEpoch != objectEpochCount) {
            if (this.softInitialized) {
                this.Reset();
            }

            this.objectEpoch = objectEpochCount;
        }

        if (!this.softInitialized) {
            this.LazyInitializeSoftOccluders();
        }

        if (filter == IsoGridOcclusionData.OcclusionFilter.Left) {
            return this.leftOccludedByOrphanStructures;
        } else {
            return filter == IsoGridOcclusionData.OcclusionFilter.Right
                ? this.rightOccludedByOrphanStructures
                : this.leftOccludedByOrphanStructures || this.rightOccludedByOrphanStructures;
        }
    }

    public ArrayList<IsoBuilding> getBuildingsCouldBeOccluders(IsoGridOcclusionData.OcclusionFilter filter) {
        if (this.objectEpoch != objectEpochCount) {
            if (this.softInitialized) {
                this.Reset();
            }

            this.objectEpoch = objectEpochCount;
        }

        if (!this.softInitialized) {
            this.LazyInitializeSoftOccluders();
        }

        if (filter == IsoGridOcclusionData.OcclusionFilter.Left) {
            return this.leftBuildingsArray;
        } else {
            return filter == IsoGridOcclusionData.OcclusionFilter.Right ? this.rightBuildingsArray : this.allBuildingsArray;
        }
    }

    private void LazyInitializeSoftOccluders() {
        boolean bOccludedByOrphanStructures = false;
        int x = this.ownerSquare.getX();
        int y = this.ownerSquare.getY();
        int z = this.ownerSquare.getZ();
        _allBuildings.clear();
        _leftBuildings.clear();
        _rightBuildings.clear();
        bOccludedByOrphanStructures |= this.GetBuildingFloorsProjectedOnSquare(_allBuildings, x, y, z);
        bOccludedByOrphanStructures |= this.GetBuildingFloorsProjectedOnSquare(_allBuildings, x + 1, y + 1, z);
        bOccludedByOrphanStructures |= this.GetBuildingFloorsProjectedOnSquare(_allBuildings, x + 2, y + 2, z);
        bOccludedByOrphanStructures |= this.GetBuildingFloorsProjectedOnSquare(_allBuildings, x + 3, y + 3, z);
        this.leftOccludedByOrphanStructures = this.leftOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_leftBuildings, x, y + 1, z);
        this.leftOccludedByOrphanStructures = this.leftOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_leftBuildings, x + 1, y + 2, z);
        this.leftOccludedByOrphanStructures = this.leftOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_leftBuildings, x + 2, y + 3, z);
        this.rightOccludedByOrphanStructures = this.rightOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_rightBuildings, x + 1, y, z);
        this.rightOccludedByOrphanStructures = this.rightOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_rightBuildings, x + 2, y + 1, z);
        this.rightOccludedByOrphanStructures = this.rightOccludedByOrphanStructures | this.GetBuildingFloorsProjectedOnSquare(_rightBuildings, x + 3, y + 2, z);
        this.leftOccludedByOrphanStructures |= bOccludedByOrphanStructures;
        _leftBuildings.addAll(_allBuildings);
        this.rightOccludedByOrphanStructures |= bOccludedByOrphanStructures;
        _rightBuildings.addAll(_allBuildings);
        _allBuildings.clear();
        _allBuildings.addAll(_leftBuildings);
        _allBuildings.addAll(_rightBuildings);
        this.leftBuildingsArray.addAll(_leftBuildings);
        this.rightBuildingsArray.addAll(_rightBuildings);
        this.allBuildingsArray.addAll(_allBuildings);
        this.softInitialized = true;
    }

    private boolean GetBuildingFloorsProjectedOnSquare(THashSet<IsoBuilding> outBuildings, int inX, int inY, int inZ) {
        boolean bOccludedByOrphanStructures = false;
        int x = inX;
        int y = inY;

        for (int z = inZ; z < IsoCell.maxHeight; y += 3) {
            IsoGridSquare testSquare = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (testSquare != null) {
                IsoBuilding building = testSquare.getBuilding();
                if (building == null) {
                    building = testSquare.roofHideBuilding;
                    if (testSquare.getZ() > 0 && building != null && building.isEntirelyEmptyOutside()) {
                        building = null;
                        bOccludedByOrphanStructures = true;
                    }
                }

                if (building != null) {
                    outBuildings.add(building);
                }

                for (int dropZ = z - 1; dropZ >= 0 && building == null; dropZ--) {
                    IsoGridSquare testDropSquare = IsoWorld.instance.currentCell.getGridSquare(x, y, dropZ);
                    if (testDropSquare != null) {
                        building = testDropSquare.getBuilding();
                        if (building == null) {
                            building = testDropSquare.roofHideBuilding;
                        }

                        if (building != null) {
                            outBuildings.add(building);
                        }
                    }
                }

                if (building == null && !bOccludedByOrphanStructures && testSquare.getZ() != 0 && testSquare.getPlayerBuiltFloor() != null) {
                    bOccludedByOrphanStructures = true;
                }
            }

            z++;
            x += 3;
        }

        return bOccludedByOrphanStructures;
    }

    public static enum OccluderType {
        Unknown,
        NotFull,
        Full;
    }

    public static enum OcclusionFilter {
        Left,
        Right,
        All;
    }
}
