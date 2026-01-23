// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.properties.PropertyContainer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;

public final class IsoRoofFixer {
    private static final boolean PER_ROOM_MODE = true;
    private static final int MAX_Z = 8;
    private static final int SCAN_RANGE = 3;
    private static final boolean ALWAYS_INVIS_FLOORS = false;
    private static boolean roofTileGlassCacheDirty = true;
    private static boolean roofTileIsGlass;
    private static IsoSprite roofTileCache;
    private static int roofTilePlaceFloorIndexCache;
    private static final String invisFloor = "invisible_01_0";
    private static final Map<Integer, String> roofGroups = new HashMap<>();
    private static IsoRoofFixer.PlaceFloorInfo[] placeFloorInfos = new IsoRoofFixer.PlaceFloorInfo[10000];
    private static int floorInfoIndex;
    private static final IsoGridSquare[] sqCache;
    private static IsoRoom workingRoom;
    private static final int[] interiorAirSpaces;
    private static final int I_UNCHECKED = 0;
    private static final int I_TRUE = 1;
    private static final int I_FALSE = 2;

    private static void ensureCapacityFloorInfos() {
        if (floorInfoIndex == placeFloorInfos.length) {
            IsoRoofFixer.PlaceFloorInfo[] old = placeFloorInfos;
            placeFloorInfos = new IsoRoofFixer.PlaceFloorInfo[placeFloorInfos.length + 400];
            System.arraycopy(old, 0, placeFloorInfos, 0, old.length);
        }
    }

    private static void setRoofTileCache(IsoObject object) {
        IsoSprite sprite = object != null ? object.sprite : null;
        if (roofTileCache != sprite) {
            roofTileCache = sprite;
            roofTilePlaceFloorIndexCache = 0;
            if (sprite != null && sprite.getProperties() != null && sprite.getProperties().get("RoofGroup") != null) {
                try {
                    int group = Integer.parseInt(sprite.getProperties().get("RoofGroup"));
                    if (roofGroups.containsKey(group)) {
                        roofTilePlaceFloorIndexCache = group;
                    }
                } catch (Exception var3) {
                }
            }

            roofTileGlassCacheDirty = true;
        }
    }

    private static boolean isRoofTileCacheGlass() {
        if (roofTileGlassCacheDirty) {
            roofTileIsGlass = false;
            if (roofTileCache != null) {
                PropertyContainer props = roofTileCache.getProperties();
                if (props != null) {
                    String mat = props.get("Material");
                    roofTileIsGlass = mat != null && mat.equalsIgnoreCase("glass");
                }
            }

            roofTileGlassCacheDirty = false;
        }

        return roofTileIsGlass;
    }

