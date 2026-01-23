// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public class NewMapBinaryFile {
    public final boolean pot;
    public final int chunkDim;
    public final int chunksPerCell;
    public final int cellDim;
    private final ArrayList<RoomDef> tempRooms = new ArrayList<>();
    private final SharedStrings sharedStrings = new SharedStrings();

    public NewMapBinaryFile(boolean pot) {
        this.pot = pot;
        this.chunkDim = pot ? 8 : 10;
        this.chunksPerCell = pot ? 32 : 30;
        this.cellDim = pot ? 256 : 300;
    }

    public static void SpawnBasement(String name, int x, int y) throws IOException {
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        String fileName = "media/binmap/" + name + ".pzby";
        NewMapBinaryFile.Header header = file.loadHeader(fileName);
        IsoPlayer player = IsoPlayer.getInstance();
        player.ensureOnTile();
        IsoChunk chunk = player.getSquare().chunk;
        int cellX = chunk.wx * 8 / 256;
        int cellY = chunk.wy * 8 / 256;
        IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(cellX, cellY);
        if (metaCell != null) {
            int minChunkWorldX = x / 8;
            int minChunkWorldY = y / 8;
            int maxChunkWorldX = x / 8 + header.width - 1;
            int maxChunkWorldY = y / 8 + header.height - 1;
            if (minChunkWorldX / 32 == maxChunkWorldX / 32) {
                if (minChunkWorldY / 32 == maxChunkWorldY / 32) {
                    file.addBasementRoomsToMetaGrid(metaCell, header, x, y);

                    for (int ly = 0; ly < header.height; ly++) {
                        for (int lx = 0; lx < header.width; lx++) {
                            NewMapBinaryFile.ChunkData chunkData = file.loadChunk(header, lx, ly);
                            file.setChunkInWorldArb(chunkData, 0, 0, 0, x + lx * file.chunkDim, y + ly * file.chunkDim, -header.levels);
                        }
                    }

                    for (int cx = minChunkWorldX; cx <= maxChunkWorldX; cx++) {
                        for (int cy = minChunkWorldY; cy <= maxChunkWorldY; cy++) {
                            IsoChunk cc = IsoCell.getInstance().getChunk(cx, cy);
                            if (cc != null) {
                                for (IsoRoomLight roomLight : cc.roomLights) {
                                    if (!IsoCell.getInstance().roomLights.contains(roomLight)) {
                                        IsoCell.getInstance().roomLights.add(roomLight);
                                    }
                                }
                            }
                        }
                    }

                    for (int xx = x; xx < x + header.width * file.chunkDim; xx++) {
                        for (int yy = y; yy < y + header.height * file.chunkDim; yy++) {
                            IsoGridSquare sq = IsoCell.getInstance().getGridSquare(xx, yy, 0);
                            if (sq != null && sq.HasStairsBelow()) {
                                IsoGridSquare sq2 = sq;
                                IsoObject[] el = sq.getObjects().getElements();

                                for (int i = 0; i < sq2.getObjects().size(); i++) {
                                    IsoObject isoObject = el[i];
                                    if (isoObject.isFloor()) {
                                        sq2.DeleteTileObject(isoObject);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    IsoCell.getInstance().chunkMap[IsoPlayer.getPlayerIndex()].calculateZExtentsForChunkMap();
                }
            }
        }
    }

    public static void SpawnBasementInChunk(IsoChunk chunk, String name, int x, int y, int bottomZ) throws IOException {
        DebugLog.Basement.println("SpawnBasementInChunk : " + name + ", at: " + x + ", " + y);
        boolean isBasementAccess = name.startsWith("ba_");
        NewMapBinaryFile file = new NewMapBinaryFile(true);
        String fileName = "media/binmap/" + name + ".pzby";
        if (isBasementAccess) {
            fileName = "media/basement_access/" + name + ".pzby";
        }

        NewMapBinaryFile.Header header = file.loadHeader(fileName);
        int CPW = 8;
        int cx = chunk.wx * 8;
        int cy = chunk.wy * 8;
        int bx1 = Math.max(cx, x) - x;
        int by1 = Math.max(cy, y) - y;
        int bx2 = Math.min(cx + 8, x + header.width * file.chunkDim) - 1 - x;
        int by2 = Math.min(cy + 8, y + header.height * file.chunkDim) - 1 - y;

        for (int ly = by1 / file.chunkDim; ly <= by2 / file.chunkDim; ly++) {
            for (int lx = bx1 / file.chunkDim; lx <= bx2 / file.chunkDim; lx++) {
                NewMapBinaryFile.ChunkData chunkData = file.loadChunk(header, lx, ly);
                file.addToIsoChunk(chunk, chunkData, x + lx * file.chunkDim, y + ly * file.chunkDim, bottomZ, isBasementAccess);
            }
        }
    }

    private boolean addBasementRoomsToMetaGrid(IsoMetaCell metaCell, NewMapBinaryFile.Header header, int x, int y) {
        ArrayList<RoomDef> roomDefs = new ArrayList<>();
        metaCell.getRoomsIntersecting(x, y, header.width * header.file.chunkDim, header.height * header.file.chunkDim, roomDefs);
        int cellX = x / 256;
        int cellY = y / 256;
        if (roomDefs.isEmpty()) {
            return false;
        } else {
            BuildingDef building = roomDefs.get(0).building;
            int offX = x - building.x;
            int offY = y - building.y;
            ArrayList<RoomDef> m_roomDefList = header.roomDefList;

            for (int i = 0; i < m_roomDefList.size(); i++) {
                RoomDef roomDef = m_roomDefList.get(i);
                RoomDef newRoom = new RoomDef(RoomID.makeID(metaCell.info.cellX, metaCell.info.cellY, metaCell.rooms.size()), roomDef.name);
                newRoom.level = roomDef.level - header.levels;

                for (RoomDef.RoomRect rect : roomDef.rects) {
                    newRoom.rects.add(new RoomDef.RoomRect(rect.x + x, rect.y + y, rect.getW(), rect.getH()));
                }

                newRoom.CalculateBounds();
                newRoom.building = building;
                building.rooms.add(newRoom);
                metaCell.addRoom(newRoom, cellX * 256, cellY * 256);
                metaCell.rooms.put(newRoom.id, newRoom);
                IsoRoom isoRoom = newRoom.getIsoRoom();
                isoRoom.createLights(false);
                IsoChunk c = IsoCell.getInstance().getChunk(newRoom.x / 8, newRoom.y / 8);
                if (c != null) {
                    for (IsoRoomLight roomLight : isoRoom.roomLights) {
                        c.roomLights.add(roomLight);
                    }
                }
            }

            building.CalculateBounds(new ArrayList<>(building.rooms));
            return true;
        }
    }

    private void mergeBuildingsIntoMetaGrid(IsoMetaCell metaCell, NewMapBinaryFile.Header header, int minChunkWorldX, int minChunkWorldY) {
        ArrayList<RoomDef> roomDefs = new ArrayList<>();
        metaCell.getRoomsIntersecting(
            minChunkWorldX * 8, minChunkWorldY * 8, header.width * header.file.chunkDim, header.height * header.file.chunkDim, roomDefs
        );

        for (int i = 0; i < roomDefs.size(); i++) {
            RoomDef roomDef = roomDefs.get(i);
            metaCell.roomList.remove(roomDef);
            metaCell.rooms.remove(roomDef.id);
            metaCell.isoRooms.remove(roomDef.id);
            metaCell.buildings.remove(roomDef.building);
            metaCell.isoBuildings.remove(roomDef.building.id);
        }

        for (int i = 0; i < header.buildingDefList.size(); i++) {
            BuildingDef buildingDef = header.buildingDefList.get(i);
            buildingDef.id = BuildingID.makeID(metaCell.getX(), metaCell.getY(), metaCell.buildings.size() + i);
            metaCell.buildings.add(buildingDef);
        }

        for (int i = 0; i < header.roomDefList.size(); i++) {
            RoomDef roomDef = header.roomDefList.get(i);
            roomDef.id = RoomID.makeID(metaCell.getX(), metaCell.getY(), metaCell.roomList.size() + i);
            metaCell.roomList.add(roomDef);
            metaCell.rooms.put(roomDef.id, roomDef);
        }
    }

    public NewMapBinaryFile.Header loadHeader(String fileName) throws IOException {
        fileName = ZomboidFileSystem.instance.getString(fileName);

        NewMapBinaryFile.Header var3;
        try (BufferedRandomAccessFile in = new BufferedRandomAccessFile(fileName, "r", 4096)) {
            var3 = this.loadHeaderInternal(fileName, in);
        }

        return var3;
    }

    private NewMapBinaryFile.Header loadHeaderInternal(String fileName, BufferedRandomAccessFile in) throws IOException {
        NewMapBinaryFile.Header header = new NewMapBinaryFile.Header();
        header.file = this;
        header.fileName = fileName;
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if (b1 == 80 && b2 == 90 && b3 == 66 && b4 == 89) {
            header.version = IsoLot.readInt(in);
            int numTileNames = IsoLot.readInt(in);

            for (int n = 0; n < numTileNames; n++) {
                String str = IsoLot.readString(in);
                header.usedTileNames.add(this.sharedStrings.get(str.trim()));
            }

            header.width = IsoLot.readInt(in);
            header.height = IsoLot.readInt(in);
            header.levels = IsoLot.readInt(in);
            int numRooms = IsoLot.readInt(in);

            for (int n = 0; n < numRooms; n++) {
                String str = IsoLot.readString(in);
                RoomDef roomDef = new RoomDef(n, this.sharedStrings.get(str));
                roomDef.level = IsoLot.readInt(in);
                int numRoomRects = IsoLot.readInt(in);

                for (int rc = 0; rc < numRoomRects; rc++) {
                    RoomDef.RoomRect rect = new RoomDef.RoomRect(IsoLot.readInt(in), IsoLot.readInt(in), IsoLot.readInt(in), IsoLot.readInt(in));
                    roomDef.rects.add(rect);
                }

                roomDef.CalculateBounds();
                header.roomDefMap.put(roomDef.id, roomDef);
                header.roomDefList.add(roomDef);
                int nObjects = IsoLot.readInt(in);

                for (int m = 0; m < nObjects; m++) {
                    int e = IsoLot.readInt(in);
                    int x = IsoLot.readInt(in);
                    int y = IsoLot.readInt(in);
                    roomDef.objects.add(new MetaObject(e, x - roomDef.x, y - roomDef.y, roomDef));
                }

                roomDef.lightsActive = Rand.Next(2) == 0;
            }

            int numBuildings = IsoLot.readInt(in);

            for (int n = 0; n < numBuildings; n++) {
                BuildingDef buildingDef = new BuildingDef();
                numRooms = IsoLot.readInt(in);
                buildingDef.id = n;

                for (int x = 0; x < numRooms; x++) {
                    RoomDef rr = header.roomDefMap.get(IsoLot.readInt(in));
                    rr.building = buildingDef;
                    if (rr.isEmptyOutside()) {
                        buildingDef.emptyoutside.add(rr);
                    } else {
                        buildingDef.rooms.add(rr);
                    }
                }

                buildingDef.CalculateBounds(this.tempRooms);
                header.buildingDefList.add(buildingDef);
            }

            header.chunkTablePosition = in.getFilePointer();
            return header;
        } else {
            throw new IOException("unrecognized file format");
        }
    }

    public NewMapBinaryFile.ChunkData loadChunk(NewMapBinaryFile.Header header, int chunkX, int chunkY) throws IOException {
        NewMapBinaryFile.ChunkData var5;
        try (BufferedRandomAccessFile in = new BufferedRandomAccessFile(header.fileName, "r", 4096)) {
            var5 = this.loadChunkInner(in, header, chunkX, chunkY);
        }

        return var5;
    }

    private NewMapBinaryFile.ChunkData loadChunkInner(RandomAccessFile in, NewMapBinaryFile.Header header, int chunkX, int chunkY) throws IOException {
        NewMapBinaryFile.ChunkData chunk = new NewMapBinaryFile.ChunkData();
        chunk.header = header;
        chunk.data = new int[this.chunkDim][this.chunkDim][header.levels][];
        int index = chunkY * header.width + chunkX;
        in.seek(header.chunkTablePosition + index * 8);
        int pos = IsoLot.readInt(in);
        in.seek(pos);
        int skip = 0;
        chunk.attributes = new int[header.levels][this.chunkDim * this.chunkDim];

        for (int z = 0; z < header.levels; z++) {
            for (int x = 0; x < this.chunkDim; x++) {
                for (int y = 0; y < this.chunkDim; y++) {
                    if (skip > 0) {
                        skip--;
                        chunk.attributes[z][x + y * this.chunkDim] = 0;
                    } else {
                        int count = IsoLot.readInt(in);
                        if (count == -1) {
                            skip = IsoLot.readInt(in);
                            if (skip > 0) {
                                skip--;
                                chunk.attributes[z][x + y * this.chunkDim] = 0;
                                continue;
                            }
                        }

                        if (count > 1) {
                            chunk.attributes[z][x + y * this.chunkDim] = IsoLot.readInt(in);
                        } else {
                            chunk.attributes[z][x + y * this.chunkDim] = 0;
                        }
                    }
                }
            }
        }

        skip = 0;

        for (int z = 0; z < header.levels; z++) {
            for (int x = 0; x < this.chunkDim; x++) {
                for (int yx = 0; yx < this.chunkDim; yx++) {
                    if (skip > 0) {
                        skip--;
                        chunk.data[x][yx][z] = null;
                    } else {
                        int countx = IsoLot.readInt(in);
                        if (countx == -1) {
                            skip = IsoLot.readInt(in);
                            if (skip > 0) {
                                skip--;
                                chunk.data[x][yx][z] = null;
                                continue;
                            }
                        }

                        if (countx > 1) {
                            chunk.data[x][yx][z] = new int[countx - 1];

                            for (int n = 1; n < countx; n++) {
                                chunk.data[x][yx][z][n - 1] = IsoLot.readInt(in);
                            }
                        } else {
                            chunk.data[x][yx][z] = null;
                        }
                    }
                }
            }
        }

        return chunk;
    }

    public void setChunkInWorldArb(NewMapBinaryFile.ChunkData chunkData, int sx, int sy, int sz, int tx, int ty, int tz) {
        IsoCell cell = IsoWorld.instance.currentCell;

        try {
            this.setChunkInWorldInnerArb(chunkData, sx, sy, sz, tx, ty, tz);

            for (int x = tx; x < tx + chunkData.header.file.chunkDim; x++) {
                for (int y = ty; y < ty + chunkData.header.file.chunkDim; y++) {
                    for (int z = tz; z < tz + chunkData.header.levels; z++) {
                        IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
                        if (sq != null) {
                            sq.RecalcAllWithNeighbours(true);
                            if (sq.HasStairsBelow()) {
                                sq.removeUnderground();
                            }
                        }
                    }
                }
            }
        } catch (Exception var14) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(var14);

            for (int x = tx; x < tx + chunkData.header.file.chunkDim; x++) {
                for (int y = ty; y < ty + chunkData.header.file.chunkDim; y++) {
                    for (int zx = tz; zx < tz + chunkData.header.levels; zx++) {
                        IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, zx);
                        if (sq != null) {
                            sq.RecalcAllWithNeighbours(true);
                        }
                    }
                }
            }
        }
    }

    public void setChunkInWorld(NewMapBinaryFile.ChunkData chunkData, int sx, int sy, int sz, IsoChunk ch, int WX, int WY) {
        IsoCell cell = IsoWorld.instance.currentCell;
        WX *= 8;
        WY *= 8;

        try {
            this.setChunkInWorldInner(chunkData, sx, sy, sz, ch, WX, WY);
        } catch (Exception var13) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(var13);

            for (int x = WX + sx; x < WX + sx + 8; x++) {
                for (int y = WY + sy; y < WY + sy + 8; y++) {
                    for (int z = sz; z < sz + chunkData.header.levels; z++) {
                        ch.setSquare(x - WX, y - WY, z, null);
                        cell.setCacheGridSquare(x, y, z, null);
                    }
                }
            }
        }
    }

    private void setChunkInWorldInner(NewMapBinaryFile.ChunkData chunkData, int sx, int sy, int sz, IsoChunk ch, int WX, int WY) {
        IsoCell cell = IsoWorld.instance.currentCell;
        new Stack();
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();

        for (int x = WX + sx; x < WX + sx + 8; x++) {
            for (int y = WY + sy; y < WY + sy + 8; y++) {
                for (int z = sz; z < sz + chunkData.header.levels; z++) {
                    boolean bClearExistingObjects = false;
                    if (x < WX + 8 && y < WY + 8 && x >= WX && y >= WY && z >= 0) {
                        int[] ints = chunkData.data[x - (WX + sx)][y - (WY + sy)][z - sz];
                        if (ints != null && ints.length != 0) {
                            int s = ints.length;
                            IsoGridSquare square = ch.getGridSquare(x - WX, y - WY, z);
                            if (square == null) {
                                square = IsoGridSquare.getNew(cell, null, x, y, z);
                                square.setX(x);
                                square.setY(y);
                                square.setZ(z);
                                ch.setSquare(x - WX, y - WY, z, square);
                            }

                            for (int xx = -1; xx <= 1; xx++) {
                                for (int yy = -1; yy <= 1; yy++) {
                                    if ((xx != 0 || yy != 0) && xx + x - WX >= 0 && xx + x - WX < 8 && yy + y - WY >= 0 && yy + y - WY < 8) {
                                        IsoGridSquare square2 = ch.getGridSquare(x + xx - WX, y + yy - WY, z);
                                        if (square2 == null) {
                                            square2 = IsoGridSquare.getNew(cell, null, x + xx, y + yy, z);
                                            ch.setSquare(x + xx - WX, y + yy - WY, z, square2);
                                        }
                                    }
                                }
                            }

                            if (s > 1 && z > IsoCell.maxHeight) {
                                IsoCell.maxHeight = z;
                            }

                            RoomDef roomDef = metaGrid.getRoomAt(x, y, z);
                            long roomID = roomDef != null ? roomDef.id : -1L;
                            square.setRoomID(roomID);
                            square.ResetIsoWorldRegion();
                            roomDef = metaGrid.getEmptyOutsideAt(x, y, z);
                            if (roomDef != null) {
                                IsoRoom room = ch.getRoom(roomDef.id);
                                square.roofHideBuilding = room == null ? null : room.building;
                            }

                            for (int n = 0; n < s; n++) {
                                String tile = chunkData.header.usedTileNames.get(ints[n]);
                                if (!chunkData.header.fixed2x) {
                                    tile = IsoChunk.Fix2x(tile);
                                }

                                IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                                if (spr == null) {
                                    Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                                } else {
                                    if (n == 0
                                        && spr.getProperties().has(IsoFlagType.solidfloor)
                                        && (!spr.properties.has(IsoFlagType.hidewalls) || ints.length > 1)) {
                                        bClearExistingObjects = true;
                                    }

                                    if (bClearExistingObjects && n == 0) {
                                        square.getObjects().clear();
                                    }

                                    CellLoader.DoTileObjectCreation(spr, spr.getType(), square, cell, x, y, z, tile);
                                }
                            }

                            square.FixStackableObjects();
                        }
                    }
                }
            }
        }
    }

    private void setChunkInWorldInnerArb(NewMapBinaryFile.ChunkData chunkData, int sx, int sy, int sz, int tx, int ty, int tz) {
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();

        for (int x = tx; x < tx + this.chunkDim; x++) {
            for (int y = ty; y < ty + this.chunkDim; y++) {
                for (int z = tz; z < tz + chunkData.header.levels; z++) {
                    if (x == 12030 && y == 2601 && z == -1) {
                        boolean bClearExistingObjects = false;
                    }

                    boolean bClearExistingObjects = false;
                    int[] ints = chunkData.data[x - tx][y - ty][z - tz];
                    if (ints != null && ints.length != 0) {
                        int tileCount = ints.length;
                        IsoGridSquare square = IsoCell.getInstance().getOrCreateGridSquare(x, y, z);
                        square.EnsureSurroundNotNull();
                        if (tileCount > 1 && z > IsoCell.maxHeight) {
                            IsoCell.maxHeight = z;
                        }

                        RoomDef roomDef = metaGrid.getRoomAt(x, y, z);
                        long roomID = roomDef != null ? roomDef.id : -1L;
                        square.setRoomID(roomID);
                        if (roomID != -1L && !square.getRoom().getBuilding().rooms.contains(square.getRoom())) {
                            square.getRoom().getBuilding().rooms.add(square.getRoom());
                        }

                        square.ResetIsoWorldRegion();

                        for (int n = 0; n < tileCount; n++) {
                            String tile = chunkData.header.usedTileNames.get(ints[n]);
                            if (!chunkData.header.fixed2x) {
                                tile = IsoChunk.Fix2x(tile);
                            }

                            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                            if (spr == null) {
                                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            } else {
                                if (n == 0) {
                                    bClearExistingObjects = true;
                                }

                                if (bClearExistingObjects && n == 0) {
                                    int numObjs = square.getObjects().size();

                                    for (int i = numObjs - 1; i >= 0; i--) {
                                        square.getObjects().get(i).removeFromWorld();
                                        square.getObjects().get(i).removeFromSquare();
                                    }
                                }

                                if (tile.contains("lighting")) {
                                    boolean var26 = false;
                                }

                                CellLoader.DoTileObjectCreation(spr, spr.getType(), square, cell, x, y, z, tile);
                            }
                        }

                        square.FixStackableObjects();
                    }
                }
            }
        }
    }

    private void addToIsoChunk(IsoChunk chunk, NewMapBinaryFile.ChunkData chunkData, int tx, int ty, int tz, boolean isBasementAccess) {
        IsoCell cell = IsoWorld.instance.getCell();
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int CPW = 8;
        int x1 = Math.max(chunk.wx * 8, tx);
        int y1 = Math.max(chunk.wy * 8, ty);
        int x2 = Math.min(chunk.wx * 8 + 8, tx + this.chunkDim);
        int y2 = Math.min(chunk.wy * 8 + 8, ty + this.chunkDim);

        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = tz; z < tz + chunkData.header.levels; z++) {
                    int[] ints = chunkData.data[x - tx][y - ty][z - tz];
                    if (ints != null && ints.length != 0) {
                        int tileCount = ints.length;
                        IsoGridSquare square = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, z);
                        if (square == null) {
                            square = IsoGridSquare.getNew(cell, null, x, y, z);
                            square.setX(x);
                            square.setY(y);
                            square.setZ(z);
                            chunk.setSquare(x - chunk.wx * 8, y - chunk.wy * 8, z, square);
                        }

                        RoomDef roomDef = metaGrid.getRoomAt(x, y, z);
                        long roomID = roomDef != null ? roomDef.id : -1L;
                        square.setRoomID(roomID);
                        int attributes = chunkData.attributes[z - tz][x - tx + (y - ty) * this.chunkDim];
                        boolean bKeepExistingFloors = (attributes & 1) != 0;
                        boolean bKeepExistingWalls = (attributes & 2) != 0;
                        boolean bKeepExistingOther = (attributes & 4) != 0;
                        if (!bKeepExistingFloors && !bKeepExistingWalls && !bKeepExistingOther) {
                            square.getObjects().clear();
                            square.getSpecialObjects().clear();
                        } else {
                            for (int i = square.getObjects().size() - 1; i >= 0; i--) {
                                IsoObject object = square.getObjects().get(i);
                                boolean bKeep = false;
                                boolean isFloor = object.sprite != null && object.sprite.solidfloor;
                                boolean isWall = object.sprite != null
                                    && (
                                        object.sprite.getProperties().has(IsoFlagType.WallW)
                                            || object.sprite.getProperties().has(IsoFlagType.WallN)
                                            || object.sprite.getProperties().has(IsoFlagType.WallNW)
                                            || object.isWallSE()
                                    );
                                boolean isOther = !isFloor && !isWall;
                                if (bKeepExistingFloors && isFloor) {
                                    bKeep = true;
                                }

                                if (bKeepExistingWalls && isWall) {
                                    bKeep = true;
                                }

                                if (bKeepExistingOther && isOther) {
                                    bKeep = true;
                                }

                                if (!bKeep) {
                                    square.getObjects().remove(i);
                                    square.getSpecialObjects().remove(object);
                                }
                            }
                        }

                        for (int n = 0; n < tileCount; n++) {
                            String tile = chunkData.header.usedTileNames.get(ints[n]);
                            if (!chunkData.header.fixed2x) {
                                tile = IsoChunk.Fix2x(tile);
                            }

                            IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                            if (spr == null) {
                                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            } else {
                                CellLoader.DoTileObjectCreation(spr, spr.getType(), square, cell, x, y, z, tile);
                            }
                        }

                        square.FixStackableObjects();
                    }
                }

                IsoGridSquare squarex = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, tz + chunkData.header.levels);
                this.removeFloorsAboveStairs(squarex);
            }
        }
    }

    void removeFloorsAboveStairs(IsoGridSquare square) {
        if (square != null) {
            int CPW = 8;
            int cx = square.chunk.wx * 8;
            int cy = square.chunk.wy * 8;
            IsoGridSquare below = square.chunk.getGridSquare(square.x - cx, square.y - cy, square.z - 1);
            if (below != null) {
                below.RecalcProperties();
                if (below.HasStairs() || below.hasSlopedSurface()) {
                    IsoObject[] objects = square.getObjects().getElements();
                    int numObjects = square.getObjects().size();

                    for (int i = 0; i < numObjects; i++) {
                        IsoObject isoObject = objects[i];
                        if (isoObject.isFloor()) {
                            square.getObjects().remove(isoObject);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static final class ChunkData {
        NewMapBinaryFile.Header header;
        int[][][][] data;
        int[][] attributes;
    }

    public static final class Header {
        NewMapBinaryFile file;
        public int width;
        public int height;
        public int levels;
        public int version;
        public final TLongObjectHashMap<RoomDef> roomDefMap = new TLongObjectHashMap<>();
        public final ArrayList<RoomDef> roomDefList = new ArrayList<>();
        public final ArrayList<BuildingDef> buildingDefList = new ArrayList<>();
        public boolean fixed2x = true;
        protected final ArrayList<String> usedTileNames = new ArrayList<>();
        String fileName;
        private long chunkTablePosition;

        public void Dispose() {
            for (int i = 0; i < this.buildingDefList.size(); i++) {
                BuildingDef buildingDef = this.buildingDefList.get(i);
                buildingDef.Dispose();
            }

            this.roomDefMap.clear();
            this.roomDefList.clear();
            this.buildingDefList.clear();
            this.usedTileNames.clear();
        }
    }
}
