// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.metagrid;

import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;

public final class WorldRegionToMetaGrid {
    private final ArrayList<BuildingDef> tempBuildingList = new ArrayList<>();
    private final ArrayList<RoomDef> tempRooms = new ArrayList<>();
    private final ArrayList<IsoWorldRegion> tempRegionList = new ArrayList<>();
    private final ArrayList<IsoWorldRegion> worldRegions = new ArrayList<>();
    private final ArrayList<IsoWorldRegion> allWorldRegions = new ArrayList<>();
    private final HashSet<IsoWorldRegion> done = new HashSet<>();
    private final HashSet<BuildingDef> added = new HashSet<>();
    private final HashMap<Integer, ArrayList<BuildingDef>> buildingsByLevel = new HashMap<>();
    private final HashMap<BuildingDef, ArrayList<IsoWorldRegion>> buildingToRegions = new HashMap<>();
    private final TShortArrayList freeSquares = new TShortArrayList();

    public void clientProcessBuildings(ArrayList<IsoGameCharacter.Location> changedCells) {
        if (!changedCells.isEmpty()) {
            int removedBuildings = 0;
            int addedBuildings = 0;

            for (IsoGameCharacter.Location location : changedCells) {
                for (int i = 0; i <= 8; i++) {
                    IsoDirections dir = i == 8 ? IsoDirections.Max : IsoDirections.fromIndex(i);
                    removedBuildings += this.removeUserDefinedBuildingsFromCell(location.x + dir.dx(), location.y + dir.dy());
                }
            }

            this.done.clear();
            this.added.clear();
            this.allWorldRegions.clear();

            for (ArrayList<BuildingDef> list : this.buildingsByLevel.values()) {
                list.clear();
            }

            for (ArrayList<IsoWorldRegion> list : this.buildingToRegions.values()) {
                list.clear();
            }

            for (IsoGameCharacter.Location location : changedCells) {
                for (int i = 0; i <= 8; i++) {
                    IsoDirections dir = i == 8 ? IsoDirections.Max : IsoDirections.fromIndex(i);
                    this.createBuildingsFromRegions(location.x + dir.dx(), location.y + dir.dy(), this.done, this.added, this.buildingsByLevel);
                }
            }

            for (int z = 0; z < 31; z++) {
                ArrayList<BuildingDef> buildings = this.buildingsByLevel.get(z);
                if (buildings != null) {
                    for (int i = buildings.size() - 1; i >= 0; i--) {
                        BuildingDef buildingDef = buildings.get(i);
                        if (this.isAdjacentToOrOverlappingAPredefinedBuilding(buildingDef)) {
                            this.discardBuilding(buildingDef, true);
                            buildings.remove(i);
                        }
                    }
                }
            }

            for (int zx = 0; zx < 31; zx++) {
                ArrayList<BuildingDef> buildings = this.buildingsByLevel.get(zx);
                if (buildings != null) {
                    for (int ix = 0; ix < buildings.size(); ix++) {
                        BuildingDef buildingDef = buildings.get(ix);
                        this.combineBuildingLevels(buildingDef, this.buildingsByLevel);
                    }
                }
            }

            for (int zxx = 0; zxx < 31; zxx++) {
                ArrayList<BuildingDef> buildings = this.buildingsByLevel.get(zxx);
                if (buildings != null) {
                    for (int ix = 0; ix < buildings.size(); ix++) {
                        BuildingDef buildingDef = buildings.get(ix);
                        this.addToMetaGrid(buildingDef);
                        addedBuildings++;
                    }
                }
            }

            if (Core.debug) {
                for (IsoWorldRegion worldRegion : this.allWorldRegions) {
                    if (worldRegion.getBuildingDef() == null) {
                        throw new IllegalStateException("world region has no assigned building");
                    }
                }

                for (ArrayList<BuildingDef> buildings : this.buildingsByLevel.values()) {
                    for (BuildingDef buildingDef : buildings) {
                        for (RoomDef room : buildingDef.getRooms()) {
                            for (RoomDef.RoomRect rect : room.getRects()) {
                                if (IsoRegions.getIsoWorldRegion(rect.x, rect.y, room.level) instanceof IsoWorldRegion iwr
                                    && iwr.getBuildingDef() != buildingDef) {
                                    DebugLog.General.error("world region building isn't the expected one");
                                }
                            }
                        }
                    }
                }
            }

            for (ArrayList<BuildingDef> list : this.buildingsByLevel.values()) {
                list.clear();
            }

            for (ArrayList<IsoWorldRegion> list : this.buildingToRegions.values()) {
                if (Core.debug) {
                    for (IsoWorldRegion region : list) {
                        if (region.getBuildingDef() == null) {
                            DebugLog.General.error("world region building isn't the expected one");
                        }
                    }
                }

                list.clear();
            }

            if (removedBuildings != 0 || addedBuildings != 0) {
                LightingJNI.buildingsChanged();
                this.updateSquares();
                IsoGridOcclusionData.SquareChanged();
                FBORenderCutaways.getInstance().squareChanged(null);
            }
        }
    }