    public static void FixRoofsAt(IsoGridSquare current) {
        try {
            FixRoofsPerRoomAt(current);
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    private static void FixRoofsPerRoomAt(IsoGridSquare current) {
        floorInfoIndex = 0;
        if (current.getZ() > 0 && !current.TreatAsSolidFloor() && current.getRoom() == null) {
            IsoRoom roomBelow = getRoomBelow(current);
            if (roomBelow != null && !roomBelow.def.isRoofFixed()) {
                resetInteriorSpaceCache();
                workingRoom = roomBelow;
                ArrayList<IsoGridSquare> squares = roomBelow.getSquares();

                for (int i = 0; i < squares.size(); i++) {
                    IsoGridSquare square = squares.get(i);
                    IsoGridSquare test = getRoofFloorForColumn(square);
                    if (test != null) {
                        ensureCapacityFloorInfos();
                        placeFloorInfos[floorInfoIndex++].set(test, roofTilePlaceFloorIndexCache);
                    }
                }

                roomBelow.def.setRoofFixed(true);
            }
        }

        for (int ix = 0; ix < floorInfoIndex; ix++) {
            placeFloorInfos[ix].square.addFloor(roofGroups.get(placeFloorInfos[ix].floorType));
        }
    }

    private static void clearSqCache() {
        for (int i = 0; i < sqCache.length; i++) {
            sqCache[i] = null;
        }
    }

    private static IsoGridSquare getRoofFloorForColumn(IsoGridSquare square) {
        if (square == null) {
            return null;
        } else {
            IsoCell cell = IsoCell.getInstance();
            int count = 0;
            boolean lastWasNull = false;
            int z = 7;

            while (true) {
                label163: {
                    if (z >= square.getZ() + 1) {
                        IsoGridSquare test = cell.getGridSquare(square.x, square.y, z);
                        if (test == null) {
                            if (z == square.getZ() + 1 && z > 0 && !isStairsBelow(square.x, square.y, z)) {
                                test = IsoGridSquare.getNew(cell, null, square.x, square.y, z);
                                cell.ConnectNewSquare(test, false);
                                test.EnsureSurroundNotNull();
                                test.RecalcAllWithNeighbours(true);
                                sqCache[count++] = test;
                            }

                            lastWasNull = true;
                            break label163;
                        }

                        if (test.TreatAsSolidFloor()) {
                            if (test.getRoom() == null) {
                                IsoObject floor = test.getFloor();
                                if (floor == null || !isObjectRoof(floor) || floor.getProperties() == null) {
                                    break;
                                }

                                PropertyContainer props = floor.getProperties();
                                if (props.has(IsoFlagType.FloorHeightOneThird) || props.has(IsoFlagType.FloorHeightTwoThirds)) {
                                    break;
                                }

                                IsoGridSquare below = cell.getGridSquare(square.x, square.y, z - 1);
                                if (below == null || below.getRoom() != null) {
                                    break;
                                }

                                lastWasNull = false;
                                break label163;
                            }

                            if (lastWasNull) {
                                test = IsoGridSquare.getNew(cell, null, square.x, square.y, z + 1);
                                cell.ConnectNewSquare(test, false);
                                test.EnsureSurroundNotNull();
                                test.RecalcAllWithNeighbours(true);
                                sqCache[count++] = test;
                            }
                        } else if (!test.HasStairsBelow()) {
                            lastWasNull = false;
                            sqCache[count++] = test;
                            break label163;
                        }
                    }

                    if (count == 0) {
                        return null;
                    }

                    boolean checkAbove = true;

                    for (int index = 0; index < count; index++) {
                        IsoGridSquare testx = sqCache[index];
                        if (testx.getRoom() == null && isInteriorAirSpace(testx.getX(), testx.getY(), testx.getZ())) {
                            return null;
                        }

                        if (isRoofAt(testx, true)) {
                            return testx;
                        }

                        for (int x = testx.x - 3; x <= testx.x + 3; x++) {
                            for (int y = testx.y - 3; y <= testx.y + 3; y++) {
                                if (x != testx.x || y != testx.y) {
                                    IsoGridSquare gs = cell.getGridSquare(x, y, testx.z);
                                    if (gs != null) {
                                        for (int i = 0; i < gs.getObjects().size(); i++) {
                                            IsoObject obj = gs.getObjects().get(i);
                                            if (isObjectRoofNonFlat(obj)) {
                                                setRoofTileCache(obj);
                                                return testx;
                                            }
                                        }

                                        IsoGridSquare above = cell.getGridSquare(gs.x, gs.y, gs.z + 1);
                                        if (above != null && !above.getObjects().isEmpty()) {
                                            for (int ix = 0; ix < above.getObjects().size(); ix++) {
                                                IsoObject obj = above.getObjects().get(ix);
                                                if (isObjectRoofFlatFloor(obj)) {
                                                    setRoofTileCache(obj);
                                                    return testx;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return null;
                }

                z--;
            }

            return null;
        }
    }

    private static void FixRoofsPerTileAt(IsoGridSquare current) {
        if (current.getZ() > 0
            && !current.TreatAsSolidFloor()
            && current.getRoom() == null
            && hasRoomBelow(current)
            && (isRoofAt(current, true) || scanIsRoofAt(current, true))) {
            if (isRoofTileCacheGlass()) {
                current.addFloor("invisible_01_0");
            } else {
                current.addFloor("carpentry_02_58");
            }
        }
    }

    private static boolean scanIsRoofAt(IsoGridSquare square, boolean checkAbove) {
        if (square == null) {
            return false;
        } else {
            for (int x = square.x - 3; x <= square.x + 3; x++) {
                for (int y = square.y - 3; y <= square.y + 3; y++) {
                    if (x != square.x || y != square.y) {
                        IsoGridSquare gs = square.getCell().getGridSquare(x, y, square.z);
                        if (gs != null && isRoofAt(gs, checkAbove)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private static boolean isRoofAt(IsoGridSquare square, boolean checkAbove) {
        if (square == null) {
            return false;
        } else {
            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject obj = square.getObjects().get(i);
                if (isObjectRoofNonFlat(obj)) {
                    setRoofTileCache(obj);
                    return true;
                }
            }

            if (checkAbove) {
                IsoGridSquare above = square.getCell().getGridSquare(square.x, square.y, square.z + 1);
                if (above != null && !above.getObjects().isEmpty()) {
                    for (int ix = 0; ix < above.getObjects().size(); ix++) {
                        IsoObject obj = above.getObjects().get(ix);
                        if (isObjectRoofFlatFloor(obj)) {
                            setRoofTileCache(obj);
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private static boolean isObjectRoof(IsoObject object) {
        return object != null
            && (object.getType() == IsoObjectType.WestRoofT || object.getType() == IsoObjectType.WestRoofB || object.getType() == IsoObjectType.WestRoofM);
    }

    private static boolean isObjectRoofNonFlat(IsoObject object) {
        if (isObjectRoof(object)) {
            PropertyContainer props = object.getProperties();
            if (props != null) {
                return !props.has(IsoFlagType.solidfloor) || props.has(IsoFlagType.FloorHeightOneThird) || props.has(IsoFlagType.FloorHeightTwoThirds);
            }
        }

        return false;
    }

    private static boolean isObjectRoofFlatFloor(IsoObject object) {
        if (isObjectRoof(object)) {
            PropertyContainer props = object.getProperties();
            if (props != null && props.has(IsoFlagType.solidfloor)) {
                return !props.has(IsoFlagType.FloorHeightOneThird) && !props.has(IsoFlagType.FloorHeightTwoThirds);
            }
        }

        return false;
    }

    private static boolean hasRoomBelow(IsoGridSquare current) {
        return getRoomBelow(current) != null;
    }

    private static IsoRoom getRoomBelow(IsoGridSquare current) {
        if (current == null) {
            return null;
        } else {
            for (int z = current.z - 1; z >= 0; z--) {
                IsoGridSquare testSq = current.getCell().getGridSquare(current.x, current.y, z);
                if (testSq != null) {
                    if (testSq.TreatAsSolidFloor() && testSq.getRoom() == null) {
                        return null;
                    }

                    if (testSq.getRoom() != null) {
                        return testSq.getRoom();
                    }
                }
            }

            return null;
        }
    }

    private static boolean isStairsBelow(int x, int y, int z) {
        if (z == 0) {
            return false;
        } else {
            IsoCell cell = IsoCell.getInstance();
            IsoGridSquare square = cell.getGridSquare(x, y, z - 1);
            return square != null && square.HasStairs();
        }
    }

    private static void resetInteriorSpaceCache() {
        for (int i = 0; i < interiorAirSpaces.length; i++) {
            interiorAirSpaces[i] = 0;
        }
    }

    private static boolean isInteriorAirSpace(int sx, int sy, int sz) {
        if (interiorAirSpaces[sz] != 0) {
            return interiorAirSpaces[sz] == 1;
        } else {
            ArrayList<IsoGridSquare> squares = workingRoom.getSquares();
            boolean hasRailing = false;
            if (!squares.isEmpty() && sz > squares.get(0).getZ()) {
                for (int i = 0; i < workingRoom.rects.size(); i++) {
                    RoomDef.RoomRect rect = workingRoom.rects.get(i);

                    for (int x = rect.getX(); x < rect.getX2(); x++) {
                        if (hasRailing(x, rect.getY(), sz, IsoDirections.N) || hasRailing(x, rect.getY2() - 1, sz, IsoDirections.S)) {
                            hasRailing = true;
                            break;
                        }
                    }

                    if (hasRailing) {
                        break;
                    }

                    for (int y = rect.getY(); y < rect.getY2(); y++) {
                        if (hasRailing(rect.getX(), y, sz, IsoDirections.W) || hasRailing(rect.getX2() - 1, y, sz, IsoDirections.E)) {
                            hasRailing = true;
                            break;
                        }
                    }
                }
            }

            interiorAirSpaces[sz] = hasRailing ? 1 : 2;
            return hasRailing;
        }
    }

    private static boolean hasRailing(int x, int y, int z, IsoDirections dir) {
        IsoCell cell = IsoCell.getInstance();
        IsoGridSquare curr = cell.getGridSquare(x, y, z);
        if (curr == null) {
            return false;
        } else {
            switch (dir) {
                case N:
                    return curr.isHoppableTo(cell.getGridSquare(x, y - 1, z));
                case E:
                    return curr.isHoppableTo(cell.getGridSquare(x + 1, y, z));
                case S:
                    return curr.isHoppableTo(cell.getGridSquare(x, y + 1, z));
                case W:
                    return curr.isHoppableTo(cell.getGridSquare(x - 1, y, z));
                default:
                    return false;
            }
        }
    }

    static {
        roofGroups.put(0, "carpentry_02_57");
        roofGroups.put(1, "roofs_01_22");
        roofGroups.put(2, "roofs_01_54");
        roofGroups.put(3, "roofs_02_22");
        roofGroups.put(4, "invisible_01_0");
        roofGroups.put(5, "roofs_03_22");
        roofGroups.put(6, "roofs_03_54");
        roofGroups.put(7, "roofs_04_22");
        roofGroups.put(8, "roofs_04_54");
        roofGroups.put(9, "roofs_05_22");
        roofGroups.put(10, "roofs_05_54");

        for (int i = 0; i < placeFloorInfos.length; i++) {
            placeFloorInfos[i] = new IsoRoofFixer.PlaceFloorInfo();
        }

        sqCache = new IsoGridSquare[8];
        interiorAirSpaces = new int[8];
    }

    private static final class PlaceFloorInfo {
        private IsoGridSquare square;
        private int floorType;

        private void set(IsoGridSquare s, int t) {
            this.square = s;
            this.floorType = t;
        }
    }
}
