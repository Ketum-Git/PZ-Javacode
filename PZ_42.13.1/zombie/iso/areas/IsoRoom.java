// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;

@UsedFromLua
public final class IsoRoom {
    private static final ArrayList<IsoGridSquare> tempSquares = new ArrayList<>();
    public final Vector<IsoGridSquare> beds = new Vector<>();
    public Rectangle bounds;
    public IsoBuilding building;
    public final ArrayList<ItemContainer> containers = new ArrayList<>();
    public final ArrayList<IsoWindow> windows = new ArrayList<>();
    public final Vector<IsoRoomExit> exits = new Vector<>();
    public int layer;
    public String roomDef = "none";
    public final Vector<IsoGridSquare> tileList = new Vector<>();
    public int transparentWalls;
    public final ArrayList<IsoLightSwitch> lightSwitches = new ArrayList<>();
    public final ArrayList<IsoRoomLight> roomLights = new ArrayList<>();
    public final ArrayList<IsoObject> waterSources = new ArrayList<>();
    public static final int MAXIMUM_DAYS = 1000000000;
    public int seen = 1000000000;
    public int visited = 1000000000;
    public RoomDef def;
    public final ArrayList<RoomDef.RoomRect> rects = new ArrayList<>(1);
    public final ArrayList<IsoGridSquare> squares = new ArrayList<>();

    public IsoBuilding getBuilding() {
        return this.building;
    }

    public String getName() {
        return this.roomDef;
    }

    public IsoBuilding CreateBuilding(IsoCell cell) {
        IsoBuilding building = new IsoBuilding(cell);
        this.AddToBuilding(building);
        return building;
    }

    public boolean isInside(int x, int y, int z) {
        for (int n = 0; n < this.rects.size(); n++) {
            int rx = this.rects.get(n).x;
            int ry = this.rects.get(n).y;
            int rx2 = this.rects.get(n).getX2();
            int ry2 = this.rects.get(n).getY2();
            if (x >= rx && y >= ry && x < rx2 && y < ry2 && z == this.layer) {
                return true;
            }
        }

        return false;
    }

    public IsoGridSquare getFreeTile() {
        boolean bDone = false;
        IsoGridSquare tile = null;
        int nCount = 100;

        while (!bDone && nCount > 0) {
            nCount--;
            bDone = true;
            if (this.tileList.isEmpty()) {
                return null;
            }

            tile = this.tileList.get(Rand.Next(this.tileList.size()));

            for (int m = 0; m < this.exits.size(); m++) {
                if (tile.getX() == this.exits.get(m).x && tile.getY() == this.exits.get(m).y) {
                    bDone = false;
                }
            }

            if (bDone && !tile.isFree(true)) {
                bDone = false;
            }
        }

        return nCount < 0 ? null : tile;
    }

    void AddToBuilding(IsoBuilding building) {
        this.building = building;
        building.AddRoom(this);

        for (IsoRoomExit exit : this.exits) {
            if (exit.to.from != null && exit.to.from.building == null) {
                exit.to.from.AddToBuilding(building);
            }
        }
    }

    /**
     * @return the WaterSources
     */
    public ArrayList<IsoObject> getWaterSources() {
        return this.waterSources;
    }

    /**
     * 
     * @param WaterSources the WaterSources to set
     */
    public void setWaterSources(ArrayList<IsoObject> WaterSources) {
        this.waterSources.clear();
        this.waterSources.addAll(WaterSources);
    }

    public boolean hasWater() {
        if (this.waterSources.isEmpty()) {
            return false;
        } else {
            Iterator<IsoObject> it = this.waterSources.iterator();

            while (it != null && it.hasNext()) {
                IsoObject o = it.next();
                if (o.hasWater()) {
                    return true;
                }
            }

            return false;
        }
    }

    public void useWater() {
        if (!this.waterSources.isEmpty()) {
            Iterator<IsoObject> it = this.waterSources.iterator();

            while (it != null && it.hasNext()) {
                IsoObject o = it.next();
                if (o.hasWater()) {
                    o.useFluid(1.0F);
                    break;
                }
            }
        }
    }

    public ArrayList<IsoWindow> getWindows() {
        return this.windows;
    }

    public void addSquare(IsoGridSquare sq) {
        if (!this.squares.contains(sq)) {
            this.squares.add(sq);
        }
    }

    public void refreshSquares() {
        this.windows.clear();
        this.containers.clear();
        this.waterSources.clear();
        this.exits.clear();
        tempSquares.clear();
        tempSquares.addAll(this.squares);
        this.squares.clear();

        for (int n = 0; n < tempSquares.size(); n++) {
            this.addSquare(tempSquares.get(n));
        }
    }

