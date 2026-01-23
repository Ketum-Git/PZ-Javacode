// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.buildingRooms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.BuildingDef;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SliceY;

public final class PlayerRoomsFile {
    private static final int VERSION1 = 1;
    private static final int VERSION = 1;
    private static final byte[] FILE_MAGIC = new byte[]{80, 82, 66, 71};
    private final ArrayList<BuildingDef> buildings = new ArrayList<>();
    private final ArrayList<RemovedBuilding> removedBuildings = new ArrayList<>();

    public void save() {
        if (!Core.getInstance().isNoSave()) {
            File file = ZomboidFileSystem.instance.getFileInCurrentSave("player_buildings.bin");

            try {
                try (
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    synchronized (SliceY.SliceBufferLock) {
                        SliceY.SliceBuffer.clear();
                        this.save(SliceY.SliceBuffer);
                        bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                    }
                }
            } catch (IOException var11) {
                throw new RuntimeException(var11);
            }
        }
    }

    void save(ByteBuffer bb) throws IOException {
        ArrayList<BuildingDef> buildings = IsoWorld.instance.getMetaGrid().buildings;
        ArrayList<BuildingDef> playerBuildings = new ArrayList<>();

        for (BuildingDef buildingDef : buildings) {
            if (buildingDef.isUserDefined()) {
                playerBuildings.add(buildingDef);
            }
        }

        bb.put(FILE_MAGIC);
        bb.putInt(1);
        ArrayList<RemovedBuilding> removedBuildings = IsoWorld.instance.getMetaGrid().getRemovedBuildings();
        bb.putShort((short)removedBuildings.size());

        for (int i = 0; i < removedBuildings.size(); i++) {
            RemovedBuilding removedBuilding = removedBuildings.get(i);
            bb.putInt(removedBuilding.x);
            bb.putInt(removedBuilding.y);
            bb.putInt(removedBuilding.z);
        }

        bb.putShort((short)playerBuildings.size());

        for (int i = 0; i < playerBuildings.size(); i++) {
            BuildingDef building = playerBuildings.get(i);
            ArrayList<RoomDef> rooms = new ArrayList<>(building.getRooms());
            rooms.addAll(building.getEmptyOutside());
            bb.putShort((short)rooms.size());

            for (int j = 0; j < rooms.size(); j++) {
                RoomDef room = rooms.get(j);
                bb.put((byte)room.level);
                GameWindow.WriteString(bb, room.name);
                bb.putShort((short)room.getRects().size());

                for (int k = 0; k < room.getRects().size(); k++) {
                    RoomDef.RoomRect rect = room.getRects().get(k);
                    bb.putInt(rect.getX());
                    bb.putInt(rect.getY());
                    bb.putShort((short)rect.getW());
                    bb.putShort((short)rect.getH());
                }
            }
        }
    }

    public void load() {
        if (!Core.getInstance().isNoSave()) {
            File file = ZomboidFileSystem.instance.getFileInCurrentSave("player_buildings.bin");

            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
                synchronized (SliceY.SliceBufferLock) {
                    SliceY.SliceBuffer.clear();
                    int numBytes = bis.read(SliceY.SliceBuffer.array());
                    SliceY.SliceBuffer.limit(numBytes);
                    this.load(SliceY.SliceBuffer);
                }
            } catch (FileNotFoundException var12) {
            } catch (Exception var13) {
                ExceptionLogger.logException(var13);
            }
        }
    }

    void load(ByteBuffer bb) throws IOException {
        byte[] magic = new byte[FILE_MAGIC.length];
        bb.get(magic);
        if (!Arrays.equals(magic, FILE_MAGIC)) {
            throw new IOException("not magic");
        } else {
            int version = bb.getInt();
            if (version >= 1 && version <= 1) {
                int numRemovedBuildings = bb.getShort();

                for (int i = 0; i < numRemovedBuildings; i++) {
                    RemovedBuilding removedBuilding = new RemovedBuilding();
                    removedBuilding.x = bb.getInt();
                    removedBuilding.y = bb.getInt();
                    removedBuilding.z = bb.getInt();
                    this.removedBuildings.add(removedBuilding);
                }

                int numBuildings = bb.getShort();

                for (int i = 0; i < numBuildings; i++) {
                    BuildingDef buildingDef = new BuildingDef(true);
                    int numRooms = bb.getShort();

                    for (int j = 0; j < numRooms; j++) {
                        RoomDef roomDef = new RoomDef(0L, "");
                        roomDef.level = bb.get();
                        roomDef.name = GameWindow.ReadStringUTF(bb);
                        roomDef.explored = true;
                        int numRects = bb.getShort();

                        for (int k = 0; k < numRects; k++) {
                            int x = bb.getInt();
                            int y = bb.getInt();
                            int w = bb.getShort();
                            int h = bb.getShort();
                            RoomDef.RoomRect rect = new RoomDef.RoomRect(x, y, w, h);
                            roomDef.getRects().add(rect);
                        }

                        if (roomDef.isEmptyOutside()) {
                            buildingDef.getEmptyOutside().add(roomDef);
                        } else {
                            buildingDef.getRooms().add(roomDef);
                        }
                    }

                    this.buildings.add(buildingDef);
                }
            }
        }
    }

    public ArrayList<BuildingDef> getBuildings() {
        return this.buildings;
    }

    public ArrayList<RemovedBuilding> getRemovedBuildings() {
        return this.removedBuildings;
    }
}
