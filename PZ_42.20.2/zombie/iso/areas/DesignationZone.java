// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.SyncZonePacket;
import zombie.util.StringUtils;

@UsedFromLua
public class DesignationZone {
    public Double id = 0.0;
    public int hourLastSeen;
    public int lastActionTimestamp;
    public String name;
    public String type;
    public int x;
    public int y;
    public int z;
    public int w;
    public int h;
    public boolean streamed = true;
    public static long lastUpdate;
    public static final ArrayList<DesignationZone> allZones = new ArrayList<>();

    public static DesignationZone addZone(String type, String name, int x, int y, int z, int x2, int y2) {
        return new DesignationZone(type, name, x, y, z, x2, y2, true);
    }

    public DesignationZone() {
    }

    public DesignationZone(String type, String name, int x, int y, int z, int x2, int y2, boolean doSync) {
        if (x > x2) {
            int x3 = x2;
            x2 = x;
            x = x3;
        }

        if (y > y2) {
            int y3 = y2;
            y2 = y;
            y = y3;
        }

        this.id = Rand.Next(9999999) + 100000.0;
        this.type = type;
        if (StringUtils.isNullOrEmpty(name)) {
            name = "";
        }

        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = x2 - x;
        this.h = y2 - y;
        if (getZoneById(this.id) == null) {
            allZones.add(this);
            if (doSync) {
                this.sync();
            }
        }
    }

    public void doMeta(int hours) {
    }

