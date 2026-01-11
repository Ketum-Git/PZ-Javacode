// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.network.GameClient;

@UsedFromLua
public final class NonPvpZone {
    private int x;
    private int y;
    private int x2;
    private int y2;
    private int size;
    private String title;
    public static final ArrayList<NonPvpZone> nonPvpZoneList = new ArrayList<>();

    public NonPvpZone() {
    }

    public NonPvpZone(String title, int x, int y, int x2, int y2) {
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

        this.setX(x);
        this.setX2(x2);
        this.setY(y);
        this.setY2(y2);
        this.title = title;
        this.size = Math.abs(x - x2 + y - y2);
    }

    public static NonPvpZone addNonPvpZone(String title, int x, int y, int x2, int y2) {
        NonPvpZone zone = new NonPvpZone(title, x, y, x2, y2);
        nonPvpZoneList.add(zone);
        zone.syncNonPvpZone(false);
        return zone;
    }

    public static void removeNonPvpZone(String title) {
        NonPvpZone zone = getZoneByTitle(title);
        if (zone != null) {
            nonPvpZoneList.remove(zone);
            zone.syncNonPvpZone(true);
        }
    }

    public static NonPvpZone getZoneByTitle(String title) {
        for (int i = 0; i < nonPvpZoneList.size(); i++) {
            NonPvpZone zone = nonPvpZoneList.get(i);
            if (zone.getTitle().equals(title)) {
                return zone;
            }
        }

        return null;
    }

    public static NonPvpZone getNonPvpZone(int x, int y) {
        for (int i = 0; i < nonPvpZoneList.size(); i++) {
            NonPvpZone zone = nonPvpZoneList.get(i);
            if (x >= zone.getX() && x < zone.getX2() && y >= zone.getY() && y < zone.getY2()) {
                return zone;
            }
        }

        return null;
    }

    public static ArrayList<NonPvpZone> getAllZones() {
        return nonPvpZoneList;
    }

    public void syncNonPvpZone(boolean remove) {
        if (GameClient.client) {
            GameClient.sendNonPvpZone(this, remove);
        }
    }

    public void save(ByteBuffer output) {
        output.putInt(this.getX());
        output.putInt(this.getY());
        output.putInt(this.getX2());
        output.putInt(this.getY2());
        output.putInt(this.getSize());
        GameWindow.WriteString(output, this.getTitle());
    }

    public void load(ByteBuffer input, int WorldVersion) {
        this.setX(input.getInt());
        this.setY(input.getInt());
        this.setX2(input.getInt());
        this.setY2(input.getInt());
        this.setSize(input.getInt());
        this.setTitle(GameWindow.ReadString(input));
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX2() {
        return this.x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return this.y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
