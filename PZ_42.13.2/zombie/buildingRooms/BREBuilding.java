// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.buildingRooms;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.areas.IsoRoom;

@UsedFromLua
public final class BREBuilding {
    final BuildingDef buildingDef = new BuildingDef(true);
    final ArrayList<BRERoom> rooms = new ArrayList<>();
    boolean edited;

    public int getRoomCount() {
        return this.rooms.size();
    }

    public BRERoom getRoomByIndex(int index) {
        return this.rooms.get(index);
    }

    public boolean isEdited() {
        return this.edited;
    }

    public void setEdited(boolean b) {
        this.edited = b;
    }

    public int getRoomIndexAt(int x, int y, int z) {
        for (int i = 0; i < this.getRoomCount(); i++) {
            BRERoom room = this.getRoomByIndex(i);
            if (room.contains(x, y, z)) {
                return i;
            }
        }

        return -1;
    }

    public BRERoom createRoom(int level) {
        BRERoom room = new BRERoom(this);
        room.roomDef.level = PZMath.clamp(level, -32, 31);
        BuildingRoomsEditor.getInstance().callLua("BeforeAddRoom", this, room);
        this.rooms.add(room);
        this.buildingDef.getRooms().add(room.roomDef);
        BuildingRoomsEditor.getInstance().callLua("AfterAddRoom", this, room);
        return room;
    }

    public void removeRoom(BRERoom room) {
        BuildingRoomsEditor.getInstance().callLua("BeforeRemoveRoom", this, room);
        this.buildingDef.getRooms().remove(room.roomDef);
        this.rooms.remove(room);
        BuildingRoomsEditor.getInstance().callLua("AfterRemoveRoom", this, room);
    }