    public boolean isStillStreamed() {
        IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
        IsoGridSquare sq2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w - 1, this.y + this.h - 1, this.z);
        return sq != null || sq2 != null;
    }

    public static void removeZone(String type, String name) {
        DesignationZone zone = getZoneByNameAndType(type, name);
        if (zone != null) {
            allZones.remove(zone);
        }
    }

    public static void removeZone(DesignationZone zone, boolean doSync) {
        if (zone != null) {
            allZones.remove(zone);
            if (doSync) {
                zone.sync();
            }
        }
    }

    public static DesignationZone getZoneByName(String name) {
        for (int i = 0; i < allZones.size(); i++) {
            DesignationZone zone = allZones.get(i);
            if (zone.name.equals(name)) {
                return zone;
            }
        }

        return null;
    }

    public static DesignationZone getZoneByNameAndType(String type, String name) {
        for (int i = 0; i < allZones.size(); i++) {
            DesignationZone zone = allZones.get(i);
            if (zone.name.equals(name) && zone.type.equals(type)) {
                return zone;
            }
        }

        return null;
    }

    public static DesignationZone getZone(int x, int y, int z) {
        for (int i = 0; i < allZones.size(); i++) {
            DesignationZone zone = allZones.get(i);
            if (x >= zone.x && x < zone.x + zone.w && y >= zone.y && y < zone.y + zone.h && zone.z == z) {
                return zone;
            }
        }

        return null;
    }

    public static DesignationZone getZoneByType(String type, int x, int y, int z) {
        for (int i = 0; i < allZones.size(); i++) {
            DesignationZone zone = allZones.get(i);
            if (x >= zone.x && x < zone.x + zone.w && y >= zone.y && y < zone.y + zone.h && zone.z == z && type.equals(zone.type)) {
                return zone;
            }
        }

        return null;
    }

    public boolean isFullyStreamed() {
        if (IsoWorld.instance.getCell() == null) {
            return false;
        } else {
            IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
            IsoGridSquare sq2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w - 1, this.y + this.h - 1, this.z);
            return sq != null && sq2 != null;
        }
    }

    public static DesignationZone getZoneById(Double id) {
        for (int i = 0; i < allZones.size(); i++) {
            if (allZones.get(i).id.equals(id)) {
                return allZones.get(i);
            }
        }

        return null;
    }

    public void unloading() {
        if (!this.isStillStreamed() && this.streamed) {
            this.streamed = false;
            this.hourLastSeen = (int)GameTime.getInstance().getWorldAgeHours();
        }
    }

    public void loading() {
        if (this.isFullyStreamed() && !this.streamed) {
            this.streamed = true;
            this.doMeta((int)GameTime.getInstance().getWorldAgeHours() - this.hourLastSeen);
        }
    }

    private void checkStreamed() {
        IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, this.z);
        IsoGridSquare sq2 = IsoWorld.instance.getCell().getGridSquare(this.x + this.w - 1, this.y + this.h - 1, this.z);
        if (sq != null && sq2 != null && !this.streamed) {
            this.streamed = true;
            this.doMeta((int)GameTime.getInstance().getWorldAgeHours() - this.hourLastSeen);
        }

        if ((sq == null || sq2 == null) && this.streamed) {
            this.streamed = false;
            this.hourLastSeen = (int)GameTime.getInstance().getWorldAgeHours();
        }
    }

    public IsoGridSquare getRandomSquare() {
        return IsoWorld.instance.getCell().getGridSquare(Rand.Next(this.x + 1, this.x + this.w - 1), Rand.Next(this.y + 1, this.y + this.h - 1), this.z);
    }

    public IsoGridSquare getRandomFreeSquare() {
        for (int i = 0; i < 1000; i++) {
            IsoGridSquare sq = IsoWorld.instance
                .getCell()
                .getGridSquare(Rand.Next(this.x + 1, this.x + this.w - 1), Rand.Next(this.y + 1, this.y + this.h - 1), this.z);
            if (sq.isFree(true)) {
                return sq;
            }
        }

        return IsoWorld.instance.getCell().getGridSquare(Rand.Next(this.x + 1, this.x - this.w - 1), Rand.Next(this.y + 1, this.y - this.h - 1), this.z);
    }

    public static ArrayList<DesignationZone> getAllZonesByType(String type) {
        ArrayList<DesignationZone> result = new ArrayList<>();

        for (int i = 0; i < allZones.size(); i++) {
            DesignationZone zone = allZones.get(i);
            if (type.equals(zone.type)) {
                result.add(zone);
            }
        }

        return result;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.sync();
    }

    public static void update() {
        if (System.currentTimeMillis() > lastUpdate + 2500L) {
            lastUpdate = System.currentTimeMillis();

            for (int i = 0; i < allZones.size(); i++) {
                allZones.get(i).checkStreamed();
                allZones.get(i).check();
            }
        }
    }

    public void check() {
    }

    public int getW() {
        return this.w;
    }

    public int getH() {
        return this.h;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public void save(ByteBuffer output) {
        output.putDouble(this.id);
        output.putInt(this.x);
        output.putInt(this.y);
        output.putInt(this.z);
        output.putInt(this.h);
        output.putInt(this.w);
        GameWindow.WriteString(output, this.type);
        GameWindow.WriteString(output, this.name);
        output.putInt(this.hourLastSeen);
    }

    public static DesignationZone load(ByteBuffer input, int worldVersion) {
        double id = input.getDouble();
        int x = input.getInt();
        int y = input.getInt();
        int z = input.getInt();
        int h = input.getInt();
        int w = input.getInt();
        String type = GameWindow.ReadString(input);
        String name = GameWindow.ReadString(input);
        int hourLastSeen = input.getInt();
        if (type.equals("AnimalZone")) {
            DesignationZoneAnimal zone = DesignationZoneAnimal.getZoneById(id);
            if (zone == null) {
                zone = new DesignationZoneAnimal(name, x, y, z, x + w, y + h, false);
            } else {
                zone.name = name;
                zone.w = w;
                zone.h = h;
            }

            zone.id = id;
            zone.hourLastSeen = hourLastSeen;
            return zone;
        } else {
            return null;
        }
    }

    public static void Reset() {
        allZones.clear();
    }

    public Double getId() {
        return this.id;
    }

    protected void sync() {
        SyncZonePacket packet = new SyncZonePacket();
        boolean added = allZones.contains(this);
        packet.setData(this, added);
        if (GameClient.client) {
            packet.sendToServer(PacketTypes.PacketType.SyncZone);
        } else if (GameServer.server) {
            packet.sendToClients(PacketTypes.PacketType.SyncZone, null);
        }
    }
}
