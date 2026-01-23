// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pot;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import zombie.iso.BuildingDef;
import zombie.iso.BuildingID;
import zombie.iso.IsoLot;
import zombie.iso.LotHeader;
import zombie.iso.MetaObject;
import zombie.iso.RoomDef;
import zombie.iso.RoomID;
import zombie.iso.SliceY;
import zombie.util.BufferedRandomAccessFile;
import zombie.util.SharedStrings;

public final class POTLotHeader {
    private static final SharedStrings sharedStrings = new SharedStrings();
    private static final ArrayList<RoomDef> tempRooms = new ArrayList<>();
    public final boolean pot;
    public final int chunkDim;
    public final int chunksPerCell;
    public final int cellDim;
    public final int x;
    public final int y;
    public int width;
    public int height;
    public int minLevel = -32;
    public int maxLevel = 31;
    public int minLevelNotEmpty = 1000;
    public int maxLevelNotEmpty = -1000;
    public int version;
    public final HashMap<Long, RoomDef> rooms = new HashMap<>();
    public final ArrayList<RoomDef> roomList = new ArrayList<>();
    public final ArrayList<BuildingDef> buildings = new ArrayList<>();
    public final ArrayList<String> tilesUsed = new ArrayList<>();
    public final TObjectIntHashMap<String> indexToTile = new TObjectIntHashMap<>(10, 0.5F, -1);
    public final byte[] zombieDensity;

    POTLotHeader(int x, int y, boolean pot) {
        this.chunkDim = pot ? 8 : 10;
        this.chunksPerCell = pot ? 32 : 30;
        this.cellDim = pot ? 256 : 300;
        this.pot = pot;
        this.x = x;
        this.y = y;
        this.width = this.chunkDim;
        this.height = this.chunkDim;
        this.zombieDensity = new byte[this.chunksPerCell * this.chunksPerCell];
    }

    void clear() {
        for (BuildingDef def : this.buildings) {
            def.Dispose();
        }

        this.buildings.clear();
        this.rooms.clear();
        this.roomList.clear();
        this.tilesUsed.clear();
        this.indexToTile.clear();
    }

    void load(File file) {
        try (BufferedRandomAccessFile in = new BufferedRandomAccessFile(file, "r", 4096)) {
            byte[] magic = new byte[4];
            in.read(magic, 0, 4);
            boolean bHasMagic = Arrays.equals(magic, LotHeader.LOTHEADER_MAGIC);
            if (!bHasMagic) {
                in.seek(0L);
            }

            this.version = IsoLot.readInt(in);
            if (this.version >= 0 && this.version <= 1) {
                int tilecount = IsoLot.readInt(in);

                for (int n = 0; n < tilecount; n++) {
                    String str = IsoLot.readString(in);
                    str = sharedStrings.get(str.trim());
                    this.tilesUsed.add(str);
                    this.indexToTile.put(str, n);
                }

                if (this.version == 0) {
                    in.read();
                } else {
                    boolean var10000 = false;
                }

                this.width = IsoLot.readInt(in);
                this.height = IsoLot.readInt(in);
                if (this.version == 0) {
                    this.minLevel = 0;
                    this.maxLevel = IsoLot.readInt(in);
                } else {
                    this.minLevel = IsoLot.readInt(in);
                    this.maxLevel = IsoLot.readInt(in);
                }

                this.minLevelNotEmpty = this.minLevel;
                this.maxLevelNotEmpty = this.maxLevel;
                int numRooms = IsoLot.readInt(in);

                for (int n = 0; n < numRooms; n++) {
                    String str = IsoLot.readString(in);
                    long roomID = RoomID.makeID(this.x, this.y, n);
                    RoomDef roomDef = new RoomDef(roomID, sharedStrings.get(str));
                    roomDef.level = IsoLot.readInt(in);
                    int numRects = IsoLot.readInt(in);

                    for (int rc = 0; rc < numRects; rc++) {
                        int x = IsoLot.readInt(in);
                        int y = IsoLot.readInt(in);
                        int w = IsoLot.readInt(in);
                        int h = IsoLot.readInt(in);
                        RoomDef.RoomRect rect = new RoomDef.RoomRect(x + this.x * this.cellDim, y + this.y * this.cellDim, w, h);
                        roomDef.rects.add(rect);
                    }

                    roomDef.CalculateBounds();
                    this.rooms.put(roomDef.id, roomDef);
                    this.roomList.add(roomDef);
                    int numObjects = IsoLot.readInt(in);

                    for (int m = 0; m < numObjects; m++) {
                        int e = IsoLot.readInt(in);
                        int x = IsoLot.readInt(in);
                        int y = IsoLot.readInt(in);
                        roomDef.objects.add(new MetaObject(e, x + this.x * this.cellDim - roomDef.x, y + this.y * this.cellDim - roomDef.y, roomDef));
                    }
                }

                int numBuildings = IsoLot.readInt(in);

                for (int n = 0; n < numBuildings; n++) {
                    BuildingDef buildingDef = new BuildingDef();
                    buildingDef.id = BuildingID.makeID(this.x, this.y, n);
                    int numRooms2 = IsoLot.readInt(in);

                    for (int x = 0; x < numRooms2; x++) {
                        int roomIndex = IsoLot.readInt(in);
                        long roomID = RoomID.makeID(this.x, this.y, roomIndex);
                        RoomDef roomDef = this.rooms.get(roomID);
                        roomDef.building = buildingDef;
                        buildingDef.rooms.add(roomDef);
                    }

                    buildingDef.CalculateBounds(tempRooms);
                    this.buildings.add(buildingDef);
                }

                for (int x = 0; x < this.chunksPerCell; x++) {
                    for (int y = 0; y < this.chunksPerCell; y++) {
                        this.zombieDensity[x + y * this.chunksPerCell] = (byte)in.read();
                    }
                }
            } else {
                throw new IOException("Unsupported version " + this.version);
            }
        } catch (Exception var22) {
            var22.printStackTrace();
        }
    }