    public boolean isAdjacent(int x, int y, int w, int h, int z) {
        for (BRERoom room : this.rooms) {
            if (room.getLevel() == z && room.isAdjacent(x, y, w, h)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersects(int x, int y, int w, int h, int z) {
        for (BRERoom room : this.rooms) {
            if (room.getLevel() == z && room.intersects(x, y, w, h)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasNonEmptyRoomsOnLevel(int z) {
        for (BRERoom room : this.rooms) {
            if (room.getLevel() == z && room.getRectangleCount() > 0) {
                return true;
            }
        }

        return false;
    }

    public BREBuilding copyFrom(BuildingDef buildingDef2) {
        for (RoomDef roomDef2 : buildingDef2.getRooms()) {
            BRERoom room1 = this.createRoom(roomDef2.level);
            room1.copyFrom(roomDef2);
        }

        for (RoomDef roomDef2 : buildingDef2.getEmptyOutside()) {
            BRERoom room1 = this.createRoom(roomDef2.level);
            room1.copyFrom(roomDef2);
        }

        return this;
    }

    public boolean isValid() {
        for (BRERoom room : this.rooms) {
            if (!room.isValid()) {
                return false;
            }
        }

        return !this.rooms.isEmpty();
    }

    private void mergeRoomsOntoMetaCell(ArrayList<RoomDef> rooms, IsoMetaCell metaCell) {
        for (int i = 0; i < rooms.size(); i++) {
            RoomDef roomDef = rooms.get(i);
            roomDef.id = RoomID.makeID(metaCell.getX(), metaCell.getY(), metaCell.roomList.size());
            if (metaCell.rooms.containsKey(roomDef.id)) {
                DebugLog.General.error("duplicate RoomDef.ID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
            }

            roomDef.metaId = roomDef.calculateMetaID(metaCell.getX(), metaCell.getY());
            metaCell.rooms.put(roomDef.id, roomDef);
            metaCell.roomList.add(roomDef);
            if (metaCell.roomByMetaId.contains(roomDef.metaId)) {
                DebugLog.General.error("duplicate RoomDef.metaID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
            }

            metaCell.roomByMetaId.put(roomDef.metaId, roomDef);
        }
    }

    private boolean addToLotHeaders(boolean bLoading) {
        IsoMetaCell metaCell = getLotHeader(this.buildingDef);
        if (metaCell == null) {
            return false;
        } else {
            int cellX = metaCell.getX();
            int cellY = metaCell.getY();
            this.buildingDef.metaId = this.buildingDef.calculateMetaID(cellX, cellY);
            BuildingDef existing = metaCell.buildingByMetaId.get(this.buildingDef.metaId);
            if (existing != null) {
                DebugLog.General.error("duplicate BuildingDef.metaID for building at %d,%d", this.buildingDef.x, this.buildingDef.y);
            }

            int buildingIndex = metaCell.buildings.size();
            this.buildingDef.id = BuildingID.makeID(cellX, cellY, buildingIndex);
            this.mergeRoomsOntoMetaCell(this.buildingDef.rooms, metaCell);
            this.mergeRoomsOntoMetaCell(this.buildingDef.emptyoutside, metaCell);
            metaCell.buildings.add(this.buildingDef);
            metaCell.buildingByMetaId.put(this.buildingDef.metaId, this.buildingDef);
            return true;
        }
    }

    private void addToMetaGrid() {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int cellX = this.buildingDef.x / 256;
        int cellY = this.buildingDef.y / 256;
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            metaCell.addRooms(this.buildingDef.rooms, cellX * 256, cellY * 256);
            metaCell.addRooms(this.buildingDef.emptyoutside, cellX * 256, cellY * 256);
            metaGrid.addRoomsToAdjacentCells(this.buildingDef);
            if (!metaGrid.buildings.contains(this.buildingDef)) {
                metaGrid.buildings.add(this.buildingDef);
            }
        }
    }

    static void calculateBounds(BuildingDef buildingDef) {
        for (RoomDef roomDef : buildingDef.getRooms()) {
            roomDef.building = buildingDef;
            roomDef.CalculateBounds();
        }

        for (RoomDef roomDef : buildingDef.getEmptyOutside()) {
            roomDef.building = buildingDef;
            roomDef.CalculateBounds();
        }

        buildingDef.CalculateBounds(new ArrayList<>());
    }

    public void applyChanges(boolean bLoading) {
        for (int i = this.buildingDef.getRooms().size() - 1; i >= 0; i--) {
            RoomDef roomDef = this.buildingDef.getRooms().get(i);
            if (roomDef.isEmptyOutside()) {
                this.buildingDef.getEmptyOutside().add(roomDef);
                this.buildingDef.getRooms().remove(i);
            }
        }

        calculateBounds(this.buildingDef);
        boolean bAdded = this.addToLotHeaders(bLoading);
        if (bAdded) {
            this.addToMetaGrid();
        }
    }

    static void removeFromWorld(BuildingDef buildingDef, boolean bLoading) {
        calculateBounds(buildingDef);
        removeFromMetaCell(buildingDef, bLoading);
        removeFromMetaGrid(buildingDef);
    }

    private static void removeFromMetaCell(BuildingDef buildingDef, boolean bLoading) {
        IsoMetaCell metaCell = getLotHeader(buildingDef);
        if (metaCell != null) {
            for (RoomDef roomDef : buildingDef.getRooms()) {
                removeRoomDef(metaCell, roomDef, bLoading);
            }

            for (RoomDef roomDef : buildingDef.getEmptyOutside()) {
                removeRoomDef(metaCell, roomDef, bLoading);
            }

            metaCell.buildings.remove(buildingDef);
            metaCell.buildingByMetaId.remove(buildingDef.metaId);
            metaCell.isoBuildings.remove(buildingDef.id);
        }
    }

    private static void removeRoomDef(IsoMetaCell metaCell, RoomDef roomDef, boolean bLoading) {
        metaCell.rooms.remove(roomDef.id);
        metaCell.roomList.remove(roomDef);
        metaCell.roomByMetaId.remove(roomDef.metaId);
        IsoRoom isoRoom = metaCell.isoRooms.remove(roomDef.id);
        if (isoRoom != null) {
            removeIsoRoom(isoRoom, bLoading);
        }
    }

    private static void removeIsoRoom(IsoRoom isoRoom, boolean bLoading) {
        isoRoom.building = null;
        isoRoom.def = null;
        isoRoom.lightSwitches.clear();
        isoRoom.rects.clear();
        if (!bLoading) {
            ArrayList<IsoRoomLight> roomLightsWorld = IsoWorld.instance.currentCell.roomLights;

            for (IsoRoomLight roomLight : isoRoom.roomLights) {
                roomLight.active = false;
                roomLightsWorld.remove(roomLight);
                if (roomLight.id != 0) {
                    int ID = roomLight.id;
                    roomLight.id = 0;
                    LightingJNI.removeRoomLight(ID);
                    GameTime.instance.lightSourceUpdate = 100.0F;
                }
            }
        }

        isoRoom.roomLights.clear();
        isoRoom.squares.clear();
    }

    private static void removeFromMetaGrid(BuildingDef buildingDef) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int cellX = buildingDef.x / 256;
        int cellY = buildingDef.y / 256;
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            metaCell.removeRooms(buildingDef.rooms);
            metaCell.removeRooms(buildingDef.emptyoutside);
            metaGrid.removeRoomsFromAdjacentCells(buildingDef);
            metaGrid.buildings.remove(buildingDef);
        }
    }

    static IsoMetaCell getLotHeader(BuildingDef buildingDef) {
        int cellX = buildingDef.x / 256;
        int cellY = buildingDef.y / 256;
        return IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
    }
}
