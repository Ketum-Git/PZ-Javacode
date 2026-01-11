// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

@UsedFromLua
public final class IsoMetaChunk {
    public static final float zombiesMinPerChunk = 0.06F;
    public static final float zombiesFullPerChunk = 12.0F;
    private byte zombieIntensity;
    private Zone[] zones;
    private int zonesSize;
    private RoomDef[] rooms;
    private int roomsSize;

    public int getZonesSize() {
        return this.zonesSize;
    }

    public void compactZoneArray() {
        if (this.zones != null && this.zonesSize != this.zones.length) {
            this.zones = Arrays.copyOf(this.zones, this.zonesSize);
        }
    }

    public void compactRoomDefArray() {
        if (this.rooms != null && this.roomsSize != this.rooms.length) {
            this.rooms = Arrays.copyOf(this.rooms, this.roomsSize);
        }
    }

    public boolean doesHaveForaging() {
        if (this.zones == null) {
            return false;
        } else {
            List<String> foragingZones = IsoWorld.instance.getBiomeMap().getForagingZones();
            return Arrays.stream(this.zones).anyMatch(z -> z != null && foragingZones.contains(z.type));
        }
    }

    public boolean doesHaveZone(String zone) {
        return this.zones == null ? false : Arrays.stream(this.zones).anyMatch(z -> z != null && z.type.equalsIgnoreCase(zone));
    }

    public int getRoomsSize() {
        return this.roomsSize;
    }

    public float getZombieIntensity(boolean bRandom) {
        float zombieIntensity = this.zombieIntensity & 255;
        float delta = zombieIntensity / 255.0F;
        if (SandboxOptions.instance.distribution.getValue() == 2) {
            zombieIntensity = 128.0F;
            delta = 0.5F;
        }

        zombieIntensity *= 0.5F;
        if (SandboxOptions.instance.zombies.getValue() == 1) {
            zombieIntensity *= 4.0F;
        } else if (SandboxOptions.instance.zombies.getValue() == 2) {
            zombieIntensity *= 3.0F;
        } else if (SandboxOptions.instance.zombies.getValue() == 3) {
            zombieIntensity *= 2.0F;
        } else if (SandboxOptions.instance.zombies.getValue() == 5) {
            zombieIntensity *= 0.35F;
        } else if (SandboxOptions.instance.zombies.getValue() == 6) {
            zombieIntensity = 0.0F;
        }

        delta = zombieIntensity / 255.0F;
        float dif = 11.94F;
        dif *= delta;
        zombieIntensity = 0.06F + dif;
        if (!bRandom) {
            return zombieIntensity;
        } else {
            float chance = delta * 10.0F;
            if (Rand.Next(3) == 0) {
                return 0.0F;
            } else {
                chance *= 0.5F;
                int random = 1000;
                if (SandboxOptions.instance.zombies.getValue() == 1) {
                    random = (int)(random / 2.0F);
                } else if (SandboxOptions.instance.zombies.getValue() == 2) {
                    random = (int)(random / 1.7F);
                } else if (SandboxOptions.instance.zombies.getValue() == 3) {
                    random = (int)(random / 1.5F);
                } else if (SandboxOptions.instance.zombies.getValue() == 5) {
                    random = (int)(random * 1.5F);
                }

                if (Rand.Next(random) < chance && IsoWorld.getZombiesEnabled()) {
                    zombieIntensity = 120.0F;
                    if (zombieIntensity > 12.0F) {
                        zombieIntensity = 12.0F;
                    }
                }

                return zombieIntensity;
            }
        }
    }

    public float getZombieIntensity() {
        return this.getZombieIntensity(true);
    }

    public void setZombieIntensity(byte zombieIntensity) {
        this.zombieIntensity = zombieIntensity;
    }

    public float getLootZombieIntensity() {
        float zombieIntensity = this.zombieIntensity & 255;
        float delta = zombieIntensity / 255.0F;
        float dif = 11.94F;
        dif *= delta;
        zombieIntensity = 0.06F + dif;
        float chance = delta * 10.0F;
        delta = delta * delta * delta;
        if (Rand.Next(300) <= chance) {
            zombieIntensity = 120.0F;
        }

        return IsoWorld.getZombiesDisabled() ? 400.0F : zombieIntensity;
    }

    public int getUnadjustedZombieIntensity() {
        return this.zombieIntensity & 0xFF;
    }

    public void addZone(Zone zone) {
        if (this.zones == null) {
            this.zones = new Zone[4];
        }

        if (this.zonesSize == this.zones.length) {
            Zone[] newZones = new Zone[this.zones.length + 4];
            System.arraycopy(this.zones, 0, newZones, 0, this.zonesSize);
            this.zones = newZones;
        }

        this.zones[this.zonesSize++] = zone;
    }

    public void removeZone(Zone zone) {
        if (this.zones != null) {
            for (int i = 0; i < this.zonesSize; i++) {
                if (this.zones[i] == zone) {
                    while (i < this.zonesSize - 1) {
                        this.zones[i] = this.zones[i + 1];
                        i++;
                    }

                    this.zones[this.zonesSize - 1] = null;
                    this.zonesSize--;
                    break;
                }
            }
        }
    }

    public Zone getZone(int index) {
        return index >= 0 && index < this.zonesSize ? this.zones[index] : null;
    }