    private void createBuildingsFromRegions(
        int cellX, int cellY, HashSet<IsoWorldRegion> done, HashSet<BuildingDef> added, HashMap<Integer, ArrayList<BuildingDef>> buildingsByLevel
    ) {
        IsoRegions.getIsoWorldRegionsInCell(cellX, cellY, this.worldRegions);

        for (IsoWorldRegion worldRegion : this.worldRegions) {
            if (worldRegion.isEnclosed() && !(worldRegion.getRoofedPercentage() < 0.5F) && !done.contains(worldRegion)) {
                BuildingDef buildingDef = worldRegion.getBuildingDef();
                if (buildingDef != null) {
                    if (added.contains(buildingDef)) {
                        continue;
                    }

                    boolean level = true;
                }

                buildingDef = this.regionToBuildingDef(worldRegion, done, added);
                int level = buildingDef.getMinLevel();
                if (!buildingsByLevel.containsKey(level)) {
                    buildingsByLevel.put(level, new ArrayList<>());
                }

                buildingsByLevel.get(level).add(buildingDef);
                added.add(buildingDef);
            }
        }
    }

    private int removeUserDefinedBuildingsFromCell(int cellX, int cellY) {
        IsoRegions.getIsoWorldRegionsInCell(cellX, cellY, this.worldRegions);

        for (IsoWorldRegion worldRegion : this.worldRegions) {
            if (worldRegion.getBuildingDef() != null) {
                worldRegion.setBuildingDef(null);
            }
        }

        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell == null) {
            return 0;
        } else {
            int removed = 0;

            for (int i = metaCell.buildings.size() - 1; i >= 0; i--) {
                BuildingDef buildingDef = metaCell.buildings.get(i);
                if (buildingDef.isUserDefined()) {
                    this.removeFromMetaGrid(buildingDef);
                    this.discardBuilding(buildingDef, true);
                    removed++;
                }
            }

            return removed;
        }
    }

    private BuildingDef regionToBuildingDef(IsoWorldRegion worldRegion, HashSet<IsoWorldRegion> done, HashSet<BuildingDef> added) {
        BuildingDef buildingDef = Pools.buildingDef.alloc();
        buildingDef.setUserDefined(true);
        this.buildingToRegions.put(buildingDef, new ArrayList<>());
        this.tempRegionList.clear();
        this.getConnectedRegions(worldRegion, this.tempRegionList);

        for (IsoWorldRegion neighbour : this.tempRegionList) {
            if (!done.contains(neighbour)) {
                done.add(neighbour);
                if (neighbour.isEnclosed() && !(neighbour.getRoofedPercentage() < 0.5F)) {
                    if (neighbour.getBuildingDef() != null) {
                        if (added.contains(neighbour.getBuildingDef())) {
                            continue;
                        }

                        DebugLog.General.error("neighbour already has a BuildingDef");
                    }

                    RoomDef roomDef = this.regionToRoomDef(neighbour);
                    roomDef.building = buildingDef;
                    roomDef.CalculateBounds();
                    buildingDef.getRooms().add(roomDef);
                    neighbour.setBuildingDef(buildingDef);
                    this.buildingToRegions.get(buildingDef).add(neighbour);
                    this.allWorldRegions.add(neighbour);
                }
            }
        }

        buildingDef.CalculateBounds(this.tempRooms);
        return buildingDef;
    }

    private RoomDef regionToRoomDef(IsoWorldRegion worldRegion) {
        RoomDef roomDef = Pools.roomDef.alloc();
        roomDef.id = 0L;
        roomDef.setName("");
        roomDef.explored = true;
        roomDef.userDefined = true;
        int z = Integer.MAX_VALUE;

        for (IsoChunkRegion chunkRegion : worldRegion.getDebugIsoChunkRegionCopy()) {
            if (z == Integer.MAX_VALUE) {
                z = chunkRegion.getzLayer();
            } else if (z != chunkRegion.getzLayer()) {
                boolean dataChunk = true;
            }

            DataChunk dataChunk = chunkRegion.getDataChunk();
            this.freeSquares.resetQuick();

            for (int i = 63; i >= 0; i--) {
                int lx = i % 8;
                int ly = i / 8;
                byte flags = dataChunk.getSquare(lx, ly, z);
                if (flags > 0 && dataChunk.getIsoChunkRegion(lx, ly, z) == chunkRegion) {
                    this.freeSquares.add((short)i);
                }
            }

            int chunkX = dataChunk.getChunkX() * 8;
            int chunkY = dataChunk.getChunkY() * 8;

            while (!this.freeSquares.isEmpty()) {
                int index = this.freeSquares.get(this.freeSquares.size() - 1);
                int lx = index % 8;
                int ly = index / 8;
                RoomDef.RoomRect rect = this.addRoomRect(roomDef, chunkRegion, lx, ly, z);

                for (int y = rect.getY(); y < rect.getY2(); y++) {
                    for (int x = rect.getX(); x < rect.getX2(); x++) {
                        index = x - chunkX + (y - chunkY) * 8;
                        this.freeSquares.remove((short)index);
                    }
                }
            }
        }

        roomDef.level = z;
        return roomDef;
    }

    private int getSpan(DataChunk dataChunk, IsoChunkRegion chunkRegion, int lx, int ly, int z) {
        int span;
        for (span = 0; lx < 8; span++) {
            byte flags = dataChunk.getSquare(lx, ly, z);
            int index = lx + ly * 8;
            if (flags <= 0 || !this.freeSquares.contains((short)index)) {
                break;
            }

            lx++;
        }

        return span;
    }

    private RoomDef.RoomRect addRoomRect(RoomDef roomDef, IsoChunkRegion chunkRegion, int lx, int ly, int z) {
        DataChunk dataChunk = chunkRegion.getDataChunk();
        int y = ly;
        int h = 1;
        int w = this.getSpan(dataChunk, chunkRegion, lx, ly, z);
        ly++;

        while (ly < 8 && this.getSpan(dataChunk, chunkRegion, lx, ly, z) >= w) {
            ly++;
            h++;
        }

        RoomDef.RoomRect rect = Pools.roomRect.alloc();
        rect.set(dataChunk.getChunkX() * 8 + lx, dataChunk.getChunkY() * 8 + y, w, h);
        roomDef.getRects().add(rect);
        return rect;
    }

    private void getConnectedRegions(IsoWorldRegion worldRegion, ArrayList<IsoWorldRegion> result) {
        if (!result.contains(worldRegion)) {
            result.add(worldRegion);

            for (IsoWorldRegion neighbour : worldRegion.getNeighbors()) {
                if (neighbour.isEnclosed() && neighbour.getRoofedPercentage() >= 0.5F) {
                    this.getConnectedRegions(neighbour, result);
                }
            }
        }
    }

    private void addToMetaGrid(BuildingDef buildingDef) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int cellX = buildingDef.getCellX();
        int cellY = buildingDef.getCellY();
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            this.mergeRoomsOntoMetaCell(buildingDef, buildingDef.rooms, metaCell);
            this.mergeRoomsOntoMetaCell(buildingDef, buildingDef.emptyoutside, metaCell);
            buildingDef.metaId = buildingDef.calculateMetaID(cellX, cellY);
            if (metaCell.buildingByMetaId.containsKey(buildingDef.metaId)) {
                DebugLog.General.error("duplicate building metaID");
            }

            metaCell.buildings.add(buildingDef);
            metaCell.buildingByMetaId.put(buildingDef.metaId, buildingDef);
            if (!metaGrid.buildings.contains(buildingDef)) {
                metaGrid.buildings.add(buildingDef);
            }

            this.recalculateBuildingAndRoomIDs(metaCell);
        }
    }

    private void removeFromMetaGrid(BuildingDef buildingDef) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int cellX = buildingDef.getCellX();
        int cellY = buildingDef.getCellY();
        IsoMetaCell metaCell = metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            BuildingDef buildingDef1 = metaCell.buildingByMetaId.get(buildingDef.metaId);
            if (buildingDef1 != null && buildingDef1 != buildingDef) {
                buildingDef = buildingDef1;
            }

            metaCell.removeRooms(buildingDef.getRooms());
            metaCell.removeRooms(buildingDef.getEmptyOutside());
            metaGrid.removeRoomsFromAdjacentCells(buildingDef);
            this.removeBuildingFromMetaCell(buildingDef, metaCell);
            this.recalculateBuildingAndRoomIDs(metaCell);
        }
    }

    private void removeBuildingFromMetaCell(BuildingDef buildingDef, IsoMetaCell metaCell) {
        for (int i = 0; i < buildingDef.rooms.size(); i++) {
            RoomDef roomDef = buildingDef.rooms.get(i);
            this.removeRoomDef(metaCell, roomDef, false);
        }

        for (int i = 0; i < buildingDef.emptyoutside.size(); i++) {
            RoomDef roomDef = buildingDef.emptyoutside.get(i);
            this.removeRoomDef(metaCell, roomDef, false);
        }

        metaCell.buildings.remove(buildingDef);
        metaCell.buildingByMetaId.remove(buildingDef.metaId);
        metaCell.isoBuildings.remove(buildingDef.id);
    }

    private void removeRoomDef(IsoMetaCell metaCell, RoomDef roomDef, boolean bLoading) {
        metaCell.rooms.remove(roomDef.id);
        metaCell.roomList.remove(roomDef);
        metaCell.roomByMetaId.remove(roomDef.metaId);
        IsoRoom isoRoom = metaCell.isoRooms.remove(roomDef.id);
        if (isoRoom != null) {
            this.removeIsoRoom(isoRoom, bLoading);
        }
    }

    private void removeIsoRoom(IsoRoom isoRoom, boolean bLoading) {
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

    private void recalculateBuildingAndRoomIDs(IsoMetaCell metaCell) {
        int cellX = metaCell.getX();
        int cellY = metaCell.getY();
        metaCell.rooms.clear();

        for (int i = 0; i < metaCell.roomList.size(); i++) {
            RoomDef roomDef = metaCell.roomList.get(i);
            roomDef.id = RoomID.makeID(cellX, cellY, i);
            metaCell.rooms.put(roomDef.id, roomDef);
        }

        for (int i = 0; i < metaCell.buildings.size(); i++) {
            BuildingDef buildingDef = metaCell.buildings.get(i);
            buildingDef.id = BuildingID.makeID(cellX, cellY, i);
        }

        ArrayList<IsoBuilding> isoBuildings = new ArrayList<>(metaCell.isoBuildings.values());
        metaCell.isoBuildings.clear();

        for (IsoBuilding isoBuilding : isoBuildings) {
            metaCell.isoBuildings.put(isoBuilding.def.id, isoBuilding);
        }

        ArrayList<IsoRoom> isoRooms = new ArrayList<>(metaCell.isoRooms.values());
        metaCell.isoRooms.clear();

        for (IsoRoom isoRoom : isoRooms) {
            metaCell.isoRooms.put(isoRoom.def.id, isoRoom);
        }
    }

    private void mergeRoomsOntoMetaCell(BuildingDef buildingDef, ArrayList<RoomDef> rooms, IsoMetaCell metaCell) {
        if (!rooms.isEmpty()) {
            for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
                RoomDef roomDef = rooms.get(roomIndex);
                roomDef.id = RoomID.makeID(metaCell.getX(), metaCell.getY(), metaCell.roomList.size());
                roomDef.metaId = roomDef.calculateMetaID(metaCell.getX(), metaCell.getY());
                if (metaCell.rooms.containsKey(roomDef.id)) {
                    DebugLog.General.error("duplicate RoomDef.ID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
                }

                metaCell.rooms.put(roomDef.id, roomDef);
                metaCell.roomList.add(roomDef);
                if (metaCell.roomByMetaId.contains(roomDef.metaId)) {
                    DebugLog.General.error("duplicate RoomDef.metaID for room at %d,%d,%d", roomDef.x, roomDef.y, roomDef.level);
                }

                metaCell.roomByMetaId.put(roomDef.metaId, roomDef);
            }

            IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
            metaCell.addRooms(rooms, buildingDef.getCellX() * 256, buildingDef.getCellY() * 256);
            metaGrid.addRoomsToAdjacentCells(buildingDef, rooms);
        }
    }

    private boolean isAdjacentToOrOverlappingAPredefinedBuilding(BuildingDef buildingDef) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        this.tempBuildingList.clear();
        metaGrid.getBuildingsIntersecting(buildingDef.getX() - 1, buildingDef.getY() - 1, buildingDef.getW() + 2, buildingDef.getH() + 2, this.tempBuildingList);

        for (BuildingDef buildingDef1 : this.tempBuildingList) {
            if (buildingDef != buildingDef1 && buildingDef.isBasement() == buildingDef1.isBasement() && !buildingDef1.isUserDefined()) {
                if (buildingDef.isAdjacent(buildingDef1, true)) {
                    return true;
                }

                if (buildingDef.overlaps(buildingDef1, true)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void combineBuildingLevels(BuildingDef buildingDef, HashMap<Integer, ArrayList<BuildingDef>> buildingsByLevel) {
        for (int z = buildingDef.getMaxLevel() + 1; z < 32; z++) {
            ArrayList<BuildingDef> buildings = buildingsByLevel.get(z);
            if (buildings != null) {
                for (int i = buildings.size() - 1; i >= 0; i--) {
                    BuildingDef buildingDef1 = buildings.get(i);
                    if (buildingDef1 == buildingDef) {
                        throw new IllegalStateException("building is in two levels");
                    }

                    if (buildingDef.isAdjacent(buildingDef1, true) || buildingDef.overlaps(buildingDef1, true)) {
                        buildingDef.addRoomsOf(buildingDef1, this.tempRooms);
                        ArrayList<IsoWorldRegion> regions = this.buildingToRegions.get(buildingDef1);
                        this.assignBuildingToRegions(regions, buildingDef);
                        regions.clear();
                        this.discardBuilding(buildingDef1, false);
                        buildingDef.resetMinMaxLevel();
                        buildings.remove(i);
                    }
                }
            }
        }
    }

    private void discardBuilding(BuildingDef buildingDef, boolean bReleaseRooms) {
        ArrayList<IsoWorldRegion> regions = this.buildingToRegions.get(buildingDef);
        this.assignBuildingToRegions(regions, null);
        regions.clear();
        this.buildingToRegions.remove(buildingDef);
        if (bReleaseRooms) {
            this.releaseRooms(buildingDef.getRooms());
            this.releaseRooms(buildingDef.getEmptyOutside());
        } else {
            buildingDef.getRooms().clear();
            buildingDef.getEmptyOutside().clear();
        }

        buildingDef.resetMinMaxLevel();
        Pools.buildingDef.release(buildingDef);
    }

    private void releaseRooms(ArrayList<RoomDef> rooms) {
        for (int i = 0; i < rooms.size(); i++) {
            RoomDef roomDef = rooms.get(i);
            Pools.roomRect.releaseAll(roomDef.getRects());
            roomDef.getRects().clear();
            roomDef.building = null;
            Pools.roomDef.release(roomDef);
        }

        rooms.clear();
    }

    private void assignBuildingToRegions(ArrayList<IsoWorldRegion> regions, BuildingDef buildingDef) {
        for (int i = 0; i < regions.size(); i++) {
            IsoWorldRegion region = regions.get(i);
            region.setBuildingDef(buildingDef);
            if (buildingDef == null) {
                this.allWorldRegions.remove(region);
            } else {
                if (!this.allWorldRegions.contains(region)) {
                    this.allWorldRegions.add(region);
                }

                this.buildingToRegions.get(buildingDef).add(region);
            }
        }
    }

    private void updateSquares() {
        this.forEachSquare(square -> {
            IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
            RoomDef roomDef = metaGrid.getRoomAt(square.x, square.y, square.z);
            long roomID = roomDef == null ? -1L : roomDef.id;
            square.setRoomID(roomID);
            roomDef = metaGrid.getEmptyOutsideAt(square.x, square.y, square.z);
            if (roomDef != null) {
                IsoRoom room = square.getChunk().getRoom(roomDef.id);
                square.roofHideBuilding = room == null ? null : room.building;
            }

            IsoRoom room = square.getRoom();
            if (roomID != -1L && room != null) {
                if (room.roomLights.isEmpty()) {
                    room.createLights(room.getRoomDef().lightsActive);
                    if (!GameServer.server) {
                        ArrayList<IsoRoomLight> roomLightsWorld = IsoWorld.instance.currentCell.roomLights;

                        for (int i = 0; i < room.roomLights.size(); i++) {
                            IsoRoomLight roomLight = room.roomLights.get(i);
                            if (!roomLightsWorld.contains(roomLight)) {
                                roomLightsWorld.add(roomLight);
                            }
                        }
                    }
                }

                for (int ix = 0; ix < square.getObjects().size(); ix++) {
                    IsoObject object = square.getObjects().get(ix);
                    if (object instanceof IsoGenerator generator && generator.isActivated() && !square.getProperties().has(IsoFlagType.exterior)) {
                        room.getBuilding().setToxic(true);
                    }

                    if (object instanceof IsoLightSwitch lightSwitch && !room.lightSwitches.contains(lightSwitch)) {
                        room.lightSwitches.add(lightSwitch);
                    }
                }
            }
        });
        this.forEachSquare(square -> {
            square.associatedBuilding = IsoWorld.instance.getMetaGrid().getAssociatedBuildingAt(square.x, square.y);
            square.RecalcProperties();
            PolygonalMap2.instance.squareChanged(square);
        });
        this.forEachChunk(chunk -> {
            chunk.invalidateRenderChunkLevels(2112L);
            chunk.getCutawayData().invalidateAll();
            chunk.checkLightingLater_AllPlayers_AllLevels();
        });
    }

    private void forEachChunk(Consumer<IsoChunk> consumer) {
        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(i);
            if (chunkMap != null && !chunkMap.ignore) {
                for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
                    for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                        IsoChunk chunk = chunkMap.getChunk(cx, cy);
                        if (chunk != null) {
                            consumer.accept(chunk);
                        }
                    }
                }
            }
        }
    }

    private void forEachSquare(Consumer<IsoGridSquare> consumer) {
        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(i);
            if (chunkMap != null && !chunkMap.ignore) {
                for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; cy++) {
                    for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; cx++) {
                        IsoChunk chunk = chunkMap.getChunk(cx, cy);
                        if (chunk != null) {
                            for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); z++) {
                                IsoGridSquare[] squares = chunk.getSquaresForLevel(z);

                                for (int j = 0; j < squares.length; j++) {
                                    IsoGridSquare square = squares[j];
                                    if (square != null) {
                                        consumer.accept(square);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