    private void addExitTo(IsoGridSquare sq, IsoGridSquare sq2) {
        IsoRoom a = null;
        IsoRoom b = null;
        if (sq != null) {
            a = sq.getRoom();
        }

        if (sq2 != null) {
            b = sq2.getRoom();
        }

        if (a != null || b != null) {
            IsoRoom use = a;
            if (a == null) {
                use = b;
            }

            IsoRoomExit e = new IsoRoomExit(use, sq.getX(), sq.getY(), sq.getZ());
            e.type = IsoRoomExit.ExitType.Door;
            if (use == a) {
                if (b != null) {
                    IsoRoomExit exit2 = b.getExitAt(sq2.getX(), sq2.getY(), sq2.getZ());
                    if (exit2 == null) {
                        exit2 = new IsoRoomExit(b, sq2.getX(), sq2.getY(), sq2.getZ());
                        b.exits.add(exit2);
                    }

                    e.to = exit2;
                } else {
                    a.building.exits.add(e);
                    if (sq2 != null) {
                        e.to = new IsoRoomExit(e, sq2.getX(), sq2.getY(), sq2.getZ());
                    }
                }

                a.exits.add(e);
            } else {
                b.building.exits.add(e);
                if (sq2 != null) {
                    e.to = new IsoRoomExit(e, sq2.getX(), sq2.getY(), sq2.getZ());
                }

                b.exits.add(e);
            }
        }
    }

    private IsoRoomExit getExitAt(int x, int y, int z) {
        for (int n = 0; n < this.exits.size(); n++) {
            IsoRoomExit e = this.exits.get(n);
            if (e.x == x && e.y == y && e.layer == z) {
                return e;
            }
        }

        return null;
    }

