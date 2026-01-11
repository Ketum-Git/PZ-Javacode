// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

public final class RoomID {
    public static long makeID(int cellX, int cellY, int roomIndex) {
        int hi = cellY << 16 | cellX;
        return (long)hi << 32 | roomIndex;
    }

    public static int getCellX(long ID) {
        int hi = (int)(ID >> 32);
        return hi & 65535;
    }

    public static int getCellY(long ID) {
        int hi = (int)(ID >> 32);
        return hi >> 16 & 65535;
    }

    public static int getIndex(long ID) {
        return (int)(ID & 4294967295L);
    }

    public static boolean isSameCell(long ID, int cellX, int cellY) {
        return getCellX(ID) == cellX && getCellY(ID) == cellY;
    }
}
