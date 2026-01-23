// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.list.array.TShortArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.joml.Vector2f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class BuildingDef {
    static final ArrayList<IsoGridSquare> squareChoices = new ArrayList<>();
    public final ArrayList<RoomDef> emptyoutside = new ArrayList<>();
    public KahluaTable table;
    public boolean seen;
    public boolean hasBeenVisited;
    public String stash;
    public int lootRespawnHour = -1;
    public TShortArrayList overlappedChunks;
    public boolean alarmed;
    public int alarmDecay = 10000000;
    public int x = 10000000;
    public int y = 10000000;
    public int x2 = -10000000;
    public int y2 = -10000000;
    public final ArrayList<RoomDef> rooms = new ArrayList<>();
    public Zone zone;
    public int food;
    public ArrayList<InventoryItem> items = new ArrayList<>();
    public HashSet<String> itemTypes = new HashSet<>();
    public long id;
    private int keySpawned;
    private int keyId = -1;
    public long metaId;
    private int minLevel = 100;
    private int maxLevel = -100;
    private final HashMap<Integer, Long> roofRoomId = new HashMap<>();
    private boolean userDefined;
    public int collapseRectX = -1;
    public int collapseRectY = -1;
    public int collapseRectX2 = -1;
    public int collapseRectY2 = -1;

    public BuildingDef() {
        this.table = LuaManager.platform.newTable();
        this.setKeyId(Rand.Next(100000000));
    }

    public BuildingDef(boolean userDefined) {
        this();
        this.userDefined = userDefined;
    }

    public int getMinLevel() {
        if (this.minLevel != 100) {
            return this.minLevel;
        } else {
            for (int i = 0; i < this.rooms.size(); i++) {
                this.minLevel = Math.min(this.rooms.get(i).level, this.minLevel);
            }

            return this.minLevel;
        }
    }

    public int getMaxLevel() {
        if (this.maxLevel != -100) {
            return this.maxLevel;
        } else {
            for (int x = 0; x < this.rooms.size(); x++) {
                this.maxLevel = Math.max(this.rooms.get(x).level, this.maxLevel);
            }

            return this.maxLevel;
        }
    }

    public KahluaTable getTable() {
        return this.table;
    }

    public ArrayList<RoomDef> getRooms() {
        return this.rooms;
    }

    public ArrayList<RoomDef> getEmptyOutside() {
        return this.emptyoutside;
    }

    public RoomDef getRoom(String roomName) {
        return this.getRoom(roomName, false);
    }

    public RoomDef getRoom(String roomName, boolean noKids) {
        for (int i = 0; i < this.rooms.size(); i++) {
            RoomDef room = this.rooms.get(i);
            boolean nope = noKids && room.isKidsRoom();
            if (!nope && room.getName().equalsIgnoreCase(roomName)) {
                return room;
            }
        }

        return null;
    }

    public boolean isAllExplored() {
        for (int n = 0; n < this.rooms.size(); n++) {
            if (!this.rooms.get(n).explored) {
                return false;
            }
        }

        return true;
    }

    public void setAllExplored(boolean b) {
        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef r = this.rooms.get(n);
            r.setExplored(b);
        }
    }

    public int getRoomsNumber() {
        return this.rooms.size();
    }

    public int getArea() {
        int squares = 0;

        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef r = this.rooms.get(n);
            squares += r.getArea();
        }

        return squares;
    }

    public RoomDef getFirstRoom() {
        return this.rooms.get(0);
    }

    public void setUserDefined(boolean b) {
        this.userDefined = b;
    }

    public int getCellX() {
        return PZMath.fastfloor(this.getX() / 256.0F);
    }

    public int getCellY() {
        return PZMath.fastfloor(this.getY() / 256.0F);
    }

    public int getCellX2() {
        return PZMath.fastfloor((this.getX2() - 1) / 256.0F);
    }

    public int getCellY2() {
        return PZMath.fastfloor((this.getY2() - 1) / 256.0F);
    }

    public int getChunkX() {
        return PZMath.fastfloor(this.x / 8.0F);
    }

    public int getChunkY() {
        return PZMath.fastfloor(this.y / 8.0F);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getX2() {
        return this.x2;
    }

    public int getY2() {
        return this.y2;
    }

    public int getW() {
        return this.x2 - this.x;
    }

    public int getH() {
        return this.y2 - this.y;
    }

    public long getID() {
        return this.id;
    }

    public String getIDString() {
        return String.valueOf(this.getID());
    }

    public void refreshSquares() {
        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef r = this.rooms.get(n);
            r.refreshSquares();
        }
    }

    public void CalculateBounds(ArrayList<RoomDef> tempRooms) {
        this.x = Integer.MAX_VALUE;
        this.y = Integer.MAX_VALUE;
        this.x2 = Integer.MIN_VALUE;
        this.y2 = Integer.MIN_VALUE;
        this.rooms.trimToSize();
        this.emptyoutside.trimToSize();

        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef r = this.rooms.get(n);
            r.rects.trimToSize();

            for (int nn = 0; nn < r.rects.size(); nn++) {
                RoomDef.RoomRect rect = r.rects.get(nn);
                if (rect.x < this.x) {
                    this.x = rect.x;
                }

                if (rect.y < this.y) {
                    this.y = rect.y;
                }

                if (rect.x + rect.w > this.x2) {
                    this.x2 = rect.x + rect.w;
                }

                if (rect.y + rect.h > this.y2) {
                    this.y2 = rect.y + rect.h;
                }
            }
        }

        for (int n = 0; n < this.emptyoutside.size(); n++) {
            RoomDef r = this.emptyoutside.get(n);
            r.rects.trimToSize();

            for (int nn = 0; nn < r.rects.size(); nn++) {
                RoomDef.RoomRect rectx = r.rects.get(nn);
                if (rectx.x < this.x) {
                    this.x = rectx.x;
                }

                if (rectx.y < this.y) {
                    this.y = rectx.y;
                }

                if (rectx.x + rectx.w > this.x2) {
                    this.x2 = rectx.x + rectx.w;
                }

                if (rectx.y + rectx.h > this.y2) {
                    this.y2 = rectx.y + rectx.h;
                }
            }
        }

        int CPW = 8;
        int minX = PZMath.fastfloor(this.x / 8.0F);
        int minY = PZMath.fastfloor(this.y / 8.0F);
        int maxX = PZMath.fastfloor(this.x2 / 8.0F);
        int maxY = PZMath.fastfloor(this.y2 / 8.0F);
        if (this.overlappedChunks == null) {
            this.overlappedChunks = new TShortArrayList((maxX - minX + 1) * (maxY - minY + 1) * 2);
        }

        this.overlappedChunks.resetQuick();
        tempRooms.clear();
        PZArrayUtil.addAll(tempRooms, this.rooms);
        PZArrayUtil.addAll(tempRooms, this.emptyoutside);

        for (int n = 0; n < tempRooms.size(); n++) {
            RoomDef r = tempRooms.get(n);

            for (int nn = 0; nn < r.rects.size(); nn++) {
                RoomDef.RoomRect rectxx = r.rects.get(nn);
                minX = (rectxx.x - 1) / 8;
                minY = (rectxx.y - 1) / 8;
                maxX = (rectxx.x + rectxx.w + 0) / 8;
                maxY = (rectxx.y + rectxx.h + 0) / 8;

                for (int wy = minY; wy <= maxY; wy++) {
                    for (int wx = minX; wx <= maxX; wx++) {
                        if (!this.overlapsChunk(wx, wy)) {
                            this.overlappedChunks.add((short)wx);
                            this.overlappedChunks.add((short)wy);
                        }
                    }
                }
            }
        }

        this.overlappedChunks.trimToSize();
    }

    public long calculateMetaID(int cellX, int cellY) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        ArrayList<RoomDef> roomDefs = this.rooms.isEmpty() ? this.emptyoutside : this.rooms;

        for (int i = 0; i < roomDefs.size(); i++) {
            RoomDef roomDef = roomDefs.get(i);
            if (roomDef.level <= minZ) {
                if (roomDef.level < minZ) {
                    minX = Integer.MAX_VALUE;
                    minY = Integer.MAX_VALUE;
                }

                minZ = roomDef.level;

                for (int j = 0; j < roomDef.rects.size(); j++) {
                    RoomDef.RoomRect rect = roomDef.rects.get(j);
                    if (rect.x <= minX && rect.y < minY) {
                        minX = rect.x;
                        minY = rect.y;
                    }
                }
            }
        }

        minX -= cellX * 256;
        minY -= cellY * 256;
        return (long)minZ << 32 | (long)minY << 16 | minX;
    }

    public void recalculate() {
        this.food = 0;
        this.items.clear();
        this.itemTypes.clear();

        for (int n = 0; n < this.rooms.size(); n++) {
            IsoRoom room = this.rooms.get(n).getIsoRoom();

            for (int m = 0; m < room.containers.size(); m++) {
                ItemContainer c = room.containers.get(m);

                for (int l = 0; l < c.items.size(); l++) {
                    InventoryItem i = c.items.get(l);
                    this.items.add(i);
                    this.itemTypes.add(i.getFullType());
                    if (i instanceof Food) {
                        this.food++;
                    }
                }
            }
        }
    }

    public boolean overlapsChunk(int wx, int wy) {
        for (int i = 0; i < this.overlappedChunks.size(); i += 2) {
            if (wx == this.overlappedChunks.get(i) && wy == this.overlappedChunks.get(i + 1)) {
                return true;
            }
        }

        return false;
    }

    public IsoGridSquare getFreeSquareInRoom() {
        squareChoices.clear();

        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef def = this.rooms.get(n);

            for (int nn = 0; nn < def.rects.size(); nn++) {
                RoomDef.RoomRect rect = def.rects.get(nn);

                for (int x = rect.getX(); x < rect.getX2(); x++) {
                    for (int y = rect.getY(); y < rect.getY2(); y++) {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, def.getZ());
                        if (sq != null && sq.isFree(false)) {
                            squareChoices.add(sq);
                        }
                    }
                }
            }
        }

        if (!squareChoices.isEmpty()) {
            IsoGridSquare square = squareChoices.get(Rand.Next(squareChoices.size()));
            squareChoices.clear();
            return square;
        } else {
            return null;
        }
    }

    public boolean containsRoom(String name) {
        for (int n = 0; n < this.rooms.size(); n++) {
            RoomDef def = this.rooms.get(n);
            if (def.name.equals(name)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFullyStreamedIn() {
        for (int i = 0; i < this.overlappedChunks.size(); i += 2) {
            int wx = this.overlappedChunks.get(i);
            int wy = this.overlappedChunks.get(i + 1);
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
            if (chunk == null) {
                return false;
            }
        }

        return true;
    }

    public boolean isAnyChunkNewlyLoaded() {
        for (int i = 0; i < this.overlappedChunks.size(); i += 2) {
            int wx = this.overlappedChunks.get(i);
            int wy = this.overlappedChunks.get(i + 1);
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
            if (chunk == null) {
                return false;
            }

            if (chunk.isNewChunk()) {
                return true;
            }
        }

        return false;
    }

    public Zone getZone() {
        return this.zone;
    }

    public int getKeyId() {
        return this.keyId;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public int getKeySpawned() {
        return this.keySpawned;
    }

    public void setKeySpawned(int keySpawned) {
        this.keySpawned = keySpawned;
    }

    public boolean isHasBeenVisited() {
        return this.hasBeenVisited;
    }

    public void setHasBeenVisited(boolean hasBeenVisited) {
        if (hasBeenVisited && !this.hasBeenVisited) {
            StashSystem.visitedBuilding(this);
        }

        this.hasBeenVisited = hasBeenVisited;
    }

    public boolean isAlarmed() {
        return this.alarmed;
    }

    public void setAlarmed(boolean alarm) {
        this.alarmed = alarm;
    }

    public RoomDef getRandomRoom() {
        return this.getRandomRoom(0, false);
    }

    public RoomDef getRandomRoom(int minArea) {
        return this.getRandomRoom(minArea, false);
    }

    public RoomDef getRandomRoom(int minArea, boolean noKids) {
        RoomDef room = this.getRooms().get(Rand.Next(0, this.getRooms().size()));
        boolean nope = noKids && room.isKidsRoom();
        if (!nope && minArea > 0 && room.area >= minArea) {
            return room;
        } else {
            int count = 0;

            while (count <= 20) {
                count++;
                room = this.getRooms().get(Rand.Next(0, this.getRooms().size()));
                nope = noKids && room.isKidsRoom();
                if (!nope && room.area >= minArea) {
                    return room;
                }
            }

            return room;
        }
    }

    public float getClosestPoint(float x, float y, Vector2f closestXY) {
        float closestDist = Float.MAX_VALUE;
        Vector2f closestXY2 = BaseVehicle.allocVector2f();
        float insideDistSq = 0.5F;

        for (int i = 0; i < this.rooms.size(); i++) {
            RoomDef roomDef = this.rooms.get(i);
            float dist = roomDef.getClosestPoint(x, y, closestXY2);
            if (dist < closestDist) {
                closestDist = dist;
                closestXY.set(closestXY2);
            }

            if (dist <= 0.5F) {
                break;
            }
        }

        BaseVehicle.releaseVector2f(closestXY2);
        return closestDist;
    }

    public void Dispose() {
        for (RoomDef roomDef : this.rooms) {
            roomDef.Dispose();
        }

        this.emptyoutside.clear();
        this.rooms.clear();
        this.resetMinMaxLevel();
    }

    public boolean containsXYZ(int x, int y, int z) {
        if (x >= this.x && y >= this.y && x < this.x + this.getW() && y < this.y + this.getH()) {
            for (int i = 0; i < this.rooms.size(); i++) {
                RoomDef roomDef = this.rooms.get(i);
                if (roomDef.level == z && roomDef.intersects(x, y, 1, 1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addRoomToCollapseRect(RoomDef room) {
    }

    public void calculateCollapseRect() {
        if (this.collapseRectX2 == -1) {
            int level = this.getMaxLevel() + 1;
            float minX = Math.min(this.x, this.x2);
            float maxX = Math.max(this.x, this.x2);
            float minY = Math.min(this.y, this.y2);
            float maxY = Math.max(this.y, this.y2);
            int roomXFloor = this.x - level * 3;
            int roomYFloor = this.y - level * 3;
            int roomX2Floor = this.x2 - level * 3;
            int roomY2Floor = this.y2 - level * 3;
            minX = Math.min(minX, (float)roomXFloor);
            maxX = Math.max(maxX, (float)roomX2Floor);
            minY = Math.min(minY, (float)roomYFloor);
            maxY = Math.max(maxY, (float)roomY2Floor);
            if (this.collapseRectX == -1) {
                this.collapseRectX = (int)minX;
                this.collapseRectY = (int)minY;
                this.collapseRectX2 = (int)maxX - 1;
                this.collapseRectY2 = (int)maxY - 1;
            } else {
                this.collapseRectX = (int)Math.min(minX, (float)this.collapseRectX);
                this.collapseRectY = (int)Math.min(minY, (float)this.collapseRectY);
                this.collapseRectX2 = (int)Math.max(maxX, (float)this.collapseRectX) - 1;
                this.collapseRectY2 = (int)Math.max(maxY, (float)this.collapseRectY) - 1;
            }
        }
    }

    public void setInvalidateCacheForAllChunks(int playerIndex, long dirtyFlags) {
        IsoChunkMap chunkMap = IsoCell.getInstance().getChunkMap(playerIndex);

        for (int i = 0; i < this.overlappedChunks.size(); i += 2) {
            int wx = this.overlappedChunks.get(i);
            int wy = this.overlappedChunks.get(i + 1);
            IsoChunk chunk = chunkMap.getChunk(wx - chunkMap.getWorldXMin(), wy - chunkMap.getWorldYMin());
            if (chunk != null) {
                chunk.getRenderLevels(playerIndex).invalidateAll(dirtyFlags);
            }
        }
    }

    public void invalidateOverlappedChunkLevelsAbove(int playerIndex, int minLevel, long dirtyFlags) {
        IsoChunkMap chunkMap = IsoCell.getInstance().getChunkMap(playerIndex);

        for (int i = 0; i < this.overlappedChunks.size(); i += 2) {
            int wx = this.overlappedChunks.get(i);
            int wy = this.overlappedChunks.get(i + 1);
            IsoChunk chunk = chunkMap.getChunk(wx - chunkMap.getWorldXMin(), wy - chunkMap.getWorldYMin());
            if (chunk != null) {
                FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);

                for (int z = PZMath.max(minLevel, chunk.minLevel); z <= chunk.maxLevel; z++) {
                    renderLevels.invalidateLevel(z, dirtyFlags);
                }
            }
        }
    }

    public boolean intersects(int x, int y, int w, int h, int z) {
        for (RoomDef roomDef : this.rooms) {
            if (roomDef.level == z && roomDef.intersects(x, y, w, h)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAdjacent(int x, int y, int w, int h, int z) {
        for (RoomDef roomDef : this.rooms) {
            if (roomDef.level == z && roomDef.isAdjacent(x, y, w, h)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAdjacent(BuildingDef other) {
        return this.isAdjacent(other, false);
    }

    public boolean isAdjacent(BuildingDef other, boolean bIgnoreZ) {
        for (int i = 0; i < this.rooms.size(); i++) {
            RoomDef roomDef = this.rooms.get(i);

            for (int j = 0; j < other.rooms.size(); j++) {
                RoomDef roomDef1 = other.rooms.get(j);
                if ((bIgnoreZ || roomDef.level == roomDef1.level) && roomDef.isAdjacent(roomDef1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean overlaps(BuildingDef other, boolean bIgnoreZ) {
        for (int i = 0; i < this.rooms.size(); i++) {
            RoomDef roomDef = this.rooms.get(i);

            for (int j = 0; j < other.rooms.size(); j++) {
                RoomDef roomDef1 = other.rooms.get(j);
                if ((bIgnoreZ || roomDef.level == roomDef1.level) && roomDef.overlaps(roomDef1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addRoomsOf(BuildingDef sourceDef, ArrayList<RoomDef> tempRooms) {
        for (int i = 0; i < sourceDef.rooms.size(); i++) {
            RoomDef roomDef = sourceDef.rooms.get(i);
            roomDef.building = this;
            this.rooms.add(roomDef);
        }

        for (int i = 0; i < sourceDef.emptyoutside.size(); i++) {
            RoomDef roomDef = sourceDef.emptyoutside.get(i);
            roomDef.building = this;
            this.emptyoutside.add(roomDef);
        }

        this.resetMinMaxLevel();
        this.CalculateBounds(tempRooms);
        sourceDef.resetMinMaxLevel();
    }

    public long getRoofRoomID(int level) {
        Long roomID = this.roofRoomId.get(level);
        if (roomID == null) {
            int cellX = this.x / 256;
            int cellY = this.y / 256;
            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(cellX, cellY);
            if (metaCell == null) {
                this.roofRoomId.put(level, -1L);
                return -1L;
            }

            int buildingIndex = metaCell.buildings.indexOf(this);
            if (buildingIndex == -1) {
                this.roofRoomId.put(level, -1L);
                return -1L;
            }

            int roomIndex = metaCell.rooms.size() + buildingIndex * 64 + 32 + level;
            roomID = RoomID.makeID(cellX, cellY, roomIndex);
            this.roofRoomId.put(level, roomID);
        }

        return roomID;
    }

    public boolean isEntirelyEmptyOutside() {
        return this.rooms.isEmpty() && !this.emptyoutside.isEmpty();
    }

    public boolean isShop() {
        for (int i = 0; i < this.rooms.size(); i++) {
            if (this.rooms.get(i).isShop()) {
                return true;
            }
        }

        return false;
    }

    public boolean isResidential() {
        return this.containsRoom("bedroom");
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }

    public boolean isBasement() {
        return !this.getRooms().isEmpty() && this.getRooms().get(0).level < 0;
    }

    public void resetMinMaxLevel() {
        this.minLevel = 100;
        this.maxLevel = -100;
    }
}