    public void removeSquare(IsoGridSquare sq) {
        this.squares.remove(sq);
        IsoRoomExit e = this.getExitAt(sq.getX(), sq.getY(), sq.getZ());
        if (e != null) {
            this.exits.remove(e);
            if (e.to != null) {
                e.from = null;
            }

            if (this.building.exits.contains(e)) {
                this.building.exits.remove(e);
            }
        }

        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoObject o = sq.getObjects().get(i);
            if (o instanceof IsoLightSwitch) {
                this.lightSwitches.remove(o);
            }
        }
    }

    public void spawnZombies() {
        VirtualZombieManager.instance.addZombiesToMap(1, this.def, false);
    }

    public void onSee() {
        if (!GameClient.client) {
            BuildingDef def = this.getBuilding().getDef();
            if (def != null && StashSystem.isStashBuilding(def)) {
                StashSystem.visitedBuilding(def);
            }
        }

        for (int i = 0; i < this.getBuilding().rooms.size(); i++) {
            IsoRoom room = this.getBuilding().rooms.elementAt(i);
            if (VirtualZombieManager.instance.shouldSpawnZombiesOnLevel(room.def.level)) {
                if (room != null && !room.def.explored) {
                    room.def.explored = true;
                }

                IsoWorld.instance.getCell().roomSpotted(room);
            }
        }
    }

    public Vector<IsoGridSquare> getTileList() {
        return this.tileList;
    }

    public ArrayList<IsoGridSquare> getSquares() {
        return this.squares;
    }

    public ArrayList<ItemContainer> getContainer() {
        return this.containers;
    }

    public IsoGridSquare getRandomSquare() {
        return this.squares.isEmpty() ? null : this.squares.get(Rand.Next(this.squares.size()));
    }

    public IsoGridSquare getRandomFreeSquare() {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true)) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true)) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public IsoGridSquare getRandomDoorFreeSquare() {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true) && square.isGoodSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true) && square.isGoodSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public IsoGridSquare getRandomWallFreeSquare() {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true) && !square.isWallSquare() && square.isGoodSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true) && !square.isWallSquare() && square.isGoodSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public IsoGridSquare getRandomWallFreePairSquare(IsoDirections dir, boolean both) {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true) && square.isFreeWallPair(dir, both)) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true) && square.isFreeWallPair(dir, both)) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public IsoGridSquare getRandomWallSquare() {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true) && square.isFreeWallSquare() && !square.isDoorSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true) && square.isFreeWallSquare() && !square.isDoorSquare()) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public IsoGridSquare getRandomDoorAndWallFreeSquare() {
        int count = 100;
        IsoGridSquare square = null;
        if (GameServer.server) {
            while (count > 0) {
                square = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.def.getX() + Rand.Next(this.def.getW()), this.def.getY() + Rand.Next(this.def.getH()), this.def.level);
                if (square != null && square.getRoom() == this && square.isFree(true) && !square.isDoorOrWallSquare() && square.getObjects().size() < 2) {
                    return square;
                }

                count--;
            }

            return null;
        } else if (this.squares.isEmpty()) {
            return null;
        } else {
            while (count > 0) {
                square = this.squares.get(Rand.Next(this.squares.size()));
                if (square.isFree(true) && !square.isDoorOrWallSquare() && square.getObjects().size() < 2) {
                    return square;
                }

                count--;
            }

            return null;
        }
    }

    public boolean hasLightSwitches() {
        if (!this.lightSwitches.isEmpty()) {
            return true;
        } else {
            for (int i = 0; i < this.def.objects.size(); i++) {
                if (this.def.objects.get(i).getType() == 7) {
                    return true;
                }
            }

            return false;
        }
    }

    public void createLights(boolean active) {
        if (this.roomLights.isEmpty()) {
            for (int n = 0; n < this.def.rects.size(); n++) {
                RoomDef.RoomRect rr = this.def.rects.get(n);
                IsoRoomLight roomLight = new IsoRoomLight(this, rr.x, rr.y, this.def.level, rr.w, rr.h);
                this.roomLights.add(roomLight);
            }
        }
    }

    public IsoRoomLight findRoomLightByID(int id) {
        for (int i = 0; i < this.roomLights.size(); i++) {
            IsoRoomLight roomLight = this.roomLights.get(i);
            if (roomLight.id == id) {
                return roomLight;
            }
        }

        return null;
    }

    public RoomDef getRoomDef() {
        return this.def;
    }

    public ArrayList<IsoLightSwitch> getLightSwitches() {
        return this.lightSwitches;
    }

    public boolean spawnRandomWorkstation() {
        IsoGridSquare square = this.getRandomWallSquare();
        if (square == null) {
            square = this.getRandomDoorFreeSquare();
        }

        if (square != null) {
            square.spawnRandomWorkstation();
            return true;
        } else {
            return false;
        }
    }

    public boolean spawnRandom2TileWorkstation() {
        boolean good = false;
        String thing = null;
        if (Rand.NextBool(3)) {
            int tool = Rand.Next(2);
            switch (tool) {
                case 0:
                    good = this.addMetalWorkbench();
                    break;
                case 1:
                    good = this.addPotteryWheel();
            }
        }

        return good ? true : this.spawnRandomWorkstation();
    }

    public boolean addMetalWorkbench() {
        return this.add2TileBench("Base.Metalworkbench", "crafted_02_2", "crafted_02_3", "crafted_02_0", "crafted_02_1", true);
    }

    public boolean addPotteryWheel() {
        return Rand.NextBool(2) ? this.addOldPotteryWheel() : this.addModernPotteryWheel();
    }

    public boolean addOldPotteryWheel() {
        return this.add2TileBench("Base.Pottery_Wheel", "crafted_01_64", "crafted_01_65", "crafted_01_66", "crafted_01_67", false);
    }

    public boolean addModernPotteryWheel() {
        return Rand.NextBool(2)
            ? this.add2TileBench("Base.Pottery_Wheel_Modern", "crafted_01_92", "crafted_01_93", "crafted_01_94", "crafted_01_95", false)
            : this.add2TileBench("Base.Pottery_Wheel_Modern", "crafted_01_88", "crafted_01_89", "crafted_01_90", "crafted_01_91", false);
    }

    public boolean add2TileBench(String bench, String sprite1, String sprite2, String sprite3, String sprite4, boolean both) {
        IsoGridSquare square = null;
        boolean north = false;
        if (Rand.NextBool(2)) {
            north = true;
            square = this.getRandomWallFreePairSquare(IsoDirections.N, both);
        }

        if (square == null) {
            square = this.getRandomWallFreePairSquare(IsoDirections.E, both);
        }

        if (!north && square == null) {
            north = true;
            square = this.getRandomWallFreePairSquare(IsoDirections.N, both);
        }

        if (square == null) {
            return false;
        } else if (north) {
            IsoGridSquare sq = square.getAdjacentSquare(IsoDirections.N);
            if (sq == null) {
                return false;
            } else {
                square.addWorkstationEntity(bench, sprite1);
                sq.addWorkstationEntity(bench, sprite2);
                return true;
            }
        } else {
            IsoGridSquare sq = square.getAdjacentSquare(IsoDirections.E);
            if (sq == null) {
                return false;
            } else {
                square.addWorkstationEntity(bench, sprite3);
                sq.addWorkstationEntity(bench, sprite4);
                return true;
            }
        }
    }

    public boolean isShop() {
        String roomName = this.getName();
        if (roomName == null) {
            return false;
        } else {
            if (ItemPickerJava.rooms.containsKey(roomName)) {
                ItemPickerJava.ItemPickerRoom roomDist = ItemPickerJava.rooms.get(roomName);
                if (roomDist != null && roomDist.isShop) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isDerelict() {
        return StringUtils.containsIgnoreCase(this.getName(), "derelict");
    }
}