    public Zone getZoneAt(int x, int y, int z) {
        if (this.zones != null && this.zonesSize > 0) {
            Zone highest = null;

            for (int i = this.zonesSize - 1; i >= 0; i--) {
                Zone zone = this.zones[i];
                if (zone.contains(x, y, z)) {
                    if (zone.isPreferredZoneForSquare) {
                        return zone;
                    }

                    if (highest == null) {
                        highest = zone;
                    }
                }
            }

            return highest;
        } else {
            return null;
        }
    }

    public ArrayList<Zone> getZonesAt(int x, int y, int z, ArrayList<Zone> result) {
        for (int i = 0; i < this.zonesSize; i++) {
            Zone zone = this.zones[i];
            if (zone.contains(x, y, z)) {
                result.add(zone);
            }
        }

        return result;
    }

    public ArrayList<Zone> getZonesAt(int x, int y, int z) {
        return this.getZonesAt(x, y, z, new ArrayList<>());
    }

    public Zone getZoneAt(int x, int y, int z, String zone) {
        return this.getZonesAt(x, y, z).stream().filter(i -> i != null && Objects.equals(i.type, zone)).findFirst().orElse(null);
    }

    public void getZonesUnique(Set<Zone> result) {
        for (int i = 0; i < this.zonesSize; i++) {
            Zone zone = this.zones[i];
            result.add(zone);
        }
    }

    public void getZonesIntersecting(int x, int y, int z, int w, int h, ArrayList<Zone> result) {
        for (int i = 0; i < this.zonesSize; i++) {
            Zone zone = this.zones[i];
            if (!result.contains(zone) && zone.intersects(x, y, z, w, h)) {
                result.add(zone);
            }
        }
    }

    public void clearZones() {
        if (this.zones != null) {
            for (int i = 0; i < this.zones.length; i++) {
                this.zones[i] = null;
            }
        }

        this.zones = null;
        this.zonesSize = 0;
    }

    public void clearRooms() {
        if (this.rooms != null) {
            for (int i = 0; i < this.rooms.length; i++) {
                this.rooms[i] = null;
            }
        }

        this.rooms = null;
        this.roomsSize = 0;
    }

    public void addRoom(RoomDef room) {
        if (this.rooms == null) {
            this.rooms = new RoomDef[8];
        }

        if (this.roomsSize == this.rooms.length) {
            RoomDef[] newRooms = new RoomDef[this.rooms.length + 8];
            System.arraycopy(this.rooms, 0, newRooms, 0, this.roomsSize);
            this.rooms = newRooms;
        }

        this.rooms[this.roomsSize++] = room;
    }

    public void removeRoom(RoomDef room) {
        if (this.rooms != null) {
            for (int i = 0; i < this.roomsSize; i++) {
                if (this.rooms[i] == room) {
                    while (i < this.roomsSize - 1) {
                        this.rooms[i] = this.rooms[i + 1];
                        i++;
                    }

                    this.rooms[this.roomsSize - 1] = null;
                    this.roomsSize--;
                    break;
                }
            }
        }
    }

    public RoomDef getRoomAt(int x, int y, int z) {
        RoomDef nonUserDefined = null;

        for (int i = this.roomsSize - 1; i >= 0; i--) {
            RoomDef room = this.rooms[i];
            if (!room.isEmptyOutside() && room.level == z && room.contains(x, y)) {
                if (room.userDefined) {
                    return room;
                }

                if (nonUserDefined == null) {
                    nonUserDefined = room;
                }
            }
        }

        return nonUserDefined;
    }

    public RoomDef getEmptyOutsideAt(int x, int y, int z) {
        for (int i = 0; i < this.roomsSize; i++) {
            RoomDef room = this.rooms[i];
            if (room.isEmptyOutside() && room.level == z && room.contains(x, y)) {
                return room;
            }
        }

        return null;
    }

    public BuildingDef getAssociatedBuildingAt(int x, int y) {
        for (int i = 0; i < this.roomsSize; i++) {
            RoomDef room = this.rooms[i];
            if (room.getBuilding().getMaxLevel() >= 0) {
                if (room.isEmptyOutside()) {
                }

                for (int n = 0; n < room.rects.size(); n++) {
                    RoomDef.RoomRect rr = room.rects.get(n);
                    if (rr.getX() - 1 <= x && rr.getY() - 1 <= y && x <= rr.getX2() && y <= rr.getY2()) {
                        return room.building;
                    }
                }
            }
        }

        return null;
    }

    public void getBuildingsIntersecting(int x, int y, int w, int h, ArrayList<BuildingDef> result) {
        for (int i = 0; i < this.roomsSize; i++) {
            RoomDef roomDef = this.rooms[i];
            if ((!roomDef.isEmptyOutside() || roomDef.getBuilding().getRooms().isEmpty())
                && !result.contains(roomDef.building)
                && roomDef.intersects(x, y, w, h)) {
                result.add(roomDef.building);
            }
        }
    }

    public void getRoomsIntersecting(int x, int y, int w, int h, ArrayList<RoomDef> result) {
        for (int i = 0; i < this.roomsSize; i++) {
            RoomDef roomDef = this.rooms[i];
            if ((!roomDef.isEmptyOutside() || roomDef.getBuilding().getRooms().isEmpty()) && !result.contains(roomDef) && roomDef.intersects(x, y, w, h)) {
                result.add(roomDef);
            }
        }
    }

    public void Dispose() {
        if (this.rooms != null) {
            Arrays.fill(this.rooms, null);
        }

        if (this.zones != null) {
            Arrays.fill(this.zones, null);
        }
    }
}