    void save(String fileName) throws IOException {
        ByteBuffer bb = SliceY.SliceBuffer;
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.clear();
        bb.put(LotHeader.LOTHEADER_MAGIC);
        bb.putInt(1);
        bb.putInt(this.tilesUsed.size());

        for (int i = 0; i < this.tilesUsed.size(); i++) {
            this.writeString(bb, this.tilesUsed.get(i));
        }

        bb.putInt(this.width);
        bb.putInt(this.height);
        bb.putInt(this.minLevelNotEmpty);
        bb.putInt(this.maxLevelNotEmpty);
        bb.putInt(this.roomList.size());

        for (RoomDef roomDef : this.roomList) {
            this.writeString(bb, roomDef.name);
            bb.putInt(roomDef.level);
            bb.putInt(roomDef.rects.size());

            for (RoomDef.RoomRect roomRect : roomDef.rects) {
                bb.putInt(roomRect.x - this.getMinSquareX());
                bb.putInt(roomRect.y - this.getMinSquareY());
                bb.putInt(roomRect.w);
                bb.putInt(roomRect.h);
            }

            bb.putInt(roomDef.objects.size());

            for (MetaObject metaObject : roomDef.objects) {
                bb.putInt(metaObject.getType());
                bb.putInt(metaObject.getX());
                bb.putInt(metaObject.getY());
            }
        }

        bb.putInt(this.buildings.size());

        for (BuildingDef buildingDef : this.buildings) {
            bb.putInt(buildingDef.rooms.size());

            for (RoomDef roomDef : buildingDef.rooms) {
                assert roomDef.id == (long)this.roomList.indexOf(roomDef);

                bb.putInt(RoomID.getIndex(roomDef.id));
            }
        }

        for (int x = 0; x < this.chunksPerCell; x++) {
            for (int y = 0; y < this.chunksPerCell; y++) {
                bb.put(this.zombieDensity[x + y * this.chunksPerCell]);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(bb.array(), 0, bb.position());
        }
    }

    void writeString(ByteBuffer bb, String str) {
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        bb.put(strBytes);
        bb.put((byte)10);
    }

    int getMinSquareX() {
        return this.x * this.cellDim;
    }

    int getMinSquareY() {
        return this.y * this.cellDim;
    }

    int getMaxSquareX() {
        return (this.x + 1) * this.cellDim - 1;
    }

    int getMaxSquareY() {
        return (this.y + 1) * this.cellDim - 1;
    }

    boolean containsSquare(int squareX, int squareY) {
        return squareX >= this.getMinSquareX() && squareX <= this.getMaxSquareX() && squareY >= this.getMinSquareY() && squareY <= this.getMaxSquareY();
    }

    void addBuilding(BuildingDef buildingDef) {
        BuildingDef buildingDefNew = new BuildingDef();
        buildingDefNew.id = this.buildings.size();

        for (RoomDef roomDef : buildingDef.rooms) {
            RoomDef roomDefNew = new RoomDef(this.roomList.size(), roomDef.name);
            roomDefNew.id = this.roomList.size();
            roomDefNew.level = roomDef.level;
            roomDefNew.building = buildingDefNew;
            roomDefNew.rects.addAll(roomDef.rects);
            roomDefNew.objects.addAll(roomDef.objects);
            roomDefNew.CalculateBounds();
            buildingDefNew.rooms.add(roomDefNew);
            this.rooms.put(roomDefNew.id, roomDefNew);
            this.roomList.add(roomDefNew);
        }

        buildingDefNew.CalculateBounds(tempRooms);
        this.buildings.add(buildingDefNew);
    }

    byte getZombieDensityForSquare(int squareX, int squareY) {
        if (!this.containsSquare(squareX, squareY)) {
            return 0;
        } else {
            int x = squareX - this.getMinSquareX();
            int y = squareY - this.getMinSquareY();
            return this.zombieDensity[x / this.chunkDim + y / this.chunkDim * this.chunksPerCell];
        }
    }

    void setZombieDensity(byte[] zombieDensityPerSquare) {
        for (int y = 0; y < this.chunksPerCell; y++) {
            for (int x = 0; x < this.chunksPerCell; x++) {
                int density = 0;

                for (int n = 0; n < this.chunkDim * this.chunkDim; n++) {
                    density += zombieDensityPerSquare[x * this.chunkDim
                        + y * this.chunkDim * this.cellDim
                        + n % this.chunkDim
                        + n / this.chunkDim * this.cellDim];
                }

                this.zombieDensity[x + y * this.chunksPerCell] = (byte)(density / (this.chunkDim * this.chunkDim));
            }
        }
    }

    int getTileIndex(String tileName) {
        tileName = sharedStrings.get(tileName);
        int index = this.indexToTile.get(tileName);
        if (index == this.indexToTile.getNoEntryValue()) {
            index = this.tilesUsed.size();
            this.indexToTile.put(tileName, this.tilesUsed.size());
            this.tilesUsed.add(tileName);
        }

        return index;
    }
}
